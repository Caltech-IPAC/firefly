/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import sCompare from 'react-addons-shallow-compare';
import {getActivePlotView, getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {getDlAry} from '../DrawLayerCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import DrawLayerPanelView from './DrawLayerPanelView.jsx';
import {flux} from '../../Firefly.js';


export const DRAW_LAYER_POPUP= 'DrawLayerPopup';

function getDialogBuilder() {
    var popup= null;
    return () => {
        if (!popup) {
            const popup= (
                <PopupPanel title={'Drawing Layers'} >
                    <DrawLayerPanel/>
                </PopupPanel>
            );
            DialogRootContainer.defineDialog(DRAW_LAYER_POPUP, popup);
        }
        return popup;
    };
}

const dialogBuilder= getDialogBuilder();

export function showDrawingLayerPopup() {
    dialogBuilder();
    dispatchShowDialog(DRAW_LAYER_POPUP);
}

export function hideDrawingLayerPopup() {
    dispatchHideDialog(DRAW_LAYER_POPUP);
}


class DrawLayerPanel extends React.Component {

    constructor(props) {
        super(props);
        var activePv= getActivePlotView(visRoot());
        this.state= {dlAry:getDlAry(),activePv};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
    }


    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        var state= this.state;
        var activePv= getActivePlotView(visRoot());

        if (activePv!==state.activePv  || getDlAry()!==state.dlAry) {
            const dlAry= getDlAry();
            var layers= getAllDrawLayersForPlot(dlAry,activePv.plotId);
            if (layers.length) {
                this.setState({dlAry,activePv});
            }
            else {
                setTimeout(() => hideDrawingLayerPopup(),0)
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
                                dialogId={DRAW_LAYER_POPUP}/>
        );
    }

}

