/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import ImagePlotCntlr from './ImagePlotCntlr.js';
import {flux} from '../Firefly.js';




export default {getPlotViewById, getPrimaryPlot, findPlotView, findPlotGroup, findPrimaryPlot, getPlotStateAry};

/**
 * get the plot view with the id
 * @param {string} plotId
 * @return {object} the plot view object
 */
function getPlotViewById(plotId) {
    if (!plotId) return null;
    var pv= flux.getState()[ImagePlotCntlr.IMAGE_PLOT_KEY].plotViewAry.find( (pv) => pv.plotId===plotId);
    return pv;
}

/**
 *
 * @param {string } plotId
 * @return {object} the primary plot for this plot id
 */
function getPrimaryPlot(plotId) {
    var pv= getPlotViewById(plotId);
    return pv && pv.primaryPlot ? pv.primaryPlot : null;
}



function findPlotView(plotId, plotViewAry) {
    if (!plotId || !plotViewAry) return null;
    return plotViewAry.find( (pv) => pv.plotId===plotId);
}

function findPrimaryPlot(plotId, plotViewAry) {
    if (!plotId || !plotViewAry) return null;
    var pv= findPlotView(plotId,plotViewAry);
    return pv && pv.primaryPlot ? pv.primaryPlot : null;
}



function findPlotGroup(plotGroupId, plotGroupAry) {
    if (!plotGroupId || !plotGroupAry) return null;
    return plotGroupAry.find( (pg) => pg.plotGroupId===plotGroupId);
}

/**
 *
 * @param pv
 * @return {*[]}
 */
function getPlotStateAry(pv) {
    var overlayStates= pv.overlayPlotViews.map( (opv) => opv.plot.plotState);
    return [pv.primaryPlot.plotState, ...overlayStates];
}
