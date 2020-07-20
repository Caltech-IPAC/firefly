import Worker from './RawData.worker.js';
import {uniqueId} from 'lodash';
import {WorkerSim} from './WorkerSim.js';


const WORKER_COUNT= 3;
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

const USE_SIM= false;

export function initWorkerContext() {
    new Worker().terminate();
}

function makeWorker(workerKey) {
    console.time(workerKey);
    const worker= USE_SIM ? WorkerSim : new Worker();
    console.timeEnd(workerKey);
    worker.onmessage= (ev) => {
        const {success,callKey}= ev.data;
        if (promiseMap.has(callKey)) {
            const pResponse= promiseMap.get(callKey);
            success ? pResponse.resolve(ev.data) : pResponse.reject(ev.data.error);
            promiseMap.delete(callKey);
        }
        else {
            console.log('could not find callKey: ' + callKey);
        }
    };
    worker.onmessageerror= () => {
        console.log(`get an message error in worker: ${workerKey}`);
    };
    worker.onerror= (err) => {
        console.log(`get an error in worker: ${workerKey}`,err);
        workerMap.delete(workerKey); // worker died
    };
    return worker;
}

function getWorker(workerKey) {
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
    nextWorkerKey=  (nextWorkerKey+1) % workerKeys.length;
    return workerKey;
}


export function removeWorker(workerKey) {
    getWorker(workerKey).terminate();
    workerMap.delete(workerKey);
}