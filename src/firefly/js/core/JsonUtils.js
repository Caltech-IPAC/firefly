/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {has} from 'lodash';
import {getRootURL} from '../util/BrowserUtil.js';
import {fetchUrl, toBoolean} from '../util/WebUtil.js';
import {ServerParams} from '../data/ServerParams.js';

export const DEF_BASE_URL = getRootURL() + 'sticky/CmdSrv';

/**
 *
 * @param {string} baseUrl
 * @param {string} cmd
 * @param paramList
 * @param {boolean} doPost
 * @return {Promise} a promise with the results
 */
function jsonRequest(baseUrl, cmd, paramList, doPost) {
    const options= {method: doPost?'POST':'GET'};
    options.params = addParam(paramList, ServerParams.COMMAND, cmd);

    return new Promise(function(resolve, reject) {
        fetchUrl(baseUrl, options, false ).then( (response) => {
            if (!response.ok) {
                reject(new Error(`Error from Server for command ${cmd}: code: ${response.status}, text: ${response.statusText}`));
                return;
            }
            response.json().then( (result) => {
                if (has(result,'0.success')) {
                    if (toBoolean(result[0].success)) {
                        resolve(result[0].data ? result[0].data : result[0]);
                    }
                    else if (has(result,'0.error')){
                        reject(new Error(result[0].error));
                    } else {
                        reject(new Error(`Unrecognized result: ${result}`));
                    }
                }
                else { //this part did not use WebPlotResultSerializer for making the return result in VisServerCommands
                    resolve(result);

                }
            });
        }).catch(function(err) {
            reject(err);
        });
    });
}

/**
 * 
 * @param {string} cmd
 * @param paramList
 * @param {boolean} doPost
 * @return {Promise} a promise with the results
 */
export function doJsonRequest(cmd, paramList, doPost=true) {
    return jsonRequest(DEF_BASE_URL, cmd, paramList, doPost);
}

/**
 *
 * @param channelId
 * @param action
 */
export function dispatchRemoteAction(channelId, action) {
    const params= {
        [ServerParams.CHANNEL_ID]: channelId,
        [ServerParams.ACTION]: JSON.stringify(action) };
    return doJsonRequest(ServerParams.VIS_PUSH_ACTION, params);
}


/**
 * add the given name-value param into a new paramList.  If a param by a given name exists, it will be replaced.
 * if name is not given, the original paramList is returned.
 * @param {Object | Object[]} paramList
 * @param {string} name
 * @param {string} value
 */
function addParam(paramList, name, value) {
    if (!name) return paramList;
    if (Array.isArray(paramList)) {
        return paramList.filter((v) => v.name !== name).concat({name, value});
    } else {
        return Object.assign({}, paramList, {[name]: value});
    }
}