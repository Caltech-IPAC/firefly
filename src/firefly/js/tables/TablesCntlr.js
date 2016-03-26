/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import update from 'react-addons-update';
import {get,omitBy, isUndefined} from 'lodash';

import {flux} from '../Firefly.js';
import * as TblUtil from './TableUtil.js';
import * as TablesCntlr from './TablesCntlr.js';
import {dispatchTableAdded} from './TablesUiCntlr.js';
import {dispatchHideDropDownUi} from '../core/LayoutCntlr.js';
import {dispatchSetupTblTracking} from '../visualize/TableStatsCntlr.js';
import {logError} from '../util/WebUtil.js';

export const TABLE_SPACE_PATH = 'table_space';

/*---------------------------- ACTIONS -----------------------------*/
export const TABLE_SEARCH         = `${TABLE_SPACE_PATH}.tableSearch`;
export const TABLE_FETCH          = `${TABLE_SPACE_PATH}.tableFetch`;
export const TABLE_FETCH_UPDATE   = `${TABLE_SPACE_PATH}.tableFetchUpdate`;
export const TABLE_NEW            = `${TABLE_SPACE_PATH}.tableNew`;
export const TABLE_UPDATE         = `${TABLE_SPACE_PATH}.tableUpdate`;
export const TABLE_REMOVE         = `${TABLE_SPACE_PATH}.tableRemove`;
export const TABLE_LOAD_STATUS    = `${TABLE_SPACE_PATH}.tableLoadStatus`;

export const TABLE_SELECT         = `${TABLE_SPACE_PATH}.tableSelect`;
export const TABLE_HIGHLIGHT      = `${TABLE_SPACE_PATH}.tableHighlight`;

/*---------------------------- CREATORS ----------------------------*/

export function tableSearch(action) {
    return (dispatch) => {
        //dispatch(validate(FETCH_TABLE, action));
        if (!action.err) {
            var {request, resultId, tbl_ui_id} = action.payload;

            dispatchSetupTblTracking(request.tbl_id);
            dispatchTableFetch(request);
            dispatchHideDropDownUi();
            if (!TblUtil.findTblUiById(resultId, tbl_ui_id)) {
                dispatchTableAdded(resultId, tbl_ui_id, request.tbl_id);
            }
        }
    };
}

export function highlightRow(action) {
    return (dispatch) => {
        const {tbl_id} = action.payload;
        var tableModel = TblUtil.findTblById(tbl_id);
        var tmpModel = TblUtil.smartMerge(tableModel, action.payload);
        const {hlRowIdx, startIdx, endIdx, pageSize} = TblUtil.gatherTableState(tmpModel);
        if (TblUtil.isTblDataAvail(startIdx, endIdx, tableModel)) {
            dispatch(action);
        } else {
            const request = Object.assign({}, tableModel.request, {startIdx, pageSize});
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
    const {tbl_id, selectInfo} = action.payload || {};
    switch (action.type) {
        case (TABLE_SELECT)  :
            if (selectInfo) {
                return update(state, { [tbl_id] : {selectInfo: {$set: selectInfo}}});
            } else return state;

        case (TABLE_HIGHLIGHT)  :
        case (TABLE_LOAD_STATUS)  :
        case (TABLE_UPDATE)  :
            return TblUtil.smartMerge(state, {[tbl_id] : action.payload});

        case (TABLE_NEW)  :
            return Object.assign({}, state, {[tbl_id] : action.payload});
        case (TABLE_REMOVE)  :
            const nstate = Object.assign({}, state);
            Reflect.deleteProperty(nstate, [tbl_id]);
            return nstate;

        default:
            return state;
    }
}

/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * Initiate a search that returns a table which will be added to a UI
 * view designated by the given resultId.
 * @param request
 * @param resultId
 * @param tbl_ui_id
 */
export function dispatchTableSearch(request, resultId, tbl_ui_id) {
    flux.process( {type: TABLE_SEARCH, payload: {request, resultId, tbl_ui_id} });
}

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
    flux.process( {type: TABLE_HIGHLIGHT, payload: omitBy({tbl_id, highlightedRow, request}, isUndefined) });
}

/**
 * update the selectInfo of the given table by tbl_id.
 * @param tbl_id
 * @param selectInfo
 */
export function dispatchTableSelect(tbl_id, selectInfo) {
    flux.process( {type: TABLE_SELECT, payload: {tbl_id, selectInfo} });
}

/**
 * remove the table's data given its id.
 * @param tbl_id  unique table identifier.
 */
export function dispatchTableRemove(tbl_id) {
    flux.process( {type: TABLE_REMOVE, payload: {tbl_id}});
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

