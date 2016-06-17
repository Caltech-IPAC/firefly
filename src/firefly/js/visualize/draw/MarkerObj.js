
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil';
import {TextLocation, Style, DEFAULT_FONT_SIZE} from './DrawingDef.js';
import {makeImagePt, makeOffsetPt} from '../Point.js';
import ShapeDataObj, {lengthToImagePixel, lengthToScreenPixel, lengthToArcsec} from './ShapeDataObj.js';
import DrawOp from './DrawOp.js';
import CsysConverter from '../CsysConverter.js';
import {has, isNil, get, isEmpty} from 'lodash';
import {defaultMarkerTextLoc} from '../../drawingLayers/MarkerToolUI.jsx';
import BrowserInfo from '../../util/BrowserInfo.js';
import {clone} from '../../util/WebUtil.js';

const HANDLER_BOX = 6;        // handler size (square size in image coordinate)
const MarkerType= new Enum(['Marker', 'HST_FT']); // TODO: more footprint styles to be defined.
const MARKER_DATA_OBJ= 'MarkerDataObj';
const DEF_WIDTH = 1;
export const MARKER_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;

var boxInArcsec = (cc) => lengthToArcsec(HANDLER_BOX, cc, ShapeDataObj.UnitType.SCREEN_PIXEL);

function make(sType) {
    const obj= DrawObj.makeDrawObj();
    obj.pts= [];
    obj.sType= sType;   // default: MarkerType.Marker
    obj.type= MARKER_DATA_OBJ;
    // may contain the following:
        //obj.text= null;
        //obj.fontName = 'helvetica';
        //obj.fontSize = DEFAULT_FONT_SIZE;
        //obj.fontWeight = 'normal';
        //obj.fontStyle = 'normal';
        //obj.width= null;
        //obj.height= null;
        //obj.style=Style.STANDARD;
        //obj.sType = MarkerType.Marker
        //obj.unitType = UnitType.PIXEL;
        //obj.includeHandler = true|false
        //obj.handlerIndex // handler starting index
        //obj.textLoc= TextLocation.CIRCLE_SE
        //obj.textOffset= null;   // offsetScreenPt
        //obj.drawObjAry= array of ShapeDataObj
    return obj;
}

var getWorldOrImage = (pt, cc) => (cc.projection.isSpecified() ? cc.getWorldCoords(pt) : cc.getImageCoords(pt));

/**
 * make drawObj for Marker with handlers or not. Marker is defined as a circle with 4 handlers at the corners
 * @param centerPt
 * @param width  image coordinate
 * @param height
 * @param isHandler
 * @param plot
 * @param text
 * @param textLoc
 * @param unitType
 * @returns {*}
 */
export function makeMarker(centerPt, width, height, isHandler, plot, text, textLoc, unitType = ShapeDataObj.UnitType.IMAGE_PIXEL) {
    var cc = CsysConverter.make(plot);
    var w = lengthToArcsec(width, cc, unitType);
    var h = lengthToArcsec(height, cc, unitType);
    var worldUnit = ShapeDataObj.UnitType.ARCSEC;
    var markerC = getWorldOrImage(centerPt, cc);
    var dObj = clone(make(MarkerType.Marker), {pts: [markerC], width: w, height: h, unitType: worldUnit});
    var retval;
    var radius = lengthToArcsec(Math.min(width, height)/2, cc, unitType);

    var mainCircle = ShapeDataObj.makeCircleWithRadius(markerC, radius, worldUnit);
    var textProps = {text: (isNil(text) ? '' : text),
                     textLoc: (isNil(textLoc) ? defaultMarkerTextLoc : textLoc)};

    dObj = clone(dObj, textProps);
    mainCircle = clone(mainCircle, textProps);
    retval = [mainCircle];    // circle is the first element in the marker's drawobj array

    dObj.includeHandler = isHandler; // start index of handler object
    dObj.handlerIndex = 1;


    var corners = [[-1, 1], [1, 1], [1, -1], [-1, -1]];
    var imgPt = cc.getImageCoords(centerPt);
    var nW = lengthToImagePixel(width/2, cc, unitType);
    var nH = lengthToImagePixel(height/2, cc, unitType);

    retval = corners.reduce((prev, coord) => {
        var x = imgPt.x + coord[0] * nW;
        var y = imgPt.y + coord[1] * nH;

        var handlerCenter = getWorldOrImage(makeImagePt(x, y), cc);
        //var s = lengthToArcsec(HANDLER_BOX, cc, ShapeDataObj.UnitType.IMAGE_PIXEL);
        var s = boxInArcsec(cc);
        var handlerBox = ShapeDataObj.makeRectangleByCenter(handlerCenter, s, s, worldUnit,
                                                            0.0, ShapeDataObj.UnitType.ARCSEC, false);
        prev.push(handlerBox);
        return prev;
    }, retval);

    dObj.drawObjAry = retval;
    return dObj;
}


function makeDrawParams(drawObj,def={}) {
    var color= DrawUtil.getColor(drawObj.color,def.color);
    var style= drawObj.style || def.style || Style.STANDARD;
    var lineWidth= drawObj.lineWidth || def.lineWidth || DEF_WIDTH;
    var textLoc= drawObj.textLoc || def.textLoc || TextLocation.DEFAULT;
    var unitType= drawObj.unitType || def.unitType || ShapeDataObj.UnitType.PIXEL;
    var fontName= drawObj.fontName || def.fontName || 'helvetica';
    var fontSize= drawObj.fontSize || def.fontSize || DEFAULT_FONT_SIZE;
    var fontWeight= drawObj.fontWeight || def.fontWeight || 'normal';
    var fontStyle= drawObj.fontStyle || def.fontStyle || 'normal';
    return {
        color,
        style,
        lineWidth,
        textLoc,
        unitType,
        fontName,
        fontSize,
        fontWeight,
        fontStyle
    };
}


var draw=  {

    usePathOptimization(drawObj,def) {
        var dp= makeDrawParams(drawObj,def);
        return dp==Style.STANDARD && (drawObj.sType==MarkerType.Marker);
    },

    getCenterPt(drawObj) {
        return drawObj.pts[0];
    },

    //pt in screen coordinate
    getScreenDist(drawObj,plot, pt) {
        var dist = -1;
        var testPt = plot.getScreenCoords(drawObj.pts[0]);
        var distToPt = (x1, y1) => {
            var dx = x1-pt.x;
            var dy = y1-pt.y;
            return Math.sqrt((dx * dx) + (dy * dy));
        };

        if (testPt) {
            if (drawObj.sType === ShapeDataObj.ShapeType.Rectangle) {
                var w = lengthToScreenPixel(drawObj.width/2, plot, drawObj.unitType);
                var h = lengthToScreenPixel(drawObj.height/2, plot, drawObj.unitType);
                var x1 = testPt.x - w;
                var x2 = testPt.x + w;
                var y1 = testPt.y - h;
                var y2 = testPt.y + h;

                var bx = (pt.x >= x1 && pt.x <= x2) ? pt.x : ((pt.x < x1) ? x1 : x2);
                var by = (pt.y >= y1 && pt.y <= y2) ? pt.y : ((pt.y < y1) ? y1 : y2);

                dist = distToPt(bx, by);
            } else if (drawObj.sType === ShapeDataObj.ShapeType.Circle) {
                var r = lengthToScreenPixel(drawObj.radius, plot, drawObj.unitType);

                dist = distToPt(testPt.x, testPt.y);
                dist = dist > r ? dist - r : 0;
            }
        }
        return dist;
    },

    draw(drawObj,ctx,drawTextAry,plot,def,vpPtM,onlyAddToPath) {
        drawMarkerObject(drawObj,ctx,drawTextAry,plot,def,vpPtM, onlyAddToPath);
    },

    toRegion(drawObj,plot, def) {
        return toMarkerRegion(drawObj,plot,def);
    },

    translateTo(drawObj, plot, apt) {
        return translateMarker(plot,drawObj,apt);
    },

    rotateAround(drawObj, plot, angle, worldPt) {
        return rotateMakerAround(plot,drawObj,angle,worldPt);
    },

    makeHighlight(drawObj, plot, def) {
        return null;
    },

    isScreenPointInside(screenPt, drawObj, plot, def) {
        return findClosestIndex(screenPt, get(drawObj, 'drawObjAry'), plot);  // >=0, if inside <0: not inside
    }
};

export default {
    make,draw, makeMarker, MARKER_DATA_OBJ
};


/**
 * horizontal distance between the screentPt and the path connecting the handle center and the marker center
 * @param handlePt handle center
 * @param centerPt marker center
 * @param screenPt screen pt
 * @param cc
 * @returns {number}
 */
function distToPathToCenter(handlePt, centerPt, screenPt, cc) {
    var markerC = cc.getScreenCoords(centerPt);
    var handleC = cc.getScreenCoords(handlePt);
    var slopeY = markerC.y - handleC.y;
    var slopeX = markerC.x - handleC.x;
    var dx = screenPt.x - handleC.x;
    var dist = MARKER_DISTANCE;

    if ((screenPt.y > markerC.y && screenPt.y <= handleC.y) ||
        (screenPt.y < markerC.y && screenPt.y >= handleC.y)) {
        var targetY = (slopeY * (dx) / slopeX) + handleC.y;

        dist = Math.abs(targetY - screenPt.y);
    }
    return dist;
}

/**
 * find the drawObj inside the marker which is closest to the given screenPt
 * @param screenPt
 * @param drawObj
 * @param cc
 * @returns {*}
 */
export function findClosestIndex(screenPt, drawObj, cc) {
    var distance = MARKER_DISTANCE;
    if (!drawObj || !has(drawObj, 'drawObjAry')) {
        return -1;
    }

    return drawObj.drawObjAry.reduce((prev, oneObj, index) => {
        var dist = draw.getScreenDist(oneObj, cc, screenPt);

        if (dist >= 0 && dist < distance) {
            distance = dist;
            prev = index;
        }
        return prev;
    }, -1);
}


/**
 * draw the object which contains drawObj array
 * @param drawObj
 * @param ctx
 * @param drawTextAry
 * @param plot
 * @param def
 * @param vpPtM
 * @param onlyAddToPath
 */
function drawMarkerObject(drawObj, ctx, drawTextAry, plot, def, vpPtM, onlyAddToPath) {
    var {drawObjAry}= drawObj;
    if (!drawObjAry) return;

    markerTextOffset(drawObj, plot);
    var dObjs = (drawObj.includeHandler) ? drawObjAry : drawObjAry.slice(0, drawObj.handlerIndex);

    // draw the child drawObj
    if (dObjs) {
        dObjs.forEach( (oneDrawObj) => DrawOp.draw(oneDrawObj, ctx, drawTextAry, plot, def, vpPtM, onlyAddToPath));
    }
}

/**
 * translate the marker to location apt
 * @param plot
 * @param drawObj
 * @param apt
 * @returns {*} an array of translated objects contained in drawObj
 */
function translateMarker(plot,drawObj,apt) {
    var centerPt = plot.getImageCoords(drawObj.pts[0]);
    var pt = plot.getImageCoords(apt);
    var dx = pt.x - centerPt.x;
    var dy = pt.y - centerPt.y;

    drawObj.pts = getWorldOrImage(apt, plot);

    if (has(drawObj, 'drawObjAry')) {
        var dAry =  drawObj.drawObjAry.map((oneObj) => {
            var {pts} = oneObj;
            var newPts = pts&&pts.map( (onePt) => {
                var ptImg = plot.getImageCoords(onePt);
                return getWorldOrImage(makeImagePt(ptImg.x + dx, ptImg.y + dy), plot);
            });
            return Object.assign({}, oneObj, {pts: newPts});
        });
        drawObj.drawObjAry = dAry;
        return dAry;
    }
    return null;
}

/**
 * rotate a marker around worldPt by angle on image coordinate
 * @param plot
 * @param drawObj
 * @param angle, in arcsec unit
 * @param worldPt
 * @returns {*} a array of rotated objects contained in drawObj
 */
function rotateMakerAround(plot,drawObj,angle,worldPt) {
    var worldImg = plot.getImageCoords(worldPt);
    var drawObjPt = plot.getImageCoords(drawObj.pts[0]);

    var rotateAroundPt = (imgPt) => {   // rotate around given worldPt on immage coordinate
        var x1 = imgPt.x - worldImg.x;
        var y1 = imgPt.y - worldImg.y;
        var x2 = x1 * Math.cos(angle) - y1 * Math.sin(angle) + worldImg.x;
        var y2 = x1 * Math.sin(angle) + y1 * Math.cos(angle) + worldImg.y;

        return getWorldOrImage(makeImagePt(x2, y2), plot);
    };

    var newPt = rotateAroundPt(drawObjPt);

    // regenerate 4 handlers on the corners of the rectangular coverage after rotation
    var handlers = drawObj.drawObjAry.slice(drawObj.handlerIndex);
    var rotatedCorners = handlers.map( (oneCorner) => {    // corners after rotation
          return rotateAroundPt(plot.getImageCoords(oneCorner.pts[0]));
    });

    // rectangular coverage after rotation in image coordinate
    var {min_x, min_y, max_x, max_y} = rotatedCorners.reduce((prev, corner) => {
        if (!has(prev, 'min_x') || corner.x < prev.min_x ) {
            prev.min_x = corner.x;
        }
        if (!has(prev, 'max_x') || corner.x > prev.max_x ) {
            prev.max_x = corner.x;
        }
        if (!has(prev, 'min_y') || corner.y < prev.min_y ) {
            prev.min_y = corner.y;
        }
        if (!has(prev, 'max_y') || corner.y > prev.max_y ) {
            prev.max_y = corner.y;
        }
        return prev;
    }, {});

    // create new handlers at the new corners
    var corners = [[min_x, max_y], [max_x, max_y], [max_x, min_y], [min_x, min_y]];   // corners of rectangular coverage'
    var s = boxInArcsec(plot);
    handlers = corners.map( (corner) => {
        return ShapeDataObj.makeRectangleByCenter(getWorldOrImage(makeImagePt(corner[0], corner[1]), plot),
                                                s, s, ShapeDataObj.UnitType.ARCSEC, 0.0, ShapeDataObj.UnitType.ARCSEC, false);
    });

    drawObj.pts = [newPt];

    // rotate each shape drawObj contained inside
    if (has(drawObj, 'drawObjAry')) {
        var dAry =  drawObj.drawObjAry.slice(0, drawObj.handlerIndex);

        dAry = dAry.map((oneObj) => {
            var {pts} = oneObj;                             // rotate pts around worldPt
            var newPts = pts && pts.map((onePt) => {
                    var ptImg = plot.getImageCoords(onePt);
                    return rotateAroundPt(ptImg);
                });
            var newObj = Object.assign({}, oneObj, {pts: newPts});
            return updateShapeAngle(newObj, angle);               // update angle for the shape which has angle property
        });

        drawObj.drawObjAry = [...dAry,...handlers];
        return drawObj.drawObjAry;
    }
    return null;
}

function updateShapeAngle(drawObj, angle) {
    if (drawObj.sType === ShapeDataObj.ShapeType.Rectangle ||
        drawObj.sType === ShapeDataObj.ShapeType.Ellipse) {
        drawObj.angle = angle;
    }
    return drawObj;
}

/**
 * update the text attached to the marker object
 * @param drawObj
 * @param text
 * @param textLoc
 * @returns {*}
 */
export function updateMarkerDrawObjText(drawObj, text, textLoc) {
    if (!has(drawObj, 'drawObjAry')) return null;

    var mainIndex = drawObj.drawObjAry.findIndex( (dObj) => (dObj.sType === ShapeDataObj.ShapeType.Circle));
    var textInfo = Object.assign({}, (!isNil(text))&&{text}, textLoc&&{textLoc});

    if ( mainIndex >= 0 && (text || textLoc )) {
        var newMain = Object.assign({}, drawObj.drawObjAry[mainIndex], textInfo);
        var newDrawObj = Object.assign({}, drawObj, textInfo);

        newDrawObj.drawObjAry[mainIndex] = newMain;

        return newDrawObj;
    }
    return null;
}

/**
 * add text offset to marker object
 * @param drawObj
 * @param cc
 */
function markerTextOffset(drawObj, cc) {
    var {textLoc = TextLocation.DEFAULT, drawObjAry} = drawObj;
    var mainObj = (drawObjAry) ? drawObjAry[0] : null;
    var dy = 0;
    var off = lengthToScreenPixel(HANDLER_BOX/2, cc, ShapeDataObj.UnitType.PIXEL);

    if (mainObj) {
        if (textLoc === TextLocation.CIRCLE_NE || textLoc === TextLocation.CIRCLE_NW) {
            dy = -off;
        } else if (textLoc === TextLocation.CIRCLE_SE || textLoc === TextLocation.CIRCLE_SW) {
            dy = +off;
        }
        var textOff = makeOffsetPt(0, dy);

        mainObj.textOffset = textOff;
        drawObj.textOffset = textOff;
    }
}


/**
 * generate region description for marker
 * @param drawObj
 * @param plot
 * @param def
 * @returns {*}
 */
function toMarkerRegion(drawObj,plot, def) {
    if (!has(drawObj, 'drawObjAry')) return [];

    var dObjs = (drawObj.includeHandler) ? drawObj.drawObjAry : drawObj.drawObjAry.slice(0, drawObj.handlerIndex);

    return dObjs.reduce( (prev, dObj) => {
        var regList = DrawOp.toRegion(dObj, plot, def);

        if (!isEmpty(regList)) {
            prev.push(...regList);
        }
        return prev;
    }, []);
}