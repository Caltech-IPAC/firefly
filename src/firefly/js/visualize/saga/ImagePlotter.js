/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {unionWith, isEmpty} from 'lodash';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr.js';
import {modifyRequest, processPlotImageSuccessResponse} from '../PlotImageTask.js';
import {callGetWebPlot, callGetWebPlot3Color} from '../../rpc/PlotServicesJson.js';
import {getPlotViewById} from '../PlotViewUtil.js';
import {Band} from '../Band.js';
import {requiresWidthHeight} from '../ZoomType.js';
import {logError} from '../../util/WebUtil.js';
import {flux} from '../../Firefly.js';


/**
 * this saga does the following:
 * <ul>
 *     <li>watches to PLOT_IMAGE_START, it the action needs with and height and it is not available, it stores the action
 *     <li>when width/height is available the action is continued
 *     <li>when a plot is set to go it dispatches the server calls
 * </ul>
 *
 */
export function* imagePlotter() {

    var waitingPlotActions= [];

    while (true) {
        var action= yield take([ImagePlotCntlr.PLOT_IMAGE_START,ImagePlotCntlr.UPDATE_VIEW_SIZE]);
        const {plotId}= action.payload;
        var pv= getPlotViewById(visRoot(),plotId);
        switch (action.type) {
            case ImagePlotCntlr.PLOT_IMAGE_START:
                if (canContinue(action,pv)) {
                    continuePlotting(makeContinueAction(action,pv),flux.getRedux().dispatch);
                }
                else {
                    waitingPlotActions= unionWith(waitingPlotActions,[action], 
                                                   (a1,a2) => a1.payload.plotId===a2.payload.plotId);
                }
                break;

            case ImagePlotCntlr.UPDATE_VIEW_SIZE:
                const waitAction= waitingPlotActions.find( (a) => a.payload.plotId===plotId);
                if (waitAction) {
                    continuePlotting(makeContinueAction(waitAction,pv),flux.getRedux().dispatch);
                    waitingPlotActions= waitingPlotActions.filter( (a) => a.payload.plotId!==plotId);
                }
                break;
        }
    }
}


/**
 * 
 * @param rawAction
 * @param pv
 * @return {*}
 */
function canContinue(rawAction,pv) {
    var {wpRequest,threeColor, redReq, greenReq, blueReq}= rawAction.payload;
    var {viewDim:{width,height}}= pv;
    var requiresWH= threeColor ? requiresWidthHeight(redReq || greenReq || blueReq) :
                                 requiresWidthHeight(wpRequest.getZoomType());
    
    if (requiresWH) return width && height;
    else return true;
}

/**
 * 
 * @param rawAction
 * @param pv
 * @return {*}
 */
function makeContinueAction(rawAction,pv) {
    var {wpRequest,redReq, greenReq, blueReq}= rawAction.payload;
    var {viewDim:{width,height}}= pv;
    redReq= addWH(redReq,width,height);
    greenReq=addWH(greenReq,width,height);
    blueReq= addWH(blueReq,width,height);
    wpRequest= addWH(wpRequest,width,height);
    const payload= Object.assign({},rawAction.payload, {wpRequest, redReq, greenReq, blueReq});
    return Object.assign({}, rawAction,{payload});
}

/**
 * 
 * @param r
 * @param {number} w
 * @param {number} h
 * @return {*}
 */
function addWH(r,w,h) {
    if (!r) return;
    r= r.makeCopy();
    r.setZoomToWidth(w);
    r.setZoomToHeight(h);
    return r;
}


/**
 * 
 * @param rawAction
 * @param dispatcher
 */
function continuePlotting(rawAction, dispatcher) {
    var {plotId,wpRequest,threeColor, redReq, greenReq, blueReq}= rawAction.payload;

    if (rawAction.payload.useContextModifications) {
        var pv= getPlotViewById(visRoot(),plotId);
        if (pv) {
            var {plotViewCtx}= pv;
            if (wpRequest && !Array.isArray(wpRequest)) {
                wpRequest= modifyRequest(plotViewCtx,wpRequest,Band.NO_BAND);
            }
            if (redReq) redReq= modifyRequest(plotViewCtx,redReq,Band.RED);
            if (greenReq) greenReq= modifyRequest(plotViewCtx,greenReq,Band.GREEN);
            if (blueReq) blueReq= modifyRequest(plotViewCtx,blueReq,Band.BLUE);
        }
    }

    var p= threeColor ? callGetWebPlot3Color(redReq,greenReq,blueReq) : callGetWebPlot(wpRequest);

    p.then( (wpResult) => processPlotImageSuccessResponse(dispatcher,rawAction.payload,wpResult) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.PLOT_IMAGE_FAIL, payload: {plotId, error:e} } );
            logError(`plot error, plotId: ${plotId}`, e);
        });
}


