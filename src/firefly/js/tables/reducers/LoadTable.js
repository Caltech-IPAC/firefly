/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {fetchUrl} from '../../util/WebUtil.js';
import { getRootPath } from '../../util/BrowserUtil.js';
import TblCntlr from '../TablesCntlr.js';
import TblUtil from '../TableUtil.js';
import {SelectInfo} from '../SelectInfo.js';

const SRV_PATH = getRootPath() + 'search/json';
const INT_MAX = Math.pow(2,31) - 1;

/**
 * reducer for LOAD_TABLE action.
 * TableModel data will be added if one does not exist.  Otherwise, it will update the existing
 * TableModel via a merge.
 * @param state the object root of TABLE_SPACE_PATH.
 * @param action LOAD_TABLE action.  action's payload should be a TableModel object.
 * @returns {*}
 */
function reducer(state={}, action={}) {

    switch (action.type) {
        case (TblCntlr.LOAD_TABLE)  :
        case (TblCntlr.TBL_HIGHLIGHT_ROW)  :
        case (TblCntlr.LOAD_TABLE_STATUS)  :
            return mergeTable(state, action.payload);
            break;

        case (TblCntlr.LOAD_TABLE_COMPLETE)  :

        default:
            return state;
    }

}

function mergeTable(state, newTable) {
    var table = TblUtil.find(state, 'main', newTable.tbl_id);
    if (table) {
        newTable = Object.assign({}, table, newTable);
    }
    newTable.selectInfo = newTable.selectInfo || {selectAll: false, exceptions: new Set(), rowCount: newTable.totalRows};
    TblUtil.put(state, newTable, 'main', newTable.tbl_id);
    return state;
}


/**
 *
 * @param tableRequest is a TableRequest params object
 * @returns {Promise.<T>}
 */
function doFetchTable(tableRequest) {
    const def = {
        startIdx: 0,
        pageSize : INT_MAX,
        tbl_id : (tableRequest.tbl_id || tableRequest.title || tableRequest.id)
    };
    var params = Object.assign(def, tableRequest);

    return fetchUrl(SRV_PATH, {params}).then( (response) => {
        return response.json().then( (tableModel) => {
            //TblUtil.transform(tableModel);
            return tableModel;
        });
    });
}

export default {
    reducer,
    doFetchTable
};