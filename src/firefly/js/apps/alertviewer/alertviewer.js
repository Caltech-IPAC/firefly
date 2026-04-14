import React from 'react';

import {createRouter} from 'firefly/templates/router/RoutedApp.jsx';
import {FormWatcher} from 'firefly/templates/router/RouteHelper';
import {UploadPanel} from 'firefly/apps/alertviewer/AlertUploadPanel';
import {AlertResultView} from 'firefly/apps/alertviewer/AlertResultView';
import {getAlertViewerCommands} from 'firefly/api/webApiCommands/AlertViewerCommands';

function getBasename() {
    const p = window.location.pathname || '';
    //strip route suffixes if present
    const noRoute = p.replace(/\/(upload|results)(\/.*)?$/, '');
    //remove trailing slash
    return noRoute.endsWith('/') ? noRoute.slice(0, -1) : noRoute;
}


const basename = getBasename(); //'/firefly/alertviewer';
const routes = [
    {
        path: '/upload',
        element: <FormWatcher submitTo='/results'><UploadPanel/></FormWatcher>
    },
    {
        path: '/results',
        element: <AlertResultView/>
    },
];


export const alertviewer = {
    init,
    props: {
        appTitle: 'Alert Viewer',
        getRouter: createRouter(basename, routes),
        appIcon: 'n/a',
        slotProps: {
            drawer: { drawerWidth:'24rem'},
            landing: {
                desc: 'Some information about the alert viewer here. This is the landing page. This text will be changed to something more useful in the future.',
                bgImage: null,
            }
        }
    },
    menu: [
        {label:'Alert', action: 'AlertUpload', primary: true, path:'/upload'},
        {label:'Help', action:'app_data.helpLoad', type:'COMMAND'},
    ],
    webApiCommands: getAlertViewerCommands(),
};


function init() {}
