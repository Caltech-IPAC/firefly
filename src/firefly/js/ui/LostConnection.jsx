/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {SimpleComponent} from './SimpleComponent.jsx';
import {getWsInfo} from '../core/AppDataCntlr.js';

export const warningDivId = 'fireflyWarn';
var divElement;

export function initLostConnectionWarning() {
    divElement = divElement || document.getElementById(warningDivId);
    if (divElement) {
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
            <div className ='lost-connection'
                 title = 'You are no longer connected to the server. This icon will go away once your connection is restored.'/>
        );
    }
}
