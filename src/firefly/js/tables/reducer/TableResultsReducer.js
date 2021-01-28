/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {updateSet, updateDelete} from '../../util/WebUtil.js';
import {set, isNil, has} from 'lodash';

import * as Cntlr from '../TablesCntlr.js';
import * as TblUtil from '../TableUtil.js';


/*---------------------------- REDUCERS -----------------------------*/
export function resultsReducer(state={results:{}}, action={}) {
    var root = state.results;
    if (!action || !action.payload) return root;
    switch (action.type) {
        case (Cntlr.TBL_RESULTS_ADDED)     :
        case (Cntlr.TBL_RESULTS_UPDATE)    :
        {
            const {options={}, ...rest} = action.payload;
            const flatten = Object.assign(rest, options);   // if options is passed as a prop, flatten it into the payload.
            const {tbl_id, tbl_group} = flatten;
            if (tbl_id ) {
                const changes = set({}, [tbl_group, 'tables', tbl_id], flatten);
                set(changes, [tbl_group, 'name'], tbl_group);
                return TblUtil.smartMerge(root, changes);
            } else return root;
        }
        case Cntlr.TBL_RESULTS_ACTIVE :
        {
            const {tbl_id, tbl_group} = action.payload;
            return updateSet(root, [tbl_group,'active'], tbl_id);
        }
        case Cntlr.TABLE_REMOVE    :
        case Cntlr.TBL_RESULTS_REMOVE    :
            return removeTable(root, action);

        case Cntlr.TABLE_UPDATE   :
            return updateTableInfo(root, action);

        default:
            return root;
    }
}

function removeTable(root, action) {
    const {tbl_id} = action.payload;
    Object.keys(root).forEach( (tbl_group) => {
        if (has(root, [tbl_group, 'tables', tbl_id])) {
            root = updateDelete(root, [tbl_group, 'tables'], tbl_id);
        }
    });

    return root;
}

function updateTableInfo(root, action) {

    const {tbl_id, title, tableMeta, request} = action.payload;
    const mtitle = title ?? tableMeta?.title ?? request?.META_INFO?.title;
    Object.keys(root).forEach( (tbl_group) => {
        if (has(root, [tbl_group, 'tables', tbl_id])) {
            if (!isNil(mtitle)) {               // only update if title updated.
                root = updateSet(root, [tbl_group, 'tables', tbl_id, 'title'], mtitle);
            }
        }
    });

    return root;
}
/*---------------------------- DISPATCHERS -----------------------------*/
