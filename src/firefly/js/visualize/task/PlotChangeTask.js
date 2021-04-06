/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNumber, isArray} from 'lodash';
import {logger} from '../../util/Logger.js';
import ImagePlotCntlr, { IMAGE_PLOT_KEY, dispatchWcsMatch, ActionScope, visRoot} from '../ImagePlotCntlr.js';
import {
    primePlot,
    getPlotViewById,
    operateOnOthersInOverlayColorGroup,
    getPlotStateAry,
    isThreeColor, findPlot, canLoadStretchData, hasClearedByteData, hasLocalStretchByteData, hasLocalRawData
} from '../PlotViewUtil.js';
import {callCrop, callChangeColor, callRecomputeStretch} from '../../rpc/PlotServicesJson.js';
import {WebPlotResult} from '../WebPlotResult.js';
import {isHiPS, WebPlot} from '../WebPlot.js';
import {makeCubeCtxAry, populateFromHeader} from './PlotImageTask';
import {locateOtherIfMatched} from './WcsMatchTask';
import {PlotAttribute} from '../PlotAttribute.js';
import PlotState from '../PlotState.js';
import {RangeValues} from '../RangeValues.js';
import {
    loadStretchData, queueChangeLocalRawDataColor,
    stretchRawData
} from '../rawData/RawDataOps.js';
import {dispatchAddTaskCount, dispatchRemoveTaskCount} from '../../core/AppDataCntlr.js';
import {Band} from '../Band.js';


//=======================================================================
//-------------------- Action Creators ----------------------------------
//=======================================================================

export function requestLocalDataActionCreator(rawAction) {
    return (dispatcher,getState) => {
        const {plotId, plotImageId, dataRequested}=rawAction.payload;
        const pv= getPlotViewById(visRoot(),plotId);
        const plot= findPlot(pv,plotImageId);
        if (!canLoadStretchData(plot)) return;
        if (dataRequested===false) {
            dispatcher(rawAction);
            return;
        }
        if (plot.dataRequested) return;
        dispatcher(rawAction);
        // loadRawData(plot,dispatcher);
        // loadRawData(plot,dispatcher);
        loadStretchData(plot,dispatcher);
    };
}

export function flipActionCreator(rawAction) {
    return (dispatcher,getState) => {
        const {plotId, rematchAfterFlip}=rawAction.payload;
        dispatcher(rawAction);
        if (!rematchAfterFlip) return;
        const matchType= getState()[IMAGE_PLOT_KEY].wcsMatchType;
        if (matchType) {
            dispatchWcsMatch({plotId,matchType});
        }
    };
}

const dispatchAndMaybeMatch= (rawAction) => (dispatcher,getState) => {
        dispatcher(rawAction);
        locateOtherIfMatched(getState()[IMAGE_PLOT_KEY], rawAction.payload.plotId);
    };


export const recenterActionCreator= (rawAction) => dispatchAndMaybeMatch(rawAction);
export const processScrollActionCreator= (rawAction) => dispatchAndMaybeMatch(rawAction);
export const rotateActionCreator= (rawAction) => dispatchAndMaybeMatch(rawAction);



let taskCnt= 0;
export function makeTaskId() {
    taskCnt++;
    return `plot_change_task-${taskCnt}`;
}



/**
 * color bar Action creator
 * @param rawAction
 * @return {Function}
 */
export function colorChangeActionCreator(rawAction) {
    return (dispatcher,getState) => {
        const store= getState()[IMAGE_PLOT_KEY];
        const {plotId,cbarId,bias,contrast, useRed=true, useGreen=true, useBlue=true}= rawAction.payload;
        const pv= getPlotViewById(store,plotId);
        const plot= primePlot(pv);
        const basePlotThreeColor= isThreeColor(pv);

        let biasToUse= basePlotThreeColor ? [.5,.5,.5] : .5;
        let contrastToUse=basePlotThreeColor ? [1,1,1] : 1;

        if (!pv) return;


        if (basePlotThreeColor) {
            if (isArray(bias)) biasToUse= bias.map( (b) => (b>1) ? 1 : b< 0 ? 0 : b);
            if (isArray(contrast)) contrastToUse= contrast.map( (c) => (c>10) ? 10 : c< 0 ? 0 : c);
        }
        else {
            if (isNumber(bias)) biasToUse= (bias>1) ? 1 : bias < 0 ? 0 : bias;
            if (isNumber(contrast)) contrastToUse= (contrast>10) ? 10 : contrast < 0 ? 0 : contrast;
        }

        if (isHiPS(plot)) {
            colorChangeHiPS(store, dispatcher, plotId, cbarId, biasToUse,contrastToUse, rawAction.payload.actionScope);
            return;
        }


        const aType= ImagePlotCntlr.COLOR_CHANGE;
        if (rawAction.payload.actionScope===ActionScope.SINGLE){
            const local=hasLocalStretchByteData(plot);
            const taskId= makeTaskId();
            if (!isThreeColor(plot)) {
                local && dispatchAddTaskCount(plot.plotId,taskId, true);
                local ?
                    queueChangeLocalRawDataColor(plot,cbarId,biasToUse,contrastToUse, undefined, makeOnComplete(dispatcher, plotId, taskId,aType)) :
                    doColorChange(dispatcher,getState, store,plotId,cbarId);
            }
            else {
                if (!local) return;
                queueChangeLocalRawDataColor(plot,0,biasToUse,contrastToUse, {useRed,useGreen,useBlue},
                    makeOnComplete(dispatcher, plotId, taskId,aType));


            }
        }
        else {
            const taskId= makeTaskId();
            const local=hasLocalStretchByteData(plot);
            if (!isThreeColor(plot)) {
                local && dispatchAddTaskCount(plot.plotId,taskId, true);
                local ?
                    queueChangeLocalRawDataColor(plot,cbarId,biasToUse,contrastToUse, undefined, makeOnComplete(dispatcher, plotId, taskId,aType)) :
                    doColorChange(dispatcher,getState, store,plotId,cbarId);
                //all other plots
            }
            else {
                if (!local) return;
                queueChangeLocalRawDataColor(plot,0,biasToUse,contrastToUse, {useRed,useGreen,useBlue},
                    makeOnComplete(dispatcher, plotId, taskId,aType));
            }
            operateOnOthersInOverlayColorGroup(store,pv, (pv) => {
                const p= primePlot(pv);
                if (!p) return;
                if (isThreeColor(p)!==basePlotThreeColor) return;
                const local=hasLocalStretchByteData(p);
                const taskId= makeTaskId();
                if (!isThreeColor(p)) { // only do others that are not three color
                    local && dispatchAddTaskCount(p.plotId,taskId, true);
                    local ?
                        queueChangeLocalRawDataColor(p,cbarId,biasToUse,contrastToUse, undefined, makeOnComplete(dispatcher, pv.plotId, taskId, aType)) :
                        doColorChange(dispatcher,getState, store,pv.plotId,cbarId);
                }
                else {
                    if (!local) return;
                    queueChangeLocalRawDataColor(p,0,biasToUse,contrastToUse, {useRed,useGreen,useBlue},
                        makeOnComplete(dispatcher, pv.plotId, taskId, aType));

                }
            });

        }


    };

}


function colorChangeHiPS(store, dispatcher, plotId, cbarId, biasToUse,contrastToUse, actionScope) {

    const doDispatch= (plotId, oldPlotState) => {
        const newPlotState= oldPlotState.copy();
        newPlotState.colorTableId = cbarId;
        dispatcher( {
            type:ImagePlotCntlr.COLOR_CHANGE,
            payload: {
                plotId,
                primaryStateJson : PlotState.convertToJSON(newPlotState,true),
                bias: biasToUse,
                contrast : contrastToUse
            }});
        dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[plotId]}} );
    };

    const pv= getPlotViewById(store,plotId);
    const plot= primePlot(pv);
    doDispatch(plotId,plot.plotState);
    if (actionScope!==ActionScope.SINGLE){
        operateOnOthersInOverlayColorGroup(store, pv, (pv) => {
            const plot= primePlot(pv);
            doDispatch(plot.plotId,plot.plotState);
        });
    }
}







/**
 * color bar Action creator
 * @param rawAction
 * @return {Function}
 */
export function stretchChangeActionCreator(rawAction) {
    return (dispatcher,getState) => {
        const store= getState()[IMAGE_PLOT_KEY];
        const {plotId,stretchData}= rawAction.payload;
        const plotView= getPlotViewById(store,plotId);
        const plot= primePlot(plotView);
        if (!plot || !plotView || !stretchData) return;

        const rvAry= [];
        stretchData.forEach( (sdE) => {
            rvAry[Band.get(sdE.band).value]= RangeValues.parse(stretchData[0].rv);
        });


        const doStretch= async (stretchPlot) => {
            if (!stretchPlot) return;
            const {plotId}= stretchPlot;
            if (hasLocalRawData(stretchPlot)) {
                const taskId= makeTaskId();
                dispatchAddTaskCount(plotId,taskId, true);
                processPlotUpdateLocal(dispatcher, plotId, taskId, stretchRawData(stretchPlot,rvAry), ImagePlotCntlr.STRETCH_CHANGE);
            }
            else if (hasLocalStretchByteData(stretchPlot) || hasClearedByteData(stretchPlot)){
                await doStretchServerCall(dispatcher,getState, store,plotId,stretchData);
            }
            else {
                doStretchServerCall(dispatcher,getState, store,plotId,stretchData);
            }
        };


        const threeColor= isThreeColor(plotView);
        doStretch(plot,dispatcher,getState,store,stretchData);
        operateOnOthersInOverlayColorGroup(store,plotView, (pv) =>
             (isThreeColor(pv)===threeColor) && doStretch(primePlot(pv),dispatcher,getState,store,stretchData));// only do others that are similar
    };
}



/**
 * @param rawAction
 * @return {Function}
 */
export function cropActionCreator(rawAction) {
    return (dispatcher,getState) => {
        const store= getState()[IMAGE_PLOT_KEY];
        const { plotId, imagePt1, imagePt2, cropMultiAll}= rawAction.payload;
        const plotView= getPlotViewById(store,plotId);
        if (!plotView || !primePlot(plotView) || !imagePt1 || !imagePt2) return;
        if (store.wcsMatchType) dispatchWcsMatch({plotId, matchType:false});
        doCrop(dispatcher,plotView,imagePt1, imagePt2, cropMultiAll,store.wcsMatchType);
    };
}



//=======================================================================
//-------------------- End Action Creators -----------------------------
//=======================================================================


/**
 *
 * @param dispatcher
 * @param pv plot view
 * @param imagePt1
 * @param imagePt2
 * @param cropMultiAll
 * @param originalWcsMatchType
 */
function doCrop(dispatcher,pv,imagePt1, imagePt2, cropMultiAll, originalWcsMatchType) {

    const makeSuccAction= (plotId, plotAry, overlayPlotViews) => ({
        type: ImagePlotCntlr.CROP,
        payload: {pvNewPlotInfoAry: [{plotId, plotAry, overlayPlotViews}]}
    });

    const makeFailAction= (plotId) => ({ type: ImagePlotCntlr.CROP_FAIL,
        payload: {plotId, message: 'Crop Failed', error: Error('crop: payload failed')}
    });

    dispatcher( { type: ImagePlotCntlr.CROP_START, payload: {plotId:pv.plotId, message:'Cropping...'} } );
    callCrop(getPlotStateAry(pv), imagePt1, imagePt2, cropMultiAll)
        .then( (wpResult) => {
            processPlotReplace(dispatcher,wpResult,pv,makeSuccAction, makeFailAction);
            originalWcsMatchType && dispatchWcsMatch({plotId:pv.plotId, matchType:originalWcsMatchType});
        })
        .catch ( (e) => { dispatcher(makeFailAction(pv.plotId) );
            logger.error(`plot error, rotate , plotId: ${pv.plotId}`, e);
            });
}



async function doStretchServerCall(dispatcher,getState, store,plotId,stretchData) {

    const plot= primePlot(store,plotId);
    dispatcher( { type: ImagePlotCntlr.STRETCH_CHANGE_START, payload: {plotId, message:'Changing Stretch...'} } );
    return callRecomputeStretch(plot.plotState,stretchData)
        .then( (wpResult) => processPlotUpdateServer(dispatcher,getState, store, plotId,wpResult,
                                   ImagePlotCntlr.STRETCH_CHANGE, ImagePlotCntlr.STRETCH_CHANGE_FAIL) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.STRETCH_CHANGE_FAIL, 
                          payload: {plotId, message: 'Stretch Failed', stretchData, error:e} } );
            logger.error(`plot error, stretch change, plotId: ${plot.plotId}`, e);
        });
}



function doColorChange(dispatcher,getState, store,plotId,cbarId) {

    const plot= primePlot(store,plotId);
    dispatcher( { type: ImagePlotCntlr.COLOR_CHANGE_START, payload: {plotId, message:'Changing Color...'} } );
    callChangeColor(plot.plotState,cbarId)
        .then( (wpResult) => processPlotUpdateServer(dispatcher,getState, store, plotId,wpResult,
                                      ImagePlotCntlr.COLOR_CHANGE, ImagePlotCntlr.COLOR_CHANGE_FAIL) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.COLOR_CHANGE_FAIL, 
                          payload: {plotId, message: 'Color change Failed', cbarId, error:e} } );
            logger.error(`plot error, color change, plotId: ${plot.plotId}`, e);
        });
}


/**
 *
 * @param dispatcher
 * @param result
 * @param pv
 * @param makeSuccessAction
 * @param makeFailAction
 */
function processPlotReplace(dispatcher, result, pv, makeSuccessAction, makeFailAction) {
    let successSent = false;
    if (result.success) {
        const resultAry= getResultAry(result);

        if (resultAry[0].success) {
            const {PlotCreateHeader:plotCreateHeader, PlotCreate:plotCreate}=resultAry[0].data;
            populateFromHeader(plotCreateHeader, plotCreate);
            const cubeCtxAry= makeCubeCtxAry(plotCreate);
            let plotAry = plotCreate.map((pc,idx) => makeCroppedPlot(pc, plotCreateHeader, pv, cubeCtxAry[idx]));
            if (plotAry.length===1 && pv.plots.length>1) {
                const newP= plotAry[0];
                plotAry= pv.plots.map( (p,idx) => idx===pv.primeIdx ? newP : p);
            }

            const existingOverlayPlotViews = pv.overlayPlotViews.filter((opv) => opv.plot);
            const overlayPlotViews = [];
            resultAry.forEach((r, i) => {
                if (i === 0) return;
                const {imageOverlayId}= existingOverlayPlotViews[i-1];
                const plot = WebPlot.makeWebPlotData(imageOverlayId, r.data[WebPlotResult.PLOT_CREATE][0], {}, true);
                overlayPlotViews[i - 1] = {plot};
            });

            dispatcher( makeSuccessAction(pv.plotId, plotAry, overlayPlotViews));
            dispatcher({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotIdAry: [pv.plotId]}});
            successSent = true;

        }
    }
    if (!successSent) dispatcher( makeFailAction(pv.plotId));
}

function getResultAry(result) {
    if (result.PlotCreate) {
        return [{success:true, data:{PlotCreate:result.PlotCreate, PlotCreateHeader: result.PlotCreateHeader}}];
    }
    else if (result[WebPlotResult.RESULT_ARY]) {
        return result[WebPlotResult.RESULT_ARY];
    }
    else {
        throw Error('Could not find PlotCreate data');
    }
}


function makeCroppedPlot(pc,plotCreateHeader, pv, cubeCtx) {
    const oldPlot= primePlot(pv);
    const plot= WebPlot.makeWebPlotData(pv.plotId, pc,
        {...oldPlot.attributes,
            [PlotAttribute.IMAGE_BOUNDS_SELECTION]:undefined,
            [PlotAttribute.SELECTION]: undefined},
    false, cubeCtx);
    plot.title= oldPlot.title;
    return plot;
}

/**
 *
 * @param dispatcher
 * @param {Function} getState
 * @param {VisRoot} oldStore
 * @param plotId
 * @param result
 * @param succActionType
 * @param failActionType
 */
function processPlotUpdateServer(dispatcher, getState, oldStore, plotId, result, succActionType, failActionType) {
    if (result.success) {

        const currentVisRoot= getState()[IMAGE_PLOT_KEY];
        const originalPlot= primePlot(oldStore,plotId);
        const plot= primePlot(currentVisRoot,plotId);
        if (originalPlot.plotImageId!==plot?.plotImageId) {
            return; //abort: plot has been replaced since this update was started
        }

        dispatcher( {
            type: succActionType,
            payload: {
                plotId,
                primaryStateJson : result[WebPlotResult.PLOT_STATE],
                primaryTiles : result[WebPlotResult.PLOT_IMAGES]
            }});
        dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[plotId]}} );
    }
    else {
        dispatcher( { type: failActionType,
            payload: {plotId, error:Error('payload failed: '+ failActionType)} } );
    }
}

function processPlotUpdateLocal(dispatcher, plotId, taskId, rawDataPromise, succActionType) {
    rawDataPromise.then( (rawData) => {
        plotUpdateLocal(dispatcher, plotId, taskId, succActionType, rawData);
    });
}


function makeOnComplete(dispatcher, plotId, taskId, succActionType)  {
    return (abort, rawData) => {
        if (abort) {
            dispatchRemoveTaskCount(plotId,taskId);
            return;
        }
        plotUpdateLocal(dispatcher, plotId, taskId, succActionType, rawData);
    };
}

function plotUpdateLocal(dispatcher, plotId, taskId, succActionType, rawData) {
    dispatchRemoveTaskCount(plotId,taskId);
    dispatcher( {
        type: succActionType,
        payload: {
            plotId,
            primaryStateJson : PlotState.convertToJSON(rawData.plotState,true),
            bias: rawData.bias,
            contrast : rawData.contrast,
            ...rawData.bandUse
        }});
    dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[plotId]}} );
}