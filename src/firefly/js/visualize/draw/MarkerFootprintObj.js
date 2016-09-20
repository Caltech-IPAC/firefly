
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Point, {makeImagePt, makeScreenPt, makeViewPortPt, makeWorldPt, SimplePt} from '../Point.js';
import {CoordinateSys} from '../CoordSys.js';
import ShapeDataObj, {lengthToImagePixel, lengthToScreenPixel,
       lengthToArcsec, makePoint, drawText, makeTextLocationComposite} from './ShapeDataObj.js';
import {POINT_DATA_OBJ, getPointDataobjArea, makePointDataObj, DrawSymbol} from './PointDataObj.js';
import {getDrawobjArea, isWithinPolygon} from './ShapeHighlight.js';
import {defaultMarkerTextLoc} from '../../drawingLayers/MarkerToolUI.jsx';
import {defaultFootprintTextLoc} from '../../drawingLayers/FootprintToolUI.jsx';
import {TextLocation, Style, DEFAULT_FONT_SIZE} from './DrawingDef.js';
import {FootprintFactory} from './FootprintFactory.js';
import {convertAngle} from '../VisUtil.js';
import DrawObj from './DrawObj.js';
import DrawUtil from './DrawUtil.js';
import DrawOp from './DrawOp.js';
import BrowserInfo from '../../util/BrowserInfo.js';
import {clone} from '../../util/WebUtil.js';
import Enum from 'enum';
import {has, isNil, get, isEmpty, isArray, set} from 'lodash';

const HANDLER_BOX = 6;        // handler size (square size in screen coordinate)
const CENTER_BOX = 60;
const CROSS_BOX = 12;
const HANDLE_COLOR = '#DAA520';
const MARKER_DATA_OBJ= 'MarkerObj';
const DEF_WIDTH = 1;

const DEFAULT_STYLE= Style.STANDARD;

export const ROTATE_BOX = 32;
export const MARKER_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;
export const MarkerType= new Enum(['Marker', 'Footprint']);
export const ANGLE_UNIT = new Enum(['arcsec', 'arcmin', 'degree', 'radian']);
export const MARKER_HANDLE = new Enum(['outline', 'resize', 'rotate']);
export const OutlineType = new Enum(['original', 'center', 'plotcenter']);

const AllHandle = [MARKER_HANDLE.outline, MARKER_HANDLE.resize, MARKER_HANDLE.rotate];
const AllOutline = [OutlineType.original, OutlineType.center, OutlineType.plotcenter];

export var getWorldOrImage = (pt, cc) => (cc.projection.isSpecified() ?
                             cc.getWorldCoords(pt, CoordinateSys.EQ_J2000) : cc.getImageCoords(pt));

function make(sType, style) {
    const obj= DrawObj.makeDrawObj();
    obj.sType= sType;   // default: MarkerType.Marker
    obj.type= MARKER_DATA_OBJ;
    obj.style = (!style) ? DEFAULT_STYLE: style;
    // may contain the following:
        //obj.text= null;
        //obj.fontName = 'helvetica';
        //obj.fontSize = DEFAULT_FONT_SIZE;
        //obj.fontWeight = 'normal';
        //obj.fontStyle = 'normal';
        //obj.width= null;
        //obj.height= null;
        //obj.color
        //obj.style=Style.STANDARD;
        //obj.sType = MarkerType.Marker
        //obj.unitType = UnitType.PIXEL;
        //obj.includeRotate = true|false  if (isRotable === true), show outlinebox + rotate handle
        //obj.includeResize = true|false  if (isEditable === true) show outlinebox + resize handle
        //obj.includeOutline = true|false  show outlinebox
        //obj.textLoc= TextLocation.CIRCLE_SE
        //obj.textOffset= null;   // offsetScreenPt
        //obj.drawObjAry= array of ShapeDataObj
        //obj.originalOutlineBox  // the 'original' outlinebox, is only recorded if the outline box in
        //                        // drawObjAry is not the original ('center' or 'plotcenter')
        //obj.isRotable    //rotable
        //obj.isEditable   //resizable
        //obj.isMovable    //movable
        //obj.outlineIndex // handler starting index, outline box pos in drawObjAry
        //obj.resizeIndex  //resize handler index in drawObjAry
        //obj.rotateIndex  //rotate handler index in drawObjAry
        // sequence: outlinebox (handleIndex) => (resizeIndex) => (rotateIndex)
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
 * @returns {{length: *, unit: *}}
 */
export function lengthSizeUnit(cc, size, unitType) {
    var len, unit;

    if (cc.projection.isSpecified()) {
        len = lengthToArcsec(size, cc, unitType);
        unit = worldUnit;
    } else {
        len = lengthToImagePixel(size, cc, unitType);
        unit = imageUnit;
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
function boxSizeUnit(cc, boxSize = HANDLER_BOX, unitType = ShapeDataObj.UnitType.SCREEN_PIXEL)  {
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
    if (has(dObj, 'isRotable')) {
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
    //mainCircle = Object.assign(mainCircle, textProps);
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
    dObj.includeRotate = !!(get(dObj, 'isRotable') && isRotate && isOutline);
    setHandleIndex(dObj);
    dObj.plotImageId = cc.plotImageId;
    return dObj;
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
    var regionDrawObjAry = FootprintFactory.getDrawObjFromOriginalRegion(regions, fpCenter, regions[0].isInstrument);
    var centerObj = makePointDataObj(fpCenter, CROSS_BOX, DrawSymbol.CROSS);
    var dObj = clone(make(MarkerType.Footprint), {pts: [makeWorldPt(fpCenter.x, fpCenter.y)]});

    centerObj.color = 'red';
    regionDrawObjAry.forEach((obj) => obj.isMarker = true);
    regionDrawObjAry.push(centerObj);
    dObj = Object.assign(dObj, {
        isMovable: true,
        isRotable: true,
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
    dObj.includeRotate = !!(get(dObj, 'isRotable') && isRotate && isOutline);
    setHandleIndex(dObj);
    dObj.plotImageId = cc.plotImageId;
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
            return dp == Style.STANDARD && (drawObj.sType == MarkerType.Marker);
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
                obj.pts.forEach( (wp) => {
                    xSum += wp.x;
                    ySum += wp.y;
                    xTot++;
                    yTot++;
                });
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

    draw(drawObj,ctx,drawTextAry,plot,def,vpPtM,onlyAddToPath) {
        drawMarkerObject(drawObj,ctx,drawTextAry,plot,def,vpPtM, onlyAddToPath);
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
    var x1 = pt.x - center.x;
    var y1 = pt.y - center.y;
    var x2 = x1 * Math.cos(angle) - y1 * Math.sin(angle) + center.x;
    var y2 = x1 * Math.sin(angle) + y1 * Math.cos(angle) + center.y;

    return Object.assign(new SimplePt(x2, y2), {type: outType});
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
    var centerPt = cc.getImageCoords(pt);
    var nW = lengthToImagePixel(width/2, cc, unitType);
    var nH = lengthToImagePixel(height/2, cc, unitType);

    return cornersImg.map( (coord) => {
        var x = centerPt.x + coord[0] * nW;
        var y = centerPt.y + coord[1] * nH;

        var rImgPt = makeImagePt(x, y);
        if (outUnit === Point.W_PT ) {
            return getWorldOrImage(rImgPt, cc);
        } else if (outUnit ===  Point.SPT ) {
            return cc.getScreenCoords(rImgPt);
        } else if (outUnit === Point.VP_PT) {
            return cc.getViewPortCoords(rImgPt);
        } else {
            return rImgPt;
        }
    });
}

/**
 * calculate the footprint overal rectangular arae in image coordinate
 * @param rDrawAry
 * @param cc
 * @returns {{width: number, height: number, unitType: *}}
 */
export function getMarkerImageSize(rDrawAry, cc) {
    var area = rDrawAry.reduce( (prev, oneDrawObj) => {
        if (get(oneDrawObj, 'isMarker', false)) {
            var {upperLeft, width, height} = getObjArea(oneDrawObj, cc);  // in image coordinate
            var [min_x, min_y, max_x, max_y] = [upperLeft.x, upperLeft.y - height, upperLeft.x + width, upperLeft.y];

            if ((!has(prev, 'min_x')) || (min_x < prev.min_x)) {
                prev.min_x = min_x;
            }
            if ((!has(prev, 'min_y')) || (min_y < prev.min_y)) {
                prev.min_y = min_y;
            }
            if ((!has(prev, 'max_x')) || (max_x > prev.max_x)) {
                prev.max_x = max_x;
            }
            if ((!has(prev, 'max_y')) || (max_y > prev.max_y)) {
                prev.max_y = max_y;
            }
        }
        return prev;
    }, {});

    var width = lengthSizeUnit(cc, area.max_x - area.min_x + 1, ShapeDataObj.UnitType.IMAGE_PIXEL);
    var height = lengthSizeUnit(cc, area.max_y - area.min_y + 1, ShapeDataObj.UnitType.IMAGE_PIXEL);
    return {width: width.len, height: height.len, unitType: width.unit,
            centerPt: getWorldOrImage(makeImagePt((area.min_x + area.max_x)/2, (area.min_y + area.max_y)/2), cc)};
}

/**
 * get the rectangular area of any drawobj (pointdata or shapadata), in image coordinate
 * @param obj
 * @param cc
 * @returns {{upperLeft: *, width: *, height: *}}
 */
function getObjArea(obj, cc) {
    var {upperLeft, width, height, centerPt, center} = (obj.type === ShapeDataObj.SHAPE_DATA_OBJ) ?
                                                getDrawobjArea(obj, cc): getPointDataobjArea(obj, cc);

    if (center) {
        centerPt = center;
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
        var rCorner = simpleRotateAroundPt(cc.getImageCoords(corner), cc.getImageCoords(pts[0]), -rotAngle, Point.IM_PT);
        if (cc.pointInViewPort(rCorner)) {
            prev++;
        }
        return prev;
    }, 0);
};

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

    // try original outline box (the current outline is not 'original)'
    if (checkOutline.includes(OutlineType.original)) {
        if (!tryOutline) {
            if (drawObj.sType === MarkerType.Marker) {               // for marker case (assume no rotation)
                var {radius = 0.0, unitType = ShapeDataObj.ShapeType.ARCSEC} = get(drawObj, ['drawObjAry', '0']) || {};

                tryOutline = ShapeDataObj.makeRectangleByCenter(drawObj.pts[0], radius*2, radius*2, unitType,
                        0.0, ShapeDataObj.UnitType.ARCSEC, false);
            } else if (!tryOutline && has(drawObj,'regions')) {        // for footprint case
                var drawObjAry;

                drawObjAry = FootprintFactory.getDrawObjFromOriginalRegion(drawObj.regions, drawObj.pts[0],
                    drawObj.regions[0].isInstrument);
                drawObjAry.forEach((obj) => obj.isMarker = true);

                var {width, height, centerPt, unitType} = getMarkerImageSize(drawObjAry, cc);
                //var w = lengthSizeUnit(cc, width, unitType);
                //var h = lengthSizeUnit(cc, height, unitType);

                var rCenterPt = simpleRotateAroundPt(cc.getImageCoords(centerPt), cc.getImageCoords(drawObj.pts[0]),
                    -angle, Point.IM_PT);

                tryOutline = ShapeDataObj.makeRectangleByCenter(getWorldOrImage(rCenterPt, cc), width, height, unitType,
                    0.0, ShapeDataObj.UnitType.ARCSEC, false);
            }
            drawObj.origianlOutlineBox = Object.assign(tryOutline, { outlineType: OutlineType.original,
                                                                     color: HANDLE_COLOR,
                                                                     renderOptions: {lineDash: [8, 5, 2, 5],
                                                                     rotAngle: angle} });
        }
        if (tryOutline && rectCornerInView(tryOutline, cc) > 0) {
            drawObj.originalOutlineBox = null;
            return tryOutline;
        }
    }

    // try center outlinebox
    if (checkOutline.includes(OutlineType.center)) {

        tryOutline = createOutlineBox(drawObj.pts[0], CENTER_BOX, CENTER_BOX, ShapeDataObj.UnitType.PIXEL, cc, angle);
        if (tryOutline) {
            return clone(tryOutline, {outlineType: OutlineType.center});
        }
    }
    // try plotcenter outlinebox
    if (checkOutline.includes(OutlineType.plotcenter)) {
        var vCenter = makeViewPortPt(cc.viewPort.dim.width / 2, cc.viewPort.dim.height / 2);

        tryOutline = createOutlineBox(vCenter, CENTER_BOX, CENTER_BOX, ShapeDataObj.UnitType.PIXEL, cc, angle);
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

    if (!pts) return retval;
    var outlineBox;

    // get existing outline box and check if it is in view
    if (!handleList.includes(MARKER_HANDLE.outline)) {
        outlineBox = drawObj.drawObjAry.length > drawObj.outlineIndex ? drawObj.drawObjAry[drawObj.outlineIndex] : null;

        if (outlineBox) {
            var cornersInView = rectCornerInView(outlineBox, cc);
            var checkOutline;  // outline candidate to be created in order

            // if the outline box is not in view, try to get a new outline box
            // if the outline is around the center or plot center, check if the original or the center outline box exist
            if (cornersInView === 0) {    // if not in view, get a new outline box
                if (outlineBox.outlineType === OutlineType.original) {
                    drawObj.originalOutlineBox = Object.assign({}, outlineBox);
                }

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
    } else {
        var {width, height, centerPt, unitType} = getMarkerImageSize(collectDrawobjAry(drawObj), cc);
        var angle = getMarkerAngleInRad(drawObj);

         outlineBox = createOutlineBoxAllSteps(pts[0], centerPt, width, height, unitType, cc, angle);
    }

    if (!outlineBox) return retval;
    retval.push(outlineBox);

    var rotAngle = getMarkerAngleInRad(drawObj);

    // add resize handles, TODO: test the case with both rotangle and resize handle
    if (get(drawObj, 'includeResize') && handleList.includes(MARKER_HANDLE.resize) &&
        outlineBox.outlineType === OutlineType.original) {
        createResizeHandle(outlineBox, cc, rotAngle).forEach((r) => retval.push(r));
     }

    // add rotate handle
    if (get(drawObj, 'includeRotate') && handleList.includes(MARKER_HANDLE.rotate)) {
        var rotateHandle = createRotateHandle(outlineBox, cc, rotAngle);

        if (rotateHandle) {
            retval.push(rotateHandle);
        }
    }

    return retval;
}

/**
 * create outline by trying to create the outline around all object, then the center, and last the plot center
 * stop creating the outline box around the center or the plot center if 'stopAt' is specified
 * @param fpCenter
 * @param outlineCenter
 * @param width
 * @param height
 * @param unitType
 * @param cc
 * @param angle
 * @param stopAt
 * @returns {*}
 */
function createOutlineBoxAllSteps(fpCenter, outlineCenter, width, height, unitType, cc, angle = 0.0, stopAt) {

    if (angle !== 0.0) {  // rotate the center around the footprint center
        var oCenter  = simpleRotateAroundPt(cc.getImageCoords(outlineCenter),
                                            cc.getImageCoords(fpCenter), -angle, Point.IM_PT);
        outlineCenter = getWorldOrImage(oCenter, cc);
    }

    var outlineBox = createOutlineBox(outlineCenter, width, height, unitType, cc, angle);

    if (outlineBox) {
        outlineBox.outlineType = OutlineType.original;
    } else if (!stopAt || stopAt !== OutlineType.center) {
        outlineBox = createOutlineBox(fpCenter, CENTER_BOX, CENTER_BOX, ShapeDataObj.UnitType.PIXEL, cc, angle);
        if (outlineBox) {
            outlineBox.outlineType = OutlineType.center;
        } else if (!stopAt || stopAt !== OutlineType.plotcenter) {
            var vCenter = makeViewPortPt(cc.viewPort.dim.width / 2, cc.viewPort.dim.height / 2);

            outlineBox = createOutlineBox(vCenter, CENTER_BOX, CENTER_BOX, ShapeDataObj.UnitType.PIXEL, cc, angle);
            outlineBox.outlineType = OutlineType.plotcenter;
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

    return rCorners.reduce((prev, handlerCenter) => {
        var handlerBox = ShapeDataObj.makeRectangleByCenter(getWorldOrImage(handlerCenter, cc),
            box.size[0], box.size[1], box.unit,
            0.0, ShapeDataObj.UnitType.ARCSEC, false);

        updateHandleRotAngle(rotAngle, handlerBox);
        set(handlerBox, 'color', HANDLE_COLOR);
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
    const [x1, x2, y1, y2] = [cc.viewPort.x, cc.viewPort.dim.width, cc.viewPort.y, cc.viewPort.dim.height];

    var rotateObj = null;
    var corners =  getRectCorners(outlineBox.pts[0], outlineBox.width, outlineBox.height, outlineBox.unitType, cc);
    var side = 4;
    var originVp = cc.getViewPortCoords(outlineBox.pts[0]);
    var vpCorners = corners.map((c) => {
        return simpleRotateAroundPt(cc.getViewPortCoords(c), originVp, rotAngle, Point.VP_PT);
    });
    var vpInView = vpCorners.map( (v) => cc.pointInViewPort(v) );

    var startIdx = has(outlineBox, 'rotateSide') ? outlineBox.rotateSide : 1;
    var endIdx = startIdx + side - 1;

    for (let idx = startIdx; idx <= endIdx; idx++) {
        var i = idx%side;
        var j = (i+1)%side;
        var ends;
        var [xlen, ylen] = [(vpCorners[j].x - vpCorners[i].x), (vpCorners[j].y - vpCorners[i].y)];

        if ( !vpInView[i] && !vpInView[j] ) continue;

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
        var hBottom = makeViewPortPt((ends[0].x + ends[1].x)/2, (ends[0].y + ends[1].y)/2);
        // center of the handle, rotate first if there is
        var hCenter = makeViewPortPt((ends[0].x + ends[1].x)/2 + handleCenter[i][0] * ROTATE_BOX,
                                     (ends[0].y + ends[1].y)/2 + handleCenter[i][1] * ROTATE_BOX);
        if (rotAngle !== 0) {
            hCenter = simpleRotateAroundPt(hCenter, hBottom, rotAngle, Point.VP_PT);
        }
        // test if all cornres of the handle after rotation are seen
        var hNotInView = cornerScreen.find ( (c) => {
            var vp = makeViewPortPt(hCenter.x + c[0] * ROTATE_BOX * 0.5, hCenter.y + c[1] * ROTATE_BOX * 0.5);
            var rVp = simpleRotateAroundPt(vp, hCenter, rotAngle, Point.VP_PT);

            return !(cc.pointInViewPort(rVp));
        });

        if (hNotInView) continue; // corners of handle are not seen

        // bottom center of the handle
        rotateObj = makePointDataObj(getWorldOrImage(hBottom, cc), ROTATE_BOX, DrawSymbol.ROTATE);

        // store the rotate angle and handle center
        rotateObj = Object.assign(rotateObj, {renderOptions: {rotAngle: (handleAngle[i%side]+rotAngle)},
                                              color: HANDLE_COLOR},
                                             {rotateCenter: hCenter});  // rotate center in viewport


        outlineBox.rotateSide = i;
        break;
    }
    return rotateObj;
}



/**
 * create outline box
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

        if (cc.pointInViewPort(rCorner)) {
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

    // consider to add the outline box which is plotcenter type in case no handles are included
    if (get(drawObj, 'includeResize', false) || get(drawObj, 'includeRotate', false)) {
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
            var dist = getScreenDistToMarker(lastObj, cc, screenPt);

            if (dist >= 0 && dist < distance) {
                distance = dist;
                prev = wIdx;
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
 * find the new rotation angle and outline box based on the given plot and region points of other plot
 * @param drawObj
 * @param cc
 */
function computeRotAngleOnPlot(drawObj, cc) {
    if (!get(drawObj, 'isRotable', false)) {
        drawObj.plotImageId = cc.plotImageId;
        return;
    }

    var crtObjAry = drawObj.drawObjAry.slice(0, drawObj.outlineIndex);
    var orgObjAry = FootprintFactory.getDrawObjFromOriginalRegion(drawObj.regions, drawObj.pts[0],
        drawObj.regions[0].isInstrument);

    var crtPt = cc.getImageCoords(crtObjAry[0].pts[0]);
    var orgPt = cc.getImageCoords(orgObjAry[0].pts[0]);
    var cImg = cc.getImageCoords(drawObj.pts[0]);

    var getRotateAngle = (crtPt, orgPt, cImg) => {
        var [x_o, y_o, x_c, y_c] = [orgPt.x - cImg.x, orgPt.y - cImg.y, crtPt.x - cImg.x, crtPt.y - cImg.y];
        var z = (x_o * y_c - y_o * x_c) > 0 ? 1 : -1;
        var innerProd = (x_o * x_c + y_o * y_c) / (Math.sqrt(x_o * x_o + y_o * y_o) * Math.sqrt(x_c * x_c + y_c * y_c));

        innerProd = innerProd > 1.0 ? 1.0 : innerProd < -1.0 ? -1.0 : innerProd;

        return -Math.acos(innerProd) * z;
    };

    var angle = getRotateAngle(crtPt, orgPt, cImg);

    drawObj.angle = 0.0;

    // reset the region object to be the original one
    orgObjAry.forEach( (obj, idx) => {
        obj.isMarker = true;
        drawObj.drawObjAry[idx] = obj;
    });

    // find outline box around the original region objects with the new rotate angle
    drawObj = Object.assign(drawObj, {angle, angleUnit: ANGLE_UNIT.radian});

    var newOutline = updateHandle(drawObj, cc, [MARKER_HANDLE.outline]);

    if (!isEmpty(newOutline)) {
        drawObj.drawObjAry[drawObj.outlineIndex] = newOutline[0];
    }

    // recover the region objects to be the one with the rotation angle
    crtObjAry.forEach((obj, index) => {
        if (get(obj, 'isMarker', false)) {
            drawObj.drawObjAry[index] = obj;
            if (obj.sType === ShapeDataObj.ShapeType.Rectangle || obj.sType === ShapeDataObj.ShapeType.Ellipse) {
                set(obj, 'renderOptions.rotAngle', angle);
            }
        }
    });

    drawObj.plotImageId = cc.plotImageId;
}
/**
 * draw the object which contains drawObj array
 * @param drawObjP
 * @param ctx
 * @param drawTextAry
 * @param plot
 * @param def
 * @param vpPtM
 * @param onlyAddToPath
 */
export function drawMarkerObject(drawObjP, ctx, drawTextAry, plot, def, vpPtM, onlyAddToPath) {
    if (has(drawObjP, 'drawObjAry')) {

        //markerTextOffset(drawObj, plot);
        //var drawObj = cloneDeep(drawObjP);

        var drawObj = Object.assign({}, drawObjP);
        drawObj.drawObjAry = drawObjP.drawObjAry.map( (obj) => Object.assign({}, obj) );

        // draw the same objects on multiple plots
        if (get(drawObj, 'plotImageId') !== plot.plotImageId && get(drawObj, 'isRotable', false)) {
            computeRotAngleOnPlot(drawObj, plot);   // update the rotate angle of outline box depending on the plot
        }

        var newObj = Object.assign({}, drawObj, {drawObjAry: drawObj.drawObjAry.slice(0, drawObj.outlineIndex)});

        // add outline box, resize and rotate handle if any is included
        if (get(drawObj, 'includeResize', false) ||
            get(drawObj, 'includeRotate', false) ||
            get(drawObj, 'includeOutline', false)) {
            newObj.drawObjAry = newObj.drawObjAry.concat(updateHandle(drawObj, plot,
                                                         [MARKER_HANDLE.resize, MARKER_HANDLE.rotate]));
        }

        // newObj is made for display
        updateColorFromDef(newObj, def);
        newObj.drawObjAry.forEach((oneDrawObj) => {
            DrawOp.draw(oneDrawObj, ctx, drawTextAry, plot, def, vpPtM, onlyAddToPath);
        });

        drawFootprintText(newObj, plot, def, drawTextAry);
        set(drawObjP, ['textWorldLoc', plot.plotImageId], newObj.textWorldLoc);
    }
}

/**
 * draw the text attached with footprint drawobj by text drawing function in ShapeDataboj
 * @param drawObj
 * @param plot
 * @param def
 * @param drawTextAry
 */
function drawFootprintText(drawObj, plot, def, drawTextAry) {

    var drawParams = makeDrawParams(drawObj, def);
    var {text, textLoc} = drawObj;
    var {fontSize} = drawParams;

    if (isNil(text)) return;

    var outlineObj = (drawObj.drawObjAry.length > drawObj.outlineIndex) ?
                      drawObj.drawObjAry[drawObj.outlineIndex] :
                      (updateHandle(drawObj, plot, []))[0];

   // if (!outlineObj || outlineObj.outlineType === OutlineType.plotcenter) return;

    if (!outlineObj) return;

    var objArea  = getObjArea(outlineObj, plot); // in image coordinate

    if (objArea) {
        var textPt = makeTextLocationComposite(plot, textLoc, fontSize,
                        objArea.width * plot.zoomFactor,
                        objArea.height * plot.zoomFactor,
                        objArea.centerPt);
        if (textPt) {
            drawText(drawObj, drawTextAry, plot, textPt, drawParams);
        }
    }
}

/**
 * update the footprint outline box once some object is clicked to be selected
 * @param drawObj
 * @param cc
 * @returns {*}
 */
export function updateFootprintOutline(drawObj, cc) {

    if (drawObj.plotImageId !== cc.plotImageId) {
        computeRotAngleOnPlot(drawObj, cc);
    }

    updateOutlineBox(drawObj, cc, true);

    return Object.assign({}, drawObj);
}

/**
 * update the outline box after translation and rotation operation.
 * @param drawObj
 * @param upgradeOutline
 * @param cc
 */
function updateOutlineBox(drawObj, cc, upgradeOutline = false) {
    var {outlineIndex, drawObjAry} = drawObj;

    if (outlineIndex && drawObjAry && drawObjAry.length > outlineIndex) {
        var outlineObj = updateHandle(drawObj, cc, [], upgradeOutline);

        if (!isEmpty(outlineObj)) {
            drawObjAry[outlineIndex] = outlineObj[0];
        } else {
            console.log('error on creating outline');
        }
    }
}

/**
 * update the translation information of the footprint or marker, stored in world coordinate or image coordinate
 * is used in case world coordinate is not available. The computation is made on image coordinate
 * @param drawObj
 * @param cc
 * @param apt
 * @param isSet set or incrment the translation
 */
export function updateFootprintTranslate(drawObj, cc, apt, isSet = false) {
    var tx = lengthSizeUnit(cc, apt.x, apt.type);
    var ty = lengthSizeUnit(cc, apt.y, apt.type);
    var deltaX, deltaY;

    if (!isSet && has(drawObj, 'translation')) {
        deltaX = tx.len;
        deltaY = ty.len;
        tx.len += drawObj.translation.x;   // add the increment
        ty.len += drawObj.translation.y;
    } else {
        deltaX = tx.len - get(drawObj, 'translation.x', 0.0);  // set absolutely
        deltaY = ty.len - get(drawObj, 'translation.y', 0.0);

    }

    var newApt = {x: tx.len, y: ty.len, type: tx.unit};
    var newObj = translateMarker(drawObj, cc, {x: deltaX, y: deltaY, type: tx.unit});

    updateOutlineBox(newObj, cc);
    return clone(newObj, {translation: newApt});
}

/**
 * update object rotate angle
 * @param drawObj
 * @param angle
 * @param angleUnit
 * @param plot
 * @param worldPt  certer point to rotate around
 * @param isSet set or increment the angle
 * @returns {*}
 */
export function updateFootprintDrawobjAngle(drawObj, plot, worldPt, angle = 0.0, angleUnit = ANGLE_UNIT.radian,  isSet = false) {
    var newAngle = convertAngle(angleUnit.key, 'radian', angle);
    var deltaAngle;

    var crtAngle = getMarkerAngleInRad(drawObj);

    // get current angle status
    if (!isSet) {
        deltaAngle = newAngle;
        newAngle += crtAngle;
    } else {
        deltaAngle = newAngle - crtAngle;
    }

    var newObj = rotateMarkerAround(drawObj, plot, deltaAngle, worldPt);
    while (newAngle < -Math.PI) {
        newAngle += 2 * Math.PI;
    }
    while (newAngle > Math.PI) {
        newAngle -= 2 * Math.PI;
    }

    updateOutlineBox(newObj, plot);
    return clone(newObj, {angle: newAngle, angleUnit: ANGLE_UNIT.radian});
}



/**
 * translate the marker by apt on image coordinate
 * @param plot
 * @param drawObj
 * @param apt
 * @returns {*} an array of translated objects contained in drawObj
 */
export function translateMarker(drawObj, plot, apt) {
    var deltaImgX = lengthToImagePixel(apt.x, plot, apt.type);
    var deltaImgY = lengthToImagePixel(apt.y, plot, apt.type);


    var moveFrom = (pti, deltaX, deltaY) => {   // translate on image coordinate
        return makeImagePt(pti.x + deltaX, pti.y + deltaY);
    };

    var newPti = moveFrom(plot.getImageCoords(drawObj.pts[0]), deltaImgX, deltaImgY);
    var newObj = clone(drawObj, {pts: [makePoint(newPti, plot, drawObj.pts[0].type)]});
    var dObjAry = collectDrawobjAry(drawObj, [MARKER_HANDLE.outline]);

    newObj.drawObjAry = dObjAry.reduce( (prev, oneDrawobj) => {
         prev.push(DrawOp.translateTo(oneDrawobj, plot, apt));
        return prev;
    }, [] );

    if (get(newObj, 'originalOutlineBox', null)) {
        newObj.originalOutlineBox = DrawOp.translateTo(drawObj.originalOutlineBox, plot, apt);
    }

    return newObj;
}


/**
 * rotate a marker around worldPt by angle on image coordinate
 * @param plot
 * @param drawObj
 * @param angle,  screen coordinate direction, in radian
 * @param worldPt
 * @returns {*} a array of rotated objects contained in drawObj
 */
export function rotateMarkerAround(drawObj,plot, angle, worldPt) {
    var {pts} = drawObj;
    if (!pts) return null;

    var worldImg = plot.getImageCoords(worldPt);
    var drawObjPt = plot.getImageCoords(pts[0]);

    var rotateAroundPt = (imgPt) => {   // rotate around given worldPt on immage coordinate
        var x1 = imgPt.x - worldImg.x;
        var y1 = imgPt.y - worldImg.y;
        var cos = Math.cos(-angle);
        var sin = Math.sin(-angle);

        var x2 = x1 * cos - y1 * sin + worldImg.x;
        var y2 = x1 * sin + y1 * cos + worldImg.y;

        return getWorldOrImage(makeImagePt(x2, y2), plot);
    };

    var newObj = clone(drawObj, {pts: [rotateAroundPt(drawObjPt)]});
    var dAry = collectDrawobjAry(drawObj, [MARKER_HANDLE.outline]);

    newObj.drawObjAry = dAry.reduce((prev, oneObj) => {
        var centerPt = has(oneObj, 'outlineType')&&oneObj.outlineType === OutlineType.plotcenter ?
                       oneObj.pts[0] : worldPt;
        var rObj = DrawOp.rotateAround(oneObj, plot, angle, centerPt);

        prev.push(rObj);
        return prev;
    }, []);

    if (get(drawObj, 'originalOutlineBox', null)) {
        newObj.originalOutlineBox = DrawOp.rotateAround(drawObj.originalOutlineBox, plot, angle, worldPt);
    }

    return newObj;
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

    if (drawObjAry && drawObjAry.length > 0) {
        var {size} = newSize;
        var radius = lengthSizeUnit(cc, Math.min(size[0], size[1])/2, newSize.unitType);

        newObj = Object.assign(newObj, {width: radius.len, height: radius.len, unitType: radius.unit });
        newObj.drawObjAry = [clone(drawObjAry[0], {radius: radius.len, unitType: radius.unit})];

        var outline = updateHandle(newObj, cc, [MARKER_HANDLE.outline]);
        if (outline) {
            newObj.drawObjAry.push(outline[0]);
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
        var textImgLoc = get(drawObj, ['textWorldLoc', plot.plotImageId], null);

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

    // if the rectangle is slanted, then find the distance by usiung distToPolygon
    var distToRect = (corners, rotAngle, centerPt) => {
        if (rotAngle !== 0.0) {
            var rCorners = corners.reduce( (prev, c) => {
                prev.push(simpleRotateAroundPt(c, centerPt, rotAngle, Point.SPT));
                return prev;
            }, []);

            return isWithinPolygon(pt, rCorners, plot) ? 0 : distToPolygon(rCorners);
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

    var distToPolygon = (pts) => {
        var vertices = [...pts, pts[0]].map((onePt) => plot.getScreenCoords(onePt));
        var totalV = pts.length;

        return vertices.reduce((prev, ver, index) => {
            if (index < totalV) {
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

             cScreen = plot.getScreenCoords(drawObj.pts[0]);
             distance = distToPt(cScreen.x, cScreen.y);
             distance = distance > r ? distance - r : 0;
         } else if (drawObj.sType === ShapeDataObj.ShapeType.Polygon) {
             var inside = isWithinPolygon(pt, drawObj.pts, plot);

             distance = inside ? 0 : distToPolygon(drawObj.pts);
         }
    }
    return distance;
}


