/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {loadScript, getRootURL} from '../util/WebUtil.js';
import {logger} from '../util/Logger.js';

const PLOTLY_SCRIPT= 'plotly-2.32.0.min.js';
const LOAD_ERR_MSG= 'Load Failed: could not load Plotly';

function initPlotLyRetriever(loadNow) {
    let plotlyLoadBegin= false;
    let waitingResolvers= [];
    let waitingRejectors= [];
    let loadedPlotlyFailed;
    let loadedPlotly;

    const getPlotLy= () => {
        if (loadedPlotly || loadedPlotlyFailed) {
            return loadedPlotly ? Promise.resolve(loadedPlotly) : Promise.reject(Error(LOAD_ERR_MSG));
        }

        const script= `${getRootURL()}${PLOTLY_SCRIPT}`;
        if (!plotlyLoadBegin) {
            plotlyLoadBegin= true;
            loadScript(script).then( () => {
                loadedPlotly= window.Plotly;
                if (loadedPlotly) {
                    waitingResolvers.forEach((r) => r(loadedPlotly));
                } else {
                    logger.error(`Plotly object is not available after ${script} is loaded`);
                    const err= Error(LOAD_ERR_MSG);
                    loadedPlotlyFailed= true;
                    waitingRejectors.forEach( (r) => r(err));
                }
                waitingResolvers = [];
                waitingRejectors = [];
            }).catch( () => {
                const err= Error(LOAD_ERR_MSG);
                loadedPlotlyFailed= true;
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
    if (loadNow) getPlotLy();
    return getPlotLy;
}


/**
 * function to return a promise to PlotLy
 */
export const getPlotLy= initPlotLyRetriever(false);
