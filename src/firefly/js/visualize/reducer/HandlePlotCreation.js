/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {uniqBy,differenceBy, isEmpty} from 'lodash';
import Cntlr, {WcsMatchType} from '../ImagePlotCntlr.js';
import {replacePlots, makePlotView, updatePlotViewScrollXY,
        findScrollPtToCenterImagePt, updateScrollToWcsMatch} from './PlotView.js';
import {makeOverlayPlotView, replaceOverlayPlots} from './OverlayPlotView.js';
import {primePlot, getPlotViewById, clonePvAry, getOverlayById, getPlotViewIdList} from '../PlotViewUtil.js';
import PlotGroup from '../PlotGroup.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {CCUtil} from '../CsysConverter.js';
import {getRotationAngle} from '../VisUtil.js';
import {updateTransform} from '../PlotTransformUtils.js';
import {makeImagePt} from '../Point.js';
import {isImage} from '../WebPlot';




export function reducer(state, action) {

    let retState= state;
    switch (action.type) {
        case Cntlr.PLOT_IMAGE_START  :
            retState= startPlot(state,action);
            break;
        case Cntlr.PLOT_IMAGE_FAIL  :
            retState= plotFail(state,action);
            break;
        case Cntlr.PLOT_IMAGE  :
            const {setNewPlotAsActive=true}= action.payload;
            retState= addPlot(state,action, action.payload.setNewPlotAsActive, setNewPlotAsActive);
            break;

        case Cntlr.PLOT_HIPS  :
            retState= addHiPS(state,action);
            break;

        case Cntlr.PLOT_HIPS_FAIL  :
            retState= hipsFail(state,action);
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



function updateDefaults(plotRequestDefaults, action) {
    const {plotId,wpRequest,wpRequestAry,redReq,greenReq, blueReq,threeColor, plotType, pvOptions}= action.payload;
    if (plotType==='hips') {
        return {...plotRequestDefaults, [plotId]:{plotType:'hips',wpRequest,pvOptions}};
    }
    else if (plotType==='image') {
        if (wpRequestAry) {
            const newObj= wpRequestAry.reduce( (obj,r) => {
                obj[r.getPlotId()]={plotType:'image', wpRequest:r,pvOptions};
                return obj;
            }, {});
            return {...plotRequestDefaults, ...newObj};
        }
        else {
            if (!wpRequest && !redReq && !greenReq && !blueReq) return plotRequestDefaults;
            return threeColor ?
                {...plotRequestDefaults, [plotId]:{plotType:'threeColor',redReq,greenReq, blueReq,pvOptions}} :
                {...plotRequestDefaults, [plotId]:{plotType:'image',wpRequest,pvOptions}};
        }
    }
}


function startPlot(state, action) {
    const plotRequestDefaults= action.payload.enableRestore && updateDefaults(state.plotRequestDefaults,action);
    const plotGroupAry= confirmPlotGroup(state.plotGroupAry,action);
    const plotViewAry= preNewPlotPrep(state.plotViewAry,action);
    const retState= {...state};
    if (plotViewAry) retState.plotViewAry= plotViewAry;
    if (plotGroupAry) retState.plotGroupAry= plotGroupAry;
    if (plotRequestDefaults) retState.plotRequestDefaults= plotRequestDefaults;
    if (action.payload.setNewPlotAsActive) retState.activePlotId= action.payload.plotId;
    return retState;
}


function addHiPS(state,action, setActive= true, newPlot= true) {
    let {plotViewAry, activePlotId, prevActivePlotId}= state;
    const {plotId, plot}= action.payload;


    if (setActive) {
        const pv= getPlotViewById(state,plotId);
        prevActivePlotId = state.activePlotId;
        activePlotId = pv.plotId;
    }

    plotViewAry = plotViewAry.map((pv) => { // map has side effect of setting active plotId, and cleaning processedTiles
        if ((pv.plotId!==plotId)) return pv;
        pv = replacePlots(pv, [plot], null, state.expandedMode, newPlot);
        pv.serverCall= 'success';
        pv.plottingStatusMsg= 'done';
        pv.rotation= 0;
        pv.flipY= false;
        pv.flipX= false;

        if (pv.viewDim) {
            const centerImagePt= makeImagePt( plot.dataWidth/2, plot.dataHeight/2);
            pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
        }
        return pv;

    });

                     //todo: this is where parameter come from the request
    return {...state, prevActivePlotId, plotViewAry, activePlotId};
}




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
        pv.request= pv.plots[0].plotState.getWebPlotRequest();
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

    const newState = {...state, prevActivePlotId, plotViewAry, activePlotId, mpwWcsPrimId, processedTiles};

    if (wcsMatchType) {
        newState.plotViewAry = plotViewAry.map((pv) => updateForWcsMatching(newState, pv, mpwWcsPrimId));
    }
    return newState;
}


function updateForWcsMatching(visRoot, pv, mpwWcsPrimId) {
    const {wcsMatchType}= visRoot;
    const plot= primePlot(pv);
    if (!plot || !wcsMatchType ) return pv;

    const masterPv=getPlotViewById(visRoot,mpwWcsPrimId);

    if (wcsMatchType===WcsMatchType.Standard) {
        pv= updateScrollToWcsMatch(visRoot.wcsMatchType, masterPv, pv);
    }
    else if (wcsMatchType===WcsMatchType.Target) {
        if (getPlotViewIdList(visRoot,pv.plotId,true,true).length<2) {
            const ft=  plot.attributes[PlotAttribute.FIXED_TARGET];
            if (ft) {
                const centerImagePt = CCUtil.getImageCoords(plot, ft);
                pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv, centerImagePt));
            }
        }
        else {
            pv= updateScrollToWcsMatch(visRoot.wcsMatchType, masterPv, pv);
        }
    }
    else if ((wcsMatchType===WcsMatchType.Pixel || wcsMatchType===WcsMatchType.PixelCenter) && isImage(plot)) {
        pv= updateScrollToWcsMatch(visRoot.wcsMatchType, masterPv, pv);
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
                             {...overlayPv, imageNumber, maskValue, color, drawingDef, plot:null} : opv);
    }

    return {...state, plotViewAry:clonePvAry(state, plotId, {overlayPlotViews:oPvArray})};
}


function addOverlay(state, action) {
    const {plotId, imageOverlayId, plot}= action.payload;

    const plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!== plotId) return pv;
        const overlayPlotViews= pv.overlayPlotViews.map( (opv) => {
            if (opv.imageOverlayId!== imageOverlayId) return opv;
            return replaceOverlayPlots(opv,{...plot, affTrans:pv.affTrans});
        });
        return {...pv, overlayPlotViews};
    });
    return {...state, plotViewAry};
}


function removeOverlay(state, action) {
    const {plotId, imageOverlayId, deleteAll}= action.payload;
    const plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!== plotId) return pv;

        const overlayPlotViews= deleteAll ? [] :
                                   pv.overlayPlotViews.filter( (opv) => opv.imageOverlayId!== imageOverlayId);
        return {...pv, overlayPlotViews};
    });
    return {...state, plotViewAry};
}



function plotOverlayFail(state,action) {
    const {plotId, imageOverlayId, detailFailReason}= action.payload;

    const plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!==plotId) return pv;
        const overlayPlotViews= pv.overlayPlotViews.filter( (opv) => imageOverlayId!==opv.imageOverlayId);
        return {...pv, overlayPlotViews, plottingStatusMsg:'Overlay failed: '+detailFailReason, serverCall:'fail' };
    });

    return {...state, plotViewAry};
}

function plotFail(state,action) {
    const {description, plotId}= action.payload;
    const {plotViewAry}= state;
    const plotView=  getPlotViewById(state,plotId);
    if (!plotView) return state;
    const changes= {plottingStatusMsg:description,serverCall:'fail' };
    return {...state,  plotViewAry:clonePvAry(plotViewAry,plotId,changes)};
}

function hipsFail(state,action) {
    const {description, plotId}= action.payload;
    const {plotViewAry}= state;
    const plotView=  getPlotViewById(state,plotId);
    if (!plotView) return state;
    const changes= {plottingStatusMsg:description,serverCall:'fail' };
    return {...state,  plotViewAry:clonePvAry(plotViewAry,plotId,changes)};
}



/**
 /**
 *
 * @param plotViewAry
 * @param action
 * @return {Array|null} new PlotViewAry or null it nothing is created.
 */
function preNewPlotPrep(plotViewAry,action) {
    const {payload}= action;
    const wpRequestAry= getRequestAry(payload);

    const pvChangeAry= wpRequestAry.map( (req) => {
        const plotId= req.getPlotId();
        const pv= makePlotView(plotId, req,payload.pvOptions);

        const {hipsImageConversion}= payload;
        if (hipsImageConversion) {
            pv.plotViewCtx= {...pv.plotViewCtx, hipsImageConversion};
        }

        if (req.getRotateNorth()) {
            pv.plotViewCtx= {...pv.plotViewCtx, rotateNorthLock :true};
        }

        pv.serverCall= 'working';
        pv.plottingStatusMsg= 'Loading';
        return pv;
    });

    const toAdd= differenceBy(pvChangeAry, plotViewAry, 'plotId');
    const originalAndReplaced= plotViewAry.map( (pv) => pvChangeAry.find( (tPv) => tPv.plotId===pv.plotId) || pv);
    return [...originalAndReplaced, ...toAdd];
}


export function endServerCallFail(state,action) {
    const {plotId,message}= action.payload;
    const stat= {serverCall:'fail'};
    if (typeof message === 'string') stat.plottingStatusMsg= message;
    return {...state, plotViewAry: clonePvAry(state.plotViewAry,plotId, stat)};
}
function workingServerCall(state,action) {
    const {plotId,message}= action.payload;
    return {...state, plotViewAry: clonePvAry(state.plotViewAry,plotId, {serverCall:'working', plottingStatusMsg:message})};
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

