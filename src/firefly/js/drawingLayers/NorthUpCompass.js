/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {get} from 'lodash';
import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {makeWorldPt, makeScreenPt} from '../visualize/Point.js';
import {primePlot, getPlotViewById} from '../visualize/PlotViewUtil.js';
import {getTopmostVisiblePoint} from '../visualize/VisUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {makeDirectionArrowDrawObj} from '../visualize/draw/DirectionArrowDrawObj.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import DrawLayerCntlr, { dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerCntlr.js';
import Color from '../util/Color.js';

const ID= 'NORTH_UP_COMPASS_TOOL';
const TYPE_ID= 'NORTH_UP_COMPASS_TYPE';


const selHelpText='North Arrow - EQ. J2000';
const factoryDef= makeFactoryDef(TYPE_ID,creator, getDrawData, getLayerChanges, null,/*getUIComponent*/null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var idCnt=0;

function* relocateCompassSaga({id}, dispatch, getState) {
    while (true) {
        var action = yield take([ImagePlotCntlr.RECENTER, ImagePlotCntlr.PROCESS_SCROLL,
                                 ImagePlotCntlr.ROTATE_CLIENT, ImagePlotCntlr.FLIP_CLIENT]);
        switch (action.type) {
            case ImagePlotCntlr.RECENTER:
            case ImagePlotCntlr.PROCESS_SCROLL:
                dispatchForceDrawLayerUpdate(id, get(primePlot(visRoot()), 'plotId'));
                break;
        }
    }
}

function creator() {

    var drawingDef= makeDrawingDef('red');
    var actionTypes= [ImagePlotCntlr.UPDATE_VIEW_SIZE,
                      ImagePlotCntlr.ZOOM_IMAGE_START,
                      ImagePlotCntlr.ZOOM_IMAGE,
                      DrawLayerCntlr.FORCE_DRAW_LAYER_UPDATE];

    idCnt++;
    var options= {
        hasPerPlotData:true,
        isPointData:false,
        canUserChangeColor: ColorChangeType.DYNAMIC
    };

    var id = `${ID}-${idCnt}`;

    dispatchAddSaga(relocateCompassSaga, {id});
    return DrawLayer.makeDrawLayer( id, TYPE_ID, selHelpText,
                                    options, drawingDef, actionTypes);

}

function getLayerChanges(drawLayer, action) {
    var {drawingDef} = drawLayer;
    var rgba = Color.getRGBA(drawingDef.color);

    switch(action.type) {
        case ImagePlotCntlr.ZOOM_IMAGE_START:
            if (drawingDef && rgba && !drawingDef.preAlpha) {
                drawingDef.preAlpha = rgba[3];
                rgba[3] = 0.0;  // fully transparent
                drawingDef.color = Color.toRGBAString(rgba);
            }
            break;

        case ImagePlotCntlr.ZOOM_IMAGE:
            if (drawingDef && drawingDef.preAlpha && rgba && drawingDef.preAlpha !== rgba[3]) {
                rgba[3] = drawingDef.preAlpha;
                drawingDef.color = Color.toRGBAString(rgba);
                drawingDef.preAlpha = undefined;
            }
            break;
    }
    return null;
}

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet){
    if (action.type !== ImagePlotCntlr.ZOOM_IMAGE) {
        var drawCompass = makeCompass(plotId, action, drawLayer);

        return drawCompass || lastDataRet;
    } else {
        return null;
    }
}

function makeCompass(plotId, action){
    var plot= primePlot(visRoot(),plotId);
    if (!plot.projection.isImplemented()) return null;

    var cc= CsysConverter.make(plot);
    if (!cc) return null;

    var pv= getPlotViewById(visRoot(),plotId);

    // const dist = 60;
    const px = 30;
    // var textSpace = {x: 8, y: 15};
    // const yOff = Math.min(Math.min(plot.screenSize.height, pv.viewDim.height)/4, dist);
    // const xOff = Math.min(Math.min(plot.screenSize.width, pv.viewDim.width)/4, dist);
    // const offStart = Math.min(xOff, yOff);

    // const {screenSize} = plot;
    // const {viewDim} = pv;

    // var compassAt = (scroll, widthHeight, xY) => {
    //     var compassAt;
    //
    //
    //     if (scroll < 0) { // viewport origin is inside viewDim
    //         var border = scroll + viewDim[widthHeight];
    //
    //         compassAt = offStart;
    //         if (compassAt > border) {
    //             compassAt = Math.max(border,  px + textSpace[xY]);
    //         }
    //     } else {
    //         compassAt = Math.min(scroll + offStart, (screenSize[widthHeight] - textSpace.x));
    //     }
    //     return compassAt;
    // };
    //
    // const sy = compassAt(pv.scrollY, 'height', 'y');
    // const sx = compassAt(pv.scrollX, 'width', 'x');
    //
    // var sptStart = makeScreenPt(sx, sy);


    const sptStart= cc.getScreenCoords(getTopmostVisiblePoint(pv, 55, 55));
    if (!sptStart) return null;


    var wpStart= cc.getWorldCoords(sptStart);
    var cdelt1 = cc.getImagePixelScaleInDeg();
    var zf= cc.zoomFactor || 1;
    var wpt2= makeWorldPt(wpStart.getLon(), wpStart.getLat() + (Math.abs(cdelt1)/zf)*(px));
    var wptE2=  makeWorldPt(wpStart.getLon()+(Math.abs(cdelt1)/zf)*(px), wpStart.getLat());
    var spt2= cc.getScreenCoords(wpt2);
    var sptE2= cc.getScreenCoords(wptE2);

    if (sptStart===null || spt2===null || sptE2===null) {
        return null;
    }

    var dataN= makeDirectionArrowDrawObj(sptStart, spt2,'N');
    var dataE= makeDirectionArrowDrawObj(sptStart, sptE2,'E');

    return [dataE, dataN];
}