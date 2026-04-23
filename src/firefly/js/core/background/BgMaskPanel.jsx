import React, {useEffect} from 'react';
import {bool, string, func, object, element} from 'prop-types';
import {Box, Button, Sheet, Skeleton, Stack, Typography} from '@mui/joy';

import {dispatchComponentStateChange, getComponentState} from '../ComponentCntlr.js';
import {useStoreConnector, Slot} from '../../ui/SimpleComponent.jsx';
import {Logger} from '../../util/Logger.js';
import {JobProgress, showJobInfo} from './JobInfo.jsx';
import {getJobInfo} from './BackgroundUtil.js';
import {InfoButton} from 'firefly/visualize/ui/Buttons.jsx';
import {showJobMonitor} from './JobMonitor';
import {dispatchJobRemove} from './BackgroundCntlr';

const logger = Logger('BgMaskPanel');
const maskPanelBreakpoint = '25rem';

/**
 * This component uses ComponentCntlr state persistence.  It is keyed by the given props's componentKey.
 * The data structure is described below.
 * @typedef {Object} data               BackgroundablePanel's data structure
 * @prop {boolean}  data.inProgress     true when a download is in progress
 * @prop {boolean}  data.jobInfo        the jobInfo given to this background request
 * @prop {function} data.onMaskComplete    function called when job has successfully completed or sent to background or canceled
 */


export const BgMaskPanel = React.memo(({componentKey, onMaskComplete, mask, showError= true, renderError, ...props}) => {

    const inProgress = useStoreConnector(() => getComponentState(componentKey)?.inProgress || false);
    const jobInfo    = useStoreConnector(() => {
                           const {jobId} = getComponentState(componentKey);
                           return jobId && getJobInfo(jobId);
                       });

    const errorInJob= ['ERROR', 'ABORTED'].includes(jobInfo?.phase);

    useEffect(() => {
        (jobInfo && !inProgress && !errorInJob) && onMaskComplete?.();
    }, [inProgress, jobInfo, errorInJob]);

    const doHide = () => {
        dispatchComponentStateChange(componentKey, {inProgress: false, hide:true});
    };
    const doShowMonitor = () => {
        doHide();
        showJobMonitor(true);
    };
    const doCancel = () => {
        if (jobInfo) dispatchJobRemove(jobInfo?.meta?.jobId);
        doHide();
    };

    const showInfo = () => showJobInfo(jobInfo?.meta?.jobId);
    const infoButton = <InfoButton enabled={!!jobInfo} onClick={showInfo}/>;

    logger.debug(inProgress ? 'show' : 'hide');
    if (inProgress) {
        // we override default zIndex=9 of Skeleton since it sometimes interferes with other components,
        // the absolute position of mask and its siblings already handles z-indexing cleanly
        const progressMask = <Skeleton sx={{zIndex: 'auto'}}/>;

        return (
            <MaskP mask={mask === undefined ? progressMask : mask} {...props}>
                <Stack spacing={2} sx={{whiteSpace: 'nowrap', m: 'auto', width: 'fit-content', alignItems: 'stretch'}}>
                    <Stack direction='row' alignItems='center' justifyContent='center' spacing={1}>
                        <JobProgress jobInfo={jobInfo} sx={{
                            [`@container (width <= ${maskPanelBreakpoint})`]: {
                                // hide the title of JobProgress when the panel is too narrow
                                '& .MuiTypography-title-sm': { maxWidth: 0, overflow: 'hidden', mr: -1 },
                            }
                        }}/>
                        {infoButton}
                    </Stack>
                    <Stack sx={{
                        gap: 1,
                        flexDirection: 'column',
                        [`@container (width > ${maskPanelBreakpoint})`]: {
                            // arrange buttons in a row only when the panel is wide enough
                            flexDirection: 'row', alignItems: 'center'
                        }
                    }}>
                        <Button variant='solid' size='sm' disabled={!jobInfo} color='primary' onClick={doHide}>Send to background</Button>
                        <Button variant='solid' size='sm' disabled={!jobInfo} color='primary' onClick={doShowMonitor}>Job Monitor</Button>
                        <Button variant='soft' size='sm' disabled={!jobInfo} onClick={doCancel}>Cancel</Button>
                    </Stack>
                </Stack>
            </MaskP>
        );
    } else if (errorInJob && showError) {
        const errorMask = <Sheet sx={{width: 1, height: 1, bgcolor: 'warning.softBg'}}/>;
        const errorMsg = jobInfo?.errorSummary?.message;
        const defaultErrorContent = (
            <Stack direction='row' alignItems='center' justifyContent='center' spacing={1}>
                <Typography level='title-md' color='warning' noWrap={true}>{errorMsg || 'Error'}</Typography>
                {infoButton}
            </Stack>
        );
        return (
            <MaskP mask={mask === undefined ? errorMask : mask} {...props}>
                {renderError?.(errorMsg, infoButton) ?? defaultErrorContent}
            </MaskP>
        );
    } else return null;
});

function MaskP({mask, children, ...props}) {
    if (!mask) return <Box sx={{containerType: 'inline-size'}}>{children}</Box>;
    return (
        <Slot component={Stack}
              sx={{
                  position: 'absolute', inset: 0,
                  containerType:'inline-size' // this allows its children to use @container queries for responding to size breakpoints
              }}
              slotProps={props}>
            {mask}
            <Box sx={{
                       position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', // to center the content
                       maxWidth: '100%', maxHeight: '100%', overflow: 'hidden', // to prevent content overflowing
                   }}>
                {children}
            </Box>
        </Slot>
    );
}

BgMaskPanel.propTypes = {
    componentKey: string.isRequired,  // key used to identify this background job
    style: object,                    // used for overriding default styling
    onMaskComplete: func,
    showError: bool,
    mask: element,
    renderError: func,                // (errorMsg, infoButton) => ReactNode; replaces default error display
};
