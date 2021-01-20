/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * Utilities related to charts
 * Created by tatianag on 3/17/16.
 */

import {
    assign, flatten, get, uniqueId, isArray, isEmpty, range, set, isObject, isString, pick, cloneDeep, merge, has,
    isUndefined, pickBy
} from 'lodash';
import shallowequal from 'shallowequal';

import {getAppOptions} from '../core/AppDataCntlr.js';
import {getTblById, isFullyLoaded, isTableLoaded, isColumnType, COL_TYPE, stripColumnNameQuotes, watchTableChanges} from '../tables/TableUtil.js';
import {TABLE_HIGHLIGHT, TABLE_LOADED, TABLE_SELECT, TABLE_SORT} from '../tables/TablesCntlr.js';
import {dispatchLoadTblStats} from './TableStatsCntlr.js';
import {dispatchChartUpdate, dispatchChartHighlighted, dispatchChartSelect, getChartData} from './ChartsCntlr.js';
import {Expression} from '../util/expr/Expression.js';
import {quoteNonAlphanumeric}  from '../util/expr/Variable.js';
import {flattenObject} from '../util/WebUtil.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {getTraceTSEntries as histogramTSGetter} from './dataTypes/FireflyHistogram.js';
import {getTraceTSEntries as heatmapTSGetter} from './dataTypes/FireflyHeatmap.js';
import {getTraceTSEntries as genericTSGetter} from './dataTypes/FireflyGenericData.js';
import Color from '../util/Color.js';
import {MetaConst} from '../data/MetaConst';
import {ALL_COLORSCALE_NAMES, colorscaleNameToVal} from './Colorscale.js';
import {findTableCenterColumns, getSpectrumDM} from '../util/VOAnalyzer.js';

export const DEFAULT_ALPHA = 0.5;

export const SCATTER = 'scatter';
export const HEATMAP = 'heatmap';

export const SELECTED_COLOR = 'rgba(255, 200, 0, 1)';
export const SELECTED_PROPS = {
    name: '__SELECTED',
    marker: {color: SELECTED_COLOR},
    error_x: {color: SELECTED_COLOR},
    error_y: {color: SELECTED_COLOR}
};

export const HIGHLIGHTED_COLOR = 'rgba(255, 165, 0, 1)';
export const HIGHLIGHTED_PROPS = {
    name: '__HIGHLIGHTED',
    marker: {color: HIGHLIGHTED_COLOR, line: {width: 2, color: HIGHLIGHTED_COLOR}}, // increase highlight marker with line border
    error_x: {color: HIGHLIGHTED_COLOR},
    error_y: {color: HIGHLIGHTED_COLOR}
};

const FSIZE = 12;

export const TBL_SRC_PATTERN = /^tables::(.+)/;

/**
 * Does the application only support single trace
 * @returns {*} true, if all charts are single trace
 */
export function singleTraceUI() {
    return get(getAppOptions(), 'charts.singleTraceUI');
}

/**
 * Maximum table rows for scatter chart support, heatmap is created for larger tables
 * @returns {*}
 */
export function getMaxScatterRows() {
    return get(getAppOptions(), 'charts.maxRowsForScatter', 5000);
}


/**
 * For scatter charts, the minimum number of points to use 'scattergl' (Web GL);
 * If the number of points less than this, 'scatter' (SVG) is used.
 * Web GL and SVG traces can be displayed in the same chart.
 * @returns {*}
 */
export function getMinScatterGLRows() {
    return get(getAppOptions(), 'charts.minScatterGLRows', 1000);
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
    const excludeNames = ['ROW_IDX', 'ROW_NUM'];
    cols.forEach((c) => {
        if (isColumnType(c, COL_TYPE.NUMBER)) {
            if (!excludeNames.includes(c.name)) { ncols.push(c); }
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
 * @prop {string}  [xLabel]     label to use with x axis
 * @prop {string}  [yLabel]     label to use with y axis
 * @prop {string}  [xOptions]   comma separated list of x axis options: grid,flip,log
 * @prop {string}  [yOptions]   comma separated list of y axis options: grid,flip,log
 * @prop {string}  [xError]     column or expression for X error
 * @prop {string}  [yError]     column or expression for Y error
 */

/**
 * @summary Convert shallow object with XYPlot parameters to scatter plot parameters object.
 * @param {XYPlotOptions} params - shallow object with XYPlot parameters
 * @returns {Object} - object, used to create Plotly chart
 * @public
 * @function makeXYPlotParams
 * @deprecated
 * @memberof firefly.util.chart
 */
export function makeXYPlotParams(params) {

    const {tbl_id, xCol, yCol, plotStyle, xError, yError, xLabel, yLabel, xOptions, yOptions, chartTitle} = params;

    let data, layout={xaxis:{}, yaxis:{showgrid: false}};

    if (xCol && yCol) {
        const trace = {tbl_id};
        trace.x = `tables::${xCol}`;
        trace.y = `tables::${yCol}`;
        if (xError) { trace.error_x = {array: `tables::${xError}`}; }
        if (yError) { trace.error_y = {array: `tables::${yError}`}; }
        trace.mode = 'markers';
        if (plotStyle) {
            if (plotStyle === 'line') {
                trace.mode = 'lines';
            } else if (plotStyle === 'linepoints') {
                trace.mode = 'lines+markers';
            } 
        }
        data = [trace];
    } else {
        const defProps = getDefaultChartProps(tbl_id) || {};
        if (!isObject(defProps)) { return; }
        data = defProps.data;
        layout = Object.assign(layout, defProps.layout);
    }

    if (xLabel) { layout.xaxis.title.text = xLabel; }
    if (yLabel) { layout.yaxis.title.text = yLabel; }
    if (xOptions) {
        if (xOptions.includes('grid')) { layout.xaxis.showgrid  = true; }
        if (xOptions.includes('flip')) { layout.xaxis.autorange  = 'reversed'; }
        if (xOptions.includes('log')) { layout.xaxis.type  = 'log'; }
    }

    if (yOptions) {
        if (yOptions.includes('grid')) { layout.yaxis.showgrid  = true; }
        if (yOptions.includes('flip')) { layout.yaxis.autorange  = 'reversed'; }
        if (yOptions.includes('log')) { layout.yaxis.type  = 'log'; }
    }
    if (chartTitle) { layout.title.text = chartTitle; }
    return {data, layout};
}


/**
 * @global
 * @public
 * @typedef {Object} HistogramOptions
 * @summary shallow object with histogram parameters
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
 * @deprecated
 * @memberof firefly.util.chart
 */
export function makeHistogramParams(params) {
    const {tbl_id, col, xOptions, yOptions, falsePositiveRate, binWidth} = params;
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
        const options = {
            columnOrExpr: col,
            algorithm,
            fixedBinSizeSelection,
            numBins,
            binWidth,
            falsePositiveRate
        };
        const layout = {xaxis: {}, yaxis: {}};
        if (xOptions) {
            if (xOptions.includes('flip')) { layout.xaxis.autorange  = 'reversed'; }
            if (xOptions.includes('log')) { layout.xaxis.type  = 'log'; }
        }
        if (yOptions) {
            if (yOptions.includes('flip')) { layout.yaxis.autorange  = 'reversed'; }
            if (yOptions.includes('log')) { layout.yaxis.type  = 'log'; }
        }

        return {
            data : [{
                type: 'fireflyHistogram',
                firefly: {
                    tbl_id,
                    options
                }
            }],
            layout
        };
    }
}

/**
 * For a given plotly point index, get rowIdx connecting a given plotly point to the table row.
 * @param traceData
 * @param pointIdx
 * @returns {number}
 */
export function getRowIdx(traceData, pointIdx) {
    // firefly.rowIdx array in the trace data connects plotly points to table row indexes
    return Number(get(traceData, `firefly.rowIdx.${pointIdx}`, pointIdx));
}

/**
 * For a given table row index, get plotly point index
 * @param traceData
 * @param rowIdx
 * @returns {number}
 */
export function getPointIdx(traceData, rowIdx) {
    const rowIdxArray = get(traceData, 'firefly.rowIdx');
    // use double equal in case we compare string to Number
    return rowIdxArray ? rowIdxArray.findIndex((e) => e == rowIdx) : rowIdx;
}

export function isScatter2d(type) {
    return type.includes('scatter') && !type.endsWith('3d');
}


export function clearChartConn({chartId}) {
    const oldTablesources = get(getChartData(chartId), 'tablesources',[]);
    if (Array.isArray(oldTablesources)) {
        oldTablesources.forEach( (traceTS) => {
            if (traceTS._cancel) {
                traceTS._cancel();
                traceTS._cancel = undefined;
            }   // cancel the previous watcher if exists
        });
    }
}

export function newTraceFrom(data, selIndexes, newTraceProps, traceAnnotations) {

    const sdata = cloneDeep(pick(data, ['x', 'y', 'z', 'legendgroup', 'error_x', 'error_y', 'text', 'hovertext', 'marker', 'hoverinfo', 'firefly' ]));
    Object.assign(sdata, {showlegend: false, type: get(data, 'type', 'scatter'), mode: 'markers'});

    // the rowIdx doesn't exist for generic plotly chart case
    if (isScatter2d(get(data, 'type', '')) &&
        !get(sdata, 'firefly.rowIdx') &&
        get(sdata, 'x.length', 0) !== 0) {
        const rowIdx = range(get(sdata, 'x.length')).map(String);
        set(sdata, 'firefly.rowIdx', rowIdx);
    }

    if (isArray(traceAnnotations) && traceAnnotations.length > 0) {
        const annotations = cloneDeep(traceAnnotations);
        const color = get(newTraceProps, 'marker.color');

        flattenAnnotations(annotations).forEach((a) => {a && (a.arrowcolor = color);});
        set(sdata, 'firefly.annotations', annotations);
    }

    // walk through object and replace values where there's an array with only the selected indexes.
    function deepReplace(obj) {
        Object.entries(obj).forEach( ([k,v]) => {
            if (Array.isArray(v) && v.length > selIndexes.length) {
                obj[k] = selIndexes.reduce((p, sIdx) => {
                    p.push(v[sIdx]);
                    return p;
                }, []);
            } else if (isObject(v)) {
                deepReplace(v);
            }
        });
    }
    deepReplace(sdata);
    const flatprops = flattenObject(newTraceProps);
    Object.entries(flatprops).forEach(([k,v]) => set(sdata, k, v));
    return sdata;
}

/**
 * Get trace annotation as a one level deep array
 * @param {array} annotations - trace annotations (there could be none, a single, or an array of annotations per point
 */
export function flattenAnnotations(annotations) {
    if (isArray(annotations)) {
        const filtered = annotations.filter((e) => !isUndefined(e));
        if (filtered.length > 0) {
            // trace annotations can have a single annotation or an array of annotations per point
            return flatten(filtered);
        }
    }
    return [];
}

export function updateSelected(chartId, selectInfo) {
    const selectInfoCls = SelectInfo.newInstance(selectInfo);
    const {data, activeTrace=0} = getChartData(chartId);
    const traceData = data[activeTrace];
    const selIndexes = Array.from(selectInfoCls.getSelected()).map((e)=>getPointIdx(traceData, e));
    if (selIndexes) {
        dispatchChartSelect({chartId, selIndexes});
    }
}

export function updateHighlighted(chartId, traceNum, highlightedRow) {
    const traceData = get(getChartData(chartId), `data.${traceNum}`);
    dispatchChartHighlighted({chartId, highlighted: getPointIdx(traceData,highlightedRow)});
}

export function getDataChangesForMappings({tableModel, mappings, traceNum}) {

    let getDataVal;
    const changes = {};
    changes[`fireflyData.${traceNum}.isLoading`] = !Boolean(tableModel);
    if (tableModel) {
        const cols = tableModel.tableData.columns.map((c) => c.name);
        const transposed = tableModel.tableData.columns.map(() => []);
        const data = get(tableModel, 'tableData.data', []);
        data.forEach((r) => {
            r.map((e, idx) => transposed[idx].push(e));
        });
        // tableModel columns are named as the paths to the trace arrays
        getDataVal = (k,v) => {
            // using plotly attribute path (key in the mappings object) as a column name
            // this makes it possible to use the same column as x and y, for example
            let idx = cols.indexOf(stripColumnNameQuotes(v));
            if (idx < 0) idx = cols.indexOf(k);
            if (idx >= 0) {
                return transposed[idx];
            } else {
                // if value is a numeric constant,
                // we might be able to use it instead of array
                // example: marker.size can be a number or an array
                const numericConstant = parseFloat(v);
                if (!Number.isNaN(numericConstant)) {
                    return numericConstant;
                }
            }
        };
    } else {
        // no tableModel case is for pre-fetch changes
        changes[`fireflyData.${traceNum}.error`] = undefined;
        getDataVal = (k,v) => v;
    }

    if (mappings) {
        Object.entries(mappings).forEach(([k,v]) => {
            const key = k.startsWith('firefly') ? k : `data.${traceNum}.${k}`;
            changes[key] = getDataVal(key,v);
        });
    }

    return changes;
}

/**
 *
 * @param {object} p
 * @param {string} p.chartId
 * @param {object[]} p.data
 */
export function handleTableSourceConnections({chartId, data, fireflyData}) {
    const tablesources = makeTableSources(chartId, data, fireflyData);
    const {tablesources:oldTablesources=[], activeTrace, curveNumberMap=[]} = getChartData(chartId);

    const hasTablesources = Array.isArray(tablesources) && tablesources.find((ts) => !isEmpty(ts));
    if (!hasTablesources) return;

    const numTraces = Math.max(tablesources.length, oldTablesources.length);
    range(numTraces).forEach( (idx) => {  // range instead of for-loop is to avoid the idx+1 JS's closure problem
        let traceTS = tablesources[idx];
        const oldTraceTS = oldTablesources[idx] || {};

        let doUpdate = false;
        if (isEmpty(traceTS)) {
            // no updates to this trace, but shared layout updates might affect this trace
            const {fireflyData:oldFireflyData} = getChartData(chartId);
            const chartDataType = get(oldFireflyData[idx], 'dataType');
            traceTS = Object.assign({}, oldTraceTS, getTraceTSEntries({chartDataType, traceTS: oldTraceTS, chartId, traceNum: idx}));
        } else {
            // if mappings are resolved, we need to get info from old tablesource
            if (!traceTS.fetchData) {
                traceTS = Object.assign({}, oldTraceTS, traceTS);
            }
        }

        if (!tablesourcesEqual(traceTS, oldTraceTS)) {
            if (oldTraceTS && oldTraceTS._cancel) {
                oldTraceTS._cancel(); // cancel the previous watcher if exists
                oldTraceTS._cancel = undefined;
                traceTS._cancel = undefined;
            }
            doUpdate = true;
        } else {
            traceTS = oldTraceTS;
        }

        // make sure table watcher is set for all non-empty table sources
        if (!isEmpty(traceTS) && !traceTS._cancel) {
            //creates a new one.. and save the cancel handle
            if (doUpdate) {
                // fetch data syncs highlighted and selected with the table
                updateChartData(chartId, idx, traceTS);
            } else {
                if (idx === activeTrace && isFullyLoaded(traceTS.tbl_id)) {
                    // update highlighted and selected
                    const tableModel = getTblById(traceTS.tbl_id);
                    const {highlightedRow, selectInfo={}} = tableModel;
                    updateHighlighted(chartId, idx, highlightedRow);
                    updateSelected(chartId, selectInfo);
                }
            }
            traceTS._cancel = setupTableWatcher(chartId, traceTS, idx);
        }
        tablesources[idx] = traceTS;
    });

    const changes = {tablesources};

    // update curveNumberMap if it does not contain all the traces
    if (curveNumberMap.length < tablesources.length) {
        const curveMap = range(tablesources.length).filter((idx) => (idx !== activeTrace));
        curveMap.push(activeTrace);
        changes['curveNumberMap'] = curveMap;
    }

    dispatchChartUpdate({chartId, changes});
}

export function setupTableWatcher(chartId, ts, idx) {
    return watchTableChanges(ts.tbl_id,
        [TABLE_LOADED, TABLE_HIGHLIGHT, TABLE_SELECT],
        (action) => updateChartData(chartId, idx, ts, action),
        uniqueId(`ucd-${ts.tbl_id}-trace`)); // watcher id for debugging
}

function tablesourcesEqual(newTS, oldTS) {

    // checking if the table or mappings options have changed
    // or table watcher has been cancelled
    return get(newTS, 'tbl_id') === get(oldTS, 'tbl_id') &&
        get(newTS, 'resultSetID') === get(oldTS, 'resultSetID') &&
        shallowequal(get(newTS, 'mappings'), get(oldTS, 'mappings')) &&
        shallowequal(get(newTS, 'options'), get(oldTS, 'options'));
}


function updateChartData(chartId, traceNum, tablesource, action={}) {

    // Only Scatter Plot does update on a table sort event.
    if (action.type === TABLE_LOADED && action.payload.invokedBy === TABLE_SORT) {
        const {data} = getChartData(chartId);
        const traceType = get(data, [traceNum, 'type'], 'scatter');
        if (!traceType.includes('scatter')) return;
    }


    // make sure the chart is not yet removed
    if (isEmpty(getChartData(chartId))) { return; }
    const {tbl_id, resultSetID, mappings} = tablesource;
    if (action.type === TABLE_HIGHLIGHT) {
        // ignore if traceNum is not active
        const {activeTrace=0} = getChartData(chartId);
        if (traceNum !== activeTrace) return;
        const {highlightedRow} = action.payload;
        updateHighlighted(chartId, traceNum, highlightedRow);
    } else if (action.type === TABLE_SELECT) {
        const {activeTrace=0} = getChartData(chartId);
        if (traceNum !== activeTrace) return;
        const {selectInfo={}} = action.payload;
        updateSelected(chartId, selectInfo);
    } else {
        if (!isFullyLoaded(tbl_id)) return;
        const tableModel = getTblById(tbl_id);
        dispatchLoadTblStats(tableModel.request);

        const changes = getDataChangesForMappings({mappings, traceNum});

        // save original table file path
        const resultSetIDNow = get(tableModel, 'tableMeta.resultSetID');
        if (resultSetIDNow !== resultSetID) {
            changes[`tablesources.${traceNum}.resultSetID`] = resultSetIDNow;
        }
        if (!isEmpty(changes)) {
            dispatchChartUpdate({chartId, changes});
        }

        // fetch data for both Firefly recognized or unrecognized plotly chart types
        if (tablesource.fetchData) {
            tablesource.fetchData(chartId, traceNum, tablesource);
        }
    }
}



function makeTableSources(chartId, data=[], fireflyData=[]) {

    const convertToDS = (flattenData) =>
                        Object.entries(flattenData)
                                .filter(([,v]) => typeof v === 'string' && v.startsWith('tables::'))
                                .reduce( (p, [k,v]) => {
                                    const [,colExp] = v.match(TBL_SRC_PATTERN) || [];
                                    if (colExp) set(p, ['mappings',k], colExp);
                                    return p;
                                }, {});


    // for some firefly specific chart types the data are
    const currentData = (data.length < fireflyData.length) ? fireflyData : data;

    return currentData.map((d, traceNum) => {
        const fireflyDataFlatten = flattenObject(fireflyData[traceNum] || {}, `fireflyData.${traceNum}`);
        // fireflyData mappings have full path
        const flattenData = assign(flattenObject(data[traceNum] || {}), fireflyDataFlatten);
        const ds = data[traceNum] ? convertToDS(flattenData) : {}; //avoid flattening arrays
        // table id can be a part of fireflyData too
        const tbl_id = get(data, `${traceNum}.tbl_id`) || get(fireflyData, `${traceNum}.tbl_id`);

        if (tbl_id) ds.tbl_id = tbl_id;

        // we use resultSetID to see if the table has changed (sorted, filtered, etc.)
        if (ds.tbl_id) {
            const tableModel = getTblById(ds.tbl_id);
            ds.resultSetID = get(tableModel, 'tableMeta.resultSetID');
        }
        // set up table server request parameters (options) for firefly specific charts
        const chartDataType = get(fireflyData[traceNum], 'dataType');
        if (!isEmpty(ds)) {
            Object.assign(ds, getTraceTSEntries({chartDataType, traceTS: ds, chartId, traceNum}));
        }
        return ds;
    });
}

function getTraceTSEntries({chartDataType, traceTS, chartId, traceNum}) {
    if (chartDataType === 'fireflyHistogram') {
        return histogramTSGetter({traceTS, chartId, traceNum});
    } else if (chartDataType === 'fireflyHeatmap') {
        return heatmapTSGetter({traceTS, chartId, traceNum});
    } else {
        return genericTSGetter({traceTS, chartId, traceNum});
    }
}

// does the default depend on the chart type?
/**
 * set default value for layout and data
 * @param chartData
 * @param resetColor reset color generator for default color assignment of the chart
 */
export function applyDefaults(chartData={}, resetColor = true) {

    const nonPieChart = isEmpty(chartData) || !has(chartData, 'data') || chartData.data.find((d) => get(d, 'type') !== 'pie');
    const noXYAxis = Boolean(!nonPieChart);

    const chartType = get(chartData, ['data', '0', 'type']);
    const hMode = chartType ==='fireflyHistogram'? 'x':'closest';
    const defaultLayout = {
        hovermode: hMode,
        legend: {
            font: {size: FSIZE},
            orientation: 'v',
            yanchor: 'top'
        },
        xaxis: {
            autorange:true,
            showgrid: false,
            lineColor: '#e9e9e9',
            tickwidth: 1,
            ticklen: 5,
            title: {
                font: {
                    size: FSIZE
                }
            },
            ticks: noXYAxis ? '' : 'outside',
            showline: !noXYAxis,
            showticklabels: !noXYAxis,
            zeroline: false,
            exponentformat:'e'
        },
        yaxis: {
            autorange:true,
            showgrid: !noXYAxis,
            lineColor: '#e9e9e9',
            tickwidth: 1,
            ticklen: 5,
            title: {
                font: {
                    size: FSIZE
                }
            },
            ticks: noXYAxis ? '' : 'outside',
            showline: !noXYAxis,
            showticklabels: !noXYAxis,
            zeroline: false,
            exponentformat:'e'
        }
    };

    chartData.layout = merge(defaultLayout, chartData.layout);

    chartData.data && chartData.data.forEach((d, idx) => {
        d.name = defaultTraceName(d, idx, '');

        const type = get(chartData, ['fireflyData', `${idx}.dataType`]) || get(d, 'type', 'scatter');

        if (idx === 0 && resetColor) {        // reset the color iterator
            getNextTraceColor(true);
            getNextTraceColorscale(true);
        }

        type && Object.entries(getDefaultColorAttributes(d, type, idx)).forEach(([k, v]) => set(chartData, k, v));

        // default dragmode is select if box selection is supported
        type && !chartData.layout.dragmode && (chartData.layout.dragmode = isBoxSelectionSupported(type) ? 'select' : 'zoom');
    });
}

export function isBoxSelectionSupported(type) {
    if (!type || !isString(type)) return false;
    return ['heatmap', 'histogram2dcontour', 'histogram2d', 'scatter'].find((e) => type.toLowerCase().includes(e));
}

/**
 * Convert column expression to internal (database) syntax
 * @param {object} p
 * @param {string} p.colOrExpr - column expression
 * @param {boolean} p.quoted - if true, quote variable names
 * @param {string[]} p.colNames - valid column names
 * @returns {*}
 */
export function formatColExpr({colOrExpr, quoted, colNames}) {

    if (!colOrExpr) return;

    // do not change the expression, if it's matching a column name
    if (colNames && colNames.find((c) => c === colOrExpr)) {
        return quoted ? `"${colOrExpr}"` : colOrExpr;
    }

    const expr = new Expression(colOrExpr, colNames);
    if (expr.isValid()) {
        // remove white space
        colOrExpr = expr.getCanonicalInput();

        if (quoted) {
            // quote columns, assuming column names are alpha-numeric
            expr.getParsedVariables().forEach((v) => {
                if (!v.startsWith('"')) {
                    const re = new RegExp('([^A-Za-z\d_"]|^)(' + v + ')([^A-Za-z\d_"]|$)', 'g');
                    while (colOrExpr.match(re)) { // while is needed to handle cases like v*v
                        colOrExpr = colOrExpr.replace(re, '$1"$2"$3'); // add quotes
                    }
                }
            });
            colOrExpr = colOrExpr.replace(/"NULL"/g, 'NULL'); // unquote NULL
        }
    }

    return colOrExpr;
}


// plotly default color (items 0-7) + color-blind friendly colors
export const TRACE_COLORS = [  '#1f77b4', '#2ca02c', '#d62728', '#9467bd',
                               '#8c564b', '#e377c2', '#7f7f7f', '#17becf',
                               '#333333', '#ff3333', '#00ccff', '#336600',
                               '#9900cc', '#ff9933', '#009999', '#66ff33',
                               '#cc9999', '#b22424', '#008fb2', '#244700',
                               '#6b008f', '#b26b24', '#006b6b', '#47b224', '#8F6B6B'];
export const TRACE_COLORSCALE = ALL_COLORSCALE_NAMES;

export function toRGBA(c, alpha) {
    if (!alpha) { alpha = DEFAULT_ALPHA; }
    const [r, g, b, a] = Color.toRGBA(c, alpha);
    return `rgba(${r},${g},${b},${a})`;
}

export function *traceColorGenerator(colorList, isColor) {
    let nextIdx = -1;

    const f = (c) => isColor ? toRGBA(c, DEFAULT_ALPHA) : c;

    while (true) {
        const result = yield (nextIdx === -1) ? '' : f(colorList[nextIdx%colorList.length]);
        result ? nextIdx = -1 : nextIdx++;
    }
}

const nextTraceColor = traceColorGenerator(TRACE_COLORS, true);
const nextTraceColorscale = traceColorGenerator(TRACE_COLORSCALE);
const getNextTraceColor = (b) => nextTraceColor.next(b).value;
const getNextTraceColorscale = (b) => nextTraceColorscale.next(b).value;

/**
 * This function returns default attributes for a new trace (via UI).
 * @param chartId
 * @param type
 * @param traceNum - an object with the default color attributes for a trace
 * @returns {*}
 */
export function getNewTraceDefaults(chartId, type='', traceNum=0) {
    let   retV;

    if (type.includes(SCATTER)) {
        // we only need to set marker color: other color attributes will be set based on marker color
        // this is handled by BasicOptions, which keeps all color attributes in sync with marker color
        const traceColor = defaultTraceColor({}, traceNum, chartId);
        retV = {
            [`data.${traceNum}.type`]: type, //make sure trace type is set
            [`data.${traceNum}.marker.color`]: traceColor,
            [`data.${traceNum}.marker.line`]: 'none',
            [`data.${traceNum}.showlegend`]: true,
            ['layout.xaxis.range']: undefined, //clear out fixed range
            ['layout.yaxis.range']: undefined //clear out fixed range
        };
        colorsOnTypes['scatter'][0].forEach((p) => { retV[`data.${traceNum}.${p}`] = traceColor; } );
    } else if (type.toLowerCase().includes(HEATMAP)) {
        retV = {
            [`data.${traceNum}.showlegend`]: true,
            ['layout.xaxis.range']: undefined, //clear out fixed range
            ['layout.yaxis.range']: undefined //clear out fixed range
        };
        /*
            There are two approaches regarding unset color scale: pick a new one for every trace or
            keep it unset to let Plotly choose the best one for the data.
         */
        // const traceColorscale = TRACE_COLORSCALE[traceNum % TRACE_COLORSCALE.length];
        // const colorscaleVal = colorscaleNameToVal(traceColorscale);
        // if (colorscaleVal) {
        //     retV[`data.${traceNum}.colorscale`] = colorscaleVal;
        // }
        // if (colorscaleVal !== traceColorscale) {
        //     retV[`fireflyData.${traceNum}.colorscale`] = traceColorscale;
        // }
    } else {
        retV = {
            [`data.${traceNum}.marker.color`]: defaultTraceColor({}, traceNum, chartId),
            [`data.${traceNum}.showlegend`]: true
        };
    }
    retV[`data.${traceNum}.name`] = defaultTraceName({}, traceNum, chartId);

    return retV;
}

function defaultTraceName(oneChartData, idx, chartId) {
    let name = get(oneChartData, 'name');
    if (name) { return name; }
    else {
        // make sure that the name is unique
        const {data=[]} = getChartData(chartId);
        let i = idx;
        let unique = false;
        while (!unique) {
            name = `trace ${i}`;
            // make sure the trace name is unique
            if (data.findIndex((d) => (get(d, 'name') === name)) < 0) {
                unique = true;
            }
            i++;
        }
        return name;
    }
}

function defaultTraceColor(oneChartData, idx, chartId) {
    let color = get(oneChartData, 'marker.color');
    if (color) { return color; }
    else {
        // make sure the color is unique
        const {data=[]} = getChartData(chartId);
        let i = idx;
        let unique = false;
        while (!unique) {
            color = toRGBA(TRACE_COLORS[i % TRACE_COLORS.length]);
            // make sure the trace name is unique
            if (data.findIndex((d) => (get(d, 'marker.color') === color)) < 0) {
                unique = true;
            }
            i++;
        }
        return color;
    }
}

export const colorsOnTypes = {
    bar: [['marker.color', 'error_x.color', 'error_y.color']],
    fireflyHistogram: [['marker.color']],
    box: [['marker.color', 'line.color']],
    heatmap: [['colorscale']],
    fireflyHeatmap: [['colorscale']],
    histogram: [['marker.color', 'error_x.color', 'error_y.color']],
    histogram2d: [['colorscale']],
    area: [['marker.color']],
    contour: [['colorscale' ]],
    histogram2dcontour: [['colorscale']],
    surface: [['colorscale']],
    mesh3d: [['colorscale']],
    chororpleth:  [['colorscale']],
    scatter: [['marker.color', 'line.color', 'textfont.color', 'error_x.color', 'error_y.color']],
    scatter3d: [['marker.color', 'line.color', 'textfont.color', 'error_x.color', 'error_y.color']],
    scattergl: [['marker.color', 'line.color',  'textfont.color', 'error_x.color', 'error_y.color']],
    scattergeo: [['marker.color', 'line.color', 'textfont.color']],
    others: [['marker.color']]
};

/**
 * This function is used to apply color attributes to a new chart
 * @param traceData
 * @param type
 * @param idx
 * @returns {{}} an object with color attributes
 */
function getDefaultColorAttributes(traceData, type, idx) {
    if (!type) return {};
    const colorSettingObj = {};

    const colorAttributes = Object.keys(colorsOnTypes).includes(type) ? colorsOnTypes[type] : colorsOnTypes.others;
    const color = getNextTraceColor();
    colorAttributes[0].filter((att) => att.endsWith('color')).forEach((att) => {
        if (!get(traceData, att)) {
            colorSettingObj[`data.${idx}.${att}`] = color;
        }
    });
    /*
        There are two approaches regarding unset color scale: pick a new one for every trace or
        keep it unset to let Plotly choose the best one for the data.
     */
    // const colorscaleName = getNextTraceColorscale();
    // const colorscaleVal =  colorscaleNameToVal(colorscaleName);
    // colorAttributes[0].filter((att) => att.endsWith('colorscale')).forEach((att) => {
    //     if (!get(traceData, att)) {
    //         if (colorscaleVal) {
    //             colorSettingObj[`data.${idx}.${att}`] = colorscaleVal;
    //         }
    //         if (colorscaleName !== colorscaleVal) {
    //             colorSettingObj[`fireflyData.${idx}.${att}`] = colorscaleName;
    //         }
    //     }
    // });
    return colorSettingObj;
}


export function getDefaultChartProps(tbl_id) {

    const tblModel = getTblById(tbl_id);
    const {tableMeta, tableData, totalRows} = tblModel || {};

    // ignore if not loaded or no-data
    if (!isTableLoaded(tblModel) || !totalRows)  return;


    // default chart props can be set in a table attribute
    const defaultChartDef = tableMeta[MetaConst.DEFAULT_CHART_DEF];
    const defaultChartProps = defaultChartDef && JSON.parse(defaultChartDef);
    if (defaultChartProps && defaultChartProps.data) {
        defaultChartProps.data.forEach((e) => e['tbl_id'] = tbl_id);
        return defaultChartProps;
    }

    // for spectra, use custom spectraviwer ..
    const spectrumDM = getSpectrumDM(tblModel);
    if (!isEmpty(spectrumDM)) return sedPlot({tbl_id, spectrumDM});


    let xCol, yCol;
    // for catalogs use lon and lat columns
    const centerColumns = findTableCenterColumns(tblModel);
    xCol = centerColumns?.lonCol;
    yCol = centerColumns?.latCol;
    if (xCol && yCol) {
        return genericXYChart({tbl_id, xCol, yCol, xOptions: 'flip'});
    }

    //otherwise use the first one-two numeric columns
    const numericCols = getNumericCols(tableData.columns);
    if (numericCols?.length > 0) {
        xCol = numericCols[0]?.name;
        yCol = numericCols.length > 1 ? numericCols[1]?.name : xCol;
        return genericXYChart({tbl_id, xCol, yCol});
    }
}

function genericXYChart({tbl_id, xCol, yCol, xOptions}) {
    if (xCol === yCol) {
        return fireflyHistogram({tbl_id, xCol});
    } else {
        return scatterOrHeatmap({tbl_id, xCol, yCol, xOptions});
    }
}

function fireflyHistogram({tbl_id, xCol}) {
    const xColName = quoteNonAlphanumeric(xCol);
    return {
        data: [{
            type: 'fireflyHistogram',
            firefly: {
                tbl_id,
                options: {
                    algorithm: 'fixedSizeBins',
                    fixedBinSizeSelection: 'numBins',
                    numBins: 20,
                    columnOrExpr: `${xColName}`
                }
            },
            name: `${xColName}`
        }]
    };
}

function scatterOrHeatmap({tbl_id, xCol, yCol, xOptions}) {
    const {totalRows} = getTblById(tbl_id) || {};

    const xColName = quoteNonAlphanumeric(xCol);
    const yColName = quoteNonAlphanumeric(yCol);
    // scatter that converts into heatmap and back depending on the number of points
    const colorscaleName = 'GreySeq';
    const colorscale = colorscaleNameToVal(colorscaleName);
    const autorange = xOptions?.includes('flip') ? 'reversed' : 'true';

    return {
        data: [{
            tbl_id,
            type: totalRows >= getMinScatterGLRows() ? 'scattergl' : 'scatter',
            mode: 'markers',
            x: xCol && `tables::${xColName}`,
            y: yCol && `tables::${yColName}`,
            colorscale,
            firefly: {
                scatterOrHeatmap: true,
                colorscale: colorscaleName
            }
        }],
        layout: {
            xaxis: {autorange},
            yaxis: {showgrid: false}
        }
    };

}

function sedPlot({tbl_id, spectrumDM}) {
    const {totalRows} = getTblById(tbl_id) || {};

    const error = ({statError, statErrLow, statErrHigh}) => {
        let arrayminus = statErrLow || statError;
        let array = statErrHigh || statError;

        if (array === arrayminus) arrayminus = undefined;
        arrayminus  = arrayminus && `tables::${arrayminus}`;
        array       = array && `tables::${array}`;

        return pickBy({arrayminus, array});
    };

    const type = totalRows >= getMinScatterGLRows() ? 'scattergl' : 'scatter';

    const {spectralAxis, fluxAxis} = spectrumDM || {};

    const xColName = quoteNonAlphanumeric(spectralAxis?.value);
    const yColName = quoteNonAlphanumeric(fluxAxis?.value);

    const x = xColName && `tables::${xColName}`;
    const y = yColName && `tables::${yColName}`;
    const error_x = error(spectralAxis || {});
    const error_y = error(fluxAxis || {});

    const xLabel = spectralAxis?.unit ? `v [${spectralAxis?.unit}]` : 'v';
    const yLabel = fluxAxis?.unit ? `Fv [${fluxAxis?.unit}]` : 'Fv';

    const firefly = { dataType: 'SED' };

    return {
        data: [ pickBy({tbl_id, type, x, y, error_x, error_y, firefly, mode: 'markers'}, (a) => !isEmpty(a))],
        layout: {
            xaxis: {
                title: xLabel,
                type: 'log'
            },
            yaxis: {
                title: yLabel,
                type: 'log'
            }
        }
    };
}