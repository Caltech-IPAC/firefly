/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {get, isEmpty, isNil, once} from 'lodash';
import {getAppOptions} from '../../core/AppDataCntlr';
import {MetaConst} from '../../data/MetaConst';
import Catalog, {CatalogType} from '../../drawingLayers/Catalog.js';
import HpxCatalog from '../../drawingLayers/hpx/HpxCatalog';
import SearchTarget from '../../drawingLayers/SearchTarget.js';
import {dispatchEnableHpxIndex, onOrderDataReady} from '../../tables/HpxIndexCntlr';
import {cloneRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {
    TABLE_HIGHLIGHT, TABLE_LOADED, TABLE_REMOVE, TABLE_SELECT, TBL_RESULTS_ACTIVE
} from '../../tables/TablesCntlr.js';
import {doFetchTable, getMetaEntry} from '../../tables/TableUtil';
import {getTblById} from '../../tables/TableUtil.js';
import {darker} from '../../util/Color.js';
import {logger} from '../../util/Logger';
import { findImageCenterColumns, getSearchTarget, isCatalog } from '../../voAnalyzer/TableAnalysis.js';
import {
    dispatchAttachLayerToPlot, dispatchChangeVisibility, dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchModifyCustomField, dlRoot, SUBGROUP,
} from '../DrawLayerCntlr.js';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {
    getDrawLayerById, getDrawLayersByType, getPlotViewAry, getPlotViewById, isDrawLayerAttached, isDrawLayerVisible,
    primePlot
} from '../PlotViewUtil.js';
import {isLsstFootprintTable} from '../task/LSSTFootprintTask.js';
import {coverageCatalogId} from './CoverageWatcher.js';


/** @type {TableWatcherDef} */
export const getCatalogWatcherDef= once(() => (
    {
        id : 'CatalogWatcher',
        watcher : watchCatalogs,
        testTable : (table) => !isLsstFootprintTable(table) && isCatalog(table),
        allowMultiples: false,
        actions: [TABLE_LOADED, TABLE_SELECT, TABLE_HIGHLIGHT, TBL_RESULTS_ACTIVE,
            TABLE_REMOVE, ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_HIPS]
    }
));

/**
 * @typedef {Object} PointType
 * @summary type of point
 * @prop WORLD
 * @prop IMAGE
 * @type {Enum}
 */
/** @type PointType */
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
 * @return {{tableManaged:boolean}}
 */
export function watchCatalogs(tbl_id, action, cancelSelf, params) {

    const {tableManaged}= params;
    if (isEmpty(visRoot().plotViewAry) && !tableManaged) {
        return {tableManaged: false, ...params}; // no plots? , don't start until there are plots
    }

    if (!action) {
        void handleCatalogUpdate(tbl_id);
        return {tableManaged:true};
    }

    const {payload}= action;
    if (payload.tbl_id && payload.tbl_id!==tbl_id) return params;

    if (!tableManaged && action.type!==TABLE_LOADED) {
        void handleCatalogUpdate(tbl_id);
    }
    const catalogId= catalogWatcherStandardCatalogId(tbl_id);
    const isImagePts= getCatalogPtType(getTblById(tbl_id))===PointType.IMAGE;

    switch (action.type) {
        case TABLE_LOADED:
            void handleCatalogUpdate(tbl_id);
            break;

        case TABLE_SELECT:
            if (isImagePts) dispatchModifyCustomField(catalogId, {selectInfo:payload.selectInfo});
            break;

        case TABLE_HIGHLIGHT:
            if (isImagePts) dispatchModifyCustomField(catalogId, {highlightedRow:payload.highlightedRow});
            break;

        case TABLE_REMOVE:
            dispatchDestroyDrawLayer(catalogId);
            dispatchDestroyDrawLayer(searchTargetId(tbl_id));
            cancelSelf();
            break;

        case ImagePlotCntlr.PLOT_HIPS:
        case ImagePlotCntlr.PLOT_IMAGE:
            attachToCatalog(tbl_id, payload);
            break;
    }
    return {tableManaged:true};
}


const searchTargetId= (tbl_id) => 'catalog_search-target-'+tbl_id;

/**
 * @param table
 * @returns {PointType}
 */
const getCatalogPtType= (table) =>
    getMetaEntry(table, MetaConst.CATALOG_OVERLAY_TYPE)?.toUpperCase()==='IMAGE_PTS' ? PointType.IMAGE : PointType.WORLD;

async function handleCatalogUpdate(tbl_id) {
    const sourceTable= getTblById(tbl_id);
    if (!sourceTable || sourceTable.isFetching) return;
    const {request, highlightedRow,selectInfo}= sourceTable;

    if (getCatalogPtType(sourceTable)===PointType.WORLD) {
        dispatchEnableHpxIndex({tbl_id});
        await onOrderDataReady(tbl_id);
        updateHpxCatalogDrawingLayer(tbl_id, request, highlightedRow, selectInfo);
    }
    else {
        // plotting PointType.IMAGE is only set up from extraction
        const columns= findImageCenterColumns(sourceTable);
        if (!findPlotViewUsingFitsPathMeta(sourceTable)) return;
        const params= { startIdx : 0, pageSize : MAX_ROW, inclCols : `"${columns.xCol}","${columns.yCol}","ROW_IDX"`};
        const req = cloneRequest(sourceTable.request, params);
        req.tbl_id = `cat-extracted-image-pts-${tbl_id}`;
        try {
            const tableModel= await doFetchTable(req);
            if (!tableModel?.tableData) return;
            updateImagePointsDrawingLayer(tbl_id,tableModel,request,highlightedRow,selectInfo);
        }
        catch (reason) {
            logger.error(`Failed to catalog plot data: ${reason}`, reason);
        }

    }
}

export const catalogWatcherStandardCatalogId= (tbl_id) => tbl_id+'--'+'catalog-watcher';


function updateImagePointsDrawingLayer(tbl_id, allRowsTable, tableRequest, highlightedRow, selectInfo) {

    const catalogId= catalogWatcherStandardCatalogId(tbl_id);
    const dl= getDrawLayerById(dlRoot(),catalogId);

    if (dl) { // update drawing layer
        dispatchModifyCustomField(catalogId, {title:allRowsTable.title, highlightedRow, selectInfo});
        return;
    }
    const columns= findImageCenterColumns(tbl_id);
    const pv=  findPlotViewUsingFitsPathMeta(getTblById(tbl_id));
    if (!pv || !columns) return;

    dispatchCreateDrawLayer(Catalog.TYPE_ID,
        {catalogId, title:allRowsTable.title, tableData:allRowsTable.tableData, tableMeta:allRowsTable.tableMeta,
            tableRequest, highlightedRow, selectInfo, columns, catalogType:CatalogType.POINT_IMAGE_PT,
            tbl_id, layersPanelLayoutId: tbl_id });
    attachToPlot(catalogId, [pv.plotId]);
}


function updateHpxCatalogDrawingLayer(tbl_id, tableRequest, highlightedRow) {

    const table= getTblById(tbl_id);
    const {title}= table;
    const catalogId= catalogWatcherStandardCatalogId(tbl_id);
    let dl= getDrawLayerById(dlRoot(),catalogId);
    if (dl) { // update drawing layer
        dispatchModifyCustomField(catalogId, {title, highlightedRow});
        return;
    }

    const color= getDrawLayersByType(dlRoot(),HpxCatalog.TYPE_ID) // fine any other draw layers with this table and set the color
        ?.find( (dl) => dl.tbl_id===tbl_id && dl.drawLayerId===coverageCatalogId(tbl_id))?.drawingDef.color;

    const catDL= dispatchCreateDrawLayer(HpxCatalog.TYPE_ID,
        {catalogId, tbl_id, title, highlightedRow, color, layersPanelLayoutId: catalogId});
    const plotIdAry= visRoot().plotViewAry
        .filter( (pv) => pv.plotViewCtx.useForSearchResults)
        .map( (pv) => pv.plotId);
    attachToPlot(catalogId,plotIdAry);

    const {showCatalogSearchTarget}= getAppOptions();
    const searchTarget= showCatalogSearchTarget ? getSearchTarget(tableRequest,table) : undefined;
    if (searchTarget) {
        const newDL = dispatchCreateDrawLayer(SearchTarget.TYPE_ID,
            {
                drawLayerId: searchTargetId(tbl_id),
                color: darker(catDL.drawingDef.color),
                searchTargetPoint: searchTarget,
                layersPanelLayoutId: catalogId,
                titlePrefix: 'Catalog ',
                canUserDelete: true,
            });
        attachToPlot(newDL.drawLayerId,plotIdAry);
    }
    dl= getDrawLayerById(dlRoot(),catalogId);

    const subgroup= getTblById(dl.tbl_id)?.tableMeta?.[SUBGROUP];
    if (dl.supportSubgroups  &&  subgroup) {
        plotIdAry.map( (plotId) =>  getPlotViewById(visRoot(), plotId))
            .filter( (pv) => subgroup!==pv?.drawingSubGroupId)
            .forEach( (pv) => {
                pv && dispatchChangeVisibility({id:dl.drawLayerId, visible:false,
                    plotId:pv.plotId, useGroup:false});
            });
    }
}

function attachToPlot(drawLayerId, plotIdAry=[]) {
    const attachAry= plotIdAry.filter( (pId) => {
        const pv= getPlotViewById(visRoot(),pId);
        return (pv && !pv?.plotViewCtx?.useForCoverage);
    });
    dispatchAttachLayerToPlot(drawLayerId, attachAry, false);
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
    const table= getTblById(tbl_id);
    if (getCatalogPtType(table)===PointType.IMAGE) return;
    const {pvNewPlotInfoAry=[], wpRequest, wpRequestAry, redReq, blueReq, greenReq} = payload;
    const catId= catalogWatcherStandardCatalogId(tbl_id);
    const dl= getDrawLayerById(dlRoot(), catId);
    if (!dl || !table) return;
    pvNewPlotInfoAry.forEach( (info, idx) => {
        let r= wpRequest || get(wpRequestAry,idx);
        if (!r) r= (redReq || blueReq || greenReq);
        if (!r || r.getAttributes[PlotAttribute.RELATED_TABLE_ID]===tbl_id) return; //Don't overlay catalogs on image data products
        const pv= getPlotViewById(visRoot(), info.plotId);
        if (pv?.plotViewCtx?.useForCoverage) return;
        dispatchAttachLayerToPlot(dl.drawLayerId, info.plotId,false, shouldDlBeVisible(dl,pv));
        const pvSubGroup= pv?.drawingSubGroupId;
        const tableSubGroup= getTblById(tbl_id)?.tableMeta[SUBGROUP];
        if (!isNil(pvSubGroup) && !isNil(tableSubGroup)  && pvSubGroup!==tableSubGroup) {
            pv && dispatchChangeVisibility({id:dl.drawLayerId, visible:false, plotId:pv.plotId, useGroup:false});
        }
    });
}


export function findPlotViewUsingFitsPathMeta(table) {
    if (!table) return;
    const filePath= getMetaEntry(table,MetaConst.FITS_FILE_PATH);
    if (!filePath) return;
    const pvAry= getPlotViewAry(visRoot());
    return pvAry.find( (pv) => primePlot(pv)?.plotState.getWorkingFitsFileStr()===filePath);
}
