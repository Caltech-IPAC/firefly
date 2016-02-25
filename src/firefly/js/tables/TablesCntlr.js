/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {pickBy} from 'lodash';

import {flux} from '../Firefly.js';
import * as TblUtil from './TableUtil.js';
import * as TablesCntlr from './TablesCntlr.js';
import {Table} from './Table.js';
import {logError} from '../util/WebUtil.js';

export const TABLE_SPACE_PATH = 'table_space';

/*---------------------------- ACTIONS -----------------------------*/
export const TABLE_FETCH          = `${TABLE_SPACE_PATH}.tableFetch`;
export const TABLE_FETCH_UPDATE     = `${TABLE_SPACE_PATH}.tableFetchMore`;
export const TABLE_NEW            = `${TABLE_SPACE_PATH}.tableNew`;
export const TABLE_UPDATE         = `${TABLE_SPACE_PATH}.tableUpdate`;
export const TABLE_LOAD_STATUS    = `${TABLE_SPACE_PATH}.tableLoadStatus`;

export const TABLE_SELECT         = `${TABLE_SPACE_PATH}.tableSelect`;
export const TABLE_HIGHLIGHT      = `${TABLE_SPACE_PATH}.tableHighlight`;

/*---------------------------- CREATORS ----------------------------*/

export function highlightRow(action) {
    return (dispatch) => {
        const {tbl_id} = action.payload;
        var table = Table.findTblById(tbl_id);
        var tmpModel = TblUtil.smartMerge(table.data, action.payload);
        const {hlRowIdx, startIdx, endIdx, pageSize} = TblUtil.gatherTableState(tmpModel);
        if (table && table.has(startIdx, endIdx)) {
            dispatch(action);
        } else {
            const request = Object.assign({}, table.data.request, {startIdx, pageSize});
            TblUtil.doFetchTable(request, startIdx+hlRowIdx).then ( (tableModel) => {
                dispatch( {type:TablesCntlr.TABLE_UPDATE, payload: tableModel} );
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
                if (action.type === TABLE_FETCH_UPDATE) {
                    dispatch( {type:TABLE_UPDATE, payload: tableModel} );
                } else {
                    dispatch( {type:TABLE_NEW, payload: tableModel} );
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
    const {tbl_id} = action.payload || {};
    switch (action.type) {
        case (TABLE_SELECT)  :
        case (TABLE_HIGHLIGHT)  :
        case (TABLE_LOAD_STATUS)  :
        case (TABLE_UPDATE)  :
            return TblUtil.smartMerge(state, {[tbl_id] : action.payload});

        case (TABLE_NEW)  :
            return Object.assign({}, state, {[tbl_id] : action.payload});

        default:
            return state;
    }
}

/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * Fetch a table from the server.
 * @param request a TableRequest params object.
 * @param hlRowIdx set the highlightedRow.  default to startIdx.
 */
export function dispatchTableFetch(request, hlRowIdx) {
    flux.process( {type: TABLE_FETCH, payload: {request, hlRowIdx} });
}

/**
 * set the highlightedRow of the given table by tbl_id.
 * @param tbl_id
 * @param highlightedRow
 * @param request
 */
export function dispatchTableHighlight(tbl_id, highlightedRow, request) {
    flux.process( {type: TABLE_HIGHLIGHT, payload: pickBy({tbl_id, highlightedRow, request}) });
}

/**
 * update the selectInfo of the given table by tbl_id.
 * @param tbl_id
 * @param selectInfo
 */
export function dispatchTableSelect(tbl_id, selectInfo) {
    flux.process( {type: TABLE_SELECT, payload: {tbl_id, selectInfo} });
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

