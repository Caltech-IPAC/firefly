/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {initOffScreenCanvas} from './TileDrawHelper.jsx';


const MAX_TILE_IMAGES= 50;
const MAX_PROPERTIES= 100;
const MAX_ALLSKY_IMAGES= 10;

const cachedImages= [];
const cachedAllSkyImages=[];
const cachedProperties=[];



export function findAllSkyCachedImage(url) {
     const result= cachedAllSkyImages.find( (v) => v.url===url);
     return result;
}

export function addAllSkyCachedImage(url, image) {
    const order2AllSky= makeOrder2AllSkyImages(image);
    cachedAllSkyImages.unshift( {url, order3:image, order2Array: order2AllSky, colorTable: 'todo'});
    if (cachedAllSkyImages.length>MAX_ALLSKY_IMAGES) cachedAllSkyImages.length= MAX_ALLSKY_IMAGES;
}



export function findTileCachedImage(url) {
    return cachedImages.find( (v) => v.url===url);
}

export function addTileCachedImage(url, image, emptyTile= false) {
    cachedImages.unshift( {url, image, emptyTile, colorTable: 'todo'});
    if (cachedImages.length>MAX_TILE_IMAGES) cachedImages.length= MAX_TILE_IMAGES;
}

export function findCachedPropertiesJSON(url,prop) {
    const result= cachedProperties.find( (v) => v.prop===prop);
    return result && result.prop;
}

export function addCachedPropertiesJSON(url,prop) {
    cachedProperties.unshift( {url, prop});
    if (cachedProperties.length>MAX_PROPERTIES) cachedProperties.length= MAX_PROPERTIES;
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
