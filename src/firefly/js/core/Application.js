/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
import 'babel/polyfill';

import AppAlt from './AppAlt.js';
//import Alt from 'alt';
import { appFlux } from './Globals.js';
import Enum from 'enum';
import {ExtensionJavaInterface } from '../gwtinterface/ExtensionJavaInterface.js';
import {ExtensionResult } from '../gwtinterface/ExtensionResult.js';
import {PlotCmdExtension } from '../visualize/PlotCmdExtension.js';
import {ReactJavaInterface } from '../gwtinterface/ReactJavaInterface.jsx';
import ColorDialog from '../visualize/ui/ColorDialog.jsx';
import ExampleDialog  from '../ui/ExampleDialog.jsx';

import {ServerRequest } from '../data/ServerRequest.js';
import {makePlotState} from '../visualize/PlotState.js';
import {getJsonData } from '../rpc/SearchServicesJson.js';

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
    window.firefly.appFlux= appFlux;
    window.firefly.gwt.ExtensionJavaInterface= ExtensionJavaInterface;
    window.firefly.gwt.ExtensionResult= ExtensionResult;
    window.firefly.gwt.PlotCmdExtension= PlotCmdExtension;
    window.firefly.gwt.makePlotState= makePlotState;
    // to call histogram and other react components from GWT
    window.firefly.gwt.ReactJavaInterface= ReactJavaInterface;

    // a method to get JSON data from external task launcher
    window.firefly.getJsonFromTask= function(launcher, task, taskParams) {
            const req = new ServerRequest('JsonFromExternalTask');
            req.setParam({name : 'launcher', value : launcher});
            req.setParam({name : 'task', value : task});
            req.setParam({name : 'taskParams', value : JSON.stringify(taskParams)});
            return getJsonData(req);
        };
    window.firefly.gwt.ColorDialog= ColorDialog;
    window.firefly.gwt.ExampleDialog= ExampleDialog;
};

