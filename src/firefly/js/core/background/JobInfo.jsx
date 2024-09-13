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


export function JobInfo({jobId, sx, tbl_id}) {
    const {phase, startTime, endTime, results, errorSummary, jobInfo={}} = useStoreConnector(() => getJobInfo(jobId) || {});
    const {dataOrigin, summary, type} = jobInfo;
    return (
        <Stack spacing={.5} p={1} sx={sx}>
            <KeywordBlock label='Phase' value={phase}/>
            <KeywordBlock label='Start Time' value={startTime}/>
            {endTime && <KeywordBlock label='End Time' value={endTime}/>}
            <DataOrigin {...{dataOrigin, type, jobId}}/>
            <FinalMsg {...{phase, results, errorSummary, summary, tbl_id}}/>
            <KeywordBlock label='ID' value={jobId} title='Internal query identifier, usable for diagnostics'/>
        </Stack>
    );
}

function DataOrigin({dataOrigin, type, jobId}) {
    if (!dataOrigin) return null;
    const isUws = type === 'UWS';
    const label = isUws ? 'UWS Job URL' : 'Service URL';
    if (!isUws) return <KeywordBlock label={label} value={dataOrigin} asLink={true}/>;
    return (
        <Stack direction='row' spacing={1}>
            <KeywordBlock sx={{width: 425, alignItems:'center'}} label={label} value={dataOrigin} asLink={true}/>
            <Button ml={1/2} size='sm' onClick={() => showUwsJob({jobId, jobUrl:dataOrigin})}>Show</Button>
        </Stack>
    );
}

function FinalMsg({phase, summary, error, tbl_id}) {
    if (phase === 'COMPLETED') {
        return (
            <Stack direction='row' spacing={1} alignItems='center'>
                <KeywordBlock label='Summary' value={summary}/>
                <OverflowMarker tbl_id={tbl_id} showText={true}/>
            </Stack>
        );
    } else if(phase === 'ERROR') {
        return <KeywordBlock label='Error' value={error}/>;
    } else return null;
}

export function UwsJobInfo({jobInfo, sx, isOpen=false}) {
    const hrefs = jobInfo?.results?.map((r) => r.href);
    return (
        <Stack spacing={.5} p={1} sx={sx}>
            {Object.entries(jobInfo)
                    .filter(([k]) => !['parameters', 'results', 'errorSummary', 'jobInfo'].includes(k))
                    .map(([k,v]) => <KeywordBlock key={k} label={k} value={v}/>)
            }
            {
                <KeywordBlock key='runId' label='runId' value={jobInfo?.jobInfo?.localRunId}/>
            }
            <CollapsibleGroup>
                <OptionalBlock label='parameters' value={jobInfo.parameters} isOpen={isOpen}/>
                <OptionalBlock label='results' value={hrefs} asLink={true} isOpen={isOpen}/>
                <OptionalBlock label='errorSummary' value={jobInfo.errorSummary} isOpen={isOpen}/>
            </CollapsibleGroup>

        </Stack>
    );
}

function OptionalBlock({label, value, asLink, isOpen}) {
    if (!value) return null;
    return (
        <CollapsibleItem componentKey={`JobInfo-${label}`} header={label} isOpen={isOpen}>
            <Stack spacing={.5}>
                {Object.entries(value).map(([k, v]) => <KeywordBlock key={k} label={k} value={v} asLink={asLink}/>)}
            </Stack>
        </CollapsibleItem>
    );
}
