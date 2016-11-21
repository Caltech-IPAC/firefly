/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

import {get} from 'lodash';

import {ServerParams} from '../data/ServerParams.js';
import {doService} from '../core/JsonUtils.js';
import {BackgroundStatus} from '../core/background/BackgroundStatus.js';
import {MAX_ROW} from '../tables/TableUtil.js';
import {getBgEmail} from '../core/background/BackgroundUtil.js';

import Enum from 'enum';

export const DownloadProgress= new Enum(['STARTING', 'WORKING', 'DONE', 'UNKNOWN', 'FAIL']);
export const ScriptAttributes= new Enum(['URLsOnly', 'Unzip', 'Ditto', 'Curl', 'Wget', 'RemoveZip']);

import {getTblById} from '../tables/TableUtil.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {getBackgroundJobs} from '../core/background/BackgroundUtil.js';

const DOWNLOAD_REQUEST = 'downloadRequest';
const SELECTION_INFO = 'selectionInfo';

const doJsonP= function() {
    return false;
};

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

    return doService(doJsonP(), ServerParams.TABLE_SEARCH, params)
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
 * returns the values of the data from the given parameters
 * @param {Object} p  parameters object
 * @param {string} p.columnName name of the column
 * @param {string} p.filePath   location of the file on the server
 * @param {string} p.selectedRows   a comma-separated string of indices of the rows to get the data from
 * @return {Promise}
 */
export const selectedValues = function({columnName, filePath, selectedRows}) {
    return doService(doJsonP(), ServerParams.SELECTED_VALUES, {columnName, filePath, selectedRows})
            .then((data) => {
                // JsonUtil may not interpret array values correctly due to error-checking.
                // returning array as a prop 'values' inside an object instead.
                return get(data, 'values');
            });
};


/**
 *
 * @param {TableRequest} request
 * @return {Promise}
 */
export const jsonSearch = function(request) {
    const params = {
        [ServerParams.REQUEST]: JSON.stringify(request),
    };
    return doService(doJsonP(), ServerParams.JSON_SEARCH, params);
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

    return doService(doJsonP(), ServerParams.PACKAGE_REQUEST, params);
}


/**
 *
 * @param {ServerRequest} request
 * @return {Promise}
 */
export const getJsonData = function(request) {
    var paramList = [];
    paramList.push({name:ServerParams.REQUEST, value: request.toString()});

    return doService(doJsonP(), ServerParams.JSON_DATA, paramList
    ).then((data) => {return data; });
};

/**
 * TODO: this functions requires FileStatus to be ported first
 * @param {string} filePath
 * @return {Promise}
 */
export const getFileStatus= function(filePath) {
    var paramList = [];
    paramList.push({name:ServerParams.SOURCE, value:filePath});

    //todo port FileStatus then uncoment
    //return doService(doJsonP(), ServerParams.CHK_FILE_STATUS, paramList
    //).then((data) => {return FileStatus.parse(data); });

    //todo port FileStatus then delete the following two lines
    return doService(doJsonP(), ServerParams.CHK_FILE_STATUS, paramList
    ).then( () => false );
};

/**
 *
 * @param {ServerRequest} request
 * @param {ServerRequest} clientRequest
 * @param {number} waitMillis
 * @return {Promise}
 */
export const submitBackgroundSearch= function(request, clientRequest, waitMillis) {
    var paramList = [];
    paramList.push({name: ServerParams.REQUEST, value: request.toString()});
    if (clientRequest !== null) {
        paramList.push({name: ServerParams.CLIENT_REQUEST, value: clientRequest.toString()});
    }
    paramList.push({name: ServerParams.WAIT_MILS, value: `${waitMillis}`});

    return doService(doJsonP(), ServerParams.SUB_BACKGROUND_SEARCH, paramList
    ).then((data) => {
               return BackgroundStatus.parse(data);
           });
};

/**
 *
 * @param {string} id background id
 * @param {boolean} polling true if polling
 * @return {Promise}
 */
export const getStatus= function(id, polling) {
    var paramList = [];
    paramList.push({name: ServerParams.ID, value: id});
    paramList.push({name: ServerParams.POLLING, value: `${polling}`});
    return doService(doJsonP(), ServerParams.GET_STATUS, paramList
    ).then((data) => {return BackgroundStatus.parse(data); });
};

/**
 *
 * @param {string} id background id
 * @return {Promise}
 */
export function removeBgJob(id) {
    const params = {[ServerParams.ID]: id};
    return doService(doJsonP(), ServerParams.REMOVE_JOB, params
    ).then( () => true);
};

/**
 *
 * @param {string} id background id
 * @return {Promise}
 */
export const cancel= function(id) {
    var paramList = [];
    paramList.push({name: ServerParams.ID, value: id});
    return doService(doJsonP(), ServerParams.CANCEL, paramList
    ).then( () => true);
};

/**
 *
 * @param {string} id background id
 * @return {Promise}
 */
export const addIDToPushCriteria= function(id) {
    var paramList = [];
    paramList.push({name: ServerParams.ID, value: id});
    return doService(doJsonP(), ServerParams.ADD_ID_TO_CRITERIA, paramList
    ).then( () => true);
};

/**
 *
 * @param {string} id background id
 * @return {Promise}
 */
export const cleanup= function(id) {
    var paramList = [];
    paramList.push({name: ServerParams.ID, value: id});
    return doService(doJsonP(), ServerParams.CLEAN_UP, paramList
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
    return doService(doJsonP(), ServerParams.DOWNLOAD_PROGRESS, paramList
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
        return doService(doJsonP(), ServerParams.SET_EMAIL, paramList
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
    return doService(doJsonP(), ServerParams.SET_ATTR, paramList
    ).then( () => true);
};

/**
 *
 * @param {string} id
 * @return {Promise}
 */
export const getEmail= function(id) {
    var paramList = [];
    paramList.push({name: ServerParams.ID, value: id});
    return doService(doJsonP(), ServerParams.GET_EMAIL, paramList
    ).then((data) => data );
};

/**
 * resend email notification to all successfully completed background jobs.
 * @param {string} email
 * @return {Promise}
 */
export const resendEmail= function(email) {
    return doService(doJsonP(), ServerParams.RESEND_EMAIL, {[ServerParams.EMAIL]: email}
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
    return doService(doJsonP(), ServerParams.CLEAR_PUSH_ENTRY, paramList
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
    return doService(doJsonP(), ServerParams.REPORT_USER_ACTION, paramList
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
    var paramList= attrAry.map( (attribute) => {
        return {name:ServerParams.ATTRIBUTE, value: attribute.toString()};
    } );
    paramList.push({name: ServerParams.ID, value: id});
    paramList.push({name: ServerParams.FILE, value: fname});
    paramList.push({name: ServerParams.SOURCE, value: dataSource});
    return doService(doJsonP(), ServerParams.CREATE_DOWNLOAD_SCRIPT, paramList);
};

/**
 *
 * @param filePath
 * @param rowsAry
 * @param colName
 * @return {Promise}
 */
export const getDataFileValues= function(filePath, rowsAry, colName) {
    var paramList = [];
    var rStr= JSON.stringify(rowsAry);
    rStr= rStr.substring(1,rStr.length-1);
    paramList.push({name: ServerParams.SOURCE, value: filePath});
    paramList.push({name: ServerParams.ROWS, value: rStr});
    paramList.push({name: ServerParams.COL_NAME, value: colName});
    return doService(doJsonP(), ServerParams.GET_DATA_FILE_VALUES, paramList
    ).then((data) => data.split(', '));
};


