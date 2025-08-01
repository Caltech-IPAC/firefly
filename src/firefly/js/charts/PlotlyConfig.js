/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {logger} from '../util/Logger.js';

const LOAD_ERR_MSG= 'Load Failed: could not load Plotly';
let loaderPromise = null;

export function getPlotLy() {
    if (!loaderPromise) {
        // Dynamically import Plotly.js so that it is not included in the main bundle (code splitting)
        loaderPromise = import(/* webpackChunkName: "plotly" */ 'plotly.js-dist-min')
            .then((mod) => mod.default || mod)
            .catch((err) => {
                loaderPromise = null; // so that we can retry next time
                logger.error(`Plotly import failed: ${err.message}`);
                throw new Error(LOAD_ERR_MSG);
            });
    }
    return loaderPromise;
}
