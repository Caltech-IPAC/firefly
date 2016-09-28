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
 *
 * @param wpAry WorldPt array, items contained: line, annulus, ellipse, box: 1, line: 2, polygon: >= 3
 * @param radiusAry RegionValue array, items contained: annulus: at least 2 radii.
 * @param dimensionAry RegionDimension array, each item contains w, h for box and box annulus
 *                                            or 2 radii for ellipse and ellipse annulus
 * @param angle RegionValue
 * @param type  RegionType
 * @param options   RegionOptions, properties of region
 * @param highlighted bool
 * @param message parsing message
 * @constructor
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
 *
 * @param color         string, 'white', 'black', 'red', 'green', 'blue', 'cyan', 'magenta', 'yellow'
 *                      default: 'green'
 * @param text          string,  default: ''
 * @param font          makeRegionFont,  default: 'helvetica 10 normal normal'
 * @param pointType     RegionPointType, default:
 * @param pointSize     number   default: 5
 * @param editable      bool     default: 1
 * @param movable       bool     default: 1
 * @param rotatable     bool     default: 0
 * @param highlightable bool     default: 1
 * @param deletable     bool     default: 1
 * @param fixedSize     bool     default: 0
 * @param include       bool     default: 1
 * @param lineWidth     number   default: 0
 * @param dashlist      string   default: "8 3"
 * @param source        bool     default: 1
 * @param dashable      bool     default: 0
 * @param offsetX       number   default: 0
 * @param offsetY       number   default: 0
 * @param message       string,  default: '', containing parsing (error) message
 * @param tag           string,  default: ''
 * @param coordSys      string
 * @constructor
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
    coordSys: 'J2000',
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
 *
 * @param value value for radius and dimension
 * @param unit  DEGREE, SCREEN_PIXEL and IMAGE_PIXEL
 * @constructor
 */

export var RegionValue =
    (value = 0.0, unit = RegionValueUnit.CONTEXT) => ({value, unit});

/**
 *
 * @param width  RegionValue width of box or first radius of ellipse
 * @param height RegionValue height of box or second radius of ellipse
 * @constructor
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
 *
 * @param rgTypeStr
 * @returns {RegionType}
 */
export var getRegionType = (rgTypeStr) => (RegionType.get(rgTypeStr) || RegionType.undefined);

/**
 *
 * @param typeStr
 * @returns {RegionPointType}
 */
export var getRegionPointType = (typeStr) => (RegionPointType.get(typeStr) || RegionPointType.undefined);

/**
 * parse font string set in region property
 * @param coordStr
 * @returns {RegionCsys}
 */
export var getRegionCoordSys = (coordStr) => (RegionCsys.get(coordStr) || RegionCsys.UNDEFINED);

/**
 *
 * @param name string - ex: Arieal, Courier, Ties, Helvetica, sans-serif, BRAGGADOCIO
 * @param point string (font size) ex: 10, 12, 14, 16
 * @param weight string ex: normal, bold
 * @param slant string ex: normal, italic
 */
export var makeRegionFont = (name = 'helvetica', point = '10', weight = 'normal', slant = 'normal') => (
        {name, point, weight, slant});

/**
 * create region on point
 * @param worldPoint
 * @param options
 * @param highlighted
 */
export function makeRegionPoint(worldPoint, options, highlighted) {
    return  makeRegion({type: RegionType.point, wpAry: [worldPoint],  options, highlighted});
}

/**
 * create region on text
 * @param worldPoint
 * @param options
 * @param highlighted
 */
export function makeRegionText(worldPoint, options, highlighted) {
    return makeRegion({type: RegionType.text, wpAry: [worldPoint], options, highlighted});
}

/**
 * create region on box
 * @param worldPoint
 * @param angle RegionValue
 * @param dim   ReginDimension
 * @param options
 * @param highlighted
 */
export function makeRegionBox(worldPoint, dim, angle, options, highlighted) {
    return makeRegion({type: RegionType.box, wpAry: [worldPoint], dimensionAry: [dim], angle, options, highlighted});
}

/**
 * create region on boxannulus
 * @param worldPoint
 * @param angle  RegionValue
 * @param dimAry array of RegionDimension, [w, h]
 * @param options
 * @param highlighted
 *
 */
export function makeRegionBoxAnnulus(worldPoint, dimAry,  angle, options, highlighted) {
    return makeRegion({type: RegionType.boxannulus, wpAry: [worldPoint], dimensionAry: dimAry, angle, options, highlighted});
}

/**
 * create region on annulus
 * @param worldPoint
 * @param radAry array of RegionValue (radius)
 * @param options
 * @param highlighted
 */
export function makeRegionAnnulus(worldPoint, radAry,  options, highlighted) {
    return makeRegion({type: RegionType.annulus, wpAry: [worldPoint], radiusAry: radAry, options, highlighted});
}

/**
 * create region on circle
 * @param worldPoint
 * @param radius  RegionValue
 * @param options
 * @param highlighted
 */
export function makeRegionCircle(worldPoint, radius,  options, highlighted) {
    return makeRegion({type: RegionType.circle, wpAry: [worldPoint], radiusAry: [radius], options, highlighted});
}

/**
 * create region on ellipse
 * @param worldPoint
 * @param angle  RegionValue
 * @param dim    RegionDimension [radius1, radius2]
 * @param options
 * @param highlighted
 */
export function makeRegionEllipse(worldPoint, dim, angle, options, highlighted) {
    return makeRegion({type: RegionType.ellipse, wpAry: [worldPoint],
                        dimensionAry: [dim], angle, options, highlighted });
}


/**
 * create region on ellipseannulus
 * @param worldPoint
 * @param angle  RegionValue
 * @param dimAry array of RegionDimension [radius1, radius2]
 * @param options
 * @param highlighted
 */
export function makeRegionEllipseAnnulus(worldPoint, dimAry, angle, options, highlighted ) {
    return makeRegion({type: RegionType.ellipseannulus, wpAry: [worldPoint],
                    dimensionAry: dimAry, angle, options, highlighted});
}

/**
 * create region on line
 * @param worldPoint1
 * @param worldPoint2
 * @param options
 * @param highlight
 */
export function makeRegionLine(worldPoint1, worldPoint2, options, highlight ) {
    return makeRegion({ type: RegionType.line, wpAry: [worldPoint1, worldPoint2], options, highlight});
}

/**
 * create region on polygon
 * @param wpAry array of WorldPt
 * @param options
 * @param highlight
 */
export function makeRegionPolygon(wpAry, options, highlight) {
    return makeRegion({type: RegionType.polygon, wpAry, options, highlight});
}

