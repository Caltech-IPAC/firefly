/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isString} from 'lodash';
import {ServerParams} from '../data/ServerParams.js';
import {doJsonRequest} from '../core/JsonUtils.js';
import {WebPlotRequest} from '../visualize/WebPlotRequest';
import {fetchUrl} from '../util/fetch';
import {getCmdSrvSyncURL} from '../util/WebUtil';

const getUploadURL= () => `${getCmdSrvSyncURL()}?${ServerParams.COMMAND}=${ServerParams.UPLOAD}`;

/**
 * tableRequest will be sent to the server as a json string.
 * @returns {Promise}
 */
export function logout() {
    return doJsonRequest(ServerParams.LOG_OUT);
}

/**
 * This is called during Firefly.init to initiated server-client sync
 * @param {string} spaName spaName is the single-page-app that's currently running.
 *                         This is not the same as a webapp, as we support multiple 
 *                         SPAs running out of one webapp
 * @returns {Promise}
 */
export function notifyServerAppInit({spaName}={}) {
    return doJsonRequest(ServerParams.INIT_APP, {spaName});
}

export function getJsonProperty(property) {
    return doJsonRequest(ServerParams.JSON_PROPERTY, {[ServerParams.PROP]:property});
}


/**
 * Upload a URL, File, Blob, or WebPlotRequest to the server, using AnyFileUpload on the firefly server side.
 * Determine how to upload based on what is passed. It may be called with a URL, Blob, File, or WebPlotRequest
 * @param {WebPlotRequest|Blob|File|String} item - if string the interpret as a URL, if is an object and has WebPlotRequest function then
 * interpret as a WebPlotRequest, Otherwise interpret as a file or blob
 * @param {String|Boolean} fileAnalysis
 * @param {Object} params - any extra parameters to the upload
 * @return {Promise}
 */
export async function upload(item, fileAnalysis= false, params={}) {
    const fetchParam= item && buildUploadParam(item);
    if (!fetchParam) {
        const msg= item ? 'Did not recognize item to upload: must be URL (String), File, Blob, or WebPlotRequest' :
                          'item parameter not given';
        return Promise.reject(Error(msg));
    }
        // put the fetchParam at the end, if it is a file or blob, it has to be the last param due to AnyFileUpload limitation
    const r= await fetchUrl(getUploadURL(), {method: 'multipart', params:{...params,fileAnalysis,...fetchParam}});
    return parseUploadResults(await r.text());
}

/**
 * return the correct upload parameter based on the type that is passed
 * @param {WebPlotRequest|Blob|File|String} item - the parameter to evaluate
 * @return {Object|undefined} an object with the correct parameter to pass to the upload,
 * or undefined if it is not the correct parameter type
 */
function buildUploadParam(item) {
    if (isString(item)) return {URL: item};
    else if (WebPlotRequest.isWPR(item)) return {webPlotRequest: item.toString()};
    else if (item instanceof Blob)  return {file:item}; // handles blob or file
}

/**
 * parse an upload result
 * @param {String} text - the result text from the upload call
 * @return {{cacheKey: String, analysisResult: String, message: String, status: String}}
 */
export function parseUploadResults(text) {
    // TODO: refactor analysisResult to be an Object?
                     // text is in format ${status}::${message}::${message}::${cacheKey}::${analysisResult}
    const [status, message, cacheKey, anaResultPart, ...rest] = text.split('::');
                     // there are '::' in the analysisResults.. put it back
    const analysisResult= (rest.length > 0) ? `${anaResultPart}::${rest.join('::')}` : anaResultPart;
    return {status, message, cacheKey, analysisResult};
}

