/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {useNavigate} from 'react-router-dom';

import {COMMAND, getMenu} from '../core/AppDataCntlr.js';
import {flux} from '../core/ReduxFlux.js';
import {dispatchSetLayoutInfo, dispatchShowDropDown} from '../core/LayoutCntlr.js';
import {BgMonitorButton} from '../core/background/BgMonitorButton.jsx';
import {makeBadge} from './ToolbarButton.jsx';
import {ROUTER} from '../templates/router/RouterEntry.jsx';

import './Menu.css';

import LOADING from 'html/images/gxt/loading.gif';

function handleAction (menuItem, navigate) {

    // set whether search menu should be shown
    if (menuItem.type === COMMAND) {
        flux.process({
            type: menuItem.action,
            payload: (menuItem.payload ? menuItem.payload : {})
        });
    }else if (menuItem.type === ROUTER) {
        const {path, params, options} = menuItem.action;
        const to = path + (params ? '?' + new URLSearchParams(params).toString() : '');
        navigate?.(to, options);
        dispatchSetLayoutInfo({dropDown:{visible: true}});
    } else {
        dispatchShowDropDown( {view: menuItem.action});
    }
}

/**
 * Create the html for a menu item
 * @param p
 * @param p.menuItem
 * @param p.isSelected
 * @param p.isWorking
 * @param p.badgeCount
 * @param p.idx
 * @returns {JSX.Element}
 */
export function MenuItem({menuItem, isSelected, isWorking=false, badgeCount=0}) {
    const clsname = 'menu__item' + (isSelected ? ' menu__item-selected' : '');
    const navigate = useNavigate();
    return (
        <div className={clsname}
             title={menuItem.desc}
             onClick={handleAction.bind(this, menuItem, navigate)}>

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
 * returns an array of drop down actions from menu items
 * @returns {*}
 */
export function getDropDownNames() {
    const menuItems = get(getMenu(), 'menuItems');
    if (!Array.isArray(menuItems)) return [];
    return menuItems.filter((el) => el.type !== COMMAND).reduce(
            (rval, mi) => {
                rval.push(mi.action);
                return rval;
            }, []);
}
