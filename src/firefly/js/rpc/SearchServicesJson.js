/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */


import {get, pickBy, cloneDeep, has, isUndefined} from 'lodash';
import {ServerParams} from '../data/ServerParams.js';
import {doJsonRequest} from '../core/JsonUtils.js';
import {submitJob, getBackgroundJobs, getJobInfo} from '../core/background/BackgroundUtil.js';
import {dispatchBgJobInfo} from '../core/background/BackgroundCntlr.js';
import {encodeUrl, updateSet, getCmdSrvSyncURL} from '../util/WebUtil.js';

import {getTblById, getResultSetID, getResultSetRequest} from '../tables/TableUtil.js';
import {MAX_ROW, getTblId, setResultSetID, setResultSetRequest, setSelectInfo} from '../tables/TableRequestUtil.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {getFireflySessionId} from '../Firefly.js';
import {download} from '../util/fetch';


const DOWNLOAD_REQUEST = 'downloadRequest';
const SELECTION_INFO = 'selectionInfo';

//TODO: convert FileStatus


const doBigIntRequest = (cmd, params) => {
    return doJsonRequest(cmd, params, true, true);
};


export function createTableSearchParams(tableRequest) {
    const def = {
        startIdx: 0,
        pageSize : MAX_ROW,
        ffSessionId: getFireflySessionId(),
    };

    tableRequest = setupNewRequest(tableRequest, def);

    const params = {
        [ServerParams.REQUEST]: JSON.stringify(tableRequest),
    };
    return params;
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
export function fetchTable(tableRequest, hlRowIdx) {

    const params = createTableSearchParams(tableRequest);

    return doBigIntRequest(ServerParams.TABLE_SEARCH, params)
    .then( (tableModel) => {
        const startIdx = get(tableModel, 'request.startIdx', 0);
        if (startIdx > 0) {
            const tblData = get(tableModel, 'tableData.data');
            if (tblData) {
                // shift data arrays indices to match partial fetch
                tableModel.tableData.data = tblData.reduce((nAry, v, idx) => {
                    nAry[idx + startIdx] = v;
                    return nAry;
                }, []);
            }
        }
        if (tableModel.selectInfo) {
            // convert selectInfo to JS object
            const selectInfo = SelectInfo.parse(tableModel.selectInfo);
            tableModel.selectInfo = selectInfo.data;
        }

        if (!isUndefined(hlRowIdx)) {
            tableModel.highlightedRow = hlRowIdx;
        } else if (!has(tableModel, 'highlightedRow')) {
            tableModel.highlightedRow = startIdx;
        }
        tableModel.title = tableModel.tableMeta?.title ?? tableModel.title;

        return tableModel;
    });
}

export function asyncFetchTable(tableRequest) {
    const params = createTableSearchParams(tableRequest);
    return submitJob(ServerParams.TABLE_SEARCH, params);
}

/**
 * a utility function used to query data from the given tableRequest without altering the table.
 * @param {TableRequest} tableRequest is a table request params object
 * @param {TableRequest} queryRequest filters, sortInfo, and inclCols are used on the tableRequest to return the results
 * @returns {Promise.<number>}
 */
export function queryTable(tableRequest, {filters, sortInfo, inclCols}) {

    const params = Object.assign(pickBy({filters, sortInfo, inclCols}), {[ServerParams.REQUEST]: JSON.stringify(tableRequest)});
    return doBigIntRequest(ServerParams.QUERY_TABLE, params)
        .then( (index) => {
            return index;
        });
}

/**
 * add a column to the table backed by this tableRequest
 * @param {TableRequest} tableRequest is a table request params object
 * @param colAttribs      column attributes.  i.e. name, type, expression, etc
 * @returns {Promise.<number>}
 */
export function addOrUpdateColumn(tableRequest, colAttribs) {

    const params = Object.assign(pickBy(colAttribs), {[ServerParams.REQUEST]: JSON.stringify(tableRequest)});
    return doJsonRequest(ServerParams.ADD_OR_UPDATE_COLUMN, params);
}

/**
 * delete a column from the table backed by this tableRequest
 * @param {TableRequest} tableRequest is a table request params object
 * @param cname      column name to delete
 * @returns {Promise.<number>}
 */
export function deleteColumn(tableRequest, cname) {

    const params = {[ServerParams.REQUEST]: JSON.stringify(tableRequest), cname};
    return doJsonRequest(ServerParams.DELETE_COLUMN, params);
}

/**
 * returns the table data for the given parameters
 * @param {Object} p  parameters object
 * @param {string[]} p.columnNames an array of column names
 * @param {TableRequest} p.request   location of the file on the server
 * @param {string} p.selectedRows   a comma-separated string of indices of the rows to get the data from
 * @return {Promise<TableModel>}
 */
export function selectedValues({columnNames, request, selectedRows}) {
    columnNames = Array.isArray(columnNames) ? columnNames.join() : String(columnNames);
    selectedRows = Array.isArray(selectedRows) ? selectedRows.join() : String(selectedRows);
    return doBigIntRequest(ServerParams.SELECTED_VALUES, {columnNames, request: JSON.stringify(request), selectedRows})
            .then((tableModel) => {
                return tableModel;
            });
}


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

    const params = {
        [DOWNLOAD_REQUEST]: JSON.stringify(dlRequest),
        [ServerParams.REQUEST]: JSON.stringify(searchRequest),
        [SELECTION_INFO]: selectionInfo
    };

    return submitJob(ServerParams.PACKAGE_REQUEST, params);
}


/**
 *
 * @param {ServerRequest} request
 * @return {Promise}
 */
export function getJsonData(request) {
    const paramList = [];
    paramList.push({name:ServerParams.REQUEST, value: request.toString()});

    return doBigIntRequest(ServerParams.JSON_DATA, paramList
    ).then((data) => {return data; });
}

/**
 * add this job to the background
 * @param {string} jobId background job id
 * @return {Promise}
 */
export function addBgJob(jobId) {
    const params = {[ServerParams.JOB_ID]: jobId};
    return doJsonRequest(ServerParams.ADD_JOB, params);
}

/**
 *
 * @param {string} jobId background job id
 * @return {Promise}
 */
export function removeBgJob(jobId) {
    const params = {[ServerParams.JOB_ID]: jobId};
    return doJsonRequest(ServerParams.REMOVE_JOB, params).then( (jobInfo) => {
        if (!jobInfo) {     // job is not on the server.. remove it locally
            dispatchBgJobInfo(updateSet(getJobInfo(jobId), 'jobInfo.monitored', false));
        }
    });
}

/**
 *
 * @param {string} jobId background job id
 * @return {Promise}
 */
export function cancel(jobId) {
    const params = {[ServerParams.JOB_ID]: jobId};
    return doJsonRequest(ServerParams.CANCEL, params);
}

export function uwsJobInfo(jobUrl, jobId) {
    const params = {[ServerParams.JOB_ID]: jobId,
                    [ServerParams.JOB_URL]: jobUrl};
    return doJsonRequest(ServerParams.UWS_JOB_INFO, params);
}

/**
 * @param {string} email
 * @return {Promise}
 */
export function setEmail(email) {
    const idList= Object.keys(getBackgroundJobs() || {});
    const paramList= idList.map( (id) => {
        return {name:ServerParams.ID, value: id};
    } );
    if(paramList.length > 0) {
        paramList.push({name:ServerParams.EMAIL, value:email});
        return doJsonRequest(ServerParams.SET_EMAIL, paramList);
    }
}

/**
 * resend email notification to all successfully completed background jobs.
 * @param {string} email
 * @return {Promise}
 */
export function resendEmail(email) {
    return doJsonRequest(ServerParams.RESEND_EMAIL, {[ServerParams.EMAIL]: email});
}

/**
 *
 * @param {string} channel
 * @param {string} desc
 * @param {string} data
 * @return {Promise}
 */
export function reportUserAction(channel, desc, data) {
    const paramList = [];
    paramList.push({name: ServerParams.CHANNEL_ID, value: channel});
    paramList.push({name: ServerParams.DATA, value: data});
    paramList.push({name: ServerParams.DESC, value: desc});
    return doJsonRequest(ServerParams.REPORT_USER_ACTION, paramList
    ).then( () => true);
}

export function getCapabilities(url) {
    return doJsonRequest(ServerParams.GET_CAPABILITIES, {URL:url});
}


/**
 *
 * @param jobId
 * @param fname
 * @param dataSource
 * @param {array} attributes and array of ScriptAttributes
 * @return {Promise}
 */
export function createDownloadScript(jobId, fname, dataSource, attributes) {
    const attrAry= Array.isArray(attributes) ? attributes : [attributes];
    const paramList= attrAry.map( (v='') => ({name:ServerParams.ATTRIBUTE, value: v.toString()}) );
    paramList.push({name: ServerParams.JOB_ID, value: jobId});
    paramList.push({name: ServerParams.FILE, value: fname});
    paramList.push({name: ServerParams.SOURCE, value: dataSource});
    return doJsonRequest(ServerParams.CREATE_DOWNLOAD_SCRIPT, paramList);
}

/**
 * Download the download script to the user's computer.
 * @param {Object} p
 * @param {string} p.packageID
 * @param {string} p.type
 * @param {string} p.fname
 * @param {string} p.dataSource
 */
export function getDownloadScript({packageID, type, fname, dataSource}) {
    const url = encodeUrl(getCmdSrvSyncURL(), {packageID, type, fname, dataSource});
    if (url) download(url);
}

/**
 * This function add some important meta info needed by the server to process this request.
 * These meta are system-only info and should not need to be known outside of this app.
 * @param {TableRequest} tableRequest
 * @param {object} defaults
 */
function setupNewRequest(tableRequest, defaults) {
    const newRequest = Object.assign(defaults, cloneDeep(tableRequest));

    const tableModel = getTblById(getTblId(newRequest));
    if (tableModel) {
        // pass along its resultSetID
        const resultSetID = getResultSetID(tableModel.tbl_id);
        resultSetID && setResultSetID(newRequest, resultSetID);
        // pass along its resultSetRequest
        const resultSetRequest = getResultSetRequest(tableModel.tbl_id);
        resultSetRequest && setResultSetRequest(newRequest, resultSetRequest);
        // pass along selectInfo
        tableModel.selectInfo && setSelectInfo(newRequest, tableModel.selectInfo);
    }
    return newRequest;
}