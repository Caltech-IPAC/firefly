/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import {bool, number, string, func, elementType} from 'prop-types';
import {isEmpty} from 'lodash';
import {flux} from '../../core/ReduxFlux.js';
import {
    dispatchAddViewer, dispatchViewerUnmounted,
    getMultiViewRoot, getViewer, getLayoutType, findViewerWithItemId,
} from '../MultiViewCntlr.js';
import {MultiImageViewerView} from './MultiImageViewerView.jsx';
import {dispatchChangeActivePlotView} from '../ImagePlotDispatch';
import {IMAGE, NewPlotMode} from '../VisConst';
import {getDlAry, visRoot} from '../VisStoreRoots';
import {currentP, getPlotViewById, getPlotViewProxyById} from '../PlotViewUtil.js';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';


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


export class MultiImageViewer extends PureComponent {//todo: turn this into a functional component

    constructor(props) {
        super(props);
        this.state= {viewer : undefined};
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
        const {pv}= currentP();
        const viewer = getViewer(getMultiViewRoot(), props.viewerId);
        const {rootWidget}= this;
        if (!pv || !viewer || !rootWidget || !viewer.lastActiveItemId) return;
        activeViewerMap.set(this.props.viewerId, Boolean(rootWidget.offsetWidth && rootWidget.offsetHeight));
        if (viewer.lastActiveItemId!==pv.plotId && !viewer.itemIdAry.includes(pv.plotId) && rootWidget.offsetWidth && rootWidget.offsetHeight) {
            setTimeout(() => {
                if (!viewWithIdMounted(pv.plotId) && !viewer.itemIdAry.includes(currentP().plotId)) {
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
        if (isEmpty(viewer?.itemIdAry)) {
            if (!gridDefFunc) return false;
            if (isEmpty(gridDefFunc([]))) return false; // it is possible the function will return some messages
        }
        if (!viewer?.itemIdAry.find( (id) => getPlotViewById(visRoot,id) || getPlotViewProxyById(visRoot,id))) {
            return false;
        } //make sure a least one id has a PlotView
        return (
            <MultiImageViewerView {...{
                ...this.props, handleToolbar, visRoot, dlAry,
                layoutType: getLayoutType(getMultiViewRoot(),viewerId,tableId),
                viewerPlotIds: viewer.itemIdAry,
                scrollGrid: viewer.scroll,
                ref: (c) => this.rootWidget= c,
            }}
            />
        );
    }
}

MultiImageViewer.propTypes= {
    viewerId : string.isRequired,
    canReceiveNewPlots : string,
    Toolbar : func,
    Legend : func,
    PlotViewProxy: elementType,
    handleToolbar : bool,
    forceRowSize : number,
    forceColSize : number,
    gridDefFunc : func,
    insideFlex : bool,
    closeFunc : func,
    tableId : string,
    controlViewerMounting : bool
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
