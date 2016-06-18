/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
import {makeMarker, findClosestIndex,  updateFootprintTranslate, updateMarkerSize,
        updateMarkerDrawObjText, updateFootprintOutline,
        MARKER_DISTANCE, OutlineType} from '../visualize/draw/MarkerFootprintObj.js';
import {getMarkerToolUIComponent} from './MarkerToolUI.jsx';
import {getDrawobjArea} from '../visualize/draw/ShapeHighlight.js';
import ShapeDataObj, {lengthToScreenPixel, lengthToArcsec} from '../visualize/draw/ShapeDataObj.js';
import {makeViewPortPt, makeImagePt} from '../visualize/Point.js';
import {clone} from '../util/WebUtil.js';
import {get, set, has, isArray} from 'lodash';
import Enum from 'enum';

const editHelpText='Click the marker and drag to move, click corner and drag to resize';
const MARKER_SIZE = 40;      // marker original size in image coordinate (radius of a circle)

const ID= 'OVERLAY_MARKER';
const TYPE_ID= 'OVERLAY_MARKER_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,null,getMarkerToolUIComponent);
const MarkerStatus = new Enum(['attached', 'select', 'attached_relocate', 'relocate', 'resize']);

export const markerInterval = 3000; // time interval for showing marker with handlers and no handlers
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

export var cancelTimeoutProcess = (toP) => { if (toP) clearTimeout(toP); };
export var getCC = (plotId) => {
    var plot = primePlot(visRoot(), plotId);
    return CsysConverter.make(plot);
};

export var getWorldOrImage = (pt, cc) => (isWorld(cc) ? cc.getWorldCoords(pt) : cc.getImageCoords(pt));
var isWorld = (cc) => (cc.projection.isSpecified());

var idCnt=0;

export function markerToolCreateLayerActionCreator(rawAction) {
    return (dispatcher) => {
        var {plotId,
             markerId: drawLayerId,
             layerTitle:Title,
             attachPlotGroup} = rawAction.payload;
        var dl = getDrawLayerById(getDlAry(), drawLayerId);

        if (!dl) {
            dispatchCreateDrawLayer(TYPE_ID, {Title, drawLayerId});
        }

        // plotId could be an array or single value
        var pId = (!plotId || (isArray(plotId)&&plotId.length === 0)) ?
                   get(visRoot(), 'activePlotId') : isArray(plotId) ? plotId[0] : plotId;

        if (pId) {
            dispatchAttachLayerToPlot(drawLayerId, pId, attachPlotGroup);

            var plot = primePlot(visRoot(), pId);
            if (plot) {
                var cc = CsysConverter.make(plot);
                var wpt = plot.attributes[PlotAttribute.FIXED_TARGET];
                var size = lengthToArcsec(MARKER_SIZE, cc, ShapeDataObj.UnitType.PIXEL);

                showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_CREATE, [size, size], wpt, pId,
                                MarkerStatus.attached, markerInterval,  drawLayerId, {isOutline: true, isResize:true});

            }
        }
    };
}


/**
 * action creator for MARKER_START, only when the cursor shows 'pointer"
 * @param rawAction
 * @returns {Function}
 */
export function markerToolStartActionCreator(rawAction) {
    return (dispatcher) => {
        var {plotId, imagePt, screenPt, drawLayer} = rawAction.payload;
        var cc = getCC(plotId);
        var wpt;
        var {markerStatus, currentSize, currentPt, timeoutProcess, drawLayerId} = drawLayer;
        var nextStatus = null, idx;
        var refPt;

        cancelTimeoutProcess(timeoutProcess);
        var markerObj = get(drawLayer, ['drawData', 'data', '0']);

        // marker can move to anywhere the mouse is clicked at while in 'attached' state
        if (markerStatus === MarkerStatus.attached) {
            if (markerObj) {
                idx = findClosestIndex(screenPt, markerObj, cc).index;

                if (has(markerObj, 'resizeIndex') && idx === markerObj.resizeIndex) {
                    wpt = getWorldOrImage(currentPt, cc);
                    nextStatus = MarkerStatus.resize;
                } else {
                    wpt = getWorldOrImage(imagePt, cc);
                    nextStatus = MarkerStatus.attached_relocate;
                }
            }
        } else if (markerStatus === MarkerStatus.select) { // marker to be moved or resized from where it is located
            if (markerObj) {
                idx = findClosestIndex(screenPt, markerObj, cc).index;
                if (has(markerObj, 'resizeIndex') &&
                    (idx >=  markerObj.resizeIndex && idx <= markerObj.resizeIndex + 3)) {
                    nextStatus = MarkerStatus.resize;
                } else if (idx >= 0 && idx <= markerObj.outlineIndex) {
                    nextStatus = MarkerStatus.relocate;
                }
                wpt = getWorldOrImage(currentPt, cc);
            }
        }
        if ([MarkerStatus.relocate, MarkerStatus.attached_relocate].includes(nextStatus)) {
            refPt = imagePt;                   // refPt is used as the reference point for next relocation
        }
        if (nextStatus) {
            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_START, evenSize(currentSize), wpt, plotId,
                                nextStatus, markerInterval, drawLayerId, {isOutline: true, isResize:true}, refPt);
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
        var {plotId, drawLayer} = rawAction.payload;
        var cc = getCC(plotId);
        var {markerStatus, currentSize, currentPt, timeoutProcess, drawLayerId} = drawLayer;
        var {includeOutline: isOutline, includeResize: isResize } = get(drawLayer, ['drawData', DataTypes.DATA, '0'], {});

        cancelTimeoutProcess(timeoutProcess);

        // marker stays at current position and size
        if ([MarkerStatus.relocate, MarkerStatus.attached_relocate, MarkerStatus.resize].includes(markerStatus)) {
            var wpt = getWorldOrImage(currentPt, cc);

            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_END, evenSize(currentSize), wpt, plotId,
                               MarkerStatus.select, markerInterval, drawLayerId, {isOutline, isResize});
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
        var {plotId, imagePt, drawLayer} = rawAction.payload;
        var cc = getCC(plotId);
        var {markerStatus, currentSize: newSize, currentPt: wpt, timeoutProcess, drawLayerId, refPt} = drawLayer;
        var imageCenter = cc.getImageCoords(wpt);
        var move = {};
        var isHandle;

        cancelTimeoutProcess(timeoutProcess);

        if (markerStatus === MarkerStatus.resize)  {               // mmarker stay at current point, and change size
            var imgUnit = ShapeDataObj.UnitType.IMAGE_PIXEL;

            newSize = [lengthToArcsec(Math.abs(imagePt.x - imageCenter.x)*2, cc, imgUnit),
                       lengthToArcsec(Math.abs(imagePt.y - imageCenter.y)*2, cc, imgUnit)];
            move.newSize = {size: newSize, unitType: ShapeDataObj.UnitType.ARCSEC};   // in world size
            isHandle = { isOutline: true, isResize: true};

        } else if (markerStatus === MarkerStatus.relocate) {      // marker move to new mouse down positon
            var dx, dy;

            if (refPt) {
                dx = imagePt.x - refPt.x;
                dy = imagePt.y - refPt.y;
                refPt = imagePt;
                wpt = getWorldOrImage(makeImagePt(imageCenter.x + dx, imageCenter.y + dy), cc);
                move.apt = {x: dx, y: dy, type: ShapeDataObj.UnitType.IMAGE_PIXEL};
                markerStatus = MarkerStatus.relocate;
                isHandle = {isOutline: true};
            }
        }
        // resize (newDize) or relocate (wpt),  status remains the same
        showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_MOVE, newSize, wpt, plotId,
                           markerStatus, 0, drawLayerId, isHandle, refPt, move);
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
            var data = get(dd, [DataTypes.DATA, '0']);
            var {text, textLoc} = data || {};
            var crtMarkerObj = data || null;

            return createMarkerObjs(action, text, textLoc, crtMarkerObj);

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            var {markerText, markerTextLoc} = action.payload.changes;

            return updateMarkerText(markerText, markerTextLoc, dd[DataTypes.DATA]);
    }
}

const cornerCursor = ['pointer', 'pointer', 'nwse-resize', 'nesw-resize', 'nwse-resize', 'nesw-resize'];

function getCursor(plotView, screenPt) {
    var dlAry = dlRoot().drawLayerAry.filter( (dl) => {
        return (dl.drawLayerTypeId === TYPE_ID) && (get(dl, 'visiblePlotIdAry').includes(plotView.plotId));
    });
    const plot= primePlot(plotView);
    var   cursor = '';
    var   cc= CsysConverter.make(plot);

    dlAry.find( (dl) => {
        var drawObj = get(dl, ['drawData', 'data', '0']);
        var idx = findClosestIndex(screenPt, drawObj, cc).index;

        if (idx >= 0 && idx < cornerCursor.length) {
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
 * @param isHandle
 * @param refPt
 * @param move
 */
function showMarkersByTimer(dispatcher, actionType, size, wpt, plotId,  doneStatus, timer, drawLayerId, isHandle,
                            refPt, move) {
    var setAction = (isHandle) => ({
        type: actionType,
        payload: {isHandle, size, wpt, plotId, markerStatus: doneStatus, drawLayerId, refPt, move}
    });

    var timeoutProcess = (timer !== 0) && (setTimeout(() => dispatcher(setAction({})), timer));
    var crtAction = set(setAction(isHandle), 'payload.timeoutProcess', timeoutProcess);
    dispatcher(crtAction);
}

/**
 * create drawlayer object containing only the properties which has updated value
 * @param action
 * @param text
 * @param textLoc
 * @param crtMarkerObj current marker drawObj
 * @returns {*}
 */
function createMarkerObjs(action, text, textLoc, crtMarkerObj) {
     var {plotId, isHandle, wpt, size, markerStatus, timeoutProcess, refPt, move} = action.payload;

     if (!plotId || !wpt) return null;

     const plot = primePlot(visRoot(), plotId);
     const cc = CsysConverter.make(plot);
     var [markerW, markerH] = isArray(size) && size.length > 1 ?  [size[0], size[1]] : [size, size];
     var unitT = ShapeDataObj.UnitType.ARCSEC;
     var markObj;

    if (markerStatus === MarkerStatus.attached ||
        markerStatus === MarkerStatus.attached_relocate) {  // position is reloacated after the layer is attached
        markObj = makeMarker(wpt, markerW, markerH, isHandle, cc, text, textLoc, unitT);
    } else if (crtMarkerObj) {
        if ((markerStatus === MarkerStatus.resize || markerStatus === MarkerStatus.relocate) && move) {
            var {apt, newSize} = move;    // move to relocate or resize

            if (apt) {      // translate
                markObj = updateFootprintTranslate(crtMarkerObj, cc, apt);
            } else if (newSize) {
                markObj = updateMarkerSize(crtMarkerObj, cc, newSize);
            } else {
                markObj = Object.assign({}, crtMarkerObj);
            }
        } else {            // start to move or resize (mouse down) or end the operation (mouse up)
            if (markerStatus !== MarkerStatus.select) {
                            // update the outlinebox when the target starts to move or resize
                markObj = updateFootprintOutline(crtMarkerObj, cc);
            } else {
                markObj = Object.assign({}, crtMarkerObj);
            }
        }
        updateHandle(isHandle, markObj); // handle display changes - resize handle disappears during translation
    }

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

             var {dist, centerPt} = getVertexDistance( markObj, cc);

             exclusiveDef = { exclusiveOnDown: true, type : 'vertexOnly' };
             vertexDef = {points:[centerPt],
                          pointDist: dist};
         }
         return clone(dlObj, {markerStatus, vertexDef, exclusiveDef});
     } else {
         return dlObj;
     }
}

/**
 * compute the radius distance of the footprint in terms of screen pixel for vertex search
 * @param markObj
 * @param cc
 * @returns {{dist: (number|*), centerPt: *}}
 */
function getVertexDistance( markObj, cc) {
    var {drawObjAry, outlineIndex} = markObj;
    var dist, w, h, centerPt;

    if (outlineIndex && drawObjAry.length > outlineIndex &&
        drawObjAry[outlineIndex].outlineType === OutlineType.original) {
        var outlineObj = Object.assign({}, drawObjAry[outlineIndex]);
        var {width, height, center} = getDrawobjArea(outlineObj, cc);

        w = lengthToScreenPixel(width, cc, ShapeDataObj.UnitType.IMAGE_PIXEL) / 2;
        h = lengthToScreenPixel(height, cc, ShapeDataObj.UnitType.IMAGE_PIXEL) / 2;
        centerPt = getWorldOrImage(center, cc);
    } else {
        w = cc.viewPort.dim.width/2;
        h = cc.viewPort.dim.height/2;
        centerPt = getWorldOrImage(makeViewPortPt(w, h), cc);
    }
    dist = Math.sqrt(Math.pow(w, 2) + Math.pow(h, 2));

    return {dist, centerPt};
}

/**
 * update the handle inclusion on the object
 * @param isHandle
 * @param markerObj
 */
function updateHandle(isHandle, markerObj) {
    var {isResize, isOutline} = isHandle || {};

    markerObj.includeResize = isResize;
    markerObj.includeOutline = isOutline;
}

