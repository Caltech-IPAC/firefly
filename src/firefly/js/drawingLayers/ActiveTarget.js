/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty} from 'lodash'
import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import PointDataObj, {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {formatPosForTextField} from '../data/form/PositionFieldDef.js';

const ID= 'ACTIVE_TARGET';
const TYPE_ID= 'ACTIVE_TARGET_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;


function creator(initPayload, presetDefaults) {
    var drawingDef= makeDrawingDef('red');
    drawingDef.symbol= DrawSymbol.CIRCLE;
    drawingDef.size= 6;
    drawingDef= Object.assign(drawingDef,presetDefaults);

    idCnt++;

    var options= {
        hasPerPlotData:true,
        isPointData:true,
        canUserChangeColor: ColorChangeType.DYNAMIC
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, {}, options, drawingDef);
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    if (dataType!==DataTypes.DATA) return null;
    return isEmpty(lastDataRet) ? computeDrawData(plotId) : lastDataRet;
}

function getLayerChanges(drawLayer, action) {
    switch (action.type) {
        case ImagePlotCntlr.ANY_REPLOT:
            break;
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            var {plotIdAry,plotId}= action.payload;
            if (!plotIdAry && !plotId) return null;
            if (!plotIdAry) plotIdAry= [plotId];
            var title= Object.assign({},drawLayer.title);
            plotIdAry.forEach( (id) => title[id]= getTitle(id));
            return {title};
    }
    return null;
}


function getTitle(plotId) {
    var plot= primePlot(visRoot(),plotId);
    var retval= 'Query Object';
    if (plot && plot.attributes[PlotAttribute.FIXED_TARGET]) {
        var wp= plot.attributes[PlotAttribute.FIXED_TARGET];
        if (wp)  retval=  wp.objName ? wp.objName :formatPosForTextField(wp);
    }
    return retval;
}

function computeDrawData(plotId) {
    if (!plotId) return null;
    var plot= primePlot(visRoot(),plotId);
    if (!plot) return [];
    var wp= plot.attributes[PlotAttribute.FIXED_TARGET];
    return wp ? [PointDataObj.make(wp)] : [];
}


