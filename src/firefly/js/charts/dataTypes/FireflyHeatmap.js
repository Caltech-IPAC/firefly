/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, isUndefined} from 'lodash';
import {getTblById, getColumn, cloneRequest, doFetchTable, MAX_ROW} from '../../tables/TableUtil.js';
import {dispatchChartUpdate, dispatchChartHighlighted, getChartData} from '../ChartsCntlr.js';
import {getPointIdx, updateSelected} from '../ChartUtil.js';
import {serializeDecimateInfo, parseDecimateKey} from '../../tables/Decimate.js';

/**
 * This function creates table source entries to get firefly scatter and error data from the server
 * (The search processor knows how to handle expressions and eliminates null x or y)
 *
 * @param p
 * @param p.traceTS
 * @param p.chartId
 * @param p.traceNum
 */
export function getTraceTSEntries({traceTS, chartId, traceNum}) {
    const {mappings} = traceTS;
    const {fireflyData, fireflyLayout} = getChartData(chartId) || {};
    // server call parameters
    const xbins = get(fireflyData, `${traceNum}.nbins.x`);
    const ybins = get(fireflyData, `${traceNum}.nbins.y`);
    let maxbins = 10000;
    let xyratio = get(fireflyLayout, 'xyratio', 1.0);
    if (xbins && ybins) {
        maxbins = xbins * ybins;
        xyratio = xbins/ybins;
    }

    //TODO: take into account zoom? and boundaries
    const {xmin, xmax, ymin, ymax} = {};

    const options = {
        xColOrExpr: get(mappings, 'x'),
        yColOrExpr: get(mappings, 'y'),
        maxbins, xyratio, xmin, xmax, ymin, ymax
    };

    return {options, fetchData};
}

function fetchData(chartId, traceNum, tablesource) {

    const {tbl_id, options, mappings} = tablesource;
    const tableModel = getTblById(tbl_id);
    const {request, highlightedRow, selectInfo} = tableModel;

    const {xColOrExpr, yColOrExpr, maxbins, xyratio, xmin, xmax, ymin, ymax} = options;
    // min rows for decimation is 0
    const req = cloneRequest(request, {startIdx: 0, pageSize: MAX_ROW,
        'decimate' : serializeDecimateInfo(xColOrExpr, yColOrExpr, maxbins, xyratio, xmin, xmax, ymin, ymax, 0)});

    doFetchTable(req).then((tableModel) => {
        if (tableModel.tableData && tableModel.tableData.data) {

            const changes = getChanges({tableModel, mappings, chartId, traceNum});
            dispatchChartUpdate({chartId, changes});

            // TODO: what should happen on table highlight or cell click?
        }
    }).catch(
        (reason) => {
            console.error(`Failed to fetch heatmap data for ${chartId} trace ${traceNum}: ${reason}`);
        }
    );
}

function getChanges({tableModel, mappings, chartId, traceNum}) {

    const {tableMeta} = tableModel;
    const decimateKey = tableMeta['decimate_key'];

    if (!decimateKey) {
        // ERROR - table is not decimated
        console.error(`Failed to get heatmap data for ${chartId} trace ${traceNum}: decimateKey not found in returned data`);
        return {};
    }

    const xLabel = get(mappings, 'x'); // default axis label for the first trace
    const yLabel = get(mappings, 'y'); // default axis label for the first trace
    const xTipLabel = xLabel.length > 20 ? 'x' : xLabel;
    const yTipLabel = yLabel.length > 20 ? 'y' : yLabel;

    const xColumn = getColumn(tableModel, get(mappings, 'x'));
    const xUnit = get(xColumn, 'units', '');
    const yColumn = getColumn(tableModel, get(mappings, 'y'));
    const yUnit = get(yColumn, 'units', '');

    const {xMin:x0, xUnit:dx, nX, yMin:y0, yUnit:dy, nY} = parseDecimateKey(decimateKey);

    // function to get center point of the bin
    const getCenter = (xval,yval) => {
        return {
            // bitwise operators convert operands to 32-bit integer
            // hence they can be used as a fast way to truncate a float to an integer
            x: x0+(~~((xval-x0)/dx)+0.5)*dx,
            y: y0+(~~((yval-y0)/dy)+0.5)*dy
        };
    };

    const x = [];
    const y = [];
    const z = [];
    const text = []; // tooltips
    const toRowIdx = new Map(); // maps decimate key (bin identifier in the form 'x:y') to rowIdx

    tableModel.tableData.data.forEach((r) => {
        // colNames = ['x', 'y', 'rowIdx', 'weight', 'decimate_key'];
        const [xval, yval, rowIdx, weight, decimateKey] = r;
        const centerPt = getCenter(xval, yval);
        x.push(centerPt.x);
        y.push(centerPt.y);
        z.push(weight);
        text.push(`<span> ${xTipLabel} = ${xval} ${xUnit} <br>` +
                    ` ${yTipLabel} = ${yval} ${yUnit} <br>` +
                    ` represents ${weight} points</span>`);
        toRowIdx.set(decimateKey, rowIdx);
    });

    //to avoid showing variable cell heatmap,
    //make sure the x and y values are populated for the empty bins
    for (let i=0; i<nX; i++ ) {
        for (let j = 0; j < nY; j++) {
            if (!toRowIdx.has(`${i}:${j}`)) {
                x.push(x0+(i+0.5)*dx);
                y.push(y0+(j+0.5)*dy);
                z.push(null); // if undefined, downloadImage might show heatmap with variable bins
            }
        }
    }

    const changes = {
        // it's not clear how to provide the tooltips
        // if alternative coordinate space is used
        //[`data.${traceNum}.x0`]: x0,
        //[`data.${traceNum}.y0`]: y0,
        //[`data.${traceNum}.dx`]: dx,
        //[`data.${traceNum}.dy`]: dy,
        [`data.${traceNum}.x`]: x,
        [`data.${traceNum}.y`]: y,
        [`data.${traceNum}.z`]: z,
        [`data.${traceNum}.text`]: text,
        [`data.${traceNum}.hoverinfo`]: 'text',
        [`fireflyData.${traceNum}.toRowIdx`]: toRowIdx
    };

    const {layout, data} = getChartData(chartId) || {};

    if (data && data.length===1) {
        // set default title if it's the first trace
        // and no title is set by the user
        const xAxisLabel = get(layout, 'xaxis.title');
        if (!xAxisLabel) {
            changes['layout.xaxis.title'] = xLabel + (xUnit ? ` (${xUnit})` : '');
        }
        const yAxisLabel = get(layout, 'yaxis.title');
        if (!yAxisLabel) {
            changes['layout.yaxis.title'] = yLabel + (yUnit ? ` (${yUnit})` : '');
        }
    }

    // color bar
    if (!get(data, `${traceNum}.colorbar`)) {
        changes[`data.${traceNum}.colorbar.thickness`] = 10;
        changes[`data.${traceNum}.colorbar.outlinewidth`] = 0;
        changes[`data.${traceNum}.colorbar.title`] = 'pts'; //`(${traceNum})`;
    }

    if (!get(data, `${traceNum}.colorbar.x`)) {
        const yside = get(layout, 'yaxis.side');
        const yOpposite = (yside === 'right');
        const colorBarIdx = data.filter((d) => get(d, 'colorbar') && get(d, 'showscale', true)).length;
        addColorbarChanges(changes, yOpposite, traceNum, colorBarIdx);
    }

    return changes;
}

export function addColorbarChanges(changes, yOpposite, traceNum, idx) {
    const f = 0.08; // colorbar shift in plot fractions
    changes[`data.${traceNum}.colorbar.xanchor`] = yOpposite ? 'right' : 'left';
    changes[`data.${traceNum}.colorbar.x`]  = yOpposite ? -0.02-idx*f : (1.02+idx*f);
}