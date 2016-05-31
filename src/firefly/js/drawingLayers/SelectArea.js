/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty} from 'lodash';
import DrawLayerCntlr, {DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {makeScreenPt} from '../visualize/Point.js';
import BrowserInfo from '../util/BrowserInfo.js';
import VisUtil from '../visualize/VisUtil.js';
import SelectBox from '../visualize/draw/SelectBox.js';
import {getPlotViewById, primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {Style} from '../visualize/draw/DrawingDef.js';
//import DrawLayerFactory from '../visualize/draw/DrawLayerFactory.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
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


const ID= 'SELECT_AREA';
const TYPE_ID= 'SELECT_AREA_TYPE';

const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,onDetach,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID


var idCnt=0;


export function selectAreaEndActionCreator(rawAction) {
    return (dispatcher, getState) => {
        var {drawLayer, plotId}= rawAction.payload;
        dispatcher({type:DrawLayerCntlr.SELECT_AREA_END, payload:rawAction.payload} );

        drawLayer= getDrawLayerById(getState()[DRAWING_LAYER_KEY], drawLayer.drawLayerId);

        if (drawLayer.drawData.data) {
            var selectBox= drawLayer.drawData.data[0];
            var sel= {pt0:selectBox.pt1,pt1:selectBox.pt2};
            dispatchAttributeChange(plotId,true,PlotAttribute.SELECTION,sel,true);
        }
    };
}

/**
 *
 * @return {Function}
 */
function creator() {

    var drawingDef= makeDrawingDef('black');
    var pairs= {
        [MouseState.MOVE.key]: DrawLayerCntlr.SELECT_MOUSE_LOC,
        [MouseState.DRAG.key]: DrawLayerCntlr.SELECT_AREA_MOVE,
        [MouseState.DOWN.key]: DrawLayerCntlr.SELECT_AREA_START,
        [MouseState.UP.key]: DrawLayerCntlr.SELECT_AREA_END,
    };

    var actionTypes= [DrawLayerCntlr.SELECT_AREA_START,
                      DrawLayerCntlr.SELECT_AREA_MOVE,
                      DrawLayerCntlr.SELECT_AREA_END,
                      DrawLayerCntlr.SELECT_MOUSE_LOC];
    
    var exclusiveDef= { exclusiveOnDown: true, type : 'anywhere' };



    idCnt++;
    var options= {
        canUseMouse:true,
        canUserChangeColor: ColorChangeType.DISABLE,
        canUserDelete: false,
        destroyWhenAllDetached: true
    };
    return DrawLayer.makeDrawLayer( `${ID}-${idCnt}`, TYPE_ID, 'Selection Tool',
                                     options, drawingDef, actionTypes, pairs, exclusiveDef, getCursor);
}

function onDetach(drawLayer,action) {
    var {plotIdAry}= action.payload;
    plotIdAry.forEach( (plotId) => dispatchAttributeChange(plotId,false,PlotAttribute.SELECTION,null,true));
}

function getCursor(plotView, screenPt) {
    const plot= primePlot(plotView);
    var cc= CsysConverter.make(plot);
    var ptAry= getPtAryFromPlot(plot);
    if (!ptAry) return null;
    var corner= findClosestCorner(cc,ptAry, screenPt, EDIT_DISTANCE);
    if (!corner) return null;
    switch (corner) {
        case Corner.NE:
            return 'ne-resize';
            break;
        case Corner.NW:
            return 'nw-resize';
            break;
        case Corner.SE:
            return 'se-resize';
            break;
        case Corner.SW:
            return 'sw-resize';
            break;
    }
    return null;
}

function getLayerChanges(drawLayer, action) {

    switch (action.type) {
        case DrawLayerCntlr.SELECT_AREA_START:
            return start(drawLayer,action);
            break;
        case DrawLayerCntlr.SELECT_AREA_MOVE:
            return drag(drawLayer,action);
            break;
        case DrawLayerCntlr.SELECT_AREA_END:
            return end(drawLayer,action);
            break;
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            return attach();
            break;
        case DrawLayerCntlr.SELECT_MOUSE_LOC:
            return moveMouse(drawLayer,action);
            break;
    }

}



function attach() {
    return {
        helpLine: selHelpText,
        drawData:{data:null},
        vertexDef: {points:null, pointDist:EDIT_DISTANCE},
        exclusiveDef: { exclusiveOnDown: true, type: 'anywhere' }
    };
}

function moveMouse(drawLayer,action) {
    var {screenPt,plotId}= action.payload;
    var plot= primePlot(visRoot(),plotId);
    var mode= getMode(plot);
    if (plot && mode==='edit') {
        var cc= CsysConverter.make(plot);
        var ptAry= getPtAryFromPlot(plot);
        if (!ptAry) return null;
        var corner= findClosestCorner(cc,ptAry, screenPt, EDIT_DISTANCE);
        var cursor;
        if (corner) {
            switch (corner) {
                case Corner.NE: cursor= 'ne-resize'; break;
                case Corner.NW: cursor= 'nw-resize'; break;
                case Corner.SE: cursor= 'se-resize'; break;
                case Corner.SW: cursor= 'sw-resize'; break;
            }
        } else {
            cursor= '';
        }
        return (drawLayer.cursor!==cursor) ? {cursor} : null;
    }
}


function start(drawLayer,action) {
    var {screenPt,imagePt,plotId,shiftDown}= action.payload;
    var plot= primePlot(visRoot(),plotId);
    var mode= getMode(plot);
    if (!plot) return;

    var retObj= {};




    if (mode==='select' || shiftDown) {
        retObj= setupSelect(imagePt);
    }
    else if (mode==='edit') {
        var ptAry= getPtAryFromPlot(plot);
        if (!ptAry) return retObj;

        var idx= findClosestPtIdx(ptAry,screenPt);
        if (idx<0) return {};
        var cc= CsysConverter.make(plot);
        var testPt= cc.getScreenCoords(ptAry[idx]);
        if (!testPt) return {};

        if (distance(testPt,screenPt)<EDIT_DISTANCE) {
            var oppoIdx= (idx+2) % 4;
            retObj.firstPt= cc.getImageWorkSpaceCoords(ptAry[oppoIdx]);
            retObj.currentPt= cc.getImageWorkSpaceCoords(ptAry[idx]);
            if (retObj.firstPt==null || retObj.currentPt==null) return {};
        }
        else {
            retObj= setupSelect(imagePt);
        }
    }
    return retObj;

}

function getPtAryFromPlot(plot) {
    var sel= plot.attributes[PlotAttribute.SELECTION];
    if (!sel) return null;
    return getPtAry(plot,sel.pt0,sel.pt1);
}

function getPtAry(plot,pt0,pt1) {
    var ptAry=[];
    var cc= CsysConverter.make(plot);
    ptAry[0]= cc.getScreenCoords(pt0);
    ptAry[2]= cc.getScreenCoords(pt1);
    if (!ptAry[0] || !ptAry[2]) return null;
    ptAry[1] = makeScreenPt(ptAry[2].x, ptAry[0].y);
    ptAry[3] = makeScreenPt(ptAry[0].x, ptAry[2].y);
    return ptAry;

}


function getPtAryForCorners(plot,pt0,pt1) {
    const screenPtAry= getPtAry(plot,pt0,pt1);
    if (isEmpty(screenPtAry)) return null;
    var cc= CsysConverter.make(plot);
    const useWld=  cc.projection.isSpecified();
    return screenPtAry.map( (sp) => useWld ? cc.getWorldCoords(sp) : cc.getImageCoords(sp));
}



function drag(drawLayer,action) {
    var {imagePt,plotId}= action.payload;
    var plot= primePlot(visRoot(),plotId);
    if (!plot) return;
    var drawSel= makeSelectObj(drawLayer.firstPt, imagePt, CsysConverter.make(plot));
    var exclusiveDef= { exclusiveOnDown: true, type : 'vertexOnly' };
    return {currentPt:imagePt,
            drawData:{data:drawSel},
            exclusiveDef,
            vertexDef:{points:getPtAryForCorners(plot,drawLayer.firstPt,imagePt), pointDist:EDIT_DISTANCE}
     };
}

function end(drawLayer,action) {
    var mode= getMode(primePlot(visRoot(),action.payload.plotId));
    return  (mode==='select') ? {helpLine: editHelpText} : {};
}

function getMode(plot) {
    if (!plot) return 'select';
    var selection = plot.attributes[PlotAttribute.SELECTION];
    return (selection) ? 'edit' : 'select';
}

const distance= (pt1,pt2) => VisUtil.computeScreenDistance(pt1.x,pt1.y,pt2.x,pt2.y);

function setupSelect(imagePt) {
    return {firstPt: imagePt, currentPt: imagePt};
}

function findClosestPtIdx(ptAry, pt) {
    var dist= Number.MAX_VALUE;
    return ptAry.reduce( (idx,testPt,i) => {
        if (!testPt || !pt) return idx;
        var testDist= distance(testPt,pt);
        if (testDist<dist) {
            dist= testDist;
            idx= i;
        }
        return idx;
    },-1);

}


function findClosestCorner(cc,ptAry, spt, testDist) {
    var idx = findClosestPtIdx(ptAry, spt);
    if (idx<0) return null;
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
 * @return {Array}
 */
function makeSelectObj(firstPt,currentPt,cc) {
    var fallbackAry= [firstPt,currentPt];

    var twoPtAry= cc.projection.isSpecified() ?
        [cc.getWorldCoords(firstPt),cc.getWorldCoords(currentPt)] : fallbackAry;

    if (!twoPtAry[0] || !twoPtAry[1]) twoPtAry= fallbackAry;

    return [SelectBox.makeSelectBox(twoPtAry[0], twoPtAry[1], Style.HANDLED)];
}


