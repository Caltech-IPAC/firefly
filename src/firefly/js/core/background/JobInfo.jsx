import React, {useEffect, useState} from 'react';

import {getElapsedTime, getJobInfo, getJobPctComplete, getMetadata, getProgressMsg, isActive, isTapJob, isUWS} from './BackgroundUtil.js';
import {Slot, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {KeywordBlock} from '../../tables/ui/TableInfo.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchHideDialog, dispatchShowDialog, isDialogVisible} from '../ComponentCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {CollapsibleItem, CollapsibleGroup} from '../../ui/panel/CollapsiblePanel.jsx';
import {uwsJobInfo} from 'firefly/rpc/SearchServicesJson.js';
import {Box, Card, Grid, LinearProgress, Sheet, Skeleton, Stack, Typography} from '@mui/joy';
import {TableErrorMsg} from 'firefly/tables/ui/TablePanel.jsx';
import {showInfoPopup} from 'firefly/ui/PopupUtil';
import {PrismADQLAware} from '../../ui/tap/AdvancedADQL';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils';
import {jobMonitorGroupKey, ResultsBlock, toDateString, useLocalTimeKey} from '../../core/background/JobMonitor';
import {CopyToClipboard} from 'firefly/visualize/ui/MouseReadout';

const dialogID = 'show-job-info';

const popupSx = {
    justifyContent: 'space-between',
    resize: 'both',
    overflow: 'auto',
    minHeight: 200, minWidth: 525,
    width: '45vh'
};

export function isJobInfoOpen() {
    return isDialogVisible(dialogID);
}

export function showJobInfo(jobId) {

    const popup = (
            <PopupPanel title='Job Information'>
                <Stack sx={popupSx}>
                    <JobInfo key={jobId} jobId={jobId} sx={{overflow: 'auto'}}/>
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
    const {results, parameters, errorSummary} = jobInfo;
    const hrefs = results?.map((r) => r.href);
    const {progress, ...aux} = jobInfo?.jobInfo ?? {};
    const hasMoreSection = hrefs || parameters || errorSummary || aux;
    const resultRenderer = () => <ResultsBlock job={jobInfo} ActionBtn={CopyHref}/>;
    return (
        <Stack spacing={1} p={1} sx={sx}>
            <JobInfoDetails jobInfo={jobInfo}/>
            <TAPDetails jobInfo={jobInfo}/>
            {/*{ meta?.runId && <KeywordBlock key='localRunId' label='local runId' value={meta.runId}/>}*/}
            { hasMoreSection && (
                <CollapsibleGroup>
                    <OptionalBlock label='Error Summary' title='Referred to as "errorSummary" in UWS' value={errorSummary} isOpen={isOpen}/>
                    <OptionalBlock label='Parameters' title='Referred to as "parameters" in UWS' value={parameters} isOpen={isOpen}/>
                    <OptionalBlock label='Results' title='Referred to as "results" in UWS' value={hrefs} Component={resultRenderer} isOpen={isOpen}/>
                    <OptionalBlock label='Extra Information' title='Referred to as "jobInfo" in UWS' value={aux} isOpen={isOpen}/>
                </CollapsibleGroup>
            )}
        </Stack>
    );
}

export function JobProgress({jobInfo, ...props}) {
    const [elapsed, setElapsed] = useState(0);
    const msg = getProgressMsg(jobInfo);

    useEffect(() => {
        const interval = setInterval(() => {
            setElapsed(getElapsedTime(jobInfo)); // triggers re-render
        }, 1000);

        return () => clearInterval(interval);
    }, [jobInfo]);

    if (!isActive(jobInfo)) return null;

    const pct = getJobPctComplete(jobInfo);
    const lpProps =  pct >= 0 ? {determinate:true, value:pct} : {};
    return (
        <Stack spacing={.25} {...props} sx={{flex: 1, ...props?.sx}}>
            <Stack direction='row' spacing={1} alignItems='baseline'>
                <Typography level='title-sm'>Progress:</Typography>
                <Typography level='body-sm' title={msg}
                            sx={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'}}>
                    {msg}
                </Typography>
                <Typography level='body-sm' color='primary'
                            sx={{fontVariantNumeric: 'tabular-nums', flexGrow: 1, textAlign: 'right'}}>
                    {elapsed}
                </Typography>
            </Stack>
            <LinearProgress variant='solid' thickness={2} {...lpProps}/>
        </Stack>
    );
}

function JobInfoDetails({jobInfo={}}) {
    const useLocalTime = useStoreConnector(() => getFieldVal(jobMonitorGroupKey, useLocalTimeKey));
    const toDate = (d) => d && new Date(d);
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
        <Grid container spacing={.5}>
            <GridRow>
                <KeywordBlock label='Phase' title='Referred to as "phase" in UWS' value={phase} mb={1}/>
                <JobIdWrapper jobInfo={jobInfo}/>
            </GridRow>
            <GridRow>
                <JobProgress jobInfo={jobInfo} sx={{mb: 1, mr: 1}}/>
            </GridRow>
            <GridRow>
                <KeywordBlock label='Creation Time' title='Referred to as "creationTime" in UWS' value={toDateString(creationTime, useLocalTime)}  {...dateProps}/>
                <KeywordBlock label='Owner' title='Referred to as "ownerId" in UWS' value={ownerId}/>
            </GridRow>
            <GridRow>
                <KeywordBlock label='Start Time' title='Referred to as "startTime" in UWS' value={toDateString(startTime, useLocalTime)} {...dateProps}/>
                <KeywordBlock label='Run time limit' title='Referred to as "executionDuration" in UWS' value={duration}/>
            </GridRow>
            <GridRow>
                <KeywordBlock label='End Time' title='Referred to as "endTime" in UWS' value={toDateString(endTime, useLocalTime)} {...dateProps}/>
                <KeywordBlock label='Actual run time' title='The difference of the "End" and "Start" times.' value={actualRt}/>
            </GridRow>
            <GridRow>
                <KeywordBlock label='Planned End Time' title='Referred to as "quote" in UWS' value={toDateString(quote, useLocalTime)} {...dateProps}/>
            </GridRow>
            <GridRow>
                <KeywordBlock label='Destruction Time' title='Referred to as "destruction" in UWS' value={toDateString(destruction, useLocalTime)} {...dateProps}/>
            </GridRow>
        </Grid>
    );
}

function GridRow({children}) {
    const [left, right] = React.Children.toArray(children);
    const lw = right ? 7 : 12;
    return (
        <>
            <Grid xs={lw}>{left}</Grid>
            {right && <Grid xs={5}>{right}</Grid>}
        </>
    );
}

function TAPDetails({jobInfo}) {
    const lang = jobInfo?.parameters?.lang;
    const params = jobInfo?.parameters || {};
    const adql = params[Object.keys(params).find((k) => k.toLowerCase() === 'query')];

    if (!adql || (lang && lang.toUpperCase() !== 'ADQL')) return null;

    return (
        <Stack spacing={0}>
            <Typography level='title-sm' mr={1/2}>ADQL QUERY</Typography>
            <Card sx={{ '--Card-padding': '4px' }}>
                <PrismADQLAware {...{
                    text:adql,
                    sx:{marginBlock: '-8px', fontSize: 'sm'},
                    slotProps:{pre: {style: {borderRadius:'8px'}}}
                }} />
            </Card>
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

function OptionalBlock({label, value, asLink, isOpen, Component=KeyValueBlock}) {
    if (!value) return null;
    return (
        <CollapsibleItem componentKey={`JobInfo-${label}`} header={label} isOpen={isOpen}>
            <Stack spacing={.5}>
                <Component asLink={asLink} value={value}/>
            </Stack>
        </CollapsibleItem>
    );
}

function CopyHref({job, resultIdx=0}) {
    const {href} = getMetadata({jobInfo:job, resultIdx});
    return <CopyToClipboard value={href} size={16} buttonStyle={{backgroundColor: 'unset'}} style={{alignSelf: 'end'}}/>
}

// value can be an object with string values or an array of strings.
// If a value contains the ':::' delimiter, it will be split into multiple values with the same key.
function KeyValueBlock({asLink, value}) {
    return (
        <>
            {Object.entries(value).map(([k, v]) =>
                String(v).split(':::').map((val, idx) => {           // matches ':::' delimiter used by Server's JobInfo.parameters
                    const isLink = asLink ?? /^https?:\/\//.test(val?.toLowerCase?.());
                    return <KeywordBlock key={k+idx} label={k} value={val} asLink={isLink}/>;
                })
            )}
        </>
    );
}