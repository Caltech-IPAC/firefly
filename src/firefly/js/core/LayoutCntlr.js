/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'react-addons-update';
import {get, set, has, pickBy, isUndefined} from 'lodash';

import {smartMerge} from '../tables/TableUtil.js';
import {flux} from '../Firefly.js';

export const LAYOUT_PATH = 'layout';

export const LO_EXPANDED = {
    mode: 'expanded',
    tables: {mode: 'expanded', view: 'tables'},
    images: {mode: 'expanded', view: 'images'},
    xyPlots: {mode: 'expanded', view: 'xyPlots'},
    none: {mode: 'expanded', view: undefined}
};

export const LO_STANDARD = {
    mode: 'standard',
    tri_view: { mode: 'standard', view: 'tri_view'},
    image_xyplot: { mode: 'standard', view: 'image_xyplot'},
    image_table: { mode: 'standard', view: 'image_table'},
    xyplot_table: { mode: 'standard', view: 'xyplot_table'}
};

/*---------------------------- Actions ----------------------------*/

export const UPDATE_LAYOUT     = 'layout.updateLayout';
export const SET_LAYOUT_MODE   = 'layout.setLayoutMode';


/*---------------------------- Reducers ----------------------------*/

export function reducer(state={}, action={}) {

    switch (action.type) {
        case UPDATE_LAYOUT :
            return smartMerge(state, action.payload);

        case SET_LAYOUT_MODE :
            const {mode, view} = action.payload;
            if (!has(state, ['mode', mode])) {
                set(state, ['mode', mode], undefined);
            }
            return update(state, {mode: {[mode]: {$set: view}}});

        default:
            return state;
    }

}



/*---------------------------- DISPATCHERS -----------------------------*/
/**
 * Updates the app_data layoutInfo.  This data is responsible for the layout of the top level components
 * i.e. search panel, results panel...
 * @param search    boolean. show the search panel.  defaults to false.
 * @param results   boolean. show the results panel. defaults to true.
 * @param hasTables boolean.  Table data available.
 * @param hasImages boolean. Image data available.
 * @param hasXyPlots boolean. XY Plot data available.
 */
export function dispatchUpdateLayout({search, results, hasTables, hasImages, hasXyPlots}) {
    flux.process({type: UPDATE_LAYOUT, payload: pickBy({search, results, hasTables, hasImages, hasXyPlots}, (v)=>(!isUndefined(v)))});
}

/**
 * see AppDataUtil.LO_MODE for available options.
 * @param mode standard or expanded
 * @param view available values from the selected mode.
 */
export function dispatchSetLayoutMode({mode=LO_STANDARD.mode, view}) {
    flux.process({type: SET_LAYOUT_MODE, payload: {mode, view}});
}



/*------------------------- Util functions -------------------------*/
export function getExpandedMode() {
    return get(flux.getState(), ['layout','mode','expanded']);
}

export function getStandardMode() {
    return get(flux.getState(), ['layout','mode','standard']);
}
