/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {logError} from '../../util/WebUtil.js';
import ImagePlotCntlr, {IMAGE_PLOT_KEY} from '../ImagePlotCntlr.js';
import {primePlot, getPlotViewById, operateOnOthersInGroup,getPlotStateAry} from '../PlotViewUtil.js';
import { callCrop, callChangeColor, callRecomputeStretch} from '../../rpc/PlotServicesJson.js';
import WebPlotResult from '../WebPlotResult.js';
import {WebPlot} from '../WebPlot.js';



//=======================================================================
//-------------------- Action Creators ----------------------------------
//=======================================================================

/**
 * color bar Action creator
 * @param rawAction
 * @return {Function}
 */
export function colorChangeActionCreator(rawAction) {
    return (dispatcher,getState) => {
        const store= getState()[IMAGE_PLOT_KEY];
        const {plotId,cbarId}= rawAction.payload;
        const pv= getPlotViewById(store,plotId);
        if (!pv) return;


        if (!primePlot(pv).plotState.isThreeColor()) {
            doColorChange(dispatcher,store,plotId,cbarId);
        }
        operateOnOthersInGroup(store,pv, (pv) => {
            const p= primePlot(pv);
            if (p && !p.plotState.isThreeColor()) { // only do others that are not three color
                doColorChange(dispatcher,store,pv.plotId,cbarId);
            }
        });

    };

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
        const pv= getPlotViewById(store,plotId);
        const plot= primePlot(pv);
        if (!plot || !pv || !stretchData) return;


        dispatcher( { type: ImagePlotCntlr.STRETCH_CHANGE_START, payload: {plotId} } );

        const threeColor= plot.plotState.isThreeColor();
        doStretch(dispatcher,store,plotId,stretchData);
        operateOnOthersInGroup(store,pv, (pv) => {
            const p= primePlot(pv);
            if (p && p.plotState.isThreeColor()===threeColor) { // only do others that are similar
                doStretch(dispatcher,store,pv.plotId,stretchData);
            }
        });
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
        if (!plotView || !imagePt1 || !imagePt2) return;
        const p= primePlot(plotView);
        if (!p) return;
        const vr= getState()[IMAGE_PLOT_KEY];
        if (vr.wcsMatchType) dispatcher({ type: ImagePlotCntlr.WCS_MATCH, payload: {wcsMatchType:false} });

        doCrop(dispatcher,plotView,imagePt1, imagePt2, cropMultiAll);
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
 */
function doCrop(dispatcher,pv,imagePt1, imagePt2, cropMultiAll) {

    const makeSuccAction= (plotId, plotAry, overlayPlotViews) => ({
        type: ImagePlotCntlr.CROP,
        payload: {pvNewPlotInfoAry: [{plotId, plotAry, overlayPlotViews}]}
    });

    const makeFailAction= (plotId) => ({ type: ImagePlotCntlr.CROP_FAIL,
        payload: {plotId, message: 'Crop Failed', error: Error('crop: payload failed')}
    });

    dispatcher( { type: ImagePlotCntlr.CROP_START, payload: {plotId:pv.plotId, message:'Cropping...'} } );
    callCrop(getPlotStateAry(pv), imagePt1, imagePt2, cropMultiAll)
    .then( (wpResult) => processPlotReplace(dispatcher,wpResult,pv,makeSuccAction, makeFailAction))
        .catch ( (e) => { dispatcher(makeFailAction(pv.plotId) );
            logError(`plot error, rotate , plotId: ${pv.plotId}`, e);
        });
}



function doStretch(dispatcher,store,plotId,stretchData) {

    const plot= primePlot(store,plotId);
    dispatcher( { type: ImagePlotCntlr.STRETCH_CHANGE_START, payload: {plotId, message:'Changing Stretch...'} } );
    callRecomputeStretch(plot.plotState,stretchData)
        .then( (wpResult) => processPlotUpdate(dispatcher,plotId,wpResult,
                                   ImagePlotCntlr.STRETCH_CHANGE, ImagePlotCntlr.STRETCH_CHANGE_FAIL) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.STRETCH_CHANGE_FAIL, 
                          payload: {plotId, message: 'Stretch Failed', stretchData, error:e} } );
            logError(`plot error, stretch change, plotId: ${plot.plotId}`, e);
        });
}



function doColorChange(dispatcher,store,plotId,cbarId) {

    const plot= primePlot(store,plotId);
    dispatcher( { type: ImagePlotCntlr.COLOR_CHANGE_START, payload: {plotId, message:'Changing Color...'} } );
    callChangeColor(plot.plotState,cbarId)
        .then( (wpResult) => processPlotUpdate(dispatcher,plotId,wpResult,
                                      ImagePlotCntlr.COLOR_CHANGE, ImagePlotCntlr.COLOR_CHANGE_FAIL) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.COLOR_CHANGE_FAIL, 
                          payload: {plotId, message: 'Color change Failed', cbarId, error:e} } );
            logError(`plot error, color change, plotId: ${plot.plotId}`, e);
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
            let plotAry = resultAry[0].data[WebPlotResult.PLOT_CREATE].map((wpInit) => makePlot(wpInit, pv));
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
        return [{success:true, data:{PlotCreate:result.PlotCreate}}];
    }
    else if (result[WebPlotResult.RESULT_ARY]) {
        return result[WebPlotResult.RESULT_ARY];
    }
    else {
        throw Error('Could not find PlotCreate data');
    }
}


function makePlot(wpInit,pv) {
    const plot= WebPlot.makeWebPlotData(pv.plotId, wpInit, primePlot(pv).attributes);
    plot.title= primePlot(pv).title;
    return plot;
}

/**
 *
 * @param dispatcher
 * @param plotId
 * @param result
 * @param succActionType
 * @param failActionType
 */
function processPlotUpdate(dispatcher, plotId, result, succActionType, failActionType) {
    if (result.success) {
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

