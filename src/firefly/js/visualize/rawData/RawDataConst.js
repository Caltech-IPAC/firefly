
export const TILE_SIZE=3000;




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
