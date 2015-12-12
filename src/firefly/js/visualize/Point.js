/**
 * Pt module.
 * @module firefly/visualize/Point.js
 */

/**
 * Created by roby on 12/2/14.
 */


import CoordinateSys from './CoordSys.js';
import Resolver, {parseResolver} from '../astro/net/Resolver.js';
import validator from 'validator';

const SPT= 'ScreenPt';
const IM_PT= 'ImagePt';
const IM_WS_PT= 'ImageWorkSpacePt';
const W_PT= 'WorldPt';
const VP_PT= 'ViewPortPt';
const PROJ_PT= 'ProjectionPt';

var Point = {  SPT, IM_PT, IM_WS_PT, VP_PT, PROJ_PT, W_PT};

var ptTypes= Object.values(Point);

//var makePt = function (x, y) {
//    var retval= {};
//    retval.getX = function () { return x; };
//    retval.getY = function () { return y; };
//    retval.toString= function() {
//        return x+";"+y;
//    };
//    return retval;
//};

export class Pt {
    constructor(x,y) {
        this.x= x;
        this.y= y;
    }
    toString() { return this.x+';'+this.y; }
}


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
export class WorldPt {
    constructor(lon,lat,coordSys,objName,resolver) {
        this.x= lon;
        this.y= lat;
        this.cSys = coordSys || CoordinateSys.EQ_J2000;
        if (objName) {
            this.objName = objName;
        }
        if (resolver) {
            this.resolver = resolver;
        }
        this.type= W_PT;

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
     * @type {function(this:exports.WorldPt)}
     * @return {string}
     */
    toString() {
        var retval = this.x + ';' + this.y + ';' + this.cSys.toString();
        if (this.objName) {
            retval += ';' + this.objName;
            if (this.resolver) {
                retval += ';' + this.resolver.key;
            }
        }
        return retval;
    }

    static parse(inStr) {
        return parseWorldPt(inStr);
    }
}


function stringAryToWorldPt(wpParts) {
    var retval= null;
    var parsedLon;
    var parseLat;
    var parsedCoordSys;
    if (wpParts.length===3) {
        parsedLon= wpParts[0];
        parseLat= wpParts[1];
        parsedCoordSys= CoordinateSys.parse(wpParts[2]);
        if (!isNaN(parsedLon) && !isNaN(parseLat) && parsedCoordSys!==null) {
            retval= makeWorldPt(parsedLon,parseLat,parsedCoordSys);
        }
    }
    else if (wpParts.length===2) {
        parsedLon= wpParts[0];
        parseLat= wpParts[1];
        if (!isNaN(parsedLon) && !isNaN(parseLat)) {
            retval= makeWorldPt(parsedLon,parseLat);
        }
    }
    else if (wpParts.length===5 || wpParts.length===4) {
        parsedLon= wpParts[0];
        parseLat= wpParts[1];
        parsedCoordSys= CoordinateSys.parse(wpParts[2]);
        var resolver= wpParts.length===5 ? parseResolver(wpParts[4]) : Resolver.UNKNOWN;
        return makeWorldPt(parsedLon,parseLat,parsedCoordSys, wpParts[3],resolver);
    }
    return retval;
}

export const makeWorldPt= function (lon,lat,coordSys,objName,resolver) {
   return new WorldPt(lon,lat,coordSys,objName,resolver) ;
};


export const makeImagePt= function(x,y) {
    return Object.assign(new Pt(x,y), {type:IM_PT});
};

export const makeImageWorkSpacePt= function(x,y) {
    return Object.assign(new Pt(x,y), {type:IM_WS_PT});
};
export const makeScreenPt= function(x,y) {
    return Object.assign(new Pt(x,y), {type:SPT});
};
export const makeViewPortPt= function(x,y) {
    return Object.assign(new Pt(x,y), {type:VP_PT});
};
export const makeProjectionPt= function(x,y) {
    return Object.assign(new Pt(x,y), {type:PROJ_PT});
};

export const pointEquals= function(p1,p2)  {
    return (p1.x===p2.x && p1.y===p2.y && p1.type===p2.type && p1.csys===p2.csys);
};


export const parsePt= function(type, inStr) {
    if (!inStr) return null;
    var parts= inStr.split(';');
    if (parts.length===2 && validator.isFloat(parts[0]) && validator.isFloat(parts[1])) {
        var pt= new Pt(validator.toFloat(parts[0]), validator.toFloat(parts[1]));
        pt.type= type;
        return pt;
    }
    return null;
};

export const parseImagePt = (inStr) => parsePt(IM_PT,inStr);
export const parseImageWorkSpacePt = (inStr) => parsePt(IM_WS_PT,inStr);
export const parseScreenPt= (inStr) => parsePt(SPT,inStr);



/**
 *
 * @param serializedWP
 * @return {WorldPt}
 */
export const parseWorldPt = function (serializedWP) {
    if (!serializedWP) return null;

    var sAry= serializedWP.split(';');
    if (sAry.length<2 || sAry.length>5) {
        return null;
    }
    return stringAryToWorldPt(sAry);
};

export const isValidPoint= (testPt) =>  (testPt && testPt.type && ptTypes.includes(testPt.type));

export default Point;
