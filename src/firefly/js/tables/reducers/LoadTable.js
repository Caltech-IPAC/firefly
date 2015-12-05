/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {fetchUrl} from '../../util/WebUtil.js';
import { getRootPath } from '../../util/BrowserUtil.js';
import TblCntlr from '../TablesCntlr.js';
import TblUtil from '../TableUtil.js';
import {REQ_PRM} from '../TableRequest.js';

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
        case (TblCntlr.HIGHLIGHT_ROW)  :
            return mergeTable(state, action.payload);
            break;

        case (TblCntlr.LOAD_TABLE_STATUS)  :
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
    TblUtil.put(state, newTable, 'main', newTable.tbl_id);
    return state;
}


/**
 *
 * @param tableRequest TableRequest
 * @returns {Promise.<T>}
 */
function doFetchTable(tableRequest) {
    const def = {
        [REQ_PRM.PAGE_SIZE] : INT_MAX,
        [REQ_PRM.TBL_ID] : (tableRequest.tbl_id || tableRequest.params.title || tableRequest.params.id)
    };
    var params = tableRequest.params ? Object.assign(def, tableRequest.params) : def;
    params = Object.assign(params, tableRequest);
    Reflect.deleteProperty( params, 'params' );

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