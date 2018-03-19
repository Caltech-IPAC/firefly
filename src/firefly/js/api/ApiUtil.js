/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component} from 'react';
import ReactDOM from 'react-dom';
import {get,isArray,isElement,isEmpty,isString} from 'lodash';
import {logErrorWithPrefix, isDebug} from '../util/WebUtil.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../core/MasterSaga.js';
import {uniqueID} from '../util/WebUtil';

// NOTE 
// NOTE 
//------------------------------------------------------------------------------------------
// Anything that is exported here becomes part of the lowlevel API
// It should have a jsdoc
//------------------------------------------------------------------------------------------
// NOTE 
// NOTE
/**
 * @public
 */
export {getBoolean, toBoolean, isDebug} from '../util/WebUtil.js';

export {getWsConnId, getWsChannel} from '../core/AppDataCntlr.js';

/**
 * show a debug message if debugging is enabled
 * @param {String|Error} msg any number of messages
 * @public
 * @func debug
 * @memberof firefly.util
 *
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
 * @public
 * @function renderDOM
 * @memberof firefly.util
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
 *
 * removed the rendered element
 */
/**
 * @param {string|Object} div a div element or a string id of the div element
 * @public
 * @function unrenderDOM
 * @memberof firefly.util
 */

export function unrenderDOM(div) {
    const divElement= isString(div) ? document.getElementById(div) : div;
    ReactDOM.unmountComponentAtNode(divElement);
}


/**
 * @summary Add a listener to any action type
 * @param {string} actionType a string or an array of strings. Each string is an action constant from firefly.action.type
 * @param {function} callBack the call back will be called with three parameters: action object, state object, and extraData
 *                 If it returns true the listener will be removed.
 * @param {object} extraData an object with data to send to the callback, can be anything
 *
 * @return {function} a function that will remove the listener
 * @public
 * @func addActionListener
 * @memberof firefly.util
 */
export function addActionListener(actionType,callBack, extraData) {

    if (!isArray(actionType) && !isString(actionType)) {
        throw 'actionType must be a string or an array of strings';
    }

    // @callback {actionWatcherCallback}
    const wrapperCallback = (action, cancelSelf, params, dispatch, getState) => {
        if (callBack(action, getState(), params)) {
            cancelSelf();
        }
    };

    const id = uniqueID();
    const actions = isArray(actionType) ? actionType : [actionType];

    dispatchAddActionWatcher({id, actions, callback: wrapperCallback, params: extraData});
    return (() => dispatchCancelActionWatcher(id));
}

