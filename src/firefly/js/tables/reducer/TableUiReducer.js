/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {has, get, isEmpty, cloneDeep, findKey, omit, set} from 'lodash';

import {updateSet, updateMerge} from '../../util/WebUtil.js';
import * as Cntlr from '../TablesCntlr.js';
import {getTblInfo, isTableLoaded, getAllColumns, getColumn, getColumnIdx, getTableUiById} from '../TableUtil.js';
import {getNumFilters} from '../FilterInfo.js';
import {makeDefaultRenderer} from '../ui/TableRenderer.js';
import {applyTblPref} from '../TablePref.js';


/*
`table_space`(table store) is separated into 3 parts; data, ui, and results
This is the 'ui' reducer.  It manages the current UI elements of a table.
Below are known props of a table UI:
tbl_id:
startIdx:
endIdx:
filterCount:
request:    the request used to fetch this table
data:       the current page of data
tableModel: the table model
tableMeta:  tableMeta from table model
title:      UI element used as table title
totalRows:
hlRowIdx:
currentPage:
pageSize:
totalPages:
highlightedRow:
selectInfo:
showLoading:
showMask:
triggeredBy:
cellRenderers:  cellRenderers used on this table.  if not set, defaults will be created
All table options are saved here, e.g.
    backgroundable, showFilters, showUnits, selectable,  ...
*/

/*---------------------------- REDUCERS -----------------------------*/
export function uiReducer(state={ui:{}}, action={}) {

    let root = state.ui;
    if (!action || !action.payload) return root;

    root = handleUiUpdates(root, action, state);
    root = handleTableUpdates(root, action, state);
    return root;
}


function handleTableUpdates(root, action, state) {

    const {tbl_ui_id, tbl_id} = action.payload;
    switch (action.type) {
        case (Cntlr.TABLE_REMOVE)    :
            return removeTable(root, action);

        case (Cntlr.TBL_RESULTS_ADDED) :
            const options = onUiUpdate(get(action, 'payload.options', {}));
            return updateMerge(root, [tbl_ui_id], {tbl_ui_id, tbl_id, triggeredBy: 'byTable', ...options});

        case (Cntlr.TABLE_FETCH)      :
        case (Cntlr.TABLE_FILTER)      :
        case (Cntlr.TABLE_SORT)     :
        case (Cntlr.TABLE_UPDATE)   :
        case (Cntlr.TABLE_REPLACE)  :
        case (Cntlr.TABLE_SELECT)   :
        case (Cntlr.TABLE_LOADED) :
        case (Cntlr.TABLE_HIGHLIGHT)  :
            // state is in-progress(fresh) data.. use it to reduce ui state.
            return uiStateReducer(root, get(state, ['data', tbl_id]), action.type);
        default:
            return root;
    }

}

function handleUiUpdates(root, action, state) {
    const {tbl_ui_id, tbl_id} = action.payload;
    switch (action.type) {
        case (Cntlr.TBL_UI_UPDATE)    :
            const changes = onUiUpdate(omit(action.payload, 'options'));
            let ui = updateMerge(root, [tbl_ui_id], {triggeredBy: action.type, ...changes});
            if (action.payload.options)  ui = updateMerge(ui, [tbl_ui_id, 'options'], action.payload.options);

            // update previous
            const {columns, columnWidths} = getTableUiById(tbl_ui_id) || {};        // these are current values
            if (has(changes, 'columns'))      ui = updateSet(ui, [tbl_ui_id,'previous', 'columns'], columns);
            if (has(changes, 'columnWidths')) ui = updateSet(ui, [tbl_ui_id,'previous', 'columnWidths'], columnWidths);

            return ui;

        case (Cntlr.TBL_UI_EXPANDED) :
            const tbl_group = findKey(get(state, 'results'), (o) =>  has(o, ['tables', tbl_id]));
            return updateSet(root, 'expanded', {tbl_group, tbl_id, tbl_ui_id});

        default:
            return root;
    }
}

/*---------------------------- utils -----------------------------*/


function removeTable(root, action) {
    const {tbl_id} = action.payload;
    Object.keys(root).filter( (ui_id) => {
        return get(root, [ui_id, 'tbl_id']) === tbl_id;
    }).forEach( (ui_id) => {
        root = omit(root, [ui_id]);
    });
    return root;
}

function uiStateReducer(ui, tableModel, action) {
    // if (!get(tableModel, 'tableData')) return ui;
    const {startIdx, endIdx, tbl_id, request, ...others} = getTblInfo(tableModel);
    const {sortInfo, filters:filterInfo, sqlFilter} = request || {};
    const filterCount = getNumFilters(request);
    const showLoading = !isTableLoaded(tableModel);
    const showMask = tableModel && tableModel.isFetching;

    var data = has(tableModel, 'tableData.data') ? tableModel.tableData.data.slice(startIdx, endIdx) : [];

    var uiData = {tbl_id, startIdx, endIdx, sortInfo, filterInfo, sqlFilter, filterCount, request, data, showLoading, showMask, triggeredBy: 'byTable', ...others};

    Object.keys(ui).filter( (ui_id) => {
        return get(ui, [ui_id, 'tbl_id']) === tbl_id;
    }).forEach( (tbl_ui_id) => {
        const {columns, title, previous, cellRenderers} = ui?.[tbl_ui_id] || {};

        if (title) {
            delete uiData.title;        // don't update UI title once it's set
        }

        // create cellRenderers if not exist
        if (!cellRenderers) {
            tableModel?.tableData?.columns?.forEach((col,idx) => {
                set(uiData, ['cellRenderers', idx], makeDefaultRenderer(col));
            });
        }

        if (action === Cntlr.TABLE_LOADED) applyTblPref(tbl_ui_id, uiData);

        uiData = adjustColInfo({uiData, tableModel, columns, previous});

        ui = updateMerge(ui, [tbl_ui_id], uiData);
    });
    return ui;
}

function onUiUpdate(uiData) {
    if (!get(uiData, 'showToolbar', true)) {
        // if showToolbar is false, make the other related UI props to be false
        uiData =  Object.assign(uiData, {showTitle:false, showPaging:false, showSave:false, showFilterButton:false});
    }
    return uiData;
}

function adjustColInfo({uiData, tableModel, columns, previous}) {
    const origCols = cloneDeep(get(tableModel, 'tableData.columns', []));
    if (isEmpty(columns) || !hasSameCnames(tableModel, columns)) {
        if (!isEmpty(origCols) && previous ) {
            const {columns, columnWidths} = previous;   // update new columns info based on previous state.
            const prevTbl = {tableData:{columns}};      // partial table used for getting column info
            const nColWidths = new Array(origCols.length).fill(-1);
            origCols.forEach((col, idx) => {
                const c = getColumn(prevTbl, col.name);
                if (c) {
                    const cidx = getColumnIdx(prevTbl, col.name);
                    ['visibility', 'fixed', 'resizable'].forEach((k) => c[k] && (col[k] = c[k]));
                    columnWidths?.[cidx] && set(nColWidths, idx, columnWidths[cidx]);
                }
            });
            uiData.columnWidths = nColWidths;
            uiData.columns = origCols;
            uiData.previous = {columns:origCols, columnWidths: nColWidths};
        } else {
            uiData.columns = origCols;
        }
    }
    return uiData;
}

function hasSameCnames(tableModel, columns=[]) {
    return getAllColumns(tableModel).map((c) => c.name).join() === columns.map((c) => c.name).join();
}
