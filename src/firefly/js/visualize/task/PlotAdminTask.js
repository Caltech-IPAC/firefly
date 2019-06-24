/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import ImagePlotCntlr, {dispatchChangeActivePlotView, dispatchPlotHiPS,
    visRoot, IMAGE_PLOT_KEY, dispatchRotate, dispatchFlip, dispatchPlotImage } from '../ImagePlotCntlr.js';
import {getExpandedViewerItemIds, findViewerWithItemId,
    getMultiViewRoot, IMAGE} from '../MultiViewCntlr.js';
import PointSelection from '../../drawingLayers/PointSelection.js';
import {dispatchAttachLayerToPlot,
    dispatchCreateDrawLayer,
    dispatchDetachLayerFromPlot,
    DRAWING_LAYER_KEY} from '../DrawLayerCntlr.js';
import { getPlotViewById, applyToOnePvOrAll, findPlotGroup, isDrawLayerAttached,
    primePlot, getDrawLayerByType } from '../PlotViewUtil.js';
import {isHiPS, isImage} from '../WebPlot.js';
import {RotateType} from '../PlotState.js';
import {clone} from '../../util/WebUtil.js';
import {detachSelectAreaRelatedLayers} from '../ui/SelectAreaDropDownView.jsx';
import {dispatchWcsMatch} from '../ImagePlotCntlr';

export function autoPlayActionCreator(rawAction) {
    return (dispatcher) => {
        const {autoPlayOn}= rawAction.payload;
        if (autoPlayOn) {
            if (!visRoot().singleAutoPlay) {
                dispatcher(rawAction);
                const id= window.setInterval( () => {
                    const {singleAutoPlay,activePlotId}= visRoot();
                    if (singleAutoPlay) {

                        const plotIdAry= getExpandedViewerItemIds(getMultiViewRoot());
                        const cIdx= plotIdAry.indexOf(activePlotId);
                        const nextIdx= cIdx===plotIdAry.length-1 ? 0 : cIdx+1;
                        dispatchChangeActivePlotView(plotIdAry[nextIdx]);
                    }
                    else {
                        window.clearInterval(id);
                    }
                },1100);
            }
        }
        else {
            dispatcher(rawAction);
        }
    };
}

const attachAll= (plotViewAry,dl) => plotViewAry.forEach( (pv) => {
    if (!isDrawLayerAttached(dl,pv.plotId)) {
        dispatchAttachLayerToPlot(dl.drawLayerTypeId,pv.plotId,false);
    }
});

const detachAll= (plotViewAry,dl) => plotViewAry.forEach( (pv) => {
    if (isDrawLayerAttached(dl,pv.plotId)) {
        dispatchDetachLayerFromPlot(dl.drawLayerTypeId,pv.plotId,false);
    }
});


export function changePointSelectionActionCreator(rawAction) {
    return (dispatcher,getState) => {
        let store= getState();

        const {plotViewAry}= store[IMAGE_PLOT_KEY];
        const typeId= PointSelection.TYPE_ID;

        dispatcher(rawAction);

        store= getState();
        const enabled= get(rawAction.payload, 'enabled') || store[IMAGE_PLOT_KEY].pointSelEnableAry.length;
        let dl= getDrawLayerByType(store[DRAWING_LAYER_KEY], typeId);
        if (store[IMAGE_PLOT_KEY].pointSelEnableAry.length && enabled) {
            if (!dl) {
                dispatchCreateDrawLayer(typeId);
                dl= getDrawLayerByType(getState()[DRAWING_LAYER_KEY], typeId);
            }
            attachAll(plotViewAry,dl);
        }
        else if (!enabled) {
            detachAll(plotViewAry,dl);
        }
    };
}

/**
 * @param {Action} rawAction
 * @returns {Function}
 */
export function restoreDefaultsActionCreator(rawAction) {
    return (dispatcher, getState) => {
        const vr= getState()[IMAGE_PLOT_KEY];
        const {plotId}= rawAction.payload;
        const {plotViewAry, positionLock}= vr;
        const pv= getPlotViewById(vr,plotId);

        detachSelectAreaRelatedLayers(pv, true);
        applyToOnePvOrAll(positionLock, plotViewAry, plotId, false,
            (pv)=> {
                if (vr.plotRequestDefaults[pv.plotId]) {
                    const plot= primePlot(pv);
                    const def= vr.plotRequestDefaults[pv.plotId];
                    const viewerId= findViewerWithItemId(getMultiViewRoot(), pv.plotId, IMAGE);
                    if (isImage(plot)) {
                        if (pv.rotation) dispatchRotate({plotId: pv.plotId, rotateType: RotateType.UNROTATE});
                        if (pv.flipY) dispatchFlip({plotId: pv.plotId});
                    }
                    switch (def.plotType) {
                        case 'threeColor' :
                            dispatchPlotImage({plotId:pv.plotId,
                                viewerId, wpRequest:[def.redReq,def.greenReq,def.blueReq],
                                threeColor:true, setNewPlotAsActive:false,
                                useContextModifications:false});
                            break;
                        case 'image' :
                            dispatchPlotImage({plotId:pv.plotId, wpRequest:def.wpRequest, setNewPlotAsActive:false,
                                viewerId, useContextModifications:false});
                            break;
                        case 'hips' :
                            dispatchPlotHiPS({plotId:pv.plotId, wpRequest:def.wpRequest, setNewPlotAsActive:false,
                                viewerId});
                            break;
                    }
                }
            });
        dispatchWcsMatch({plotId, matchType:false});
    };
}

/**
 * @param {Action} rawAction
 * @returns {Function}
 */
export function deletePlotViewActionCreator(rawAction) {
    return (dispatcher, getState) => {
        const vr= getState()[IMAGE_PLOT_KEY];
        const viewerId= findViewerWithItemId(getMultiViewRoot(), rawAction.payload.plotId, IMAGE);

        if (vr.wcsMatchType && !rawAction.payload.holdWcsMatch) {
            dispatcher({ type: ImagePlotCntlr.WCS_MATCH, payload: {wcsMatchType:false} });
        }
        dispatcher({type:rawAction.type, payload: clone(rawAction.payload, {viewerId})} );
    };
}
