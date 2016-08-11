/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import * as XYPlotCntlr from '../charts/XYPlotCntlr.js';
//import * as HistogramCntlr from '../visualize/HistogramCntlr.js';
//import * as TableStatsCntlr from '../visualize/TableStatsCntlr.js';

export {uniqueChartId} from '../charts/ChartUtil.js';


/*
 * @desc  build  lowLevelApi
 * @module lowLevelApi
 * @param tblId
 * @param chartId
 * @param xyPlotParams
 * @memeberof lowLevelApi
 */
export function loadPlotDataForTbl(tblId, chartId, xyPlotParams) {
    XYPlotCntlr.dispatchLoadPlotData(chartId, xyPlotParams, tblId);
}
