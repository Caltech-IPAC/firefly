/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'react-addons-update';
import {isEmpty} from 'lodash';
import Cntlr, {ExpandType} from '../ImagePlotCntlr.js';
import PlotView, {replacePlotView, replacePrimaryPlot, changePrimePlot} from './PlotView.js';
import {WebPlot, clonePlotWithZoom, PlotAttribute} from '../WebPlot.js';
import {logError, updateSet} from '../../util/WebUtil.js';
import {CCUtil} from '../CsysConverter.js';
import {PlotPref} from './../PlotPref.js';
import {primePlot,
        clonePvAry,
        clonePvAryWithPv,
        applyToOnePvOrGroup,
        getPlotViewIdxById,
        getPlotGroupIdxById,
        findPlotGroup,
        getPlotViewById} from '../PlotViewUtil.js';
import {makeImagePt, makeWorldPt} from '../Point.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import CsysConverter from '../CsysConverter.js'


//============ EXPORTS ===========
//============ EXPORTS ===========

const isFitFill= (userZoomType) =>  (userZoomType===UserZoomTypes.FIT || userZoomType===UserZoomTypes.FILL);
const clone = (obj,params={}) => Object.assign({},obj,params);

/**
 * 
 * @param pv
 * @param att
 * @param toAll
 * @return {*} new plotview objject
 */
function replaceAtt(pv,att, toAll) {
    if (toAll) {
        const plots= pv.plots.map( (p) => clone(p,{attributes:clone(p.attributes, att)}));
        return clone(pv,{plots});
    }
    else {
        var p= primePlot(pv);
        return replacePrimaryPlot(pv,clone(p,{attributes:clone(p.attributes, att)}));
    }
}


export function reducer(state, action) {

    var retState= state;
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


function changePlotAttribute(state,action) {
    var {plotId,attKey,attValue,toAll}= action.payload;
    var {plotViewAry,plotGroupAry}= state;
    var pv= getPlotViewById(state,plotId);
    var plot= primePlot(pv);
    if (!plot) return state;

    var plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);

    plotViewAry= applyToOnePvOrGroup( plotViewAry, plotId, plotGroup,
        (pv)=> replaceAtt(pv,{[attKey]:attValue},toAll) );
    return clone(state,{plotViewAry});
}



function changeLocking(state,action) {
    var {plotId, zoomLockingEnabled, zoomLockingType}=  action.payload;
    return clone(state,{plotViewAry: state.plotViewAry.map( (pv) =>
            (pv.plotId===plotId) ?
                update(pv, {plotViewCtx : {$merge :{zoomLockingEnabled,zoomLockingType} }} ) : pv
        )});
}


function zoomStart(state, action) {
    var {plotViewAry, expandedMode}= state;
    const {plotId, zoomLevel, userZoomType, zoomLockingEnabled}= action.payload;
    var pvIdx=getPlotViewIdxById(state,plotId);
    var plot= pvIdx>-1 ? primePlot(plotViewAry[pvIdx]) : null;
    if (!plot) return plotViewAry;
    var pv= plotViewAry[pvIdx];

    // up date book keeping
    var newCtx= {zoomLockingEnabled};
    if (expandedMode===ExpandType.COLLAPSE) newCtx.lastCollapsedZoomLevel= zoomLevel;
    if (zoomLockingEnabled && isFitFill(userZoomType))  newCtx.zoomLockingType= userZoomType;


    // update zoom factor and scroll position
    var centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
    pv= replacePrimaryPlot(pv,clonePlotWithZoom(plot,zoomLevel));
    pv= PlotView.updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));
    pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv) =>
                     clone(oPv, {plot:clonePlotWithZoom(oPv.plot,zoomLevel)}) );


    // return new state
    return update(state, { plotViewAry : {
                  [pvIdx]: {$set: update(pv, {plotViewCtx: {$merge: newCtx}
        }) } }});
}

function installTiles(state, action) {
    var {plotViewAry}= state;
    const {plotId, primaryStateJson,primaryTiles,overlayStateJsonAry,overlayTilesAry }= action.payload;
    var pv= getPlotViewById(state,plotId);
    var plot= primePlot(pv);

    if (!plot || !primaryStateJson) {
        logError('primePlot undefined or primaryStateJson is not set.', new Error());
        console.log('installTiles: state, action', state, action);
        return state;
    }

    var centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
    pv= replacePrimaryPlot(pv,WebPlot.setPlotState(plot,primaryStateJson,primaryTiles));
    pv.serverCall='success';
    pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv,idx) => {
        var p= WebPlot.setPlotState(oPv.plot,overlayStateJsonAry[idx],overlayTilesAry[idx]);
        return clone(oPv, {plot:p});
    });
    pv= PlotView.updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));

    plot= primePlot(pv); // get the updated on
    PlotPref.putCacheColorPref(pv.plotViewCtx.preferenceColorKey, plot.plotState);
    PlotPref.putCacheZoomPref(pv.plotViewCtx.preferenceZoomKey, plot.plotState);

    return clone(state, {plotViewAry : replacePlotView(plotViewAry,pv)});
}


function processScroll(state,action) {
    const {plotId,scrollPt}= action.payload;
    var plotViewAry= PlotView.updatePlotGroupScrollXY(plotId,state.plotViewAry,state.plotGroupAry,scrollPt);
    return Object.assign({},state,{plotViewAry});
}

function updateViewSize(state,action) {
    const {plotId,width,height}= action.payload;

    var plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!==plotId ) return pv;
        var centerImagePt;
        var plot= primePlot(pv);
        if (plot) {
            if (pv.scrollX<0 || pv.scrollY<0) {
                centerImagePt= makeImagePt(plot.dataWidth/2, plot.dataHeight/2);
            }
            else {
                centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
            }
        }
        pv= Object.assign({}, pv, {viewDim: {width, height}});
        if (centerImagePt) {
            pv= PlotView.updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));
        }
        return pv;

    });
    return Object.assign({},state,{plotViewAry});
}

function recenter(state,action) {
    const {plotId, centerPt}= action.payload;
    var {plotGroupAry,plotViewAry}= state;
    const pv= getPlotViewById(state,plotId);
    var plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);

    plotViewAry= applyToOnePvOrGroup(plotViewAry,plotId,plotGroup, recenterPv(centerPt));
    return clone(state,{plotViewAry});
}

/**
 * Center on the FIXED_TARGET attribute or the center of the plot or specified center point
 * @param centerPt center point
 * @return {{}} a new plot view
 */

function recenterPv(centerPt) {
    return (pv) => {
        const plot = primePlot(pv);
        if (!plot) return pv;
        var centerImagePt;

        if (centerPt) {
            if (centerPt.type === Point.IM_PT) {
                centerImagePt = makeImagePt(centerPt.x, centerPt.y);
            } else {
                centerImagePt = makeWorldPt(centetPt.x, centerPt.y);
            }
        } else {
            var wp = plot.attributes[PlotAttribute.FIXED_TARGET];
            if (wp) {
                centerImagePt = CCUtil.getImageCoords(plot, wp);
            }
            else {
                centerImagePt = makeImagePt(plot.dataWidth / 2, plot.dataHeight / 2);
            }
        }
        return PlotView.updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv, centerImagePt));
    };
}

function makeNewPrimePlot(state,action) {
    var {plotId,primeIdx}= action.payload;
    var pv=  getPlotViewById(state,plotId);
    if (!pv || isEmpty(pv.plots) || pv.plots.length<=primeIdx) return state;
    pv= changePrimePlot(pv, primeIdx);
    return clone(state,{plotViewAry:clonePvAryWithPv(state,pv)});
}

function changeGroupLocking(state,action) {
    var {plotId,groupLocked}=  action.payload;
    const {plotGroupId} = getPlotViewById(state,plotId);

    const pgIdx= getPlotGroupIdxById(state,plotGroupId);

    if (pgIdx < 0) return state;
    return updateSet(state, ['plotGroupAry',pgIdx,'lockRelated'], groupLocked);

}

function endServerCallFail(state,action) {
    var {plotId,message}= action.payload;
    const stat= {serverCall:'fail'};
    if (typeof message === 'string') stat.plottingStatus= message;
    return clone(state,{plotViewAry:clonePvAry(state,plotId, stat)});
}
function workingServerCall(state,action) {
    var {plotId,message}= action.payload;
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
    const {plotId, message:plottingStatus, done}= action.payload;
    //console.log(`updatePlotProgress: plotId;${plotId}, message:${plottingStatus}, done:${done}`);
    if (!plotId) return state;
    const plotView=  getPlotViewById(state,plotId);
    if (!plotView) return state;
    if (plotView.plottingStatus===plottingStatus) return state;
    const changes= {plottingStatus,serverCall:done ? 'success': 'working'};
    return clone(state,{plotViewAry:clonePvAry(state,plotId, changes)});
}
