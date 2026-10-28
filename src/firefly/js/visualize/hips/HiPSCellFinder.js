/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import { getHealpixCornerTool, getProGradeTilePixels, getVisibleHiPSCells, tileCoordsWrap} from '../HiPSUtil.js';
import {isHiPSAitoff} from '../WebPlot.js';
import {isQuadTileOnScreen} from '../iv/TileDrawHelper.jsx';
import {makeDevicePt} from '../Point';
import {CysConverter} from '../CsysConverter';

const imageOffsets= [[0,0],[0,1],[1,0],[1,1]]; // which half of the parent tile image, in units of the child tile size
const POLE_DEGREES= 10; // tiles with any corner within this many degrees of a healpix pole are subdivided more
const POLE_SUBDIVIDE_DEPTH= 3; // number of levels to subdivide a tile near the pole
const INNER_POLE_DEGREES= 2; // when zoomed beyond the tile norder, tiles this close to a pole are subdivided even more
const INNER_POLE_SUBDIVIDE_DEPTH= 4; // number of levels to subdivide a tile in the inner pole area when zoomed
const DEEP_ZOOM_OVER= 3; // when zoomed more than this many orders beyond the tile norder, use DEEP_INNER_POLE_SUBDIVIDE_DEPTH
const DEEP_INNER_POLE_SUBDIVIDE_DEPTH= 5; // number of levels to subdivide a tile in the inner pole area when deeply zoomed
const MIN_POLE_SPLIT_SIZE= 32; // device pixels, a tile smaller than this on the screen is not subdivided because of a pole

/**
 * @param {Array.<WorldPt>} wpCorners - corners in the HiPS data coordinate system
 * @param {number} zoomOver - how many orders the desired norder is beyond the tile norder, 0 if not zoomed beyond
 * @return {number} how many levels below the base tile a tile with these corners should be subdivided because of pole distortion
 */
function getPoleDepth(wpCorners, zoomOver) {
    const maxLat= Math.max(...wpCorners.map( (wp) => wp ? Math.abs(wp.y) : 0));
    if (zoomOver>0 && maxLat >= 90-INNER_POLE_DEGREES) {
        return zoomOver>DEEP_ZOOM_OVER ? DEEP_INNER_POLE_SUBDIVIDE_DEPTH : INNER_POLE_SUBDIVIDE_DEPTH;
    }
    if (maxLat >= 90-POLE_DEGREES) return POLE_SUBDIVIDE_DEPTH;
    return 0;
}

/**
 * @param {Array.<DevicePt>} devPtCorners
 * @return {number} the larger of the width or height of the bounding box of the corners
 */
function getDevSize(devPtCorners) {
    const pts= devPtCorners.filter(Boolean);
    if (!pts.length) return 0;
    const xs= pts.map( (pt) => pt.x);
    const ys= pts.map( (pt) => pt.y);
    return Math.max(Math.max(...xs)-Math.min(...xs), Math.max(...ys)-Math.min(...ys));
}

/**
 * @global
 * @public
 * @typedef {Object} HiPSDeviceTileData
 *
 * @prop {number} tileNumber - HiPS pixel number
 * @prop {number} norder - healpix order
 * @prop {Array.<DevicePt>} devPtCorners - the target corners of the tile in device coordinates
 * @prop {number} dx - x offset into image, for a subCell of a normal tile: 0 or 1, the half of the parent tile
 * @prop {number} dy - y offset into image, for a subCell of a normal tile: 0 or 1, the half of the parent tile
 * @prop {Array.<WorldPt>} [wpCorners] - the corners in the data coordinate system, only set on subCells
 * @prop {boolean} [incompleteCell] - some corners cannot be projected or the cell wraps, only the subCells are drawn
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
    const dataCoordSys= plot.dataCoordSys;
    const centerDevPt= cc.getDeviceCoords(centerWp);
    // this function is performance sensitive, use for loops instead of map and filter
    if (isHiPSAitoff(plot)){
        const aitoffAll= fov>200;
        for(let i= 0; (i<cells.length); i++) {
            cell= cells[i];
            const coordsWrap = tileCoordsWrap(cc, cell.wpCorners);
            devPtCorners= [];
            if (coordsWrap) {
                const subCells= computeIncompleteTiles(cc, dataCoordSys, norder, cell.ipix, viewDim, {checkWrap:true, allOnScreen:aitoffAll});
                if (subCells) {
                    retCells.push({devPtCorners, tileNumber:cell.ipix, dx:0, dy:0, norder, incompleteCell:true, subCells});
                }
            }
            else {
                for(let j=0; (j<cell.wpCorners.length); j++)  {
                    devPtCorners[j]= cc.getDeviceCoords(cell.wpCorners[j]);
                    if (!devPtCorners[j]) break;
                }
                if (aitoffAll || isQuadTileOnScreen(devPtCorners, viewDim)) {
                    const subCells= computeDeeperTiles(cc,dataCoordSys,norder,desiredNorder,cell.ipix,cell.wpCorners,devPtCorners);
                    retCells.push({devPtCorners, tileNumber:cell.ipix, dx:0, dy:0, norder, incompleteCell:false, subCells});
                }
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

            if (badCnt>2) {
                const subCells= computeIncompleteTiles(cc,dataCoordSys,norder,cells[i].ipix,viewDim, {centerDevPt});
                if (subCells) retCells.push({devPtCorners, tileNumber:cells[i].ipix, dx:0, dy:0, norder,subCells, incompleteCell:true});
            }
            else if (isQuadTileOnScreen(devPtCorners, viewDim)) {
                const subCells= computeDeeperTiles(cc,dataCoordSys,norder,desiredNorder,cells[i].ipix,cells[i].wpCorners,devPtCorners);
                retCells.push({devPtCorners, tileNumber:cells[i].ipix, dx:0, dy:0, norder,subCells});
            }
        }
    }
    return retCells;
}

/**
 * Compute the drawable parts of a cell that has some corners that cannot be projected (e.g. at the edge of the sky)
 * or, with checkWrap, that wraps around the edge of an aitoff projection.
 * Children that fully project are kept, incomplete children are replaced by their fully projected grandchildren.
 * Only parts that are on the screen are kept, unless allOnScreen is true.
 * Drawable parts near a pole are subdivided further, so they reach the same depth (POLE_SUBDIVIDE_DEPTH) below the
 * parent as a normal cell near the pole.
 * @param {CysConverter} cc
 * @param {CoordinateSys} dataCoordSys
 * @param {number} norder - norder of the parent tile
 * @param {number} npix - healpix pixel number of the parent tile
 * @param {{width:number,height:number}} viewDim
 * @param {Object} [options]
 * @param {boolean} [options.checkWrap] - treat a child that wraps as incomplete
 * @param {boolean} [options.allOnScreen] - skip the on screen check, everything is visible (aitoff full sky)
 * @param {DevicePt} [options.centerDevPt] - center of the view, if defined grandchild cells are shimmed
 * @return {Array.<HiPSDeviceTileData>|undefined} the drawable child cells, or undefined if none
 */
function computeIncompleteTiles(cc, dataCoordSys, norder, npix, viewDim, {checkWrap=false, allOnScreen=false, centerDevPt}={}) {
    const onScreen= (t) => !t.incompleteCell && (allOnScreen || isQuadTileOnScreen(t.devPtCorners, viewDim));
    const addPoleSubCells= (t, level) => {
        t.subCells= makeSubTilesToDepth(cc,dataCoordSys,t,{level});
    };
    const subTiles= getProgradeTiles(cc,dataCoordSys,norder,npix, {allowPartial:true, checkWrap});
    if (!subTiles) return;
    subTiles.forEach( (t) => {
        if (t.incompleteCell) {
            const grandChildCells= (getProgradeTiles(cc,dataCoordSys,norder+1,t.tileNumber,
                        {allowPartial:true, checkWrap, shim:Boolean(centerDevPt), centerDevPt, viewDim}) ?? [])
                .filter(onScreen);
            grandChildCells.forEach( (gc) => addPoleSubCells(gc, 2));
            t.subCells= grandChildCells.length>0 ? grandChildCells : undefined;
        }
    });
    const retSubTiles= subTiles.filter( (t) => t.incompleteCell ? t.subCells : onScreen(t));
    retSubTiles.forEach( (t) => {
        if (!t.incompleteCell) addPoleSubCells(t, 1);
    });
    return retSubTiles.length>0 ? retSubTiles : undefined;
}

/**
 * Compute the child cells of a tile so it can be drawn as smaller quads when zoomed in beyond the tile norder
 * or when the tile is near a pole, where the healpix cells are too distorted to draw accurately as 2 triangles.
 * For zoom, goes at most two levels deep: one level when desiredNorder is norder+1, otherwise two.
 * Near a pole, goes POLE_SUBDIVIDE_DEPTH levels deep, or INNER_POLE_SUBDIVIDE_DEPTH when zoomed and very close to the pole,
 * each child is evaluated by its own distance to the pole.
 * The dx/dy of each child is 0 or 1, the half of the parent tile image it comes from.
 * @param {CysConverter} cc
 * @param {CoordinateSys} dataCoordSys
 * @param {number} norder - norder of the parent tile
 * @param {number} desiredNorder
 * @param {number} npix - healpix pixel number of the parent tile
 * @param {Array.<WorldPt>} wpCorners - corners of the parent tile in the data coordinate system
 * @param {Array.<DevicePt>} devPtCorners - corners of the parent tile in device coordinates
 * @return {Array.<HiPSDeviceTileData>|undefined} the 4 child cells, or undefined if not needed or any child cannot be projected
 */
function computeDeeperTiles(cc, dataCoordSys, norder, desiredNorder, npix, wpCorners, devPtCorners) {
    const zoomOver= desiredNorder>norder ? desiredNorder-norder : 0;
    const zoomDepth= Math.min(zoomOver, 2);
    return makeSubTilesToDepth(cc, dataCoordSys, {norder, tileNumber:npix, wpCorners, devPtCorners}, {zoomDepth, zoomOver});
}

/**
 * Recursively make child cells until both the zoom depth and the pole depth of each cell are reached.
 * The pole depth is computed for each cell from its own corners, and a cell smaller than MIN_POLE_SPLIT_SIZE
 * on the screen is not subdivided because of a pole.
 * @param {CysConverter} cc
 * @param {CoordinateSys} dataCoordSys
 * @param {{norder:number, tileNumber:number, wpCorners:Array.<WorldPt>, devPtCorners:Array.<DevicePt>}} tile - the tile to subdivide
 * @param {Object} [options]
 * @param {number} [options.zoomDepth] - levels below the base tile to subdivide because of zoom
 * @param {number} [options.zoomOver] - how many orders the desired norder is beyond the base tile norder
 * @param {number} [options.level] - how many levels this tile is below the base tile
 * @return {Array.<HiPSDeviceTileData>|undefined}
 */
function makeSubTilesToDepth(cc, dataCoordSys, tile, {zoomDepth=0, zoomOver=0, level=0}={}) {
    const {norder, tileNumber, wpCorners, devPtCorners}= tile;
    const zoomSplit= level<zoomDepth;
    const poleSplit= level<getPoleDepth(wpCorners,zoomOver) && getDevSize(devPtCorners)>MIN_POLE_SPLIT_SIZE;
    if (!zoomSplit && !poleSplit) return;
    const subTiles= getProgradeTiles(cc,dataCoordSys,norder,tileNumber);
    if (!subTiles) return;
    subTiles.forEach( (t) => {
        t.subCells= makeSubTilesToDepth(cc,dataCoordSys,t,{zoomDepth,zoomOver,level:level+1});
    });
    return subTiles;
}

/**
 * Make the 4 child cells (at norder+1) of a healpix pixel, with device corners and image offsets.
 * @param {CysConverter} cc
 * @param {CoordinateSys} dataCoordSys
 * @param {number} norder - norder of the parent tile
 * @param {number} npix - healpix pixel number of the parent tile
 * @param {Object} [options]
 * @param {boolean} [options.allowPartial] - allow for an incomplete tile set
 * @param {boolean} [options.checkWrap] - a child that wraps (aitoff) gets no corners and is marked incomplete
 * @param {boolean} [options.shim] - if true, fill in 1 or 2 unprojectable corners of a child using the shim functions
 * @param {DevicePt} [options.centerDevPt] - center of the view, required when shim is true
 * @param {{width:number,height:number}} [options.viewDim] - required when shim is true
 * @return {Array.<HiPSDeviceTileData>|undefined} the 4 child cells, or undefined if any child cannot be projected
 *            and allowPartial is false. With allowPartial, a child with unprojectable corners has incompleteCell set.
 *            The devPtCorners of an incomplete child only has the projectable corners, so the positions do not match the healpix corners.
 */
function getProgradeTiles(cc, dataCoordSys, norder, npix,
                          {allowPartial=false, checkWrap=false, shim=false, centerDevPt, viewDim}={}) {
    const proNorder= norder+1;
    const proNside= 2**proNorder;
    const healpixCache=getHealpixCornerTool();
    const pgTiles= getProGradeTilePixels(npix)
        .map( (tileNumber,idx) => {
            const {wpCorners}= healpixCache.makeCornersForPix(tileNumber, proNside, dataCoordSys);
            const wraps= checkWrap && tileCoordsWrap(cc, wpCorners, 10);
            const devCorners= wraps ? [] : wpCorners.map( (wp) => cc.getDeviceCoords(wp));
            let devPtCorners= devCorners.filter(Boolean);
            if (shim && devPtCorners.length<4 && centerDevPt && viewDim) {
                if (devPtCorners.length===3) devPtCorners= shim1DevPtCorner(devCorners,centerDevPt, viewDim);
                else if (devPtCorners.length===2) devPtCorners= shim2DevPtCorner(devCorners,centerDevPt, viewDim);
            }

            if (!allowPartial && devPtCorners.length!==4) return;

            return {
                tileNumber,
                norder: proNorder,
                devPtCorners,
                wpCorners,
                incompleteCell: devPtCorners.length!==4,
                dx: imageOffsets[idx][0],
                dy: imageOffsets[idx][1],
            };
        });
    return pgTiles.every(Boolean) ? pgTiles : undefined; // a child is only undefined when allowPartial is false
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

