/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {isEmpty, isNil} from 'lodash';

import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {InputField} from '../../ui/InputField.jsx';
import Validate from '../../util/Validate.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../ComponentCntlr.js';
import {getBackgroundInfo, isActive, isAborted, isDone, isSuccess, getJobInfo, canCreateScript
} from './BackgroundUtil.js';
import {dispatchBgJobInfo, dispatchJobRemove, dispatchBgSetEmailInfo, dispatchJobCancel} from './BackgroundCntlr.js';
import {showScriptDownloadDialog} from '../../ui/ScriptDownloadDialog.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {download} from '../../util/fetch';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {updateSet} from '../../util/WebUtil.js';
import {getRequestFromJob} from '../../tables/TableRequestUtil.js';
import {showJobInfo} from './JobInfo.jsx';
import {Checkbox, Stack, ChipDelete, Typography, Box, Link, Tooltip, LinearProgress} from '@mui/joy';

import LOADING from 'html/images/gxt/loading.gif';
import CANCEL from 'html/images/stop.gif';
import DOWNLOAED from 'html/images/blue_check-on_10x10.gif';
import FAILED from 'html/images/exclamation16x16.gif';
import INFO from 'html/images/info-icon.png';
import CompleteButton from 'firefly/ui/CompleteButton';


export function showBackgroundMonitor(show=true) {
    const content= (
        <PopupPanel title={'Background Monitor'} >
            <BackgroundMonitor />
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('BackgroundMonitor', content);
    if (show) {
        dispatchShowDialog('BackgroundMonitor');
    } else {
        dispatchHideDialog('BackgroundMonitor');
    }
}

 function BackgroundMonitor() {

    const {jobs={}, email, enableEmail, help_id} = useStoreConnector(() => getBackgroundInfo());

    const items = Object.values(jobs)
                    .filter((job) => job.jobInfo?.monitored)
                    .map( (job) =>  job.jobInfo?.type === 'PACKAGE' ?
                        <PackageJob key={job.jobId} jobInfo={job} /> :
                        <SearchJob key={job.jobId} jobInfo={job} />
                    );

    return (
        <Stack justifyContent='space-between' spacing={1} alignItems='left' minHeight={200}
                    minWidth={330} maxWidth={590} sx={{resize: 'both', overflow: 'auto'}}>
            <Box {...{flexGrow: 1, overflow: 'auto'}}>
                {!isEmpty(items) && items}
            </Box>
            <BgFooter {...{help_id, email, enableEmail}}/>
        </Stack>
    );
}

function BgFooter ({help_id='basics.bgmon', email, enableEmail}) {

    const onHide = () => {
        showBackgroundMonitor(false);
    };

    const onEmailChanged = (v) => {
        if (v?.valid) {
            if (email !== v.value) dispatchBgSetEmailInfo({email: v.value});
        }
    };

    enableEmail = isNil(enableEmail) ? !!email : enableEmail;
    const toggleEnableEmail = (e) => {
        const checked = e.target.checked;
        const m_email = checked ? email : '';
        dispatchBgSetEmailInfo({email: m_email, enableEmail: checked});

    };

      // TODO: email is commented out, when we make a notification plan we can re-add it.

    return (
        <Stack direction='row' alignItems='flex-start' justifyContent='space-between'>
            <Tooltip title='Hide Background Monitor'>
                <CompleteButton color='primary' onSuccess={onHide} text={'Hide'} />
            </Tooltip>
            <Stack>
                {/*<Checkbox p={1} size='sm' checked={enableEmail} onChange={toggleEnableEmail} label={'Enable email notification'}/>*/ }
                {enableEmail &&
                    <Stack direction='row' alignItems='center' width={200}>
                        <Typography component='label' level='title-md' sx={{mr: 1}}>
                            Email:
                        </Typography>
                        <InputField
                            validator={Validate.validateEmail.bind(null, 'an email field')}
                            tooltip='Enter an email to be notified when a process completes.'
                            slotProps={{label:{sx:{ml:0}}}}
                            value={email}
                            placeholder='Enter an email to get notification'
                            sx={{width:170}}
                            onChange={onEmailChanged}
                            actOn={['blur','enter']}
                        />
                    </Stack>
                }
            </Stack>
            <HelpIcon helpId={help_id}/>
        </Stack>
        );
}

function SearchJob({jobInfo}) {
    const {email} = jobInfo;
    return (
        <Box {...{m:0.5, p:0.5}}>
            <JobHeader jobInfo={jobInfo}/>
            {email && (
                <Stack {...{direction:'row', justifyContent:'space-between', alignItems:'center', width: '100%', p:0.5}}>
                    <Typography level='body-sm'>Notification email sent</Typography>
                </Stack>
            )}
        </Box>
    );
}

function PackageJob({jobInfo}) {
    const {jobId, results, DATA_SOURCE, email, jobInfo:others} = jobInfo;
    const label = others?.label || 'unknown';

    const script = canCreateScript(jobInfo) && isSuccess(jobInfo) && results?.length > 1;

    const items = results?.map( (url, idx) => <PackageItem key={'multi-' + idx} jobId={jobInfo.jobId} index={idx} />);

    return (
        <Stack {...{m: 0.5, p: 0.5, justifyContent:'flex-end'}} >
            <JobHeader jobInfo={jobInfo}/>
            {items?.length > 1 &&
                <Stack {...{ml:2.5}}> {items} </Stack>
            }
            {(email || script) &&
                <Stack {...{direction:'row', justifyContent:'space-between', alignItems:'center', width:'100%', p:0.5}}>
                    {email ? <Typography level='body-sm'>Notification email sent</Typography> : null}
                    {script && (
                        <Link component={'button'} onClick={() => showScriptDownloadDialog({ jobId, label, DATA_SOURCE })}>
                            Get Download Script
                        </Link>
                    )}
                </Stack>
            }
        </Stack>
    );
}

function JobHeader({jobInfo}) {
    const jobId = jobInfo?.jobId;
    const label = jobInfo?.jobInfo?.label;

    const removeBgStatus = () => {
        dispatchJobRemove(jobId);
    };
    const doCancel = () => {
        dispatchJobCancel(jobId);
    };
    const showInfo = () => {
        showJobInfo(jobId);
    };
    return (
        <Stack {...{alignItems:'center', flexDirection:'row', width: '100%' }}>
            <Stack {...{direction:'row', alignItems:'center', overflow: 'hidden', title:{label}, justifyContent:'flex-start'}}>
                <Typography level='title-md' alignItems='center' sx={{ml: 0.375, whiteSpace: 'nowrap', overflow: 'hidden',
                    textOverflow: 'ellipsis'}}>{label}
                </Typography>
                <Box display={'flex'} height={20} sx={{'&:hover img': {cursor: 'pointer'}, ml:1}}>
                    <img src={INFO} onClick={showInfo}/>
                </Box>
            </Stack>
            <Stack direction='row' spacing={2} alignItems='flex-end' sx={{pl: 1, flexShrink: 0, flexGrow: 1}} justifyContent={'flex-end'}>
                <JobProgress jobInfo={jobInfo}/>
                <Box>
                    {isActive(jobInfo) &&
                        <Box display={'flex'} height={14} width={14} sx={{'&:hover img': {cursor: 'pointer'}, ml:3}}>
                            <img src={CANCEL} onClick={doCancel} title='Abort this job.'/>
                        </Box>
                    }
                    {isDone(jobInfo) &&
                        <Tooltip placement='left' title='Remove Background Job'>
                            <ChipDelete {...{ onClick: () => removeBgStatus(), sx:{'--Chip-deleteSize': '1.5rem'} }}/>
                        </Tooltip>
                    }
                </Box>
            </Stack>
        </Stack>
    );
}

function JobProgress({jobInfo}) {
    const {jobId} = jobInfo;
    const {progressDesc, progress, type} = jobInfo.jobInfo || {};
    if (isActive(jobInfo)) {
        return (
            <Box {...{alignItems:'center', width:150, flex:2}}>
                <Typography sx={{mr: 1}}>{progressDesc}</Typography>
                <LinearProgress determinate={true} value={progress} />
            </Box>
        );
    } else if (isAborted(jobInfo)) {
        return <Typography>{jobInfo?.error || 'Job aborted'}</Typography>;
    } else if (isSuccess(jobInfo)) {
        if (type === 'PACKAGE') {
            return jobInfo?.results?.length === 1 ? <PackageItem {...{SINGLE:true, jobId, index:0}} /> : <div/> ;
        } else {
            const showTable = () => {
                const request = getRequestFromJob(jobInfo.jobId);
                request && dispatchTableSearch(request);
            };
            return (<Link onClick={() => showTable()}>
                Show results
                </Link>);
        }
    } else {
        return <Typography  color={'danger'}>Job Failed</Typography>;
    }
}

function PackageItem({SINGLE, jobId, index}) {
    const jobInfo = useStoreConnector(() => getJobInfo(jobId));

    const {parameters, downloadState} = jobInfo;
    const dlreq = JSON.parse(parameters?.downloadRequest);
    const dlState = downloadState?.[index];

    const doDownload = () => {
        const url = jobInfo?.results?.[index]?.href;
        if (!url) {
            showInfoPopup('Bad URL. Cannot download');
            return;
        }
        dispatchBgJobInfo( updateSet(jobInfo, ['downloadState',index], 'WORKING') );
        download(url).then( () => {
            dispatchBgJobInfo( updateSet(jobInfo, ['downloadState',index], 'DONE') );
        }).catch(() => {
            dispatchBgJobInfo( updateSet(jobInfo, ['downloadState',index], 'FAIL') );
        });
    };
    const dlmsg = SINGLE ? 'Download Now' : `Download Part #${index+1}`;


    const action = dlreq?.WS_DEST_PATH
        ? (<Link>
            Downloaded to Workspace
        </Link>)
        : (<Link onClick={() => doDownload()}>
            {dlmsg}
        </Link>);
    const showState = !SINGLE || (SINGLE && dlState);

    return (
        <Stack {...{direction:'row', width:200, height:20, spacing:1, justifyContent:'flex-end', display: 'inline-flex', my:1/4, mx:1/2}}>
            {action}
            {showState && (
                <Stack direction='row' width={15} height={15} justifyContent={'flex-end'}>
                    {dlState === 'DONE' && <img src={DOWNLOAED} alt='Done'/>}
                    {dlState === 'WORKING' && <img src={LOADING} alt='Loading'/>}
                    {dlState === 'FAIL' && <img src={FAILED} alt='Failed' title='Download may have failed or timed out'/>}
                </Stack>
            )}
        </Stack>
    );
}
