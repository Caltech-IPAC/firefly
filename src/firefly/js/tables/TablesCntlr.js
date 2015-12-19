/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {fetchUrl, logError} from '../util/WebUtil.js';
import { getRootPath } from '../util/BrowserUtil.js';
import TblUtil from './TableUtil.js';
import LoadTable from './reducers/LoadTable.js';

const TABLE_SPACE_PATH = 'table-space';
const MAIN_SPACE_PATH = `${TABLE_SPACE_PATH}.main`;


/*---------------------------- ACTIONS -----------------------------*/
const FETCH_TABLE           = `${TABLE_SPACE_PATH}.fetchTable`;
const LOAD_TABLE            = `${TABLE_SPACE_PATH}.loadTable`;
const LOAD_TABLE_STATUS     = `${TABLE_SPACE_PATH}.loadTableStatus`;
const LOAD_TABLE_COMPLETE   = `${TABLE_SPACE_PATH}.loadTableComplete`;

const TBL_SELECT_ROW    = `${TABLE_SPACE_PATH}.tblSelectRow`;
const TBL_HIGHLIGHT_ROW = `${TABLE_SPACE_PATH}.tblHighlighRow`;

/*---------------------------- CREATORS ----------------------------*/

function loadTable(action) {
    return validate(LOAD_TABLE, action);
}

function fetchTable(action) {
    return (dispatch) => {
        dispatch(validate(FETCH_TABLE, action));
        if (!action.err) {
            LoadTable.doFetchTable(action.payload).then ( (tableModel) => {
                dispatch( loadTable({type:LOAD_TABLE, payload: tableModel}) );
            }).catch( (error) => {
                logError(error);
                // if fetch causes error, re-dispatch that same action with error msg.
                action.err = error;
                dispatch(action);
            });
        }
    };
}


/*---------------------------- REDUCERS -----------------------------*/
function reducer(state={}, action={}) {
    var newState = Object.assign({}, state);

    switch (action.type) {
        case (TBL_HIGHLIGHT_ROW)  :
        case (LOAD_TABLE_STATUS)  :
        case (LOAD_TABLE_COMPLETE)  :
        case (LOAD_TABLE)  :
            newState = LoadTable.reducer(newState, action);
            break;
        case (FETCH_TABLE)  :
            var tmpAction = {'type' : LOAD_TABLE, 'payload': {'tbl_id' : action.payload.params.tbl_id, 'tableMeta' : {'isLoading' : true}} };
            newState = LoadTable.reducer(newState, tmpAction);
            if (tmpAction.err) {
                TblUtil.error(action, tmpAction.err);
            }
            break;
        case (TBL_SELECT_ROW)  :



        default:
            return state;
    }
    return newState;
}

/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * Load this dataModel into the application state.
 * tableModel may be new, existing, partial, or complete.
 * If tableModel is new, it will add it.  If it existed, it will do an update.
 * Update will always attempt to merge the data, regardless of partial or complete.
 * @param tableModel the dataModel to load.
 */
function dispatchLoadTable(tableModel) {
    flux.process( {type: LOAD_TABLE, payload: {tableModel}});
}

/**
 * Fetch a table from the server.
 * @param tableRequest a TableRequest object.
 */
function dispatchFetchTable(tableRequest) {
    flux.process( {type: FETCH_TABLE, payload: tableRequest });
}

/**
 * set the highlightedRow of the given table by tbl_id.
 * @param tbl_id
 * @param highlightedRow
 */
function dispatchHighlightRow(tbl_id, highlightedRow) {
    flux.process( {type: TBL_HIGHLIGHT_ROW, payload: {tbl_id, highlightedRow} });
}


/*---------------------------- EXPORTS -----------------------------*/
export default {
    TABLE_SPACE_PATH,
    MAIN_SPACE_PATH,
    FETCH_TABLE,
    LOAD_TABLE,
    LOAD_TABLE_STATUS,
    LOAD_TABLE_COMPLETE,
    TBL_SELECT_ROW,
    TBL_HIGHLIGHT_ROW,
    reducer,
    dispatchLoadTable,
    dispatchFetchTable,
    dispatchHighlightRow,
    fetchTable,
    loadTable
};



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

