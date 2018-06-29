import DrawObj from './DrawObj';
import {makeWorldPt} from '../Point.js';
import {isEmpty, has, set, get} from 'lodash';
import {makeRegionPolygon} from '../region/Region.js';
import {drawRegions} from '../region/RegionDrawer.js';
import {distanceToPolygon} from './ShapeDataObj.js';
import {getMocOrderIndex, getMocSidePointsNuniq, getCornerForPix, getMocNuniq,
        isTileVisibleByPosition, initSidePoints, NSIDE} from '../HiPSMocUtil.js';
import {getHealpixCornerTool,  getVisibleHiPSCells, getPointMaxSide} from '../HiPSUtil.js';
import {getScreenPixScaleArcSec} from '../WebPlot.js';
import {convertAngle} from '../VisUtil.js';
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

    Object.assign(obj, {regionOptions: {}, mocGroup, style});
    return obj;
}


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
        return this.groupInLevels;
    }

    upLevel() {
        if (this.currentLastOrder === 0 || this.currentLastOrder === this.minOrder) return false; // no up

        const crtLevel = this.currentLastOrder;
        const upLevel = crtLevel - 1;

        if (!has(this.groupInLevels, [upLevel])) {
            set(this.groupInLevels, [upLevel], []);
        }

        const tilesInUplevel = this.reduceToOrderFrom(crtLevel, upLevel);
        this.groupInLevels[upLevel].push(...tilesInUplevel);

        this.currentLastOrder = upLevel;
        set(this.groupInLevels, [crtLevel], []);
        return true;
    }

    downLevel() {
        const crtLevel = this.currentLastOrder;
        const nextLevel = crtLevel + 1;
        const crtTiles = this.groupInLevels[crtLevel];

        if (has(crtTiles[crtTiles.length-1], 'from')) {
            return false;
        }

        while(has(crtTiles[crtTiles.length-1], 'from')) {
            const oneTile = crtTiles.pop();
            const tilesOfNext = get(oneTile, 'from');

            this.groupInLevels[nextLevel].push(...tilesOfNext);
        }

        this.currentLastOrder += 1;
        return false;
    }

    getMocGroup() {
        return this.groupInLevels;
    }

    countTiles() {
        return Object.keys(this.groupInLevels).reduce((prev, order) => {
            prev += this.groupInLevels[order].length;
            return prev;
        }, 0);
    }

    countTilesAtOrder(order) {
        return (this.includeOrder(order)) ? this.groupInLevels[order].length : 0;
    }

    getOrders() {
        const orders = Object.keys(this.groupInLevels).filter((order) => {
            return (this.groupInLevels[order].length !== 0);
        });
        return sortNumber(orders);
    }

    getTilesAtOrder(order) {
        return this.includeOrder(order) ? this.groupInLevels[order] : null;
    }

    includeOrder(order) {
        return (order >= this.minOrder && order <= this.maxOrder);
    }

    reduceToOrderFrom(fromOrder, toOrder, tileList = [], stopAt) {
        if (!this.includeOrder(fromOrder) || !this.includeOrder(toOrder) ||
            toOrder > fromOrder) {
            return null;
        }
        if (fromOrder === toOrder) {
            return this.groupInLevels[fromOrder];
        }

        if (!stopAt) {
            stopAt = this.countTilesAtOrder(fromOrder);
        }

        const sNum = 2 * (fromOrder - toOrder);
        const rNpixs = tileList.map((oneTile) => oneTile.npix);
        const retVal = tileList.slice();

        this.groupInLevels[fromOrder].find((oneTile) => {
            const {npix} = oneTile;
            const nextNpix = npix >> sNum;  // divide by 4 ** (fromOrder - toOrder)

            const idx = rNpixs.indexOf(nextNpix);
            if (idx < 0) {
                const nextNuniq = getMocNuniq(toOrder, nextNpix);

                retVal.push({
                    npix: nextNpix, nuniq: nextNuniq,
                    from: [oneTile]
                });
                rNpixs.push(nextNpix);
                stopAt--;
            } else {
                const {from} = get(retVal, [idx]);

                from.push(oneTile);
            }
            return stopAt === 0 ? true : false;
        });
        return retVal;
    }

    moveLevelTo(level) {
        while (level !== this.currentLastOrder) {
            (level < this.currentLastOrder) ? this.upLevel() : this.downLevel();
        }
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

function collectVisibleTilesFromMoc(mocGroup, plot, displayOrder) {
    const TOTAL = Math.min(Math.max(Math.trunc(mocGroup.countTiles() * 0.2), 5000), 7000);
    const startOrder = Math.min(5, displayOrder);
    const healpixCache = getHealpixCornerTool();
    const {visibleMap, highestOrderInMap} = getVisibleTiles(startOrder, plot);
    const cc = CsysConverter.make(plot);
    const resTiles = {};

    const isNotInsideMap = (od, ipix) => {
        const testOrder = Math.min(od, highestOrderInMap);

        if (has(visibleMap, testOrder)) {      // check map first if order is greater than map's highest order or 0
            const npixTest = testOrder === od ? ipix : ipix >> (od - testOrder)*2;
            const idx = visibleMap[testOrder].npix.indexOf(npixTest);

            if (idx < 0) {
                return {invisible: true};
            } else {
                if (od === testOrder) {     // original tile, not parent tile
                    return {invisible: false, wpCorners: get(visibleMap, [testOrder, 'corners', idx])};
                }
            }
        }
        return {invisible: false};   // parent tile visible or tile is order 0 (not included in the map)
    };


    const addingCandidates = (ipix, fOrder, wpCorners) => {
        if (!wpCorners) {
            const corners = (fOrder !== 0) ? healpixCache.makeCornersForPix(ipix, NSIDE[fOrder], plot.dataCoordSys) :
                            getCornerForPix(fOrder, ipix, plot.dataCoordSys, healpixCache);
            wpCorners = corners.wpCorners;
        }

        if (isTileVisibleByPosition(wpCorners, cc)) {
            getCornerForPix(fOrder, ipix, plot.dataCoordSys, healpixCache, wpCorners);
            return true;
        } else {
            return false;
        }
    };

    let total = 0;
    const addToResult = (norder, npix, nuniq) => {
        resTiles[nuniq] = {norder, npix};
        total++;
    };

    const includedNpixs = [];      // visible tile npix list at level toOrder
    const notIncludedNpixs = [];   // invisible tile npix list

    const reduceTiles = (fromOrder, toOrder, tiles, onePerBigTile=true) => {
        tiles = tiles ? tiles : mocGroup.getTilesAtOrder(fromOrder);
        const sNum = 2 * (fromOrder - toOrder);    // divide by 4 ** (from-to)
        let   lastIdx = 0;

        tiles.find((oneTile, idx) => {
            const {npix, nuniq} = oneTile;
            const nextNpix = npix >> sNum;  // new npix
            if (idx > lastIdx) lastIdx = idx;

            // check if lower order npix of current tile, nextNpix, is already stored
            if (includedNpixs.includes(nextNpix)) {
                if (!onePerBigTile && addingCandidates(npix, fromOrder)) {
                    addToResult(fromOrder, npix, nuniq);
                }
                return total >= TOTAL;
            } else if (notIncludedNpixs.includes(nextNpix)) {
                return false;
            }

            // new nextNpix
            const tileInfo = isNotInsideMap(toOrder, nextNpix);
            if (tileInfo.invisible) {  // not visible for sure
                notIncludedNpixs.push(nextNpix);
                return false;
            }

            if (addingCandidates(npix, fromOrder)) {
                addToResult(fromOrder, npix, nuniq);
                includedNpixs.push(nextNpix);      // add to included list
            } else {
                return false;
            }

            return total >= TOTAL;
        });
    };

    const selectTilesByMap = (fromOrder, tiles) => {
        tiles = !isEmpty(tiles) ? tiles : mocGroup.getTilesAtOrder(fromOrder);
        let   lastIdx = 0;

        tiles.find((oneTile, idx) => {
            const {npix, nuniq} = oneTile;

            if (idx > lastIdx) lastIdx = idx;

            // new nextNpix
            const tileInfo = isNotInsideMap(fromOrder, npix);
            if (tileInfo.invisible) {  // not visible for sure
                return false;
            }

            if (addingCandidates(npix, fromOrder, tileInfo.wpCorners)) {
                addToResult(fromOrder, npix, nuniq);
            } else {
                return false;
            }


            return total >= TOTAL;
        });
    };

    const expandVisibleMap = (toOrder)  => {
        if (toOrder <= highestOrderInMap || has(visibleMap, toOrder)) {
            return true;
        }

        const incNum = NSIDE[2*(toOrder - highestOrderInMap)];   // npix increment

        if ((visibleMap[highestOrderInMap].npix.length * incNum) < mocGroup.countTilesAtOrder(toOrder)) {
            const npixAry = new Array(incNum).fill(0).map((i, idx) => idx);

            const nextSet = visibleMap[highestOrderInMap].npix.reduce((prev, oneNpix) => {
                const base_npix = oneNpix * incNum;

                npixAry.forEach((i) => {
                    prev.npix.push(base_npix + i);
                });

                return prev;
            }, {npix: []} );

            set(visibleMap, [toOrder], nextSet);
            return true;
        }
        return false;
    };

    initSidePoints();

    // collect tiles by expanding the visibleMap first
    for (let d = mocGroup.minOrder; d <= mocGroup.maxOrder; d++) {
        expandVisibleMap(d) ? selectTilesByMap(d, null) : reduceTiles(d, highestOrderInMap, null, false);

        if (total >= TOTAL) break;
    }

    return resTiles;
}

function getVisibleTiles(order, plot) {
    const {centerWp, fov} = getPointMaxSide(plot, plot.viewDim);
    let norder;

    if (fov > 160) {
        norder = Math.min(5, order);
    } else {
        norder = order +  Math.min(Math.ceil(Math.log2(180 / fov)), 4);
    }

    const visibleMap = new Array(norder).fill(0).map((i, idx) => idx+1)
                        .reduce((prev, d) => {
                           set(prev, [d], getVisibleHiPSCells(d, centerWp, fov, plot.dataCoordSys)
                                            .reduce((npix_set, oneTile) => {
                                                npix_set.npix.push(oneTile.ipix);
                                                npix_set.corners.push(oneTile.wpCorners);
                                                return npix_set;
                                          }, {npix: [], corners: []}));
                            return prev;
                        }, {});

    return {visibleMap, highestOrderInMap: norder};
}


export function setMocDisplayOrder(mocObj, plot, newDisplayOrder) {
    if (newDisplayOrder) {
        const {mocGroup} = mocObj;
        const newAllCells = collectVisibleTilesFromMoc(mocGroup, plot, newDisplayOrder);

        set(mocObj, ['allCells'], newAllCells);
        return Object.assign({}, mocObj, {displayOrder: newDisplayOrder});
    } else {
        return mocObj;
    }
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
export function createOneDrawObjInMoc(nuniq, norder, npix, displayOrder, coordsys, healpixCache, regionOptions) {
    const {wpCorners} = getCornerForPix(norder, npix, coordsys, healpixCache, null);


    displayOrder = (norder >= 8) ? norder : displayOrder;   // no extra points inserted into the polygon's 4 corners if the order is high
    const polyPts = getMocSidePointsNuniq(norder, npix, displayOrder, coordsys, wpCorners);
    const polyRegion = makeRegionPolygon(polyPts, regionOptions);
    const drawObj = drawRegions([polyRegion])[0];
    const mocInfo = {norder, displayOrder, npix, nuniq};
    Object.assign(drawObj, {color: undefined, mocInfo});

    return drawObj;
}

// create all drawObjs
export function createDrawObjsInMoc(mocObj, plot) {
    const {displayOrder, regionOptions={}, allCells} = mocObj;
    const healpixCache = getHealpixCornerTool();

    const drawObjAry = Object.keys(allCells).reduce((prev, nuniq) => {

        const {norder, npix} = allCells[nuniq];
        const drawObj = createOneDrawObjInMoc(nuniq, norder, npix, displayOrder, plot.dataCoordSys,
                                              healpixCache, regionOptions);


        if (drawObj) {
            //drawObj.text = ''+ drawObj.nuniq;
            prev.push(drawObj);
        }

        return prev;
    }, []);

    Object.assign(mocObj, {drawObjAry});

    return drawObjAry;
}

// displayOrder provides an order numbeer to hint if producing more pixels around the polygon for the tile with lower
// order than 'displayOdrder' to have better resolution for moc rendering
export function getMaxDisplayOrder(minOrder, maxOrder, plot, total_nuniq = 0) {
    const pArcsec = getScreenPixScaleArcSec(plot) * 8;
    let displayOrder;

    if (minOrder === maxOrder) {
        maxOrder = minOrder + 12;
    }

    displayOrder = maxOrder;

    for (let d = minOrder+1; d <= maxOrder; d++) {
        const tileArcsec = getTileSize(d);

        if (tileArcsec < pArcsec) {
            displayOrder = d-1;
            break;
        }
    }

    if (minOrder === 0 && total_nuniq < 10000) {
        displayOrder = Math.max(displayOrder, 3);
    }

    return {displayOrder, pixelSize: pArcsec};
}

const titleSizeMap = {};
function getTileSize(nOrder) {
    nOrder= Math.trunc(nOrder);
    if (titleSizeMap[nOrder]) return titleSizeMap[nOrder];
    const rad= Math.sqrt(4*Math.PI / (12*((2**nOrder)**2)));
    titleSizeMap[nOrder]=  convertAngle('radian', 'arcsec', rad);
    return titleSizeMap[nOrder];
}

function toRegion(drawObjAry, plot, drawParams) {
	//const {color, lineWidth} = drawParams;

	const resRegions = [];
	//updateColorFromDef(drawObj, def);

	return drawObjAry.reduce( (prev, dObj) => {
		const regList = DrawOp.toRegion(dObj, plot, drawParams);

		if (!isEmpty(regList)) {
			prev.push(...regList);
		}

		return prev;
	}, resRegions);
}

function sortNumber(aryToBeSorted, isAscend = true) {
    const sortNumber = isAscend ? (a, b) => (a - b) : (a, b) => (b - a);

    const numAry = aryToBeSorted.map((oneText) => {
        const num = Number(oneText);

        return isNaN(num) ? 0 : num;
    });
    numAry.sort(sortNumber);
    return numAry;
}
