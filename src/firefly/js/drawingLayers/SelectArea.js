/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import DrawingLayerReducer from './DrawLayerReducer.js';
import DrawingLayerCntlr from '../visualize/DrawingLayerCntlr.js';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawingLayer  from '../visualize/draw/DrawingLayer.js';
import {MouseState} from '../visualize/VisMouseCntlr.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {makeScreenPt} from '../visualize/Point.js';
import BrowserInfo from '../util/BrowserInfo.js';
import VisUtil from '../visualize/VisUtil.js';
import SelectBox from '../visualize/draw/SelectBox.js';
import PlotViewUtils from '../visualize/PlotViewUtil.js';
import {flux} from '../Firefly.js';

import Enum from 'enum';




const Corner = new Enum([ 'NE','NW','SE','SW' ]);



const selHelpText=
`Click and drag to select an area, then choose from Options
To modify your selection: click on the corners
To select again: hold down the shift key, click, and drag`;

const editHelpText=
`Click and drag a corner to resize selection area, then choose from Options.
To select again: hold down the shift key, click, and drag`;

const EDIT_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;


const LAYER_ID= 'SELECT_AREA';


export default {dispatchInitSelectArea, LAYER_ID};


function dispatchInitSelectArea() {
    DrawingLayerCntlr.dispatchCreateDrawLayer(LAYER_ID,makeLayerReducer());
}

function dispatchSelectAreaEnd(mouseStatePayload) {
    var {plotId,drawingLayer}= mouseStatePayload;
    var selectBox= drawingLayer.drawData.data[0];
    var sel= {pt0:selectBox.pt1,pt1:selectBox.pt2};
    ImagePlotCntlr.dispatchAttributeChange(plotId,true,PlotAttribute.SELECTION,sel);
    flux.process({type:DrawingLayerCntlr.SELECT_AREA_END, payload:mouseStatePayload} );
}


/**
 *
 * @return {Function}
 */
function makeLayerReducer() {

    var drawingDef= makeDrawingDef('black');
    var pairs= {
        [MouseState.MOVE.key]: DrawingLayerCntlr.SELECT_MOUSE_LOC,
        [MouseState.DRAG.key]: {exclusive: true, actionType:DrawingLayerCntlr.SELECT_AREA_MOVE},
        [MouseState.DOWN.key]: DrawingLayerCntlr.SELECT_AREA_START,
        [MouseState.UP.key]: dispatchSelectAreaEnd
    };

    var actionTypes= [DrawingLayerCntlr.SELECT_AREA_START,
                      DrawingLayerCntlr.SELECT_AREA_MOVE,
                      DrawingLayerCntlr.SELECT_AREA_END,
                      DrawingLayerCntlr.SELECT_MOUSE_LOC];

    var layer= DrawingLayer.makeDrawingLayer(
        LAYER_ID,
        {canUseMouse:true},
        drawingDef, actionTypes, pairs );

    return DrawingLayerReducer.makeReducer( layer, null, getLayerChanges);
}


function getLayerChanges(drawingLayer, action) {

    switch (action.type) {
        case DrawingLayerCntlr.SELECT_AREA_START:
            return start(drawingLayer,action);
            break;
        case DrawingLayerCntlr.SELECT_AREA_MOVE:
            return drag(drawingLayer,action);
            break;
        case DrawingLayerCntlr.SELECT_AREA_END:
            return end(drawingLayer,action);
            break;
        case DrawingLayerCntlr.ATTACH_LAYER_TO_PLOT:
            return attach();
            break;
        case DrawingLayerCntlr.SELECT_MOUSE_LOC:
            return moveMouse(drawingLayer,action);
            break;
    }

}

//function getDrawData(dataType, plotId, drawingLayer, action, lastDataRet) {
//
//    switch (dataType) {
//        case DataTypes.DATA:
//            return computeDrawingLayer(action);
//            break;
//        case DataTypes.HIGHLIGHT_DATA:
//            break;
//        case DataTypes.SELECTED_IDX_ARY:
//            break;
//    }
//    return null;
//}



function attach() {
    return {
        mode: 'select',
        helpLine: selHelpText
    };
}

function moveMouse(drawingLayer,action) {
    var {screenPt,plotId}= action.payload;
    if (drawingLayer.mode==='edit') {
        var pv= PlotViewUtils.getPlotViewById(plotId);
        if (!pv) return;
        var cc= CsysConverter.make(pv.primaryPlot);
        var ptAry= getPtAry(pv);
        var corner= findClosestCorner(cc,ptAry, screenPt, EDIT_DISTANCE);
        if (corner) {
            switch (corner) {
                case Corner.NE: return {cursor: 'ne-resize'};
                case Corner.NW: return {cursor: 'nw-resize'};
                case Corner.SE: return {cursor: 'se-resize'};
                case Corner.SW: return {cursor: 'sw-resize'};
            }
        } else {
            return {cursor: ''};
        }
    }
}


function start(drawingLayer,action) {
    var {screenPt,imagePt,plotId,shiftDown}= action.payload;
    var {mode}= drawingLayer;
    var pv= PlotViewUtils.getPlotViewById(plotId);
    if (!pv) return;
    var plot= pv.primaryPlot;

    var retObj= {};
    if (mode==='select' || shiftDown) {
        retObj= setupSelect(pv,imagePt);
    }
    else if (mode==='edit') {
        var ptAry= getPtAry(pv);
        if (!ptAry) return retObj;

        var idx= findClosestPtIdx(ptAry,screenPt);
        var cc= CsysConverter.make(plot);
        var testPt= cc.getScreenCoords(ptAry[idx]);
        if (!testPt) return {};

        if (distance(testPt,screenPt)<EDIT_DISTANCE) {
            var oppoIdx= (idx+2) % 4;
            retObj.firstPt= cc.getImageWorkSpaceCoords(ptAry[oppoIdx]);
            retObj.currentPt= cc.getImageWorkSpaceCoords(ptAry[idx]);
            if (retObj.firstPt==null || retObj.currentPt==null) return {};
        }
    }
    return retObj;

}

function getPtAry(pv) {
    var sel= pv.primaryPlot.attributes[PlotAttribute.SELECTION];
    if (!sel) return null;
    var cc= CsysConverter.make(pv.primaryPlot);
    var ptAry=[];
    ptAry[0]= cc.getScreenCoords(sel.pt0);
    ptAry[2]= cc.getScreenCoords(sel.pt1);
    if (!ptAry[0] || !ptAry[2]) return null;
    ptAry[1] = makeScreenPt(ptAry[2].x, ptAry[0].y);
    ptAry[3] = makeScreenPt(ptAry[0].x, ptAry[2].y);
    return ptAry;
}





function drag(drawingLayer,action) {
    var {imagePt,plotId}= action.payload;
    var pv= PlotViewUtils.getPlotViewById(plotId);
    if (!pv) return;
    var plot= pv.primaryPlot;
    var drawSel= makeSelectObj(drawingLayer.firstPt, imagePt, CsysConverter.make(plot));
    return {currentPt:imagePt, drawData:{data:drawSel}};
}

function end(drawingLayer,action) {
    var {mode}= drawingLayer;
    var retObj= {};
    if (mode==='select') {
        retObj.mode= 'edit';
        retObj.helpLine= editHelpText;
    }
    return retObj;
}




const distance= (pt1,pt2) => VisUtil.computeScreenDistance(pt1.x,pt1.y,pt2.x,pt2.y);

function setupSelect(pv,imagePt) {
    return {firstPt: imagePt, currentPt: imagePt};
}

function findClosestPtIdx(ptAry, pt) {
    var dist= Number.MAX_VALUE;
    return ptAry.reduce( (idx,testPt,i) => {
        var testDist= distance(testPt,pt);
        if (testDist<dist) {
            dist= testDist;
            idx= i;
        }
        return idx;
    },0);

}


function findClosestCorner(cc,ptAry, spt, testDist) {
    var idx = findClosestPtIdx(ptAry, spt);
    var testPt = cc.getScreenCoords(ptAry[idx]);

    if (!testPt) return null;
    if (distance(testPt, spt)>testDist) return null;

    var idxBelow= idx-1>-1? idx-1 : 3;
    var idxAbove= idx+1<4? idx+1 : 0;

    var west= (ptAry[idx].x== Math.min( ptAry[idxBelow].x, ptAry[idxAbove].x));
    var north= (ptAry[idx].y== Math.min( ptAry[idxBelow].y, ptAry[idxAbove].y));

    var corner= Corner.NE;
    if      (north && west) corner= Corner.NW;
    else if (north && !west) corner= Corner.NE;
    else if (!north && west) corner= Corner.SW;
    else if (!north && !west) corner= Corner.SE;

    return corner;
}


/**
 *
 * @param {object} firstPt
 * @param {object} currentPt
 * @param {CysConverter} cc
 * @return {[]}
 */
function makeSelectObj(firstPt,currentPt,cc) {
    var fallbackAry= [firstPt,currentPt];

    var twoPtAry= cc.projection.isSpecified() ?
        [cc.getWorldCoords(firstPt),cc.getWorldCoords(currentPt)] : fallbackAry;

    if (!twoPtAry[0] || !twoPtAry[1]) twoPtAry= fallbackAry;

    return [SelectBox.makeSelectBox(twoPtAry[0], twoPtAry[1], SelectBox.Style.HANDLED)];
}


