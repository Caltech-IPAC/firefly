import React from 'react';
import PropTypes from 'prop-types';
import {Typography, Box, Link, Stack} from '@mui/joy';

import {isEmpty} from 'lodash';
import {getTblById, hasAuxData} from '../TableUtil.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {CollapsiblePanel} from '../../ui/panel/CollapsiblePanel.jsx';
import {ContentEllipsis} from './TableRenderer.js';
import {ObjectTree} from './ObjectTree.jsx';
import {StatefulTabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {JobInfo} from '../../core/background/JobInfo.jsx';
import {getJobIdFromTblId} from '../TableRequestUtil.js';
import {CopyToClipboard} from '../../visualize/ui/MouseReadout.jsx';
import {HelpIcon} from '../../ui/HelpIcon.jsx';


export function TableInfo(props) {

    const {tbl_id} = props;
    if (!tbl_id) {
        return <div>No additional Information</div>;
    }
   const jobId = getJobIdFromTblId(tbl_id);

    return (
        <Stack p={1} height={1} boxSizing='border-box'>
            <StatefulTabs componentKey='TablePanelOptions' defaultSelected={0} borderless={true} useFlex={true} style={{flex: '1 1 0'}}>
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
            <div>
                <HelpIcon helpId={'tables.info'} style={{float: 'right', marginRight: 10}}/>
            </div>
        </Stack>
    );
}

TableInfo.propTypes = {
    tbl_id: PropTypes.string
};


function MetaContent({tbl_id}) {
    if (hasAuxData(tbl_id)) {
        return <MetaInfo tbl_id={tbl_id} isOpen={true} sx={{ w: 1, border: 'none', m: 'unset', p: 'unset'}} />;
    } else {
        return <div style={{margin: 20, fontWeight: 'bold'}}>No metadata available</div>;
    }
}


export function MetaInfo({tbl_id, sx, isOpen=false}) {
    const contentStyle={display: 'flex', flexDirection: 'column', overflow: 'hidden', paddingBottom: 1};
    const collapsiblePanelStyle={width: '100%'};

    if (!hasAuxData(tbl_id)) {
        return null;
    }
    const {keywords, links, params, resources, groups} = getTblById(tbl_id);

    return (
        <Stack sx={sx}>
            { !isEmpty(keywords) &&
            <CollapsiblePanel componentKey={tbl_id + '-meta'} header='Table Meta' style={collapsiblePanelStyle} {...{isOpen, contentStyle}}>
                {keywords.concat()                                             // make a copy so the original array does not mutate
                    .filter( ({key}) => key)
                    .sort(({key:k1}, {key:k2}) => (k1+'').localeCompare(k2+''))        // sort it by key
                    .map(({value, key}, idx) => <KeywordBlock key={'keywords-' + idx} label={key} value={value}/>)
                }
            </CollapsiblePanel>
            }
            { !isEmpty(params) &&
            <CollapsiblePanel componentKey={tbl_id + '-params'} header='Table Params' style={collapsiblePanelStyle} {...{isOpen, contentStyle}}>
                {params.concat()                                                          // same logic as keywords, but sort by name
                    .sort(({name:k1}, {name:k2}) => k1.localeCompare(k2))
                    .map(({name, value, type='N/A'}, idx) => <KeywordBlock key={'params-' + idx} label={`${name}(${type})`} value={value}/>)
                }
            </CollapsiblePanel>
            }
            { !isEmpty(groups) &&
            <CollapsiblePanel componentKey={tbl_id + '-groups'} header='Groups' style={collapsiblePanelStyle} {...{isOpen, contentStyle}}>
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
            </CollapsiblePanel>
            }
            { !isEmpty(links) &&
            <CollapsiblePanel componentKey={tbl_id + '-links'} header='Links' style={collapsiblePanelStyle} {...{isOpen, contentStyle}}>
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
            </CollapsiblePanel>
            }
            { !isEmpty(resources) &&
            <CollapsiblePanel componentKey={tbl_id + '-resources'} header='Resources' style={collapsiblePanelStyle} {...{isOpen, contentStyle}}>
                {resources.map((rs, idx) => {
                    const showValue = () => showInfoPopup(
                        <div style={{whiteSpace: 'pre'}}>
                            <ObjectTree data={rs} title={<b>Resource</b>} className='MetaInfo__tree'/>
                        </div> );
                    return (
                        <div key={'resources-' + idx} style={{display: 'inline-flex', alignItems: 'center'}}>
                            { rs.ID && <Keyword label='ID' value={rs.ID}/> }
                            { rs.name && <Keyword label='name' value={rs.name}/> }
                            { rs.type && <Keyword label='type' value={rs.type}/> }
                            { rs.utype && <Keyword label='utype' value={rs.utype}/> }
                            <a className='ff-href' onClick={showValue}> show value</a>
                        </div>
                    );
                })
                }
            </CollapsiblePanel>
            }
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
                    <ContentEllipsis text={value} style={{padding: 1, margin: 'unset'}}><Typography level='body-xs' title={value} noWrap mr={1/2}>{value}</Typography></ContentEllipsis>
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