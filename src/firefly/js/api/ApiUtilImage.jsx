
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {take,race,call} from 'redux-saga/effects';
import {MouseState} from '../visualize/VisMouseSync.js';
import ImagePlotCntlr, {visRoot,dispatchChangeExpandedMode, 
                        dispatchApiToolsView, ExpandType} from '../visualize/ImagePlotCntlr.js';
import {isString} from 'lodash';
import {flux} from '../Firefly.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import  {DefaultApiReadout} from '../visualize/ui/DefaultApiReadout.jsx';
import  {VerySimpleMouseReadout} from '../visualize/ui/VerySimpleMouseReadout.jsx';
import DialogRootContainer from '../ui/DialogRootContainer.jsx';
import {PopupPanel, LayoutType} from '../ui/PopupPanel.jsx';
import {dispatchShowDialog,dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr.js';
import {readoutRoot, isLockByClick} from '../visualize/MouseReadoutCntlr.js';
import {mouseUpdatePromise} from '../visualize/VisMouseSync.js';
import {renderDOM,unrenderDOM} from './ApiUtil.js';
import {RangeValues} from '../visualize/RangeValues.js';

const API_READOUT= 'apiReadout';

// NOTE 
// NOTE 
//----------------------------------------------------------------
// Anything that is exported here becomes part of the lowlevel API
// It should have a jsdoc
//----------------------------------------------------------------
// NOTE 
// NOTE 

export {RangeValues} from '../visualize/RangeValues.js';
export {WPConst, WebPlotRequest, findInvalidWPRKeys, confirmPlotRequest} from '../visualize/WebPlotRequest.js';
export {RequestType} from '../visualize/RequestType';
export {ExpandType} from '../visualize/ImagePlotCntlr.js';



/**
 * 
 * @param Component the react component to use for expanded view
 * @param props props for the component
 * @param {string|{}} div a div element or a string id of the div element
 */
export function initImageViewExpanded(Component, props={}, div ){

    // const EXPANDED_DIV= 'expandedArea';
    // var expandedDivEl;
    // if (div) {
    //     expandedDivEl= isString(div) ? document.getElementById(div) : div;
    // }
    // else {
    //     expandedDivEl= document.createElement('div');
    //     document.body.appendChild(expandedDivEl);
    //     expandedDivEl.id= EXPANDED_DIV;
    //     expandedDivEl.style.visibility= 'hidden';
    //     expandedDivEl.className= 'api-expanded';
    //
    // }
    //
    // var isExpanded= false;
    //
    // const unExpand= () =>{
    //     unrenderDOM(expandedDivEl);
    //     isExpanded= false;
    //     dispatchChangeExpandedMode(ExpandType.COLLAPSE);
    //     expandedDivEl.style.visibility= 'hidden';
    // };
    //
    //
    // flux.addListener(() => {
    //     const vr= visRoot();
    //     if (vr.expandedMode!==ExpandType.COLLAPSE && !isExpanded) {
    //         isExpanded= true;
    //         expandedDivEl.style.visibility= 'visible';
    //         renderDOM(expandedDivEl, Component, Object.assign({closeFunc:unExpand}, props),
    //             { left: 1, right: 1, top: 1, bottom: 1, position: 'absolute' });
    //     }
    // });
    //
    
    dispatchApiToolsView(true);//todo move this somewhere, not sure where
}


/**
 * initialize the auto readout. Must be call once at the begging to get the popup readout running.
 * @param ReadoutComponent
 * @param props
 */
export function initAutoReadout(ReadoutComponent= DefaultApiReadout, 
                                props={MouseReadoutComponent:VerySimpleMouseReadout}){
    dispatchAddSaga(autoReadoutVisibility, {ReadoutComponent,props});
}


/**
 *
 * @param stretchType the type of stretch may be 'Percent', 'Absolute', 'Sigma'
 * @param lowerValue lower value of stretch, based on stretchType
 * @param upperValue upper value of stretch, based on stretchType
 * @param algorithm the stretch algorithm to use, may be 'Linear', 'Log', 'LogLog', 'Equal', 'Squared', 'Sqrt'
 */
export function serializeSimpleRangeValues(stretchType,lowerValue,upperValue,algorithm) {
    const rv= RangeValues.makeSimple(stretchType,lowerValue,upperValue,algorithm);
    return rv.serialize();
}


//========== Private ================================
//========== Private ================================
//========== Private ================================
//========== Private ================================

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function *autoReadoutVisibility({ReadoutComponent,props}) {
    var inDialog= false;
    var showing;
    var mouseState;
    // var action;
    var doYield= true;
    var winner;
    while (true) {
        // if (doYield) action= yield take([MOUSE_STATE_CHANGE, ImagePlotCntlr.CHANGE_EXPANDED_MODE]);

        if (doYield) {
            winner = yield race({
                expandedChange: take([ImagePlotCntlr.CHANGE_EXPANDED_MODE]),
                mouse: call(mouseUpdatePromise)
            });
        }


        doYield= true;
        if (winner.mouse) {
            mouseState = winner.mouse.mouseState;
        }
        if (visRoot().expandedMode!==ExpandType.COLLAPSE) {
            hideReadout();
            continue;
        }
        if (!mouseState) continue;
        showing= isDialogVisible(API_READOUT);
        if (mouseState!==MouseState.EXIT && !showing) {
            showReadout(ReadoutComponent,props, (inD) => {
                inDialog= inD;
            });
        }
        else if (mouseState===MouseState.EXIT && showing) {
            const winner = yield race({
                           expandedChange: take([ImagePlotCntlr.CHANGE_EXPANDED_MODE]),
                           mouse: call(mouseUpdatePromise),
                           timer: call(delay, 3000)
                         });
            if ((!winner.expandedChange || !winner.mouse) && !inDialog && !isLockByClick(readoutRoot())) {
                hideReadout();
            }
            else {
                doYield= false;
            }
        }
    }
}








function showReadout(DefaultApiReadout, props={}, mouseInDialog) {
    const popup= (
        <PopupPanel title={'Details'} layoutPosition={LayoutType.TOP_RIGHT} mouseInDialog={mouseInDialog} >
            <DefaultApiReadout {...props} />
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(API_READOUT, popup);
    dispatchShowDialog(API_READOUT);
}

function hideReadout() {
    if (isDialogVisible(API_READOUT)) dispatchHideDialog(API_READOUT);
}

