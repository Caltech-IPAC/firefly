/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {Stack, Typography} from '@mui/joy';
import {isEmpty} from 'lodash';
import React from 'react';
import PointDataObj from '../visualize/draw/PointDataObj.js';
import {DrawSymbol} from '../visualize/draw/DrawSymbol.js';
import {getNextColor, makeDrawingDef, releaseColor} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getActivePlotView, getPlotViewById, primePlot} from '../visualize/PlotViewUtil';
import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {formatWorldPt} from '../visualize/ui/WorldPtFormat.jsx';
import {flux} from 'firefly/core/ReduxFlux.js';
import Point from 'firefly/visualize/Point.js';
import CsysConverter from 'firefly/visualize/CsysConverter.js';
import DrawLayerCntlr from 'firefly/visualize/DrawLayerCntlr.js';
import {FixedPtControl} from './FixedPtControl.jsx';

const ID= 'SEARCH_TARGET';
const TYPE_ID= 'SEARCH_TARGET_TYPE';
const UPDATE_SEARCH_TARGET= 'SearchTarget.UpdateSearchTarget'; // a 'private' action just for grid, dispatch by grid

const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,layerRemoved);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;


function creator(initPayload, presetDefaults) {

    const {drawLayerId, displayGroupId, plotId, layersPanelLayoutId, titlePrefix, fullTitle, searchTargetPoint, color, canUserDelete=false}= initPayload;
    const drawingDef= {
        ...makeDrawingDef(color||getNextColor(), {lineWidth:1, size:10, fontWeight:'bolder', symbol: DrawSymbol.POINT_MARKER } ),
        ...presetDefaults};
    idCnt++;


    const options= {
        plotId,
        displayGroupId: displayGroupId ?? drawLayerId,
        layersPanelLayoutId,
        fullTitle,
        titlePrefix,
        searchTargetPoint,
        isPointData:true,
        autoFormatTitle:false,
        destroyWhenAllDetached: true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        canUserDelete,
        allocatedColor: !Boolean(color),
    };
    return DrawLayer.makeDrawLayer(drawLayerId || `${ID}-${idCnt}`,TYPE_ID, {}, options, drawingDef, [ImagePlotCntlr.RECENTER, UPDATE_SEARCH_TARGET]);
}

function layerRemoved(drawLayer,action) {
    if (drawLayer.allocatedColor) releaseColor(drawLayer.drawingDef.color);
}

function getLayerChanges(drawLayer, action) {
    const {plotId}= drawLayer;
    let pv;
    let plot;
    if (plotId) {
        pv= getPlotViewById(visRoot(),plotId);
        plot= primePlot(pv);
    }
    let drawData= drawLayer.drawData;
    let drawingDef= drawLayer.drawingDef;
    switch (action.type) {
        case ImagePlotCntlr.RECENTER :
            setTimeout(() => flux.process({ type: UPDATE_SEARCH_TARGET, payload: {plotId}}), 1);
            drawData= drawLayer.drawData;
            break;
        case ImagePlotCntlr.UPDATE_SEARCH_TARGET:
            drawData= undefined;
            break;
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {changes}= action.payload;
            drawData= undefined;
            if (changes.color) {
                drawingDef= {...drawingDef,color:changes.color};
            }
            break;
        case DrawLayerCntlr.CHANGE_DRAWING_DEF:
            drawingDef= action.payload.drawingDef;
            drawingDef.preferedColor= drawingDef.color;
            drawData= computeDrawData({...drawLayer,drawingDef});
            break;
    }
    return { title:getTitle(pv, plot, drawLayer), drawingDef, drawData};
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    if (dataType!==DataTypes.DATA) return null;
    return isEmpty(lastDataRet) ? computeDrawData(drawLayer) : lastDataRet;
}

function getTitle(pv, plot, dl) {
    const pt= dl.searchTargetPoint || plot?.attributes?.[PlotAttribute.FIXED_TARGET];
    if (!pt) return null;
    if (!pv) pv= getActivePlotView(visRoot());

    let ptDiv;
    let minWidth;
    let preStr;
    if (pt.type===Point.W_PT) {
        preStr=  dl.fullTitle || `${dl.titlePrefix}Search: `;
        const emGuess= preStr.length+2 + pt.objName?pt.objName.length: 18;
        const titleEmLen= Math.min(emGuess ,24);
        minWidth= (titleEmLen+8)+'em';
        ptDiv= (<Typography level={pt.objName ? 'body-sm' : 'body-xs'}> {formatWorldPt(pt,3,false)} </Typography>);
    }
    else {
        preStr=  dl.fullTitle || dl.titlePrefix+' ' || 'Image Point: ';
        const titleEmLen= Math.min(preStr.length+2+16,24);
        minWidth= (titleEmLen+8)+'em';
        const cc= CsysConverter.make(plot);
        const convertedWp= cc.getWorldCoords(pt);
        ptDiv= (
            <Typography level='body-sm'>
                {`(${Math.round(pt.x)}, ${Math.round(pt.y)})`}
                {convertedWp&&
                    <div style={{paddingTop:3}}>
                        {formatWorldPt(convertedWp,3,false)}
                    </div>
                }
            </Typography>);
    }
    return (
        <Stack {...{direction:'row', justifyContent: 'space-between', alignItems:'center', width: 100, minWidth}}>
            <Stack {...{direction: 'row', alignItems: 'center'}}>
                <Typography {...{pr: .5}} >{preStr}</Typography>
                {ptDiv}
                <FixedPtControl pv={pv} wp={pt} />
            </Stack>
        </Stack>
    );
}

function computeDrawData(drawLayer) {
    const {plotId}= drawLayer;
    const plot= primePlot(visRoot(),plotId);
    let wp= drawLayer.searchTargetPoint;
    if (!wp && plot) {
        if (!plot) return [];
        wp= drawLayer.searchTargetPoint || plot.attributes[PlotAttribute.FIXED_TARGET];
    }
    return wp ? [PointDataObj.make(wp)] : [];
}
