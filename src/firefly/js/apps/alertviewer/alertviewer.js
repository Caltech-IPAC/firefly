import React from 'react';
import QueryStats from '@mui/icons-material/QueryStats';
import {Sheet, Stack, Typography} from '@mui/joy';

import {createRouter} from 'firefly/templates/router/RoutedApp.jsx';
import {FormWatcher} from 'firefly/templates/router/RouteHelper';
import {LandingPage} from 'firefly/templates/fireflyviewer/LandingPage.jsx';
import {AlertIdPanel} from 'firefly/apps/alertviewer/AlertUploadPanel';
import {AlertResultView} from 'firefly/apps/alertviewer/AlertResultView';
import {getAlertViewerCommands} from 'firefly/api/webApiCommands/AlertViewerCommands';
import {dispatchShowDropDown} from 'firefly/core/LayoutCntlr';
import {dispatchOnAppReady} from 'firefly/core/AppDataCntlr';

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
        element: <FormWatcher submitTo='/results'><AlertIdPanel loadInPlace={true}/></FormWatcher>
    },
    {
        path: '/results',
        element: <AlertResultView/>
    },
];

function AlertViewerBottomSection() {
    return (
        <Sheet variant='soft' sx={{pt: 8, pb: 4, px: 2}}>
            <Stack spacing={10} alignItems='center'>
                <Stack spacing={3}>
                    <Stack spacing={2} alignItems='center'>
                        <QueryStats sx={{ width: '6rem', height: '6rem' }} />
                        <Stack spacing={1} alignItems='center'>
                            <Typography level='h2' fontWeight='md'>Getting Started</Typography>
                        </Stack>
                    </Stack>
                    <Stack direction='row' spacing={6}>
                        <Stack spacing={.5} alignItems='center'>
                            <Typography level='title-lg' color='primary'>Navigate to the Alert tab</Typography>
                            <Typography level='body-md'>Search for results via Alert IDs</Typography>
                        </Stack>
                    </Stack>
                </Stack>
                <Stack spacing={2} alignItems='center'>
                    <Typography level='body-lg'>Visualizations of the results will appear in this tab</Typography>
                </Stack>
            </Stack>
        </Sheet>
    );
}

function AlertViewerLanding() {
    return (
        <LandingPage slotProps={{
            topSection: {title: 'Welcome to AlertViewer'},
            bottomSection: {
                component: AlertViewerBottomSection
            }
        }}/>
    );
}


export const alertviewer = {
    init,
    props: {
        appTitle: 'Alert Viewer',
        backgroundMonitor: false,
        getRouter: createRouter(basename, routes),
        appIcon: 'n/a',
        slotProps: {
            drawer: { drawerWidth:'24rem'},
            landing: {component: AlertViewerLanding},
        }
    },
    menu: [
        {label:'Alert', action: 'AlertUpload', primary: true, path:'/upload'},
        {label:'Help', action:'app_data.helpLoad', type:'COMMAND'},
    ],
    webApiCommands: getAlertViewerCommands(),
};


function init() {
    const {pathname, search} = window.location;
    const params = new URLSearchParams(search);
    const hasUrlApi = params.has('api');
    if (!hasUrlApi && (pathname === basename || pathname === `${basename}/`)) {
        dispatchOnAppReady(() => dispatchShowDropDown({view: 'AlertUpload'}));
    }
}
