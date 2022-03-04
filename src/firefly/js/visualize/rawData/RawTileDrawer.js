import {getEntry} from 'firefly/visualize/rawData/RawDataCache.js';
import {TILE_SIZE} from 'firefly/visualize/rawData/RawDataCommon.js';
import {contains, intersects} from 'firefly/visualize/VisUtil.js';
import {createCanvas} from 'firefly/util/WebUtil.js';

/**
 *
 * @param ctx
 * @param {number} zf
 * @param {Array.<RawTileData>}rawTileDataAry
 * @param {number} x
 * @param {number} y
 * @param {number} width
 * @param {number} height
 * @param {String} dataCompress - should be 'FULL' or 'HALF' or 'QUARTER'
 */
function writeToCanvas(ctx, zf, rawTileDataAry, x, y, width, height, dataCompress) {
    ctx.imageSmoothingEnabled = false;
    const realTileSize = dataCompress === 'FULL' ? TILE_SIZE : dataCompress === 'HALF' ? TILE_SIZE / 2 : TILE_SIZE / 4;
    const factor = dataCompress === 'FULL' ? zf : dataCompress === 'HALF' ? zf * 2 : zf * 4;
    const step = Math.trunc(realTileSize * factor);
    let id, drawX, drawY;
    const len = rawTileDataAry.length;
    for (let i = 0; (i < len); i++) { // use for loop for optimization
        if (needsTile(x / factor, y / factor, width / factor, height / factor, rawTileDataAry[i])) {
            id = rawTileDataAry[i];
            drawX = Math.trunc(((id.x / realTileSize) * step) - x);
            drawY = Math.trunc(((id.y / realTileSize) * step) - y);
            ctx.drawImage(id.rawImageTile, 0, 0, id.width, id.height, drawX, drawY, id.width * factor, id.height * factor);
        }
    }
}

function getLocalScreenTile(plot, tileDef, overrideZoomFactor) {
    const {x, y, width, height} = tileDef;
    const {zoomFactor} = plot;
    const entry = getEntry(plot.plotImageId);
    if (!entry?.rawTileDataGroup) return;
    const {rawTileDataAry, dataCompress} = entry.rawTileDataGroup;
    const screenCanvasTile = createCanvas(width, height);
    const ctx = screenCanvasTile.getContext('2d');
    writeToCanvas(ctx, overrideZoomFactor ?? zoomFactor, rawTileDataAry, x, y, width, height, dataCompress);
    return screenCanvasTile;
}

export function drawScreenTileToMainCanvas(plot, tileDef, mainCanvas, toX, toY, toW, toH, overrideZoomFactor) {
    const {x, y, width, height} = tileDef;
    const {zoomFactor} = plot;
    const entry = getEntry(plot.plotImageId);
    if (!entry?.rawTileDataGroup) return;
    const {rawTileDataAry, dataCompress} = entry.rawTileDataGroup;

    const ctx = mainCanvas.getContext('2d');
    ctx.save();
    ctx.translate(toX, toY);
    const path = new Path2D();
    path.rect(0, 0, toW, toH);
    ctx.clip(path);
    ctx.imageSmoothingEnabled = false;

    writeToCanvas(ctx, overrideZoomFactor ?? zoomFactor, rawTileDataAry, x, y, width, height, dataCompress);
    ctx.restore();
}

export function makeThumbnailCanvas(plot, tSize = 140) {
    const tZoomLevel = tSize / Math.max(plot.dataWidth, plot.dataHeight);
    return getLocalScreenTileAtZoom(plot, 0, 0, tSize, tSize, tZoomLevel);
}

export function getLocalScreenTileAtZoom(plot, x, y, width, height, zoomLevel) {
    return getLocalScreenTile(plot, {x, y, width, height}, zoomLevel);
}

/**
 *
 * @param x
 * @param y
 * @param width
 * @param height
 * @param {Object} obj
 * @param obj.x
 * @param obj.y
 * @param obj.width
 * @param obj.height
 * @return {boolean}
 */
function needsTile(x, y, width, height, {x: idX, y: idY, width: idWidth, height: idHeight}) {
    return (contains(x, y, width, height, idX, idY) ||
        contains(idX, idY, idWidth, idHeight, x, y) ||
        intersects(x, y, width, height, idX, idY, idWidth, idHeight));
}