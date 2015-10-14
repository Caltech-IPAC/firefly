/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import { Actions } from 'flummox';


export class ExternalAccessActions extends Actions {
    extensionAdd(extension) {
        return extension;
    }

    extensionActivate(extension, resultData) {
        return {extension, resultData};
    }

    channelActivate(channelID) {
        return channelID;
    }
}
