/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {difference, flatten, get, has, isArray, isEmpty, isString, isUndefined, isObject,uniq} from 'lodash';
import {getPlotGroupById} from './PlotGroup.js';
import {makeDevicePt, makeImagePt, makeWorldPt, pointEquals} from './Point.js';
import {clone} from '../util/WebUtil.js';
import {dispatchDestroyDrawLayer, getDlAry} from './DrawLayerCntlr.js';
import {makeTransform} from './PlotTransformUtils.js';
import CysConverter, {CCUtil} from './CsysConverter';
import {computeDistance} from './VisUtil.js';
import {isHiPS, isImage} from './WebPlot.js';
import {isDefined} from '../util/WebUtil';
import {getWavelength, isWLAlgorithmImplemented, PLANE} from './projection/Wavelength.js';
import {getNumberHeader, HdrConst} from './FitsHeaderUtil.js';


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
 * @param {PlotView[]|PlotView|VisRoot|CysConverter|WebPlot} ref this can be the visRoot or the plotViewAry, or a plotView Object.
 * @return {boolean} true if there is a projection
 */
export function hasWCSProjection(ref) {
    if (!ref) return false;
    let projection;
    if (ref.projection) {
        projection= ref.projection;
    }
    else  {
        const p= primePlot(ref);
        if (!p) return false;
        projection= p.projection;
    }
    return projection.isSpecified() && projection.isImplemented();
}

/**
 * Return an array of plotId's that are in the plot group associated with the the pvOrId parameter.
 * @param {VisRoot} visRoot - root of the visualization object in store
 * @param pvOrId this parameter will take the plotId string or a plotView object
 * @param onlyIfPositionLocked
 * @param hasPlots
 * @returns {Array.<PlotView>}
 */
export function getPlotViewIdList(visRoot, pvOrId, onlyIfPositionLocked=true, hasPlots=false) {
    if (!pvOrId) return [];
    const pv= (typeof pvOrId ==='string') ? getPlotViewById(visRoot,pvOrId) : pvOrId;
    const locked= visRoot.positionLock;
    if (!locked && onlyIfPositionLocked) return [pv.plotId];
    const idList=  visRoot.plotViewAry.map( (pv) => pv.plotId);
    if (!hasPlots) return idList;

    return idList.filter( (id) => get(getPlotViewById(visRoot,id),'plots.length') );
}

/**
 * Return an array of plotId's that are in the plot group associated with the the pvOrId parameter.
 * @param {VisRoot} visRoot - root of the visualization object in store
 * @param pvOrId this parameter will take the plotId string or a plotView object
 * @param onlyIfGroupLocked
 * @param hasPlots
 * @returns {Array.<PlotView>}
 */
export function getPlotViewIdListInOverlayGroup(visRoot,pvOrId,onlyIfGroupLocked=true, hasPlots=false) {
    if (!pvOrId) return [];
    const pv= (typeof pvOrId ==='string') ? getPlotViewById(visRoot,pvOrId) : pvOrId;
    const gid= pv.plotGroupId;
    const group= getPlotGroupById(visRoot,gid);
    const locked= hasOverlayColorLock(pv,group);
    if (!locked && onlyIfGroupLocked) return [pv.plotId];
    const idList=  visRoot.plotViewAry.filter( (pv) => pv.plotGroupId===gid).map( (pv) => pv.plotId);
    if (!hasPlots) return idList;

    return idList.filter( (id) => get(getPlotViewById(visRoot,id),'plots.length') );
}

/**
 * return an array of plotIds that are all under visRoot and based on the overlay/color lock of the group associated
 * width the pvOrId parameter
 * @param visRoot
 * @param pvOrId
 * @param hasPlots
 * @param plotTypeMustMatch
 * @returns {Array.<string>}  plotId Array
 */
export function getAllPlotViewIdByOverlayLock(visRoot, pvOrId, hasPlots=false, plotTypeMustMatch) {
    if (!pvOrId) return [];
    const majorPv= (typeof pvOrId ==='string') ? getPlotViewById(visRoot,pvOrId) : pvOrId;
    const gid= majorPv.plotGroupId;
    const group= getPlotGroupById(visRoot,gid);
    const locked= hasOverlayColorLock(majorPv,group);

    if (!locked) {
        return [majorPv.plotId];
    } else {
        return visRoot.plotViewAry.filter((pv) => !hasPlots || get(pv, 'plots.length'))
                                  .filter((pv) => !plotTypeMustMatch || isImage(primePlot(pv))===isImage(primePlot(majorPv)))
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
        obj.viewDim && obj.overlayPlotViews && obj.plotViewCtx);
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
export function getLayerTitle(plotId,dl) {
    if (!dl.autoFormatTitle) return dl.title;
    return isObject(dl.title) ? dl.title[plotId] : dl.title;
}


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
// export function hasGroupLock(pv,plotGroup) {
//     return Boolean(plotGroup && plotGroup.plotGroupId && plotGroup.overlayColorLock &&
//                     pv && pv.plotGroupId===plotGroup.plotGroupId);
// }

export function hasOverlayColorLock(pv,plotGroup) {
    return Boolean(plotGroup && plotGroup.plotGroupId && plotGroup.overlayColorLock &&
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
    const groupLock= hasOverlayColorLock(getPlotViewById(plotViewAry,plotId),plotGroup) || forceAllInGroup;
    return groupLock ?
        plotViewAry.filter( (pv) => pv.plotGroupId===plotGroup.plotGroupId) :
        [getPlotViewById(plotViewAry,plotId)];
}


export const primePlotType= (pv) => get(primePlot(pv), 'plotType', 'image');


/**
 * Perform an operation on all the PlotViews in a group except the source, get the plotViewAry and group from the store.
 * The operations are only performed if the group is locked.
 * @param {VisRoot} visRoot - root of the visualization object in store
 * @param {PlotView} sourcePv
 * @param {Function} operationFunc
 * @param {boolean} ignoreThreeColor
 * @param {boolean} anyPlotType
 * @return {Array} new plotView array after the operation
 */
export function operateOnOthersInOverlayColorGroup(visRoot,sourcePv,operationFunc, ignoreThreeColor=false, anyPlotType= false) {
    const plotGroup= getPlotGroupById(visRoot,sourcePv.plotGroupId);
    const srcType= primePlotType(sourcePv);
    if (hasOverlayColorLock(sourcePv,plotGroup)) {
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
 *
 * @param {VisRoot} visRoot
 * @param {PlotView} sourcePv
 * @param {Function} operationFunc
 * @param {boolean} ignoreThreeColor
 * @param {boolean} anyPlotType
 */
export function operateOnOthersInPositionGroup(visRoot,sourcePv,operationFunc, ignoreThreeColor=false, anyPlotType= false) {
    const srcType= primePlotType(sourcePv);
    if (visRoot.positionLock) {
        visRoot.plotViewAry.forEach( (pv) => {
            if (ignoreThreeColor && isThreeColor(primePlot(pv))) return;
            if (pv.plotId!==sourcePv.plotId && (primePlotType(pv)===srcType || anyPlotType))  {
                operationFunc(pv);
            }
        });
    }
}



/**
 * Perform an operation on all the PlotViews in a group except the source a return a new version of the plotViewAry
 * The operations are only performed if the group is locked.
 * Typically used inside of reducer
 * @param {VisRoot} vr
 * @param sourcePv
 * @param plotViewAry
 * @param {boolean} matchAnyType matching across plot types ie, would return a hips or an image
 * @param {Function} operationFunc the function to operate on the other plot views
 * @param {PlotView} operationFunc.param pv the PlotView to operate on
 * @return {Array.<PlotView>} new plotView array after the operation
 */
export function matchPlotViewByPositionGroup(vr, sourcePv, plotViewAry, matchAnyType, operationFunc) {
    const srcType= primePlotType(sourcePv);
    if (vr.positionLock) {
        plotViewAry= plotViewAry.map( (pv) => {
            return (pv.plotId!==sourcePv.plotId && (primePlotType(pv)===srcType || matchAnyType)) ?
                operationFunc(pv) : pv;
        });
    }
    return plotViewAry;
}


/**
 * perform an operation on a plotView or its related group depending on the lock state.
 * Typically used inside of reducer
 * @param {boolean} toAll
 * @param {Array.<PlotView>} plotViewAry plotViewAry
 * @param {string} plotId the that is primary.
 * @param {boolean} matchAnyType matching across plot types ie, would return a hips or an image
 * @param {Function} operationFunc the function to operate on the other plot views
 * @param {PlotView} operationFunc.param pv the PlotView to operate on
 * @return {Array.<PlotView>} new plotViewAry
 */

export function applyToOnePvOrAll(toAll, plotViewAry, plotId, matchAnyType, operationFunc) {
    const sourcePv= getPlotViewById(plotViewAry,plotId);
    if (!sourcePv) return;
    const srcType= primePlotType(sourcePv);
    return plotViewAry.map( (pv) => {
        if (pv.plotId===plotId) return operationFunc(pv);
        else if (toAll && (primePlotType(pv)===srcType || matchAnyType) ) return operationFunc(pv);
        else return pv;
    });
}

export function applyToOnePvOrOverlayGroup(plotViewAry, plotId,plotGroup,matchAnyType, operationFunc) {
    const sourcePv= getPlotViewById(plotViewAry,plotId);
    if (!sourcePv) return;
    const srcType= primePlotType(sourcePv);
    const groupLock= plotGroup && hasOverlayColorLock(sourcePv,plotGroup);
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
 * First find the PlotView with the plotId, then clone the PlotView with the changes specified in the object.
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
 * @param {string} plotImageId
 * @return {WebPlot}
 */
export function findPlot(plotView, plotImageId) {
    if (!plotView) return undefined;
    return plotView.plots.find( (p) => plotImageId===p.plotImageId);
}


/**
 *
 * @param {PlotView} pv plot view
 * @return {boolean}
 */
export function isMultiImageFitsWithSameArea(pv) {
    if (!isMultiImageFits(pv)) return false;
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
 * Find the point in the plot that is at the center of the display.
 * The point returned is in ImagePt coordinates.
 * We return it in and ImagePt not screen because if the plot is zoomed the image point will still
 * be what we want in the center. A alternate scrollX and scrollY may be passed otherwise the current
 * scrollX and scrollY is used.
 *
 * @param {PlotView} plotView
 * @param {number} [scrollX] optional scrollX, if not defined use plotView.scrollX
 * @param {number} [scrollY] optional scrollY, if not defined use plotView.scrollY
 * @return {ImagePt} the center point
 */
export function findCurrentCenterPoint(plotView,scrollX,scrollY) {
    const plot= primePlot(plotView);
    if (!plot) return null;
    const {viewDim}= plotView;

    let cc;
    if (!isUndefined(scrollX) && !isUndefined(scrollY)) { //case scrollX && scrollY are passed
        const trans= makeTransform(0,0, scrollX, scrollY,  plotView.rotation, plotView.flipX, plotView.flipY, viewDim);
        cc= CysConverter.make(plot,trans);
    }
    else if (isUndefined(plotView.scrollX) || isUndefined(plotView.scrollY)) { //case scrollX && scrollY not pass and that are not defined in plotview, use 0,0
        const trans= makeTransform(0,0, 0, 0,  plotView.rotation, plotView.flipX, plotView.flipY, viewDim);
        cc= CysConverter.make(plot,trans);
    }
    else { // case scrollX && scrollY not passed and we get then  from plotView
        cc= CysConverter.make(plot);
    }
    return cc.getImageCoords(makeDevicePt(viewDim.width/2, viewDim.height/2));
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

export function getCorners(plot) {
    if (!plot) return null;
    const cc= CysConverter.make(plot);
    const {dataWidth:w, dataHeight:h} = plot;

    const c1= cc.getWorldCoords(makeImagePt(0,0));
    const c2= cc.getWorldCoords(makeImagePt(w,0));
    const c3= cc.getWorldCoords(makeImagePt(w,h));
    const c4= cc.getWorldCoords(makeImagePt(0,h));
    if (!c1 || !c2 || !c3 || !c4) return null;
    return [c1,c2,c3,c4];
}


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

/**
 *
 * @param {PlotView} pv
 * @param {number} [alternateZoomFactor]
 * @return {number} fov in degrees
 */
export function getFoV(pv, alternateZoomFactor) {
    const plot= primePlot(pv);
    const cc = CysConverter.make(plot);
    const {width, height} = pv.viewDim;
    const allSkyImage= Boolean(isImage(plot) && plot.projection.isWrappingProjection());
    if (!cc || !width || !height) return;
    if (alternateZoomFactor) cc.zoomFactor= alternateZoomFactor;
    const pt1 = cc.getWorldCoords(makeDevicePt(1, height / 2));
    const pt2 = (allSkyImage) ? cc.getWorldCoords(makeDevicePt(width/2, height / 2))  :
                                cc.getWorldCoords(makeDevicePt(width - 1, height / 2));

    const dist= (pt1 && pt2) && computeDistance(pt1, pt2);
    if (dist) return allSkyImage ? dist*2 : dist;

    if (isHiPS(plot)) {
        return 180; // todo: this may need to consider the projection type in to future, ie aitoff 360
    }
    else if (allSkyImage) {
        return 360;
    }
    else { // not allsky image, but computation is outside of projection, use an alternate approach
        const ip1=  cc.getWorldCoords(makeImagePt(0, 0));
        const ip2=  cc.getWorldCoords(makeImagePt(plot.dataWidth, 0));
        const idist= (ip1 && ip2) && computeDistance(ip1, ip2);
        if (!idist) return false;
        return (pv.viewDim.width/plot.screenSize.width) * idist;
    }
}



// =============================================================
// =============================================================
// ------------  Cube and Multi Image FITS functions -----------
// =============================================================
// =============================================================

/**
 * Find if there are cubes or images
 * @param {PlotView} pv
 * @return {boolean} true if there are cubes or images
 */
export function isMultiImageFits(pv) {
    return Boolean(isMultiHDUFits(pv) || getNumberOfCubesInPV(pv)>0);
}

/**
 * Does the image file have more than one HDU
 * @param {PlotView} pv
 * @return {boolean} true if this file has more than one HDU
 */
export function isMultiHDUFits(pv) {
    if (!pv) return false;
    const hduCnt= uniq(pv.plots.map( (p) => getNumberHeader(p,HdrConst.SPOT_EXT,0)));
    return hduCnt.length>1;
}

/**
 * @param {PlotView} pv
 * @return {Array.<number>|boolean}
 */
export function getHduPlotStartIndexes(pv) {
    if (!pv) return false;
    if (!isMultiHDUFits(pv)) return [0];
    return pv.plots
        .map( (p,idx) => idx)
        .filter( (idx) => getImageCubeIdx(pv.plots[idx])<1);
}

/**
 * @param {PlotView} pv
 * @return {Array.<number>|boolean}
 */
export const getHDUCount= (pv) => getHduPlotStartIndexes(pv).length;


/**
 * Count the number of cubes
 * @param {PlotView} pv
 * @return {number} the number of cubes, 0 if none
 */
export function getNumberOfCubesInPV(pv) {
    if (!pv || !isImage(primePlot(pv)) ) return 0;
    return pv.plots.reduce( (total, p, idx) => {
        if (idx===0) return getImageCubeIdx(p)>=0 ? 1 : 0;
        return ( getHDU(p)!==getHDU(pv.plots[idx-1]) && getImageCubeIdx(p)>-1) ? total+1 : total;
    }, 0);
}

/**
 * Get the total number of planes in the cube of the plot
 * @param {PlotView} pv
 * @param {WebPlot} [plot] the plot to check, defaults to primaryPlot
 * @return {number}
 */
export function getCubePlaneCnt(pv, plot) {
    if (!plot) plot= primePlot(pv);
    if (!isImageCube(plot)) return 0;
    const hdu= getHDU(plot);
    return pv.plots.filter( (p) => getHDU(p)===hdu).length;
}


/**
 * Get the HDU of this primaryPlot in the FITS file
 * @param {PlotView} pv
 * @return {number} the HDU number, single images will always return 0
 */
export function getPrimaryPlotHdu(pv) {
    if (!pv) return 0;
    const p= primePlot(pv);
    if (!isImage(p)) return 0;
    return p ? getHDU(p) : 0;
}

/**
 * Get the HDU of this image in the FITS file, this is not the index of the HDU of load images since there
 * might be tables in between images
 * @param {WebPlot} plot
 * @return {number} the HDU number, single images will always return 0
 */
export const getHDU= (plot) => getNumberHeader(plot,HdrConst.SPOT_EXT,0);

/**
 *
 * @param {PlotView} pv
 * @param {WebPlot} plot
 * @return {number}
 */
export function getHDUIndex(pv, plot= undefined) {
    if (!pv) return 0;
    const p= plot ||  primePlot(pv);
    const plotIdx= pv.plots.findIndex( (testP) => testP===p);
    const startIndexes= getHduPlotStartIndexes(pv);
    const hduIdx= startIndexes.findIndex( (value,arrayIdx) => value<=plotIdx && (arrayIdx===startIndexes.length-1 || startIndexes[arrayIdx+1]>plotIdx ));
    return hduIdx;
}

/**
 * Find if there are image cube this this plotview
 * @param {PlotView} pv
 * @return {boolean} true if there are any image cube in the PlotView
 */
export const hasImageCubes = (pv) => getNumberOfCubesInPV(pv)>0;

/**
 * get the plane index of this plot in cube
 * @param {WebPlot} plot
 * @return {number} the plane index, -1 if not in a cube
 */
export const getImageCubeIdx = (plot) => (plot && plot.cubeCtx) ? plot.cubeCtx.cubePlane : -1;


/**
 * plot is plane in a image cube
 * @param {WebPlot} plot
 * @return {boolean} true if plot is a plane in a cube, otherwise false
 */
export const isImageCube = (plot) => getImageCubeIdx(plot) > -1;

/**
 * Given a HDU index and optionally a cube index, return the image idx
 * @param {PlotView} pv
 * @param {number} hduIdx
 * @param {number|'follow'} cubeIdx
 * @return {number}
 */
export function convertHDUIdxToImageIdx(pv, hduIdx, cubeIdx=0) {
    if (!pv || !isPlotView(pv)) return;
    if (!isMultiImageFits(pv)) return 0;
    const plot= primePlot(pv);
    if (cubeIdx==='follow' && isImageCube(plot)) {
        const idx= pv.plots.findIndex((p)=> p===plot);
        cubeIdx= convertImageIdxToHDU(pv,idx).cubeIdx;
    }
    const startIndexes= getHduPlotStartIndexes(pv);
    if (hduIdx>startIndexes.length-1)return 0;
    const cnt= getCubePlaneCnt(pv, pv.plots[startIndexes[hduIdx]]);
    return (isImageCube(pv.plots[startIndexes[hduIdx]]) && cubeIdx<cnt) ? startIndexes[hduIdx]+cubeIdx : startIndexes[hduIdx];
}

/**
 * Give a image index return the hduIndx and optionally the cubeIdx
 * @param {PlotView} pv
 * @param {number} imageIdx
 * return {{hduIdx:number, cubeIdx:number, isCube:boolean}}
 */
export function convertImageIdxToHDU(pv, imageIdx) {
    if (!pv || !isPlotView(pv)) return {hduIdx:undefined, cubeIdx:undefined, isCube:false};
    if (!isMultiImageFits(pv) || imageIdx>pv.plots.length-1) return {hduIdx:0, cubeIdx:undefined, isCube:false};
    const isCube=  isImageCube(pv.plots[imageIdx]);
    const startIndexes= getHduPlotStartIndexes(pv);
     // startIndexes.findIndex( (i) => (i>=imageIdx) );
    const hduIdx=getHDUIndex(pv,pv.plots[imageIdx]);
    return {hduIdx, cubeIdx:isCube && imageIdx-startIndexes[hduIdx], isCube};
}





//=============================================================
//=============================================================
//---------- Wavelength functions
//=============================================================
//=============================================================

/**
 * check to see if wavelength data is available
 * Only CTYPEka = 'WAVE-ccc', exists, the hasWLInfo is true.
 * If the wlType is not defined, it is a pure cube data
 * @param {WebPlot} plot
 * @return {boolean}
 */
export function hasWLInfo(plot) {
    if (!plot) return false;
    return Boolean(plot.wlData && isDefined(plot.wlData.wlType) && isWLAlgorithmImplemented(plot.wlData) );
}

export function wavelengthInfoParsedSuccessfully(plot) {
    return hasWLInfo(plot) && !Boolean(plot.wlData.failReason);
}

export function getWavelengthParseFailReason(plot) {
    return hasWLInfo(plot) && plot.wlData.failReason;
}

/**
 * check to see if wavelength data is available as the plain level (not pixel level) only
 * @param {WebPlot} plot
 * @return {boolean}
 */
export const hasPlaneOnlyWLInfo= (plot) => hasWLInfo(plot) && plot.wlData.algorithm===PLANE;

export const hasPixelLevelWLInfo= (plot) => hasWLInfo(plot) && plot.wlData.algorithm!==PLANE;

/**
 * Return the units string
 * @param plot
 * @return {string}
 */
export function getWaveLengthUnits(plot) {
    if (!plot || !hasWLInfo(plot)) return '';
    return get(plot, 'wlData.units','');
}


const MICRON_SYMBOL= String.fromCharCode(0x03BC)+'m';

/**
 *
 * @param {WebPlot|String} plotOrStr - pass a webplot to get the units from and the format or a string that will be formatted
 * @param {boolean} anyPartOfStr - replace units with symbols in any part of a longer string
 * @return {string}
 */
export function getFormattedWaveLengthUnits(plotOrStr, anyPartOfStr=false) {
    const uStr= isString(plotOrStr) ? plotOrStr : getWaveLengthUnits(plotOrStr);
    if (anyPartOfStr) {
        return uStr.replace(new RegExp('microns|micron|um|micrometers','gi'),MICRON_SYMBOL);
    }
    else {
        const u= uStr.toLowerCase();
        return (u.startsWith('micron') || u==='um' || u==='micrometers') ? MICRON_SYMBOL : uStr;
    }
}

/**
 *
 * @param {WebPlot} plot
 * @param {Point} pt
 * @param {number} cubeIdx
 * @return {number}
 */
export const getPtWavelength= (plot, pt, cubeIdx) =>
          hasWLInfo(plot) && getWavelength(CCUtil.getImageCoords(plot,pt),cubeIdx,plot.wlData);


