/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty} from 'lodash';
import React from 'react';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import PointDataObj, {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {formatWorldPt, formatWorldPtToString} from '../visualize/ui/WorldPtFormat';
import {getUIComponent} from './FixedMarkerUI.jsx';

const ID= 'FIXED_MARKER';
const TYPE_ID= 'FIXED_MARKER_TYPE';



const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null, getUIComponent,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;


function creator(initPayload, presetDefaults) {

    let drawingDef= makeDrawingDef('yellow', {lineWidth:1, size:6, fontWeight:'bolder', symbol: DrawSymbol.CROSS } );
    drawingDef= Object.assign(drawingDef,presetDefaults);
    idCnt++;

    const options= {
        isPointData:true,
        autoFormatTitle:false,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        worldPt: initPayload.worldPt
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, {}, options, drawingDef);
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    if (dataType!==DataTypes.DATA) return null;
    return isEmpty(lastDataRet) ? computeDrawData(drawLayer) : lastDataRet;
}

function getLayerChanges(drawLayer, action) {
    switch (action.type) {
        case ImagePlotCntlr.ANY_REPLOT:
            break;
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            let {plotIdAry,plotId}= action.payload;
            if (!plotIdAry && !plotId) return null;
            if (!plotIdAry) plotIdAry= [plotId];
            const title= getTitle(drawLayer);
            return {title};
    }
    return null;
}


function getTitle(drawLayer) {
    const {worldPt:wp}= drawLayer;
    // return formatWorldPtToString(wp,true);
    return (
        <div style={{display:'inline-flex', width: 100}} title={formatWorldPtToString(wp)}>
            {formatWorldPt(wp,true,true)}
        </div>
    );
    // return wp.objName ? wp.objName :formatPosForTextField(wp);
}

function computeDrawData(drawLayer) {
    const {worldPt:wp}= drawLayer;
    return wp ? [PointDataObj.make(wp)] : [];
}


