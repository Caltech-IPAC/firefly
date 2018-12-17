/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import DrawLayerCntlr, {dlRoot, dispatchAttachLayerToPlot,
                        dispatchCreateDrawLayer, getDlAry} from '../visualize/DrawLayerCntlr.js';
import {visRoot, dispatchChangeCenterOfProjection} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef, TextLocation} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {primePlot, getDrawLayerById, getCenterOfProjection} from '../visualize/PlotViewUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getWorldOrImage, makeMarker, findClosestIndex,  updateFootprintTranslate, updateMarkerSize,
        updateFootprintDrawobjText, updateFootprintOutline,  lengthSizeUnit, isOutlineBoxOriginal,
        MARKER_DISTANCE, MarkerType, ROTATE_BOX, MARKER_SIZE} from '../visualize/draw/MarkerFootprintObj.js';
import {getMarkerToolUIComponent} from './MarkerToolUI.jsx';
import {getDrawobjArea} from '../visualize/draw/ShapeHighlight.js';
import ShapeDataObj, {lengthToScreenPixel, lengthToArcsec} from '../visualize/draw/ShapeDataObj.js';
import {makeDevicePt, makeImagePt} from '../visualize/Point.js';
import {clone} from '../util/WebUtil.js';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import {isHiPS} from '../visualize/WebPlot.js';
import {get, set, has, isArray, isEmpty} from 'lodash';
import Enum from 'enum';

const editHelpText='Click the marker and drag to move, click corner and drag to resize';


const ID= 'OVERLAY_MARKER';
const TYPE_ID= 'OVERLAY_MARKER_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID,creator, null, getLayerChanges,null,getMarkerToolUIComponent);
const MarkerStatus = new Enum(['attached', 'select', 'attached_relocate', 'relocate', 'resize']);

export const markerInterval = 3000; // time interval for showing marker with handlers and no handlers
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

export var cancelTimeoutProcess = (toP) => { if (toP) clearTimeout(toP); };
export const getPlot = (pId) => ( primePlot(visRoot(), pId) );
export var getCC = (plotId) => {
    var plot = primePlot(visRoot(), plotId);
    return CsysConverter.make(plot);
};
export const isGoodPlot = (pId) => (Boolean(getPlot(pId)));

export var initMarkerPos = (plot, cc) => {
    var pos = plot.attributes[PlotAttribute.FIXED_TARGET];

    if (!cc) cc = CsysConverter.make(plot);

    if (!pos || !cc.pointInView(getWorldOrImage(pos, cc))) {
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
        const {currentSize, currentPt, timeoutProcess, markerStatus, unitT} = markerObj.actionInfo || {};
        const {drawLayerId} = drawLayer;
        const cc = getCC(plotId);
        var wpt;                         // center for re-render
        var nextStatus = null, idx;
        var refPt;                       // reference for relocate
        let move;

        cancelTimeoutProcess(timeoutProcess);
        // marker can move to anywhere the mouse is clicked at while in 'attached' state
        if (markerStatus === MarkerStatus.attached) {
            if (markerObj) {
                idx = findClosestIndex(screenPt, markerObj, cc).index;

                wpt = getWorldOrImage(currentPt, cc);
                if (has(markerObj, 'resizeIndex') &&
                    (idx >= markerObj.resizeIndex && idx <= markerObj.resizeIndex + 3)) {
                    nextStatus = MarkerStatus.resize;
                } else {
                    move = getMovement(currentPt, imagePt, cc);  // calculate the move to move other plot

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

        if (nextStatus && isPointViewable(cc, refPt)) {
            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_START, plotId, nextStatus, markerInterval,
                               drawLayerId, {isOutline: true, isResize:true}, wpt, evenSize(currentSize), unitT, refPt, move);
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
        const {currentSize, currentPt, timeoutProcess, markerStatus, unitT} = get(markerObj, 'actionInfo', {});
        const {includeOutline: isOutline, includeResize: isResize} = markerObj;
        const {drawLayerId} = drawLayer;

        cancelTimeoutProcess(timeoutProcess);

        // marker stays at current position and size
        if ([MarkerStatus.relocate, MarkerStatus.attached_relocate, MarkerStatus.resize].includes(markerStatus)) {
            var wpt = getWorldOrImage(currentPt, getCC(plotId));

            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_END, plotId,
                               MarkerStatus.select, markerInterval, drawLayerId, {isOutline, isResize}, wpt, evenSize(currentSize), unitT);
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
        var {markerStatus, currentSize: newSize, currentPt: wpt, timeoutProcess, refPt, unitT} = get(markerObj, 'actionInfo', {});
        var imageCenter = cc.getImageCoords(wpt);
        var move = {};
        var isHandle;
        const IMGUnit = ShapeDataObj.UnitType.IMAGE_PIXEL;

        cancelTimeoutProcess(timeoutProcess);

        if (markerStatus === MarkerStatus.resize)  {               // mmarker stay at current point, and change size
            if (!cc.pointInView(getWorldOrImage(imagePt, cc))) return;               // resize stops

            newSize = [lengthToArcsec(Math.abs(imagePt.x - imageCenter.x)*2, cc, IMGUnit),
                       lengthToArcsec(Math.abs(imagePt.y - imageCenter.y)*2, cc, IMGUnit)];

            if (newSize[0] < 2) newSize[0]=2;
            if (newSize[1] < 2) newSize[1]=2;
            move.newSize = {size: newSize, unitType: ShapeDataObj.UnitType.ARCSEC};   // in world size
            isHandle = { isOutline: true, isResize: true};
        } else if (markerStatus === MarkerStatus.relocate || markerStatus === MarkerStatus.attached_relocate) {
            // marker move to new mouse down positon
            var dx, dy;

            if (refPt) {
                const prePt = cc.getImageCoords(refPt);

                dx = imagePt.x - prePt.x;
                dy = imagePt.y - prePt.y;
                wpt = getWorldOrImage(makeImagePt(imageCenter.x + dx, imageCenter.y + dy), cc);

                if (!cc.pointInView(wpt)) {  // HiPS plot, wpt is out of range, no move
                    dx = 0;
                    dy = 0;
                    wpt = getWorldOrImage(makeImagePt(imageCenter.x, imageCenter.y), cc);
                } else {
                    refPt = imagePt;
                }

                dx = lengthSizeUnit(cc, dx, IMGUnit);
                dy = lengthSizeUnit(cc, dy, IMGUnit);
                move.apt = {x: dx.len, y: dy.len, type: dx.unit};

                markerStatus = MarkerStatus.relocate;
                isHandle = {isOutline: true, isResize: true};
            }
        }

        // resize (newDize) or relocate (wpt),  status remains the same
        if (!isEmpty(move) && isPointViewable(cc, refPt)) {
            showMarkersByTimer(dispatcher, DrawLayerCntlr.MARKER_MOVE, plotId,
                markerStatus, 0, drawLayerId, isHandle, wpt, newSize, unitT, refPt, move);
        }
    };
}

// markerCenter, outlineCenter, dx, dy in image coordinate
export function rotateHiPSImage(cc, markerCenter, outlineCenter, dx, dy) {
    const findDiff = (center, dx, dy) => {
        const newDevPt = cc.getDeviceCoords(makeImagePt(center.x + dx, center.y + dy));
        const oldDevPt = cc.getDeviceCoords(center);
        const delta = 2;

        const xdiff = Math.sign(newDevPt.x - oldDevPt.x) * delta;
        const ydiff = Math.sign(newDevPt.y - oldDevPt.y) * delta;

        return {xdiff, ydiff};
    };

    const markerDiff = markerCenter ? findDiff(markerCenter, dx, dy) : {xdiff: 0, ydiff: 0};
    const outlineDiff = outlineCenter ? findDiff(outlineCenter, dx, dy) : {xdiff: 0, ydiff: 0};
    const xDelta = Math.abs(markerDiff.xdiff) > Math.abs(outlineDiff.xdiff) ? markerDiff.xdiff
                                                                             : outlineDiff.xdiff;
    const yDelta = Math.abs(markerDiff.ydiff) > Math.abs(outlineDiff.ydiff) ? markerDiff.ydiff
                                                                             : outlineDiff.ydiff;
    const plot= primePlot(visRoot(), cc.plotId);
    const centerOfProj= getCenterOfProjection(plot);
    const originalCenterOfProjDev= cc.getDeviceCoords(centerOfProj);
    const newCenterOfProjDev= makeDevicePt(originalCenterOfProjDev.x+xDelta,
                                           originalCenterOfProjDev.y+yDelta);
    const newWp= cc.getWorldCoords(newCenterOfProjDev, plot.imageCoordSys);

    if (!newWp) return;

    if (newWp.y < -89.7) newWp.y= -89.7;
    if (newWp.y >  89.7) newWp.y=  89.7;

    dispatchChangeCenterOfProjection({plotId: cc.plotId, centerProjPt:newWp});
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
    if (![ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION, ImagePlotCntlr.ANY_REPLOT].includes(action.type) &&
        (!drawLayerId || drawLayerId !== drawLayer.drawLayerId))  {
        return null;
    }

    const dd = Object.assign({}, drawLayer.drawData);
    const {plotIdAry=[]} = drawLayer;
    var   {wpt, size, unitT} = action.payload;
    var   retV = drawLayer;

    switch (action.type) {
        case DrawLayerCntlr.MARKER_CREATE:
            const ccPlotId = getCC(plotId);
            const sizePlot = lengthToArcsec(MARKER_SIZE, ccPlotId, ShapeDataObj.UnitType.PIXEL);

            plotIdAry.forEach((pId) => {
                const plot = getPlot(pId);
                if (plot) {
                    wpt = initMarkerPos(plot);
                    retV = createMarkerObjs(action, drawLayer, pId, wpt, sizePlot, retV, ShapeDataObj.UnitType.ARCSEC);
                }
            });

            return retV;

        case DrawLayerCntlr.MARKER_START:
        case DrawLayerCntlr.MARKER_MOVE:
        case DrawLayerCntlr.MARKER_END:
            var wptObj;

            if (DrawLayerCntlr.MARKER_MOVE === action.type && action.payload.move.newSize) {

                const {newSize} = action.payload.move;
                wptObj = wpt;
                retV = createMarkerObjs(action, drawLayer, plotId, wptObj, size, retV, unitT);
                const newMarker = get(dd, [DataTypes.DATA, plotId, 'drawObjAry', '0']);
                if (newMarker) {
                    const newMarkerRadius = {radius: newMarker.radius, unitType: newMarker.unitType};
                    newSize.newRadius = newMarkerRadius;      // change the radius size per active plot radius change

                    plotIdAry.forEach((pId) => {
                        if (pId !== plotId && isGoodPlot(pId)) {
                            retV = createMarkerObjs(action, drawLayer, pId, get(dd, ['data', pId, 'pts', '0']),
                                                    size, retV, unitT);
                        }
                    });
                }
            } else {
                plotIdAry.forEach((pId) => {
                    if (isGoodPlot(pId)) {
                        wptObj = (pId === plotId) ? wpt : get(dd, ['data', pId, 'pts', '0']);
                        retV = createMarkerObjs(action, drawLayer, pId, wptObj, size, retV, unitT);
                    }
                });
            }

            return retV;

        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            if (!isEmpty(get(drawLayer, ['drawData', 'data']))) {
                 return attachToNewPlot(drawLayer, get(action.payload,['plotIdAry']));
            }
            break;

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {markerText, markerTextLoc} = action.payload.changes;
            if (plotIdAry) {
                return updateMarkerText(markerText, markerTextLoc, dd[DataTypes.DATA], plotIdAry);
            }
            break;

        case ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION:
        case ImagePlotCntlr.ANY_REPLOT:
            if (plotIdAry) {
                plotIdAry.forEach((pId) => {
                    if (isGoodPlot(pId)) {
                        wptObj = get(dd, ['data', pId, 'pts', '0']);
                        const cc = getCC(pId);
                        if (wptObj && cc.pointInView(wptObj)) {
                            retV = createMarkerObjs(action, drawLayer, pId, wptObj, size, retV, unitT);
                        }
                    }
                });
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
        if (isGoodPlot(pId)) {
            const textUpdatedObj = updateFootprintDrawobjText(markerDrawObj[pId], text, textLoc);

            if (textUpdatedObj) {
                markerDrawObj[pId] = textUpdatedObj;
            }
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
 * @param unitT  unit of size
 * @param refPt
 * @param move
 */
function showMarkersByTimer(dispatcher, actionType, plotId, doneStatus, timer, drawLayerId, isHandle,
                            wpt, size, unitT, refPt, move) {

    var setAction = (isHandle) => ({
        type: actionType,
        payload: {isHandle, plotId, markerStatus: doneStatus, drawLayerId, refPt, move, wpt, size, unitT}
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
            vertexDef = {points: {[plotId]: []}, pointDist: {[plotId]: MARKER_DISTANCE}};
        } else {
            var cc = CsysConverter.make(primePlot(visRoot(), plotId));
            const {dist, centerPt} = getVertexDistance(markObj, cc);

            exclusiveDef = {exclusiveOnDown: true, type: 'vertexOnly'};
            vertexDef = dlObj.vertexDef || {};
            const {points={}, pointDist={}}= vertexDef || {};
            points[plotId] = [centerPt];
            pointDist[plotId] = dist;
            vertexDef = {points,  pointDist};
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
 * @param unitT marker size unit
 * @returns {*}
 */
function createMarkerObjs(action, dl, plotId, wpt, size, prevRet, unitT) {
    if (!plotId || !wpt) return null;

    const {isHandle, timeoutProcess, move, refPt} = action.payload;
    let {markerStatus} = action.payload;
    const crtMarkerObj = get(dl, ['drawData', DataTypes.DATA, plotId], {});
    var   {text = ''} = crtMarkerObj;
    const {textLoc = TextLocation.REGION_SE} = crtMarkerObj;

    if (markerStatus === MarkerStatus.attached ||
        markerStatus === MarkerStatus.attached_relocate) {
        text = get(dl, 'title', '');    // title is the default text by the footprint
    }

    const cc = CsysConverter.make(primePlot(visRoot(), plotId));
    var [markerW, markerH] = isArray(size) && size.length > 1 ?  [size[0], size[1]] : [size, size];
    var markObj;

    if (markerStatus === MarkerStatus.attached ||
        markerStatus === MarkerStatus.attached_relocate) {
        markObj = makeMarker(wpt, markerW, markerH, isHandle, cc, text, textLoc, unitT);

        // position is relocated after the layer is attached by the click
        if (markerStatus === MarkerStatus.attached_relocate) {
            markObj = translateForRelocate(markObj,  move, cc);
            wpt = get(markObj, ['pts', '0']);
        }
    } else if (crtMarkerObj) {
        if ((markerStatus === MarkerStatus.resize || markerStatus === MarkerStatus.relocate) && !isEmpty(move)) {
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
                if (ifUpdateOutline(crtMarkerObj, cc)) {
                    markObj = updateFootprintOutline(crtMarkerObj, cc, true);
                } else {
                    markObj = Object.assign({}, crtMarkerObj);
                }
            }
        }
        updateHandle(isHandle, markObj); // handle display changes - resize handle disappears during translation
    }

    if (!isOutlineBoxOriginal(markObj)) {
        const outlineBox = markObj.drawObjAry[markObj.outlineIndex];
        if (outlineBox) {
            wpt = outlineBox.pts[0];
            markerW = lengthToArcsec(outlineBox.width, cc, outlineBox.unitType);
            markerH = lengthToArcsec(outlineBox.height, cc, outlineBox.unitType);
        }
    }

    if ([ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION, ImagePlotCntlr.ANY_REPLOT].includes(action.type) &&
        !markerStatus) {
        markerStatus = get(dl.drawData, [DataTypes.DATA, plotId, 'actionInfo', 'markerStatus'],  MarkerStatus.select);
    }

    const actionInfo = {currentPt: wpt,       // marker center, world or image coordinate
                        currentSize: [markerW, markerH],
                        timeoutProcess: timeoutProcess ? timeoutProcess : null,
                        refPt: refPt ? refPt : null,
                        unitT,
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

export function isPointViewable(cc, pt) {
    return isHiPS(cc) ? cc.pointInView(pt) : cc.pointOnDisplay(pt);
}

export function ifUpdateOutline(markObj, cc) {
    if (isOutlineBoxOriginal(markObj)) {
        const inView = isPointViewable(cc, markObj.drawObjAry[markObj.outlineIndex].pts[0]);

        return !inView;
    } else {
        return true;
    }
}

/**
 * update the location of marker/footprint in sync with the relocation of the marker/footprint in the active plot
 * when it is first formed (the stage that the status is changed from 'attached' to 'attached_relocate')
 * @param markObj marker/footprint object
 * @param move    object with translation info.
 * @param cc
 * @returns {*}
 */
export function translateForRelocate(markObj, move, cc) {
    if (move && has(move, 'apt')) {
        markObj = updateFootprintTranslate(markObj, cc, move.apt);
    }
    return markObj;
}
/**
 * calculate the movement of the marker/footprint object in the active plot
 * @param currentPt current center position of the object
 * @param imagePt location where the object to be moved to on image coordinate
 * @param cc
 * @returns {{}}
 */
export function getMovement(currentPt, imagePt, cc) {
    const prevImg = cc.getImageCoords(currentPt);
    const move = {};

    let dx = imagePt.x - prevImg.x;
    let dy = imagePt.y - prevImg.y;

    dx = lengthSizeUnit(cc, dx, ShapeDataObj.UnitType.IMAGE_PIXEL);
    dy = lengthSizeUnit(cc, dy, ShapeDataObj.UnitType.IMAGE_PIXEL);
    move.apt = {x: dx.len, y: dy.len, type: dx.unit};       // for moving marker in other plot
    return move;
}
/**
 * compute the radius distance of the footprint in terms of screen pixel for vertex search
 * @param markObj
 * @param cc
 * @returns {{dist: (number|*), centerPt: *}}
 */
export function getVertexDistance( markObj, cc) {
    const {drawObjAry, outlineIndex} = markObj;
    let dist, w, h, centerPt;
    let findOutline = false;

    if (!debugOutline(markObj)) {
        var outlineObj = Object.assign({}, drawObjAry[outlineIndex]);
        var {width, height, center} = getDrawobjArea(outlineObj, cc) || {};

        if (center) {
            w = lengthToScreenPixel(width, cc, ShapeDataObj.UnitType.IMAGE_PIXEL) / 2;
            h = lengthToScreenPixel(height, cc, ShapeDataObj.UnitType.IMAGE_PIXEL) / 2;
            centerPt = getWorldOrImage(center, cc);
            dist = markObj.sType === MarkerType.Marker ? 0 : ROTATE_BOX;
            findOutline = true;
        }
    }
    if (!findOutline) {
        w = cc.viewDim.width/2;
        h = cc.viewDim.height/2;
        centerPt = getWorldOrImage(makeDevicePt(w, h), cc);
        w = h = 0;
        dist = 0;
        console.log('error: check the outline box');
    }
    // footprint or marker in world coordinate (there is projection in coordinate system)
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

/**
 * add the marker drawing objects into the new plot created after the drawing layer is created
 * @param drawLayer
 * @param plotIdAry plotId array including new plot and old plot
 * @returns {*}
 */
function attachToNewPlot(drawLayer, plotIdAry) {
    const data = get(drawLayer, ['drawData', 'data'], {});
    if (isEmpty(data)) return null;

    const existPlotId = !isEmpty(data) && Object.keys(data).find((pId) => {
            return !isEmpty(drawLayer.drawData.data[pId]);
        });

    if (!existPlotId) return null;

    const { text, textLoc, renderOptions, actionInfo, translation, drawObjAry} =
                                                    get(drawLayer, ['drawData', 'data', existPlotId]);
    const newPlotId = plotIdAry.find((pId) => isEmpty(data[pId]));
    if (!newPlotId) return null;

    const existPlot = primePlot(visRoot(), existPlotId);
    const {radius=1, unitType, pts} = get(drawObjAry, ['0']) || {};
    let   s = lengthToArcsec(radius * 2, CsysConverter.make(existPlot), unitType);  // in pixel unit

    const plot = primePlot(visRoot(), newPlotId);
    const cc = CsysConverter.make(plot);
    let wpt = pts[0];
    const markerObj = makeMarker(wpt, s, s,
                               {isOutline: false, isResize: false},
                                cc, text, textLoc, ShapeDataObj.UnitType.ARCSEC);
    // update current size and point in case the outlinebox is center or plotcenter type. (not the original one)
    if (!isOutlineBoxOriginal(markerObj)) {
        const outlineBox = markerObj.drawObjAry[markerObj.outlineIndex];
        if (outlineBox) {
            wpt = outlineBox.pts[0];
            s = lengthToArcsec(outlineBox.width, cc, outlineBox.unitType);
            s = lengthToArcsec(outlineBox.height, cc, outlineBox.unitType);
        } else {
            console.log('error: check null outlinebox');
        }
    }

    const aInfo = Object.assign({}, actionInfo, {currentPt: wpt, currentSize: [s, s], unitT: ShapeDataObj.UnitType.ARCSEC});
    set(drawLayer.drawData, [DataTypes.DATA, newPlotId], Object.assign(markerObj,
                                                         {actionInfo: aInfo, renderOptions, translation}));
    const dlObj = {drawData: drawLayer.drawData, helpLine: editHelpText};
    if (aInfo.markerStatus) {
        const {exclusiveDef, vertexDef} = updateVertexInfo(markerObj, newPlotId, drawLayer, drawLayer);

        if (exclusiveDef && vertexDef) {
            return clone(dlObj, {markerStatus: aInfo.markerStatus, vertexDef, exclusiveDef});
        }
    }
    return dlObj;
}

function debugOutline(markObj) {
    if (!markObj.drawObjAry[markObj.outlineIndex]) {
        console.log('error: outline box does not exist');
        return true;
    }
    return false;
}
