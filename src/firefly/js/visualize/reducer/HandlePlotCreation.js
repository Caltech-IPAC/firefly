/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {uniqBy,unionBy} from 'lodash';
import Cntlr, {ExpandType} from '../ImagePlotCntlr.js';
import PlotView, {makePlotView} from './PlotView.js';
import {getPlotViewById} from '../PlotViewUtil.js';
import PlotGroup from '../PlotGroup.js';


//============ EXPORTS ===========
//============ EXPORTS ===========



const clone = (obj,params={}) => Object.assign({},obj,params);


export function reducer(state, action) {

    var retState= state;
    var plotViewAry;
    var plotGroupAry;
    var plotRequestDefaults;
    var activePlotId= state.activePlotId;
    switch (action.type) {
        case Cntlr.PLOT_IMAGE_START  :
            plotRequestDefaults= updateDefaults(state.plotRequestDefaults,action);
            plotGroupAry= confirmPlotGroup(state.plotGroupAry,action);
            plotViewAry= preNewPlotPrep(state.plotViewAry,action);
            break;
        case Cntlr.PLOT_IMAGE_FAIL  :
            plotViewAry= plotFail(state,action);
            break;
        case Cntlr.PLOT_IMAGE  :
            plotViewAry= addPlot(state,action);
            // activePlotId= action.payload.plotId;
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
            plotViewAry= addPlot(state,action);
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

    var {wpRequestAry}= action.payload;
    if (wpRequestAry) {
        const newObj= wpRequestAry.reduce( (obj,r) => {
            obj[r.getPlotId()]={threeColor:false, wpRequest:r};
            return obj;
        }, {});
        return clone(plotRequestDefaults, newObj);
    }
    else {
        var {plotId,wpRequest,redReq,greenReq, blueReq,threeColor}= action.payload;
        return threeColor ?
            clone(plotRequestDefaults, {[plotId]:{threeColor,redReq,greenReq, blueReq}}) :
            clone(plotRequestDefaults, {[plotId]:{threeColor,wpRequest}});
    }
};

function addPlot(state,action) {
    const {plotViewAry}= state;
    const {pvNewPlotInfoAry}= action.payload;

    if (pvNewPlotInfoAry) { // used for doing groups
        return plotViewAry.map( (pv) => {
            const info= pvNewPlotInfoAry.find( (i) => i.plotId===pv.plotId);
            if (!info) return pv;
            const {plotAry, overlayPlotViews}= info;
            return PlotView.replacePlots(pv,plotAry,overlayPlotViews);
        });
    }
    else {// used for single plot update
        console.error(`Deprecated payload send to handPlotChange.addPlot: type:${action.type}`);
        const {plotAry, overlayPlotViews, plotId}= action.payload;
        return plotViewAry.map( (pv) => {
             return pv.plotId===plotId ? PlotView.replacePlots(pv,plotAry,overlayPlotViews) : pv;
         });
    }
};


function plotFail(state,action) {
    const {description, plotId}= action.payload;
    var {plotViewAry}= state;
    const plotView=  getPlotViewById(state,plotId);
    if (!plotView) return state;
    return plotViewAry.map( (pv) => pv.plotId===plotId ? 
                        clone(pv,{plottingStatus:description}) : pv);
}

/**
 /**
 *
 * @param plotViewAry
 * @param action
 * @return {[]|null} new PlotViewAry or null it nothing is created.
 */
function preNewPlotPrep(plotViewAry,action) {
    const wpRequestAry= getRequestAry(action.payload);

    const pvChangeAry= wpRequestAry.map( (req) => {
        const plotId= req.getPlotId();
        var pv= getPlotViewById(plotViewAry,plotId);
        return pv ? clone(pv, { plottingStatus:'Plotting...', 
                                plots:[],  
                                primeIdx:-1
                              }) : makePlotView(plotId, req,null);
    });

    return unionBy(pvChangeAry,plotViewAry, 'plotId');
}


/**
 *
 * @param plotGroupAry
 * @param action
 * @return {[]|null} new PlotGroupAry or null if nothing is created.
 */
function confirmPlotGroup(plotGroupAry,action) {
    const wpRequestAry= getRequestAry(action.payload);


    var newGrpAry= wpRequestAry
        .filter( (r) => !plotGroupExist(r.getPlotGroupId(),plotGroupAry))
        .map( (r) => PlotGroup.makePlotGroup(r.getPlotGroupId(), r.isGroupLocked()));

    return [...plotGroupAry,...uniqBy(newGrpAry, 'plotGroupId')];

}


function pvExist(plotId, plotViewAry) {
    return (plotViewAry.some( (pv) => pv.plotId===plotId ));
}


function plotGroupExist(plotGroupId, plotGroupAry) {
    return (plotGroupAry.some( (pg) => pg.plotGroupId===plotGroupId ));
}


function getRequestAry(obj) {
    if (obj.wpRequestAry) return obj.wpRequestAry;
    var rKey= ['wpRequest','redReq','blueReq','greenReq'].find( (key) => obj[key] ? true : false);
    return rKey ? [obj[rKey]] : null;
}

