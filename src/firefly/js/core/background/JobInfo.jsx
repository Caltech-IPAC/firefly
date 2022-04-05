import React from 'react';

import {getJobInfo} from './BackgroundUtil.js';
import {CollapsiblePanel} from '../../ui/panel/CollapsiblePanel.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {KeywordBlock, Keyword} from '../../tables/ui/TableInfo.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog} from '../ComponentCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';


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



export function JobInfo({jobId, style}) {
    const {phase, startTime, endTime, dataOrigin, results, error, summary} = useStoreConnector(() => getJobInfo(jobId) || {});
    return (
        <div className='JobInfo__main' style={style}>
            <div className='JobInfo__items'>
                <KeywordBlock label='Phase' value={phase}/>
                <KeywordBlock label='Start Time' value={startTime}/>
                {endTime && <KeywordBlock label='End Time' value={endTime}/>}
                {dataOrigin && <KeywordBlock label='Job Link' value={dataOrigin} asLink={true}/>}
                <FinalMsg {...{phase, results, error, summary}}/>
                <KeywordBlock label='ID' value={jobId} title='Internal query identifier, usable for diagnostics'/>
            </div>
        </div>
    );
}

function FinalMsg({phase, summary, error}) {
    if (phase === 'COMPLETED') {
        return <KeywordBlock label='Summary' value={summary}/>;
    } else if(phase === 'ERROR') {
        return <KeywordBlock label='Error' value={error}/>;
    } else return null;
}
