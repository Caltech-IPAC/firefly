/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {jcnvc2} from './Jcnvc2.js';
import {gtjulp, unjulp} from './Gtjul2.js';



export const EQUATORIAL_J = 0;
export const EQUATORIAL_B = 1;
export const GALACTIC     = 2;
export const ECLIPTIC_B   = 3;
export const SUPERGALACTIC   = 4;
export const ECLIPTIC_J   = 13;



/**
 * do the conversion when there is no proper motion
 * in_sys and out_sys should be on of the final int defined in this class.
 * @param {number} in_sys one of {EQUATORIAL_J, EQUATORIAL_B, GALACTIC, ECLIPTIC_B, SUPERGALACTIC, ECLIPTIC_J}
 * @param {number} in_equinox
 * @param {number} in_lon
 * @param {number} in_lat
 * @param {number} out_sys one of {EQUATORIAL_J, EQUATORIAL_B, GALACTIC, ECLIPTIC_B, SUPERGALACTIC, ECLIPTIC_J}
 * @param {number} out_equinox
 * @param {number} tobs
 * @return {{lon: number, lat: number}}
 */
export function doConv(in_sys, in_equinox, in_lon, in_lat, out_sys, out_equinox, tobs)  {
    var out_lon = 0.0;
    var out_lat = 0.0;
    const ret = jcnvc2(in_sys, in_equinox, in_lon, in_lat, out_sys,  out_equinox, out_lon, out_lat, tobs);
    return {lon:ret.xnew, lat:ret.ynew};
}


/**
 * only handles the Proper Motion conversion from Equatorial B1950 to J2000, or vice versa.
 * proper motion unit: arcsec/year
 * @param {boolean} fromB1950ToJ2000
 * @param {number} in_lon
 * @param {number} in_lat
 * @param {number} in_pmlon
 * @param {number} in_pmlat
 * @return {{ra: number, dec: number, raPM: number, decPM: number}}
 */
export function doConvPM(fromB1950ToJ2000, in_lon, in_lat, in_pmlon, in_pmlat) {
    var out_lon = 0.0;
    var out_lat = 0.0;
    var out_pmlon = 0.0;
    var out_pmlat = 0.0;
    var in_equinox = 1950.0;
    var out_equinox = 1950.0;
    var in_p = 0.0;
    var in_v = 0.0;
    var in_ieflag = 1; // remove the E-terms of aberration if any 
    // -1 if do not want to remove the E-terms
    var ret;

    // convert from arcsec to seconds(AR8211, 9/2008), per year to per century
    var pmLon = (in_pmlon * 100.0) / (15.0 * Math.cos(in_lat * Math.PI / 180.0));
    if (fromB1950ToJ2000) {  // from B1950 to J2000
        ret = gtjulp(in_equinox, in_lon, in_lat, pmLon, in_pmlat * 100.0,
            in_p, in_v, in_ieflag, out_lon, out_lat, out_pmlon, out_pmlat);
    }
    else {// from J2000 to B1950
        ret = unjulp(in_lon, in_lat, pmLon, in_pmlat * 100.0, in_p, in_v, in_ieflag,
            out_equinox, out_lon, out_lat, out_pmlon, out_pmlat);
    }
    // convert from seconds to arcsec(AR8211, 9/2008), per century to per year, 

    var pmLonOut = (ret.pmra / 100.0) * 15.0 * Math.cos(ret.dec * Math.PI / 180.0);

    return {ra: ret.ra, dec: ret.dec, raPM: pmLonOut, decPM: ret.pmdec/100.0};
}

