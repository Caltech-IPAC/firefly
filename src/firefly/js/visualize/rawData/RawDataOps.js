import {isArray, isArrayBuffer, uniqueId} from 'lodash';
import {allBandAry, Band} from '../Band.js';
import {contains, intersects} from '../VisUtil.js';
import {findPlot, getOverlayById, getPlotViewById, isThreeColor, primePlot} from '../PlotViewUtil.js';
import {createCanvas, isGPUAvailableInWorker, isImageBitmap} from '../../util/WebUtil.js';
import ImagePlotCntlr, {dispatchRequestLocalData, visRoot} from '../ImagePlotCntlr.js';
import {PlotState} from '../PlotState.js';
import {getNextWorkerKey, postToWorker} from '../../threadWorker/WorkerAccess.js';
import {addRawDataToCache, getEntry, FULL, CLEARED, STRETCH_ONLY} from './RawDataCache.js';
import {getColorModel} from './rawAlgorithm/ColorTable.js';
import {getGPUOps} from './RawImageTilesGPU.js';
import {getGpuJs} from './GpuJsConfig.js';
import {
    makeAbortFetchAction,
    makeColorAction, makeFluxDirectAction,
    makeLoadAction,
    makeRetrieveStretchByteDataAction,
    makeStretchAction
} from './RawDataThreadActionCreators.js';
import {TILE_SIZE} from './RawDataCommon.js';

const nextColorChangeParams= new Map();
const colorChangeDonePromises= new Map();


function createTileFromImageData(buffer, width,height) {
    const c= createCanvas(width,height);
    if (isImageBitmap(buffer)) {
        c.getContext('2d').drawImage(buffer,0,0);
    }
    else if (buffer instanceof HTMLCanvasElement) {
        return buffer;
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
        const {pixelData3C, pixelDataStandard,workerTmpTile, width,height, rawImageTile }= rawTileDataAry[i];
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

/**
 *
 * @return {Array.<ScreenTileDef>}
 */
function getLocalScreenTileDefList() {
    return [{local:true, key:`autoTile---${uniqueId('tileid-')}`}];
}



function writeToCanvas(ctx, zf, rawTileDataAry,x,y,width,height) {
    ctx.imageSmoothingEnabled = false;
    const step= Math.trunc(TILE_SIZE*zf);
    let id, drawX,drawY;
    const len= rawTileDataAry.length;
    for(let i=0; (i<len);i++) { // use for loop for optimization
        if (needsTile(x/zf,y/zf,width/zf,height/zf,rawTileDataAry[i])) {
            id= rawTileDataAry[i];
            drawX= Math.trunc( ((id.x/TILE_SIZE)*step) - x);
            drawY= Math.trunc( ((id.y/TILE_SIZE)*step) - y);
            ctx.drawImage(id.rawImageTile,0,0,id.width,id.height,drawX,drawY, id.width*zf, id.height*zf);
        }
    }
}

function getLocalScreenTile(plot, tileDef, overrideZoomFactor) {
    const {x,y,width,height}= tileDef;
    const {zoomFactor}= plot;
    const entry= getEntry(plot.plotImageId);
    if (!entry?.rawTileDataGroup) return;
    const {rawTileDataAry}= entry.rawTileDataGroup;
    const screenCanvasTile = createCanvas(width,height);
    const ctx= screenCanvasTile.getContext('2d');
    writeToCanvas(ctx, overrideZoomFactor ?? zoomFactor, rawTileDataAry, x,y, width, height);
    return screenCanvasTile;
}



export function drawScreenTileToMainCanvas(plot, tileDef, mainCanvas, toX,toY,toW,toH, overrideZoomFactor) {
    const {x,y,width,height}= tileDef;
    const {zoomFactor}= plot;
    const entry= getEntry(plot.plotImageId);
    if (!entry?.rawTileDataGroup) return;
    const {rawTileDataAry}= entry.rawTileDataGroup;

    const ctx= mainCanvas.getContext('2d');
    ctx.save();
    ctx.translate(toX,toY);
    const path= new Path2D();
    path.rect(0,0,toW,toH);
    ctx.clip(path);
    ctx.imageSmoothingEnabled = false;

    writeToCanvas(ctx, overrideZoomFactor ?? zoomFactor, rawTileDataAry, x,y, width, height);
    ctx.restore();
}


function makeThumbnailCanvas(plot, tSize=70) {
    const tZoomLevel= tSize /Math.max( plot.dataWidth, plot.dataHeight);
    return getLocalScreenTileAtZoom(plot,0,0,tSize,tSize,tZoomLevel);
}

export function getLocalScreenTileAtZoom(plot, x,y,width,height,zoomLevel) {
    return getLocalScreenTile(plot,{x, y, width, height},zoomLevel);
}




/**
 *
 * @param x
 * @param y
 * @param width
 * @param height
 * @param id
 * @return {boolean}
 */
function needsTile(x, y, width, height, {x:idX,y:idY,width:idWidth,height:idHeight}) {
    return  ( contains(x,y,width,height, idX, idY) ||
              contains(idX, idY, idWidth, idHeight, x,y) ||
              intersects(x,y,width,height, idX, idY, idWidth, idHeight));
}

const defBandUse= {useRed:true,useGreen:true,useBlue:true};

/**
 *
 * @param plot
 * @param colorTableId
 * @param bias
 * @param contrast
 * @param bandUse
 * @param onComplete function to call with rawData object when done, note this call will only happen if is is not overridden by another call
 * @return {Promise<void>}
 */
export function queueChangeLocalRawDataColor(plot, colorTableId, bias, contrast, bandUse=defBandUse, onComplete) {
    const {plotImageId}= plot;
    const entry = getEntry(plotImageId);
    if (!entry) return;
    const p= colorChangeDonePromises.get(plotImageId);
    if (!entry.colorChangingInProgress || !p) {
        changeLocalRawDataColor(plot,colorTableId,bias,contrast,bandUse).then( (rawData) => onComplete(false, rawData));
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
               changeLocalRawDataColor(plot,colorTableId,bias,contrast,bandUse).then( (rawData) => onComplete(false, rawData));
           }
       });
    }
    nextColorChangeParams.set(plotImageId,{plot, colorTableId,bias, contrast, onComplete});
}

/**
 * color change needs to do the following
 * @param plot
 * @param colorTableId
 * @param bias
 * @param contrast
 * @param bandUse
 * @return {Object}
 */
export async function changeLocalRawDataColor(plot, colorTableId, bias, contrast, bandUse=defBandUse) {
    const entry = getEntry(plot.plotImageId);
    if (!entry) return;

    let plotStateSerialized;
    let rawTileDataGroup;
    entry.colorChangingInProgress= true;

    let donePromiseResolve;

    const donePromise= new Promise( (resolve) => {
        donePromiseResolve= resolve;
    });
    colorChangeDonePromises.set(plot.plotImageId, donePromise);

    if (isGPUAvailableInWorker()) {
        const colorResult= await postToWorker(makeColorAction(plot,colorTableId,bias,contrast,bandUse, entry.workerKey));
        plotStateSerialized = colorResult.plotStateSerialized;
        rawTileDataGroup= colorResult.rawTileDataGroup;
    }
    else {
        const newPlotState = plot.plotState.copy();
        newPlotState.colorTableId = colorTableId;
        plotStateSerialized= newPlotState.toJson(false);
        rawTileDataGroup= entry.rawTileDataGroup;
    }
    entry.rawTileDataGroup = await populateTilesAsync(rawTileDataGroup, colorTableId, undefined, undefined, bias,contrast, bandUse);
    entry.thumbnailEncodedImage = makeThumbnailCanvas(plot);
    entry.colorChangingInProgress= false;
    const localScreenTileDefList = getLocalScreenTileDefList();
    colorChangeDonePromises.delete(plot.plotImageId);
    donePromiseResolve?.();
    return {plotState:PlotState.parse(plotStateSerialized), localScreenTileDefList,bias,contrast,bandUse};
}



/**
 * zoom change needs to
 *   - confirm the stretch data exist.
 *   - recompute the localScreenTileDefList
 *   - change zoom factor in plot state
 * @param plot
 * @param zoomFactor
 * @return {Object}
 */
export function changeLocalRawDataZoom(plot, zoomFactor) {
    if (!getEntry(plot.plotImageId)) return;
    const newPlotState = plot.plotState.copy();
    newPlotState.zoomLevel = zoomFactor;
    return {localScreenTileDefList: getLocalScreenTileDefList(), plotState: newPlotState};
}

export async function changeLocalMaskColor(plot, maskColor) {
    if (!getEntry(plot.plotImageId)) return;
    const newPlotState = plot.plotState.copy();
    const entry = getEntry(plot.plotImageId);
    entry.rawTileDataGroup = await populateTilesAsync(entry.rawTileDataGroup, 0,  true, maskColor);
    return {localScreenTileDefList: getLocalScreenTileDefList(), plotState: newPlotState};
}


/**
 * @param {WebPlot} plot
 * @param {ImagePt} ipt
 * @param {Band} band
 * @return {Promise.<Number|NaN>}
 */
export async function getFluxDirect(plot, ipt, band = Band.NO_BAND) {
    const entry = getEntry(plot.plotImageId);
    if (!entry) return NaN;
    const fluxResult= await postToWorker(makeFluxDirectAction(plot,ipt,band,entry.workerKey));
    return fluxResult.value;
}


export function hasLocalRawDataInStore(plot) {
    const entry = getEntry(plot?.plotImageId);
    if (!entry) return false;
    if (entry.dataType!==FULL) return false;

    if (isThreeColor(plot)) {
        const {plotState} = plot;
        return allBandAry.every((band) => {
            if (!plotState.isBandUsed(band)) return true;
            return Boolean(entry[band.key]);
        });
    } else {
        return Boolean(entry[Band.NO_BAND.key]);
    }
}

export function hasLocalStretchByteDataInStore(plot) {
    if (hasLocalRawDataInStore(plot)) return true;
    const entry = getEntry(plot?.plotImageId);
    if (!entry) return false;
    return (entry.rawTileDataGroup && entry.dataType && entry.dataType!==CLEARED);
}

export function hasClearedDataInStore(plot) {
    if (!plot) return false;
    return getEntry(plot.plotImageId)?.dataType===CLEARED;
}

export function clearLocalStretchData(plot) {
    if (!plot) return;
    if (hasLocalRawDataInStore(plot)) return;
    const {plotImageId, plotId}= plot;
    const entry= getEntry(plotImageId);
    dispatchRequestLocalData({plotId,plotImageId,dataRequested:false});
    if (!entry) return;
    entry.dataType= CLEARED;
}


export function loadStretchData(pv, plot, dispatcher) {
    const plotAry= [plot];

    plotAry.forEach(  (p) => {
        const workerKey= getEntry(p.plotImageId)?.workerKey ?? getNextWorkerKey();
        const {plotImageId}= p;
        const imageOverlayId= p.plotId!==pv.plotId ? p.plotId : undefined; // i have an overlay image
        const mask= Boolean(imageOverlayId);
        const oPv= mask ? getOverlayById(pv,imageOverlayId) : undefined;
        const maskColor= mask ? oPv?.colorAttributes.color : undefined;
        const maskBits= mask ? oPv?.maskValue : undefined;
        loadStandardStretchData(p, !mask, mask, maskColor, maskBits, workerKey)
            .then( (rawData) =>
                rawData && dispatcher({
                    type: ImagePlotCntlr.UPDATE_RAW_IMAGE_DATA,
                    payload:{plotId:pv.plotId, imageOverlayId, plotImageId, rawData
                    }})
        );
    });
}

export async function stretchRawData(plot, rvAry) {

    const entry = getEntry(plot.plotImageId);
    const stretchResult= await postToWorker(makeStretchAction(plot,Band.NO_BAND, entry.workerKey,rvAry));
    const {rawTileDataGroup, plotStateSerialized} = stretchResult;
    entry.rawTileDataGroup = await populateTilesAsync(rawTileDataGroup, plot.plotState.getColorTableId());
    entry.thumbnailEncodedImage = makeThumbnailCanvas(plot);
    const localScreenTileDefList = getLocalScreenTileDefList();
    return {plotState:PlotState.parse(plotStateSerialized), localScreenTileDefList};

}


async function loadStandardStretchData( plot, checkForPlotUpdate, mask, maskColor, maskBits, workerKey) {
    const {processHeader} = plot.rawData.bandData[0];
    const {plotImageId, plotId}= plot;
    let entry = getEntry(plotImageId);
    if (entry) {
        entry.dataType= CLEARED;
        if (entry.loadingCnt) {
            postToWorker(makeAbortFetchAction(plotImageId, workerKey));
        }
    }
    else {
        addRawDataToCache(plotImageId, processHeader, workerKey, Band.NO_BAND, CLEARED);
        entry = getEntry(plotImageId);
    }
    entry.loadingCnt++;
    try {
        const stretchResult = await postToWorker(
            makeRetrieveStretchByteDataAction(plot, plot.plotState, mask, maskBits, maskColor, workerKey));
        entry.loadingCnt--;
        if (!stretchResult.success) return;

        let latestPlot;
        let continueLoading;
        if (checkForPlotUpdate) {
            const latestPlotView= getPlotViewById(visRoot(),plot.plotId);
            latestPlot= findPlot(latestPlotView,plot.plotImageId);
            if (!latestPlot) return;
            const {plotState} = latestPlot;
            continueLoading = plotState.getBands().every((b) => plotState.getRangeValues(b)?.toJSON() === plot.plotState.getRangeValues(b)?.toJSON());
        }
        else {
            continueLoading= true;
            latestPlot= plot;
        }

        if (continueLoading) {
            entry.dataType = STRETCH_ONLY;
            return mask ?  completeMaskLoad(latestPlot, stretchResult, maskColor) : completeLoad(latestPlot, stretchResult); //todo - get mask color
        } else {
            clearLocalStretchData(latestPlot);
        }
    } catch (e) {
        entry.loadingCnt--;
    }
}


/**
 *
 * @param {WebPlot} plot
 * @param {{rawTileDataGroup:RawTileDataGroup, plotStateSerialized:string}} stretchResult
 * @return {Promise<void>}
 */
async function completeLoad(plot, stretchResult) {
    const {rawTileDataGroup, plotStateSerialized} = stretchResult;
    const plotState = PlotState.parse(plotStateSerialized);
    const entry = getEntry(plot.plotImageId);
    entry.rawTileDataGroup = await populateTilesAsync(rawTileDataGroup, plotState.getColorTableId());
    entry.thumbnailEncodedImage = makeThumbnailCanvas(plot);
    const localScreenTileDefList = getLocalScreenTileDefList();
    return {...plot.rawData, plotState, localScreenTileDefList};
}


async function completeMaskLoad(plot, stretchResult, maskColor) {
    const {rawTileDataGroup, plotStateSerialized} = stretchResult;
    const plotState = PlotState.parse(plotStateSerialized);
    const entry = getEntry(plot.plotImageId);
    entry.rawTileDataGroup = await populateTilesAsync(rawTileDataGroup, 0,  true, maskColor);
    const localScreenTileDefList = getLocalScreenTileDefList();
    return {...plot.rawData, plotState, localScreenTileDefList};
}




//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
//--- This code is use if we bring the float data over
//--- right now we have disabled this feature
//-----------------------------------------------------------------------
//-----------------------------------------------------------------------

export function loadRawData(plotOrAry, dispatcher) {
    const plotAry= isArray(plotOrAry) ? plotOrAry : [plotOrAry];

    plotAry.forEach(  (p) => {
        const workerKey= getNextWorkerKey();
        let promise;
        const {plotId,plotImageId}= p;
        if (isThreeColor(p)) {
            promise= load3ColorRawData(p, workerKey);
        } else {
            promise= loadStandardRawData(p,workerKey);
        }
        promise.then( (rawData) =>
            rawData && dispatcher({type: ImagePlotCntlr.UPDATE_RAW_IMAGE_DATA, payload:{plotId, plotImageId, rawData}}));
    });
}

async function load3ColorRawData( plot, workerKey) {
    const {plotState} = plot;
    const promiseAry = allBandAry.map((b) => plotState.isBandUsed(b) ? postToWorker(makeLoadAction(plot, b, workerKey)) : Promise.resolve());
    const loadResultAry= (await Promise.all(promiseAry)).map( (r) => r ? r : {success:false} );
    loadResultAry.forEach(({success}, idx) => {
        const band = Band.get(idx);
        if (band && success) {
            const {processHeader} = plot.rawData.bandData[idx];
            if (!processHeader) return;
            addRawDataToCache(plot.plotImageId, processHeader, workerKey, band);
        }
    });
    let latestPlot= primePlot(visRoot(),plot.plotId);
    if (!latestPlot) return;
    const stretchResult= await postToWorker(makeStretchAction(latestPlot, Band.NO_BAND,workerKey));
    latestPlot= primePlot(visRoot(),plot.plotId);
    const latestPlotView= getPlotViewById(visRoot(),plot.plotId);
    latestPlot= findPlot(latestPlotView,plot.plotImageId);
    if (!latestPlot) return;
    return completeLoad(latestPlot,stretchResult);
}

async function loadStandardRawData( plot, workerKey) {
    const loadResult = await postToWorker(makeLoadAction(plot,Band.NO_BAND, workerKey));
    if (!loadResult.success) throw ('Error loading the data');
    let latestPlot= primePlot(visRoot(),plot.plotId);
    if (!latestPlot) return;
    const stretchResult = await postToWorker(makeStretchAction(latestPlot,Band.NO_BAND, workerKey));
    const {processHeader} = plot.rawData.bandData[0];
    latestPlot= primePlot(visRoot(),plot.plotId);
    if (!latestPlot) return;
    addRawDataToCache(plot.plotImageId, processHeader, workerKey, Band.NO_BAND);
    completeLoad(plot,stretchResult);
}
