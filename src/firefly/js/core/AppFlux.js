/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import { Flux } from 'flummox';
import { ExternalAccessActions } from '../actions/ExternalAccessActions.js';
import { ExternalAccessStore} from '../store/ExternalAccessStore.js';

export class AppFlux extends Flux {
    constructor() {
        super();
        this.createActions('ExternalAccessActions', ExternalAccessActions );
        this.createStore('ExternalAccessStore', ExternalAccessStore, this);
    }
}
