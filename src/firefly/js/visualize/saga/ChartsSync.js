/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';


import * as TablesCntlr from '../../tables/TablesCntlr.js';
import * as TableStatsCntlr from '../../charts/TableStatsCntlr.js';
import * as TableUtil from '../../tables/TableUtil.js';


import * as ChartsCntlr from '../../charts/ChartsCntlr.js';
import {DATATYPE_XYCOLS, getDefaultXYPlotOptions} from '../../charts/XYPlotCntlr.js';
import {SCATTER} from '../../charts/ChartUtil.js';

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
                if (ChartsCntlr.getNumCharts(tbl_id) === 0) {
                    // how do I know the default chart should be added?
                    // add a default chart if the group is main
                    if (TableUtil.getTableInGroup(tbl_id,'main')) {
                        // default chart is xy plot of coordinate columns or first two numeric columns
                        const defaultOptions = getDefaultXYPlotOptions(tbl_id);
                        if (defaultOptions) {
                            ChartsCntlr.dispatchChartAdd({
                                chartId: 'xyplot-'+tbl_id,
                                chartType: SCATTER,
                                groupId: tbl_id,
                                deletable: false,
                                chartDataElements: [{tblId: tbl_id, type: DATATYPE_XYCOLS.id, options: defaultOptions}]
                            });
                        }
                    }
                } else {
                    if (ChartsCntlr.getNumCharts(tbl_id, true)>0) {  // has related mounted charts
                        const {invokedBy} = action.payload;
                        if (invokedBy !== TablesCntlr.TABLE_SORT) {
                            TableStatsCntlr.dispatchLoadTblStats(TableUtil.getTblById(tbl_id)['request']);
                        }
                        ChartsCntlr.updateRelatedData(tbl_id, invokedBy);
                    }
                }

                break;
        }
    }
}
