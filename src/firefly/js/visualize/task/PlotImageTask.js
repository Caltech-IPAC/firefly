/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flatten, isArray, uniqueId, uniq, uniqBy, get, isEmpty} from 'lodash';
import {WebPlotRequest, GridOnStatus} from '../WebPlotRequest.js';
import ImagePlotCntlr, {visRoot, makeUniqueRequestKey,
                        IMAGE_PLOT_KEY, dispatchDeleteOverlayPlot} from '../ImagePlotCntlr.js';
import {dlRoot, dispatchCreateDrawLayer, dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import {WebPlot,PlotAttribute, RDConst} from '../WebPlot.js';
import CsysConverter from '../CsysConverter.js';
import {dispatchActiveTarget, getActiveTarget} from '../../core/AppDataCntlr.js';
import VisUtils from '../VisUtil.js';
import {PlotState} from '../PlotState.js';
import Point, {makeImagePt} from '../Point.js';
import {WPConst, DEFAULT_THUMBNAIL_SIZE} from '../WebPlotRequest.js';
import {Band} from '../Band.js';
import {PlotPref} from '../PlotPref.js';
import ActiveTarget  from '../../drawingLayers/ActiveTarget.js';
import PointSelection  from '../../drawingLayers/PointSelection.js';
import * as DrawLayerCntlr from '../DrawLayerCntlr.js';
import {makePostPlotTitle} from '../reducer/PlotTitle.js';
import {dispatchAddViewerItems, EXPANDED_MODE_RESERVED, IMAGE, DEFAULT_FITS_VIEWER_ID} from '../MultiViewCntlr.js';
import {getPlotViewById, getDrawLayerByType, getPlotViewIdListInGroup} from '../PlotViewUtil.js';
import {enableMatchingRelatedData} from '../RelatedDataUtil.js';
import {modifyRequestForWcsMatch} from './WcsMatchTask.js';
import WebGrid from '../../drawingLayers/WebGrid.js';

//const INIT_STATUS_UPDATE_DELAY= 7000;

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


function makeSinglePlotPayload(vr, rawPayload, requestKey) {

   var {wpRequest, plotId, threeColor, viewerId=DEFAULT_FITS_VIEWER_ID, attributes, setNewPlotAsActive= true,
         holdWcsMatch= false, pvOptions= {}, addToHistory= false,useContextModifications= true}= rawPayload;

    wpRequest= ensureWPR(wpRequest);

    const req= getFirstReq(wpRequest);


    if (isArray(wpRequest)) {
        if (!plotId) plotId= req.getPlotId() || uniqueId('defaultPlotId-');
        wpRequest.forEach( (r) => {if (r) r.setPlotId(plotId);});
    }
    else {
        if (!plotId) plotId= req.getPlotId() || uniqueId('defaultPlotId-');
        wpRequest.setPlotId(plotId);
    }

    if (vr.wcsMatchType && vr.mpwWcsPrimId && holdWcsMatch) {
        const wcsPrim= getPlotViewById(vr,vr.mpwWcsPrimId);
        wpRequest= isArray(wpRequest) ?
            wpRequest.map( (r) => modifyRequestForWcsMatch(wcsPrim, r)) :
            modifyRequestForWcsMatch(wcsPrim, wpRequest);
    }


    const payload= { plotId:req.getPlotId(),
                     plotGroupId:req.getPlotGroupId(),
                     groupLocked:req.isGroupLocked(),
                     requestKey, attributes, viewerId, pvOptions, addToHistory,
                     useContextModifications, threeColor, setNewPlotAsActive};

    const existingPv= getPlotViewById(vr,plotId);
    if (existingPv) {
        payload.oldOverlayPlotViews= {[plotId] :existingPv.overlayPlotViews};
    }

    if (threeColor) {
        if (isArray(wpRequest)) {
            payload.redReq= addRequestKey(wpRequest[Band.RED.value], requestKey);
            payload.greenReq= addRequestKey(wpRequest[Band.GREEN.value], requestKey);
            payload.blueReq= addRequestKey(wpRequest[Band.BLUE.value], requestKey);
        }
        else {
            payload.redReq= addRequestKey(wpRequest,requestKey);
        }
    }
    else {
        payload.wpRequest= addRequestKey(wpRequest,requestKey);
    }

    return payload;
}


/**
 *
 * @param rawAction
 * @return {Function}
 */
function makePlotImageAction(rawAction) {
    return (dispatcher, getState) => {

        var vr= getState()[IMAGE_PLOT_KEY];
        var {wpRequestAry}= rawAction.payload;
        var payload;
        const requestKey= makeUniqueRequestKey('plotRequestKey');

        if (!wpRequestAry) {
            payload= makeSinglePlotPayload(vr, rawAction.payload, requestKey);
        }
        else {
            payload= {
                wpRequestAry:ensureWPR(wpRequestAry),
                viewerId:rawAction.payload.viewerId,
                attributes:rawAction.payload.attributes,
                pvOptions: rawAction.payload.pvOptions,
                threeColor:false,
                addToHistory:false,
                useContextModifications:true,
                groupLocked:true,
                requestKey
            };

            payload.wpRequestAry= payload.wpRequestAry.map( (req) =>
                            addRequestKey(req,makeUniqueRequestKey('groupItemReqKey-'+req.getPlotId())));


            payload.oldOverlayPlotViews= wpRequestAry
                .map( (wpr) => getPlotViewById(vr,wpr.getPlotId()))
                .filter( (pv) => get(pv, 'overlayPlotViews'))
                .reduce( (obj, pv) => {
                    obj[pv.plotId]= pv.overlayPlotViews;
                    return obj;
            },{});

            if (vr.wcsMatchType && vr.mpwWcsPrimId && rawAction.payload.holdWcsMatch) {
                const wcsPrim= getPlotViewById(vr,vr.mpwWcsPrimId);
                payload.wpRequestAry= payload.wpRequestAry.map( (wpr) => modifyRequestForWcsMatch(wcsPrim, wpr));
            }
        }

        if (firstTime) {
            initBuildInDrawLayers();
            firstTime= false;
        }

        payload.requestKey= requestKey;

        vr= getState()[IMAGE_PLOT_KEY];

        if (vr.wcsMatchType && !rawAction.payload.holdWcsMatch) {
            dispatcher({ type: ImagePlotCntlr.WCS_MATCH, payload: {wcsMatchType:false} });
        }



        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_START,payload});
        // NOTE - saga ImagePlotter handles next step
        // NOTE - saga ImagePlotter handles next step
        // NOTE - saga ImagePlotter handles next step
    };
}


function addRequestKey(r,requestKey) {
    if (!r) return;
    r= r.makeCopy();
    r.setRequestKey(requestKey);
    return r;
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
export function modifyRequest(pvCtx, r, band, useCtxMods) {

    if (!r) return r;

    const retval= r.makeCopy();

    if (retval.getRotateNorth()) retval.setRotateNorth(false);
    if (retval.getRotate()) retval.setRotate(false);
    if (retval.getRotationAngle()) retval.setRotationAngle(0);

    if (!pvCtx || !useCtxMods) return retval;

    if (pvCtx.defThumbnailSize!==DEFAULT_THUMBNAIL_SIZE && !r.containsParam(WPConst.THUMBNAIL_SIZE)) {
        retval.setThumbnailSize(pvCtx.defThumbnailSize);
    }


    const cPref= PlotPref.getCacheColorPref(pvCtx.preferenceColorKey);
    if (cPref) {
        if (cPref[band]) retval.setInitialRangeValues(cPref[band]);
        retval.setInitialColorTable(cPref.colorTableId);
    }

    const zPref= PlotPref.getCacheZoomPref(pvCtx.preferenceZoomKey);
    if (zPref) {
        retval.setInitialZoomLevel(zPref.zooomLevel);
    }

    return retval;

}
/**
 *
 * @param dispatcher
 * @param {object} payload the payload of the original action
 * @param {object} result the result of the search
 */
export function processPlotImageSuccessResponse(dispatcher, payload, result) {
    let resultPayload;
    let successAry= [];
    let failAry= [];

     // the following line checks to see if we are processing the results from the right request
    if (payload.requestKey && result.requestKey && payload.requestKey!==result.requestKey) return;

    if (result.success && Array.isArray(result.data)) {
        successAry= result.data.filter( (d) => d.data.success);
        failAry= result.data.filter( (d) => !d.data.success);
    }
    else {
        if (result.success) successAry= [{data:result}];
        else                failAry= [{data:result}];
    }


    const pvNewPlotInfoAry= successAry.map( (r) => handleSuccessfulCall(r.data.PlotCreate,payload, r.data.requestKey) );
    resultPayload= Object.assign({},payload, {pvNewPlotInfoAry});
    if (successAry.length) {
        dispatcher({type: ImagePlotCntlr.PLOT_IMAGE, payload: resultPayload});
        const plotIdAry = pvNewPlotInfoAry.map((info) => info.plotId);
        dispatcher({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotIdAry}});

        if (isEmpty(payload.oldOverlayPlotViews)) {
            matchAndActivateOverlayPlotViewsByGroup(plotIdAry);
        }
        else {
            matchAndActivateOverlayPlotViews(plotIdAry, payload.oldOverlayPlotViews);
        }


        pvNewPlotInfoAry
            .forEach((info) => info.plotAry
                .forEach( (p)  => addDrawLayers(p.plotState.getWebPlotRequest(), p) ));



        //todo- this this plot is in a group and locked, make a unique list of all the drawing layers in the group and add to new
        dispatchAddViewerItems(EXPANDED_MODE_RESERVED, plotIdAry, IMAGE);
    }


    failAry.forEach( (r) => {
        const {data}= r;
        if (payload.plotId) dispatchAddViewerItems(EXPANDED_MODE_RESERVED, [payload.plotId], IMAGE);
        resultPayload= Object.assign({},payload);
        // todo: add failure stuff to resultPayload here
        resultPayload.briefDescription= data.briefFailReason;
        resultPayload.description= 'Plot Failed- ' + data.userFailReason;
        resultPayload.detailFailReason= data.detailFailReason;
        resultPayload.plotId= data.plotId;
        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_FAIL, payload:resultPayload} );
    });

}

// function requestSuccesful(resultData, req) {
//     if (!resultData.success) return false;
//     if (!resultData.requestKey) return true;
//     if (resultData.requestKey !== findRequest() )
//
//
// }


function addDrawLayers(request, plot ) {
    const {plotId}= plot;
    request.getOverlayIds().forEach((drawLayerTypeId)=> {
        const dl = getDrawLayerByType(dlRoot(), drawLayerTypeId);
        if (dl) {
            if (dl.drawLayerTypeId===ActiveTarget.TYPE_ID) {
                const pt= plot.attributes[PlotAttribute.FIXED_TARGET];
                if (pt && pt.type===Point.W_PT) {
                    DrawLayerCntlr.dispatchAttachLayerToPlot(dl.drawLayerId, plotId);
                }
            }
            else {
                DrawLayerCntlr.dispatchAttachLayerToPlot(dl.drawLayerId, plotId);
            }
            if (dl.drawLayerTypeId===PointSelection.TYPE_ID) {
                DrawLayerCntlr.dispatchAttachLayerToPlot(dl.drawLayerId, plotId);
            }
        }
    });

    if (request.getGridOn()!==GridOnStatus.FALSE) {
        const dl = getDrawLayerByType(dlRoot(), WebGrid.TYPE_ID);
        const useLabels= request.getGridOn()===GridOnStatus.TRUE;
        if (!dl) dispatchCreateDrawLayer(WebGrid.TYPE_ID, {useLabels});
        dispatchAttachLayerToPlot(WebGrid.TYPE_ID, plotId, true);
    }
}



function getRequest(payload) {
    return payload.wpRequest || payload.redReq ||  payload.blueReq ||  payload.greenReq;
}

 /**
 * @global
 * @public
 * @typedef {Object} NewPlotInfo
 * @summary Main part of the payload of successful call to the server
 *
 * @prop {String} plotId,
 * @prop {String} requestKey,
 * @prop {WebPlot[]} plotAry
 * @prop {OverPlotView[]} overlayPlotViews
 *
 */


/**
 *
 * @param plotCreate
 * @param payload
 * @param requestKey
 * @return {NewPlotInfo}
 */
const handleSuccessfulCall= function(plotCreate, payload, requestKey) {
    const plotState= PlotState.makePlotStateWithJson(plotCreate[0].plotState);
    const plotId= plotState.getWebPlotRequest().getPlotId();

    var plotAry= plotCreate.map((wpInit) => makePlot(wpInit,plotId, payload.attributes));
    if (plotAry.length) updateActiveTarget(plotAry[0]);
    return {plotId, requestKey, plotAry, overlayPlotViews:null};
};

function makePlot(wpInit,plotId, attributes) {
    var plot= WebPlot.makeWebPlotData(plotId, wpInit);
    var r= plot.plotState.getWebPlotRequest();
    plot.title= makePostPlotTitle(plot,r);
    if (r.isMinimalReadout()) plot.attributes[PlotAttribute.MINIMAL_READOUT]= true;
    if (r.getRelatedTableRow()>-1) plot.attributes[PlotAttribute.TABLE_ROW]= r.getRelatedTableRow();
    Object.assign(plot.attributes,attributes);
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
        var circle = req.getRequestArea(); if (req.getOverlayPosition())     activeTarget= req.getOverlayPosition();
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

/**
 *
 * @param {String[]} plotIdAry
 * @param {Object.<string, OverlayPlotView[]>} oldOverlayPlotViews
 */
function matchAndActivateOverlayPlotViews(plotIdAry, oldOverlayPlotViews) {
    plotIdAry.forEach( (plotId) => dispatchDeleteOverlayPlot({plotId, deleteAll:true}));

    plotIdAry
        .map( (plotId) => getPlotViewById(visRoot(), plotId))
        .filter( (pv) => pv)
        .forEach( (pv) => enableMatchingRelatedData(pv,oldOverlayPlotViews[pv.plotId]));
}



/**
 *
 * @param {String[]} plotIdAry
 */
function matchAndActivateOverlayPlotViewsByGroup(plotIdAry) {
    const vr= visRoot();
    plotIdAry
        .map( (plotId) => getPlotViewById(visRoot(), plotId))
        .filter( (pv) => pv)
        .forEach( (pv) => {
            const opvMatchArray= uniqBy(flatten(getPlotViewIdListInGroup(vr, pv.plotId)
                                                       .filter( (id) => id!== pv.plotId)
                                                       .map( (id) => getPlotViewById(vr,id))
                                                       .map( (gpv) => gpv.overlayPlotViews)),
                                   'maskNumber' );
            enableMatchingRelatedData(pv,opvMatchArray);
        });
}
