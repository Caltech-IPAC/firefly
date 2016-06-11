/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, set, isArray} from 'lodash';
import DrawLayerCntlr, {dispatchAttachLayerToPlot} from '../visualize/DrawLayerCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import CsysConverter from '../visualize/CsysConverter.js';
import BrowserInfo from '../util/BrowserInfo.js';
import VisUtil from '../visualize/VisUtil.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {makeImagePt, makeScreenPt} from '../visualize/Point.js';
import Enum from 'enum';


const editHelpText=
'Click center and drage to move, click corner and drage to resize';

const EDIT_DISTANCE= BrowserInfo.isTouchInput() ? 18 : 10;
const MARKER_SIZE = 40;      // marker original size in screen coordinate (radius of a circle)
const HANDLER_BOX = 4;        // handler size (square size in screen coordinate)
const markerInterval = 3000; // time interval for showing marker with handlers and no handlerss


const ID= 'OVERLAY_MARKER';
const TYPE_ID= 'OVERLAY_MARKER_TYPE';

const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,null,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID
const MarkerStatus = new Enum(['attached', 'select', 'relocate', 'resize']);

var getCC = (plotId) => {
    var plot = primePlot(visRoot(), plotId);
    return CsysConverter.make(plot);
};

var isWorld = (cc) => (cc.projection.isSpecified());
var getWorldOrImage = (pt, cc) => (isWorld(cc) ? cc.getWorldCoords(pt) : cc.getImageCoords(pt));

var idCnt=0;

/**
 * action creator for MARKER_ATTACH
 * @param rawAction
 * @returns {Function}
 */
export function markerToolAttachActionCreator(rawAction) {
    return (dispatcher) => {
        //console.log('in attach');
        var {plotId= get(visRoot(), 'activePlotId'), drawLayerId, attachPlotGroup } = rawAction.payload;

        if (plotId) {
            dispatchAttachLayerToPlot(drawLayerId, plotId, attachPlotGroup);

            var plot = primePlot(visRoot(), plotId);
            if (plot) {
                var wpt = plot.attributes[PlotAttribute.FIXED_TARGET];

                showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_ATTACH, evenSize(), wpt, plotId,
                    MarkerStatus.attached, markerInterval, drawLayerId);

            }
        }
    };
}

/**
 * action creator for MARKER_START
 * @param rawAction
 * @returns {Function}
 */
export function markerToolStartActionCreator(rawAction) {
    return (dispatcher) => {
        //console.log('in start');
        var {plotId, imagePt, screenPt, drawLayer} = rawAction.payload;
        var cc = getCC(plotId);
        var wpt;
        var {markerStatus, currentSize, currentPt, timeoutProcess, drawLayerId} = drawLayer;

        cancelTimeoutProcess(timeoutProcess);

        if (markerStatus === MarkerStatus.attached)  {             // marker moves to the mouse down position
            wpt = getWorldOrImage(imagePt, cc);

            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_START, evenSize(currentSize), wpt, plotId,
                MarkerStatus.relocate, markerInterval, drawLayerId);
        } else if (markerStatus === MarkerStatus.select) {        // check the position of mouse down: on circle, on handler, or none
            var idx = findClosestIndex(drawLayer.drawData.data, screenPt, cc);

            if (idx >= 0) {
                var nextStatus = idx === 0 ? MarkerStatus.relocate : MarkerStatus.resize;
                wpt = getWorldOrImage(currentPt, cc);

                // makrer stays at current position
                showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_START, evenSize(currentSize), wpt, plotId,
                    nextStatus, markerInterval, drawLayerId);
            }
        }
    };
}

/**
 * action creator for MARKER_END
 * @param rawAction
 * @returns {Function}
 */
export function markerToolEndActionCreator(rawAction) {
    return (dispatcher) => {
        //console.log('in end');
        var {plotId, drawLayer} = rawAction.payload;
        var cc = getCC(plotId);
        var {markerStatus, currentSize, currentPt, timeoutProcess, drawLayerId} = drawLayer;

        cancelTimeoutProcess(timeoutProcess);

        // mouse stay at current position and size
        if (markerStatus === MarkerStatus.relocate || markerStatus === MarkerStatus.resize) {
            var wpt = getWorldOrImage(currentPt, cc);

            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_END, evenSize(currentSize), wpt, plotId,
                MarkerStatus.select, markerInterval, drawLayerId);
        }
    };
}

/**
 * action create for MARKER_MOVE
 * @param rawAction
 * @returns {Function}
 */
export function markerToolMoveActionCreator(rawAction) {
    return (dispatcher) => {
        //console.log('in move');
        var {plotId, imagePt, screenPt, drawLayer} = rawAction.payload;
        var cc = getCC(plotId);
        var {markerStatus, currentSize: newSize, currentPt: wpt, timeoutProcess, drawLayerId} = drawLayer;

        cancelTimeoutProcess(timeoutProcess);

        if (markerStatus === MarkerStatus.resize)  {               // mmarker stay at current point, and change size
            var screenCenter = cc.getScreenCoords(wpt);
            newSize = [Math.abs(screenPt.x - screenCenter.x)*2, Math.abs(screenPt.y - screenCenter.y)*2];
        } else if (markerStatus === MarkerStatus.relocate) {      // marker move to new mouse down positon
            wpt = getWorldOrImage(imagePt, cc);
        }
        showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_MOVE, newSize, wpt, plotId,
            markerStatus, 0, drawLayerId);
    };
}


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

    //var actionTypes=[DrawLayerCntlr.MARKER_LOC];

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

/**
 * reducer for MarkerTool layer
 * @param drawLayer
 * @param action
 * @returns {*}
 */
function getLayerChanges(drawLayer, action) {
    var {drawLayerId} = action.payload;

    if (!drawLayerId || drawLayerId !== drawLayer.drawLayerId) return null;

    switch (action.type) {
        case DrawLayerCntlr.MARKER_START:
        case DrawLayerCntlr.MARKER_ATTACH:
        case DrawLayerCntlr.MARKER_MOVE:
        case DrawLayerCntlr.MARKER_END:
            //console.log(action.type);
            return makeMarker(action);
            //console.log('next marker status: ' + (get(dl, 'markerStatus') ? dl.markerStatus.key : 'null'));
    }
}

/**
 * make rectangle with the same width and height
 * @param size
 * @returns {*[]}
 */
var evenSize = (size) => {
    var s;
    if (size) {
        s = (isArray(size) && size.length > 1) ? Math.min(size[0], size[1]): size;
    } else {
        s = MARKER_SIZE;
    }
    return [s, s];
};

var cancelTimeoutProcess = (toP) => { if (toP) clearTimeout(toP); };


const screenDistance= (pt1,pt2) => VisUtil.computeScreenDistance(pt1.x,pt1.y,pt2.x,pt2.y);

/**
 * find the drawObj inside the marker which is closest to the given screenPt
 * @param drawObjAry
 * @param screenPt
 * @param cc
 * @returns {*}
 */
function findClosestIndex(drawObjAry, screenPt, cc) {
    var distance = EDIT_DISTANCE;

    return  drawObjAry.reduce( (prev, drawObj, index) => {
        var dist;
        var centerPt = cc.getScreenCoords(drawObj.pts[0]);

        if (drawObj.type === ShapeDataObj.SHAPE_DATA_OBJ) {

            dist = distance;
            if (drawObj.sType === ShapeDataObj.ShapeType.Circle) {
                dist = screenDistance(screenPt, centerPt) - drawObj.radius;
                if (dist < 0) {
                    dist = 0;
                }
            } else if (drawObj.sType === ShapeDataObj.ShapeType.Rectangle) {

                var x1 = centerPt.x - drawObj.width / 2;
                var x2 = centerPt.x + drawObj.width / 2;
                var y1 = centerPt.y - drawObj.height / 2;
                var y2 = centerPt.y + drawObj.height / 2;

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
        }
        return prev;
    }, -1);
}


/**
 * dispatch action to locate marker with corners and no corner by timer interval on the draw layer
 * @param dispatcher
 * @param actionType
 * @param size   in screen coordinate
 * @param wpt    marker location world coordinate
 * @param plotId
 * @param doneStatus
 * @param drawLayerId
 * @param timer  milliseconds
 */
function showMarkersByTimer(dispatcher, actionType, size, wpt, plotId,  doneStatus, timer, drawLayerId) {
    var setAction = (isHandler) => ({
        type: actionType,
        payload: {isHandler, size, wpt, plotId, markerStatus: doneStatus, drawLayerId}
    });

    var timeoutProcess = (timer !== 0) && (setTimeout(() => dispatcher(setAction(false)), timer));
    var crtAction = set(setAction(true), 'payload.timeoutProcess', timeoutProcess);
    dispatcher(crtAction);
}

/**
 * create drawlayer object containing only the properties which has updated value
 * @param action
 * @returns {*}
 */
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
        currentPt: wpt,                    // center, world or image coordinate
        currentSize: [markerW, markerH]
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
             exclusiveDef = { exclusiveOnDown: true, type : 'vertexOnly' };
             vertexDef = {points:[imgPt], pointDist: Math.sqrt(Math.pow(markerW/2, 2) + Math.pow(markerH/2, 2))};
         }
         return Object.assign(dlObj, {markerStatus, vertexDef, exclusiveDef});
     } else {
         return dlObj;
     }
}


/**
 * create drawObj object set for marker
 * @param plot
 * @param centerPt
 * @param width
 * @param height
 * @param includeHandler
 * @returns {Array}
 */
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

            var handlerCenter = getWorldOrImage(makeImagePt(x, y), cc);
            var handlerBox = ShapeDataObj.makeRectangleByCenter(handlerCenter, HANDLER_BOX, HANDLER_BOX,
                            ShapeDataObj.UnitType.PIXEL, 0.0, ShapeDataObj.UnitType.ARCSEC, false);
            prev.push(handlerBox);
            return prev;
        }, retval);
    }
    return retval;
}

