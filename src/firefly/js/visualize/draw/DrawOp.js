/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import PointDataObj, {POINT_DATA_OBJ} from './PointDataObj.js';
import SelectBox from './SelectBox.js';
import ShapeDataObj from './ShapeDataObj.js';
import FootprintObj from './FootprintObj.js';
import DirectionArrowDrawObj from './DirectionArrowDrawObj.js';
import MarkerFootprintObj from './MarkerFootprintObj.js';
import MocObj from './MocObj.js';
import ImageLineBasedObj from './ImageLineBasedObj.js';
import {has} from 'lodash';

export var drawTypes= {
    [POINT_DATA_OBJ] : PointDataObj.draw,
    [SelectBox.SELECT_BOX] : SelectBox.draw,
    [FootprintObj.FOOTPRINT_OBJ] : FootprintObj.draw,
    [DirectionArrowDrawObj.DIR_ARROW_DRAW_OBJ] : DirectionArrowDrawObj.draw,
    [ShapeDataObj.SHAPE_DATA_OBJ] : ShapeDataObj.draw,
    [MarkerFootprintObj.MARKER_DATA_OBJ] : MarkerFootprintObj.draw,
    [MocObj.MOC_OBJ] : MocObj.draw,
    [ImageLineBasedObj.IMGFP_OBJ] : ImageLineBasedObj.draw
};

class DrawOp {

    /**
     *
     * @param {{type:string}} drawObj
     * @param {Object} drawingDef
     */
    static usePathOptimization(drawObj,drawingDef) {
        return op(drawObj,'usePathOptimization',false)(drawObj,drawingDef);
    }

    /**
     * @param drawObj
     * @return {Point}
     */
    static getCenterPt(drawObj) {
        return op(drawObj,'getCenterPt')(drawObj);
    }

    /**
     *
     * @param drawObj
     * @param csysConv
     * @param pt
     */
    static getScreenDist(drawObj,csysConv, pt) {
        return op(drawObj,'getScreenDist')(drawObj,csysConv, pt);
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
    static draw(drawObj,ctx,csysConv,def,vpPtM,onlyAddToPath) {
        if (drawObj && (!has(drawObj, 'isRendered') || drawObj.isRendered)) {
            op(drawObj, 'draw')(drawObj, ctx, csysConv, def, vpPtM, onlyAddToPath);
        }
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
