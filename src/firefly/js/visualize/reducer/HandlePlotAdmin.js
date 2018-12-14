/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import Cntlr, {ExpandType, isImageExpanded} from '../ImagePlotCntlr.js';
import {getPlotViewById} from '../PlotViewUtil.js';
import {clone} from '../../util/WebUtil.js';


export function reducer(state, action) {

    switch (action.type) {
        case Cntlr.API_TOOLS_VIEW  :
            return clone(state,{apiToolsView:action.payload.apiToolsView});

        case Cntlr.CHANGE_ACTIVE_PLOT_VIEW:
            return changeActivePlotView(state,action);
        case Cntlr.CHANGE_EXPANDED_MODE:
            return changeExpandedMode(state,action);
        case Cntlr.CHANGE_MOUSE_READOUT_MODE:
            return changeMouseReadout(state, action);
        case Cntlr.CHANGE_POINT_SELECTION:
            return changePointSelection(state,action);
        case Cntlr.DELETE_PLOT_VIEW:
            return deletePlotView(state,action);

        case Cntlr.WCS_MATCH:
            const {wcsMatchCenterWP,wcsMatchType,mpwWcsPrimId}= action.payload;
            return clone(state,{wcsMatchCenterWP,wcsMatchType,mpwWcsPrimId});
        case Cntlr.EXPANDED_AUTO_PLAY:
            if (state.singleAutoPlay!==action.payload.autoPlayOn) {
                return clone(state,{singleAutoPlay:action.payload.autoPlayOn});
            }
            else {
                return state;
            }
    }
    return state;
}


function changePointSelection(state,action) {
    const {requester,enabled}= action.payload;
    const {pointSelEnableAry}= state;
    if (enabled) {
        if (pointSelEnableAry.includes(requester)) return state;
        return clone(state,{pointSelEnableAry: [...pointSelEnableAry,requester]});
    }
    else {
        if (!pointSelEnableAry.includes(requester)) return state;
        return clone(state,{pointSelEnableAry: pointSelEnableAry.filter( (e) => e!==requester)});
    }
}

function changeMouseReadout(state, action) {

    const fieldKey=action.payload.readoutType;
    const payload = action.payload;
    const newRadioValue = payload.newRadioValue;
    const oldRadioValue = state[fieldKey];
    if (newRadioValue ===oldRadioValue) return state;
    return Object.assign({}, state, {[fieldKey]:newRadioValue});

}

function changeActivePlotView(state,action) {
    const {plotId}= action.payload;
    if (plotId===state.activePlotId) return state;
    const prevActivePlotId= state.activePlotId;
    if (plotId && !getPlotViewById(state,plotId)) return state;

    return clone(state, {prevActivePlotId, activePlotId:action.payload.plotId});
}

function changeExpandedMode(state,action) {
    let {expandedMode}= action.payload;

    if (expandedMode===true) expandedMode= state.previousExpandedMode;
    else if (!expandedMode) expandedMode= ExpandType.COLLAPSE;

    if (expandedMode===state.expandedMode) return state;

    const changes= {expandedMode,singleAutoPlay:false};


    if (isImageExpanded(expandedMode)) { // we are currently expanded, just changing modes, e.g. grid to single
        changes.previousExpandedMode= expandedMode;
        if (state.wcsMatchType) {
            changes.mpwWcsPrimId= state.activePlotId;
        }
    }

    return clone(state, changes);
}


function deletePlotView(state,action) {
    const {plotId}= action.payload;
    if (!state.plotViewAry.find( (pv) => pv.plotId===plotId)) return state;

    state= clone(state, {plotViewAry:state.plotViewAry.filter( (pv) => pv.plotId!==plotId)});
    if (state.activePlotId===plotId) {
        state.activePlotId= get(state,'plotViewAry.0.plotId',null);
    }
    if (state.prevActivePlotId===plotId || state.prevActivePlotId===state.activePlotId) {
        state.prevActivePlotId= null;
    }
    if (state.mpwWcsPrimId===plotId) {
        state.mpwWcsPrimId= state.prevActivePlotId || state.activePlotId || get(state,'plotViewAry.0.plotId',null);
    }
    state.processedTiles= state.processedTiles.filter( (d) => d.plotId!==plotId);// remove old client tile data
    return state;
}
