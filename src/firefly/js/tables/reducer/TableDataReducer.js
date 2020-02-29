/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, omit} from 'lodash';

import {updateSet} from '../../util/WebUtil.js';
import * as TblUtil from '../TableUtil.js';
import * as Cntlr from '../TablesCntlr.js';
import {SelectInfo} from '../SelectInfo.js';
import {getColByUCD, getColByUtype, getTblById, getColumn} from '../TableUtil.js';
import {isObsCoreLike} from '../../util/VOAnalyzer.js';
import {MetaConst} from '../../data/MetaConst.js';


/*---------------------------- REDUCERS -----------------------------*/
export function dataReducer(state={data:{}}, action={}) {
    let root = state.data;

    if (!action || !action.payload) return root;

    root = handleTableUpdates(root, action);
    root = handleObsCoreTable(root, action);

    return root;
}

/*-------------------------------------------------------------------------*/

function handleTableUpdates(root, action) {

    const {tbl_id, selectInfo, highlightedRow} = action.payload;

    const doSelect = () => {
        if (selectInfo) {
            const nroot = clientTableSelectionSync(root, tbl_id, selectInfo); // no change for non-client tables
            return updateSet(nroot, [tbl_id, 'selectInfo'], selectInfo);
        } else return root;
    };

    const doLoaded = () => {
        const statusPath = [tbl_id, 'tableMeta', 'Loading-Status'];
        if (get(root, statusPath, 'COMPLETED') !== 'COMPLETED') {
            return updateSet(root, statusPath, 'COMPLETED');
        } else return root;
    };

    const doHighlight = () => {
        const updates = {[tbl_id] : {isFetching:false, tbl_id, highlightedRow}};
        return TblUtil.smartMerge(root, updates);
    };

    const doUpdate = () => {
        const updates = {[tbl_id] : {isFetching:false, ...action.payload}};
        return TblUtil.smartMerge(root, updates);
    };

    const doFetch = () => {
        const nTable = Object.assign({ isFetching: true, selectInfo: SelectInfo.newInstance({rowCount: 0}).data }, action.payload);
        const origTableModel = get(root, [tbl_id, 'origTableModel']);
        if (origTableModel) { nTable.origTableModel = origTableModel; }
        return updateSet(root, [tbl_id], nTable);
    };

    const doReplace = () => {
        const rowCount = action.payload.totalRows || get(action, 'payload.tableData.data.length', 0);
        const nTable = Object.assign({ isFetching: false, selectInfo: SelectInfo.newInstance({rowCount}).data }, action.payload);
        return updateSet(root, [tbl_id], nTable);
    };

    switch (action.type) {
        case (Cntlr.TABLE_SELECT) :
            return doSelect();
        case (Cntlr.TABLE_LOADED) :
            return doLoaded();
        case (Cntlr.TABLE_HIGHLIGHT)    :
            return doHighlight();
        case (Cntlr.TABLE_UPDATE)       :
            return doUpdate();
        case (Cntlr.TABLE_FETCH)      :
            return doFetch();
        case (Cntlr.TABLE_REPLACE)  :
            return doReplace();
        case (Cntlr.TABLE_REMOVE)  :
            return omit(root, [tbl_id]);
    }
    return root;
}

/* Applies ObsCore's related logic to the table before it's placed into the store */
function handleObsCoreTable(root, action) {

    const {tbl_id} = action.payload || {};

    if (action.type === Cntlr.TABLE_LOADED) {
        // obscore related
        root = checkReleaseDate(root, tbl_id);
        root = checkDataRights(root, tbl_id);
    }
    return root;
}

function checkReleaseDate (root, tbl_id) {

    if (!tbl_id) return root;
    const tableModel = getTblById(tbl_id);

    if (!getColumn(tableModel, MetaConst.RELEASE_DATE_COL)) {
        const col = getColByUtype(tableModel, 'obscore:Curation.releaseDate')[0] ||
                    getColByUCD(tableModel, 'time.release')[0] ||
                    ( isObsCoreLike(tableModel) && getColumn(tableModel, 'obs_release_date'));

        return col ? updateSet(root, [tbl_id, 'tableMeta', MetaConst.RELEASE_DATE_COL], col.name) : root;
    }
    return root;
};

function checkDataRights (root, tbl_id) {

    // values may be one of Public/Secure/Proprietary.  Secure is authenticated, public access is allowed.

    if (!tbl_id) return root;
    const tableModel = getTblById(tbl_id);

    if (!getColumn(tableModel, MetaConst.DATARIGHTS_COL)) {
        const col = getColByUtype(tableModel, 'obscore:Curation.rights')[0] ||
                    // getColByUCD(tableModel, 'meta.code')[0] ||                           // This UCD is not exclusive to data_rights.  We should not use it.
                    ( isObsCoreLike(tableModel) && getColumn(tableModel, 'data_rights'));

        return col ? updateSet(root, [tbl_id, 'tableMeta', MetaConst.DATARIGHTS_COL], col.name) : root;
    }
    return root;
};


function clientTableSelectionSync(root, tbl_id, selectInfo) {
    const origTableModel = get(root, [tbl_id, 'origTableModel']);
    const origTblData = get(origTableModel, 'tableData.data');
    if (!origTableModel || !origTblData) {
        return root;
    }
    // if ROW_IDX column is present in the table, its row indexes differ from the original table
    const tableModel = get(root, [tbl_id]);
    const idxCol = TblUtil.getColumnIdx(tableModel, 'ROW_IDX');
    if (idxCol < 0) {
        return updateSet(root, [tbl_id, 'origTableModel', 'selectInfo'], selectInfo);
    }
    const data = get(tableModel, 'tableData.data');
    const selectInfoCls = SelectInfo.newInstance(selectInfo);
    const origSelectInfoCls = SelectInfo.newInstance(get(origTableModel, 'selectInfo'));
    // set original table selections to match those in the derived table
    data.forEach((row, i) => {
        const origIdx = parseInt(row[idxCol]);
        origSelectInfoCls.setRowSelect(origIdx, selectInfoCls.isSelected(i));
    });
    return updateSet(root, [tbl_id, 'origTableModel', 'selectInfo'], origSelectInfoCls.data);
}