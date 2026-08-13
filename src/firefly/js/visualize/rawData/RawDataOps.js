import {isUndefined} from 'lodash';
import shallowequal from 'shallowequal';
import BrowserInfo from '../../util/BrowserInfo';
import {Band} from '../Band.js';
import {PlotAttribute} from '../PlotAttribute';
import {
    currentP, findPlot, getOverlayById, getPlotViewById, hasLocalStretchByteData, isThreeColor, primePlot
} from '../PlotViewUtil.js';
import {MEG} from '../../util/WebUtil.js';
import {BYTE_DATA_REFRESH, PLOT_IMAGE_FAIL} from '../VisConst';
import {dispatchAttributeChange, dispatchMarkOutOfMemory, dispatchRequestLocalData} from '../ImagePlotDispatch';
import {visRoot} from '../VisStoreRoots';
import {PlotState} from '../PlotState.js';
import {getNextWorkerKey, isWorkerOutOfMemory, postToWorker} from '../../threadWorker/WorkerAccess.js';
import {
    addLoadingPromise, addRawDataToCache, CLEARED, getEntry, markOutOfMemory, STRETCH_ONLY
} from './RawDataCache.js';
import {getColorModelByGPUType} from './ColorTable.js';
import {createTileWithGPU} from './RawImageTilesGPU.js';
import {getGpuJs, getGpuJsImmediate} from './GpuJsConfig.js';
import {
    makeAbortFetchAction, makeColorAction, makeMaskColorAction, makeRetrieveStretchByteDataAction,
} from './RawDataThreadActionCreators.js';
import {FULL, HALF, logGpuState, MAX_FULL_DATA_SIZE, QUARTER, shouldUseGpuInWorker} from './RawDataCommon.js';
import {makeThumbnailCanvas} from 'firefly/visualize/rawData/RawTileDrawer.js';
import {Logger} from 'firefly/util/Logger.js';

const nextColorChangeParams= new Map();
const colorChangeDonePromises= new Map();
const currentRunningZoomImageId= new Map();
const QUARTER_ZOOM_FACT= .15;
const HALF_ZOOM_FACT= .42;


/**
 *
 * @param {Object} obj
 * @param obj.rawTileDataGroup
 * @param [obj.colorTableId]
 * @param [obj.mask]
 * @param [obj.maskColor]
 * @param obj.bias
 * @param obj.contrast
 * @param obj.bandUse
 * @param obj.nanPixelColor
 * @return {Promise<unknown>}
 */
async function populateTilesAsync({rawTileDataGroup:groupFromWorker, colorTableId=0, mask=false, maskColor=undefined,
                               bias=.5, contrast=1, bandUse, nanPixelColor}) {
    const {rawTileDataAry:tileFromWorker}= groupFromWorker;
    let rawTileDataAry;
    const workerBitMapTile= undefined;

    if (tileFromWorker[0].workerBitMapTile) { // just move workerBitMapTile to rawImageTile
        rawTileDataAry= tileFromWorker.map( (obj) => ({...obj, rawImageTile:obj.workerBitMapTile, workerBitMapTile}));
    }
    else { // worker returned array data because no worker gpu support, todo: can I deprecate this else?
        rawTileDataAry= [];
        const webGpu= await BrowserInfo.supportsWebGpu();
        if (!webGpu && !getGpuJsImmediate() ) await getGpuJs(); // make sure the GPU code is loaded up front
        const cm= await getColorModelByGPUType(colorTableId,nanPixelColor);
        for(let i=0; (i<tileFromWorker.length); i++) {
            const bitMap= await createTileWithGPU(tileFromWorker[i],cm,mask, maskColor, bias, contrast,bandUse);
            rawTileDataAry[i]= { ...tileFromWorker[i], workerBitMapTile, rawImageTile:bitMap};
        }
    }
    return {...groupFromWorker, rawTileDataAry, colorTableId, nanPixelColor};
}



/**
 * @typedef {Object} ChangeColorResults
 * @prop {PlotState} plotState
 * @prop {number} bias
 * @prop {number} contrast
 * @prop {{useRed:boolean,useBlue:boolean,useGreen:boolean}} bandUse
 */

/**
 *
 * @param {Object} params
 * @param params.plot
 * @param params.colorTableId
 * @param params.bias
 * @param params.contrast
 * @param {Array.<number>} params.nanPixelColor
 * @param params.bandUse
 * @param params.onComplete function to call with rawData object when done, note this call will only happen if is is not overridden by another call
 * @return {Promise<ChangeColorResults>}
 */
export function queueChangeLocalRawDataColor(params) {
    const {onComplete, ...changeParams}= params;
    const {plotImageId,plotId}= params.plot;
    const entry = getEntry(plotImageId);
    if (isOutOfMemoryInWorker(plotImageId)) {
        console.log('queueChangeLocalRawDataColor: not calling worker: out of memory');
        handleOutOfMemory(params.plot);
        return;
    }
    if (!entry.initialized) return;
    const p= colorChangeDonePromises.get(plotImageId);
    if (!entry.colorChangingInProgress || !p) {
        return changeLocalRawDataColor(changeParams)
            .then( (colorChangeResults) => onComplete(plotId, false, colorChangeResults));
    }
    if (nextColorChangeParams.has(plotImageId)) {
        nextColorChangeParams.get(plotImageId).onComplete(plotId,true);
        return;
    }
    else {
       p.then( () => {
           if (nextColorChangeParams.has(plotImageId) && currentP(plotId).pv) {
               const {onComplete, ...nextChangeParams}= nextColorChangeParams.get(plotImageId);
               nextColorChangeParams.delete(plotImageId);
               changeLocalRawDataColor(nextChangeParams)
                   .then( (colorChangeResults) => onComplete(nextChangeParams.plot.plotId, false, colorChangeResults));
           }
       });
    }
    nextColorChangeParams.set(plotImageId,params);
    return p;
}

/**
 * color change needs to do the following
 * @param {Object} obj
 * @param {WebPlot} obj.plot
 * @param {number} obj.colorTableId
 * @param {number} obj.bias
 * @param {number} obj.contrast
 * @param {Array.<number>} obj.nanPixelColor
 * @param [obj.bandUse]
 * @return {ChangeColorResults}
 */
export async function changeLocalRawDataColor(obj) {
    const defBandUse= {useRed:true,useGreen:true,useBlue:true};
    const {plot={}, colorTableId, bias, contrast, nanPixelColor, bandUse=defBandUse}= obj;
    const {plotImageId} = plot;
    const entry = getEntry(plotImageId);
    if (!entry.initialized) return {};

    let plotStateSerialized;
    let rawTileDataGroup;
    entry.colorChangingInProgress= true;

    if (isOutOfMemoryInWorker(plotImageId)) {
        console.log('changeLocalRawDataColor: not calling worker: out of memory');
        return;
    }

    let donePromiseResolve;

    const donePromise= new Promise( (resolve) => {
        donePromiseResolve= resolve;
    });
    colorChangeDonePromises.set(plotImageId, donePromise);

    try {
        if (shouldUseGpuInWorker()) {
            const colorResult= await postToWorker(makeColorAction({...obj, workerKey:entry.workerKey}));
            plotStateSerialized = colorResult.plotStateSerialized;
            rawTileDataGroup= colorResult.rawTileDataGroup;
        }
        else {
            const newPlotState = plot.plotState.copy();
            plotStateSerialized= newPlotState.toJson(true);
            rawTileDataGroup= entry.rawTileDataGroup;
        }
        entry.rawTileDataGroup = await populateTilesAsync({rawTileDataGroup, ...obj});
        entry.thumbnailEncodedImage = makeThumbnailCanvas(plot);
        entry.colorChangingInProgress= false;
        colorChangeDonePromises.delete(plotImageId);
        donePromiseResolve?.();
        const plotState= PlotState.parse(plotStateSerialized);
        return {plotState, bias,contrast,bandUse, nanPixelColor, colorTableId};

    } catch (data) {
        colorChangeDonePromises.delete(plotImageId);
        donePromiseResolve?.();
        entry.colorChangingInProgress= false;
        console.log('color change exception');
        console.log(data);
        if (isWorkerOutOfMemory(data.error)) handleOutOfMemory(plot);
        return {plotState:plot.plotState, bias,contrast,bandUse, nanPixelColor, colorTableId};
    }
}

export function isOutOfMemoryInWorker(plotImageId) {
    const entry = getEntry(plotImageId);
    if (!entry.initialized || !entry.workerKey) return false;
    return entry.outOfMemory;
}

function handleOutOfMemory(plot) {
    const {plotId,plotImageId} = plot;
    markOutOfMemory(plotImageId);
    setTimeout(() => {
        dispatchMarkOutOfMemory({plotId});
        dispatchAttributeChange({
            plotId,
            changes: {[PlotAttribute.USER_WARNINGS]:
                    {
                        tooltip: 'Image out of memory',
                        msg: 'This image is out of memory, the functions are limited: no color or stretch changes'
                    }}
        });
    });
}


export function colorTableMatches(plot) {
    if (!plot || isThreeColor(plot)) return true;
    const entry = getEntry(plot.plotImageId);
    if (!entry.initialized) return true;
    if (isUndefined(entry?.rawTileDataGroup?.colorTableId)) return true;
    if (entry.rawTileDataGroup.colorTableId!==plot.colorTableId) return false;
    if (!shallowequal(entry.rawTileDataGroup.nanPixelColor, plot.rawData?.bandData?.[0]?.nanPixelColor)) return false;
    // const {bias, contrast}= plot.rawData.bandData[0];
    //todo add check bias and contrast here
    return true;
}

export async function changeLocalMaskColorOnOverlayPlotView(opv, maskColor) {

    const promiseAry= opv.plots
        .filter( (p) => hasLocalStretchByteData(p))
        .map( (p) => changeLocalMaskColor(p,maskColor) );
    return Promise.all(promiseAry);
}


async function changeLocalMaskColor(plot, maskColor) {
    const entry = getEntry(plot.plotImageId);
    if (!entry.initialized) return;
    const newPlotState = plot.plotState.copy();
    const {workerKey} = entry;
    const result= await postToWorker(makeMaskColorAction({plot,maskColor,workerKey}));
    const rawTileDataGroup= result.rawTileDataGroup;
    entry.rawTileDataGroup = await populateTilesAsync({rawTileDataGroup, make:true, maskColor});
    return { plotState: newPlotState};
}

export function hasLocalStretchByteDataInStore(plot) {
    const entry = getEntry(plot?.plotImageId);
    if (!entry.initialized) return false;
    return (entry.rawTileDataGroup && entry?.dataType!==CLEARED);
}

function clearLocalStretchData(plot) {
    if (!plot) return;
    const {plotImageId, plotId}= plot;
    const entry= getEntry(plotImageId);
    dispatchRequestLocalData({plotId,plotImageId,dataRequested:false});
    if (!entry.initialized) return;
    entry.dataType= CLEARED;
}


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
    if (size < MEG) return FULL;
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
export async function loadInitialStretchData(pv, plot, dispatcher) {

    logGpuState();

    const entry= getEntry(plot.plotImageId);
    if (isOutOfMemoryInWorker(plot.plotImageId)) {
        console.log('loadStandardStretchData: not calling worker: out of memory');
        handleOutOfMemory(plot);
        return {success:false, fatal:true};
    }
    const workerKey= entry.workerKey ?? getNextWorkerKey();
    const {plotImageId}= plot;
    const {plotImageId:plotImageIdForValidation}= primePlot(pv);

    const plotInvalid= () => primePlot(visRoot(),plotId)?.plotImageId!==plotImageIdForValidation;
    const {plotId}= pv;
    const imageOverlayId= plot.plotId!==pv.plotId ? plot.plotId : undefined; // i have an overlay image
    const mask= Boolean(imageOverlayId);
    const oPv= mask ? getOverlayById(pv,imageOverlayId) : undefined;
    const maskOptions= mask ? {maskColor:oPv?.colorAttributes.color, maskBits: oPv?.maskValue } : undefined;
    const dataCompress= getFirstDataCompress(plot,mask);
    const {success, fatal, silentAbort=false,status}= await loadStandardStretchData(workerKey, plot,
                  {dataCompress, backgroundUpdate:false, checkForPlotUpdate:!mask}, maskOptions);

    if (plotInvalid()) return;
    if (success) {
        dispatcher({ type: BYTE_DATA_REFRESH, payload:{plotId, imageOverlayId, plotImageId}});
    }
    else {
        if (fatal && !silentAbort) {
            Logger('RawDataOps').warn(`dispatch to the plot failed on BYTE_DATA_REFRESH: ${dataCompress}, status: ${status}`);
            if (status===404) {
                Logger('RawDataOps').warn('it appears that the getTile called failed, this might be the server calls are not sticky, or server is out of memory');
                dispatcher({ type: PLOT_IMAGE_FAIL,
                    payload:{plotId, description:'Failed: server configuration error' }});
            }
            else {
                dispatcher({ type: PLOT_IMAGE_FAIL,
                    payload:{plotId, description:'Failed: Could not retrieve image render data' }});
            }
        }

        else {
            Logger('RawDataOps').warn(`non fatal, dispatch the the plot failed on BYTE_DATA_REFRESH: ${dataCompress}`);
            if (!silentAbort) {
                dispatcher({ type: PLOT_IMAGE_FAIL,
                    payload:{plotId, description:'Failed: Could not retrieve image render data' }});
            }
        }
    }
}

/**
 * load the stretch data again if it is either QUARTER or HALF, or FULL, if it is already FULL then return
 * @param {String} plotId
 * @param dispatcher
 * @return {Promise<void>}
 */
export async function updateStretchDataAfterZoom(plotId,dispatcher) {
    const plot= primePlot(visRoot(),plotId);
    if (!plot) return;
    const {plotImageId}= plot;
    const entry = getEntry(plotImageId,false);
    if (!entry || isOutOfMemoryInWorker(plotImageId)) return false;
    if (entry.loadingPromise) {// let current running loadStandardStretchData finished
        await entry.loadingPromise;
        await delay(5); // this is to solve race condition with same promise resolution
    }
    if (!entry.rawTileDataGroup) {
        Logger('RawDataOps').warn('updateStretchDataAfterZoom: unexpected empty rawTileDataGroup');
        return;
    }
    const {dataCompress}=  entry.rawTileDataGroup;
    const nextDataCompress= getNextDataCompress(dataCompress,plot);
    if (dataCompress===FULL || nextDataCompress===dataCompress) return; // if the compression target is already achieved then return
    if (currentRunningZoomImageId.get(plotImageId)?.dataCompress===nextDataCompress) return; // if a call is already scheduled for this compression target then return
    const workerKey= getEntry(plotImageId)?.workerKey ?? getNextWorkerKey();
    const reqId= getStretchReqId();
    currentRunningZoomImageId.set(plotImageId,{reqId,dataCompress:nextDataCompress});
    await delay(100);  // allow for superseding request
    if (currentRunningZoomImageId.get(plotImageId).reqId!==reqId) return; //if superseding call came in then return
    const {success,fatal}= await loadStandardStretchData(workerKey, plot, { dataCompress:nextDataCompress, backgroundUpdate: true, checkForPlotUpdate: true});
    if (success) {
        dispatcher({ type: BYTE_DATA_REFRESH, payload:{plotId, imageOverlayId:undefined, plotImageId}});
    }
    else {
        handleAfterZoomFail(dispatcher,plotId,fatal,nextDataCompress);
    }
    currentRunningZoomImageId.delete(plot.plotImageId);
}

function handleAfterZoomFail(dispatcher, plotId, fatal,nextDataCompress) {
    let msg;
    if (fatal) {
        msg= `requestDataAfterZoom: dispatch the the plot failed on BYTE_DATA_REFRESH: ${nextDataCompress}`;
        dispatcher({ type: PLOT_IMAGE_FAIL,
            payload:{plotId, description:'Failed: Could not retrieve image render data (requestDataAfterZoom)' }});
    }
    else {
        msg= `should never happen: request again: non fatal failure BYTE_DATA_REFRESH: ${nextDataCompress}`;
    }
    Logger('RawDataOps').warn(msg);
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
    const {processHeader,nanPixelColor} = plot.rawData.bandData[0];
    const {plotImageId,colorTableId:originalColorTableId}= plot;
    const veryLargeData= plot.dataWidth*plot.dataHeight > MAX_FULL_DATA_SIZE;
    const entry = getEntry(plotImageId);
    if (isOutOfMemoryInWorker(plotImageId)) {
        console.log('loadStandardStretchData: not calling worker: out of memory');
        return {success:false, fatal:true};
    }
    if (entry.initialized) {
        if (!backgroundUpdate) entry.dataType= CLEARED;
        if (entry.loadingPromise) {
            await postToWorker(makeAbortFetchAction(plotImageId, workerKey));
        }
    }
    else {
        if (backgroundUpdate) return {success:false, fatal:false};
        addRawDataToCache(plotImageId, processHeader, workerKey, Band.NO_BAND, CLEARED);
    }
    try {
        const stretchPromise = postToWorker(
            makeRetrieveStretchByteDataAction(plot, plot.plotState, maskOptions, dataCompress, veryLargeData, workerKey));
        addLoadingPromise(plotImageId, stretchPromise);
        const stretchResult = await stretchPromise;
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
            entry.initialized = true;
            const success=  maskOptions ?
                await completeMaskLoad(latestPlot, stretchResult, maskOptions.maskColor) :
                await completeLoad(latestPlot, stretchResult, originalColorTableId, nanPixelColor); //todo - get mask color
            return {success, fatal:false};
        } else {
            clearLocalStretchData(latestPlot);
            return {success:false, fatal: stretchResult.fatal, silentAbort:true};
        }
    } catch (failResult) {
        if (isWorkerOutOfMemory(failResult.error)) {
            handleOutOfMemory(plot);
            return {success:false, fatal:true};
        }
        else {
            const {success,fatal, aborted=false}= failResult;
            return {success, fatal, silentAbort:aborted, status:failResult.status};
        }
    }
}


/**
 * @param {WebPlot} plot
 * @param {{rawTileDataGroup:RawTileDataGroup, plotStateSerialized:string}} stretchResult
 * @param {number} originalColorTableId
 * @param nanPixelColor
 * @return {Promise<boolean>}
 */
async function completeLoad(plot, stretchResult, originalColorTableId, nanPixelColor) {
    const currPlot= primePlot(visRoot(),plot.plotId);
    if (!currPlot) return false;
    const entry = getEntry(plot.plotImageId);
    if (originalColorTableId===currPlot.colorTableId) {
        entry.rawTileDataGroup = await populateTilesAsync({rawTileDataGroup:stretchResult.rawTileDataGroup, colorTableId:currPlot.colorTableId, nanPixelColor});
        entry.thumbnailEncodedImage = makeThumbnailCanvas(currPlot);
    }
    else {
        const {bias,contrast,nanPixelColor}= currPlot.rawData.bandData[0];
        await changeLocalRawDataColor({plot:currPlot, colorTableId:currPlot.colorTableId, bias, contrast, nanPixelColor});
    }
    entry.rawTileDataGroup.colorTableId= currPlot.colorTableId;
    entry.rawTileDataGroup.nanPixelColor= currPlot.rawData.bandData[0].nanPixelColor;
    return true;
}


async function completeMaskLoad(plot, stretchResult, maskColor) {
    const {rawTileDataGroup} = stretchResult;
    const entry = getEntry(plot.plotImageId);
    entry.rawTileDataGroup = await populateTilesAsync({rawTileDataGroup, mask:true, maskColor});
    return true;
}
