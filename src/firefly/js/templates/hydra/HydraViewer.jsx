/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {useEffect} from 'react';
import PropTypes, {element, node, object, shape, string} from 'prop-types';
import {Typography} from '@mui/joy';
import {cloneDeep} from 'lodash/lang.js';

import {flux} from '../../core/ReduxFlux.js';
import {
    dispatchSetMenu,
    dispatchOnAppReady,
    dispatchNotifyRemoteAppReady,
    getSearchInfo,
    dispatchAddPreference
} from '../../core/AppDataCntlr.js';
import {getLayouInfo, SHOW_DROPDOWN, LO_VIEW} from '../../core/LayoutCntlr.js';
import {hydraManager} from './HydraManager';
import {getActionFromUrl} from '../../core/History.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';

import {ImageExpandedMode} from '../../visualize/iv/ImageExpandedMode.jsx';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {startTTFeatureWatchers} from '../common/ttFeatureWatchers.js';
import {dispatchSetLayoutMode, LO_MODE} from '../../core/LayoutCntlr';
import {getExpandedChartProps} from '../../charts/ChartsCntlr.js';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../../visualize/MultiViewCntlr.js';
import App from 'firefly/ui/App.jsx';
import {Slot, useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {makeMenuItems, SearchPanel} from 'firefly/ui/SearchPanel.jsx';
import {APP_HINT_IDS, appHintPrefName, LandingPage} from 'firefly/templates/fireflyviewer/LandingPage.jsx';
import {Stacker} from 'firefly/ui/Stacker.jsx';
import {setIf as setIfUndefined} from 'firefly/util/WebUtil.js';


/*
 * This is a viewer.
 */
export function HydraViewer({menu, appTitle, slotProps, ...props}) {


    useEffect(() => {
        dispatchAddSaga(hydraManager);
        startTTFeatureWatchers();
        dispatchOnAppReady(() => {
            onReady(menu, appTitle);
        });
    }, []);

    const mSlotProps = applyLayoutFix({slotProps, props});

    return (
        <App slotProps={mSlotProps} {...props}>
            <ResultSection slotProps={mSlotProps}/>
        </App>
    );
}

/**
 * menu is an array of menu items {label, action, icon, desc, type}.
 * dropdownPanels is an array of additional react elements which are mapped to a menu item's action.
 * @type {{title: *, menu: *, appTitle: *, appIcon: *, dropdownPanels: *, views: *}}
 */
HydraViewer.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    appIcon: PropTypes.element,
    footer: PropTypes.element,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    style: PropTypes.object,
    hasCoverage: PropTypes.bool,
    hasDataProduct: PropTypes.bool,
    slotProps: shape({
        drawer: object,         // use 'component' to define the drawer's ElementType.  the rest will be passed as drawer's props
        banner: object,
        dropdown: object,
        landing: object
    })
};

HydraViewer.defaultProps = {
    appTitle: 'Time Series Viewer'
};

function onReady(menu, appTitle) {
    dispatchSetMenu({menuItems: makeMenuItems(menu)});

    const {hasImages, hasTables, hasXyPlots} = getLayouInfo();
    if (!(hasImages || hasTables || hasXyPlots)) {
        const goto = getActionFromUrl() || {type: SHOW_DROPDOWN};
        if (goto) {
            flux.process(goto);
            // if app didn't start with Results view, app hint for tabs menu is not needed
            goto?.type === SHOW_DROPDOWN && dispatchAddPreference(appHintPrefName(appTitle, APP_HINT_IDS.TABS_MENU), false);
        }
    }
    dispatchNotifyRemoteAppReady();
}

function ResultSection({slotProps}) {

    const layoutInfo = useStoreConnector(getLayouInfo);
    const {showImages, showXyPlots, showTables} = layoutInfo;

    if (!(showImages || showXyPlots || showTables)) return <Slot component={HydraLanding} {...slotProps?.landing}/>;


    const {currentSearch, images} = layoutInfo;
    const {expanded=LO_VIEW.none} = layoutInfo.mode || {};

    const {allSearchItems} = getSearchInfo();
    if (!allSearchItems) return <div/>;
    const {renderStandardView:StandardView, renderExpandedView} = allSearchItems[currentSearch] || {};
    const ExpandedImpl= renderExpandedView || showExpandedView;
    const standard = StandardView ? <StandardView {...{layoutInfo}}/> : <div/>;

    return expanded === LO_VIEW.none ? standard : <ExpandedImpl {...{layoutInfo, expanded, images}}/>;
}

function showExpandedView ({expanded}) {

    let view;
    if (expanded === LO_VIEW.tables) {
        view = (<TablesContainer mode='both'
                         closeable={true}
                         expandedMode={expanded===LO_VIEW.tables}
                         tableOptions={{help_id:'main1TSV.table'}}/>
                );
    } else if (expanded === LO_VIEW.xyPlots) {
        const {expandedViewerId}= getExpandedChartProps();
        const chartExpandedMode= expanded===LO_VIEW.xyPlots;
        view = (<ChartsContainer closeable={true}
                                 tbl_group='main'
                                 addDefaultChart={true}
                                 viewerId={chartExpandedMode ? expandedViewerId : undefined}
                                 useOnlyChartsInViewer={chartExpandedMode && expandedViewerId!==DEFAULT_PLOT2D_VIEWER_ID}
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
        <div style={{ display: 'flex', flexGrow: 1, overflow: 'hidden'}}>
            {view}
        </div>
    );
}

function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
}

export function HydraLanding({icon, title, desc, slotProps={}, ...props} ) {

    const Greetings = () => (
        <Stacker startDecorator={icon} direction='column' alignItems='start'>
            <Typography level='h4'>{title || 'Welcome message here'}</Typography>
            <Typography level='body-md'>{desc || 'Additional description of this application'}</Typography>
        </Stacker>
    );

    const mSlotProps = cloneDeep(slotProps || {});
    setIfUndefined(mSlotProps,'bgMonitorHint.sx.right', 50);
    setIfUndefined(mSlotProps,'topSection.component', Greetings);      // use custom topSection
    setIfUndefined(mSlotProps,'contentSection.sx', {maxWidth: '80em', mx: 'auto'});   // limit page's width

    return <LandingPage slotProps={mSlotProps} {...props}/>;
}

HydraLanding.propTypes = {
    icon: element,
    title: string,
    desc: node,
    ...LandingPage.propTypes,
};


export function applyLayoutFix({slotProps, props}) {

    const mSlotProps = cloneDeep(slotProps || {});

    // defaultView is used when the requested view does not match any of the predefined views in dropdown.
    // with Hydra's SearchPanel, component(view) are defined by a search's renderer, i.g. renderStandardView
    // this is a convenience way to direct all undefined requests to SearchPanel.
    setIfUndefined(mSlotProps,'dropdown.defaultView', <SearchPanel/>);

    // adjust banner for appIcon
    setIfUndefined(mSlotProps,'banner.slotProps.icon.style.marginTop', -43);
    setIfUndefined(mSlotProps,'banner.slotProps.tabs.pl', '120px');

    setIfUndefined(mSlotProps,'landing.icon', props?.appIcon);
    setIfUndefined(mSlotProps,'landing.title', props?.appTitle);
    return mSlotProps;
}