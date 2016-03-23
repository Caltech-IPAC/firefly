/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'react-addons-update';
import {set, has, omit} from 'lodash';

import {flux} from '../Firefly.js';
import {smartMerge, uniqueTblUiId} from './TableUtil.js';


export const TABLE_UI_PATH = 'table_ui';


/*---------------------------- ACTIONS -----------------------------*/
export const TBL_UI_ADDED = `${TABLE_UI_PATH}.added`;
export const TBL_UI_UPDATE = `${TABLE_UI_PATH}.update`;
export const TBL_UI_REMOVED = `${TABLE_UI_PATH}.removed`;
export const TBL_UI_STATE_UPDATE = `${TABLE_UI_PATH}.stateUpdate`;
/*---------------------------- CREATORS ----------------------------*/

/*---------------------------- REDUCERS -----------------------------*/
export function reducer(state={}, action={}) {
    if (!action || !action.payload) return state;

    switch (action.type) {
        case (TBL_UI_ADDED)     :
        case (TBL_UI_UPDATE)    :
            const {tbl_ui_gid, expandedMode, tbl_ui} = action.payload;
            if (tbl_ui_gid && tbl_ui && tbl_ui.tbl_id ) {
                const groupUi = set({}, ['results',tbl_ui_gid, 'tables', tbl_ui.tbl_ui_id], tbl_ui);
                groupUi.results[tbl_ui_gid].expandedMode = expandedMode;
                return smartMerge(state, groupUi);
            } else return state;
        case (TBL_UI_STATE_UPDATE)    :
            const {tbl_ui_id} = action.payload;
            if (tbl_ui_id) {
                const uiState = set({}, ['work',tbl_ui_id], action.payload);
                return smartMerge(state, uiState);
            } else return state;
        case (TBL_UI_REMOVED)    :
            return tableRemoved(state, action);

        default:
            return state;
    }
}

function tableRemoved(state, action) {
    const {tbl_ui_gid, tbl_ui_id} = action.payload;
    if (has(state, ['results', tbl_ui_gid, 'tables', tbl_ui_id])) {
        const nTables = omit(state.results[tbl_ui_gid].tables, tbl_ui_id);
        var nState = update(state, {results: {[tbl_ui_gid]: {tables: {$set: nTables}}}});
        if (has(state, ['work', tbl_ui_id])) {
            const nWork = omit(state.work, tbl_ui_id);
            nState = update(nState, {work: {$set: nWork}});
        }
        return nState;
    } else return state;
}

/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * Add this table into the TableResults
 * @param tbl_ui_gid  table ui group
 * @param tbl_ui_id   table ui id
 * @param tbl_id      table id.
 * @param removable  true if this table can be removed from view.
 */
export function dispatchTableAdded(tbl_ui_gid, tbl_ui_id=uniqueTblUiId(), tbl_id, removable=true) {
    const tbl_ui = {tbl_id, removable, tbl_ui_id};
    flux.process( {type: TBL_UI_ADDED, payload: {tbl_ui_gid, tbl_ui}});
}

/**
 * Add this table into the TableResults
 * @param tbl_ui_gid  table ui group
 * @param tbl_ui_id      table ui id.
 */
export function dispatchTableUiRemoved(tbl_ui_gid, tbl_ui_id) {
    flux.process( {type: TBL_UI_REMOVED, payload: {tbl_ui_gid, tbl_ui_id}});
}

/**
 * Update the table ui properties
 * @param tbl_ui_gid  table ui group
 * @param tbl_ui      table ui properties.
 */
export function dispatchTableUpdate(tbl_ui_gid, tbl_ui) {
    flux.process( {type: TBL_UI_UPDATE, payload: {tbl_ui_gid, tbl_ui}});
}

