/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get,isPlainObject,isArray} from 'lodash';
import {logError} from '../util/WebUtil.js';
import {WebPlotRequest} from './WebPlotRequest.js';
import ImagePlotCntlr, {visRoot, makeUniqueRequestKey} from './ImagePlotCntlr.js';
import {WebPlot,PlotAttribute} from './WebPlot.js';
import CsysConverter from './CsysConverter.js';
import {dispatchActiveTarget, getActiveTarget} from '../core/AppDataCntlr.js';
import VisUtils from './VisUtil.js';
import {PlotState} from './PlotState.js';
import {makeImagePt} from './Point.js';
import {WPConst, DEFAULT_THUMBNAIL_SIZE} from './WebPlotRequest.js';
import {Band} from './Band.js';
import {PlotPref} from './PlotPref.js';
import ActiveTarget  from '../drawingLayers/ActiveTarget.js';
import * as DrawLayerCntlr from './DrawLayerCntlr.js';
import {makePostPlotTitle} from './reducer/PlotTitle.js';
import {dispatchAddImages, EXPANDED_MODE_RESERVED} from './MultiViewCntlr.js';

const INIT_STATUS_UPDATE_DELAY= 7000;

export default {makePlotImageAction};



//======================================== Exported Functions =============================
//======================================== Exported Functions =============================



var firstTime= true;


function ensureWPR(inVal) {
    if (isArray(inVal)) {
        return inVal.map( (v) => WebPlotRequest.makeFromObj(v));
    }
    else {
        return WebPlotRequest.makeFromObj(inVal);
    }
}

const getFirstReq= (wpRAry) => isArray(wpRAry) ? wpRAry.find( (r) => r?true:false) : wpRAry;


function makeSinglePlotPayload({wpRequest,plotId, threeColor, viewerId, 
                                addToHistory= false,useContextModifications= true}  ) {
    wpRequest= ensureWPR(wpRequest);

    const req= getFirstReq(wpRequest);


    if (isArray(wpRequest)) {
        if (!plotId) plotId= req.getPlotId();
        wpRequest.forEach( (r) => {if (r) r.setPlotId(plotId);});
    }
    else {
        if (plotId) wpRequest.setPlotId(plotId);
    }


    const payload= { plotId:req.getPlotId(),
                     plotGroupId:req.getPlotGroupId(),
                     groupLocked:req.isGroupLocked(),
                     viewerId, addToHistory, useContextModifications, threeColor};

    if (threeColor) {
        if (isArray(wpRequest)) {
            payload.redReq= wpRequest[Band.RED.value];
            payload.greenReq= wpRequest[Band.GREEN.value];
            payload.blueReq= wpRequest[Band.BLUE.value];
        }
        else {
            payload.redReq= wpRequest;
        }
    }
    else {
        payload.wpRequest= wpRequest;
    }

    return payload;
}


/**
 *
 * @param rawAction
 * @return {Function}
 */
function makePlotImageAction(rawAction) {
    return (dispatcher) => {

        var {wpRequestAry}= rawAction.payload;
        var payload;

        if (!wpRequestAry) {
            payload= makeSinglePlotPayload(rawAction.payload);
        }
        else {
            payload= {
                wpRequestAry:ensureWPR(wpRequestAry),
                viewerId:rawAction.payload.viewerId,
                threeColor:false,
                addToHistory:false,
                useContextModifications:true,
                groupLocked:true
            };
        }

        if (firstTime) {
            initBuildInDrawLayers();
            firstTime= false;
        }

        payload.requestKey= makeUniqueRequestKey();

        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_START,payload});
        // NOTE - sega ImagePlotter handles next step
        // NOTE - sega ImagePlotter handles next step
        // NOTE - sega ImagePlotter handles next step
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
    //    retval.setInitialZoomLevel(plot.zoomFac);
    //}

    if (pvCtx.defThumbnailSize!=DEFAULT_THUMBNAIL_SIZE && !r.containsParam(WPConst.THUMBNAIL_SIZE)) {
        retval.setThumbnailSize(pvCtx.defThumbnailSize);
    }


    var cPref= PlotPref.getCacheColorPref(pvCtx.preferenceColorKey);
    if (cPref) {
        if (cPref[band]) retval.setInitialRangeValues(cPref[band]);
        retval.setInitialColorTable(cPref.colorTableId);
    }

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
export function processPlotImageSuccessResponse(dispatcher, payload, result) {
    var resultPayload;
    var successAry= [];
    var failAry= [];

    if (result.success && Array.isArray(result.data)) {
        successAry= result.data.filter( (d) => d.success);
        failAry= result.data.filter( (d) => !d.success);
    }
    else {
        if (result.success) successAry= [{data:result}];
        else                failAry= [{data:result}];
    }


    const pvNewPlotInfoAry= successAry.map( (r) => handleSuccess(r.data.PlotCreate,payload) );
    resultPayload= Object.assign({},payload, {pvNewPlotInfoAry});
    if (successAry.length) {
        dispatcher({type: ImagePlotCntlr.PLOT_IMAGE, payload: resultPayload});
        const plotIdAry = pvNewPlotInfoAry.map((info) => info.plotId);
        dispatcher({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotIdAry}});


        pvNewPlotInfoAry.forEach((info) => {
            info.plotAry.map((p) => ({r: p.plotState.getWebPlotRequest(), plotId: p.plotId}))
                .forEach((obj) => obj.r.getOverlayIds()
                    .forEach((drawLayerId)=> DrawLayerCntlr.dispatchAttachLayerToPlot(drawLayerId, obj.plotId)));
        });

        //todo- this this plot is in a group and locked, make a unique list of all the drawing layers in the group and add to new
        dispatchAddImages(EXPANDED_MODE_RESERVED, plotIdAry);
    }


    failAry.forEach( (r) => {
        const {data}= r;
        if (payload.plotId) dispatchAddImages(EXPANDED_MODE_RESERVED, [payload.plotId]);
        resultPayload= Object.assign({},payload);
        // todo: add failure stuff to resultPayload here
        resultPayload.briefDescription= data.briefFailReason;
        resultPayload.description= 'Plot Failed- ' + data.userFailReason;
        resultPayload.detailFailReason= data.detailFailReason;
        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_FAIL, payload:resultPayload} );
    });

}





function getRequest(payload) {
    return payload.wpRequest || payload.redReq ||  payload.blueReq ||  payload.greenReq;
}


const handleSuccess= function(plotCreate, payload) { //TODO: finish
    const plotState= PlotState.makePlotStateWithJson(plotCreate[0].plotState);
    const plotId= plotState.getWebPlotRequest().getPlotId();

    var plotAry= plotCreate.map((wpInit) => makePlot(wpInit,plotId));
    if (plotAry.length) updateActiveTarget(plotAry[0]);
    return {plotId, plotAry, overlayPlotViews:null};
};

function makePlot(wpInit,plotId) {
    var plot= WebPlot.makeWebPlotData(plotId, wpInit);
    var r= plot.plotState.getWebPlotRequest();
    plot.title= makePostPlotTitle(plot,r);
    if (r.isMinimalReadout()) plot.attributes[PlotAttribute.MINIMAL_READOUT]= true;
    if (r.getRelatedTableRow()>-1) plot.attributes[PlotAttribute.TABLE_ROW]= r.getRelatedTableRow();
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


