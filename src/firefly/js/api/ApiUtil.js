
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import ReactDOM from 'react-dom';
import {get,isEmpty} from 'lodash';
import {isElement,isString} from 'lodash';
import {logError} from '../util/WebUtil.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {take} from 'redux-saga/effects';

// NOTE 
// NOTE 
//------------------------------------------------------------------------------------------
// Anything that is exported here becomes part of the lowlevel API
// It should have a jsdoc
//------------------------------------------------------------------------------------------
// NOTE 
// NOTE




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
    if (!isEmpty(msg) && isDebug()) {
        logError(...msg);
    }
}






/**
 *
 * @param {string|Object} div a div element or a string id of the div element
 * @param {Object} Component a react component
 * @param {Object} [props] props for the react component
 * @param {Object} [style] css style object, style of this root element. Remember, JavaScript refers to css
 * with camelCase instead of hyphenated.  For example, 'background-color' would be 'backgroundColor' in JavaScript.
 * @namespace firefly
 * @Function renderDOM
 */
export function renderDOM(div, Component, props, style={width:'100%', height:'100%'}) {
    const divElement= isString(div) ? document.getElementById(div) : div;

    if (!isElement(divElement)) debug(`the div element ${isString(div)?div:''} is not defined in the html` );
    if (!Component) debug('Component must be defined');


    const renderStuff= (
        <div style={style}
             className='rootStyle' >
            <Component {...props} />
        </div>
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
 *                 If it returns true if will be called the next time, if it returns false it will be removed.
 * @namespace firefly
 */
export function addListener(actionType,callBack) {
    dispatchAddSaga(actionReport,{actionType,callBack});
}





//=========================================================================
//------------------ Private ----------------------------------------------
//=========================================================================

function *actionReport({actionType,callBack},dispatch,getState) {
    if (!actionType && !callBack) return;
    var keepGoing= true;
    while (keepGoing) {
        const action= yield take(actionType);
        keepGoing= callBack(action,getState());
    }
}
