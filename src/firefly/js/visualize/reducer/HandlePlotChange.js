/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'react-addons-update';
import {isEmpty,isUndefined} from 'lodash';
import Cntlr, {ExpandType, WcsMatchType, ActionScope} from '../ImagePlotCntlr.js';
import {replacePlotView, replacePrimaryPlot, changePrimePlot,
        findWCSMatchScrollPosition,updatePlotViewScrollXY,
        findCurrentCenterPoint, findScrollPtToCenterImagePt, findScrollPtToPlaceOnDevPt,
       updatePlotGroupScrollXY} from './PlotView.js';
import {WebPlot, clonePlotWithZoom, PlotAttribute} from '../WebPlot.js';
import {logError, updateSet} from '../../util/WebUtil.js';
import {CCUtil, CysConverter} from '../CsysConverter.js';
import {isPlotNorth, getRotationAngle} from '../VisUtil.js';
import {PlotPref} from './../PlotPref.js';
import {primePlot,
        clonePvAry,
        clonePvAryWithPv,
        applyToOnePvOrGroup,
        getPlotViewIdxById,
        getPlotGroupIdxById,
        findPlotGroup,
        isInSameGroup,
        getPlotViewById} from '../PlotViewUtil.js';
import {makeImagePt, makeWorldPt, makeScreenPt, makeDevicePt} from '../Point.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import {RotateType} from '../PlotState.js';
import Point from '../Point.js';
import {updateTransform} from '../PlotPostionUtil.js';


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
        case Cntlr.RECENTER:
            retState= recenter(state,action);
            break;
        case Cntlr.ROTATE_CLIENT:
            retState= updateClientRotation(state,action);
            break;
        case Cntlr.FLIP_CLIENT:
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

    plotViewAry= applyToOnePvOrGroup( plotViewAry, plotId, plotGroup,
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
    const centerImagePt= findCurrentCenterPoint(pv);
    pv= replacePrimaryPlot(pv,clonePlotWithZoom(plot,zoomLevel));


    if (state.wcsMatchType && mpwWcsPrimId!==plotId) {
        const masterPV= getPlotViewById(state, mpwWcsPrimId);
        const {scrollX,scrollY}= masterPV;
        const newSp= findWCSMatchScrollPosition(state, mpwWcsPrimId, primePlot(pv), makeScreenPt(scrollX,scrollY));
        pv= updatePlotViewScrollXY(pv, newSp);
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
    const {plotId, primaryStateJson,primaryTiles,overlayStateJsonAry,overlayTilesAry }= action.payload;
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
        const {scrollX,scrollY}= masterPV;
        const newSp= findWCSMatchScrollPosition(state, mpwWcsPrimId, primePlot(pv), makeScreenPt(scrollX,scrollY));
        pv= updatePlotViewScrollXY(pv, newSp);
    }
    else {
        const centerImagePt= findCurrentCenterPoint(pv);
        pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
    }


    if (!isEmpty(overlayStateJsonAry) && overlayStateJsonAry.length===pv.overlayPlotViews.length) {
        pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv,idx) => {
            const p= WebPlot.setPlotState(oPv.plot,overlayStateJsonAry[idx],overlayTilesAry[idx]);
            return clone(oPv, {plot:p});
        });
    }

    plot= primePlot(pv); // get the updated on
    PlotPref.putCacheColorPref(pv.plotViewCtx.preferenceColorKey, plot.plotState);
    PlotPref.putCacheZoomPref(pv.plotViewCtx.preferenceZoomKey, plot.plotState);

    return clone(state, {plotViewAry : replacePlotView(plotViewAry,pv)});
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


function updateClientRotation(state,action) {
    const {plotId,angle, rotateType, actionScope}= action.payload;
    const {plotGroupAry}= state;
    let   {plotViewAry}= state;

    const plot= primePlot(state,plotId);
    if (!plot) return state;
    if (rotateType===RotateType.NORTH && isPlotNorth(plot)) return state;

    let targetAngle;
    let rotateNorthLock;

    switch (rotateType) {
        case RotateType.NORTH:
            targetAngle= 360 - getRotationAngle(plot);
            rotateNorthLock= true;
            break;
        case RotateType.ANGLE:
            targetAngle= angle;
            rotateNorthLock= false;
            break;
        case RotateType.UNROTATE:
            targetAngle= 0;
            rotateNorthLock= false;
            break;
    }

    targetAngle= (360 + targetAngle) % 360;
    // const realAngle= (360-targetAngle) % 360;

    const pv= getPlotViewById(state,plotId);
    const plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);

    plotViewAry= actionScope===ActionScope.GROUP ?
               applyToOnePvOrGroup(plotViewAry,plotId,plotGroup, (pv) => rotatePv(pv,targetAngle, rotateNorthLock)) :
               clonePvAryWithPv(plotViewAry, rotatePv(pv,targetAngle,rotateNorthLock));

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
    const scrollPt= findScrollPtToPlaceOnDevPt(pv,ipt,centerOfDev)
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
        applyToOnePvOrGroup(plotViewAry,plotId,plotGroup, (pv) => flipPv(pv,isY)) :
        clonePvAryWithPv(plotViewAry, flipPv(pv,isY));

    return clone(state,{plotViewAry});
}


function updateViewSize(state,action) {
    const {plotId,width,height}= action.payload;


    const plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!==plotId ) return pv;
        const plot= primePlot(pv);


        const w= isUndefined(width) ? pv.viewDim.width : width;
        const h= isUndefined(height) ? pv.viewDim.height : height;
        pv= Object.assign({}, pv, {viewDim: {width:w, height:h}});
        if (!plot) return pv;

        let masterPv;

        if (state.wcsMatchType===WcsMatchType.Standard && state.mpwWcsPrimId!==plotId) {
            masterPv= getPlotViewById(state, state.mpwWcsPrimId);
            if (masterPv) {
                const {scrollX,scrollY}= masterPv;
                const newSp= findWCSMatchScrollPosition(state, state.mpwWcsPrimId, plotId, makeScreenPt(scrollX,scrollY));
                pv= updatePlotViewScrollXY(pv, newSp);
            }
        }
        else if (state.wcsMatchType===WcsMatchType.Target) {
            const newSp= findWCSMatchScrollPosition(state, state.mpwWcsPrimId, plotId, makeScreenPt(scrollX,scrollY));
            pv= updatePlotViewScrollXY(pv, newSp);
        }
        else {
            const centerImagePt= (pv.scrollX<0 || pv.scrollY<0) ?
                                 makeImagePt(plot.dataWidth/2, plot.dataHeight/2) :
                                 findCurrentCenterPoint(pv);
            pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
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
        plotViewAry= updatePlotGroupScrollXY(state, plotId, plotViewAry, state.plotGroupAry,
                                                       makeScreenPt(newPv.scrollX, newPv.scrollY));
    }
    else {
        plotViewAry= applyToOnePvOrGroup(plotViewAry,plotId,plotGroup, recenterPv(centerPt, centerOnImage));
    }

    return clone(state,{plotViewAry});
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
        return updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv, centerImagePt));
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
    return updateSet(state, ['plotGroupAry',pgIdx,'lockRelated'], groupLocked);

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

function updatePlotProgress(state,action) {
    const {plotId, message:plottingStatus, done, requestKey}= action.payload;
    //console.log(`updatePlotProgress: plotId;${plotId}, message:${plottingStatus}, done:${done}`);
    if (!plotId) return state;
    const plotView=  getPlotViewById(state,plotId);
    if (!plotView) return state;
    if (requestKey!==plotView.request.getRequestKey()) return state;
    if (plotView.plottingStatus===plottingStatus) return state;
    const changes= {plottingStatus,serverCall:done ? 'success': 'working'};
    return clone(state,{plotViewAry:clonePvAry(state,plotId, changes)});
}
