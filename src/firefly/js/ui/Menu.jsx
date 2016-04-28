/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, get} from 'lodash';
import {COMMAND, getMenu} from '../core/AppDataCntlr.js';
import {flux} from '../Firefly.js';
import {dispatchShowDropDown} from '../core/LayoutCntlr.js';
// import {deepDiff} from '../util/WebUtil.js';
import './Menu.css';


function handleAction (menuItem) {

    // set whether search menu should be shown
    if (menuItem.type === COMMAND) {
        flux.process({type: menuItem.action, payload:{}});
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
function makeMenuItem(menuItem, isSelected) {
    var clsname = 'menu__item' + (isSelected ? ' menu__item-selected' : '');
    return (
        <td key={menuItem.action} align='left' style={{verticalAlign: 'bottom'}} onClick={handleAction.bind(this, menuItem)}>
            <div tabIndex='0' className={clsname} role='tab'>
                <input type='text' tabIndex='-1' role='presentation' style={{opacity: '0', height: '1px', width: '1px', zIndex: '-1', overflow: 'hidden', position: 'absolute'}}></input>
                <div style={{minWidth: '75px'}}>
                    <table cellSpacing='0' cellPadding='0' style={{margin: '0px auto'}}>
                        <tbody>
                        <tr>
                            <td align='left' style={{marginRight: '3px', display: 'none'}} />
                            <td align='left' style={{verticalAlign: 'top'}}>
                                <div className='menu__item-label' title={menuItem.desc}
                                     style={{whiteSpace: 'nowrap', padding: '6px 0px'}}>{menuItem.label}
                                </div>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </td>
    );
}



export class Menu extends Component {

    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

    render() {
        var {menu} = this.props;
        if (get(menu, 'menuItems',[]).length === 0) return <div/>;

        var items = [];
        menu.menuItems.forEach( (item) => {
            items.push(makeMenuItem(item, item.action === menu.selected));
        });

        return (
            <div id='leftBar' style={{display: 'inline-block'}}>
                <table cellSpacing='0' cellPadding='0' style={{width: '1px'}}>
                    <tbody>
                    <tr>
                        <td style={{width: '5px'}} />
                        {items}
                    </tr>
                    </tbody>
                </table>
            </div>
        );
    }
}

Menu.propTypes = {
    menu   : React.PropTypes.object.isRequired
};

/**
 * returns an array of drop down actions from the given menu items
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