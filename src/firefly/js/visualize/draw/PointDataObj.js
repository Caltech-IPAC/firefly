/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import DrawObj from './DrawObj.js';
import DrawUtil from './DrawUtil.js';
import {TextLocation, DEFAULT_FONT_SIZE} from './DrawingDef.js';
import Point, {makeOffsetPt, makeScreenPt} from '../Point.js';
import CsysConverter, {CCUtil} from '../CsysConverter.js';
import {RegionType, regionPropsList} from '../region/Region.js';
import {startRegionDes, setRegionPropertyDes} from '../region/RegionDescription.js';
import ShapeDataObj, {fontHeight, drawText, translateTo, rotateAround} from './ShapeDataObj.js';
import {isWithinPolygon, makeShapeHighlightRenderOptions, DELTA, defaultDashline} from './ShapeHighlight.js';
import { handleTextFromRegion } from './ShapeToRegion.js';
import VisUtil from '../VisUtil.js';
import {isNil, isEmpty, has, set, cloneDeep} from 'lodash';
import {defaultRegionSelectColor, defaultRegionSelectStyle} from '../DrawLayerCntlr.js';



/**
 *  enum
 *  one of 'X','SQUARE','CROSS','DIAMOND','DOT','CIRCLE', 'SQUARE_X', 'EMP_CROSS','EMP_SQUARE_X', 'BOXCIRCLE', 'ARROW'
 * */
export const DrawSymbol = new Enum([
    'X','SQUARE','CROSS','DIAMOND','DOT','CIRCLE', 'SQUARE_X', 'EMP_CROSS','EMP_SQUARE_X',
    'BOXCIRCLE', 'ARROW', 'ROTATE'
], { ignoreCase: true });

export const POINT_DATA_OBJ= 'PointDataObj';
const DEFAULT_SIZE= 4;
const DOT_DEFAULT_SIZE = 1;
const DEFAULT_SYMBOL = DrawSymbol.X;

/**
 * drawObj for point, optional: textLoc
 * @param {number} [size]
 * @param {{x:name,y:name,type:string}} pt
 * @param {Enum} [symbol]
 * @param {string} [text]
 * @return {object}
 */
export function make(pt,size,symbol,text) {
    if (!pt) return null;

    var obj= DrawObj.makeDrawObj();
    obj.type= POINT_DATA_OBJ;
    obj.pt= pt;

    if (size) obj.size= size;
    if (symbol) obj.symbol= symbol;
    if (text) obj.text= text;
    return obj;
}

export function makePointDataObj(pt, size, symbol, text) {
    return make(pt, size, symbol, text);
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
        if (!drawObj.symbol) return true;
        const s= DrawSymbol.get(drawObj.symbol);
        return s!==DrawSymbol.EMP_CROSS && s!==DrawSymbol.EMP_SQUARE_X;
    },

    getCenterPt(drawObj) {return drawObj.pt; },

    getScreenDist(drawObj, plot, pt) {
        var dist = -1;
        var testPt= plot ? CCUtil.getScreenCoords(plot,drawObj.pt) : drawObj.pt;

        if (testPt && testPt.type===Point.SPT) {
            var dx= pt.x - testPt.x;
            var dy= pt.y - testPt.y;
            dist= Math.sqrt(dx*dx + dy*dy);
        }

        return dist;
    },

    draw(drawObj,ctx,drawTextAry, plot,def,vpPtM,onlyAddToPath) {
        var drawParams= makeDrawParams(drawObj,def);
        drawPt(ctx,drawTextAry,drawObj.pt, plot, drawObj, drawParams,  drawObj.renderOptions,vpPtM,onlyAddToPath);
    },

    toRegion(drawObj,plot, def) {
        var drawParams= makeDrawParams(drawObj,def);

        return toRegion(drawObj.pt, plot, drawObj, drawParams,drawObj.renderOptions);
    },

    translateTo(drawObj,plot, apt) {
        return Object.assign({}, drawObj, {...translatePtTo(plot,drawObj, apt)});
    },

    rotateAround(drawObj, plot, angle, worldPt) {
        return Object.assign({}, drawObj, {...rotatePtAround(plot, drawObj, angle, worldPt)});
    },

    makeHighlight(drawObj, plot, def = {}) {
        return makeHighlightPointDataObj(drawObj,  CsysConverter.make(plot), def);
    },

    isScreenPointInside(screenPt, drawObj, plot, def = {}) {
        return isInPointDataobj(drawObj,  CsysConverter.make(plot), screenPt);
    }
};

export default {make,draw};

////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////

/**
 * translate the point symbol
 * @param {WebPlot} plot
 * @param drawObj
 * @param apt
 * @returns {{pt: *}}
 */
function translatePtTo(plot, drawObj, apt) {
    var tPt = translateTo(plot, [drawObj.pt], apt);

    return {pt: tPt[0]};  // use translateTo in shapedataobj
}

/**
 * rotate the point symbol (rotate the point defined for the point, not the entire symbol)
 * if the entire symbol needs to be rotated, set the angle to renderOptions.rotAngle externally
 * @param {WebPlot} plot
 * @param drawObj
 * @param {number} angle in screen coodinate direction, radian
 * @param {WorldPt} worldPt
 * @returns {{pt: *}}
 */
function rotatePtAround(plot, drawObj, angle, worldPt) {
    var rPt = rotateAround(plot, [drawObj.pt], angle, worldPt);

     return {pt: rPt[0]}; // use rotateAround in shapedataobj
}


function makeDrawParams(pointDataObj,def) {
    const symbol= DrawSymbol.get(pointDataObj.symbol || def.symbol || DEFAULT_SYMBOL);
    const size= (symbol===DrawSymbol.DOT) ? pointDataObj.size || def.size || DOT_DEFAULT_SIZE :
                                          pointDataObj.size || def.size || DEFAULT_SIZE;
    const fontName= pointDataObj.fontName || def.fontName || 'helvetica';
    const fontSize= pointDataObj.fontSize || def.fontSize || DEFAULT_FONT_SIZE;
    const fontWeight= pointDataObj.fontWeight || def.fontWeight || 'normal';
    const fontStyle= pointDataObj.fontStyle || def.fontStyle || 'normal';
    const textLoc= pointDataObj.textLoc || def.textLoc || TextLocation.DEFAULT;

    return {
        color: DrawUtil.getColor(pointDataObj.color,def.color),
        textLoc,
        fontName,
        fontSize,
        fontWeight,
        fontStyle,
        size,
        symbol
    };
}

/**
 *
 * @param ctx
 * @param drawTextAry
 * @param pt
 * @param {WebPlot} plot
 * @param drawObj
 * @param drawParams
 * @param renderOptions
 * @param vpPtM
 * @param onlyAddToPath
 */
function drawPt(ctx, drawTextAry, pt, plot, drawObj, drawParams, renderOptions, vpPtM, onlyAddToPath) {
    if (!pt) return;

    if (!plot || pt.type===Point.SPT) {
        drawXY(ctx,drawTextAry, pt, plot, drawObj, drawParams, renderOptions, onlyAddToPath);
    }
    else {
        var vpPt;
        if (vpPtM && pt.type===Point.W_PT) {
            var success= plot.getViewPortCoordsOptimize(pt,vpPtM);
            vpPt= success ? vpPtM : null;
        }
        else {
            vpPt=plot.getViewPortCoords(pt);
        }
        if (plot.pointInViewPort(vpPt)) {
            drawXY(ctx,drawTextAry, vpPt, plot, drawObj, drawParams, renderOptions, onlyAddToPath);
        }
    }
}



/**
 * get the covered area of the point object in screen pixel coordinate, no rotation is considered
 * size means:
 * circle: radius, square, square_x, diamond, cross, x, boxcircle: half of width (height)
 * dot: width, arrow: height, width
 * rotate: the shape is a stick with a rotation mark at the one end,
 *         width, height is the size, and pt is located at the other end of the stick
 * @param drawObj
 * @param cc
 * @returns {*} pixel and size in screen pixel
 */
export function getPointDataobjArea(drawObj, cc) {
    var {size, symbol, pt} = drawObj;
    var width, height;
    var spt = cc.getScreenCoords(pt);

    if (!spt) return {};

    switch(symbol) {
        case DrawSymbol.BOXCIRCLE:
        case DrawSymbol.CIRCLE:
        case DrawSymbol.EMP_SQUARE_X:
        case DrawSymbol.EMP_CROSS :
            width = (size + 2) * 2;
            break;
        case DrawSymbol.SQUARE :
        case DrawSymbol.SQUARE_X :
        case DrawSymbol.DIAMOND :
        case DrawSymbol.CROSS :
        case DrawSymbol.X :
            width = size * 2;
            break;
        case DrawSymbol.DOT :
            width = size ? size : 2;
            break;
        case DrawSymbol.ARROW :  // pt is set for right lower corner
            width = Math.floor((size+1)/2) * 2;
            spt.x -= width/2;
            spt.y -= width/2;
            break;
        case DrawSymbol.ROTATE:
            width = size;
            spt.x += width/2;
            break;
        default:
            width = size * 2;
    }

    height = width;
    return {centerPt: spt, width, height};
}
/**
 * calculate the text location based on text location, offset, font size, point size and point symbol.
 * @param drawObj
 * @param plot
 * @param textLoc
 * @param fontSize
 * @returns {*}
 */
function makeTextLocationPoint(drawObj, plot, textLoc, fontSize) {
    var area = getPointDataobjArea(drawObj, plot);   // in screen pixel
    if (!area) return null;

    var {centerPt, width, height} = area;
    var fHeight = fontHeight(fontSize) + 4;
    var opt;

    switch(textLoc) {
        case TextLocation.REGION_NE:
            opt = makeOffsetPt(-width/2, -height/2 - fHeight);
            break;
        case TextLocation.REGION_NW:
            opt = makeOffsetPt(width/2, -height/2 - fHeight);
            break;
        case TextLocation.REGION_SE:
            opt = makeOffsetPt(-width/2, height/2 + fHeight);
            break;
        case TextLocation.REGION_SW:
            opt = makeOffsetPt(width/2, height/2 + fHeight);
            break;
        default:
            opt = makeOffsetPt(0, 0);
    }

    return makeScreenPt(centerPt.x + opt.x, centerPt.y + opt.y);
}


function drawXY(ctx, drawTextAry, pt, plot, drawObj, drawParams,renderOptions, onlyAddToPath) {
    var {color, textLoc, fontName, fontSize, fontWeight, fontStyle}= drawParams;
    var {text, textOffset} = drawObj;


    drawSymbolOnPlot(ctx, pt.x, pt.y, drawParams,renderOptions, onlyAddToPath);
    if (!text)  return;

    if (isNil(color)) {
        color = 'black';
    }

    var vpt;

    if (textLoc) {
        vpt = plot.getViewPortCoords(makeTextLocationPoint(drawObj, plot, textLoc, fontSize));
    } else {
        vpt = plot.getViewPortCoords(pt);
    }
    if (textOffset && (textOffset.x !== 0.0 || textOffset.y !== 0.0)) {
        drawText(drawObj, drawTextAry, plot, vpt, drawParams);
    } else {
        DrawUtil.drawText(drawTextAry, text, vpt.x, vpt.y, color, renderOptions,
                          fontName, fontSize, fontWeight, fontStyle);
    }
 }

function drawSymbolOnPlot(ctx, x, y, drawParams, renderOptions, onlyAddToPath) {
    var {color,size}= drawParams;
    switch (drawParams.symbol) {
        case DrawSymbol.X :
            DrawUtil.drawX(ctx, x, y, color, size, renderOptions, onlyAddToPath);
            break;
        case DrawSymbol.EMP_CROSS :
            DrawUtil.drawEmpCross(ctx, x, y, color,size,renderOptions,   'white');
            break;
        case DrawSymbol.EMP_SQUARE_X:
            DrawUtil.drawEmpSquareX(ctx, x, y, color, size,renderOptions,  'black', 'white');
            break;
        case DrawSymbol.CROSS :
            DrawUtil.drawCross(ctx, x, y, color, size,renderOptions,  onlyAddToPath);
            break;
        case DrawSymbol.SQUARE :
            DrawUtil.drawSquare(ctx, x, y, color, size,renderOptions,  onlyAddToPath);
            break;
        case DrawSymbol.SQUARE_X :
            DrawUtil.drawSquareX(ctx, x, y, color, size,renderOptions,  onlyAddToPath);
            break;
        case DrawSymbol.DIAMOND :
            DrawUtil.drawDiamond(ctx, x, y, color, size,renderOptions,  onlyAddToPath);
            break;
        case DrawSymbol.DOT :
            DrawUtil.drawDot(ctx, x, y, color, size,renderOptions,  onlyAddToPath);
            break;
        case DrawSymbol.CIRCLE :
            DrawUtil.drawCircle(ctx, x, y, color, 1, size, renderOptions, onlyAddToPath);
            break;
        case DrawSymbol.BOXCIRCLE :
            DrawUtil.drawBoxcircle(ctx, x, y, color, size, renderOptions, onlyAddToPath);
            break;
        case DrawSymbol.ARROW :
            DrawUtil.drawArrow(ctx, x, y, color, size, renderOptions, onlyAddToPath);
            break;
        case DrawSymbol.ROTATE:
            DrawUtil.drawRotate(ctx, x, y, color, size, renderOptions, onlyAddToPath);
            break;
        default :
            break;
    }
}

function toRegion(pt, plot, drawObj, drawParams, renderOptions) {
    var {size, symbol, color}= drawParams;
    var pointType;
    var des;
    var cc = CsysConverter.make(plot);
    var wpt = pt; // cc.getWorldCoords(pt); keep the original coordinate system in description
    var retList = [];

    switch (symbol) {
        case DrawSymbol.X :
            pointType = 'x';
            break;
        case DrawSymbol.EMP_CROSS :
        case DrawSymbol.CROSS :
            pointType = 'cross';
            break;
        case DrawSymbol.SQUARE :
        case DrawSymbol.SQUARE_X :
        case DrawSymbol.EMP_SQUARE_X:
            pointType = 'box';
            break;
        case DrawSymbol.DIAMOND :
            pointType = 'diamond';
            break;
        case DrawSymbol.DOT :
            size = 2;
            pointType = 'box';
            break;
        case DrawSymbol.CIRCLE :
            pointType = 'circle';
            break;
        case DrawSymbol.BOXCIRCLE :
            pointType = 'boxcircle';
            break;
        case DrawSymbol.ARROW :
            pointType = 'arrow';
            break;
        case DrawSymbol.ROTATE:
            return retList;   // no region
            break;
        default:
            pointType = 'box';
    }


    des = startRegionDes(RegionType.point, cc, [wpt]);
    if (isEmpty(des)) return retList;

    var s = (size && pointType &&
              (symbol !== DrawSymbol.DOT && pointType !== 'arrow')) ? size*2 : size;

    des += setRegionPropertyDes(regionPropsList.COLOR, color) +
           setRegionPropertyDes(regionPropsList.PTTYPE, {pointType, pointSize: s});

    handleTextFromRegion(retList, des, drawObj, drawParams, RegionType.point, cc);


    return retList;

}

/**
 * @summary render highlight on top of region
 * @param drawObj
 * @param color
 * @param style
 * @param lineW
 * @returns {*}
 */
function makeHighlightOnPoint(drawObj, color, style, lineW) {
    var newDrawObj = cloneDeep(drawObj);

    if (style.includes('Dotted')) {
        set(newDrawObj, 'renderOptions', { lineDash: defaultDashline });
    }
    set(newDrawObj, 'color', color );
    set(newDrawObj, 'lineWidth', (lineW <= 0 ? get(oneObj, 'lineWidth', 1) : lineW) );

    newDrawObj.symbol = DrawSymbol.get(drawObj.symbol.key);
    newDrawObj.isRendered = 1;

    return newDrawObj;
}


/**
 * @summary make drawobj to highlight PointDataObj
 * @param {Object} drawObj
 * @param {Object} cc
 * @param {Object} def
 * @returns {Object} inside or not and the distance to the point center
 */
export function makeHighlightPointDataObj(drawObj, cc, def) {
    var color = def && has(def, 'selectColor') ? def.selectColor : defaultRegionSelectColor;
    var style = def && has(def, 'selectStyle') ? def.selectStyle : defaultRegionSelectStyle;

    if (style !== defaultRegionSelectStyle) {
        return makeHighlightOnPoint(drawObj, color, style, (def&&has(def, 'lineWidth') ? def.lineWidth : 0));
    }

    var area = getPointDataobjArea(drawObj, cc);
    var w = ((DELTA + 1) * 2 + area.width);
    var h = ((DELTA + 1) * 2 + area.height);
    var wCenter = cc.getWorldCoords(area.centerPt);

    var rectObj = ShapeDataObj.makeRectangleByCenter(wCenter, w, h, ShapeDataObj.UnitType.PIXEL,
                                                     0.0, ShapeDataObj.UnitType.ARCSEC, false);

    makeShapeHighlightRenderOptions( rectObj, color );
    return rectObj;
}

/**
 * check if a specified point is within PointDataobj
 * @param drawObj
 * @param cc
 * @param pt
 * @returns {{inside: *, dist: *}}
 */
export function isInPointDataobj(drawObj, cc, pt) {
    var sPt = cc.getScreenCoords(pt);
    var area = getPointDataobjArea(drawObj, cc);
    var {centerPt, width: w, height: h} = area;
    var dist;

    var inside = isWithinPolygon(sPt,
                                [ makeScreenPt(centerPt.x - w/2, centerPt.y - h/2),
                                  makeScreenPt(centerPt.x + w/2, centerPt.y - h/2),
                                  makeScreenPt(centerPt.x + w/2, centerPt.y + h/2),
                                  makeScreenPt(centerPt.x - w/2, centerPt.y + h/2)], cc);
    if (inside) {
        dist = VisUtil.computeScreenDistance(sPt.x, sPt.y, centerPt.x, centerPt.y);
    }

    return {inside, dist};
}

