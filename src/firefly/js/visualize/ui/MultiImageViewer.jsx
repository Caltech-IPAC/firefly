/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {isEmpty} from 'lodash';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../../Firefly.js';
import {dispatchAddViewer, getMultiViewRoot, getViewer, getLayoutType} from '../MultiViewCntlr.js';
import {MultiImageViewerView} from './MultiImageViewerView.jsx';
import {visRoot} from '../ImagePlotCntlr.js';

export class MultiImageViewer extends Component {

    constructor(props) {
        super(props);
        this.state= {viewer : null};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
    }

    componentWillReceiveProps(nextProps) {
        this.storeUpdate(nextProps);
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate(this.props));
        var {viewerId, canAdd}= this.props;
        dispatchAddViewer(viewerId,canAdd);
    }

    storeUpdate(props) {
        var {state}= this;
        var {viewerId}= props;
        var viewer= getViewer(getMultiViewRoot(),viewerId);
        if (viewer!==state.viewer || visRoot()!==state.visRoot) {
            this.setState({viewer,visRoot:visRoot()});
        }
    }

    render() {
        const {viewer}= this.state;
        const {forceRowSize, forceColSize, gridDefFunc,Toolbar,viewerId}= this.props;
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
                                  visRoot={this.state.visRoot}
            />
        );
    }
}

MultiImageViewer.propTypes= {
    viewerId : PropTypes.string.isRequired,
    canAdd : PropTypes.bool,
    Toolbar : PropTypes.func,
    forceRowSize : PropTypes.number,
    forceColSize : PropTypes.number,
    gridDefFunc : PropTypes.func
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
    canAdd : false
};
