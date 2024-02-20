import React from 'react';
import PropTypes from 'prop-types';
import {Typography, Box, Link, Stack} from '@mui/joy';

import {isEmpty} from 'lodash';
import {getTblById, hasAuxData} from '../TableUtil.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {CollapsibleGroup, CollapsibleItem} from '../../ui/panel/CollapsiblePanel.jsx';
import {ContentEllipsis} from './TableRenderer.js';
import {ObjectTree} from './ObjectTree.jsx';
import {StatefulTabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {JobInfo} from '../../core/background/JobInfo.jsx';
import {getJobIdFromTblId} from '../TableRequestUtil.js';
import {CopyToClipboard} from '../../visualize/ui/MouseReadout.jsx';
import {HelpIcon} from '../../ui/HelpIcon.jsx';


export function TableInfo({tbl_id, tabsProps, ...props}) {

    if (!tbl_id) {
        return <div>No additional Information</div>;
    }
   const jobId = getJobIdFromTblId(tbl_id);

    return (
        <Stack p={1} height={1} spacing={1} {...props}>
            <StatefulTabs componentKey='TablePanelOptions' {...tabsProps}>
                {jobId &&
                <Tab name='Job Info'>
                    <JobInfo jobId={jobId}/>
                </Tab>
                }
                <Tab name='Table Metadata'>
                    <div style={{width: '100%', height: '100%', overflow: 'auto'}}>
                        <MetaContent tbl_id={tbl_id}/>
                    </div>
                </Tab>
            </StatefulTabs>
            <Stack alignItems='flex-end'>
                <HelpIcon helpId='tables.info'/>
            </Stack>
        </Stack>
    );
}

TableInfo.propTypes = {
    tbl_id: PropTypes.string,
    tabsProps: PropTypes.object         // this get passed into TabPanel
};


function MetaContent({tbl_id}) {
    if (hasAuxData(tbl_id)) {
        return <MetaInfo tbl_id={tbl_id} isOpen={true} sx={{ w: 1, border: 'none', m: 'unset', p: 'unset'}} />;
    } else {
        return <Stack><Typography level='title-md'>No metadata available</Typography></Stack>;
    }
}


export function MetaInfo({tbl_id, isOpen=false, ...props}) {

    if (!hasAuxData(tbl_id)) {
        return null;
    }
    const {keywords, links, params, resources, groups} = getTblById(tbl_id);

    return (
        <Stack {...props}>
            <CollapsibleGroup size='sm'>
                { !isEmpty(keywords) &&
                <CollapsibleItem componentKey={tbl_id + '-meta'} header='Table Meta' isOpen={isOpen}>
                    {keywords.concat()                                             // make a copy so the original array does not mutate
                        .filter( ({key}) => key)
                        .sort(({key:k1}, {key:k2}) => (k1+'').localeCompare(k2+''))        // sort it by key
                        .map(({value, key}, idx) => <KeywordBlock key={'keywords-' + idx} label={key} value={value}/>)
                    }
                </CollapsibleItem>
                }
                { !isEmpty(params) &&
                <CollapsibleItem componentKey={tbl_id + '-params'} header='Table Params' isOpen={isOpen}>
                    {params.concat()                                                          // same logic as keywords, but sort by name
                        .sort(({name:k1}, {name:k2}) => k1.localeCompare(k2))
                        .map(({name, value, type='N/A'}, idx) => <KeywordBlock key={'params-' + idx} label={`${name}(${type})`} value={value}/>)
                    }
                </CollapsibleItem>
                }
                { !isEmpty(groups) &&
                <CollapsibleItem componentKey={tbl_id + '-groups'} header='Groups' isOpen={isOpen}>
                    {groups.map((rs, idx) => {
                        const showValue = () => showInfoPopup(
                            <div style={{whiteSpace: 'pre'}}>
                                <ObjectTree data={rs} title={<b>Group</b>} className='MetaInfo__tree'/>
                            </div> );
                        return (
                            <div key={'groups-' + idx} style={{display: 'inline-flex', alignItems: 'center'}}>
                                { rs.ID && <Keyword label='ID' value={rs.ID}/> }
                                { rs.name && <Keyword label='name' value={rs.name}/> }
                                { rs.UCD && <Keyword label='UCD' value={rs.UCD}/> }
                                { rs.utype && <Keyword label='utype' value={rs.utype}/> }
                                <a className='ff-href' onClick={showValue}> show value</a>
                            </div>
                        );
                    })
                    }
                </CollapsibleItem>
                }
                { !isEmpty(links) &&
                <CollapsibleItem componentKey={tbl_id + '-links'} header='Links' isOpen={isOpen}>
                    {links.map((l, idx) => {
                        return (
                            <div key={'links-' + idx} style={{display: 'inline-flex', alignItems: 'center'}}>
                                { l.ID && <Keyword label='ID' value={l.ID}/> }
                                { l.role && <Keyword label='role' value={l.role}/> }
                                { l.type && <Keyword label='type' value={l.type}/> }
                                { l.href && <LinkTag {...l}/>}
                            </div>
                        );
                    })
                    }
                </CollapsibleItem>
                }
                { !isEmpty(resources) &&
                <CollapsibleItem componentKey={tbl_id + '-resources'} header='Resources' isOpen={isOpen}>
                    {resources.map((rs, idx) => {
                        const showValue = () => showInfoPopup(
                            <div style={{whiteSpace: 'pre'}}>
                                <ObjectTree data={rs} title={<b>Resource</b>} className='MetaInfo__tree'/>
                            </div> );
                        return (
                            <Stack direction='row' key={'resources-' + idx}>
                                { rs.ID && <Keyword label='ID' value={rs.ID}/> }
                                { rs.name && <Keyword label='name' value={rs.name}/> }
                                { rs.type && <Keyword label='type' value={rs.type}/> }
                                { rs.utype && <Keyword label='utype' value={rs.utype}/> }
                                <Link onClick={showValue}> show value</Link>
                            </Stack>
                        );
                    })
                    }
                </CollapsibleItem>
                }

            </CollapsibleGroup>
        </Stack>
    );
}

export function KeywordBlock({style={}, label, value, title, asLink}) {
    return (
        <div style={{display: 'inline-flex', alignItems: 'center', ...style}}>
            <Keyword {...{label, value, title, asLink}}/>
        </div>
    );
}

export function Keyword({label, value, title, asLink}) {
    label = label && label + ':';
    if (label || value) {
        value = String(value);
        return (
            <>
                {label && <Typography level='title-sm' title={title} mr={1/2}>{label}</Typography>}
                { asLink ? <LinkTag title={value} href={value} /> :
                    <ContentEllipsis text={value} sx={{p: '1px', margin: 'unset'}}><Typography level='body-xs' title={value} noWrap mr={1/2}>{value}</Typography></ContentEllipsis>
                }
            </>
        );
    }
    return null;
}


export function LinkTag({href, title}) {
    title = title || href;
    if (href) {
        return (
            <Box overflow='hidden'>
                <Link level='body-xs' sx={{overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap'}}
                    href={href} title={title} target='Links'
                    startDecorator={<CopyToClipboard value={href} size={16} buttonStyle={{backgroundColor: 'unset'}}/>}
                >{title}</Link>
            </Box>
        );
    } else return null;
}