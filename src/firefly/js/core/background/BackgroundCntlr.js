/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {set, get, has, pick, isNil} from 'lodash';

import {flux} from '../../Firefly.js';
import {smartMerge} from '../../tables/TableUtil.js';
import {updateDelete, updateSet, download} from '../../util/WebUtil.js';
import {showBackgroundMonitor} from './BackgroundMonitor.jsx';
import {isSuccess, getDataTagMatcher} from './BackgroundUtil.js';
import * as SearchServices from '../../rpc/SearchServicesJson.js';
import {Keys} from './BackgroundStatus.js';

export const BACKGROUND_PATH = 'background';

/*---------------------------- ACTIONS -----------------------------*/
export const BG_STATUS          = `${BACKGROUND_PATH}.bgStatus`;
export const BG_MONITOR_SHOW    = `${BACKGROUND_PATH}.bgMonitorShow`;
export const BG_JOB_ADD         = `${BACKGROUND_PATH}.bgJobAdd`;
export const BG_JOB_UPDATE         = `${BACKGROUND_PATH}.bgJobUpdate`;
export const BG_JOB_REMOVE      = `${BACKGROUND_PATH}.bgJobRemove`;
export const BG_JOB_CANCEL      = `${BACKGROUND_PATH}.bgJobCancel`;
export const BG_SET_EMAIL       = `${BACKGROUND_PATH}.bgSetEmail`;
export const BG_Package         = `${BACKGROUND_PATH}.bgPackage`;
export const BG_ALLOW_DATA_TAG  = `${BACKGROUND_PATH}.bgAllowDataTag`;

export default {actionCreators, reducers};

/*---------------------------- CREATORS ----------------------------*/
function actionCreators() {
    return {
        [BG_MONITOR_SHOW]: bgMonitorShow,
        [BG_SET_EMAIL]: bgSetEmail,
        [BG_Package]: bgPackage,
        [BG_JOB_ADD]: bgJobAdd,
        [BG_JOB_REMOVE]: bgJobRemove,
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
export function dispatchBgSetEmailInfo({email, enableEmail}) {
    flux.process({ type : BG_SET_EMAIL, payload: {email, enableEmail} });
}

/**
 * a list of patterns use to filter the incoming background jobs.
 * @param {string[]}  patterns
 */
export function dispatchAllowDataTag(patterns) {
    flux.process({ type : BG_ALLOW_DATA_TAG, payload: {patterns} });
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


/*---------------------------- private -----------------------------*/


function bgMonitorShow(action) {
    return () => {
        const {show=true} = action.payload;
        showBackgroundMonitor(show);
    };
}

function bgJobAdd(action) {
    return (dispatch) => {
        const bgInfo = action.payload;
        if ( bgInfo && get(bgInfo, [Keys.DATA_TAG], '').match(getDataTagMatcher()) ) {
            SearchServices.addBgJob(bgInfo);
            dispatch(action);
        }
    };
}

function bgJobRemove(action) {
    return (dispatch) => {
        const {id} = action.payload;
        if (id) {
            SearchServices.removeBgJob(id);
            dispatch(action);
        }
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
        const {email, enableEmail} = action.payload;
        if (!isNil(email)) {
            SearchServices.setEmail(email);
        }
        dispatch(action);
    };
}

function bgPackage(action) {
    return (dispatch) => {
        const {dlRequest, searchRequest, selectionInfo} = action.payload;
        SearchServices.packageRequest(dlRequest, searchRequest, selectionInfo)
            .then((bgStatus) => {
                if (bgStatus) {
                    bgStatus = bgStatusTransform(bgStatus);
                    const url = get(bgStatus, ['ITEMS', 0, 'url']);
                    if (url && isSuccess(get(bgStatus, 'STATE'))) {
                        download(url);
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
        case BG_SET_EMAIL : {
            const {email, enableEmail} = action.payload;
            let nstate = state;
            if (!isNil(email)) nstate = updateSet(state, 'email', email);
            if (!isNil(enableEmail)) nstate = updateSet(state, 'enableEmail', enableEmail);
            return nstate;
            break;
        }
        case BG_ALLOW_DATA_TAG :
            const {patterns} = action.payload;
            return updateSet(state, 'allowDataTag', patterns);
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
    if (has(state, ['jobs', bgstats.ID])) {
        return handleBgJobAdd(state, action);
    } else return state;
}

function handleBgJobAdd(state, action) {
    var bgstats = action.payload;
    bgstats = bgStatusTransform(bgstats);
    const nState = set({}, ['jobs', bgstats.ID], bgstats);
    if (!nState.email && !isNil(bgstats.email)) nState.email = bgstats.email;       // use email from server if one is not set
    return smartMerge(state, nState);
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
 * @param bgStatus
 * @returns {{ITEMS: Array.<*>}}
 */
export function bgStatusTransform(bgStatus) {
    const ITEMS = Object.keys(bgStatus)
        .filter( (k) => k.startsWith('ITEMS_') )
        .map( (k) => {
            const [,index] = k.split('_');
            const INDEX = Number(index);
            return {INDEX, ...bgStatus[k]};
        }).sort((a, b) => a.INDEX - b.INDEX);
    const REST = pick(bgStatus,  Object.keys(bgStatus).filter( (k) => !k.startsWith('ITEMS_')));

    return {ITEMS, ...REST};
}



