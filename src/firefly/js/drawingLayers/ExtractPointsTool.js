/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import DrawLayerCntlr, {dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {primePlot, isActivePlotView} from '../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import PointDataObj from '../visualize/draw/PointDataObj.js';
import {DrawSymbol} from '../visualize/draw/DrawSymbol.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {flux} from '../core/ReduxFlux.js';
import {clone} from '../util/WebUtil.js';
import {isEmpty} from 'lodash';

const ID= 'POINT_EXTRACTION_TOOL';
const TYPE_ID= 'POINT_EXTRACTION_TOOL_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,onDetach,null);


export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;

function dispatchSelectPoint(mouseStatePayload) {
    const {plotId,screenPt,drawLayer,shiftDown}= mouseStatePayload;
    if (shiftDown) return;
    if (drawLayer.drawData.data) {
        const plot= primePlot(visRoot(),plotId);
        const ptAry= plot.attributes[PlotAttribute.PT_ARY] ?? [];
        const cc= CsysConverter.make(plot);
        const newPtAry= ptAry.filter( (pt) => cc.pointInPlot(pt));
        const newPt= makeSelectedPt(screenPt,plotId);
        if (newPt) newPtAry.push(newPt);
        dispatchAttributeChange(
            {plotId, changes:{[PlotAttribute.PT_ARY]:newPtAry} });
        flux.process({type:DrawLayerCntlr.EXTRACT_POINT, payload:mouseStatePayload} );
        dispatchForceDrawLayerUpdate(drawLayer.drawLayerId, plotId);
    }
}


function onDetach(drawLayer,action) {
    const {plotIdAry}= action.payload;
    plotIdAry?.forEach( (plotId) => {
        if (primePlot(visRoot(),plotId)?.attributes[PlotAttribute.PT_ARY]) {
            dispatchAttributeChange(
                { plotId, overlayColorScope:false,
                    changes:{[PlotAttribute.PT_ARY]:undefined,
                             [PlotAttribute.SELECT_ACTIVE_CHART_PT]: undefined } }
            );
        }
    });
}



function creator(initPayload, presetDefaults) {
    const drawingDef= { ...makeDrawingDef('pink'), symbol: DrawSymbol.DIAMOND, size: 8, ...presetDefaults };
    idCnt++;

    const pairs= {
        [MouseState.UP.key]: dispatchSelectPoint
    };
    const actionTypes= [DrawLayerCntlr.SELECT_POINT];
    const options = {
        isPointData: true,
        hasPerPlotData: true,
        canUserDelete: false,
        canUserChangeColor: ColorChangeType.DYNAMIC
    };

    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, 'Point Extractor Tools', options, drawingDef, actionTypes, pairs);
}



function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {

    if (dataType!==DataTypes.DATA) return undefined;
    const active= isActivePlotView(visRoot(), plotId);
    const drawAry= drawPoints(drawLayer,action, active, plotId);
    return drawAry || lastDataRet;
}



function getLayerChanges(drawLayer, action) {
    switch (action.type) {
        case DrawLayerCntlr.CHANGE_DRAWING_DEF:
            return {drawingDef: clone(drawLayer.drawingDef,action.payload.drawingDef)};
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            return dealWithMods(drawLayer,action);
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            return attach(drawLayer);
        case DrawLayerCntlr.FORCE_DRAW_LAYER_UPDATE:
            return saveLastData(action.payload.plotIdAry[0]);
    }
}


function attach(drawLayer) {

    const plotId= drawLayer.plotIdAry[0];
    const data= drawLayer.lastData;
    if (!plotId || !data) return;
    setTimeout( () => {
        drawLayer.plotIdAry.forEach( (pId) => {
            const p= primePlot(visRoot(),pId);
            if (p && !p.attributes[PlotAttribute.PT_ARY]) {
                dispatchAttributeChange({plotId:pId, changes:{[PlotAttribute.PT_ARY]:data}, toAllPlotsInPlotView:false});
                dispatchForceDrawLayerUpdate(drawLayer.drawLayerId, pId);
            }
        });
    }, 3);
}

function saveLastData(plotId) {
    const plot= primePlot(visRoot(),plotId);
    return {lastData:plot?.attributes[PlotAttribute.PT_ARY]};
}

function dealWithMods(drawLayer,action) {
    const {changes,plotIdAry}= action.payload;
    if (Object.keys(changes).includes('activePt')) {
        let plot= primePlot(visRoot());
        if (!plotIdAry.includes(plot.plotId)) plot= primePlot(visRoot(),plotIdAry[0]);
        const cc= CsysConverter.make(plot);
        if (!cc) return {};
        // const {activePt}= changes;
        return changes;
    }
    return {};
}





function makeSelectedPt(screenPt,plotId) {
    const plot= primePlot(visRoot(),plotId);
    const cc= CsysConverter.make(plot);
    const selPt= cc.getWorldCoords(screenPt); //todo put back
    return (selPt) ? selPt : cc.getImageCoords(screenPt);
}


function drawPoints(drawLayer, action, active, plotId) {
    const {plotIdAry}= action.payload;

    const isEmptyData = () => {
        const data = drawLayer?.drawData?.data;
        if (isEmpty(data)) return true;
        return Object.values(data).every((v) => isEmpty(v));
    };
    // attach drawing layer to the plot which is created after the drawing layer
    if ( action.type === DrawLayerCntlr.ATTACH_LAYER_TO_PLOT &&
        !isEmptyData() && plotIdAry && plotIdAry.includes(plotId))  {
        if (drawLayer.plotIdAry) {
            let dAry;

            const prevPID = drawLayer.plotIdAry.find((pid) => {
                dAry = drawLayer?.drawData?.data[pid];
                return dAry?.length;
            });

            return !prevPID ? undefined : dAry;
        }
    }


    const plot= primePlot(visRoot(),plotId||plotIdAry?.[0]);
    if (!plot) return [];
    const ptAry= plot.attributes[PlotAttribute.PT_ARY] ?? [];

    const drawAry= ptAry.map( (pt) => PointDataObj.make(pt));

    if (drawLayer.activePt) {
        drawAry.push( PointDataObj.make(drawLayer.activePt,7, DrawSymbol.EMP_SQUARE_X));
    }
    return drawAry;
}
