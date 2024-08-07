/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {primePlot, isDrawLayerVisible} from '../visualize/PlotViewUtil.js';
import {
    getHiPSNorderlevel, getVisibleHiPSCells,
    getPointMaxSide, getMaxDisplayableHiPSGridLevel, tileCoordsWrap
} from '../visualize/HiPSUtil.js';
import FootprintObj from '../visualize/draw/FootprintObj.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {makeImagePt} from '../visualize/Point.js';
import CysConverter from '../visualize/CsysConverter';
import {getUIComponent} from './HiPSGridlUI.jsx';
import {getAllPlotViewIdByOverlayLock} from '../visualize/PlotViewUtil';
import {isDefined} from '../util/WebUtil.js';
import {changeHiPSProjectionCenterAndType, isHiPSAitoff} from 'firefly/visualize/WebPlot.js';

const ID= 'HIPS_GRID';
const TYPE_ID= 'HIPS_GRID_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator,null,getLayerChanges,null,getUIComponent);

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
        showLabels: true
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, {}, options, drawingDef);
}

function getLayerChanges(drawLayer, action) {
    switch (action.type) {
        case ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION:
        case ImagePlotCntlr.ANY_REPLOT:
            return {drawData:computeDrawData(drawLayer,action, drawLayer.gridType,drawLayer.gridLockLevel)};
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            const {plotId} = action.payload;
            let {plotIdAry}= action.payload;

            if (!plotIdAry && !plotId) return null;
            plotIdAry = plotIdAry ? plotIdAry : [plotId];

            const title= Object.assign({},drawLayer.title);
            plotIdAry.forEach( (id) => title[id]= getTitle());

            return {title,
                drawData:computeDrawData(drawLayer,action,drawLayer.gridType,drawLayer.gridLockLevel) };
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            return dealWithMods(drawLayer,action);
        case DrawLayerCntlr.CHANGE_VISIBILITY:
            if (action.payload.visible) {
                return {drawData:computeDrawData(drawLayer,action, drawLayer.gridType,drawLayer.gridLockLevel, true)};
            }
    }
    return null;
}


function getTitle() {
    return 'HEALPix (HiPS) Grid';
}


function dealWithMods(drawLayer,action) {
    const {changes}= action.payload;
    if (isDefined(changes.showLabels)) {
        return {
            ...drawLayer,
            showLabels: changes.showLabels,
            drawData:
                computeDrawData({...drawLayer, showLabels: changes.showLabels},action,drawLayer.gridType,drawLayer.gridLockLevel)
        };
    }
    else if (changes.gridType) {
        const gridLockLevel= changes.gridLockLevel || drawLayer.gridLockLevel;
       return {gridType:changes.gridType, gridLockLevel,
              drawData:computeDrawData(drawLayer,action,changes.gridType,gridLockLevel) };
    }
    return null;
}

function computeDrawData(drawLayer,action, gridType, gridLockLevel, isVisible = false) {
    const {payload}= action;
    const plotIdAry= payload.plotId ? getAllPlotViewIdByOverlayLock(visRoot(), payload.plotId, false, true) : payload.plotIdAry;
    if (plotIdAry) {
        const drawData= {data: {...drawLayer.drawData.data}};
        const projectionTypeChange= isDefined(payload.fullSky);

        plotIdAry.forEach( (plotId) => {
            if (plotId && (isDrawLayerVisible(drawLayer, plotId) || isVisible)) {
                drawData.data[plotId] = computeDrawDataForId(plotId, gridType, gridLockLevel, projectionTypeChange, drawLayer.showLabels);
            } else {
                drawData.data[plotId] = null;
            }
        });
        return drawData;
    }
    else {
        return drawLayer.drawData;
    }
}

function computeDrawDataForId(plotId, gridType, gridLockLevel, projectionTypeChange, showLabels=true) {
    let plot= primePlot(visRoot(),plotId);
    if (!plot) return [];
    let aitoff= isHiPSAitoff(plot);
    if (projectionTypeChange) {
        aitoff= !aitoff;
        plot= changeHiPSProjectionCenterAndType(plot,undefined,aitoff);
    }

    const cc= CysConverter.make(plot);

    let norder, desiredNorder;
    if (gridType==='lock') {
        norder= Math.min(Number(gridLockLevel), getMaxDisplayableHiPSGridLevel(plot));
        desiredNorder= norder;
    }
    else {
        const limitByMax= gridType==='match';
        const result= getHiPSNorderlevel(plot, limitByMax);
        norder= result.norder;
        desiredNorder= result.desiredNorder;
    }

    const {fov, centerWp}= getPointMaxSide(plot, plot.viewDim);
    const cells= getVisibleHiPSCells(norder,desiredNorder, centerWp,fov, plot.viewDim, plot.dataCoordSys, aitoff);

    const nonWrapCells= fov>=130 && aitoff ? cells.filter( (c) => !tileCoordsWrap(cc, c.wpCorners)) : cells;

    // const fpAry= cells.map( (c) => c.wpCorners);
    const fpAry= nonWrapCells
        .map( (c) => {
            const scrCorners= c.wpCorners.map( (corner) => cc.getImageCoords(corner));
            if (scrCorners.some ((scrC) => !scrC)) return null;
            return scrCorners;
        })
        .filter( (c) => c);




    const idAry= showLabels? nonWrapCells
        .map( (c) => {
            const s1= cc.getImageCoords(c.wpCorners[0]);
            const s2= cc.getImageCoords(c.wpCorners[2]);
            if (!s1 || !s2) return null;
            return ShapeDataObj.makeTextWithOffset(makeImagePt(-10,-6),
                 makeImagePt( (s1.x+s2.x)/2, (s1.y+s2.y)/2), `${norder}/${c.ipix}`);
        })
        .filter( (v) => v) : [];
    return [FootprintObj.make(fpAry), ...idAry];
}


