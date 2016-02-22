/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import update from 'react-addons-update';
import Cntlr, {ExpandType} from '../ImagePlotCntlr.js';
import PlotView, {replacePlotView, replacePrimaryPlot} from './PlotView.js';
import WebPlot, {clonePlotWithZoom} from '../WebPlot.js';
import PlotViewUtil, {
    primePlot,
    applyToOnePvOrGroup,
    getPlotViewIdxById,
    getPlotViewById} from '../PlotViewUtil.js';
import {makeImagePt} from '../Point.js';
import {UserZoomTypes} from '../ZoomUtil.js';


//============ EXPORTS ===========
//============ EXPORTS ===========

const isFitFill= (userZoomType) =>  (userZoomType===UserZoomTypes.FIT || userZoomType===UserZoomTypes.FILL);
const clone = (obj,params={}) => Object.assign({},obj,params);

function replaceAtt(pv,att) {
    var p= primePlot(pv);
    return replacePrimaryPlot(pv,clone(p,{attributes:clone(p.attributes, att)}));
}


export function reducer(state, action) {

    var retState= state;
    switch (action.type) {
        case Cntlr.ZOOM_IMAGE_START  :
            retState= zoomStart(state, action);
            break;
        case Cntlr.ZOOM_IMAGE_FAIL  :
        case Cntlr.STRETCH_CHANGE_FAIL:
        case Cntlr.COLOR_CHANGE_FAIL:
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
        case Cntlr.CHANGE_PLOT_ATTRIBUTE :
            retState= changePlotAttribute(state,action);
            break;
        case Cntlr.ZOOM_LOCKING:
            retState= changeLocking(state,action);
            break;
        case Cntlr.EXPANDED_LIST:
            retState= updateExpandedList(state,action);
            break;
        default:
            break;
    }
    return retState;
}




function updateExpandedList(state,action) {
    var {plotIdAry}= action.payload;
    var {plotViewAry}= state;
    var newState= clone(state,
        {plotViewAry: plotViewAry.map( (pv) =>
            update(pv, {plotViewCtx : {inExpandedList :{$set :  plotIdAry.includes(pv.plotId)} }} ))
    });
    if (!plotIdAry.includes(state.activePlotId)) {
        newState.activePlotId= plotIdAry[0];
    }

    return newState;
}


function changePlotAttribute(state,action) {
    var {plotId,attKey,attValue}= action.payload;
    var {plotViewAry,plotGroupAry}= state;
    var pv= getPlotViewById(state,plotId);
    var plot= primePlot(pv);
    if (!plot) return state;

    var plotGroup= PlotViewUtil.findPlotGroup(pv.plotGroupId,plotGroupAry);

    plotViewAry=  applyToOnePvOrGroup( plotViewAry, plotId, plotGroup,
        (pv)=> replaceAtt(pv,{[attKey]:attValue}) );
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
    if (!plot) return plotViewAry;

    var centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
    pv= replacePrimaryPlot(pv,WebPlot.setPlotState(plot,primaryStateJson,primaryTiles));
    pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv,idx) => {
        var p= WebPlot.setPlotState(oPv.plot,overlayStateJsonAry[idx],overlayTilesAry[idx]);
        return clone(oPv, {plot:p});
    });
    pv= PlotView.updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));

    return clone(state, {plotViewAry : replacePlotView(plotViewAry,pv)});
}


function processScroll(state,action) {
    const {plotId,scrollScreenPt}= action.payload;
    var plotViewAry= PlotView.updatePlotGroupScrollXY(plotId,state.plotViewAry,state.plotGroupAry,scrollScreenPt);
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



