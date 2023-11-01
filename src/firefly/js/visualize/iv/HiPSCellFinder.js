/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getCornersForCell, getVisibleHiPSCells, tileCoordsWrap} from 'firefly/visualize/HiPSUtil.js';
import {isHiPSAitoff} from 'firefly/visualize/WebPlot.js';
import {isQuadTileOnScreen} from 'firefly/visualize/iv/TileDrawHelper.jsx';
import {makeDevicePt} from 'firefly/visualize/Point.js';
import {CysConverter} from 'firefly/api/ApiUtilImage.jsx';

/**
 * @global
 * @public
 * @typedef {Object} HiPSDeviceTileData
 *
 * @prop {number} tileNumber - HiPS pixel number
 * @prop {number} nside - healpix level
 * @prop {Array.<DevicePt>} devPtCorners - the target corners of the tile in device coordinates
 * @prop {number} dx - x offset into image
 * @prop {number} dy - y offset into image
 * @prop {boolean} [coordsWrap]
 * @prop {Array.<HiPSDeviceTileData>} [subCells]
 */



/**
 * @param {WebPlot} plot
 * @param viewDim
 * @param {number} norder
 * @param {number} fov
 * @param {WorldPt} centerWp
 * @param {number} desiredNorder
 * @return {Array.<HiPSDeviceTileData>}
 */
export function findCellOnScreen(plot, viewDim, norder, fov,centerWp, desiredNorder) {
    const cells= getVisibleHiPSCells(norder,desiredNorder??norder,centerWp, fov, plot.viewDim, plot.dataCoordSys, isHiPSAitoff(plot));
    const cc= CysConverter.make(plot);

    const retCells= [];
    let badCnt;
    let cell;
    let devPtCorners;
    const centerDevPt= cc.getDeviceCoords(centerWp);
    // this function is performance sensitive, use for loops instead of map and filter
    if (isHiPSAitoff(plot)){
        const aitoffAll= fov>200;
        for(let i= 0; (i<cells.length); i++) {
            cell= tileCoordsWrap(cc, cells[i].wpCorners) ? makeWrappingCell(cells[i],norder,plot.dataCoordSys, cc) : cells[i];
            devPtCorners= [];
            if (!cell.coordsWrap || !aitoffAll ) {
                for(let j=0; (j<cell.wpCorners.length); j++)  {
                    devPtCorners[j]= cc.getDeviceCoords(cell.wpCorners[j]);
                    if (!devPtCorners[j]) break;
                }
            }
            if (aitoffAll || isQuadTileOnScreen(devPtCorners, viewDim)) {
                retCells.push({devPtCorners, tileNumber:cell.ipix, dx:0, dy:0, nside: norder,
                    coordsWrap:Boolean(cell.coordsWrap), subCells: cell.subCells});
            }
        }
    }
    else {
        for(let i= 0; (i<cells.length); i++) {
            devPtCorners= [];
            badCnt=0;
            for(let j=0; (j<cells[i].wpCorners.length); j++)  {
                devPtCorners[j]= cc.getDeviceCoords(cells[i].wpCorners[j]);
                if (!devPtCorners[j]) {
                    badCnt++;
                    if (badCnt>2) break;
                }
            }
            if (badCnt===1) devPtCorners= shim1DevPtCorner(devPtCorners,centerDevPt, viewDim);
            else if (badCnt===2) devPtCorners= shim2DevPtCorner(devPtCorners,centerDevPt, viewDim);
            if (isQuadTileOnScreen(devPtCorners, viewDim)) {
                retCells.push({devPtCorners, tileNumber:cells[i].ipix, dx:0, dy:0, nside: norder});
            }
        }
    }
    return retCells;
}

/**
 *
 * @param wc
 * @param {number} norder
 * @param {CoordinateSys} dataCoordSys
 * @param {CysConverter} cc
 * @prop {HiPSDeviceTileData}
 */
function makeWrappingCell(wc, norder, dataCoordSys, cc) {
    if (norder>2) return {...wc, coordsWrap:true};
    const nside= norder+1;
    const subCells= Array(4).fill(nside,0,4).map( (sc,idx) => {
                const tileNumber = wc.ipix * 4 + idx;
                const wpCorners= getCornersForCell(nside, tileNumber, dataCoordSys);
                return  {
                    nside, tileNumber, wpCorners, dx: 0, dy: 0,
                    devPtCorners: wpCorners.map( (wp) => cc.getDeviceCoords(wp)),
                };
            }
        )
        .filter( (sc) => !tileCoordsWrap(cc, sc.wpCorners, 10));
    return { ...wc, coordsWrap: true, subCells };
}



function shim1DevPtCorner(devPtCorners,centerDevPt, viewDim) {
    const {width,height}= viewDim;
    const cX= centerDevPt.x;
    const avgY= devPtCorners.reduce( (sum, pt) => pt ? sum+pt.y : sum,0)/(devPtCorners.length-1);
    const maxX= devPtCorners.reduce( (max, pt) => pt ? Math.abs(cX-pt.x) > Math.abs(cX-max) ? pt.x : max : max ,cX);
    let y= avgY;
    if (y<50) y=1;
    else if (y>height-50) y=height;
    let x= maxX;
    if (x<50) x=1;
    else if (x>width-50) x=width;
    return devPtCorners.map( (pt) => pt ? pt : makeDevicePt(x,y));
}

function shim2DevPtCorner(devPtCorners,centerDevPt, viewDim) {
    const {width,height}= viewDim;
    const cY= centerDevPt.y;

    const avgX= devPtCorners.reduce( (sum, pt) => pt ? sum+pt.x : sum,0)/(devPtCorners.length-2);
    const maxY= devPtCorners.reduce( (max, pt) => pt ? Math.abs(cY-pt.y) > Math.abs(cY-max) ? pt.y : max : max ,cY);

    let y= maxY;
    if (y<50) y=1;
    else if (y>height-50) y=height;
    let xToUse= avgX-10;
    return devPtCorners.map( (pt) => {
        if (pt) return pt;
        let x= xToUse;
        if (x<50) x=1;
        else if (x>width-50) x=width;
        const retPt= makeDevicePt(xToUse,y);
        xToUse+=20;
        return retPt;
    });
}

