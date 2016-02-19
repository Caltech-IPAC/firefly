/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Cntlr, {ExpandType} from '../ImagePlotCntlr.js';
import PlotView, {makePlotView} from './PlotView.js';
import PlotGroup from '../PlotGroup.js';


//============ EXPORTS ===========
//============ EXPORTS ===========

export default {
    reducer
};


const clone = (obj,params={}) => Object.assign({},obj,params);


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
            plotViewAry= addPlot(state,action);
            activePlotId= action.payload.plotId;
            // todo: also process adding to history
            break;
        case Cntlr.ROTATE_START  :
        case Cntlr.ROTATE_FAIL  :
        case Cntlr.FLIP_START:
        case Cntlr.FLIP_FAIL:
        case Cntlr.CROP_START:
        case Cntlr.CROP_FAIL:
            break;
        case Cntlr.ROTATE  :
            plotViewAry= addPlot(state,action);
            break;
        case Cntlr.FLIP:
            plotViewAry= addPlot(state,action);
            break;
        case Cntlr.CROP: //todo- crop
            break;
        default:
            break;
    }
    if (plotGroupAry || plotViewAry || plotRequestDefaults) {
        retState= clone(state, {activePlotId});
        if (plotViewAry) retState.plotViewAry= plotViewAry;
        if (plotGroupAry) retState.plotGroupAry= plotGroupAry;
        if (plotRequestDefaults) retState.plotRequestDefaults= plotRequestDefaults;
    }
    return retState;
}



const updateDefaults= function(plotRequestDefaults, action) {
    var {plotId,wpRequest,redReq,greenReq, blueReq,threeColor}= action.payload;
    return threeColor ?
        clone(plotRequestDefaults, {[plotId]:{threeColor,redReq,greenReq, blueReq}}) :
        clone(plotRequestDefaults, {[plotId]:{threeColor,wpRequest}});
};

const addPlot= function(state,action) {
    var {plotViewAry}= state;
    const {plotId, plotAry, overlayPlotViews}= action.payload;
    var expanded= state.expandedMode!==ExpandType.COLLAPSE;
    return plotViewAry.map( (pv) => {
        return pv.plotId===plotId ? PlotView.replacePlots(pv,plotAry,expanded, overlayPlotViews) : pv;
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

    var pv= makePlotView(plotId, getDefRequest(action.payload),null);
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


function getDefRequest(obj) {
    var rKey= ['wpRequest','redReq','blueReq','greenReq'].find( (key) => obj[key] ? true : false);
    return rKey ? obj[rKey] : null;
}

