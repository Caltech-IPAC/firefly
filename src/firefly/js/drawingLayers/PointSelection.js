/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {visRoot,dispatchAttributeChange} from '../visualize/ImagePlotCntlr.js';
import {primePlot, isActivePlotView} from '../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import PointDataObj from '../visualize/draw/PointDataObj.js';
import {DrawSymbol} from '../visualize/draw/DrawSymbol.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {flux} from '../core/ReduxFlux.js';
import {clone} from '../util/WebUtil.js';
import {get, isEmpty} from 'lodash';

const ID= 'POINT_SELECTION';
const TYPE_ID= 'POINT_SELECTION_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,onDetach,null);


export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;

function dispatchSelectPoint(mouseStatePayload) {
    const {plotId,screenPt,drawLayer}= mouseStatePayload;
    if (mouseStatePayload.shiftDown) return;
    if (drawLayer.drawData.data) {
        flux.process({type:DrawLayerCntlr.SELECT_POINT, payload:mouseStatePayload} );
        dispatchAttributeChange(
            {plotId,
            attKey:PlotAttribute.ACTIVE_POINT,attValue:{pt:makeSelectedPt(screenPt,plotId)}
        });
    }
}


function onDetach(drawLayer,action) {
    const {plotIdAry}= action.payload;
    plotIdAry.forEach( (plotId) => {
        const plot= primePlot(visRoot(),plotId);
        if (plot && plot.attributes[PlotAttribute.ACTIVE_POINT]) {
            dispatchAttributeChange(
                { plotId ,overlayColorScope:false,attKey:PlotAttribute.ACTIVE_POINT,attValue:null }
            );
        }
    });
}



function creator(initPayload, presetDefaults) {
    var drawingDef= makeDrawingDef('pink');
    drawingDef.symbol= DrawSymbol.SQUARE;
    drawingDef.size= 6;
    drawingDef= Object.assign(drawingDef,presetDefaults);
    idCnt++;

    const pairs= {
        [MouseState.UP.key]: dispatchSelectPoint
    };
    const actionTypes= [DrawLayerCntlr.SELECT_POINT];
    const options = {
        isPointData: true,
        hasPerPlotData: true,
        canUserDelete: false,
        canUserChangeColor: ColorChangeType.DYNAMIC
    };

    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, 'Selected Point', options, drawingDef, actionTypes, pairs);
}



function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {

    if (dataType!==DataTypes.DATA) return null;
    var active= isActivePlotView(visRoot(), plotId);
    var drawAry= selectAPoint(drawLayer,action, active, plotId);
    return drawAry || lastDataRet;
}



function getLayerChanges(drawLayer, action) {
    if  (action.type===DrawLayerCntlr.CHANGE_DRAWING_DEF) {
        return {drawingDef: clone(drawLayer.drawingDef,action.payload.drawingDef)};
    }
}

function makeSelectedPt(screenPt,plotId) {
    var plot= primePlot(visRoot(),plotId);
    var cc= CsysConverter.make(plot);
    var selPt= cc.getWorldCoords(screenPt); //todo put back

    if (!selPt) selPt= cc.getImageCoords(screenPt);
    return selPt;
}


function selectAPoint(drawLayer, action, active, pId) {
    var {screenPt, plotId, plotIdAry}= action.payload;

    const isEmptyData = () => {
        const data = get(drawLayer, ['drawData', 'data']);

        if (isEmpty(data)) return true;
        const idx = Object.keys(data).findIndex((onePid) => !isEmpty(data[onePid]));

        return idx < 0;
    };
    // attach drawing layer to the plot which is created after the drawing layer
    if (!screenPt &&
        action.type === DrawLayerCntlr.ATTACH_LAYER_TO_PLOT &&
        !isEmptyData() && plotIdAry && plotIdAry.includes(pId))  {
        if (drawLayer.plotIdAry) {
            let dAry;

            const prevPID = drawLayer.plotIdAry.find((id) => {
                dAry = get(drawLayer, ['drawData', 'data', id], null);
                return dAry&&dAry.length;
            });

            return !prevPID ? null : dAry;
        }
    }

    if (!screenPt) {
        return null;
    }


    var selPt= makeSelectedPt(screenPt,plotId);
    if (!selPt) return null;
    var drawAry;
    if (active) {
        drawAry= [
            PointDataObj.make(selPt)
        ];
    }
    else {
        drawAry= [PointDataObj.make(selPt)];
    }
    return  drawAry;
}
