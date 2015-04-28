/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by roby on 4/3/15.
 */
"use strict";


import React from 'react/addons';

const SCRIPT_NAME = __SCRIPT_NAME__;

var getScriptURL = (function() {
    var scripts = document.getElementsByTagName('script');
    //var index = scripts.length - 1;
    //var myScript = scripts[index];
    //var myScript= [].slice.call(scripts).filter( s => s.src.indexOf(SCRIPT_NAME)>-1);
    var myScript;
    for(var i=0; (i<scripts.length); i++) {
        if (scripts[i].src.indexOf(SCRIPT_NAME)>-1) {
            myScript= scripts[i].src;
            break;
        }
    }
    return function() { return myScript; };
})();


//todo: I am not doing this right, I think we need to get webpack to put the script
//todo: name in as a global varible
export var getRootURL = (function() {

    var workingURL= getScriptURL();
    //var tryIdx= scriptURL.indexOf('js/fftools/fireflyJSTools.js');
    //var workingURL= tryIdx>-1 ? scriptURL.substring(0,tryIdx) : scriptURL;
    var rootURL= workingURL.substring(0,workingURL.lastIndexOf('/')) + '/';
    return () => rootURL;
})();



export var getRootPath = (function() {
    var url= getRootURL();
    var rootPath= url;
    if (url.startsWith('http')) {
        var part1= url.substring(url.indexOf('://')+3)
        rootPath= part1.substring(part1.indexOf('/'))
    }
    return () =>rootPath;
})();


export var getHost = (function() {
    var url= getRootURL();
    var host= url.substring(url.indexOf("//")+2);
    if (host.indexOf("/")>-1){
        host= host.substr(0,host.indexOf("/"));
    }
    if (host.indexOf(":")>-1) {
        host= host.substr(0,host.indexOf(":"));
    }
    return () =>host;
})();

export var getPort = (function() {
    var port= -1;
    var url= getRootURL();
    if (url.startsWith('http')) {
        port= url.startsWith('https') ? 144 : 80;
    }
    var hostPort= url.substring(url.indexOf("//")+2);
    if (hostPort.indexOf("/")>-1){
        hostPort= hostPort.substr(0,hostPort.indexOf("/"));
    }
    if (hostPort.indexOf(":")>-1) {
        port= hostPort.substr(hostPort.indexOf(":")+1);
    }
    return () =>port;
})();


export const getAbsoluteLeft= function(elem) {
    var left = 0;
    var curr = elem;
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
}

export const getAbsoluteTop= function(elem) {
    var top = 0;
    var curr = elem;
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



