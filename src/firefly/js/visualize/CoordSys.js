/**
 * Created by roby on 12/2/14.
 */



export const EQUATORIAL_J = 0;
export const EQUATORIAL_B = 1;
export const GALACTIC_JSYS     = 2;
export const ECLIPTIC_B   = 3;
export const SUPERGALACTIC_JSYS   = 4;
export const ECLIPTIC_J   = 13;


/**
 * @typedef {Object} CoordinateSys
 * @summary coordinate system
 * @description value is one of the following constants; EQ_J2000, EQ_B2000, EQ_B1950, GALACTIC,
 * SUPERGALACTIC, ECL_J2000, ECL_B1950, PIXEL, SCREEN_PIXEL, UNDEFINED,
 *
 * @global
 * @public
 */


export const CoordinateSys = function () {


    var init = function (desc, equatorial, jsys, equinox) {
        return {
            toString() { return desc; },
            isEquatorial() { return equatorial; },
            getJsys() { return jsys; },
            getEquinox() { return equinox; }
        };
    };

    var EQ_J2000 = init('EQ_J2000', true, EQUATORIAL_J, 2000 );
    var EQ_B2000 = init('EQ_B2000', true, EQUATORIAL_B, 2000);
    var EQ_B1950 = init('EQ_B1950', true, EQUATORIAL_B, 1950);
    var GALACTIC = init('GALACTIC', false, GALACTIC_JSYS, 2000);
    var SUPERGALACTIC = init('SUPERGALACTIC', false, SUPERGALACTIC_JSYS, 2000 );
    var ECL_J2000 = init('EC_J2000', false, ECLIPTIC_J, 2000);
    var ECL_B1950 = init('EC_B1950', false, ECLIPTIC_B, 1950);

    var PIXEL = init('PIXEL', false,-999, 0);
    var SCREEN_PIXEL = init('SCREEN_PIXEL', false,-999, 0);
    var UNDEFINED = init('UNDEFINED', false,-999, 0);
    var ZEROBASED = init('ZERO-BASED', false, -999, 0);
    var FITSPIXEL = init('FITSPIXEL', false, -999, 0);


    var parse= function(desc) {
        var coordSys;
        if (!desc) return null;
        desc= desc.toUpperCase();
        if        (desc===EQ_J2000.toString() || desc==='EQJ' || desc==='J2000' ) {
            coordSys = EQ_J2000;
        } else if (desc===EQ_B2000.toString())  {
            coordSys = EQ_B2000;
        } else if (desc===EQ_B1950.toString() || desc==='EQB' || desc==='B1950' ) {
            coordSys = EQ_B1950;
        } else if (desc===GALACTIC.toString() || desc==='GAL') {
            coordSys = GALACTIC;
        } else if (desc===SUPERGALACTIC.toString()) {
            coordSys = SUPERGALACTIC;
        } else if (desc===ECL_J2000.toString() || desc==='ECJ') {
            coordSys = ECL_J2000;
        } else if (desc===ECL_B1950.toString() || desc==='ECB') {
            coordSys = ECL_B1950;
        } else if (desc===SCREEN_PIXEL.toString()) {
            coordSys = SCREEN_PIXEL;
        } else if (desc===PIXEL.toString()) {
            coordSys = PIXEL;
        } else if (desc===ZEROBASED.toString()) {
            coordSys = ZEROBASED;
        } else if (desc===FITSPIXEL.toString()) {
            coordSys = FITSPIXEL;
        } else {
            coordSys = null;
        }

        return coordSys;
    };

    var retval = {};
    retval.parse= parse;
    retval.EQ_J2000 = EQ_J2000;
    retval.EQ_B2000 = EQ_B2000;
    retval.EQ_B1950 = EQ_B1950;
    retval.GALACTIC = GALACTIC;
    retval.SUPERGALACTIC = SUPERGALACTIC;
    retval.ECL_J2000 = ECL_J2000;
    retval.ECL_B1950 = ECL_B1950;
    retval.PIXEL = PIXEL;
    retval.ZEROBASED = ZEROBASED;
    retval.FITSPIXEL = FITSPIXEL;
    retval.SCREEN_PIXEL = SCREEN_PIXEL;
    retval.UNDEFINED = UNDEFINED;


    return retval;
}();

export function findCoordSys(jsys, equinox) {
    if      (jsys===EQUATORIAL_J && equinox===2000) return CoordinateSys.EQ_J2000;
    else if (jsys===EQUATORIAL_B && equinox===2000) return CoordinateSys.EQ_B2000;
    else if (jsys===EQUATORIAL_B && equinox===1950) return CoordinateSys.EQ_B1950;
    else if (jsys===ECLIPTIC_J && equinox===2000) return CoordinateSys.ECL_J2000;
    else if (jsys===ECLIPTIC_B && equinox===1950) return CoordinateSys.ECL_B1950;
    else if (jsys===GALACTIC_JSYS) return CoordinateSys.GALACTIC;
    else if (jsys===SUPERGALACTIC_JSYS) return CoordinateSys.SUPERGALACTIC;
    else return CoordinateSys.UNDEFINED;
}

export default CoordinateSys;

