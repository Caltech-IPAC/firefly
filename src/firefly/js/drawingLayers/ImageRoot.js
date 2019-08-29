/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty} from 'lodash';
import React from 'react';
import numeral from 'numeral';
import PointDataObj from '../visualize/draw/PointDataObj.js';
import {DrawSymbol} from '../visualize/draw/DrawSymbol.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getPlotViewById, primePlot} from '../visualize/PlotViewUtil';
import {dispatchChangeImageVisibility, visRoot} from '../visualize/ImagePlotCntlr';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr';
import {isHiPS, isImage} from '../visualize/WebPlot';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {getUIComponent} from './ImageRootUI.jsx';
import {isDefined} from '../util/WebUtil';

const ID= 'IMAGE_ROOT';
const TYPE_ID= 'IMAGE_ROOT_TYPE';

const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null, getUIComponent,onVisibilityChange);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;

const precision4Digit = '0.0000';

function onVisibilityChange(drawLayer,action) {
    dispatchChangeImageVisibility({plotId:drawLayer.plotId,visible:action.payload.visible});
}


function creator(initPayload, presetDefaults) {

    const drawingDef= {
        ...makeDrawingDef('yellow', {lineWidth:1, size:10, fontWeight:'bolder', symbol: DrawSymbol.POINT_MARKER } ),
        ...presetDefaults};
    idCnt++;

    const {plotId}= initPayload;


    const options= {
        plotId,
        searchTargetVisible: true,
        isPointData:false,
        autoFormatTitle:false,
        canUserChangeColor: ColorChangeType.DISABLE,
        canUserDelete: false,
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, {}, options, drawingDef);
}

function getLayerChanges(drawLayer, action) {
    const {plotId}= drawLayer;
    if (!plotId) return null;
    const pv= getPlotViewById(visRoot(),plotId);
    const plot= primePlot(pv);
    if (!pv || !plot) return null;
    switch (action.type) {
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            // if (!pv.plotViewCtx.displayFixedTarget) return;
            const hasPt= Boolean(pv.plotViewCtx.displayFixedTarget && plot.attributes[PlotAttribute.FIXED_TARGET]);
            return {
                title: getTitle(pv, plot, drawLayer),
                isPointData:hasPt,
                canUserChangeColor: hasPt? ColorChangeType.DYNAMIC : ColorChangeType.DISABLE};
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {changes}= action.payload;
            if (isDefined(changes.searchTargetVisible)) {
                const dd= Object.assign({},drawLayer.drawData);
                dd[DataTypes.DATA]= null;
                return {drawData:dd, searchTargetVisible:changes.searchTargetVisible};
            }
            break;
        default:
            return { title:getTitle(pv, plot, drawLayer)};
    }
    return null;
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    if (dataType!==DataTypes.DATA) return null;
    return isEmpty(lastDataRet) ? computeDrawData(drawLayer) : lastDataRet;
}

function getTitle(pv, plot, drawLayer) {
    if (!plot) return '';
    const {request:r}= pv;
    const showSize= Boolean(r.getSizeInDeg() && isImage(plot) );
    const titleEmLen= Math.min(plot.title.length+2,24);
    const minWidth= showSize || !drawLayer.isPointData ? (titleEmLen+6)+'em' : titleEmLen+'em';
    return (
        <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            width: 100,
            minWidth,
            overflow: 'hidden',
            textOverflow: 'ellipsis'
        } }
             title={plot.title}>
            <div>{plot.title}</div>
            <div style={{paddingLeft: 10, fontSize:'80%'}}>{`${isHiPS(plot) ? 'HiPS' : 'Image'}${showSize?',':''}`}</div>
            {showSize &&
                     <div  style={{paddingLeft: 5, fontSize:'80%'}}>
                         {`Search Size: ${numeral(r.getSizeInDeg()).format(precision4Digit)}${String.fromCharCode(176)}`}
                     </div>
            }
        </div>
    );
}

function computeDrawData(drawLayer) {
    const {plotId}= drawLayer;
    if (!plotId) return null;
    const pv= getPlotViewById(visRoot(),plotId);
    const plot= primePlot(visRoot(),plotId);
    if (!plot || !drawLayer.searchTargetVisible || !pv.plotViewCtx.displayFixedTarget) return [];
    const wp= plot.attributes[PlotAttribute.FIXED_TARGET];
    return wp ? [PointDataObj.make(wp)] : [];
}
