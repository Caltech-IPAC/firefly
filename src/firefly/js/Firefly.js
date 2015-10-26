/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {reduxFlux} from './core/ReduxFlux.js';

export const flux = reduxFlux;

export var firefly = {

    bootstrap() {
        return flux.bootstrap();
    },

    process(rawAction, condition) {
        return flux.process(rawAction, condition);
    },

    addListener(listener, ...types) {

    }

};

