/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {loadScript, getRootURL, getGlobalObj} from '../../util/WebUtil.js';

// const GPU_JS_SCRIPT= 'gpu-browser.min-2.15.0.js';
const GPU_JS_SCRIPT= 'gpu-browser.min-2.15.0-FIX-GPUX.js';
const LOAD_ERR_MSG= 'Load Failed: could not load gpu.js';

let foundGPU;

function initGpuJsRetriever(loadNow) {
    let gpuJsLoadBegin= false;
    let waitingResolvers= [];
    let waitingRejectors= [];
    let loadedGpuJsFailed;
    let loadedGpuJs;

    const getGpuJs = (rootUrl= getRootURL()) => {
        if (loadedGpuJs || loadedGpuJsFailed) {
            return loadedGpuJs ? Promise.resolve(loadedGpuJs) : Promise.reject(Error(LOAD_ERR_MSG));
        }

        const script= `${rootUrl}${GPU_JS_SCRIPT}`;
        if (!gpuJsLoadBegin) {
            gpuJsLoadBegin= true;
            loadScript(script).then( () => {
                loadedGpuJs= getGlobalObj().GPUX;
                if (loadedGpuJs) {
                    foundGPU= loadedGpuJs;
                    waitingResolvers.forEach((r) => r(loadedGpuJs));
                } else {
                    console.log(`GPU object is not available after ${script} is loaded`);
                    const err= Error(LOAD_ERR_MSG);
                    loadedGpuJsFailed= true;
                    waitingRejectors.forEach( (r) => r(err));
                }
                waitingResolvers = [];
                waitingRejectors = [];
            }).catch( () => {
                const err= Error(LOAD_ERR_MSG);
                loadedGpuJsFailed= true;
                waitingRejectors.forEach( (r) => r(err));
                waitingResolvers= [];
                waitingRejectors= [];
            });
        }
        return new Promise( function(resolve, reject) {
            waitingResolvers.push(resolve);
            waitingRejectors.push(reject);
        });
    };
    if (loadNow) getGpuJs();
    return getGpuJs;
}


/**
 * function to return a promise to gpu.js
 */
export const getGpuJs= initGpuJsRetriever(false);


export const getGpuJsImmediate = () => foundGPU;