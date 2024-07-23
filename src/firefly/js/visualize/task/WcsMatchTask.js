/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isEmpty} from 'lodash';
import ImagePlotCntlr, {
    ActionScope, dispatchAttributeChange, dispatchChangeCenterOfProjection, dispatchChangeHiPS, dispatchFlip,
    dispatchPositionLocking, dispatchRecenter, dispatchRotate, dispatchUpdateViewSize, dispatchZoom, IMAGE_PLOT_KEY,
    visRoot, WcsMatchType
} from '../ImagePlotCntlr.js';
import {isEastLeftOfNorth, isPlotRotatedNorth} from '../WebPlotAnalysis';
import {
    applyToOnePvOrAll, findCurrentCenterPoint, getCenterOfProjection, getCorners, getDrawLayerByType,
    getMatchingRotationAngle,
    getPlotViewAry, getPlotViewById, hasWCSProjection, isRotationMatching, primePlot
} from '../PlotViewUtil.js';
import {isHiPS, isImage} from '../WebPlot.js';
import {PlotAttribute} from '../PlotAttribute';
import {
    FullType, getArcSecPerPix, getEstimatedFullZoomFactor, getZoomLevelForScale, UserZoomTypes
} from '../ZoomUtil.js';
import {RotateType} from '../PlotState.js';
import {CCUtil} from '../CsysConverter.js';
import {makeScreenPt, pointEquals} from '../Point.js';
import CoordinateSys from '../CoordSys';
import {dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dlRoot} from '../DrawLayerCntlr';
import ImageOutline from '../../drawingLayers/ImageOutline';
import {dispatchAddActionWatcher} from '../../core/MasterSaga';


function watchForCompletedPlot(action, cancelSelf, params, dispatch, getState) {

    const {plotId, masterPlotId, wcsMatchType}= params;

    if (action.type===ImagePlotCntlr.PLOT_IMAGE_FAIL) {
        if (action.payload.plotId===plotId) cancelSelf();
        return params;
    }
    const visRoot= getState()[IMAGE_PLOT_KEY];
    const masterPlot= primePlot(visRoot, masterPlotId);
    const plot= primePlot(visRoot, plotId);
    if (!masterPlot || !plot) return params;

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
    cancelSelf();
}


export function wcsMatchActionCreator(action) {
    return (dispatcher, getState) => {
        let visRoot= getState()[IMAGE_PLOT_KEY];
        const plotId= action.payload.plotId || visRoot.activePlotId || visRoot.plotViewAry[0]?.plotId;
        const matchType= WcsMatchType.get(action.payload.matchType);
        const {lockMatch}= action.payload;

        if (!plotId && lockMatch) {
            dispatchPositionLocking(undefined, lockMatch); //TODO:
            if (lockMatch) dispatcher({ type: ImagePlotCntlr.WCS_MATCH, payload: {wcsMatchType:matchType} });
            return;
        }
        let masterPv= getPlotViewById(visRoot, plotId);

        const width= masterPv?.viewDim?.width ?? false;
        const height= masterPv?.viewDim?.height ?? false;
        const image= isImage(primePlot(masterPv));
        const hips= isHiPS(primePlot(masterPv));

        if (image && lockMatch && (!width  || !height)) {
            dispatcher({
                type: ImagePlotCntlr.WCS_MATCH,
                payload: {wcsMatchCenterWP:null,wcsMatchType:matchType,mpwWcsPrimId:plotId, lockMatch}
            });
            applyToOnePvOrAll(true, visRoot.plotViewAry, masterPv.plotId, false,
                (pv) => {
                    if (masterPv.plotId!==pv.plotId) {
                        dispatchAddActionWatcher( {
                            callback: watchForCompletedPlot,
                            params: {plotId:pv.plotId, masterPlotId:plotId, wcsMatchType:matchType},
                            actions: [ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_IMAGE_FAIL]
                        } );
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
        if (!masterPlot || !hasWCSProjection(masterPlot)) return;

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
            (matchType===WcsMatchType.Standard || matchType===WcsMatchType.Target) && matchHiPStoPlotView(visRoot,masterPv);
        }
        else if (hips) {
            dispatchZoom({plotId, userZoomType: UserZoomTypes.LEVEL, level:masterPlot.zoomFactor});
            dispatchChangeCenterOfProjection({plotId,centerProjPt:getCenterOfProjection(masterPlot)});
            dispatchChangeHiPS({plotId, coordSys: masterPlot.imageCoordSys});
            visRoot= getState()[IMAGE_PLOT_KEY];
            const imagePv= visRoot.plotViewAry.find( (aPv) => isImage(primePlot(aPv)));
            const hipsPV= getPlotViewById(visRoot,plotId);
            matchImageToHips(hipsPV,imagePv);
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


/**
 * @param {VisRoot} vr
 * @param {String} plotId
 */
export function locateOtherIfMatched(vr,plotId) {
    const pv = getPlotViewById(vr, plotId);
    if (vr.wcsMatchType !== WcsMatchType.Target && vr.wcsMatchType !== WcsMatchType.Standard) return;
    if (isImage(primePlot(pv))) matchHiPStoPlotView(vr, pv);
    else if (isHiPS(primePlot(pv))) {
        const imagePv= vr.plotViewAry.find( (aPv) => isImage(primePlot(aPv)));
        matchImageToHips(pv, imagePv);
    }
}



export const {matchImageToHips, matchHiPStoPlotView}= (() => {
    let lock= false;

    return {
        matchImageToHips: (hipsPv, imagePv) => {
            if (isHiPS(primePlot(imagePv))) return;
            if (lock) return;
            lock= true;
            imageToHips(hipsPv,imagePv);
            lock= false;
        },

        matchHiPStoPlotView: (visRoot, pv) => {
            if (lock) return;
            lock= true;
            const hipsViewerIds = getPlotViewAry(visRoot)
                .filter((testPv) => isHiPS(primePlot(testPv)))
                .map((h) => h.plotId);
            matchHiPSToImage(pv, hipsViewerIds);
            lock= false;
        }
    };
})();

function imageToHips(hipsPv, imagePv) {
    const imagePlot= primePlot(imagePv);
    const hipsPlot= primePlot(hipsPv);
    if (!imagePlot || !hipsPlot) return;
    const wp= getCenterOfProjection(hipsPlot);
    const imageCenter= CCUtil.getWorldCoords(imagePlot, findCurrentCenterPoint(imagePv));
    if (!pointEquals(imageCenter,wp)) {
        dispatchRecenter({plotId: imagePlot.plotId, centerPt:wp});
    }

    const targetASpix= getArcSecPerPix(hipsPlot,hipsPlot.zoomFactor);
    if (Math.abs(getArcSecPerPix(imagePlot,hipsPlot.zoomFactor)-targetASpix) >.001) {
        const level= getZoomLevelForScale(imagePlot, targetASpix);
        dispatchZoom({plotId:imagePlot.plotId, userZoomType:UserZoomTypes.LEVEL, level, actionScope:ActionScope.GROUP});
    }

    visRoot().plotViewAry.forEach( (iPv) => isImage(primePlot(iPv)) && rotateToMatch(iPv, hipsPv));
}


/**
 * Add add a image outline to some HiPS display and attempts to zoom to the same scale.
 * @param {PlotView} pv
 * @param {Array.<string>} hipsPVidAry
 */
function matchHiPSToImage(pv, hipsPVidAry) {
    if (!pv || isEmpty(hipsPVidAry)) return;
    const attributes=  getCornersAttribute(pv);
    const plot= primePlot(pv);
    const wpCenter= CCUtil.getWorldCoords(plot,findCurrentCenterPoint(pv));
    if (!wpCenter) return;
    const dl = getDrawLayerByType(dlRoot(), ImageOutline.TYPE_ID);
    if (!dl) dispatchCreateDrawLayer(ImageOutline.TYPE_ID);
    const connectPids= dl ? dl.plotIdAry : [];
    const asPerPix= getArcSecPerPix(plot,plot.zoomFactor);
    hipsPVidAry.forEach( (id) => {
        dispatchAttributeChange({ plotId:id, overlayColorScope:false, positionScope:false, changes:attributes});
        if (!connectPids.includes(id)) dispatchAttachLayerToPlot(ImageOutline.TYPE_ID, id);
        dispatchChangeCenterOfProjection({plotId:id, centerProjPt:wpCenter});
        //Since HiPs map only support JS2000 and Galactic coordinates, only the image is plotted with these two coordinates
        //the change is dispatched. If not, do nothing
        const jNorth= isPlotRotatedNorth(plot, CoordinateSys.EQ_J2000);
        const gNorth= isPlotRotatedNorth(plot, CoordinateSys.GALACTIC);
        if (jNorth || gNorth) {
            const hpv= getPlotViewById(visRoot(),id);
            if (!isRotationMatching(pv, hpv)) {
                if (jNorth)      dispatchChangeHiPS({plotId: id, coordSys: CoordinateSys.EQ_J2000});
                else if (gNorth) dispatchChangeHiPS({plotId: id, coordSys: CoordinateSys.GALACTIC});
            }
        }
        const hipsPv= getPlotViewById(visRoot(), id);
        const hipsPlot= primePlot(hipsPv);
        const level= getZoomLevelForScale(hipsPlot,asPerPix);
        if (Math.abs(getArcSecPerPix(hipsPlot, hipsPlot.zoomFactor)-asPerPix) >.001) {
            dispatchZoom({ plotId:id, userZoomType: UserZoomTypes.LEVEL, level});
        }
    });
}

function getCornersAttribute(pv) {
    const plot= primePlot(pv);
    const cAry= getCorners(plot);
    if (!cAry) return {};
    return {
        [PlotAttribute.OUTLINEIMAGE_BOUNDS]: cAry,
        [PlotAttribute.OUTLINEIMAGE_TITLE]: plot.title
    };
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

    if (!isFlipYMatching(pv, masterPv)) {
        dispatchFlip({plotId:pv.plotId, rematchAfterFlip:false, actionScope: ActionScope.SINGLE});
    }


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


    if (pv.flipY!==masterPv.flipY) dispatchFlip({plotId:pv.plotId, rematchAfterFlip:false, actionScope: ActionScope.SINGLE});
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
    if (!plot || !masterPlot) return;
    dispatchRotate({
        plotId: pv.plotId,
        rotateType: RotateType.ANGLE,
        angle: getMatchingRotationAngle(masterPv,pv),
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

