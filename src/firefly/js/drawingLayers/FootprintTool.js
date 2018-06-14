/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import DrawLayerCntlr, {dlRoot, dispatchAttachLayerToPlot,
                        dispatchCreateDrawLayer, getDlAry} from '../visualize/DrawLayerCntlr.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef, TextLocation} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {ANGLE_UNIT, OutlineType, getWorldOrImage, findClosestIndex, makeFootprint,
        lengthSizeUnit, updateFootprintDrawobjAngle,
        updateFootprintTranslate, updateFootprintOutline} from '../visualize/draw/MarkerFootprintObj.js';
import {markerInterval, getCC, cancelTimeoutProcess, initMarkerPos, getPlot,
        updateVertexInfo, updateMarkerText, translateForRelocate, getMovement, isGoodPlot} from './MarkerTool.js';
import {getFootprintToolUIComponent} from './FootprintToolUI.jsx';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {isPointInView} from '../visualize/draw/ShapeHighlight.js';
import {clone} from '../util/WebUtil.js';
import {getDS9Region} from '../rpc/PlotServicesJson.js';
import {FootprintFactory} from '../visualize/draw/FootprintFactory.js';
import {makeImagePt} from '../visualize/Point.js';
import {get, set, isArray, has, isNil, isEmpty} from 'lodash';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import {isHiPS} from '../visualize/WebPlot.js';
import Enum from 'enum';


const editHelpText='Click any part of the footprint and drag to move, click rotate handle and drag to rotate';

const footprintInterval = markerInterval; // time interval for showing marker with handlers and no handlers
const ID= 'OVERLAY_FOOTPRINT';
const TYPE_ID= 'OVERLAY_FOOTPRINT_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,null, getFootprintToolUIComponent);
const FootprintStatus = new Enum(['attached', 'select', 'attached_relocate', 'relocate', 'rotate']);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;

export function footprintCreateLayerActionCreator(rawAction) {
    return (dispatcher) => {
        var {plotId,
            footprintId: drawLayerId,
            layerTitle: Title,
            attachPlotGroup,
            footprint,
            instrument} = rawAction.payload;

        if (footprint)  {
            var footprintDef = '${footprintDef}' + `${footprint}${instrument? '_'+instrument : ''}`;
            var isInstrument = !!instrument;
            var fpInfo = { footprint, instrument};

            getDS9Region(footprintDef).then((result) => {
                if (has(result, 'RegionData') && result.RegionData.length > 0) {
                    var regions = FootprintFactory.getOriginalRegionsFromStc(result.RegionData, isInstrument);

                    if (regions) {
                        var dl = getDrawLayerById(getDlAry(), drawLayerId);

                        if (!dl) {
                            dispatchCreateDrawLayer(TYPE_ID, {Title, drawLayerId, regions, fpInfo});
                        }

                        // plotId could be an array or single value
                        var pId = (!plotId || (isArray(plotId) && plotId.length === 0)) ?
                            get(visRoot(), 'activePlotId') : isArray(plotId) ? plotId[0] : plotId;

                        if (pId) {
                            dispatchAttachLayerToPlot(drawLayerId, pId, attachPlotGroup);

                            var plot = primePlot(visRoot(), pId);
                            if (plot) {
                                var wpt = initMarkerPos(plot);

                                showFootprintByTimer(dispatcher, DrawLayerCntlr.FOOTPRINT_CREATE, regions, pId,
                                    FootprintStatus.attached, footprintInterval, drawLayerId,
                                    {isOutline: true, isRotate: true}, fpInfo, wpt);

                            }
                        }
                    }
                }
            });
        }
    };
}



/**
* action creator for FOOTPRINT_START, only when the cursor shows 'pointer"
* @param rawAction
* @returns {Function}
*/
export function footprintStartActionCreator(rawAction) {
    return (dispatcher) => {
        const {plotId, imagePt, screenPt, drawLayer} = rawAction.payload;
        const cc = getCC(plotId);
        const {drawLayerId, regions, fpInfo} = drawLayer;
        const footprintObj = get(drawLayer, ['drawData', DataTypes.DATA, plotId], {});
        const {footprintStatus, currentPt, timeoutProcess} = get(footprintObj, 'actionInfo', {});
        var   wpt, idx, refPt;
        var   nextStatus = null;
        let   move = {};

        cancelTimeoutProcess(timeoutProcess);
        if (!getWorldOrImage(imagePt, cc)) return;
        // marker can move to anywhere the mouse click at while in 'attached' state
        if (footprintStatus === FootprintStatus.attached) {
            if (footprintObj) {
                idx = findClosestIndex(screenPt, footprintObj, cc).index;
                if (has(footprintObj, 'rotateIndex') && idx === footprintObj.rotateIndex) {
                    wpt = getWorldOrImage(currentPt, cc);
                    nextStatus = FootprintStatus.rotate;
                } else {
                    wpt = getWorldOrImage(currentPt, cc);     // relocate the footprint right after the layer is attached
                    move = getMovement(currentPt, imagePt, cc);
                    nextStatus = FootprintStatus.attached_relocate;
                }
            }
        } else if (footprintStatus === FootprintStatus.select) {
            if (footprintObj) {
                idx = findClosestIndex(screenPt, footprintObj, cc).index;
                if (has(footprintObj, 'rotateIndex') && idx === footprintObj.rotateIndex) {
                    nextStatus = FootprintStatus.rotate;
                } else if (idx >= 0 && idx <= footprintObj.outlineIndex) {
                    nextStatus = FootprintStatus.relocate;
                }
            }
            wpt = getWorldOrImage(currentPt, cc);
        }
        if ([FootprintStatus.relocate, FootprintStatus.attached_relocate, FootprintStatus.rotate].includes(nextStatus)) {
            refPt = imagePt;                   // refPt is used for calculating the relocated offset of next time
        }

        if (nextStatus && isPointInView(refPt, cc)) {
            showFootprintByTimer(dispatcher, DrawLayerCntlr.FOOTPRINT_START, regions, plotId,
                nextStatus, footprintInterval, drawLayerId, {isOutline: true, isRotate:true}, fpInfo, wpt, refPt, move);
        }
    };
}

/**
 * action creator for FOOTPRINT_END
 * @param rawAction
 * @returns {Function}
 */
export function footprintEndActionCreator(rawAction) {
    return (dispatcher) => {
        const {plotId, drawLayer} = rawAction.payload;
        const cc = getCC(plotId);
        const {drawLayerId, regions, fpInfo} = drawLayer;
        const footprintObj = get(drawLayer, ['drawData', DataTypes.DATA, plotId], {});
        const {footprintStatus, currentPt, timeoutProcess} = get(footprintObj, 'actionInfo', {});
        var   {includeOutline: isOutline, includeRotate: isRotate } = footprintObj;

        cancelTimeoutProcess(timeoutProcess);
        // marker stays at current position and size
        if ([FootprintStatus.relocate, FootprintStatus.attached_relocate, FootprintStatus.rotate].includes(footprintStatus)) {
            const wpt = getWorldOrImage(currentPt, cc);

            showFootprintByTimer(dispatcher, DrawLayerCntlr.FOOTPRINT_END, regions, plotId,
                            FootprintStatus.select, footprintInterval, drawLayerId, {isOutline, isRotate}, fpInfo, wpt);
        }
    };
}

/**
 * action create for FOOTPRINT_MOVE
 * @param rawAction
 * @returns {Function}
 */
export function footprintMoveActionCreator(rawAction) {
    var angleBetween = (origin, v1, v2) => {
        var [v1_x, v1_y] = [v1.x - origin.x, v1.y - origin.y];
        var [v2_x, v2_y] = [v2.x - origin.x, v2.y - origin.y];
        var z = (v1_x * v2_y - v1_y *  v2_x) > 0 ? 1 : -1;
        var innerProd = (v1_x * v2_x + v1_y * v2_y)/(Math.sqrt(v1_x * v1_x + v1_y * v1_y) *
            Math.sqrt(v2_x * v2_x + v2_y * v2_y));

        var angle = (innerProd > 1.0) ? Math.acos(1.0)
                                      : (innerProd < -1) ? Math.acos(-1.0) : Math.acos(innerProd);
        return z * angle;
    };


    return (dispatcher) => {
        const {plotId, imagePt, drawLayer} = rawAction.payload;
        const cc = getCC(plotId);
        const {drawLayerId, regions, fpInfo} = drawLayer;
        const footprintObj = get(drawLayer, ['drawData', DataTypes.DATA, plotId], {});
        var   {footprintStatus, currentPt: wpt, timeoutProcess, refPt} = get(footprintObj, 'actionInfo', {});
        var move = {};
        var isHandle;
        var prePt = cc.getImageCoords(refPt);


        cancelTimeoutProcess(timeoutProcess);

        if (footprintStatus === FootprintStatus.rotate)  {    // footprint rotate by angle on screen angle
            if (!isPointInView(imagePt, cc)) return;   // rotate stops
            const rotateCenter = cc.getImageCoords(centerForRotation(footprintObj, wpt)); // center of outline

            move.angle = -angleBetween(rotateCenter, prePt, imagePt); // angle on screen
            move.angleUnit = ANGLE_UNIT.radian;
            refPt = imagePt;
            isHandle = {isOutline: true, isRotate: true};
        } else if (footprintStatus === FootprintStatus.relocate || footprintStatus === FootprintStatus.attached_relocate) {
            // marker move to new mouse move position
            var deltaX = imagePt.x - prePt.x;
            var deltaY = imagePt.y - prePt.y;
            var fpCenterImg = cc.getImageCoords(wpt);

            // both footprint center and outline center are tested if being moved out of view
            wpt = getWorldOrImage(makeImagePt(fpCenterImg.x + deltaX, fpCenterImg.y + deltaY), cc);
            const olCenterImg = cc.getImageCoords(centerForOutline(footprintObj));
            const nextOlCenter = olCenterImg ? getWorldOrImage(makeImagePt(olCenterImg.x + deltaX, olCenterImg.y + deltaY), cc)
                                               : null;

            if (!wpt || !isPointInView(nextOlCenter, cc)) {  // HiPS plot, wpt is out of range, no move
                //if (isHiPS(cc)) rotateHiPSImage(cc, fpCenterImg, olCenterImg, deltaX, deltaY);
                deltaX = 0;
                deltaY = 0;
                wpt = getWorldOrImage(makeImagePt(fpCenterImg.x, fpCenterImg.y), cc); // reference point (refPt) no change
            } else {
                refPt = imagePt;
            }

            deltaX = lengthSizeUnit(cc, deltaX, ShapeDataObj.UnitType.IMAGE_PIXEL);
            deltaY = lengthSizeUnit(cc, deltaY, ShapeDataObj.UnitType.IMAGE_PIXEL);
            move.apt = {x: deltaX.len, y: deltaY.len, type: deltaX.unit};

            footprintStatus = FootprintStatus.relocate;
            isHandle = {isOutline: true, isRotate: true};
        }

        if (!isEmpty(move) && isPointInView(refPt, cc)) {
            showFootprintByTimer(dispatcher, DrawLayerCntlr.FOOTPRINT_MOVE, regions, plotId,
                                 footprintStatus, 0, drawLayerId, isHandle, fpInfo, wpt, refPt, move);
        }
    };
}


/**
 * create drawing layer for a new marker
 * @param {object} initPayload
 * @return {Function}
 */
function creator(initPayload) {

    var rColor = get(initPayload, ['regions', '0', 'options', 'color'], 'green');
    var drawingDef= makeDrawingDef(rColor);
    var pairs= {
        [MouseState.DRAG.key]: DrawLayerCntlr.FOOTPRINT_MOVE,
        [MouseState.DOWN.key]: DrawLayerCntlr.FOOTPRINT_START,
        [MouseState.UP.key]: DrawLayerCntlr.FOOTPRINT_END
    };

    var actionTypes= [DrawLayerCntlr.FOOTPRINT_MOVE,
                      DrawLayerCntlr.FOOTPRINT_START,
                      DrawLayerCntlr.FOOTPRINT_END,
                      DrawLayerCntlr.FOOTPRINT_CREATE];

    var exclusiveDef= { exclusiveOnDown: true, type : 'anywhere' };


    idCnt++;
    var options= {
        canUseMouse:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        canUserDelete: true,
        hasPerPlotData: true,
        destroyWhenAllDetached: true
    };
    var title = get(initPayload, 'Title', 'Footprint Tool');
    var dl = DrawLayer.makeDrawLayer( get(initPayload, 'drawLayerId', ` ${ID}-${idCnt}`),
                                    TYPE_ID, title,
                                    options, drawingDef, actionTypes, pairs, exclusiveDef, getCursor);
    dl.regions = get(initPayload, 'regions', null);
    dl.fpInfo = get(initPayload, 'fpInfo', {});
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
    var  retV = null;
    let  wptObj;

    switch (action.type) {

        case DrawLayerCntlr.FOOTPRINT_CREATE:
            plotIdAry.forEach((pId) => {
                const plot = getPlot(pId);
                if (plot) {
                    retV = createFootprintObjs(action, drawLayer, pId, initMarkerPos(plot), retV);
                }
            });

            return retV;

        case DrawLayerCntlr.FOOTPRINT_START:
        case DrawLayerCntlr.FOOTPRINT_MOVE:
        case DrawLayerCntlr.FOOTPRINT_END:
            const {wpt} = action.payload;

            plotIdAry.forEach((pId) => {
                if (isGoodPlot(pId)) {
                    wptObj = (pId === plotId) ? wpt : get(dd, ['data', pId, 'pts', '0']);
                    retV = createFootprintObjs(action, drawLayer, pId, wptObj, retV);
                }
            });
            return retV;

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {fpText, fpTextLoc, angleDeg} = action.payload.changes;

            if (plotIdAry) {
                if (!isNil(angleDeg)) {
                    return updateFootprintAngle(angleDeg, dd[DataTypes.DATA], plotIdAry);
                } else {
                    return updateMarkerText(fpText, fpTextLoc, dd[DataTypes.DATA], plotIdAry);
                }
            }
            break;
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            if (!isEmpty(get(drawLayer, ['drawData', 'data']))) {
                return attachToNewPlot(drawLayer, get(action.payload, ['plotIdAry', '0']));
            }
            break;
        case ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION:
        case ImagePlotCntlr.ANY_REPLOT:
            console.log('type '+action.type);
            if (plotIdAry) {
                plotIdAry.forEach((pId) => {
                    if (isGoodPlot(pId)) {
                        wptObj = get(dd, ['data', pId, 'pts', '0']);
                        const cc = getCC(pId);

                        if (isHiPS(cc) && wptObj && isPointInView(wptObj, cc)) {
                            retV = createFootprintObjs(action, drawLayer, pId, wptObj, retV);
                        }
                    }
                });
            }

            break;
        default:
            return null;
    }
    return retV;
}

function getCursor(plotView, screenPt) {
    var  cursor = '';
    var dlAry = dlRoot().drawLayerAry.filter( (dl) => {
        return (dl.drawLayerTypeId === TYPE_ID) && (get(dl, 'visiblePlotIdAry').includes(plotView.plotId));
    });

    if (!screenPt) {
        //alert('null screenpt');
        return cursor;
    }
    const  cc= CsysConverter.make(primePlot(plotView));

    dlAry.find( (dl) => {
        var drawObj = get(dl, ['drawData', 'data', plotView.plotId]);
        var idx = findClosestIndex(screenPt, drawObj, cc).index;

        if (idx >= 0 && idx <= drawObj.outlineIndex) {
            cursor = 'pointer';
            return true;
        } else if (idx === drawObj.rotateIndex) {
            cursor = 'alias';
            return true;
        } else {
            return false;
        }
    });
    return cursor;
}

/**
 * set footprint rotate angle
 * @param angleDegStr
 * @param footprintDrawObj
 * @param plotIdAry
 * @returns {*}
 */
function updateFootprintAngle(angleDegStr, footprintDrawObj, plotIdAry) {
    var angleDeg;
    angleDeg = angleDegStr ? parseFloat(angleDegStr) : 0.0;
    while (angleDeg > 180.0 || angleDeg < -180.0) {
        angleDeg = angleDeg < 0 ? angleDeg + 360 : angleDeg - 360;
    }

    plotIdAry.forEach((plotId) => {
        if (isGoodPlot(plotId)) {
            const cc = getCC(plotId);

            const angleUpdatedObj = updateFootprintDrawobjAngle(footprintDrawObj[plotId], cc,
                                    footprintDrawObj[plotId].pts[0], angleDeg, ANGLE_UNIT.degree, true);
            if (angleUpdatedObj) {
                angleUpdatedObj.angleFromUI = true;
                footprintDrawObj[plotId] = angleUpdatedObj;
            }
        }
    });

    return {drawData: {data: footprintDrawObj}};
}



/**
 * dispatch action to locate marker with corners and no corner by timer interval on the drawing layer
 * @param dispatcher
 * @param actionType
 * @param regions regions contained in footprint drawObj
 * @param plotId
 * @param doneStatus
 * @param timer  milliseconds
 * @param drawLayerId
 * @param isHandle
 * @param fpInfo
 * @param wpt    marker location world coordinate
 * @param refPt
 * @param move
 */
function showFootprintByTimer(dispatcher, actionType, regions, plotId, doneStatus, timer, drawLayerId, isHandle,
                              fpInfo, wpt, refPt, move) {
    var setAction = (isHandle) => ({
        type: actionType,
        payload: {isHandle, regions, wpt, plotId, footprintStatus: doneStatus, drawLayerId, fpInfo, refPt, move}
    });
    var timeoutProcess = (timer !== 0) && (setTimeout(() => dispatcher(setAction({})), timer));
    var crtAction = set(setAction(isHandle), 'payload.timeoutProcess', timeoutProcess);
    dispatcher(crtAction);
}

/**
 * create drawlayer object containing only the properties which has updated value
 * @param action
 * @param dl
 * @param plotId
 * @param wpt
 * @param prevRet previous return object
 * @returns {*}
 */
function createFootprintObjs(action, dl, plotId, wpt, prevRet) {
    if (!plotId || !wpt) return null;

    const {isHandle, regions, timeoutProcess, refPt, move} = action.payload;
    let {footprintStatus} = action.payload;
    const crtFpObj = get(dl, ['drawData', DataTypes.DATA, plotId], {});
    var  {text = ''} = crtFpObj;
    const {textLoc = TextLocation.REGION_SE} = crtFpObj;

    if (footprintStatus === FootprintStatus.attached ||
        footprintStatus === FootprintStatus.attached_relocate) {
        text = get(dl, 'title', '');    // title is the default text by the footprint
    }

     var cc = getCC(plotId);
     var footprintObj;


     if (footprintStatus === FootprintStatus.attached ||
         footprintStatus === FootprintStatus.attached_relocate) {
         footprintObj = makeFootprint(regions, wpt, isHandle, cc, text, textLoc);

         // position is relocated after the layer is attached by the click
         if (footprintStatus === FootprintStatus.attached_relocate) {
             footprintObj = translateForRelocate(footprintObj,  move, cc);
             wpt = get(footprintObj, ['pts', '0']);
         }

     } else if (crtFpObj) {
         if ((footprintStatus === FootprintStatus.rotate || footprintStatus === FootprintStatus.relocate) && !isEmpty(move)) {
             var {apt} = move;    // move to relocate or rotate

             if (apt) {      // translate
                 footprintObj = updateFootprintTranslate(crtFpObj, cc, apt);
                 resetRotateSide(footprintObj);
                 wpt = get(footprintObj, ['pts', '0']);
             } else {
                 footprintObj = updateFootprintDrawobjAngle(crtFpObj, cc, wpt, move.angle, move.angleUnit);
                 footprintObj.angleFromUI = false;
             }
         } else {       // start to move or rotate (mouse down) or end the operation (mouse up)
             if (footprintStatus !== FootprintStatus.select ) {
                        // update the outlinebox when the target starts to move or rotate or change project center
                 footprintObj = updateFootprintOutline(crtFpObj, cc);
             } else {
                 footprintObj = Object.assign({}, crtFpObj);
             }
         }
         updateHandle(isHandle, footprintObj);
     }

     if ([ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION, ImagePlotCntlr.ANY_REPLOT].includes(action.type) &&
         !footprintStatus) {
         footprintStatus = get(dl.drawData, [DataTypes.DATA, plotId, 'actionInfo', 'footprintStatus'],  FootprintStatus.select);
     }
     footprintObj.plotId = plotId;
     footprintObj.lastZoom = cc.zoomFactor;
     const actionInfo = {currentPt: wpt,       // marker center, world or image coordinate
                         timeoutProcess: timeoutProcess ? timeoutProcess : null,
                         refPt: refPt ? refPt : null,   // in World coordinate to be consistent in case plot is changed
                         footprintStatus};
     set(dl.drawData, [DataTypes.DATA, plotId],  Object.assign(footprintObj, {actionInfo}));
     var dlObj = {drawData: dl.drawData, helpLine: editHelpText};

     if (footprintStatus) {
         var {exclusiveDef, vertexDef} = updateVertexInfo(footprintObj, plotId, dl, prevRet);

         if (exclusiveDef && vertexDef) {
             return clone(dlObj, {footprintStatus, vertexDef, exclusiveDef});
         }
     }
     return dlObj;

}

/**
 * get rotation center
 * @param drawObj
 * @param wpt
 * @returns {*}
 */
function centerForRotation(drawObj, wpt) {
    var {outlineIndex, drawObjAry} = drawObj;
    var outlineBox =  (outlineIndex && drawObjAry && drawObjAry.length > outlineIndex ) ? drawObjAry[outlineIndex] : null;

    //return outlineBox.pts[0];
    return (outlineBox && outlineBox.outlineType === OutlineType.plotcenter) ? outlineBox.pts[0] :
           (outlineBox ? wpt :null);
}

function centerForOutline(drawObj) {
    const {outlineIndex, drawObjAry} = drawObj;
    const outlineBox = (outlineIndex && drawObjAry && drawObjAry.length > outlineIndex ) ? drawObjAry[outlineIndex] : null;

    return outlineBox ? outlineBox.pts[0] : null;
}
/**
 * update the handle inclusion on the footprint object
 * @param isHandle
 * @param footprintObj
 */
function updateHandle(isHandle, footprintObj) {
    var {isRotate, isOutline} = isHandle || {};

    footprintObj.includeRotate = isRotate;
    footprintObj.includeOutline = isOutline;
}

function resetRotateSide(footprintObj) {
    var {drawObjAry, outlineIndex} = footprintObj;
    const rSide = 'rotateSide';

    if (outlineIndex && drawObjAry.length > outlineIndex) {
        var outlineBox = drawObjAry[outlineIndex];

        if (has(outlineBox, rSide)) {
            outlineBox[rSide] = 1;
        }
    }
}

/**
 * add the footprint drawing objects into the new plot after the drawing layer is created
 * @param drawLayer
 * @param newPlotId new plot
 * @returns {*}
 */
function attachToNewPlot(drawLayer, newPlotId) {
    const data = get(drawLayer, ['drawData', 'data'], {});
    const existPlotId = !isEmpty(data) && Object.keys(data).find((pId) => {
            return !isEmpty(drawLayer.drawData.data[pId]);
        });

    if (!existPlotId) return null;

    const { text, textLoc, renderOptions, actionInfo, translation, angle, angleUnit,
            drawData, regions} = get(drawLayer, ['drawData', 'data', existPlotId]);
    const plot = primePlot(visRoot(), newPlotId);
    const cc = CsysConverter.make(plot);
    let wpt = initMarkerPos(plot, cc);
    let footprintObj = makeFootprint(regions, wpt,
                                  {isOutline: drawData? drawData.isOutline : false,
                                   isRotate: drawData? drawData.isRorate : false},
                                   cc, text, textLoc);

    if (!isEmpty(translation)) {
        footprintObj = updateFootprintTranslate(footprintObj, cc, translation);
        //footprintObj= updateFootprintOutline(footprintObj, cc);
        resetRotateSide(footprintObj);
        wpt = get(footprintObj, ['pts', '0']);
    }

    if (angle) {
        footprintObj = updateFootprintDrawobjAngle(footprintObj, cc, wpt, angle, angleUnit);
        footprintObj.angleFromUI = false;
    }

    const aInfo = Object.assign({}, actionInfo, {currentPt: wpt});
    set(drawLayer.drawData, [DataTypes.DATA, newPlotId], Object.assign(footprintObj, {actionInfo: aInfo,
                                                                                      renderOptions, translation}));
    const dlObj = {drawData: drawLayer.drawData, helpLine: editHelpText};

    if (aInfo.footprintStatus) {
        const {exclusiveDef, vertexDef} = updateVertexInfo(footprintObj, newPlotId, drawLayer, drawLayer);

        if (exclusiveDef && vertexDef) {
            return clone(dlObj, {footprintStatus: aInfo.footprintStatus, vertexDef, exclusiveDef});
        }
    }
    return dlObj;
}
