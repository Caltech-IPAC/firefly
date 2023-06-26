import React from 'react';

import {warningDivId} from 'firefly/ui/LostConnection.jsx';
import {Alerts} from 'firefly/ui/DropDownContainer.jsx';
import {Banner} from 'firefly/ui/Banner.jsx';
import {Menu} from 'firefly/ui/Menu.jsx';
import {getMenu} from 'firefly/core/AppDataCntlr.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';

/**
 * This is the app root component.  It handles banner and alerts.
 * @param p         props
 * @param p.appTitle  application title
 * @param p.appIcon   application icon
 * @param p.altAppIcon    alternative icon
 * @param p.style     root panel style
 * @param p.bannerLeftStyle   style for the left portion of the banner
 * @param p.bannerMiddleStyle style for the middle portion of the banner
 * @param p.children  the main content of the application
 * @return {JSX.Element}
 */
export function FireflyLayout({appTitle, appIcon, altAppIcon, style={}, bannerLeftStyle, bannerMiddleStyle, children}) {

    const menu = useStoreConnector(getMenu);

    return (
        <div id='App' className='rootStyle' style={{display: 'flex', flexDirection: 'column', ...style}}>
            <div>
                <BannerSection {...{menu, appTitle, appIcon, altAppIcon, bannerLeftStyle, bannerMiddleStyle}}/>
                <div id={warningDivId} data-decor='full' className='warning-div center'/>
                <Alerts />
            </div>
            {children}
        </div>
    );
}

function BannerSection({menu, ...rest}) {
    return (
        <Banner key='banner'
                menu={<Menu menu={menu} /> }
                {...rest}
        />
    );
}


