/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Badge, Stack, Typography, Sheet, Button} from '@mui/joy';

import React, {useContext, useEffect} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, isEqual, omit, cloneDeep, set} from 'lodash';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ChartPanel} from './ChartPanel.jsx';
import {MultiItemViewerView} from '../../visualize/ui/MultiItemViewerView.jsx';
import {
    dispatchAddViewer, dispatchViewerUnmounted, dispatchUpdateCustom,
    getMultiViewRoot, getViewer, getLayoutType, PLOT2D, getViewerItemIds, dispatchRemoveViewerItems,
    dispatchAddViewerItems, NewPlotMode, PINNED_CHART_VIEWER_ID
} from '../../visualize/MultiViewCntlr.js';
import {
    getExpandedChartProps, getChartData, CHART_ADD, CHART_REMOVE, getChartIdsInGroup, dispatchChartAdd, dispatchChartRemove, CHART_UPDATE
} from '../ChartsCntlr.js';
import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {MultiChartToolbarStandard, MultiChartToolbarExpanded} from './MultiChartToolbar.jsx';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {Logger} from '../../util/Logger.js';
import {findGroupByTblId, getActiveTableId, getTblById, monitorChanges} from '../../tables/TableUtil';
import {TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_REMOVE, dispatchActiveTableChanged} from '../../tables/TablesCntlr';
import {getTblIdFromChart, hasTracesFromSameTable, isChartLoading, uniqueChartId} from '../ChartUtil';
import {getActiveViewerItemId} from './MultiChartViewer';
import {ActiveChartsPanel} from './ChartsContainer';
import {StatefulTabs, switchTab, Tab} from '../../ui/panel/TabPanel';
import {HelpIcon} from '../../ui/HelpIcon';
import {getComponentState, dispatchComponentStateChange} from '../../core/ComponentCntlr';
import {SplitPanel} from '../../ui/panel/DockLayoutPanel';
import {hideInfoPopup, showInfoPopup, showPinMessage} from '../../ui/PopupUtil.jsx';
import {dispatchAddActionWatcher} from 'firefly/core/MasterSaga';
import {PinButton, ShowTableButton} from 'firefly/visualize/ui/Buttons.jsx';

export const PINNED_CHART_PREFIX = 'pinned-';
export const PINNED_GROUP = PINNED_CHART_VIEWER_ID;                 // use same id for now.
const logger = Logger('PinnedChartContainer');
const PINNED_MAX = 12;

export const PinnedChartContainer = (props) => {
    const {viewerId} = props;

    const tabs          = useStoreConnector(() =>  getComponentState(PINNED_CHART_VIEWER_ID));
    const {showPinnedTab, activeLabel, pinnedLabel} = usePinnedChartInfo(({viewerId}));

    const TabToolbar = () => {
        return (
            <Stack direction='row' justifyContent='space-between'>
                <ToggleLayout/>
                <Help/>
            </Stack>
        );
    };

    if (tabs?.sideBySide) {
        return (
            <Stack id='chart-pinned-sideBySide' overflow='hidden' flexGrow={1}>
                <Stack flexGrow={1} position='relative'>
                    <SplitPanel split='vertical' defaultSize={400} style={{display: 'inline-flex'}} pKey='chart-sideBySide'>
                        <Stack>
                            <Typography level='title-md'>
                                {activeLabel}
                            </Typography>
                            <ActiveChartsPanel {...props}/>
                        </Stack>
                        <Stack>
                            <Typography level='title-md'>
                                {pinnedLabel}
                            </Typography>
                            <PinnedChartPanel {...props}/>
                        </Stack>
                    </SplitPanel>
                </Stack>
            </Stack>
        );
    } else {
        return (
            <Stack id='chart-pinned-tabs' overflow='hidden' height={1}>
                <StatefulTabs componentKey={PINNED_CHART_VIEWER_ID}>
                    <Tab name={activeLabel}>
                        <ActiveChartsPanel {...props}/>
                    </Tab>
                    {showPinnedTab &&
                        <Tab name='Pinned Charts' label={<BadgeLabel labelStr='Pinned Charts'/>}>
                            <PinnedChartPanel {...props}/>
                        </Tab>
                    }
                </StatefulTabs>
            </Stack>
        );
    }
};
PinnedChartContainer.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool,
    chartId: PropTypes.string,
    viewerId : PropTypes.string,
    tbl_group : PropTypes.string,
    addDefaultChart : PropTypes.bool,
    noChartToolbar : PropTypes.bool,
    useOnlyChartsInViewer :PropTypes.bool
};

export const usePinnedChartInfo = ({viewerId}) => {
    const chartIds      = useStoreConnector(() =>  getChartIdsInGroup(PINNED_GROUP));
    const activeLabel   = useStoreConnector(() => getViewerItemIds(getMultiViewRoot(), viewerId)?.length > 1 ? 'Active Charts' : 'Active Chart');

    const pinnedLabel = chartIds?.length > 1 ? 'Pinned Charts' : 'Pinned Chart';
    const showPinnedTab = chartIds?.length >= 1;
    return {chartIds, showPinnedTab, activeLabel, pinnedLabel};

};


const Help = () => <HelpIcon helpId={'chartarea.info'}/>;


// --------------------- Pin Chart ---------------------------

export const PinChart = ({viewerId, tbl_group}) => {

    // don't show pin chart button in pinned chart panel
    // and in data product viewer because pinning functionality requires chart data table to persist in store
    if (viewerId === PINNED_CHART_VIEWER_ID || viewerId.startsWith('DPC')) return null;

    const doPinChart = () => {
        let chartId = getActiveViewerItemId(viewerId, true);      // viewerId is Active Charts viewer
        if (!chartId) {
            const tblId = getActiveTableId(tbl_group);
            chartId = getChartIdsInGroup(tblId)?.[0] ?? getChartIdsInGroup('default')?.[0];
        }
        pinChart({chartId});
    };
    return <PinButton onClick={doPinChart} tip='Pin this chart'/>;
};

export function pinChart({chartId, autoLayout=false }) {


    if (!isChartLoading(chartId)) {
        doPinChart({chartId,autoLayout});
        return;
    }
    // there are cases when chart is not fully loaded.  if so, wait before pinning the chart
    dispatchAddActionWatcher({actions:[CHART_UPDATE], callback: (action, cancelSelf) => {
            const aChartId = action.payload?.chartId;
            if (chartId !== aChartId) {
                cancelSelf?.();
                return;
            }
            if (!isChartLoading(chartId)) { //chart is fully loaded now
                cancelSelf?.();
                doPinChart({chartId, autoLayout});
            }
        }
    });
}

export function BadgeLabel({labelStr}) {
    const badgeCnt= useStoreConnector(() => getViewerItemIds(getMultiViewRoot(),PINNED_CHART_VIEWER_ID)?.length??0);
    return badgeCnt===0 ?  labelStr:
        (
            <Badge {...{badgeContent:badgeCnt,
                sx:{'& .MuiBadge-badge': {top:9, right:6}} }}>
                <div>
                    <div className='text-ellipsis' style={{marginRight: 17}}>{labelStr}</div>
                </div>
            </Badge>
        );
}

function doPinChart({chartId, autoLayout=true }) {

    const chartData = cloneDeep(omit(getChartData(chartId), ['_original', 'mounted']));
    chartData?.tablesources?.forEach((ts) => Reflect.deleteProperty(ts, '_cancel'));

    const pinnedCnt = getViewerItemIds(getMultiViewRoot(), PINNED_CHART_VIEWER_ID)?.length ?? 0;
    if (pinnedCnt >= PINNED_MAX) {
        showInfoPopup('Only pinning table: You have reached the maximum number of allowable pinned charts.', 'Max Pinned Charts');
        return;
    }

    const addChart = () => {
        dispatchChartAdd({
            ...chartData,
            chartId: uniqueChartId(PINNED_CHART_PREFIX),
            groupId: PINNED_GROUP,
            viewerId: PINNED_CHART_VIEWER_ID,
            deletable: true,
            mounted: true
        });
        if (autoLayout && pinnedCnt === 0) {
            // if auto-layout, show side-by-side on first pinned chart
            setLayout(true);
        }
        const {sideBySide} = getComponentState(PINNED_CHART_VIEWER_ID);
        if (!sideBySide) showPinMessage('Pinning chart');
    };

    if (!chartData?.layout?.title?.text) {
        // if chart has no title, ask to use table's title
        const tbl_id = chartData?.data?.[0]?.tbl_id || chartData?.fireflyData?.[0].tbl_id;
        const title = getTblById(tbl_id)?.title || '';
        set(chartData, 'layout.title', title);
        addChart();
        hideInfoPopup();
    } else {
        addChart();
    }
}


// --------------------- simple toolbar actions ---------------------------

export const ShowTable = ({viewerId, tbl_group}) => {

    const activeTblId = useStoreConnector(() => getActiveTableId());
    const {chartId, activeChartTblId} = useStoreConnector(() => {
        const chartId = getActiveViewerItemId(PINNED_CHART_VIEWER_ID, true);
        const activeChartTblId = getTblIdFromChart(chartId);
        return {chartId, activeChartTblId};
    });

    // this button should only appear inside pinned charts viewer and when the selected chart has traces' data in the same table
    if (viewerId !== PINNED_CHART_VIEWER_ID || !hasTracesFromSameTable(chartId)) return null;

    const showTable = () => dispatchActiveTableChanged(activeChartTblId, tbl_group);
    const enabled = activeChartTblId !== activeTblId;

    return activeChartTblId ? <ShowTableButton enabled={enabled} onClick={showTable} tip='Show table corresponding to this chart'/> : null;

};

export const ToggleLayout = () => {

    const {sideBySide=false} = getComponentState(PINNED_CHART_VIEWER_ID);
    const canToggle =  getViewerItemIds(getMultiViewRoot(), PINNED_CHART_VIEWER_ID)?.length > 0;
    const [modeLabel, modeTitle] = sideBySide ? ['As Tabs', 'Switch to tabs layout'] : ['Side-By-Side', 'Switch to Side-By-Side layout'];

    return canToggle &&
        <Button variant='plain' onClick={toggleLayout} title={modeTitle}>
            {modeLabel}
        </Button>;
};


export const toggleLayout = () => {
    const {sideBySide=false} = getComponentState(PINNED_CHART_VIEWER_ID);
    setLayout(!sideBySide);
};

const setLayout = (sideBySide) => {
    dispatchComponentStateChange(PINNED_CHART_VIEWER_ID, {sideBySide});
};

export const showAsTabs = (showPinnedCharts=false) => {
    if (showPinnedCharts) {
        switchTab(PINNED_CHART_VIEWER_ID, 1);     // tab index 1 is pinned charts
    }
    setLayout(false);
};

// ------------------------------------------------



export const PinnedChartPanel = (props) => {

    const {tbl_group='main', expandedMode, closeable} = props;
    const canReceiveNewItems=NewPlotMode.create_replace.key;
    const viewerId = PINNED_CHART_VIEWER_ID;

    const {renderTreeId} = useContext(RenderTreeIdCtx);

    // ensure viewer is initialized
    useEffect(() => {
        dispatchAddViewer(viewerId, canReceiveNewItems, PLOT2D,true, renderTreeId);
        if (expandedMode) {
            const {chartId} = getExpandedChartProps();
            dispatchUpdateCustom(viewerId, {activeItemId: chartId});
        }
        return () => dispatchViewerUnmounted(viewerId);

    }, [viewerId, expandedMode, renderTreeId]);

    useEffect(() => {
        // make sure the viewer is updated with related charts on start
        // important when we use external viewer
        doUpdateViewer(viewerId, tbl_group);

        const accept = (a) => {
            const {tbl_id, chartId} = a.payload;
            return chartId || tbl_group === findGroupByTblId(tbl_id);
        };
        return monitorChanges([CHART_ADD, CHART_REMOVE, TBL_RESULTS_ACTIVE, TABLE_REMOVE, TABLE_LOADED], accept, (a)=> doUpdateViewer(viewerId, tbl_group, a), `wtg-${viewerId}-${tbl_group}`);

    }, [tbl_group, viewerId]);

    const viewer = useStoreConnector(() => getViewer(getMultiViewRoot(),viewerId));
    const activeItemId = useStoreConnector(() =>  getActiveViewerItemId(viewerId, true));

    if (!viewer || isEmpty(viewer.itemIdAry)) {
        return expandedMode && closeable ? <BlankClosePanel/> : null;
    }

    const deletable = viewer.itemIdAry.length > 1;                  // if there are more than 1 chart in the viewer, they should be deletable by default
    const layoutType= getLayoutType(getMultiViewRoot(), viewerId);

    const onChartSelect = (ev,chartId) => {
        if (chartId !== activeItemId) {
            dispatchUpdateCustom(viewerId, {activeItemId: chartId});
        }
        stopPropagation(ev);
    };

    const makeItemViewer = (chartId) => (
        <Sheet id='chart-item' sx={{height:1, width:1, display:'flex'}}
               variant='outlined'
               color={ chartId === activeItemId ? 'warning' : 'neutral'}
               onClick={(ev)=>onChartSelect(ev,chartId)}
               onTouchStart={stopPropagation}
               onMouseDown={stopPropagation}>
            <ChartPanel key={chartId} showToolbar={false} chartId={chartId} deletable={deletable} thumbnail={layoutType === 'grid'}/>
        </Sheet>
    );

    const makeItemViewerFull = (chartId) => (
        <Stack id='chart-itemFull' onClick={stopPropagation} sx={{height:1, width:1}}
               onTouchStart={stopPropagation}
               onMouseDown={stopPropagation}>
            <ChartPanel key={chartId} showToolbar={false} chartId={chartId} deletable={deletable}/>
        </Stack>
    );

    const viewerItemIds = viewer.itemIdAry;
    const ToolBar = expandedMode ? MultiChartToolbarExpanded : MultiChartToolbarStandard;

    logger.log('Active chart ID: ' + activeItemId);
    if (!activeItemId) return null;

    return (
        <Stack id='chart-pinnedChart' width={1} height={1}>
            <Stack flexGrow={1} position='relative'>
                <ToolBar chartId={activeItemId} expandable={!expandedMode} {...{expandedMode, closeable, viewerId, tbl_group, layoutType, activeItemId}}/>
                <MultiItemViewerView {...props} {...{viewerId, layoutType, makeItemViewer, makeItemViewerFull, activeItemId, viewerItemIds}}/>
            </Stack>
        </Stack>
    );
};

PinnedChartPanel.propTypes= {
    tbl_group: PropTypes.string,
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool
};


function stopPropagation(ev) {
    ev.stopPropagation();
}

function BlankClosePanel() {
    return (
        <Stack height={1}>
            <CloseButton style={{paddingLeft: 10, position: 'absolute', top: 0, left: 0}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>
        </Stack>
    );
}

function doUpdateViewer(viewerId, tbl_group, action) {
    switch (action?.type) {
        case TABLE_REMOVE: {
            const {tbl_id} = action.payload;
            getChartIdsInGroup(PINNED_GROUP)
                .filter((id) => getChartData(id)?.tbl_id === tbl_id)
                .forEach((id) => dispatchChartRemove(id));
            break;
        }
        case CHART_REMOVE: {
            const pinnedCnt = getViewerItemIds(getMultiViewRoot(), PINNED_CHART_VIEWER_ID)?.length ?? 0;
            if (pinnedCnt <= 0) {
                switchTab(PINNED_CHART_VIEWER_ID, 0);  // set selected tab back to 0
            }
            break;
        }
    }

    const currentIds = getViewerItemIds(getMultiViewRoot(), PINNED_GROUP);
    const chartIds = getChartIdsInGroup(PINNED_GROUP);

    if (!isEqual(chartIds, currentIds)) {
        dispatchRemoveViewerItems(PINNED_GROUP, currentIds);
        dispatchAddViewerItems(PINNED_GROUP, chartIds, PLOT2D);
    }
}
