/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {get, map} from 'lodash';

import {flux} from '../Firefly.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {appDataReducer, menuReducer, alertsReducer} from './AppDataReducers.js';
import Point, {isValidPoint} from '../visualize/Point.js';
import {getModuleName} from '../util/WebUtil.js';
import {getWsChannel} from './messaging/WebSocketClient.js';

export const APP_DATA_PATH = 'app_data';
export const COMMAND = 'COMMAND';

/*---------------------------- ACTIONS -----------------------------*/

export const APP_LOAD = `${APP_DATA_PATH}.appLoad`;
export const APP_UPDATE = `${APP_DATA_PATH}.appUpdate`;
export const ADD_TASK_COUNT = `${APP_DATA_PATH}.addTaskCount`;
export const REMOVE_TASK_COUNT = `${APP_DATA_PATH}.removeTaskCount`;
export const ACTIVE_TARGET = `${APP_DATA_PATH}.activeTarget`;
export const APP_OPTIONS = `${APP_DATA_PATH}.appOptions`;

export const ADD_PREF = `${APP_DATA_PATH}.addPreference`;
export const REMOVE_PREF = `${APP_DATA_PATH}.removePreference`;
export const REINIT_RESULT_VIEW = `${APP_DATA_PATH}.reinitResultView`;
export const ROOT_URL_PATH = `${APP_DATA_PATH}.rootUrlPath`;
export const SET_ALERTS = `${APP_DATA_PATH}.setAlerts`;
export const HELP_LOAD = `${APP_DATA_PATH}.helpLoad`;
export const LOAD_SEARCHES = `${APP_DATA_PATH}.loadSearches`;

/** fired when there's a connection is added/removed from this channel.  useful for tracking connections in channel, etc   */
export const WS_CONN_UPDATED = `${APP_DATA_PATH}.wsConnUpdated`;

/** grab focus */
export const GRAB_WINDOW_FOCUS = `${APP_DATA_PATH}.grabFocus`;


/** @type {SearchInfo} */
const searchInfo = {};

export default {actionCreators, reducers};

function actionCreators() {
    return {
        [APP_LOAD]:     loadAppData,
        [GRAB_WINDOW_FOCUS]:     grabWindowFocus,
        [HELP_LOAD]:  onlineHelpLoad,
        [LOAD_SEARCHES]:  loadSearches
    };
}

function reducers() {
    return {
        [APP_DATA_PATH]: reducer
    };
}

/*---------------------------- DISPATCHERS -----------------------------*/
/**
 *
 * @param {string} rootUrlPath set the root url path.  This allows use to create full urls from relative urls
 */
export function dispatchRootUrlPath(rootUrlPath) {
    flux.process({type: ROOT_URL_PATH, payload: {rootUrlPath}});
}

/**
 * set the options defined for the app that were set in the html file
 * @param {AppOptions} appOptions the options for thei app
 */
export function dispatchAppOptions(appOptions) {
    flux.process({type: APP_OPTIONS, payload: {appOptions}});
}


/**
 * @param componentId the id or array of ids of the component to record the task count
 * @param taskId id of task, you create with makeTaskId()
 */
export function dispatchAddTaskCount(componentId,taskId) {
    flux.process({type: ADD_TASK_COUNT, payload: {componentId,taskId}});
}

/**
 * @param componentId the id or array of ids of the component to record the task count
 * @param taskId id of task, create with makeTaskId()
 */
export function dispatchRemoveTaskCount(componentId,taskId) {
    flux.process({type: REMOVE_TASK_COUNT, payload: {componentId,taskId}});
}

/**
 *
 * @param name name of preference
 * @param value value of preference
 */
export function dispatchAddPreference(name,value) {
    flux.process({type: ADD_PREF, payload: {name,value}});
}

/**
 *
 * @param name name of preference
 */
export function dispatchRemovePreference(name) {
    flux.process({type: REMOVE_PREF, payload: {name}});
}

/**
 *
 * @param menu the menu object to set.
 */
export function dispatchSetMenu(menu) {
    flux.process({ type : APP_UPDATE, payload: {menu} });
}

/**
 * Load search info into the application
 * @param {Object[]} p  dispatch parameters
 * @param {Search[]} p.title      the title or name of this group.
 * @param {Search[]} p.searches   an array of searches.
 * @param {string}   [activeSearch] the current selected search.  defaults to the first search.
 */
export function dispatchLoadSearches(groups, activeSearch) {
    flux.process({ type : LOAD_SEARCHES, payload: {groups, activeSearch} });
}

/**
 * updates app-data.  This does a merge with the existing data.
 * @param appData
 * @returns {{type: string, payload: *}}
 */
export function dispatchUpdateAppData(appData) {
    flux.process({ type : APP_UPDATE, payload: appData });
}

/**
 * execute this callback when app is ready.
 * @param {function} callback
 */
export function dispatchOnAppReady(callback) {
    if (isAppReady()) {
        callback && callback(flux.getState());
    } else {
        dispatchAddSaga(doOnAppReady, callback);
    }
}


/*---------------------------- EXPORTED FUNTIONS -----------------------------*/
export function isAppReady() {
    const gwtReady = !get(window, 'firefly.use_gwt', false) ||
        get(flux.getState(), [APP_DATA_PATH, 'gwtLoaded']);

    return getWsChannel() && get(flux.getState(), [APP_DATA_PATH, 'isReady']) && gwtReady;
}

export function getSearchInfo() {
    const {activeSearch} = get(flux.getState(), [APP_DATA_PATH, 'searches']);
    return Object.assign({}, searchInfo, {activeSearch});
}

export function getMenu() {
    return get(flux.getState(), [APP_DATA_PATH, 'menu']);
}

export function getAlerts() {
    return get(flux.getState(), [APP_DATA_PATH, 'alerts'], {});
}

export const getActiveTarget= function() { return flux.getState()[APP_DATA_PATH].activeTarget; };

export function getTaskCount(componentId) {
    var state= flux.getState()[APP_DATA_PATH];
    return state.taskCounters[componentId] ? state.taskCounters[componentId] : 0;
}

export function getPreference(name) {
    return flux.getState()[APP_DATA_PATH].preferences[name];
}

export function getRootUrlPath() {
    return flux.getState()[APP_DATA_PATH].rootUrlPath;
}

export function getAppOptions() {
    return flux.getState()[APP_DATA_PATH].appOptions;
}

/**
 * @param channel
 * @returns {number}  the number of connections/clients connected to the given channel
 */
export function getConnectionCount(channel) {
    return get(flux.getState(), [APP_DATA_PATH, 'connections', channel, 'length'], 0);
}

/**
 * @param wp center WorldPt
 * @param corners array of 4 WorldPts that represent the corners of a image
 */
export const dispatchActiveTarget= function(wp,corners) {
    var payload={};
    if (isValidPoint(wp) && wp.type===Point.W_PT) {
        payload.worldPt= wp;
    }
    if (corners) {
        payload.corners= corners;
    }
    if (Object.keys(payload).length) {
        flux.process({type: ACTIVE_TARGET, payload});
    }
};

/*---------------------------- REDUCERS -----------------------------*/

function reducer(state={}, action={}) {

    var nstate = appDataReducer(state, action);
    nstate = menuReducer(nstate, action);
    nstate = alertsReducer(nstate, action);

    return nstate;
}

/*---------------------------- CREATORS ----------------------------*/

function loadAppData() {

    return function (dispatch) {
        dispatch({ type : APP_LOAD });
        fetchAppData(dispatch, 'fftools_v1.0.1 Beta', 2);
    };
}

function grabWindowFocus() {
    return blinkWindowTitle;
}

function onlineHelpLoad( action )
{
    return () => {
        /*global __$help_base_url*/
        var url = typeof __$help_base_url === 'undefined' ? 'unknown' : __$help_base_url;       // this is a global var.. ending with '/'
        var windowName = 'onlineHelp';
        var moduleName = getModuleName();

        if (moduleName) {
            url +=  moduleName;
            windowName += '-' + moduleName;
        }

        if (action.payload && action.payload.helpId) {
            url += '/#id=' + action.payload.helpId;
        } else {
            url += '/';
        }

        if (url) {
            window.open(url, windowName);
        }
    };
}

function loadSearches(action) {
    return function (dispatch) {
        var {groups=[], activeSearch} = action.payload;
        groups.forEach( (g) => {
            Object.entries(get(g, 'searchItems',{}))
                  .forEach(([k,v]) => v.name = k);      // insert key as name into the search object.
        });
        activeSearch = activeSearch || get(Object.values(get(groups, [0, 'searchItems'], {})), [0, 'name']);    // defaults to first searchItem
        const allSearchItems = Object.assign({}, ...map(groups, 'searchItems'));
        Object.assign(searchInfo, {allSearchItems, groups});
        dispatch({ type : APP_UPDATE, payload: {searches: {activeSearch}}});
    };
}

/*---------------------------- PRIVATE -----------------------------*/

/**
 *
 * @param {AppData} appData The partial object to merge with the appData branch under root
 * @returns {Action}
 */
function updateAppData(appData) {
    return { type : APP_UPDATE, payload: appData };
}

/**
 * This saga watches for app_data.isReady.  
 * When that happens, it will execute the given callback with the current state. 
 * @param {function} callback
 * @param {function} dispatch
 * @param {function} getState
 */
function* doOnAppReady(callback, dispatch, getState) {

    var isReady = isAppReady();
    while (!isReady) {
        yield take([APP_UPDATE, APP_LOAD]);
        isReady = isAppReady();
    }
    callback && callback(getState());
}

/**
 * fetches all of the necessary data to construct app_data.
 * set isReady to true once done.
 * @param dispatch
 */
function fetchAppData(dispatch) {
    dispatch(updateAppData(
        {
            isReady: true,
        }));
}

const blinkWindowTitle = ( () => {
    var oldTitle = oldTitle || document.title;
    var msg = 'Updated!';
    var timeoutId;
    var blink = () => { document.title = document.title === msg ? oldTitle : msg; };
    var clear = () => {
        clearInterval(timeoutId);
        document.title = oldTitle;
        window.onmousemove = null;
        timeoutId = null;
    };
    return function () {
        if (!timeoutId) {
            timeoutId = setInterval(blink, 1000);
            window.onmousemove = clear;
        }
    };
})();

