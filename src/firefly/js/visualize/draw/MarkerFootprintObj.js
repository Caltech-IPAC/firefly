
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Point, {makeImagePt, makeScreenPt, makeDevicePt, makeWorldPt, SimplePt} from '../Point.js';
import {CoordinateSys} from '../CoordSys.js';
import ShapeDataObj, {lengthToImagePixel, lengthToScreenPixel,
       lengthToArcsec, makePoint, drawText, makeTextLocationComposite, flipTextLocAroundY} from './ShapeDataObj.js';
import {POINT_DATA_OBJ, getPointDataobjArea, makePointDataObj} from './PointDataObj.js';
import {DrawSymbol} from './DrawSymbol.js';
import {getDrawobjArea, isWithinPolygon, isDrawobjAreaInView} from './ShapeHighlight.js';
import {defaultMarkerTextLoc} from '../../drawingLayers/MarkerToolUI.jsx';
import {defaultFootprintTextLoc} from '../../drawingLayers/FootprintToolUI.jsx';
import {TextLocation, Style, DEFAULT_FONT_SIZE} from './DrawingDef.js';
import {FootprintFactory} from './FootprintFactory.js';
import {convertAngle} from '../VisUtil.js';
import DrawObj from './DrawObj.js';
import DrawOp from './DrawOp.js';
import BrowserInfo from '../../util/BrowserInfo.js';
import {clone} from '../../util/WebUtil.js';
import Enum from 'enum';
import {has, isNil, get, isEmpty, isArray, set} from 'lodash';
import {isHiPS} from '../WebPlot.js';
import {hasWCSProjection} from '../PlotViewUtil';

const HANDLER_BOX = 6;        // handler size (square size in screen coordinate)
const CENTER_BOX = 60;
const CROSS_BOX = 12;
const HANDLE_COLOR = '#DAA520';
const DEF_WIDTH = 1;
export const MARKER_SIZE = 40;      // marker original size in image coordinate (radius of a circle)
export const MARKER_DATA_OBJ= 'MarkerObj';

const DEFAULT_STYLE= Style.STANDARD;
const textLocSeq = [TextLocation.REGION_SE, TextLocation.REGION_SW, TextLocation.REGION_NW, TextLocation.REGION_NE];


export const ROTATE_BOX = 32;    // in screen pixel
export const MARKER_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;
export const MarkerType= new Enum(['Marker', 'Footprint']);
export const ANGLE_UNIT = new Enum(['arcsec', 'arcmin', 'degree', 'radian']);
export const MARKER_HANDLE = new Enum(['outline', 'resize', 'rotate']);
export const OutlineType = new Enum(['original', 'center', 'plotcenter']);

const AllHandle = [MARKER_HANDLE.outline, MARKER_HANDLE.resize, MARKER_HANDLE.rotate];
const AllOutline = [OutlineType.original, OutlineType.center, OutlineType.plotcenter];

export var getWorldOrImage = (pt, cc) => (hasWCSProjection(cc) ?
                                           cc.getWorldCoords(pt, CoordinateSys.EQ_J2000) : cc.getImageCoords(pt));


/**
 * Create a DrawObj for footprint or marker that contains the following:
 *  obj.text= null;
 *  obj.fontName
 *  obj.fontSize
 *  obj.fontWeight
 *  obj.fontStyle
 *  obj.width
 *  obj.height
 *  obj.color
 *  obj.style=Style.STANDARD;
 *  obj.sType = MarkerType.Marker or MarkerType.Footprint
 *  obj.unitType
 *  obj.includeRotate = true|false  if (isRotatable === true), show outlinebox + rotate handle
 *  obj.includeResize = true|false  if (isEditable === true) show outlinebox + resize handle
 *  obj.includeOutline = true|false  show outlinebox
 *  obj.textLoc
 *  obj.textOffset  // offsetScreenPt
 *  obj.pts         // center of footprint or marker, rotation center
 *  obj.drawObjAry   // array of ShapeDataObj it contains
 *  obj.originalOutlineBox  // the 'original' outlinebox, is only recorded if the outline box around the footprint is in view
 *                        // the outlinebox object included in drawObjAry may not be the original (coult be becomes around 'center' or 'plotcenter')
 *  obj.isRotatable  //rotablable
 *  obj.isEditable   //resizable
 *  obj.isMovable    //movable
 *  obj.outlineIndex // handler starting index, outline box object position in drawObjAry
 *  obj.resizeIndex  //resize handler index in drawObjAry
 *  obj.rotateIndex  //rotate handler index in drawObjAry
 *  sequence: outlinebox (handleIndex) => (resizeIndex) => (rotateIndex)
 * @param sType
 * @param style
 * @returns {*}
 */
function make(sType, style) {
    const obj= DrawObj.makeDrawObj();
    obj.sType= sType;   // default: MarkerType.Marker
    obj.type= MARKER_DATA_OBJ;
    obj.style = (!style) ? DEFAULT_STYLE: style;

    return obj;
}

const worldUnit = ShapeDataObj.UnitType.ARCSEC;
const imageUnit = ShapeDataObj.UnitType.IMAGE_PIXEL;
const screenUnit = ShapeDataObj.UnitType.PIXEL;

/**
 * convert 1-D size to either in arcsec or image pixel unit in case no projection is available
 * @param cc
 * @param size
 * @param unitType
 * @param resUnit output unit
 * @returns {{length: *, unit: *}}
 */
export function lengthSizeUnit(cc, size, unitType, resUnit=imageUnit) {
    var len, unit;

    if (hasWCSProjection(cc)) {
        len = lengthToArcsec(size, cc, unitType);
        unit = worldUnit;
    } else if (imageUnit.is(resUnit)) {
        len = lengthToImagePixel(size, cc, unitType);
        unit = imageUnit;
    } else if (screenUnit.is(resUnit)) {
        len = lengthToImagePixel(size, cc, unitType)*cc.zoomFactor;
        unit = screenUnit;
    }
    return {len, unit};
}


/**
 * generate handler box size and unit in either arcsec unit or image pixel unit in case no projection exists.
 * @param cc
 * @param boxSize
 * @param unitType
 * @returns {{size: *, unit: *}}
 */
function boxSizeUnit(cc, boxSize = HANDLER_BOX, unitType = ShapeDataObj.UnitType.PIXEL)  {
    var size;
    var unit;
    var sizeUnit;

    if (isArray(boxSize)) {
        size = [];
        boxSize.forEach( (s) => {
            sizeUnit = lengthSizeUnit(cc, s, unitType);
            size.push(sizeUnit.len);
            unit = sizeUnit.unit;
        });
    } else {
        sizeUnit = lengthSizeUnit(cc, boxSize, unitType);
        size = [sizeUnit.len, sizeUnit.len];
        unit = sizeUnit.unit;
    }

    return {size, unit};
}

const cornersImg = [[-1, 1], [1, 1], [1, -1], [-1, -1]];
const cornerScreen = [[-1, -1], [1, -1], [1, 1], [-1, 1]];

export var setHandleIndex = (dObj) => {
    var idx = dObj.outlineIndex + 1;


    if (has(dObj, 'isEditable')) {
        dObj.resizeIndex = idx;
        idx += 4;
    }
    if (has(dObj, 'isRotatable')) {
        dObj.rotateIndex = idx;
    }
};

/**
 * make drawObj for Marker with handlers or not. Marker is defined as a circle with 4 handlers at the corners
 * outline box is created
 * @param centerPt
 * @param width  image coordinate
 * @param height
 * @param isHandle
 * @param cc
 * @param text
 * @param textLoc
 * @param unitType
 * @returns {*}
 */
export function makeMarker(centerPt, width, height, isHandle, cc, text, textLoc, unitType = ShapeDataObj.UnitType.IMAGE_PIXEL) {
    var {size, unit} = boxSizeUnit(cc, [width, height], unitType);
    var markerC = getWorldOrImage(centerPt, cc);
    var dObj = clone(make(MarkerType.Marker), {pts: [markerC], width: size[0], height: size[1], unitType: unit});
    var radius = lengthSizeUnit(cc, Math.min(width, height)/2, unitType);
    var mainCircle = ShapeDataObj.makeCircleWithRadius(markerC, radius.len, radius.unit);
    var textProps = {text: (isNil(text) ? '' : text),
                     textLoc: (isNil(textLoc) ? defaultMarkerTextLoc : textLoc)};

    mainCircle.isMarker = true;
    dObj = Object.assign(dObj, textProps);

    dObj = Object.assign(dObj, {
        isMovable: true,
        isEditable: true,
        drawObjAry: [mainCircle],
        outlineIndex: 1
    });

    var {isRotate, isResize, isOutline} = isHandle || {};
    var handle = updateHandle(dObj, cc, [MARKER_HANDLE.outline]);

    if (handle) {
        dObj.drawObjAry.push(handle[0]);
    }

    dObj.includeOutline = !!(isOutline);
    dObj.includeResize = !!(get(dObj, 'isEditable') && isResize && isOutline);
    dObj.includeRotate = !!(get(dObj, 'isRotatable') && isRotate && isOutline);
    setHandleIndex(dObj);

    return dObj;
}

function createCrossCenter(centerPt) {
    const centerObj = makePointDataObj(centerPt, CROSS_BOX, DrawSymbol.CROSS);
    centerObj.color = 'red';

    return centerObj;
}


/**
 * make foopprint drawobj, create drawObj on all regions defined
 * outline box is created and included in the drawObjAry always, includeOutline is used to determine if it is drawn
 * @param regions   region defined in one footprint
 * @param centerPt  destination of center point (defaultly mapped from [0, 0] world coordinate)
 * @param isHandle  with rotate handle or not,
 * @param cc
 * @param text		text around the footprint
 * @param textLoc
 * @return footprint drawobj
 */
export function makeFootprint(regions, centerPt, isHandle, cc, text, textLoc) {
    var fpCenter = getWorldOrImage(centerPt, cc);
    var regionDrawObjAry = FootprintFactory.getDrawObjFromOriginalRegion(regions, fpCenter, regions[0].isInstrument, cc);
    var centerObj = createCrossCenter(fpCenter);
    var dObj = clone(make(MarkerType.Footprint), {pts: [makeWorldPt(fpCenter.x, fpCenter.y)]});

    regionDrawObjAry.forEach((obj) => {
        obj.isMarker = true;
    });

    regionDrawObjAry.push(centerObj);


    dObj = Object.assign(dObj, {
        isMovable: true,
        isRotatable: isHandle.isRotate,
        drawObjAry: regionDrawObjAry,
        outlineIndex: regionDrawObjAry.length,
        regions
    });

    var textProps = {text: (isNil(text) ? '' : text),
        textLoc: (isNil(textLoc) ? defaultFootprintTextLoc : textLoc)};

    dObj = Object.assign(dObj, textProps);

    var {isRotate, isResize, isOutline} = isHandle || {};
    var handle = updateHandle(dObj, cc, [MARKER_HANDLE.outline]);

    if (handle) {
        dObj.drawObjAry.push(handle[0]);
    }

    dObj.includeOutline = !!(isOutline);
    dObj.includeResize = !!(get(dObj, 'isEditable') && isResize && isOutline);
    dObj.includeRotate = !!(get(dObj, 'isRotatable') && isRotate && isOutline);

    setHandleIndex(dObj);
    return dObj;
}


function makeDrawParams(drawObj,def={}) {
    var color= def.color || drawObj.color || 'green';
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
        // need more investigation

        if (drawObj.sType === MarkerType.Marker ) {
            var dp = makeDrawParams(drawObj, def);
            return dp === Style.STANDARD && (drawObj.sType === MarkerType.Marker);
        } else {
            return drawObj.lineWidth===1;
        }
    },

    getCenterPt(drawObj) {
        if (drawObj.sType === MarkerType.Marker) return drawObj.pts[0];

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

    //pt in screen coordinate
    getScreenDist(drawObj,plot, pt) {
        return  findClosestIndex(pt, drawObj, plot).distance;
    },

    draw(drawObj,ctx,plot,def,vpPtM,onlyAddToPath) {
        drawMarkerObject(drawObj,ctx,plot,def,vpPtM, onlyAddToPath);
    },

    toRegion(drawObj,plot, def) {
        return toMarkerRegion(drawObj,plot,def);
    },

    translateTo(drawObj, plot, apt) {
        return translateMarker(plot,drawObj,apt);
    },

    // in radian
    rotateAround(drawObj, plot, angle, worldPt) {
        return rotateMarkerAround(drawObj, plot, angle,worldPt);
    },

    makeHighlight(drawObj, plot, def) {
        return null;
    },

    isScreenPointInside(screenPt, drawObj, plot, def) {
        var {index} = findClosestIndex(screenPt, drawObj, plot);

        return (index >= 0);
    }
};

export default {
    make, draw, makeMarker, makeFootprint, MARKER_DATA_OBJ
};


export var getMarkerAngleInRad = (obj) => {
    var {angle = 0.0, angleUnit = ANGLE_UNIT.radian} = obj;

    return convertAngle(angleUnit.key, 'radian', angle);
};

/**
 * collect drawObj contained in footprint or marker
 * @param drawObj
 * @param includeList fine the drawObj (outlineBox, resize handle or rotate handle) in the list
 * @returns {Array}
 */
function collectDrawobjAry(drawObj, includeList = []) {
    var {drawObjAry} = drawObj;
    var retval = [];

    if (!drawObjAry) return retval;

    // collect all drawobj built on original defined region (footprint)
    retval = drawObjAry.reduce ((prev, oneObj, index) => {
        if (index < drawObj.outlineIndex) {
            prev.push(Object.assign({}, oneObj));
        }
        return prev;
    }, []);

    if (includeList.includes(MARKER_HANDLE.outline)) {
        if (drawObjAry.length > drawObj.outlineIndex) {
            retval.push(Object.assign({}, drawObjAry[drawObj.outlineIndex]));
        }
    }

    if (drawObj.includeResize && (includeList.includes(MARKER_HANDLE.resize))) {
        if (drawObjAry.length >= drawObj.resizeIndex + 4) {
            for (let i = 0; i < 4; i++) {
                retval.push(Object.assign({}, drawObjAry[drawObj.resizeIndex + i]));
            }
        }
    }

    if (drawObj.includeRotate && (includeList.includes(MARKER_HANDLE.rotate))) {
        if (drawObjAry.length > drawObj.rotateIndex) {
            retval.push(Object.assign({}, drawObjAry[drawObj.rotateIndex]));
        }
    }

    return retval;
}

/**
 * rotate point around center point by assume two points are in the same coordinate
 * @param pt
 * @param center
 * @param angle
 * @param outType output type of the point
 * @returns {*}
 */
var simpleRotateAroundPt = (pt, center, angle, outType) => {   // rotate around center of same coordinate
    if (!center || !pt) {
        return null;
    } else {
        var x1 = pt.x - center.x;
        var y1 = pt.y - center.y;
        var x2 = x1 * Math.cos(angle) - y1 * Math.sin(angle) + center.x;
        var y2 = x1 * Math.sin(angle) + y1 * Math.cos(angle) + center.y;

        return Object.assign(new SimplePt(x2, y2), {type: outType});
    }
};

/**
 * get rect corners by giving center point and the dimension of the rectangle
 * @param pt
 * @param width
 * @param height
 * @param unitType
 * @param cc
 * @param outUnit
 * @returns {Array}
 */
function getRectCorners(pt, width, height, unitType, cc, outUnit = Point.W_PT) {
    // if centerPt is null, then return corners with null in array
    var centerPt = cc.getImageCoords(pt);
    var nW = lengthToImagePixel(width/2, cc, unitType);
    var nH = lengthToImagePixel(height/2, cc, unitType);

    return cornersImg.map( (coord) => {
        if (!centerPt)  {
            return null;
        }
        var x = centerPt.x + coord[0] * nW;
        var y = centerPt.y + coord[1] * nH;

        var rImgPt = makeImagePt(x, y);
        if (outUnit === Point.W_PT ) {
            return getWorldOrImage(rImgPt, cc);
        } else if (outUnit ===  Point.SPT ) {
            return cc.getScreenCoords(rImgPt);
        } else {
            return rImgPt;
        }
    });
}

/**
 * calculate the footprint overall rectangular area in image coordinate
 * @param rDrawAry
 * @param cc
 * @returns {{width: number, height: number, unitType: *}}
 */
export function getMarkerImageSize(rDrawAry, cc) {
    const mArea = {};


    const badArea = rDrawAry.findIndex( (oneDrawObj) => {
        if (get(oneDrawObj, 'isMarker', false)) {
            const area = getObjArea(oneDrawObj, cc);  // in image coordinate
            if (!area) {
                return true;    // some marker is out of view
            }
            const {upperLeft, width, height} = area;
            const [min_x, min_y, max_x, max_y] = [upperLeft.x, upperLeft.y - height, upperLeft.x + width, upperLeft.y];

            if ((!has(mArea, 'min_x')) || (min_x < mArea.min_x)) {
                mArea.min_x = min_x;
            }
            if ((!has(mArea, 'min_y')) || (min_y < mArea.min_y)) {
                mArea.min_y = min_y;
            }
            if ((!has(mArea, 'max_x')) || (max_x > mArea.max_x)) {
                mArea.max_x = max_x;
            }
            if ((!has(mArea, 'max_y')) || (max_y > mArea.max_y)) {
                mArea.max_y = max_y;
            }
        }
        return false;
    });

    if (badArea >= 0) {
        return null;
    }    // in case all drawObj is out of plot area

    var width = lengthSizeUnit(cc, mArea.max_x - mArea.min_x, ShapeDataObj.UnitType.IMAGE_PIXEL);
    var height = lengthSizeUnit(cc, mArea.max_y - mArea.min_y, ShapeDataObj.UnitType.IMAGE_PIXEL);
    return {width: width.len, height: height.len, unitType: width.unit,
            centerPt: getWorldOrImage(makeImagePt((mArea.min_x + mArea.max_x)/2, (mArea.min_y + mArea.max_y)/2), cc)};
}

/**
 * get the rectangular area of any drawobj (pointdata or shapadata), in image coordinate
 * @param obj
 * @param cc
 * @param onlyCheckCenter only check if area center exists or not
 * @returns {{upperLeft: *, width: *, height: *}}
 */
function getObjArea(obj, cc, onlyCheckCenter = false) {
    const area= (obj.type === ShapeDataObj.SHAPE_DATA_OBJ) ? getDrawobjArea(obj, cc): getPointDataobjArea(obj, cc);
    if (!area) return null;

    var {upperLeft, width, height, centerPt, center} = area || {};

    if (center) {
        centerPt = center;
    }

    if (!centerPt ||
        (onlyCheckCenter && !cc.pointInView(centerPt) || (!onlyCheckCenter && !isDrawobjAreaInView(cc, null, area)))) {

    /*
        (!onlyCheckCenter &&
            (!cc.pointInView(centerPt) || !isDrawobjAreaInView(cc, null, area)))) {    // in case the cover area is out of plot area
    */
        return null;
    }

    // convert dimension on image coordinate
    if (obj.type === POINT_DATA_OBJ) {
        upperLeft = cc.getImageCoords( makeScreenPt(centerPt.x - width/2, centerPt.y - height/2));
        width = lengthToImagePixel(width, cc, ShapeDataObj.UnitType.PIXEL);
        height = lengthToImagePixel(height, cc, ShapeDataObj.UnitType.PIXEL);
    }

    return {upperLeft, width, height, centerPt, unitType: imageUnit};
}

/**
 * update the rotAngle set in drawobj's renderOptions
 * @param rotAngle
 * @param obj
 */
var updateHandleRotAngle = (rotAngle, obj) => {
    if (rotAngle === 0.0) return;
    var angle = get(obj, 'renderOptions.rotAngle', 0.0);
    angle += rotAngle;

    set(obj, 'renderOptions.rotAngle', angle);
};

/**
 * count how many corners of the rectangle are in viewport
 * @param drawObj rectangular drawobj
 * @param cc
 * @returns {*}
 */
var rectCornerInView = (drawObj, cc) => {
    var {pts} = drawObj;
    var corners = getRectCorners(pts[0], drawObj.width, drawObj.height, drawObj.unitType, cc);

    var rotAngle = get(drawObj, 'renderOptions.rotAngle', 0.0);

    return corners.reduce( (prev, corner) =>
    {
        var rCorner = corner ? simpleRotateAroundPt(cc.getImageCoords(corner), cc.getImageCoords(pts[0]), -rotAngle, Point.IM_PT) : null;
        if (rCorner && cc.pointInView(rCorner)) {
            prev++;
        }
        return prev;
    }, 0);
};

/**
 * regenerate the footprint elements in terms of the current footprint center and the original regions
 * @param drawObj
 * @param cc CysConverter object
 * @returns {*}
 */
function getOriginalFPDrawObj(drawObj, cc){
    const drawObjAry = FootprintFactory.getDrawObjFromOriginalRegion(drawObj.regions, drawObj.pts[0],
        drawObj.regions[0].isInstrument, cc);

    drawObjAry.forEach((obj) => obj.isMarker = true);
    return drawObjAry;
}

/**
 * create outline box based on the drawObjs with some rotation angle
 * @param drawObj
 * @param cc
 * @param checkOutline
 * @returns {*}
 */
function remakeOutlineBox(drawObj, cc, checkOutline = AllOutline) {
    var {originalOutlineBox:tryOutline} = drawObj;
    var angle = getMarkerAngleInRad(drawObj);

    if (!cc.pointInView(drawObj.pts[0])) {
        return null;
    }

    // try original outline box (the current outline is not 'original)'
    if (checkOutline.includes(OutlineType.original)) {
        if (!tryOutline) {
            if (drawObj.sType === MarkerType.Marker) {               // for marker case (assume no rotation)
                var {radius = 0.0, unitType = ShapeDataObj.ShapeType.ARCSEC} = get(drawObj, ['drawObjAry', '0']) || {};

                tryOutline =  ShapeDataObj.makeRectangleByCenter(drawObj.pts[0], radius*2, radius*2, unitType,
                                                                 0.0, ShapeDataObj.UnitType.ARCSEC, false);
            } else if (has(drawObj,'regions')) {        // for footprint case
                const drawObjAry = getOriginalFPDrawObj(drawObj, cc);

                // finding the center of all markers and rotate the center around the center of the footprint object
                var {width, height, centerPt, unitType:ut} = getMarkerImageSize(drawObjAry, cc) || {};
                if (!centerPt) {
                    tryOutline = null;
                } else {
                    var rCenterPt = simpleRotateAroundPt(cc.getImageCoords(centerPt), cc.getImageCoords(drawObj.pts[0]),
                                                         -angle, Point.IM_PT);

                    if (!cc.pointInView(rCenterPt)) {
                        tryOutline = null;
                    } else {
                        tryOutline = ShapeDataObj.makeRectangleByCenter(getWorldOrImage(rCenterPt, cc), width, height, ut,
                                                                        0.0, ShapeDataObj.UnitType.ARCSEC, false);
                    }
                }
            }
            if (tryOutline) {
                tryOutline = Object.assign(tryOutline, {
                                                outlineType: OutlineType.original,
                                                color: HANDLE_COLOR,
                                                renderOptions: {
                                                    lineDash: [8, 5, 2, 5],
                                                    rotAngle: angle
                                                }
                             });
                if (get(drawObj, 'originalOutlineBox', null)) {
                    drawObj.originalOutlineBox = tryOutline;
                }
            }
        }
        if (tryOutline && rectCornerInView(tryOutline, cc) > 0) {
            //drawObj.originalOutlineBox = null; ???
            return tryOutline;
        }
    }
    const boxSize = getCenterBoxSize(drawObj.sType);

    // try center outlinebox
    if (checkOutline.includes(OutlineType.center)) {

        tryOutline = createOutlineBox(drawObj.pts[0], boxSize, boxSize, ShapeDataObj.UnitType.PIXEL, cc, angle);
        if (tryOutline) {
            return clone(tryOutline, {outlineType: OutlineType.center});
        }
    }
    // try plotcenter outlinebox
    if (checkOutline.includes(OutlineType.plotcenter)) {
        var vCenter = makeDevicePt(cc.viewDim.width / 2, cc.viewDim.height / 2);

        tryOutline = createOutlineBox(vCenter, boxSize, boxSize, ShapeDataObj.UnitType.PIXEL, cc, angle);
        if (tryOutline) {
            return Object.assign(tryOutline, {outlineType: OutlineType.plotcenter});
        }
    }
    return null;
}

/**
 * update the resize handle, rotate handle, and outline box,  whichever is applied
 * @param drawObj
 * @param cc
 * @param handleList
 * @param upgradeOutline check if upgrade existing outline box from 'center' to 'original' or from 'plotcenter' to
 *                        'center' or 'original' if they are visible in viewport
 * @returns {*}
 */
function updateHandle(drawObj, cc, handleList = AllHandle, upgradeOutline = false) {
    var {pts} = drawObj;
    var retval = [];    // outlinebox, resize and rotate

    if (!pts || !pts[0]) return retval;
    var outlineBox;

    // get existing outline box and check if it is in view
    if (!handleList.includes(MARKER_HANDLE.outline)) {
        outlineBox = drawObj.drawObjAry.length > drawObj.outlineIndex ? drawObj.drawObjAry[drawObj.outlineIndex] : null;

        if (outlineBox) {
            var cornersInView = rectCornerInView(outlineBox, cc);
            var checkOutline;  // outline candidate to be created in order

            // if the outline box is not in view, try to get a new outline box
            // if the outline is around the footprint center or plot center, check if the original or the center outline box exist
            if (cornersInView === 0) {    // if not in view, get a new outline box
                if (outlineBox.outlineType === OutlineType.original) {
                    drawObj.originalOutlineBox = Object.assign({}, outlineBox); // save the original outline box
                }

                // remake outlinebox from some candidates in case the original outline box is out of display range
                checkOutline = AllOutline.reduce ( (prev, outl) => {
                    if (outl !== outlineBox.outlineType || outlineBox.outlineType === OutlineType.plotcenter) {
                        prev.push(outl);
                    }
                    return prev;
                }, []);

                outlineBox = remakeOutlineBox(drawObj, cc, checkOutline);
            } else if (upgradeOutline && outlineBox.outlineType !== OutlineType.original) {
                checkOutline = (outlineBox.outlineType === OutlineType.center) ?
                               [OutlineType.original] : [OutlineType.original, OutlineType.center];

                var tryOutline = remakeOutlineBox(drawObj, cc, checkOutline);

                // keep existing outlinebox in case the upgrade fails
                if (tryOutline)  outlineBox = tryOutline;
            }
        } else {
            outlineBox = remakeOutlineBox(drawObj, cc);
        }
    } else {  // do from the scratch
        var {width, height, centerPt, unitType} = getMarkerImageSize(collectDrawobjAry(drawObj), cc) || {};
        var angle = getMarkerAngleInRad(drawObj);

         outlineBox = createOutlineBoxAllSteps(pts[0], centerPt, width, height, unitType, cc, drawObj.sType, angle);
    }

    if (!outlineBox) return retval;
    retval.push(outlineBox);

    var rotAngle = getMarkerAngleInRad(drawObj);

    // add resize handles,
    if ((get(drawObj, 'includeResize') || get(drawObj, 'isEditable')) && handleList.includes(MARKER_HANDLE.resize)) {
        //&& (outlineBox.outlineType === OutlineType.original)) {
        createResizeHandle(outlineBox, cc, rotAngle).forEach((r) => retval.push(r));
     }

    // add rotate handle, plotcenter outline has no rotate handle
    if ((get(drawObj, 'includeRotate') || get(drawObj, 'isRotatable')) &&
        handleList.includes(MARKER_HANDLE.rotate)) {
        var rotateHandle = createRotateHandle(outlineBox, cc, rotAngle);

        if (rotateHandle) {
            retval.push(rotateHandle);
        }
    }

    return retval;
}

/**
 * create outline by first trying to create the outline around all object, or around the center if the object
 * is oversized, or around the plot center if no corner is seen
 * stop creating the outline box around the center or the plot center if 'stopAt' is specified
 * @param fpCenter
 * @param outlineCenter
 * @param width
 * @param height
 * @param unitType
 * @param cc
 * @param sType
 * @param angle angle rotate on screen domain. reverse the angle on image domain
 * @param stopAt
 * @returns {*}
 */
function createOutlineBoxAllSteps(fpCenter, outlineCenter, width, height, unitType, cc, sType, angle = 0.0, stopAt) {

    if (angle !== 0.0 && outlineCenter) {  // rotate the center around the footprint center
        var oCenter  = simpleRotateAroundPt(cc.getImageCoords(outlineCenter),
                                            cc.getImageCoords(fpCenter), -angle, Point.IM_PT);
        outlineCenter = getWorldOrImage(oCenter, cc);
    }

    var outlineBox = outlineCenter ? createOutlineBox(outlineCenter, width, height, unitType, cc, angle) : null;
    const sBox = getCenterBoxSize(sType);

    if (outlineBox) {   // outline box around the footprint is visible
        outlineBox.outlineType = OutlineType.original;
    } else if (!stopAt || stopAt !== OutlineType.center) { // try outline box around center and plot center
        outlineBox = createOutlineBox(fpCenter, sBox, sBox, ShapeDataObj.UnitType.PIXEL, cc, angle);
        if (outlineBox) {
            outlineBox.outlineType = OutlineType.center;
        } else if (!stopAt || stopAt !== OutlineType.plotcenter) {
            var vCenter = makeDevicePt(cc.viewDim.width / 2, cc.viewDim.height / 2);

            outlineBox = createOutlineBox(vCenter, sBox, sBox, ShapeDataObj.UnitType.PIXEL, cc, angle);
            if (outlineBox) outlineBox.outlineType = OutlineType.plotcenter;
        }
    }
    return outlineBox;
}


/**
 * create resize handle on the corners of outline box
 * @param outlineBox
 * @param cc
 * @param rotAngle
 * @returns {*}
 */
function createResizeHandle(outlineBox, cc, rotAngle) {
    var box = boxSizeUnit(cc);
    var outlineCenter = cc.getImageCoords(outlineBox.pts[0]);

    var rCorners = getRectCorners(outlineBox.pts[0], outlineBox.width, outlineBox.height,
                                 outlineBox.unitType, cc, Point.IM_PT).map( (c) => {
        return simpleRotateAroundPt(c, outlineCenter, -rotAngle, Point.IM_PT);
    });

    // some corner could be null
    return rCorners.reduce((prev, handlerCenter) => {
        const corner = getWorldOrImage(handlerCenter, cc);
        let   handlerBox = null;

        if (corner) {
            handlerBox = ShapeDataObj.makeRectangleByCenter(corner,
                box.size[0], box.size[1], box.unit,
                0.0, ShapeDataObj.UnitType.ARCSEC, false);

            updateHandleRotAngle(rotAngle, handlerBox);
            set(handlerBox, 'color', HANDLE_COLOR);
        }
        prev.push(handlerBox);
        return prev;
    }, []);
}

/**
 * create rotate handle at prorper side of outline box by trying the right, top, left and bottom sides respectively
 * @param outlineBox
 * @param cc
 * @param rotAngle
 * @returns {*}
 */
function createRotateHandle(outlineBox, cc, rotAngle) {
    const handleCenter = [[0, -0.5], [0.5, 0], [0, 0.5], [-0.5, 0]];  // handle center relative to the end of handle bar
    const handleAngle = [Math.PI * 3/2, 0, Math.PI*0.5, Math.PI];
    const circleLoc = [[0, -0.75], [0.75, 0], [0, 0.75], [-0.75, 0]];  // circle center relative to the end of handle bar
    const [x1, x2, y1, y2] = [0, cc.screenSize.width, 0, cc.screenSize.height];
    var rotateObj = null;

    const corners = getRectCorners(outlineBox.pts[0], outlineBox.width, outlineBox.height, outlineBox.unitType, cc, Point.SPT);
    var side = 4;
    var originVp = cc.getScreenCoords(outlineBox.pts[0]);
    var vpCorners = corners.map((c) => {
        return simpleRotateAroundPt(cc.getScreenCoords(c), originVp, rotAngle, Point.SPT);
    });

    var startIdx = has(outlineBox, 'rotateSide') ? outlineBox.rotateSide : 1;
    var endIdx = startIdx + side - 1;

    for (let idx = startIdx; idx <= endIdx; idx++) {
        var i = idx%side;
        var j = (i+1)%side;
        var ends;

        if ( !vpCorners[i] || !vpCorners[j] ) continue;  // skip any corner that is null

        var [xlen, ylen] = [(vpCorners[j].x - vpCorners[i].x), (vpCorners[j].y - vpCorners[i].y)];

        // find the ends of the side intercepted by the border of viewport
        ends = [vpCorners[i], vpCorners[j]].map((vp) => {
            var {x, y} = vp;

            if (vp.x >= x1 && vp.x <= x2) {
               x = vp.x;
               y = vp.y;
            } else {
               x = (vp.x < x1) ? x1 : x2;
               y = (x - vp.x) * ylen/xlen + vp.y;
            }

            if (y < y1) {
               x = (y1 - y) * xlen / ylen + x;
               y = y1;
            } else if (y > y2) {
                x = (y2 - y) * xlen / ylen + x;
                y = y2;
            }
            return {x, y};
        });

        // bottom center of the hanle
        var hBottom = makeScreenPt((ends[0].x + ends[1].x)/2, (ends[0].y + ends[1].y)/2);
        // center of the handle, rotate first if there is
        var hCenter = makeScreenPt((ends[0].x + ends[1].x)/2 + handleCenter[i][0] * ROTATE_BOX,
                                    (ends[0].y + ends[1].y)/2 + handleCenter[i][1] * ROTATE_BOX);
        if (rotAngle !== 0) {
            hCenter = simpleRotateAroundPt(hCenter, hBottom, rotAngle, Point.SPT);
        }

        // bottom center of the handle
        const handleBottom = getWorldOrImage(hBottom, cc);
        if (!cc.pointInView(handleBottom)  || !cc.pointInView(getWorldOrImage(hCenter, cc))) continue;

        // test if all cornres of the circle at the handle after rotation are seen
        const cCenter = makeScreenPt((ends[0].x + ends[1].x)/2 + circleLoc[i][0] * ROTATE_BOX,
                                     (ends[0].y + ends[1].y)/2 + circleLoc[i][1] * ROTATE_BOX);
        var hNotInView = cornerScreen.findIndex ( (c) => {
            // corners around circle portion
            var vp = makeScreenPt(cCenter.x + c[0] * ROTATE_BOX * 0.25, cCenter.y + c[1] * ROTATE_BOX * 0.25);
            var rVp = simpleRotateAroundPt(vp, hBottom, rotAngle, Point.SPT);

            return !cc.pointInView(getWorldOrImage(rVp, cc));
        });
        if (hNotInView >= 0 || !cc.pointInView(hBottom)) continue;

        rotateObj = makePointDataObj(handleBottom, ROTATE_BOX, DrawSymbol.ROTATE);

        // store the rotate angle and handle center
        rotateObj = Object.assign(rotateObj, {renderOptions: {rotAngle: (handleAngle[i%side]+rotAngle)},
                                              color: HANDLE_COLOR},
                                              {rotateCenter: cc.getDeviceCoords(hCenter)});  // rotate center in device coordinate
        outlineBox.rotateSide = i;
        break;
    }
    return rotateObj;
}


/**
 * create outline box by checking if any corner is in view
 * @param centerPt
 * @param width
 * @param height
 * @param unitType
 * @param cc
 * @param angle
 * @returns {*}
 */
function createOutlineBox(centerPt, width, height, unitType, cc, angle = 0.0) {
    var corners = getRectCorners(centerPt, width, height, unitType, cc);

    var totalInView = corners.reduce( (prev, corner) =>
    {
        var rCorner = (angle !== 0) ?
                      simpleRotateAroundPt(cc.getImageCoords(corner), cc.getImageCoords(centerPt), -angle, Point.IM_PT) :
                      corner;

        if (rCorner && cc.pointOnDisplay(rCorner)) {
            prev++;
        }
        return prev;
    }, 0);

    var outlineBox = null;
    if (totalInView >= 1) {
        var w = lengthSizeUnit(cc, width, unitType);
        var h = lengthSizeUnit(cc, height, unitType);

        outlineBox = ShapeDataObj.makeRectangleByCenter(centerPt, w.len, h.len, w.unit,
                                                    0.0, ShapeDataObj.UnitType.ARCSEC, false);

        outlineBox = Object.assign(outlineBox, {color: HANDLE_COLOR,
                                                renderOptions: {lineDash: [8, 5, 2, 5],
                                                                rotAngle: angle}});
    }
    return outlineBox;
}

/**
 * find the drawObj inside the marker which is closest to the given screenPt (marker and the handlers)
 * @param screenPt
 * @param drawObj
 * @param cc
 * @returns {*}
 */
export function findClosestIndex(screenPt, drawObj, cc) {
    var distance = MARKER_DISTANCE;
    if (!drawObj || !has(drawObj, 'drawObjAry')) {
        return {index: -1};
    }

    var {outlineIndex, drawObjAry} = drawObj;

    var dObjAry = drawObjAry.slice(0, outlineIndex);

    // consider to add the outline box which is at plot center in case no handles are included
    if ((get(drawObj, 'includeResize', false) || get(drawObj, 'includeRotate', false)) || !isOutlineBoxOriginal(drawObj)) {
        dObjAry = dObjAry.concat(updateHandle(drawObj, cc, [MARKER_HANDLE.rotate, MARKER_HANDLE.resize]));
        if (isEmpty(dObjAry)) {
            return {index: -1};
        }
    } else {
        dObjAry.push( drawObjAry[outlineIndex]);
    }

    // check from the last drawobj (roate and resize handles are checked first)
    // not include the outline box and stop search in case distance zero is found
    var lastIdx = dObjAry.length-1;
    var index =  dObjAry.reduce((prev, oneObj, index) => {
        var wIdx = lastIdx - index;
        //if (wIdx !== drawObj.outlineIndex && distance > 0) {
        if (distance > 0) {
            var lastObj = dObjAry[wIdx];

            if (lastObj) {
                var dist = getScreenDistToMarker(lastObj, cc, screenPt);

                if (dist >= 0 && dist < distance) {
                    distance = dist;
                    prev = wIdx;
                }
            }
        }
        return prev;
    }, -1);
    return {index, distance};
}

/**
 * update the color based on the setting in def for all drawing objects except center, resize & rotate handles
 * @param drawObj
 * @param def
 */
function updateColorFromDef(drawObj, def) {
    var getColor = (obj) => (get(def, 'color') || drawObj.color || 'green'); // def color overide the original color

    drawObj.color = getColor(drawObj);
    if (!drawObj.drawObjAry) return;

    drawObj.drawObjAry.forEach((oneDrawObj) => {
        if (oneDrawObj && get(oneDrawObj, 'isMarker', false)) {  // drawing object contained in footprint
            oneDrawObj.color = getColor(oneDrawObj);
            if (has(oneDrawObj, 'drawObjAry')) {      // a composite drawing object, like polygon
                oneDrawObj.drawObjAry.forEach((oneShape) => {
                    oneShape.color = getColor(oneShape);
                });
            }
        }
    });
}


/**
 * draw the object which contains drawObj array
 * @param drawObjP
 * @param ctx
 * @param plot
 * @param def
 * @param vpPtM
 * @param onlyAddToPath
 */
export function drawMarkerObject(drawObjP, ctx, plot, def, vpPtM, onlyAddToPath) {
    if (has(drawObjP, 'drawObjAry')) {

        //markerTextOffset(drawObj, plot);
        //var drawObj = cloneDeep(drawObjP);

        var drawObj = Object.assign({}, drawObjP);
        drawObj.drawObjAry = drawObjP.drawObjAry.map( (obj) => Object.assign({}, obj) );

        var newObj = Object.assign({}, drawObj, {drawObjAry: drawObj.drawObjAry.slice(0, drawObj.outlineIndex)});
        // add outline box, resize and rotate handle if any is included, or show outline if the outline is not the original
        if ((get(drawObj, 'includeResize', false) ||
             get(drawObj, 'includeRotate', false) ||
             get(drawObj, 'includeOutline', false)) || !isOutlineBoxOriginal(drawObj)) {
            newObj.drawObjAry = newObj.drawObjAry.concat(updateHandle(drawObj, plot,
                                                         [MARKER_HANDLE.resize, MARKER_HANDLE.rotate]));
        }

        // newObj is made for display
        updateColorFromDef(newObj, def);

        // change th outline box to be solid line in case of 'plotceneter' type outline
        if (isOutlineBoxWithType(newObj, OutlineType.plotcenter)) {
            set(newObj.drawObjAry[newObj.outlineIndex], ['renderOptions', 'lineDash'], null);
        }
        newObj.drawObjAry.forEach((oneDrawObj) => {
            if (oneDrawObj) {      // the resize or rotate drawObj could be null
                DrawOp.draw(oneDrawObj, ctx, plot, def, vpPtM, onlyAddToPath);
            }
        });

        drawFootprintText(newObj, plot, def, ctx);
        set(drawObjP, 'textWorldLoc', newObj.textWorldLoc);
        set(drawObjP, 'textObj', ShapeDataObj.makeText(getWorldOrImage(newObj.textWorldLoc, plot), newObj.text));
    }
}

/**
 * draw the text attached with footprint drawobj by text drawing function in ShapeDataboj
 * @param drawObj
 * @param plot
 * @param def
 * @param ctx
 */
function drawFootprintText(drawObj, plot, def, ctx) {

    var drawParams = makeDrawParams(drawObj, def);
    var {text, textLoc, textWorldLoc} = drawObj;
    var {fontSize} = drawParams;

    if (isNil(text)) return;
    const outlineObj = (drawObj.drawObjAry.length > drawObj.outlineIndex) ?
                      drawObj.drawObjAry[drawObj.outlineIndex] :
                      (isHiPS(plot) ? (updateHandle(drawObj, plot, []))[0] : remakeOutlineBox(drawObj, plot, [OutlineType.original]));


    // if no outline exists, display text as stored location
    // if outline exists, display text at defined textLoc or alternate location defined textLoc is out of image area
    if (!outlineObj) {
        if (textWorldLoc && !isHiPS(plot)) {
            drawText(drawObj, ctx, plot, textWorldLoc, drawParams);
        }
    } else {

        const objArea = getObjArea(outlineObj, plot, true); // in image coordinate

        if (objArea) {
            textLoc = flipTextLocAroundY(plot, textLoc);
            const firstIdx = textLocSeq.findIndex((t) => t.key === textLoc.key);

            for (let t = firstIdx; t < firstIdx + textLocSeq.length; t++) {
                const tLoc = textLocSeq[t % textLocSeq.length];

                const textPt = makeTextLocationComposite(plot, tLoc, fontSize,
                    objArea.width * plot.zoomFactor,
                    objArea.height * plot.zoomFactor,
                    objArea.centerPt);
                if (textPt) {
                    if (drawText(drawObj, ctx, plot, textPt, drawParams)) {
                        break;   // text is drawn
                    }
                }
            }
        }
    }
}


/**
 * update the footprint outline box once some object is clicked to be selected or relocated.
 * re-render the entire footprint in case the plot zoom changes or footprint on HiPS plot is relocated
 * @param drawObj
 * @param cc
 * @param bForced force to recalculate the drawObj from the original regions
 * @returns {*}
 */
export function updateFootprintOutline(drawObj, cc, bForced = false) {


    if (drawObj.sType === MarkerType.Marker || (!bForced && drawObj.lastZoom === cc.zoomFactor)) {
    //if (drawObj.sType === MarkerType.Marker || drawObj.sType === MarkerType.Footprint) {
        if (drawObj.lastZoom !== cc.zoomFactor) {
            bForced = true;     // if marker and zoom factror changes
        }
        updateOutlineBox(drawObj, cc, !bForced, bForced);
    } else {
        const centerPt = getWorldOrImage(drawObj.pts[0], cc);
        const {angle} = drawObj;

        drawObj.drawObjAry = getOriginalFPDrawObj(drawObj, cc);
        drawObj.angle = 0;
        drawObj.drawObjAry.push(createCrossCenter(centerPt));

        const handle = updateHandle(drawObj, cc, [MARKER_HANDLE.outline]);
        if (handle?.length) {
            drawObj.drawObjAry.push(handle[0]);
        }

        drawObj.angle = angle;
        const angleRad = getMarkerAngleInRad(drawObj);

        drawObj.drawObjAry = drawObj.drawObjAry.reduce((prev, oneObj) => {
            const rCenter = has(oneObj, 'outlineType')&&oneObj.outlineType.is(OutlineType.plotcenter) ?
                             get(oneObj, ['pts', '0']) : centerPt;
            var rObj = rCenter ? DrawOp.rotateAround(oneObj, cc, angleRad, rCenter) : oneObj;

            prev.push(rObj);
            return prev;
        }, []);

        if (get(drawObj, 'originalOutlineBox', null)) {
            drawObj.originalOutlineBox = isOutlineBoxOriginal(drawObj) ? Object.assign({}, drawObj.drawObjAry[drawObj.outlineIndex]) : null;
        }
    }

    return Object.assign({}, drawObj);
}

/**
 * update the outline box after translation and rotation operation.
 * @param drawObj
 * @param cc
 * @param upgradeOutline
 * @param resetOutline  recompute the outline box from the original regions
 */
function updateOutlineBox(drawObj, cc, upgradeOutline = false, resetOutline = false) {
    var {outlineIndex, drawObjAry} = drawObj;

    if (outlineIndex && drawObjAry && drawObjAry.length > outlineIndex) {
        var outlineObj = resetOutline ? updateHandle(drawObj, cc, [MARKER_HANDLE.outline])
                                      : updateHandle(drawObj, cc, [], upgradeOutline);

        if (!isEmpty(outlineObj)) {
            drawObjAry[outlineIndex] = outlineObj[0];
        } else {
            console.log('error on creating outline');
        }
    }
}

/**
 * update the translation information of the footprint or marker, store it in either world coordinate or image coordinate.
 * The computation is either made on image coordinate or originally defined regions (for HiPS)
 * @param drawObj
 * @param cc
 * @param apt
 * @param isSet set or incrment the translation
 */
export function updateFootprintTranslate(drawObj, cc, apt, isSet = false) {
    var tx = lengthSizeUnit(cc, apt.x, apt.type);
    var ty = lengthSizeUnit(cc, apt.y, apt.type);
    var deltaX, deltaY;

    if (!isSet) {
        deltaX = tx.len;
        deltaY = ty.len;
        if (!isEmpty(drawObj.translation)) {
            tx.len += drawObj.translation.x;   // add the increment
            ty.len += drawObj.translation.y;
        }
    } else {
        deltaX = tx.len - get(drawObj, 'translation.x', 0.0);  // set absolutely
        deltaY = ty.len - get(drawObj, 'translation.y', 0.0);

    }

    var newApt = {x: tx.len, y: ty.len, type: tx.unit};
    var newObj = translateMarker(drawObj, cc, {x: deltaX, y: deltaY, type: tx.unit});

    return clone(newObj, {translation: newApt});
}

/**
 * update object rotate angle
 * @param drawObj
 * @param plot
 * @param worldPt  center point to rotate around
 * @param angle
 * @param angleUnit
 * @param isSet set or increment the rotation angle
 * @returns {*}
 */
export function updateFootprintDrawobjAngle(drawObj, plot, worldPt, angle = 0.0, angleUnit = ANGLE_UNIT.radian,  isSet = false) {
    var newAngle = convertAngle(angleUnit.key, 'radian', angle);
    var deltaAngle;

    var crtAngle = getMarkerAngleInRad(drawObj);

    // get current angle status
    if (!isSet) {
        deltaAngle = newAngle;
        //newAngle += crtAngle;
    } else {
        deltaAngle = newAngle - crtAngle;
    }

    return rotateMarkerAround(drawObj, plot, deltaAngle, worldPt);
}


/**
 * translate marker or footprint
 *  - marker or non HiPS image: translate the marker/footprint by apt on image coordinate
 *  - HiPS: recompute the footprint per originally defined regions by translating the footprint center first
 * @param drawObj
 * @param plot
 * @param apt
 * @returns {*} an array of translated objects contained in drawObj
 */
export function translateMarker(drawObj, plot, apt) {
    const deltaImgX = lengthToImagePixel(apt.x, plot, apt.type);
    const deltaImgY = lengthToImagePixel(apt.y, plot, apt.type);


    const moveFrom = (pti, deltaX, deltaY) => {   // translate on image coordinate
        return makeImagePt(pti.x + deltaX, pti.y + deltaY);
    };

    const newPti = moveFrom(plot.getImageCoords(drawObj.pts[0]), deltaImgX, deltaImgY);
    let newObj = clone(drawObj, {pts: [makePoint(newPti, plot, drawObj.pts[0].type)]});

    // do translation on world instead of on image domain by relocating the center first
    if (isHiPS(plot) && (drawObj.sType !== MarkerType.Marker)) {
        newObj = updateFootprintOutline(newObj, plot, true);
    } else {

        const dObjAry = collectDrawobjAry(drawObj, [MARKER_HANDLE.outline]);
        newObj.drawObjAry = dObjAry.reduce((prev, oneDrawobj) => {
            prev.push(DrawOp.translateTo(oneDrawobj, plot, apt));
            return prev;
        }, []);
    }

    if (get(newObj, 'originalOutlineBox', null)) {
        newObj.originalOutlineBox = isOutlineBoxOriginal(newObj) ? Object.assign({}, newObj.drawObjAry[newObj.outlineIndex]) : null;
    }

    // translate text object
    if (newObj.textObj && newObj.textWorldLoc) {
        const newText = DrawOp.translateTo(newObj.textObj, plot, apt);
        const newTextLoc = plot.getImageCoords(newText.pts[0]);

        Object.assign(newObj, {textObj: newText, textWorldLoc: newTextLoc});
    }

    //updateOutlineBox(newObj, plot, true);

    return newObj;
}


function adjustAngle(angle) {
    let newAngle = angle;

    while (newAngle < -Math.PI) {
        newAngle += 2 * Math.PI;
    }
    while (newAngle > Math.PI) {
        newAngle -= 2 * Math.PI;
    }
    return newAngle;
}
/**
 * rotate a marker around worldPt by angle on image coordinate
 * @param drawObj
 * @param plot
 * @param dAngle  screen coordinate direction, in radian
 * @param worldPt
 * @returns {array} a array of rotated objects contained in drawObj
 */
export function rotateMarkerAround(drawObj,plot, dAngle, worldPt) {
    const {pts} = drawObj;
    if (!pts || !pts[0] || !worldPt) return null;

    const worldImg = plot.getImageCoords(worldPt);
    const drawObjPt = plot.getImageCoords(pts[0]);

    var rotateAroundPt = (imgPt) => {   // rotate around given worldPt on image coordinate
        var x1 = imgPt.x - worldImg.x;
        var y1 = imgPt.y - worldImg.y;
        var cos = Math.cos(-dAngle);
        var sin = Math.sin(-dAngle);

        var x2 = x1 * cos - y1 * sin + worldImg.x;
        var y2 = x1 * sin + y1 * cos + worldImg.y;

        return getWorldOrImage(makeImagePt(x2, y2), plot);
    };

    const newObj = clone(drawObj, {pts: [rotateAroundPt(drawObjPt)]});
    const dAry = collectDrawobjAry(drawObj, [MARKER_HANDLE.outline]);
    const newAngle =  adjustAngle(getMarkerAngleInRad(newObj) + dAngle);

    newObj.drawObjAry = dAry.reduce((prev, oneObj) => {
        const rCenter = has(oneObj, 'outlineType')&&oneObj.outlineType.is(OutlineType.plotcenter) ?
                        get(oneObj, ['pts', '0']) : worldPt;
        rCenter ?  prev.push(DrawOp.rotateAround(oneObj, plot, dAngle, rCenter)) : prev.push(oneObj);

        return prev;
    }, []);

    if (get(newObj, 'originalOutlineBox', null)) {
        const outlineBox = newObj.drawObjAry.length > newObj.outlineIndex ? newObj.drawObjAry[newObj.outlineIndex] : null;
        newObj.originalOutlineBox = (outlineBox&&(outlineBox.outlineType === OutlineType.original)) ? Object.assign({}, outlineBox) : null;
    }

    if (newObj.textObj && newObj.textWorldLoc) {
        const newText = DrawOp.rotateAround(newObj.textObj, plot, dAngle, worldPt);
        const newTextLoc = plot.getImageCoords(newText.pts[0]);

        Object.assign(newObj, {textObj: newText, textWorldLoc: newTextLoc});
    }
    //updateOutlineBox(newObj, plot, true);
    return clone(newObj, {angle: newAngle, angleUnit: ANGLE_UNIT.radian});
}

/**
 * update marker object (main marker and outline box) per new size
 * @param markerObj
 * @param cc
 * @param newSize
 */
export function updateMarkerSize(markerObj, cc, newSize) {
    var {drawObjAry} = markerObj;
    var newObj = clone(markerObj);

    // scale the marker size proportional to the change of outline box in case the marker is out of the plot area
    const scaleMarkerSize = (oSize) => {
        let radius, unit;

        if (!isOutlineBoxOriginal(markerObj)) {
            const pOutline = drawObjAry[markerObj.outlineIndex];
            const pOutlineSize = lengthSizeUnit(cc, pOutline.width, pOutline.unitType);

            radius = drawObjAry[0].radius * oSize.len/pOutlineSize.len;
            unit = drawObjAry[0].unitType;
        } else {
            radius = oSize.len/2;
            unit = oSize.unit;
        }

        return {len: radius, unit};
    };

    if (drawObjAry && drawObjAry.length > 0) {
        const {size, newRadius} = newSize; // if newRadius exists, recompute the marker outline

        const oSize = newRadius ? null : lengthSizeUnit(cc, Math.min(size[0], size[1]), newSize.unitType);  // update outline box size
        const mRadius = newRadius? {len: newRadius.radius, unit: newRadius.unitType} : scaleMarkerSize(oSize);

        newObj = Object.assign(newObj, {width: mRadius.len*2, height: mRadius.len*2, unitType: mRadius.unit });
        const mObj = clone(drawObjAry[0], {radius: mRadius.len, unitType: mRadius.unit});    // update marker size
        let   oObj;

        if (drawObjAry.length > 1 && oSize)  {                            // update outline size
            oObj = clone(drawObjAry[1], {width: oSize.len, height: oSize.len, unitType: oSize.unit});
        } else {
            oObj = null;    // recompute outline if given newRadius
        }
        newObj.drawObjAry = oObj? [mObj, oObj] : [mObj];

        if (!oObj) {
            const outline = updateHandle(newObj, cc, [MARKER_HANDLE.outline]);
            if (outline) {
                newObj.drawObjAry[newObj.outlineIndex] = outline[0];
            } else {
                console.log('outline is null');
            }
        }
    }

    return newObj;
}


/**
 * update the text attached to the footprint object
 * @param drawObj
 * @param text
 * @param textLoc
 * @returns {*}
 */
export function updateFootprintDrawobjText(drawObj, text, textLoc) {
    if (!text) text = '';
    if (!textLoc) textLoc = defaultFootprintTextLoc;
    var textInfo={text, textLoc};

    return Object.assign({}, drawObj, textInfo);
}


/**
 * generate region description for marker or footprint
 * @param drawObj
 * @param plot
 * @param def
 * @returns {*}
 */
export function toMarkerRegion(drawObj,plot, def) {
    if (!has(drawObj, 'drawObjAry')) return [];

    var dObjs = drawObj.drawObjAry.slice(0, drawObj.outlineIndex); // exclude center, outline box, and handles.

    updateColorFromDef(drawObj, def);
    var resRegions = [];
    var {text} = drawObj;

    if (text) {
        var textImgLoc = get(drawObj, 'textWorldLoc', null);

        if (textImgLoc) {
            var fontName= drawObj.fontName || def.fontName || 'helvetica';
            var fontSize= drawObj.fontSize || def.fontSize || DEFAULT_FONT_SIZE;
            var fontWeight= drawObj.fontWeight || def.fontWeight || 'normal';
            var fontStyle= drawObj.fontStyle || def.fontStyle || 'normal';

            fontSize = fontSize.slice(0, fontSize.indexOf('pt'));

            var textReg = `text ${textImgLoc.x}i ${textImgLoc.y}i # color=${drawObj.color} text={${text}}` +
                          ` font="${fontName} ${fontSize} ${fontWeight} ${fontStyle}"`;
            resRegions = [textReg];
        }
    }
    return dObjs.reduce( (prev, dObj) => {
        if (get(dObj, 'isMarker', false)) {      // ignore the drawObj not derived from the defined region
            var regList = DrawOp.toRegion(dObj, plot, def);

            if (!isEmpty(regList)) {
                prev.push(...regList);
            }
        }
        return prev;
    }, resRegions);
}


/**
 * calculate the distance between a screen point and any type of region contained in marker or footprint entity
 * the distance is 0 in case the given point is inside any of the contained shape
 * @param drawObj
 * @param plot
 * @param pt
 * @returns {number}
 */
export function getScreenDistToMarker(drawObj, plot, pt) {
    var distance = -1;
    var cScreen;

    var distToPt = (x1, y1) => {
        var dx = x1-pt.x;
        var dy = y1-pt.y;
        return Math.sqrt((dx * dx) + (dy * dy));
    };

    // if the rectangle is slanted, then find the distance by using distToPolygon, centerPt: rotate center of the drawObj
    var distToRect = (corners, rotAngle, centerPt) => {
        const hasNull = corners.some((oneCorner) => !oneCorner);   // corners may contain null conrner

        if (rotAngle !== 0.0 || hasNull) {
            var rCorners = corners.reduce( (prev, c) => {
                prev.push(simpleRotateAroundPt(c, centerPt, rotAngle, Point.SPT));
                return prev;
            }, []);

            return (!hasNull && isWithinPolygon(pt, rCorners, plot)) ? 0 : distToPolygon(rCorners);
        } else {
            var [x1, x2, y1, y2] = [corners[0].x, corners[1].x, corners[1].y, corners[2].y];
            var bx = (pt.x >= x1 && pt.x <= x2) ? pt.x : ((pt.x < x1) ? x1 : x2);
            var by = (pt.y >= y1 && pt.y <= y2) ? pt.y : ((pt.y < y1) ? y1 : y2);

            return distToPt(bx, by);
        }
    };

    var distToLine = (pt1, pt2) => {
        var {x: x1, y: y1} = pt1;
        var {x: x2, y: y2} = pt2;
        var dist;

        if (x1 > x2) {  // exchange the order
            [x1, x2] = [x2, x1];
            [y1, y2] = [y2, y1];
        }

        if (pt.x < x1) {
            dist = distToPt(x1, y1);
        } else if (pt.x > x2) {
            dist = distToPt(x2, y2);
        } else {
            var [a, b, c] = [y2-y1, x1-x2, x2*y1-x1*y2];

            if (a === 0 && b === 0) {
                dist = distToPt(x1, y1);
            } else {
                dist = Math.abs(pt.x * a + pt.y * b + c)/Math.sqrt(a*a + b*b);
            }
        }
        return dist;
    };

    // skip the convex which is null
    var distToPolygon = (pts) => {
        var vertices = [...pts, pts[0]].map((onePt) => plot.getScreenCoords(onePt));
        var totalV = pts.length;

        return vertices.reduce((prev, ver, index) => {
            if ((index < totalV) && (vertices[index]) && (vertices[index+1])) {
                var d = distToLine(vertices[index], vertices[index+1]);

                if (d < prev) prev = d;
            }
            return prev;
        }, Number.MAX_VALUE);
    };

    var corners;
    var rotAngle;

     // the center of rectangle is specified
     if (drawObj.type === POINT_DATA_OBJ) {
         var {size} = drawObj;

         cScreen = plot.getScreenCoords(drawObj.symbol === DrawSymbol.ROTATE ? drawObj.rotateCenter : drawObj.pt);

         corners = getRectCorners(cScreen, size, size, screenUnit, plot, Point.SPT);
         rotAngle = get(drawObj, 'renderOptions.rotAngle', 0.0);
         distance = distToRect(corners, rotAngle, cScreen);
     } else if (drawObj.type === ShapeDataObj.SHAPE_DATA_OBJ) {
         if (drawObj.sType === ShapeDataObj.ShapeType.Rectangle) {
             corners = getRectCorners(drawObj.pts[0], drawObj.width, drawObj.height, drawObj.unitType, plot, Point.SPT);
             rotAngle = get(drawObj, 'renderOptions.rotAngle', 0.0);
             distance = distToRect(corners, rotAngle, plot.getScreenCoords(drawObj.pts[0]));
         } else if (drawObj.sType === ShapeDataObj.ShapeType.Circle) {
             var r = lengthToScreenPixel(drawObj.radius, plot, drawObj.unitType);

             if (plot.pointInView(drawObj.pts[0])) {
                 cScreen = plot.getScreenCoords(drawObj.pts[0]);
                 distance = distToPt(cScreen.x, cScreen.y);
                 distance = distance > r ? distance - r : 0;
             } else {
                 distance = Number.MAX_VALUE;      // not counted for the point out of plot area
             }

         } else if (drawObj.sType === ShapeDataObj.ShapeType.Polygon) {
             var inside = isWithinPolygon(pt, drawObj.pts, plot);

             distance = inside ? 0 : distToPolygon(drawObj.pts);
         }
    }
    return distance;
}


export function markerToRegion(drawObj, plot, dl, bSeperateText = false) {
    const rgDesAry = [];
    let oneRegionDes;

    get(drawObj, 'drawObjAry', []).forEach( (dObj) => {
        dObj.bSeperateText = bSeperateText;
        // only render marker/footprint elements (no rotate handle or outline) if it is 'MarkerObj'
        if (dObj.isMarker) {
            oneRegionDes = DrawOp.toRegion(dObj, plot, dl.drawingDef);
            if (!isEmpty(oneRegionDes)) {
                rgDesAry.push(...oneRegionDes);
            }
        }
        dObj.bSeperateText = null;
    });

    oneRegionDes = drawObj.textObj && DrawOp.toRegion(drawObj.textObj, plot, dl.drawingDef);
    if (!isEmpty(oneRegionDes)) {
        rgDesAry.push(...oneRegionDes);
    }
    return rgDesAry;
}

export function isOutlineBoxOriginal(drawObj) {
    return !isOutlineBoxWithType(drawObj, OutlineType.center) && !isOutlineBoxWithType(drawObj, OutlineType.plotcenter);
}

function isOutlineBoxWithType(drawObj, outlineType) {
    return (get(drawObj.drawObjAry, 'length', 0) > drawObj.outlineIndex) &&
            outlineType.is(drawObj.drawObjAry[drawObj.outlineIndex].outlineType);
}

function getCenterBoxSize(objType) {
    return  (MarkerType.Marker.is(objType)) ? MARKER_SIZE : CENTER_BOX;
}