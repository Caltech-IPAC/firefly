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
import {MetaConst} from '../data/MetaConst.js';
import {getTblById, getColumnIdx, getCellValue} from '../tables/TableUtil.js';
import {Expression} from '../util/expr/Expression.js';
import {logError} from '../util/WebUtil.js';
import {XYPLOT_DATA_KEY} from './XYPlotCntlr.js';
import {HISTOGRAM_DATA_KEY} from './HistogramCntlr.js';

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


export function getChartSpace(chartType) {
    switch(chartType) {
        case SCATTER:
            return get(flux.getState(), XYPLOT_DATA_KEY);
        case HISTOGRAM:
            return get(flux.getState(), HISTOGRAM_DATA_KEY);
        default:
            logError(`Unknown chart type ${chartType}`);
            return undefined;
    }
}

export function hasRelatedCharts(tblId, space) {
    if (space) {
        return Boolean(Object.keys(space).find((chartId) => {
            return space[chartId].tblId === tblId;
        }));
    } else {
        return hasRelatedCharts(tblId, getChartSpace(SCATTER)) ||
            hasRelatedCharts(tblId, getChartSpace(HISTOGRAM));
    }
}

export function getChartIdsWithPrefix(prefix, space) {
    if (space) {
        return Object.keys(space).filter((c)=>{ return c.startsWith(prefix); });
    } else {
        const chartIds = [];
        chartIds.push(...getChartIdsWithPrefix(prefix, getChartSpace(SCATTER)));
        chartIds.push(...getChartIdsWithPrefix(prefix, getChartSpace(HISTOGRAM)));
        return chartIds;
    }
}

export function hasDefaultCharts() {
    return (getChartIdsWithPrefix('default').length > 0);
}

export function getTblIdForChartId(chartId) {
    return  get(getChartSpace(SCATTER), [chartId, 'tblId']) ||
            get(getChartSpace(HISTOGRAM), [chartId, 'tblId']);
}

export function numRelatedCharts(tblId) {
    let numRelated = 0;
    let c;
    const keys = [SCATTER, HISTOGRAM];
    keys.forEach( (key) => {
        const space = getChartSpace(key);
        for (c in space) {
            if (space.hasOwnProperty(c) && space[c].tblId === tblId) {
                numRelated++;
            }
        }
    });
    return numRelated;
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

function colWithName(cols, name) {
    return cols.find((c) => { return c.name===name; });
}

function getNumericCols(cols) {
    const ncols = [];
    cols.forEach((c) => {
        if (c.type.match(/^[dfil]/) !== null) {      // int, float, double, long .. or their short form.
            ncols.push(c);
        }
    });
    return ncols;
}

export function getDefaultXYPlotParams(tbl_id) {

    const {tableMeta, tableData, totalRows}= getTblById(tbl_id);

    if (!totalRows) {
        return;
    }

    // for catalogs use lon and lat columns
    let isCatalog = Boolean(tableMeta[MetaConst.CATALOG_OVERLAY_TYPE] && tableMeta[MetaConst.CATALOG_COORD_COLS]);
    let xCol = undefined, yCol = undefined;

    if (isCatalog) {
        const s = tableMeta[MetaConst.CATALOG_COORD_COLS].split(';');
        if (s.length !== 3) return;
        xCol = colWithName(tableData.columns, s[0]); // longtitude
        yCol = colWithName(tableData.columns, s[1]); // latitude

        if (!xCol || !yCol) {
            isCatalog = false;
        }
    }

    // otherwise use the first one-two numeric columns
    if (!isCatalog) {
        const numericCols = getNumericCols(tableData.columns);
        if (numericCols.length >= 2) {
            xCol = numericCols[0];
            yCol = numericCols[1];
        } else if (numericCols.length > 1) {
            xCol = numericCols[0];
            yCol = numericCols[0];
        }
    }

    return (xCol && yCol) ?
    {
        x: {columnOrExpr: xCol.name, options: isCatalog ? 'flip' : '_none_'},
        y: {columnOrExpr: yCol.name}
    } : undefined;
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
 * @summary Convert shallow object with XYPlot parameters to scatter plot parameters object
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
 * @prop {string}  [source]     location of the ipac table, url or file path; ignored when histogram view is added to table
 * @prop {string}  [tbl_id]     table id of the table this plot is connected to
 * @prop {string}  [chartTitle] title of the chart
 * @prop {string}  col          column or expression to use for histogram, can contain multiple column names ex. log(col) or (col1-col2)/col3
 * @prop {number}  [numBins=50] number of bins' for fixed bins algorithm (default)
 * @prop {number}  [falsePositiveRate] false positive rate for bayesian blocks algorithm
 * @prop {string}  [xOptions]   comma separated list of x axis options: flip,log
 * @prop {string}  [yOptions]   comma separated list of y axis options: flip,log
 */

/**
 * @summary Convert shallow object with Histogram parameters to histogram plot parameters object
 * @param {HistogramOptions} params - shallow object with Histogram parameters
 * @returns {Object} - object, used to create Histogram chart
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
