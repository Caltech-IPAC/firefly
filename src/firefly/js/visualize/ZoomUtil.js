/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Enum from 'enum';
import numeral from 'numeral';
import {logError} from '../util/WebUtil.js';
import {PlotAttribute} from './WebPlot.js';
import ImagePlotCntlr, {ActionScope, IMAGE_PLOT_KEY, dispatchUpdateViewSize} from './ImagePlotCntlr.js';
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

const ZOOM_WAIT_MS= 2000; // 2 seconds

var zoomTimers= []; // todo: should I use a map? should it be in the redux store?

//======================================== Exported Functions =============================
//======================================== Exported Functions =============================



/**
 * zoom Action creator,
 * @param rawAction
 * @return {Function}
 */
export function zoomActionCreator(rawAction) {
    return (dispatcher, getState) => {
        var {plotId,userZoomType,zoomLockingEnabled= false,
             forceDelay=false, level= 1, actionScope= ActionScope.GROUP}= rawAction.payload;
        userZoomType= UserZoomTypes.get(userZoomType);
        actionScope= ActionScope.get(actionScope);
        var visRoot= getState()[IMAGE_PLOT_KEY];
        var pv= getPlotViewById(visRoot,plotId);
        if (!pv) return;


        var isFullScreen;
        var useDelay;
        var goodParams= false;
        var plot= primePlot(pv);
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
            var dim= pv.viewDim;
            isFullScreen= true;
            useDelay= forceDelay ? true : false; //todo


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


        if (Math.floor(plot.zoomFactor*1000)===Math.floor(level*1000)) { //zoom level the same - just return
            return;
        }


        if (goodParams) {
            visRoot= getState()[IMAGE_PLOT_KEY];
            doZoom(dispatcher,visRoot, plotId,level,isFullScreen,zoomLockingEnabled,userZoomType,useDelay);
            var matchFunc= makeZoomLevelMatcher(dispatcher, visRoot,pv,level,isFullScreen,zoomLockingEnabled,userZoomType,useDelay);
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


function makeZoomLevelMatcher(dispatcher, visRoot, sourcePv,level,isFullScreen,zoomLockingEnabled,userZoomType,useDelay) {
    const selectedPlot= primePlot(sourcePv);
    const targetArcSecPix= getArcSecPerPix(selectedPlot, level);

    return (pv) => {
        var  plot= primePlot(pv);
        if (!plot) return;
        var newZoomLevel= level;
        if (targetArcSecPix) {
            var  plotLevel= getZoomLevelForScale(plot, targetArcSecPix);

            // we want each plot to have the same arcsec / pixel as the target level
            // if the new level is only slightly different then use the target level
           newZoomLevel= (Math.abs(plotLevel-level)<.01) ? level : plotLevel;
        }
        doZoom(dispatcher,visRoot,pv.plotId,newZoomLevel,isFullScreen,zoomLockingEnabled,userZoomType,useDelay);
    };
}


/**
 *
 * @param dispatcher
 * @param visRoot
 * @param plotId
 * @param zoomLevel
 * @param zoomLockingEnabled
 * @param userZoomType
 * @param isFullScreen
 * @param useDelay
 */
function doZoom(dispatcher,visRoot,plotId,zoomLevel,isFullScreen, zoomLockingEnabled, userZoomType,useDelay) {
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

    var zoomWait= useDelay ? ZOOM_WAIT_MS : 5;

    if (true) {
        var timerId= setTimeout(zoomPlotIdNow, zoomWait, dispatcher,visRoot,plotId,zoomLevel,isFullScreen);
        zoomTimers.push({plotId,timerId});
    }
    else {
        zoomPlotIdNow(dispatcher,visRoot,plotId,zoomLevel,isFullScreen);
    }
}



function zoomPlotIdNow(dispatcher,visRoot,plotId,zoomLevel,isFullScreen) {
    zoomTimers= zoomTimers.filter((t) => t.plotId!==plotId);

    var pv= getPlotViewById(visRoot,plotId);
    if (!primePlot(pv)) return;  // the plot what changed, abort zoom
    callSetZoomLevel(getPlotStateAry(pv),zoomLevel,isFullScreen)
        .then( (wpResult) => processZoomSuccess(dispatcher,plotId,zoomLevel,wpResult) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_FAIL, payload: {plotId, zoomLevel, error:e} } );
            logError(`plot error, plotId: ${pv.plotId}`, e);
        });
}



/**
 *
 * @param dispatcher
 * @param plotId
 * @param zoomLevel
 * @param result
 */
function processZoomSuccess(dispatcher, plotId, zoomLevel, result) {
    var successSent= false;
    if (result.success) {
        var resultAry = result[WebPlotResult.RESULT_ARY];
        if (resultAry[0].success) {
            var overlayStateJsonAry = [];
            var overlayTilesAry= [];
            resultAry.forEach( (r,i) => {
                if (i===0) return;
                overlayStateJsonAry[i - 1] = r.data[WebPlotResult.PLOT_STATE];
                overlayTilesAry[i - 1] = r.data[WebPlotResult.PLOT_IMAGES];
            });
            dispatcher( {
                type: ImagePlotCntlr.ZOOM_IMAGE,
                payload: {
                    plotId,
                    primaryStateJson : resultAry[0].data[WebPlotResult.PLOT_STATE],
                    primaryTiles : resultAry[0].data[WebPlotResult.PLOT_IMAGES],
                    overlayStateJsonAry, overlayTilesAry
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
    var newLevel= 1;
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
    var {width,height} = screenDim;
    var overrideFullType= fullType;

    if (plot.attributes[PlotAttribute.EXPANDED_TO_FIT_TYPE]) {
        var s= plot.attributes[PlotAttribute.EXPANDED_TO_FIT_TYPE];
        if (VisUtil.FullType.has(s)) overrideFullType= VisUtil.FullType.get(s);
    }
    return VisUtil.getEstimatedFullZoomFactor(overrideFullType, plot.dataWidth, plot.dataHeight,
                                              width,height, tryMinFactor);
}


export function getZoomMax() { return levels[levels.length-1]; }



function getOnePlusLevelDesc(level) {
    var retval;
    var remainder= level % 1;
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
    var retval;
    var zfInt= Math.floor(level*10000);

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
