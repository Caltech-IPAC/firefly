/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {cloneDeep, has, get, isEmpty, isString, isUndefined, omit, omitBy, set, range} from 'lodash';
import shallowequal from 'shallowequal';

import {flux} from '../Firefly.js';
import {updateSet, updateMerge, updateObject, toBoolean} from '../util/WebUtil.js';
import {getTblById} from '../tables/TableUtil.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {logError} from '../util/WebUtil.js';
import {dispatchAddViewerItems} from '../visualize/MultiViewCntlr.js';
import {getPointIdx, getRowIdx, handleTableSourceConnections, clearChartConn, newTraceFrom,
        applyDefaults, HIGHLIGHTED_PROPS, SELECTED_PROPS, TBL_SRC_PATTERN} from './ChartUtil.js';
import {FilterInfo} from '../tables/FilterInfo.js';
import {SelectInfo} from '../tables/SelectInfo.js';

export const CHART_SPACE_PATH = 'charts';
export const UI_PREFIX = `${CHART_SPACE_PATH}.ui`;
export const DATA_PREFIX = `${CHART_SPACE_PATH}.data`;

export const useScatterGL = toBoolean(sessionStorage.getItem('scatterGL'));       // defaults to false
export const useChartRedraw = toBoolean(sessionStorage.getItem('chartRedraw'));   // defaults to false

/*---------------------------- ACTIONS -----------------------------*/
export const CHART_ADD = `${DATA_PREFIX}/chartAdd`;
export const CHART_UPDATE = `${DATA_PREFIX}/chartUpdate`;
export const CHART_REMOVE = `${DATA_PREFIX}/chartRemove`;
export const CHART_HIGHLIGHT = `${DATA_PREFIX}/chartHighlight`;
export const CHART_SELECT = `${DATA_PREFIX}/chartSelectSelection`;
export const CHART_FILTER_SELECTION = `${DATA_PREFIX}/chartFilterSelection`;
export const CHART_SET_ACTIVE_TRACE = `${DATA_PREFIX}/chartSetActiveTrace`;
export const CHART_DATA_FETCH = `${DATA_PREFIX}/chartDataFetch`;
export const CHART_DATA_UPDATE = `${DATA_PREFIX}/chartDataUpdate`;
export const CHART_OPTIONS_UPDATE = `${DATA_PREFIX}/chartOptionsUpdate`;
export const CHART_OPTIONS_REPLACE = `${DATA_PREFIX}/chartOptionsReplace`;

export const CHART_UI_EXPANDED      = `${UI_PREFIX}.expanded`;
export const CHART_MOUNTED = `${UI_PREFIX}/mounted`;
export const CHART_UNMOUNTED = `${UI_PREFIX}/unmounted`;

const FIRST_CDEL_ID = '0'; // first data element id (if missing)

const FIREFLY_TRACE_TYPES = ['fireflyHistogram', 'fireflyHeatmap'];

export default {actionCreators, reducers};

const isDebug = () => get(window, 'firefly.debug', false);

function actionCreators() {
    return {
        [CHART_ADD]:     chartAdd,
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


/**
 * @global
 * @public
 * @typedef {Object} ChartDataElement - object, which describes a chart element
 * @prop {string} [id] chart data element id
 * @prop {string} type - data type like 'xycols' or 'histogram'
 * @prop {string} tblId - table id of the source table
 * @prop {Object} options - options for this chart element
 * @prop {Object} [data] - data for this chart element
 * @prop {Object} [meta] - meta data, which can include tblSource and other information about the data
 * @see ChartDataType
 */


/*
 * Add chart to the UI
 *
 * @param {Object} p - dispatch parameters
 *  @param {string} p.chartId - chart id
 *  @param {string} p.chartType - chart type, ex. 'scatter', 'histogram'
 *  @param {Array<ChartDataElement>} p.chartDataElements array
 *  @param {string} [p.groupId] - chart group for grouping charts together
 *  @param {boolean} [p.deletable] - is the chart deletable, if undefined: single chart in a group is not deletable, multiple are deletable
 *  @param {string} [p.help_id] - help id, if undefined, no help icon shows up
 *  @param {Function} [p.dispatcher=flux.process] - only for special dispatching uses such as remote
 *  @public
 *  @function dispatchChartAdd
 *  @memberof firefly.action
 */
export function dispatchChartAdd({chartId, chartType, chartDataElements, groupId='main', deletable, help_id, mounted=undefined, dispatcher= flux.process, ...rest}) {
    dispatcher({type: CHART_ADD, payload: {chartId, chartType, chartDataElements, groupId, deletable, help_id, mounted, ...rest}});
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

/**
 * Fetch chart data. When newOptions are present, they are compared to the most recent options to decide if the fetch is necessary
 * or only options need to be updated.
 *
 * @summary Fetch chart data.
 *  @param {Object} p - dispatch parameetrs
 *  @param {string} p.chartId - chart id
 *  @param {ChartDataElement} p.chartDataElement - chart data element id
 *  @param {Object} [p.newOptions] - options to use for data fetching
 *  @param {string} [p.invokedBy] - if defined, table action, which triggered the dispatch
 *  @param {Function} [p.dispatcher=flux.process] - only for special dispatching uses such as remote
 *  @public
 *  @function dispatchChartDataFetch
 *  @memberof firefly.action
 */
export function dispatchChartDataFetch({chartId, chartDataElement, newOptions, invokedBy, dispatcher=flux.process}) {
    dispatcher({type: CHART_DATA_FETCH, payload: {chartId, chartDataElement, newOptions, invokedBy}});
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

/*
// this dispatcher can be used to replace any of the chartDataElement's field
export function dispatchChartDataUpdate({chartId, chartDataElementId, tblId, options, isDataReady, data, meta, dispatcher=flux.process}) {
    dispatcher({type: CHART_DATA_UPDATE, payload: {chartId, chartDataElementId, options, data, meta, tblId}});
}
*/

/**
 * Update selected chart options.
 *
 * @param {Object} p - dispatch parameters
 * @param {string} p.chartId - chart id
 * @param {string} [p.chartDataElementId] - chart data element id, if omitted, default id is used
 * @param {Object} p.updates - each property key is a path to the property that needs to be updated, property value is the value
 * @param {boolean} [p.noFetch=false] - if true, only ui options have changed, the data are not affected
 * @param {Function} [p.dispatcher=flux.process] - only for special dispatching uses such as remote
 *  @public
 *  @function dispatchChartOptionsUpdate
 *  @memberof firefly.action
 */
export function dispatchChartOptionsUpdate({chartId, chartDataElementId=FIRST_CDEL_ID, updates, noFetch=false, dispatcher=flux.process}) {
    dispatcher({type: CHART_OPTIONS_UPDATE, payload: {chartId, chartDataElementId, updates, noFetch}});
}

/**
 * Completely replace the options of the given chart data element id
 *
 * @param {Object} p - dispatch parameters
 * @param {string} p.chartId - chart id
 * @param {string} [p.chartDataElementId] - chart data element id, if omitted, default id is used
 * @param {Object} p.newOptions - chart data element options
 * @param {Function} [p.dispatcher=flux.process] - only for special dispatching uses such as remote
 *  @public
 *  @function dispatchChartOptionsReplace
 *  @memberof firefly.action
 */
export function dispatchChartOptionsReplace({chartId, chartDataElementId=FIRST_CDEL_ID, newOptions, dispatcher=flux.process}) {
    dispatcher({type: CHART_OPTIONS_REPLACE, payload: {chartId, chartDataElementId, newOptions}});
}


/**
 * Put a chart into an expanded mode.
 * @param {string} chartId - chart id
 * @param {Function} [dispatcher=flux.process] - only for special dispatching uses such as remote
 */
export function dispatchChartExpanded(chartId, dispatcher=flux.process) {
    dispatcher( {type: CHART_UI_EXPANDED, payload: {chartId}});
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
// action creator for CHART_OPTIONS_UPDATE
export function makeChartOptionsUpdate(getChartDataType) {
    return (rawAction) => {
        return (dispatch) => {
            const {chartId, chartDataElementId, updates, noFetch} = rawAction.payload;
            const chartDataElement = getChartDataElement(chartId, chartDataElementId);
            if (!chartDataElement) {
                logError(`[chartOptionsUpdate] Chart data element is not found: ${chartId}, ${chartDataElementId}`);
                return;
            }
            if (noFetch) {
                dispatch({type: CHART_OPTIONS_UPDATE, payload: {chartId, chartDataElementId, updates}});
            } else {
                // create a new copy, since the options will be compared to decide if fetch is needed
                const newOptions = cloneDeep(chartDataElement.options);
                Object.keys(updates).forEach((path) => {
                    set(newOptions, path, updates[path]);
                });
                doChartDataFetch(dispatch, {chartId, chartDataElement, newOptions}, getChartDataType);
            }
        };
    };
}

// action creator for CHART_OPTIONS_REPLACE
export function makeChartOptionsReplace(getChartDataType) {
    return (rawAction) => {
        return (dispatch) => {
            const {chartId, chartDataElementId, newOptions} = rawAction.payload;
            const chartDataElement = getChartDataElement(chartId, chartDataElementId);
            if (!chartDataElement) {
                logError(`[chartOptionsReplace] Chart data element is not found: ${chartId}, ${chartDataElementId}`);
            } else {
                doChartDataFetch(dispatch, {chartId, chartDataElement, newOptions}, getChartDataType);
            }
        };
    };
}


// action creator for CHART_DATA_FETCH
export function makeChartDataFetch (getChartDataType) {
    return (rawAction) => {
        return (dispatch) => {
            doChartDataFetch(dispatch, rawAction.payload, getChartDataType);
        };
    };
}

function chartAdd(action) {
    return (dispatch) => {
        const {chartId, chartType} = action.payload;
        clearChartConn({chartId});

        if (chartType === 'plot.ly') {
            // the action payload might need to be updated for firefly trace types
            const newPayload = handleFireflyTraceTypes(action.payload);
            const actionToDispatch = (newPayload === action.payload) ? action : Object.assign({}, action, {payload: newPayload});
            dispatch(actionToDispatch);
            const {viewerId, data, fireflyData} = actionToDispatch.payload;
            if (viewerId) {
                // viewer will be added if it does not exist already
                dispatchAddViewerItems(viewerId, [chartId], 'plot2d');
            }

            // lazy table connection
            // handle reset case - when a chart is already mounted
            const {mounted} = getChartData(chartId);
            if (mounted > 0) {
                handleTableSourceConnections({chartId, data, fireflyData});
            }
        } else {
            dispatch(action);
        }
    };
}

function chartUpdate(action) {
    return (dispatch) => {
        var {chartId, changes} = action.payload;
        // remove any table's mappings from changes because it will be applied by the connectors.
        const changesWithoutTblMappings = omitBy(changes, (v) => isString(v) && v.match(TBL_SRC_PATTERN));
        set(action, 'payload.changes', changesWithoutTblMappings);
        dispatch(action);


        const {data, fireflyData} = Object.entries(changes)
                             .filter(([k,v]) => (k.startsWith('data') || k.startsWith('fireflyData')))
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
        const {data, tablesources, activeTrace:activeDataTrace=0, selected} = getChartData(chartId);

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
                const hlTrace = newTraceFrom(data[traceNum], [highlighted], HIGHLIGHTED_PROPS);
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
        const {activeTrace=0, data, tablesources} = getChartData(chartId);

        // when skipping hover, selecting chart points does not work
        // disable chart select in this case
        if (get(data, `${activeTrace}.hoverinfo`) === 'skip') { return; }

        let selected = undefined;
        if (!isEmpty(tablesources)) {
            const {tbl_id} = tablesources[activeTrace] || {};
            const {totalRows} = getTblById(tbl_id);
            const selectInfoCls = SelectInfo.newInstance({rowCount: totalRows});

            selIndexes.forEach((idx) => {
                selectInfoCls.setRowSelect(getRowIdx(data[activeTrace], idx), true);
            });
            TablesCntlr.dispatchTableSelect(tbl_id, selectInfoCls.data);
        }
        // avoid updating chart twice
        // don't update before table select
        if (!chartTrigger) {
            const hasSelected = !isEmpty(selIndexes);
            selected = newTraceFrom(data[activeTrace], selIndexes, SELECTED_PROPS);
            dispatchChartUpdate({chartId, changes: {hasSelected, selected, selection: undefined}});
        }
    };
}

function chartFilterSelection(action) {
    return (dispatch) => {
        const {chartId} = action.payload;
        const {activeTrace=0, selection, tablesources} = getChartData(chartId);
        if (!isEmpty(tablesources)) {
            const {tbl_id, mappings} = tablesources[activeTrace];
            const {x,y} = mappings;
            const [xMin, xMax] = get(selection, 'range.x', []);
            const [yMin, yMax] = get(selection, 'range.y', []);
            const {request} = getTblById(tbl_id);
            const filterInfoCls = FilterInfo.parse(request.filters);

            filterInfoCls.setFilter(x, '> ' + xMin);
            filterInfoCls.addFilter(x, '< ' + xMax);
            filterInfoCls.setFilter(y, '> ' + yMin);
            filterInfoCls.addFilter(y, '< ' + yMax);

            const newRequest = Object.assign({}, request, {filters: filterInfoCls.serialize()});
            TablesCntlr.dispatchTableFilter(newRequest);
            dispatchChartUpdate({chartId, changes:{selection: undefined}});
        }
    };
}

function setActiveTrace(action) {
    return (dispatch) => {
        const {chartId, activeTrace} = action.payload;
        const {data, tablesources, curveNumberMap} = getChartData(chartId);
        const changes = getActiveTraceChanges({activeTrace, data, tablesources, curveNumberMap});
        dispatchChartUpdate({chartId, changes});
    };
}

function getActiveTraceChanges({activeTrace, data, tablesources, curveNumberMap}) {
    const tbl_id = get(tablesources, [activeTrace, 'tbl_id']);
    let selected = undefined;
    let highlighted = undefined;
    let curveMap = undefined;
    if (tbl_id) {
        const {selectInfo, highlightedRow} = getTblById(tbl_id) || {};
        if (selectInfo) {
            const selectInfoCls = SelectInfo.newInstance(selectInfo);
            const selIndexes = Array.from(selectInfoCls.getSelected()).map((e)=>getPointIdx(data[activeTrace], e));
            if (selIndexes.length > 0) {
                selected = newTraceFrom(data[activeTrace], selIndexes, SELECTED_PROPS);
            }
        }
        highlighted = newTraceFrom(data[activeTrace], [getPointIdx(data[activeTrace], highlightedRow)], HIGHLIGHTED_PROPS);
        if (curveNumberMap) {
            curveMap = range(curveNumberMap.length).filter((idx) => (idx !== activeTrace));
            curveMap.push(activeTrace);
        }
    }
    return {activeTrace, selected, highlighted, selection: undefined, curveNumberMap: curveMap};
}

function isFireflyType(type) {
    return  FIREFLY_TRACE_TYPES.includes(type);
}

/**
 * Move firefly attributes from data and layout objects to fireflyData and fireflyLayout
 * @param payload – original action payload
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
                fd.dataType = d.type;
                fireflyData.push(fd);
                plotlyData.push(omit(d, ['firefly']));
            } else {
                fireflyData.push(undefined);
                plotlyData.push(d);
            }
        });
        newPayload = Object.assign({}, newPayload, {data: plotlyData, fireflyData});
    }
    if (layout.firefly) {
        const fireflyLayout = layout.firefly;
        const plotlyLayout = omit(layout, 'firefly');
        newPayload = Object.assign({}, newPayload, {layout: plotlyLayout, fireflyLayout});
    }
    return newPayload;
}


/*-----------------------------------------------------------------------------------------*/

/**
 *
 * @param dispatch
 * @param payload
 * @param payload.chartId
 * @param payload.chartDataElement
 * @param [payload.newOptions] - new options, if undefined, current chartDataElement's options are used
 * @param [payload.invokedBy] - if defined, table action type, which triggered the fetch
 * @param {Function} getChartDataType - function, which returns @link{ChartDataType} for a given chartDataElement's type
 */
function doChartDataFetch(dispatch, payload, getChartDataType) {
    const {chartId, chartDataElement, invokedBy} = payload;
    const oldOptions = chartDataElement.options;
    let {newOptions=oldOptions} = payload;

    const dataTypeId = chartDataElement.type;
    const cdt = dataTypeId ? getChartDataType(dataTypeId) : {};
    if (!cdt) {
        logError('No chart data type is found for '+dataTypeId);
        return;
    }

    const {id:chartDataElementId, tblId, data, meta={}} = chartDataElement;

    let dataFetchNeeded = false;
    let newMeta = meta;

    if (tblId) {
        const tblModel = getTblById(tblId);

        // if table load produced an error, we can not get chart data
        const error = get(tblModel, error);
        if (error) {
            const message = 'Failed to fetch chart data';
            logError(`${message}: ${error}`);
            dispatch(chartDataUpdate(
                {
                    chartId,
                    chartDataElementId,
                    isDataReady: true,
                    error: {message, error},
                    data: undefined
                }));
            return;
        }

        const tblSource = get(tblModel, 'tableMeta.resultSetID');
        const tblSourceChart = get(meta, 'tblSource');

        if (tblSourceChart !== tblSource) {
            newMeta = Object.assign({}, meta, {tblSource});

            // if the sort is invoked by table sort no fetch might be necessary, but the table source needs to be updated
            if (invokedBy === TablesCntlr.TABLE_SORT) {
                const fetchOnTblSort = !isUndefined(chartDataElement.fetchOnTblSort) ? chartDataElement.fetchOnTblSort :
                    !isUndefined(cdt.fetchOnTblSort) ? cdt.fetchOnTblSort : true;
                if (!fetchOnTblSort) {
                    dispatch({
                        type: CHART_DATA_UPDATE,
                        payload: {
                            chartId,
                            chartDataElementId,
                            meta: newMeta
                        }
                    });
                    return;
                }
            }

            dataFetchNeeded = true;
        }
    }

    if (dataFetchNeeded || (oldOptions !== newOptions)) {

        const getUpdatedOptions = chartDataElement.getUpdatedOptions || cdt.getUpdatedOptions || ((opts) => opts);
        const fetchData= chartDataElement.fetchData || cdt.fetchData;
        const fetchParamsChanged = chartDataElement.fetchParamsChanged  || cdt.fetchParamsChanged || (() => !isUndefined(fetchData));

        // need to fetch data if fetch parameters have changed
        dataFetchNeeded = dataFetchNeeded || !oldOptions ||  fetchParamsChanged(oldOptions, newOptions, chartDataElement);

        if (!dataFetchNeeded) {
            // when server call (fetch) parameters do not change but chart options change,
            // we do need to update options, but we can reuse the old chart data
            newOptions = getUpdatedOptions(newOptions, tblId, data, meta);
            dispatch({type: CHART_DATA_UPDATE,
                payload: {
                    chartId,
                    chartDataElementId,
                    options: newOptions
                }
            });
        } else {
            dispatch({type: CHART_DATA_FETCH,
                payload: {
                    chartId,
                    chartDataElementId,
                    tblId,
                    options: newOptions,
                    meta: newMeta
                }
            });
            fetchData(dispatch, chartId, chartDataElementId);
        }
    }
}

/**
 * Update data (and possibly other chartDataElement's fields)
 * @param payload
 * @param payload.chartId
 * @param payload.chartDataElementId
 * @param payload.isDataReady
 * @param payload.error
 * @param payload.error.message
 * @param payload.error.reason
 * @param payload.data
 * @param [payload.options]
 * @param [payload.meta]
 * @returns {{type: string, payload: object}} - chart data update action
 */
export function chartDataUpdate(payload) {
    return { type : CHART_DATA_UPDATE, payload };
}


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
        return state;
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

//const chartActions = [CHART_UI_EXPANDED,CHART_MOUNTED,CHART_UNMOUNTED,
//    CHART_ADD,CHART_REMOVE,CHART_DATA_FETCH,CHART_DATA_LOADED, CHART_OPTIONS_UPDATE,
//    TablesCntlr.TABLE_REMOVE, TablesCntlr.TABLE_SELECT];


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
            const {chartId, chartType, chartDataElements, mounted, ...rest}  = action.payload;
            // if a chart is replaced (added with the same id) mounted should not change
            const nMounted = isUndefined(mounted) ? get(state, [chartId, 'mounted']) : mounted;
            if (chartType==='plot.ly') {
                rest['_original'] = cloneDeep(action.payload);
                applyDefaults(rest);
                useScatterGL && changeToScatterGL(rest);

                // the first trace is put as the last curve for plotly rendering
                const tData = get(rest, ['data', 'length']);
                if (tData) {
                    Object.assign(rest, {activeTrace: tData-1, curveNumberMap: range(tData)});
                }
            }
            state = updateSet(state, chartId,
                omitBy({
                    chartType,
                    mounted: nMounted,
                    chartDataElements: chartDataElementsToObj(chartDataElements),
                    ...rest
                }, isUndefined));
            isDebug() && console.log(`ADD ${chartId} mounted ${nMounted}`);
            return state;
        }
        case (CHART_UPDATE) :
        {
            const {chartId, changes}  = action.payload;
            var chartData = getChartData(chartId);
            chartData = updateObject(chartData, changes);
            useScatterGL && changeToScatterGL(chartData);

            return updateSet(state, chartId, chartData);
        }
        case (CHART_REMOVE)  :
        {
            const {chartId} = action.payload;
            isDebug() && console.log('REMOVE '+chartId);
            clearChartConn(chartId);
            return omit(state, chartId);
        }
        case (CHART_DATA_FETCH)  :
        {
            const {chartId, chartDataElementId, ...rest} = action.payload;
            if (has(state, [chartId, 'chartDataElements', chartDataElementId])) {
                state = updateMerge(state, [chartId, 'chartDataElements', chartDataElementId],
                    {
                        chartDataElementId,
                        isDataReady: false,
                        data: undefined,
                        ...rest
                    });
            }
            return state;
        }
        case (CHART_DATA_UPDATE) :
        {
            const {chartId, chartDataElementId, ...rest} = action.payload;
            if (has(state, [chartId, 'chartDataElements', chartDataElementId])) {
                state = updateMerge(state, [chartId, 'chartDataElements', chartDataElementId], {...rest});
            }
            return state;
        }
        case (CHART_OPTIONS_UPDATE) :
        {
            const {chartId, chartDataElementId, updates} = action.payload;
            let options = get(state, [chartId, 'chartDataElements', chartDataElementId, 'options']);
            if (!options) {
                logError(`[CHART_OPTIONS_UPDATE] Chart data element optionsis not found: ${chartId}, ${chartDataElementId}`);
                return state;
            }
            Object.keys(updates).forEach((path) => {
                options = updateSet(options, path, updates[path]);
            });
            return updateSet(state, [chartId, 'chartDataElements', chartDataElementId, 'options'], options);
        }
        case (CHART_MOUNTED) :
        {
            const {chartId} = action.payload;
            if (has(state, chartId)) {
                const n = get(state, [chartId,'mounted'], 0);
                state = updateSet(state, [chartId,'mounted'], Number(n) + 1);
                isDebug() && console.log(`MOUNTED ${chartId} mounted ${state[chartId].mounted}`);
            }

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
                    logError(`CHART_UNMOUNT on unmounted chartId ${chartId}`);
                }
                isDebug() && console.log(`UNMOUNTED ${chartId} mounted ${state[chartId].mounted}`);
            }
            return state;
        }
        case (TablesCntlr.TABLE_SELECT) :
        {
            // TODO: allow to register chartType dependent sagas to take care of some specific cleanup like this?
            const tbl_id = action.payload.tbl_id; //also has selectInfo
            let newState = state;
            Object.keys(state).forEach((cid) => {
                const chartDataElements = state[cid].chartDataElements || [];
                Object.keys(chartDataElements).forEach( (id) => {
                    if (chartDataElements[id].tblId === tbl_id && has(chartDataElements[id], 'options.selection')) {
                        newState = updateSet(newState, [cid, 'chartDataElements', id, 'options', 'selection'], undefined);
                    }
                });
            });
            return newState;
        }
        case (TablesCntlr.TABLE_REMOVE) :
        {
            const tbl_id = action.payload.tbl_id;
            const chartsToDelete = [];
            let newState=state;
            Object.keys(state).forEach((cid) => {
                let chartDataElements = state[cid].chartDataElements || [];
                Object.keys(chartDataElements).forEach( (id) => {
                    if (chartDataElements[id].tblId === tbl_id) {
                        chartDataElements = omit(chartDataElements, id);
                        newState = updateSet(newState, [cid, 'chartDataElements'], chartDataElements);
                        if (isEmpty(chartDataElements)) {
                            chartsToDelete.push(cid);
                        }
                    }
                });
            });
            return chartsToDelete.length > 0 ? omit(newState, chartsToDelete) : newState;
        }
        default:
            return state;
    }
}

function chartDataElementsToObj(chartDataElementsArr) {
    if (!chartDataElementsArr) return;
    const obj = {};
    chartDataElementsArr.forEach((el, idx) => {
        if (isUndefined(el.id)) {
            el.id = (idx === 0) ? FIRST_CDEL_ID : String(idx);
        }
        if (el.options && !el.defaultOptions) {
            el.defaultOptions = cloneDeep(el.options);
        }
        obj[el.id] = el;
    });
    return obj;
}


/**
 * @param state - ui part of chart state
 * @param action - action
 * @returns {*} - updated ui part of the state
 */
function reduceUI(state={}, action={}) {
    switch (action.type) {
        case (CHART_UI_EXPANDED) :
            const {chartId}  = action.payload;
            return updateSet(state, 'expanded', chartId);
        default:
            return state;
    }
}

/**
 * Reset chart to the original
 * @param chartId
 */
export function resetChart(chartId) {
    const {_original} = getChartData(chartId);
    _original && dispatchChartAdd(_original);
}

export function removeTrace({chartId, traceNum}) {
    const {activeTrace, data, fireflyData, tablesources, curveNumberMap} = getChartData(chartId);
    const changes = {};

    [[data, 'data'], [fireflyData, 'fireflyData'], [tablesources, 'tablesources']].forEach(([arr,name]) => {
        if (arr && traceNum < arr.length) {
            changes[name] = arr.filter((e,i) => i !== traceNum);
        }
    });

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
                        tablesources: changes['tablesources'],
                        curveNumberMap: newCurveMap})
                );
            }
        }
    }

    if (!isEmpty(changes)) {
        dispatchChartUpdate({chartId, changes});
    }
}

export function getChartData(chartId, defaultChartData={}) {
    return get(flux.getState(), [CHART_SPACE_PATH, 'data', chartId], defaultChartData);
}

export function getChartDataElement(chartId, chartDataElementId=FIRST_CDEL_ID) {
    return get(flux.getState(), [CHART_SPACE_PATH, 'data', chartId, 'chartDataElements', chartDataElementId]);
}

/**
 * Get error object associated with the given chart data element
 * @param chartId
 * @returns {Array<{message:string, reason:object}>} an array of error objects
 */
export function getErrors(chartId) {
    const errors = [];
    const chartData = get(flux.getState(), [CHART_SPACE_PATH, 'data', chartId], {});
    if (chartData.chartType === 'plot.ly') {
        get(chartData, 'fireflyData', []).forEach((d) => {
            const error = get(d, 'error');
            error && errors.push(error);
        });
    } else {
        const chartDataElements = chartData.chartDataElements;
        if (chartDataElements) {
            Object.keys(chartDataElements).forEach((id) => {
                const error = chartDataElements[id].error;
                error && errors.push(error);
            });
        }
    }
    return errors;
}

export function dispatchError(chartId, traceNum, reason) {
    const message = `Failed to fetch trace${traceNum > 0 && traceNum} data`;
    logError(`${message}: ${reason}`);
    let reasonStr = `${reason}`.toLowerCase();
    if (reasonStr.match(/not supported/)) {
        reasonStr = 'Unsupported feature requested. Please choose valid options.';
    } else if (reasonStr.match(/invalid column/)) {
        reasonStr = 'Non-existent column or invalid expression. Please choose valid X and Y.';
    } else {
        reasonStr = 'Please contact Help Desk. Check browser console for more information.';
    }
    const changes = [];
    changes.push(`fireflyData.${traceNum}.error`, {message, reason: reasonStr});
    changes.push(`fireflyData.${traceNum}.isLoading`, false);
    dispatchChartUpdate({chartId, changes});
}

export function getExpandedChartProps() {
    const chartId = get(flux.getState(), [CHART_SPACE_PATH, 'ui', 'expanded']);
    return {chartId};
}

export function getNumCharts(tblId, mounted, groupId) {
    let numRelated = 0;
    const state = get(flux.getState(), [CHART_SPACE_PATH, 'data']);
    Object.keys(state).forEach((cid) => {
        if ((isUndefined(mounted) || (Boolean(state[cid].mounted) === mounted)) &&
            (isUndefined(groupId) || (state[cid].groupId === groupId))) {
            const chartDataElements = state[cid].chartDataElements;
            let dependsOnTblId = isUndefined(tblId) ? true : undefined;
            if (dependsOnTblId === undefined) {
                if (chartDataElements) {
                    dependsOnTblId =
                        Object.keys(chartDataElements).some((id) => {
                            return (chartDataElements[id].tblId === tblId);
                        });
                } else {
                    const {tablesources, activeTrace=0} = getChartData(cid);
                    const tablesource = get(tablesources, [activeTrace]);
                    const tbl_id = get(tablesource, 'tbl_id');
                    dependsOnTblId = (tbl_id === tblId);
                }
            }
            if (dependsOnTblId) {
                numRelated++;
            }
        }
    });
    return numRelated;
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

/**
 *
 * @param {string} tblId - table id
 * @param {string} invokedBy - table action, which triggered the update
 */
export function updateRelatedData(tblId, invokedBy) {
    const state = get(flux.getState(), [CHART_SPACE_PATH, 'data']);
    Object.keys(state).forEach((cid) => {
        if (isChartMounted(cid)) {
            const chartDataElements = state[cid].chartDataElements;
            if (chartDataElements) {
                Object.keys(chartDataElements).forEach((id) => {
                    if (chartDataElements[id].tblId === tblId) {
                        dispatchChartDataFetch({chartId: cid, chartDataElement: chartDataElements[id], invokedBy});
                    }
                });
            }
        }
    });
}

export function updateChartData(chartId, tblId) {
    const chartDataElements = get(flux.getState(), [CHART_SPACE_PATH, 'data', chartId, 'chartDataElements']);
    if (chartDataElements) {
        Object.keys(chartDataElements).forEach((id) => {
            if (chartDataElements[id].tblId === tblId) {
                dispatchChartDataFetch({chartId, chartDataElement: chartDataElements[id]});
            }
        });
    }
}

export function getRelatedTblIds(chartId) {
    const tblIds = [];
    const chartDataElements = get(flux.getState(), [CHART_SPACE_PATH, 'data', chartId, 'chartDataElements']);
    if (chartDataElements) {
        Object.keys(chartDataElements).forEach((id) => {
            const tblId = chartDataElements[id].tblId;
            if (tblIds.indexOf(tblId)<0) {
                tblIds.push(tblId);
            }
        });
    }
    return tblIds;
}

export function isChartMounted(chartId) {
    // when chart is added, consider it mounted
    return Boolean(get(flux.getState(), [CHART_SPACE_PATH, 'data', chartId, 'mounted']));
}

