/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isEmpty, uniqueId} from 'lodash';
import * as TblCntlr from './TablesCntlr.js';
import * as TblUiCntlr from './TablesUiCntlr.js';
import {SelectInfo} from './SelectInfo.js';
import {flux} from '../Firefly.js';
import {fetchUrl} from '../util/WebUtil.js';
import { getRootPath } from '../util/BrowserUtil.js';
import {TableRequest} from './TableRequest.js';

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
            if (!tableModel.selectionInfo) {
                tableModel.selectionInfo = SelectInfo.newInstance({rowCount:tableModel.totalRows}).data;
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
    if (type === TblCntlr.FETCH_TABLE ) {
        if (isEmpty(request.id)) {
            error(action, 'Required "id" field is missing.');
        }
        if (isEmpty(request.tbl_id)) {
            error(action, 'Required "tbl_id" field is missing.');
        }
    } else if(type === TblCntlr.TBL_HIGHLIGHT_ROW) {
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

export function findById(id) {
    var tableSpace = flux.getState()[TblCntlr.TABLE_SPACE_PATH];
    return get(tableSpace, id);
}

/**
 * find table ui info by tbl_ui_id and tbl_ui_gid
 * @param tid
 * @param gid
 * @returns {*}
 */
export function findUiById(tbl_ui_id, tbl_ui_gid) {
    return get(flux.getState(), [TblUiCntlr.TABLE_UI_PATH, tbl_ui_gid, tbl_ui_id]);
}

/**
 * return true if the table referenced by the given tbl_id is fully loaded.
 * @param tbl_id
 * @returns {boolean}
 */
export function isFullyLoaded(tbl_id) {
    return isTableLoaded(findById(tbl_id));
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

    if ( source && typeof(source)=='object') {
        if(source instanceof Array) {
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
        return (target == source) ? target : source;
    }
}

export function uniqueTblId() {
    return uniqueId('tbl_id-');
}

export function uniqueTblUiId() {
    return uniqueId('tbl_ui_id-');
}

export function uniqueTblUiGid() {
    return uniqueId('tbl_ui_gid-');
}