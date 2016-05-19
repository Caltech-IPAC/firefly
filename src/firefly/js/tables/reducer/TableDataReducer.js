/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, omit} from 'lodash';

import {updateSet} from '../../util/WebUtil.js';
import * as TblUtil from '../TableUtil.js';
import * as Cntlr from '../TablesCntlr.js';
import {SelectInfo} from '../SelectInfo.js';



/*---------------------------- REDUCERS -----------------------------*/
export function dataReducer(state={data:{}}, action={}) {
    const {tbl_id, selectInfo} = action.payload || {};
    var root = state.data;
    switch (action.type) {
        case (Cntlr.TABLE_SELECT)  :
            if (selectInfo) {
                return updateSet(root, [tbl_id, 'selectInfo'], selectInfo);
            } else return root;

        case (Cntlr.TABLE_NEW_LOADED)  :
            const statusPath = [tbl_id, 'tableMeta', 'Loading-Status'];
            if (get(root, statusPath, 'COMPLETED') !== 'COMPLETED') {
                return updateSet(root, statusPath, 'COMPLETED');
            } else return root;

        case (Cntlr.TABLE_HIGHLIGHT)  :
        case (Cntlr.TABLE_UPDATE)  :
            return TblUtil.smartMerge(root, {[tbl_id] : action.payload});

        case (Cntlr.TABLE_NEW)  :
        case (Cntlr.TABLE_REPLACE)  :
            const nTable = Object.assign({isFetching:false, selectInfo: SelectInfo.newInstance({}).data},action.payload);
            return updateSet(root, [tbl_id], nTable);

        case (Cntlr.TABLE_REMOVE)  :
            return omit(root, [tbl_id]);

        default:
            return root;
    }
}
