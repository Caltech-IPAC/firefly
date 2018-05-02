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


import {get, isUndefined, isNumber} from 'lodash';
import {clone} from '../util/WebUtil.js';
import {CysConverter} from './CsysConverter.js';
import {makeWorldPt, makeDevicePt} from './Point.js';
import {SpatialVector, HealpixIndex, radecToPolar, ORDER_MAX} from '../externalSource/aladinProj/HealpixIndex.js';
import {convert, computeDistance} from './VisUtil.js';
import {replaceHeader, getScreenPixScaleArcSec} from './WebPlot.js';
import {primePlot} from './PlotViewUtil.js';
import CoordinateSys from './CoordSys';
import {encodeServerUrl} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {toDegrees, toRadians} from './VisUtil.js';


export const MAX_SUPPORTED_HIPS_LEVEL= ORDER_MAX-1;



var workingHealpixIdx;
function getHealpixIndex(nside) {
    if (!isNumber(nside)) return undefined;
    if (!workingHealpixIdx || workingHealpixIdx.nside!==nside) workingHealpixIdx= new HealpixIndex(nside);
    return workingHealpixIdx;
}



/**
 *
 * @param {WebPlot} plot
 * @param {WorldPt} wp new center of projection
 */
export function changeProjectionCenter(plot, wp) {
    if (!plot) return undefined;
    wp= convert(wp, plot.projection.coordSys);
    const header= clone(plot.projection.header, {crval1:wp.x, crval2:wp.y});
    return replaceHeader(plot,header);
}


/**
 * Determine how deep we show a grid on the current Hips map.  This will change depending on how deeply it is zoomed.
 * @param plot
 * @return {number}
 */
export function getMaxDisplayableHiPSGridLevel(plot) {
    let {norder}= getHiPSNorderlevel(plot);
    norder = norder>3 ? norder+3 : norder+2;
    if (norder>MAX_SUPPORTED_HIPS_LEVEL) norder= MAX_SUPPORTED_HIPS_LEVEL;
    return norder;
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
    // const rad= Math.sqrt(4*Math.PI / (12*Math.pow(512*Math.pow(2,nOrder) , 2)));
    const rad= Math.sqrt(4*Math.PI / (12*((512*(2**nOrder))**2)));
    tilePixelAngSizeCacheMap[nOrder]= toDegrees(rad);
    return tilePixelAngSizeCacheMap[nOrder];
}

/**
 *
 * @param {WebPlot} plot
 * @param {boolean} [limitToImageDepth] When true, do not return a number that is greater than what this HiPS map
 * can display.  Use hipsProperties.hips_order to determine.
 * @return {{useAllSky:boolean, norder:number}} norder is the result, useAllSky true when the norder is 2 or 3 but
 * is zoom out so much that the full norder 3 tiles are not necessary, if all sky map is available the is should be used
 * for drawing.
 */
export function getHiPSNorderlevel(plot, limitToImageDepth= false) {
    if (!plot) return {norder:-1, useAllSky:false};

    const screenPixArcsecSize= getScreenPixScaleArcSec(plot);
    if (screenPixArcsecSize> 130) return  {useAllSky:true, norder:2};
    if (screenPixArcsecSize> 100) return  {useAllSky:true, norder:3};

    let norder= getNOrderForPixArcSecSize(screenPixArcsecSize);

    if (limitToImageDepth) {
        const maxOrder= Number(get(plot, 'hipsProperties.hips_order', '3'));
        norder= Math.min(norder, maxOrder);
    }
    return {norder, useAllSky:false};

}

const nOrderForPixAsSizeCacheMap= {};

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

        // norder = Math.log(nside)/Math.log(2); // convert to a base 2 log - 	logb(x) = logc(x) / logc(b)
        norder = Math.log2(nside); // convert to a base 2 log - 	logb(x) = logc(x) / logc(b)
        norder= Math.max(3, norder);
        nOrderForPixAsSizeCacheMap[sizeInArcSecKey]= norder;
    }
    return norder;
}


export function makeHiPSTileUrl(plot, nOrder, tileNumber) {
    if (!plot) return null;
    const dir= Math.floor(tileNumber/10000)*10000;
    const exts= get(plot, 'hipsProperties.hips_tile_format', 'jpg');
    const cubeExt= plot.cubeDepth>1 && plot.cubeIdx>0 ? '_'+plot.cubeIdx : '';
    return makeHipsUrl(`${plot.hipsUrlRoot}/Norder${nOrder}/Dir${dir}/Npix${tileNumber}${cubeExt}.${getHiPSTileExt(exts)}`, plot.proxyHips);
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
    return makeHipsUrl(`${urlRoot}/Norder3/Allsky${cubeExt}.${getHiPSTileExt(exts)}`, proxy);
}

export function makeHiPSAllSkyUrlFromPlot(plot) {
    if (!plot) return null;
    const exts= get(plot, 'hipsProperties.hips_tile_format', 'jpg');
    const cubeIdx= plot.cubeDepth>1 && plot.cubeIdx>0 ? plot.cubeIdx : 0;
    return makeHiPSAllSkyUrl(plot.hipsUrlRoot, exts, cubeIdx, plot.proxyHips);

}


/**
 * Build the HiPS url
 * @param {String} url
 * @param {boolean} proxy - make a URL the uses proxying
 * @return {String} the modified url
 */
export function makeHipsUrl(url, proxy) {
    if (proxy) {
        const params= {
            hipsUrl : url
        };
        return encodeServerUrl(getRootURL() + 'servlet/Download', params);
    }
    else {
        return url;
    }

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

    const pt0= cc.getWorldCoords( makeDevicePt(0,0), plot.imageCoordSys);
    const ptWidth= cc.getWorldCoords( makeDevicePt(width,0), plot.imageCoordSys);
    const ptHeight= cc.getWorldCoords( makeDevicePt(0,height), plot.imageCoordSys);


    if (!ptWidth || !ptHeight) {
        return {centerWp,centerDevPt, fov:180};
    }

    const widthInDeg= computeDistance(pt0, ptWidth);
    const heightInDeg= computeDistance(pt0, ptHeight);
    return {centerWp, centerDevPt, fov:Math.max(widthInDeg,heightInDeg)};
}


/**
 *
 * @param {PlotView} pv
 * @param {number} size in degrees
 * @return {number} a zoom level
 */
export function getHiPSZoomLevelToFit(pv,size) {
    const {width,height}=pv.viewDim;
    const plot= primePlot(pv);
    if (!plot || !width || !height) return 1;

    // make version of plot centered at 0,0
    const tmpPlot= changeProjectionCenter(plot, makeWorldPt(0,0, plot.imageCoordSys));
    const cc= CysConverter.make(tmpPlot);
    const pt1= cc.getImageCoords( makeWorldPt(0,0, plot.imageCoordSys));
    const pt2= cc.getImageCoords( makeWorldPt(size,0, plot.imageCoordSys));
    return Math.min(width, height)/Math.abs(pt2.x-pt1.x);
}

/**
 *
 * @param {PlotView} pv
 * @return {number} fov in degrees
 */
export function getHiPSFoV(pv) {
    const cc= CysConverter.make(primePlot(pv));
    const {width,height}=pv.viewDim;
    if (!cc || !width || !height) return;

    const pt1= cc.getWorldCoords( makeDevicePt(1,height/2));
    const pt2= cc.getWorldCoords( makeDevicePt(width-1,height/2));
    return (pt1 && pt2) ? computeDistance(pt1,pt2) : 180;
}


function makeAllCorners(nOrder, coordSys) {
    const nside= 2**nOrder;
    const pixCnt = HealpixIndex.nside2Npix(nside);
    const pixList= new Array(pixCnt).fill(0).map( (v,ipix) => ipix);
    const hpxIdx = getHealpixIndex(nside);
    const spVec = new SpatialVector();
    return pixList.map( (ipix) => {
        const corners = hpxIdx.corners_nest(ipix, 1);
        const wpCorners= corners.map( (c) => {
            spVec.setXYZ(c.x, c.y, c.z);
            return makeWorldPt(spVec.ra(), spVec.dec(), coordSys);
        });
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
           switch (coordSys) {
               case CoordinateSys.EQ_J2000: return fullAry ? j2Corners[norder] : j2Corners[norder][ipix];
               case CoordinateSys.GALACTIC: return fullAry ? galCorners[norder] :galCorners[norder][ipix];
               default: return null;
           }
       },

        makeCornersForPix(ipix, nside, coordSys) {
            const cacheEntry= this.findCacheData(nside,coordSys, ipix);
            if (cacheEntry) return cacheEntry;

            const corners = getHealpixIndex(nside).corners_nest(ipix, 1);
            const spVec = new SpatialVector();
            const wpCorners= corners.map( (c) => {
                spVec.setXYZ(c.x, c.y, c.z);
                return makeWorldPt(spVec.ra(), spVec.dec(), coordSys);
            });
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
var healpixCache;

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
    //todo

    const {norder}= getHiPSNorderlevel(plot, true);
    if (norder>MAX_SUPPORTED_HIPS_LEVEL-9) return undefined;
    const hpxIdx= getHealpixIndex(2**(norder+9));

    const polar = radecToPolar(wp.x,wp.y);
    return {norder:norder+9, pixel:hpxIdx.ang2pix_nest(polar.theta, polar.phi)};
}


/**
 *
 * @param norder
 * @param {WorldPt} centerWp - center of visible area, coorindate system of this point should be same as the projection
 * @param {number} fov - Math.max(width, height) of the field of view in degrees (i think)
 * @param {CoordinateSys} dataCoordSys
 * @return {Array.<{ipix:string, wpCorners:Array.<WorldPt>}>} an array of objects the contain the healpix
 *            pixel number and a worldPt array of corners
 */
export function getVisibleHiPSCells (norder, centerWp, fov, dataCoordSys) {
    const healpixCache= getHealpixCornerTool();
    const dataCenterWp= convert(centerWp, dataCoordSys);

    if (fov>80 && norder<=3) { // get all the cells and filter them
        return filterAllSky(dataCenterWp, healpixCache.getFullCellList(norder,dataCoordSys));
    }
    else { // get only the healpix number for the fov and create the cell list
        const nside = 2**norder;
        const pixList = getHealpixIndex(nside).queryDisc(makeSpatialVector(dataCenterWp), getSearchRadiusInRadians(fov), true, true);
        return pixList.map( (ipix) => healpixCache.makeCornersForPix(ipix, nside, dataCoordSys));
    }
}


/**
 *
 * @param {WorldPt} wp
 * @return {SpatialVector}
 */
export function makeSpatialVector(wp) {
    const spatialVector = new SpatialVector();
    spatialVector.set(wp.getLon(),wp.getLat());
    return spatialVector;
}


/**
 * convert to radius and extend a litte (suggestion from Aladin)
 * @param {number} fov in degrees
 * @return {number} exetended radius in radians
 */
function getSearchRadiusInRadians(fov) {

    if (fov>60) return toRadians((fov/2)* 1.6);
    else if (fov>12) return toRadians((fov/2)*1.45);
    else return toRadians((fov/2)* 1.1);
}


function filterAllSky(centerWp, cells) {
    return cells.filter( (cell) =>{
        const {wpCorners}= cell;
        return (computeDistance(centerWp, wpCorners[0]) <90);
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

