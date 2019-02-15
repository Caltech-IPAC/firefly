/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {isEqual, isEmpty} from 'lodash';

import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {PLOT2D, DEFAULT_PLOT2D_VIEWER_ID, dispatchAddViewerItems, dispatchRemoveViewerItems, dispatchUpdateCustom, getViewerItemIds, getMultiViewRoot} from '../../visualize/MultiViewCntlr.js';
import {monitorChanges, findGroupByTblId, getActiveTableId, isFullyLoaded} from '../../tables/TableUtil.js';
import {TBL_RESULTS_ACTIVE, TABLE_LOADED} from '../../tables/TablesCntlr';
import {CHART_ADD, CHART_REMOVE, getChartIdsInGroup, getChartData, dispatchChartAdd} from '../ChartsCntlr.js';
import {getDefaultChartProps} from '../ChartUtil.js';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ChartPanel} from './ChartPanel.jsx';
import {MultiChartViewer, getActiveViewerItemId} from './MultiChartViewer.jsx';



// DEFAULT_PLOT2D_VIEWER_ID, 'main'
function watchTblGroup(viewerId, tblGroup, addDefaultChart) {
    return () => {
        if (!tblGroup) return;
        const accept = (a) => {
            const {tbl_id, tbl_group, chartId} = a.payload;
            return chartId || tblGroup === (tbl_group || findGroupByTblId(tbl_id));
        };
        const actions = [CHART_ADD, CHART_REMOVE, TBL_RESULTS_ACTIVE];
        if (addDefaultChart) actions.push(TABLE_LOADED);
        return monitorChanges(actions, accept, updateViewer(viewerId, tblGroup), `wtg-${viewerId}-${tblGroup}`);
    };
}

function updateViewer(viewerId, tblGroup) {
    return (action) => {
        switch (action.type) {
            case TABLE_LOADED:
                const {tbl_id} = action.payload;
                ensureDefaultChart(tbl_id);
                break;
            case TBL_RESULTS_ACTIVE:
            case CHART_ADD:
            case CHART_REMOVE:
                action.type === TBL_RESULTS_ACTIVE && ensureDefaultChart(action.payload.tbl_id);
                const {chartId} = action.payload;
                doUpdateViewer(viewerId, tblGroup, chartId);
        }
    };
}

function ensureDefaultChart(tbl_id) {
    if (getChartIdsInGroup(tbl_id).length === 0) {
        const defaultChartProps = getDefaultChartProps(tbl_id);
        if (!isEmpty(defaultChartProps))  {
            // default chart
            dispatchChartAdd({
                chartId: 'default-' + tbl_id,
                chartType: 'plot.ly',
                groupId: tbl_id,
                ...defaultChartProps
            });
        }
    }
}

function doUpdateViewer(viewerId, tblGroup, chartId) {
    const tblId = getActiveTableId(tblGroup);
    const chartIds = [];
    chartIds.push(...getChartIdsInGroup(tblId), ...getChartIdsInGroup('default'));
    const currentIds = getViewerItemIds(getMultiViewRoot(), viewerId);
    if (!isEqual(chartIds, currentIds)) {
        dispatchRemoveViewerItems(viewerId, currentIds);
        dispatchAddViewerItems(viewerId, chartIds, PLOT2D);

        // update active chart
        if (chartId && isEmpty(getChartData(chartId))) {
            //removed chart - do not change active chartId unless
            if (getActiveViewerItemId(viewerId) === chartId) {
                dispatchUpdateCustom(viewerId, {activeItemId: chartIds[0]});
            }
        } else {
            const activeItemId = chartIds.includes(chartId) ? chartId : chartIds[0];
            dispatchUpdateCustom(viewerId, {activeItemId});
        }
    }
}


/**
 * Default viewer
 * When tbl_group is defined, only the charts related to the active chart in this table group are displayed
 * When addDefaultChart is true, a default chart is created for each table in the group
 */
export class ChartsContainer extends PureComponent {
    constructor(props) {
        super(props);

    }

    UNSAFE_componentWillMount() {
        const {viewerId=DEFAULT_PLOT2D_VIEWER_ID, tbl_group, addDefaultChart, chartId} = this.props;
        if (tbl_group) {
            if (addDefaultChart) {
                const tbl_id = getActiveTableId(tbl_group);
                if (isFullyLoaded(tbl_id)) {
                    ensureDefaultChart(tbl_id);
                }
            }
            // make sure the viewer is updated with related charts on start
            // important when we use external viewer
            doUpdateViewer(viewerId, tbl_group, chartId);
        }
    }

    componentDidMount() {
        const {viewerId=DEFAULT_PLOT2D_VIEWER_ID, tbl_group, addDefaultChart} = this.props;

        const monitor = watchTblGroup(viewerId, tbl_group, addDefaultChart);
        this.removeMonitor = monitor();
    }

    componentWillUnmount() {
        this.removeMonitor && this.removeMonitor();
        this.removeMonitor = undefined;
    }

    render() {
        const {chartId, expandedMode, closeable, viewerId=DEFAULT_PLOT2D_VIEWER_ID} = this.props;


        if (chartId) {
            return expandedMode ?
                <ExpandedView key='chart-expanded' closeable={closeable} chartId={chartId}/> :
                <ChartPanel key={chartId} expandable={true} chartId={chartId}/>;
        } else {
            return (
                <MultiChartViewer {...{closeable, viewerId : viewerId, expandedMode}}/>
            );
        }
    }
}

ChartsContainer.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool,
    chartId: PropTypes.string,
    viewerId : PropTypes.string,
    tbl_group : PropTypes.string,
    addDefaultChart : PropTypes.bool
};

function ExpandedView(props) {
    const {closeable, chartId} = props;

    return (
        <div style={{position: 'absolute', top: 0, left: 0, right: 0, bottom: 0}}>
            <ChartPanel key={'expanded-'+chartId} expandedMode={true} expandable={false} chartId={chartId}/>
            {closeable && <CloseButton style={{position: 'absolute', top: 0, left: 0, paddingLeft: 10}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>}
        </div>
    );
}

ExpandedView.propTypes = {
    closeable: PropTypes.bool,
    chartId: PropTypes.string
};
