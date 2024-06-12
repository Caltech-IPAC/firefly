import React, {useEffect} from 'react';
import {bool, string, func, object} from 'prop-types';
import {Button, Stack} from '@mui/joy';

import {dispatchJobAdd, dispatchJobCancel} from './BackgroundCntlr.js';
import {dispatchComponentStateChange, getComponentState} from '../ComponentCntlr.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {Logger} from '../../util/Logger.js';
import {showJobInfo} from './JobInfo.jsx';
import {getJobInfo} from './BackgroundUtil.js';
import INFO from 'html/images/info-icon.png';

const logger = Logger('BgMaskPanel');

/**
 * This component uses ComponentCntlr state persistence.  It is keyed by the given props's componentKey.
 * The data structure is described below.
 * @typedef {Object} data               BackgroundablePanel's data structure
 * @prop {boolean}  data.inProgress     true when a download is in progress
 * @prop {boolean}  data.jobInfo        the jobInfo given to this background request
 * @prop {function} data.onMaskComplete    function called when job has successfully completed or sent to background or canceled
 */


export const BgMaskPanel = React.memo(({componentKey, onMaskComplete, showError= true, style={}}) => {

    const inProgress = useStoreConnector(() => getComponentState(componentKey)?.inProgress || false);
    const jobInfo    = useStoreConnector(() => {
                           const {jobId} = getComponentState(componentKey);
                           return jobId && getJobInfo(jobId);
                       });
    const sendToBg = () => {
        if (jobInfo) {
            dispatchComponentStateChange(componentKey, {inProgress:false});
            dispatchJobAdd(jobInfo);
        }
    };
    const abort = () => {
        if (jobInfo) {
            dispatchComponentStateChange(componentKey, {inProgress:false});
            dispatchJobCancel(jobInfo.jobId);
        }
    };

    const showInfo = () => {
        jobInfo && showJobInfo(jobInfo.jobId);
    };

    const msg = jobInfo?.errorSummary?.message || jobInfo?.jobInfo?.progressDesc || 'Working...';

    const Options = () => (
        <Stack direction='row' spacing={1}>
            <Button variant='solid' color='primary' disabled={!jobInfo} onClick={sendToBg}>Send to background</Button>
            <Button disabled={!jobInfo} onClick={abort}>Cancel</Button>
        </Stack>
    );

    const errorInJob= ['ERROR', 'ABORTED'].includes(jobInfo?.phase);

    useEffect(() => {
        (jobInfo && !inProgress && !errorInJob) && onMaskComplete?.();
    }, [inProgress, jobInfo, errorInJob]);


    logger.debug(inProgress ? 'show' : 'hide');
    if (inProgress) {
        return (
            <div className='BgMaskPanel' style={style}>
                <div className='loading-mask'/>
                <div style={{display: 'flex', alignItems: 'center'}}>
                    <div className='BgMaskPanel__content'>
                        <div style={{display: 'inline-flex', alignItems: 'center', justifyContent: 'center'}}>
                            <div className='BgMaskPanel__msg'>{msg}</div>
                            <img className='JobInfo__items--link' onClick={showInfo} src={INFO} style={{height: 20}}/>
                        </div>
                        <Options/>
                    </div>
                </div>
            </div>
        );
    } else if (errorInJob && showError) {
        return (
            <div className='BgMaskPanel' style={style}>
                <div className='mask-only'/>
                <div style={{display: 'flex', alignItems: 'center'}}>
                    <div className='BgMaskPanel__content'>
                        <div style={{display: 'inline-flex', alignItems: 'center', justifyContent: 'center'}}>
                            <div style={{marginRight: 5, fontSize: 18}}> {jobInfo.phase} </div>
                            <img className='JobInfo__items--link' onClick={showInfo} src={INFO} style={{height: 20}}/>
                        </div>
                        <div className='BgMaskPanel__msg'>{msg}</div>
                    </div>
                </div>
            </div>
        );
    } else return null;
});

BgMaskPanel.propTypes = {
    componentKey: string.isRequired,  // key used to identify this background job
    style: object,                    // used for overriding default styling
    onMaskComplete: func,
    showError: bool
};
