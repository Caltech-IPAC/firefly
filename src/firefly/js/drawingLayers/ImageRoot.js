/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {sprintf} from '../externalSource/sprintf';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getPlotViewById, primePlot} from '../visualize/PlotViewUtil';
import {dispatchChangeImageVisibility, visRoot} from '../visualize/ImagePlotCntlr';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr';
import {isHiPS, isImage} from '../visualize/WebPlot';

const ID= 'IMAGE_ROOT';
const TYPE_ID= 'IMAGE_ROOT_TYPE';

const factoryDef= makeFactoryDef(TYPE_ID,creator,undefined,getLayerChanges,undefined, undefined,onVisibilityChange);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;

const precision4Digit = '0.0000';

function onVisibilityChange(drawLayer,action) {
    dispatchChangeImageVisibility({plotId:drawLayer.plotId,visible:action.payload.visible});
}


function creator(initPayload, presetDefaults) {

    const drawingDef= {
        ...makeDrawingDef('yellow', {fontWeight:'bolder'} ),
        ...presetDefaults};
    idCnt++;

    const {plotId, layersPanelLayoutId}= initPayload;


    const options= {
        plotId,
        layersPanelLayoutId,
        searchTargetVisible: true,
        isPointData:false,
        autoFormatTitle:false,
        canUserChangeColor: ColorChangeType.DISABLE,
        destroyWhenAllDetached: true,
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
            return { title: getTitle(pv, plot, drawLayer), };
        default:
            return { title:getTitle(pv, plot, drawLayer)};
    }
    return undefined;
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
                         {`Search Size: ${sprintf('%.4f',r.getSizeInDeg())}${String.fromCharCode(176)}`}
                     </div>
            }
        </div>
    );
}

