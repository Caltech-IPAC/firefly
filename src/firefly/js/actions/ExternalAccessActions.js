/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";

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
