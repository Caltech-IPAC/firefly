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
import ShapeDataObj, {UnitType} from '../visualize/draw/ShapeDataObj.js';
import {makeImagePt, makeScreenPt, pointEquals} from 'firefly/visualize/Point.js';
import {getScreenPixScaleArcSec} from '../visualize/WebPlot';
import {SelectedShape} from './SelectedShape.js';
import {clampInRange} from 'firefly/util/MathUtil';
import {logger} from 'firefly/util/Logger';

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
    if (!drawLayer.isInteractive) return;
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
        if (!ptAry?.length) {
            const {x,y}= screenPt;
            const scale= 250/getScreenPixScaleArcSec(plot);
            const pAry= [
                cc.getWorldCoords( makeScreenPt(x-scale,y-scale)),
                cc.getWorldCoords( makeScreenPt(x+scale,y-scale)),
                cc.getWorldCoords( makeScreenPt(x+scale,y+scale)),
                cc.getWorldCoords( makeScreenPt(x-scale,y+scale))
            ];
            dispatchAttributeChange({plotId,changes: {
                    [PlotAttribute.POLYGON_ARY] : pAry,
                    [PlotAttribute.USER_SEARCH_WP]:wp
            }});
        }
        else {
            const relPloygonAry= plot.attributes[PlotAttribute.RELATIVE_IMAGE_POLYGON_ARY];
            if (!relPloygonAry) return;
            const dp= cc.getImageCoords(wp);
            const polygonAry= relPloygonAry.map( (pt) => cc.getWorldCoords( makeImagePt(dp.x-pt.x, dp.y-pt.y)));
            dispatchAttributeChange({plotId,changes: {
                    [PlotAttribute.POLYGON_ARY] : polygonAry,
                    [PlotAttribute.USER_SEARCH_WP]:wp
                }});
        }
    }
    else if (plot.attributes[PlotAttribute.USE_BOX]) {
        dispatchAttributeChange( {plotId, changes:{
                [PlotAttribute.SELECTION_TYPE]: SelectedShape.rect.key,
                [PlotAttribute.USER_SEARCH_WP]:wp
            }
        });
    }
    else {
        dispatchAttributeChange( {plotId, changes:{
                [PlotAttribute.SELECTION_TYPE]: SelectedShape.circle.key,
                [PlotAttribute.USER_SEARCH_WP]:wp
            }
        });
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

function creator({minSize=1/3600,maxSize=100, searchType=RADIUS}={}, presetDefaults, color='yellow') {
    const drawingDef= { ...makeDrawingDef(color), symbol: DrawSymbol.DIAMOND, size: 8, ...presetDefaults };
    idCnt++;
    const pairs= {
        [MouseState.UP.key]: dispatchSelectPoint, // point selection event on image will move the search region to that point
        [MouseState.DOWN.key]: saveLastDown
        // TODO: add events and listeners for interaction with rotation and resize handles
    };
    const actionTypes= [DrawLayerCntlr.SELECT_POINT];
    const options = {
        isPointData: false,
        hasPerPlotData: true,
        canUserDelete: false,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        destroyWhenAllDetached : false,
        minSize,
        maxSize,
        searchType,
        isInteractive: true
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
            return {drawingDef: {...drawLayer.drawingDef,...action.payload.drawingDef}};
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            return {...action.payload.changes};
    }
}

function drawSearchSelection(drawLayer, action, active, plotId) {
    const {plotIdAry}= action.payload;
    const plot= primePlot(visRoot(),plotId||plotIdAry?.[0]);
    if (!plot) return [];
    if (plot.attributes[PlotAttribute.USE_POLYGON]) return drawSearchSelectionPolygon(plot, drawLayer);
    if (plot.attributes[PlotAttribute.USE_BOX]) return drawSearchSelectionTransformBox(plot, drawLayer);
    return drawSearchSelectionCircle(plot, drawLayer);
}

function drawSearchSelectionCircle(plot, drawLayer) {
    const {minSize,maxSize}= drawLayer;
    const wp= plot.attributes[PlotAttribute.USER_SEARCH_WP];
    if (!wp) return [];
    const radius= plot.attributes[PlotAttribute.USER_SEARCH_RADIUS_DEG];
    const drawAry= [];
    const drawRadius = clampInRange(radius, minSize, maxSize);
    const shadow={offX:1,offY:1,color:'black',blur:1};
    if (drawLayer.isInteractive) { // dispatchSelectPoint will move the draw layer on a point selection event
        const x= PointDataObj.make(wp,7, DrawSymbol.EMP_SQUARE_X,undefined,{shadow});
        drawAry.push(x);
        radius && drawAry.push( {...ShapeDataObj.makeCircleWithRadius(wp, drawRadius*3600,UnitType.ARCSEC), lineWidth:3, renderOptions:{shadow}} );
    }
    else { // dispatchSelectPoint will do nothing (isInteractive=false in search refinement tool)
        const cross= PointDataObj.make(wp,3, DrawSymbol.CROSS, undefined, {shadow});
        drawAry.push(cross);
        radius && drawAry.push(
            {...ShapeDataObj.makeCircleWithRadius(wp, drawRadius*3600,UnitType.ARCSEC),
                lineWidth:2,
                renderOptions:{lineDash:[7,3], shadow}
            });
    }
    return drawAry;
}

function drawSearchSelectionPolygon(plot, drawLayer) {
    const wpAry= plot?.attributes[PlotAttribute.POLYGON_ARY];
    if (!wpAry || wpAry.length<3) return [];
    return [ drawLayer.isInteractive ?
        {...ShapeDataObj.makePolygon(wpAry), lineWidth:3 } : // movable layer
        {...ShapeDataObj.makePolygon(wpAry), lineWidth:1,  renderOptions:{lineDash:[5,5]}}]; // locked layer
}

function drawSearchSelectionTransformBox(plot, drawLayer) {
    const wp= plot.attributes[PlotAttribute.USER_SEARCH_WP];
    // const box = plot.attributes[PlotAttribute.USER_SEARCH_BOX];
    const wpAry = plot?.attributes[PlotAttribute.POLYGON_ARY];
    const rotAxisWp = plot.attributes[PlotAttribute.USER_SEARCH_BOX_AXIS_WP];
    if (!wp || !wpAry) return [];
    logger.debug('Box SearchSelectTool Draw Layer:', {center: wp.toString(), corners: wpAry.map((wp)=>wp.toString()), rotAxis: rotAxisWp.toString()});

    // TODO: draw rotate and resize handles when the box is selected (similar to footprint selection behavior)
    return [
        PointDataObj.make(wp,7, DrawSymbol.EMP_SQUARE_X,undefined),
        {...ShapeDataObj.makePolygon(wpAry), lineWidth:3},
        {...ShapeDataObj.makeLine(wp, rotAxisWp, true), lineWidth:2, renderOptions:{lineDash:[5,5]}},
    ];
}
