/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {ServerParams} from '../data/ServerParams.js';
import {doJsonRequest} from '../core/JsonUtils.js';


/**
 * tableRequest will be sent to the server as a json string.
 * @returns {Promise}
 * @public
 * @func doFetchTable
 * @memberof firefly.util.table
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
 * @public
 * @func doFetchTable
 * @memberof firefly.util.table
 */
export function init({spaName}={}) {
    return doJsonRequest(ServerParams.INIT_APP, {spaName});
}
