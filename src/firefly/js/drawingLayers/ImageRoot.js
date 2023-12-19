/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack, Typography} from '@mui/joy';
import React from 'react';
import {sprintf} from '../externalSource/sprintf';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getPlotViewById, primePlot} from '../visualize/PlotViewUtil';
import {dispatchChangeHiPS, dispatchChangeImageVisibility, visRoot} from '../visualize/ImagePlotCntlr';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr';
import {isHiPS, isImage} from '../visualize/WebPlot';

const ID= 'IMAGE_ROOT';
const TYPE_ID= 'IMAGE_ROOT_TYPE';

const factoryDef= makeFactoryDef(TYPE_ID,creator,undefined,getLayerChanges,undefined, undefined,onVisibilityChange);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;

function onVisibilityChange(drawLayer,action) {
    dispatchChangeImageVisibility({plotId:drawLayer.plotId,visible:action.payload.visible});
}


function creator(initPayload, presetDefaults) {

    const {plotId, layersPanelLayoutId}= initPayload;
    const plot= primePlot(visRoot(),plotId);
    const drawingDef= {
        ...makeDrawingDef(plot?.blankColor??'yellow', {fontWeight:'bolder'} ),
        ...presetDefaults};
    idCnt++;



    const options= {
        plotId,
        layersPanelLayoutId,
        searchTargetVisible: true,
        isPointData:false,
        autoFormatTitle:false,
        canUserChangeColor: ColorChangeType.DISABLE, //todo change infrastruct to support a onColorChange
        destroyWhenAllDetached: true,
        canUserDelete: false,
    };
    return DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, {}, options, drawingDef);
}

function getLayerChanges(drawLayer, action) {
    const {payload,type}= action;
    const {plotId}= drawLayer;
    if (!plotId) return null;
    const pv= getPlotViewById(visRoot(),plotId);
    const plot= primePlot(pv);
    if (!pv || !plot) return null;
    const canUserHide= !plot.blank;
    const canUserChangeColor= plot.blank ? ColorChangeType.DYNAMIC : ColorChangeType.DISABLE;
    if (isHiPS(plot) && payload.drawingDef?.color && plot.blankColor !== payload.drawingDef.color) {
        setTimeout(() => dispatchChangeHiPS({plotId,blankColor:payload.drawingDef.color}), 5);
    }
    switch (type) {
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            return { title: getTitle(pv, plot, drawLayer), canUserHide, canUserChangeColor};
        default:
            return { title: getTitle(pv, plot, drawLayer), canUserHide, canUserChangeColor};
    }
    return undefined;
}



function getTitle(pv, plot, drawLayer) {
    if (!plot) return '';
    const {request:r}= pv;
    const showSize= Boolean(r.getSizeInDeg() && isImage(plot) );
    const titleEmLen= Math.min(plot.title.length+2,24);
    const minWidth= showSize || !drawLayer.isPointData ? (titleEmLen+6)+'em' : titleEmLen+'em';
    const maxWidth=450;
    const {blank=false}= plot;
    const hipsStr= blank ? '': 'Image (HiPS)';
    return () => {
       return  (
            <Stack {...{
                direction: 'row',
                alignItems: 'center',
                spacing:1,
                maxWidth,
                minWidth,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                title:plot.title,
            } } >
                 <Typography>{`${isHiPS(plot) ? hipsStr : 'Image (FITS)'}${showSize?',':''}`}</Typography>
                 {showSize &&
                 <Typography {...{level:'body-xs'}}>
                   {`Search Size: ${sprintf('%.4f',r.getSizeInDeg())}${String.fromCharCode(176)}`}
                 </Typography>
                 }
            </Stack>
        );
    };
}

