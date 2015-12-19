/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import cntlr from '../AppDataCntlr.js';


function reducer(state={}, action={}) {

    const menuAction = state.menuItems && state.menuItems.find( (el) => el && el.action === action.type);
    const selected = menuAction ? action.type : '';
    return Object.assign({}, state, {selected});
}

export default {
    reducer
};