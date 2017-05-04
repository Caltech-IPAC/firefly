/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import {take} from 'redux-saga/effects';
import ImagePlotCntlr, {WcsMatchType, IMAGE_PLOT_KEY,
                       dispatchGroupLocking, dispatchZoom, dispatchRotate, dispatchFlip,
                       dispatchUpdateViewSize, dispatchRecenter, ActionScope} from '../ImagePlotCntlr.js';
import {getPlotViewById, primePlot, applyToOnePvOrGroup, findPlotGroup} from '../PlotViewUtil.js';
import {PlotAttribute} from '../WebPlot.js';
import {FullType, isPlotNorth, isEastLeftOfNorth, getRotationAngle} from '../VisUtil.js';
import {getEstimatedFullZoomFactor, getArcSecPerPix, getZoomLevelForScale, UserZoomTypes} from '../ZoomUtil.js';
import {RotateType} from '../PlotState.js';
import {CCUtil} from '../CsysConverter.js';
import {ZoomType} from '../ZoomType.js';
import {makeScreenPt} from '../Point.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';


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
            syncPlotToLevel(pv, masterPv, asPerPix);
            dispatchUpdateViewSize(pv.plotId);
        }
    }

}


export function wcsMatchActionCreator(action) {
    return (dispatcher, getState) => {
        const {plotId}= action.payload;
        const matchType= WcsMatchType.get(action.payload.matchType);
        let visRoot= getState()[IMAGE_PLOT_KEY];
        let masterPv= getPlotViewById(visRoot, plotId);

        const width= get(masterPv,'viewDim.width',false);
        const height= get(masterPv,'viewDim.height',false);

        let group= findPlotGroup(masterPv.plotGroupId, visRoot.plotGroupAry);


        if (!matchType || !width  || !height) {
            dispatcher({
                type: ImagePlotCntlr.WCS_MATCH,
                payload: {wcsMatchCenterWP:null,wcsMatchType:matchType,mpwWcsPrimId:plotId}
            });
            if (matchType) {
                applyToOnePvOrGroup(visRoot.plotViewAry, masterPv.plotId, group,
                    (pv) => {
                        if (masterPv.plotId!==pv.plotId) {
                            dispatchAddSaga( watchForCompletedPlot, {plotId:pv.plotId, masterPlotId:plotId, wcsMatchType:matchType});
                        }
                    }
                );
            }
            else {
                applyToOnePvOrGroup(visRoot.plotViewAry, masterPv.plotId, group,
                    (pv) => dispatchUpdateViewSize(pv.plotId));
            }
            return;
        }

        const wcsMatchCenterWP= findWcsMatchPoint(masterPv, plotId, matchType);



        dispatcher({
            type: ImagePlotCntlr.WCS_MATCH,
            payload: {wcsMatchCenterWP,wcsMatchType:matchType,mpwWcsPrimId:masterPv.plotId}
        });
        dispatchGroupLocking(masterPv.plotId,true);

        visRoot= getState()[IMAGE_PLOT_KEY];
        group= findPlotGroup(masterPv.plotGroupId, visRoot.plotGroupAry);
        masterPv= getPlotViewById(visRoot, plotId);
        const masterPlot= primePlot(masterPv);

        const level = matchType===WcsMatchType.Standard  || matchType===WcsMatchType.Target ?
                  masterPlot.zoomFactor :
                  getEstimatedFullZoomFactor(primePlot(masterPv),masterPv.viewDim, FullType.WIDTH_HEIGHT);
        const asPerPix= getArcSecPerPix(masterPlot,level);



        dispatchUpdateViewSize(masterPv.plotId);

        if (matchType===WcsMatchType.Target) {
            const ft=  masterPlot.attributes[PlotAttribute.FIXED_TARGET];
            if (ft) dispatchRecenter({plotId:masterPv.plotId, centerPt:ft});
        }

        applyToOnePvOrGroup(visRoot.plotViewAry, masterPv.plotId, group,
                     (pv) => {
                         if (masterPv.plotId!==pv.plotId) {
                             syncPlotToLevel(pv, masterPv, asPerPix);
                             dispatchUpdateViewSize(pv.plotId);
                         }
                     }
            );
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


function syncPlotToLevel(pv, masterPv, targetASpix) {
    const plot= primePlot(pv);
    if (!plot) return;
    const currZoomLevel= plot.zoomFactor;


    const targetLevel= getZoomLevelForScale(plot, targetASpix);
    // we want each plot to have the same arcsec / pixel as the target level
    // if the new level is only slightly different then use the target level
    const newZoomLevel= (Math.abs(targetLevel-currZoomLevel)<.01) ? currZoomLevel : targetLevel;

    if (!isFlipYMatching(pv, masterPv)) dispatchFlip({plotId:pv.plotId, actionScope: ActionScope.SINGLE});


    if (!isRotationMatching(pv, masterPv)) rotateToMatch(pv, masterPv, masterPv.flipY);
    zoomToLevel(plot, newZoomLevel);
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

function rotateToMatch(pv, masterPv, flipY) {
    const plot= primePlot(pv);
    const masterPlot= primePlot(masterPv);
    if (!plot) return;
    const masterRot= masterPv.rotation * (flipY ? -1 : 1);
    var targetRotation= ((getRotationAngle(masterPlot)+  masterRot)  -
                           (getRotationAngle(plot))) * (flipY ? 1 : -1);
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


function isNorth(pv) {
    const plot= primePlot(pv);
    if (!plot) return false;
    return (pv.plotViewCtx.rotateNorthLock || (isPlotNorth(plot) && !pv.rotation) );
}

function isRotationMatching(pv1, pv2) {
    const p1= primePlot(pv1);
    const p2= primePlot(pv2);

    if (!p1 || !p2) return false;

    if (isNorth(pv1) && isNorth(pv2)) {
        return true;
    }
    else {
        const r1= getRotationAngle(p1) + pv1.rotation;
        const r2= getRotationAngle(p2) + pv1.rotation;
        return Math.abs(r1-r2) < .9;
    }
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

