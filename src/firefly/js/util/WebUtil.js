/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*global __MODULE_NAME__*/

import Enum from 'enum';
import update from 'react-addons-update';
import {get, set, has, omit, isObject, union, isFunction, isEqual,  isNil, last, isObjectLike} from 'lodash';
import { getRootURL } from './BrowserUtil.js';
import {getWsConnId, getWsChannel} from '../core/messaging/WebSocketClient.js';
import {getDownloadProgress, DownloadProgress} from '../rpc/SearchServicesJson.js';

const  MEG          = 1048576;
const GIG          = 1048576 * 1024;
const MEG_TENTH    = MEG / 10;
const GIG_HUNDREDTH= GIG / 100;
const K            = 1024;

export const WS_CHANNEL_HD = 'FF-channel';
export const WS_CONNID_HD  = 'FF-connID';
export const ParamType= new Enum(['POUND', 'QUESTION_MARK']);


export function getModuleName() {
    return (typeof __MODULE_NAME__ === 'undefined') ? undefined : __MODULE_NAME__;
}

/**
 * Returns a string where all characters that are not valid for a complete URL have been escaped.
 * Also, it will do URL rewriting for session tracking if necessary.
 * Fires SESSION_MISMATCH if the seesion ID on the client is different from the one on the server.
 *
 * @param url    this could be a full or partial url.  Delimiter characters will be preserved.
 * @param {array|Object} params parameters to be appended to the url.  These parameters may contain
 *               delimiter characters.  Unlike url, delimiter characters will be encoded as well.
 *               if the parameters are a array then it should be objects {name:string,value:string} otherwise
 *               it can be an object literal
 * @return {string} encoded url
 */
export const encodeUrl= function(url, params) {

    var rval = url.trim();
    if ( !rval.toLowerCase().startsWith('http') ) {
        rval = getRootURL() + rval;
    }
    if (!params) return rval;

    var parts = url.split('?');
    var baseUrl = parts[0];
    var queryStr= encodeParams(params);

    return encodeURI(baseUrl) + (queryStr.length ? '?' : '') + queryStr;
};

/**
 * convert a params object to an encoded url fragment.
 * this function supports nested object.  if the value of a param is an object
 * or an array of {name, value}, it will encode the child, and then encode the parent as well.
 * @param {Object|Object[]} params key/value object or an array of {name,value}.
 * @returns {*}
 */
export function encodeParams(params) {
    if (Array.isArray(params)) {
        params = params.reduce( (rval, val) => {
            const key = get(val, 'name');
            key && (rval[key] = get(val, 'value'));
            return rval;
        }, {});
    }

    return Object.keys(params).reduce((rval, key) => {
        key = encodeURIComponent(key.trim());
        var val = get(params, key, '');
        rval = rval.length ? rval + '&' : rval;
        if (typeof val === 'object') {
            return rval + key + '=' + encodeURIComponent(encodeParams(val));
        } else {
            return rval + key + '=' + encodeURIComponent(val);
        }
    },'');
}

/**
 * convert a queryStr into an Object.  This is an inverse of encodeParams.
 * It support Object whose value is another Object.
 * @param queryStr
 * @returns {*}
 */
export function decodeParams(queryStr) {
    const params = queryStr.replace(/^\?/, '').split('&');
    return params.reduce( (rval, param) => {
        const parts = param.split('=').map((s) => s.trim());
        var val = decodeURIComponent(get(parts, [1], ''));
        if (val.includes('&')) {
            val = decodeParams(val);
        }
        if (isBooleanString(val)) {
            val = toBoolean(val);
        }
        rval[parts[0]] = val;
        return rval;
    }, {});
}


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
    return encodeUrl(url, params);
};

/**
 * A wrapper for the underlying window.fetch function.
 * see https://github.com/github/fetch for usage.
 * see https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API for current status on the API
 * see https://fetch.spec.whatwg.org/ for official standard
 *
 * This function applies default behaviors before fetching.
 *
 * @param {string} url the URL to connect
 * @param {Object} options
 * @param {Object} options.params a parameter map to send along with the request.  It does not need to
 *                                  be encoded.  Base on the method used, it will be handled internally.
 * @param {string} options.method can be one of get, post, or multipart
 *                                when 'multipart', it will post with 'multipart/form-data' encoding.
 * @return {Promise} a promise of the response when successful, or reject with an Error.
 */
export function fetchUrl(url, options, doValidation= true) {

    if (!url) return;

    // define defaults request options
    options = options || {};
    const req = { method: 'get',
            mode: 'cors',
            credentials: 'include',
            cache: 'default'
        };
    options = Object.assign(req, options);

    const headers = {
        [WS_CHANNEL_HD]: getWsChannel(),
        [WS_CONNID_HD]: getWsConnId()
    };
    options.headers = Object.assign(headers, options.headers);

    if (options.params) {
        if (options.method.toLowerCase() === 'get') {
            url = encodeUrl(url, options.params);
        } else {
            url = encodeUrl(url);
            if (!options.body) {
                // if 'post' but, body is not provided, add the parameters into the body.
                if (options.method.toLowerCase() === 'post') {
                    options.headers['Content-type'] = 'application/x-www-form-urlencoded';
                    options.body = Object.keys(options.params).map((key) => {
                                        return encodeURIComponent(key) + '=' + encodeURIComponent(options.params[key]);
                                    }).join('&');
                } else if (options.method.toLowerCase() === 'multipart') {
                    options.method = 'post';
                    var data = new FormData();
                    Object.keys(options.params).forEach( (key) => {
                        data.append(key, options.params[key]);
                    });
                    options.body = data;
                }
                Reflect.deleteProperty(options, 'params');
            }
        }
    }

    // do the actually fetch, then return a promise.
    return fetch(url, options)
        .then( (response) => {
            if (!doValidation) return response;
            if (response.ok) {
                return response; 
            } else {
                return new Error(`${url} failed with status: ${response}.statusText`);
            }
        }).catch( (error) => {
            return new Error(`Request failed: ${url}`, error);
        });
}

export function logError(...message) {
    if (message) {
        message.forEach( (m) => console.log(has(m,'stack') ? m.stack : m) );
    }
}
export function logErrorWithPrefix(prefix, ...message) {
    if (message) {
        message.forEach( (m,idx) => {
            if (idx===0) {
                if (m.stack) {
                    console.log(prefix);
                    console.log(m);
                }
                else {
                    console.log(`${prefix} ${m}`);
                }
            }
            else {
                console.log(m.stack ? m.stack : m);
            }
        } );
    }
}

/**
 * @param {string} url  the url to download.  It should be based on AnyFileDownload
 * @param {number} [numTries=1000]  number of time to check for progress until giving up
 * @returns {Promise}  resolve is called on DONE and reject when FAIL.
 */
export function downloadWithProgress(url, numTries=1000) {
    return new Promise(function(resolve, reject) {
        const {search} = parseUrl(url);
        var cnt = 0;
        const doIt = () => {
            const interval = Math.min(5000, Math.pow(2, 2*cnt/10)*1000);  //gradually increase between 1 and 5 secs
            console.log('Interval: ' + interval);
            setTimeout(function () {
                cnt++;
                getDownloadProgress(search).then((v) => {
                    if (DownloadProgress.DONE.is(v)) {
                        resolve(v);
                    } else if (DownloadProgress.FAIL.is(v)) {
                        reject(v);
                    } else {
                        if (cnt < numTries) {
                            doIt();
                        } else {
                            reject(`Number of tries(${numTries}) exceeded without results`);
                        }
                    }
                });
            }, interval);
        };
        doIt();
        download(url);
    });
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

export function parseUrl(url) {
    const parser = document.createElement('a');
    const pathAry = [];
    parser.href = url;

    // Convert query string to object
    const searchObject = decodeParams(parser.search);

    // Convert path string to object
    const paths = parser.pathname.split('/');
    paths.forEach((v) => {
        v.split(';').forEach( (pp, idx) => {
            if (idx > 0) {
                pp.split(',').forEach( (kvp) => {
                    const kv = kvp.split('=').map((s) => s.trim()).map((s) => decodeURIComponent(s));
                    pathAry[idx-1] = Object.assign(pathAry[idx-1] || {}, {path: v, [kv[0]]: kv[1]});
                    }
                );
            }
        });
    });
    const p = last(paths);
    const filename = p.includes(';') ? p.split(';')[0] : p;

    return {
        protocol: parser.protocol,
        host: parser.host,
        hostname: parser.hostname,
        port: parser.port,
        path: parser.pathname,
        search: parser.search.replace(/^\?/, ''),
        hash: parser.hash,
        searchObject,
        filename,
        pathAry
    };
}
export function getSizeAsString(size) {
    var  kStr= 'K';
    var  mStr= 'M';
    var  gStr= 'G';

    var retval;
    if (size > 0 && size < (1*MEG)) {
        retval= ((size / K) + 1) + kStr;
    }
    else if (size >= (1*MEG) && size <  (2*GIG) ) {
        var megs = Math.round(size / MEG);
        var  remain=  Math.round(size % MEG);
        var decimal =  Math.round(remain / MEG_TENTH);
        retval= megs +'.'+ decimal + mStr;
    }
    else if (size >= (2*GIG) ) {
        var  gigs =  Math.round(size / GIG);
        var remain=  Math.round(size % GIG);
        var decimal =  Math.round(remain / GIG_HUNDREDTH);
        retval= gigs +'.'+ decimal + gStr;
    }
    return retval;
}

function isRequiredUpdateObject(o) {
    return Array.isArray(o) || (o && o.constructor === Object.prototype.constructor);
}

/**
 * Diff the the two objects and prints differences to console
 * @param o1
 * @param o2
 * @param p the title
 * @param collapsed show the differences collapsed
 */
export function deepDiff(o1, o2, p, collapsed=false) {
    const notify = (status) => {
        console.warn(' Update %s', status);
        console.log('%cbefore', 'font-weight: bold', o1);
        console.log('%cafter ', 'font-weight: bold', o2);
    };
    if (!isEqual(o1, o2)) {
        collapsed ? console.groupCollapsed(p) : console.group(p);
        if ([o1, o2].every(isFunction)) {
            notify('avoidable?');
        } else if (![o1, o2].every(isRequiredUpdateObject)) {
            notify('required.');
        } else {
            const keys = union(Object.keys(o1), Object.keys(o2));
            for (const key of keys) {
                deepDiff(o1[key], o2[key], key);
            }
        }
        console.groupEnd();
    } else if (o1 !== o2) {
        collapsed ? console.groupCollapsed(p) : console.group(p);
        notify('avoidable!');
        if (isObject(o1) && isObject(o2)) {
            const keys = union(Object.keys(o1), Object.keys(o2));
            for (const key of keys) {
                deepDiff(o1[key], o2[key], key);
            }
        }
        console.groupEnd();
    }
}
/*----------------------------< COOKIES ----------------------------*/
export function setCookie(name, value, options = {}) {
    var str = `${encodeURIComponent(name)}=${encodeURIComponent(value)}`;

    if (isNil(value)) options.maxage = -1;

    if (options.maxage) {
        options.expires = new Date(+new Date() + options.maxage);
    }

    if (options.path) str += '; path=' + options.path;
    if (options.domain) str += '; domain=' + options.domain;
    if (options.expires) str += '; expires=' + options.expires.toUTCString();
    if (options.secure) str += '; secure';

    document.cookie = str;
}

export function getCookie(name) {
    const cookies = parseCookies(document.cookie);
    return name ? cookies[name] : cookies;
}

function parseCookies(str) {
    var obj = {},
        pairs = str.split(/ *; */);

    if (!pairs[0]) return obj;

    pairs.forEach( (pair) => {
        pair = pair.split('=');
        obj[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1]);
    });
    return obj;
}
/*---------------------------- COOKIES >----------------------------*/

/*----------------------------< update ----------------------------*/
/**
 * This is a wrapper of React update's $set for use with deep object update.
 * *Syntax is similar to lodash set.
 * @param object (Object): The object to modify.
 * @param path (Array|string): The path of the property to set.
 * @param value (*): The value to set.
 */
export function updateSet(object, path, value) {
    if (!has(object, path)) {
        set(object, path);
    }
    const o = set({}, path, {$set: value});
    return update(object, o);
}

/**
 * This is a wrapper of React update's $merge for use with deep object update.
 * *Syntax is similar to as lodash set.
 * @param object (Object): The object to modify.
 * @param path (Array|string): The path of the property to merge.
 * @param value (*): The value to merge.
 */
export function updateMerge(object, path, value) {
    if (!has(object, path)) {
        set(object, path, {});
    }
    const o = set({}, path, {$merge: value});
    return update(object, o);
}

/**
 * This is a generic wrapper of React's update for use with deep object update.
 * Command can be on of:
 * {$push: array}, {$unshift: array}, {$splice: array of arrays}, {$set: any}, {$apply: function}
 * see React's update for details.
 * *Syntax is similar to as lodash set.
 * @param object (Object): The object to modify.
 * @param path (Array|string): The path of the property to apply the function to.
 * @param command (*): The command portion of react's update.
 */
export function updateWith(object, path, command) {
    const defVal = command.$merge ? {} : [];
    if (!has(object, path)) {
        set(object, path, defVal);
    }
    const o = set({}, path, command);
    return update(object, o);
}

/**
 * Delete a property from an object
 * Syntax is similar to as lodash set.
 * @param object (Object): The object to modify.
 * @param path (Array|string): The path of the container of the property to delete.
 * @param value (*): The property to delete.
 */
export function updateDelete(object, path, value) {
    const v = omit(get(object, path), value);
    return updateSet(object, path, v);
}

/**
 * Simple wrapper around Object.assign.  This can be used for our most common use case of Object.assign.  It is not
 * an attempt to replace it.  It is used with you want to copy and object an add new values into it.
 * @param obj object to clone
 * @param params an object to merge with the new object
 */
export const clone = (obj={},params={}) => Object.assign({},obj,params);

/*---------------------------- update >----------------------------*/

/**
 * return the boolean value of prop for the given object
 * @param object
 * @param prop
 * @param def
 * @returns {*}
 */
export function getBoolean(object, prop, def=undefined) {
    return toBoolean(object && object[prop], def);
}

/**
 * return true if val is boolean true, or 'true' case-insensitive
 * @param val the value to convert.
 * @param def return def if val is undefined
 * @returns {*}
 */
export function toBoolean(val, def=undefined) {
    return val === undefined ? def :
        typeof val === 'boolean'? val :
        String(val).toLowerCase() === 'true';
}

/**
 * return true if val is 'true' or 'fase' case-insensitive
 * @param val the string value to check.
 * @returns {*}
 */
export function isBooleanString(val) {
    const s = String(val).toLowerCase();
    return s === 'true' || s === 'false';
}

