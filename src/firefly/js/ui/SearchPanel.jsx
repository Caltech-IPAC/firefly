/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {Sheet, Stack, Tooltip, Typography, Button, ToggleButtonGroup} from '@mui/joy';
import {set} from 'lodash';

import {dispatchUpdateAppData} from '../core/AppDataCntlr.js';
import {getSearchInfo} from '../core/AppDataCntlr.js';
import {FormPanel} from './FormPanel.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import {StatefulTabs, Tab} from './panel/TabPanel.jsx';
import {makeSearchOnce} from '../util/WebUtil';
import {useNavigate, useInRouterContext} from 'react-router-dom';

const changeSearchOptionOnce= makeSearchOnce(); // setup options to immediately execute the search the first time

export function SearchPanel({style={}, initArgs={}}) {
    const searchPanelState = useStoreConnector(getSearchInfo);

    const {activeSearch, groups, allSearchItems={}, flow='vertical', title} = searchPanelState;
    const searchItem = allSearchItems[activeSearch];

    useEffect(() => {
        const {searchoption, callId}=initArgs?.urlApi ?? {};
        changeSearchOptionOnce(
            Boolean(searchoption),
            () => dispatchUpdateAppData({searches: {activeSearch: searchoption}}),
            callId);
    });

    const isSingleSearch = Object.keys(allSearchItems).length === 1;
    if (isSingleSearch) {
        return (
            <Stack id='search-vertical' flexGrow={1}>
                {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
                <SearchForm style={{height: 'auto'}} searchItem={searchItem} initArgs={initArgs}/>
            </Stack>
        );
    }

    if (flow === 'vertical') {
        const sideBar = <SideBar {...{activeSearch, groups}}/> ;
        return (
            <Stack id='search-vertical' flexGrow={1}>
                {title && <Typography textAlign='center' level='h2'>{title}</Typography>}
                <Stack direction='row' flexGrow={1} sx={style}>
                    {sideBar}
                    {searchItem &&
                    <Stack flexGrow={1} p={1}>
                        <SearchForm searchItem={searchItem}  initArgs={initArgs}/>
                    </Stack>
                    }
                </Stack>
            </Stack>
        );
    } else {
        const onTabSelect = (index,id,name) => dispatchUpdateAppData(set({}, ['searches', 'activeSearch'], name));
        return (
            <Stack id='search-tabs' flexGrow={1}>
                {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
                <StatefulTabs componentKey={`SearchPanel_${title}`} onTabSelect={onTabSelect}>
                    {searchesAsTabs(allSearchItems, initArgs)}
                </StatefulTabs>
            </Stack>
        );
    }
}

SearchPanel.propTypes = {
    style:  PropTypes.object,
    initArgs: PropTypes.object
};

function searchesAsTabs(allSearchItems, initArgs) {

    return allSearchItems &&
        Object.values(allSearchItems).map( (searchItem) => {
            const label = searchItem.title || searchItem.name;
            return  (
                <Tab key={label} name={label}>
                    <div className='SearchPanel__form'>
                        <SearchForm searchItem={searchItem} initArgs={initArgs}/>
                    </div>
                </Tab>
            );
        } );
}

const searchOnce= makeSearchOnce(); // setup options to immediately execute the search the first time

export function executeOK(clickFunc,initArgs,searchItem) {
    searchOnce(
        () => {
            if (!initArgs?.urlApi?.execute) return false;
                            // if search option is not passed or if passed, it must match the rendered searchItem
            return !initArgs?.urlApi.searchoption || searchItem.name === initArgs?.urlApi.searchoption;
        },
        () => clickFunc(),
        initArgs?.urlApi?.callId
    );
}


function SearchForm({searchItem, style, initArgs}) {
    const {name, form} = searchItem;
    const {render:Render, useFormPanel:useFormPanel=true,...rest} = form;

    return (
        useFormPanel ? (
            <FormPanel groupKey={name} style={style}
                       getDoOnClickFunc={(clickFunc) => executeOK(clickFunc,initArgs,searchItem)}
                       {...rest}>
                <Render {...{searchItem, initArgs}} />
            </FormPanel>
        ) : (
            <Render {...{searchItem, initArgs}} />
        )
    );
}

function SideBar({activeSearch, groups=[]}) {
    return (
        <Sheet variant='soft' sx={{minWidth:200}}>
            <Stack spacing={1}>
                {
                    groups.map( (group, idx) => <SearchGroup key={group.title || idx} {...{group, activeSearch}}/>)
                }
            </Stack>
        </Sheet>
    );
}

function SearchGroup({group, activeSearch}) {
    return useInRouterContext() ? <SearchGroupRouter {...{group, activeSearch}}/> : <SearchGroupFlux {...{group, activeSearch}}/>;
}

function SearchGroupFlux({group, activeSearch}) {
    const onChange = (e,v) => dispatchUpdateAppData(set({}, ['searches', 'activeSearch'], v));
    return <SearchGroupImpl {...{group, activeSearch, onChange}}/>;
}

function SearchGroupRouter({group, activeSearch}) {
    const navigate = useNavigate();
    const onChange = (e,v) => {
        dispatchUpdateAppData(set({}, ['searches', 'activeSearch'], v));
        const search = group[v];
        navigate(search.path);
    };
    return <SearchGroupImpl {...{group, activeSearch, onChange}}/>;
}

function SearchGroupImpl({group, activeSearch, onChange}) {
    const {title, searchItems} = group;
    return (
        <Stack >
            <Typography level='title-lg'>{title}</Typography>
            <ToggleButtonGroup value={activeSearch} orientation='vertical' onChange={onChange}
                               sx={{
                                   '& button': {justifyContent:'left'},
                                   '--ButtonGroup-connected': '0'
                               }}
            >
                {
                    Object.values(searchItems)
                        .map( (search) => {
                            return <SearchItem  key={search.name} {...{search, activeSearch}}/>;
                        })
                }
            </ToggleButtonGroup>
        </Stack>
    );
}

function SearchItem({search}) {
    const ttips = search.desc || search.title || search.name;
    const label = search.title || search.name;
    return (
        <Tooltip title={ttips}>
            <Button value={search.name} variant='plain'>{label}</Button>
        </Tooltip>
    );
}
