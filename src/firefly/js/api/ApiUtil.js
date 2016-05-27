/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import ReactDOM from 'react-dom';
import {get,isEmpty} from 'lodash';
import {isElement,isString} from 'lodash';
import {logErrorWithPrefix} from '../util/WebUtil.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {take,race,call} from 'redux-saga/effects';

// NOTE 
// NOTE 
//------------------------------------------------------------------------------------------
// Anything that is exported here becomes part of the lowlevel API
// It should have a jsdoc
//------------------------------------------------------------------------------------------
// NOTE 
// NOTE

export {getBoolean} from '../util/WebUtil.js';
export {toBoolean} from '../util/WebUtil.js';


/**
 * Is in debug mode
 * @namespace firefly
 * @Function
 */
export const isDebug = () => get(window, 'firefly.debug', false);

/**
 * show a debug message if debugging is enabled
 * @param {String|Error} msg any number of messages
 */
export function debug(...msg) {
    if (isDebug() && !isEmpty(msg)) {
        logErrorWithPrefix('Firefly:', ...msg);
    }
}






/**
 *
 * @param {string|Object} div a div element or a string id of the div element
 * @param {Object} Component a react component
 * @param {Object} [props] props for the react component
 * @namespace firefly
 * @Function renderDOM
 */
export function renderDOM(div, Component, props) {
    const divElement= isString(div) ? document.getElementById(div) : div;

    if (!isElement(divElement)) debug(`the div element ${isString(div)?div:''} is not defined in the html` );
    if (!Component) debug('Component must be defined');
    divElement.classList.add('rootStyle');

    const renderStuff= (
            <Component {...props} />
    );

    ReactDOM.render(renderStuff,divElement);
}


/**
 * removed the rendered element
 * @param {string|Object} div a div element or a string id of the div element
 * @namespace firefly
 */
export function unrenderDOM(div) {
    const divElement= isString(div) ? document.getElementById(div) : div;
    ReactDOM.unmountComponentAtNode(divElement);
}



/**
 * Add a listener to any action type
 * @param {string} actionType a string or and array of strings. Each string is an action constant from firefly.action.type
 * @param {function} callBack the call back will be call with two parameters: action object and state object
 *                 If it returns true the listener will be removed.
 * @return {function} a function that will remove the listener
 */
export function addActionListener(actionType,callBack) {
    var pResolve;
    const cancelPromise= new Promise((resolve) => pResolve= resolve);
    dispatchAddSaga(actionReport,{actionType,callBack, cancelPromise});
    return () => pResolve();
}

//=========================================================================
//------------------ Private ----------------------------------------------
//=========================================================================

/**
 * This saga call a user callback when when one of the actionTypes are dispatched
 * This saga does the following:
 * <ul>
 *     <li>waits until a type in the actionType list is dispatched or a cancel promise
 *     <li>if an action call the callback
 *     <li>if the call back returns true or the cancel promise is resolved then exit
 * </ul>
 * @param {string|[]} actionType 
 * @param callBack the user callback
 * @param cancelPromise a promise to cancel the callback
 * @param dispatch
 * @param getState a function get the application state
 * 
 */
function *actionReport({actionType,callBack, cancelPromise},dispatch,getState) {
    if (!actionType && !callBack) return;
    var stopListening= false;
    while (!stopListening) {
        var raceWinner = yield race({
            action: take(actionType),
            cancel: call(() => cancelPromise)
        });
        stopListening= raceWinner.action ? callBack(raceWinner.action,getState()) : true;
    }
}
