/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */




import DrawObj from './DrawObj';
import DrawUtil from './DrawUtil.js';
import Point, {makeScreenPt, makeWorldPt} from '../Point.js';
import CoordinateSys from '../CoordSys.js';
import VisUtil from '../VisUtil.js';




const DIR_ARROW_DRAW_OBJ= 'DirectionArrayDrawObj';



/**
 *
 * @param {{x:name,y:name,type:string}} startPt
 * @param {{x:name,y:name,type:string}} endPt
 * @param text
 * @return {object}
 */
function makeDirectionArrowDrawObj({startPt, endPt, text}) {
    if (!startPt || !endPt) return null;

    var obj= DrawObj.makeDrawObj();
    obj.type= DIR_ARROW_DRAW_OBJ;
    obj.startPt= startPt;
    obj.endPt= endPt;
    obj.text= text;
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

    usePathOptimization(drawObj) { return false; },

    getCenterPt(drawObj) { drawObj.startPt; },

    getScreenDist(drawObj,plot, pt) {
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
        var {startPt,endPt,renderOptions}= drawObj;
        drawDirectionArrow(ctx,startPt,endPt,drawParams,renderOptions);
    },

    toRegion(drawObj,plot, def) {
        var drawParams= makeDrawParams(drawObj,def);
        var {startPt,endPt,renderOptions}= drawObj;
        toRegion(startPt,endPt,drawParams,renderOptions);
    }
};

export default {makeDirectionArrowDrawObj,draw,DIR_ARROW_DRAW_OBJ};

////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////
////////////////////////////////////////////////






function makeDrawParams(pointDataObj,def) {
    return {
        color: DrawUtil.getColor(this.color,def.color),
        text : pointDataObj.text
    };
}

function drawDirectionArrow(ctx,startPt,endPt,drawParams,renderOptions) {
    var pt1= startPt;
    var pt2= endPt;
    var {color,text}=  drawParams;

    var ret= VisUtil.getArrowCoords(pt1.x, pt1.y, pt2.x, pt2.y);

    var drawList= [];
    drawList.push(makeScreenPt(ret.x1,ret.y1));
    drawList.push(makeScreenPt(ret.x2,ret.y2));
    drawList.push(makeScreenPt(ret.barbX2,ret.barbY2));

    DrawUtil.drawPath(ctx, color,2,drawList,false, renderOptions);

    DrawUtil.drawText(ctx,ret.textX, ret.textY, color, '9px serif',  text, renderOptions);
    DrawUtil.drawText(ctx,ret.textX, ret.textY, color, '9px serif',  text, renderOptions);
}

function toRegion(startPt,endPt,drawParams,renderOptions) {
    var pt1= startPt;
    var pt2= endPt;
    var {color,text}=  drawParams;

    var ret= VisUtil.getArrowCoords(pt1.x, pt1.y, pt2.x, pt2.y);
    var line1= window.ffgwt.util.dd.RegionLines.makeRegionLines(
                     [makeWorldPt(ret.x1,ret.y1, CoordinateSys.SCREEN_PIXEL).toString(),
                      makeWorldPt(ret.x2,ret.y2, CoordinateSys.SCREEN_PIXEL).toString()] );

    var line2= window.ffgwt.util.dd.RegionLines.makeRegionLines(
                    [makeWorldPt(ret.barbX1,ret.barbY1, CoordinateSys.SCREEN_PIXEL).toString(),
                     makeWorldPt(ret.barbX2,ret.barbY2, CoordinateSys.SCREEN_PIXEL).toString()] );

    var regText= window.ffgwt.util.dd.RegionText.makeRegionText(
                   makeWorldPt(ret.textX,ret.textY, CoordinateSys.SCREEN_PIXEL).toString());

    line1.getOptions().setColor(color);
    line2.getOptions().setColor(color);
    regText.getOptions().setColor(color);
    regText.getOptions().setText(text);

    return [line1,line2,regText];
}

