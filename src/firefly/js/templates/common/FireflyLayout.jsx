import React from 'react';
import {object, bool, element} from 'prop-types';
import {Stack, Sheet, LinearProgress} from '@mui/joy';
import {pick} from 'lodash/object.js';

import {warningDivId} from 'firefly/ui/LostConnection.jsx';
import {Alerts} from 'firefly/ui/DropDownContainer.jsx';
import {Banner} from 'firefly/ui/Banner.jsx';
import {Menu} from 'firefly/ui/Menu.jsx';
import {getMenu, isAppReady} from 'firefly/core/AppDataCntlr.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {MainPanel} from 'firefly/templates/common/MainPanel.jsx';
import {AppConfigDrawer} from '../../ui/AppConfigDrawer.jsx';

/**
 * Default Firefly application layout.
 * <banner/>                // menu, icons, title, etc
 * <warning|alerts/>
 * <main>                   // main section of the application.  displays either dropdown, results or expandedView.
 *     <dropdown>
 *         <content>
 *         <footer + version>
 *     </dropdown>
 *     <results/>
 * </main>
 * @param props
 * @param props.footer
 * @param props.useDefaultExpandedView uses the default ExpandedView.  Default to false.
 * @param props.dropDownComponent uses a dropdown component, e.g. DropDownContainer.
 * @param props.sx
 * @param props.children
 */
export function FireflyLayout({footer, useDefaultExpandedView, drawerComponent, dropDownComponent, sx, children, ...props}) {

    const menu = useStoreConnector(getMenu);
    const isReady = useStoreConnector(isAppReady);

    const bannerProps = pick(props, [
        'menu', 'appTitle', 'appIcon', 'altAppIcon', 'bannerLeftStyle','bannerMiddleStyle', 'enableVersionDialog', 'showTitleOnBanner'
    ]);

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
                <BannerSection {...bannerProps} menu={menu}/>
                <div id={warningDivId} data-decor='full' className='warning-div center'/>
                <Alerts />
                {drawerComponent}
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
    drawerComponent: element,
    dropDownComponent: element,
    sx: object
};



/**
 * Handles banner and login
 * @param p             props
 * @param p.appTitle    application title
 * @param p.appIcon     application icon
 * @param p.altAppIcon  alternative icon
 * @param p.bannerLeftStyle   style for the left portion of the banner
 * @param p.bannerMiddleStyle style for the middle portion of the banner
 * @param p.enableVersionDialog show version dialog when banner icon is clicked
 * @return {JSX.Element}
 */
function BannerSection({menu, ...rest}) {
    return (
        <Banner key='banner'
                menu={<Menu/> }
                {...rest}
        />
    );
}


