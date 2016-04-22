/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import update from 'react-addons-update';
import {get} from 'lodash';

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
                return update(root, { [tbl_id] : {selectInfo: {$set: selectInfo}}});
            } else return root;

        case (Cntlr.TABLE_NEW_LOADED)  :
            if (get(root, [tbl_id, 'tableMeta', 'Loading-Status'], 'COMPLETED') !== 'COMPLETED') {
                return update(root, { [tbl_id] : {tableMeta: {['Loading-Status']:  {$set: 'COMPLETED'}}}});
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
