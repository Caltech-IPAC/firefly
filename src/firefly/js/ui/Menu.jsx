/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback} from 'react';
import PropTypes from 'prop-types';

import {COMMAND, getMenu} from '../core/AppDataCntlr.js';
import {flux} from '../core/ReduxFlux.js';
import {dispatchShowDropDown} from '../core/LayoutCntlr.js';
import {BgMonitorButton} from '../core/background/BgMonitorButton.jsx';
import {makeBadge} from './ToolbarButton.jsx';

import './Menu.css';

import LOADING from 'html/images/gxt/loading.gif';

function handleAction (menuItem) {

    // set whether search menu should be shown
    if (menuItem.type === COMMAND) {
        flux.process({
            type: menuItem.action,
            payload: (menuItem.payload ?? {})
        });
    } else {
        dispatchShowDropDown( {view: menuItem.action});
    }
}

/**
 * Create the html for a menu item
 * @param p
 * @param p.menuItem
 * @param p.isSelected
 * @param p.clickHandler     onClick handler.  Defaults to #handleAction
 * @param p.isWorking
 * @param p.badgeCount
 * @param p.idx
 * @returns {JSX.Element}
 */
export function MenuItem({menuItem, isSelected, clickHandler, isWorking=false, badgeCount=0}) {
    const clsname = 'menu__item' + (isSelected ? ' menu__item-selected' : '');
    const onClick = () => {
        clickHandler ??= handleAction;
        clickHandler(menuItem);
    };
    return (
        <div className={clsname}
             title={menuItem.desc}
             onClick={onClick}>

            {isWorking && <img style={{height: 13, marginRight: 3}} src={LOADING}/>}
            {menuItem.label}
            {!!badgeCount && <div className='menu__item--badge'>{makeBadge(badgeCount)}</div> }

        </div>
    );
}

export function Menu({menu={}}) {
    const {menuItems=[], showBgMonitor=true} = menu;
    if (!menuItems.length) return <div/>;

    const items = menuItems.map((item, idx) => <MenuItem key={idx} menuItem={item} isSelected={item.action === menu.selected}/>);

    return (
        <div className='menu__main'>
            <div className='menu__item--holder'> {items} </div>
            <div className='menu__item--holder'>
                {showBgMonitor && <BgMonitorButton/>}
            </div>
        </div>
    );
}

Menu.propTypes = {
    menu   : PropTypes.object.isRequired
};

/**
 * returns an array menuItems
 * @returns {*}
 */
export function getMenuItems() {
    return getMenu()?.menuItems;
}

/**
 * returns an array of drop down actions from menu items
 * @returns {*}
 */
export function getDropDownNames() {
    const menuItems = getMenuItems();
    if (!Array.isArray(menuItems)) return [];
    return menuItems.filter((mi) => mi.type !== COMMAND)
                    .map((mi) => mi.action);
}
