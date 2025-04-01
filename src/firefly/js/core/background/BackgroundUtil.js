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
import {logger} from '../../util/Logger';

export const Phase = new Enum(['PENDING', 'QUEUED', 'EXECUTING', 'COMPLETED', 'ERROR', 'ABORTED', 'HELD', 'SUSPENDED', 'ARCHIVED']);

export const submitJob = (cmd, params) => {
    // submit this job.  No need to add it into flux.  Server will push update to it.
    params[ServerParams.COMMAND] = cmd;
    return jsonFetch(getCmdSrvAsyncURL(), params, true).catch( (e) => { logger.error(e); });
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
 * @returns {Job}
 */
export function getJobInfo(jobId) {
    return getBackgroundJobs()?.[jobId];
}

export function isSearchJob(job) {
    return job?.jobInfo?.type === 'SEARCH' || job?.jobInfo?.type === 'UWS';
}

/**
 * returns the email used for background status notification.
 * @returns {string}
 */
export function getBgEmail() {
    return get(flux.getState(), [BACKGROUND_PATH, 'email']);
}

/**
 * returns the background related info.  Currently, it's email and sendNotif.
 * @returns {object.<string>}
 */
export function getBgInfo() {
    const {email, sendNotif} =  get(flux.getState(), BACKGROUND_PATH) || {};
    return {email, sendNotif};
}

export function canCreateScript(jobInfo) {
    return jobInfo.type === 'PACKAGE';
}

export function isDone(jobInfo) {
    return Phase.get('COMPLETED | ERROR | ABORTED | ARCHIVED').has(jobInfo?.phase);
}

export function isFail(jobInfo) {
    return Phase.get('ERROR | ABORTED').has(jobInfo?.phase);
}

export function isActive(jobInfo) {
    return Phase.get('PENDING | QUEUED | EXECUTING').has(jobInfo?.phase);
}
export function isArchived(jobInfo) {
    return Phase.ARCHIVED.is(jobInfo?.phase);
}

export function isAborted(jobInfo) {
    return Phase.ABORTED.is(jobInfo?.phase);
}

export function isPending(jobInfo) {
    return Phase.PENDING.is(jobInfo?.phase);
}

export function isQueued(jobInfo) {
    return Phase.QUEUED.is(jobInfo?.phase);
}

export function isSuccess(jobInfo) {
    return Phase.COMPLETED.is(jobInfo?.phase);
}

export function getErrMsg(jobInfo) {
    return jobInfo?.errorSummary?.message;
}

export const SCRIPT_ATTRIB = new Enum(['URLsOnly', 'Unzip', 'Ditto', 'Curl', 'Wget', 'RemoveZip']);

export function doPackageRequest({dlRequest, searchRequest, selectInfo, bgKey, downloadType, onComplete}) {
    const sentToBg = (jobInfo) => {
        dispatchJobAdd(jobInfo);
    };

    dispatchComponentStateChange(bgKey, {inProgress:true});
    SearchServices.packageRequest(dlRequest, searchRequest, selectInfo, downloadType)
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
