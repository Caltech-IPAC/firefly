/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/**
 * Many utility functions for working with HiPS.  Several of the more computationally intensive ones use cached results
 *
 * Here is some documentation on HiPS.
 * https://aladin.u-strasbg.fr/hips/hipsdoc.pdf
 * http:www.ivoa.net/documents/HiPS/20170519/REC-HIPS-1.0-20170519.pdf
 */


import {isUndefined} from 'lodash';
import {
    ang2pixNest, HealpixIndex, ORDER_MAX, radecToPolar, SpatialVector
} from '../externalSource/aladinProj/HealpixIndex.js';
import {getFireflySessionId} from '../Firefly';
import {encodeServerUrl, getRootURL, loadImage} from '../util/WebUtil.js';
import {CoordinateSys} from './CoordSys.js';
import {CysConverter} from './CsysConverter.js';
import {getFoV, primePlot} from './PlotViewUtil.js';
import {makeDevicePt, makeWorldPt} from './Point.js';
import {makeHiPSProjection} from './projection/Projection';
import {computeDistance, convertCelestial, toDegrees, toRadians} from './VisUtil.js';
import {changeHiPSProjectionCenter, getScreenPixScaleArcSec, isHiPSAitoff} from './WebPlot.js';


export const MAX_SUPPORTED_HIPS_LEVEL= ORDER_MAX-2;



let workingHealpixIdx;

/**
 * get a Healpix index for this nside
 * optimization: don't recreate if it created the same one the last call
 * @param {Number} nside
 * @return {HealpixIndex} the HealpixIndex object based on nside
 */
function getHealpixIndex(nside) {
    if (workingHealpixIdx?.nside!==nside) workingHealpixIdx= new HealpixIndex(nside);
    return workingHealpixIdx;
}



/**
 * Determine how deep we show a grid on the current Hips map.  This will change depending on how deeply it is zoomed.
 * @param plot
 * @return {number}
 */
export function getMaxDisplayableHiPSGridLevel(plot) {
    let {norder}= getHiPSNorderlevel(plot);
    norder = norder>3 ? norder+5 : norder+2;
    return (norder>MAX_SUPPORTED_HIPS_LEVEL) ? MAX_SUPPORTED_HIPS_LEVEL : norder;
}



const  angSizeCacheMap = new WeakMap(); // use week map since we don't want to keep old plot objects

/**
 * Return the angular size of the pixel at the nOrder level for this plot.
 * Results are cached so this function is very efficient.
 * @param plot
 * @return {*}
 */
export function getPlotTilePixelAngSize(plot) {
    if (!plot) return 0;
    let size= angSizeCacheMap.get(plot);
    if (isUndefined(size)) {
        size= getTilePixelAngSize(getHiPSNorderlevel(plot,true).norder);
        angSizeCacheMap.set(plot, size);
    }
    return size;
}



const tilePixelAngSizeCacheMap= {};

/**
 * Return the angular size of the pixel of a nOrder level. this function assumes 512x512 tiles sizes
 * Results are cached so this function is very efficient.
 * @param {Number} nOrder
 * @return {Number} the angular size of a pixel in a HiPS tile
 */
export function getTilePixelAngSize(nOrder) {
    nOrder= Math.trunc(nOrder);
    if (tilePixelAngSizeCacheMap[nOrder]) return tilePixelAngSizeCacheMap[nOrder];
    const rad= Math.sqrt(4*Math.PI / (12*((512*(2**nOrder))**2)));
    tilePixelAngSizeCacheMap[nOrder]= toDegrees(rad);
    return tilePixelAngSizeCacheMap[nOrder];
}

/**
 *
 * @param {WebPlot} plot
 * @param {boolean} [limitToImageDepth] When true, do not return a number that is greater than what this HiPS map
 * can display.  Use hipsProperties.hips_order to determine.
 * @return {{useAllSky:boolean, norder:number, desiredNorder:number, isMaxOrder:boolean}} norder is the result, useAllSky true when the norder is 2 or 3 but
 * is zoom out so much that the full norder 3 tiles are not necessary, if all sky map is available the is should be used
 * for drawing.
 */
export function getHiPSNorderlevel(plot, limitToImageDepth= false) {
    if (!plot) return {norder:-1, useAllSky:false};

    const screenPixArcsecSize= getScreenPixScaleArcSec(plot);
    if (screenPixArcsecSize> 130) return  {useAllSky:true, norder:2, desiredNorder:2};
    if (screenPixArcsecSize> 100) return  {useAllSky:true, norder:3, desiredNorder:3};

    let norder= getNOrderForPixArcSecSize(screenPixArcsecSize);
    if (norder>MAX_SUPPORTED_HIPS_LEVEL) norder= MAX_SUPPORTED_HIPS_LEVEL;
    const desiredNorder= norder;
    let maxOrder= Number(plot.hipsProperties?.hips_order);

    if (limitToImageDepth) {
        if (!maxOrder) {
            const hipsPixelScale= Number(plot.hipsProperties?.hips_pixel_scale);
            maxOrder= hipsPixelScale ? getNOrderForPixArcSecSize(hipsPixelScale*3600) : 3;
        }
        norder= Math.min(norder, maxOrder);
    }
    return {norder, desiredNorder, useAllSky:false, isMaxOrder:norder===maxOrder};

}


export function getCatalogNorderlevel(plot,minNOrder=7, maxNorder,gridSize=128) {
    if (!plot) return -1;

    const screenPixArcsecSize= getScreenPixScaleArcSec(plot);
    if (screenPixArcsecSize> 100) return minNOrder;
    const sizeInArcSecKey= `${screenPixArcsecSize.toFixed(7)}---${gridSize}`;
    let norder= catalogNOrderForPixAsSizeCacheMap[sizeInArcSecKey];
    if (!norder) {
        const nside = HealpixIndex.calculateNSide(screenPixArcsecSize*gridSize);
        norder= Math.max(minNOrder, Math.log2(nside));
        norder= Math.min(norder, maxNorder);
        if (norder>maxNorder) norder= maxNorder;
        catalogNOrderForPixAsSizeCacheMap[sizeInArcSecKey]= norder;
    }
    return norder;
}






const nOrderForPixAsSizeCacheMap= {};
const catalogNOrderForPixAsSizeCacheMap= {};

/**
 * Return the best norder for a given screen pixel angular size in arc seconds
 * Results are cached so this function is very efficient.
 * @param {Number} sizeInArcSec - pixel size in arc seconds
 * @return {Number} the best norder for the pixel
 */
function getNOrderForPixArcSecSize(sizeInArcSec) {
    const sizeInArcSecKey= sizeInArcSec.toFixed(7);
    let norder= nOrderForPixAsSizeCacheMap[sizeInArcSecKey];
    if (isUndefined(norder)) {
        const nside = HealpixIndex.calculateNSide(sizeInArcSec*512); // 512 size tiles hardcoded, should fix

        norder = Math.log2(nside);
        norder= Math.max(3, norder);
        nOrderForPixAsSizeCacheMap[sizeInArcSecKey]= norder;
    }
    return norder;
}

function getCatalogNOrderForPixArcSecSize(sizeInArcSec) {
    const sizeInArcSecKey= sizeInArcSec.toFixed(7);
    let norder= catalogNOrderForPixAsSizeCacheMap[sizeInArcSecKey];
    if (isUndefined(norder)) {
        const nside = HealpixIndex.calculateNSide(sizeInArcSec*56);
        norder = Math.log2(nside);
        norder= Math.max(5, norder);
        catalogNOrderForPixAsSizeCacheMap[sizeInArcSecKey]= norder;
    }
    return norder;
}

export function makeHiPSTileUrl(plot, nOrder, tileNumber) {
    if (!plot) return null;
    const dir= Math.floor(tileNumber/10000)*10000;
    const exts= plot.hipsProperties?.hips_tile_format ?? 'jpg';
    const cubeExt= plot.cubeDepth>1 && plot.cubeIdx>0 ? '_'+plot.cubeIdx : '';
    const root= plot.hipsUrlRoot.endsWith('/') ? plot.hipsUrlRoot : plot.hipsUrlRoot+'/';
    return makeHipsUrl(`${root}Norder${nOrder}/Dir${dir}/Npix${tileNumber}${cubeExt}.${getHiPSTileExt(exts)}`,
        plot.proxyHips, plot.hipsFromHipsList);
}

/**
 * @param urlRoot
 * @param exts
 * @param cubeIdx
 * @param proxy
 * @return {*}
 */
export function makeHiPSAllSkyUrl(urlRoot,exts,cubeIdx= 0, proxy= false) {
    if (!urlRoot || !exts) return null;
    const cubeExt= cubeIdx? '_'+cubeIdx : '';
    const root= urlRoot.endsWith('/') ? urlRoot : urlRoot+'/';
    return makeHipsUrl(`${root}Norder3/Allsky${cubeExt}.${getHiPSTileExt(exts)}`, proxy, true);
}

export function makeHiPSPropertiesUrl(urlRoot,proxy) {
    const root= urlRoot.endsWith('/') ? urlRoot : urlRoot+'/';
    return makeHipsUrl(`${root}properties`, proxy, false);
}

export function makeHiPSAllSkyUrlFromPlot(plot) {
    if (!plot) return null;
    const exts= plot.hipsProperties?.hips_tile_format ?? 'jpg';
    const cubeIdx= plot.cubeDepth>1 && plot.cubeIdx>0 ? plot.cubeIdx : 0;
    return makeHiPSAllSkyUrl(plot.hipsUrlRoot, exts, cubeIdx, plot.proxyHips);

}


/**
 * Build the HiPS url
 * @param {String} url
 * @param {boolean} proxy - make a URL the uses proxying
 * @param {boolean} alwaysUseCached - true if this url references a hips tile
 * @return {String} the modified url
 */
export function makeHipsUrl(url, proxy, alwaysUseCached=false) {
    if (!proxy) return url;
    const params= {
        downloadSessionId: getFireflySessionId(),
        hipsUrl : url,
        alwaysUseCached,
    };
    return encodeServerUrl(getRootURL() + 'servlet/Download', params);
}


/**
 * Choose an extension to use from the available extensions.
 * @param {Array.<String>} exts
 * @return {string}
 */
export function getHiPSTileExt(exts) {
    if (!exts) return null;
    if (exts.includes('png')) return'png';
    else if (exts.includes('jpeg') || exts.includes('jpg')) return 'jpg';
    else return 'jpg';

}

/**
 *
 * @param {WebPlot} plot
 * @param {Dimension} viewDim
 * @return {{centerWp:WorldPt, fov: number, centerDevPt: DevicePt}}
 */
export function getPointMaxSide(plot, viewDim) {

    const cc= CysConverter.make(plot);
    const {width,height}= viewDim;
    const centerDevPt= makeDevicePt(width/2, height/2);
    const centerWp= cc.getWorldCoords( centerDevPt, plot.imageCoordSys);

    const ptWidth= cc.getWorldCoords( makeDevicePt(width,0), plot.imageCoordSys);
    const ptHeight= cc.getWorldCoords( makeDevicePt(0,height), plot.imageCoordSys);


    if (!ptWidth || !ptHeight) {
        return {centerWp,centerDevPt, fov:isHiPSAitoff(plot) ? 360 : 180};
    }
    return {centerWp, centerDevPt, fov:getFoV(plot)};
}


/**
 *
 * @param {PlotView} pv
 * @param {number} fov in degrees
 * @return {number} a zoom level
 */
export function getHiPSZoomLevelForFOV(pv, fov) {
    const {width,height}=pv.viewDim;
    const plot= primePlot(pv);
    if (!plot || !width || !height) return 1;
   
    // make version of plot centered at 0,0 with zoom level 1
    const tmpPlot= changeHiPSProjectionCenter({...plot, zoomFactor:1}, makeWorldPt(0,0, plot.imageCoordSys));
    const cc= CysConverter.make(tmpPlot);
    const pt1= cc.getDeviceCoords( makeWorldPt(0,0, plot.imageCoordSys));
    const pt2= cc.getDeviceCoords( makeWorldPt(fov/2,0, plot.imageCoordSys));
    if (!pt1 || !pt2) return 0;
    const devSize= Math.abs(pt2.x-pt1.x)*2;
    return width / devSize;
}


function makeAllCorners(nOrder, coordSys) {
    const nside= 2**nOrder;
    const pixCnt = HealpixIndex.nside2Npix(nside);
    const pixList= Array.from({length:pixCnt}, (v,ipix) => ipix);
    const hpxIdx = getHealpixIndex(nside);
    return pixList.map( (ipix) => {
        const corners = hpxIdx.corners_nest(ipix, 1);
        const wpCorners= corners.map( (c) => specVectToWP(c,coordSys) );
        return { ipix, wpCorners };
    });
}



/**
 * This function make an object (with functions) to cache allsky type computations.
 * @return {*}
 */
function makeHealpixCornerCacheTool() {

    const nsideToNorder= {8:3, 4:2, 2:1}; // this is what is in the cache

    const j2Corners=[undefined,
        makeAllCorners(1, CoordinateSys.EQ_J2000),
        makeAllCorners(2, CoordinateSys.EQ_J2000),
        makeAllCorners(3, CoordinateSys.EQ_J2000),
    ];

    const galCorners=  [undefined,
        makeAllCorners(1, CoordinateSys.GALACTIC),
        makeAllCorners(2, CoordinateSys.GALACTIC),
        makeAllCorners(3, CoordinateSys.GALACTIC),
    ];


    return {
       findCacheData(nside, coordSys, ipix= undefined)  {
           const norder= nsideToNorder[nside];
           if (!norder) return null;
           const fullAry= isUndefined(ipix);
           if (CoordinateSys.EQ_J2000.toString()===coordSys.toString()) return fullAry ? j2Corners[norder] : j2Corners[norder][ipix];
           if (CoordinateSys.GALACTIC.toString()===coordSys.toString()) return fullAry ? galCorners[norder] :galCorners[norder][ipix];
           return null;
       },

        makeCornersForPix(ipix, nside, coordSys) {
            const cacheEntry= this.findCacheData(nside,coordSys, ipix);
            if (cacheEntry) return cacheEntry;

            const corners = getHealpixIndex(nside).corners_nest(ipix, 1);
            const wpCorners= corners.map( (c) => specVectToWP(c, coordSys) );
            return { ipix, wpCorners };
        },

        /**
         *
         * @param {number} norder - must be 1, 2 or 3
         * @param coordSys
         * @return {*}
         */
        getFullCellList(norder, coordSys)  {
            return this.findCacheData(2**norder,coordSys);
        },
    };
}


/**
 * @Function
 * lazily defined.
 * @param {number} ipix
 * @param {number} nside
 * @param {HealpixIndex} healpixIdx
 */
let healpixCache;

export function getHealpixCornerTool() {
    if (!healpixCache) healpixCache= makeHealpixCornerCacheTool();
    return healpixCache;
}

/**
 *
 * @param {WebPlot} plot
 * @param {WorldPt} wp
 * @return {{norder:number, pixel:number}} the pixel if we can go that deep, undefined otherwise
 */
export function getHealpixPixel(plot, wp) {
    const {norder}= getHiPSNorderlevel(plot, true);
    if (norder>MAX_SUPPORTED_HIPS_LEVEL-9) return undefined;
    const polar = radecToPolar(wp.x,wp.y);
    const tilePixel= ang2pixNest(polar.theta, polar.phi,2**(norder));
    const pixel= ang2pixNest(polar.theta, polar.phi,2**(norder+9));
    const tileCoords= healpixPixelTo512TileXY(pixel);
    return { norder:norder+9, tileNorder: norder, pixel, tilePixel, tileCoords };
}


const twoPos= [256,128,64,32,16,8,4,2,1];

/**
 * Get the x,y of the tile for the healpix pixel. This assumes 512x512 tiles.
 * @param pixel
 * @return {{x: number, y: number}}
 */
function healpixPixelTo512TileXY(pixel) {
    const tilePixel= Math.trunc(pixel/(512*512));
    const internalPixOffset= pixel - (tilePixel*(512*512));
    const pixStr= internalPixOffset.toString(2).padStart(18,'0');
    let x= 0;
    let y= 0;
    let j;
    for(let i= 0; i<17; i+=2) {
        j= i/2;
        const bits= pixStr.substring(i,i+2);
        switch (bits) {
            case '01':
                y+= twoPos[j];
                break;
            case '10':
                x+= twoPos[j];
                break;
            case '11':
                y+= twoPos[j];
                x+= twoPos[j];
                break;
        }
    }
    return {x,y};
}

/**
 *
 * @param norder
 * @param desiredNorder - when render very deep desired norder give an indication how deep the zoom is beyond the tile level
 * @param {WorldPt} centerWp - center of visible area, coordinate system of this point should be same as the projection
 * @param {number} fov - Math.max(width, height) of the field of view in degrees (i think)
 * @param {{width:number,height:number}} viewDim
 * @param {CoordinateSys} dataCoordSys
 * @param {Boolean} isAitoff
 * @return {Array.<{ipix:number, wpCorners:Array.<WorldPt>}>} an array of objects the contain the healpix
 *            pixel number and a worldPt array of corners
 */
export function getVisibleHiPSCells (norder, desiredNorder, centerWp, fov, viewDim, dataCoordSys, isAitoff= false) {
    if (isAitoff && fov > 130 && norder<=3) { // return all cells
        return getHealpixCornerTool().getFullCellList(norder,dataCoordSys);
    }
    else if (fov>80 && norder<=3) { // get all the cells and filter them
        const dataCenterWp= convertCelestial(centerWp, dataCoordSys);
        return filterAllSky(dataCenterWp, getHealpixCornerTool().getFullCellList(norder,dataCoordSys));
    }
    else { // get only the healpix number for the fov and create the cell list
        return getPixCellList(norder,desiredNorder, centerWp,fov,viewDim, dataCoordSys);
    }
}

/**
 * get only the healpix number for the fov and create the cell list
 * @param norder
 * @param desiredNorder - when render very deep desired norder give an indication how deep the zoom is beyond the tile level
 * @param {WorldPt} centerWp - center of visible area, coordinate system of this point should be same as the projection
 * @param {number} fov - Math.max(width, height) of the field of view in degrees (i think)
 * @param {{width:number, height:number}} viewDim
 * @param {CoordinateSys} dataCoordSys
 * @return {Array.<{ipix:number, wpCorners:Array.<WorldPt>}>} an array of objects the contain the healpix
 *            pixel number and a worldPt array of corners
 */
function getPixCellList(norder,desiredNorder, centerWp, fov, viewDim, dataCoordSys) {
    const {width,height}= viewDim;
    const diag= (width**2 + height**2)**.5; // Pythagorean theorem
    const diagRatio= diag/width;
    const healpixCache=getHealpixCornerTool();
    const dataCenterWp= convertCelestial(centerWp, dataCoordSys);
    const norderToUse= norder>=desiredNorder ? norder : desiredNorder;
    const radiusRad= getSearchRadiusInRadians(fov*diagRatio); // use a radius for the diagonal of the view
    const nsideToUse = 2**norderToUse;
    const pixList = getHealpixIndex(nsideToUse).queryDisc(wpToSpecVect(dataCenterWp), radiusRad, true, true);
    const pixShift= 4**(desiredNorder-norder);
    const list= norder>=desiredNorder ? pixList : [...new Set(pixList.map((pix) => Math.trunc(pix/pixShift)))];
    const nside= 2**norder;
    return list.map( (ipix) => healpixCache.makeCornersForPix(ipix, nside, dataCoordSys));

}

export function getCornersForCell(norder, ipix, dataCoordSys) {
    return (norder<=3) ?
        getHealpixCornerTool().getFullCellList(norder, dataCoordSys)?.[ipix]?.wpCorners :
        getHealpixCornerTool().makeCornersForPix(ipix, 2**norder, dataCoordSys); // this branch in this call is untested

}

/**
 * get any HiPS cells which is fully or partially visible within given fov area
 * @param {Number} norder
 * @param {WorldPt} centerWp
 * @param {Number} fov
 * @param {CoordinateSys} dataCoordSys
 * @param {Boolean} isAitoff
 * @returns {Array.<{ipix:Number, wpCorners:Array.<WorldPt>}>}
 */
export function getAllVisibleHiPSCells (norder, centerWp, fov, dataCoordSys, isAitoff) {
    const healpixCache= getHealpixCornerTool();
    const dataCenterWp= convertCelestial(centerWp, dataCoordSys);

    if (isAitoff && fov > 130 && norder<=3) {
        return healpixCache.getFullCellList(norder,dataCoordSys);
    }
    else if (fov>80 && norder<=3) { // get all the cells and filter them
        const cells = healpixCache.getFullCellList(norder,dataCoordSys);

        return cells.filter( (cell) =>{
            const {wpCorners}= cell;
            const visibleCorner = wpCorners.find((oneCorner) => computeDistance(dataCenterWp, oneCorner) < 90);

            return visibleCorner;

        });
    } else { // get only the healpix number for the fov and create the cell list
        const nside = 2**norder;
        const pixList = getHealpixIndex(nside).queryDisc(wpToSpecVect(dataCenterWp), getSearchRadiusInRadians(fov), true, true);
        return pixList.map( (ipix) => healpixCache.makeCornersForPix(ipix, nside, dataCoordSys));
    }
}

/**
 *
 * @param {WorldPt} wp
 * @return {SpatialVector}
 */
export function wpToSpecVect(wp) {
    const spatialVector = new SpatialVector();
    spatialVector.set(wp.getLon(),wp.getLat());
    return spatialVector;
}

function specVectToWP(spVec,coordSys) {
    return makeWorldPt(spVec.ra(), spVec.dec(), coordSys);

}


/**
 * convert to radius and extend a little (suggestion from Aladin)
 * @param {number} fov in degrees
 * @return {number} extended radius in radians
 */
function getSearchRadiusInRadians(fov) {

    let v;
    if (fov>60) v= toRadians((fov/2)* 1.6);
    else if (fov>12) v= toRadians((fov/2)*1.45);
    else v= toRadians((fov/2)* 1.1);
    return v>Math.PI ? Math.PI : v;
}


function filterAllSky(centerWp, cells) {
    return cells.filter( (cell) =>{
        const {wpCorners}= cell;
        return (computeDistance(centerWp, wpCorners[0]) <100 || computeDistance(centerWp, wpCorners[2]) <100);
    });
}

export const API_HIPS_CONSTANTS= {
    TWO_MASS: 'http://alasky.u-strasbg.fr/2MASS/Color',
    DSS_COLORED: 'http://alasky.u-strasbg.fr/DSS/DSSColor',
    DSS2_RED: 'http://alasky.u-strasbg.fr/DSS/DSS2Merged',
    AllWISE_COLOR: 'http://alasky.u-strasbg.fr/AllWISE/RGB-W4-W2-W1/',
    IRAC_COLOR: 'http://alasky.u-strasbg.fr/SpitzerI1I2I4color',
};

/**
 * return the value of the constant or if not found return the given value.
 * @param c
 * @return {*}
 */
export function resolveHiPSConstant(c) {
    return API_HIPS_CONSTANTS[c] || c;

}

export function tileCoordsWrap(cc, [wp1,wp2,wp3,wp4], factor= 3) {
    const wrap=
        cc.coordsWrap(wp1,wp2,factor) ||
        cc.coordsWrap(wp1,wp4,factor) ||
        cc.coordsWrap(wp2,wp3,factor) ||
        cc.coordsWrap(wp3,wp4,factor);
    return wrap;
}

/**
 * get property value, some key may have alternate one to get the value, need more investigation
 * @param properties
 * @param pkey
 * @returns {*}
 */

export function getPropertyItem(properties, pkey) {
    if (pkey === 'ivoid') {
        return properties['creator_did'] || properties['publisher_did'];
    } else {
        return properties[pkey];
    }
}

/**
 * check if tile1 (norder1 & npix1) is within tile2 (norder2 & npix2)
 * @param norder1
 * @param npix1
 * @param norder2
 * @param npix2
 * @returns {boolean}
 */
export function isTileInside(norder1, npix1, norder2, npix2) {
    // make norder1 as the lower order
    if (norder1 === norder2) {
        return npix1 === npix2;
    }

    if (norder1 < norder2) {
        return false;
    }

    // norder1 > norder2
    const subTotal = 4**(norder1-norder2);
    const base_npix = npix2*subTotal;

    return (npix1 >= base_npix) && (npix1 < (base_npix+subTotal));

}

/**
 * replace the hips projection if the coordinate system changes
 * @param {WebPlot|undefined} plot
 * @param coordinateSys
 * @param {WorldPt} wp
 * @return {WebPlot}
 */
export function replaceHiPSProjection(plot, coordinateSys, wp = makeWorldPt(0, 0)) {
    const newWp = convertCelestial(wp, coordinateSys);
    const projection = makeHiPSProjection(coordinateSys, newWp.x, newWp.y, isHiPSAitoff(plot));
    //note: the dataCoordSys stays the same
    return {...plot, imageCoordSys: projection.coordSys, projection, allWCSMap: {'': projection}};
}


const loadBegin= new Map();
const waitingResolvers= new Map();
const waitingRejectors= new Map();

function clearLoadImageCacheEntry(url) {
    loadBegin.delete(url);
    waitingResolvers.delete(url);
    waitingRejectors.delete(url);
}

export async function loadImageMultiCall(url) {

    if (!waitingResolvers.has(url)) waitingResolvers.set(url,[]);
    if (!waitingRejectors.has(url)) waitingRejectors.set(url,[]);

    if (!loadBegin.get(url)) {
        loadBegin.set(url,true);
        loadImage(url).then( (im) => {
            im ?
                waitingResolvers.get(url)?.forEach((r) => r(im)) :
                waitingRejectors.get(url)?.forEach( (r) => r(new Error('could not load image')));
            clearLoadImageCacheEntry(url);
        }).catch( (err) => {
            waitingRejectors.get(url)?.forEach( (r) => r(err));
            clearLoadImageCacheEntry(url);
        });
    }

    return new Promise( function(resolve, reject) {
        waitingResolvers.get(url).push(resolve);
        waitingRejectors.get(url).push(reject);
    });
}


