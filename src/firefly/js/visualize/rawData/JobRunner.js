import {isString} from 'lodash';


/**
 * @file Will running a set for functions (that return a promise) in a batch style. For example if there are
 * 20 networks calls, and you want to run only 4 at a time.
 *
 */

const RESTARTED= 'restarted';
let jobCnt= 0;
const ABORT_MSG= 'Job Aborted while running';

/**
 * @param {number} maxRunning
 * @return {JobRunnerContext}
 */
export function makeJobRunningContext(maxRunning=2) {
    /** @type JobRunnerContext */
    const ctx= { waitingJobs: [], runningJobs: [], fetchWorkerIsRunning: false,
        restartRaceResolve: undefined, maxRunning, };
    ctx.restartRace = () => ctx.restartRaceResolve?.(RESTARTED);
    // todo: in 2027 this can be replaced with Promise.withResolvers()
    ctx.restartRacePromise= () => new Promise((resolve) => ctx.restartRaceResolve = resolve);
    ctx.createJobPromise= (f,jobGroupId, abortController) => createJobPromise(ctx, f,jobGroupId, abortController);
    ctx.abortJobs= (jobGroupId) => abortJobs(ctx, jobGroupId);
    return ctx;
}


export function isAbortedError(error) {
    const msg= isString(error) ? error.toLowerCase() : (error?.message?.toLowerCase() ?? '');
    return msg.includes('aborted');
}

/**
 *
 * @param {JobRunnerContext} ctx
 * @param {Function} f - a function that returns a promise. This is the work for the job
 * @param {String} jobGroupId - a string for a group of jobs
 * @param {AbortController} [abortController]
 * @return {Promise<unknown>}
 */
async function createJobPromise(ctx, f, jobGroupId, abortController) {
    /** @type Job */
    const job= {
        f,
        started:false,
        completed:false,
        success:true,
        failReason:undefined,
        aborted:false,
        promise:undefined,
        jobGroupId,
        abortController,
        jobId: `job-${jobCnt}`
    };
    jobCnt++;
    ctx.waitingJobs.push(job);
    ctx.restartRace();
    void fetchWorker(ctx);

    if (job.promise) return await job.promise;

    return new Promise( (resolve, reject) => {
        const timeoutId = setInterval(async () => {
            if (job.aborted) {
                clearInterval(timeoutId);
                reject(new Error(ABORT_MSG));
                return;
            }
            if (!job.promise) return;
            clearInterval(timeoutId);
            try {
                const results = await job.promise;
                if (job.aborted) {
                    reject(new Error('Job Aborted while running'));
                    return;
                }
                job.completed = true;
                
                resolve(results);
            } catch (e) {
                reject(e);
            }
        }, 5);
    });
}


async function fetchWorker(ctx) {
    if (ctx.fetchWorkerIsRunning) return;
    ctx.fetchWorkerIsRunning = true;

    while (ctx.waitingJobs.length || ctx.runningJobs.length) {
        const promises= ctx.runningJobs.filter( (w) => w.jobManagementPromise).map( (w) => w.jobManagementPromise);
        for(let i=promises.length; (i<ctx.maxRunning && ctx.waitingJobs.length); i++){
            const job= ctx.waitingJobs.shift();
            const p= executeJob(job);
            job.jobManagementPromise= p;
            promises.push(p);
            ctx.runningJobs.push(job);
        }
        promises.push(ctx.restartRacePromise());
        const winnerJob= await Promise.race(promises);
        if (winnerJob!==RESTARTED) {
            ctx.runningJobs= ctx.runningJobs.filter( (job) => winnerJob.jobId !== job.jobId);
        }
    }
    ctx.fetchWorkerIsRunning = false;
}

export function abortJobs(ctx,jobGroupId) {
    ctx.runningJobs
        .filter( (job) => job.jobGroupId===jobGroupId && !job.completed && !job.aborted)
        .forEach( (job) => {
            job.abortController?.abort(`${jobGroupId}: Job Aborted while running`);
            job.aborted = true;
        });
    ctx.runningJobs= ctx.runningJobs.filter( (job) => !job.aborted);
    ctx.waitingJobs
        .filter( (job) => job.jobGroupId===jobGroupId)
        .forEach( (job) => {
            job.aborted = true;
        });
    ctx.waitingJobs= ctx.waitingJobs.filter( (job) => !job.aborted);
}

async function executeJob(job) {
    const promise= job.f();
    job.promise= promise;
    job.started=true;
    try {
        await promise;
    }
    catch (e) {
        job.failReason = e;
        job.success= false;
    }
    job.completed=true;
    return job;
}

/**
 * @global
 * @public
 * @typedef {Object} JobRunnerContext
 *
 * @summary Context for JobRunner
 *
 * @prop {Array} waitingJobs
 * @prop {Array} runningJobs
 * @prop {Boolean} fetchWorkerIsRunning
 * @prop restartRaceResolve
 * @prop {Number} maxRunning
 * @prop {Function} restartRace
 * @prop {Function} restartRacePromise
 * @prop {Function} createJobPromise
 * @prop {Function} abortJobs
 */

/**
 * @typedef {Object} Job
 *
 * @prop f
 * @prop {boolean} started
 * @prop {boolean} completed
 * @prop {boolean} success
 * @prop {string} failReason
 * @prop {boolean} aborted
 * @prop promise
 * @prop {String} jobGroupId
 * @prop {AbortController} abortController
 * @prop {String} jobId
 */
