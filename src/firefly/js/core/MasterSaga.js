/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {take, fork, cancel} from 'redux-saga/effects';
import {get} from 'lodash';

import {uniqueID} from '../util/WebUtil.js';

export const ADD_SAGA= 'MasterSaga.addSaga';
export const ADD_ACTION_WATCHER= 'MasterSaga.addActionWatcher';
export const CANCEL_ACTION_WATCHER= 'MasterSaga.cancelActionWatcher';
export const CANCEL_ALL_ACTION_WATCHER= 'MasterSaga.cancelAllActionWatcher';


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
 * @param {string}   [p.id]     a unique identifier for this watcher.  This is needed for dispatchCancel*.
 *                              When not given, a unique ID will be created.  You can still cancel this watcher via
 *                              callback's cancelSelf function.
 * @param {string[]} p.actions  an array of action types to watch
 * @param {function} p.callback a callback function to handle the action(s).  It is called with (action, cancelSelf, params, dispatch, getState).
 *                                action: the triggered action
*                                 cancelSelf: a function to cancel this watcher
 *                                params: the given params object
 *                                dispatch: flux's dispatcher
 * @param {function} p.params   a pass-along parameter object to be sent when callback is called.
 */
export function dispatchAddActionWatcher({id, actions, callback, params={}}) {
    flux.process({ type: ADD_ACTION_WATCHER, payload: {id, actions, callback, params}});
}

/**
 * cancel the watcher with the given id.
 * @param {string} id  a unique identifier of the watcher to cancel
 */
export function dispatchCancelActionWatcher(id) {
    flux.process({ type: CANCEL_ACTION_WATCHER, payload: {id}});
}

/**
 * Cancel all watchers.  Should only be called during re-init scenarios. 
 */
export function dispatchCancelAllActionWatchers() {
    flux.process({ type: CANCEL_ALL_ACTION_WATCHER});
}


/**
 * This saga launches all the predefined Sagas then loops and waits for any ADD_SAGA actions and launches those Segas
 */
export function* masterSaga() {
    let watchers = {};

    // Start a saga from any action
    while (true) {
        const action= yield take([ADD_SAGA, ADD_ACTION_WATCHER, CANCEL_ACTION_WATCHER, CANCEL_ALL_ACTION_WATCHER]);

        switch (action.type) {
            case ADD_SAGA: {
                const {getState, dispatch}= flux.getRedux();
                const {saga,params}= action.payload;
                if (typeof saga === 'function') yield fork( saga, params, dispatch, getState);
                break;
            }
            case ADD_ACTION_WATCHER: {
                const {getState, dispatch}= flux.getRedux();
                const {id=uniqueID(), actions, callback, params}= action.payload;
                if (actions && callback) {
                    if (watchers[id]) {
                        yield cancel(watchers[id]);
                    }
                    const saga = createSaga({id, actions, callback, params, dispatch, getState});
                    const task = yield fork(saga, dispatch, getState);
                    watchers[id] = task;
                    isDebug() && console.log(`watcher ${id} added.  #watcher: ${Object.keys(watchers).length}`);
                }
                break;
            }
            case CANCEL_ACTION_WATCHER: {
                const {id}= action.payload;
                const task = watchers[id];
                if (task) {
                    yield cancel(task);
                    Reflect.deleteProperty(watchers, id);
                    isDebug() && console.log(`watcher ${id} cancelled.  #watcher: ${Object.keys(watchers).length}`);
                }
                break;
            }
            case CANCEL_ALL_ACTION_WATCHER: {
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


function createSaga({id, actions=[], callback, params, dispatch, getState}) {
    const cancelSelf = ()=> dispatch({ type: CANCEL_ACTION_WATCHER, payload: {id}});
    const saga = function* () {
        while (true) {
            const action= yield take(actions);
            try {
                callback(action, cancelSelf, params, dispatch, getState);
            } catch (e) {
                console.log(`Encounter error while executing watcher: ${id}  error: ${e}`);
            }
        }
    };
    return saga;
}

const isDebug = () => get(window, 'firefly.debug', false);
