/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty, get, set, isArray} from 'lodash';
import DrawLayerCntlr, {DRAWING_LAYER_KEY, dispatchAttachLayerToPlot} from '../visualize/DrawLayerCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import CsysConverter from '../visualize/CsysConverter.js';
import BrowserInfo from '../util/BrowserInfo.js';
import VisUtil from '../visualize/VisUtil.js';
import SelectBox from '../visualize/draw/SelectBox.js';
import {getPlotViewById, primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {Style} from '../visualize/draw/DrawingDef.js';
//import DrawLayerFactory from '../visualize/draw/DrawLayerFactory.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {makeImagePt, makeScreenPt} from '../visualize/point.js';
import {flux} from '../Firefly.js';
import Enum from 'enum';


const editHelpText=
'Click center and drage to move, click corner and drage to resize';

const EDIT_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;
const MARKER_SIZE = 40;      // marker original size in screen coordinate (radius of a circle)
const HANDLER_BOX = 4;        // handler size (square size in screen coordinate)
const markerInterval = 3000; // time interval for showing marker with handlers and no handlerss


const ID= 'OVERLAY_MARKER';
const TYPE_ID= 'OVERLAY_MARKER_TYPE';

const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,onDetach,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID
const MarkerStatus = new Enum(['attached', 'select', 'relocate', 'resize']);

var getCC = (plotId) => {
    var plot = primePlot(visRoot(), plotId);
    return CsysConverter.make(plot);
};

var idCnt=0;

/**
 *
 * @return {Function}
 */
function creator(initPayload) {

    var drawingDef= makeDrawingDef('red');
    var pairs= {
        [MouseState.DRAG.key]: DrawLayerCntlr.MARKER_MOVE,
        [MouseState.DOWN.key]: DrawLayerCntlr.MARKER_START,
        [MouseState.UP.key]: DrawLayerCntlr.MARKER_END
    };

    var actionTypes= [DrawLayerCntlr.MARKER_MOVE,
                      DrawLayerCntlr.MARKER_START,
                      DrawLayerCntlr.MARKER_END,
                      DrawLayerCntlr.MARKER_ATTACH];
    
    var exclusiveDef= { exclusiveOnDown: true, type : 'anywhere' };


    idCnt++;
    var options= {
        canUseMouse:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        canUserDelete: true,
        destroyWhenAllDetached: true
    };
    return  DrawLayer.makeDrawLayer( get(initPayload, 'drawLayerId', `${ID}-${idCnt}`),
                                    TYPE_ID, get(initPayload, 'Title', 'Marker Tool'),
                                    options, drawingDef, actionTypes, pairs, exclusiveDef);
}

function onDetach(drawLayer,action) {
    var {plotIdAry}= action.payload;
    plotIdAry.forEach( (plotId) => dispatchAttributeChange(plotId,false,PlotAttribute.SELECTION,null,true));
}

function getLayerChanges(drawLayer, action) {
    //var dd = Object.assign({}, drawLayer.drawData);

    switch (action.type) {
        case DrawLayerCntlr.MARKER_START:
            console.log(action.type);
            var dl = makeMarker(action);
            console.log('next marker status: ' +(get(dl, 'markerStatus') ? dl.markerStatus.key : 'null'));
            return dl;
        case DrawLayerCntlr.MARKER_MOVE:
        case DrawLayerCntlr.MARKER_END:
        case DrawLayerCntlr.MARKER_ATTACH:
            console.log(action.type);
            var dl = makeMarker(action);
            console.log('next marker status: ' + (get(dl, 'markerStatus') ? dl.markerStatus.key : 'null'));
            return dl;
    }

}

var evenSize = (size) => {
    var s;
    if (size) {
        s = (isArray(size) && size.length > 1) ? Math.min(size[0], size[1]): size;
    } else {
        s = MARKER_SIZE;
    }
    return [s, s];
};

export function markerToolAttachActionCreator(rawAction) {
    return (dispatcher) => {
        var {plotId= get(visRoot(), 'activePlotId'), drawLayerId, attachPlotGroup } = rawAction.payload;

        if (plotId) {
            dispatchAttachLayerToPlot(drawLayerId, plotId, attachPlotGroup);

            var plot = primePlot(visRoot(), plotId);
            if (plot) {
                var wpt = plot.attributes[PlotAttribute.FIXED_TARGET];

                showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_ATTACH, evenSize(), wpt, plotId,
                                   MarkerStatus.attached, markerInterval);

            }
        }
    };
}


export function markerToolStartActionCreator(rawAction) {
    return (dispatcher) => {
        var {plotId, imagePt, screenPt, drawLayer} = rawAction.payload;
        var cc = getCC(plotId);

        var wpt;
        var {markerStatus, currentSize, currentPt, timeoutProcess} = drawLayer;

        console.log('start: status = ' + (markerStatus? markerStatus.key: 'null') + ' timeout: ' + (timeoutProcess ? timeoutProcess : 'null'));
        if (timeoutProcess) clearTimeout(timeoutProcess);

        if (markerStatus === MarkerStatus.attached)  {             // marker moves to the mouse down position
            wpt = cc.getWorldCoords(imagePt);
            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_START, evenSize(currentSize), wpt, plotId,
                               MarkerStatus.relocate, markerInterval, timeoutProcess);
        } else if (markerStatus === MarkerStatus.select) {        // check the position of mouse down: on circle, on handler, or none
             var idx = findClosestIndex(drawLayer.drawData.data, screenPt, cc);


             if (idx >= 0) {
                 var nextStatus = idx === 0 ? MarkerStatus.relocate : MarkerStatus.resize;
                 wpt = cc.getWorldCoords(currentPt);

                 // makrer stays at current position
                 showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_START, evenSize(currentSize), wpt, plotId,
                                    nextStatus, markerInterval);
             }
        }
    };
}

export function markerToolEndActionCreator(rawAction) {
    return (dispatcher) => {
        var {plotId, drawLayer} = rawAction.payload;
        var cc = getCC(plotId);
        var {markerStatus, currentSize, currentPt, timeoutProcess} = drawLayer;

        if (timeoutProcess) clearTimeout(timeoutProcess);
console.log('end: status = ' + (markerStatus? rawAction.payload.drawLayer.markerStatus.key: 'null') + ' timeout: ' + (timeoutProcess ? timeoutProcess : 'null'));
        // mouse stay at current position and size
        if (markerStatus === MarkerStatus.relocate || markerStatus === MarkerStatus.resize) {
            var wpt = cc.getWorldCoords(currentPt);
            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_END, evenSize(currentSize), wpt, plotId,
                               timeoutProcess, MarkerStatus.select, markerInterval);
        }
    };
}

export function markerToolMoveActionCreator(rawAction) {
    return (dispatcher) => {
        var {plotId, imagePt, screenPt, drawLayer} = rawAction.payload;
        var cc = getCC(plotId);
        var {markerStatus, currentSize: newSize, currentPt: wpt, timeoutProcess} = drawLayer;

        if (timeoutProcess) clearTimeout(timeoutProcess);
        console.log('move: status = ' + (markerStatus? markerStatus.key: 'null') + ' timeout: ' + (timeoutProcess ? timeoutProcess : 'null'));

        if (markerStatus === MarkerStatus.resize)  {               // mmarker stay at current point, and change size
            var screenCenter = cc.getScreenCoords(wpt);
            newSize = [Math.abs(screenPt.x - screenCenter.x)*2, Math.abs(screenPt.y - screenCenter.y)*2];
        } else if (markerStatus === MarkerStatus.relocate) {      // marker move to new mouse down positon
            wpt = cc.getWorldCoords(imagePt);
        }
        showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_MOVE, newSize, wpt, plotId,
                           markerStatus, markerInterval);
    };
}

const screenDistance= (pt1,pt2) => VisUtil.computeScreenDistance(pt1.x,pt1.y,pt2.x,pt2.y);

function findClosestIndex(drawObjAry, screenPt, cc) {
    var distance = EDIT_DISTANCE;

    return  drawObjAry.reduce( (prev, drawObj, index) => {
        var dist;
        var centerPt = cc.getScreenCoords(drawObj.pts);

        if (drawObj.type === ShapeDataObj.UnitType.Circle) {
            dist = screenDistance(screenPt, centerPt) - drawObj.radius;
            if (dist < 0) {
                dist = 0;
            }
        } else if (drawObj.type === ShapeDataObj.UnitType.Rectangle) {
            var x1, x2, y1, y2;

            x1 = centerPt.x - drawObj.width/2;
            x2 = centerPt.x + drawObj.width/2;
            y1 = centerPt.y - drawObj.height/2;
            y2 = centerPt.y + drawObj.height/2;

            var bx = (screenPt.x >= x1 && screenPt.x <= x2) ? screenPt.x :
                     ((screenPt.x < x1) ? x1 : x2);
            var by = (screenPt.y >= y1 && screenPt.y <= y2) ? screenPt.y :
                     ((screenPt.y < y1) ? y1 : y2);

            dist = screenDistance(screenPt, makeScreenPt(bx, by));
        }

        if (dist < distance) {
            distance = dist;
            prev = index;
        }
        return prev;
    }, -1);
}


/**
 * dispatch action to locate marker with corners and no corner by timer interval on the draw layer
 * @param dispatcher
 * @param size   in screen coordinate
 * @param wpt    marker location world coordinate
 * @param plotId
 * @param timer  milliseconds
 */
function showMarkersByTimer(dispatcher, actionType, size, wpt, plotId,  doneStatus, timer) {
    var setAction = (isHandler) => ({
        type: actionType,
        payload: {isHandler, size, wpt, plotId, markerStatus: doneStatus}
    });

    var timeoutProcess = setTimeout(() => dispatcher(setAction(false)), timer);
    console.log('new timeout ' + timeoutProcess);
    var crtAction = set(setAction(true), 'payload.timeoutProcess', timeoutProcess);
    dispatcher(crtAction);
}

function makeMarker(action) {
     var {plotId, isHandler, wpt, size, markerStatus, timeoutProcess} = action.payload;

     if (!plotId || !wpt) return null;

     const plot = primePlot(visRoot(), plotId);
     var [markerW, markerH] = isArray(size) && size.length > 1 ?  [size[0], size[1]] : [size, size];
     var markObjs = makeMarkDrawObj(plot, wpt, markerW, markerH, isHandler);
     var exclusiveDef, vertexDef; // cursor point
     var dlObj =  {
        helpLine: editHelpText,
        drawData: {data:markObjs},
        currentPt: wpt,                    // center
        currentSize: [markerW, markerH],
     };
     if (timeoutProcess) {
         dlObj.timeoutProcess = timeoutProcess;
     }

     if (markerStatus) {
         var cc = CsysConverter.make(plot);
         const imgPt = cc.getImageCoords(wpt);

         if (markerStatus === MarkerStatus.attached) {
             exclusiveDef = { exclusiveOnDown: true, type : 'anywhere' };
             vertexDef = {points:null, pointDist:EDIT_DISTANCE};
         } else {
             exclusiveDef = { exclusiveOnDown: true, type : 'vertexThenAnywhere' };
             vertexDef = {points:[imgPt], pointDist: Math.sqrt(Math.pow(markerW/2, 2) + Math.pow(markerH/2, 2))};
         }
         return Object.assign(dlObj, {markerStatus, vertexDef, exclusiveDef});
     } else {
         return dlObj;
     }
}

function makeMarkDrawObj(plot, centerPt, width, height, includeHandler) {
    var retval;
    var radius = Math.min(width, height)/2;

    retval = [ShapeDataObj.makeCircleWithRadius(centerPt, radius)];

    if (includeHandler) {
        var cc = CsysConverter.make(plot);
        var corners = [[-1, 1], [1, 1], [-1, -1], [1, -1]];
        var imgPt = cc.getImageCoords(centerPt);

        retval = corners.reduce((prev, coord) => {
            var x = imgPt.x + coord[0] * width/(2*cc.zoomFactor);
            var y = imgPt.y + coord[1] * height/(2*cc.zoomFactor);

            var handlerCenter = cc.getWorldCoords(makeImagePt(x, y));
            var handlerBox = ShapeDataObj.makeRectangleByCenter(handlerCenter, HANDLER_BOX, HANDLER_BOX,
                            ShapeDataObj.UnitType.PIXEL, 0.0, ShapeDataObj.UnitType.ARCSEC, false);
            prev.push(handlerBox);
            return prev;
        }, retval);
    }
    return retval;
}

