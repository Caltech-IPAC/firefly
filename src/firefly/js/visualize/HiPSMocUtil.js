import {getDrawLayersByType} from './PlotViewUtil.js';
import {getDlAry, dispatchCreateDrawLayer} from './DrawLayerCntlr.js';
import HiPSMOC from '../drawingLayers/HiPSMOC.js';
import {getHealpixCornerTool} from './HiPSUtil.js';
import {get, set, isEmpty} from 'lodash';

const MOC = '_moc';
let   mocCnt = 0;

export function makeMocTableId(ivoid) {
    const id = ivoStr(ivoid);

    return id.endsWith(MOC) ? id : id+MOC;
}

export function ivoStr(ivoid) {
    return ivoid ? ivoid.trim().replace('ivo://', '').replace(/\//g,'_') : 'moc_table_' + (++mocCnt);
}

export const NSIDE2 = new Array(30).fill(0).map((v, i) => 2**i);
export const NSIDE4 = new Array(20).fill(0).map((v, i) => 4**i);

export function getMocNuniq(order, npix) {
    return NSIDE4[order+1] + npix;
}

export function getMocOrderIndex(Nuniq) {
    const norder = Math.floor(Math.log2(Nuniq/4)/2);
    const npix = Nuniq - NSIDE4[norder + 1];

    return {norder, npix};
}

/**
 * add new layer on MOC table
 * @param {string} tbl_id moc table id
 * @param {string} fitsPath moc fits path
 * @param {string} mocUrl  moc fits url
 * @returns {T|SelectInfo|*|{}}
 */
export function addNewMocLayer(tbl_id, fitsPath, mocUrl) {
    const dls = getDrawLayersByType(getDlAry(), HiPSMOC.TYPE_ID);
    let   dl = dls.find((oneLayer) => oneLayer.drawLayerId === tbl_id);

    if (!dl) {
        const mocFitsInfo = {fitsPath, mocUrl, tbl_id};
        dl = dispatchCreateDrawLayer(HiPSMOC.TYPE_ID, {mocFitsInfo});
    }
    return dl;
}

const sideCells = [[0], [0, 1, 3, 2]];   // side cells from order 0

/**
 * tile at each side as the tile of order 0 is sub-divided into 4 tiles recursively up to order 'maxOrder'
 * @param {number} maxOrder  the max order to generate the side cells
 * @return {array} side cells from left bottom corner of order 0 to maxOrder, the return is like
 *                  [ [0]   // cell 0 at order 0
 *                    [0, 1, 3, 2]   // side cells at order 1, from the side cells of order 0
 *                    [0, .........] // side cells at order 2, from the side cells of order 1
 *                   ...]
 */
export function computeSideCellsToOrder(maxOrder) {
    const sideOffset = [[0, 1], [1, 3], [3, 2], [2, 0]];               // offset used to get sub-divided side cells of next order at each side
    const cornerOffset = [[2, 0, 1], [0, 1, 3], [1, 3, 2], [3, 2, 0]];  // offset used to get sub-divided corner cells of next order at each corner

    for (let i = sideCells.length; i <= maxOrder; i++) {
        const cornerInt = (2**(i-1)) - 1;

        // sub-divide side cells of previous order
        const cells = sideCells[i-1].reduce((prev, cellNum, idx) => {
                const atSide = Math.floor(idx/cornerInt);             // side #
                if (idx%cornerInt === 0) {                            // cell at the corner
                    const corners = cornerOffset[atSide].map((offset) => (cellNum*4+offset));   // sub-divided cells at the side

                    prev = [...prev,...corners];
                } else {                                              // side but corner cells
                    const sideCells = sideOffset[atSide].map((offset) => (cellNum*4 + offset));  // sub-divided cells at the side

                    prev = [...prev,...sideCells];
                }
                return prev;
        }, []);
        const newCells = [...cells.slice(1), cells[0]];

        sideCells.push(newCells);
    }

    return sideCells;
}


let sidePoints = {};
const sideCorners = [[2, 3], [3, 0], [0, 1], [1, 2]];

function setPoints(norder, npix, corners) {
    if (!get(sidePoints, norder)) {
        set(sidePoints, norder, {[npix]: corners});
    } else {
        set(sidePoints, [norder, npix], corners);
    }
}

/**
 * get the stored pixels along the sides of the tile
 * @param order
 * @param npix
 * @param wpCorners
 * @returns {*}
 */
export function getSidePointsNorder(order, npix, wpCorners) {
    let corners;
    let retVal = get(sidePoints, [order, npix]);

    if (isEmpty(retVal)) {
        if (wpCorners) {
            corners = [2, 3, 0, 1].map((t) => wpCorners[t]);
            setPoints(order, npix, corners);
        }
        retVal = corners;
    }

    return retVal;
}

/**
 * get 4 corners of the tile based on all pixels around its polygon representation
 * @param ptAry
 * @returns {*}
 */
export function getCornersFromSidePoints(ptAry) {
    if (ptAry.length > 4) {
        const sideLen = Math.trunc(ptAry.length / 4);
        const selCorners = [];

        for (let i = 0; i < ptAry.length; i += sideLen) {
            selCorners.push(ptAry[i]);
        }
        return selCorners;
    } else {
        return ptAry;
    }
}

export function initSidePoints(storedSidePoints) {
    sidePoints = storedSidePoints;
}


/**
 * get corners from healpix, some fixing on order 0, need more investigation later.
 * @param wpCorners  in original order
 * @param npix
 * @param healpixCache
 * @param coordsys
 * @returns {*}      in original order
 */
export function fixCornersOrderZero(wpCorners, npix, healpixCache, coordsys) {
    const base_npix = npix * 4;
    const selCorners = [2, 3, 0, 1];
    sideCells[1].forEach((ipx, idx) => {
        const pt = healpixCache.makeCornersForPix(base_npix + ipx, 2, coordsys);
        const cIdx = selCorners[idx];
        if (pt.wpCorners[cIdx].x !== wpCorners[cIdx].x ||
            pt.wpCorners[cIdx].y !== wpCorners[cIdx].y) {
            wpCorners[cIdx] = pt.wpCorners[cIdx];
        }
    });
    return wpCorners;
}

/**
 * return tile corner pixels from stored side points or the corners are either passed externally or calculated
 * internally. The returned corners is in original order.
 * Note: some tile of order 0 is fixed by side points of order 1, the calculation on the corners of
 * order 0 tile is to be investigated later.
 * @param norder
 * @param npix
 * @param coordsys
 * @param healpixCache
 * @param corners  original corner
 * @param bKeep
 * @returns {*}  original corner
 */
export function getCornerForPix(norder, npix, coordsys, healpixCache, corners, bKeep = true) {
    const ptAry = getSidePointsNorder(norder, npix, null);

    if (!isEmpty(ptAry)) {    // already exist
        const selCorners = getCornersFromSidePoints(ptAry);

        return {wpCorners: [2, 3, 0, 1].map((i) => selCorners[i])};
    }

    const {wpCorners} = corners ? {wpCorners: corners} : healpixCache.makeCornersForPix(npix, NSIDE2[norder], coordsys);

    // correct the corners for order 0 to coincide with those of higher order
    if (norder === 0) {
        fixCornersOrderZero(wpCorners, npix, healpixCache, coordsys);
    }

    if (bKeep) {
        getSidePointsNorder(norder, npix, wpCorners);
    }
    return {wpCorners};
}



//const upMap = new Array(30).fill(0).map ((i, idx) => Math.floor(idx/2) + Math.floor(idx/3));
/**
 * return the pixels around the MOC tile represented by polygon including at least 4 corners of the tile
 * more pixels between every two corners are produced for better rendering resolution
 * @param norder
 * @param npix
 * @param topOrder
 * @param coordsys
 * @param isAllSky
 * @returns {*}
 */
export function getMocSidePointsNuniq(norder, npix, topOrder, coordsys, isAllSky) {
    const sPoints = getSidePointsNorder(norder, npix);

    if (!sPoints) return null;
    const healpixCache = getHealpixCornerTool();
    const newSidePoints = sPoints.slice();
    const crtSidePointsOrder = Math.log2((newSidePoints.length / 4));
    const  dUp = isAllSky ? Math.min((topOrder - norder), 8) : Math.floor((topOrder - norder +1)/2);

    if (crtSidePointsOrder === dUp) {
        return newSidePoints;
    } else if (crtSidePointsOrder < dUp) {   // needs to insert more points by dUp-crtSidePointsOrder levels
        const sideCells = computeSideCellsToOrder(dUp);

        // repeatedly insert the corner points into current side points representation order by order up

        for (let i = crtSidePointsOrder + 1; i <= dUp; i++) {             // order difference from norder
            const nextOrder = norder + i;             // order of next side points representation
            const base_npix = npix * (NSIDE4[i]);
            const upCells = sideCells[i];
            const totalPtsOneSide = upCells.length / 4;
            let insertAt = 1;

            [0, 1, 2, 3].forEach((s) => {
                const firstIdx = s * totalPtsOneSide + 1;
                const lastIdx = firstIdx - 1 + totalPtsOneSide;
                for (let j = firstIdx; j <= lastIdx; j += 2) {
                    const k = j % upCells.length;
                    const nextNpix = base_npix + upCells[k];
                    const {wpCorners} = getCornerForPix(nextOrder, nextNpix, coordsys, healpixCache, null, false);

                    newSidePoints.splice(insertAt, 0, wpCorners[sideCorners[s][0]]);
                    insertAt += 2;
                }
            });
        }
        setPoints(norder, npix, newSidePoints);
        return newSidePoints;
    } else if (crtSidePointsOrder > dUp) {            // already insert enough points
        if (topOrder < norder) {
            return getCornersFromSidePoints(newSidePoints);   // original 4 corners
        } else {
            const levelDown = NSIDE2[crtSidePointsOrder - dUp];
            const newPoints = newSidePoints.reduce((prev, v, idx) => {
                if (idx%levelDown === 0) {
                    prev.push(v);
                }
                return prev;
            }, []);
            return newPoints;
        }
    }
}

export function isTileVisibleByPosition(wpCorners, cc) {
    const selCorners = wpCorners.length > 4 ? getCornersFromSidePoints(wpCorners) : wpCorners;
    const {width, height} = get(cc, 'viewDim') || {};
    if (!width || !height) return false;

    return selCorners.find((onePt) => {
        const devPt = cc.getDeviceCoords(onePt);
        return (devPt) ? ((devPt.x >= 0 && devPt.x <= width)&&(devPt.y >= 0 && devPt.y <= height)) : false;
    });
}

