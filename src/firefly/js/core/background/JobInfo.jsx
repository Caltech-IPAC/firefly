import React from 'react';

import {getJobInfo} from './BackgroundUtil.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {KeywordBlock} from '../../tables/ui/TableInfo.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchHideDialog, dispatchShowDialog, isDialogVisible} from '../ComponentCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {CollapsibleItem, CollapsibleGroup} from '../../ui/panel/CollapsiblePanel.jsx';
import {uwsJobInfo} from 'firefly/rpc/SearchServicesJson.js';
import {Box, Skeleton, Stack} from '@mui/joy';
import {TableErrorMsg} from 'firefly/tables/ui/TablePanel.jsx';
import {showInfoPopup} from 'firefly/ui/PopupUtil';

const dialogID = 'show-job-info';

const popupSx = {
    justifyContent: 'space-between',
    resize: 'both',
    overflow: 'auto',
    minHeight: 200, minWidth: 500,
    width: '45vh'
};

export function isJobInfoOpen() {
    return isDialogVisible(dialogID);
}

export function showJobInfo(jobId) {
    const popup = (
        <PopupPanel title='Job Information' >
            <Stack key={jobId} sx={popupSx}>
                <JobInfo jobId={jobId} sx={{overflow: 'auto'}}/>
                <HelpIcon helpId={'basics.bgJobInfo'} sx={{ml: 'auto'}}/>
            </Stack>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(dialogID, popup);
    dispatchShowDialog(dialogID);
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
            <JobInfoDetails {...rest}/>
            {/*{ meta?.runId && <KeywordBlock key='localRunId' label='local runId' value={meta.runId}/>}*/}
            { hasMoreSection && (
                <CollapsibleGroup>
                    <OptionalBlock label='Parameters' title='Referred to as "parameters" in UWS' value={parameters} isOpen={isOpen}/>
                    <OptionalBlock label='Results' title='Referred to as "results" in UWS' value={hrefs} asLink={true} isOpen={isOpen}/>
                    <OptionalBlock label='Error Summary' title='Referred to as "errorSummary" in UWS' value={errorSummary} isOpen={isOpen}/>
                    <OptionalBlock label='Extra Information' title='Referred to as "jobInfo" in UWS' value={aux} isOpen={isOpen}/>
                </CollapsibleGroup>
            )}
        </Stack>
    );
}

function JobInfoDetails({jobId, ownerId, phase, creationTime, startTime, endTime, quote, executionDuration, destruction}) {
    let actualRt = '';
    startTime = startTime && new Date(startTime);
    endTime = endTime && new Date(endTime);
    creationTime = creationTime && new Date(creationTime);
    quote = quote && new Date(quote);
    destruction = destruction && new Date(destruction);
    if (endTime && startTime) {
        actualRt = Math.round((endTime - startTime) / 1000) + 's';
    }
    const duration =  executionDuration ? executionDuration + 's' : '';
    const dateProps = {width: '18em', justifyContent:'space-between'};
    return (
        <Stack direction='row' spacing={2}>
            <Stack>
                <KeywordBlock label='Phase' title='Referred to as "phase" in UWS' value={phase} mb={1}/>
                <KeywordBlock label='Created' title='Referred to as "creationTime" in UWS' value={creationTime?.toISOString()}  {...dateProps}/>
                <KeywordBlock label='Start Time' title='Referred to as "startTime" in UWS' value={startTime?.toISOString()} {...dateProps}/>
                <KeywordBlock label='End Time' title='Referred to as "endTime" in UWS' value={endTime?.toISOString()} {...dateProps}/>
                <KeywordBlock label='Planned end' title='Referred to as "quote" in UWS' value={quote?.toISOString()} {...dateProps}/>
                <KeywordBlock label='Destruction' title='Referred to as "destruction" in UWS' value={destruction?.toISOString()} {...dateProps}/>
            </Stack>
            <Stack>
                <KeywordBlock label='Job ID' title='Referred to as "jobId" in UWS' value={jobId} mb={1}/>
                <KeywordBlock label='Owner' title='Referred to as "ownerId" in UWS' value={ownerId}/>
                <KeywordBlock label='Run time limit' title='Referred to as "executionDuration" in UWS' value={duration}/>
                <KeywordBlock label='Actual run time' title='The difference of the "End" and "Start" times.' value={actualRt}/>
            </Stack>
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
