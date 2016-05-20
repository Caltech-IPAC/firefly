/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'react-addons-update';
import {get, set, has, pickBy, isUndefined, isEmpty} from 'lodash';
import Enum from 'enum';

import {smartMerge} from '../tables/TableUtil.js';
import {flux} from '../Firefly.js';

export const LAYOUT_PATH = 'layout';

// this enum is flaggable, therefore you can use any combination of the 3, i.e. 'tables | images'.
export const LO_VIEW = new Enum(['none', 'tables', 'images', 'xyPlots'], { ignoreCase: true });
export const LO_MODE = new Enum(['expanded', 'standard']);

/*---------------------------- Actions ----------------------------*/

export const SET_LAYOUT_MODE    = `${LAYOUT_PATH}.setLayoutMode`;
export const SHOW_DROPDOWN      = `${LAYOUT_PATH}.showDropDown`;
export const HIDE_DROPDOWN      = `${LAYOUT_PATH}.hideDropDown`;


/*---------------------------- Reducers ----------------------------*/

export function reducer(state={dropDown: {}}, action={}) {
    const {mode, view} = action.payload || {};

    switch (action.type) {
        case SET_LAYOUT_MODE :
            if (!has(state, ['mode', mode])) {
                set(state, ['mode', mode], undefined);
            }
            return update(state, {mode: {[mode]: {$set: view}}});

        case SHOW_DROPDOWN :
            return update(state, {dropDown: {$set: {visible: true, view}}});

        case HIDE_DROPDOWN :
            return update(state, {dropDown: {$set: {visible: false}}});

        default:
            return state;
    }

}



/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * set the layout mode of the application.  see LO_MODE and LO_VIEW enums for options.
 * @param mode standard or expanded
 * @param view the select view to be displayed.
 */
export function dispatchSetLayoutMode(mode=LO_MODE.standard, view) {
    flux.process({type: SET_LAYOUT_MODE, payload: {mode, view}});
}

/**
 * show the drop down container
 * @param view name of the component to display in the drop-down container
 */
export function dispatchShowDropDown({view}) {
    flux.process({type: SHOW_DROPDOWN, payload: {view}});
}

/**
 * hide the drop down container
 */
export function dispatchHideDropDown() {
    flux.process({type: HIDE_DROPDOWN, payload: {}});
}


/*------------------------- Util functions -------------------------*/
export function getExpandedMode() {
    return get(flux.getState(), ['layout','mode','expanded']);
}

export function getStandardMode() {
    return get(flux.getState(), ['layout','mode','standard']);
}

export function getDropDownInfo() {
    return get(flux.getState(), 'layout.dropDown', {visible: false});
}

export function getLayouInfo() {
    const hasImages = get(flux.getState(), 'allPlots.plotViewAry.length') > 0;
    const hasTables = !isEmpty(get(flux.getState(), 'table_space.results.main.tables', {}));
    const hasXyPlots = hasTables;
    const expanded = getExpandedMode();
    const standard = getStandardMode();
    const ddInfo = getDropDownInfo();
    
    return {hasImages, hasTables, hasXyPlots, expanded, standard, 
            dropdownVisible: ddInfo.visible, dropdownView: ddInfo.view};
}
