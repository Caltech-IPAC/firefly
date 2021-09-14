/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get, isEmpty} from 'lodash';
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
import {FixedPtControl} from './CatalogUI.jsx';
import {flux} from 'firefly/core/ReduxFlux.js';
import Point from 'firefly/visualize/Point.js';
import {sprintf} from 'firefly/externalSource/sprintf.js';

const ID= 'SEARCH_TARGET';
const TYPE_ID= 'SEARCH_TARGET_TYPE';
const UPDATE_SEARCH_TARGET= 'SearchTarget.UpdateSearchTarget'; // a 'private' action just for grid, dispatch by grid

const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,layerRemoved);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;


function creator(initPayload, presetDefaults) {

    const {drawLayerId, displayGroupId, plotId, layersPanelLayoutId, titlePrefix, searchTargetPoint, color, canUserDelete=false}= initPayload;
    const drawingDef= {
        ...makeDrawingDef(color||getNextColor(), {lineWidth:1, size:10, fontWeight:'bolder', symbol: DrawSymbol.POINT_MARKER } ),
        ...presetDefaults};
    idCnt++;


    const options= {
        plotId,
        displayGroupId,
        layersPanelLayoutId,
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
    const drawData=  (action.type===UPDATE_SEARCH_TARGET) ? undefined : drawLayer.drawData;
    if (action.type===ImagePlotCntlr.RECENTER) {
        setTimeout(() => { flux.process({ type: UPDATE_SEARCH_TARGET, payload: { plotId}}); },1);
    }
    return { title:getTitle(pv, plot, drawLayer), drawData};
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    if (dataType!==DataTypes.DATA) return null;
    return isEmpty(lastDataRet) ? computeDrawData(drawLayer) : lastDataRet;
}

function getTitle(pv, plot, dl) {
    const pt= dl.searchTargetPoint || get(plot,['attributes',PlotAttribute.FIXED_TARGET]);
    if (!pt) return null;
    if (!pv) pv= getActivePlotView(visRoot());

    let ptDiv;
    let minWidth;
    let preStr;
    if (pt.type===Point.W_PT) {
        preStr=  `${dl.titlePrefix}Search: `;
        const emGuess= preStr.length+2 + pt.objName?pt.objName.length: 18;
        const titleEmLen= Math.min(emGuess ,24);
        minWidth= (titleEmLen+8)+'em';
        ptDiv= (<div> {formatWorldPt(pt,3,false)} </div>);
    }
    else {
        preStr=  dl.titlePrefix+' ' || 'Image Point: ';
        const titleEmLen= Math.min(preStr.length+2+16,24);
        minWidth= (titleEmLen+8)+'em';
        ptDiv= (<div> {`(${Math.round(pt.x)}, ${Math.round(pt.y)})`} </div>);
    }
    return (
        <div style={{display: 'flex', justifyContent: 'space-between', alignItems:'center', width: 100, minWidth}}>
            <div style={{display: 'flex', alignItems: 'center'}}>
                <span style={{paddingRight: 5}} >{preStr}</span>
                {ptDiv}
                <FixedPtControl pv={pv} wp={pt} />
            </div>
        </div>
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
