/*jshint browserify:true*/

/**
 * Pt module.
 * @module firefly/visualize/Point.js
 */

/**
 * Created by roby on 12/2/14.
 */
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";

var CoordinateSys= require("./CoordSys.js");
import Resolver from "ipac-firefly/astro/net/Resolver.js";
import validator from "validator";



//var makePt = function (x, y) {
//    var retval= {};
//    retval.getX = function () { return x; };
//    retval.getY = function () { return y; };
//    retval.toString= function() {
//        return x+";"+y;
//    };
//    return retval;
//};

class Pt {
    constructor(x,y) {
        this.x= x;
        this.y= y;
    }
    toString() { return this.x+";"+this.y; }

    static parse(inStr) {
        if (!inStr) return null;
        var parts= inStr.split(";");
        if (parts.length===2 && validator.isFloat(parts[0]) && validator.isFloat(parts[1])) {
            return new Pt(validator.isFloat(parts[0]), validator.isFloat(parts[1]));
        }
        return null;
    }
}

class ImagePt extends Pt {
    constructor(x,y) {
        super(x,y);
    }
    static parse(inStr) {
        var p= Pt.parse(inStr);
        return p ? new ImagePt(p.x,p.y) : null;
    }
}


class ImageWorkSpacePt extends Pt {
    constructor(x,y) {
        super(x,y);
    }
    static parse(inStr) {
        var p= Pt.parse(inStr);
        return p ? new ImageWorkSpacePt(p.x,p.y) : null;
    }
}



    //var makeWorldPt = function (params) {
    //    var cSys = params.coordinateSys ? params.coordinateSys : CoordinateSys.EQ_J2000;
    //    var pt= makePt(params.lon, params.lat);
    //    pt.getLon = pt.getX;
    //    pt.getLat = pt.getY;
    //
    //    if (params.resolver) {
    //        pt._resolver = params.resolver;
    //    }
    //
    //    pt.getCoordSys = function () {
    //        return cSys;
    //    };
    //
    //
    //    pt.toString= function() {
    //        return pt.getX()+";"+pt.getY()+";"+cSys.toString();
    //    };
    //
    //    pt.serialize= pt.toString;
    //    return pt;
    //
    //};

var makeWorldPt = function (params) {
    return new WorldPt(params.params.lon,params.lat);
};

    /**
     * WorldPt constructor
     * @type {Function}
     * @constructor
     * @alias module:firefly/visualize/Pt.WorldPt
     * @param {number} lon - the longitude
     * @param {number} lat - the latitude
     * @param {CoordinateSys} [coordSys=CoordinateSys.EQ_J2000]- the coordinate system constant
     * @param {string} [objName] - the object name the was used for name resolution
     * @param {Resolver} [resolver] - the resolver use to return this point
     */
    class WorldPt extends Pt {
        constructor(lon,lat,coordSys,objName,resolver) {
            super(lon,lat);

            this.cSys = coordSys || CoordinateSys.EQ_J2000;
            if (objName) {
                this.objName = objName;
            }
            if (resolver) {
                this.resolver = resolver;
            }

        }
        /**
         * Return the lon
         * @type {function(this:exports.WorldPt)}
         * @return {Number}
         */
        getLon() { return this.x; }

        /**
         * Return the lat
         * @type {function(this:exports.WorldPt)}
         * @return {Number}
         */
        getLat() { return this.y; }

        /**
         * Returns the coordinate system of this point
         * @type {function(this:exports.WorldPt)}
         * @returns {CoordinateSys}
         */
        getCoordSys() { return this.cSys; }


        getResolver() { return this.resolver ? this.resolver : null; }

        getObjName() { return (this.objName) ? this.objName : null; }


        /**
         * return the string representation of the WorldPt. This output can be used
         * to recreate a WorldPt using parseWorldPt
         * @see {exports.parseWorldPt}
         * @type {function(this:exports.WorldPt)}
         * @return {string}
         */
        toString() {
            var retval = this.x + ";" + this.y + ";" + this.cSys.toString();
            if (this.objName) {
                retval += ";" + this.objName;
                if (this.resolver) {
                    retval += ";" + this.resolver.key;
                }
            }
            return retval;
        }

    }

    var parseWorldPt = function (serializedWP) {

        function stringAryToWorldPt(wpParts) {
            var retval= null;
            var parsedLon;
            var parseLat;
            var parsedCoordSys;
            if (sAry.length===3) {
                parsedLon= wpParts[0];
                parseLat= wpParts[1];
                parsedCoordSys= CoordinateSys.parse(wpParts[2]) ;
                if (!isNaN(parsedLon) && !isNaN(parseLat) && parsedCoordSys!==null) {
                    retval= new WorldPt(parsedLon,parseLat,parsedCoordSys);
                }
            }
            else if (wpParts.length===2) {
                parsedLon= wpParts[0];
                parseLat= wpParts[1];
                if (!isNaN(parsedLon) && !isNaN(parseLat)) {
                    retval= new WorldPt(parsedLon,parseLat);
                }
            }
            else  if (sAry.length===5 || sAry.length===4)  {
                parsedLon= wpParts[0];
                parseLat= wpParts[1];
                parsedCoordSys= CoordinateSys.parse(wpParts[2]) ;
                var resolver= sAry.length===5 ? Resolver.parse(sAry[4]) : Resolver.UNKNOWN;
                return new WorldPt(parsedLon,parseLat,parsedCoordSys, sAry[3],resolver);
            }
            return retval;

        }

        if (!serializedWP) {
            return null;
        }
        var sAry= serializedWP.split(";");
        if (sAry.length<2 || sAry.length>5) {
            return null;
        }
        return stringAryToWorldPt(sAry);
    };



exports.WorldPt= WorldPt;
exports.ImagePt= ImagePt;
exports.ImageWorkSpacePt= ImageWorkSpacePt;
exports.Pt= Pt;
exports.parseWorldPt= parseWorldPt;
    //exports.makePt= makePt;

