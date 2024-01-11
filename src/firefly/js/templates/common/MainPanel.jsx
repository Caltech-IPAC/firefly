import {Stack} from '@mui/joy';
import React from 'react';

import {dispatchSetLayoutMode, getLayouInfo, LO_MODE, LO_VIEW} from 'firefly/core/LayoutCntlr.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {TablesContainer} from 'firefly/tables/ui/TablesContainer.jsx';
import {getExpandedChartProps} from 'firefly/charts/ChartsCntlr.js';
import {ChartsContainer} from 'firefly/charts/ui/ChartsContainer.jsx';
import {DEFAULT_PLOT2D_VIEWER_ID} from 'firefly/visualize/MultiViewCntlr.js';
import {ImageExpandedMode} from 'firefly/visualize/iv/ImageExpandedMode.jsx';
import {VersionInfo} from 'firefly/ui/VersionInfo.jsx';

/**
 * Main panel of the FireflyLayout.  It handles the 3 possible views: drop-down, standard, expanded.
 * It also add a footer to the drop-down if footerComponent is set.
 * @param p     props
 * @param p.style   panel root style
 * @param p.dropDownComponent   drop-down component to show when drop-down is visible
 * @param p.footerComponent     footer component to show
 * @param p.showDropDown        set drop-down state
 * @param p.children            main content to show when drop-down is hidden
 * @return {JSX.Element}
 */
export function MainPanel({style, dropDownComponent, footerComponent, showDropDown, children}) {
    const {mode} = useStoreConnector(getLayouInfo);
    const expanded = mode?.expanded || LO_VIEW.none;
    showDropDown ??= !!dropDownComponent;

    const contentView  = () => expanded === LO_VIEW.none ? children : <ExpandedView expanded={expanded}/>;
    const dropDownView = () => {
        return (
            <>
                <Stack {...{direction:'row', flexGrow: 1}}>
                    {dropDownComponent}
                </Stack>
                {footerComponent &&
                    <div id='footer' className='DD-ToolBar__footer'>
                        {footerComponent}
                        <div className='DD-ToolBar__version'>
                            <VersionInfo/>
                        </div>
                    </div>
                }
            </>
        );
    };

    return (
        <Stack direction='row' height={1} style={style}>
            <Stack {...{flexGrow: 1}}>
                {showDropDown ? dropDownView() : contentView()}
            </Stack>
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


