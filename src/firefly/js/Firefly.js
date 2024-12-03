
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import 'isomorphic-fetch';
import {Stack, Typography} from '@mui/joy';
import React from 'react';
import {createRoot} from 'react-dom/client';
import {set, defer, once} from 'lodash';
import 'styles/global.css';

import {APP_LOAD, dispatchAppOptions, dispatchUpdateAppData} from './core/AppDataCntlr.js';
import {FireflyViewer} from './templates/fireflyviewer/FireflyViewer.js';
import {FireflySlate} from './templates/fireflyslate/FireflySlate.jsx';
import {LandingPage} from './templates/fireflyviewer/LandingPage.jsx';
import {LcViewer} from './templates/lightcurve/LcViewer.jsx';
import {HydraViewer} from './templates/hydra/HydraViewer.jsx';
import {routeEntry, ROUTER} from './templates/router/RouteHelper.jsx';
import {initApi} from './api/ApiBuild.js';
import {dispatchUpdateLayoutInfo} from './core/LayoutCntlr.js';
import {FireflyRoot} from './ui/FireflyRoot.jsx';
import {SIAv2SearchPanel} from './ui/tap/SIASearchRootPanel';
import {getSIAv2Services} from './ui/tap/SiaUtil';
import {TapSearchPanel} from './ui/tap/TapSearchRootPanel';
import {dispatchChangeReadoutPrefs} from './visualize/MouseReadoutCntlr.js';
import {showInfoPopup} from './ui/PopupUtil';
import {bootstrapRedux, flux} from './core/ReduxFlux.js';
import {getOrCreateWsConn} from './core/messaging/WebSocketClient.js';
import {ActionEventHandler} from './core/messaging/MessageHandlers.js';
import {getJsonProperty, notifyServerAppInit} from './rpc/CoreServices.js';
import {getPropsWith, mergeObjectOnly, getProp, toBoolean, documentReady,uuid} from './util/WebUtil.js';
import {dispatchChangeTableAutoScroll, dispatchWcsMatch, visRoot} from './visualize/ImagePlotCntlr.js';
import {Logger} from './util/Logger.js';
import {evaluateWebApi, initWebApi, isUsingWebApi, WebApiStat} from './api/WebApi.js';
import {WebApiHelpInfoPage} from './ui/WebApiHelpInfoPage.jsx';
import {dispatchOnAppReady} from './core/AppDataCntlr.js';
import {getBootstrapRegistry} from './core/BootstrapRegistry.js';
import {showLostConnection} from './ui/LostConnection.jsx';
import {recordHistory} from './core/History.js';
import {setDefaultImageColorTable} from './visualize/WebPlotRequest.js';
import {initWorkerContext} from './threadWorker/WorkerAccess.js';
import {getTAPServices} from './ui/tap/TapKnownServices.js';
import {loadAllJobs} from './core/background/BackgroundUtil.js';
import {
    makeDefImageSearchActions, makeDefTableSearchActions, makeDefTapSearchActions, makeExternalSearchActions
} from './ui/DefaultSearchActions.js';

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
    HydraViewer,
    [ROUTER]: ROUTER      // root component is passed in via getRouter
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

const IRSA_CAT= 'IRSA searches';
const ARCHIVE= 'Archive Searches';

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
 * @prop {PROP_SHEET} table.propertySheet - specifies how to show propertySheet
 */

/** @type {AppProps} */
const defAppProps = {
    div: 'app',
    template: undefined,        // don't set a default value for this.  it's also used as a switch for API vs UI mode
    appTitle: '',
    showUserInfo: false,
    showViewsSwitch: true,
    rightButtons: undefined,
    landingPage: <LandingPage/>,
    fileDropEventAction: 'FileUploadDropDownCmd',

    menu: [
        {label:'Images', action:'ImageSelectDropDownCmd', primary: true, category:IRSA_CAT},
        {label:'TAP', action: 'TAPSearch', primary: true, category: ARCHIVE},
        {label: 'SIAv2 Searches', action: 'SIAv2Search', primary:true, category: ARCHIVE},
        {label:'IRSA Catalogs', action: 'IrsaCatalog', primary: true, category:IRSA_CAT},
        {label:'VO SCS Search', action: 'ClassicVOCatalogPanelCmd', primary: false, category: ARCHIVE},
        {label:'NED', action: 'ClassicNedSearchCmd', primary: false, category:'NED Search'},
        {label:'Upload', action: 'FileUploadDropDownCmd', primary: true},
        {label:'HiPS Search', action: 'HiPSSearchPanel', primary: false, category:ARCHIVE},
        {label:'IRSA SIAv2', action: 'IRSA_USING_SIAv2', primary: false, category:IRSA_CAT},
    ],

    dropdownPanels: [
        <SIAv2SearchPanel lockService={true} lockedServiceName='IRSA' groupKey='IRSA_USING_SIAv2'
                        layout= {{width: '100%'}}
                          lockTitle='IRSA SIAv2 Search'
                        name='IRSA_USING_SIAv2'/>,
        ]
};

/** @type {FireflyOptions} */
const defFireflyOptions = {
    multiTableSearchCmdOptions: [
        {id: 'tap', title: 'TAP Searches'},
        {id: 'irsacat', title: 'IRSA Catalogs'},
        {id: 'vocat'},
        {id: 'nedcat'}
    ],
    theme: {
        customized: undefined,          // a function that returns a customized theme
        colorMode: undefined,           // can be 'dark' or 'light'.  When not specified(default), it will use device's settings.
    },
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
    'help.base.url': undefined,                     // onlinehelp base URL

    charts: {
        defaultDeletable: undefined,    // by default if there are more than one chart in container, all charts are deletable
        maxRowsForScatter: 5000,        // maximum table rows for scatter chart support, heatmap is created for larger tables
        minScatterGLRows: 1000,         // minimum number of points to use WebGL 'scattergl' instead of SVG 'scatter'
        singleTraceUI: false,           // by default we support multi-trace in UI
        upperLimitUI: false,            // by default user can not set upper limit column in scatter options
        allowPinnedCharts: false,        // true to use Chart container with 'pin' feature
        ui: {HistogramOptions: {fixedAlgorithm: undefined}}     // by default we allow both "uniform binning" and "bayesian blocks"
    },
    hips : {
        useForImageSearch: true,
        hipsSources: 'all',
        defHipsSources: {source: 'irsa', label: 'Featured'},
        mergedListPriority: 'irsa',
        mocMaxDepth : 5,
        mocDefaultStyle : 'DESTINATION_OUTLINE',
    },
    table : {
        pageSize: 100,
        showPropertySheetButton: false,  // by default, hide it because most applications have a dedicated property sheet component
        propertySheet: {
            selectableRows: true   // will only take effect if property sheet is displayed as table
        }
    },
    image : {
        defaultColorTable: 1,
        canCreateExtractionTable: false,
    },
    tapObsCore: {
        enableObsCoreDownload: true,
        // debug: true,
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
    searchActions : [
        ...makeExternalSearchActions(),
        ...makeDefTableSearchActions(),
        ...makeDefTapSearchActions(),
        ...makeDefImageSearchActions(),
    ],
    tap : {
        services: getTAPServices( ['IRSA', 'NED', 'NASA Exoplanet Archive', 'KOA', 'HEASARC', 'MAST Images',
                                   'CADC', 'VizieR (CDS)', 'Simbad (CDS)', 'Gaia', 'GAVO', 'HSA', 'NOIR Lab'] ),
        defaultMaxrec: 50000
    },
    SIAv2 : {
        services: getSIAv2Services( ['IRSA', 'CADC', ]),
        defaultMaxrec: 50000
    }
};






/**
 * add options to store and setup any options that need specific initialization
 * @param {Object} appSpecificOptions
 */
function installOptions(appSpecificOptions) {
    const options=  mergeObjectOnly(defFireflyOptions, appSpecificOptions); // app specific will override default
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
 * @param {FireflyOptions} appSpecificOptions
 * @param {Array.<WebApiCommand>} webApiCommands
 */
function fireflyInit(props, appSpecificOptions={}, webApiCommands) {

    if (initDone) return;

    const reactComponents= Object.entries(props).filter( ([,v]) => v?.props);
    const appProps = {...mergeObjectOnly(defAppProps, props), ...Object.fromEntries(reactComponents)}; // mergeObjectOnly does not handle react components correctly

    const viewer = Templates[appProps.template];

    if (viewer) {
        // in non API usages, renderTreeId is not used, this line is just for clarity
        appProps.renderTreeId = undefined;
    }
    else {
        // in API mode, show propertySheet popup button unless it's set.
        set(appSpecificOptions, 'table.showPropertySheetButton', appSpecificOptions?.table?.showPropertySheetButton ?? true);
    }

    installOptions(appSpecificOptions);

    // initialize UI or API depending on entry mode.
    documentReady().then(() => {
        viewer ? renderRoot(undefined, viewer, appProps,webApiCommands) : initApi(props);
    });
    initDone = true;
}

/*
 *
 * @param {string} divId
 * @param {AppProps} props
 * @return {Object} return object has two functions {unrender:Function, render:Function}
 */
export function startAsAppFromApi(divId, overrideProps={template: 'FireflySlate'}) {


    const Message = ({}) => (
        <Stack alignItems='center'>
            <Typography sx={{fontSize: 'xl4'}} color='neutral'> Welcome to Firefly Viewer for Python</Typography>
        </Stack>
    );

    const landingPage= (<LandingPage slotProps={{
        topSection: {component: Message},
        bottomSection : {
            actionItems: [
                { text: 'Use API to send data', subtext: 'load data using Python API' },
                { text: 'Search for data', subtext: 'using the tabs above or side menu' },
                { text: 'Upload a file', subtext: 'drag & drop here' }
            ]
        }
    }}/>);

    const props = {
        landingPage,
        ...mergeObjectOnly({...window.firefly.originalAppProps}, overrideProps), div:divId, appFromApi:true};
    const viewer = Templates[props.template];
    if (!divId || !viewer) {
        !divId  && logger.error('required: divId');
        !viewer && logger.error(`required: props.template, must be one of ${Object.keys(Templates).join()}`);
        return;
    }

    props.disableDefaultDropDown && dispatchUpdateLayoutInfo({disableDefaultDropDown:true});
    props.readoutDefaultPref && dispatchChangeReadoutPrefs(props.readoutDefaultPref);
    props.wcsMatchType && dispatchWcsMatch({matchType:props.wcsMatchType, lockMatch:true});
    props.apiHandlesExpanded= true;

    dispatchAppOptions({ charts: { allowPinnedCharts: true}});

    if (!props.menu) {
        const other= 'Other Searches';
        const general= 'General Searches';
        props.menu= [
            { label: 'Upload', action: 'FileUploadDropDownCmd', primary:true },
            { label: 'TAP Searches', action: 'TAPSearch', primary:true, category: general },
            { label: 'SIAv2 Searches', action: 'SIAv2Search', primary:true, category: general },
            { label: 'IRSA Images', action: 'ImageSelectDropDownSlateCmd', category: other },
            { label: 'IRSA Catalogs', action: 'IrsaCatalogDropDown', category: other },
        ];
    }


    const e= document.getElementById(divId);

    const makeControlObj= () => {
        let root = e && createRoot(e);
        return {
            render : () => root && renderRoot(root, viewer, props),
            unrender: () => {
                root?.unmount();
                root = e && createRoot(e);
            },
        };
    };
    const controlObj= makeControlObj();
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
 * @param {FireflyOptions} clientAppSpecificOptions - firefly options specific to this client
 * @param {Array.<WebApiCommand>} webApiCommands
 * @returns {Promise.<boolean>}
 */
function bootstrap(props, clientAppSpecificOptions, webApiCommands) {

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

        let srvAppSpecificOptions={};
        try {
            srvAppSpecificOptions= await getJsonProperty('FIREFLY_OPTIONS');
        }
        catch (err) {
            logger.error('could not retrieve valid server options');
        }
        const appSpecificOptions = mergeObjectOnly(clientAppSpecificOptions, srvAppSpecificOptions);

        const client= await getOrCreateWsConn(); // establish websocket connection first before doing anything else.

        fireflyInit(props, appSpecificOptions, webApiCommands);

        client.addListener(ActionEventHandler);
        window.firefly.wsClient = client;
        notifyServerAppInit({spaName:`${props.appTitle||''}--${props.template?props.template:'api'}`});
        loadAllJobs();
        resolve?.();


    }).then(() => {
        // when all is done, mark app as 'ready'
        defer(() => {
            setTimeout(() => {
                dispatchUpdateAppData({isReady: true})
            },3);
        });
        initWorkerContext();
    });
}

function renderRoot(root, viewer, props, webApiCommands) {
    const e= document.getElementById(props.div);
    if (!e) {
        showInfoPopup('HTML page is not setup correctly, Firefly cannot start.');
        logger.error(`DOM Element "${props.div}" is not found in the document, Firefly cannot start.`);
        return;
    }

    const rootToUse = root ?? createRoot(e);
    initWebApi(webApiCommands);
    const webApi= isUsingWebApi(webApiCommands);
    const doAppRender= () => {
        if (props.template === ROUTER) {
            routeEntry(rootToUse, props);
        } else {
            rootToUse.render(
                <FireflyRoot ctxProperties={props}>
                    { React.createElement(viewer, {...props, normalInit: !webApi})}
                </FireflyRoot>
            );
        }
    };
    webApi ? handleWebApi(webApiCommands, e, doAppRender) : doAppRender();
}


function handleWebApi(webApiCommands, e, doAppRender) {
    const {status, helpType, contextMessage, cmd, execute,
        params, badParams, missingParams}= evaluateWebApi(webApiCommands);
    switch (status) {
        case WebApiStat.EXECUTE_API_CMD:
            let apiCompleted= false;
            // window.history.pushState('home', 'Home', new URL(window.location).pathname); // ?? is this necessary?
            doAppRender();
            dispatchOnAppReady(() =>  {
                if (apiCompleted) return;
                execute?.(cmd,params);
                apiCompleted= true;
            });
            break;
        case WebApiStat.SHOW_HELP:
            createRoot(e).render(
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

