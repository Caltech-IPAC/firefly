/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import DrawLayerCntlr, {dlRoot, dispatchAttachLayerToPlot,
                        dispatchCreateDrawLayer, getDlAry} from '../visualize/DrawLayerCntlr.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef, TextLocation} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {primePlot, getDrawLayerById, getPlotViewIdListInGroup} from '../visualize/PlotViewUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getWorldOrImage, makeMarker, findClosestIndex,  updateFootprintTranslate, updateMarkerSize,
        updateFootprintDrawobjText, updateFootprintOutline,  lengthSizeUnit,
        MARKER_DISTANCE, OutlineType, MarkerType, ROTATE_BOX} from '../visualize/draw/MarkerFootprintObj.js';
import {getMarkerToolUIComponent} from './MarkerToolUI.jsx';
import {getDrawobjArea} from '../visualize/draw/ShapeHighlight.js';
import ShapeDataObj, {lengthToScreenPixel, lengthToArcsec} from '../visualize/draw/ShapeDataObj.js';
import {makeDevicePt, makeImagePt} from '../visualize/Point.js';
import {clone} from '../util/WebUtil.js';
import {get, set, has, isArray} from 'lodash';
import Enum from 'enum';

const editHelpText='Click the marker and drag to move, click corner and drag to resize';
const MARKER_SIZE = 40;      // marker original size in image coordinate (radius of a circle)

const ID= 'OVERLAY_MARKER';
const TYPE_ID= 'OVERLAY_MARKER_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID,creator, null, getLayerChanges,null,getMarkerToolUIComponent);
const MarkerStatus = new Enum(['attached', 'select', 'attached_relocate', 'relocate', 'resize']);

export const markerInterval = 3000; // time interval for showing marker with handlers and no handlers
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

export var cancelTimeoutProcess = (toP) => { if (toP) clearTimeout(toP); };
export var getCC = (plotId) => {
    var plot = primePlot(visRoot(), plotId);
    return CsysConverter.make(plot);
};

export var initMarkerPos = (plot, cc) => {
    var pos = plot.attributes[PlotAttribute.FIXED_TARGET];

    if (!cc) cc = CsysConverter.make(plot);

    if (!pos) {
        pos = makeDevicePt(cc.viewDim.width / 2, cc.viewDim.height / 2);
    }
    return getWorldOrImage(pos, cc);
};

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

            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_CREATE, pId,
                               MarkerStatus.attached, markerInterval,  drawLayerId, {isOutline: true, isResize:true});
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
        const {plotId, imagePt, screenPt, drawLayer} = rawAction.payload;
        const markerObj = get(drawLayer, ['drawData', DataTypes.DATA, plotId]);
        const {currentSize, currentPt, timeoutProcess, markerStatus} = markerObj.actionInfo || {};
        const {drawLayerId} = drawLayer;
        const cc = getCC(plotId);
        var wpt;                         // center for re-render
        var nextStatus = null, idx;
        var refPt;                       // reference for relocate

        cancelTimeoutProcess(timeoutProcess);
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

        if ([MarkerStatus.relocate, MarkerStatus.attached_relocate, MarkerStatus.resize].includes(nextStatus)) {
            refPt = imagePt;                   // refPt is used as the reference point for next relocation
        }

        if (nextStatus) {
            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_START, plotId, nextStatus, markerInterval,
                               drawLayerId, {isOutline: true, isResize:true}, wpt, evenSize(currentSize), refPt);
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
        const {plotId, drawLayer} = rawAction.payload;
        const markerObj = get(drawLayer, ['drawData', DataTypes.DATA, plotId], {});
        const {currentSize, currentPt, timeoutProcess, markerStatus} = get(markerObj, 'actionInfo', {});
        const {includeOutline: isOutline, includeResize: isResize} = markerObj;
        const {drawLayerId} = drawLayer;

        cancelTimeoutProcess(timeoutProcess);

        // marker stays at current position and size
        if ([MarkerStatus.relocate, MarkerStatus.attached_relocate, MarkerStatus.resize].includes(markerStatus)) {
            var wpt = getWorldOrImage(currentPt, getCC(plotId));

            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_END, plotId,
                               MarkerStatus.select, markerInterval, drawLayerId, {isOutline, isResize}, wpt, evenSize(currentSize));
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
        const {plotId, imagePt, drawLayer} = rawAction.payload;
        const markerObj = get(drawLayer, ['drawData', DataTypes.DATA, plotId], {});
        const {drawLayerId} = drawLayer;
        const cc = getCC(plotId);
        var {markerStatus, currentSize: newSize, currentPt: wpt, timeoutProcess, refPt} = get(markerObj, 'actionInfo', {});
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
        } else if (markerStatus === MarkerStatus.relocate || markerStatus === MarkerStatus.attached_relocate) {
            // marker move to new mouse down positon
            var dx, dy;

            if (refPt) {
                dx = imagePt.x - refPt.x;
                dy = imagePt.y - refPt.y;
                refPt = imagePt;
                wpt = getWorldOrImage(makeImagePt(imageCenter.x + dx, imageCenter.y + dy), cc);

                dx = lengthSizeUnit(cc, dx, ShapeDataObj.UnitType.IMAGE_PIXEL);
                dy = lengthSizeUnit(cc, dy, ShapeDataObj.UnitType.IMAGE_PIXEL);
                move.apt = {x: dx.len, y: dy.len, type: dx.unit};

                markerStatus = MarkerStatus.relocate;
                isHandle = {isOutline: true, isResize: true};
            }
        }

        // resize (newDize) or relocate (wpt),  status remains the same
        showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_MOVE, plotId,
                           markerStatus, 0, drawLayerId, isHandle, wpt, newSize, refPt, move);
    };
}


/**
 * create drawing layer for a new marker
 * @param initPayload
 * @returns {Function}
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
        hasPerPlotData: true,
        destroyWhenAllDetached: true
    };

    var title = get(initPayload, 'Title', 'Marker Tool');
    var dl = DrawLayer.makeDrawLayer( get(initPayload, 'drawLayerId', `${ID}-${idCnt}`),
                                    TYPE_ID, title,
                                    options, drawingDef, actionTypes, pairs, exclusiveDef, getCursor);
    dl.defaultTitle = title;
    return dl;
}


/**
 * reducer for MarkerTool layer
 * @param drawLayer
 * @param action
 * @returns {*}
 */
function getLayerChanges(drawLayer, action) {
    const {drawLayerId, plotId} = action.payload;
    if (!drawLayerId || drawLayerId !== drawLayer.drawLayerId) return null;

    const dd = Object.assign({}, drawLayer.drawData);
    var   plotIdAry;
    var   {wpt, size} = action.payload;
    var   retV = null;

    switch (action.type) {
        case DrawLayerCntlr.MARKER_CREATE:
            plotIdAry = getPlotViewIdListInGroup(visRoot(), plotId);
            var ccPlotId = CsysConverter.make(primePlot(visRoot(), plotId));
            var sizePlot = lengthToArcsec(MARKER_SIZE, ccPlotId, ShapeDataObj.UnitType.PIXEL);

            plotIdAry.forEach((pId) => {
                const plot = primePlot(visRoot(), pId);
                const cc = CsysConverter.make(plot);
                wpt = initMarkerPos(plot, cc);

                retV = createMarkerObjs(action, drawLayer, pId, wpt, sizePlot, retV);
            });

            return retV;

        case DrawLayerCntlr.MARKER_START:
        case DrawLayerCntlr.MARKER_MOVE:
        case DrawLayerCntlr.MARKER_END:
            var wptObj;
            plotIdAry = getPlotViewIdListInGroup(visRoot(), plotId);
            plotIdAry.forEach((pId) => {
                wptObj = (pId === plotId) ? wpt : get(dd, ['data', pId, 'pts', '0']);
                retV = createMarkerObjs(action, drawLayer, pId, wptObj, size, retV);
            });

            return retV;

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {markerText, markerTextLoc, activePlotId} = action.payload.changes;
            plotIdAry = getPlotViewIdListInGroup(visRoot(), activePlotId);
            if (plotIdAry) {
                return updateMarkerText(markerText, markerTextLoc, dd[DataTypes.DATA], plotIdAry);
            }
            break;
        default:
            return retV;

    }
    return retV;
}

const cornerCursor = ['pointer', 'pointer', 'nwse-resize', 'nesw-resize', 'nwse-resize', 'nesw-resize'];


function getCursor(plotView, screenPt) {
    var dlAry = dlRoot().drawLayerAry.filter( (dl) => {
        return (dl.drawLayerTypeId === TYPE_ID) && (get(dl, 'visiblePlotIdAry').includes(plotView.plotId));
    });
    var   cursor = '';
    const cc= CsysConverter.make(primePlot(plotView));

    dlAry.find( (dl) => {
        var drawObj = get(dl, ['drawData', 'data', plotView.plotId]);
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
 * @param plotIdAry
 * @returns {*}
 */
export function updateMarkerText(text, textLoc, markerDrawObj, plotIdAry) {

    plotIdAry.forEach((pId) => {
        var textUpdatedObj = updateFootprintDrawobjText(markerDrawObj[pId], text, textLoc);

        if (textUpdatedObj) {
            markerDrawObj[pId] = textUpdatedObj;
        }
    });

    return {drawData: {data: markerDrawObj}};
}

/**
 * make rectangle with the same width and height
 * @param size
 * @returns {Array}
 */
function evenSize(size = 0) {
    var s = (isArray(size) && size.length > 1) ? Math.min(size[0], size[1]): size;

    return [s, s];
}


/**
 * dispatch action to locate marker with corners and no corner by timer interval on the drawing layer
 * @param dispatcher
 * @param actionType
 * @param plotId
 * @param doneStatus
 * @param timer  milliseconds
 * @param drawLayerId
 * @param isHandle
 * @param wpt    marker location world coordinatee
 * @param size   in screen coordinate
 * @param refPt
 * @param move
 */
function showMarkersByTimer(dispatcher, actionType, plotId, doneStatus, timer, drawLayerId, isHandle,
                            wpt, size, refPt, move) {

    var setAction = (isHandle) => ({
        type: actionType,
        payload: {isHandle, plotId, markerStatus: doneStatus, drawLayerId, refPt, move, wpt, size}
    });

    var timeoutProcess = (timer !== 0) && (setTimeout(() => dispatcher(setAction({})), timer));
    var crtAction = set(setAction(isHandle), 'payload.timeoutProcess', timeoutProcess);
    dispatcher(crtAction);
}

/**
 * calculate vertex distance for catching the marker
 * @param markObj
 * @param plotId
 * @param dl
 * @param dlObj
 * @returns {{vertexDef: *, exclusiveDef: *}}
 */
export function updateVertexInfo(markObj, plotId, dl, dlObj) {
    var markerStatus = markObj.sType === MarkerType.Marker ? get(markObj, ['actionInfo', 'markerStatus']) :
                                                             get(markObj, ['actionInfo', 'footprintStatus']);
    var exclusiveDef, vertexDef;

    if (markerStatus) {
        if (markerStatus.key === MarkerStatus.attached.key) {
            exclusiveDef = {exclusiveOnDown: true, type: 'anywhere'};
            vertexDef = {points: null, pointDist: MARKER_DISTANCE};
        } else {
            var cc = CsysConverter.make(primePlot(visRoot(), plotId));
            const {dist, centerPt} = getVertexDistance(markObj, cc);
            var   {vertexDef, exclusiveDef} = dlObj || {};
            var   points = get(vertexDef, ['points'], []);

            exclusiveDef = {exclusiveOnDown: true, type: 'vertexOnly'};
            var idx = dl.plotIdAry ? dl.plotIdAry.findIndex((p) => p === plotId) : -1;
            if (idx >= 0) {
                points[idx] = centerPt;
                vertexDef = {points, pointDist: dist};
            }
        }
        return {vertexDef, exclusiveDef};
    }
    return {};
}

/**
 * create drawlayer object containing only the properties which has updated value
 * @param action
 * @param dl
 * @param plotId
 * @param wpt
 * @param size
 * @param prevRet previous return object
 * @returns {*}
 */
function createMarkerObjs(action, dl, plotId, wpt, size, prevRet) {
    if (!plotId || !wpt) return null;

    const {isHandle, markerStatus, timeoutProcess, move, refPt} = action.payload;
    const crtMarkerObj = get(dl, ['drawData', DataTypes.DATA, plotId], {});
    var   {text = ''} = crtMarkerObj;
    const {textLoc = TextLocation.REGION_SE} = crtMarkerObj;

    if (markerStatus === MarkerStatus.attached ||
        markerStatus === MarkerStatus.attached_relocate) {
        text = get(dl, 'title', '');    // title is the default text by the footprint
    }

    const cc = CsysConverter.make(primePlot(visRoot(), plotId));
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
                wpt = get(markObj, ['pts', '0']);
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

    const actionInfo = {currentPt: wpt,       // marker center, world or image coordinate
                        currentSize: [markerW, markerH],
                        timeoutProcess: timeoutProcess ? timeoutProcess : null,
                        refPt: refPt ? refPt : null,
                        markerStatus};

    set(dl.drawData, [DataTypes.DATA, plotId], Object.assign(markObj, {actionInfo}));
    var dlObj = {drawData: dl.drawData, helpLine: editHelpText};

    if (markerStatus) {
        var {exclusiveDef, vertexDef} = updateVertexInfo(markObj, plotId, dl, prevRet);

        if (exclusiveDef && vertexDef) {
            return clone(dlObj, {markerStatus, vertexDef, exclusiveDef});
        }
    }

    return dlObj;
}

/**
 * compute the radius distance of the footprint in terms of screen pixel for vertex search
 * @param markObj
 * @param cc
 * @returns {{dist: (number|*), centerPt: *}}
 */
export function getVertexDistance( markObj, cc) {
    var {drawObjAry, outlineIndex} = markObj;
    var dist, w, h, centerPt;

    if (outlineIndex && drawObjAry.length > outlineIndex &&
        drawObjAry[outlineIndex].outlineType === OutlineType.original) {
        var outlineObj = Object.assign({}, drawObjAry[outlineIndex]);
        var {width, height, center} = getDrawobjArea(outlineObj, cc);

        w = lengthToScreenPixel(width, cc, ShapeDataObj.UnitType.IMAGE_PIXEL) / 2;
        h = lengthToScreenPixel(height, cc, ShapeDataObj.UnitType.IMAGE_PIXEL) / 2;
        centerPt = getWorldOrImage(center, cc);
        dist = markObj.sType === MarkerType.Marker ? 0 : ROTATE_BOX;
    } else {
        w = cc.viewDim.width/2;
        h = cc.viewDim.height/2;
        centerPt = getWorldOrImage(makeDevicePt(w, h), cc);
        dist = 0;
    }
    dist += Math.sqrt(Math.pow(w, 2) + Math.pow(h, 2));

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

