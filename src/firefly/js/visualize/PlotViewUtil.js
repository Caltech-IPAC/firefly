/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import ImagePlotCntlr from './ImagePlotCntlr.js';
import DrawingLayerCntlr from './DrawingLayerCntlr.js';
import DrawingLayer from './draw/DrawingLayer.js';
import PlotGroup from './PlotGroup.js';
import {flux} from '../Firefly.js';




export default {getPlotViewById, getPrimaryPlot, findPlotView, getPlotViewAry,operateOnOthersInGroup,
                findPlotGroup, findPrimaryPlot, getPlotStateAry, matchPlotView,isActivePlotView,
                getActivePlotView,getAllPlots, getAllDrawingLayers, getAllDrawingLayersStore};

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


function getPlotViewAry() {
    return flux.getState()[ImagePlotCntlr.IMAGE_PLOT_KEY].plotViewAry;
}




function isActivePlotView(plotId) {
    var store= flux.getState()[ImagePlotCntlr.IMAGE_PLOT_KEY];
    return store.activePlotId===plotId;
}

function getActivePlotView() {
    var store= flux.getState()[ImagePlotCntlr.IMAGE_PLOT_KEY];
    return store.plotViewAry.find( (pv) => pv.plotId===store.activePlotId);
}

function getAllPlots() {
    return flux.getState()[ImagePlotCntlr.IMAGE_PLOT_KEY];
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

function hasGroupLock(pv,plotGroup) {
    return (plotGroup &&
            plotGroup.plotGroupId &&
            plotGroup.lockRelated &&
            pv && pv.plotGroupId===plotGroup.plotGroupId);
}

/**
 * Perform an operation on all the PlotViews in a group except the source a return a new version of the plotViewAry
 * The operations are only performed if the group is locked.
 * INSIDE REDUCER
 * @param sourcePv
 * @param plotViewAry
 * @param plotGroup
 * @param operationFunc
 * @return {[]} new plotView array after the operation
 */
function matchPlotView(sourcePv,plotViewAry,plotGroup,operationFunc) {
    if (hasGroupLock(sourcePv,plotGroup)) {
        plotViewAry= plotViewAry.map( (pv) => {
            return (pv.plotGroupId===sourcePv.plotGroupId && pv.plotId!==sourcePv.plotId) ?
                operationFunc(pv) : pv;
        });
    }
    return plotViewAry;
}

/**
 * Perform an operation on all the PlotViews in a group except the source, get the plotViewAry and group from the store.
 * The operations are only performed if the group is locked.
 * OUTSIDE REDUCER
 * @param sourcePv
 * @param plotViewAry
 * @param plotGroup
 * @param operationFunc
 * @return {[]} new plotView array after the operation
 */
function operateOnOthersInGroup(sourcePv,operationFunc) {
    var plotGroup= PlotGroup.getPlotGroupById(sourcePv.plotGroupId);
    if (hasGroupLock(sourcePv,plotGroup)) {
        getPlotViewAry().forEach( (pv) => {
            if (pv.plotGroupId===sourcePv.plotGroupId && pv.plotId!==sourcePv.plotId)  {
                operationFunc(pv);
            }
        });
    }
}


function getAllDrawingLayersStore() {
    return flux.getState()[DrawingLayerCntlr.DRAWING_LAYER_KEY].dlContainerAry;
}

function getAllDrawingLayers(plotId) {
    var ary= flux.getState()[DrawingLayerCntlr.DRAWING_LAYER_KEY].dlContainerAry;
    return ary.filter( (c) => c.drawingLayer.plotIdAry
        .find( (id) => id===plotId||id===DrawingLayer.ALL_PLOTS))
        .map( (c) => c.drawingLayer);
}
