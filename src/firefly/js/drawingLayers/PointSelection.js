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
import {MouseState} from '../visualize/VisMouseSync.js';
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
        dispatchAttributeChange(plotId,true,PlotAttribute.ACTIVE_POINT,{pt:makeSelectedPt(screenPt,plotId),true});
    }
}


function onDetach(drawLayer,action) {
    var {plotIdAry}= action.payload;
    plotIdAry.forEach( (plotId) => dispatchAttributeChange(plotId,false,PlotAttribute.ACTIVE_POINT,null,true));
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
    var active= isActivePlotView(visRoot(), plotId);
    var drawAry= selectAPoint(drawLayer,action, active);
    return drawAry || lastDataRet;
}



function getLayerChanges(drawLayer, action) {
    return null;
}

function makeSelectedPt(screenPt,plotId) {
    var plot= primePlot(visRoot(),plotId);
    var cc= CsysConverter.make(plot);
    var selPt= cc.getWorldCoords(screenPt); //todo put back

    if (!selPt) selPt= cc.getImageCoords(screenPt);
    return selPt;
}


function selectAPoint(drawLayer, action, active) {
    var {screenPt,plotId}= action.payload;
    if (!screenPt) return null;
    var selPt= makeSelectedPt(screenPt,plotId);
    if (!selPt) return null;
    var drawAry;
    if (active) {
        drawAry= [
            PointDataObj.make(selPt,5,DrawSymbol.CIRCLE),
            PointDataObj.make(selPt,5,DrawSymbol.SQUARE)
        ];
    }
    else {
        drawAry= [PointDataObj.make(selPt,5,DrawSymbol.CIRCLE)];
    }
    return  drawAry;
}
