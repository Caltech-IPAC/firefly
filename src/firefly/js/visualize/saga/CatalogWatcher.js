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
import {getDrawLayerById, getPlotViewById, getActivePlotView,   findCurrentCenterPoint} from '../PlotViewUtil.js';
import {dlRoot} from '../DrawLayerCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import Catalog from '../../drawingLayers/Catalog.js';
import {logError} from '../../util/WebUtil.js';
import {getMaxScatterRows} from '../../charts/ChartUtil.js';
import {isLsstFootprintTable} from '../task/LSSTFootprintTask.js';
import {dispatchRecenter} from '../ImagePlotCntlr.js';
import {parseWorldPt, pointEquals, makeWorldPt} from '../Point.js';
import {computeCentralPointAndRadius} from '../VisUtil.js';
import CsysConverter from '../CsysConverter.js';
import {COVERAGE_CREATED} from './CoverageWatcher.js';
import {isImage,isHiPS} from '../WebPlot.js';
import {findTableCenterColumns} from '../../util/VOAnalyzer.js';





/** type {TableWatcherDef} */
export const catalogWatcherDef = {
    id : 'CatalogWatcher',
    watcher : watchCatalogs,
    testTable : testTableForCatalogs,
    actions: [TABLE_LOADED, TABLE_SELECT, TABLE_HIGHLIGHT, TABLE_UPDATE, TBL_RESULTS_ACTIVE,
              TABLE_REMOVE, ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_HIPS]
};


function testTableForCatalogs(table) {

    const {tableMeta, tableData}= table;
    if ( !tableMeta[MetaConst.CATALOG_OVERLAY_TYPE]) return false;
    if (isLsstFootprintTable(table)) return false;
    const columns= findTableCenterColumns(table);

    if (!columns) return false;
    if (!tableData.columns.find( isCName(columns.lonCol)) && !tableData.columns.find(isCName(columns.latCol))) {
        return false;
    }
    return true;

}


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


    if (isEmpty(visRoot().plotViewAry) && !tableManaged) {
        return {tableManaged: false, ...params}; // no plots? , don't start until there are plots
    }

    if (!action) {
        handleCatalogUpdate(tbl_id);
        return {tableManaged:true};
    }

    const {payload}= action;
    const {tableManaged}= params;
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
            cancelSelf();
            break;

        case TBL_RESULTS_ACTIVE:
            recenterImage(getTblById(tbl_id));
            break;

        case ImagePlotCntlr.PLOT_HIPS:
        case ImagePlotCntlr.PLOT_IMAGE:
            attachToCatalog(tbl_id, payload.pvNewPlotInfoAry);
            break;
    }
    return {tableManaged:true};
}


const isCName = (name) => (c) => c.name===name;

/**
 * update the projection center of hips plot to be aligned with the target of catalog search
 * @param tbl
 */
function recenterImage(tbl) {
    const pv = getActivePlotView(visRoot());
    const plot = pv && pv.plots[pv.primeIdx];

    // exclude coverage image
    if (!plot || get(plot, ['attributes', COVERAGE_CREATED], false)) {
        return;
    }

    const cc = CsysConverter.make(plot);
    const {UserTargetWorldPt, polygon} = tbl.request || {};
    const centerPt =  cc.getWorldCoords(findCurrentCenterPoint(pv));

    let   newCenter;

    if (UserTargetWorldPt) {    // search method: cone, elliptical, bo
        newCenter = parseWorldPt(UserTargetWorldPt);
    } else if (polygon) {       // search method polygon
        const allPts = polygon.trim().split(/\s+/);
        const pts = allPts.reduce((prevPts, pt_x, idx) => {
            if ((idx % 2 === 0) && ((idx + 1) < allPts.length)) {
                const wPt = makeWorldPt(parseFloat(pt_x), parseFloat(allPts[idx + 1]));

                prevPts.push(wPt);
            }
            return prevPts;
        }, []);

        const {centralPoint} = computeCentralPointAndRadius(pts);
        newCenter = centralPoint;
    }

    const allSky= Boolean( (isImage(plot) && plot.projection.isWrappingProjection()) || isHiPS(plot));
    // recenter image for 'hips' and allsky 'image' type
    if (newCenter && allSky && !pointEquals(centerPt, newCenter)) {
        dispatchRecenter({plotId: plot.plotId, centerPt: newCenter});
    }
}

function handleCatalogUpdate(tbl_id) {
    const sourceTable= getTblById(tbl_id);


    const {totalRows, request, highlightedRow,selectInfo, title}= sourceTable;
    const maxScatterRows = getMaxScatterRows();
    const columns= findTableCenterColumns(sourceTable);

    recenterImage(sourceTable);

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
            if (tableModel.tableData && tableModel.tableData.data) {
                updateDrawingLayer(tbl_id, tableModel.title,
                    tableModel.tableData, tableModel.tableMeta,
                    request, highlightedRow, selectInfo, columns, dataTooBigForSelection);
            }
        }
    ).catch(
        (reason) => {
            logError(`Failed to catalog plot data: ${reason}`, reason);
        }
    );
}

function updateDrawingLayer(tbl_id, title, tableData, tableMeta, tableRequest,
                            highlightedRow, selectInfo, columns, dataTooBigForSelection) {

    const plotIdAry= visRoot().plotViewAry.map( (pv) => pv.plotId);

    const dl= getDrawLayerById(dlRoot(),tbl_id);
    if (dl) { // update drawing layer
        dispatchModifyCustomField(tbl_id, {title, tableData, tableMeta, tableRequest,
                                           highlightedRow, selectInfo, columns,
                                           dataTooBigForSelection});
    }
    else { // new drawing layer
        const angleInRadian= isTableUsingRadians(tableMeta);
        dispatchCreateDrawLayer(Catalog.TYPE_ID,
            {catalogId:tbl_id, title, tableData, tableMeta, tableRequest, highlightedRow,
                                selectInfo, columns, dataTooBigForSelection, catalog:true,
                                angleInRadian});
        dispatchAttachLayerToPlot(tbl_id, plotIdAry);
        const dl= getDrawLayerById(dlRoot(),tbl_id);
        if (dl.supportSubgroups  &&  dl.tableMeta[SUBGROUP]) {
            plotIdAry.map( (plotId) =>  getPlotViewById(visRoot(), plotId))
                .filter( (pv) => dl.tableMeta[SUBGROUP]!==get(pv, 'drawingSubGroupId'))
                .forEach( (pv) => pv && dispatchChangeVisibility({id:dl.drawLayerId, visible:false,
                                                                  plotId:pv.plotId, useGroup:false}));
        }
    }
}

function attachToCatalog(tbl_id, pvNewPlotInfoAry=[]) {
    const dl= getDrawLayerById(dlRoot(), tbl_id);
    if (!dl) return;
    pvNewPlotInfoAry.forEach( (info) => {
        dispatchAttachLayerToPlot(dl.drawLayerId, info.plotId);
        const pv= getPlotViewById(visRoot(), info.plotId);
        const pvSubGroup= get(pv, 'drawingSubGroupId');
        const tableSubGroup= dl.tableMeta[SUBGROUP];
        if (!isNil(pvSubGroup) && !isNil(tableSubGroup)  && pvSubGroup!==tableSubGroup) {
            pv && dispatchChangeVisibility({id:dl.drawLayerId, visible:false, plotId:pv.plotId, useGroup:false});
        }
    });
}




