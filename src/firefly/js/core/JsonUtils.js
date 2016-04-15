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
import {debounce, get, has, omitBy, isUndefined, isString} from 'lodash';

//var http= require('http');

//const TIMEOUT = 10 * 60 * 1000;  // 10 min
const DEF_BASE_URL = getRootURL() + 'sticky/CmdSrv';
const DEF_PATH = getRootPath() + 'sticky/CmdSrv';


function preparePostParamList(cmd,paramList) {
    if (Array.isArray(paramList)) {
        const initObj= cmd ? {name: ServerParams.COMMAND, value: cmd} : {};
         return paramList.reduce( (rval, entry) => {
                    if (entry.name) rval[entry.name]= get(entry, 'value','');
                    return rval;
                }, initObj);
    }
    else {
        return Object.assign({},paramList,{[ServerParams.COMMAND]:cmd});
    }
}



const makeURL= function(baseUrl, cmd, paramList, isJsonp= false) {
    if (Array.isArray(paramList)) {
        if (cmd) paramList.push({name: ServerParams.COMMAND, value: cmd});
        if (isJsonp) paramList.push({name: ServerParams.DO_JSONP, value: 'true'});
    }
    else {
        var add= {};
        if (cmd) add[ServerParams.COMMAND]= cmd;
        if (isJsonp) add[ServerParams.DO_JSONP]= 'true';
        paramList= Object.assign({},paramList,add);
    }
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
 * @param doPost
 */
export const jsonRequest= function(baseUrl, cmd, paramList, doPost) {
    const options= {method: doPost?'POST':'GET'};
    var url;
    if (doPost) {
        url= encodeServerUrl(baseUrl,{});
        options.params= preparePostParamList(cmd,paramList);
    }
    else {
        url = makeURL(baseUrl, cmd, paramList);
    }


    return new Promise(function(resolve, reject) {
        fetchUrl(url,options ).then( (response) => {
            response.json().then( (result) => {
                if (result && result[0]) {
                    if (result[0] && result[0].success && result[0].success !== 'false' && result[0].data) {
                        resolve(result[0].data);

                    }
                    else if (result[0] && get(result,'0.error')){//result[0].error) {
                        reject(new Error(result[0].error));
                    } else {
                        reject(new Error(`Unreconized result: ${result}`));
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
export const doJsonRequest= function(cmd, paramList, doPost=false) {
    return jsonRequest(DEF_PATH, cmd, paramList, doPost);
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

