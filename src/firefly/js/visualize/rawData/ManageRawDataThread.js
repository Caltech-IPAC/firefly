import {isArrayBuffer} from 'lodash';
import {allBandAry, Band} from '../Band.js';
import {ServerParams} from '../../data/ServerParams.js';
import {AJAX_REQUEST, doFetchUrl, REQUEST_WITH} from '../../util/lowerLevelFetch.js';
import {getEntry} from './RawDataThreadCache.js';
import {RangeValues, STRETCH_ASINH} from '../RangeValues.js';
import {makeHistogram} from './rawAlgorithm/Histogram.js';
import {RGBIntensity} from './rawAlgorithm/RGBIntensity.js';
import {addRawDataToCache, getEntryByBand, removeRawData} from './RawDataThreadCache.js';
import {stretchPixels3Color, stretchPixels8Bit, stretchPixelsForMask} from './rawAlgorithm/Stretch.js';
import {getColorModel} from './rawAlgorithm/ColorTable.js';
import PlotState from '../PlotState.js';
import {RawDataThreadActions} from '../../threadWorker/WorkerThreadActions.js';
import {parseImagePt} from '../Point.js';
import {getSizeAsString, isGPUAvailableInWorker, isImageBitmap, isOffscreenCanvas } from '../../util/WebUtil.js';
import {TILE_SIZE} from './RawDataConst.js';
import {getGPUOps} from './RawImageTilesGPU.js';
import {getGpuJs} from './GpuJsConfig.js';
import {createTransitionalTileWithCPU} from './RawImageTilesCPU.js';

const {FETCH_DATA, STRETCH, COLOR, GET_FLUX, REMOVE_RAW_DATA}= RawDataThreadActions;



export function doRawDataWork({type,payload}) {
    try {
        payload= deserialize(payload);

        switch (type) {
            case FETCH_DATA: return fetchRawDataArray(payload); // todo: needs work
            case STRETCH: return doStretchData(payload);
            case COLOR: return doColorChange(payload);
            case GET_FLUX: return getFluxDirect(payload);
            case REMOVE_RAW_DATA: return Promise.resolve({data:{entryCnt:removeRawData(payload.plotImageId)}});
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


/**
 * @param {Object} payload
 * @return {Promise.<Number|undefined>}
 */
export function getFluxDirect(payload) {
    const {plotImageId, iptSerialized, plotState, band = Band.NO_BAND}= payload;
    if (!plotState.isBandUsed(band)) return Promise.resolve({data:{value:undefined}});
    const ipt= parseImagePt(iptSerialized);
    const {float1d, processHeader} = getEntryByBand(plotImageId, band);
    if (!float1d) return Promise.resolve({data:{value:undefined}});
    const index = convertImagePtToRawDataIdx(ipt, processHeader.naxis1, processHeader.naxis2);

    const raw_dn = float1d[index];
    if ((raw_dn === processHeader.blank_value) || isNaN(raw_dn)) return Promise.resolve({data:{value:NaN}});

    let value;
    if (processHeader.origin.startsWith('Palomar Transient Factory')) {  // todo: this is a special case that I should fix
        value= -2.5 * .43429 * Math.log(raw_dn / processHeader.exptime) +
            processHeader.imagezpt +
            processHeader.extinct * processHeader.airmass;
        /* .43429 changes from natural log to common log */
    } else {
        value= raw_dn * processHeader.bscale + processHeader.bzero;
    }
    return Promise.resolve({data:{value}});
}

function convertImagePtToRawDataIdx(ipt, naxis1, naxis2) {
    const xInt = Math.round(ipt.x - 0.5);
    const yInt = Math.round(ipt.y - 0.5);
    if ((xInt < 0) || (xInt >= naxis1) || (yInt < 0) || (yInt >= naxis2)) return undefined;
    return ((naxis2 - 1) - yInt) * naxis1 + xInt;
}



function getTransferable(result) {
    if (!result?.rawTileDataGroup) return [];
    const {rawTileDataAry}= result?.rawTileDataGroup;
    let tran;
    tran = rawTileDataAry
        .map((e) => isArrayBuffer(e.pixelDataStandard) && e.pixelDataStandard)
        .filter((e) => e);
    if (!tran.length) {
        tran = rawTileDataAry
            .map((e) => e.pixelData3C?.filter( (a) => isArrayBuffer(a)))
            .filter((e) => e)
            .flat();
    }
    if (!tran.length) {
        tran = rawTileDataAry
            .map((e) => isArrayBuffer(e.workerTmpTile) ? e.workerTmpTile : undefined)
            .filter((e) => e);
    }
    if (!tran.length) {
        tran= rawTileDataAry
            .map( (e) => isOffscreenCanvas(e.workerTmpTile) || isImageBitmap(e.workerTmpTile) ? e.workerTmpTile : undefined)
            .filter( (e) => e);
    }
    return tran;

}

async function doColorChange(payload) {
    const {plotImageId,plotState,colorTableId, rootUrl} = payload;
    const result= await changeLocalRawDataColor(plotImageId,colorTableId,plotState,rootUrl);
    const transferable= getTransferable(result);
    return {data:result, transferable};
}

async function doStretchData(payload) {
    const {plotImageId,plotState,dataWidth,dataHeight, rvStrAry, threeColor, rootUrl} = payload;
    const rvAry= rvStrAry.map( (rvStr) => RangeValues.parse(rvStr));
    const result= await changeLocalRawDataStretch(plotImageId,dataWidth,dataHeight,plotState,rvAry,threeColor, rootUrl);
    const transferable= getTransferable(result);
    return {data:result, transferable};
}

async function fetchRawDataArray(payload) {
    const {plotImageId,plotStateSerialized,band= Band.NO_BAND, processHeader, cmdSrvUrl} = payload;

    try {
        console.time(`fetch ${plotImageId}`);
        const float1d= await callFloatData(plotImageId, plotStateSerialized, band, cmdSrvUrl);
        addRawDataToCache(plotImageId,float1d,undefined,undefined,processHeader,band);
        console.timeEnd(`fetch ${plotImageId}`);
        console.log(`${plotImageId}: ${getSizeAsString(float1d.length*4)}`);
        return {data:true};
    }
    catch (e) {
        console.log('callFloatData failed');
        return {data:false};
    }
}

export async function callFloatData(plotImageId,plotStateSerialized,band,cmdSrvUrl) {
    const optionsWithDef= {
        method: 'post',
        mode: 'cors',
        credentials: 'include',
        cache: 'default',
        params: {
            [ServerParams.COMMAND]: ServerParams.GET_FLOAT_DATA,
            [ServerParams.STATE] : plotStateSerialized,
                [ServerParams.BAND] : band.key,
        },
        headers: {
            // [WS_CHANNEL_HD]: channel,
            // [WS_CONNID_HD]: connId,
            [REQUEST_WITH]: AJAX_REQUEST,
        }
    };

    const response= await doFetchUrl(cmdSrvUrl, optionsWithDef, false );
    if (!response.ok) {
        throw(new Error(`Error from Server for getFloatData: code: ${response.status}, text: ${response.statusText}`));
    }
    const arrayBuffer= await response.arrayBuffer();
    // --- save for a while - copy method for endian conversion
    // const dv= new DataView(arrayBuffer);
    // const data= new Float32Array(arrayBuffer.byteLength/4);
    // for(let i=0;i<data.length; i++) data[i]= dv.getFloat32(i*4);
    // -- copy in place style
    // const dv= new DataView(arrayBuffer);
    // for(let i=0;i<data.length; i++) data[i]= dv.getFloat32(i*4);

    return new Float32Array(arrayBuffer);
}

function setupHistogram(plotImageId, band) {
    const bandEntry = getEntryByBand(plotImageId, band);
    const {float1d, datamin, datamax} = bandEntry;
    if (!bandEntry) return;
    bandEntry.histogram = makeHistogram(float1d, datamin, datamax);
}



/**
 * color change needs to do the following
 *   - clear the raw data tiles
 *   - set the color in the plot state
 * @param plotImageId
 * @param colorTableId
 * @param plotState
 * @param rootUrl
 * @return {Object}
 */
async function changeLocalRawDataColor(plotImageId, colorTableId, plotState, rootUrl) {
    const entry = getEntry(plotImageId);
    const bandEntry=entry?.[Band.NO_BAND.key];
    if (!bandEntry) return;
    const newPlotState = plotState.copy();
    newPlotState.colorTableId = colorTableId;
    const {retRawTileDataGroup, localRawTileDataGroup}=
        await populateRawImagePixelDataInWorker(entry.rawTileDataGroup, colorTableId, false, rootUrl);
    entry.rawTileDataGroup= localRawTileDataGroup;
    return {rawTileDataGroup:retRawTileDataGroup, plotStateSerialized: newPlotState.toJson(false)};
}




async function changeLocalRawDataStretch(plotImageId, dataWidth, dataHeight, plotState, rvAry, threeColor, rootUrl) {
    const entry = getEntry(plotImageId);
    if (!entry) return;
    let rawTileDataGroup = entry.rawTileDataGroup || createRawTileDataGroup(dataWidth, dataHeight);
    plotState = plotState.copy();

    if (threeColor) {
        const bAry= allBandAry.map( (b) => plotState.isBandUsed(b) ? b : undefined );
        const float1dAry= bAry.map( (b) => b ? entry[b.key].float1d : undefined);
        const processHeaderAry= bAry.map( (b) => b ? entry[b.key].processHeader : undefined);
        const histAry= bAry.map( (b) => {
            if (!b) return undefined;
            if (!entry[b.key].histogram) setupHistogram(plotImageId,b);
            return entry[b.key].histogram;
        });
        const isAsinH= rvAry.find( (rv) => rv)?.algorithm===STRETCH_ASINH;

        let rgbIntensity;
        if (isAsinH && float1dAry.every( (a) => a)) {
            rgbIntensity= new RGBIntensity();
            rgbIntensity.addRangeValues(float1dAry,processHeaderAry,histAry, rvAry);
        }

        rawTileDataGroup= await stretch3CAsync(rawTileDataGroup, float1dAry, processHeaderAry, histAry, rvAry,rgbIntensity);

        const fallbackRV=rvAry.find( (rv) => rv);
        allBandAry.forEach( (b,idx) => {
            plotState.bandStateAry[b.value].rangeValues = undefined;
            if (plotState.isBandUsed(b)) {
                plotState.bandStateAry[b.value].rangeValuesSerialize = (rvAry[idx]||fallbackRV).toJSON();
            }
        });

    } else {
        const bIdx = Band.NO_BAND.value;
        const bandEntry = entry[Band.NO_BAND.key];
        if (!bandEntry) return;
        if (!bandEntry.histogram) setupHistogram(plotImageId, Band.NO_BAND);
        const {float1d, processHeader, histogram} = bandEntry;


        const rv = rvAry[bIdx];
        rawTileDataGroup= await stretchStandardAsync(rawTileDataGroup, float1d, processHeader, histogram, rv);
        plotState.bandStateAry[bIdx].rangeValues = undefined;
        plotState.bandStateAry[bIdx].rangeValuesSerialize = rv.toJSON();
    }
    const {retRawTileDataGroup, localRawTileDataGroup}=
        await populateRawImagePixelDataInWorker(rawTileDataGroup, plotState.getColorTableId(), threeColor, rootUrl);
    entry.rawTileDataGroup= localRawTileDataGroup;


    return {rawTileDataGroup:retRawTileDataGroup, plotStateSerialized: plotState.toJson(false)};
}

function stretchStandardAsync(rawTileDataGroup, float1d,processHeader,histogram,rv) {
    const gen= stretchStandardGenerator(rawTileDataGroup, float1d,processHeader,histogram,rv);
    return new Promise((resolve, reject) => {
        const id= setInterval( () => {
            const result= gen.next();
            if (!result.done) return;
            clearInterval(id);
            result.value ? resolve(result.value) : reject();
        },0);
    });
}


function stretch3CAsync(rawTileDataGroup, float1dAry, processHeaderAry, histAry, rvAry,rgbIntensity) {
    const gen= stretch3CGenerator(rawTileDataGroup, float1dAry, processHeaderAry, histAry, rvAry,rgbIntensity);
    return new Promise((resolve, reject) => {
        const id= setInterval( () => {
            const result= gen.next();
            if (!result.done) return;
            clearInterval(id);
            result.value ? resolve(result.value) : reject();
        },0);
    });
}

function* stretchStandardGenerator(rawTileDataGroup, float1d,processHeader,histogram,rv) {
    const newRawTileDataAry= [];
    const {rawTileDataAry}= rawTileDataGroup;
    for(let i=0; (i<rawTileDataAry.length);i++) {
        const id= rawTileDataAry[i];
        newRawTileDataAry[i]= constructStandardPixelData(float1d, id, processHeader, histogram, rv);
        if (i<rawTileDataAry.length-1) yield;
    }
    return {...rawTileDataGroup, rawTileDataAry:newRawTileDataAry};
}

function* stretch3CGenerator(rawTileDataGroup, float1dAry, processHeaderAry, histAry, rvAry,rgbIntensity) {
    const newRawTileDataAry= [];
    const {rawTileDataAry}= rawTileDataGroup;
    for(let i=0; (i<rawTileDataAry.length);i++) {
        const id= rawTileDataAry[i];
        newRawTileDataAry[i]= construct3CPixelData(float1dAry, id, processHeaderAry, histAry, rvAry,rgbIntensity);
        if (i<rawTileDataAry.length-1) yield;
    }
    return {...rawTileDataGroup, rawTileDataAry:newRawTileDataAry};
}

function construct3CPixelData(float1dAry, imageTileData, processHeaderAry, histAry, rangeValuesAry, rgbIntensity)  {
    const {x, y, width, height, lastPixel, lastLine}= imageTileData;
    const pixelDataAry= float1dAry.map( (float1d) => float1d && new Uint8Array(width*height) );
    stretchPixels3Color(rangeValuesAry, float1dAry, pixelDataAry, processHeaderAry, histAry,
        rgbIntensity, x, lastPixel, y, lastLine );
    return {...imageTileData, pixelData3C:pixelDataAry};
}

/**
 *
 * @param {Float32Array} float1d
 * @param imageTileData
 * @param processHeader
 * @param histogram
 * @param {RangeValues} rangeValues
 * @return {RawTileData} return a new version of RawTileData what is populated with the stretch data array(s)
 */
function constructStandardPixelData(float1d, imageTileData, processHeader, histogram, rangeValues)  {
    const {x, y, width, height, lastPixel, lastLine}= imageTileData;
    const pixelData= new Uint8Array(width*height);
    stretchPixels8Bit(rangeValues, float1d, pixelData, processHeader, histogram, x, lastPixel, y, lastLine);
    return {...imageTileData, pixelDataStandard:pixelData};
}

export function constructMaskPixelData(float1d, imageTileData, processHeader)  {
    const {x, y, width, height, imageMasks, lastPixel, lastLine}= imageTileData;
    const pixelData= new Uint8Array(width*height);
    const pixelhist = new Int32Array(256);
    stretchPixelsForMask( x, lastPixel, y, lastLine, processHeader.naxis1, 255, float1d,
        pixelData, pixelhist, imageMasks);
    throw new Error('todo: imageType===TYPE_8_BIT -- mask');
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

const USE_GPU= true;



async function populateRawTileDataArray(rawTileDataAry,colorModel,isThreeColor, rootUrl) {
    const GPU= USE_GPU ? await getGpuJs(rootUrl) : undefined;
    const createTransitionalTile= USE_GPU ? getGPUOps(GPU).createTransitionalTileWithGPU : createTransitionalTileWithCPU;
    const presult= rawTileDataAry.map( (id) => createTransitionalTile(id,colorModel,isThreeColor));
    return await Promise.all(presult);
}

async function populateRawImagePixelDataInWorker(rawTileDataGroup, colorTableId, isThreeColor, rootUrl) {
    if (isGPUAvailableInWorker()) {
        const colorModel= getColorModel(colorTableId);
        const rawTileDataAry=  await populateRawTileDataArray(rawTileDataGroup.rawTileDataAry,colorModel,isThreeColor, rootUrl);


        const localRawTileDataGroup=  {...rawTileDataGroup, rawTileDataAry, colorTableId};
        const retRawTileDataGroup= {...localRawTileDataGroup};
        retRawTileDataGroup.rawTileDataAry=  retRawTileDataGroup.rawTileDataAry.map( (rt) =>
            ({...rt,
                pixelData3C:  undefined,
                pixelDataStandard: undefined,
            }));
        return {localRawTileDataGroup,retRawTileDataGroup};
    }
    else {
        const localRawTileDataGroup=  {...rawTileDataGroup, colorTableId};
        localRawTileDataGroup.rawTileDataAry=  localRawTileDataGroup.rawTileDataAry.map( (rt) =>
            ({...rt,
                pixelData3C: rt.pixelData3C && rt.pixelData3C.map( (a) => a && a.buffer),
                pixelDataStandard: rt.pixelDataStandard && rt.pixelDataStandard.buffer
            }));
        const retRawTileDataGroup= {...localRawTileDataGroup};
        return {localRawTileDataGroup,retRawTileDataGroup};

    }
}

