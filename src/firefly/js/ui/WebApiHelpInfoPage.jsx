

import React, {Fragment} from 'react';
import {isEmpty,isObject,isArray} from 'lodash';
import {getReservedParamKeyDesc, makeExample, ReservedParams, WebApiHelpType} from '../api/WebApi';

import './WebApiHelpInfoPage.css';




export function WebApiHelpInfoPage({helpType,contextMessage='',cmd,params,webApiCommands, badParams, missingParams}) {
    let showContextMsg= false;
    let msg= '';
    let showAllHelp= false;
    switch (helpType) {
        case WebApiHelpType.OVERVIEW_HELP:
            msg = 'Web API Help';
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
            {!isEmpty(params) && <ParameterList params={{cmd, ...params}} url={window.location.href}
                                                badParams={badParams} missingParams={missingParams}/>}
            <div>{showContextMsg && contextMessage}</div>
            {showAllHelp && webApiCommands.map( (c) => <CommandOverview webApiCommand={c} key={c.cmd}/>)}
            {cmdEntry && <CommandOverview webApiCommand={cmdEntry} key={cmdEntry.cmd}/>}
        </div>
    );
}

const pEntryLine= (k,v,key) => (
    <Fragment key={key}>
        <div className='webApiParamName'>{k||''}</div>
        <div className='webApiDash'>{k?'-':''}</div>
        <div className='webApiDesc'>{v}</div>
    </Fragment>
);

function OverViewEntries({parameters}) {
    const rpKeys= Object.keys(ReservedParams);
    return Object.entries(parameters).map( ([k,v]) => {
        let desc= v;
        let rStr= '';
        const keyStr= rpKeys.includes(k) ? getReservedParamKeyDesc(k) : k;
        if (isObject(v)) {
            desc= v.desc;
            rStr= v.isRequired ? ' (required)' : '';
        }

        const firstLine= (kIn,vIn) => pEntryLine(kIn,`${vIn}${rStr}`,kIn+'-0');

        if (isArray(v)) {
            return v.map( (vEntry,idx) => idx===0 ? firstLine(keyStr,vEntry,v) : pEntryLine('',vEntry,keyStr+'-'+idx));
        }
        else {
            return firstLine(keyStr,desc);
        }
    }).flat();
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
                    <OverViewEntries parameters={parameters} />
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


const ParameterList = ({params, url, badParams=[], missingParams=[]})  => (
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
                            <div className='webApiDesc'>{v.toString()}</div>
                        </Fragment>
                    );
                })
                }
            </div>
            {!isEmpty(missingParams) &&
                <div>
                    <div style={{marginLeft: 20, fontSize: 'smaller'}}>Missing required parameters:</div>
                        {missingParams.map( (p) => {
                            const cName= 'webApiParamName webApiBadParam';
                            return (
                                <div style={{marginLeft:145}} className={cName}>{p}</div>
                            );
                        })
                        }
                </div>
            }
        </div>
    );

