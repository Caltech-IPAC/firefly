/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import ImagePlotCntlr, {WcsMatchType, IMAGE_PLOT_KEY,
                       dispatchGroupLocking, dispatchZoom, dispatchRotate,
                       dispatchUpdateViewSize, ActionScope} from '../ImagePlotCntlr.js';
import {getPlotViewById, primePlot, applyToOnePvOrGroup, findPlotGroup} from '../PlotViewUtil.js';
import {PlotAttribute} from '../WebPlot.js';
import PlotView from '../reducer/PlotView.js';
import {getCenterPtOfPlot, FullType, isPlotNorth, getRotationAngle} from '../VisUtil.js';
import {getEstimatedFullZoomFactor, getArcSecPerPix, getZoomLevelForScale, UserZoomTypes} from '../ZoomUtil.js';
import {RotateType} from '../PlotState.js';
import {CCUtil} from '../CsysConverter.js';
import {makeScreenPt} from '../Point.js';



export function wcsMatchActionCreator(action) {
    return (dispatcher, getState) => {
        var {plotId, matchType}= action.payload;
        matchType= WcsMatchType.get(matchType);
        var visRoot= getState()[IMAGE_PLOT_KEY];
        var masterPv= getPlotViewById(visRoot, plotId);
        const northUp= matchType===WcsMatchType.NorthCenOnPt || matchType===WcsMatchType.NorthCenOnMoving;

        const width= get(masterPv,'viewDim.width',false);
        const height= get(masterPv,'viewDim.height',false);

        const group= findPlotGroup(masterPv.plotGroupId, visRoot.plotGroupAry);

        if (!matchType || matchType.Off || !width  || !height) {
            dispatcher({
                type: ImagePlotCntlr.WCS_MATCH,
                payload: {wcsMatchCenterWP:null,wcsMatchType:false,mpwWcsPrimId:null}
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
        masterPv= getPlotViewById(visRoot, plotId);
        const masterPlot= primePlot(masterPv);

        const level = matchType===WcsMatchType.Standard ?
                  masterPlot.zoomFactor :
                  getEstimatedFullZoomFactor(primePlot(masterPv),masterPv.viewDim, FullType.WIDTH_HEIGHT);
        const asPerPix= getArcSecPerPix(masterPlot,level);



        applyToOnePvOrGroup(visRoot.plotViewAry, masterPv.plotId, group,
                     (pv) => {
                         syncPlotToLevel(primePlot(pv), masterPlot, asPerPix, northUp);
                         dispatchUpdateViewSize(pv.plotId);
                     }
            );
    };
}


function syncPlotToLevel(plot, masterPlot, targetASpix, northUp) {
    if (!plot) return;
    const currZoomLevel= plot.zoomFactor;
    const targetLevel= getZoomLevelForScale(plot, targetASpix);
    // we want each plot to have the same arcsec / pixel as the target level
    // if the new level is only slightly different then use the target level
    const newZoomLevel= (Math.abs(targetLevel-currZoomLevel)<.01) ? currZoomLevel : targetLevel;

    if (northUp) {
        if (isNorth(plot)) {
            zoomToLevel(plot, targetLevel);
        }
        else {
            rotateNorth(plot,newZoomLevel);
        }
    }
    else {
        if (isRotationMatching(plot, masterPlot)) {
            zoomToLevel(plot, targetLevel);
        }
        else {
            rotateToMatch(plot, masterPlot, newZoomLevel);
        }
    }
}


function zoomToLevel(plot, newZoomLevel) {
    if (newZoomLevel!==plot.zoomLevel) {
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

function rotateNorth(plot, newZoomLevel) {
    dispatchRotate({
        plotId: plot.plotId,
        rotateType: RotateType.NORTH,
        newZoomLevel,
        keepWcsLock : true,
        actionScope: ActionScope.SINGLE,
    });
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
    switch (matchType) {
        case WcsMatchType.NorthCenOnPt:
            return p.attributes[PlotAttribute.FIXED_TARGET] || getCenterPtOfPlot(p);
        case WcsMatchType.NorthCenOnMoving:
            return null;
        case WcsMatchType.Standard:
            return CCUtil.getWorldCoords(p, makeScreenPt(p.screenSize.width/2,p.screenSize.height/2));
    }
    return null;
}
