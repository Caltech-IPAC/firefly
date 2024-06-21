import React, {useEffect} from 'react';
import {bool, string, func, object, element} from 'prop-types';
import {Button, LinearProgress, Skeleton, Stack, Typography} from '@mui/joy';

import {dispatchJobAdd, dispatchJobCancel} from './BackgroundCntlr.js';
import {dispatchComponentStateChange, getComponentState} from '../ComponentCntlr.js';
import {useStoreConnector, Slot} from '../../ui/SimpleComponent.jsx';
import {Logger} from '../../util/Logger.js';
import {showJobInfo} from './JobInfo.jsx';
import {getJobInfo} from './BackgroundUtil.js';
import {InfoButton} from 'firefly/visualize/ui/Buttons.jsx';

const logger = Logger('BgMaskPanel');

/**
 * This component uses ComponentCntlr state persistence.  It is keyed by the given props's componentKey.
 * The data structure is described below.
 * @typedef {Object} data               BackgroundablePanel's data structure
 * @prop {boolean}  data.inProgress     true when a download is in progress
 * @prop {boolean}  data.jobInfo        the jobInfo given to this background request
 * @prop {function} data.onMaskComplete    function called when job has successfully completed or sent to background or canceled
 */


export const BgMaskPanel = React.memo(({componentKey, onMaskComplete, mask, showError= true, ...props}) => {

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
            <MaskP msg={msg} jobInfo={jobInfo} mask={mask} {...props}>
                <Options/>
            </MaskP>
        );
    } else if (errorInJob && showError) {
        return (
            <MaskP msg={msg} jobInfo={jobInfo} mask={mask} {...props}/>
        );
    } else return null;
});

function MaskP({msg, jobInfo, children, mask=<Skeleton/>, ...props}) {
    const showInfo = () => {
        showJobInfo(jobInfo.jobId);
    };

    return (
        <Slot component={Stack} position='absolute' sx={{inset:0}} slotProps={props}>
            {mask}
            <Stack whiteSpace='nowrap' position='absolute' zIndex={10} top='50%' left='50%' spacing={2} sx={{transform: 'translate(-50%, -50%)'}}>
                <Stack direction='row' alignItems='center' justifyContent='center' spacing={1}>
                    <Typography level='title-md' color='warning' fontStyle='italic' noWrap={true}>{msg}</Typography>
                    <InfoButton enabled={!!jobInfo} onClick={showInfo}/>
                </Stack>
                {children}
                <LinearProgress color='neutral'/>
            </Stack>
        </Slot>

    );
}

BgMaskPanel.propTypes = {
    componentKey: string.isRequired,  // key used to identify this background job
    style: object,                    // used for overriding default styling
    onMaskComplete: func,
    showError: bool,
    mask: element
};
