/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {updateStretchDataAfterZoom} from '../rawData/RawDataOps.js';
import {UserZoomTypes, getArcSecPerPix, getEstimatedFullZoomFactor,
    getNextZoomLevel, getZoomLevelForScale, FullType} from '../ZoomUtil.js';
import {isImage, isHiPS} from '../WebPlot.js';
import ImagePlotCntlr, { ActionScope, IMAGE_PLOT_KEY, WcsMatchType,
    dispatchUpdateViewSize, dispatchRecenter, dispatchChangeCenterOfProjection } from '../ImagePlotCntlr.js';
import { getPlotViewById, primePlot, operateOnOthersInPositionGroup, applyToOnePvOrAll} from '../PlotViewUtil.js';
import {isImageViewerSingleLayout, getMultiViewRoot} from '../MultiViewCntlr.js';
import {doHiPSImageConversionIfNecessary} from './PlotHipsTask.js';
import {matchImageToHips, matchHiPStoPlotView} from './WcsMatchTask';
import {findHipsCenProjToPlaceWptOnDevPtByInteration,} from '../reducer/PlotView.js';
import {CCUtil} from '../CsysConverter.js';
import {logger} from '../../util/Logger.js';

const isFitFill= (uzType) => uzType===UserZoomTypes.FIT || uzType===UserZoomTypes.FILL;

/**
 * zoom Action creator,
 * @param rawAction
 * @return {Function}
 */
export function zoomActionCreator(rawAction) {
    return (dispatcher, getState) => {

        const {plotId,zoomLockingEnabled= false,level:payloadLevel, devicePt, upDownPercent}= rawAction.payload;
        let {userZoomType,actionScope= ActionScope.GROUP}= rawAction.payload;
        userZoomType= UserZoomTypes.get(userZoomType);
        actionScope= ActionScope.get(actionScope);
        let visRoot= getState()[IMAGE_PLOT_KEY];
        const pv= getPlotViewById(visRoot,plotId);
        const plot= primePlot(pv);
        if (!plot) return;

        const {level, validParams}= evaluateZoomType(visRoot,pv,userZoomType,payloadLevel,upDownPercent);

        if (!validParams) {
            logger.error('zoom failed: zoom payload parameters wrong', rawAction.payload);
            return;
        }

        if (isImage(plot) && Math.floor(plot.zoomFactor*1000)===Math.floor(level*1000)) {
            if (isFitFill(userZoomType)) dispatchRecenter({plotId, centerOnImage:true});
        }

        visRoot= getState()[IMAGE_PLOT_KEY];
        doZoom(dispatcher,plot,level,zoomLockingEnabled,userZoomType,devicePt,getState);
        if (actionScope===ActionScope.GROUP) {
            const {wcsMatchType}= visRoot;
            const matchByScale= (wcsMatchType!==WcsMatchType.Pixel && wcsMatchType!==WcsMatchType.PixelCenter);
            const devPt= isHiPS(plot) ? undefined : devicePt;
            const matchFunc= makeZoomLevelMatcher(dispatcher, visRoot,pv,level,matchByScale,
                                                   zoomLockingEnabled,userZoomType,devPt, getState);
            operateOnOthersInPositionGroup(getState()[IMAGE_PLOT_KEY],pv, matchFunc);
        }
        alignWCS(getState,pv);
    };

}


/**
 * look at the userZoomType parameter a return the right zoom level plus some information about how to zoom
 * @param {VisRoot} visRoot
 * @param {PlotView} pv
 * @param {UserZoomTypes} userZoomType
 * @param {number} payloadLevel
 * @param {number} [upDownPercent] value between 0 and 1 - 1 is 100% of the next step up or down
 * @return {{level: number, validParams: boolean}}
 */
function evaluateZoomType(visRoot, pv, userZoomType, payloadLevel= 1, upDownPercent=1) {

    let level;
    let validParams= false;

    const plot= primePlot(pv);
    if (userZoomType===UserZoomTypes.LEVEL) { //payload.level is only used in this mode, otherwise it is computed
        level= payloadLevel;
        validParams= true;
    }
    else if ([UserZoomTypes.UP,UserZoomTypes.DOWN,UserZoomTypes.ONE].includes(userZoomType)) {
        level= getNextZoomLevel(plot,userZoomType, upDownPercent);
        validParams= true;
    }
    else {
        const dim= pv.viewDim;
        if (dim.width && dim.height) {
            if (userZoomType===UserZoomTypes.FIT) {
                level = getEstimatedFullZoomFactor(plot, dim, FullType.WIDTH_HEIGHT);
            }
            else if (userZoomType===UserZoomTypes.FILL) {
                level = getEstimatedFullZoomFactor(plot, dim, FullType.ONLY_WIDTH);
            }
            else if (userZoomType===UserZoomTypes.WCS_MATCH_PREV) {
                if (visRoot.prevActivePlotId) {
                    const {wcsMatchType}= visRoot;
                    const masterPlot= primePlot(visRoot,visRoot.prevActivePlotId);
                    const asPerPix= getArcSecPerPix(masterPlot,masterPlot.zoomFactor);
                    level= (wcsMatchType!==WcsMatchType.Pixel && wcsMatchType!==WcsMatchType.PixelCenter) ?
                                 getZoomLevelForScale(plot, asPerPix) : masterPlot.zoomFactor;
                }
                else { // just to a fit
                    level = getEstimatedFullZoomFactor(plot, dim, FullType.WIDTH_HEIGHT);
                }
            }
            validParams= true;
        }
    }
    return {level, validParams};
}


function alignWCS(getState, pv) {
    let visRoot= getState()[IMAGE_PLOT_KEY];
    if (!visRoot.wcsMatchType) return;
    if (isImageViewerSingleLayout(getMultiViewRoot(), visRoot, pv.plotId)) {
        dispatchUpdateViewSize(pv.plotId);
    }
    else {
        applyToOnePvOrAll(true, visRoot.plotViewAry, pv.plotId, false, (pv) => dispatchUpdateViewSize(pv.plotId) );
    }
    visRoot= getState()[IMAGE_PLOT_KEY]; // need a new one after actions
    pv= getPlotViewById(visRoot, pv.plotId);
    if (visRoot.wcsMatchType===WcsMatchType.Target || visRoot.wcsMatchType===WcsMatchType.Standard) {
        if (isImage(primePlot(pv))) {
            matchHiPStoPlotView(visRoot,pv);
        }
        else if (isHiPS(primePlot(pv))) {
           matchImageToHips(pv, getPlotViewById(visRoot, visRoot.mpwWcsPrimId));
        }
    }
}


function makeZoomLevelMatcher(dispatcher, visRoot, sourcePv,level,matchByScale,
                              zoomLockingEnabled,userZoomType,devicePt,getState) {
    const selectedPlot= primePlot(sourcePv);
    const targetArcSecPix= matchByScale && getArcSecPerPix(selectedPlot, level);

    return (pv) => {
        const  plot= primePlot(pv);
        if (!plot) return;
        let newZoomLevel= level;
        if (targetArcSecPix) {
            const  plotLevel= getZoomLevelForScale(plot, targetArcSecPix);

            // we want each plot to have the same arcsec / pixel as the target level
            // if the new level is only slightly different, then use the target level
           newZoomLevel= (!plotLevel || (Math.abs(plotLevel-level)<.01)) ? level : plotLevel;
        }
        doZoom(dispatcher,plot,newZoomLevel,zoomLockingEnabled,userZoomType,devicePt,getState);
    };
}


/**
 * Zoom the image or hips.
 * First check to see it the plot should be converted between HiPS and image
 *
 * @param dispatcher
 * @param {WebPlot} plot
 * @param {number} zoomLevel
 * @param {boolean} zoomLockingEnabled
 * @param {UserZoomTypes} userZoomType
 * @param {DevicePt} devicePt
 * @param {Function} getState
 */
function doZoom(dispatcher,plot,zoomLevel, zoomLockingEnabled, userZoomType,devicePt,getState) {
    const visRoot = getState()[IMAGE_PLOT_KEY];
    const pv = getPlotViewById(visRoot, plot.plotId);
    const autoConvertOnZoom = pv?.plotViewCtx.hipsImageConversion?.autoConvertOnZoom ?? false;
    if (autoConvertOnZoom) {
        const oldZoomLevel = plot.zoomFactor;
        const converted = doHiPSImageConversionIfNecessary(pv, oldZoomLevel, zoomLevel);
        if (converted) return;
    }

    if (isHiPS(plot)) processHiPSZoom(dispatcher, plot, zoomLevel, userZoomType, zoomLockingEnabled, devicePt, getState);
    else processFitsImageZoom(dispatcher, plot, zoomLevel, userZoomType, zoomLockingEnabled, devicePt);
}

function processHiPSZoom(dispatcher, plot, zoomLevel, userZoomType, zoomLockingEnabled, devicePt, getState) {
    dispatcher({
        type: ImagePlotCntlr.ZOOM_HIPS,
        payload: {plotId:plot.plotId, zoomLevel, zoomLockingEnabled, userZoomType, devicePt}
    });
    const {plotId}= plot;
    if (devicePt) {
        const postZoomVisRoot = getState()[IMAGE_PLOT_KEY];
        const postPv = getPlotViewById(postZoomVisRoot, plotId);
        const wptBeforeZoom = CCUtil.getWorldCoords(plot, devicePt);
        if (wptBeforeZoom) {
            const centerProjPt = findHipsCenProjToPlaceWptOnDevPtByInteration(postPv, wptBeforeZoom, devicePt);
            centerProjPt && dispatchChangeCenterOfProjection({plotId, centerProjPt});
        }
    }
    dispatcher({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotId}});
}

function processFitsImageZoom(dispatcher, plot, zoomLevel, userZoomType, zoomLockingEnabled, devicePt) {
    const {plotId}= plot;
    dispatcher( {
        type: ImagePlotCntlr.ZOOM_IMAGE,
        payload: { zoomLevel, zoomLockingEnabled,userZoomType, devicePt, plotId, primaryStateJson:undefined}});
    dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[plotId]}} );
    void updateStretchDataAfterZoom(plotId, dispatcher);
}
