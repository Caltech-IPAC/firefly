/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * This Object contains the specifications of the DS9 region
 */

import Enum from 'enum';
import {has, isNil, get, omit} from 'lodash';

export const RegionType = new Enum(['circle', 'annulus', 'ellipse', 'ellipseannulus', 'box',
                                    'boxannulus', 'line', 'point', 'polygon', 'text', 'message',
                                    'undefined', 'global'], {ignoreCase: true});

export const RegionCsys = new Enum(['PHYSICAL', 'FK4', 'B1950', 'FK5', 'J2000',
                                    'IMAGE', 'ECLIPTIC', 'GALACTIC',
                                    'ICRS', 'AMPLIFIER', 'LINEAR', 'DETECTOR', 'UNDEFINED'], {ignoreCase: true});

export const RegionValueUnit = new Enum(['CONTEXT', 'DEGREE', 'RADIAN', 'ARCMIN', 'ARCSEC',
                                         'SCREEN_PIXEL', 'IMAGE_PIXEL'], {ignoreCase: true});

export const RegionPointType = new Enum(['circle', 'box', 'cross', 'diamond', 'x',
                                         'arrow', 'boxcircle', 'undefined',
                                         'vector', 'ruler', 'compass', 'projection',
                                         'panda', 'epanda', 'bpanda', 'composite'], {ignoreCase: true});



var cloneArg = (arg) => Object.keys(arg).reduce((prev, key) =>
                        {
                            if (!isNil(arg[key])) {
                                prev[key] = arg[key];
                            }
                            return prev;
                        }, {});

/**
 * @summary make region object
 * @param {Object[]} wpAry WorldPt array, items contained: line, annulus, ellipse, box: 1, line: 2, polygon: >= 3
 * @param {Object[]} radiusAry RegionValue array, items contained: annulus: at least 2 radii.
 * @param {Object[]} dimensionAry RegionDimension array, each item contains w, h for box and box annulus
 *                                            or 2 radii for ellipse and ellipse annulus
 * @param {Object} angle RegionValue
 * @param {Object} type  RegionType
 * @param {Object} options   RegionOptions, properties of region
 * @param {boolean} highlighted bool
 * @param {string} message parsing message
 * @function makeRegion
 */
export var makeRegion = (
            {
                wpAry,       // for polygon, polygonCenter is added optionally based on VisUtil.computerCentralAndRadius
                radiusAry,
                dimensionAry,
                angle,
                type = RegionType.undefined,
                options,
                highlighted,
                desc,
                message}) => (
                cloneArg({
                    wpAry,
                    radiusAry,
                    dimensionAry,
                    angle,
                    type,
                    options,
                    highlighted,
                    desc,
                    message
                })
);


/**
 * @summary make object for region properties
 * @param {string} color 'white', 'black', 'red', 'green' (default), 'blue', 'cyan', 'magenta', 'yellow'
 * @param {string} text default: ''
 * @param {Object} font from makeRegionFont, default is from 'helvetica 10 normal normal'
 * @param {Object} pointType  RegionPointType,
 * @param {number} pointSize  default: 5
 * @param {boolean} editable  default: 1
 * @param {boolean} movable   default: 1
 * @param {boolean} rotatable default: 0
 * @param {boolean} highlightable default: 1
 * @param {boolean} deletable default: 1
 * @param {boolean} fixedSize default: 0
 * @param {boolean} include default: 1
 * @param {number} lineWidth default: 0
 * @param {string} dashlist default: "8 3"
 * @param {boolean} source  default: 1
 * @param {boolean} dashable default: 0
 * @param {number} offsetX default: 0
 * @param {number} offsetY default: 0
 * @param {string} message default: '', containing parsing (error) message
 * @param {string} tag default: ''
 * @param {string} coordSys default: 'PHYSICAL'
 * @function makeRegionOptions
 */

export var makeRegionOptions = (
            {
                color = 'green',
                text,
                font,
                pointType,
                pointSize,
                editable,
                movable,
                rotatable,
                highlightable,
                deletable,
                fixedSize,
                include,
                lineWidth,
                dashable,
                dashlist,
                source,
                offsetX,
                offsetY,
                message,
                tag,
                coordSys} )  => (
                cloneArg({color, text, font, pointType, pointSize,
                          editable, movable, rotatable, highlightable, deletable, fixedSize, include,
                          lineWidth, dashable, dashlist, source, offsetX, offsetY, message, tag, coordSys})
                );


export const regionPropsList = {
        COLOR:  'color',
        TEXT:   'text',
        FONT:   'font',
        PTTYPE: 'pointType',
        PTSIZE: 'pointSize',
        EDIT:   'editable',
        MOVE:   'movable',
        ROTATE: 'rotatable',
        HIGHLITE:'highlightable',
        SELECT:  'selectable',
        DELETE:  'deletable',
        DASH:    'dashable',
        DASHLIST:'dashlist',
        FIXED:   'fixedSize',
        INCLUDE: 'include',
        LNWIDTH: 'lineWidth',
        LINE:    'line',
        RULER:   'ruler',
        SOURCE:  'source',
        TAG:     'tag',
        OFFX:    'offsetX',
        OFFY:    'offsetY',
        TEXTLOC: 'textloc',
        COORD:   'coordSys',
        MSG:     'message'
};

export var defaultRegionProperty = {
    color: 'green',
    text:  '',
    font:  {name: 'helvetica', point: '10', weight: 'normal', slant: 'normal'},
    pointType: RegionPointType.cross,
    pointSize: 5,
    editable: 1,
    movable:  1,
    rotatable: 0,
    highlightable: 1,
    selectable: 1,
    deletable: 1,
    fixedSize: 0,
    dashable: 0,
    dashlist: '8 3',
    include: 1,
    lineWidth: 1,
    line: '1 1',
    ruler: 'arcsec',
    tag: '',
    source: 1,
    offsetX: 0,
    offsetY: 0,
    coordSys: 'PHYSICAL',
    textLoc: 'DEFAULT',

    message: ''
};

export var getRegionDefault = (prop) => (has(defaultRegionProperty, prop) ? defaultRegionProperty[prop] : null);
export var setRegionPropDefault = (prop, value) => {
            if (has(defaultRegionProperty, prop)) {
                defaultRegionProperty[prop] = value;
            }
           };


/**
 * @summary region value object
 * @param {number} value value for radius and dimension
 * @param {Object} unit  DEGREE, SCREEN_PIXEL and IMAGE_PIXEL
 * @function RegionValue
 */

export var RegionValue =
    (value = 0.0, unit = RegionValueUnit.CONTEXT) => ({value, unit});

/**
 * @summary region value object for dimension
 * @param {Object} width  RegionValue width of box or first radius of ellipse
 * @param {Object} height RegionValue height of box or second radius of ellipse
 * @function RegionDimension
 */
export var RegionDimension = (width, height) => ({width, height});

/**
 * regiopn parse exception
 */
export class RegParseException extends Error {
    constructor(message) {
        super(message);
        this.message = message;
        this.name = 'RegionParseError';
    }
}

export var makeRegionMsg = (msg) => makeRegion({type: RegionType.message, message: msg});

/**
 * @summary get region type
 * @param {string} rgTypeStr
 * @returns {Object} RegionType
 */
export var getRegionType = (rgTypeStr) => (RegionType.get(rgTypeStr) || RegionType.undefined);

/**
 * $summary get region point type
 * @param {string} typeStr
 * @returns {Object} RegionPointType
 */
export var getRegionPointType = (typeStr) => (RegionPointType.get(typeStr) || RegionPointType.undefined);

/**
 * @summary get coordinate system
 * @param {string} coordStr
 * @returns {Object} RegionCsys
 */
export var getRegionCoordSys = (coordStr) => (RegionCsys.get(coordStr) || RegionCsys.UNDEFINED);

/**
 * @summary get region font object
 * @param {string} name ex: Arieal, Courier, Ties, Helvetica, sans-serif, BRAGGADOCIO
 * @param {string} point font size,  ex: 10, 12, 14, 16
 * @param {string} weight ex: normal, bold
 * @param {string} slant ex: normal, italic
 * @returns {Object}
 */
export var makeRegionFont = (name = 'helvetica', point = '10', weight = 'normal', slant = 'normal') => (
        {name, point, weight, slant});

/**
 * @summary create region on point
 * @param {Object} worldPoint
 * @param {Object} options
 * @param {boolean} highlighted
 * @returns {Object}
 */
export function makeRegionPoint(worldPoint, options, highlighted) {
    return  makeRegion({type: RegionType.point, wpAry: [worldPoint],  options, highlighted});
}

/**
 * @summary create region on text
 * @param {Object} worldPoint
 * @param {Object} options
 * @param {boolean} highlighted
 * @returns {Object}
 */
export function makeRegionText(worldPoint, options, highlighted) {
    return makeRegion({type: RegionType.text, wpAry: [worldPoint], options, highlighted});
}

/**
 * @summary create region on box
 * @param {Object} worldPoint
 * @param {Object} angle RegionValue
 * @param {Object} dim   ReginDimension containing width and height
 * @param {Object} options
 * @param {boolean} highlighted
 * @returns {Object}
 */
export function makeRegionBox(worldPoint, dim, angle, options, highlighted) {
    return makeRegion({type: RegionType.box, wpAry: [worldPoint], dimensionAry: [dim], angle, options, highlighted});
}

/**
 * @summary create region on boxannulus
 * @param {Object} worldPoint
 * @param {Object} angle  RegionValue
 * @param {Object[]} dimAry array of RegionDimension containing width and height
 * @param {Object} options
 * @param {boolean} highlighted
 * @returns {Object}
 */
export function makeRegionBoxAnnulus(worldPoint, dimAry,  angle, options, highlighted) {
    return makeRegion({type: RegionType.boxannulus, wpAry: [worldPoint], dimensionAry: dimAry, angle, options, highlighted});
}

/**
 * @summary create region on annulus
 * @param {Object} worldPoint
 * @param {Object[]} radAry array of RegionValue (radius)
 * @param {Object} options
 * @param {boolean} highlighted
 * @returns {Object}
 */
export function makeRegionAnnulus(worldPoint, radAry,  options, highlighted) {
    return makeRegion({type: RegionType.annulus, wpAry: [worldPoint], radiusAry: radAry, options, highlighted});
}

/**
 * @summary create region on circle
 * @param {Object} worldPoint
 * @param {Object} radius  RegionValue
 * @param {Object} options
 * @param {boolean} highlighted
 * @returns {Object}
 */
export function makeRegionCircle(worldPoint, radius,  options, highlighted) {
    return makeRegion({type: RegionType.circle, wpAry: [worldPoint], radiusAry: [radius], options, highlighted});
}

/**
 * @summary create region on ellipse
 * @param {Object} worldPoint
 * @param {Object} angle  RegionValue
 * @param {Object} dim    RegionDimension containing radius 1 and radius 2
 * @param {Object} options
 * @param {boolean} highlighted
 * @returns {Object}
 */
export function makeRegionEllipse(worldPoint, dim, angle, options, highlighted) {
    return makeRegion({type: RegionType.ellipse, wpAry: [worldPoint],
                        dimensionAry: [dim], angle, options, highlighted });
}


/**
 * @summary create region on ellipseannulus
 * @param {Object} worldPoint
 * @param {Object} angle  RegionValue
 * @param {Object[]} dimAry array of RegionDimension containing radius 1 and radius  2
 * @param {Object} options
 * @param {boolean} highlighted
 * @returns {Object}
 */
export function makeRegionEllipseAnnulus(worldPoint, dimAry, angle, options, highlighted ) {
    return makeRegion({type: RegionType.ellipseannulus, wpAry: [worldPoint],
                    dimensionAry: dimAry, angle, options, highlighted});
}

/**
 * @summary create region on line
 * @param {Object} worldPoint1
 * @param {Object} worldPoint2
 * @param {Object} options
 * @param {boolean} highlight
 * @returns {Object}
 */
export function makeRegionLine(worldPoint1, worldPoint2, options, highlight ) {
    return makeRegion({ type: RegionType.line, wpAry: [worldPoint1, worldPoint2], options, highlight});
}

/**
 * @summary create region on polygon
 * @param {Object[]} wpAry array of WorldPt
 * @param {Object} options
 * @param {boolean} highlight
 * @returns {Object}
 */
export function makeRegionPolygon(wpAry, options, highlight) {
    return makeRegion({type: RegionType.polygon, wpAry, options, highlight});
}

