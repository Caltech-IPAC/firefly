/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';

import {get} from 'lodash';

import {logError} from '../../util/WebUtil.js';

import * as TablesCntlr from '../../tables/TablesCntlr.js';
import * as TableStatsCntlr from '../../charts/TableStatsCntlr.js';
import * as TableUtil from '../../tables/TableUtil.js';

import * as XYPlotCntlr from '../../charts/XYPlotCntlr.js';
import * as ChartsCntlr from '../../charts/ChartsCntlr.js';
import * as HistogramCntlr from '../../charts/HistogramCntlr.js';
import {SCATTER, HISTOGRAM, getChartSpace, hasRelatedCharts, getDefaultXYPlotParams} from '../../charts/ChartUtil.js';

/**
 * this saga handles chart related side effects
 */
export function* syncCharts() {
    var xyPlotState, histogramState;

    while (true) {
        const action= yield take([ChartsCntlr.CHART_MOUNTED, TablesCntlr.TABLE_LOADED]);
        if (!ChartsCntlr.getNumRelatedCharts()) { continue; }
        const request= action.payload.request;
        switch (action.type) {
            case ChartsCntlr.CHART_MOUNTED:
                const {tblId, chartId, chartType} = action.payload;
                if (TableUtil.isFullyLoaded(tblId)) {
                    if (ChartsCntlr.getNumRelatedCharts(tblId)===1) {
                        TableStatsCntlr.dispatchLoadTblStats(TableUtil.getTblById(tblId)['request']);
                        if (!hasRelatedCharts(tblId)) {
                            // default chart is xy plot of coordinate columns or first two numeric columns
                            const defaultParams = getDefaultXYPlotParams(tblId);
                            if (defaultParams) {
                                XYPlotCntlr.dispatchLoadPlotData(tblId, defaultParams, tblId);
                            }
                        }
                    }
                    updateChartDataIfNeeded(tblId, chartId, chartType);
                }

                break;
            case TablesCntlr.TABLE_LOADED:
                const {tbl_id} = action.payload;

                // check if there are any mounted charts related to this table
                if (ChartsCntlr.getNumRelatedCharts(tbl_id, true) > 0) {
                    let hasRelated = false;
                    xyPlotState = getChartSpace(SCATTER);
                    Object.keys(xyPlotState).forEach((cid) => {
                        if (xyPlotState[cid].tblId === tbl_id) {
                            hasRelated = true;
                            if (ChartsCntlr.isChartMounted(tbl_id, cid, SCATTER)) {
                                const xyPlotParams = xyPlotState[cid].xyPlotParams;
                                XYPlotCntlr.dispatchLoadPlotData(cid, xyPlotParams, tbl_id);
                            }
                        }
                    });

                    // table statistics and histogram data do not change on table sort
                    const {invokedBy} = action.payload;
                    if (invokedBy !== TablesCntlr.TABLE_SORT) {
                        histogramState = getChartSpace(HISTOGRAM);
                        Object.keys(histogramState).forEach((cid) => {
                            if (histogramState[cid].tblId === tbl_id) {
                                hasRelated = true;
                                if (ChartsCntlr.isChartMounted(tbl_id, cid, 'histogram')) {
                                    const histogramParams = histogramState[cid].histogramParams;
                                    HistogramCntlr.dispatchLoadColData(cid, histogramParams, tbl_id);
                                }
                            }
                        });

                        TableStatsCntlr.dispatchLoadTblStats(request);
                        if (!hasRelated) {
                            // default chart is xy plot of coordinate columns or first two numeric columns
                            const defaultParams = getDefaultXYPlotParams(tbl_id);
                            if (defaultParams) {
                                XYPlotCntlr.dispatchLoadPlotData(tbl_id, defaultParams, tbl_id);
                            }
                        }
                    }
                }
                break;
        }
    }
}



function updateChartDataIfNeeded(tblId, chartId, chartType) {
    const tblSource = get(TableUtil.getTblById(tblId), 'tableMeta.tblFilePath');
    const chartSpace = getChartSpace(chartType);
    const chartTblSource = get(chartSpace,[chartId,'tblSource']);
    if (tblSource && (!chartTblSource || chartTblSource !== tblSource)) {
        switch(chartType) {
            case SCATTER:
                const xyPlotParams = get(chartSpace, [chartId, 'xyPlotParams']);
                if (xyPlotParams) {
                    XYPlotCntlr.dispatchLoadPlotData(chartId, xyPlotParams, tblId);
                }
                break;
            case HISTOGRAM:
                const histogramParams = get(chartSpace, [chartId, 'histogramParams']);
                if (histogramParams) {
                    HistogramCntlr.dispatchLoadColData(chartId, histogramParams, tblId);
                }
                break;
            default:
                logError(`Unknown chart type ${chartType}`);
        }
    }
}
