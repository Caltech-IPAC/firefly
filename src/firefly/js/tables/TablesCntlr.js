/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';
import {get, set, omitBy, pickBy, isUndefined, cloneDeep} from 'lodash';

import {flux} from '../Firefly.js';
import * as TblUtil from './TableUtil.js';
import shallowequal from 'shallowequal';
import {dataReducer} from './reducer/TableDataReducer.js';
import {uiReducer} from './reducer/TableUiReducer.js';
import {resultsReducer} from './reducer/TableResultsReducer.js';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {dispatchSetupTblTracking} from '../visualize/TableStatsCntlr.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';

export const TABLE_SPACE_PATH = 'table_space';
export const TABLE_RESULTS_PATH = 'table_space.results.tables';
export const DATA_PREFIX = 'table';
export const RESULTS_PREFIX = 'tableResults';
export const UI_PREFIX = 'tableUi';

/*---------------------------- ACTIONS -----------------------------*/
export const TABLE_SEARCH         = `${DATA_PREFIX}.search`;
export const TABLE_FETCH          = `${DATA_PREFIX}.fetch`;
export const TABLE_FETCH_UPDATE   = `${DATA_PREFIX}.fetchUpdate`;
export const TABLE_NEW            = `${DATA_PREFIX}.new`;
export const TABLE_NEW_LOADED     = `${DATA_PREFIX}.newLoaded`;
export const TABLE_UPDATE         = `${DATA_PREFIX}.update`;
export const TABLE_REPLACE        = `${DATA_PREFIX}.replace`;
export const TABLE_REMOVE         = `${DATA_PREFIX}.remove`;
export const TABLE_SELECT         = `${DATA_PREFIX}.select`;
export const TABLE_HIGHLIGHT      = `${DATA_PREFIX}.highlight`;

export const TBL_RESULTS_ADDED    = `${RESULTS_PREFIX}.added`;
export const TBL_RESULTS_UPDATE   = `${RESULTS_PREFIX}.update`;
export const TBL_RESULTS_ACTIVE   = `${RESULTS_PREFIX}.active`;

export const TBL_UI_UPDATE        = `${UI_PREFIX}.update`;

/*---------------------------- CREATORS ----------------------------*/

export function tableSearch(action) {
    return (dispatch) => {
        //dispatch(validate(FETCH_TABLE, action));
        if (!action.err) {
            var {request, tbl_ui_id=TblUtil.uniqueTblUiId()} = action.payload;
            const {tbl_id} = request;
            
            dispatchSetupTblTracking(tbl_id);
            dispatchTableFetch(request);
            dispatchHideDropDown();
            if (!TblUtil.findTblResultsById(tbl_ui_id)) {
                dispatchTableAdded(tbl_ui_id, tbl_id, get(request, 'META_INFO.title'));
                dispatchAddSaga(doOnTblLoaded, {tbl_id, callback:() => dispatchActiveTableChanged(tbl_id)});
            }
        }
    };
}

export function highlightRow(action) {
    return (dispatch) => {
        const {tbl_id} = action.payload;
        var tableModel = TblUtil.findTblById(tbl_id);
        var tmpModel = TblUtil.smartMerge(tableModel, action.payload);
        const {hlRowIdx, startIdx, endIdx, pageSize} = TblUtil.getTblInfo(tmpModel);
        if (TblUtil.isTblDataAvail(startIdx, endIdx, tableModel)) {
            dispatch(action);
        } else {
            const request = cloneDeep(tableModel.request);
            set(request, 'META_INFO.padResults', true);
            Object.assign(request, {startIdx, pageSize});
            TblUtil.doFetchTable(request, startIdx+hlRowIdx).then ( (tableModel) => {
                dispatch( {type:TABLE_UPDATE, payload: tableModel} );
            }).catch( (error) => {
                dispatch({type: TABLE_UPDATE, payload: {tbl_id: request.tbl_id, error: `Fail to load table. \n   ${error}`}});
            });
        }
    };
}

export function fetchTable(action) {
    return (dispatch) => {
        //dispatch(validate(FETCH_TABLE, action));
        if (!action.err) {
            var {request, hlRowIdx} = action.payload;
            var actionType, {tbl_id} = request;
            if (action.type === TABLE_FETCH_UPDATE) {
                actionType = TABLE_UPDATE;
            } else {
                actionType = TABLE_NEW;
                request.startIdx = 0;
                dispatch({type: TABLE_REPLACE, payload: {tbl_id, isFetching: true}});
                dispatchAddSaga(doOnTblLoaded, {tbl_id, callback:dispatchTableLoaded});
            }

            TblUtil.doFetchTable(request, hlRowIdx).then ( (tableModel) => {
                dispatch( {type: actionType, payload: tableModel} );
            }).catch( (error) => {
                dispatch({type: TABLE_UPDATE, payload: {tbl_id, error: `Fail to load table. \n   ${error}`}});
            });
        }
    };
}


/*---------------------------- REDUCERS -----------------------------*/
export function reducer(state={data:{}, results: {}, ui:{}}, action={}) {
    
    var nstate = {...state};
    nstate.results = resultsReducer(nstate, action);
    nstate.data = dataReducer(nstate, action);
    nstate.ui   = uiReducer(nstate, action);
    
    if (shallowequal(state, nstate)) {
        return state;
    } else {
        return nstate;
    }
}

/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * Initiate a search that returns a table which will be added to result view.
 * @param request
 * @param tbl_ui_id  a unique id used to identify this table widget.  One will be generated if not given.
 */
export function dispatchTableSearch(request, tbl_ui_id) {
    flux.process( {type: TABLE_SEARCH, payload: pickBy({request, tbl_ui_id}) });
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
 * Notify that a new table has been added and it is fully loaded.
 * @param tbl_info table info.  see TableUtil.getTblInfo for details.
 */
export function dispatchTableLoaded(tbl_info) {
    flux.process( {type: TABLE_NEW_LOADED, payload: tbl_info });
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

/**
 * replace the tableModel matching the given tableModel.tbl_id.
 * If one does not exists, it will be added.  This add does not dispatch TABLE_NEW.
 * @param tableModel  the tableModel to replace with
 */
export function dispatchTableReplace(tableModel) {
    flux.process( {type: TABLE_REPLACE, payload: tableModel});
}


/**
 * Add this table into the TableResults
 * @param tbl_ui_id   table ui id
 * @param tbl_id      table id
 * @param title       table title
 * @param removable  true if this table can be removed from view.
 */
export function dispatchTableAdded(tbl_ui_id=TblUtil.uniqueTblUiId(), tbl_id, title, removable=true) {
    title = title || tbl_id;
    flux.process( {type: TBL_RESULTS_ADDED, payload: {tbl_id, title, removable, tbl_ui_id}});
}

/**
 *
 * @param tbl_ui_info
 */
export function dispatchTableUiUpdate(tbl_ui_info) {
    flux.process( {type: TBL_UI_UPDATE, payload: tbl_ui_info});
}

/**
 * 
 * @param tbl_id
 */
export function dispatchActiveTableChanged(tbl_id) {
    flux.process({type: TBL_RESULTS_ACTIVE, payload: {tbl_id}});
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


/**
 * this saga does the following:
 * <ul>
 *     <li>watches to table load
 *     <li>when table is completely loaded, it will execute the given callback with loaded table info
 * </ul>
 * @param tbl_id  table id to watch
 * @param callback  callback to execute when table is loaded.
 */
export function* doOnTblLoaded({tbl_id, callback}) {

    var isLoaded = false, hasData = false;
    while (!(isLoaded && hasData)) {
        const action = yield take([TABLE_NEW, TABLE_UPDATE]);
        const a_id = get(action, 'payload.tbl_id');
        if (tbl_id === a_id) {
            isLoaded = isLoaded || TblUtil.isTableLoaded(action.payload);
            const tableModel = TblUtil.findTblById(tbl_id);
            hasData = hasData || get(tableModel, 'tableData.columns.length');
            if (get(tableModel, 'error')) {
                // there was an error loading this table.
                // exit without callback
                return;
            }
        }
    }
    callback && callback(TblUtil.getTblInfoById(tbl_id));
}
