/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {isEmpty} from 'lodash';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../../Firefly.js';

import {MultiItemViewerView} from '../../visualize/ui/MultiItemViewerView.jsx';
import {dispatchAddViewer, dispatchViewerUnmounted, dispatchUpdateCustom,
        getMultiViewRoot, getViewer, getLayoutType,PLOT2D} from '../../visualize/MultiViewCntlr.js';
import {getExpandedChartProps} from '../ChartsCntlr.js';
import {ChartPanel} from './ChartPanel.jsx';
import {MultiChartToolbarStandard, MultiChartToolbarExpanded} from './MultiChartToolbar.jsx';

export class MultiChartViewer extends Component {

    constructor(props) {
        super(props);
        this.state= {viewer : null};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillReceiveProps(nextProps) {
        if (this.props.viewerId!==nextProps.viewerId) {
            dispatchAddViewer(nextProps.viewerId,nextProps.canReceiveNewItems,PLOT2D,true);
            dispatchViewerUnmounted(this.props.viewerId);
        } else if (nextProps.expandedMode && this.props.expandedMode!==nextProps.expandedMode) {
            const {chartId} = getExpandedChartProps();
            dispatchUpdateCustom(nextProps.viewerId, {activeItemId: chartId});
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
        var {viewerId, canReceiveNewItems}= this.props;
        dispatchAddViewer(viewerId,canReceiveNewItems,PLOT2D,true);
        if (this.props.expandedMode) {
            const {chartId} = getExpandedChartProps();
            dispatchUpdateCustom(viewerId, {activeItemId: chartId});
        }
    }

    storeUpdate(props) {
        var {state}= this;
        var {viewerId}= props;
        var viewer= getViewer(getMultiViewRoot(),viewerId);
        if (viewer!==state.viewer) {
            if (this.iAmMounted) this.setState({viewer});
        }
    }

    render() {
        const {Toolbar, viewerId, expandedMode, closeable} = this.props;
        const {viewer}= this.state;
        const layoutType= getLayoutType(getMultiViewRoot(),viewerId);
        const activeItemId = getViewer(getMultiViewRoot(), viewerId).customData.activeItemId;
        if (!viewer || isEmpty(viewer.itemIdAry)) return false;


        const makeItemViewer = (chartId) => (
            <ChartPanel key={chartId} expandable={!expandedMode} chartId={chartId}/>
        );

        const makeItemViewerFull = (chartId) => (
            <ChartPanel key={chartId} expandedMode={expandedMode} expandable={!expandedMode} chartId={chartId}/>
        );

        const makeToolbar = Toolbar ? () => (<Toolbar {...this.props} />) :
            expandedMode ? () => (<MultiChartToolbarExpanded {...{closeable, viewerId, layoutType, activeItemId}}/>) :
                () => (<MultiChartToolbarStandard {...{viewerId, layoutType, activeItemId}}/>);

        const newProps = {
            viewerItemIds: viewer.itemIdAry,
            activeItemId,
            layoutType,
            makeItemViewer,
            makeItemViewerFull,
            makeToolbar
        };

        return (
            <MultiItemViewerView {...this.props} {...newProps}/>
        );
    }
}

MultiChartViewer.propTypes= {
    viewerId : PropTypes.string.isRequired,
    canReceiveNewItems : PropTypes.string,
    Toolbar : PropTypes.func,
    forceRowSize : PropTypes.number,
    forceColSize : PropTypes.number,
    gridDefFunc : PropTypes.func,
    insideFlex : PropTypes.bool,
    closeable : PropTypes.bool,

    expandedMode: PropTypes.bool

};
