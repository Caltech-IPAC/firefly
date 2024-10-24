/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';
import DrawLayerCntlr, {dlRoot, dispatchAttachLayerToPlot,
                        dispatchCreateDrawLayer, getDlAry} from '../visualize/DrawLayerCntlr.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef, TextLocation} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {primePlot, getDrawLayerById, getDrawLayersByType} from '../visualize/PlotViewUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {ANGLE_UNIT, OutlineType, getWorldOrImage, findClosestIndex, makeFootprint,
        lengthSizeUnit, updateFootprintDrawobjAngle,
        updateFootprintTranslate, updateFootprintOutline} from '../visualize/draw/MarkerFootprintObj.js';
import {
    getCC, cancelTimeoutProcess, initMarkerPos, getPlot, ifUpdateOutline, hasNoProjection,
    updateVertexInfo, updateMarkerText, translateForRelocate, getMovement, isGoodPlot, MarkerStatus
} from './MarkerTool.js';
import {getFootprintToolUIComponent} from './FootprintToolUI.jsx';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {clone} from '../util/WebUtil.js';
import {getDS9Region} from '../rpc/PlotServicesJson.js';
import {FootprintFactory} from '../visualize/draw/FootprintFactory.js';
import {makeImagePt} from '../visualize/Point.js';
import {get, set, isArray, has, isNil, isEmpty} from 'lodash';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import {relocatable} from './FootprintLocatable.js';
import {MarkerToolUI} from './MarkerToolUI.jsx';


const editHelpText='Click any part of the footprint and drag to move, click rotate handle and drag to rotate';
const footprintInterval = 3000; // time interval for showing marker with handlers and no handlers

export const getMarkerToolUIComponent = (drawLayer,pv) => <MarkerToolUI drawLayer={drawLayer} pv={pv}/>;

const ID= 'OVERLAY_FOOTPRINT';
const TYPE_ID= 'OVERLAY_FOOTPRINT_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,null, getFootprintToolUIComponent);
const FootprintStatus = new Enum(['attached', 'select', 'attached_relocate', 'relocate', 'rotate']);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID
const  FPFilePrefix = '${footprintFile}';
const FPDefPrefix = '${footprintDef}';

let idCnt=0;
export const createNewFootprintLayerId = () => {
    const layerList =  getDrawLayersByType(getDlAry(), TYPE_ID);

    while (true) {
        const newId = `${ID}-${idCnt++}`;
        const dl = layerList.find((layer) => {
            return (layer.drawLayerId && layer.drawLayerId === newId);
        });
        if (!dl) {
            return newId;
        }
    }
};

let titleCnt = 1;
export const getFootprintLayerTitle = (layerTitle) => {
    const defaultFootprintTitle = 'footprint tool';
    const layerList =  getDrawLayersByType(getDlAry(), TYPE_ID);
    let   cntTitle = 0;

    while (true) {
        const newTitle = layerTitle ? (!cntTitle ? layerTitle : `${layerTitle}-${cntTitle}`)
            : `${defaultFootprintTitle}-${titleCnt++}`;
        const dl = layerList && layerList.find((layer) => {
            return (layer.title && layer.title === newTitle);
        });

        if (layerTitle) {
            cntTitle++;
        }
        if (!dl) {
            return newTitle;
        }
    }
};

const getRotate = (regions) => {
    return  get(regions, [0, 'options', 'rotatable'], 1)===1;
};

export function footprintCreateLayerActionCreator(rawAction) {
    return (dispatcher) => {
        const {plotId,
            footprintId: drawLayerId,
            layerTitle: title,
            attachPlotGroup,
            footprint,
            instrument,      // if instrument, move center of the instrument to the target, or move (0,0) to the target
            relocateBy,
            fromFile,
            fromRegionAry} = rawAction.payload;

        const fpInfo = { footprint, instrument, relocateBy, fromFile, fromRegionAry};
        const isPredefined = (!fromFile&&!fromRegionAry);
        const isInstrument = isPredefined ? (!!instrument) : (relocateBy&&(relocateBy === relocatable.center.key));
        const pId = (!plotId || (isArray(plotId) && plotId.length === 0)) ?
                              get(visRoot(), 'activePlotId') : isArray(plotId) ? plotId[0] : plotId;
        const plot = primePlot(visRoot(), pId);
        const cc = CsysConverter.make(plot);

        const handleRegions = (regionAry) => {
            const regions = FootprintFactory.getOriginalRegionsFromStc(regionAry, isInstrument, !isPredefined);
            const isRotate = getRotate(regions);    // footprint is rotatable in default, or define rotatable in regions

            if (regions) {
                const dl = getDrawLayerById(getDlAry(), drawLayerId);

                if (!dl) {
                    dispatchCreateDrawLayer(TYPE_ID, {title, drawLayerId, regions, fpInfo});
                }

                if (pId) {
                    dispatchAttachLayerToPlot(drawLayerId, pId, attachPlotGroup);
                    if (plot) {
                        const wpt = initMarkerPos(plot);

                        showFootprintByTimer(dispatcher, DrawLayerCntlr.FOOTPRINT_CREATE, regions, pId,
                            FootprintStatus.attached, footprintInterval, drawLayerId,
                            {isOutline: true, isRotate}, fpInfo, wpt);

                    }
                }
            }
        };

        if (hasNoProjection(cc)) return;
        if (footprint)  {
            const footprintDef = !isPredefined ? FPFilePrefix+footprint
                                               : FPDefPrefix + `${footprint}${instrument? '_'+instrument : ''}`;

            getDS9Region(footprintDef).then((result) => {
                if (has(result, 'RegionData') && result.RegionData.length > 0) {
                    handleRegions(result.RegionData);
                }
            });
        } else if (isArray(fromRegionAry) && fromRegionAry.length > 0) {
            handleRegions(fromRegionAry);
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
        const {plotId, imagePt, screenPt, drawLayer} = rawAction.payload; // imagePt/screenPt is the click point
        const cc = getCC(plotId);
        const {drawLayerId, regions, fpInfo} = drawLayer;
        const footprintObj = get(drawLayer, ['drawData', DataTypes.DATA, plotId], {});
        const {footprintStatus, currentPt, timeoutProcess} = get(footprintObj, 'actionInfo', {});
        const isRotate = getRotate(footprintObj.regions);
        let   wpt, idx, refPt;
        let   nextStatus = null;
        let   move = {};

        cancelTimeoutProcess(timeoutProcess);
        if (!getWorldOrImage(imagePt, cc)) return;
        // marker can move to anywhere the mouse click at while in 'attached' state
        if (footprintStatus === FootprintStatus.attached) {
            if (footprintObj) {
                idx = findClosestIndex(screenPt, footprintObj, cc).index;
                wpt = getWorldOrImage(currentPt, cc);

                if (has(footprintObj, 'rotateIndex') && idx === footprintObj.rotateIndex) {
                    nextStatus = FootprintStatus.rotate;
                } else {
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

        if (nextStatus && cc.isPointViewable(refPt)) {
            showFootprintByTimer(dispatcher, DrawLayerCntlr.FOOTPRINT_START, regions, plotId,
                nextStatus, footprintInterval, drawLayerId, {isOutline: true, isRotate}, fpInfo, wpt, refPt, move);
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
        const isRotate = getRotate(footprintObj.regions);

        cancelTimeoutProcess(timeoutProcess);
        // marker stays at current position and size
        if ([FootprintStatus.relocate, FootprintStatus.attached_relocate, FootprintStatus.rotate].includes(footprintStatus)) {
            const wpt = getWorldOrImage(currentPt, cc);

            showFootprintByTimer(dispatcher, DrawLayerCntlr.FOOTPRINT_END, regions, plotId,
                            FootprintStatus.select, footprintInterval, drawLayerId,
                            {isOutline: true, isRotate}, fpInfo, wpt);
        }
    };
}

/**
 * action create for FOOTPRINT_MOVE
 * @param rawAction
 * @returns {Function}
 */
export function footprintMoveActionCreator(rawAction) {
    const angleBetween = (origin, v1, v2) => {
        const [v1_x, v1_y] = [v1.x - origin.x, v1.y - origin.y];
        const [v2_x, v2_y] = [v2.x - origin.x, v2.y - origin.y];
        const z = (v1_x * v2_y - v1_y *  v2_x) > 0 ? 1 : -1;
        const innerProd = (v1_x * v2_x + v1_y * v2_y)/(Math.sqrt(v1_x * v1_x + v1_y * v1_y) *
            Math.sqrt(v2_x * v2_x + v2_y * v2_y));

        const angle = (innerProd > 1.0) ? Math.acos(1.0)
                                      : (innerProd < -1) ? Math.acos(-1.0) : Math.acos(innerProd);
        return z * angle;
    };


    return (dispatcher) => {
        const {plotId, imagePt, drawLayer} = rawAction.payload;
        const cc = getCC(plotId);
        const {drawLayerId, regions, fpInfo} = drawLayer;
        const footprintObj = get(drawLayer, ['drawData', DataTypes.DATA, plotId], {});
        let   {footprintStatus, currentPt: wpt, refPt} = get(footprintObj, 'actionInfo', {});
        const {timeoutProcess} = get(footprintObj, 'actionInfo', {});
        const move = {};
        let isHandle;
        const prePt = cc.getImageCoords(refPt);
        const isRotate = getRotate(footprintObj.regions);


        cancelTimeoutProcess(timeoutProcess);

        if (footprintStatus === FootprintStatus.rotate)  {    // footprint rotate by angle on screen angle
            if (!cc.pointInView(imagePt)) return;   // rotate stops
            const rotateCenter = cc.getImageCoords(centerForRotation(footprintObj, wpt)); // center of outline

            move.angle = -angleBetween(rotateCenter, prePt, imagePt); // angle on screen
            move.angleUnit = ANGLE_UNIT.radian;
            refPt = imagePt;
            isHandle = {isOutline:true, isRotate};
        } else if (footprintStatus === FootprintStatus.relocate || footprintStatus === FootprintStatus.attached_relocate) {
            // marker move to new mouse move position
            let deltaX = imagePt.x - prePt.x;
            let deltaY = imagePt.y - prePt.y;
            const fpCenterImg = cc.getImageCoords(wpt);

            // both footprint center and outline center are tested if being moved out of view
            wpt = getWorldOrImage(makeImagePt(fpCenterImg.x + deltaX, fpCenterImg.y + deltaY), cc);
            const olCenterImg = cc.getImageCoords(centerForOutline(footprintObj));
            const nextOlCenter = olCenterImg ? getWorldOrImage(makeImagePt(olCenterImg.x + deltaX, olCenterImg.y + deltaY), cc)
                                               : null;

            if (!wpt || !cc.pointInView(nextOlCenter)) {  // HiPS plot, wpt is out of range, no move
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
            isHandle = {isOutline:true, isRotate};
        }

        if (!isEmpty(move) && cc.isPointViewable(refPt)) {
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

    const rColor = get(initPayload, ['regions', '0', 'options', 'color'], 'green');
    const drawingDef= makeDrawingDef(rColor);
    const pairs= {
        [MouseState.DRAG.key]: DrawLayerCntlr.FOOTPRINT_MOVE,
        [MouseState.DOWN.key]: DrawLayerCntlr.FOOTPRINT_START,
        [MouseState.UP.key]: DrawLayerCntlr.FOOTPRINT_END
    };

    const actionTypes= [DrawLayerCntlr.FOOTPRINT_MOVE,
                      DrawLayerCntlr.FOOTPRINT_START,
                      DrawLayerCntlr.FOOTPRINT_END,
                      DrawLayerCntlr.FOOTPRINT_CREATE];

    const exclusiveDef= { exclusiveOnDown: true, type : 'anywhere' };

    const options= {
        canUseMouse:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        canUserDelete: true,
        hasPerPlotData: true,
        destroyWhenAllDetached: false,
        destroyWhenAllUserDetached : true
    };

    const title = initPayload.title ? initPayload.title : getFootprintLayerTitle();
    const id = initPayload.drawLayerId ? initPayload.drawLayerId : createNewFootprintLayerId();
    const dl = DrawLayer.makeDrawLayer(id, TYPE_ID, title,
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

    const dd = {...drawLayer.drawData};
    const {plotIdAry=[]} = drawLayer;
    let  retV = drawLayer;
    let  wptObj;

    switch (action.type) {

        case DrawLayerCntlr.FOOTPRINT_CREATE:
            plotIdAry.forEach((pId) => {
                const plot = getPlot(pId);
                if (plot && !hasNoProjection(CsysConverter.make(plot))) {
                    retV = createFootprintObjs(action, drawLayer, pId, initMarkerPos(plot), retV);
                }
            });

            return retV;

        case DrawLayerCntlr.FOOTPRINT_START:
        case DrawLayerCntlr.FOOTPRINT_MOVE:
        case DrawLayerCntlr.FOOTPRINT_END:
            const {wpt} = action.payload;

            plotIdAry.forEach((pId) => {
                if (isGoodPlot(pId) && !isEmpty(get(dd, ['data', pId]))) {
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
            // if (drawLayer?.drawData?.data) {
                action.payload?.plotIdAry?.forEach((pId) => {
                    if (isEmpty(dd?.data?.[pId])) {
                        retV = attachToNewPlot(retV, pId);
                    }
                });
            // }
            break;

        case ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION:
        case ImagePlotCntlr.ANY_REPLOT:
            if (plotIdAry) {
                plotIdAry.forEach((pId) => {
                    if (isGoodPlot(pId) && !isEmpty(get(dd, ['data', pId]))) {
                        wptObj = get(dd, ['data', pId, 'pts', '0']);
                        const cc = getCC(pId);
                        if (wptObj && cc.pointInView(wptObj)) {
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
    let  cursor = '';
    const dlAry = dlRoot().drawLayerAry.filter( (dl) => {
        return (dl.drawLayerTypeId === TYPE_ID) && (get(dl, 'visiblePlotIdAry').includes(plotView.plotId));
    });

    if (!screenPt) {
        //alert('null screenpt');
        return cursor;
    }
    const  cc= CsysConverter.make(primePlot(plotView));

    dlAry.find( (dl) => {
        const drawObj = get(dl, ['drawData', 'data', plotView.plotId]);
        const idx = findClosestIndex(screenPt, drawObj, cc).index;

        if (idx >= 0 && idx <= drawObj.outlineIndex) {
            cursor = 'pointer';
            return true;
        } else if (has(drawObj, 'rotateIndex') && (idx === drawObj.rotateIndex)) {
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
    let angleDeg = angleDegStr ? parseFloat(angleDegStr) : 0.0;
    while (angleDeg > 180.0 || angleDeg < -180.0) {
        angleDeg = angleDeg < 0 ? angleDeg + 360 : angleDeg - 360;
    }

    plotIdAry.forEach((plotId) => {
        if (isGoodPlot(plotId) && !isEmpty(footprintDrawObj[plotId])) {
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
    const setAction = (isHandle) => ({
        type: actionType,
        payload: {isHandle, regions, wpt, plotId, footprintStatus: doneStatus, drawLayerId, fpInfo, refPt, move}
    });

    const timeoutProcess = (timer !== 0) && (setTimeout(() => dispatcher(setAction({})), timer));
    const crtAction = set(setAction(isHandle), 'payload.timeoutProcess', timeoutProcess);
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
    if (!plotId || !wpt) {
        if (plotId) {
            set(dl.drawData, [DataTypes.DATA, plotId],  null);
            return {drawData: dl.drawData, helpLine: editHelpText};
        }
        return prevRet;
    }

    const {isHandle, regions, timeoutProcess, refPt, move} = action.payload;
    let   {footprintStatus} = action.payload;
    const crtFpObj = get(dl, ['drawData', DataTypes.DATA, plotId], {});
    let  {text = ''} = crtFpObj || {};
    const {textLoc = TextLocation.REGION_SE} = crtFpObj || {};

    if (footprintStatus === FootprintStatus.attached ||
        footprintStatus === FootprintStatus.attached_relocate) {
        text = get(dl, 'title', '');    // title is the default text by the footprint
    }

     const cc = getCC(plotId);
     let footprintObj;

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
             const {apt} = move;    // move to relocate or rotate

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
                 if (ifUpdateOutline(crtFpObj, cc)) {
                     footprintObj = updateFootprintOutline(crtFpObj, cc, true);
                 } else {
                     footprintObj = Object.assign({}, crtFpObj);
                 }
             }
         }
         updateHandle(isHandle, footprintObj);
     }

     if ([ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION, ImagePlotCntlr.ANY_REPLOT].includes(action.type) && !footprintStatus) {
         footprintStatus = get(dl.drawData, [DataTypes.DATA, plotId, 'actionInfo', 'footprintStatus'], FootprintStatus.select);
     }
     footprintObj.plotId = plotId;
     footprintObj.lastZoom = cc.zoomFactor;
     const actionInfo = {
         currentPt: wpt,       // marker center, world or image coordinate
         timeoutProcess: timeoutProcess ? timeoutProcess : null,
         refPt: refPt ? refPt : null,   // in World coordinate to be consistent in case plot is changed
         footprintStatus
     };

     set(dl.drawData, [DataTypes.DATA, plotId], Object.assign(footprintObj, {actionInfo}));


     const dlObj = {drawData: dl.drawData, helpLine: editHelpText, lastDrawData: dl.drawData.data[plotId]};

     if (footprintStatus) {
         const {exclusiveDef, vertexDef} = updateVertexInfo(footprintObj, plotId, prevRet);

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
    const {outlineIndex, drawObjAry} = drawObj;
    const outlineBox =  (outlineIndex && drawObjAry && drawObjAry.length > outlineIndex ) ? drawObjAry[outlineIndex] : null;

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
    const {isRotate, isOutline} = isHandle || {};

    footprintObj.includeRotate = isRotate;
    footprintObj.includeOutline = isOutline;
}

function resetRotateSide(footprintObj) {
    const {drawObjAry, outlineIndex} = footprintObj;
    const rSide = 'rotateSide';

    if (outlineIndex && drawObjAry.length > outlineIndex) {
        const outlineBox = drawObjAry[outlineIndex];

        if (has(outlineBox, rSide)) {
            outlineBox[rSide] = 1;
        }
    }
}

/**
 * add the footprint drawing objects into the new plot after the drawing layer is created
 * @param drawLayer
 *  @param newPlotId new plot to attach
 * @returns {*}
 */
function  attachToNewPlot(drawLayer, newPlotId) {
    const data = get(drawLayer, ['drawData', 'data'], {});

    const existPlotId = !isEmpty(data) && Object.keys(data).find((pId) => {
            return !isEmpty(drawLayer.drawData.data[pId]);
        });

    if (!existPlotId && !drawLayer.lastDrawData) return drawLayer;

    const { text, textLoc, renderOptions, actionInfo, translation, angle, angleUnit, regions, isRotatable, pts}
        = drawLayer?.drawData?.data?.[existPlotId] ?? drawLayer.lastDrawData;

    const plot = primePlot(visRoot(), newPlotId);
    const cc = CsysConverter.make(plot);
    if (hasNoProjection(cc)) {
        return drawLayer;
    }
    let wpt = initMarkerPos(plot, cc);
    let footprintObj = makeFootprint(regions, wpt,
        {
            isOutline: false,
            isRotate: isRotatable
        },
        cc, text, textLoc);

    if (!isEmpty(translation)) {
        footprintObj = updateFootprintTranslate(footprintObj, cc, translation, true);
        resetRotateSide(footprintObj);
        wpt = get(footprintObj, ['pts', '0']);
    }

    if (angle) {
        footprintObj = updateFootprintDrawobjAngle(footprintObj, cc, wpt, angle, angleUnit);
        footprintObj.angleFromUI = false;
    }

    if (ifUpdateOutline(footprintObj, cc)) {
        footprintObj = updateFootprintOutline(footprintObj, cc, true);
    }

    const aInfo = {...actionInfo, currentPt: wpt};

    const newDrawData= drawLayer.drawData?.data ? {...drawLayer.drawData} : {data:{}};
    newDrawData.data[newPlotId]= {...footprintObj, actionInfo: aInfo, renderOptions, translation };

    const dlObj = {drawData: newDrawData, helpLine: editHelpText};

    if (aInfo.footprintStatus) {
        const {exclusiveDef, vertexDef} = updateVertexInfo(newDrawData.data[newPlotId], newPlotId, drawLayer);

        if (exclusiveDef && vertexDef) {
            return {...dlObj, footprintStatus: aInfo.footprintStatus, vertexDef, exclusiveDef};
        }
    }
    return dlObj;
}
