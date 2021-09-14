/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {has} from 'lodash';
import {toBoolean} from '../util/WebUtil.js';
import {ServerParams} from '../data/ServerParams.js';
import {logger} from '../util/Logger.js';
import {fetchUrl} from '../util/fetch';
import {getCmdSrvURL} from '../util/WebUtil';
import jsonBigInt from 'json-bigint';


const JSONbigint = jsonBigInt({ useNativeBigInt: true });

/**
 *
 * @param {string} baseUrl
 * @param {string} cmd
 * @param paramList
 * @param {boolean} doPost
 * @param {boolean} useBigInt       // use BigInt supported json parser
 * @return {Promise} a promise with the results
 */
function jsonRequest(baseUrl, cmd, paramList, doPost, useBigInt) {
    const options = {method: doPost ? 'POST' : 'GET'};
    if (doPost) baseUrl+=`?${ServerParams.COMMAND}=${cmd}`;
    options.params = addParam(paramList, ServerParams.COMMAND, cmd);

    return new Promise(function (resolve, reject) {
        fetchUrl(baseUrl, options, false).then((response) => {
            if (!response.ok) {
                reject(new Error(`Error from Server for command ${cmd}: code: ${response.status}, text: ${response.statusText}`));
                return;
            }

            const handleResults = (result) => {
                if (has(result, '0.success')) {
                    if (toBoolean(result[0].success)) {
                        resolve(result[0].data ? result[0].data : result[0]);
                    } else if (has(result, '0.error')) {
                        reject(new Error(result[0].error));
                    } else {
                        reject(new Error(`Unrecognized result: ${result}`));
                    }
                } else { //this part did not use WebPlotResultSerializer for making the return result in VisServerCommands
                    resolve(result);
                }
            };

            if (useBigInt) {
                response.text().then((json) => {
                    const result = JSONbigint.parse(json);
                    handleResults(result);
                }).catch(function (err) {
                    reject(err);
                });

            } else {
                response.json().then((result) => {
                    handleResults(result);
                }).catch(function (err) {
                    reject(err);
                });
            }
        });
    });
}

/**
 * 
 * @param {string} cmd
 * @param paramList
 * @param {boolean} doPost
 * @param {boolean} useBigInt  // support BigInt in JSON.  default to false
 * @return {Promise} a promise with the results
 */
export function doJsonRequest(cmd, paramList, doPost=true, useBigInt=false) {
    return jsonRequest(getCmdSrvURL(), cmd, paramList, doPost, useBigInt);
}

/**
 *
 * @param {String} channelId
 * @param {Action} action
 */
export function dispatchRemoteAction(channelId, action) {
    const params= {
        [ServerParams.CHANNEL_ID]: channelId,
        [ServerParams.ACTION]: JSON.stringify(action) };
    logger.tag('dispatchRemote').debug(action);
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