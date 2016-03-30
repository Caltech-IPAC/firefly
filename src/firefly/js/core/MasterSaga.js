/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {fork, take} from 'redux-saga/effects';
import {watchCatalogs} from '../visualize/saga/CatalogWatcher.js';

export const ADD_SAGA= 'MasterSaga.addSaga';


/**
 * 
 * @param saga a generator function that uses redux-saga
 */
export function dispatchAddSaga(saga) {
    flux.process({ type: ADD_SAGA, payload: { saga }});
}


/**
 * This saga launches all the predefined Sagas then loops and waits for any ADD_SAGA actions and launches those Segas
 */
export function* masterSaga() {
    var saga;
    
    // This section starts any predefined Sagas
    yield fork( watchCatalogs);
    
    // Start a saga from any action
    while (true) {
        var action= yield take(ADD_SAGA);
        saga= action.payload.saga;
        if (typeof saga === 'function') yield fork( saga);
    }
}