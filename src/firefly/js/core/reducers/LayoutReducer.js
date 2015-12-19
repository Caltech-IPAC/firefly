/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import cntlr from '../AppDataCntlr.js';


function reducer(state={}, action={}, menu) {

    switch (action.type) {
        case cntlr.SEARCH_SHOW :
            return Object.assign({}, state, {search: true});

        case cntlr.SEARCH_HIDE :
            return Object.assign({}, state, {search: false});

        default:
            return state;
    }

}
export default {
    reducer
};