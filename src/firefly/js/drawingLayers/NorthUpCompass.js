/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {dispatchForceDrawLayerUpdate} from '../visualize/DrawLayerDispatch';
import {
    FLIP, FORCE_DRAW_LAYER_UPDATE, PROCESS_SCROLL, RECENTER, ROTATE, UPDATE_VIEW_SIZE, ZOOM_HIPS, ZOOM_IMAGE
} from '../visualize/VisConst';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {dlRoot} from '../visualize/VisStoreRoots';
import {getTopmostVisiblePoint} from '../visualize/WebPlotAnalysis';
import {makeWorldPt, makeScreenPt, makeDevicePt} from '../visualize/Point.js';
import {currentP} from '../visualize/PlotViewUtil.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {makeDirectionArrowDrawObj} from '../visualize/draw/DirectionArrowDrawObj.js';
import {getPixScaleDeg} from '../visualize/WebPlot.js';
import {CoordinateSys} from '../visualize/CoordSys.js';
import {getDrawLayerById, hasWCSProjection} from '../visualize/PlotViewUtil';
import {dispatchAddActionWatcher} from '../core/MasterSaga';

const ID= 'NORTH_UP_COMPASS_TOOL';
const TYPE_ID= 'NORTH_UP_COMPASS_TYPE';
const selHelpText='North Arrow - EQ. J2000';

const factoryDef= makeFactoryDef(TYPE_ID,creator, getDrawData, null, null,/*getUIComponent*/null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

let idCnt=0;

function relocateCompass(action, cancelSelf, {drawLayerId}) {
    const layer= getDrawLayerById(dlRoot(), drawLayerId);
    layer ? dispatchForceDrawLayerUpdate(drawLayerId, currentP().plotId) : cancelSelf();
}

function creator() {
    const drawingDef= makeDrawingDef('red');
    const actionTypes= [UPDATE_VIEW_SIZE, FORCE_DRAW_LAYER_UPDATE];

    idCnt++;
    const options= {
        hasPerPlotData:true,
        isPointData:false,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        destroyWhenAllDetached: false
    };

    const drawLayerId = `${ID}-${idCnt}`;
    dispatchAddActionWatcher({
        callback:relocateCompass,
        params: {drawLayerId},
        actions:[RECENTER, PROCESS_SCROLL, ZOOM_IMAGE, ZOOM_HIPS, ROTATE, FLIP]
    });
    return DrawLayer.makeDrawLayer(drawLayerId, TYPE_ID, selHelpText, options, drawingDef, actionTypes);
}

function getDrawData(dataType, plotId, drawLayer, action) {
   return makeCompass(plotId, action, drawLayer);
}

function makeCompass(plotId){
    const {pv,plot}= currentP(plotId);
    if (!plot || !hasWCSProjection(plot)) return;
    const cc= CsysConverter.make(plot);
    if (!cc) return;

    // const dist = 60;
    const px = 30;

    let sptStart= cc.getScreenCoords(getTopmostVisiblePoint(plot,pv.viewDim, 55, 120));
    if (!sptStart) return;


    let wpStart= cc.getWorldCoords(sptStart, CoordinateSys.EQ_J2000);

    if (!wpStart) {
        wpStart= cc.getWorldCoords( makeDevicePt(cc.viewDim.width/2, cc.viewDim.height/2), CoordinateSys.EQ_J2000);
        if (!wpStart) return;
        sptStart= cc.getScreenCoords(wpStart);
    }

    const cdelt1 = getPixScaleDeg(plot);
    const zf= cc.zoomFactor || 1;
    const wpt2= makeWorldPt(wpStart.getLon(), wpStart.getLat() + (Math.abs(cdelt1)/zf)*(px), CoordinateSys.EQ_J2000);
    const spt2= cc.getScreenCoords(wpt2);
    const wpt3= makeWorldPt(wpStart.getLon() + (Math.abs(cdelt1)/(Math.cos(wpStart.getLat()*Math.PI/180.)*zf))*(px),
                           wpStart.getLat(), CoordinateSys.EQ_J2000);
    const spt3= cc.getScreenCoords(wpt3);
    // don't use spt3 because of funny effects near the celestial poles
    // the sign of the cross product of compass vectors tells us if the image is mirror-reversed from the sky
    const cross_product= (spt3.x - sptStart.x)*(spt2.y - sptStart.y) - (spt3.y - sptStart.y)*(spt2.x - sptStart.x);
    const sptE2= getEastFromNorthOnScreen(cc, sptStart, spt2, Math.sign(cross_product));

    if (!sptStart || !spt2 || !sptE2) return;


    const dataN= makeDirectionArrowDrawObj(sptStart, spt2,'N');
    const dataE= makeDirectionArrowDrawObj(sptStart, sptE2,'E');
    return [dataE, dataN];
}

function getEastFromNorthOnScreen(cc, origin, nVec, sign) {
    const originSpt = cc.getScreenCoords(origin);
    const vec1Spt = cc.getScreenCoords(nVec);
    const x2 = sign*(vec1Spt.y - originSpt.y) + originSpt.x;
    const y2 = -sign*(vec1Spt.x - originSpt.x) + originSpt.y;
    return makeScreenPt(x2, y2);
}
