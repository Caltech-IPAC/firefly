/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {set, get} from 'lodash';
import {dispatchUpdateAppData} from '../core/AppDataCntlr.js';
import {getSearchInfo} from '../core/AppDataCntlr.js';
import {FormPanel} from './FormPanel.jsx';
import {SimpleComponent} from './SimpleComponent.jsx';
import {StatefulTabs, Tab} from './panel/TabPanel.jsx';
import {makeSearchOnce} from '../util/WebUtil';

export class SearchPanel extends SimpleComponent {

    getNextState() {
        return getSearchInfo();
    }

    render() {
        const {style={}, initArgs={}} = this.props;
        const {activeSearch, groups, allSearchItems={}, flow='vertical', title} = this.state;
        const searchItem = allSearchItems[activeSearch];

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


function SearchForm({searchItem, style, initArgs}) {
    const {name, form} = searchItem;
    const {render:Render, ...rest} = form;

    const saveClick= (clickFunc) => initArgs?.urlApi?.execute && searchOnce(true, clickFunc);


    return (
        <FormPanel groupKey={name} style={style}
                   getDoOnClickFunc={saveClick}
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
