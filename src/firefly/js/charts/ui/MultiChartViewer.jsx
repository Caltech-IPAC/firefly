/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './ChartPanel.css';

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, isUndefined} from 'lodash';
import {flux} from '../../Firefly.js';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ChartPanel} from './ChartPanel.jsx';
import {MultiItemViewerView} from '../../visualize/ui/MultiItemViewerView.jsx';
import {dispatchAddViewer, dispatchViewerUnmounted, dispatchUpdateCustom,
        getMultiViewRoot, getViewer, getLayoutType,PLOT2D} from '../../visualize/MultiViewCntlr.js';
import {getExpandedChartProps, getChartData} from '../ChartsCntlr.js';
import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';

import {MultiChartToolbarStandard, MultiChartToolbarExpanded} from './MultiChartToolbar.jsx';

export function getActiveViewerItemId(viewerId) {
    return getViewer(getMultiViewRoot(), viewerId).customData.activeItemId;
}

export class MultiChartViewer extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {viewer : null};
    }

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
        var {viewerId, canReceiveNewItems, expandedMode}= this.props;
        dispatchAddViewer(viewerId,canReceiveNewItems,PLOT2D,true);
        if (expandedMode) {
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
        let activeItemId = getActiveViewerItemId(viewerId);
        if (isUndefined(activeItemId) || !getChartData(activeItemId)) {
            activeItemId = viewer.itemIdAry[0];
        }

        const onChartSelect = (ev,chartId) => {
            if (chartId !== activeItemId) {
                dispatchUpdateCustom(viewerId, {activeItemId: chartId});
            }
            stopPropagation(ev);
        };


        const makeItemViewer = (chartId) => (
            <div className={chartId === activeItemId ? 'ChartPanel ChartPanel--active' : 'ChartPanel'}
                 onClick={(ev)=>onChartSelect(ev,chartId)}
                 onTouchStart={stopPropagation}
                 onMouseDown={stopPropagation}>
                <ChartPanel key={chartId} showToolbar={false} chartId={chartId}/>
            </div>
        );

        const makeItemViewerFull = (chartId) => (
            <div onClick={stopPropagation}
                 onTouchStart={stopPropagation}
                 onMouseDown={stopPropagation}>
                <ChartPanel key={chartId} showToolbar={false} chartId={chartId}/>
            </div>
        );

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
                <ChartPanel key={'toolbar-'+activeItemId} expandedMode={expandedMode} expandable={!expandedMode}  showChart={false} chartId={activeItemId}/>

                <div className='ChartPanel__chartarea--withToolbar'>
                    <MultiItemViewerView {...this.props} {...newProps}/>
                </div>
                {expandedMode && closeable && <CloseButton style={{paddingLeft: 10, position: 'absolute', top: 0, left: 0}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
            </div>
        </div>

        );
    }
}


const stopPropagation= (ev) => ev.stopPropagation();


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
