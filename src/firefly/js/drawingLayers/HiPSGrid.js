/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {primePlot, isDrawLayerVisible} from '../visualize/PlotViewUtil.js';
import {getBestHiPSlevel, getVisibleHiPSCells,
    getPointMaxSide, getMaxDisplayableHiPSLevel} from '../visualize/HiPSUtil.js';
import FootprintObj from '../visualize/draw/FootprintObj.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {makeImagePt} from '../visualize/Point.js';
import CysConverter from '../visualize/CsysConverter';
import {getUIComponent} from './HiPSGridlUI.jsx';

const ID= 'HIPS_GRID';
const TYPE_ID= 'HIPS_GRID_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null,getUIComponent);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

let idCnt=0;


function creator(initPayload, presetDefaults) {

    let drawingDef= makeDrawingDef('magenta', {lineWidth:1, size:6} );
    drawingDef= Object.assign(drawingDef,presetDefaults);


    idCnt++;

    const options= {
        hasPerPlotData:true,
        isPointData:false,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        gridType: 'auto',
        gridLockLevel: 3,
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
            return dealWithMods(drawLayer,action);
            break;
    }
    return null;
}


function getTitle() {
    return 'HiPS';
}


function dealWithMods(drawLayer,action) {
    const {changes}= action.payload;
    if (changes.gridType) {
       return {gridType:changes.gridType, gridLockLevel: changes.gridLockLevel || 3};
    }
    return null;
}

function computeDrawData(drawLayer, plotId) {
    if (!plotId) return null;
    const plot= primePlot(visRoot(),plotId);
    if (!plot) return [];

    const cc= CysConverter.make(plot);

    let norder;
    if (drawLayer.gridType==='lock') {
        norder= Math.min(Number(drawLayer.gridLockLevel), getMaxDisplayableHiPSLevel(plot));
    }
    else {
        const limitByMax= drawLayer.gridType==='match';
        norder= getBestHiPSlevel(plot, limitByMax);
        if (norder==='allsky') norder= 3;
    }

    const {fov, centerWp}= getPointMaxSide(plot, plot.viewDim);
    const cells= getVisibleHiPSCells(norder,centerWp,fov, plot.dataCoordSys);

    // const fpAry= cells.map( (c) => c.wpCorners);
    const fpAry= cells
        .map( (c) => {
            const scrCorners= c.wpCorners.map( (corner) => cc.getImageCoords(corner));
            if (scrCorners.some ((scrC) => !scrC)) return null;
            return scrCorners;
        })
        .filter( (c) => c);



    const idAry= cells
        .map( (c) => {
            const s1= cc.getImageCoords(c.wpCorners[0]);
            const s2= cc.getImageCoords(c.wpCorners[2]);
            if (!s1 || !s2) return null;
            return ShapeDataObj.makeTextWithOffset(makeImagePt(-10,-6), makeImagePt( (s1.x+s2.x)/2, (s1.y+s2.y)/2), `${norder}/${c.ipix}`);
        })
        .filter( (v) => v);
    return [FootprintObj.make(fpAry), ...idAry];
}


