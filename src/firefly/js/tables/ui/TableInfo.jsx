import React from 'react';
import PropTypes from 'prop-types';
import {Typography, Link, Stack, Chip, Sheet} from '@mui/joy';

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
        <Stack component={Sheet} variant='outlined' p={1} height={1} spacing={1} overflow='hidden' {...props}>
            <StatefulTabs componentKey='TablePanelOptions' {...tabsProps} sx={{overflow:'auto'}}>
                {jobId &&
                <Tab name='Job Info'>
                    <JobInfo jobId={jobId} tbl_id={tbl_id}/>
                </Tab>
                }
                <Tab name='Table Metadata'>
                    <Stack p={1} overflow='auto'>
                        <MetaContent tbl_id={tbl_id}/>
                    </Stack>
                </Tab>
            </StatefulTabs>
            <Stack alignItems='end' pr={1}>
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
    const keywordBlocksRowProps = { direction: 'row', alignItems: 'baseline', spacing: .5 };

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
                        return (
                            <Stack key={'groups-' + idx} {...keywordBlocksRowProps}>
                                { rs.ID && <KeywordBlock label='ID' value={rs.ID}/> }
                                { rs.name && <KeywordBlock label='name' value={rs.name}/> }
                                { rs.UCD && <KeywordBlock label='UCD' value={rs.UCD}/> }
                                { rs.utype && <KeywordBlock label='utype' value={rs.utype}/> }
                                <ShowObjectTreeBtn data={rs} title={'Group'}/>
                            </Stack>
                        );
                    })
                    }
                </CollapsibleItem>
                }
                { !isEmpty(links) &&
                <CollapsibleItem componentKey={tbl_id + '-links'} header='Links' isOpen={isOpen}>
                    {links.map((l, idx) => {
                        return (
                            <Stack key={'links-' + idx} {...keywordBlocksRowProps}>
                                { l.ID && <KeywordBlock label='ID' value={l.ID}/> }
                                { l.role && <KeywordBlock label='role' value={l.role}/> }
                                { l.type && <KeywordBlock label='type' value={l.type}/> }
                                { l.href && <LinkTag {...l}/>}
                            </Stack>
                        );
                    })
                    }
                </CollapsibleItem>
                }
                { !isEmpty(resources) &&
                <CollapsibleItem componentKey={tbl_id + '-resources'} header='Resources' isOpen={isOpen}>
                    {resources.map((rs, idx) => {
                        return (
                            <Stack key={'resources-' + idx} {...keywordBlocksRowProps}>
                                { rs.ID && <KeywordBlock label='ID' value={rs.ID}/> }
                                { rs.name && <KeywordBlock label='name' value={rs.name}/> }
                                { rs.type && <KeywordBlock label='type' value={rs.type}/> }
                                { rs.utype && <KeywordBlock label='utype' value={rs.utype}/> }
                                <ShowObjectTreeBtn data={rs} title={'Resource'}/>
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


const ShowObjectTreeBtn = ({data, title}) => {
    const popupSx = {
        '.FF-Popup-Content-root': {
            overflow: 'auto', resize: 'both', justifyContent: 'space-between',
            height: '50vh', minHeight: 200,
            width: '60vh', minWidth: 200,
        },
        '.FF-Popup-Content': {height: 1, maxWidth: 'unset', minWidth: 'unset'} //unset width bounds because controlled by resizable parent (-root)
    };
    const sx = {overflow: 'auto', width: 1, height: 1};

    return (
        <Chip onClick={() => showInfoPopup(<ObjectTree {...{data, title, sx}}/>, undefined, popupSx)}
              color='primary' sx={{alignSelf: 'center'}}> show </Chip>
    );
};

export function KeywordBlock({sx={}, label, value, title, asLink}) {
    return (
        <Stack direction='row' whiteSpace='nowrap' alignItems='baseline' sx={sx}>
            <Keyword {...{label, value, title, asLink}}/>
        </Stack>
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
                    <ContentEllipsis text={value} sx={{p: '1px', margin: 'unset'}}><Typography level='body-sm' title={value} noWrap mr={1/2}>{value}</Typography></ContentEllipsis>
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
            <Stack overflow='hidden' direction='row' spacing={.75} alignItems='baseline'>
                <CopyToClipboard value={href} size={16} buttonStyle={{backgroundColor: 'unset'}} style={{alignSelf: 'end'}}/>
                <Link level='body-sm' href={href} title={title} target='Links'
                      sx={{overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap'}}>
                    {title}
                </Link>
            </Stack>
        );
    } else return null;
}