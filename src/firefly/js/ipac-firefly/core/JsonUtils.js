/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * @author Trey Roby
 * Date: 3/5/12
 */

/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";

import { getRootURL, getRootPath, getHost, getPort } from "ipac-firefly/util/BrowserUtil.js";
import { encodeServerUrl } from "ipac-firefly/util/WebUtil.js";
import {ServerParams} from "ipac-firefly/data/ServerParams.js";
import http from "http";
//var http= require('http');

var Promise= require("es6-promise").Promise;

const TIMEOUT = 10 * 60 * 1000;  // 10 min
const DEF_BASE_URL = getRootURL() + "sticky/CmdSrv";
const DEF_PATH = getRootPath() + "sticky/CmdSrv";

const makeURL= function(baseUrl, cmd, paramList, isJsonp) {
    if (cmd) paramList.push({name: ServerParams.COMMAND, value: cmd});
    if (isJsonp) paramList.push({name: ServerParams.DO_JSONP, value: "true"});
    return encodeServerUrl(baseUrl, paramList);
}


export const defaultJsonpRequest= function(cmd, paramList, cb) {
    jsonpRequest(DEF_BASE_URL, cmd, paramList, cb);
}

export const jsonpRequest= function(baseUrl, cmd, paramList, cb) {//TODO - convert
    var url = makeURL(baseUrl, cmd, paramList, true);
    // TODO: use the jsonp module here
}


export const defaultJsonRequest= function(cmd, paramList) {
    return jsonRequest(DEF_PATH, cmd, paramList);
}

/**
 *
 * @param baseUrl
 * @param cmd
 * @param paramList
 * @param cb
 */
export const jsonRequest= function(baseUrl, cmd, paramList) {
    var url = makeURL(baseUrl, cmd, paramList, false);



    var workerPromise= new Promise(function(resolve, reject) {
        var options= {
            //method : 'GET',
            path : url,
            //headers : {},
            host : getHost(),
            port : getPort(),
        };
        http.get(options, function (res) {
            res.on('data', function (buf) {
                var result= JSON.parse(buf);
                if (result[0].success) {
                    resolve(result[0].data);
                }
                else {
                    reject(new Error('Could not parse: '+ buf));
                }
            });

            res.on('end', function () {
            });
            res.on('close', function (err) {
                reject(new Error(err? 'Error Code:' +err.code : "unknown"))
            });
        }.bind(this));

    }).then(function(buf) { return JSON.parse(buf); });

    return workerPromise;
}

export const doSimpleService= function(doJsonP, cmd, asyncCB) {
    doService(doJsonP, cmd, [], asyncCB, (s) => s);
}


export const doService= function(doJsonP, cmd, paramList) {
    if (doJsonP) {
        return defaultJsonpRequest(cmd, paramList);
    } else {
        return defaultJsonRequest(cmd,paramList)
    }
}


