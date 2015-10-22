/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React from 'react';
import thunkMiddleware from 'redux-thunk';
import loggerMiddleware from 'redux-logger';
import { createStore, applyMiddleware, combineReducers } from 'redux';
import { connect, Provider } from 'react-redux';
import { actionSideEffectMiddleware } from 'firefly/side-effects';
import AppDataCntlr  from './AppDataCntlr.js';
import FieldGroupCntlr from '../fieldGroup/FieldGroupCntlr.js';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';

/**
 * A map to rawAction.type to an ActionCreator
 * @type {Map<string, function>}
 */
const actionCreators = new Map();

/**
 * A collection of reducers keyed by the node's name under the root.
 * @type {Object<string, function>}
 */
const reducers = {
    [AppDataCntlr.APP_DATA_KEY]: AppDataCntlr.reducer,
    [FieldGroupCntlr.FIELD_GROUP_KEY]: FieldGroupCntlr.reducer,
    [ImagePlotCntlr.IMAGE_PLOT_KEY]: ImagePlotCntlr.reducer
};

let redux;


// pre-map a set of action => creator prior to boostrapping.
actionCreators.set(AppDataCntlr.APP_LOAD, AppDataCntlr.loadAppData);
actionCreators.set(FieldGroupCntlr.VALUE_CHANGE, FieldGroupCntlr.valueChangeActionCreator);


function createRedux() {
    // create a rootReducer from all of the registered reducers
    const rootReducer = combineReducers(reducers);

    // redux is a store and more.. it manages reducers as well as thunk actions
    // we'll call it redux for now.
    return applyMiddleware(actionSideEffectMiddleware, thunkMiddleware, loggerMiddleware)(createStore)(rootReducer);
}

function bootstrap() {
    redux = createRedux();
    return new Promise(
        function (resolve, reject) {
            // there may be async logic here..
            // if not, simply invoke resolve.
            resolve('success');
        });
}

/**
 * Process the rawAction.  This uses the actionCreators map to resolve
 * the ActionCreator given the action.type.  If one is not mapped, then it'll
 * create a simple 'pass through' ActionCreator that returns the rawAction as an action.
 * @param rawAction
 * @param condition
 * @returns {Promise}
 */
function process(rawAction, condition) {
    if (!redux) throw Error('firefly has not been bootstrapped');

    var ac = actionCreators.get(rawAction.type);
    if (ac) {
        redux.dispatch(ac(rawAction));
    } else {
        redux.dispatch( rawAction );
    }

    return new Promise(
        function (resolve, reject) {
            if (condition) {
                // monitor application state for changes until condition is met..
                // invoke resolve() when this happens.
                // invoke now, since condition is not yet implemented.
                resolve('success');
            } else {
                // if no condition, go ahead and fulfill the promise
                resolve('success');
            }
        });
}

function addListener(listener, ...types) {
    if (!redux) return;
    if (types.length) {
        return () => {
            var appState = redux.getState()
        };
    } else {
        return redux.subscribe(listener);
    }
}

function registerCreator(type, actionCreator) {
    actionCreators.set(type, actionCreator);
}

function registerReducer(dataRoot, reducer) {
    reducers[dataRoot] = reducer;
}

function getState() {
    return redux ? redux.getState() : null;
}


function createSmartComponent(connector, component) {
    var Wrapper = connect(connector)(component);
    return (
        <Provider store={redux}>
            {() => <Wrapper/>}
        </Provider>
    );
}


export var reduxFlux = {
    registerCreator,
    registerReducer,
    bootstrap,
    getState,
    process,
    addListener,
    createSmartComponent
};

