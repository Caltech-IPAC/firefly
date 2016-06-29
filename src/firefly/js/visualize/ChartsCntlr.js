/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {updateSet} from '../util/WebUtil.js';

export const CHART_SPACE_PATH = 'charts';
export const UI_PREFIX = `${CHART_SPACE_PATH}.ui`;

export const CHART_UI_EXPANDED      = `${UI_PREFIX}.expanded`;

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

export function reducer(state={ui:{}}, action={}) {
    //var root = state.ui;
    switch (action.type) {
        case (CHART_UI_EXPANDED) :
            const {chartId, tblId, chartType, optionsPopup} = action.payload;
            //return updateSet(root, 'expanded', {chartId, tblId});
            return updateSet(state, 'ui.expanded', {chartId, tblId, chartType, optionsPopup});
        default:
            //return root;
            return state;
    }
}