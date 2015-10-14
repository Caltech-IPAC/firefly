/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import thunkMiddleware from 'redux-thunk';
import loggerMiddleware from 'redux-logger';
import { createStore, applyMiddleware } from 'redux';


var reducers = {};

/*
 * This is a map of request_id to ActionCreator
 * */
var actionCreators = new Map();

var redux = {};

function creatRedux() {
    // this is needed so that the store would recognize thunk actions
    const createStoreWithMiddleware = applyMiddleware(
        thunkMiddleware, // lets us dispatch() functions
        loggerMiddleware // neat middleware that logs actions
    )(createStore);

    // create a rootReducer from all of the registered reducers
    var rootReducer = combineReducers(reducers);

    // redux is a store and more.. it manages reducers as well as thunk actions
    // we'll call it redux for now.
    return createStoreWithMiddleware(rootReducer);
}

function doBoostrap() {
    redux = creatRedux();
    return new Promise(
        function (resolve, reject) {
            // there may be async logic here..
            // if not, simply invoke resolve.
            resolve('success');
        })
}

function doProcess(request, condition) {

    var ac = actionCreators.get(request.type);
    if (ac) {
        ac(request);
    } else {
        redux.dispatch(() => { return request });
    }

    return new Promise(
        function (resolve, reject) {
            if (condition) {
                // monitor application state for changes until condition is met..
                // invoke resolve() when this happens.
            } else {
                // if no condition, go ahead and fulfill the promise
                resolve('success');
            }
        })
}

function addTypesListener(listener, ...types) {
    if (types) {
        return () => {
            var appState = redux.getState();

        }
    } else {
        return redux.subscribe(listener);
    }

}


var flux = {
    registerCreator(type, actionCreator) {
        creator.set(type, actionCreator);
    },

    registerReducer(dataRoot, reducer) {
        reducers[dataRoot] = reducer;
    },

    bootstrap() {
        return doBoostrap();
    },

    process(request, condition) {
        return doProcess(request, condition);
    },

    getState() {
        return redux.getState();
    },

    addListener(listener, ...types) {
        return addTypesListener(listener, types);
    }

};

export default flux;
