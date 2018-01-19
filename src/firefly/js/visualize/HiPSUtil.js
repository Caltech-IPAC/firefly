/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


// This module contain utilities for using HiPS. Here is some documentation on HiPS.
// https://aladin.u-strasbg.fr/hips/hipsdoc.pdf
// http:www.ivoa.net/documents/HiPS/20170519/REC-HIPS-1.0-20170519.pdf


import {get} from 'lodash';
import {clone} from '../util/WebUtil.js';
import {CysConverter} from './CsysConverter.js';
import {makeWorldPt, makeDevicePt} from './Point.js';
import {SpatialVector, HealpixIndex} from '../externalSource/aladinProj/HealpixIndex.js';
import {convert, computeDistance} from './VisUtil.js';
import {replaceHeader} from './WebPlot.js';
import {primePlot} from './PlotViewUtil.js';
import CoordinateSys from './CoordSys';
import {encodeServerUrl} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';


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


export function getMaxDisplayableHiPSLevel(plot) {
    let {norder}= getBestHiPSlevel(plot);
    norder = norder>3 ? norder+3 : norder+2;
    if (norder>15) norder= 15;
    return norder;
}


/**
 *
 * @param {WebPlot} plot
 * @param {boolean} [limitToImageDepth]
 * @return {{useAllSky:boolean, norder:number}}
 */
export function getBestHiPSlevel(plot, limitToImageDepth= false) {
    if (!plot) return {norder:-1, useAllSky:false};

    const {fov}= getPointMaxSide(plot,plot.viewDim);

    const screenPix= (fov/plot.viewDim.width)*3600;
    if (screenPix> 130) return  {useAllSky:true, norder:2};
    if (screenPix> 100) return  {useAllSky:true, norder:3};

    const nside = HealpixIndex.calculateNSide(screenPix*512);

    let norder = Math.log(nside)/Math.log(2);
    norder= Math.max(3, norder);

    if (limitToImageDepth) {
        const maxOrder= Number(get(plot, 'hipsProperties.hips_order', '3'));
        norder= Math.min(norder, maxOrder);
    }
    return {norder, useAllSky:false};

}


export function makeHiPSTileUrl(plot, nOrder, tileNumber) {
    if (!plot) return null;
    const dir= Math.floor(tileNumber/10000)*10000;
    const exts= get(plot, 'hipsProperties.hips_tile_format', 'jpg');
    const cubeExt= plot.cubeDepth>1 && plot.cubeIdx>0 ? '_'+plot.cubeIdx : '';
    return makeHipsUrl(`${plot.hipsUrlRoot}/Norder${nOrder}/Dir${dir}/Npix${tileNumber}${cubeExt}.${getHiPSTileExt(exts)}`, plot.proxyHips);
}

/**
 *
 * @param urlRoot
 * @param exts
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


export function getHiPSTileExt(exts) {
    if (!exts) return null;
    if (exts.includes('png')) return'png';
    else if (exts.includes('jpeg') || exts.includes('jpg')) return 'jpg';
    else return 'jpg';

}

/**
 *
 * @param plot
 * @param viewDim
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
    const pt1= cc.getImageCoords( makeWorldPt(0,0));
    const pt2= cc.getImageCoords( makeWorldPt(size,0));
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


function makeCorners(hpIdx, pixList, coordSys) {
    const spVec = new SpatialVector();
    return pixList.map( (ipix) => {
        const corners = hpIdx.corners_nest(ipix, 1);
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
function makeSimpleHpxCornerCache() {
    const tmpHealpixIdx3 = new HealpixIndex(8);
    tmpHealpixIdx3.init();
    const tmpHealpixIdx2 = new HealpixIndex(4);
    tmpHealpixIdx2.init();
    const npix = HealpixIndex.nside2Npix(8);
    const cachedCorners8= [];
    for (let ipix=0; ipix<npix; ipix++) {
        cachedCorners8.push(tmpHealpixIdx3.corners_nest(ipix, 1));
    }


    const level3pixelCnt = HealpixIndex.nside2Npix(8);
    const level2pixelCnt = HealpixIndex.nside2Npix(4);
    const cachedLevel3FullPixelList= [];
    const cachedLevel2FullPixelList= [];
    for (let ipix=0; ipix<level3pixelCnt; ipix++) cachedLevel3FullPixelList[ipix]= ipix;
    const j2000Leve3Corners= makeCorners(tmpHealpixIdx3, cachedLevel3FullPixelList, CoordinateSys.EQ_J2000);
    const galLevel3Corners= makeCorners(tmpHealpixIdx3, cachedLevel3FullPixelList, CoordinateSys.GALACTIC);


    for (let ipix=0; ipix<level2pixelCnt; ipix++) cachedLevel2FullPixelList[ipix]= ipix;
    const j2000Leve2Corners= makeCorners(tmpHealpixIdx2, cachedLevel2FullPixelList, CoordinateSys.EQ_J2000);
    const galLevel2Corners= makeCorners(tmpHealpixIdx2, cachedLevel2FullPixelList, CoordinateSys.GALACTIC);


    return {
        cornersNest(ipix,nside, healpixIdx) {
            return nside === 8 ? cachedCorners8[ipix] : healpixIdx.corners_nest(ipix, 1);
        },
        getFullLevel3CornerList(coordSys)  {
            switch (coordSys) {
                case CoordinateSys.EQ_J2000: return j2000Leve3Corners;
                case CoordinateSys.GALACTIC: return galLevel3Corners;
                default: return null;
            }
        },
        getFullLevel2CornerList(coordSys)  {
            switch (coordSys) {
                case CoordinateSys.EQ_J2000: return j2000Leve2Corners;
                case CoordinateSys.GALACTIC: return galLevel2Corners;
                default: return null;
            }
        }

    };

}

/**
 * @Function
 * @param {number} ipix
 * @param {number} nside
 * @param {HealpixIndex} healpixIdx
 */
let healpixCache;



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
    if (!healpixCache) healpixCache= makeSimpleHpxCornerCache();
    const nside = Math.pow(2, norder);
    const dataCenterWp= convert(centerWp, dataCoordSys);
    let hpxIdx;

         // ------------------------------
         // first, find the Healpix pixels for the field of view, if the fov is large just return all of them
         // ------------------------------
    let pixList;
    if (fov>80 && norder===3) { // this case if so common, don't recompute, use cache
        return filterAllSky(dataCenterWp, healpixCache.getFullLevel3CornerList(dataCoordSys));
    }
    else if (fov>80 && norder===2) { // this case if so common, don't recompute, use cache
        return filterAllSky(dataCenterWp, healpixCache.getFullLevel2CornerList(dataCoordSys));
    }
    else if (fov>80) { // with norder 1 or 2
        hpxIdx = new HealpixIndex(nside);
        hpxIdx.init();
        pixList= [];
        const npix = HealpixIndex.nside2Npix(nside);
        for (let ipix=0; ipix<npix; ipix++) pixList[ipix]= ipix;
    }
    else {
        hpxIdx = new HealpixIndex(nside);
        hpxIdx.init();
        const spatialVector = new SpatialVector();
        spatialVector.set(dataCenterWp.x, dataCenterWp.y);
        let radius = fov/2;
                          // we need to extend the radius (suggestion from Aladin)
        if (fov>60) radius *= 1.6;
        else if (fov>12) radius *=1.45;
        else radius *= 1.1;

        pixList = hpxIdx.queryDisc(spatialVector, radius*Math.PI/180.0, true, true);
    }

         // ------------------------------
         // second, find the 4 corners for every Healpix pixel
         // ------------------------------
    const spVec = new SpatialVector();
    let cells= pixList.map( (ipix) => {
        const corners = healpixCache.cornersNest(ipix, nside, hpxIdx);
        const wpCorners= corners.map( (c) => {
            spVec.setXYZ(c.x, c.y, c.z);
            return makeWorldPt(spVec.ra(), spVec.dec(), dataCoordSys);
        });
        return { ipix, wpCorners };
    });

    if (fov>80) {
        cells= filterAllSky(dataCenterWp, cells);
    }

    return cells;
}



function filterAllSky(centerWp, cells) {
    return cells.filter( (cell) =>{
        const {wpCorners}= cell;
        return (computeDistance(centerWp, wpCorners[0]) <90);
    });
}


export const hipsSURVEYS = [
    {
        url: 'http://alasky.u-strasbg.fr/2MASS/Color',
        label: '2MASS colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/DSS/DSSColor',
        label: 'DSS colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/DSS/DSS2Merged',
        label: 'DSS2 Red (F+R)'
    },
    {
        url: 'http://alasky.u-strasbg.fr/Fermi/Color',
        label: 'Fermi color'
    },
    {
        url: 'http://alasky.u-strasbg.fr/FinkbeinerHalpha',
        label: 'Halpha'
    },
    {
        url: 'http://alasky.u-strasbg.fr/GALEX/GR6-02-Color',
        label: 'GALEX Allsky Imaging Survey colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/IRISColor',
        label: 'IRIS colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/MellingerRGB',
        label: 'Mellinger colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/SDSS/DR9/color',
        label: 'SDSS9 colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/SpitzerI1I2I4color',
        label: 'IRAC color I1,I2,I4 - (GLIMPSE, SAGE, SAGE-SMC, SINGS)'
    },
    {
        url: 'http://alasky.u-strasbg.fr/VTSS/Ha',
        label: 'VTSS-Ha'
    },
    {
        url: 'http://saada.u-strasbg.fr/xmmallsky',
        label: 'XMM-Newton stacked EPIC images (no phot. normalization)',
    },
    {
        url: 'http://saada.u-strasbg.fr/xmmallsky/',
        label: 'XMM PN colored'
    },
    {
        url: 'http://alasky.u-strasbg.fr/AllWISE/RGB-W4-W2-W1/',
        label: 'AllWISE color'
    },
    {
        url: 'http://www.spitzer.caltech.edu/glimpse360/aladin/data',
        label: 'GLIMPSE360'
    }
];





export function getDefaultHiPSSurveys() {
    return hipsSURVEYS;
}
