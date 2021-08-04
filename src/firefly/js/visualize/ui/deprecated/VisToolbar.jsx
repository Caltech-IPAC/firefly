/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {memo, useState} from 'react';
import PropTypes from 'prop-types';
import {omit} from 'lodash';
import shallowequal from 'shallowequal';
import ImagePlotCntlr, {visRoot} from '../../ImagePlotCntlr.js';
import {getDlAry} from '../../DrawLayerCntlr.js';
import {getAllDrawLayersForPlot,getActivePlotView} from '../../PlotViewUtil.js';
import {VisToolbarViewWrapper} from '../VisToolbarView.jsx';
import {dispatchShowDialog,dispatchHideDialog, isDialogVisible} from '../../../core/ComponentCntlr.js';
import DialogRootContainer from '../../../ui/DialogRootContainer.jsx';
import {LayoutType, PopupPanel} from '../../../ui/PopupPanel.jsx';
import {getPreference} from '../../../core/AppDataCntlr.js';
import {TARGET_LIST_PREF} from '../ImageCenterDropDown.jsx';
import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {dispatchAddActionWatcher} from '../../../core/MasterSaga.js';
import {pvEqualExScroll} from '../../PlotViewUtil.js';

const omList= ['plotViewAry'];

export const ToolTipCtx = React.createContext( { tipOnCB : undefined, tipOffCB : undefined});

/**
 * Return a new State if some values have changed in the store. If critical check show nothing has changed
 * return the old state. If the old state if returned the component will not update.
 * Check if any of the following changed.
 *  - drawing layer count
 *  - the part of visRoot that is not the plotViewAry
 *  - active plot id
 *  - the plotViewCtx in the active plot view
 *  This is a optimization so that the toolbar does not re-render every time the the plot scrolls
 *  @param {Object} oldState
 */
function getStoreState(oldState) {
    const vr= visRoot();
    const {activePlotId}= vr;
    const newPv= getActivePlotView(vr);
    const recentTargetAry= getPreference(TARGET_LIST_PREF, []);
    const dlCount= activePlotId ?
            getAllDrawLayersForPlot(getDlAry(),activePlotId).length + (newPv?.overlayPlotViews?.length??0)  : 0;

    const newState= {visRoot:vr, dlCount, recentTargetAry};
    if (!oldState) return newState;

       // -- if old state is passed, then do some comparisons to see if the state needs to update updated
    const tAryIsEqual= shallowequal(recentTargetAry, oldState?.recentTargetAry);

    if (vr===oldState.visRoot && dlCount===oldState.dlCount && tAryIsEqual) return oldState; // nothing has changed

    let needsUpdate= dlCount!==oldState.dlCount || !tAryIsEqual || activePlotId!==oldState.visRoot.activePlotId;
    if (!needsUpdate) needsUpdate= !shallowequal(omit(vr,omList),omit(oldState.visRoot,omList));

    const oldPv= getActivePlotView(oldState.visRoot);
    if (!needsUpdate) needsUpdate= !pvEqualExScroll(oldPv,newPv);
    
    return (needsUpdate) ? newState : oldState;
}


export const VisToolbar = memo( ({messageUnder= false, style}) => {
    const [tip, setTip] = useState(undefined);

        // put the context in state so we don't have to recreate it everytime, save on toolbar buttons renders
    const [ctxBase] = useState({tipOnCB: (tip) => setTip(tip), tipOffCB: () => setTip(undefined)});
    const [{visRoot,dlCount, recentTargetAry}] = useStoreConnector(getStoreState);

    return (
        <ToolTipCtx.Provider value={ctxBase}>
            <VisToolbarViewWrapper visRoot={visRoot} toolTip={tip} dlCount={dlCount}
                                   messageUnder={messageUnder} style={style} recentTargetAry={recentTargetAry}/>
        </ToolTipCtx.Provider>
    );
});

VisToolbar.propTypes= {
    messageUnder : PropTypes.bool,
    style : PropTypes.object
};


// <ToolTipCtx.Provider value={{tipOnCB: (tip) => setTip(tip), tipOffCB: () => setTip(undefined)}}>

export function showTools(element) {
    if (isDialogVisible('PopupToolbar')) return;
    const popup= (
        <PopupPanel title={'Tools'} layoutPosition={LayoutType.TOP_LEFT} >
            <VisToolbar messageUnder={true}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('PopupToolbar', popup, element);
    dispatchShowDialog('PopupToolbar');
    dispatchAddActionWatcher({
        actions:[ImagePlotCntlr.CHANGE_EXPANDED_MODE],
        callback: (action, cancelSelf) => {
            dispatchHideDialog('PopupToolbar');
            cancelSelf();
        }
    });
}
