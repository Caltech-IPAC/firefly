/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import DrawLayerCntlr, {DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef,Style, TextLocation} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import CsysConverter from '../visualize/CsysConverter.js';
import BrowserInfo from '../util/BrowserInfo.js';
import VisUtil from '../visualize/VisUtil.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import PointDataObj from '../visualize/draw/PointDataObj.js';
import {primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {hasWCSProjection} from '../visualize/PlotViewUtil';
import {DrawingType} from '../visualize/draw/DrawObj';
import {DrawSymbol} from 'firefly/visualize/draw/DrawSymbol.js';


const EDIT_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;


const ID= 'EXTRACT_LINE_TOOL';
const TYPE_ID= 'EXTRACT_LINE_TOOL_TYPE';

const selHelpText='Click and drag to extract a line';
const editHelpText='Click and drag at either end to adjust extraction';

const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,onDetach);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

let idCnt=0;

export function extractLineToolStartActionCreator(rawAction) {
    return (dispatcher, getState) => {
        const {plotId} = rawAction.payload;
        dispatcher({type: DrawLayerCntlr.ELT_START, payload: rawAction.payload});
        dispatchAttributeChange({
            plotId, overlayColorScope: true,
            changes: {[PlotAttribute.SELECT_ACTIVE_CHART_PT]: undefined}
        });
    };
}

export function extractLineToolEndActionCreator(rawAction) {
    return (dispatcher, getState) => {
        let {drawLayer}= rawAction.payload;
        const {plotId}= rawAction.payload;
        dispatcher({type:DrawLayerCntlr.DT_END, payload:rawAction.payload} );
        drawLayer= getDrawLayerById(getState()[DRAWING_LAYER_KEY], drawLayer.drawLayerId);
        const srcPlot= primePlot(visRoot(),plotId);
        const cc= CsysConverter.make(srcPlot);
        const srcWorld = hasWCSProjection(cc);

        const sel= {pt0:drawLayer.firstPt,pt1:drawLayer.currentPt};

        drawLayer.plotIdAry.forEach( (pId) => {
            if (pId===plotId) {
                dispatchAttributeChange({plotId:pId, toAllPlotsInPlotView:true, overlayColorScope:false,
                    changes: {
                        [PlotAttribute.ACTIVE_DISTANCE]: sel,
                        [PlotAttribute.EXTRACTION_DATA]: true,
                    }
                });
            }
            else {
                const p= primePlot(visRoot(),pId);
                if (srcWorld && hasWCSProjection(p)) {
                    const targetCC= CsysConverter.make(p);
                    const pt0= targetCC.getImageCoords(cc.getWorldCoords(drawLayer.firstPt));
                    const pt1= targetCC.getImageCoords(cc.getWorldCoords(drawLayer.currentPt));
                    dispatchAttributeChange({plotId:pId, toAllPlotsInPlotView:false, overlayColorScope:false,
                        changes:{
                            [PlotAttribute.ACTIVE_DISTANCE]: {pt0,pt1},
                            [PlotAttribute.EXTRACTION_DATA]: true,
                        },
                    });
                }
            }

        } );
    };
}

/**
 *
 * @return {Function}
 */
function creator() {
    const drawingDef= makeDrawingDef('red');
    const pairs= {
        [MouseState.DRAG.key]: DrawLayerCntlr.ELT_MOVE,
        [MouseState.DOWN.key]: DrawLayerCntlr.ELT_START,
        [MouseState.UP.key]: DrawLayerCntlr.ELT_END
    };


    const exclusiveDef= { exclusiveOnDown: true, type : 'anywhere' };

    const actionTypes= [DrawLayerCntlr.ELT_START, DrawLayerCntlr.ELT_MOVE, DrawLayerCntlr.ELT_END];

    idCnt++;
    const options= {
        canUseMouse:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        destroyWhenAllDetached: true
    };
    return DrawLayer.makeDrawLayer( `${ID}-${idCnt}`, TYPE_ID, 'Extract Line Tool',
                                     options, drawingDef, actionTypes, pairs, exclusiveDef, getCursor );
}

function onDetach(drawLayer,action) {
    const {plotIdAry}= action.payload;
    plotIdAry.forEach( (plotId) => {
        const plot= primePlot(visRoot(),plotId);
        if (plot && plot.attributes[PlotAttribute.ACTIVE_DISTANCE]) {
            dispatchAttributeChange({
                plotId,overlayColorScope:false,
                changes: {
                    [PlotAttribute.ACTIVE_DISTANCE]: null,
                    [PlotAttribute.EXTRACTION_DATA]: false,
                }
            });
        }
    });
}

function getCursor(plotView, screenPt) {
    const ptAry= getPtAry(primePlot(plotView));
    if (!ptAry) return;
    const idx= findClosestPtIdx(ptAry,screenPt);
    if (screenDistance(ptAry[idx],screenPt)<EDIT_DISTANCE) {
        return 'pointer';
    }
}

function getLayerChanges(drawLayer, action) {
    switch (action.type) {
        case DrawLayerCntlr.ELT_START:
            return start(drawLayer, action);
        case DrawLayerCntlr.ELT_MOVE:
            return drag(drawLayer,action);
        case DrawLayerCntlr.ELT_END:
            return end(action);
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            return attach();
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            return dealWithMods(drawLayer,action);
    }
}

/**
 * @param {Point} firstPt
 * @param {Point} currPt
 * @param {Point} drawAry
 * @return {object}
 */
function makeBaseReturnObj(firstPt,currPt,drawAry )  {

    const exclusiveDef= { exclusiveOnDown: true, type : 'vertexThenAnywhere' };

    return {drawData:{data:drawAry},
            exclusiveDef,
            vertexDef:{points:[firstPt, currPt], pointDist:EDIT_DISTANCE}
    };

}

function dealWithMods(drawLayer,action) {
    const {changes,plotIdAry}= action.payload;
    if (Object.keys(changes).includes('activePt')) {
        let plot= primePlot(visRoot());
        if (!plotIdAry.includes(plot.plotId)) plot= primePlot(visRoot(),plotIdAry[0]);
        const cc= CsysConverter.make(plot);
        if (!cc) return null;
        const {activePt}= changes;
        const {firstPt,currentPt}= drawLayer;
        const drawAry= makeSelectObj(drawLayer.firstPt, drawLayer.currentPt, activePt, drawLayer.offsetCal, cc);
        return {activePt, ...makeBaseReturnObj(firstPt, currentPt,drawAry)};
    }
    return null;
}

function attach() {
    return {
        helpLine: selHelpText,
        drawData:{data:null},
        activePt: undefined,
        offsetCal: false,
        firstPt: null,
        currentPt: null,
        moveHead: true,      // drag start from head or not
        vertexDef: {points:null, pointDist:EDIT_DISTANCE},
        exclusiveDef: { exclusiveOnDown: true, type : 'anywhere' }
    };
    
}

function getMode(plot) {
    if (!plot) return 'select';
    const selection = plot.attributes[PlotAttribute.ACTIVE_DISTANCE];
    return (selection) ? 'edit' : 'select';
}

function start(drawLayer, action) {
    const {imagePt,plotId,shiftDown}= action.payload;
    const plot= primePlot(visRoot(),plotId);
    const mode= getMode(plot);
    if (!plot) return;
    let retObj= {};
    if (mode==='select' || shiftDown) {
        retObj= setupSelect(imagePt);
    }
    else if (mode==='edit') {
        const ptAry= getPtAry(plot);
        if (!ptAry) return retObj;

        const cc= CsysConverter.make(plot);
        const spt= cc.getScreenCoords(imagePt);
        const idx= findClosestPtIdx(ptAry,spt);
        const testPt= cc.getScreenCoords(ptAry[idx]);
        if (!testPt) return {};
        retObj.activePt= undefined;

        if (screenDistance(testPt,spt)<EDIT_DISTANCE) {   // swap the first and current point, redraw the distance tool
            retObj.moveHead = (idx === 1);
            retObj.firstPt= cc.getImageWorkSpaceCoords(ptAry[0]);
            retObj.currentPt= cc.getImageWorkSpaceCoords(ptAry[1]);
            retObj.activePt= undefined;
            if (!retObj.firstPt || !retObj.currentPt) return {};

            const drawAry = makeSelectObj(retObj.firstPt, retObj.currentPt, undefined, drawLayer.offsetCal, CsysConverter.make(plot));
            return Object.assign(retObj, makeBaseReturnObj(retObj.firstPt, retObj.currentPt, drawAry));
        }
        else {
            retObj= setupSelect(imagePt) ;
        }
    }
    return retObj;

}


function drag(drawLayer,action) {
    const {imagePt,plotId}= action.payload;
    const plot= primePlot(visRoot(),plotId);
    const cc= CsysConverter.make(plot);
    if (!cc) return;

    const newFirst = drawLayer.moveHead ? drawLayer.firstPt : imagePt;
    const newCurrent = drawLayer.moveHead ? imagePt : drawLayer.currentPt;

    const drawAry= makeSelectObj(newFirst, newCurrent, undefined, drawLayer.offsetCal, cc);
    const line= Object.assign({firstPt: newFirst, currentPt:newCurrent, activePt:undefined}, makeBaseReturnObj(newFirst, newCurrent,drawAry));
    return line;
}

function end(action) {
    const {plotId}= action.payload;
    const mode= getMode(primePlot(visRoot(),plotId));
    const retObj= {};
    if (mode==='select') {
        retObj.helpLine= editHelpText;
    }
    return retObj;
}


function setupSelect(imagePt) {
    return {firstPt: imagePt, currentPt: imagePt,  activePt:undefined, moveHead: true};
}

function findClosestPtIdx(ptAry, pt) {
    let dist= Number.MAX_VALUE;
    return ptAry.reduce( (idx,testPt,i) => {
        const testDist= screenDistance(testPt,pt);
        if (testDist<dist) {
            dist= testDist;
            idx= i;
        }
        return idx;
    },0);

}

const screenDistance= (pt1,pt2) => VisUtil.computeScreenDistance(pt1.x,pt1.y,pt2.x,pt2.y);


/**
 * @param {Point} firstPt
 * @param {Point} currentPt
 * @param {Point} activePt
 * @param {boolean} offsetCal
 * @param {CysConverter} cc
 * @return {Array}
 */
function makeSelectObj(firstPt,currentPt, activePt, offsetCal, cc) {
    const ptAry= [firstPt,currentPt];
    let anyPt1;    // for ends of all ruler vectors
    let anyPt2;
    const LWIDTH = 3;

    if (!hasWCSProjection(cc)) {
        anyPt1 = cc.getImageCoords(ptAry[0]);
        anyPt2 = cc.getImageCoords(ptAry[1]);
    } else {
        anyPt1 = cc.getWorldCoords(ptAry[0]);
        anyPt2 = cc.getWorldCoords(ptAry[1]);
    }
    if (!anyPt1 || !anyPt2) return;

    const obj= ShapeDataObj.makeLine(anyPt1, anyPt2, true);   // make line with arrow at the current end
    obj.style= Style.STARTHANDLED;
    obj.lineWidth = LWIDTH;
    obj.fontSize = '16px';
    obj.fontWeight = 'bold';
    obj.textLoc=TextLocation.LINE_TOP_STACK;
    obj.texttBaseLine = 'middle';
    obj.drawEvenIfWrapping= true;
    obj.supportedDrawingTypes=  (hasWCSProjection(cc)) ?  DrawingType.WcsCoordsOnly : DrawingType.ImageCoordsOnly;
    const retval= [obj];
    if (activePt) {
        retval.push(
            PointDataObj.make(activePt,7, DrawSymbol.EMP_SQUARE_X)
        );
    }
    return retval;
}


function getPtAry(plot) {
    const sel= plot.attributes[PlotAttribute.ACTIVE_DISTANCE];
    if (!sel) return null;
    const cc = CsysConverter.make(plot);

    const ptAry=[];
    ptAry[0]= cc.getScreenCoords(sel.pt0);
    ptAry[1]= cc.getScreenCoords(sel.pt1);
    if (!ptAry[0] || !ptAry[1]) return null;
    return ptAry;
}
