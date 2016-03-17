/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import AppDataCntlr from '../core/AppDataCntlr.js';
import ImagePlotCntlr, {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef,Style, TextLocation} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {MouseState} from '../visualize/VisMouseCntlr.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import CsysConverter from '../visualize/CsysConverter.js';
import { makeOffsetPt, makeWorldPt, makeImagePt, makeViewPortPt,makeScreenPt} from '../visualize/Point.js';
import BrowserInfo from '../util/BrowserInfo.js';
import VisUtil from '../visualize/VisUtil.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {primePlot, getPlotViewById} from '../visualize/PlotViewUtil.js';
import {getUIComponent} from './NorthUpCompassUI.jsx';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {flux} from '../Firefly.js';
import {makeDirectionArrowDrawObj} from '../visualize/draw/DirectionArrowDrawObj.js';

const ID= 'NORTH_UP_COMPASS_TOOL';
const TYPE_ID= 'NORTH_UP_COMPASS_TYPE';


const selHelpText='North Arrow - EQ. J2000';
const editHelpText='Help text here!';

const factoryDef= makeFactoryDef(TYPE_ID,creator, getDrawData,/* getLayerChanges*/ null, onDetach,/*getUIComponent*/null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID


var idCnt=0;


/**
 *
 * @return {Function}
 */
function creator() {

    var drawingDef= makeDrawingDef('red');
    var actionTypes= [ImagePlotCntlr.UPDATE_VIEW_SIZE, ImagePlotCntlr.PROCESS_SCROLL];

    idCnt++;

    var options= {
        hasPerPlotData:true,
        isPointData:false,
        canUserChangeColor: ColorChangeType.DYNAMIC
    };
    return DrawLayer.makeDrawLayer( `${ID}-${idCnt}`, TYPE_ID, 'North Arrow - EQ. J2000',
                                     options, drawingDef, actionTypes);
}

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet){
    var drawCompass= makeCompass(plotId, action);
    return drawCompass || lastDataRet;
}

function getLayerChanges(drawLayer, action) {

    switch (action.type) {
/*        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            return attach(drawLayer,action);
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            console.log(action.type);
            break;
        case DrawLayerCntlr.FORCE_DRAW_LAYER_UPDATE:
            console.log(action.type);
            break;*/
        case DrawLayerCntlr.PROCESS_SCROLL:
            console.log(action.type);
            break;

    }
    return null;

}

function overlay(action) {
    /*var {plotIdAry}= action.payload;

    var cc= CsysConverter.make(primePlot(visRoot(),plotIdAry[0]));
    if (!cc) return null;
    */
    var {plotIdAry}= action.payload;
    var drawCompass= makeCompass(plotIdAry[0], action);
    return drawCompass;

}

//TODO: WHY and WHEN is it used????
function onDetach(drawLayer,action) {
    var {plotIdAry}= action.payload;
    plotIdAry.forEach( (plotId) => dispatchAttributeChange(plotId,false,PlotAttribute.SHOW_COMPASS,null));
}

function makeCompass(plotId, action){


    var plot= primePlot(visRoot(),plotId);
    var pv= getPlotViewById(visRoot(),plotId);
    var cc= CsysConverter.make(primePlot(visRoot(),plotId));
    if (!cc) return null;

    //var iWidth= cc.viewPort.dim.width;//plot.dataWidth;
    //var iHeight= cc.viewPort.dim.height;//plot.dataHeight;
    //var ix= (iWidth<100) ? iWidth*.5 : iWidth*.25;
    //var iy= (iHeight<100) ? iHeight*.5 : iWidth*.25;
    //var  vpt = makeViewPortPt(ix,iy);
    var  sPt = makeScreenPt(plot.viewPort.x+pv.scrollX+70, plot.viewPort.y+pv.scrollY+70);

    /*if(!cc.imageWorkSpacePtInPlot(vpt)){
        vpt = makeImagePt(ix,iy);

    }*/
    var wpStart= cc.getWorldCoords(sPt);
    var cdelt1 = cc.getImagePixelScaleInDeg();
    var zf= cc.zoomFactor || 1;
    var wpt2= makeWorldPt(wpStart.getLon(), wpStart.getLat() + (Math.abs(cdelt1)/zf)*(60/2));
    var wptE2=  makeWorldPt(wpStart.getLon()+(Math.abs(cdelt1)/zf)*(60/2), wpStart.getLat());

    var sptStart= cc.getScreenCoords(wpStart);
    var spt2= cc.getScreenCoords(wpt2);

    var sptE2= cc.getScreenCoords(wptE2);
    if (sptStart===null || spt2===null || sptE2===null) {
        return null;
    }

    var dataN= makeDirectionArrowDrawObj(sptStart, spt2,"N");
    var dataE= makeDirectionArrowDrawObj(sptStart, sptE2,"E");

    //return Arrays.asList(new DrawObj[]{dataN, dataE});
    //
    var arrows = VisUtil.getArrowCoords();

    var pt = makeWorldPt(10.68479, 41.26906);//cc.getWorldCoords(makeImagePt(0, 0));
    var obj = ShapeDataObj.makeCircleWithRadius(pt,50);
    var txt = ShapeDataObj.makeText(pt,"Text");
    obj.color = 'yellow';
    return [dataE, dataN];
}

function attach(drawLayer, action) {
    var sel = overlay(action);
    return {
        drawData:{
            data:sel
        }
    };
}





//////////////////////////////////////////////////
//////////////////////////////////////////////////
//////////////////////////////////////////////////











