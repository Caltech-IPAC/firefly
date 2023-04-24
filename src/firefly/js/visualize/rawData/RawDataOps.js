import {isArray, isArrayBuffer} from 'lodash';
import {Band} from '../Band.js';
import {findPlot, getOverlayById, getPlotViewById, isThreeColor, primePlot} from '../PlotViewUtil.js';
import {createCanvas, isImageBitmap, MEG} from '../../util/WebUtil.js';
import ImagePlotCntlr, {dispatchRequestLocalData, visRoot} from '../ImagePlotCntlr.js';
import {PlotState} from '../PlotState.js';
import {getNextWorkerKey, postToWorker} from '../../threadWorker/WorkerAccess.js';
import {addRawDataToCache, CLEARED, getEntry, STRETCH_ONLY} from './RawDataCache.js';
import {getColorModel} from './rawAlgorithm/ColorTable.js';
import {getGPUOps} from './RawImageTilesGPU.js';
import {getGpuJs} from './GpuJsConfig.js';
import {
    makeAbortFetchAction, makeColorAction, makeRetrieveStretchByteDataAction,
} from './RawDataThreadActionCreators.js';
import {FULL, HALF, MAX_FULL_DATA_SIZE, QUARTER, shouldUseGpuInWorker} from './RawDataCommon.js';
import {makeThumbnailCanvas} from 'firefly/visualize/rawData/RawTileDrawer.js';
import {Logger} from 'firefly/util/Logger.js';

const nextColorChangeParams= new Map();
const colorChangeDonePromises= new Map();
const imageIdsRequested= new Map();
const QUARTER_ZOOM_FACT= .15;
const HALF_ZOOM_FACT= .42;


/**
 *
 * @param {ImageBitmap|HTMLCanvasElement|ArrayBuffer} buffer
 * @param {number} width
 * @param {number} height
 * @return {HTMLCanvasElement}
 */
function createTileFromImageData(buffer, width,height) {
    if (buffer instanceof HTMLCanvasElement) return buffer;
    const c= createCanvas(width,height);
    if (isImageBitmap(buffer)) {
        c.getContext('2d').drawImage(buffer,0,0);
    }
    else {
        c.getContext('2d').putImageData(new ImageData(new Uint8ClampedArray(buffer),width,height),0,0);
    }
    return c;
}



function* rawTileGenerator(rawTileDataGroup, colorTableId, mask, maskColor, bias, contrast, bandUse, GPU) {
    const {rawTileDataAry}= rawTileDataGroup;
    const newRawTileDataAry= [];
    const gpu= getGPUOps(GPU);
    for(let i=0; (i<rawTileDataAry.length);i++) {
        const inData= rawTileDataAry[i];
        const {pixelData3C, pixelDataStandard,workerTmpTile, width,height}= rawTileDataAry[i];
        let tile;

        if (isArrayBuffer(pixelDataStandard) || pixelData3C?.some( (a) => isArrayBuffer(a)) ) {
            tile= gpu.createTileWithGPU(rawTileDataAry[i],getColorModel(colorTableId),isArray(pixelData3C), mask, maskColor, bias, contrast,bandUse);
        }
        else {
            tile= createTileFromImageData(workerTmpTile, width,height);
        }
        newRawTileDataAry[i]= {...inData, workerTmpTile: undefined, rawImageTile:tile};
        if (i<rawTileDataAry.length-1) yield;
    }
    return {...rawTileDataGroup, rawTileDataAry:newRawTileDataAry, colorTableId};
}


async function populateTilesAsync(rawTileDataGroup, colorTableId,mask, maskColor, bias,contrast, bandUse) {
    const chunkSize= 5;
    const GPU= await getGpuJs();
    const gen= rawTileGenerator(rawTileDataGroup,colorTableId, mask, maskColor, bias, contrast, bandUse, GPU);
    return new Promise((resolve, reject) => {
        const id= setInterval( () => {

            let result= {done:false};
            for(let i=0; (i<chunkSize && !result.done); i++) {
                result= gen.next();
                if (!result.done) return;
            }
            clearInterval(id);
            result.value ? resolve(result.value) : reject();
        },0);
    });
}

const defBandUse= {useRed:true,useGreen:true,useBlue:true};

/**
 * @typedef {Object} ChangeColorResults
 * @prop {PlotState} plotState
 * @prop {number} bias
 * @prop {number} contrast
 * @prop {{useRed:boolean,useBlue:boolean,useGreen:boolean}} bandUse
 */

/**
 *
 * @param plot
 * @param colorTableId
 * @param bias
 * @param contrast
 * @param bandUse
 * @param onComplete function to call with rawData object when done, note this call will only happen if is is not overridden by another call
 * @return {Promise<ChangeColorResults>}
 */
export function queueChangeLocalRawDataColor(plot, colorTableId, bias, contrast, bandUse=defBandUse, onComplete) {
    const {plotImageId}= plot;
    const entry = getEntry(plotImageId);
    if (!entry) return;
    const p= colorChangeDonePromises.get(plotImageId);
    if (!entry.colorChangingInProgress || !p) {
        changeLocalRawDataColor(plot,colorTableId,bias,contrast,bandUse)
            .then( (colorChangeResults) => onComplete(false, colorChangeResults));
        return;
    }
    if (nextColorChangeParams.has(plotImageId)) {
        nextColorChangeParams.get(plotImageId).onComplete(true);
    }
    else {
       p.then( () => {
           if (nextColorChangeParams.has(plotImageId) && getPlotViewById(visRoot(),plot.plotId)) {
               const {plot, colorTableId, bias, contrast, onComplete}= nextColorChangeParams.get(plotImageId);
               nextColorChangeParams.delete(plotImageId);
               changeLocalRawDataColor(plot,colorTableId,bias,contrast,bandUse)
                   .then( (colorChangeResults) => onComplete(false, colorChangeResults));
           }
       });
    }
    nextColorChangeParams.set(plotImageId,{plot, colorTableId,bias, contrast, onComplete});
}

/**
 * color change needs to do the following
 * @param {WebPlot} plot
 * @param {number} colorTableId
 * @param {number} bias
 * @param {number} contrast
 * @param bandUse
 * @return {ChangeColorResults}
 */
export async function changeLocalRawDataColor(plot, colorTableId, bias, contrast, bandUse=defBandUse) {
    const entry = getEntry(plot.plotImageId);
    if (!entry) return {};

    let plotStateSerialized;
    let rawTileDataGroup;
    entry.colorChangingInProgress= true;

    let donePromiseResolve;

    const donePromise= new Promise( (resolve) => {
        donePromiseResolve= resolve;
    });
    colorChangeDonePromises.set(plot.plotImageId, donePromise);

    if (shouldUseGpuInWorker()) {
        const colorResult= await postToWorker(makeColorAction(plot,colorTableId,bias,contrast,bandUse, entry.workerKey));
        plotStateSerialized = colorResult.plotStateSerialized;
        rawTileDataGroup= colorResult.rawTileDataGroup;
    }
    else {
        const newPlotState = plot.plotState.copy();
        plotStateSerialized= newPlotState.toJson(true);
        rawTileDataGroup= entry.rawTileDataGroup;
    }
    entry.rawTileDataGroup = await populateTilesAsync(rawTileDataGroup, colorTableId, undefined, undefined, bias,contrast, bandUse);
    entry.thumbnailEncodedImage = makeThumbnailCanvas(plot);
    entry.colorChangingInProgress= false;
    colorChangeDonePromises.delete(plot.plotImageId);
    donePromiseResolve?.();
    const plotState= PlotState.parse(plotStateSerialized);
    return {plotState, bias,contrast,bandUse, colorTableId};
}


export function colorTableMatches(plot) {
    if (!plot || isThreeColor(plot)) return true;
    const entry = getEntry(plot.plotImageId);
    if (!entry?.rawTileDataGroup?.colorTableId) return true;
    if (entry.rawTileDataGroup.colorTableId!==plot.colorTableId) return false;
    // const {bias, contrast}= plot.rawData.bandData[0];
    //todo add check bias and contrast here
    return true;
}



export async function changeLocalMaskColor(plot, maskColor) {
    if (!getEntry(plot.plotImageId)) return;
    const newPlotState = plot.plotState.copy();
    const entry = getEntry(plot.plotImageId);
    entry.rawTileDataGroup = await populateTilesAsync(entry.rawTileDataGroup, 0,  true, maskColor);
    return { plotState: newPlotState};
}

export function hasLocalStretchByteDataInStore(plot) {
    const entry = getEntry(plot?.plotImageId);
    if (!entry) return false;
    return (entry.rawTileDataGroup && entry?.dataType!==CLEARED);
}

export function hasClearedDataInStore(plot) {
    return Boolean(plot && getEntry(plot.plotImageId)?.dataType===CLEARED);
}

function clearLocalStretchData(plot) {
    if (!plot) return;
    const {plotImageId, plotId}= plot;
    const entry= getEntry(plotImageId);
    dispatchRequestLocalData({plotId,plotImageId,dataRequested:false});
    if (!entry) return;
    entry.dataType= CLEARED;
}

// keep code around
// function isNoisyImage(plot) {
//     if (isNaN(plot?.webFitsData?.[Band.NO_BAND.value]?.largeBinPercent)) return false;
//     return (!isThreeColor(plot) && plot.webFitsData[Band.NO_BAND.value].largeBinPercent>.03);
// }

export function getDataCompress(plotImageId) {
    return getEntry(plotImageId)?.rawTileDataGroup?.dataCompress;
}

/**
 * @param {WebPlot} plot
 * @param {boolean} mask
 * @return {string} -  should be 'FULL' or 'HALF' or 'QUARTER'
 */
function getFirstDataCompress(plot, mask) {
    if (mask) return FULL;
    const {dataWidth, dataHeight, zoomFactor}= plot;
    const size= dataWidth*dataHeight;
    if (size < 6*MEG) return zoomFactor<.3 ? HALF : FULL;

    if (zoomFactor<QUARTER_ZOOM_FACT) return QUARTER;
    else if (zoomFactor<HALF_ZOOM_FACT) return HALF;
    else return size < MAX_FULL_DATA_SIZE ? FULL : HALF;
}


/**
 * @param {String} firstCompress - the result of getFirstDataCompress
 * @param {WebPlot} plot
 * @return {string} -  should be 'FULL' or 'HALF' or 'QUARTER'
 */
function getNextDataCompress(firstCompress, plot) {
    if (firstCompress===FULL) return FULL;
    const {zoomFactor, dataWidth, dataHeight}= plot;
    if (zoomFactor<QUARTER_ZOOM_FACT) {
        return firstCompress;
    }
    else if (zoomFactor<HALF_ZOOM_FACT) {
        return (firstCompress===QUARTER) ? HALF : firstCompress;
    }
    else {
        const size= dataWidth*dataHeight;
        return (size > MAX_FULL_DATA_SIZE) ? HALF : FULL;
    }
}

const delay = async (ms) => new Promise((resolve) => setTimeout(resolve, ms));

let reqIdCounter= 0;
const getStretchReqId= () => `stretch-req-${++reqIdCounter}`;


/**
 * load the stretch data from the server for a given stretch parameters. The loading will determine how much it compresses
 * this data it could be QUARTER, HALF, or FULL
 * @param pv
 * @param plot
 * @param dispatcher
 * @return {Promise<void>}
 */
export async function loadStretchData(pv, plot, dispatcher) {

    const workerKey= getEntry(plot.plotImageId)?.workerKey ?? getNextWorkerKey();
    const {plotImageId}= plot;
    const {plotImageId:plotImageIdForValidation}= primePlot(pv);

    const plotInvalid= () => primePlot(visRoot(),plotId)?.plotImageId!==plotImageIdForValidation;
    const reqId= getStretchReqId();
    imageIdsRequested.set(plotImageId,reqId);
    const {plotId}= pv;
    const imageOverlayId= plot.plotId!==pv.plotId ? plot.plotId : undefined; // i have an overlay image
    const mask= Boolean(imageOverlayId);
    const oPv= mask ? getOverlayById(pv,imageOverlayId) : undefined;
    const maskOptions= mask ? {maskColor:oPv?.colorAttributes.color, maskBits: oPv?.maskValue } : undefined;
    const dataCompress= getFirstDataCompress(plot,mask);
    const {success:firstSuccess, fatal}= await loadStandardStretchData(workerKey, plot,
                  {dataCompress, backgroundUpdate:false, checkForPlotUpdate:!mask}, maskOptions);
    imageIdsRequested.delete(plot.plotImageId);

    if (plotInvalid()) return;
    if (firstSuccess) {
        dispatcher({ type: ImagePlotCntlr.BYTE_DATA_REFRESH, payload:{plotId, imageOverlayId, plotImageId}});
    }
    else {
        if (fatal) {
            Logger('RawDataOps').warn(`dispatch the the plot failed on BYTE_DATA_REFRESH: ${dataCompress}`);
            if (dataCompress!==FULL) {
                await requestAgain(reqId, plotId, plot, 1, FULL, workerKey, dispatcher);
            }
            else {
                dispatcher({ type: ImagePlotCntlr.PLOT_IMAGE_FAIL,
                    payload:{plotId, description:'Failed: Could not retrieve image render data' }});
            }
        }
    }
}

/**
 * load the stretch data again if it is either QUARTER or HALF, if it is already FULL then return
 * @param {String} plotId
 * @param dispatcher
 * @param {boolean} secondTry - true if call recursively
 * @return {Promise<void>}
 */
export async function updateStretchDataAfterZoom(plotId,dispatcher, secondTry=false) {
    const plot= primePlot(visRoot(),plotId);
    if (!plot) return;
    const entry = getEntry(plot.plotImageId);
    if (!entry?.rawTileDataGroup) return;
    const {dataCompress}=  entry.rawTileDataGroup;
    const nextDataCompress= getNextDataCompress(dataCompress,plot);
    if (dataCompress===FULL || nextDataCompress===dataCompress) return; // if the compression target is already achieved then return

    const workerKey= getEntry(plot.plotImageId)?.workerKey ?? getNextWorkerKey();
    const reqId= getStretchReqId();
    const runningReqId= imageIdsRequested.get(plot.plotImageId);
    if (runningReqId && runningReqId!==reqId) return; // abort another request for this image is running
    imageIdsRequested.set(plot.plotImageId,reqId);
    const success= await requestAgain(reqId, plot.plotId, plot, 100, nextDataCompress, workerKey, dispatcher);
    imageIdsRequested.delete(plot.plotImageId);
    if (success) {
        if (secondTry) return;
        void await updateStretchDataAfterZoom(plotId,dispatcher); // try again the zoom may have changed
    }


               // only to this code if first try and request failed
    await delay(3000);
    if (getEntry(plot.plotImageId)?.rawTileDataGroup.dataCompress===nextDataCompress) return; // image already achieved target via another request
    const runningReqId2= imageIdsRequested.get(plot.plotImageId);
    if (runningReqId2 && runningReqId2!==reqId) return; // abort another request for this image is running
    void await updateStretchDataAfterZoom(plotId, dispatcher, true); // second try if mainly a fallback, code will rarely get here
}

/**
 *
 * @param {String} reqId - requestId
 * @param {String} plotId
 * @param {WebPlot|undefined} plot
 * @param {Number} waitTime
 * @param {String} dataCompress - should be 'FULL' or 'HALF' or 'QUARTER'
 * @param {String} workerKey
 * @param {function} dispatcher
 * @return {Promise<boolean>}
 */
async function requestAgain(reqId, plotId, plot, waitTime, dataCompress, workerKey, dispatcher) {
    if (!plot) return false;
    const {plotImageId}= plot;
    await delay(waitTime);
    if (imageIdsRequested.get(plotImageId)!==reqId) return false; // abort another request for this image has started
    const {success,fatal}= await loadStandardStretchData(workerKey, plot,
                       { dataCompress, backgroundUpdate: true, checkForPlotUpdate: true});
    if (success) {
        dispatcher({ type: ImagePlotCntlr.BYTE_DATA_REFRESH, payload:{plotId, imageOverlayId:undefined, plotImageId}});
    }
    else {
        if (fatal) {
            Logger('RawDataOps').warn(`dispatch the the plot failed on BYTE_DATA_REFRESH: ${dataCompress}`);
            dispatcher({ type: ImagePlotCntlr.PLOT_IMAGE_FAIL,
                payload:{plotId, description:'Failed: Could not retrieve image render data' }});
        }
    }
    return success;
}


/**
 *
 * @param {String} workerKey
 * @param {WebPlot} plot
 * @param {Object} loadingOptions
 * @param {String} loadingOptions.dataCompress- should be 'FULL' or 'HALF' or 'QUARTER'
 * @param {boolean} loadingOptions.backgroundUpdate
 * @param {boolean} loadingOptions.checkForPlotUpdate
 * @param {Object|undefined} [maskOptions]
 * @param {String} maskOptions.maskColor
 * @param {Number} maskOptions.maskBits
 * @return {Promise<{success:boolean, fatal: boolean}>}
 */
async function loadStandardStretchData(workerKey, plot, loadingOptions, maskOptions) {
    const {dataCompress=FULL, backgroundUpdate=false, checkForPlotUpdate=true}= loadingOptions;
    const {processHeader} = plot.rawData.bandData[0];
    const {plotImageId,colorTableId:originalColorTableId}= plot;
    const veryLargeData= plot.dataWidth*plot.dataHeight > MAX_FULL_DATA_SIZE;
    let entry = getEntry(plotImageId);
    if (entry) {
        if (!backgroundUpdate) entry.dataType= CLEARED;
        if (entry.loadingCnt) {
            postToWorker(makeAbortFetchAction(plotImageId, workerKey));
        }
    }
    else {
        if (backgroundUpdate) return {success:false, fatal:false};
        addRawDataToCache(plotImageId, processHeader, workerKey, Band.NO_BAND, CLEARED);
        entry = getEntry(plotImageId);
    }
    entry.loadingCnt++;
    try {
        const stretchResult = await postToWorker(
            makeRetrieveStretchByteDataAction(plot, plot.plotState, maskOptions, dataCompress, veryLargeData, workerKey));
        entry.loadingCnt--;
        if (!stretchResult.success) return {success:false, fatal: stretchResult.fatal};

        let latestPlot;
        let continueLoading;
        if (checkForPlotUpdate) {
            const latestPlotView= getPlotViewById(visRoot(),plot.plotId);
            latestPlot= findPlot(latestPlotView,plot.plotImageId);
            if (!latestPlot) return {success:false, fatal:false};
            const {plotState} = latestPlot;
            continueLoading = plotState.getBands().every((b) => plotState.getRangeValues(b)?.toJSON() === plot.plotState.getRangeValues(b)?.toJSON());
        }
        else {
            continueLoading= true;
            latestPlot= plot;
        }

        if (continueLoading) {
            entry.dataType = STRETCH_ONLY;
            const success=  maskOptions ?
                await completeMaskLoad(latestPlot, stretchResult, maskOptions.maskColor) :
                await completeLoad(latestPlot, stretchResult, originalColorTableId); //todo - get mask color
            return {success, fatal:false};
        } else {
            clearLocalStretchData(latestPlot);
            return {success:false, fatal: stretchResult.fatal};
        }
    } catch (failResult) {
        const {success,fatal}= failResult;
        entry.loadingCnt--;
        return {success, fatal};
    }
}


/**
 * @param {WebPlot} plot
 * @param {{rawTileDataGroup:RawTileDataGroup, plotStateSerialized:string}} stretchResult
 * @param {number} originalColorTableId
 * @return {Promise<boolean>}
 */
async function completeLoad(plot, stretchResult, originalColorTableId) {
    const currPlot= primePlot(visRoot(),plot.plotId);
    if (!currPlot) return false;
    const entry = getEntry(plot.plotImageId);
    if (originalColorTableId===currPlot.colorTableId) {
        entry.rawTileDataGroup = await populateTilesAsync(stretchResult.rawTileDataGroup, currPlot.colorTableId);
    }
    else {
        const {bias,contrast}= currPlot.rawData.bandData[0];
        await changeLocalRawDataColor(currPlot, currPlot.colorTableId, bias, contrast);
    }
    entry.rawTileDataGroup.colorTableId= currPlot.colorTableId;
    entry.thumbnailEncodedImage = makeThumbnailCanvas(currPlot);
    return true;
}


async function completeMaskLoad(plot, stretchResult, maskColor) {
    const {rawTileDataGroup} = stretchResult;
    const entry = getEntry(plot.plotImageId);
    entry.rawTileDataGroup = await populateTilesAsync(rawTileDataGroup, 0,  true, maskColor);
    return true;
}




//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
//--- This code is use if we bring the float data over
//--- right now we have disabled this feature
//-----------------------------------------------------------------------
//-----------------------------------------------------------------------

// export function loadRawData(plotOrAry, dispatcher) {
//     const plotAry= isArray(plotOrAry) ? plotOrAry : [plotOrAry];
//
//     plotAry.forEach(  (p) => {
//         const workerKey= getNextWorkerKey();
//         let promise;
//         const {plotId,plotImageId}= p;
//         if (isThreeColor(p)) {
//             promise= load3ColorRawData(p, workerKey);
//         } else {
//             promise= loadStandardRawData(p,workerKey);
//         }
//         promise.then( (rawData) =>
//             rawData && dispatcher({type: ImagePlotCntlr.UPDATE_RAW_IMAGE_DATA, payload:{plotId, plotImageId, rawData}}));
//     });
// }
//
// async function load3ColorRawData( plot, workerKey) {
//     const {plotState} = plot;
//     const promiseAry = allBandAry.map((b) => plotState.isBandUsed(b) ? postToWorker(makeLoadAction(plot, b, workerKey)) : Promise.resolve());
//     const loadResultAry= (await Promise.all(promiseAry)).map( (r) => r ? r : {success:false} );
//     loadResultAry.forEach(({success}, idx) => {
//         const band = Band.get(idx);
//         if (band && success) {
//             const {processHeader} = plot.rawData.bandData[idx];
//             if (!processHeader) return;
//             addRawDataToCache(plot.plotImageId, processHeader, workerKey, band);
//         }
//     });
//     let latestPlot= primePlot(visRoot(),plot.plotId);
//     if (!latestPlot) return;
//     const stretchResult= await postToWorker(makeStretchAction(latestPlot, Band.NO_BAND,workerKey));
//     latestPlot= primePlot(visRoot(),plot.plotId);
//     const latestPlotView= getPlotViewById(visRoot(),plot.plotId);
//     latestPlot= findPlot(latestPlotView,plot.plotImageId);
//     if (!latestPlot) return;
//     return completeLoad(latestPlot,stretchResult);
// }
//
// async function loadStandardRawData( plot, workerKey) {
//     const loadResult = await postToWorker(makeLoadAction(plot,Band.NO_BAND, workerKey));
//     if (!loadResult.success) throw ('Error loading the data');
//     let latestPlot= primePlot(visRoot(),plot.plotId);
//     if (!latestPlot) return;
//     const stretchResult = await postToWorker(makeStretchAction(latestPlot,Band.NO_BAND, workerKey));
//     const {processHeader} = plot.rawData.bandData[0];
//     latestPlot= primePlot(visRoot(),plot.plotId);
//     if (!latestPlot) return;
//     addRawDataToCache(plot.plotImageId, processHeader, workerKey, Band.NO_BAND);
//     completeLoad(plot,stretchResult);
// }

// export function hasLocalRawDataInStore(plot) {
//     const entry = getEntry(plot?.plotImageId);
//     if (!entry) return false;
//     if (entry.dataType!==FULL) return false;
//
//     if (isThreeColor(plot)) {
//         const {plotState} = plot;
//         return allBandAry.every((band) => {
//             if (!plotState.isBandUsed(band)) return true;
//             return Boolean(entry[band.key]);
//         });
//     } else {
//         return Boolean(entry[Band.NO_BAND.key]);
//     }
// }
// /**
//  * @param {WebPlot} plot
//  * @param {ImagePt} ipt
//  * @param {Band} band
//  * @return {Promise.<Number|NaN>}
//  */
// export async function getFluxDirect(plot, ipt, band = Band.NO_BAND) {
//     const entry = getEntry(plot.plotImageId);
//     if (!entry) return NaN;
//     const fluxResult= await postToWorker(makeFluxDirectAction(plot,ipt,band,entry.workerKey));
//     return fluxResult.value;
// }

// export async function stretchRawData(plot, rvAry) {
//     const entry = getEntry(plot.plotImageId);
//     const stretchResult= await postToWorker(makeStretchAction(plot,Band.NO_BAND, entry.workerKey,rvAry));
//     const {rawTileDataGroup, plotStateSerialized} = stretchResult;
//     entry.rawTileDataGroup = await populateTilesAsync(rawTileDataGroup, plot.plotState.getColorTableId());
//     entry.thumbnailEncodedImage = makeThumbnailCanvas(plot);
//     return {plotState:PlotState.parse(plotStateSerialized)};
//
// }
//
//
