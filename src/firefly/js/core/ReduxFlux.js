/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import thunkMiddleware from 'redux-thunk';
import loggerMiddleware from 'redux-logger';
import { createStore, applyMiddleware } from 'redux';
import rootreducer from '../stores/Reducers.js';


// this is needed so that the store would recognize thunk actions
const createStoreWithMiddleware = applyMiddleware(
    thunkMiddleware, // lets us dispatch() functions
    loggerMiddleware // neat middleware that logs actions
)(createStore);


// redux is a store and more.. it manages reducers as well as thunk actions
// we'll call it redux for now.
let store = createStoreWithMiddleware(rootreducer);


var reducers = new Map();

/*
* This is a map of request_id to ActionCreator
* */
var creators = new Map();


function createFlux() {

}

function doBoostrap() {
    createFlux();
}


var flux = {
    registerCreator(type, actionCreator) {
        creator.set(type, actionCreator);
    },

    registerReducer(dataRoot, reducer) {
        reducers.set(dataRoot, reducer)
    },

    bootstrap() {
        return new Promise(
            function (resolve, reject) {
                doBoostrap();
                if (true) {
                    resolve("success");
                } else {
                    reject("fail");
                }
            })
    },

    process(request, condition) {
        return new Promise(
            function (resolve, reject) {
                if (condition) {

                } else {
                    // if no condition, go ahead and fulfill the promise
                    resolve("success");
                }
            })
    }

};

export default flux;