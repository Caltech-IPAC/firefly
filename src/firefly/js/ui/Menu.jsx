/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import './Menu.css';
import {flux} from '../Firefly.js';
import appDataCntlr from '../core/AppDataCntlr.js';
import {dispatchSetDropDownUi} from '../core/LayoutCntlr.js';



function handleAction (menuItem) {

    // set whether search menu should be shown
    if (menuItem.type === appDataCntlr.DROP_DOWN_TYPE) {
        dispatchSetDropDownUi( {view: menuItem.action});
    } else {
        flux.process({type: menuItem.action, payload:{}});
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

const Menu = React.createClass({

    propTypes: {
        menu   : React.PropTypes.object.isRequired
    },

    render() {
        var {menu} = this.props;

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
});

export default Menu;
