/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {makeHiPSTileUrl} from '../HiPSUtil.js';
import {CysConverter} from '../CsysConverter.js';
import {toRadians, contains, intersects, getBoundingBox} from '../VisUtil.js';
import {makeDevicePt} from '../Point.js';
import {isHiPS} from '../WebPlot.js';
import {createCanvas, memorizeUsingMap} from '../../util/WebUtil';

export function createImageUrl(plot, tile) {
    if (isHiPS(plot)) return makeHiPSTileUrl(plot,tile.nside, tile.tileNumber);
    else console.log('fits image URL tiles are deprecated');
}

export function initOffScreenCanvas(dim) {
    const offscreenCanvas = createCanvas(dim.width,dim.height);

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
    const c= createCanvas(w,h);
    const ctx = c.getContext('2d');
    ctx.fillStyle = 'rgb(0,0,0)';
    ctx.fillRect(0, 0, w, h);
    return c;
}

export function isQuadTileOnScreen(corners, viewDim) {
    if (corners.some ((scrC) => !scrC)) return false;
    return isQuadTileOnScreenCachable(
        Math.round(corners[0].x),Math.round(corners[0].y),
        Math.round(corners[1].x),Math.round(corners[1].y),
        Math.round(corners[2].x),Math.round(corners[2].y),
        Math.round(corners[3].x),Math.round(corners[3].y),
        viewDim.width,viewDim.height);

}

const isQuadTileOnScreenCachable= memorizeUsingMap( (x1,y1,x2,y2,x3,y3,x4,y4, width,height) => {
    const corners= [{x:x1,y:y1}, {x:x2,y:y2},{x:x3,y:y3},{x:x4,y:y4}];
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
}, 10000);


export function computeBounding(plot,w,h) {
    const cc= CysConverter.make(plot);
    const ptAry= [
        cc.getScreenCoords(makeDevicePt(0,0)),
        cc.getScreenCoords(makeDevicePt(w,0)),
        cc.getScreenCoords(makeDevicePt(w,h)),
        cc.getScreenCoords(makeDevicePt(0,h)),
    ];
    return getBoundingBox(ptAry);
}
