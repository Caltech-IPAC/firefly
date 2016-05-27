/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {updateSet, updateDelete} from '../../util/WebUtil.js';
import {set, get, has, findKey} from 'lodash';

import * as Cntlr from '../TablesCntlr.js';
import * as TblUtil from '../TableUtil.js';


/*---------------------------- REDUCERS -----------------------------*/
export function resultsReducer(state={results:{}}, action={}) {
    var root = state.results;
    if (!action || !action.payload) return root;
    const {tbl_id, tbl_group} = action.payload;
    switch (action.type) {
        case (Cntlr.TBL_RESULTS_ADDED)     :
        case (Cntlr.TBL_RESULTS_UPDATE)    :
            if (tbl_id ) {
                const changes = set({}, [tbl_group, 'tables', tbl_id], action.payload);
                return TblUtil.smartMerge(root, changes);
            } else return root;

        case Cntlr.TBL_RESULTS_ACTIVE :
            return updateSet(root, [tbl_group,'active'], tbl_id);
            
        case (Cntlr.TABLE_REMOVE)    :
            return removeTable(root, action);
        
        default:
            return root;
    }
}

function removeTable(root, action) {
    const {tbl_id} = action.payload;
    Object.keys(root).forEach( (tbl_group) => {
        if (has(root, [tbl_group, 'tables', tbl_id])) {
            root = updateDelete(root, [tbl_group, 'tables'], tbl_id);

            if (tbl_id === get(root, [tbl_group,'active'])) {
                // active table have been remove. set it to the first available table
                const first = findKey(tbl_group.tables);
                const newActiveId = first && first.tbl_id;
                root = updateSet(root, [tbl_group,'active'], newActiveId);
            }
        }
    });

    return root;
}

/*---------------------------- DISPATCHERS -----------------------------*/
