import {allBandAry, Band} from '../Band.js';
import {ServerParams} from '../../data/ServerParams.js';
import {addRawDataToCache, getEntry, removeRawData} from './RawDataThreadCache.js';
import PlotState from '../PlotState.js';
import {RawDataThreadActions} from '../../threadWorker/WorkerThreadActions.js';
import { getSizeAsString, lowLevelDoFetch, MEG, } from '../../util/WebUtil.js';
import {
    abortFetch,
    getTransferable,
    makeFetchOptions,
    populateRawImagePixelDataInWorker,
    TILE_SIZE
} from './RawDataCommon.js';
// import {doStretchData, fetchRawDataArray, getFluxDirect} from './RawFloatData.js';

const {FETCH_DATA, STRETCH, COLOR, GET_FLUX, REMOVE_RAW_DATA, FETCH_STRETCH_BYTE_DATA, ABORT_FETCH}= RawDataThreadActions;



export function doRawDataWork({type,payload}) {
    try {
        payload= deserialize(payload);

        switch (type) {
            case ABORT_FETCH: return abortFetch(payload);
            // case FETCH_DATA: return fetchRawDataArray(payload);
            case FETCH_DATA: return console.log(`${type} is disabled`);
            case FETCH_STRETCH_BYTE_DATA: return fetchByteDataArray(payload);
            // case STRETCH: return doStretchData(payload);
            case STRETCH: return console.log(`${type} is disabled`);
            case COLOR: return doColorChange(payload);
            // case GET_FLUX: return getFluxDirect(payload);
            case GET_FLUX: return console.log(`${type} is disabled`);
            case REMOVE_RAW_DATA: {
                abortFetch(payload);
                return Promise.resolve({data:{entryCnt:removeRawData(payload.plotImageId)}});
            }
        }
    }
    catch (error) {
        return Promise.resolve({success:false, error});
    }
}

function deserialize(payload) {
    const newPayload= {...payload};
    if (payload.band) newPayload.band= Band.get(payload.band.key);
    if (payload.plotStateSerialized) newPayload.plotState= PlotState.parse(payload.plotStateSerialized);
    return newPayload;
}




async function doColorChange(payload) {
    const {plotImageId,plotState,colorTableId, threeColor, bias, contrast, rootUrl, useRed=true,useGreen=true,useBlue=true} = payload;
    const bandUse= {useRed,useGreen,useBlue};
    const result= await changeLocalRawDataColor(plotImageId,colorTableId,threeColor, bias, contrast, bandUse, plotState,rootUrl);
    const transferable= getTransferable(result);
    return {data:result, transferable};
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

async function fetchByteDataArray(payload) {
    const {plotImageId,plotStateSerialized, plotState, processHeader, dataWidth, dataHeight,
        bias, contrast, cmdSrvUrl, rootUrl, mask= false, maskBits=0, maskColor=''} = payload;

    try {
        const start= Date.now();
        const allTileAry= await callStretchedByteData(plotImageId, plotStateSerialized, plotState,
            dataWidth,dataHeight, mask, maskBits, cmdSrvUrl);
        const rawTileDataGroup= createRawTileDataGroup(dataWidth,dataHeight);
        if (plotState.isThreeColor()) {
            let rt;
            let tileIdx=0;
            for(let i=0; (i<rawTileDataGroup.rawTileDataAry.length); i++) {
                rt= rawTileDataGroup.rawTileDataAry[i];
                rt.pixelData3C= [];
                allBandAry.forEach( (b) => {
                    if (plotState.isBandUsed(b)) {
                        rt.pixelData3C[b.value]= allTileAry[tileIdx];
                        tileIdx++;
                    }
                    else {
                        rt.pixelData3C[b.value]= new Uint8ClampedArray(dataWidth*dataHeight);
                        rt.pixelData3C[b.value].fill(0);
                    }
                });
            }
        }
        else {
            if (mask) {
                rawTileDataGroup.rawTileDataAry.forEach( (rt,idx) => rt.pixelDataStandard= convertToBits(allTileAry[idx]));
            }
            else {
                rawTileDataGroup.rawTileDataAry.forEach( (rt,idx) => rt.pixelDataStandard= allTileAry[idx]);
            }
        }
        let entry= getEntry(plotImageId);
        if (!entry) {
            addRawDataToCache(plotImageId,undefined,undefined,undefined,processHeader);
            entry= getEntry(plotImageId);
        }
        const {retRawTileDataGroup, localRawTileDataGroup}=
                await populateRawImagePixelDataInWorker(rawTileDataGroup, plotState.colorTableId, plotState.isThreeColor(),
                                                        mask, maskColor, bias, contrast, {}, rootUrl);
        entry.rawTileDataGroup= localRawTileDataGroup;


        // logging code - uncomment to log
        const elapse= Date.now()-start;
        const totalLen= allTileAry.reduce((total,tile) => total+tile.length, 0);
        const mbPerSec= (totalLen/MEG) / (elapse/1000);
        // console.debug(`${plotImageId}: ${getSizeAsString(totalLen)}, ${elapse} ms, MB/Sec: ${mbPerSec}`);
        const result= {rawTileDataGroup:retRawTileDataGroup, plotStateSerialized};
        const transferable= getTransferable(result);
        return {data:result, transferable};
    }
    catch (e) {
        console.log(e);
        return {data:{success:false, messsage: 'call aborted'}};
    }
}


export async function callStretchedByteData(plotImageId,plotStateSerialized,plotState, dataWidth,dataHeight,mask,maskBits,cmdSrvUrl) {

    const options=  makeFetchOptions(plotImageId, {
        [ServerParams.COMMAND]: ServerParams.GET_BYTE_DATA,
        [ServerParams.STATE] : plotStateSerialized,
        [ServerParams.TILE_SIZE] : TILE_SIZE,
        [ServerParams.MASK_DATA] : mask,
        [ServerParams.MASK_BITS] : maskBits,

    });

    const response= await lowLevelDoFetch(cmdSrvUrl, options, false );
    if (!response.ok) {
        throw(new Error(`Error from Server for getStretchedByteData: code: ${response.status}, text: ${response.statusText}`));
    }
    const arrayBuffer= await response.arrayBuffer();
    const byte1d= new Uint8ClampedArray(arrayBuffer);
    const tileSize= TILE_SIZE;


    let xPanels= Math.trunc(dataWidth / TILE_SIZE);
    let yPanels= Math.trunc(dataHeight / TILE_SIZE);
    if (dataWidth % TILE_SIZE > 0) xPanels++;
    if (dataHeight % TILE_SIZE > 0) yPanels++;

    let pos= 0;
    let idx=0;
    const allTileAry= [];
    const colorCnt= plotState.isThreeColor() ? plotState.getBands().length : 1;
    for(let i= 0; i<xPanels; i++) {
        for (let j = 0; j < yPanels; j++) {
            for(let bandIdx=0; (bandIdx<colorCnt); bandIdx++) {
                const width = (i < xPanels - 1) ? tileSize : ((dataWidth - 1) % tileSize + 1);
                const height = (j < yPanels - 1) ? tileSize : ((dataHeight - 1) % tileSize + 1);
                const len= width*height;
                allTileAry[idx]= byte1d.slice(pos,pos+len);
                idx++;
                pos+=len;
            }
        }
    }
    return allTileAry;
}




/**
 * color change needs to do the following
 *   - clear the raw data tiles
 *   - set the color in the plot state
 * @param {string} plotImageId
 * @param {number} colorTableId
 * @param {number} bias
 * @param {number} contrast
 * @param {PlotState} plotState
 * @param {string} rootUrl
 * @return {Object}
 */
async function changeLocalRawDataColor(plotImageId, colorTableId, threeColor, bias, contrast, bandUse, plotState, rootUrl) {
    const entry = getEntry(plotImageId);
    const bandEntry=entry?.[Band.NO_BAND.key];
    if (!bandEntry) return;
    const newPlotState = plotState.copy();
    newPlotState.colorTableId = colorTableId;
    const {retRawTileDataGroup, localRawTileDataGroup}=
        await populateRawImagePixelDataInWorker(entry.rawTileDataGroup, colorTableId, threeColor, false, '',
            bias, contrast, bandUse, rootUrl);
    entry.rawTileDataGroup= localRawTileDataGroup;
    return {rawTileDataGroup:retRawTileDataGroup, plotStateSerialized: newPlotState.toJson(true)};
}







/**
 *
 * @param {number} dataWidth
 * @param {number} dataHeight
 * @param rgbIntensity
 * @param tileSize
 * @return {{rgbIntensity: RGBIntensity, rawTileDataAry: Array.<RawTileData>}}
 */
export function createRawTileDataGroup(dataWidth,dataHeight, rgbIntensity, tileSize= TILE_SIZE) {

    let xPanels= Math.trunc(dataWidth / tileSize);
    let yPanels= Math.trunc(dataHeight / tileSize);
    if (dataWidth % tileSize > 0) xPanels++;
    if (dataHeight % tileSize > 0) yPanels++;

    const rawTileDataAry= [];
    const yIndexes= [];

    for(let i= 0; i<xPanels; i++) {
        for(let j= 0; j<yPanels; j++) {
            const width= (i<xPanels-1) ? tileSize : ((dataWidth-1) % tileSize + 1);
            const height= (j<yPanels-1) ? tileSize : ((dataHeight-1) % tileSize + 1);
            rawTileDataAry.push(createImageTileData(tileSize*i,tileSize*j,width,height));
            if (i===0) yIndexes.push(tileSize*j);
        }
    }
    return {rawTileDataAry, rgbIntensity};
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
        workerTmpTile: undefined,
        imageMasks: undefined,
        rawImageTile: undefined,
    };
}



