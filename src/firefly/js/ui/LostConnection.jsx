/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {get} from 'lodash';
import {createRoot} from 'react-dom/client';
import {dispatchAddActionWatcher} from '../core/MasterSaga';
import {APP_UPDATE} from '../core/AppDataCntlr';

export const warningDivId = 'fireflyLostConnWarn';

/**
 * When connection is lost, a warning indicator will attach itself to a div with an id that starts with 'fireflyLostConnWarn'.
 * This means there can be more than one.  However, if one is not detected, it will do nothing.
 * When constructing the div, a 'data-decor' attribute can be set to affect how the warning look.
 *      <div id="fireflyLostConnWarn" data-decor="small" class="warning-div center"></div>
 *
 * Currently, we support 'data-decor' of 'small', 'medium', and 'full' with 'medium' as the default.
 *  small   : 24x24 transparent icon with tooltips
 *  medium  : 48x48 transparent icon with tooltips
 *  full    : 48x48 transparent icon with the message visible
 *
 * For conveniences, there are 3 defined styles to layout the fireflyLostConnWarn div:
 *   warning-div right:    horizontally right-align with a z-index of 300
 *   warning-div left:     horizontally left-align with a z-index of 300
 *   warning-div center:   horizontally center-align with a z-index of 300
 *
 * See src/firefly/html/demo/ffapi-table-test.html for an example of multiple warning is different styles.
 * There is one on top center and 3 on the bottom.  Search for div with id starting with 'fireflyLostConnWarn'.
 *
 */
export function initLostConnectionWarning() {


    const onConnectionUpdate = (action) => {
        const {payload} = action || {};
        if (! get(payload, 'websocket.isConnected', true)) {
            showLostConnection();
        }
    };

    dispatchAddActionWatcher({ actions:[APP_UPDATE], callback: onConnectionUpdate});

}



const reactRoots= new Map();

export function showLostConnection() {
    const divElements = document.querySelectorAll('[id^="fireflyLostConnWarn"]');
    if (divElements) {
        divElements.forEach( (div) => {
            reactRoots.get(div)?.unmount();
            const decor = div.getAttribute('data-decor') || 'medium';
            const root= reactRoots.get(div) ?? createRoot(div);
            reactRoots.set(div,root);
            root.render(<LostConnection {...{decor}}/>);
        });
    }
}

export function hideLostConnection() {
    const divElements = document.querySelectorAll('[id^="fireflyLostConnWarn"]');
    if (divElements) {
        divElements.forEach( (div) => {
            reactRoots.get(div)?.unmount();
        });
    }
}

function LostConnection ({decor}) {
    const clzName = decor === 'small' ? 'lost-connection--small' : 'lost-connection';
    const msg = 'You are no longer connected to the server. Try reloading the page to reconnect.';
    if (decor !== 'full') return <div className = {clzName} title = {msg} />;

    return (
        <div style={{display: 'inline-flex', alignItems: 'center', color: 'maroon', width: 300}}>
            <div className={clzName} style={{margin: '1px 5px'}}/>
            <div> {msg}</div>
        </div>
    );
}
