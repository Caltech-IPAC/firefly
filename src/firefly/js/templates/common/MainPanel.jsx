import {Stack} from '@mui/joy';
import React from 'react';

import {dispatchSetLayoutMode, getLayouInfo, LO_MODE, LO_VIEW} from 'firefly/core/LayoutCntlr.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {TablesContainer} from 'firefly/tables/ui/TablesContainer.jsx';
import {getExpandedChartProps} from 'firefly/charts/ChartsCntlr.js';
import {ChartsContainer} from 'firefly/charts/ui/ChartsContainer.jsx';
import {ImageExpandedMode} from 'firefly/visualize/iv/ImageExpandedMode.jsx';
import {VersionInfo} from 'firefly/ui/VersionInfo.jsx';

import FOOTER_BG from 'images/ipac_bar.jpg';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../../visualize/VisConst';

/**
 * Main panel of the FireflyLayout.  It handles the 3 possible views: drop-down, standard, expanded.
 * It also adds a footer to the drop-down view if footerComponent is set.
 *
 * Typically, these 3 views render the following:
 * 1) standard:   LandingPage or ResultsPanel.StandardView.DockLayoutPanel with split view of image, table and chart containers
 * 2) expanded:   ResultsPanel.ExpandedView with one of image, table or chart container expanded to entire main panel
 * 3) drop-down:  DropDownContainer containing any panel except the above two like search panel, upload panel, etc.
 * @param p     props
 * @param p.dropDownComponent   drop-down component to show when drop-down is visible
 * @param p.footer     footer component to show
 * @param p.showDropDown        set drop-down state
 * @param p.useDefaultExpandedView  use default ExpandedView
 * @param p.children            main content to show when it's not drop-down or expanded view
 * @return {JSX.Element}
 */
export function MainPanel({dropDownComponent, footer, showDropDown, useDefaultExpandedView=false, children}) {
    const {mode} = useStoreConnector(getLayouInfo);
    const expanded = mode?.expanded || LO_VIEW.none;
    showDropDown ??= !!dropDownComponent;

    const contentView  = () => useDefaultExpandedView && expanded !== LO_VIEW.none ? <ExpandedView expanded={expanded}/> : children;
    const dropDownView = () => {
        return (
            <Stack id='drop-down' flexGrow={1} spacing={1} mb={1} overflow='hidden'>
                <Stack flexGrow={1} px={1} overflow='auto'>
                    {dropDownComponent}
                </Stack>
            </Stack>
        );
    };

    return (
        <Stack flexGrow={1} overflow='auto'>
                {showDropDown ? dropDownView() : contentView()}
        </Stack>
    );
}


export function ExpandedView ({expanded}) {

    let view;
    if (expanded === LO_VIEW.tables) {
        view = (<TablesContainer mode='both'
                                 closeable={true}
                                 expandedMode={expanded === LO_VIEW.tables}
                                 tableOptions={{help_id: 'main1TSV.table'}}/>
        );
    } else if (expanded === LO_VIEW.xyPlots) {
        const {expandedViewerId} = getExpandedChartProps();
        const chartExpandedMode = expanded === LO_VIEW.xyPlots;
        view = (<ChartsContainer closeable={true}
                                 tbl_group='main'
                                 addDefaultChart={true}
                                 viewerId={chartExpandedMode ? expandedViewerId : undefined}
                                 useOnlyChartsInViewer={chartExpandedMode && expandedViewerId !== DEFAULT_PLOT2D_VIEWER_ID}
                                 expandedMode={chartExpandedMode}/>
        );
    } else {
        view = (
            <ImageExpandedMode key='results-plots-expanded' closeFunc={closeExpanded}/>
        );
    }
    return (
        <Stack {...{direction: 'row', flexGrow: 1, overflow: 'hidden'}}>
            {view}
        </Stack>
    );
}

function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
}


