/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * Utilities related to charts
 * Created by tatianag on 3/17/16.
 */

import {getTblById, getColumnIdx, getCellValue} from '../tables/TableUtil.js';
import {Expression} from '../util/expr/Expression.js';
import {logError} from '../util/WebUtil.js';

/**
 * This method returns an object with the keys x,y,highlightedRow
 */
export const getHighlighted = function(xyPlotParams, tblId) {

    const tableModel = getTblById(tblId);
    if (tableModel && xyPlotParams) {
        const rowIdx = tableModel.highlightedRow;
        const xIn = xyPlotParams.x.columnOrExpr;
        const yIn = xyPlotParams.y.columnOrExpr;

        var x, y;
        if (getColumnIdx(tableModel, xIn) >= 0) {
            x = getCellValue(tableModel, rowIdx, xIn);
        } else {
            x = getExpressionValue(xIn, tableModel, rowIdx);
        }

        if (getColumnIdx(tableModel, yIn) >= 0) {
            y = getCellValue(tableModel, rowIdx, yIn);
        } else {
            y = getExpressionValue(yIn, tableModel, rowIdx);
        }
        return {x:Number(x), y:Number(y), rowIdx};
    }
};

function getExpressionValue(strExpr, tableModel, rowIdx) {

    const expr = new Expression(strExpr); // no check for allowed variables, already validated
    if (expr.isValid()) {
        const parsedVars = expr.getParsedVariables();
        parsedVars.forEach((v)=> {
            if (getColumnIdx(tableModel, v) >= 0) {
                const val = getCellValue(tableModel, rowIdx, v);
                expr.setVariableValue(v, Number(val));
            }
        });
        return expr.getValue();
    } else {
        logError('Invalid expression '+expr.getInput(), expr.getError().error);
    }
}

//export const getTblId = function(chartId) {
//    return chartId; // will be chart GUI id in future
//};
