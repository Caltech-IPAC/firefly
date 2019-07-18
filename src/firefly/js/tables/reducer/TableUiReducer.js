/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {has, get, isEmpty, cloneDeep, findKey, omit} from 'lodash';

import {updateSet, updateMerge} from '../../util/WebUtil.js';
import * as Cntlr from '../TablesCntlr.js';
import {getTblInfo, isTableLoaded, smartMerge, getAllColumns} from '../TableUtil.js';


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
        case (Cntlr.TBL_RESULTS_REMOVE)    :
            return removeTable(root, action);

        case (Cntlr.TBL_RESULTS_ADDED) :
            const options = onUiUpdate(get(action, 'payload.options', {}));
            return updateSet(root, [tbl_ui_id], {tbl_ui_id, tbl_id, triggeredBy: 'byTable', ...options});

        case (Cntlr.TABLE_FETCH)      :
        case (Cntlr.TABLE_FILTER)      :
        case (Cntlr.TABLE_SORT)     :
        case (Cntlr.TABLE_UPDATE)   :
        case (Cntlr.TABLE_REPLACE)  :
        case (Cntlr.TABLE_SELECT)   :
        case (Cntlr.TABLE_LOADED) :
        case (Cntlr.TABLE_HIGHLIGHT)  :
            // state is in-progress(fresh) data.. use it to reduce ui state.
            return uiStateReducer(root, get(state, ['data', tbl_id]));
        default:
            return root;
    }

}

function handleUiUpdates(root, action, state) {
    const {tbl_ui_id, tbl_id} = action.payload;
    switch (action.type) {
        case (Cntlr.TBL_UI_UPDATE)    :
            const changes = onUiUpdate(action.payload);
            return updateMerge(root, [tbl_ui_id], {triggeredBy: action.type, ...changes});

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

function uiStateReducer(ui, tableModel) {
    // if (!get(tableModel, 'tableData')) return ui;
    const {startIdx, endIdx, tbl_id, ...others} = getTblInfo(tableModel);
    const filterInfo = get(tableModel, 'request.filters');
    const filterCount = filterInfo ? filterInfo.split(';').length : 0;
    const sortInfo = get(tableModel, 'request.sortInfo');
    const showLoading = !isTableLoaded(tableModel);
    const showMask = tableModel && tableModel.isFetching;

    var data = has(tableModel, 'tableData.data') ? tableModel.tableData.data.slice(startIdx, endIdx) : [];
    var tableRowCount = data.length;

    var uiData = {tbl_id, startIdx, endIdx, tableRowCount, sortInfo, filterInfo, filterCount, data, showLoading, showMask, triggeredBy: 'byTable', ...others};

    Object.keys(ui).filter( (ui_id) => {
        return get(ui, [ui_id, 'tbl_id']) === tbl_id;
    }).forEach( (tbl_ui_id) => {
        const columns = get(ui, [tbl_ui_id, 'columns']);
        uiData.columns = ensureColumns({tableModel, columns});

        if (!isEmpty(columns) && get(tableModel, 'tableData.columns') && !hasSameCnames(tableModel, columns)) {
            uiData.columnWidths = undefined;
        }
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

function ensureColumns({tableModel, columns}) {
    if (isEmpty(columns) || !hasSameCnames(tableModel, columns)) {
        return cloneDeep(get(tableModel, 'tableData.columns', []));
    } else {
        return smartMerge(get(tableModel, 'tableData.columns', []), columns);
    }
}

function hasSameCnames(tableModel, columns=[]) {
    return getAllColumns(tableModel).map((c) => c.name).join() === columns.map((c) => c.name).join();
}
