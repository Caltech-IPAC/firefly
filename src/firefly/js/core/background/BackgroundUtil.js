/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, omit, isNil} from 'lodash';
import Enum from 'enum';
import {take} from 'redux-saga/effects';

import {flux} from '../../Firefly.js';
import {BACKGROUND_PATH} from './BackgroundCntlr.js';
import {getModuleName} from '../../util/WebUtil.js';
import {packageRequest} from '../../rpc/SearchServicesJson.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {dispatchComponentStateChange} from '../ComponentCntlr.js';
import {dispatchAddActionWatcher} from '../MasterSaga.js';
import {BG_JOB_ADD, BG_STATUS, bgStatusTransform} from './BackgroundCntlr.js';


/**
 * returns the whole background info object
 * @returns {BackgroundInfo}
 */
export function getBackgroundInfo() {
    return get(flux.getState(), [BACKGROUND_PATH], {});
}

/**
 * returns an array of all background jobs.
 * @returns {Array.<string>}
 */
export function getBackgroundJobs() {
    return get(flux.getState(), [BACKGROUND_PATH, 'jobs']);
}

/**
 * returns background status for the given id.
 * @param id
 * @returns {BgStatus}
 */
export function getBgStatusById(id) {
    return get(flux.getState(), [BACKGROUND_PATH, 'jobs', id]);
}

/**
 * returns the email used for background status notification.
 * @returns {string}
 */
export function getBgEmail() {
    return get(flux.getState(), [BACKGROUND_PATH, 'email']);
}

/**
 * returns the email related info.  Currently, it's email and enableEmail.
 * @returns {object.<string>}
 */
export function getBgEmailInfo() {
    let {email, enableEmail} =  get(flux.getState(), BACKGROUND_PATH) || {};
    enableEmail = isNil(enableEmail) ? !!email : enableEmail;
    return {email, enableEmail};
}

export function emailSent(bgStatus) {
    return get(bgStatus, 'ATTRIBUTES', '').includes('EmailSent');
}

export function canCreateScript(bgStatus) {
    return get(bgStatus, 'ATTRIBUTES', '').includes('DownloadScript');
}

export function isDone(state) {
    return BG_STATE.get('USER_ABORTED | CANCELED | FAIL | SUCCESS | UNKNOWN_PACKAGE_ID').has(state);
}

export function isFail(state) {
    return BG_STATE.get('FAIL | USER_ABORTED | UNKNOWN_PACKAGE_ID | CANCELED').has(state);
}

export function isSuccess(state) {
    return BG_STATE.SUCCESS.is(state);
}

export function isActive(state) {
    return BG_STATE.get('WAITING | WORKING | STARTING').has(state);
}

export function getErrMsg(bgStatus) {
    return Object.entries(omit(bgStatus, 'MESSAGE_CNT')).filter( ([k,v]) => k.startsWith('MESSAGE_'))
                            .map(([k,v]) => v)
                            .join('; ');
}


/**
 * returns a regex used to filter the incoming BackgroundStatus's DataTag.
 * @returns {RegExp}
 */
export function getDataTagMatcher() {
    var patterns = get(flux.getState(), [BACKGROUND_PATH, 'allowDataTag']) || [`^${getModuleName()}-.*`, 'catalog'];
    patterns = Array.isArray(patterns) ? patterns : patterns.split(',').map((s) => s.trim());
    return new RegExp(patterns.join('|'));
}

/**
 * returns a regex used to filter the incoming BackgroundStatus's DataTag.
 * @returns {RegExp}
 */
export function setDataTagMatcher() {
    var patterns = get(flux.getState(), [BACKGROUND_PATH, 'allowDataTag']) || [`^${getModuleName()}-.*`, 'catalog'];
    patterns = Array.isArray(patterns) ? patterns : patterns.split(',').map((s) => s.trim());
    return new RegExp(patterns.join('|'));
}


export const SCRIPT_ATTRIB = new Enum(['URLsOnly', 'Unzip', 'Ditto', 'Curl', 'Wget', 'RemoveZip']);
export const BG_STATE  = new Enum([
    /**
     * WAITING - Waiting to start request
     */
    'WAITING',
    /**
     * STARTING - Starting to package - used only on client side, before getting first status
     */
    'STARTING',
    /**
     * server is working on the packaging
     */
    'WORKING',
    /**
     * server is notifying of more data
     */
    'NEW_DATA',
    /**
     * USER_ABORTED - user aborted the package - should only be set on client side
     */
    'USER_ABORTED',
    /**
     * FAIL - server failed to package request
     */
    'FAIL',
    /**
     * SUCCESS - server successfully packaged request
     */
    'SUCCESS',
    /**
     * CANCELED - packaging was canceled, set by server when checking the canceled flag
     */
    'CANCELED',
    /**
     * UNKNOWN_PACKAGE_ID - a status used for a request with a unknown package id
     */
    'UNKNOWN_PACKAGE_ID'
]);


export function bgDownload({dlRequest, searchRequest, selectInfo}, {key, onComplete, sentToBg}) {
    dispatchComponentStateChange(key, {inProgress:true, bgStatus:undefined});
    packageRequest(dlRequest, searchRequest, SelectInfo.newInstance(selectInfo).toString())
        .then((bgStatus) => {
            if (bgStatus) {
                dispatchComponentStateChange(key, {bgStatus});
                bgStatus = bgStatusTransform(bgStatus);
                if (isSuccess(get(bgStatus, 'STATE'))) {
                    onComplete && onComplete(bgStatus);
                    dispatchComponentStateChange(key, {inProgress:false, bgStatus:undefined});
                } else {
                    // not done; track progress
                    trackBackgroundJob({bgID: bgStatus.ID, key, onComplete, sentToBg});
                }
            }
        });
}

export function trackBackgroundJob({bgID, key, onComplete, sentToBg}) {
    dispatchAddActionWatcher({  actions:[BG_STATUS,BG_JOB_ADD],
                callback: bgTracker,
                params: {bgID, key, onComplete, sentToBg}});
}

/**
 * @callback actionWatcherCallback
 * @param action
 * @param cancelSelf
 * @param params
 */
function bgTracker(action, cancelSelf, params={}) {
    const {bgID, key, onComplete, sentToBg} = params;
    const bgStatus = bgStatusTransform(action.payload || {});
    const {STATE, ID} = bgStatus;
    if (ID === bgID) {
        switch (action.type) {
            case BG_STATUS:
                if (isDone(STATE)) {
                    cancelSelf();
                    dispatchComponentStateChange(key, {inProgress:false, bgStatus:undefined});
                    onComplete && onComplete(bgStatus);
                }
                break;
            case BG_JOB_ADD:
                cancelSelf();
                dispatchComponentStateChange(key, {inProgress:false});
                sentToBg && sentToBg(bgStatus);
                break;
        }
    }
}
