/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get,isEmpty} from 'lodash';
import {flux} from '../Firefly.js';
import {clone} from '../util/WebUtil.js';

export const READOUT_PREFIX= 'ReadoutCntlr';

export const READOUT_DATA= `${READOUT_PREFIX}.ReadoutData`;
export const CHANGE_LOCK_BY_CLICK= `${READOUT_PREFIX}.ChangeLockByClick`;
export const CHANGE_READOUT_PREFS= `${READOUT_PREFIX}.ChangeReadoutPref`;
export const CHANGE_LOCK_UNLOCK_BY_CLICK= `${READOUT_PREFIX}.ChangeLockUnlockByClick`;
export const READOUT_KEY= 'readout';
export const STANDARD_READOUT= 'standardReadout';

export const NUM_VAL= 'value';
export const POINT_VAL= 'point';
export const DESC_VAL= 'desc';

export function readoutRoot() { return flux.getState()[READOUT_KEY]; }

//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

/**
 * 
 * @param plotId
 * @param readoutItems
 * @param threeColor
 * @param readoutKey
 */
export function dispatchReadoutData(plotId, readoutItems, threeColor= false, readoutKey=STANDARD_READOUT) {
    flux.process({type: READOUT_DATA, payload: {readoutKey, plotId, threeColor, 
                                                readoutItems, 
                                                 hasValues:!isEmpty(readoutItems)}});
}

export function dispatchChangeLockByClick(lockByClick) {
    flux.process({type: CHANGE_LOCK_BY_CLICK, payload: {lockByClick}});
}


export function dispatchChangeLockUnlockByClick(isLocked) {
    flux.process({type: CHANGE_LOCK_UNLOCK_BY_CLICK, payload: {isLocked}});
}

export function dispatchChangeReadoutPrefs(readoutPref) {
    flux.process({type: CHANGE_READOUT_PREFS, payload: {readoutPref}});
}


//======================================== Utility Functions =============================
//======================================== Utility Functions =============================
//======================================== Utility Functions =============================

export function getReadout(root,readoutKey= STANDARD_READOUT) {
    return root[readoutKey];
}

export function getReadoutPref(root,key) {
    return root.readoutPref[key];
}

export function isLockByClick(root) {
    return root.lockByClick;
}


export function isAutoReadIsLocked(root) {
    return root.isLocked;
}


/**
 * 
 * @param title
 * @param value
 * @param unit
 * @param precision
 * @return {{title: *, value: *, unit: *, precision: *}}
 */
export function makeValueReadoutItem(title,value,unit, precision) {
    return {type:NUM_VAL, title,value,unit,precision};
}

/**
 * 
 * @param title
 * @param pt
 * @return {{title: *, pt: *}}
 */
export function makePointReadoutItem(title,pt) {
    return {type:POINT_VAL,title,value:pt};
}

export function makeDescriptionItem(title) {
    return {value:title, type:DESC_VAL};
}



//--------------------------------------------------------------------


export function reducer(state=initState(), action={}) {

    if (!action.payload || !action.type) return state;

    var retState= state;
    switch (action.type) {
        case READOUT_DATA:
            retState= processReadoutData(state,action);
            break;
        case CHANGE_LOCK_BY_CLICK:
            retState= clone(state,{lockByClick:action.payload.lockByClick});
            break;

        case CHANGE_LOCK_UNLOCK_BY_CLICK:
            retState= clone(state,{isLocked:action.payload.isLocked});
            break;
        case CHANGE_READOUT_PREFS:
            var readoutPref = state.readoutPref;
            var key = Object.keys(action.payload.readoutPref);
            readoutPref[key]=action.payload.readoutPref[ key];
            retState= clone(state,{readoutPref:clone(state.readoutPref,readoutPref)});
            break;

        default:
            break;
    }
    return retState;
}


 function processReadoutData(state,action) {
     const {plotId, readoutKey,readoutItems, threeColor, hasValues}= action.payload;
     return clone(state, {[readoutKey]: {plotId, threeColor, hasValues, readoutItems}});
 }




const initState= function() {

    return {
        [STANDARD_READOUT] : {},
        lockByClick : false,
        isLocked: false,
        readoutPref :{
            mouseReadout1:'eqj2000hms',
            mouseReadout2: 'fitsIP',
            pixelSize: 'pixelSize'

        }
    };

};

