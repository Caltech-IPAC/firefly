/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useLayoutEffect} from 'react';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {getActivePlotView, getAllDrawLayersForPlot, primePlot} from '../PlotViewUtil.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {getDlAry} from '../DrawLayerCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {DrawLayerPanelView} from './DrawLayerPanelView.jsx';
import {flux} from '../../core/ReduxFlux.js';
import {lastMouseImageReadout} from '../VisMouseSync';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {useMouseStoreConnector} from './MouseStoreConnector.jsx';

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

function makeState() {
    const activePv= getActivePlotView(visRoot());
    const dlAry= getDlAry();
    const imageOverlayLength= activePv ? activePv.overlayPlotViews.length : 0;
    const layersLength= activePv ? getAllDrawLayersForPlot(dlAry,activePv.plotId).length : 0;

    return {
        dlAry,
        activePv,
        hasLayers: (layersLength + imageOverlayLength)>0,
        mouseOverMaskValue: Number(lastMouseImageReadout()?.readoutItems?.imageOverlay?.valueBase10 || 0),
    };
}


function DrawLayerPanel() {
    const {dlAry,activePv,mouseOverMaskValue,hasLayers}= useMouseStoreConnector(makeState);
    useLayoutEffect(() => void (!hasLayers && setTimeout(() => hideDrawingLayerPopup(),5)), [hasLayers]);

    if (!activePv) return false;
    return (
        <DrawLayerPanelView dlAry={dlAry}
                            drawLayerFactory={flux.getDrawLayerFactory()}
                            plotView={activePv}
                            mouseOverMaskValue={mouseOverMaskValue}
                            dialogId={DRAW_LAYER_POPUP}/>
    );
}

const defaultTitle = 'Layers- ';
export function DrawLayerPanelTitle({}) {
    const plotTitle = useStoreConnector(() => primePlot(visRoot())?.title );
    return (plotTitle ? `${defaultTitle}${plotTitle}` : defaultTitle);
}

const dialogBuilder= getDialogBuilder();

export function showDrawingLayerPopup(div) {
    dialogBuilder(div);
    dispatchShowDialog(DRAW_LAYER_POPUP,div);
}

export function hideDrawingLayerPopup() { dispatchHideDialog(DRAW_LAYER_POPUP); }

