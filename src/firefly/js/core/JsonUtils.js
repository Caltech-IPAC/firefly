/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

/*eslint prefer-template:0 */
import {get, has} from 'lodash';
import { getRootURL} from '../util/BrowserUtil.js';
import { encodeUrl, toBoolean } from '../util/WebUtil.js';
import {ServerParams} from '../data/ServerParams.js';
import {fetchUrl} from '../util/WebUtil.js';

//var http= require('http');

//const TIMEOUT = 10 * 60 * 1000;  // 10 min
export const DEF_BASE_URL = getRootURL() + 'sticky/CmdSrv';


const makeURL= function(baseUrl, cmd, paramList, isJsonp= false) {
    paramList = cmd ? addParam(paramList, ServerParams.COMMAND, cmd) : paramList;
    paramList = isJsonp ? addParam(paramList, ServerParams.DO_JSONP, 'true') : paramList;
    return encodeUrl(baseUrl, paramList);
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
 * @param doPost
 */
export const jsonRequest= function(baseUrl, cmd, paramList, doPost) {
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
};

/**
 * 
 * @param cmd
 * @param paramList
 * @param doPost
 */
export const doJsonRequest= function(cmd, paramList, doPost=true) {
    return jsonRequest(DEF_BASE_URL, cmd, paramList, doPost);
};

export const doService= function(doJsonP, cmd, paramList) {
    if (doJsonP) {
        return defaultJsonpRequest(cmd, paramList);
    } else {
        return doJsonRequest(cmd,paramList);
    }
};

export const doSimpleService= function(doJsonP, cmd, asyncCB) {
    doService(doJsonP, cmd, [], asyncCB, (s) => s);
};

/**
 * add the given name-value param into a new paramList.  If a param by a given name exists, it will be replaced.
 * if name is not given, the original paramList is returned.
 * @param {Object, Object[]} paramList
 * @param {string} name
 * @param {string} value
 */
function addParam(paramList, name, value) {
    if (!name) return paramList;
    if (Array.isArray(paramList)) {
        return paramList.filter((v) => v.name !== name).concat({name, value});
    } else return Object.assign({}, paramList, {[name]: value});
}