/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNil, get, isEmpty, once, isNaN} from 'lodash';
import Enum from 'enum';
import {TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_REMOVE,TABLE_UPDATE,TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import {SUBGROUP, dispatchAttachLayerToPlot, dispatchChangeVisibility, dispatchCreateDrawLayer,
        dispatchDestroyDrawLayer, dispatchModifyCustomField} from '../DrawLayerCntlr.js';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr.js';
import {getTblById, doFetchTable, isTableUsingRadians} from '../../tables/TableUtil.js';
import {cloneRequest, makeTableFunctionRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {serializeDecimateInfo} from '../../tables/Decimate.js';
import {
    getDrawLayerById, getPlotViewAry, getPlotViewById, isDrawLayerAttached, isDrawLayerVisible, primePlot
} from '../PlotViewUtil.js';
import {dlRoot} from '../DrawLayerCntlr.js';
import Catalog, {CatalogType} from '../../drawingLayers/Catalog.js';
import {logger} from '../../util/Logger.js';
import {getMaxScatterRows} from '../../charts/ChartUtil.js';
import {isLsstFootprintTable} from '../task/LSSTFootprintTask.js';
import {parseWorldPt} from '../Point.js';
import {findImageCenterColumns, findTableCenterColumns, isCatalog} from '../../util/VOAnalyzer.js';
import {getAppOptions} from '../../core/AppDataCntlr';
import {makeWorldPt} from '../Point';
import {CoordinateSys} from '../CoordSys.js';
import {PlotAttribute} from '../PlotAttribute.js';
import SearchTarget from '../../drawingLayers/SearchTarget.js';
import {darker} from '../../util/Color.js';
import {getMetaEntry} from '../../tables/TableUtil';
import {MetaConst} from '../../data/MetaConst';


/** @type {TableWatcherDef} */
export const getCatalogWatcherDef= once(() => (
    {
        id : 'CatalogWatcher',
        watcher : watchCatalogs,
        testTable : (table) => !isLsstFootprintTable(table) && isCatalog(table),
        allowMultiples: false,
        actions: [TABLE_LOADED, TABLE_SELECT, TABLE_HIGHLIGHT, TABLE_UPDATE, TBL_RESULTS_ACTIVE,
            TABLE_REMOVE, ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_HIPS]
    }
));

/**
 * @typedef {Object} PointType
 * @summary type of point
 * @prop WORLD
 * @prop GRID
 * @type {Enum}
 */
const PointType= new Enum(['WORLD', 'IMAGE']);

/**
 * type {TableWatchFunc}
 * this saga does the following:
 * <ul>
 *     <li>Waits until first fits image is plotted
 *     <li>loads all the table that are catalogs
 *     <li>Then loops:
 *     <ul>
 *         <li>waits for a table new table, update, highlight or select change and then updates the drawing layer
 *         <li>waits for a new plot and adds any catalog
 *     </ul>
 * </ul>
 * @param tbl_id
 * @param action
 * @param cancelSelf
 * @param params
 * @return {*}
 */
export function watchCatalogs(tbl_id, action, cancelSelf, params) {


    const {tableManaged}= params;
    if (isEmpty(visRoot().plotViewAry) && !tableManaged) {
        return {tableManaged: false, ...params}; // no plots? , don't start until there are plots
    }

    if (!action) {
        handleCatalogUpdate(tbl_id);
        return {tableManaged:true};
    }

    const {payload}= action;
    if (payload.tbl_id && payload.tbl_id!==tbl_id) return params;

    if (!tableManaged && action.type!==TABLE_LOADED) {
        handleCatalogUpdate(tbl_id);
    }

    switch (action.type) {
        case TABLE_LOADED:
            handleCatalogUpdate(tbl_id);
            break;

        case TABLE_SELECT:
            dispatchModifyCustomField(tbl_id, {selectInfo:payload.selectInfo});
            break;

        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
            dispatchModifyCustomField(tbl_id, {highlightedRow:payload.highlightedRow});
            break;

        case TABLE_REMOVE:
            dispatchDestroyDrawLayer(tbl_id);
            dispatchDestroyDrawLayer(searchTargetId(tbl_id));
            cancelSelf();
            break;

        // case TBL_RESULTS_ACTIVE:
        //     recenterImage(getTblById(tbl_id));
        //     break;

        case ImagePlotCntlr.PLOT_HIPS:
        case ImagePlotCntlr.PLOT_IMAGE:
            attachToCatalog(tbl_id, payload);
            break;
    }
    return {tableManaged:true};
}


const searchTargetId= (tbl_id) => 'search-target-'+tbl_id;

/**
 * @param table
 * @returns {PointType}
 */
const getCatalogPtType= (table) =>
    getMetaEntry(table, MetaConst.CATALOG_OVERLAY_TYPE)?.toUpperCase()==='IMAGE_PTS' ? PointType.IMAGE : PointType.WORLD;

function handleCatalogUpdate(tbl_id) {
    const sourceTable= getTblById(tbl_id);
    if (!sourceTable || sourceTable.isFetching) return;
    const {totalRows, request, highlightedRow,selectInfo, title}= sourceTable;
    const maxScatterRows = getMaxScatterRows();


    const catalogPtType= getCatalogPtType(sourceTable);
    const columns= catalogPtType===PointType.WORLD ? findTableCenterColumns(sourceTable) : findImageCenterColumns(sourceTable);

    // recenterImage(sourceTable);
    const c1= catalogPtType===PointType.WORLD ? columns.lonCol : columns.xCol;
    const c2= catalogPtType===PointType.WORLD ? columns.latCol : columns.yCol;
    if (catalogPtType===PointType.IMAGE && !findPlotViewUsingFitsPathMeta(sourceTable)) return;

    const params= {
        startIdx : 0,
        pageSize : MAX_ROW,
        inclCols : `"${c1}","${c2}","ROW_IDX"`        // column names should be in quotes
    };


    let req = cloneRequest(sourceTable.request, params);
    let dataTooBigForSelection= false;
    if (totalRows > maxScatterRows) {
        const sreq = cloneRequest(sourceTable.request, {inclCols: `"${c1}","${c2}"`});
        req = makeTableFunctionRequest(sreq, 'DecimateTable', title,
            {decimate: serializeDecimateInfo(c1, c2, 10000), pageSize: MAX_ROW});
        dataTooBigForSelection= true;
    }

    req.tbl_id = `cat-${tbl_id}`;

    doFetchTable(req).then(
        (tableModel) => {
            if (tableModel.tableData) {
                updateDrawingLayer(tbl_id, tableModel,
                    request, highlightedRow, selectInfo, columns, dataTooBigForSelection, catalogPtType);
            }
        }
    ).catch(
        (reason) => {
            logger.error(`Failed to catalog plot data: ${reason}`, reason);
        }
    );
}

function updateDrawingLayer(tbl_id, tableModel, tableRequest,
                            highlightedRow, selectInfo, columns, dataTooBigForSelection, catalogPtType) {

    const plotIdAry= visRoot().plotViewAry.map( (pv) => pv.plotId);
    const {title, tableData, tableMeta}= tableModel;

    const dl= getDrawLayerById(dlRoot(),tbl_id);
    const {showCatalogSearchTarget}= getAppOptions();
    const searchTarget= showCatalogSearchTarget ? getSearchTarget(tableRequest,tableModel) : undefined;
    if (dl) { // update drawing layer
        dispatchModifyCustomField(tbl_id, {title, tableData, tableMeta, tableRequest,
                                           highlightedRow, selectInfo, columns,
                                           dataTooBigForSelection});
    }
    else { // new drawing layer
        if (catalogPtType===PointType.WORLD) {
            const angleInRadian= isTableUsingRadians(tableModel, [columns.lonCol,columns.latCol]);
            const catDL= dispatchCreateDrawLayer(Catalog.TYPE_ID,
                {catalogId:tbl_id, title, tableData, tableMeta, tableRequest, highlightedRow,
                    selectInfo, columns, dataTooBigForSelection, catalogType:CatalogType.POINT,
                    layersPanelLayoutId: tbl_id, angleInRadian});
            dispatchAttachLayerToPlot(tbl_id, plotIdAry);
            if (searchTarget) {
                const newDL = dispatchCreateDrawLayer(SearchTarget.TYPE_ID,
                    {
                        drawLayerId: searchTargetId(tbl_id),
                        color: darker(catDL.drawingDef.color),
                        searchTargetPoint: searchTarget,
                        layersPanelLayoutId: tbl_id,
                        titlePrefix: 'Catalog ',
                        canUserDelete: true,
                    });
                dispatchAttachLayerToPlot(newDL.drawLayerId, plotIdAry, false);
            }
            const dl= getDrawLayerById(dlRoot(),tbl_id);
            if (dl.supportSubgroups  &&  dl.tableMeta[SUBGROUP]) {
                plotIdAry.map( (plotId) =>  getPlotViewById(visRoot(), plotId))
                    .filter( (pv) => dl.tableMeta[SUBGROUP]!==get(pv, 'drawingSubGroupId'))
                    .forEach( (pv) => pv && dispatchChangeVisibility({id:dl.drawLayerId, visible:false,
                        plotId:pv.plotId, useGroup:false}));
            }
        }
        else {
            const pv=  findPlotViewUsingFitsPathMeta(getTblById(tbl_id));
            if (!pv) return;
            dispatchCreateDrawLayer(Catalog.TYPE_ID,
                {catalogId:tbl_id, title, tableData, tableMeta, tableRequest, highlightedRow,
                    selectInfo, columns, dataTooBigForSelection, catalogType:CatalogType.POINT_IMAGE_PT,
                    layersPanelLayoutId: tbl_id });
            dispatchAttachLayerToPlot(tbl_id, [pv.plotId]);
        }
    }
}

function shouldDlBeVisible(dl,pv) {
    if (!pv) return true;
    return Boolean(pv.drawingSubGroupId) ||
        getPlotViewAry(visRoot())
            .filter( (pv) => isDrawLayerAttached(dl, pv.plotId ))
            .map( (pv) => isDrawLayerVisible(dl, pv.plotId ))
            .reduce( (allV,v) => allV && v, true);
}

function attachToCatalog(tbl_id, payload) {
    const {pvNewPlotInfoAry=[], wpRequest, wpRequestAry, redReq, blueReq, greenReq} = payload;
    const dl= getDrawLayerById(dlRoot(), tbl_id);
    const table= getTblById(tbl_id);
    if (!dl || !table) return;
    if (getCatalogPtType(table)===PointType.IMAGE) return;
    pvNewPlotInfoAry.forEach( (info, idx) => {
        let r= wpRequest || get(wpRequestAry,idx);
        if (!r) r= (redReq || blueReq || greenReq);
        if (!r || r.getAttributes[PlotAttribute.DATALINK_TABLE_ID]===tbl_id) return; //Don't overlay catalogs on image data products
        const pv= getPlotViewById(visRoot(), info.plotId);
        dispatchAttachLayerToPlot(dl.drawLayerId, info.plotId,false, shouldDlBeVisible(dl,pv));
        const pvSubGroup= pv?.drawingSubGroupId;
        const tableSubGroup= dl.tableMeta[SUBGROUP];
        if (!isNil(pvSubGroup) && !isNil(tableSubGroup)  && pvSubGroup!==tableSubGroup) {
            pv && dispatchChangeVisibility({id:dl.drawLayerId, visible:false, plotId:pv.plotId, useGroup:false});
        }
    });
}


export function getSearchTarget(r, tableModel, searchTargetStr, overlayPositionStr) {
    if (!r) r= tableModel.request;
    if (searchTargetStr) return parseWorldPt(searchTargetStr);
    if (overlayPositionStr) return parseWorldPt(overlayPositionStr);
    const pos= getMetaEntry(tableModel,MetaConst.OVERLAY_POSITION);
    if (pos) return parseWorldPt(pos);
    if (r.UserTargetWorldPt) return parseWorldPt(r.UserTargetWorldPt);
    if (r.QUERY) return extractCircleFromADQL(r.QUERY);
    if (r.source?.toLowerCase()?.includes('circle')) return extractCircleFromUrl(r.source);
}

function extractCircleFromUrl(url) {
    const params=new URL(url)?.searchParams;
    if (!params) return;
    if (params.has('ADQL')) return extractCircleFromADQL(params.get('ADQL'));
    if (params.has('POS')) return extractCircleFromPOS(params.get('POS'));
    const pts= [...params.entries()]
        .map(([,v]) => v)
        .filter( (v) => v.toLowerCase()?.includes('circle'))
        .map( (cStr) => extractCircleFromPOS(cStr))
        .filter( (wp) => wp);
    return (pts.length > 0) ? pts[0] : undefined;
}

function extractCircleFromPOS(circleStr) {
    const c= circleStr?.toLowerCase();
    if (!c?.startsWith('circle')) return;
    const cAry= c.split(' ').filter( (s) => s);
    const raNum= cAry[1];
    const decNum= cAry[2];
    if (isNaN(raNum) || isNaN(decNum)) return;
    return makeWorldPt(raNum,decNum);
}

function extractCircleFromADQL(adql) {
    const regEx= /CIRCLE\s?\(.*\)/;
    const result= regEx.exec(adql);
    if (!result) return;
    const circle= result[0];
    const parts= circle.split(',');
    if (parts.length<4) return;
    let cStr= parts[0].split('(')[1];
    if (!cStr) return;
    if (cStr.startsWith(`\'`) && cStr.endsWith(`\'`)) { // eslint-disable-line quotes
        cStr= cStr.substring(1, cStr.length-1) ;
    }
    if (!isNaN(Number(parts[1]))  && !isNaN(Number(parts[1]))) {
        return makeWorldPt(parts[1], parts[2], CoordinateSys.parse(cStr));
    }
}

export function findPlotViewUsingFitsPathMeta(table) {
    if (!table) return;
    const filePath= getMetaEntry(table,MetaConst.FITS_FILE_PATH);
    if (!filePath) return;
    const pvAry= getPlotViewAry(visRoot());
    return pvAry.find( (pv) => primePlot(pv)?.plotState.getWorkingFitsFileStr()===filePath);
}
