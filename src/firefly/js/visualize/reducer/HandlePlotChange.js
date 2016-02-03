/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Cntlr, {ExpandType} from '../ImagePlotCntlr.js';
import PlotView from './PlotView.js';
import WebPlot from '../WebPlot.js';
import PlotViewUtil from '../PlotViewUtil.js';
import {makeImagePt} from '../Point.js';
import {UserZoomTypes} from '../ZoomUtil.js';


//============ EXPORTS ===========
//============ EXPORTS ===========

export default {
    reducer
};


function reducer(state, action) {

    var retState= state;
    var plotViewAry;
    //var plotGroupAry;
    //var plotRequestDefaults;
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
            plotViewAry= installTiles(state.plotViewAry,action);
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
        default:
            break;
    }
    if (plotViewAry) {
        retState= Object.assign({},state, {plotViewAry});
    }
    return retState;
}


const isFitFill= (userZoomType) =>  (userZoomType===UserZoomTypes.FIT || userZoomType===UserZoomTypes.FILL);
const clone = (obj,params={}) => Object.assign({},obj,params);


const replaceAtt = (pv,key,val)=> PlotView.replacePrimary(pv,
                                                WebPlot.addWPAttribute(pv.primaryPlot,key,val));

function changePlotAttribute(state,action) {
    var {plotId,attKey,attValue}= action.payload;
    var {plotViewAry,plotGroupAry}= state;
    var pv= PlotViewUtil.findPlotView(plotId,plotViewAry);
    var plotGroup= PlotViewUtil.findPlotGroup(pv.plotGroupId,plotGroupAry);
    if (pv && pv.primaryPlot) {
        plotViewAry=  PlotView.replacePlotView(plotViewAry,replaceAtt(pv,attKey,attValue));
        plotViewAry=  PlotViewUtil.matchPlotView(pv,plotViewAry,plotGroup, (pv)=> replaceAtt(pv,attKey,attValue));
        return clone(state,{plotViewAry});
    }
    return state;
}

function changeLocking(state,action) {
    var {plotId, zoomLockingEnabled, zoomLockingType}=  action.payload;
    var {plotViewAry}= state;
    plotViewAry= plotViewAry.map( (pv) => {
        if (pv.plotId!==plotId) return pv;
        pv= clone(pv);
        pv.plotViewCtx= clone(pv.plotViewCtx,{zoomLockingEnabled,zoomLockingType});
        return pv;
    });
    return clone(state,{plotViewAry});
}


function zoomStart(state, action) {
    var {plotViewAry, expandedMode}= state;
    const {plotId, zoomLevel, userZoomType, zoomLockingEnabled}= action.payload;
    var pv=PlotViewUtil.findPlotView(plotId,plotViewAry);
    var plot= pv ? pv.primaryPlot : null;
    if (!plot) return plotViewAry;
    var originalZF= plot.zoomFactor;

    // update zoom factor and scroll position

    var centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
    pv= PlotView.replacePrimary(pv,WebPlot.setZoomFactor(plot,zoomLevel));
    pv= PlotView.updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));
    pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv) =>
                     clone(oPv, {plot:WebPlot.setZoomFactor(oPv.plot,zoomLevel)}) );

   // up date book keeping

    pv.plotViewCtx= clone(pv.plotViewCtx,{zoomLockingEnabled});
    if (expandedMode===ExpandType.COLLAPSE) {
        pv.plotViewCtx.lastCollapsedZoomLevel= originalZF;
    }
    if (zoomLockingEnabled && isFitFill(userZoomType)) {
        pv.plotViewCtx.zoomLockingType= userZoomType;
    }

    // return new state
    plotViewAry= PlotView.replacePlotView(plotViewAry,pv);
    return clone(state,{plotViewAry});
}

function installTiles(plotViewAry, action) {
    const {plotId, primaryStateJson,primaryTiles,overlayStateJsonAry,overlayTilesAry }= action.payload;
    var pv=PlotViewUtil.findPlotView(plotId,plotViewAry);
    var plot= pv ? pv.primaryPlot : null;
    if (!plot) return plotViewAry;

    var centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
    pv= PlotView.replacePrimary(pv,WebPlot.setPlotState(plot,primaryStateJson,primaryTiles));
    pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv,idx) => {
        var p= WebPlot.setPlotState(oPv.plot,overlayStateJsonAry[idx],overlayTilesAry[idx]);
        return clone(oPv, {plot:p});
    });
    pv= PlotView.updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));
    return PlotView.replacePlotView(plotViewAry,pv);
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
        if (pv.primaryPlot) {
            if (pv.scrollX<0 || pv.scrollY<0) {
                centerImagePt= makeImagePt(pv.primaryPlot.dataWidth/2, pv.primaryPlot.dataHeight/2);
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



