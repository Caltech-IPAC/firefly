/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by roby on 4/3/15.
 */
"use strict";

require("babel/polyfill");

import React from 'react/addons';

var getScriptURL = (function() {
    var scripts = document.getElementsByTagName('script');
    var index = scripts.length - 1;
    var myScript = scripts[index];
    return function() { return myScript.src; };
})();


export var getRootURL = (function() {
    var scriptURL= getScriptURL();
    var rootURL= scriptURL.substring(0,scriptURL.lastIndexOf('/')) + '/';
    return function() { return rootURL; };
})();




export const fireflyInit= function() {


    var touch= false; // ToDo: determine if we are on a touch device
    if (touch)  {
        React.initializeTouchEvents(true)
    }

};





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



