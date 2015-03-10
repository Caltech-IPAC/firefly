/*jshint browserify:true*/

var numeral= require("numeral");
/**
 * Created by roby on 12/2/14.
 */
var Vis= (function(retVis) {
    "use strict";

    var hasModule = (typeof module !== 'undefined' && module.exports && exports);

    var CoordinateSys = function () {


        var init = function (value, equatorial) {
            var desc= value;
            return {
                toString: function () {
                    return desc;
                },
                isEquatorial: function () {
                    return equatorial ? equatorial : false;
                }
            };
        };

        var EQ_J2000 = init("EQ_J2000", true);
        var EQ_B2000 = init("EQ_B2000", true);
        var EQ_B1950 = init("EQ_B1950", true);
        var GALACTIC = init("GALACTIC", false);
        var SUPERGALACTIC = init("SUPERGALACTIC", false);
        var ECL_J2000 = init("EC_J2000", false);
        var ECL_B1950 = init("EC_B1950", false);
        var PIXEL = init("PIXEL", false);
        var SCREEN_PIXEL = init("SCREEN_PIXEL", false);
        var UNDEFINED = init("UNDEFINED", false);


        var parse= function(desc) {
            var coordSys;
            desc= desc.toUpperCase();
            if        (desc===EQ_J2000.toString() || desc==="EQJ" || desc==="J2000" ) {
                coordSys = EQ_J2000;
            } else if (desc===EQ_B2000.toString())  {
                coordSys = EQ_B2000;
            } else if (desc===EQ_B1950.toString() || desc==="EQB" || desc==="B1950" ) {
                coordSys = EQ_B1950;
            } else if (desc===GALACTIC.toString() || desc==="GAL") {
                coordSys = GALACTIC;
            } else if (desc===SUPERGALACTIC.toString()) {
                coordSys = SUPERGALACTIC;
            } else if (desc===ECL_J2000.toString() || desc==="ECJ") {
                coordSys = ECL_J2000;
            } else if (desc===ECL_B1950.toString() || desc==="ECB") {
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

    if (hasModule) {
        module.exports= CoordinateSys;
    }

    retVis.CoordinateSys= CoordinateSys;

    return retVis;

}(Vis || {}));
