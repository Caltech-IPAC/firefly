/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {isEmpty} from 'lodash';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../../Firefly.js';
import {dispatchAddViewer, dispatchViewerMounted, dispatchViewerUnmounted, 
        getMultiViewRoot, getViewer, getLayoutType} from '../MultiViewCntlr.js';
import {MultiImageViewerView} from './MultiImageViewerView.jsx';
import {visRoot} from '../ImagePlotCntlr.js';
import {getDlAry} from '../DrawLayerCntlr.js';

export class MultiImageViewer extends Component {

    constructor(props) {
        super(props);
        this.state= {viewer : null};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillReceiveProps(nextProps) {
        if (this.props.viewerId!==nextProps.viewerId) {
            dispatchAddViewer(nextProps.viewerId,nextProps.canReceiveNewPlots,true);
            dispatchViewerUnmounted(this.props.viewerId);
        }
        this.storeUpdate(nextProps);
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
        dispatchViewerUnmounted(this.props.viewerId);
    }

    componentWillMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate(this.props));
        var {viewerId, canReceiveNewPlots}= this.props;
        dispatchAddViewer(viewerId,canReceiveNewPlots,true);
    }

    storeUpdate(props) {
        var {state}= this;
        var {viewerId}= props;
        var viewer= getViewer(getMultiViewRoot(),viewerId);
        if (viewer!==state.viewer || visRoot()!==state.visRoot || getDlAry() != state.dlAry) {
            if (this.iAmMounted) this.setState({viewer,visRoot:visRoot(),dlAry:getDlAry()});
        }
    }

    render() {
        const {forceRowSize, forceColSize, gridDefFunc,Toolbar,viewerId, insideFlex, closeFunc, canDelete}= this.props;
        const {viewer,visRoot,dlAry}= this.state;
        const layoutType= getLayoutType(getMultiViewRoot(),viewerId);
        if (!viewer || isEmpty(viewer.plotIdAry)) return false;
        return (
            <MultiImageViewerView viewerPlotIds={viewer.plotIdAry}
                                  forceRowSize={forceRowSize}
                                  forceColSize={forceColSize}
                                  gridDefFunc={gridDefFunc}
                                  layoutType={layoutType}
                                  Toolbar={Toolbar}
                                  viewerId={viewerId}
                                  visRoot={visRoot}
                                  dlAry={dlAry}
                                  showWhenExpanded={false}
                                  insideFlex={insideFlex}
                                  closeFunc={closeFunc}
                                  canDelete={canDelete}
            />
        );
    }
}

MultiImageViewer.propTypes= {
    viewerId : PropTypes.string.isRequired,
    canReceiveNewPlots : PropTypes.bool,
    Toolbar : PropTypes.func,
    forceRowSize : PropTypes.number,
    forceColSize : PropTypes.number,
    gridDefFunc : PropTypes.func,
    insideFlex : PropTypes.bool,
    closeFunc : PropTypes.func,
    canDelete :  PropTypes.bool
};

// function gridDefFunc(plotIdAry) : [ {title :string, [plotId:string]}]
//
// the gridDefFunc function will take an array of plot id and return
// an array of objects that contain an optional title and an array of plotIds
// each element of the array should represent a row each plotId a plot in that row,
// an empty element will act as a place holder.

// if gridDefFunc is defined it overrides the forceRowSize and forceColSize parameters.
// forceRowSize is defined if overrides forceColSize parameter.


MultiImageViewer.defaultProps= {
    canReceiveNewPlots : false,
    canDelete : true
};
