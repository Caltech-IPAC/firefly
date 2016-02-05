/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getPlotGroupById} from './PlotGroup.js';
import difference from 'lodash/array/difference';




export default {
    findPlotView, getPlotViewAry,operateOnOthersInGroup,
    findPlotGroup, getPlotStateAry, matchPlotView,isActivePlotView,
    hasGroupLock, getActivePlotView, getAllDrawLayers, getAllDrawLayersForPlot,
    getPlotViewIdListInGroup, getDrawLayerByType,
    isDrawLayerVisible, isDrawLayerAttached, getLayerTitle
};


/**
 *
 * @param {{} | [] }ref this can be the visRoot or a plotView Object.  If it is view root then it will
 * find the active plotView and get the primePlot otherwise it will find the primePlot in the plotView
 * @param {string} [plotId] this is passed then the behavior changes some.  If the ref is a visRoot then it will used the passed
 * plotId instead of the visRoot active plotId. Also when the plotId is passed then ref is also allowed to be the
 * plotViewAry.  In this case, it will find the prime plot on the plotViewAry by looking up the plotView in the array
 * and then returning the primePLot
 */
export function primePlot(ref,plotId) {
    var pv;
    if (!ref) return null;
    if (typeof plotId !== 'string') plotId= null;
    if (ref.plotViewAry && ref.activePlotId) { // I was passed the visRoot
        var id= plotId?plotId:ref.activePlotId;
        pv= getPlotViewById(ref,id);
    }
    else if (plotId && Array.isArray(ref) && ref.length>0 && ref[0].plotId) { //i was passed a plotViewAry
        pv= ref.find( (pv) => pv.plotId===plotId);
    }
    else if (ref.plotId && ref.plots) { // i was passed a plotView
        pv= ref;
    }
    return pv ? pv.plots[pv.primeIdx] : null;
}


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

export function getPlotViewIdxById(visRoot,plotId) {
    if (!plotId) return null;
    return visRoot.plotViewAry.findIndex( (pv) => pv.plotId===plotId);
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
export function operateOnOthersInGroup(visRoot,sourcePv,operationFunc) {
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

export function getDrawLayerById(dlAry,id) {
    return dlAry.find( (dl) => dl.drawLayerId===id);
}

export function getDrawLayersByDisplayGroup(dlAry,displayGroupId) {
    return dlAry.find( (dl) => dl.displayGroupId===displayGroupId);
}

export function drawLayersDiffer(dl1Ary,dl2Ary) {
    if (dl1Ary===dl2Ary) return false;
    if (dl1Ary.length!==dl2Ary.length) return true;
    return difference(dl1Ary,dl2Ary).length>0;
}

/**
 *
 * @param {Object} dl the drawLayer
 * @param plotId
 * @return {boolean}
 */
export function isDrawLayerVisible(dl, plotId) { return dl ? dl.visiblePlotIdAry.includes(plotId) : false; }

export function isDrawLayerAttached(dl, plotId) { return dl ? dl.plotIdAry.includes(plotId) : false; }

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
    return [primePlot(pv).plotState, ...overlayStates];
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
export function findPlotView(plotId, plotViewAry) {
    if (!plotId || !plotViewAry) return null;
    return plotViewAry.find( (pv) => pv.plotId===plotId);
}

export function findPlotViewIdx(plotId, plotViewAry) {
    if (!plotId || !plotViewAry) return null;
    return plotViewAry.findIndex( (pv) => pv.plotId===plotId);
}



/**
 * find the plot group from the array
 * USE INSIDE REDUCER ONLY
 * @param plotGroupId
 * @param plotGroupAry
 * @return {*}
 */
export function findPlotGroup(plotGroupId, plotGroupAry) {
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
export function matchPlotView(sourcePv,plotViewAry,plotGroup,operationFunc) {
    if (hasGroupLock(sourcePv,plotGroup)) {
        plotViewAry= plotViewAry.map( (pv) => {
            return (pv.plotGroupId===sourcePv.plotGroupId && pv.plotId!==sourcePv.plotId) ?
                operationFunc(pv) : pv;
        });
    }
    return plotViewAry;
}


/**
 * perform an operation or a plotView or its related group depending on the lock state.
 * @param plotViewAry plotViewAry
 * @param plotId the that is primary.
 * @param plotGroup the group to check against
 * @param operationFunc the function to operate on the other plot views
 * @return {[]} new plotViewAry
 */
export function applyToOnePvOrGroup(plotViewAry, plotId,plotGroup,operationFunc) {
    var groupLock= hasGroupLock(findPlotView(plotId,plotViewAry),plotGroup);
    return plotViewAry.map( (pv) => {
        if (pv.plotId===plotId) return operationFunc(pv);
        else if (groupLock && pv.plotGroupId===plotGroup.plotGroupId) return operationFunc(pv);
        else return pv;
    });
}
