import React from 'react';

import {getJobInfo} from './BackgroundUtil.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {KeywordBlock} from '../../tables/ui/TableInfo.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog} from '../ComponentCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {CollapsiblePanel} from 'firefly/ui/panel/CollapsiblePanel.jsx';
import {uwsJobInfo} from 'firefly/rpc/SearchServicesJson.js';
import {Button, Stack} from '@mui/joy';
import {OverflowMarker} from 'firefly/tables/ui/TablePanel.jsx';


export function showJobInfo(jobId) {
    const ID = 'show-job-info';
    const popup = (
        <PopupPanel title='Job Information' >
            <div key={jobId} className='JobInfo__popup'>
                <JobInfo jobId={jobId}/>
                <div style={{margin: '2px 19px 6px'}}>
                    <HelpIcon helpId={'basics.bgJobInfo'} style={{float: 'right'}}/>
                </div>
            </div>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(ID, popup);
    dispatchShowDialog(ID);
}

export async function showUwsJob({jobUrl, jobId}) {

    const id = 'show-uws-job-info';
    const mask = (
        <PopupPanel title='UWS Job' >
            <div key={jobId} className='JobInfo__popup uws'>
                <div className='loading-mask'/>
            </div>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(id, mask);
    dispatchShowDialog(id);

    const jobInfo = await uwsJobInfo(jobUrl, jobId);
    const popup = (
        <PopupPanel title='UWS Job' >
            <div key={jobId} className='JobInfo__popup uws'>
                <UwsJobInfo jobInfo={jobInfo}/>
                <div style={{margin: '2px 19px 6px'}}>
                    <HelpIcon helpId={'basics.uwsJob'} style={{float: 'right'}}/>
                </div>
            </div>
        </PopupPanel>
    );

    DialogRootContainer.defineDialog(id, popup);
    dispatchShowDialog(id);
}


export function JobInfo({jobId, style, tbl_id}) {
    const {phase, startTime, endTime, results, errorSummary, jobInfo={}} = useStoreConnector(() => getJobInfo(jobId) || {});
    const {dataOrigin, summary, type} = jobInfo;
    return (
        <div className='JobInfo__main' style={style}>
            <div className='JobInfo__items'>
                <KeywordBlock label='Phase' value={phase}/>
                <KeywordBlock label='Start Time' value={startTime}/>
                {endTime && <KeywordBlock label='End Time' value={endTime}/>}
                <DataOrigin {...{dataOrigin, type, jobId}}/>
                <FinalMsg {...{phase, results, errorSummary, summary, tbl_id}}/>
                <KeywordBlock label='ID' value={jobId} title='Internal query identifier, usable for diagnostics'/>
            </div>
        </div>
    );
}

function DataOrigin({dataOrigin, type, jobId}) {
    if (!dataOrigin) return null;
    const isUws = type === 'UWS';
    const label = isUws ? 'UWS Job URL' : 'Service URL';
    if (!isUws) return <KeywordBlock label={label} value={dataOrigin} asLink={true}/>;
    return (
        <div style={{display: 'inline-flex'}}>
            <KeywordBlock style={{width: 425}} label={label} value={dataOrigin} asLink={true}/>
            <Button ml={1/2} onClick={() => showUwsJob({jobId})}>Show</Button>
        </div>
    );
}

function FinalMsg({phase, summary, error, tbl_id}) {
    if (phase === 'COMPLETED') {
        return (
            <Stack direction='row' spacing={1} alignItems='center'>
                <KeywordBlock label='Summary' value={summary}/>
                <OverflowMarker tbl_id={tbl_id}/>
            </Stack>
        );
    } else if(phase === 'ERROR') {
        return <KeywordBlock label='Error' value={error}/>;
    } else return null;
}

export function UwsJobInfo({jobInfo, style, isOpen=false}) {
    const hrefs = jobInfo?.results?.map((r) => r.href);
    return (
        <div className='JobInfo__main' style={style}>
            <div className='JobInfo__items uws' style={{overflowY: 'auto'}}>
                <> {
                    Object.entries(jobInfo)
                        .filter(([k]) => !['parameters', 'results', 'errorSummary', 'jobInfo'].includes(k))
                        .map(([k,v]) => <KeywordBlock key={k} label={k} value={v}/>)
                }</>

                <OptionalBlock label='parameters' value={jobInfo.parameters} isOpen={isOpen}/>
                <OptionalBlock label='results' value={hrefs} asLink={true} isOpen={isOpen}/>
                <OptionalBlock label='errorSummary' value={jobInfo.errorSummary} isOpen={isOpen}/>

            </div>
        </div>
    );
}

function OptionalBlock({label, value, asLink, isOpen}) {
    if (!value) return null;
    return (
        <CollapsiblePanel componentKey={`JobInfo-${label}`} header={label} isOpen={isOpen}>
            <div className='JobInfo__items'>
                <> {
                    Object.entries(value).map(([k,v]) => <KeywordBlock key={k} label={k} value={v} asLink={asLink}/>)
                }</>
            </div>
        </CollapsiblePanel>
    );
}
