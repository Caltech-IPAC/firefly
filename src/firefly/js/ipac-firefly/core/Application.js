/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */




/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/
/*globals window*/

'use strict';


if (!window._babelPolyfill) {
    require("babel/polyfill");
}
import AppAlt from './AppAlt.js';
//import Alt from 'alt';

require('babel/polyfill');
import { appFlux } from './Globals.js';
import Enum from 'enum';
import {ExtensionJavaInterface } from 'ipac-firefly/gwtinterface/ExtensionJavaInterface.js';
import {ExtensionResult } from 'ipac-firefly/gwtinterface/ExtensionResult.js';
import {PlotCmdExtension } from 'ipac-firefly/visualize/PlotCmdExtension.js';
import {ReactJavaInterface } from 'ipac-firefly/gwtinterface/ReactJavaInterface.jsx';
import {ColorDialog } from "ipac-firefly/visualize/ui/ColorDialog.jsx";

import {ServerRequest } from 'ipac-firefly/data/ServerRequest.js';
import {getJsonData } from 'ipac-firefly/rpc/SearchServicesJson.js';

export const NetworkMode = new Enum(['RPC', 'JSON', 'JSONP']);

class Application {
    constructor() {
        this.networkMode= NetworkMode.JSON;
        this.alt= AppAlt;
    }


}

export const application= new Application();






export const fireflyInit= function() {


    var touch= false; // ToDo: determine if we are on a touch device
    if (touch) {
        React.initializeTouchEvents(true);
    }

    if (!window.firefly) window.firefly= {};
    if (!window.firefly.gwt) {
        window.firefly.gwt= {};
    }
 //   window.firefly.gwt.ReactJavaInterface= ReactJavaInterface;
    window.firefly.appFlux= appFlux;
    window.firefly.gwt.ExtensionJavaInterface= ExtensionJavaInterface;
    window.firefly.gwt.ExtensionResult= ExtensionResult;
    window.firefly.gwt.PlotCmdExtension= PlotCmdExtension;
    // a method to get JSON data from external task launcher
    window.firefly.getJsonFromTask= function(launcher, task, taskParams) {
            let req = new ServerRequest('JsonFromExternalTask');
            req.setParam({name : 'launcher', value : launcher});
            req.setParam({name : 'task', value : task});
            req.setParam({name : 'taskParams', value : JSON.stringify(taskParams)});
            return getJsonData(req);
        };
    window.firefly.gwt.ColorDialog= ColorDialog;
};

