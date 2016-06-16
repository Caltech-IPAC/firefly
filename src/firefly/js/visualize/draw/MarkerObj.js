
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil';
import {TextLocation, Style, DEFAULT_FONT_SIZE} from './DrawingDef.js';
import {makeImagePt, makeOffsetPt} from '../Point.js';
import ShapeDataObj from './ShapeDataObj.js';
import DrawOp from './DrawOp.js';
import CsysConverter from '../CsysConverter.js';
import {has, isNil, get, isEmpty} from 'lodash';
import {defaultMarkerTextLoc} from '../../drawingLayers/MarkerToolUI.jsx';
import BrowserInfo from '../../util/BrowserInfo.js';

const HANDLER_BOX = 4;        // handler size (square size in screen coordinate)
const MarkerType= new Enum(['Marker', 'HST_FT']); // TODO: more footprint styles to be defined.
const MARKER_DATA_OBJ= 'MarkerDataObj';
const DEF_WIDTH = 1;
export const MARKER_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;

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
        //obj.textLoc= TextLocation.CIRCLE_SE
        //obj.textOffset= null;   // offsetScreenPt
        //obj.drawObjAry= array of ShapeDataObj
    return obj;
}

var getWorldOrImage = (pt, cc) => (cc.projection.isSpecified() ? cc.getWorldCoords(pt) : cc.getImageCoords(pt));

/**
 * make drawObj for Marker with handlers or not. Marker is defined as a circle with 4 handlers at the corners
 * @param centerPt
 * @param width
 * @param height
 * @param isHandler
 * @param plot
 * @param text
 * @param textLoc
 * @param unitType
 * @returns {*}
 */
export function makeMarker(centerPt, width, height, isHandler, plot, text, textLoc, unitType = ShapeDataObj.UnitType.PIXEL) {
    var dObj = Object.assign(make(MarkerType.Marker), {pts: [centerPt], width, height, unitType});
    var retval;
    var radius = Math.min(width, height)/2;
    var mainCircle = ShapeDataObj.makeCircleWithRadius(centerPt, radius, unitType);
    var textProps = {text: (isNil(text) ? '' : text),
                     textLoc: (isNil(textLoc) ? defaultMarkerTextLoc : textLoc)};

    dObj = Object.assign(dObj, textProps);
    mainCircle = Object.assign(mainCircle, textProps);
    retval = [mainCircle];    // circle is the first element in the marker's drawobj array

    dObj.includeHandler = isHandler ? 1 : 0; // start index of handler object


    if (isHandler) {
        var cc = CsysConverter.make(plot);
        var corners = [[-1, 1], [1, 1], [-1, -1], [1, -1]];
        var imgPt = cc.getImageCoords(centerPt);
        var nW = (width)/(2 * cc.zoomFactor);
        var nH = (height)/(2 * cc.zoomFactor);

        retval = corners.reduce((prev, coord) => {
            var x = imgPt.x + coord[0] * nW;
            var y = imgPt.y + coord[1] * nH;

            var handlerCenter = getWorldOrImage(makeImagePt(x, y), cc);
            var handlerBox = ShapeDataObj.makeRectangleByCenter(handlerCenter, HANDLER_BOX, HANDLER_BOX,
                                 ShapeDataObj.UnitType.PIXEL, 0.0, ShapeDataObj.UnitType.ARCSEC, false);
            prev.push(handlerBox);
            return prev;
        }, retval);
    }

    dObj.drawObjAry = retval;
    markerTextOffset(dObj);
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
                var x1 = testPt.x - drawObj.width / 2;
                var x2 = testPt.x + drawObj.width / 2;
                var y1 = testPt.y - drawObj.height / 2;
                var y2 = testPt.y + drawObj.height / 2;

                var bx = (pt.x >= x1 && pt.x <= x2) ? pt.x : ((pt.x < x1) ? x1 : x2);
                var by = (pt.y >= y1 && pt.y <= y2) ? pt.y : ((pt.y < y1) ? y1 : y2);

                dist = distToPt(bx, by);
            } else if (drawObj.sType === ShapeDataObj.ShapeType.Circle) {
                dist = distToPt(testPt.x, testPt.y);
                dist = dist > drawObj.radius ? dist - drawObj.radius : 0;
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
 * find the drawObj inside the marker which is closest to the given screenPt
 * @param screenPt
 * @param drawObj
 * @param cc
 * @returns {*}
 */
export function findClosestIndex(screenPt, drawObj, cc) {
    var distance = MARKER_DISTANCE;
    if (!has(drawObj, 'drawObjAry')) {
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

    // draw the child drawObj
    if (drawObjAry) {
        drawObjAry.forEach( (oneDrawObj) => DrawOp.draw(oneDrawObj, ctx, drawTextAry, plot, def, vpPtM, onlyAddToPath));
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
 * @param angle
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
    var handlers = null;

    // regenerate 4 handlers on the corners of the rectangular coverage after rotation
    if (drawObj.includeHandler > 0) {
        var w = drawObj.width/(2*plot.zoomFactor);    // half of width in image coordinate
        var h = drawObj.height/(2*plot.zoomFactor);   // half of height in image coordinate
        var corners = [[-1, 1], [1, 1], [1, -1], [-1, -1]];
        var rotatedCorners = corners.map( (oneCorner) => {    // corners after rotation
            var x = oneCorner[0] * w + drawObjPt.x;   // 4 corners before rotation on image coordinate
            var y = oneCorner[1] * h + drawObjPt.y;

            return rotateAroundPt(makeImagePt(x, y));
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

        corners = [[min_x, max_y], [max_y, max_y], [max_x, min_y], [min_x, min_y]];   // corners of rectangular coverage
        handlers = corners.map( (corner) => {
            return ShapeDataObj.makeRectangleByCenter(getWorldOrImage(makeImagePt(corner[0], corner[1]), plot),
                HANDLER_BOX, HANDLER_BOX, ShapeDataObj.UnitType.PIXEL, 0.0, ShapeDataObj.UnitType.ARCSEC, false);
        });
    }

    drawObj.pts = [newPt];

    // rotate each shape drawObj contained inside
    if (has(drawObj, 'drawObjAry')) {
        var dAry = drawObj.includeHandler > 0 ? drawObj.drawObjAry.slice(drawObj.includeHandler): drawObj.drawObjAry;

        dAry = dAry.map((oneObj) => {
            var {pts} = oneObj;                             // rotate pts around worldPt
            var newPts = pts && pts.map((onePt) => {
                    var ptImg = plot.getImageCoords(onePt);
                    return rotateAroundPt(ptImg);
                });
            var newObj = Object.assign({}, oneObj, {pts: newPts});
            updateShapeAngle(newObj, angle);               // update angle for the shape which has angle property
            return newObj;
        });

        drawObj.drawObjAry = handlers ? [...dAry,...handlers] : dAry;
        return drawObj.drawObjAry;
    }
    return null;
}

function updateShapeAngle(drawObj, angle) {
    if (drawObj.sType === ShapeDataObj.ShapeType.Rectangle ||
        drawObj.sType === ShapeDataObj.ShapeType.Ellipse) {
        drawObj.angle = angle;
    }
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
        markerTextOffset(newDrawObj);

        return newDrawObj;
    }
    return null;
}

/**
 * add text offset to marker object
 * @param drawObj
 */
function markerTextOffset(drawObj) {
    var {textLoc = TextLocation.DEFAULT, drawObjAry} = drawObj;
    var mainObj = (drawObjAry) ? drawObjAry[0] : null;
    var dy = 0;

    if (mainObj) {
        if (textLoc === TextLocation.CIRCLE_NE || textLoc === TextLocation.CIRCLE_NW) {
            dy = -HANDLER_BOX/2;
        } else if (textLoc === TextLocation.CIRCLE_SE || textLoc === TextLocation.CIRCLE_SW) {
            dy = +HANDLER_BOX/2;
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

    return drawObj.drawObjAry.reduce( (prev, dObj) => {
        var regList = DrawOp.toRegion(dObj, plot, def);

        if (!isEmpty(regList)) {
            prev.push(...regList);
        }
        return prev;
    }, []);
}