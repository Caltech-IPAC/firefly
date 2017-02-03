/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {encodeServerUrl} from '../../util/WebUtil.js';
import {getRootURL} from '../../util/BrowserUtil.js';
const BG_IMAGE= 'image-working-background-24x24.png';
const BACKGROUND_STYLE = `url(+ ${BG_IMAGE} ) top left repeat`;



/**
 *
 * @param {string} src  url of the tile
 * @param {object} pt  where to put the tile
 * @param {number} width
 * @param {number} height
 * @param {number} scale
 * @param {number} opacity
 * @return {object}
 */
export function makeImageFromTile(src, pt, width, height, scale,opacity=1) {
    const s= {
        position : 'absolute',
        left : pt.x,
        top : pt.y,
        width: width*scale,
        height: height*scale,
        background: BACKGROUND_STYLE,
        opacity
    };
    return (
        <img src={src} key={src} style={s}/>
    );

}



export function createImageUrl(plot, tile) {
    const params = {
        file: tile.url,
        state: plot.plotState.toJson(),
        type: 'tile',
        x: tile.xoff,
        y: tile.yoff,
        width: tile.width,
        height: tile.height
    };
    return encodeServerUrl(getRootURL() + 'sticky/FireFly_ImageDownload', params);
}

/**
 *
 * @param {object} tile - object returned from server the describes the file
 * @param {number} x
 * @param {number} y
 * @param {number} w
 * @param {number} h
 * @param {number} scale
 * @return {boolean}
 */
export function isTileVisible(tile, x, y, w, h, scale) {

    const tileX= tile.xoff*scale;
    const tileY= tile.yoff*scale;
    const tileWidth= tile.width*scale;
    const tileHeight= tile.height*scale;

    return (x + w > tileX &&
            y + h > tileY &&
            x < tileX  + tileWidth &&
            y < tileY + tileHeight);
}
