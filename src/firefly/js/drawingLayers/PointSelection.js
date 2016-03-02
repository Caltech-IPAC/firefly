/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {primePlot, isActivePlotView} from '../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import PointDataObj, {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {MouseState} from '../visualize/VisMouseCntlr.js';
import {flux} from '../Firefly.js';

const ID= 'POINT_SELECTION';
const TYPE_ID= 'POINT_SELECTION_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,onDetach,null);


export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;

function dispatchSelectPoint(mouseStatePayload) {
    var {plotId,screenPt,drawLayer}= mouseStatePayload;
    if (drawLayer.drawData.data) {
        flux.process({type:DrawLayerCntlr.SELECT_POINT, payload:mouseStatePayload} );
        dispatchAttributeChange(plotId,true,PlotAttribute.ACTIVE_POINT,{pt:makeSelectedPt(screenPt,plotId)});
    }
}


function onDetach(drawLayer,action) {
    var {plotIdAry}= action.payload;
    plotIdAry.forEach( (plotId) => dispatchAttributeChange(plotId,false,PlotAttribute.ACTIVE_POINT,null));
}



function creator(initPayload) {
    var drawingDef= makeDrawingDef('pink');
    drawingDef.symbol= DrawSymbol.CIRCLE;
    idCnt++;

    var pairs= {
        [MouseState.UP.key]: dispatchSelectPoint
    };

    var actionTypes= [DrawLayerCntlr.SELECT_POINT];

    var options= {
        isPointData:true,
        hasPerPlotData:true,
        canUserChangeColor: ColorChangeType.DYNAMIC
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, 'Selected Point', options, drawingDef, actionTypes, pairs);
}



function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {

    if (dataType!==DataTypes.DATA) return null;

    if (isActivePlotView(visRoot(), plotId)) {
        return selectAPoint(drawLayer,action, true);
    }
    else {
        return selectAPoint(drawLayer,action, false);
    }

}



function getLayerChanges(drawLayer, action) {
    //switch (action.type) {
    //    case DrawLayerCntlr.SELECT_POINT:
    //        return selectAPoint(drawLayer,action);
    //        break;
    //    case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
    //        break;
    //}
    return null;
}

function makeSelectedPt(screenPt,plotId) {
    var plot= primePlot(visRoot(),plotId);
    var cc= CsysConverter.make(plot);
    var selPt= cc.getWorldCoords(screenPt);
    if (!selPt) selPt= cc.getImageCoords(screenPt);
    return selPt;
}


function selectAPoint(drawLayer, action, active) {
    var {screenPt,plotId}= action.payload;
    var selPt= makeSelectedPt(screenPt,plotId);
    var drawAry;
    if (active) {
        drawAry= [
            PointDataObj.make(selPt,5,DrawSymbol.CIRCLE),
            PointDataObj.make(selPt,5,DrawSymbol.SQUARE)
        ];
    }
    else {
        drawAry= [PointDataObj.make(selPt,6,DrawSymbol.CIRCLE)];
    }
    return  drawAry;
}
