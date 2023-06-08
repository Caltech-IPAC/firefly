import {warningDivId} from 'firefly/ui/LostConnection.jsx';
import {DropDownContainer} from 'firefly/ui/DropDownContainer.jsx';
import React, {useEffect} from 'react';
import {pickBy} from 'lodash';
import {Banner} from 'firefly/ui/Banner.jsx';
import {Menu} from 'firefly/ui/Menu.jsx';
import {dispatchSetLayoutInfo, dispatchSetLayoutMode, getLayouInfo, LO_MODE, LO_VIEW} from 'firefly/core/LayoutCntlr.js';
import {getMenu} from 'firefly/core/AppDataCntlr.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {dispatchAddActionWatcher} from 'firefly/core/MasterSaga.js';
import {dispatchOnAppReady} from 'firefly/core/AppDataCntlr.js';
import {TABLE_SEARCH} from 'firefly/tables/TablesCntlr.js';
import {TablesContainer} from 'firefly/tables/ui/TablesContainer.jsx';
import {getExpandedChartProps} from 'firefly/charts/ChartsCntlr.js';
import {ChartsContainer} from 'firefly/charts/ui/ChartsContainer.jsx';
import {DEFAULT_PLOT2D_VIEWER_ID} from 'firefly/visualize/MultiViewCntlr.js';
import {ImageExpandedMode} from 'firefly/visualize/iv/ImageExpandedMode.jsx';


export function FireflyLayout(props) {
    const {appTitle, appIcon, altAppIcon, footer, dropdownPanels=[], style, bannerLeftStyle, bannerMiddleStyle, defDropDown, children} = props;
    const layoutInfo = useStoreConnector(getLayouInfo);
    const menu = useStoreConnector(getMenu);
    const {dropDown, mode} = layoutInfo;
    const {visible=!!defDropDown, view=defDropDown, initArgs} = dropDown || {};
    const {expanded=LO_VIEW.none} = mode || {};

    useEffect(()=> {
        dispatchOnAppReady(() => {
            dispatchAddActionWatcher({actions:[TABLE_SEARCH],
                callback: (action, cancelSelf) => {
                    dispatchSetLayoutInfo({dropDown:{visible: false}});
                }
            });
        });
    },[]);


    const mainContent = expanded === LO_VIEW.none ? children : showExpandedView({expanded});

    return (
        <div id='App' className='rootStyle' style={style}>
            <header>
                <BannerSection {...{menu, appTitle, appIcon, altAppIcon, bannerLeftStyle, bannerMiddleStyle}}/>
                <div id={warningDivId} data-decor='full' className='warning-div center'/>
                <DropDownContainer
                    key='dropdown'
                    footer={footer}
                    visible={!!visible}
                    selected={view}
                    initArgs={initArgs}
                    {...{dropdownPanels} } />
            </header>
            <main style={{position: 'relative', padding: 0}}>
                {mainContent}
                <div id='for-popup' style={{zIndex:1}}/>
            </main>
        </div>
    );

}


function showExpandedView ({expanded}) {

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
            <ImageExpandedMode
                key='results-plots-expanded'
                closeFunc={closeExpanded}/>
        );
    }
    return (
        <div style={{display: 'flex', flexGrow: 1, overflow: 'hidden'}}>
            {view}
        </div>
    );
}

function BannerSection(props) {
    const {menu, ...rest} = pickBy(props);
    return (
        <Banner key='banner'
                menu={<Menu menu={menu} /> }
                {...rest}
        />
    );
}

function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
}


