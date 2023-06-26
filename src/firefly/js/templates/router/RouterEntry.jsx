
import React from 'react';
import {RouterProvider} from 'react-router-dom';
import ReactDOM from 'react-dom';
import {dispatchNotifyRemoteAppReady, dispatchSetMenu} from 'firefly/core/AppDataCntlr.js';

export const ROUTER = 'router';

export function routerEntry(props) {
    const {div, getRouter} = props;
    const divEl= document.getElementById(div);

    window.firefly.ignoreHistory = true;
    dispatchSetMenu({menuItems: props.menu});
    dispatchNotifyRemoteAppReady();

    const view = (
        <RouterProvider router={getRouter(props)} />
    );

    ReactDOM.render( view, divEl);

}

