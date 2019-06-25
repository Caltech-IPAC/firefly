/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import {take} from 'redux-saga/effects';
import ImagePlotCntlr, {
    ActionScope,
    dispatchFlip,
    dispatchPositionLocking,
    dispatchRecenter,
    dispatchRotate,
    dispatchUpdateViewSize,
    dispatchZoom,
    IMAGE_PLOT_KEY,
    WcsMatchType
} from '../ImagePlotCntlr.js';
import {applyToOnePvOrAll, getPlotViewById, primePlot} from '../PlotViewUtil.js';
import {PlotAttribute} from '../WebPlot.js';
import {FullType, getRotationAngle, isEastLeftOfNorth, isPlotNorth, isRotationMatching} from '../VisUtil.js';
import {getArcSecPerPix, getEstimatedFullZoomFactor, getZoomLevelForScale, UserZoomTypes} from '../ZoomUtil.js';
import {RotateType} from '../PlotState.js';
import {CCUtil} from '../CsysConverter.js';
import {ZoomType} from '../ZoomType.js';
import {makeScreenPt} from '../Point.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {getCenterOfProjection, hasWCSProjection} from '../PlotViewUtil';
import {isHiPS, isImage} from '../WebPlot';
import {matchHiPStoPlotView} from './PlotHipsTask';
import {dispatchChangeCenterOfProjection, dispatchChangeHiPS} from '../ImagePlotCntlr';


export function* watchForCompletedPlot(options, dispatch, getState) {


    let masterPlot;
    let plot;

    while (!masterPlot || !plot) {
        const action = yield take([ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_IMAGE_FAIL]);
        const {plotId, masterPlotId, wcsMatchType}= options;

        if (action.type===ImagePlotCntlr.PLOT_IMAGE_FAIL && action.payload.plotId===plotId) {
            return;
        }
        const visRoot= getState()[IMAGE_PLOT_KEY];
        masterPlot= primePlot(visRoot, masterPlotId);
        plot= primePlot(visRoot, plotId);


        if (masterPlot && plot) {
            const masterPv= getPlotViewById(visRoot, masterPlotId);
            const pv= getPlotViewById(visRoot, plotId);
            const level = wcsMatchType===WcsMatchType.Standard  || wcsMatchType===WcsMatchType.Target ?
                masterPlot.zoomFactor :
                getEstimatedFullZoomFactor(primePlot(masterPv),masterPv.viewDim, FullType.WIDTH_HEIGHT);
            const asPerPix= getArcSecPerPix(masterPlot,level);
            if (wcsMatchType===WcsMatchType.Target) {
                const ft=  masterPlot.attributes[PlotAttribute.FIXED_TARGET];
                if (ft) dispatchRecenter({plotId:masterPv.plotId, centerPt:ft});
            }
            syncPlotToLevelForWcsMatching(pv, masterPv, asPerPix);
            dispatchUpdateViewSize(pv.plotId);
        }
    }

}


export function wcsMatchActionCreator(action) {
    return (dispatcher, getState) => {
        let visRoot= getState()[IMAGE_PLOT_KEY];
        const plotId= action.payload.plotId || visRoot.activePlotId || get(visRoot.plotViewAry, '0.plotId');
        const matchType= WcsMatchType.get(action.payload.matchType);
        const {lockMatch}= action.payload;

        if (!plotId && lockMatch) {
            dispatchPositionLocking(undefined, lockMatch); //TODO:
            if (lockMatch) dispatcher({ type: ImagePlotCntlr.WCS_MATCH, payload: {wcsMatchType:matchType} });
            return;
        }
        let masterPv= getPlotViewById(visRoot, plotId);

        const width= get(masterPv,'viewDim.width',false);
        const height= get(masterPv,'viewDim.height',false);
        const image= isImage(primePlot(masterPv));
        const hips= isHiPS(primePlot(masterPv));

        // if (!matchType || !width  || !height) {
        if (image && lockMatch && (!width  || !height)) {
            dispatcher({
                type: ImagePlotCntlr.WCS_MATCH,
                payload: {wcsMatchCenterWP:null,wcsMatchType:matchType,mpwWcsPrimId:plotId, lockMatch}
            });
            applyToOnePvOrAll(true, visRoot.plotViewAry, masterPv.plotId, false,
                (pv) => {
                    if (masterPv.plotId!==pv.plotId) {
                        dispatchAddSaga( watchForCompletedPlot, {plotId:pv.plotId, masterPlotId:plotId, wcsMatchType:matchType});
                    }
                }
            );
            return;
        }
        else if (!lockMatch && (!width  || !height)) {
            return;
        }

        const wcsMatchCenterWP= findWcsMatchPoint(masterPv, plotId, matchType);

        dispatcher({
            type: ImagePlotCntlr.WCS_MATCH,
            payload: {wcsMatchCenterWP,wcsMatchType:matchType,mpwWcsPrimId:masterPv.plotId}
        });


        if (!matchType) {
            dispatchPositionLocking(masterPv.plotId,false);
            return;
        }


        dispatchPositionLocking(masterPv.plotId,true);

        visRoot= getState()[IMAGE_PLOT_KEY];
        masterPv= getPlotViewById(visRoot, plotId);
        const masterPlot= primePlot(masterPv);
        if (!masterPlot) return;

        if (image) {
            const level = matchType ?
                masterPlot.zoomFactor :
                getEstimatedFullZoomFactor(primePlot(masterPv),masterPv.viewDim, FullType.WIDTH_HEIGHT);
            const asPerPix= getArcSecPerPix(masterPlot,level);

            dispatchUpdateViewSize(masterPv.plotId);

            if (matchType===WcsMatchType.Target) {
                const ft=  masterPlot.attributes[PlotAttribute.FIXED_TARGET];
                if (ft) dispatchRecenter({plotId:masterPv.plotId, centerPt:ft});
            }

            applyToOnePvOrAll(true, visRoot.plotViewAry, masterPv.plotId, false,
                (pv) => {
                    if (masterPv.plotId!==pv.plotId && isImage(primePlot(pv))) {
                        if (matchType===WcsMatchType.Pixel || matchType===WcsMatchType.PixelCenter) {
                            syncPlotToLevelForPixelMatching(pv,masterPv);
                        }
                        else {
                            syncPlotToLevelForWcsMatching(pv, masterPv, asPerPix);
                        }
                        dispatchUpdateViewSize(pv.plotId);
                    }
                }
            );
            matchHiPStoPlotView(visRoot,masterPv);
        }
        else if (hips) {
            dispatchZoom({plotId, userZoomType: UserZoomTypes.LEVEL, level:masterPlot.zoomFactor});
            dispatchChangeCenterOfProjection({plotId,centerProjPt:getCenterOfProjection(masterPlot)});
            dispatchChangeHiPS({plotId, coordSys: masterPlot.imageCoordSys});
        }

        if (!lockMatch) {
            dispatchPositionLocking(masterPv.plotId,false);
            dispatcher({
                type: ImagePlotCntlr.WCS_MATCH,
                payload: {wcsMatchCenterWP,wcsMatchType:false,mpwWcsPrimId:masterPv.plotId}
            });
        }
    };
}






export function modifyRequestForWcsMatch(pv, wpr) {
    const plot= primePlot(pv);
    if (!plot) return wpr;
    const newWpr= wpr.makeCopy();
    const asPerPix= getArcSecPerPix(plot,plot.zoomFactor);
    newWpr.setRotateNorth(false);
    newWpr.setRotate(false);
    if (isPlotNorth(plot)) {
        newWpr.setRotateNorth(true);
    }
    else {
        const targetRotation= getRotationAngle(plot) + pv.rotation;
        newWpr.setRotate(true);
        newWpr.setRotationAngle(targetRotation);
    }
    newWpr.setZoomType(ZoomType.ARCSEC_PER_SCREEN_PIX);
    newWpr.setZoomArcsecPerScreenPix(asPerPix);
    return newWpr;
}


/**
 * @param {PlotView} pv
 * @param {PlotView} masterPv
 * @param {number} targetASpix
 */
function syncPlotToLevelForWcsMatching(pv, masterPv, targetASpix) {
    const plot= primePlot(pv);
    if (!plot) return;
    if (!hasWCSProjection(pv)) return;
    const currZoomLevel= plot.zoomFactor;


    const targetLevel= getZoomLevelForScale(plot, targetASpix);
    // we want each plot to have the same arcsec / pixel as the target level
    // if the new level is only slightly different then use the target level
    const newZoomLevel= (Math.abs(targetLevel-currZoomLevel)<.01) ? currZoomLevel : targetLevel;

    if (!isFlipYMatching(pv, masterPv)) dispatchFlip({plotId:pv.plotId, actionScope: ActionScope.SINGLE});


    if (!isRotationMatching(pv, masterPv)) rotateToMatch(pv, masterPv);
    zoomToLevel(plot, newZoomLevel);
}


/**
 * @param {PlotView} pv
 * @param {PlotView} masterPv
 */
function syncPlotToLevelForPixelMatching(pv, masterPv) {
    const plot= primePlot(pv);
    const masterPlot= primePlot(masterPv);
    if (!plot || !masterPlot) return;


    if (pv.flipY!==masterPv.flipY) dispatchFlip({plotId:pv.plotId, actionScope: ActionScope.SINGLE});
    if (pv.rotation!==masterPv.rotation) {
        dispatchRotate({ plotId: plot.plotId, rotateType: RotateType.ANGLE,
            angle: 360-masterPv.rotation, actionScope: ActionScope.SINGLE, });
    }
    zoomToLevel(plot, masterPlot.zoomFactor);
}


function zoomToLevel(plot, newZoomLevel) {
    if (newZoomLevel!==plot.zoomFactor) {
        dispatchZoom({
            plotId: plot.plotId,
            userZoomType: UserZoomTypes.LEVEL,
            level: newZoomLevel,
            zoomLockingEnabled: true,
            actionScope: ActionScope.SINGLE,
            maxCheck: false
        });
    }
}

function rotateToMatch(pv, masterPv) {
    const plot= primePlot(pv);
    const masterPlot= primePlot(masterPv);
    if (!plot) return;
    const masterRot= masterPv.rotation * (masterPv.flipY ? -1 : 1);
    let targetRotation= ((getRotationAngle(masterPlot)+  masterRot)  -
                           (getRotationAngle(plot))) * (masterPv.flipY ? 1 : -1);
    if (targetRotation<0) targetRotation+= 360;
    dispatchRotate({
        plotId: plot.plotId,
        rotateType: RotateType.ANGLE,
        angle: targetRotation,
        actionScope: ActionScope.SINGLE,
    });
}



function isFlipYMatching(pv1, pv2) {
    return isEast(pv1) === isEast(pv2);
}

function isEast(pv) {
    const p= primePlot(pv);
    if (!p) return true;
    const imageDataEast= isEastLeftOfNorth(p);
    return (imageDataEast && !pv.flipY) || (!imageDataEast && pv.flipY);
}


/**
 *
 * @param {PlotView} pv
 * @param {String} plotId
 * @param {Enum} matchType
 * @return {WorldPt}
 */
function findWcsMatchPoint(pv, plotId, matchType) {
    const p= primePlot(pv);
    if (!p) return null;
    switch (matchType) {
        case WcsMatchType.Standard:
            return CCUtil.getWorldCoords(p, makeScreenPt(p.screenSize.width/2,p.screenSize.height/2));
    }
    return null;
}

