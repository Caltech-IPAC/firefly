/**
 * Created by loi on 1/19/16.
 */

import {flux} from '../ReduxFlux.js';

export var ActionEventHandler = {
    matches : ({name, data}) => {
        return name === 'FLUX_ACTION';
    },

    onEvent: ({name, data}) => {
        flux.process(data);
    }
};