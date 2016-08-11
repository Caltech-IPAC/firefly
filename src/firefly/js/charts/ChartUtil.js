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
 * @module lowLevelApi
 *
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
 * @param prefix
 * @returns {*}
 * @memeberof lowLevelApi
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
        if (c.type.match(/^[dfil]/) != null) {      // int, float, double, long .. or their short form.
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
        if (numericCols.length > 2) {
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



