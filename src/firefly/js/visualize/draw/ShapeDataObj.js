/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil';
import {
    convertAngle, convertCelestial, lineCrossesRect, segmentIntersectRect, calculatePosition, distToLine, distanceToPolygon,
    computeSimpleDistance, computeSimpleSlopeAngle
} from '../VisUtil.js';
import {TextLocation, Style, DEFAULT_FONT_SIZE} from './DrawingDef.js';
import Point, {makeScreenPt, makeDevicePt, makeOffsetPt, makeWorldPt, makeImagePt, SimplePt} from '../Point.js';
import {toRegion} from './ShapeToRegion.js';
import {getDrawobjArea,  isScreenPtInRegion, makeHighlightShapeDataObj} from './ShapeHighlight.js';
import CsysConverter from '../CsysConverter.js';
import {has, isNil, get, set, isEmpty} from 'lodash';
import {getPlotViewById, getCenterOfProjection, getFoV} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {getPixScaleArcSec, getScreenPixScaleArcSec} from '../WebPlot.js';
import {toRadians, toDegrees} from '../VisUtil.js';
import {rateOpacity, maximizeOpacity} from '../../util/Color.js';

const FONT_FALLBACK= ',sans-serif';

/**
 * @typedef {Object} UnitType
 * @type {Enum}
 * @prop PIXEL
 * @prop ARCSEC
 * @prop IMAGE_PIXEL
 */
export const UnitType= new Enum(['PIXEL','ARCSEC','IMAGE_PIXEL']);
/**
 * @typedef {Object} ShapeType
 * @type {Enum}
 * @prop Line
 * @prop Text
 * @prop Circle
 * @prop Rectangle
 * @prop Ellipse
 * @prop Annulus
 * @prop BoxAnnulus
 * @prop EllipseAnnulus
 * @prop Polygon
 */
export const ShapeType= new Enum(['Line', 'Text','Circle', 'Rectangle', 'Ellipse',
                         'Annulus', 'BoxAnnulus', 'EllipseAnnulus', 'Polygon'], { ignoreCase: true });
export const SHAPE_DATA_OBJ= 'ShapeDataObj';
const DEF_WIDTH = 1;

const compositeObj = [ShapeType.Annulus, ShapeType.BoxAnnulus, ShapeType.EllipseAnnulus, ShapeType.Polygon];

export function makePoint(pt, plot, toType) {
    if (toType === Point.W_PT) {
        return plot.getWorldCoords(pt);
    } else if (toType === Point.SPT) {
        return plot.getScreenCoords(pt);
    } else if (toType === Point.IM_PT) {
        return plot.getImageCoords(pt);
    } else {
        return plot.getWorldCoords(pt);
    }
}

export function flipTextLocAroundY(plot, textLoc) {
    const pv = getPlotViewById(visRoot(), plot.plotId);

    if (pv?.flipY) {
        const locSet = [TextLocation.CIRCLE_NE, TextLocation.CIRCLE_NW,
            TextLocation.CIRCLE_SE, TextLocation.CIRCLE_SW,
            TextLocation.RECT_NE, TextLocation.RECT_NW,
            TextLocation.RECT_SE, TextLocation.RECT_SW,
            TextLocation.ELLIPSE_NE, TextLocation.ELLIPSE_NW,
            TextLocation.ELLIPSE_SE, TextLocation.ELLIPSE_SW,
            TextLocation.REGION_NE, TextLocation.REGION_NW,
            TextLocation.REGION_SE, TextLocation.REGION_SW];

        let idx = locSet.findIndex((loc) => (loc === textLoc));

        if (idx >= 0) {
            idx = idx%2 ? idx - 1 : idx + 1;
            return locSet[idx];
        }
    }
    return textLoc;
}

export function getPVRotateAngle(plot, angle) {
     const pv = getPlotViewById(visRoot(), plot.plotId);

     let angleInRadian = pv.rotation ? convertAngle('deg', 'radian', pv.rotation) : 0.0;
     if (pv.flipY) {
         angleInRadian = Math.PI - (angle - angleInRadian);
     } else {
         angleInRadian += angle;
     }

     const twoPI = Math.PI * 2;
     while (angleInRadian < 0 || angleInRadian >= twoPI) {
         if (angleInRadian < 0) {
             angleInRadian += twoPI;
         } else {
             angleInRadian -= twoPI;
         }
     }

     return angleInRadian;
}


function make(sType) {
    const obj= DrawObj.makeDrawObj();
    obj.pts= [];
    obj.sType= sType;
    obj.type= SHAPE_DATA_OBJ;
    // may contain the following:
        //obj.text= null;
        //obj.fontName = 'helvetica';
        //obj.fontSize = DEFAULT_FONT_SIZE;
        //obj.fontWeight = 'normal';
        //obj.fontStyle = 'normal';
        //obj.width= null;
        //obj.height= null;
        //obj.radius= null;
        //obj.style= Style.STANDARD;
        //obj.unitType = UnitType.PIXEL; // only supported by Circle so far
        //obj.textLoc= TextLocation.DEFAULT;
        //obj.textOffset= null;   // offsetScreenPt
    return obj;
}

function makeLine(pt1, pt2, bArrow=false) {
    return Object.assign(make(ShapeType.Line), {pts:[pt1, pt2], withArrow: bArrow});
}

function makeCircle(pt1, pt2) {
    return Object.assign(make(ShapeType.Circle), {pts:[pt1, pt2]});
}

function makeCircleWithRadius(pt1, radius, unitType= UnitType.PIXEL) {
    return Object.assign(make(ShapeType.Circle), {pts:[pt1],radius, unitType});
}

function makeRectangle(pt1, width, height, unitType= UnitType.PIXEL) {
    return Object.assign(make(ShapeType.Rectangle), {pts:[pt1],width,height, unitType});
}

function makeRectangleByCorners(pt1, pt2) {
    return Object.assign(make(ShapeType.Rectangle), {pts:[pt1, pt2]});
}

/**
 * make rectangle drawobj with the center and rotate angle specified
 * @param pt1
 * @param width
 * @param height
 * @param unitType
 * @param angle
 * @param angleUnit
 * @param isOnWorld if the rectangle have the width and height move along the east and north direction over the world domain
 * @param centerInView
 * @returns {Object}
 */
function makeRectangleByCenter(pt1, width, height, unitType=UnitType.PIXEL, angle = 0.0, angleUnit = UnitType.ARCSEC,
                               isOnWorld = true, centerInView = false) {
    const isCenter = true;
    return Object.assign(make(ShapeType.Rectangle), {pts:[pt1], width, height, unitType, angle, angleUnit, isOnWorld, isCenter, centerInView});
}

/**
 * make ellipse drawobj with the center and rotate angle specified
 * @param pt1
 * @param radius1
 * @param radius2
 * @param unitType
 * @param angle
 * @param angleUnit
 * @param isOnWorld if the rectangle have the width and height move along the east and north direction over the world domain
 * @returns {Object}
 */
function makeEllipse(pt1, radius1, radius2, unitType=UnitType.PIXEL, angle = 0.0, angleUnit = UnitType.ARCSEC, isOnWorld = true) {
    return Object.assign(make(ShapeType.Ellipse), {pts:[pt1], radius1, radius2, unitType, angle, angleUnit, isOnWorld});
}

/**
 * DrawObj contains pts, sType, radiusAry, unitType, drawObjAry
 * and optional properties for color, lineWidth, text, textOffset, font, textLoc
 * @param pt1
 * @param radiusAry [r1, r2, ...]
 * @param unitType
 * @param drawObjAry
 * @returns {Object}
 */
function makeAnnulus(pt1, radiusAry, unitType = UnitType.PIXEL, drawObjAry = null) {
    return Object.assign(make(ShapeType.Annulus), {pts:[pt1], radiusAry, unitType, drawObjAry});
}

/**
 * DrawObj contains pts, sType, radiusAry, unitType, drawObjAry
 * and optional properties for color, lineWidth, text, textOffset, font, textLoc
 * @param pt1
 * @param dimensionAry [[w, h], [w, h], ...]
 * @param unitType
 * @param angle
 * @param angleUnit
 * @param drawObjAry
 * @returns {*}
 */
function makeBoxAnnulus(pt1, dimensionAry, unitType = UnitType.PIXEL,
                        angle = 0.0, angleUnit = UnitType.ARCSEC, drawObjAry = null) {
    return Object.assign(make(ShapeType.BoxAnnulus), {pts:[pt1], dimensionAry, unitType, angle, angleUnit, drawObjAry});
}


/**
 * DrawObj contains pts, sType, radiusAry, unitType, drawObjAry
 * and optional properties for color, lineWidth, text, textOffset, font, textLoc
 * @param pt1
 * @param dimensionAry [[r1, r2], [r1, r2], ...]
 * @param unitType
 * @param angle
 * @param angleUnit
 * @param drawObjAry
 * @returns {*}
 */
function makeEllipseAnnulus(pt1, dimensionAry, unitType = UnitType.PIXEL,
                            angle = 0.0, angleUnit = UnitType.ARCSEC, drawObjAry = null) {
    return Object.assign(make(ShapeType.EllipseAnnulus), {pts:[pt1], dimensionAry, unitType, angle, angleUnit, drawObjAry});
}

/**
 * DrawObj contains pts, sType, radiusAry, unitType, drawObjAry
 * and optional properties for color, lineWidth, text, textOffset, font, textLoc
 * @param ptAry
 * @param drawObjAry
 * @returns {*}
 */
function makePolygon(ptAry, drawObjAry=null) {
    return Object.assign(make(ShapeType.Polygon), {pts: ptAry, drawObjAry});
}

/**
 * @desc make a text
 *  @param   pt
 *  @param  text
 *  @param  rotationAngle - the rotation angle + 'deg'
 *  @param isLonLine
 * @return {*}
 */

function makeText(pt, text, rotationAngle=undefined, isLonLine=true) {
    return Object.assign(make(ShapeType.Text), {pts:[pt],text, rotationAngle, isLonLine});
}

function makeTextWithOffset(textOffset, pt, text) {
    return Object.assign(make(ShapeType.Text), {pts:[pt],text,textOffset});
}


function makeDrawParams(drawObj,def={}) {
    const style= drawObj.style || def.style || Style.STANDARD;
    const lineWidth= drawObj.lineWidth || def.lineWidth || DEF_WIDTH;
    const textLoc= drawObj.textLoc || def.textLoc || TextLocation.DEFAULT;
    const unitType= drawObj.unitType || def.unitType || UnitType.PIXEL;
    const fontName= drawObj.fontName || def.fontName || 'helvetica';
    const fontSize= drawObj.fontSize || def.fontSize || DEFAULT_FONT_SIZE;
    const fontWeight= drawObj.fontWeight || def.fontWeight || 'normal';
    const fontStyle= drawObj.fontStyle || def.fontStyle || 'normal';
    const rotationAngle = drawObj.rotationAngle||undefined;

    return {
        color: DrawUtil.getColor(drawObj.color,def.color),
        lineWidth,
        textLoc,
        unitType,
        style,
        fontName,
        fontSize,
        fontWeight,
        fontStyle,
        rotationAngle
    };
}

const draw=  {


    usePathOptimization(drawObj,def) {
        return def.canUseOptimization;
    },

    getCenterPt(drawObj) {
        const {pts}= drawObj;
        const validPts = pts.filter( (pt) => pt!==null);

        if (validPts && validPts.length > 0) {
            let xSum = 0;
            let ySum = 0;
            let xTot = 0;
            let yTot = 0;

            validPts.forEach((wp) => {
                if (wp!==null) {
                    xSum += wp.x;
                    ySum += wp.y;
                    xTot++;
                    yTot++;
                }

            });


            const type = validPts[0].type;

            const x = xSum/xTot;
            const y = ySum/yTot;

            return type === Point.W_PT ? makeWorldPt(x, y) : SimplePt.make(x, y, type);

        } else {
            return makeWorldPt(0, 0);
        }
    },


    getScreenDist(drawObj,plot, pt) {
        let dist = -1;

        if (drawObj.sType === ShapeType.Line ) return distToLine(drawObj.pts, plot, pt);
        if (drawObj.sType === ShapeType.Polygon) return distanceToPolygon(drawObj.pts, plot, pt);
        if (drawObj.sType === ShapeType.Circle) return distanceToCircle(drawObj, plot, pt);
        if (drawObj.sType === ShapeType.Rectangle) return distanceToRectangle(drawObj, plot, pt);

        if (isScreenPtInRegion(drawObj, pt, plot).inside) return 0;
        const testPt = plot.getScreenCoords(draw.getCenterPt(drawObj));

        if (testPt) {    // distance to center, it can be updated to be the distance between the pt and the boundary
            const spt = plot.getScreenCoords(pt);
            const dx= spt.x - testPt.x;
            const dy= spt.y - testPt.y;
            dist= Math.sqrt(dx*dx + dy*dy);
        }
        return dist;
    },

    draw(drawObj,ctx,plot,def,vpPtM,onlyAddToPath) {
        const drawParams= makeDrawParams(drawObj,def);
        drawShape(drawObj,ctx,plot,drawParams,onlyAddToPath);
    },

    toRegion(drawObj,plot, def) {
        const drawParams= makeDrawParams(drawObj,def);
        return toRegion(drawObj,plot,drawParams);
    },

    translateTo(drawObj,plot, apt) {
        return translateShapeTo(drawObj, plot, apt);
    },

    rotateAround(drawObj, plot, angle, worldPt) {
        return rotateShapeAround(drawObj, plot, angle, worldPt);
    },

    makeHighlight(drawObj, plot, def) {
        return makeHighlightShapeDataObj(drawObj, CsysConverter.make(plot),  def);
    },

    isScreenPointInside(screenPt, drawObj, plot, def) {
        return  isScreenPtInRegion(drawObj, screenPt, CsysConverter.make(plot), def);
    }
};

export default {
    make,draw,makeLine,makeCircle,makeCircleWithRadius,
    makeRectangleByCorners,makeText, makeRectangleByCenter,
    makeTextWithOffset,makeRectangle, makeEllipse,
    makeAnnulus, makeBoxAnnulus, makeEllipseAnnulus, makePolygon,
    SHAPE_DATA_OBJ, FONT_FALLBACK, UnitType, ShapeType
};


function getRectangleCenterScreenPt(drawObj,plot,unitType) {
    let pt0, pt1;
    let w, h;
    const {width,height,pts}= drawObj;

    if (pts.length===1 && width && height) {
        pt0 = plot.getScreenCoords(pts[0]);

        switch (unitType) {
            case UnitType.PIXEL:
                w= width;
                h= height;
                break;

            case UnitType.ARCSEC:
                const corners = getRectCorners(pts[0], false, width, height, plot);
                return corners ? plot.getScreenCoords(corners.center) : null;

            case UnitType.IMAGE_PIXEL:
                const scale= plot.zoomFactor;
                w= scale*width;
                h= scale*height;
                break;
            default:
                w= width;
                h= height;
                break;
        }

        return makeScreenPt(pt0.x+w/2, pt0.y+h/2);

    }
    else {
        pt0= plot.getScreenCoords(pts[0]);
        pt1= plot.getScreenCoords(pts[1]);
        return makeScreenPt((pt0.x+pt1.x)/2, (pt0.y+pt1.y)/2);
    }
}

/**
 * get the 4 corners of the rectangle with width and height in the unit of arcsec
 *
 * if the given point is the center of rectangle, then find the corners around,
 * otherwise, the given point is assumed to be the upper right corner, then find the center first
 *
 * the return contains 4 corners and the center in world coordinate
 *
 * @param pt point in any coordinates
 * @param isCenter if the given point is the center of the rectangle
 * @param width  in arcsec
 * @param height in arcsec
 * @param {CysConverter} plot
 * @returns {{upperLeft: *, upperRight: *, lowerLeft: *, lowerRight: *, center:WorldPt}} corners in world coordinate
 */
function getRectCorners(pt, isCenter, width, height, plot) {
    let wpt = plot.getWorldCoords(pt);
    const w = width/2;
    const h = height/2;


    if (!wpt) return false;
    // compute 4 corners in J2000
    if (!isCenter) {
        const posCenter = calculatePosition(wpt, +w, -h); // go east and south to find the center

        wpt = makeWorldPt(posCenter.getLon(), posCenter.getLat());
    }

    const posLeft = calculatePosition(wpt, +w, 0.0); // go east
    const posRight = calculatePosition(wpt, -w, 0.0);
    const posUp = calculatePosition(wpt, 0.0, +h);   // go north
    const posDown = calculatePosition(wpt, 0.0, -h);

    const upperLeft = makeWorldPt(posLeft.getLon(), posUp.getLat());
    const upperRight = makeWorldPt(posRight.getLon(), posUp.getLat());
    const lowerLeft = makeWorldPt(posLeft.getLon(), posDown.getLat());
    const lowerRight = makeWorldPt(posRight.getLon(), posDown.getLat());

    // return 4 corners and center in world coordinate
    return {upperLeft, upperRight, lowerLeft, lowerRight, center: wpt};
}


const imagePixelToArcsec = (val, cc) => val * getPixScaleArcSec(cc);
const screenPixelToArcsec = (val, cc) => val * getScreenPixScaleArcSec(cc);

/**
 * calculate the rectangle slanted angle on the screen
 * the angle is in clockwise direction, consistent with canvas drawing
 *
 * return the upperLeft corner, dimension and slanted angle of the rectangle in screen or image coordinate
 * and the center of the rectangle
 *
 * @param wpt
 * @param {CysConverter} plot
 * @param isCenter
 * @param width
 * @param height
 * @param unit
 * @param isOnWorld
 * @returns {{width: *, height: *, unit: *, angle: *}} angle in radian on image coordinate, height, width in image pixel
 */

export function rectOnImage(wpt, isCenter, plot, width, height, unit, isOnWorld) {
    let corners;
    let imgUpperLeft, imgUpperRight, imgLowerLeft, imgLowerRight;
    let centerPt;
    let angle;

    if (isOnWorld) {
        // change the size to be arcse for 'center' typed rect
        if (unit === UnitType.IMAGE_PIXEL) {
            width = imagePixelToArcsec(width, plot);
            height = imagePixelToArcsec(height, plot);
        } else if (unit === UnitType.PIXEL) {
            width = screenPixelToArcsec(width, plot);
            height = screenPixelToArcsec(height, plot);
        }

        corners = getRectCorners(wpt, isCenter, width, height, plot);
        if (!corners) return null;

        imgUpperLeft = plot.getImageCoords(corners.upperLeft);
        imgUpperRight = plot.getImageCoords(corners.upperRight);
        imgLowerLeft = plot.getImageCoords(corners.lowerLeft);
        imgLowerRight = plot.getImageCoords(corners.lowerRight);
        if (!imgUpperLeft || !imgUpperRight || !imgLowerLeft || !imgLowerRight) return null;

        let xdist = (imgUpperLeft.x - imgUpperRight.x);
        let ydist = (imgUpperLeft.y - imgUpperRight.y);

        angle = Math.atan(ydist / xdist);
        width = Math.sqrt(xdist * xdist + ydist * ydist);

        xdist = (imgUpperLeft.x - imgLowerLeft.x);
        ydist = (imgUpperLeft.y - imgLowerLeft.y);
        height = Math.sqrt(xdist * xdist + ydist * ydist);

        unit = UnitType.IMAGE_PIXEL;

        centerPt = isCenter ? wpt : corners.center;
    } else {
        angle = 0.0;

        width = lengthToImagePixel(width, plot, unit);
        height = lengthToImagePixel(height, plot, unit);
        unit = UnitType.IMAGE_PIXEL;


        if (!isCenter) {
            const pt = plot.getImageCoords(wpt);  // upperLeft corner

            if (!pt) return null;
            centerPt = makeImagePt(pt.x + width/2, pt.y - height/2);
        } else {
            centerPt = plot.getImageCoords(wpt);
        }

        if (centerPt) {
            imgUpperLeft = makeImagePt(centerPt.x - width / 2, centerPt.y + height / 2);
            imgUpperRight = makeImagePt(centerPt.x + width / 2, centerPt.y + height / 2);
            imgLowerLeft = makeImagePt(centerPt.x - width / 2, centerPt.y - height / 2);
            imgLowerRight = makeImagePt(centerPt.x + width / 2, centerPt.y - height / 2);
        }

        if (!centerPt || !imgUpperLeft || !imgUpperRight || !imgLowerLeft || !imgLowerRight) return null;
    }

    return {width, height, unit, centerPt, angle, corners:[imgUpperLeft, imgUpperRight, imgLowerRight, imgLowerLeft]};
}

/**
 * compute if any of the 4 corners of rect is in viewport
 * @param cornerAry world coordinate
 * @param {CysConverter} plot
 * @returns {boolean}
 */
function cornerInView(cornerAry, plot) {
    let cInView;

    if (cornerAry) {
        cInView = cornerAry.find( (c) => plot.pointOnDisplay(c) );
    }

    return !isNil(cInView);
}

export function getValueInScreenPixel(plot, arcsecValue) {
    const retval= plot ? arcsecValue/(getScreenPixScaleArcSec(plot)) : arcsecValue;
    return retval<2 ? 2 : Math.round(retval);
}

/**
 *
 * @param drawObj
 * @param ctx
 * @param {CysConverter} plot
 * @param drawParams
 * @param onlyAddToPath
 */
export function drawShape(drawObj, ctx,  plot, drawParams, onlyAddToPath) {

    switch (drawObj.sType) {
        case ShapeType.Text:
            drawText(drawObj, ctx, plot, drawObj.pts[0], drawParams);
            break;
        case ShapeType.Line:
            drawLine(drawObj, ctx,  plot, drawParams, onlyAddToPath);
            break;
        case ShapeType.Circle:
            drawCircle(drawObj, ctx,  plot, drawParams);
            break;
        case ShapeType.Rectangle:
            drawRectangle(drawObj, ctx,  plot, drawParams, onlyAddToPath);
            break;
        case ShapeType.Ellipse:
            drawEllipse(drawObj, ctx,  plot, drawParams, onlyAddToPath);
            break;
        case ShapeType.Polygon:
            drawPolygon(drawObj, ctx,  plot, drawParams, onlyAddToPath);
            break;
        case ShapeType.Annulus:
        case ShapeType.BoxAnnulus:
        case ShapeType.EllipseAnnulus:
            drawCompositeObject(drawObj, ctx,  plot, drawParams, onlyAddToPath);
            break;}
}

function boxIntersectRect(pts, plot, w, h, isCenter, sRect, renderOptions) {
    const corners_rect = [];     // corners in device coordinate

    if (isCenter) {
        if (!sRect) return false;

        for (let i = 0; i < 4; i++) {
            const c_pt = plot.getDeviceCoords(sRect.corners[i]);
            if (!c_pt) return false;
            corners_rect.push(c_pt);
        }
    } else {
        const pt = plot.getDeviceCoords(pts);
        const corner_loc = [[0, 0], [1, 0], [1, 1], [0, 1]];
        const {translation} = renderOptions || {};     // consider translation
        const t_x = translation? translation.x : 0;
        const t_y = translation? translation.y : 0;

        for (let i = 0; i < 0; i++) {
            const c_pt = plot.getDeviceCoords(makeDevicePt(corner_loc[i][0] * w + pt.x + t_x,
                corner_loc[i][1] * h + pt.y + t_y));

            if (!c_pt) {
                return false;
            }
            corners_rect.push(c_pt);
        }
    }

    // find if one corner is with null device coordinate
    if (corners_rect.find((oneCorner) => !oneCorner)) {
        return false;
    }

    // find if one corner is in view
    if  (cornerInView(corners_rect, plot))  {
        return true;
    }

    const x1 = 0;
    const y1 = 0;
    const {width: x2, height: y2} = plot.viewDim;
    const view_two_corners = [makeDevicePt(x1, y1), makeDevicePt(x2, y2)];


    // all corners are out of view, check if any side intersects the viewDim
    for (let s = 0; s < 4; s++) {
        const idx1 = s;
        const idx2 = (s+1)%4;

        if (segmentIntersectRect(corners_rect[idx1], corners_rect[idx2], view_two_corners)) {
            return true;
        }
    }
    return false;
}


function circleIntersectRect(centerPt, circleRadius, plot, renderOptions) {
    // radius (x - center_x)^2 + (y - center_y) ^2 = circleRadius^2
    const center_dev = plot.getDeviceCoords(centerPt);
    const rect_x1 = 0;
    const rect_y1 = 0;
    const {width: rect_x2, height: rect_y2} = plot.viewDim;
    const {translation} = renderOptions || {};
    const center_x_dev = center_dev.x + (translation ? translation.x : 0);
    const center_y_dev = center_dev.y + (translation ? translation.y : 0);

    const center_pt = makeDevicePt(center_x_dev, center_y_dev);
    if (!center_pt) return false;

    const rsquare = Math.pow(circleRadius, 2);

    function diff_dist_to_center(x, y) {
        return  Math.pow((x - center_pt.x), 2) + Math.pow((y - center_pt.y), 2) - rsquare;
    }

    let offset = 0.0;
    let inside = 0;

    for (let x of [rect_x1, rect_x2]) {
        for (let y of [rect_y1, rect_y2]) {
            const loc = diff_dist_to_center(x, y);

            if (loc === 0.0) {   // circle outline intersect the corner
                return true;
            }

            const s = Math.abs(loc)/loc;   // 1.0 or -1.0
            if (offset === 0.0) {
                offset = s;
            } else {
                if (offset !== s) {   // current corner at different side of the circle from previous corner
                    return true;
                }
            }
            if (s < 0) {             // count the number of corners inside the circle
                inside++;
            }
        }
    }
    // rect inside circle
    if (inside === 4) {
        return false;
    }
    // all corners are out of circle area. Check if the circle intersects with any side of the rectangle.
    if (center_pt.x >= rect_x1 && center_pt.x <= rect_x2) {
        if ((center_pt.y >= rect_y1 && center_pt.y <= rect_y2) ||
            Math.abs(center_pt.y - rect_y1) <= circleRadius || Math.abs(center_pt.y - rect_y2) <= circleRadius) {
            return true;
        }
    } else if (center_pt.y >= rect_y1 && center_pt.y <= rect_y2) {
        if (Math.abs(center_pt.x - rect_x1) <= circleRadius || Math.abs(center_pt.x - rect_x2) <= circleRadius) {
            return true;
        }
    }

    return false;
}


/**
 *
 * @param drawObj
 * @param ctx
 * @param {CysConverter} plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawLine(drawObj, ctx,  plot, drawParams, onlyAddToPath) {
    const {pts, text, renderOptions, withArrow, drawEvenIfWrapping}= drawObj;
    const {style, color, lineWidth, textLoc, fontSize}= drawParams;

    const devPt0= plot.getDeviceCoords(pts[0]);
    const devPt1= plot.getDeviceCoords(pts[1]);
    if (!devPt0 || !devPt1) return;

    if (!drawEvenIfWrapping && plot.projection.isWrappingProjection()) {
        const wpPt0= plot.getWorldCoords(pts[0]);
        const wpPt1= plot.getWorldCoords(pts[1]);
        if (wpPt0 && wpPt1 && plot.coordsWrap(wpPt0,wpPt1)) return;
    }

    const inView= lineCrossesRect(devPt0.x,devPt0.y,devPt1.x,devPt1.y,0,0,plot.viewDim.width,plot.viewDim.height);

    if (!inView) return;

    if (!onlyAddToPath || style===Style.HANDLED) {
        DrawUtil.beginPath(ctx, color,lineWidth, renderOptions);
    }
    ctx.moveTo(devPt0.x, devPt0.y);
    ctx.lineTo(devPt1.x, devPt1.y);
    if (!onlyAddToPath || style===Style.HANDLED) DrawUtil.stroke(ctx);
    if (withArrow) {
        DrawUtil.drawArrowOnLine(ctx, devPt0, devPt1, color);
    }


    if (!isNil(text)) {
        if (textLoc === TextLocation.LINE_TOP_STACK) {
            const textLines = text.split('\n');
            textLines.forEach((oneText, idx) => {
                const tLocPt = makeTextLocationLine(plot, textLoc, fontSize, pts[0], pts[1], idx+1, drawObj);
                drawObj.text = oneText;
                drawText(drawObj,  ctx, plot, plot.getDeviceCoords(tLocPt), drawParams);
            });
            drawObj.text = text;
        } else {
            const textLocPt = makeTextLocationLine(plot, textLoc, fontSize, pts[0], pts[1]);
            drawText(drawObj, ctx, plot, plot.getDeviceCoords(textLocPt), drawParams);
        }
    }

    if ([Style.HANDLED, Style.STARTHANDLED, Style.ENDHANDLED].includes(style)) {
        const rAngle = computeSimpleSlopeAngle(devPt0, devPt1);
        const rOptions = {};

        rOptions.rotAngle = rAngle;

        // add handle to the 'from' end of the line
        if (style === Style.STARTHANDLED || style === Style.HANDLED) {
            rOptions.translation = {x: devPt0.x, y: devPt0.y};
            DrawUtil.fillRec(ctx, color, -3, -3, 7, 7, rOptions);
        }

        // add handle to the 'to' end of the line
        if (style === Style.ENDHANDLED || style === Style.HANDLED) {
            rOptions.translation = {x: devPt1.x, y: devPt1.y};
            DrawUtil.fillRec(ctx, color, -3, -3, 7, 7, rOptions);
        }
    }
}

/**
 *
 * @param drawObj
 * @param ctx
 * @param {CysConverter} plot
 * @param drawParams
 */
function drawCircle(drawObj, ctx,  plot, drawParams) {
    const {pts, text, radius, renderOptions}= drawObj;
    const {color, lineWidth, fontSize, textLoc, unitType}= drawParams;


    let screenRadius= 1;
    let cenDevPt;
    let inView = false;

    if (pts.length===1 && !isNil(radius)) {
        switch (unitType) {
            case UnitType.PIXEL: screenRadius= radius;
                break;
            case UnitType.IMAGE_PIXEL: screenRadius= (plot.zoomFactor*radius);
                break;
            case UnitType.ARCSEC: screenRadius= getValueInScreenPixel(plot,radius);
                break;
        }
        cenDevPt= plot ? plot.getDeviceCoords(pts[0]) : pts[0];


        if (!cenDevPt) {
            return;
        }

        // check if center is within display area or circle intersect the display area
        if (!plot || plot.pointOnDisplay(cenDevPt) ||
            circleIntersectRect(pts[0], screenRadius, plot, renderOptions)) {
            inView = true;
            DrawUtil.drawCircle(ctx,cenDevPt.x, cenDevPt.y,color, screenRadius, lineWidth, renderOptions,false);
        }
    }
    else {
        const pt0= plot ? plot.getDeviceCoords(pts[0]) : pts[0];
        const pt1= plot ? plot.getDeviceCoords(pts[1]) : pts[1];
        if (!pt0 || !pt1) return;

        cenDevPt = makeDevicePt((pt0.x+pt1.x)/2, (pt0.y+pt1.y)/2);
        const xDist= Math.abs(pt0.x-pt1.x)/2;
        const yDist= Math.abs(pt0.y-pt1.y)/2;
        screenRadius= Math.min(xDist,yDist);

        if (!plot || plot.pointOnDisplay(pt0) || plot.pointOnDisplay(pt1) || plot.pointOnDisplay(cenDevPt) ||
            circleIntersectRect(cenDevPt, screenRadius, plot, renderOptions)) {
            inView = true;
            DrawUtil.drawCircle(ctx,cenDevPt.x,cenDevPt.y,color, screenRadius, lineWidth, renderOptions,false );
        }
    }

    if (inView && !isNil(text)) {
        const textPt= makeTextLocationCircle(plot,textLoc, fontSize, cenDevPt, (screenRadius+lineWidth));
        drawText(drawObj, ctx, plot,textPt, drawParams);
    }
}

/**
 * @param drawObj
 * @param ctx
 * @param {CysConverter} plot
 * @param inPt
 * @param drawParams
 * @return {boolean} text is drawn or not
 */
export function drawText(drawObj, ctx, plot, inPt, drawParams) {
    if (!inPt) return false;
    const pv = getPlotViewById(visRoot(), plot.plotId);
    if (!pv) return false;
    
    const {text, textOffset, renderOptions, rotationAngle, isLonLine, textBaseline= 'top',
           textAngle=0, offsetOnScreen=false}= drawObj;
    let { textAlign='start'} = drawObj;
    //the angle of the grid line
    let angle;
    let pvAngle=undefined;

    if (rotationAngle){
        const lineAngle = parseFloat( rotationAngle.substring(0,  rotationAngle.length-3));
        pvAngle = pv.flipY? 180 - pv.rotation:pv.rotation;
        if (pvAngle>0) {
            if (isLonLine  && pvAngle<=210 ){
                //flip the label text
                pvAngle +=180;

            }

            if (!isLonLine && pvAngle > 80  && pvAngle<280) {
                //flip the label text
                pvAngle +=180;

            }
        }

        angle = pvAngle + lineAngle;
    } else {
        // offsetOnScreen only handle offset regardless of flip
        angle = pv.flipY&&!offsetOnScreen ? 180 + pv.rotation : pv.rotation;
    }

    let devicePt= plot.getDeviceCoords(inPt);
    if (!devicePt) {
        return false;
    }

    let x, y;
    if (offsetOnScreen) {
        const scrPt = plot.getScreenCoords(inPt);
        if (pv.flipY) {     // image and flipY case
            if (textAlign === 'end') {
                textAlign = 'start';
            } else if (textAlign === 'start') {
                textAlign = 'end';
            }
        }

        x = textOffset ? scrPt.x + textOffset.x : scrPt.x;
        y = textOffset ? scrPt.y + textOffset.y : scrPt.y;

        devicePt = plot.getDeviceCoords(makeScreenPt(x, y));
        if (!devicePt) {
            return false;
        }
        x = devicePt.x;
        y = devicePt.y;
    } else  {
        x = textOffset ? devicePt.x + textOffset.x : devicePt.x;
        y = textOffset ? devicePt.y + textOffset.y : devicePt.y;
    }

    if (textAngle) {
        angle = angle ? angle - textAngle : -textAngle;
    }

    const {fontName, fontSize, fontWeight, fontStyle}= drawParams;
    const color = drawParams.color || drawObj.color || 'black';

    let textHeight= 12;
    if (!isNaN(parseFloat(fontSize.substring(0, fontSize.length - 2)))) {
        textHeight = parseFloat(fontSize.substring(0, fontSize.length - 2)) * 14 / 10;
    }

    const textWidth = textHeight*text.length*8/20;

    if (x<2) {
        if (x<=-textWidth) return false; // don't draw
        x = 2;
    }
    if (y<2) {
        if (y<=-textHeight) return false; // don't draw
        y = 2;
    }

    const dim= plot.viewDim;
    const south = dim.height - textHeight - 2;
    const east = dim.width - textWidth - 2;

    if (x > east) {
        if (x>dim.width) return false; // don't draw
        x = east;
    }
    if (y > south) {
        if (y>dim.height) return false; // don't draw
        y = south;
    }

    const textColor = maximizeOpacity(color);

    DrawUtil.drawTextCanvas(ctx, text, x, y, textColor, Object.assign({}, renderOptions, {rotAngle:0.0}),
        {rotationAngle:angle, textBaseline, textAlign},
        {fontName:fontName+FONT_FALLBACK, fontSize, fontWeight, fontStyle}
    );



    drawObj.textWorldLoc = plot.getImageCoords(makeDevicePt(x, y));
    return true;
}

/**
 *
 * @param drawObj
 * @param ctx
 * @param {CysConverter} plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawRectangle(drawObj, ctx, plot, drawParams, onlyAddToPath) {
    const {pts, text, width, height, angleUnit, isCenter = false, isOnWorld = false, centerInView=false}= drawObj;
    let {renderOptions}= drawObj;
    const {color, lineWidth, style, textLoc, unitType, fontSize}= drawParams;
    let {angle = 0.0}= drawObj;
    let inView = false;
    let centerPt;
    let x, y, w, h;

    w = 0; h = 0; x = 0; y = 0;
    centerPt = null;

    // one center point case
    if (pts.length===1 && !isNil(width) && !isNil(height)) {
        let rectAngle = 0.0;      // in radian

        let sRect = null;
        const devPt0 = plot ? plot.getDeviceCoords(pts[0]) : pts[0];

        if (!devPt0) {
            return;
        }

        if (isCenter&&plot) {  // center and world coordinate: make the rectangle along the great circle of the globe
            sRect = rectOnImage(pts[0], isCenter, plot, width, height, unitType, isOnWorld);
        }
        //const devPt0 = plot ? plot.getDeviceCoords(pts[0]) : pts[0];
        // get w, h, and angle in the unit of device coordinate

        switch (unitType) {
            case UnitType.PIXEL:
                if (isCenter && sRect) {
                    w = sRect.width * plot.zoomFactor;
                    h = sRect.height * plot.zoomFactor;
                    rectAngle = -sRect.angle;
                } else {
                    w = width;
                    h = height;
                }
                break;
            case UnitType.ARCSEC:
                if (isCenter && sRect) {      // center and in view
                    w = sRect.width * plot.zoomFactor;
                    h = sRect.height * plot.zoomFactor;

                    // pt0 is the center
                    rectAngle = -sRect.angle;
                } else if (plot) {         // not center case
                    w = getValueInScreenPixel(plot, width);
                    h = getValueInScreenPixel(plot, height);
                } else {
                    w = width;
                    h = height;
                }

                break;
            case UnitType.IMAGE_PIXEL:
                if (isCenter && sRect) {
                    w = sRect.width * plot.zoomFactor;
                    h = sRect.height * plot.zoomFactor;
                    rectAngle = -sRect.angle;
                } else if (plot) {
                    w = plot.zoomFactor * width;
                    h = plot.zoomFactor * height;
                } else {
                    w = width;
                    h = height;
                }
                break;
            default:
                w = width;
                h = height;
        }

        if (!plot || (!isCenter && plot.pointOnDisplay(devPt0)) ||
                     (isCenter && sRect && centerInView && plot.pointOnDisplay(devPt0)) ||
                     boxIntersectRect(pts[0], plot, w, h, isCenter, sRect, renderOptions)) {
        //if ((!isCenter && plot.pointOnDisplay(devPt0)) ||
        //             (isCenter && sRect
        //                       && ((centerInView&&plot.pointOnDisplay(devPt0))||
        //                           (sRect && cornerInView(sRect.corners, plot))))) {
            inView = true;

            x = devPt0.x;   // x, y in device coordinates
            y = devPt0.y;
            if (h < 0 ) {
                h *= -1;
                y = (isCenter) ? y - h/2 : y - h;
            }

            if (w < 0 && !isCenter) {
                w *= -1;
                x = (isCenter) ? x - w/2 : x - w;
            }

            // make adjustment for rectangle with center and rotate angle

            if (isCenter) {
                if (angleUnit === UnitType.ARCSEC) {
                    angle = convertAngle('arcsec', 'radian', angle);
                } else if (angleUnit === UnitType.IMAGE_PIXEL) {
                    angle =  plot.zoomFactor * angle;
                }

                angle += rectAngle  + get(renderOptions, 'rotAngle', 0.0);
                angle = getPVRotateAngle(plot, angle);

                if (has(renderOptions, 'translation')) {
                    x += renderOptions.translation.x;
                    y += renderOptions.translation.y;
                }
                // move the rotation point to the center, drawing position[0, 0] is visually at {x, y}
                renderOptions = Object.assign({}, renderOptions,
                        {
                            rotAngle: angle,
                            translation: {x, y}
                        });

                centerPt = makeDevicePt(x, y);

                if (get(drawObj, 'inc')) {   // adjustment for highlight box
                    w = Math.floor(w + drawObj.inc);
                    h = Math.floor(h + drawObj.inc);
                }

                // draw the rect from {-w/2, -h/2} relative to the new origin
                x = -w/2;
                y = -h/2;
            } else {
                centerPt = makeDevicePt(x+w/2, y+h/2);
            }

            if ((style === Style.FILL || style === Style.DESTINATION_OUTLINE)) {
                DrawUtil.fillRec(ctx, color, x, y, w, h, renderOptions, color);
            } else {
                if (!onlyAddToPath || style === Style.HANDLED) {
                    DrawUtil.beginPath(ctx, color, lineWidth, renderOptions);
                }

                //--------------

                ctx.rect(x, y, w, h);
                if (!onlyAddToPath || style === Style.HANDLED) {
                    DrawUtil.stroke(ctx);
                }
            }
        }
    } else {  // two corners case
        const dPt0 = plot ? plot.getDeviceCoords(pts[0]) : pts[0];
        const dPt1= plot ? plot.getDeviceCoords(pts[1]) : pts[1];
        if (!dPt0 || !dPt1) return;

        x = dPt0.x;
        y = dPt0.y;
        w = dPt1.x - x;
        h = dPt1.y - y;

        if (plot && !boxIntersectRect(pts[0], plot, w, h, false, null, renderOptions)) {
            return;
        }

        //if (plot && (!plot.pointOnDisplay(dPt0) && !plot.pointOnDisplay(dPt1))) return;

        inView = true;

        if ((style === Style.FILL || style === Style.DESTINATION_OUTLINE)) {
            DrawUtil.fillRec(ctx, color, x, y, w, h, renderOptions, color);
        } else {
            if (!onlyAddToPath || style === Style.HANDLED) {
                DrawUtil.beginPath(ctx, color, lineWidth, renderOptions);
            }
            ctx.rect(x, y, w, h);
            if (!onlyAddToPath || style === Style.HANDLED) {
                DrawUtil.stroke(ctx);
            }
        }
        centerPt = makeDevicePt(x+w/2, y+h/2);
        angle = 0.0;
    }

    if (!isNil(text) && inView) {
        const textPt= makeTextLocationRectangle(plot, textLoc, fontSize, centerPt, w, h, angle, lineWidth);
        drawText(drawObj, ctx, plot, textPt, drawParams);
    }
    if (style === Style.HANDLED && inView) {
        // todo
    }
}

/**
 * draw ellipse
 * @param drawObj
 * @param ctx
 * @param {CysConverter} plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawEllipse(drawObj, ctx, plot, drawParams, onlyAddToPath) {
    const {pts, text, radius1, radius2,  angleUnit, isOnWorld = true}= drawObj;
    const {color, lineWidth, style, textLoc, unitType, fontSize}= drawParams;
    let {angle= 0, renderOptions}= drawObj;
    let inView = false;
    let centerPt= null;
    let pt0;
    let w= 0;
    let h= 0;
    let eAngle = 0.0;

    if ( pts.length ===1 && !isNil(radius1) && !isNil(radius2)) {
        pt0 = plot ? plot.getDeviceCoords(pts[0]) : pts[0];

        if (!pt0) {
            return;
        }
        centerPt = pt0;

        if (plot) {
            const sRect = rectOnImage(pts[0], true, plot, radius1*2, radius2*2, unitType, isOnWorld);
            if (!sRect) return;

            w = sRect.width * plot.zoomFactor/2;
            h = sRect.height * plot.zoomFactor/2;
            if (!boxIntersectRect(pts[0], plot, w, h, true, sRect, renderOptions)) {
            // if (!plot.pointOnDisplay(pt0) && !sRect && !cornerInView(sRect.corners, plot)) {
                inView = false;
            } else {
                eAngle = -sRect.angle;
                inView = true;
            }
        } else {
            inView = true;
            w = radius1;
            h = radius2;
        }

        if (inView) {
            if (h < 0) {
                h *= -1;
            }
            if (w < 0) {
                w *= -1;
            }

            // make adjustment for rectangle with center and rotate angle, angle = arc/w
            if (angleUnit === UnitType.ARCSEC) {
                angle = convertAngle('arcsec', 'radian', angle);
            } else if (angleUnit === UnitType.IMAGE_PIXEL) {
                angle = plot.zoomFactor * angle;
            }

            angle += eAngle + get(renderOptions, 'rotAngle', 0.0);
            angle = getPVRotateAngle(plot, angle);

            renderOptions = Object.assign({}, renderOptions,
                {
                    rotAngle: 0.0
                });

            if (!onlyAddToPath || style === Style.HANDLED) {
                DrawUtil.beginPath(ctx, color, lineWidth, renderOptions);
            }
            ctx.ellipse(pt0.x, pt0.y, w, h, angle, 0, 2*Math.PI);
            if (!onlyAddToPath || style === Style.HANDLED) {
                DrawUtil.stroke(ctx);
            }
        }

    }

    if (!isNil(text) && inView) {
        const textPt= makeTextLocationEllipse(plot, textLoc, fontSize, centerPt, w, h, angle, lineWidth);
        drawText(drawObj, ctx, plot, textPt, drawParams);
    }
    if (style === Style.HANDLED && inView) {
        // todo
    }
}

/**
 * draw the object which contains drawObj array
 * @param drawObj
 * @param ctx
 * @param {CysConverter} plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawCompositeObject(drawObj, ctx, plot, drawParams, onlyAddToPath) {
    const {drawObjAry}= drawObj;

    // draw the child drawObj
    if (drawObjAry) {
        drawObjAry.forEach( (oneDrawObj) => drawShape(oneDrawObj, ctx, plot, drawParams, onlyAddToPath));
    }

    drawCompositeText(drawObj, ctx, plot, drawParams);
}

/**
 @summary find world pt between project center & 'pt' and this point falls on the great circle plane built from center and 'pt'
 @param {Object} pt
 @param {Object} plot
 @param {number} angleFromCenter
 */
function getWorldPtByAngleFromProjectCenter(pt, plot, angleFromCenter) {
    const pt1 = getCenterOfProjection(plot);
    const pt2 = convertCelestial(pt, plot.projection.coordSys);

    /* world point to Cartesian coordinate */
    const WorldPtToxyz = (wpt) => {
        const sinRA = Math.sin(toRadians(wpt.x));
        const cosRA = Math.cos(toRadians(wpt.x));
        const sinDec = Math.sin(toRadians(wpt.y));
        const cosDec = Math.cos(toRadians(wpt.y));

        return {x: cosDec * cosRA, y: cosDec * sinRA, z: sinDec};
    };

    /* Cartesian coordinate to world point */
    const xyzToWorldPt = (x, y, z) => {
        const pole = 89.9999;

        // make dec -pole ~ pole
        let dec = toDegrees(Math.asin(z));   // -90~90
        let ra;

        if (dec > pole) {
            dec = pole;
        } else if (dec < -pole) {
            dec = -pole;
        }

        // make ra 0~360
        if (x === 0) {
            ra = y >= 0 ? 90 : 270;
        } else {
            ra = toDegrees(Math.atan(y / x));     // -90~90
            if (x < 0) {                          // -90~270
                ra += 180;
            }
            if (ra < 0) ra += 360;
        }

        return convertCelestial(makeWorldPt(ra, dec, plot.projection.coordSys));
    };

    // get norm in cartesian coordinate
    const getNormalFrom = (v1, v2) => {
        const nx = v1.y * v2.z - v2.y * v1.z;
        const ny = v1.z * v2.x - v2.z * v1.x;
        const nz = v1.x * v2.y - v2.x * v1.y;

        const l = Math.sqrt(nx**2 + ny**2 + nz**2);
        return {x: nx / l, y: ny / l, z: nz / l};
    };

    /* get the projection oriented boundary on display as moving along the the great circle from pt1 to pt2 */
    const pt1_c = WorldPtToxyz(pt1);
    const pt2_c = WorldPtToxyz(pt2);
    const norm = getNormalFrom(pt1_c, pt2_c);

    //angle between pt1 & pt2
    //note: the angle should be less than 180 deg along counterclockwise direction. (Math.acos works for angle between 0-180),
    //      otherwise triple product (a.(bxc)) could be used to detect if the angle between pt1 & pt2
    //      alogn the counterclockwise direction is greater than 180 or not.
    const deg = toDegrees(Math.acos(pt1_c.x * pt2_c.x + pt1_c.y * pt2_c.y + pt1_c.z * pt2_c.z));
    //angle from vector v to pt2
    const radToPt2 = toRadians(Math.abs(deg - angleFromCenter));


    // find vector v along the greater circle between pt1 & pt2, starting from pt1 with distance angle 'angleFromPt1'
    // v is solved by mv = y, where dot(v, pt1) = cos(angleFromPt1), dot(v, pt2) = cos(deg-angleFromPt1), dot(n, v) = 0
    const m = [[pt1_c.x, pt1_c.y, pt1_c.z],
        [pt2_c.x, pt2_c.y, pt2_c.z],
        [norm.x, norm.y, norm.z]];

    const y = [Math.cos(toRadians(angleFromCenter)), Math.cos(radToPt2), 0];  // 1: inner product(ret, pt2), 0: inner product(ret, pt1) = 0 (cos(90))

    const det_m = m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) +
        m[0][1] * (m[1][2] * m[2][0] - m[2][2] * m[1][0]) +
        m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);

    const adj_m = [[(m[1][1] * m[2][2] - m[2][1] * m[1][2]), -(m[0][1] * m[2][2] - m[2][1] * m[0][2]), (m[0][1] * m[1][2] - m[1][1] * m[0][2])],
        [-(m[1][0] * m[2][2] - m[2][0] * m[1][2]), (m[0][0] * m[2][2] - m[2][0] * m[0][2]), -(m[0][0] * m[1][2] - m[1][0] * m[0][2])],
        [(m[1][0] * m[2][1] - m[2][0] * m[1][1]), -(m[0][0] * m[2][1] - m[2][0] * m[0][1]), (m[0][0] * m[1][1] - m[1][0] * m[0][1])]];


    const v = adj_m.map((r) => {
        return (r[0] * y[0] + r[1] * y[1] + r[2] * y[2]) / det_m;
    });

    return  xyzToWorldPt(v[0], v[1], v[2]);
}

function getEdgePtOnGreatCircleFromCenterTo(pt, plot) {
    const eastAngle = 89.99999;

    return getWorldPtByAngleFromProjectCenter(pt, plot, eastAngle);
}

/**
 * draw polygon - draw outline of the polygon or fill the polygon
 * @param drawObj
 * @param ctx
 * @param {CysConverter} plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawPolygon(drawObj, ctx,  plot, drawParams, onlyAddToPath) {
    const {style, color, lineWidth=1} = drawParams;

    if ((style !== Style.FILL && style!==Style.DESTINATION_OUTLINE) && !isEmpty(drawObj.drawObjAry)) {
        drawCompositeObject(drawObj, ctx,  plot, drawParams, onlyAddToPath);
        return;
    }

    const adjustPointOnDisplay = (pt, devPt, style) => {
        if (!devPt) {
            const newPt = getEdgePtOnGreatCircleFromCenterTo(pt, plot);
            devPt = plot.getDeviceCoords(newPt);
            if (!devPt) {
                console.log('null DevPt');
                return devPt;
            }
         }

        if (style !== Style.FILL || style !== Style.DESTINATION_OUTLINE) return devPt;

        const {x,y}= devPt;
        const {width,height}= plot.viewDim;
        const retPt = {};

        retPt.x = (x < 0) ? 0 : ((x > width) ? width : x);
        retPt.y = (y < 0) ? 0 : ((y > height) ? height: y);

        return retPt;

    };

    const isPolygonInDisplay = (pts, style, plot) => {
        let wrapping= false;
        const checkWrap= Boolean(plot.projection.isWrappingProjection()) && getFoV(plot) >200;
        let devPts = pts.map((onePt,idx) => {
            const dPt= plot.getDeviceCoords(onePt);
            if (idx===0 || !dPt) return dPt;
            if (checkWrap && plot.coordsWrap(onePt, pts[idx-1])) {
                wrapping= true;
                return undefined;
            }
            return dPt;
        });

        if (checkWrap) {
            devPts= devPts.filter( (pt) => pt);
        }

        const {width, height} = plot.viewDim;

        // detect if none of the points are visible
        const anyVisible = devPts.find((onePt) => {
            return (onePt && (onePt.x >= 0) && (onePt.x < width) && (onePt.y >= 0) && (onePt.y < height));
        });

        if (!anyVisible) {
             return {devPts, inDisplay: 0};
        }

        let inDisplay = 0;
        const newPts = devPts.map((onePt, idx) => {
            const newPt = adjustPointOnDisplay(pts[idx], onePt);
            if (newPt) {
                inDisplay++;
            }
            return newPt;
        });


        return {devPts:newPts, inDisplay, wrapping};
    };

    const {pts, renderOptions}= drawObj;

    if (pts) {

        const {devPts, inDisplay, wrapping} = isPolygonInDisplay(pts, style, plot);  // check if polygon is out of display

        if (inDisplay <= 0 || wrapping) return;   // not visible

        if ((style === Style.FILL || style === Style.DESTINATION_OUTLINE)) {
            if (inDisplay < devPts.length) {
                console.log('less visible');     // test if this may happen
            }
            const devPtAll = devPts.filter((pt) => pt);
            const fillColor = drawObj.fillColor || color;
            const strokeColor = drawObj.strokeColor||rateOpacity(ctx.strokeStyle, 0);

            DrawUtil.fillPath(ctx, fillColor, devPtAll, true, renderOptions, strokeColor);
        } else {
            const polyPath = (pts, close) => {
                if (!onlyAddToPath) {
                    DrawUtil.beginPath(ctx, color, lineWidth, renderOptions);
                }
                DrawUtil.polygonPath(ctx, pts, close, null, null);
                if (!onlyAddToPath) {
                    DrawUtil.stroke(ctx);
                }
            };

            if (devPts.length === inDisplay) {
                polyPath(devPts, true);
            } else {
                // in case the polygon border has some discontinuities in display area
                for (let i = 0; i < devPts.length; ) {
                    if (devPts[i]) {
                        for (let j = i+1 ; j < devPts.length; j++) {
                            // get next if devPts visible or not the last point
                            if (devPts[j] && (j !== (devPts.length-1))) continue;

                            // visible and the last
                            if (devPts[j] && (j === devPts.length-1)) {
                                j++;
                            }
                            polyPath(devPts.slice(i,j), false);
                            i = j+1;
                            break;
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
    }

    if (drawObj.text) {
        drawCompositeText(drawObj, ctx, plot, drawParams);
    }
}


function drawCompositeText(drawObj, ctx, plot, drawParams) {
    const {text} = drawObj;
    const {textLoc, fontSize, lineWidth} = drawParams;

    // draw the text associated with the shape, find the overall covered area first
    if (!isNil(text)) {
        const objArea = getDrawobjArea(drawObj, plot);

        if (objArea && isAreaInView(objArea, plot)) {
            const textPt = makeTextLocationComposite(plot, textLoc, fontSize,
                objArea.width * plot.zoomFactor,
                objArea.height * plot.zoomFactor,
                objArea.center,
                lineWidth);
            if (textPt) {
                drawText(drawObj, ctx, plot, textPt, drawParams);
            }
        }
    }
}

/**
 * locate text for circle, return the location in screen coordinate
 * @param {CysConverter} plot
 * @param textLoc
 * @param fontSize
 * @param centerPt
 * @param screenRadius
 * @return {null}
 */
function makeTextLocationCircle(plot, textLoc, fontSize, centerPt, screenRadius) {
    const scrCenPt= plot.getScreenCoords(centerPt);
    if (!scrCenPt || screenRadius<1) return null;
    let opt;
    const fHeight = fontHeight(fontSize);

    switch (textLoc) {
        case TextLocation.CIRCLE_NE:
            opt= makeOffsetPt(-1*screenRadius, -1*(screenRadius+fHeight));
            break;
        case TextLocation.CIRCLE_NW:
            opt= makeOffsetPt(screenRadius, -1*(screenRadius+fHeight) );
            break;
        case TextLocation.CIRCLE_SE:
            opt= makeOffsetPt(-1*screenRadius, screenRadius);
            break;
        case TextLocation.CIRCLE_SW:
            opt= makeOffsetPt(screenRadius, screenRadius);
            break;
        default:
            opt= makeOffsetPt(0,0);
            break;
    }
    return makeScreenPt(scrCenPt.x+opt.x, scrCenPt.y+opt.y);

}

/**
 * locate text position for line in screen coordinate
 * @param {CysConverter} plot
 * @param textLoc
 * @param fontSize
 * @param inPt0
 * @param inPt1
 * @param tIndex text line number for multipe text lines on LINE_TOP_STACK location style
 * @param drawObj line obj
 * @return {ScreenPt}
 */
function makeTextLocationLine(plot, textLoc, fontSize, inPt0, inPt1, tIndex, drawObj) {
    if (!inPt0 || !inPt1) return null;
    let pt0= plot.getScreenCoords(inPt0);
    let pt1= plot.getScreenCoords(inPt1);

    if (!pt0 || !pt1) return null;
    const height= fontHeight(fontSize);
    let x, y;

    // pt1 is supposed to be lower on screen, not for TextLocation.LINE_TOP_STACK
    if ((pt0.y > pt1.y) && (textLoc !== TextLocation.LINE_TOP_STACK)) {
        [pt1, pt0] = [pt0, pt1];
        x = pt1.x+5;
        y = pt1.y+5;
    }

    if (textLoc===TextLocation.LINE_MID_POINT || textLoc===TextLocation.LINE_MID_POINT_OR_BOTTOM ||
            textLoc===TextLocation.LINE_MID_POINT_OR_TOP) {
        const dist= computeSimpleDistance(pt1,pt0);
        if (textLoc===TextLocation.LINE_MID_POINT_OR_BOTTOM && dist<100) {
            textLoc= TextLocation.LINE_BOTTOM;
        }
        if (textLoc===TextLocation.LINE_MID_POINT_OR_TOP && dist<80) {
            textLoc= TextLocation.LINE_TOP;
        }
    }

    switch (textLoc) {
        case TextLocation.LINE_TOP:
            y= pt0.y - (height+5);
            x =pt0.x + 5;
            break;
        case TextLocation.LINE_BOTTOM:
        case TextLocation.DEFAULT:
            break;
        case TextLocation.LINE_MID_POINT:
        case TextLocation.LINE_MID_POINT_OR_BOTTOM:
        case TextLocation.LINE_MID_POINT_OR_TOP:
            x= (pt1.x+pt0.x)/2;
            y= (pt1.y+pt0.y)/2;
            break;
        case TextLocation.LINE_TOP_STACK:    // stack multiple text lines at top side of the line
                                             // calculate rotation angle on screen coordinate domain
            const slope = computeSimpleSlopeAngle(pt0, pt1);
            const ratio = [1/5, 4/5];
            let   offset = height * (tIndex + 0.5);
            const r = -1;

            const pv = getPlotViewById(visRoot(), plot.plotId);

            if ((!pv.flipY && slope >= -Math.PI/2 && slope < Math.PI/2 && !pv.flipY) ||
                (pv.flipY && (slope < -Math.PI/2 || slope >= Math.PI/2 ))) { // quadrant 1 & 4 for no flipY
                                                                             // quadrant 2 & 3 for flipY
                x = pt1.x * ratio[0] + pt0.x * ratio[1];    // closer to p0
                y = pt1.y * ratio[0] + pt0.y * ratio[1];

                if (pv.flipY) {
                    offset *= r;
                }

                if (drawObj) {
                    drawObj.textAngle = -slope * 180.0 / Math.PI;
                    if (pv.flipY) {
                        drawObj.textAngle *= r;
                    }
                }

             } else {                                      // quadrant 2 & 3 for no flip, quadrant 1 & 4 for flipY
                x = pt0.x * ratio[0] + pt1.x * ratio[1];   // closer to p1
                y = pt0.y * ratio[0] + pt1.y * ratio[1];


                if (!pv.flipY) {
                    offset *= r;
                }
                if (drawObj) {
                    drawObj.textAngle = (Math.PI - slope) * 180.0 / Math.PI;
                    if (pv.flipY) {
                        drawObj.textAngle *= r;
                    }
                }
            }
            x = x + offset * Math.sin(slope);
            y = y - Math.abs(offset * Math.cos(slope));

            break;
        default:
            break;
    }

    return makeScreenPt(x,y);
}


/**
 * compute text location for rectangle in screen coordinate
 * @param {CysConverter} plot
 * @param textLoc
 * @param fontSize
 * @param centerPt
 * @param width
 * @param height
 * @param angle
 * @param lineWidth
 * @returns {ScreenPt} screen point
 */
function makeTextLocationRectangle(plot, textLoc, fontSize, centerPt, width, height, angle = 0.0, lineWidth = 1) {
    const scrCenPt= plot.getScreenCoords(centerPt);
    if (!scrCenPt || width <1 || height < 1) return null;

    const w = widthAfterRotation(width, height, angle)/2;
    const h = heightAfterRotation(width, height, angle)/2;

    let opt;
    const fHeight = fontHeight(fontSize);

    const offy = fHeight + lineWidth;
    switch (textLoc) {
        case TextLocation.RECT_NE:
            opt= makeOffsetPt(-1*w, -1*(h + offy));
            break;
        case TextLocation.RECT_NW:
            opt= makeOffsetPt(w, -1*(h));
            break;
        case TextLocation.RECT_SE:
            opt= makeOffsetPt(-1*w, h+offy);
            break;
        case TextLocation.RECT_SW:
            opt= makeOffsetPt(w, h);
            break;
        default:
            opt= makeOffsetPt(0,0);
    }
    return makeScreenPt(scrCenPt.x+opt.x, scrCenPt.y+opt.y);
}


/**
 * compute text location for ellipse in screen coordinate
 * @param {CysConverter} plot
 * @param textLoc
 * @param fontSize
 * @param centerPt
 * @param radius1  radius on horizotal axis
 * @param radius2  radius on vertical axis
 * @param angle    in radian
 * @param lineWidth
 * @returns {ScreenPt} screen location
 */
function makeTextLocationEllipse(plot, textLoc, fontSize, centerPt, radius1, radius2, angle, lineWidth = 1) {
    const scrCenPt= plot.getScreenCoords(centerPt);
    if (!scrCenPt || radius1 < 1 || radius2 < 1) return null;

    const w = widthAfterRotation(radius1, radius2, angle);  // half of horizontal coverage
    const h = heightAfterRotation(radius1, radius2, angle); // half of vertical coverage

    let opt;
    const height = fontHeight(fontSize);
    const offy = height + lineWidth;

    switch (textLoc) {
        case TextLocation.ELLIPSE_NE:
            opt= makeOffsetPt(-1*w, -1*(h + offy));
            break;
        case TextLocation.ELLIPSE_NW:
            opt= makeOffsetPt(w, -1*(h));
            break;
        case TextLocation.ELLIPSE_SE:
            opt= makeOffsetPt(-1*w, h + offy) ;
            break;
        case TextLocation.ELLIPSE_SW:
            opt= makeOffsetPt(w, h);
            break;
        default:
            opt= makeOffsetPt(0,0);
    }
    return makeScreenPt(scrCenPt.x+opt.x, scrCenPt.y+opt.y);
}


/**
 * compute text location for annulus-like and polygon shape
 * @param cc
 * @param textLoc
 * @param fontSize
 * @param width     in screen coordinate
 * @param height
 * @param centerPt
 * @param lineWidth
 * @returns {ScreenPt}
 */
export function makeTextLocationComposite(cc, textLoc, fontSize, width, height, centerPt, lineWidth = 1) {
    const w = width/2;
    const h = height/2 + lineWidth + 2;   // leave space for highlight box
    const scrCenterPt = cc.getScreenCoords(centerPt);

    if (!scrCenterPt || width <= 0 || height <= 0) return null;

    let opt;
    const offy = fontHeight(fontSize);

    switch (textLoc) {
        case TextLocation.REGION_NE:
            opt= makeOffsetPt(-w, -(h+offy));
            break;
        case TextLocation.REGION_NW:
            opt= makeOffsetPt(w, -(h+offy));
            break;
        case TextLocation.REGION_SE:
            opt= makeOffsetPt(-w, h) ;
            break;
        case TextLocation.REGION_SW:
            opt= makeOffsetPt(w, h);
            break;
        default:
            opt= makeOffsetPt(0,0);
    }
    return makeScreenPt(scrCenterPt.x+opt.x, scrCenterPt.y+opt.y);
}

// appears to be unused
// export function convertPt (pt, plot, toType) {
//   if (toType === Point.W_PT) {
//       return plot.getWorldCoords(pt);
//   } else if (toType === Point.SPT) {
//       return plot.getScreenCoords(pt);
//   } else {
//       return plot.getImageCoords(pt);
//   }
// }

/**
 * apt could be image or screen coordinate
 * @param {CysConverter} plot
 * @param pts
 * @param apt
 * @returns {*}
 */
export function translateTo(plot, pts, apt) {
    const pt_x = lengthToImagePixel(apt.x, plot, apt.type);
    const pt_y = lengthToImagePixel(apt.y, plot, apt.type);

    return pts.map( (inPt) => {
        const pti= plot.getImageCoords(inPt);
        return pti ? makePoint(makeImagePt(pt_x+pti.x, pt_y+pti.y), plot, inPt.type):null;
    });
}

export function translateShapeTo(drawObj, plot, apt) {
    if (!has(drawObj, 'pts')) return drawObj;

    const newPts = translateTo(plot, drawObj.pts, apt);
    const newObj = Object.assign({}, drawObj, {pts: newPts});

    // handle composite object
    if (compositeObj.includes(drawObj.sType)) {
        // translate each object included
        newObj.drawObjAry = drawObj.drawObjAry.map( (obj) => {
            return translateShapeTo(obj, plot, apt);
        });
    }

    return newObj;
}

/**
 * rotate the obj around a center pt
 * @param drawObj
 * @param {CysConverter} plot
 * @param angle in screen coordinate direction, radian
 * @param worldPt
 * @returns {*}
 */
export function rotateShapeAround(drawObj, plot, angle, worldPt) {
    if (!has(drawObj, 'pts')) return drawObj;

    const newPts = rotateAround(plot, drawObj.pts, angle, worldPt);
    const newObj = Object.assign({}, drawObj, {pts: newPts});
    const addRotAngle = (obj) => {
        if (obj.sType === ShapeType.Rectangle || obj.sType === ShapeType.Ellipse) {
            let rotAngle = angle;

            rotAngle += get(obj, 'renderOptions.rotAngle', 0.0);
            set(obj, 'renderOptions.rotAngle', rotAngle);
        }
    };

    addRotAngle(newObj);

    // handle composite object
    if (compositeObj.includes(newObj.sType)) {
        // rotate each object included
        newObj.drawObjAry = drawObj.drawObjAry.map( (obj) => {
            return rotateShapeAround(obj, plot, angle, worldPt);
        });
    }

    return newObj;
}

/**
 * rotate an array of points around a center on image coordinate
 * @param {CysConverter} plot
 * @param pts
 * @param angle in screen coordinate direction, radian
 * @param wc
 * @returns {*}
 */
export function rotateAround(plot, pts, angle, wc) {
    const center = plot.getImageCoords(wc);

    return pts.map( (p1) => {
        if (!p1) return null;

        const pti= plot.getImageCoords(p1);

        if (!pti) {
            return null;
        }

        const x1 = pti.x - center.x;
        const y1 = pti.y - center.y;
        const sin = Math.sin(-angle);
        const cos = Math.cos(-angle);

        // APPLY ROTATION
        const temp_x1 = x1 * cos - y1 * sin;
        const temp_y1 = x1 * sin + y1 * cos;

        // TRANSLATE BACK
        return makePoint(makeImagePt(temp_x1 + center.x, temp_y1 + center.y), plot, p1.type);
    });
}

/**
 * check if at least one corner of the covered area (in image coordinate) is within viewport
 * @param objArea
 * @param {CysConverter} plot
 * @returns {boolean}
 */
function isAreaInView(objArea, plot) {
    const {width: w, height:  h, center: pt} = objArea;
    const corners = [makeImagePt(pt.x - w/2, pt.y + h/2), makeImagePt(pt.x + w/2, pt.y + h/2),
        makeImagePt(pt.x + w/2, pt.y - h/2), makeImagePt(pt.x - w/2, pt.y - h/2)];
    const ptInView = corners.find((cPt) => ( plot.pointOnDisplay(cPt)));

    return !isNil(ptInView);
}


// calculate the font height in screen coordinate based on given font size
export function fontHeight (fontSize) {
    let height = 12;

    if (!isNaN(parseFloat(fontSize.substring(0, fontSize.length-2)))) {
        height = parseFloat(fontSize.substring(0, fontSize.length-2)) * 14/10 + 0.5;
    }
    return height;
}

/**
 * calculate the length in screen coordinate
 * @param r
 * @param {CysConverter} plot
 * @param unitType
 * @returns {*}
 */
export function lengthToImagePixel(r, plot, unitType) {
    let imageRadius;

    switch (unitType) {
        case UnitType.PIXEL:
            imageRadius = r/plot.zoomFactor;
            break;
        case UnitType.IMAGE_PIXEL:
            imageRadius = r;
            break;
        case UnitType.ARCSEC:
            imageRadius = r/getPixScaleArcSec(plot);
            break;
        default:
            imageRadius = r;
    }
    return imageRadius;
}

/**
 * calculate the length in screen coordinate
 * @param r
 * @param {CysConverter} plot
 * @param unitType
 * @returns {*}
 */
export function lengthToScreenPixel(r, plot, unitType) {
    let screenRadius;

    switch (unitType) {
        case UnitType.PIXEL:
            screenRadius = r;
            break;
        case UnitType.IMAGE_PIXEL:
            screenRadius = r * plot.zoomFactor;
            break;
        case UnitType.ARCSEC:
            screenRadius =  getValueInScreenPixel(plot, r);
            break;
        default:
            screenRadius = r;
    }
    return screenRadius;
}


/**
 * calculate the length in world coordinate
 * @param r
 * @param {CysConverter} plot
 * @param unitType
 * @returns {*}
 */
export function lengthToArcsec(r, plot, unitType) {
    let arcsecRadius;

    switch (unitType) {
        case UnitType.PIXEL:
            arcsecRadius = screenPixelToArcsec(r, plot);
            break;
        case UnitType.IMAGE_PIXEL:
            arcsecRadius = imagePixelToArcsec(r, plot);
            break;
        case UnitType.ARCSEC:
            arcsecRadius = r;
            break;
        default:
            arcsecRadius = r;
    }
    return arcsecRadius;
}


/**
 * calcuate the horizontal and vertical coverage after rotating a rectagle with width and height
 * @param width
 * @param height
 * @param angle
 * @returns {number}
 */
export function widthAfterRotation(width, height, angle) {
    const wcos = width * Math.cos(angle);
    const hsin = height * Math.sin(angle);

    return Math.max(Math.abs(wcos-hsin), Math.abs(wcos+hsin));
}

export function heightAfterRotation(width, height, angle) {
    const wsin = width * Math.sin(angle);
    const hcos = height * Math.cos(angle);

    return Math.max(Math.abs(wsin-hcos), Math.abs(wsin+hcos));
}

export function distanceToCircle(drawObj, cc, pt) {
    let   {radius, unitType} = drawObj;

    if (radius) {
        radius = cc ? lengthToScreenPixel(radius, cc, unitType) : radius;
        const devIn= cc.getDeviceCoords(pt);
        const devC= cc.getDeviceCoords(drawObj.pts[0]);
        return computeSimpleDistance(devIn,devC);
    }
    else {
         return 1;
    }

    // return distanceToCircle(radius, drawObj.pts, cc, pt);
}

export function distanceToRectangle(drawObj, cc, pt) {
    const spt = cc ? cc.getScreenCoords(pt) : makeScreenPt(pt.x, pt.y);
    const {width, height, unitType, pts, isOnWorld, isCenter, angle = 0.0, angleUnit} = drawObj;
    let   corners;
    const dist = Number.MAX_VALUE;

    if (pts.length === 2) {
        const p0 = cc ? cc.getScreenCoords(pts[0]) : makeScreenPt(pts[0].x, pts[0].y);
        const p1 = cc ? cc.getScreenCoords(pts[1]) : makeScreenPt(pts[1].x, pts[1].y);

        corners = [makeScreenPt(p0.x, p0.y), makeScreenPt(p1.x, p0.y), makeScreenPt(p1.x, p1.y), makeScreenPt(p0.x, p1.y)];
    } else if (pts.length === 1) {
        if (cc) {
            const rectImage = rectOnImage(pts, isCenter, cc, width, height, unitType, isOnWorld);

            corners = rectImage.corners;    // corners on image domain
            corners = corners.map((c) => cc.getScreenCoords(c));
        } else {
            const w = width/2;
            const h = height/2;

            corners = [[-w, -h], [w, -h], [w, h], [-w, h]].map((c) => makeScreenPt(pts[0].x+c[0], pts[0].y+c[1]));
        }

        const a = angleUnit === UnitType.ARCSEC ? convertAngle('arcsec', 'radian', angle)
                                                : (angleUnit === UnitType.IMAGE_PIXEL ? cc.zoomFactor * angle : angle);
        if (a !== 0) {
            corners = corners.map((c) => makeScreenPt(c.x * Math.cos(a)- c.y * Math.sin(a), c.x * Math.sin(a) + c.y * Math.cos(a)));
        }
    } else {
        return dist;
    }

    return corners.reduce((prev, pt, idx) => {
        const nIdx = (idx+1)%4;
        const d = distToLine([corners[idx], corners[nIdx]], cc, spt);

        if (d < prev) {
            prev = d;
        }
        return prev;
    }, dist);

}