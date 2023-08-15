/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {filter, isEmpty, isArray, uniq, once} from 'lodash';

import {startCoverageWatcher} from '../../visualize/saga/CoverageWatcher.js';

import {LO_VIEW, SHOW_DROPDOWN, SET_LAYOUT_MODE, ENABLE_SPECIAL_VIEWER, SPECIAL_VIEWER,
          getLayouInfo, dispatchUpdateLayoutInfo,
          dispatchAddCell, dispatchRemoveCell, getNextCell, getGridView} from '../../core/LayoutCntlr.js';
import {findGroupByTblId, getTblIdsByGroup, getTblById} from '../../tables/TableUtil.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TABLE_REMOVE, TBL_RESULTS_ACTIVE, TABLE_SORT} from '../../tables/TablesCntlr.js';
import {dispatchLoadTblStats} from '../../charts/TableStatsCntlr';

import {CHART_ADD, CHART_REMOVE} from '../../charts/ChartsCntlr.js';
import {clone} from '../../util/WebUtil.js';

import ImagePlotCntlr from '../../visualize/ImagePlotCntlr.js';
import {REPLACE_VIEWER_ITEMS, IMAGE, getViewerItemIds, getMultiViewRoot, getViewer, findViewerWithItemId} from '../../visualize/MultiViewCntlr.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../../core/MasterSaga.js';


export function startLayoutManager(id, params) {

    const actions = [
        ImagePlotCntlr.PLOT_IMAGE_START, ImagePlotCntlr.PLOT_IMAGE,
        ImagePlotCntlr.DELETE_PLOT_VIEW, REPLACE_VIEWER_ITEMS,
        TABLE_REMOVE, TABLE_LOADED, TBL_RESULTS_ADDED, TBL_RESULTS_ACTIVE,
        CHART_ADD, CHART_REMOVE,
        SHOW_DROPDOWN, SET_LAYOUT_MODE, ENABLE_SPECIAL_VIEWER
    ];
    dispatchAddActionWatcher({id, actions, callback: layoutManager, params});
    const stopLayoutManger= () => dispatchCancelActionWatcher(id);
    return stopLayoutManger;
}




/**
 * this manager manages what main components get display on the screen.
 * These main components are image plots, charts, tables, dropdown panel, etc.
 * This manager implements the default firefly viewer's requirements.
 * Because it may differs between applications, it is okay to have a custom layout manager if needed.
 * @param {Action} action
 * @param {Function} cancelSelf
 * @param {Object} params
 */
function layoutManager(action, cancelSelf, params) {

    let {alreadyStartSagas= [], renderTreeId}= params;

    const layoutInfo = getLayouInfo();
    let newLayoutInfo = layoutInfo;

    switch (action.type) {
        case ImagePlotCntlr.PLOT_IMAGE_START:
        case ImagePlotCntlr.PLOT_IMAGE :
        case REPLACE_VIEWER_ITEMS:

            newLayoutInfo = handleNewImage(newLayoutInfo, action, renderTreeId);
            break;
        case ImagePlotCntlr.DELETE_PLOT_VIEW:
            newLayoutInfo = handlePlotDelete(newLayoutInfo, action, renderTreeId);
            break;
        case TBL_RESULTS_ADDED:
            newLayoutInfo = handleNewTable(newLayoutInfo, action, renderTreeId, params.groupIgnoreFilter);
            break;
        case TABLE_LOADED:
            newLayoutInfo = handleTableLoaded(newLayoutInfo, action, renderTreeId, params.groupIgnoreFilter);
            break;
        case TABLE_REMOVE:
            newLayoutInfo = handleTableDelete(newLayoutInfo, action, renderTreeId, params.groupIgnoreFilter);
            break;
        case CHART_ADD:
            newLayoutInfo = handleNewChart(newLayoutInfo, action, renderTreeId);
            break;
        case CHART_REMOVE:
            newLayoutInfo = handleChartDelete(newLayoutInfo, action, renderTreeId);
            break;
        case ENABLE_SPECIAL_VIEWER:
            const startCellId= startSpecialViewerSaga(action, alreadyStartSagas, renderTreeId);
            if (startCellId) alreadyStartSagas= [...alreadyStartSagas,startCellId];
            break;
    }
    if (newLayoutInfo !== layoutInfo) {
        dispatchUpdateLayoutInfo(newLayoutInfo);
    }
    return clone(params, {alreadyStartSagas});
}


function startSpecialViewerSaga(action, alreadyStarted) {
    const {cellId}= action.payload;

    if (alreadyStarted.includes(cellId)) return undefined;

    const viewerType= SPECIAL_VIEWER.get(action.payload.viewerType);
    switch (viewerType) {
        case SPECIAL_VIEWER.coverageImage:
            startCoverageWatcher({viewerId:cellId, ignoreCatalogs:true, paused:false});
            break;

    }
    return cellId;
}

function handleNewTable(layoutInfo, action, renderTreeId, groupIgnoreFilter) {
    const {tbl_id} = action.payload;
    const gridView= getGridView(layoutInfo, renderTreeId);
    const tbl_group= findGroupByTblId(tbl_id);
    if (tbl_group) {
        const item= gridView.find( (g) => g.cellId===tbl_group);
        if (!item && !tbl_group.includes(groupIgnoreFilter)) {
            const cell= getNextCell(gridView,2,1);
            dispatchAddCell({row:cell.row,col:cell.col,width:2,height:1,cellId:tbl_group,type:LO_VIEW.tables, renderTreeId});
        }
    }
    return layoutInfo;
}

function handleTableLoaded(layoutInfo, action, renderTreeId, groupIgnoreFilter) {
    const {tbl_id, invokedBy} = action.payload;
    const table= getTblById(tbl_id);
    if (table && invokedBy !== TABLE_SORT &&  !table.origTableModel) {
        const tbl_group= findGroupByTblId(tbl_id);
        if (tbl_group && !tbl_group.includes(groupIgnoreFilter)) {
            dispatchLoadTblStats(table.request);
        }
    }
}

function handleTableDelete(layoutInfo, action, renderTreeId, groupIgnoreFilter) {
    const {tbl_group}= action.payload;
    const tblIdAry= getTblIdsByGroup(tbl_group);
    if (tbl_group && isEmpty(tblIdAry)) {
        if (tbl_group.includes(groupIgnoreFilter)) return;
        dispatchRemoveCell({cellId:tbl_group, renderTreeId});
    }
}

function handlePlotDelete(layoutInfo, action, renderTreeId) {
    const {viewerId}= action.payload;
    const itemAry= getViewerItemIds(getMultiViewRoot(), viewerId);
    if (isEmpty(itemAry)) {
        const viewer=  getViewer(getMultiViewRoot(),viewerId);
        if (!viewer.internallyManaged) dispatchRemoveCell({cellId:viewerId, renderTreeId});
    }
}

function handleChartDelete(layoutInfo, action, renderTreeId) {
    const {viewerId}= action.payload;
    const itemAry= getViewerItemIds(getMultiViewRoot(), viewerId);
    if (isEmpty(itemAry)) {
        const viewer=  getViewer(getMultiViewRoot(),viewerId);
        if (!viewer.internallyManaged) dispatchRemoveCell({cellId:viewerId, renderTreeId});
    }
}


function handleNewImage(layoutInfo, action, renderTreeId) {
    const {payload}= action;
    const gridView= getGridView(layoutInfo, renderTreeId);
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
        const viewerId= findViewerWithItemId(mvRoot, plotId, IMAGE);
        const viewer= viewerId && getViewer(mvRoot, viewerId);
        // console.log(`handleNewImage: ${renderTreeId}: v: ${viewer && viewer.renderTreeId}, p: ${payload.renderTreeId}`);
        if (!viewer || viewer.internallyManaged) return;
        if (isUnmatchingLayout(renderTreeId,viewer, payload)) return layoutInfo;
        if (viewer.customData.independentLayout) return;
        
        const item= gridView.find( (g) => g.cellId===viewer.viewerId);
        if (!item) {
            const cell= getNextCell(gridView,2,2);
            dispatchAddCell({row:cell.row,col:cell.col,width:2,height:2,cellId:viewer.viewerId,type:LO_VIEW.images, renderTreeId});
        }
    });

    return layoutInfo;
}


function isUnmatchingLayout(renderTreeId, viewer, payload) {
    if (!renderTreeId) return false;

    if ((viewer.renderTreeId && renderTreeId!==viewer.renderTreeId) ||
        (payload.renderTreeId && renderTreeId!==payload.renderTreeId))  {
        return true;
    }
    return false;
}




function handleNewChart(layoutInfo, action, renderTreeId) {

    const {payload}= action;
    const {viewerId}= payload;
    const gridView= getGridView(layoutInfo, renderTreeId);

    const item= gridView.find( (g) => g.cellId===viewerId);
    if (renderTreeId && payload.renderTreeId && renderTreeId!==payload.renderTreeId) {
        return layoutInfo;
    }
    
    if (!item) {
        const cell= getNextCell(gridView,2,2);
        const viewer= viewerId && getViewer(getMultiViewRoot(), viewerId);
        if (viewer || viewer?.internallyManaged) return;

        dispatchAddCell({row:cell.row,col:cell.col,width:2,height:2,cellId:viewerId,type:LO_VIEW.xyPlots, renderTreeId});
    }

    return layoutInfo;
}

