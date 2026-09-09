/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {uniqBy, differenceBy, isEmpty, isNumber, isString, uniq} from 'lodash';
import {
    CROP, CROP_FAIL, CROP_START, DELETE_OVERLAY_PLOT, PLOT_HIPS, PLOT_HIPS_FAIL, PLOT_IMAGE, PLOT_IMAGE_FAIL,
    PLOT_IMAGE_START, PLOT_MASK, PLOT_MASK_FAIL, PLOT_MASK_START, PLOT_PROXY, REMOVE_PROXY, WcsMatchType
} from '../VisConst';
import {getRotationAngle} from '../WebPlotAnalysis';
import {
    replacePlots, makePlotView, updatePlotViewScrollXY,
    findScrollPtToCenterImagePt, updateScrollToWcsMatch, initScrollCenterPoint
} from './PlotView.js';
import {makeOverlayPlotView, initOverlayPlots} from './OverlayPlotView.js';
import {
    primePlot, getPlotViewById, clonePvAry, getOverlayById, getPlotViewIdListByPositionLock,
    getCubeLength, getHDU, getCubePlaneIdx
} from '../PlotViewUtil.js';
import {getPlotGroupById, makePlotGroup} from '../PlotGroup.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {CCUtil} from '../CsysConverter.js';
import {updateTransform} from '../PlotTransformUtils.js';
import {makeImagePt} from '../Point.js';
import {isImage} from '../WebPlot';
import {getNumberHeader, HdrConst} from 'firefly/visualize/FitsHeaderUtil.js';


export function reducer(state, action) {
    switch (action.type) {
        case PLOT_IMAGE_START  :
            return startPlot(state,action);
        case PLOT_IMAGE_FAIL  :
            return plotFail(state,action);
        case PLOT_IMAGE  :
            const {setNewPlotAsActive=true}= action.payload;
            return addPlot(state,action, action.payload.setNewPlotAsActive, setNewPlotAsActive);
        case PLOT_PROXY:
            return addProxy(state,action);
        case REMOVE_PROXY:
            return removeProxy(state,action);
        case PLOT_HIPS  :
            return addHiPS(state,action);
        case PLOT_HIPS_FAIL  :
            return hipsFail(state,action);
        case PLOT_MASK_START:
            return newOverlayPrep(state,action);
        case PLOT_MASK:
            return addOverlay(state,action);
        case DELETE_OVERLAY_PLOT:
            return removeOverlay(state,action);
        case PLOT_MASK_FAIL:
            return plotOverlayFail(state,action);
        case CROP_START:
            return workingServerCall(state,action);
        case CROP_FAIL:
            return endServerCallFail(state,action);
        case CROP:
            return addPlot(state,action, false, false);
        default:
            return state;
    }
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


function addProxy(state,action={}) {
    const {plotProxyAry}= state;
    const {payload:{plotId, ...rest}={}}= action;
    if (!plotId) return state;
    const hasProxy= Boolean(plotProxyAry.find( (proxy) => proxy.plotId === plotId));

    const newPAry= hasProxy
        ? plotProxyAry.map( (proxy) => proxy.id===plotId ? {plotId, ...rest} : proxy)
        : [...plotProxyAry, {plotId,...rest}];
    return { ...state, plotProxyAry: newPAry};
}

function removeProxy(state,action) {
    const {plotProxyAry}= state;
    const {payload:{plotId}={}}= action;
    if (!plotId) return state;
    return {
        ...state,
        plotProxyAry: plotProxyAry.filter( (proxy) => proxy.plotId!==plotId),
    };
}

function startPlot(state, action) {
    const plotRequestDefaults= action.payload.enableRestore && updateDefaults(state.plotRequestDefaults,action);
    const plotGroupAry= confirmPlotGroup(state.plotGroupAry,action);
    const plotViewAry= preNewPlotPrep(state,action, plotGroupAry);
    const retState= {...state};
    if (plotViewAry) {
        retState.plotViewAry= plotViewAry;
        retState.plotProxyAry= state.plotProxyAry
            .filter( (proxy) => !plotViewAry.some( (pv) => pv.plotId===proxy.plotId));
    }
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

    plotViewAry = plotViewAry.map((pv) => { // map has side effect of setting active plotId
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

/**
 * Count the number of cubes
 * @param {PlotView} pv
 * @return {number} the number of cubes, 0 if none
 */
function countCubes(pv) {
    if (!pv || !isImage(primePlot(pv)) ) return 0;
    return pv.plots.reduce( (total, p, idx) => {
        if (idx===0) return getCubePlaneIdx(p)>=0 ? 1 : 0;
        return ( getHDU(p)!==getHDU(pv.plots[idx-1]) && getCubePlaneIdx(p)>-1) ? total+1 : total;
    }, 0);
}


function addPlot(state,action, setActive, newPlot) {
    const {wcsMatchType}= state;
    let {plotViewAry, activePlotId, prevActivePlotId, mpwWcsPrimId}= state;
    const {pvNewPlotInfoAry}= action.payload;

    plotViewAry = plotViewAry.map((pv) => { // map has side effect of setting active plotId
        const info = pvNewPlotInfoAry.find((i) => i.plotId === pv.plotId && (!i.requestKey || i.requestKey===pv.request.getRequestKey()));
        if (!info) return pv;
        const {plotAry, overlayPlotViews}= info;
        if (setActive) {
            prevActivePlotId = state.activePlotId;
            activePlotId = pv.plotId;
        }
        pv = replacePlots(pv, plotAry, overlayPlotViews, state.expandedMode, newPlot);
        const hduCnt= uniq(pv.plots.map( (p) => getNumberHeader(p,HdrConst.SPOT_EXT,0)));
        pv.plotViewCtx.multiHdu= hduCnt.length>1;
        pv.plotViewCtx.cubeCnt= countCubes(pv);
        pv.plotViewCtx.hduPlotStartIndexes=  pv.plotViewCtx.multiHdu ?
                                pv.plots.map( (p,idx) => idx).filter( (idx) => getCubePlaneIdx(pv.plots[idx])<1) : [0];


        if (pv.plotViewCtx.cubeCnt>0) {
            const firstCubePlotIdx= pv.plots.findIndex( (p) => p.cubeIdx>-1);
            const cnt= getCubeLength(pv.plots[firstCubePlotIdx]);
            const frameIdx= getFirstFrameFromAttribute(pv,cnt);

            if (frameIdx>0) {
                const idx= pv.plots.findIndex( (p) => frameIdx===p.cubeIdx);
                if (idx>-1) {
                    pv.primeIdx= idx;
                    pv= initScrollCenterPoint(pv);
                }
            }
        }

        pv.request= pv.plots[0].plotState.getWebPlotRequest();
        if (pv.plotViewCtx.rotateNorthLock) {
            pv.rotation= 360 - getRotationAngle(primePlot(pv));
            pv= updateTransform(pv);
        }
        if (pv.request.getRotate()) {
            pv.rotation=  Math.trunc(pv.request.getRotationAngle() -  getRotationAngle(primePlot(pv)));
            pv= updateTransform(pv);
        }
        return pv;
    });

    if (!mpwWcsPrimId) mpwWcsPrimId = activePlotId;

    const newState = {...state, prevActivePlotId, plotViewAry, activePlotId, mpwWcsPrimId};

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
        if (getPlotViewIdListByPositionLock(visRoot,pv.plotId) ) {
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
    const {plotId, imageOverlayId, hduNumber, maskValue, maskNumber, description,
           color, title, drawingDef, relatedDataId,lazyLoadPayload, fileKey}= action.payload;

    const pv= getPlotViewById(state, plotId);
    if (!pv) return state;

    const overlayPv= getOverlayById(pv, imageOverlayId);
    let oPvArray;
    let opv;
    if (!overlayPv) {
        oPvArray= isEmpty(pv.overlayPlotViews) ? [] : pv.overlayPlotViews.slice(0);
        opv= makeOverlayPlotView(imageOverlayId, plotId, title, hduNumber,
                                           maskNumber, maskValue, color, drawingDef, relatedDataId,
                                           fileKey);
        opv.description= description;
        if (lazyLoadPayload) {
            opv.lazyLoadPayload= lazyLoadPayload;
            opv.visible= false;
        }
        oPvArray.push(opv);
    }
    else {
        oPvArray= pv.overlayPlotViews.map( (opv) => opv.imageOverlayId===imageOverlayId ?
                             {...overlayPv, imageNumber:hduNumber, maskValue, color, drawingDef, plot:null} : opv);
    }

    return {...state, plotViewAry:clonePvAry(state, plotId, {overlayPlotViews:oPvArray})};
}


function addOverlay(state, action) {
    const {plotId, imageOverlayId, cube=false, primeIdx=-1}= action.payload;

    const plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!== plotId) return pv;
        const overlayPlotViews= pv.overlayPlotViews.map( (opv) => {
            if (opv.imageOverlayId!== imageOverlayId) return opv;
            const plots= action.payload.plots.map( (p) => ({...p,affTrans:pv.affTrans}));
            return initOverlayPlots({...opv,cube, plots, primeIdx});
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
    const {description, plotId, plotIdAry:inPlotIdAry=[]}= action.payload;
    const plotIdAry= plotId ? [plotId] : inPlotIdAry;
    const changes= {plottingStatusMsg:description,serverCall:'fail', nonRecoverableFail:true };
    const plotViewAry= state.plotViewAry?.map( (pv) => plotIdAry.includes(pv.plotId) ? {...pv,...changes}  : pv);
    return {...state,  plotViewAry};
}

function hipsFail(state,action) {
    const {description, plotId}= action.payload;
    const {plotViewAry}= state;
    const plotView=  getPlotViewById(state,plotId);
    if (!plotView) return state;
    const changes= {plottingStatusMsg:description,serverCall:'fail', nonRecoverableFail:true };
    return {...state,  plotViewAry:clonePvAry(plotViewAry,plotId,changes)};
}



/**
 /**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @param {Array.<PlotGroup>} plotGroupAry
 * @return {Array|null} new PlotViewAry or null it nothing is created.
 */
function preNewPlotPrep(state,action, plotGroupAry) {
    const {plotViewAry}= state;
    const {payload}= action;
    const wpRequestAry= getRequestAry(payload);

    const pvChangeAry= wpRequestAry.map( (req) => {
        const plotId= req.getPlotId();
        const plotGroup= getPlotGroupById(plotGroupAry,req.getPlotGroupId());
        const pv= makePlotView(plotId, req,{
            rotateNorthLock: plotGroup.rotateNorthLockSticky,
            flipYLock: plotGroup.flipYSticky,
            ...payload.pvOptions,
        });

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
        .map( (r) => makePlotGroup(r.getPlotGroupId(), r.isGroupLocked()));

    return [...plotGroupAry,...uniqBy(newGrpAry, 'plotGroupId')];

}

function plotGroupExist(plotGroupId, plotGroupAry) {
    return (plotGroupAry.some( (pg) => pg.plotGroupId===plotGroupId ));
}


function getRequestAry(obj) {
    if (obj.wpRequestAry) return obj.wpRequestAry;
    const rKey= ['wpRequest','redReq','blueReq','greenReq'].find( (key) => Boolean(obj[key]));
    return rKey ? [obj[rKey]] : null;
}

function getFirstFrameFromAttribute(pv,totFrames) {
    const first=  pv?.request?.getAttributes()?.[PlotAttribute.CUBE_FIRST_FRAME] ?? 0;
    if (first===0) return 0;
    if (isNumber(first)) {
        const frame= Math.trunc(first);
        if (frame<totFrames) return frame;
    }
    if (isString(first)) {
        const num= parseInt(first);
        const frame= first.endsWith('%') ? Math.trunc( (totFrames-1) * (num/100)) : Math.trunc(num);
        if (frame<totFrames) return frame;
    }
    return 0;
}
