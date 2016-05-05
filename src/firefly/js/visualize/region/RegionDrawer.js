/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import { RegionType, RegionValueUnit, regionPropsList,
         getRegionDefault } from './Region.js';
import {makeOffsetPt} from '../Point.js';
import {convertAngle} from '../VisUtil.js';
import {get, isEmpty} from 'lodash';
import ShapeDataObj from '../draw/ShapeDataObj.js';
import {DrawSymbol, make as makePoint } from '../draw/PointDataObj.js';
import {union, isArray} from 'lodash';
import {TextLocation} from '../draw/DrawingDef.js';


/**
 * make drawObj based on region type
 * @param regionAry Region array
 * @returns {*}     drawing object array
 */

export function drawRegions(regionAry) {
        return regionAry.reduce((prev, regionObj) => {
                var drawobjAry = drawRegion(regionObj);

                prev = [...prev,...drawobjAry];
                return prev;
            }, []);
}

var addRegionInfo = (dAry, rgObj) => {
    if (isArray(dAry)) {
        dAry.forEach((d) => d.region = rgObj);
    } else {
        dAry.region = rgObj;
    }
};

/**
 * make DrawObj for one region
 * @param regionObj
 * @returns {Array}
 */
export function drawRegion(regionObj) {
    var dAry = [];

    if (!regionObj.options || regionObj.type === RegionType.undefined || regionObj.type === RegionType.message) {
        return dAry;
    }

    switch (regionObj.type) {
        case RegionType.circle:
            dAry = drawRegionCircle(regionObj);
            break;
        case RegionType.annulus:
            dAry = drawRegionAnnulus(regionObj);
            break;
        case RegionType.box:
            dAry = drawRegionBox(regionObj);
            break;
        case RegionType.boxannulus:
            dAry = drawRegionBoxAnnulus(regionObj);
            break;
        case RegionType.line:
            dAry = drawRegionLine(regionObj);
            break;
        case RegionType.point:
            dAry = drawRegionPoint(regionObj);
            break;
        case RegionType.polygon:
            dAry = drawRegionPolygon(regionObj);
            break;
        case RegionType.ellipse:
            dAry = drawRegionEllipse(regionObj);
            break;
        case RegionType.ellipseannulus:
            dAry = drawRegionEllipseAnnulus(regionObj);
            break;
        case RegionType.text:
            dAry = drawRegionText(regionObj);
            break;
        default:
            break;
    }

    addRegionInfo(dAry, regionObj);   // attach region object into drawObj
    return dAry;
}


/**
 * convert RegionValueUnit to ShapeDataObj.UnitType
 * @param unit
 * @returns {*}
 */

var regionUnitToDrawObj = (unit) => {
    if (unit === RegionValueUnit.SCREEN_PIXEL) {
        return ShapeDataObj.UnitType.PIXEL;
    } else if (unit === RegionValueUnit.IMAGE_PIXEL) {
        return ShapeDataObj.UnitType.IMAGE_PIXEL;
    } else {
        return ShapeDataObj.UnitType.ARCSEC;
    }
};


/**
 * convert RegionPointType to DrawObj DrawSymbol
 * @param ptype
 * @returns {*}
 */
function regionPtTypeToDrawObj(ptype) {
    var s;

    switch(ptype.key) {
        case 'circle':
            s = DrawSymbol.CIRCLE;
            break;
        case 'box':
            s = DrawSymbol.SQUARE;
            break;
        case 'cross':
            s = DrawSymbol.CROSS;
            break;
        case 'diamond':
            s = DrawSymbol.DIAMOND;
            break;
        case 'x':
            s = DrawSymbol.X;
            break;
        case 'boxcircle':
            s = DrawSymbol.BOXCIRCLE;     // alternate
            break;
        case 'arrow':
            s = DrawSymbol.ARROW;
            break;
        default:
            s = DrawSymbol.DOT;
    }
    return s;
}

/**
 * add addition property to DrawObj based on selected RegionOptions properties
 * @param rgPropAry
 * @param rgOptions
 * @param dObj
 */
function updateDrawobjProp(rgPropAry, rgOptions, dObj) {

    function addPropToDrawObj(regionProp, rgOptions, dObj) {
        switch(regionProp) {
            case regionPropsList.COLOR:
                dObj.color = get(rgOptions, regionProp, getRegionDefault(regionProp));
                break;
            case regionPropsList.TEXT:
                var t = get(rgOptions, regionProp, getRegionDefault(regionProp));
                if (!isEmpty(t)) {
                    dObj.text = t;
                }
                break;

            case regionPropsList.FONT:
                if (dObj.text) {
                    var f = get(rgOptions, regionProp, getRegionDefault(regionProp));

                    dObj.fontName = f.name;
                    dObj.fontSize = f.point;
                    dObj.fontWeight = f.weight;
                    dObj.fontStyle = f.slant;
                }
                break;

            case regionPropsList.LNWIDTH:
                dObj.lineWidth = get(rgOptions, regionProp, getRegionDefault(regionProp));
                break;

            case regionPropsList.OFFX:
                if (dObj.text) {
                    var x = get(rgOptions, regionPropsList.OFFX, getRegionDefault(regionPropsList.OFFX));
                    var y = get(rgOptions, regionPropsList.OFFY, getRegionDefault(regionPropsList.OFFY));
                    if (x != 0 || y != 0) {
                        dObj.textOffset = makeOffsetPt(x, y);
                    }
                }
                break;

            case regionPropsList.PTTYPE:
                var s = get(rgOptions, regionProp, getRegionDefault(regionProp));
                dObj.symbol = regionPtTypeToDrawObj(s);
                break;

            case regionPropsList.PTSIZE:
                dObj.size = get(rgOptions, regionProp, getRegionDefault(regionProp));
                break;

            default:
                break;
        }
    }

    rgPropAry.forEach((prop) => addPropToDrawObj(prop, rgOptions, dObj));
}

/**
 * produce DrawObj for region circle
 * @param r
 * @param wp
 * @param options
 * @param propChkAry
 * @returns {*}
 */

function drawOneCircle(r, wp, options, propChkAry) {

    var unit = regionUnitToDrawObj(r.unit);
    var radius;

    if (unit === ShapeDataObj.UnitType.ARCSEC) {
        radius = convertAngle(r.unit.key, ShapeDataObj.UnitType.ARCSEC.key, r.value);
    } else {
        radius = r.value;
    }

    var dObj = ShapeDataObj.makeCircleWithRadius(wp, radius, unit);

    updateDrawobjProp(propChkAry, options, dObj);
    return dObj;
}

function drawRegionCircle(regionObj) {
    var l = regionObj.radiusAry.length-1;
    var dObj = drawOneCircle(regionObj.radiusAry[l], regionObj.wpAry[0], regionObj.options,
                            [regionPropsList.COLOR, regionPropsList.TEXT, regionPropsList.FONT,
                             regionPropsList.LNWIDTH, regionPropsList.OFFX]);

    return [dObj];
}

/**
 * produce DrawObj array for region annulus, get the circle from the outmost first
 * @param regionObj
 * @returns {*}
 */
function drawRegionAnnulus(regionObj) {
    var firstObj = drawRegionCircle(regionObj);

    firstObj[0].textLoc = TextLocation.CIRCLE_SE;
    var moreObj = regionObj.radiusAry.reverse().slice(1).map((r) => {
        var nextObj = drawOneCircle(r, regionObj.wpAry[0], regionObj.options, []);

        nextObj.color = firstObj[0].color;
        nextObj.lineWidth = firstObj[0].lineWidth;
        return nextObj;
    });

    return union(firstObj, moreObj);
}


/**
 * make one drawobj for box with ratote angle, makeRectangleByCenter is used
 * @param w  width
 * @param h  height
 * @param wp center of box
 * @param options region properties
 * @param propChkAry property cheeck list
 * @param a rotation angle
 * @returns {*}
 */
function drawOneBox(w, h, a, wp, options, propChkAry) {
    var unit = regionUnitToDrawObj(w.unit);
    var angleUnit = regionUnitToDrawObj(a.unit);
    var width, height, angle;

    if (unit === ShapeDataObj.UnitType.ARCSEC) {
        width = convertAngle(w.unit.key, ShapeDataObj.UnitType.ARCSEC.key, w.value);
        height = convertAngle(h.unit.key, ShapeDataObj.UnitType.ARCSEC.key, h.value);
    } else {
        width = w.value;
        height = h.value;
    }

    if (angleUnit === ShapeDataObj.UnitType.ARCSEC) {
        angle= convertAngle(a.unit.key, angleUnit.key, a.value);
    } else {
        angle = a.value;
    }

    var dObj = ShapeDataObj.makeRectangleByCenter(wp, width, height, unit, angle, angleUnit);

    updateDrawobjProp(propChkAry, options, dObj);
    return dObj;
}

/**
 * make DrawObj for one box with angle
 * @param regionObj
 * @returns {*[]}
 */

function drawRegionBox(regionObj) {
    var l = regionObj.dimensionAry.length-1;
    var dObj = drawOneBox(regionObj.dimensionAry[l].width,
                          regionObj.dimensionAry[l].height,
                          regionObj.angle,
                          regionObj.wpAry[0],
                          regionObj.options,
                          [regionPropsList.COLOR, regionPropsList.TEXT, regionPropsList.FONT,
                              regionPropsList.LNWIDTH, regionPropsList.OFFX]);
    return [dObj];
}

/**
 * make DrawObj array for region box annulus, get the outmost box first
 * @param regionObj
 * @returns {*}
 */
function drawRegionBoxAnnulus(regionObj) {
    var firstObj = drawRegionBox(regionObj);

    firstObj[0].textLoc = TextLocation.RECT_SE;
    var moreObj = regionObj.dimensionAry.reverse().slice(1).map((d) => {
        var nextObj = drawOneBox(d.width, d.height, regionObj.angle, regionObj.wpAry[0], regionObj.options, []);

        nextObj.color = firstObj[0].color;
        nextObj.lineWidth = firstObj[0].lineWidth;
        return nextObj;
    });

    return union(firstObj, moreObj);
}

/**
 * make DrawObj for region line
 * @param pt1
 * @param pt2
 * @param options
 * @param propChkAry
 * @returns {*}
 */
function drawOneLine(pt1, pt2, options, propChkAry) {
    var  dObj = ShapeDataObj.makeLine(pt1, pt2);

    updateDrawobjProp(propChkAry, options, dObj);
    return dObj;
}

function drawRegionLine(regionObj) {
    var dObj = drawOneLine(regionObj.wpAry[0], regionObj.wpAry[1], regionObj.options,
                          [regionPropsList.COLOR, regionPropsList.TEXT, regionPropsList.FONT,
                            regionPropsList.LNWIDTH, regionPropsList.OFFX]);
    return [dObj];
}

/**
 * make DrawObj array for region polygon
 * @param regionObj
 * @returns {*}
 */
function drawRegionPolygon(regionObj) {
    var firstObj = drawRegionLine(regionObj);
    var wpAry = [...regionObj.wpAry.slice(1), regionObj.wpAry[0]];

    var moreObj = wpAry.slice(0, -1).map( (wp, index) => {
        var nextObj = drawOneLine(wp, wpAry[index+1], regionObj.options, []);

        nextObj.color = firstObj[0].color;
        nextObj.lineWidth = firstObj[0].lineWidth;
        return nextObj;
    });

    return union(firstObj, moreObj);
}



/**
 * make one drawobj for ellipse with ratote angle, makeEllipse is used
 * @param r1  radius1
 * @param r2  radius2
 * @param wp center of ellipse
 * @param options region properties
 * @param propChkAry property cheeck list
 * @param a rotation angle
 * @returns {*}
 */
function drawOneEllipse(r1, r2, a, wp, options, propChkAry) {
    var unit = regionUnitToDrawObj(r1.unit);
    var angleUnit = regionUnitToDrawObj(a.unit);
    var radius1, radius2, angle;

    if (unit === ShapeDataObj.UnitType.ARCSEC) {
        radius1 = convertAngle(r1.unit.key, ShapeDataObj.UnitType.ARCSEC.key, r1.value);
        radius2 = convertAngle(r2.unit.key, ShapeDataObj.UnitType.ARCSEC.key, r2.value);
    } else {
        radius1 = r1.value;
        radius2 = r2.value;
    }

    if (angleUnit === ShapeDataObj.UnitType.ARCSEC) {
        angle= convertAngle(a.unit.key, angleUnit.key, a.value);
    } else {
        angle = a.value;
    }

    var dObj = ShapeDataObj.makeEllipse(wp, radius1, radius2, unit, angle, angleUnit);

    updateDrawobjProp(propChkAry, options, dObj);
    return dObj;
}

/**
 * make DrawObj for one ellipse with angle
 * @param regionObj
 * @returns {*[]}
 */

function drawRegionEllipse(regionObj) {
    var l = regionObj.dimensionAry.length-1;
    var dObj = drawOneEllipse(regionObj.dimensionAry[l].width,
        regionObj.dimensionAry[l].height,
        regionObj.angle,
        regionObj.wpAry[0],
        regionObj.options,
        [regionPropsList.COLOR, regionPropsList.TEXT, regionPropsList.FONT,
            regionPropsList.LNWIDTH, regionPropsList.OFFX]);

    return [dObj];
}

/**
 * make DrawObj array for region ellipse annulus, get the outmost ellipse first
 * @param regionObj
 * @returns {*}
 */
function drawRegionEllipseAnnulus(regionObj) {
    var firstObj = drawRegionEllipse(regionObj);

    firstObj[0].textLoc = TextLocation.ELLIPSE_SE;
    var moreObj = regionObj.dimensionAry.reverse().slice(1).map((d) => {
        var nextObj = drawOneEllipse(d.width, d.height, regionObj.angle, regionObj.wpAry[0], regionObj.options, []);

        nextObj.color = firstObj[0].color;
        nextObj.lineWidth = firstObj[0].lineWidth;
        return nextObj;
    });

    return union(firstObj, moreObj);
}



/**
 * make DrawObj for region point
 * @param regionObj
 * @returns {*[]}
 */
function drawRegionPoint(regionObj) {
    var pObj = makePoint(regionObj.wpAry[0]);

    updateDrawobjProp([regionPropsList.COLOR, regionPropsList.TEXT, regionPropsList.FONT,
                        regionPropsList.PTTYPE, regionPropsList.PTSIZE, regionPropsList.OFFX],
                      regionObj.options, pObj);

    return [pObj];
}

/**
 * make DrawObj for region text
 * @param regionObj
 * @returns {*[]}
 */
function drawRegionText(regionObj) {
    var text = get(regionObj, 'options.text');

    if (!text) {
        text = getRegionDefault(regionPropsList.TEXT);
    }

    var tObj = ShapeDataObj.makeText(regionObj.wpAry[0], text);
    updateDrawobjProp([regionPropsList.COLOR, regionPropsList.FONT, regionPropsList.OFFX],
                       regionObj.options, tObj);

    return [tObj];
}