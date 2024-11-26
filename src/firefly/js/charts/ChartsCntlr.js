/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {cloneDeep, has, get, isArray, isEmpty, isString, isUndefined, omit, omitBy, set, range, unset} from 'lodash';
import shallowequal from 'shallowequal';

import {flux} from '../core/ReduxFlux.js';
import {updateSet, updateObject, toBoolean} from '../util/WebUtil.js';
import {Logger} from '../util/Logger.js';
import {getTblById, getColumns, isFullyLoaded, COL_TYPE} from '../tables/TableUtil.js';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {DEFAULT_PLOT2D_VIEWER_ID, dispatchAddViewerItems, dispatchUpdateCustom, dispatchRemoveViewerItems,
    getMultiViewRoot, getViewer} from '../visualize/MultiViewCntlr.js';
import {applyDefaults, flattenAnnotations, formatColExpr, getPointIdx, getRowIdx, handleTableSourceConnections,
        clearChartConn, newTraceFrom, setupTableWatcher, HIGHLIGHTED_PROPS, SELECTED_PROPS, TBL_SRC_PATTERN,
        getSelIndexes, isSpectralOrder, combineAllTraceFrom} from './ChartUtil.js';
import {FilterInfo} from '../tables/FilterInfo.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {REINIT_APP, getAppOptions} from '../core/AppDataCntlr.js';
import {showInfoPopup} from '../ui/PopupUtil.jsx';
import {makeHistogramParams, makeXYPlotParams, getDefaultChartProps} from './ChartUtil.js';
import {adjustColorbars, hasFireflyColorbar} from './dataTypes/FireflyHeatmap.js';

export const CHART_SPACE_PATH = 'charts';
export const UI_PREFIX = `${CHART_SPACE_PATH}.ui`;
export const DATA_PREFIX = `${CHART_SPACE_PATH}.data`;

export const usePlotlyReact = toBoolean(sessionStorage.getItem('plotlyReact'));   // defaults to false
export const useScatterGL = toBoolean(sessionStorage.getItem('scatterGL'));       // defaults to false
export const useChartRedraw = toBoolean(sessionStorage.getItem('chartRedraw'));   // defaults to false

/*---------------------------- ACTIONS -----------------------------*/
export const CHART_ADD = `${DATA_PREFIX}/chartAdd`;
export const CHART_UPDATE = `${DATA_PREFIX}/chartUpdate`;
export const CHART_REMOVE = `${DATA_PREFIX}/chartRemove`;
export const CHART_TRACE_REMOVE = `${DATA_PREFIX}/chartTraceRemove`;
export const CHART_HIGHLIGHT = `${DATA_PREFIX}/chartHighlight`;
export const CHART_SELECT = `${DATA_PREFIX}/chartSelectSelection`;
export const CHART_FILTER_SELECTION = `${DATA_PREFIX}/chartFilterSelection`;
export const CHART_SET_ACTIVE_TRACE = `${DATA_PREFIX}/chartSetActiveTrace`;

export const CHART_UI_EXPANDED      = `${UI_PREFIX}.expanded`;
export const CHART_MOUNTED = `${UI_PREFIX}/mounted`;
export const CHART_UNMOUNTED = `${UI_PREFIX}/unmounted`;


const FIREFLY_TRACE_TYPES = ['scatter', 'scattergl', 'fireflyHistogram', 'fireflyHeatmap'];

const EMPTY_ARRAY = [];

export default {actionCreators, reducers};

let cleanupWatcherStarted = false;
const logger = Logger('ChartsCntlr');

function actionCreators() {
    return {
        [CHART_ADD]:     chartAdd,
        [CHART_REMOVE]:  chartRemove,
        [CHART_TRACE_REMOVE]:  chartTraceRemove,
        [CHART_UPDATE]:  chartUpdate,
        [CHART_HIGHLIGHT]: chartHighlight,
        [CHART_FILTER_SELECTION]: chartFilterSelection,
        [CHART_SELECT]: chartSelect,
        [CHART_SET_ACTIVE_TRACE]: setActiveTrace
    };
}

function reducers() {
    return {
        [CHART_SPACE_PATH]: reducer
    };
}



/*
 * Add chart to the UI
 *
 * @param {Object} p - dispatch parameters
 *  @param {string} p.chartId - chart id
 *  @param {string} p.chartType - (for backward compatibility) chart type, ex. 'scatter', 'histogram'
 *  @param {string} [p.groupId] - chart group for grouping charts together
 *  @param {string} [p.viewerId] – viewer where chart will be displayed
 *  @param {boolean} [p.deletable] - is the chart deletable, if undefined: single chart in a group is not deletable, multiple are deletable
 *  @param {string} [p.help_id] - help id, if undefined, no help icon shows up
 *  @param {Function} [p.dispatcher=flux.process] - only for special dispatching uses such as remote
 *  @public
 *  @function dispatchChartAdd
 *  @memberof firefly.action
 */
export function dispatchChartAdd({chartId, chartType='plot.ly', groupId='main', viewerId=DEFAULT_PLOT2D_VIEWER_ID, deletable, help_id, mounted=undefined, dispatcher= flux.process, ...rest}) {
    dispatcher({type: CHART_ADD, payload: {chartId, chartType, groupId, viewerId, deletable, help_id, mounted, ...rest}});
}

/*
 * Delete chart and related data
 *  @param {string} chartId - chart id
 *  @param {Function} [dispatcher=flux.process] - only for special dispatching uses such as remote
 *  @public
 *  @function dispatchChartRemove
 *  @memberof firefly.action
 */
export function dispatchChartRemove(chartId, dispatcher= flux.process) {
    dispatcher({type: CHART_REMOVE, payload: {chartId}});
}

/*
 * Delete chart trace and the related data
 *  @param {string} chartId - chart id
 *  @param {number} traceNum - trace index to remove
 *  @param {Function} [dispatcher=flux.process] - only for special dispatching uses such as remote
 *  @public
 *  @function dispatchChartTraceRemove
 *  @memberof firefly.action
 */
export function dispatchChartTraceRemove(chartId, traceNum, dispatcher= flux.process) {
    dispatcher({type: CHART_TRACE_REMOVE, payload: {chartId, traceNum}});
}

/**
 * Update chart data. The parameter should have a partial object it wants to update.
 * The keys of the partial object should be in path-string format, ie. 'a.b.c'.
 * @summary Update chart data.
 *  @param {Object} p - dispatch parameetrs
 *  @param {string} p.chartId - chart id
 *  @param {Object} p.changes - object with the path-string keys and values of the changed props
 *  @param {Function} [p.dispatcher=flux.process] - only for special dispatching uses such as remote
 *  @public
 *  @function dispatchChartUpdate
 *  @memberof firefly.action
 */
export function dispatchChartUpdate({chartId, changes, dispatcher=flux.process}) {
    dispatcher({type: CHART_UPDATE, payload: {chartId, changes}});
}

/**
 * Highlight the given highlighted data point. Highlighted is an index into the current data array.
 * @param {object} p parameter object
 * @param {string} p.chartId      required.  
 * @param {number} p.highlighted  index of the current data array
 * @param {number} [p.traceNum] - highlighted trace number
 * @param {number} [p.traceName] - highlighted trace name
 * @param {boolean} [p.chartTrigger] - action is triggered by chart
 * @param {function} [p.dispatcher]
 */
export function dispatchChartHighlighted({chartId, highlighted, traceNum, traceName, chartTrigger, dispatcher=flux.process}) {
    dispatcher({type: CHART_HIGHLIGHT, payload: {chartId, highlighted, traceNum, traceName, chartTrigger}});
}

/**
 * Perform filter on the current selection.  This function applies only to data bound to table.
 * @param {object} p parameter object
 * @param {string} p.chartId      required.
 * @param {number} [p.highlighted]  highlight the data point if given.
 * @param {function} [p.dispatcher]
 */
export function dispatchChartFilterSelection({chartId, highlighted, dispatcher=flux.process}) {
    dispatcher({type: CHART_FILTER_SELECTION, payload: {chartId, highlighted}});
}

/**
 * Perform select(checked) on the current selection.  This function applies only to data bound to table.
 * @param {object} p parameter object
 * @param {string} p.chartId      required.
 * @param {number[]} p.selIndexes required.  An array of indexes to select.
 * @param {boolean} p.chartTrigger - action is triggered by chart
 * @param {function} [p.dispatcher]
 */
export function dispatchChartSelect({chartId, selIndexes, chartTrigger, dispatcher=flux.process}) {
    dispatcher({type: CHART_SELECT, payload: {chartId, selIndexes, chartTrigger}});
}

/**
 * Perform select(checked) on the current selection.  This function applies only to data bound to table.
 * @param {object} p parameter object
 * @param {string} p.chartId      required.
 * @param {number} p.activeTrace required.  An array of indexes to select.
 * @param {function} [p.dispatcher]
 */
export function dispatchSetActiveTrace({chartId, activeTrace, dispatcher=flux.process}) {
    dispatcher({type: CHART_SET_ACTIVE_TRACE, payload: {chartId, activeTrace}});
}


/**
 * Put a chart into an expanded mode.
 * @param {object} p
 * @param {string} p.chartId - chart id
 * @param {string} p.expandedViewerId- chart id
 * @param {Function} [p.dispatcher=flux.process] - only for special dispatching uses such as remote
 */
export function dispatchChartExpanded({dispatcher=flux.process, ...rest}) {
    dispatcher( {type: CHART_UI_EXPANDED, payload: rest});
}


/*
 *  Dispatched when chart becomes visible (is rendered for the first time after being invisible)
 *  When chart is mounted, its data need to be in sync with the related table
 *  @param {string} chartId - chart id
 *  @param {Function} [dispatcher=flux.process] - only for special dispatching uses such as remote
 */
export function dispatchChartMounted(chartId, dispatcher= flux.process) {
    dispatcher({type: CHART_MOUNTED, payload: {chartId}});
}

/*
 *  Dispatched when chart becomes invisible
 *  When chart is unmounted, its data synced with the related table only when it becomes mounted again
 *  @param {string} chartId - chart id
 *  @param {Function} [dispatcher=flux.process] - only for special dispatching uses such as remote
 */
export function dispatchChartUnmounted(chartId, dispatcher= flux.process) {
    dispatcher({type: CHART_UNMOUNTED, payload: {chartId}});
}

/*------------------------------- action creators -------------------------------------------*/


function chartAdd(action) {
    return (dispatch) => {
        const {chartId, chartType, deletable, renderTreeId} = action.payload;
        clearChartConn({chartId});

        if (chartType === 'plot.ly') {
            // the action payload might need to be updated for firefly trace types
            const newPayload = handleFireflyTraceTypes(action.payload);
            // update payload to handle plotly api changes
            handleApiChanges(newPayload);

            // use application default if deletable is not defined for this chart
            if (isUndefined(deletable)) {
                newPayload.deletable = get(getAppOptions(), 'charts.defaultDeletable');
            }
            const actionToDispatch = (newPayload === action.payload) ? action : Object.assign({}, action, {payload: newPayload});
            dispatch(actionToDispatch);
            const {viewerId, data, fireflyData} = actionToDispatch.payload;
            if (viewerId) {
                // viewer will be added if it does not exist already
                dispatchAddViewerItems(viewerId, [chartId], 'plot2d', renderTreeId);
            }

            // lazy table connection
            // handle reset case - when a chart is already mounted
            const {mounted} = getChartData(chartId);
            if (mounted > 0) {
                handleTableSourceConnections({chartId, data, fireflyData});
            }
            if (!cleanupWatcherStarted) {
                dispatchAddActionWatcher({id: 'chartCleanup', actions:[TablesCntlr.TABLE_REMOVE], callback: cleanupRelatedChartData});
                cleanupWatcherStarted = true;
            }
        } else {
            // supporting deprecated API, which uses the specialized parameters
            const {chartId, chartType, params={}, ...rest} = action.payload;
            const {tbl_id} = params;

            const doChartAdd = (p) => {
                const chartData = chartType === 'scatter' ? makeXYPlotParams(p)
                                : chartType === 'histogram' ? makeHistogramParams(p)
                                : getDefaultChartProps(tbl_id);
                if (chartData) {
                    dispatchChartAdd({chartId, ...chartData, ...rest});
                }
            };

            // @callback {actionWatcherCallback}
            const onTblLoad = (action, cancelSelf, params) => {
                if (get(action.payload, 'tbl_id') === tbl_id) {
                    doChartAdd(params);
                    cancelSelf && cancelSelf();
                }
            };

            if (isFullyLoaded(tbl_id)) {
                doChartAdd(params);
            } else {
                // add watcher that will add chart on table load
                const actions = [TablesCntlr.TABLE_LOADED];
                dispatchAddActionWatcher({id:`onTblLoad-${chartId}`, actions, callback: onTblLoad, params});
            }
        }
    };
}

function chartRemove(action) {
    return (dispatch) => {
        const {chartId} = action.payload;
        clearChartConn({chartId});
        const viewerId = get(getChartData(chartId), 'viewerId');
        if (viewerId) {
            dispatchRemoveViewerItems(viewerId, [chartId]);
            if (getViewer(getMultiViewRoot(), viewerId)?.customData?.activeItemId === chartId) {
                dispatchUpdateCustom(viewerId, {activeItemId: undefined});
            }
        }
        dispatch({type: action.type, payload: Object.assign({},action.payload, {viewerId})});
    };
}

function chartTraceRemove(action) {
    return (dispatch) => {
        const {chartId, traceNum} = action.payload;
        const {tablesources=[]} = getChartData(chartId);
        // cancel and replace table watchers, affected by the trace removal
        tablesources.forEach((ts,i) => {
            if (i>=traceNum && ts && ts._cancel) {
                ts._cancel();
                if (i !== traceNum) {
                    const newIdx = i-1;
                    ts._cancel = setupTableWatcher(chartId, ts, newIdx);
                }
            }
        });
        dispatch(action);
    };
}

function chartUpdate(action) {
    return (dispatch) => {
        const {chartId, changes} = action.payload;
        // when selection  is undefined, selections layer must be removed
        if (changes.hasOwnProperty('selection') && !changes.selection) changes['layout.selections'] = [];
        // remove any table's mappings from changes because it will be applied by the connectors.
        const changesWithoutTblMappings = omitBy(changes, (v) => isString(v) && v.match(TBL_SRC_PATTERN));
        set(action, 'payload.changes', changesWithoutTblMappings);
        dispatch(action);


        const {data, fireflyData} = Object.entries(changes)
                             .filter(([k,]) => (k.startsWith('data') || k.startsWith('fireflyData')))
                             .reduce( (p, [k,v]) => set(p, k, v), {}); // take all of the data changes and create an object from it.

        // lazy table connection
        const {mounted} = getChartData(chartId);
        if (mounted > 0) {
            handleTableSourceConnections({chartId, data, fireflyData});
        }
    };
}

function chartHighlight(action) {
    return (dispatch) => {
        const {chartId, highlighted=0, chartTrigger=false} = action.payload;
        // TODO: activeTrace is not implemented.  switch to trace.. then highlight(?)
        const {data, fireflyData, tablesources, activeTrace:activeDataTrace=0, selected} = getChartData(chartId);

        const {traceNum=activeDataTrace, traceName} = action.payload; // highlighted trace can be selected or highlighted trace of the data trace

        // when skipping hover, clicking on chart point produces no event
        // disable chart highlight in this case
        if (get(data, `${traceNum}.hoverinfo`) === 'skip') { return; }

        const ttype = get(data, [traceNum, 'type'], 'scatter');

        if (!isEmpty(tablesources) && ttype.includes('scatter')) {
            // activeTrace is different from activeDataTrace if a selected point highlighted, for example
            const {tbl_id} = tablesources[activeDataTrace] || {};
            if (!tbl_id) return;
            // avoid updating chart twice
            // update only as a response to table highlight change
            if (!chartTrigger) {
                const traceAnnotations = get(fireflyData, `${traceNum}.annotations`);
                const hlTrace = newTraceFrom(data[traceNum], [highlighted], HIGHLIGHTED_PROPS, traceAnnotations);
                dispatchChartUpdate({chartId, changes: {highlighted: hlTrace}});
            }
            let traceData = data[traceNum];

            if (traceNum !== activeDataTrace) {
                if (traceName === SELECTED_PROPS.name) {
                    // highlighting selected point
                    traceData = selected;
                } else if (traceName === HIGHLIGHTED_PROPS.name) {
                    // no need to highlight highlighted
                    return;
                }
            }
            if (traceData) {
                const highlightedRowIdx = getRowIdx(traceData, highlighted);
                TablesCntlr.dispatchTableHighlight(tbl_id, highlightedRowIdx);
            }
        }
    };
}

function chartSelect(action) {
    return (dispatch) => {
        const {chartId, selIndexes=[], chartTrigger=false} = action.payload;
        const {activeTrace=0, data, fireflyData, tablesources} = getChartData(chartId);

        // when skipping hover, selecting chart points does not work
        // disable chart select in this case
        if (get(data, `${activeTrace}.hoverinfo`) === 'skip') { return; }

        // avoid updating chart twice
        // don't update before table select
        if (chartTrigger) {
            if (!isEmpty(tablesources)) {
                const {tbl_id} = tablesources[activeTrace] || {};
                const {totalRows} = getTblById(tbl_id);
                const selectInfoCls = SelectInfo.newInstance({rowCount: totalRows});

                selIndexes.forEach(([ptIdx, traceIdx]) => selectInfoCls.setRowSelect(getRowIdx(data[traceIdx], ptIdx), true));
                TablesCntlr.dispatchTableSelect(tbl_id, selectInfoCls.data);
            }
        } else {
            let selected = undefined;
            const hasSelected = !isEmpty(selIndexes);
            if (isSpectralOrder(chartId)) {
                selected = combineAllTraceFrom(chartId, selIndexes, SELECTED_PROPS);
                dispatchChartUpdate({chartId, changes: {hasSelected, selected, selection: undefined}});
            } else {
                const traceAnnotations = get(fireflyData, `${activeTrace}.annotations`);
                selected = newTraceFrom(data[activeTrace], selIndexes.map((s) => s[0]), SELECTED_PROPS, traceAnnotations);
                dispatchChartUpdate({chartId, changes: {hasSelected, selected, selection: undefined}});
            }
        }
    };
}

function chartFilterSelection(action) {
    return (dispatch) => {
        const {chartId} = action.payload;
        const {activeTrace=0, selection, tablesources} = getChartData(chartId);
        if (!isEmpty(tablesources)) {
            const {tbl_id, mappings} = tablesources[activeTrace];
            const numericCols = getColumns(getTblById(tbl_id), COL_TYPE.NUMBER).map((c) => c.name);

            let {x,y} = mappings;
            let upperLimit = get(mappings,  `fireflyData.${activeTrace}.yMax`);
            let lowerLimit = get(mappings,  `fireflyData.${activeTrace}.yMin`);
            // use standard form without spaces for filter key
            // to make sure the key is replaced when setFilter is used
            [x,y,upperLimit,lowerLimit] = [x, y, upperLimit,lowerLimit].map((v) => v && formatColExpr({colOrExpr:v, colNames:numericCols}));
            if (upperLimit) {
                y = `ifnull(${y},${upperLimit})`;
            } else if (lowerLimit) {
                y = `ifnull(${y},${lowerLimit})`;
            }

            const multiArea = get(selection, 'multiArea')
            if (multiArea) {
                showInfoPopup('Filtering is only supported for one selection area.', 'Warning');
                return;
            }
            const [xMin, xMax] = get(selection, 'range.x', []);
            const [yMin, yMax] = get(selection, 'range.y', []);
            const {request} = getTblById(tbl_id);
            const filterInfoCls = FilterInfo.parse(request.filters);

            filterInfoCls.setFilter(x, '> ' + xMin);
            filterInfoCls.addFilter(x, '< ' + xMax);
            filterInfoCls.setFilter(y, '> ' + yMin);
            filterInfoCls.addFilter(y, '< ' + yMax);


            // filters are processed by db, column expressions need to use syntax db understands
            // filters can be set for any column type, numeric or non-numeric
            const allColumns = getColumns(getTblById(tbl_id)).map((c) => c.name);
            const formatKey = (k) => formatColExpr({colOrExpr:k, quoted:true, colNames:allColumns});

            const newRequest = Object.assign({}, request, {filters: filterInfoCls.serialize(formatKey)});
            TablesCntlr.dispatchTableFilter(newRequest);
            dispatchChartUpdate({chartId, changes:{selection: undefined}});
        }
    };
}

function setActiveTrace(action) {
    return (dispatch) => {
        const {chartId, activeTrace} = action.payload;
        const {data, fireflyData, tablesources, curveNumberMap} = getChartData(chartId);
        const changes = getActiveTraceChanges({chartId, activeTrace, data, fireflyData, tablesources, curveNumberMap});
        dispatchChartUpdate({chartId, changes});
    };
}

function getActiveTraceChanges({chartId, activeTrace, data, fireflyData, tablesources, curveNumberMap}) {
    const tbl_id = get(tablesources, [activeTrace, 'tbl_id']);
    let selected = undefined;
    let highlighted = undefined;
    let curveMap = undefined;
    if (tbl_id) {
        const {selectInfo, highlightedRow} = getTblById(tbl_id) || {};
        const traceAnnotations = get(fireflyData, `${activeTrace}.annotations`);
        if (selectInfo) {
            const selectInfoCls = SelectInfo.newInstance(selectInfo);
            const selIndexes = getSelIndexes(data, selectInfoCls, activeTrace);
            if (selIndexes.length > 0) {
                if (isSpectralOrder(chartId)) {
                    selected = combineAllTraceFrom(chartId, selIndexes, SELECTED_PROPS);
                } else {
                    const traceAnnotations = get(fireflyData, `${activeTrace}.annotations`);
                    selected = newTraceFrom(data[activeTrace], selIndexes.map((s) => s[0]), SELECTED_PROPS, traceAnnotations);
                }
            }
        }
        highlighted = newTraceFrom(data[activeTrace], [getPointIdx(data[activeTrace], highlightedRow)], HIGHLIGHTED_PROPS, traceAnnotations);
        if (curveNumberMap) {
            curveMap = range(curveNumberMap.length).filter((idx) => (idx !== activeTrace));
            curveMap.push(activeTrace);
        }
    }
    return {activeTrace, selected, highlighted, selection: undefined, curveNumberMap: curveMap};
}

function isFireflyType(type) {
    return  !type || FIREFLY_TRACE_TYPES.includes(type);
}

/**
 * Move firefly attributes from data and layout objects to fireflyData and fireflyLayout
 * @param payload - original action payload
 * @return updated action payload
 */
function handleFireflyTraceTypes(payload) {
    if (payload['fireflyData']) return payload;
    const {data=[], layout={}} = payload;
    let newPayload = payload;
    if (data.find((d) => isFireflyType(d.type))) {
        const fireflyData = [];
        const plotlyData = [];
        data.forEach((d) => {
            if (isFireflyType(d.type)) {
                const fd = get(d, 'firefly', {});
                if (!fd.dataType) fd.dataType = d.type;     // use data.type if not defined.
                fireflyData.push(fd);
                plotlyData.push(omit(d, ['firefly']));
            } else {
                fireflyData.push(undefined);
                plotlyData.push(d);
            }
        });
        newPayload = Object.assign({}, newPayload, {data: plotlyData, fireflyData});
    }
    if (layout.firefly || isArray(layout.annotations)) {
        const fireflyLayout = layout.firefly;
        const plotlyLayout = omit(layout, 'firefly');

        if (isArray(layout.annotations)) {
            // save annotations array into fireflyLayout
            if (!fireflyLayout.annotations) { fireflyLayout.annotations = []; }
            fireflyLayout.annotations.push(plotlyLayout.annotations);
        }


        newPayload = Object.assign({}, newPayload, {layout: plotlyLayout, fireflyLayout});
    }
    return newPayload;
}

/**
 * Convert deprecated title attributes: string title and titlefont
 * into title: {text, font}
 * Updates payload in place, if an update is necessary
 * @param payload
 */
function handleApiChanges(payload) {
    const {layout={}} = payload;
    const titleAttr = [
        ['title', 'titlefont'],
        ['xaxis.title', 'xaxis.titlefont'],
        ['yaxis.title', 'yaxis.titlefont']];
    titleAttr.forEach(([t, f]) => {
        const title = get(layout, t);
        if (isString(get(layout, t))) {
            unset(layout, t);
            set(layout, `${t}.text`, title);
        }
        const font = get(layout, f, {});
        if (!isEmpty(font)) {
            unset(layout, f);
            set(layout, `${t}.font`, font);
        }
    });
}


/*-----------------------------------------------------------------------------------------*/


/*
 Possible structure of store:
  /ui
    expanded: Object - the information about expanded chart
    {
         chartId: string
    }
  /data - chart data, object with the keys that are chart id
    chart data
*/

export function reducer(state={ui:{}, data:{}}, action={}) {

    if (!action.type.startsWith(TablesCntlr.DATA_PREFIX) && !action.type.startsWith(CHART_SPACE_PATH)){
        if (action.type === REINIT_APP) {
            return {ui:{}, data:{}};
        } else {
            return state;
        }
    }

    const nstate = {...state};

    //nstate.xyplot = reduceXYPlot(state['xyplot'], action);
    //nstate.histogram = reduceHistogram(state['histogram'], action);

    nstate.data = reduceData(state['data'], action);

    // generic for all chart types
    nstate.ui = reduceUI(state['ui'], action);

    if (shallowequal(state, nstate)) {
        return state;
    } else {
        return nstate;
    }
}

function changeToScatterGL(chartData) {
    get(chartData, 'data', []).forEach((d) => d.type === 'scatter' && (d.type = 'scattergl'));  // use scattergl instead of scatter
    ['selected', 'highlighted'].map((k) => get(chartData, k, {})).forEach((d) => d.type === 'scatter' && (d.type = 'scattergl'));
}

/**
 * @param state - ui part of chart state
 * @param action - action
 * @returns {*} - updated ui part of the state
 */
function reduceData(state={}, action={}) {
    //if (chartActions.indexOf(action.type) < 0) { return state; } // useful when debugging
    switch (action.type) {
        case (CHART_ADD) :
        {
            const {chartId, chartType, mounted, ...rest}  = action.payload;
            // if a chart is replaced (added with the same id) mounted should not change
            const nMounted = isUndefined(mounted) ? get(state, [chartId, 'mounted']) : mounted;
            // save the original payload, so that the chart could be recreated
            rest['_original'] = cloneDeep(action.payload);
            applyDefaults(rest);
            useScatterGL && changeToScatterGL(rest);

            // the first trace is put as the last curve for plotly rendering
            const tData = get(rest, ['data', 'length']);
            if (tData) {
                Object.assign(rest, {activeTrace: tData-1, curveNumberMap: range(tData)});
            }
            state = updateSet(state, chartId,
                omitBy({
                    chartId,
                    chartType,
                    mounted: nMounted,
                    ...rest
                }, isUndefined));
            logger.info(`CHART_ADD ${chartId} #mounted ${nMounted}`);
            return state;
        }
        case (CHART_UPDATE) :
        {
            const {chartId, changes}  = action.payload;
            let chartData = getChartData(chartId);
            chartData = updateObject(chartData, changes);
            useScatterGL && changeToScatterGL(chartData);

            return updateSet(state, chartId, chartData);
        }
        case (CHART_TRACE_REMOVE)  :
        {
            const {chartId, traceNum} = action.payload;
            const {changes, moreChanges} = removeTrace({chartId, traceNum});
            let chartData = getChartData(chartId);
            if (!isEmpty(changes)) {
                // changes to array fields: data, fireflyData, etc
                chartData = updateObject(chartData, changes);
                if (!isEmpty(moreChanges)) {
                    // changes to the specific trace attributes
                    chartData = updateObject(chartData, moreChanges);
                }
            }
            return updateSet(state, chartId, chartData);
        }
        case (CHART_REMOVE)  :
        {
            const {chartId} = action.payload;
            logger.info('CHART_REMOVE '+chartId);
            return omit(state, chartId);
        }
        case (CHART_MOUNTED) :
        {
            const {chartId} = action.payload;
            const n = get(state, [chartId,'mounted'], 0);
            state = updateSet(state, [chartId,'mounted'], Number(n) + 1);
            logger.info(`CHART_MOUNTED ${chartId} #mounted ${state[chartId].mounted}`);

            return state;
        }
        case (CHART_UNMOUNTED) :
        {
            const {chartId} = action.payload;
            if (has(state, chartId)) {
                const n = get(state, [chartId,'mounted'], 0);
                if (n > 0) {
                    state = updateSet(state, [chartId, 'mounted'], Number(n) - 1);
                } else {
                    logger.error(`CHART_UNMOUNT on unmounted chartId ${chartId}`);
                }
                logger.info(`CHART_UNMOUNTED ${chartId} #mounted ${state[chartId].mounted}`);
            }
            return state;
        }
        default:
            return state;
    }
}

export function getAnnotations(chartId) {
    const chartData = getChartData(chartId);
    let annotations = get(chartData, 'fireflyLayout.annotations', EMPTY_ARRAY);

    get(chartData, 'fireflyData', []).forEach((d) => {
        const traceAnnotations = flattenAnnotations(d.annotations);
        if (traceAnnotations.length > 0) {
            annotations = annotations.concat(traceAnnotations);
        }
    });
    return annotations;
}

/**
 * Return trace symbol
 * In some cases we use distinct symbols to mark the specific points of the trace (ex. upper limits)
 * In these cases data.[traceNum].marker.symbol will be an array and fireflyData.[traceNum].marker.symbol
 * will contain the trace symbol
 * @param data
 * @param fireflyData
 * @param traceNum
 * @returns {*}
 */
export function getTraceSymbol(data, fireflyData, traceNum) {
    let symbol = get(data, `${traceNum}.marker.symbol`, 'circle');
    if (isArray(symbol)) { symbol = get(fireflyData, `${traceNum}.marker.symbol`, 'circle'); }
    return symbol;
}



/**
 * @param state - ui part of chart state
 * @param action - action
 * @returns {*} - updated ui part of the state
 */
function reduceUI(state={}, action={}) {
    switch (action.type) {
        case (CHART_UI_EXPANDED) :
            return updateSet(state, 'expanded', action.payload);
        case (CHART_REMOVE) :
            if (get(action.payload, 'chartId') === get(getExpandedChartProps(), 'chartId')) {
                return omit(state, 'expanded');
            }
            return state;
        default:
            return state;
    }
}

/**
 * @callback actionWatcherCallback
 * @param action
 */
function cleanupRelatedChartData(action) {
    const tbl_id = get(action.payload, 'tbl_id');
    if (!tbl_id) return;
    const charts = get(flux.getState(), [CHART_SPACE_PATH, 'data']);
    if (!charts || isEmpty(charts)) { return; }
    const getMatchingTSIdx = (chartId) => {
        return get(getChartData(chartId), 'tablesources', []).findIndex((e) => get(e, 'tbl_id') === tbl_id);
    };
    Object.keys(charts).forEach((chartId) => {
        let traceNum = getMatchingTSIdx(chartId);
        while ( traceNum >= 0) {
            const {data, tablesources} = getChartData(chartId);
            // remove trace or remove chart if the last trace
            tablesources[traceNum]._cancel && tablesources[traceNum]._cancel();
            if (data.length === 1) {
                dispatchChartRemove(chartId);
            } else {
                dispatchChartTraceRemove(chartId, traceNum);
            }
            traceNum = getMatchingTSIdx(chartId);
        }
    });
}

export function hasUpperLimits(chartId, traceNum) {
    const yMax = get(getChartData(chartId), `fireflyData.${traceNum}.yMax`);
    return !isUndefined(yMax);
}

export function hasLowerLimits(chartId, traceNum) {
    const yMin = get(getChartData(chartId), `fireflyData.${traceNum}.yMin`);
    return !isUndefined(yMin);
}

export function dataLoadedUpdate(changes) {
    // when the chart data finished loading, fireflyData.traceNum.isLoading is switched to false
    return Object.keys(changes).find((k)=>(k.match(/isLoading$/) && !changes[k]));
}

/**
 * Reset chart to the original
 * @param chartId
 */
export function resetChart(chartId) {
    const {_original} = getChartData(chartId);
    _original && dispatchChartAdd(_original);
}

function removeTrace({chartId, traceNum}) {
    const {activeTrace, data, fireflyData, layout, tablesources, curveNumberMap} = getChartData(chartId);
    const changes = {};
    const moreChanges = {};


    [[data, 'data'], [fireflyData, 'fireflyData'], [tablesources, 'tablesources']].forEach(([arr,name]) => {
        if (arr && traceNum < arr.length) {
            changes[name] = arr.filter((e,i) => i !== traceNum);
        }
    });
    
    // handle colorbars
    if (hasFireflyColorbar(chartId, traceNum)) {
        Object.assign(moreChanges, adjustColorbars({data: changes['data'], fireflyData: changes['fireflyData'], layout}));
    }

    if (curveNumberMap && traceNum < curveNumberMap.length) {
        // new curve map has the same order of traces as the old curve map
        const newCurveMap = curveNumberMap.filter((e) => (e !== traceNum)).map((e) => (e > traceNum ? e-1 : e));
        changes['curveNumberMap'] = newCurveMap;
        if (newCurveMap.length > 0) {
            const newActiveTrace = newCurveMap[newCurveMap.length-1];
            if (newActiveTrace !== activeTrace) {
                changes['activeTrace'] = newActiveTrace;
            }
            if (traceNum === activeTrace) {
                Object.assign(changes,
                    getActiveTraceChanges({activeTrace: newActiveTrace,
                        data: changes['data'],
                        fireflyData: changes['fireflyData'],
                        tablesources: changes['tablesources'],
                        curveNumberMap: newCurveMap})
                );
            }
        }
    }

    return {changes, moreChanges};
}

export function getChartData(chartId, defaultChartData={}) {
    return get(flux.getState(), [CHART_SPACE_PATH, 'data', chartId], defaultChartData);
}


/**
 * Get error object associated with the given chart data element
 * @param chartId
 * @returns {Array<{message:string, reason:object}>} an array of error objects
 */
export function getErrors(chartId) {
    const errors = [];
    const chartData = get(flux.getState(), [CHART_SPACE_PATH, 'data', chartId], {});
    get(chartData, 'fireflyData', []).forEach((d) => {
        const error = get(d, 'error');
        error && errors.push(error);
    });

    return errors;
}

export function dispatchError(chartId, traceNum, reason) {

    const {data=[]} = getChartData(chartId);
    let forTrace = '';
    if (data.length === 1) {
        const name = get(data, `${traceNum}.name`);
        // if a trace is user named, mention the name
        forTrace = name && !name.toLowerCase().startsWith('trace') ? `: ${name} data` : '';
    } else if (data.length > 1) {
        // mention trace name when there are multiple traces
        const name = get(data, `${traceNum}.name`, `trace ${traceNum}`);
        forTrace = ` for ${name}`;
    }
    let message = `Cannot display requested data${forTrace}`;

    let reasonStr = `${reason}`.toLowerCase();
    if (reasonStr.match(/not supported/)) {
        reasonStr = 'Unsupported feature requested. Please choose valid options.';
    } else if (reasonStr.match(/data exception/) || reasonStr.match(/column not found/)) {
        // error from db
        reasonStr = reasonStr.replace('error: ', '');
    } else if (reasonStr.match(/invalid column/)) {
        reasonStr = 'Non-existent column or invalid expression. Please choose valid X and Y.';
    } else if (reasonStr.match(/rows exceed/)) {
        message = 'Please filter the table or use different chart type.';
        reasonStr = reason;
    // } else if (reasonStr.match(/same column/)) {
    //     message = 'The columns requested are identical or one of them is not numerical.';
    //     reasonStr = reason;
    } else if (reasonStr.match(/failed to retrieve/) || reasonStr.match(/no data/) || reasonStr.match(/null/)){
        message = `No data available${forTrace}`;
        reasonStr = '';
    } else {
        logger.error(`${message}: ${reason}`);
        reasonStr = reason;
    }
    const changes = {};
    changes[`fireflyData.${traceNum}.error`] = {message, reason: reasonStr};
    changes[`fireflyData.${traceNum}.isLoading`] = false;
    dispatchChartUpdate({chartId, changes});
}

export function getExpandedChartProps() {
    return  flux.getState()[CHART_SPACE_PATH]?.ui?.expanded ?? {};
}


export function getChartIdsForTable(tbl_id) {
    const chartIds = [];
    const state = get(flux.getState(), [CHART_SPACE_PATH, 'data']);
    Object.keys(state).forEach((cid) => {
        if (state[cid].tbl_id === tbl_id) {
            chartIds.push(cid);
        }
    });
    return chartIds;
}

export function getChartIdsInGroup(groupId) {
    const chartIds = [];
    const state = get(flux.getState(), [CHART_SPACE_PATH, 'data']);
    Object.keys(state).forEach((cid) => {
        if (state[cid].groupId === groupId) {
            chartIds.push(cid);
        }
    });
    return chartIds;
}

export function removeChartsInGroup(groupId) {
    const chartData = get(flux.getState(), [CHART_SPACE_PATH, 'data']);
    Object.values(chartData)
            .filter( (v) => !groupId || v.groupId === groupId)
            .forEach( (v) => dispatchChartRemove(v.chartId));
}

