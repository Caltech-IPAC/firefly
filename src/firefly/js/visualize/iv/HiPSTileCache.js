/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isString} from 'lodash';
import {initOffScreenCanvas} from './TileDrawHelper.jsx';
import {createCanvas} from '../../util/WebUtil.js';


const MAX_TILE_IMAGES= 400;
const MAX_ALLSKY_IMAGES= 80;

let cachedImages= new Map();
let failedCachedImages= new Map();
let cachedAllSkyImages= new Map();

/**
 * @global
 * @public
 * @typedef {Object} HiPSAllSkyCacheInfo
 *
 * @prop {string} url
 * @prop {Image}  order3 single image with the all order 3 allsky
 * @prop {Array.<Canvas>}  order2Array array of order 2 image tiles, each index if the tile number
 * @prop {Array.<Canvas>}  order3Array array of order 3 image tiles, each index if the tile number
 */



const makeKey= (url,colorTableId=-1,bias=.5,contrast=1) =>
    `${url}-${colorTableId}-${Math.trunc(bias*100)}-${Math.trunc(contrast*100)}`;

/**
 *
 * @param url
 * @param [colorTableId]
 * @param [bias]
 * @param [contrast]
 * @return {HiPSAllSkyCacheInfo}
 */
export function findAllSkyCachedImage(url,colorTableId,bias,contrast) {
    const result=  cachedAllSkyImages.get(makeKey(url,colorTableId,bias,contrast));
    if (result) result.time= Date.now();
    return result;
}

/**
 *
 * @param {String} url
 * @param {Canvas|Image} image
 * @param {number} [colorTableId]
 * @param {number} [bias]
 * @param {number} [contrast]
 */
export function addAllSkyCachedImage(url, image, colorTableId,bias,contrast) {
    const order2Array= makeOrder2AllSkyImages(image);
    const order3Array= makeOrder3AllSkyImages(image);
    cachedAllSkyImages.set(makeKey(url,colorTableId,bias,contrast),
        {url, colorTableId,bias,contrast, order3:image, order2Array, order3Array, colorTable: 'todo', time: Date.now()});
    if (cachedAllSkyImages.size>MAX_ALLSKY_IMAGES+(MAX_ALLSKY_IMAGES*.1)) {
        cachedAllSkyImages= cleanupCache(cachedAllSkyImages,MAX_ALLSKY_IMAGES);
    }
}


export function findTileCachedImage(url, colorTableId,bias,contrast) {
    const result= cachedImages.get(makeKey(url,colorTableId,bias,contrast));
    if (result) result.time= Date.now();
    return result;
}


export function addTileCachedImage(url, image, colorTableId,bias,contrast) {
    let cacheImage= image;
    if (image instanceof HTMLImageElement) {
        cacheImage= createCanvas(image.width,image.height);
        cacheImage.getContext('2d').drawImage(image,0,0);
    }
    cachedImages.set( makeKey(url,colorTableId,bias,contrast),
        {url, image:cacheImage, colorTableId,bias,contrast, emptyTile:false, colorTable: 'todo', time: Date.now()});
    if (cachedImages.size>MAX_TILE_IMAGES+(MAX_TILE_IMAGES*.25)) {
        cachedImages= cleanupCache(cachedImages, MAX_TILE_IMAGES);
    }
}

export function addFailedImage(url) {
    const result=  cachedImages.get(url);
    if (!result) failedCachedImages.set(makeKey(url), Date.now());
}

export function isInFailTileCached(url) {
    if (!isString(url)) return false;
    const time=  failedCachedImages.get(makeKey(url));
    if (!time) return false;
    const found= Date.now()-time < (1000 * 10); // search less than 10 seconds old
    if (!found) failedCachedImages.delete(url);
    return found;
}

export function removeNonNativeCachedTiles() {
    cachedImages= removeNonNativeTilesForCache(cachedImages);
    cachedAllSkyImages= removeNonNativeTilesForCache(cachedAllSkyImages);
}


function cleanupCache(cacheMap, maxEntries) {
    const entries= Array.from(cacheMap.entries()).sort( (e1, e2) => e2[1].time-e1[1].time);
    if (entries.length>maxEntries) entries.length= maxEntries;
    return new Map(entries);
}

function removeNonNativeTilesForCache(cacheMap) {
    const entries= Array.from(cacheMap.entries());
    const newEntries= entries.filter( ([k,v]) => v.colorTableId===-1);
    return new Map(newEntries);
}



function makeOrder2AllSkyImages(order3Image) {
    const sourceSize= order3Image.width/27;
    const targetSize= sourceSize*2;
    const allsky2Array= [];
    for(let i=0; i<192; i++) {
        const canvas= initOffScreenCanvas({width:targetSize, height:targetSize});
        const ctx=canvas.getContext('2d');

        for(let j=0; j<4; j++) {
            const order3pix= i*4 + j;
            const offset= Math.floor(order3pix/27);
            const sy= sourceSize * offset;
            const sx=  sourceSize * (order3pix - 27*offset);
            const dx= j<2 ? 0 : sourceSize;
            const dy= j%2===0 ? 0 : sourceSize;
            ctx.drawImage(order3Image, sx, sy, sourceSize,sourceSize, dx,dy  ,sourceSize,sourceSize );
        }
        allsky2Array[i]= canvas;
    }
    return allsky2Array;
}

function makeOrder3AllSkyImages(order3Image) {
    const sourceSize= order3Image.width/27;
    const allsky2Array= [];
    for(let order3pix=0; order3pix<768; order3pix++) {
        const canvas= initOffScreenCanvas({width:sourceSize, height:sourceSize});
        const ctx=canvas.getContext('2d');
        const offset= Math.floor(order3pix/27);
        const sy= sourceSize * offset;
        const sx=  sourceSize * (order3pix - 27*offset);
        ctx.drawImage(order3Image, sx, sy, sourceSize,sourceSize, 0,0,sourceSize,sourceSize );
        allsky2Array[order3pix]= canvas;
    }
    return allsky2Array;
}
