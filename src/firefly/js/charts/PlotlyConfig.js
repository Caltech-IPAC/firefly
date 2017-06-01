/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



// NOTE - you should uncomment any plot type that Firefly is using.
// NOTE - Remember to uncomment both the import and the register

import {loadScript} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';
/*
import Plotly from 'plotly.js/lib/core';
import bar from 'plotly.js/lib/bar';
import box from 'plotly.js/lib/box';
import candlestick from 'plotly.js/lib/candlestick';
import histogram from 'plotly.js/lib/histogram';
import pie from 'plotly.js/lib/pie';
import scatter from 'plotly.js/lib/scatter';
*/

// import calendars from 'plotly.js/lib/calendars';
// import choropleth from 'plotly.js/lib/choropleth';
// import contour from 'plotly.js/lib/contour';
// import contourgl from 'plotly.js/lib/contourgl';
// import heatmap from 'plotly.js/lib/heatmap';
// import heatmapgl from 'plotly.js/lib/heatmapgl';
// import histogram2d from 'plotly.js/lib/histogram2d';
// import histogram2dcontour from 'plotly.js/lib/histogram2dcontour';
// import mesh3d from 'plotly.js/lib/mesh3d';
// import ohlc from 'plotly.js/lib/ohlc';
// import pointcloud from 'plotly.js/lib/pointcloud';
// import scatter3d from 'plotly.js/lib/scatter3d';
// import scattergeo from 'plotly.js/lib/scattergeo';
// import scattergl from 'plotly.js/lib/scattergl';
// import scattermapbox from 'plotly.js/lib/scattermapbox'; // build does not work for this one
// import scatterternary from 'plotly.js/lib/scatterternary';
// import surface from 'plotly.js/lib/surface';



/*
Plotly.register([
    bar,
    box,
    candlestick,
    histogram,
    pie,
    scatter,
    // calendars,
    // choropleth,
    // contour,
    // contourgl,
    // heatmap,
    // heatmapgl,
    // histogram2d,
    // histogram2dcontour,
    // mesh3d,
    // ohlc,
    // pointcloud,
    // scatter3d,
    // scattergeo,
    // scattergl,
    // scattermapbox, // build does not work for this one
    // scatterternary,
    // surface,
]);

export default Plotly;
*/




const PLOTLY_SCRIPT= 'plotly-1.27.1.min.js';
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
