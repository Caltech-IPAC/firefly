/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {logger} from '../../util/Logger.js';
import ImagePlotCntlr, {makeUniqueRequestKey, IMAGE_PLOT_KEY, dispatchPlotMaskLazyLoad} from '../ImagePlotCntlr.js';
import {
    primePlot,
    getOverlayByPvAndId,
    getPlotViewById,
    getOverlayById,
    hasLocalStretchByteData,
} from '../PlotViewUtil.js';
import {PlotState} from '../PlotState.js';
import {RequestType} from '../RequestType.js';
import {ZoomType} from '../ZoomType.js';
import {clone} from '../../util/WebUtil.js';
import {WebPlot} from '../WebPlot.js';
import {callGetWebPlot} from '../../rpc/PlotServicesJson.js';
import {dispatchAddActionWatcher} from '../../core/MasterSaga';
import {isPlotIdInPvNewPlotInfoAry} from '../PlotViewUtil';
import {changeLocalMaskColor} from 'firefly/visualize/rawData/RawDataOps.js';
import {populateFromHeader} from 'firefly/visualize/task/CreateTaskUtil.js';

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

        const {plotId,imageOverlayId, maskValue, imageNumber, title,fileKey,
               uiCanAugmentTitle= true, maskNumber, relatedDataId, lazyLoad}= rawAction.payload;
        let {color}= rawAction.payload;
        if (!color) color= colorList[maskNumber % colorList.length];



        var payload= {
            fileKey,
            plotId,
            maskValue,
            maskNumber,
            imageNumber,
            color,
            title,
            imageOverlayId,
            uiCanAugmentTitle,
            relatedDataId,
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
                maskCall(vr, dispatcher,payload, color);
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

        maskCall(vr, dispatcher,data);
    };
}

/**
 *
 * @param vr
 * @param dispatcher
 * @param payload
 */
function maskCall(vr, dispatcher, payload) {
    const {plotId,imageOverlayId, maskValue, imageNumber, fileKey, color}= payload;
    const pv= getPlotViewById(vr, plotId);
    const maskRequest= makeMaskRequest(fileKey,imageOverlayId ,pv,maskValue,imageNumber, color);

    callGetWebPlot(maskRequest).then( (wpResult) => processMaskSuccessResponse(dispatcher,payload,wpResult) )
        .catch ( (e) => {
            logger.error(`plot mask error, plotId: ${payload.plotId}`, e);
        });
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
                if (opv.colorAttributes.color!==color && hasLocalStretchByteData(opv.plot)) {
                    dispatchHandled= true;
                    changeLocalMaskColor(opv.plot,color)
                        .then( () => dispatcher(rawAction) );
                }
            }
        }

       !dispatchHandled && dispatcher(rawAction);
        
        // note - rawAction.payload.doReplot - is deprecated
    };
}


/**
 *
 * @param fileKey
 * @param imageOverlayId
 * @param pv
 * @param maskValue
 * @param imageNumber
 * @param color
 * @return {*}
 */
function makeMaskRequest(fileKey, imageOverlayId, pv, maskValue, imageNumber, color) {
    const plot= primePlot(pv);
    const state= plot ? plot.plotState : null;

    var originalRequest= state ? state.getWebPlotRequest(): pv.request;

    const r= originalRequest.makeCopy();
    if (fileKey) {
        r.setRequestType(RequestType.FILE);
        r.setFileName(fileKey);
    }

    r.setMaskBits(maskValue);
    r.setPlotId(imageOverlayId);
    r.setPlotAsMask(true);
    r.setMaskColors([color]);
    if (plot) {
        r.setMaskRequiredWidth(plot.dataWidth);
        r.setMaskRequiredHeight(plot.dataHeight);
    }
    r.setMultiImageIdx(imageNumber);

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

        const plotState= PlotState.makePlotStateWithJson(PlotCreate[0].plotState);
        const imageOverlayId= plotState.getWebPlotRequest().getPlotId();

        const plot= WebPlot.makeWebPlotData(imageOverlayId, undefined, PlotCreate[0], {}, true, undefined, plotState.getWebPlotRequest());
        plot.tileData = undefined;
        const resultPayload= clone(payload, {plot});
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

