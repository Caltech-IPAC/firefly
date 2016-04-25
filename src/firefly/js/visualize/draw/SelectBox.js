

import Enum from 'enum';
import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil';
import Point, {makeScreenPt, makeViewPortPt} from '../Point.js';
import {CCUtil} from '../CsysConverter.js';
import VisUtil from '../VisUtil.js';
import {Style} from './DrawingDef.js';


const SELECT_BOX= 'SelectBox';
const DEFAULT_STYLE= Style.STANDARD;
const DEF_SHADOW= DrawUtil.makeShadow(4,1,1,'black');


/**
 *
 * @param {{x:name,y:name,type:string}} pt1 first corner
 * @param {{x:name,y:name,type:string}} pt2  second corner
 * @param {Enum} [style]
 * @return {object}
 */
function makeSelectBox(pt1,pt2,style) {
    if (!pt1 && !pt2) return null;

    var obj= DrawObj.makeDrawObj();
    obj.type= SELECT_BOX;
    obj.pt1= pt1;
    obj.pt2= pt2;
    obj.renderOptions={shadow:DEF_SHADOW};
    obj.style= style || Style.STANDARD;

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
        return false;
    },

    getCenterPt(drawObj) {
        var {pt1,pt2}= drawObj;
        var x = (pt1.x + pt2.x) / 2;
        var y = (pt1.y + pt2.y) / 2;
        return {x, y, type: pt1.type};
    },

    getScreenDist(drawObj,plot, pt) {
        var dist = -1;
        var sp1= plot.getScreenCoords(drawObj.pt1);
        var sp2= plot.getScreenCoords(drawObj.pt2);
        if (sp1 && sp2) {
            var width= Math.abs(sp1.x - sp2.x);
            var height= Math.abs(sp1.y - sp2.y);

            var testPt= makeScreenPt(sp1.x + width/2, sp1.y + height/2);

            var dx= pt.x - testPt.x;
            var dy= pt.y - testPt.y;
            dist= Math.sqrt(dx*dx + dy*dy);
        }
        return dist;
    },

    draw(drawObj,ctx,drawTextAry,plot,def,vpPtM,onlyAddToPath) {
        var drawParams= makeDrawParams(drawObj,def);
        var {pt1,pt2,renderOptions}= drawObj;
        drawImageBox(ctx,pt1, pt2, plot,drawParams,renderOptions);
    },

    toRegion(drawObj,plot, def) {
        var drawParams= makeDrawParams(this,def);
        var {pt1,pt2,renderOptions}= drawObj;
        toRegion(pt1,pt2, plot,drawParams,renderOptions);
    }
};

export default {makeSelectBox,SELECT_BOX,Style,draw};

////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////





function makeDrawParams(selectBox,def) {
    var style= selectBox.style || def.style || DEFAULT_STYLE;
    var innerBoxColor= selectBox.innderBoxColor || def.innderBoxColor || 'white';
    return {
        color: DrawUtil.getColor(selectBox.color,def.color),
        innerBoxColor,
        style
    };
}

function drawImageBox(ctx, pt1, pt2, plot, drawParams, renderOptions) {
    if (plot) {
        var vpt1=plot.getViewPortCoords(pt1);
        var vpt2=plot.getViewPortCoords(pt2);
        if (vpt1 && vpt2 && crossesViewPort(plot, vpt1,vpt2)) {
            drawBox(ctx,vpt1,vpt2,drawParams,renderOptions);
        }
    }
    else {
        drawBox(ctx,pt1,pt2,drawParams,renderOptions);
    }
}

function crossesViewPort(plot, vpt1, vpt2) {
    if (!vpt1 || !vpt2) return false;
    var pt0= vpt1;
    var pt2= vpt2;

    var sWidth= pt2.x-pt0.x;
    var sHeight=pt2.y-pt0.y;

    var pt1= makeViewPortPt(pt0.x+sWidth,pt0.y);
    var pt3= makeViewPortPt(pt0.x,pt0.y+sHeight);
    var retval= false;
    if (plot.pointInViewPort(pt0) || plot.pointInViewPort(pt1) ||
        plot.pointInViewPort(pt2) ||
        plot.pointInViewPort(pt3) ) {
        retval= true;
    }
    else {
        var spt= plot.getScreenCoords(vpt1);
        if (spt) {
            var x0= spt.x;
            var y0= spt.y;
            var dim= plot.viewPort.dim;
            if (sWidth<0) {
                x0+=sWidth;
                sWidth*= -1;
            }
            if (sHeight<0) {
                y0+=sHeight;
                sHeight*= -1;
            }

            retval= VisUtil.intersects(x0,y0, sWidth,sHeight,
                                       plot.viewPort.x, plot.viewPort.y,
                                       dim.width, dim.height );
        }
    }
    return retval;
}



function drawBox(ctx, pt0, pt2, drawParams,renderOptions) {

    var {style,color,innerBoxColor}= drawParams;
    var lineWidth= (style===Style.STANDARD) ? 2 : 1;

    var sWidth= pt2.x-pt0.x;
    var sHeight= pt2.y-pt0.y;
    DrawUtil.strokeRec(ctx,color, lineWidth, pt0.x, pt0.y, sWidth, sHeight);

    if (style==Style.HANDLED) {
        DrawUtil.drawInnerRecWithHandles(ctx, innerBoxColor, 2, pt0.x, pt0.y, pt2.x, pt2.y);
        var pt1= makeViewPortPt(pt0.x+sWidth,pt0.y);
        var pt3= makeViewPortPt(pt0.x,pt0.y+sHeight);

        DrawUtil.beginPath(ctx,color,3,renderOptions);
        DrawUtil.drawHandledLine(ctx, color, pt0.x, pt0.y, pt1.x, pt1.y, true);
        DrawUtil.drawHandledLine(ctx, color, pt1.x, pt1.y, pt2.x, pt2.y, true);
        DrawUtil.drawHandledLine(ctx, color, pt2.x, pt2.y, pt3.x, pt3.y, true);
        DrawUtil.drawHandledLine(ctx, color, pt3.x, pt3.y, pt0.x, pt0.y, true);
        DrawUtil.stroke(ctx);
    }
}


function toRegion(pt1,pt2,plot, drawParams) {
    var wp= CCUtil.getWorldPtRepresentation(pt1);
    var {color} = drawParams;
    var dim;

    var width=  pt2.x-pt1.x;
    var height= pt2.y-pt1.y;

    if (pt1.type===Point.IM_WS_PT || pt1.type===Point.IM_PT) {
        dim= window.ffgwt.util.dd.RegionDimension.makeRegionDimension(
            window.ffgwt.util.dd.RegionValue.makeRegionValue(width, 'IMAGE_PIXEL'),
            window.ffgwt.util.dd.RegionValue.makeRegionValue(height, 'IMAGE_PIXEL'));
    }
    else if (pt1.type===Point.SPT) {
        dim= window.ffgwt.util.dd.RegionDimension.makeRegionDimension(
                window.ffgwt.util.dd.RegionValue.makeRegionValue(width, 'SCREEN_PIXEL'),
                window.ffgwt.util.dd.RegionValue.makeRegionValue(height, 'SCREEN_PIXEL'));
    }
    else  {
        dim= window.ffgwt.util.dd.RegionDimension.makeRegionDimension(
                window.ffgwt.util.dd.RegionValue.makeRegionValue(width, 'DEGREE'),
                window.ffgwt.util.dd.RegionValue.makeRegionValue(height, 'DEGREE'));
    }
    var zero= window.ffgwt.util.dd.RegionValue.makeRegionValue(0, 'SCREEN_PIXEL');
    var r= window.ffgwt.util.dd.RegionBox.makeRegionBox(wp.toString(),dim, zero);
    r.getOptions().setColor(color);
    return [r];
}


