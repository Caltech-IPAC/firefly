/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {set, get, has, pick} from 'lodash';

import {flux} from '../../Firefly.js';
import {smartMerge} from '../../tables/TableUtil.js';
import {updateDelete, updateSet, download} from '../../util/WebUtil.js';
import {showBackgroundMonitor} from './BackgroundMonitor.jsx';
import {isSuccess} from './BackgroundUtil.js';
import * as SearchServices from '../../rpc/SearchServicesJson.js';

export const BACKGROUND_PATH = 'background';

/*---------------------------- ACTIONS -----------------------------*/
export const BG_STATUS          = `${BACKGROUND_PATH}.bgStatus`;
export const BG_MONITOR_SHOW    = `${BACKGROUND_PATH}.bgMonitorShow`;
export const BG_JOB_ADD         = `${BACKGROUND_PATH}.bgJobAdd`;
export const BG_JOB_REMOVE      = `${BACKGROUND_PATH}.bgJobRemove`;
export const BG_JOB_CANCEL      = `${BACKGROUND_PATH}.bgJobCancel`;
export const BG_JOB_IMMEDIATE   = `${BACKGROUND_PATH}.bgJobImmediate`;
export const BG_SET_EMAIL       = `${BACKGROUND_PATH}.bgSetEmail`;
export const BG_Package         = `${BACKGROUND_PATH}.bgPackage`;

export default {actionCreators, reducers};

/*---------------------------- CREATORS ----------------------------*/
function actionCreators() {
    return {
        [BG_MONITOR_SHOW]: bgMonitorShow,
        [BG_SET_EMAIL]: bgSetEmail,
        [BG_Package]: bgPackage,
        [BG_JOB_CANCEL]: bgJobCancel
    };
}

/*---------------------------- REDUCERS -----------------------------*/
function reducers() {
    return {
        [BACKGROUND_PATH]: reducer
    };
}


/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * Action to show/hide the background monitor.  To hide, set showBgMonitor to fase
 * @param {Object}  p   payload
 * @param {boolean} p.showBgMonitor
 */
export function dispatchBgMonitorShow({show=true}) {
    flux.process({ type : BG_MONITOR_SHOW, payload: {show} });
}

/**
 * Add/update the background status of the job referenced by ID.
 * @param {BgStatus}  bgStatus
 */
export function dispatchBgStatus(bgStatus) {
    flux.process({ type : BG_STATUS, payload: bgStatus });
}

/**
 * set the email used for background status notification 
 * @param {string}  email
 */
export function dispatchBgSetEmail(email) {
    flux.process({ type : BG_SET_EMAIL, payload: {email} });
}

/**
 * Add this job to the background monitoring system.
 * @param {string} bgStatus
 */
export function dispatchJobAdd(bgStatus) {
    flux.process({ type : BG_JOB_ADD, payload: bgStatus });
}

/**
 * Remove the job from background monitor given its id.
 * @param {string} id
 */
export function dispatchJobRemove(id) {
    flux.process({ type : BG_JOB_REMOVE, payload: {id} });
}

/**
 * Cancel the background job given its id.
 * @param {string} id
 */
export function dispatchJobCancel(id) {
    flux.process({ type : BG_JOB_CANCEL, payload: {id} });
}

/**
 * Action to show/hide the background monitor.  To hide, set showBgMonitor to fase
 * @param {DownloadRequest} dlRequest
 * @param {TableRequest} searchRequest
 * @param {string} selectionInfo
 */
export function dispatchPackage(dlRequest, searchRequest, selectionInfo) {
    flux.process({ type : BG_Package, payload: {dlRequest, searchRequest, selectionInfo} });
}


/**
 * this saga will trigger callback when a package request is either added or handled immediately
 * @param {Object} p   props
 * @param {string} p.title  download request's title
 * @param {function} p.callback  callback to execute when package request returned.
 */
export function* doOnPackage({title, callback}) {

    var isDone = false;
    while (!(isDone)) {
        const action = yield take([BG_JOB_IMMEDIATE, BG_JOB_ADD]);
        isDone = title === get(action, 'payload.Title');
    }
    callback && callback();
}

/*---------------------------- private -----------------------------*/


function bgMonitorShow(action) {
    return () => {
        const {show=true} = action.payload;
        showBackgroundMonitor(show);
    };
}

function bgJobCancel(action) {
    return (dispatch) => {
        const {id} = action.payload;
        if (id) {
            SearchServices.cancel(id);
        }
    };
}

function bgSetEmail(action) {
    return (dispatch) => {
        const {email} = action.payload;
        if (email) {
            SearchServices.setEmail(email);
            dispatch(action);
        }
    };
}

function bgPackage(action) {
    return (dispatch) => {
        const {dlRequest, searchRequest, selectionInfo} = action.payload;
        SearchServices.packageRequest(dlRequest, searchRequest, selectionInfo)
            .then((bgStatus) => {
                if (bgStatus) {
                    bgStatus = transform(bgStatus);
                    const url = get(bgStatus, ['ITEMS', 0, 'url']);
                    if (url && isSuccess(get(bgStatus, 'STATE'))) {
                        download(url);
                        dispatch({type: BG_JOB_IMMEDIATE, payload: bgStatus});       // allow saga to catch flow.
                    } else {
                        dispatchJobAdd(bgStatus);
                    }
                }
            });
    };
}


function reducer(state={}, action={}) {

    switch (action.type) {
        case BG_STATUS :
            return handleBgStatusUpdate(state, action);
            break;
        case BG_SET_EMAIL :
            const {email} = action.payload;
            return updateSet(state, 'email', email);
            break;
        case BG_JOB_ADD :
            return handleBgJobAdd(state, action);
            break;
        case BG_JOB_REMOVE :
            return handleBgJobRemove(state, action);
            break;
        default:
            return state;
    }

}


function handleBgStatusUpdate(state, action) {
    var bgstats = action.payload;
    bgstats = transform(bgstats);
    if (has(state, ['jobs', bgstats.ID])) {
        const nState = set({}, ['jobs', bgstats.ID], bgstats);
        return smartMerge(state, nState);
    } else return state;
}

function handleBgJobAdd(state, action) {
    var bgstats = action.payload;
    return updateSet(state, ['jobs', bgstats.ID], bgstats);
}

function handleBgJobRemove(state, action) {
    const {id} = action.payload;
    const nState = has(state, ['jobs', id]) ? updateDelete(state, 'jobs', id) : state;
    return nState;
}

/**
 * take the server's BackgroundStatus and transform it into something more usable
 * on the client.  It will also apply some processing that was previously done at
 * the component level.
 * NOTE:  added INDEX
 * @param bgstats
 * @returns {{ITEMS: Array.<*>}}
 */
function transform(bgstats) {
    const ITEMS = Object.keys(bgstats)
        .filter( (k) => k.startsWith('PACKAGE_PROGRESS_') )
        .map( (k) => {
            const [,,index] = k.split('_');
            const INDEX = Number(index);
            return {INDEX, ...bgstats[k]};
        }).sort((a, b) => a.INDEX - b.INDEX);
    const REST = pick(bgstats,  Object.keys(bgstats).filter( (k) => !k.startsWith('PACKAGE_PROGRESS_')));

    return {ITEMS, ...REST};
}



