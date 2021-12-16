/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Cntlr, {ExpandType} from '../ImagePlotCntlr.js';
import {getPlotViewById, isImageExpanded} from '../PlotViewUtil.js';

export function reducer(state, action) {
    switch (action.type) {
        case Cntlr.API_TOOLS_VIEW:
            return {...state,apiToolsView:action.payload.apiToolsView};
        case Cntlr.CHANGE_ACTIVE_PLOT_VIEW:
            return changeActivePlotView(state,action);
        case Cntlr.CHANGE_EXPANDED_MODE:
            return changeExpandedMode(state,action);
        case Cntlr.CHANGE_MOUSE_READOUT_MODE:
            return changeMouseReadout(state, action);
        case Cntlr.CHANGE_POINT_SELECTION:
            return changePointSelection(state,action);
        case Cntlr.CHANGE_TABLE_AUTO_SCROLL:
            if (state.autoScrollToHighlightedTableRow===action.payload.enabled) return state;
            return {...state, autoScrollToHighlightedTableRow:action.payload.enabled};
        case Cntlr.USE_TABLE_AUTO_SCROLL:
            if (state.useAutoScrollToHighlightedTableRow===action.payload.enabled) return state;
            return {...state, useAutoScrollToHighlightedTableRow:action.payload.useAutoScroll};
        case Cntlr.DELETE_PLOT_VIEW:
            return deletePlotView(state,action);
        case Cntlr.WCS_MATCH:
            const {wcsMatchCenterWP,wcsMatchType,mpwWcsPrimId}= action.payload;
            return {...state,wcsMatchCenterWP,wcsMatchType,mpwWcsPrimId};
        case Cntlr.EXPANDED_AUTO_PLAY:
            if (state.singleAutoPlay===action.payload.autoPlayOn) return state;
            return {...state,singleAutoPlay:action.payload.autoPlayOn};
    }
    return state;
}


function changePointSelection(state,action) {
    const {requester,enabled}= action.payload;
    const {pointSelEnableAry}= state;
    if (enabled) {
        if (pointSelEnableAry.includes(requester)) return state;
        return {...state,pointSelEnableAry: [...pointSelEnableAry,requester]};
    }
    else {
        if (!pointSelEnableAry.includes(requester)) return state;
        return {...state,pointSelEnableAry: pointSelEnableAry.filter( (e) => e!==requester)};
    }
}

function changeMouseReadout(state, action) {
    const {payload} = action;
    const fieldKey=payload.readoutType;
    const oldRadioValue = state[fieldKey];
    if (payload.newRadioValue===oldRadioValue) return state;
    return {...state, [fieldKey]:payload.newRadioValue};
}

function changeActivePlotView(state,action) {
    const {plotId}= action.payload;
    if (plotId===state.activePlotId) return state;
    if (plotId && !getPlotViewById(state,plotId)) return state;
    return {...state, prevActivePlotId:state.activePlotId, activePlotId:plotId};
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
    return {...state, ...changes};
}

function deletePlotView(state,action) {
    const {plotId}= action.payload;
    if (!state.plotViewAry.find( (pv) => pv.plotId===plotId)) return state;

    const nextState= {...state, plotViewAry:state.plotViewAry.filter( (pv) => pv.plotId!==plotId)};
    if (state.activePlotId===plotId) {
        nextState.activePlotId= state.plotViewAry[0]?.plotId;
    }
    if (state.prevActivePlotId===plotId || state.prevActivePlotId===state.activePlotId) {
        nextState.prevActivePlotId= undefined;
    }
    if (state.mpwWcsPrimId===plotId) {
        nextState.mpwWcsPrimId= state.prevActivePlotId || state.activePlotId || state.plotViewAry[0]?.plotId;
    }
    return nextState;
}
