/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';

export const READOUT_PREFIX= 'ReadoutCntlr';
export const CHANGE_LOCK_BY_CLICK= `${READOUT_PREFIX}.ChangeLockByClick`;
export const CHANGE_READOUT_PREFS= `${READOUT_PREFIX}.ChangeReadoutPref`;
export const CHANGE_LOCK_UNLOCK_BY_CLICK= `${READOUT_PREFIX}.ChangeLockUnlockByClick`;
export const READOUT_KEY= 'readout';
export const STANDARD_READOUT= 'standardImageReadout';
export const HIPS_STANDARD_READOUT= 'standardHiPSReadout';
export const NUM_VAL= 'value';
export const POINT_VAL= 'point';
export const HEALPIX_VAL= 'healpix';
export const DESC_VAL= 'desc';

export function readoutRoot() { return flux.getState()[READOUT_KEY]; }

export default {
    reducers () {return {[READOUT_KEY]: reducer};},
};



//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

export const dispatchChangeLockByClick= (lockByClick) =>
    flux.process({type: CHANGE_LOCK_BY_CLICK, payload: {lockByClick}});

export const dispatchChangeLockUnlockByClick= (isLocked) =>
    flux.process({type: CHANGE_LOCK_UNLOCK_BY_CLICK, payload: {isLocked}});

export const dispatchChangeReadoutPrefs= (readoutPref) =>
    flux.process({type: CHANGE_READOUT_PREFS, payload: {readoutPref}});

//======================================== Utility Functions =============================
//======================================== Utility Functions =============================
//======================================== Utility Functions =============================

export const isLockByClick= (root) => root.lockByClick;

export const isAutoReadIsLocked= (root) => root.isLocked;

/**
 * 
 * @param title
 * @param value
 * @param unit
 * @param precision
 * @return {{title: *, value: *, unit: *, precision: *}}
 */
export const makeValueReadoutItem= (title,value,unit, precision) => ({type:NUM_VAL, title,value,unit,precision});

/**
 * 
 * @param title
 * @param pt
 * @return {{title: *, pt: *}}
 */
export const makePointReadoutItem= (title,pt) => ({type:POINT_VAL,title,value:pt});

export const makeHealpixReadoutItem= (title,norder, value) => ({type:HEALPIX_VAL,title,norder, value});

export const makeDescriptionItem= (title) => ({value:title, type:DESC_VAL});

//--------------------------------------------------------------------


function reducer(state=initState(), action={}) {
    if (!action.payload || !action.type) return state;

    switch (action.type) {
        case CHANGE_LOCK_BY_CLICK:
            return {...state,lockByClick:action.payload.lockByClick};
        case CHANGE_LOCK_UNLOCK_BY_CLICK:
            return {...state,isLocked:action.payload.isLocked};
        case CHANGE_READOUT_PREFS:
            const readoutPref = state.readoutPref;
            const key = Object.keys(action.payload.readoutPref);
            readoutPref[key]=action.payload.readoutPref[ key];
            return {...state,readoutPref:{...state.readoutPref,...readoutPref}};
        default:
            return state;
    }
}

const initState= () =>
    ({
        lockByClick : false,
        isLocked: false,
        readoutPref :{
            imageMouseReadout1:'eqj2000hms',
            imageMouseReadout2: 'fitsIP',
            pixelSize: 'pixelSize',
            hipsMouseReadout1:'eqj2000hms',
            hipsMouseReadout2:'galactic',
            healpixPixel:'healpixPixel',
            healpixNorder:'healpixNorder',
            wl:'wl',
        }
    });
