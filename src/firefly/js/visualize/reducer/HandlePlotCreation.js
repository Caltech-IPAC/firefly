/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {uniqBy,unionBy, isEmpty} from 'lodash';
import Cntlr, {WcsMatchType} from '../ImagePlotCntlr.js';
import {replacePlots, makePlotView,
        findWCSMatchScrollPosition, updatePlotViewScrollXY, findScrollPtToCenterImagePt} from './PlotView.js';
import {makeOverlayPlotView, replaceOverlayPlots} from './OverlayPlotView.js';
import {primePlot, getPlotViewById, clonePvAry, getOverlayById, getPlotViewIdListInGroup} from '../PlotViewUtil.js';
import {makeScreenPt} from '../Point.js';
import PlotGroup from '../PlotGroup.js';
import {PlotAttribute} from '../WebPlot.js';
import {CCUtil} from '../CsysConverter.js';
import {getRotationAngle} from '../VisUtil.js';
import {updateTransform} from '../PlotTransformUtils.js';


//============ EXPORTS ===========
//============ EXPORTS ===========



const clone = (obj,params={}) => Object.assign({},obj,params);


export function reducer(state, action) {

    let retState= state;
    let plotViewAry;
    let plotGroupAry;
    let plotRequestDefaults;
    const {setNewPlotAsActive=true}= action.payload;
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
                if (setNewPlotAsActive) retState.activePlotId= action.payload.plotId;
            }
            break;
        case Cntlr.PLOT_IMAGE_FAIL  :
            retState= plotFail(state,action);
            break;
        case Cntlr.PLOT_IMAGE  :
            retState= addPlot(state,action, action.payload.setNewPlotAsActive, setNewPlotAsActive);
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

        case Cntlr.CROP_START:
            retState= workingServerCall(state,action);
            break;
        
        case Cntlr.CROP_FAIL:
            retState= endServerCallFail(state,action);
            break;

        case Cntlr.CROP:
            retState= addPlot(state,action, false, false);
            break;
        default:
            break;
    }
    return retState;
}



const updateDefaults= function(plotRequestDefaults, action) {

    const {wpRequestAry}= action.payload;
    if (wpRequestAry) {
        const newObj= wpRequestAry.reduce( (obj,r) => {
            obj[r.getPlotId()]={threeColor:false, wpRequest:r};
            return obj;
        }, {});
        return clone(plotRequestDefaults, newObj);
    }
    else {
        const {plotId,wpRequest,redReq,greenReq, blueReq,threeColor}= action.payload;
        return threeColor ?
            clone(plotRequestDefaults, {[plotId]:{threeColor,redReq,greenReq, blueReq}}) :
            clone(plotRequestDefaults, {[plotId]:{threeColor,wpRequest}});
    }
};

function addPlot(state,action, setActive, newPlot) {
    const {wcsMatchType}= state;
    let {plotViewAry, activePlotId, prevActivePlotId, mpwWcsPrimId, processedTiles}= state;
    const {pvNewPlotInfoAry}= action.payload;

    if (!pvNewPlotInfoAry) {
        console.error(new Error(
            `Deprecated payload send to handPlotChange.addPlot: type:${action.type}, plot not updated`));
        return state;
    }


    plotViewAry = plotViewAry.map((pv) => { // map has side effect of setting active plotId, and cleaning processedTiles
        const info = pvNewPlotInfoAry.find((i) => i.plotId === pv.plotId && (!i.requestKey || i.requestKey===pv.request.getRequestKey()));
        if (!info) return pv;
        const {plotAry, overlayPlotViews}= info;
        if (setActive) {
            prevActivePlotId = state.activePlotId;
            activePlotId = pv.plotId;
        }
        pv = replacePlots(pv, plotAry, overlayPlotViews, state.expandedMode, newPlot);
        if (pv.plotViewCtx.rotateNorthLock) {
            pv.rotation= 360 - getRotationAngle(primePlot(pv));
            pv= updateTransform(pv);
        }
        if (pv.request.getRotate()) {
            pv.rotation=  Math.trunc(pv.request.getRotationAngle() -  getRotationAngle(primePlot(pv)));
            pv= updateTransform(pv);
        }
        processedTiles= processedTiles.filter( (d) => d.plotId!==pv.plotId); // remove old client tile data
        return pv;
    });

    if (!mpwWcsPrimId) mpwWcsPrimId = activePlotId;

    const newState = clone(state, {prevActivePlotId, plotViewAry, activePlotId, mpwWcsPrimId, processedTiles});

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
            const masterPv=getPlotViewById(visRoot,mpwWcsPrimId);
            if (masterPv) {
                const {scrollX,scrollY}= masterPv;
                const newSp= findWCSMatchScrollPosition(visRoot, mpwWcsPrimId, primePlot(pv), makeScreenPt(scrollX,scrollY));
                pv= updatePlotViewScrollXY(pv, newSp);
            }
        }
    }
    else if (wcsMatchType===WcsMatchType.Target) {
        if (getPlotViewIdListInGroup(visRoot,pv.plotId,true,true).length<2) {
            const ft=  plot.attributes[PlotAttribute.FIXED_TARGET];
            if (ft) {
                const centerImagePt = CCUtil.getImageCoords(plot, ft);
                pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv, centerImagePt));
            }
        }
        else {
            // const offPt= findWCSMatchOffset(visRoot, mpwWcsPrimId, primePlot(pv));
            const masterPv=getPlotViewById(visRoot,mpwWcsPrimId);
            if (masterPv) {
                const newSp= findWCSMatchScrollPosition(visRoot, mpwWcsPrimId, pv.plotId, makeScreenPt(scrollX,scrollY));
                pv= updatePlotViewScrollXY(pv, newSp);
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
    let oPvArray;
    let opv;
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
            return replaceOverlayPlots(opv,clone(plot, {affTrans:pv.affTrans}));
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
    const {plotViewAry}= state;
    const plotView=  getPlotViewById(state,plotId);
    if (!plotView) return state;
    const changes= {plottingStatus:description,serverCall:'fail' };
    return clone(state,  {plotViewAry:clonePvAry(plotViewAry,plotId,changes)});
}

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
        let pv= getPlotViewById(plotViewAry,plotId);
        pv= pv ? clone(pv, { plottingStatus:'Plotting...', plots:[],  primeIdx:-1, request: req && req.makeCopy() }) :
                 makePlotView(plotId, req,action.payload.pvOptions);

        // if (req.getRotate()) pv.rotation= req.getRotationAngle();
        if (req.getRotateNorth()) {
            pv.plotViewCtx= clone(pv.plotViewCtx, {rotateNorthLock :true});
        }
        return pv;
    });

    return unionBy(pvChangeAry,plotViewAry, 'plotId');
}

export function endServerCallFail(state,action) {
    const {plotId,message}= action.payload;
    const stat= {serverCall:'fail'};
    if (typeof message === 'string') stat.plottingStatus= message;
    return clone(state, {plotViewAry: clonePvAry(state.plotViewAry,plotId, stat)});
}
function workingServerCall(state,action) {
    const {plotId,message}= action.payload;
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


    const newGrpAry= wpRequestAry
        .filter( (r) => !plotGroupExist(r.getPlotGroupId(),plotGroupAry))
        .map( (r) => PlotGroup.makePlotGroup(r.getPlotGroupId(), r.isGroupLocked()));

    return [...plotGroupAry,...uniqBy(newGrpAry, 'plotGroupId')];

}


// function pvExist(plotId, plotViewAry) {
//     return (plotViewAry.some( (pv) => pv.plotId===plotId ));
// }


function plotGroupExist(plotGroupId, plotGroupAry) {
    return (plotGroupAry.some( (pg) => pg.plotGroupId===plotGroupId ));
}


function getRequestAry(obj) {
    if (obj.wpRequestAry) return obj.wpRequestAry;
    const rKey= ['wpRequest','redReq','blueReq','greenReq'].find( (key) => Boolean(obj[key]));
    return rKey ? [obj[rKey]] : null;
}

