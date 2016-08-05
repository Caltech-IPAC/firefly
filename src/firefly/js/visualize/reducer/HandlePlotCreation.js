/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, uniqBy,unionBy} from 'lodash';
import Cntlr, {ExpandType} from '../ImagePlotCntlr.js';
import PlotView, {makePlotView} from './PlotView.js';
import {getPlotViewById, clonePvAry} from '../PlotViewUtil.js';
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
            if (plotGroupAry || plotViewAry || plotRequestDefaults) {
                retState= clone(state, {activePlotId});
                if (plotViewAry) retState.plotViewAry= plotViewAry;
                if (plotGroupAry) retState.plotGroupAry= plotGroupAry;
                if (plotRequestDefaults) retState.plotRequestDefaults= plotRequestDefaults;
            }
            break;
        case Cntlr.PLOT_IMAGE_FAIL  :
            retState= plotFail(state,action);
            break;
        case Cntlr.PLOT_IMAGE  :
            retState= addPlot(state,action, true, true);
            // activePlotId= action.payload.plotId;
            // todo: also process adding to history
            break;

        case Cntlr.ROTATE_START  :
        case Cntlr.FLIP_START:
        case Cntlr.CROP_START:
            retState= workingServerCall(state,action);
            break;
        
        
        case Cntlr.ROTATE_FAIL  :
        case Cntlr.FLIP_FAIL:
        case Cntlr.CROP_FAIL:
            retState= endServerCallFail(state,action);
            break;
        case Cntlr.ROTATE  :
            retState= addPlot(state,action, true, false);
            break;
        case Cntlr.FLIP:
            retState= addPlot(state,action, true, false);
            break;
        case Cntlr.CROP:
            retState= addPlot(state,action, true, false);
            break;
        default:
            break;
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

function addPlot(state,action, replace, setActive) {
    var {plotViewAry, activePlotId}= state;
    const {pvNewPlotInfoAry}= action.payload;

    if (pvNewPlotInfoAry) { // used for doing groups
        plotViewAry= plotViewAry.map( (pv) => { // map has side effect of setting active plotId
            const info= pvNewPlotInfoAry.find( (i) => i.plotId===pv.plotId);
            if (!info) return pv;
            const {plotAry, overlayPlotViews}= info;
            if (setActive) activePlotId= pv.plotId;
            return PlotView.replacePlots(pv,plotAry,overlayPlotViews, state.expandedMode, replace);
        });
    }
    else {// used for single plot update
        console.error(`Deprecated payload send to handPlotChange.addPlot: type:${action.type}`);
        const {plotAry, overlayPlotViews, plotId}= action.payload;
        plotViewAry= plotViewAry.map( (pv) => { // map has side effect of setting active plotId
            if (pv.plotId!==plotId ) return pv;
            if (setActive) activePlotId= plotId;
            return PlotView.replacePlots(pv,plotAry,overlayPlotViews, state.expandedMode, replace);
         });
    }
    return clone(state, {plotViewAry,activePlotId});
}


function plotFail(state,action) {
    const {description, plotId}= action.payload;
    var {plotViewAry}= state;
    const plotView=  getPlotViewById(state,plotId);
    if (!plotView) return plotViewAry;
    const changes= {plottingStatus:description,serverCall:'fail' };
    return clone(state,  {plotViewAry:clonePvAry(plotViewAry,plotId,changes)});
}


// function plotFail(state,action) {
//     const {description, plotId, wpRequestAry}= action.payload;
//     var {plotViewAry}= state;
//     if (plotId) {
//         const plotView=  getPlotViewById(state,plotId);
//         if (!plotView) return state;
//         const changes= {plottingStatus:description,serverCall:'fail' };
//         return clone(state,  {plotViewAry:clonePvAry(plotViewAry,plotId,changes)});
//     }
//     else if (wpRequestAry) {
//         const pvChangeAry= wpRequestAry.map( (r) => {
//             const pv=  getPlotViewById(state,plotId);
//             if (!pv) return null;
//             return clone(pv,{plottingStatus:description,serverCall:'fail' } );
//         });
//         const newPlotViewAry= unionBy(pvChangeAry,plotViewAry, 'plotId');
//         return clone(state,  {plotViewAry:newPlotViewAry});
//     }
// }



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
                              }) : makePlotView(plotId, req,action.payload.pvOptions);
    });

    return unionBy(pvChangeAry,plotViewAry, 'plotId');
}

export function endServerCallFail(state,action) {
    var {plotId,message}= action.payload;
    var {plotViewAry}= state;
    const stat= {serverCall:'fail'};
    if (typeof message === 'string') stat.plottingStatus= message;
    return clone(state, {plotViewAry: clonePvAry(state.plotViewAry,plotId, stat)});
}
function workingServerCall(state,action) {
    var {plotId,message}= action.payload;
    return clone(state, {plotViewAry:
               clonePvAry(state.plotViewAry,plotId, {serverCall:'working', plottingStatus:message})});
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

