/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {Alert, Stack} from '@mui/joy';

import {getDropDownInfo} from '../core/LayoutCntlr.js';
import {SearchPanel} from '../ui/SearchPanel.jsx';
import {IrsaCatalogSearch} from '../visualize/ui/IrsaCatalogSearch.jsx';
import {ClassicNedSearchPanel, ClassicVOCatalogPanel} from '../visualize/ui/ExtraIpacSearches.jsx';
import {ImageSearchDropDown} from '../visualize/ui/ImageSearchPanelV2.jsx';
import {TestSearchPanel} from '../ui/TestSearchPanel.jsx';
import {TestQueriesPanel} from '../ui/TestQueriesPanel.jsx';
import {ChartSelectDropdown} from '../ui/ChartSelectDropdown.jsx';
import {FileUploadDropdown} from '../ui/FileUploadDropdown.jsx';
import {WorkspaceDropdown} from '../ui/WorkspaceDropdown.jsx';
import {getAlerts} from '../core/AppDataCntlr.js';
import {MultiSearchPanel} from 'firefly/ui/MultiSearchPanel.jsx';
import {TapSearchPanel} from 'firefly/ui/tap/TapSearchRootPanel.jsx';
import {DLGeneratedDropDown} from './dynamic/DLGeneratedDropDown.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent.jsx';


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
    MultiTableSearchCmd: {view: <MultiSearchPanel/>,  layout: {width: '100%'}},
    FileUploadDropDownCmd: {view: <FileUploadDropdown />, layout: {width: '100%'}},
    WorkspaceDropDownCmd: {view: <WorkspaceDropdown />},
    DLGeneratedDropDownCmd: {view: <DLGeneratedDropDown/>, layout: {width: '100%'}},
    IrsaCatalog: {view: <IrsaCatalogSearch/>, layout: {width: '100%'}},
    ClassicVOCatalogPanelCmd : {view: <ClassicVOCatalogPanel/>, layout: {width: '100%'}},
    ClassicNedSearchCmd : {view: <ClassicNedSearchPanel/>, layout: {width: '100%'}},
    // --- testing
    TestSearch: {view: <TestSearchPanel />, layout: {width: '100%'}},
    TestSearches: {view: <TestQueriesPanel />, layout: {width: '100%'}}

};




/*
 * The container for items appearing in the drop down panel.
 * This container mimic a card layout in which it will accept multiple cards.
 * However, only one selected card will be displayed at a time.
 * Items in this container must have a 'name' property.  It will be used to
 * compare to the selected card.
 */
export function DropDownContainer ({style={}, visible:defVisible, selected:defSelected, dropdownPanels, defaultView, watchInitArgs= true, children}) {

    const visible   = useStoreConnector(() => getDropDownInfo()?.visible ?? defVisible);
    const selected  = useStoreConnector(() => getDropDownInfo()?.view ?? defSelected);       // the selected view name
    const initArgs  = useStoreConnector(() => watchInitArgs ? getDropDownInfo()?.initArgs : undefined);
    const [, setInit] = useState();

    let {view, layout} = dropDownMap[selected] || {};

    view ??= defaultView;

    useEffect( () => {
        React.Children.forEach(children, (el) => {
            const {name:key, layout} = el?.props || {};
            if (key) dropDownMap[key] = {view: el, layout};
        });

        if (dropdownPanels) {
            dropdownPanels.forEach( (el) => {
                const {name:key, layout} = el?.props || {};
                if (key) dropDownMap[key] = {view: el, layout};
            } );
        }
        setInit(true);
    }, [dropdownPanels]);

    if (!view && React.Children.count(children) === 1 && React.isValidElement(children)) view = React.Children.toArray(children)[0];
    if (!visible) return <div/>;

    if (layout) Object.assign(style, layout);

    return (
        <Stack flexGrow={1}>
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
DropDownContainer.defaultProps = {
    visible: false
};

export function Alerts() {

    const {msg} = useStoreConnector(getAlerts);
    if (msg) {
        /* eslint-disable react/no-danger */
        return (
            <Alert variant='outlined' color='warning'
                   sx={{
                       p:1/2,
                       justifyContent:'center',
                       borderRadius:0,
                       backgroundColor:'#FFF7C2'            // retro look
                   }}
            >
                <div dangerouslySetInnerHTML={{__html: msg}} />
            </Alert>
        );
    } else return null;
}
