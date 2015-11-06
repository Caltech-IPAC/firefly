/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

/*eslint prefer-template:0 */
import { getRootURL, getRootPath, getHost, getPort } from '../util/BrowserUtil.js';
import { encodeServerUrl } from '../util/WebUtil.js';
import {ServerParams} from '../data/ServerParams.js';
import {fetchUrl} from '../util/WebUtil.js';

//var http= require('http');

//const TIMEOUT = 10 * 60 * 1000;  // 10 min
const DEF_BASE_URL = getRootURL() + 'sticky/CmdSrv';
const DEF_PATH = getRootPath() + 'sticky/CmdSrv';

const makeURL= function(baseUrl, cmd, paramList, isJsonp) {
    if (cmd) paramList.push({name: ServerParams.COMMAND, value: cmd});
    if (isJsonp) paramList.push({name: ServerParams.DO_JSONP, value: 'true'});
    return encodeServerUrl(baseUrl, paramList);
};

export const jsonpRequest= function(baseUrl, cmd, paramList) {//TODO - convert
    var url = makeURL(baseUrl, cmd, paramList, true);
    // TODO: use the jsonp module here by call the network
};

export const defaultJsonpRequest= function(cmd, paramList, cb) {
    jsonpRequest(DEF_BASE_URL, cmd, paramList, cb);
};

/**
 *
 * @param baseUrl
 * @param cmd
 * @param paramList
 */
export const jsonRequest= function(baseUrl, cmd, paramList) {
    var url = makeURL(baseUrl, cmd, paramList, false);

    return new Promise(function(resolve, reject) {

        fetchUrl(url).then( (response) => {
            response.json().then( (result) => {
                if (result[0].success) {
                    if (result[0].success === 'true') {
                        resolve(result[0].data);
                    } else {
                        if (result[0].error) {
                            reject(new Error(result[0].error));
                        } else {
                            reject(new Error(`Unknown failure: ${result}`));
                        }
                    }
                } else {
                    reject(new Error(`Unreconized result: ${result}`));
                }
            });
        }).catch(function(err) {
            reject(err);
        });
    });
};

export const defaultJsonRequest= function(cmd, paramList) {
    return jsonRequest(DEF_PATH, cmd, paramList);
};

export const doService= function(doJsonP, cmd, paramList) {
    if (doJsonP) {
        return defaultJsonpRequest(cmd, paramList);
    } else {
        return defaultJsonRequest(cmd,paramList);
    }
};

export const doSimpleService= function(doJsonP, cmd, asyncCB) {
    doService(doJsonP, cmd, [], asyncCB, (s) => s);
};

