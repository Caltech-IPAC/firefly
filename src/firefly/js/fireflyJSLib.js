/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {firefly} from './Firefly.js';
import {GwtEventHandler} from './core/messaging/MessageHandlers.js';
import {dispatchUpdateAppData} from './core/AppDataCntlr.js';

if (! window.ffgwt ) {
    window.ffgwt = {
        onLoaded: () => dispatchUpdateAppData({gwtLoaded: true})
    };
}

firefly.bootstrap();
if (window.firefly.wsClient) {
    window.firefly.wsClient.addListener(GwtEventHandler);
}
