
import {makeScreenPt} from '../Point.js';

var FALLBACK_COLOR = 'red';

export default {getColor, beginPath, stroke, strokeRec, drawLine, drawText, drawPath, makeShadow,
                drawHandledLine, drawInnerRecWithHandles, rotateAroundScreenPt,
                drawX, drawSquareX, drawSquare, drawEmpSquareX, drawCross,
                drawEmpCross, drawDiamond, drawDot, drawCircle,clear,clearCanvas, fillRec};

function drawHandledLine(ctx, color, sx, sy, ex, ey, onlyAddToPath= false) {
    var slope= NaN;

    if (ex-sx!=0) slope= (ey-sy) / (ex-sx);
    var x, y;
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

    var x0= Math.min(inX1,inX2)+lineWidth;
    var y0= Math.min(inY1,inY2)+lineWidth;
    var width= Math.abs(inX1-inX2)-(2*lineWidth);
    var height= Math.abs(inY1-inY2)-(2*lineWidth);
    strokeRec(ctx,color, lineWidth, x0,y0,width,height);
    var x2= x0+width;
    var y2= y0+height;

    var x1= x0+width;
    var y1= y0;

    var x3= x0;
    var y3= y0+height;

    beginPath(ctx,color,3);

    drawHandledLine(ctx, color, x0,y0,x1,y1,true);
    drawHandledLine(ctx, color, x1,y1,x2,y2, true);
    drawHandledLine(ctx, color, x2,y2,x3,y3,true);
    drawHandledLine(ctx, color, x3,y3,x0,y0,true);

    stroke(ctx);
}

/**
 *
 * @param drawTextAry
 * @param text
 * @param x
 * @param y
 * @param color
 * @param fontFamily
 * @param size
 * @param fontWeight
 * @param fontStyle
 */
function drawText(drawTextAry,text, x,y,color,
                  fontFamily='helvetica', size='9px',
                  fontWeight='normal', fontStyle='normal') {


    //todo
    // it I don't use canvas I need to set css and shadow and calculate translation

    var style= {
        position:'absolute',
        color,
        left:x,
        top:y,
        fontFamily,
        'fontSize': size,
        fontWeight,
        fontStyle,
        'backgroundColor': 'white',
        'MozBorderRadius': '5px',
        'borderRadius': '5px',
        'WebkitBorderRadius': '5px'
    };
    drawTextAry.push({text,style});
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

function addStyle(ctx,renderOptions) {
    var {shadow,rotAngle,translation}= renderOptions;
    if (shadow) {
        var {blur,color,offX,offY} = shadow;
        if (blur) ctx.shawdowBlur= blur;
        if (color) ctx.shawdowColor= color;
        if (offX) ctx.shawdowOffsetX= offX;
        if (offY) ctx.shawdowOffsetY= offY;
    }

    if (rotAngle) {
        ctx.rotation(rotAngle);
    }

    if (translation) {
        ctx.translate(translation.x,translation.y);
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





function rotateAroundScreenPt(worldPt, plot, angle, centerScreenPt) {
    var pti = plot.getScreenCoords(worldPt);
    var xc = centerScreenPt.x;
    var x1 = pti.x - xc;
    var yc = centerScreenPt.y;
    var y1 = pti.y - yc;

    // APPLY ROTATION
    var temp_x1 = x1 * Math.cos(angle) - y1 * Math.sin(angle);
    var temp_y1 = x1 * Math.sin(angle) + y1 * Math.cos(angle);

    // TRANSLATE BACK
    return plot.getWorldCoords(makeScreenPt(temp_x1 + xc, temp_y1 + yc));
}




function drawX(ctx, x, y, color, size, renderOptions, onlyAddToPath) {
    if (!onlyAddToPath) beginPath(ctx,color,1, renderOptions);
    ctx.moveTo(x - size, y - size);
    ctx.lineTo(x + size, y + size);
    ctx.moveTo(x-size,y+size);
    ctx.lineTo(x+size,y-size);
    if (!onlyAddToPath) stroke();
}

function drawSquareX(ctx, x, y, color, size, renderOptions, onlyAddToPath) {
    if (!onlyAddToPath) beginPath(ctx,color,1, renderOptions);
    drawX(ctx,x,y,color,size,renderOptions, onlyAddToPath);
    drawSquare(ctx,x,y,color,size,renderOptions, true);
    if (!onlyAddToPath) stroke();
}

function drawSquare(ctx, x, y, color, size, renderOptions, onlyAddToPath) {
    if (onlyAddToPath) {
        ctx.rect(x - size, y - size, 2 * size, 2 * size);
    }
    else {
        strokeRec(ctx,color,1,x-size,y-size, 2*size, 2*size,renderOptions);
    }
}

function drawEmpSquareX(ctx, x, y, color, size, renderOptions, c1, c2) {
    drawX(ctx,x,y,color,renderOptions, false);
    drawSquare(ctx,x,y,c1,renderOptions,  false);
    drawSquare(ctx,x,y,c2, size+2,renderOptions, false);
}



function drawCross(ctx, x, y, color, size,renderOptions, onlyAddToPath) {
    if (!onlyAddToPath) beginPath(ctx,color,1, renderOptions);
    ctx.moveTo(x-size,y);
    ctx.lineTo(x+size,y);
    ctx.moveTo(x,y-size);
    ctx.lineTo(x,y+size);
    if (!onlyAddToPath) stroke();
}


function drawEmpCross(ctx, x, y, size,renderOptions, color1, color2) {

    drawLine(ctx, color1, 1, x-size,y, x+size, y, renderOptions);
    drawLine(ctx, color1, 1, x,y-size, x, y+size, renderOptions);

    drawLine(ctx, color2, 1, x-(size+1),y, x-(size+2), y, renderOptions);
    drawLine(ctx, color2, 1, x+(size+1),y, x+(size+2), y, renderOptions);

    drawLine(ctx, color2, 1, x,y-(size+1), x, y-(size+2), renderOptions);
    drawLine(ctx, color2, 1, x,y+(size+1), x, y+(size+2), renderOptions);
}


function drawDiamond(ctx, x, y, color, size,renderOptions, onlyAddToPath) {

    if (!onlyAddToPath) beginPath(ctx,color,1, renderOptions);
    ctx.moveTo(x,y-size);
    ctx.lineTo(x+size, y);
    ctx.moveTo(x+size, y);
    ctx.lineTo(x, y+size);
    ctx.moveTo(x, y+size);
    ctx.lineTo(x-size,y);
    ctx.moveTo(x-size,y);
    ctx.lineTo(x,y-size);
    if (!onlyAddToPath) stroke();
}



function drawDot(ctx, x, y, color, size, renderOptions, onlyAddToPath) {
    var begin= size>1 ? y- (size/2) : y;
    var end= size>1 ? y+ (size/2) : y;

    if (!onlyAddToPath) beginPath(ctx,color,1, renderOptions);
    for(var i=begin; (i<=end); i++) {
        ctx.moveTo(x-size,i);
        ctx.lineTo(x+size,i);
    }

    if (!onlyAddToPath) stroke();
}

/**
 *
 * @param ctx
 * @param x
 * @param y
 * @param color
 * @param lineWidth
 * @param size
 * @param renderOptions
 * @param onlyAddToPath
 */
function drawCircle(ctx, x, y, color, lineWidth, size, renderOptions, onlyAddToPath) {
    if (!onlyAddToPath) beginPath(ctx,color,lineWidth, renderOptions);
    var radius= size+2;
    ctx.moveTo(x+radius-1,y);
    ctx.arc(x, y, radius, 0, 2 * Math.PI);
    if (!onlyAddToPath) stroke();
}

function fillRec(ctx, color, x, y, width, height, renderOptions) {
    ctx.save();
    ctx.fillStyle=color;
    if (renderOptions) addStyle(ctx,renderOptions);
    ctx.fillRect(x, y, width, height);
    ctx.restore();
}



function clear(ctx,width,height) {
    if (!ctx) return;
    //for(CanvasLabelShape label : _labelList) {
    //    panel.removeLabel(label.getLabel());
    //}
    ctx.clearRect(0,0,width,height);
    //_labelList.clear();
}


function clearCanvas(canvas) {
    if (!canvas) return;
    var ctx= canvas.getContext('2d');
    clear(ctx,canvas.width,canvas.height);

}
