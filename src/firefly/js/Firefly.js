/**
 * Created by loi on 10/2/15.
 */
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import flux from 'firefly/core/ReduxFlux.js';
import tableApi from 'firefly/util/api.js';


var firefly = {
    registerAction(type, actionCreator) {
        flux.registerAction(type, actionCreator);
    },

    registerReducer(dataRoot, reducer) {
        flux.registerReducer(dataRoot, reducer);
    },

    bootstrap() {
        return flux.bootstrap();
    },
    showTable() {
        tableApi.showTabl();
    }
}

export default firefly;