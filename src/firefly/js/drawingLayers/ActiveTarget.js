/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import PlotViewUtil from '../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import PointDataObj, {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {formatPosForTextField} from '../data/form/PositionFieldDef.js';

const ID= 'ACTIVE_TARGET';
const TYPE_ID= 'ACTIVE_TARGET_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;


function creator(initPayload) {
    var drawingDef= makeDrawingDef('blue');
    drawingDef.symbol= DrawSymbol.CIRCLE;
    idCnt++;
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, {},
                                        {hasPerPlotData:true, isPointData:true},
                                         drawingDef);
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {

    switch (dataType) {
        case DataTypes.DATA:
            return lastDataRet || computeDrawLayer(plotId);
            break;
        case DataTypes.HIGHLIGHT_DATA:
            break;
        case DataTypes.SELECTED_IDX_ARY:
            break;
    }
    return null;
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
    var pv= PlotViewUtil.getPlotViewById(visRoot(),plotId);
    var retval= 'Query Object';
    if (pv && pv.primaryPlot.attributes[PlotAttribute.FIXED_TARGET]) {
        var wp= pv.primaryPlot.attributes[PlotAttribute.FIXED_TARGET];
        if (wp)  retval=  wp.objName ? wp.objName :formatPosForTextField(wp);
    }
    return retval;
}

function computeDrawLayer(plotId) {
    if (!plotId) return null;
    var pv= PlotViewUtil.getPlotViewById(visRoot(),plotId);
    var wp= pv.primaryPlot.attributes[PlotAttribute.FIXED_TARGET];
    return wp ? [PointDataObj.make(wp)] : [];
}


