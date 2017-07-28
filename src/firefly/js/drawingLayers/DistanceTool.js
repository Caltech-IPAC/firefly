/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import numeral from 'numeral';
import {isBoolean, get} from 'lodash';
import DrawLayerCntlr, {DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import {getPreference} from '../core/AppDataCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef,Style, TextLocation} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import CsysConverter from '../visualize/CsysConverter.js';
import { makeOffsetPt, makeWorldPt, makeImagePt} from '../visualize/Point.js';
import BrowserInfo from '../util/BrowserInfo.js';
import VisUtil from '../visualize/VisUtil.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {getUIComponent} from './DistanceToolUI.jsx';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';


const EDIT_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;


const ID= 'DISTANCE_TOOL';
const TYPE_ID= 'DISTANCE_TOOL_TYPE';


export const DIST_READOUT = 'DistanceReadout';
export const ARC_MIN = 'arcmin';
export const ARC_SEC = 'arcsec';
export const DEG = 'deg';
const HTML_DEG= String.fromCharCode(176);


const selHelpText='Click and drag to find a distance';
const editHelpText='Click and drag at either end to adjust distance';


const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,onDetach,getUIComponent);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID


var idCnt=0;


export function distanceToolEndActionCreator(rawAction) {
    return (dispatcher, getState) => {
        var {drawLayer, plotId}= rawAction.payload;
        dispatcher({type:DrawLayerCntlr.DT_END, payload:rawAction.payload} );
        drawLayer= getDrawLayerById(getState()[DRAWING_LAYER_KEY], drawLayer.drawLayerId);
        
        var sel= {pt0:drawLayer.firstPt,pt1:drawLayer.currentPt};
        dispatchAttributeChange(plotId,true,PlotAttribute.ACTIVE_DISTANCE,sel,true);
    };
}





/**
 *
 * @return {Function}
 */
function creator() {

    var drawingDef= makeDrawingDef('red');
    var pairs= {
        [MouseState.DRAG.key]: DrawLayerCntlr.DT_MOVE,
        [MouseState.DOWN.key]: DrawLayerCntlr.DT_START,
        [MouseState.UP.key]: DrawLayerCntlr.DT_END
    };


    var exclusiveDef= { exclusiveOnDown: true, type : 'anywhere' };

    var actionTypes= [DrawLayerCntlr.DT_START,
                      DrawLayerCntlr.DT_MOVE,
                      DrawLayerCntlr.DT_END];

    idCnt++;
    var options= {
        canUseMouse:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        destroyWhenAllDetached: true
    };
    return DrawLayer.makeDrawLayer( `${ID}-${idCnt}`, TYPE_ID, 'Distance Tool',
                                     options, drawingDef, actionTypes, pairs, exclusiveDef, getCursor );
}

function onDetach(drawLayer,action) {
    var {plotIdAry}= action.payload;
    plotIdAry.forEach( (plotId) => dispatchAttributeChange(plotId,false,PlotAttribute.ACTIVE_DISTANCE,null,true));
}

function getCursor(plotView, screenPt) {
    const plot= primePlot(plotView);

    var ptAry= getPtAry(plot);
    if (!ptAry) return null;
    var idx= findClosestPtIdx(ptAry,screenPt);
    if (screenDistance(ptAry[idx],screenPt)<EDIT_DISTANCE) {
        return 'pointer';
    }
    return null;
}


function getLayerChanges(drawLayer, action) {

    switch (action.type) {
        case DrawLayerCntlr.DT_START:
            return start(action);
            break;
        case DrawLayerCntlr.DT_MOVE:
            return drag(drawLayer,action);
            break;
        case DrawLayerCntlr.DT_END:
            return end(action);
            break;
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            if (!get(action.payload, 'isExistingDrawLayer', false)) {
                return attach();
            }
            break;
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            return dealWithMods(drawLayer,action);
            break;
        case DrawLayerCntlr.FORCE_DRAW_LAYER_UPDATE:
            return dealWithUnits(drawLayer,action);
            break;

    }
    return null;

}

/**
 * 
 * @param plot
 * @param firstPt
 * @param currPt
 * @param drawAry
 * @return {object}
 */
function makeBaseReturnObj(plot,firstPt,currPt,drawAry )  {

    var exclusiveDef= { exclusiveOnDown: true, type : 'vertexThenAnywhere' };

    return {drawData:{data:drawAry},
            exclusiveDef,
            vertexDef:{points:[firstPt, currPt], pointDist:EDIT_DISTANCE}
    };

}

function dealWithUnits(drawLayer,action) {
    var {plotIdAry}= action.payload;
    const plot= primePlot(visRoot(),plotIdAry[0]);
    var cc= CsysConverter.make(plot);
    if (!cc) return null;
    var drawAry= makeSelectObj(drawLayer.firstPt, drawLayer.currentPt, drawLayer.posAngle,cc);

    return makeBaseReturnObj(plot,drawLayer.firstPt, drawLayer.currentPt,drawAry);
}



function dealWithMods(drawLayer,action) {
    var {changes,plotIdAry}= action.payload;
    if (isBoolean(changes.posAngle)) {
        const plot= primePlot(visRoot(),plotIdAry[0]);
        var cc= CsysConverter.make(plot);
        if (!cc) return null;
        var drawAry= makeSelectObj(drawLayer.firstPt, drawLayer.currentPt, changes.posAngle,cc);
        return Object.assign({posAngle:changes.posAngle},
                              makeBaseReturnObj(plot,drawLayer.firstPt, drawLayer.currentPt,drawAry));
    }
    return null;
}


function attach() {
    return {
        helpLine: selHelpText,
        drawData:{data:null},
        posAngle: false,
        firstPt: null,
        currentPt: null,
        vertexDef: {points:null, pointDist:EDIT_DISTANCE},
        exclusiveDef: { exclusiveOnDown: true, type : 'anywhere' }
    };
    
}

function getMode(plot) {
    if (!plot) return 'select';
    var selection = plot.attributes[PlotAttribute.ACTIVE_DISTANCE];
    return (selection) ? 'edit' : 'select';
}

function start(action) {
    var {imagePt,plotId,shiftDown}= action.payload;
    var plot= primePlot(visRoot(),plotId);
    var mode= getMode(plot);
    if (!plot) return;
    var retObj= {};
    if (mode==='select' || shiftDown) {
        retObj= setupSelect(imagePt);
    }
    else if (mode==='edit') {
        var ptAry= getPtAry(plot);
        if (!ptAry) return retObj;

        var cc= CsysConverter.make(plot);
        var spt= cc.getScreenCoords(imagePt);
        var idx= findClosestPtIdx(ptAry,spt);
        
        var testPt= cc.getScreenCoords(ptAry[idx]);
        if (!testPt) return {};

        if (screenDistance(testPt,spt)<EDIT_DISTANCE) {
            var oppoIdx= idx===0 ? 1 : 0;
            retObj.firstPt= cc.getImageWorkSpaceCoords(ptAry[oppoIdx]);
            retObj.currentPt= cc.getImageWorkSpaceCoords(ptAry[idx]);
            if (!retObj.firstPt || !retObj.currentPt) return {};
        }
        else {
            retObj= setupSelect(imagePt) ;
        }
    }
    return retObj;

}


function drag(drawLayer,action) {
    var {imagePt,plotId}= action.payload;
    const plot= primePlot(visRoot(),plotId);
    var cc= CsysConverter.make(plot);
    if (!cc) return;
    var drawAry= makeSelectObj(drawLayer.firstPt, imagePt, drawLayer.posAngle,cc); //todo switch back
    return Object.assign({currentPt:imagePt}, makeBaseReturnObj(plot,drawLayer.firstPt, imagePt,drawAry));
}

function end(action) {
    var {plotId}= action.payload;
    var mode= getMode(primePlot(visRoot(),plotId));
    var retObj= {};
    if (mode==='select') {
        retObj.helpLine= editHelpText;
    }
    return retObj;
}



function setupSelect(imagePt) {
    return {firstPt: imagePt, currentPt: imagePt};
}

function findClosestPtIdx(ptAry, pt) {
    var dist= Number.MAX_VALUE;
    return ptAry.reduce( (idx,testPt,i) => {
        var testDist= screenDistance(testPt,pt);
        if (testDist<dist) {
            dist= testDist;
            idx= i;
        }
        return idx;
    },0);

}



const screenDistance= (pt1,pt2) => VisUtil.computeScreenDistance(pt1.x,pt1.y,pt2.x,pt2.y);


/**
 *
 * @param dist
 * @param isWorld
 * @param pref
 * @return {*}
 */
function getDistText(dist, isWorld, pref) {
    if (isWorld)  {
        if(pref===ARC_MIN){
            return ` ${numeral(dist*60.0).format('0.000')}'`;
        }
        else if(pref===ARC_SEC){
            return ` ${numeral(dist*3600).format('0.000')}"`;
        } else {
            return ` ${numeral(dist).format('0.000')}${HTML_DEG}`;
        }
    }
    else {
        return ` ${Math.floor(dist)} Pixels`;
    }
}


/**
 *
 * @param {object} firstPt
 * @param {object} currentPt
 * @param {boolean} posAngle
 * @param {CysConverter} cc
 * @return {Array}
 */
function makeSelectObj(firstPt,currentPt, posAngle,cc) {
    var pref= getPreference(DIST_READOUT);
    var retval;
    var ptAry= [firstPt,currentPt];

    var anyPt1;
    var anyPt2;
    var dist;
    var world= cc.projection.isSpecified();

    if (world) {
        anyPt1 = cc.getWorldCoords(ptAry[0]);
        anyPt2 = cc.getWorldCoords(ptAry[1]);
        dist= VisUtil.computeDistance(anyPt1,anyPt2);
    }
    else {
        anyPt1 = ptAry[0];
        anyPt2 = ptAry[1];
        dist= screenDistance(anyPt1,anyPt2);
    }

    if (!anyPt1 || !anyPt2) return null;

    var obj= ShapeDataObj.makeLine(anyPt1,anyPt2);
    obj.style= Style.HANDLED;
    obj.text= getDistText(dist,world,pref);
    obj.textLoc=TextLocation.LINE_MID_POINT;
    obj.textOffset= makeOffsetPt(-15, 0);

    if (posAngle) {
        var eastPt;
        var westPt;

        if (anyPt1.x>anyPt2.x) {
            eastPt= anyPt1;
            westPt= anyPt2;
        }
        else {
            eastPt= anyPt2;
            westPt= anyPt1;
        }

        var adjDist;
        var opDist;
        var lonDelta1;
        var lonDelta2;
        var latDelta1;
        var latDelta2;
        if (world) {
            lonDelta1= makeWorldPt(eastPt.x, eastPt.y);
            lonDelta2= makeWorldPt(westPt.x, eastPt.y);
            adjDist= VisUtil.computeDistance(lonDelta1,lonDelta2);

            latDelta1= makeWorldPt(westPt.x, eastPt.y);
            latDelta2= makeWorldPt(westPt.x, westPt.y);
            opDist= VisUtil.computeDistance(latDelta1,latDelta2);
        }
        else {
            lonDelta1= makeImagePt(eastPt.x, eastPt.y);
            lonDelta2= makeImagePt(westPt.x, eastPt.y);
            adjDist= screenDistance(lonDelta1,lonDelta2);

            latDelta1= makeImagePt(westPt.x, eastPt.y);
            latDelta2= makeImagePt(westPt.x, westPt.y);
            opDist= screenDistance(latDelta1,latDelta2);
        }

        var adj= ShapeDataObj.makeLine(lonDelta1,lonDelta2);
        var op= ShapeDataObj.makeLine(latDelta1, latDelta2);
        var lonDelta1TextPt= lonDelta1;

        adj.textLoc=TextLocation.LINE_MID_POINT;
        adj.text= getDistText(adjDist,world,pref);
        op.textLoc=TextLocation.LINE_MID_POINT;
        op.text= getDistText(opDist,world,pref);
        op.textOffset= makeOffsetPt(0,15);

        var sinX= opDist/dist;
        var angle= VisUtil.toDegrees(Math.asin(sinX));

        var aStr=  `${numeral(angle).format('0.000')}${HTML_DEG}`;
        var angleShape= ShapeDataObj.makeTextWithOffset(makeOffsetPt(8,-8), lonDelta1TextPt, aStr);

        retval= [obj,adj,op,angleShape];
    }
    else {
        retval= [obj];
    }
    return retval;
}


function getPtAry(plot) {
    var sel= plot.attributes[PlotAttribute.ACTIVE_DISTANCE];
    if (!sel) return null;
    var cc= CsysConverter.make(plot);
    var ptAry=[];
    ptAry[0]= cc.getScreenCoords(sel.pt0);
    ptAry[1]= cc.getScreenCoords(sel.pt1);
    if (!ptAry[0] || !ptAry[1]) return null;
    return ptAry;
}




//////////////////////////////////////////////////
//////////////////////////////////////////////////
//////////////////////////////////////////////////











