
import {isString, isNil} from 'lodash';
import CoordinateSys from './CoordSys.js';
import Resolver, {parseResolver} from '../astro/net/Resolver.js';
import validator from 'validator';

const SPT= 'ScreenPt';
const IM_PT= 'ImagePt';
const IM_WS_PT= 'ImageWorkSpacePt';
const W_PT= 'WorldPt';
const DEV_PT= 'DevicePt';
const PROJ_PT= 'ProjectionPt';
const OFFSET_PT= 'OffsetPt';

const Point = {  SPT, IM_PT, IM_WS_PT, DEV_PT, PROJ_PT, W_PT, OFFSET_PT};



/**
 * @typedef {Object} Point
 * @summary a Point
 *
 * @prop {Number} x
 * @prop {Number} y
 * @prop {String} type one of 'RenderedPt', 'ScreenPt', 'ImagePt', 'ImageWorkSpacePt', 'WorldPt', 'ViewPortPt', 'ProjectionPt', 'OffsetPt'
 * @public
 * @global
 */

/**
 * @typedef {Object} DevicePt
 * @summary a rendered point on the device screen, including rotation, flipping, etc
 * @prop {Number} x
 * @prop {Number} y
 * @prop {String} type constant must be 'DevicePt'
 * @public
 * @global
 */

/**
 * @typedef {Object} ScreenPt
 * @summary a point on the image screen
 * @prop {Number} x
 * @prop {Number} y
 * @prop {String} type constant must be 'ScreenPt'
 * @public
 * @global
 */

/**
 * @typedef {Object} ImagePt
 * @summary a point in image file coordinates
 * @prop {Number} x
 * @prop {Number} y
 * @prop {String} type constant must be 'ImagePt'
 * @public
 * @global
 */

/**
 * @typedef {Object} WorldPt
 * @summary a point on the sky
 * @prop {Number} x
 * @prop {Number} y
 * @prop {String} type constant must be 'WorldPt'
 * @prop {CoordinateSys} cSys - the coordinate system constant
 * @prop {String} objName - the object name the was used for name resolution, may not be defined
 * @prop {Resolver} objName - the resolver used to create this worldPt, may not be defined
 *
 * @public
 * @global
 */



export class SimplePt {
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
 * @public
 * @global
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
        let retval = this.x + ';' + this.y + ';' + this.cSys.toString();
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
    let retval= null;
    let parsedLon;
    let parseLat;
    let parsedCoordSys;
    if (wpParts.length===3) {
        parsedLon= Number(wpParts[0]);
        parseLat= Number(wpParts[1]);
        parsedCoordSys= CoordinateSys.parse(wpParts[2]);
        if (!isNaN(parsedLon) && !isNaN(parseLat) && parsedCoordSys!==null) {
            retval= makeWorldPt(parsedLon,parseLat,parsedCoordSys);
        }
    }
    else if (wpParts.length===2) {
        parsedLon= Number(wpParts[0]);
        parseLat= Number(wpParts[1]);
        if (!isNaN(parsedLon) && !isNaN(parseLat)) {
            retval= makeWorldPt(parsedLon,parseLat);
        }
    }
    else if (wpParts.length===5 || wpParts.length===4) {
        parsedLon= Number(wpParts[0]);
        parseLat= Number(wpParts[1]);
        parsedCoordSys= CoordinateSys.parse(wpParts[2]);
        const resolver= wpParts.length===5 ? parseResolver(wpParts[4]) : Resolver.UNKNOWN;
        return makeWorldPt(parsedLon,parseLat,parsedCoordSys, wpParts[3],resolver);
    }
    return retval;
}

/**
 * @summary A point on the sky with a coordinate system
 * @param {number|string} lon - longitude in degrees, strings are converted to numbers
 * @param {number|string} lat - latitude in degrees, strings are converted to numbers
 * @param {CoordinateSys} coordSys - The coordinate system of this worldPt
 *
 * @param {String} [objName] -  object name used to create this worldPt
 * @param resolver - the resolver used to create this worldPt
 * @return {WorldPt}
 *
 *
 * @function makeWorldPt
 * @public
 * @global
 */
export function makeWorldPt(lon,lat,coordSys,objName,resolver) {
    return new WorldPt(Number(lon),Number(lat),coordSys,objName,resolver) ;
}


/**
 * @summary A point in the image file
 * @param {number|string} x - the x, string is converted to number
 * @param {number|string} y - the y, string is converted to number
 *
 * @return {ImagePt}
 *
 * @function makeImagePt
 * @memberof firefly.util.image
 * @public
 * @global
 */
export const makeImagePt= (x,y) => Object.assign(new SimplePt(Number(x),Number(y)), {type:IM_PT});


/**
 *
 * @param {number|string} x - the x, string is converted to number
 * @param {number|string} y - the y, string is converted to number
 * @memberof firefly.util.image
 * @return {Pt}
 */
export const makeImageWorkSpacePt= (x,y) => Object.assign(new SimplePt(Number(x),Number(y)), {type:IM_WS_PT});




/**
 * @summary A point of the display image
 * @param {number|string} x - the x, string is converted to number
 * @param {number|string} y - the y, string is converted to number
 *
 * @return {ScreenPt}
 *
 * @function makeScreenPt
 * @memberof firefly.util.image
 * @public
 * @global
 */
export const makeScreenPt= (x,y) => Object.assign(new SimplePt(Number(x),Number(y)), {type:SPT});

export const makeDevicePt= (x,y) => Object.assign(new SimplePt(Number(x),Number(y)), {type:DEV_PT});

export const makeProjectionPt= (x,y) => Object.assign(new SimplePt(Number(x),Number(y)), {type:PROJ_PT});

export const makeOffsetPt= (x,y) => Object.assign(new SimplePt(Number(x),Number(y)), {type:OFFSET_PT});


/**
 * @summary Test if two points are equals.  They must be the same coordinate system and have the same values to be
 * equal. Two points that are null or undefined are also considered equal.
 * If both points are WorldPt and are equal in values and coordinate system but have a
 * different resolver and object names, * they are still considered equal.
 *
 * @param {Point} p1 - the first point
 * @param {Point} p2 - the second point
 *
 * @return  true if equals
 *
 * @function pointEquals
 * @memberof firefly.util.image
 * @public
 * @global
 */
export function pointEquals(p1,p2)  {
    if (isNil(p1) && isNil(p2)) return true;
    else if (isNil(p1) || isNil(p2)) return false;
    return (p1.x===p2.x && p1.y===p2.y && p1.type===p2.type && p1.csys===p2.csys);
}


/**
 * @summary Parse a point
 * @param type
 * @param inStr
 * @return {Point}
 * @memberof firefly.util.image
 * @public
 * @global
 */
export const parsePt= function(type, inStr) {
    if (!inStr) return null;
    const parts= inStr.split(';');
    if (parts.length===2 && validator.isFloat(parts[0]) && validator.isFloat(parts[1])) {
        const pt= new SimplePt(validator.toFloat(parts[0]), validator.toFloat(parts[1]));
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
    if (!serializedWP || !isString(serializedWP)) return null;

    const sAry= serializedWP.split(';');
    if (sAry.length<2 || sAry.length>5) return null;
    return stringAryToWorldPt(sAry);
};

const ptTypes= Object.values(Point);

export const isValidPoint= (testPt) =>  (testPt && testPt.type && ptTypes.includes(testPt.type));

export default Point;
