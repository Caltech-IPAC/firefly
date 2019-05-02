/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import {get} from 'lodash';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {getActivePlotView, getAllDrawLayersForPlot, primePlot} from '../PlotViewUtil.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {SimpleComponent} from '../../ui/SimpleComponent.jsx';
import {getDlAry} from '../DrawLayerCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {DrawLayerPanelView} from './DrawLayerPanelView.jsx';
import {flux} from '../../Firefly.js';
import {addImageReadoutUpdateListener, lastMouseImageReadout} from '../VisMouseSync';


export const DRAW_LAYER_POPUP= 'DrawLayerPopup';

function getDialogBuilder() {
    var popup= null;
    return (div) => {
        if (!popup) {
            const popup= (
                <PopupPanel title={<DrawLayerPanelTitle/>} >
                    <DrawLayerPanel/>
                </PopupPanel>
            );
            DialogRootContainer.defineDialog(DRAW_LAYER_POPUP, popup, div);
        }
        return popup;
    };
}

class DrawLayerPanelTitle extends SimpleComponent {
    getNextState(np) {
        return {plotTitle: get(primePlot(visRoot()), 'title')};
    }

    render() {
        const defaultTitle = 'Layers- ';
        const {plotTitle} = this.state;
        return (plotTitle ? `${defaultTitle}${plotTitle}` : defaultTitle);
    }
}

const dialogBuilder= getDialogBuilder();

export function showDrawingLayerPopup(div) {
    dialogBuilder(div);
    dispatchShowDialog(DRAW_LAYER_POPUP);
}

export function hideDrawingLayerPopup() {
    dispatchHideDialog(DRAW_LAYER_POPUP);
}


class DrawLayerPanel extends PureComponent {

    constructor(props) {
        super(props);
        var activePv= getActivePlotView(visRoot());
        this.state= {dlAry:getDlAry(),activePv, mouseOverMaskValue:0};
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        if (this.removeMouseListener) this.removeMouseListener();
    }


    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
        this.removeMouseListener= addImageReadoutUpdateListener(() => this.storeUpdate());
    }

    storeUpdate() {
        var state= this.state;
        var activePv= getActivePlotView(visRoot());
        const mouseOverMaskValue= get(lastMouseImageReadout(),'readoutItems.imageOverlay.value',0);

        if (activePv!==state.activePv  || getDlAry()!==state.dlAry  || mouseOverMaskValue!==this.state.mouseOverMaskValue) {
            const dlAry= getDlAry();
            const imageOverlayLength= activePv ? activePv.overlayPlotViews.length : 0;
            var layers= activePv ? getAllDrawLayersForPlot(dlAry,activePv.plotId) : [];
            if ((layers.length + imageOverlayLength)>0) {
                this.setState({dlAry,activePv, mouseOverMaskValue});
            }
            else {
                setTimeout(() => hideDrawingLayerPopup(),0);
            }
        }
    }

    render() {
        var {activePv}= this.state;
        if (!activePv) return false;
        return (
            <DrawLayerPanelView dlAry={this.state.dlAry}
                                drawLayerFactory={flux.getDrawLayerFactory()}
                                plotView={activePv}
                                mouseOverMaskValue={this.state.mouseOverMaskValue}
                                dialogId={DRAW_LAYER_POPUP}/>
        );
    }

}

