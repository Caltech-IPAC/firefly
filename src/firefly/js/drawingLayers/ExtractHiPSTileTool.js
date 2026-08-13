/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {visRoot} from '../api/ApiUtilImage';
import ShapeDataObj from '../visualize/draw/ShapeDataObj';
import {dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerDispatch';
import {dispatchAttributeChange} from '../visualize/ImagePlotDispatch';
import {PlotAttribute} from '../visualize/PlotAttribute';
import {isDrawLayerVisible, currentP, getCenterOfProjection, refreshP} from '../visualize/PlotViewUtil.js';
import {getHiPSNorderlevel, getHealpixCellAtNorder} from '../visualize/HiPSUtil.js';
import FootprintObj from '../visualize/draw/FootprintObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import CysConverter from '../visualize/CsysConverter';
import {getAllPlotViewIdByOverlayLock} from '../visualize/PlotViewUtil';
import {isDefined} from '../util/WebUtil.js';
import {changeHiPSProjectionCenterAndType, isHiPSAitoff} from 'firefly/visualize/WebPlot.js';
import {makeImagePt, pointEquals} from '../visualize/Point';
import {
    ANY_REPLOT, ATTACH_LAYER_TO_PLOT, CHANGE_CENTER_OF_PROJECTION, CHANGE_VISIBILITY, FORCE_DRAW_LAYER_UPDATE,
    MODIFY_CUSTOM_FIELD, SELECT_POINT
} from '../visualize/VisConst';
import {MouseState} from '../visualize/VisMouseSync';

const ID= 'EXTRACT_HIPS_FILE_TOOL';
const TYPE_ID= `${ID}_TYPE`;


let lastDownClick= undefined;

const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,null,undefined);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

let idCnt=0;

function getTargetOrders(plot) {
    const maxOrder = Number(plot.hipsProperties?.hips_order);
    const {norder} = getHiPSNorderlevel(plot, true);
    return { maxOrder, norder};
}

function dispatchSelectPoint(mouseStatePayload) {
    const {plotId,screenPt,drawLayer}= mouseStatePayload;
    if (mouseStatePayload.shiftDown || !drawLayer.drawData.data) return;
    let {plot}= currentP(plotId);
    if (!plot?.hasFits) return;
    const center= getCenterOfProjection(plot);
    if (lastDownClick && (!pointEquals(center, lastDownClick?.center) || lastDownClick.plotId!==plotId)) return;

    setTimeout(() => {
        plot= refreshP(plot);
        if (!plot) return;
        const cc= CsysConverter.make(currentP(plotId).plot);
        const pt= cc.getWorldCoords(screenPt);
        if (!pt) return;

        const {maxOrder,norder}= getTargetOrders(plot);
        const orderToUse = plot.hasFitsCube ? maxOrder : norder;
        const cell = getHealpixCellAtNorder(orderToUse, pt, plot.dataCoordSys);
        const oldCell= plot.attributes[PlotAttribute.ACTIVE_HIPS_CELL] ?? {};
        if (oldCell.ipix!==cell.ipix || oldCell.norder!==cell.norder) {
            dispatchAttributeChange( {plotId,
                changes: {
                    [PlotAttribute.ACTIVE_HIPS_CELL]: cell,
                    [PlotAttribute.ACTIVE_HIPS_NORDER]: orderToUse,
                }});
            dispatchForceDrawLayerUpdate(drawLayer.drawLayerId, plotId);
        }
    },0);
}


function saveLastDown(mouseStatePayload) {
    const {plotId}= mouseStatePayload;
    lastDownClick= {center:getCenterOfProjection(currentP(plotId).plot), plotId};
}

function creator(initPayload, presetDefaults) {

    let drawingDef= makeDrawingDef('magenta', {lineWidth:1, size:6} );
    drawingDef= Object.assign(drawingDef,presetDefaults);


    idCnt++;

    const pairs= {
        [MouseState.UP.key]: dispatchSelectPoint,
        [MouseState.DOWN.key]: saveLastDown
    };
    const actionTypes= [SELECT_POINT];

    const options= {
        hasPerPlotData:true,
        isPointData:false,
        canUserChangeColor: ColorChangeType.DYNAMIC,
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, {}, options, drawingDef, actionTypes, pairs);
}

function getLayerChanges(drawLayer, action) {
    switch (action.type) {
        case CHANGE_CENTER_OF_PROJECTION:
        case ANY_REPLOT:
        case FORCE_DRAW_LAYER_UPDATE:
            return {drawData:computeDrawData(drawLayer,action)};
        case ATTACH_LAYER_TO_PLOT:
            const {plotId} = action.payload;
            let {plotIdAry}= action.payload;

            if (!plotIdAry && !plotId) return null;
            plotIdAry = plotIdAry ? plotIdAry : [plotId];

            const title= Object.assign({},drawLayer.title);
            plotIdAry.forEach( (id) => title[id]= getTitle());

            return {title, drawData:computeDrawData(drawLayer,action,) };
        case MODIFY_CUSTOM_FIELD:
            return dealWithMods(drawLayer,action);
        case CHANGE_VISIBILITY:
            if (action.payload.visible) {
                return {drawData:computeDrawData(drawLayer,action, true)};
            }
    }
    return null;
}


function getTitle() {
    return 'Extract HiPS Tile';
}


function dealWithMods(drawLayer,action) {
    // for future use
}

function computeDrawData(drawLayer,action, isVisible = false) {
    const {payload}= action;
    const plotIdAry= payload.plotId ? getAllPlotViewIdByOverlayLock(visRoot(), payload.plotId, false, true) : payload.plotIdAry;
    if (plotIdAry) {
        const drawData= {data: {...drawLayer.drawData.data}};
        const projectionTypeChange= isDefined(payload.fullSky);

        plotIdAry.forEach( (plotId) => {
            if (plotId && (isDrawLayerVisible(drawLayer, plotId) || isVisible)) {
                drawData.data[plotId] = computeDrawDataForId(plotId, projectionTypeChange);
            } else {
                drawData.data[plotId] = null;
            }
        });
        return drawData;
    }
    else {
        return drawLayer.drawData;
    }
}

function computeDrawDataForId(plotId, projectionTypeChange) {
    let {plot} = currentP(plotId);

    const cell= plot?.attributes[PlotAttribute.ACTIVE_HIPS_CELL];
    const markedNorder= plot?.attributes[PlotAttribute.ACTIVE_HIPS_NORDER];
    if (!plot?.hasFits || !cell) return undefined;
    let aitoff = isHiPSAitoff(plot);
    const {maxOrder,norder}= getTargetOrders(plot);
    if (isNaN(maxOrder)) return undefined;

    if (projectionTypeChange) {
        aitoff = !aitoff;
        plot = changeHiPSProjectionCenterAndType(plot, undefined, aitoff);
    }

    const cc = CysConverter.make(plot);
    const scrCorners = cell.wpCorners.map((corner) => cc.getImageCoords(corner));
    if (scrCorners.some((scrC) => !scrC)) return undefined;

    const s1 = cc.getImageCoords(cell.wpCorners[0]);
    const s2 = cc.getImageCoords(cell.wpCorners[2]);
    const drawAry = [FootprintObj.make([scrCorners])];
    if (s1 && s2) {
        drawAry.push(
             ShapeDataObj.makeTextWithOffset(
                 norder<maxOrder ? makeImagePt(-30, -30) : makeImagePt(-10, -30),
                makeImagePt((s1.x + s2.x) / 2, (s1.y + s2.y) / 2),
                `Extract: ${markedNorder} / ${cell.ipix}`
            )
        );
    }

    return drawAry;
}


