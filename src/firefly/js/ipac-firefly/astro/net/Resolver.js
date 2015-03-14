/*jshint browserify:true*/

var Resolver=  {
    NED           : {key : "ned", desc : "NED"},
    Simbad        : {key : "simbad", desc : "simbad"},
    NedThenSimbad : {key : "nedthensimbad", desc : "try ned then simbad" },
    SimbadThenNed : {key : "simbadthenned", value : "try simbad then ned"},
    PTF           : {key : "ptf", desc : "ptf"},
    smart         : {key : "smart", desc  : "try ned and simbad, then decide"},
    UNKNOWN       : {key : "unknown", desc : "resolved with unknown resolver"},
    NONE          : {key : "none", desc : "none"}
};

Resolver.NedThenSimbad.combination=  [Resolver.NED, Resolver.Simbad];
Resolver.SimbadThenNed.combination=  [Resolver.Simbad, Resolver.NED];

var parse= function(resolveStr) {
    "use strict";

    var retval= null;
    for( var resolveType in Resolver) {
        if( Resolver.hasOwnProperty( resolveType ) ) {
            if (resolveStr.toLowerCase()===Resolver[resolveType].key) {
                retval= Resolver[resolveType];
                break;
            }
        }
    }
    return retval;
};

exports.NED= Resolver.NED;
exports.simbad= Resolver.simbad;
exports.nedthensimbad= Resolver.nedthensimbad;
exports.simbadthenned= Resolver.simbadthenned;
exports.ptf= Resolver.ptf;
exports.smart= Resolver.smart;
exports.unknown= Resolver.unknown;
exports.none= Resolver.none;
exports.parse= parse;
