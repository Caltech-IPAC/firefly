/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import update from 'react-addons-update';
import {pickBy} from 'lodash';

import {flux} from '../Firefly.js';
import * as TblUtil from './TableUtil.js';
import * as TablesCntlr from './TablesCntlr.js';
import {Table} from './Table.js';
import {logError} from '../util/WebUtil.js';

export const TABLE_SPACE_PATH = 'table_space';

/*---------------------------- ACTIONS -----------------------------*/
export const FETCH_TABLE           = `${TABLE_SPACE_PATH}.fetchTable`;
export const REFETCH_TABLE         = `${TABLE_SPACE_PATH}.refetchTable`;
export const REPLACE_TABLE         = `${TABLE_SPACE_PATH}.replaceTable`;
export const LOAD_TABLE            = `${TABLE_SPACE_PATH}.loadTable`;
export const LOAD_TABLE_STATUS     = `${TABLE_SPACE_PATH}.loadTableStatus`;
export const LOAD_TABLE_COMPLETE   = `${TABLE_SPACE_PATH}.loadTableComplete`;

export const TBL_SELECT_ROW    = `${TABLE_SPACE_PATH}.selectRow`;
export const TBL_HIGHLIGHT_ROW = `${TABLE_SPACE_PATH}.highlighRow`;

/*---------------------------- CREATORS ----------------------------*/

export function loadTable(action) {
    return validate(LOAD_TABLE, action);
}

export function highlightRow(action) {
    return (dispatch) => {
        const {tbl_id, request} = action.payload;
        var table = Table.findTblById(tbl_id);
        var tmpModel = TblUtil.smartMerge(table.data, action.payload);
        const {hlRowIdx, startIdx, endIdx, pageSize} = TblUtil.gatherTableState(tmpModel);
        if (table && table.has(startIdx, endIdx)) {
            dispatch(action);
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
    };
}

export function fetchTable(action) {
    return (dispatch) => {
        //dispatch(validate(FETCH_TABLE, action));
        if (!action.err) {
            var {request, hlRowIdx} = action.payload;
            TblUtil.doFetchTable(request, hlRowIdx).then ( (tableModel) => {
                if (action.type === REFETCH_TABLE) {
                    dispatch( {type:REPLACE_TABLE, payload: tableModel} );
                } else {
                    dispatch( loadTable({type:LOAD_TABLE, payload: tableModel}) );
                }
            }).catch( (error) => {
                logError(error);
                // if fetch causes error, re-dispatch that same action with error msg.
                TblUtil.error(error);
                dispatch(action);
            });
        }
    };
}


/*---------------------------- REDUCERS -----------------------------*/
export function reducer(state={}, action={}) {
    const {tbl_id, selectionInfo, highlightedRow} = action.payload || {};
    switch (action.type) {
        case (TBL_SELECT_ROW)  :
            if (selectionInfo) {
                return update(state, { [tbl_id] : {selectionInfo: {$set: selectionInfo}}});
            } else return state;

        case (TBL_HIGHLIGHT_ROW)  :
        case (LOAD_TABLE_STATUS)  :
        case (LOAD_TABLE_COMPLETE)  :
        case (LOAD_TABLE)  :
            return TblUtil.smartMerge(state, {[tbl_id] : action.payload});

        case (REPLACE_TABLE)  :
            return Object.assign({}, state, {[tbl_id] : action.payload});

        default:
            return state;
    }
}

/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * Load this dataModel into the application state.
 * tableModel may be new, existing, partial, or complete.
 * If tableModel is new, it will add it.  If it existed, it will do an update.
 * Update will always attempt to merge the data, regardless of partial or complete.
 * @param tableModel the dataModel to load.
 */
export function dispatchLoadTable(tableModel) {
    flux.process( {type: LOAD_TABLE, payload: {tableModel}});
}

/**
 * Fetch a table from the server.
 * @param request a TableRequest params object.
 * @param hlRowIdx set the highlightedRow.  default to startIdx.
 */
export function dispatchFetchTable(request, hlRowIdx) {
    flux.process( {type: FETCH_TABLE, payload: {request, hlRowIdx} });
}

/**
 * set the highlightedRow of the given table by tbl_id.
 * @param tbl_id
 * @param highlightedRow
 * @param request
 */
export function dispatchHighlightRow(tbl_id, highlightedRow, request) {
    flux.process( {type: TBL_HIGHLIGHT_ROW, payload: pickBy({tbl_id, highlightedRow, request}) });
}

/**
 * update the selectInfo of the given table by tbl_id.
 * @param tbl_id
 * @param selectInfo
 */
export function dispatchRowSelect(tbl_id, selectInfo) {
    flux.process( {type: TBL_SELECT_ROW, payload: {tbl_id, selectInfo} });
}


/*---------------------------- PRIVATE -----------------------------*/

/**
 * validates the action object based on the given type.
 * In case when a validation error occurs, the action's err property will be
 * updated with the error.
 * @param type
 * @param action
 * @returns the given action
 */
function validate(type, action) {
    return TblUtil.doValidate(type, action);
}

