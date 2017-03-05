/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import {logError} from '../../util/WebUtil.js';
import ImagePlotCntlr, {makeUniqueRequestKey, IMAGE_PLOT_KEY, dispatchPlotMask, dispatchZoom, dispatchPlotMaskLazyLoad} from '../ImagePlotCntlr.js';
import {UserZoomTypes} from '../ZoomUtil.js';
import {primePlot, getOverlayByPvAndId, getPlotViewById, getOverlayById} from '../PlotViewUtil.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {PlotState, RotateType} from '../PlotState.js';
import {RequestType} from '../RequestType.js';
import {ZoomType} from '../ZoomType.js';
import {clone} from '../../util/WebUtil.js';
import {WebPlot} from '../WebPlot.js';
import {callGetWebPlot} from '../../rpc/PlotServicesJson.js';
import {take} from 'redux-saga/effects';

const colorList= [
    '#FF0000','#00FF00', '#0000FF', '#91D33D',
    '#AE14E0','#FFC0CB', '#EBAA38', '#F6E942',
    '#00E8FF','#8B572A', '#B8E986', '#4A90E2',
    '#BD10E0','#E0107F', '#B9F81C', '#F19301',
];

function *getColor()  {
    var nextColor=0;
    while (true) {
        yield colorList[nextColor % colorList.length];
        nextColor++;
    }
}
const colorChoose= getColor();

/**
 *
 */
const nextColor= () => colorChoose.next().value;

export function* watchForCompletedPlot(options) {


    let idMatch= false;
    while (!idMatch) {
        const action = yield take([ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_IMAGE_FAIL]);
        const {pvNewPlotInfoAry, plotId}= action.payload;
        idMatch = Boolean(pvNewPlotInfoAry && pvNewPlotInfoAry.find((i) => i.plotId === options.plotId)) ||
                               plotId===options.plotId;
        if (idMatch) {
            if (action.type===ImagePlotCntlr.PLOT_IMAGE) {
                dispatchPlotMaskLazyLoad(options.opv.lazyLoadPayload);
            }
        }
    }

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
        var {color}= rawAction.payload;
        // if (!color) color= nextColor();
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
                dispatchAddSaga( watchForCompletedPlot, {plotId, opv});
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
            color:opv.color,
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
            logError(`plot mask error, plotId: ${payload.plotId}`, e);
        });
}

export function overlayPlotChangeAttributeActionCreator(rawAction) {
    return (dispatcher,getStore) => {
        dispatcher(rawAction);
        if (rawAction.payload.doReplot) {
            const {plotId,imageOverlayId}= rawAction.payload;
            var vr= getStore()[IMAGE_PLOT_KEY];
            var opv= getOverlayByPvAndId(vr,plotId, imageOverlayId);
            if (!opv) return;
            const {imageNumber,color, maskValue, title}= opv;
            var fileKey;
            if (opv.plot) {
                const wpr= opv.plot.plotState.getWebPlotRequest();
                if (wpr.getRequestType()===RequestType.FILE || wpr.getFileName()) {
                    fileKey= wpr.getFileName();
                }
            }
            dispatchPlotMask({plotId,imageOverlayId, fileKey, maskValue, imageNumber, color, title});

            vr= getStore()[IMAGE_PLOT_KEY];
            opv= getOverlayByPvAndId(vr,plotId, imageOverlayId);
            const plot= primePlot();
            if (plot && get(opv, 'plot.zoomFactor') !== plot.zoomFactor) {
               dispatchZoom({
                   plotId:plot.plotId,
                   UserZoomType:UserZoomTypes.LEVEL,
                   level: plot.zoomFactor
               });
            }
        }
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
        // r.setInitialZoomLevel(state.getZoomLevel());
        if (state.isRotated()) {
            const rt= state.getRotateType();
            // r.setMultiImageIdx(0);
            if (rt===RotateType.NORTH) {
                r.setRotateNorth(true);
            }
            else if (rt===RotateType.ANGLE) {
                r.setRotate(true);
                r.setRotationAngle(state.getRotationAngle());
                r.setRotateFromNorth(false);
            }
        }
        else {
            r.setRotate(false);
            r.setRotateNorth(false);
            r.setRotationAngle(0);
        }
        r.setFlipY(state.isFlippedY());
        r.setFlipX(false);//todo handle flip x
    }
    return r;
}



function processMaskSuccessResponse(dispatcher, payload, result) {

    if (result.success) {
        const {PlotCreate}= result;

        const plotState= PlotState.makePlotStateWithJson(PlotCreate[0].plotState);
        const imageOverlayId= plotState.getWebPlotRequest().getPlotId();

        var plot= WebPlot.makeWebPlotData(imageOverlayId, PlotCreate[0], {}, true);
        const resultPayload= clone(payload, {plot});
        dispatcher({type: ImagePlotCntlr.PLOT_MASK, payload: resultPayload});
    }
    else {
        const resultPayload= Object.assign({},payload);
        // todo: add failure stuff to resultPayload here
        resultPayload.briefDescription= result.briefFailReason;
        resultPayload.description= 'Plot Failed- ' + result.userFailReason;
        resultPayload.detailFailReason= result.detailFailReason;
        dispatcher( { type: ImagePlotCntlr.PLOT_MASK_FAIL, payload:resultPayload} );
    }

}

