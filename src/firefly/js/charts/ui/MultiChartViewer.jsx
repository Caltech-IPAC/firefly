/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './ChartPanel.css';

import React, {Component,PropTypes} from 'react';
import {isEmpty, isUndefined} from 'lodash';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../../Firefly.js';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ChartPanel} from './ChartPanel.jsx';
import {MultiItemViewerView} from '../../visualize/ui/MultiItemViewerView.jsx';
import {dispatchAddViewer, dispatchViewerUnmounted, dispatchUpdateCustom,
        getMultiViewRoot, getViewer, getLayoutType,PLOT2D} from '../../visualize/MultiViewCntlr.js';
import {getExpandedChartProps, getChartData} from '../ChartsCntlr.js';
import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {PlotlyChartPanel} from './PlotlyChartPanel.jsx';
import {getToolbarUI} from '../ChartUtil.js';

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
        const {viewerId, expandedMode, closeable} = this.props;
        const {viewer}= this.state;
        const layoutType= getLayoutType(getMultiViewRoot(),viewerId);
        if (!viewer || isEmpty(viewer.itemIdAry)) return false;
        let activeItemId = getViewer(getMultiViewRoot(), viewerId).customData.activeItemId;
        if (isUndefined(activeItemId) || !getChartData(activeItemId)) {
            activeItemId = viewer.itemIdAry[0];
        }

        const onChartSelect = (chartId) => {
            if (chartId !== activeItemId) {
                dispatchUpdateCustom(viewerId, {activeItemId: chartId});
            }
        };

        const makeItemViewer = (chartId) => (
            <div className={chartId === activeItemId ? 'ChartPanel ChartPanel--active' : 'ChartPanel'} onClick={()=>onChartSelect(chartId)}>
                {getChartPanel(chartId)};
            </div>
        );

        const makeItemViewerFull = (chartId) => getChartPanel(chartId);

        const newProps = {
            viewerItemIds: viewer.itemIdAry,
            activeItemId,
            layoutType,
            makeItemViewer,
            makeItemViewerFull
        };

        //console.log('Active chart ID: '+activeItemId);
        return (
        <div className='ChartPanel__container'>
            <div className='ChartPanel__wrapper'>
                {expandedMode ? <MultiChartToolbarExpanded {...{closeable, viewerId, layoutType, activeItemId}}/> : <MultiChartToolbarStandard {...{viewerId, layoutType, activeItemId}}/>}
                <ToolBar expandedMode={expandedMode} chartId={activeItemId}/>

                <div className='ChartPanel__chartarea--withToolbar'>
                    <MultiItemViewerView {...this.props} {...newProps}/>
                </div>
                {expandedMode && closeable && <CloseButton style={{paddingLeft: 10, position: 'absolute', top: 0, left: 0}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
            </div>
        </div>

        );
    }
}

function getChartPanel(chartId) {
    const {chartType} = getChartData(chartId) || {};
    return chartType === 'plot.ly' ?
        <PlotlyChartPanel key={chartId} showToolbar={false} chartId={chartId}/>:
        <ChartPanel key={chartId} showToolbar={false} chartId={chartId}/>;
}

function ToolBar({chartId, expandedMode}) {
    const {chartType} = getChartData(chartId) || {};
    if (chartType === 'plot.ly') {
        const ToolbarUI = getToolbarUI(chartId) || (() => null);
        return <ToolbarUI chartId={chartId} expandable={!expandedMode}/>;
    } else {
        return <ChartPanel key={'toolbar-'+chartId} expandedMode={expandedMode} expandable={!expandedMode}  showChart={false} chartId={chartId}/>;
    }
}

MultiChartViewer.propTypes= {
    viewerId : PropTypes.string.isRequired,
    canReceiveNewItems : PropTypes.string,
    forceRowSize : PropTypes.number,
    forceColSize : PropTypes.number,
    gridDefFunc : PropTypes.func,
    insideFlex : PropTypes.bool,
    closeable : PropTypes.bool,

    expandedMode: PropTypes.bool

};
