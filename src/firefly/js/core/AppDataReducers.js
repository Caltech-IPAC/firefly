/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {SHOW_DROPDOWN} from './LayoutCntlr.js';
import * as AppDataCntlr from './AppDataCntlr.js';
import {updateSet} from '../util/WebUtil.js';
import BrowserCache from '../util/BrowserCache.js';

const APP_PREFERENCES= 'APP_PREFERENCES';

export function appDataReducer(state, action={}) {
    switch (action.type) {
        case AppDataCntlr.APP_LOAD  :
            return getInitState();

        case AppDataCntlr.APP_UPDATE  :
            return Object.assign({}, state, action.payload);

        case AppDataCntlr.ACTIVE_TARGET  :
            return updateActiveTarget(state,action);

        case AppDataCntlr.REMOVE_TASK_COUNT  :
            return removeTaskCount(state,action);

        case AppDataCntlr.ADD_TASK_COUNT  :
            return addTaskCount(state,action);

        case AppDataCntlr.ADD_PREF  :
            return addPreference(state,action);

        case AppDataCntlr.REMOVE_PREF  :
            return removePreference(state,action);

        case AppDataCntlr.SET_USER_INFO  :
            return setUserInfo(state,action);

        case AppDataCntlr.ROOT_URL_PATH :
            return Object.assign({},state, {rootUrlPath:action.payload.rootUrlPath});

        case AppDataCntlr.WS_CONN_UPDATED :
            return updateSet(state, ['connections'], action.payload);

        case AppDataCntlr.APP_OPTIONS :
            return updateSet(state, ['appOptions'], action.payload.appOptions);

        default:
            return state;
    }
}



export function menuReducer(state={}, action={}) {
    switch (action.type) {
        case SHOW_DROPDOWN  :
            const {visible, view=''} = action.payload;
            const selected = visible ? view : '';
            return Object.assign({}, state, {selected});

        default:
            return state;
    }
}

export function alertsReducer(state={}, action={}) {
    switch (action.type) {
        case AppDataCntlr.SET_ALERTS :
            const {msg=''} = action.payload || {};
            return updateSet(state, ['alerts'], {msg});
        default:
            return state;
    }
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

function setUserInfo(state,action) {
    const userInfo = action.payload;
    return Object.assign({}, state, {userInfo});
};


/**
 *
 * @returns {AppDataStore}
 */
function getInitState() {


    /**
     * @global
     * @public
     * @typedef {Object} AppDataStore
     *
     * @summary Information about the core of the application
     *
     * @prop {boolean} isReady : false,
     * @prop {Object.<String,Array>} connections  channel:[] ... keyed by channel, contains an array of connId(s).
     * @prop {WorldPt} activeTarget
     * @prop {string} rootUrlPath
     * @prop {Array} taskCounters
     * @prop {Object} commandState
     * @prop {Object.<String,String>} preferences,
     * @prop {Object} appOptions : {}
     */
    return {
        isReady : false,
        connections: {},      // channel:[] ... keyed by channel, contains an array of connId(s).
        activeTarget: null,
        rootUrlPath : null,
        taskCounters: [],
        commandState:{},   // key is command id, value is anything the action drops in, only stateful commands need this
        preferences:initPreferences(),  // preferences, will be backed by local storage
        appOptions : {}
    };
}

function initPreferences() {
    var prefs= BrowserCache.get(APP_PREFERENCES);
    return prefs || {};
}

