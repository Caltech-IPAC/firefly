import {getAppOptions} from '../../api/ApiUtil.js';
import DrawObj from './DrawObj';
import {makeWorldPt} from '../Point.js';
import {isEmpty, has, set, isUndefined} from 'lodash';
import {makeRegionPolygon} from '../region/Region.js';
import {drawRegions} from '../region/RegionDrawer.js';
import {getMocOrderIndex, getMocSidePointsNuniq, getCornerForPix, getMocNuniq,
        isTileVisibleByPosition, initSidePoints, NSIDE4} from '../HiPSMocUtil.js';
import {getHealpixCornerTool,  getAllVisibleHiPSCells, getPointMaxSide, getHiPSNorderlevel} from '../HiPSUtil.js';
import DrawOp from './DrawOp.js';
import CsysConverter from '../CsysConverter.js';
import {Style, TextLocation,DEFAULT_FONT_SIZE} from './DrawingDef.js';
import {rateOpacity} from '../../util/Color.js';
import {distanceToPolygon} from '../VisUtil.js';
import {isHiPSAitoff} from 'firefly/visualize/WebPlot.js';
import {getFoV} from 'firefly/visualize/PlotViewUtil.js';

const MOC_OBJ= 'MOCObj';
const DEFAULT_STYLE= Style.STANDARD;
const DEF_WIDTH = 1;
const MAX_MAPORDER = 25;
const PTILE_OPACITY_RATIO = 0.5;
// const OUTLINE_OPACITY_RATIO = 0.8;
const OUTLINE_OPACITY_RATIO = 1;

/**
 * Draw one or more polygons defined in Multi-Order Coverage Map, MOC.
 * properties in DrawObj:
 *   drawObjAry: set of polygon drawObj
 *   regions:    set of polygon regions
 *   cellNums:   cell number list
 *
 * @param {Array} cellNums MOC cell number list
 * @param {Object} drawingDef
 * @param mocCsys
 * @return {object}
 */
function make(cellNums, drawingDef, mocCsys) {
    if (!cellNums || !cellNums.length) return null;

    const {style=DEFAULT_STYLE, color} = drawingDef || {};
    const obj = DrawObj.makeDrawObj();
    obj.type = MOC_OBJ;

    const mocGroup = MocGroup.make(cellNums, undefined, mocCsys);
    mocGroup.makeGroups();
    return {...obj, regionOptions: {message: 'polygon2'}, mocGroup, style, color};
}




////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////

const draw=  {

	usePathOptimization(drawObj) {
		return drawObj.lineWidth===1;
	},

	getCenterPt(drawObj) {
		const {drawObjAry}= drawObj;
        if (!drawObjAry) return makeWorldPt(xSum, ySum);
		let xSum = 0;
		let ySum = 0;
		let xTot = 0;
		let yTot = 0;

        drawObjAry.forEach( (obj) => {
            obj?.pts?.forEach((wp) => {
                xSum += wp.x;
                ySum += wp.y;
                xTot++;
                yTot++;
            });
        });
        return makeWorldPt(xSum / xTot, ySum / yTot);
	},

	getScreenDist(drawObj,plot, pt) {
		let minDist = Number.MAX_VALUE;
		const {drawObjAry} = drawObj || {};
        drawObjAry?.forEach( (dObj) => {
            const d = distanceToPolygon(dObj.pts, plot, pt);
            if (d < minDist) {
                minDist = d;
            }
        });
		return minDist;
	},

	draw(drawObj,ctx,plot,def,vpPtM,onlyAddToPath) {
        const drawParams= makeDrawParams(drawObj,def);
		drawMoc(drawObj, ctx, plot, drawParams, vpPtM, onlyAddToPath);
	},

	toRegion(drawObj, plot, def) {
		return toRegion(drawObj.drawObjAry, plot, makeDrawParams(drawObj,def),drawObj.renderOptions);
	},

	translateTo(drawObj,plot, apt) { }, //todo - maybe unnecessary
	rotateAround(drawObj, plot, angle, worldPt) { }//todo - maybe unnecessary
};

export default {make,draw, MOC_OBJ, PTILE_OPACITY_RATIO};

////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////


function makeDrawParams(drawObj,def) {
    const style= drawObj.style || def.style || Style.STANDARD;
    const lineWidth= drawObj.lineWidth || def.lineWidth || DEF_WIDTH;
    const textLoc= drawObj.textLoc || def.textLoc || TextLocation.DEFAULT;
    const fontName= drawObj.fontName || def.fontName || 'helvetica';
    const fontSize= drawObj.fontSize || def.fontSize || DEFAULT_FONT_SIZE;
    const fontWeight= drawObj.fontWeight || def.fontWeight || 'normal';
    const fontStyle= drawObj.fontStyle || def.fontStyle || 'normal';
    const rotationAngle = drawObj.rotationAngle||undefined;
    const color = drawObj.color || def.color || 'green';

    return { color, lineWidth, textLoc, style, fontName, fontSize, fontWeight, fontStyle, rotationAngle };
}


const PROD_VISIBLE_TOTAL = 200000*2200;
/**
 * class MocGroup convert moc nuniq values into a set of norder and npix and store all nuniq per norder
 */
export class MocGroup {
    constructor(cellList, mGroup = null, plot = {}, mocCsys) {
        if (mGroup) {
            this.cells = mGroup.cells;
            this.groupInLevels = mGroup.groupInLevels;
            this.minOrder = mGroup.minOrder;
            this.maxOrder = mGroup.maxOrder;
            this.mocCsys = mGroup.mocCsys;
        } else {
            this.cells = cellList;
            this.groupInLevels = {};
            this.minOrder = 100;
            this.maxOrder = 0;
            this.mocCsys= mocCsys;
        }
        this.healpixCache = getHealpixCornerTool();

        // for each collection of visible tiles
        this.initCollection(plot);
    }

    static make(cellList, plot, mocCsys) {
        return new MocGroup(cellList, null, plot, mocCsys);
    }

    static copy(mGroup, plot) {
        return mGroup && new MocGroup(undefined, mGroup, plot);
    }

    initCollection(plot) {
        this.displayOrder = 8; // default
        this.hipsOrder = 2; // default
        this.inCollectVisibleTiles = false;
        this.visibleMap = {};
        this.highestOrderInMap = 1; // set in getVisibleTileMap
        this.resultCellsFromMoc = [];
        this.vTiles = {};   // visible tiles after first step filter by looking at parent tiles of lower order
        this.TOTAL = 0;
        this.total = 0;     // count total collected visible tiles for rendering
        this.incNpixs = {};     // npix of the tile that is tested as visible previously in reduceTiles
        this.notIncNpixs = {};  // npix of the tile that is tested as invisible previously in reduceTiles
        this.nextOrderToCollect = this.minOrder;
        this.plot = plot;
    }

    makeGroups() {
        this.cells.forEach((oneNuniq) => {
            const {norder, npix} = getMocOrderIndex(oneNuniq);
            const newMember = {npix, nuniq: oneNuniq};

            if (norder > this.maxOrder) {
                this.maxOrder = norder;
            }
            if (norder < this.minOrder) {
                this.minOrder = norder;
            }
            if (has(this.groupInLevels, [norder])) {
                this.groupInLevels[norder].push(newMember);
            } else {
                this.groupInLevels[norder] = [newMember];
            }
        });
        this.isAllSky = (this.minOrder === 0) && (this.maxOrder === 0) && (this.cells.length > 0);
        this.nextOrderToCollect = this.minOrder;
        return this.groupInLevels;
    }

    // get all tiles at some order including the created tile containing children or not
    getTilesAtOrder(order) {
        return this.includeOrder(order) ? this.groupInLevels[order] : null;
    }

    includeOrder(order) {
        return (order >= this.minOrder && order <= this.maxOrder && has(this.groupInLevels, [order]));
    }

    getMaxDisplayOrder() {
        const MAX_DEPTH = getAppOptions()?.hips?.mocMaxDepth ?? 5;
        const {norder} = getHiPSNorderlevel(this.plot);

        this.hipsOrder = norder;
        this.displayOrder = Math.max(this.minOrder, this.hipsOrder) + MAX_DEPTH;
    }

    getVisibleTileMap() {          // init the visibleMap by finding visible tiles with the order less than displayOrder
        this.visibleMap = {};
        this.highestOrderInMap = 0;

        new Array(Math.min(this.displayOrder, MAX_MAPORDER)).fill(0).map((i, idx) => idx + 1)
            .find((d) => {
                if ((d > 1) && Object.keys(this.visibleMap[d - 1]).length > 5000) {
                    return true;
                }
                if (d>3 && getFoV(this.plot) > 200) return true;

                set(this.visibleMap, [d], getVisibleTilesAtOrderPerNpix(this.plot, d, this.mocCsys));
                this.highestOrderInMap = d;
                return false;
            });
    }

    // npix collected from visibleMap at order
    visibleNpixAt(order) {
        return has(this.visibleMap, [order]) ? Object.keys(this.visibleMap[order]) : [];
    }

    noVisibleTilesAtOrder(order) {
        return ((!has(this.vTiles, [order])) || (!this.vTiles[order]) || (this.vTiles[order].length === 0));
    }

    filterTilesOnOrder(tiles, fromOrder, pOrder, vSet) {
        const fNum = (fromOrder - pOrder);

        return tiles.filter((oneTile) => {
            const ipix = lowerNpix(oneTile.npix, fNum);
            if (has(vSet, [ipix])) {
                return oneTile;
            } else {
                return false;
            }
        });
    }

    // first step filter out invisible tiles by looking at low  order parent tile
    searchVisibleTiles(stopAtOrder) {
        let parentOrder = this.highestOrderInMap;   // the order best fit for the plot
        let len = this.visibleNpixAt(parentOrder).length;
        const maxOrder = stopAtOrder ? Math.min(stopAtOrder, this.maxOrder) : this.maxOrder;

        // find the order with small number of visible tiles from highestOrderInMap for current plot
        for (let order = parentOrder-1; order >= 1; order--) {
            if (has(this.visibleMap, [order]) && this.visibleNpixAt(order).length <= len) {
                parentOrder = order;
                len = this.visibleNpixAt(order).length;
                if (len <= 50 && (order <= Math.max(1, maxOrder - 6))) break;
            }
        }

        for (let d = this.minOrder; d <= maxOrder; d++) {
            const tiles = this.getTilesAtOrder(d);
            if (!tiles || tiles.length===0) {          // no tiles on this order
                set(this.vTiles, [d], tiles);
            } else if (d < parentOrder)  {  // tile with order less than parent order to reduced to (including 0)
                set(this.vTiles, [d], tiles);
            } else {                                   // filter out invisible tiles by looking at parent order
                set(this.vTiles, [d], this.filterTilesOnOrder(tiles, d, parentOrder, this.visibleMap[parentOrder]));
            }
        }
    }

    addToResult(norder, npix, nuniq, isParentTile) {
        const uniq = !nuniq ? getMocNuniq(norder, npix) : nuniq;

        this.resultCellsFromMoc.push({nuniq: uniq, norder, npix, isParentTile});
        this.total++;
    }

    isNotInsideMap(visibleSet, ipix) {
        return  {invisible: !has(visibleSet, ipix), wpCorners: visibleSet[ipix]} ;
    };

    addingCandidates(ipix, nOrder, wpCorners, nuniq, isParentTile = false){
        getCornerForPix(nOrder, ipix, this.mocCsys, this.healpixCache, wpCorners);
        this.addToResult(nOrder, ipix, nuniq, isParentTile);
        return true;
    }

    // select visible tile from the MOC at order 0 (visible map not include visible tiles at order 0)
    selectTilesOrderZero(tiles) {
        const cc = CsysConverter.make(this.plot);
        tiles.find((oneTile) => {
            const {npix, nuniq} = oneTile;
            const {wpCorners} = getCornerForPix(0, npix, this.mocCsys, this.healpixCache);

            if (isTileVisibleByPosition(wpCorners, cc)) {
                this.addToResult(0, npix,nuniq);
            }

            return this.total >= this.TOTAL;
        });
    }

    // find visible tile for the MOC by checking the visible map
    selectTilesByMap(fromOrder, tiles) {
        tiles = isUndefined(tiles) ? this.getTilesAtOrder(fromOrder) : tiles;
        if (!tiles || tiles.length === 0) return;

        const vSet = this.visibleMap?.[fromOrder] ?? {};

        tiles.find((oneTile) => {
            const {npix, nuniq} = oneTile;
            // new nextNpix
            const tileInfo = this.isNotInsideMap(vSet, npix);
            if (tileInfo.invisible) {  // tile is not visible
                return false;
            }

            this.addingCandidates(npix, fromOrder, tileInfo.wpCorners, nuniq);
            return this.total >= this.TOTAL;
        });
    }


    reduceTiles(fromOrder, toOrder)  {
        const tiles = this.vTiles[fromOrder];
        if (!tiles || tiles.length === 0) return;                      // no tiles at child order

        const pVisibleSet = this.visibleMap[toOrder];
        if (Object.keys(pVisibleSet).length === 0) return;             // no visible tiles at parent order

        const includedNpixs = this.incNpixs?.[toOrder] ?? [];
        const notIncludedNpixs = this.notIncNpixs?.[toOrder] ?? [];
        const pNum = (fromOrder - toOrder);   // divide by 4 ** (from-to)


        tiles.find((oneTile) => {
            const {npix} = oneTile;
            const nextNpix = lowerNpix(npix, pNum);           // npix of parent tile at 'toOrder'

            if (includedNpixs.includes(nextNpix) || notIncludedNpixs.includes(nextNpix)) {    // already tested
                return false;
            }

            const tileInfo = this.isNotInsideMap(pVisibleSet, nextNpix);
            if (tileInfo.invisible) {                // parent tile is not visible
                notIncludedNpixs.push(nextNpix);
                return false;
            }
            if (this.addingCandidates(nextNpix, toOrder, tileInfo.wpCorners, null, true)) {    // add tile to the return list
                includedNpixs.push(nextNpix);
            }
            return (this.total >= this.TOTAL);
        });


        set(this.incNpixs, [toOrder], includedNpixs);
        set(this.notIncNpixs, [toOrder], notIncludedNpixs);
    }

    canGetVisTiles(order, plot) {
        if (getFoV(plot) < 200) return true;
        return order<=3;
    }

    expandVisibleMap(toOrder) {
        if (has(this.visibleMap, toOrder)) {
            return true;
        }

        if (toOrder > Math.min(this.displayOrder, MAX_MAPORDER)) return false;

        const lastOrder = this.getHighestOrderFromMap(toOrder);

        for (let pOrder = lastOrder+1; pOrder <= toOrder; pOrder++) {
            const incNum = NSIDE4[1];   // npix increment
            const sNpix = this.visibleNpixAt(pOrder-1).length * incNum;   // predicted maximum number of npix (tile) for 'toOrder in visibleMap
            const nTiles = this.vTiles[toOrder].length*0.5*(1+1/NSIDE4[toOrder-pOrder]);     // predicted max visible MOC tile at 'pOrder'

            // exntend the visible list in case the total visible MOC tile and the predicted maximum number less than some criterion
            if (((sNpix * nTiles <= PROD_VISIBLE_TOTAL)&&(sNpix <= 200000)) || (sNpix < nTiles && sNpix < 10000)) {
                let nextSet;

                if ( (this.canGetVisTiles(pOrder, this.plot)) &&
                    (this.visibleMap?.[(pOrder - 1)] && this.visibleNpixAt(pOrder - 1).length < 5000)) {
                    nextSet = getVisibleTilesAtOrderPerNpix(this.plot, pOrder, this.mocCsys);        // for not too big list
                } else {                                                       // extended from list of lower order
                    const npixAry = new Array(incNum).fill(0).map((i, idx) => idx);
                    nextSet = Object.keys(this.visibleMap[pOrder - 1]).reduce((prev, oneNpix) => {
                        const base_npix = oneNpix * incNum;

                        npixAry.forEach((i) => {
                            set(prev, [(base_npix + i)], null);
                        });

                        return prev;
                    }, {});
                }
                set(this.visibleMap, [pOrder], nextSet);
            } else {
                break;
            }
            if (pOrder === toOrder) {
                return true;
            }
        }
        return false;
    }

    getHighestOrderFromMap(byOrder) {
        return Object.keys(this.visibleMap).reduce((prev, order) => {
            const norder = Number(order);
            const vSet = this.visibleNpixAt(norder);

            if (!isEmpty(vSet) && (norder > prev) && (norder < byOrder)) {
                prev = norder;
            }
            return prev;
        }, 0);
    }

    collectVisibleTilesFromMoc(plot, storedSidePoints, timeLimit=20) {
        initSidePoints(storedSidePoints);

        if (!this.inCollectVisibleTiles) {
            this.initCollection(plot);
            this.getMaxDisplayOrder();
            this.getVisibleTileMap();

            this.searchVisibleTiles();      // filter out invisible tiles first, update this.vTiles
            this.inCollectVisibleTiles = true;
            this.nextOrderToCollect = this.minOrder;
            this.TOTAL = Object.keys(this.vTiles).reduce((prev, order) => {
                    prev += (this.noVisibleTilesAtOrder(order) ? 0 : this.vTiles[order].length);
                    return prev;
            }, 0);

        } else {
            let nextOrder = this.nextOrderToCollect;
            const t1 = performance.now();

            for (let d = Math.max(this.nextOrderToCollect, this.minOrder); d <= this.displayOrder; d++) {
                if (this.noVisibleTilesAtOrder(d)) {
                    nextOrder = d+1;
                    continue;
                }

                if (d === 0) {
                    this.selectTilesOrderZero(this.vTiles[0]);
                    nextOrder = d+1;
                } else {
                    const isExpanded = this.expandVisibleMap(d);

                    if (isExpanded) {
                        this.selectTilesByMap(d, this.vTiles[d]);
                        nextOrder = d+1;
                    } else {
                        nextOrder = d;
                        this.displayOrder = nextOrder;
                        break;
                    }
                }
                if (timeLimit) {
                    if (((performance.now() - t1) > timeLimit) || d === this.displayOrder) {
                        this.nextOrderToCollect = nextOrder;
                        return;
                    }
                }

                if (this.total >= this.TOTAL) break;
            }

            if ((this.total < this.TOTAL) && (nextOrder <= this.maxOrder) ) {
                const parentOrder = this.getHighestOrderFromMap(nextOrder);
                for (let rd = nextOrder; rd <= this.maxOrder; rd++) {
                    if (this.noVisibleTilesAtOrder(rd)) continue;
                    this.reduceTiles(rd, parentOrder);   // reduce tiles of higher order to parent tiles of lower order

                    if (timeLimit && ((performance.now() - t1) > timeLimit)) {
                        this.nextOrderToCollect= rd+1;
                        return;
                    }
                }
            }

            this.inCollectVisibleTiles = false;
        }
    }

    isInCollection() {
        return this.inCollectVisibleTiles;
    }

}


function drawMoc(mocObj, ctx, cc, drawParams, vpPtM,onlyAddToPath) {
    mocObj?.drawObjAry?.forEach((dObj) => {
        DrawOp.draw(dObj, ctx, cc, drawParams, vpPtM, onlyAddToPath);
    });
}


// create one drawObj for one tile
function createOneDrawObjInMoc(nuniq, norder, npix, displayOrder, hipsOrder, mocCoordsys, regionOptions, isAllSky, isParentTile) {
    const polyPts = getMocSidePointsNuniq(norder, npix, hipsOrder+6, mocCoordsys, isAllSky);
    if (!polyPts)  return null;

    const polyRegion = makeRegionPolygon(polyPts, regionOptions);
    const drawObj = drawRegions([polyRegion])[0];
    const mocInfo = {norder, displayOrder, hipsOrder, npix, nuniq, isParentTile};
    return {...drawObj, color: undefined, mocInfo};
}

// create all drawObjs
export function createDrawObjsInMoc(mocObj, plot, mocCsys, startIdx, endIdx, storedSidePoints) {
    initSidePoints(storedSidePoints);
    const {displayOrder, regionOptions={}, allCells, hipsOrder, mocGroup, style=Style.STANDARD, color} = mocObj;

    startIdx = (startIdx >= 0 && startIdx < allCells.length) ? startIdx : 0;
    endIdx = (endIdx && endIdx < allCells.length) ? endIdx : allCells.length-1;

    if (startIdx === 0) {
        mocObj.drawObjAry = [];
    }

    const drawObjs = allCells.slice(startIdx, endIdx+1).reduce((prev, oneCell) => {
        const {norder, npix, nuniq, isParentTile} = oneCell;
        const drawObj = createOneDrawObjInMoc(nuniq, norder, npix, displayOrder, hipsOrder,  mocCsys,
                                              regionOptions, mocGroup.isAllSky, isParentTile);

        if (drawObj) {
            drawObj.style = style;
            if (isParentTile) {       // this fill color is specifically for the round-up tile
                const opacity= drawObj.style===Style.FILL ? PTILE_OPACITY_RATIO : OUTLINE_OPACITY_RATIO;
                drawObj.fillColor = rateOpacity(color, opacity);
            }
            prev.push(drawObj);
        }
        return prev;
    }, []);

    return drawObjs;
}



function getVisibleTilesAtOrderPerNpix(plot, order, mocCsys) {
    const {centerWp, fov} = getPointMaxSide(plot, plot.viewDim);

    return getAllVisibleHiPSCells(order, centerWp, fov, mocCsys, isHiPSAitoff(plot))
            .reduce((npix_set, oneTile) => {
                set(npix_set, [oneTile.ipix], oneTile.wpCorners);
                return npix_set;
            }, {});
}

export function setMocDisplayOrder(mocObj) {
    const {resultCellsFromMoc: allCells, displayOrder, hipsOrder} = mocObj.mocGroup || {};
    return Object.assign(mocObj, {allCells, displayOrder, hipsOrder});
}

function toRegion(drawObjAry, plot, drawParams) {
	const resRegions = [];

	return drawObjAry.reduce( (prev, dObj) => {
		const regList = DrawOp.toRegion(dObj, plot, drawParams);

		if (!isEmpty(regList)) {
			prev.push(...regList);
		}

		return prev;
	}, resRegions);
}

const outputTime = (msg, t0) => {
    const t1 = performance.now();
    console.log(msg + ' took ' + (t1-t0) + ' msec');
};

const lowerNpix = (npix, level) => Math.trunc(npix/NSIDE4[level]);