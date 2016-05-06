/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {firefly} from 'firefly/Firefly.js';
import {GwtEventHandler} from './core/messaging/MessageHandlers.js';

firefly.bootstrap();
if (window.firefly.wsClient) {
    window.firefly.wsClient.addListener(GwtEventHandler);
}
