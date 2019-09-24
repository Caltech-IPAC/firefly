/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flatten, isArray, uniqueId, uniqBy, get, isEmpty} from 'lodash';
import {WebPlotRequest, GridOnStatus} from '../WebPlotRequest.js';
import ImagePlotCntlr, {visRoot, makeUniqueRequestKey, IMAGE_PLOT_KEY} from '../ImagePlotCntlr.js';
import {dlRoot, dispatchCreateDrawLayer, dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import {dispatchActiveTarget, getActiveTarget} from '../../core/AppDataCntlr.js';
import {WebPlot, RDConst, isImage} from '../WebPlot.js';
import {PlotAttribute} from '../PlotAttribute';
import VisUtils from '../VisUtil.js';
import {PlotState} from '../PlotState.js';
import {WPConst, DEFAULT_THUMBNAIL_SIZE} from '../WebPlotRequest.js';
import {Band} from '../Band.js';
import {PlotPref} from '../PlotPref.js';
import {clone} from '../../util/WebUtil.js';
import {makePostPlotTitle} from '../reducer/PlotTitle.js';
import {dispatchAddViewerItems, getMultiViewRoot, findViewerWithItemId, EXPANDED_MODE_RESERVED, IMAGE, DEFAULT_FITS_VIEWER_ID} from '../MultiViewCntlr.js';
import {getPlotViewById, getDrawLayerByType, getDrawLayersByType, getDrawLayerById, getPlotViewIdList} from '../PlotViewUtil.js';
import {enableMatchingRelatedData, enableRelatedDataLayer} from '../RelatedDataUtil.js';
import {modifyRequestForWcsMatch} from './WcsMatchTask.js';
import WebGrid from '../../drawingLayers/WebGrid.js';
import HiPSGrid from '../../drawingLayers/HiPSGrid.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import HiPSMOC from '../../drawingLayers/HiPSMOC.js';
import {dispatchPlotProgressUpdate, dispatchRecenter, dispatchWcsMatch} from '../ImagePlotCntlr';
import {isDefined} from '../../util/WebUtil';
import {HdrConst} from '../FitsHeaderUtil.js';
import {doFetchTable} from '../../tables/TableUtil';
import ImageRoot from '../../drawingLayers/ImageRoot';

//======================================== Exported Functions =============================
//======================================== Exported Functions =============================


export function ensureWPR(inVal) {
    if (isArray(inVal)) {
        return inVal.map( (v) => WebPlotRequest.makeFromObj(v));
    }
    else {
        return WebPlotRequest.makeFromObj(inVal);
    }
}

export function determineViewerId(viewerId, plotId) {
    if (viewerId) return viewerId;
    const newViewerId= findViewerWithItemId(getMultiViewRoot(), plotId, IMAGE);
    return newViewerId || DEFAULT_FITS_VIEWER_ID;
}

export function getHipsImageConversion(hipsImageConversion ) {
    return hipsImageConversion && {
        hipsRequestRoot:ensureWPR(hipsImageConversion.hipsRequestRoot),
        imageRequestRoot:ensureWPR(hipsImageConversion.imageRequestRoot),
        allSkyRequest: ensureWPR(hipsImageConversion.allSkyRequest),
        fovDegFallOver: hipsImageConversion.fovDegFallOver,
        fovMaxFitsSize: hipsImageConversion.fovMaxFitsSize,
        plotAllSkyFirst: hipsImageConversion.plotAllSkyFirst,
        autoConvertOnZoom: hipsImageConversion.autoConvertOnZoom
    };

}

const getFirstReq= (wpRAry) => isArray(wpRAry) ? wpRAry.find( (r) => Boolean(r)) : wpRAry;


function makeSinglePlotPayload(vr, rawPayload, requestKey) {

   const {threeColor, attributes, setNewPlotAsActive= true,
         holdWcsMatch= true, useContextModifications= true, enableRestore= true,
         renderTreeId}= rawPayload;
   let {plotId, wpRequest, pvOptions= {}}= rawPayload;

    wpRequest= ensureWPR(wpRequest);

    const hipsImageConversion= getHipsImageConversion(rawPayload.hipsImageConversion);
    if (hipsImageConversion) pvOptions= clone(pvOptions, {hipsImageConversion});

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

    const payload= { plotId: req.getPlotId(),
                     plotGroupId: req.getPlotGroupId(),
                     groupLocked: req.isGroupLocked(),
                     viewerId: determineViewerId(rawPayload.viewerId, plotId),
                     hipsImageConversion,
                     requestKey, attributes, pvOptions, enableRestore,
                     useContextModifications, threeColor, setNewPlotAsActive, renderTreeId};

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
export function makePlotImageAction(rawAction) {
    return (dispatcher, getState) => {

        let vr= getState()[IMAGE_PLOT_KEY];
        const {wpRequestAry}= rawAction.payload;
        let payload;
        const requestKey= makeUniqueRequestKey('plotRequestKey');

        if (!wpRequestAry) {
            payload= makeSinglePlotPayload(vr, rawAction.payload, requestKey);
        }
        else {
            const {viewerId=DEFAULT_FITS_VIEWER_ID, attributes,
                   setNewPlotAsActive= true, pvOptions= {},
                   useContextModifications= true, enableRestore= true,
                   renderTreeId}= rawAction.payload;
            payload= {
                wpRequestAry:ensureWPR(wpRequestAry),
                viewerId,
                attributes,
                pvOptions,
                setNewPlotAsActive,
                threeColor:false,
                useContextModifications,
                enableRestore,
                groupLocked:true,
                requestKey,
                renderTreeId
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

        // if (!getDrawLayerByType(getDlAry(), ActiveTarget.TYPE_ID)) {
        //     initBuildInDrawLayers();
        // }

        payload.requestKey= requestKey;
        payload.plotType= 'image';

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
 * @param useCtxMods
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

    return retval;

}

/**
 *
 * @param dispatcher
 * @param {object} payload the payload of the original action
 * @param {object} result the result of the search
 */
export function processPlotImageSuccessResponse(dispatcher, payload, result) {
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

    successAry.forEach( (r) => {
        const plotState= PlotState.makePlotStateWithJson(r.data.PlotCreate[0].plotState);
        const wpRequest= r.data.PlotCreateHeader ?  WebPlotRequest.parse(r.data.PlotCreateHeader.plotRequestSerialize) : plotState.getWebPlotRequest();
        dispatchPlotProgressUpdate(wpRequest.getPlotId(), 'Loading Images', false,wpRequest.getRequestKey());
    });

    lookForRelatedDataThenContinue(successAry,failAry, payload, dispatcher);

}

function lookForRelatedDataThenContinue(successAry,failAry, payload, dispatcher) {
    setTimeout( () => {
        const promiseAry= [Promise.resolve()];
        successAry.forEach( (s) => s.data.PlotCreate.forEach( (pc) => {
            const tType= pc.relatedData && pc.relatedData.find( (r) => r.dataType==='WAVELENGTH_TABLE');
            if (tType) {
                const p= doFetchTable(tType.searchParams).then( (wlTable) => {
                    pc.relatedData.push({dataType:'WAVELENGTH_TABLE_RESOLVED',dataKey:tType.dataKey+'-resolved', table:wlTable});
                });
                promiseAry.push(p);
            }
        }));
        Promise.all(promiseAry).then( () =>continuePlotImageSuccess(dispatcher, payload, successAry, failAry));
    } , 5);
}

function continuePlotImageSuccess(dispatcher, payload, successAry, failAry) {

    if (successAry.length) {
        const pvNewPlotInfoAry= successAry.map( (r) => handleSuccessfulCall(r.data.PlotCreate, r.data.PlotCreateHeader,payload, r.data.requestKey) );
        const resultPayload= Object.assign({},payload, {pvNewPlotInfoAry});
        dispatcher({type: ImagePlotCntlr.PLOT_IMAGE, payload: resultPayload});
        const plotIdAry = pvNewPlotInfoAry.map((info) => info.plotId);
        dispatcher({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotIdAry}});

        matchAndActivateOverlayPlotViewsByGroup(plotIdAry);


        pvNewPlotInfoAry
            .forEach((info) => info.plotAry
                .forEach( (p)  => {
                    const pv= getPlotViewById(visRoot(),p.plotId);
                    addDrawLayers(p.plotState.getWebPlotRequest(), pv, p);
                    if (p.attributes[PlotAttribute.INIT_CENTER]) dispatchRecenter({plotId:p.plotId});
                } ));



        //todo- this this plot is in a group and locked, make a unique list of all the drawing layers in the group and add to new
        dispatchAddViewerItems(EXPANDED_MODE_RESERVED, plotIdAry, IMAGE);

        const vr= visRoot();
        if (vr.wcsMatchType && vr.positionLock) {
            dispatchWcsMatch( {plotId:vr.activePlotId, matchType:vr.wcsMatchType, lockMatch:true});
        }
    }


    failAry.forEach( (r) => {
        const {data}= r;
        if (payload.plotId) dispatchAddViewerItems(EXPANDED_MODE_RESERVED, [payload.plotId], IMAGE);
        const failPayload= {
            ...payload,
            briefDescription: data.briefFailReason,
            description: 'Failed- ' + data.userFailReason,
            detailFailReason: data.detailFailReason,
            plotId: data.plotId,
        };
        const pv= getPlotViewById(visRoot(),data.plotId);
        if (pv && pv.request && pv.request.getRequestKey()===data.requestKey) {
            dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_FAIL, payload:failPayload} );
        }

    });
}


export function addDrawLayers(request, pv, plot) {
    const {plotId}= plot;



    const fixedLayers= getDrawLayersByType(getDlAry(), ImageRoot.TYPE_ID);
    let newDL= fixedLayers.find( (dl) => dl.plotId===plot.plotId);
    if (!newDL) {
        newDL= dispatchCreateDrawLayer(ImageRoot.TYPE_ID, {plotId});
        dispatchAttachLayerToPlot(newDL.drawLayerId, pv.plotId, false);
    }

    request.getOverlayIds().forEach((drawLayerTypeId)=> {
        const dls = getDrawLayersByType(dlRoot(), drawLayerTypeId);
        dls.forEach((dl) => {
            if (dl.canAttachNewPlot) {
                const visibility = (dl.drawLayerTypeId === HiPSGrid.TYPE_ID) ||
                                    (dl.drawLayerTypeId === HiPSMOC.TYPE_ID && isEmpty(dl.visiblePlotIdAry))
                                    ? false : true;
                dispatchAttachLayerToPlot(dl.drawLayerId, plotId,  true, visibility, false);
            }
        });
    });

    if (request.getGridOn()!==GridOnStatus.FALSE && isImage(plot)) {
        const dl = getDrawLayerByType(dlRoot(), WebGrid.TYPE_ID);
        const useLabels= request.getGridOn()===GridOnStatus.TRUE;
        if (!dl) dispatchCreateDrawLayer(WebGrid.TYPE_ID, {useLabels});
        dispatchAttachLayerToPlot(WebGrid.TYPE_ID, plotId, false);
    }

    if (plot.relatedData) {
        plot.relatedData.forEach( (rd) => {
            if (rd.dataType === RDConst.TABLE) {
                const dl = getDrawLayerById(dlRoot(), rd.relatedDataId);
                if (!dl) enableRelatedDataLayer(visRoot(), getPlotViewById(visRoot(), plotId), rd);
            }

        });
    }
}



// function getRequest(payload) {
//     return payload.wpRequest || payload.redReq ||  payload.blueReq ||  payload.greenReq;
// }

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

 function findCubePlane(plotCreate) {
     const plotState= PlotState.makePlotStateWithJson(plotCreate.plotState);
     if (plotState.isThreeColor()) return -1;
     if (plotCreate.headerAry && isDefined(plotCreate.headerAry[0][HdrConst.SPOT_PL])) { // this should be the zero plane of the cube
         return Number(plotCreate.headerAry[0][HdrConst.SPOT_PL].value);
     }
     else if (!plotCreate.headerAry) {  // if no headerAry, it is a plane of the cube
         return plotState.getCubePlaneNumber();
     }
     else {
         return -1;
     }
 }



 function populateBandStateFromHeader(bandState, plotCreateHeader) {
     bandState.plotRequestSerialize = plotCreateHeader.plotRequestSerialize;
     bandState.uploadFileNameStr= plotCreateHeader.uploadFileNameStr;
     bandState.originalFitsFileStr= plotCreateHeader.originalFitsFileStr;
     bandState.workingFitsFileStr= plotCreateHeader.workingFitsFileStr;
 }


/**
 * readds the data into each plotCreate from the plotCreateHeader.
 * The data in the header is replicated in each plot and was clear for network transfere
 * efficiency. This optimization is very import for large cubes.
 * Note- data is modified in place plotCreate will me changed, no new object is created.
 * @param plotCreateHeader
 * @param plotCreate
 */
export function populateFromHeader(plotCreateHeader, plotCreate) {
     if (!plotCreateHeader) return;
     for(let i=0; i<plotCreate.length; i++) {
         if (isArray(plotCreate[0].bandStateAry)) {
             for (let j = 0; j < 3; j++) {
                 if (plotCreate[0].bandStateAry[j]) {
                     populateBandStateFromHeader(plotCreate[i].plotState.bandStateAry[j],plotCreateHeader);
                 }
             }
         }
         else {
             populateBandStateFromHeader(plotCreate[i].plotState.bandStateAry,plotCreateHeader);
         }
         plotCreate[i].dataDesc= plotCreateHeader.dataDesc;
         plotCreate[i].zeroHeaderAry= plotCreateHeader.zeroHeaderAry;
     }
 }

 export function makeCubeCtxAry(plotCreate) {
     let cubeStartIdx=-1;
     const cubeCtxAry= plotCreate
         .map( (pC,idx) => {
             const cubePlane= findCubePlane(pC);
             if (cubePlane<0) return undefined;
             if (cubePlane===0) cubeStartIdx= idx;
             const cubeStartPC= plotCreate[cubeStartIdx];
             return {
                 cubePlane,
                 cubeHeaderAry: cubeStartPC.headerAry,
                 relatedData: cubeStartPC.relatedData,
                 dataWidth: cubeStartPC.dataWidth,
                 dataHeight: cubeStartPC.dataHeight,
                 imageCoordSys: cubeStartPC.imageCoordSys
             };
         });
     return cubeCtxAry;
 }

/**
 *
 * @param {Array.<Object>} plotCreate
 * @param {Object} plotCreateHeader
 * @param payload
 * @param requestKey
 * @return {PvNewPlotInfo}
 */
function handleSuccessfulCall(plotCreate, plotCreateHeader, payload, requestKey) {
    // const plotCreate= plotCreateStrAry.map( (s) => JSON.parse(s));

    populateFromHeader(plotCreateHeader, plotCreate);
    const cubeCtxAry= makeCubeCtxAry(plotCreate);
    const plotState= PlotState.makePlotStateWithJson(plotCreate[0].plotState);
    const plotId= plotState.getWebPlotRequest().getPlotId();
    const initAttributes= {...plotCreateHeader.attributes, ...payload.attributes};

    const plotAry= plotCreate.map((wpInit,idx) => makePlot(wpInit,plotId, initAttributes, cubeCtxAry[idx]) );
    if (plotAry.length) updateActiveTarget(plotAry[0]);
    return {plotId, requestKey, plotAry, overlayPlotViews:null};
}

function makePlot(wpInit,plotId, attributes, cubeCtx) {
    const r= PlotState.makePlotStateWithJson(wpInit.plotState).getWebPlotRequest();
    const plot= WebPlot.makeWebPlotData(plotId, wpInit, {...attributes,  ...r.getAttributes()},false, cubeCtx);
    plot.title= makePostPlotTitle(plot,r);
    return plot;
}


/**
 * @param {WebPlot} plot
 */
function updateActiveTarget(plot) {
    if (!plot) return;

    const req= plot.plotState.getWebPlotRequest();
    if (!req) return;

    let activeTarget;
    if (!getActiveTarget()) {
        const circle = req.getRequestArea();
        if (req.getOverlayPosition())     activeTarget= req.getOverlayPosition();
        else if (circle && circle.center) activeTarget= circle.center;
        else                              activeTarget= VisUtils.getCenterPtOfPlot(plot);

    }
    if (activeTarget) dispatchActiveTarget(activeTarget);
}


/**
 *
 * @param {String[]} plotIdAry
 * @param {Object.<string, OverlayPlotView[]>} oldOverlayPlotViews
 */
function matchAndActivateOverlayPlotViews(plotIdAry, oldOverlayPlotViews) {
    // plotIdAry.forEach( (plotId) => dispatchDeleteOverlayPlot({plotId, deleteAll:true}));

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
            const opvMatchArray= uniqBy(flatten(getPlotViewIdList(vr, pv.plotId)
                                                       .filter( (id) => id!== pv.plotId)
                                                       .map( (id) => getPlotViewById(vr,id))
                                                       .map( (gpv) => gpv.overlayPlotViews)),
                                   'maskNumber' );
            enableMatchingRelatedData(pv,opvMatchArray);
        });
}
