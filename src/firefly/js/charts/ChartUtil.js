/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * Utilities related to charts
 * Created by tatianag on 3/17/16.
 */
import {get, uniqueId, isUndefined, omitBy, zip, isEmpty, range, set, isObject, pick, cloneDeep, merge, isNil, has} from 'lodash';
import shallowequal from 'shallowequal';

import {flux} from '../Firefly.js';
import {getAppOptions} from '../core/AppDataCntlr.js';
import {getTblById, getColumnIdx, getCellValue, isFullyLoaded, watchTableChanges} from '../tables/TableUtil.js';
import {TABLE_HIGHLIGHT, TABLE_LOADED, TABLE_SELECT, TABLE_REMOVE} from '../tables/TablesCntlr.js';
import {dispatchChartUpdate, dispatchChartHighlighted, dispatchChartSelect, getChartData} from './ChartsCntlr.js';
import {Expression} from '../util/expr/Expression.js';
import {logError, flattenObject} from '../util/WebUtil.js';
import {UI_PREFIX} from './ChartsCntlr.js';
import {ScatterOptions} from './ui/options/ScatterOptions.jsx';
import {HeatmapOptions} from './ui/options/HeatmapOptions.jsx';
import {FireflyHistogramOptions} from './ui/options/FireflyHistogramOptions.jsx';
import {HistogramOptions} from './ui/options/PlotlyHistogramOptions.jsx';
import {BasicOptions} from './ui/options/BasicOptions.jsx';
import {ScatterToolbar, BasicToolbar} from './ui/PlotlyToolbar';
import {SelectInfo} from '../tables/SelectInfo.js';
import {getTraceTSEntries as scatterTSGetter} from './dataTypes/FireflyScatter.js';
import {getTraceTSEntries as histogramTSGetter} from './dataTypes/FireflyHistogram.js';
import {getTraceTSEntries as heatmapTSGetter} from './dataTypes/FireflyHeatmap.js';
import {getTraceTSEntries as genericTSGetter} from './dataTypes/FireflyGenericData.js';

export const SCATTER = 'scatter';
export const HEATMAP = 'heatmap';
export const HISTOGRAM = 'histogram';

export const SELECTED_COLOR = '#ffc800';
export const SELECTED_PROPS = {
    name: '__SELECTED',
    marker: {color: SELECTED_COLOR},
    error_x: {color: SELECTED_COLOR},
    error_y: {color: SELECTED_COLOR}
};

export const HIGHLIGHTED_COLOR = '#ffa500';
export const HIGHLIGHTED_PROPS = {
    name: '__HIGHLIGHTED',
    marker: {color: HIGHLIGHTED_COLOR, line: {width: 2, color: '#ffa500'}}, // increase highlight marker with line border
    error_x: {color: HIGHLIGHTED_COLOR},
    error_y: {color: HIGHLIGHTED_COLOR}
};

const FSIZE = 12;

export const TBL_SRC_PATTERN = /^tables::(.+)/;

export function isPlotly() {
    return get(getAppOptions(), 'charts.chartEngine')==='plotly';
}

/**
 * This method returns an object with the keys x,y,highlightedRow
 *
 * @param {XYPlotParams} xyPlotParams
 * @param {string} tblId
 * @returns {{x: number, y: number, rowIdx}}
 */
export function getHighlighted(xyPlotParams, tblId) {

    const tableModel = getTblById(tblId);
    if (tableModel && xyPlotParams) {
        const rowIdx = tableModel.highlightedRow;
        const highlighted = {rowIdx};
        [
            {n:'x',v:xyPlotParams.x.columnOrExpr},
            {n:'y',v:xyPlotParams.y.columnOrExpr},
            {n:'xErr', v:xyPlotParams.x.error},
            {n:'xErrLow', v:xyPlotParams.x.errorLow},
            {n:'xErrHigh', v:xyPlotParams.x.errorHigh},
            {n:'yErr', v:xyPlotParams.y.error},
            {n:'yErrLow', v:xyPlotParams.y.errorLow},
            {n:'yErrHigh', v:xyPlotParams.y.errorHigh}
        ].map((entry) => {
            if (entry.v) {
                highlighted[entry.n] = getColOrExprValue(tableModel, rowIdx, entry.v);
            }
        });
        return highlighted;
    }
}

/**
 * This method returns the value of the column cell or an expression from multiple column cells in a given row
 *
 * @param {TableModel} tableModel - table model
 * @param {number} rowIdx - row index in the table
 * @param {string} colOrExpr - column name or expression
 * @returns {number} value of the column or expression in the given row
 */
export function getColOrExprValue(tableModel, rowIdx, colOrExpr) {
    if (tableModel) {
        var val;
        if (getColumnIdx(tableModel, colOrExpr) >= 0) {
            val = getCellValue(tableModel, rowIdx, colOrExpr);
            val = isFinite(parseFloat(val)) ? Number(val) : Number.NaN;
        } else {
            val = getExpressionValue(tableModel, rowIdx, colOrExpr);
        }
        return val;
    }
}

function getExpressionValue(tableModel, rowIdx, strExpr) {

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
 * @prop {string}  [plotStyle]  points, linepoints, line
 * @prop {string}  [sortColOrExpr] sort column or expression (when line plot is requested
 * @prop {number}  [xyRatio]    aspect ratio (must be between 1 and 10), if not defined the chart will fill all available space
 * @prop {string}  [stretch]    'fit' to fit plot into available space or 'fill' to fill the available width (applied when xyPlotRatio is defined)
 * @prop {string}  [xLabel]     label to use with x axis
 * @prop {string}  [yLabel]     label to use with y axis
 * @prop {string}  [xUnit]      unit for x axis
 * @prop {string}  [yUnit]      unit for y axis
 * @prop {string}  [xOptions]   comma separated list of x axis options: grid,flip,log
 * @prop {string}  [yOptions]   comma separated list of y axis options: grid,flip,log
 * @prop {string}  [xError]     column or expression for X error
 * @prop {string}  [yError]     column or expression for Y error
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
    const {xCol, yCol, xError, yError, xLabel, yLabel, xUnit, yUnit, xOptions, yOptions, plotStyle, sortColOrExpr, xyRatio, stretch} = params;
    const xyPlotParams = xCol && yCol ?
    omitBy({
        plotStyle,
        sortColOrExpr,
        xyRatio,
        stretch,
        x : omitBy({ columnOrExpr : xCol, error: xError, label : xLabel, unit : xUnit, options : xOptions}, isUndefined),
        y : omitBy({ columnOrExpr : yCol, error: yError, label : yLabel, unit : yUnit, options : yOptions}, isUndefined)
    }, isUndefined) : undefined;
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
    const {col, xOptions, yOptions, falsePositiveRate, binWidth} = params;
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
        const histogramParams =
        {
            columnOrExpr: col,
            algorithm,
            fixedBinSizeSelection,
            numBins,
            binWidth,
            falsePositiveRate,
            x: xOptions||'',
            y: yOptions||''
        };
        return histogramParams;
    }
}

export function getChartUi(chartId) {
    return get(flux.getState(), `${UI_PREFIX}.${chartId}`);
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

export function getOptionsUI(chartId) {
    // based on chartData, determine what options to display
    const {data, fireflyData, activeTrace=0} = getChartData(chartId);
    const type = get(data, [activeTrace, 'type'], 'scatter');
    const dataType = get(fireflyData, [activeTrace, 'dataType'], '');
    if (dataType === 'fireflyScatter' || isScatter2d(type)) {
        return ScatterOptions;
    } else if (type === 'histogram') {
        return HistogramOptions;
    } else if (dataType === 'fireflyHistogram') {
            return FireflyHistogramOptions;
    } else if (dataType === 'fireflyHeatmap') {
            return HeatmapOptions;
    } else {
        return BasicOptions;
    }
}

export function getToolbarUI(chartId, activeTrace=0) {
    const {data} =  getChartData(chartId);
    const type = get(data, [activeTrace, 'type'], '');
    if (isScatter2d(type)) {
        return ScatterToolbar;
    } else {
        return BasicToolbar;
    }
}

export function clearChartConn({chartId}) {
    var oldTablesources = get(getChartData(chartId), 'tablesources',[]);
    if (Array.isArray(oldTablesources)) {
        oldTablesources.forEach( (traceTS) => {
            if (traceTS._cancel) traceTS._cancel();   // cancel the previous watcher if exists
        });
    }
}

export function newTraceFrom(data, selIndexes, newTraceProps) {

    const sdata = cloneDeep(pick(data, ['x', 'y', 'z', 'error_x', 'error_y', 'text', 'marker', 'hoverinfo', 'firefly' ]));
    Object.assign(sdata, {showlegend: false, type: get(data, 'type', 'scatter'), mode: 'markers'});

    // the rowIdx doesn't exist for generic plotly chart case
    if (isScatter2d(get(data, 'type', '')) &&
        !get(sdata, 'firefly.rowIdx') &&
        get(sdata, 'x.length', 0) !== 0) {
        const rowIdx = range(get(sdata, 'x.length')).map(String);
        set(sdata, 'firefly.rowIdx', rowIdx);
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
 *
 * @param {object} p
 * @param {string} p.chartId
 * @param {object[]} p.data
 */
export function handleTableSourceConnections({chartId, data, fireflyData}) {
    var tablesources = makeTableSources(chartId, data, fireflyData);
    var oldTablesources = get(getChartData(chartId), 'tablesources',[]);

    const hasTablesources = Array.isArray(tablesources) && tablesources.find((ts) => !isEmpty(ts));
    if (!hasTablesources) return;

    const numTraces = Math.max(tablesources.length, oldTablesources.length);
    range(numTraces).forEach( (idx) => {  // range instead of for-loop is to avoid the idx+1 JS's closure problem
        const traceTS = tablesources[idx];
        const oldTraceTS = oldTablesources[idx] || {};
        if (isEmpty(traceTS)) {
            tablesources[idx] = oldTraceTS;     // if no updates.. move the previous one into the new tablesources
        } else {
            if (!tablesourcesEqual(traceTS, oldTraceTS)) {
                const {tbl_id} = traceTS;

                if (oldTraceTS && oldTraceTS._cancel) oldTraceTS._cancel();   // cancel the previous watcher if exists
                //creates a new one.. and save the cancel handle
                updateChartData(chartId, idx, traceTS);
                traceTS._cancel = watchTableChanges(tbl_id, [TABLE_LOADED, TABLE_HIGHLIGHT, TABLE_SELECT, TABLE_REMOVE], (action) => updateChartData(chartId, idx, traceTS, action));
            } else {
                tablesources[idx] = oldTraceTS;
            }
        }
    });
    dispatchChartUpdate({chartId, changes:{tablesources}});
}

function tablesourcesEqual(newTS, oldTS) {
    //shallowequal(newTS, omit(oldTS, '_cancel'));
    return get(newTS, 'tbl_id') === get(oldTS, 'tbl_id') &&
        get(newTS, 'tblFilePath') === get(oldTS, 'tblFilePath') &&
        shallowequal(get(newTS, 'mappings'), get(oldTS, 'mappings')) &&
        shallowequal(get(newTS, 'options'), get(oldTS, 'options'));
}

function updateChartData(chartId, traceNum, tablesource, action={}) {
    const {tbl_id, tblFilePath, mappings} = tablesource;
    if (action.type === TABLE_HIGHLIGHT) {
        // ignore if traceNum is not active
        const {activeTrace=0} = getChartData(chartId);
        if (traceNum !== activeTrace) return;
        const {highlightedRow} = action.payload;
        const traceData = get(getChartData(chartId), `data.${traceNum}`);
        dispatchChartHighlighted({chartId, highlighted: getPointIdx(traceData,highlightedRow)});
    } else if (action.type === TABLE_SELECT) {
        const {activeTrace=0} = getChartData(chartId);
        if (traceNum !== activeTrace) return;
        const {selectInfo={}} = action.payload;
        updateSelected(chartId, selectInfo);
    } else if (action.type === TABLE_REMOVE) {
        const changes = {};
        Object.entries(mappings).forEach(([k,v]) => {
            changes[`data.${traceNum}.${k}`] = [];
        });
        dispatchChartUpdate({chartId, changes});
        dispatchChartHighlighted({chartId, highlighted: undefined}); 
    } else {
        if (!isFullyLoaded(tbl_id)) return;
        const tableModel = getTblById(tbl_id);

        // save original table file path
        const tblFilePathNow = get(tableModel, 'tableMeta.tblFilePath');
        if (tblFilePathNow !== tblFilePath) {
            const changes = {[`tablesources.${traceNum}.tblFilePath`]: tblFilePathNow};
            dispatchChartUpdate({chartId, changes});
        }

        // fetch data for both Firefly recognized or unrecognized plotly chart types
        if (tablesource.fetchData) {
            tablesource.fetchData(chartId, traceNum, tablesource);
        }
    }
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

export function getDataChangesForMappings({tableModel, mappings, traceNum}) {
    const cols = tableModel.tableData.columns.map( (c) => c.name);
    const transposed = zip(...tableModel.tableData.data);
    const changes = {};
    Object.entries(mappings).forEach(([k,v]) => {
        changes[`data.${traceNum}.${k}`] = transposed[cols.indexOf(v)];
    });
    return changes;
}

function makeTableSources(chartId, data=[], fireflyData=[]) {

    const convertToDS = (flattenData) =>
                        Object.entries(flattenData)
                                .filter(([k,v]) => typeof v === 'string' && v.startsWith('tables::'))
                                .reduce( (p, [k,v]) => {
                                    const [,colExp] = v.match(TBL_SRC_PATTERN) || [];
                                    if (colExp) set(p, ['mappings',k], colExp);
                                    return p;
                                }, {});


    // for some firefly specific chart types the data are
    const currentData = (data.length < fireflyData.length) ? fireflyData : data;

    return currentData.map((d, traceNum) => {
        const ds = data[traceNum] ? convertToDS(flattenObject(data[traceNum])) : {}; //avoid flattening arrays
        // table id can be a part of fireflyData too
        const tbl_id = get(data, `${traceNum}.tbl_id`) || get(fireflyData, `${traceNum}.tbl_id`);

        if (tbl_id) ds.tbl_id = tbl_id;

        // we use tblFilePath to see if the table has changed (sorted, filtered, etc.)
        if (ds.tbl_id) {
            const tableModel = getTblById(ds.tbl_id);
            ds.tblFilePath = get(tableModel, 'tableMeta.tblFilePath');
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
    if (chartDataType === 'fireflyScatter') {
        return scatterTSGetter({traceTS, chartId, traceNum});
    } else if (chartDataType === 'fireflyHistogram') {
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
    //const chartType = get(chartData, ['data', '0', 'type']);
    //const noXYAxis = chartType && (chartType === 'pie');

    const nonPieChart = isEmpty(chartData) || !has(chartData, 'data') || chartData.data.find((d) => get(d, 'type') !== 'pie');
    const noXYAxis = Boolean(!nonPieChart);

    const defaultLayout = {
        hovermode: 'closest',
        dragmode: 'zoom',
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
            titlefont: {
                size: FSIZE
            },
            ticks: noXYAxis ? '' : 'outside',
            showline: !noXYAxis,
            showticklabels: !noXYAxis,
            zeroline: false
        },
        yaxis: {
            autorange:true,
            showgrid: !noXYAxis,
            lineColor: '#e9e9e9',
            tickwidth: 1,
            ticklen: 5,
            titlefont: {
                size: FSIZE
            },
            ticks: noXYAxis ? '' : 'outside',
            showline: !noXYAxis,
            showticklabels: !noXYAxis,
            zeroline: false
        }
    };

    chartData.layout = merge(defaultLayout, chartData.layout);

    chartData.data && chartData.data.forEach((d, idx) => {
        d.name = setDefaultName(d, idx);

        const type = get(d, 'type') || get(chartData, ['fireflyData', idx, 'dataType']);

        if (idx === 0 && resetColor) {        // reset the color iterator
            getNextTraceColor(true);
            getNextTraceColorscale(true);
        }

        type && Object.entries(setDefaultColor(d, type)).forEach(([k, v]) => set(d, k, v));
    });
}


// plotly default color (items 0-7) + color-blind friendly colors
export const TRACE_COLORS = [  '#1f77b4', '#2ca02c', '#d62728', '#9467bd',
                               '#8c564b', '#e377c2', '#7f7f7f', '#17becf',
                               '#333333', '#ff3333', '#00ccff', '#336600',
                               '#9900cc', '#ff9933', '#009999', '#66ff33',
                               '#cc9999', '#b22424', '#008fb2', '#244700',
                               '#6b008f', '#b26b24', '#006b6b', '#47b224', '8F6B6B'];
export const TRACE_COLORSCALE = [  'Bluered', 'Blues', 'Earth', 'Electric', 'Greens',
                                'Greys', 'Hot', 'Jet', 'Picnic', 'Portland', 'Rainbow',
                                'RdBu', 'Reds', 'Viridis', 'YlGnBu', 'YlOrRd' ];

export function *traceColorGenerator(colorList) {
    let nextIdx = -1;

    while (true) {
        const result = yield (nextIdx === -1) ? '' : colorList[nextIdx%colorList.length];
        result ? nextIdx = -1 : nextIdx++;
    }
}

const nextTraceColor = traceColorGenerator(TRACE_COLORS);
const nextTraceColorscale = traceColorGenerator(TRACE_COLORSCALE);
const getNextTraceColor = (b) => nextTraceColor.next(b).value;
const getNextTraceColorscale = (b) => nextTraceColorscale.next(b).value;


export function getNewTraceDefaults(type='', traceNum=0) {
    let   retV;

    if (type.includes(SCATTER)) {
        retV = {
            [`data.${traceNum}.type`]: type, //make sure trace type is set
            [`data.${traceNum}.marker.color`]: TRACE_COLORS[traceNum] || undefined,
            [`data.${traceNum}.marker.line`]: 'none',
            [`data.${traceNum}.showlegend`]: true,
            ['layout.xaxis.range']: undefined, //clear out fixed range
            ['layout.yaxis.range']: undefined //clear out fixed range
        };
    } else if (type.includes(HEATMAP)) {
        retV = {
            [`data.${traceNum}.showlegend`]: true,
            ['layout.xaxis.range']: undefined, //clear out fixed range
            ['layout.yaxis.range']: undefined //clear out fixed range
        };
    } else {
        retV = {
            [`data.${traceNum}.marker.color`]: TRACE_COLORS[traceNum] || undefined,
            [`data.${traceNum}.showlegend`]: true
        };
    }

    const dataKey = `data.${traceNum}.`;
    const data = Object.entries(retV).reduce((prev, [k, v]) => {
        if (k.startsWith(dataKey)) {
            set(prev, k.substring(dataKey.length), v);
        }
        return prev;
    }, {type});


    retV = Object.assign(retV,
                        {[`${dataKey}.name`]: setDefaultName(data, traceNum)},
                        setDefaultColor(data, type, traceNum));

    return retV;
}

function setDefaultName(oneChartData, idx) {
    const name = get(oneChartData, 'name');
    return name ? name : `trace ${idx}`;
}

function setDefaultColor(data, type, idx) {
    if (!type) return {};

    const colorsOnTypes = {
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
        fireflyScatter: [['marker.color', 'line.color', 'textfont.color', 'error_x.color', 'error_y.color']],
        scatter3d: [['marker.color', 'line.color', 'textfont.color', 'error_x.color', 'error_y.color']],
        scattergl: [['marker.color', 'line.color',  'textfont.color', 'error_x.color', 'error_y.color']],
        scattergeo: [['marker.color', 'line.color', 'textfont.color']],
        others: [['marker.color']]
    };

    const getColorSetting = (attGroup) => {
        const colorSettingObj = {};

        attGroup.forEach((oneColorGroup) => {
            const [COLOR, COLORSCALE] = [0, 1];
            const colorInfo = [{endstr: 'color', idx: COLOR, existingVal: '', next: getNextTraceColor},
                               {endstr: 'colorscale', idx: COLORSCALE, existingVal: '', next: getNextTraceColorscale}];
            const getColorType = (typePath) => {
                return colorInfo.findIndex((colorType) => (typePath.endsWith(colorType.endstr)));
            };
            const noValSetAtts = oneColorGroup.filter((colorAtt) => {
                const color = get(data, colorAtt);
                if (color) {
                    colorInfo[getColorType(colorAtt)].existingVal = color;
                }

                return !color;
            });

            if (noValSetAtts.length > 0) {

                const defaultColors = colorInfo.map((oneColor) => {
                    const attIdx = noValSetAtts.findIndex((oneAtt) => (oneAtt.endsWith(oneColor.endstr)));

                    return (attIdx >= 0) ? (oneColor.existingVal || oneColor.next()): '';
                });

                noValSetAtts.forEach((oneAtt) => {
                    const colorKey = isNil(idx) ? oneAtt : `data.${idx}.${oneAtt}`;
                    colorSettingObj[colorKey] = (defaultColors[getColorType(oneAtt)]);
                });
            }
        });
        return colorSettingObj;
    };
    const colorList = Object.keys(colorsOnTypes).includes(type) ? colorsOnTypes[type] : colorsOnTypes.others;
    return getColorSetting(colorList);
}



