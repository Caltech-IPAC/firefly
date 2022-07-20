import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';

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


export const BgMaskPanel = React.memo(({componentKey, onMaskComplete, style={}}) => {

    const {inProgress, jobInfo} = useStoreConnector((oldState={}) => {
        const {inProgress:oProg, jobInfo:oInfo} = oldState;
        const {inProgress=false, jobId} = getComponentState(componentKey);
        const jobInfo = getJobInfo(jobId);
        if (oProg === inProgress && shallowequal(oInfo, jobInfo)) return oldState;
        return {inProgress, jobInfo};
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

    const msg = jobInfo?.error || jobInfo?.progressDesc || 'Working...';

    const Options = () => ( inProgress &&
        <div className='BgMaskPanel__actions'>
            <div className='button large' onClick={sendToBg}>Send to background</div>
            <div className='button large' onClick={abort}>Cancel</div>
        </div>
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
    } else if (errorInJob) {
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
    componentKey: PropTypes.string.isRequired,  // key used to identify this background job
    style: PropTypes.object,                    // used for overriding default styling
    onMaskComplete: PropTypes.func
};
