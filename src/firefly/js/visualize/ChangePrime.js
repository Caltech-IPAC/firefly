/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import ImagePlotCntlr, {IMAGE_PLOT_KEY, dispatchZoom, dispatchProcessScroll} from './ImagePlotCntlr.js';
import {getPlotViewById, primePlot} from './PlotViewUtil.js';
import {getPixScaleArcSec, getScreenPixScaleArcSec} from './WebPlot.js';
import {hasWCSProjection} from './PlotViewUtil.js';
import {UserZoomTypes, getZoomLevelForScale} from './ZoomUtil.js';
import {CysConverter} from './CsysConverter.js';
import {makeScreenPt} from './Point.js';
import {dispatchAddActionWatcher} from '../core/MasterSaga';

function matcher(oldP,newP) {
    return {
        isSameZoomLevel: oldP.zoomFactor===newP.zoomFactor,
        isSameSize: oldP.dataWidth===newP.dataWidth && oldP.dataHeight===newP.dataHeight,
        isProjection: hasWCSProjection(oldP) && hasWCSProjection(newP),
        isSameScale: hasWCSProjection(oldP) && hasWCSProjection(newP) &&
            getPixScaleArcSec(oldP)===getPixScaleArcSec(newP)
    };
}


function getZoomDecision(oldP,newP) {
    const m= matcher(oldP,newP);
    if (m.isProjection) {
        if (m.isSameScale && m.isSameSize && m.isSameZoomLevel) { //match by level
            return {zoom:false};
        }
        else if (m.isSameScale) { //match by level
            return {zoom:true, zoomByScale:false};
        }
        else { // match by scale
            return {zoom:true, zoomByScale:true};
        }
    }
    else { // match by level
        if (m.isSameSize && m.isSameZoomLevel)  {
            return {zoom:false};
        }
        else {
            return {zoom:true, zoomByScale:false};
        }
    }
}

export function changePrime(rawAction, dispatcher, getState) {
    const {plotId,primeIdx:newPrimeIdx}= rawAction.payload;
    const visRoot= getState()[IMAGE_PLOT_KEY];
    const pv= getPlotViewById(visRoot, plotId);
    const {plots,primeIdx}= pv;
    const oldP= plots[primeIdx];
    const newP= plots[newPrimeIdx];
    if (newPrimeIdx>=plots.length) return;

    const cc= CysConverter.make(primePlot(pv));
    const scrollToImagePt= cc.getImageCoords(makeScreenPt(pv.scrollX,pv.scrollY));
    dispatcher(rawAction);
    checkZoom(plotId,oldP, newP, scrollToImagePt,visRoot);
}

/** @type actionWatcherCallback */
function zoomCompleteWatch(action, cancelSelf, {plotId,scrollToImagePt},dispatch,getState) {
    if (action.payload.plotId===plotId) {
        const visRoot= getState()[IMAGE_PLOT_KEY];
        (action.type===ImagePlotCntlr.ZOOM_IMAGE) && changeScrollToImagePt(visRoot,plotId,scrollToImagePt);
        cancelSelf();
    }
}

function changeScrollToImagePt(visRoot, plotId, scrollToImagePt) {
    const pv= getPlotViewById(visRoot,plotId);
    const cc= CysConverter.make(primePlot(pv));
    dispatchProcessScroll({plotId, scrollPt:cc.getScreenCoords(scrollToImagePt)});
}

const addWatcher= (plotId,scrollToImagePt) => dispatchAddActionWatcher( {
    callback:zoomCompleteWatch, params:{plotId,scrollToImagePt}, actions:[ImagePlotCntlr.ZOOM_IMAGE] });


function checkZoom(plotId, oldP, newP, scrollToImagePt, visRoot) {
    const zoomOp= getZoomDecision(oldP,newP);
    if (zoomOp.zoom) {
        if (zoomOp.zoomByScale) {
            const targetArcSecPix= getScreenPixScaleArcSec(oldP);
            const level= getZoomLevelForScale(newP,targetArcSecPix);
            addWatcher(plotId,scrollToImagePt);
            dispatchZoom({ plotId, userZoomType:UserZoomTypes.LEVEL, maxCheck:false, level });
        }
        else {
            addWatcher(plotId,scrollToImagePt);
            dispatchZoom({ plotId, userZoomType:UserZoomTypes.LEVEL, maxCheck:false, level:oldP.zoomFactor });
        }
    }
    else {
        changeScrollToImagePt(visRoot,plotId,scrollToImagePt);
    }
}
