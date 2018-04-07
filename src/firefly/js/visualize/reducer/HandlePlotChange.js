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
        applyToOnePvOrGroup,
        matchPlotView,
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
        case Cntlr.GROUP_LOCKING:
            retState= changeGroupLocking(state,action);
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



        case Cntlr.ADD_PROCESSED_TILES:
            retState= addProcessedTileData(state,action);
            break;

        default:
            break;
    }
    return retState;
}

/**
 *
 * @param state
 * @param action
 * @return {*}
 */

function changePlotAttribute(state,action) {
    const {plotId,attKey,attValue,toAll}= action.payload;
    const {plotGroupAry}= state;
    let {plotViewAry}= state;
    const pv= getPlotViewById(state,plotId);
    const plot= primePlot(pv);
    if (!plot) return state;

    const plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);

    plotViewAry= applyToOnePvOrGroup( plotViewAry, plotId, plotGroup, false,
        (pv)=> replaceAtt(pv,{[attKey]:attValue},toAll) );
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
    const {plotViewAry, expandedMode, mpwWcsPrimId}= state;
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


    if (state.wcsMatchType && mpwWcsPrimId!==plotId) {
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
    const {plotViewAry, mpwWcsPrimId}= state;
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

    if (state.wcsMatchType && mpwWcsPrimId!==plotId) {
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
 * @param state
 * @param action
 * @return {*}
 */
function processProjectionChange(state,action) {
    const {plotId,centerProjPt}= action.payload;
    const {plotViewAry}= state;
    const pv= getPlotViewById(state, plotId);
    const {plotGroupAry}= state;

    const plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);

    const newPlotViewAry= applyToOnePvOrGroup( plotViewAry, plotId, plotGroup, false,
         (pv)=> {
             const plot= primePlot(pv);
             if (plot) pv= replacePrimaryPlot(pv, changeProjectionCenter(plot,centerProjPt));
             return pv;
         } );


    // const plot= primePlot(pv);
    // if (!plot) return state;
    // pv= replacePrimaryPlot(pv, changeProjectionCenter(plot,centerProjPt));
    // return clone(state, {plotViewAry : replacePlotView(plotViewAry,pv)});
    return clone(state, {plotViewAry : newPlotViewAry});
}

function changeHiPS(state,action) {
    const {plotId,hipsProperties, hipsUrlRoot, coordSys, cubeIdx, applyToGroup}= action.payload;
    let {centerProjPt}= action.payload;
    let {plotViewAry}= state;

    let pv= getPlotViewById(state, plotId);
    let plot= primePlot(pv);
    if (!plot) return state;

    plot= clone(plot);


    // single plot stuff

    if (hipsUrlRoot) plot.hipsUrlRoot= hipsUrlRoot;
    if (hipsProperties) {
        plot.hipsProperties= hipsProperties;
        plot.title= getHiPsTitleFromProperties(hipsProperties);
        plot.cubeDepth= Number(get(hipsProperties, 'hips_cube_depth')) || 1;
        plot.cubeIdx= Number(get(hipsProperties, 'hips_cube_firstframe')) || 0;
        plot= replaceHiPSProjectionUsingProperties(plot, hipsProperties, getCenterOfProjection(plot) );
    }

    if (!isUndefined(cubeIdx) && plot.cubeDepth>1 && cubeIdx<plot.cubeDepth) {
        plot.cubeIdx= cubeIdx;
    }
    pv= replacePrimaryPlot(pv, plot);
    pv.serverCall= 'success';
    pv.plottingStatus= 'done';
    plotViewAry= replacePlotView(plotViewAry,pv);


    // group plot stuff
    const plotGroup= applyToGroup && findPlotGroup(pv.plotGroupId,state.plotGroupAry);

    if (coordSys) {
        plotViewAry= changeHipsCoordinateSys(plotViewAry, state.plotGroupAry,pv, coordSys, applyToGroup);
        if (!centerProjPt) centerProjPt= convert(getCenterOfProjection(plot), coordSys);
    }

    if (centerProjPt) {
        plotViewAry= applyToOnePvOrGroup(plotViewAry, plot.plotId, plotGroup, false,
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
 * @param plotGroupAry
 * @param {PlotView} pv
 * @param {CoordinateSys} coordSys
 * @param {boolean} applyToGroup
 * @return {Array<PlotView>}
 */
function changeHipsCoordinateSys(plotViewAry, plotGroupAry, pv, coordSys, applyToGroup) {
    const plotGroup= applyToGroup && findPlotGroup(pv.plotGroupId,plotGroupAry);
    return applyToOnePvOrGroup(plotViewAry, pv.plotId, plotGroup, false,
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

function rotatePvToMatch(pv, matchToPv, rotateNorthLock) {
    if (isRotationMatching(pv,matchToPv)) return pv;
    const plot= primePlot(pv);
    const matchToPlot= primePlot(matchToPv);
    if (!plot || !matchToPlot) return pv;
    const masterRot= matchToPv.rotation * (matchToPv.flipY ? -1 : 1);
    let targetRotation= ((getRotationAngle(matchToPlot)+  masterRot)  - (getRotationAngle(plot))) *
        (matchToPv.flipY ? 1 : -1);
    targetRotation= targetRotation ? 360- targetRotation : 0;
    pv= clone(pv);
    pv.plotViewCtx= clone(pv.plotViewCtx, {rotateNorthLock});
    pv.rotation= (360 + targetRotation) % 360;
    return updateTransform(pv);
}


function updateClientRotation(state,action) {
    const {plotId,angle, rotateType, actionScope}= action.payload;
    const {plotGroupAry}= state;
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
    // const realAngle= (360-targetAngle) % 360;

    const plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);
    const masterPv= rotatePv(pv,targetAngle,rotateNorthLock);
    if (state.wcsMatchType || rotateType===RotateType.NORTH) {
        plotViewAry= clonePvAryWithPv(plotViewAry, masterPv);
        if (actionScope===ActionScope.GROUP) {
            plotViewAry= matchPlotView(masterPv,plotViewAry, plotGroup, false, (pv) => rotatePvToMatch(pv,masterPv, rotateNorthLock));
        }
    }
    else {
        plotViewAry= actionScope===ActionScope.GROUP ?
            applyToOnePvOrGroup(plotViewAry,plotId,plotGroup, false, (pv) => rotatePv(pv,targetAngle, rotateNorthLock)) :
            clonePvAryWithPv(plotViewAry, masterPv);
    }

    return clone(state,{plotViewAry});
}


function flipPv(pv, isY) {
    pv= clone(pv);
    if (isY) pv.flipY= !pv.flipY;
    else     pv.flipX= !pv.flipX;
    const cc= CysConverter.make(primePlot(pv));
    const {width,height}= pv.viewDim;
    const centerOfDev= makeDevicePt(width/2,height/2);
    const ipt= cc.getImageCoords(centerOfDev);
    pv= updateTransform(pv);
    const scrollPt= findScrollPtToPlaceOnDevPt(pv,ipt,centerOfDev);
    return updatePlotViewScrollXY(pv,scrollPt);
}


function updateClientFlip(state,action) {
    const {plotId,isY,actionScope=ActionScope.GROUP }= action.payload;
    const {plotGroupAry}= state;
    let   {plotViewAry}= state;

    const pv= getPlotViewById(state,plotId);
    if (!pv) return state;
    const plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);

    plotViewAry= actionScope===ActionScope.GROUP ?
        applyToOnePvOrGroup(plotViewAry,plotId,plotGroup, false, (pv) => flipPv(pv,isY)) :
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

        if (isHiPS(plot)) {
            centerImagePt= makeImagePt( plot.dataWidth/2, plot.dataHeight/2);
            pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
        }
        else if (state.wcsMatchType===WcsMatchType.Standard && state.mpwWcsPrimId!==plotId) {
            pv= updateScrollToWcsMatch(state.wcsMatchType, masterPv, pv);
        }
        else if (state.wcsMatchType===WcsMatchType.Target) {
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



function recenter(state,action) {
    const {plotId, centerPt, centerOnImage }= action.payload;
    const {plotGroupAry}= state;
    let   {plotViewAry}= state;
    const pv= getPlotViewById(state,plotId);
    const plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);

    if (state.wcsMatchType) {
        const newPv= recenterPv(centerPt, centerOnImage)(pv);
        plotViewAry= replacePlotView(plotViewAry, newPv);
        plotViewAry= matchPlotView(newPv, plotViewAry,plotGroup, false, (pv) => {
            return updateScrollToWcsMatch(state.wcsMatchType, newPv, pv);
        } );
    }
    else {
        plotViewAry= applyToOnePvOrGroup(plotViewAry,plotId,plotGroup,true, recenterPv(centerPt, centerOnImage));
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
            const wp = plot.attributes[PlotAttribute.FIXED_TARGET];
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

function changeGroupLocking(state,action) {
    const {plotId,groupLocked}=  action.payload;
    const {plotGroupId} = getPlotViewById(state,plotId);

    const pgIdx= getPlotGroupIdxById(state,plotGroupId);

    if (pgIdx < 0) return state;
    state= updateSet(state, ['plotGroupAry',pgIdx,'lockRelated'], groupLocked);

    if (groupLocked && isHiPS(primePlot(state,plotId))) {
        const pv= getPlotViewById(state, plotId);
        const plot= primePlot(pv);
        const plotViewAry= changeHipsCoordinateSys(state.plotViewAry, state.plotGroupAry, pv,plot.imageCoordSys,true );
        state= clone(state, {plotViewAry});
        state= processProjectionChange(state,
            clone(action, {payload:{plotId, centerProjPt:getCenterOfProjection(plot) }}));
    }

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
