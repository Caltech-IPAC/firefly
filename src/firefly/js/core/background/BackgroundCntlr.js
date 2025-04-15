/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../ReduxFlux';

import {updateSet} from '../../util/WebUtil.js';
import {showJobMonitor, showMultiResults} from './JobHistory.jsx';
import {isSuccess} from './BackgroundUtil.js';
import * as SearchServices from '../../rpc/SearchServicesJson.js';
import {doPackageRequest} from './BackgroundUtil.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
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
export const BG_JOB_ARCHIVE      = `${BACKGROUND_PATH}.bgJobArchive`;
export const BG_SET_INFO       = `${BACKGROUND_PATH}.bgSetInfo`;
export const BG_Package         = `${BACKGROUND_PATH}.bgPackage`;

export default {actionCreators, reducers};

/*---------------------------- CREATORS ----------------------------*/
function actionCreators() {
    return {
        [BG_MONITOR_SHOW]: bgMonitorShow,
        [BG_SET_INFO]: bgSetInfo,
        [BG_Package]: bgPackage,
        [BG_JOB_ADD]: bgJobAdd,
        [BG_JOB_REMOVE]: bgJobRemove,
        [BG_JOB_CANCEL]: bgJobCancel,
        [BG_JOB_ARCHIVE]: bgJobArchive
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
 * Add/update the jobInfo of the background job referenced by jobId.
 * @param {Job}  jobInfo
 */
export function dispatchBgJobInfo(jobInfo) {
    flux.process({ type : BG_JOB_INFO, payload: jobInfo });
}

/**
 * set the email used for background status notification 
 * @param {string}  email
 */
export function dispatchBgSetInfo({email, sendNotif}) {
    flux.process({ type : BG_SET_INFO, payload: {email, sendNotif} });
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
 * Archive the job
 * @param {string} jobId
 */
export function dispatchJobArchive(jobId) {
    flux.process({ type : BG_JOB_ARCHIVE, payload: {jobId} });
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
export function dispatchPackage(dlRequest, searchRequest, selectionInfo, bgKey, downloadType) {
    flux.process({ type : BG_Package, payload: {dlRequest, searchRequest, selectionInfo, bgKey, downloadType} });
}


/*---------------------------- private -----------------------------*/


function bgMonitorShow(action) {
    return (dispatch) => {
        const {show=true} = action.payload;
        showJobMonitor(show);
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
            SearchServices.cancel(jobId);
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

function bgJobArchive(action) {
    return (dispatch) => {
        const {jobId} = action.payload;
        if (jobId) {
            SearchServices.archive(jobId);
            dispatch(action);
        }
    };
}

function bgSetInfo(action) {
    return (dispatch) => {
        const {email, sendNotif} = action.payload;
        SearchServices.setBgInfo(email, sendNotif);
        dispatch(action);
    };
}

function bgPackage(action) {
    return (dispatch) => {
        const {dlRequest={}, searchRequest, selectionInfo, bgKey='', downloadType} = action.payload;
        let {fileLocation, wsSelect, BaseFileName} = dlRequest;

        BaseFileName = BaseFileName.endsWith('.zip') ? BaseFileName : BaseFileName.trim() + '.zip';
        if (fileLocation === WORKSPACE) {
            if (!validateFileName(wsSelect, BaseFileName)) return false;
        }

        const onComplete = (jobInfo) => {
            const results = jobInfo?.results;     // on immediate download, there can only be one item(file).
            if (isSuccess(jobInfo)) {
                if (fileLocation === WORKSPACE) {
                    // file(s) are already pushed to workspace on the server-side.  Just update the UI.
                    dispatchWorkspaceUpdate();
                } else {
                    if (results?.length > 1) {
                        showMultiResults(jobInfo);
                    } else {
                        const url= jobInfo?.results?.[0]?.href;
                        download(url);
                    }
                }
            } else {
                jobInfo?.error && showInfoPopup(jobInfo.error);
            }
        };
        doPackageRequest({dlRequest, searchRequest, selectInfo:selectionInfo, bgKey, downloadType, onComplete});
    };
}


function reducer(state={}, action={}) {

    switch (action.type) {
        case BG_JOB_INFO:
            const jobId = action.payload?.meta?.jobId;
            let nstate = state;
            if (jobId) {
                const updates = {jobs: {[jobId]: action.payload}};
                nstate = TblUtil.smartMerge(nstate, updates);
            }
            return nstate;
            break;
        case BG_SET_INFO : {
            const {email, sendNotif} = action.payload;
            let nstate = updateSet(state, 'email', email);
            nstate  = updateSet(nstate, 'sendNotif', sendNotif);
            return nstate;
            break;
        }
        default:
            return state;
    }

}


