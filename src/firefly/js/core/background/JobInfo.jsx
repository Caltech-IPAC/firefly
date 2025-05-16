import React from 'react';

import {getJobInfo, isTapJob, isUWS} from './BackgroundUtil.js';
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
import {PrismADQLAware} from '../../ui/tap/AdvancedADQL';

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
            <PopupPanel title='Job Information'>
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
    const {results, parameters, errorSummary, jobInfo:aux} = jobInfo;
    const hrefs = results?.map((r) => r.href);
    const hasMoreSection = hrefs || parameters || errorSummary || aux;
    return (
        <Stack spacing={1} p={1} sx={sx}>
            <JobInfoDetails jobInfo={jobInfo}/>
            <TAPDetails jobInfo={jobInfo}/>
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

const toDate = (d) => d && new Date(d);

function JobInfoDetails({jobInfo={}}) {
    const {ownerId, phase, executionDuration} = jobInfo;
    const startTime = toDate(jobInfo.startTime);
    const endTime = toDate(jobInfo.endTime);
    const creationTime = toDate(jobInfo.creationTime);
    const quote = toDate(jobInfo.quote);
    const destruction = toDate(jobInfo.destruction);
    const actualRt = endTime && startTime ? Math.round((endTime - startTime) / 1000) + 's' : '';
    const duration =  executionDuration ? executionDuration + 's' : '';
    const dateProps = {width: '18rem', justifyContent:'space-between'};
    return (
        <Stack direction='row' spacing={4}>
            <Stack>
                <KeywordBlock label='Phase' title='Referred to as "phase" in UWS' value={phase} mb={1}/>
                <KeywordBlock label='Created' title='Referred to as "creationTime" in UWS' value={creationTime?.toISOString()}  {...dateProps}/>
                <KeywordBlock label='Start Time' title='Referred to as "startTime" in UWS' value={startTime?.toISOString()} {...dateProps}/>
                <KeywordBlock label='End Time' title='Referred to as "endTime" in UWS' value={endTime?.toISOString()} {...dateProps}/>
                <KeywordBlock label='Planned end' title='Referred to as "quote" in UWS' value={quote?.toISOString()} {...dateProps}/>
                <KeywordBlock label='Destruction' title='Referred to as "destruction" in UWS' value={destruction?.toISOString()} {...dateProps}/>
            </Stack>
            <Stack>
                <JobIdWrapper jobInfo={jobInfo}/>
                <KeywordBlock label='Owner' title='Referred to as "ownerId" in UWS' value={ownerId}/>
                <KeywordBlock label='Run time limit' title='Referred to as "executionDuration" in UWS' value={duration}/>
                <KeywordBlock label='Actual run time' title='The difference of the "End" and "Start" times.' value={actualRt}/>
            </Stack>
        </Stack>
    );
}

function TAPDetails({jobInfo}) {
    const lang = jobInfo?.parameters?.lang;
    const params = jobInfo?.parameters || {};
    const adql = params[Object.keys(params).find((k) => k.toLowerCase() === 'query')];

    if (!adql || (lang && lang.toUpperCase() !== 'ADQL')) return null;

    return (
        <Stack spacing={0}>
            <KeywordBlock label='ADQL QUERY'/>
            <PrismADQLAware text={adql} sx={{marginBlock: '-8px', fontSize: 'sm'}}/>
        </Stack>
    );
}

function JobIdWrapper({jobInfo}) {
    const {jobId, jobInfo: aux} = jobInfo;
    const href = isUWS(jobInfo) && aux?.jobUrl;
    const label = isTapJob(jobInfo) ? 'TAP Job ID' : isUWS(jobInfo) ? 'UWS Job ID' : 'Job ID';
    const title = isUWS(jobInfo) ? 'Referred to as "jobId" in UWS' : 'Internal identifier for the job';
    return <KeywordBlock value={jobId} mb={1} asLink={!!href} {...{href, label, title}} />;
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
