/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {cloneDeep, has, get, isEmpty, isUndefined, omit, omitBy, set} from 'lodash';
import shallowequal from 'shallowequal';

import {flux} from '../Firefly.js';
import {updateSet, updateMerge} from '../util/WebUtil.js';
import {getTblById} from '../tables/TableUtil.js';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {logError} from '../util/WebUtil.js';

export const CHART_SPACE_PATH = 'charts';
export const UI_PREFIX = `${CHART_SPACE_PATH}.ui`;
export const DATA_PREFIX = `${CHART_SPACE_PATH}.data`;


/*---------------------------- ACTIONS -----------------------------*/
export const CHART_ADD = `${DATA_PREFIX}/chartAdd`;
export const CHART_REMOVE = `${DATA_PREFIX}/chartRemove`;
export const CHART_DATA_FETCH = `${DATA_PREFIX}/chartDataFetch`;
export const CHART_DATA_UPDATE = `${DATA_PREFIX}/chartDataUpdate`;
export const CHART_OPTIONS_UPDATE = `${DATA_PREFIX}/chartOptionsUpdate`;
export const CHART_OPTIONS_REPLACE = `${DATA_PREFIX}/chartOptionsReplace`;

export const CHART_UI_EXPANDED      = `${UI_PREFIX}.expanded`;
export const CHART_MOUNTED = `${UI_PREFIX}/mounted`;
export const CHART_UNMOUNTED = `${UI_PREFIX}/unmounted`;

const FIRST_CDEL_ID = '0'; // first data element id (if missing)

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
export function dispatchChartAdd({chartId, chartType, chartDataElements, groupId='main', deletable, help_id, dispatcher= flux.process}) {
    dispatcher({type: CHART_ADD, payload: {chartId, chartType, chartDataElements, deletable, help_id, groupId}});
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
        const tblSource = get(getTblById(tblId), 'tableMeta.tblFilePath');
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
        dataFetchNeeded = dataFetchNeeded || !oldOptions || fetchParamsChanged(oldOptions, newOptions);

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


/**
 * @param state - ui part of chart state
 * @param action - action
 * @returns {*} - updated ui part of the state
 */
function reduceData(state={}, action={}) {
    //if (chartActions.indexOf(action.type) < 0) { return state; } // useful when debugging
    switch (action.type) {
        case (CHART_ADD) :
            const {chartId, chartType, groupId, deletable, help_id, chartDataElements}  = action.payload;

            state = updateSet(state, chartId,
                omitBy({chartType, chartDataElements: chartDataElementsToObj(chartDataElements), groupId, deletable, help_id}, isUndefined));
            return state;
        case (CHART_REMOVE)  :
        {   const {chartId} = action.payload;
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
            }
            return state;
        }
        case (TablesCntlr.TABLE_SELECT) :
        {
            // TODO: allow to register chartType dependent sagas to take care of some specific cleanup like this?
            const tbl_id = action.payload.tbl_id; //also has selectInfo
            let newState = state;
            Object.keys(state).forEach((cid) => {
                const chartDataElements = state[cid].chartDataElements;
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
                let chartDataElements = state[cid].chartDataElements;
                Object.keys(state[cid].chartDataElements).forEach( (id) => {
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


export function getChartData(chartId) {
    return get(flux.getState(), [CHART_SPACE_PATH, 'data', chartId]);
}

export function getChartDataElement(chartId, chartDataElementId=FIRST_CDEL_ID) {
    return get(flux.getState(), [CHART_SPACE_PATH, 'data', chartId, 'chartDataElements', chartDataElementId]);
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
            const dependsOnTblId = isUndefined(tblId) ? true :
                Object.keys(chartDataElements).some((id) => {
                    return (chartDataElements[id].tblId === tblId);
                });
            if (dependsOnTblId) { numRelated++; }
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
            Object.keys(chartDataElements).forEach((id) => {
                if (chartDataElements[id].tblId === tblId) {
                    dispatchChartDataFetch({chartId: cid, chartDataElement: chartDataElements[id], invokedBy});
                }
            });
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

