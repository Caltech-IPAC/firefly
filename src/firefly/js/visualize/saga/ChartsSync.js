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



/**
 * this saga handles chart related side effects
 */
export function* syncCharts() {
    var tableStatsState, xyPlotState, histogramState;

    while (true) {
        const action= yield take([TableStatsCntlr.SETUP_TBL_TRACKING, TablesCntlr.TABLE_NEW_LOADED]);
        const request= action.payload.request;
        switch (action.type) {
            case TableStatsCntlr.SETUP_TBL_TRACKING:
                const {tblId} = action.payload;
                if (TableUtil.isFullyLoaded(tblId)) {
                    TableStatsCntlr.dispatchLoadTblStats(TableUtil.findTblById(tblId).request);
                }
                break;
            case TablesCntlr.TABLE_NEW_LOADED:
                const {tbl_id} = action.payload;
                tableStatsState = flux.getState()[TableStatsCntlr.TBLSTATS_DATA_KEY];
                if (has(tableStatsState, tbl_id)) {
                    TableStatsCntlr.dispatchLoadTblStats(request);
                }
                xyPlotState = flux.getState()[XYPlotCntlr.XYPLOT_DATA_KEY];
                if (has(xyPlotState, tbl_id)) {
                    const xyPlotParams = xyPlotState[tbl_id].xyPlotParams;
                    XYPlotCntlr.dispatchLoadPlotData(xyPlotParams, request);
                }
                histogramState = flux.getState()[HistogramCntlr.HISTOGRAM_DATA_KEY];
                if (has(histogramState, tbl_id)) {
                    const histogramParams = histogramState[tbl_id].histogramParams;
                    HistogramCntlr.dispatchLoadColData(histogramParams, request);
                }
                break;
        }
    }
}

