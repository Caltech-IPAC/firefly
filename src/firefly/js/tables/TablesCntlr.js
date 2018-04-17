/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, set, omitBy, pickBy, pick, isNil, cloneDeep, findKey, isEqual, unset} from 'lodash';

import {flux} from '../Firefly.js';
import * as TblUtil from './TableUtil.js';
import {submitBackgroundSearch} from '../rpc/SearchServicesJson.js';
import shallowequal from 'shallowequal';
import {dataReducer} from './reducer/TableDataReducer.js';
import {uiReducer} from './reducer/TableUiReducer.js';
import {resultsReducer} from './reducer/TableResultsReducer.js';
import {updateMerge, logError} from '../util/WebUtil.js';
import {FilterInfo} from './FilterInfo.js';
import {selectedValues} from '../rpc/SearchServicesJson.js';
import {BG_STATUS, BG_JOB_ADD, dispatchJobAdd} from '../core/background/BackgroundCntlr.js';
import {trackBackgroundJob, isSuccess, isDone, getErrMsg} from '../core/background/BackgroundUtil.js';
import {REINIT_APP} from '../core/AppDataCntlr.js';
import {dispatchComponentStateChange} from '../core/ComponentCntlr.js';


export const TABLE_SPACE_PATH = 'table_space';
export const TABLE_RESULTS_PATH = 'table_space.results.tables';
export const DATA_PREFIX = 'table';
export const RESULTS_PREFIX = 'tableResults';
export const UI_PREFIX = 'tableUi';

/*---------------------------- ACTIONS -----------------------------*/
/**
 * This action does a fetch and then add the results into the UI.
 * Sequence of actions:  TABLE_SEARCH -> TABLE_FETCH -> TBL_RESULTS_ADDED -> TBL_RESULTS_ACTIVE -> TABLE_LOADED
 */
export const TABLE_SEARCH = `${DATA_PREFIX}.search`;

/**
 * Add this tableModel to the table store and the UI.  If tbl_id exists, data will be replaced.
 * Sequence of actions:  TABLE_REPLACE -> TABLE_LOADED, with invokedBy = TABLE_FETCH
 */
export const TABLE_ADD_LOCAL = `${DATA_PREFIX}.addLocal`;

/**
 * Fetch table data.  If tbl_id exists, data will be cleared.
 * Sequence of actions:  TABLE_FETCH -> TABLE_UPDATE(+) -> TABLE_LOADED, with invokedBy = TABLE_FETCH
 */
export const TABLE_FETCH = `${DATA_PREFIX}.fetch`;

/**
 * Used internally; Update table data.
 */
export const TABLE_UPDATE = `${DATA_PREFIX}.update`;

/**
 * Used internally; Repace table data.
 */
export const TABLE_REPLACE = `${DATA_PREFIX}.replace`;

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
 * Sort table data.  Sequence of actions:  TABLE_FETCH -> TABLE_UPDATE(+) -> TABLE_LOADED -> TABLE_SORT
 */
export const TABLE_SORT = `${DATA_PREFIX}.sort`;

/**
 * Filter table data.   Sequence of actions:  TABLE_FETCH -> TABLE_UPDATE(+) -> TABLE_LOADED -> TABLE_FILTER
 */
export const TABLE_FILTER = `${DATA_PREFIX}.filter`;

/**
 * Filter table data on selected rows.   Sequence of actions:  TABLE_FETCH -> TABLE_UPDATE(+) -> TABLE_LOADED -> TABLE_FILTER
 */
export const TABLE_FILTER_SELROW = `${DATA_PREFIX}.filterSelrow`;

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
 * Add the table into the UI given information from the payload.
 */
export const TBL_RESULTS_ADDED = `${RESULTS_PREFIX}.added`;

/**
 * Remove the table UI.  Table model will remain.
 */
export const TBL_RESULTS_REMOVE = `${RESULTS_PREFIX}.remove`;

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



export default {actionCreators, reducers};

function actionCreators() {
    return {
        [TABLE_SEARCH]:     tableSearch,
        [TABLE_ADD_LOCAL]:  tableAddLocal,
        [TABLE_HIGHLIGHT]:  highlightRow,
        [TABLE_SELECT]:     tableSelect,
        [TABLE_FETCH]:      tableFetch,
        [TABLE_SORT]:       tableSort,
        [TABLE_FILTER]:     tableFilter,
        [TABLE_FILTER_SELROW]:  tableFilterSelrow,
        [TBL_RESULTS_ADDED]:    tblResultsAdded,
        [TABLE_REMOVE]:         tblRemove,
        [TBL_RESULTS_REMOVE]:   tblResultRemove
    };
}

function reducers() {
    return {
        [TABLE_SPACE_PATH]: reducer
    };
}


/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * Initiate a search that returns a table which will be added to result view.
 * @param {TableRequest} request
 * @param {TblOptions} options  table options
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchTableSearch(request, options, dispatcher= flux.process) {
    dispatcher( {type: TABLE_SEARCH, payload: pickBy({request, options}) });
}

/**
 * Add this tableModel into the system and then add it to the result view.
 * If one exists, it will be replaced.
 * This operation is used when the tableModel is maintained locally without server's support.
 * @param {TableModel} tableModel  the tableModel to insert
 * @param {TblOptions} options  table options
 * @param {boolean}    [addUI=true]  add this table to the UI
 * @param {function}   dispatcher only for special dispatching uses such as remote
 */
export function dispatchTableAddLocal(tableModel, options, addUI=true, dispatcher= flux.process) {
    dispatcher( {type: TABLE_ADD_LOCAL, payload: {tableModel, options, addUI}});
}

/**
 * Fetch table data from the server.
 * @param request a table request params object.
 * @param hlRowIdx set the highlightedRow.  default to startIdx.
 * @param invokedBy used to indicate what trigger the fetch.
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchTableFetch(request, hlRowIdx, invokedBy, dispatcher= flux.process) {
    dispatcher( {type: TABLE_FETCH, payload: {request, hlRowIdx, invokedBy} });
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
 * Filter the table given the request.
 * @param {TableRequest} request a table request params object.
 * @param {number[]} selected row indices
 * @param {number} hlRowIdx set the highlightedRow.  default to startIdx.
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchTableFilterSelrow(request, selected, hlRowIdx, dispatcher= flux.process) {
    dispatcher( {type: TABLE_FILTER_SELROW, payload: {request, selected, hlRowIdx} });
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
 * @param {string} tbl_ui_id     table ui id
 */
export function dispatchTblResultsAdded(tbl_id, title, options, tbl_ui_id) {
    flux.process( {type: TBL_RESULTS_ADDED, payload: {tbl_id, title, tbl_ui_id, options}});
}

/**
 * Remove table UI from the results area.
 * @param {string} tbl_id     table id
 */
export function dispatchTblResultsRemove(tbl_id) {
    flux.process( {type: TBL_RESULTS_REMOVE, payload: {tbl_id}});
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

/*---------------------------- CREATORS ----------------------------*/

function tableSearch(action) {
    return (dispatch) => {
        //dispatch(validate(FETCH_TABLE, action));
        if (!action.err) {
            dispatch(action);
            var {request={}, options={}} = action.payload;
            const {tbl_ui_id, backgroundable = false} = options;
            const {tbl_id} = request;
            const title = get(request, 'META_INFO.title');
            request.pageSize = options.pageSize = options.pageSize || request.pageSize || 100;
            if (TblUtil.getTblById(tbl_id)) {
                // table exists... this is a new search.  old data should be removed.
                dispatchTableRemove(tbl_id);
            }
            if (backgroundable) {
                request = set(request, 'META_INFO.backgroundable', true);
            }
            dispatchTableFetch(request);
            dispatchTblResultsAdded(tbl_id, title, options, tbl_ui_id);
        }
    };
}

function tableAddLocal(action) {
    return (dispatch) => {
        const {tableModel={}, options={}, addUI=true} = action.payload || {};
        const {tbl_ui_id} = options || {};
        const title = tableModel.title || get(tableModel, 'request.META_INFO.title') || 'untitled';
        const tbl_id = tableModel.tbl_id || get(tableModel, 'request.tbl_id');

        Object.assign(tableModel, {isFetching: false});
        set(tableModel, 'tableMeta.Loading-Status', 'COMPLETED');
        if (!tableModel.origTableModel) tableModel.origTableModel = tableModel;
        if (addUI) dispatchTblResultsAdded(tbl_id, title, options, tbl_ui_id);
        dispatch( {type: TABLE_REPLACE, payload: tableModel} );
        dispatchTableLoaded(Object.assign( TblUtil.getTblInfo(tableModel), {invokedBy: TABLE_FETCH}));
    };
}

function tblResultsAdded(action) {
    return (dispatch) => {
        //dispatch(validate(FETCH_TABLE, action));
        if (!action.err) {
            var {tbl_id, title, options={}, tbl_ui_id} = action.payload;

            title = title || tbl_id;
            options = Object.assign({tbl_group: 'main', removable: true}, options);
            const pageSize = get(options, 'pageSize');
            if ( pageSize && !Number.isInteger(pageSize)) {
                options.pageSize = parseInt(pageSize);
            }
            if (!TblUtil.getTableInGroup(tbl_id, options.tbl_group)) {
                tbl_ui_id = tbl_ui_id || TblUtil.uniqueTblUiId();
                dispatch({type: TBL_RESULTS_ADDED, payload: {tbl_id, title, tbl_ui_id, options}});
            }
            dispatchActiveTableChanged(tbl_id, options.tbl_group);
        }
    };
}

function tblResultRemove(action) {
    return tblRemove(action);   // same logic.. update active table.
}

function tblRemove(action) {
    return (dispatch) => {
        const {tbl_id} = action.payload;
        const tbl_group= TblUtil.findGroupByTblId(tbl_id);
        dispatch({type:action.type, payload:Object.assign({},action.payload, {tbl_group})});
        const results = get(flux.getState(), [TABLE_SPACE_PATH, 'results'], {});
        Object.keys(results).forEach( (tbl_group) => {
            if (get(results, [tbl_group, 'active']) === tbl_id) {
                dispatchActiveTableChanged(findKey(results[tbl_group].tables), tbl_group);
            }
        });
    };
}

function highlightRow(action) {
    return (dispatch) => {
        const {tbl_id, highlightedRow, request={}} = action.payload;
        var tableModel = TblUtil.getTblById(tbl_id);
        if (!tableModel || tableModel.error || highlightedRow < 0 || highlightedRow >= tableModel.totalRows) return;   // out of bound.. ignore.
        if (highlightedRow === tableModel.highlightedRow && !request.pageSize) return;   // nothing to change

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
                dispatch({type: TABLE_UPDATE, payload: TblUtil.createErrorTbl(tbl_id, `Unable to load table. \n   ${error.message}`)});
            });
        }
    };
}

function tableSelect(action) {
    return (dispatch) => {
        const {tbl_id, selectInfo={}} = action.payload;
        const cSelectInfo = get(TblUtil.getTblById(tbl_id), 'selectInfo', {});
        if (!isEqual(selectInfo, cSelectInfo)) {
            dispatch(action);       // only dispatch action if changes are needed.
        }
    };
}

function tableFetch(action) {
    return (dispatch) => {
        var {request, hlRowIdx} = action.payload;
        const {tbl_id} = request;

        dispatch( updateMerge(action, 'payload', {tbl_id}) );

        TblUtil.onTableLoaded(tbl_id).then( (tableModel) => {
            dispatchTableLoaded(Object.assign(TblUtil.getTblInfo(tableModel), {invokedBy: TABLE_FETCH}));
        });

        doTableFetch({tbl_id, request, hlRowIdx, dispatch});
    };
}

function tableSort(action) {
    return (dispatch) => {
        if (!action.err) {
            var {request, hlRowIdx} = action.payload;
            const {tbl_id} = request;
            const tableStub = setupTableOps(tbl_id, request);
            if (!tableStub) return;

            dispatch({type:TABLE_FETCH, payload: tableStub});

            TblUtil.onTableLoaded(tbl_id).then( (tableModel) => {
                dispatchTableLoaded(Object.assign(TblUtil.getTblInfo(tableModel), {invokedBy: TABLE_SORT}));
                dispatch(action);
            });

            doTableFetch({tbl_id, request: tableStub.request, hlRowIdx, dispatch});
        }
    };
}

function setupTableOps(tbl_id, nrequest) {
    const tableModel = TblUtil.getTblById(tbl_id);
    if (!tableModel) return;

    const {request, tableMeta, selectInfo} = tableModel;
    const tableData = pick(tableModel.tableData, 'columns');
    const nreq = Object.assign({}, request, nrequest);
    return {tbl_id, request:nreq, tableMeta, selectInfo, tableData};
}


function tableFilter(action) {
    return (dispatch) => {
        if (!action.err) {
            var {request, hlRowIdx} = action.payload;
            const {tbl_id} = request;
            const tableStub = setupTableOps(tbl_id, request);
            if (!tableStub) return;

            dispatch({type:TABLE_FETCH, payload: tableStub});

            TblUtil.onTableLoaded(tbl_id).then( (tableModel) => {
                dispatchTableLoaded(Object.assign(TblUtil.getTblInfo(tableModel), {invokedBy: TABLE_FILTER}));
                dispatch(action);
            });

            doTableFetch({tbl_id, request: tableStub.request, hlRowIdx,  dispatch});
        }
    };
}

function doTableFetch({request, hlRowIdx, dispatch, tbl_id}) {
    request.startIdx = 0;
    const backgroundable = get(request, 'META_INFO.backgroundable', false);
    if (backgroundable) {
        asyncFetch(request, hlRowIdx, dispatch, tbl_id);
    } else {
        syncFetch(request, hlRowIdx, dispatch, tbl_id);
    }
}


/**
 * This function convert the selected rows into its associated ROW_IDX, add it as a filter, then
 * dispatch it to tableFilter for processing.
 * @param {Action} action
 * @returns {function}
 */
function tableFilterSelrow(action) {
    return () => {
        var {request={}, hlRowIdx, selected=[]} = action.payload || {};
        const {tbl_id, filters} = request;
        const tableModel = TblUtil.getTblById(tbl_id);
        const filterInfoCls = FilterInfo.parse(filters);

        if (tableModel.origTableModel) {
            const selRowIds = selected.map((idx) => TblUtil.getCellValue(tableModel, idx, 'ROW_IDX') || idx).toString();
            filterInfoCls.addFilter('ROW_IDX', `IN (${selRowIds})`);
            request = Object.assign({}, request, {filters: filterInfoCls.serialize()});
            dispatchTableFilter(request, hlRowIdx);
        } else {
            getRowIdFor(request, selected).then( (selectedRowIdAry) => {
                const value = selectedRowIdAry.reduce((rv, val, idx) => {
                        return rv + (idx ? ',':'') + val;
                    }, 'IN (') + ')';
                filterInfoCls.addFilter('ROW_IDX', value);
                request = Object.assign({}, request, {filters: filterInfoCls.serialize()});
                dispatchTableFilter(request, hlRowIdx);
            });
        }
    };
}
/*-----------------------------------------------------------------------------------------*/

function initState() {
    return {data:{}, results: {}, ui:{}};
}

/*---------------------------- REDUCERS -----------------------------*/

function reducer(state=initState(), action={}) {

    if (action.type===REINIT_APP) return initState();

    isDebug() && get(action, 'type','').includes(DATA_PREFIX) && (console.log(action.type + ': ' + get(action, 'payload.tbl_id')));

    const nstate = {...state};
    nstate.results = resultsReducer(nstate, action);
    nstate.data = dataReducer(nstate, action);
    nstate.ui   = uiReducer(nstate, action);

    if (shallowequal(state, nstate)) {
        return state;
    } else {
        return nstate;
    }
}

/*-----------------------------------------------------------------------------------------*/




function getRowIdFor(request, selected) {
    const params = {columnNames: ['ROW_IDX'], request, selectedRows: selected};
    return selectedValues(params).then((tableModel) => {
        return TblUtil.getColumnValues(tableModel, 'ROW_IDX');
    });
}

function syncFetch(request, hlRowIdx, dispatch, tbl_id) {
    unset(request, 'META_INFO.backgroundable');
    TblUtil.doFetchTable(request, hlRowIdx)
        .then( (tableModel) => {
            try {
                dispatch({type:TABLE_UPDATE, payload: tableModel});
            } catch (e) {
                logError(e.stack);
            }
        }).catch((error) => {
            dispatch({type: TABLE_UPDATE, payload: TblUtil.createErrorTbl(tbl_id, error.message)});
        });
}

function asyncFetch(request, hlRowIdx, dispatch, tbl_id) {
    const onComplete = (bgStatus) => {
        const {STATE} = bgStatus || {};
        if (isSuccess(STATE)) {
            syncFetch(request, hlRowIdx, dispatch, tbl_id);
        } else {
            dispatch({type: TABLE_UPDATE, payload: TblUtil.createErrorTbl(tbl_id, getErrMsg(bgStatus))});
        }
    };

    const sentToBg = (bgStatus) => {
        dispatchTblResultsRemove(tbl_id);
        dispatchJobAdd(bgStatus);
    };

    const bgKey = TblUtil.makeBgKey(tbl_id);
    dispatchComponentStateChange(bgKey, {inProgress:true, bgStatus:undefined});
    submitBackgroundSearch(request, request, 1000)
        .then ( (bgStatus) => {
            if (bgStatus) {
                dispatchComponentStateChange(bgKey, {bgStatus});
                if (isDone(bgStatus.STATE)) {
                    onComplete(bgStatus);
                    dispatchComponentStateChange(bgKey, {inProgress:false, bgStatus:undefined});
                } else {
                    // not done; track progress
                    trackBackgroundJob({bgID: bgStatus.ID, key: bgKey, onComplete, sentToBg});
                }
            }
        }).catch( (error) => {
            dispatch({type: TABLE_UPDATE, payload: TblUtil.createErrorTbl(tbl_id, error.message)});
        });
}

const isDebug = () => get(window, 'firefly.debug', false);
