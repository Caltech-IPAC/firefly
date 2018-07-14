import DrawObj from './DrawObj';
import {makeWorldPt} from '../Point.js';
import {isEmpty, has, set, get, isUndefined} from 'lodash';
import {makeRegionPolygon} from '../region/Region.js';
import {drawRegions} from '../region/RegionDrawer.js';
import {distanceToPolygon} from './ShapeDataObj.js';
import {getMocOrderIndex, getMocSidePointsNuniq, getCornerForPix, getMocNuniq,
        isTileVisibleByPosition, initSidePoints, NSIDE2, NSIDE4, fixCornersOrderZero} from '../HiPSMocUtil.js';
import {getHealpixCornerTool,  getVisibleHiPSCells, getPointMaxSide, getHiPSNorderlevel} from '../HiPSUtil.js';
import DrawOp from './DrawOp.js';
import CsysConverter from '../CsysConverter.js';
import {primePlot} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {Style, TextLocation,DEFAULT_FONT_SIZE} from './DrawingDef.js';

const MOC_OBJ= 'MOCObj';
const DEFAULT_STYLE= Style.STANDARD;
const DEF_WIDTH = 1;

/**
 * Draw one or more polygons defined in Multi-Order Coverage Map, MOC.
 * properties in DrawObj:
 *   drawObjAry: set of polygon drawObj
 *   regions:    set of polygon regions
 *   cellNums:   cell number list
 *
 * @param {Array} cellNums MOC cell number list
 * @param {Object} drawingDef
 * @return {object}
 */
function make(cellNums, drawingDef) {
    if (!cellNums && !cellNums.length) return null;

    const {style=DEFAULT_STYLE} = drawingDef || {};
    const obj = DrawObj.makeDrawObj();
    obj.type = MOC_OBJ;

    const mocGroup = new MocGroup(cellNums);
    mocGroup.makeGroups();
    Object.assign(obj, {regionOptions: {message: 'polygon2'}, mocGroup, style});

    return obj;
}

/**
 * class MocGroup convert moc nuniq values into a set of norder and npix and store all nuniq per norder
 */
class MocGroup {
    constructor(cellList) {
        this.cells = cellList;
        this.groupInLevels = {};
        this.minOrder = 100;
        this.maxOrder = 0;
        this.currentLastOrder = 0;
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
        this.currentLastOrder = this.maxOrder;
        this.isAllSky = (this.minOrder === 0) && (this.maxOrder === 0) && (this.cells.length === 12);
        return this.groupInLevels;
    }

    // total number of tiles represented by MoCGroup object
    countTiles() {
        return this.cells.length;
    }


    // get all tiles at some order includeing the created tile containing children or not
    getTilesAtOrder(order, includeChild = false) {
        if (this.includeOrder(order)) {
            if (includeChild) return this.groupInLevels[order];

            return this.groupInLevels[order].filter((oneTile) => !oneTile.from);
        } else {
            return null;
        }
    }

    includeOrder(order) {
        return (order >= this.minOrder && order <= this.maxOrder && has(this.groupInLevels, [order]));
    }
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
		var {drawObjAry}= drawObj;
		var xSum = 0;
		var ySum = 0;
		var xTot = 0;
		var yTot = 0;

		if (drawObjAry) {
			drawObjAry.forEach( (obj) => {
				if (obj && obj.pts) {
					obj.pts.forEach((wp) => {
						xSum += wp.x;
						ySum += wp.y;
						xTot++;
						yTot++;
					});
				}
			});
			return makeWorldPt(xSum / xTot, ySum / yTot);
		} else {
			return makeWorldPt(xSum, ySum);
		}
	},

	getScreenDist(drawObj,plot, pt) {
		let minDist = Number.MAX_VALUE;

		const {drawObjAry} = drawObj || {};

		if (drawObjAry) {
			drawObjAry.forEach( (dObj) => {
				const d = distanceToPolygon(dObj, plot, pt);
				if (d < minDist) {
					minDist = d;
				}
			});
		}
		return minDist;
	},

	draw(drawObj,ctx,plot,def,vpPtM,onlyAddToPath) {
        const drawParams= makeDrawParams(drawObj,def);
		drawMoc(drawObj, ctx, plot, drawParams, vpPtM, onlyAddToPath);
	},

	toRegion(drawObj, plot, def) {
		return toRegion(drawObj.drawObjAry, plot, makeDrawParams(drawObj,def),drawObj.renderOptions);
	},

	translateTo(drawObj,plot, apt) {
        return;   //todo
	},

	rotateAround(drawObj, plot, angle, worldPt) {
        return; // todo
	}
};

export default {make,draw, MOC_OBJ};

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

    return {
        color,
        lineWidth,
        textLoc,
        style,
        fontName,
        fontSize,
        fontWeight,
        fontStyle,
        rotationAngle
    };
}

/**
 * collect all visible tiles from defined MOC based on plot and a selected maximum display number
 * the tile collection is for moc display on the given plot
 * the maximum display number is derived based on the plot features, such as pixel size, etc.
 * @param mocGroup
 * @param plot
 * @param displayOrder   maximum norder of the tile to be collected
 * @returns {{}}
 */
function collectVisibleTilesFromMoc(mocGroup, plot, displayOrder) {
    //const TOTAL = Math.min(Math.max(Math.trunc(mocGroup.countTiles() * 0.2), 5000), 7000);
    const TOTAL = mocGroup.countTiles();
    const healpixCache = getHealpixCornerTool();
    const {visibleMap, highestOrderInMap} = getVisibleTiles(displayOrder, plot);  // search visible tiles of current plot at lower orders
    const cc = CsysConverter.make(plot);
    const resTiles = {};

    // check if npix is included in a array of npix from visible tiles of some order
    const isNotInsideMap = (visibleSet, ipix) => {
        return  {invisible: !has(visibleSet, ipix), wpCorners: visibleSet[ipix]} ;
    };

    // add tile info including norder, npix and nuniq numbers into the returned visible tiles set
    const addingCandidates = (ipix, nOrder, wpCorners, nuniq) => {
        getCornerForPix(nOrder, ipix, plot.dataCoordSys, healpixCache, wpCorners);
        addToResult(nOrder, ipix, nuniq);
        return true;
    };

    let total = 0;           // total number in the return array
    const addToResult = (norder, npix, nuniq) => {
        const uniq = !nuniq ? getMocNuniq(norder, npix) : nuniq;

        resTiles[uniq] = {norder, npix};
        total++;
    };

    // npix array collected from visibleMap at some order
    const visibleNpixAt = (order) => {
        return has(visibleMap, [order]) ? Object.keys(visibleMap[order]) : [];
    };

    const incNpixs = {};     // store npix of the tile that is tested visible previously
    const notIncNpixs = {};  // store npix of the tile that is tested invisible previously

    // find visible tiles for the MOC by checking the visibility of the parent tiles at lower order
    const reduceTiles = (fromOrder, toOrder, visibleNpixsToOrder, tiles) => {
        tiles = isUndefined(tiles) ?  mocGroup.getTilesAtOrder(fromOrder, false) : tiles;

        if (!tiles || tiles.length === 0) return;                        // no tiles at child order

        const pVisibleSet = visibleNpixsToOrder;
        if (Object.keys(pVisibleSet).length === 0) return;     // no visible tiles at parent order

        const includedNpixs = get(incNpixs, [toOrder], []);
        const notIncludedNpixs = get(notIncNpixs, [toOrder], []);
        const pNum = (fromOrder - toOrder) << 1;    // divide by 4 ** (from-to)


        tiles.find((oneTile) => {
            const {npix} = oneTile;
            const nextNpix = npix >> pNum;           // npix of parent tile at 'toOrder'

            if (includedNpixs.includes(nextNpix) || notIncludedNpixs.includes(nextNpix)) {    // already tested
                return false;
            }

            const tileInfo = isNotInsideMap(pVisibleSet, nextNpix);
            if (tileInfo.invisible) {                // parent tile is not visible
                notIncludedNpixs.push(nextNpix);
                return false;
            }
            if (addingCandidates(nextNpix, toOrder, tileInfo.wpCorners)) {    // add tile to the return list
                includedNpixs.push(nextNpix);
            }
            return (total >= TOTAL);
        });


        set(incNpixs, [toOrder], includedNpixs);
        set(notIncNpixs, [toOrder], notIncludedNpixs);
    };

    // find visible tile for the MOC by checking the visible map
    const selectTilesByMap = (fromOrder, tiles) => {
        tiles = isUndefined(tiles) ? mocGroup.getTilesAtOrder(fromOrder, false) : tiles;
        if (!tiles || tiles.length === 0) return;

        const vSet = get(visibleMap, [fromOrder], {});

        tiles.find((oneTile) => {
            const {npix, nuniq} = oneTile;
            // new nextNpix
            const tileInfo = isNotInsideMap(vSet, npix);
            if (tileInfo.invisible) {  // tile is not visible
                return false;
            }

            addingCandidates(npix, fromOrder, tileInfo.wpCorners, nuniq);
            return total >= TOTAL;
        });
    };

    // select visible tile from the MOC at order 0 (visible map not include visible tiles at order 0)
    const selectTilesOrderZero = (tiles) => {
        tiles.find((oneTile) => {
            const {npix, nuniq} = oneTile;
            const {wpCorners} =  healpixCache.makeCornersForPix(npix, NSIDE2[0], plot.dataCoordSys);  // get corners
            fixCornersOrderZero(wpCorners, npix, healpixCache, plot.dataCoordSys);

            if (isTileVisibleByPosition(wpCorners, cc)) {
                getCornerForPix(0, npix, plot.dataCoordSys, healpixCache, wpCorners);
                addToResult(0, npix,nuniq);
            }

            return total >= TOTAL;
        });
    };

    // add visible tiles to visibleMap for order 'toOrder'
    const PROD_VISIBLE_TOTAL = 200000*2000;
    const expandVisibleMap = (toOrder, plot, vTiles)  => {
        if (has(visibleMap, toOrder)) {
            return true;
        }

        if (toOrder > displayOrder) return false;

        const lastOrder = getHighestOrderFrom(visibleMap, toOrder);

        for (let pOrder = lastOrder+1; pOrder <= toOrder; pOrder++) {
            const incNum = NSIDE4[pOrder - lastOrder];   // npix increment
            const sNpix = visibleNpixAt(pOrder-1).length * incNum;   // predicted maximum number of npix (tile) for 'toOrder in visibleMap
            const nTiles = vTiles.length*0.5*(1+1/NSIDE4[toOrder-pOrder]);     // predicted max visible MOC tile at 'pOrder'

        // exntend the visible list in case the total visible MOC tile and the predicted maximum number less than some criterion
            if (((sNpix * nTiles <= PROD_VISIBLE_TOTAL)&&(sNpix <= 200000)) || (sNpix < nTiles && sNpix < 10000)) {
                let nextSet;

                if (get(visibleMap, [(pOrder - 1)]) && visibleNpixAt(pOrder - 1).length < 5000) {
                    nextSet = getVisibleTilesAtOrderPerNpix(plot, toOrder);        // for not too big list
                } else {                                                       // extended from list of lower order

                    const npixAry = new Array(incNum).fill(0).map((i, idx) => idx);
                    nextSet = Object.keys(visibleMap[pOrder - 1]).reduce((prev, oneNpix) => {
                        const base_npix = oneNpix * incNum;

                        npixAry.forEach((i) => {
                            set(prev, [(base_npix + i)], null);
                        });

                        return prev;
                    }, {});
                }
                set(visibleMap, [pOrder], nextSet);
            } else {
                break;
            }
            if (pOrder === toOrder) {
                return true;
            }
        }
        return false;
    };

    /*
     get the highest order less than the given 'byOrder' in the visibleMap with tile corners data ( the visible list is
     originally formed by  calling getVisibleHiPSCells, not extended from that of lower level)
    */
    const getHighestOrderFrom = (vMap, byOrder) => {
        return Object.keys(vMap).reduce((prev, order) => {
            const norder = Number(order);
            const vSet = visibleNpixAt(norder);

            //if (!isEmpty(vSet) && get(visibleMap, [norder, vSet[0]])  && (norder > prev) && (norder < byOrder)) {
            if (!isEmpty(vSet) && (norder > prev) && (norder < byOrder)) {
                prev = norder;
            }
            return prev;
        }, 0);
    };


    initSidePoints();

    const filterTilesOnOrder = (tiles, fromOrder, pOrder, vSet) => {
        const fNum = (fromOrder - pOrder) * 2;

        return tiles.filter((oneTile) => {
            const ipix = oneTile.npix >> fNum;
            if (has(vSet, [ipix])) {
                return oneTile;
            } else {
                return false;
            }
        });
    };

    /*
     find visible tiles of the MOC based on the visible parents at lower parents for collecting the
     visible tiles efficiently
     */

    const searchVisibleTiles = (stopAt) => {
        let parentOrder = highestOrderInMap;   // the order best fit for the plot
        let len = visibleNpixAt(parentOrder).length;
        const maxOrder = stopAt ? Math.min(stopAt, mocGroup.maxOrder) : mocGroup.maxOrder;

        // find the order with small number of visible tiles for current plot
        for (let order = parentOrder; order >= 1; order--) {
            if (has(visibleMap, [order]) && visibleNpixAt(order).length <= len) {
                parentOrder = order;
                len = visibleNpixAt(order).length;
                if (len <= 50 && (order <= Math.max(1, maxOrder - 6))) break;
            }
        }

        const vTiles = {};
        const vSet = visibleMap[parentOrder];

        for (let d = mocGroup.minOrder; d <= maxOrder; d++) {
            const tiles = mocGroup.getTilesAtOrder(d);
            if (!tiles || tiles.length===0) {
                set(vTiles, [d], tiles);
            } else if (d < parentOrder || d === 0)  {
                set(vTiles, [d], tiles);
            } else {
                set(vTiles, [d], filterTilesOnOrder(tiles, d, parentOrder, vSet));
            }
        }
        return vTiles;
    };


    // initial step to filter out all invisible tiles by looking at the parent tiles at lower order
    const vTiles = searchVisibleTiles() ;
    const noTilesAtOrder = (order) => {
        return ((!has(vTiles, [order])) || (!vTiles[order]) || (vTiles[order].length === 0));
    };

    const visibleCount = () => {
        const tileCount = Object.keys(vTiles).reduce((prev, order) => {
            set(prev, [order], (noTilesAtOrder(order) ? 0 : vTiles[order].length));

            return prev;
        }, {});

        const vCount = Object.keys(visibleMap).reduce((prev, order) => {
            set(prev, [order], (has(visibleMap, [order])&&visibleMap[order] ? visibleNpixAt(order).length : 0));
            return prev;
        }, {});

        return {tileCount, vCount};
    };

    /*
     second step to find visible tiles by checking the visibleMap and convert all tiles of higher order to parent tiles
     in case no list for selection  is not available in visibleMap
    */

    let firstAt = mocGroup.maxOrder+1;
    for (let d = mocGroup.minOrder; d <= displayOrder; d++) {
        if (noTilesAtOrder(d)) continue;

        if (d === 0) {
            selectTilesOrderZero(vTiles[0]);
        } else {
            const isExpanded = expandVisibleMap(d, plot, vTiles[d]);

            if (isExpanded) {
                selectTilesByMap(d, vTiles[d]);
                firstAt = d+1;
            } else {
                firstAt = d;
                break;
            }
        }
        if (total >= TOTAL) break;
    }

    if ((total < TOTAL) && (firstAt <= mocGroup.maxOrder) ) {
        const parentOrder = getHighestOrderFrom(visibleMap, firstAt);
        for (let rd = firstAt; rd <= mocGroup.maxOrder; rd++) {
            if (noTilesAtOrder(rd)) continue;
            reduceTiles(rd, parentOrder,  get(visibleMap, [parentOrder], {}), vTiles[rd]);   // reduce tiles of higher order to parent tiles of lower order
        }
    }

    return resTiles;
}


function drawMoc(mocObj, ctx, cc, drawParams, vpPtM,onlyAddToPath) {
    const plot = primePlot(visRoot(), cc.plotId);
    const healpixCache = getHealpixCornerTool();
    const {displayOrder, regionOptions={}, allCells, drawObjAry} = mocObj;

    if (drawObjAry) {
        drawObjAry && drawObjAry.forEach((dObj) => {
            DrawOp.draw(dObj, ctx, cc, drawParams, vpPtM, onlyAddToPath);
        });
    } else {
        Object.keys(allCells).forEach((nuniq) => {
            const {norder, npix} = allCells[nuniq];
            const drawObj = createOneDrawObjInMoc(nuniq, norder, npix, displayOrder, plot.dataCoordSys,
                healpixCache, regionOptions);

            drawObj && DrawOp.draw(drawObj, ctx, cc, drawParams, vpPtM, onlyAddToPath);
        });
    }

}


// create one drawObj for one tile
export function createOneDrawObjInMoc(nuniq, norder, npix, displayOrder, hipsOrder, coordsys, healpixCache, regionOptions, isAllSky) {
    const {wpCorners} = getCornerForPix(norder, npix, coordsys, healpixCache, null);


    //hipsOrder = (norder >= 8) ? norder : hipsOrder;   // no extra points inserted into the polygon's 4 corners if the order is high
    const polyPts = getMocSidePointsNuniq(norder, npix, hipsOrder+6, coordsys, wpCorners, isAllSky);
    const polyRegion = makeRegionPolygon(polyPts, regionOptions);
    const drawObj = drawRegions([polyRegion])[0];
    const mocInfo = {norder, displayOrder, hipsOrder, npix, nuniq};
    Object.assign(drawObj, {color: undefined, mocInfo});

    return drawObj;
}

// create all drawObjs
export function createDrawObjsInMoc(mocObj, plot) {
    const {displayOrder, regionOptions={}, allCells, hipsOrder, mocGroup} = mocObj;
    const healpixCache = getHealpixCornerTool();

    const drawObjAry = Object.keys(allCells).reduce((prev, nuniq) => {
        const {norder, npix} = allCells[nuniq];
            //if ((hipsOrder + 6 - norder) <= 10) {
        const drawObj = createOneDrawObjInMoc(nuniq, norder, npix, displayOrder, hipsOrder, plot.dataCoordSys,
                healpixCache, regionOptions, mocGroup.isAllSky);

        if (drawObj) {
            //  drawObj.text = ''+ nuniq;
            prev.push(drawObj);
        }
        return prev;
    }, []);

    Object.assign(mocObj, {drawObjAry});
    return drawObjAry;
}


function getVisibleTilesAtOrderPerNpix(plot, order) {
    const {centerWp, fov} = getPointMaxSide(plot, plot.viewDim);

    return getVisibleHiPSCells(order, centerWp, fov, plot.dataCoordSys)
            .reduce((npix_set, oneTile) => {
                set(npix_set, [oneTile.ipix], oneTile.wpCorners);
                return npix_set;
            }, {});
}


function getVisibleTiles(order, plot) {
    const visibleMap = {};
    let   highestOrderInMap;

    new Array(order).fill(0).map((i, idx) => idx+1)
        .find((d) => {

             if ((d > 1) && Object.keys(visibleMap[d - 1]).length > 5000) {
                return true;
             }

             set(visibleMap, [d], getVisibleTilesAtOrderPerNpix(plot, d));
             highestOrderInMap = d;
            return false;
        });

    return {visibleMap, highestOrderInMap};
}


export function setMocDisplayOrder(mocObj, plot, newDisplayOrder, newHipsOrderLevel) {
    if (newDisplayOrder) {
        const {mocGroup} = mocObj;
        const newAllCells = collectVisibleTilesFromMoc(mocGroup, plot, newDisplayOrder);

        set(mocObj, ['allCells'], newAllCells);
        return Object.assign({}, mocObj, {displayOrder: newDisplayOrder, hipsOrder: newHipsOrderLevel});
    } else {
        return mocObj;
    }
}


/*
 displayOrder provides an order numbeer to hint if producing more pixels around the polygon for the tile with lower
 order than 'displayOdrder' to have better resolution for moc rendering
*/
export function getMaxDisplayOrder(minOrder, maxOrder, plot) {
    const {norder} = getHiPSNorderlevel(plot);
    const displayOrder = Math.max(minOrder, norder) + 6;
    /*
    if ((maxOrder - minOrder) < 3) {
        displayOrder = Math.max(minOrder, norder) + 6;
    } else {
        displayOrder = Math.min(Math.max(norder, minOrder)+6, maxOrder);
    }
    */
    return {displayOrder, hipsOrderLevel: norder};
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

