/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {isNil, isObject} from 'lodash';

import {flux} from '../ReduxFlux';
import {updateSet} from '../../util/WebUtil.js';
import {showBackgroundMonitor} from './BackgroundMonitor.jsx';
import {isSuccess} from './BackgroundUtil.js';
import * as SearchServices from '../../rpc/SearchServicesJson.js';
import {doPackageRequest} from './BackgroundUtil.js';
import {showInfoPopup, hideInfoPopup} from '../../ui/PopupUtil.jsx';
import {WORKSPACE} from '../../ui/WorkspaceSelectPane.jsx';
import {validateFileName} from '../../ui/WorkspaceViewer.jsx';
import {dispatchWorkspaceUpdate} from '../../visualize/WorkspaceCntlr.js';
import {download} from '../../util/fetch';
import * as TblUtil from 'firefly/tables/TableUtil.js';

export const BACKGROUND_PATH = 'background';

/*---------------------------- ACTIONS -----------------------------*/
export const BG_JOB_INFO        = `${BACKGROUND_PATH}.jobInfo`;
export const BG_MONITOR_SHOW    = `${BACKGROUND_PATH}.bgMonitorShow`;

export const BG_JOB_ADD         = `${BACKGROUND_PATH}.bgJobAdd`;
export const BG_JOB_REMOVE      = `${BACKGROUND_PATH}.bgJobRemove`;
export const BG_JOB_CANCEL      = `${BACKGROUND_PATH}.bgJobCancel`;
export const BG_SET_EMAIL       = `${BACKGROUND_PATH}.bgSetEmail`;
export const BG_Package         = `${BACKGROUND_PATH}.bgPackage`;

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
 * Action to show/hide the background monitor.  To hide, set showBgMonitor to false
 * @param {Object}  p   payload
 * @param {boolean} p.showBgMonitor
 */
export function dispatchBgMonitorShow({show=true}) {
    flux.process({ type : BG_MONITOR_SHOW, payload: {show} });
}

/**
 * Add/update the jobInfo of the background job referenced by jobId.
 * @param {JobInfo}  jobInfo
 */
export function dispatchBgJobInfo(jobInfo) {
    flux.process({ type : BG_JOB_INFO, payload: jobInfo });
}

/**
 * set the email used for background status notification 
 * @param {string}  email
 */
export function dispatchBgSetEmailInfo({email, enableEmail}) {
    flux.process({ type : BG_SET_EMAIL, payload: {email, enableEmail} });
}

/**
 * Add this job to the background monitoring system.
 * @param {string} jobInfo
 */
export function dispatchJobAdd(jobInfo) {
    flux.process({ type : BG_JOB_ADD, payload: jobInfo });
}

/**
 * Remove the job from background monitor given its id.
 * @param {string} jobId
 */
export function dispatchJobRemove(jobId) {
    flux.process({ type : BG_JOB_REMOVE, payload: {jobId} });
}

/**
 * Cancel the background job given its id.
 * @param {string} jobId
 */
export function dispatchJobCancel(jobId) {
    flux.process({ type : BG_JOB_CANCEL, payload: {jobId} });
}

/**
 * Submit download request
 * @param {DownloadRequest} dlRequest
 * @param {TableRequest} searchRequest
 * @param {string} selectionInfo
 * @param {string} bgKey  used for updating UI states related to backgrounding
 */
export function dispatchPackage(dlRequest, searchRequest, selectionInfo, bgKey) {
    flux.process({ type : BG_Package, payload: {dlRequest, searchRequest, selectionInfo, bgKey} });
}


/*---------------------------- private -----------------------------*/


function bgMonitorShow(action) {
    return (dispatch) => {
        const {show=true} = action.payload;
        showBackgroundMonitor(show);
        dispatch(action);
    };
}

function bgJobAdd(action) {
    return (dispatch) => {
        const {jobId} = action.payload;
        if ( jobId ) {
            SearchServices.addBgJob(jobId);
            dispatch(action);
        }
    };
}

function bgJobRemove(action) {
    return (dispatch) => {
        const {jobId} = action.payload;
        if (jobId) {
            SearchServices.removeBgJob(jobId);
            dispatch(action);
        }
    };
}

function bgJobCancel(action) {
    return (dispatch) => {
        const {jobId} = action.payload;
        if (jobId) {
            SearchServices.cancel(jobId);
            dispatch(action);
        }
    };
}

function bgSetEmail(action) {
    return (dispatch) => {
        const {email} = action.payload;
        if (!isNil(email)) {
            SearchServices.setEmail(email);
        }
        dispatch(action);
    };
}

function bgPackage(action) {
    return (dispatch) => {
        const {dlRequest={}, searchRequest, selectionInfo, bgKey=''} = action.payload;
        let {fileLocation, wsSelect, BaseFileName} = dlRequest;

        BaseFileName = BaseFileName.endsWith('.zip') ? BaseFileName : BaseFileName.trim() + '.zip';
        if (fileLocation === WORKSPACE) {
            if (!validateFileName(wsSelect, BaseFileName)) return false;
        }

        const showBgMonitor = () => {
            showBackgroundMonitor();
            hideInfoPopup();
        };

        const onComplete = (jobInfo) => {
            const results = jobInfo?.results;     // on immediate download, there can only be one item(file).
            if (isSuccess(jobInfo)) {
                if (fileLocation === WORKSPACE) {
                    // file(s) are already pushed to workspace on the server-side.  Just update the UI.
                    dispatchWorkspaceUpdate();
                } else {
                    if (results?.length > 1) {
                        dispatchJobAdd(jobInfo);
                        const msg = (
                            <div style={{fontStyle: 'italic', width: 275}}>This download resulted in multiple files.<br/>
                                See <div onClick={showBgMonitor} className='clickable' style={{color: 'blue', display: 'inline'}} >Background Monitor</div> for download options
                            </div>
                        );
                        showInfoPopup(msg, 'Multipart download');

                    } else {
                        const url= jobInfo?.results?.[0]?.href;
                        download(url);
                    }
                }
            } else {
                jobInfo?.error && showInfoPopup(jobInfo.error);
            }
        };
        doPackageRequest({dlRequest, searchRequest, selectInfo:selectionInfo, bgKey, onComplete});
    };
}


function reducer(state={}, action={}) {

    switch (action.type) {
        case BG_JOB_INFO:
            const {jobId} = action.payload;
            let nstate = state;
            if (jobId) {
                const updates = {jobs: {[jobId]: action.payload}};
                nstate = TblUtil.smartMerge(nstate, updates);
            }
            return nstate;
            break;
        case BG_SET_EMAIL : {
            const {email, enableEmail} = action.payload;
            let nstate = state;
            if (!isNil(email)) nstate = updateSet(state, 'email', email);
            if (!isNil(enableEmail)) nstate = updateSet(state, 'enableEmail', enableEmail);
            return nstate;
            break;
        }
        case BG_JOB_ADD :
        case BG_JOB_CANCEL :
        case BG_JOB_REMOVE :
        default:
            return state;
    }

}


