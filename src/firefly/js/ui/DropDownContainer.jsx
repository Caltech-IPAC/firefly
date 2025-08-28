/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {Alert, Stack} from '@mui/joy';

import {getDropDownInfo} from '../core/LayoutCntlr.js';
import {GatorProtocolRootPanel} from '../visualize/ui/GatorProtocolRootPanel';
import {SearchPanel} from './SearchPanel';
import {HiPSSearchPanel} from '../visualize/ui/HiPSSearchPanel.jsx';
import {IrsaCatalogSearchDefault} from '../visualize/ui/IrsaCatalogSearch.jsx';
import {ClassicNedSearchPanel, ClassicVOCatalogPanel} from '../visualize/ui/ExtraIpacSearches.jsx';
import {ImageSearchDropDown} from '../visualize/ui/ImageSearchPanelV2.jsx';
import {SIAv2SearchPanel} from './tap/SIASearchRootPanel';
import {TestSearchPanel} from './TestSearchPanel';
import {TestQueriesPanel} from './TestQueriesPanel';
import {ChartSelectDropdown} from './ChartSelectDropdown';
import {FileUploadDropdown} from './FileUploadDropdown';
import {WorkspaceDropdown} from './WorkspaceDropdown';
import {getAlerts} from '../core/AppDataCntlr.js';
import {MultiSearchPanel} from 'firefly/ui/MultiSearchPanel.jsx';
import {TapSearchPanel} from 'firefly/ui/tap/TapSearchRootPanel.jsx';
import {DLGeneratedDropDown} from './dynamic/DLGeneratedDropDown.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';
import {JobMonitor} from '../core/background/JobMonitor';
import {isHtml} from 'firefly/tables/TableUtil';


export const flexGrowWithMax = {width: '100%', maxWidth: 1400};

export const irsaStandMultiOptions= [
    {id: 'irsacat', title:'IRSA'},
    {id: 'nedcat'},
    {id: 'upload'},
    {id: 'tap', title:'VO TAP Search'},
    {id: 'vocat'},
];

export const dropDownMap = {
    Search: {view: <SearchPanel />},
    ImageSelectDropDownCmd: {view: <ImageSearchDropDown />, layout: flexGrowWithMax},
    ImageSelectDropDownSlateCmd: {view: <ImageSearchDropDown gridSupport={true} />, layout: flexGrowWithMax},
    ChartSelectDropDownCmd: {view: <ChartSelectDropdown />},
    TAPSearch: {view: <TapSearchPanel/>, layout: {width: '100%'}},
    SIAv2Search: {view: <SIAv2SearchPanel/>, layout: {width: '100%'}},
    MultiTableSearchCmd: {view: <MultiSearchPanel/>,  layout: {width: '100%'}},
    FileUploadDropDownCmd: {view: <FileUploadDropdown />, layout: {width: '100%'}},
    BackgroundMonitorCmd: {view: <JobMonitor />, layout: {width: '100%'}},
    WorkspaceDropDownCmd: {view: <WorkspaceDropdown />},
    DLGeneratedDropDownCmd: {view: <DLGeneratedDropDown name='DLGeneratedDropDownCmd' loadRegistry={false}/>, layout: {width: '100%'}},
    IrsaCatalog: {view: <IrsaCatalogSearchDefault/>, layout: {width: '100%'}},
    GatorProtocolRootPanel: {view:<GatorProtocolRootPanel name='GatorProtocol' title={'LSDB'}/>, layout: {width: '100%'}},
    ClassicVOCatalogPanelCmd : {view: <ClassicVOCatalogPanel/>, layout: {width: '100%'}},
    ClassicNedSearchCmd : {view: <ClassicNedSearchPanel/>, layout: {width: '100%'}},
    // --- testing
    TestSearch: {view: <TestSearchPanel />, layout: {width: '100%'}},
    TestSearches: {view: <TestQueriesPanel />, layout: {width: '100%'}},
    HiPSSearchPanel: {view: <HiPSSearchPanel name='HiPSSearchPanel'/>, layout: {width: '100%'}}
};




/*
 * The container for items appearing in the drop down panel.
 * This container mimic a card layout in which it will accept multiple cards.
 * However, only one selected card will be displayed at a time.
 * Items in this container must have a 'name' property.  It will be used to
 * compare to the selected card.
 */
export function DropDownContainer ({style={}, visible:defVisible=false, selected:defSelected, dropdownPanels, defaultView, watchInitArgs= true, children}) {

    const visible   = useStoreConnector(() => getDropDownInfo()?.visible ?? defVisible);
    const selected  = useStoreConnector(() => getDropDownInfo()?.view ?? defSelected);       // the selected view name
    const initArgs  = useStoreConnector(() => watchInitArgs ? getDropDownInfo()?.initArgs : undefined);
    const [, setInit] = useState();

    let {view, layout} = dropDownMap[selected] || {};

    if (!view && React.Children.count(children) === 1 && React.isValidElement(children)) view = React.Children.toArray(children)[0];

    view ??= defaultView;

    useEffect( () => {
        if (dropdownPanels) {
            dropdownPanels.forEach( (el) => {
                const {name:key, layout} = el?.props || {};
                if (key) dropDownMap[key] = {view: el, layout};
            } );
        }
        setInit(true);
    }, [dropdownPanels]);

    if (!visible) return <div/>;

    if (layout) Object.assign(style, layout);

    return (
        <Stack flexGrow={1} maxHeight={1}>
            {view && React.cloneElement(view, { initArgs } ) }
        </Stack>
    );
}

DropDownContainer.propTypes = {
    visible: PropTypes.bool,
    selected: PropTypes.string,
    dropdownPanels: PropTypes.arrayOf(PropTypes.element),
    alerts: PropTypes.node,
    watchInitArgs: PropTypes.bool
};

export function Alerts() {
    let { msg } = useStoreConnector(getAlerts);
    
    if (!msg) return null;

    const looksHtml = isHtml(msg);

    if (!looksHtml) { //escape special chars then render safely as HTML below
        msg = msg
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/\n/g, '<br/>');
    }

    return (
        <Alert variant='soft' color='warning' className='ff-alert'
               sx={{p: 0.5, borderRadius: 0, justifyContent: 'center', width: '100%', textAlign: 'center',
                '& p': { m: 0 }, '& ul, & ol': { m: 0, pl: 2 },
                '& a': {
                    color: 'inherit',
                    textDecoration: 'underline',
                    fontWeight: 600,
                }
            }}
        >
            {/* eslint-disable-next-line react/no-danger */}
            <div style={{ width: '100%' }} className='ff-alert-content' dangerouslySetInnerHTML={{ __html: msg }} />
        </Alert>
    );
}


