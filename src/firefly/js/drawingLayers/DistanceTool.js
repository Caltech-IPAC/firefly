/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isBoolean, isEmpty, isUndefined} from 'lodash';
import DrawLayerCntlr, {DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import {getPreference} from '../core/AppDataCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef,Style, TextLocation} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import CsysConverter from '../visualize/CsysConverter.js';
import { makeOffsetPt, makeWorldPt} from '../visualize/Point.js';
import BrowserInfo from '../util/BrowserInfo.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {computeDistance, computeScreenDistance, getPositionAngle} from '../visualize/VisUtil';
import {getUIComponent} from './DistanceToolUI.jsx';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {hasWCSProjection} from '../visualize/PlotViewUtil';
import {isHiPS} from '../visualize/WebPlot.js';
import {DrawingType} from '../visualize/draw/DrawObj';
import {makeImagePt} from '../visualize/Point';


const EDIT_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;


const ID= 'DISTANCE_TOOL';
const TYPE_ID= 'DISTANCE_TOOL_TYPE';
const SIGFIG = 3;

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
        const srcPlot= primePlot(visRoot(),plotId);
        const cc= CsysConverter.make(srcPlot);
        const srcWorld = hasWCSProjection(cc);

        var sel= {pt0:drawLayer.firstPt,pt1:drawLayer.currentPt};

        drawLayer.plotIdAry.forEach( (pId) => {
            if (pId===plotId) {
                dispatchAttributeChange({plotId:pId, toAllPlotsInPlotView:false, overlayColorScope:false,
                    attKey:PlotAttribute.ACTIVE_DISTANCE,attValue:sel,
                });
            }
            else {
                const p= primePlot(visRoot(),pId);
                if (srcWorld && hasWCSProjection(p)) {
                    const targetCC= CsysConverter.make(p);
                    const pt0= targetCC.getImageCoords(cc.getWorldCoords(drawLayer.firstPt));
                    const pt1= targetCC.getImageCoords(cc.getWorldCoords(drawLayer.currentPt));
                    dispatchAttributeChange({plotId:pId, toAllPlotsInPlotView:false, overlayColorScope:false,
                        attKey:PlotAttribute.ACTIVE_DISTANCE,attValue:{pt0,pt1},
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
        [MouseState.DRAG.key]: DrawLayerCntlr.DT_MOVE,
        [MouseState.DOWN.key]: DrawLayerCntlr.DT_START,
        [MouseState.UP.key]: DrawLayerCntlr.DT_END
    };


    const exclusiveDef= { exclusiveOnDown: true, type : 'anywhere' };

    const actionTypes= [DrawLayerCntlr.DT_START,
                      DrawLayerCntlr.DT_MOVE,
                      DrawLayerCntlr.DT_END];

    idCnt++;
    const options= {
        canUseMouse:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        destroyWhenAllDetached: true
    };
    return DrawLayer.makeDrawLayer( `${ID}-${idCnt}`, TYPE_ID, 'Distance Tool',
                                     options, drawingDef, actionTypes, pairs, exclusiveDef, getCursor );
}

function onDetach(drawLayer,action) {
    const {plotIdAry}= action.payload;
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

    const ptAry= getPtAry(plot);
    if (!ptAry) return null;
    const idx= findClosestPtIdx(ptAry,screenPt);
    if (screenDistance(ptAry[idx],screenPt)<EDIT_DISTANCE) {
        return 'pointer';
    }
    return null;
}


function getLayerChanges(drawLayer, action) {

    switch (action.type) {
        case DrawLayerCntlr.DT_START:
            return start(drawLayer, action);
        case DrawLayerCntlr.DT_MOVE:
            return drag(drawLayer,action);
        case DrawLayerCntlr.DT_END:
            return end(action);
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            if (isEmpty(drawLayer?.drawData?.data)) {
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
 * Fs
 * @param plot
 * @param firstPt
 * @param currPt
 * @param drawAry
 * @return {object}
 */
function makeBaseReturnObj(plot,firstPt,currPt,drawAry )  {

    const exclusiveDef= { exclusiveOnDown: true, type : 'vertexThenAnywhere' };

    return {drawData:{data:drawAry},
            exclusiveDef,
            vertexDef:{points:[firstPt, currPt], pointDist:EDIT_DISTANCE}
    };

}


function dealWithUnits(drawLayer,action) {
    const {plotIdAry}= action.payload;
    const plot= primePlot(visRoot(),plotIdAry[0]);
    const cc= CsysConverter.make(plot);
    if (!cc || !plot) return null;

    const selection = plot.attributes[PlotAttribute.ACTIVE_DISTANCE];
    if (!selection) return null;
    const {pt0,pt1}=selection;

    const drawAry= makeSelectObj(pt0,pt1, drawLayer.offsetCal,cc);

    return makeBaseReturnObj(plot,pt0, pt1,drawAry);
}

function dealWithMods(drawLayer,action) {
    const {changes,plotIdAry}= action.payload;
    if (isBoolean(changes.offsetCal)) {
        const plot= primePlot(visRoot(),plotIdAry[0]);
        const cc= CsysConverter.make(plot);
        if (!cc) return null;
        const selection = plot.attributes[PlotAttribute.ACTIVE_DISTANCE];
        if (!selection) return null;
        const {pt0,pt1}=selection;
        const drawAry= makeSelectObj(pt0,pt1, changes.offsetCal, cc);
        return Object.assign({offsetCal:changes.offsetCal},
                              makeBaseReturnObj(plot,drawLayer.firstPt, drawLayer.currentPt,drawAry));
    }
    return null;
}


export function getUnitStyle(cc, world) {

    if (!cc || !world) {
        return UNIT_PIXEL_ONLY;
    } else {
        return isHiPS(primePlot(visRoot(),cc.plotId)) ? UNIT_NO_PIXEL : UNIT_ALL;
    }
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
        moveHead: true,      // drag start from head or not
        vertexDef: {points:null, pointDist:EDIT_DISTANCE},
        exclusiveDef: { exclusiveOnDown: true, type : 'anywhere' }
    };
    
}

function getMode(plot) {
    if (!plot) return 'select';
    var selection = plot.attributes[PlotAttribute.ACTIVE_DISTANCE];
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

        if (screenDistance(testPt,spt)<EDIT_DISTANCE) {   // swap the first and current point, redraw the distance tool
            retObj.moveHead = (idx === 1);
            retObj.firstPt= cc.getImageWorkSpaceCoords(ptAry[0]);
            retObj.currentPt= cc.getImageWorkSpaceCoords(ptAry[1]);
            if (!retObj.firstPt || !retObj.currentPt) return {};

            const drawAry = makeSelectObj(retObj.firstPt, retObj.currentPt, drawLayer.offsetCal, CsysConverter.make(plot));
            return Object.assign(retObj, makeBaseReturnObj(plot, retObj.firstPt, retObj.currentPt, drawAry));
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

    const newFirst = drawLayer.moveHead ? drawLayer.firstPt : imagePt;
    const newCurrent = drawLayer.moveHead ? imagePt : drawLayer.currentPt;

    const drawAry= makeSelectObj(newFirst, newCurrent, drawLayer.offsetCal, cc);
    return Object.assign({firstPt: newFirst, currentPt:newCurrent}, makeBaseReturnObj(plot,  newFirst, newCurrent,drawAry));
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
    return {firstPt: imagePt, currentPt: imagePt,  moveHead: true};
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



const screenDistance= (pt1,pt2) => computeScreenDistance(pt1.x,pt1.y,pt2.x,pt2.y);


/**
 *
 * @param dist
 * @param pref
 * @return {*}
 */
function getDistText(dist, pref) {
    if (pref !== PIXEL)  {     // world & pref is undefined or  world & pref is not PIXEL
        if(pref===ARC_MIN){
            return ` ${Number(dist*60.0).toPrecision(SIGFIG)}'`;
        }
        else if(pref===ARC_SEC){
            return ` ${Number(dist*3600).toPrecision(SIGFIG)}"`;
        } else {
            return ` ${Number(dist).toPrecision(SIGFIG)}${HTML_DEG}`;
        }
    } else {                                        // not world or world & pref is PIXEL
        return ` ${Math.floor(dist)} Pixels`;
    }
}

function getPosAngleText(pt0, pt1, isWorld, cc, pref) {
    if (!isWorld || pref === PIXEL) return '';

    const PAPrefix = 'PA = ';
    const w0 = cc.getWorldCoords(pt0);
    const w1 = cc.getWorldCoords(pt1);
    let   posAngle = getPositionAngle(w0.x, w0.y, w1.x, w1.y);

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
        return ` ${PAPrefix}${Number(posAngle*60.0).toPrecision(SIGFIG)}'`;
    } else if(pref===ARC_SEC){
        return ` ${PAPrefix}${Number(posAngle*3600).toPrecision(SIGFIG)}"`;
    } else {
        return ` ${PAPrefix}${Number(posAngle).toPrecision(SIGFIG)}${HTML_DEG}`;
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

function lookupDistance(pt1, pt2, cc, pref) {
    let anyPt1, anyPt2;

    if (pref === PIXEL) {
        anyPt1 = cc.getImageCoords(pt1);
        anyPt2 = cc.getImageCoords(pt2);
        if (!anyPt1 || !anyPt2) return null;
        return screenDistance(anyPt1, anyPt2);
    } else {
        anyPt1 = cc.getWorldCoords(pt1);
        anyPt2 = cc.getWorldCoords(pt2);
        if (!anyPt1 || !anyPt2) return null;
        return  computeDistance(anyPt1,anyPt2);
    }
}
/**
 * @param {object} firstPt
 * @param {object} currentPt
 * @param {boolean} offsetCal
 * @param {CysConverter} cc
 * @return {Array}
 */
function makeSelectObj(firstPt,currentPt, offsetCal, cc) {
    const world = cc ? hasWCSProjection(cc) : false;
    const unitStyle = getUnitStyle(cc, world);
    const pref = getUnitPreference(unitStyle);

    const ptAry= [firstPt,currentPt];
    let retval;
    let anyPt1;    // for ends of all ruler vectors
    let anyPt2;
    let dist;
    const LWIDTH = 3;

    // for distance overlay set with world unit, the ends of the ruler are with world coordinate values
    // or the ends of the ruler are with pixel values of image coordinate
    if (pref === PIXEL) {
        anyPt1 = cc.getImageCoords(ptAry[0]);
        anyPt2 = cc.getImageCoords(ptAry[1]);
        if (!anyPt1 || !anyPt2) return null;
        dist = screenDistance(anyPt1,anyPt2);
    } else {
        anyPt1 = cc.getWorldCoords(ptAry[0]);
        anyPt2 = cc.getWorldCoords(ptAry[1]);
        if (!anyPt1 || !anyPt2) return null;
        dist= computeDistance(anyPt1,anyPt2);
    }

    const obj= ShapeDataObj.makeLine(anyPt1, anyPt2, true);   // make line with arrow at the current end
    const distText = getDistText(dist, pref);
    const posAText = getPosAngleText(ptAry[0], ptAry[1], world, cc, (pref === PIXEL ? PIXEL : DEG));
    obj.text = ((posAText) ? (posAText + '\n') : '') + distText;
    obj.style= Style.STARTHANDLED;
    obj.lineWidth = LWIDTH;
    obj.fontSize = '16px';
    obj.fontWeight = 'bold';
    obj.textLoc=TextLocation.LINE_TOP_STACK;
    obj.texttBaseLine = 'middle';
    obj.drawEvenIfWrapping= true;
    obj.supportedDrawingTypes=  (pref===PIXEL) ? DrawingType.ImageCoordsOnly : DrawingType.WcsCoordsOnly;

    if (!offsetCal) {
        retval= [obj];
    } else {
        const d1 = cc.getScreenCoords(ptAry[0]);     // use screen coordinate to determine the text position
        const d2 = cc.getScreenCoords(ptAry[1]);
        const d_midx = (d1.x+d2.x)/2;
        const d_midy = (d1.y+d2.y)/2;

        let highPt, lowPt;
        if (d1.y <= d2.y) {
            highPt = anyPt1;
            lowPt = anyPt2;
        } else {
            highPt = anyPt2;
            lowPt = anyPt1;
        }

        let lon1, lon2;
        let lat1, lat2;
        const plot= primePlot(visRoot(),cc.plotId);
        const aHiPS = isHiPS(plot);
        let  corner_pt;

        if (pref !== PIXEL) {
            corner_pt = makeWorldPt(highPt.x, lowPt.y);

            if (isPointBelowLine(highPt, lowPt, corner_pt, cc)) {
                lon1 = makeWorldPt(lowPt.x, lowPt.y);
                lon2 = corner_pt;

                lat1 = corner_pt;
                lat2 = makeWorldPt(highPt.x, highPt.y);
            } else {
                corner_pt = makeWorldPt(lowPt.x, highPt.y);
                lon1 = makeWorldPt(highPt.x, highPt.y);
                lon2 = corner_pt;

                lat1 = corner_pt;
                lat2 = makeWorldPt(lowPt.x, lowPt.y);
            }
        } else {
            lon1= makeImagePt(highPt.x, lowPt.y);   // corner
            lon2= makeImagePt(lowPt.x, lowPt.y);

            lat1= makeImagePt(highPt.x, lowPt.y);  // corner
            lat2= makeImagePt(highPt.x, highPt.y);
        }

        const adjDist = lookupDistance(lon1, lon2, cc, pref);
        const opDist = lookupDistance(lat1, lat2, cc, pref);

        const adj = ShapeDataObj.makeLine(cc.getWorldCoords(lon1),cc.getWorldCoords(lon2));
        const op = ShapeDataObj.makeLine(cc.getWorldCoords(lat1), cc.getWorldCoords(lat2));

        adj.lineWidth = LWIDTH;
        op.lineWidth = LWIDTH;

        // no distance is shown on vertical or horizontal offset vector
        const lineD = (d2.y - d1.y)*(d2.x - d1.x);

        let end1 = cc.getScreenCoords(adj.pts[0]);
        let end2 = cc.getScreenCoords(adj.pts[1]);
        const seg1 = 10.0;
        const seg2 = 16.0;

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
        obj.drawEvenIfWrapping= true;
        adj.supportedDrawingTypes=  (pref===PIXEL) ? DrawingType.ImageCoordsOnly : DrawingType.WcsCoordsOnly;


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
        obj.drawEvenIfWrapping= true;
        op.supportedDrawingTypes=  (pref===PIXEL) ? DrawingType.ImageCoordsOnly : DrawingType.WcsCoordsOnly;

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
