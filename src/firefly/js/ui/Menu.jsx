/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';
import {COMMAND, getMenu} from '../core/AppDataCntlr.js';
import {flux} from '../Firefly.js';
import {dispatchShowDropDown} from '../core/LayoutCntlr.js';
import {BgMonitorButton} from '../core/background/BgMonitorButton.jsx';
import {makeBadge} from './ToolbarButton.jsx';
// import {deepDiff} from '../util/WebUtil.js';
import './Menu.css';

import LOADING from 'html/images/gxt/loading.gif';

function handleAction (menuItem) {

    // set whether search menu should be shown
    if (menuItem.type === COMMAND) {
        flux.process({type: menuItem.action,
                      payload: (menuItem.payload ? menuItem.payload : {})});
    } else {
        dispatchShowDropDown( {view: menuItem.action});
    }
}

/**
 * Create the html for a menu item
 * @param menuItem
 * @param isSelected
 * @returns {XML}
 */
export function  makeMenuItem(menuItem, isSelected, isWorking, badgeCount) {
    var clsname = 'menu__item' + (isSelected ? ' menu__item-selected' : '');
    return (
        <div className={clsname}
             key={menuItem.action}
             title={menuItem.desc}
             onClick={handleAction.bind(this, menuItem)}>

            {isWorking && <img style={{height: 13, marginRight: 3}} src={LOADING}/>}
            {menuItem.label}
            {!!badgeCount && <div className='menu__item--badge'>{makeBadge(badgeCount)}</div> }

        </div>
    );
}

export class Menu extends PureComponent {

    constructor(props) {
        super(props);
    }


    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

    render() {
        const {menu} = this.props;
        const {menuItems=[], showBgMonitor=true} = menu || {};
        if (menuItems.length === 0) return <div/>;

        var items = menuItems.map( (item) => {
            return makeMenuItem(item, item.action === menu.selected);
        });

        return (
            <div className='menu__main'>
                <div className='menu__item--holder'> {items} </div>
                <div className='menu__item--holder'>
                    {showBgMonitor && <BgMonitorButton/>}
                </div>
            </div>
        );
    }
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
