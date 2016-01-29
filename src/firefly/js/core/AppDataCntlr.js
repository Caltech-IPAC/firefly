/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {pick, isUndefined} from 'lodash';

import {flux} from '../Firefly.js';
import BrowserCache from '../util/BrowserCache.js';
import history from './History.js';
import layoutReducer from './reducers/LayoutReducer.js';
import menuRenderer from './reducers/MenuReducer.js';
import strLeft from 'underscore.string/strLeft';
import strRight from 'underscore.string/strRight';
import {fetchUrl} from '../util/WebUtil.js';
import Point, {isValidPoint} from '../visualize/Point.js';

const APP_DATA_PATH = 'app-data';
const SEARCH_TYPE = 'search';
const TASK= 'task-';
const APP_PREFERENCES= 'APP_PREFERENCES';
var taskCnt=0;

/*---------------------------- ACTIONS -----------------------------*/

const APP_LOAD = `${APP_DATA_PATH}.appLoad`;
const APP_UPDATE = `${APP_DATA_PATH}.appUpdate`;
const SHOW_DIALOG = `${APP_DATA_PATH}.showDialog`;
const HIDE_DIALOG = `${APP_DATA_PATH}.hideDialog`;
const ADD_TASK_COUNT = `${APP_DATA_PATH}.addTaskCount`;
const REMOVE_TASK_COUNT = `${APP_DATA_PATH}.removeTaskCount`;
const HIDE_ALL_DIALOGS = `${APP_DATA_PATH}.hideAllDialogs`;
const ACTIVE_TARGET = `${APP_DATA_PATH}.activeTarget`;

const SHOW_SEARCH       = `${APP_DATA_PATH}.searchShow`;
const UPDATE_LAYOUT     = `${APP_DATA_PATH}.updateLayout`;

const DISPLAY_MODE_CHANGE   = `${APP_DATA_PATH}.displayModeChange`;

const ADD_PREF = `${APP_DATA_PATH}.addPreference`;
const REMOVE_PREF = `${APP_DATA_PATH}.removePreference`;


/*---------------------------- CREATORS ----------------------------*/

const showDialog= function(dialogId,ownerId=undefined) {
    flux.process({type: SHOW_DIALOG, payload: {dialogId, ownerId}});
};

const hideDialog= function(dialogId) {
    flux.process({type: HIDE_DIALOG, payload: {dialogId}});
};

const hideAllDialogs= function() {
    flux.process({type: HIDE_ALL_DIALOGS, payload: {}});
};

const makeTaskId= function() {
    taskCnt++;
    return TASK+taskCnt++;
};


const updateActiveTarget= function(state,action) {
    var {worldPt,corners}= action;
    if (!worldPt || !corners) return state;
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

function getCommandState(stateId) {
    return flux.getState()[APP_DATA_PATH].commandState[stateId];
}


const showDialogChange= function(state,action) {
    if (!action.payload || !action.payload.dialogId) return state;
    var {dialogId,ownerId}= action.payload;

    state= Object.assign({},state);

    if (!state.dialogs) state.dialogs= {};

    if (!state.dialogs[dialogId]) {
        state.dialogs[dialogId]= {visible:false};
    }

    if (!state.dialogs[dialogId].visible || ownerId!==state.dialogs[dialogId].ownerId) {
        state.dialogs[dialogId].visible= true;
        if (ownerId) state.dialogs[dialogId].ownerId= ownerId;
    }
    return state;
};

const hideDialogChange= function(state,action) {
    if (!action.payload) return state;
    var {dialogId}= action.payload;
    if (!dialogId || !state.dialogs || !state.dialogs[dialogId]) return state;

    if (state.dialogs[dialogId].visible) {
        state= Object.assign({},state);
        state.dialogs[dialogId]= {visible: false};
    }
    return state;
};

const hideAllDialogsChange= function(state) {
    if (!state.dialogs) return state;
    Object.keys(state.dialogs).forEach( (dialog) => { dialog.visible=false; } );
    return Object.assign({}, state);
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

function loadAppData() {

    return function (dispatch) {
        dispatch({ type : APP_LOAD });
        fetchAppData(dispatch, 'fftools_v1.0.1 Beta', 2);
    };
}

/**
 *
 * @param appData {Object} The partial object to merge with the appData branch under root
 * @returns {{type: string, payload: object}}
 */
function updateAppData(appData) {
    return { type : APP_UPDATE, payload: appData };
}

function isDialogVisible(dialogKey) {
    var dialogs= flux.getState()[APP_DATA_PATH].dialogs;
    return (dialogs && dialogs[dialogKey] && dialogs[dialogKey].visible) ? true : false;
}

function getDialogOwner(dialogKey) {
    var dialogs= flux.getState()[APP_DATA_PATH].dialogs;
    if (!dialogs || !dialogs[dialogKey]) return null;
    var {visible,ownerId}= dialogs[dialogKey];
    return (visible && ownerId) ? ownerId : null;
}

export const getActiveTarget= function() { return flux.getState()[APP_DATA_PATH].activeTarget; };

export function getTaskCount(componentId) {
    var state= flux.getState()[APP_DATA_PATH];
    return state.taskCounters[componentId] ? state.taskCounters[componentId] : 0;
}

function getPreference(name) {
    return flux.getState()[APP_DATA_PATH].preferences[name];
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
        taskCounters: [],
        dialogs: {},      // key is dialog id, value is object {visible:true/false} maybe more in this object in future
        commandState:{},   // key is command id, value is anything the action drops in, only stateful commands need this
        preferences:initPreferences()  // preferences, will be backed by local storage
    };
}

function initPreferences() {
    var prefs= BrowserCache.get(APP_PREFERENCES);
    return prefs || {};
}


function reducer(state=getInitState(), action={}) {

    history.add(state, action);

    var newState = addDataReducer(state, action);

    var menu = menuRenderer.reducer(newState.menu, action);
    var layoutInfo = layoutReducer.reducer(newState.layoutInfo, action, menu);

    return mergeAll(state, newState, {menu, layoutInfo});
}

function mergeAll(orig, newval, updates) {

    var hasChanged = orig != newval;
    hasChanged = hasChanged || Object.keys(updates).reduce( (prev, next) => prev || orig[next] != updates[next], false);

    return hasChanged ? Object.assign({}, newval, updates) : orig;
}

function addDataReducer(state, action={}) {
    switch (action.type) {
        case APP_LOAD  :
            return getInitState();

        case APP_UPDATE  :
            return Object.assign({}, state, action.payload);

        case SHOW_DIALOG  :
            return showDialogChange(state,action);

        case HIDE_DIALOG  :
            return hideDialogChange(state,action);

        case HIDE_ALL_DIALOGS  :
            return hideAllDialogsChange(state,action);

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

        default:
            return state;
    }
}
/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * @param componentId the id or array of ids of the component to record the task count
 * @param taskId id of task, you create with makeTaskId()
 */
function dispatchAddTaskCount(componentId,taskId) {
    flux.process({type: ADD_TASK_COUNT, payload: {componentId,taskId}});
}

/**
 * @param componentId the id or array of ids of the component to record the task count
 * @param taskId id of task, create with makeTaskId()
 */
function dispatchRemoveTaskCount(componentId,taskId) {
    flux.process({type: REMOVE_TASK_COUNT, payload: {componentId,taskId}});
}

/**
 *
 * @param name name of preference
 * @param value value of preference
 */
function dispatchAddPreference(name,value) {
    flux.process({type: ADD_PREF, payload: {name,value}});
}

/**
 *
 * @param name name of preference
 */
function dispatchRemovePreference(name) {
    flux.process({type: REMOVE_PREF, payload: {name}});
}

/**
 * Updates the app-data layoutInfo.  This data is responsible for the layout of the top level components
 * i.e. search panel, results panel...
 * @param search    boolean. show the search panel.  defaults to false.
 * @param results   boolean. show the results panel. defaults to true.
 * @param mode      enum, one of ["auto", "tri", "sbs", "tb"]. defaults to "auto".
 * @param views     array of enum ["tables", "images", "xyPlots"].  Used in conjunction with mode to define what to show.
 * @param hasTables boolean.  Table data available.
 * @param hasImages boolean. Image data available.
 * @param hasXyPlots boolean. XY Plot data available.
 */
function dispatchUpdateLayout({search, results, mode, views, hasTables, hasImages, hasXyPlots}) {
    flux.process({type: UPDATE_LAYOUT, payload: pick({search, results, mode, views, hasTables, hasImages, hasXyPlots}, (v)=>(!isUndefined(v)))});
}


/*---------------------------- EXPORTS -----------------------------*/

export default {
    APP_LOAD,
    APP_UPDATE,
    SHOW_DIALOG,
    HIDE_DIALOG,
    APP_DATA_PATH,
    SHOW_SEARCH,
    UPDATE_LAYOUT,
    SEARCH_TYPE,
    DISPLAY_MODE_CHANGE,
    reducer,
    loadAppData,
    updateAppData,
    isDialogVisible,
    getDialogOwner,
    showDialog,
    hideDialog,
    hideAllDialogs,
    getPreference,
    dispatchAddTaskCount,
    dispatchRemoveTaskCount,
    dispatchAddPreference,
    dispatchRemovePreference,
    dispatchUpdateLayout,
    makeTaskId,
    getCommandState
};

/*---------------------------- PRIVATE -----------------------------*/

/**
 * fetches all of the necessary data to construct app-data.
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
                    menu: makeMenu(props),
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
        const type = props[`${action}.ToolbarButtonType`] || SEARCH_TYPE;
        menuItems.push({label, action, icon, desc, type});
    });
    return {selected, menuItems};
}

