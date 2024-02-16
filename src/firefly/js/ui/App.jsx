import React from 'react';

import {FireflyLayout} from 'firefly/templates/common/FireflyLayout.jsx';
import {DropDownContainer} from 'firefly/ui/DropDownContainer.jsx';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {getLayouInfo} from 'firefly/core/LayoutCntlr.js';
import {AppConfigDrawer} from './AppConfigDrawer.jsx';


export  default function App({children, dropdownPanels, drawerComponent, watchInitArgs, selected, ...props}) {

    const {dropDown} = useStoreConnector(getLayouInfo);

    const {visible, view} = dropDown || {};

    let dropDownComponent = null;       // when dropDown is null, it will show children(results)
    if (visible && view) {
        dropDownComponent = (
            <DropDownContainer visible={true} {...{dropdownPanels, selected, watchInitArgs}}>
                {view}
            </DropDownContainer>
        );
    }

    const drawer= drawerComponent ?? <AppConfigDrawer  appIcon={props.appIcon} appTitle={props.appTitle}/>;
    return (
        <FireflyLayout {...{dropDownComponent, drawerComponent:drawer, ...props}}>
            {children}
        </FireflyLayout>
    );
}


