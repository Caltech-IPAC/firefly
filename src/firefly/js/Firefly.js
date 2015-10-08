/**
 * Created by loi on 10/2/15.
 */
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Flux from './core/ReduxFlux.js';

export var flux = Flux;

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

};

export default firefly;
