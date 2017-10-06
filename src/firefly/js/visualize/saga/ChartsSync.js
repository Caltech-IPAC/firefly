/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';
import {isEqual} from 'lodash';

import * as TablesCntlr from '../../tables/TablesCntlr.js';
import * as TableStatsCntlr from '../../charts/TableStatsCntlr.js';
import * as TableUtil from '../../tables/TableUtil.js';
import * as ChartsCntlr from '../../charts/ChartsCntlr.js';

import {getDefaultXYPlotOptions, DT_XYCOLS} from '../../charts/dataTypes/XYColsCDT.js';
import {SCATTER, multitraceDesign} from '../../charts/ChartUtil.js';

import {PLOT2D, DEFAULT_PLOT2D_VIEWER_ID, dispatchAddViewerItems, dispatchRemoveViewerItems, dispatchUpdateCustom, getViewerItemIds, getMultiViewRoot} from '../../visualize/MultiViewCntlr.js';

/**
 * this saga handles chart related side effects
 */
export function* syncCharts() {
    while (true) {
        const action= yield take([ChartsCntlr.CHART_ADD, ChartsCntlr.CHART_MOUNTED, ChartsCntlr.CHART_REMOVE, TablesCntlr.TABLE_LOADED]);
        if (multitraceDesign()) {
            return;
        }
        switch (action.type) {
            case ChartsCntlr.CHART_ADD:
            case ChartsCntlr.CHART_MOUNTED:
            {
                const {chartId} = action.payload;
                ChartsCntlr.getRelatedTblIds(chartId).forEach((tblId) => {
                    if (TableUtil.isFullyLoaded(tblId)) {
                        if (!TableStatsCntlr.getColValStats(tblId) && ChartsCntlr.getNumCharts(tblId) === 1) {
                            TableStatsCntlr.dispatchLoadTblStats(TableUtil.getTblById(tblId)['request']);
                        }
                        ChartsCntlr.updateChartData(chartId, tblId);
                    }
                });
                break;
            }
            case ChartsCntlr.CHART_REMOVE:
            {
                const {chartId} = action.payload;
                dispatchRemoveViewerItems(DEFAULT_PLOT2D_VIEWER_ID, [chartId]);
                break;
            }
            case TablesCntlr.TABLE_LOADED:
                const {tbl_id} = action.payload;
                if (ChartsCntlr.getNumCharts(tbl_id, true)>0) {  // has related mounted charts
                    const {invokedBy} = action.payload;
                    if (invokedBy !== TablesCntlr.TABLE_SORT) {
                        TableStatsCntlr.dispatchLoadTblStats(TableUtil.getTblById(tbl_id)['request']);
                    }
                    ChartsCntlr.updateRelatedData(tbl_id, invokedBy);
                }
                break;
        }
    }
}

/**
 * This saga makes synchronizes the default chart viewer with the active table
 */
export function* syncChartViewer() {
    while (true) {
        const action = yield take([ChartsCntlr.CHART_ADD, TablesCntlr.TBL_RESULTS_ACTIVE]);
        if (multitraceDesign()) {
            return;
        }
        switch (action.type) {
            case ChartsCntlr.CHART_ADD:
            case TablesCntlr.TBL_RESULTS_ACTIVE:
                const {chartId, tbl_id} = action.payload;
                updateDefaultViewer(chartId, tbl_id);
                break;
        }
    }
}

/**
 * This saga adds a default chart
 */
export function* addDefaultScatter() {
    while (true) {
        const action = yield take([TablesCntlr.TABLE_LOADED]);
        if (multitraceDesign()) {
            return;
        }
        const {tbl_id} = action.payload;
        // check if a default chart needs to be added
        if (ChartsCntlr.getNumCharts(tbl_id) === 0) {
            // how do I know the default chart should be added?
            // add a default chart if the group is main
            if (TableUtil.getTableInGroup(tbl_id, 'main')) {
                // default chart is xy plot of coordinate columns or first two numeric columns
                const defaultOptions = getDefaultXYPlotOptions(tbl_id);
                if (defaultOptions) {
                    ChartsCntlr.dispatchChartAdd({
                        chartId: 'xyplot-' + tbl_id,
                        chartType: SCATTER,
                        groupId: tbl_id,
                        chartDataElements: [{tblId: tbl_id, type: DT_XYCOLS, options: defaultOptions}]
                    });
                }
            }
        }
    }
}


function updateDefaultViewer(chartId, active_tbl_id) {
    const tblId = active_tbl_id || TableUtil.getActiveTableId();
    const chartIds = [];
    chartIds.push(...ChartsCntlr.getChartIdsInGroup(tblId), ...ChartsCntlr.getChartIdsInGroup('default'));
    const currentIds = getViewerItemIds(getMultiViewRoot(),DEFAULT_PLOT2D_VIEWER_ID);
    if (!isEqual(chartIds, currentIds)) {
        const activeItemId =  chartIds.includes(chartId) ? chartId : undefined;
        dispatchUpdateCustom(DEFAULT_PLOT2D_VIEWER_ID, {activeItemId});
        dispatchRemoveViewerItems(DEFAULT_PLOT2D_VIEWER_ID,currentIds);
        dispatchAddViewerItems(DEFAULT_PLOT2D_VIEWER_ID, chartIds, PLOT2D);
    }
}