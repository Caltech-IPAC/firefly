/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {get} from 'lodash'; 
import strLeft from 'underscore.string/strLeft';
import strRight from 'underscore.string/strRight';

import {flux} from '../Firefly.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import BrowserCache from '../util/BrowserCache.js';
import menuRenderer from './reducers/MenuReducer.js';
import {fetchUrl} from '../util/WebUtil.js';
import Point, {isValidPoint} from '../visualize/Point.js';
import {getModuleName} from '../util/WebUtil.js';

export const APP_DATA_PATH = 'app_data';
export const COMMAND = 'COMMAND';
const TASK= 'task-';
const APP_PREFERENCES= 'APP_PREFERENCES';
var taskCnt=0;

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

//const HELP_LOAD = `${APP_DATA_PATH}.helpLoad`;
export const HELP_LOAD = 'overviewHelp';    //note: consistent with AppMenu.prop

/*---------------------------- CREATORS ----------------------------*/

const makeTaskId= function() {
    taskCnt++;
    return TASK+taskCnt++;
};

export const updateActiveTarget= function(state,action) {
    var {worldPt,corners}= action;
    if (!worldPt || !corners) return state;
    return Object.assign({}, state, {activeTarget:{worldPt,corners}});
};


export const addTaskCount= function(state,action) {
    var {componentId,taskId}= action.payload;
    if (!componentId && !taskId) return state;
    var taskArray= state.taskCounters[componentId] | [];
    taskArray= [...taskArray,taskId];
    var taskCounters= Object.assign({}, taskCounters, {[componentId]:taskArray});
    return Object.assign({},state, {taskCounters});
};

export const removeTaskCount= function(state,action) {
    var {componentId,taskId}= action.payload;
    if (!componentId && !taskId) return state;
    var taskArray= state.taskCounters[componentId] | [];
    taskArray= taskArray.filter( (id) => id!==taskId);
    var taskCounters= Object.assign({}, taskCounters, {[componentId]:taskArray});
    return Object.assign({},state, {taskCounters});
};

export function getCommandState(stateId) {
    return flux.getState()[APP_DATA_PATH].commandState[stateId];
}



export function addPreference(state,action) {
    if (!action.payload) return state;
    var {name,value}= action.payload;
    var preferences= Object.assign({},state.preferences,{[name]:value} );
    BrowserCache.put(APP_PREFERENCES,preferences);
    return Object.assign({},state,{preferences});
}

export function removePreference(state,action) {
    if (!action.payload) return state;
    var {name}= action.payload;
    var preferences= Object.assign({},state.preferences);
    Reflect.deleteProperty(preferences,name);
    BrowserCache.put(APP_PREFERENCES,preferences);
    return Object.assign({},state,{preferences});
}

export function loadAppData() {

    return function (dispatch) {
        dispatch({ type : APP_LOAD });
        fetchAppData(dispatch, 'fftools_v1.0.1 Beta', 2);
    };
}

export function onlineHelpLoad( action )
{
    return () => {
        var url = flux.getState()[APP_DATA_PATH].props['help.base.url'];  // ending with '/'
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
 * @param appData {Object} The partial object to merge with the appData branch under root
 * @returns {{type: string, payload: object}}
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

    if (action.type && !action.type.startsWith(APP_DATA_PATH)) return state;

    var newState = appDataReducer(state, action);

    var menu = menuRenderer.reducer(newState.menu, action);

    return mergeAll(state, newState, {menu});
}

function mergeAll(orig, newval, updates) {

    var hasChanged = orig !== newval;
    hasChanged = hasChanged || Object.keys(updates).reduce( (prev, next) => prev || orig[next] != updates[next], false);

    return hasChanged ? Object.assign({}, newval, updates) : orig;
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
    return get(flux.getState(), [APP_DATA_PATH, 'isReady']) &&
           get(flux.getState(), [APP_DATA_PATH, 'gwtLoaded']);
}

export function getMenu() {
    return get(flux.getState(), [APP_DATA_PATH, 'menu']);
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
    Promise.all( [loadProperties()] )
        .then(function (results) {
            const props = results[0];
            dispatch(updateAppData(
                {
                    isReady: true,
                    // menu: makeMenu(props),
                    props
                }));
        })
        .catch(function (reason) {
            console.log('Fail', reason);
        });
}

/**
 * returns a Promise containing the properties object.
 */
function loadProperties() {

    return fetchUrl('servlet/FireFly_PropertyDownload').then( (response) => {
        return response.text().then( (text) => {
            const lines = text.split( '\n' ).filter( (val) => !val.trim().startsWith('#') );
            const props = {};
            lines.forEach( (line) => {
                if (line.indexOf('=')) {
                    props[strLeft(line, '=').trim()] = strRight(line, '=').trim().replace(/\\(?=[\=!:#])/g, '');
                }
            } );
            return props;
        });
    }).catch(function(err) {
        return new Error(`Unable to load properties: ${err}`);
    });
}

/**
 *
 * @param props
 * @returns {{selected: string, menuItems: Array}}
 */
function makeMenu(props) {
    var menuItems = [];
    var selected = '';
    var items = props['AppMenu.Items'] || '';
    items.split(/\s+/).forEach( (action) => {
        const label = props[`${action}.Title`];
        const desc = props[`${action}.ShortDescription`];
        const icon = props[`${action}.Icon`];
        const type = props[`${action}.ToolbarButtonType`] || '';
        menuItems.push({label, action, icon, desc, type});
    });
    return {selected, menuItems};
}

