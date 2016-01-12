/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import numeral from 'numeral';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import AppDataCntlr from '../core/AppDataCntlr.js';
import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef,Style, TextLocation} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseCntlr.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {makeScreenPt, makeOffsetPt, makeWorldPt, makeImagePt} from '../visualize/Point.js';
import BrowserInfo from '../util/BrowserInfo.js';
import VisUtil from '../visualize/VisUtil.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {getPlotViewById} from '../visualize/PlotViewUtil.js';
import {getUIComponent} from './DistanceToolUI.jsx';
//import DrawLayerFactory from '../visualize/draw/DrawLayerFactory.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {flux} from '../Firefly.js';

import Enum from 'enum';




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


const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,getUIComponent);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID


var idCnt=0;

function dispatchDistanceToolEnd(mouseStatePayload) {
    var {plotId,drawLayer}= mouseStatePayload;
    var sel= {pt0:drawLayer.firstPt,pt1:drawLayer.currentPt};
    ImagePlotCntlr.dispatchAttributeChange(plotId,true,PlotAttribute.ACTIVE_DISTANCE,sel);
    flux.process({type:DrawLayerCntlr.DT_END, payload:mouseStatePayload} );
}


/**
 *
 * @return {Function}
 */
function creator() {

    var drawingDef= makeDrawingDef('red');
    var pairs= {
        [MouseState.DRAG.key]: {exclusive: true, actionType:DrawLayerCntlr.DT_MOVE},
        [MouseState.DOWN.key]: DrawLayerCntlr.DT_START,
        [MouseState.UP.key]: dispatchDistanceToolEnd
    };

    var actionTypes= [DrawLayerCntlr.DT_START,
                      DrawLayerCntlr.DT_MOVE,
                      DrawLayerCntlr.DT_END];

    idCnt++;
    var options= {
        canUseMouse:true,
        canUserChangeColor: ColorChangeType.DYNAMIC
    };
    return DrawLayer.makeDrawLayer( `${ID}-${idCnt}`, TYPE_ID, 'Distance Tool',
                                     options, drawingDef, actionTypes, pairs );
}


function getLayerChanges(drawLayer, action) {

    switch (action.type) {
        case DrawLayerCntlr.DT_START:
            return start(drawLayer,action);
            break;
        case DrawLayerCntlr.DT_MOVE:
            return drag(drawLayer,action);
            break;
        case DrawLayerCntlr.DT_END:
            return end(drawLayer,action);
            break;
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            return attach();
            break;
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            return dealWithMods(drawLayer,action)
            break;
        case DrawLayerCntlr.FORCE_DRAW_LAYER_UPDATE:
            return dealWithUnits(drawLayer,action)
            break;

    }
    return null;

}


function dealWithUnits(drawLayer,action) {
    var {plotIdAry}= action.payload;
    var pv= getPlotViewById(visRoot(),plotIdAry[0]);
    if (!pv) return null;
    var cc= CsysConverter.make(pv.primaryPlot);
    var drawSel= makeSelectObj(drawLayer.firstPt, drawLayer.currentPt, drawLayer.posAngle,cc);
    return {drawData:{data:drawSel}};
}



function dealWithMods(drawLayer,action) {
    var {changes,plotIdAry}= action.payload;
    if (typeof changes.posAngle=== 'boolean') {
        var pv= getPlotViewById(visRoot(),plotIdAry[0]);
        if (!pv) return null;
        var cc= CsysConverter.make(pv.primaryPlot);
        var drawSel= makeSelectObj(drawLayer.firstPt, drawLayer.currentPt, changes.posAngle,cc);
        return {posAngle:changes.posAngle, drawData:{data:drawSel}};
    }
    return null;
}


function attach() {
    return {
        mode: 'select',
        helpLine: selHelpText,
        drawData:{data:null},
        posAngle: false,
        firstPt: null,
        currentPt: null
    };
}


function start(drawLayer,action) {
    var {screenPt,imagePt,plotId,shiftDown}= action.payload;
    var {mode}= drawLayer;
    var pv= getPlotViewById(visRoot(),plotId);
    if (!pv) return;
    var plot= pv.primaryPlot;
    var retObj= {};
    if (mode==='select' || shiftDown) {
        retObj= setupSelect(imagePt);
    }
    else if (mode==='edit') {
        var ptAry= getPtAry(pv);
        if (!ptAry) return retObj;

        var idx= findClosestPtIdx(ptAry,screenPt);
        var cc= CsysConverter.make(plot);
        var testPt= cc.getScreenCoords(ptAry[idx]);
        if (!testPt) return {};

        if (screenDistance(testPt,screenPt)<EDIT_DISTANCE) {
            var oppoIdx= idx===0 ? 1 : 0;
            retObj.firstPt= cc.getImageWorkSpaceCoords(ptAry[oppoIdx]);
            retObj.currentPt= cc.getImageWorkSpaceCoords(ptAry[idx]);
            if (retObj.firstPt==null || retObj.currentPt==null) return {};
        }
        else {
            retObj= setupSelect(imagePt) ;
        }
    }
    return retObj;

}


function drag(drawLayer,action) {
    var {imagePt,plotId}= action.payload;
    var pv= getPlotViewById(visRoot(),plotId);
    if (!pv) return;
    var cc= CsysConverter.make(pv.primaryPlot);
    var drawSel= makeSelectObj(drawLayer.firstPt, imagePt, drawLayer.posAngle,cc); //todo switch back
    //var drawSel= makeSelectObj(drawLayer.firstPt, imagePt, true,cc); //todo switch back
    return {currentPt:imagePt, drawData:{data:drawSel}};
}

function end(drawLayer,action) {
    var {mode}= drawLayer;
    var retObj= {};
    if (mode==='select') {
        retObj.mode= 'edit';
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
 * @return {[]}
 */
function makeSelectObj(firstPt,currentPt, posAngle,cc) {
    var pref= AppDataCntlr.getPreference(DIST_READOUT);
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


function getPtAry(pv) {
    var sel= pv.primaryPlot.attributes[PlotAttribute.ACTIVE_DISTANCE];
    if (!sel) return null;
    var cc= CsysConverter.make(pv.primaryPlot);
    var ptAry=[];
    ptAry[0]= cc.getScreenCoords(sel.pt0);
    ptAry[1]= cc.getScreenCoords(sel.pt1);
    if (!ptAry[0] || !ptAry[1]) return null;
    return ptAry;
}




//////////////////////////////////////////////////
//////////////////////////////////////////////////
//////////////////////////////////////////////////











