/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import * as XYPlotCntlr from '../visualize/XYPlotCntlr.js';
//import * as HistogramCntlr from '../visualize/HistogramCntlr.js';
//import * as TableStatsCntlr from '../visualize/TableStatsCntlr.js';

export {uniqueChartId} from '../visualize/ChartUtil.js';

export function loadPlotDataForTbl(tblId, chartId, xyPlotParams) {
    XYPlotCntlr.dispatchLoadPlotData(chartId, xyPlotParams, tblId);
}
