/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import PlotViewUtil from '../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import PointDataObj, {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import DrawLayer, {DataTypes} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';

const ID= 'ACTIVE_TARGET';
const TYPE_ID= 'ACTIVE_TARGET_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,null,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;

//function dispatchInitActiveTarget() {
//    DrawLayerCntlr.dispatchCreateDrawLayer(creator());
//}

function creator(initPayload) {
    var drawingDef= makeDrawingDef('blue');
    drawingDef.symbol= DrawSymbol.CIRCLE;
    idCnt++;
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID,
                                        {hasPerPlotData:true, isPointData:true},
                                         drawingDef);
}



function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {

    switch (dataType) {
        case DataTypes.DATA:
            return computeDrawLayer(plotId);
            break;
        case DataTypes.HIGHLIGHT_DATA:
            break;
        case DataTypes.SELECTED_IDX_ARY:
            break;
    }
    return null;
}



function computeDrawLayer(plotId) {
    if (!plotId) return null;
    var pv= PlotViewUtil.getPlotViewById(plotId);
    var wp= pv.primaryPlot.attributes[PlotAttribute.FIXED_TARGET];
    return wp ? [PointDataObj.make(wp)] : [];
}


