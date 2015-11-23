/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Enum from 'enum';
import {flux} from '../Firefly.js';
import {logError} from '../util/WebUtil.js';
import ImagePlotCntlr from './ImagePlotCntlr.js';
import PlotViewUtil from './PlotViewUtil.js';
import PlotServicesJson from '../rpc/PlotServicesJson.js';
import WebPlotResult from './WebPlotResult.js';


const levels= [ .03125, .0625, .125,.25,.5, .75, 1,2,3, 4,5, 6,
                7,8, 9, 10, 11, 12, 13, 14, 15, 16, 32];


const zoomMax= levels[levels.length-1];
//const zoomMin= levels[0];

const UserZoomTypes= new Enum(['UP','DOWN', 'FIT', 'FILL', 'ONE']);
const ZoomScope= new Enum(['GROUP','SINGLE', 'LIST']);
const ZOOM_WAIT_MS= 2000; // 2 seconds

export default {dispatchZoom, makeZoomAction, UserZoomTypes};

const zoomTimers= [];

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
        var pv= PlotViewUtil.getPlotViewById(plotId);
        if (!pv) return;



        var level;
        var isFullScreen;
        var useDelay;
        if ([UserZoomTypes.UP,UserZoomTypes.DOWN,UserZoomTypes.ONE].includes(userZoomType)) {
            level= getNextZoomLevel(pv.primaryPlot.zoomFactor,userZoomType);
            isFullScreen= false;
            useDelay= true; //todo

        }
        else {
           //todo
            console.log('todo: '+ userZoomType.key);
        }
        doZoom(dispatcher,rawAction,plotId,level,isFullScreen,useDelay);

    };

}


/**
 *
 * @param dispatcher
 * @param rawAction
 * @param plotId
 * @param zoomLevel
 * @param isFullScreen
 * @param useDelay
 */
function doZoom(dispatcher,rawAction,plotId,zoomLevel,isFullScreen, useDelay) {
    var payload= Object.assign({zoomLevel},rawAction.payload);
    dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_START, payload } );

    var idx= zoomTimers.findIndex( (t) => t.plotId===plotId);
    if (idx>-1) {
        clearTimeout(zoomTimers[idx].timerId);
        zoomTimers.splice(idx,1);
    }

    if (useDelay) {
        var timerId= setTimeout(doZoomNow, ZOOM_WAIT_MS, dispatcher,rawAction,plotId,zoomLevel,isFullScreen);
        zoomTimers.push({plotId,timerId});
    }
    else {
        doZoomNow(ZOOM_WAIT_MS, dispatcher,rawAction,plotId,zoomLevel,isFullScreen);
    }
}



function doZoomNow(dispatcher,rawAction,plotId,zoomLevel,isFullScreen) {
    var idx= zoomTimers.findIndex( (t) => t.plotId===plotId);
    if (idx>-1) zoomTimers.splice(idx,1);

    var pv= PlotViewUtil.getPlotViewById(plotId);
    PlotServicesJson.setZoomLevel(PlotViewUtil.getPlotStateAry(pv),zoomLevel,isFullScreen)
        .then( (wpResult) => processZoomSuccess(dispatcher,plotId,zoomLevel,rawAction.payload,wpResult) )
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
function processZoomSuccess(dispatcher, plotId, zoomLevel, rawPayload, result) {
    var payload = Object.assign({zoomLevel}, rawPayload);
    if (result.success) {
        var resultAry = result[WebPlotResult.RESULT_ARY];
        if (resultAry[0].success) {
            payload.plotId= plotId;
            payload.primaryStateJson = resultAry[0].data[WebPlotResult.PLOT_STATE];
            payload.primaryTiles = resultAry[0].data[WebPlotResult.PLOT_IMAGES];
            payload.overlayStateJsonAry = [];
            payload.overlayTilesAry = [];
            for (let i = 1; (i < resultAry.length); i++) {
                payload.overlayStateJsonAry[i - 1] = resultAry[i].data[WebPlotResult.PLOT_STATE];
                payload.overlayTilesAry[i - 1] = resultAry[i].data[WebPlotResult.PLOT_IMAGES];
            }
        }
        dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE, payload} );
    }
    else {
        dispatcher( { type: ImagePlotCntlr.ZOOM_IMAGE_FAIL, payload: {plotId, zoomLevel, error:Error('payload failed')} } );
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


