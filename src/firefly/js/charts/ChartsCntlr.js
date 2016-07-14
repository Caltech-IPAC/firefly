/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {has, get, isUndefined, omit} from 'lodash';
import shallowequal from 'shallowequal';

import {flux} from '../Firefly.js';
import {updateSet, updateMerge} from '../util/WebUtil.js';

import * as TablesCntlr from '../tables/TablesCntlr.js';
import {reduceXYPlot} from './XYPlotCntlr.js';
import {reduceHistogram} from './HistogramCntlr.js';

export const CHART_SPACE_PATH = 'charts';
export const UI_PREFIX = `${CHART_SPACE_PATH}.ui`;

export const CHART_UI_EXPANDED      = `${UI_PREFIX}.expanded`;
export const CHART_MOUNTED = `${UI_PREFIX}/mounted`;
export const CHART_UNMOUNTED = `${UI_PREFIX}/unmounted`;
export const DELETE = `${UI_PREFIX}/delete`;

/**
 * request to put a chart into an expanded mode.
 * @param {string} chartId - chart id
 * @param {string} tblId - table id, to which this chart is related. Acts as a group id
 * @param {string} chartType (ex. scatter, histogram)
 */
export function dispatchChartExpanded(chartId, tblId, chartType) {
    flux.process( {type: CHART_UI_EXPANDED, payload: {chartId, tblId, chartType}});
}

/*
 *  Dispatched when chart becomes visible (is rendered for the first time after being invisible)
 *  When chart is mounted, its data need to be in sync with the related table
 *  @param {string} tblId - table id (table data for this chart)
 *  @param {string} chartId - chart id
 *  @param {string} chartType - chart type, ex. scatter, histogram
 */
export function dispatchChartMounted(tblId, chartId, chartType, dispatcher= flux.process) {
    dispatcher({type: CHART_MOUNTED, payload: {tblId, chartId, chartType}});
}

/*
 *  Dispatched when chart becomes invisible
 *  When chart is unmounted, its data synced with the related table only when it becomes mounted again
 *  @param {string} tblId - table id (table data for this chart)
 *  @param {string} chartId - chart id
 *  @param {string} chartType - chart type, ex. scatter, histogram
 */
export function dispatchChartUnmounted(tblId, chartId, chartType, dispatcher= flux.process) {
    dispatcher({type: CHART_UNMOUNTED, payload: {tblId, chartId, chartType}});
}

/*
 * Delete chart and related data
 *  @param {string} tblId - table id (table data for this chart)
 *  @param {string} chartId - chart id
 *  @param {string} chartType - chart type, ex. scatter, histogram
 */
export function dispatchDelete(tblId, chartId, chartType, dispatcher= flux.process) {
    dispatcher({type: DELETE, payload: {tblId, chartId, chartType}});
}

const uniqueId = (chartId, chartType) => { return `${chartType}|${chartId}`; };
const isChartType = (uChartId, chartType) => { return uChartId.startsWith(chartType); };


/*
 Possible structure of store:
  /ui
    expanded: Object - the information about expanded chart
    {
         chartId: string
         tblId: string
         chartType: string, ex. 'scatter', 'histogram'
    }
  /tbl
    tblId: Object - the name of this node matches table id
    {
        uChartId: Object - the name of this node matches 'chartType|chartId'
        {
            mounted: boolean,
            n: number, undefined means 1
        }
    }
  /xyplot - parameters and data for scatter plot, see XYPlotCntlr
  /histogram - parameters and data for histogram plot, see HistogramCntlr
*/

export function reducer(state={ui:{}, tbl: {}, xyplot:{}, histogram:{}}, action={}) {

    if (!action.type.startsWith(TablesCntlr.DATA_PREFIX) && !action.type.startsWith(CHART_SPACE_PATH)){
        return state;
    }

    const nstate = {...state};

    nstate.xyplot = reduceXYPlot(state['xyplot'], action);
    nstate.histogram = reduceHistogram(state['histogram'], action);

    // generic for all chart types
    nstate.ui = reduceUI(state['ui'], action);
    // organized by table id
    nstate.tbl = reduceByTbl(state['tbl'], action);

    if (shallowequal(state, nstate)) {
        return state;
    } else {
        return nstate;
    }
}

const chartActions = [CHART_UI_EXPANDED,CHART_MOUNTED,CHART_UNMOUNTED,DELETE];

/**
 * @param state - ui part of chart state
 * @param action - action
 * @returns {*} - updated ui part of the state
 */
function reduceUI(state={}, action={}) {
    if (chartActions.indexOf(action.type) > -1) {
        const {chartId, tblId, chartType}  = action.payload;
        switch (action.type) {
            case (CHART_UI_EXPANDED) :
                return updateSet(state, 'expanded', {chartId, tblId, chartType});
            default:
                return state;
        }
    } else {
        return state;
    }

}


/**
 *
 * @param state - by table part of chart state, stores info which charts are mounted
 * @param action - action
 * @returns {*} - new by table part of chart state
 */
function reduceByTbl(state={}, action={}) {
    if (action.type === TablesCntlr.TABLE_REMOVE) {
        const {tbl_id} = action.payload;
        if (has(state, tbl_id)) {
            return Object.assign({}, omit(state, [tbl_id]));
        }
        return state;
    } else if (chartActions.indexOf(action.type) > -1) {
        const {chartId, tblId, chartType}  = action.payload;
        const uChartId = uniqueId(chartId, chartType);
        switch (action.type) {
            case (CHART_MOUNTED) :
                if (!has(state, tblId)) {
                    return updateSet(state, tblId, {[uChartId]: {mounted: true}});
                } else {
                    if (get(state, [tblId, uChartId, 'mounted'])) {
                        //other version of the same chart is mounted
                        let n = get(state, [tblId, uChartId, 'n']);
                        n = n ? Number(n) : 1;
                        return updateMerge(state, [tblId, uChartId], {upToDate: true, n: n + 1});
                    } else {
                        return updateSet(state, [tblId, uChartId], {mounted: true});
                    }
                }
            case (CHART_UNMOUNTED) :
                if (has(state, [tblId])) {
                    if (get(state, [tblId, uChartId, 'mounted'])) {
                        let n = get(state, [tblId, uChartId, 'n']);
                        n = n ? Number(n) : 1;
                        if (n > 1) {
                            //multiple versions of the same chart are mounted
                            return updateMerge(state, [tblId, uChartId], {n: n - 1});
                        } else {
                            return updateSet(state, [tblId, uChartId], {mounted: false});
                        }
                    }
                }
                return state;
            case (DELETE) :
                if (has(state, [tblId, uChartId])) {
                    if (Object.keys(state[tblId]).length > 1) {
                        return updateSet(state, tblId, omit(state[tblId], [uChartId]));
                    } else {
                        return Object.assign({}, omit(state, [tblId]));
                    }
                }
                return state;
            default:
                return state;
        }
    } else {
        return state;
    }

}


export function getExpandedChartProps() {
    return get(flux.getState(), [CHART_SPACE_PATH, 'ui', 'expanded']);
}


export function getNumRelatedCharts(tblId, mounted, chartType) {
    let numRelated = 0;
    const byTblSpace = get(flux.getState(), [CHART_SPACE_PATH, 'tbl']);
    if (byTblSpace) {
        if (isUndefined(tblId)) {
            Object.keys(byTblSpace).forEach((tblId)=>{
                numRelated += getNumRelatedCharts(tblId, mounted);
            });
        } else if (byTblSpace[tblId]) {
            Object.keys(byTblSpace[tblId]).forEach((uChartId) => {
                if (isUndefined(mounted)||byTblSpace[tblId][uChartId].mounted===mounted) {
                    if (isUndefined(chartType) || isChartType(uChartId, chartType)) {
                        numRelated++;
                    }
                }
            });
        }
    }
    return numRelated;
}

export function isChartMounted(tblId, chartId, chartType) {
    const uChartId = uniqueId(chartId, chartType);
    return Boolean(get(flux.getState(), [CHART_SPACE_PATH, 'tbl', tblId, uChartId, 'mounted']));
}
