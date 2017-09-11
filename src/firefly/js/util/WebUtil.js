/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*global __MODULE_NAME__*/

import Enum from 'enum';
import shallowequal from 'shallowequal';
import {get, set, has, omit, isObject, union, isFunction, isEqual,  isNil,
        last, isPlainObject, forEach, isEmpty, find, omitBy} from 'lodash';

import {getRootURL} from './BrowserUtil.js';
import {getWsConnId, getWsChannel} from '../core/messaging/WebSocketClient.js';
import {getDownloadProgress, DownloadProgress} from '../rpc/SearchServicesJson.js';

// todo: we want to replace react-addons-update with immutability-helper. However there is some behavior difference with error
// todo: handling, I observed it when updateMerge is called from ChartsCntrl.js,reduceData,case CHART_DATA_FETCH, line 415
// todo: In this case can an exception is thrown. to reproduce: just do catalog search
// todo: import update from 'immutability-helper';
import update from 'react-addons-update';




const  MEG          = 1048576;
const GIG          = 1048576 * 1024;
const MEG_TENTH    = MEG / 10;
const GIG_HUNDREDTH= GIG / 100;
const K            = 1024;

export const REQUEST_WITH = 'X-Requested-With';
export const AJAX_REQUEST = 'XMLHttpRequest';
export const WS_CHANNEL_HD = 'FF-channel';
export const WS_CONNID_HD  = 'FF-connID';
export const ParamType= new Enum(['POUND', 'QUESTION_MARK']);


export function getModuleName() {
    return (typeof __MODULE_NAME__ === 'undefined') ? undefined : __MODULE_NAME__;
}


/**
 * load a js script by dynamicly adding a script tag.
 * @param scriptName
 * @return {Promise} when the script is loaded or failed to load
 */
export function loadScript(scriptName) {
    const loadPromise= new Promise(
        function(resolve, reject) {
            const head= document.getElementsByTagName('head')[0];
            const script= document.createElement('script');
            script.type= 'text/javascript';
            script.src= scriptName;
            head.appendChild(script);
            script.onload= (ev) => resolve(ev);
            script.onerror= (ev) => reject(ev);
        });
    return loadPromise;
}

/**
 * Create an image with a promise that resolves when the image is loaded
 * @param {string} src the source of the image typically a url or local image data
 * @return {Promise} a promised that will resolved with the loaded image object
 */
export function loadImage(src) {
    return new Promise( (resolve, reject) => {
        const im = new Image();
        im.src= src;
        im.onload= () => resolve(im);
        im.onerror= (ev) => reject(ev);
    });
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
 * returns an array of {name, value} pairs based on the given params object.
 * If the value in the params object is an array, it will be flatten into multiple
 * name-value pairs based on the same key.
 * @param params
 * @returns {string}
 */
export function toNameValuePairs(params) {
    if (isPlainObject(params)) {
        return Object.entries(params)
            .filter( ([key, val]) => key)       // remove empty params
            .reduce( (rval, [key, val]) => {
                if (Array.isArray(val)) {
                    return rval.concat(val.map( (v) => ({name:key, value:v}) ));
                } else return rval.concat({name:key, value:val});
            }, []);
    } else return params;
}

/**
 * convert a params object to an encoded url fragment.
 * this function supports nested object.  if the value of a param is an object
 * or an array of {name, value}, it will encode the child, and then encode the parent as well.
 * @param {Object|Object[]} params key/value object or an array of {name,value}.
 * @returns {*}
 */
export function encodeParams(params) {
    params = toNameValuePairs(params);  // convert to name-value pairs if params is a plain object.

    return params.filter( (p) => has(p, 'name') )       // only take name-value pair.
        .map(({name, value}) => [name.trim(), isPlainObject(value) ? JSON.stringify(value) : value])  // map nam/value pair into [name,value] and convert object to json
        .map(([name, value]) => [name, encodeURIComponent(value)])    // encoded it
        .map(([name, value]) => name + '=' + value)    // create key=val parts
        .join('&');     // combine the parts, separating them by '&'
}

/**
 * convert a queryStr into an Object.  This is an inverse of encodeParams.
 * It support Object whose value is another Object.
 * @param queryStr
 * @returns {*}
 */
export function decodeParams(queryStr) {
    const toVal = (s) => {
        var val = s;
        try {
            val = JSON.parse(val);
        } catch(e) {
            val = isBooleanString(val) ? toBoolean(val) : val;
        }
        return val;
    };

    return  queryStr.replace(/^\?/, '')                     // remove prefex '?' if exists
                    .split('&')                             // separate into param array
                    .map((p) => p.split('=', 2))             // split into key/value pairs
                    .map(([key, val='']) => [key.trim(), val.trim()] )   // trim key and values
                    .map(([key, val]) => [key, toVal(decodeURIComponent(val))]) // decode and convert val
                    .reduce((rval, [key, val]) => set(rval, [key], val), {}); // create a simple object of key/value.
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
 * @param {boolean} doValidation
 * @return {Promise} a promise of the response when successful, or reject with an Error.
 */
export function fetchUrl(url, options, doValidation= true) {

    if (!url) return;


    // ?info=json&access_token_refresh_interval=<seconds>
    /*global __$sso_redirect_uri*/
    if (typeof __$sso_redirect_uri !== 'undefined') {
        const refreshUrl =  `${getRootURL()}${__$sso_redirect_uri}?info=json&access_token_refresh_interval=0`;
        fetch(refreshUrl).then( (resp) =>  {
            resp.text().then( (text) => {
                console.log( text );
            });
        });
    }



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
        [WS_CONNID_HD]: getWsConnId(),
        [REQUEST_WITH]: AJAX_REQUEST
    };
    options.headers = Object.assign(headers, options.headers);

    if (options.params) {
        const params = toNameValuePairs(options.params);        // convert to name-value pairs if it's a simple object.
        if (options.method.toLowerCase() === 'get') {
            url = encodeUrl(url, params);
        } else {
            url = encodeUrl(url);
            if (!options.body) {
                // if 'post' but, body is not provided, add the parameters into the body.
                if (options.method.toLowerCase() === 'post') {
                    options.headers['Content-type'] = 'application/x-www-form-urlencoded';
                    options.body = params.map(({name, value=''}) => encodeURIComponent(name) + '=' + encodeURIComponent(value))
                                    .join('&');
                } else if (options.method.toLowerCase() === 'multipart') {
                    options.method = 'post';
                    var data = new FormData();
                    params.forEach( ({name, value}) => {
                        data.append(name, value);
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
            } else if(response.status === 401){
                return new Error('You are no longer logged in');
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
 * The changes's key must be in path string format, ie.  'a.b.c'.  The value of 
 * of the changes's object will be placed into the give object path.
 * @param object (Object): The object to update.
 * @param changes (Object): The changes to be made.
 * @return the updated object
 */
export function updateObject(object, changes) {
    if (changes) {
        object = Object.entries(changes).reduce( (p, [k,v]) => updateSet(p, k, v), object);
    }
    return object;
}


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
 * replace all occurrences of a string with another string
 * @param {string} str - string to operate on
 * @param {string} find - string to search for
 * @param {string} replace - replacement string
 * @return {string} updated strign
 */
export function replaceAll(str, find, replace) {
    return str.replace(new RegExp(find, 'g'), replace);
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

/**
 * A shim about requestIdleFrame
 * @param callback the callback to happens when idle
 * @see https://developer.mozilla.org/en-US/docs/Web/API/Window/requestIdleCallback
 * @function
 */
export const requestIdleCallback =
    window.requestIdleCallback ||
    function (callback) {
        return setTimeout(() => {
            const start = Date.now();
            callback({
                didTimeout: false,
                timeRemaining: () => Math.max(0, 50 - (Date.now() - start))
            });
        }, 1);
    };

export const cancelIdleCallback = window.cancelIdleCallback || ((id) => clearTimeout(id));

/**
 *
 * @param object
 * @param testFunc - test function to decide which entries should be flattened.  defaults to isPlainObject
 * @returns {*}
 */
export function flattenObject(object, testFunc=isPlainObject) {
    return Object.assign( {}, ...function _flatten( objectBit, path = '' ) {  //spread the result into our return object
        return [].concat(                                                     //concat everything into one level
            ...Object.keys( objectBit ).map(                                  //iterate over object
                (key) => {
                    const fullKey = path === '' ? key : `${ path }.${ key }`;
                    //check if there is a nested object
                    return  (objectBit[ key ] && testFunc(objectBit[ key ])) ?
                        _flatten( objectBit[ key ], fullKey ) :               //call itself if there is
                        ( { [ fullKey ]: objectBit[ key ] } );                //append object with its path as key
                }
            )
        );
    }( object ) );
};


export function deltas(a, b, wrapArray=true) {

    const diff = (a1, b1, wrapArray) => {
        var r = {};
        doDiff(a1, b1, r, wrapArray);
        return r;
    };

    const doDiff = (a2, b2, r, wrapArray) => {
        forEach(a2, function(v, k) {
            // already checked this or equal or original has no value...
            if (b2 && (r.hasOwnProperty(k) || shallowequal(b2[k], v))) return;
            // but what if it returns an empty object? still attach?
            r[k] = b2 && isPlainObject(v) ? diff(v, b2[k], wrapArray) : v;
            if (wrapArray && Array.isArray(r[k])) {
                r[k] = [r[k]];
            }
        });
    };

    const rval = diff(a, b, wrapArray);
    removeEmpties(rval);
    return rval;
}

function removeEmpties(o) {
    for (const k in o) {
        if (!o[k] || !isPlainObject(o[k])) {
            continue; // If null or not an object, skip to the next iteration
        }
        // The property is an object
        removeEmpties(o[k]); // <-- Make a recursive call on the nested object
        if (Object.keys(o[k]).length === 0) {
            delete o[k]; // The object had no properties, so delete that property
        }
    }
}