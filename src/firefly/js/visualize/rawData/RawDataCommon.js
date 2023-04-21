import {isArrayBuffer} from 'lodash';
import BrowserInfo from '../../util/BrowserInfo.js';
import {getGpuJs} from './GpuJsConfig.js';
import {getGPUOps} from './RawImageTilesGPU.js';
import {createTransitionalTileWithCPU} from './RawImageTilesCPU.js';
import {
    AJAX_REQUEST, getGlobalObj,
    isGPUAvailableInWorker,
    isImageBitmap,
    isOffscreenCanvas, MEG,
    REQUEST_WITH
} from '../../util/WebUtil.js';
import {getColorModel} from './rawAlgorithm/ColorTable.js';
import {RawDataThreadActions} from 'firefly/threadWorker/WorkerThreadActions.js';

export const HALF= 'HALF';
export const QUARTER= 'QUARTER';
export const FULL= 'FULL';

const abortControllers= new Map(); // map of imagePlotId and AbortController
export const TILE_SIZE = 3000;
export const MAX_FULL_DATA_SIZE = 1500*MEG; //max size of byte data that can be loaded, file size will be 4x to 8x bigger
const USE_GPU = true;

export function shouldUseGpuInWorker() {
    if (BrowserInfo.isSafari()) return false;
    return isGPUAvailableInWorker();
}

/**
 *
 * @param rawTileDataAry
 * @param colorModel
 * @param {boolean} isThreeColor
 * @param mask
 * @param {String} maskColor
 * @param {number} bias
 * @param {number} contrast
 * @param {boolean} bandUse
 * @param {String} rootUrl
 * @return {Promise}
 */
async function populateRawTileDataArray(rawTileDataAry, colorModel, isThreeColor, mask, maskColor, bias, contrast, bandUse, rootUrl) {
    const GPU = USE_GPU ? await getGpuJs(rootUrl) : undefined;
    const createTransitionalTile = USE_GPU ? getGPUOps(GPU).createTransitionalTileWithGPU : createTransitionalTileWithCPU;
    const presult = rawTileDataAry.map((id) => createTransitionalTile(id, colorModel, isThreeColor, mask, maskColor, bias, contrast, bandUse));
    return await Promise.all(presult);
}

export async function populateRawImagePixelDataInWorker(rawTileDataGroup, colorTableId, isThreeColor, mask, maskColor, bias, contrast, bandUse, rootUrl) {
    if (shouldUseGpuInWorker() && !mask) {
        const colorModel = getColorModel(colorTableId);
        const rawTileDataAry = await populateRawTileDataArray(rawTileDataGroup.rawTileDataAry, colorModel, isThreeColor,  mask, maskColor, bias, contrast, bandUse, rootUrl);


        const localRawTileDataGroup = {...rawTileDataGroup, rawTileDataAry, colorTableId};
        const retRawTileDataGroup = {...localRawTileDataGroup};
        retRawTileDataGroup.rawTileDataAry = retRawTileDataGroup.rawTileDataAry.map((rt) =>
            ({
                ...rt,
                pixelData3C: undefined,
                pixelDataStandard: undefined,
            }));
        return {localRawTileDataGroup, retRawTileDataGroup};
    } else {
        const localRawTileDataGroup = {...rawTileDataGroup, colorTableId};
        localRawTileDataGroup.rawTileDataAry = localRawTileDataGroup.rawTileDataAry.map((rt) =>
            ({
                ...rt,
                pixelData3C: rt.pixelData3C && rt.pixelData3C.map((a) => a && a.buffer),
                pixelDataStandard: rt.pixelDataStandard && rt.pixelDataStandard.buffer
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
    const ac= getGlobalObj().AbortController && new AbortController();
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
 * @prop {ArrayBuffer|ImageBitmap|Canvas} workerTmpTile - the input to make a canvas tile with
 * @prop {Uint8Array|ArrayBuffer|undefined} pixelDataStandard
 * @prop {Array.<Uint8Array|ArrayBuffer>} pixelData3C
 * @prop {*|undefined} imageMask
 * @prop rawImageTile
 */

/**
 * @typedef RawTileDataGroup
 * @prop {String} dataCompress - should be 'FULL' or 'HALF' or 'QUARTER'
 * @prop {number} colorTableId
 * @prop {Array.<RawTileData>} rawTileData
 */
