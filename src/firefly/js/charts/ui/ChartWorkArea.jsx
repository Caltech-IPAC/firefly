/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import './ChartPanel.css';

import React, {useContext, useEffect} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, isEqual, isUndefined, omit, cloneDeep, set} from 'lodash';
import SplitPane from 'react-split-pane';

import {CloseButton} from '../../ui/CloseButton.jsx';
import {ChartPanel} from './ChartPanel.jsx';
import {MultiItemViewerView} from '../../visualize/ui/MultiItemViewerView.jsx';
import {
    dispatchAddViewer, dispatchViewerUnmounted, dispatchUpdateCustom,
    getMultiViewRoot, getViewer, getLayoutType, PLOT2D, getViewerItemIds, dispatchRemoveViewerItems, dispatchAddViewerItems, NewPlotMode
} from '../../visualize/MultiViewCntlr.js';
import {getExpandedChartProps, getChartData, CHART_ADD, CHART_REMOVE, getChartIdsInGroup, dispatchChartAdd, dispatchChartRemove} from '../ChartsCntlr.js';
import {LO_VIEW, LO_MODE, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';
import {MultiChartToolbarStandard, MultiChartToolbarExpanded} from './MultiChartToolbar.jsx';
import {RenderTreeIdCtx} from '../../ui/RenderTreeIdCtx.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {Logger} from '../../util/Logger.js';
import {findGroupByTblId, getActiveTableId, getTblById, monitorChanges} from '../../tables/TableUtil';
import {TABLE_LOADED, TBL_RESULTS_ACTIVE, TABLE_REMOVE, dispatchActiveTableChanged} from '../../tables/TablesCntlr';
import {uniqueChartId} from '../../charts/ChartUtil';
import {getActiveViewerItemId} from '../../charts/ui/MultiChartViewer';
import {DefaultChartsContainer} from '../../charts/ui/ChartsContainer';
import {StatefulTabs, switchTab, Tab} from '../../ui/panel/TabPanel';
import {HelpIcon} from '../../ui/HelpIcon';
import {getComponentState, dispatchComponentStateChange} from '../../core/ComponentCntlr';
import {SplitContent} from '../../ui/panel/DockLayoutPanel';
import {hideInfoPopup, showInfoPopup, showYesNoPopup, showPinMessage} from '../../ui/PopupUtil.jsx';

const logger = Logger('ChartWorkArea');
const PINNED_CHART_PREFIX = 'pinned-';

const PINNED_VIEWER_ID = 'PINNED_CHARTS_VIEWER';
const PINNED_GROUP = PINNED_VIEWER_ID;                 // use same id for now.
const PINNED_MAX = 6;



export function pinChart({chartId, autoLayout=true}) {
    const chartData = cloneDeep(omit(getChartData(chartId), ['_original', 'mounted']));
    chartData?.tablesources?.forEach((ts) => Reflect.deleteProperty(ts, '_cancel'));

    const pinnedCnt = getViewerItemIds(getMultiViewRoot(), PINNED_VIEWER_ID)?.length ?? 0;
    if (pinnedCnt >= PINNED_MAX) {
        showInfoPopup('You have reached the maximum number of allowable pinned charts');
        return;
    }

    const addChart = () => {
        dispatchChartAdd({
            ...chartData,
            chartId: uniqueChartId(PINNED_CHART_PREFIX),
            groupId: PINNED_GROUP,
            viewerId: PINNED_VIEWER_ID,
            deletable: true,
            mounted: true
        });
        if (autoLayout && pinnedCnt === 0) {
            // if auto-layout, show side-by-side on first pinned chart
            showSideBySide();
        }
        const {sideBySide} = getComponentState(PINNED_VIEWER_ID);
        if (!sideBySide) showPinMessage('Pinning chart');
    };

    if (!chartData?.layout?.title?.text) {
        // if chart has no title, ask to use table's title
        const tbl_id = chartData?.data?.[0]?.tbl_id || chartData?.fireflyData?.[0].tbl_id;
        let title = getTblById(tbl_id)?.title || '';
        showYesNoPopup(
            (
                <div style={{display: 'inline-flex', whiteSpace: 'nowrap', alignItems: 'center'}}>
                    <div style={{marginRight: 3}}>Chart Title:</div>
                    <input defaultValue={title} onClick={(e)=>e?.target.select()} onChange={(e) => title=e?.target?.value}/>
                </div>
            ),
            (p,ans) => {
                if (ans) set(chartData, 'layout.title', title);
                addChart();
                hideInfoPopup();
            },
            'Add a title?'
        );
    } else {
        addChart();
    }
}

export const toggleSideBySide = () => {
    const {sideBySide=false} = getComponentState(PINNED_VIEWER_ID);
    dispatchComponentStateChange(PINNED_VIEWER_ID, {sideBySide: !sideBySide});
};

export const showSideBySide = () => {
    dispatchComponentStateChange(PINNED_VIEWER_ID, {sideBySide: true});
};

export const showAsTabs = (showPinnedCharts=false) => {
    if (showPinnedCharts) {
        switchTab(PINNED_VIEWER_ID, 1);     // tab index 1 is pinned charts
    }
    dispatchComponentStateChange(PINNED_VIEWER_ID, {sideBySide: false});
};


let splitLoc = 400;
export const ChartWorkArea = (props) => {
    const {viewerId, tbl_group} = props;

    const chartIds = useStoreConnector(() =>  getChartIdsInGroup(PINNED_GROUP));
    const tabs = useStoreConnector(() =>  getComponentState(PINNED_VIEWER_ID));
    const activeLabel = useStoreConnector(() => getViewerItemIds(getMultiViewRoot(), viewerId)?.length > 1 ? 'Active Charts' : 'Active Chart');

    if (!chartIds?.length)  {           // use the default container if there's no pinned charts
        return (
            <div className='TabPanel__main boxed' style={{height: '100%'}}>
                <Toolbar {...{viewerId, tbl_group}}/>
                <DefaultChartsContainer {...props}/>
            </div>
        );
    }

    if (tabs?.sideBySide) {
        const label = {fontWeight: 'bold', height: 15, marginLeft: 3};
        return (
            <div className='TabPanel__main boxed' style={{height: '100%'}}>
                <Toolbar {...{viewerId, tbl_group, style:{position: 'absolute', right:0, zIndex: 1}}}/>
                <div style={{flexGrow: 1, position: 'relative'}}>
                    <SplitPane split='vertical' defaultSize={splitLoc} onChange={(size) => splitLoc = size} style={{display: 'inline-flex'}}>
                        <SplitContent style={{display: 'flex', flexDirection: 'column'}}>
                            <div style={label}>{activeLabel}</div>
                            <DefaultChartsContainer {...props}/>
                        </SplitContent>
                        <SplitContent style={{display: 'flex', flexDirection: 'column'}}>
                            <div style={label}>Pinned Charts</div>
                            <PinnedCharts {...props}/>
                        </SplitContent>
                    </SplitPane>
                </div>
            </div>
        );
    } else {
        return (
            <div className='TabPanel__main boxed' style={{height: '100%'}}>
                <Toolbar {...{viewerId, tbl_group, style:{position: 'absolute', right:0, zIndex: 1}}}/>
                <StatefulTabs componentKey={PINNED_VIEWER_ID} defaultSelected={0} useFlex={true} style={{flex: '1 1 0', marginTop: 5}}>
                    <Tab name={activeLabel}>
                        <DefaultChartsContainer {...props}/>
                    </Tab>
                    <Tab name='Pinned Charts'>
                        <PinnedCharts {...props}/>
                    </Tab>
                </StatefulTabs>
            </div>
        );
    }
};
ChartWorkArea.propTypes = {
    expandedMode: PropTypes.bool,
    closeable: PropTypes.bool,
    chartId: PropTypes.string,
    viewerId : PropTypes.string,
    tbl_group : PropTypes.string,
    addDefaultChart : PropTypes.bool,
    noChartToolbar : PropTypes.bool,
    useOnlyChartsInViewer :PropTypes.bool
};

const Toolbar = ({viewerId, tbl_group, style={}}) => {
    const doPinChart = () => {
        let chartId = getActiveViewerItemId(viewerId);      // viewerId is Active Charts viewer
        if (!chartId) {
            const tblId = getActiveTableId(tbl_group);
            chartId = getChartIdsInGroup(tblId)?.[0] ?? getChartIdsInGroup('default')?.[0];
        }
        pinChart({chartId});
    };
    const activeChartTblId = useStoreConnector(() => {
        const chartId = getActiveViewerItemId(PINNED_VIEWER_ID);
        return getChartData(chartId)?.data?.[0]?.tbl_id;
    });

    const showTable = () => dispatchActiveTableChanged(activeChartTblId, tbl_group);

    const {sideBySide=false, selectedIdx} = getComponentState(PINNED_VIEWER_ID);
    const canPin = sideBySide || selectedIdx !== 1;
    const canShowTable = activeChartTblId && (sideBySide || selectedIdx === 1);
    const canToggle =  getViewerItemIds(getMultiViewRoot(), PINNED_VIEWER_ID)?.length > 0;
    const [modeLabel, modeTitle] = sideBySide ? ['As Tabs', 'Switch to tabs layout'] : ['Side-By-Side', 'Switch to Side-By-Side layout'];

    return (
        <div style={{display: 'inline-flex', justifyContent: 'flex-end', backgroundColor: 'inherit', marginBottom: 3, ...style}}>
            {canPin && <div onClick={doPinChart} title='Pin the active chart' className='button text' style={{marginRight:20}}>Pin Chart</div>}
            {canShowTable && <div onClick={showTable} title='Show the table associated with this chart' className='button text'>Show Table</div>}
            {canToggle && <div onClick={toggleSideBySide} title={modeTitle} className='button text'>{modeLabel}</div>}
            <HelpIcon helpId={'chartarea.info'} style={{margin: '0 10px'}}/>
        </div>
    );
};

export const PinnedCharts = (props) => {

    const {tbl_group='main', expandedMode} = props;
    const canReceiveNewItems=NewPlotMode.create_replace.key, closeable=false;
    const viewerId = 'PINNED_CHARTS_VIEWER';

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

    if (!viewer || isEmpty(viewer.itemIdAry)) {
        return expandedMode && closeable ? <BlankClosePanel/> : null;
    }

    let activeItemId = getActiveViewerItemId(viewerId);
    if (isUndefined(activeItemId) || !getChartData(activeItemId)?.chartType) {
        activeItemId = viewer.itemIdAry[0];
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
        <div className={chartId === activeItemId ? 'ChartPanel ChartPanel--active' : 'ChartPanel'}
             onClick={(ev)=>onChartSelect(ev,chartId)}
             onTouchStart={stopPropagation}
             onMouseDown={stopPropagation}>
            <ChartPanel key={chartId} showToolbar={false} chartId={chartId} deletable={deletable}/>
        </div>
    );

    const makeItemViewerFull = (chartId) => (
        <div onClick={stopPropagation}
             onTouchStart={stopPropagation}
             onMouseDown={stopPropagation}>
            <ChartPanel key={chartId} showToolbar={false} chartId={chartId} deletable={deletable}/>
        </div>
    );

    const viewerItemIds = viewer.itemIdAry;
    const ToolBar = expandedMode ? MultiChartToolbarExpanded : MultiChartToolbarStandard;

    logger.log('Active chart ID: ' + activeItemId);

    return (
        <div className='ChartPanel__container'>
            <div className='ChartPanel__wrapper'>
                <ToolBar chartId={activeItemId} expandable={!expandedMode} {...{expandedMode, closeable, viewerId, layoutType, activeItemId}}/>
                <MultiItemViewerView {...props} {...{layoutType, makeItemViewer, makeItemViewerFull, viewerItemIds}}/>
            </div>
        </div>
    );
};

PinnedCharts.propTypes= {
    tbl_group: PropTypes.string,
    expandedMode: PropTypes.bool,
};



function stopPropagation(ev) {
    ev.stopPropagation();
}

function BlankClosePanel() {
    return (
        <div className='ChartPanel__container'>
            <div style={{position: 'relative', flexGrow: 1}}>
                <CloseButton style={{paddingLeft: 10, position: 'absolute', top: 0, left: 0}} onClick={() => dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none)}/>
            </div>
        </div>
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
            const pinnedCnt = getViewerItemIds(getMultiViewRoot(), PINNED_VIEWER_ID)?.length ?? 0;
            if (pinnedCnt <= 0) {
                switchTab(PINNED_VIEWER_ID, 0);  // set selected tab back to 0
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
