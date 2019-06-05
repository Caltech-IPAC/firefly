
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import 'babel-polyfill';
import 'isomorphic-fetch';
import React from 'react';
import ReactDOM from 'react-dom';
import {set, get, defer} from 'lodash';
import 'styles/global.css';

import {APP_LOAD, dispatchAppOptions, dispatchUpdateAppData} from './core/AppDataCntlr.js';
import {FireflyViewer} from './templates/fireflyviewer/FireflyViewer.js';
import {FireflySlate} from './templates/fireflyslate/FireflySlate.jsx';
import {LcViewer} from './templates/lightcurve/LcViewer.jsx';
import {HydraViewer} from './templates/hydra/HydraViewer.jsx';
import {initApi} from './api/ApiBuild.js';
import {dispatchUpdateLayoutInfo} from './core/LayoutCntlr.js';
import {dispatchChangeReadoutPrefs} from './visualize/MouseReadoutCntlr.js';
import {showInfoPopup} from './ui/PopupUtil';

import {reduxFlux} from './core/ReduxFlux.js';
import {wsConnect} from './core/messaging/WebSocketClient.js';
import {ActionEventHandler} from './core/messaging/MessageHandlers.js';
import {init} from './rpc/CoreServices.js';
import {getPropsWith, mergeObjectOnly} from './util/WebUtil.js';
import {initLostConnectionWarning} from './ui/LostConnection.jsx';
import {dispatchChangeTableAutoScroll, dispatchWcsMatch, visRoot} from './visualize/ImagePlotCntlr';

export const flux = reduxFlux;

var initDone = false;

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
 * @typedef {Object} AppProps
 * @summary A property object used for customizing the application
 *
 * @prop {String} [template] - UI template to display.  API mode if not given
 * @prop {string} [views]    - some template may have multiple views.  If not given, the default view of the template will be used.
 * @prop {string} [div=app]  - ID of a div to place the viewer in.
 * @prop {string} [appTitle] - title of this application.
 * @prop {boolean} [showUserInfo=false] - show user information.  This is used when authentication is available
 * @prop {boolean} [showViewsSwitch] - show/hide the swith views buttons
 * @prop {Array.<function>} [rightButtons]    - function(s) returning a button to be displayed on the top-right of the result page.
 * 
 *
 * @prop {Object} menu         custom menu bar
 * @prop {string} menu.label   button's label
 * @prop {string} menu.action  action to fire on button clicked
 * @prop {string} menu.type    use 'COMMAND' for actions that's not drop-down related.
 */


/**
 * @global
 * @public
 * @typedef {Object} FireflyOptions
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

/** @type {AppProps} */
const defAppProps = {
    div: 'app',
    template: undefined,        // don't set a default value for this.  it's also used as a switch for API vs UI mode
    appTitle: '',
    showUserInfo: false,
    showViewsSwitch: false,
    rightButtons: undefined,

    menu: [
        {label:'Images', action:'ImageSelectDropDownCmd'},
        {label:'Catalogs', action:'IrsaCatalogDropDown'},
        {label:'Charts', action:'ChartSelectDropDownCmd'},
        {label:'Upload', action: 'FileUploadDropDownCmd'},
        //{label:'Workspace', action: 'WorkspaceDropDownCmd'}
    ],
};

/** @type {FireflyOptions} */
const defFireflyOptions = {
    MenuItemKeys: {},
    imageTabs: undefined,
    irsaCatalogFilter: undefined,
    catalogSpacialOp: undefined,
    imageMasterSources: ['ALL'],
    imageMasterSourcesOrder: undefined,
    workspace : { showOptions: false},
    wcsMatchType: false,
    imageScrollsToHighlightedTableRow: true,

    charts: {
        defaultDeletable: undefined, // by default if there are more than one chart in container, all charts are deletable
        maxRowsForScatter: 5000, // maximum table rows for scatter chart support, heatmap is created for larger tables
        minScatterGLRows: 1000, // minimum number of points to use WebGL 'scattergl' instead of SVG 'scatter'
        singleTraceUI: false, // by default we support multi-trace in UI
        upperLimitUI: false, // by default user can not set upper limit column in scatter options
        ui: {HistogramOptions: {fixedAlgorithm: undefined}} // by default we allow both "uniform binning" and "bayesian blocks"
    },
    hips : {
        useForImageSearch: true,
        hipsSources: 'all',
        defHipsSources: {source: 'irsa', label: 'Featured'},
        mergedListPriority: 'irsa'
    },
    coverage : {
        // TODO: need to define all options with defaults here.  used in FFEntryPoint.js
    },
    tap : {
        services: [
            { label: 'IRSA https://irsa.ipac.caltech.edu/TAP',
                value: 'https://irsa.ipac.caltech.edu/TAP' },
            { label: 'NED https://ned.ipac.caltech.edu/tap',
                value: 'https://ned.ipac.caltech.edu/tap/' },
            { label: 'CADC https://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/tap',
                value: 'https://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/tap' },
            { label: 'GAIA https://gea.esac.esa.int/tap-server/tap',
                value: 'https://gea.esac.esa.int/tap-server/tap' },
            { label: 'GAVO http://dc.g-vo.org/tap',
                value: 'http://dc.g-vo.org/tap'},
            { label: 'MAST https://vao.stsci.edu/CAOMTAP/TapService.aspx',
                value: 'https://vao.stsci.edu/CAOMTAP/TapService.aspx' }
        ],
        defaultMaxrec: 50000
    }
};


/**
 *
 * @param props
 * @param options
 */
function fireflyInit(props, options={}) {

    if (initDone) return;

    props = mergeObjectOnly(defAppProps, props);
    props.renderTreeId= undefined; // in not API usages, renderTreeId is not used
    options = mergeObjectOnly(defFireflyOptions, options);
    const {template} = props;

    const viewer = get(Templates, template);

    const touch= false; // ToDo: determine if we are on a touch device
    if (touch) {
        React.initializeTouchEvents(true);
    }

    // setup application options
    dispatchAppOptions(options);
    if (options.disableDefaultDropDown) {
        dispatchUpdateLayoutInfo({disableDefaultDropDown:true});
    }
    if (options.readoutDefaultPref) {
        dispatchChangeReadoutPrefs(options.readoutDefaultPref);
    }
    if (options.wcsMatchType) {
        dispatchWcsMatch({matchType:options.wcsMatchType, lockMatch:true});
    }

    if (options.imageScrollsToHighlightedTableRow!==visRoot().autoScrollToHighlightedTableRow) {
        dispatchChangeTableAutoScroll(options.imageScrollsToHighlightedTableRow);
    }

    // initialize UI or API depending on entry mode.
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

    initDone = true;
}

/*
 *
 * @param {string} divId
 * @param {Object} props
 * @param {Object} options
 * @return {Object} return object has two properties unrender and render
 */
export function startAsAppFromApi(divId, props={}, options={}) {
    const defProps= {...defAppProps};
    props = mergeObjectOnly(defProps, props);
    props= {...props, ...{div:divId}};
    const {template} = props;
    const viewer = get(Templates, template);
    options = mergeObjectOnly(defFireflyOptions, options);
    dispatchAppOptions(options);
    if (props.disableDefaultDropDown) {
        dispatchUpdateLayoutInfo({disableDefaultDropDown:true});
    }
    if (props.readoutDefaultPref) {
        dispatchChangeReadoutPrefs(options.readoutDefaultPref);
    }
    if (viewer) {
        const element= document.getElementById(props.div);
        const controlObj= {
             unrender: () => ReactDOM.unmountComponentAtNode(element),
             render: () => renderRoot(viewer, props)
        };
        controlObj.render();
        return controlObj;
    }

}

/**
 * returns version information in a key/value object.
 * @returns {VersionInfo}
 */
export function getVersion() {
  return getPropsWith('version.');
}


export const firefly = {
    bootstrap,
    addListener: flux.addListener,
    process: flux.process,
};



/**
 * boostrap Firefly api or application.
 * @param {AppProps} props - application properties
 * @param {FireflyOptions} options - startup options
 * @returns {Promise.<boolean>}
 */
function bootstrap(props, options) {

    // if initialized, don't run it again.
    if (window.firefly && window.firefly.initialized) return Promise.resolve();

    set(window, 'firefly.initialized', true);
    return  new Promise((resolve) => {

        flux.bootstrap();
        flux.process( {type : APP_LOAD} );  // setup initial store/state

        ensureUsrKey();
        // establish websocket connection first before doing anything else.
        wsConnect((client) => {
            fireflyInit(props, options);

            client.addListener(ActionEventHandler);
            window.firefly.wsClient = client;
            init();    //TODO.. need to add spaName when we decide to support it.
            initLostConnectionWarning();

            resolve && resolve();
        });
    }).then(() => {
        // when all is done.. mark app as 'ready'
        defer(() => dispatchUpdateAppData({isReady: true}));
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



function ensureUsrKey() {
    if (hasOldUsrKey()) {
        document.cookie = 'usrkey=;path=/;max-age=-1';
        document.cookie = `usrkey=;path=${location.pathname};max-age=-1`;
    }
    const usrKey = getCookie('usrkey');
    if (!usrKey) {
        document.cookie = `usrkey=${uuid()};max-age=${3600 * 24 * 7 * 2}`;
    }
}

function uuid() {
    var seed = Date.now();
    if (window.performance && typeof window.performance.now === 'function') {
        seed += performance.now();
    }
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = (seed + Math.random() * 16) % 16 | 0;
        seed = Math.floor(seed/16);

        return (c === 'x' ? r : r & (0x3|0x8)).toString(16);
    });

    return uuid;
}

function hasOldUsrKey() {
    return document.cookie.split(';').map((s) => s.trim())
        .some( (c) => {
            const [name='', val=''] = c.split('=');
            return name === 'usrkey' && val.includes('/');
        });

}

function getCookie(name) {
    return ('; ' + document.cookie)
        .split('; ' + name + '=')
        .pop()
        .split(';')
        .shift();
}

