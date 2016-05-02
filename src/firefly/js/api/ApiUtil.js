
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
 */
export const isDebug = () => get(window, 'firefly.debug', false);

/**
 * show a debug message if debugging is enabled
 * @param msg any number of messages
 */
export function debugMsg(...msg) {
    if (!isEmpty(msg) && isDebug()) {
        logError(...msg);
    }
}



/**
 *
 * @param {string|{}} div a div element or a string id of the div element
 * @param {{}} Component a react component
 * @param {{}} [props] props for the react component
 * @param {{}} [style] css style object, style of this root element
 */
export function renderDOM(div, Component, props, style={width:'100%', height:'100%'}) {
    const divElement= isString(div) ? document.getElementById(div) : div;

    if (!isElement(divElement)) debugMsg(`the div element ${isString(div)?div:''} is not defined in the html` );
    if (!Component) debugMsg('Component must be defined');


    const renderStuff= (
        <div style={style}
             className='rootStyle' >
            <Component {...props} />
        </div>
    );

    ReactDOM.render(renderStuff,divElement);
}


/**
 * removed the redenered element
 * @param {string|{}} div a div element or a string id of the div element
 */
export function unrenderDOM(div) {
    const divElement= isString(div) ? document.getElementById(div) : div;
    ReactDOM.unmountComponentAtNode(divElement);
}



/**
 *
 * @param actionType a string or and array of strings. Each string is an action constant from firefly.action.type
 * @param callBack the call back will be call with two parameters: action object and state object
 *                 If it returns true if will be called the next time, if it returns false it will be removed.
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
