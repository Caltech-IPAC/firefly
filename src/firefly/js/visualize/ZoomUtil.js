/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Enum from 'enum';
import {get} from 'lodash';
import numeral from 'numeral';
import {logError} from '../util/WebUtil.js';
import {PlotAttribute} from './WebPlot.js';
import ImagePlotCntlr, {ActionScope, IMAGE_PLOT_KEY,
                       dispatchUpdateViewSize, dispatchRecenter} from './ImagePlotCntlr.js';
import {getPlotViewById,primePlot,getPlotStateAry,
        operateOnOthersInGroup, applyToOnePvOrGroup, findPlotGroup} from './PlotViewUtil.js';
import {callSetZoomLevel} from '../rpc/PlotServicesJson.js';
import {isImageViewerSingleLayout, getMultiViewRoot} from './MultiViewCntlr.js';
import WebPlotResult from './WebPlotResult.js';
import VisUtil from './VisUtil.js';


export const levels= [ .03125, .0625, .125,.25,.5, .75, 1,2,3, 4,5, 6,
                7,8, 9, 10, 11, 12, 13, 14, 15, 16, 32];

const zoomMax= levels[levels.length-1];

/**
 * can be 'UP','DOWN', 'FIT', 'FILL', 'ONE', 'LEVEL', 'WCS_MATCH_PREV'
 * @public
 * @global
 */
export const UserZoomTypes= new Enum(['UP','DOWN', 'FIT', 'FILL', 'ONE', 'LEVEL', 'WCS_MATCH_PREV'], { ignoreCase: true });

const ZOOM_WAIT_MS= 1500; // 1.5 seconds

let zoomTimers= []; // todo: should I use a map? should it be in the redux store?

//======================================== Exported Functions =============================
//======================================== Exported Functions =============================



/**
 * zoom Action creator,
 * @param rawAction
 * @return {Function}
 */
export function zoomActionCreator(rawAction) {
    return (dispatcher, getState) => {
        const {plotId,zoomLockingEnabled= false,forceDelay=false}= rawAction.payload;
        let {userZoomType,actionScope= ActionScope.GROUP, level= 1}= rawAction.payload;
        userZoomType= UserZoomTypes.get(userZoomType);
        actionScope= ActionScope.get(actionScope);
        let visRoot= getState()[IMAGE_PLOT_KEY];
        const pv= getPlotViewById(visRoot,plotId);
        if (!pv) return;


        let isFullScreen;
        let useDelay;
        let goodParams= false;
        const plot= primePlot(pv);
        if (userZoomType===UserZoomTypes.LEVEL) { //payload.level is only used in this mode, otherwise it is computed
            isFullScreen= false;
            useDelay= false;
            goodParams= true;
        }
        else if ([UserZoomTypes.UP,UserZoomTypes.DOWN,UserZoomTypes.ONE].includes(userZoomType)) {
            level= getNextZoomLevel(plot.zoomFactor,userZoomType);
            isFullScreen= false;
            useDelay= true; //todo
            goodParams= true;
        }
        else {
            const dim= pv.viewDim;
            isFullScreen= true;
            useDelay= forceDelay;


            if (dim.width && dim.height) {
                if (userZoomType===UserZoomTypes.FIT) {
                    level = getEstimatedFullZoomFactor(plot, dim, VisUtil.FullType.WIDTH_HEIGHT);
                }
                else if (userZoomType===UserZoomTypes.FILL) {
                    level = getEstimatedFullZoomFactor(plot, dim, VisUtil.FullType.ONLY_WIDTH);
                }
                else if (userZoomType===UserZoomTypes.WCS_MATCH_PREV) {
                    if (visRoot.prevActivePlotId) {
                        const masterPlot= primePlot(visRoot,visRoot.prevActivePlotId);
                        const asPerPix= getArcSecPerPix(masterPlot,masterPlot.zoomFactor);
                        level= getZoomLevelForScale(plot, asPerPix);
                    }
                    else { // just to a fit
                        level = getEstimatedFullZoomFactor(plot, dim, VisUtil.FullType.WIDTH_HEIGHT);
                    }
                }
                goodParams= true;
            }

        }


        let zoomActive= true;
        if (Math.floor(plot.zoomFactor*1000)===Math.floor(level*1000)) { //zoom level the same - just return
            if (userZoomType===UserZoomTypes.FIT || userZoomType===UserZoomTypes.FILL) {
                dispatchRecenter({plotId, centerOnImage:true});
            }
            zoomActive= false;
        }


        if (goodParams) {
            visRoot= getState()[IMAGE_PLOT_KEY];
            if (zoomActive) doZoom(dispatcher,visRoot, plot,level,isFullScreen,zoomLockingEnabled,userZoomType,useDelay,getState);
            const matchFunc= makeZoomLevelMatcher(dispatcher, visRoot,pv,level,isFullScreen,
                                                   zoomLockingEnabled,userZoomType,useDelay, getState);
            if (actionScope===ActionScope.GROUP) {
                operateOnOthersInGroup(getState()[IMAGE_PLOT_KEY],pv, matchFunc);
            }
            visRoot= getState()[IMAGE_PLOT_KEY];
            alignWCS(visRoot,pv);
        }
        else {
            dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_FAIL,
                          payload: {plotId, zoomLevel:level, error:'zoom parameters wrong'} } );
        }
    };

}


function alignWCS(visRoot, pv) {
    if (visRoot.wcsMatchType) {
        if (isImageViewerSingleLayout(getMultiViewRoot(), visRoot, pv.plotId)) {
            dispatchUpdateViewSize(pv.plotId);
        }
        else {
            const pg= findPlotGroup(pv.plotGroupId, visRoot.plotGroupAry);
            applyToOnePvOrGroup(visRoot.plotViewAry, pv.plotId, pg,
                                               (pv) => dispatchUpdateViewSize(pv.plotId) );
        }

    }

}


function makeZoomLevelMatcher(dispatcher, visRoot, sourcePv,level,isFullScreen,zoomLockingEnabled,userZoomType,useDelay,getState) {
    const selectedPlot= primePlot(sourcePv);
    const targetArcSecPix= getArcSecPerPix(selectedPlot, level);

    return (pv) => {
        const  plot= primePlot(pv);
        if (!plot) return;
        let newZoomLevel= level;
        if (targetArcSecPix) {
            const  plotLevel= getZoomLevelForScale(plot, targetArcSecPix);

            // we want each plot to have the same arcsec / pixel as the target level
            // if the new level is only slightly different then use the target level
           newZoomLevel= (Math.abs(plotLevel-level)<.01) ? level : plotLevel;
        }
        doZoom(dispatcher,visRoot,plot,newZoomLevel,isFullScreen,zoomLockingEnabled,userZoomType,useDelay,getState);
    };
}


/**
 * Begin zooming
 *
 * @param dispatcher
 * @param {VisRoot} visRoot
 * @param {WebPlot} plot
 * @param {number} zoomLevel
 * @param {boolean} isFullScreen
 * @param {boolean} zoomLockingEnabled
 * @param {UserZoomType} userZoomType
 * @param {boolean} useDelay
 * @param {Function} getState
 */
function doZoom(dispatcher,visRoot,plot,zoomLevel,isFullScreen, zoomLockingEnabled, userZoomType,useDelay,getState) {


   if (Math.floor(plot.zoomFactor*1000)===Math.floor(zoomLevel*1000)) return;


    const {plotId}= plot;
    dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_START,
                  payload:{plotId,zoomLevel, zoomLockingEnabled,userZoomType} } );


     // note - this filter has a side effect of canceling the timer. There might be a better way to do this.
    zoomTimers= zoomTimers.filter((t) => {
        if (t.plotId===plotId) {
            clearTimeout(t.timerId);
            return false;
        }
        return true;
    });

    const zoomWait= useDelay ? ZOOM_WAIT_MS : 5;

    const timerId= setTimeout(zoomPlotIdNow, zoomWait, dispatcher,visRoot,plotId,zoomLevel,isFullScreen,getState);
    zoomTimers.push({plotId,timerId});
}


/**
 * call the server to do the zoom
 * @param dispatcher
 * @param visRoot
 * @param plotId
 * @param zoomLevel
 * @param isFullScreen
 * @param {Function} getState
 */
function zoomPlotIdNow(dispatcher,visRoot,plotId,zoomLevel,isFullScreen,getState) {
    zoomTimers= zoomTimers.filter((t) => t.plotId!==plotId);

    const pv= getPlotViewById(visRoot,plotId);
    if (!primePlot(pv)) return;  // the plot was deleted, abort zoom
    callSetZoomLevel(getPlotStateAry(pv),zoomLevel,isFullScreen)
        .then( (wpResult) => processZoomSuccess(dispatcher,visRoot,plotId,zoomLevel,wpResult,getState) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_FAIL, payload: {plotId, zoomLevel, error:e} } );
            logError(`plot error, plotId: ${pv.plotId}`, e);
        });
}



/**
 * The server appears to have returned a successful zoom
 * @param dispatcher
 * @param originalVisRoot
 * @param plotId
 * @param zoomLevel
 * @param result
 * @param {Function} getState
 */
function processZoomSuccess(dispatcher, originalVisRoot, plotId, zoomLevel, result, getState) {
    let successSent= false;
    if (result.success) {
        const resultAry = result[WebPlotResult.RESULT_ARY];
        if (resultAry[0].success) {
            const overlayUpdateAry= [];

            const currentVisRoot= getState()[IMAGE_PLOT_KEY];
            const pv= getPlotViewById(currentVisRoot,plotId);

            const originalPlot= primePlot(originalVisRoot,plotId);
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




export function getNextZoomLevel(currLevel, zoomType) {
    let newLevel= 1;
    if (zoomType===UserZoomTypes.UP) {
        newLevel= currLevel>=zoomMax ? zoomMax : levels.find( (l) => l>currLevel);
    }
    else if (zoomType===UserZoomTypes.ONE) {
        newLevel= 1;
    }
    else if (zoomType===UserZoomTypes.DOWN) {
        newLevel= levels[0];
        let found= false;
        for(let i= levels.length-1; (i>=0); i--) {
            found= (levels[i]<currLevel);
            if (found) {
                newLevel= levels[i];
                break;
            }
        }
    }
    else {
        logError('unsupported zoomType');
    }
    return newLevel;
}


/**
 *
 * @param plot
 * @param screenDim
 * @param fullType
 * @param tryMinFactor
 */
export function getEstimatedFullZoomFactor(plot, screenDim, fullType, tryMinFactor=-1) {
    const {width,height} = screenDim;
    let overrideFullType= fullType;

    if (plot.attributes[PlotAttribute.EXPANDED_TO_FIT_TYPE]) {
        const s= plot.attributes[PlotAttribute.EXPANDED_TO_FIT_TYPE];
        if (VisUtil.FullType.has(s)) overrideFullType= VisUtil.FullType.get(s);
    }
    return VisUtil.getEstimatedFullZoomFactor(overrideFullType, plot.dataWidth, plot.dataHeight,
                                              width,height, tryMinFactor);
}


export function getZoomMax() { return levels[levels.length-1]; }



function getOnePlusLevelDesc(level) {
    let retval;
    const remainder= level % 1;
    if (remainder < .1 || remainder>.9) {
        retval= Math.round(level)+'x';
    }
    else {
        retval= numeral(level).format('0.000')+'x';
    }
    return retval;
}

export function getArcSecPerPix(plot, zoomFact) {
    return plot.projection.getPixelScaleArcSec() / zoomFact;
}

export function getZoomLevelForScale(plot, arcsecPerPix) {
    return plot.projection.getPixelScaleArcSec() / arcsecPerPix;
}

export function convertZoomToString(level) {
    let retval;
    const zfInt= Math.floor(level*10000);

    if (zfInt>=10000)      retval= getOnePlusLevelDesc(level); // if level > then 1.0
    else if (zfInt===312)  retval= '1/32x';     // 1/32
    else if (zfInt===625)  retval= '1/16x';     // 1/16
    else if (zfInt===1250) retval= '1/8x';      // 1/8
    else if (zfInt===2500) retval= String.fromCharCode(188) +'x'; // 1/4
    else if (zfInt===7500) retval= String.fromCharCode(190) +'x';   // 3/4
    else if (zfInt===5000) retval= String.fromCharCode(189) +'x';   // 1/2
    else                   retval= numeral(level).format('0.0')+'x';
    return retval;
}
