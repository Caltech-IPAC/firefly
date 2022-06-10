/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import DrawLayerCntlr, {dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {primePlot, isActivePlotView, getCenterOfProjection} from '../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import PointDataObj from '../visualize/draw/PointDataObj.js';
import {DrawSymbol} from '../visualize/draw/DrawSymbol.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {clone} from '../util/WebUtil.js';
import ShapeDataObj, {UnitType} from '../visualize/draw/ShapeDataObj.js';
import {makeDevicePt, makeImagePt, makeWorldPt, pointEquals} from 'firefly/visualize/Point.js';
import {SelectedShape} from './SelectedShape.js';

const ID= 'SEARCH_SELECT_TOOL';
const TYPE_ID= 'SEARCH_SELECT_TOOL_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,onDetach,null);

const RADIUS= 'RADIUS';
const DIAMETER= 'DIAMETER'; // todo - no yet implemented
const AREA= 'AREA'; // todo - no yet implemented

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

let idCnt=0;
let lastProjectionCenter= undefined;

function dispatchSelectPoint(mouseStatePayload) {
    const {plotId,screenPt,drawLayer,shiftDown}= mouseStatePayload;
    if (shiftDown || !drawLayer.drawData.data) return;
    const plot= primePlot(visRoot(),plotId);
    const cc= CsysConverter.make(plot);
    if (!plot) return;
    const wp= cc.getWorldCoords(screenPt);
    const center= getCenterOfProjection(plot);
    if (lastProjectionCenter && (!pointEquals(center, lastProjectionCenter?.center) || lastProjectionCenter.plotId!==plotId)) return;
    lastProjectionCenter= undefined;

    if (plot.attributes[PlotAttribute.USE_POLYGON]){
        const ptAry= plot.attributes[PlotAttribute.POLYGON_ARY];
        if (!ptAry?.length) return;
        const relPloygonAry= plot.attributes[PlotAttribute.RELATIVE_IMAGE_POLYGON_ARY];
        if (!relPloygonAry) return;
        const dp= cc.getImageCoords(wp);
        const polygonAry= relPloygonAry.map( (pt) => cc.getWorldCoords( makeImagePt(dp.x-pt.x, dp.y-pt.y)));
        dispatchAttributeChange({plotId,changes: {
                [PlotAttribute.POLYGON_ARY] : polygonAry,
            }});
    }
    else {
        dispatchAttributeChange( {plotId, changes:{[PlotAttribute.USER_SEARCH_WP]:wp} });
    }
    dispatchForceDrawLayerUpdate(drawLayer.drawLayerId, plotId);
}

function saveLastDown(mouseStatePayload) {
    const {plotId}= mouseStatePayload;
    const plot= primePlot(visRoot(),plotId);
    lastProjectionCenter= {center:getCenterOfProjection(plot), plotId};
}

function onDetach(drawLayer,action) {
    action.payload.plotIdAry?.forEach( (plotId) => {
        if (primePlot(visRoot(),plotId)?.attributes[PlotAttribute.USER_SEARCH_WP]) return;
        dispatchAttributeChange({plotId, overlayColorScope:false, changes:{[PlotAttribute.USER_SEARCH_WP]:undefined}});
    });
}

function creator({minSize=1/3600,maxSize=100, searchType=RADIUS}={}, presetDefaults) {
    const drawingDef= { ...makeDrawingDef('yellow'), symbol: DrawSymbol.DIAMOND, size: 8, ...presetDefaults };
    idCnt++;
    const pairs= {
        [MouseState.UP.key]: dispatchSelectPoint,
        [MouseState.DOWN.key]: saveLastDown
    };
    const actionTypes= [DrawLayerCntlr.SELECT_POINT];
    const options = {
        isPointData: false,
        hasPerPlotData: true,
        canUserDelete: false,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        destroyWhenAllDetached : true,
        minSize,
        maxSize,
        searchType,
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, 'Search Select Tool', options, drawingDef, actionTypes, pairs);
}

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    if (dataType!==DataTypes.DATA) return undefined;
    const active= isActivePlotView(visRoot(), plotId);
    const drawAry= drawSearchSelection(drawLayer,action, active, plotId);
    return drawAry || lastDataRet;
}

function getLayerChanges(drawLayer, action) {
    switch (action.type) {
        case DrawLayerCntlr.CHANGE_DRAWING_DEF:
            return {drawingDef: clone(drawLayer.drawingDef,action.payload.drawingDef)};
    }
}

function drawSearchSelection(drawLayer, action, active, plotId) {
    const {plotIdAry}= action.payload;
    const plot= primePlot(visRoot(),plotId||plotIdAry?.[0]);
    if (!plot) return [];
    return plot.attributes[PlotAttribute.USE_POLYGON] ?
        drawSearchSelectionPolygon(plot, drawLayer) : drawSearchSelectionCircle(plot, drawLayer);
}

function drawSearchSelectionCircle(plot, drawLayer) {
    const {minSize,maxSize}= drawLayer;
    const wp= plot.attributes[PlotAttribute.USER_SEARCH_WP];
    if (!wp) return [];
    const radius= plot.attributes[PlotAttribute.USER_SEARCH_RADIUS_DEG];
    const drawAry= [ PointDataObj.make(wp,7, DrawSymbol.EMP_SQUARE_X)];
    const drawRadius= radius<=maxSize ? (radius >= minSize ? radius : minSize) : maxSize;
    radius && drawAry.push( {...ShapeDataObj.makeCircleWithRadius(wp, drawRadius*3600,UnitType.ARCSEC), lineWidth:3} );
    return drawAry;
}

function drawSearchSelectionPolygon(plot, drawLayer) {
    const wpAry= plot?.attributes[PlotAttribute.POLYGON_ARY];
    if (!(wpAry?.length>2)) return [];
    const drawAry= [{...ShapeDataObj.makePolygon(wpAry), lineWidth:3 }];
    return drawAry;
}
