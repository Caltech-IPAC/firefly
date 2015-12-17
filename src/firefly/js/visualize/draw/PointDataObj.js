/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */




import Enum from 'enum';
import DrawObj from './DrawObj.js';
import DrawUtil from './DrawUtil.js';
import Point from '../Point.js';
import {CCUtil} from '../CsysConverter.js';




export const DrawSymbol = new Enum([
    'X','SQUARE','CROSS','DIAMOND','DOT','CIRCLE', 'SQUARE_X', 'EMP_CROSS','EMP_SQUARE_X'
]);

export const POINT_DATA_OBJ= 'PointDataObj';
const DEFAULT_SIZE= 4;
const DOT_DEFAULT_SIZE = 1;
const DEFAULT_SYMBOL = DrawSymbol.X;




/**
 *
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
        return drawObj.symbol!=DrawSymbol.EMP_CROSS && drawObj.symbol!=DrawSymbol.EMP_SQUARE_X;
    },

    getCenterPt(drawObj) {return drawObj.pt; },

    getScreenDist(drawObj, plot, pt) {
        var dist = -1;
        var testPt= plot ? plot.getScreenCoords(drawObj.pt) : drawObj.pt;

        if (testPt.type===Point.SPT) {
            var dx= pt.x - testPt.x;
            var dy= pt.y - testPt.y;
            dist= Math.sqrt(dx*dx + dy*dy);
        }

        return dist;
    },

    draw(drawObj,ctx,plot,def,vpPtM,onlyAddToPath) {
        var drawParams= makeDrawParams(drawObj,def);
        drawPt(ctx,drawObj.pt, plot,drawParams,drawObj.renderOptions,vpPtM,onlyAddToPath);
    },

    toRegion(drawObj,plot, def) {
        var drawParams= makeDrawParams(drawObj,def);
        toRegion(drawObj.pt, plot,drawParams,drawObj.renderOptions);
    }
};

export default {make,draw};

////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////



function makeDrawParams(pointDataObj,def) {
    var symbol= pointDataObj.symbol || def.symbol || DEFAULT_SYMBOL;
    var size= (symbol===DrawSymbol.DOT) ? pointDataObj.size || def.size || DOT_DEFAULT_SIZE :
                                          pointDataObj.size || def.size || DEFAULT_SIZE;

    return {
        color: DrawUtil.getColor(pointDataObj.color,def.color),
        text : pointDataObj.text,
        size,
        symbol
    };

}

/**
 *
 * @param ctx
 * @param pt
 * @param plot
 * @param drawParams
 * @param renderOptions
 * @param vpPtM
 * @param onlyAddToPath
 */
function drawPt(ctx, pt, plot,drawParams, renderOptions, vpPtM, onlyAddToPath) {
    if (!pt) return;

    if (!plot || pt.type===Point.SPT) {
        drawXY(ctx,pt.x,pt.y,drawParams, renderOptions, onlyAddToPath);
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
            drawXY(ctx,vpPt.x,vpPt.y,drawParams, renderOptions, onlyAddToPath);
        }
    }
}


function drawXY(ctx, x, y, drawParams,renderOptions, onlyAddToPath) {
    var {text}= drawParams;
    drawSymbolOnPlot(ctx, x,y, drawParams,renderOptions, onlyAddToPath);
    if (text) DrawUtil.drawText(ctx,x+5,y,drawParams.color,'9px serif',text);
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
            DrawUtil.drawCircle(ctx, x, y, color, size, renderOptions, onlyAddToPath);
            break;
        default :
            break;
    }
}




function toRegion(pt, plot,drawParams,renderOptions) {
    var r;
    var {size}= drawParams;
    var wp= CCUtil.getWorldPtRepresentation(pt);
    var pointType;
    switch (drawParams.symbol) {
        case DrawSymbol.X :
            pointType='X';
            break;
        case DrawSymbol.EMP_CROSS :
        case DrawSymbol.CROSS :
            pointType='Cross';
            break;
        case DrawSymbol.SQUARE :
            pointType='Box';
            break;
        case DrawSymbol.DIAMOND :
            pointType='Diamond';
            break;
        case DrawSymbol.DOT :
            size= 2;
            pointType='Box';
            break;
        case DrawSymbol.CIRCLE :
            pointType='Circle';
            break;
        default :
            r= null;
    }
    r= window.ffgwt.util.dd.RegionPoint.makeRegionPoint(wp.toString(),pointType,size);
    r.getOptions().setColor(drawParams.color);
    return [r];
}



