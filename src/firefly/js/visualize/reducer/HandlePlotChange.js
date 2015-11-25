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
            break;
        case Cntlr.ZOOM_IMAGE  :
            plotViewAry= installZoomTiles(state.plotViewAry,action);
            // todo: also process adding to history
            break;
        case Cntlr.UPDATE_VIEW_SIZE :
            retState= updateViewSize(state,action);
            break;
        case Cntlr.PROCESS_SCROLL  :
            retState= processScroll(state,action);
            break;
        default:
            break;
    }
    if (plotViewAry) {
        retState= Object.assign({},state, {plotViewAry});
    }
    return retState;
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

function installZoomTiles(plotViewAry, action) {
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



