/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {flatten, has, isArray, isEmpty, isObject, isString, isUndefined, uniq} from 'lodash';
import shallowequal from 'shallowequal';
import {getPlotGroupById} from './PlotGroup.js';
import {makeDevicePt, makeImagePt, makeWorldPt, pointEquals} from './Point.js';
import {makeTransform} from './PlotTransformUtils.js';
import CysConverter, {CCUtil} from './CsysConverter';
import {isHiPS, isImage} from './WebPlot.js';
import {isDefined, memorizeLastCall} from '../util/WebUtil';
import {getWavelength, isWLAlgorithmImplemented, getVrad, isVRADAlgorithmImplemented, PLANE} from './projection/Wavelength.js';
import {getNumberHeader, HdrConst} from './FitsHeaderUtil.js';
import {computeDistance, getRotationAngle, isCsysDirMatching, isEastLeftOfNorth, isPlotNorth} from './VisUtil';
import {removeRawData} from './rawData/RawDataCache.js';
import {MAX_DIRECT_IMAGE_SIZE, MAX_RAW_IMAGE_SIZE} from './rawData/RawDataCommon.js';
import {hasClearedDataInStore, hasLocalRawDataInStore, hasLocalStretchByteDataInStore} from './rawData/RawDataOps.js';


export const CANVAS_IMAGE_ID_START= 'image-';
export const CANVAS_DL_ID_START= 'dl-';
/**
 * 
 * @param {VisRoot | PlotView[]} ref
 * @returns {PlotView[]}
 */
export function getPlotViewAry(ref) {
    if (!ref) return undefined;
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
 * last call results are cached, for optimizations, after 2000 calls cached calls are about 3 times faster.
 *
 * @param {PlotView[]|PlotView|VisRoot} ref this can be the visRoot or the plotViewAry, or a plotView Object.
 * @param {string} [plotId] the plotId, required with plotViewAry, ignored for  plotView, optional with visRoot
 * @returns {WebPlot|undefined} the plot, or undefined if it is not found
 */
export const primePlot= memorizeLastCall( (ref,plotId) => {
    let pv;
    if (!ref) return;
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
    return (pv && pv.primeIdx>=0) ? pv.plots[pv.primeIdx] : undefined;
},4);



/**
 * get the PlotView by id
 * last call results are cached, for optimizations, after 2000 calls cached calls are about 2 times faster.
 * cache is hit about 1.5 times for every non-cache all
 *
 * @param {VisRoot | PlotView[] } ref this can be the visRoot object or a plotViewAry array.
 * @param {string} plotId
 *
 * @returns {PlotView|undefined} the plot view object, or undefined if it is not found
 */
export const getPlotViewById= memorizeLastCall( (ref,plotId) =>{
    if (!plotId) return undefined;
    const plotViewAry= getPlotViewAry(ref);
    if (!plotViewAry) return undefined;
    return plotViewAry.find( (pv) => pv.plotId===plotId);
},4);

export const getPlotViewIdxById= (ref,plotId) =>
                    plotId && getPlotViewAry(ref)?.findIndex( (pv) => pv.plotId===plotId);

export function getPlotGroupIdxById(ref,plotGroupId) {
    const plotGroupAry= ref?.plotGroupAry ? ref.plotGroupAry : ref;
    return plotGroupAry?.findIndex( (pg) => pg.plotGroupId===plotGroupId);
}


/**
 * @param {PlotView[]|PlotView|VisRoot|CysConverter|WebPlot} ref this can be the visRoot or the plotViewAry, or a plotView Object.
 * @return {boolean} true if there is a projection
 */
export function hasWCSProjection(ref) {
    if (!ref) return false;
    const projection= ref.projection ?? primePlot(ref)?.projection;
    return Boolean(projection?.isSpecified() && projection?.isImplemented());
}

/**
 * Return an array of plotId's that are in the plot group associated with the the pvOrId parameter.
 * @param {VisRoot} visRoot - root of the visualization object in store
 * @param pvOrId this parameter will take the plotId string or a plotView object
 * @returns {Array.<String>}
 */
export function getPlotViewIdListByPositionLock(visRoot, pvOrId) {
    if (!pvOrId) return [];
    const pv= isString(pvOrId) ? getPlotViewById(visRoot,pvOrId) : pvOrId;
    if (!visRoot.positionLock) return [pv.plotId];
    return visRoot.plotViewAry
        .map( (pv) => pv.plotId)
        .filter( (id) => getPlotViewById(visRoot,id)?.plots?.length );
}
/**
 * Return an array of plotId's that are in the plot group associated with the the pvOrId parameter.
 * @param {VisRoot} visRoot - root of the visualization object in store
 * @param pvOrId this parameter will take the plotId string or a plotView objectgs
 * @returns {Array.<String>}
 */
export function getPlotViewIdListInOverlayGroup(visRoot,pvOrId) {
    if (!pvOrId) return [];
    const pv= (typeof pvOrId ==='string') ? getPlotViewById(visRoot,pvOrId) : pvOrId;
    if (!pv) return [];
    const gid= pv.plotGroupId;
    const group= getPlotGroupById(visRoot,gid);
    if (!hasOverlayColorLock(pv,group) ) return [pv.plotId];
    return  visRoot.plotViewAry.filter( (pv) => pv.plotGroupId===gid).map( (pv) => pv.plotId);
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
        return visRoot.plotViewAry.filter((pv) => !hasPlots || pv.plots?.length)
            .filter((pv) => !plotTypeMustMatch || isImage(primePlot(pv))===isImage(primePlot(majorPv)))
            .map((pv) => pv.plotId);
    }
}


/**
 * Is this plotview the active one
 * @param {VisRoot} visRoot - root of the visualization object in store
 * @param {string} plotId
 * @returns {boolean} is active, there will be only one active at a time
 */
export const isActivePlotView= (visRoot,plotId) => visRoot.activePlotId===plotId;

/**
 * Get the active PlotView from the store
 * @param {VisRoot} visRoot - root of the visualization object in store
 * @return {PlotView} the active plot view
 */
export const getActivePlotView= (visRoot) => visRoot?.plotViewAry.find( (pv) => pv.plotId===visRoot.activePlotId);

/**
 *
 * @param {WebPlot|PlotView} plotOrPv plot or plotView object.  if a PlotView then it test the primePlot of the plotView
 * @return {boolean} true if three color false if not or plot is null
 */
export function isThreeColor(plotOrPv) {
    if (!plotOrPv) return false;
    const plot= isPlotView(plotOrPv) ? primePlot(plotOrPv) : plotOrPv;
    return Boolean(plot?.plotState.isThreeColor());
}

/**
 * Return true if this is a PlotView object
 * @param obj
 * @return boolean
 */
export const isPlotView= (obj) =>
           isObject(obj) && Boolean(obj.plots && obj.plotId && obj.viewDim && obj.overlayPlotViews && obj.plotViewCtx);


/**
 *
 * @param {PlotView} pv
 * @param {String} imageOverlayId
 * @return {OverlayPlotView|undefined}
 */
export const getOverlayById= (pv, imageOverlayId) =>
                       pv?.overlayPlotViews.find( (opv) => opv.imageOverlayId===imageOverlayId);

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
export const getOverlayByPvAndId = (ref,plotId,imageOverlayId) =>
                                   getOverlayById(getPlotViewById(ref,plotId),imageOverlayId);



export const removeRawDataByPlotView= (pv) => pv?.plots.forEach( (p) => removeRawData(p.plotImageId));

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
 * @param {Array.<DrawLayer>|DrawLayerRoot} ref - the root of the drawing layer controller or the master array of all drawing layers
 * @param id draw layer id
 * @returns {DrawLayer|undefined} the draw layer or undefined if not found
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


//--------------------------------------------------------------
//--------------------------------------------------------------
//--------- general functions
//--------------------------------------------------------------
//--------------------------------------------------------------


/**
 *
 * @param {VisRoot} visRoot
 * @param plotId
 */
export function plotInActiveGroup(visRoot, plotId) {
    if (!visRoot?.activePlotId) return false;
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
    const overlayStates= pv.overlayPlotViews.map( (opv) => opv.plot?.plotState).filter( (s) => s);
    const p= primePlot(pv);
    const pvStateAry= p ? [p.plotState] : [];
    return [...pvStateAry, ...overlayStates];
}


export function hasOverlayColorLock(pv,plotGroup) {
    return Boolean(plotGroup && plotGroup.plotGroupId && plotGroup.overlayColorLock &&
        pv && pv.plotGroupId===plotGroup.plotGroupId);
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


export const primePlotType= (pv) => primePlot(pv)?.plotType ?? 'image';


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
 * @return {Array.<PlotView>|undefined} new plotViewAry if plotId has no PlotView
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
export const clonePvAry= (ref, plotId, obj) =>
              getPlotViewAry(ref)?.map( (pv) => pv.plotId===plotId ? {...pv,...obj} : pv);

/**
 * make a new copy of the plotview array with the passed plotView replacing the old one
 * @param ref visRoot or plotViewAry
 * @param {PlotView} plotView
 * @return {Array.<PlotView>}
 */
export const clonePvAryWithPv= (ref, plotView) =>
              getPlotViewAry(ref)?.map( (pv) => pv.plotId===plotView.plotId ? plotView : pv);

/**
 * Find a plot by id in a PlotView
 * @param {PlotView} plotView
 * @param {string} plotImageId
 * @return {WebPlot}
 */
export const findPlot= (plotView, plotImageId) => plotView?.plots.find( (p) => plotImageId===p.plotImageId);


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
 * @return {ImagePt|undefined} the center point
 */
export function findCurrentCenterPoint(plotView,scrollX,scrollY) {
    const plot= primePlot(plotView);
    if (!plot) return undefined;
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
 * @param {String} plotId - plotId to test
 * @return {boolean}
 */
export function isPlotIdInPvNewPlotInfoAry(pvNewPlotInfoAry, plotId) {
    if (!pvNewPlotInfoAry || !plotId) return false;
    return pvNewPlotInfoAry.some( (npi) => npi.plotId===plotId);
}


/**
 *
 * @param {Canvas} c
 * @param {String} start
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
 * @return {Array.<Canvas>}
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
 * @return {number|boolean} fov in degrees, or false if it can't be computed
 */
export function getFoV(pv, alternateZoomFactor) {
    const plot= primePlot(pv);
    const cc = CysConverter.make(plot);
    const {width, height} = pv.viewDim;
    const allSkyImage= Boolean(isImage(plot) && plot.projection.isWrappingProjection());
    if (!cc || !width || !height) return false;
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
export const isMultiImageFits= (pv) => Boolean(isMultiHDUFits(pv) || getNumberOfCubesInPV(pv)>0);

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
 * @return {Number}
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
export const getImageCubeIdx = (plot) => plot?.cubeCtx?.cubePlane ?? -1;


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
 * @return {number|undefined} the hdu number or undefined if bad parameters
 */
export function convertHDUIdxToImageIdx(pv, hduIdx, cubeIdx=0) {
    if (!pv || !isPlotView(pv)) return undefined;
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
    const hduIdx=getHDUIndex(pv,pv.plots[imageIdx]);
    return {hduIdx, cubeIdx:isCube && imageIdx-startIndexes[hduIdx], isCube};
}




//=============================================================
//=============================================================
//---------- wavelength functions
//---------- in general spectral cord fits functions
//=============================================================
//=============================================================

/**
 * check to see if wavelength/radio velocity/frequency data is available
 * Only CTYPEka = 'WAVE-ccc', exists, the hasWLInfo is true.
 * also check if vrad data is available, when CTYPE# = 'VRAS-xxx', hasVRADInfo is true.
 * If the wlType or vradType is not defined, it is a pure cube data
 * @param {WebPlot} plot
 * @return {boolean}
 */
export const hasWLInfo= (plot) =>
           Boolean(plot && plot.wlData && isDefined(plot.wlData.wlType) && isWLAlgorithmImplemented(plot.wlData) );

export const wavelengthInfoParsedSuccessfully= (plot) => hasWLInfo(plot) && !Boolean(plot.wlData.failReason);

export const getWavelengthParseFailReason= (plot) => hasWLInfo(plot) && plot.wlData.failReason;

/**
 * also check if vrad data is available, when CTYPE# = 'VRAS-xxx', hasVRADInfo is true.
 * @param {WebPlot} plot
 * @returns {boolean}
 */
export const hasVRADInfo= (plot) =>
           Boolean(plot && plot.wlData && isDefined(plot.vradData.vradType) && isVRADAlgorithmImplemented(plot.vradData) );

export const vradInfoParsedSuccessfully= (plot) => hasVRADInfo(plot) && !Boolean(plot.vradData.failReason);

export const getVradParseFailReason= (plot) => hasVRADInfo(plot) && plot.vradData.failReason;

/**
 * check to see if wavelength data is available as the plain level (not pixel level) only
 * @param {WebPlot} plot
 * @return {boolean}
 */
export const hasPlaneOnlyWLInfo= (plot) => hasWLInfo(plot) && plot.wlData.algorithm===PLANE;

export const hasPixelLevelWLInfo= (plot) => hasWLInfo(plot) && plot.wlData.algorithm!==PLANE;

export const hasPlaneOnlyVRADInfo= (plot) => hasVRADInfo(plot) && plot.vradData.algorithm===PLANE;

/**
 * Return the units string
 * @param plot
 * @return {string}
 */
export function getWaveLengthUnits(plot) {
    if (!plot || !hasWLInfo(plot)) return '';
    /*if (plot.header.CTYPE3==='VRAD') {
        return plot.wlData.units= 'km/s';
    }*/
    return plot.wlData?.units ?? '';
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

/**
 *
 * @param {WebPlot} plot
 * @param {Point} pt
 * @param {number} cubeIdx
 * @return {number}
 */
export const getPtVrad= (plot, pt, cubeIdx) =>
          hasVRADInfo(plot) && getVrad(CCUtil.getImageCoords(plot,pt),cubeIdx,plot.vradData);

//=============================================================
//=============================================================
//---------- PlotView Equality functions
//=============================================================
//=============================================================

/**
 * Check if two PlotViews are equals by comparing only certain keys
 * @param {PlotView} pv1
 * @param {PlotView} pv2
 * @param {Array.<String>} pvKeys
 * @param {Array.<String>} plotKeys
 */
export function isPlotViewsEqual(pv1, pv2, pvKeys=[], plotKeys=[]) {
    if (isEmpty(pvKeys)) return pv1===pv2;
    const p1= primePlot(pv1);
    const p2= primePlot(pv2);
    if (Boolean(p1)!==Boolean(p2)) return false;

    if (p1 && p2) {
        if (isImage(p1) !== isImage(p1)) return false;
        if (isEmpty(plotKeys)) {
            if (p1 !== p2) return false;
        } else {
            const plotEqual = plotKeys.every((k) => p1[k] === p2[k]);
            if (!plotEqual) return false;
        }
    }
    return pvKeys.every( (k) => pv1[k]===pv2[k]);
}

export function isPlotViewArysEqual(pvAry1, pvAry2, pvKeys=[], plotKeys=[]) {
    if (isEmpty(pvAry1) && isEmpty(pvAry1)) return true;
    if (isEmpty(pvAry1)!==isEmpty(pvAry1)) return false;
    if (pvAry1.length!==pvAry2.length) return false;
    return pvAry1.every( (pv,idx) => isPlotViewsEqual(pv,pvAry2[idx], pvKeys, plotKeys));
}


/**
 * Return true if the two PlotViews are equals except for scrolling parameters
 * This code is written to be a efficient as possible
 *    - by testing the mostly likely differences first.
 *    - caching the last call
 *    - don't check the fields that are considered immutable in the plotview or plot object
 *    - don't check the fields that are more minor that will only change if a major field changes
 *                - i.e. if the plotId is the same then the plotGroupId and drawingSubGroupId will be the same
 *                - i.e. if plot.plotImageId is the same then dataWidth, dataHeight, some hips parameters will be the same
 *
 *  IMPORTANT - this function should be updated if plotView or WebPlot objects are updated
 *
 * @param {PlotView} pv1
 * @param {PlotView} pv2
 * @return {boolean}
 */
export const pvEqualExScroll= memorizeLastCall((pv1,pv2) => {
    if (pv1 === pv2) return true;
    let result= true;
    if (!pv1 || !pv2 ||
        pv1.plotId !== pv2.plotId ||
        pv1.primeIdx!==pv2.primeIdx &&
        pv1.serverCall!==pv2.serverCall &&
        pv1.plottingStatusMsg !== pv2.plottingStatusMsg) {
        result= false;
    }
    if (result && pv1.plots !== pv2.plots) {
        const p1 = primePlot(pv1);
        const p2 = primePlot(pv2);
        if (p1 !== p2) {
            if (p1.plotImageId !== p2.plotImageId ||  //
                isHiPS(p1) !== isHiPS(p2) ||
                p1.attributes !== p2.attributes ||
                p1.screenSize !== p2.screenSize ||
                p1.zoomFactor !== p2.zoomFactor ||
                p1.imageCoordSys !== p2.imageCoordSys ||
                p1.dataRequested!==p2.dataRequested ||
                p1.blankColor!==p2.blankColor ||
                p1.rawData!==p2.rawData ||
                p1.plotState !== p2.plotState) result= false;
        }
    }
    if (result && (
        pv1.visible!==pv2.visible || pv1.request!==pv2.request ||
        pv1.viewDim!==pv2.viewDim || pv1.menuItemKeys!==pv2.menuItemKeys ||
        pv1.plotViewCtx!==pv2.plotViewCtx || pv1.rotation!==pv2.rotation ||
        pv1.flipY!==pv2.flipY)) result= false;
    if (result && !shallowequal(pv1.overlayPlotViews, pv2.overlayPlotViews)) result= false;
    return result;
},4);



//===============================================================
//----------------       ----------------------------------------
//===============================================================



/**
 * Return true if the plot in both PlotViews are rotated the same
 * @param {PlotView} pv1
 * @param {PlotView} pv2
 * @return {boolean}
 */
export function isRotationMatching(pv1, pv2) {
    const p1 = primePlot(pv1);
    const p2 = primePlot(pv2);

    if (!p1 || !p2) return false;
    if (isNorthCountingRotation(pv1, p1) && isNorthCountingRotation(pv2, p2)) return true;
    const r1 = getRotationAngle(p1) + pv1.rotation;
    const r2 = getRotationAngle(p2) + pv2.rotation;
    return Math.abs((r1 % 360) - (r2 % 360)) < .9;
}

const isNorthCountingRotation = (pv, plot) => pv.plotViewCtx.rotateNorthLock || (isPlotNorth(plot) && !pv.rotation);

/**
 * return an angle that will rotate the pv to match the rotation of masterPv
 * @param {PlotView} masterPv
 * @param {PlotView} pv
 * @return {number}
 */
export function getMatchingRotationAngle(masterPv, pv) {
    const plot = primePlot(pv);
    const masterPlot = primePlot(masterPv);
    if (!plot || !masterPlot) return 0;
    const masterRot = masterPv.rotation * (masterPv.flipY ? -1 : 1);
    const rot = getRotationAngle(plot);
    let targetRotation;
    if (isEastLeftOfNorth(masterPlot)) {
        targetRotation = ((getRotationAngle(masterPlot) + masterRot) - rot) * (masterPv.flipY ? 1 : -1);
    } else {
        targetRotation = ((getRotationAngle(masterPlot) + (360 - masterRot)) - rot) * (masterPv.flipY ? 1 : -1);

    }
    if (!isCsysDirMatching(plot, masterPlot)) targetRotation = 360 - targetRotation;
    if (targetRotation < 0) targetRotation += 360;
    if (targetRotation > 359) targetRotation %= 360;
    return targetRotation;
}

export function canLoadStretchData(plot) {
    if (!plot || !isImage(plot)) return false;
    return (plot.dataWidth*plot.dataHeight) < MAX_RAW_IMAGE_SIZE;
}

export function canLoadStretchDataDirect(plot) {
    if (!plot || !isImage(plot)) return false;
    const size= (plot.dataWidth*plot.dataHeight);
    return size<Math.min(MAX_RAW_IMAGE_SIZE,MAX_DIRECT_IMAGE_SIZE);
}

export function isAllStretchDataLoadable(vr) {
    return getPlotViewAry(vr)
        .map( (pv) => primePlot(pv))
        .filter( (p) => isImage(p))
        .every( (p) => canLoadStretchData(p));
}

export function hasLocalRawData(plot) {
    return hasLocalRawDataInStore(plot);
}

export function hasLocalStretchByteData(plot) {
    return hasLocalStretchByteDataInStore(plot);
}

export function hasClearedByteData(plot) {
    return hasClearedDataInStore(plot);
}

export function isAllStretchDataLoaded(vr) {
    return getPlotViewAry(vr)
        .map( (pv) => primePlot(pv))
        .filter( (p) => isImage(p))
        .every( (p) => hasLocalStretchByteData(p));
}
