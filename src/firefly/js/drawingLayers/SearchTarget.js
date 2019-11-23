/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get, isEmpty} from 'lodash';
import React from 'react';
import PointDataObj from '../visualize/draw/PointDataObj.js';
import {DrawSymbol} from '../visualize/draw/DrawSymbol.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getActivePlotView, getPlotViewById, primePlot} from '../visualize/PlotViewUtil';
import {visRoot} from '../visualize/ImagePlotCntlr';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {formatWorldPt} from '../visualize/ui/WorldPtFormat.jsx';
import {FixedPtControl} from './CatalogUI.jsx';

const ID= 'SEARCH_TARGET';
const TYPE_ID= 'SEARCH_TARGET_TYPE';

const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;


function creator(initPayload, presetDefaults) {

    const {drawLayerId, plotId, layersPanelLayoutId, titlePrefix, searchTargetWP, color, canUserDelete=false}= initPayload;
    const drawingDef= {
        ...makeDrawingDef(color||'yellow', {lineWidth:1, size:10, fontWeight:'bolder', symbol: DrawSymbol.POINT_MARKER } ),
        ...presetDefaults};
    idCnt++;


    const options= {
        plotId,
        layersPanelLayoutId,
        titlePrefix,
        searchTargetWP,
        isPointData:true,
        autoFormatTitle:false,
        destroyWhenAllDetached: true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        canUserDelete
    };
    return DrawLayer.makeDrawLayer(drawLayerId || `${ID}-${idCnt}`,TYPE_ID, {}, options, drawingDef);
}

function getLayerChanges(drawLayer, action) {
    const {plotId}= drawLayer;
    let pv;
    let plot;
    if (plotId) {
        pv= getPlotViewById(visRoot(),plotId);
        plot= primePlot(pv);
    }
    return { title:getTitle(pv, plot, drawLayer)};
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    if (dataType!==DataTypes.DATA) return null;
    return isEmpty(lastDataRet) ? computeDrawData(drawLayer) : lastDataRet;
}

function getTitle(pv, plot, dl) {
    const wp= dl.searchTargetWP || get(plot,['attributes',PlotAttribute.FIXED_TARGET]);
    if (!wp) return null;
    if (!pv) pv= getActivePlotView(visRoot());
    const preStr=  `${dl.titlePrefix}Search: `;
    const emGuess= preStr.length+2 + wp.objName?wp.objName.length: 18;
    const titleEmLen= Math.min(emGuess ,24);
    const minWidth= (titleEmLen+8)+'em';
    return (
        <div style={{display: 'flex', justifyContent: 'space-between', alignItems:'center', width: 100, minWidth}}>
            <div style={{display: 'flex', alignItems: 'center'}}>
                <span style={{paddingRight: 5}} >{preStr}</span>
                <div> {formatWorldPt(wp,3,false)} </div>
                <FixedPtControl pv={pv} wp={wp} />
            </div>
        </div>
    );
}

function computeDrawData(drawLayer) {
    const {plotId}= drawLayer;
    const plot= primePlot(visRoot(),plotId);
    let wp= drawLayer.searchTargetWP;
    if (!wp && plot) {
        if (!plot) return [];
        wp= drawLayer.searchTargetWP || plot.attributes[PlotAttribute.FIXED_TARGET];
    }
    return wp ? [PointDataObj.make(wp)] : [];
}
