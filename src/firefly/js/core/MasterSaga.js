/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {take, fork, cancel} from 'redux-saga/effects';
import {get} from 'lodash';

export const ADD_SAGA= 'MasterSaga.addSaga';
export const ADD_WATCHER= 'MasterSaga.addWatcher';
export const CANCEL_WATCHER= 'MasterSaga.cancelWatcher';
export const CANCEL_ALL_WATCHER= 'MasterSaga.cancelAllWatcher';


/**
 * 
 * @param {generator} saga a generator function that uses redux-saga
 * @param {{}} params this object is passed the to sega as the first parrmeter
 */
export function dispatchAddSaga(saga, params={}) {
    flux.process({ type: ADD_SAGA, payload: { saga,params}});
}

/**
 * @param {object}   p
 * @param {string}   p.id       a unique identifier for this watcher
 * @param {string[]} p.actions  an array of action types to watch
 * @param {function} p.callback a callback function to handle the action(s).  The triggered action will be passed back as a parameter.
 */
export function dispatchAddWatcher({id, actions, callback}) {
    flux.process({ type: ADD_WATCHER, payload: {id, actions, callback}});
}

/**
 * cancel the watcher with the given id.
 * @param {string} id  a unique identifier of the watcher to cancel
 */
export function dispatchCancelWatcher(id) {
    flux.process({ type: CANCEL_WATCHER, payload: {id}});
}

/**
 * Cancel all watchers.  Should only be called during re-init scenarios. 
 */
export function dispatchCancelAllWatchers() {
    flux.process({ type: CANCEL_ALL_WATCHER});
}


/**
 * This saga launches all the predefined Sagas then loops and waits for any ADD_SAGA actions and launches those Segas
 */
export function* masterSaga() {
    let watchers = {};

    // Start a saga from any action
    while (true) {
        const action= yield take([ADD_SAGA, ADD_WATCHER, CANCEL_WATCHER, CANCEL_ALL_WATCHER]);

        switch (action.type) {
            case ADD_SAGA: {
                const {getState, dispatch}= flux.getRedux();
                const {saga,params}= action.payload;
                if (typeof saga === 'function') yield fork( saga, params, dispatch, getState);
                break;
            }
            case ADD_WATCHER: {
                const {getState, dispatch}= flux.getRedux();
                const {id, actions, callback}= action.payload;
                if (id && actions && callback) {
                    if (watchers[id]) {
                        yield cancel(watchers[id]);
                    }
                    const saga = createSaga({id, actions, callback});
                    const task = yield fork(saga, dispatch, getState);
                    watchers[id] = task;
                    isDebug() && console.log(`watcher ${id} added.  #watcher: ${Object.keys(watchers).length}`);
                }
                break;
            }
            case CANCEL_WATCHER: {
                const {id}= action.payload;
                const task = watchers[id];
                if (task) {
                    yield cancel(task);
                    Reflect.deleteProperty(watchers, id);
                    isDebug() && console.log(`watcher ${id} cancelled.  #watcher: ${Object.keys(watchers).length}`);
                }
                break;
            }
            case CANCEL_ALL_WATCHER: {
                const ids = Object.keys(watchers);
                for (let i = 0; i < ids.length; i++) {
                    const task = watchers[ids[i]];
                    yield cancel(task);
                }
                watchers = {};
                break;
            }
        }
    }
}


function createSaga({id, actions=[], callback}) {
    const saga = function* () {
        while (true) {
            const action= yield take(actions);
            try {
                callback && callback(action);
            } catch (e) {
                console.log(`Encounter error while executing watcher: ${id}  error: ${e}`);
            }
        }
    };
    return saga;
}

const isDebug = () => get(window, 'firefly.debug', true);
