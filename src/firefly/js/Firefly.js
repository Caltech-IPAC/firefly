/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*global __VERSION_TAG__*/


import 'babel/polyfill';
import 'isomorphic-fetch';
import React from 'react';
import 'styles/global.css';
import {isUndefined} from 'lodash';

import {APP_LOAD, dispatchUpdateAppData} from './core/AppDataCntlr.js';
import {ExtensionJavaInterface } from './gwtinterface/ExtensionJavaInterface.js';
import {ExtensionResult } from './gwtinterface/ExtensionResult.js';
import {PlotCmdExtension } from './visualize/PlotCmdExtension.js';
import {ReactJavaInterface } from './gwtinterface/ReactJavaInterface.jsx';
import {showExampleDialog}  from './ui/ExampleDialog.jsx';

import {ServerRequest } from './data/ServerRequest.js';
import PlotState from './visualize/PlotState.js';
import {getJsonData } from './rpc/SearchServicesJson.js';
import ExternalAccessUtils from './core/ExternalAccessUtils.js';

import {reduxFlux} from './core/ReduxFlux.js';
import {wsConnect} from './core/messaging/WebSocketClient.js';
import {ActionEventHandler} from './core/messaging/MessageHandlers.js';

export const flux = reduxFlux;

/**
 * work around for transition from flummox to redux
 */
const appFlux= {
    getActions : function(type) {
        if (type==='ExternalAccessActions') {
            return {
                extensionAdd : ExternalAccessUtils.extensionAdd,
                extensionActivate : ExternalAccessUtils.extensionActivate,
                channelActivate : ExternalAccessUtils.channelActivate
            };
        }
        return undefined;
    }

};


function fireflyInit() {

    if (! window.ffgwt ) {
        window.ffgwt = {
            onLoaded: () => dispatchUpdateAppData({gwtLoaded: true})
        };
    }

    if (! (window.firefly && window.firefly.initialized) ) {
        flux.bootstrap();
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
        window.firefly.gwt.makePlotState= PlotState.makePlotState;
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
        window.firefly.gwt.showExampleDialog= showExampleDialog;
        window.firefly.initialized = true;

        // start WebSocketClient
        const wsClient = wsConnect();
        wsClient.addListener(ActionEventHandler);
        window.firefly.wsClient = wsClient;

        if (!isUndefined(__VERSION_TAG__)) {
            window.firefly.version = __VERSION_TAG__;
        }
    }
}

export function getVersion() {
  return isUndefined(__VERSION_TAG__) ? 'unknown' :
        window.firefly.version = __VERSION_TAG__;
} 


export var firefly = {

    bootstrap() {
        return new Promise(function(resolve, reject) {
            fireflyInit();
            flux.process( {type : APP_LOAD} );
            resolve();
        });
    },

    process(rawAction, condition) {
        return flux.process(rawAction, condition);
    },

    addListener(listener, ...types) {

    }

};



