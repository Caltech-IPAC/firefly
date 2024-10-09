/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty, isUndefined, isString, isNumber} from 'lodash';
import {Band} from '../Band.js';
import {getExtType} from '../FitsHeaderUtil.js';
import Cntlr, {ExpandType, WcsMatchType, ActionScope} from '../ImagePlotCntlr.js';
import {
    getRotationAngle, isCsysDirMatching, isEastLeftOfNorth, isPlotNorth
} from '../WebPlotAnalysis';
import {RotateType} from '../PlotState';
import {replacePlotView, replacePrimaryPlot, changePrimePlot, updatePlotViewScrollXY,
        findScrollPtToCenterImagePt, findScrollPtToPlaceOnDevPt,
        updateScrollToWcsMatch, updatePlotGroupScrollXY} from './PlotView.js';
import {
    WebPlot, clonePlotWithZoom, isHiPS, isImage,
    replaceHiPSProjectionUsingProperties, getHiPsTitleFromProperties, DEFAULT_BLANK_HIPS_TITLE,
    changeHiPSProjectionCenterAndType, changeHiPSProjectionCenter
} from '../WebPlot.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {replaceHiPSProjection} from '../HiPSUtil.js';
import {updateSet} from '../../util/WebUtil.js';
import {CCUtil, CysConverter} from '../CsysConverter.js';
import {convertCelestial} from '../VisUtil';
import {PlotPref} from '../PlotPref';
import {
    primePlot, clonePvAry, clonePvAryWithPv, applyToOnePvOrAll, applyToOnePvOrOverlayGroup,
    matchPlotViewByPositionGroup, getPlotViewIdxById, getPlotGroupIdxById, findPlotGroup,
    getPlotViewById, findCurrentCenterPoint, getCenterOfProjection,
    isRotationMatching, hasWCSProjection, isThreeColor, getHDU, getMatchingRotationAngle
} from '../PlotViewUtil.js';
import Point, {parseAnyPt, makeImagePt, makeWorldPt, makeDevicePt} from '../Point.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import {updateTransform} from '../PlotTransformUtils.js';
import {WebPlotRequest, WPConst} from '../WebPlotRequest.js';

const isFitFill= (userZoomType) =>  (userZoomType===UserZoomTypes.FIT || userZoomType===UserZoomTypes.FILL);

/**
 * 
 * @param {PlotView} pv
 * @param {object} att
 * @param {boolean} toAll
 * @return {PlotView} new plotview object
 */
function replaceAtt(pv,att, toAll) {
    if (toAll) {
        const plots= pv.plots.map( (p) => ({...p,attributes:{...p.attributes, ...att}}));
        return {...pv,plots};
    }
    else {
        const p= primePlot(pv);
        return replacePrimaryPlot(pv,{...p,attributes:{...p.attributes, ...att}});
    }
}


/**
 * @global
 * @public
 * @typedef {Object} ImageTile
 * @summary a single image tile
 *
 * @prop {number} width - width of this tile
 * @prop {number} height - height of this tile
 * @prop {number} index - index of this tile
 * @prop {string} url - file key to use in the service to retrieve this tile
 * @prop {number} x - pixel offset of this tile
 * @prop {number} y - pixel offset of this tile
 *
 */



export function reducer(state, action) {

    const retState= state;
    switch (action.type) {
        case Cntlr.ZOOM_HIPS  : return zoomSetup(state, action);
        case Cntlr.ZOOM_IMAGE  : return updateDisplayData(zoomSetup(state, action),action);
        case Cntlr.COLOR_CHANGE  :
        case Cntlr.STRETCH_CHANGE  : return updateDisplayData(state,action);
        case Cntlr.UPDATE_VIEW_SIZE : return updateViewSize(state,action);
        case Cntlr.PROCESS_SCROLL  : return processScroll(state,action);
        case Cntlr.CHANGE_CENTER_OF_PROJECTION: return processProjectionChange(state,action);
        case Cntlr.CHANGE_HIPS : return changeHiPS(state,action);
        case Cntlr.RECENTER: return recenter(state,action);
        case Cntlr.ROTATE: return updateClientRotation(state,action);
        case Cntlr.FLIP: return updateClientFlip(state,action);
        case Cntlr.CHANGE_PLOT_ATTRIBUTE : return changePlotAttribute(state,action);
        case Cntlr.ZOOM_LOCKING: return changeLocking(state,action);
        case Cntlr.POSITION_LOCKING: return changePositionLocking(state,action);
        case Cntlr.OVERLAY_COLOR_LOCKING: return changeOverlayColorLocking(state,action);
        case Cntlr.PLOT_PROGRESS_UPDATE  : return updatePlotProgress(state,action);
        case Cntlr.CHANGE_PRIME_PLOT  : return makeNewPrimePlot(state,action);
        case Cntlr.OVERLAY_PLOT_CHANGE_ATTRIBUTES : return changeOverlayPlotAttributes(state,action);
        case Cntlr.CHANGE_HIPS_IMAGE_CONVERSION : return changeHipsImageConversionSettings(state,action);
        case Cntlr.BYTE_DATA_REFRESH: return markByteDataRefresh(state,action);
        case Cntlr.CHANGE_IMAGE_VISIBILITY: return changeVisibility(state,action);
        case Cntlr.REQUEST_LOCAL_DATA: return requestLocalData(state,action);
        case Cntlr.CHANGE_SUBHIGHLIGHT_PLOT_VIEW: return changeSubHighPlotView(state,action);
    }
    return retState;
}

/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function changePlotAttribute(state,action) {
    const {plotId,attKey,attValue,changes={},overlayColorScope,positionScope,toAllPlotsInPlotView}= action.payload;
    let {plotViewAry}= state;
    const pv= getPlotViewById(state,plotId);
    const plot= primePlot(pv);
    if (!plot) return state;
    const newAtts= {...changes};
    if (attKey) newAtts[attKey]= attValue;

    if (positionScope) {
        plotViewAry= applyToOnePvOrAll(state.positionLock, plotViewAry, plotId, false,
            (pv)=> replaceAtt(pv,newAtts,toAllPlotsInPlotView) );
    }
    else if (overlayColorScope) {
        const plotGroup= findPlotGroup(pv.plotGroupId,state.plotGroupAry);
        plotViewAry= applyToOnePvOrOverlayGroup(plotViewAry, plotId, plotGroup, false,
            (pv)=> replaceAtt(pv,newAtts,toAllPlotsInPlotView) );
    }
    else {
        plotViewAry= applyToOnePvOrAll(false, plotViewAry, plotId, false,
            (pv)=> replaceAtt(pv,newAtts,toAllPlotsInPlotView) );
    }
    return {...state,plotViewAry};
}



function changeLocking(state,action) {
    const {plotId, zoomLockingEnabled, zoomLockingType:inZoomLockingType}=  action.payload;
    const zoomLockingType= UserZoomTypes.get(inZoomLockingType);
    const plotViewAry= state.plotViewAry.map( (pv) =>
                (pv.plotId===plotId) ?
                    {...pv, plotViewCtx:{...pv.plotViewCtx, zoomLockingEnabled,zoomLockingType}} : pv);
    return {...state,plotViewAry};
}


function zoomSetup(state, action) {
    const {wcsMatchType, plotViewAry, expandedMode, mpwWcsPrimId}= state;
    const {plotId, zoomLevel, userZoomType, zoomLockingEnabled, devicePt}= action.payload;
    const pvIdx=getPlotViewIdxById(state,plotId);
    const plot= pvIdx>-1 ? primePlot(plotViewAry[pvIdx]) : null;
    if (!plot) return state;
    let pv= plotViewAry[pvIdx];

    // update bookkeeping
    const plotViewCtx= {...pv.plotViewCtx};
    if (expandedMode===ExpandType.COLLAPSE) plotViewCtx.lastCollapsedZoomLevel= zoomLevel;
    if (zoomLockingEnabled && isFitFill(userZoomType))  plotViewCtx.zoomLockingType= userZoomType;


    // update zoom factor and scroll position
    const centerImagePt= isFitFill(userZoomType) ?
                          makeImagePt(plot.dataWidth/2, plot.dataHeight/2) : findCurrentCenterPoint(pv);
    const mouseOverImagePt= devicePt && CCUtil.getImageCoords(plot,devicePt);

    pv= replacePrimaryPlot(pv,clonePlotWithZoom(plot,zoomLevel));
    pv.plotViewCtx= plotViewCtx;


    if (wcsMatchType && mpwWcsPrimId!==plotId) {
        const masterPv= getPlotViewById(state, mpwWcsPrimId);
        pv= updateScrollToWcsMatch(state.wcsMatchType, masterPv, pv);
    }
    else {
        if (isImage(plot)) {
            pv= updatePlotViewScrollXY(pv,
                devicePt ?
                    findScrollPtToPlaceOnDevPt(pv, mouseOverImagePt,devicePt) :
                    findScrollPtToCenterImagePt(pv,centerImagePt));
        }
        else {
            pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
        }
    }


    if (pv.overlayPlotViews?.length) {
        pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv) => ({...oPv, plot:clonePlotWithZoom(oPv.plot,zoomLevel) }));
    }
    return {...state, plotViewAry:replacePlotView(state.plotViewAry, pv)};
}

function updateHiPSColor(state,action) {
    const {plotViewAry}= state;
    const {plotId, bias,contrast,colorTableId}= action.payload;
    let pv= getPlotViewById(state,plotId);
    const plot= primePlot(pv);
    const newPlot= {...plot,colorTableId};

    if (isNumber(bias) || isNumber(contrast)) {
        const {bandData:oldBandData}= newPlot.rawData;
        const bandData= oldBandData.map( (entry)  => ({...entry,
            bias:  isNumber(bias) ? bias : entry.bias,
            contrast:  isNumber(contrast) ? contrast : entry.contrast,
        }));
        newPlot.rawData= {...plot.rawData,bandData};
    }
    pv= replacePrimaryPlot(pv, newPlot);
    return {...state, plotViewAry : replacePlotView(plotViewAry,pv)};

}

function updateImageDisplayData(state,action) {
    const {plotViewAry, mpwWcsPrimId, wcsMatchType, plotGroupAry}= state;
    const {plotId, primaryStateJson,overlayUpdateAry, rawData,bias,contrast, useRed, useGreen, useBlue, zoomLevel:newZoomFactor, colorTableId=-1}= action.payload;
    const inPv= getPlotViewById(state,plotId);
    const inPlot= primePlot(inPv);

    let pv= {...inPv, serverCall:'success'};
    const zoomFactor= (action.type===Cntlr.ZOOM_IMAGE) ? newZoomFactor : inPlot.zoomFactor;
    pv= replacePrimaryPlot(pv,
        WebPlot.replacePlotValues(inPlot,primaryStateJson,zoomFactor, rawData,colorTableId, bias,contrast,useRed,useGreen,useBlue));
    if (action.type===Cntlr.COLOR_CHANGE && !isThreeColor(pv)) {
        const cId= primePlot(pv).colorTableId;
        pv.plots= pv.plots.map( (p) => {
            return {...p,colorTableId:cId}; //todo bias and control need to be set here
        });
    }

    if (wcsMatchType && mpwWcsPrimId!==plotId) {
        const masterPV= getPlotViewById(state, mpwWcsPrimId);
        pv= updateScrollToWcsMatch(state.wcsMatchType, masterPV, pv);
    }
    else {
        const centerImagePt= findCurrentCenterPoint(pv);
        pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
    }


    if (!isEmpty(overlayUpdateAry) ) {
        pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv) => {
            if (!oPv.plot) return oPv;

            const overlayUpdate= overlayUpdateAry.find( (u) => u.imageOverlayId===oPv.imageOverlayId);
            if (!overlayUpdate) return oPv;

            const p= WebPlot.replacePlotValues(oPv.plot,overlayUpdate.overlayStateJson);
            return {...oPv, plot:p};
        });
    }

    const plot= primePlot(pv); // get the updated on
    let newPlotGroupAry= plotGroupAry;
    if (action.type===Cntlr.STRETCH_CHANGE) {
        pv= updateForStretch(pv,plot);
        const rv= plot.plotState.getRangeValues();
        newPlotGroupAry= plotGroupAry.map( (g) => g.plotGroupId===pv.plotGroupId ? {...g,defaultRangeValues:rv} : g);
    }
    PlotPref.putCacheColorPref(pv.plotViewCtx.preferenceColorKey, plot.plotState, plot.colorTableId);

    return {...state, plotViewAry : replacePlotView(plotViewAry,pv), plotGroupAry:newPlotGroupAry};
}


function updateForStretch(inPv, plot) {
    const pv= {...inPv};
    plot.dataRequested= false;
    const rv= plot.plotState.getRangeValues();
    const hduIdx= getHDU(plot);
    const extType= getExtType(plot);

    const stretchChangeMatch= (p) => {
        if (p===plot) return false;
        if (getHDU(p)===hduIdx) return true;
        if (extType && extType===getExtType(p)) return true;
        return false;
    };

    if (!isThreeColor(pv)) {
        pv.plots= pv.plots.map((p) => {
            if (!stretchChangeMatch(p)) return p;
            const newPlotState= p.plotState.copy();
            newPlotState.setRangeValues(Band.NO_BAND,rv);
            return {...p,dataRequested:false,plotState:newPlotState};
        });
    }
    return pv;
}

function updateDisplayData(state, action) {
    const {plotId}= action.payload;
    const pv= getPlotViewById(state,plotId);
    const plot= primePlot(pv);
    return isHiPS(plot) ? updateHiPSColor(state,action) : updateImageDisplayData(state,action);
}


/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function processProjectionChange(state,action) {
    const {plotId,centerProjPt,fullSky= undefined}= action.payload;

    const {plotViewAry,wcsMatchType}= state;
    const newPlotViewAry= applyToOnePvOrAll(state.positionLock, plotViewAry, plotId, false,
         (pv)=> {
             const plot= primePlot(pv);
             if (plot) pv= replacePrimaryPlot(pv, changeHiPSProjectionCenterAndType(plot,centerProjPt,fullSky));
             return pv;
         } );
    const matchingByWcs= wcsMatchType===WcsMatchType.Standard || wcsMatchType===WcsMatchType.Target;
    let newState= {...state, plotViewAry :newPlotViewAry};
    if (matchingByWcs && centerProjPt)  {
        const imagePv= newPlotViewAry.find( (aPv) => isImage(primePlot(aPv)));
        if (imagePv) {
            const finalPvAry= recenterUsingWcsMatch(newState,imagePv,centerProjPt, false,true);
            newState= {...newState, plotViewAry :finalPvAry};
        }
    }
    return newState;
}

/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function changeHiPS(state,action) {
    const {plotId,hipsProperties, coordSys, hipsUrlRoot, cubeIdx, applyToGroup, blank= false, blankColor}= action.payload;
    let {centerProjPt}= action.payload;
    let {plotViewAry}= state;
    const {positionLock}= state;

    let pv= getPlotViewById(state, plotId);
    const originalPlot= primePlot(pv);
    if (!originalPlot) return state;

    let plot= {...originalPlot};
    let {plotViewCtx}= pv;

    // single plot stuff

    if (hipsUrlRoot) {
        plot.hipsUrlRoot= hipsUrlRoot;
        plot.blank= blank;
    }


    if (hipsProperties) {
        plot.hipsProperties= hipsProperties;
        plot.title= plot.blank ? DEFAULT_BLANK_HIPS_TITLE : getHiPsTitleFromProperties(hipsProperties);
        plot.cubeDepth= Number(hipsProperties?.hips_cube_depth) || 1;
        plot.cubeIdx= Number(hipsProperties?.hips_cube_firstframe) || 0;
        plot= replaceHiPSProjectionUsingProperties(plot, hipsProperties, getCenterOfProjection(plot) );
        if (!centerProjPt) {
            centerProjPt= convertCelestial(getCenterOfProjection(originalPlot), plot.dataCoordSys);
        }
    }

    if (!isUndefined(cubeIdx) && plot.cubeDepth>1 && cubeIdx<plot.cubeDepth) {
        plot.cubeIdx= cubeIdx;
    }
    if (!isUndefined(blankColor)) plot.blankColor= blankColor;

    if (hipsProperties || hipsUrlRoot || !isUndefined(cubeIdx)) {
        plot.plotImageId= `${pv.plotId}--${pv.plotViewCtx.plotCounter}`;
        const hipsImageConversion=(hipsUrlRoot && plotViewCtx.hipsImageConversion) ?
            {...plotViewCtx.hipsImageConversion,
                hipsRequestRoot: plotViewCtx.hipsImageConversion.hipsRequestRoot?.makeCopy({[WPConst.HIPS_ROOT_URL]:hipsUrlRoot})} :
            plotViewCtx.hipsImageConversion;
        plotViewCtx= {...plotViewCtx, plotCounter:plotViewCtx.plotCounter+1, hipsImageConversion};
    }

    pv= replacePrimaryPlot(pv, plot);
    pv.serverCall= 'success';
    pv.plottingStatusMsg= 'done';
    pv.plotViewCtx= plotViewCtx;
    plotViewAry= replacePlotView(plotViewAry,pv);

    if (coordSys) {
        plotViewAry= changeHipsCoordinateSys(plotViewAry, pv, coordSys, applyToGroup && positionLock);
        if (!centerProjPt) centerProjPt= convertCelestial(getCenterOfProjection(originalPlot), coordSys);
    }

    if (centerProjPt) {
        plotViewAry= applyToOnePvOrAll(state.positionLock, plotViewAry, plot.plotId, false,
            (pv) => {
                const p= changeHiPSProjectionCenter(primePlot(pv),centerProjPt);
                return replacePrimaryPlot(pv, p);
            });
    }

    return {...state, plotViewAry};
}


/**
 *
 * @param {Array<PlotView>} plotViewAry
 * @param {PlotView|undefined} pv
 * @param {CoordinateSys} coordSys
 * @param {boolean} applyToGroup
 * @return {Array<PlotView>}
 */
function changeHipsCoordinateSys(plotViewAry, pv, coordSys, applyToGroup) {
    return applyToOnePvOrAll(applyToGroup, plotViewAry, pv?.plotId, false,
        (pv) => {
            let plot= primePlot(pv);
            plot= replaceHiPSProjection(plot, coordSys, getCenterOfProjection(plot) );
            return replacePrimaryPlot(pv, plot);
        });
}


function processScroll(state,action) {
    const {plotId,scrollPt}= action.payload;
    const {plotGroupAry, wcsMatchType}= state;
    let {plotViewAry, mpwWcsPrimId}= state;
    plotViewAry= updatePlotGroupScrollXY(state,plotId,plotViewAry, plotGroupAry,scrollPt);

    if (wcsMatchType) mpwWcsPrimId= plotId;

    return Object.assign({},state,{plotViewAry, mpwWcsPrimId});
}


function rotatePv(pv, targetAngle, rotateNorthLock) {
    pv= {...pv, rotation:targetAngle, plotViewCtx:{...pv.plotViewCtx, rotateNorthLock}};
    return updateTransform(pv);
}

function rotatePvToMatch(pv, masterPv, rotateNorthLock) {
    if (isRotationMatching(pv,masterPv)) return pv;
    const masterPlot= primePlot(masterPv);
    const plot= primePlot(pv);
    if (!masterPlot || !plot) return pv;
    if (getRotationAngle(masterPlot)!==getRotationAngle(plot)) rotateNorthLock= false;
    const csysDirMatching= isCsysDirMatching(plot,masterPlot);
    let rotation= getMatchingRotationAngle(masterPv,pv);
    if (isEastLeftOfNorth(masterPlot)) {
        rotation= csysDirMatching ? 360-rotation :360+rotation;
    }
    else {
        rotation= csysDirMatching ? 360+rotation :360-rotation;
    }
    pv= {...pv, rotation, plotViewCtx: {...pv.plotViewCtx, rotateNorthLock}};
    return updateTransform(pv);
}



/**
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function updateClientRotation(state,action) {
    const {plotId,angle, rotateType, actionScope}= action.payload;
    const {wcsMatchType}= state;
    let   {plotViewAry,plotGroupAry}= state;

    const pv= getPlotViewById(state,plotId);
    const plot= primePlot(pv);
    if (!plot) return state;
    if (rotateType===RotateType.NORTH && isPlotNorth(plot) && !pv.rotation) return state;

    let targetAngle;
    let rotateNorthLock;

    switch (rotateType) {
        case RotateType.NORTH:
            targetAngle= 360 - getRotationAngle(plot);
            rotateNorthLock= true;
            break;
        case RotateType.ANGLE:
            targetAngle= angle ? 360-angle : 0;
            rotateNorthLock= false;
            break;
        case RotateType.UNROTATE:
            targetAngle= 0;
            rotateNorthLock= false;
            break;
    }

    targetAngle= (360 + targetAngle) % 360;
    if (!isEastLeftOfNorth(plot) && !pv.flipY) {
        targetAngle= 360-targetAngle;
    }
    else if (isEastLeftOfNorth(plot) && pv.flipY) {
        targetAngle= 360-targetAngle;
    }

    const masterPv= rotatePv(pv,targetAngle,rotateNorthLock);
    const matchingByWcs= wcsMatchType===WcsMatchType.Standard || wcsMatchType===WcsMatchType.Target;
    if (matchingByWcs || (rotateType===RotateType.NORTH && !wcsMatchType) ) {
        plotViewAry= clonePvAryWithPv(plotViewAry, masterPv);
        if (actionScope===ActionScope.GROUP) {
            plotViewAry= matchPlotViewByPositionGroup(state, masterPv,plotViewAry, false, (pv) => rotatePvToMatch(pv,masterPv, rotateNorthLock));
        }
    }
    else {
        plotViewAry= actionScope===ActionScope.GROUP ?
            applyToOnePvOrAll(state.positionLock, plotViewAry,plotId,false, (aPv) => rotatePv(aPv,targetAngle, rotateNorthLock&&aPv===pv)) :
            clonePvAryWithPv(plotViewAry, masterPv);
    }
    const newPv= getPlotViewById(plotViewAry,pv.plotId);
    plotGroupAry= plotGroupAry.map( (g) =>
        g.plotGroupId===pv.plotGroupId ? {...g, rotateNorthLockSticky:newPv.plotViewCtx.rotateNorthLock }: g);

    return {...state,plotViewAry, plotGroupAry};
}

function flipPv(pv, isY) {
    pv= {...pv};
    if (isY) {
        pv.flipY= !pv.flipY;
        if (pv.rotation!==0) pv.rotation= 360-pv.rotation;
    }
    else {
        pv.flipX= !pv.flipX;
    }
    const cc= CysConverter.make(primePlot(pv));
    if (!cc) return pv;
    const {width,height}= pv.viewDim;
    const centerOfDev= makeDevicePt(width/2,height/2);
    const ipt= cc.getImageCoords(centerOfDev);
    pv= updateTransform(pv);
    const scrollPt= findScrollPtToPlaceOnDevPt(pv,ipt,centerOfDev);
    return updatePlotViewScrollXY(pv,scrollPt);
}


/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function updateClientFlip(state,action) {
    const {plotId,isY,actionScope=ActionScope.GROUP }= action.payload;
    let   {plotViewAry, plotGroupAry}= state;

    const pv= getPlotViewById(state,plotId);
    if (!pv) return state;

    plotViewAry= actionScope===ActionScope.GROUP ?
        applyToOnePvOrAll(state.positionLock, plotViewAry,plotId,false, (pv) => flipPv(pv,isY)) :
        clonePvAryWithPv(plotViewAry, flipPv(pv,isY));

    const newPv= getPlotViewById(plotViewAry,pv.plotId);
    plotGroupAry= plotGroupAry.map( (g) => g.plotGroupId===pv.plotGroupId ? {...g, flipYSticky:newPv.flipY}: g);
    return {...state,plotViewAry, plotGroupAry};
}


function updateViewSize(state,action) {
    const {plotId,width,height}= action.payload;


    const plotViewAry= state.plotViewAry.map( (pv) => {
        if (pv.plotId!==plotId ) return pv;
        const plot= primePlot(pv);
        let centerImagePt;

        if (plot) {
            centerImagePt= (pv.scrollX===-1 && pv.scrollY===-1) ?
                makeImagePt(plot.dataWidth/2, plot.dataHeight/2) :
                findCurrentCenterPoint(pv);
        }
        const w= isUndefined(width) ? pv.viewDim.width : width;
        const h= isUndefined(height) ? pv.viewDim.height : height;
        pv= Object.assign({}, pv, {viewDim: {width:w, height:h}});
        if (!plot) return pv;

        const masterPv= getPlotViewById(state, state.mpwWcsPrimId);
        pv= updateTransform(pv);
        const hasProj= hasWCSProjection(pv) ;

        if (isHiPS(plot)) {
            centerImagePt= makeImagePt( plot.dataWidth/2, plot.dataHeight/2);
            pv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt));
        }
        else if (state.wcsMatchType===WcsMatchType.Standard && state.mpwWcsPrimId!==plotId && hasProj) {
            pv= updateScrollToWcsMatch(state.wcsMatchType, masterPv, pv);
        }
        else if (state.wcsMatchType===WcsMatchType.Target && hasProj) {
            pv= updateScrollToWcsMatch(state.wcsMatchType, masterPv, pv);
        }
        else if (state.wcsMatchType===WcsMatchType.Pixel || state.wcsMatchType===WcsMatchType.PixelCenter) {
            pv= updateScrollToWcsMatch(state.wcsMatchType, masterPv, pv);
        }
        else if (isUndefined(pv.scrollX) || isUndefined(pv.scrollY)) {
            pv= recenterPv(null, false)(pv);
        }
        else {
            pv= centerImagePt ? updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv,centerImagePt)) : recenterPv(null,false)(pv);
        }

        pv= updateTransform(pv);
        return pv;

    });
    return {...state,plotViewAry};
}




/**
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function recenter(state,action) {
    const {plotId, centerPt, centerOnImage, updateFixedTarget= false, updateWcsPrimId=true}= action.payload;
    const {wcsMatchType}= state;
    const pv= getPlotViewById(state,plotId);

    let plotViewAry;
    if (wcsMatchType) {
        plotViewAry= recenterUsingWcsMatch(state,pv,centerPt,centerOnImage, false, updateFixedTarget);
    }
    else {
        plotViewAry= applyToOnePvOrAll(state.positionLock, state.plotViewAry,plotId,false,
            recenterPv(centerPt, centerOnImage, updateFixedTarget));
    }


    return {...state,plotViewAry,
        mpwWcsPrimId:(updateWcsPrimId||!state.mpwWcsPrimId) ? plotId: state.mpwWcsPrimId};
}


function recenterUsingWcsMatch(state, pv, centerPt, centerOnImage=false, onlyImages= false, updateFixedTarget= false) {
    const {wcsMatchType}= state;
    let   {plotViewAry}= state;
    const newPv= recenterPv(centerPt, centerOnImage, updateFixedTarget)(pv);
    plotViewAry= replacePlotView(plotViewAry, newPv);
    plotViewAry= matchPlotViewByPositionGroup(state, newPv, plotViewAry, false, (pv) => {
        if (onlyImages && isHiPS(primePlot(pv))) return pv;
        if (isImage(primePlot(pv))) {
            return updateScrollToWcsMatch(wcsMatchType, newPv, pv);
        }
        else {
            return recenterPv(centerPt, centerOnImage, updateFixedTarget)(pv);
        }
    } );
    return plotViewAry;
}


/**
 * Center on the FIXED_TARGET attribute or the center of the plot or specified center point
 * @param {Point|null|undefined} centerPt center point
 * @param {boolean} centerOnImage - only used if centerPt is not defined.  If true then the centering will be
 *                  the center of the image.  If false, then the center point will be the
 *                  FIXED_TARGET attribute, if defined. Otherwise it will be the center of the image.
 * @param {boolean} updateFixedTarget if true the PlotAttribute.FIXED_TARGET will but updated to this point
 * @return {Function} a new plot view
 */
function recenterPv(centerPt,  centerOnImage, updateFixedTarget= false) {
    return (pv) => {
        const plot = primePlot(pv);
        if (!plot) return pv;
        let centerImagePt= centerPt;

        if (!centerPt) {
            let point = plot.attributes[PlotAttribute.INIT_CENTER] || plot.attributes[PlotAttribute.FIXED_TARGET] ;
            if (point && !centerOnImage) {
                if (isString(point)) point= parseAnyPt(point);
                centerImagePt = CCUtil.getImageCoords(plot, point);
            }
            else {
                centerImagePt = makeImagePt(plot.dataWidth / 2, plot.dataHeight / 2);
            }
        }

        if (isImage(primePlot(pv))) {
            const newPv= updatePlotViewScrollXY(pv, findScrollPtToCenterImagePt(pv, centerImagePt));
            if (!updateFixedTarget || centerPt.type!==Point.W_PT) return newPv;
            const newPlot= {...primePlot(pv), attributes:{...plot.attributes,[PlotAttribute.FIXED_TARGET]:centerPt}};
            return replacePrimaryPlot(newPv,newPlot);
        }
        else {
            let cp;
            if (centerPt) cp= CCUtil.getWorldCoords(plot,centerPt);
            if (!cp) cp= plot.attributes[PlotAttribute.FIXED_TARGET];
            if (!cp) cp= makeWorldPt(0,0,plot.imageCoordSys);
            const newPlot= changeHiPSProjectionCenter(plot,cp);
            if (updateFixedTarget || centerPt?.type===Point.W_PT) {
                newPlot.attributes= {...newPlot.attributes, [PlotAttribute.FIXED_TARGET]:centerPt};
            }
            return replacePrimaryPlot(pv, newPlot);
        }

    };
}

function makeNewPrimePlot(state,action) {
    const {plotId,primeIdx}= action.payload;
    let pv=  getPlotViewById(state,plotId);
    if (!pv || isEmpty(pv.plots) || pv.plots.length<=primeIdx) return state;
    pv= changePrimePlot(pv, primeIdx);
    return {...state, plotViewAry:clonePvAryWithPv(state,pv)};
}

/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @return {VisRoot}
 */
function changePositionLocking(state,action) {
    const {plotId,positionLock}=  action.payload;
    if (positionLock && plotId && isHiPS(primePlot(state,plotId))) {
        const pv= getPlotViewById(state, plotId);
        const plot= primePlot(pv);
        const plotViewAry= changeHipsCoordinateSys(state.plotViewAry, pv,plot.imageCoordSys,true );
        state= {...state, plotViewAry};
        state= processProjectionChange(state, {...action, payload:{plotId, centerProjPt:getCenterOfProjection(plot) }});
    }
    return {...state, positionLock};
}


function changeOverlayColorLocking(state,action) {
    const {plotId,overlayColorLock}=  action.payload;
    const {plotGroupId} = getPlotViewById(state,plotId);

    const pgIdx= getPlotGroupIdxById(state,plotGroupId);

    if (pgIdx < 0) return state;
    state= updateSet(state, ['plotGroupAry',pgIdx,'overlayColorLock'], overlayColorLock);
    return state;
}

function changeOverlayPlotAttributes(state,action) {
    const {plotId, imageOverlayId, attributes}= action.payload;
    const plotViewAry= state.plotViewAry
        .map( (pv) => {
            if (pv.plotId!==plotId) return pv;
            const overlayPlotViews = pv.overlayPlotViews
                 .map( (opv) => opv.imageOverlayId!==imageOverlayId ? opv : {...opv,...attributes});
            return {...pv, overlayPlotViews};
        });
    return {...state,plotViewAry};
}

function changeHipsImageConversionSettings(state,action) {
    const {plotId, hipsImageConversionChanges}= action.payload;
    const changes= {...hipsImageConversionChanges};
    const plotViewAry= state.plotViewAry
        .map( (pv) => {
            if (pv.plotId!==plotId || !pv.plotViewCtx.hipsImageConversion) return pv;
            if (changes.hipsRequestRoot) changes.hipsRequestRoot= WebPlotRequest.makeFromObj(changes.hipsRequestRoot);
            if (changes.imageRequestRoot) changes.imageRequestRoot= WebPlotRequest.makeFromObj(changes.imageRequestRoot);
            if (changes.allSkyRequest) changes.allSkyRequest= WebPlotRequest.makeFromObj(changes.allSkyRequest);
            pv= {...pv};
            pv.plotViewCtx= {...pv.plotViewCtx};
            pv.plotViewCtx.hipsImageConversion= {...pv.plotViewCtx.hipsImageConversion, ...changes};
            return pv;
        });
    return {...state,plotViewAry};

}

function markByteDataRefresh(state, action) {
    const {plotId, plotImageId, imageOverlayId}= action.payload;
    const pv= getPlotViewById(state,plotId);
    if (!pv) return state;
    let updatedPv;

    if (imageOverlayId) {
        const overlayPlotViews= pv.overlayPlotViews?.map( (oPv) => {
            if (oPv?.plot?.plotImageId!==plotImageId) return oPv;
            return {...oPv, plot:{...oPv.plot}};
        });
        updatedPv= {...pv, overlayPlotViews};
    }
    else {
        const plots= pv.plots.map( (p) => p.plotImageId===plotImageId ? {...p}: p);
        updatedPv= {...pv,plots};
    }

    const plotViewAry= replacePlotView(state.plotViewAry,updatedPv);
    return {...state, plotViewAry};
}

function requestLocalData(state, action) {
    const {plotImageId, plotId, dataRequested=true, imageOverlayId}= action.payload;
    const pv= getPlotViewById(state,plotId);
    if (!pv) return state;
    let updatedPv;
    if (imageOverlayId) {
        const overlayPlotViews= pv.overlayPlotViews.map( (oPv) => {
            if (oPv?.plot?.plotImageId!==plotImageId) return oPv;
            oPv.plot= {...oPv.plot,dataRequested};
            return oPv;
        });
        updatedPv= {...pv, overlayPlotViews};
    }
    else {
        const plots= pv.plots.map( (p) => p.plotImageId===plotImageId ? {...p,dataRequested}: p);
        updatedPv= {...pv,plots};
    }



    const plotViewAry= replacePlotView(state.plotViewAry,updatedPv);
    return {...state, plotViewAry};
}

function updatePlotProgress(state,action) {
    const {plotId, message:plottingStatusMsg, done, requestKey, callSuccess=true, allowBackwardUpdates= false}= action.payload;
    const plotView=  getPlotViewById(state,plotId);

    // validate the update
    if (!plotView) return state;
    if (requestKey!==plotView.request.getRequestKey()) return state;
    if (plotView.plottingStatusMsg===plottingStatusMsg) return state;
    if (!done && plotView.serverCall!=='working' && !allowBackwardUpdates) return state;

    // do the update
    const serverCall= done ? callSuccess ? 'success' : 'fail' : 'working';
    return {...state,plotViewAry:clonePvAry(state,plotId, {plottingStatusMsg,serverCall})};
}

function changeVisibility(state,action) {
    const {plotId, imageOverlayId, visible}= action.payload;
    if (imageOverlayId) {
        const plotViewAry= state.plotViewAry.map( (pv) => {
            if (pv.plotId!==plotId) return pv;
            const overlayPlotViews=
                pv.overlayPlotViews.map( (opv) => opv.imageOverlayId===imageOverlayId ? {...opv,visible} : opv);
            return {...pv, overlayPlotViews};
        });
        return {...state, plotViewAry};
    }
    else {
        return {...state,plotViewAry:clonePvAry(state,plotId, {visible})};
    }

}

function changeSubHighPlotView(state,action) {
    const {subHighlightAry}= action.payload;
    let anyChanged= false;

    const plotViewAry= state.plotViewAry.map( (pv) => {
        const entry= subHighlightAry.find( (e) => e.plotId===pv.plotId);
        if (!entry) return pv;
        if (pv.subHighlight===entry.subHighlight) return pv;
        anyChanged= true;
        return {...pv, subHighlight:entry.subHighlight};
    });
    return anyChanged ? {...state, plotViewAry} : state;
}
