/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * Utilities related to charts
 * Created by tatianag on 3/17/16.
 */
import {get} from 'lodash';
import {uniqueId} from 'lodash';

import {flux} from '../Firefly.js';
import {getTblById, getColumnIdx, getCellValue} from '../tables/TableUtil.js';
import {Expression} from '../util/expr/Expression.js';
import {logError} from '../util/WebUtil.js';
import {XYPLOT_DATA_KEY} from '../visualize/XYPlotCntlr.js';
import {HISTOGRAM_DATA_KEY} from '../visualize/HistogramCntlr';


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

export function getTblIdForChartId(chartId) {
    return  get(flux.getState()[XYPLOT_DATA_KEY], [chartId, 'tblId']) ||
            get(flux.getState()[HISTOGRAM_DATA_KEY], [chartId, 'tblId']);
}

export function uniqueChartId(prefix) {
    return uniqueId(prefix?prefix+'-c':'c');
}



