/**
 * Created by loi on 1/19/16.
 */

import {isEmpty} from 'lodash';
import {reduxFlux} from '../ReduxFlux.js';

export var GwtEventHandler = {
    matches : ({name, data}) => {
        return name !== 'FLUX_ACTION' && !isEmpty(data);
    },

    onEvent: ({name, data}) => {
        // call gwt if exists
        if (window.ffgwt) {
            window.ffgwt.Core.ClientEventQueue.onMessage(data);
        }
    }
};

export var ActionEventHandler = {
    matches : ({name, data}) => {
        return name === 'FLUX_ACTION';
    },

    onEvent: ({name, data}) => {
        reduxFlux.process(data);
    }
};