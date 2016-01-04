/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Enum from 'enum';
import numeral from 'numeral';
import {flux} from '../Firefly.js';
import {logError} from '../util/WebUtil.js';
import {PlotAttribute} from './WebPlot.js';
import ImagePlotCntlr, {visRoot} from './ImagePlotCntlr.js';
import PlotViewUtil from './PlotViewUtil.js';
import PlotGroup from './PlotGroup.js';
import PlotServicesJson from '../rpc/PlotServicesJson.js';
import WebPlotResult from './WebPlotResult.js';
import VisUtil from './VisUtil.js';


const levels= [ .03125, .0625, .125,.25,.5, .75, 1,2,3, 4,5, 6,
                7,8, 9, 10, 11, 12, 13, 14, 15, 16, 32];


const zoomMax= levels[levels.length-1];
//const zoomMin= levels[0];

const UserZoomTypes= new Enum(['UP','DOWN', 'FIT', 'FILL', 'ONE']);
const ZoomScope= new Enum(['GROUP','SINGLE', 'LIST']);
const ZOOM_WAIT_MS= 2000; // 2 seconds

export default {dispatchZoom, makeZoomAction, UserZoomTypes};

var zoomTimers= []; // todo: should I use a map? should it be in the redux store?

//======================================== Exported Functions =============================
//======================================== Exported Functions =============================


/**
 *
 * @param {string} plotId
 * @param {UserZoomTypes} userZoomType
 * @param {ZoomScope} zoomScope
 */
function dispatchZoom(plotId, userZoomType, zoomScope=ZoomScope.GROUP ) {

    flux.process({
        type: ImagePlotCntlr.ZOOM_IMAGE,
        payload :{
            plotId, userZoomType, zoomScope
        }});
}


/**
 * zoom Action creator, todo: zoomScope, fit, fill, and much, much more
 * @param rawAction
 * @return {Function}
 */
function makeZoomAction(rawAction) {
    return (dispatcher) => {
        var {plotId,userZoomType}= rawAction.payload;
        var pv= PlotViewUtil.getPlotViewById(visRoot(),plotId);
        if (!pv) return;



        var level;
        var isFullScreen;
        var useDelay;
        var continueZoom= false;
        if ([UserZoomTypes.UP,UserZoomTypes.DOWN,UserZoomTypes.ONE].includes(userZoomType)) {
            level= getNextZoomLevel(pv.primaryPlot.zoomFactor,userZoomType);
            isFullScreen= false;
            useDelay= true; //todo
            continueZoom= true;
        }
        else {
            var dim= pv.viewDim;
            isFullScreen= true;
            useDelay= true; //todo

            if (dim.width && dim.height) {
                if (userZoomType===UserZoomTypes.FIT) {
                    level = getEstimatedFullZoomFactor(pv, dim, VisUtil.FullType.WIDTH_HEIGHT);
                }
                else if (userZoomType===UserZoomTypes.FILL) {
                    level = getEstimatedFullZoomFactor(pv, dim, VisUtil.FullType.ONLY_WIDTH);
                }
                continueZoom= true;
            }

        }
        if (continueZoom) {
            doZoom(dispatcher,plotId,level,isFullScreen,useDelay);
            var matchFunc= makeZoomLevelMatcher(dispatcher, pv,level,isFullScreen,useDelay);
            PlotViewUtil.operateOnOthersInGroup(visRoot(),pv, matchFunc);
        }
        else {
            dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_FAIL, payload: {plotId, zoomLevel:level, error:'zoom parameters wrong'} } );
        }
    };

}




function makeZoomLevelMatcher(dispatcher, sourcePv,level,isFullScreen,useDelay) {
    const selectedPlot= sourcePv.primaryPlot;
    const targetArcSecPix= getArcSecPerPix(selectedPlot, level);

    return (pv) => {
        var  plot= pv.primaryPlot;
        var newZoomLevel= level;
        if (targetArcSecPix) {
            var  plotLevel= getZoomLevelForScale(plot, targetArcSecPix);

            // we want each plot to have the same arcsec / pixel as the target level
            // if the new level is only slightly different then use the target level
           newZoomLevel= (Math.abs(plotLevel-level)<.01) ? level : plotLevel;
        }
        doZoom(dispatcher,pv.plotId,newZoomLevel,isFullScreen,useDelay);
    };
}


/**
 *
 * @param dispatcher
 * @param plotId
 * @param zoomLevel
 * @param isFullScreen
 * @param useDelay
 */
function doZoom(dispatcher,plotId,zoomLevel,isFullScreen, useDelay) {
    dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_START, payload:{plotId,zoomLevel} } );


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
        var timerId= setTimeout(zoomPlotIdNow, zoomWait, dispatcher,plotId,zoomLevel,isFullScreen);
        zoomTimers.push({plotId,timerId});
    }
    else {
        zoomPlotIdNow(dispatcher,plotId,zoomLevel,isFullScreen);
    }
}



function zoomPlotIdNow(dispatcher,plotId,zoomLevel,isFullScreen) {
    zoomTimers= zoomTimers.filter((t) => t.plotId!==plotId);

    var pv= PlotViewUtil.getPlotViewById(visRoot(),plotId);
    PlotServicesJson.setZoomLevel(PlotViewUtil.getPlotStateAry(pv),zoomLevel,isFullScreen)
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
 * @param rawPayload
 * @param {object} result
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




function getNextZoomLevel(currLevel, zoomType) {
    var newLevel= 1;
    if (zoomType===UserZoomTypes.UP) {
        newLevel= currLevel>=zoomMax ? zoomMax : levels.find( (l) => l>currLevel);
    }
    else if (zoomType===UserZoomTypes.ONE) {
        newLevel= 1;
    }
    else if (zoomType==UserZoomTypes.DOWN) {
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
        console.log('unsupported zoomType');
    }
    return newLevel;
}


/**
 *
 * @param pv
 * @param screenDim
 * @param fullType
 * @param tryMinFactor
 */
function getEstimatedFullZoomFactor(pv, screenDim, fullType, tryMinFactor=-1) {
    var {width,height} = screenDim;
    var overrideFullType= fullType;
    var plot= pv.primaryPlot;

    if (pv.attributes[PlotAttribute.EXPANDED_TO_FIT_TYPE]) {
        var s= pv.attributes[PlotAttribute.EXPANDED_TO_FIT_TYPE];
        if (VisUtil.FullType.has(s)) overrideFullType= VisUtil.FullType.get(s);
    }
    return VisUtil.getEstimatedFullZoomFactor(overrideFullType, plot.dataWidth, plot.dataHeight,
                                              width,height, tryMinFactor);
}





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

function getArcSecPerPix(plot, zoomFact) {
    return plot.projection.getPixelScaleArcSec() / zoomFact;
}

function getZoomLevelForScale(plot, arcsecPerPix) {
    return plot.projection.getImagePixelScaleInArcSec() / arcsecPerPix;
}

function convertZoomToString(level) {
    var retval;
    var zfInt= Math.floor(level*10000);

    if (zfInt>=10000)      retval= getOnePlusLevelDesc(level); // if level > then 1.0
    else if (zfInt===312)  retval= '1/32x';     // 1/32
    else if (zfInt===625)  retval= '1/16x';     // 1/16
    else if (zfInt===1250) retval= '1/8x';      // 1/8
    else if (zfInt===2500) retval= '&#188;x';   // 1/4
    else if (zfInt===7500) retval= '&#190;x';   // 3/4
    else if (zfInt===5000) retval= '&#189;x';   // 1/2
    else                   retval= numeral(level).format('0.0')+'x';
    return retval;
}
