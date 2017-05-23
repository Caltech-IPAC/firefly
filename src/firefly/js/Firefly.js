/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*global __$version_tag*/

import 'babel-polyfill';
import 'isomorphic-fetch';
import React from 'react';
import ReactDOM from 'react-dom';
import 'styles/global.css';

import {APP_LOAD} from './core/AppDataCntlr.js';
import {FireflyViewer} from './templates/fireflyviewer/FireflyViewer.js';
import {LcViewer} from './templates/lightcurve/LcViewer.jsx';
import {initApi} from './api/ApiBuild.js';

import {ServerRequest } from './data/ServerRequest.js';
import PlotState from './visualize/PlotState.js';
import {getJsonData } from './rpc/SearchServicesJson.js';
import ExternalAccessUtils from './core/ExternalAccessUtils.js';

import {reduxFlux} from './core/ReduxFlux.js';
import {wsConnect} from './core/messaging/WebSocketClient.js';
import {ActionEventHandler} from './core/messaging/MessageHandlers.js';
import {dispatchAppOptions} from './core/AppDataCntlr.js';

export const flux = reduxFlux;

/**
 * A list of available templates
 * @enum {string}
 */
export const Templates = {
    /**
     * This templates has multiple views:  'images', 'tables', and 'xyPlots'.
     * They can be combined with ' | ', i.e.  'images | tables'
     */
    FireflyViewer,
    LightCurveViewer : LcViewer
};



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

    if (! (window.firefly && window.firefly.initialized) ) {
        flux.bootstrap();
        var touch= false; // ToDo: determine if we are on a touch device
        if (touch) {
            React.initializeTouchEvents(true);
        }

        if (!window.firefly) window.firefly= {};

        window.firefly.appFlux= appFlux;
        // to call histogram and other react components from GWT

        // a method to get JSON data from external task launcher
        window.firefly.getJsonFromTask= function(launcher, task, taskParams) {
            const req = new ServerRequest('JsonFromExternalTask');
            req.setParam({name : 'launcher', value : launcher});
            req.setParam({name : 'task', value : task});
            req.setParam({name : 'taskParams', value : JSON.stringify(taskParams)});
            return getJsonData(req);
        };
        window.firefly.initialized = true;

        // start WebSocketClient
        const wsClient = wsConnect();
        wsClient.addListener(ActionEventHandler);
        window.firefly.wsClient = wsClient;
    }
}

export function getVersion() {
  return typeof __$version_tag === 'undefined' ? 'unknown' : __$version_tag;
} 


export const firefly = {

    bootstrap,

    process(rawAction, condition) {
        return flux.process(rawAction, condition);
    },

    addListener(listener, ...types) {

    }

};


/**
 * boostrap Firefly api or application.
 * @param options   global options used by both application and api
 * @param viewer    render this viewer onto the document.  if viewer does not exists, it will init api instead.
 * @param props     viewer's props used for rendering.
 * @returns {Promise.<boolean>}
 */
function bootstrap(options, viewer, props) {

    fireflyInit();
    flux.process( {type : APP_LOAD} );

    if (options) {
        const defOps = {
            MenuItemKeys: {},
            imageTabs: undefined,
            irsaCatalogFilter: undefined,
            catalogSpacialOp: undefined
        };
        dispatchAppOptions(Object.assign({},defOps, options));
    }
    if (viewer) {
        ReactDOM.render(React.createElement(viewer, props),
            document.getElementById(props.div));
    } else {
        initApi();

    }
    return Promise.resolve(true);
}
