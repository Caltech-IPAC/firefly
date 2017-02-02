/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {uniqBy,unionBy, isEmpty} from 'lodash';
import Cntlr, {WcsMatchType} from '../ImagePlotCntlr.js';
import PlotView, {replacePlots, makePlotView,findWCSMatchOffset, updatePlotViewScrollXY} from './PlotView.js';
import {makeOverlayPlotView, replaceOverlayPlots} from './OverlayPlotView.js';
import {primePlot, getPlotViewById, clonePvAry, getOverlayById, getPlotViewIdListInGroup} from '../PlotViewUtil.js';
import {makeScreenPt} from '../Point.js';
import PlotGroup from '../PlotGroup.js';
import {PlotAttribute} from '../WebPlot.js';
import {CCUtil} from '../CsysConverter.js';


//============ EXPORTS ===========
//============ EXPORTS ===========



const clone = (obj,params={}) => Object.assign({},obj,params);


export function reducer(state, action) {

    var retState= state;
    var plotViewAry;
    var plotGroupAry;
    var plotRequestDefaults;
    switch (action.type) {
        case Cntlr.PLOT_IMAGE_START  :
            plotRequestDefaults= updateDefaults(state.plotRequestDefaults,action);
            plotGroupAry= confirmPlotGroup(state.plotGroupAry,action);
            plotViewAry= preNewPlotPrep(state.plotViewAry,action);
            if (plotGroupAry || plotViewAry || plotRequestDefaults) {
                retState= clone(state);
                if (plotViewAry) retState.plotViewAry= plotViewAry;
                if (plotGroupAry) retState.plotGroupAry= plotGroupAry;
                if (plotRequestDefaults) retState.plotRequestDefaults= plotRequestDefaults;
            }
            break;
        case Cntlr.PLOT_IMAGE_FAIL  :
            retState= plotFail(state,action);
            break;
        case Cntlr.PLOT_IMAGE  :
            retState= addPlot(state,action, action.payload.setNewPlotAsActive, true);
            // todo: also process adding to history
            break;

        case Cntlr.PLOT_MASK_START:
            retState= newOverlayPrep(state,action);
            break;

        case Cntlr.PLOT_MASK:
            retState= addOverlay(state,action);
            break;

        case Cntlr.DELETE_OVERLAY_PLOT:
            retState= removeOverlay(state,action);
            break;


        case Cntlr.PLOT_MASK_FAIL:
            retState= plotOverlayFail(state,action);
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
        case Cntlr.FLIP:
        case Cntlr.CROP:
            retState= addPlot(state,action, false, false);
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

function addPlot(state,action, setActive, newPlot) {
    var {plotViewAry, activePlotId, prevActivePlotId, mpwWcsPrimId, wcsMatchType}= state;
    const {pvNewPlotInfoAry}= action.payload;

    if (!pvNewPlotInfoAry) {
        console.error(new Error(
            `Deprecated payload send to handPlotChange.addPlot: type:${action.type}, plot not updated`));
        return state;
    }


    plotViewAry = plotViewAry.map((pv) => { // map has side effect of setting active plotId
        const info = pvNewPlotInfoAry.find((i) => i.plotId === pv.plotId && (!i.requestKey || i.requestKey===pv.request.getRequestKey()));
        if (!info) return pv;
        const {plotAry, overlayPlotViews}= info;
        if (setActive) {
            prevActivePlotId = state.activePlotId;
            activePlotId = pv.plotId;
        }
        pv = replacePlots(pv, plotAry, overlayPlotViews, state.expandedMode, newPlot);
        return pv;
    });

    if (!mpwWcsPrimId) mpwWcsPrimId = activePlotId;

    const newState = clone(state, {prevActivePlotId, plotViewAry, activePlotId, mpwWcsPrimId});

    if (wcsMatchType) {
        newState.plotViewAry = plotViewAry.map((pv) => updateForWcsMatching(newState, pv, mpwWcsPrimId));
    }
    return newState;
}


function updateForWcsMatching(visRoot, pv, mpwWcsPrimId) {
    const {wcsMatchType}= visRoot;
    const plot= primePlot(pv);
    if (!plot || !wcsMatchType ) return pv;

    if (wcsMatchType===WcsMatchType.Standard) {
        if (mpwWcsPrimId!==pv.plotId) {
            const offPt= findWCSMatchOffset(visRoot, mpwWcsPrimId, primePlot(pv));
            const masterPv=getPlotViewById(visRoot,mpwWcsPrimId);
            if (masterPv) {
                pv= updatePlotViewScrollXY(pv, makeScreenPt(masterPv.scrollX-offPt.x, masterPv.scrollY-offPt.y), false);
            }
        }
    }
    else if (wcsMatchType===WcsMatchType.Target) {
        if (getPlotViewIdListInGroup(visRoot,pv.plotId,true,true).length<2) {
            const ft=  plot.attributes[PlotAttribute.FIXED_TARGET];
            if (ft) {
                const centerImagePt = CCUtil.getImageCoords(plot, ft);
                pv= updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv, centerImagePt, false));
            }
        }
        else {
            const offPt= findWCSMatchOffset(visRoot, mpwWcsPrimId, primePlot(pv));
            const masterPv=getPlotViewById(visRoot,mpwWcsPrimId);
            if (masterPv) {
                pv= updatePlotViewScrollXY(pv, makeScreenPt(masterPv.scrollX-offPt.x, masterPv.scrollY-offPt.y), false);
            }
        }
    }
    return pv;
}


function newOverlayPrep(state, action) {
    const {plotId, imageOverlayId, imageNumber, maskValue, maskNumber,
           color, title, drawingDef, relatedDataId,lazyLoadPayload, fileKey}= action.payload;

    const pv= getPlotViewById(state, plotId);
    if (!pv) return state;

    const overlayPv= getOverlayById(pv, imageOverlayId);
    var oPvArray;
    var opv;
    if (!overlayPv) {
        oPvArray= isEmpty(pv.overlayPlotViews) ? [] : pv.overlayPlotViews.slice(0);
        opv= makeOverlayPlotView(imageOverlayId, plotId, title, imageNumber,
                                           maskNumber, maskValue, color, drawingDef, relatedDataId,
                                           fileKey);
        if (lazyLoadPayload) {
            opv.lazyLoadPayload= lazyLoadPayload;
            opv.visible= false;
        }
        oPvArray.push(opv);
    }
    else {
        oPvArray= pv.overlayPlotViews.map( (opv) => opv.imageOverlayId===imageOverlayId ?
                             clone(overlayPv, {imageNumber, maskValue, color, drawingDef, plot:null} ) : opv);
    }

    return clone(state, {plotViewAry:clonePvAry(state, plotId, {overlayPlotViews:oPvArray}) } );
}


function addOverlay(state, action) {
    const {plotId, imageOverlayId, plot}= action.payload;

    const plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!== plotId) return pv;
        const overlayPlotViews= pv.overlayPlotViews.map( (opv) => {
            if (opv.imageOverlayId!== imageOverlayId) return opv;
            return replaceOverlayPlots(opv,plot);
        });
        return clone(pv, {overlayPlotViews});
    });
    return clone(state, {plotViewAry});
}


function removeOverlay(state, action) {
    const {plotId, imageOverlayId, deleteAll}= action.payload;
    const plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!== plotId) return pv;

        const overlayPlotViews= deleteAll ? [] :
                                   pv.overlayPlotViews.filter( (opv) => opv.imageOverlayId!== imageOverlayId);
        return clone(pv, {overlayPlotViews});
    });
    return clone(state, {plotViewAry});
}



function plotOverlayFail(state,action) {
    const {plotId, imageOverlayId, detailFailReason}= action.payload;

    const plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!==plotId) return pv;
        const overlayPlotViews= pv.overlayPlotViews.filter( (opv) => imageOverlayId!==opv.imageOverlayId);
        return clone(pv, {overlayPlotViews, plottingStatus:'Overlay failed: '+detailFailReason, serverCall:'fail' });
    });

    return clone(state, {plotViewAry});
}

function plotFail(state,action) {
    const {description, plotId}= action.payload;
    var {plotViewAry}= state;
    const plotView=  getPlotViewById(state,plotId);
    if (!plotView) return state;
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
 * @return {Array|null} new PlotViewAry or null it nothing is created.
 */
function preNewPlotPrep(plotViewAry,action) {
    const wpRequestAry= getRequestAry(action.payload);

    const pvChangeAry= wpRequestAry.map( (req) => {
        const plotId= req.getPlotId();
        var pv= getPlotViewById(plotViewAry,plotId);
        return pv ? clone(pv, { plottingStatus:'Plotting...', 
                                plots:[],  
                                primeIdx:-1,
                                request: req
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
 * @return {Array|null} new PlotGroupAry or null if nothing is created.
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

