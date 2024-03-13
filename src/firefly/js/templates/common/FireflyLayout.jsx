import React from 'react';
import {object, bool, element, shape, elementType} from 'prop-types';
import {Stack, Sheet, LinearProgress} from '@mui/joy';
import {pick} from 'lodash/object.js';

import {warningDivId} from 'firefly/ui/LostConnection.jsx';
import {Alerts} from 'firefly/ui/DropDownContainer.jsx';
import {Banner} from 'firefly/ui/Banner.jsx';
import {Menu} from 'firefly/ui/Menu.jsx';
import {getMenu, isAppReady} from 'firefly/core/AppDataCntlr.js';
import {Slot, useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {MainPanel} from 'firefly/templates/common/MainPanel.jsx';
import {AppConfigDrawer} from '../../ui/AppConfigDrawer.jsx';

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
function BannerSection({menu, banner, ...rest}) {
    return (
        <Banner key='banner'
                menu={<Menu/>}
                {...rest}
                {...banner}
        />
    );
}


