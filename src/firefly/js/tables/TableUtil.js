/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, set} from 'lodash';
import isBlank from 'underscore.string/isBlank';
import TblCntlr from './TablesCntlr.js';
import {flux} from '../Firefly.js';

function doValidate(type, action) {
    if (type !== action.type) {
        error(action, `Incorrect type:${action.type} was sent to a ${type} actionCreator.`);
    }
    if (!action.payload) {
        error(action, 'Invalid action.  Payload is missing.');
    }
    if (type === TblCntlr.FETCH_TABLE ) {
        if (isBlank(action.payload.id)) {
            error(action, 'Required "id" field is missing.');
        }
    } else {
        if (isBlank(action.payload.tbl_id)) {
            error(action, 'Required "tbl_id" field is missing.');
        }
        if(type === TblCntlr.TBL_HIGHLIGHT_ROW) {
            const idx = action.payload.highlightedRow;
            if (!idx || idx<0) {
                error(action, 'highlightedRow must be a positive number.');
            }
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
function error(action, cause) {
    (action.err = action.err || []).push(cause);
}

function findById(id, space='main') {
    var tableSpace = flux.getState()[TblCntlr.TABLE_SPACE_PATH];
    return find(tableSpace, space,id);
}

/**
 * put a table model object into the given application state.
 * @param tableSpace
 * @param value the table model object to put
 * @returns {*}
 */
function put(tableSpace, value, space='main') {
    return set(tableSpace, space + '.' + value.tbl_id, value);
}

/**
 * find the object at the given paths.
 * @param data  data root.  find will start from here.
 * @param paths an array of path elements.
 * @returns {*} an object or undefined if paths does not exist under the data root.
 */
function find(data, ...paths) {
    return data ? get(data, paths) : null;
}


function isFullyLoaded(id, space='main') {
    const table = findById(id, space);
    if (table && table.model) {
        if (table.model.tableMeta.isFullyLoaded) {
            return true;
        }
    }
    return false;
}

/**
 * This function transform the json data from the server to fit the need of the UI.
 * For instance, the column's name is repeated after transform.  This is good for the UI.
 * But, it's more efficient to not include it during data transfer from the server.
 * @param tableModel
 * @returns {*}
 */
function transform(tableModel) {

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

export default {
    error,
    doValidate,
    isFullyLoaded,
    findById,
    put,
    find,
    transform
};