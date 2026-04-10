/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {logger} from '../../util/Logger.js';
import ImagePlotCntlr, {
    makeUniqueRequestKey, IMAGE_PLOT_KEY, dispatchPlotMaskLazyLoad, visRoot
} from '../ImagePlotCntlr.js';
import { primePlot, getOverlayByPvAndId, getPlotViewById,
    getOverlayById,
    convertImageIdxToHDU,
} from '../PlotViewUtil.js';
import {PlotState} from '../PlotState.js';
import {RequestType} from '../RequestType.js';
import {ZoomType} from '../ZoomType.js';
import {WebPlot} from '../WebPlot.js';
import {callGetWebPlot} from '../../rpc/PlotServicesJson.js';
import {dispatchAddActionWatcher} from '../../core/MasterSaga';
import {isPlotIdInPvNewPlotInfoAry} from '../PlotViewUtil';
import {changeLocalMaskColorOnOverlayPlotView} from 'firefly/visualize/rawData/RawDataOps.js';
import {makeCubeCtxAry, populateFromHeader} from 'firefly/visualize/task/CreateTaskUtil.js';

const colorList= [
    '#FF0000','#00FF00', '#0000FF', '#91D33D',
    '#AE14E0','#FFC0CB', '#EBAA38', '#F6E942',
    '#00E8FF','#8B572A', '#B8E986', '#4A90E2',
    '#BD10E0','#E0107F', '#B9F81C', '#F19301',
];

function watchForCompletedPlot(action, cancelSelf, params) {
    const {pvNewPlotInfoAry, plotId}= action.payload;

    if (action.type===ImagePlotCntlr.PLOT_IMAGE_FAIL) {
        if (action.payload.plotId===plotId) cancelSelf();
        return params;
    }
    if (!isPlotIdInPvNewPlotInfoAry(pvNewPlotInfoAry,plotId) && plotId!==params.plotId) return params;
    dispatchPlotMaskLazyLoad(params.opv.lazyLoadPayload);
    cancelSelf();
}

/**
 *
 * @param rawAction
 * @return {Function}
 */
export function plotImageMaskActionCreator(rawAction) {
    return (dispatcher,getStore) => {
        var vr= getStore()[IMAGE_PLOT_KEY];

        const {plotId, imageOverlayId, maskValue, hduNumber, title, fileKey,description,
               uiCanAugmentTitle= true, maskNumber, relatedDataId, lazyLoad}= rawAction.payload;
        let {color}= rawAction.payload;
        if (!color) color= colorList[maskNumber % colorList.length];

        const payload= {
            fileKey, plotId, maskValue, maskNumber, hduNumber, color, title,
            imageOverlayId, uiCanAugmentTitle, relatedDataId, description,
            requestKey: makeUniqueRequestKey('overlay')
        };

        const pv= getPlotViewById(vr, plotId);
        if (pv) {
            const plot= primePlot(pv);

            if (lazyLoad || !plot) {
                payload.lazyLoadPayload= {plotId,imageOverlayId};
            }

            dispatcher({type:ImagePlotCntlr.PLOT_MASK_START, payload});

            if (!lazyLoad && !plot) {
                vr= getStore()[IMAGE_PLOT_KEY];
                const pv= getPlotViewById(vr, plotId);
                const opv= getOverlayById(pv, imageOverlayId);
                dispatchAddActionWatcher( {
                    callback: watchForCompletedPlot,
                    params: {plotId, opv},
                    actions: [ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_IMAGE_FAIL]
                } );
            }

            if (!lazyLoad && plot) {
                void maskCall(vr, dispatcher,payload);
            }
        }
    };
}

export function plotImageMaskLazyActionCreator(rawAction) {
    return (dispatcher,getStore) => {
        const {plotId,imageOverlayId }= rawAction.payload;
        dispatcher( { type: ImagePlotCntlr.OVERLAY_PLOT_CHANGE_ATTRIBUTES,
                      payload: { plotId,imageOverlayId, attributes:{visible:true}} });
        const vr= getStore()[IMAGE_PLOT_KEY];
        const opv= getOverlayById(getPlotViewById(vr, plotId), imageOverlayId);
        if (!opv) return;

        const data= {plotId,imageOverlayId,
            color:opv.colorAttributes.color,
            maskValue:opv.maskValue,
            maskNumber:opv.maskNumber,
            imageNumber:opv.imageNumber,
            fileKey:opv.fileKey,
        };

        void maskCall(vr, dispatcher,data);
    };
}

/**
 *
 * @param vr
 * @param dispatcher
 * @param payload
 */
async function maskCall(vr, dispatcher, payload) {
    try {
        const {plotId,imageOverlayId, maskValue, imageNumber, fileKey}= payload;
        const pv= getPlotViewById(vr, plotId);
        const maskRequest= makeMaskRequest(fileKey,imageOverlayId, pv,maskValue,imageNumber);
        const wpResult= await callGetWebPlot(maskRequest);
        processMaskSuccessResponse(dispatcher,payload,wpResult);
    } catch (e) {
        logger.error(`plot mask error, plotId: ${payload.plotId}`, e);
    }
}

export function overlayPlotChangeAttributeActionCreator(rawAction) {
    return (dispatcher,getStore) => {

        let dispatchHandled= false;
        const {plotId,imageOverlayId}= rawAction.payload;
        if (rawAction.payload.attributes?.colorAttributes?.color && imageOverlayId) {
            const vr= getStore()[IMAGE_PLOT_KEY];
            const opv= getOverlayByPvAndId(vr,plotId, imageOverlayId);
            if (opv)  {
                const {color}= rawAction.payload.attributes.colorAttributes;
                if (opv.colorAttributes.color!==color) {
                    dispatchHandled= true;
                    changeLocalMaskColorOnOverlayPlotView(opv,color)
                        .then( () => dispatcher(rawAction) );
                }
            }
        }

       !dispatchHandled && dispatcher(rawAction);
    };
}


/**
 *
 * @param fileKey
 * @param imageOverlayId
 * @param pv
 * @param maskValue
 * @param hduNumber
 * @return {*}
 */
function makeMaskRequest(fileKey, imageOverlayId, pv, maskValue, hduNumber) {
    const plot= primePlot(pv);
    const state= plot ? plot.plotState : null;

    const originalRequest= state ? state.getWebPlotRequest(): pv.request;

    const r= originalRequest.makeCopy();
    if (fileKey) {
        r.setRequestType(RequestType.FILE);
        r.setFileName(fileKey);
    }

    r.setMaskBits(maskValue);
    r.setPlotId(imageOverlayId);
    r.setPlotAsMask(true);
    r.setMultiImageExts(hduNumber);

    //TODO check flip and set handle flip case
    if (state) {
        r.setZoomType(ZoomType.LEVEL);
        r.setInitialZoomLevel(plot.zoomFactor);
    }
    return r;
}



function processMaskSuccessResponse(dispatcher, payload, result) {

    if (result.success) {
        const {PlotCreate, PlotCreateHeader}= result;
        populateFromHeader(PlotCreateHeader, PlotCreate);
        const cubeCtx= makeCubeCtxAry(PlotCreate);

        const plotState= PlotState.makePlotStateWithJson(PlotCreate[0].plotState);
        const imageOverlayId= plotState.getWebPlotRequest().getPlotId();
        const request0= plotState.getWebPlotRequest();

        const resultPayload= {...payload};
        const pv= getPlotViewById(visRoot(),payload.plotId);
        const cubeIndex= convertImageIdxToHDU(pv,pv.primeIdx).cubeIdx;
        const plots= PlotCreate.map( (pc,idx) => {
            const plot=WebPlot.makeWebPlotData({plotId:imageOverlayId, wpInit:pc, asOverlay:true, cubeCtx:cubeCtx[idx], request0});
            plot.tileData = undefined;
            return plot;
        });
        resultPayload.plots=plots;
        resultPayload.cube=PlotCreate.length>1;
        resultPayload.primeIdx= 0;
        resultPayload.plot=resultPayload.plots[cubeIndex];
        dispatcher({type: ImagePlotCntlr.PLOT_MASK, payload: resultPayload});
    }
    else {
        const resultPayload= Object.assign({},payload);
        // todo: add failure stuff to resultPayload here
        resultPayload.briefDescription= result.briefFailReason;
        resultPayload.description= 'Failed- ' + result.userFailReason;
        resultPayload.detailFailReason= result.detailFailReason;
        dispatcher( { type: ImagePlotCntlr.PLOT_MASK_FAIL, payload:resultPayload} );
    }

}

