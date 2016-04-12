/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'react-addons-update';
import {set, get, has, omit} from 'lodash';

import * as Cntlr from '../TablesCntlr.js';
import * as TblUtil from '../TableUtil.js';


/*---------------------------- REDUCERS -----------------------------*/
export function resultsReducer(state={results:{}}, action={}) {
    var root = state.results;
    if (!action || !action.payload) return root;
    switch (action.type) {
        case (Cntlr.TBL_RESULTS_ADDED)     :
        case (Cntlr.TBL_RESULTS_UPDATE)    :
            const {tbl_ui_id} = action.payload;
            if (tbl_ui_id ) {
                const changes = set({}, ['tables', tbl_ui_id], action.payload);
                changes.active = get(action, 'payload.tbl_id');
                return TblUtil.smartMerge(root, changes);
            } else return root;

        case Cntlr.TBL_RESULTS_ACTIVE :
            const {tbl_id} = action.payload;
            if (!has(root, 'active')) {
                set(root, 'active', undefined);
            }
            return update(root, {active: {$set: tbl_id}});
            
        case (Cntlr.TABLE_REMOVE)    :
            return removeTable(root, action);
        
        default:
            return root;
    }
}

function removeTable(root, action) {
    const {tbl_id} = action.payload;
    Object.keys(root.tables).filter( (tbl_ui_id) => {
        return get(root, ['tables', tbl_ui_id, 'tbl_id']) === tbl_id;
    }).forEach( (tbl_ui_id) => {
        if (has(root, ['tables', tbl_ui_id])) {
            const changes = omit(root.tables, tbl_ui_id);
            root = update(root, {tables: {$set: changes}});
        }
    });
    return root;
}

/*---------------------------- DISPATCHERS -----------------------------*/
