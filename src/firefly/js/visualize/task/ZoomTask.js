/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import {UserZoomTypes, getArcSecPerPix, getEstimatedFullZoomFactor,
    getNextZoomLevel, getZoomLevelForScale, FullType} from '../ZoomUtil.js';
import {logger} from '../../util/Logger.js';
import {isImage, isHiPS} from '../WebPlot.js';
import ImagePlotCntlr, {ActionScope, IMAGE_PLOT_KEY, WcsMatchType,
                       dispatchUpdateViewSize, dispatchRecenter} from '../ImagePlotCntlr.js';
import {getPlotViewById,primePlot,getPlotStateAry, operateOnOthersInPositionGroup,
    applyToOnePvOrAll} from '../PlotViewUtil.js';
import {callSetZoomLevel} from '../../rpc/PlotServicesJson.js';
import {isImageViewerSingleLayout, getMultiViewRoot} from '../MultiViewCntlr.js';
import {WebPlotResult} from '../WebPlotResult.js';
import {doHiPSImageConversionIfNecessary} from './PlotHipsTask.js';
import {matchImageToHips, matchHiPStoPlotView} from './WcsMatchTask';


const ZOOM_WAIT_MS= 1500; // 1.5 seconds
const BAD_PARAMS_MSG= 'zoom payload parameters wrong';

const isFitFill= (uzType) => uzType===UserZoomTypes.FIT || uzType===UserZoomTypes.FILL;

let zoomTimers= [];


/**
 * zoom Action creator,
 * @param rawAction
 * @return {Function}
 */
export function zoomActionCreator(rawAction) {
    return (dispatcher, getState) => {

        const {plotId,zoomLockingEnabled= false,forceDelay=false, level:payloadLevel}= rawAction.payload;
        let {userZoomType,actionScope= ActionScope.GROUP}= rawAction.payload;
        userZoomType= UserZoomTypes.get(userZoomType);
        actionScope= ActionScope.get(actionScope);
        let visRoot= getState()[IMAGE_PLOT_KEY];
        const pv= getPlotViewById(visRoot,plotId);
        const plot= primePlot(pv);
        if (!plot) return;


        const {level, isFullScreen, useDelay, validParams}=
                      evaluateZoomType(visRoot,pv,userZoomType,forceDelay,payloadLevel);

        if (!validParams) {
            dispatcher( {
                type: ImagePlotCntlr.ZOOM_IMAGE_FAIL, payload: {plotId, zoomLevel:level, error:BAD_PARAMS_MSG} } );
            return;
        }



        let zoomActive= true;
        if (isImage(plot) && Math.floor(plot.zoomFactor*1000)===Math.floor(level*1000)) { //zoom level the same - just return
            if (isFitFill(userZoomType)) dispatchRecenter({plotId, centerOnImage:true});
            zoomActive= false;
        }


        visRoot= getState()[IMAGE_PLOT_KEY];
        if (zoomActive) doZoom(dispatcher,plot,level,isFullScreen,zoomLockingEnabled,userZoomType,useDelay,getState);
        if (actionScope===ActionScope.GROUP) {
            const {wcsMatchType}= visRoot;
            const matchByScale= (wcsMatchType!==WcsMatchType.Pixel && wcsMatchType!==WcsMatchType.PixelCenter);
            const matchFunc= makeZoomLevelMatcher(dispatcher, visRoot,pv,level,matchByScale, isFullScreen,
                                                   zoomLockingEnabled,userZoomType,useDelay, getState);
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
 * @param {boolean} forceDelay
 * @param {number} payloadLevel
 * @return {{level: number, isFullScreen: boolean, useDelay: boolean, validParams: boolean}}
 */
function evaluateZoomType(visRoot, pv, userZoomType, forceDelay, payloadLevel= 1) {

    let level;
    let isFullScreen;
    let useDelay;
    let validParams= false;

    const plot= primePlot(pv);
    if (userZoomType===UserZoomTypes.LEVEL) { //payload.level is only used in this mode, otherwise it is computed
        level= payloadLevel;
        isFullScreen= false;
        useDelay= false;
        validParams= true;
    }
    else if ([UserZoomTypes.UP,UserZoomTypes.DOWN,UserZoomTypes.ONE].includes(userZoomType)) {
        level= getNextZoomLevel(plot,userZoomType);
        isFullScreen= false;
        useDelay= true;
        validParams= true;
    }
    else {
        const dim= pv.viewDim;
        isFullScreen= true;
        useDelay= forceDelay;


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

    return {level, isFullScreen, useDelay, validParams};
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


function makeZoomLevelMatcher(dispatcher, visRoot, sourcePv,level,matchByScale,isFullScreen,
                              zoomLockingEnabled,userZoomType,useDelay,getState) {
    const selectedPlot= primePlot(sourcePv);
    const targetArcSecPix= matchByScale && getArcSecPerPix(selectedPlot, level);

    return (pv) => {
        const  plot= primePlot(pv);
        if (!plot) return;
        let newZoomLevel= level;
        if (targetArcSecPix) {
            const  plotLevel= getZoomLevelForScale(plot, targetArcSecPix);

            // we want each plot to have the same arcsec / pixel as the target level
            // if the new level is only slightly different then use the target level
           newZoomLevel= (!plotLevel || (Math.abs(plotLevel-level)<.01)) ? level : plotLevel;
        }
        doZoom(dispatcher,plot,newZoomLevel,isFullScreen,zoomLockingEnabled,userZoomType,useDelay,getState);
    };
}


/**
 * Zoom the plot. Image zoom is done is done in two steps. HiPS zoom only requires one. Between step one and step 2
 * check to see it the plot should be converted between HiPS and image
 *
 * @param dispatcher
 * @param {WebPlot} plot
 * @param {number} zoomLevel
 * @param {boolean} isFullScreen
 * @param {boolean} zoomLockingEnabled
 * @param {UserZoomType} userZoomType
 * @param {boolean} useDelay
 * @param {Function} getState
 */
function doZoom(dispatcher,plot,zoomLevel,isFullScreen, zoomLockingEnabled, userZoomType,useDelay,getState) {

    const oldZoomLevel= plot.zoomFactor;

    const preZoomVisRoot= getState()[IMAGE_PLOT_KEY];

    if (isImage(plot) && Math.floor(oldZoomLevel*1000)===Math.floor(zoomLevel*1000)) return;


    //-----------------------------------------------------------------
    // Part 1: do initial zoom and check for plot type and conversion
    //-----------------------------------------------------------------
    const {plotId}= plot;
    dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_START,
                  payload:{plotId,zoomLevel, zoomLockingEnabled,userZoomType} } );


    const pv= getPlotViewById(preZoomVisRoot,plotId);
    const autoConvertOnZoom= get(pv.plotViewCtx, 'hipsImageConversion.autoConvertOnZoom',false);
    if (autoConvertOnZoom) {
        const converted= doHiPSImageConversionIfNecessary(pv, oldZoomLevel, zoomLevel);
        if (converted) return;
    }


    if (isHiPS(plot) ) {
        dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotId} } );
        return;
    }

    //---------------------------------------------------------------------------------------
    // Part 2: for image, setup timer delay,  make server call to regenerate the zoomed image
    //---------------------------------------------------------------------------------------

     // note - this filter has a side effect of canceling the timer. There might be a better way to do this.
    zoomTimers= zoomTimers.filter((t) => {
        if (t.plotId===plotId) {
            clearTimeout(t.timerId);
            return false;
        }
        return true;
    });

    const zoomWait= useDelay ? ZOOM_WAIT_MS : 5;

    const timerId= setTimeout(zoomPlotIdNow, zoomWait, dispatcher,preZoomVisRoot,plotId,zoomLevel,isFullScreen,getState);
    zoomTimers.push({plotId,timerId});
}



/**
 * call the server to do the zoom
 * @param dispatcher
 * @param preZoomVisRoot
 * @param plotId
 * @param zoomLevel
 * @param isFullScreen
 * @param {Function} getState
 */
function zoomPlotIdNow(dispatcher,preZoomVisRoot,plotId,zoomLevel,isFullScreen,getState) {
    zoomTimers= zoomTimers.filter((t) => t.plotId!==plotId);

    const pv= getPlotViewById(preZoomVisRoot,plotId);
    if (!primePlot(pv)) return;  // the plot was deleted, abort zoom
    callSetZoomLevel(getPlotStateAry(pv),zoomLevel,isFullScreen)
        .then( (wpResult) => processZoomSuccess(dispatcher,preZoomVisRoot,plotId,zoomLevel,wpResult,getState) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_FAIL, payload: {plotId, zoomLevel, error:e} } );
            logger.error(`plot error, Zoom, plotId: ${pv.plotId}`, e);
        });
}



/**
 * The server appears to have returned a successful zoom
 * @param dispatcher
 * @param preZoomVisRoot
 * @param plotId
 * @param zoomLevel
 * @param result
 * @param {Function} getState
 */
function processZoomSuccess(dispatcher, preZoomVisRoot, plotId, zoomLevel, result, getState) {
    let successSent= false;
    if (result.success) {
        const resultAry = result[WebPlotResult.RESULT_ARY];
        if (resultAry[0].success) {
            const overlayUpdateAry= [];

            const currentVisRoot= getState()[IMAGE_PLOT_KEY];
            const pv= getPlotViewById(currentVisRoot,plotId);

            const originalPlot= primePlot(preZoomVisRoot,plotId);
            const plot= primePlot(currentVisRoot,plotId);
            if (originalPlot.plotImageId!==get(plot,'plotImageId')) {
                return; //abort: plot has been replaced since this zoom was started
            }

            const existingOverlayPlotViews = pv.overlayPlotViews.filter((opv) => opv.plot);

            resultAry.forEach( (r,i) => {
                if (i===0) return;
                overlayUpdateAry[i-1]= {
                    imageOverlayId: existingOverlayPlotViews[i-1].imageOverlayId,
                    overlayStateJson: r.data[WebPlotResult.PLOT_STATE],
                    overlayTiles: r.data[WebPlotResult.PLOT_IMAGES]
                };
            });
            dispatcher( {
                type: ImagePlotCntlr.ZOOM_IMAGE,
                payload: {
                    plotId,
                    primaryStateJson : resultAry[0].data[WebPlotResult.PLOT_STATE],
                    primaryTiles : resultAry[0].data[WebPlotResult.PLOT_IMAGES],
                    overlayUpdateAry
                }});
            dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[plotId]}} );
            successSent= true;
        }
    }
    if (!successSent) {
        dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_FAIL,
                      payload: {plotId, zoomLevel, error:Error('payload failed')} } );
    }
}

