/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {logError} from '../util/WebUtil.js';
import ImagePlotCntlr, {visRoot} from './ImagePlotCntlr.js';
import PlotServicesJson from '../rpc/PlotServicesJson.js';
import WebPlotResult from './WebPlotResult.js';
import WebPlot from './WebPlot.js';
import CsysConverter from './CsysConverter.js';
import AppDataCntlr from '../core/AppDataCntlr.js';
import VisUtils from './VisUtil.js';
import {makeImagePt} from './Point.js';
import {WPConst, DEFAULT_THUMBNAIL_SIZE} from './WebPlotRequest.js';
import {getPlotViewById} from './PlotViewUtil.js';
import Band from './Band.js';
import PlotPref from './PlotPref.js';
import ActiveTarget  from '../drawingLayers/ActiveTarget.js';
import DrawLayerCntrl from './DrawLayerCntlr.js';

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

        var {plotId,wpRequest}= rawAction.payload;

        if (!plotId) {
            dispatcher({ type: ImagePlotCntlr.PLOT_IMAGE_FAIL,
                         payload: {plotId, error:Error('plotId is required')} });
            return;
        }



        setTimeout(dispatchUpdateStatus, INIT_STATUS_UPDATE_DELAY);
        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_START,
                      payload: rawAction.payload
        } );

        if (rawAction.useContextModifications) {
            wpRequest= modifyRequest(plotId,wpRequest,Band.NO_BAND,true);
        }


        PlotServicesJson.getWebPlot(wpRequest)
            .then( (wpResult) => processSuccessResponse(dispatcher,rawAction.payload,wpResult) )
            .catch ( (e) => {
                dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_FAIL, payload: {plotId, error:e} } );
                logError(`plot error, plotId: ${plotId}`, e);
            });

        //do network call.then => call completed action
    };
}







//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================


/**
 *
 * @param {string} plotId
 * @param {WebPlotRequest} r
 * @param {Band} band
 * @return {WebPlotRequest}
 */
function modifyRequest(plotId, r, band) {

    if (!r) return r;

    var pv= PlotViewUtil.getPlotViewById(visRoot(),plotId);
    if (!pv) return r;

    var retval= r.makeCopy();

    var userModRot= pv && pv.userModifiedRotate;
    if (pv.options.rotateNorth) retval.setRotateNorth(true);
    if (r.getRotateNorthSuggestion() && userModRot) retval.setRotateNorth(true);



    //if (r.getRequestType()==RequestType.URL ) { //todo, when do we need to make if a full url, I think in cross-site mode
    //    r.setURL(modifyURLToFull(r.getURL()));
    //}


    //if (pv.options.rememberZoom && pv.primaryPlot) {
    //    retval.setZoomType(ZoomType.STANDARD);
    //    retval.setInitialZoomLevel(plot.getZoomFact());
    //}

    if (pv.defThumbnailSize!=DEFAULT_THUMBNAIL_SIZE && !r.containsParam(WPConst.THUMBNAIL_SIZE)) {
        retval.setThumbnailSize(pv.defThumbnailSize);
    }


    var cPref= PlotPref.getCacheColorPref(pv.preferenceColorKey);
    if (cPref) {
        if (cPref[band]) retval.setInitialRangeValues(cPref[band]);
        retval.setInitialColorTable(cPref.colorTableId);
    }


    if (pv.attributes[WPConst.GRID_ID]) {
        retval.setGridId(pv.attributes[WPConst.GRID_ID]);
    }

    var zPref= PlotPref.getCacheZoomPref(pv.preferenceZoomKey);
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
const processSuccessResponse= function(dispatcher, payload, result) {
    var resultPayload;

    if (result.success) {
        resultPayload= handleSuccess(result,payload);
        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE, payload:resultPayload} );
        dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[resultPayload.plotId]}} );
        resultPayload.plotAry
            .map( (p) => ({r:p.plotState.getWebPlotRequest(),plotId:p.plotId}))
            .forEach( (obj) => obj.r.getOverlayIds()
                .forEach( (drawLayerId)=>  DrawLayerCntrl.dispatchAttachLayerToPlot(drawLayerId,obj.plotId)));

        //todo- this this plot is in a group and locked, make a unique list of all the drawing layers in the group and add to new
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
    //console.log(`result from plot call:`, result);
    var crAry= result[WebPlotResult.PLOT_CREATE];

    var plotAry= crAry.map( (wpInit) => WebPlot.makeWebPlotData(payload.plotId, wpInit));
    if (plotAry.length) {
        updateActiveTarget(plotAry[0].plotState.getWebPlotRequest(), plotAry[0]);
    }
    return Object.assign({}, payload, {plotAry});


    //crAry.forEach( (wpInit) => {
    //
    //
    //    if (getRequest(payload).isMinimalReadout()) plotInit.attributes[WPConst.MINIMAL_READOUT]=true;
    //    //todo:  arrange plot data and make an entry for each plot
    //
    //});


};


/**
 *
 * @param {WebPlotRequest} req
 * @param {WebPlot} plot
 */
function updateActiveTarget(req,plot) {
    if (!req && !plot) return;


    var corners;
    var activeTarget;


    if (!AppDataCntlr.getActiveTarget()) {
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


    if (activeTarget || corners) AppDataCntlr.setActiveTarget(activeTarget,corners);
}

function initBuildInDrawLayers() {
    DrawLayerCntrl.dispatchCreateDrawLayer(ActiveTarget.TYPE_ID);
}


