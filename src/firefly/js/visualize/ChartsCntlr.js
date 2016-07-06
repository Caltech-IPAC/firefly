/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {has, get, isUndefined, omit} from 'lodash';

import {flux} from '../Firefly.js';
import {updateSet, updateMerge} from '../util/WebUtil.js';

import * as TablesCntlr from '../tables/TablesCntlr.js';

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
 * @param {boolean} optionsPopup - true,  if options should be displayed as a popup
 */
export function dispatchChartExpanded(chartId, tblId, chartType, optionsPopup) {
    flux.process( {type: CHART_UI_EXPANDED, payload: {chartId, tblId, chartType, optionsPopup}});
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

const chartActions = [CHART_UI_EXPANDED,CHART_MOUNTED,CHART_UNMOUNTED,DELETE];

export function reducer(state={ui:{}}, action={}) {
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

            case (CHART_UI_EXPANDED) :
                const {optionsPopup} = action.payload;
                //return updateSet(root, 'expanded', {chartId, tblId});
                return updateSet(state, 'ui.expanded', {chartId, tblId, chartType, optionsPopup});
            case (CHART_MOUNTED) :

                if (!has(state, ['tbl', tblId])) {
                    return updateSet(state, ['tbl',tblId], {[uChartId]: {mounted: true}});
                } else {
                    if (get(state, ['tbl', tblId, uChartId, 'mounted'])) {
                        //other version of the same chart is mounted
                        let n = get(state, ['tbl', tblId, uChartId, 'n']);
                        n = n ? Number(n) : 1;
                        return updateMerge(state, ['tbl', tblId, uChartId], {upToDate: true, n: n + 1});
                    } else {
                        return updateSet(state, ['tbl', tblId, uChartId], {mounted: true});
                    }
                }
            case (CHART_UNMOUNTED) :
                if (has(state, ['tbl', tblId])) {
                    if (get(state, ['tbl', tblId, uChartId, 'mounted'])) {
                        let n = get(state, ['tbl', tblId, uChartId, 'n']);
                        n = n ? Number(n) : 1;
                        if (n > 1) {
                            //multiple versions of the same chart are mounted
                            return updateMerge(state, ['tbl', tblId, uChartId], {n: n - 1});
                        }
                    } else {
                        return updateSet(state, ['tbl', tblId, uChartId], {mounted: false});
                    }
                }
                return state;
            case (DELETE) :
                if (has(state, ['tbl', tblId, uChartId])) {
                    if (Object.keys(state['tbl'][tblId]).length > 1) {
                        return updateSet(state, ['tbl', tblId], omit(state['tbl'][tblId], [uChartId]));
                    } else {
                        return updateSet(state, 'tbl', omit(state['tbl'], [tblId]));
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