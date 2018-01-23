/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty} from 'lodash';
import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {primePlot, isDrawLayerVisible} from '../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import {getBestHiPSlevel, getVisibleHiPSCells,
    getPointMaxSide, getMaxDisplayableHiPSLevel} from '../visualize/HiPSUtil.js';
import FootprintObj from '../visualize/draw/FootprintObj.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {makeImagePt} from '../visualize/Point.js';
import CysConverter from '../visualize/CsysConverter';

const ID= 'IMAGE_OUTLINE';
const TYPE_ID= 'IMAGE_OUTLINE_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

let idCnt=0;


function creator(initPayload, presetDefaults) {

    let drawingDef= makeDrawingDef('blue', {lineWidth:1} );
    drawingDef= Object.assign(drawingDef,presetDefaults);


    idCnt++;

    const options= {
        hasPerPlotData:true,
        isPointData:false,
        canUserChangeColor: ColorChangeType.DYNAMIC,
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, {}, options, drawingDef);
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    if (!isDrawLayerVisible(drawLayer, plotId)) return null;
    if (dataType!==DataTypes.DATA) return null;
    // return isEmpty(lastDataRet) ? computeDrawData(plotId) : lastDataRet;
    return  computeDrawData(drawLayer, plotId);
}

function getLayerChanges(drawLayer, action) {
    switch (action.type) {
        case ImagePlotCntlr.ANY_REPLOT:
            break;
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            const {plotId}= action.payload;
            let {plotIdAry}= action.payload;
            if (!plotIdAry && !plotId) return null;
            if (!plotIdAry) plotIdAry= [plotId];
            const title= Object.assign({},drawLayer.title);
            plotIdAry.forEach( (id) => title[id]= getTitle(id));
            return {title};
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            break;
    }
    return null;
}


function getTitle(plotId) {
    const plot= primePlot(visRoot(),plotId);
    const lastTitle= plot.attributes[PlotAttribute.LAST_IMAGE_TITLE];
    return `${lastTitle?lastTitle:'Image'} outline`;
}

function computeDrawData(drawLayer, plotId) {
    if (!plotId) return null;
    const plot= primePlot(visRoot(),plotId);
    if (!plot) return [];

    const wpAry= plot.attributes[PlotAttribute.LAST_IMAGE_BOUNDS];
    const lastTitle= plot.attributes[PlotAttribute.LAST_IMAGE_TITLE];

    if (isEmpty(wpAry)) {
        return [];
    }
    const cc= CysConverter.make(plot);
    const s1= cc.getDeviceCoords(wpAry[0]);
    const s2= cc.getDeviceCoords(wpAry[2]);
    if (!s1 || !s2) return [];
    const loc= cc.getWorldCoords(makeImagePt( (s1.x+s2.x)/2, (s1.y+s2.y)/2));
    const retObj= [FootprintObj.make([wpAry])];
    if (lastTitle && loc) {
        const textObj= ShapeDataObj.makeTextWithOffset(makeImagePt(-10,-6), loc, lastTitle);
        retObj.push(textObj);
    }
    return retObj;
}


