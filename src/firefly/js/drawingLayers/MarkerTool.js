/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, set, isArray, isEmpty} from 'lodash';
import DrawLayerCntlr, {dlRoot, dispatchAttachLayerToPlot,
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
import ShapeDataObj, {lengthToScreenPixel, lengthToArcsec} from '../visualize/draw/ShapeDataObj.js';
import {clone} from '../util/WebUtil.js';
import Enum from 'enum';


const editHelpText='Click center and drage to move, click corner and drage to resize';

const MARKER_SIZE = 40;      // marker original size in image coordinate (radius of a circle)
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
                var cc = CsysConverter.make(plot);
                var wpt = plot.attributes[PlotAttribute.FIXED_TARGET];
                var size = lengthToArcsec(MARKER_SIZE, cc, ShapeDataObj.UnitType.SCREEN_PIXEL);
                showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_CREATE, [size, size], wpt, pId,
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
        var nextStatus = null, idx;
        var refPt;

        cancelTimeoutProcess(timeoutProcess);

        // marker can move to anywhere the mouse click at while in 'attached' state
        if (markerStatus === MarkerStatus.attached) {
            idx = findClosestIndex(screenPt, get(drawLayer, ['drawData', 'data', '0']), cc);

            if (idx > 1) {
                wpt = getWorldOrImage(currentPt, cc);
                nextStatus = MarkerStatus.resize;
            } else {
                wpt = getWorldOrImage(imagePt, cc);
                nextStatus = MarkerStatus.relocate;
            }
        } else if (markerStatus === MarkerStatus.select) {
            idx = findClosestIndex(screenPt, get(drawLayer, ['drawData', 'data', '0']), cc);

            nextStatus = idx === 0 ? MarkerStatus.relocate : MarkerStatus.resize;
            wpt = getWorldOrImage(currentPt, cc);
        }
        if (nextStatus === MarkerStatus.relocate) {
            refPt = imagePt;
        }
        if (nextStatus) {
            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_START, evenSize(currentSize), wpt, plotId,
                                nextStatus, markerInterval, drawLayerId, refPt);
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
        var {plotId, imagePt, drawLayer} = rawAction.payload;
        var cc = getCC(plotId);
        var {markerStatus, currentSize: newSize, currentPt: wpt, timeoutProcess, drawLayerId, refPt} = drawLayer;
        var imageCenter = cc.getImageCoords(wpt);

        cancelTimeoutProcess(timeoutProcess);

        if (markerStatus === MarkerStatus.resize)  {               // mmarker stay at current point, and change size
            var imgUnit = ShapeDataObj.UnitType.IMAGE_PIXEL;

            newSize = [lengthToArcsec(Math.abs(imagePt.x - imageCenter.x)*2, cc, imgUnit),
                       lengthToArcsec(Math.abs(imagePt.y - imageCenter.y)*2, cc, imgUnit)];

        } else if (markerStatus === MarkerStatus.relocate) {      // marker move to new mouse down positon
            var dx, dy;

            if (refPt) {
                dx = imagePt.x - refPt.x;
                dy = imagePt.y - refPt.y;
                imageCenter.x += dx;
                imageCenter.y += dy;
                refPt = imagePt;
                wpt = getWorldOrImage(imageCenter, cc);
            }
        }
        // resize (newDize) or relocate (wpt),  status remains the same
        showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_MOVE, newSize, wpt, plotId,
                           markerStatus, 0, drawLayerId, refPt);
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
                                    options, drawingDef, actionTypes, pairs, exclusiveDef, getCursor);
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

const cornerCursor = ['pointer', 'nw-resize', 'ne-resize', 'se-resize', 'sw-resize'];

function getCursor(plotView, screenPt) {
    var dlAry = dlRoot().drawLayerAry.filter( (dl) => {
        return (dl.drawLayerTypeId === TYPE_ID) && (get(dl, 'plotIdAry').includes(plotView.plotId));
    });
    const plot= primePlot(plotView);
    var   cursor = '';
    var   cc= CsysConverter.make(plot);

    dlAry.find( (dl) => {
        var drawObj = get(dl, ['drawData', 'data', '0']);
        var idx = findClosestIndex(screenPt, drawObj, cc);

        if (idx >= 0 && idx <= cornerCursor.length) {
            cursor = cornerCursor[idx];
            return true;
        } else {
            return false;
        }
    });
    return cursor;
}

/**
 * update text in marker object
 * @param text
 * @param textLoc
 * @param markerDrawObj
 * @returns {*}
 */
function updateMarkerText(text, textLoc, markerDrawObj) {
    var textUpdatedObj = updateMarkerDrawObjText(markerDrawObj[0], text, textLoc);

    return textUpdatedObj? {drawData: {data: [textUpdatedObj]}} : {};
}

/**
 * make rectangle with the same width and height
 * @param size
 * @returns {*[]}
 */
function evenSize(size = 0) {
    var s = (isArray(size) && size.length > 1) ? Math.min(size[0], size[1]): size;

    return [s, s];
}


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
function showMarkersByTimer(dispatcher, actionType, size, wpt, plotId,  doneStatus, timer, drawLayerId, refPt) {
    var setAction = (isHandler) => ({
        type: actionType,
        payload: {isHandler, size, wpt, plotId, markerStatus: doneStatus, drawLayerId, refPt}
    });

    var timeoutProcess = (timer !== 0) && (setTimeout(() => dispatcher(setAction(false)), timer));
    var crtAction = set(setAction(true), 'payload.timeoutProcess', timeoutProcess);
    dispatcher(crtAction);
}

/**
 * create drawlayer object containing only the properties which has updated value
 * @param action
 * @param text
 * @param textLoc
 * @returns {*}
 */
function createMarkerObjs(action, text, textLoc) {
     var {plotId, isHandler, wpt, size, markerStatus, timeoutProcess, refPt} = action.payload;

     if (!plotId || !wpt) return null;

     const unitT = ShapeDataObj.UnitType.ARCSEC;
     const plot = primePlot(visRoot(), plotId);
     var [markerW, markerH] = isArray(size) && size.length > 1 ?  [size[0], size[1]] : [size, size];
     var markObj = makeMarker(wpt, markerW, markerH, isHandler, plot, text, textLoc, unitT);
     var exclusiveDef, vertexDef; // cursor point
     var dlObj =  {
        helpLine: editHelpText,
        drawData: {data: [markObj]},
        currentPt: wpt,                    // marker center, world or image coordinate
        currentSize: [markerW, markerH]
     };
     dlObj = clone(dlObj, {timeoutProcess: timeoutProcess ? timeoutProcess : null,
                           refPt: refPt ? refPt : null});    // reference pt for next action

     if (markerStatus) {
         if (markerStatus === MarkerStatus.attached) {
             exclusiveDef = { exclusiveOnDown: true, type : 'anywhere' };
             vertexDef = {points:null, pointDist:MARKER_DISTANCE};
         } else {
             const cc = CsysConverter.make(plot);
             exclusiveDef = { exclusiveOnDown: true, type : 'vertexOnly' };
             vertexDef = {points:[wpt],
                          pointDist: Math.sqrt(Math.pow(lengthToScreenPixel(markerW/2, cc, unitT), 2) +
                                               Math.pow(lengthToScreenPixel(markerH/2, cc, unitT), 2))};
         }
         return clone(dlObj, {markerStatus, vertexDef, exclusiveDef});
     } else {
         return dlObj;
     }
}


