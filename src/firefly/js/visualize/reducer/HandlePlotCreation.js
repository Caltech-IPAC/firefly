/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Cntlr from '../ImagePlotCntlr.js';
import PlotView from './PlotView.js';
import WebPlot from '../WebPlot.js';
import PlotGroup from '../PlotGroup.js';
import PlotViewUtil from '../PlotViewUtil.js';


//============ EXPORTS ===========
//============ EXPORTS ===========

export default {
    reducer
};




function reducer(state, action) {

    var retState= state;
    var plotViewAry;
    var plotGroupAry;
    var plotRequestDefaults;
    var activePlotId= state.activePlotId;
    switch (action.type) {
        case Cntlr.PLOT_IMAGE_START  :
            plotRequestDefaults= updateDefaults(state.plotRequestDefaults,action);
            plotGroupAry= confirmPlotGroup(state.plotGroupAry,action);
            plotViewAry= confirmPlotView(state.plotViewAry,action);
            break;
        case Cntlr.PLOT_IMAGE_FAIL  :
            break;
        case Cntlr.PLOT_IMAGE  :
            plotViewAry= addPlot(state.plotViewAry,action);
            activePlotId= action.payload.plotId;
            // todo: also process adding to history
            break;
        default:
            break;
    }
    if (plotGroupAry || plotViewAry || plotRequestDefaults) {
        retState= Object.assign({},state, {activePlotId});
        if (plotViewAry) retState.plotViewAry= plotViewAry;
        if (plotGroupAry) retState.plotGroupAry= plotGroupAry;
        if (plotRequestDefaults) retState.plotRequestDefaults= plotRequestDefaults;
    }
    return retState;
}



const updateDefaults= function(plotRequestDefaults, action) {
    var retDef;
    var {plotId,wpRequest,redReq,greenReq, blueReq,threeColor}= action.payload;
    if (threeColor) {
        retDef= Object.assign({}, plotRequestDefaults, {[plotId]:{threeColor,redReq,greenReq, blueReq}});
    }
    else {
        retDef= Object.assign({}, plotRequestDefaults, {[plotId]:{threeColor,wpRequest}});
    }
    return retDef;
};

const addPlot= function(plotViewAry,action) {
    const {plotId, plotAry}= action.payload;
    return plotViewAry.map( (pv) => {
        return pv.plotId===plotId ? PlotView.replacePlots(pv,plotAry) : pv;
    });
};

/**
 /**
 *
 * @param plotViewAry
 * @param action
 * @return {[]|null} new PlotViewAry or null it nothing is created.
 */
function confirmPlotView(plotViewAry,action) {
    const {plotId}= action.payload;
    if (pvExist(plotId,plotViewAry)) return null;

    const payload= action.payload;
    var rKey= ['wpRequest','redReq','blueReq','greenReq'].find( (key) => payload[key] ? true : false);
    var pv= PlotView.makePlotView(plotId, payload[rKey] );
    return [...plotViewAry,pv];
}

/**
 *
 * @param plotGroupAry
 * @param action
 * @return {[]|null} new PlotGroupAry or null if nothing is created.
 */
function confirmPlotGroup(plotGroupAry,action) {
    const {plotGroupId,groupLocked}= action.payload;
    if (plotGroupExist(plotGroupId,plotGroupAry)) return null;
    var plotGroup= PlotGroup.makePlotGroup(plotGroupId, groupLocked);
    return [...plotGroupAry,plotGroup];
}


function pvExist(plotId, plotViewAry) {
    return (plotViewAry.some( (pv) => pv.plotId===plotId ));
}

function plotGroupExist(plotGroupId, plotGroupAry) {
    return (plotGroupAry.some( (pg) => pg.plotGroupId===plotGroupId ));
}



