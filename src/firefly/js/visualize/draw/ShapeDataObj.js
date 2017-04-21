/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import validator from 'validator';
import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil';
import VisUtil, {convertAngle} from '../VisUtil.js';
import {TextLocation, Style, DEFAULT_FONT_SIZE} from './DrawingDef.js';
import Point, {makeScreenPt, makeDevicePt, makeOffsetPt, makeWorldPt, makeImagePt} from '../Point.js';
import {toRegion} from './ShapeToRegion.js';
import {getDrawobjArea,  isScreenPtInRegion, makeHighlightShapeDataObj} from './ShapeHighlight.js';
import CsysConverter from '../CsysConverter.js';
import {has, isNil, get, set} from 'lodash';
import {getPlotViewById} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';

const FONT_FALLBACK= ',sans-serif';

const UnitType= new Enum(['PIXEL','ARCSEC','IMAGE_PIXEL']);
export const ShapeType= new Enum(['Line', 'Text','Circle', 'Rectangle', 'Ellipse',
                         'Annulus', 'BoxAnnulus', 'EllipseAnnulus', 'Polygon'], { ignoreCase: true });
const SHAPE_DATA_OBJ= 'ShapeDataObj';
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

    if (pv.flipY) {
        const locSet = [TextLocation.CIRCLE_NE, TextLocation.CIRCLE_NW,
            TextLocation.CIRCLE_SE, TextLocation.CIRCLE_SW,
            TextLocation.RECT_NE, TextLocation.RECT_NW,
            TextLocation.RECT_SE, TextLocation.RECT_SW,
            TextLocation.ELLIPSE_NE, TextLocation.ELLIPSE_NW,
            TextLocation.ELLIPSE_SE, TextLocation.ELLIPSE_SW,
            TextLocation.REGION_NE, TextLocation.REGION_NW,
            TextLocation.REGION_SE, TextLocation.REGION_SW];

        var idx = locSet.findIndex((loc) => (loc === textLoc));

        if (idx >= 0) {
            idx = idx%2 ? idx - 1 : idx + 1;
            return locSet[idx];
        }
    }
    return textLoc;
}

export function getPVRotateAngle(plot, angle) {
     const pv = getPlotViewById(visRoot(), plot.plotId);

     var angleInRadian = pv.rotation ? convertAngle('deg', 'radian', pv.rotation) : 0.0;
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

function makeLine(pt1, pt2) {
    return Object.assign(make(ShapeType.Line), {pts:[pt1, pt2]});
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
 * @returns {Object}
 */
function makeRectangleByCenter(pt1, width, height, unitType=UnitType.PIXEL, angle = 0.0, angleUnit = UnitType.ARCSEC, isOnWorld = true) {
    const isCenter = true;
    return Object.assign(make(ShapeType.Rectangle), {pts:[pt1], width, height, unitType, angle, angleUnit, isOnWorld, isCenter});
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
 *  @param  rotationAngle - the rotation angle + deg
 * @return {*}
 */
function makeText(pt, text, rotationAngle='0deg') {
    return Object.assign(make(ShapeType.Text), {pts:[pt],text, rotationAngle});
}

function makeTextWithOffset(textOffset, pt, text) {
    return Object.assign(make(ShapeType.Text), {pts:[pt],text,textOffset});
}


function makeDrawParams(drawObj,def={}) {
    var style= drawObj.style || def.style || Style.STANDARD;
    var lineWidth= drawObj.lineWidth || def.lineWidth || DEF_WIDTH;
    var textLoc= drawObj.textLoc || def.textLoc || TextLocation.DEFAULT;
    var unitType= drawObj.unitType || def.unitType || UnitType.PIXEL;
    var fontName= drawObj.fontName || def.fontName || 'helvetica';
    var fontSize= drawObj.fontSize || def.fontSize || DEFAULT_FONT_SIZE;
    var fontWeight= drawObj.fontWeight || def.fontWeight || 'normal';
    var fontStyle= drawObj.fontStyle || def.fontStyle || 'normal';
    var rotationAngle = drawObj.rotationAngle||'0deg';
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
        const dp= makeDrawParams(drawObj,def);
        return dp===Style.STANDARD &&
            (drawObj.sType===ShapeType.Line || drawObj.sType===ShapeType.Rectangle);
    },

    getCenterPt(drawObj) {
        return drawObj.pts[0];
    },

    getScreenDist(drawObj,plot, pt) {
        let dist = -1;
        let testPt;
        if (drawObj.sType===ShapeType.Rectangle) {
            testPt = getRectangleCenterScreenPt(drawObj,plot);
        } else {
            testPt = plot.getScreenCoords(drawObj.pts[0]);
        }
        if (testPt) {
            const dx= pt.x - testPt.x;
            const dy= pt.y - testPt.y;
            dist= Math.sqrt(dx*dx + dy*dy);
        }
        return dist;
    },

    draw(drawObj,ctx,drawTextAry,plot,def,vpPtM,onlyAddToPath) {
        const drawParams= makeDrawParams(drawObj,def);
        drawShape(drawObj,ctx,drawTextAry,plot,drawParams,onlyAddToPath);
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
                return plot.getScreenCoords(corners.center);

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
 * @param plot
 * @returns {{upperLeft: *, upperRight: *, lowerLeft: *, lowerRight: *}} corners in world coordinate
 */
function getRectCorners(pt, isCenter, width, height, plot) {
    let wpt = plot.getWorldCoords(pt);
    const w = width/2;
    const h = height/2;


    // compute 4 corners in J2000
    if (!isCenter) {
        const posCenter = VisUtil.calculatePosition(wpt, +w, -h); // go east and south to find the center

        wpt = makeWorldPt(posCenter.getLon(), posCenter.getLat());
    }

    const posLeft = VisUtil.calculatePosition(wpt, +w, 0.0); // go east
    const posRight = VisUtil.calculatePosition(wpt, -w, 0.0);
    const posUp = VisUtil.calculatePosition(wpt, 0.0, +h);   // go north
    const posDown = VisUtil.calculatePosition(wpt, 0.0, -h);

    const upperLeft = makeWorldPt(posLeft.getLon(), posUp.getLat());
    const upperRight = makeWorldPt(posRight.getLon(), posUp.getLat());
    const lowerLeft = makeWorldPt(posLeft.getLon(), posDown.getLat());
    const lowerRight = makeWorldPt(posRight.getLon(), posDown.getLat());

    // return 4 corners and center in world coordinate
    return {upperLeft, upperRight, lowerLeft, lowerRight, center: wpt};
}


const imagePixelToArcsec = (val, cc) => val * cc.projection.getPixelScaleArcSec();
const screenPixelToArcsec = (val, cc) => val * cc.projection.getPixelScaleArcSec()/cc.zoomFactor;

/**
 * calculate the rectangle slanted angle on the screen
 * the angle is in clockwise direction, consistent with canvas drawing
 *
 * return the upperLeft corner, dimension and slanted angle of the rectangle in screen or image coordinate
 * and the center of the rectangle
 *
 * @param wpt
 * @param plot
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
 * @param plot
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
    const retval= plot ?
                  arcsecValue/(plot.projection.getPixelScaleArcSec()/plot.zoomFactor) :
                  arcsecValue;
    return retval<2 ? 2 : Math.round(retval);
}

/**
 *
 * @param drawObj
 * @param ctx
 * @param drawTextAry
 * @param plot
 * @param drawParams
 * @param onlyAddToPath
 */
export function drawShape(drawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath) {

    switch (drawObj.sType) {
        case ShapeType.Text:
            drawText(drawObj,drawTextAry, plot, drawObj.pts[0], drawParams);
            break;
        case ShapeType.Line:
            drawLine(drawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath);
            break;
        case ShapeType.Circle:
            drawCircle(drawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath);
            break;
        case ShapeType.Rectangle:
            drawRectangle(drawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath);
            break;
        case ShapeType.Ellipse:
            drawEllipse(drawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath);
            break;
        case ShapeType.Annulus:
        case ShapeType.BoxAnnulus:
        case ShapeType.EllipseAnnulus:
        case ShapeType.Polygon:
            drawCompositeObject(drawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath);
            break;}
}

/**
 *
 * @param drawObj
 * @param ctx
 * @param drawTextAry
 * @param plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawLine(drawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath) {
    const {pts, text, renderOptions}= drawObj;
    const {style, color, lineWidth, textLoc, fontSize}= drawParams;

    let inView= false;
    const devPt0= plot.getDeviceCoords(pts[0]);
    const devPt1= plot.getDeviceCoords(pts[1]);
    if (!devPt0 || !devPt1) return;


    if (plot.pointOnDisplay(devPt0) || plot.pointOnDisplay(devPt1)) {
        inView= true;
        if (!onlyAddToPath || style===Style.HANDLED) {
            DrawUtil.beginPath(ctx, color,lineWidth, renderOptions);
        }
        ctx.moveTo(devPt0.x, devPt0.y);
        ctx.lineTo(devPt1.x, devPt1.y);
        if (!onlyAddToPath || style===Style.HANDLED) DrawUtil.stroke(ctx);
    }

    if (!isNil(text) && inView) {
        const textLocPt= makeTextLocationLine(plot, textLoc, fontSize,pts[0], pts[1]);
        drawText(drawObj, drawTextAry, plot, plot.getDeviceCoords(textLocPt), drawParams);
    }

    if (style===Style.HANDLED && inView) {
        DrawUtil.fillRec(ctx, color,devPt0.x-2, devPt0.y-2, 5,5, renderOptions);
        DrawUtil.fillRec(ctx, color,devPt1.x-2, devPt1.y-2, 5,5, renderOptions);
    }
}

/**
 *
 * @param drawObj
 * @param ctx
 * @param drawTextAry
 * @param plot
 * @param drawParams
 */
function drawCircle(drawObj, ctx, drawTextAry, plot, drawParams) {
    const {pts, text, radius, renderOptions}= drawObj;
    const {color, lineWidth, fontSize, textLoc, unitType}= drawParams;


    let screenRadius= 1;
    let centerPt;
    let cenDevPt;

    if (pts.length===1 && !isNil(radius)) {
        switch (unitType) {
            case UnitType.PIXEL: screenRadius= radius;
                break;
            case UnitType.IMAGE_PIXEL: screenRadius= (plot.zoomFactor*radius);
                break;
            case UnitType.ARCSEC: screenRadius= getValueInScreenPixel(plot,radius);
                break;
        }
        cenDevPt= plot.getDeviceCoords(pts[0]);
        if (plot.pointOnDisplay(cenDevPt)) {
            DrawUtil.drawCircle(ctx,cenDevPt.x, cenDevPt.y,color,lineWidth, screenRadius,renderOptions,false);
        }
    }
    else {
        const pt0= plot.getDeviceCoords(pts[0]);
        const pt1= plot.getDeviceCoords(pts[1]);
        if (!pt0 || !pt1) return;
        if (plot.pointOnDisplay(pt0) || plot.pointOnDisplay(pt1)) {

            const xDist= Math.abs(pt0.x-pt1.x)/2;
            const yDist= Math.abs(pt0.y-pt1.y)/2;
            screenRadius= Math.min(xDist,yDist);

            const x= Math.min(pt0.x,pt1.x) + Math.abs(pt0.x-pt1.x)/2;
            const y= Math.min(pt0.y,pt1.y) + Math.abs(pt0.y-pt1.y)/2;
            cenDevPt= makeDevicePt(x,y);

            DrawUtil.drawCircle(ctx,cenDevPt.x,cenDevPt.y,color,lineWidth,screenRadius,renderOptions,false );
        }
    }

    if (centerPt && !isNil(text)) {
        const textPt= makeTextLocationCircle(plot,textLoc, fontSize, centerPt, (screenRadius+lineWidth));
        drawText(drawObj, drawTextAry, plot,textPt, drawParams);
    }
}


/**
 * @param drawObj
 * @param drawTextAry
 * @param plot
 * @param inPt
 * @param drawParams
 */
export function drawText(drawObj, drawTextAry, plot, inPt, drawParams) {
    if (!inPt) return;
    
    const {text, textOffset, renderOptions, rotationAngle}= drawObj;
    const {fontName, fontSize, fontWeight, fontStyle}= drawParams;
    const color = drawParams.color || drawObj.color || 'black';

    const devicePt= plot.getDeviceCoords(inPt);
    if (plot.pointOnDisplay(devicePt)) {

        let {x,y}= devicePt;

        let textHeight= 12;
        if (validator.isFloat(fontSize.substring(0, fontSize.length - 2))) {
            textHeight = parseFloat(fontSize.substring(0, fontSize.length - 2)) * 14 / 10;
        }

        const textWidth = textHeight*text.length*8/20;
        if (textOffset) {
            x+=textOffset.x;
            y+=textOffset.y;
        }
        if (x<2) {
            if (x<=-textWidth) return; // don't draw
            x = 2;
        }
        if (y<2) {
            if (y<=-textHeight) return; // don't draw
            y = 2;
        }

        const dim= plot.viewDim;
        const south = dim.height - textHeight - 2;
        const east = dim.width - textWidth - 2;

        if (x > east) {
            if (x>dim.width) return; // don't draw
            x = east;
        }
        if (y > south) {
            if (y>dim.height) return; // don't draw
            y = south;
        }

        DrawUtil.drawText(drawTextAry, text, x, y, color, renderOptions,
                fontName+FONT_FALLBACK, fontSize, fontWeight, fontStyle,null,null,rotationAngle);
        drawObj.textWorldLoc = plot.getImageCoords(makeDevicePt(x, y));
    }
}

/**
 *
 * @param drawObj
 * @param ctx
 * @param drawTextAry
 * @param plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawRectangle(drawObj, ctx, drawTextAry,  plot, drawParams, onlyAddToPath) {
    const {pts, text, width, height, angleUnit, isCenter = false, isOnWorld = false}= drawObj;
    let {renderOptions}= drawObj;
    const {color, lineWidth, style, textLoc, unitType, fontSize}= drawParams;
    let {angle = 0.0}= drawObj;
    let inView = false;
    let centerPt;
    let pt0, pt1;
    let x, y, w, h;

    w = 0; h = 0; x = 0; y = 0;
    centerPt = null;

    if (pts.length===1 && !isNil(width) && !isNil(height)) {
        let rectAngle = 0.0;      // in radian

        let sRect;

        if (isCenter) {
            sRect = rectOnImage(pts[0], isCenter, plot, width, height, unitType, isOnWorld);
        }

        const devPt0 = plot ? plot.getDeviceCoords(pts[0]) : pts[0];
        if (!plot || (!isCenter && plot.pointOnDisplay(devPt0)) ||
                     (isCenter && sRect && cornerInView(sRect.corners, plot))) {
            inView = true;

            switch (unitType) {
                case UnitType.PIXEL:
                    if (isCenter) {
                        w = sRect.width * plot.zoomFactor;
                        h = sRect.height * plot.zoomFactor;
                        rectAngle = -sRect.angle;
                    } else {
                        w = width;
                        h = height;
                    }
                    break;
                case UnitType.ARCSEC:
                    if (isCenter) {      // center and in view
                        w = sRect.width * plot.zoomFactor;
                        h = sRect.height * plot.zoomFactor;

                        // pt0 is the center
                        rectAngle = -sRect.angle;
                    } else {         // not center case
                        w = getValueInScreenPixel(plot, width);
                        h = getValueInScreenPixel(plot, height);
                    }

                    break;
                case UnitType.IMAGE_PIXEL:
                    if (isCenter) {
                        w = sRect.width * plot.zoomFactor;
                        h = sRect.height * plot.zoomFactor;
                        rectAngle = -sRect.angle;
                    } else {
                        w = plot.zoomFactor * width;
                        h = plot.zoomFactor * height;
                     }
                     break;
                default:
                    w = width;
                    h = height;
            }



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

                //angle = angleAfterFlip(angle);

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
    else {  // two corners case
        const devPt0= plot ? plot.getDeviceCoords(pts[0]) : pts[0];
        const devPt1= plot ? plot.getDeviceCoords(pts[1]) : pts[1];
        if (!pt0 || !pt1) return;
        if (plot && (!plot.pointOnDisplay(devPt0) && !plot.pointOnDisplay(devPt1))) return;

        inView = true;
        //todo here here here

        x = devPt0.x;
        y = devPt0.y;
        w = devPt1.x - x;
        h = devPt1.y - y;
        if (!onlyAddToPath || style === Style.HANDLED) {
            DrawUtil.beginPath(ctx, color, lineWidth, renderOptions);
        }
        ctx.rect(x, y, w, h);
        if (!onlyAddToPath || style === Style.HANDLED) {
            DrawUtil.stroke(ctx);
        }
        centerPt = makeDevicePt(x+w/2, y+h/2);
        angle = 0.0;
        }

    if (!isNil(text) && inView) {
        const textPt= makeTextLocationRectangle(plot, textLoc, fontSize, centerPt, w, h, angle, lineWidth);
        drawText(drawObj, drawTextAry, plot, textPt, drawParams);
    }
    if (style === Style.HANDLED && inView) {
        // todo
    }
}

/**
 * draw ellipse
 * @param drawObj
 * @param ctx
 * @param drawTextAry
 * @param plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawEllipse(drawObj, ctx, drawTextAry,  plot, drawParams, onlyAddToPath) {
    const {pts, text, radius1, radius2, renderOptions, angleUnit, isOnWorld = true}= drawObj;
    const {color, lineWidth, style, textLoc, unitType, fontSize}= drawParams;
    let {angle= 0}= drawObj;
    let inView = false;
    let centerPt= null;
    let pt0;
    let w= 0;
    let h= 0;
    let eAngle = 0.0;

    if ( pts.length ===1 && !isNil(radius1) && !isNil(radius2)) {
        pt0 = plot ? plot.getDeviceCoords(pts[0]) : pts[0];
        centerPt = pt0;

        if (plot) {
            const sRect = rectOnImage(pts[0], true, plot, radius1*2, radius2*2, unitType, isOnWorld);
            if (!plot.pointOnDisplay(pt0) || !sRect || !cornerInView(sRect.corners, plot)) {
                inView = false;
            } else {
                w = sRect.width * plot.zoomFactor/2;
                h = sRect.height * plot.zoomFactor/2;
                eAngle = -sRect.angle;
                inView = true;
            }
        } else {
            inView = true;
            switch (unitType) {
                case UnitType.PIXEL:
                    w = radius1;
                    h = radius2;
                    break;
                case UnitType.ARCSEC:
                    w = getValueInScreenPixel(plot, radius1);
                    h = getValueInScreenPixel(plot, radius2);
                    break;
                case UnitType.IMAGE_PIXEL:
                    w = plot.zoomFactor * radius1;
                    h = plot.zoomFactor * radius2;
                    break;
                default:
                    w = radius1;
                    h = radius2;
                    break;
            }
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

            angle += eAngle;
            angle = getPVRotateAngle(plot, angle);

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
        drawText(drawObj, drawTextAry, plot, textPt, drawParams);
    }
    if (style === Style.HANDLED && inView) {
        // todo
    }
}

/**
 * draw the object which contains drawObj array
 * @param drawObj
 * @param ctx
 * @param drawTextAry
 * @param plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawCompositeObject(drawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath) {
    const {drawObjAry, text}= drawObj;
    const {lineWidth, textLoc, fontSize} = drawParams;

    // draw the child drawObj
    if (drawObjAry) {
        drawObjAry.forEach( (oneDrawObj) => drawShape(oneDrawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath));
    }

    // draw the text asscociated with the shape, find the overal covered area first
    if (!isNil(text)) {
        const objArea = getDrawobjArea(drawObj, plot);

        if (objArea && isAreaInView(objArea, plot)) {
            const textPt = makeTextLocationComposite(plot, textLoc, fontSize,
                            objArea.width * plot.zoomFactor,
                            objArea.height * plot.zoomFactor,
                            objArea.center,
                            lineWidth);
            if (textPt) {
                drawText(drawObj, drawTextAry, plot, textPt, drawParams);
            }
        }
    }
}



/**
 * locate text for circle, return the location in screen coordinate
 * @param plot
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
 * @param plot
 * @param textLoc
 * @param fontSize
 * @param inPt0
 * @param inPt1
 * @return {ScreenPt}
 */
function makeTextLocationLine(plot, textLoc, fontSize, inPt0, inPt1) {
    if (!inPt0 || !inPt1) return null;
    let pt0= plot.getScreenCoords(inPt0);
    let pt1= plot.getScreenCoords(inPt1);

    if (!pt0 || !pt1) return null;
    const height= fontHeight(fontSize);

    // pt1 is supposed to be lower on screen
    if (pt0.y > pt1.y) {
        [pt1, pt0] = [pt0, pt1];
    }
    let x = pt1.x+5;
    let y = pt1.y+5;

    if (textLoc===TextLocation.LINE_MID_POINT || textLoc===TextLocation.LINE_MID_POINT_OR_BOTTOM ||
            textLoc===TextLocation.LINE_MID_POINT_OR_TOP) {
        const dist= VisUtil.computeSimpleDistance(pt1,pt0);
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
        default:
            break;
    }

    return makeScreenPt(x,y);
}


/**
 * compute text location for rectangle in screen coordinate
 * @param plot
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
 * @param plot
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

    if (!scrCenterPt || width < 1 || height < 1) return null;

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
 * @param plot
 * @param pts
 * @param apt
 * @returns {*}
 */
export function translateTo(plot, pts, apt) {
    const pt_x = lengthToImagePixel(apt.x, plot, apt.type);
    const pt_y = lengthToImagePixel(apt.y, plot, apt.type);

    return pts.map( (inPt) => {
        const pti= plot.getImageCoords(inPt);
        return makePoint(makeImagePt(pt_x+pti.x, pt_y+pti.y), plot, inPt.type);
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
 * @param plot
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
 * @param plot
 * @param pts
 * @param angle in screen coordinate direction, radian
 * @param wc
 * @returns {*}
 */
export function rotateAround(plot, pts, angle, wc) {
    const center = plot.getImageCoords(wc);

    return pts.map( (p1) => {
        const pti= plot.getImageCoords(p1);
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
 * @param plot
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

    if (validator.isFloat(fontSize.substring(0, fontSize.length-2))) {
        height = parseFloat(fontSize.substring(0, fontSize.length-2)) * 14/10 + 0.5;
    }
    return height;
}

/**
 * calculate the length in screen coordinate
 * @param r
 * @param plot
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
            imageRadius = r/plot.projection.getPixelScaleArcSec();
            break;
        default:
            imageRadius = r;
    }
    return imageRadius;
}

/**
 * calculate the length in screen coordinate
 * @param r
 * @param plot
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
 * @param plot
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
