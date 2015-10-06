/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import alt from '../core/AppAlt.js';

class DialogActions {

    constructor() {
        this.generateActions(
            'showDialog', 'hideDialog'
        );
    }
}

export default alt.createActions(DialogActions);
