/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * Utilities related to charts
 * Created by tatianag on 3/17/16.
 */
import {uniqueId, isUndefined, omitBy} from 'lodash';

import {getTblById, getColumnIdx, getCellValue} from '../tables/TableUtil.js';
import {Expression} from '../util/expr/Expression.js';
import {logError} from '../util/WebUtil.js';

export const SCATTER = 'scatter';
export const HISTOGRAM = 'histogram';


/**
 * This method returns an object with the keys x,y,highlightedRow
 *
 * @param {XYPlotParams} xyPlotParams
 * @param {string} tblId
 * @returns {{x: number, y: number, rowIdx}}
 */
export const getHighlighted = function(xyPlotParams, tblId) {

    const tableModel = getTblById(tblId);
    if (tableModel && xyPlotParams) {
        const rowIdx = tableModel.highlightedRow;
        const highlighted = {rowIdx};
        [
            {n:'x',v:xyPlotParams.x.columnOrExpr},
            {n:'y',v:xyPlotParams.y.columnOrExpr},
            {n:'xErr', v:xyPlotParams.x.error},
            {n:'xErrLow', v:xyPlotParams.x.errorLow},
            {n:'xErrHigh', v:xyPlotParams.x.errorHigh},
            {n:'yErr', v:xyPlotParams.y.error},
            {n:'yErrLow', v:xyPlotParams.y.errorLow},
            {n:'yErrHigh', v:xyPlotParams.y.errorHigh}
        ].map((entry) => {
            if (entry.v) {
                highlighted[entry.n] = getColOrExprValue(tableModel, rowIdx, entry.v);
            }
        });
        return highlighted;
    }
};

/**
 * This method returns the value of the column cell or an expression from multiple column cells in a given row
 *
 * @param {TableModel} tableModel - table model
 * @param {number} rowIdx - row index in the table
 * @param {string} colOrExpr - column name or expression
 * @returns {number} value of the column or expression in the given row
 */
export function getColOrExprValue(tableModel, rowIdx, colOrExpr) {
    if (tableModel) {
        var val;
        if (getColumnIdx(tableModel, colOrExpr) >= 0) {
            val = getCellValue(tableModel, rowIdx, colOrExpr);
            val = isFinite(parseFloat(val)) ? Number(val) : Number.NaN;
        } else {
            val = getExpressionValue(tableModel, rowIdx, colOrExpr);
        }
        return val;
    }
}

function getExpressionValue(tableModel, rowIdx, strExpr) {

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

/**
 * @summary Get unique chart id
 * @param {string} [prefix] - prefix
 * @returns {string} unique chart id
 * @public
 * @function uniqueChartId
 * @memberof firefly.util.chart
 */
export function uniqueChartId(prefix) {
    return uniqueId(prefix?prefix+'-c':'c');
}

export function colWithName(cols, name) {
    return cols.find((c) => { return c.name===name; });
}

export function getNumericCols(cols) {
    const ncols = [];
    cols.forEach((c) => {
        if (c.type.match(/^[dfil]/) !== null) {      // int, float, double, long .. or their short form.
            ncols.push(c);
        }
    });
    return ncols;
}



/**
 * @global
 * @public
 * @typedef {Object} XYPlotOptions - shallow object with XYPlot parameters
 * @prop {string}  [source]     location of the ipac table, url or file path; ignored when XY plot view is added to table
 * @prop {string}  [tbl_id]     table id of the table this plot is connected to
 * @prop {string}  [chartTitle] title of the chart
 * @prop {string}  xCol         column or expression to use for x values, can contain multiple column names ex. log(col) or (col1-col2)/col3
 * @prop {string}  yCol         column or expression to use for y values, can contain multiple column names ex. sin(col) or (col1-col2)/col3
 * @prop {string}  [plotStyle]  points, linepoints, line
 * @prop {string}  [sortColOrExpr] sort column or expression (when line plot is requested
 * @prop {number}  [xyRatio]    aspect ratio (must be between 1 and 10), if not defined the chart will fill all available space
 * @prop {string}  [stretch]    'fit' to fit plot into available space or 'fill' to fill the available width (applied when xyPlotRatio is defined)
 * @prop {string}  [xLabel]     label to use with x axis
 * @prop {string}  [yLabel]     label to use with y axis
 * @prop {string}  [xUnit]      unit for x axis
 * @prop {string}  [yUnit]      unit for y axis
 * @prop {string}  [xOptions]   comma separated list of x axis options: grid,flip,log
 * @prop {string}  [yOptions]   comma separated list of y axis options: grid,flip,log
 * @prop {string}  [xError]     column or expression for X error
 * @prop {string}  [yError]     column or expression for Y error
 */

/**
 * @summary Convert shallow object with XYPlot parameters to scatter plot parameters object.
 * @param {XYPlotOptions} params - shallow object with XYPlot parameters
 * @returns {XYPlotParams} - object, used to create XYPlot chart
 * @public
 * @function makeXYPlotParams
 * @memberof firefly.util.chart
 */
export function makeXYPlotParams(params) {
    const {xCol, yCol, xError, yError, xLabel, yLabel, xUnit, yUnit, xOptions, yOptions, plotStyle, sortColOrExpr, xyRatio, stretch} = params;
    const xyPlotParams = xCol && yCol ?
    omitBy({
        plotStyle,
        sortColOrExpr,
        xyRatio,
        stretch,
        x : omitBy({ columnOrExpr : xCol, error: xError, label : xLabel, unit : xUnit, options : xOptions}, isUndefined),
        y : omitBy({ columnOrExpr : yCol, error: yError, label : yLabel, unit : yUnit, options : yOptions}, isUndefined)
    }, isUndefined) : undefined;
    return xyPlotParams;
}


/**
 * @global
 * @public
 * @typedef {Object} HistogramOptions - shallow object with histogram parameters
 * @prop {string}  [source]     location of the ipac table, url or file path; ignored when histogram view is added to table
 * @prop {string}  [tbl_id]     table id of the table this plot is connected to
 * @prop {string}  [chartTitle] title of the chart
 * @prop {string}  col          column or expression to use for histogram, can contain multiple column names ex. log(col) or (col1-col2)/col3
 * @prop {number}  [numBins=50] number of bins for fixed bins algorithm (default)
 * @prop {number}  [falsePositiveRate] false positive rate for bayesian blocks algorithm
 * @prop {string}  [xOptions]   comma separated list of x axis options: flip,log
 * @prop {string}  [yOptions]   comma separated list of y axis options: flip,log
 */

/**
 * @summary Convert shallow object with Histogram parameters to histogram plot parameters object.
 * @param {HistogramOptions} params - shallow object with Histogram parameters
 * @returns {HistogramParams} - object, used to create Histogram chart
 * @public
 * @function makeHistogramParams
 * @memberof firefly.util.chart
 */
export function makeHistogramParams(params) {
    const {col, xOptions, yOptions, falsePositiveRate, binWidth} = params;
    let numBins = params.numBins;
    let fixedBinSizeSelection = params.fixedBinSizeSelection;
    if (!falsePositiveRate) {
        if (!fixedBinSizeSelection) {
            fixedBinSizeSelection = 'numBins';
        }
        if (!numBins) {
            numBins = 50;
        }
    }
    const algorithm = numBins ? 'fixedSizeBins' : 'bayesianBlocks';

    if (col) {
        const histogramParams =
        {
            columnOrExpr: col,
            algorithm,
            fixedBinSizeSelection,
            numBins,
            binWidth,
            falsePositiveRate,
            x: xOptions||'',
            y: yOptions||''
        };
        return histogramParams;
    }
}
