/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import DrawLayer from './draw/DrawLayer.js';
import {getPlotGroupById} from './PlotGroup.js';
import {flux} from '../Firefly.js';




export default {
    findPlotView, getPlotViewAry,operateOnOthersInGroup,
    findPlotGroup, findPrimaryPlot, getPlotStateAry, matchPlotView,isActivePlotView,
    hasGroupLock, getActivePlotView, getAllDrawLayers, getAllDrawLayersForPlot,
    getPlotViewIdListInGroup, getDrawLayerByType,
    isDrawLayerVisible, isDrawLayerAttached, getLayerTitle
};



//--------------------------------------------------------------
//--------------------------------------------------------------
//--------- outside of functions
//--------------------------------------------------------------
//--------------------------------------------------------------



/**
 * get the plot view with the id
 * @param visRoot - root of the visualization object in store
 * @param {string} plotId
 * @return {object} the plot view object
 */
export function getPlotViewById(visRoot,plotId) {
    if (!plotId) return null;
    return visRoot.plotViewAry.find( (pv) => pv.plotId===plotId);
}

/**
 * Return an array of plotId's that are in the plot group associated with the the pvOrId parameter.
 * @param visRoot - root of the visualization object in store
 * @param pvOrId this parameter will take the plotId string or a plotView object
 * @param onlyIfGroupLocked
 * @return {*}
 */
export function getPlotViewIdListInGroup(visRoot,pvOrId,onlyIfGroupLocked=true) {
    if (!pvOrId) return [];
    var pv= (typeof pvOrId ==='string') ? getPlotViewById(visRoot,pvOrId) : pvOrId;
    var gid= pv.plotGroupId;
    var group= getPlotGroupById(visRoot,gid);
    var locked= hasGroupLock(pv,group);
    if (!locked && onlyIfGroupLocked) return [pv];
    return getPlotViewAry(visRoot).filter( (pv) => pv.plotGroupId===gid).map( (pv) => pv.plotId);
}

/**
 * @param visRoot - root of the visualization object in store
 * the the PlotView array from the store
 * @return {Array}
 */
function getPlotViewAry(visRoot) { return visRoot.plotViewAry; }

/**
 * Is this plotview the active one
 * @param visRoot - root of the visualization object in store
 * @param plotId
 * @return {boolean} is active, there will be only one active at a time
 */
export function isActivePlotView(visRoot,plotId) { return visRoot.activePlotId===plotId; }

/**
 * Get the active PlotView from the store
 * @param visRoot - root of the visualization object in store
 * @return {object} the active plot view
 */
export function getActivePlotView(visRoot) {
    return visRoot.plotViewAry.find( (pv) => pv.plotId===visRoot.activePlotId);
}


/**
 * Perform an operation on all the PlotViews in a group except the source, get the plotViewAry and group from the store.
 * The operations are only performed if the group is locked.
 * @param visRoot - root of the visualization object in store
 * @param sourcePv
 * @param operationFunc
 * @return {[]} new plotView array after the operation
 */
function operateOnOthersInGroup(visRoot,sourcePv,operationFunc) {
    var plotGroup= getPlotGroupById(visRoot,sourcePv.plotGroupId);
    if (hasGroupLock(sourcePv,plotGroup)) {
        getPlotViewAry(visRoot).forEach( (pv) => {
            if (pv.plotGroupId===sourcePv.plotGroupId && pv.plotId!==sourcePv.plotId)  {
                operationFunc(pv);
            }
        });
    }
}


//--------------------------------------------------------------
//--------- Drawing Layer outside functions
//--------------------------------------------------------------



/**
 * Get all drawing layers container from the store
 * @return {Array}
 */
export function getAllDrawLayers(dlRoot) { return dlRoot.drawLayerAry; }

/**
 * construct an array of drawing layer from the store
 * @param dlAry - the master array of all drawing layers
 * @param plotId
 * @return {Array}
 */
export function getAllDrawLayersForPlot(dlAry,plotId) {
    return dlAry
        .filter( (dl) => dl.plotIdAry
        .find( (id) => id===plotId));
}


export function getDrawLayerByType(dlAry,typeId) {
    return dlAry.find( (dl) => dl.drawLayerTypeId===typeId);
}

export function getDrawLayersByDisplayGroup(dlAry,displayGroupId) {
    return dlAry.find( (dl) => dl.displayGroupId===displayGroupId);
}

/**
 *
 * @param {Object} dl the drawLayer
 * @param plotId
 * @return {boolean}
 */
export function isDrawLayerVisible(dl, plotId) { return dl ? dl.visiblePlotIdAry.includes(plotId) : false; }

function isDrawLayerAttached(dl, plotId) { return dl ? dl.plotIdAry.includes(plotId) : false; }

/**
 *
 * @param plotId
 * @param dl
 * @return {*}
 */
function getLayerTitle(plotId,dl) { return (typeof dl.title === 'string') ? dl.title : dl.title[plotId]; }

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

