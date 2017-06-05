/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {set} from 'lodash';
import {dispatchUpdateAppData} from '../core/AppDataCntlr.js';
import {getSearchInfo} from '../core/AppDataCntlr.js';
import {FormPanel} from './FormPanel.jsx';
import {SimpleComponent} from './SimpleComponent.jsx';

export class SearchPanel extends SimpleComponent {

    getNextState() {
        return getSearchInfo();
    }

    render() {
        const {style={}} = this.props;
        const {activeSearch, groups} = this.state;
        if (!groups) return null;
        const {allSearchItems} = getSearchInfo();

        const sideBar = Object.keys(allSearchItems).length > 1 ? <SideBar {...{activeSearch, groups}}/> : null;
        const searchItem = allSearchItems[activeSearch];

        return (
            <div className='SearchPanel' style={style}>
                {sideBar}
                {searchItem &&
                    <div className='SearchPanel__form'>
                        <SearchForm searchItem={searchItem} />
                    </div>
                }
            </div>
        );
    }
}

SearchPanel.propTypes = {
    style:      PropTypes.object
};


function SearchForm({searchItem}) {
    const {name, form, } = searchItem;
    const {render, ...rest} = form;
    return (
        <FormPanel groupKey={name} {...rest}>
            {render(searchItem)}
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
    const clsname = search.name ===  activeSearch ? 'SearchPanel__searchItem selected' : 'SearchPanel__searchItem';
    const onClick = () => dispatchUpdateAppData(set({}, ['searches', 'activeSearch'], search.name));
    return <div className={clsname} onClick={onClick} title={ttips}>{label}</div>;
}
