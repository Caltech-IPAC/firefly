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
    var root = state.data;
    switch (action.type) {
        case (Cntlr.TABLE_SELECT)  :
        {
            const {tbl_id, selectInfo} = action.payload;
            if (selectInfo) {
                const nroot = clientTableSelectionSync(root, tbl_id, selectInfo); // no change for non-client tables
                return updateSet(nroot, [tbl_id, 'selectInfo'], selectInfo);
            } else return root;
        }
        case (Cntlr.TABLE_LOADED)  :
        {
            const {tbl_id} = action.payload;
            const statusPath = [tbl_id, 'tableMeta', 'Loading-Status'];
            if (get(root, statusPath, 'COMPLETED') !== 'COMPLETED') {
                return updateSet(root, statusPath, 'COMPLETED');
            } else return root;
        }
        case (Cntlr.TABLE_HIGHLIGHT)  :
        case (Cntlr.TABLE_UPDATE)  :
        {
            var {tbl_id} = action.payload;
            const updates = {[tbl_id] : {isFetching:false, ...action.payload}};
            return TblUtil.smartMerge(root, updates);
        }
        case (Cntlr.TABLE_FETCH)      :
        {
            const {tbl_id} = action.payload || {};
            const nTable = Object.assign({isFetching:true, selectInfo: SelectInfo.newInstance({rowCount:0}).data}, action.payload);
            const origTableModel = get(root, [tbl_id, 'origTableModel']);
            if (origTableModel) { nTable.origTableModel = origTableModel; }
            return updateSet(root, [tbl_id], nTable);
        }
        case (Cntlr.TABLE_REPLACE)  :
        {
            const {tbl_id} = action.payload || {};
            const rowCount = action.payload.totalRows || get(action, 'payload.tableData.data.length', 0);
            const nTable = Object.assign({isFetching:false, selectInfo: SelectInfo.newInstance({rowCount}).data}, action.payload);
            return updateSet(root, [tbl_id], nTable);
        }
        case (Cntlr.TABLE_REMOVE)  :
        {
            const {tbl_id} = action.payload;
            return omit(root, [tbl_id]);
        }
        default:
            return root;
    }
}

function clientTableSelectionSync(state, tbl_id, selectInfo) {
    const origTableModel = get(state, [tbl_id, 'origTableModel']);
    const origTblData = get(origTableModel, 'tableData.data');
    if (!origTableModel || !origTblData) {
        return state;
    }
    // if ORIG_IDX column is present in the table, its row indexes differ from the original table
    const tableModel = get(state, [tbl_id]);
    const idxCol = TblUtil.getColumnIdx(tableModel, 'ORIG_IDX');
    if (idxCol < 0) {
        return updateSet(state, [tbl_id, 'origTableModel', 'selectInfo'], selectInfo);
    }
    const data = get(tableModel, 'tableData.data');
    const selectInfoCls = SelectInfo.newInstance(selectInfo);
    const origSelectInfoCls = SelectInfo.newInstance(get(origTableModel, 'selectInfo'));
    // set original table selections to match those in the derived table
    data.forEach((row, i) => {
        const origIdx = parseInt(row[idxCol]);
        origSelectInfoCls.setRowSelect(origIdx, selectInfoCls.isSelected(i));
    });
    return updateSet(state, [tbl_id, 'origTableModel', 'selectInfo'], origSelectInfoCls.data);
}