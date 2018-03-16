/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {SimpleComponent} from './SimpleComponent.jsx';
import {getWsInfo} from '../core/AppDataCntlr.js';
import noInternet from 'html/images/no_internet.png';


var divElement;

export function initLostConnectionWarning() {
    if (!divElement) {
        divElement= document.createElement('div');
        document.body.appendChild(divElement);
        divElement.id= 'Lost_Connection';
        divElement.style.position= 'absolute';
        divElement.style.left= '0';
        divElement.style.top= '0';

        ReactDOM.render(<LostConnection/>, divElement);
    }
}


class LostConnection extends SimpleComponent {

    getNextState() {
        return getWsInfo();
    }

    render() {
        const {isConnected=true} = this.state || {};

        if (isConnected) return null;
        return (
            <div className='lost-connection'>
                <img src={noInternet}/>
                <div>You are no longer connected to the server</div>
                <div>This message will go away once your connection is restored</div>
            </div>
        );
    }
}
