import React from 'react';

import {FireflyLayout} from 'firefly/templates/common/FireflyLayout.jsx';
import {DropDownContainer} from 'firefly/ui/DropDownContainer.jsx';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {getLayouInfo} from 'firefly/core/LayoutCntlr.js';


/*
A wrapper component that uses FireflyLayout with DropDownContainer as its dropdown component.
This component is commonly used across Firefly-based applications.
It relies on LayoutCntrl to determine the visibility of the dropdown.
This abstraction is needed because route-base applications use route to
determine what gets rendered.  Moving LayoutCntrl out of FireflyLayout allow route-based
apps to use it while still having full control of when dropdown is rendered.
*/

export  default function App({children, dropdownPanels, watchInitArgs, selected, slotProps, ...props}) {

    const {dropDown} = useStoreConnector(getLayouInfo);

    const {visible, view} = dropDown || {};

    let dropDownComponent = null;       // when dropDown is null, it will show children(results)
    if (visible && view) {
        dropDownComponent = (
            <DropDownContainer visible={true} {...{dropdownPanels, selected, watchInitArgs, ...slotProps?.dropdown}}>
                {view}
            </DropDownContainer>
        );
    }

    return (
        <FireflyLayout {...{dropDownComponent, slotProps, ...props}}>
            {children}
        </FireflyLayout>
    );
}


App.propTypes = {
    ...FireflyLayout.propTypes
};