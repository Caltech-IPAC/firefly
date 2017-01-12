/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import ImagePlotCntlr, {WcsMatchType, IMAGE_PLOT_KEY,
                       dispatchGroupLocking, dispatchZoom, dispatchRotate,
                       dispatchUpdateViewSize, dispatchRecenter, ActionScope} from '../ImagePlotCntlr.js';
import {getPlotViewById, primePlot, applyToOnePvOrGroup, findPlotGroup} from '../PlotViewUtil.js';
import {PlotAttribute} from '../WebPlot.js';
import PlotView from '../reducer/PlotView.js';
import {getCenterPtOfPlot, FullType, isPlotNorth, getRotationAngle} from '../VisUtil.js';
import {getEstimatedFullZoomFactor, getArcSecPerPix, getZoomLevelForScale, UserZoomTypes} from '../ZoomUtil.js';
import {RotateType} from '../PlotState.js';
import {CCUtil} from '../CsysConverter.js';
import {ZoomType} from '../ZoomType.js';
import {makeScreenPt} from '../Point.js';



export function wcsMatchActionCreator(action) {
    return (dispatcher, getState) => {
        var {plotId, matchType}= action.payload;
        matchType= WcsMatchType.get(matchType);
        var visRoot= getState()[IMAGE_PLOT_KEY];
        var masterPv= getPlotViewById(visRoot, plotId);

        const width= get(masterPv,'viewDim.width',false);
        const height= get(masterPv,'viewDim.height',false);

        var group= findPlotGroup(masterPv.plotGroupId, visRoot.plotGroupAry);


        if (!matchType || !width  || !height) {
            dispatcher({
                type: ImagePlotCntlr.WCS_MATCH,
                payload: {wcsMatchCenterWP:null,wcsMatchType:matchType,mpwWcsPrimId:plotId}
            });
            applyToOnePvOrGroup(visRoot.plotViewAry, masterPv.plotId, group,
                (pv) => dispatchUpdateViewSize(pv.plotId));
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
                             syncPlotToLevel(primePlot(pv), masterPlot, asPerPix);
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
        const targetRotation= getRotationAngle(plot);
        newWpr.setRotate(true);
        newWpr.setRotationAngle(targetRotation);
    }
    newWpr.setZoomType(ZoomType.ARCSEC_PER_SCREEN_PIX);
    newWpr.setZoomArcsecPerScreenPix(asPerPix);
    return newWpr;
}


function syncPlotToLevel(plot, masterPlot, targetASpix) {
    if (!plot) return;
    const currZoomLevel= plot.zoomFactor;


    const targetLevel= getZoomLevelForScale(plot, targetASpix);
    // we want each plot to have the same arcsec / pixel as the target level
    // if the new level is only slightly different then use the target level
    const newZoomLevel= (Math.abs(targetLevel-currZoomLevel)<.01) ? currZoomLevel : targetLevel;

    if (isRotationMatching(plot, masterPlot)) {
        zoomToLevel(plot, targetLevel);
    }
    else {
        rotateToMatch(plot, masterPlot, newZoomLevel);
    }
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

function rotateToMatch(plot, masterPlot, newZoomLevel) {
    const targetRotation= getRotationAngle(plot) - getRotationAngle(masterPlot);
    dispatchRotate({
        plotId: plot.plotId,
        rotateType: RotateType.ANGLE,
        angle: targetRotation,
        keepWcsLock : true,
        newZoomLevel,
        actionScope: ActionScope.SINGLE,
    });
}





function isNorth(plot) {
    if (!plot) return false;
    const {plotState}= plot;
    return (plotState.getRotateType()===RotateType.NORTH || isPlotNorth(plot) );
}

function isRotationMatching(p1, p2) {
    if (!p1 || !p2) return false;

    if (isNorth(p1) && isNorth(p2)) {
        return true;
    }
    else {
        const r1= getRotationAngle(p1);
        const r2= getRotationAngle(p2);
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

