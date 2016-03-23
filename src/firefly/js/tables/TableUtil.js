/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isEmpty, uniqueId, padEnd, cloneDeep} from 'lodash';
import * as TblCntlr from './TablesCntlr.js';
import * as TblUiCntlr from './TablesUiCntlr.js';
import {SelectInfo} from './SelectInfo.js';
import {flux} from '../Firefly.js';
import {fetchUrl, encodeServerUrl} from '../util/WebUtil.js';
import {getRootPath, getRootURL} from '../util/BrowserUtil.js';
import {TableRequest} from './TableRequest.js';

const SAVE_TABLE_URL = getRootURL() + 'servlet/SaveAsIpacTable';
const SRV_PATH = getRootPath() + 'search/json';
const INT_MAX = Math.pow(2,31) - 1;

/**
 *
 * @param tableRequest is a TableRequest params object
 * @param hlRowIdx set the highlightedRow.  default to startIdx.
 * @returns {Promise.<T>}
 */
export function doFetchTable(tableRequest, hlRowIdx) {
    const def = {
        startIdx: 0,
        pageSize : INT_MAX,
        tbl_id : (tableRequest.tbl_id || tableRequest.title || tableRequest.id)
    };
    var params = Object.assign(def, tableRequest);

    return fetchUrl(SRV_PATH, {params}).then( (response) => {
        return response.json().then( (tableModel) => {
            const startIdx = get(tableModel, ['request',TableRequest.keys.startIdx], 0);
            if (startIdx > 0) {
                // shift data arrays indices to match partial fetch
                tableModel.tableData.data = tableModel.tableData.data.reduce( (nAry, v, idx) => {
                    nAry[idx+startIdx] = v;
                    return nAry;
                }, []);
            }
            tableModel.highlightedRow = hlRowIdx || startIdx;
            if (!tableModel.selectInfo) {
                tableModel.selectInfo = SelectInfo.newInstance({rowCount:tableModel.totalRows}).data;
            }
            return tableModel;
        });
    });
}

export function doValidate(type, action) {
    if (type !== action.type) {
        error(action, `Incorrect type:${action.type} was sent to a ${type} actionCreator.`);
    }
    if (!action.payload) {
        error(action, 'Invalid action.  Payload is missing.');
    }
    var {request} = action.payload;
    if (type === TblCntlr.TABLE_FETCH ) {
        if (request.id) {
            error(action, 'Required "id" field is missing.');
        }
        if (request.tbl_id) {
            error(action, 'Required "tbl_id" field is missing.');
        }
    } else if(type === TblCntlr.TABLE_HIGHLIGHT) {
        const idx = action.payload.highlightedRow;
        if (!idx || idx<0) {
            error(action, 'highlightedRow must be a positive number.');
        }
    }
    return action;
}

/**
 * update the given action with a new error given by cause.
 * action.err is stored as an array of errors.  Errors may be a String or an Error type.
 * @param action  the actoin to update
 * @param cause  the error to be added.
 */
export function error(action, cause) {
    (action.err = action.err || []).push(cause);
}

/**
 * return true is there is data within the given range.  this is needed because
 * of paging table not loading the full table.
 * @param startIdx
 * @param endIdx
 * @param tableModel
 * @returns {boolean}
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


export function findTblById(id) {
    return get(flux.getState(),[TblCntlr.TABLE_SPACE_PATH, id]);
}

/**
 * find table ui_group info by tbl_ui_gid
 * @param tbl_ui_gid
 * @returns {*}
 */
export function findUiGroupById(tbl_ui_gid) {
    return get(flux.getState(), [TblUiCntlr.TABLE_UI_PATH, 'results', tbl_ui_gid]);
}

/**
 * find table ui_group info by tbl_ui_gid
 * @param tbl_ui_gid
 * @param tbl_ui_id
 * @returns {*}
 */
export function findTblUiById(tbl_ui_gid, tbl_ui_id) {
    return get(flux.getState(), [TblUiCntlr.TABLE_UI_PATH, 'results', tbl_ui_gid, 'tables', tbl_ui_id]);
}

/**
 * find working table state by tbl_ui_id
 * @param tbl_ui_id
 * @returns {*}
 */
export function findTablePanelStateById(tbl_ui_id) {
    return get(flux.getState(), [TblUiCntlr.TABLE_UI_PATH, 'work', tbl_ui_id]);
}

/**
 * return true if the table referenced by the given tbl_id is fully loaded.
 * @param tbl_id
 * @returns {boolean}
 */
export function isFullyLoaded(tbl_id) {
    return isTableLoaded(findTblById(tbl_id));
}

export function getCellValue(tableModel, rowIdx, colName) {
    if (tableModel.tableData && tableModel.tableData.data) {
        const cols = tableModel.tableData.columns;
        const colIdx = cols.findIndex((col) => {
            return col.name === colName;
        });
        // might be undefined if row is not loaded
        return get(tableModel.tableData.data, [rowIdx, colIdx]);
    }
}


/**
 * return true if the given table is fully loaded.
 * @param tableModel
 * @returns {boolean}
 */
export function isTableLoaded(tableModel) {
    const status = tableModel && get(tableModel, 'tableMeta.Loading-Status', 'COMPLETED');
    return status === 'COMPLETED';
}

/**
 * This function transform the json data from the server to fit the need of the UI.
 * For instance, the column's name is repeated after transform.  This is good for the UI.
 * But, it's more efficient to not include it during data transfer from the server.
 * @param tableModel
 * @returns {*}
 */
export function transform(tableModel) {

    if (tableModel.tableData && tableModel.tableData.data) {
        const cols = tableModel.tableData.columns;
        // change row data from [ [val] ] to [ {cname:val} ]
        tableModel.tableData.data = tableModel.tableData.data.map( (row) => {
            return cols.reduce( (nrow, col, cidx) => {
                nrow[col.name] = row[cidx];
                return nrow;
            }, {});
        });
    }
}

/**
 * This function merges the source object into the target object
 * by traversing and comparing every like path.  If a value was
 * merged at any data node in the data graph, the node and all of its
 * parent nodes will be shallow cloned and returned.  Otherwise, the target's value
 * will be returned.
 * @param target
 * @param source
 * @returns {*}
 */
export function smartMerge(target, source) {
    if (!target) return source;

    if ( source && typeof(source)==='object') {
        if(Array.isArray(source)) {
            let aryChanges = [];
            source.forEach( (v, idx) => {
                const nval = smartMerge(target[idx], source[idx]);
                if (nval !== target[idx]) {
                    aryChanges[idx] = nval;
                }
            });
            if (isEmpty(aryChanges)) return target;
            else {
                let nAry = target.slice();
                aryChanges.forEach( (v, idx) => nAry[idx] = v );
                return nAry;
            }
        } else {
            let objChanges = {};
            Object.keys(source).forEach( (k) => {
                const nval = smartMerge(target[k], source[k]);
                if (nval !== target[k]) {
                    objChanges[k] = nval;
                }
            });
            return (isEmpty(objChanges)) ? target : Object.assign({}, target, objChanges);
        }
    } else {
        return (target === source) ? target : source;
    }
}

export function gatherTableState(tableModel) {
    var {tbl_id, highlightedRow} = tableModel;

    const pageSize = get(tableModel, 'request.pageSize', 1);  // there should be a pageSize.. default to 1 in case of error.  pageSize cannot be 0 because it'll overflow.
    const currentPage = highlightedRow >= 0 ? Math.floor(highlightedRow / pageSize)+1 : 1;
    const hlRowIdx = highlightedRow >= 0 ? highlightedRow % pageSize : 0;
    const startIdx = (currentPage-1) * pageSize;
    const endIdx = Math.min(startIdx+pageSize, tableModel.totalRows) || startIdx ;
    var totalPages = Math.ceil((tableModel.totalRows || 0)/pageSize);
    return {tbl_id, startIdx, endIdx, hlRowIdx, currentPage, pageSize,totalPages, highlightedRow};
}


export function tableToText(columns, dataAry, showUnits=false) {

    var textHead = columns.reduce( (pval, cval, idx) => {
        return pval + (columns[idx].visibility === 'show' ? `${padEnd(cval.name, columns[idx].width)}|` : '');
    }, '|');

    if (showUnits) {
        textHead += '\n' + columns.reduce( (pval, cval, idx) => {
            return pval + (columns[idx].visibility === 'show' ? `${padEnd(cval.units || '', columns[idx].width)}|` : '');
        }, '|');
    }

    var textData = dataAry.reduce( (pval, row) => {
        return pval +
            row.reduce( (pv, cv, idx) => {
                return pv + (get(columns, [idx,'visibility']) === 'show' ? `${padEnd(cv || '', columns[idx].width)} ` : '');
            }, ' ') + '\n';
    }, '');
    return textHead + '\n' + textData;
}


/**
 *
 * @param columns
 * @param request
 * @param filename
 * @returns {encoded}
 */
export function getTableSourceUrl(columns, request, filename) {
    const {startIdx, pageSize, inclCols} = TableRequest.keys;
    request = cloneDeep(request);
    const visiCols = columns.filter( (col) => {
                return col.visibility === 'show';
            }).map( (col) => {
                return col.name;
            } );
    if (visiCols.length !== columns.length) {
        request[inclCols] = visiCols.toString();
    }

    request[startIdx] = 0;
    request[pageSize] = Number.MAX_SAFE_INTEGER;
    filename = filename || request.file_name || request.id;
    const requestCls = TableRequest.newInstance(request);
    const params = {
        Request: requestCls.toString(),
        file_name: filename
    };
    return encodeServerUrl(SAVE_TABLE_URL, params);
}



export function uniqueTblId() {
    return uniqueId('tbl_id-');
}

export function uniqueTblUiGid() {
    return uniqueId('tbl_ui_gid-');
}

export function uniqueTblUiId() {
    return uniqueId('tbl_ui_id-');
}