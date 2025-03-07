/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {useEffect} from 'react';
import PropTypes, {node, object, shape, string} from 'prop-types';
import {Divider, Typography} from '@mui/joy';
import {cloneDeep, defaultsDeep} from 'lodash';

import {
    dispatchSetMenu,
    dispatchOnAppReady,
    dispatchNotifyRemoteAppReady,
    getSearchInfo,
} from '../../core/AppDataCntlr.js';
import {getLayouInfo, LO_VIEW} from '../../core/LayoutCntlr.js';
import {hydraManager} from './HydraManager';
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
import {LandingPage} from 'firefly/templates/fireflyviewer/LandingPage.jsx';
import {Stacker} from 'firefly/ui/Stacker.jsx';
import {setIf as setIfUndefined} from 'firefly/util/WebUtil.js';
import {handleInitialAppNavigation} from 'firefly/templates/common/FireflyLayout';
import {hexColorWithAlpha} from 'firefly/util/Color';


/*
 * This is a viewer.
 */
export function HydraViewer({menu, appTitle= 'Time Series Viewer', slotProps, ...props}) {


    useEffect(() => {
        dispatchAddSaga(hydraManager);
        startTTFeatureWatchers();
        dispatchOnAppReady(() => {
            onReady(menu, appTitle);
        });
    }, []);

    const mSlotProps = applyLayoutFix({slotProps, props});

    // defaultView is used when the requested view does not match any of the predefined views in dropdown.
    // with Hydra's SearchPanel, component(view) are defined by a search's renderer, i.g. renderStandardView
    // this is a convenience way to direct all undefined requests to SearchPanel.
    setIfUndefined(mSlotProps,'dropdown.defaultView', <SearchPanel/>);

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

function onReady(menu, appTitle) {
    dispatchSetMenu({menuItems: makeMenuItems(menu)});

    const {hasImages, hasTables, hasXyPlots} = getLayouInfo();
    if (!(hasImages || hasTables || hasXyPlots)) {
        handleInitialAppNavigation({menu, appTitle, defaultToShowDropdown: true});
    }
    dispatchNotifyRemoteAppReady();
}

function ResultSection({slotProps}) {

    const layoutInfo = useStoreConnector(getLayouInfo);
    const {showImages, showXyPlots, showTables} = layoutInfo;

    if (!(showImages || showXyPlots || showTables)) return <Slot component={HydraLanding} slotProps={slotProps?.landing}/>;


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

const HydraAppBranding = ({title, desc}) => (
    <Stacker direction='column' alignItems='start'>
        <Typography level='h4'>{title || 'Welcome message here'}</Typography>
        <Typography level='body-md'>{desc || 'Additional description of this application'}</Typography>
    </Stacker>
);

const defaultCommonProps = ({title, desc}) => ({
    bgMonitorHint: { sx: { right: 50 } },
    topSection: { title, desc }
});


const defaultPropsWithoutBgImage = ({icon}) => ({
    contentSection: { sx: { maxWidth: '80rem', mx: 'auto' } },
    topSection: { component: HydraAppBranding }, // use custom topSection instead of DefaultAppBranding
    bottomSection: { icon }
});

const defaultPropsWithBgImage = ({bgImage}) => ({
    // visually combine topSection & bottomSection into contentSection that can contrast with the bgContainer
    bgContainer: {
        sx: {
            display: 'flex', flexGrow: 1,
            backgroundImage: `url(${bgImage})`, backgroundSize: 'cover', backgroundPosition: 'center'
        }
    },
    contentSection: {
        sx: (theme) => {
            // background image will remain same in both the theme modes, and we need text to contrast with the image,
            // so we create a translucent dark overlay and put text over it as if it's dark theme
            const darkPalette = theme.colorSchemes.dark.palette;
            return {
                maxWidth: '56rem', m: 'auto',
                backgroundColor: hexColorWithAlpha(darkPalette.background.surface.split(', ')?.[1]?.slice(0, -1) ?? '#000', 0.6),
                backdropFilter: 'blur(1px)',
                '.MuiTypography-body-md, .MuiTypography-body-lg': { color: darkPalette.text.secondary },
                '.MuiTypography-h2': { color: darkPalette.text.primary },
                '.MuiTypography-colorPrimary': { color: `rgb(${darkPalette.primary.mainChannel})` },
                '.MuiDivider-root': { backgroundColor: darkPalette.neutral.solidBg }
            };
        },
        divider: <Divider sx={{ width: '4rem', alignSelf: 'center' }} />
    },
    bottomSection: {
        icon: false, // to prevent default being applied (on undefined)
        slotProps: {
            root: { sx: { py: 4, pb: 0, backgroundColor: 'transparent' } }
        }
    }
});


export function HydraLanding({icon, title, desc, bgImage, slotProps={}, ...props} ) {
    const mSlotProps = cloneDeep(slotProps);
    defaultsDeep(mSlotProps, defaultCommonProps({title, desc}));

    bgImage
        ? defaultsDeep(mSlotProps, defaultPropsWithBgImage({bgImage}))
        : defaultsDeep(mSlotProps, defaultPropsWithoutBgImage({icon}));

    return <LandingPage slotProps={mSlotProps} {...props}/>;
}

HydraLanding.propTypes = {
    icon: node,
    title: string,
    desc: node,
    bgImage: string,
    ...LandingPage.propTypes,
};


export function applyLayoutFix({slotProps, props}) {
    const mSlotProps = cloneDeep(slotProps || {});
    defaultsDeep(mSlotProps, {
        banner: {
            slotProps: {
                // Adjust banner for appIcon
                icon: {
                    style: { marginTop: -40 }, // Won't take precedence if defined in sx
                    sx: { color: 'primary.softActiveColor' } // Same as active tab's font color
                },
                tabs: { pl: '120px' }
            }
        },
        landing: { title: props?.appTitle }
    });

    return mSlotProps;
}