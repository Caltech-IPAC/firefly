/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {fork, take} from 'redux-saga/effects';
import {watchCatalogs} from '../visualize/saga/CatalogWatcher.js';
import {syncCharts} from '../visualize/saga/ChartsSync.js';
import {imagePlotter} from '../visualize/saga/ImagePlotter.js';
import {watchReadout} from '../visualize/saga/MouseReadoutWatch.js';
import {watchForRelatedActions} from '../fieldGroup/FieldGroupCntlr.js';

export const ADD_SAGA= 'MasterSaga.addSaga';


/**
 * 
 * @param {generator} saga a generator function that uses redux-saga
 * @param {{}} params this object is passed the to sega as the first parrmeter
 */
export function dispatchAddSaga(saga, params) {
    flux.process({ type: ADD_SAGA, payload: { saga,params}});
}


/**
 * This saga launches all the predefined Sagas then loops and waits for any ADD_SAGA actions and launches those Segas
 */
export function* masterSaga() {

    yield take();
    const {getState, dispatch}= flux.getRedux();
    // This section starts any predefined Sagas
    yield fork( watchCatalogs, null, dispatch, getState);
    yield fork( imagePlotter, null, dispatch, getState);
    yield fork( syncCharts, null, dispatch, getState);
    yield fork( watchReadout, null, dispatch, getState);
    yield fork( watchForRelatedActions, null, dispatch, getState);

    // Start a saga from any action
    while (true) {
        var action= yield take(ADD_SAGA);
        const {saga,params}= action.payload;
        if (typeof saga === 'function') yield fork( saga, params, dispatch, getState);
    }
}