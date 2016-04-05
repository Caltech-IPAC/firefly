/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {logError} from '../util/WebUtil.js';
import ImagePlotCntlr, {visRoot} from './ImagePlotCntlr.js';
import {callGetWebPlot, callGetWebPlot3Color} from '../rpc/PlotServicesJson.js';
import WebPlotResult from './WebPlotResult.js';
import WebPlot, {PlotAttribute} from './WebPlot.js';
import CsysConverter from './CsysConverter.js';
import {dispatchActiveTarget, getActiveTarget} from '../core/AppDataCntlr.js';
import VisUtils from './VisUtil.js';
import {makeImagePt} from './Point.js';
import {WPConst, DEFAULT_THUMBNAIL_SIZE} from './WebPlotRequest.js';
import {getPlotViewById} from './PlotViewUtil.js';
import {Band} from './Band.js';
import PlotPref from './PlotPref.js';
import ActiveTarget  from '../drawingLayers/ActiveTarget.js';
import DrawLayerCntlr from './DrawLayerCntlr.js';
import {makePostPlotTitle} from './reducer/PlotTitle.js';
import {dispatchAddImages, EXPANDED_MODE_RESERVED} from './MultiViewCntlr.js';

const INIT_STATUS_UPDATE_DELAY= 7000;

export default {makePlotImageAction};



//======================================== Exported Functions =============================
//======================================== Exported Functions =============================


function dispatchUpdateStatus() {
    // todo: check to see if the task is finished
    // todo: if not finished
    // todo:       call server for status update for request id, start next timer
    // todo:       when call returns:
    // todo:             fire  ImagePlotCntlr.PLOT_PROGRESS_UPDATE for plot Id
    // todo:             reset time for 2 seconds
    // todo:
    // todo: Also, move to ImagePlotCntlr

}


var firstTime= true;


/**
 *
 * @param rawAction
 * @return {Function}
 */
function makePlotImageAction(rawAction) {
    return (dispatcher) => {

        if (firstTime) {
            initBuildInDrawLayers();
            firstTime= false;
        }

        var {plotId,wpRequest,threeColor, redReq, greenReq, blueReq}= rawAction.payload;

        if (!plotId) {
            dispatcher({ type: ImagePlotCntlr.PLOT_IMAGE_FAIL,
                         payload: {plotId, error:Error('plotId is required')} });
            return;
        }

        setTimeout(dispatchUpdateStatus, INIT_STATUS_UPDATE_DELAY);
        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_START,
                      payload: rawAction.payload
        } );
        // NOTE - sega ImagePlotter handles next step
        // keeping the commented code for time being, might clean this up more

        //
        // if (rawAction.payload.useContextModifications) {
        //     var pv= getPlotViewById(visRoot(),plotId);
        //     if (pv) {
        //         var {plotViewCtx}= pv;
        //         if (wpRequest && !Array.isArray(wpRequest)) {
        //             wpRequest= modifyRequest(plotViewCtx,wpRequest,Band.NO_BAND);
        //         }
        //         if (redReq) redReq= modifyRequest(plotViewCtx,redReq,Band.RED);
        //         if (greenReq) greenReq= modifyRequest(plotViewCtx,greenReq,Band.GREEN);
        //         if (blueReq) blueReq= modifyRequest(plotViewCtx,blueReq,Band.BLUE);
        //     }
        // }
        //
        // var p= threeColor ? callGetWebPlot3Color(redReq,greenReq,blueReq) : callGetWebPlot(wpRequest);
        //
        // p.then( (wpResult) => processPlotImageSuccessResponse(dispatcher,rawAction.payload,wpResult) )
        //     .catch ( (e) => {
        //         dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_FAIL, payload: {plotId, error:e} } );
        //         logError(`plot error, plotId: ${plotId}`, e);
        //     });

    };
}







//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================


/**
 *
 * @param {object} pvCtx
 * @param {WebPlotRequest} r
 * @param {Band} band
 * @return {WebPlotRequest}
 */
export function modifyRequest(pvCtx, r, band) {

    if (!r || !pvCtx) return r;

    var retval= r.makeCopy();

    var userModRot= pvCtx.userModifiedRotate;
    if (pvCtx.rotateNorthLock) retval.setRotateNorth(true);
    if (r.getRotateNorthSuggestion() && userModRot) retval.setRotateNorth(true);



    //if (r.getRequestType()===RequestType.URL ) { //todo, when do we need to make if a full url, I think in cross-site mode
    //    r.setURL(modifyURLToFull(r.getURL()));
    //}


    //if (pv.options.rememberZoom && primePlot(pv)) {
    //    retval.setZoomType(ZoomType.STANDARD);
    //    retval.setInitialZoomLevel(plot.getZoomFact());
    //}

    if (pvCtx.defThumbnailSize!=DEFAULT_THUMBNAIL_SIZE && !r.containsParam(WPConst.THUMBNAIL_SIZE)) {
        retval.setThumbnailSize(pvCtx.defThumbnailSize);
    }


    var cPref= PlotPref.getCacheColorPref(pvCtx.preferenceColorKey);
    if (cPref) {
        if (cPref[band]) retval.setInitialRangeValues(cPref[band]);
        retval.setInitialColorTable(cPref.colorTableId);
    }


    if (pvCtx.gridId) retval.setGridId(pvCtx.gridId);


    var zPref= PlotPref.getCacheZoomPref(pvCtx.preferenceZoomKey);
    if (zPref) {
        retval.setInitialZoomLevel(zPref.zooomLevel);
    }

    //for(Map.Entry<String,String> entry : _reqMods.entrySet()) { //todo, I don't think I need this any more, use for defered loading
    //    retval.setParam(new Param(entry.getKey(), entry.getValue()));
    //}
    return retval;

}



/**
 *
 * @param dispatcher
 * @param {object} payload the payload of the original action
 * @param {object} result the result of the search
 */
export const processPlotImageSuccessResponse= function(dispatcher, payload, result) {
    var resultPayload;

    if (result.success) {
        resultPayload= handleSuccess(result,payload);
        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE, payload:resultPayload} );
        dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[resultPayload.plotId]}} );
        resultPayload.plotAry
            .map( (p) => ({r:p.plotState.getWebPlotRequest(),plotId:p.plotId}))
            .forEach( (obj) => obj.r.getOverlayIds()
                .forEach( (drawLayerId)=>  DrawLayerCntlr.dispatchAttachLayerToPlot(drawLayerId,obj.plotId)));

        //todo- this this plot is in a group and locked, make a unique list of all the drawing layers in the group and add to new
        dispatchAddImages(EXPANDED_MODE_RESERVED, [resultPayload.plotId]);
    }
    else {
        var req= getRequest(payload);
        resultPayload= Object.assign({},payload);
        // todo: add failure stuff to resultPayload here
        var title = req.getTitle() || '';
        resultPayload.briefDescription= result.briefFailReason;
        resultPayload.description= title + ': Plot Failed- ' + result.userFailReason;
        resultPayload.detailFailReason= result.detailFailReason;
        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_FAIL, payload:resultPayload} );
    }
};


function getRequest(payload) {
    return payload.wpRequest || payload.redReq ||  payload.blueReq ||  payload.greenReq;
}


const handleSuccess= function(result, payload) {
    var plotAry= result[WebPlotResult.PLOT_CREATE].map((wpInit) => makePlot(wpInit,payload.plotId));
    if (plotAry.length) updateActiveTarget(plotAry[0]);
    return Object.assign({}, payload, {plotAry});

};

function makePlot(wpInit,plotId) {
    var plot= WebPlot.makeWebPlotData(plotId, wpInit);
    var r= plot.plotState.getWebPlotRequest();
    plot.title= makePostPlotTitle(plot,r);
    if (r.isMinimalReadout()) plot.attributes[PlotAttribute.MINIMAL_READOUT]= true;
    return plot;
}


/**
 * @param {WebPlot} plot
 */
function updateActiveTarget(plot) {
    if (!plot) return;

    var req= plot.plotState.getWebPlotRequest();
    if (!req) return;

    var corners;
    var activeTarget;


    if (!getActiveTarget()) {
        var circle = req.getRequestArea();

        if (req.getOverlayPosition())     activeTarget= req.getOverlayPosition();
        else if (circle && circle.center) activeTarget= circle.center;
        else                              activeTarget= VisUtils.getCenterPtOfPlot(plot);

    }

    if (req.getSaveCorners()) {
        var w= plot.dataWidth;
        var h= plot.dataHeight;
        var cc= CsysConverter.make(plot);
        var pt1= cc.getWorldCoords(makeImagePt(0, 0));
        var pt2= cc.getWorldCoords(makeImagePt(w, 0));
        var pt3= cc.getWorldCoords(makeImagePt(w,h));
        var pt4= cc.getWorldCoords(makeImagePt(0, h));
        if (pt1 && pt2 && pt3 && pt4) {
            corners= [pt1,pt2,pt3,pt4];
        }
    }

    if (activeTarget || corners) dispatchActiveTarget(activeTarget,corners);
}

function initBuildInDrawLayers() {
    DrawLayerCntlr.dispatchCreateDrawLayer(ActiveTarget.TYPE_ID);
}


