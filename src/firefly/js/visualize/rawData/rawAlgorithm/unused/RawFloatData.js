import {ServerParams} from '../../../../data/ServerParams.js';
import {getSizeAsString, lowLevelDoFetch, MEG} from '../../../../util/WebUtil.js';
import {addRawDataToCache, getEntry, getEntryByBand} from '../../RawDataThreadCache.js';
import {makeHistogram} from './Histogram.js';
import {allBandAry, Band} from '../../../Band.js';
import {RangeValues, STRETCH_ASINH} from '../../../RangeValues.js';
import {RGBIntensity} from './RGBIntensity.js';
import {createRawTileDataGroup} from '../../ManageRawDataThread.js';
import {stretchPixels3Color, stretchPixels8Bit, stretchPixelsForMask} from './Stretch.js';
import {parseImagePt} from '../../../Point.js';
import {getTransferable, makeFetchOptions, populateRawImagePixelDataInWorker} from '../../RawDataCommon.js';

// todo - the color id is not longer in plot state it is in plot

export async function callFloatData(plotImageId,plotStateSerialized,band,cmdSrvUrl) {
    const options= makeFetchOptions(plotImageId, {
        [ServerParams.COMMAND]: ServerParams.GET_FLOAT_DATA,
        [ServerParams.STATE] : plotStateSerialized,
        [ServerParams.BAND] : band.key,
    });

    const response= await lowLevelDoFetch(cmdSrvUrl, options, false );
    if (!response.ok) {
        throw(new Error(`Error from Server for getFloatData: code: ${response.status}, text: ${response.statusText}`));
    }
    const arrayBuffer= await response.arrayBuffer();
    return new Float32Array(arrayBuffer);
}


export async function fetchRawDataArray(payload) {
    const {plotImageId,plotStateSerialized,band= Band.NO_BAND, processHeader, cmdSrvUrl} = payload;

    try {
        // console.time(`fetch ${plotImageId}`);
        const start= Date.now();
        const float1d= await callFloatData(plotImageId, plotStateSerialized, band, cmdSrvUrl);
        addRawDataToCache(plotImageId,float1d,undefined,undefined,processHeader,band);
        // console.timeEnd(`fetch ${plotImageId}`);
        const elapse= Date.now()-start;
        const mbPerSec= (float1d.length*4/MEG) / (elapse/1000);
        // console.log(`${plotImageId}: ${getSizeAsString(float1d.length*4)}, ${elapse} ms, MB/Sec: ${mbPerSec}`);
        return {data:true};
    }
    catch (e) {
        console.log('callFloatData failed');
        return {data:false};
    }
}



function setupHistogram(plotImageId, band) {
    const bandEntry = getEntryByBand(plotImageId, band);
    const {float1d, datamin, datamax} = bandEntry;
    if (!bandEntry) return;
    bandEntry.histogram = makeHistogram(float1d, datamin, datamax);
}

/**
 *
 * @param {Object} payload
 * @return {Promise<{Object}>}
 */
export async function doStretchData(payload) {
    const {plotImageId,plotState,dataWidth,dataHeight, rvStrAry, threeColor, rootUrl} = payload;
    const rvAry= rvStrAry.map( (rvStr) => RangeValues.parse(rvStr));
    const result= await changeLocalRawDataStretch(plotImageId,dataWidth,dataHeight,plotState,rvAry,threeColor, rootUrl);
    const transferable= getTransferable(result);
    return {data:result, transferable};
}

/**
 *
 * @param plotImageId
 * @param dataWidth
 * @param dataHeight
 * @param plotState
 * @param rvAry
 * @param threeColor
 * @param rootUrl
 * @return {Promise<{rawTileDataGroup: RawTileDataGroup, plotStateSerialized:string}>}
 */
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
                plotState.bandStateAry[b.value].rangeValues= (rvAry[idx]||fallbackRV);
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
        plotState.bandStateAry[bIdx].rangeValues = rv;
    }
    const {retRawTileDataGroup, localRawTileDataGroup}=
        await populateRawImagePixelDataInWorker(rawTileDataGroup, 1, threeColor, false, '', .5, 1, rootUrl);
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

/**
 * @param {Object} payload
 * @return {Promise.<Number|undefined>}
 */
export function getFluxDirect(payload) {
    const {plotImageId, iptSerialized, plotState, band = Band.NO_BAND} = payload;
    if (!plotState.isBandUsed(band)) return Promise.resolve({data: {value: undefined}});
    const ipt = parseImagePt(iptSerialized);
    const {float1d, processHeader} = getEntryByBand(plotImageId, band);
    if (!float1d) return Promise.resolve({data: {value: undefined}});
    const index = convertImagePtToRawDataIdx(ipt, processHeader.naxis1, processHeader.naxis2);

    const raw_dn = float1d[index];
    if ((raw_dn === processHeader.blank_value) || isNaN(raw_dn)) return Promise.resolve({data: {value: NaN}});

    let value;
    if (processHeader.origin.startsWith('Palomar Transient Factory')) {  // todo: this is a special case that I should fix
        value = -2.5 * .43429 * Math.log(raw_dn / processHeader.exptime) +
            processHeader.imagezpt +
            processHeader.extinct * processHeader.airmass;
        /* .43429 changes from natural log to common log */
    } else {
        value = raw_dn * processHeader.bscale + processHeader.bzero;
    }
    return Promise.resolve({data: {value}});
}

function convertImagePtToRawDataIdx(ipt, naxis1, naxis2) {
    const xInt = Math.round(ipt.x - 0.5);
    const yInt = Math.round(ipt.y - 0.5);
    if ((xInt < 0) || (xInt >= naxis1) || (yInt < 0) || (yInt >= naxis2)) return undefined;
    return ((naxis2 - 1) - yInt) * naxis1 + xInt;
}

/**
 * UNTEST, I am not sure it should even be on the client
 * @param float1d
 * @param imageTileData
 * @param processHeader
 */
export function constructMaskPixelData(float1d, imageTileData, processHeader)  {
    const {x, y, width, height, imageMasks, lastPixel, lastLine}= imageTileData;
    const pixelData= new Uint8Array(width*height);
    const pixelhist = new Int32Array(256);
    stretchPixelsForMask( x, lastPixel, y, lastLine, processHeader.naxis1, 255, float1d,
        pixelData, pixelhist, imageMasks);
    throw new Error('todo: imageType===TYPE_8_BIT -- mask');
}

