/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {get} from 'lodash';
import {SimpleComponent} from './SimpleComponent.jsx';
import {getWsInfo} from '../core/AppDataCntlr.js';
import {dispatchAddActionWatcher} from '../core/MasterSaga';
import {APP_UPDATE} from '../core/AppDataCntlr';

export const warningDivId = 'fireflyWarn';

/**
 * The lost-connection will attach itself to a div with an id that starts with 'fireflyWarn'.
 * This means there can be more than one.  However, if one is not detected, it will do nothing.
 * When constructing the div, you may also provide a 'data-decor' attribute.  This will affect how the warning looks.
 *      <div id="fireflyWarn" data-decor="small" class="warning-div center"></div>
 *
 * Currently, we support 'data-decor' or 'small', 'medium', and 'full' with 'medium' as the default.
 *  small   : 24x24 transparent icon with tooltips
 *  medium  : 48x48 transparent icon with tooltips
 *  full    : 48x48 transparent icon with the message visible
 *
 * For conveniences, there are 3 defined styles one can apply to a fireflyWarn div:
 *   warning-div right:    horizontally right-align with a z-index of 300
 *   warning-div left:     horizontally left-align with a z-index of 300
 *   warning-div center:   horizontally center-align with a z-index of 300
 *
 *
 * See src/firefly/html/demo/ffapi-table-test.html for an example of multiple warning is different styles.
 * There is one on top center and 3 on the bottom.  Search for div with id starting with 'fireflyWarn'.
 *
 */
export function initLostConnectionWarning() {


    const onConnectionUpdate = (action) => {
        const {payload} = action || {};
        if (! get(payload, 'websocket.isConnected', true)) {
            const divElements = document.querySelectorAll('[id^="fireflyWarn"]');
            if (divElements) {
                divElements.forEach( (div) => {
                    ReactDOM.unmountComponentAtNode(div);        // in case one is already mounted.
                    const decor = div.getAttribute('data-decor') || 'medium';
                    ReactDOM.render(<LostConnection {...{decor}}/>, div);
                });
            }
        }
    };

    dispatchAddActionWatcher({ actions:[APP_UPDATE], callback: onConnectionUpdate});

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
