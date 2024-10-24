 /*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 * 4/14/16
 */


import  {get, isBoolean} from 'lodash';
import {clone, isDefined} from '../util/WebUtil.js';
import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getUIComponent} from './WebGridUI.jsx';
import { makeGridDrawData } from './ComputeWebGridData.js';
import {changeHiPSProjectionCenterAndType, isHiPSAitoff, isImage} from '../visualize/WebPlot.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {getPreference} from '../core/AppDataCntlr.js';
import {flux} from '../core/ReduxFlux.js';
import CoordinateSys from '../visualize/CoordSys.js';


export const COORDINATE_PREFERENCE = 'coordinate';

const coordinateArray = [
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
const UPDATE_GRID= 'Grid.UpdateGrid'; // a 'private' action just for grid, dispatch by grid
const factoryDef= makeFactoryDef(TYPE_ID,creator, getDrawData,getLayerChanges, null,getUIComponent);
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID
/**
 * Color used to draw the grid
 **/
const  DEF_GRID_COLOR = 'green';
 
var idCnt=0;

/**
 *  @param  params
 * @return {Function}
 */
function creator(params) {

    const drawingDef= makeDrawingDef( DEF_GRID_COLOR);
    const useLabels= isBoolean(params.useLabels) ? params.useLabels : true;
    const id= params.drawLayerId || `${ID}-${idCnt}`;
    const options= {
        hasPerPlotData:true,
        isPointData:false,
        useLabels,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        destroyWhenAllDetached: false,
        destroyWhenAllUserDetached: true,
    };
    
    return DrawLayer.makeDrawLayer( id, TYPE_ID, 'Grid', options , drawingDef, [ImagePlotCntlr.UPDATE_VIEW_SIZE, UPDATE_GRID ]);
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

     const projectionTypeChange= action.type===ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION && isDefined(action.payload.fullSky);
     if (projectionTypeChange) {
         let aitoff= isHiPSAitoff(plot);
         aitoff= !aitoff;
         plot= changeHiPSProjectionCenterAndType(plot,undefined,aitoff);
     }

     const cc= CsysConverter.make(plot);
     if (!cc) return null;

   return lastDataRet ||makeGridDrawData(plot, cc, drawLayer.useLabels) ;

 }

 /**
  * This method is called when the user's has custom field changes
  * @param drawLayer - drawLayer object
  * @param action  - the action object which may contains user's custom fields
  * @returns {*} - a new object which contains the new changes and the null data
  */
 function getLayerChanges(drawLayer, action) {
     let drawData;
     switch (action.type){
         case ImagePlotCntlr.ANY_REPLOT:
             if (drawLayer.drawData?.data  ) {
                 if (action.payload.plotIdAry){
                    const data= clone(drawLayer.drawData.data);
                    action.payload.plotIdAry.forEach( (plotId) => data[plotId]= null);
                    drawData= clone(drawLayer.drawData, {data});
                 }
                 else {
                     drawData= Object.assign({},drawLayer.drawData, {data:null});
                 }
                 return {drawData};
             }
             break;
         case ImagePlotCntlr.UPDATE_VIEW_SIZE:
             setTimeout(() => {
                 flux.process({ type: UPDATE_GRID, payload: { plotId: action.payload.plotId}});
             });
             break;
         case UPDATE_GRID:
         case ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION:
             if (drawLayer.drawData?.data ) {
                 const data = Object.keys(drawLayer.drawData.data).reduce((d, plotId) => {
                     d[plotId] = isImage(primePlot(visRoot(), plotId)) ? drawLayer.drawData.data[plotId] : null;
                     return d;
                 }, {});
                 drawData= Object.assign({},drawLayer.drawData, {data});
                 return {drawData};
             }
             break;
         case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
             const {coordinate}= action.payload.changes;
             if (coordinate !== drawLayer.coordinate ) {
                 const drawData= Object.assign({},drawLayer.drawData, {data:null});
                 return { coordinate, drawData};
             }
             break;
     }
     return null;
     
 }

 /**
  * This method prepare the input parameters needed for calculating the drawing data array
  * @param plot - primePlot object
  * @returns {{width: (dataWidth|*), height: (*|dataHeight), screenWidth: *, csys: *, labelFormat: string}}
  */
 export function getDrawLayerParameters(plot){
     const prefCsysName = getPreference(COORDINATE_PREFERENCE);
     const nameList= coordinateArray.map(({coordName}) => coordName);
     const csysName= nameList.includes(prefCsysName) ? prefCsysName : 'eq2000hms';

     return {width:plot.dataWidth, height:plot.dataHeight, screenWidth:plot.screenSize.width,
         csys:getCoordinateSystem(csysName),labelFormat:csysName.endsWith('hms')? 'hms':'dcm'};
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
