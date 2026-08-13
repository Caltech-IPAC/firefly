/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {cloneDeep, get, isNil} from 'lodash';
import Enum from 'enum';
import moment from 'moment';
import {makeUniquePlotIdFromBase} from '../../visualize/PlotViewUtil';

import {flux} from '../ReduxFlux';
import {BACKGROUND_PATH, BG_JOB_INFO, dispatchBgLoadJobs, dispatchJobAdd} from './BackgroundCntlr.js';
import {getCmdSrvAsyncURL, isURL} from '../../util/WebUtil.js';
import {COMPONENT_STATE_CHANGE, dispatchComponentStateChange, getComponentState} from '../ComponentCntlr.js';
import {dispatchAddActionWatcher} from '../MasterSaga.js';
import {jsonFetch} from '../JsonUtils.js';
import {ServerParams} from '../../data/ServerParams.js';
import * as SearchServices from '../../rpc/SearchServicesJson.js';
import {logger} from '../../util/Logger';
import * as TblUtil from 'firefly/tables/TableUtil';
import {copyRequestOptions, getRequestFromJob, getTblId, makeFileRequest, makeTblRequest} from 'firefly/tables/TableRequestUtil';
import {MIXED_FITS_MIME_TYPE, MULTI_SPECTRUM_MIME_TYPE, MULTI_SPECTRUM_PROC_ID} from './BackgroundConst.js';
import {VO_TABLE_CONTENT_TYPE} from 'firefly/voAnalyzer/VoConst';
import {dispatchTableRemove, dispatchTableSearch, dispatchTableUpdate} from 'firefly/tables/TablesCntlr';
import WebPlotRequest, {TitleOptions} from 'firefly/visualize/WebPlotRequest';
import {getAViewFromMultiView, getMultiViewRoot} from 'firefly/visualize/MultiViewCntlr';
import {dispatchPlotImage} from '../../visualize/ImagePlotDispatch';
import {IMAGE} from '../../visualize/VisConst';
import {dispatchFormSubmit} from 'firefly/core/AppDataCntlr';
import {showJobMonitor, showMultiMultiResults} from 'firefly/core/background/JobMonitor';
import {getTableUiByTblId} from 'firefly/tables/TableUtil';






/** @typedef Phase
 * enum can be one of
 * @prop PENDING
 * @prop QUEUED
 * @prop EXECUTING
 * @prop COMPLETED
 * @prop ERROR
 * @prop ABORTED
 * @prop HELD
 * @prop SUSPENDED
 * @prop ARCHIVED
 * @prop UNKNOWN
 * @prop {Function} get
 * @type {Enum}
 */



/** @type Phase */
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

export const fetchJobResult = (jobInfo) => {
    const results = jobInfo?.results;
    if (!results || results.length===0) return Promise.resolve(null);
    const resultUrl = results[0]?.url;
    if (!resultUrl) return Promise.resolve(null);
    return jsonFetch(resultUrl);
};

export const fetchJobInfo = (jobId) => {
    const url = getCmdSrvAsyncURL() + '/' + jobId;
    return jsonFetch(url);
};

export const loadAllJobs = () => {
    const url = getCmdSrvAsyncURL();
    jsonFetch(url).then( ({jobs=[], overflow}) => {
        // convert List<JobInfo> to Object<JobId, JobInfo>; always dispatch, even when there are zero jobs,
        // so the store's jobs map goes from undefined ('not loaded yet') to {} ('loaded; none found')
        const jobsMap = Object.fromEntries(jobs.map((j) => [j?.meta?.jobId, j]));
        dispatchBgLoadJobs({jobs:jobsMap, overflow});
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

export function isUWS(job) {
    return ['UWS', 'TAP'].includes(job?.meta?.type);
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
 * @returns {Object.<string>}
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

export function getElapsedTime(jobInfo) {
    const creationTime = jobInfo?.creationTime ? new Date(jobInfo.creationTime) : new Date();
    const d = moment.duration(new Date() - creationTime);
    const fmt = d.asHours() < 1 ? 'mm:ss' : 'HH:mm:ss';
    return moment.utc(d.asMilliseconds()).format(fmt);
}

export function getJobPctComplete (jobInfo) {
    const {percentComplete, itemsProcessed, totalItems} = jobInfo?.jobInfo?.progress ?? {};
    if (percentComplete < 0 && totalItems < 1) return -1;
    const pct = percentComplete >= 0 ? percentComplete : itemsProcessed / totalItems * 100;
    return Math.min(Math.round(pct), 100);
}

export function getProgressMsg(jobInfo) {
    const {message, itemsProcessed, totalItems} = jobInfo?.jobInfo?.progress ?? {};
    const pct = getJobPctComplete(jobInfo);
    const fixCase = (s) => s?.charAt(0).toUpperCase() + s?.slice(1).toLowerCase();

    if (message) return message;
    if (totalItems > 0) return `${itemsProcessed} of ${totalItems} processed`;
    if (pct > 0) return `${pct}% complete`;
    return `${fixCase(jobInfo?.phase ?? '')}...`;
}

export const SCRIPT_ATTRIB = new Enum(['URLList', 'Unzip', 'Ditto', 'Curl', 'Wget', 'RemoveZip']);

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
            onComplete?.(jobInfo);
            dispatchComponentStateChange(key, {inProgress:false});
        }
    } else if (getComponentState(key)?.hide) {
        cancelSelf();
        hide?.();
    }
}

export function handleJobResult({jobInfo, hlRowIdx}) {
    jobInfo = fixTapResults(jobInfo);
    const {results, tbl_id} = getMetadata({jobInfo});
    if (results?.length===0) {
        dispatchTableUpdate(TblUtil.createErrorTbl(tbl_id, 'No results found'));
    } else if (results.length === 1) {
        loadJobResult({jobInfo, resultIdx:0});
    } else {
        showMultiMultiResults(jobInfo);
    }
}

export function fixTapResults(jobInfo) {
    if (!isTapJob(jobInfo)) return jobInfo;

    const jobUrl = jobInfo?.jobInfo?.jobUrl;
    if (jobUrl) {       // if jobUrl is available, we can construct the result URL for TAP job since it's in the standard.
        const copy = cloneDeep(jobInfo);
        copy.results = [{
            href: `${jobUrl}/results/result`,
            mimeType: 'application/x-votable+xml',
            id: 'result',
        }];
        return copy;
    }
    return jobInfo;     // otherwise, return as is and hope the server provides the result URL correctly in the jobInfo.
}


export function loadJobResult({jobInfo, resultIdx}) {
    const {request, tbl_id, loader, href,  mimeType, id, size} = getMetadata({jobInfo, resultIdx});
    loader?.({jobInfo, request, href, mimeType, id, size});
    if (!loader?.createsTable) {        // this result is not a table, i.e. an image or a chart; drop the table the search created for it.
        dispatchTableRemove(tbl_id, true);
    }
}

const FITS_EXTS = ['fits', 'fit'];
const TABLE_EXTS = ['csv', 'tbl', 'tsv', 'txt', 'vot', 'xml'];
const COMPRESSION_EXTS = ['gz'];

function parseHrefExtension(href) {
    try {
        const resource = new URL(href).pathname.split('/').pop();
        if (!resource?.includes('.')) return null;

        const parts = resource.toLowerCase().split('.');
        const rawExt = parts.at(-1);
        const wrapper = COMPRESSION_EXTS.includes(rawExt) ? parts.pop() : null;
        const ext = parts.length > 1 ? parts.at(-1) : null;

        return { resource, rawExt, ext, wrapper, isFile: ext !== null };
    } catch {
        return null;
    }
}

// Returns the loader for the given media type, or undefined when it's not a type we know how to load.
function  getMimeLoader(mimeType, href) {
    if (!mimeType && href) {
        const {ext} = parseHrefExtension(href) ?? {};
        if (FITS_EXTS.includes(ext)) {
            mimeType = 'application/fits';
        } else if (TABLE_EXTS.includes(ext)) {
            mimeType = VO_TABLE_CONTENT_TYPE;
        }
    }

    // normalized mimeType so it matches against the known types
    const nMimeType = String(mimeType).toLowerCase().replace(/\s+/g, '');
    switch (nMimeType) {
        case MIXED_FITS_MIME_TYPE:
            return loadMixedResult;
        case MULTI_SPECTRUM_MIME_TYPE:
            return loadMultiSpectrumResult;
        case 'application/fits':
        case 'application/x-fits':
        case 'image/fits':
            return loadImageResult;
        case VO_TABLE_CONTENT_TYPE:
            return loadTableResult;
        default:
            return undefined;
    }
}

const handleLayoutChanges = (jobInfo) => {
    showJobMonitor(false);
    const {submitTo} = getMetadata({jobInfo});
    if (submitTo)  dispatchFormSubmit({submitTo}); // if this is a routed app, submit the form to update the route
};

export function loadTableResult({jobInfo, requestSupplier}) {
    const {tbl_id, href, request} = getMetadata({jobInfo});
    if (!isURL(href)) {
        dispatchTableUpdate(TblUtil.createErrorTbl(tbl_id, `Invalid result URL: ${href}`));
    }

    const tblRequest= requestSupplier?.(jobInfo) || makeFileRequest(null, href, null, {tbl_id});
    copyRequestOptions(request, tblRequest);

    const {tbl_ui_id} = getTableUiByTblId(tbl_id) || {};        // re-use existing table UI if exists
    dispatchTableSearch(tblRequest, {tbl_ui_id});
    handleLayoutChanges(jobInfo);
}
loadTableResult.createsTable = true;

// Load a MultiSpectrum table result, transforming it into an obs_core table with datalinks to each spectrum.
export function loadMultiSpectrumResult({jobInfo}) {

    loadTableResult({jobInfo, requestSupplier: (ji) => {
            const {tbl_id, href} = getMetadata({jobInfo: ji});
            return makeTblRequest(MULTI_SPECTRUM_PROC_ID, null, {source: href}, {tbl_id});
    }});
}
loadMultiSpectrumResult.createsTable = true;


export function getMetadata({jobInfo, resultIdx=0}) {
    const request = getRequestFromJob(jobInfo?.meta?.jobId);  // the request is initiated from Firefly
    const tbl_id = getTblId(request);
    const submitTo = request?.META_INFO?.form_submitTo;
    const results = jobInfo?.results || [];
    const metaMimeType = jobInfo?.meta?.mimeType;       // job-level override;
    const {href, mimeType, id, size} = results[resultIdx];
    const loader = getMimeLoader(metaMimeType || mimeType, href);
    if (metaMimeType && !loader) {
        logger.warn(`No loader for declared result media type: ${metaMimeType}; loading it as a plain table`);
    }
    return {tbl_id, request, submitTo, href, loader: loader ?? loadTableResult, results, mimeType, id, size};
}

export function loadMixedResult({jobInfo, href}) {
    loadImageResult({jobInfo, href});      // placeholder for mixed content loader to be implemented later
}

export function loadImageResult({jobInfo, request, href}) {
    const wpRequest = WebPlotRequest.makeURIPlotRequest(href);
    const {viewerId=''} = getAViewFromMultiView(getMultiViewRoot(), IMAGE) || {};
    wpRequest.setPlotGroupId(viewerId);

    const requestTitle = request?.META_INFO?.title;
    if (requestTitle) {
        wpRequest.setTitleOptions(TitleOptions.NONE);
        wpRequest.setTitle(requestTitle);
    }

    const plotId= makeUniquePlotIdFromBase(request.META_INFO.jobPlotId ?? `image-${jobInfo.jobId}`);
    dispatchPlotImage({plotId, wpRequest, viewerId});
    handleLayoutChanges(jobInfo);
}

