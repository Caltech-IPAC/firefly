/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {Divider, Sheet, Stack} from '@mui/joy';
import {isEmpty, isUndefined} from 'lodash';
import {flux} from '../../core/ReduxFlux.js';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ChartPanel} from './ChartPanel.jsx';
import {MultiItemViewerView} from '../../visualize/ui/MultiItemViewerView.jsx';
import {dispatchAddViewer, dispatchViewerUnmounted, dispatchUpdateCustom,
        getMultiViewRoot, getViewer, getLayoutType, PLOT2D} from '../../visualize/MultiViewCntlr.js';
import {getExpandedChartProps, getChartData} from '../ChartsCntlr.js';
import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';

import {MultiChartToolbarStandard, MultiChartToolbarExpanded} from './MultiChartToolbar.jsx';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';

export function getActiveViewerItemId(viewerId, useDefault) {
    const viewer= getViewer(getMultiViewRoot(),viewerId);
    const activeItemId = viewer?.customData?.activeItemId;
    return activeItemId || (useDefault ? viewer?.itemIdAry?.[0] : undefined);
}


function nextState(props, state) {
    const {viewerId}= props;
    const viewer= getViewer(getMultiViewRoot(),viewerId);
    if (viewer!==state.viewer) {
        return {viewer};
    }
    return null;
}

export class MultiChartViewer extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {viewer : getViewer(getMultiViewRoot(), props.viewerId)};
    }

    static getDerivedStateFromProps(props,state) {
        return nextState(props,state);
    }

    componentDidUpdate(prevProps) {
        const {viewerId}= this.props;
        if (prevProps.viewerId!==viewerId) {
            const {renderTreeId}= this.context;
            dispatchAddViewer(viewerId,this.props.canReceiveNewItems,PLOT2D,true,renderTreeId);
            dispatchViewerUnmounted(prevProps.viewerId);
        } else if (this.props.expandedMode && prevProps.expandedMode!==this.props.expandedMode) {
            const {chartId} = getExpandedChartProps();
            dispatchUpdateCustom(viewerId, {activeItemId: chartId});
        }

    }


    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
        dispatchViewerUnmounted(this.props.viewerId);

    }

    componentDidMount() {
        const {viewerId, canReceiveNewItems, expandedMode}= this.props;
        const {renderTreeId}= this.context;
        dispatchAddViewer(viewerId,canReceiveNewItems,PLOT2D,true, renderTreeId);
        if (expandedMode) {
            const {chartId} = getExpandedChartProps();
            dispatchUpdateCustom(viewerId, {activeItemId: chartId});
        }
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }


    storeUpdate() {
        const ns= nextState(this.props,this.state);
        if (this.iAmMounted && ns) this.setState(ns);
    }

    render() {
        const {viewerId, expandedMode, closeable, noChartToolbar, label, showAddChart=false,
            toolbarVariant, useBorder} = this.props;
        const {viewer}= this.state;
        const layoutType= getLayoutType(getMultiViewRoot(),viewerId);
        if (!viewer || isEmpty(viewer.itemIdAry)) {
            if (expandedMode && closeable) {
                return (
                    <Stack flexGrow={1}>
                        <Stack flexGrow={1}>
                        {expandedMode && closeable && <CloseButton style={{paddingLeft: 10, position: 'absolute', top: 0, left: 0}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
                        </Stack>
                    </Stack>
                );
            } else {
                return false;
            }
        }
        let activeItemId = getActiveViewerItemId(viewerId);
        if (isUndefined(activeItemId) || !getChartData(activeItemId)) {
            activeItemId = viewer.itemIdAry[0];
        }
        // if there are more than 1 chart in the viewer, they should be deletable by default
        const deletable = viewer.itemIdAry.length > 1;

        const onChartSelect = (ev,chartId) => {
            if (chartId !== activeItemId) {
                dispatchUpdateCustom(viewerId, {activeItemId: chartId});
            }
            stopPropagation(ev);
        };

        const glass = Boolean(noChartToolbar);

        const makeItemViewer = (chartId) => (
            <Sheet id='chart-item' sx={{height:1, width:1, display:'flex'}}
                   variant='outlined'
                   color={ chartId === activeItemId ? 'warning' : 'neutral'}
                   onClick={(ev)=>onChartSelect(ev,chartId)}
                   onTouchStart={stopPropagation}
                   onMouseDown={stopPropagation}>
                <ChartPanel key={chartId} showToolbar={false} chartId={chartId} deletable={deletable} glass={glass}/>
            </Sheet>
        );

        const makeItemViewerFull = (chartId) => (
            <Stack id='chart-itemFull' onClick={stopPropagation} sx={{height:1, width:1}}
                 onTouchStart={stopPropagation}
                 onMouseDown={stopPropagation}>
                <ChartPanel key={chartId} showToolbar={false} chartId={chartId} deletable={deletable} glass={glass}/>
            </Stack>
        );

        const newProps = {
            viewerItemIds: viewer.itemIdAry,
            activeItemId,
            layoutType,
            makeItemViewer,
            makeItemViewerFull,
            label
        };

        //console.log('Active chart ID: '+activeItemId);

        const ToolBar = expandedMode ? MultiChartToolbarExpanded : MultiChartToolbarStandard;
        const showChartToolbar = !Boolean(noChartToolbar);

        const borderSettings= useBorder ?
            {border: '1px solid', borderColor: 'divider', borderRadius: '5px'} : {};
        return (
            <Stack {...{id:'chart-multiviewer', width:1, height:1, position:'relative', ...borderSettings} }>
                {showChartToolbar &&
                    <>
                        <ToolBar chartId={activeItemId} expandable={!expandedMode} {...{
                            expandedMode,
                            closeable,
                            viewerId,
                            layoutType,
                            activeItemId,
                            toolbarVariant,
                            showAddChart
                        }}/>
                        <Divider orientation='horizontal'/>
                    </>
                }
                <MultiItemViewerView {...this.props} {...newProps}/>
            </Stack>
        );
    }
}


const stopPropagation= (ev) => ev.stopPropagation();

MultiChartViewer.contextType= RenderTreeIdCtx;

MultiChartViewer.propTypes= {
    viewerId : PropTypes.string,
    canReceiveNewItems : PropTypes.string,
    forceRowSize : PropTypes.number,
    forceColSize : PropTypes.number,
    gridDefFunc : PropTypes.func,
    insideFlex : PropTypes.bool,
    closeable : PropTypes.bool,
    expandedMode: PropTypes.bool,
    noChartToolbar: PropTypes.bool,
    toolbarVariant : PropTypes.string,
    label : PropTypes.string,
    showAddChart: PropTypes.bool
};
