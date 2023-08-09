/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {set} from 'lodash';
import {dispatchUpdateAppData} from '../core/AppDataCntlr.js';
import {getSearchInfo} from '../core/AppDataCntlr.js';
import {FormPanel} from './FormPanel.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import {StatefulTabs, Tab} from './panel/TabPanel.jsx';
import {makeSearchOnce} from '../util/WebUtil';

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
            <div>
                {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
                <SearchForm style={{height: 'auto'}} searchItem={searchItem} initArgs={initArgs}/>
            </div>
        );
    }

    if (flow === 'vertical') {
        const sideBar = <SideBar {...{activeSearch, groups}}/> ;
        return (
            <div>
                {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
                <div className='SearchPanel' style={style}>
                    {sideBar}
                    {searchItem &&
                    <div className='SearchPanel__form'>
                        <SearchForm searchItem={searchItem}  initArgs={initArgs}/>
                    </div>
                    }
                </div>
            </div>
        );
    } else {
        const onTabSelect = (index,id,name) => dispatchUpdateAppData(set({}, ['searches', 'activeSearch'], name));
        return (
            <div>
                {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
                <StatefulTabs componentKey={`SearchPanel_${title}`} onTabSelect={onTabSelect} resizable={true} useFlex={true} borderless={true} contentStyle={{backgroundColor: 'transparent'}}>
                    {searchesAsTabs(allSearchItems, initArgs)}
                </StatefulTabs>
            </div>
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

function executeOK(clickFunc,initArgs,searchItem) {
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
    const {render:Render, ...rest} = form;

    return (
        <FormPanel groupKey={name} style={style}
                   getDoOnClickFunc={(clickFunc) => executeOK(clickFunc,initArgs,searchItem) }
                   {...rest}>
            <Render {...{searchItem, initArgs}} />
        </FormPanel>
    );
}

function SideBar({activeSearch, groups=[]}) {
    return (
        <div className='SearchPanel__sidebar'>
        {
            groups.map( (group, idx) => <SearchGroup key={group.title || idx} {...{group, activeSearch}}/>)
        }
        </div>
    );
}

function SearchGroup({group, activeSearch}) {
    const {title, searchItems} = group;
    return (
        <div className='SearchPanel__group'>{title}
            {
                Object.values(searchItems)
                    .map( (search) => {
                        return <SearchItem  key={search.name} {...{search, activeSearch}}/>;
                    })
            }
        </div>
    );
}

function SearchItem({search, activeSearch}) {
    const ttips = search.desc || search.title || search.name;
    const label = search.title || search.name;
    const clsname = search.name ===  activeSearch ? 'selected' : 'normal';
    const onClick = () => dispatchUpdateAppData(set({}, ['searches', 'activeSearch'], search.name));
    return <div className='SearchPanel__searchItem' onClick={onClick} title={ttips}><span className={clsname}>{label}</span></div>;
}
