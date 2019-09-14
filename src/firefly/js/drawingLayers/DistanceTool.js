/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import numeral from 'numeral';
import {isBoolean, isEmpty, get, isUndefined} from 'lodash';
import DrawLayerCntlr, {DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import {getPreference} from '../core/AppDataCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef,Style, TextLocation} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import CsysConverter from '../visualize/CsysConverter.js';
import { makeOffsetPt, makeWorldPt, makeScreenPt} from '../visualize/Point.js';
import BrowserInfo from '../util/BrowserInfo.js';
import VisUtil from '../visualize/VisUtil.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {getUIComponent} from './DistanceToolUI.jsx';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {hasWCSProjection} from '../visualize/PlotViewUtil';
import {isHiPS} from '../visualize/WebPlot.js';


const EDIT_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;


const ID= 'DISTANCE_TOOL';
const TYPE_ID= 'DISTANCE_TOOL_TYPE';

export const UNIT_PIXEL_ONLY = 0;
export const UNIT_NO_PIXEL = 1;
export const UNIT_ALL = 2;

export const DIST_READOUT = 'DistanceReadout';
export const ARC_MIN = 'arcmin';
export const ARC_SEC = 'arcsec';
export const DEG = 'deg';
export const PIXEL = 'pixel';

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
        dispatchAttributeChange({plotId,attKey:PlotAttribute.ACTIVE_DISTANCE,attValue:sel});
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
    plotIdAry.forEach( (plotId) => {
        const plot= primePlot(visRoot(),plotId);
        if (plot && plot.attributes[PlotAttribute.ACTIVE_DISTANCE]) {
            dispatchAttributeChange({
                plotId,overlayColorScope:false,
                attKey:PlotAttribute.ACTIVE_DISTANCE,attValue:null
            });
        }
    });
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
        case DrawLayerCntlr.DT_MOVE:
            return drag(drawLayer,action);
        case DrawLayerCntlr.DT_END:
            return end(action);
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            if (isEmpty(get(drawLayer, ['drawData', 'data']))) {
                return attach();
            }
            break;
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            return dealWithMods(drawLayer,action);
        case DrawLayerCntlr.FORCE_DRAW_LAYER_UPDATE:
            return dealWithUnits(drawLayer,action);

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
    var drawAry= makeSelectObj(drawLayer.firstPt, drawLayer.currentPt, drawLayer.offsetCal,cc);

    return makeBaseReturnObj(plot,drawLayer.firstPt, drawLayer.currentPt,drawAry);
}

function dealWithMods(drawLayer,action) {
    var {changes,plotIdAry}= action.payload;
    if (isBoolean(changes.offsetCal)) {
        const plot= primePlot(visRoot(),plotIdAry[0]);
        var cc= CsysConverter.make(plot);
        if (!cc) return null;
        var drawAry= makeSelectObj(drawLayer.firstPt, drawLayer.currentPt, changes.offsetCal, cc);
        return Object.assign({offsetCal:changes.offsetCal},
                              makeBaseReturnObj(plot,drawLayer.firstPt, drawLayer.currentPt,drawAry));
    }
    return null;
}


export function getUnitStyle(cc, world) {
    if (isUndefined(world)) {
        world = hasWCSProjection(cc);
    }

    if (!world) {
        return UNIT_PIXEL_ONLY;
    }
    const plot= primePlot(visRoot(),cc.plotId);
    const aHiPS = isHiPS(plot);
    if (aHiPS) {
        return UNIT_NO_PIXEL;
    }
    return UNIT_ALL;
}

export function getUnitPreference(unitStyle) {
    if (unitStyle === UNIT_PIXEL_ONLY) {
        return PIXEL;
    }
    const uFromPref = getPreference(DIST_READOUT);
    return (unitStyle === UNIT_NO_PIXEL && uFromPref === PIXEL) ? DEG : uFromPref || DEG;
}



function attach() {
    return {
        helpLine: selHelpText,
        drawData:{data:null},
        offsetCal: false,
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
    var drawAry= makeSelectObj(drawLayer.firstPt, imagePt, drawLayer.offsetCal, cc); //todo switch back
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
 * @param pref
 * @return {*}
 */
function getDistText(dist, pref) {
    if (pref !== PIXEL)  {     // world & pref is undefined or  world & pref is not PIXEL
        if(pref===ARC_MIN){
            return ` ${numeral(dist*60.0).format('0.000')}'`;
        }
        else if(pref===ARC_SEC){
            return ` ${numeral(dist*3600).format('0.000')}"`;
        } else {
            return ` ${numeral(dist).format('0.000')}${HTML_DEG}`;
        }
    } else {                                        // not world or world & pref is PIXEL
        return ` ${Math.floor(dist)} Pixels`;
    }
}

function getPosAngleText(pt0, pt1, isWorld, cc, pref) {
    if (!isWorld) return '';

    const PAPrefix = 'PA = ';
    const w0 = cc.getWorldCoords(pt0);
    const w1 = cc.getWorldCoords(pt1);
    let   posAngle = VisUtil.getPositionAngle(w0.x, w0.y, w1.x, w1.y);

    while (true) {
        if (posAngle > 180.0) {
            posAngle -= 360.0;
        } else if (posAngle <= -180.0) {
            posAngle += 360.0;
        } else {
            break;
        }
    }

    if(pref===ARC_MIN){
        return ` ${PAPrefix}${numeral(posAngle*60.0).format('0.000')}'`;
    } else if(pref===ARC_SEC){
        return ` ${PAPrefix}${numeral(posAngle*3600).format('0.000')}"`;
    } else {
        return ` ${PAPrefix}${numeral(posAngle).format('0.000')}${HTML_DEG}`;
    }
}


/**
 * check if the point is below the line by placing x & y with p.x & p.y in
 * ax+by+c formed by two ends of the line and check the value after that
 * @param line_pt1
 * @param line_pt2   point of greater  y
 * @param pt
 * @param cc
 * @returns {boolean}
 */
function isPointBelowLine(line_pt1, line_pt2, pt, cc) {
    const pt1 = cc.getScreenCoords(line_pt1);
    const pt2 = cc.getScreenCoords(line_pt2);
    const p = cc.getScreenCoords(pt);
    const diff = (p.x - pt2.x)*(pt2.y - pt1.y)-(pt2.x - pt1.x)*(p.y - pt2.y);

    return ((pt2.x <= pt1.x) && (diff >= 0)) || ((pt2.x > pt1.x) && (diff < 0)) ;
}
/**
 * @param {object} firstPt
 * @param {object} currentPt
 * @param {boolean} offsetCal
 * @param {CysConverter} cc
 * @return {Array}
 */
function makeSelectObj(firstPt,currentPt, offsetCal, cc) {
    const world = hasWCSProjection(cc);
    const unitStyle = getUnitStyle(cc, world);
    const pref = getUnitPreference(unitStyle);

    const ptAry= [firstPt,currentPt];
    let retval;
    let anyPt1;
    let anyPt2;
    let dist;
    const LWIDTH = 3;

    if (pref === PIXEL) {
        anyPt1 = cc.getScreenCoords(ptAry[0]);
        anyPt2 = cc.getScreenCoords(ptAry[1]);
        if (!anyPt1 || !anyPt2) return null;
        dist = screenDistance(anyPt1,anyPt2);
    } else {
        anyPt1 = cc.getWorldCoords(ptAry[0]);
        anyPt2 = cc.getWorldCoords(ptAry[1]);
        if (!anyPt1 || !anyPt2) return null;
        dist= VisUtil.computeDistance(anyPt1,anyPt2);
    }

    const d1 = cc.getScreenCoords(anyPt1);
    const d2 = cc.getScreenCoords(anyPt2);
    const d_midx = (d1.x+d2.x)/2;
    const d_midy = (d1.y+d2.y)/2;

    const obj= ShapeDataObj.makeLine(anyPt1,anyPt2, true);   // make line with arrow at the second end
    const distText = getDistText(dist, pref);
    const posAText = getPosAngleText(firstPt, currentPt, world, cc, DEG);
    obj.text = ((posAText) ? (posAText + '\n') : '') + distText;
    obj.style= Style.STARTHANDLED;
    obj.lineWidth = LWIDTH;
    obj.fontSize = '16px';
    obj.fontWeight = 'bold';
    obj.textLoc=TextLocation.LINE_TOP_STACK;
    obj.texttBaseLine = 'middle';

    if (offsetCal) {
        let highPt;
        let lowPt;

        if (d1.y <= d2.y) {
            highPt = anyPt1;
            lowPt = anyPt2;
        } else {
            highPt = anyPt2;
            lowPt = anyPt1;
        }

        let adjDist;
        let opDist;
        let lonDelta1;
        let lonDelta2;
        let latDelta1;
        let latDelta2;
        const plot= primePlot(visRoot(),cc.plotId);
        const aHiPS = isHiPS(plot);
        let  corner_pt;

        if (pref !== PIXEL) {
            corner_pt = makeWorldPt(highPt.x, lowPt.y);

            if (isPointBelowLine(highPt, lowPt, corner_pt, cc)) {
                lonDelta1 = makeWorldPt(lowPt.x, lowPt.y);
                lonDelta2 = corner_pt;
                adjDist = VisUtil.computeDistance(lonDelta1, lonDelta2);

                latDelta1 = corner_pt;
                latDelta2 = makeWorldPt(highPt.x, highPt.y);
                opDist = VisUtil.computeDistance(latDelta1, latDelta2);
            } else {
                corner_pt = makeWorldPt(lowPt.x, highPt.y);
                lonDelta1 = makeWorldPt(highPt.x, highPt.y);
                lonDelta2 = corner_pt;
                adjDist = VisUtil.computeDistance(lonDelta1, lonDelta2);

                latDelta1 = corner_pt;
                latDelta2 = makeWorldPt(lowPt.x, lowPt.y);
                opDist = VisUtil.computeDistance(latDelta1, latDelta2);
            }
        } else {
            lonDelta1= makeScreenPt(highPt.x, lowPt.y);
            lonDelta2= makeScreenPt(lowPt.x, lowPt.y);
            adjDist= screenDistance(lonDelta1,lonDelta2);

            latDelta1= makeScreenPt(highPt.x, lowPt.y);
            latDelta2= makeScreenPt(highPt.x, highPt.y);
            opDist= screenDistance(latDelta1,latDelta2);
        }

        const adj = ShapeDataObj.makeLine(lonDelta1,lonDelta2);
        const op = ShapeDataObj.makeLine(latDelta1, latDelta2);

        adj.lineWidth = LWIDTH;
        op.lineWidth = LWIDTH;

        const lineD = (d2.y - d1.y)*(d2.x - d1.x);

        let end1 = cc.getScreenCoords(adj.pts[0]);
        let end2 = cc.getScreenCoords(adj.pts[1]);
        const seg1 = 10.0;
        const seg2 = 16.0;

        console.log('seg1: '+seg1+ ' seg2: '+seg2);
        const ad_midy = (end1.y+end2.y)/2;
        adj.textLoc=TextLocation.LINE_MID_POINT;
        adj.textAlign = 'center';
        adj.textBaseLine = 'middle';
        adj.text = aHiPS || lineD !== 0 ? getDistText(adjDist,pref) : '';
        adj.textOffset = ad_midy >= d_midy ? makeOffsetPt(0, seg1) : makeOffsetPt(-seg2, -seg2);
        adj.style = Style.HANDLED;
        adj.fontSize = '16px';
        adj.fontWeight = 'bold';
        adj.offsetOnScreen = true;

        // for HiPS image, adj may not be perfectly horizontal
        if (end1.y === end2.y) {
            adj.textAngle = 0.0;
        } else {
            adj.textAngle = -Math.atan((end2.y - end1.y) / (end2.x -end1.x)) * 180.0 / Math.PI;
        }

        end1 = cc.getScreenCoords(op.pts[0]);
        end2 = cc.getScreenCoords(op.pts[1]);
        const op_midx = (end1.x + end2.x)/2;
        op.textLoc=TextLocation.LINE_MID_POINT;
        op.text= lineD !== 0 || aHiPS ? getDistText(opDist,pref) : '';
        op.textAlign = op_midx < d_midx ? 'end' : 'start';
        op.textOffset = op_midx < d_midx ? makeOffsetPt(-seg1, 0) : makeOffsetPt(seg1, 0);
        op.textBaseLine = 'middle';
        op.style = Style.HANDLED;
        op.fontSize = '16px';
        op.fontWeight = 'bold';
        op.offsetOnScreen = true;

        // for HiPS image, op may not be perfectly vertical
        if (end1.x === end2.x) {
            op.textAngle = 0.0;
        } else {
            const a =  Math.atan((end2.y- end1.y)/(end2.x - end1.x));

            if (a > 0) {
                op.textAngle = -(a - Math.PI/2) * 180.0/Math.PI;
            } else {
                op.textAngle = -(a + Math.PI/2) * 180.0/Math.PI;
            }
        }
        retval= [obj,adj,op];
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











