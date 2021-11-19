/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {isEmpty, isNil} from 'lodash';

import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {InputField} from '../../ui/InputField.jsx';
import {ProgressBar} from '../../ui/ProgressBar.jsx';
import Validate from '../../util/Validate.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../ComponentCntlr.js';
import {getBackgroundInfo, isActive, isAborted, isDone, isSuccess, canCreateScript, getJobInfo} from './BackgroundUtil.js';
import {dispatchBgJobInfo, dispatchJobRemove, dispatchBgSetEmailInfo, dispatchJobCancel} from './BackgroundCntlr.js';
import {showScriptDownloadDialog} from '../../ui/ScriptDownloadDialog.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {download} from '../../util/fetch';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {updateSet} from '../../util/WebUtil.js';
import {getRequestFromJob} from '../../tables/TableRequestUtil.js';
import {showJobInfo} from './JobInfo.jsx';

import LOADING from 'html/images/gxt/loading.gif';
import CANCEL from 'html/images/stop.gif';
import DOWNLOAED from 'html/images/blue_check-on_10x10.gif';
import FAILED from 'html/images/exclamation16x16.gif';
import INFO from 'html/images/info-icon.png';
import './BackgroundMonitor.css';


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

    const [{jobs={}, email, enableEmail, help_id}] = useStoreConnector(() => getBackgroundInfo());

    const items = Object.values(jobs)
                    .filter((job) => job?.monitored)
                    .map( (job) =>  job?.type === 'PACKAGE' ?
                        <PackageJob key={job.jobId} jobInfo={job} /> :
                        <SearchJob key={job.jobId} jobInfo={job} />
                    );
    return (
        <div className='BGMon'>
            <div className='BGMon__content'>
                {!isEmpty(items) && items}
            </div>
            <BgFooter {...{help_id, email, enableEmail}}/>
        </div>
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
    return (
        <div className='BGMon__footer' key='bgMonFooter'>
            <button className='button std hl' onClick={onHide}
                    title='Hide Background Monitor'>Hide</button>
            <div>
                <div style={{width: 250}}><input type='checkbox' checked={enableEmail} value='' onChange={toggleEnableEmail}/>Enable email notification</div>
                {enableEmail &&
                    <InputField
                        validator={Validate.validateEmail.bind(null, 'an email field')}
                        tooltip='Enter an email to be notified when a process completes.'
                        label='Email:'
                        labelStyle={{display: 'inline-block', marginLeft: 18, width: 32, fontWeight: 'bold'}}
                        value={email}
                        placeholder='Enter an email to get notification'
                        style={{width: 170}}
                        onChange={onEmailChanged}
                        actOn={['blur','enter']}
                    />
                }
            </div>
            <div>
                <HelpIcon helpId={help_id} />
            </div>
        </div>
    );
}

function SearchJob({jobInfo}) {
    const {email} = jobInfo;
    return (
        <div className='BGMon__package'>
            <div className='BGMon__package--box'>
                <JobHeader jobInfo={jobInfo}/>
                { email &&
                    <div className='BGMon__package--status'>
                        <div>Notification email sent</div>
                    </div>
                }
            </div>
        </div>
    );
}

function PackageJob({jobInfo}) {
    const {jobId, results, DATA_SOURCE, email, label='unknown'} = jobInfo;
    const script = canCreateScript(jobInfo) && isSuccess(jobInfo) && results?.length > 1;

    const items = results?.map( (url, idx) => <PackageItem key={'multi-' + idx} jobId={jobInfo.jobId} index={idx} />);

    return (
        <div className='BGMon__package'>
            <div className='BGMon__package--box'>
                <JobHeader jobInfo={jobInfo}/>
                {items?.length > 1 &&
                    <div className='BGMon__multiItems'> {items} </div>
                }
                { (email || script) &&
                    <div className='BGMon__package--status'>
                        { email ? <div>Notification email sent</div> : <div/>}
                        { script  && <div className='BGMon__packageItem--url' onClick={() => showScriptDownloadDialog({jobId, label, DATA_SOURCE})}>Get Download Script</div> }
                    </div>
                }
            </div>
        </div>
    );
}

function JobHeader({jobInfo}) {
    const {jobId, label} = jobInfo || {};

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
        <div className='BGMon__header'>
            <div title={label} style={{display: 'inline-flex', overflow: 'hidden'}}>
                <div className='BGMon__header--title'>{label}</div>
                <img src={INFO} onClick={showInfo} className='JobInfo__items--link'/>
            </div>
            <div style={{display: 'inline-flex', alignItems: 'center', paddingLeft: 5, flexShrink: 0}}>
                <JobProgress jobInfo={jobInfo}/>
                <div className='BGMon__header--action'>
                    {isActive(jobInfo) && <img className='BGMon__action' src={CANCEL} onClick={doCancel} title='Abort this job.'/>}
                    {isDone(jobInfo) &&
                    <div className='btn-close'
                         title='Remove Background Job'
                         onClick={removeBgStatus}/>
                    }
                </div>
            </div>
        </div>
    );
}

function JobProgress({jobInfo}) {
    const {progressDesc, progress, jobId} = jobInfo;
    if (isActive(jobInfo)) {
        return (
            <div className='BGMon__packageItem'>
                <ProgressBar value= {progress} text= {progressDesc}/>
            </div>
        );
    } else if (isAborted(jobInfo)) {
        return <div>{jobInfo?.error || 'Job aborted'}</div>;
    } else if (isSuccess(jobInfo)) {
        if (jobInfo?.type === 'SEARCH') {
            const showTable = () => {
                const request = getRequestFromJob(jobInfo.jobId);
                request && dispatchTableSearch(request);
            };
            return (<div className='BGMon__packageItem--url' onClick={showTable}>Show results</div> );
        } else {
            return jobInfo?.results?.length === 1 ? <PackageItem {...{SINGLE:true, jobId, index:0}} /> : <div/> ;
        }
    } else {
        return <div className='BGMon__header--error'>Job Failed</div>;
    }
}


function PackageItem({SINGLE, jobId, index}) {
    const [jobInfo] = useStoreConnector(() => getJobInfo(jobId));

    const {parameters, downloadState} = jobInfo;
    const dlreq = JSON.parse(parameters?.downloadRequest);
    const dlState = downloadState?.[index];

    const doDownload = () => {
        const url = jobInfo?.results?.[index];
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
                   ? <div className='BGMon__packageItem--info'> Downloaded to Workspace </div>
                   : <div className='BGMon__packageItem--url' onClick={doDownload}>{dlmsg}</div>;
    const showState = !SINGLE || (SINGLE && dlState);
    return (
        <div className='BGMon__packageItem'>
            {action}
            {showState &&
                <div style={{display: 'inline-flex', alignItems: 'center'}}>
                    <div style={{width: 15}}>
                        {dlState === 'DONE' && <img src={DOWNLOAED}/>}
                        {dlState === 'WORKING' && <img src={LOADING}/>}
                        {dlState === 'FAIL' && <img src={FAILED} title='Download may have failed or timed out'/>}
                    </div>
                </div>
            }
        </div>
    );
}
