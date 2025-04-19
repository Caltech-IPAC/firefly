import React from 'react';

import {getJobInfo} from './BackgroundUtil.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {KeywordBlock} from '../../tables/ui/TableInfo.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchHideDialog, dispatchShowDialog} from '../ComponentCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {CollapsibleItem, CollapsibleGroup} from 'firefly/ui/panel/CollapsiblePanel.jsx';
import {uwsJobInfo} from 'firefly/rpc/SearchServicesJson.js';
import {Box, Button, Skeleton, Stack} from '@mui/joy';
import {OverflowMarker, TableErrorMsg} from 'firefly/tables/ui/TablePanel.jsx';
import {showInfoPopup} from 'firefly/ui/PopupUtil';


const popupSx = {
    justifyContent: 'space-between',
    resize: 'both',
    overflow: 'auto',
    minHeight: 200, minWidth: 500,
    width: '45vh'
};

export function showJobInfo(jobId) {
    const ID = 'show-job-info';
    const popup = (
        <PopupPanel title='Job Information' >
            <Stack key={jobId} sx={popupSx}>
                <JobInfo jobId={jobId} sx={{overflow: 'auto'}}/>
                <HelpIcon helpId={'basics.bgJobInfo'} sx={{ml: 'auto'}}/>
            </Stack>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(ID, popup);
    dispatchShowDialog(ID);
}

export async function showUwsJob({jobUrl, jobId}) {

    const id = 'show-uws-job-info';
    const mask = (
        <PopupPanel title='UWS Job' >
            <Box key={jobId} sx={{...popupSx, position: 'relative'}}>
                <Skeleton/>
            </Box>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(id, mask);
    dispatchShowDialog(id);

    try {
        const jobInfo = await uwsJobInfo(jobUrl, jobId);
        const popup = (
            <PopupPanel title='UWS Job' >
                <Stack key={jobId} sx={popupSx}>
                    <UwsJobInfo jobInfo={jobInfo} sx={{overflow: 'auto'}}/>
                    <HelpIcon helpId={'basics.uwsJob'} sx={{ml: 'auto'}}/>
                </Stack>
            </PopupPanel>
        );

        DialogRootContainer.defineDialog(id, popup);
        dispatchShowDialog(id);
    } catch (error) {
        dispatchHideDialog(id);
        showInfoPopup(<TableErrorMsg error={error}/>, 'Error');
    }
}


export function JobInfo({jobId, ...props}) {
    const jobInfo = useStoreConnector(() => getJobInfo(jobId) || {});
    return <UwsJobInfo jobInfo={jobInfo} {...props}/>;
}

export function UwsJobInfo({jobInfo, sx, isOpen=false}) {
    const {results, parameters, errorSummary, meta, jobInfo:aux, ...rest} = jobInfo;
    const hrefs = results?.map((r) => r.href);
    const hasMoreSection = hrefs || parameters || errorSummary || aux;
    return (
        <Stack spacing={.5} p={1} sx={sx}>
            {Object.entries(rest)
                    .filter(([k, v]) => k && v)
                    .map(([k,v]) => <KeywordBlock key={k} label={k} value={v}/>)
            }
            {/*{ meta?.runId && <KeywordBlock key='localRunId' label='local runId' value={meta.runId}/>}*/}
            { hasMoreSection && (
                <CollapsibleGroup>
                    <OptionalBlock label='parameters' value={parameters} isOpen={isOpen}/>
                    <OptionalBlock label='results' value={hrefs} asLink={true} isOpen={isOpen}/>
                    <OptionalBlock label='errorSummary' value={errorSummary} isOpen={isOpen}/>
                    <OptionalBlock label='jobInfo' value={aux} isOpen={isOpen}/>
                </CollapsibleGroup>
            )}
        </Stack>
    );
}

function OptionalBlock({label, value, asLink, isOpen}) {
    if (!value) return null;
    return (
        <CollapsibleItem componentKey={`JobInfo-${label}`} header={label} isOpen={isOpen}>
            <Stack spacing={.5}>
                {Object.entries(value).map(([k, v]) => {
                        const isLink = asLink ?? /^https?:\/\//.test(v?.toLowerCase?.());
                        return <KeywordBlock key={k} label={k} value={v} asLink={isLink}/>;
                    }
                )}
            </Stack>
        </CollapsibleItem>
    );
}
