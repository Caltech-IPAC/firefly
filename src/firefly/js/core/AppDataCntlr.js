/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {get} from 'lodash';

import {flux} from '../Firefly.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import BrowserCache from '../util/BrowserCache.js';
import {menuReducer, alertsReducer} from './AppDataReducers.js';
import Point, {isValidPoint} from '../visualize/Point.js';
import {getModuleName, updateSet} from '../util/WebUtil.js';
import {getWsChannel} from './messaging/WebSocketClient.js';

export const APP_DATA_PATH = 'app_data';
export const COMMAND = 'COMMAND';
const APP_PREFERENCES= 'APP_PREFERENCES';

/*---------------------------- ACTIONS -----------------------------*/

export const APP_LOAD = `${APP_DATA_PATH}.appLoad`;
export const APP_UPDATE = `${APP_DATA_PATH}.appUpdate`;
export const ADD_TASK_COUNT = `${APP_DATA_PATH}.addTaskCount`;
export const REMOVE_TASK_COUNT = `${APP_DATA_PATH}.removeTaskCount`;
export const ACTIVE_TARGET = `${APP_DATA_PATH}.activeTarget`;

export const ADD_PREF = `${APP_DATA_PATH}.addPreference`;
export const REMOVE_PREF = `${APP_DATA_PATH}.removePreference`;
export const REINIT_RESULT_VIEW = `${APP_DATA_PATH}.reinitResultView`;
export const ROOT_URL_PATH = `${APP_DATA_PATH}.rootUrlPath`;
export const SET_ALERTS = `${APP_DATA_PATH}.setAlerts`;
export const HELP_LOAD = `${APP_DATA_PATH}.helpLoad`;

/** fired when there's a connection is added/removed from this channel.  useful for tracking connections in channel, etc   */
export const WS_CONN_UPDATED = `${APP_DATA_PATH}.wsConnUpdated`;

/** grab focus */
export const GRAB_WINDOW_FOCUS = `${APP_DATA_PATH}.grabFocus`;

/*---------------------------- CREATORS ----------------------------*/

export function loadAppData() {

    return function (dispatch) {
        dispatch({ type : APP_LOAD });
        fetchAppData(dispatch, 'fftools_v1.0.1 Beta', 2);
    };
}

export function grabWindowFocus() {
    return blinkWindowTitle;
}

export function onlineHelpLoad( action )
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

/**
 *
 * @param {AppData} appData The partial object to merge with the appData branch under root
 * @returns {Action}
 */
export function updateAppData(appData) {
    return { type : APP_UPDATE, payload: appData };
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
    if (isValidPoint(wp) && wp.type==Point.W_PT) {
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

function getInitState() {
    return {
        isReady : false,
        connections: {},      // channel:[] ... keyed by channel, contains an array of connId(s).
        activeTarget: null,
        rootUrlPath : null,
        taskCounters: [],
        commandState:{},   // key is command id, value is anything the action drops in, only stateful commands need this
        preferences:initPreferences()  // preferences, will be backed by local storage
    };
}

function initPreferences() {
    var prefs= BrowserCache.get(APP_PREFERENCES);
    return prefs || {};
}


export function reducer(state=getInitState(), action={}) {

    var nstate = appDataReducer(state, action);
    nstate = menuReducer(nstate, action);
    nstate = alertsReducer(nstate, action);

    return nstate;
}

function appDataReducer(state, action={}) {
    switch (action.type) {
        case APP_LOAD  :
            return getInitState();

        case APP_UPDATE  :
            return Object.assign({}, state, action.payload);

        case ACTIVE_TARGET  :
            return updateActiveTarget(state,action);

        case REMOVE_TASK_COUNT  :
            return removeTaskCount(state,action);

        case ADD_TASK_COUNT  :
            return addTaskCount(state,action);

        case ADD_PREF  :
            return addPreference(state,action);

        case REMOVE_PREF  :
            return removePreference(state,action);
        
        case ROOT_URL_PATH :
            return Object.assign({},state, {rootUrlPath:action.payload.rootUrlPath});

        case WS_CONN_UPDATED :
            return updateSet(state, ['connections'], action.payload);

        default:
            return state;
    }
}
/*---------------------------- DISPATCHERS -----------------------------*/


/**
 * 
 * @param set the root url path.  This allows use to create full urls from relative urls
 */
export function dispatchRootUrlPath(rootUrlPath) {
    flux.process({type: ROOT_URL_PATH, payload: {rootUrlPath}});
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
 * updates app-data.  This does a merge with the existing data.
 * @param appData
 * @returns {{type: string, payload: *}}
 */
export function dispatchUpdateAppData(appData) {
    flux.process({ type : APP_UPDATE, payload: appData });
}

/**
 * execute this callback when app is ready.
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

export function getMenu() {
    return get(flux.getState(), [APP_DATA_PATH, 'menu']);
}

export function getAlerts() {
    return get(flux.getState(), [APP_DATA_PATH, 'alerts'], {});
}
/*---------------------------- PRIVATE -----------------------------*/

/**
 * this saga does the following:
 * <ul>
 *     <li>watches for app_data.isReady
 *     <li>when isReady, it will execute the given callback with the current state
 * </ul>
 * @param callback  callback to execute when table is loaded.
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



/*---------------------------- REDUCING FUNTIONS -----------------------------*/
const updateActiveTarget= function(state,action) {
    var {worldPt,corners}= action.payload;
    if (!worldPt && !corners) return state;
    return Object.assign({}, state, {activeTarget:{worldPt,corners}});
};

const addTaskCount= function(state,action) {
    var {componentId,taskId}= action.payload;
    if (!componentId && !taskId) return state;
    var taskArray= state.taskCounters[componentId] | [];
    taskArray= [...taskArray,taskId];
    var taskCounters= Object.assign({}, taskCounters, {[componentId]:taskArray});
    return Object.assign({},state, {taskCounters});
};

const removeTaskCount= function(state,action) {
    var {componentId,taskId}= action.payload;
    if (!componentId && !taskId) return state;
    var taskArray= state.taskCounters[componentId] | [];
    taskArray= taskArray.filter( (id) => id!==taskId);
    var taskCounters= Object.assign({}, taskCounters, {[componentId]:taskArray});
    return Object.assign({},state, {taskCounters});
};

function addPreference(state,action) {
    if (!action.payload) return state;
    var {name,value}= action.payload;
    var preferences= Object.assign({},state.preferences,{[name]:value} );
    BrowserCache.put(APP_PREFERENCES,preferences);
    return Object.assign({},state,{preferences});
}

function removePreference(state,action) {
    if (!action.payload) return state;
    var {name}= action.payload;
    var preferences= Object.assign({},state.preferences);
    Reflect.deleteProperty(preferences,name);
    BrowserCache.put(APP_PREFERENCES,preferences);
    return Object.assign({},state,{preferences});
}

const blinkWindowTitle = ( () => {
    var oldTitle = oldTitle || document.title;
    var msg = 'Updated!';
    var timeoutId;
    var blink = () => { document.title = document.title == msg ? oldTitle : msg; };
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