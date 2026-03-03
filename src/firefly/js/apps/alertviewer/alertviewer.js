import React from 'react';

import {createRouter} from 'firefly/templates/router/RoutedApp.jsx';
import {UploadPanel} from 'firefly/templates/alert/AlertViewer';
import {AlertResultView} from 'firefly/templates/alert/AlertResultView';
import {FormWatcher} from 'firefly/templates/router/RouteHelper';


const basename = '/firefly/alertviewer';
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
                desc: 'Access data from the Spectro-Photometer for the History of the Universe, Epoch of Reionization and Ices Explorer',
                bgImage: null,
            }
        }
    },
    menu: [
        {label:'Upload', action: 'AlertUpload', path:'/upload'},
        {label:'Help', action:'app_data.helpLoad', type:'COMMAND'},
    ],
    webApiCommands: {},
};


function init() {}