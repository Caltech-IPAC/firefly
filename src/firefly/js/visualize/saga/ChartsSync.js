/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';


import * as TablesCntlr from '../../tables/TablesCntlr.js';
import * as TableStatsCntlr from '../../charts/TableStatsCntlr.js';
import * as TableUtil from '../../tables/TableUtil.js';
import * as ChartsCntlr from '../../charts/ChartsCntlr.js';

/**
 * this saga handles chart related side effects
 */
export function* syncCharts() {
    while (true) {
        const action= yield take([ChartsCntlr.CHART_ADD, ChartsCntlr.CHART_MOUNTED, TablesCntlr.TABLE_LOADED]);
        switch (action.type) {
            case ChartsCntlr.CHART_ADD:
            case ChartsCntlr.CHART_MOUNTED:
                const {chartId} = action.payload;
                ChartsCntlr.getRelatedTblIds(chartId).forEach((tblId) => {
                    if (TableUtil.isFullyLoaded(tblId)) {
                        if (ChartsCntlr.getNumCharts(tblId) === 1) {
                            TableStatsCntlr.dispatchLoadTblStats(TableUtil.getTblById(tblId)['request']);
                        }
                        ChartsCntlr.updateChartData(chartId);
                    }
                });
                break;
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
