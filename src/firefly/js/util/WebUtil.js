/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import isBlank from 'underscore.string/isBlank';
import { getRootURL } from './BrowserUtil.js';

export const ParamType= new Enum(['POUND', 'QUESTION_MARK']);

const saveAsIpacUrl = getRootURL() + 'servlet/SaveAsIpacTable';

/*global __MODULE_NAME__*/
export function getModuleName() {
    return __MODULE_NAME__;
}

/**
 * Returns a string where all characters that are not valid for a complete URL have been escaped.
 * Also, it will do URL rewriting for session tracking if necessary.
 * Fires SESSION_MISMATCH if the seesion ID on the client is different from the one on the server.
 *
 * @param url    this could be a full or partial url.  Delimiter characters will be preserved.
 * @param paramType  if the the parameters are for the server use QUESTION_MARK, if the client use POUND - TODO: make this optional
 * @param {array|Object} params parameters to be appended to the url.  These parameters may contain
 *               delimiter characters.  Unlike url, delimiter characters will be encoded as well.
 *               if the parameters are a array then it should be objects {name:string,value:string} otherwise
 *               it can be an object literal
 * @return {string} encoded url
 */
export const encodeUrl= function(url, paramType, params) {
    var paramChar= paramType===ParamType.QUESTION_MARK ? '?': '#';
    var parts = url.split('\\'+paramChar, 2);
    var baseUrl = parts[0];
    //var queryStr = encodeURI(parts.length===2 ? parts[1] : '');

    var paramAry;

    if (Array.isArray(params)) {
        paramAry= params;
    }
    else {
        paramAry= Object.keys(params).reduce( (ary,key) => {
            ary.push({name:key, value : params[key]});
            return ary;
        },[]);
    }

    var queryStr= paramAry.reduce((str,param,idx) => {
        if (param && param.name) {
            var key = encodeURI(param.name.trim());
            var valStr='';
            if (typeof(param.value) != 'undefined' && param.value != null) {
                valStr= String(param.value);
            }
            var val = valStr.length ? encodeURIComponent(valStr.trim()) : '';
            str += val.length ? key + '=' + val + (idx < paramAry.length-1 ? '&' : '') : key;
            return str;
        }
    },'');

    return encodeURI(baseUrl) + (queryStr.length ? paramChar + queryStr : '');
};


/**
 * Returns a string where all characters that are not valid for a complete URL have been escaped.
 * Also, it will do URL rewriting for session tracking if necessary.
 * Fires SESSION_MISMATCH if the seesion ID on the client is different from the one on the server.
 *
 * @param url    this could be a full or partial url.  Delimiter characters will be preserved.
 * @param params parameters to be appended to the url.  These parameters may contain
 *               delimiter characters.  Unlike url, delimiter characters will be encoded as well.
 * @return encoded url
 */
export const encodeServerUrl= function(url, params) {
    return encodeUrl(url, ParamType.QUESTION_MARK,params);
};




/**
 *
 * @param {ServerRequest} request
 * @return {string} encoded
 */
export const getTableSourceUrl= function(request) {
    request.setStartIndex(0);
    request.setPageSize(Number.MAX_SAFE_INTEGER);
    var source = { name : 'request', value : request.toString()};  //todo : i don't think I got this line right
    var filename = request.getParam('file_name');
    if (!filename) filename = request.getRequestId();
    var fn = { name: 'file_name', value : filename};
    return encodeServerUrl(saveAsIpacUrl, source, fn);
};



/**
 * A wrapper for the underlying window.fetch function.
 * see https://github.com/github/fetch for usage.
 * see https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API for current status on the API
 * see https://fetch.spec.whatwg.org/ for official standard

 * This function applies default behaviors before fetching.
 * options.params is a custom property used to carry a set of parameters.  It does not need to
 *                be encoded.  Base on the method used, it will be handled internally.
 *
 * @param url
 * @param options
 * @return a promise of the response when successful, or reject with an Error.
 */
export function fetchUrl(url, options) {

    if (!url) return;

    // define defaults request options
    options = options || {};
    const req = { method: 'GET',
            mode: 'cors',
            credentials: 'include',
            cache: 'default'
        };
    options = Object.assign(req, options);

    const headers = {};
    if (options.method.toUpperCase() === 'POST') {
        // add default content-type header when method is 'post'
        headers['Content-type'] = 'application/x-www-form-urlencoded; charset=UTF-8';
    }
    options.headers = Object.assign(headers, options.headers);

    if (options.params) {
        if (options.method.toUpperCase() === 'GET') {
            url = makeUrl(url, options.params);
        } else {
            url = makeUrl(url);
            if (!options.body) {
                // if 'post' but, body is not provided, add the parameters into the body.
                var data = new FormData();
                Object.keys(options.params).forEach( (key) => {
                    data.append(key, options.params[key]);
                });
                options.body = data;
                Reflect.deleteProperty(options, 'params');
            }
        }
    }

    // do the actually fetch, then return a promise.
    return fetch(url, options)
        .then( (response) => {
            if (response.ok) {
                return response;
            } else {
                return new Error(`${url} failed with status: ${response}.statusText`);
            }
        }).catch( (error) => {
            return new Error(`Request failed: ${url}`, error);
        });
}

function makeUrl(url, params) {
    var rval = url.trim();
    if ( !(rval.toLowerCase().startsWith('http') || rval.startsWith('/')) ) {
        rval = getRootURL() + rval;
    }
    if (!params) return rval;

    if (rval.indexOf('?') < 0) {
        rval += '?';
    }
    for(var key in params) {
        if(!rval.match('[?&]$')) {
            rval += '&';
        }
        rval += encodeURI(key);
        let val = params[key];
        if (!isBlank(val)) {
            rval += '=' + encodeURIComponent(val.toString().trim());
        }
    }
    return rval;
}


export function logError(...message) {
    if (message) {
        message.forEach( (m) => console.log(m.stack ? m.stack : m) );
    }
}


export function download(url) {
    var nullFrame = document.getElementById('null_frame');
    if (!nullFrame) {
        nullFrame = document.createElement('iframe');
        nullFrame.id = 'null_frame';
        nullFrame.style.display = 'none';
        nullFrame.style.width = '0px';
        nullFrame.style.height = '0px';
        document.body.appendChild(nullFrame);
    }
    nullFrame.src = url;
}