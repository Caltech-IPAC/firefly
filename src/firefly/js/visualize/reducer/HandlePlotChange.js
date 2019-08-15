/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'immutability-helper';
import {get, isEmpty,isUndefined, isArray, unionBy} from 'lodash';
import Cntlr, {ExpandType, WcsMatchType, ActionScope} from '../ImagePlotCntlr.js';
import {replacePlotView, replacePrimaryPlot, changePrimePlot, updatePlotViewScrollXY,
        findScrollPtToCenterImagePt, findScrollPtToPlaceOnDevPt,
        updateScrollToWcsMatch, updatePlotGroupScrollXY} from './PlotView.js';
import {WebPlot, clonePlotWithZoom, PlotAttribute, isHiPS, isImage,
    replaceHiPSProjectionUsingProperties, replaceHiPSProjection, getHiPsTitleFromProperties} from '../WebPlot.js';
import {changeProjectionCenter} from '../HiPSUtil.js';
import {logError, updateSet} from '../../util/WebUtil.js';
import {CCUtil, CysConverter} from '../CsysConverter.js';
import {convert, isPlotNorth, getRotationAngle, isRotationMatching} from '../VisUtil.js';
import {PlotPref} from './../PlotPref.js';
import {primePlot,
        clonePvAry,
        clonePvAryWithPv,
        applyToOnePvOrAll,
        applyToOnePvOrOverlayGroup,
        matchPlotViewByPositionGroup,
        getPlotViewIdxById,
        getPlotGroupIdxById,
        getOverlayById,
        findPlotGroup,
        isInSameGroup,
        findPlot,
        getPlotViewById,
        findCurrentCenterPoint,
        getCenterOfProjection} from '../PlotViewUtil.js';
import {makeImagePt, makeWorldPt, makeDevicePt} from '../Point.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import {RotateType} from '../PlotState.js';
import Point from '../Point.js';
import {updateTransform} from '../PlotTransformUtils.js';
import {WebPlotRequest} from '../WebPlotRequest.js';
import {hasWCSProjection} from '../PlotViewUtil';
import {getMatchingRotationAngle, isCsysDirMatching, isEastLeftOfNorth} from '../VisUtil';


//============ EXPORTS ===========
//============ EXPORTS ===========

const isFitFill= (userZoomType) =>  (userZoomType===UserZoomTypes.FIT || userZoomType===UserZoomTypes.FILL);
const clone = (obj,params={}) => Object.assign({},obj,params);

/**
 * 
 * @param {PlotView} pv
 * @param {object} att
 * @param {boolean} toAll
 * @return {PlotView} new plotview object
 */
function replaceAtt(pv,att, toAll) {
    if (toAll) {
        const plots= pv.plots.map( (p) => clone(p,{attributes:clone(p.attributes, att)}));
        return clone(pv,{plots});
    }
    else {
        const p= primePlot(pv);
        return replacePrimaryPlot(pv,clone(p,{attributes:clone(p.attributes, att)}));
    }
}


export function reducer(state, action) {

    let retState= state;
    switch (action.type) {
        case Cntlr.ZOOM_IMAGE_START  :
            retState= zoomStart(state, action);
            break;


        case Cntlr.STRETCH_CHANGE_START  :
        case Cntlr.COLOR_CHANGE_START  :
            retState= workingServerCall(state,action);
            break;
        
        
        case Cntlr.ZOOM_IMAGE_FAIL  :
        case Cntlr.STRETCH_CHANGE_FAIL:
        case Cntlr.COLOR_CHANGE_FAIL:
            retState= endServerCallFail(state,action);
            break;
        case Cntlr.COLOR_CHANGE  :
        case Cntlr.ZOOM_IMAGE  :
        case Cntlr.STRETCH_CHANGE  :
            retState= installTiles(state,action);
            // todo: also process adding to history
            break;
        case Cntlr.UPDATE_VIEW_SIZE :
            retState= updateViewSize(state,action);
            break;
        case Cntlr.PROCESS_SCROLL  :
            retState= processScroll(state,action);
            break;

        case Cntlr.CHANGE_CENTER_OF_PROJECTION:
            retState= processProjectionChange(state,action);
            break;
        case Cntlr.CHANGE_HIPS :
            retState= changeHiPS(state,action);
            break;

        case Cntlr.RECENTER:
            retState= recenter(state,action);
            break;
        case Cntlr.ROTATE:
            retState= updateClientRotation(state,action);
            break;
        case Cntlr.FLIP:
            retState= updateClientFlip(state,action);
            break;
        case Cntlr.CHANGE_PLOT_ATTRIBUTE :
            retState= changePlotAttribute(state,action);
            break;
        case Cntlr.ZOOM_LOCKING:
            retState= changeLocking(state,action);
            break;
        case Cntlr.POSITION_LOCKING:
            retState= changePositionLocking(state,action);
            break;
        case Cntlr.OVERLAY_COLOR_LOCKING:
            retState= changeOverlayColorLocking(state,action);
            break;
        case Cntlr.PLOT_PROGRESS_UPDATE  :
            retState= updatePlotProgress(state,action);
            break;
        case Cntlr.CHANGE_PRIME_PLOT  :
            retState= makeNewPrimePlot(state,action);
            break;

        case Cntlr.OVERLAY_PLOT_CHANGE_ATTRIBUTES :
            retState= changeOverlayPlotAttributes(state,action);
            break;

        case Cntlr.CHANGE_HIPS_IMAGE_CONVERSION :
            retState= changeHipsImageConversionSettings(state,action);
            break;


        case Cntlr.ADD_PROCESSED_TILES:
            retState= addProcessedTileData(state,action);
            break;

        case Cntlr.CHANGE_IMAGE_VISIBILITY:
            retState= changeVisibility(state,action);
            break;

        default:
            break;
    }
    return retState;
}

/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function changePlotAttribute(state,action) {
    const {plotId,attKey,attValue,overlayColorScope,positionScope,toAllPlotsInPlotView}= action.payload;
    let {plotViewAry}= state;
    const pv= getPlotViewById(state,plotId);
    const plot= primePlot(pv);
    if (!plot) return state;

    if (positionScope) {
        plotViewAry= applyToOnePvOrAll(state.positionLock, plotViewAry, plotId, false,
            (pv)=> replaceAtt(pv,{[attKey]:attValue},toAllPlotsInPlotView) );
    }
    else if (overlayColorScope) {
        const plotGroup= findPlotGroup(pv.plotGroupId,state.plotGroupAry);
        plotViewAry= applyToOnePvOrOverlayGroup(plotViewAry, plotId, plotGroup, false,
            (pv)=> replaceAtt(pv,{[attKey]:attValue},toAllPlotsInPlotView) );
    }
    else {
        plotViewAry= applyToOnePvOrAll(false, plotViewAry, plotId, false,
            (pv)=> replaceAtt(pv,{[attKey]:attValue},toAllPlotsInPlotView) );
    }
    return clone(state,{plotViewAry});
}



function changeLocking(state,action) {
    const {plotId, zoomLockingEnabled, zoomLockingType}=  action.payload;
    const zt= UserZoomTypes.get(zoomLockingType);
    return clone(state,{plotViewAry: state.plotViewAry.map( (pv) =>
            (pv.plotId===plotId) ?
                update(pv, {plotViewCtx : {$merge :{zoomLockingEnabled,zoomLockingType:zt} }} ) : pv
        )});
}


function zoomStart(state, action) {
    const {wcsMatchType, plotViewAry, expandedMode, mpwWcsPrimId}= state;
    const {plotId, zoomLevel, userZoomType, zoomLockingEnabled}= action.payload;
    const pvIdx=getPlotViewIdxById(state,plotId);
    const plot= pvIdx>-1 ? primePlot(plotViewAry[pvIdx]) : null;
    if (!plot) return state;
    let pv= plotViewAry[pvIdx];

    // up date book keeping
    const newCtx= {zoomLockingEnabled};
    if (expandedMode===ExpandType.COLLAPSE) newCtx.lastCollapsedZoomLevel= zoomLevel;
    if (zoomLockingEnabled && isFitFill(userZoomType))  newCtx.zoomLockingType= userZoomType;


    // update zoom factor and scroll position
    const centerImagePt= isFitFill(userZoomType) ?
                          makeImagePt(plot.dataWidth/2, plot.dataHeight/2) :
                          findCurrentCenterPoint(pv);

    pv= replacePrimaryPlot(pv,clonePlotWithZoom(plot,zoomLevel));


    if (wcsMatchType && mpwWcsPrimId!==plotId) {
        const masterPv= getPlotViewById(state, mpwWcsPrimId);
        pv= updateScrollToWcsMatch(state.wcsMatchType, masterPv, pv);
    }
    else {
        pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
    }


    pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv) =>
                     clone(oPv, {plot:clonePlotWithZoom(oPv.plot,zoomLevel)}) );


    // return new state
    return update(state, { plotViewAry : {
                  [pvIdx]: {$set: update(pv, {plotViewCtx: {$merge: newCtx}
        }) } }});
}

function installTiles(state, action) {
    const {plotViewAry, mpwWcsPrimId, wcsMatchType}= state;
    const {plotId, primaryStateJson,primaryTiles,overlayUpdateAry}= action.payload;
    let pv= getPlotViewById(state,plotId);
    let plot= primePlot(pv);

    if (!plot || !primaryStateJson) {
        logError('primePlot undefined or primaryStateJson is not set.', new Error());
        console.log('installTiles: state, action', state, action);
        return state;
    }

    pv.serverCall='success';
    pv= replacePrimaryPlot(pv,WebPlot.setPlotState(plot,primaryStateJson,primaryTiles));

    if (wcsMatchType && mpwWcsPrimId!==plotId) {
        const masterPV= getPlotViewById(state, mpwWcsPrimId);
        pv= updateScrollToWcsMatch(state.wcsMatchType, masterPV, pv);
    }
    else {
        const centerImagePt= findCurrentCenterPoint(pv);
        pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
    }


    if (!isEmpty(overlayUpdateAry) ) {
        pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv) => {
            if (!oPv.plot) return oPv;

            const overlayUpdate= overlayUpdateAry.find( (u) => u.imageOverlayId===oPv.imageOverlayId);
            if (!overlayUpdate) return oPv;

            const p= WebPlot.setPlotState(oPv.plot,overlayUpdate.overlayStateJson,overlayUpdate.overlayTiles);
            return clone(oPv, {plot:p});
        });
    }

    plot= primePlot(pv); // get the updated on
    PlotPref.putCacheColorPref(pv.plotViewCtx.preferenceColorKey, plot.plotState);
    PlotPref.putCacheZoomPref(pv.plotViewCtx.preferenceZoomKey, plot.plotState);
    const processedTiles= state.processedTiles.filter( (d) => d.plotId!==plotId); // remove old client tile data

    return clone(state, {plotViewAry : replacePlotView(plotViewAry,pv), processedTiles});
}


/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function processProjectionChange(state,action) {
    const {plotId,centerProjPt}= action.payload;
    const {plotViewAry}= state;
    const newPlotViewAry= applyToOnePvOrAll(state.positionLock, plotViewAry, plotId, false,
         (pv)=> {
             const plot= primePlot(pv);
             if (plot) pv= replacePrimaryPlot(pv, changeProjectionCenter(plot,centerProjPt));
             return pv;
         } );

    return clone(state, {plotViewAry : newPlotViewAry});
}

/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function changeHiPS(state,action) {
    const {plotId,hipsProperties, coordSys, hipsUrlRoot, cubeIdx, applyToGroup}= action.payload;
    let {centerProjPt}= action.payload;
    let {plotViewAry, positionLock}= state;

    let pv= getPlotViewById(state, plotId);
    const originalPlot= primePlot(pv);
    if (!originalPlot) return state;

    let plot= clone(originalPlot);


    // single plot stuff

    if (hipsUrlRoot) plot.hipsUrlRoot= hipsUrlRoot;
    if (hipsProperties) {
        plot.hipsProperties= hipsProperties;
        plot.title= getHiPsTitleFromProperties(hipsProperties);
        plot.cubeDepth= Number(get(hipsProperties, 'hips_cube_depth')) || 1;
        plot.cubeIdx= Number(get(hipsProperties, 'hips_cube_firstframe')) || 0;
        plot= replaceHiPSProjectionUsingProperties(plot, hipsProperties, getCenterOfProjection(plot) );
        if (!centerProjPt) {
            centerProjPt= convert(getCenterOfProjection(originalPlot), plot.dataCoordSys);
        }
    }

    if (!isUndefined(cubeIdx) && plot.cubeDepth>1 && cubeIdx<plot.cubeDepth) {
        plot.cubeIdx= cubeIdx;
    }
    pv= replacePrimaryPlot(pv, plot);
    pv.serverCall= 'success';
    pv.plottingStatus= 'done';
    plotViewAry= replacePlotView(plotViewAry,pv);

    if (coordSys) {
        plotViewAry= changeHipsCoordinateSys(plotViewAry, pv, coordSys, applyToGroup && positionLock);
        if (!centerProjPt) centerProjPt= convert(getCenterOfProjection(originalPlot), coordSys);
    }

    if (centerProjPt) {
        plotViewAry= applyToOnePvOrAll(state.positionLock, plotViewAry, plot.plotId, false,
            (pv) => {
                const p= changeProjectionCenter(primePlot(pv),centerProjPt);
                return replacePrimaryPlot(pv, p);
            });
    }

    return clone(state, {plotViewAry});
}


/**
 *
 * @param {Array<PlotView>} plotViewAry
 * @param {PlotView} pv
 * @param {CoordinateSys} coordSys
 * @param {boolean} applyToGroup
 * @return {Array<PlotView>}
 */
function changeHipsCoordinateSys(plotViewAry, pv, coordSys, applyToGroup) {
    return applyToOnePvOrAll(applyToGroup, plotViewAry, pv.plotId, false,
        (pv) => {
            let plot= primePlot(pv);
            plot= replaceHiPSProjection(plot, coordSys, getCenterOfProjection(plot) );
            return replacePrimaryPlot(pv, plot);
        });
}


function processScroll(state,action) {
    const {plotId,scrollPt}= action.payload;
    const {plotGroupAry, wcsMatchType}= state;
    let {plotViewAry, mpwWcsPrimId}= state;
    plotViewAry= updatePlotGroupScrollXY(state,plotId,plotViewAry, plotGroupAry,scrollPt);

    if (wcsMatchType && isInSameGroup(state, plotId, mpwWcsPrimId)) {
        mpwWcsPrimId= plotId;
    }
    return Object.assign({},state,{plotViewAry, mpwWcsPrimId});
}


function rotatePv(pv, targetAngle, rotateNorthLock) {
    pv= clone(pv);
    pv.rotation= targetAngle;
    pv.plotViewCtx= clone(pv.plotViewCtx, {rotateNorthLock});
    return updateTransform(pv);

}

function rotatePvToMatch(pv, masterPv, rotateNorthLock) {
    if (isRotationMatching(pv,masterPv)) return pv;
    const masterPlot= primePlot(masterPv);
    const plot= primePlot(pv);
    if (getRotationAngle(masterPlot)!==getRotationAngle(plot)) rotateNorthLock= false;
    const csysDirMatching= isCsysDirMatching(plot,masterPlot);
    let rotation= getMatchingRotationAngle(masterPv,pv);
    if (isEastLeftOfNorth(masterPlot)) {
        rotation= csysDirMatching ? 360-rotation :360+rotation;
    }
    else {
        rotation= csysDirMatching ? 360+rotation :360-rotation;
    }
    pv= {...pv, rotation};
    pv.plotViewCtx= clone(pv.plotViewCtx, {rotateNorthLock});
    return updateTransform(pv);
}



/**
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function updateClientRotation(state,action) {
    const {plotId,angle, rotateType, actionScope}= action.payload;
    const {wcsMatchType}= state;
    let   {plotViewAry}= state;

    const pv= getPlotViewById(state,plotId);
    const plot= primePlot(pv);
    if (!plot) return state;
    if (rotateType===RotateType.NORTH && isPlotNorth(plot) && !pv.rotation) return state;

    let targetAngle;
    let rotateNorthLock;

    switch (rotateType) {
        case RotateType.NORTH:
            targetAngle= 360 - getRotationAngle(plot);
            rotateNorthLock= true;
            break;
        case RotateType.ANGLE:
            targetAngle= angle ? 360-angle : 0;
            rotateNorthLock= false;
            break;
        case RotateType.UNROTATE:
            targetAngle= 0;
            rotateNorthLock= false;
            break;
    }

    targetAngle= (360 + targetAngle) % 360;
    if (!isEastLeftOfNorth(plot)) targetAngle= 360-targetAngle;

    const masterPv= rotatePv(pv,targetAngle,rotateNorthLock);
    const matchingByWcs= wcsMatchType===WcsMatchType.Standard || wcsMatchType===WcsMatchType.Target;
    if (matchingByWcs || (rotateType===RotateType.NORTH && !wcsMatchType) ) {
        plotViewAry= clonePvAryWithPv(plotViewAry, masterPv);
        if (actionScope===ActionScope.GROUP) {
            plotViewAry= matchPlotViewByPositionGroup(state, masterPv,plotViewAry, false, (pv) => rotatePvToMatch(pv,masterPv, rotateNorthLock));
        }
    }
    else {
        plotViewAry= actionScope===ActionScope.GROUP ?
            applyToOnePvOrAll(state.positionLock, plotViewAry,plotId,false, (aPv) => rotatePv(aPv,targetAngle, rotateNorthLock&&aPv===pv)) :
            clonePvAryWithPv(plotViewAry, masterPv);
    }

    return clone(state,{plotViewAry});
}

function flipPv(pv, isY) {
    pv= clone(pv);
    if (isY) pv.flipY= !pv.flipY;
    else     pv.flipX= !pv.flipX;
    const cc= CysConverter.make(primePlot(pv));
    if (!cc) return pv;
    const {width,height}= pv.viewDim;
    const centerOfDev= makeDevicePt(width/2,height/2);
    const ipt= cc.getImageCoords(centerOfDev);
    pv= updateTransform(pv);
    const scrollPt= findScrollPtToPlaceOnDevPt(pv,ipt,centerOfDev);
    return updatePlotViewScrollXY(pv,scrollPt);
}


/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function updateClientFlip(state,action) {
    const {plotId,isY,actionScope=ActionScope.GROUP }= action.payload;
    const {plotGroupAry}= state;
    let   {plotViewAry}= state;

    const pv= getPlotViewById(state,plotId);
    if (!pv) return state;

    plotViewAry= actionScope===ActionScope.GROUP ?
        applyToOnePvOrAll(state.positionLock, plotViewAry,plotId,false, (pv) => flipPv(pv,isY)) :
        clonePvAryWithPv(plotViewAry, flipPv(pv,isY));

    return clone(state,{plotViewAry});
}


function updateViewSize(state,action) {
    const {plotId,width,height}= action.payload;


    const plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!==plotId ) return pv;
        const plot= primePlot(pv);
        let centerImagePt;

        if (plot) {
            centerImagePt= (pv.scrollX===-1 && pv.scrollY===-1) ?
                makeImagePt(plot.dataWidth/2, plot.dataHeight/2) :
                findCurrentCenterPoint(pv);
        }
        const w= isUndefined(width) ? pv.viewDim.width : width;
        const h= isUndefined(height) ? pv.viewDim.height : height;
        pv= Object.assign({}, pv, {viewDim: {width:w, height:h}});
        if (!plot) return pv;

        const masterPv= getPlotViewById(state, state.mpwWcsPrimId);
        pv= updateTransform(pv);
        const hasProj= hasWCSProjection(pv) ;

        if (isHiPS(plot)) {
            centerImagePt= makeImagePt( plot.dataWidth/2, plot.dataHeight/2);
            pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
        }
        else if (state.wcsMatchType===WcsMatchType.Standard && state.mpwWcsPrimId!==plotId && hasProj) {
            pv= updateScrollToWcsMatch(state.wcsMatchType, masterPv, pv);
        }
        else if (state.wcsMatchType===WcsMatchType.Target && hasProj) {
            pv= updateScrollToWcsMatch(state.wcsMatchType, masterPv, pv);
        }
        else if (state.wcsMatchType===WcsMatchType.Pixel || state.wcsMatchType===WcsMatchType.PixelCenter) {
            pv= updateScrollToWcsMatch(state.wcsMatchType, masterPv, pv);
        }
        else if (isUndefined(pv.scrollX) || isUndefined(pv.scrollY)) {
            pv= recenterPv(null, false)(pv);
        }
        else {
            pv= centerImagePt ? updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt)) : recenter((null,false)(pv));
        }

        pv= updateTransform(pv);
        return pv;

    });
    return Object.assign({},state,{plotViewAry});
}



/**
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function recenter(state,action) {
    const {plotId, centerPt, centerOnImage }= action.payload;
    const {plotGroupAry, wcsMatchType}= state;
    let   {plotViewAry}= state;
    const pv= getPlotViewById(state,plotId);

    if (wcsMatchType) {
        const newPv= recenterPv(centerPt, centerOnImage)(pv);
        plotViewAry= replacePlotView(plotViewAry, newPv);
        plotViewAry= matchPlotViewByPositionGroup(state, newPv, plotViewAry, false, (pv) => {
            return updateScrollToWcsMatch(wcsMatchType, newPv, pv);
        } );
    }
    else {
        plotViewAry= applyToOnePvOrAll(state.positionLock, plotViewAry,plotId,false, recenterPv(centerPt, centerOnImage));
    }


    return clone(state,{plotViewAry, mpwWcsPrimId:plotId});
}

/**
 * Center on the FIXED_TARGET attribute or the center of the plot or specified center point
 * @param {Point} centerPt center point
 * @param {boolean} only used if centerPt is not defined.  If true then the centering will be
 *                  the center of the image.  If false, then the center point will be the
 *                  FIXED_TARGET attribute, if defined. Otherwise it will be the center of the image.
 * @return {{}} a new plot view
 */

function recenterPv(centerPt,  centerOnImage) {
    return (pv) => {
        const plot = primePlot(pv);
        if (!plot) return pv;
        let centerImagePt;

        if (centerPt) {
            if (centerPt.type === Point.IM_PT) {
                centerImagePt = makeImagePt(centerPt.x, centerPt.y);
            } else {
                centerImagePt = makeWorldPt(centerPt.x, centerPt.y);
            }
        } else {
            const wp = plot.attributes[PlotAttribute.INIT_CENTER] || plot.attributes[PlotAttribute.FIXED_TARGET] ;
            if (wp && !centerOnImage) {
                centerImagePt = CCUtil.getImageCoords(plot, wp);
            }
            else {
                centerImagePt = makeImagePt(plot.dataWidth / 2, plot.dataHeight / 2);
            }
        }

        if (isImage(primePlot(pv))) {
            return updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv, centerImagePt));
        }
        else {
            let cp;
            if (centerPt) cp= CCUtil.getWorldCoords(plot,centerPt);
            if (!cp) cp= plot.attributes[PlotAttribute.FIXED_TARGET];
            if (!cp) cp= makeWorldPt(0,0,plot.imageCoordSys);
            return replacePrimaryPlot(pv, changeProjectionCenter(plot,cp));
        }

    };
}

function makeNewPrimePlot(state,action) {
    const {plotId,primeIdx}= action.payload;
    let pv=  getPlotViewById(state,plotId);
    if (!pv || isEmpty(pv.plots) || pv.plots.length<=primeIdx) return state;
    pv= changePrimePlot(pv, primeIdx);
    return clone(state,{plotViewAry:clonePvAryWithPv(state,pv)});
}

/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function changePositionLocking(state,action) {
    const {plotId,positionLock}=  action.payload;
    if (positionLock && plotId && isHiPS(primePlot(state,plotId))) {
        const pv= getPlotViewById(state, plotId);
        const plot= primePlot(pv);
        const plotViewAry= changeHipsCoordinateSys(state.plotViewAry, pv,plot.imageCoordSys,true );
        state= clone(state, {plotViewAry});
        state= processProjectionChange(state,
            clone(action, {payload:{plotId, centerProjPt:getCenterOfProjection(plot) }}));
    }

    return {...state, positionLock};

}


function changeOverlayColorLocking(state,action) {
    const {plotId,overlayColorLock}=  action.payload;
    const {plotGroupId} = getPlotViewById(state,plotId);

    const pgIdx= getPlotGroupIdxById(state,plotGroupId);

    if (pgIdx < 0) return state;
    state= updateSet(state, ['plotGroupAry',pgIdx,'overlayColorLock'], overlayColorLock);
    return state;
}


function endServerCallFail(state,action) {
    const {plotId,message}= action.payload;
    const stat= {serverCall:'fail'};
    if (typeof message === 'string') stat.plottingStatus= message;
    return clone(state,{plotViewAry:clonePvAry(state,plotId, stat)});
}
function workingServerCall(state,action) {
    const {plotId,message}= action.payload;
    return clone(state,{plotViewAry:clonePvAry(state,plotId,
                           {serverCall:'working', plottingStatus:message})});
}


function changeOverlayPlotAttributes(state,action) {
    const {plotId, imageOverlayId, attributes}= action.payload;
    const plotViewAry= state.plotViewAry
        .map( (pv) => {
            if (pv.plotId!==plotId) return pv;
            const overlayPlotViews = pv.overlayPlotViews
                 .map( (opv) => opv.imageOverlayId!==imageOverlayId ? opv : clone(opv,attributes));
            return clone(pv, {overlayPlotViews});
        });
    return clone(state,{plotViewAry});
}

function changeHipsImageConversionSettings(state,action) {
    const {plotId, hipsImageConversionChanges}= action.payload;
    const changes= {...hipsImageConversionChanges};
    const plotViewAry= state.plotViewAry
        .map( (pv) => {
            if (pv.plotId!==plotId || !pv.plotViewCtx.hipsImageConversion) return pv;
            if (changes.hipsRequestRoot) changes.hipsRequestRoot= WebPlotRequest.makeFromObj(changes.hipsRequestRoot);
            if (changes.imageRequestRoot) changes.imageRequestRoot= WebPlotRequest.makeFromObj(changes.imageRequestRoot);
            if (changes.allSkyRequest) changes.allSkyRequest= WebPlotRequest.makeFromObj(changes.allSkyRequest);
            pv= {...pv};
            pv.plotViewCtx= {...pv.plotViewCtx};
            pv.plotViewCtx.hipsImageConversion= {...pv.plotViewCtx.hipsImageConversion, ...changes};
            return pv;
        });
    return clone(state,{plotViewAry});

}



function addProcessedTileData(state,action) {
    const {plotId, plotImageId, imageOverlayId, zoomFactor}= action.payload;
    let {clientTileAry}= action.payload;
    let {processedTiles}= state;
    const pv= getPlotViewById(state, plotId);
    if (!pv) return state;
    const plot= imageOverlayId? get(getOverlayById(pv,imageOverlayId),'plot') : findPlot(pv, plotImageId);
    if (!plot) return state;
    if (plot.zoomFactor!==zoomFactor) return state;

    if (!isArray(clientTileAry)) clientTileAry= [clientTileAry];
    let entry= processedTiles.find( (d) => d.plotId===plotId &&
                                              d.imageOverlayId===imageOverlayId &&
                                              d.plotImageId===plotImageId);
    const doReplace= Boolean(entry);
    if (doReplace && entry.zoomFactor!==zoomFactor) entry= null;

    entry= entry ? clone(entry) : { plotId, imageOverlayId, plotImageId, zoomFactor, clientTileAry:[] };

    entry.clientTileAry= unionBy(clientTileAry,entry.clientTileAry, 'url');

    processedTiles= doReplace ? processedTiles.map( (d) =>
                                         d.plotId===plotId &&
                                         d.imageOverlayId===imageOverlayId &&
                                         d.plotImageId===plotImageId ? entry : d) :
                               [...processedTiles, entry];

    return clone(state, {processedTiles});
}

function updatePlotProgress(state,action) {
    const {plotId, message:plottingStatus, done, requestKey, callSuccess=true}= action.payload;
    //console.log(`updatePlotProgress: plotId;${plotId}, message:${plottingStatus}, done:${done}`);
    if (!plotId) return state;
    const plotView=  getPlotViewById(state,plotId);
    if (!plotView) return state;
    if (requestKey!==plotView.request.getRequestKey()) return state;
    if (plotView.plottingStatus===plottingStatus) return state;

    const serverCall= done ? callSuccess ? 'success' : 'fail' : 'working';

    return clone(state,{plotViewAry:clonePvAry(state,plotId, {plottingStatus,serverCall})});
}

function changeVisibility(state,action) {
    const {plotId, visible}= action.payload;
    return {...state,plotViewAry:clonePvAry(state,plotId, {visible})};

}
