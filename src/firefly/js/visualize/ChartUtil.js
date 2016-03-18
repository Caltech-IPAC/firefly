/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * Utilities related to charts
 * Created by tatianag on 3/17/16.
 */

import {findTblById, getCellValue} from '../tables/TableUtil.js';


/**
 * This method returns an object with the keys x,y,highlightedRow
 */
export const getHighlighted = function(xyPlotParams, tblId) {
    const tableModel = findTblById(tblId);
    if (tableModel && xyPlotParams) {
        const rowIdx = tableModel.highlightedRow;
        //TODO: support column expressions
        const x = getCellValue(tableModel, rowIdx, xyPlotParams.x.columnOrExpr);
        const y = getCellValue(tableModel, rowIdx, xyPlotParams.y.columnOrExpr);
        return {x:Number(x), y:Number(y), rowIdx};
    }
};

//export const getTblId = function(chartId) {
//    return chartId; // will be chart GUI id in future
//};
