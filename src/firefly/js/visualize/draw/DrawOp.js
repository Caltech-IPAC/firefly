/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import PointDataObj, {POINT_DATA_OBJ} from './PointDataObj.js';
import SelectBox from './SelectBox.js';
import ShapeDataObj from './ShapeDataObj.js';
import FootprintObj from './FootprintObj.js';
import DirectionArrowDrawObj from './DirectionArrowDrawObj.js';
import FootprintObj from './FootprintObj.js'
import MarkerFootprintObj from './MarkerFootprintObj.js';

export var drawTypes= {
    [POINT_DATA_OBJ] : PointDataObj.draw,
    [SelectBox.SELECT_BOX] : SelectBox.draw,
    [FootprintObj.FOOTPRINT_OBJ] : FootprintObj.draw,
    [DirectionArrowDrawObj.DIR_ARROW_DRAW_OBJ] : DirectionArrowDrawObj.draw,
    [ShapeDataObj.SHAPE_DATA_OBJ] : ShapeDataObj.draw,
    [MarkerFootprintObj.MARKER_DATA_OBJ] : MarkerFootprintObj.draw
};

class DrawOp {

    /**
     *
     * @param {{type:string}} drawObj
     */
    static usePathOptimization(drawObj,drawingDef) {
        return op(drawObj,'usePathOptimization',false)(drawObj,drawingDef);
    }

    /**
     *
     * @param drawObj
     */
    static getCenterPt(drawObj) {
        return op(drawObj,'getCenterPt')(drawObj);
    }

    /**
     *
     * @param drawObj
     * @param plot
     * @param pt
     */
    static getScreenDist(drawObj,plot, pt) {
        return op(drawObj,'getScreenDist')(drawObj,plot, pt);
    }

    /**
     *
     * @param drawObj
     * @param ctx
     * @param csysConv
     * @param def
     * @param vpPtM
     * @param onlyAddToPath
     */
    static draw(drawObj,ctx,drawTextAry,csysConv,def,vpPtM,onlyAddToPath) {
        op(drawObj,'draw')(drawObj, ctx, drawTextAry, csysConv, def, vpPtM,onlyAddToPath);
    }

    /**
     *
     * @param drawObj
     * @param plot
     * @param def
     */
    static toRegion(drawObj,plot, def) {
        return op(drawObj,'toRegion')(drawObj, plot,def);
    }

    /**
     *
     * @param drawObj
     * @param plot
     * @param apt
     */
    static translateTo(drawObj,plot, apt) {
        return op(drawObj,'translateTo',false)(drawObj, plot,apt);
    }

    /**
     *
     * @param drawObj
     * @param plot
     * @param angle
     * @param worldPt
     */
    static rotateAround(drawObj, plot, angle, worldPt) {
        return op(drawObj,'rotateAround',false)(drawObj, plot, angle, worldPt);
    }

    /**
     *
     * @param {string }key
     * @param {{draw:function, toRegion:function, getScreenDist:function, getCenterPt:function}} drawFunctionObj
     */
    static addDrawType(key,drawFunctionObj) { drawTypes[key]= drawFunctionObj; }

    static makeHighlight(drawObj, plot, def = {}) {
        return op(drawObj, 'makeHighlight', false)(drawObj, plot, def);
    }

    static isScreenPointInside(screenPt, drawObj, plot, def = {}) {
        return op(drawObj, 'isScreenPointInside', false)(screenPt, drawObj, plot, def);
    }
}



const noOp= () => false;

function op(drawObj, func, required=true) {
    if (drawObj[func]) return drawObj[func];

    var {type}= drawObj;
    if (type && drawTypes[type] && drawTypes[type][func]) {
        return drawTypes[drawObj.type][func];
    }

    if (required) {
        return () => console.log(`function ${func} is required for drawObj ${type?'of '+type:''}`);
    }
    else {
        return noOp;
    }
}

export default DrawOp;
