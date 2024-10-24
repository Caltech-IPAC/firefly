/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flatten, isArray, uniqBy} from 'lodash';
import {getExtName, getExtType} from '../FitsHeaderUtil.js';
import {getCenterPtOfPlot} from '../WebPlotAnalysis';
import {DEFAULT_THUMBNAIL_SIZE, WebPlotRequest, WPConst} from '../WebPlotRequest.js';
import ImagePlotCntlr, {IMAGE_PLOT_KEY, makeUniqueRequestKey, visRoot,
    dispatchPlotProgressUpdate, dispatchRecenter, dispatchWcsMatch} from '../ImagePlotCntlr.js';
import {dispatchActiveTarget, getActiveTarget} from '../../core/AppDataCntlr.js';
import {RDConst, WebPlot} from '../WebPlot.js';
import {PlotAttribute} from '../PlotAttribute';
import {PlotState} from '../PlotState.js';
import {Band} from '../Band.js';
import {PlotPref} from '../PlotPref.js';
import {makePostPlotTitle} from '../reducer/PlotTitle.js';
import {dispatchAddViewerItems, EXPANDED_MODE_RESERVED, IMAGE} from '../MultiViewCntlr.js';
import {getPlotViewById, getPlotViewIdListInOverlayGroup, hasWCSProjection} from '../PlotViewUtil.js';
import {enableMatchingRelatedData} from '../RelatedDataUtil.js';
import {doFetchTable} from '../../tables/TableUtil.js';
import {callGetWebPlot, callGetWebPlot3Color, callGetWebPlotGroup} from '../../rpc/PlotServicesJson.js';
import {onViewDimDefined} from '../PlotCompleteMonitor.js';
import {getActiveRequestKey} from './ActivePlottingTask.js';
import {
    addDrawLayers, makeCubeCtxAry, makeGroupPayload, makeSinglePlotPayload, populateFromHeader
} from './CreateTaskUtil.js';
import {logger} from '../../util/Logger.js';


/**
 *
 * @param {Action} rawAction
 * @return {Function}
 */
export function makePlotImageAction(rawAction) {
    return (dispatcher, getState) => {
        let vr= getState()[IMAGE_PLOT_KEY];
        const {wpRequestAry}= rawAction.payload;
        const requestKey= makeUniqueRequestKey('plotRequestKey');

        const payload= wpRequestAry ?
            makeGroupPayload(vr,rawAction.payload, requestKey) :
            makeSinglePlotPayload(vr, rawAction.payload, requestKey);

        vr= getState()[IMAGE_PLOT_KEY];
        if (vr.wcsMatchType && !rawAction.payload.holdWcsMatch) {
            dispatcher({ type: ImagePlotCntlr.WCS_MATCH, payload: {wcsMatchType:false} });
        }
        const action= { type: ImagePlotCntlr.PLOT_IMAGE_START,payload};
        dispatcher(action);
        wpRequestAry ? executeGroupSearch(action, dispatcher) : executeSingleSearch(action,dispatcher);
    };
}

async function executeSingleSearch(rawAction, dispatcher) {
    const {plotId,threeColor, useContextModifications:useCtxMods}= rawAction.payload;
    let {wpRequest, redReq, greenReq, blueReq}= rawAction.payload;

    const pv= getPlotViewById(visRoot(),plotId);
    if (pv) {
        const {plotViewCtx}= pv;
        if (wpRequest && !Array.isArray(wpRequest)) {
            wpRequest= modifyRequest(plotViewCtx,wpRequest,Band.NO_BAND, useCtxMods);
        }
        if (redReq) redReq= modifyRequest(plotViewCtx,redReq,Band.RED, useCtxMods);
        if (greenReq) greenReq= modifyRequest(plotViewCtx,greenReq,Band.GREEN, useCtxMods);
        if (blueReq) blueReq= modifyRequest(plotViewCtx,blueReq,Band.BLUE, useCtxMods);
    }
    try {
        const wpResult= threeColor ? await callGetWebPlot3Color(redReq,greenReq,blueReq) : await callGetWebPlot(wpRequest);
        processPlotImageSuccessResponse(dispatcher,rawAction.payload,wpResult);
    } catch (e) {
        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_FAIL, payload: {plotId, error:e} } );
        logger.error(`plot error, ImagePlotter, plotId: ${plotId}`, e);
    }
}

async function executeGroupSearch(rawAction, dispatcher) {
    const {requestKey, useContextModifications:useCtxMods}= rawAction.payload;
    let {wpRequestAry}= rawAction.payload;

    wpRequestAry= wpRequestAry.map( (req) =>{
        const pv= getPlotViewById(visRoot(),req.getPlotId());
        return pv ? modifyRequest(pv.plotViewCtx,req,Band.NO_BAND, useCtxMods) : req;
    });
    try {
        const wpResult= await callGetWebPlotGroup(wpRequestAry, requestKey);
        processPlotImageSuccessResponse(dispatcher,rawAction.payload,wpResult);
    } catch (e) {
        const plotIdAry= wpRequestAry.map( (r) => r.getPlotId()).filter( (id) => id);
        dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_FAIL, payload: {plotIdAry, wpRequestAry, error:e} } );
        logger.error('plot group error', e);
    }
}


/**
 *
 * @param {object} pvCtx
 * @param {WebPlotRequest} r
 * @param {Band} band
 * @param useCtxMods
 * @return {WebPlotRequest}
 */
function modifyRequest(pvCtx, r, band, useCtxMods) {
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

    return retval;
}

function getRequestFromResult(result) {
    if (result.PlotCreateHeader) return WebPlotRequest.parse(result.PlotCreateHeader.plotRequestSerialize);
    return PlotState.makePlotStateWithJson(result.PlotCreate[0].plotState)?.getWebPlotRequest();
}


async function setRelatedDataItem(pc, searchParams, dataKey, hduIdx, hduName, hduVersion, hduLevel) {
    try {
        const wlTable= await doFetchTable(searchParams);
        if (wlTable) {
            pc.relatedData.push({
                dataType:RDConst.WAVELENGTH_TABLE_RESOLVED, dataKey:dataKey+'-resolved',
                table:wlTable, hduIdx, hduName, hduVersion, hduLevel
            });
        }
    }
    catch (e) {
        console.log(`failed: related data: hdu: ${hduIdx}, hdu name: ${hduName}, used for: ${dataKey} --- server error: ${e.toString()}`);
    }

}

function getRelatedData(successAry) {
    const promiseAry= [Promise.resolve()];
    successAry.forEach( (s) => s.PlotCreate
        .forEach( (pc) => {
            const tTypeAry= pc.relatedData && pc.relatedData.filter( (r) => r.dataType==='WAVELENGTH_TABLE');
            if (tTypeAry?.length) {
                tTypeAry.forEach( async ({searchParams, dataKey, hduIdx, hduName, hduVersion, hduLevel}) => {
                    const p= setRelatedDataItem(pc, searchParams, dataKey, hduIdx, hduName, hduVersion, hduLevel);
                    promiseAry.push(p);
                });
            }
        }));
    return Promise.all(promiseAry);
}

/**
 *
 * @param dispatcher
 * @param {object} payload the payload of the original action
 * @param {object} result the result of the search
 */
async function processPlotImageSuccessResponse(dispatcher, payload, result) {

     // the following line checks to see if we are processing the results from the right request
    if (payload.requestKey && result.requestKey && payload.requestKey!==result.requestKey) return;
    if (payload.wpRequestAry &&
        !payload.wpRequestAry.every( (r) => r.getRequestKey() === getActiveRequestKey(r.getPlotId()))) return;
    if (payload.wpRequest && payload.wpRequest.getRequestKey()!==getActiveRequestKey(payload.wpRequest.getPlotId())) return;

    const group= Array.isArray(result.data);
    const successAry= group ? result.data.filter( (d) => d.data.success).map( (d) => d.data) : result.success ? [result] : [];
    const failAry= group ? result.data.filter( (d) => !d.data.success).map( (d) => d.data) : !result.success ? [result] : [];

    successAry.forEach( (result) => {
        const wpRequest= getRequestFromResult(result);
        dispatchPlotProgressUpdate(wpRequest.getPlotId(), 'Loading Images', false,wpRequest.getRequestKey());
    });

    // try {
        await getRelatedData(successAry);
    // }
    // catch (e) {
    //    console.log('related data failed'+ e) ;
    // }
    successAry.forEach( (r) => {
        onViewDimDefined(getRequestFromResult(r)?.getPlotId())
            .then(() => processSuccessResult(dispatcher, payload, [r]));
    });
    if (failAry.length) processFailResult(failAry,payload, dispatcher);
}

function processSuccessResult(dispatcher, payload, successAry) {
    const pvNewPlotInfoAry= successAry.map( (r) => createPlots(r.PlotCreate, r.PlotCreateHeader,payload, r.requestKey) );
    const resultPayload= Object.assign({},payload, {pvNewPlotInfoAry});

    pvNewPlotInfoAry.forEach( ({plotAry}) => { // images are small enough, clear the png tiles then images will load direct
        const realPlotAry = isArray(plotAry) ? plotAry : [plotAry];
        realPlotAry.forEach((p) => p.tileData = undefined );
    });

    dispatcher({type: ImagePlotCntlr.PLOT_IMAGE, payload: resultPayload});
    const plotIdAry = pvNewPlotInfoAry.map((info) => info.plotId);
    const filteredPlotIdAry= plotIdAry.filter( (id) => getPlotViewById(visRoot(),id));
    dispatcher({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotIdAry:filteredPlotIdAry}});

    matchAndActivateOverlayPlotViewsByGroup(filteredPlotIdAry);

    pvNewPlotInfoAry
        .forEach((info) => info.plotAry
            .forEach( (p)  => {
                const pv= getPlotViewById(visRoot(),p.plotId);
                if (!pv) return;
                addDrawLayers(p.plotState.getWebPlotRequest(), pv, p);
                if (p.attributes[PlotAttribute.INIT_CENTER]) dispatchRecenter({plotId:p.plotId});
            } ));

    //todo- this this plot is in a group and locked, make a unique list of all the drawing layers in the group and add to new

    const expandPlotIdAry= filteredPlotIdAry.filter( (pid) => getPlotViewById(visRoot(),pid)?.plotViewCtx.canBeExpanded);
    dispatchAddViewerItems(EXPANDED_MODE_RESERVED, expandPlotIdAry, IMAGE);

    const vr= visRoot();
    if (vr.wcsMatchType && vr.positionLock) {
        const matchId= getPlotViewById(vr,vr.mpwWcsPrimId)?.plotId ?? vr.activePlotId;
        dispatchWcsMatch( {plotId:matchId, matchType:vr.wcsMatchType, lockMatch:true});
    }
}

function processFailResult(failAry, originalPayload, dispatcher) {
    failAry.forEach( (r) => {
        const {briefFailReason, userFailReason, detailFailReason, plotId, requestKey}= r;
        if (originalPayload.plotId) dispatchAddViewerItems(EXPANDED_MODE_RESERVED, [originalPayload.plotId], IMAGE);
        if (getPlotViewById(visRoot(),plotId)?.request?.getRequestKey()===requestKey) {
            const payload= {
                ...originalPayload, plotId, detailFailReason,
                briefDescription: briefFailReason,
                description: 'Failed- ' + userFailReason};
            dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_FAIL, payload} );
        }
    });
}


/**
 * @global
 * @public
 * @typedef {Object} PvNewPlotInfo
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
 * @param {Array.<WebPlotInitializer>} plotCreate
 * @param {Object} plotCreateHeader
 * @param payload
 * @param requestKey
 * @return {PvNewPlotInfo}
 */
function createPlots(plotCreate, plotCreateHeader, payload, requestKey) {
    populateFromHeader(plotCreateHeader, plotCreate);
    const cubeCtxAry= makeCubeCtxAry(plotCreate);
    const plotState= PlotState.makePlotStateWithJson(plotCreate[0].plotState);
    const request0= plotState.getWebPlotRequest();
    const rv0= plotState.getRangeValues();
    const plotId= request0.getPlotId();
    const viewDim= getPlotViewById(visRoot(), plotId)?.viewDim;
    const initAttributes= plotCreateHeader ? {...payload.attributes, ...plotCreateHeader.attributes} :
                                             {...payload.attributes};

    const attributes= {...initAttributes,  ...request0.getAttributes()};
    let title;
    const plotAry= plotCreate.map((wpInit,idx) => {
        const plot= WebPlot.makeWebPlotData(plotId, viewDim, wpInit, attributes,false, cubeCtxAry[idx],request0,rv0);
        const extStr= plotCreate.length===1 ? getExtName(plot) || getExtType(plot) : undefined;
        if (!title) title= makePostPlotTitle(plot,request0, extStr);
        plot.title= title;
        return plot;
    });
    if (plotAry.length) updateActiveTarget(plotAry[0]);
    return {plotId, requestKey, plotAry, overlayPlotViews:null};
}



/**
 * @param {WebPlot} plot
 */
function updateActiveTarget(plot) {
    if (!plot || !hasWCSProjection(plot)) return;
    const req= plot.plotState.getWebPlotRequest();
    if (!req) return;
    let activeTarget;
    if (!getActiveTarget()) {
        const circle = req.getRequestArea();
        if (req.getOverlayPosition())     activeTarget= req.getOverlayPosition();
        else if (circle && circle.center) activeTarget= circle.center;
        else                              activeTarget= getCenterPtOfPlot(plot);

    }
    if (activeTarget) dispatchActiveTarget(activeTarget);
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
            const opvMatchArray= uniqBy(flatten(getPlotViewIdListInOverlayGroup(vr, pv.plotId)
                                                       .filter( (id) => id!== pv.plotId)
                                                       .map( (id) => getPlotViewById(vr,id))
                                                       .map( (gpv) => gpv.overlayPlotViews)),
                                   'maskNumber' );
            enableMatchingRelatedData(pv,opvMatchArray);
        });
}