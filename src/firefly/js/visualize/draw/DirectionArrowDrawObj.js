/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil.js';
import Point, {makeScreenPt} from '../Point.js';
import VisUtil from '../VisUtil.js';
import CsysConverter from '../CsysConverter.js';
import {startRegionDes, setRegionPropertyDes, endRegionDes} from '../region/RegionDescription.js';
import {regionPropsList, RegionType} from '../region/Region.js';
import {isEmpty} from 'lodash';


const DIR_ARROW_DRAW_OBJ= 'DirectionArrayDrawObj';



/**
 *
 * @param startPt
 * @param endPt
 * @param text
 * @return {object}
 */
export function makeDirectionArrowDrawObj(startPt, endPt, text) {
    if (!startPt || !endPt) return null;

    const obj= DrawObj.makeDrawObj();
    obj.type= DIR_ARROW_DRAW_OBJ;
    obj.startPt= startPt;
    obj.endPt= endPt;
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

const draw=  {

    usePathOptimization(drawObj) { return false; },

    getCenterPt(drawObj) { return drawObj.startPt; },

    getScreenDist(drawObj,plot, pt) {
        let dist = -1;
        const testPt= plot ? plot.getScreenCoords(drawObj.pt) : drawObj.pt;

        if (testPt.type===Point.SPT) {
            const dx= pt.x - testPt.x;
            const dy= pt.y - testPt.y;
            dist= Math.sqrt(dx*dx + dy*dy);
        }
        return dist;
    },

    /**
     *
     * @param drawObj
     * @param ctx
     * @param drawTextAry
     * @param plot
     * @param def
     * @param vpPtM
     * @param onlyAddToPath
     */
    draw(drawObj,ctx,drawTextAry,plot,def,vpPtM,onlyAddToPath) {
        const drawParams= makeDrawParams(drawObj,def);
        const {startPt,endPt,renderOptions}= drawObj;
        drawDirectionArrow(ctx,drawTextAry, plot, startPt,endPt,drawParams,renderOptions);
    },

    toRegion(drawObj,plot, def) {
        const drawParams= makeDrawParams(drawObj,def);
        const {startPt,endPt,renderOptions}= drawObj;

        return toRegion(startPt,endPt,plot,drawParams,renderOptions);
    }
};

export default {makeDirectionArrowDrawObj,draw,DIR_ARROW_DRAW_OBJ};

////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////



function makeDrawParams(obj,def) {
    return {
        color: DrawUtil.getColor(obj.color,def.color),
        text : obj.text
    };
}

/**
 * @summary draw direction arrow on image coordinate
 * @param ctx
 * @param drawTextAry
 * @param plot
 * @param startPt
 * @param endPt
 * @param drawParams
 * @param renderOptions
 */
function drawDirectionArrow(ctx,drawTextAry,plot, startPt,endPt,drawParams,renderOptions) {
    const pt1= plot ? plot.getScreenCoords(startPt) : startPt;
    const pt2= plot ? plot.getScreenCoords(endPt) : endPt;
    const {color,text}=  drawParams;

    const ret= VisUtil.getArrowCoords(pt1.x, pt1.y, pt2.x, pt2.y);
    const drawList= [];
    const textScreen = makeScreenPt(ret.textX, ret.textY);
    const textLoc = plot ? plot.getDeviceCoords(textScreen) : textScreen;
    const p1Screen = makeScreenPt(ret.x1,ret.y1);
    const p2Screen = makeScreenPt(ret.x2,ret.y2);
    const barScreen = makeScreenPt(ret.barbX2,ret.barbY2);

    drawList.push(plot ? plot.getDeviceCoords(p1Screen) : p1Screen) ;
    drawList.push(plot ? plot.getDeviceCoords(p2Screen) : p2Screen);
    drawList.push(plot ? plot.getDeviceCoords(barScreen) : barScreen);

    DrawUtil.drawPath(ctx, color,2,drawList,false, renderOptions);
    DrawUtil.drawText(drawTextAry, text, textLoc.x, textLoc.y, color, renderOptions);
}

function toRegion(startPt,endPt,plot,drawParams,renderOptions) {
    const pt1 = startPt;    // screen point
    const pt2 = endPt;
    const {color,text}=  drawParams;
    const ret = VisUtil.getArrowCoords(pt1.x, pt1.y, pt2.x, pt2.y);
    const cc = CsysConverter.make(plot);
    let des;
    const retList = [];

    const wpt1 = cc.getWorldCoords(makeScreenPt(ret.x1, ret.y1));
    const wpt2 = cc.getWorldCoords(makeScreenPt(ret.x2, ret.y2));
    const barbPt = cc.getWorldCoords(makeScreenPt(ret.barbX2, ret.barbY2));
    const textPt = cc.getWorldCoords(makeScreenPt(ret.textX, ret.textY));

    des = startRegionDes(RegionType.line, cc, [wpt1, wpt2], null, null);
    if (isEmpty(des)) return [];

    des += setRegionPropertyDes(regionPropsList.COLOR, color) +
           setRegionPropertyDes(regionPropsList.LNWIDTH, 2);
    des = endRegionDes(des);
    retList.push(des);

    des = startRegionDes(RegionType.line, cc, [wpt2, barbPt], null, null);
    if (isEmpty(des)) return [];

    des += setRegionPropertyDes(regionPropsList.COLOR, color) +
        setRegionPropertyDes(regionPropsList.LNWIDTH, 2);
    des = endRegionDes(des);
    retList.push(des);

    des = startRegionDes(RegionType.text, cc, [textPt]);
    if (isEmpty(des)) return [];

    des += setRegionPropertyDes(regionPropsList.COLOR, color);

    if (text) {
        des += setRegionPropertyDes(regionPropsList.TEXT, text) +
               setRegionPropertyDes(regionPropsList.FONT, {name: 'helvetica', size: 9, weight: 'normal', slant: 'normal'} );
    }
    des = endRegionDes(des);

    retList.push(des);
    return retList;
}



