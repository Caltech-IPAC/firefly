import {getGpuJs} from './GpuJsConfig.js';
import {getGPUOps} from './RawImageTilesGPU.js';
import {createTransitionalTileWithCPU} from './RawImageTilesCPU.js';
import {
    AJAX_REQUEST, getGlobalObj,
    isGPUAvailableInWorker,
    isImageBitmap,
    isOffscreenCanvas, MEG, K,
    REQUEST_WITH
} from '../../util/WebUtil.js';
import {getColorModel} from './rawAlgorithm/ColorTable.js';
import {isArrayBuffer} from 'lodash';


const abortControllers= new Map(); // map of imagePlotId and AbortController
export const TILE_SIZE = 3000;
export const MAX_RAW_IMAGE_SIZE = 400*MEG; // 400 megs
export const MAX_DIRECT_IMAGE_SIZE= 250*K;
const USE_GPU = true;

async function populateRawTileDataArray(rawTileDataAry, colorModel, isThreeColor, bias, contrast, bandUse, rootUrl) {
    const GPU = USE_GPU ? await getGpuJs(rootUrl) : undefined;
    const createTransitionalTile = USE_GPU ? getGPUOps(GPU).createTransitionalTileWithGPU : createTransitionalTileWithCPU;
    const presult = rawTileDataAry.map((id) => createTransitionalTile(id, colorModel, isThreeColor, bias, contrast, bandUse));
    return await Promise.all(presult);
}

export async function populateRawImagePixelDataInWorker(rawTileDataGroup, colorTableId, isThreeColor, bias, contrast, bandUse, rootUrl) {
    if (isGPUAvailableInWorker()) {
        const colorModel = getColorModel(colorTableId);
        const rawTileDataAry = await populateRawTileDataArray(rawTileDataGroup.rawTileDataAry, colorModel, isThreeColor, bias, contrast, bandUse, rootUrl);


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
    return {data:true};
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


/**
 * @typedef RawTileData
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
 * @prop {Array.<RawTileData>} rawTileData
 */
