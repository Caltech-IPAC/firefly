/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';

import cntlr from '../AppDataCntlr.js';
import {smartMerge} from '../../tables/TableUtil.js';


function reducer(state={}, action={}) {

    switch (action.type) {
        case cntlr.SHOW_SEARCH :
            const nState = {search: get(action, 'payload.search')};
            return smartMerge(state, nState);

        case cntlr.UPDATE_LAYOUT :
            return smartMerge(state, action.payload);

        default:
            return state;
    }

}
export default {
    reducer
};