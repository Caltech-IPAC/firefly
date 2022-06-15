
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import 'isomorphic-fetch';
import React from 'react';
import ReactDOM from 'react-dom';
import {set, defer, once} from 'lodash';
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
import {bootstrapRedux, flux} from './core/ReduxFlux.js';
import {getOrCreateWsConn} from './core/messaging/WebSocketClient.js';
import {ActionEventHandler} from './core/messaging/MessageHandlers.js';
import {getServerDefinedOptions, notifyServerAppInit} from './rpc/CoreServices.js';
import {getPropsWith, mergeObjectOnly, getProp, toBoolean, documentReady,uuid} from './util/WebUtil.js';
import {dispatchChangeTableAutoScroll, dispatchWcsMatch, visRoot} from './visualize/ImagePlotCntlr.js';
import {Logger} from './util/Logger.js';
import {evaluateWebApi, isUsingWebApi, WebApiStat} from './api/WebApi.js';
import {WebApiHelpInfoPage} from './ui/WebApiHelpInfoPage.jsx';
import {dispatchOnAppReady} from './core/AppDataCntlr.js';
import {getBootstrapRegistry} from './core/BootstrapRegistry.js';
import {showLostConnection} from './ui/LostConnection.jsx';
import {recordHistory} from './core/History.js';
import {setDefaultImageColorTable} from './visualize/WebPlotRequest.js';
import {initWorkerContext} from './threadWorker/WorkerAccess.js';
import {getTAPServices} from './ui/tap/TapKnownServices.js';
import {loadAllJobs} from './core/background/BackgroundUtil.js';

let initDone = false;
const logger = Logger('Firefly-init');

export const getFireflySessionId= once(() =>
    toBoolean(getProp('version.ExtendedCache')) ? 'KEEP_SESSION' : `FF-Session-${Date.now()}`);

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
 * @prop {string} catalogSpatialOp -  two values undefined or 'polygonWhenPlotExist'. when catalogSpatialOp === 'polygonWhenPlotExist' then
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
        {label:'TAP/Table Searches', action: 'MultiTableSearchCmd'},
        // {label:'Tap Searches', action: 'TAPSearch'},
        // {label:'Classic Searches', action: 'MultiTableSearchCmd'},
        {label:'Charts', action:'ChartSelectDropDownCmd'},
        {label:'Upload', action: 'FileUploadDropDownCmd'},
    ],
};

/** @type {FireflyOptions} */
const defFireflyOptions = {
    multiTableSearchCmdOptions: [
        {id: 'tap', title: 'TAP Searches'},
        {id: 'irsacat', title: 'IRSA Catalogs'},
        {id: 'vocat'},
        {id: 'nedcat'}
    ],
    MenuItemKeys: {},
    imageTabs: undefined,
    irsaCatalogFilter: undefined,
    catalogSpatialOp: undefined,
    imageMasterSources: ['ALL'],
    imageDisplayType:'standard',
    showCatalogSearchTarget: true,
    imageMasterSourcesOrder: undefined,
    workspace : { showOptions: false},
    wcsMatchType: false,
    imageScrollsToHighlightedTableRow: true,
    imageScrollsToActiveTableOnLoadOrSelect: true,
    'help.base.url': undefined,                     // this overrides property set during build time.

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
    table : {
        pageSize: 100,
    },
    image : {
        defaultColorTable: 1,
        canCreateExtractionTable: false,
    },
    coverage : {
        // TODO: need to define all options with defaults here.  used in FFEntryPoint.js
    },
    dataProducts : {
        factoryOverride: [
        ],
        clientAnalysis: {
            // xxxx : abcDataProductsAnalyzer

        },
    },
    tap : {
        services: getTAPServices( ['IRSA', 'NED', 'NASA Exoplanet Archive', 'KOA', 'HEASARC', 'MAST Images',
                                   'CADC', 'VizieR (CDS)', 'Simbad (CDS)', 'Gaia', 'GAVO', 'HSA'] ),
        defaultMaxrec: 50000
    }
};






/**
 * add options to store and setup any options that need specific initialization
 * @param {Object} options
 */
function installOptions(options) {
    // setup options
    dispatchAppOptions(options);
    options.disableDefaultDropDown && dispatchUpdateLayoutInfo({disableDefaultDropDown:true});
    options.readoutDefaultPref && dispatchChangeReadoutPrefs(options.readoutDefaultPref);
    options.wcsMatchType && dispatchWcsMatch({matchType:options.wcsMatchType, lockMatch:true});
    setDefaultImageColorTable(options.image?.defaultColorTable ?? 1);

    if (options.imageScrollsToHighlightedTableRow!==visRoot().autoScrollToHighlightedTableRow) {
        dispatchChangeTableAutoScroll(options.imageScrollsToHighlightedTableRow);
    }

}

/**
 *
 * @param {AppProps} props
 * @param {FireflyOptions} options
 * @param {Array.<WebApiCommand>} webApiCommands
 */
function fireflyInit(props, options={}, webApiCommands) {

    if (initDone) return;

    props = mergeObjectOnly(defAppProps, props);
    const viewer = Templates[props.template];
    if (viewer) props.renderTreeId= undefined; // in non API usages, renderTreeId is not used, this line is just for clarity

    installOptions(mergeObjectOnly(defFireflyOptions, options));

    // initialize UI or API depending on entry mode.
    documentReady().then(() => viewer ? renderRoot(viewer, props,webApiCommands) : initApi(props));
    initDone = true;
}

/*
 *
 * @param {string} divId
 * @param {AppProps} props
 * @return {Object} return object has two functions {unrender:Function, render:Function}
 */
export function startAsAppFromApi(divId, overrideProps={template: 'FireflySlate'}) {

    const props = {...mergeObjectOnly({...window.firefly.originalAppProps}, overrideProps), div:divId};
    const viewer = Templates[props.template];
    if (!divId || !viewer) {
        !divId  && logger.error('required: divId');
        !viewer && logger.error(`required: props.template, must be one of ${Object.keys(Templates).join()}`);
        return;
    }

    props.disableDefaultDropDown && dispatchUpdateLayoutInfo({disableDefaultDropDown:true});
    props.readoutDefaultPref && dispatchChangeReadoutPrefs(props.readoutDefaultPref);
    props.wcsMatchType && dispatchWcsMatch({matchType:props.wcsMatchType, lockMatch:true});

    const controlObj= {
        unrender: () => {
                const e= document.getElementById(divId);
                if (!e) return;
                ReactDOM.unmountComponentAtNode(e);
        },
        render: () => renderRoot(viewer, props)
    };
    controlObj.render();
    return controlObj;
}

/**
 * returns version information in a key/value object.
 * @returns {VersionInfo}
 */
export const getVersion= once(() => getPropsWith('version.') );
const ffTag= () => getVersion().BuildFireflyTag ?? '';

const releaseRE= /release-\d+(\.\d+)+/;
const preRE=  /pre-(\d)+-\d+(\.\d+)+/;
const cycleRE= /cycle-\d+\.\d+/;
const justVersion= /\d+(\.\d+)+/;

export function getFireflyLibraryVersionStr() {
    const {BuildFireflyBranch:branch='unknown-branch', BuildCommit:commit, BuildCommitFirefly:ffCommit} = getVersion();

    if (isVersionFormalRelease()) return getFormalReleaseVersionStr();
    else if (isVersionPreRelease()) return getPrereleaseVersionStr();
    else if (getDevCycle()) return getDevVersionStr(branch, ffCommit??commit);
    else return `0.0-${branch}-development`;
}

const getPrereleaseVersionStr= () =>
    ffTag().match(justVersion)?.[0]+'-PRE-'+ ffTag().match(preRE)?.[0].split('-')[1];

const getDevVersionStr= (branch,commit) =>
    `${getDevCycle()}-DEV${branch!=='dev'?':'+branch:''}_${commit?.substring(0,4)}`;

const getFormalReleaseVersionStr= () => ffTag().match(justVersion)?.[0];


export const isVersionFormalRelease = ()  => Boolean(ffTag().match(releaseRE));
export const isVersionPreRelease = ()  => Boolean(ffTag().match(preRE));
export function getDevCycle() {
    const {DevCycleTag:tag} = getVersion();
    return tag?.match(cycleRE) ? tag?.match(justVersion)[0] : '';
}


export const firefly = {
    bootstrap
};

/* eslint-disable  quotes */


/**
 * bootstrap Firefly api or application.
 * @param {AppProps} props - application properties
 * @param {FireflyOptions} options - startup options
 * @param {Array.<WebApiCommand>} webApiCommands
 * @returns {Promise.<boolean>}
 */
function bootstrap(props, options, webApiCommands) {

    if (window?.firefly?.initialized) return Promise.resolve(); // if initialized, don't run it again.

    set(window, 'firefly.initialized', true);
    return new Promise(async (resolve) => {

        const processDecor= (process) => (rawAction) => {
            getOrCreateWsConn().catch(() => showLostConnection());
            process(rawAction);
            recordHistory(rawAction);
        };

        bootstrapRedux( getBootstrapRegistry(), processDecor);
        flux.process( {type : APP_LOAD} );  // setup initial store/state

        ensureUsrKey();
        // establish websocket connection first before doing anything else.

        let srvOptions={};
        try {
            srvOptions= await getServerDefinedOptions();
        }
        catch (err) {
            logger.error('could not retrieve valid server options');
        }

        const finalOptions = mergeObjectOnly(options, srvOptions);
        const client= await getOrCreateWsConn();

        fireflyInit(props, finalOptions, webApiCommands);

        client.addListener(ActionEventHandler);
        window.firefly.wsClient = client;
        notifyServerAppInit({spaName:`${props.appTitle||''}--${props.template?props.template:'api'}`});
        loadAllJobs();
        resolve?.();


    }).then(() => {
        // when all is done, mark app as 'ready'
        defer(() => dispatchUpdateAppData({isReady: true}));
        initWorkerContext();
    });
}

function renderRoot(viewer, props, webApiCommands) {
    const e= document.getElementById(props.div);
    if (!e) {
        showInfoPopup('HTML page is not setup correctly, Firefly cannot start.');
        logger.error(`DOM Element "${props.div}" is not found in the document, Firefly cannot start.`);
        return;
    }

    const doAppRender= () => ReactDOM.render(React.createElement(viewer, props), e);
    isUsingWebApi(webApiCommands) ? handleWebApi(webApiCommands, e, doAppRender) : doAppRender();
}


function handleWebApi(webApiCommands, e, doAppRender) {
    const {status, helpType, contextMessage, cmd, execute,
        params, badParams, missingParams}= evaluateWebApi(webApiCommands);
    switch (status) {
        case WebApiStat.EXECUTE_API_CMD:
            window.history.pushState('home', 'Home', new URL(window.location).pathname); // ?? is this necessary?
            doAppRender();
            dispatchOnAppReady(() =>  execute?.(cmd,params));
            break;
        case WebApiStat.SHOW_HELP:
            ReactDOM.render(
                React.createElement(
                    WebApiHelpInfoPage,
                    {helpType, contextMessage, cmd, params, webApiCommands, badParams, missingParams}), e);
            break;
        default:
            logger.error('Unexpect status, can\'t handle web api: '+ status);
            break;
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

