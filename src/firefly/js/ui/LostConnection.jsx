/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {SimpleComponent} from './SimpleComponent.jsx';
import {getWsInfo} from '../core/AppDataCntlr.js';

export const warningDivId = 'fireflyWarn';
var divElement;


/**
 * The lost-connection will attach itself to a div with an id of 'fireflyWarn' if one exists.  Otherwise, it will do nothing.
 * When constructing the div, you may also provide a 'data-decor' attribute.  This will affect how the warning looks.
 *      <div id="fireflyWarn" data-decor="small" class="warning-div"></div>
 *
 * Currently, we support 'data-decor' or 'small', 'medium', and 'full' with 'medium' as the default.
 *  small   : 24x24 transparent icon with tooltips
 *  medium  : 48x48 transparent icon with tooltips
 *  full    : 48x48 transparent icon with the message visible
 *
 * For conveniences, there are 2 defined styles one can apply to a fireflyWarn div:
 *   warning-div:           horizontally right-align with a z-index of 300
 *   warning-div--center:   horizontally center-align with a z-index of 300
 */
export function initLostConnectionWarning() {
    divElement = divElement || document.getElementById(warningDivId);
    if (divElement) {
        const decor = divElement.getAttribute('data-decor') || 'medium';
        ReactDOM.render(<LostConnection {...{decor}}/>, divElement);
    }
}


class LostConnection extends SimpleComponent {

    getNextState() {
        return getWsInfo();
    }

    render() {
        const {isConnected=true} = this.state || {};
        if (isConnected) return null;

        const {decor} = this.props;
        if (decor === 'full') {
            return (
                <div style={{display: 'inline-flex', alignItems: 'center', color: 'maroon'}}>
                    <div className='lost-connection' style={{margin: '1px 5px'}}/>
                    <div>
                        You are no longer connected to the visualization server. <br/>
                        This message will go away once your connection is restored.
                    </div>
                </div>
            );
        } else {
            const clzName = decor === 'small' ? 'lost-connection--small' : 'lost-connection';
            return (
                <div className = {clzName}
                     title = 'You are no longer connected to the visualization server. This icon will go away once your connection is restored.'/>
            );
        }

    }
}
