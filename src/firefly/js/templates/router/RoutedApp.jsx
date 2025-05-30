import React, {useEffect} from 'react';
import {createBrowserRouter, useOutlet} from 'react-router-dom';

import {FireflyLayout} from '../common/FireflyLayout.jsx';
import {IrsaFooterSmall} from '../../ui/IrsaFooter.jsx';
import {useDropdownRoute} from './RouteHelper.jsx';
import {DropDownContainer} from '../../ui/DropDownContainer.jsx';
import {startTTFeatureWatchers} from '../common/ttFeatureWatchers.js';
import {dispatchNotifyRemoteAppReady, dispatchOnAppReady, dispatchSetMenu} from '../../core/AppDataCntlr.js';
import {applyLayoutFix, HydraLanding} from '../hydra/HydraViewer.jsx';
import {Slot} from '../../ui/SimpleComponent.jsx';
import {JobMonitor, jobMonitorPath} from '../../core/background/JobMonitor';


/*

An application that uses FireflyLayout and its common components such as DropDownContainer, Menu, Banner, LandingPage, etc.
For a better understanding of its responsibilities, refer to the FireflyLayout documentation.

A significant distinction of RoutedApp lies in its rendering mechanism. Typically, routed apps render based on their paths.
This functionality remains intact, extending up to what FireflyLayout is responsible for.
However, RoutedApp also support Firefly's layout controller (LayoutCntlr), which controls the display of Menu state, dropdown state,
and the MainPanel (results) of the application.
RoutedApp achieves this by synchronizing and mapping Firefly's layout information with the application's defined routes.


Several key concepts to understand when using RoutedApp:

1. The route define what get rendered.  Therefore, RoutedApp should be at the root of all routes.
2. React Router introduces the notion of child routes and the element rendered within the parent's element, referred as 'outlet' in the code.
   Examine this code snippet to understand the differences between an outlet and children.

    const routes = [
        {
            path: '/',
            element: <RoutedApp />,
            children: [
                {
                    path: '/searchByPosition',
                    element: <SearchByPosition/>
                },
            ]
        },
        {
            path: '/somethingElse',
            element: <RoutedApp> <SomethingElse/> </RoutedApp>,
        }
    ];

In this example, `<SearchByPosition/>` serves as an outlet of RoutedApp since its route is defined as a child of RoutedApp.
'<SomethingElse/>' appears as children because it's declared inside of RoutedApp like regular react component.


RoutedApp's helpers in RouteHelper:

routeEntry:  The entry point of the application, similar to FFEntryPoint or IrsaEntry.
FormWatcher:  Wrap around FormPanel.  Necessary whe the application feature more than one result route.  Otherwise, use 'mainPanel'.
useDropdownRoute:  The hook marrying route behavior on top of Firefly's layout controller.

*/
export  default function RoutedApp({slotProps, menu, mainPanel, children, dropdownPanels, ...props}) {

    useEffect(() => {
        startTTFeatureWatchers();
        dispatchSetMenu({menuItems: menu});
        dispatchOnAppReady(() => {
            dispatchNotifyRemoteAppReady();
        });
    }, []);

    const [visible, search] = useDropdownRoute();
    const outlet = useOutlet();

    const mSlotProps = applyLayoutFix({slotProps, props});

    let dropDownComponent = null;
    if (visible && outlet) {
        dropDownComponent = (
            <DropDownContainer visible={true} dropdownPanels={dropdownPanels} footer={<IrsaFooterSmall/>} {...mSlotProps?.dropdown}>
                {outlet}
            </DropDownContainer>
        );
    }

    const landingPage = <Slot component={HydraLanding} slotProps={mSlotProps?.landing}/>;

    const results = outlet || children || (search ? mainPanel || landingPage : landingPage);

    return (
        <FireflyLayout footer={<IrsaFooterSmall/>} useDefaultExpandedView={true}
                       slotProps={mSlotProps}
                       {...{dropDownComponent, ...props}}
        >
            {results}
        </FireflyLayout>
    );
}

RoutedApp.propTypes = FireflyLayout.propTypes;

export function createRouter(basename, routes=[]) {
    return (props) => {
        const allRoutes = [
            {
                path: '/',
                element: <RoutedApp {...props}/>,
                children: [
                    {   path: jobMonitorPath,
                        element: <JobMonitor/>,
                    },
                    ...routes,
                ]
            }
        ];
        return createBrowserRouter(allRoutes, {basename});
    };
}