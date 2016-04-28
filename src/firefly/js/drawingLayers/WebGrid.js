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
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import AppDataCntlr from '../core/AppDataCntlr.js';
import CoordinateSys from '../visualize/CoordSys.js';
import {get} from 'lodash';


export const COORDINATE_PREFERENCE = 'Coordinate';

const  coordinateArray = [
     {coordName:'eq2000hms',        csys:CoordinateSys.EQ_J2000},
     {coordName:'eq2000dcm',        csys:CoordinateSys.EQ_J2000},
     {coordName:'eqb1950hms',       csys:CoordinateSys.EQ_B1950},
     {coordName:'eqb1950dcm',       csys: CoordinateSys.EQ_B1950},
     {coordName:'galactic',         csys:CoordinateSys.GALACTIC},
     {coordName:'superGalactic',    csys:CoordinateSys.SUPERGALACTIC},
     {coordName:'epj2000',          csys:CoordinateSys.ECL_J2000},
     {coordName:'epb1950',          csys:CoordinateSys.ECL_B1950}

 ];

const ID= 'WEB_GRID';
const TYPE_ID= 'WEB_GRID_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID,creator, getDrawData,getLayerChanges, null,getUIComponent);
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

     return lastDataRet ||getData(plot, cc) ;

 }

 /**
  * This method is called when the user's has customer field changes
  * @param drawLayer - drawLayer object
  * @param action  - the action object which may contains user's custom fields
  * @returns {*} - a new object which contains the new changes and the null data
  */
 function getLayerChanges(drawLayer, action) {
     if  (action.type!==DrawLayerCntlr.MODIFY_CUSTOM_FIELD) return null; // don't do anything
     //get the changes in the action payload
     const {changes}= action.payload.hasOwnProperty('changes') ? action.payload:null;
     //If the customer field is the same as the drawLayer's, update the drawData, if not return null
     if (changes!==drawLayer.changes) return null;
     const drawDataObj= Object.assign({},drawLayer.drawData);
     //clear the data inside the drawDataObj object
     drawDataObj.data= null;
     //return a new object with a null data in the drawData
     return Object.assign({changes}, {drawData: drawDataObj});
 }

 /**
  * This method prepare the input parameters needed for calculating the drawing data array
  * @param plot - primePlot object
  * @returns {{width: (dataWidth|*), height: (*|dataHeight), screenWidth: *, csys: *, labelFormat: string}}
  */
 export function getDrawLayerParameters(plot){
     var width = plot.dataWidth;
     var height = plot.dataHeight;
     var screenWidth = plot.screenSize.width;

     var csysName = AppDataCntlr.getPreference(COORDINATE_PREFERENCE);
     if (!csysName) {
         csysName = 'eq2000hms';//set default
     }
     var csys=getCoordinateSystem(csysName);
     var labelFormat=csysName.endsWith('hms')? 'hms':'dcm';

     return {width, height, screenWidth, csys,labelFormat};
 }

 /**
  * Return a Coordinate object for a given coordinate name
  * @param csysName - the string expression of the coordinate system
  * @returns the corresponding CoordSys object
  */

 function getCoordinateSystem(csysName) {

     for (let i = 0; i < coordinateArray.length; i += 1) {
         if (get(coordinateArray[i], 'coordName') === csysName) {
             return coordinateArray[i].csys;

         }
     }

 }
