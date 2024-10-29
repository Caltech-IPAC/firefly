import React from 'react';
import {object, bool, element, shape, elementType} from 'prop-types';
import {Stack, Sheet, LinearProgress} from '@mui/joy';
import {pick} from 'lodash/object.js';

import {warningDivId} from 'firefly/ui/LostConnection.jsx';
import {Alerts} from 'firefly/ui/DropDownContainer.jsx';
import {Banner} from 'firefly/ui/Banner.jsx';
import {Menu} from 'firefly/ui/Menu.jsx';
import {dispatchAddPreference, getMenu, isAppReady} from 'firefly/core/AppDataCntlr.js';
import {Slot, useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {MainPanel} from 'firefly/templates/common/MainPanel.jsx';
import {AppConfigDrawer} from '../../ui/AppConfigDrawer.jsx';
import {getActionFromUrl} from 'firefly/core/History';
import {SHOW_DROPDOWN} from 'firefly/core/LayoutCntlr';
import {flux} from 'firefly/core/ReduxFlux';
import {APP_HINT_IDS, appHintPrefName} from 'firefly/templates/fireflyviewer/LandingPage';

/*
 __________________________________________
|  ___________                             |
| |           |       <banner/>            |
|_|           |____________________________|
| |           |                            |
| |           |       <warning|alerts/>    |
|_|           |____________________________|
| |           |                            |
| |           |          <main>            |
| |           |     _________________      |
| | <drawer>  |    |                 |     |
| |  open     |    |     children    |     |
| |  extent   |    |_________________|     |
| |  from     |     _________________      |
| |  top to   |    |                 |     |
| |  bottom   |    |   <dropdown>    |     |
| |           |    |  ____________   |     |
| |           |    | |  content   |  |     |
| |           |    | |____________|  |     |
| |           |    |                 |     |
| |           |    |  <footer/vers>  |     |
| |___________|    |_________________|     |
|__________________________________________|

In the main section, if the dropdown component is not null, it will be displayed.
Otherwise, this component's children will be displayed.
At this level, there are three slots available: drawer, banner, and dropdown.
Use slotProps to customize them.
*/

/**
 * Default Firefly application layout.
 * @param props
 * @param props.footer
 * @param props.useDefaultExpandedView uses the default ExpandedView.  Default to false.
 * @param props.dropDownComponent uses a dropdown component, e.g. DropDownContainer.
 * @param props.sx
 * @param props.slotProps
 * @param props.children
 */
export function FireflyLayout({footer, useDefaultExpandedView,
                                  dropDownComponent, sx, children, slotProps, ...props}) {

    const menu = useStoreConnector(getMenu);
    const isReady = useStoreConnector(isAppReady);

    const bannerProps = pick(props, ['appTitle', 'appIcon']);

    if (!isReady) {         // loading indicator.
        return (
            <Stack width={1} height={1}>
                <Stack m='auto' spacing={1} width={300}>
                    <LinearProgress/>
                </Stack>
            </Stack>
        );
    }

    return (
        <Sheet id='app-root' component={Stack} spacing={1} sx={{position:'absolute', inset:'0', ...sx}}>
            <Stack>
                <BannerSection {...bannerProps} menu={menu} {...slotProps?.banner}/>
                <div id={warningDivId} data-decor='full' className='warning-div center'/>
                <Alerts />
                <Slot component={AppConfigDrawer} slotProps={slotProps?.drawer}/>
            </Stack>
            <MainPanel {...{footer, useDefaultExpandedView, dropDownComponent}}>
                {children}
            </MainPanel>
        </Sheet>
    );
}


FireflyLayout.propTypes = {
    useDefaultExpandedView: bool,
    footer: element,
    dropDownComponent: element,
    sx: object,
    slotProps: shape({
        drawer: shape({
            component: elementType,         // defaults to AppConfigDrawer
            ...AppConfigDrawer.propTypes,
        }),
        banner: object,
        dropdown: object
    })
};



/**
 * Handles banner and login
 * @param p             props
 * @param p.menu        menu info
 * @param p.appTitle    application title
 * @param p.appIcon     application icon
 * @param p.banner      banner props
 * @return {JSX.Element}
 */
function BannerSection({menu, banner, useMenu=true, ...rest}) {
    return (
        <Banner key='banner'
                menu={useMenu? <Menu/> : undefined}
                {...rest}
                {...banner}
        />
    );
}

/**
 * Handles where to go when the application starts. It determines goto location based on URL action, `landing` configuration
 * in menu items, and a default flag (in order). Also, updates hint preferences if not starting with "Results" view.
 * @param p props
 * @param p.menu {MenuItem[]} menu info
 * @param p.appTitle {string} application title
 * @param p.defaultToShowDropdown {boolean} flag to default to the display of dropdown (a tab that is not 'Results')
 */
export const handleInitialAppNavigation = ({menu, appTitle, defaultToShowDropdown = false}) => {
    const urlAction = getActionFromUrl();
    const landingItem = menu.find((item) => item.landing);

    let gotoAction;
    if (urlAction) {
        gotoAction = urlAction; // goto where URL points, can be TABLE_SEARCH, SHOW_DROPDOWN, etc.
    } else if (landingItem) {
        gotoAction = { type: SHOW_DROPDOWN, view: landingItem.action }; // goto menu item with landing=true
    } else if (defaultToShowDropdown) {
        gotoAction = { type: SHOW_DROPDOWN }; // goto 1st shown tab that isn't "Results"
    }

    if (gotoAction) {
        flux.process(gotoAction);

        if (gotoAction.type === SHOW_DROPDOWN) { // app didn't start with Results view, app hint for tabs menu is not needed
            dispatchAddPreference(appHintPrefName(appTitle, APP_HINT_IDS.TABS_MENU), false);
        }
    }
};
