/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import update from 'react-addons-update';
import {get} from 'lodash';

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
            const nTable = Object.assign({selectInfo: SelectInfo.newInstance({}).data},action.payload);
            return update(root, {$merge: {[tbl_id] : nTable}});

        case (Cntlr.TABLE_REMOVE)  :
            root = Object.assign({}, root);
            Reflect.deleteProperty(root, [tbl_id]);
            return root;

        default:
            return root;
    }
}
