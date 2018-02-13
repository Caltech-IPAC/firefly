/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {take, fork, spawn, cancel} from 'redux-saga/effects';
import {get, isFunction, isUndefined} from 'lodash';

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
 * Action watcher callback.
 * @callback actionWatcherCallback
 * @param {Action} action - the triggering action
 * @param {function} cancelSelf - a function to cancel this watcher
 * @param {object} params - params passed through dispatchAddActionWatcher or (if not undefined) the last returned value from the watcher callback
 * @param {function} dispatch: flux's dispatcher
 * @param {function} getState: flux's getState function
 */

/**
 * @param {object}   p
 * @param {string}   [p.id]     a unique identifier for this watcher.  This is needed for dispatchCancel*.
 *                              When not given, a unique ID will be created.  You can still cancel this watcher via
 *                              callback's cancelSelf function.
 * @param {string[]} p.actions  an array of action types to watch
 * @param {actionWatcherCallback} p.callback a callback function to handle the action(s).
 *                                It is called with
 *                                (action:Action, cancelSelf:Function, params:Object, dispatch:Function, getState:Function).
 *                                {Action} action: the triggered action
 *                                {Function} cancelSelf: a function to cancel this watcher
 *                                {Object} params: the given params object, it the watcher callback returns a value
 *                                        then on the next call the last returned value
 *                                {Function} dispatch: flux's dispatcher
 *                                {Function} getState: flux's getState function
 * @param {Object} p.params   a pass-along parameter object to be sent when callback is called.
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
                // with fork every exception will bubble up from the child to the parent:
                // an unhandled exception in one saga will cancel all sibling sagas
                // with spawn, only the saga with the unhandled error will be cancelled
                // the unhandled errors are caught by middleware and logged to console
                if (isFunction(saga)) {
                    yield spawn(saga, params, dispatch, getState);
                } else {
                    console.error('Can not add saga: callback must be a generator function');
                }
                break;
            }
            case ADD_ACTION_WATCHER: {
                const {getState, dispatch}= flux.getRedux();
                const {actions, callback, params}= action.payload;
                if (actions && isFunction(callback)) {
                    const {id=callback.name+uniqueID()}= action.payload;
                    if (watchers[id]) {
                        yield cancel(watchers[id]);
                    }
                    const watcherSaga = createWatcherSaga({id, actions, callback, params, dispatch, getState});
                    const task = yield fork(watcherSaga, dispatch, getState);
                    watchers[id] = task;
                    isDebug() && console.log(`watcher ${id} added.  #watcher: ${Object.keys(watchers).length}`);
                } else {
                    console.error('Can not create action watcher: invalid actions or callback');
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


function createWatcherSaga({id, actions=[], callback, params, dispatch, getState}) {
    const cancelSelf = ()=> dispatch({ type: CANCEL_ACTION_WATCHER, payload: {id}});
    const saga = function* () {

        let prevParams= params;
        let returnedParams;

        // loop exits when saga is cancelled
        while (true) {
            const action = yield take(actions);
            try {
                // the same callback can return modified parameters or undefined
                // when undefined is returned use previous parameters
                returnedParams = callback(action, cancelSelf, prevParams, dispatch, getState);
                if (!isUndefined(returnedParams)) {
                    prevParams = returnedParams;
                }
            } catch (e) {
                console.log(`Encounter error while executing watcher: ${id}  error: ${e}`);
                console.log(e);
            }
        }
    };
    return saga;
}

const isDebug = () => get(window, 'firefly.debug', false);
