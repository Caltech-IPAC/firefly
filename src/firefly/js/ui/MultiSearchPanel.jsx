import React from 'react';
import {once} from 'lodash';
import {
    ClassicCatalogUploadPanel,
    ClassicNedSearchPanel,
    ClassicVOCatalogPanel
} from 'firefly/visualize/ui/ExtraIpacSearches.jsx';
import {StatefulTabs, Tab} from 'firefly/ui/panel/TabPanel.jsx';
import {TapSearchPanel} from 'firefly/ui/tap/TapSearchRootPanel.jsx';
import {CatalogSelectViewPanel} from 'firefly/visualize/ui/CatalogSelectViewPanel.jsx';
import {FileUploadDropdown} from 'firefly/ui/FileUploadDropdown.jsx';
import {getAppOptions} from 'firefly/api/ApiUtil.js';

const multiSearchComponents= [
    {
        id: 'irsacat',
        title: 'IRSA Catalogs',
        tip: 'Query IRSA Tables',
        Component: CatalogSelectViewPanel,
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

const getDefTabIdx= once((initArgs) => {
    if (!initArgs.defaultSelectedId) return 0;
    const idx= getComponentAry().findIndex( ({id}) => id===initArgs.defaultSelectedId);
    return idx>-1 ? idx : 0;
});

const makeTabLabel= (str) => (<div style={tabStyle}>{str}</div>);

export function MultiSearchPanel({initArgs={}}) {

    return (
        <div style={{padding: '15px 5px 0 5px', flex:'1 1 0', position:'relative'}}>
            <div style={{fontSize:'large', fontWeight: 'bold', position:'absolute', left: 5, top: 6, alignSelf: 'center' }}>
                Table Search
            </div>
            <StatefulTabs componentKey='MultiCatalogTabs' defaultSelected={getDefTabIdx(initArgs)}
                          borderless={true} useFlex={true} style={{flex: '1 1 0', height: '100%'}}
                          label={<div style={{paddingLeft: 150}}/>}>
                {getComponentAry().map( ({id, title,tip,Component}) => (
                    <Tab name={tip} id={id} key={id} label={makeTabLabel(title)}>
                        <Component initArgs={initArgs}/>
                    </Tab>
                )) }
            </StatefulTabs>
        </div>
    );
}