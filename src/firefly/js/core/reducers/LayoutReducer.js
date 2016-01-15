/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {pick, isEqual} from 'lodash';

import cntlr from '../AppDataCntlr.js';


function reducer(state={}, action={}, menu) {
    var nState = state;

    switch (action.type) {
        case cntlr.SHOW_SEARCH :
            const nSearch = action.payload && action.payload.search;
            if (nSearch !== state.search) {
                nState = Object.assign({}, state, {search: nSearch});
            }
            return nState;

        case cntlr.UPDATE_LAYOUT :
            if (! isEqual(pick(state, Object.keys(action.payload)), action.payload) ) {
                nState = Object.assign(state, action.payload);
            }
            return nState;

        default:
            return nState;
    }

}
export default {
    reducer
};