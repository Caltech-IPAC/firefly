import React from 'react';
import PropTypes from 'prop-types';

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
        <div style={{padding: 10, padding: '10px 5px 5px', height: '100%', boxSizing: 'border-box', display: 'flex', flexDirection: 'column'}}>
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
                <HelpIcon helpId={''} style={{float: 'right'}}/>
            </div>
        </div>
    );
}

TableInfo.propTypes = {
    tbl_id: PropTypes.string
};


function MetaContent({tbl_id}) {
    if (hasAuxData(tbl_id)) {
        return <MetaInfo tbl_id={tbl_id} isOpen={true} style={{ width: '100%', border: 'none', margin: 'unset', padding: 'unset'}} />;
    } else {
        return <div style={{margin: 20, fontWeight: 'bold'}}>No metadata available</div>;
    }
}


export function MetaInfo({tbl_id, style, isOpen=false}) {
    const contentStyle={display: 'flex', flexDirection: 'column', maxHeight: 200, overflow: 'auto', paddingBottom: 1};
    const wrapperStyle={width: '100%'};

    if (!hasAuxData(tbl_id)) {
        return null;
    }
    const {keywords, links, params, resources, groups} = getTblById(tbl_id);

    return (
        <div className='TablePanel__meta' style={style}>
            { !isEmpty(keywords) &&
            <CollapsiblePanel componentKey={tbl_id + '-meta'} header='Table Meta' {...{isOpen, contentStyle, wrapperStyle}}>
                {keywords.concat()                                             // make a copy so the original array does not mutate
                    .filter( ({key}) => key)
                    .sort(({key:k1}, {key:k2}) => (k1+'').localeCompare(k2+''))        // sort it by key
                    .map(({value, key}, idx) => <KeywordBlock key={'keywords-' + idx} label={key} value={value}/>)
                }
            </CollapsiblePanel>
            }
            { !isEmpty(params) &&
            <CollapsiblePanel componentKey={tbl_id + '-params'} header='Table Params' {...{isOpen, contentStyle, wrapperStyle}}>
                {params.concat()                                                          // same logic as keywords, but sort by name
                    .sort(({name:k1}, {name:k2}) => k1.localeCompare(k2))
                    .map(({name, value, type='N/A'}, idx) => <KeywordBlock key={'params-' + idx} label={`${name}(${type})`} value={value}/>)
                }
            </CollapsiblePanel>
            }
            { !isEmpty(groups) &&
            <CollapsiblePanel componentKey={tbl_id + '-groups'} header='Groups' {...{isOpen, contentStyle, wrapperStyle}}>
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
            <CollapsiblePanel componentKey={tbl_id + '-links'} header='Links' {...{isOpen, contentStyle, wrapperStyle}}>
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
            <CollapsiblePanel componentKey={tbl_id + '-resources'} header='Resources' {...{isOpen, contentStyle, wrapperStyle}}>
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
        </div>
    );
}

export function KeywordBlock({style={}, label, value, title, asLink}) {
    return (
        <div style={{display: 'inline-flex', alignItems: 'center', ...style}}>
            <Keyword {...{label, value, title, asLink}}/>
        </div>
    );
}

export function Keyword({style={}, label, value, title, asLink}) {
    label = label && label + ':';
    if (label || value) {
        return (
            <React.Fragment>
                {label && <div title={title} className='keyword-label'>{label}</div>}
                { asLink ? <LinkTag title={value} href={value} /> :
                    <ContentEllipsis text={value} style={{padding: 1, margin: 'unset'}}><div title={value} style={{whiteSpace: 'nowrap',...style}} className='keyword-value'>{value}</div></ContentEllipsis>
                }
            </React.Fragment>
        );
    }
    return null;
}


export function LinkTag({href, title}) {
    title = title || href;
    if (href) {
        return (
            <div style={{display: 'inline-flex', alignItems: 'center', overflow: 'hidden'}}>
                <CopyToClipboard value={href} size={16} buttonStyle={{backgroundColor: 'unset'}}/>
                <a className='ff-href' style={{padding: '0 3px', verticalAlign: 'middle', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', lineHeight: 1.5}}
                   href={href} title={title} target='Links'>{title}</a>
            </div>
        );
    } else return null;
}