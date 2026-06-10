import BrowserInfo from '../../util/BrowserInfo';
import {Band} from '../Band.js';
import {ServerParams} from '../../data/ServerParams.js';
import {getColorModelByGPUType} from './ColorTable';
import {getGpuJs, getGpuJsImmediate} from './GpuJsConfig';
import {isAbortedError, makeJobRunningContext} from './JobRunner';
import {addRawDataToCache, getEntry, getEntryCount, removeRawData} from './RawDataThreadCache.js';
import PlotState from '../PlotState.js';
import {RawDataThreadActions} from '../../threadWorker/WorkerThreadActions.js';
import {AJAX_REQUEST, lowLevelDoFetch, REQUEST_WITH} from '../../util/WebUtil.js';
import {
    getRealDataDim, getTransferable,
    populateRawImagePixelDataInWorker, shouldUseGpuInWorker,
    TILE_SIZE, FULL, HALF, HALF_FULL, QUARTER, QUARTER_HALF, QUARTER_HALF_FULL,
} from './RawDataCommon.js';
import {createTileWithGPU} from './RawImageTilesGPU';

const {FETCH_DATA, STRETCH, COLOR, MASK_COLOR, GET_FLUX, REMOVE_RAW_DATA, FETCH_STRETCH_BYTE_DATA, ABORT_FETCH, CLOSE_WHEN_IDLE}= RawDataThreadActions;
const jobRunner= makeJobRunningContext(3);

export async function doRawDataWork({type,payload,sendStatus}) {
    let scheduleClose= false;
    const webGpu= await BrowserInfo.supportsWebGpu();
    if (shouldUseGpuInWorker() && !webGpu && !getGpuJsImmediate()  && payload.rootUrl) {
        await getGpuJs(payload.rootUrl); // make sure the GPU code is loaded up front
    }
    try {
        payload= deserialize(payload);

        switch (type) {
            case ABORT_FETCH: return abortFetch(payload);
            case FETCH_STRETCH_BYTE_DATA: return fetchByteDataArray(payload,sendStatus);
            case COLOR: return doColorChange(payload);
            case MASK_COLOR: return doMaskColorChange(payload);
            case REMOVE_RAW_DATA: {
                void jobRunner.abortJobs(payload.plotImageId);
                await deleteByteData(payload.cmdSrvUrl,payload.plotImageId, payload.plotStateSerialized, FULL);
                return {data:{type:REMOVE_RAW_DATA, entryCnt:removeRawData(payload.plotImageId)}};
            }
            case CLOSE_WHEN_IDLE: {
                if (!scheduleClose) {
                    scheduleClose=true;
                    doScheduleClose();
                }
                return {data:{success:true, type: RawDataThreadActions.CLOSE_WHEN_IDLE}};
            }

            case FETCH_DATA:
            case STRETCH:
            case GET_FLUX:
                return {success:false, error:`${type} is disabled`};
        }
    }
    catch (error) {
        return {success:false, error};
    }
}

function deserialize(payload) {
    const newPayload= {...payload};
    if (payload.band) newPayload.band= Band.get(payload.band.key);
    if (payload.plotStateSerialized) newPayload.plotState= PlotState.parse(payload.plotStateSerialized);
    return newPayload;
}



function doScheduleClose() {
    let idleCnt= 0;
    const id= setInterval( () => {
        if (!getEntryCount()) idleCnt++;
        else idleCnt=0;
        if (idleCnt===2) {
           clearInterval(id);
           self.close();
        }

    }, 1000);
}

async function doColorChange(payload) {
    const {plotImageId,plotState,colorTableId, threeColor, bias, contrast, rootUrl, useRed=true,useGreen=true,useBlue=true,nanPixelColor} = payload;
    const bandUse= {useRed,useGreen,useBlue};
    const result= await changeLocalRawDataColor(plotImageId,colorTableId,threeColor, bias, contrast, bandUse, plotState,rootUrl,nanPixelColor);
    return {data:{...result, type:COLOR, transferable: getTransferable(result)}};
}

async function doMaskColorChange(payload) {
    const {plotImageId,plotState,maskColor, rootUrl,} = payload;
    const entry = getEntry(plotImageId);
    const newPlotState = plotState.copy();

    const {retRawTileDataGroup, localRawTileDataGroup}=
        await populateRawImagePixelDataInWorker({rawTileDataGroup:entry.rawTileDataGroup, mask:true, maskColor,rootUrl});
    entry.rawTileDataGroup= localRawTileDataGroup;

    const result=  {rawTileDataGroup:retRawTileDataGroup, plotStateSerialized: newPlotState.toJson(true)};
    return {data:{...result, type:MASK_COLOR, transferable: getTransferable(result)}};
}

function convertToBits(ary) {
    const retAry= new Uint8ClampedArray(Math.trunc(ary.length/8)+1);
    const len= ary.length;
    for(let i=0;(i<len);i++) {
        if (ary[i]) {
            retAry[Math.trunc(i / 8)] = retAry[Math.trunc(i / 8)] | (1 << (i % 8));
        }
    }
    return retAry;
}

/**
 * @param {StretchWorkerActionPayload} payload
 * @param {Function} sendStatus
 */
async function fetchByteDataArray(payload,sendStatus) {
    const {plotImageId,plotStateSerialized, processHeader, dataWidth, dataHeight,
        dataCompress=FULL, colorTableId, nanPixelColor} = payload;

    try {
        const callResults= await callStretchedByteData(payload,sendStatus);
        if (!callResults.success) {
            return {data:{success:false, aborted: Boolean(callResults.aborted),
                    type: FETCH_STRETCH_BYTE_DATA, fatal: true, message: callResults.message,
                    status: callResults.status,
            }};
        }

        const {tileResultsAry}= callResults;
        const rawTileDataGroup= createRawTileDataGroupStandardPopulated(dataWidth,dataHeight, dataCompress,
                                                        colorTableId, nanPixelColor, tileResultsAry);
        let entry= getEntry(plotImageId);
        if (!entry) {
            addRawDataToCache(plotImageId,undefined,undefined,undefined,processHeader);
            entry= getEntry(plotImageId);
        }
        const retRawTileDataGroup = {...rawTileDataGroup};
        if (!shouldUseGpuInWorker()) {
            retRawTileDataGroup.rawTileDataAry= retRawTileDataGroup.rawTileDataAry.map((rt) =>
                        ({ ...rt, pixelData3C: undefined, pixelDataStandard: undefined, }));
        }

        entry.rawTileDataGroup= rawTileDataGroup;
        entry.rawTileDataGroup.rawTileDataAry = rawTileDataGroup.rawTileDataAry.map((rt) =>
                        ({ ...rt, workerBitMapTile:undefined, }));

        const result= {rawTileDataGroup:retRawTileDataGroup, plotStateSerialized, type: FETCH_STRETCH_BYTE_DATA};
        const transferable= getTransferable(result);
        return {data:result, transferable};
    }
    catch (e) {
        console.log('fetchByteDataArray', e);
        const result= {data:{success:false, fatal: false, type: FETCH_STRETCH_BYTE_DATA, messsage: 'call aborted: ' + e.toString()}};
        return result;
    }
}

export async function abortFetch({plotImageId}) {
    jobRunner.abortJobs(plotImageId);
    return {data:{success:true, type: RawDataThreadActions.ABORT_FETCH}};
}

function getCompressParam(dataCompress, veryLargeData=false) {
    switch (dataCompress) {
        case FULL: return FULL;
        case HALF: return dataCompress===veryLargeData ? HALF : HALF_FULL;
        case QUARTER: return dataCompress===veryLargeData ? QUARTER_HALF : QUARTER_HALF_FULL;
    }
    return FULL;
}

/**
 * @typedef StretchByteDataResults
 * @prop {boolean} success
 * @prop {string} message
 * @prop {Array.<Uint8ClampedArray>} allTileAry
 */

/**
 * @param {StretchWorkerActionPayload} payload
 * @param {Function} sendStatus
 * @return {Promise<StretchByteDataResults>}
 */
export async function callStretchedByteData(payload,sendStatus ) {

    const {plotImageId,plotStateSerialized,plotState, dataWidth,dataHeight,
        nanPixelColor,colorTableId, mask=false,maskBits,cmdSrvUrl:url, dataCompress= 'FULL'}= payload;

    const colorModel= !mask && !plotState.isThreeColor() && await getColorModelByGPUType(colorTableId,nanPixelColor);
    const ct= getCompressParam(dataCompress, payload.veryLargeData);
    const {options}=  makeFetchOptions(plotImageId,
        {
            ...getBaseByteDataParams(plotStateSerialized,'create',ct),
            [ServerParams.MASK_DATA] : mask,
            [ServerParams.MASK_BITS] : maskBits,
            [ServerParams.TILE_SIZE] : TILE_SIZE,
        },
        'create');

    if (dataCompress!==FULL && dataCompress!==HALF && dataCompress!==QUARTER) throw(new Error('dataCompress must be FULL or HALF or QUARTER'));

    const response= await lowLevelDoFetch(url, options, false );
    if (!response.ok) {
        const message= `Fatal: Error from Server for getStretchedByteData: code: ${response.status}, text: ${response.statusText}`;
        console.log('callStretchedByteData: '+message);
        return { success:false, message, allTileAry:[] };
    }
    else {
        const results= await response.json();
        if (!results[0]?.data?.tileCount) {
            const message= 'callStretchedByteData: error no tiles created';
            console.log(message);
            return { success:false, message, allTileAry:[] };
        }
    }

    const {tileSize,xPanels,yPanels, realDataWidth, realDataHeight} =  getRealDataDim(dataCompress,dataWidth,dataHeight);

    let tileNumber=0;

    const promiseAry= [];
    let totalTiles=0;
    let processedTiles=0;

    const incUpdateCnt= async () => {
        processedTiles++;
        if ((processedTiles % 4)===0 && totalTiles) {
            sendStatus(`${processedTiles} of ${totalTiles}`);
        }
    };


    for(let i= 0; i<xPanels; i++) {
        for (let j = 0; j < yPanels; j++) {
            const width = (i < xPanels - 1) ? tileSize : ((realDataWidth - 1) % tileSize + 1);
            const height = (j < yPanels - 1) ? tileSize : ((realDataHeight - 1) % tileSize + 1);
            promiseAry.push(getATile({tileNumber, colorModel,width,height,payload,incUpdateCnt}));
            tileNumber++;
        }
    }

    totalTiles= promiseAry.length;

    const results= await Promise.allSettled(promiseAry);
    sendStatus('');
    const tileResultsAry= results.map( (r) => r.value);
    const success= !tileResultsAry.some( (r) => Boolean(r?.error || !r));
    const firstErrStat= !success ? tileResultsAry.find( (r) => r.error || !r)?.status : 200;
    const aborted= success ? false : tileResultsAry.some( (r) => r?.aborted);
    if (success || !aborted) deleteByteData(url,plotImageId, plotStateSerialized, ct); // don't clean up if aborted since it will probably be overridden anyway, avoids a race condition
    return success
        ? {success, message:'', tileResultsAry}
        : {success, message:'tile retrieve failed,', tileResultsAry:[], aborted, status: firstErrStat};
}

function deleteByteData(url, plotImageId, plotStateSerialized, ct) {
    const {options}= makeFetchOptions(plotImageId, getBaseByteDataParams(plotStateSerialized,'delete',ct),undefined);
    void lowLevelDoFetch(url, options, false );
}

/**
 * Retrieve and process the tile
 * @param obj
 * @param obj.tileNumber
 * @param obj.colorModel
 * @param obj.width
 * @param obj.height
 * @param obj.payload
 * @param obj.incUpdateCnt
 * @return {Promise<{pixelDataStandard: ArrayBuffer, workerBitMapTile: HTMLCanvasElement|OffscreenCanvas|ImageBitmap}>}
 */
async function getATile({tileNumber, colorModel, width, height, payload,incUpdateCnt}) {
    const {cmdSrvUrl:url, plotImageId, plotState, plotStateSerialized, mask, maskColor, bias=.5, contrast=1,
        dataCompress, veryLargeData}= payload;
    const isThreeColor = plotState.isThreeColor();
    const doBitmap= shouldUseGpuInWorker();
    const ct= getCompressParam(dataCompress, veryLargeData);
    const params= { ...getBaseByteDataParams(plotStateSerialized,'getTile',ct), [ServerParams.TILE_NUMBER]: tileNumber+''};

    const signalId= 'tile'+tileNumber;

    try {
        if (isThreeColor) {
            const bandUse= {useRed:plotState.isBandUsed(Band.RED),useGreen:plotState.isBandUsed(Band.RED),useBlue:plotState.isBandUsed(Band.RED)};
            const pixelData3C=[undefined,undefined,undefined];
            const bandAry= plotState.getBands();
            const threeCPromises= [];
            for(let i=0; (i<bandAry.length); i++){
                const bandStr= bandAry[i].toString();
                const {options,abortController}= makeFetchOptions(plotImageId, { ...params, [ServerParams.BAND]: bandStr},signalId+bandStr);
                threeCPromises.push(fetchTileDataInQueue(url,options,signalId+bandStr,plotImageId, abortController));
            }
            for(let i=0; (i<bandAry.length); i++){
                const response= await threeCPromises[i];
                if (!response || response.error) return response ?? {error:true};
                pixelData3C[bandAry[i].value]= response.array;
            }
            const inData= {width,height, pixelData3C, pixelDataStandard:undefined};
            const workerBitMapTile= doBitmap
                ? await createTileWithGPU(inData, colorModel, mask, maskColor, bias, contrast, bandUse)
                : undefined;
            incUpdateCnt?.();
            return {pixelDataStandard:undefined, pixelData3C, workerBitMapTile};
        }
        else {
            const {options,abortController}= makeFetchOptions(plotImageId, params,signalId);
            const response= await fetchTileDataInQueue(url,options,signalId, plotImageId, abortController);
            if (!response || response.error) return response ?? {error:true};
            const responseAry= response.array;
            const pixelDataStandard= mask ? convertToBits(responseAry) : responseAry;
            const inData= {width,height, pixelData3C:undefined, pixelDataStandard};
            const workerBitMapTile= doBitmap
                ? await createTileWithGPU(inData, colorModel, mask, maskColor, bias, contrast, undefined)
                : undefined;
            incUpdateCnt?.();
            return {pixelDataStandard, workerBitMapTile};
        }
    } catch (error) {
        console.error(error);
        return {error, aborted:isAbortedError(error)};
    }
}

async function fetchTileData(url, options) {
    const response= await lowLevelDoFetch(url, options, false);
    if (!response.ok) return {error: true, status:response.status, statusText:response.statusText};
    const responseBuffer= await response.arrayBuffer();
    if (!responseBuffer) return {error:true};
    return {ok:true, array: new Uint8ClampedArray(responseBuffer)};
}

async function fetchTileDataInQueue(url, options, signalId, plotImageId, abortController) {
    return jobRunner.createJobPromise(() => fetchTileData(url, options),plotImageId, abortController);
}


function getBaseByteDataParams(plotStateSerialized,tileAction,ct) {
    return {
        [ServerParams.COMMAND]: ServerParams.GET_BYTE_DATA,
        [ServerParams.STATE] : plotStateSerialized,
        [ServerParams.TILE_ACTION]: tileAction,
        [ServerParams.DATA_COMPRESS] : ct,
    };
}


/**
 * color change needs to do the following
 *   - clear the raw data tiles
 *   - set the color in the plot state
 * @param {string} plotImageId
 * @param {number} colorTableId
 * @param {boolean} threeColor
 * @param {number} bias
 * @param {number} contrast
 * @param {boolean} bandUse
 * @param {PlotState} plotState
 * @param {string} rootUrl
 * @param  nanPixelColor
 * @return {Object}
 */
async function changeLocalRawDataColor(plotImageId, colorTableId, threeColor, bias, contrast, bandUse, plotState, rootUrl,nanPixelColor) {
    const entry = getEntry(plotImageId);
    const bandEntry=entry?.[Band.NO_BAND.key];
    if (!bandEntry) return;
    const newPlotState = plotState.copy();
    const {retRawTileDataGroup, localRawTileDataGroup}=
        await populateRawImagePixelDataInWorker({rawTileDataGroup:entry.rawTileDataGroup, colorTableId, threeColor,
            bias, contrast, bandUse, rootUrl,nanPixelColor});
    entry.rawTileDataGroup= localRawTileDataGroup;
    return {rawTileDataGroup:retRawTileDataGroup, plotStateSerialized: newPlotState.toJson(true)};
}




export function createRawTileDataGroupStandardPopulated(dataWidth,dataHeight, dataCompress,
                                                colorTableId, nanPixelColor, tileResultsAry) {
    const rawTileDataGroup= createRawTileDataGroup(dataWidth,dataHeight, dataCompress, undefined);
    rawTileDataGroup.colorTableid= colorTableId;
    rawTileDataGroup.nanPixelColor = nanPixelColor;
    rawTileDataGroup.rawTileDataAry.forEach( (entry,i) =>{
        entry.pixelDataStandard= tileResultsAry[i].pixelDataStandard;
        entry.pixelData3C= tileResultsAry[i].pixelData3C;
        entry.workerBitMapTile= tileResultsAry[i].workerBitMapTile;
    });
    return rawTileDataGroup;
}



/**
 *
 * @param {number} dataWidth
 * @param {number} dataHeight
 * @param {String} dataCompress - should be 'FULL' or 'HALF' or 'QUARTER'
 * @param rgbIntensity
 * @return {RawTileData}
 */
export function createRawTileDataGroup(dataWidth,dataHeight, dataCompress=FULL, rgbIntensity) {
    const {tileSize,xPanels,yPanels, realDataWidth, realDataHeight} =  getRealDataDim(dataCompress,dataWidth,dataHeight);
    const rawTileDataAry= [];

    for(let i= 0; i<xPanels; i++) {
        for(let j= 0; j<yPanels; j++) {
            const width= (i<xPanels-1) ? tileSize : ((realDataWidth-1) % tileSize + 1);
            const height= (j<yPanels-1) ? tileSize : ((realDataHeight-1) % tileSize + 1);
            rawTileDataAry.push(createImageTileData(tileSize*i,tileSize*j,width,height));
        }
    }
    return {rawTileDataAry, dataCompress, rgbIntensity, nanPixelColor:[0,0,0]};
}


/**
 *
 * @param {number} x
 * @param {number} y
 * @param {number} width
 * @param {number} height
 * @return {RawTileData}
 */
export function createImageTileData(x,y,width,height) {

    return {
        x,y,width,height,
        lastPixel: x + width -1,
        lastLine: y +height -1,
        pixelDataStandard: undefined,
        pixelData3C: undefined,
        workerBitMapTile: undefined,
        imageMasks: undefined,
        rawImageTile: undefined,
    };
}

export function makeFetchOptions(plotImageId, params, signalId) {
    const options= {
        method: 'post',
        mode: 'cors',
        credentials: 'include',
        cache: 'default',
        params,
        headers: {
            [REQUEST_WITH]: AJAX_REQUEST,
        }
    };
    if (!signalId) return {options};
    const ac= globalThis.AbortController && new AbortController();
    if (ac) options.signal= ac.signal;
    return {options, abortController:ac};
}