/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isNil} from 'lodash';
import Enum from 'enum';

import {flux} from '../ReduxFlux';
import {BACKGROUND_PATH, BG_JOB_INFO, dispatchBgJobInfo, dispatchJobAdd} from './BackgroundCntlr.js';
import {getCmdSrvAsyncURL} from '../../util/WebUtil.js';
import {COMPONENT_STATE_CHANGE, dispatchComponentStateChange, getComponentState} from '../ComponentCntlr.js';
import {dispatchAddActionWatcher} from '../MasterSaga.js';
import {jsonFetch} from '../JsonUtils.js';
import {ServerParams} from '../../data/ServerParams.js';
import * as SearchServices from '../../rpc/SearchServicesJson.js';
import {logger} from '../../util/Logger';

export const Phase = new Enum(['PENDING', 'QUEUED', 'EXECUTING', 'COMPLETED', 'ERROR', 'ABORTED', 'HELD', 'SUSPENDED', 'ARCHIVED', 'UNKNOWN'], {ignoreCase: true});

export function getPhaseTips(phase) {
    const tips = {
        [Phase.PENDING]: 'The job is accepted by the service but not yet committed for execution by the client',
        [Phase.QUEUED]: 'Job is awaiting execution; the service is temporarily busy',
        [Phase.EXECUTING]: 'Job is currently running',
        [Phase.COMPLETED]: 'Job has completed; result(s) available for viewing or download',
        [Phase.ERROR]: 'Job has failed; detailed error information may be available',
        [Phase.ABORTED]: 'Job has been terminated by user or administrator',
        [Phase.HELD]: 'The job is HELD pending execution and will not automatically be executed',
        [Phase.SUSPENDED]: 'Job temporarily paused by the system',
        [Phase.ARCHIVED]: 'Minimal job record retained for reference, but results have been deleted',
        [Phase.UNKNOWN]: 'The job is in an unknown state.',
    };
    return tips[phase] || '';
}

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
    return ['SEARCH', 'UWS', 'TAP'].includes(job?.meta?.type);
}

export function isTapJob(job) {
    return job?.meta?.type === 'TAP';
}

export function getJobTitle(job) {
    return job.jobInfo?.title ?? job.runId ?? job.jobId;
}

export function isMonitored(job) {
    return !!job?.meta?.monitored;
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
    const {email, notifEnabled} =  get(flux.getState(), BACKGROUND_PATH) || {};
    return {email, notifEnabled};
}

export function canCreateScript(jobInfo) {
    return jobInfo.type === 'PACKAGE';
}

export function isDone(jobInfo) {
    return Phase.get('COMPLETED | ERROR | ABORTED | ARCHIVED').has(Phase.get(jobInfo?.phase));
}

export function isFail(jobInfo) {
    return Phase.get('ERROR | ABORTED').has(Phase.get(jobInfo?.phase));
}

export function isActive(jobInfo) {
    return Phase.get('PENDING | QUEUED | EXECUTING').has(Phase.get(jobInfo?.phase));
}

export function isExecuting(jobInfo) {
    return Phase.EXECUTING.is(Phase.get(jobInfo?.phase));
}

export function isArchived(jobInfo) {
    return Phase.ARCHIVED.is(Phase.get(jobInfo?.phase));
}

export function isAborted(jobInfo) {
    return Phase.ABORTED.is(Phase.get(jobInfo?.phase));
}

export function isPending(jobInfo) {
    return Phase.PENDING.is(Phase.get(jobInfo?.phase));
}

export function isQueued(jobInfo) {
    return Phase.QUEUED.is(Phase.get(jobInfo?.phase));
}

export function isSuccess(jobInfo) {
    return Phase.COMPLETED.is(Phase.get(jobInfo?.phase));
}

export function getErrMsg(jobInfo) {
    return jobInfo?.errorSummary?.message;
}

export const SCRIPT_ATTRIB = new Enum(['URLsOnly', 'Unzip', 'Ditto', 'Curl', 'Wget', 'RemoveZip']);

export function doPackageRequest({dlRequest, searchRequest, selectInfo, bgKey, downloadType, onComplete}) {

    dispatchComponentStateChange(bgKey, {inProgress:true, hide:false});
    SearchServices.packageRequest(dlRequest, searchRequest, selectInfo, downloadType)
        .then((jobInfo) => {
            const jobId = jobInfo?.meta?.jobId;
            if (isNil(jobId))  return;
            dispatchJobAdd(jobInfo);
            const inProgress = !isDone(jobInfo);
            dispatchComponentStateChange(bgKey, {inProgress, jobId});
            if (inProgress) {
                // not done; track progress
                trackBackgroundJob({jobId, key: bgKey, onComplete});
            } else {
                onComplete?.(jobInfo);
            }
        });
}

export function trackBackgroundJob({jobId, key, onComplete, hide}) {
    dispatchAddActionWatcher({  actions:[BG_JOB_INFO,COMPONENT_STATE_CHANGE],
                callback: bgTracker,
                params: {jobId, key, onComplete, hide}});
}

/**
 * @callback actionWatcherCallback
 * @param action
 * @param cancelSelf
 * @param params
 */
function bgTracker(action, cancelSelf, params={}) {
    const {jobId, key, onComplete, hide} = params;
    const {type, payload:jobInfo} = action || {};

    if ( type === BG_JOB_INFO && jobInfo?.meta?.jobId === jobId) {
        if (isDone(jobInfo)) {
            cancelSelf();
            dispatchComponentStateChange(key, {inProgress:false});
            onComplete?.(jobInfo);
        }
    } else if (getComponentState(key)?.hide) {
        cancelSelf();
        hide?.();
    }
}
