/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, isArray, isUndefined, uniqueId, isNil} from 'lodash';
import {getTblById, getColumns, getColumn, doFetchTable, stripColumnNameQuotes} from '../../tables/TableUtil.js';
import {cloneRequest, makeSubQueryRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchChartUpdate, dispatchError, getChartData, getTraceSymbol, hasUpperLimits, hasLowerLimits} from '../ChartsCntlr.js';
import {formatColExpr, getDataChangesForMappings, updateHighlighted, updateSelection, isScatter2d, getMaxScatterRows, getMinScatterGLRows} from '../ChartUtil.js';
import {getTraceTSEntries as heatmapTSGetter} from './FireflyHeatmap.js';
import {errorTypeFieldKey} from '../ui/options/Errors.jsx';

// some chart properties can either come from a table column or be a number
const numberOrArrayProps = ['marker.size'];

/**
 * This function creates table source entries to get plotly chart data from the server
 * For the non-firefly plotly chart types
 * @param p parameters
 * @param p.traceTS
 * @param p.chartId
 * @param p.traceNum
 * @returns {{options: *, fetchData: fetchData}}
 */
export function getTraceTSEntries({traceTS, chartId, traceNum}) {
    const {mappings} = traceTS || {};

    if (mappings) {
        const options = Object.assign({}, mappings);

        if (hasUpperLimits(chartId, traceNum) || hasLowerLimits(chartId, traceNum)) {
            const {layout, data, fireflyData} = getChartData(chartId);
            const {range=[], autorange=true, type='linear'} = get(layout, 'yaxis', {});
            const symbol = getTraceSymbol(data, fireflyData, traceNum);
            const mode = get(data, `${traceNum}.mode`);
            const reversed = (autorange === 'reversed') || (range[1] < range[0]);
            options['ytype'] = type;
            options['yreversed'] = reversed;
            options['symbol'] = symbol;
            options['mode'] = mode;
        }
        return {options, fetchData};

    } else {
        return {};
    }
}

async function fetchData(chartId, traceNum, tablesource) {

    const {tbl_id, mappings} = tablesource;
    if (!mappings) {
        return;
    }

    const originalTableModel = getTblById(tbl_id);
    const {highlightedRow, selectInfo, totalRows} = originalTableModel;
    const {fireflyData} = getChartData(chartId);

    if (totalRows > getMaxScatterRows()) {
        // scatterOrHeatmap attribute is set when a heatmap trace is used to represent a scatter
        if (fireflyData?.[traceNum]?.scatterOrHeatmap) {
            // if the number of rows is above a threshhold,
            // heatmap chart should be produced
            const {options: heatmapOptions, fetchData: heatmapFetchData} = heatmapTSGetter({
                traceTS: tablesource,
                chartId,
                traceNum
            });
            const heatmapTS = Object.assign({}, tablesource, {options: heatmapOptions});
            return heatmapFetchData(chartId, traceNum, heatmapTS);
        }
    }

    const sreq = createChartTblRequest(chartId, traceNum, tablesource);

    const tableModel = await doFetchTable(sreq).catch((reason) => {
        dispatchError(chartId, traceNum, reason);
    });

    if (tableModel.error) {
        return dispatchError(chartId, traceNum, tableModel.error);
    }

    if (tableModel.tableData && tableModel.tableData.data) {
        const changes = getDataChangesForMappings({tableModel, mappings, traceNum});

        // extra changes based on trace type
        addOtherChanges({changes, chartId, traceNum, tablesource, tableModel: originalTableModel});

        dispatchChartUpdate({chartId, changes});
        const {activeTrace} = getChartData(chartId);
        if (isUndefined(activeTrace) || activeTrace === traceNum) {
            updateHighlighted(chartId, traceNum, highlightedRow);
            updateSelection(chartId, selectInfo);
        }
    } else {
        dispatchError(chartId, traceNum, 'No data');
    }
}

export function createChartTblRequest(chartId, traceNum, tablesource) {

    const {tbl_id, mappings} = tablesource;
    const originalTableModel = getTblById(tbl_id);
    const {request} = originalTableModel;
    const {fireflyData} = getChartData(chartId);

    const colNames = getColumns(originalTableModel).map((c) => c.name);

    // default behavior
    let sreq = cloneRequest(request, {
        startIdx: 0,
        pageSize: MAX_ROW,
        inclCols: Object.entries(mappings).filter(([k, v]) => !(numberOrArrayProps.includes(k) && !isNaN(v))).map(([k, v]) => {   // if field can be a number and is a number, then skip
            // we'd like expression columns to be named as the paths to trace data arrays, ex. data[0].x
            // otherwise use column names to preserve original column attributes (type, format, etc.)
            const asStr = colNames.includes(v) || colNames.includes(stripColumnNameQuotes(v)) ? '' :
                k.startsWith('firefly') ? ` as "${k}"` : ` as "data.${traceNum}.${k}"`;
            return `${formatColExpr({colOrExpr: v, quoted: true, colNames})}${asStr}`;
        }).filter((c, i, a) => a.indexOf(c) === i).// remove duplicates
        join(', ')    // allows to use the same columns, ex. "w1" as "x", "w1" as "marker.color"
    }, true);
    if (fireflyData?.[traceNum]?.filters) {
        const inclCols = (sreq.inclCols ? sreq.inclCols + ',' : '') + '"ROW_NUM" as "ORIG_IDX"';
        sreq = makeSubQueryRequest(request, sreq.title, sreq.params,{filters: fireflyData?.[traceNum].filters, inclCols, pageSize: MAX_ROW});
    }
    return sreq;
}


export function addOtherChanges({changes, chartId, traceNum, tablesource, tableModel}) {
    const chartData = getChartData(chartId);
    const type = get(chartData, `data.${traceNum}.type`, 'scatter');
    let scatter2d = isScatter2d(type);
    if (!scatter2d) {
        // scatterOrHeatmap attribute is set when a heatmap trace is used to represent a scatter
        const scatterOrHeatmap = get(chartData, `fireflyData.${traceNum}.scatterOrHeatmap`);
        if (scatterOrHeatmap) {
            // moving from heatmap to scatter representation
            // if we started as a heatmap, "markers" mode needs to be set, because the default mode is "lines"
            changes[`data.${traceNum}.mode`] = 'markers';
            // clean arrays set in heatmap mode
            changes[`data.${traceNum}.text`] = undefined;
            changes[`data.${traceNum}.z`] = undefined;
            // tablesource options have changed
            const {options} = tablesource;
            changes[`tablesources.${traceNum}.options`] = options;
            scatter2d = true;
        }
    }

    if (scatter2d) {
        addScatterChanges({changes, chartId, traceNum, tablesource, tableModel});
    }
}

function setupAxisInfo({changes, tableModel, mappings, fireflyData, layout, data, traceNum}) {

    const xColumn = getColumn(tableModel, stripColumnNameQuotes(get(mappings, 'x')));
    const xUnit = xColumn?.units || fireflyData?.[traceNum]?.xUnit || '';
    const yColumn = getColumn(tableModel, stripColumnNameQuotes(get(mappings, 'y')));
    const yUnit = yColumn?.units || fireflyData?.[traceNum]?.yUnit || '';

    // default axes labels (remove surrounding quotes, if any)
    const xLabel = stripColumnNameQuotes(get(mappings, 'x'));
    const yLabel = stripColumnNameQuotes(get(mappings, 'y'));

    const xaxis = (data?.[traceNum]?.xaxis ?? 'x').replace('x', 'xaxis') ;  // plotly convention.. 'x', 'x2' => 'xaxis', 'xaxis2'
    const yaxis = (data?.[traceNum]?.yaxis ?? 'y').replace('y', 'yaxis') ;

    let xAxisLabel = get(layout, `${xaxis}.title.text`);
    let yAxisLabel = get(layout, `${yaxis}.title.text`);

    if (!xAxisLabel) {
        xAxisLabel = xLabel + (xUnit ? ` (${xUnit})` : '');
        changes[`layout.${xaxis}.title.text`] = xAxisLabel;
    }

    if (!yAxisLabel) {
        yAxisLabel = yLabel + (yUnit ? ` (${yUnit})` : '');
        changes[`layout.${yaxis}.title.text`] = yAxisLabel;
    }

    // point tooltip labels
    let xTipLabel = xLabel?.length <= 20 ? xLabel : 'x';
    let yTipLabel = yLabel?.length <= 20 ? yLabel : 'y';

    const {xTTLabelSrc, yTTLabelSrc} = get(fireflyData, traceNum, {});
    if (xTTLabelSrc) {
        xTipLabel = (xTTLabelSrc === 'axis') ? (xAxisLabel || 'x') : 'x';
    }
    if (yTTLabelSrc) {
        yTipLabel = (yTTLabelSrc === 'axis') ? (yAxisLabel || 'y') : 'y';
    }

    return {xTipLabel, yTipLabel, xUnit, yUnit};
}

/**
 * Firefly added scatter changes
 * @param {Object} p - parameters
 * @param p.changes existing changes
 * @param p.chartId chart id
 * @param p.traceNum
 * @param p.tablesource
 * @param p.tableModel - original table model
 */
export function addScatterChanges({changes, chartId, traceNum, tablesource, tableModel}) {

    const {mappings} = tablesource;
    const {layout, data, fireflyData} = getChartData(chartId) || {};

    const colors = get(changes, [`data.${traceNum}.marker.color`]);
    let cTipLabel = isArray(colors) ? get(mappings, 'marker.color') : '';
    if (cTipLabel.length > 20) {
        cTipLabel = 'c';
    }

    // legend group is used to show/hide traces together
    // highlight and selected traces should have the same legend group as the active scatter
    if (!get(data, `${traceNum}.legendgroup`)) {
        changes[`data.${traceNum}.legendgroup`] = uniqueId('grp');
    }

    const {xTipLabel, yTipLabel, xUnit, yUnit} = setupAxisInfo({changes, tableModel, mappings, fireflyData, layout, data, traceNum});

    const x = get(changes, [`data.${traceNum}.x`]);
    if (!x) return;

    const traceType = (isArray(x) && x.length > getMinScatterGLRows()) ? 'scattergl' : 'scatter';
    changes[`data.${traceNum}.type`] = traceType;


    // handle limits: update new or erase old
    const annotations = [];
    const symbol = getTraceSymbol(data, fireflyData, traceNum);
    if (hasUpperLimits(chartId, traceNum) || hasLowerLimits(chartId, traceNum)) {
        // if there is a limit value and no y value, y is set to the limit value
        let numNewPts = 0;
        let symbolArr = undefined;
        const arrowcolor = get(data, `${traceNum}.marker.color`);
        const {range = [], autorange = true, type: ytype} = get(layout, 'yaxis', {});
        const {type: xtype} = get(layout, 'xaxis', {});
        const reversed = (autorange === 'reversed') || (range[1] < range[0]);
        let sign = reversed ? -1 : 1;

        // there should be an upper limit value or lower limit value, but not both at the same time
        // both will appear if present, but there a single point value associated with both limits
        // if we have a use case, we can think how to handle it

        const addAnnotations = (v, i) => {
            let yy = parseFloat(v);
            if (Number.isFinite(yy)) {
                // create a point if not present and change its symbol
                if (!Number.isFinite(parseFloat(changes[`data.${traceNum}.y`][i]))) {
                    changes[`data.${traceNum}.y`][i] = v;
                    // change the symbol
                    if (numNewPts === 0) {
                        symbolArr = new Array(x.length);
                        symbolArr.fill(symbol);
                    }
                    symbolArr[i] = 'line-ew-open';
                    numNewPts++;
                }


                // annotation position should take into account axis type
                let xx = parseFloat(changes[`data.${traceNum}.x`][i]);
                if (xtype === 'log') {
                    xx = Math.log10(xx);
                }

                if (ytype === 'log') {
                    yy = Math.log10(yy);
                }

                if (isUndefined(annotations[i])) {
                    annotations[i] = [];
                }
                annotations[i].push({
                    x: xx,
                    y: yy,
                    xref: 'x',
                    yref: 'y',
                    showarrow: true,
                    arrowhead: 3,
                    ax: 0,
                    ay: sign * (-40),
                    yshift: sign * (-30),
                    standoff: 10,
                    arrowcolor
                });
            }
        };

        // per trace annotations
        const upperLimitArray = changes[`fireflyData.${traceNum}.yMax`];
        if (upperLimitArray) { upperLimitArray.forEach(addAnnotations); }

        const lowerLimitArray = changes[`fireflyData.${traceNum}.yMin`];
        if (lowerLimitArray) {
            sign = -sign;
            lowerLimitArray.forEach(addAnnotations);
        }

        // set an array of marker symbols
        if (numNewPts > 0) {
            changes[`data.${traceNum}.marker.symbol`] = symbolArr;
            changes[`fireflyData.${traceNum}.marker.symbol`] = symbol;
        }
    }
    // per trace annotations
    changes[`fireflyData.${traceNum}.annotations`] = annotations;

    // handle errors

    let xErr = get(changes, [`data.${traceNum}.error_x.array`], []);
    let xErrHigh = xErr;
    if (xErr.length > 0) {
        // plotly interprepts empty strings in error arrays as 0 - need to convert them to numbers
        changes[`data.${traceNum}.error_x.array`] = xErr.map((e)=>parseFloat(e));
    }
    const xErrLow = get(changes, [`data.${traceNum}.error_x.arrayminus`], []);
    if (xErrLow.length > 0) {
        changes[`data.${traceNum}.error_x.arrayminus`] = xErrLow.map((e)=>parseFloat(e));
        xErr = []; // asymmetric error
    } else {
        xErrHigh = []; // symmetric error
    }
    const hasXErrors = xErrLow.length > 0 || xErr.length > 0;
    const xErrorType = get({fireflyData}, errorTypeFieldKey(traceNum, 'x'));
    // changes[`data.${traceNum}.error_x.visible`] = hasXErrors && xErrorType !== 'none';
    changes[`data.${traceNum}.error_x.symmetric`] = xErrorType==='sym' || xErr.length > 0;

    let yErr = get(changes, [`data.${traceNum}.error_y.array`], []);
    let yErrHigh = yErr;
    if (yErr.length > 0) {
        changes[`data.${traceNum}.error_y.array`] = yErr.map((e)=>parseFloat(e));
    }
    const yErrLow = get(changes, [`data.${traceNum}.error_y.arrayminus`], []);
    if (yErrLow.length > 0) {
        changes[`data.${traceNum}.error_y.arrayminus`] = yErrLow.map((e)=>parseFloat(e));
        yErr = []; // asymmetric error
    } else {
        yErrHigh = []; // symmetric error
    }
    const hasYErrors =  yErrLow.length > 0 || yErr.length > 0;
    const yErrorType = get({fireflyData}, errorTypeFieldKey(traceNum, 'y'));
    // changes[`data.${traceNum}.error_y.visible`] = hasYErrors && yErrorType !== 'none';
    changes[`data.${traceNum}.error_y.symmetric`] = yErrorType==='sym' || yErr.length > 0;

    // set tooltips

    // hoverinfo 'skip' disables hover layer - hence we can not highlight clicking on a point or select points in the chart
    // if we support it, we need to exclude select button from the tools appearing on select
    // also, we might want to disable showing highlighted and selected points in the chart
    if (get(data, `${traceNum}.hoverinfo`, 'text') === 'text') {
        const text = x.map((xval, idx) => {
            const y = get(changes, [`data.${traceNum}.y`]);
            const yval = y[idx];
            const xerr = hasXErrors ? formatError(xval, xErr[idx], xErrLow[idx], xErrHigh[idx]) : '';
            const yerr = hasYErrors ? formatError(yval, yErr[idx], yErrLow[idx], yErrHigh[idx]) : '';
            const cval = isArray(colors) ? `<br> ${cTipLabel} = ${parseFloat(colors[idx])} ` : '';
            let   ul = '';
            if (annotations[idx]) {
                if (!isNil(changes[`fireflyData.${traceNum}.yMax`]?.[idx])) {
                    ul = '<br> Upper Limit ';
                } else if (!isNil(changes[`fireflyData.${traceNum}.yMin`]?.[idx])) {
                    ul = '<br> Lower Limit ';
                }
            }
            return `<span> ${xTipLabel} = ${formatVal(xval)}${xerr} ${xUnit} <br>` +
                ` ${yTipLabel} = ${formatVal(yval)}${yerr} ${yUnit} ${ul} ${cval}</span>`;
        });

        if (traceType === 'scatter') {
            changes[`data.${traceNum}.hovertext`] = text;
        } else {
            //scattergl does not support hovertext
            changes[`data.${traceNum}.text`] = text;
        }
        changes[`data.${traceNum}.hoverinfo`] = 'text';
    }



    // TODO: colorbar needs more work:
    // it does not get updated when color scale is changing,
    // also ticks labels are intermixed with integers,
    // color bar and yaxis should be displayed on the opposite sides of the chart

    // if color map is used, show the scale
    //const colorMapColOrExpr = mappings['marker.color'];
    //changes[`data.${traceNum}.marker.autoscale`] = false;
    //changes[`data.${traceNum}.marker.showscale`] = Boolean(colorMapColOrExpr);
    //if (colorMapColOrExpr) {
    //    changes[`data.${traceNum}.marker.colorbar`] = {
    //        //xanchor: yOpposite ? 'right' : 'left',
    //        //x: yOpposite ? -0.02 : 1.02,
    //        showticklabels: false,
    //        thickness: 5,
    //        outlinewidth: 0,
    //        title: {text: colorMapColOrExpr}
    //    };
    //}
}

/**
 * This is a work-around for plotly not handling BigInt.
 * We store BigInt as string.  Plotly will plot it as a number.  When converted, precision will be lost.
 * Instead of showing the plotted value, we show the original BigInt as a string.
 * @param v the value to format
 * @returns {number}
 */
const formatVal = (v) => {
    const val = parseFloat(v);
    return (Number.isInteger(val) && !Number.isSafeInteger(val)) ? v : val;
};

/**
 * Format errors - all parameters can be strings or numbers
 * @param {string|number} val
 * @param {string|number} err
 * @param {string|number} errLow
 * @param {string|number} errHigh
 * @returns {string}
 */
export const formatError = function(val, err, errLow, errHigh) {
    // TODO use format for expressions in future - still hard to tell how many places to save
    let str = '';
    const errNum = parseFloat(err);
    if (Number.isFinite(errNum)) {
        //return ' \u00B1 '+numeral(lowErr).format(fmtLow); //Unicode U+00B1 is plusmn
        str = ` \u00B1${formatVal(err)}`; //Unicode U+00B1 is plusmn
    } else {
        const errHighNum = parseFloat(errHigh);
        if (Number.isFinite(errHighNum)) {
            str += ` \u002B${formatVal(errHigh)}`;
        }
        const errLowNum = parseFloat(errLow);
        if (Number.isFinite(errLowNum)) {
            if (str) str += ' /';
            str += ` \u2212${formatVal(errLow)}`;
        }
    }
    return str;
};
