/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import ImagePlotCntlr, {dispatchChangeActivePlotView, dispatchPlotHiPS,
    visRoot, IMAGE_PLOT_KEY, dispatchRotate, dispatchFlip, dispatchPlotImage,
    ActionScope, dispatchWcsMatch } from '../ImagePlotCntlr.js';
import {getExpandedViewerItemIds, findViewerWithItemId,
    getMultiViewRoot, IMAGE} from '../MultiViewCntlr.js';
import PointSelection from '../../drawingLayers/PointSelection.js';
import {
    dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchDetachLayerFromPlot, DRAWING_LAYER_KEY
} from '../DrawLayerCntlr.js';
import { getPlotViewById, applyToOnePvOrAll, isDrawLayerAttached,
    primePlot, getDrawLayerByType, removeRawDataByPlotView } from '../PlotViewUtil.js';
import {isImage} from '../WebPlot.js';
import {RotateType} from '../PlotState.js';
import {detachSelectAreaRelatedLayers} from '../ui/SelectAreaDropDownView.jsx';
import {getAppOptions} from '../../core/AppDataCntlr';
import {WcsMatchType} from '../ImagePlotCntlr';
import {hasOverlayColorLock, findPlotGroup } from '../PlotViewUtil';
import {onPlotComplete} from '../PlotCompleteMonitor';



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
            if (dl) dispatchDestroyDrawLayer(dl.drawLayerId);
        }
    };
}

/**
 * @param {Action} rawAction
 * @returns {Function}
 */
export function restoreDefaultsActionCreator(rawAction) {
    return (dispatcher, getState) => {
        const vr = getState()[IMAGE_PLOT_KEY];
        const {plotId} = rawAction.payload;
        const {plotViewAry, positionLock} = vr;
        const pv = getPlotViewById(vr, plotId);
        const plotGroup= findPlotGroup(pv.plotGroupId,vr.plotGroupAry);
        const toAll =  hasOverlayColorLock(pv, plotGroup) | positionLock;

        detachSelectAreaRelatedLayers(pv, true);
        const {wcsMatchType:matchType, defNorthUpLock}= getAppOptions();
        applyToOnePvOrAll(toAll, plotViewAry, plotId, false,
            (pv)=> {
                if (vr.plotRequestDefaults[pv.plotId]) {
                    const plot= primePlot(pv);
                    const def= vr.plotRequestDefaults[pv.plotId];
                    const viewerId= findViewerWithItemId(getMultiViewRoot(), pv.plotId, IMAGE);
                    if (isImage(plot)) {
                        if (pv.rotation) {
                            dispatchRotate({plotId: pv.plotId,
                                rotateType: defNorthUpLock ? RotateType.NORTH : RotateType.UNROTATE,
                                actionScope:ActionScope.SINGLE});
                        }
                        if (pv.flipY) dispatchFlip({plotId: pv.plotId, actionScope:ActionScope.SINGLE});
                    }
                    switch (def.plotType) {
                        case 'threeColor' :
                            const rR= def?.wpRequest?.makeCopy();
                            const bR= def?.wpRequest?.makeCopy();
                            const gR= def?.wpRequest?.makeCopy();
                            if (defNorthUpLock) {
                                rR?.setRotateNorth(true);
                                bR?.setRotateNorth(true);
                                gR?.setRotateNorth(true);
                            }
                            dispatchPlotImage({plotId:pv.plotId, viewerId, wpRequest:[rR,bR,gR],
                                threeColor:true, setNewPlotAsActive:false,
                                useContextModifications:false, pvOptions:def.pvOptions});
                            break;
                        case 'image' :
                            const r= def.wpRequest.makeCopy();
                            if (defNorthUpLock) r.setRotateNorth(true);
                            dispatchPlotImage({plotId:pv.plotId, wpRequest:r, setNewPlotAsActive:false,
                                viewerId, useContextModifications:false, pvOptions:def.pvOptions});
                            break;
                        case 'hips' :
                            dispatchPlotHiPS({plotId:pv.plotId, wpRequest:def.wpRequest, setNewPlotAsActive:false,
                                viewerId, pvOptions:def.pvOptions});
                            break;
                    }
                    defNorthUpLock && dispatchRotate({plotId: pv.plotId,
                        rotateType: RotateType.NORTH, actionScope:ActionScope.SINGLE});
                }
            });

        const lockMatch= Boolean(WcsMatchType.get(matchType));
        dispatchWcsMatch({plotId, matchType, lockMatch});
        if (defNorthUpLock && lockMatch) {
            onPlotComplete(pv.plotId).then( (pv) => {
                dispatchRotate({plotId: pv.plotId, rotateType: RotateType.NORTH, actionScope:ActionScope.GROUP});
            });
        }
    };
}

/**
 * @param {Action} rawAction
 * @returns {Function}
 */
export function deletePlotViewActionCreator(rawAction) {
    return (dispatcher, getState) => {
        const vr= getState()[IMAGE_PLOT_KEY];
        const {payload}= rawAction;
        const {plotId}= payload;
        const viewerId= findViewerWithItemId(getMultiViewRoot(), plotId, IMAGE);
        removeRawDataByPlotView(getPlotViewById(vr,plotId));

        if (vr.wcsMatchType && !payload.holdWcsMatch) {
            dispatcher({ type: ImagePlotCntlr.WCS_MATCH, payload: {wcsMatchType:false} });
        }
        dispatcher({type:ImagePlotCntlr.DELETE_PLOT_VIEW, payload: {...payload, viewerId}});
    };
}
