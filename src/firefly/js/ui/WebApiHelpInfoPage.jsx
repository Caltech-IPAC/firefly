

import {Box, Link, Sheet, Stack, Typography} from '@mui/joy';
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
        <Sheet sx={{py:3, pr:3}}>
            <Typography level='h2' style={{textAlign:'center'}}>{msg}</Typography>
            {!showAllHelp && <Typography component='div' sx={{ml:1}}>Overview Help: <Link href={overviewUrl}> {overviewUrl}</Link> </Typography>}
            {!isEmpty(params) && <ParameterList params={{cmd, ...params}} url={window.location.href}
                                                badParams={badParams} missingParams={missingParams}/>}
            <Typography sx={{ml:1}}>{showContextMsg && contextMessage}</Typography>
            {showAllHelp && webApiCommands.map( (c) => <CommandOverview webApiCommand={c} key={c.cmd}/>)}
            {cmdEntry && <CommandOverview webApiCommand={cmdEntry} key={cmdEntry.cmd}/>}
        </Sheet>
    );
}

const pEntryLine= (k,v,key) => (
    <Fragment key={key}>
        <Typography level='body-xs' sx={{justifySelf: 'end', fontFamily: 'monospace'}}>{k||''}</Typography>
        <Typography level='body-xs' >{k?'-':''}</Typography>
        <Typography level='body-xs'>{v}</Typography>
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
        <Stack sx={{mt: 2, ml:1}}>
            <Stack direction='row' spacing={1/2} alignItems='center'>
                <Typography level='h3'>Command:</Typography>
                <Typography level='body-lg' color='warning' style={{fontFamily:'monospace', pl:1}}>{webApiCommand.cmd}</Typography>
            </Stack>
            <Stack sx={{ml: 3, mt:1}}>
                {overview.map( (s) => <Typography key={s}>{s}</Typography>)}
            </Stack>
            {examples && <ShowExamples examples={examples}/> }
            <Stack sx={{mt:1, ml:2}}>
                <Typography level='h4'>Parameters</Typography>
                <Box sx={{ml:7}} className='webApiParamsGrid'>
                    <OverViewEntries parameters={parameters} />
                </Box>
            </Stack>
        </Stack>

    );
}



function ShowExamples({examples}) {
    if (examples[0].sectionDesc) {
        return (
            <Stack sx={{mt:1, mb:1/2, ml:3}}>
                <Typography level='h4'>Examples</Typography>
                    {examples.map( ({sectionDesc,examples:examplesGroup}) => {
                        return (
                            <Stack sx={{mt: 1, mb:1/2, ml:1}} key={sectionDesc}>
                                <Typography>{sectionDesc}</Typography>
                                <ShowExampleGroup examples={examplesGroup} />
                            </Stack>
                        );
                    })}
            </Stack>
            );

    }
    else {
        return (
            <Stack sx={{mt:1, mb:1/2, ml:3}}>Examples:
                <ShowExampleGroup examples={examples}/>
            </Stack>
        );
    }
}

function ShowExampleGroup({examples}) {
    return (
        <Box sx={{mt:1, mb:1/2, ml:1}} className='webApiExamplesGrid'>
            {examples.map( (e) => {
                return (
                    <Fragment key={e.url}>
                        <Typography level='body-sm' className='webApiExName'>{e.desc}</Typography>
                        <div/>
                        <Link level='body-sm' href={e.url}> {e.url} </Link>
                    </Fragment>
                );}
            ) }
        </Box>
    );
}


const ParameterList = ({params, url, badParams=[], missingParams=[]})  => (
        <Sheet color='danger' variant='soft' sx={{mt: 1, ml:1}}>
            <Stack direction='row' spacing={1}>
                <Typography style={{fontStyle: 'italic', paddingRight: 10}}>URL:</Typography>
                <Typography>{url}</Typography>
            </Stack>
            <Box className='webApiParamsGrid'>
                {Object.entries(params).map( ([k,v]) => {
                    const isBad= badParams.includes(k);
                    return (
                        v &&
                        <Fragment key={k}>
                            <Typography color={isBad?'danger':'neutral'} sx={{justifySelf: 'end'}}  >{k}</Typography>
                            <div className='webApiDash'> = </div>
                            <Typography className='webApiDesc' sx={{alignSelf:'center'}}>{v.toString()}</Typography>
                        </Fragment>
                    );
                })
                }
            </Box>
            {!isEmpty(missingParams) &&
                <Box>
                    <Typography >Missing or bad parameters</Typography>
                        {missingParams.map( (p) => ( <Typography color='danger' sx={{ml:18}}>{p}</Typography> )) }
                </Box>
            }
        </Sheet>
    );

