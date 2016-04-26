 /*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 * 4/14/16
 */

import  {visRoot} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getUIComponent} from './WebGridUI.jsx';
import { getData } from './ComputeWebGridData.js';
 import DrawLayerCntlr, {dispatchModifyCustomField, dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerCntlr.js';



const ID= 'WEB_GRID';
const TYPE_ID= 'WEB_GRID_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID,creator, getDrawData,null, null,getUIComponent);
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID
/**
 * Color used to draw the grid
 **/
const  DEF_GRID_COLOR = 'green';
var idCnt=0;


/**
 *
 * @return {Function}
 */
function creator() {

    var drawingDef= makeDrawingDef( DEF_GRID_COLOR);
    var options= {
        hasPerPlotData:true,
        isPointData:false,
        canUserChangeColor: ColorChangeType.DYNAMIC
    };

    return DrawLayer.makeDrawLayer( `${ID}-${idCnt}`, TYPE_ID, 'grid',
        options , drawingDef, {});

}

 /**
  * Implement this method defined in DrawLayerFactory.js
  * @param dataType
  * @param plotId
  * @param drawLayer
  * @param action
  * @param lastDataRet
  * @returns {*}
  */

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet){

     var plot= primePlot(visRoot(),plotId);
     if (!plot)return null;

     var cc= CsysConverter.make(plot);
     if (!cc) return null;

     var drawDataArray= getData(plot, cc);
     return drawDataArray || lastDataRet ;
 }


 function getLayerChanges(drawLayer, action) {
     if  (action.type!==DrawLayerCntlr.MODIFY_CUSTOM_FIELD) return null; // don't do anything
     const {coordinate}= action.payload;                  // get new coordSys
     if (coordinate!==drawLayer.coordinate) return null;       // it not different, don't do anything
     const dd= Object.assign({},drawLayer.drawData);   // the the drawData from the layer
     dd[drawLayer.DATA]= null;                                      // clear it
     return Object.assign({coordinate:{drawData:dd}});     // return the drawLayer changes
 }

