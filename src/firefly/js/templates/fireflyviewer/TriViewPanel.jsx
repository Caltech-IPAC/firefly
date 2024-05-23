/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Stack} from '@mui/joy';
import {
    BadgeLabel, PinnedChartPanel, usePinnedChartInfo
} from 'firefly/charts/ui/PinnedChartContainer.jsx';
import {PropertySheetAsTable} from 'firefly/tables/ui/PropertySheet';
import {pick} from 'lodash';
import PropTypes from 'prop-types';
import React, {memo, useContext} from 'react';
import {getExpandedChartProps} from '../../charts/ChartsCntlr.js';
import {allowPinnedCharts} from '../../charts/ChartUtil.js';
import {ActiveChartsPanel} from '../../charts/ui/ChartsContainer.jsx';
import {dispatchUpdateLayoutInfo, getLayouInfo, getResultCounts, LO_VIEW} from '../../core/LayoutCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {AppInitLoadingMessage} from '../../ui/AppInitLoadingMessage.jsx';
import {AppPropertiesCtx} from '../../ui/AppPropertiesCtx.jsx';
import {Tab, Tabs} from '../../ui/panel/TabPanel.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {DEFAULT_PLOT2D_VIEWER_ID, PINNED_CHART_VIEWER_ID} from '../../visualize/MultiViewCntlr.js';
import {
    makeCoverageTab, makeFitsPinnedTab, makeMultiProductViewerTab, TriViewImageSection
} from '../../visualize/ui/TriViewImageSection.jsx';
import {ResultsPanel} from './ResultsPanel.jsx';

const stateKeys= ['title', 'mode', 'showTables', 'showImages', 'showXyPlots', 'images', 'coverageSide', 'initLoadCompleted'];
const LEFT= 'LEFT';
const RIGHT= 'RIGHT';
const triViewKey= 'images | tables | xyplots';
const tblImgKey= 'tables | images';
const imgXyKey= 'images | xyplots';
const tblXyKey= 'tables | xyplots';
const xYTblKey= 'xyplots | tables';


export const TriViewPanel= memo(( {showViewsSwitch=true, leftButtons, centerButtons, rightButtons} ) => {
    const {landingPage,initLoadingMessage,apiHandlesExpanded=false}= useContext(AppPropertiesCtx);
    const state= useStoreConnector(() => pick(getLayouInfo(), stateKeys));
    const {title, mode, showTables, showImages, showXyPlots, images={}, coverageSide=LEFT, initLoadCompleted} = state;
    const {expanded, standard=triViewKey, closeable} = mode ?? {};
    const content = {};
    const {showMeta, showFits, dataProductTableId, showCoverage} = images;
    const coverageRight= showCoverage && coverageSide===RIGHT;
    const coverageLeft= showCoverage && coverageSide===LEFT;
    const currLayoutMode= getLayouInfo()?.mode?.standard?.toString() ?? triViewKey;
    const imagesWithCharts= currLayoutMode===tblXyKey || currLayoutMode===xYTblKey;

    if (initLoadingMessage && !initLoadCompleted) return (<AppInitLoadingMessage message={initLoadingMessage}/>);
    if (!showImages && !showXyPlots && !showTables) return landingPage;

    if (!imagesWithCharts && (showImages || coverageLeft)) {
        content.imagePlot = (<TriViewImageSection key='res-tri-img'
                                                  closeable={closeable}
                                                  coverageSide={coverageSide}
                                                  imageExpandedMode={expanded===LO_VIEW.images}
                                                  {...images}  />);
    }
    if (showXyPlots || coverageRight || imagesWithCharts) {
        content.rightSide= (<RightSide {...{key:'rightSide', expanded,closeable,showXyPlots, showMeta, showFits,
            dataProductTableId, coverageRight, imagesWithCharts}}/>);
        if (expanded===LO_VIEW.xyPlots) content.xyPlot= content.rightSide;
    }
    const renderTables= showTables && currLayoutMode?.includes('tables');
    if (renderTables) {
        content.tables = (<TablesContainer key='res-tables'
                                           mode='both'
                                           closeable={closeable}
                                           expandedMode={expanded===LO_VIEW.tables}/>);
        if (currLayoutMode===xYTblKey) content.flip= true;
    }

    const expandType = LO_VIEW.get(expanded) ?? LO_VIEW.none;
    if (apiHandlesExpanded && expandType!==LO_VIEW.none) return;
    
    return (
        <ResultsPanel {...{key:'results', title,
                      searchDesc:searchDesc({showViewsSwitch, leftButtons, centerButtons, rightButtons, }),
                      expanded, standard}}
                      { ...content} />
    );
});



TriViewPanel.propTypes = {
    showViewsSwitch: PropTypes.bool,
    leftButtons: PropTypes.arrayOf( PropTypes.func ),
    centerButtons: PropTypes.arrayOf( PropTypes.func ),
    rightButtons: PropTypes.arrayOf( PropTypes.func ),
    coverageSide:  PropTypes.string,
    landingPage: PropTypes.object,
};

let lastSelected;

const ACTIVE_CHART_TAB_ID= 'activeCharts';
const PINNED_CHART_TAB_ID= 'pinnedCharts';
const COVERAGE_TAB_ID= 'coverage';
const DP_TAB_ID='meta';
const PINNED_IMAGE_TAB_ID='fits';
const PROPERTY_SHEET_TAB_ID='rowDetails';

function getSelectedTab(idObj,coverageRight,showXyPlots,showFits,anyTables,anyPinnedCharts) {
    const tabIds= Object.entries(idObj).filter(([,v]) => v).map(([k]) => k);
    if (tabIds.includes(lastSelected)) return lastSelected;
    if (!anyTables) {
        if (showFits) return PINNED_IMAGE_TAB_ID;
        if (anyPinnedCharts) return PINNED_CHART_TAB_ID;
    }
    return coverageRight ? COVERAGE_TAB_ID : showXyPlots ? ACTIVE_CHART_TAB_ID : PINNED_CHART_TAB_ID;
}

function makeKey(idObj) {
    return Object.entries(idObj)
        .filter(([,v]) => v)
        .map(([k]) => k)
        .join('-');
}



function RightSide({expanded, closeable, showXyPlots, showMeta, showFits, dataProductTableId, coverageRight, imagesWithCharts }) {

    const onTabSelect = (id) => {
        lastSelected= id;
        dispatchUpdateLayoutInfo({rightSide:{selectedTab:id}});
    };

    const chartExpandedMode= expanded===LO_VIEW.xyPlots;
    const {expandedViewerId}= getExpandedChartProps();
    const viewerId = DEFAULT_PLOT2D_VIEWER_ID;
    const {showPinnedTab, activeLabel, pinnedLabel} = usePinnedChartInfo({viewerId});

    if (chartExpandedMode) {
        if (expandedViewerId === PINNED_CHART_VIEWER_ID) {
            return makePinnedChartTab({viewerId, activeLabel, chartExpandedMode, closeable});
        } else {
            return makeActiveChartTab({viewerId, activeLabel, chartExpandedMode, closeable});
        }
    }

    const {pinChartCnt=0,tableCnt=0}= getResultCounts() ?? {};
    const anyTables= Boolean(tableCnt);
    const anyPinnedCharts= Boolean(pinChartCnt);
    const idObj= {
        [ACTIVE_CHART_TAB_ID]: anyTables && showXyPlots,
        [PINNED_CHART_TAB_ID]: allowPinnedCharts() && showPinnedTab,
        [COVERAGE_TAB_ID]: anyTables && (imagesWithCharts || coverageRight),
        [DP_TAB_ID]: imagesWithCharts && showMeta,
        [PINNED_IMAGE_TAB_ID]: imagesWithCharts && showFits,
        [PROPERTY_SHEET_TAB_ID]:anyTables,
    };

    const style= {height: '100%'};
    const defaultSelected= getSelectedTab(idObj,coverageRight,showXyPlots,showFits,anyTables,anyPinnedCharts);


    if (idObj[PINNED_IMAGE_TAB_ID] && Object.values(idObj).filter( (v) => v).length===1) {
        return makeFitsPinnedTab({id:PINNED_IMAGE_TAB_ID,asTab:false});
    }


    return(
        <Tabs {...{key:makeKey(idObj), style, onTabSelect, defaultSelected} } >
            {idObj[ACTIVE_CHART_TAB_ID] && makeActiveChartTab({activeLabel, chartExpandedMode, closeable, asTab:true, id:ACTIVE_CHART_TAB_ID}) }
            {idObj[PINNED_CHART_TAB_ID] && makePinnedChartTab({pinnedLabel, chartExpandedMode, closeable, asTab:true, id:PINNED_CHART_TAB_ID}) }
            {idObj[COVERAGE_TAB_ID] && makeCoverageTab({id:COVERAGE_TAB_ID})}
            {idObj[DP_TAB_ID] && makeMultiProductViewerTab({dataProductTableId,id:DP_TAB_ID})}
            {idObj[PINNED_IMAGE_TAB_ID] && makeFitsPinnedTab({id:PINNED_IMAGE_TAB_ID,asTab:true})}
            {idObj[PROPERTY_SHEET_TAB_ID] && makePropertySheetTab({id:PROPERTY_SHEET_TAB_ID})}
        </Tabs>
    );
}

function makeActiveChartTab({activeLabel, chartExpandedMode, closeable, asTab, id}) {
    const chartpanel = (<ActiveChartsPanel closeable={closeable} expandedMode={chartExpandedMode}
                              useOnlyChartsInViewer={false}
                              tbl_group='main' addDefaultChart={true}/>);

    return asTab ? <Tab name={activeLabel} removable={false} id={id}>{chartpanel}</Tab> : chartpanel;
}

function makePinnedChartTab({pinnedLabel, chartExpandedMode, closeable, asTab,id}) {
    const chartpanel =(<PinnedChartPanel closeable={closeable} expandedMode={chartExpandedMode}
                             useOnlyChartsInViewer={false}
                             tbl_group='main' addDefaultChart={true}/>);

    return asTab ? <Tab name={pinnedLabel} removable={false} id={id} label={<BadgeLabel labelStr={pinnedLabel}/>}>{chartpanel}</Tab> : chartpanel;
    }

function searchDesc({showViewsSwitch, leftButtons, centerButtons, rightButtons}) {

    const hasContent = showViewsSwitch || leftButtons || centerButtons || rightButtons;
    if (!hasContent) return  <div/>;

    return (
        <Stack {...{direction: 'row', justifyContent: 'space-between', marginBottom:'-1px'}}>
            <Stack direction='row'>
                {leftButtons?.map( (el) => el()) }
            </Stack>
            <Stack direction='row'>
                {centerButtons?.map( (el) => el()) }
            </Stack>
            <Stack direction='row'>
                {rightButtons?.map( (el) => el()) }
                <Box sx={{width: 1, px:1}}/>
            </Stack>
        </Stack>
    );
}

function makePropertySheetTab({id}) {
    return (
        <Tab key='rowDetails' name='Details' removable={false} id={id}>
            <PropertySheetAsTable
                slotProps={{ toolbar:{variant:'plain'}, root:{variant: 'plain'} }}
            />
        </Tab>
    );
}