/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'react-addons-update';
import {pickBy, get, isEmpty} from 'lodash';

import {flux} from '../Firefly.js';
import {smartMerge} from './TableUtil.js';
import {Table} from './Table.js';
import * as TblUtil from './TableUtil.js';
import * as TablesCntlr from './TablesCntlr.js';


export const TABLE_UI_PATH = 'table_ui';


/*---------------------------- ACTIONS -----------------------------*/
export const TBL_UI_ADDED = `${TABLE_UI_PATH}.uiAdded`;
export const TBL_UI_GOTO_PAGE = `${TABLE_UI_PATH}.uiGotoPage`;
export const TBL_UI_RESIZE = `${TABLE_UI_PATH}.resize`;
export const TBL_UI_COL_RESIZE = `${TABLE_UI_PATH}.colResize`;
/*---------------------------- CREATORS ----------------------------*/

export function gotoPage(action) {
    return (dispatch) => {

        if (action.payload) {
            var {tbl_id, currentPage, pageSize, hlRowIdx} = action.payload;
            const startIdx = (currentPage-1) * pageSize;
            const endIdx = startIdx + pageSize;
            var table = Table.findTblById(tbl_id);
            if (table && table.has(startIdx, endIdx)) {
                TablesCntlr.dispatchHighlightRow(tbl_id, startIdx+hlRowIdx);
            } else {
                const request = Object.assign({}, table.data.request, {startIdx, pageSize});
                TblUtil.doFetchTable(request, startIdx+hlRowIdx).then ( (tableModel) => {
                    dispatch( TablesCntlr.loadTable({type:TablesCntlr.LOAD_TABLE, payload: tableModel}) );
                }).catch( (error) => {
                    TblUtil.error(error);
                    // if fetch causes error, re-dispatch that same action with error msg.
                    action.err = error;
                });
            }
        }
    };
}


/*---------------------------- REDUCERS -----------------------------*/
export function reducer(state={}, action={}) {
    if (!action || !action.payload) return state;

    switch (action.type) {
        case (TBL_UI_ADDED)     :
        case (TBL_UI_RESIZE)    :
        case (TBL_UI_GOTO_PAGE) :
        case (TBL_UI_COL_RESIZE):
            const {tbl_ui_id, tbl_ui_gid} = action.payload;
            if (tbl_ui_gid && tbl_ui_id) {
                return update(state, { [tbl_ui_gid] : {$set: {[tbl_ui_id]: action.payload}}});
            }
            break;

        default:
            return state;
    }
}

/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * Notify flux that a TablePanel was added.
 * @param tbl_ui_gid    unique group id of the TablePanel
 * @param tbl_ui_id     unique id of the TablePanel
 * @param tbl_id        tbl_id of the table data being served
 */
export function dispatchUiAdded({tbl_ui_gid, tbl_ui_id, tbl_id, ...rest}) {
    var payload = Object.assign({}, pickBy({tbl_ui_gid, tbl_ui_id, tbl_id, ...rest}));   // take only defined params
    flux.process( {type: TBL_UI_ADDED, payload});
}

