/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {useEffect, useState} from 'react';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {getActivePlotView, getAllDrawLayersForPlot, primePlot} from '../PlotViewUtil.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {getDlAry} from '../DrawLayerCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {DrawLayerPanelView} from './DrawLayerPanelView.jsx';
import {flux} from '../../Firefly.js';
import {addImageReadoutUpdateListener, lastMouseImageReadout} from '../VisMouseSync';
import {useStoreConnector} from '../../ui/SimpleComponent';

export const DRAW_LAYER_POPUP= 'DrawLayerPopup';

function getDialogBuilder() {
    let popup= null;
    return (div) => {
        if (!popup) {
            popup= (
                <PopupPanel title={<DrawLayerPanelTitle/>} >
                    <DrawLayerPanel/>
                </PopupPanel>
            );
            DialogRootContainer.defineDialog(DRAW_LAYER_POPUP, popup, div);
        }
        return popup;
    };
}

const defaultTitle = 'Layers- ';
export function DrawLayerPanelTitle({}) {
    const [plotTitle] = useStoreConnector(() => primePlot(visRoot())?.title );
    return (plotTitle ? `${defaultTitle}${plotTitle}` : defaultTitle);
}

const dialogBuilder= getDialogBuilder();

export function showDrawingLayerPopup(div) {
    dialogBuilder(div);
    dispatchShowDialog(DRAW_LAYER_POPUP);
}

export function hideDrawingLayerPopup() { dispatchHideDialog(DRAW_LAYER_POPUP); }

function storeUpdate(stateObj,setStateObj)  {
    const activePv= getActivePlotView(visRoot());
    const mouseOverMaskValue= lastMouseImageReadout()?.readoutItems?.imageOverlay?.value ?? 0;

    if (activePv===stateObj.activePv && getDlAry()===stateObj.dlAry &&
        mouseOverMaskValue===stateObj.mouseOverMaskValue) return;

    const dlAry= getDlAry();
    const imageOverlayLength= activePv ? activePv.overlayPlotViews.length : 0;
    const layersLength= activePv ? getAllDrawLayersForPlot(dlAry,activePv.plotId).length : 0;
    const hasLayers= (layersLength + imageOverlayLength)>0;
    hasLayers ? setStateObj({dlAry,activePv, mouseOverMaskValue}) : setTimeout(() => hideDrawingLayerPopup(),0);
}

function DrawLayerPanel() {
    const [stateObj, setStateObj] = useState({dlAry:getDlAry(),activePv:getActivePlotView(visRoot()), mouseOverMaskValue:0});

    useEffect(() => {
        const removeFluxListener= flux.addListener(() => storeUpdate(stateObj,setStateObj));
        const removeMouseListener= addImageReadoutUpdateListener(() => storeUpdate(stateObj,setStateObj));
        return () => {
            removeFluxListener();
            removeMouseListener();
        };
    },[stateObj, setStateObj]);

    if (!stateObj.activePv) return false;
    return (
        <DrawLayerPanelView dlAry={stateObj.dlAry}
                            drawLayerFactory={flux.getDrawLayerFactory()}
                            plotView={stateObj.activePv}
                            mouseOverMaskValue={stateObj.mouseOverMaskValue}
                            dialogId={DRAW_LAYER_POPUP}/>
    );

}

