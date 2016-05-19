/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';

import {dispatchAddSaga} from '../core/MasterSaga.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import * as TableUtil from '../tables/TableUtil.js';

import * as XYPlotCntlr from '../visualize/XYPlotCntlr.js';
//import * as HistogramCntlr from '../visualize/HistogramCntlr.js';
//import * as TableStatsCntlr from '../visualize/TableStatsCntlr.js';

export {uniqueChartId} from '../visualize/ChartUtil.js';

export function loadPlotDataForTbl(tblId, chartId, xyPlotParams) {
    dispatchAddSaga(getChart, {tblId, chartId, xyPlotParams});
}

export function* getChart({tblId, chartId, xyPlotParams}) {

    if (TableUtil.isFullyLoaded(tblId)) {
        XYPlotCntlr.dispatchLoadPlotData(chartId, xyPlotParams, TableUtil.getTblById(tblId)['request']);
        return;
    }

    while (true) {
        const action = yield take([TablesCntlr.TABLE_NEW_LOADED]);
        const {tbl_id, request} = action.payload;
        if (tbl_id === tblId) {
            XYPlotCntlr.dispatchLoadPlotData(chartId, xyPlotParams, request);
            return;
        }
    }
}