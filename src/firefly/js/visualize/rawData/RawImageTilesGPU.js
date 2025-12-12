import BrowserInfo from '../../util/BrowserInfo';
import {isArrayBuffer, isArray, once} from 'lodash';
import {toRGB} from 'firefly/util/Color.js';
import {gpujs_createRawDataTile3CRGB, gpujs_createRawDataTileImageRGB} from './GpuJsJobs';
import {process3CTileViaWebGPU, processMaskTileViaWebGPU, processSingleBandTileViaWebGPU} from './WebGpuJobs';

const getTilingFuncs= once(() => {
    const webGpu= BrowserInfo.supportsWebGpu();
    return {
        threeCTile: webGpu ? process3CTileViaWebGPU: gpujs_createRawDataTile3CRGB,
        maskTile: webGpu ? processMaskTileViaWebGPU: createRawDataTileImageForMask,
        singleBandTile: webGpu ? processSingleBandTileViaWebGPU: gpujs_createRawDataTileImageRGB,
    };
});


/**
 *
 * create a tile with either web gpu or gpu.js. We plan to retire gpu.js when all browsers support web gpu
 *
 * @param {RawTileData} inData
 * @param colorModel
 * @param {boolean} mask
 * @param {string} maskColor
 * @param bias
 * @param contrast
 * @param bandUse
 * @return {HTMLCanvasElement|OffscreenCanvas|ImageBitmap}
 */
export async function createTileWithGPU(inData, colorModel, mask= false, maskColor='', bias=.5, contrast=1, bandUse) {
    const {width,height, pixelData3C, pixelDataStandard}= inData;
    const {threeCTile, maskTile, singleBandTile} = getTilingFuncs();

    if (isArray(pixelData3C)) { // three color
        const ary= pixelData3C.map( (a) => a && get8BitAry(a));
        return threeCTile(ary, width,height,bias,contrast,bandUse);
    }
    else if (mask) { // mask
        return maskTile(maskColor,get8BitAry(pixelDataStandard), width,height);
    }
    else { // single band, false color
        return singleBandTile(colorModel, get8BitAry(pixelDataStandard), width,height,bias,contrast);
    }
}


/**
 * Create a mask canvas tile
 * @param {String} maskColor
 * @param pixelData - each byte in pixelData represents 8 pixels, so the data is compressed into bits
 * @param {number} width
 * @param {number} height
 * @return {HTMLCanvasElement}
 */
async function createRawDataTileImageForMask(maskColor, pixelData, width, height) {
    const [red,green,blue]= toRGB(maskColor);
    const imData= new ImageData(width,height);
    const data= imData.data;
    const len= data.length;
    let pixBit;
    let pixDataIdx;
    for(let i= 0; i<len; i+=4) {
        pixDataIdx= Math.trunc(Math.trunc(i/4)/8);
        pixBit=  pixelData[pixDataIdx] &  (1 << (Math.trunc(i/4) % 8)) ;
        if (!pixBit) {
            data[i]= red;
            data[i+1]= green;
            data[i+2]= blue;
            data[i+3]= 255;
        }
        else {
            data[i]= data[i+1]= data[i+2]= data[i+3]= 0;
        }
    }
    return await globalThis.createImageBitmap(imData);
}

function get8BitAry(a)  {
    try {
        return isArrayBuffer(a) ? new Uint8ClampedArray(a) : a;
    }
    catch (e) {
        console.error(e);
        throw e;
    }
}
