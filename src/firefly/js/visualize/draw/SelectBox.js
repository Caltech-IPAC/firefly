
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil';
import {makeScreenPt, makeDevicePt, makeImagePt} from '../Point.js';
import VisUtil from '../VisUtil.js';
import {Style} from './DrawingDef.js';
import CsysConverter from '../CsysConverter.js';
import {RegionValue, RegionDimension, RegionValueUnit, RegionType, regionPropsList} from '../region/Region.js';
import {startRegionDes, setRegionPropertyDes, endRegionDes} from '../region/RegionDescription.js';


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

    const obj= DrawObj.makeDrawObj();
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

const draw=  {

    usePathOptimization(drawObj) {
        return false;
    },

    getCenterPt(drawObj) {
        const {pt1,pt2}= drawObj;
        const x = (pt1.x + pt2.x) / 2;
        const y = (pt1.y + pt2.y) / 2;
        return {x, y, type: pt1.type};
    },

    getScreenDist(drawObj,plot, pt) {
        let dist = -1;
        const sp1= plot.getScreenCoords(drawObj.pt1);
        const sp2= plot.getScreenCoords(drawObj.pt2);
        if (sp1 && sp2) {
            const width= Math.abs(sp1.x - sp2.x);
            const height= Math.abs(sp1.y - sp2.y);

            const testPt= makeScreenPt(sp1.x + width/2, sp1.y + height/2);

            const dx= pt.x - testPt.x;
            const dy= pt.y - testPt.y;
            dist= Math.sqrt(dx*dx + dy*dy);
        }
        return dist;
    },

    draw(drawObj,ctx,drawTextAry,plot,def,vpPtM,onlyAddToPath) {
        const drawParams= makeDrawParams(drawObj,def);
        const {pt1,pt2,renderOptions}= drawObj;
        drawImageBox(ctx,pt1, pt2, plot,drawParams,renderOptions);
    },

    toRegion(drawObj,plot, def) {
        const drawParams= makeDrawParams(drawObj, def);
        const {pt1,pt2,renderOptions}= drawObj;

        return toRegion(pt1, pt2, plot, drawParams, renderOptions);
    }
};

export default {makeSelectBox,SELECT_BOX,Style,draw};

////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////





function makeDrawParams(selectBox,def) {
    const style= selectBox.style || def.style || DEFAULT_STYLE;
    const innerBoxColor= selectBox.innderBoxColor || def.innderBoxColor || 'white';
    return {
        color: DrawUtil.getColor(selectBox.color,def.color),
        innerBoxColor,
        style
    };
}

function drawImageBox(ctx, pt1, pt2, plot, drawParams, renderOptions) {
    if (plot) {
        const devPt1= plot.getDeviceCoords(pt1);
        const devPt2= plot.getDeviceCoords(pt2);
        if (devPt1 && devPt2 && crossesDisplay(plot, devPt1,devPt2)) {
            drawBox(ctx,devPt1,devPt2,drawParams,renderOptions);
        }
    }
    else {
        drawBox(ctx,pt1,pt2,drawParams,renderOptions);
    }
}

function crossesDisplay(plot, devPt1, devPt2) {
    if (!devPt1 || !devPt2) return false;
    const pt0= devPt1;
    const pt2= devPt2;

    let sWidth= pt2.x-pt0.x;
    let sHeight=pt2.y-pt0.y;

    const pt1= makeDevicePt(pt0.x+sWidth,pt0.y);
    const pt3= makeDevicePt(pt0.x,pt0.y+sHeight);
    let retval= false;
    if (plot.pointOnDisplay(pt0) || plot.pointOnDisplay(pt1) ||
        plot.pointOnDisplay(pt2) ||
        plot.pointOnDisplay(pt3) ) {
        retval= true;
    }
    else {
        const spt= plot.getScreenCoords(devPt1);
        if (spt) {
            let x0= spt.x;
            let y0= spt.y;
            const dim= plot.viewDim;
            if (sWidth<0) {
                x0+=sWidth;
                sWidth*= -1;
            }
            if (sHeight<0) {
                y0+=sHeight;
                sHeight*= -1;
            }

            retval= VisUtil.intersects(x0,y0, sWidth,sHeight, //todo: this need to be fixed
                                       0,0, dim.width, dim.height );
        }
    }
    return retval;
}


/**
 *
 * @param ctx
 * @param {DevicePt} pt0
 * @param {DevicePt} pt2
 * @param drawParams
 * @param renderOptions
 */
function drawBox(ctx, pt0, pt2, drawParams,renderOptions) {

    const {style,color,innerBoxColor}= drawParams;
    const lineWidth= (style===Style.STANDARD) ? 2 : 1;

    const sWidth= pt2.x-pt0.x;
    const sHeight= pt2.y-pt0.y;
    DrawUtil.strokeRec(ctx,color, lineWidth, pt0.x, pt0.y, sWidth, sHeight);

    if (style===Style.HANDLED) {
        DrawUtil.drawInnerRecWithHandles(ctx, innerBoxColor, 2, pt0.x, pt0.y, pt2.x, pt2.y);
        const pt1= makeDevicePt(pt0.x+sWidth,pt0.y);
        const pt3= makeDevicePt(pt0.x,pt0.y+sHeight);

        // const devPt1=  todo

        DrawUtil.beginPath(ctx,color,3,renderOptions);
        DrawUtil.drawHandledLine(ctx, color, pt0.x, pt0.y, pt1.x, pt1.y, true);
        DrawUtil.drawHandledLine(ctx, color, pt1.x, pt1.y, pt2.x, pt2.y, true);
        DrawUtil.drawHandledLine(ctx, color, pt2.x, pt2.y, pt3.x, pt3.y, true);
        DrawUtil.drawHandledLine(ctx, color, pt3.x, pt3.y, pt0.x, pt0.y, true);
        DrawUtil.stroke(ctx);
    }
}


function toRegion(pt1,pt2,plot, drawParams, renderOptions) {
    var {innerBoxColor, color, style} = drawParams;
    var dim, innerDim;
    var lineWidth= (style===Style.STANDARD) ? 2 : 1;
    var innerLineWidth = 2;
    var screenW, screenH;
    var centerPt;
    var des;
    var cc = CsysConverter.make(plot);
    var retList  = [];

    // convert to image point, calculate dimension on image pixel
    // make selectbox display invariant on various screen pixel systems
    var wpt1 = cc.getImageCoords(pt1);
    var wpt2 = cc.getImageCoords(pt2);

    screenW = Math.abs(wpt1.x - wpt2.x) * cc.zoomFactor;
    screenH = Math.abs(wpt1.y - wpt2.y) * cc.zoomFactor;

    centerPt =  makeImagePt((wpt1.x+wpt2.x)/2, (wpt1.y+wpt2.y)/2);
    dim = RegionDimension(
              RegionValue(screenW, RegionValueUnit.SCREEN_PIXEL),
              RegionValue(screenH, RegionValueUnit.SCREEN_PIXEL));
    innerDim =  RegionDimension(
              RegionValue((screenW - (2 * innerLineWidth)), RegionValueUnit.SCREEN_PIXEL),
              RegionValue((screenH - (2 * innerLineWidth)), RegionValueUnit.SCREEN_PIXEL));

    // box in color of 'innerBoxColor'
    des = startRegionDes(RegionType.box, cc, [centerPt], [innerDim], null, true);
    if (des.length === 0) return [];
    des += setRegionPropertyDes(regionPropsList.COLOR, innerBoxColor) +
           setRegionPropertyDes(regionPropsList.LNWIDTH, innerLineWidth);
    des = endRegionDes(des);
    retList.push(des);

    // box in color of 'color'

    des = startRegionDes(RegionType.box, cc, [centerPt], [dim], null, true);
    if (des.length === 0) return [];
    des += setRegionPropertyDes(regionPropsList.COLOR, color) +
           setRegionPropertyDes(regionPropsList.LNWIDTH, lineWidth);
    des = endRegionDes(des);
    retList.push(des);

    return retList;
}


