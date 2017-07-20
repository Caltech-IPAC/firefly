/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import {get, set, isEmpty, unset} from 'lodash';

import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {InputField} from '../../ui/InputField.jsx';
import {ProgressBar} from '../../ui/ProgressBar.jsx';
import Validate from '../../util/Validate.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {getBackgroundInfo, BG_STATE, isActive, isDone, isSuccess, emailSent, canCreateScript} from './BackgroundUtil.js';
import {dispatchBgStatus, dispatchJobRemove, dispatchBgSetEmail, dispatchJobCancel} from './BackgroundCntlr.js';
import {downloadWithProgress} from '../../util/WebUtil.js';
import {DownloadProgress} from '../../rpc/SearchServicesJson.js';
import {showScriptDownloadDialog} from '../../ui/ScriptDownloadDialog.jsx';
import {SimpleComponent} from '../../ui/SimpleComponent.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';

import LOADING from 'html/images/gxt/loading.gif';
import CANCEL from 'html/images/stop.gif';
import DOWNLOAED from 'html/images/blue_check-on_10x10.gif';
import FAILED from 'html/images/exclamation16x16.gif';

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

class BackgroundMonitor extends SimpleComponent {

    getNextState(np) {
        return getBackgroundInfo();
    }

    render() {
        const {jobs={}, email='', help_id} = this.state || {};
        const packages = Object.entries(jobs)
                        .filter(([id, job]) => get(job, 'TYPE') === 'PACKAGE')
                        .map( ([id, job]) => {
                            return (<PackageStatus key={id} {...job} />);
                        });
        const searches = Object.entries(jobs)
            .filter(([id, job]) => get(job, 'TYPE') === 'SEARCH')
            .map( ([id, job]) => {
                return (<SearchItem key={id} {...job} />);
            });
        const unknown = Object.entries(jobs)
            .filter(([id, job]) => get(job, 'TYPE') === 'UNKNOWN')
            .map( ([id, job]) => {
                return (<SearchItem key={id} {...job} />);
            });                                                         // failed requests..  should not happen.  Not sure what to do with this yet.
        return (
            <div className='BGMon'>
                <div className='BGMon__content'>
                    {!isEmpty(packages) && packages}
                    {!isEmpty(searches) && searches}
                    {!isEmpty(unknown) && unknown}
                </div>
                <BgFooter {...{help_id, email}}/>
            </div>
        );
    }
}

class BgFooter extends PureComponent {

    onHide() {
        showBackgroundMonitor(false);
    }

    onEmailChanged(v) {
        if (get(v, 'valid')) {
            const {email} = this.props;
            if (email !== v.value) dispatchBgSetEmail(v.value);
        }
    }

    render() {
        const {help_id = 'basics.bgmon', email} = this.props;
        return (
            <div className='BGMon__footer' key='bgMonFooter'>
                <button className='button std hl' onClick={this.onHide}
                        title='Hide Background Monitor'>Hide</button>
                <InputField
                    validator={Validate.validateEmail.bind(null, 'an email field')}
                    tooltip='Enter an email to be notified when a process completes.'
                    label='Email:'
                    labelStyle={{display: 'inline-block', width: 40, fontWeight: 'bold'}}
                    value={email}
                    placeholder='Enter an email to get notification'
                    size={27}
                    onChange={this.onEmailChanged.bind(this)}
                    actOn={['blur','enter']}
                />
                <div>
                    <HelpIcon helpId={help_id} />
                </div>
            </div>
        );
    }
}

function SearchItem(bgStatus) {
    const {ID, STATE, SERVER_REQ, Title='unknown'} = bgStatus;
    const emailed = emailSent(bgStatus);    // this is not implemented yet.
    return (
        <div className='BGMon__package'>
            <div className='BGMon__package--box'>
                <SearchStatus {...{ID, STATE, Title, SERVER_REQ}}/>
                { emailed &&
                    <div className='BGMon__package--status'>
                        <div>Notification email sent</div>
                    </div>
                }
            </div>
        </div>
    );
}

// eslint-disable-next-line
function SearchStatus({ID, STATE, Title, SERVER_REQ}) {
    var progress;
    if (BG_STATE.WAITING.is(STATE)) {
        progress = <div className='BGMon__header--waiting'>In progress... <img style={{marginLeft: 3}} src={LOADING}/></div>;
    } else if (BG_STATE.CANCELED.is(STATE)) {
        progress = <div>User aborted this request</div>;
    } else {
        if (isEmpty(SERVER_REQ)) {
            progress = <div className='BGMon__header--waiting'>unexpected error</div>;
        } else {
            const request = JSON.parse(SERVER_REQ);
            unset(request, 'META_INFO.backgroundable');
            progress = <div className='BGMon__packageItem--url' onClick={() => dispatchTableSearch(request, {backgroundable: false})}>Show results</div>;
        }
    }
    return (
        <PackageHeader {...{ID, Title, progress, STATE}} />
    );
}

function PackageStatus(bgStatus) {
    const {PACKAGE_CNT, ID, STATE, DATA_SOURCE, Title='unknown'} = bgStatus;
    const Content = PACKAGE_CNT > 1 ? MultiPackage : SinglePackage;
    const emailed = emailSent(bgStatus);
    const script = canCreateScript(bgStatus) && isSuccess(STATE) && PACKAGE_CNT > 1;
    
    return (
        <div className='BGMon__package'>
            <div className='BGMon__package--box'>
                <Content {...bgStatus}/>
                { (emailed || script) &&
                    <div className='BGMon__package--status'>
                        { emailed ? <div>Notification email sent</div> : <div/>}
                        { script  && <div className='BGMon__packageItem--url' onClick={() => showScriptDownloadDialog({ID, Title, DATA_SOURCE})}>Get Download Script</div> }
                    </div>
                }
            </div>
        </div>
    );
}

// eslint-disable-next-line
function SinglePackage({ID, Title, STATE, ITEMS=[]}) {
    var progress;
    if (BG_STATE.WAITING.is(STATE)) {
        progress = <div className='BGMon__header--waiting'>Computing number of packages... <img style={{marginLeft: 3}} src={LOADING}/></div>;
    } else if (BG_STATE.CANCELED.is(STATE)) {
        progress = <div>User aborted this request</div>;
    } else {
        const params = ITEMS[0] || {};
        progress = <PackageItem SINGLE={true} STATE={STATE} ID={ID} {...params} />;
    }
    return (
        <PackageHeader {...{ID, Title, progress, STATE}} />
    );
}

// eslint-disable-next-line
function MultiPackage({ID, Title, STATE, ITEMS}) {
    var progress = BG_STATE.CANCELED.is(STATE) && <div>User aborted this request</div>;
    const packages = ITEMS.map( (pi, INDEX) => {
                return <PackageItem key={'multi-' + INDEX} STATE={STATE} ID={ID} {...ITEMS[INDEX]} />;
            });

    return (
        <div>
            <PackageHeader {...{ID, Title, progress, STATE}} />
            <div className='BGMon__multiItems'>
                {packages}
            </div>
        </div>
    );
}

// eslint-disable-next-line
function PackageHeader({ID, Title, progress, STATE}) {
    const removeBgStatus = () => {
        dispatchJobRemove(ID);
    };
    const doCancel = () => {
        dispatchJobCancel(ID);
    };

    return (
        <div className='BGMon__header'>
            <div className='BGMon__header--title' title={Title}>{Title}</div>
            <div style={{display: 'inline-flex', alignItems: 'center', paddingLeft: 5}}>
                {progress}
                <div className='BGMon__header--action'>
                    {isActive(STATE) && <img className='BGMon__action' src={CANCEL} onClick={doCancel} title='Abort this job.'/>}
                    {isDone(STATE) &&
                    <div className='btn-close'
                         title='Remove Background Job'
                         onClick={removeBgStatus}/>
                    }
                </div>
            </div>
        </div>
    );
}

function PackageItem(progress) {
    const {SINGLE, ID, INDEX, STATE, finalCompressedBytes, processedBytes, totalBytes, processedFiles, totalFiles, url, downloaded} = progress;
    const doDownload = () => {
        var bgStatus = set({ID}, ['ITEMS', INDEX, 'downloaded'], DownloadProgress.WORKING);
        dispatchBgStatus(bgStatus);
        downloadWithProgress(url)
            .then( () => {
                bgStatus = set({ID}, ['ITEMS', INDEX, 'downloaded'], DownloadProgress.DONE);
                dispatchBgStatus(bgStatus);
                }
            ).catch(() => {
                bgStatus = set({ID}, ['ITEMS', INDEX, 'downloaded'], DownloadProgress.FAIL);
                dispatchBgStatus(bgStatus);
            });
    };
    const BY_SIZE = totalBytes && totalBytes >= processedBytes;
    var pct= BY_SIZE ? (processedBytes / totalBytes) :
                         (processedFiles / totalFiles);
    pct = Math.round(100 * pct);
    const pctOf = BY_SIZE ? Math.round(totalBytes/1024/1024) + ' MB' :
                    totalFiles + ' files';
    const finalSize = Math.round(finalCompressedBytes/1024/1024, -1);
    const dlmsg = SINGLE ? 'Download Now' : `Download Part #${INDEX+1}`;

    if (isSuccess(STATE) || pct === 100) {      // because when there is only 1 item, state is set to success but pct does not calculate to 100%
        return (
            <div className='BGMon__packageItem'>
                <div className='BGMon__packageItem--url' onClick={doDownload}>{dlmsg}</div>
                <div style={{display: 'inline-flex', alignItems: 'center'}}>
                    <div style={{marginRight: 3}}>{`${finalSize} MB`}</div>
                    <div style={{width: 15}}>
                        {DownloadProgress.DONE.is(downloaded) && <img src={DOWNLOAED}/>}
                        {DownloadProgress.WORKING.is(downloaded) && <img src={LOADING}/>}
                        {DownloadProgress.FAIL.is(downloaded) && <img src={FAILED} title='Download may have failed or timed out'/>}
                    </div>
                </div>
            </div>
        );
    } else if(pct === 0) {
        return false;
    } else {
        return (
            <div className='BGMon__packageItem'>
                <ProgressBar
                    value= {pct}
                    text= {`Zipped ${pct}% of ${pctOf}`}
                />
            </div>
        );
    }
}
