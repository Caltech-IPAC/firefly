/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


const MAX_TILE_IMAGES= 50;
const MAX_PROPERTIES= 100;
const MAX_ALLSkY_IMAGES= 5;

const cachedImages= [];
const cachedAllSkyImages=[];
const cachedProperties=[];



export function findAllSkyImage(url) {
     const result= cachedAllSkyImages.find( (v) => v.url===url);
     return result && result.image;
}

export function addAllSkyImage(url,image) {
    cachedAllSkyImages.unshift( {url, image, colorTable: 'todo'});
    if (cachedAllSkyImages.length>MAX_ALLSkY_IMAGES) cachedAllSkyImages.length= MAX_ALLSkY_IMAGES;
}



export function findTileImage(url) {
    return cachedImages.find( (v) => v.url===url);
}

export function addTileImage(url,image, emptyTile= false) {
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
