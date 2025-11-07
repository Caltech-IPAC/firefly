/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNumber, isArray, set} from 'lodash';
import {logger} from '../../util/Logger.js';
import ImagePlotCntlr, { IMAGE_PLOT_KEY, dispatchWcsMatch, ActionScope, visRoot} from '../ImagePlotCntlr.js';
import {
    primePlot,
    getPlotViewById,
    operateOnOthersInOverlayColorGroup,
    getPlotStateAry,
    isThreeColor,
    findPlot,
    getOverlayById
} from '../PlotViewUtil.js';
import {callCrop} from '../../rpc/PlotServicesJson.js';
import {WebPlotResult} from '../WebPlotResult.js';
import {isHiPS, isImage, WebPlot} from '../WebPlot.js';
import {locateOtherIfMatched} from './WcsMatchTask';
import {PlotAttribute} from '../PlotAttribute.js';
import PlotState from '../PlotState.js';
import {RangeValues} from '../RangeValues.js';
import {loadStretchData, queueChangeLocalRawDataColor} from '../rawData/RawDataOps.js';
import {dispatchAddWorkingTask} from '../../core/AppDataCntlr.js';
import {Band} from '../Band.js';
import {makeCubeCtxAry, populateFromHeader} from 'firefly/visualize/task/CreateTaskUtil.js';
import {parseAnyPt} from 'firefly/visualize/Point';


//=======================================================================
//-------------------- Action Creators ----------------------------------
//=======================================================================

export function requestLocalDataActionCreator(rawAction) {
    return (dispatcher) => {
        const {plotId, plotImageId, dataRequested= true, imageOverlayId}=rawAction.payload;
        const pv= getPlotViewById(visRoot(),plotId);
        const plot= imageOverlayId ? getOverlayById(pv,imageOverlayId)?.plot : findPlot(pv,plotImageId);
        if (!isImage(plot)) return;
        if (dataRequested===false) {
            dispatcher(rawAction);
            return;
        }
        if (plot.dataRequested) return;
        dispatcher(rawAction);
        loadStretchData(pv, plot,dispatcher);
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


export const recenterActionCreator = (rawAction) => {
    // `recenter` reducer expects centerPt to be a Point object, so parse centrePt because it may be a serialised string
    return dispatchAndMaybeMatch(set(rawAction,'payload.centerPt', parseAnyPt(rawAction.payload.centerPt)));
};

export const processScrollActionCreator= (rawAction) => dispatchAndMaybeMatch(rawAction);
export const rotateActionCreator= (rawAction) => dispatchAndMaybeMatch(rawAction);





/**
 * color bar Action creator
 * @param rawAction
 * @return {Function}
 */
export function colorChangeActionCreator(rawAction) {
    return (dispatcher,getState) => {
        const store= getState()[IMAGE_PLOT_KEY];
        const {plotId}= rawAction.payload;
        const pv= getPlotViewById(store,plotId);
        if (!pv) return;

        isHiPS(primePlot(pv))
            ? colorChangeHiPS(store, dispatcher, rawAction.payload)
            : colorChangeImage(store,dispatcher,rawAction.payload);
    };

}

function colorChangeImage(store,dispatcher,payload) {

    const onComplete= (plotId, abort, colorChangeResults) => {
        if (abort) return;
        const {bias, contrast, colorTableId, nanPixelColor, bandUse}= colorChangeResults;
        dispatcher( {
            type: ImagePlotCntlr.COLOR_CHANGE,
            payload: { plotId, bias, contrast, colorTableId, nanPixelColor, ...bandUse }
        });
        dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[plotId]}} );
    };


    const {plotId,cbarId,nanPixelColor, useRed=true, useGreen=true, useBlue=true}= payload;
    const pv= getPlotViewById(store,plotId);
    const plot= primePlot(pv);
    const basePlotThreeColor= isThreeColor(pv);
    const {bias,contrast}= getBiasContrast(pv,payload.bias, payload.contrast);
    const colorParams= {plot,bias,contrast, colorTableId:cbarId, nanPixelColor, onComplete};
    const bandUse= {useRed,useGreen,useBlue};

    const promise= basePlotThreeColor
        ? queueChangeLocalRawDataColor({...colorParams, colorTableId:0, bandUse})
        : queueChangeLocalRawDataColor(colorParams);
    dispatchAddWorkingTask(plotId,promise);
    if (payload.actionScope!==ActionScope.SINGLE) {
        operateOnOthersInOverlayColorGroup(store,pv, (itemPv) => {
            const p= primePlot(itemPv);
            if (!p) return;
            if (isThreeColor(p)!==basePlotThreeColor) return;
            const promise= isThreeColor(p)
                ? queueChangeLocalRawDataColor({...colorParams,plot:p,colorTableId:0, bandUse})
                : queueChangeLocalRawDataColor({...colorParams,plot:p});
            dispatchAddWorkingTask(p.plotId,promise);
        });
    }
}


function colorChangeHiPS(store, dispatcher, payload) {
    const {plotId,cbarId,actionScope}= payload;
    const pv= getPlotViewById(store,plotId);
    const plot= primePlot(pv);
    const {bias,contrast}= getBiasContrast(pv,payload.bias, payload.contrast);

    const doDispatch= (plotId) => {
        dispatcher( {
            type:ImagePlotCntlr.COLOR_CHANGE,
            payload: { plotId, colorTableId: cbarId, bias, contrast}});
        dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[plotId]}} );
    };

    doDispatch(plotId,plot.plotState);
    if (actionScope!==ActionScope.SINGLE){
        operateOnOthersInOverlayColorGroup(store, pv, (pv) => {
            const plot= primePlot(pv);
            doDispatch(plot.plotId);
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
            doStretchDispatch(dispatcher,getState, store,plotId,stretchData);
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



function doStretchDispatch(dispatcher,getState, store,plotId,stretchData) {

    const plot= primePlot(store,plotId);
    const plotState= plot.plotState.copy();
    stretchData.forEach( (sd) =>  plotState.setRangeValues(Band.get(sd.band),RangeValues.parse(sd.rv)));

    dispatcher( {
        type: ImagePlotCntlr.STRETCH_CHANGE,
        payload: { plotId, primaryStateJson : PlotState.convertToJSON(plotState) }
    });
    dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[plotId]}} );

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
                const plot = WebPlot.makeWebPlotData({plotId:imageOverlayId, viewDim:pv.viewDim,
                    wpInit:r.data[WebPlotResult.PLOT_CREATE][0],  asOverlay:true});
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
    const {plotId,viewDim}= pv;
    const plot= WebPlot.makeWebPlotData({plotId, viewDim,wpInit:pc,cubeCtx,
        attributes:{...oldPlot.attributes,
            [PlotAttribute.IMAGE_BOUNDS_SELECTION]:undefined,
            [PlotAttribute.SELECTION]: undefined,
            [PlotAttribute.SELECTION_SOURCE]: undefined
        },
    });
    plot.title= oldPlot.title;
    plot.colorTableId= oldPlot.colorTableId;
    return plot;
}


function getBiasContrast(pv,bias,contrast) {
    const threeC= isThreeColor(pv);
    let biasToUse= threeC ? [.5,.5,.5] : .5;
    let contrastToUse=threeC ? [1,1,1] : 1;

    if (threeC) {
        if (isArray(bias)) biasToUse= bias.map( (b) => (b>1) ? 1 : b< 0 ? 0 : b);
        if (isArray(contrast)) contrastToUse= contrast.map( (c) => (c>10) ? 10 : c< 0 ? 0 : c);
    }
    else {
        if (isNumber(bias)) biasToUse= (bias>1) ? 1 : bias < 0 ? 0 : bias;
        if (isNumber(contrast)) contrastToUse= (contrast>10) ? 10 : contrast < 0 ? 0 : contrast;
    }
    return {bias:biasToUse,contrast:contrastToUse};
}
