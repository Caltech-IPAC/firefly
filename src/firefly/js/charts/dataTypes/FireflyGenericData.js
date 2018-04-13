/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, isArray, truncate, uniqueId} from 'lodash';
import {COL_TYPE, getTblById, getColumns, getColumn, doFetchTable} from '../../tables/TableUtil.js';
import {cloneRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchChartUpdate, dispatchError, getChartData, getTraceSymbol, hasUpperLimits} from '../ChartsCntlr.js';
import {formatColExpr, getDataChangesForMappings, replaceQuotesIfSurrounding, updateHighlighted, updateSelected, isScatter2d, getMaxScatterRows} from '../ChartUtil.js';


/**
 * This function creates table source entries to get plotly chart data from the server
 * For the non-firefly plotly chart types
 * @param traceTS
 * @returns {{options: *, fetchData: fetchData}}
 */
export function getTraceTSEntries({traceTS, chartId, traceNum}) {
    const {mappings} = traceTS || {};

    if (mappings) {
        const options = Object.assign({}, mappings);

        if (hasUpperLimits(chartId, traceNum)) {
            const {layout, data, fireflyData} = getChartData(chartId);
            const {range=[], autorange=true, type='linear'} = get(layout, 'yaxis', {});
            const symbol = getTraceSymbol(data, fireflyData, traceNum);
            const reversed = (autorange === 'reversed') || (range[1] < range[0]);
            options['ytype'] = type;
            options['yreversed'] = reversed;
            options['symbol'] = symbol;
        }
        return {options, fetchData};

    } else {
        return {};
    }
}

function fetchData(chartId, traceNum, tablesource) {

    const {tbl_id, mappings} = tablesource;
    if (!mappings) { return; }

    const originalTableModel = getTblById(tbl_id);
    const {request, highlightedRow, selectInfo, totalRows} = originalTableModel;

    const maxScatterRows = getMaxScatterRows();
    if (totalRows > maxScatterRows && get(getChartData(chartId), `data.${traceNum}.type`, 'scatter') === 'scatter') {
        dispatchError(chartId, traceNum, `${totalRows} rows exceed scatter chart best performance limit of ${maxScatterRows}.`);
        return;
    }

    const numericCols = getColumns(originalTableModel, COL_TYPE.NUMBER).map((c) => c.name);

    // default behavior
    const sreq = cloneRequest(request, {
        startIdx: 0,
        pageSize: MAX_ROW,
        inclCols: Object.entries(mappings).map(([k,v]) => {
            // we'd like expression columns to be named as the paths to trace data arrays, ex. data[0].x
            //const asStr = (numericCols.includes(v)) ? '' : k.startsWith('firefly') ? ` as "${k}"` :` as "data.${traceNum}.${k}"`;
            const asStr = k.startsWith('firefly') ? ` as "${k}"` :` as "data.${traceNum}.${k}"`;
            return `${formatColExpr({colOrExpr: v, quoted: true, colNames: numericCols})}${asStr}`;
        }).join(', ')    // allows to use the same columns, ex. "w1" as "x", "w1" as "marker.color"
    });

    const sreqTblId = uniqueId(request.tbl_id);
    sreq.META_INFO.tbl_id = sreqTblId;
    sreq.tbl_id = sreqTblId;

    doFetchTable(sreq).then(
        (tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                const changes = getDataChangesForMappings({tableModel, mappings, traceNum});

                // extra changes based on trace type
                addOtherChanges({changes, chartId, traceNum, tablesource, tableModel});

                dispatchChartUpdate({chartId, changes});
                updateHighlighted(chartId, traceNum, highlightedRow);
                updateSelected(chartId, selectInfo);
            }
        }
    ).catch(
        (reason) => {
            dispatchError(chartId, traceNum, reason);
        }
    );
}


function addOtherChanges({changes, chartId, traceNum, tablesource, tableModel}) {
    const type = get(getChartData(chartId), `data.${traceNum}.type`, 'scatter');

    if (isScatter2d(type)) {
        addScatterChanges({changes, chartId, traceNum, tablesource, tableModel});
    }
}

/**
 * Firefly added scatter changes
 * @param {Object} p - parameters
 * @param p.changes existing changes
 * @param p.chartId chart id
 * @param p.traceNum
 * @param p.tablesource
 * @param p.tableModel
 */
function addScatterChanges({changes, chartId, traceNum, tablesource, tableModel}) {

    const {mappings} = tablesource;
    const xColumn = getColumn(tableModel, get(mappings, 'x'));
    const xUnit = get(xColumn, 'units', '');
    const yColumn = getColumn(tableModel, get(mappings, 'y'));
    const yUnit = get(yColumn, 'units', '');

    // default axes labels for the first trace (remove surrounding quotes, if any)
    const xLabel = replaceQuotesIfSurrounding(get(mappings, 'x'));
    const yLabel = replaceQuotesIfSurrounding(get(mappings, 'y'));
    const xTipLabel = truncate(xLabel, {length: 20});
    const yTipLabel = truncate(yLabel, {length: 20});

    const colors = get(changes, [`data.${traceNum}.marker.color`]);
    let cTipLabel = isArray(colors) ? get(mappings, 'marker.color') : '';
    if (cTipLabel.length > 20) {
        cTipLabel = 'c';
    }


    const {layout, data, fireflyData} = getChartData(chartId) || {};

    // legend group is used to show/hide traces together
    // highlight and selected traces should have the same legend group as the active scatter
    if (!get(data, `${traceNum}.legendgroup`)) {
        changes[`data.${traceNum}.legendgroup`] = uniqueId('grp');
    }

    if (!get(data, `${traceNum}.type`)) {
        changes[`data.${traceNum}.type`] = 'scatter';
    }

    // set default title if it's the first trace
    // and no title is set by the user
    if (data && data.length === 1) {
        const xAxisLabel = get(layout, 'xaxis.title');
        if (!xAxisLabel) {
            changes['layout.xaxis.title'] = xLabel + (xUnit ? ` (${xUnit})` : '');
        }
        const yAxisLabel = get(layout, 'yaxis.title');
        if (!yAxisLabel) {
            changes['layout.yaxis.title'] = yLabel + (yUnit ? ` (${yUnit})` : '');
        }
    }

    const x = get(changes, [`data.${traceNum}.x`]);
    if (!x) return;

    // handle upper limits: update new or erase old
    let annotations = [];
    const symbol = getTraceSymbol(data, fireflyData, traceNum);
    if (mappings[`fireflyData.${traceNum}.yMax`]) {
        // if there is an upper limit value and no y value, y is set to upper limit value
        let numNewPts = 0;
        let symbolArr = undefined;
        const arrowcolor = get(data, `${traceNum}.marker.color`);
        const {range = [], autorange = true, type: ytype} = get(layout, 'yaxis', {});
        const {type: xtype} = get(layout, 'xaxis', {});
        const reversed = (autorange === 'reversed') || (range[1] < range[0]);
        const sign = reversed ? -1 : 1;



        // per trace annotations
        annotations = changes[`fireflyData.${traceNum}.yMax`].map((v, i) => {

            let yy = parseFloat(v);
            if (Number.isFinite(yy)) {
                // create a point if not present and change its symbol
                if (!Number.isFinite(parseFloat(changes[`data.${traceNum}.y`][i]))) {
                    changes[`data.${traceNum}.y`][i] = changes[`fireflyData.${traceNum}.yMax`][i];
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
                if (xtype === 'log') { xx = Math.log10(xx); }

                if (ytype === 'log') { yy = Math.log10(yy); }

                return {
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
                };
            } else {
                return undefined;
            }
        });
        // set an array of marker symbols
        if (numNewPts > 0) {
            changes[`data.${traceNum}.marker.symbol`] = symbolArr;
            changes[`fireflyData.${traceNum}.marker.symbol`] = symbol;
        }
    }
    // per trace annotations
    changes[`fireflyData.${traceNum}.annotations`] = annotations;

    // set tooltips

    // hoverinfo 'skip' disables hover layer - hence we can not highlight clicking on a point or select points in the chart
    // if we support it, we need to exclude select button from the tools appearing on select
    // also, we might want to disable showing highlighted and selected points in the chart
    if (get(data, `${traceNum}.hoverinfo`, 'text') === 'text') {
        const xErrLow = get(changes, [`data.${traceNum}.error_x.arrayminus`], []);
        const xErrHigh = xErrLow.length > 0 ? get(changes, [`data.${traceNum}.error_x.array`], []) : [];
        const xErr = xErrLow.length > 0 ? [] : get(changes, [`data.${traceNum}.error_x.array`], []);
        const hasXErrors = xErrLow.length > 0 || xErr.length > 0;
        changes[`data.${traceNum}.error_x.visible`] = hasXErrors;
        changes[`data.${traceNum}.error_x.symmetric`] = xErr.length > 0;

        const y = get(changes, [`data.${traceNum}.y`]);
        const yErrLow = get(changes, [`data.${traceNum}.error_y.arrayminus`], []);
        const yErrHigh = yErrLow.length > 0 ? get(changes, [`data.${traceNum}.error_y.array`], []) : [];
        const yErr = yErrLow.length > 0 ? [] : get(changes, [`data.${traceNum}.error_y.array`], []);
        const hasYErrors = yErrLow.length > 0 || yErr.length > 0;

        changes[`data.${traceNum}.error_y.visible`] = hasYErrors;
        changes[`data.${traceNum}.error_y.symmetric`] = yErr.length > 0;

        const text = x.map((xval, idx) => {
            const yval = y[idx];
            const xerr = hasXErrors ? formatError(xval, xErr[idx], xErrLow[idx], xErrHigh[idx]) : '';
            const yerr = hasYErrors ? formatError(yval, yErr[idx], yErrLow[idx], yErrHigh[idx]) : '';
            const cval = isArray(colors) ? `<br> ${cTipLabel} = ${parseFloat(colors[idx])} ` : '';
            const ul = annotations[idx] ? '<br> Upper Limit ' : '';
            return `<span> ${xTipLabel} = ${parseFloat(xval)}${xerr} ${xUnit} <br>` +
                ` ${yTipLabel} = ${parseFloat(yval)}${yerr} ${yUnit} ${ul} ${cval}</span>`;
        });
        changes[`data.${traceNum}.hovertext`] = text;
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
    //        title: colorMapColOrExpr
    //    };
    //}
}

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
        str = ` \u00B1${errNum}`; //Unicode U+00B1 is plusmn
    } else {
        const errHighNum = parseFloat(errHigh);
        if (Number.isFinite(errHighNum)) {
            str += ` \u002B${errHighNum}`;
        }
        const errLowNum = parseFloat(errLow);
        if (Number.isFinite(errLowNum)) {
            if (str) str += ' /';
            str += ` \u2212${errLowNum}`;
        }
    }
    return str;
};
