/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, set, isArray, isEmpty} from 'lodash';
import DrawLayerCntlr, {dispatchAttachLayerToPlot,
                        dispatchCreateDrawLayer, getDlAry} from '../visualize/DrawLayerCntlr.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {makeMarker, findClosestIndex,
        updateMarkerDrawObjText, MARKER_DISTANCE} from '../visualize/draw/MarkerObj.js';
import {getMarkerToolUIComponent} from './MarkerToolUI.jsx';
import Enum from 'enum';


const editHelpText='Click center and drage to move, click corner and drage to resize';

const MARKER_SIZE = 40;      // marker original size in screen coordinate (radius of a circle)
const markerInterval = 3000; // time interval for showing marker with handlers and no handlers
const ID= 'OVERLAY_MARKER';
const TYPE_ID= 'OVERLAY_MARKER_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,null,getMarkerToolUIComponent);
const MarkerStatus = new Enum(['attached', 'select', 'relocate', 'resize']);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var getCC = (plotId) => {
    var plot = primePlot(visRoot(), plotId);
    return CsysConverter.make(plot);
};

var isWorld = (cc) => (cc.projection.isSpecified());
var getWorldOrImage = (pt, cc) => (isWorld(cc) ? cc.getWorldCoords(pt) : cc.getImageCoords(pt));

var idCnt=0;


var cancelTimeoutProcess = (toP) => { if (toP) clearTimeout(toP); };

export function markerToolCreateLayerActionCreator(rawAction) {
    return (dispatcher) => {
        var {plotId,
             markerId: drawLayerId,
             layerTitle:Title, attachPlotGroup} = rawAction.payload;
        var dl = getDrawLayerById(getDlAry(), drawLayerId);

        if (!dl) {
            dispatchCreateDrawLayer(TYPE_ID, {Title, drawLayerId});
        }

        // plotId could be an array or single value
        var pId = (!plotId || (isArray(plotId)&&plotId.length === 0)) ? get(visRoot(), 'activePlotId') :
                                                                        isArray(plotId) ? plotId[0] : plotId;

        if (pId) {
            dispatchAttachLayerToPlot(drawLayerId, pId, attachPlotGroup);

            var plot = primePlot(visRoot(), pId);
            if (plot) {
                var wpt = plot.attributes[PlotAttribute.FIXED_TARGET];

                showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_CREATE, evenSize(), wpt, pId,
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
            var idx = findClosestIndex(screenPt, get(drawLayer, ['drawData', 'data', '0']), cc);

            if (idx >= 0) {
                var nextStatus = idx === 0 ? MarkerStatus.relocate : MarkerStatus.resize;

                wpt = getWorldOrImage(currentPt, cc);

                // makrer stays at current position for further mouse drag (resize or relocate) or mouse up
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

        // marker stays at current position and size
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
        // resize (newDize) or relocate (wpt),  status remains the same
        showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_MOVE, newSize, wpt, plotId,
                           markerStatus, 0, drawLayerId);
    };
}


/**
 * create drawing layer for a new marker
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
                      DrawLayerCntlr.MARKER_CREATE];

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
    var dd = Object.assign({}, drawLayer.drawData);

    switch (action.type) {
        case DrawLayerCntlr.MARKER_CREATE:
        case DrawLayerCntlr.MARKER_START:
        case DrawLayerCntlr.MARKER_MOVE:
        case DrawLayerCntlr.MARKER_END:
            var data = get(dd, DataTypes.DATA);
            var {text, textLoc} = isEmpty(data) ? {} : data[0];

            return createMarkerObjs(action, text, textLoc);

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            var {markerText, markerTextLoc} = action.payload.changes;

            return updateMarkerText(markerText, markerTextLoc, dd[DataTypes.DATA]);
    }
}

/**
 *
 * @param text
 * @param textLoc
 * @param markerDrawObj
 * @returns {*}
 */
function updateMarkerText(text, textLoc, markerDrawObj) {
    var textUpdatedObj = updateMarkerDrawObjText(markerDrawObj[0], text, textLoc);

    return textUpdatedObj? {drawData: {data: [textUpdatedObj]}} : null;
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


/**
 * dispatch action to locate marker with corners and no corner by timer interval on the drawing layer
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
function createMarkerObjs(action, text, textLoc) {
     var {plotId, isHandler, wpt, size, markerStatus, timeoutProcess} = action.payload;

     if (!plotId || !wpt) return null;

     const plot = primePlot(visRoot(), plotId);
     var [markerW, markerH] = isArray(size) && size.length > 1 ?  [size[0], size[1]] : [size, size];
     var markObj = makeMarker(wpt, markerW, markerH, isHandler, plot, text, textLoc);
     var exclusiveDef, vertexDef; // cursor point
     var dlObj =  {
        helpLine: editHelpText,
        drawData: {data: [markObj]},
        currentPt: wpt,                    // center, world or image coordinate
        currentSize: [markerW, markerH]
     };
     if (timeoutProcess) {
         dlObj.timeoutProcess = timeoutProcess;
     }

     if (markerStatus) {
         if (markerStatus === MarkerStatus.attached) {
             exclusiveDef = { exclusiveOnDown: true, type : 'anywhere' };
             vertexDef = {points:null, pointDist:MARKER_DISTANCE};
         } else {
             exclusiveDef = { exclusiveOnDown: true, type : 'vertexOnly' };
             vertexDef = {points:[wpt], pointDist: Math.sqrt(Math.pow(markerW/2, 2) + Math.pow(markerH/2, 2))};
         }
         return Object.assign(dlObj, {markerStatus, vertexDef, exclusiveDef});
     } else {
         return dlObj;
     }
}


