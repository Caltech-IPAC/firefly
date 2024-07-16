import {isArrayBuffer, once} from 'lodash';
import BrowserInfo from '../../util/BrowserInfo.js';
import {createTileWithGPU} from './RawImageTilesGPU.js';
import {AJAX_REQUEST, MEG, REQUEST_WITH} from '../../util/WebUtil.js';
import {getColorModel} from './ColorTable.js';
import {RawDataThreadActions} from 'firefly/threadWorker/WorkerThreadActions.js';

export const HALF= 'HALF';
export const QUARTER= 'QUARTER';
export const FULL= 'FULL';

const abortControllers= new Map(); // map of imagePlotId and AbortController
export const TILE_SIZE = 3000;
export const MAX_FULL_DATA_SIZE = 1200*MEG; //max size of byte data that can be loaded, file size will be 4x to 8x bigger

export function shouldUseGpuInWorker() {
    if (BrowserInfo.isSafari() && !BrowserInfo.isVersionAtLeast(17)) return false;
    return Boolean(globalThis.OffscreenCanvas);
}

export const isImageBitmap= (b) => globalThis.ImageBitmap && (b instanceof globalThis.ImageBitmap);
export const isOffscreenCanvas= (b) => globalThis.OffscreenCanvas && (b instanceof globalThis.OffscreenCanvas);



export const logGpuState= once(() => {
    const gpuType= BrowserInfo.supportsWebGpu() ? 'webgpu' : 'gpu.js';
    const outStr= shouldUseGpuInWorker()
        ? `Images: gpu in worker, gpu: ${gpuType}`
        : `Images: gpu in main thread: ${gpuType}`;
    console.log(outStr);
});

/**
 *
 * @param {Object} obj
 * @param obj.rawTileDataGroup
 * @param obj.colorModel
 * @param obj.mask
 * @param {String} obj.maskColor
 * @param {number} obj.bias
 * @param {number} obj.contrast
 * @param {boolean} obj.bandUse
 * @return {Promise}
 */
async function populateTileDataInWorker(obj) {
    const {rawTileDataGroup, colorModel, mask=false, maskColor, bias, contrast, bandUse={}}= obj;
    const pResult = rawTileDataGroup.rawTileDataAry.map( async (inData) => ({
        ...inData,
        workerBitMapTile: await createTileWithGPU(inData, colorModel, mask, maskColor, bias, contrast, bandUse)
    }));
    return Promise.all(pResult);
}

/**
 *
 * @param {Object} obj
 * @param obj.rawTileDataGroup
 * @param obj.colorTableId
 * @param obj.isThreeColor
 * @param obj.mask
 * @param obj.maskColor
 * @param obj.bias
 * @param obj.contrast
 * @param [obj.bandUse]
 * @param obj.rootUrl
 * @param obj.nanPixelColor
 * @return {Promise}
 */
export async function populateRawImagePixelDataInWorker(obj) {
    const {rawTileDataGroup, colorTableId, mask, nanPixelColor, isThreeColor=false}= obj;
    if (shouldUseGpuInWorker()) {
        const colorModel = !mask && !isThreeColor && getColorModel(colorTableId,nanPixelColor, !BrowserInfo.supportsWebGpu());
        const rawTileDataAry = await populateTileDataInWorker({...obj,colorModel});


        const localRawTileDataGroup = {...rawTileDataGroup, rawTileDataAry, colorTableId, nanPixelColor};
        const retRawTileDataGroup = {...localRawTileDataGroup};
        retRawTileDataGroup.rawTileDataAry = retRawTileDataGroup.rawTileDataAry.map((rt) =>
            ({ ...rt, pixelData3C: undefined, pixelDataStandard: undefined, }));
        return {localRawTileDataGroup, retRawTileDataGroup};
    } else {
        const localRawTileDataGroup = {...rawTileDataGroup, colorTableId, nanPixelColor};
        localRawTileDataGroup.rawTileDataAry = localRawTileDataGroup.rawTileDataAry.map((rt) =>
            ({
                ...rt,
                pixelData3C: rt.pixelData3C?.map((a) => a?.buffer),
                pixelDataStandard: rt.pixelDataStandard?.buffer
            }));
        const retRawTileDataGroup = {...localRawTileDataGroup};
        return {localRawTileDataGroup, retRawTileDataGroup};
    }
}

export function makeFetchOptions(plotImageId, params) {
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
    const ac= globalThis.AbortController && new AbortController();
    if (ac) {
        abortControllers.set(plotImageId,ac);
        options.signal= ac.signal;
    }
    return options;
}

export async function abortFetch({plotImageId}) {
    abortControllers.get(plotImageId)?.abort();
    return {data:{success:true, type: RawDataThreadActions.ABORT_FETCH}};
}

export function getTransferable(result) {
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
            .map((e) => isArrayBuffer(e.workerBitMapTile) ? e.workerBitMapTile : undefined)
            .filter((e) => e);
    }
    if (!tran.length) {
        tran= rawTileDataAry
            .map( (e) => isOffscreenCanvas(e.workerBitMapTile) || isImageBitmap(e.workerBitMapTile) ? e.workerBitMapTile : undefined)
            .filter( (e) => e);
    }
    return tran;

}

export function getRealDataDim( dataCompress, dataWidth, dataHeight) {

    const tileSize= dataCompress===FULL ? TILE_SIZE : dataCompress===HALF ? TILE_SIZE/2 : TILE_SIZE/4;

    let xPanels= Math.trunc(dataWidth / TILE_SIZE);
    let yPanels= Math.trunc(dataHeight / TILE_SIZE);
    if (dataWidth % TILE_SIZE > 0) xPanels++;
    if (dataHeight % TILE_SIZE > 0) yPanels++;

    let realDataWidth= dataWidth;
    let realDataHeight= dataHeight;
    if (dataCompress===QUARTER) {
        realDataWidth = dataWidth % 4 === 0 ? Math.trunc(dataWidth / 4) : Math.trunc(dataWidth / 4) + 1;
        realDataHeight = dataHeight % 4 === 0 ? Math.trunc(dataHeight / 4) : Math.trunc(dataHeight / 4) + 1;
    }
    else if (dataCompress===HALF) {
        realDataWidth= dataWidth % 2 === 0 ? Math.trunc(dataWidth /2) : Math.trunc(dataWidth /2) + 1;
        realDataHeight= dataHeight % 2 === 0 ? Math.trunc(dataHeight /2) : Math.trunc(dataHeight /2) + 1;
    }

    return {tileSize,xPanels,yPanels, realDataWidth, realDataHeight};

}

// export function getDataCompress(plotImageId) {
//     return getEntry(plotImageId)?.rawTileDataGroup?.dataCompress;
// }


/**
 * @typedef {Object} RawTileData
 *
 * @prop {number} x
 * @prop {number} y
 * @prop {number} width
 * @prop {number} height
 * @prop {Number} lastPixel
 * @prop {Number} lastLine
 * @prop {ImageBitmap} workerBitMapTile - the worker produces a ImageBitmap with new browsers
 * @prop {Uint8Array|ArrayBuffer|undefined} pixelDataStandard
 * @prop {Array.<Uint8Array|ArrayBuffer>} pixelData3C
 * @prop {*|undefined} imageMask
 * @prop rawImageTile
 */

/**
 * @typedef RawTileDataGroup
 * @prop {String} dataCompress - should be 'FULL' or 'HALF' or 'QUARTER'
 * @prop {number} colorTableId
 * @prop {Array.<Number>} nanPixelColor
 * @prop {Array.<RawTileData>} rawTileData
 */
