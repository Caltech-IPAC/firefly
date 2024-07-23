
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil';
import {makeScreenPt, makeDevicePt, makeImagePt} from '../Point.js';
import {intersects} from '../VisUtil.js';
import {Style} from './DrawingDef.js';
import CsysConverter from '../CsysConverter.js';
import {RegionValue, RegionDimension, RegionValueUnit, RegionType, regionPropsList} from '../region/Region.js';
import {startRegionDes, setRegionPropertyDes, endRegionDes} from '../region/RegionDescription.js';
import {SelectedShape} from '../../drawingLayers/SelectedShape';

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

    draw(drawObj,ctx,plot,def,vpPtM,onlyAddToPath) {
        const drawParams= makeDrawParams(drawObj,def);
        const {pt1,pt2,renderOptions}= drawObj;
        drawImageBox(ctx,pt1, pt2, plot,drawParams,renderOptions);
    },

    toRegion(drawObj,plot, def) {
        const drawParams= makeDrawParams(drawObj, def);
        const {pt1,pt2,renderOptions}= drawObj;
        const {rotAngle=0.0} = drawObj;

        return toRegion(pt1, pt2, plot, drawParams, renderOptions, rotAngle);
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
    const {selectedShape, handleColor} = selectBox;
    return {
        color: DrawUtil.getColor(selectBox.color,def.color),
        innerBoxColor,
        style,
        selectedShape,
        handleColor
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

            retval= intersects(x0,y0, sWidth,sHeight, //todo: this need to be fixed
                                       0,0, dim.width, dim.height );
        }
    }
    return retval;
}


/**
 * @param ctx
 * @param {DevicePt} pt0
 * @param {DevicePt} pt2
 * @param drawParams
 * @param renderOptions
 */
function drawBox(ctx, pt0, pt2, drawParams,renderOptions) {

    const   {style,color,innerBoxColor, selectedShape=SelectedShape.rect.key}= drawParams;
    const   {handleColor=color} = drawParams;
    const lineWidth= (style===Style.STANDARD) ? 2 : 1;

    const sWidth= pt2.x-pt0.x;
    const sHeight= pt2.y-pt0.y;

    if (selectedShape === SelectedShape.rect.key) {    // no rectangle for handle only case
        DrawUtil.strokeRec(ctx, color, lineWidth, pt0.x, pt0.y, sWidth, sHeight);
    } else if (selectedShape === SelectedShape.circle.key) {
        DrawUtil.drawCircleWithHandles(ctx, color, lineWidth, pt0.x, pt0.y, pt2.x, pt2.y);
    } else if (selectedShape === SelectedShape.ellipse.key) {
        DrawUtil.drawEclipseWithHandles(ctx, color, lineWidth, pt0.x, pt0.y, pt2.x, pt2.y);
    }

    if (style === Style.HANDLED) {

        if (selectedShape === SelectedShape.rect.key) {    // no rectangle for handle only case
            DrawUtil.drawInnerRecWithHandles(ctx, innerBoxColor, 2, pt0.x, pt0.y, pt2.x, pt2.y, lineWidth);
        } else if (selectedShape === SelectedShape.circle.key) {
            DrawUtil.drawCircleWithHandles(ctx, innerBoxColor, 2, pt0.x, pt0.y, pt2.x, pt2.y, lineWidth);
        } else if (selectedShape === SelectedShape.circle.key) {
            DrawUtil.drawEclipseWithHandles(ctx, innerBoxColor, 2, pt0.x, pt0.y, pt2.x, pt2.y, lineWidth);

        }

        const pt1= makeDevicePt(pt0.x+sWidth,pt0.y);
        const pt3= makeDevicePt(pt0.x,pt0.y+sHeight);

        // const devPt1=  todo
        DrawUtil.beginPath(ctx, handleColor, 3, renderOptions);
        DrawUtil.drawHandledLine(ctx, handleColor, pt0.x, pt0.y, pt1.x, pt1.y, true);
        DrawUtil.drawHandledLine(ctx, handleColor, pt1.x, pt1.y, pt2.x, pt2.y, true);
        DrawUtil.drawHandledLine(ctx, handleColor, pt2.x, pt2.y, pt3.x, pt3.y, true);
        DrawUtil.drawHandledLine(ctx, handleColor, pt3.x, pt3.y, pt0.x, pt0.y, true);
        DrawUtil.stroke(ctx);
    }
}


function toRegion(pt1,pt2,plot, drawParams, renderOptions, rotAngle=0.0) {
    const {innerBoxColor, color, style, selectedShape=SelectedShape.rect.key} = drawParams;
    const lineWidth= (style===Style.STANDARD) ? 2 : 1;
    const cc = CsysConverter.make(plot);
    const innerLineWidth = 2;
    let   des;
    const retList  = [];

    // convert to image point, calculate dimension on image pixel
    // make selectbox display invariant on various screen pixel systems
    const dev1 = cc.getDeviceCoords(pt1);
    const dev3 = cc.getDeviceCoords(pt2);
    const dev2 = makeDevicePt(dev3.x, dev1.y);
    const imgPt1 = cc.getImageCoords(dev1);
    const imgPt2 = cc.getImageCoords(dev2);
    const imgPt3 = cc.getImageCoords(dev3);

    const dist = (pt1, pt2) => {
        return Math.sqrt(Math.pow(pt1.x - pt2.x, 2) + Math.pow(pt1.y-pt2.y, 2));
    };
    const imgW = dist(imgPt1, imgPt2);
    const imgH = dist(imgPt2, imgPt3);
    const imgLineWidth = innerLineWidth/cc.zoomFactor;

    const centerPt =  makeImagePt((imgPt1.x+imgPt3.x)/2, (imgPt1.y+imgPt3.y)/2);
    const r1 = selectedShape === SelectedShape.rect.key ? imgW : imgW/2;
    const r2 = selectedShape === SelectedShape.rect.key ? imgH : imgH/2;
    const regionType = selectedShape === SelectedShape.rect.key ? RegionType.box : RegionType.ellipse;
    const angle = rotAngle ? RegionValue(-rotAngle*180.0/Math.PI, RegionValueUnit.DEGREE) : RegionValue(0, RegionValueUnit.DEGREE);

    const dim = RegionDimension(
        RegionValue(r1, RegionValueUnit.IMAGE_PIXEL),
        RegionValue(r2, RegionValueUnit.IMAGE_PIXEL));
    const innerDim = RegionDimension(
        RegionValue((r1 - (2 * imgLineWidth)), RegionValueUnit.IMAGE_PIXEL),
        RegionValue((r2 - (2 * imgLineWidth)), RegionValueUnit.IMAGE_PIXEL));

    // box in color of 'innerBoxColor'
    des = startRegionDes(regionType, cc, [centerPt], [innerDim], null, true, angle);
    if (des.length === 0) return [];
    des += setRegionPropertyDes(regionPropsList.COLOR, innerBoxColor) +
           setRegionPropertyDes(regionPropsList.LNWIDTH, innerLineWidth);
    des = endRegionDes(des);
    retList.push(des);

    // box in color of 'color'

    des = startRegionDes(regionType, cc, [centerPt], [dim], null, true, angle);
    if (des.length === 0) return [];
    des += setRegionPropertyDes(regionPropsList.COLOR, color) +
           setRegionPropertyDes(regionPropsList.LNWIDTH, lineWidth);
    des = endRegionDes(des);
    retList.push(des);

    return retList;
}


