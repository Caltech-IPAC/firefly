/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {cloneDeep} from 'lodash/lang.js';

import {
    dispatchSetMenu,
    dispatchOnAppReady, dispatchNotifyRemoteAppReady, getAppOptions,
} from '../../core/AppDataCntlr.js';
import {LO_VIEW, getLayouInfo} from '../../core/LayoutCntlr.js';
import {AppConfigDrawer} from '../../ui/AppConfigDrawer.jsx';
import {getActiveRowToImageDef} from '../../visualize/saga/ActiveRowToImageWatcher.js';
import {getCatalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';
import {getMocWatcherDef} from '../../visualize/saga/MOCWatcher.js';
import {getUrlLinkWatcherDef} from '../../visualize/saga/UrlLinkWatcher.js';
import {layoutManager} from './FireflyViewerManager.js';
import {LayoutChoiceVisualAccordion} from './LayoutChoice.jsx';
import {TriViewPanel} from './TriViewPanel.jsx';
import {startImagesLayoutWatcher} from '../../visualize/ui/TriViewImageSection.jsx';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {getImageMasterData} from '../../visualize/ui/AllImageSearchConfig.js';
import {getWorkspaceConfig, initWorkspace} from '../../visualize/WorkspaceCntlr.js';

import {getAllStartIds, getObsCoreWatcherDef, startTTFeatureWatchers} from '../common/ttFeatureWatchers';
import App from 'firefly/ui/App.jsx';
import {setIf as setIfUndefined} from 'firefly/util/WebUtil.js';
import {handleInitialAppNavigation} from 'firefly/templates/common/FireflyLayout';

/**
 * This FireflyViewer is a generic application with some configurable behaviors.
 * The application is separated into these major parts:  banner, menu, searches, and results.
 * The props below allow you to alter their default behaviors.
 *
 * <b>Props</b>
 * <li><b>title</b>:  This title will appears at center top of the results area. Defaults to 'FFTools'. </li>
 * <li><b>menu</b>:  menu is an array of menu items {label, action, icon, desc, type}.  Leave type blank for dropdown.  If type='COMMAND', it will fire the action without triggering dropdown.</li>
 * <li><b>appTitle</b>:  The title of the FireflyViewer.  It will appears at top left of the banner. Defaults to 'Firefly'. </li>
 * <li><b>appIcon</b>:  A react element rendered at where appIcon should appear. </li>
 * <li><b>footer</b>:   A react elements to place on the footer when the menu drop down. </li>
 * <li><b>dropdownPanels</b>:  An array of additional react elements which are mapped to a menu item's action. </li>
 * <li><b>views</b>:  The type of result view.  Choices are 'images', 'tables', and 'xyPlots'.  They can be combined with ' | ', i.e.  'images | tables'</li>
 *
 */
export function FireflyViewer ({menu, options, views='images | tables | xyplots', showViewsSwitch, leftButtons,
                                   centerButtons, rightButtons, normalInit=true, appTitle='Firefly',
                                   landingPage, slotProps, apiHandlesExpanded, ...appProps}){

    useEffect(() => {
        getImageMasterData();
        const eviews = LO_VIEW.get(views) || LO_VIEW.none;
        startTTFeatureWatchers(getAllStartIds());
        startTTFeatureWatchers(
            [
                getMocWatcherDef().id,
                getCatalogWatcherDef().id,
                getUrlLinkWatcherDef().id,
                getActiveRowToImageDef().id,
                getAppOptions().enableObsCoreDownload && getObsCoreWatcherDef().id,
            ]
        );
        if (eviews.has(LO_VIEW.images) ) startImagesLayoutWatcher();
        dispatchAddSaga(layoutManager,{views,apiHandlesExpanded});
        if (getWorkspaceConfig()) { initWorkspace(); }
    }, []);

    useEffect(() => {
        dispatchOnAppReady(() => onReady({menu, options, normalInit, appTitle}));
    }, []);

    const FireflySidebar= (props) => (
        <AppConfigDrawer {...props}>
            <LayoutChoiceVisualAccordion/>
        </AppConfigDrawer>
    );

    const mSlotProps = cloneDeep(slotProps || {});
    setIfUndefined(mSlotProps, 'drawer.component', FireflySidebar);
    setIfUndefined(mSlotProps, 'banner.enableVersionDialog', true);

    return (
        <App slotProps={mSlotProps} {...{views, ...appProps}}>
            <DynamicResults {...{views, showViewsSwitch, landingPage, leftButtons, centerButtons,
                rightButtons}}/>
        </App>
    );
}

/**
 * menu is an array of menu items {label, action, icon, desc, type}.
 * dropdownPanels is an array of additional react elements which are mapped to a menu item's action.
 * @type {{title: *, menu: *, appTitle: *, appIcon: *, dropdownPanels: *, views: *}}
 */
FireflyViewer.propTypes = {
    title: PropTypes.string,
    menu: PropTypes.arrayOf(PropTypes.object),
    appTitle: PropTypes.string,
    appIcon: PropTypes.element,
    showUserInfo: PropTypes.bool,
    footer: PropTypes.element,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    views: PropTypes.string,     // combination of LO_VIEW separated by ' | '.  ie. 'images | tables'.
    style: PropTypes.object,
    showViewsSwitch: PropTypes.bool,
    leftButtons: PropTypes.arrayOf( PropTypes.func ),
    centerButtons: PropTypes.arrayOf( PropTypes.func ),
    rightButtons: PropTypes.arrayOf( PropTypes.func ),
    options: PropTypes.object,
    normalInit: PropTypes.bool
};


function onReady({menu, options={}, normalInit, appTitle}) {
    if (menu) {
        const {backgroundMonitor= true}= options;
        dispatchSetMenu({menuItems: menu, showBgMonitor:backgroundMonitor});
    }
    const {hasImages, hasTables, hasXyPlots} = getLayouInfo();
    if (normalInit && (!(hasImages || hasTables || hasXyPlots))) {
        handleInitialAppNavigation({menu, appTitle});
    }
    dispatchNotifyRemoteAppReady();
}

function DynamicResults(props) {
    var {views, ...rest} = props;
    if (LO_VIEW.get(views)) {
        return <TriViewPanel {...rest}/>;
    }
}
DynamicResults.propTypes = {
    views: PropTypes.oneOfType([
                    PropTypes.string,
                    PropTypes.object]),
    showViewsSwitch: PropTypes.bool,
    leftButtons: PropTypes.arrayOf( PropTypes.func ),
    centerButtons: PropTypes.arrayOf( PropTypes.func ),
    rightButtons: PropTypes.arrayOf( PropTypes.func )
};

