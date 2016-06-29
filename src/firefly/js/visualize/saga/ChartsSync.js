/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';

import {has} from 'lodash';

import {flux} from '../../Firefly.js';

import * as TablesCntlr from '../../tables/TablesCntlr.js';
import * as TableStatsCntlr from '../TableStatsCntlr.js';
import * as TableUtil from '../../tables/TableUtil.js';

import * as XYPlotCntlr from '../XYPlotCntlr.js';
import * as HistogramCntlr from '../HistogramCntlr.js';
import {hasRelatedCharts, getDefaultXYPlotParams} from '../ChartUtil.js';

/**
 * this saga handles chart related side effects
 */
export function* syncCharts() {
    var tableStatsState, xyPlotState, histogramState;

    while (true) {
        const action= yield take([TableStatsCntlr.SETUP_TBL_TRACKING, TablesCntlr.TABLE_NEW_LOADED, TablesCntlr.TABLE_SORT]);
        const request= action.payload.request;
        switch (action.type) {
            case TableStatsCntlr.SETUP_TBL_TRACKING:
                const {tblId} = action.payload;
                if (TableUtil.isFullyLoaded(tblId)) {
                    TableStatsCntlr.dispatchLoadTblStats(TableUtil.getTblById(tblId)['request']);
                    if (!hasRelatedCharts(tblId)) {
                        // default chart is xy plot of coordinate columns or first two numeric columns
                        const defaultParams = getDefaultXYPlotParams(tblId);
                        if (defaultParams) {
                            XYPlotCntlr.dispatchLoadPlotData(tblId, defaultParams, tblId);
                        }
                    }
                }
                break;
            case TablesCntlr.TABLE_SORT:
            case TablesCntlr.TABLE_NEW_LOADED:
                const {tbl_id} = action.payload;
                let hasRelated = false;

                xyPlotState = flux.getState()[XYPlotCntlr.XYPLOT_DATA_KEY];
                Object.keys(xyPlotState).forEach((cid) => {
                    if (xyPlotState[cid].tblId === tbl_id) {
                        const xyPlotParams = xyPlotState[cid].xyPlotParams;
                        XYPlotCntlr.dispatchLoadPlotData(cid, xyPlotParams, tbl_id);
                        hasRelated = true;
                    }
                });

                // table statistics and histogram data do not change on table sort
                if (action.type !== TablesCntlr.TABLE_SORT) {
                    histogramState = flux.getState()[HistogramCntlr.HISTOGRAM_DATA_KEY];
                    Object.keys(histogramState).forEach((cid) => {
                        if (histogramState[cid].tblId === tbl_id) {
                            const histogramParams = histogramState[cid].histogramParams;
                            HistogramCntlr.dispatchLoadColData(cid, histogramParams, tbl_id);
                            hasRelated = true;
                        }
                    });

                    tableStatsState = flux.getState()[TableStatsCntlr.TBLSTATS_DATA_KEY];
                    if (has(tableStatsState, tbl_id)) {
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



