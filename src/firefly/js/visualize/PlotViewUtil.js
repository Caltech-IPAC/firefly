/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import ImagePlotCntlr from './ImagePlotCntlr.js';
import DrawLayerCntlr from './DrawLayerCntlr.js';
import DrawLayer from './draw/DrawLayer.js';
import PlotGroup from './PlotGroup.js';
import {flux} from '../Firefly.js';




export default {getPlotViewById, getPrimaryPlot, findPlotView, getPlotViewAry,operateOnOthersInGroup,
                findPlotGroup, findPrimaryPlot, getPlotStateAry, matchPlotView,isActivePlotView,
                getActivePlotView,getAllPlots, getAllDrawLayers, getAllDrawLayersStore,
                getPlotViewIdListInGroup, getDrawLayerByType, isDrawLayerVisible, isDrawLayerAttached};



//--------------------------------------------------------------
//--------------------------------------------------------------
//--------- outside of functions
//--------------------------------------------------------------
//--------------------------------------------------------------



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
 * @param pvOrId this parameter will take the plotId or a plotView object
 * @param onlyIfGroupLocked
 * @return {*}
 */
function getPlotViewIdListInGroup(pvOrId,onlyIfGroupLocked=true) {
    if (!pvOrId) return [];
    var pv= (typeof pv ==='string') ? getPlotViewById(pvOrId) : pvOrId;
    var gid= pv.plotGroupId;
    var group= PlotGroup.getPlotGroupById(gid);
    var locked= hasGroupLock(pv,group);
    if (!locked && onlyIfGroupLocked) return [pv];
    return getPlotViewAry().filter( (pv) => pv.plotGroupId===gid).map( (pv) => pv.plotId);
}

/**
 * the the PlotView array from the store
 * FOR USE OUTSIDE OF REDUCER
 * @return {Array}
 */
function getPlotViewAry() {
    return flux.getState()[ImagePlotCntlr.IMAGE_PLOT_KEY].plotViewAry;
}

/**
 * Is this plotview the active one
 * FOR USE OUTSIDE OF REDUCER
 * @param plotId
 * @return {boolean} is active, there will be only one active at a time
 */
function isActivePlotView(plotId) {
    var store= flux.getState()[ImagePlotCntlr.IMAGE_PLOT_KEY];
    return store.activePlotId===plotId;
}

/**
 * Get the active PlotView from the store
 * FOR USE OUTSIDE OF REDUCER
 * @return {object} the active plot view
 */
function getActivePlotView() {
    var store= flux.getState()[ImagePlotCntlr.IMAGE_PLOT_KEY];
    return store.plotViewAry.find( (pv) => pv.plotId===store.activePlotId);
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
/**
 * get the all plot object from the store
 * FOR USE OUTSIDE OF REDUCER
 * @return {object}
 */
function getAllPlots() {
    return flux.getState()[ImagePlotCntlr.IMAGE_PLOT_KEY];
}


/**
 * get the primary plot by the plot id
 * FOR USE OUTSIDE OF REDUCER
 * @param {string } plotId
 * @return {object} the primary plot for this plot id
 */
function getPrimaryPlot(plotId) {
    var pv= getPlotViewById(plotId);
    return pv && pv.primaryPlot ? pv.primaryPlot : null;
}

//--------------------------------------------------------------
//--------- Drawing Layer outside functions
//--------------------------------------------------------------



/**
 * Get all drawing layers container from the store
 * FOR USE OUTSIDE OF REDUCER
 * @return {Array}
 */
function getAllDrawLayersStore() {
    return flux.getState()[DrawLayerCntlr.DRAWING_LAYER_KEY].drawLayerAry;
}

/**
 * construct an array of drawing layer from the store
 * FOR USE OUTSIDE OF REDUCER
 * @param plotId
 * @return {Array}
 */
function getAllDrawLayers(plotId) {
    var ary= flux.getState()[DrawLayerCntlr.DRAWING_LAYER_KEY].drawLayerAry;
    return ary.filter( (dl) => dl.plotIdAry
        .find( (id) => id===plotId||id===DrawLayer.ALL_PLOTS));
}


function getDrawLayerByType(plotId, typeId) {
    return getAllDrawLayers(plotId).find( (dl) => dl.drawLayerTypeId===typeId);
}

/**
 *
 * @param {Object} dl the drawLayer
 * @param plotId
 * @return {boolean}
 */
function isDrawLayerVisible(dl, plotId) { return dl ? dl.visiblePlotIdAry.includes(plotId) : false; }

function isDrawLayerAttached(dl, plotId) { return dl ? dl.plotIdAry.includes(plotId) : false; }


//--------------------------------------------------------------
//--------------------------------------------------------------
//--------- general functions
//--------------------------------------------------------------
//--------------------------------------------------------------



/**
 * make an array of plot starts from the primary plot and all its image overlays
 * @param pv
 * @return {*[]}
 */
function getPlotStateAry(pv) {
    var overlayStates= pv.overlayPlotViews.map( (opv) => opv.plot.plotState);
    return [pv.primaryPlot.plotState, ...overlayStates];
}

/**
 *
 * @param {object} pv
 * @param {object} plotGroup
 * @return {boolean}
 */
function hasGroupLock(pv,plotGroup) {
    return (plotGroup &&
    plotGroup.plotGroupId &&
    plotGroup.lockRelated &&
    pv && pv.plotGroupId===plotGroup.plotGroupId);
}

//--------------------------------------------------------------
//--------------------------------------------------------------
//--------- Inside reducer functions
//--------------------------------------------------------------
//--------------------------------------------------------------


/**
 * find the plotView from the plotViewAry
 * USE INSIDE REDUCER ONLY
 * @param plotId
 * @param plotViewAry
 * @return {*}
 */
function findPlotView(plotId, plotViewAry) {
    if (!plotId || !plotViewAry) return null;
    return plotViewAry.find( (pv) => pv.plotId===plotId);
}

/**
 * find the primaryPlot from the plotViewAry
 * USE INSIDE REDUCER ONLY
 * @param plotId
 * @param plotViewAry
 * @return {null}
 */
function findPrimaryPlot(plotId, plotViewAry) {
    if (!plotId || !plotViewAry) return null;
    var pv= findPlotView(plotId,plotViewAry);
    return pv && pv.primaryPlot ? pv.primaryPlot : null;
}


/**
 * find the plot group from the array
 * USE INSIDE REDUCER ONLY
 * @param plotGroupId
 * @param plotGroupAry
 * @return {*}
 */
function findPlotGroup(plotGroupId, plotGroupAry) {
    if (!plotGroupId || !plotGroupAry) return null;
    return plotGroupAry.find( (pg) => pg.plotGroupId===plotGroupId);
}


/**
 * Perform an operation on all the PlotViews in a group except the source a return a new version of the plotViewAry
 * The operations are only performed if the group is locked.
 * USE INSIDE REDUCER ONLY
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

