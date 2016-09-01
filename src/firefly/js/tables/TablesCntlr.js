/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';
import {get, set, omitBy, pickBy, isNil, cloneDeep} from 'lodash';

import {flux} from '../Firefly.js';
import * as TblUtil from './TableUtil.js';
import shallowequal from 'shallowequal';
import {dataReducer} from './reducer/TableDataReducer.js';
import {uiReducer} from './reducer/TableUiReducer.js';
import {resultsReducer} from './reducer/TableResultsReducer.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {updateMerge} from '../util/WebUtil.js';

export const TABLE_SPACE_PATH = 'table_space';
export const TABLE_RESULTS_PATH = 'table_space.results.tables';
export const DATA_PREFIX = 'table';
export const RESULTS_PREFIX = 'tableResults';
export const UI_PREFIX = 'tableUi';

/*---------------------------- ACTIONS -----------------------------*/
/**
 * Fetch table data.  If tbl_id exists, data will be cleared.
 * Sequence of actions:  TABLE_FETCH -> TABLE_UPDATE(+) -> TABLE_LOADED, with invokedBy = TABLE_FETCH
 */
export const TABLE_FETCH = `${DATA_PREFIX}.fetch`;

/**
 * Fired when table is completely loaded on the server.
 * Payload contains getTblInfo() + invokedBy.
 * invokedBy is the action that invoke this table load. ie, fetch, sort, filter, etc.
 */
export const TABLE_LOADED = `${DATA_PREFIX}.loaded`;

/**
 * Removes table data and all UI elements associated with the data.
 */
export const TABLE_REMOVE = `${DATA_PREFIX}.remove`;

/**
 * Sort table data.  Sequence of actions:  TABLE_SORT -> TABLE_UPDATE(+) -> TABLE_LOADED, with invokedBy = TABLE_SORT
 */
export const TABLE_SORT = `${DATA_PREFIX}.sort`;

/**
 * Filter table data.   Sequence of actions:  TABLE_FILTER -> TABLE_UPDATE(+) -> TABLE_LOADED, with invokedBy = TABLE_FILTER
 */
export const TABLE_FILTER = `${DATA_PREFIX}.filter`;

/**
 * Fired when table selection changes.
 */
export const TABLE_SELECT = `${DATA_PREFIX}.select`;

/**
 * Fired when highlighted row changes.
 * @type {string}
 */
export const TABLE_HIGHLIGHT = `${DATA_PREFIX}.highlight`;

/**
 * This action does a fetch and then add the results into the UI.
 * Sequence of actions:  TABLE_FETCH -> TABLE_UPDATE -> TABLE_LOADED -> TBL_RESULTS_ADDED -> TBL_RESULTS_ACTIVE
 */
export const TABLE_SEARCH = `${DATA_PREFIX}.search`;

/**
 * Add the table into the UI given information from the payload.
 */
export const TBL_RESULTS_ADDED = `${RESULTS_PREFIX}.added`;

/**
 * Add the table into the UI given information from the payload.
 */
export const TBL_RESULTS_UPDATE = `${RESULTS_PREFIX}.update`;

/**
 * Active table changes.  There is only one active table per table group.
 */
export const TBL_RESULTS_ACTIVE = `${RESULTS_PREFIX}.active`;


/**
 * Fired when UI information have changed.  Used mainly by UI elements.
 */
export const TBL_UI_UPDATE = `${UI_PREFIX}.update`;

/**
 * Fired when table expanded/collapsed state changes.
 */
export const TBL_UI_EXPANDED = `${UI_PREFIX}.expanded`;

/**
 * Fired when partial table data is updated.  Used internally to maintain state.
 */
export const TABLE_UPDATE = `${DATA_PREFIX}.update`;

/**
 * Fired when full table data is updated.  Used internally to maintain state.
 */
export const TABLE_REPLACE = `${DATA_PREFIX}.replace`;


/**
 * Top level store for table related data.  It's mounted as 'table_space' under the application state
 * @typedef {object} TableSpace
 * @prop {Object.<string, TableModel>}  data    repository for table model; keyed by tbl_id
 * @prop {Object.<string, TableGroup>}  results repository for table group information; keyed by tbl_group name
 * @prop {Object.<string, Object>}      ui      repository for table UI state; keyed by tbl_ui_id
 */


/**
 * Table model.  The top level table data object with meta info.
 * @typedef {object} TableModel
 * @prop {string}   tbl_id    unique ID of this table.
 * @prop {string}   title     title, used on label.
 * @prop {TableRequest} request  the request used to create this table
 * @prop {TableMeta} tableMeta   table's meta information stored as key/value pair.
 * @prop {TableData} tableData  table's meta information stored as key/value pair.
 * @prop {number}   totalRows   total number of rows.
 * @prop {number}   highlightedRow  the current highlighted row index.  index is natural order starting from 0.
 * @prop {object}   selectInfo  selection information.  use SelectInfo.newInstance take advantage of helper's functions.
 * @prop {boolean}  isFetching  true if data is being fetched and not ready for display.
 * @prop {string}   error       error message if the request fail to create a table.
 */

/**
 * Table data.  Table data object.
 * @typedef {object} TableData
 * @prop {TableColumn[]} columns    table column definition.
 * @prop {string[][]} data          2D array containing the table data
 */

/**
 * Table column information.
 * @typedef {object} TableColumn
 * @prop {string} name      name of the column
 * @prop {string} label     display name of the column
 * @prop {string} type      data type
 * @prop {string} units     data units
 * @prop {string} desc      description of the column
 * @prop {number} width     column display width
 * @prop {number} prefWidth preferred width.  if width is not defined
 * @prop {boolean} sortable true if undefined
 * @prop {string} visibility    show, hide, or hidden.  hidden columns are not viewable by users.
 * @prop {string} sortByCols    for multi-columns sorting.  column names separated by comma(',').
 * @prop {string} related       highlight related rows based on this column's value.
 */

/**
 * Table meta information.  Below is only a small set of predefined meta used by table.
 * The meta information in this object are used by many components for many reasons.  ie catalog overlay.
 * @typedef {object} TableMeta
 * @prop {string} Loading-Status COMPLETED or INPROGRESS
 * @prop {string} tblFilePath   path of the source of this table on the server-side.
 * @prop {string} isFullyLoaded 'true' when table is completely loaded on the server-side.
 * @prop {string} source    path of the original table source before any operations were performed. ie sort, filter, etc.  this may not be fully supported.
 */

/**
 * Table request.  Below is a list of predefined parameters available for table request.  All of the options are optional.  
 * These parameters let you control what data and how it will be returned.
 * @typedef {object} TableRequest
 * @prop {number} startIdx  the starting index to fetch.  defaults to zero.
 * @prop {number} pageSize  the number of rows per page.  defaults to 100.
 * @prop {string} filters   list of conditions separted by comma(,). Format:  (col_name|index) operator value.
 *                  operator is one of '> < = ! >= <= IN'.  See DataGroupQueryStatement.java doc for more details.
 * @prop {string} sortInfo  sort information.  Format:  (ASC|DESC),col_name[,col_name]*
 * @prop {string} inclCols  list of columns to select.  Column names separted by comma(,)
 * @prop {string} decimate  decimation information.
 * @prop {object} META_INFO meta information passed as key/value pair to server then returned as tableMeta.
 * @prop {string} use       one of 'catalog_overlay', 'catalog_primary', 'data_primary'.
 * @prop {string} tbl_id    a unique id of a table. auto-create if not given.
 */

/**
 * Table group.  Define a group of tables used by the UI.
 * @typedef {Object} TableGroup
 * @prop {string}   name     unique name of this group
 * @prop {string}   active   tbl_id of the active table in this group
 * @prop {Object.<string, TableGroupItem>}   tables     a map of TableGroupItem(s) keyed by tbl_id
 */

/**
 * Table group item.  Contains enough key information to identify the table data as well as the UI data associate with this item.
 * @typedef {Object} TableGroupItem
 * @prop {string}   tbl_group  table group name
 * @prop {string}   tbl_id     unique id of the table data
 * @prop {string}   tbl_ui_id  unique id of the table's UI data
 * @prop {string}   title      title or label of the table
 * @prop {boolean}  removable  true if this item can be removed from group.
 * @prop {Object.<string, *>}   options   table options, ie.  selectable, expandable
 */


/*---------------------------- CREATORS ----------------------------*/

export function tableSearch(action) {
    return (dispatch) => {
        //dispatch(validate(FETCH_TABLE, action));
        if (!action.err) {
            var {request={}, options={}, tbl_group} = action.payload;
            const {tbl_id} = request;
            const title = get(request, 'META_INFO.title');
            request.pageSize = options.pageSize = options.pageSize || request.pageSize || 100;

            dispatchTableFetch(request);
            if (!TblUtil.getTableInGroup(tbl_id, tbl_group)) {
                const {tbl_group, removable} = options || {};
                dispatchTblResultsAdded(tbl_id, title, options, removable, tbl_group);
                dispatchAddSaga(doOnTblLoaded, {tbl_id, callback:() => dispatchActiveTableChanged(tbl_id, tbl_group)});
            }
        }
    };
}

export function highlightRow(action) {
    return (dispatch) => {
        const {tbl_id} = action.payload;
        var tableModel = TblUtil.getTblById(tbl_id);
        var tmpModel = TblUtil.smartMerge(tableModel, action.payload);
        const {hlRowIdx, startIdx, endIdx, pageSize} = TblUtil.getTblInfo(tmpModel);
        if (TblUtil.isTblDataAvail(startIdx, endIdx, tableModel)) {
            dispatch(action);
        } else {
            const request = cloneDeep(tableModel.request);
            set(request, 'META_INFO.padResults', true);
            Object.assign(request, {startIdx, pageSize});
            TblUtil.doFetchTable(request, startIdx+hlRowIdx).then ( (tableModel) => {
                dispatch( {type:TABLE_HIGHLIGHT, payload: tableModel} );
            }).catch( (error) => {
                dispatch({type: TABLE_HIGHLIGHT, payload: createErrorTbl(tbl_id, `Fail to load table. \n   ${error}`)});
            });
        }
    };
}

export function tableFetch(action) {
    return (dispatch) => {
        if (!action.err) {
            var {request, hlRowIdx} = action.payload;
            const {tbl_id} = request;

            dispatchAddSaga( doOnTblLoaded, {tbl_id, callback:() => dispatchTableLoaded( Object.assign(TblUtil.getTblInfoById(tbl_id), {invokedBy: action.type}) )});
            dispatch( updateMerge(action, 'payload', {tbl_id, isFetching: true}) );
            request.startIdx = 0;
            TblUtil.doFetchTable(request, hlRowIdx).then ( (tableModel) => {
                dispatch( {type: TABLE_UPDATE, payload: tableModel} );
            }).catch( (error) => {
                dispatch({type: TABLE_UPDATE, payload: createErrorTbl(tbl_id, `Fail to load table. \n   ${error}`)});
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
 * @param {TblOptions} options  table options
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchTableSearch(request, options, dispatcher= flux.process) {
    dispatcher( {type: TABLE_SEARCH, payload: pickBy({request, options}) });
}

/**
 * Fetch table data from the server.
 * @param request a table request params object.
 * @param hlRowIdx set the highlightedRow.  default to startIdx.
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchTableFetch(request, hlRowIdx, dispatcher= flux.process) {
    dispatcher( {type: TABLE_FETCH, payload: {request, hlRowIdx} });
}

/**
 * Sort the table given the request.
 * @param request a table request params object.
 * @param hlRowIdx set the highlightedRow.  default to startIdx.
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchTableSort(request, hlRowIdx, dispatcher= flux.process) {
    dispatcher( {type: TABLE_SORT, payload: {request, hlRowIdx} });
}

/**
 * Filter the table given the request.
 * @param request a table request params object.
 * @param hlRowIdx set the highlightedRow.  default to startIdx.
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchTableFilter(request, hlRowIdx, dispatcher= flux.process) {
    dispatcher( {type: TABLE_FILTER, payload: {request, hlRowIdx} });
}

/**
 * Notify that a new table has been added and it is fully loaded.
 * @param tbl_info table info.  see TableUtil.getTblInfo for details.
 */
export function dispatchTableLoaded(tbl_info) {
    flux.process( {type: TABLE_LOADED, payload: tbl_info });
}

/**
 * set the highlightedRow of the given table by tbl_id.
 * @param tbl_id
 * @param highlightedRow
 * @param request
 */
export function dispatchTableHighlight(tbl_id, highlightedRow, request) {
    flux.process( {type: TABLE_HIGHLIGHT, payload: omitBy({tbl_id, highlightedRow, request}, isNil) });
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
 * If one does not exists, it will be added.  This add does not dispatch TABLE_FETCH.
 * @param tableModel  the tableModel to replace with
 */
export function dispatchTableReplace(tableModel) {
    flux.process( {type: TABLE_REPLACE, payload: tableModel});
}

/**
 * Add this table UI into the results area.
 * @param {string} tbl_id        table id
 * @param {string} title         title to be displayed with the table.
 * @param {TblOptions} options   table options.
 * @param {boolean} removable    true if this table can be removed from view.
 * @param {string} tbl_group     table group.  defaults to 'main'
 * @param {string} tbl_ui_id     table ui id
 */
export function dispatchTblResultsAdded(tbl_id, title, options, removable, tbl_group, tbl_ui_id=TblUtil.uniqueTblUiId()) {
    title = title || tbl_id;
    tbl_group = tbl_group || 'main';
    removable = isNil(removable) ? true : removable;
    flux.process( {type: TBL_RESULTS_ADDED, payload: {tbl_id, tbl_group, title, removable, tbl_ui_id, options}});
}

/**
 * request to have table in expanded mode.
 * @param tbl_ui_id
 * @param tbl_id
 */
export function dispatchTblExpanded(tbl_ui_id, tbl_id) {
    flux.process( {type: TBL_UI_EXPANDED, payload: {tbl_ui_id, tbl_id}});
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
 * @param tbl_group
 */
export function dispatchActiveTableChanged(tbl_id, tbl_group='main') {
    flux.process({type: TBL_RESULTS_ACTIVE, payload: {tbl_id, tbl_group}});
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
        const action = yield take([TABLE_UPDATE]);
        const a_id = get(action, 'payload.tbl_id');
        if (tbl_id === a_id) {
            isLoaded = isLoaded || TblUtil.isTableLoaded(action.payload);
            const tableModel = TblUtil.getTblById(tbl_id);
            hasData = hasData || get(tableModel, 'tableData.columns.length');
            if (get(tableModel, 'error')) {
                // there was an error loading this table.
                callback(createErrorTbl(tbl_id, tableModel.error));
                return;
            }
        }
    }
    callback && callback(TblUtil.getTblInfoById(tbl_id));
}

function createErrorTbl(tbl_id, error) {
    return {tbl_id, error};
}