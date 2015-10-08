/*jshint browserify:true*/


function toString() {
    return this.key;
}





var Resolver=  {
    NED           : {key : 'ned', desc : 'NED'},
    Simbad        : {key : 'simbad', desc : 'simbad'},
    NedThenSimbad : {key : 'nedthensimbad', desc : 'try ned then simbad' },
    SimbadThenNed : {key : 'simbadthenned', value : 'try simbad then ned'},
    PTF           : {key : 'ptf', desc : 'ptf'},
    smart         : {key : 'smart', desc  : 'try ned and simbad, then decide'},
    UNKNOWN       : {key : 'unknown', desc : 'resolved with unknown resolver'},
    NONE          : {key : 'none', desc : 'none'},
};



export var parseResolver= function(resolveStr) {

    if (!resolveStr) return null;
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



Resolver.NedThenSimbad.combination=  [Resolver.NED, Resolver.Simbad];
Resolver.SimbadThenNed.combination=  [Resolver.Simbad, Resolver.NED];

Resolver.NED.toString= toString;
Resolver.Simbad.toString= toString;
Resolver.NedThenSimbad.toString= toString;
Resolver.SimbadThenNed.toString= toString;
Resolver.PTF.toString= toString;
Resolver.smart.toString= toString;
Resolver.UNKNOWN.toString= toString;
Resolver.NONE.toString= toString;


export default Resolver;

