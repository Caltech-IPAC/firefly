import React from 'react';

import {getBackgroundInfo, isActive} from './BackgroundUtil.js';
import {BG_MONITOR_SHOW} from './BackgroundCntlr.js';
import {MenuItem} from '../../ui/Menu.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';


const showBgMonAction = { type:'COMMAND',
    action: BG_MONITOR_SHOW,
    label: 'Background Monitor',
    desc: 'Watch and retrieve background tasks for packaging and catalogs'
};


export function BgMonitorButton ({sx}) {

    const {jobs={}} = useStoreConnector(() => getBackgroundInfo());

    const monitoredJobs = Object.values(jobs).filter( (info) => info?.jobInfo?.monitored );
    const isWorking = monitoredJobs.some( (info) => isActive(info) );

    return (<MenuItem {...{
        sx,
        size:'sm',
        menuItem:showBgMonAction,
        isWorking,
        badgeCount: monitoredJobs.length
    }}/>);
}

