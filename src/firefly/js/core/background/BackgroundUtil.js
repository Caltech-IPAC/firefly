/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isNil} from 'lodash';
import Enum from 'enum';

import {flux} from '../ReduxFlux';
import {BACKGROUND_PATH, BG_JOB_INFO, dispatchBgJobInfo, dispatchJobAdd} from './BackgroundCntlr.js';
import {getCmdSrvAsyncURL} from '../../util/WebUtil.js';
import {dispatchComponentStateChange} from '../ComponentCntlr.js';
import {dispatchAddActionWatcher} from '../MasterSaga.js';
import {BG_JOB_ADD} from './BackgroundCntlr.js';
import {jsonFetch} from '../JsonUtils.js';
import {ServerParams} from '../../data/ServerParams.js';
import * as SearchServices from '../../rpc/SearchServicesJson.js';



export const submitJob = (cmd, params) => {
    // submit this job.  No need to add it into flux.  Server will push update to it.
    params[ServerParams.COMMAND] = cmd;
    return jsonFetch(getCmdSrvAsyncURL(), params, true);
};

export const modifyJob = (jobId, cmd, params) => {
    const url = getCmdSrvAsyncURL() + '/' + jobId + `?${ServerParams.COMMAND}=${cmd}`;
    return jsonFetch(url, params);
};

export const fetchJobResult = (jobId) => {
    const url = getCmdSrvAsyncURL() + '/' + jobId + '/results/result';
    return jsonFetch(url, null, false, true);
};

export const fetchJobInfo = (jobId) => {
    const url = getCmdSrvAsyncURL() + '/' + jobId;
    return jsonFetch(url);
};

export const loadAllJobs = () => {
    const url = getCmdSrvAsyncURL();
    jsonFetch(url).then( ({jobs}) => {
        jobs?.forEach( (jobId) => {
            fetchJobInfo(jobId).then( (jobInfo) => {
                if (jobInfo) {
                    dispatchBgJobInfo(jobInfo);
                }
            });
        });
    });
};






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
 * returns background jobInfo for the given jobId.
 * @param jobId
 * @returns {JobInfo}
 */
export function getJobInfo(jobId) {
    return get(flux.getState(), [BACKGROUND_PATH, 'jobs', jobId]);
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

export function canCreateScript(jobInfo) {
    return jobInfo.type === 'PACKAGE';
}

export function isDone(jobInfo) {
    return ['COMPLETED', 'ERROR', 'ABORTED'].includes(jobInfo?.phase);
}

export function isFail(jobInfo) {
    return ['ERROR', 'ABORTED'].includes(jobInfo?.phase);
}

export function isAborted(jobInfo) {
    return 'ABORTED' === jobInfo?.phase;
}

export function isSuccess(jobInfo) {
    return jobInfo?.phase === 'COMPLETED';
}

export function isActive(jobInfo) {
    return ['PENDING', 'QUEUED', 'EXECUTING'].includes(jobInfo?.phase);
}

export function getErrMsg(jobInfo) {
    return jobInfo?.errorSummary?.message;
}

export const SCRIPT_ATTRIB = new Enum(['URLsOnly', 'Unzip', 'Ditto', 'Curl', 'Wget', 'RemoveZip']);

export function doPackageRequest({dlRequest, searchRequest, selectInfo, bgKey, onComplete}) {
    const sentToBg = (jobInfo) => {
        dispatchJobAdd(jobInfo);
    };

    dispatchComponentStateChange(bgKey, {inProgress:true});
    SearchServices.packageRequest(dlRequest, searchRequest, selectInfo)
        .then((jobInfo) => {
            const jobId = jobInfo?.jobId;
            const inProgress = !isDone(jobInfo);
            dispatchComponentStateChange(bgKey, {inProgress, jobId});
            if (inProgress) {
                // not done; track progress
                trackBackgroundJob({jobId, key: bgKey, onComplete, sentToBg});
            } else {
                onComplete?.(jobInfo);
            }
        });
}

export function trackBackgroundJob({jobId, key, onComplete, sentToBg}) {
    dispatchAddActionWatcher({  actions:[BG_JOB_INFO,BG_JOB_ADD],
                callback: bgTracker,
                params: {jobId, key, onComplete, sentToBg}});
}

/**
 * @callback actionWatcherCallback
 * @param action
 * @param cancelSelf
 * @param params
 */
function bgTracker(action, cancelSelf, params={}) {
    const {jobId, key, onComplete, sentToBg} = params;
    const jobInfo = action.payload || {};
    if ( jobInfo?.jobId === jobId) {
        switch (action.type) {
            case BG_JOB_INFO:
                if (isDone(jobInfo)) {
                    cancelSelf();
                    dispatchComponentStateChange(key, {inProgress:false});
                    onComplete?.(jobInfo);
                }
                break;
            case BG_JOB_ADD:
                cancelSelf();
                dispatchComponentStateChange(key, {inProgress:false});
                sentToBg?.(jobInfo);
                break;
        }
    }
}
