import {isString, isNumber, isNil, isObject, isArray} from 'lodash';
import CoordinateSys from './CoordSys.js';
import Resolver, {parseResolver} from '../astro/net/Resolver.js';
import CoordUtil from 'firefly/visualize/CoordUtil.js';
import {memorizeLastCall} from 'firefly/util/WebUtil.js';

const SPT= 'ScreenPt';
const IM_PT= 'ImagePt';
const FITS_IM_PT= 'FitsImagePt';
const ZERO_BASED_IM_PT= 'ZeroBasedImagePt';
const IM_WS_PT= 'ImageWorkSpacePt';
const W_PT= 'WorldPt';
const DEV_PT= 'DevicePt';
const PROJ_PT= 'ProjectionPt';
const OFFSET_PT= 'OffsetPt';

const Point = {  SPT, IM_PT, IM_WS_PT, FITS_IM_PT, ZERO_BASED_IM_PT, DEV_PT, PROJ_PT, W_PT, OFFSET_PT};

/**
 * @typedef {Object} Point
 * @summary a Point
 *
 * @prop {Number} x
 * @prop {Number} y
 * @prop {String} type one of 'RenderedPt', 'ScreenPt', 'ImagePt', 'ImageWorkSpacePt', 'WorldPt', 'ProjectionPt', 'OffsetPt'
 * @prop {CoordinateSys} [cSys] - the coordinate system constant, only used for world point
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
  * @typedef {Object} ImageWpt
  * @summary a point in image file coordinates
  * @prop {Number} x
  * @prop {Number} y
  * @prop {String} type constant must be 'ImageWpt'
  * @public
  * @global
  */

/**
 * @typedef {Object} FitsImagePt
 * @summary a point in FITS standard image file coordinates
 * @prop {Number} x
 * @prop {Number} y
 * @prop {String} type constant must be 'ImagePt'
 * @public
 * @global
 */

/**
 * @typedef {Object} ZeroBasedImagePt
 * @summary a point in Zero based image file coordinates, ca be offset by LTV1 and LTV2
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
 * @prop {Resolver} resolver - the resolver used to create this worldPt, may not be defined
 * @prop {function} getLon
 * @prop {function} getLat
 * @prop {function} getCoordSys
 *
 * @public
 * @global
 */



export class SimplePt {
    constructor(objOrX,y, forceToInt) {
        if (isArray(objOrX)) {
            forceToInt= y || forceToInt; // in  this case, forceToInt was passed in second position
            this.x= Number(objOrX[0]);
            this.y= Number(objOrX[1]);
        }
        else if (isObject(objOrX)) {
            forceToInt= y || forceToInt; // in  this case, forceToInt was passed in second position
            this.x= Number(objOrX.x);
            this.y= Number(objOrX.y);
        }
        else {
            this.x= toNum(objOrX);
            this.y= toNum(y);
        }
        if (forceToInt) {
            this.x= Math.trunc(this.x);
            this.y= Math.trunc(this.y);
        }
    }
    toString() { return this.x+';'+this.y; }

    static make(x, y, type) {
        const pt = new SimplePt(x, y);

        if (type && Object.keys(Point).includes(type)) {
            pt.type = type;
        }
        return pt;
    }
}


/**
 * WorldPt constructor
 * @type {WorldPt}
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
class WorldPtObj {
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


    getResolver() { return this.resolver ? this.resolver : undefined; }

    getObjName() { return (this.objName) ? this.objName : undefined; }


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
}


/**
 *
 * @param {Array.<String>} wpParts
 * @return {*}
 */
function stringAryToWorldPt(wpParts) {
    const len= wpParts.length;
    const x= Number(wpParts[0]);
    const y= Number(wpParts[1]);
    if  (isNaN(x) || isNaN(y)) return undefined;

    const csys= CoordinateSys.parse(wpParts[2]);

    if (csys===CoordinateSys.PIXEL)        return makeImagePt(x, y); // image point
    if (csys===CoordinateSys.SCREEN_PIXEL) return makeScreenPt(x, y); // screen point

    if (len===2 || len===3) return makeWorldPt(x,y,csys);

    const resolver= wpParts[4] ? parseResolver(wpParts[4]) : Resolver.UNKNOWN;
    return makeResolvedWorldPt(makeWorldPt(x,y,csys), wpParts[3], resolver);
}

const toDeg= (angle) => angle * (180 / Math.PI);

/**
 * return a number give a number or a string
 * @param {number|string|undefined|null} v
 * @return {number} converted to a number
 */
function toNum(v) {
    if (isNil(v)) return NaN;
    if (isString(v) && !v) return NaN;
    return Number(v);
}

/**
 * @summary A point on the sky with a coordinate system
 * @param {number|string} lon - longitude in degrees, strings are converted to numbers
 * @param {number|string} lat - latitude in degrees, strings are converted to numbers
 * @param {CoordinateSys} [coordSys] - The coordinate system of this worldPt
 * @param {boolean} [detectHMS] - if lon and lat are in sexagesimal, convert to number
 *                              coordinate system must equatorial
 * @param {boolean} [angleInRadian] - if true then convert to degrees
 * @return {WorldPt}
 *
 * @Function makeWorldPt
 * @public
 * @global
 */
export const makeWorldPt=
    memorizeLastCall((lon, lat, coordSys= CoordinateSys.EQ_J2000, detectHMS= false, angleInRadian= false) =>{
        let lonNum=toNum(lon);
        let latNum=toNum(lat);
        if (isNaN(lonNum) || isNaN(latNum)) {
            if (!detectHMS) return undefined;
            if (isString(lon) && isString(lat) && coordSys.isEquatorial()) {
                lonNum= CoordUtil.convertStringToLon(lon,CoordinateSys.EQ_J2000);
                latNum= CoordUtil.convertStringToLat(lat,CoordinateSys.EQ_J2000);
            }
            if (isNaN(lonNum) || isNaN(latNum)) return undefined;
        }
        return new WorldPtObj( angleInRadian ? toDeg(lonNum): lonNum, angleInRadian ? toDeg(latNum): latNum, coordSys);
    });

/**
 * Create a new world point with the object name and resolver defined
 * @param {WorldPt} wp
 * @param {String} objName -  object name used to create this worldPt
 * @param [resolver] - the resolver used to create this worldPt
 * @return {WorldPt}
 */
export function makeResolvedWorldPt(wp,objName,resolver=Resolver.UNKNOWN) {
    return new WorldPtObj(wp.x,wp.y,wp.cSys,objName,resolver);
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
export const makeImagePt= (x,y) => Object.assign(new SimplePt(x,y), {type:IM_PT});

/**
 * @summary A image point in a FITS file with FITS based offset Standard
 * @param {number|string} x - the x, string is converted to number
 * @param {number|string} y - the y, string is converted to number
 *
 * @return {FitsImagePt}
 *
 * @function makeFitsImagePt
 * @memberof firefly.util.image
 * @public
 * @global
 */
export const makeFitsImagePt= (x,y) => Object.assign(new SimplePt(x,y), {type:FITS_IM_PT});

/**
 * @summary A image point in a Zero based standard, can be offset with the LVT1 and LTV2 headers
 * @param {number|string} x - the x, string is converted to number
 * @param {number|string} y - the y, string is converted to number
 *
 * @return {ImagePt}
 *
 * @function makeZeroBasedImagePt
 * @memberof firefly.util.image
 * @public
 * @global
 */
export const makeZeroBasedImagePt= (x,y) => Object.assign(new SimplePt(x,y), {type:ZERO_BASED_IM_PT});

/**
 *
 * @param {number|string} x - the x, string is converted to number
 * @param {number|string} y - the y, string is converted to number
 * @memberof firefly.util.image
 * @return {Point}
 */
export const makeImageWorkSpacePt= (x,y) => Object.assign(new SimplePt(x,y), {type:IM_WS_PT});

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
export const makeScreenPt= (x,y) => Object.assign(new SimplePt(x,y), {type:SPT});

/**
 * @summary A point on the physical space of the image display area
 * @param {number|string} x - the x, string is converted to number
 * @param {number|string} y - the y, string is converted to number
 *
 * @return {DevicePt}
 *
 * @function makeDevicePt
 * @memberof firefly.util.image
 * @public
 * @global
 */
export const makeDevicePt= (x,y) => Object.assign(new SimplePt(x,y), {type:DEV_PT});

export const makeProjectionPt= (x,y) => Object.assign(new SimplePt(x,y), {type:PROJ_PT});

export const makeOffsetPt= (x,y) => Object.assign(new SimplePt(x,y), {type:OFFSET_PT});


/**
 * given an x,y, and a CoordinateSys object or string, make the correct type of point.
 * @param {number} x
 * @param {number} y
 * @param {CoordinateSys|String} coordSys
 * @return {Point}
 */
export function makeAnyPt(x,y,coordSys) {
    const csys= isString(coordSys) ? CoordinateSys.parse(coordSys) : coordSys;
    switch (csys) {
        case CoordinateSys.SCREEN_PIXEL:
        case CoordinateSys.UNDEFINED:    return makeScreenPt(x, y);
        case CoordinateSys.PIXEL:        return makeImagePt(x, y);
        case CoordinateSys.ZEROBASED:    return makeZeroBasedImagePt(x, y);
        case CoordinateSys.FITSPIXEL:    return makeFitsImagePt(x, y);
        default:                         return makeWorldPt(x,y,coordSys,true);
    }
}

/**
 * given serialized point object return the appropriate point.
 * @param inStr
 * @return {Point|WorldPt|ScreenPt|ImagePt|ZeroBasedImagePt|undefined}
 */
export function parseAnyPt(inStr) {
    const wp= parseWorldPt(inStr);
    if (!wp) return;
    const {cSys}= wp;
    return (cSys===CoordinateSys.SCREEN_PIXEL || cSys===CoordinateSys.UNDEFINED || cSys===CoordinateSys.PIXEL ||
            cSys===CoordinateSys.ZEROBASED || cSys===CoordinateSys.FITSPIXEL) ? makeAnyPt(wp.x,wp.y,wp.cSys) : wp;
}

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
    return (p1.x===p2.x && p1.y===p2.y && p1.type===p2.type && p1.cSys===p2.cSys);
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
    if (!inStr) return undefined;
    const parts= inStr.split(';');
    const p0Num= Number(parts[0]);
    const p1Num= Number(parts[1]);
    if (parts.length===2 && !isNaN(p0Num) && !isNaN(p1Num)) {
        const pt= new SimplePt(p0Num, p1Num);
        pt.type= type;
        return pt;
    }
    return undefined;
};

export const parseImagePt = (inStr) => parsePt(IM_PT,inStr);
export const parseImageWorkSpacePt = (inStr) => parsePt(IM_WS_PT,inStr);
export const parseScreenPt= (inStr) => parsePt(SPT,inStr);


/**
 *
 * @param {String} serializedWP
 * @return {WorldPt}
 */
export const parseWorldPt = function (serializedWP) {
    if (isValidPoint(serializedWP)) return serializedWP;
    if (!serializedWP || !isString(serializedWP)) return undefined;

    const sAry= serializedWP.split(';');
    if (sAry.length<2 || sAry.length>5) return undefined;
    return stringAryToWorldPt(sAry);
};

const ptTypes= Object.values(Point);

/**
 * Make sure this is a defined value, valid point object, has a valid type, and the coordinates are numbers.
 * @param {*} testPt - any value
 * @return {boolean} true if this point is valid
 */
export const isValidPoint= (testPt) =>  Boolean(testPt?.type && ptTypes.includes(testPt?.type) &&
                                                isNumber(testPt?.x) && isNumber(testPt?.y));

export default Point;
