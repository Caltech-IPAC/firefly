/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'react-addons-update';
import {isEmpty,isUndefined} from 'lodash';
import Cntlr, {ExpandType} from '../ImagePlotCntlr.js';
import PlotView, {replacePlotView, replacePrimaryPlot, changePrimePlot,
                  findWCSMatchOffset, updatePlotViewScrollXY} from './PlotView.js';
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
        isInSameGroup,
        getPlotViewById} from '../PlotViewUtil.js';
import {makeImagePt, makeWorldPt, makeScreenPt} from '../Point.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import Point from '../Point.js';


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

/**
 *
 * @param state
 * @param action
 * @return {*}
 */

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
    const zt= UserZoomTypes.get(zoomLockingType);
    return clone(state,{plotViewAry: state.plotViewAry.map( (pv) =>
            (pv.plotId===plotId) ?
                update(pv, {plotViewCtx : {$merge :{zoomLockingEnabled,zoomLockingType:zt} }} ) : pv
        )});
}


function zoomStart(state, action) {
    var {plotViewAry, expandedMode, mpwWcsPrimId}= state;
    const {plotId, zoomLevel, userZoomType, zoomLockingEnabled}= action.payload;
    var pvIdx=getPlotViewIdxById(state,plotId);
    var plot= pvIdx>-1 ? primePlot(plotViewAry[pvIdx]) : null;
    if (!plot) return state;
    var pv= plotViewAry[pvIdx];

    // up date book keeping
    var newCtx= {zoomLockingEnabled};
    if (expandedMode===ExpandType.COLLAPSE) newCtx.lastCollapsedZoomLevel= zoomLevel;
    if (zoomLockingEnabled && isFitFill(userZoomType))  newCtx.zoomLockingType= userZoomType;


    // update zoom factor and scroll position
    var centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
    pv= replacePrimaryPlot(pv,clonePlotWithZoom(plot,zoomLevel));


    if (state.wcsMatchType && mpwWcsPrimId!==plotId) {
        const masterPV= getPlotViewById(state, mpwWcsPrimId);
        const {scrollX,scrollY}= masterPV;
        const offPt= findWCSMatchOffset(state, mpwWcsPrimId, primePlot(pv));
        pv= updatePlotViewScrollXY(pv, makeScreenPt(scrollX-offPt.x,scrollY-offPt.y), false);
    }
    else {
        pv= updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));
    }


    pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv) =>
                     clone(oPv, {plot:clonePlotWithZoom(oPv.plot,zoomLevel)}) );


    // return new state
    return update(state, { plotViewAry : {
                  [pvIdx]: {$set: update(pv, {plotViewCtx: {$merge: newCtx}
        }) } }});
}

function installTiles(state, action) {
    var {plotViewAry, mpwWcsPrimId}= state;
    const {plotId, primaryStateJson,primaryTiles,overlayStateJsonAry,overlayTilesAry }= action.payload;
    var pv= getPlotViewById(state,plotId);
    var plot= primePlot(pv);

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
        const offPt= findWCSMatchOffset(state, mpwWcsPrimId, primePlot(pv));
        pv= updatePlotViewScrollXY(pv, makeScreenPt(scrollX-offPt.x,scrollY-offPt.y), false);
    }
    else {
        var centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
        pv= updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));
    }


    if (!isEmpty(overlayStateJsonAry) && overlayStateJsonAry.length===pv.overlayPlotViews.length) {
        pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv,idx) => {
            var p= WebPlot.setPlotState(oPv.plot,overlayStateJsonAry[idx],overlayTilesAry[idx]);
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
    var {plotViewAry, plotGroupAry, wcsMatchType, mpwWcsPrimId}= state;
    plotViewAry= PlotView.updatePlotGroupScrollXY(state,plotId,plotViewAry, plotGroupAry,scrollPt);

    if (wcsMatchType && isInSameGroup(state, plotId, mpwWcsPrimId)) {
        mpwWcsPrimId= plotId;
    }
    return Object.assign({},state,{plotViewAry, mpwWcsPrimId});
}

function updateViewSize(state,action) {
    const {plotId,width,height}= action.payload;


    var plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!==plotId ) return pv;
        var plot= primePlot(pv);

        const w= isUndefined(width) ? pv.viewDim.width : width;
        const h= isUndefined(height) ? pv.viewDim.height : height;

        if (plot) {
            var centerImagePt;
            if (pv.scrollX<0 || pv.scrollY<0) {
                centerImagePt= makeImagePt(plot.dataWidth/2, plot.dataHeight/2);
            }
            else {
                centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
            }
        }
        pv= Object.assign({}, pv, {viewDim: {width:w, height:h}});
        if (plot && state.wcsMatchType && state.mpwWcsPrimId!==plotId) {
            const offPt= findWCSMatchOffset(state, state.mpwWcsPrimId, plotId);
            const masterPv=getPlotViewById(state,state.mpwWcsPrimId);
            pv= updatePlotViewScrollXY(pv, makeScreenPt(masterPv.scrollX-offPt.x, masterPv.scrollY-offPt.y), false);
        }
        else if (centerImagePt) {
            pv= updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));
        }
        return pv;

    });
    return Object.assign({},state,{plotViewAry});
}


// function alignWCS(state,action) {
//     if (state.wcsMatchType && state.mpwWcsPrimId!==plotId) {
//         const offPt= findWCSMatchOffset(state, state.mpwWcsPrimId, plotId);
//         const masterPv=getPlotViewById(state,state.mpwWcsPrimId);
//         pv= updatePlotViewScrollXY(pv, makeScreenPt(masterPv.scrollX-offPt.x, masterPv.scrollY-offPt.y), false);
//     }
//
// }




function recenter(state,action) {
    const {plotId, centerPt}= action.payload;
    var {plotGroupAry,plotViewAry, wcsMatchCenterWP}= state;
    const pv= getPlotViewById(state,plotId);
    var plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);

    plotViewAry= applyToOnePvOrGroup(plotViewAry,plotId,plotGroup, recenterPv(centerPt, wcsMatchCenterWP));
    return clone(state,{plotViewAry});
}

/**
 * Center on the FIXED_TARGET attribute or the center of the plot or specified center point
 * @param centerPt center point
 * @param wcsMatchCenterWP wcs match point if it exist
 * @return {{}} a new plot view
 */

function recenterPv(centerPt, wcsMatchCenterWP) {
    return (pv) => {
        const plot = primePlot(pv);
        if (!plot) return pv;
        var centerImagePt;

        if (centerPt) {
            if (centerPt.type === Point.IM_PT) {
                centerImagePt = makeImagePt(centerPt.x, centerPt.y);
            } else {
                centerImagePt = makeWorldPt(centerPt.x, centerPt.y);
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
        return updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv, centerImagePt));
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
