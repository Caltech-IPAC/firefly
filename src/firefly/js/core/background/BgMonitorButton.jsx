import {IconButton} from '@mui/joy';
import React from 'react';

import {getBackgroundInfo, isActive} from './BackgroundUtil.js';
import {BG_MONITOR_SHOW} from './BackgroundCntlr.js';
import {MenuItemButton} from '../../ui/Menu.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import PendingActionsOutlinedIcon from '@mui/icons-material/PendingActionsOutlined';


const showBgMonAction = { type:'COMMAND',
    action: 'background.bgMonitorShow',
    label: 'Background Monitor',
    desc: 'Watch and retrieve background tasks for packaging and catalogs'
};


export function BgMonitorButton ({sx,size}) {

    const {jobs={}} = useStoreConnector(() => getBackgroundInfo());

    const monitoredJobs = Object.values(jobs).filter( (info) => info?.jobInfo?.monitored );
    const isWorking = monitoredJobs.some( (info) => isActive(info) );


    return (
        <MenuItemButton {...{ sx, size:'sm', menuItem:showBgMonAction, isWorking,
            icon:size!=='lg' ? <PendingActionsOutlinedIcon/> : undefined,
            badgeCount: monitoredJobs.length }}/>);
}

