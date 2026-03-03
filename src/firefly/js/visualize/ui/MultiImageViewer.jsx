/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isEmpty,omit} from 'lodash';
import {flux} from '../../core/ReduxFlux.js';
import {
    NewPlotMode, dispatchAddViewer, dispatchViewerUnmounted,
    getMultiViewRoot, getViewer, getLayoutType, findViewerWithItemId, IMAGE,
} from '../MultiViewCntlr.js';
import {MultiImageViewerView} from './MultiImageViewerView.jsx';
import {visRoot, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {getPlotViewById} from '../PlotViewUtil.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {getActivePlotView} from '../PlotViewUtil';


const activeViewerMap= new Map();

function nextState(props, state) {
    const viewer= getViewer(getMultiViewRoot(),props.viewerId);
    if (viewer!==state.viewer || visRoot()!==state.visRoot || getDlAry() !== state.dlAry) {
        return {viewer,visRoot:visRoot(),dlAry:getDlAry()};
    }
    return null;
}



function viewWithIdMounted(itemId) {
    const vId= findViewerWithItemId(getMultiViewRoot(),itemId,IMAGE);
    if (!vId) return false;
    return Boolean(activeViewerMap.get(vId));
}


export class MultiImageViewer extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {viewer : null};
    }


    static getDerivedStateFromProps(props,state) {
        return nextState(props,state);
    }

    componentDidUpdate(prevProps) {
        const {props}= this;
        const {canReceiveNewPlots= NewPlotMode.create_replace.key, controlViewerMounting=true}= props;
        if (this.props.viewerId!==prevProps.viewerId) {
            const {renderTreeId}= this.context;
            if (controlViewerMounting) {
                dispatchAddViewer(props.viewerId, canReceiveNewPlots, IMAGE,true, renderTreeId);
                dispatchViewerUnmounted(prevProps.viewerId);
            }

        }
        const pv= getActivePlotView(visRoot());
        const viewer = getViewer(getMultiViewRoot(), props.viewerId);
        const {rootWidget}= this;
        if (!pv || !viewer || !rootWidget || !viewer.lastActiveItemId) return;
        activeViewerMap.set(this.props.viewerId, Boolean(rootWidget.offsetWidth && rootWidget.offsetHeight));
        if (viewer.lastActiveItemId!==pv.plotId && !viewer.itemIdAry.includes(pv.plotId) && rootWidget.offsetWidth && rootWidget.offsetHeight) {
            setTimeout(() => {
                if (!viewWithIdMounted(pv.plotId) && !viewer.itemIdAry.includes(getActivePlotView(visRoot())?.plotId)) {
                    dispatchChangeActivePlotView(viewer.lastActiveItemId);
                }
            }, 5);
        }

    }

    componentWillUnmount() {
        this.iAmMounted= false;
        const {controlViewerMounting=true, viewerId}= this.props;
        if (this.removeListener) this.removeListener();
        if (controlViewerMounting) dispatchViewerUnmounted(viewerId);
        activeViewerMap.delete(viewerId);
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate());
        const {controlViewerMounting= true, viewerId, canReceiveNewPlots=NewPlotMode.create_replace.key}= this.props;
        const {renderTreeId}= this.context;
        if (controlViewerMounting) dispatchAddViewer(viewerId,canReceiveNewPlots,IMAGE, true, renderTreeId);
    }

    storeUpdate() {
        const ns= nextState(this.props,this.state);
        if (this.iAmMounted && ns) this.setState(ns);
    }

    render() {
        const {viewerId,tableId,gridDefFunc,handleToolbar=true}= this.props;
        const {viewer,visRoot,dlAry}= this.state;
        const layoutType= getLayoutType(getMultiViewRoot(),viewerId,tableId);
        if (!viewer) return false;
        if (isEmpty(viewer.itemIdAry)) {
            if (!gridDefFunc) return false;
            if (isEmpty(gridDefFunc([]))) return false; // it is possible the function will returns some messages
        }
        const newProps= omit(this.props, ['viewerPlotIds']);
        const aId= viewer.itemIdAry.find( (id) => getPlotViewById(visRoot,id));
        const pv= getPlotViewById(visRoot, aId);
        if (!pv) return false;
        return (
            <MultiImageViewerView {...newProps}
                                  viewerPlotIds={viewer.itemIdAry}
                                  layoutType={layoutType}
                                  handleToolbar={handleToolbar}
                                  scrollGrid={viewer?.scroll ?? false}
                                  visRoot={visRoot}
                                  ref={(c) => this.rootWidget= c}
                                  dlAry={dlAry}
            />
        );
    }
}

MultiImageViewer.propTypes= {
    viewerId : PropTypes.string.isRequired,
    canReceiveNewPlots : PropTypes.string,
    Toolbar : PropTypes.func,
    handleToolbar : PropTypes.bool,
    forceRowSize : PropTypes.number,
    forceColSize : PropTypes.number,
    gridDefFunc : PropTypes.func,
    insideFlex : PropTypes.bool,
    closeFunc : PropTypes.func,
    tableId : PropTypes.string,
    controlViewerMounting : PropTypes.bool
};

// function gridDefFunc(plotIdAry) : [ {title :string, plotId:[string]}]
//
// the gridDefFunc function will take an array of plot id and return
// an array of objects that contain an optional title and an array of plotIds
// each element of the array should represent a row each plotId a plot in that row,
// an empty element will act as a place holder.

// if gridDefFunc is defined it overrides the forceRowSize and forceColSize parameters.
// forceRowSize is defined if overrides forceColSize parameter.

MultiImageViewer.contextType= RenderTreeIdCtx;
