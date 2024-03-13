import {Sheet, Stack, Typography} from '@mui/joy';
import React from 'react';
import {once} from 'lodash';
import {
    ClassicCatalogUploadPanel,
    ClassicNedSearchPanel,
    ClassicVOCatalogPanel
} from 'firefly/visualize/ui/ExtraIpacSearches.jsx';
import {StatefulTabs, Tab} from 'firefly/ui/panel/TabPanel.jsx';
import {TapSearchPanel} from 'firefly/ui/tap/TapSearchRootPanel.jsx';
import {IrsaCatalogSearch} from 'firefly/visualize/ui/IrsaCatalogSearch.jsx';
import {FileUploadDropdown} from 'firefly/ui/FileUploadDropdown.jsx';
import {getAppOptions} from 'firefly/api/ApiUtil.js';
import {dispatchComponentStateChange} from 'firefly/core/ComponentCntlr.js';

const multiSearchComponents= [
    {
        id: 'irsacat',
        title: 'IRSA Catalogs',
        tip: 'Query IRSA Tables',
        Component: IrsaCatalogSearch,
    },
    {
        id: 'tap',
        title: 'VO TAP Searches',
        tip: 'Query Virtual Observatory Table Access Protocol Services',
        Component: ({initArgs}) => <TapSearchPanel initArgs={initArgs} titleOn={false} />
    },
    {
        id: 'classicCatalogUpload_DEPRECATED',
        title: 'Load Catalog File',
        tip: 'Load a file from disk or IRSA Workspace',
        Component: ClassicCatalogUploadPanel
    },
    {
        id: 'upload',
        title: 'Load Catalog File',
        tip: 'Load a file from disk or IRSA Workspace',
        Component: FileUploadDropdown,
    },
    {
        id: 'vocat',
        title: 'VO SCS Search',
        tip: 'Query Virtual Observatory Simple Cone Search Services',
        Component: ClassicVOCatalogPanel
    },
    {
        id: 'nedcat',
        title: 'NED',
        tip: 'Query Object Table',
        Component: ClassicNedSearchPanel,
    }
];

const defPanelList= [ {id: 'tap'}, {id: 'irsacat'}, {id: 'upload'}, {id: 'vocat'}, {id: 'nedcat'}];
const tabStyle= {fontSize:'11pt', textShadow: '1px 1px 0 rgb(238 238 238)'};

const getComponentAry= once(() => {
    const commands= getAppOptions()?.multiTableSearchCmdOptions ?? defPanelList;
    return commands
        .map( ({id,title,tip}) => {
            const c= multiSearchComponents.find( (c) => c.id===id);
            return c && {id, title: title??c.title,Component:c.Component,tip:tip??c.tip};
        })
        .filter( (c) => c);
} );

const getDefTabId= once((initArgs) => getTabId(initArgs));

function getTabId(args) {
    return args.defaultSelectedId;
}

const makeTabLabel= (str) => (<div style={tabStyle}>{str}</div>);

export function setMultiSearchPanelTab(setId) {
    const selectedIdx= getComponentAry().findIndex( ({id}) => setId===id);
    if (selectedIdx>-1) {
        dispatchComponentStateChange('MultiCatalogTabs', {selectedIdx});
    }
}


export function MultiSearchPanel({initArgs={}}) {

    return (
        <Stack flexGrow={1}>
            <Typography level='h4'>
                Table Search
            </Typography>
            <StatefulTabs componentKey='MultiCatalogTabs' defaultSelected={getDefTabId(initArgs)}>
                {getComponentAry().map( ({id, title,tip,Component}) => (
                    <Tab name={tip} id={id} key={id} label={title}>
                        <Component initArgs={initArgs}/>
                    </Tab>
                )) }
            </StatefulTabs>
        </Stack>
    );
}