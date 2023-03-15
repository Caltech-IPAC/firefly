
import {getBackgroundInfo, isActive} from './BackgroundUtil.js';
import {BG_MONITOR_SHOW} from './BackgroundCntlr.js';
import {makeMenuItem} from '../../ui/Menu.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';


const showBgMonAction = { type:'COMMAND',
    action: BG_MONITOR_SHOW,
    label: 'Background Monitor',
    desc: 'Watch and retrieve background tasks for packaging and catalogs'
};


export function BgMonitorButton () {

    const {jobs={}} = useStoreConnector(() => getBackgroundInfo());

    const monitoredJobs = Object.values(jobs).filter( (info) => info?.jobInfo?.monitored );
    const working = monitoredJobs.some( (info) => isActive(info) );

    return makeMenuItem(showBgMonAction, false, working, monitoredJobs.length);
}

