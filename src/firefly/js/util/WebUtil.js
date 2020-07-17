/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import { forEach, fromPairs, get, has, isArray, isBoolean, isEqual, isFunction, isNil, isObject,
    isPlainObject, last, mergeWith, omit, once, set, union } from 'lodash';
import slug from 'slug';
import shallowequal from 'shallowequal';
import update from 'immutability-helper';


const MEG          = 1048576;
const GIG          = 1048576 * 1024;
const MEG_TENTH    = MEG / 10;
const GIG_HUNDREDTH= GIG / 100;
const K            = 1024;


/*global __PROPS__*/        // this is defined at build-time.
export const getGlobalProps= once( () => __PROPS__ ?? {});

export const getProp= (key, def) => getGlobalProps()[key] ?? def;

const getScriptURL = once(() => {
    const SCRIPT_NAME = getProp('SCRIPT_NAME');
    return [...document.getElementsByTagName('script')]
        .filter((s) => SCRIPT_NAME
            .some((name) => s.src.indexOf(name) > -1))[0]?.src;
});

export const getRootURL = once(() => {
    if (getProp('SCRIPT_NAME') === undefined) return '//localhost:8080/';
    const workingURL = getScriptURL() || window?.location.href;
    return workingURL.substring(0, workingURL.lastIndexOf('/')) + '/';
});

export const getCmdSrvURL = () => `${getRootURL()}sticky/CmdSrv`;

/**
 * returns an object of key:value where keyPrefix is removed from the keys.  i.e
 * <code>
 *      {
 *          version_a: a_val,
 *          version_b: b_val
 *      }
 *      getPropsWith('version_')  => {a: a_val, b: b_val}
 * </code>
 * @param keyPrefix
 * @returns {object}
 */
export function getPropsWith(keyPrefix) {
    return Object.fromEntries(Object.entries(getGlobalProps())
        .filter(([k,]) => k.startsWith(keyPrefix))
        .map( ([k,v]) => [k.substring(keyPrefix.length),v]));
}

export const getModuleName= () => getProp('MODULE_NAME');


/**
 * given a partial url and a rootPath, create a full url, if not rootPath the use document.URL for the rootpath
 * @param {string} url
 * @param {string} rootPath
 * @return {string}
 */
export function modifyURLToFull(url, rootPath) {
    const docUrl = document.URL;
    const lastSlash = docUrl.lastIndexOf('/');
    if (!url && !rootPath) return (lastSlash === docUrl.indexOf('//') + 1) ? docUrl : docUrl.substring(0, lastSlash + 1);
    if (!url) return rootPath;
    if (isFull(url)) return url;
    if (rootPath) return rootPath.endsWith('/') ? rootPath + url : rootPath + '/' + url;
    if (lastSlash === docUrl.indexOf('//') + 1) return docUrl + '/' + url;
    return docUrl.substring(0, lastSlash + 1) + url;
}

function isFull(url) {
    const hPref = ['http', 'https', '/', 'file'];
    url = url.toLowerCase();
    return hPref.some((s) => url.startsWith(s));
}


export const isDefined= (x) => x!==undefined;

/**
 * load a js script by dynamically adding a script tag.
 * @param scriptName
 * @return {Promise} when the script is loaded or failed to load
 */
export function loadScript(scriptName) {
    return new Promise(
        function(resolve, reject) {
            const head= document.getElementsByTagName('head')[0];
            const script= document.createElement('script');
            script.type= 'text/javascript';
            script.charset= 'utf-8';
            script.src= scriptName;
            head.appendChild(script);
            script.onload= (ev) => resolve(ev);
            script.onerror= (ev) => reject(ev);
        });
}

/**
 * Create a function that caches last N (up to 20) function call results
 * This is better for functions that take immutable objects. it compare every argument using ===
 * This cache size has to be smaller (<29=0) since if must iterate though all the last results.
 * @param fn - the function to wrap
 * @param {number} [cacheSize=1] the number of saved calls with a maximum of 20
 * @return {function}
 */
export function memorizeLastCall(fn, cacheSize=1) {
    const lastCallCache= [];
    const maxCache= Math.min(cacheSize,20);
    return (...args) => {
        const cachedEntry= lastCallCache.find( (result) => args.every( (a,idx) => a===result.args[idx]));
        if (cachedEntry) return cachedEntry.retval;
        const retval= fn(...args);
        lastCallCache.unshift({args, retval});
        if (lastCallCache.length>maxCache) lastCallCache.length=maxCache;
        return retval;
    };
}

/**
 * Create a function that caches function call result
 * This function is better use for call types that do heavy computations that take number or string as all the parameters.
 * If you are wrapping a function that takes an object you should pass a makeKey function.
 * @param fn - the function to wrap
 * @param {number} [maxMapSize=5000] when the cache number goes over 5000 the map is cleared
 * @param {function} [makeKey] - make a cache key from the function arguments call with the arguments as an array as makeKey(args). Defaults to calling joint on the arguments
 * @return {function}
 */
export function memorizeUsingMap(fn, maxMapSize=5000, makeKey= (args) => args.join()) {
    const cacheMap= new Map();
    return (...args) => {
        const key= makeKey(args);
        let retval= cacheMap.get(key);
        if (isDefined(retval)) return retval;
        retval= fn(...args);
        if (cacheMap.size>maxMapSize) cacheMap.clear();
        cacheMap.set(key, retval);
        return retval;
    };
}

/**
 * Create an image with a promise that resolves when the image is loaded
 * @param {string} src the source of the image typically a url or local image data
 * @return {Promise} a promise that will resolved with the loaded image object
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
 * Return a promise that resolves when document is ready
 * @return {Promise<void>}
 */
export function documentReady() {
    return (window?.document.readyState==='complete' || window?.document.readyState==='interactive') ?
        Promise.resolve() :
        new Promise((resolve) => window?.addEventListener('load', () => resolve() )); // maybe could use: document.addEventListener('DOMContentLoaded'
}


/**
 * Create an image with a promise and a cancel function.  The promise resolves when the image is loaded.
 * @param {string} src the source of the image typically a url or local image data
 * @return {{promise:Promise, cancelImageLoad:Function}} an object with the promise and the cancel function
 */
export function loadCancelableImage(src) {
    const im = new Image();
    let promiseReject;
    let continueExecution= true;
    let imageCompleted= false;
    const promise= new Promise( (resolve, reject) => {
        promiseReject= reject;
        im.src= src;
        im.onload= () => {
            imageCompleted= true;
            continueExecution && resolve(im);
        };
        im.onerror= (ev) => {
            imageCompleted= true;
            continueExecution && reject(ev);
        };
    });

    const cancelImageLoad= () => {
        if (!imageCompleted) {
            promiseReject(new Error('image load canceled'));
            continueExecution= false;
            im.src= '';
        }
    };
    return {promise, cancelImageLoad};
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
    let rval = url.trim();
    if ( !rval.toLowerCase().startsWith('http') ) {
        rval = getRootURL() + rval;
    }
    if (!params) return rval;

    const parts = url.split('?');
    const baseUrl = parts[0];
    const queryStr= encodeParams(params);

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
 * Returns a string where all characters that are not valid for a complete URL have been escaped.
 * Also, it will do URL rewriting for session tracking if necessary.
 * Fires SESSION_MISMATCH if the seesion ID on the client is different from the one on the server.
 *
 * @param url    this could be a full or partial url.  Delimiter characters will be preserved.
 * @param params parameters to be appended to the url.  These parameters may contain
 *               delimiter characters.  Unlike url, delimiter characters will be encoded as well.
 * @return encoded url
 */
export const encodeServerUrl= (url, params) => encodeUrl(url, params);

export const logError= (...message) => message && message.forEach( (m) => console.log(has(m,'stack') ? m.stack : m) );

/**
 * Copy the content of the string to the clipboard
 * @param str
 */
export function copyToClipboard(str) {
    const el = document.createElement('textarea');  // Create a <textarea> element
    el.value = str;                                 // Set its value to the string that you want copied
    el.setAttribute('readonly', '');                // Make it readonly to be tamper-proof
    el.style.position = 'absolute';
    el.style.left = '-9999px';                      // Move outside the screen to make it invisible
    document.body.appendChild(el);                  // Append the <textarea> element to the HTML document
    const selected =
        document.getSelection().rangeCount > 0      // Check if there is any content selected previously
            ? document.getSelection().getRangeAt(0) // Store selection if found
            : false;                                // Mark as false to know no selection existed before
    el.select();                                    // Select the <textarea> content
    document.execCommand('copy');                   // Copy - only works as a result of a user action (e.g. click events)
    document.body.removeChild(el);                  // Remove the <textarea> element
    if (selected) {                                 // If a selection existed before copying
        document.getSelection().removeAllRanges();  // Unselect everything on the HTML document
        document.getSelection().addRange(selected); // Restore the original selection
    }
}


// export function downloadSimple(url) {
//     let nullFrame = document.getElementById('null_frame');
//     if (!nullFrame) return;
//     nullFrame = document.createElement('iframe');
//     nullFrame.id = 'null_frame';
//     nullFrame.style.display = 'none';
//     nullFrame.style.width = '0px';
//     nullFrame.style.height = '0px';
//     document.body.appendChild(nullFrame);
//     nullFrame.src = url;
// }
//

export function parseUrl(url) {
    const {hash, host, hostname, href, origin, pathname, port, protocol, search, searchParams, username, password} = new URL(url);

    // Convert query string to object map with decoded key/value pairs
    const searchObject = {};
    const parseVal = (val) => {
        try {
            val = JSON.parse(val);
        } catch(e) {}
         return isBooleanString(val) ? toBoolean(val) : val;
    };
    searchParams.forEach((val, key) => searchObject[key] =parseVal(val));

    const paths = pathname.replace(/^\//, '').split('/');
    const filename = last(paths).split(';')[0];

    // Convert paths into an array of [[path, {key:val}], [path, {key:val}], ...]
    const pathAry = [];
    paths.forEach((v, idx) => {
        const [p, pval=''] = v.split(';');
        const params = pval.split(',').reduce((pv, s='') => {
                            if (s.length > 0) {
                                const [k,v=''] = s.split('=');
                                pv[k.trim()] = decodeURIComponent(v.trim());
                            }
                            return pv;
                        }, {});
        pathAry[idx] = [p, params];
    });

    return {hash, host, hostname, href, origin, pathname, port, protocol, username, password,
        path: pathname,
        search: search.replace(/^\?/, ''),
        searchObject,
        filename,
        pathAry
    };
}

export function getSizeAsString(size) {
    const  kStr= 'K';
    const  mStr= 'M';
    const  gStr= 'G';

    if (size > 0 && size < (MEG)) {
        return ((size / K)) + kStr;
    }
    else if (size >= (MEG) && size <  (2*GIG) ) {
        const megs = Math.round(size / MEG);
        const  remain=  Math.round(size % MEG);
        const decimal =  Math.round(remain / MEG_TENTH);
        return megs +'.'+ decimal + mStr;
    }
    else if (size >= (2*GIG) ) {
        const  gigs =  Math.round(size / GIG);
        const remain=  Math.round(size % GIG);
        const decimal =  Math.round(remain / GIG_HUNDREDTH);
        return gigs +'.'+ decimal + gStr;
    }
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
        } else if (![o1, o2].every((o) => Array.isArray(o) || (o?.constructor === Object.prototype.constructor))) {
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
    let str = `${encodeURIComponent(name)}=${encodeURIComponent(value)}`;

    if (isNil(value)) options.maxage = -1;
    if (options.maxage) options.expires = new Date(+new Date() + options.maxage);
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
    const obj = {};
    const pairs = str.split(/ *; */);

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
export const updateObject = (object, changes) =>
                changes ? Object.entries(changes).reduce( (p, [k,v]) => updateSet(p, k, v), object) : object;


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
 * similar to deep merge, except when a value is an array, it will replace the value instead of merging.
 * @param {Object} target  - the target or destination object
 * @param {Object} sources - the source objects
 */
export function mergeObjectOnly(target, sources) {
    return mergeWith(target, sources,
        (tval, sval) => {
            if (isArray(tval)) {
                return sval;
            }                
        }
    );
}

/**
 *
 * Return a function that will only do a search once.
 * The function that takes 2 parameters, validateSearch:Function|boolean, doSearch:Function|undefined
 * When validateSearch search returns true then doSearch is called and search is considered done.
 * To mark the search as done without actually doing it then just pass true as first parameter
 * makeSearchOnce is similar to lodash once but includes a validate as a way to make it done.
 * Note that the execution of the doSearch function is deferred.
 * @param {boolean } defer - if true run the search deferred
 * @return {Function} a function with the signature f(validateSearch,doSearch)
 */
export function makeSearchOnce(defer=true) {
    let executionComplete= false;
    return (validateSearch, doSearch) => {
        if (executionComplete) return;
        const valid= (isBoolean(validateSearch) && validateSearch) || (isFunction(validateSearch) && validateSearch());
        if (!valid) return;
        if (doSearch) defer ? setTimeout(doSearch,5) : doSearch();
        executionComplete=true;
    };
}

/**
 * Simple wrapper around Object.assign.  This can be used for our most common use case of Object.assign.  It is not
 * an attempt to replace it.  It is used with you want to copy and object an add new values into it.
 * @param obj object to clone
 * @param params an object to merge with the new object
 */
export const clone = (obj={},params={}) => Object.assign({},obj,params);


/**
 * Given an array then return an object with the key and value the same from each entry of the array.
 * @param {Array} ary
 * @return {Object}
 */
export function convertToIdentityObj(ary) {
   return fromPairs( ary.map( (v) => [v,v] ));
}

/*---------------------------- update >----------------------------*/

/**
 * A strict version of parseInt.  Anything that is not an integer will return NaN, i.e '12xx34'
 * @param value
 * @returns {number} a number or NaN if it cannot be parse into an interger
 */
export function strictParseInt(value) {
    if (/^[-+]?(\d+|Infinity)$/.test(value)) {
        return Number(value);
    } else {
        return NaN;
    }
}

/**
 * return the boolean value of prop for the given object
 * @param object
 * @param prop
 * @param def
 * @returns {boolean}
 */
export const getBoolean = (object, prop, def=undefined) => toBoolean(object && object[prop], def);


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
export const replaceAll= (str, find, replace) => str.replace(new RegExp(find, 'g'), replace);

/**
 * return true if val is 'true' or 'false' case-insensitive
 * @param val the string value to check.
 * @returns {boolean}
 */
export function isBooleanString(val) {
    const s = String(val).toLowerCase();
    return s === 'true' || s === 'false';
}

export function uniqueID() {
    return Date.now() + '_' + Math.floor(Math.random() * 1000);
}

/**
 * A shim about requestIdleFrame
 * @param callback the callback to happens when idle
 * @see https://developer.mozilla.org/en-US/docs/Web/API/Window/requestIdleCallback
 * @function
 */
export const requestIdleCallback =
    window?.requestIdleCallback ||
    function (callback) {
        return setTimeout(() => {
            const start = Date.now();
            callback({
                didTimeout: false,
                timeRemaining: () => Math.max(0, 50 - (Date.now() - start))
            });
        }, 1);
    };

export const cancelIdleCallback = window?.cancelIdleCallback || ((id) => clearTimeout(id));

/**
 *
 * @param object
 * @param prefix - prefix to use for all object properties
 * @param testFunc - test function to decide which entries should be flattened.  defaults to isPlainObject
 * @returns {*}
 */
export function flattenObject(object, prefix='', testFunc=isPlainObject) {
    return Object.assign( {}, ...function _flatten( objectBit, path=prefix) {  //spread the result into our return object
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
}


export function deltas(a, b, wrapArray=true) {
    const diff = (a1, b1, wrapArray) => {
        const r = {};
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

export function hashCode(str) {
    let hash = 5381;
    let i = str.length;

    while(i) {
        hash = (hash * 33) ^ str.charCodeAt(--i);
    }
    /* JavaScript does bitwise operations (like XOR, above) on 32-bit signed
     * integers. Since we want the results to be always positive, convert the
     * signed int to an unsigned by doing an unsigned bitshift. */
    return hash >>> 0;
}

export const isNumeric= (n) => !isNaN(parseFloat(n)) && isFinite(n);

/**
 * removes extra spaces from a string.
 * <ul><li><code>" bbb    ccc  ddd"</code></li></ul>
 * should become:
 * <ul><li><code>aaa "bbb ccc ddd" eee</code></li></ul>
 * @param {String} s
 */
export const crunch= (s = '') => s && s.replace(/[ \t\n\r\f]/g, ' ').trim().replace(/\s{2,}/g, ' ');

export function matches(s, regExp, ignoreCase) {
    if (isNil(s)) return false;
    const re = ignoreCase ? new RegExp(regExp, 'i') : new RegExp(regExp);
    const result = re.exec(s);
    if (result===null || !result.length) return false;
    for (let i = 0; (i < result.length); i++) {
        if (s === result[i]) return true;
    }
    return false;
}

export const matchesIgCase= (s, regExp) => matches(s, regExp, true);

export function uuid() {
    let seed = Date.now() + (window?.performance?.now?.() ?? '');
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,  (c) => {
        const r = (seed + Math.random() * 16) % 16 | 0;
        seed = Math.floor(seed/16);
        return (c === 'x' ? r : r & (0x3|0x8)).toString(16);
    });
}

export function toSlug(str, options=undefined) {
    return slug(str,options);
}
