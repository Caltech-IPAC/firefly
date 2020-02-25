/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {take} from 'redux-saga/effects';
import {omit,pick} from 'lodash';
import shallowequal from 'shallowequal';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {getAllDrawLayersForPlot,getActivePlotView, primePlot} from '../PlotViewUtil.js';
import {flux} from '../../Firefly.js';
import {VisToolbarViewWrapper} from './VisToolbarView.jsx';
import {dispatchShowDialog,dispatchHideDialog, isDialogVisible} from '../../core/ComponentCntlr.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {LayoutType, PopupPanel} from '../../ui/PopupPanel.jsx';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {isHiPS} from '../WebPlot.js';
import {getPreference} from '../../core/AppDataCntlr';
import {TARGET_LIST_PREF} from './ImageCenterDropDown';

const omList= ['plotViewAry'];
const pvPickList= ['plotViewCtx','primeIdx', 'flipY'];



export const ToolTipCtx = React.createContext( {
    tipOnCB : undefined,
    tipOffCB : undefined,
});


export class VisToolbar extends PureComponent {
    constructor(props) {
        super(props);
        this.state= {visRoot:visRoot(), dlCount:0, tip:''};
        this.tipOn= (tip) => this.setState({tip});
        this.tipOff= () => this.setState({tip:null});
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }


    /**
     * If the object changed then check if any of the following changed.
     *  - drawing layer count
     *  - the part of visRoot that is not the plotViewAry
     *  - active plot id
     *  - the plotViewCtx in the active plot view
     *  This is a optimization so that the toolbar does not re-render every time the the plot scrolls
     */
    storeUpdate() {
        const vr= visRoot();
        let dlCount= 0;
        const newPv= getActivePlotView(vr);
        const oldPv= getActivePlotView(this.state.visRoot);
        const recentTargetAry= getPreference(TARGET_LIST_PREF, []);
        if (vr.activePlotId) {
            dlCount= getAllDrawLayersForPlot(getDlAry(),vr.activePlotId).length + newPv.overlayPlotViews.length;
        }
        const tAryIsEqual= shallowequal(recentTargetAry, this.state.recentTargetAry);

        if (vr===this.state.visRoot && dlCount===this.state.dlCount && tAryIsEqual) return;

        let needsUpdate= dlCount!==this.state.dlCount || !tAryIsEqual;
        if (!needsUpdate) needsUpdate= vr.activePlotId!==this.state.visRoot.activePlotId;

        if (!needsUpdate) needsUpdate= !shallowequal(omit(vr,omList),omit(this.state.visRoot,omList));

        if (!needsUpdate) {
            if (oldPv===newPv) return;
            needsUpdate= !shallowequal(pick(oldPv,pvPickList),pick(newPv,pvPickList));
        }

        if (!needsUpdate) {
            const oldPlot = primePlot(oldPv);
            const newPlot = primePlot(newPv);
            needsUpdate = oldPlot?.zoomFactor !== newPlot?.zoomFactor;

            if (!needsUpdate && isHiPS(oldPlot) && isHiPS(newPlot)) {
                needsUpdate = (oldPlot.hipsUrl !== newPlot.hipsUrl) || (oldPlot.imageCoordSys !== newPlot.imageCoordSys);
            }
        }


        if (needsUpdate && this.iAmMounted) {
            this.setState({visRoot:visRoot(), dlCount, recentTargetAry});
        }
    }

    render() {
        const {messageUnder, style}= this.props;
        const {visRoot,tip,dlCount, recentTargetAry}= this.state;
        return (
            <ToolTipCtx.Provider value={{tipOnCB: this.tipOn, tipOffCB: this.tipOff}}>
                <VisToolbarViewWrapper visRoot={visRoot} toolTip={tip} dlCount={dlCount}
                                       messageUnder={messageUnder} style={style} recentTargetAry={recentTargetAry}/>
            </ToolTipCtx.Provider>
        );
    }


}

VisToolbar.propTypes= {
    messageUnder : PropTypes.bool,
    style : PropTypes.object
};

VisToolbar.defaultProps= {
    messageUnder : false
};



export function showTools(element) {
    if (!isDialogVisible('PopupToolbar')) {
        const popup= (
            <PopupPanel title={'Tools'} layoutPosition={LayoutType.TOP_LEFT} >
                <VisToolbar messageUnder={true}/>
            </PopupPanel>
        );
        DialogRootContainer.defineDialog('PopupToolbar', popup, element);
        dispatchShowDialog('PopupToolbar');
        dispatchAddSaga(autoClose);
    }
}

function *autoClose() {
    yield take([ImagePlotCntlr.CHANGE_EXPANDED_MODE]);
    dispatchHideDialog('PopupToolbar');
}

