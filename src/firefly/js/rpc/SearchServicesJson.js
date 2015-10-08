/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

//import {application,NetworkMode} from "../core/Application.js";
import {ServerParams} from '../data/ServerParams.js';
import {RawDataSet} from '../data/table/RawDataSet.js';
import {doService} from '../core/JsonUtils.js';
import {BackgroundStatus} from '../core/background/BackgroundStatus.js';

import Enum from 'enum';

export const DownloadProgress= new Enum(['STARTING', 'WORKING', 'DONE', 'UNKNOWN', 'FAIL']);
export const ScriptAttributes= new Enum(['URLsOnly', 'Unzip', 'Ditto', 'Curl', 'Wget', 'RemoveZip']);

const doJsonP= function() {
    return false;
    //return application.networkMode===NetworkMode.JSON;
};

//TODO: convert FileStatus
//TODO: convert BackgroundSTAtus

/**
 *
 * @param {ServerRequest} request
 * @return {Promise}
 */
export const getRawDataSet= function(request) {
    var paramList = [];
    paramList.push({name:ServerParams.REQUEST, value: request.toString()});

    return doService(doJsonP(), ServerParams.RAW_DATA_SET, paramList
    ).then(data => {return RawDataSet.parse(data); });
};

/**
 *
 * @param {ServerRequest} request
 * @return {Promise}
 */
export const getJsonData = function(request) {
    var paramList = [];
    paramList.push({name:ServerParams.REQUEST, value: request.toString()});

    return doService(doJsonP(), ServerParams.JSON_DATA, paramList
    ).then(data => {return JSON.parse(data); });
};

/**
 *
 * @param filePath
 * @return {Promise}
 */
export const getFileStatus= function(filePath) {
    var paramList = [];
    paramList.push({name:ServerParams.SOURCE, value:filePath});

    return doService(doJsonP(), ServerParams.CHK_FILE_STATUS, paramList
    ).then(data => {return FileStatus.parse(data); });
};

/**
 *
 * @param request
 * @param clientRequest
 * @param waitMillis
 * @return {Promise}
 */
export const submitBackgroundSearch= function(request, clientRequest, waitMillis) {
    var paramList = [];
    paramList.push({name: ServerParams.REQUEST, value: request.toString()});
    if (clientRequest !== null) {
        paramList.push({name: ServerParams.CLIENT_REQUEST, value: clientRequest.toString()});
    }
    paramList.push({name: ServerParams.WAIT_MILS, value: waitMillis + ''});

    return doService(doJsonP(), ServerParams.SUB_BACKGROUND_SEARCH, paramList
    ).then(data => {
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
    paramList.push({name: ServerParams.POLLING, value: polling+''});
    return doService(doJsonP(), ServerParams.GET_STATUS, paramList
    ).then(data => {return BackgroundStatus.parse(data); });
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
    ).then(data => {return DownloadProgress.get(data); });
};

/**
 *
 * @param {string} filePath
 * @return {Promise}
 */
export const getEnumValues= function(filePath) {
    var paramList = [];
    paramList.push({name: ServerParams.SOURCE, value: filePath});
    return doService(doJsonP(), ServerParams.GET_ENUM_VALUES, paramList
    ).then(data => {return RawDataSet.parse(data); });
};


/**
 *
 * @param {array|string} ids - one id or an array of ids
 * @param {string} email
 * @return {Promise}
 */
export const setEmail= function(ids, email) {
    var idList=  Array.isArray(ids) ? ids : [ids];
    var paramList= idList.map( id => {
        return {name:ServerParams.ID, value: id};
    } );
    paramList.push({name:ServerParams.EMAIL, value:email});
    return doService(doJsonP(), ServerParams.SET_EMAIL, paramList
    ).then( () => true);
};

/**
 *
 * @param {array|string} ids one id or an array of ids
 * @param {JobAttributes} job attribute
 * @return {Promise}
 */
export const setAttribute= function(ids, attribute) {
    var idList=  Array.isArray(ids) ? ids : [ids];
    var paramList= idList.map( id => {
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
    ).then(data => data );
};

/**
 *
 * @param {array|string} ids - one id or an array of ids
 * @param {string} email
 * @return {Promise}
 */
export const resendEmail= function(ids, email) {
    var idList=  Array.isArray(ids) ? ids : [ids];
    var paramList= idList.map( id => {
        return {name:ServerParams.ID, value: id};
    } );
    paramList.push({name:ServerParams.EMAIL, value:email});
    return doService(doJsonP(), ServerParams.RESEND_EMAIL, paramList
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
    paramList.push({name: ServerParams.IDX, value: idx+''});
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
    var paramList= attrAry.map( attribute => {
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
    ).then(data => data.split(', '));
};


