/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Cntlr from '../ImagePlotCntlr.js';
import PlotView from './PlotView.js';
import WebPlot from '../WebPlot.js';
import PlotViewUtil from '../PlotViewUtil.js';


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
            plotViewAry= scaleImage(state.plotViewAry, action);
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
        default:
            break;
    }
    if (plotViewAry) {
        retState= Object.assign({},state, {plotViewAry});
    }
    return retState;
}




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
        return Object.assign({},state,{plotViewAry});
    }
    return state;
}


function scaleImage(plotViewAry, action) {
    const {plotId, zoomLevel}= action.payload;
    var pv=PlotViewUtil.findPlotView(plotId,plotViewAry);
    var plot= pv ? pv.primaryPlot : null;
    if (!plot) return plotViewAry;

    var centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
    pv= PlotView.replacePrimary(pv,WebPlot.setZoomFactor(plot,zoomLevel));
    pv= PlotView.updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));
    pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv) => {
        var p= WebPlot.setZoomFactor(oPv.plot,zoomLevel);
        return Object.assign({},oPv, {plot:p});
    });
    return PlotView.replacePlotView(plotViewAry,pv);
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
        return Object.assign({},oPv, {plot:p});
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
        return pv.plotId===plotId ? PlotView.updateViewDim(pv,{width, height}) : pv;
    });
    return Object.assign({},state,{plotViewAry});
}



