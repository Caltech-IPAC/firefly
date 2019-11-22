/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNil, get, isEmpty} from 'lodash';
import {TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_REMOVE,TABLE_UPDATE,TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import {SUBGROUP, dispatchAttachLayerToPlot, dispatchChangeVisibility, dispatchCreateDrawLayer,
        dispatchDestroyDrawLayer, dispatchModifyCustomField} from '../DrawLayerCntlr.js';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr.js';
import {getTblById, doFetchTable, isTableUsingRadians} from '../../tables/TableUtil.js';
import {cloneRequest, makeTableFunctionRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {serializeDecimateInfo} from '../../tables/Decimate.js';
import {getDrawLayerById, getPlotViewById} from '../PlotViewUtil.js';
import {dlRoot} from '../DrawLayerCntlr.js';
import Catalog from '../../drawingLayers/Catalog.js';
import {logError} from '../../util/WebUtil.js';
import {getMaxScatterRows} from '../../charts/ChartUtil.js';
import {isLsstFootprintTable} from '../task/LSSTFootprintTask.js';
import {parseWorldPt} from '../Point.js';
import {findTableCenterColumns, isCatalog} from '../../util/VOAnalyzer.js';
import {getAppOptions} from '../../core/AppDataCntlr';
import {makeWorldPt} from '../Point';
import {CoordinateSys} from '../CoordSys.js';
import {PlotAttribute} from '../PlotAttribute.js';
import SearchTarget from '../../drawingLayers/SearchTarget.js';
import {darker} from '../../util/Color.js';


/** @type {TableWatcherDef} */
export const catalogWatcherDef = {
    id : 'CatalogWatcher',
    watcher : watchCatalogs,
    testTable : (table) => !isLsstFootprintTable(table) && isCatalog(table),
    allowMultiples: false,
    actions: [TABLE_LOADED, TABLE_SELECT, TABLE_HIGHLIGHT, TABLE_UPDATE, TBL_RESULTS_ACTIVE,
              TABLE_REMOVE, ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_HIPS]
};


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

function handleCatalogUpdate(tbl_id) {
    const sourceTable= getTblById(tbl_id);
    if (!sourceTable || sourceTable.isFetching) return;
    const {totalRows, request, highlightedRow,selectInfo, title}= sourceTable;
    const maxScatterRows = getMaxScatterRows();
    const columns= findTableCenterColumns(sourceTable);

    // recenterImage(sourceTable);

    const params= {
        startIdx : 0,
        pageSize : MAX_ROW,
        inclCols : `"${columns.lonCol}","${columns.latCol}","ROW_IDX"`        // column names should be in quotes
    };

    let req = cloneRequest(sourceTable.request, params);
    let dataTooBigForSelection= false;
    if (totalRows > maxScatterRows) {
        const sreq = cloneRequest(sourceTable.request, {inclCols: `"${columns.lonCol}","${columns.latCol}"`});
        req = makeTableFunctionRequest(sreq, 'DecimateTable', title,
            {decimate: serializeDecimateInfo(columns.lonCol, columns.latCol, 10000), pageSize: MAX_ROW});
        dataTooBigForSelection= true;
    }

    req.tbl_id = `cat-${tbl_id}`;

    doFetchTable(req).then(
        (tableModel) => {
            if (tableModel.tableData) {
                updateDrawingLayer(tbl_id, tableModel,
                    request, highlightedRow, selectInfo, columns, dataTooBigForSelection);
            }
        }
    ).catch(
        (reason) => {
            logError(`Failed to catalog plot data: ${reason}`, reason);
        }
    );
}

function updateDrawingLayer(tbl_id, tableModel, tableRequest,
                            highlightedRow, selectInfo, columns, dataTooBigForSelection) {

    const plotIdAry= visRoot().plotViewAry.map( (pv) => pv.plotId);
    const {title, tableData, tableMeta}= tableModel;

    const dl= getDrawLayerById(dlRoot(),tbl_id);
    const {showCatalogSearchTarget}= getAppOptions();
    const searchTarget= showCatalogSearchTarget ? getSearchTarget(tableRequest) : undefined;
    if (dl) { // update drawing layer
        dispatchModifyCustomField(tbl_id, {title, tableData, tableMeta, tableRequest,
                                           highlightedRow, selectInfo, columns,
                                           dataTooBigForSelection});
    }
    else { // new drawing layer
        const angleInRadian= isTableUsingRadians(tableModel, [columns.lonCol,columns.latCol]);
        const catDL= dispatchCreateDrawLayer(Catalog.TYPE_ID,
            {catalogId:tbl_id, title, tableData, tableMeta, tableRequest, highlightedRow,
                                selectInfo, columns, dataTooBigForSelection, catalog:true,
                                layersPanelLayoutId: tbl_id,
                                angleInRadian});
        dispatchAttachLayerToPlot(tbl_id, plotIdAry);

        const newDL = dispatchCreateDrawLayer(SearchTarget.TYPE_ID,
            {
                drawLayerId: searchTargetId(tbl_id),
                color: darker(catDL.drawingDef.color),
                searchTargetWP: searchTarget,
                layersPanelLayoutId: tbl_id,
                titlePrefix: 'Catalog ',
                canUserDelete: true,
            });
        dispatchAttachLayerToPlot(newDL.drawLayerId, plotIdAry, false);



        const dl= getDrawLayerById(dlRoot(),tbl_id);
        if (dl.supportSubgroups  &&  dl.tableMeta[SUBGROUP]) {
            plotIdAry.map( (plotId) =>  getPlotViewById(visRoot(), plotId))
                .filter( (pv) => dl.tableMeta[SUBGROUP]!==get(pv, 'drawingSubGroupId'))
                .forEach( (pv) => pv && dispatchChangeVisibility({id:dl.drawLayerId, visible:false,
                                                                  plotId:pv.plotId, useGroup:false}));
        }
    }
}

function attachToCatalog(tbl_id, payload) {
    const {pvNewPlotInfoAry=[], wpRequest, wpRequestAry, redReq, blueReq, greenReq} = payload;
    const dl= getDrawLayerById(dlRoot(), tbl_id);
    if (!dl) return;
    pvNewPlotInfoAry.forEach( (info, idx) => {
        let r= wpRequest || get(wpRequestAry,idx);
        if (!r) r= (redReq || blueReq || greenReq);
        if (!r || r.getAttributes[PlotAttribute.DATALINK_TABLE_ID]===tbl_id) return; //Don't overlay catalogs on image data products
        dispatchAttachLayerToPlot(dl.drawLayerId, info.plotId);
        const pv= getPlotViewById(visRoot(), info.plotId);
        const pvSubGroup= get(pv, 'drawingSubGroupId');
        const tableSubGroup= dl.tableMeta[SUBGROUP];
        if (!isNil(pvSubGroup) && !isNil(tableSubGroup)  && pvSubGroup!==tableSubGroup) {
            pv && dispatchChangeVisibility({id:dl.drawLayerId, visible:false, plotId:pv.plotId, useGroup:false});
        }
    });
}


export function getSearchTarget(r, searchTargetStr, overlayPositionStr) {
    if (searchTargetStr) return parseWorldPt(searchTargetStr);
    if (overlayPositionStr) return parseWorldPt(overlayPositionStr);
    if (r.UserTargetWorldPt) return parseWorldPt(r.UserTargetWorldPt);
    if (!r.QUERY) return;
    const regEx= /CIRCLE\s?\(.*\)/;
    const result= regEx.exec(r.QUERY);
    if (!result) return;
    const circle= result[0];
    const parts= circle.split(',');
    if (parts.length!==4) return;
    let cStr= parts[0].split('(')[1];
    if (!cStr) return;
    if (cStr.startsWith(`\'`) && cStr.endsWith(`\'`)) {
       cStr= cStr.substring(1, cStr.length-1) ;
    }
    if (!isNaN(Number(parts[1]))  && !isNaN(Number(parts[1]))) {
        return makeWorldPt(parts[1], parts[2], CoordinateSys.parse(cStr))
    }

    
}


