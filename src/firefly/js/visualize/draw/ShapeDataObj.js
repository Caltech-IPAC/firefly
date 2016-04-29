
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import validator from 'validator';
import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil';
import VisUtil, {convertAngle} from '../VisUtil.js';
import {TextLocation, Style, DEFAULT_FONT_SIZE} from './DrawingDef.js';
import {makeScreenPt, makeViewPortPt, makeOffsetPt} from '../Point.js';
import {has, isNil} from 'lodash';



const FONT_FALLBACK= ',sans-serif';
const HTML_DEG= '&deg;';

var UnitType= new Enum(['PIXEL','ARCSEC','IMAGE_PIXEL']);
var ShapeType= new Enum(['Line', 'Text','Circle', 'Rectangle', 'Ellipse']);




const SHAPE_DATA_OBJ= 'ShapeDataObj';
const DEF_WIDTH = 1;


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

function makeRectangleByCenter(pt1, width, height, unitType=UnitType.PIXEL, angle = 0.0, angleUnit = UnitType.ARCSEC) {
    return Object.assign(make(ShapeType.Rectangle), {pts:[pt1], width, height, unitType, angle, angleUnit});
}

function makeEllipse(pt1, radius1, radius2, unitType=UnitType.PIXEL, angle = 0.0, angleUnit = UnitType.ARCSEC) {
    return Object.assign(make(ShapeType.Ellipse), {pts:[pt1], radius1, radius2, unitType, angle, angleUnit});
}

/**
 *
 * @param pt
 * @param text
 * @return {*}
 */
function makeText(pt, text) {
    return Object.assign(make(ShapeType.Text), {pts:[pt],text});
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
    return {
        color: DrawUtil.getColor(drawObj.color,def.color),
        lineWidth,
        textLoc,
        unitType,
        style,
        fontName,
        fontSize,
        fontWeight,
        fontStyle
    };
}


var draw=  {

    usePathOptimization(drawObj,def) {
        var dp= makeDrawParams(drawObj,def);
        return dp==Style.STANDARD &&
            (drawObj.sType==ShapeType.Line || drawObj.sType==ShapeType.Rectangle);
    },

    getCenterPt(drawObj) {
        return drawObj.pts[0];
    },

    getScreenDist(drawObj,plot, pt) {
        var dist = -1;
        var testPt;
        if (drawObj.sType==ShapeType.Rectangle) {
            testPt = getRectangleCenterScreenPt(drawObj,plot);
        } else {
            testPt = plot.getScreenCoords(drawObj.pts[0]);
        }
        if (testPt) {
            var dx= pt.x - testPt.x;
            var dy= pt.y - testPt.y;
            dist= Math.sqrt(dx*dx + dy*dy);
        }
        return dist;
    },

    draw(drawObj,ctx,drawTextAry,plot,def,vpPtM,onlyAddToPath) {
        var drawParams= makeDrawParams(drawObj,def);
        drawShape(drawObj,ctx,drawTextAry,plot,drawParams,onlyAddToPath);
    },

    toRegion(drawObj,plot, def) {
        var drawParams= makeDrawParams(drawObj,def);
        toRegion(drawObj,plot,drawParams);
    },

    translateTo(drawObj,plot, apt) {
        return translateTo(plot,drawObj.pts,apt);
    },

    rotateAround(drawObj, plot, angle, worldPt) {
        return rotateAround(plot,drawObj.pts,angle,worldPt);
    }
};

export default {
    make,draw,makeLine,makeCircle,makeCircleWithRadius,
    makeRectangleByCorners,makeText, makeRectangleByCenter,
    makeTextWithOffset,makeRectangle, makeEllipse, SHAPE_DATA_OBJ, UnitType
};



function getRectangleCenterScreenPt(drawObj,plot,unitType) {
    var pt0, pt1;
    var w, h;
    var {width,height,pts}= drawObj;
    if (pts.length===1 && width && height) {
        pt0 = plot.getScreenCoords(pts[0]);

        switch (unitType) {
            case UnitType.PIXEL:
                w= width;
                h= height;
                break;
            case UnitType.ARCSEC:
                w= getValueInScreenPixel(plot,width);
                h= getValueInScreenPixel(plot,height);
                break;
            case UnitType.IMAGE_PIXEL:
                var scale= plot.zoomFactor;
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

function getValueInScreenPixel(plot, arcsecValue) {
    var retval= plot ?
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
function drawShape(drawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath) {

    var {sType,pts}= drawObj;
    switch (sType) {
        case ShapeType.Text:
            drawText(drawObj,drawTextAry,plot,pts[0], drawParams);
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
function drawLine(drawObj, ctx, drawTextAry, plot, drawParams, onlyAddToPath) {
    var {pts, text, renderOptions}= drawObj;
    var {style, color, lineWidth, textLoc, fontSize}= drawParams;

    var inView= false;
    var pt0= plot.getViewPortCoords(pts[0]);
    var pt1= plot.getViewPortCoords(pts[1]);
    if (!pt0 || !pt1) return;
    if (plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1)) {
        inView= true;
        if (!onlyAddToPath || style===Style.HANDLED) {
            DrawUtil.beginPath(ctx, color,lineWidth, renderOptions);
        }
        ctx.moveTo(pt0.x, pt0.y);
        ctx.lineTo(pt1.x, pt1.y);
        if (!onlyAddToPath || style===Style.HANDLED) DrawUtil.stroke(ctx);
    }

    if (text && inView) {
        const textLocPt= makeTextLocationLine(plot, textLoc, fontSize,pts[0], pts[1]);
        drawText(drawObj, drawTextAry, plot, plot.getViewPortCoords(textLocPt),
                drawParams);
    }

    if (style==Style.HANDLED && inView) {
        DrawUtil.fillRec(ctx, color,pt0.x-2, pt0.y-2, 5,5, renderOptions);
        DrawUtil.fillRec(ctx, color,pt1.x-2, pt1.y-2, 5,5, renderOptions);
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
    var {pts, text, radius, renderOptions}= drawObj;
    var {color, lineWidth, textLoc, unitType,}= drawParams;


    var inView= false;
    var screenRadius= 1;
    var centerPt=null;

    if (pts.length==1 && !isNil(radius)) {
        switch (unitType) {
            case UnitType.PIXEL: screenRadius= radius;
                break;
            case UnitType.IMAGE_PIXEL: screenRadius= (plot.zoomFactor*radius);
                break;
            case UnitType.ARCSEC: screenRadius= getValueInScreenPixel(plot,radius);
                break;
        }
        centerPt= plot.getViewPortCoords(pts[0]);
        if (plot.pointInViewPort(centerPt)) {
            DrawUtil.drawCircle(ctx,centerPt.x, centerPt.y,color,lineWidth,
                                screenRadius,renderOptions,false);
            inView= true;
        }
    }
    else {
        const pt0= plot.getViewPortCoords(pts[0]);
        const pt1= plot.getViewPortCoords(pts[1]);
        if (!pt0 || !pt1) return;
        if (plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1)) {
            inView= true;

            const xDist= Math.abs(pt0.x-pt1.x)/2;
            const yDist= Math.abs(pt0.y-pt1.y)/2;
            screenRadius= Math.min(xDist,yDist);

            const x= Math.min(pt0.x,pt1.x) + Math.abs(pt0.x-pt1.x)/2;
            const y= Math.min(pt0.y,pt1.y) + Math.abs(pt0.y-pt1.y)/2;
            centerPt= makeViewPortPt(x,y);

            DrawUtil.drawCircle(ctx,x,y,color,lineWidth,screenRadius,renderOptions,false );
        }
    }

    if (text && inView && centerPt) {
        var textPt= makeTextLocationCircle(plot,textLoc,centerPt,screenRadius);
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
function drawText(drawObj, drawTextAry, plot, inPt, drawParams) {
    var {text, textOffset, renderOptions}= drawObj;
    var {fontName, fontSize, fontWeight, fontStyle}= drawParams;

    if (!inPt) return;
    var pt= plot.getViewPortCoords(inPt);
    if (plot.pointInViewPort(pt)) {
        var x= pt.x<2 ? 2 : pt.x;
        var y= pt.y<2 ? 2 : pt.y;

        var height= 12;
        if (validator.isFloat(fontSize)) {
            height = parseFloat(fontSize.substring(0, fontSize.length - 2)) * 14 / 10;
        }

        var width = height*text.length*8/20;
        if (textOffset) {
            x+=textOffset.x;
            y+=textOffset.y;
        }
        if (x<2) x = 2;
        if (y<2) y = 2;
        var dim= plot.viewPort.dim;
        var south = dim.height - height - 2;
        var east = dim.width - width - 2;

        if (x > east) x = east;
        if (y > south)y = south;
        else if (y<height) y= height;

        //FIXME:color text black on white background - yellow on white background is not readable
        //TODO: better solution would be to adapt text color with background

        // in case shape 'text' defines the color of its own
        var color = drawObj.sType === ShapeType.Text&&has(drawObj, 'color') ? drawObj.color : 'black';
        DrawUtil.drawText(drawTextAry, text, x, y, color, renderOptions,
                fontName+FONT_FALLBACK, fontSize, fontWeight, fontStyle);
    }
}

/**
 *
 * @param drawObj
 * @param ctx
 * @param plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawRectangle(drawObj, ctx, drawTextAry,  plot, drawParams, onlyAddToPath) {
    var {pts, text, width, height, renderOptions, angle, angleUnit}= drawObj;
    var {color, lineWidth, style, textLoc, unitType}= drawParams;
    var inView = false;
    var centerPt;
    var pt0, pt1;
    var x, y, w, h;


    w = 0; h = 0; x = 0; y = 0;
    centerPt = null;

    if (pts.length===1 && !isNil(width) && !isNil(height)) {
        var isCenter = !isNil(angle);

        pt0 = plot ? plot.getViewPortCoords(pts[0]) : pts[0];
        if (!plot || plot.pointInViewPort(pt0)) {
            inView = true;

            switch (unitType) {
                case UnitType.PIXEL:
                    w = width;
                    h = height;
                    break;
                case UnitType.ARCSEC:
                    w = getValueInScreenPixel(plot, width);
                    h = getValueInScreenPixel(plot, height);
                    break;
                case UnitType.IMAGE_PIXEL:
                    w = plot.zoomFactor * width;
                    h = plot.zoomFactor * height;
                    break;
                default:
                    w = width;
                    h = height;
                    break;
            }

            x = pt0.x;
            y = pt0.y;
            if (h < 0 && !isCenter) {
                h *= -1;
                y -= h;
            }
            if (w < 0 && !isCenter) {
                w *= -1;
                x -= w;
            }

            // make adjustment for rectangle with center and rotate angle

            if (isCenter) {
                if (angleUnit === UnitType.ARCSEC) {
                    angle = convertAngle('arcsec', 'radian', angle);
                } else if (angleUnit === UnitType.IMAGE_PIXEL) {
                    angle =  plot.zoomFactor * angle;
                }

                // move the rotation point to the center, drawing position[0, 0] is visually at {x, y}
                renderOptions = Object.assign({}, renderOptions,
                        {
                            rotAngle: angle,
                            translation: {x, y}
                        });

                centerPt = makeScreenPt(x, y);

                // draw the rect from {-w/2, -h/2} relative to the new origin
                x = -w/2;
                y = -h/2;
            } else {
                centerPt = makeScreenPt(x+w/2, y+h/2);
            }

            if (!onlyAddToPath || style === Style.HANDLED) {
                DrawUtil.beginPath(ctx, color, lineWidth, renderOptions);
            }
            ctx.rect(x, y, w, h);
            if (!onlyAddToPath || style === Style.HANDLED) {
                DrawUtil.stroke(ctx);
            }
        }

    }
    else {
        pt0 = plot ? plot.getViewPortCoords(pts[0]) : pts[0];
        pt1 = plot ? plot.getViewPortCoords(pts[1]) : pts[1];
        if (!pt0 || !pt1) return;

        if (!plot || plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1)) {
            inView = true;

            x = pt0.x;
            y = pt0.y;
            w = pt1.x - x;
            h = pt1.y - y;
            if (!onlyAddToPath || style === Style.HANDLED) {
                DrawUtil.beginPath(ctx, color, lineWidth, renderOptions);
            }
            ctx.rect(x, y, w, h);
            if (!onlyAddToPath || style === Style.HANDLED) {
                DrawUtil.stroke(ctx);
            }
            centerPt = makeScreenPt(x+w/2, y+h/2);
        }
    }

    if (text && inView) {
        var textPt= makeTextLocationRectangle(plot, textLoc, centerPt, w, h, angle);
        drawText(drawObj, drawTextAry, plot, textPt, drawParams);
    }
    if (style == Style.HANDLED && inView) {
        // todo
    }
}


/**
 * draw ellipse
 * @param drawObj
 * @param ctx
 * @param plot
 * @param drawParams
 * @param onlyAddToPath
 */
function drawEllipse(drawObj, ctx, drawTextAry,  plot, drawParams, onlyAddToPath) {
    var {pts, text, radius1, radius2, renderOptions, angle, angleUnit}= drawObj;
    var {color, lineWidth, style, textLoc, unitType}= drawParams;
    var inView = false;
    var centerPt;
    var pt0;
    var x, y, w, h;

    centerPt = null;
    x = 0; y=0; w=0; h=0;
    if ( pts.length ===1 && !isNil(radius1) && !isNil(radius2) && !isNil(angle) && angleUnit) {
        pt0 = plot ? plot.getViewPortCoords(pts[0]) : pts[0];
        centerPt = pt0;
        if (!plot || plot.pointInViewPort(pt0)) {
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

            x = pt0.x;
            y = pt0.y;
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

            if (!onlyAddToPath || style === Style.HANDLED) {
                DrawUtil.beginPath(ctx, color, lineWidth, renderOptions);
            }
            ctx.ellipse(x, y, w, h, angle, 0, 2*Math.PI);
            if (!onlyAddToPath || style === Style.HANDLED) {
                DrawUtil.stroke(ctx);
            }
        }

    }

    if (text && inView) {
        var textPt= makeTextLocationEllipse(plot, textLoc, centerPt, w, h, angle);
        drawText(drawObj, drawTextAry, plot, textPt, drawParams);
    }
    if (style == Style.HANDLED && inView) {
        // todo
    }
}

/**
 *
 * @param plot
 * @param pts
 * @return {number}
 */
function findRadius(plot,pts) {
    var retval= -1;
    var pt0= plot.getScreenCoords(pts[0]);
    var pt1= plot.getScreenCoords(pts[1]);
    if (pt0 && pt1) {
        var xDist= Math.abs(pt0.x-pt1.x)/2;
        var yDist= Math.abs(pt0.y-pt1.y)/2;
        retval= Math.min(xDist,yDist);
    }
    return retval;
}


/**
 *
 * @param plot
 * @param textLoc
 * @param centerPt
 * @param screenRadius
 * @return {null}
 */
function makeTextLocationCircle(plot, textLoc, centerPt, screenRadius) {
    var scrCenPt= plot.getScreenCoords(centerPt);
    if (!scrCenPt || screenRadius<1) return null;
    var opt;
    switch (textLoc) {
        case TextLocation.CIRCLE_NE:
            opt= makeOffsetPt(-1*screenRadius, -1*(screenRadius+10));
            break;
        case TextLocation.CIRCLE_NW:
            opt= makeOffsetPt(screenRadius, -1*(screenRadius));
            break;
        case TextLocation.CIRCLE_SE:
            opt= makeOffsetPt(-1*screenRadius, screenRadius+5);
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
 *
 * @param plot
 * @param textLoc
 * @param fontSize
 * @param inPt0
 * @param inPt1
 * @return {null}
 */
function makeTextLocationLine(plot, textLoc, fontSize, inPt0, inPt1) {
    if (!inPt0 || !inPt1) return null;
    const pt0= plot.getScreenCoords(inPt0);
    const pt1= plot.getScreenCoords(inPt1);

    if (!pt0 || !pt1) return null;
    var height= 12;
    if (validator.isFloat(fontSize)) {
        height = parseFloat(fontSize.substring(0, fontSize.length - 2)) * 14 / 10;
    }
    var x = pt1.x+5;
    var y = pt1.y+5;

    if (textLoc===TextLocation.LINE_MID_POINT || textLoc===TextLocation.LINE_MID_POINT_OR_BOTTOM ||
            textLoc===TextLocation.LINE_MID_POINT_OR_TOP) {
        var dist= VisUtil.computeSimpleDistance(pt1,pt0);
        if (textLoc===TextLocation.LINE_MID_POINT_OR_BOTTOM && dist<100) {
            textLoc= TextLocation.LINE_BOTTOM;
        }
        if (textLoc===TextLocation.LINE_MID_POINT_OR_TOP && dist<80) {
            textLoc= TextLocation.LINE_TOP;
        }
    }

    switch (textLoc) {
        case TextLocation.LINE_TOP:
            y= pt1.y- (height+5);
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

function widthAfterRotation(width, height, angle) {
    const wcos = width * Math.cos(angle);
    const hsin = height * Math.sin(angle);

    return Math.max(Math.abs(wcos-hsin), Math.abs(wcos+hsin));
}

function heightAfterRotation(width, height, angle) {
    const wsin = width * Math.sin(angle);
    const hcos = height * Math.cos(angle);

    return Math.max(Math.abs(wsin-hcos), Math.abs(wsin+hcos));
}


/**
 * compute text location for rectangle
 * @param plot
 * @param textLoc
 * @param centerPt
 * @param width
 * @param height
 * @param angle
 * @returns {object} screen point
 */
function makeTextLocationRectangle(plot, textLoc, centerPt, width, height, angle) {
    var scrCenPt= plot.getScreenCoords(centerPt);
    if (!scrCenPt || width <1 || height < 1) return null;

    var w = widthAfterRotation(width, height, angle)/2;
    var h = heightAfterRotation(width, height, angle)/2;

    var opt;
    switch (textLoc) {
        case TextLocation.RECT_NE:
            opt= makeOffsetPt(-1*w, -1*h);
            break;
        case TextLocation.RECT_NW:
            opt= makeOffsetPt(w-10, -1*(h));
            break;
        case TextLocation.RECT_SE:
            opt= makeOffsetPt(-1*w, h);
            break;
        case TextLocation.RECT_SW:
            opt= makeOffsetPt(w-10, h);
            break;
        default:
            opt= makeOffsetPt(0,0);
            break;
    }
    return makeScreenPt(scrCenPt.x+opt.x, scrCenPt.y+opt.y);
}


/**
 * compute text location for ellipse
 * @param plot
 * @param textLoc
 * @param centerPt
 * @param radius1  radius on horizotal axis
 * @param radius2  radius on vertical axis
 * @returns {object} screen location
 */
function makeTextLocationEllipse(plot, textLoc, centerPt, radius1, radius2, angle) {
    var scrCenPt= plot.getScreenCoords(centerPt);
    if (!scrCenPt || radius1 < 1 || radius2 < 1) return null;

    var w = widthAfterRotation(radius1, radius2, angle);  // half of horizontal coverage
    var h = heightAfterRotation(radius1, radius2, angle); // half of vertical coverage

    var opt;
    switch (textLoc) {
        case TextLocation.ELLIPSE_NE:
            opt= makeOffsetPt(-1*w, -1*(h));
            break;
        case TextLocation.ELLIPSE_NW:
            opt= makeOffsetPt(w-10, -1*(h));
            break;
        case TextLocation.ELLIPSE_SE:
            opt= makeOffsetPt(-1*w, h);
            break;
        case TextLocation.ELLIPSE_SW:
            opt= makeOffsetPt(w-10, h);
            break;
        default:
            opt= makeOffsetPt(0,0);
            break;
    }
    return makeScreenPt(scrCenPt.x+opt.x, scrCenPt.y+opt.y);

}


/**
 *
 * @param drawObj
 * @param plot
 * @param drawParams
 * @return {Array}
 */
function toRegion(drawObj, plot, drawParams) {
    var {color,unitType,textLoc,fontSize}= drawParams;
    var {sType,pts,text,textOffset,radius,width,height}= drawObj;
    var retList= [];
    switch (sType) {
        case ShapeType.Text:
            retList= makeTextRegion(text, pts[0], textOffset, plot, color);
            break;
        case ShapeType.Line:
            retList= makeLineRegion(plot,color,pts,text,textLoc,fontSize);
            break;
        case ShapeType.Circle:
            retList= makeCircleRegion(plot,pts,radius,unitType,textLoc,color);
            break;
        case ShapeType.Rectangle:
            retList= makeRectangleRegion(pts,plot,text,width,height,color);
            break;
    }
    return retList;
}


/**
 *
 * @param text
 * @param textOffset
 * @param inPt
 * @param plot
 * @param color
 * @return {*}
 */
function makeTextRegion( text, inPt, textOffset, plot, color) {
    if (!text || !inPt) return [];
    var wp= plot.getWorldCoords(inPt);
    if (!wp) return [];


    var rt= window.ffgwt.util.dd.RegionText.makeRegionText( wp.toString());
    var op= rt.getOptions();
    op.setColor(color);
    op.setText(makeNonHtml(text));

    if (textOffset) {
        op.setOffsetX(textOffset.x);
        op.setOffsetY(textOffset.y);
    }
    return [rt];
}


function makeNonHtml(s) {
    var retval= s;
    if (s.endsWith(HTML_DEG)) {
        retval= s.substring(0,s.indexOf(HTML_DEG)) + ' deg';
    }
    return retval;
}


/**
 *
 * @param plot
 * @param color
 * @param pts
 * @param text
 * @param textLoc
 * #param fontSize
 * @return {Array}
 */
function makeLineRegion(plot, color, pts, text, textLoc, fontSize) {
    var wp0= plot.getWorldCoords(pts[0]);
    var wp1= plot.getWorldCoords(pts[1]);

    var retList= [];
    if (wp0 && wp1) {

        var rl= window.ffgwt.util.dd.RegionLines.makeRegionLines(
                [wp0.toString(), wp1.toString()] );
        rl.getOptions().setColor(color);
        retList= [rl];

        if (text) {
            var textPt= makeTextLocationLine(plot, textLoc, pts[0], pts[1],fontSize);
            var textReg= makeTextRegion(text, plot.getWorldCoords(textPt), null, plot, color);
            retList= [...retList, ...textReg];
        }
    }
    return retList;
}

/**
 * ONLY VALID FOR CIRCLE!
 * @param plot
 * @param pts
 * @return
 */
function getCircleCenter(plot,pts) {
    const pt0= plot.getScreenCoords(pts[0]);
    const pt1= plot.getScreenCoords(pts[1]);
    if (!pt0 || !pt1) return null;
    const x= Math.min(pt0.x,pt1.x) + Math.abs(pt0.x-pt1.x)/2;
    const y= Math.min(pt0.y,pt1.y) + Math.abs(pt0.y-pt1.y)/2;
    return plot.getWorldCoords(makeScreenPt(x,y));
}


/**
 *
 * @param plot
 * @param pts
 * @param radius
 * @param unitType
 * @param text
 * @param textLoc
 * @param color
 * @return {Array}
 */
function makeCircleRegion(plot, pts, radius, unitType, text, textLoc, color) {
    var wp;
    var textPtScreen;
    var retList= [];
    var st= UnitType.PIXEL;
    if (pts.length===1 && radius) {
        st= unitType;
        wp= plot.getWorldCoords(pts[0]);
        textPtScreen= makeTextLocationCircle(plot,textLoc,wp, radius);
    }
    else {
        wp= getCircleCenter(plot, pts);
        radius= findRadius(plot,pts);
        textPtScreen= makeTextLocationCircle(plot,textLoc,wp,radius);
    }

    if (wp) {
        var unit= (st===UnitType.PIXEL) ? 'SCREEN_PIXEL' :'ARCSEC';
        var rV= window.ffgwt.util.dd.RegionValue.makeRegionValue(radius, unit);
        var ra= window.ffgwt.util.dd.RegionText.RegionAnnulus( wp.toString(),rV );
        ra.getOptions().setColor(color);
        retList.add(ra);
        retList= [ra];
        if (text) {
            var textPt= plot.getWorldCoords(textPtScreen);
            var textReg= makeTextRegion(text, textPt, null, plot, color);
            retList= [...retList,...textReg];
        }
    }
    return retList;
}


/**
 *
 * @param pts
 * @param plot
 * @param text
 * @param width
 * @param height
 * @param color
 * @return {Array}
 */
function makeRectangleRegion(pts, plot, text, width, height, color) {
    if (!pts.length) return [];
    var textPt;
    var pt0= plot.getScreenCoords(pts[0]);
    var pt1;
    var x= pt0.x;
    var y= pt0.y;
    var w, h;
    var wp;
    var retList= [];
    if (pts.length===1 && width  && height) {
        w= width;
        h= height;
        if (h<0) {
            h*=-1;
            y-=h;
        }
        if (w<0) {
            w*=-1;
            x-=w;
        }
        wp= plot.getWorldCoords(makeScreenPt(x,y));
        textPt= wp;
    }
    else {
        pt1= plot.getScreenCoords(pts[1]);
        if (!pt1) return [];
        w=  pt1.x-pt0.x;
        h=  pt1.x-pt0.y;
        wp= plot.getWorldCoords(makeScreenPt(x,y));
        textPt= plot.getWorldCoords(pt1);
    }

    if (wp) {
        var wRv= window.ffgwt.util.dd.RegionValue.makeRegionValue(w, 'SCREEN_PIXEL');
        var hRv= window.ffgwt.util.dd.RegionValue.makeRegionValue(h, 'SCREEN_PIXEL');
        var dim= window.ffgwt.util.dd.RegionDimension.makeRegionDimension(wRv,hRv);
        var zero= window.ffgwt.util.dd.RegionValue.makeRegionValue(0, 'SCREEN_PIXEL');
        var rb= window.ffgwt.util.dd.makeRegionBox(wp.toString(),dim,zero);
        rb.getOptions().setColor(color);
        retList= [rb];
        if (text) {
            makeTextRegion(retList, textPt, plot, color);
        }
        if (text) {
            var textReg= makeTextRegion(text, textPt, null, plot, color);
            retList= [...retList,...textReg];
        }
    }
    return retList;
}




function translateTo(plot, pts, apt) {
    var pt= plot.getScreenCoords(apt);

    return pts.map( (inPt) => {
        var pti= plot.getScreenCoords(inPt);
        return plot.getWorldCoords(makeScreenPt(pt.x+pti.x,pt.y+pti.y));
    });
}


function rotateAround(plot, pts, angle, wc) {
    return pts.map( (p1) => {
        var pti= plot.getScreenCoords(p1);
        var center= plot.getScreenCoords(wc);
        var xc = center.x;
        var x1 = pti.x - xc;
        var yc = center.y;
        var y1 = pti.y - yc;

        // APPLY ROTATION
        var temp_x1 = x1 * Math.cos(angle) - y1 * Math.sin(angle);
        var temp_y1 = x1 * Math.sin(angle) + y1 * Math.cos(angle);

        // TRANSLATE BACK
        return plot.getWorldCoords(makeScreenPt(temp_x1 + xc, temp_y1 + yc));
    });
}

