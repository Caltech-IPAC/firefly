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
import ExternalAccessCntlr from './ExternalAccessCntlr.js';
import VisMouseCntlr from '../visualize/VisMouseCntlr.js';
import HistogramCntlr from '../visualize/HistogramCntlr.js';

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
    [AppDataCntlr.APP_DATA_PATH]: AppDataCntlr.reducer,
    [VisMouseCntlr.VIS_MOUSE_KEY]: VisMouseCntlr.reducer,
    [FieldGroupCntlr.FIELD_GROUP_KEY]: FieldGroupCntlr.reducer,
    [ImagePlotCntlr.IMAGE_PLOT_KEY]: ImagePlotCntlr.reducer,
    [ExternalAccessCntlr.EXTERNAL_ACCESS_KEY]: ExternalAccessCntlr.reducer,
    [HistogramCntlr.HISTOGRAM_DATA_KEY]: HistogramCntlr.reducer
};

let redux = null;


// pre-map a set of action => creator prior to boostraping.
actionCreators.set(AppDataCntlr.APP_LOAD, AppDataCntlr.loadAppData);
actionCreators.set(FieldGroupCntlr.VALUE_CHANGE, FieldGroupCntlr.valueChangeActionCreator);
actionCreators.set(ExternalAccessCntlr.EXTENSION_ACTIVATE, ExternalAccessCntlr.extensionActivateActionCreator);
actionCreators.set(ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.plotImageActionCreator);
actionCreators.set(ImagePlotCntlr.ZOOM_IMAGE, ImagePlotCntlr.zoomActionCreator);

actionCreators.set(HistogramCntlr.LOAD_TBL_STATS, HistogramCntlr.loadTblStats);
actionCreators.set(HistogramCntlr.LOAD_COL_DATA, HistogramCntlr.loadColData);

/**
 * object with a key that can be filtered out, value should be a boolean or a function that returns a boolean
 */
var filterOutOfLogging= {
    [ExternalAccessCntlr.EXTENSION_ACTIVATE]: (action) => !action.payload.extension || action.payload.extension.extType!=='PLOT_MOUSE_READ_OUT',
    [FieldGroupCntlr.MOUNT_COMPONENT]: false
};

/**
 * array of action types that will be logged as collapsed
 */
var collapsedLogging= [
    ExternalAccessCntlr.EXTENSION_ACTIVATE
];

window.enableFireflyReduxLogging= true;


function logFilter(getState,action) {
    if (!window.enableFireflyReduxLogging) return false;

    var fType= typeof filterOutOfLogging[action.type];
    if (filterOutOfLogging[action.type] != 'undefined') {
        if (fType==='function') {
            return (filterOutOfLogging[action.type](action));
        }
        else if (fType==='boolean') {
            return filterOutOfLogging[action.type];
        }
        else {
            return false;
        }
    }
    else {
        return true;
    }
}



function collapsedFilter(getState,action) {
    return collapsedLogging.includes(action.type);
}


var logger= loggerMiddleware({duration:true, predicate:logFilter, collapsed:collapsedFilter});


function createRedux() {
    // create a rootReducer from all of the registered reducers
    const rootReducer = combineReducers(reducers);

    // redux is a store and more.. it manages reducers as well as thunk actions
    // we'll call it redux for now.
    return applyMiddleware(actionSideEffectMiddleware, thunkMiddleware, logger)(createStore)(rootReducer);
}

function bootstrap() {
    if (redux === null) {
        redux = createRedux();
    }
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
 *
 * <i>Note: </i> Often it makes sense to have a utility function call <code>process</code>. In that case
 * the utility function should meet the follow criteria.  This is a good way to document and default the
 * payload parameters.  The utility function should implment the following standard:
 * <ul>
 *     <li>The function name should start with "dispatch"</li>
 *     <li>The action type as the second part of the name</li>
 *     <li>The function should be exported from the controller</li>
 *     <li>The function parameters should the documented with jsdocs</li>
 *     <li>Optional parameters should be clear</li>
 * </ul>
 * Utility function Example - if action type is <code>PLOT_IMAGE</code> and the <code>PLOT_IMAGE</code> action
 * is exported from the ImagePlotCntlr module.  The the name should be <code>processPlotImage</code>.
 *
 *
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
            var appState = redux.getState();
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

