/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'immutability-helper';
import {isEmpty, isUndefined, isArray, unionBy, isString, isNumber} from 'lodash';
import Cntlr, {ExpandType, WcsMatchType, ActionScope} from '../ImagePlotCntlr.js';
import {replacePlotView, replacePrimaryPlot, changePrimePlot, updatePlotViewScrollXY,
        findScrollPtToCenterImagePt, findScrollPtToPlaceOnDevPt,
        updateScrollToWcsMatch, updatePlotGroupScrollXY} from './PlotView.js';
import {
    WebPlot, clonePlotWithZoom, isHiPS, isImage,
    replaceHiPSProjectionUsingProperties, getHiPsTitleFromProperties, DEFAULT_BLANK_HIPS_TITLE
} from '../WebPlot.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {replaceHiPSProjection, changeProjectionCenter} from '../HiPSUtil.js';
import {updateSet} from '../../util/WebUtil.js';
import {CCUtil, CysConverter} from '../CsysConverter.js';
import {convert, isPlotNorth, getRotationAngle, isCsysDirMatching, isEastLeftOfNorth} from '../VisUtil';
import {PlotPref} from '../PlotPref';
import {
    primePlot,
    clonePvAry,
    clonePvAryWithPv,
    applyToOnePvOrAll,
    applyToOnePvOrOverlayGroup,
    matchPlotViewByPositionGroup,
    getPlotViewIdxById,
    getPlotGroupIdxById,
    getOverlayById,
    findPlotGroup,
    findPlot,
    getPlotViewById,
    findCurrentCenterPoint,
    getCenterOfProjection,
    getMatchingRotationAngle,
    isRotationMatching,
    hasWCSProjection, canLoadStretchDataDirect, isThreeColor
} from '../PlotViewUtil.js';
import Point, {parseAnyPt, makeImagePt, makeWorldPt, makeDevicePt} from '../Point.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import PlotState, {RotateType} from '../PlotState.js';
import {updateTransform} from '../PlotTransformUtils.js';
import {WebPlotRequest} from '../WebPlotRequest.js';
import {logger} from '../../util/Logger.js';


//============ EXPORTS ===========
//============ EXPORTS ===========

const isFitFill= (userZoomType) =>  (userZoomType===UserZoomTypes.FIT || userZoomType===UserZoomTypes.FILL);
const clone = (obj,params={}) => ({...obj,...params});

/**
 * 
 * @param {PlotView} pv
 * @param {object} att
 * @param {boolean} toAll
 * @return {PlotView} new plotview object
 */
function replaceAtt(pv,att, toAll) {
    if (toAll) {
        const plots= pv.plots.map( (p) => ({...p,attributes:{...p.attributes, ...att}}));
        return {...pv,plots};
    }
    else {
        const p= primePlot(pv);
        return replacePrimaryPlot(pv,{...p,attributes:{...p.attributes, ...att}});
    }
}


export function reducer(state, action) {

    let retState= state;
    switch (action.type) {
        case Cntlr.ZOOM_IMAGE_START  :
            retState= zoomStart(state, action);
            break;
        /**
         * @global
         * @public
         * @typedef {Object} ImageTile
         * @summary a single image tile
         *
         * @prop {number} width - width of this tile
         * @prop {number} height - height of this tile
         * @prop {number} index - index of this tile
         * @prop {string} url - file key to use in the service to retrieve this tile
         * @prop {number} x - pixel offset of this tile
         * @prop {number} y - pixel offset of this tile
         *
         */


        case Cntlr.STRETCH_CHANGE_START  :
        case Cntlr.COLOR_CHANGE_START  :
            retState= workingServerCall(state,action);
            break;
        
        
        case Cntlr.ZOOM_IMAGE_FAIL  :
            retState= zoomFail(state,action);
            break;
        case Cntlr.STRETCH_CHANGE_FAIL:
        case Cntlr.COLOR_CHANGE_FAIL:
            retState= endServerCallFail(state,action);
            break;
        case Cntlr.ZOOM_IMAGE  :
            retState= zoomImage(state,action);
            break;
        case Cntlr.COLOR_CHANGE  :
        case Cntlr.STRETCH_CHANGE  :
            retState= installTiles(state,action);
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

        case Cntlr.UPDATE_RAW_IMAGE_DATA:
            retState= updateRawImageData(state,action);
            break;

        case Cntlr.CHANGE_IMAGE_VISIBILITY:
            retState= changeVisibility(state,action);
            break;
        case Cntlr.REQUEST_LOCAL_DATA:
            retState= requestLocalData(state,action);
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
    const {plotId,attKey,attValue,changes={},overlayColorScope,positionScope,toAllPlotsInPlotView}= action.payload;
    let {plotViewAry}= state;
    const pv= getPlotViewById(state,plotId);
    const plot= primePlot(pv);
    if (!plot) return state;
    const newAtts= {...changes};
    if (attKey) newAtts[attKey]= attValue;

    if (positionScope) {
        plotViewAry= applyToOnePvOrAll(state.positionLock, plotViewAry, plotId, false,
            (pv)=> replaceAtt(pv,newAtts,toAllPlotsInPlotView) );
    }
    else if (overlayColorScope) {
        const plotGroup= findPlotGroup(pv.plotGroupId,state.plotGroupAry);
        plotViewAry= applyToOnePvOrOverlayGroup(plotViewAry, plotId, plotGroup, false,
            (pv)=> replaceAtt(pv,newAtts,toAllPlotsInPlotView) );
    }
    else {
        plotViewAry= applyToOnePvOrAll(false, plotViewAry, plotId, false,
            (pv)=> replaceAtt(pv,newAtts,toAllPlotsInPlotView) );
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
    const {plotId, zoomLevel, userZoomType, zoomLockingEnabled, devicePt, localRawData}= action.payload;
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
                          makeImagePt(plot.dataWidth/2, plot.dataHeight/2) : findCurrentCenterPoint(pv);
    const mouseOverImagePt= devicePt && CCUtil.getImageCoords(plot,devicePt);

    pv= replacePrimaryPlot(pv,clonePlotWithZoom(plot,zoomLevel));


    if (wcsMatchType && mpwWcsPrimId!==plotId) {
        const masterPv= getPlotViewById(state, mpwWcsPrimId);
        pv= updateScrollToWcsMatch(state.wcsMatchType, masterPv, pv);
    }
    else {
        if (isImage(plot)) {
            pv= updatePlotViewScrollXY(pv,
                devicePt ? findScrollPtToPlaceOnDevPt(pv, mouseOverImagePt,devicePt) :
                findScrollPtToCenterImagePt(pv,centerImagePt));
            if (localRawData) pv.localZoomStart= true;

        }
        else {
            pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
        }
    }


    if (pv.overlayPlotViews?.length) {
        pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv) =>
            clone(oPv, {plot:clonePlotWithZoom(oPv.plot,zoomLevel)}) );
    }


    // return new state
    return update(state, { plotViewAry : {
                  [pvIdx]: {$set: update(pv, {plotViewCtx: {$merge: newCtx}
        }) } }});
}

function zoomImage(state,action) {
    const {rawData}= action.payload;
    const newState= rawData ? zoomStart(state, action): state;
    return installTiles(newState,action);
}

function installTiles(state, action) {
    const {plotViewAry, mpwWcsPrimId, wcsMatchType}= state;
    const clearLocal= action.type===Cntlr.STRETCH_CHANGE;
    const {plotId, primaryStateJson,primaryTiles,overlayUpdateAry,
        rawData,bias,contrast, useRed, useGreen, useBlue}= action.payload;
    let pv= getPlotViewById(state,plotId);
    let plot= primePlot(pv);



    if (!plot || !primaryStateJson) {
        logger.error('primePlot undefined or primaryStateJson is not set.', new Error());
        console.log('installTiles: state, action', state, action);
        return state;
    }

    if (isHiPS(plot)) {
        const plotState= PlotState.makePlotStateWithJson(primaryStateJson);
        const newPlot= {...plot,plotState};

        if (isNumber(bias) || isNumber(contrast)) {
            const {bandData:oldBandData}= newPlot.rawData;
            const bandData= oldBandData.map( (entry)  => ({...entry,
                bias:  isNumber(bias) ? bias : entry.bias,
                contrast:  isNumber(contrast) ? contrast : entry.contrast,
            }));
            newPlot.rawData= {...plot.rawData,bandData};
        }
        pv= replacePrimaryPlot(pv, newPlot);
        return {...state, plotViewAry : replacePlotView(plotViewAry,pv)};
    }

    pv= {...pv};
    pv.serverCall='success';
    const tileData= canLoadStretchDataDirect(plot) ? undefined : primaryTiles;
    pv= replacePrimaryPlot(pv,
        WebPlot.replacePlotValues(plot,primaryStateJson,tileData,rawData,bias,contrast,useRed,useGreen,useBlue));
    if (action.type===Cntlr.COLOR_CHANGE && !isThreeColor(pv) && canLoadStretchDataDirect(plot)) {
        const cId= primePlot(pv).plotState.getColorTableId();
        pv.plots= pv.plots.map( (p) => {
            const plotState= p.plotState.copy();
            plotState.setColorTableId(cId);
            return {...p,plotState};
        });
    }
    if (rawData) pv.localZoomStart= false;

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

            const p= WebPlot.replacePlotValues(oPv.plot,overlayUpdate.overlayStateJson,overlayUpdate.overlayTiles);
            return clone(oPv, {plot:p});
        });
    }

    plot= primePlot(pv); // get the updated on
    if (clearLocal) plot.dataRequested= false;
    PlotPref.putCacheColorPref(pv.plotViewCtx.preferenceColorKey, plot.plotState);
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
    const {plotViewAry,wcsMatchType}= state;
    const newPlotViewAry= applyToOnePvOrAll(state.positionLock, plotViewAry, plotId, false,
         (pv)=> {
             const plot= primePlot(pv);
             if (plot) pv= replacePrimaryPlot(pv, changeProjectionCenter(plot,centerProjPt));
             return pv;
         } );
    const matchingByWcs= wcsMatchType===WcsMatchType.Standard || wcsMatchType===WcsMatchType.Target;
    let newState= {...state, plotViewAry :newPlotViewAry};
    if (matchingByWcs)  {
        const imagePv= newPlotViewAry.find( (aPv) => isImage(primePlot(aPv)));
        if (imagePv) {
            const finalPvAry= recenterUsingWcsMatch(newState,imagePv,centerProjPt, false,true);
            newState= {...newState, plotViewAry :finalPvAry};
        }
    }
    return newState;
}

/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function changeHiPS(state,action) {
    const {plotId,hipsProperties, coordSys, hipsUrlRoot, cubeIdx, applyToGroup, blank= false, blankColor}= action.payload;
    let {centerProjPt}= action.payload;
    let {plotViewAry}= state;
    const {positionLock}= state;

    let pv= getPlotViewById(state, plotId);
    const originalPlot= primePlot(pv);
    if (!originalPlot) return state;

    let plot= {...originalPlot};
    let {plotViewCtx}= pv;

    // single plot stuff

    if (hipsUrlRoot) {
        plot.hipsUrlRoot= hipsUrlRoot;
        plot.blank= blank;
    }


    if (hipsProperties) {
        plot.hipsProperties= hipsProperties;
        plot.title= plot.blank ? DEFAULT_BLANK_HIPS_TITLE : getHiPsTitleFromProperties(hipsProperties);
        plot.cubeDepth= Number(hipsProperties?.hips_cube_depth) || 1;
        plot.cubeIdx= Number(hipsProperties?.hips_cube_firstframe) || 0;
        plot= replaceHiPSProjectionUsingProperties(plot, hipsProperties, getCenterOfProjection(plot) );
        if (!centerProjPt) {
            centerProjPt= convert(getCenterOfProjection(originalPlot), plot.dataCoordSys);
        }
    }

    if (!isUndefined(cubeIdx) && plot.cubeDepth>1 && cubeIdx<plot.cubeDepth) {
        plot.cubeIdx= cubeIdx;
    }
    if (!isUndefined(blankColor)) plot.blankColor= blankColor;

    if (hipsProperties || hipsUrlRoot || !isUndefined(cubeIdx)) {
        plot.plotImageId= `${pv.plotId}--${pv.plotViewCtx.plotCounter}`;
        plotViewCtx= {...plotViewCtx, plotCounter:plotViewCtx.plotCounter+1};
    }

    pv= replacePrimaryPlot(pv, plot);
    pv.serverCall= 'success';
    pv.plottingStatusMsg= 'done';
    pv.plotViewCtx= plotViewCtx;
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

    if (wcsMatchType) mpwWcsPrimId= plotId;

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
    if (!masterPlot || !plot) return pv;
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
            pv= centerImagePt ? updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt)) : recenterPv(null,false)(pv);
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
    const {plotId, centerPt, centerOnImage, updateFixedTarget= false}= action.payload;
    const {wcsMatchType}= state;
    const pv= getPlotViewById(state,plotId);

    let plotViewAry;
    if (wcsMatchType) {
        plotViewAry= recenterUsingWcsMatch(state,pv,centerPt,centerOnImage, false, updateFixedTarget);
    }
    else {
        plotViewAry= applyToOnePvOrAll(state.positionLock, state.plotViewAry,plotId,false,
            recenterPv(centerPt, centerOnImage, updateFixedTarget));
    }


    return clone(state,{plotViewAry, mpwWcsPrimId:plotId});
}


function recenterUsingWcsMatch(state, pv, centerPt, centerOnImage=false, onlyImages= false, updateFixedTarget= false) {
    const {wcsMatchType}= state;
    let   {plotViewAry}= state;
    const newPv= recenterPv(centerPt, centerOnImage, updateFixedTarget)(pv);
    plotViewAry= replacePlotView(plotViewAry, newPv);
    plotViewAry= matchPlotViewByPositionGroup(state, newPv, plotViewAry, false, (pv) => {
        if (onlyImages && isHiPS(primePlot(pv))) return pv;
        if (isImage(primePlot(pv))) {
            return updateScrollToWcsMatch(wcsMatchType, newPv, pv);
        }
        else {
            return recenterPv(centerPt, centerOnImage, updateFixedTarget)(pv);
        }
    } );
    return plotViewAry;
}


/**
 * Center on the FIXED_TARGET attribute or the center of the plot or specified center point
 * @param {Point|null|undefined} centerPt center point
 * @param {boolean} centerOnImage - only used if centerPt is not defined.  If true then the centering will be
 *                  the center of the image.  If false, then the center point will be the
 *                  FIXED_TARGET attribute, if defined. Otherwise it will be the center of the image.
 * @param {boolean} updateFixedTarget if true the PlotAttribute.FIXED_TARGET will but updated to this point
 * @return {Function} a new plot view
 */

function recenterPv(centerPt,  centerOnImage, updateFixedTarget= false) {
    return (pv) => {
        const plot = primePlot(pv);
        if (!plot) return pv;
        let centerImagePt= centerPt;

        if (!centerPt) {
            let point = plot.attributes[PlotAttribute.INIT_CENTER] || plot.attributes[PlotAttribute.FIXED_TARGET] ;
            if (point && !centerOnImage) {
                if (isString(point)) point= parseAnyPt(point);
                centerImagePt = CCUtil.getImageCoords(plot, point);
            }
            else {
                centerImagePt = makeImagePt(plot.dataWidth / 2, plot.dataHeight / 2);
            }
        }

        if (isImage(primePlot(pv))) {
            const newPv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv, centerImagePt));
            if (!updateFixedTarget || centerPt.type!==Point.W_PT) return newPv;
            const newPlot= {...primePlot(pv), attributes:{...plot.attributes,[PlotAttribute.FIXED_TARGET]:centerPt}};
            return replacePrimaryPlot(newPv,newPlot);
        }
        else {
            let cp;
            if (centerPt) cp= CCUtil.getWorldCoords(plot,centerPt);
            if (!cp) cp= plot.attributes[PlotAttribute.FIXED_TARGET];
            if (!cp) cp= makeWorldPt(0,0,plot.imageCoordSys);
            const newPlot= changeProjectionCenter(plot,cp);
            if (updateFixedTarget || centerPt?.type===Point.W_PT) {
                newPlot.attributes= {...newPlot.attributes, [PlotAttribute.FIXED_TARGET]:centerPt};
            }
            return replacePrimaryPlot(pv, newPlot);
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

function zoomFail(state,action) {
    const {plotId}= action.payload;
    const newState= {...state, plotViewAry: clonePvAry(state,plotId, {localZoomStart:false}) };
    return endServerCallFail(newState,action);
}


function endServerCallFail(state,action) {
    const {plotId,message}= action.payload;
    const stat= {serverCall:'fail'};
    if (isString(message)) stat.plottingStatusMsg= message;
    return clone(state,{plotViewAry:clonePvAry(state,plotId, stat)});
}
function workingServerCall(state,action) {
    const {plotId,message}= action.payload;
    return clone(state,{plotViewAry:clonePvAry(state,plotId,
                           {serverCall:'working', plottingStatusMsg:message})});
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
    const plot= imageOverlayId? getOverlayById(pv,imageOverlayId)?.plot : findPlot(pv, plotImageId);
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

function updateRawImageData(state, action) {
    const {plotId, plotImageId, imageOverlayId, rawData}= action.payload;
    const pv= getPlotViewById(state,plotId);
    if (!pv) return state;
    let updatedPv;

    if (imageOverlayId) {
        const overlayPlotViews= pv.overlayPlotViews?.map( (oPv) => {
            if (oPv?.plot?.plotImageId!==plotImageId) return oPv;
            oPv.plot= {...oPv.plot,rawData};
            return oPv;
        });
        updatedPv= {...pv, overlayPlotViews};
    }
    else {
        const plots= pv.plots.map( (p) => p.plotImageId===plotImageId ? {...p,rawData}: p);
        updatedPv= {...pv,plots};
    }

    const plotViewAry= replacePlotView(state.plotViewAry,updatedPv);
    return {...state, plotViewAry};
}

function requestLocalData(state, action) {
    const {plotImageId, plotId, dataRequested=true, imageOverlayId}= action.payload;
    const pv= getPlotViewById(state,plotId);
    if (!pv) return state;
    let updatedPv;
    if (imageOverlayId) {
        const overlayPlotViews= pv.overlayPlotViews.map( (oPv) => {
            if (oPv?.plot?.plotImageId!==plotImageId) return oPv;
            oPv.plot= {...oPv.plot,dataRequested};
            return oPv;
        });
        updatedPv= {...pv, overlayPlotViews};
    }
    else {
        const plots= pv.plots.map( (p) => p.plotImageId===plotImageId ? {...p,dataRequested}: p);
        updatedPv= {...pv,plots};
    }



    const plotViewAry= replacePlotView(state.plotViewAry,updatedPv);
    return {...state, plotViewAry};
}

function updatePlotProgress(state,action) {
    const {plotId, message:plottingStatusMsg, done, requestKey, callSuccess=true, allowBackwardUpdates= false}= action.payload;
    const plotView=  getPlotViewById(state,plotId);

    // validate the update
    if (!plotView) return state;
    if (requestKey!==plotView.request.getRequestKey()) return state;
    if (plotView.plottingStatusMsg===plottingStatusMsg) return state;
    if (!done && plotView.serverCall!=='working' && !allowBackwardUpdates) return state;

    // do the update
    const serverCall= done ? callSuccess ? 'success' : 'fail' : 'working';
    return clone(state,{plotViewAry:clonePvAry(state,plotId, {plottingStatusMsg,serverCall})});
}

function changeVisibility(state,action) {
    const {plotId, visible}= action.payload;
    return {...state,plotViewAry:clonePvAry(state,plotId, {visible})};

}
