/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {logError} from '../util/WebUtil.js';
import ImagePlotCntlr, {makeUniqueRequestKey, IMAGE_PLOT_KEY, dispatchPlotMask} from './ImagePlotCntlr.js';
import {primePlot, getOverlayByPvAndId, getPlotViewById} from './PlotViewUtil.js';
import {PlotState, RotateType} from './PlotState.js';
import {RequestType} from './RequestType.js';
import {ZoomType} from './ZoomType.js';
import {clone} from '../util/WebUtil.js';
import {WebPlot} from './WebPlot.js';
import {callGetWebPlot} from '../rpc/PlotServicesJson.js';


function *getColor()  {
    const autoColor= ['#FF0000','#00FF00','#0000FF','#91D33D', '#AE14DB','#FF0000', '#EBAA38', '#F6E942'];
    var nextColor=0;
    while (true) {
        yield autoColor[nextColor % autoColor.length];
        nextColor++;
    }
}
const colorChoose= getColor();

/**
 *
 */
const nextColor= () => colorChoose.next().value;

/**
 *
 * @param rawAction
 * @return {Function}
 */
export function plotImageMaskActionCreator(rawAction) {
    return (dispatcher,getStore) => {
        const vr= getStore()[IMAGE_PLOT_KEY];

        const {plotId,imageOverlayId, maskValue, imageNumber, title,fileKey, maskNumber}= rawAction.payload;
        var {color}= rawAction.payload;
        if (!color) color= nextColor();


        const pv= getPlotViewById(vr, plotId);
        const maskRequest= makeMaskRequest(fileKey,imageOverlayId ,pv,maskValue,imageNumber, color);


        var payload= {
            plotId,
            maskValue,
            maskNumber,
            imageNumber,
            color,
            title,
            imageOverlayId,
            maskRequest,
            requestKey: makeUniqueRequestKey()
        };
        dispatcher({type:ImagePlotCntlr.PLOT_MASK_START, payload});



        callGetWebPlot(maskRequest).then( (wpResult) => processMaskSuccessResponse(dispatcher,payload,wpResult) )
            .catch ( (e) => {
                dispatcher( { type: ImagePlotCntlr.PLOT_MASK_FAIL, payload:  clone(rawAction.payload, {error:e}) } );
                logError(`plot mask error, plotId: ${plotId}`, e);
            });


    };
}

export function overlayPlotChangeAttributeActionCreator(rawAction) {
    return (dispatcher,getStore) => {
        dispatcher(rawAction);
        if (rawAction.payload.doReplot) {
            const {plotId,imageOverlayId}= rawAction.payload;
            const vr= getStore()[IMAGE_PLOT_KEY];
            const opv= getOverlayByPvAndId(vr,plotId, imageOverlayId);
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
        r.setZoomType(ZoomType.STANDARD);
        r.setInitialZoomLevel(state.getZoomLevel());
        if (state.isRotated()) {
            const rt= state.getRotateType();
            r.setMultiImageIdx(0);
            if (rt===RotateType.NORTH) {
                r.setRotateNorth(true);
            }
            else if (rt===RotateType.ANGLE) {
                r.setRotate(true);
                r.setRotationAngle(state.getRotationAngle());
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

