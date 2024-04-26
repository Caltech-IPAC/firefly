/**
 * Created by roby on 12/2/14.
 */



export const EQUATORIAL_J = 0;
export const EQUATORIAL_B = 1;
export const GALACTIC_JSYS     = 2;
export const ECLIPTIC_B   = 3;
export const SUPERGALACTIC_JSYS   = 4;
export const ECLIPTIC_J   = 13;
export const NONCELESTIAL = -999;


/**
 * @typedef {Object} CoordinateSys
 * @summary coordinate system
 * @description value is one of the following constants;
 * EQ_J2000, EQ_B2000, EQ_B1950, GALACTIC, SUPERGALACTIC, ECL_J2000, ECL_B1950,
 * PIXEL, SCREEN_PIXEL, ZERO_BASED, FITSPIXEL, UNDEFINED
 *
 * @prop {Function} isEquatorial - True, if coordinate system is Equatorial
 * @prop {Function} isCelestial - True, if coordinate system is a recognized celestial system
 * @prop {Function} getJsys
 * @prop {Function} getEquinox
 * @global
 * @public
 */


export const CoordinateSys = function () {
    const init = (desc, equatorial, jsys, equinox) => {
        return {
            toString() { return desc; },
            isEquatorial() { return equatorial; },
            isCelestial() { return jsys !== NONCELESTIAL; },
            getJsys() { return jsys; },
            getEquinox() { return equinox; }
        };
    };

    // recognized celestial coordinate systems
    const EQ_J2000 = init('EQ_J2000', true, EQUATORIAL_J, 2000 );
    const EQ_B2000 = init('EQ_B2000', true, EQUATORIAL_B, 2000);
    const EQ_B1950 = init('EQ_B1950', true, EQUATORIAL_B, 1950);
    const GALACTIC = init('GALACTIC', false, GALACTIC_JSYS, 2000);
    const SUPERGALACTIC = init('SUPERGALACTIC', false, SUPERGALACTIC_JSYS, 2000 );
    const ECL_J2000 = init('EC_J2000', false, ECLIPTIC_J, 2000);
    const ECL_B1950 = init('EC_B1950', false, ECLIPTIC_B, 1950);

    const PIXEL = init('PIXEL', false, NONCELESTIAL, 0);
    const SCREEN_PIXEL = init('SCREEN_PIXEL', false, NONCELESTIAL, 0);
    const ZEROBASED = init('ZERO-BASED', false, NONCELESTIAL, 0);
    const FITSPIXEL = init('FITSPIXEL', false, NONCELESTIAL, 0);

    const UNDEFINED = init('UNDEFINED', false, NONCELESTIAL, 0);

    const parse= (desc) => {
        if (!desc) return undefined;
        desc= desc.toUpperCase();
        if        (desc===EQ_J2000.toString() || desc==='EQJ2000' ||desc==='EQJ' || desc==='J2000' || desc==='ICRS') {
            return EQ_J2000;
        } else if (desc===EQ_B2000.toString() || desc==='EQB2000' )  {
            return EQ_B2000;
        } else if (desc===EQ_B1950.toString() || desc==='EQB1950' || desc==='EQB' || desc==='B1950' ) {
            return EQ_B1950;
        } else if (desc===GALACTIC.toString() || desc==='GAL') {
            return GALACTIC;
        } else if (desc===SUPERGALACTIC.toString()) {
            return SUPERGALACTIC;
        } else if (desc===ECL_J2000.toString() || desc==='ECJ2000' | desc==='ECLJ2000' || desc==='ECJ') {
            return ECL_J2000;
        } else if (desc===ECL_B1950.toString() || desc==='ECLB1950'|| desc==='ECB') {
            return ECL_B1950;
        } else if (desc===SCREEN_PIXEL.toString()) {
            return SCREEN_PIXEL;
        } else if (desc===PIXEL.toString()) {
            return PIXEL;
        } else if (desc===ZEROBASED.toString() || desc==='ZERO_BASED') {
            return ZEROBASED;
        } else if (desc===FITSPIXEL.toString()) {
            return FITSPIXEL;
        } else {
            return undefined;
        }
    };

    return {
        parse, EQ_J2000, EQ_B2000, EQ_B1950, GALACTIC, SUPERGALACTIC, ECL_J2000, ECL_B1950,
        PIXEL, ZEROBASED, FITSPIXEL, SCREEN_PIXEL, UNDEFINED
    };
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
