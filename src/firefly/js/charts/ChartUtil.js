/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * Utilities related to charts
 * Created by tatianag on 3/17/16.
 */
import {uniqueId} from 'lodash';

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
 * @prop {number}  [xyRatio]    aspect ratio (must be between 1 and 10), if not defined the chart will fill all available space
 * @prop {string}  [stretch]    'fit' to fit plot into available space or 'fill' to fill the available width (applied when xyPlotRatio is defined)
 * @prop {string}  [xLabel]     label to use with x axis
 * @prop {string}  [yLabel]     label to use with y axis
 * @prop {string}  [xUnit]      unit for x axis
 * @prop {string}  [yUnit]      unit for y axis
 * @prop {string}  [xOptions]   comma separated list of x axis options: grid,flip,log
 * @prop {string}  [yOptions]   comma separated list of y axis options: grid,flip,log
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
    const {xCol, yCol, xyRatio, stretch, xLabel, yLabel, xUnit, yUnit, xOptions, yOptions} = params;
    const xyPlotParams = xCol && yCol ?
    {
        xyRatio,
        stretch,
        x : { columnOrExpr : xCol, label : xLabel, unit : xUnit, options : xOptions},
        y : { columnOrExpr : yCol, label : yLabel, unit : yUnit, options : yOptions}
    } : undefined;
    return xyPlotParams;
}


/**
 * @global
 * @public
 * @typedef {Object} HistogramOptions - shallow object with histogram parameters
 * @prop {string}  [sourc    w332e]     location of the ipac table, url or file path; ignored when histogram view is added to table
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
    const {col, xOptions, yOptions, falsePositiveRate} = params;
    let numBins = params.numBins;
    if (!falsePositiveRate && !numBins) {numBins = 50;}
    const algorithm = numBins ? 'fixedSizeBins' : 'bayesianBlocks';

    if (col) {
        const histogramParams =
        {
            columnOrExpr: col,
            algorithm,
            numBins,
            falsePositiveRate,
            x: xOptions||'',
            y: yOptions||''
        };
        return histogramParams;
    }
}
