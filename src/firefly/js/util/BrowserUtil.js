/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by roby on 4/3/15.
 */

import {getProp} from './WebUtil.js';

const SCRIPT_NAME = getProp('SCRIPT_NAME');

const getScriptURL = (() => {
    
    var scripts = document.getElementsByTagName('script');
    var myScript;
    for(var i=0; (i<scripts.length); i++) {
        for (var j = 0; (j < SCRIPT_NAME.length); j++) {
            if (scripts[i].src.indexOf(SCRIPT_NAME[j]) > -1) {
                myScript = scripts[i].src;
                break;
            }
        }
    }
    return () => myScript;
})();


export const getRootURL = (() => {
    let rootURL = '//localhost:8080/';
    if (SCRIPT_NAME !== undefined) {
        const workingURL= getScriptURL() || window.location.href;
        rootURL= workingURL.substring(0,workingURL.lastIndexOf('/')) + '/';
    }
    return () => rootURL;
})();



// export var getRootPath = (function() {
//     var url= getRootURL();
//     var rootPath= url;
//     if (url.startsWith('http')) {
//         var part1= url.substring(url.indexOf('://')+3);
//         rootPath= part1.substring(part1.indexOf('/'));
//     }
//     return () =>rootPath;
// })();


export var getHost = (() => {
    const url= getRootURL();
    let host= url.substring(url.indexOf('//')+2);
    if (host.indexOf('/')>-1){
        host= host.substr(0,host.indexOf('/'));
    }
    if (host.indexOf(':')>-1) {
        host= host.substr(0,host.indexOf(':'));
    }
    return () =>host;
})();

export const getPort = (function() {
    let port= -1;
    const url= getRootURL();
    if (url.startsWith('http')) {
        port= url.startsWith('https') ? 144 : 80;
    }
    var hostPort= url.substring(url.indexOf('//')+2);
    if (hostPort.indexOf('/')>-1){
        hostPort= hostPort.substr(0,hostPort.indexOf('/'));
    }
    if (hostPort.indexOf(':')>-1) {
        port= hostPort.substr(hostPort.indexOf(':')+1);
    }
    return () =>port;
})();


export const getAbsoluteLeft= function(elem) {
    let left = 0;
    let curr = elem;
    // This intentionally excludes body which has a null offsetParent.
    while (curr.offsetParent) {
        left -= curr.scrollLeft;
        curr = curr.parentNode;
    }
    while (elem) {
        left += elem.offsetLeft;
        elem = elem.offsetParent;
    }
    return left;
};

export const getAbsoluteTop= function(elem) {
    let top = 0;
    let curr = elem;
    // This intentionally excludes body which has a null offsetParent.
    while (curr.offsetParent) {
        top -= curr.scrollTop;
        curr = curr.parentNode;
    }
    while (elem) {
        top += elem.offsetTop;
        elem = elem.offsetParent;
    }
    return top;
};


/**
 *
 * @param {string} url
 * @param {string} rootPath
 * @return {string}
 */
export const modifyURLToFull= function(url, rootPath) {
    let retURL = url;
    if (url) {
        if (!isFull(url)) {
            if (!rootPath) {
                const docUrl = document.URL;
                const lastSlash = docUrl.lastIndexOf('/');
                if (lastSlash > -1) {
                    const rootURL = docUrl.substring(0, lastSlash + 1);
                    retURL = rootURL + url;
                } else {
                    retURL = docUrl + '/' + url;
                }
            } else {
                retURL = rootPath.endsWith('/') ? rootPath + url : rootPath + '/' + url;
            }
        }
    }
    else {
        retURL= rootPath;
    }
    return retURL;
};


const hPref= ['http', 'https', '/', 'file'];

const isFull= function (url) {
    url = url.toLowerCase();
    return hPref.some( (s) => url.startsWith(s));
};


