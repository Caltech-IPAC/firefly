

import React, {Fragment} from 'react';
import {isEmpty} from 'lodash';
import {makeExample, WebApiHelpType} from '../api/WebApi';

import './WebApiHelpInfoPage.css';




export function WebApiHelpInfoPage({helpType,contextMessage='',cmd,params,webApiCommands, badParams}) {



    let showContextMsg= false;
    let msg= '';
    let showAllHelp= false;
    switch (helpType) {
        case WebApiHelpType.OVERVIEW_HELP:
            msg = 'Web API Overview Help';
            showAllHelp= true;
            break;
        case  WebApiHelpType.COMMAND_NOT_FOUND:
            msg = 'Web API unrecognized command parameter: ' + cmd;
            showAllHelp= true;
            break;
        case  WebApiHelpType.NO_COMMAND:
            msg = 'Web API: The command parameter was not specified';
            showAllHelp= true;
            break;
        case  WebApiHelpType.INVALID_PARAMS:
            msg = `Web API: command '${cmd}' has invalid parameters`;
            showContextMsg= true;
            break;
        case  WebApiHelpType.COMMAND_HELP:
            msg = 'Web API Help for command: '+ cmd;
            break;
    }

    const cmdEntry= !showAllHelp && webApiCommands.find( (c) => c.cmd===cmd);
    const overviewUrl= makeExample('').url;

    return (
        <div style={{padding: '20px 0 20px 20px', backgroundColor: 'white'}}>
            <div style={{fontSize:'18pt', textAlign:'center'}}>{msg}</div>
            {!showAllHelp && <div>To See Overview Help: <a href={overviewUrl}> {overviewUrl}</a> </div>}
            {!isEmpty(params) && <ParameterList params={{cmd, ...params}} url={window.location.href} badParams={badParams}/>}
            <div>{showContextMsg && contextMessage}</div>
            {showAllHelp && webApiCommands.map( (c) => <CommandOverview webApiCommand={c} key={c.cmd}/>)}
            {cmdEntry && <CommandOverview webApiCommand={cmdEntry} key={cmdEntry.cmd}/>}
        </div>
    );
}


function CommandOverview({webApiCommand}) {
    const {overview, parameters, examples}= webApiCommand;
    return (
        <div style={{marginTop: 20}}>
            <div >
                <span style={{fontSize:'14pt'}}>Command:</span>
                <span style={{fontFamily:'monospace', fontWeight:'bold', paddingLeft:10}}>{webApiCommand.cmd}</span>
                </div>
            <div style={{margin: '10px 0 0 20px'}}>
                {overview.map( (s) => <div key={s}>{s}</div>)}
            </div>
            {examples && <ShowExamples examples={examples}/> }
            <div style={{margin: '10px 0 0 20px'}}>
                <div>Parameters:</div>
                <div className='webApiParamsGrid'>
                    {Object.entries(parameters).map( ([k,v]) => {
                        return (
                            <Fragment key={k} >
                                <div className='webApiParamName'>{k}</div>
                                <div className='webApiDash'> - </div>
                                <div className='webApiDesc'>{v}</div>
                            </Fragment>
                        );
                    })}
                </div>
            </div>
        </div>

    );
}



function ShowExamples({examples}) {
    if (examples[0].sectionDesc) {
        return (
            <div style={{margin: '10px 0 5px 20px'}}>Examples:
                <div>
                    {examples.map( ({sectionDesc,examples:examplesGroup}) => {
                        return (
                            <div style={{margin: '10px 0 5px 10px'}} key={sectionDesc}>{sectionDesc}
                                <ShowExampleGroup examples={examplesGroup} />
                            </div>
                        );
                    })}
                </div>
            </div>
            );

    }
    else {
        return (
            <div style={{margin: '10px 0 5px 20px'}}>Examples:
                <ShowExampleGroup examples={examples}/>
            </div>
        );
    }
}

function ShowExampleGroup({examples}) {
    return (
        <div style={{margin: '10px 0 5px 20px'}}>
            <div className='webApiExamplesGrid'>
                {examples.map( (e) => {
                    return (
                        <Fragment key={e.url}>
                            <div className='webApiExName'>{e.desc}</div>
                            <div  className='webApiDash' >   </div>
                            <a href={e.url} > {e.url}</a>
                        </Fragment>

                    );}
                ) }
            </div>
        </div>
    );
}


function ParameterList({params, url, badParams=[]}) {
    // <div>{!isEmpty(params) ? JSON.stringify(params):''}</div>
    return (
        <div style={{marginTop: 10}}>
            <span style={{fontStyle: 'italic', paddingRight: 10}}>URL:</span><span>{url}</span>
            <div className='webApiParamsGrid'>
                {Object.entries(params).map( ([k,v]) => {
                    const isBad= badParams.includes(k);
                    const cName= isBad ? 'webApiParamName webApiBadParam' :'webApiParamName';
                    return (
                        v &&
                        <Fragment key={k}>
                            <div className={cName}>{k}</div>
                            <div className='webApiDash'> = </div>
                            <div className='webApiDesc'>{v}</div>
                        </Fragment>
                    );
                })
                }
            </div>
        </div>
    );
}


