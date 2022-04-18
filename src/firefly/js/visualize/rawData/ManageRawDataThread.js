import {allBandAry, Band} from '../Band.js';
import {ServerParams} from '../../data/ServerParams.js';
import {addRawDataToCache, getEntry, removeRawData} from './RawDataThreadCache.js';
import PlotState from '../PlotState.js';
import {RawDataThreadActions} from '../../threadWorker/WorkerThreadActions.js';
import {lowLevelDoFetch} from '../../util/WebUtil.js';
import {
    abortFetch, getRealDataDim, getTransferable,
    makeFetchOptions, populateRawImagePixelDataInWorker, TILE_SIZE } from './RawDataCommon.js';

const {FETCH_DATA, STRETCH, COLOR, GET_FLUX, REMOVE_RAW_DATA, FETCH_STRETCH_BYTE_DATA, ABORT_FETCH}= RawDataThreadActions;



export function doRawDataWork({type,payload}) {
    try {
        payload= deserialize(payload);

        switch (type) {
            case ABORT_FETCH: return abortFetch(payload);
            case FETCH_STRETCH_BYTE_DATA: return fetchByteDataArray(payload);
            case COLOR: return doColorChange(payload);
            case REMOVE_RAW_DATA: {
                abortFetch(payload);
                return Promise.resolve({data:{type:REMOVE_RAW_DATA, entryCnt:removeRawData(payload.plotImageId)}});
            }

            case FETCH_DATA:
            case STRETCH:
            case GET_FLUX:
                return Promise.resolve({success:false, error:`${type} is disabled`});
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
    return {data:{...result, type:COLOR, transferable: getTransferable(result)}};
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
        bias, contrast, cmdSrvUrl, rootUrl, mask= false, maskBits=0, maskColor='',
        veryLargeData= false, dataCompress='FULL', colorTableId} = payload;

    try {
        // const start= Date.now();
        const callResults= await callStretchedByteData(plotImageId, plotStateSerialized, plotState,
            dataWidth,dataHeight, mask, maskBits, cmdSrvUrl, dataCompress, veryLargeData);
        if (!callResults.success) {
            return {data:{success:false, type: FETCH_STRETCH_BYTE_DATA, fatal: true, message: callResults.message}};
        }

        const {allTileAry}= callResults;
        const rawTileDataGroup= createRawTileDataGroup(dataWidth,dataHeight, dataCompress);
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
            rawTileDataGroup.rawTileDataAry.forEach( (rt,idx) =>
                rt.pixelDataStandard= mask? convertToBits(allTileAry[idx]) : allTileAry[idx]);
        }
        let entry= getEntry(plotImageId);
        if (!entry) {
            addRawDataToCache(plotImageId,undefined,undefined,undefined,processHeader);
            entry= getEntry(plotImageId);
        }
        const {retRawTileDataGroup, localRawTileDataGroup}=
                await populateRawImagePixelDataInWorker(rawTileDataGroup, colorTableId, plotState.isThreeColor(),
                                                        mask, maskColor, bias, contrast, {}, rootUrl);
        entry.rawTileDataGroup= localRawTileDataGroup;


        // logging code - uncomment to log
        // const elapse= Date.now()-start;
        // const totalLen= allTileAry.reduce((total,tile) => total+tile.length, 0);
        // const mbPerSec= (totalLen/MEG) / (elapse/1000);
        // console.debug(`${plotImageId}: ${getSizeAsString(totalLen)}, ${elapse} ms, MB/Sec: ${mbPerSec}`);
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

function getCompressParam(dataCompress, veryLargeData) {
    switch (dataCompress) {
        case 'FULL': return 'FULL';
        case 'HALF': return dataCompress= veryLargeData ? 'HALF' : 'HALF_FULL';
        case 'QUARTER': return dataCompress= veryLargeData ? 'QUARTER_HALF' : 'QUARTER_HALF_FULL';
    }
    return 'FULL';
}

/**
 * @typedef StretchByteDataResults
 * @prop {boolean} success
 * @prop {string} message
 * @prop {Array.<Uint8ClampedArray>} allTileAry
 */

/**
 *
 * @param {String} plotImageId
 * @param plotStateSerialized
 * @param plotState
 * @param {number} dataWidth
 * @param {number} dataHeight
 * @param {boolean} mask
 * @param {number} maskBits
 * @param {String} cmdSrvUrl
 * @param {String} dataCompress - should be 'FULL' or 'HALF' or 'QUARTER'
 * @param {boolean} veryLargeData - if true and dataCompress is 'QUARTER' never request full size
 * @return {Promise<StretchByteDataResults>}
 */
export async function callStretchedByteData(plotImageId,plotStateSerialized,plotState, dataWidth,dataHeight,
                                            mask,maskBits,cmdSrvUrl, dataCompress= 'FULL', veryLargeData= false) {

    const options=  makeFetchOptions(plotImageId, {
        [ServerParams.COMMAND]: ServerParams.GET_BYTE_DATA,
        [ServerParams.TILE_SIZE] : TILE_SIZE,
        [ServerParams.STATE] : plotStateSerialized,
        [ServerParams.MASK_DATA] : mask,
        [ServerParams.MASK_BITS] : maskBits,
        [ServerParams.DATA_COMPRESS] : getCompressParam(dataCompress, veryLargeData)
    });

    if (dataCompress!=='FULL' && dataCompress!=='HALF' && dataCompress!=='QUARTER') throw(new Error('dataCompress must be FULL or HALF or QUARTER'));

    const response= await lowLevelDoFetch(cmdSrvUrl, options, false );
    if (!response.ok) {
        return {
            success:false,
            message: `Fatal: Error from Server for getStretchedByteData: code: ${response.status}, text: ${response.statusText}`,
            allTileAry:[]
        };
    }
    const byte1d= new Uint8ClampedArray(await response.arrayBuffer());
    if (!byte1d.length) {
        return {
            success:false,
            message: 'Fatal: No data returned from getStretchedByteData',
            allTileAry:[]
        };
    }

    const {tileSize,xPanels,yPanels, realDataWidth, realDataHeight} =  getRealDataDim(dataCompress,dataWidth,dataHeight);

    let pos= 0;
    let idx=0;
    const allTileAry= [];
    const colorCnt= plotState.isThreeColor() ? plotState.getBands().length : 1;
    for(let i= 0; i<xPanels; i++) {
        for (let j = 0; j < yPanels; j++) {
            for(let bandIdx=0; (bandIdx<colorCnt); bandIdx++) {
                const width = (i < xPanels - 1) ? tileSize : ((realDataWidth - 1) % tileSize + 1);
                const height = (j < yPanels - 1) ? tileSize : ((realDataHeight - 1) % tileSize + 1);
                const len= width*height;
                allTileAry[idx]= byte1d.slice(pos,pos+len);
                idx++;
                pos+=len;
            }
        }
    }
    return {success: true, message:'', allTileAry};
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
 * @return {Object}
 */
async function changeLocalRawDataColor(plotImageId, colorTableId, threeColor, bias, contrast, bandUse, plotState, rootUrl) {
    const entry = getEntry(plotImageId);
    const bandEntry=entry?.[Band.NO_BAND.key];
    if (!bandEntry) return;
    const newPlotState = plotState.copy();
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
 * @param {String} dataCompress - should be 'FULL' or 'HALF' or 'QUARTER'
 * @param rgbIntensity
 * @return {RawTileData}
 */
export function createRawTileDataGroup(dataWidth,dataHeight, dataCompress='FULL', rgbIntensity) {
    const {tileSize,xPanels,yPanels, realDataWidth, realDataHeight} =  getRealDataDim(dataCompress,dataWidth,dataHeight);
    const rawTileDataAry= [];
    // const yIndexes= [];

    for(let i= 0; i<xPanels; i++) {
        for(let j= 0; j<yPanels; j++) {
            const width= (i<xPanels-1) ? tileSize : ((realDataWidth-1) % tileSize + 1);
            const height= (j<yPanels-1) ? tileSize : ((realDataHeight-1) % tileSize + 1);
            rawTileDataAry.push(createImageTileData(tileSize*i,tileSize*j,width,height));
            // if (i===0) yIndexes.push(tileSize*j);
        }
    }
    return {rawTileDataAry, dataCompress, rgbIntensity};
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



