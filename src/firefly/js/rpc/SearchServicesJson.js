/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

import {get, set} from 'lodash';

import {ServerParams} from '../data/ServerParams.js';
import {doJsonRequest, DEF_BASE_URL} from '../core/JsonUtils.js';
import {BackgroundStatus} from '../core/background/BackgroundStatus.js';
import {MAX_ROW} from '../tables/TableUtil.js';
import {getBgEmail} from '../core/background/BackgroundUtil.js';
import {encodeUrl, download} from '../util/WebUtil.js';

import Enum from 'enum';
import {getTblById} from '../tables/TableUtil.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {getBackgroundJobs} from '../core/background/BackgroundUtil.js';

export const DownloadProgress= new Enum(['STARTING', 'WORKING', 'DONE', 'UNKNOWN', 'FAIL']);
export const ScriptAttributes= new Enum(['URLsOnly', 'Unzip', 'Ditto', 'Curl', 'Wget', 'RemoveZip']);


const DOWNLOAD_REQUEST = 'downloadRequest';
const SELECTION_INFO = 'selectionInfo';

//TODO: convert FileStatus


/**
 * tableRequest will be sent to the server as a json string.
 * @param {TableRequest} tableRequest is a table request params object
 * @param {number} [hlRowIdx] set the highlightedRow.  default to startIdx.
 * @returns {Promise.<TableModel>}
 * @public
 * @func doFetchTable
 * @memberof firefly.util.table
 */
export function fetchTable(tableRequest, hlRowIdx) {

    const def = {
        startIdx: 0,
        pageSize : MAX_ROW
    };
    tableRequest = Object.assign(def, tableRequest);
    const params = {
        [ServerParams.REQUEST]: JSON.stringify(tableRequest),
    };

    return doJsonRequest(ServerParams.TABLE_SEARCH, params)
    .then( (tableModel) => {
        const startIdx = get(tableModel, 'request.startIdx', 0);
        if (startIdx > 0) {
            // shift data arrays indices to match partial fetch
            tableModel.tableData.data = tableModel.tableData.data.reduce( (nAry, v, idx) => {
                nAry[idx+startIdx] = v;
                return nAry;
            }, []);
        }
        tableModel.highlightedRow = hlRowIdx || startIdx;
        return tableModel;
    });
}

/**
 * tableRequest will be sent to the server as a json string.
 * @param {TableRequest} tableRequest is a table request params object
 * @param {string} filterInfo filter info string used to find the first row that matches it.
 * @returns {Promise.<number>}
 */
export function findTableIndex(tableRequest, filterInfo) {

    const params = {
        [ServerParams.REQUEST]: JSON.stringify(tableRequest),
        filterInfo
    };
    return doJsonRequest(ServerParams.TABLE_FIND_INDEX, params)
        .then( (index) => {
            return index;
        });
}

/**
 * returns the table data for the given parameters
 * @param {Object} p  parameters object
 * @param {string[]} p.columnNames an array of column names
 * @param {string} p.filePath   location of the file on the server
 * @param {string} p.selectedRows   a comma-separated string of indices of the rows to get the data from
 * @return {Promise<TableModel>}
 */
export const selectedValues = function({columnNames, filePath, selectedRows}) {
    columnNames = Array.isArray(columnNames) ? columnNames.join() : String(columnNames);
    selectedRows = Array.isArray(selectedRows) ? selectedRows.join() : String(selectedRows);
    return doJsonRequest(ServerParams.SELECTED_VALUES, {columnNames, filePath, selectedRows})
            .then((tableModel) => {
                return tableModel;
            });
};


/**
 *
 * @param {DownloadRequest} dlRequest
 * @param {Object} searchRequest
 * @param {string} selectionInfo
 */
export function packageRequest(dlRequest, searchRequest, selectionInfo) {
    if (!selectionInfo) {
        const {totalRow} = getTblById(searchRequest.tbl_id) || {};
        if (totalRow) {
            selectionInfo = SelectInfo.newInstance({selectAll: true, rowCount: totalRow}).toString();
        }
    }
    if (!dlRequest.Email && getBgEmail()) {
        dlRequest.Email = getBgEmail();
    }

    const params = {
        [DOWNLOAD_REQUEST]: JSON.stringify(dlRequest),
        [ServerParams.REQUEST]: JSON.stringify(searchRequest),
        [SELECTION_INFO]: selectionInfo
    };

    return doJsonRequest(ServerParams.PACKAGE_REQUEST, params);
}


/**
 *
 * @param {ServerRequest} request
 * @return {Promise}
 */
export const getJsonData = function(request) {
    var paramList = [];
    paramList.push({name:ServerParams.REQUEST, value: request.toString()});

    return doJsonRequest(ServerParams.JSON_DATA, paramList
    ).then((data) => {return data; });
};

/**
 *
 * @param {ServerRequest} request
 * @param {ServerRequest} clientRequest
 * @param {number} waitMillis
 * @return {Promise}
 */
export const submitBackgroundSearch= function(request, clientRequest, waitMillis) {
    if (getBgEmail()) {
        request = set(request, ['META_INFO', ServerParams.EMAIL], getBgEmail());
    }
    const params = {
        [ServerParams.REQUEST]: JSON.stringify(request),
        [ServerParams.WAIT_MILS]: String(waitMillis)
    };
    clientRequest && (params[ServerParams.CLIENT_REQUEST] = JSON.stringify(clientRequest));

    return doJsonRequest(ServerParams.SUB_BACKGROUND_SEARCH, params);
};

/**
 * add this job to the background
 * @param {string} id background id
 * @return {Promise}
 */
export function addBgJob(bgStats) {
    const params = {bgStats: JSON.stringify(bgStats)};
    return doJsonRequest(ServerParams.ADD_JOB, params).then( () => true);
};

/**
 *
 * @param {string} id background id
 * @return {Promise}
 */
export function removeBgJob(id) {
    const params = {[ServerParams.ID]: id};
    return doJsonRequest(ServerParams.REMOVE_JOB, params).then( () => true);
};

/**
 *
 * @param {string} id background id
 * @return {Promise}
 */
export const cancel= function(id) {
    var paramList = [];
    paramList.push({name: ServerParams.ID, value: id});
    return doJsonRequest(ServerParams.CANCEL, paramList
    ).then( () => true);
};

/**
 *
 * @param {string} fileKey
 * @return {Promise}
 */
export const getDownloadProgress= function(fileKey) {
    var paramList = [];
    paramList.push({name: ServerParams.FILE, value: fileKey});
    return doJsonRequest(ServerParams.DOWNLOAD_PROGRESS, paramList
    ).then((data) => {return DownloadProgress.get(data); });
};


/**
 * @param {string} email
 * @return {Promise}
 */
export const setEmail= function(email) {
    var idList= Object.keys(getBackgroundJobs() || {});
    var paramList= idList.map( (id) => {
        return {name:ServerParams.ID, value: id};
    } );
    if(paramList.length > 0) {
        paramList.push({name:ServerParams.EMAIL, value:email});
        return doJsonRequest(ServerParams.SET_EMAIL, paramList
        ).then( () => true);
    }
};

/**
 *
 * @param {array|string} ids one id or an array of ids
 * @param {JobAttributes} attribute job attribute
 * @return {Promise}
 */
export const setAttribute= function(ids, attribute) {
    var idList=  Array.isArray(ids) ? ids : [ids];
    var paramList= idList.map( (id) => {
        return {name:ServerParams.ID, value: id};
    } );
    paramList.push({name:ServerParams.ATTRIBUTE, value:attribute.toString()});
    return doJsonRequest(ServerParams.SET_ATTR, paramList
    ).then( () => true);
};

/**
 * resend email notification to all successfully completed background jobs.
 * @param {string} email
 * @return {Promise}
 */
export const resendEmail= function(email) {
    return doJsonRequest(ServerParams.RESEND_EMAIL, {[ServerParams.EMAIL]: email}
    ).then( () => true);
};

/**
 *
 * @param {string} id
 * @param {number} idx
 * @return {Promise}
 */
export const clearPushEntry= function(id, idx ) {
    var paramList = [];
    paramList.push({name: ServerParams.ID, value: id});
    paramList.push({name: ServerParams.IDX, value: `${idx}`});
    return doJsonRequest(ServerParams.CLEAR_PUSH_ENTRY, paramList
    ).then( () => true);
};

/**
 *
 * @param {string} channel
 * @param {string} desc
 * @param {string} data
 * @return {Promise}
 */
export const reportUserAction= function(channel, desc, data) {
    var paramList = [];
    paramList.push({name: ServerParams.CHANNEL_ID, value: channel});
    paramList.push({name: ServerParams.DATA, value: data});
    paramList.push({name: ServerParams.DESC, value: desc});
    return doJsonRequest(ServerParams.REPORT_USER_ACTION, paramList
    ).then( () => true);
};


/**
 *
 * @param id
 * @param fname
 * @param dataSource
 * @param {array} attributes and array of ScriptAttributes
 * @return {Promise}
 */
export const createDownloadScript= function(id, fname, dataSource, attributes) {
    var attrAry= Array.isArray(attributes) ? attributes : [attributes];
    var paramList= attrAry.map( (v='') => ({name:ServerParams.ATTRIBUTE, value: v.toString()}) );
    paramList.push({name: ServerParams.ID, value: id});
    paramList.push({name: ServerParams.FILE, value: fname});
    paramList.push({name: ServerParams.SOURCE, value: dataSource});
    return doJsonRequest(ServerParams.CREATE_DOWNLOAD_SCRIPT, paramList);
};

/**
 * Download the download script to the user's computer.
 * @param {Object} p
 * @param {string} p.packageID
 * @param {string} p.type
 * @param {string} p.fname
 * @param {string} p.dataSource
 */
export function getDownloadScript({packageID, type, fname, dataSource}) {
    const url = encodeUrl(DEF_BASE_URL, {packageID, type, fname, dataSource});
    if (url) download(url);
}

