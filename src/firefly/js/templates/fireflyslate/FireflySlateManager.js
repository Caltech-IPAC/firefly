/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {filter, isEmpty, get, isArray, uniq} from 'lodash';

import {startImageMetadataWatcher} from '../../visualize/saga/ImageMetaDataWatcher.js';
import {startCoverageWatcher} from '../../visualize/saga/CoverageWatcher.js';

import {LO_VIEW, SHOW_DROPDOWN, SET_LAYOUT_MODE, ENABLE_SPECIAL_VIEWER, SPECIAL_VIEWER,
          getLayouInfo, dispatchUpdateLayoutInfo, dispatchAddCell, dispatchRemoveCell, getNextCell} from '../../core/LayoutCntlr.js';
import {findGroupByTblId, getTblIdsByGroup, getTblById} from '../../tables/TableUtil.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TABLE_REMOVE, TBL_RESULTS_ACTIVE, TABLE_SORT} from '../../tables/TablesCntlr.js';
import {dispatchLoadTblStats} from '../../charts/TableStatsCntlr';

import {CHART_ADD, CHART_REMOVE} from '../../charts/ChartsCntlr.js';

import ImagePlotCntlr from '../../visualize/ImagePlotCntlr.js';
import {REPLACE_VIEWER_ITEMS, IMAGE, getViewerItemIds, getMultiViewRoot, findViewerWithItemId} from '../../visualize/MultiViewCntlr.js';
import {getAppOptions} from '../../core/AppDataCntlr.js';

/**
 * this manager manages what main components get display on the screen.
 * These main components are image plots, charts, tables, dropdown panel, etc.
 * This manager implements the default firefly viewer's requirements.
 * Because it may differs between applications, it is okay to have a custom layout manager if needed.
 * @param {object} p
 * @param {string} [p.title] title to display
 * @param {string} [p.views] defaults to tri-view if not given.
 */
export function* layoutManager({title}) {

    let alreadyStartSagas= [];

    while (true) {
        const action = yield take([
            ImagePlotCntlr.PLOT_IMAGE_START, ImagePlotCntlr.PLOT_IMAGE,
            ImagePlotCntlr.DELETE_PLOT_VIEW, REPLACE_VIEWER_ITEMS,
            TABLE_REMOVE, TABLE_LOADED, TBL_RESULTS_ADDED, TBL_RESULTS_ACTIVE,
            CHART_ADD, CHART_REMOVE,
            SHOW_DROPDOWN, SET_LAYOUT_MODE, ENABLE_SPECIAL_VIEWER
        ]);

        /**
         * This is the current state of the layout store.  Action handlers should return newLayoutInfo if state changes
         * If state has changed, it will be dispatched into the flux.
         * @type {LayoutInfo}
         * @prop {boolean}  layoutInfo.showTables  show tables panel
         * @prop {boolean}  layoutInfo.showXyPlots show charts panel
         * @prop {boolean}  layoutInfo.showImages  show images panel
         * @prop {string}   layoutInfo.searchDesc  optional string describing search criteria used to generate this result.
         * @prop {boolean}  layoutInfo.autoExpand  this is true when manager think it should be expanded, ie. single view
         * @prop {Object}   layoutInfo.images      images specific states
         * @prop {string}   layoutInfo.images.metaTableId  tbl_id of the image meta table
         * @prop {string}   layoutInfo.images.selectedTab  selected tab of the images tabpanel
         * @prop {string}   layoutInfo.images.showCoverage  show images coverage tab
         * @prop {string}   layoutInfo.images.showFits  show images fits data tab
         * @prop {string}   layoutInfo.images.showMeta  show images image metea tab
         * @prop {string}   layoutInfo.images.coverageLockedOn
         */
        var layoutInfo = getLayouInfo();
        var newLayoutInfo = layoutInfo;

        switch (action.type) {
            case ImagePlotCntlr.PLOT_IMAGE_START:
            case ImagePlotCntlr.PLOT_IMAGE :
            case REPLACE_VIEWER_ITEMS:

                newLayoutInfo = handleNewImage(newLayoutInfo, action);
                break;
            case ImagePlotCntlr.DELETE_PLOT_VIEW:
                newLayoutInfo = handlePlotDelete(newLayoutInfo, action);
                break;
            case TBL_RESULTS_ADDED:
                newLayoutInfo = handleNewTable(newLayoutInfo, action);
                break;
            case TABLE_LOADED:
                newLayoutInfo = handleTableLoaded(newLayoutInfo, action);
                break;
            case TABLE_REMOVE:
                newLayoutInfo = handleTableDelete(newLayoutInfo, action);
                break;
            case CHART_ADD:
                newLayoutInfo = handleNewChart(newLayoutInfo, action);
                break;
            case CHART_REMOVE:
                newLayoutInfo = handleChartDelete(newLayoutInfo, action);
                break;
            case ENABLE_SPECIAL_VIEWER:
                const startCellId= startSpecialViewerSaga(action, alreadyStartSagas);
                if (startCellId) alreadyStartSagas= [...alreadyStartSagas,startCellId];
                break;
        }


        if (newLayoutInfo !== layoutInfo) {
            dispatchUpdateLayoutInfo(newLayoutInfo);
        }
    }
}


function startSpecialViewerSaga(action, alreadyStarted) {
    const {cellId}= action.payload;

    if (alreadyStarted.includes(cellId)) return undefined;

    const viewerType= SPECIAL_VIEWER.get(action.payload.viewerType);
    switch (viewerType) {

        case SPECIAL_VIEWER.tableImageMeta:
            startImageMetadataWatcher({viewerId: cellId, paused:false});
            break;
        case SPECIAL_VIEWER.coverageImage:
            startCoverageWatcher({viewerId:cellId, ignoreCatalogs:true, paused:false});
            break;

    }
    return cellId;
}

function handleNewTable(layoutInfo, action) {
    const {tbl_id} = action.payload;
    const {gridView=[]}= layoutInfo;
    const tbl_group= findGroupByTblId(tbl_id);
    if (tbl_group) {
        const item= gridView.find( (g) => g.cellId===tbl_group);
        if (!item) {
            const cell= getNextCell(gridView,2,1);
            dispatchAddCell({row:cell.row,col:cell.col,width:2,height:1,cellId:tbl_group,type:LO_VIEW.tables});
        }
    }
    return layoutInfo;
}

function handleTableLoaded(layoutInfo, action) {
    const {tbl_id, invokedBy} = action.payload;
    const table= getTblById(tbl_id);
    if (table && invokedBy !== TABLE_SORT &&  !table.origTableModel) {
        dispatchLoadTblStats(table.request);
    }
}

function handleTableDelete(layoutInfo, action) {
    const {tbl_group}= action.payload;
    const tblIdAry= getTblIdsByGroup(tbl_group);
    if (tbl_group && isEmpty(tblIdAry)) {
        dispatchRemoveCell({cellId:tbl_group});
    }
}

function handlePlotDelete(layoutInfo, action) {
    const {viewerId}= action.payload;
    const itemAry= getViewerItemIds(getMultiViewRoot(), viewerId);
    if (isEmpty(itemAry)) {
        dispatchRemoveCell({cellId:viewerId});
    }
}

function handleChartDelete(layoutInfo, action) {
    const {viewerId}= action.payload;
    const itemAry= getViewerItemIds(getMultiViewRoot(), viewerId);
    if (isEmpty(itemAry)) {
        dispatchRemoveCell({cellId:viewerId});
    }
}


function handleNewImage(layoutInfo, action) {
    const {payload}= action;
    const {gridView=[]}= layoutInfo;
    const mvRoot= getMultiViewRoot();
    let wpRequestAry;
    if (payload.wpRequestAry) { // multi image case
        wpRequestAry= payload.wpRequestAry;
    }
    else if (isArray(payload.wpRequest)) {  // 3 color case
        wpRequestAry= [payload.wpRequest[0]];
    }
    else if (payload.redReq || payload.greenReq || payload.blueReq) {  // another 3 color case
        wpRequestAry= filter([payload.redReq || payload.greenReq || payload.blueReq]);
    }
    else if (payload.wpRequest) { // single plot case
        wpRequestAry= [payload.wpRequest];
    }

    if (isEmpty(wpRequestAry)) return layoutInfo;
    const plotIdAry= uniq(wpRequestAry.map( (r) => r.getPlotId() ));

    plotIdAry.forEach( (plotId) => {
        const viewer= findViewerWithItemId(mvRoot, plotId, IMAGE);
        if (viewer) {
             const item= gridView.find( (g) => g.cellId===viewer);
             if (!item) {
                 const cell= getNextCell(gridView,2,2);
                 dispatchAddCell({row:cell.row,col:cell.col,width:2,height:2,cellId:viewer,type:LO_VIEW.images});
             }
        }
    });

    return layoutInfo;
}

function handleNewChart(layoutInfo, action) {

    const {viewerId}= action.payload;
    const {gridView=[]}= layoutInfo;

    const item= gridView.find( (g) => g.cellId===viewerId);
    if (!item) {
        const cell= getNextCell(gridView,3,1);
        dispatchAddCell({row:cell.row,col:cell.col,width:1,height:1,cellId:viewerId,type:LO_VIEW.xyPlots});
    }

    return layoutInfo;
}

