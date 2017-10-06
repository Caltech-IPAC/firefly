/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get} from 'lodash';
import {getTblById, getColumn, cloneRequest, doFetchTable, MAX_ROW} from '../../tables/TableUtil.js';
import {dispatchChartUpdate, dispatchError, getChartData} from '../ChartsCntlr.js';
import {serializeDecimateInfo, parseDecimateKey} from '../../tables/Decimate.js';
import BrowserInfo from  '../../util/BrowserInfo.js';

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

    //should we take into account zoom?
    const xmin = get(fireflyLayout, 'xaxis.min');
    const xmax = get(fireflyLayout, 'xaxis.max');
    const ymin = get(fireflyLayout, 'yaxis.min');
    const ymax = get(fireflyLayout, 'yaxis.max');

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
    const {request} = tableModel;

    const {xColOrExpr, yColOrExpr, maxbins, xyratio, xmin, xmax, ymin, ymax} = options;
    // min rows for decimation is 0
    const req = cloneRequest(request, {startIdx: 0, pageSize: MAX_ROW,
        'decimate' : serializeDecimateInfo(xColOrExpr, yColOrExpr, maxbins, xyratio, xmin, xmax, ymin, ymax, 0)});

    doFetchTable(req).then((tableModel) => {
        if (tableModel.tableData && tableModel.tableData.data) {

            const changes = getChanges({tableModel, mappings, chartId, traceNum});
            changes[`fireflyData.${traceNum}.isLoading`] = false;
            dispatchChartUpdate({chartId, changes});

            // TODO: what should happen on table highlight or cell click?
        }
    }).catch(
        (reason) => {
            //console.error(`Failed to fetch heatmap data for ${chartId} trace ${traceNum}: ${reason}`);
            dispatchError(chartId, traceNum, reason);
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
        [`data.${traceNum}.type`]: 'heatmap',
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
    // this is the default behavior for new heatmap trace with no colorbar props
    // each new default colorbar affects positioning of the previous
    if (!get(data, `${traceNum}.colorbar`)) {
        changes[`data.${traceNum}.colorbar.thickness`] = 10;
        changes[`data.${traceNum}.colorbar.outlinewidth`] = 0;
        // colorbar.title is causing hover text display issues in Firefox
        // see https://github.com/plotly/plotly.js/issues/2003
        if (!BrowserInfo.isFirefox()) {
            changes[`data.${traceNum}.colorbar.title`] = get(data, `${traceNum}.name`, 'pts');
        }

        const nbars = data.filter((d) => get(d, 'colorbar') && get(d, 'showscale', true)).length + 1;

        const yside = get(layout, 'yaxis.side');
        const yOpposite = (yside === 'right');
        let cnt = 1;
        let traceNotHandled = true;
        data.forEach( (d, i) => {
            if (get(d, 'colorbar') && get(d, 'showscale', true)) {
                // if the trace data come out of order, we still want colorbars ordered
                if (traceNum<i && traceNotHandled) {
                    addColorbarLengthChanges(changes, yOpposite, nbars, traceNum, cnt);
                    traceNotHandled = false;
                    cnt++;
                }
                addColorbarLengthChanges(changes, yOpposite, nbars, i, cnt);
                cnt++;
            }
        });
        if (traceNotHandled) {
            addColorbarLengthChanges(changes, yOpposite, nbars, traceNum, cnt);
        }
    }

    return changes;
}

function addColorbarLengthChanges(changes, yOpposite, nbars, traceNum, cnt) {
    changes[`data.${traceNum}.colorbar.yanchor`] = 'bottom';
    changes[`data.${traceNum}.colorbar.len`] = 1/nbars;
    changes[`data.${traceNum}.colorbar.y`] = 1-cnt/nbars; // first trace on top
    addColorbarChanges(changes, yOpposite, traceNum);
    changes['layout.legend.orientation'] = 'h'; // horizontal legend at the bottom
}

// colorbar needs to be on the opposite size of the axis
// x is between -2 and 3, negative if yaxis is on the left (default)
// positive and bigger than 1 when yaxis is on the right (opposite)
export function addColorbarChanges(changes, yOpposite, traceNum, x=1.02) {
    if (yOpposite) {
        changes[`data.${traceNum}.colorbar.xanchor`] = 'right';
        changes[`data.${traceNum}.colorbar.x`] = x>0 ? 1-x : x;
    } else {
        changes[`data.${traceNum}.colorbar.xanchor`] = 'left';
        changes[`data.${traceNum}.colorbar.x`] = x>0 ? x : 1-x;
    }
}