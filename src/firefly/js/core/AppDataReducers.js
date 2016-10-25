/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {SHOW_DROPDOWN} from './LayoutCntlr.js';
import {SET_ALERTS} from './AppDataCntlr.js';
import {updateSet} from '../util/WebUtil.js';

export function menuReducer(state={}, action={}) {
    switch (action.type) {
        case SHOW_DROPDOWN  :
            const {visible, view=''} = action.payload;
            const selected = visible ? view : '';
            return Object.assign({}, state, {selected});

        default:
            return state;
    }
}

export function alertsReducer(state={}, action={}) {
    switch (action.type) {
        case SET_ALERTS :
            const {msg=''} = action.payload || {};
            return updateSet(state, ['alerts'], {msg});
        default:
            return state;
    }
}
