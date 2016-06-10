
import Enum from 'enum';
import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil';
import {makeScreenPt, makeWorldPt} from '../Point.js';
import {Style} from './DrawingDef.js';
import {startRegionDes, setRegionPropertyDes, endRegionDes} from '../region/RegionDescription.js';
import {regionPropsList, RegionType} from '../region/Region.js';
import {isEmpty} from 'lodash';
import CsysConverter from '../CsysConverter.js';

const FOOTPRINT_OBJ= 'FootprintObj';
const DEFAULT_STYLE= Style.STANDARD;
const DEF_WIDTH = 1;


/**
 * Draw one or more closed polygons.
 *
 * This class can only be used with a WebPlot, You must use WorldPt
 *
 * @author tatianag, Trey
 * @version $Id: FootprintObj.java,v 1.14 2012/11/30 23:17:01 roby Exp $
 *
 * @param {[[]]} footprintAry an array of arrays of WorldPt, each array represents 1 footprint
 * @param {Enum} style
 * @return {object}
 */
function make(footprintAry,style) {
	if (!footprintAry && !footprintAry.length) return null;

	var obj= DrawObj.makeDrawObj();
	obj.type= FOOTPRINT_OBJ;
	obj.footprintAry= footprintAry;
	if (!style) obj.style= Style.STANDARD;
	return obj;

}




////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////

var draw=  {

	usePathOptimization(drawObj) {
		return drawObj.lineWidth===1;
	},

	getCenterPt(drawObj) {
		var {footprintAry}= drawObj;
		var xSum = 0;
		var ySum = 0;
		var xTot = 0;
		var yTot = 0;

		footprintAry.forEach( (footprint) => {
			footprint.forEach( (wp) => {
				xSum += wp.x;
				ySum += wp.y;
				xTot++;
				yTot++;
			});
		});
		return makeWorldPt(xSum / xTot, ySum / yTot);
	},

	getScreenDist(drawObj,plot, pt) {
		var minDistSq = Number.MAX_VALUE;
		const cc= CsysConverter.make(plot);

		drawObj.footprintAry.forEach( (footprint) => {
			var totX = 0;
			var distSq;
			var totY = 0;
			var last = cc.getScreenCoords(footprint[footprint.length - 1]);
			footprint.forEach( (wpt) => {
				var testPt = cc.getScreenCoords(wpt);
				if (testPt) {
					distSq = ptSegDistSq(testPt.x, testPt.y, last.x, last.y, pt.x, pt.y);
					totX += testPt.x;
					totY += testPt.y;
					if (distSq < minDistSq) minDistSq = distSq;
				}
				last = testPt;
			});
			var aveX = totX / footprint.length;
			var aveY = totY / footprint.length;
			distSq = distToPtSq(aveX, aveY, pt.x, pt.y);
			if (distSq < minDistSq) minDistSq = distSq;
		});

		return Math.sqrt(minDistSq);
	},

	draw(drawObj,ctx,drawTextAry,plot,def,vpPtM,onlyAddToPath) {
		var drawParams= makeDrawParams(drawObj,def);
		drawFootprint(ctx, plot, drawObj.footprintAry, drawParams, drawObj.renderOptions, onlyAddToPath);
	},

	toRegion(drawObj, plot, def) {
		return toRegion(drawObj.footprintAry, plot, makeDrawParams(drawObj,def),drawObj.renderOptions);
	},

	translateTo(drawObj,plot, apt) {
		Object.assign({},drawObj, {footprintAry:translateTo(drawObj.footprintAry,plot,apt)});
	},

	rotateAround(drawObj, plot, angle, worldPt) {
		Object.assign({},drawObj, {footprintAry:rotateAround(drawObj.footprintAry,worldPt)});
	}
};

export default {make,draw, FOOTPRINT_OBJ};

////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////


function makeDrawParams(fpObj,def) {
	var style= fpObj.style || def.style || DEFAULT_STYLE;
	var lineWidth= fpObj.lineWidth || def.lineWidth || DEF_WIDTH;
	return {
		color: DrawUtil.getColor(fpObj.color,def.color),
		lineWidth,
		style
	};
}





function distToPtSq(x0, y0, x1, y1) {
	var dx = x1 - x0;
	var dy = y1 - y0;
	return dx * dx + dy * dy;
}

/**
 * Returns the square of the distance from a point to a line segment. The
 * distance measured is the distance between the specified point and the
 * closest point between the specified endpoints. If the specified point
 * intersects the line segment in between the endpoints, this method returns
 * 0.0.
 *
 * COPIED from java.awt.geom.Line2D, all documentation of this method is
 * from Line2D.
 */
/**
 *
 * @param {number} x1 the coordinates of the beginning of the specified line segment
 * @param {number} y1 the coordinates of the beginning of the specified line segment
 * @param {number} x2 the coordinates of the end of the specified line segment
 * @param {number} y2 the coordinates of the end of the specified line segment
 * @param {number} px the coordinates of the specified point being measured against the specified line segment
 * @param {number} py the coordinates of the specified point being measured against the specified line segment
 * @return {number} a double value that is the square of the distance from the
 *         specified point to the specified line segment.
 */
function ptSegDistSq(x1, y1, x2, y2, px, py) {
	// Adjust vectors relative to x1,y1
	// x2,y2 becomes relative vector from x1,y1 to end of segment
	x2 -= x1;
	y2 -= y1;
	// px,py becomes relative vector from x1,y1 to test point
	px -= x1;
	py -= y1;
	var dotprod = px * x2 + py * y2;
	var projlenSq;
	if (dotprod <= 0) {
		// px,py is on the side of x1,y1 away from x2,y2
		// distance to segment is length of px,py vector
		// "length of its (clipped) projection" is now 0.0
		projlenSq = 0.0;
	} else {
		// switch to backwards vectors relative to x2,y2
		// x2,y2 are already the negative of x1,y1=>x2,y2
		// to get px,py to be the negative of px,py=>x2,y2
		// the dot product of two negated vectors is the same
		// as the dot product of the two normal vectors
		px = x2 - px;
		py = y2 - py;
		dotprod = px * x2 + py * y2;
		if (dotprod <= 0) {
			// px,py is on the side of x2,y2 away from x1,y1
			// distance to segment is length of (backwards) px,py vector
			// "length of its (clipped) projection" is now 0.0
			projlenSq = 0.0;
		} else {
			// px,py is between x1,y1 and x2,y2
			// dotprod is the length of the px,py vector
			// projected on the x2,y2=>x1,y1 vector times the
			// length of the x2,y2=>x1,y1 vector
			projlenSq = (dotprod * dotprod) / (x2 * x2 + y2 * y2);
		}
	}
	// Distance to line is now the length of the relative point
	// vector minus the length of its projection onto the line
	// (which is zero if the projection falls outside the range
	// of the line segment).
	var lenSq = px * px + py * py - projlenSq;
	if (lenSq < 0) lenSq = 0;
	return lenSq;
}


function drawFootprint(ctx, plot, footprintAry, drawParams, renderOptions, onlyAddToPath) {
	var inView = false;
	var footprint= null;
	var wpt= null;
	for (footprint of footprintAry) {
		for (wpt of footprint) {
			if (wpt && plot.pointInViewPort(wpt)) {
				inView = true;
				break;
			}
		}
		if (inView) break;
	}

	if (inView) {
		for (footprint of footprintAry) {
			drawStandardFootprint(ctx, footprint, plot, drawParams, onlyAddToPath);
		}
	}
}


function drawStandardFootprint(ctx, footprint, plot, drawParams, onlyAddToPath) {

	var wpt0 = footprint[footprint.length - 1];
	var wpt= null;
	var pt0;
	var pt;
	var {color,lineWidth} = drawParams;
	if (!onlyAddToPath) DrawUtil.beginPath(ctx,color,lineWidth);
	for (wpt of footprint) {
		pt0 = plot.getViewPortCoords(wpt0);
		pt = plot.getViewPortCoords(wpt);
		if (!pt0 || !pt) return;
		ctx.moveTo(pt0.x, pt0.y);
		if (!plot.coordsWrap(wpt0, wpt)) {
			ctx.lineTo(pt.x, pt.y);
		}
		wpt0 = wpt;
	}
	if (!onlyAddToPath) DrawUtil.stroke(ctx);
}


function toRegion(footprintAry, plot, drawParams) {
	var {color, lineWidth} = drawParams;
	var cc = CsysConverter.make(plot);
	var wpAry;
	var des;

	wpAry = footprintAry.map( (footprint) => cc.getWorldCoords(footprint) );
	des = startRegionDes(RegionType.polygon, cc, wpAry, null, null);
	if (isEmpty(des)) return [];

	des +=  setRegionPropertyDes(regionPropsList.COLOR, color) +
			setRegionPropertyDes(regionPropsList.LNWIDTH, lineWidth);
	des = endRegionDes(des);

	return [des];
}

function translateTo(footprintAry, plot, apt) {
	var pt = plot.getScreenCoords(apt);

	return footprintAry.map( (footprint) => {
		return footprint.map( (wpt) => {
			var pti = plot.getScreenCoords(wpt);
			return (pti) ? plot.getWorldCoords(makeScreenPt(pti.x + pt.x, pti.y + pt.y)) : wpt;
		});
	});
}

function rotateAround(footprintAry, plot, angle, wc) {
	var center = plot.getScreenCoords(wc);

	return footprintAry.map( (footprint) => {
		footprint.map( (p1) => DrawUtil.rotateAroundScreenPt(p1,plot,angle,center));
	});
}




