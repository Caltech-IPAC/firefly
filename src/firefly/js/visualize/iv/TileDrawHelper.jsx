/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {isNil} from 'lodash';
import {encodeServerUrl} from '../../util/WebUtil.js';
import {makeHiPSTileUrl} from '../HiPSUtil.js';
import {primePlot} from '../PlotViewUtil.js';
import {getRootURL} from '../../util/BrowserUtil.js';
import {CysConverter} from '../CsysConverter.js';
import {makeTransform} from '../PlotTransformUtils.js';
import {toRadians, contains, intersects, getBoundingBox} from '../VisUtil.js';
import {makeDevicePt} from '../Point.js';
import {isImage} from '../WebPlot.js';

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
    if (isImage(plot)) {
        const params = {
            file: tile.url,
            state: plot.plotState.toJson(false),
            type: 'tile',
            x: tile.x,
            y: tile.y,
            width: tile.width,
            height: tile.height
        };
        return encodeServerUrl(getRootURL() + 'sticky/FireFly_ImageDownload', params);
    }
    else {
        return makeHiPSTileUrl(plot,tile.nside, tile.tileNumber);
    }
        
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

    const tileX= tile.x*scale;
    const tileY= tile.y*scale;
    const tileWidth= tile.width*scale;
    const tileHeight= tile.height*scale;

    return (x + w > tileX &&
            y + h > tileY &&
            x < tileX  + tileWidth &&
            y < tileY + tileHeight);
}

export function initOffScreenCanvas(dim) {
    const offscreenCanvas = document.createElement('canvas');
    offscreenCanvas.width = dim.width;
    offscreenCanvas.height = dim.height;

    const offscreenCtx = offscreenCanvas.getContext('2d');

    offscreenCtx.font= 'italic 15pt Arial';
    offscreenCtx.fillStyle= 'rgba(0,0,0,.4)';
    offscreenCtx.strokeStyle='rgba(0,0,0,.2)';
    offscreenCtx.textAlign= 'center';
    offscreenCtx.lineWidth= 1;


    return offscreenCanvas;
}


export function drawEmptyRecTile(x,y,w,h,ctx,plotView ) {
    if (w>150 && h>100) {
        ctx.save();
        const {flipY, rotation}= plotView;
        ctx.translate(x+w/2,y+h/2);
        ctx.rotate(toRadians(flipY ? rotation : -rotation));
        if (flipY)  ctx.scale(-1,1);
        ctx.fillText('Loading Tile...',0,0);
        ctx.restore();
        ctx.strokeRect(x, y, w-1, h-1);
    }

}


export function createEmptyTile(w,h) {
    const c = document.createElement('canvas');
    c.width = w;
    c.height = h;
    const ctx = c.getContext('2d');
    ctx.fillStyle = 'rgb(0,0,0)';
    ctx.fillRect(0, 0, w, h);
    return c;
}




// -- keeping this function around for a while
// export function drawEmptyQuadTile(devPtAry,ctx,plotView ) {
//     const xAry= devPtAry.map( (pt) => pt.x);
//     const yAry= devPtAry.map( (pt) => pt.y);
//
//
//     const minX= Math.min(...xAry);
//     const maxX= Math.max(...xAry);
//     const minY= Math.min(...yAry);
//     const maxY= Math.max(...yAry);
//
//
//     if (maxX-minX>150 && maxY-minY>100) {
//         const textX= xAry.reduce( (sum,x)=> sum+x,0)/ xAry.length;
//         const textY= yAry.reduce( (sum,y)=> sum+y,0)/ yAry.length;
//         ctx.save();
//         const {flipY, rotation}= plotView;
//         ctx.translate(textX,textY);
//         ctx.rotate(toRadians(flipY ? rotation : -rotation));
//         if (flipY)  ctx.scale(-1,1);
//         ctx.fillText('Loading Tile...',0,0);
//         ctx.restore();
//
//         ctx.save();
//         ctx.beginPath();
//         devPtAry.forEach( (pt, idx) => idx===0 ? ctx.moveTo(pt.x,pt.y) : ctx.lineTo(pt.x,pt.y) );
//         ctx.closePath();
//         ctx.stroke();
//         ctx.restore();
//     }
// }

export function isQuadTileOnScreen(corners, viewDim) {
    if (corners.some ((scrC) => !scrC)) return false;
    const {width,height}= viewDim;

    if (!corners.some ((scrC) => contains(0,0,width,height,scrC.x,scrC.y))) {

        const xAry= corners.map( (p) => p.x);
        const yAry= corners.map( (p) => p.y);
        const minX= Math.min(...xAry);
        const maxX= Math.max(...xAry);
        const minY= Math.min(...yAry);
        const maxY= Math.max(...yAry);


        const boundBoxAry= [
            {x: minX, y: minY},
            {x: maxX, y: minY},
            {x: maxX, y: maxY},
            {x: minX, y: maxY}
        ];
        if (!boundBoxAry.some ((mP) => contains(0,0,width,height,mP.x,mP.y))) {
            const bbWidth= maxX-minX;
            const bbHeight= maxY-minY;
            if (!contains(minX,minY, bbWidth, bbHeight, 0,0) &&
                !contains(minX,minY, bbWidth, bbHeight, width, 0) &&
                !contains(minX,minY, bbWidth, bbHeight, width, height) &&
                !contains(minX,minY, bbWidth, bbHeight, 0, height) ) {
                if (!intersects(minX,minY, bbWidth, bbHeight, 0,0,width,height)) {
                    return false;
                }
            }
        }

    }
    return true;

}


export function computeBounding(plot,w,h) {
    const ptAry= [];
    const cc= CysConverter.make(plot);
    ptAry.push(cc.getScreenCoords(makeDevicePt(0,0)));
    ptAry.push(cc.getScreenCoords(makeDevicePt(w,0)));
    ptAry.push(cc.getScreenCoords(makeDevicePt(w,h)));
    ptAry.push(cc.getScreenCoords(makeDevicePt(0,h)));
    return getBoundingBox(ptAry);
}


/**
 *
 * @param plotView
 * @param targetCanvas
 * @param color
 * @param offsetX
 * @param offsetY
 */
export function renderBoundBox(plotView, targetCanvas, color, offsetX, offsetY) {
    window.requestAnimationFrame(() => {
        const ctx= targetCanvas.getContext('2d');
        ctx.save();
        ctx.clearRect(0,0,targetCanvas.width, targetCanvas.height);
        ctx.fillStyle = color;

        const {scrollX, scrollY, flipX,flipY, viewDim, rotation}= plotView;
        if (flipY) offsetX*=-1;
        const plot= primePlot(plotView);
        const {width,height}= plot.screenSize;

        if (!isNil(plotView.scrollX) && !isNil(plotView.scrollY)) {
            const affTrans= makeTransform(offsetX,offsetY, scrollX, scrollY, rotation, flipX, flipY, viewDim);
            ctx.setTransform(affTrans.a, affTrans.b, affTrans.c, affTrans.d, affTrans.e, affTrans.f);
            ctx.fillRect(0,0, width, height);
        }
        ctx.restore();
    });
}



