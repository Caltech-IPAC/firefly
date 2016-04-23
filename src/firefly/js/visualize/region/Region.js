/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * This Object contains the specifications of the DS9 region
 */

import Enum from 'enum';

export const RegionType = new Enum(['circle', 'annulus', 'ellipse', 'ellipseannulus', 'box',
                                    'boxannulus', 'line', 'point', 'polygon', 'text', 'undefined']);

export const RegionCsys = new Enum(['PHYSICAL', 'FK4', 'B1950', 'FK5', 'J2000',
                                    'IMAGE', 'ECLIPTIC', 'GALACTIC',
                                    'ICRS', 'AMPLIFIER', 'LINEAR', 'DETECTOR', 'UNDEFINED']);

export const RegionValueUnit = new Enum(['CONTEXT', 'DEGREE', 'RADIUS', 'ARCMIN', 'ARCSEC',
                                         'SCREEN_PIXEL', 'IMAGE_PIXEL']);

export const RegionPointType = new Enum(['circle', 'box', 'cross', 'diamond', 'x',
                                         'arrow', 'boxcircle', 'undefined']);


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
 * @constructor
 */
export var Region =
    ( {wpAry = null,
      radiusAry = null,
      dimensionAry = null,
      angle = null,
      type = RegionType.undefined,
      options = null,
      highlighted = false}
    ) => ({type, wpAry, radiusAry, dimensionAry, angle, options, highlighted});

/**
 *
 * @param color     string, 'white', 'black', 'red', 'green', 'blue', 'cyan', 'magenta', 'yellow'
 * @param text      string
 * @param font      RegionFont
 * @param pointType     RegionPointType
 * @param pointSize     number
 * @param editable      bool
 * @param movable       bool
 * @param rotatable     bool
 * @param highlightable bool
 * @param deletable     bool
 * @param fixedSize     bool
 * @param include       bool
 * @param lineWidth     number
 * @param offsetX       number
 * @param offsetY       number
 * @param message       string, containing parsing message
 * @constructor
 */

export var RegionOptions =
    ( {color = 'blue',
        text  = null,
        font  = null,
        pointType = null,
        pointSize =  0,
        editable  =  true,
        movable   =  true,
        rotatable =  true,
        highlightable =  true,
        deletable = true,
        fixedSize = false,
        include   = true,
        lineWidth = 0,
        offsetX   = 0,
        offsetY   = 0,
        message  = null}
    ) => ({color, text, font, pointType, pointSize, editable, movable, rotatable, highlightable,
        deletable, fixedSize, include, lineWidth, offsetX, offsetY, message});

/**
 *
 * @param value value for radius and dimension
 * @param unit  DEGREE, SCREEN_PIXEL and IMAGE_PIXEL
 * @constructor
 */

export var RegionValue =
    (value = 0.0, unit = RegionValueUnit) => ({value, unit});

/**
 *
 * @param width  RegionValue width of box or first radius of ellipse
 * @param height RegionValue height of box or second radius of ellipse
 * @constructor
 */
export var RegionDimension = (width, height) => ({width, height});

/**
 *
 * @param name  string
 * @param point  string for font size
 * @param weight string
 * @param slant  string
 * @constructor
 */
export var RegionFont = ({name = 'SansSerif', point = '10', weight = 'bold', slant = 'italic'}) =>
                        ( {name, point, weight, slant} );


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


var findEnumKey = (enumObj, val) => ( enumObj.enums.find((item) => item.key.toLowerCase() === val.toLowerCase()));
/**
 *
 * @param rgTypeStr
 * @returns {RegionType}
 */
export function getRegionType(rgTypeStr) {
    const regType = findEnumKey( RegionType, rgTypeStr);

    return regType ? regType : RegionType.undefined;
}

/**
 *
 * @param typeStr
 * @returns {RegionPointType}
 */
export function getRegionPointType(typeStr) {
    const pt = findEnumKey(RegionPointType, typeStr);

    return pt ? pt : RegionPointType.undefined;
}

/**
 * parse font string set in region property
 * @param coordStr
 * @returns {RegionCsys}
 */
export function getRegionCoordSys(coordStr) {
    const cs = findEnumKey(RegionCsys, coordStr);

    return cs ? cs : RegionCsys.UNDEFINED;
}


/**
 *
 * @param name string
 * @param point string (font size)
 * @param weight string
 * @param slant string
 */
export function makeFont(name, point, weight, slant) {
    return RegionFont({name, point, weight, slant});
}

/**
 * create region on point
 * @param worldPoint
 * @param options
 * @param highlighted
 */
export function makePoint(worldPoint, options = null, highlighted = false) {
    return  Region({type: RegionType.point, wpAry: [worldPoint],  options, highlighted});
}

/**
 * create region on text
 * @param worldPoint
 * @param options
 * @param highlighted
 */
export function makeText(worldPoint, options = null, highlighted = false) {
    return Region({type: RegionType.text, wpAry: [worldPoint], options, highlighted});
}

/**
 * create region on box
 * @param worldPoint
 * @param angle RegionValue
 * @param dim   ReginDimension
 * @param options
 * @param highlighted
 */
export function makeBox(worldPoint, dim, angle, options = null, highlighted = false) {
    return Region({type: RegionType.box, wpAry: [worldPoint], dimensionAry: [dim], angle, options, highlighted});
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
export function makeBoxAnnulus(worldPoint, dimAry,  angle, options = null, highlighted = false ) {
    return Region({type: RegionType.boxannulus, wpAry: [worldPoint], dimensionAry: dimAry, angle, options, highlighted});
}

/**
 * create region on annulus
 * @param worldPoint
 * @param radAry array of RegionValue (radius)
 * @param options
 * @param highlighted
 */
export function makeAnnulus(worldPoint, radAry,  options = null, highlighted = false) {
    return Region({type: RegionType.annulus, wpAry: [worldPoint],
                   radiusAry: radAry, options, highlighted});
}

/**
 * create region on circle
 * @param worldPoint
 * @param radius  RegionValue
 * @param options
 * @param highlighted
 */
export function makeCircle(worldPoint, radius,  options = null, highlighted = false) {
    return Region({type: RegionType.circle, wpAry: [worldPoint],
                    radiusAry: [radius], options, highlighted});
}

/**
 * create region on ellipse
 * @param worldPoint
 * @param angle  RegionValue
 * @param dim    RegionDimension [radius1, radius2]
 * @param options
 * @param highlighted
 */
export function makeEllipse(worldPoint, dim, angle, options = null, highlighted = false) {
    return Region({type: RegionType.ellipse, wpAry: [worldPoint],
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
export function makeEllipseAnnulus(worldPoint, dimAry, angle, options = null, highlighted = false ) {
    return Region({type: RegionType.ellipseannulus, wpAry: [worldPoint],
                    dimensionAry: dimAry, angle, options, highlighted});
}

/**
 * create region on line
 * @param worldPoint1
 * @param worldPoint2
 * @param options
 * @param highlight
 */
export function makeLine(worldPoint1, worldPoint2, options = null, highlight = false ) {
    return Region({ type: RegionType.line, wpAry: [worldPoint1, worldPoint2], options, highlight});
}

/**
 * create region on polygon
 * @param wpAry array of WorldPt
 * @param options
 * @param highlight
 */
export function makePolygon(wpAry, options = null, highlight = false) {
    return Region({type: RegionType.polygon, wpAry, options, highlight});
}
