/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {set, has, get, isEmpty, cloneDeep, findKey} from 'lodash';

import {updateSet, updateMerge} from '../../util/WebUtil.js';
import * as Cntlr from '../TablesCntlr.js';
import * as TblUtil from '../TableUtil.js';


/*---------------------------- REDUCERS -----------------------------*/
export function uiReducer(state={ui:{}}, action={}) {
    var root = state.ui;
    if (!action || !action.payload) return root;
    const {tbl_ui_id, tbl_id} = action.payload;
    switch (action.type) {
        case (Cntlr.TBL_UI_UPDATE)    :
            return updateAllUi(root, tbl_id, tbl_ui_id, action.payload);
        case (Cntlr.TABLE_REMOVE)    :
            return removeTable(root, action);

        case (Cntlr.TBL_RESULTS_ADDED) :
            const {options} = action.payload || {}; 
            return Object.assign(root, {[tbl_ui_id]:{tbl_ui_id, tbl_id, ...options}});

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

        case (Cntlr.TBL_UI_EXPANDED) :
            const {tbl_ui_id, tbl_id} = action.payload;
            const tbl_group = findKey(get(state, 'results'), (o) => {
                return has(o, ['tables', tbl_id]);
            });
            return updateSet(root, 'expanded', {tbl_group, tbl_id, tbl_ui_id});

        default:
            return root;
    }
}

function removeTable(root, action) {
    const {tbl_id} = action.payload;
    Object.keys(root).filter( (ui_id) => {
        return get(root, [ui_id, 'tbl_id']) === tbl_id;
    }).forEach( (ui_id) => {
        root = Object.assign({}, root);
        Reflect.deleteProperty(root, [ui_id]);
    });
    return root;
}

/*---------------------------- DISPATCHERS -----------------------------*/

/*---------------------------- utils -----------------------------*/

function uiStateReducer(ui, tableModel) {
    // if (!get(tableModel, 'tableData')) return ui;
    const {startIdx, endIdx, tbl_id, ...others} = TblUtil .getTblInfo(tableModel);
    const filterInfo = get(tableModel, 'request.filters');
    const filterCount = filterInfo ? filterInfo.split(';').length : 0;
    const sortInfo = get(tableModel, 'request.sortInfo');
    const showLoading = !TblUtil.isTableLoaded(tableModel);
    const showMask = tableModel.isFetching;

    var data = has(tableModel, 'tableData.data') ? tableModel.tableData.data.slice(startIdx, endIdx) : [];
    var tableRowCount = data.length;

    var uiData = {tbl_id, startIdx, endIdx, tableRowCount, sortInfo, filterInfo, filterCount, data, showLoading, showMask, ...others};

    Object.keys(ui).filter( (ui_id) => {
        return get(ui, [ui_id, 'tbl_id']) === tbl_id;
    }).forEach( (tbl_ui_id) => {
        const columns = get(ui, [tbl_ui_id, 'columns']);
        uiData.columns = ensureColumns({tableModel, columns});
        ui = updateMerge(ui, [tbl_ui_id], uiData);
    });
    return ui;
}

function updateAllUi(ui, tbl_id, tbl_ui_id, payload) {
    if (tbl_ui_id) {
        return updateMerge(ui, tbl_ui_id, payload);
    } else {
        Object.keys(ui).filter( (ui_id) => {
            return get(ui, [ui_id, 'tbl_id']) === tbl_id;
        }).forEach( (tbl_ui_id) => {
            const changes = set({}, [tbl_ui_id], payload);
            ui = TblUtil.smartMerge(ui, changes);
        });
    }
    return ui;
}

const ensureColumns = ({tableModel, columns}) => {
    if (isEmpty(columns)) {
        return cloneDeep(get(tableModel, 'tableData.columns', []));
    } else {
        return columns;
    }
};
