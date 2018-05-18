import DrawObj from './DrawObj';
import {makeWorldPt} from '../Point.js';
import {Style} from './DrawingDef.js';
import {isEmpty} from 'lodash';
import {makeRegionPolygon, makeRegionOptions} from '../region/Region.js';
import {drawRegions} from '../region/RegionDrawer.js';
import {distanceToPolygon} from './ShapeDataObj.js';
import {getMocOrderIndex, computeSideCellsToOrder} from '../HiPSMOCUtil.js';
import {getHealpixCornerTool} from '../HiPSUtil.js';
import {TextLocation} from './DrawingDef.js';
import DrawOp from './DrawOp.js';
import {get} from 'lodash';


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
 * @param {Object} coordsys
 * @param {Object} options
 * @return {object}
 */
function make(cellNums, coordsys, options) {
	if (!cellNums && !cellNums.length) return null;

	const obj= DrawObj.makeDrawObj();
	obj.type= MOC_OBJ;

    const healpixCache = getHealpixCornerTool();

    const cellOrder = cellNums.reduce((prev, oneNum) => {
        const {order} = getMocOrderIndex(oneNum);
        if (order > prev.max) {
            prev.max = order;
        }
        if (order < prev.min) {
            prev.min = order;
        }
        return prev;
    }, {max: 0, min: 100});

    if (cellOrder.max === cellOrder.min)  {
        cellOrder.max = 6;    // check if order 6 is good enough for display and performance
    }

    const sideCells = computeSideCellsToOrder(cellOrder.max);
    const allMocNums = [];
    const all_npix = cellNums.slice().reduce((prev, oneNum) => {
        const {order, npix} = getMocOrderIndex(oneNum);

        if (order === cellOrder.max) {
            prev.push([npix]);
        } else {
            const diff = cellOrder.max - order; // order difference to max order

            // get all cells of 4 sides at max order from npix of current order
            const cellsAtMaxOrder = sideCells[diff].map((oneCell) => {
                return (npix * (4**diff) + oneCell);
            });

            prev.push(cellsAtMaxOrder);

        }
        allMocNums.push({nuniq: oneNum, order, npix});
        return prev;
    }, []);

    const sideCorners = [[2, 3], [3, 0], [0, 1], [1, 2]]; //corners to be extracted from the 4 corners of each side cell,
                                                          // corners of each cell starts from right upper corner
    const regionOptions = makeRegionOptions(options ? options : {});

    // create a polygon region based on the side cells
    const createRegionOnSideCells = (npix_set) => {
        if (npix_set.length === 1) { // npix from order of maxOrder
            const {wpCorners} = healpixCache.makeCornersForPix(npix_set[0], (2**sideCells.max), coordsys);

            return makeRegionPolygon(wpCorners, regionOptions);
        } else {
            const oneSideCells = Math.floor(npix_set.length / 4);    // total cell of each side

            const polyPts = [0, 1, 2, 3].reduce((pts, s) => {
                    const firstIdx = s*oneSideCells;
                    const lastIdx = firstIdx + oneSideCells - 1;

                    for (let i = firstIdx; i <= lastIdx; i++) {
                        const {wpCorners} = healpixCache.makeCornersForPix(npix_set[i], (2**cellOrder.max), coordsys);
                        pts.push(wpCorners[sideCorners[s][0]]);
                        if (i === lastIdx) pts.push(wpCorners[sideCorners[s][1]]);
                    }
                    return pts;
               }, []);

            return makeRegionPolygon(polyPts, regionOptions);
        }

    };

    const regions = all_npix.reduce((prev, one_npix_set) => {
        const region = createRegionOnSideCells(one_npix_set);
        prev.push(region);

        return prev;
    }, []);


    const drawObjAry = drawRegions(regions);
    drawObjAry.forEach((obj, idx) => {
        obj.color = undefined;
        obj.mocNum = allMocNums[idx];
    });


    drawObjAry.forEach((oneObj) => {   // put text of npix number at center of the polygon
        oneObj.text = ''+ oneObj.mocNum.nuniq;
        oneObj.textLoc = TextLocation.CENTER;
    });

    Object.assign(obj,{drawObjAry});
	return obj;
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
		drawMoc(ctx, plot, drawObj.drawObjAry, def,vpPtM,onlyAddToPath);
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


function makeDrawParams(fpObj,def) {
	const style= def.style || fpObj.style || DEFAULT_STYLE;
	const lineWidth= def.lineWidth || fpObj.lineWidth || DEF_WIDTH;
	return {
		color: def.color || fpObj.color,
		lineWidth,
		style
	};
}


function drawMoc(ctx, plot, drawObjAry, def, vpPtM,onlyAddToPath) {
	drawObjAry.forEach((dObj) => {
        DrawOp.draw(dObj, ctx, plot, def, vpPtM,onlyAddToPath);
	});
}

export function getMocCell(mocObj, idx) {
    return (mocObj.type === MOC_OBJ && idx >= 0) ? get(mocObj, ['drawObjAry', idx]) : null;
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





