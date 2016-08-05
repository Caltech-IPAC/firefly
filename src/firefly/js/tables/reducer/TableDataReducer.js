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

        case (Cntlr.TABLE_LOADED)  :
            const statusPath = [tbl_id, 'tableMeta', 'Loading-Status'];
            if (get(root, statusPath, 'COMPLETED') !== 'COMPLETED') {
                return updateSet(root, statusPath, 'COMPLETED');
            } else return root;

        case (Cntlr.TABLE_HIGHLIGHT)  :
        case (Cntlr.TABLE_UPDATE)  :
            return TblUtil.smartMerge(root, {[tbl_id] : {isFetching:false, ...action.payload}});

        case (Cntlr.TABLE_FETCH)      :
        case (Cntlr.TABLE_FILTER)      :
        case (Cntlr.TABLE_SORT)     :
        case (Cntlr.TABLE_REPLACE)  :
            const rowCount = action.payload.totalRows || get(action, 'payload.tableData.data.length', 0);
            const nTable = Object.assign({isFetching:false, selectInfo: SelectInfo.newInstance({rowCount}).data}, action.payload);
            return updateSet(root, [tbl_id], nTable);

        case (Cntlr.TABLE_REMOVE)  :
            return omit(root, [tbl_id]);

        default:
            return root;
    }
}
