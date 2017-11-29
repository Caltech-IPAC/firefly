/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {difference,isArray,has,isString,get, isEmpty,flatten} from 'lodash';
import {getPlotGroupById} from './PlotGroup.js';
import {makeImagePt, pointEquals} from './Point.js';
import {CysConverter} from './CsysConverter.js';
import {clone} from '../util/WebUtil.js';
import {getDlAry, dispatchDestroyDrawLayer} from './DrawLayerCntlr.js';
import {makeWorldPt} from './Point.js';



export const CANVAS_IMAGE_ID_START= 'image-';
export const CANVAS_DL_ID_START= 'dl-';
/**
 * 
 * @param {VisRoot | PlotView[]} ref
 * @returns {PlotView[]}
 */
export function getPlotViewAry(ref) {
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
 * @param {PlotView[]|PlotView|VisRoot} ref this can be the visRoot or the plotViewAry, or a plotView Object.
 * @param {string} [plotId] the plotId, required with plotViewAry, ignored for  plotView, optional with visRoot
 * @returns {WebPlot} the plot
 */
export function primePlot(ref,plotId) {
    let pv;
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
 * @param {VisRoot | PlotView[] } ref this can be the visRoot object or a plotViewAry array.
 * @param {string} plotId
 * @returns {PlotView} the plot view object
 */
export function getPlotViewById(ref,plotId) {
    if (!plotId) return null;
    const plotViewAry= getPlotViewAry(ref);
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
 * @returns {PlotView[]}
 */
export function expandedPlotViewAry(ref,activePlotId=null) {
    const plotViewAry= getPlotViewAry(ref);
    return plotViewAry.filter( (pv) => (pv.plotId===activePlotId || pv.plotViewCtx.inExpandedList));
}


/**
 * Return an array of plotId's that are in the plot group associated with the the pvOrId parameter.
 * @param {VisRoot} visRoot - root of the visualization object in store
 * @param pvOrId this parameter will take the plotId string or a plotView object
 * @param onlyIfGroupLocked
 * @param hasPlots
 * @returns {*}
 */
export function getPlotViewIdListInGroup(visRoot,pvOrId,onlyIfGroupLocked=true, hasPlots=false) {
    if (!pvOrId) return [];
    const pv= (typeof pvOrId ==='string') ? getPlotViewById(visRoot,pvOrId) : pvOrId;
    const gid= pv.plotGroupId;
    const group= getPlotGroupById(visRoot,gid);
    const locked= hasGroupLock(pv,group);
    if (!locked && onlyIfGroupLocked) return [pv.plotId];
    const idList=  visRoot.plotViewAry.filter( (pv) => pv.plotGroupId===gid).map( (pv) => pv.plotId);
    if (!hasPlots) return idList;

    return idList.filter( (id) => get(getPlotViewById(visRoot,id),'plots.length') );
}

/**
 * return an array of plotIds that are all under visRoot and based on the group lock of the group associated
 * width the pvOrId parameter
 * @param visRoot
 * @param pvOrId
 * @param hasPlots
 * @returns {*}
 */
export function getAllPlotViewId(visRoot, pvOrId, hasPlots=false) {
    if (!pvOrId) return [];
    const pv= (typeof pvOrId ==='string') ? getPlotViewById(visRoot,pvOrId) : pvOrId;
    const gid= pv.plotGroupId;
    const group= getPlotGroupById(visRoot,gid);
    const locked= hasGroupLock(pv,group);

    if (!locked) {
        return [pv.plotId];
    } else {
        return visRoot.plotViewAry.filter((pv) => (!hasPlots || get(pv, 'plots.length')))
                                  .map((pv) => pv.plotId);
    }
}

/**
 * Is this plotview the active one
 * @param {VisRoot} visRoot - root of the visualization object in store
 * @param plotId
 * @returns {boolean} is active, there will be only one active at a time
 */
export function isActivePlotView(visRoot,plotId) { return visRoot.activePlotId===plotId; }

/**
 * Get the active PlotView from the store
 * @param {VisRoot} visRoot - root of the visualization object in store
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
    let plot= plotOrPv;
    if (isPlotView(plotOrPv)) plot= primePlot(plotOrPv);
    return plot ? plot.plotState.isThreeColor() : false;
}

/**
 * Return true if this is a PlotView object
 * @param obj
 * @return boolean
 */
export function isPlotView(obj) {
    return Boolean(obj && obj.plots && obj.plotId &&
        obj.options && obj.viewDim &&
        obj.overlayPlotViews && obj.plotViewCtx);
}


/**
 *
 * @param {PlotView} plotView
 * @param {String} imageOverlayId
 * @return {OverlayPlotView}
 */
export function getOverlayById(plotView, imageOverlayId) {
    if (!plotView) return null;
    return plotView.overlayPlotViews.find( (opv) => opv.imageOverlayId===imageOverlayId);
}

/**
 * Look for a cached image tile that has been reprocessed from the original. Match by plotId and the original
 * url key of the unprocessed tile
 * @param {VisRoot} visRoot
 * @param {string} plotId
 * @param {string} originalUrlkey
 * @return {ClientTile}
 */
export function findProcessedTile(visRoot, plotId, originalUrlkey) {
    if (!visRoot) return undefined;
    return flatten(visRoot.processedTiles.filter( (e) => e.plotId===plotId).map( (e) => e.clientTileAry))
        .find( (tile) => tile.url===originalUrlkey );
}


/**
 *
 * @param ref
 * @param plotId
 * @param imageOverlayId
 * @return {OverlayPlotView}
 */
export function getOverlayByPvAndId(ref,plotId,imageOverlayId) {
    return getOverlayById(getPlotViewById(ref,plotId),imageOverlayId);
}


/**
 * construct an array of drawing layer from the store
 * @param ref - the root of the drawing layer controller or the master array of all drawing layers
 * @param plotId
 * @param mustBeVisible
 * @return {Array}
 */
export function getAllDrawLayersForPlot(ref,plotId,mustBeVisible=false) {
    if (!ref) return undefined;
    if (!plotId) return [];
    const dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
    if (isEmpty(dlAry)) return [];
    if (mustBeVisible) {
        return dlAry.filter( (dl) => dl.visiblePlotIdAry.includes(plotId));
    }
    else {
        return dlAry.filter( (dl) => dl.plotIdAry.includes(plotId));
    }
}


export function getConnectedPlotsIds(ref, drawLayerId) {
    if (!ref) return null;
    const dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
    const dl= getDrawLayerById(dlAry,drawLayerId);
    return dl ? dl.plotIdAry : [];
}



/**
 * get the first found drawing layer of type 'typeId'
 * @param {DrawLayer[]|DrawLayerRoot} ref - the root of the drawing layer controller or the master array of all drawing layers
 * @param typeId
 * @return {object} the draw layer
 */
export function getDrawLayerByType(ref,typeId) {
    if (!ref) return undefined;
    const dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
    return dlAry.find( (dl) => dl.drawLayerTypeId===typeId);
}

/**
 * get all drawing layers of type 'typeId'
 * @param {DrawLayer[]|DrawLayerRoot} ref - the root of the drawing layer controller or the master array of all drawing layers
 * @param typeId
 * @return {object} the draw layer
 */
export function getDrawLayersByType(ref,typeId) {
    if (!ref) return undefined;
    const dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
    return dlAry.filter( (dl) => dl.drawLayerTypeId===typeId);
}

/**
 *
 * @param {DrawLayer[]|DrawLayerRoot} ref - the root of the drawing layer controller or the master array of all drawing layers
 * @param id draw layer id
 * @returns {Array} the draw layer
 */
export function getDrawLayerById(ref,id) {
    if (!ref) return undefined;
    const dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
    return dlAry.find( (dl) => dl.drawLayerId===id);
}

/**
 * UNTESTED - I think I will need this eventually
 * @param {DrawLayer[]|DrawLayerRoot} ref - the root of the drawing layer controller or the master array of all drawing layers
 * @param {string} displayGroupId
 * @return {Array} the draw layer
 */
export function getDrawLayersByDisplayGroup(ref,displayGroupId) {
    const dlAry= ref.drawLayerAry ? ref.drawLayerAry : ref;
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
 * True is the drawing layer is visible
 * @param {Object} dl the drawLayer
 * @param {string} plotId
 * @returns {boolean}
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


export function deleteAllDrawLayers() {
    (getDlAry() || [])
        .filter((l) => l.drawLayerId)
        .forEach((id) => dispatchDestroyDrawLayer(id));
}

//--------------------------------------------------------------
//--------------------------------------------------------------
//--------- general functions
//--------------------------------------------------------------
//--------------------------------------------------------------


/**
 *
 * @param {visRoot} visRoot
 * @param plotId
 */
export function plotInActiveGroup(visRoot, plotId) {
    if (!get(visRoot, 'activePlotId')) return false;
    const pv= getPlotViewById(visRoot, plotId);
    const activePv= getPlotViewById(visRoot, visRoot.activePlotId);
    return (pv.plotGroupId===activePv.plotGroupId);
}

/**
 * make an array of plot starts from the primary plot and all its image overlays
 * @param pv
 * @return {Array}
 */
export function getPlotStateAry(pv) {
    const overlayStates= pv.overlayPlotViews.map( (opv) => get(opv.plot,'plotState')).filter( (s) => s);
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


/**
 * Check it two plot ids are in the same group
 * @param {VisRoot | PlotView[] } ref this can be the visRoot object or a plotViewAry array.
 * @param {string} plotId1 first plotId
 * @param {string} plotId2 second plotId
 * @return {boolean} true if in same group`
 */
export function isInSameGroup(ref, plotId1, plotId2) {
    const pv1= getPlotViewById(ref,plotId1);
    const pv2= getPlotViewById(ref,plotId2);
    return pv1.plotGroupId===pv2.plotGroupId;
}

/**
 * find the plot group from the array
 * @param {string} plotGroupId
 * @param plotGroupAry
 * @return {*}
 */
export function findPlotGroup(plotGroupId, plotGroupAry) {
    if (!plotGroupId || !plotGroupAry) return null;
    return plotGroupAry.find( (pg) => pg.plotGroupId===plotGroupId);
}

/**
 * based on groupLock return an array which will either contain one plotView or the whole group
 * @param {Array.<PlotView>} plotViewAry
 * @param {string} plotId
 * @param {PlotGroup} plotGroup
 * @param {boolean} forceAllInGroup
 * @return {Array.<PlotView>}
 */
export function getOnePvOrGroup(plotViewAry, plotId,plotGroup, forceAllInGroup= false) {
    const groupLock= hasGroupLock(getPlotViewById(plotViewAry,plotId),plotGroup) || forceAllInGroup;
    return groupLock ?
        plotViewAry.filter( (pv) => pv.plotGroupId===plotGroup.plotGroupId) :
        [getPlotViewById(plotViewAry,plotId)];
}


export const primePlotType= (pv) => get(primePlot(pv), 'type', 'image');


/**
 * Perform an operation on all the PlotViews in a group except the source, get the plotViewAry and group from the store.
 * The operations are only performed if the group is locked.
 * @param visRoot - root of the visualization object in store
 * @param sourcePv
 * @param operationFunc
 * @param ignoreThreeColor
 * @param anyPlotType
 * @return {Array} new plotView array after the operation
 */
export function operateOnOthersInGroup(visRoot,sourcePv,operationFunc, ignoreThreeColor=false, anyPlotType= false) {
    const plotGroup= getPlotGroupById(visRoot,sourcePv.plotGroupId);
    const srcType= primePlotType(sourcePv);
    if (hasGroupLock(sourcePv,plotGroup)) {
        visRoot.plotViewAry.forEach( (pv) => {
            if (ignoreThreeColor && isThreeColor(primePlot(pv))) return;
            if (pv.plotGroupId===sourcePv.plotGroupId && pv.plotId!==sourcePv.plotId &&
                (primePlotType(pv)===srcType || anyPlotType))  {
                operationFunc(pv);
            }
        });
    }
}



/**
 * Perform an operation on all the PlotViews in a group except the source a return a new version of the plotViewAry
 * The operations are only performed if the group is locked.
 * Typically used inside of reducer
 * @param sourcePv
 * @param plotViewAry
 * @param plotGroup
 * @param {boolean} matchAnyType matching across plot types ie, would return a hips or an image
 * @param {Function} operationFunc the function to operate on the other plot views
 * @param {PlotView} operationFunc.param pv the PlotView to operate on
 * @return {Array.<PlotView>} new plotView array after the operation
 */
export function matchPlotView(sourcePv,plotViewAry,plotGroup,matchAnyType, operationFunc) {
    const srcType= primePlotType(sourcePv);
    if (hasGroupLock(sourcePv,plotGroup)) {
        plotViewAry= plotViewAry.map( (pv) => {
            return (pv.plotGroupId===sourcePv.plotGroupId && pv.plotId!==sourcePv.plotId &&
                    (primePlotType(pv)===srcType || matchAnyType)) ?
                operationFunc(pv) : pv;
        });
    }
    return plotViewAry;
}


/**
 * perform an operation on a plotView or its related group depending on the lock state.
 * Typically used inside of reducer
 * @param plotViewAry plotViewAry
 * @param plotId the that is primary.
 * @param plotGroup the group to check against
 * @param {boolean} matchAnyType matching across plot types ie, would return a hips or an image
 * @param {Function} operationFunc the function to operate on the other plot views
 * @param {PlotView} operationFunc.param pv the PlotView to operate on
 * @return {Array.<PlotView>} new plotViewAry
 */

export function applyToOnePvOrGroup(plotViewAry, plotId,plotGroup,matchAnyType, operationFunc) {
    const sourcePv= getPlotViewById(plotViewAry,plotId);
    if (!sourcePv) return;
    const srcType= primePlotType(sourcePv);
    const groupLock= hasGroupLock(sourcePv,plotGroup);
    return plotViewAry.map( (pv) => {
        if (pv.plotId===plotId) {
            return operationFunc(pv);
        }
        else if (groupLock && pv.plotGroupId===plotGroup.plotGroupId && (primePlotType(pv)===srcType || matchAnyType) ) {
            return operationFunc(pv);
        }
        else {
            return pv;
        }
    });
}


/**
 * First fine the PlotView with the plotId, then clone the PlotView with the changes specified in the object.
 * Then return a new PlotView array with the changes.
 * @param {PlotView[]|VisRoot} ref visRoot or plotViewAry
 * @param {string} plotId
 * @param {Object} obj fields to replace
 * @return {Array.<PlotView>}
 */
export function clonePvAry(ref, plotId, obj) {
    const plotViewAry= getPlotViewAry(ref);
    if (!plotViewAry) return null;

    return plotViewAry.map( (pv) => pv.plotId===plotId ? clone(pv,obj) : pv);
}

/**
 * make a new copy of the plotview array with the passed plotView replacing the old one
 * @param ref visRoot or plotViewAry
 * @param {PlotView} plotView
 * @return {Array.<PlotView>}
 */
export function clonePvAryWithPv(ref, plotView) {
    const plotViewAry= getPlotViewAry(ref);
    if (!plotViewAry) return null;
    return plotViewAry.map( (pv) => pv.plotId===plotView.plotId ? plotView : pv);
}

/**
 * Find a plot by id in a PlotView
 * @param {PlotView} plotView
 * @param {string} imagePlotId
 * @return {WebPlot}
 */
export function findPlot(plotView, imagePlotId) {
    if (!plotView) return null;
    return plotView.plots.find( (p) => imagePlotId===p.imagePlotId);
}


/**
 *
 * @param {PlotView} pv plot view
 * @return {boolean}
 */
export function isMultiImageFitsWithSameArea(pv) {
    if (!pv.plotViewCtx.containsMultiImageFits) return false;
    const plot= primePlot(pv);
    const {dataWidth:w, dataHeight:h} = plot;

    const ic1= makeImagePt(0,0);
    const ic2= makeImagePt(w,0);
    const ic3= makeImagePt(0,h);
    const ic4= makeImagePt(w,h);

    const projName= plot.projection.getProjectionName();
    const cc= CysConverter.make(plot);

    const c1= cc.getWorldCoords(ic1);
    const c2= cc.getWorldCoords(ic2);
    const c3= cc.getWorldCoords(ic3);
    const c4= cc.getWorldCoords(ic4);
    if (!c1 || !c2 || !c3 || !c4) return false;

    return pv.plots.every( (p) => {
        if (w!==p.dataWidth || h!==p.dataHeight) return false;
        if (projName!==p.projection.getProjectionName()) return false;

        const pCC= CysConverter.make(p);
        const iwc1= pCC.getWorldCoords(ic1);
        const iwc2= pCC.getWorldCoords(ic2);
        const iwc3= pCC.getWorldCoords(ic3);
        const iwc4= pCC.getWorldCoords(ic4);
        if (!iwc1 || !iwc2 || !iwc3 || !iwc4) return false;

        return (pointEquals(iwc1,c1) && 
                pointEquals(iwc2,c2) && 
                pointEquals(iwc3,c3) && 
                pointEquals(iwc4,c4) );
    });
}

/**
 * get the WorldPt that is at the center of the plots projection
 * @param plot
 * @return {WorldPt}
 */
export function getCenterOfProjection(plot) {
    return plot && makeWorldPt(plot.projection.header.crval1, plot.projection.header.crval2,  plot.imageCoordSys);
}

/**
 *
 * @param {Array.<PvNewPlotInfo>} pvNewPlotInfoAry
 * @return {Array.<String>}
 */
export function getPvNewPlotIdAry(pvNewPlotInfoAry) {
    return pvNewPlotInfoAry.map( (npi) => npi.plotId);
}

/**
 *
 * @param {Array.<PvNewPlotInfo>} pvNewPlotInfoAry
 * @param {String} plotId - plotId to test
 * @return {boolean}
 */
export function isPlotIdInPvNewPlotInfoAry(pvNewPlotInfoAry, plotId) {
    return pvNewPlotInfoAry.some( (npi) => npi.plotId===plotId);
}


/**
 *
 * @param {Canvas} c
 * @param {number} start
 * @param {String} plotId
 * @return {number}
 */
const getCanvasIdx= (c,start,plotId) => Number(c.id.substr(0, c.id.length - plotId.length - 1).substr(start));


/**
 *
 * @param plotId
 * @return {Array.<canvas>}
 */
export function getAllCanvasLayersForPlot(plotId) {
    const cAry= [...document.getElementsByTagName('canvas')];
    const layers= cAry.filter( (canvas) => canvas.id && canvas.id.endsWith(plotId));

    const imageLayers= layers.filter( (canvas) => canvas.id.startsWith(CANVAS_IMAGE_ID_START))
        .sort( (c1,c2) => {
            const n1= getCanvasIdx(c1,CANVAS_IMAGE_ID_START,plotId);
            const n2= getCanvasIdx(c2,CANVAS_IMAGE_ID_START,plotId);
            return n1<n2;
        });
    const dlLayers= layers.filter( (canvas) => canvas.id.startsWith(CANVAS_DL_ID_START))
        .sort( (c1,c2) => {
            const n1= getCanvasIdx(c1,CANVAS_DL_ID_START,plotId);
            const n2= getCanvasIdx(c2,CANVAS_DL_ID_START,plotId);
            return n1<n2;
        });
    return [...imageLayers,...dlLayers];
}
