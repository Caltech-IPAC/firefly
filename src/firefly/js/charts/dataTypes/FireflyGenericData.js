/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, isArray, uniqueId} from 'lodash';
import {COL_TYPE, getTblById, getColumns, getColumn, doFetchTable} from '../../tables/TableUtil.js';
import {cloneRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchChartUpdate, dispatchError, getChartData} from '../ChartsCntlr.js';
import {formatColExpr, getDataChangesForMappings, updateHighlighted, updateSelected, isScatter2d} from '../ChartUtil.js';

/**
 * This function creates table source entries to get plotly chart data from the server
 * (The search processor knows how to handle expressions and eliminates null mapping key)
 *
 */

/**
 * This function creates table source entries to get plotly chart data from the server
 * (The search processor knows how to handle expressions and eliminates null mapping key)
 * For the non-firefly plotly chart types
 * @param traceTS
 * @returns {{options: *, fetchData: fetchData}}
 */
export function getTraceTSEntries({traceTS}) {
    const {mappings} = traceTS || {};

    if (mappings) {
        return {options: mappings, fetchData};

    } else {
        return {};
   }
}

function fetchData(chartId, traceNum, tablesource) {

    const {tbl_id, mappings} = tablesource;
    if (!mappings) { return; }

    const originalTableModel = getTblById(tbl_id);
    const {request, highlightedRow, selectInfo} = originalTableModel;
    const numericCols = getColumns(originalTableModel, COL_TYPE.NUMBER).map((c) => c.name);

    // default behavior
    const sreq = cloneRequest(request, {
        startIdx: 0,
        pageSize: MAX_ROW,
        inclCols: Object.entries(mappings).map(([k,v]) => {
            return `${formatColExpr({colOrExpr: v, quoted: true, colNames: numericCols})} as "${k}"`;
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

    const xLabel = get(mappings, 'x'); // default axis label for the first trace
    const yLabel = get(mappings, 'y'); // default axis label for the first trace
    const xTipLabel = xLabel.length > 20 ? 'x' : xLabel;
    const yTipLabel = yLabel.length > 20 ? 'y' : yLabel;

    const colors = get(changes, [`data.${traceNum}.marker.color`]);
    let cTipLabel = isArray(colors) ? get(mappings, 'marker.color') : '';
    if (cTipLabel.length > 20) {
        cTipLabel = 'c';
    }


    const {layout, data} = getChartData(chartId) || {};

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

    // set tooltips
    const x = get(changes, [`data.${traceNum}.x`]);
    if (!x) return;

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
            return `<span> ${xTipLabel} = ${parseFloat(xval)}${xerr} ${xUnit} <br>` +
                ` ${yTipLabel} = ${parseFloat(yval)}${yerr} ${yUnit} ${cval}</span>`;
        });
        changes[`data.${traceNum}.text`] = text;
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
    if (Number.isFinite(parseFloat(err))) {
        //return ' \u00B1 '+numeral(lowErr).format(fmtLow); //Unicode U+00B1 is plusmn
        str = ` \u00B1${err}`; //Unicode U+00B1 is plusmn
    } else {
        if (Number.isFinite(parseFloat(errHigh))) {
            str += ` \u002B${errHigh}`;
        }
        if (Number.isFinite(parseFloat(errLow))) {
            if (str) str += ' /';
            str += ` \u2212${errLow}`;
        }
    }
    return str;
};
