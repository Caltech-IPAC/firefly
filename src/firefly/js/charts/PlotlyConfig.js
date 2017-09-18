/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {loadScript} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';


const PLOTLY_SCRIPT= 'plotly-1.28.2.min.js';
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

        const script= `${getRootURL()}/${PLOTLY_SCRIPT}`;
        if (!plotlyLoadBegin) {
            plotlyLoadBegin= true;
            loadScript(script).then( () => {
                loadedPlotly= window.Plotly;
                waitingResolvers.forEach( (r) => r(loadedPlotly));
                waitingResolvers= undefined;
                waitingRejectors= undefined;
            }).catch( () => {
                const err= Error(LOAD_ERR_MSG);
                waitingRejectors.forEach( (r) => r(err));
                loadedPlotlyFailed= true;
                waitingResolvers= undefined;
                waitingRejectors= undefined;
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
export const getPlotLy= initPlotLyRetriever(true);
