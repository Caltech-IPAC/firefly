/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {isNil} from 'lodash';
import ImagePlotCntlr, {IMAGE_PLOT_KEY, dispatchZoom, dispatchRotate,
       dispatchProcessScroll,ActionScope} from './ImagePlotCntlr.js';
import {getPlotViewById, primePlot} from './PlotViewUtil.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {UserZoomTypes, getZoomLevelForScale} from './ZoomUtil.js';
import {RotateType} from './PlotState.js';
import {CysConverter} from './CsysConverter.js';
import {makeScreenPt} from './Point.js';



function matcher(oldP,newP) {
    return {
        isSameZoomLevel: oldP.zoomFactor===newP.zoomFactor,
        isSameSize: oldP.dataWidth===newP.dataWidth && oldP.dataHeight===newP.dataHeight,
        isProjection: oldP.projection.isSpecified() && newP.projection.isSpecified(),
        isSameScale: oldP.projection && oldP.projection.isSpecified() &&
                     newP.projection && newP.projection.isSpecified() &&
                     oldP.projection.getPixelScaleArcSec()===newP.projection.getPixelScaleArcSec()
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


    const doRotate= checkRotation(plotId,oldP,newP);
    if (doRotate) {
        dispatchAddSaga(zoomRotateSega,{plotId,oldP,newP, scrollToImagePt});
        doRotate();
    }
    else {
        checkZoom(plotId,oldP, newP, scrollToImagePt,visRoot);
    }
}




function* zoomRotateSega({plotId,oldP,newP, scrollToImagePt}, dispatch,getState) {
    while(true) {
        var action= yield take([ImagePlotCntlr.ROTATE]);
        const visRoot= getState()[IMAGE_PLOT_KEY];
        const {plotId:testPlotId}= action.payload.pvNewPlotInfoAry[0];
        if (testPlotId===plotId) {
            checkZoom(plotId,oldP,newP, scrollToImagePt,visRoot);
            return;
        }
    }
}

function* zoomCompleteSega({plotId,scrollToImagePt},dispatch,getState) {
    while(true) {
        var action= yield take([ImagePlotCntlr.ZOOM_IMAGE]);
        const {plotId:testPlotId}= action.payload;
        if (testPlotId===plotId) {
            const visRoot= getState()[IMAGE_PLOT_KEY];
            changeScrollToImagePt(visRoot,plotId,scrollToImagePt);
            return;
        }
    }
}

function changeScrollToImagePt(visRoot, plotId, scrollToImagePt) {
    const pv= getPlotViewById(visRoot,plotId);
    const cc= CysConverter.make(primePlot(pv));
    dispatchProcessScroll({plotId, scrollPt:cc.getScreenCoords(scrollToImagePt)});
}


function checkZoom(plotId, oldP, newP, scrollToImagePt, visRoot) {
    const zoomOp= getZoomDecision(oldP,newP);
    if (zoomOp.zoom) {
        if (zoomOp.zoomByScale) {
            const targetArcSecPix= oldP.projection.getPixelScaleArcSec() / oldP.zoomFactor;
            const level= getZoomLevelForScale(newP,targetArcSecPix);
            dispatchAddSaga(zoomCompleteSega,{plotId,scrollToImagePt});
            dispatchZoom({
                plotId,
                userZoomTypes:UserZoomTypes.LEVEL,
                maxCheck:false, 
                level
            });
        }
        else {
            dispatchAddSaga(zoomCompleteSega,{plotId,scrollToImagePt});
            dispatchZoom({
                plotId,
                userZoomTypes:UserZoomTypes.LEVEL,
                maxCheck:false, 
                level:oldP.zoomFactor
            });
        }
    }
    else {
        changeScrollToImagePt(visRoot,plotId,scrollToImagePt);
    }
}



function checkRotation(plotId,oldP,newP) {

    const oldRotType= oldP.plotState.getRotateType();
    const newRotType= newP.plotState.getRotateType();
    const oldAngle= oldP.plotState.getRotationAngle();
    const newAngle= newP.plotState.getRotationAngle();
    const differentAngles= Math.floor(oldAngle)!==Math.floor(newAngle);

    if (oldRotType===RotateType.NORTH && newRotType!==RotateType.NORTH) {
        return () => dispatchRotate({plotId,
                                     rotateType:RotateType.NORTH,  
                                     actionScope:ActionScope.SINGLE
                                    });
    }

    if (oldRotType===RotateType.ANGLE && (differentAngles || isNil(newAngle))) {
        return () => dispatchRotate({plotId,
                                     rotateType:RotateType.ANGLE, 
                                     angle:oldAngle, 
                                     actionScope:ActionScope.SINGLE
                                     });
    }

    if (oldRotType===RotateType.UNROTATE && newRotType!==RotateType.UNROTATE) {
        return () => dispatchRotate({plotId,
                                     rotateType:RotateType.UNROTATE, 
                                     actionScope:ActionScope.SINGLE
                                    });
    }
    return false;
}


