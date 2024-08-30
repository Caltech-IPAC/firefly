
import {isNil, set} from 'lodash';
import {makeScreenPt,  makeDevicePt} from '../Point.js';
import {DrawSymbol} from './DrawSymbol.js';
import {toRadians} from '../VisUtil.js';


const FALLBACK_COLOR = 'red';

export default {getColor, beginPath, stroke, fill, strokeRec, drawLine, drawText, drawTextCanvas, drawPath, makeShadow,
                drawHandledLine, drawInnerRecWithHandles, drawCircleWithHandles, drawEclipseWithHandles, rotateAroundScreenPt,
                drawX, drawSquareX, drawSquare, drawEmpSquareX, drawCross, drawSymbol, drawPointMarker,
                drawEmpCross, drawDiamond, drawDot, drawCircle, drawEllipse, drawBoxcircle,
                drawArrow, drawRotate, clear,clearCanvas, fillRec, getDrawingSize, polygonPath,
                getSymbolSize, getSymbolSizeBasedOn, beginFillPath, endFillPath, fillPath,
                drawArrowOnLine
                };

function drawHandledLine(ctx, color, sx, sy, ex, ey, onlyAddToPath= false) {
    let slope= NaN;

    if (ex-sx!==0) slope= (ey-sy) / (ex-sx);
    let x, y;
    if (!onlyAddToPath) beginPath(ctx,color,3);

    if (isNaN(slope)) {
        y= (sy < ey) ? sy+5 : sy-5;
        ctx.moveTo(sx,sy);
        ctx.lineTo(sx,y);

        y= (sy < ey) ? ey-5 : ey+5;
        ctx.moveTo(ex,ey);
        ctx.lineTo(ex,y);
    }
    else if (Math.abs(sx-ex) > Math.abs(sy-ey)) {  // horizontal
        x= (sx < ex) ? sx+5 : sx-5;
        y= slope * (x - sx) + sy;

        ctx.moveTo(sx,sy);
        ctx.lineTo(x,y);

        x= (sx < ex) ? ex-5 : ex+5;
        y= slope * (x - ex) + ey;

        ctx.moveTo(ex,ey);
        ctx.lineTo(x,y);
    }
    else {

        y= (sy < ey) ? sy+5 : sy-5;
        x= (y-sy)/slope + sx;
        ctx.moveTo(sx,sy);
        ctx.lineTo(x,y);


        y= (sy < ey) ? ey-5 : ey+5;
        x= (y-ey)/slope + ex;
        ctx.moveTo(ex,ey);
        ctx.lineTo(x,y);

    }
    if (!onlyAddToPath) stroke(ctx);
}

function drawInnerRecWithHandles(ctx, color, lineWidth, inX1, inY1, inX2, inY2) {

    const x0= Math.min(inX1,inX2)+lineWidth;
    const y0= Math.min(inY1,inY2)+lineWidth;
    const width= Math.abs(inX1-inX2)-(2*lineWidth);
    const height= Math.abs(inY1-inY2)-(2*lineWidth);
    strokeRec(ctx,color, lineWidth, x0,y0,width,height);
    const x2= x0+width;
    const y2= y0+height;

    const x1= x0+width;
    const y1= y0;

    const x3= x0;
    const y3= y0+height;

    beginPath(ctx,color,3);

    drawHandledLine(ctx, color, x0,y0,x1,y1,true);
    drawHandledLine(ctx, color, x1,y1,x2,y2, true);
    drawHandledLine(ctx, color, x2,y2,x3,y3,true);
    drawHandledLine(ctx, color, x3,y3,x0,y0,true);

    stroke(ctx);
}

function drawCircleWithHandles(ctx, color, lineWidth, inX1, inY1, inX2, inY2, outerThickness = 0) {

    const x0 = (inX1+inX2)/2;
    const y0 = (inY1+inY2)/2;
    const rx = (inX2 - inX1)/2;
    const ry = (inY2 - inY1)/2;

    let r1 = Math.abs(rx);
    let r2 = Math.abs(ry);
    r1 = Math.max(r1 - outerThickness, 1);
    r2 = Math.max(r2 - outerThickness, 1);
    const radius= Math.min(r1,r2);

    drawCircle(ctx, x0, y0, color,   radius,lineWidth);
    // drawEllipse(ctx, x0, y0, color, lineWidth, r1, r2, 0);
}

function drawEclipseWithHandles(ctx, color, lineWidth, inX1, inY1, inX2, inY2, outerThickness = 0) {

    const x0 = (inX1+inX2)/2;
    const y0 = (inY1+inY2)/2;
    const rx = (inX2 - inX1)/2;
    const ry = (inY2 - inY1)/2;

    let r1 = Math.abs(rx);
    let r2 = Math.abs(ry);
    r1 = Math.max(r1 - outerThickness, 1);
    r2 = Math.max(r2 - outerThickness, 1);

    drawEllipse(ctx, x0, y0, color, lineWidth, r1, r2, 0);
}


/**
 *
 * @param drawTextAry
 * @param text
 * @param x
 * @param y
 * @param color
 * @param renderOptions
 * @param fontFamily
 * @param size
 * @param fontWeight
 * @param fontStyle
 * @param backGroundColor
 * @param padDing
 * @param rotationAngle
 */
function drawText(drawTextAry,text, x,y,color,
                  renderOptions,
                  fontFamily='helvetica', size='9px',
                  fontWeight='normal', fontStyle='normal',
                  backGroundColor, padDing,rotationAngle=undefined ) {


    //todo
    // it I don't use canvas I need to set css and shadow
    if (renderOptions && renderOptions.translation) {
        const {translation}= renderOptions;
        x+= translation.x;
        y+= translation.y;
    }

    const style= {
        position:'absolute',
        color,
        left:x,
        top:y,
        fontFamily,
        'fontSize': size,
        fontWeight,
        fontStyle,
        'MozBorderRadius': '1px',
        'borderRadius': '1px',
        'WebkitBorderRadius': '1px',
    };

    if (backGroundColor) style.backgroundColor = backGroundColor;
    if (rotationAngle) style.transform = 'rotate('+rotationAngle+')';
    if (padDing) style.padding = padDing;

    drawTextAry.push({text,style});
}

/**
 *
 * @param ctx
 * @param {String} text
 * @param {number} x
 * @param {number} y
 * @param {String} color
 * @param {Object} renderOptions
 * @param {Object} locationOptions
 * @param {String} locationOptions.textBaseline
 * @param {String} locationOptions.textAlign
 * @param {number} locationOptions.rotationAngle angle in degrees
 * @param {Object} fontOptions
 * @param {String} [fontOptions.fontFamily]
 * @param {String} [fontOptions.size]
 * @param {String} [fontOptions.fontWeight]
 * @param {String} [fontOptions.fontStyle]
 */
function drawTextCanvas(ctx, text, x,y,color= 'red', renderOptions= {}, locationOptions= {}, fontOptions= {} ) {

    ctx.save();


    const {textBaseline= 'top', textAlign= 'start', rotationAngle=0} = locationOptions;
    const {fontName='helvetica', fontSize='9px', fontWeight='normal', fontStyle='normal'} = fontOptions;

    ctx.font= `${fontStyle} ${fontWeight} ${fontSize} ${fontName}`;
    // offscreenCtx.fillStyle= 'rgba(0,0,0,.4)';
    // offscreenCtx.strokeStyle='rgba(0,0,0,.2)';
    // ctx.textAlign= 'center';
    // ctx.translate(x+w/2,y+h/2);
    ctx.fillStyle = color;
    ctx.textBaseline= textBaseline;
    ctx.textAlign= textAlign;
    addStyle(ctx,renderOptions);
    if (rotationAngle) {
        ctx.translate(x,y);
        const rotate= Number(rotationAngle);
        if (rotate) ctx.rotate(toRadians(rotate));
        ctx.fillText(text,0,0);
    }
    else {
        ctx.fillText(text,x,y);
    }
    ctx.restore();
}

/**
 * create a shadow object to use with DrawObj
 * @param blur
 * @param offX
 * @param offY
 * @param color
 * @return {{blur: *, offX: *, offY: *, color: *}}
 */
function makeShadow(blur, offX, offY, color) {
    return {blur, offX, offY, color};
}


/**
 *
 * @param {string} objColor
 * @param {string} defColor
 * @return {string}
 */
function getColor(objColor,defColor) {
    return objColor || defColor || FALLBACK_COLOR;
}


/**
 *
 * @param {object} ctx
 * @param {string} color
 * @param {number} lineWidth
 * @param {object} [renderOptions]
 */
function beginPath(ctx,color,lineWidth,renderOptions) {
    ctx.save();
    ctx.lineWidth=lineWidth;
    ctx.strokeStyle=color;
    if (renderOptions) addStyle(ctx,renderOptions);
    ctx.beginPath();
}


function stroke(ctx) {
    ctx.stroke();
    ctx.restore();
}

function fill(ctx,fillPath) {
    ctx.fill(fillPath);
    ctx.restore();
}

/**
 * start fill path
 * @param {object} ctx
 * @param {object} [renderOptions]
 * @param {string} color
 * @param {string} strokeColor
 */
function beginFillPath(ctx, renderOptions, color ='', strokeColor='') {
    ctx.save();
    if (color) ctx.fillStyle=color;
    if (strokeColor) ctx.strokeStyle = strokeColor;
    if (renderOptions) addStyle(ctx,renderOptions);
    ctx.beginPath();
}

/**
 * end fill path
 * @param ctx
 * @param close
 * @param bStroke
 */
function endFillPath(ctx, close = true, bStroke = true) {
    if (close) ctx.closePath();
    ctx.fill();
    if (bStroke) ctx.stroke();
    ctx.restore();
}

function addStyle(ctx,renderOptions) {
    if (!ctx || !renderOptions) return;
    const {shadow,rotAngle,translation, lineDash, lineJoin}= renderOptions;

    if (lineJoin) {
       ctx.lineJoin= lineJoin;
    }

    if (lineDash) {
        ctx.setLineDash(lineDash);
    }
    if (shadow) {
        const {blur,color,offX,offY} = shadow;
        if (blur) ctx.shadowBlur= blur;
        if (color) ctx.shadowColor= color;
        if (offX) ctx.shadowOffsetX= offX;
        if (offY) ctx.shadowOffsetY= offY;
    }

    if (translation) {
        ctx.translate(translation.x,translation.y);
    }

    if (!isNil(rotAngle)) {
        ctx.rotate(rotAngle);
    }
}

/**
 *
 * @param {context} ctx
 * @param {string} color
 * @param {number} lineWidth
 * @param {number} x
 * @param {number} y
 * @param {number} width
 * @param {number} height
 * @param {object} [renderOptions]
 */
function strokeRec(ctx, color, lineWidth, x, y, width, height, renderOptions) {
    ctx.save();
    if (renderOptions) addStyle(ctx,renderOptions);
    ctx.lineWidth=lineWidth;
    ctx.strokeStyle=color;
    ctx.strokeRect(x,y,width,height);
    ctx.restore();
}

/**
 *
 * @param ctx
 * @param color
 * @param lineWidth
 * @param sx
 * @param sy
 * @param ex
 * @param ey
 * @param renderOptions
 */
function drawLine(ctx,color, lineWidth, sx, sy, ex, ey,renderOptions) {
    ctx.save();
    if (renderOptions) addStyle(ctx,renderOptions);
    ctx.lineWidth=lineWidth;
    ctx.strokeStyle=color;
    ctx.beginPath();
    ctx.moveTo(sx, sy);
    ctx.lineTo(ex, ey);
    ctx.stroke();
    ctx.restore();
}

function drawPath(ctx, color, lineWidth, pts, close, renderOptions) {
    ctx.save();
    if (renderOptions) addStyle(ctx,renderOptions);
    ctx.lineWidth=lineWidth;
    ctx.strokeStyle=color;
    ctx.beginPath();

    pts.forEach( (pt,idx) => {
        (idx===0) ? ctx.moveTo(pt.x,pt.y) : ctx.lineTo(pt.x,pt.y);
    });
    if (close) ctx.closePath();
    ctx.stroke();
    ctx.restore();
}

function polygonPath(ctx, pts, close, fillColor, strokeColor) {
    if (pts.length < 3) return;
    const allPts = close ? [...pts, pts[0]] : pts;

    allPts.forEach( (pt,idx) => {
        (idx===0) ? ctx.moveTo(pt.x,pt.y) : ctx.lineTo(pt.x,pt.y);
    });
    if (fillColor) ctx.fillStyle = fillColor;
    if (strokeColor) ctx.strokeStyle = strokeColor;
}

function fillPath(ctx, color, pts, close, renderOptions, strokeColor='') {
    ctx.save();
    if (renderOptions) addStyle(ctx,renderOptions);

    if (strokeColor) ctx.strokeStyle = strokeColor;
    ctx.fillStyle = color;

    ctx.beginPath();
    pts.forEach( (pt,idx) => {
        (idx===0) ? ctx.moveTo(pt.x,pt.y) : ctx.lineTo(pt.x,pt.y);
    });
    if (close) ctx.closePath();

    ctx.fill();
    if (strokeColor) ctx.stroke();

    ctx.restore();
}

function rotateAroundScreenPt(worldPt, plot, angle, centerScreenPt) {
    const pti = plot.getScreenCoords(worldPt);
    const xc = centerScreenPt.x;
    const x1 = pti.x - xc;
    const yc = centerScreenPt.y;
    const y1 = pti.y - yc;

    // APPLY ROTATION
    const temp_x1 = x1 * Math.cos(angle) - y1 * Math.sin(angle);
    const temp_y1 = x1 * Math.sin(angle) + y1 * Math.cos(angle);

    // TRANSLATE BACK
    return plot.getWorldCoords(makeScreenPt(temp_x1 + xc, temp_y1 + yc));
}




function drawX(ctx, x, y, color, size,lineWidth, renderOptions, onlyAddToPath) {
    if (!onlyAddToPath) beginPath(ctx,color,lineWidth, renderOptions);
    ctx.moveTo(x - size, y - size);
    ctx.lineTo(x + size, y + size);
    ctx.moveTo(x-size,y+size);
    ctx.lineTo(x+size,y-size);
    if (!onlyAddToPath) stroke(ctx);
}

function drawSquareX(ctx, x, y, color, size,lineWidth, renderOptions, onlyAddToPath) {
    if (!onlyAddToPath) beginPath(ctx,color,lineWidth, renderOptions);
    drawX(ctx,x,y,color,size,lineWidth,renderOptions, onlyAddToPath);
    drawSquare(ctx,x,y,color,size,lineWidth,renderOptions, true);
    if (!onlyAddToPath) stroke(ctx);
}

function drawSquare(ctx, x, y, color, size,lineWidth, renderOptions, onlyAddToPath) {
    if (onlyAddToPath) {
        ctx.rect(x - size, y - size, 2 * size, 2 * size);
    }
    else {
        strokeRec(ctx,color,lineWidth,x-size,y-size, 2*size, 2*size,renderOptions);
    }
}


function drawEmpSquareX(ctx, x, y, color, size,lineWidth, renderOptions, c1, c2) {
    drawX(ctx,x,y,color, size,lineWidth, renderOptions, false);
    drawSquare(ctx,x,y,c1,size, renderOptions,  false);
    drawSquare(ctx,x,y,c2, size+2,renderOptions, false);
}



function drawCross(ctx, x, y, color, size,lineWidth,renderOptions, onlyAddToPath) {
    if (!onlyAddToPath) beginPath(ctx,color,lineWidth, renderOptions);
    ctx.moveTo(x-size,y);
    ctx.lineTo(x+size,y);
    ctx.moveTo(x,y-size);
    ctx.lineTo(x,y+size);
    if (!onlyAddToPath) stroke(ctx);
}



function drawPointMarker(ctx, x, y, color, size,lineWidth,renderOptions, onlyAddToPath) {
    const gap= size<5 ? 1 : Math.trunc(size*.3);
    if (!onlyAddToPath) beginPath(ctx,color,lineWidth, renderOptions);
    ctx.moveTo(x-size,y);
    ctx.lineTo(x-gap,y);

    ctx.moveTo(x+size,y);
    ctx.lineTo(x+gap,y);


    ctx.moveTo(x,y-size);
    ctx.lineTo(x,y-gap);
    ctx.moveTo(x,y+size);
    ctx.lineTo(x,y+gap);

    if (!onlyAddToPath) stroke(ctx);
    drawCircle(ctx, x, y, color, Math.trunc(size*.8), lineWidth, renderOptions, onlyAddToPath);
}


function drawEmpCross(ctx, x, y, size,lineWidth,renderOptions, color1, color2) {

    drawLine(ctx, color1, lineWidth, x-size,y, x+size, y, renderOptions);
    drawLine(ctx, color1, lineWidth, x,y-size, x, y+size, renderOptions);

    drawLine(ctx, color2, lineWidth, x-(size+1),y, x-(size+2), y, renderOptions);
    drawLine(ctx, color2, lineWidth, x+(size+1),y, x+(size+2), y, renderOptions);

    drawLine(ctx, color2, lineWidth, x,y-(size+1), x, y-(size+2), renderOptions);
    drawLine(ctx, color2, lineWidth, x,y+(size+1), x, y+(size+2), renderOptions);
}


function drawDiamond(ctx, x, y, color, size,lineWidth,renderOptions, onlyAddToPath) {

    if (!onlyAddToPath) beginPath(ctx,color,lineWidth, renderOptions);
    ctx.moveTo(x,y-size);
    ctx.lineTo(x+size, y);
    ctx.moveTo(x+size, y);
    ctx.lineTo(x, y+size);
    ctx.moveTo(x, y+size);
    ctx.lineTo(x-size,y);
    ctx.moveTo(x-size,y);
    ctx.lineTo(x,y-size);
    if (!onlyAddToPath) stroke(ctx);
}



function drawDot(ctx, x, y, color, size, lineWidth,renderOptions, onlyAddToPath) {
    const begin= size>1 ? y- (size/2) : y;
    const end= size>1 ? y+ (size/2) : y;

    if (!onlyAddToPath) beginPath(ctx,color,lineWidth, renderOptions);
    for(let i=begin; (i<=end); i++) {
        ctx.moveTo(x-size/2,i);
        ctx.lineTo(x+size/2,i);
    }

    if (!onlyAddToPath) stroke(ctx);
}


function drawBoxcircle(ctx, x, y, color, size,  lineWidth, renderOptions, onlyAddToPath) {
    drawSquare(ctx, x, y, color, size,lineWidth, renderOptions, onlyAddToPath);
    drawCircle(ctx, x, y, color, size, lineWidth, renderOptions, onlyAddToPath);
}

function drawArrow(ctx, x, y, color, size,lineWidth, renderOptions, onlyAddToPath) {
    if (!onlyAddToPath) beginPath(ctx,color, lineWidth, renderOptions);
    ctx.moveTo(x,y);
    ctx.lineTo(x-size, y-size);
    ctx.moveTo(x, y);
    ctx.lineTo(x, y-size*1);
    ctx.moveTo(x, y);
    ctx.lineTo(x-size*1, y);
    if (!onlyAddToPath) stroke(ctx);
}

/**
 * @desc draw rotate symbol in the western location
 * @param ctx
 * @param x (x, y) point to the end of the bar of rotate symbol
 * @param y
 * @param color
 * @param size
 * @param lineWidth
 * @param renderOptions
 * @param onlyAddToPath
 */
function drawRotate(ctx, x, y, color, size, lineWidth,renderOptions, onlyAddToPath) {
    const r = size/4;
    const aoff = (r/2 < 1) ? 1 : r/2;
    const xc = 3*r;
    const yc = 0;

    if (renderOptions) {
        set(renderOptions, 'translation', {x, y});
    }

    if (!onlyAddToPath) beginPath(ctx, color, lineWidth, renderOptions);
    //ctx.translate(x, y);
    ctx.arc(xc, yc, r, 0, 2*Math.PI);
    ctx.moveTo(xc+r, yc);
    ctx.lineTo(xc+r+aoff-1, yc-aoff-1);
    ctx.moveTo(xc+r, yc);
    ctx.lineTo(xc+r-aoff-1, yc-aoff+1);
    ctx.moveTo(xc, yc);
    ctx.lineTo(0, 0);
    ctx.moveTo(0, 0);
    ctx.arc(0, yc, 1, 0, 2*Math.PI);
    //ctx.moveTo(yc-2*r, 0);
    //ctx.rect(0, yc-2*r, size, size);
    if (!onlyAddToPath) stroke(ctx);
}

/**
 *
 * @param ctx
 * @param x
 * @param y
 * @param color
 * @param lineWidth
 * @param r1
 * @param r2
 * @param angle rotation angle in radian
 * @param renderOptions
 * @param onlyAddToPath
 */
function drawEllipse(ctx, x, y, color, lineWidth, r1, r2, angle, renderOptions, onlyAddToPath ) {

    if (!onlyAddToPath) beginPath(ctx, color, lineWidth, renderOptions);
    ctx.ellipse(x, y, r1, r2, angle, 0, 2*Math.PI);
    if (!onlyAddToPath) stroke(ctx);
}

function drawSymbol(ctx, x, y, drawParams, renderOptions, onlyAddToPath) {
    const {color,size, lineWidth}= drawParams;
    switch (drawParams.symbol) {
        case DrawSymbol.X :
            drawX(ctx, x, y, color, size,lineWidth, renderOptions, onlyAddToPath);
            break;
        case DrawSymbol.EMP_CROSS :
            drawEmpCross(ctx, x, y, color,size,lineWidth,renderOptions,   'white');
            break;
        case DrawSymbol.EMP_SQUARE_X:
            drawEmpSquareX(ctx, x, y, color, size,lineWidth,renderOptions,  'black', 'white');
            break;
        case DrawSymbol.CROSS :
            drawCross(ctx, x, y, color, size,lineWidth,renderOptions,  onlyAddToPath);
            break;
        case DrawSymbol.SQUARE :
            drawSquare(ctx, x, y, color, size,lineWidth,renderOptions,  onlyAddToPath);
            break;
        case DrawSymbol.SQUARE_X :
            drawSquareX(ctx, x, y, color, size,lineWidth,renderOptions,  onlyAddToPath);
            break;
        case DrawSymbol.DIAMOND :
            drawDiamond(ctx, x, y, color, size,renderOptions,  onlyAddToPath);
            break;
        case DrawSymbol.DOT :
            drawDot(ctx, x, y, color, size,lineWidth,renderOptions,  onlyAddToPath);
            break;
        case DrawSymbol.CIRCLE :
            drawCircle(ctx, x, y, color,   size,lineWidth, renderOptions, onlyAddToPath);
            break;
        case DrawSymbol.BOXCIRCLE :
            drawBoxcircle(ctx, x, y, color, size,lineWidth, renderOptions, onlyAddToPath);
            break;
        case DrawSymbol.ARROW :
            drawArrow(ctx, x, y, color, size, lineWidth, renderOptions, onlyAddToPath);
            break;
        case DrawSymbol.ROTATE :
            drawRotate(ctx, x, y, color, size, lineWidth, renderOptions, onlyAddToPath);
            break;
        case DrawSymbol.POINT_MARKER :
            drawPointMarker(ctx, x, y, color, size, lineWidth, renderOptions, onlyAddToPath);
            break;

        default :
            break;
    }
}

function getDrawingSize(size, symbol) {
    let width = 10;

    size += 1;
    switch (symbol) {
        case DrawSymbol.X :
        case DrawSymbol.CROSS :
        case DrawSymbol.EMP_SQUARE_X:
        case DrawSymbol.SQUARE_X :
        case DrawSymbol.SQUARE :
        case DrawSymbol.DIAMOND :
        case DrawSymbol.BOXCIRCLE :
        case DrawSymbol.CIRCLE :
        case DrawSymbol.POINT_MARKER :
            width = size * 2;
            break;
        case DrawSymbol.EMP_CROSS :
            width = (size+2) * 2;
            break;
        case DrawSymbol.DOT :
        case DrawSymbol.ARROW :
        case DrawSymbol.ROTATE :
            width = size+1;
            break;
        default :
            break;
    }
    width = Math.ceil(width);
    const height = width;
    return {width, height};
}

/**
 * @summary symbol size based on the given canvas dimension
 * @param width
 * @param height
 * @param symbol
 * @returns {number}
 */
function getSymbolSize(width, height, symbol) {
    let size = 5;

    switch (symbol) {
        case DrawSymbol.X :
        case DrawSymbol.CROSS :
        case DrawSymbol.EMP_SQUARE_X:
        case DrawSymbol.SQUARE_X :
        case DrawSymbol.SQUARE :
        case DrawSymbol.DIAMOND :
        case DrawSymbol.BOXCIRCLE :
        case DrawSymbol.CIRCLE :
        case DrawSymbol.POINT_MARKER :
            size = Math.min(width, height)/2;
            break;
        case DrawSymbol.EMP_CROSS :
            size = Math.min(width, height)/2 - 2;
            break;
        case DrawSymbol.DOT :
        case DrawSymbol.ARROW :
        case DrawSymbol.ROTATE :
            size = Math.min(width, height)-1;
            break;
        default :
            break;
    }

    size -= 1;
    return size;
}

function getSymbolSizeBasedOn(symbol, drawingDef) {
    const {width, height} = getDrawingSize(drawingDef.size, drawingDef.symbol);

    return getSymbolSize(width, height, symbol);
}

/**
 *
 * @param ctx
 * @param x
 * @param y
 * @param color
 * @param size
 * @param lineWidth
 * @param renderOptions
 * @param onlyAddToPath
 */
function drawCircle(ctx, x, y, color,  size,lineWidth, renderOptions= null, onlyAddToPath= false) {
    const radius= size;
    if (onlyAddToPath) {
        ctx.moveTo(x+radius,y);
    }
    else {
        beginPath(ctx, color, lineWidth, renderOptions);
    }
    ctx.arc(x, y, radius, 0, 2 * Math.PI);
    if (!onlyAddToPath) stroke(ctx);
}

function fillRec(ctx, color, x, y, width, height, renderOptions, strokeColor) {
    ctx.save();
    ctx.fillStyle=color;
    if (renderOptions) addStyle(ctx,renderOptions);
    ctx.fillRect(x, y, width, height);

    if (strokeColor) {
        ctx.strokeStyle = strokeColor;
        ctx.strokeRect(x, y, width, height);
    }
    ctx.restore();
}



function clear(ctx,width,height) {
    if (!ctx) return;
    ctx.clearRect(0,0,width,height);
}


function clearCanvas(canvas) {
    if (!canvas) return;
    clear(canvas.getContext('2d'),canvas.width,canvas.height);
}


/**
 * add a solid arrow to the second end of the line
 * @param ctx
 * @param fromPt in device coordinate
 * @param toPt   in device coordinate
 * @param color
 */
function drawArrowOnLine(ctx, fromPt, toPt, color) {
    const ahead = 16;
    const aAngle = Math.PI/6;
    const dx = toPt.x - fromPt.x;
    const dy = toPt.y - fromPt.y;
    const d = Math.sqrt(dx*dx+dy*dy);
    const aD = d < ahead ? d : ahead;
    const angle = Math.atan2(dy, dx);
    const pts = [];

    pts.push(toPt);
    pts.push(makeDevicePt(toPt.x - aD * Math.cos(angle - aAngle), toPt.y - aD * Math.sin(angle - aAngle)));
    pts.push(makeDevicePt(toPt.x - aD * Math.cos(angle + aAngle), toPt.y - aD * Math.sin(angle + aAngle)));

    fillPath(ctx, color, pts, true);
}