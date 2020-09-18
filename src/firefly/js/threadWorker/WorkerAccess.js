import Worker from './firefly-thread.worker.js';
import {uniqueId} from 'lodash';
// import {WorkerSim} from './WorkerSim.js';
import {Logger} from '../util/Logger.js';


const logger= Logger('WorkerAccess');
const WORKER_COUNT= 6;
const workerKeys= [];
for(let i=0; (i<WORKER_COUNT); i++) workerKeys.push(`worker-${i}`);
let nextWorkerKey= 0;

const workerMap= new Map();
const promiseMap= new Map();

/**
 * @typedef {Object} WorkerAction
 * @prop {String} type - the action constant, a unique string identifying this action
 * @prop {String} workerKey - the key to specify which worker to use
 * @prop {String} callKey - use internally to the postToWorker to track promise returns, added by postToWorker
 * @prop {Object} [payload] - object with anything, the data
 * @global
 * @public
 */

// const USE_SIM= false;

export function initWorkerContext() {
    new Worker().terminate();
}

function makeWorker(workerKey) {
    const worker= new Worker();
    worker.onmessage= (ev) => {
        const {success,callKey}= ev.data;
        if (promiseMap.has(callKey)) {
            const pResponse= promiseMap.get(callKey);
            success ? pResponse.resolve(ev.data) : pResponse.reject(ev.data);
            promiseMap.delete(callKey);
        }
        else {
            logger.error('could not find callKey: ' + callKey);
        }
    };
    worker.onmessageerror= () => {
        logger.error(`get an message error in worker: ${workerKey}`);
    };
    worker.onerror= (err) => {
        logger.error(`get an error in worker: ${workerKey}`,err);
        workerMap.delete(workerKey); // worker died
    };
    return worker;
}

function getWorker(workerKey) {
    if (!workerKeys.includes(workerKey)) {
        logger.error(`workerKey must be one of: ${workerKeys.join()}`);
        return;
    }
    if (!workerMap.has(workerKey)) {
        const w= makeWorker(workerKey);
        workerMap.set(workerKey,w);
    }
    return workerMap.get(workerKey);
}

/**
 *
 * @param {WorkerAction} action
 * @return {Promise<unknown>}
 */
export function postToWorker(action) {
    if (!action.workerKey) throw('postToWorker requires worker key');
    const callKey= uniqueId('callkey-');
    const worker= getWorker(action.workerKey);
    action= {...action, callKey};
    worker.postMessage(action);

    return new Promise( (resolve, reject) => {
        promiseMap.set(callKey, {callKey, resolve,reject});
    });
}

export function getNextWorkerKey() {
    const workerKey= workerKeys[nextWorkerKey];
    nextWorkerKey++;
    nextWorkerKey=  (nextWorkerKey) % workerKeys.length;
    return workerKey;
}


export function removeWorker(workerKey) {
    getWorker(workerKey).terminate();
    workerMap.delete(workerKey);
}