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
import {FireflySlate} from './templates/fireflyslate/FireflySlate.jsx';
import {LcViewer} from './templates/lightcurve/LcViewer.jsx';
import {HydraViewer} from './templates/hydra/HydraViewer.jsx';
import {initApi} from './api/ApiBuild.js';
import {dispatchUpdateLayoutInfo} from './core/LayoutCntlr.js';
import {showInfoPopup} from './ui/PopupUtil';

import {ServerRequest } from './data/ServerRequest.js';
import {getJsonData } from './rpc/SearchServicesJson.js';

import {reduxFlux} from './core/ReduxFlux.js';
import {wsConnect} from './core/messaging/WebSocketClient.js';
import {ActionEventHandler} from './core/messaging/MessageHandlers.js';
import {dispatchAppOptions} from './core/AppDataCntlr.js';
import {init} from './rpc/CoreServices.js';

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
    FireflySlate,
    LightCurveViewer : LcViewer,
    HydraViewer
};


/**
 * @global
 * @public
 * @typedef {Object} StartupConfigOptions
 *
 * @summary An object that is defined in the html that has configuration options for Firefly
 *
 *
 * @prop {Object} MenuItemKeys -  an object the references MenuItemKeys.js that can turn on or off buttons on the image tool bar
 * @prop {Array.<string> } imageTabs - specifies the order of the time in the image dialog e.g. - [ 'fileUpload', 'url', '2mass', 'wise', 'sdss', 'msx', 'dss', 'iras' ]
 * @prop {string|function} irsaCatalogFilter - a function or a predefined key that specifies how the catalogs are filter in the UI
 * @prop {string} catalogSpacialOp -  two values undefined or 'polygonWhenPlotExist'. when catalogSpacialOp === 'polygonWhenPlotExist' then
 *                                  the catalog panel will show the polygon option as default when possible
 * @prop {Array.<string> } imageMasterSources -  default - ['ALL'], source to build image master data from
 * @prop {Array.<string> } imageMasterSourcesOrder - for the image dialog sort order of the projects, anything not listed is put on bottom
 *
 */


const defConfigOptions = {
    MenuItemKeys: {},
    imageTabs: undefined,
    irsaCatalogFilter: undefined,
    catalogSpacialOp: undefined,
    imageMasterSources: ['ALL'],
    imageMasterSourcesOrder: '',
};




function fireflyInit() {

    if (! (window.firefly && window.firefly.initialized) ) {
        flux.bootstrap();
        const touch= false; // ToDo: determine if we are on a touch device
        if (touch) {
            React.initializeTouchEvents(true);
        }

        if (!window.firefly) window.firefly= {};

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
        wsConnect().then((client) => {
            client.addListener(ActionEventHandler);
            window.firefly.wsClient = client;
            init();    //TODO.. need to add spaName when we decide to support it.
        });
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

    return  new Promise((resolve) => {

        fireflyInit();
        flux.process( {type : APP_LOAD} );
        resolve && resolve();

        if (options) {
            dispatchAppOptions(Object.assign({},defConfigOptions, options));
            if (options.disableDefaultDropDown) {
                dispatchUpdateLayoutInfo({disableDefaultDropDown:true});
            }
        }

        if (viewer) {
            if (window.document.readyState==='complete' || window.document.readyState==='interactive') {
                renderRoot(viewer, props);
            }
            else {
                console.log('Waiting for document to finish loading');
                window.addEventListener('load', () => renderRoot(viewer, props) ); // maybe could use: document.addEventListener('DOMContentLoaded'
            }
        } else {
            initApi();
        }
    });
}

function renderRoot(viewer, props) {
    const e= document.getElementById(props.div);
    if (e)  {
        ReactDOM.render(React.createElement(viewer, props), e);
    }
    else {
        showInfoPopup('HTML page is not setup correctly, Firefly cannot start.');
        console.log(`DOM Element "${props.div}" is not found in the document, Firefly cannot start.`);
    }
}
