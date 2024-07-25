import {isArray, uniqueId} from 'lodash';
import {getRotationAngle, isPlotRotatedNorth} from '../WebPlotAnalysis';
import {setActiveRequestKey} from './ActivePlottingTask.js';
import {
    getAllDrawLayersForPlot, getDrawLayerById, getDrawLayerByType, getDrawLayersByType, getPlotViewById,
    isDrawLayerAttached, primePlot, removeRawDataByPlotView
} from '../PlotViewUtil.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {GridOnStatus, WebPlotRequest} from '../WebPlotRequest.js';
import CsysConverter from '../CsysConverter.js';
import {Band} from '../Band.js';
import {makeUniqueRequestKey, visRoot} from '../ImagePlotCntlr.js';
import { DEFAULT_FITS_VIEWER_ID, findViewerWithItemId, getMultiViewRoot, IMAGE } from '../MultiViewCntlr.js';
import {isImage, processHeaderData, RDConst} from '../WebPlot.js';
import {enableRelatedDataLayer} from '../RelatedDataUtil.js';
import {getArcSecPerPix} from '../ZoomUtil.js';
import {ZoomType} from '../ZoomType.js';
import {isDefined} from '../../util/WebUtil.js';
import {HdrConst} from '../FitsHeaderUtil.js';
import {
    dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dispatchDestroyDrawLayer, dispatchDetachLayerFromPlot, dlRoot,
    getDlAry
} from '../DrawLayerCntlr.js';
import ImageRoot from '../../drawingLayers/ImageRoot.js';
import SearchTarget from '../../drawingLayers/SearchTarget.js';
import HiPSGrid from '../../drawingLayers/HiPSGrid.js';
import HiPSMOC from '../../drawingLayers/HiPSMOC.js';
import WebGrid from '../../drawingLayers/WebGrid.js';


/**
 * Attempt to make an WebPlotRequest from and Object or an array
 * If an array is past make an array of WebPlotRequest if and Object is passed make a single WebPlotRequest
 * @param {Object|Array} v - an object with WebPlotRequest keys or and array of objects
 * @return {WebPlotRequest|Array.<WebPlotRequest>}
 */
export function ensureWPR(v) {
    return isArray(v) ? v.map((v) => WebPlotRequest.makeFromObj(v)) : WebPlotRequest.makeFromObj(v);
}

export function getHipsImageConversion(hipsImageConversion) {
    return hipsImageConversion && {
        hipsRequestRoot: ensureWPR(hipsImageConversion.hipsRequestRoot),
        imageRequestRoot: ensureWPR(hipsImageConversion.imageRequestRoot),
        fovDegFallOver: hipsImageConversion.fovDegFallOver,
        fovMaxFitsSize: hipsImageConversion.fovMaxFitsSize,
        plotAllSkyFirst: hipsImageConversion.plotAllSkyFirst,
        autoConvertOnZoom: hipsImageConversion.autoConvertOnZoom
    };
}

export function determineViewerId(viewerId, plotId) {
    if (viewerId) return viewerId;
    const newViewerId = findViewerWithItemId(getMultiViewRoot(), plotId, IMAGE);
    return newViewerId || DEFAULT_FITS_VIEWER_ID;
}

export function modifyRequestForWcsMatch(pv, wpr) {
    const plot = primePlot(pv);
    if (!plot || !wpr) return wpr;
    const newWpr = wpr.makeCopy();
    const asPerPix = getArcSecPerPix(plot, plot.zoomFactor);
    newWpr.setRotateNorth(false);
    newWpr.setRotate(false);
    if (isPlotRotatedNorth(plot)) {
        newWpr.setRotateNorth(true);
    } else {
        const targetRotation = getRotationAngle(plot) + pv.rotation;
        newWpr.setRotate(true);
        newWpr.setRotationAngle(targetRotation);
    }
    newWpr.setZoomType(ZoomType.ARCSEC_PER_SCREEN_PIX);
    newWpr.setZoomArcsecPerScreenPix(asPerPix);
    return newWpr;
}

export function makeSinglePlotPayload(vr, rawPayload, requestKey) {
    const {
        threeColor, attributes, setNewPlotAsActive = true,
        holdWcsMatch = true, useContextModifications = true, enableRestore = true,
        renderTreeId
    } = rawPayload;
    let {plotId, wpRequest, pvOptions = {}} = rawPayload;

    wpRequest = ensureWPR(wpRequest);

    const hipsImageConversion = getHipsImageConversion(rawPayload.hipsImageConversion);
    if (hipsImageConversion) pvOptions = {...pvOptions, hipsImageConversion};

    const req = getFirstReq(wpRequest);


    if (isArray(wpRequest)) {
        if (!plotId) plotId = req.getPlotId() || uniqueId('defaultPlotId-');
        wpRequest.forEach((r) => {
            if (r) r.setPlotId(plotId);
        });
    } else {
        if (!plotId) plotId = req.getPlotId() || uniqueId('defaultPlotId-');
        wpRequest.setPlotId(plotId);
    }

    if (vr.wcsMatchType && vr.mpwWcsPrimId && holdWcsMatch) {
        const wcsPrim = getPlotViewById(vr, vr.mpwWcsPrimId);
        wpRequest = isArray(wpRequest) ?
            wpRequest.map((r) => modifyRequestForWcsMatch(wcsPrim, r)) :
            modifyRequestForWcsMatch(wcsPrim, wpRequest);
    }

    const payload = {
        plotId: req.getPlotId(),
        plotGroupId: req.getPlotGroupId(),
        groupLocked: req.isGroupLocked(),
        viewerId: determineViewerId(rawPayload.viewerId, plotId),
        plotType: 'image',
        hipsImageConversion,
        requestKey, attributes, pvOptions, enableRestore,
        useContextModifications, threeColor, setNewPlotAsActive, renderTreeId
    };

    const existingPv = getPlotViewById(vr, plotId);
    if (existingPv) {
        payload.oldOverlayPlotViews = {[plotId]: existingPv.overlayPlotViews};
    }

    if (threeColor) {
        if (isArray(wpRequest)) {
            payload.redReq = addRequestKey(wpRequest[Band.RED.value], requestKey);
            payload.greenReq = addRequestKey(wpRequest[Band.GREEN.value], requestKey);
            payload.blueReq = addRequestKey(wpRequest[Band.BLUE.value], requestKey);
        } else {
            payload.redReq = addRequestKey(wpRequest, requestKey);
        }
    } else {
        payload.wpRequest = addRequestKey(wpRequest, requestKey);
    }
    removeRawDataByPlotView(getPlotViewById(visRoot(), payload.plotId));
    [payload.wpRequest, payload.redReq, payload.blueReq, payload.greenReq]
        .forEach((r) => r && setActiveRequestKey(r.getPlotId(), requestKey));

    return payload;
}

export function makeGroupPayload(vr, rawPayload, requestKey) {
    const {
        viewerId = DEFAULT_FITS_VIEWER_ID, attributes, setNewPlotAsActive = true, pvOptions = {}, wpRequestAry,
        useContextModifications = true, enableRestore = true, renderTreeId
    } = rawPayload;
    const payload = {
        wpRequestAry: ensureWPR(wpRequestAry),
        viewerId,
        attributes,
        pvOptions,
        setNewPlotAsActive,
        threeColor: false,
        useContextModifications,
        enableRestore,
        groupLocked: true,
        requestKey,
        renderTreeId,
        plotType: 'image'
    };
    const keyRoot = makeUniqueRequestKey('groupItemReqKey-');
    payload.wpRequestAry = payload.wpRequestAry.map((req) => {
        const key = makeUniqueRequestKey(keyRoot + '-' + req.getPlotId());
        setActiveRequestKey(req.getPlotId(), key);
        return addRequestKey(req, key);
    });
    payload.wpRequestAry.forEach((r) => removeRawDataByPlotView(getPlotViewById(visRoot(), r.getPlotId())));

    payload.oldOverlayPlotViews = wpRequestAry
        .map((wpr) => getPlotViewById(vr, wpr.getPlotId()))
        .filter((pv) => pv?.overlayPlotViews)
        .reduce((obj, pv) => {
            obj[pv.plotId] = pv.overlayPlotViews;
            return obj;
        }, {});
    if (vr.wcsMatchType && vr.mpwWcsPrimId && rawPayload.holdWcsMatch) {
        const wcsPrim = getPlotViewById(vr, vr.mpwWcsPrimId);
        payload.wpRequestAry = payload.wpRequestAry.map((wpr) => modifyRequestForWcsMatch(wcsPrim, wpr));
    }
    return payload;
}

const getFirstReq= (wpRAry) => isArray(wpRAry) ? wpRAry.find( (r) => Boolean(r)) : wpRAry;

function addRequestKey(r,requestKey) {
    if (!r) return;
    r= r.makeCopy();
    r.setRequestKey(requestKey);
    return r;
}

/**
 * Only used during parsing, not for general use
 * @param {WebPlotInitializer} plotCreate
 * @return {number} plane index or -1 if not a cube plane
 */
function findCubePlane(plotCreate) {
    // if there is a header and SPOT_PL is defined it is the zero plane of a cube, otherwise if is not a cube
    // if no header then it is a cube and get the index from plotState
    if (plotCreate.plotState.threeColor) return -1;
    if (plotCreate.headerAry) {
        return isDefined(plotCreate.headerAry[0][HdrConst.SPOT_PL]) ? 0 : -1;
    }
    return plotCreate.plotState.bandStateAry.cubePlaneNumber;
}

/**
 * @param {Array.<WebPlotInitializer>} plotCreate
 * @return {Array.<Object>}
 */
export function makeCubeCtxAry(plotCreate) {
    let cubeStartIdx = -1;
    let headerInfo;

    const cubeCtxAry = plotCreate
        .map((pC, idx) => {
            const cubePlane = findCubePlane(pC);
            if (cubePlane < 0) return undefined;
            if (cubePlane === 0) {
                cubeStartIdx = idx;
                headerInfo = processHeaderData(pC);
            }
            const cubeStartPC = plotCreate[cubeStartIdx];
            const h = plotCreate[cubeStartIdx].headerAry[0];
            const cubeLength = h.NAXIS3.value === '1' && h.NAXIS4?.value ? Number(h.NAXIS4.value) : Number(h.NAXIS3.value);

            return {
                cubeCntNumber: pC.plotState.bandStateAry.cubeCnt,
                cubePlane,
                cubeLength,
                cubeHeaderAry: cubeStartPC.headerAry,
                processHeader: headerInfo.processHeader,
                wlData: headerInfo.wlData,
                wlTableRelatedAry: headerInfo.wlTableRelatedAry,
                relatedData: (idx===cubeStartIdx) ? cubeStartPC.relatedData :
                    cubeStartPC.relatedData?.filter( ({dataType}) => dataType!==RDConst.IMAGE_MASK),
                dataWidth: cubeStartPC.dataWidth,
                dataHeight: cubeStartPC.dataHeight,
                imageCoordSys: cubeStartPC.imageCoordSys,
                fluxUnits: cubeStartPC.fitsData.fluxUnits,
                getFitsFileSize: cubeStartPC.fitsData.getFitsFileSize,
                desc: cubeStartPC.desc
            };
        });
    return cubeCtxAry;
}




function populateBandStateFromHeader(bandState, plotCreateHeader) {
    bandState.plotRequestSerialize = plotCreateHeader.plotRequestSerialize;
    bandState.uploadFileNameStr = plotCreateHeader.uploadFileNameStr;
    bandState.originalFitsFileStr = plotCreateHeader.originalFitsFileStr;
    bandState.workingFitsFileStr = plotCreateHeader.workingFitsFileStr;
    bandState.rangeValuesSerialize = plotCreateHeader.rangeValuesSerialize;
    bandState.multiImageFile = Boolean(plotCreateHeader.multiImageFile);
}

/**
 * readds the data into each plotCreate from the plotCreateHeader.
 * The data in the header is replicated in each plot and was clear for network transfer
 * efficiency. This optimization is very import for large cubes.
 * Note- plotCreate is modified in place plotCreate will be changed, no new object is created.
 * @param plotCreateHeader
 * @param plotCreate
 */
export function populateFromHeader(plotCreateHeader, plotCreate) {
    if (!plotCreateHeader) return;
    for (let i = 0; i < plotCreate.length; i++) {
        if (isArray(plotCreate[0].bandStateAry)) {
            for (let j = 0; j < 3; j++) {
                if (plotCreate[0].bandStateAry[j]) {
                    populateBandStateFromHeader(plotCreate[i].plotState.bandStateAry[j], plotCreateHeader);
                }
            }
        } else {
            populateBandStateFromHeader(plotCreate[i].plotState.bandStateAry, plotCreateHeader);
        }
        plotCreate[i].dataDesc = plotCreateHeader.dataDesc;
        plotCreate[i].zeroHeaderAry = plotCreateHeader.zeroHeaderAry;
        if (plotCreateHeader.multiImage) plotCreate[i].plotState.multiImage = plotCreateHeader.multiImage;
    }
}

export function addDrawLayers(request, pv, plot) {
    const {plotId} = plot;
    const imageRootLayers = getDrawLayersByType(getDlAry(), ImageRoot.TYPE_ID);
    let newDL = imageRootLayers.find((dl) => dl.plotId === plot.plotId);
    if (!newDL) {
        newDL = dispatchCreateDrawLayer(ImageRoot.TYPE_ID, {plotId, layersPanelLayoutId: plotId});
        dispatchAttachLayerToPlot(newDL.drawLayerId, pv.plotId, false);
    }

    const displayFixedTarget = (pv?.plotViewCtx?.displayFixedTarget ?? false) && plot.attributes[PlotAttribute.FIXED_TARGET];
    const ftId = plotId + '--image-search-target';
    if (displayFixedTarget) {
        const wp = plot.attributes[PlotAttribute.FIXED_TARGET];
        const cc = CsysConverter.make(plot);
        if (cc.pointInPlot(wp)) {
            const searchTargetLayers = getDrawLayersByType(getDlAry(), SearchTarget.TYPE_ID);
            newDL = searchTargetLayers.find((dl) => dl.plotId === plot.plotId);
            if (!newDL) {
                newDL = dispatchCreateDrawLayer(SearchTarget.TYPE_ID,
                    {
                        drawLayerId: ftId,
                        displayGroupId: 'AUTO_TARGET_OVERLAY',
                        color: 'yellow',
                        plotId,
                        layersPanelLayoutId: plotId,
                        titlePrefix: isImage(plot) ? 'Image ' : 'HiPS '
                    });
                dispatchAttachLayerToPlot(newDL.drawLayerId, pv.plotId, false);
            }
        }
    } else {
        dispatchDestroyDrawLayer(ftId);
    }

    request.getOverlayIds().forEach((drawLayerTypeId) => {
        const dls = getDrawLayersByType(dlRoot(), drawLayerTypeId);
        dls.forEach((dl) => {
            if (drawLayerTypeId===HiPSMOC.TYPE_ID && pv.plotViewCtx.useForCoverage) return;
            if (dl.canAttachNewPlot && !isDrawLayerAttached(dl, plotId)) {
                const visibility = !((dl.drawLayerTypeId === HiPSGrid.TYPE_ID) || //HiPSGrid and HiPSMOC don't come up visible by default
                    (dl.drawLayerTypeId === HiPSMOC.TYPE_ID && !dl.visiblePlotIdAry.length));
                dispatchAttachLayerToPlot(dl.drawLayerId, plotId, true, visibility, true);
            }
        });
    });

    getAllDrawLayersForPlot(dlRoot(), plot.plotId).forEach((dl) => {
        if (isImage(plot)) {
            if (dl.drawLayerTypeId === HiPSGrid.TYPE_ID || dl.drawLayerTypeId === HiPSMOC.TYPE_ID) {
                dispatchDetachLayerFromPlot(dl.drawLayerId, plotId);
            }
        }
    });


    if (request.getGridOn() !== GridOnStatus.FALSE) {
        const dl = getDrawLayerByType(dlRoot(), WebGrid.TYPE_ID);
        const useLabels = request.getGridOn() === GridOnStatus.TRUE;
        if (!dl) dispatchCreateDrawLayer(WebGrid.TYPE_ID, {useLabels});
        dispatchAttachLayerToPlot(WebGrid.TYPE_ID, plotId, false);
    }

    if (plot.relatedData) {
        plot.relatedData.forEach((rd) => {
            if (rd.dataType === RDConst.TABLE) {
                const dl = getDrawLayerById(dlRoot(), rd.relatedDataId);
                if (!dl) enableRelatedDataLayer(visRoot(), getPlotViewById(visRoot(), plotId), rd);
            }
        });
    }
}