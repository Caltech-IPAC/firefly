/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, isArray} from 'lodash';
import {getTblById, getColumn, doFetchTable, stripColumnNameQuotes} from '../../tables/TableUtil.js';
import {makeTableFunctionRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchChartUpdate, dispatchError, getChartData} from '../ChartsCntlr.js';
import {isScatter2d, getMaxScatterRows, singleTraceUI} from '../ChartUtil.js';
import {serializeDecimateInfo, parseDecimateKey} from '../../tables/Decimate.js';
import BrowserInfo from  '../../util/BrowserInfo.js';
import {formatColExpr} from '../ChartUtil.js';
import {colorscaleNameToVal, OneColorSequentialCS, PlotlyCS} from '../Colorscale.js';
import {cloneRequest} from '../../tables/TableRequestUtil.js';
import {COL_TYPE, getColumns} from '../../tables/TableUtil.js';
import {getTraceTSEntries as genericTSGetter} from './FireflyGenericData.js';
import {DECIMATE_TAG} from '../../tables/Decimate.js';

const DEFBINS = 100;
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

    if (!mappings) return {};

    const {fireflyData, fireflyLayout} = getChartData(chartId) || {};
    // server call parameters
    const xbins = get(fireflyData, `${traceNum}.nbins.x`);
    const ybins = get(fireflyData, `${traceNum}.nbins.y`);
    const xNum = xbins ? Number(xbins) : DEFBINS;
    const yNum = ybins ? Number(ybins) : DEFBINS;

    const maxbins = xNum * yNum;
    const xyratio = xNum/yNum;

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

    const {tbl_id, options} = tablesource;
    const tableModel = getTblById(tbl_id);

    // if heatmap is used to represent scatter
    // and the number of rows falls below a threshhold,
    // scatter chart should be produced
    const {fireflyData} = getChartData(chartId);
    const scatterOrHeatmap = get(fireflyData, [traceNum, 'scatterOrHeatmap']);
    const totalRows= get(tableModel, 'totalRows');
    if (scatterOrHeatmap && totalRows <= getMaxScatterRows()) {
        const {options:scatterOptions, fetchData:scatterFetchData} = genericTSGetter({traceTS:tablesource, chartId, traceNum});
        const scatterTS = Object.assign({}, tablesource, {options: scatterOptions});
        return scatterFetchData(chartId, traceNum, scatterTS);
    }

    const numericCols = getColumns(tableModel, COL_TYPE.NUMBER).map((c) => c.name);
    const {request} = tableModel;

    const {xColOrExpr, yColOrExpr, maxbins, xyratio, xmin, xmax, ymin, ymax} = options;

    const xColName = numericCols.includes(xColOrExpr) ? xColOrExpr : 'xColumnExpression';
    const asX = (xColName === xColOrExpr) ? '' : ` as "${xColName}"`;
    const yColName = numericCols.includes(yColOrExpr) ? yColOrExpr : 'yColumnExpression';
    const asY = (yColName === yColOrExpr) ? '' : ` as "${yColName}"`;

    // inclCols should not have duplicates
    const sameXY = xColName === yColName;
    const sreq = cloneRequest(request, {
        inclCols: `${formatColExpr({colOrExpr:xColOrExpr, quoted: true, colNames: numericCols})}${asX}`+
        (sameXY ? '' : `,${formatColExpr({colOrExpr:yColOrExpr, quoted: true, colNames: numericCols})}${asY}`)
    });

    // min rows for decimation is 0
    const req = makeTableFunctionRequest(sreq, 'DecimateTable', 'heatmap',
        {decimate: serializeDecimateInfo(xColName, yColName, maxbins, xyratio, xmin, xmax, ymin, ymax, 0), pageSize: MAX_ROW});

    doFetchTable(req).then((tableModel) => {
        if (tableModel.error) {
            dispatchError(chartId, traceNum, tableModel.error);
            return;
        }

        if (tableModel.tableData && tableModel.tableData.data) {

            const changes = getChanges({tableModel, tablesource, chartId, traceNum});
            changes[`fireflyData.${traceNum}.isLoading`] = false;
            dispatchChartUpdate({chartId, changes});

            // TODO: what should happen on table highlight or cell click?
        } else {
            dispatchError(chartId, traceNum, 'No data');
        }
    }).catch(
        (reason) => {
            dispatchError(chartId, traceNum, reason);
        }
    );
}

function getChanges({tableModel, tablesource, chartId, traceNum}) {
    const {mappings} = tablesource;
    const {tableMeta} = tableModel;
    const decimateKey = tableMeta['decimate_key'];

    if (!decimateKey) {
        // ERROR - table is not decimated
        console.error(`Failed to get heatmap data for ${chartId} trace ${traceNum}: decimateKey not found in returned data`);
        return {};
    }

    // default axes labels for the first trace (remove surrounding quotes, if any)
    const xLabel = stripColumnNameQuotes(get(mappings, 'x'));
    const yLabel = stripColumnNameQuotes(get(mappings, 'y'));
    const xTipLabel = xLabel.length > 20 ? xLabel.substring(0,18)+'...' : xLabel;
    const yTipLabel = yLabel.length > 20 ? yLabel.substring(0,18)+'...' : yLabel;

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
        text.push(`<span> ${xTipLabel} = ${parseFloat(xval)} ${xUnit} <br>` +
                    ` ${yTipLabel} = ${parseFloat(yval)} ${yUnit} <br>` +
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
        [`data.${traceNum}.hoverinfo`]: 'text'
    };

    const chartData = getChartData(chartId);
    const {layout, data, fireflyData} = chartData;

    // check for scatter represented by heatmap
    if (isScatter2d(get(data, `${traceNum}.type`, 'scatter'))) {
        // moving from scatter to heatmap representation
        changes[`fireflyData.${traceNum}.scatterOrHeatmap`] = true;
        // clean arrays set in scatter mode
        changes[`data.${traceNum}.hoverText`] = undefined;
        // set colorscale if it was not set before
        const colorscale = get(data, `${traceNum}.colorscale`);
        if (!colorscale || (!isArray(colorscale) && !PlotlyCS.includes(colorscale))) {
            const colorscaleName = OneColorSequentialCS[traceNum % OneColorSequentialCS.length];
            changes[`data.${traceNum}.colorscale`] = colorscaleNameToVal(colorscaleName);
            changes[`fireflyData.${traceNum}.colorscale`] = colorscaleName;
        }
        // tablesource options have changed
        const {options} = tablesource;
        changes[`tablesources.${traceNum}.options`] = options;
    }

    if (data && data.length===1) {
        // set default title if it's the first trace
        // and no title is set by the user
        const xAxisLabel = get(layout, 'xaxis.title.text');
        if (!xAxisLabel) {
            changes['layout.xaxis.title.text'] = xLabel + (xUnit ? ` (${xUnit})` : '');
        }
        const yAxisLabel = get(layout, 'yaxis.title.text');
        if (!yAxisLabel) {
            changes['layout.yaxis.title.text'] = yLabel + (yUnit ? ` (${yUnit})` : '');
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
            if (singleTraceUI() || (data?.length===1)) {
                changes[`data.${traceNum}.colorbar.title.text`] = 'pts';
            } else {
                changes[`data.${traceNum}.colorbar.title.text`] = get(data, `${traceNum}.name`, 'pts');
            }
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

    // update xbins or ybins in fireflyData
    ['x', 'y'].forEach((axis) => {
        const metaKey = `${DECIMATE_TAG}.${axis.toUpperCase()}BINS`;

        if (tableMeta[metaKey]) {
            const binPath = `${traceNum}.nbins.${axis}`;

            if (get(fireflyData, binPath) !== tableMeta[metaKey]) {
                changes[`fireflyData.${binPath}`] = tableMeta[metaKey];
            }
        }
    });


    return changes;
}

export function hasFireflyColorbar(chartId, traceNum) {
    const {fireflyData=[]} = getChartData(chartId);
    return Boolean(get(fireflyData, `${traceNum}.fireflyColorbar`));
}

/**
 * Returns object with changes
 * @param p
 * @param p.data
 * @param p.fireflyData
 * @param p.layout
 * @return changes
 */
export function adjustColorbars({data, fireflyData, layout}) {
    if (data) {
        const changes = {};
        const nbars = data.filter((d) => get(d, 'colorbar') && get(d, 'showscale', true)).length;
        const yside = get(layout, 'yaxis.side');
        const yOpposite = (yside === 'right');
        let cnt = 1;
        data.forEach((d, i) => {
            if (get(fireflyData, `${i}.fireflyColorbar`) && get(d, 'colorbar') && get(d, 'showscale', true)) {
                addColorbarLengthChanges(changes, yOpposite, nbars, i, cnt);
                cnt++;
            }
        });
        return changes;
    }
}

function addColorbarLengthChanges(changes, yOpposite, nbars, traceNum, cnt) {
    changes[`data.${traceNum}.colorbar.yanchor`] = 'bottom';
    changes[`data.${traceNum}.colorbar.len`] = 1/nbars;
    changes[`data.${traceNum}.colorbar.y`] = 1-cnt/nbars; // first trace on top
    addColorbarChanges(changes, yOpposite, traceNum);
    changes['layout.legend.orientation'] = 'h'; // horizontal legend at the bottom
    changes[`fireflyData.${traceNum}.fireflyColorbar`] = true;
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