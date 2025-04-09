/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {getPreference} from '../core/AppDataCntlr';
import {flux} from '../core/ReduxFlux.js';
import Enum from 'enum';


export const READOUT_PREFIX= 'ReadoutCntlr';
export const CHANGE_LOCK_BY_CLICK= `${READOUT_PREFIX}.ChangeLockByClick`;
export const CHANGE_READOUT_PREFS= `${READOUT_PREFIX}.ChangeReadoutPref`;
export const READOUT_KEY= 'readout';
export const STANDARD_READOUT= 'standardImageReadout';
export const HIPS_STANDARD_READOUT= 'standardHiPSReadout';
export const NUM_VAL= 'value';
export const FILE_VAL= 'fileValue';
export const POINT_VAL= 'point';
export const HEALPIX_VAL= 'healpix';
export const DESC_VAL= 'desc';


export const STATUS_UNAVAILABLE= 'UNAVAILABLE';
export const STATUS_NAN= 'NaN';
export const STATUS_UNDEFINED= 'UNDEFINED';
export const STATUS_VALUE= 'VALUE';
export const TYPE_EMPTY= 'EMPTY';
export const TYPE_DECIMAL_INT = 'DECIMAL_INT';
export const TYPE_FLOAT= 'FLOAT';
export const TYPE_BASE16= 'BASE16';
export const TYPE_BASE_OTHER= 'BASE_OTHER';


export const MR_PIXEL_SIZE= 'pixelSize';
export const MR_SPIXEL_SIZE= 'sPixelSize';
export const MR_EQJ2000_HMS= 'eqj2000hms';
export const MR_EQJ2000_DCM= 'eqj2000DCM';
export const MR_GALACTIC= 'galactic';
export const MR_SUPER_GALACTIC= 'superGalactic';
export const MR_EQB1950= 'eqb1950';
export const MR_EQB1950_DCM= 'eqb1950DCM';
export const MR_ECLJ2000= 'eclJ2000';
export const MR_ECL1950= 'eclB1950';
export const MR_WCS_COORDS= 'wcsCoords';
export const MR_FITS_IP= 'fitsIP';
export const MR_ZERO_IP= 'zeroIP';
export const MR_HEALPIX_PIXEL= 'healpixPixel';
export const MR_HEALPIX_NORDER='healpixNorder';
export const MR_WL= 'wl';
export const MR_BAND_WIDTH= 'bandWidth';

export const MR_FIELD_IMAGE_MOUSE_READOUT1= 'imageMouseReadout1';
export const MR_FIELD_IMAGE_MOUSE_READOUT2= 'imageMouseReadout2';
export const MR_FIELD_HIPS_MOUSE_READOUT1= 'hipsMouseReadout1';
export const MR_FIELD_HIPS_MOUSE_READOUT2= 'hipsMouseReadout2';

/**
 * Equatorial Coordinates Representation Type
 * @typedef {Enum} EQ_TYPE
 * @prop DCM Decimal Degrees
 * @prop HMS Hour Minute Second
 */
/** @type EQ_TYPE */
export const EQ_TYPE = new Enum(['DCM', 'HMS'], {ignoreCase: true});



export function readoutRoot() { return flux.getState()[READOUT_KEY]; }

export default {
    reducers () {return {[READOUT_KEY]: reducer};},
};



//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

export const dispatchChangeLockByClick= (lockByClick) =>
    flux.process({type: CHANGE_LOCK_BY_CLICK, payload: {lockByClick}});

export const dispatchChangeReadoutPrefs= (readoutPref) =>
    flux.process({type: CHANGE_READOUT_PREFS, payload: {readoutPref}});



export function initReadoutPrefs() {
    dispatchChangeReadoutPrefs(
        {
            [MR_FIELD_IMAGE_MOUSE_READOUT1]: getPreference(MR_FIELD_IMAGE_MOUSE_READOUT1,MR_EQJ2000_HMS),
            [MR_FIELD_IMAGE_MOUSE_READOUT2]: getPreference(MR_FIELD_IMAGE_MOUSE_READOUT2,MR_FITS_IP),
            [MR_FIELD_HIPS_MOUSE_READOUT1]: getPreference(MR_FIELD_HIPS_MOUSE_READOUT1,MR_EQJ2000_HMS),
            [MR_FIELD_HIPS_MOUSE_READOUT2]: getPreference(MR_FIELD_HIPS_MOUSE_READOUT2,MR_GALACTIC),
        }
    );
}

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
 * @return {{title: string, value: *, unit: string, precision: number, radix: number}}
 */
export const makeValueReadoutItem= (title,value,unit, precision) => ({type:NUM_VAL, title,value,unit,precision});

export const makeFileValueReadoutItem= (title,valueBase10, valueBase16,radix, status,type, unit,precision) =>
    ({type:FILE_VAL, title,valueBase10, valueBase16,status,radix, readoutType:type,unit,precision});

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
        case CHANGE_READOUT_PREFS:
            return {...state,readoutPref: {...state.readoutPref, ...action.payload.readoutPref}};
        default:
            return state;
    }
}


const initState= () =>
    ({
        lockByClick : false,
        isLocked: false,
        readoutPref :{
            [MR_FIELD_IMAGE_MOUSE_READOUT1]: MR_EQJ2000_HMS, //getPreference(MR_FIELD_IMAGE_MOUSE_READOUT1,MR_EQJ2000_HMS),
            [MR_FIELD_IMAGE_MOUSE_READOUT2]: MR_FITS_IP, //getPreference(MR_FIELD_IMAGE_MOUSE_READOUT2,MR_FITS_IP),
            imageMouseNoncelestialReadout1: MR_WCS_COORDS,
            imageMouseNoncelestialReadout2: MR_FITS_IP,
            pixelSize: MR_PIXEL_SIZE,
            [MR_FIELD_HIPS_MOUSE_READOUT1]: MR_EQJ2000_HMS, //getPreference(MR_FIELD_HIPS_MOUSE_READOUT1,MR_EQJ2000_HMS),
            [MR_FIELD_HIPS_MOUSE_READOUT2]: MR_GALACTIC, //getPreference(MR_FIELD_HIPS_MOUSE_READOUT2,MR_GALACTIC),
            mouseReadoutValueCopy: 'str',
            healpixPixel: MR_HEALPIX_PIXEL,
            healpixNorder: MR_HEALPIX_NORDER,
            intFluxValueRadix: '10',
            floatFluxValueRadix: '10',
            wl: MR_WL,
            bandWidth: MR_BAND_WIDTH,
        }
    });
