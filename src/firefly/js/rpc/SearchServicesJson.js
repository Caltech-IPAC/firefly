/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */


import {get, set, pickBy, cloneDeep, has, isUndefined} from 'lodash';
import {ServerParams} from '../data/ServerParams.js';
import {doJsonRequest} from '../core/JsonUtils.js';
import {getBgEmail} from '../core/background/BackgroundUtil.js';
import {encodeUrl, getModuleName} from '../util/WebUtil.js';

import Enum from 'enum';
import {getTblById, getResultSetID, getResultSetRequest} from '../tables/TableUtil.js';
import {MAX_ROW, DataTagMeta, getTblId, setResultSetID, setResultSetRequest, setSelectInfo} from '../tables/TableRequestUtil.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {getBackgroundJobs} from '../core/background/BackgroundUtil.js';
import {getFireflySessionId} from '../Firefly.js';
import * as TblUtil from '../tables/TableUtil';
import {download} from '../util/fetch';
import {getCmdSrvURL} from '../util/WebUtil';

export const DownloadProgress= new Enum(['STARTING', 'WORKING', 'DONE', 'UNKNOWN', 'FAIL']);
export const ScriptAttributes= new Enum(['URLsOnly', 'Unzip', 'Ditto', 'Curl', 'Wget', 'RemoveZip']);


const DOWNLOAD_REQUEST = 'downloadRequest';
const SELECTION_INFO = 'selectionInfo';

//TODO: convert FileStatus


const doBigIntRequest = (cmd, params) => {
    return doJsonRequest(cmd, params, true, true);
};


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
        pageSize : MAX_ROW,
        ffSessionId: getFireflySessionId(),
    };
    
    tableRequest = setupNewRequest(tableRequest, def);

    const params = {
        [ServerParams.REQUEST]: JSON.stringify(tableRequest),
    };

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
    if (!dlRequest.Email && getBgEmail()) {
        dlRequest.Email = getBgEmail();
    }
    // insert DataTag if not present
    if (!get(searchRequest, DataTagMeta)) {
        set(searchRequest, DataTagMeta, `${getModuleName()}-${ServerParams.PACKAGE_REQUEST}`);
    }

    const params = {
        [DOWNLOAD_REQUEST]: JSON.stringify(dlRequest),
        [ServerParams.REQUEST]: JSON.stringify(searchRequest),
        [SELECTION_INFO]: selectionInfo
    };

    return doBigIntRequest(ServerParams.PACKAGE_REQUEST, params);
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
 *
 * @param {ServerRequest} request
 * @param {ServerRequest} clientRequest
 * @param {number} waitMillis
 * @return {Promise}
 */
export function submitBackgroundSearch(request, clientRequest, waitMillis) {
    if (getBgEmail()) {
        request = set(request, ['META_INFO', ServerParams.EMAIL], getBgEmail());
    }
    request.ffSessionId = request.ffSessionId ?? getFireflySessionId();

    // insert DataTag if not present
    if (!get(request, DataTagMeta)) {
        set(request, DataTagMeta, `${getModuleName()}-${request.id}`);
    }
    const params = {
        [ServerParams.REQUEST]: JSON.stringify(request),
        [ServerParams.WAIT_MILS]: String(waitMillis)
    };
    clientRequest && (params[ServerParams.CLIENT_REQUEST] = JSON.stringify(clientRequest));

    return doBigIntRequest(ServerParams.SUB_BACKGROUND_SEARCH, params);
}

/**
 * add this job to the background
 * @param {string} bgStatus background id
 * @return {Promise}
 */
export function addBgJob(bgStatus) {
    const params = {bgStatus: JSON.stringify(bgStatus)};
    return doJsonRequest(ServerParams.ADD_JOB, params).then( () => true);
}

/**
 *
 * @param {string} id background id
 * @return {Promise}
 */
export function removeBgJob(id) {
    const params = {[ServerParams.ID]: id};
    return doJsonRequest(ServerParams.REMOVE_JOB, params).then( () => true);
}

/**
 *
 * @param {string} id background id
 * @return {Promise}
 */
export function cancel(id) {
    const paramList = [];
    paramList.push({name: ServerParams.ID, value: id});
    return doJsonRequest(ServerParams.CANCEL, paramList
    ).then( () => true);
}

/**
 *
 * @param {string} fileKey
 * @return {Promise}
 */
export function getDownloadProgress(fileKey) {
    const paramList = [];
    paramList.push({name: ServerParams.FILE, value: fileKey});
    return doJsonRequest(ServerParams.DOWNLOAD_PROGRESS, paramList
    ).then((data) => {return DownloadProgress.get(data); });
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
        return doJsonRequest(ServerParams.SET_EMAIL, paramList
        ).then( () => true);
    }
}

/**
 *
 * @param {array|string} ids one id or an array of ids
 * @param {JobAttributes} attribute job attribute
 * @return {Promise}
 */
export function setAttribute(ids, attribute) {
    const idList=  Array.isArray(ids) ? ids : [ids];
    const paramList= idList.map( (id) => {
        return {name:ServerParams.ID, value: id};
    } );
    paramList.push({name:ServerParams.ATTRIBUTE, value:attribute.toString()});
    return doJsonRequest(ServerParams.SET_ATTR, paramList
    ).then( () => true);
}

/**
 * resend email notification to all successfully completed background jobs.
 * @param {string} email
 * @return {Promise}
 */
export function resendEmail(email) {
    return doJsonRequest(ServerParams.RESEND_EMAIL, {[ServerParams.EMAIL]: email}
    ).then( () => true);
}

/**
 *
 * @param {string} id
 * @param {number} idx
 * @return {Promise}
 */
export function clearPushEntry(id, idx ) {
    const paramList = [];
    paramList.push({name: ServerParams.ID, value: id});
    paramList.push({name: ServerParams.IDX, value: `${idx}`});
    return doJsonRequest(ServerParams.CLEAR_PUSH_ENTRY, paramList
    ).then( () => true);
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


/**
 *
 * @param id
 * @param fname
 * @param dataSource
 * @param {array} attributes and array of ScriptAttributes
 * @return {Promise}
 */
export function createDownloadScript(id, fname, dataSource, attributes) {
    const attrAry= Array.isArray(attributes) ? attributes : [attributes];
    const paramList= attrAry.map( (v='') => ({name:ServerParams.ATTRIBUTE, value: v.toString()}) );
    paramList.push({name: ServerParams.ID, value: id});
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
    const url = encodeUrl(getCmdSrvURL(), {packageID, type, fname, dataSource});
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