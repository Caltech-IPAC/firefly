/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {isEqual, isEmpty} from 'lodash';

import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {PLOT2D, DEFAULT_PLOT2D_VIEWER_ID, dispatchAddViewerItems, dispatchRemoveViewerItems, dispatchUpdateCustom, getViewerItemIds, getMultiViewRoot, dispatchChangeViewerLayout} from '../../visualize/MultiViewCntlr.js';
import {monitorChanges, findGroupByTblId, getActiveTableId, isFullyLoaded, } from '../../tables/TableUtil.js';
import {TBL_RESULTS_ACTIVE, TABLE_LOADED, TABLE_SELECT} from '../../tables/TablesCntlr';
import {CHART_ADD, CHART_REMOVE, getChartIdsInGroup, getChartData, dispatchChartAdd, getExpandedChartProps} from '../ChartsCntlr.js';
import {getDefaultChartProps, allowPinnedCharts} from '../ChartUtil.js';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ChartPanel, ChartToolbar} from './ChartPanel.jsx';
import {MultiChartViewer, getActiveViewerItemId} from './MultiChartViewer.jsx';
import {PinnedChartContainer} from 'firefly/charts/ui/PinnedChartContainer.jsx';
import {Stack} from '@mui/joy';



// DEFAULT_PLOT2D_VIEWER_ID, 'main'
function watchTblGroup(viewerId, tblGroup, addDefaultChart) {
    if (!tblGroup) return;
    const accept = (a) => {
        const {tbl_id, tbl_group, chartId} = a.payload;
        return chartId || tblGroup === (tbl_group || findGroupByTblId(tbl_id));
    };
    const actions = [CHART_ADD, CHART_REMOVE, TBL_RESULTS_ACTIVE];
    if (addDefaultChart) actions.push(TABLE_LOADED, TABLE_SELECT);
    return monitorChanges(actions, accept, updateViewer(viewerId, tblGroup), `wtg-${viewerId}-${tblGroup}`);
}

function updateViewer(viewerId, tblGroup) {
    return (action) => {
        switch (action.type) {
            case TABLE_LOADED:
            case TABLE_SELECT:
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

/**
 * Ensure there's a chart for the given tbl_id.  If not, create a default one.
 * @param tbl_id
 * @return {string} return the chartId for the given tbl_id
 */
export function ensureDefaultChart(tbl_id) {
    const chartIds = getChartIdsInGroup(tbl_id);
    if (chartIds.length === 0) {
        const defaultChartProps = getDefaultChartProps(tbl_id);
        if (!isEmpty(defaultChartProps))  {
            // default chart
            const chartId = 'default-' + tbl_id;
            dispatchChartAdd({
                chartId,
                chartType: 'plot.ly',
                groupId: tbl_id,
                ...defaultChartProps
            });
            return chartId;
        }
    } else return chartIds[0];
}

function doUpdateViewer(viewerId, tblGroup, chartId, useOnlyChartsInViewer) {
    const currentIds = getViewerItemIds(getMultiViewRoot(), viewerId);
    if (useOnlyChartsInViewer) {
        if (!getActiveViewerItemId(viewerId)) {
            dispatchUpdateCustom(viewerId, {activeItemId: currentIds[0]});
        }
        return;
    }
    const tblId = getActiveTableId(tblGroup);
    const chartIds = [];
    chartIds.push(...getChartIdsInGroup(tblId), ...getChartIdsInGroup('default'));
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


export const ChartsContainer = (props)  =>{
    const {chartId, expandedMode} = props;

    return expandedMode || chartId || !allowPinnedCharts() ?
        <ActiveChartsPanel {...props}/>:
        <PinnedChartContainer {...props}/> ;
};

ChartsContainer.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool,
    chartId: PropTypes.string,
    viewerId : PropTypes.string,
    tbl_group : PropTypes.string,
    addDefaultChart : PropTypes.bool,
    noChartToolbar : PropTypes.bool,
    useOnlyChartsInViewer :PropTypes.bool
};


/**
 * Default viewer
 * When tbl_group is defined, only the charts related to the active chart in this table group are displayed
 * When addDefaultChart is true, a default chart is created for each table in the group
 * @param props properties for this component
 */
export const ActiveChartsPanel = (props) => {

    const {viewerId=DEFAULT_PLOT2D_VIEWER_ID, tbl_group, addDefaultChart, chartId, useOnlyChartsInViewer,
        expandedMode, closeable, noChartToolbar, toolbarVariant, useBorder} = props;

    useEffect(() => {
        if (tbl_group) {
            if (addDefaultChart) {
                const tbl_id = getActiveTableId(tbl_group);
                if (isFullyLoaded(tbl_id)) {
                    ensureDefaultChart(tbl_id);
                }
            }
            // make sure the viewer is updated with related charts on start
            // important when we use external viewer
            doUpdateViewer(viewerId, tbl_group, chartId, useOnlyChartsInViewer);
        }

        if (!useOnlyChartsInViewer) {
            return watchTblGroup(viewerId, tbl_group, addDefaultChart, useOnlyChartsInViewer);
        }

    }, [tbl_group, viewerId, chartId, useOnlyChartsInViewer]);

    if (chartId) {
        if (expandedMode) {
            return (
                <ExpandedView {...{
                    key: 'chart-expanded',
                    closeable,
                    chartId,
                    tbl_group,
                    noChartToolbar,
                    toolbarVariant,
                    viewerId
                }}/>
            );
        } else {
            return (
                <ChartPanel {...{
                    key: chartId,
                    expandable: true,
                    chartId,
                    tbl_group,
                    toolbarVariant,
                    showToolbar: !Boolean(noChartToolbar),
                }}/>
            );
        }
    } else {
           return (
                <MultiChartViewer {...{
                    closeable,
                    viewerId,
                    tbl_group,
                    expandedMode,
                    noChartToolbar,
                    toolbarVariant,
                    useBorder,
                    showAddChart: true}}
                />
            );
    }
};

ActiveChartsPanel.propTypes = ChartsContainer.propTypes;


function ExpandedView(props) {
    const {closeable, chartId, viewerId} = props;
    return (
        <Stack height={1}>
            <ChartToolbarExt {...{chartId, closeable, viewerId}}/>
            <Stack flexGrow={1}>
                <ChartPanel {...{
                    key: 'expanded-'+chartId,
                    expandedMode: true,
                    expandable: false,
                    chartId,
                    showToolbar: false
                }}/>
            </Stack>
        </Stack>
    );
}

ExpandedView.propTypes = {
    closeable: PropTypes.bool,
    chartId: PropTypes.string,
    noChartToolbar : PropTypes.bool,
    viewerId: PropTypes.string
};


export function closeExpandedChart(viewerId) {
    const {layout} = getExpandedChartProps();
    viewerId && layout && dispatchChangeViewerLayout(viewerId, layout);                      // switch back to previous layout if exists
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
}

const ChartToolbarExt = ({chartId, viewerId, tbl_group, noChartToolbar, closeable=true}) => {
    if (!closeable && noChartToolbar) return null;

    return (
        <Stack direction='row' justifyContent='space-between'>
            {closeable && <CloseButton onClick={() => closeExpandedChart(viewerId)}/>}
            {!noChartToolbar && <ChartToolbar {...{chartId, viewerId, tbl_group, expandable:false, expandedMode:true}}/>}
        </Stack>
    );
};
