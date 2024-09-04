/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, map, isUndefined, isEmpty} from 'lodash';
import {flux} from './ReduxFlux';
import {dispatchAddActionWatcher} from './MasterSaga';
import {appDataReducer, menuReducer, alertsReducer} from './AppDataReducers.js';
import Point, {isValidPoint} from '../visualize/Point.js';
import {getModuleName, getProp, getRootURL, isFullURL} from '../util/WebUtil.js';
import {dispatchRemoteAction} from './JsonUtils.js';
import {getWsConn} from './messaging/WebSocketClient';
import {getLayouInfo} from 'firefly/core/LayoutCntlr';

export const APP_DATA_PATH = 'app_data';
export const COMMAND = 'COMMAND';

/*---------------------------- ACTIONS -----------------------------*/

export const APP_LOAD = `${APP_DATA_PATH}.appLoad`;
export const REINIT_APP= `${APP_DATA_PATH}.reinitApp`;
export const APP_UPDATE = `${APP_DATA_PATH}.appUpdate`;
export const ADD_TASK_COUNT = `${APP_DATA_PATH}.addTaskCount`;
export const REMOVE_TASK_COUNT = `${APP_DATA_PATH}.removeTaskCount`;
export const ACTIVE_TARGET = `${APP_DATA_PATH}.activeTarget`;
export const APP_OPTIONS = `${APP_DATA_PATH}.appOptions`;

export const ADD_PREF = `${APP_DATA_PATH}.addPreference`;
export const REMOVE_PREF = `${APP_DATA_PATH}.removePreference`;
export const ROOT_URL_PATH = `${APP_DATA_PATH}.rootUrlPath`;
export const SET_ALERTS = `${APP_DATA_PATH}.setAlerts`;
export const SET_USER_INFO = `${APP_DATA_PATH}.setUserInfo`;
export const HELP_LOAD = `${APP_DATA_PATH}.helpLoad`;
export const LOAD_SEARCHES = `${APP_DATA_PATH}.loadSearches`;
export const NOTIFY_REMOTE_APP_READY = `${APP_DATA_PATH}.notifyRemoteAppReady`;
export const FORM_SUBMIT = `${APP_DATA_PATH}.formSubmit`;
export const FORM_CANCEL = `${APP_DATA_PATH}.formCancel`;

/** fired when there's a connection is added/removed from this channel.  useful for tracking connections in channel, etc   */
export const WS_CONN_UPDATED = `${APP_DATA_PATH}.wsConnUpdated`;

/** grab focus */
export const GRAB_WINDOW_FOCUS = `${APP_DATA_PATH}.grabFocus`;

/** the extension to add to a channel string to make a viewer channel */
const CHANNEL_VIEWER_EXTENSION = '__viewer';
const channel_matcher = new RegExp(`(.+)${CHANNEL_VIEWER_EXTENSION}(?:-(.+))?`)

/** @type {SearchInfo} */
const searchInfo = {};

export default {actionCreators, reducers};

function actionCreators() {
    return {
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
 * @param [replace=true] if true, replace the taskId in the list
 */
export function dispatchAddTaskCount(componentId,taskId, replace= true) {
    flux.process({type: ADD_TASK_COUNT, payload: {componentId,taskId, replace}});
}


let taskCnt= 0;
export function makeTaskId(key='task') {
    taskCnt++;
    return `${key}-${taskCnt}`;
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
 * @param {SearchInfo} searchInfo   search information
 */
export function dispatchLoadSearches(searchInfo) {
    flux.process({ type : LOAD_SEARCHES, payload:searchInfo});
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
 * Notify other apps this app is ready
 */
export function dispatchNotifyRemoteAppReady() {
    const channel= getWsChannel();
    if (!channel) return;
    const [, sourceChannel, app] = channel.match(channel_matcher) || [,channel];
    dispatchRemoteAction(sourceChannel,{ type : NOTIFY_REMOTE_APP_READY, payload: {ready:true, viewerChannel:channel}});
}

/**
 * execute this callback when app is ready.
 * @param {function} callback
 */
export function dispatchOnAppReady(callback) {
    if (isAppReady()) {
        callback && callback(flux.getState());
    } else {
        dispatchAddActionWatcher({actions: [APP_UPDATE, APP_LOAD], callback: doOnAppReady, params: {callback}});
    }
}

/**
 * Dispatch FORM_SUBMIT with the given payload.  This is only a notification action.  It does not do anything with the payload.
 * Payload can be a number of things.  See FormPanel for details.  But, this function can also be called outside of FormPanel
 * where a FORM_SUBMIT is intended.
 * @param {object} payload   data submitted.
 */
export function dispatchFormSubmit(payload) {
    flux.process({ type : FORM_SUBMIT, payload});
}

/**
 * Dispatch FORM_CANCEL with the given payload.  This is only a notification action.
 * @param {object} payload   data for identification.
 */
export function dispatchFormCancel(payload) {
    flux.process({ type : FORM_CANCEL, payload});
}

/*---------------------------- EXPORTED FUNCTIONS -----------------------------*/


/**
 * Given a channel string return a version of the will be the channel for a viewer
 * @param {String} channel
 * @return {string}
 */
export function makeViewerChannel(channel, file) {
    const endFile= isFullURL(file) ? new URL(file).pathname : file;
    const cleanFile = endFile ? '-' + endFile.replaceAll('.', '_') : '';
    return channel + CHANNEL_VIEWER_EXTENSION + cleanFile;
}

export function isAppReady() {
    return get(flux.getState(), [APP_DATA_PATH, 'isReady']);
}

export function getSearchInfo() {
    const {activeSearch} = get(flux.getState(), [APP_DATA_PATH, 'searches'], {});
    return Object.assign({}, searchInfo, {activeSearch});
}

export function getSearchByName(name) {
    const {allSearchItems={}} = getSearchInfo();
    return allSearchItems[name];
}

export function getMenu() {
    return get(flux.getState(), [APP_DATA_PATH, 'menu']);
}

export function getAlerts() {
    return get(flux.getState(), [APP_DATA_PATH, 'alerts'], {});
}

export const getActiveTarget= function() { return flux.getState()[APP_DATA_PATH].activeTarget; };

export function getTaskCount(componentId) {
    const state= flux.getState()[APP_DATA_PATH];
    return state.taskCounters[componentId] ? state.taskCounters[componentId].length : 0;
}

export function getPreference(name, def) {
    const v= flux.getState()[APP_DATA_PATH].preferences[name];
    if (isUndefined(v)) return def;
    return v;
}

export function getRootUrlPath() {
    return flux.getState()[APP_DATA_PATH].rootUrlPath;
}

export function getAppOptions() {
    return flux.getState()[APP_DATA_PATH].appOptions ?? window.firefly?.options ?? {};
}

export function getUserInfo() {
    return flux.getState()[APP_DATA_PATH].userInfo;
}

export function getSearchActions() {
    const {searchActions, searchActionsCmdMask}= getAppOptions() ?? {};
    if (isEmpty(searchActions)) return undefined;
    if (isEmpty(searchActionsCmdMask)) return searchActions;
    const retSearchActions= searchActions.filter( ({cmd}) => searchActionsCmdMask.includes(cmd));
    return retSearchActions;
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
 * @param [corners] array of 4 WorldPts that represent the corners of a image
 */
export const dispatchActiveTarget= function(wp,corners=undefined) {
    const payload={};
    if (isValidPoint(wp) && wp.type===Point.W_PT) {
        payload.worldPt= wp;
    }
    else if (!wp) {
        payload.worldPt= wp;
    }
    if (corners) {
        payload.corners= corners;
    }
    if (Object.keys(payload).length) {
        flux.process({type: ACTIVE_TARGET, payload});
    }
};

/**
 * @param baseUrl   the baseUrl of the websocket connection.  Defaults to current location.
 * @returns {string}  the channel websocket is connected to.
 */
export function getWsChannel(baseUrl) {
    return getWsConn(baseUrl)?.channel;
}

/**
 * @param baseUrl   the baseUrl of the websocket connection.  Defaults to current location.
 * @returns {string} the connection ID websocket is connected to.
 */
export function getWsConnId(baseUrl) {
    return getWsConn(baseUrl)?.connId;
}

/**
 * Returns the name/action of the select menu item.  If not set, it will determine what it should be
 * @param {object} menu
 * @param {object} dropDown
 * @returns {string} the name/action selected menu item
 */
export function getSelectedMenuItem(menu,dropDown) {
    menu ??= getMenu();
    dropDown ??= getLayouInfo()?.dropDown;
    // if (!menu || !dropDown?.visible) return '';
    if (!menu) return '';
    if (menu.selected) return menu.selected;
    let selected = menu.menuItems?.find(({action}) => (action===dropDown?.view))?.action;
    if (!selected && dropDown?.visible) selected = menu.menuItems[0].action;
    return selected;
}


/*---------------------------- REDUCERS -----------------------------*/

function reducer(state={}, action={}) {

    var nstate = appDataReducer(state, action);
    nstate = menuReducer(nstate, action);
    nstate = alertsReducer(nstate, action);

    return nstate;
}

/*---------------------------- CREATORS ----------------------------*/

function grabWindowFocus() {
    return blinkWindowTitle;
}

function onlineHelpLoad( action )
{
    return () => {
        let url = getAppOptions()?.['help.base.url'] || getProp('help.base.url');
        url = url.endsWith('/') ? url : url + '/';

        const {helpId, isDarkMode} = action?.payload || {};
        if (isDarkMode) url += '?mode=dark';
        if (helpId)     url += '#id=' + helpId;

        url = new URL(url, getRootURL());                   // use rootURL instead of document.baseURI if relative
        const moduleName = getProp('help.subpath', getModuleName());
        const windowName = `onlineHelp-${moduleName}`;
        window.open(url.href, windowName);
    };
}

function loadSearches(action) {
    return function (dispatch) {
        var {groups=[], activeSearch, renderAsMenuItems=false, flow, ...rest} = action.payload;
        groups.forEach( (g) => {
            Object.entries(get(g, 'searchItems',{}))
                  .forEach(([k,v]) => v.name = k);      // insert key as name into the search object.
        });
        activeSearch = activeSearch || get(Object.values(get(groups, [0, 'searchItems'], {})), [0, 'name']);    // defaults to first searchItem
        const allSearchItems = Object.assign({}, ...map(groups, 'searchItems'));
        if (renderAsMenuItems) flow ??= 'hidden';

        Object.assign(searchInfo, rest, {flow, renderAsMenuItems, allSearchItems, groups});
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
 * Action watcher callback. Watches for app_data.isReady.
 * When app data are loaded, it will execute the callback passed with params with the current state.
 * @callback actionWatcherCallback
 * @param action
 * @param cancelSelf
 * @param params
 * @param {function} params.callback function to execute on completion
 * @param {function} dispatch
 * @param {function} getState
 */
function doOnAppReady(action, cancelSelf, params={}, dispatch, getState) {
    if (isAppReady()) {
        cancelSelf();
        params.callback && params.callback(getState());
    }
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

