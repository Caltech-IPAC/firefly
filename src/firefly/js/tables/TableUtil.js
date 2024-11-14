/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {chunk, cloneDeep, get, has, isArray, isEmpty, isNil, isObject, isPlainObject, isString, isUndefined, omitBy,
    padEnd, set, uniqueId, omit, uniq} from 'lodash';
import Enum from 'enum';

import {getWsConnId} from '../core/AppDataCntlr.js';
import {doJsonRequest} from '../core/JsonUtils';
import {sprintf} from '../externalSource/sprintf.js';
import {fetchUrl} from '../util/fetch';
import {makeFileRequest, MAX_ROW} from './TableRequestUtil.js';
import * as TblCntlr from './TablesCntlr.js';
import {SortInfo, SORT_ASC, UNSORTED} from './SortInfo.js';
import {FilterInfo, getNumFilters, FILTER_SEP} from './FilterInfo.js';
import {SelectInfo} from './SelectInfo.js';
import {flux} from '../core/ReduxFlux.js';
import {encodeServerUrl, uniqueID} from '../util/WebUtil.js';
import {createTableSearchParams, fetchTable, queryTable, selectedValues} from '../rpc/SearchServicesJson.js';
import {ServerParams} from '../data/ServerParams.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../core/MasterSaga.js';
import {MetaConst} from '../data/MetaConst';
import {getCmdSrvSyncURL, toBoolean, strictParseInt} from '../util/WebUtil';
import {upload} from '../rpc/CoreServices.js';
import {dd2sex} from '../visualize/CoordUtil.js';

export const SYS_COLUMNS = ['ROW_IDX', 'ROW_NUM'];
export const DOC_FUNCTIONS_URL = 'https://duckdb.org/docs/sql/functions/overview.html';

// this is so test can mock the function when used within it's module
const local = {
    isTableLoaded,
    getTblById,
    getTblInfoById,
    getColumn,
    getColumns,
    getCellValue,
    getSelectedData
};
export default local;

const TEXT  = ['char'];
const INT   = ['long', 'int', 'short', 'integer'];
const FLOAT = ['double', 'float', 'real'];
const BOOL  = ['boolean','bool'];
const DATE  = ['date'];
const NUMBER= [...INT, ...FLOAT];
const USE_STRING = [...TEXT, ...DATE];

// export const COL_TYPE = new Enum(['ALL', 'NUMBER', 'TEXT', 'INT', 'FLOAT']);
export const COL_TYPE = new Enum({ANY:[],TEXT, INT, FLOAT, BOOL, DATE, NUMBER, USE_STRING});
export const TBL_STATE = new Enum(['ERROR', 'LOADING', 'NO_DATA', 'NO_MATCH', 'OK']);

/**
 * @param {TableColumn} col
 * @param {COL_TYPE} type
 * @returns true if col is of the given type
 */
export function isColumnType(col={}, type) {
    const flg = '_t-' + type.key;       // for efficiency
    if (!has(col, flg)) {
        col[flg] = isOfType(col.type, type);
    }
    return col[flg];
}

/**
 *
 * @param {string} s string
 * @param {COL_TYPE} type
 * @returns {boolean}
 */
export function isOfType(s, type) {
    return type === COL_TYPE.ANY || COL_TYPE[type].value.includes(s);
}

export function isClientTable(tbl_id) {
    const tableModel = getTblById(tbl_id) || {};
    return !!tableModel.origTableModel;
}


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
        return Promise.resolve(processRequest(tableModel, tableRequest, hlRowIdx));
    } else {
        return fetchTable(tableRequest, hlRowIdx);
    }
}

export async function fetchSpacialBinary(tableRequest) {
    const params = createTableSearchParams(tableRequest);
    const cmd= ServerParams.TABLE_SEARCH_SPATIAL_BINARY;
    const response= await fetchUrl(getCmdSrvSyncURL()+`?${ServerParams.COMMAND}=${cmd}`,{method:'POST', params },false);
    if (!response.ok) {
        throw(new Error(`Error from Server for ${cmd}: code: ${response.status}, text: ${response.statusText}`));
    }
    const arrayBuffer= await response.arrayBuffer();
    return arrayBuffer;
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
    return watchForTableLoaded(true, tbl_id);
}

/*
 * similar to onTableLoaded but callback happens before TABLE_LOADED is fired
 */
export function preTableLoaded(tbl_id) {
    return watchForTableLoaded(false, tbl_id);
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
 * @returns {String}
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
        TblCntlr.dispatchTableRemove(tbl_id, false);        // all table will be removed.  not need to fireActiveTableChanged
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
    return Object.keys(get(tableGroup, 'tables', []));
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
    return Object.values(uiRoot).find( (tblUiState) => tblUiState?.tbl_id === tbl_id);
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
    const comparators = filterInfo.split(FILTER_SEP).map((s) => s.trim()).map((s) => FilterInfo.createComparator(s, tableModel));
    const idx = get(tableModel, 'tableData.data', []).findIndex((row, idx) => comparators.reduce( (rval, matcher) => rval && matcher(row, idx), true));
    if (idx >= 0) {
        return Promise.resolve(idx);
    } else {
        const inclCols = 'ROW_NUM as ORG_ROWNUM';
        return queryTable(tableModel.request, {filters: filterInfo, inclCols}).then( (tableModel) => {
            return get(tableModel, ['tableData','data']) ? get(getColumnValues(tableModel, 'ORG_ROWNUM'), '0', -1) : -1;
        });
    }
}


/**
 * Returns the column index with the given name; otherwise, -1.
 * @param {TableModel} tableModel
 * @param {string} colName
 * @param {boolean} ignoreCase if true then case when finding columns
 * @returns {number}
 * @public
 * @func getColumnIdx
 * @memberof firefly.util.table
 */
export function getColumnIdx(tableModel, colName, ignoreCase= false) {
    const cols = getAllColumns(tableModel);
    return cols.findIndex((col) => {
        return ignoreCase? col?.name?.toLowerCase()===colName?.toLowerCase() : col?.name === colName;
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
    const cols = getAllColumns(tableModel);
    return get(cols.find((col)=> col.name === colName), 'type', '');
}


/**
 * returns column information for the given name.
 * @param {TableModel} tableModel
 * @param {string} colName
 * @param {boolean} ignoreCase if true then case when finding columns
 * @returns {TableColumn}
 * @public
 * @func getColumn
 * @memberof firefly.util.table
 */
export function getColumn(tableModel, colName, ignoreCase= false) {
    const colIdx = getColumnIdx(tableModel, colName, ignoreCase);
    if (colIdx >= 0) {
        return get(tableModel, `tableData.columns.${colIdx}`);
    }
}


export function columnIDToName(tableModel, ID) {
    if (!tableModel || !ID) return undefined;
    return getAllColumns(tableModel)?.find((col) => col.ID === ID)?.name;
}


/**
 * returns column information for the given ID.  This function searches all columns, including hidden ones.
 * @param {TableModel} tableModel
 * @param {string} ID
 * @returns {TableColumn}
 * @public
 * @func getColumn
 * @memberof firefly.util.table
 */
export function getColumnByID(tableModel, ID) {
    return getAllColumns(tableModel).find((c) => c.ID === ID);
}

/**
 * returns column information for the given ref.  This function searches all columns, including hidden ones.
 * It will first try to match ref with ID.  If not found, it will return the first column with matching name.
 * @param {TableModel} tableModel
 * @param {string} ref      matches a column ID or name, in that order.
 * @returns {TableColumn}
 * @public
 * @func getColumnByRef
 * @memberof firefly.util.table
 */
export function getColumnByRef(tableModel, ref) {
    return getColumnByID(tableModel, ref) ||
            getAllColumns(tableModel).find((c) => c.name === ref);
}

export function getFilterCount(tableModel) {
    const request = get(tableModel, 'request');
    const filterCount = getNumFilters(request);
    return filterCount;
}

export function clearFilters(tableModel) {
    const {request, tbl_id} = tableModel || {};
    if (request && (request.filters || request.sqlFilter)) {
        TblCntlr.dispatchTableFilter({tbl_id, filters: '', sqlFilter: ''});
    }
}

export function getSqlFilter(tbl_id) {
    const sqlFilter =  get(getTblById(tbl_id), 'request.sqlFilter');
    if (sqlFilter) {
        const [op, sql] = sqlFilter.split('::');
        return {op, sql};
    }
    return {};
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
    if (colIdx >= 0 && colIdx < get(tableModel, 'tableData.columns.length', 0)) {
        return get(tableModel, 'tableData.data', []).map( (r) => r[colIdx]);
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
 * Fold 1d array value according to column's arraySize specification.
 * For example, if `col.arraySize` is `'2x3'` and `val` is `[1,2,3,4,5,6]`, the result will be `[[1,2],[3,4],[5,6]]`.
 * @param {TableColumn} col
 * @param {Object} val
 * @returns {*} array value with right dimensions, if a column is an array, or an unchanged value
 */
export function convertToArraySize(col, val) {

    const aryDim = (col.arraySize || '').split('x');

    if (col.type === 'char' && aryDim.length > 0) {
        aryDim.shift();    // remove first dimension because char array is presented as string
    }

    if (aryDim.length > 1) {
        for(let i = 0; i < aryDim.length-1; i++) {
            val = chunk(val, strictParseInt(aryDim[i]));
        }
    }
    return val;
}


/**
 * Firefly has 3 column meta that affect the formatting of the column's data.  They are
 * listed below in order of highest to lowest precedence.
 *
 * fmtDisp		: A Java format string.  It can be used to format any type.  i.e.  "cost $%.2f"
 * format		: Same as fmtDisp
 * precision	: This only applies to floating point numbers.
 *                A string Tn where T is either F, E, G, HMS, or DMS
 *                When T is F or E, n is the number of significant figures after the decimal point.
 *                When T is G, n is the number of significant digits
 *                When T is HMS or DMS, n is ignored

 * @param {TableColumn} col
 * @param {Object} val
 * @return {string}
 * @public
 * @func formatValue
 * @memberof firefly.util.table
 */
export function formatValue(col, val) {
    const {fmtDisp, format, precision, nullString} = col || {};

    if (isNil(val)) return (nullString || '');

    if (Array.isArray(val)) {
        val = convertToArraySize(col, val);
        return isString(val) ? val : JSON.stringify(val);
    }

    if (fmtDisp) {
        return sprintf(fmtDisp, val);
    } else if (format) {
        return sprintf(format, val);
    } else if (isColumnType(col, COL_TYPE.INT)) {
        if (typeof val !== 'bigint' && isNaN(val)) return Number.NaN+'';
        return sprintf('%i', val);
    } else if (isColumnType(col, COL_TYPE.FLOAT)) {
        if (precision) {
            let [type, prec] = parsePrecision(precision);
            if (type === 'HMS') {
                return dd2sex(val, false, true);    // use prec+3 to get num of decimal places
            } else if (type === 'DMS') {
                return dd2sex(val, true, true);     // use prec+4 to get num of decimal places
            } else {
                if (typeof val !== 'bigint' && isNaN(val)) return Number.NaN+'';
                if (!type || type === 'F') type = 'f';
                prec = '.' + prec;
                return sprintf('%' + prec + type, val);
            }
        } else {
            return sprintf('%J', val);
        }
    }
    return String(val);
}

export function parsePrecision(s = '') {
    const [, type, sprec] = s.trim().toUpperCase().match(/^(HMS|DMS|[EFG])?(\d*)$/) || [];
    const prec = sprec ? parseInt(sprec) : -1;
    if (type === 'G' && prec < 1) return [];
    if (['E','F'].includes(type) && prec < 0) return [];
    if (['HMS','DMS'].includes(type)) return [type];

    return [type, prec];
}

export function getTypeLabel(col={}) {
    const {type, arraySize=''} = col;
    if (!type) return '';
    const aryDim = arraySize.split('x').filter( (s) => s).length;
    if (type === 'char' && aryDim === 1) {
        return type;
    } else {
        return type + (aryDim > 0 ? `[${arraySize}]` : '');
    }
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
    const {tableModel, totalRows, selectInfo, request} = local.getTblInfoById(tbl_id);
    const selectedRows = [...SelectInfo.newInstance(selectInfo).getSelected()];  // get selected row idx as an array

    if (selectedRows.length === 0 || isTblDataAvail(0, totalRows -1, tableModel)) {
        return Promise.resolve(getSelectedDataSync(tbl_id, columnNames));
    } else {
        return selectedValues({columnNames, request, selectedRows});
    }
}

/**
 * Similar to getSelectedData, but will only check data available on the client.
 * It will not attempt to fetch required data.  This is good for client table.
 * @param tbl_id
 * @param columnNames
 * @return {TableModel}
 */
export function getSelectedDataSync(tbl_id, columnNames=[]) {
    const {tableModel, tableMeta, selectInfo} = local.getTblInfoById(tbl_id);
    const selectedRows = [...SelectInfo.newInstance(selectInfo).getSelected()];  // get selected row idx as an array

    if (columnNames.length === 0) {
        columnNames = getAllColumns(tableModel).map( (c) => c.name);       // return all columns
    }
    const meta = cloneDeep(tableMeta);

    const columns = columnNames.map((cname) => local.getColumn(tableModel, cname))
        .filter((c) => c)
        .map((c) => cloneDeep(c));

    const data = selectedRows.sort()
        .map( (rIdx) => columns.map((c) => local.getCellValue(tableModel, rIdx, c.name)));

    return {tableMeta: meta, totalRows: data.length, tableData: {columns, data}};
}

/**
 * return true if the given table is fully loaded.
 * @param {TableModel} tableModel
 * @returns {boolean}
 * @memberof ffirefly.util.table
 * @func isTableLoaded
 */
export function isTableLoaded(tableModel={}) {
    const {isFetching} = tableModel;
    const status = get(tableModel, 'tableMeta.Loading-Status');
    return !isFetching && status === 'COMPLETED';
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
            const [s1, s2] = [alterFalsyVal(r1[colIdx]), alterFalsyVal(r2[colIdx])];
            return multiplier * (s1 > s2 ? 1 : -1);
        };
    } else {
        comparator = (r1, r2) => {
            let [v1, v2] = [r1[colIdx], r2[colIdx]];
            v1 = v1 === null ? -Number.MAX_VALUE : isUndefined(v1) ? Number.NEGATIVE_INFINITY : Number(v1);
            v2 = v2 === null ? -Number.MAX_VALUE : isUndefined(v2) ? Number.NEGATIVE_INFINITY : Number(v2);
            return multiplier * (Number(v1) - Number(v2));
        };
    }
    tableData.sort(comparator);
    return tableData;
}

export function alterFalsyVal(s) {
    return s === '' ? '\u0002' : s === null ? '\u0001' : isUndefined(s) ? '\u0000' : s;
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
        const comparators = filterInfoStr.split(FILTER_SEP).map((s) => s.trim()).map((s) => FilterInfo.createComparator(s, table));
        table.tableData.data = table.tableData.data.filter((row, idx) => {
            return comparators.reduce( (rval, match) => rval && match(row, idx), true);
        } );
        table.totalRows = table.tableData.data.length;
    }
    return table.tableData;
}


export function cloneClientTable (tableModel) {
    const nTable = cloneDeep(tableModel);
    nTable.origTableModel = tableModel;

    set(nTable.origTableModel, 'tableMeta.resultSetRequest', JSON.stringify(tableModel.request));

    nTable.isFetching = false;
    set(nTable, 'tableMeta.Loading-Status', 'COMPLETED');

    if (nTable.tableData) {
        const {data=[], columns=[]} = nTable.tableData;
        // add ROW_IDX to working table for tracking original row index
        columns.push({name: 'ROW_IDX', type: 'int', visibility: 'hidden'});
        data.forEach((r, idx) => r.push(idx));
    }
    return nTable;
}

export function processRequest(tableModel, tableRequest, hlRowIdx) {
    const {filters, sortInfo, inclCols} = tableRequest;
    const {origTableModel=tableModel} = tableModel;
    let {startIdx=0, pageSize} = tableRequest;

    const nTable = cloneClientTable(origTableModel);
    let {data, columns} = nTable.tableData;

    nTable.request = tableRequest;
    pageSize = pageSize > 0 ? pageSize : data.length || MAX_ROW;

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

    const prevHlRowIdx = get(tableRequest, ['META_OPTIONS', MetaConst.HIGHLIGHTED_ROW_BY_ROWIDX], 0);
    if (hlRowIdx) {
        const currentPage = Math.floor(hlRowIdx / pageSize) + 1;
        startIdx = (currentPage-1) * pageSize;
        nTable.highlightedRow = hlRowIdx;
    } else if (prevHlRowIdx) {
        // preserve previous highlightedRow if possible.
        let relRowIdx = data.findIndex( (r, rIdx) => getCellValue(nTable, rIdx, 'ROW_IDX') === prevHlRowIdx );
        relRowIdx = relRowIdx < 0 ? 0 : relRowIdx;
        const currentPage = Math.floor(relRowIdx / pageSize) + 1;
        startIdx = (currentPage-1) * pageSize;
        nTable.highlightedRow = relRowIdx;
    } else {
        nTable.highlightedRow = startIdx;
    }

    data = data.slice(startIdx, startIdx + pageSize);

    nTable.tableData.data = data;
    nTable.tableData.columns = columns;

    // set selections from the original table
    const idxCol = getColumnIdx(nTable, 'ROW_IDX');
    if (idxCol >= 0) {
        const nTableSelectInfoCls = SelectInfo.newInstance({rowCount: data.length});
        const origSelectInfoCls = SelectInfo.newInstance(get(origTableModel, 'selectInfo'));
        data.forEach((row, i) => {
            const origIdx = parseInt(row[idxCol]);
            // set selection
            nTableSelectInfoCls.setRowSelect(i, origSelectInfoCls.isSelected(origIdx));
        });
        nTable.selectInfo = nTableSelectInfoCls.data;
    }

    if (!nTable.error) {
        set(origTableModel, 'tableMeta.resultSetRequest', JSON.stringify(nTable.request));
    }

    return nTable;
}

/**
 * collects all available table information given the tbl_id
 * @param {string} tbl_id
 * @param {number} [aPageSize]  use this pageSize instead of the one in the request.
 * @returns {{tableModel, tbl_id, title, totalRows, request, startIdx, endIdx, hlRowIdx, currentPage, pageSize, totalPages, highlightedRow, selectInfo, error, tableMeta, backgroundable}}
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
 * @returns {{tableModel, tbl_id, title, totalRows, request, startIdx, endIdx, hlRowIdx, currentPage, pageSize, totalPages, highlightedRow, selectInfo, error, tableMeta, backgroundable}}
 * @public
 * @memberof firefly.util.table
 * @func getTblInfo
 */
export function getTblInfo(tableModel, aPageSize) {
    if (!tableModel) return {};
    var {tbl_id, request, highlightedRow=0, totalRows=0, tableMeta={}, selectInfo, error} = tableModel;
    const title = tableMeta.title || request?.META_INFO?.title || 'untitled';
    const pageSize = aPageSize > 0 ? aPageSize : fixPageSize(request?.pageSize);
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
    const backgroundable = get(tableModel, 'request.META_INFO.backgroundable', false);

    return { tableModel, tbl_id, title, totalRows, request, startIdx, endIdx, hlRowIdx, currentPage, pageSize,totalPages, highlightedRow, selectInfo, error, tableMeta, backgroundable};
}


/**
 * Return the row data as an object keyed by the column name
 * @param {TableModel|undefined} tableModel
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
    return upload(blob).then( ({status, cacheKey}) => {
        const request = makeFileRequest('save as text', cacheKey, null, {pageSize: MAX_ROW});
        return makeTableSourceUrl(columns, request, params);
    });
}

export function makeTableSourceUrl(columns, request, otherParams) {
    const tableRequest = Object.assign(cloneDeep(request), {startIdx: 0,pageSize : MAX_ROW});
    const visiCols = columns.filter( (col) => get(col, 'visibility', 'show') === 'show')
                            .map( (col) => col.name);
    if (visiCols.length !== columns.length) {
        tableRequest['inclCols'] = visiCols.map( (c) => c.includes('"') ? c : '"' + c + '"').join();  // add quotes to cname unless it's already quoted.
    }
    const origTable = getTblById(request?.tbl_id);
    const precision = columns.filter( (col) => col.precision)
                             .filter( (col) => col.precision !== get(getColumn(origTable, col.name), 'precision'))
                             .map( (col) => [`col.${col.name}.precision`, col.precision]);
    if (precision) {
        tableRequest.META_INFO = tableRequest.META_INFO || {};
        precision.forEach(([key, val]) => tableRequest.META_INFO[key] = val);
    }

    const nullStr = columns.filter( (col) => col.nullString)
        .filter( (col) => col.nullString !== get(getColumn(origTable, col.name), 'nullString'))
        .map( (col) => [`col.${col.name}.nullString`, col.nullString]);
    if (nullStr) {
        tableRequest.META_INFO = tableRequest.META_INFO || {};
        nullStr.forEach(([key, val]) => tableRequest.META_INFO[key] = val);
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
    return wsCmd ? params : encodeServerUrl(getCmdSrvSyncURL(), params);
}

export function setHlRowByRowIdx(nreq, tableModel) {
    const hlRowIdx = getCellValue(tableModel, tableModel.highlightedRow, 'ROW_IDX');
    if (hlRowIdx) {
        set(nreq, ['META_OPTIONS', MetaConst.HIGHLIGHTED_ROW_BY_ROWIDX], hlRowIdx);
    }
}


/**
 *
 * @param {TableModel|String} tableOrId - parameters accepts the table model or tha table id
 * @param {number} rowIdx - rowIdx to check
 * @param {number} [hlRowIdx] - highlighted row index, defaults to tableModel.highlightedRow
 * @return {boolean} true if it is sub-highlight
 */
export function isSubHighlightRow(tableOrId, rowIdx, hlRowIdx) {
    const tableModel = getTM(tableOrId);
    if (!tableModel?.tableMeta) return false;
    const relatedCols = tableModel.tableMeta['tbl.relatedCols'];
    if (!relatedCols) return false;
    const colNameAry= relatedCols.split(',').map( (c) => c.trim());
    const highlightedRow= hlRowIdx ?? tableModel.highlightedRow;

    const makeCellKey= (row) => colNameAry.map((cname) => getCellValue(tableModel, row, cname)).join('|');
        
    return makeCellKey(highlightedRow) === makeCellKey(rowIdx);
}

export function hasSubHighlightRows(tableOrId) {
    const tableModel = getTM(tableOrId);
    if (!tableModel?.tableMeta) return false;
    return Boolean(tableModel.tableMeta['tbl.relatedCols']);
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
                    columns.map((c, idx) => padEnd(getTypeLabel(c), colWidths[idx])),
                    columns.find((c) => c.units) && columns.map((c, idx) => padEnd(c.units, colWidths[idx])),
                    columns.find((c) => c.nullString) && columns.map((c, idx) => padEnd(c.nullString, colWidths[idx]))
                ]
                .filter( (ary) => ary)
                .map( (ary) => '|' + ary.join('|') + '|')
                .join('\n');

    const dataStr = data.map((row) =>
                    ' ' +
                        columns.map((c, idx) =>
                                padEnd(formatValue(c, row[idx]), colWidths[idx])
                                .replace(/[^\x1F-\x7F]/g, '\xBF')).join(' ') +              // replace non-printable chars with (191 in LATIN-1) inverted '?'.  same logic as DataType.format()
                    ' ').join('\n');

    return [meta, '\\', head, dataStr].join('\n');
}

export function tableTextView(columns, dataAry, tableMeta) {

    const colWidths = calcColumnWidths(columns, dataAry, {useWidth: false});
    const meta = tableMeta && Object.entries(tableMeta).map(([k,v]) => `\\${k} = ${v}`).join('\n');

    const cols = columns.map((c, idx) => get(c,'visibility', 'show') === 'show' ? [c, idx] : null).filter((c) => c);  // only visible columns: [col, colIdx]

    const colSep = '+' + cols.map(([, idx]) => '-'.repeat(colWidths[idx])).join('+') + '+';
    const names  = '|' + cols.map(([c, idx]) => padEnd(c.label || c.name, colWidths[idx])).join('|') + '|';
    const types  = '|' + cols.map(([c, idx]) => padEnd(getTypeLabel(c), colWidths[idx])).join('|') + '|';

    const head = [colSep, names, types, colSep].join('\n');

    const dataStr = dataAry.map((row) =>
                ' ' +
                    cols.map(([c, idx]) =>
                            padEnd(formatValue(c, row[idx]), colWidths[idx])
                            .replace(/[^\x1F-\x7F]/g, '\xBF')).join(' ') +              // replace non-printable chars with (191 in LATIN-1) inverted '?'.  same logic as DataType.format()
                ' ').join('\n');

    return [meta, head, dataStr].filter((c) => c).join('\n');
}

/**
 * returns a details view of the highlightedRow in a form of a tableModel
 * with columns Name, Value, Type, Units, and Description
 * @param {string} tbl_id             tbl_id of the table
 * @param {number} highlightedRow     the row to generate the details for
 * @param {string} details_tbl_id     tbl_id of the details table.  defaults to tbl_id + '_details'
 */
export function tableDetailsView(tbl_id, highlightedRow, details_tbl_id) {
    const tableModel = getTblById(tbl_id);
    const dataCols = getColumns(tableModel);
    const {totalRows} = tableModel || {};
    const nTblId = details_tbl_id || tbl_id + '_details';

    if (totalRows <= 0 || highlightedRow < 0 || highlightedRow > tableModel.totalRows) {
        return {tbl_id: nTblId, error: 'No Data Found'};
    }

    const allColKeys = Object.keys(Object.assign({}, ...dataCols));

    const columns = [
        { key: 'name', name: 'Name', type: 'char', desc: 'Column name', dataGetter: (c) => c.label || c.name },
        { key: 'value', name: 'Value', type: 'char', dataGetter: (c) => formatValue(c, getCellValue(tableModel, highlightedRow, c.name)) },
        { key: 'units', name: 'Units', type: 'char', dataGetter: (c) => c.units || '' },
        { key: 'desc', name: 'Description', type: 'char', dataGetter: (c) => c.desc || '' },
        { key: 'type', name: 'Type', type: 'char', dataGetter: (c) => getTypeLabel(c) },
        { key: 'UCD', name: 'UCD', type: 'char', dataGetter: (c) => c.UCD || '' },
        { key: 'utype', name: 'UType', type: 'char', dataGetter: (c) => c.utype || '' },
    ].filter((col, index) =>
        index < 2 || allColKeys.includes(col.key) // filter columns if not present in given data columns, except name and value
    );

    const data = dataCols.map((c) =>
        columns.map((col) => (col.dataGetter ? col.dataGetter(c) : ''))
    );

    // add enum values for filtering of the following columns
    ['type', 'units', 'UCD'].forEach((colKey) => {
        if (allColKeys.includes(colKey)) {
            const colIdx = columns.findIndex((col) => col.key === colKey);
            const colValues = data.map((rowData) => rowData[colIdx]);
            columns[colIdx].enumVals = uniq(colValues.filter((d) => d)).join(',');
        }
    });

    const prevDetails = getTblById(nTblId) || {};
    const {request={}} = prevDetails;
    let nTable = {
        tbl_id: nTblId,
        request,
        title: 'Additional Information',
        tableData: {columns: columns.map((col) => omit(col, ['key', 'dataGetter'])), data},
        totalRows: data.length,
        highlightedRow: prevDetails.highlightedRow
    };
    if (request.sortInfo || request.filters) {
        setHlRowByRowIdx(request, prevDetails);
        nTable = processRequest(nTable, request);
    }
    return nTable;
}

/**
 * returns an array of the value with the maximum length for each column.
 * The width is the number of characters needed to display
 * the header and the data in a table given columns and dataAry.
 * @param {TableColumn[]} columns  array of column object
 * @param {TableData} dataAry  array of array.
 * @param {object} opt options
 * @param {number} opt.maxAryWidth  maximum width of column with array values
 * @param {number} opt.maxColWidth  maximum width of column without array values
 * @param {boolean} opt.useWidth    use width and prefWidth props in calculation
 * @returns {string[]} an array of values corresponding to the given columns array.
 * @memberof firefly.util.table
 * @func calcColumnWidths
 */
export function getColMaxValues(columns, dataAry, opt) {
    return columns.map( (cv, idx) => getColMaxVal(cv, idx, dataAry, opt));
}

export function getColMaxVal(col, columnIndex, dataAry,
                                {
                                    maxAryWidth = Number.MAX_SAFE_INTEGER,
                                    maxColWidth = Number.MAX_SAFE_INTEGER,
                                    useWidth = true,
                                }={}) {
    const width = useWidth? col.prefWidth || col.width : 0;
    if (width) {
        return 'O'.repeat(width);           // O is a good reference for average width of a character
    }

    let maxVal = '';

    // the 4 headers
    [col.label || col.name, col.units + '()', getTypeLabel(col), col.nullString].forEach( (v) => {
        if (v?.length > maxVal.length) maxVal = v;
    });

    // the data
    dataAry.forEach((row) => {
        const v = formatValue(col, row[columnIndex]);
        if (v.length > maxVal.length) maxVal = v;
    });

    // limits
    if (col.arraySize && maxVal.length > maxAryWidth) maxVal = maxVal.substr(0, maxAryWidth);
    if (maxVal.length > maxColWidth) maxVal = maxVal.substr(0, maxColWidth);

    return maxVal;
}

/**
 * returns an array of the maximum width for each column.
 * The width is the number of characters needed to display
 * the header and the data in a table given columns and dataAry.
 * @param {TableColumn[]} columns  array of column object
 * @param {TableData} dataAry  array of array.
 * @param {object} opt options
 * @param {number} opt.maxAryWidth  maximum width of column with array values
 * @param {number} opt.maxColWidth  maximum width of column without array values
 * @param {boolean} opt.useWidth    use width and prefWidth props in calculation
 * @returns {number[]} an array of widths corresponding to the given columns array.
 * @memberof firefly.util.table
 * @func calcColumnWidths
 */
export function calcColumnWidths(columns, dataAry, opt) {
    return getColMaxValues(columns, dataAry, opt).map((v) => v.length);
}

/**
 * There are some inconsistencies in how a request is created.
 * This fixes any of the inconsistencies it finds.
 * @param request
 */
export function fixRequest(request) {
    // ensure tbl_id exists and it's set correctly.
    const tbl_id = request.tbl_id || get(request, 'META_INFO.tbl_id', uniqueTblId());
    request.tbl_id = tbl_id;
    set(request, 'META_INFO.tbl_id', tbl_id);
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
 * @param {TableModel} table the table model
 * @param {Array.<String>} [columnNames]
 * @memberof firefly.util.table
 * @func isTableUsingRadians
 *
 */
export function isTableUsingRadians(table, columnNames) {
    if (!table && !table.isFetching) return false;
    const {tableMeta,tableData}=  table;
    if (isArray(columnNames) && tableData) {
        if (columnNames.every( (cName) => isColRadians(table, cName))) return true;
        if (columnNames.every( (cName) => isColDegree(table, cName))) return false;
    }
     // it the columns down specify it then we will only return true for LSST tables
    return Boolean(has(tableMeta, 'HIERARCH.AFW_TABLE_VERSION'));
}

export function isColRadians(table, colName) {
    if (!table) return false;
    const column= getColumn(table,colName);
    const unitField= get(column,'units','').toLowerCase();
    return unitField.startsWith('rad');
}

export function isColDegree(table, colName) {
    if (!table) return false;
    const column= getColumn(table,colName);
    const unitField= get(column,'units','').toLowerCase();
    return unitField.startsWith('deg');
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
 * @param {COL_TYPE} type  one of predefined COL_TYPE.  defaults to 'ANY'.
 * @returns {Array<TableColumn>}
 * @public
 * @memberof firefly.util.table
 * @func getColumns
 */
export function getColumns(tableModel, type=COL_TYPE.ANY) {
    return getColsByType(getAllColumns(tableModel), type);
}

/**
 * @summary returns all columns of the given table including hidden ones.
 * @param {TableModel} tableModel
 * @returns {Array<TableColumn>}
 * @public
 * @memberof firefly.util.table
 * @func getAllColumns
 */
export function getAllColumns(tableModel) {
    return get(tableModel, 'tableData.columns', []);
}

/**
 * @summary returns only the non-hidden columns matching the given type.
 * @param {Array<TableColumn>} tblColumns
 * @param {COL_TYPE} type  one of predefined COL_TYPE.  defaults to 'ANY'.
 * @returns {Array<TableColumn>}
 */
export function getColsByType(tblColumns=[], type=COL_TYPE.ANY) {
    return tblColumns.filter((col) =>
                get(col, 'visibility') !== 'hidden' &&
                isColumnType(col, type));
}

export function getColByUtype(tableModel, utype) {
    return getColumns(tableModel).filter( (col) => col.utype === utype);
}

export function getColByUCD(tableModel, ucd) {
    return getColumns(tableModel).filter( (col) => col.UCD === ucd);
}

/**
 * returns a 2-part array, [release date column, datarights column] if this table
 * contains proprietary data
 * @param tableModel
 * @returns {string[]}
 */
export function getProprietaryInfo(tableModel) {
    const rcname = get(tableModel, ['tableMeta', MetaConst.RELEASE_DATE_COL]);
    const dcname = get(tableModel, ['tableMeta', MetaConst.DATARIGHTS_COL]);
    return rcname || dcname ? [rcname, dcname] : [];
}

export function hasRowAccess(tableModel, rowIdx) {

    const [rcname, dcname] = getProprietaryInfo(tableModel);
    if (!rcname && !dcname) return true;        // no proprietary info

    if (dcname) {
        const rights = String(getCellValue(tableModel, rowIdx, dcname)).trim().toLowerCase();
        if (['public', 'secure', '1', 'true', 't'].includes(rights) ) {
            return true;
        }
    }
    if (rcname) {
        const rdate = getCellValue(tableModel, rowIdx, rcname);
        if (!rdate) return false;
        let rDateObj= new Date(rdate);
        if (rDateObj.toString()==='Invalid Date') {
            rDateObj= new Date(rdate.split(' ',2)?.join('T'));
            if (rDateObj.toString()==='Invalid Date') return false;
        }
        if (Date.now() > rDateObj) return true;
    }
    return false;
}


/**
 * @param {string} tbl_id
 * @returns {string} returns a key used by table to store backgrounding information.  This is used by BgMaskPanel.
 */
export function makeBgKey(tbl_id) {
    return `tables:${tbl_id}`;
}

/**
 * If input is '"x"', outputs 'x', but if input is '"x"+"y"' or 'log("x")' output is the same as input
 * @param s
 * @returns {*}
 */
export function stripColumnNameQuotes(s) {
    if (!s) return ;
    const newS = s.replace(/^"(.+)"$/, '$1');
    return newS.includes('"') ? s : newS;
}

export function tblDropDownId(tbl_id) {
    return `table_dropDown-${tbl_id}`;
}

/**
 *
 * @param {String} tableOrId
 * @return {undefined|TableModel}
 */
function getTM(tableOrId) {
    if (isString(tableOrId)) return getTblById(tableOrId);  // was passed a table Id
    if (isObject(tableOrId)) return tableOrId;
    return undefined;
}

/**
 * case insensitive search of meta data for an entry, if not found return the defVal
 * @param {TableModel|String} tableOrId - parameters accepts the table model or tha table id
 * @param {String} metaKey - the metadata key
 * @param {*|undefined} [defVal] - the defVal to return if not found, defaults to undefined
 * @return {String|undefined|*} value or the meta data or the defVal
 */
export function getMetaEntry(tableOrId,metaKey,defVal= undefined) {
    const tableMeta= get(getTM(tableOrId),'tableMeta');
    if (!tableMeta || !isString(metaKey)) return defVal;
    const keyUp = metaKey.toUpperCase();
    const [foundKey,value]= Object.entries(tableMeta).find( ([k]) => k.toUpperCase()===keyUp) || [];
    return (foundKey && value!==undefined) ? value : defVal;
}

/**
 * case insensitive search of meta data for boolean a entry, if not found return the defVal
 * @param {TableModel|String} tableOrId - parameters accepts the table model or tha table id
 * @param {String} metaKey - the metadata key
 * @param {boolean} [defVal= false] - the defVal to return if not found, defaults to false
 * @return {boolean} value or the meta data or the defVal
 */
export function getBooleanMetaEntry(tableOrId,metaKey,defVal= false) {
    return toBoolean(getMetaEntry(tableOrId,metaKey,undefined),Boolean(defVal),['true','t','yes','y']);
}

/**
 * return true if this table contains any auxiliary data, like meta, links, and params
 * @param tbl_id
 * @returns {boolean}
 */
export function hasAuxData(tbl_id) {
    const {keywords, links, params, resources, groups} = getTblById(tbl_id) || {};
    return !isEmpty(keywords) || !isEmpty(links) || !isEmpty(params) || !isEmpty(resources) || !isEmpty(groups);
}

/**
 * @param tbl_id  ID of the table
 * @param tableModel  or, the tableModel itself.
 * @return TBL_STATE of the table.
 */
export function getTableState(tbl_id, tableModel={}) {
    const {error, status, isFetching, totalRows, request={}} = getTblById(tbl_id) || tableModel;
    const {filters, sqlFilter} = request;

    if (error) return TBL_STATE.ERROR;
    if (isFetching) return TBL_STATE.LOADING;
    if (totalRows === 0) return TBL_STATE.NO_DATA;

    // check status
    if (status?.code && (status.code < 200 || status.code >= 400) ) return TBL_STATE.ERROR;
    if (status?.code === 204) return TBL_STATE.NO_MATCH;     // (204 No Content) - No data found matching the given filter criteria

    if (totalRows === 0 && (filters || sqlFilter)) return TBL_STATE.NO_MATCH;

    return TBL_STATE.OK;
}

export function fixPageSize(pageSize) {
    if ( !Number.isInteger(pageSize) )                  pageSize = parseInt(pageSize);
    if ( !Number.isInteger(pageSize) || pageSize <= 0)  pageSize = MAX_ROW;
    return pageSize;
}

/**
 * @param {string} cnames
 * @return {string[]} array of column names split from a comma separated string, ignoring commas inside double-quotes
 */
export function splitCols(cnames='') {
    return cnames.split(/,(?=(?:[^"]*"[^"]*")*[^"]*$)/);
}

/**
 * @param {string} values
 * @return {string[]} array of values split from a comma separated string, ignoring commas inside single-quotes
 */
export function splitVals(values='') {
    return values.split(/,(?=(?:[^']*'[^']*')*[^']*$)/);
}

export function parseError(error) {
    const message = error?.message ?? error;
    if (error?.cause) {
        const [_, type, cause] = error?.cause.match(/(.+?):(.+)/) || [];
        return {message, type, cause};
    } else {
        const [_, error, cause] = message?.match(/(.+?):(.+)/) || [];     // formatted error messages; 'error:cause'
        return {message: error || message, cause};
    }
}

export function isOverflow(tbl_id) {
    const {resources, tableMeta} = getTblById(tbl_id) || {};

    if (tableMeta?.QUERY_STATUS?.toUpperCase() === 'OVERFLOW') return true;

    const results = resources?.find((r) => r.type === 'results');
    return results?.infos?.QUERY_STATUS === 'OVERFLOW';
}

/*-------------------------------------private------------------------------------------------*/

/**
 * @param afterLoaded   if true, watch for TABLE_LOADED, then resolve
 * @param tbl_id        table to watch
 */
function watchForTableLoaded(afterLoaded, tbl_id) {

    if (isFullyLoaded(tbl_id)) {
        return Promise.resolve(getTblById(tbl_id));
    } else {
        const actions = [ afterLoaded ? TblCntlr.TABLE_LOADED : TblCntlr.TABLE_UPDATE, TblCntlr.TABLE_REPLACE, TblCntlr.TABLE_REMOVE];
        const callback = (action, cancelSelf, {resolve}) => {
            if (!resolve) cancelSelf();

            if (tbl_id === action.payload?.tbl_id) {
                if ( action.type === TblCntlr.TABLE_REMOVE) {
                    cancelSelf();
                } else {
                    const tableModel = getTblById(tbl_id);
                    if (tableModel?.error) {
                        // there was an error loading this table.
                        resolve(createErrorTbl(tbl_id, tableModel.error));
                        cancelSelf();
                    } else if (isTableLoaded(tableModel) &&  get(tableModel, 'tableData.columns.length')) {
                        resolve(getTblInfoById(tbl_id));
                        cancelSelf();
                    }
                }
            }
        };

        return new Promise((resolve) => {
            dispatchAddActionWatcher({ actions, callback, params: {resolve}});
        });
    }
}

