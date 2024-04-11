/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {MENU_UPDATE, SHOW_DROPDOWN} from './LayoutCntlr.js';
import * as AppDataCntlr from './AppDataCntlr.js';
import {mergeObjectOnly, updateSet} from '../util/WebUtil.js';
import BrowserCache from '../util/BrowserCache.js';
import {smartMerge} from '../tables/TableUtil.js';

const APP_PREFERENCES= 'APP_PREFERENCES';

export function appDataReducer(state, action={}) {
    switch (action.type) {
        case AppDataCntlr.APP_LOAD  :
            return getInitState();

        case AppDataCntlr.APP_UPDATE  :
            return smartMerge(state, action.payload);

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
            return {...state, appOptions:mergeObjectOnly(state.appOptions, action.payload.appOptions)};
        default:
            return state;
    }
}



export function menuReducer(state={}, action={}) {
    switch (action.type) {
        case SHOW_DROPDOWN  :
            const {visible, view=''} = action.payload;
            const selected = visible ? view : '';
            return updateSet(state, ['menu', 'selected'], selected);
        case MENU_UPDATE:
            const {menu} = action.payload;
            return updateSet(state, ['menu'], menu);
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



/*---------------------------- REDUCING FUNCTIONS -----------------------------*/
const updateActiveTarget= function(state,action) {
    const {worldPt,corners}= action.payload;
    return Object.assign({}, state, {activeTarget:{worldPt,corners}});
};

const addTaskCount= function(state,action) {
    const {componentId,taskId, replace}= action.payload;
    if (!componentId && !taskId) return state;
    let taskArray= state.taskCounters[componentId] || [];
    if (replace) {
        taskArray= taskArray.includes(taskId) ? taskArray : [...taskArray,taskId];
    }
    else {
        taskArray= [...taskArray,taskId];
    }
    const taskCounters= Object.assign({}, state.taskCounters, {[componentId]:taskArray});
    return Object.assign({},state, {taskCounters});
};

const removeTaskCount= function(state,action) {
    const {componentId,taskId}= action.payload;
    if (!componentId && !taskId) return state;
    let taskArray= state.taskCounters[componentId] || [];
    taskArray= taskArray.filter( (id) => id!==taskId);
    const taskCounters= Object.assign({}, state.taskCounters, {[componentId]:taskArray});
    return Object.assign({},state, {taskCounters});
};

function addPreference(state,action) {
    if (!action.payload) return state;
    const {name,value}= action.payload;
    const preferences= Object.assign({},state.preferences,{[name]:value} );
    BrowserCache.put(APP_PREFERENCES,preferences);
    return Object.assign({},state,{preferences});
}

function removePreference(state,action) {
    if (!action.payload) return state;
    const {name}= action.payload;
    const preferences= Object.assign({},state.preferences);
    Reflect.deleteProperty(preferences,name);
    BrowserCache.put(APP_PREFERENCES,preferences);
    return Object.assign({},state,{preferences});
}

function setUserInfo(state,action) {
    const userInfo = action.payload;
    return Object.assign({}, state, {userInfo});
}


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
     * @prop {Object} menu  
     * @prop {Object} alerts  ie. system notification messages
     * @prop {SearchInfo} searches define searches available to this application.
     */
    return {
        isReady : false,
        connections: {},      // channel:[] ... keyed by channel, contains an array of connId(s).
        activeTarget: null,
        rootUrlPath : null,
        taskCounters: [],
        commandState:{},   // key is command id, value is anything the action drops in, only stateful commands need this
        preferences:initPreferences(),  // preferences, will be backed by local storage
        appOptions : {},
        menu: {},
        alerts: {},
        searches: {}
    };
}

function initPreferences() {
    const prefs= BrowserCache.get(APP_PREFERENCES);
    return prefs || {};
}

