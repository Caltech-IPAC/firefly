/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import './Menu.css';



function handleAction (menuItem) {
    window.alert('MenuItem clicked:' + menuItem.action);
}

function makeMenuItem(menuItem) {
    return (
        <td key={menuItem.action} align='left' style={{verticalAlign: 'bottom'}} onClick={handleAction.bind(this, menuItem)}>
            <div tabIndex='0' className='menu__item' role='tab'>
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
        menu   : React.PropTypes.arrayOf(React.PropTypes.object).isRequired
    },

    render() {

        var items = [];
        this.props.menu.forEach( (item) => {
            items.push(makeMenuItem(item));
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
