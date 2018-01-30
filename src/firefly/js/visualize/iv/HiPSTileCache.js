/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {initOffScreenCanvas} from './TileDrawHelper.jsx';


const MAX_TILE_IMAGES= 150;
const MAX_ALLSKY_IMAGES= 20;

let cachedImages= new Map();
let cachedAllSkyImages= new Map();

/**
 * @global
 * @public
 * @typedef {Object} HiPSAllSkyCacheInfo
 *
 * @prop {string} url
 * @prop {Image}  order3 single image with the all order 3 allsky
 * @prop {Array.<Canvas>}  order2Array array of order 2 image tiles, each index if the tile number
 */


/**
 *
 * @param url
 * @return {HiPSAllSkyCacheInfo}
 */
export function findAllSkyCachedImage(url) {
    const result=  cachedAllSkyImages.get(url);
    if (result) result.time= Date.now();
    return result;
}

export function addAllSkyCachedImage(url, image) {
    const order2AllSky= makeOrder2AllSkyImages(image);
    cachedAllSkyImages.set(url, {url, order3:image, order2Array: order2AllSky, colorTable: 'todo', time: Date.now()});
    if (cachedAllSkyImages.size>MAX_ALLSKY_IMAGES+(MAX_ALLSKY_IMAGES*.1)) {
        cachedAllSkyImages= cleanupCache(cachedAllSkyImages,MAX_ALLSKY_IMAGES);
    }
}


export function findTileCachedImage(url) {
    const result=  cachedImages.get(url);
    if (result) result.time= Date.now();
    return result;
}


export function addTileCachedImage(url, image, emptyTile= false) {
    cachedImages.set(url, {url, image, emptyTile, colorTable: 'todo', time: Date.now()});
    if (cachedImages.size>MAX_TILE_IMAGES+(MAX_TILE_IMAGES*.25)) {
        cachedImages= cleanupCache(cachedImages, MAX_TILE_IMAGES);
    }
}





function cleanupCache(cacheMap, maxEntries) {
    const entries= Array.from(cacheMap.entries()).sort( (e1, e2) => e2[1].time-e1[1].time);
    if (entries.length>maxEntries) entries.length= maxEntries;
    return new Map(entries);
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
