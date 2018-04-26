/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take, fork, cancel} from 'redux-saga/effects';
import {get, set, has, isEmpty, isUndefined, uniqueId, cloneDeep, omitBy, isNil, isPlainObject, isArray, padEnd} from 'lodash';
import Enum from 'enum';

import {makeFileRequest, MAX_ROW} from './TableRequestUtil.js';
import * as TblCntlr from './TablesCntlr.js';
import {SortInfo, SORT_ASC, UNSORTED} from './SortInfo.js';
import {FilterInfo} from './FilterInfo.js';
import {SelectInfo} from './SelectInfo.js';
import {flux} from '../Firefly.js';
import {encodeServerUrl, uniqueID} from '../util/WebUtil.js';
import {fetchTable, queryTable, selectedValues} from '../rpc/SearchServicesJson.js';
import {DEF_BASE_URL} from '../core/JsonUtils.js';
import {ServerParams} from '../data/ServerParams.js';
import {doUpload} from '../ui/FileUpload.jsx';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../core/MasterSaga.js';
import {getWsConnId} from '../core/messaging/WebSocketClient.js';

export const COL_TYPE = new Enum(['ALL', 'NUMBER', 'TEXT']);
const char_types = ['char', 'c', 's', 'str'];
const num_types = ['double', 'd', 'long', 'l', 'int', 'i', 'float', 'f'];


/**
 * tableRequest will be sent to the server as a json string.
 * @param {TableRequest} tableRequest is a table request params object
 * @param {number} [hlRowIdx] set the highlightedRow.  default to startIdx.
 * @returns {Promise.<TableModel>}
 * @public
 * @func doFetchTable
 * @memberof firefly.util.table
 */
export function doFetchTable(tableRequest, hlRowIdx) {
    const {tbl_id} = tableRequest;
    const tableModel = getTblById(tbl_id) || {};
    if (tableModel.origTableModel) {
        return Promise.resolve(processRequest(tableModel.origTableModel, tableRequest, hlRowIdx));
    } else {
        return fetchTable(tableRequest, hlRowIdx);
    }
}


/**
 * return a promise of a tableModel for the given tbl_id.
 * @param {string} tbl_id the table ID to watch for.
 * @returns {Promise.<TableModel>}
 * @public
 * @func onTableLoad
 * @memberof firefly.util.table
 */
export function onTableLoaded(tbl_id) {
    if (isFullyLoaded(tbl_id)) {
        return Promise.resolve(getTblById(tbl_id));
    } else {
        return new Promise((resolve) => {
            dispatchAddActionWatcher({
                actions:[TblCntlr.TABLE_UPDATE, TblCntlr.TABLE_REPLACE],
                callback: doOnTblLoaded,
                params: {tbl_id, resolve}
            });
        });
    }
}

/**
 * returns true is there is data within the given range.  this is needed because
 * of paging table not loading the full table.
 * @param {number} startIdx
 * @param {number} endIdx
 * @param {TableModel} tableModel
 * @returns {boolean}
 * @func isTblDataAvail
 * @memberof firefly.util.table
 */
export function isTblDataAvail(startIdx, endIdx, tableModel) {
    if (!tableModel) return false;
    endIdx =  endIdx >0 ? Math.min( endIdx, tableModel.totalRows) : startIdx;
    if (startIdx >=0 && endIdx > startIdx) {
        const data = get(tableModel, 'tableData.data', []);
        const dataCount = Object.keys(data.slice(startIdx, endIdx)).length;
        return dataCount === (endIdx-startIdx);
    } else return false;
}

/**
 * returns the table model with the given tbl_id
 * @param tbl_id
 * @returns {TableModel}
 * @public
 * @func getTblById
 * @memberof firefly.util.table
 */
export function getTblById(tbl_id) {
    return get(flux.getState(),[TblCntlr.TABLE_SPACE_PATH, 'data', tbl_id]);
}

/**
 * returns all table group IDs
 * @returns {string[]}
 * @memberof firefly.util.table
 * @func getAllTableGroupIds
 */
export function getAllTableGroupIds() {
    const groups = get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'results']) || {};
    return Object.keys(groups);
}

/**
 * returns the table group information
 * @param {string} tbl_group    the group name to look for
 * @returns {TableGroup}
 * @public
 * @memberof firefly.util.table
 * @func getTableGroup
 */
export function getTableGroup(tbl_group='main') {
    return get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'results', tbl_group]);
}

/**
 * returns the table group name given a tbl_id.  it will return undefined if
 * the given tbl_id is not in a group.
 * @param {string} tbl_id    table id
 * @returns {TableGroup}
 * @public
 * @memberof firefly.util.table
 * @func findGroupByTblId
 */
export function findGroupByTblId(tbl_id) {
    const resultsRoot = get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'results'], {});
    const groupName = Object.keys(resultsRoot).find( (tbl_grp_id) => {
        return has(resultsRoot, [tbl_grp_id, 'tables', tbl_id]);
    });
    return groupName;
}

export function removeTablesFromGroup(tbl_group_id = 'main') {
    const tblAry = getTblIdsByGroup(tbl_group_id);
    tblAry && tblAry.forEach((tbl_id) => {
        TblCntlr.dispatchTableRemove(tbl_id);
    });
}

/**
 * returns an array of tbl_id for the given tbl_group_id
 * @param {string} tbl_group_id    table group name.  defaults to 'main' if not given
 * @returns {String[]} array of tbl_id
 * @public
 * @func getTblIdsByGroup
 * @memberof firefly.util.table
 */
export function getTblIdsByGroup(tbl_group_id = 'main') {
    const tableGroup = get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'results', tbl_group_id]);
    return Object.keys(get(tableGroup, 'tables', {}));
}

/**
 * returns the table information for the given id and group.
 * @param {string} tbl_id       table id.
 * @param {string} tbl_group    table group name.  defaults to 'main' if not given
 * @returns {TableModel}
 * @public
 * @func getTableInGroup
 * @memberof firefly/util/table
 */
export function getTableInGroup(tbl_id, tbl_group='main') {
    return get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'results', tbl_group, 'tables',  tbl_id]);
}

/**
 * get the table working state by tbl_ui_id
 * @param {string} tbl_ui_id     table UI id.
 * @returns {Object}
 * @memberof firefly.util.table
 * @func  getTableUiById
 */
export function getTableUiById(tbl_ui_id) {
    return get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'ui', tbl_ui_id]);
}

/**
 * returns the first table working state for the given tbl_id
 * @param {string} tbl_id
 * @returns {Object}
 * @public
 * @func getTableUiByTblId
 * @memberof firefly.util.table
 */
export function getTableUiByTblId(tbl_id) {
    const uiRoot = get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'ui'], {});
    return Object.keys(uiRoot).find( (ui_id) => get(uiRoot, [ui_id, 'tbl_id']) === tbl_id);
}

/**
 * returns the working state of the currently expanded table.
 * @returns {Object}
 * @memberof firefly.util.table
 * @func getTblExpandedInfo
 */
export function getTblExpandedInfo() {
    return get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH, 'ui', 'expanded'], {});
}

/**
 * returns true if the table referenced by the given tbl_id is fully loaded.
 * @param {string} tbl_id
 * @returns {boolean}
 * @memberof firefly.util.table
 * @func  isFullyLoaded
 */
export function isFullyLoaded(tbl_id) {
    return isTableLoaded(getTblById(tbl_id));
}

/**
 * return the resultSetID of this table.
 * This is a unique ID used to identify the resultset on the server used to populate this table.
 * @see TableRequestUtil.setResultSetID for the reverse
 * @param {string} tbl_id
 * @returns {string}
 */
export function getResultSetID(tbl_id) {
    return get(getTblById(tbl_id), 'tableMeta.resultSetID', '');
}

/**
 * return the resultSetRequest of this table.
 * This is a JSON string of the request used to created this table
 * @see TableRequestUtil.setResultSetRequest for the reverse
 * @param {string} tbl_id
 * @returns {string}
 */
export function getResultSetRequest(tbl_id) {
    return get(getTblById(tbl_id), 'tableMeta.resultSetRequest', '');
}

/**
 * Returns the first index of the found row.  It will search the table on the client first.
 * If none is found and the table is partially loaded, it will search the server-side as well.
 * @param {string} tbl_id the tbl_id of the table to search on.
 * @param {string} filterInfo filter info string used to find the first row that matches it.
 * @returns {Promise.<number>} Returns the index of the found row, else -1.
 * @func findIndex
 * @memberof firefly.util.table
 */
export function findIndex(tbl_id, filterInfo) {

    const tableModel = getTblById(tbl_id);
    if (!tableModel) return Promise.resolve(-1);
    const comparators = filterInfo.split(';').map((s) => s.trim()).map((s) => FilterInfo.createComparator(s, tableModel));
    const idx = get(tableModel, 'tableData.data', []).findIndex((row, idx) => comparators.reduce( (rval, matcher) => rval && matcher(row, idx), true));
    if (idx >= 0) {
        return Promise.resolve(idx);
    } else {
        const inclCols = 'ROW_NUM';
        return queryTable(tableModel.request, {filterInfo, inclCols}).then( (tableModel) => {
            return get(getColumnValues(tableModel, inclCols), '0', -1);
        });
    }
}


/**
 * Returns the column index with the given name; otherwise, -1.
 * @param {TableModel} tableModel
 * @param {string} colName
 * @returns {number}
 * @public
 * @func getColumnIdx
 * @memberof firefly.util.table
 */
export function getColumnIdx(tableModel, colName) {
    const cols = get(tableModel, 'tableData.columns', []);
    return cols.findIndex((col) => {
        return col.name === colName;
    });
}

/**
 * Returns the column data type with the given name
 * @param {TableModel} tableModel
 * @param {string} colName
 * @returns {string}
 * @public
 */
export function getColumnType(tableModel, colName) {
    const cols = get(tableModel, 'tableData.columns', []);

    return get(cols.find((col)=> col.name === colName), 'type', '');
}


/**
 * returns column information for the given name.
 * @param {TableModel} tableModel
 * @param {string} colName
 * @returns {TableColumn}
 * @public
 * @func getColumn
 * @memberof firefly.util.table
 */
export function getColumn(tableModel, colName) {
    const colIdx = getColumnIdx(tableModel, colName);
    if (colIdx >= 0) {
        return get(tableModel, `tableData.columns.${colIdx}`);
    }
}

export function getFilterCount(tableModel) {
    const filterInfo = get(tableModel, 'request.filters');
    const filterCount = filterInfo ? filterInfo.split(';').length : 0;
    return filterCount;
}

export function clearFilters(tableModel) {
    const {request, tbl_id} = tableModel || {};
    if (request && request.filters) {
        TblCntlr.dispatchTableFilter({tbl_id, filters: ''}, 0);
    }
}

export function clearSelection(tableModel) {
    if (tableModel) {
        const selectInfoCls = SelectInfo.newInstance({rowCount: tableModel.totalRows});
        TblCntlr.dispatchTableSelect(tableModel.tbl_id, selectInfoCls.data);
    }
}

/**
 * return the tbl_id of the active table for the given group.
 * @param {string} tbl_group group name; defaults to 'main' if not given.
 * @returns {string}
 * @public
 * @memberof firefly.util.table
 * @func getActiveTableId
 */
export function getActiveTableId(tbl_group='main') {
    return get(flux.getState(), [TblCntlr.TABLE_SPACE_PATH,'results',tbl_group,'active']);
}

/**
 *
 * @param {TableModel} tableModel
 * @param {number} rowIdx
 * @param {string} colName
 * @return {string}
 * @public
 * @func getCellValue
 * @memberof firefly.util.table
 */
export function getCellValue(tableModel, rowIdx, colName) {
    if (get(tableModel, 'tableData.data.length', 0) > 0) {
        const colIdx = getColumnIdx(tableModel, colName);
        return get(tableModel, ['tableData', 'data', rowIdx, colIdx]);
    }
}

/**
 * returns an array of all the values for a column
 * @param {TableModel} tableModel
 * @param {string} colName
 * @return {Object[]}
 * @func getColumnValues
 * @public
 * @memberof firefly.util.table
 */
export function getColumnValues(tableModel, colName) {
    const colIdx = getColumnIdx(tableModel, colName);
    if (colIdx >= 0 && colIdx < get(tableModel, 'tableData.data.length', 0)) {
        return get(tableModel, 'tableData.data').map( (r) => r[colIdx]);
    } else {
        return [];
    }
}

/**
 * returns an array of all the values for a row
 * @param {TableModel} tableModel
 * @param {number} rowIdx
 * @return {Object[]}
 * @public
 * @memberof firefly.util.table
 * @func getRowValues
 */
export function getRowValues(tableModel, rowIdx) {
    return get(tableModel, ['tableData', 'data', rowIdx], []);
}

/**
 * returns an array of all the values for a columns
 * @param {string} tbl_id
 * @param {string[]} columnNames  defaults to all columns
 * @return {Promise.<TableModel>}
 * @func getSelectedData
 * @public
 * @memberof firefly.util.table
 */
export function getSelectedData(tbl_id, columnNames=[]) {
    const {tableModel, tableMeta, totalRows, selectInfo, request} = getTblInfoById(tbl_id);
    const selectedRows = [...SelectInfo.newInstance(selectInfo).getSelected()];  // get selected row idx as an array
    if (columnNames.length === 0) {
        columnNames = getColumns(tableModel).map( (c) => c.name);       // return all columns
    }

    if (selectedRows.length === 0 || isTblDataAvail(0, totalRows -1, tableModel)) {
        const meta = cloneDeep(tableMeta);
        const columns = tableModel.tableData.columns
                            .filter((c) => columnNames.includes(c.name))
                            .map( (c) => cloneDeep(c));

        const data = selectedRows.sort()
                            .map( (rIdx) => columnNames.reduce( (rval, c) => {
                                    rval.push(getCellValue(tableModel, rIdx, c));
                                    return rval;
                                }, []));
        return Promise.resolve({tableMeta: meta, totalRows: data.length, tableData: {columns, data}});
    } else {
        return selectedValues({columnNames, request, selectedRows});
    }
}

/**
 * return true if the given table is fully loaded.
 * @param {TableModel} tableModel
 * @returns {boolean}
 * @memberof ffirefly.util.table
 * @func isTableLoaded
 */
export function isTableLoaded(tableModel) {
    const status = tableModel && !tableModel.isFetching && get(tableModel, 'tableMeta.Loading-Status', 'COMPLETED');
    return status === 'COMPLETED';
}

/**
 * This function merges the source object into the target object
 * by traversing and comparing every like path.  If a value was
 * merged at any data node in the data graph, the node and all of its
 * parent nodes will be shallow cloned and returned.  Otherwise, the target's value
 * will be returned.
 * @param {Object} target
 * @param {Object} source
 * @returns {Object}
 * @public
 * @memberof firefly.util.table
 * @func smartMerge
 */
export function smartMerge(target, source) {
    if (!target) return source;

    if (isPlainObject(source) && isPlainObject(target)) {
        const objChanges = {};
        Object.keys(source).forEach((k) => {
            const nval = smartMerge(target[k], source[k]);
            if (nval !== target[k]) {
                objChanges[k] = nval;
            }
        });
        return (isEmpty(objChanges)) ? target : Object.assign({}, target, objChanges);
    } else if (isArray(source) && isArray(target)){
        const aryChanges = [];
        source.forEach((v, idx) => {
            const nval = smartMerge(target[idx], source[idx]);
            if (nval !== target[idx]) {
                aryChanges[idx] = nval;
            }
        });
        if (isEmpty(aryChanges)) return target;
        else {
            const nAry = target.slice();
            aryChanges.forEach((v, idx) => nAry[idx] = v);
            return nAry;
        }
    } else {
        return (target === source) ? target : source;
    }
}

/**
 * sort table data in-place.
 * @param {TableData} tableData
 * @param {TableColumn[]} columns
 * @param {string} sortInfoStr
 * @returns {TableData}
 * @public
 * @memberof firefly.util.table
 * @func sortTableData
 */
export function sortTableData(tableData, columns, sortInfoStr) {
    const sortInfoCls = SortInfo.parse(sortInfoStr);
    const colName = get(sortInfoCls, 'sortColumns.0');
    const dir = get(sortInfoCls, 'direction', UNSORTED);
    if (dir === UNSORTED || get(tableData, 'length', 0) === 0) return tableData;

    const multiplier = dir === SORT_ASC ? 1 : -1;
    const colIdx = columns.findIndex( (col) => {return col.name === colName;} );
    const col = columns[colIdx];
    if (!col) return tableData;

    var comparator;
    if (!col.type || ['char', 'c'].includes(col.type) ) {
        comparator = (r1, r2) => {
            const [s1, s2] = [r1[colIdx], r2[colIdx]];
            return multiplier * (s1 > s2 ? 1 : -1);
        };
    } else {
        comparator = (r1, r2) => {
            const [v1, v2] = [r1[colIdx], r2[colIdx]];
            return multiplier * (Number(v1) - Number(v2));
        };
    }
    tableData.sort(comparator);
    return tableData;
}

/**
 * filter the given table.  This function update the table data in-place.
 * @param {TableModel} table
 * @param {string} filterInfoStr filters are separated by comma(',').
 * @memberof firefly.util.table
 * @func filterTable
 */
export function filterTable(table, filterInfoStr) {
    if (filterInfoStr) {
        const comparators = filterInfoStr.split(';').map((s) => s.trim()).map((s) => FilterInfo.createComparator(s, table));
        table.tableData.data = table.tableData.data.filter((row, idx) => {
            return comparators.reduce( (rval, match) => rval && match(row, idx), true);
        } );
        table.totalRows = table.tableData.data.length;
    }
    return table.tableData;
}

export function processRequest(origTableModel, tableRequest, hlRowIdx) {
    const {filters, sortInfo, inclCols, startIdx, pageSize} = tableRequest;

    var nTable = cloneDeep(origTableModel);
    nTable.origTableModel = origTableModel;
    nTable.request = tableRequest;
    var {data, columns} = nTable.tableData;

    if (filters || sortInfo) {      // need to track original rowId.
        columns.push({name: 'ORIG_IDX', type: 'int', visibility: 'hidden'});
        data.forEach((r, idx) => r.push(String(idx)));
    }

    if (filters) {
        filterTable(nTable, filters);
    }
    data = nTable.tableData.data;
    if (sortInfo) {
        data = sortTableData(data, columns, sortInfo);
    }
    if (inclCols) {
        const colAry = inclCols.split(',').map((s) => s.trim());
        columns = columns.filters( (c) => colAry.includes(c));
        const inclIdices = columns.map( (c) => origTableModel.tableData.indexOf(c));
        data = data.map( (r) =>  r.filters( (c, idx) => inclIdices.includes(idx)));
    }
    data = data.slice(startIdx, startIdx + (pageSize ? pageSize : data.length));
    nTable.highlightedRow = hlRowIdx || startIdx;
    nTable.tableData.data = data;
    nTable.tableData.columns = columns;

    return nTable;
}

/**
 * collects all available table information given the tbl_id
 * @param {string} tbl_id
 * @param {number} aPageSize  use this pageSize instead of the one in the request.
 * @returns {{tableModel, tbl_id, title, totalRows, request, startIdx, endIdx, hlRowIdx, currentPage, pageSize, totalPages, highlightedRow, selectInfo, error, tableMeta, bgStatus}}
 * @public
 * @memberof firefly.util.table
 * @func getTblInfoById
 */
export function getTblInfoById(tbl_id, aPageSize) {
    const tableModel = getTblById(tbl_id);
    return getTblInfo(tableModel, aPageSize);
}

/**
 * collects all available table information given the tableModel.
 * @param {TableModel} tableModel
 * @param {number} aPageSize  use this pageSize instead of the one in the request.
 * @returns {{tableModel, tbl_id, title, totalRows, request, startIdx, endIdx, hlRowIdx, currentPage, pageSize, totalPages, highlightedRow, selectInfo, error, tableMeta, bgStatus}}
 * @public
 * @memberof firefly.util.table
 * @func getTblInfo
 */
export function getTblInfo(tableModel, aPageSize) {
    if (!tableModel) return {};
    var {tbl_id, request, highlightedRow=0, totalRows=0, tableMeta={}, selectInfo, error} = tableModel;
    const title = tableMeta.title || get(request, 'META_INFO.title');
    const pageSize = aPageSize || get(request, 'pageSize', MAX_ROW);  // there should be a pageSize.. default to 1 in case of error.  pageSize cannot be 0 because it'll overflow.
    if (highlightedRow < 0 ) {
        highlightedRow = 0;
    } else  if (highlightedRow >= totalRows-1) {
        highlightedRow = totalRows-1;
    }
    const currentPage = highlightedRow >= 0 ? Math.floor(highlightedRow / pageSize)+1 : 1;
    const hlRowIdx = highlightedRow >= 0 ? highlightedRow % pageSize : 0;
    const startIdx = (currentPage-1) * pageSize;
    const endIdx = Math.min(startIdx+pageSize, totalRows) || get(tableModel,'tableData.data.length', startIdx) ;
    const totalPages = Math.ceil((totalRows || 0)/pageSize);
    const bgStatus = get(tableModel, 'bgStatus');
    return { tableModel, tbl_id, title, totalRows, request, startIdx, endIdx, hlRowIdx, currentPage, pageSize,totalPages, highlightedRow, selectInfo, error, tableMeta, bgStatus};
}


/**
 * Return the row data as an object keyed by the column name
 * @param {TableModel} tableModel
 * @param {Number} [rowIdx] = the index of the row to return, default to highlighted row
 * @return {Object<String,String>} the values of the row keyed by the column name
 * @public
 * @memberof firefly.util.table
 * @func getTblRowAsObj
 */
export function getTblRowAsObj(tableModel, rowIdx= undefined) {
    if (!tableModel) return {};
    const {highlightedRow, tableData} = tableModel;
    if (!tableData) return {};
    const {data, columns}= tableData;
    if (isUndefined(rowIdx)) rowIdx= highlightedRow;
    if (rowIdx<0 || rowIdx>= get(tableData, 'data.length',0)) return {};
    const row= data[rowIdx];
    if (!row) return {};
    return row.reduce( (obj,v, idx)  => {
           obj[columns[idx].name]= v;
           return obj;
          }, {});
}

/**
 * returns the url to download a snapshot of the current table data.
 * @param {string} tbl_ui_id  UI id of the table
 * @param {object} params supplement parameter setting such as the information for workspace
 * @returns {string}
 * @public
 * @memberof firefly.util.table
 * @func getTableSourceUrl
 */
export function getTableSourceUrl(tbl_ui_id, params) {
    const {columns, request} = getTableUiById(tbl_ui_id) || {};
    return makeTableSourceUrl(columns, request, params);
}

/**
 * Async version of getTableSourceUrl.  If the given tbl_ui_id is backed by a local TableModel,
 * then we need to push/upload the content of the server before it can be referenced via url.
 * @param {string} tbl_ui_id  UI id of the table
 * @param {object} params supplement parameter setting such as the information for workspace
 * @returns {Promise.<string, Error>}
 */
export function getAsyncTableSourceUrl(tbl_ui_id, params) {
    const {tbl_id, columns} = getTableUiById(tbl_ui_id) || {};
    const ipacTable = tableToIpac(getTblById(tbl_id));
    const blob = new Blob([ipacTable]);
    //const file = new File([new Blob([ipacTable])], filename);
    return doUpload(blob).then( ({status, cacheKey}) => {
        const request = makeFileRequest('save as text', cacheKey, {pageSize: MAX_ROW});
        return makeTableSourceUrl(columns, request, params);
    });
}

function makeTableSourceUrl(columns, request, otherParams) {
    const tableRequest = Object.assign(cloneDeep(request), {startIdx: 0,pageSize : MAX_ROW});
    const visiCols = columns.filter( (col) => get(col, 'visibility', 'show') === 'show')
                            .map( (col) => col.name);
    if (visiCols.length !== columns.length) {
        tableRequest['inclCols'] = visiCols.map( (c) => c.includes('"') ? c : '"' + c + '"').join();  // add quotes to cname unless it's already quoted.
    }
    Reflect.deleteProperty(tableRequest, 'tbl_id');
    const {wsCmd, file_name} = otherParams || {};

    const params = omitBy({
        [ServerParams.COMMAND]: (!wsCmd ? ServerParams.TABLE_SAVE : ServerParams.WS_PUT_TABLE_FILE),
        [ServerParams.REQUEST]: JSON.stringify(tableRequest),
        file_name: (!wsCmd) && (file_name || get(tableRequest, 'META_INFO.title'))
    }, isNil);

    if (otherParams) {
        Object.assign(params, omitBy(otherParams, isNil));
    }
    return wsCmd ? params : encodeServerUrl(DEF_BASE_URL, params);
}

/**
 * convert this table into IPAC format
 * @param tableModel
 * @returns {string}
 */
export function tableToIpac(tableModel) {
    const {tableData, tableMeta} = tableModel;
    const {columns, data} = tableData || {};

    const colWidths = calcColumnWidths(columns, data);

    const meta = Object.entries(tableMeta).map(([k,v]) => `\\${k} = ${v}`)
                    .concat(columns.filter( (c) => c.visibility === 'hidden').map( (c) => `\\col.${c.name}.Visibility = ${c.visibility}`))
                    .concat(columns.filter( (c) => c.filterable).map( (c) => `\\col.${c.name}.Filterable = ${c.filterable}`))
                    .concat(columns.filter( (c) => c.sortable).map( (c) => `\\col.${c.name}.Sortable = ${c.sortable}`))
                    .concat(columns.filter( (c) => c.label).map( (c) => `\\col.${c.name}.Label = ${c.label}`))
                    .concat(columns.filter( (c) => c.desc).map( (c) => `\\col.${c.name}.ShortDescription = ${c.desc}`))
                    .join('\n');

    const head = [
                    columns.map((c, idx) => padEnd(c.name, colWidths[idx])),
                    columns.map((c, idx) => padEnd(c.type, colWidths[idx])),
                    columns.find((c) => c.units) && columns.map((c, idx) => padEnd(c.units, colWidths[idx])),
                    columns.find((c) => c.nullString) && columns.map((c, idx) => padEnd(c.nullString, colWidths[idx]))
                ]
                .filter( (ary) => ary)
                .map( (ary) => '|' + ary.join('|') + '|')
                .join('\n');
    const dataStr = data.map( (row) => ' ' + row.map( (c, idx) => padEnd(c, colWidths[idx])).join(' ') + ' ' )
                    .join('\n');

    return [meta, '\\', head, dataStr].join('\n');
}

export function tableTextView(columns, dataAry, showUnits=false, tableMeta) {

    const colWidths = calcColumnWidths(columns, dataAry);
    const meta = tableMeta && Object.entries(tableMeta).map(([k,v]) => `\\${k} = ${v}`).join('\n');
    const head = [
                    columns.map((c, idx) => get(c,'visibility', 'show') === 'show' && padEnd(c.name, colWidths[idx])),
                    columns.map((c, idx) => get(c,'visibility', 'show') === 'show' && padEnd(c.type, colWidths[idx])),
                    showUnits && columns.map((c, idx) => get(c,'visibility', 'show') === 'show' && padEnd(c.units, colWidths[idx]))
                ]
                .filter( (ary) => ary)
                .map( (ary) => '|' + ary.filter( (v) => v ).join('|') + '|')
                .join('\n');

    const dataStr = dataAry.map((row) => ' ' +
                                    row.map((c, idx) => get(columns, `${idx}.visibility`, 'show') === 'show' && padEnd(c, colWidths[idx]))
                                       .filter((v) => v)
                                       .join(' ')
                                    + ' ')
                           .join('\n');

    return [meta, head, dataStr].join('\n');
}


/**
 * returns an object map of the column name and its width.
 * The width is the number of characters needed to display
 * the header and the data in a table given columns and dataAry.
 * @param {TableColumn[]} columns  array of column object
 * @param {TableData} dataAry  array of array.
 * @returns {Object.<string,number>} a map of cname -> width
 * @memberof firefly.util.table
 * @func calcColumnWidths
 */
export function calcColumnWidths(columns, dataAry) {
    return columns.reduce( (pv, cv, idx) => {
        const cname = cv.label || cv.name;
        var width = Math.max(cname.length, get(cv, 'units.length', 0),  get(cv, 'type.length', 0));
        width = dataAry.reduce( (maxWidth, row) => {
            return Math.max(maxWidth, get(row, [idx, 'length'], 0));
        }, width);  // max width of data
        pv[idx] = width;
        return pv;
    }, {ROW_IDX: 8});
}

/**
 * create a unique table id (tbl_id)
 * @returns {string}
 * @public
 * @memberof firefly.util.table
 * @func uniqueTblId
 */
export function uniqueTblId() {
    const uid = getWsConnId();
    const id = uniqueId(`tbl_id-c${uid}-`);
    if (getTblById(id)) {
        return uniqueTblId();
    } else {
        return id;
    }
}

/**
 * create a unique table UI id (tbl_ui_id)
 * @returns {string}
 * @public
 * @memberof firefly.util.table
 * @func uniqueTblUiId
 */
export function uniqueTblUiId() {
    const uid = getWsConnId();
    const id = uniqueId(`tbl_ui_id-c${uid}-`);
    if (getTableUiById(id)) {
        return uniqueTblUiId();
    } else {
        return id;
    }
}
/**
 *  This function provides a patch until we can reliably determine that the ra/dec columns use radians or degrees.
 * @param tableOrMeta the table object or the tableMeta object
 * @memberof firefly.util.table
 * @func isTableUsingRadians
 *
 */
export function isTableUsingRadians(tableOrMeta) {
    if (!tableOrMeta) return false;
    const tableMeta= tableOrMeta.tableMeta || tableOrMeta;
    return has(tableMeta, 'HIERARCH.AFW_TABLE_VERSION');
}

export function createErrorTbl(tbl_id, error) {
    return set({tbl_id, error}, 'tableMeta.Loading-Status', 'COMPLETED');
}



/**
 * this function invoke the given callback when changes are made to the given tbl_id
 * @param {string}   tbl_id  table id to watch
 * @param {Object}   actions  an array of table actions to watch
 * @param {function} callback  callback to execute when table is loaded.
 * @param {string} [watcherId] action watcher id to be used
 * @return {function} returns a function used to cancel
 */
export function watchTableChanges(tbl_id, actions, callback, watcherId) {
    const accept = (a) => tbl_id === (get(a, 'payload.tbl_id') || get(a, 'payload.request.tbl_id'));
    return monitorChanges(actions, accept, callback, watcherId);
}

/**
 * this function invoke the given callback when the given actions occur.
 * @param {Object}   actions  an array of actions to watch
 * @param {function} accept  a function used to filter incoming actions.  if not given, it will accept all.
 * @param {function} callback  callback to execute when action occurs.
 * @param {string} [watcherId] action watcher id to be used
 * @return {function} returns a function used to cancel
 */
export function monitorChanges(actions, accept, callback, watcherId) {
    if (!Array.isArray(actions) || actions.length === 0 || !callback) return;

    const id = watcherId || uniqueID();
    const mCallback = (action) => {
        if (accept(action)) {
            callback(action);
        }
    };
    dispatchAddActionWatcher({id, actions, callback:mCallback});
    return () => dispatchCancelActionWatcher(id);
}


/**
 * @summary returns the non-hidden columns of the given table.  If type is given, it
 * will only return columns that match type.
 * @param {TableModel} tableModel
 * @param {COL_TYPE} type  one of predefined COL_TYPE.  defaults to 'ALL'.
 * @returns {Array<TableColumn>}
 * @public
 * @memberof firefly.util.table
 * @func getColumns
 */
export function getColumns(tableModel, type=COL_TYPE.ALL) {
    return getColsByType(get(tableModel, 'tableData.columns', []), type);
}

/**
 * @summary returns only the non-hidden columns matching the given type.
 * @param {Array<TableColumn>} tblColumns
 * @param {COL_TYPE} type  one of predefined COL_TYPE.  defaults to 'ALL'.
 * @returns {Array<TableColumn>}
 */
export function getColsByType(tblColumns=[], type=COL_TYPE.ALL) {
    const matcher = type === COL_TYPE.TEXT ? isTextType : isNumericType;
    return tblColumns.filter((col) => get(col, 'visibility') !== 'hidden'
                        && (type === COL_TYPE.ALL || matcher(col)));
}

export function isNumericType(col={}) {
    return num_types.includes(col.type);
}

export function isTextType(col={}) {
    return char_types.includes(col.type);
}

/**
 * @param {string} tbl_id
 * @returns {string} returns a key used by table to store backgrounding information.  This is used by BgMaskPanel.
 */
export function makeBgKey(tbl_id) {
    return `tables:${tbl_id}`;
}

/*-------------------------------------private------------------------------------------------*/
/**
 * Action watcher callback for table update, which is invoked when
 * the table given by tbl_id is fully loaded.
 * @callback actionWatcherCallback
 * @param action  action that triggered this watcher
 * @param cancelSelf  function to cancel this watcher
 * @param params  parameters object
 * @param {string}   params.tbl_id  table id to watch
 * @param {function} params.resolve  callback to execute when table is loaded.
 */
function doOnTblLoaded(action, cancelSelf, {tbl_id, resolve}) {
    if (!resolve) cancelSelf();

    if (tbl_id === get(action, 'payload.tbl_id')) {
        const tableModel = getTblById(tbl_id);
        if (get(tableModel, 'error')) {
            // there was an error loading this table.
            resolve(createErrorTbl(tbl_id, tableModel.error));
            cancelSelf();
        } else if (isTableLoaded(tableModel) &&  get(tableModel, 'tableData.columns.length')) {
            resolve(getTblInfoById(tbl_id));
            cancelSelf();
        }
    }
}
