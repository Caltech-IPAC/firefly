/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {difference,isArray,has,isString,omit,isEmpty} from 'lodash';
import {getPlotGroupById} from './PlotGroup.js';
import {makeImagePt, pointEquals} from './Point.js';
import {CsysConverter} from './CsysConverter.js';




const clone = (obj,params={}) => Object.assign({},obj,params);

/**
 * 
 * @param {Object | PlotView[]} ref
 * @return {PlotView[]}
 */
function getPlotViewAry(ref) {
    if (ref.plotViewAry && has(ref,'activePlotId')) { // I was passed the visRoot
        return ref.plotViewAry;
    }
    else if (isArray(ref) && ref.length>0 && ref[0].plotId)  { // passed a plotViewAry
        return ref;
    }
}

/**
 * Take a visRoot, a plotViewAry, or a plotView and find the primePlot.  plotId is required when passing
 * a plotViewAry. plotId is ignored when passing a plotView.  It is optional when passed a visRoot. 
 * When plotId is not included with visRoot it uses the activePlotId.
 *
 * @param {{} |PlotView[]|PlotView} ref this can be the visRoot or the plotViewAry, or a plotView Object.  
 * @param {string} [plotId] the plotId, required with plotViewAry, ignored for  plotView, optional with visRoot
 * @return {WebPlot} the plot
 */
export function primePlot(ref,plotId) {
    var pv;
    if (!ref) return null;
    if (!isString(plotId)) plotId= '';
    if (ref.plotViewAry) { // I was passed the visRoot, use either plot it or the active plot id
        pv= getPlotViewById(ref, plotId ? plotId : ref.activePlotId);
    }
    else if (plotId && isArray(ref) && ref.length>0 && ref[0].plotId) { //i was passed a plotViewAry
        pv= ref.find( (pv) => pv.plotId===plotId);
    }
    else if (ref.plotId && ref.plots) { // i was passed a plotView
        pv= ref;
    }
    return (pv && pv.primeIdx>=0) ? pv.plots[pv.primeIdx] : null;
}


/**
 * @param {{} | PlotView[] } ref this can be the visRoot object or a plotViewAry array.
 * @param {string} plotId
 * @return {object} the plot view object
 */
export function getPlotViewById(ref,plotId) {
    if (!plotId) return null;
    var plotViewAry= getPlotViewAry(ref);
    if (!plotViewAry) return null;

    return plotViewAry.find( (pv) => pv.plotId===plotId);
}

export function getPlotViewIdxById(visRoot,plotId) {
    if (!plotId) return null;
    return visRoot.plotViewAry.findIndex( (pv) => pv.plotId===plotId);
}

export function getPlotGroupIdxById(ref,plotGroupId) {
    if (!ref) return null;
    const plotGroupAry= ref.plotGroupAry ? ref.plotGroupAry : ref;
    return plotGroupAry.findIndex( (pg) => pg.plotGroupId===plotGroupId);
}


/**
 *
 * @param {Object | PlotView[]} ref visRoot or plotViewAry
 * @param [activePlotId]
 * @return {PlotView[]}
 */
export function expandedPlotViewAry(ref,activePlotId=null) {
    var plotViewAry= getPlotViewAry(ref);
    return plotViewAry.filter( (pv) => (pv.plotId===activePlotId || pv.plotViewCtx.inExpandedList));
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
    if (!locked && onlyIfGroupLocked) return [pv.plotId];
    return visRoot.plotViewAry.filter( (pv) => pv.plotGroupId===gid).map( (pv) => pv.plotId);
}


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
 *
 * @param plotOrPv plot or plotView object.  if a plot view then it test the primePlot of the plotView
 * @return {boolean} true if three color false if not or plot is null
 */
export function isThreeColor(plotOrPv) {
    var plot= plotOrPv;
    if (plotOrPv.plots) plot= primePlot(plotOrPv);
    return plot ? plot.plotState.isThreeColor() : false;
}

/**
 * Perform an operation on all the PlotViews in a group except the source, get the plotViewAry and group from the store.
 * The operations are only performed if the group is locked.
 * @param visRoot - root of the visualization object in store
 * @param sourcePv
 * @param operationFunc
 * @param ignoreThreeColor
 * @return {Array} new plotView array after the operation
 */
export function operateOnOthersInGroup(visRoot,sourcePv,operationFunc, ignoreThreeColor=false) {
    var plotGroup= getPlotGroupById(visRoot,sourcePv.plotGroupId);
    if (hasGroupLock(sourcePv,plotGroup)) {
        visRoot.plotViewAry.forEach( (pv) => {
            if (ignoreThreeColor && isThreeColor(primePlot(pv))) return;
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
 * @param ref - the root of the drawing layer controller or the master array of all drawing layers
 * @param plotId
 * @param mustBeVisible
 * @return {Array}
 */
export function getAllDrawLayersForPlot(ref,plotId,mustBeVisible=false) {
    if (!plotId) return [];
    var dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
    if (isEmpty(dlAry)) return [];
    if (mustBeVisible) {
        return dlAry.filter( (dl) => dl.visiblePlotIdAry.includes(plotId));
    }
    else {
        return dlAry.filter( (dl) => dl.plotIdAry.includes(plotId));
    }
}


export function getConnectedPlotsIds(ref, drawLayerId) {
    var dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
    const dl= getDrawLayerById(dlAry,drawLayerId);
    return dl ? dl.plotIdAry : [];
}



/**
 *
 * @param ref - the root of the drawing layer controller or the master array of all drawing layers
 * @param typeId
 * @return {object} the draw layer
 */
export function getDrawLayerByType(ref,typeId) {
    var dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
    return dlAry.find( (dl) => dl.drawLayerTypeId===typeId);
}

/**
 *
 * @param ref - the root of the drawing layer controller or the master array of all drawing layers
 * @param id draw layer id
 * @return {object} the draw layer
 */
export function getDrawLayerById(ref,id) {
    var dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
    return dlAry.find( (dl) => dl.drawLayerId===id);
}

/**
 * UNTESTED - I think I will need this eventually
 * @param ref - the root of the drawing layer controller or the master array of all drawing layers
 * @param displayGroupId
 * @return {object} the draw layer
 */
export function getDrawLayersByDisplayGroup(ref,displayGroupId) {
    var dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
    return dlAry.find( (dl) => dl.displayGroupId===displayGroupId);
}

/**
 * UNTESTED - I think I will need this eventually
 * @param dl1Ary
 * @param dl2Ary
 * @return {boolean}
 */
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
export function getLayerTitle(plotId,dl) { return (typeof dl.title === 'string') ? dl.title : dl.title[plotId]; }

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
export function getPlotStateAry(pv) {
    var overlayStates= pv.overlayPlotViews.map( (opv) => opv.plot.plotState);
    const p= primePlot(pv);
    const pvStateAry= p ? [p.plotState] : [];
    return [...pvStateAry, ...overlayStates];
}

/**
 *
 * @param {object} pv
 * @param {object} plotGroup
 * @return {boolean}
 */
export function hasGroupLock(pv,plotGroup) {
    return Boolean(plotGroup && plotGroup.plotGroupId && plotGroup.lockRelated &&
                    pv && pv.plotGroupId===plotGroup.plotGroupId);
}

//--------------------------------------------------------------
//--------------------------------------------------------------
//--------- Inside reducer functions
//--------------------------------------------------------------
//--------------------------------------------------------------



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
 * @return {Array} new plotView array after the operation
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
 * @return {Array} new plotViewAry
 */
export function applyToOnePvOrGroup(plotViewAry, plotId,plotGroup,operationFunc) {
    var groupLock= hasGroupLock(getPlotViewById(plotViewAry,plotId),plotGroup);
    return plotViewAry.map( (pv) => {
        if (pv.plotId===plotId) return operationFunc(pv);
        else if (groupLock && pv.plotGroupId===plotGroup.plotGroupId) return operationFunc(pv);
        else return pv;
    });
}


/**
 * based on groupLock return an array which will either contain one plotView or the whole group 
 * @param plotViewAry
 * @param plotId
 * @param plotGroup
 * @return {*}
 */
export function getOnePvOrGroup(plotViewAry, plotId,plotGroup) {
    var groupLock= hasGroupLock(getPlotViewById(plotViewAry,plotId),plotGroup);
    return groupLock ? 
        plotViewAry.filter( (pv) => pv.plotGroupId===plotGroup.plotGroupId) : 
        [getPlotViewById(plotViewAry,plotId)];
}


/**
 * make a new copy of the plotview array with an object set on a cloned copy of the matched plotview.
 * @param ref visRoot or plotViewAry
 * @param {string} plotId
 * @param {{}} obj fields to replace
 * @return {[]}
 */
export function clonePvAry(ref, plotId, obj) {
    var plotViewAry= getPlotViewAry(ref);
    if (!plotViewAry) return null;

    return plotViewAry.map( (pv) => pv.plotId===plotId ? clone(pv,obj) : pv);
}

/**
 * make a new copy of the plotview array with the passed plotView replacing the old one
 * @param ref visRoot or plotViewAry
 * @param {obj} plotView
 * @return {[]}
 */
export function clonePvAryWithPv(ref, plotView) {
    var plotViewAry= getPlotViewAry(ref);
    if (!plotViewAry) return null;
    return plotViewAry.map( (pv) => pv.plotId===plotView.plotId ? plotView : pv);
}



/**
 *
 * @param pv plot view
 * @return {boolean}
 */
export function isMultiImageFitsWithSameArea(pv) {
    if (!pv.plotViewCtx.containsMultiImageFits) return false;
    var plot= primePlot(pv);
    var {dataWidth:w, dataHeight:h} = plot;

    var ic1= makeImagePt(0,0);
    var ic2= makeImagePt(w,0);
    var ic3= makeImagePt(0,h);
    var ic4= makeImagePt(w,h);

    var projName= plot.projection.getProjectionName();
    var cc= CsysConverter.make(plot);

    var c1= cc.getWorldCoords(ic1);
    var c2= cc.getWorldCoords(ic2);
    var c3= cc.getWorldCoords(ic3);
    var c4= cc.getWorldCoords(ic4);
    if (!c1 || !c2 || !c3 || !c4) return false;

    return pv.plots.every( (p) => {
        if (w!==p.dataWidth || h!==p.dataHeight) return false;
        if (projName!==p.projection.getProjectionName()) return false;

        var pCC= CsysConverter.make(p);
        var iwc1= pCC.getWorldCoords(ic1);
        var iwc2= pCC.getWorldCoords(ic2);
        var iwc3= pCC.getWorldCoords(ic3);
        var iwc4= pCC.getWorldCoords(ic4);
        if (!iwc1 || !iwc2 || !iwc3 || !iwc4) return false;

        return (pointEquals(iwc1,c1) && 
                pointEquals(iwc2,c2) && 
                pointEquals(iwc3,c3) && 
                pointEquals(iwc4,c4) );
    });
}






