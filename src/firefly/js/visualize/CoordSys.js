/**
 * Created by roby on 12/2/14.
 */



const EQUATORIAL_J = 0;
const EQUATORIAL_B = 1;
const GALACTIC     = 2;
const ECLIPTIC_B   = 3;
const SUPERGALACTIC   = 4;
const ECLIPTIC_J   = 13;



var CoordinateSys = function () {


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
    var GALACTIC = init('GALACTIC', false, GALACTIC, 2000);
    var SUPERGALACTIC = init('SUPERGALACTIC', false, SUPERGALACTIC, 2000 );
    var ECL_J2000 = init('EC_J2000', false, ECLIPTIC_J, 2000);
    var ECL_B1950 = init('EC_B1950', false, ECLIPTIC_B, 2000);
    var PIXEL = init('PIXEL', false,-999, 0);
    var SCREEN_PIXEL = init('SCREEN_PIXEL', false,-999, 0);
    var UNDEFINED = init('UNDEFINED', false,-999, 0);


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
    retval.SCREEN_PIXEL = SCREEN_PIXEL;
    retval.UNDEFINED = UNDEFINED;


    return retval;
}();

export default CoordinateSys;

