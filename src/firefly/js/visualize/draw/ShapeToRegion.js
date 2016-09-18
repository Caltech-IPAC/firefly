/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import validator from 'validator';
import CsysConverter from '../CsysConverter.js';
import {convertAngle} from '../VisUtil.js';
import {makeScreenPt} from '../Point.js';
import {startRegionDes, setRegionPropertyDes, endRegionDes} from '../region/RegionDescription.js';
import {RegionType, regionPropsList, RegionValue, RegionValueUnit, RegionDimension} from '../region/Region.js';
import {DEFAULT_TEXTLOC} from '../region/RegionDrawer.js';
import ShapeDataObj, {rectOnImage} from './ShapeDataObj.js';
import {has, get, isNil, isEmpty} from 'lodash';

const HTML_DEG= '&deg;';


/**
 * generate region description
 * @param drawObj
 * @param plot   primePlot
 * @param drawParams
 * @return {Array}
 */
export function toRegion(drawObj, plot, drawParams) {
    var {sType,pts}= drawObj;
    var retList= [];

    var cc = CsysConverter.make(plot);

    switch (sType) {
        case ShapeDataObj.ShapeType.Text:
            retList= makeTextRegion(pts[0], cc, drawObj, drawParams);
            break;
        case ShapeDataObj.ShapeType.Line:
            retList= makeLineRegion(pts, cc, drawObj, drawParams);
            break;
        case ShapeDataObj.ShapeType.Circle:
            retList= makeCircleRegion(pts, cc, drawObj, drawParams);
            break;
        case ShapeDataObj.ShapeType.Rectangle:
            retList= makeRectangleRegion(pts, cc, drawObj, drawParams);
            break;
        case ShapeDataObj.ShapeType.Ellipse:
            retList = makeEllipseRegion(pts, cc, drawObj, drawParams);
            break;
        case ShapeDataObj.ShapeType.Annulus:
            retList = makeAnnulusRegion(pts, cc, drawObj, drawParams);
            break;
        case ShapeDataObj.ShapeType.BoxAnnulus:
            retList = makeBoxAnnulusRegion(pts, cc, drawObj, drawParams);
            break;
        case ShapeDataObj.ShapeType.EllipseAnnulus:
            retList = makeEllipseAnnulusRegion(pts, cc, drawObj, drawParams);
            break;
        case ShapeDataObj.ShapeType.Polygon:
            retList = makePolygonRegion(pts, cc, drawObj, drawParams);
            break;
    }
    return retList;
}


/**
 * get font size string in case the string ends with point unit
 * @param fontSize
 * @returns {*}
 */

var getFontSize = (fontSize) => {

    if (fontSize.length > 2 && fontSize.match(/\D\D$/)) {
        if (validator.isFloat(fontSize.slice(0, -2))) {
            return parseInt(fontSize.slice(0, -2));
        }
    } else if (validator.isFloat(fontSize)) {
        return parseInt(fontSize);
    }
    return null;
};

/**
 * generate region description for text
 * @param inPt
 * @param cc
 * @param drawObj
 * @param drawParams
 * @returns {*}
 */
function makeTextRegion( inPt, cc, drawObj, drawParams) {
    var {color}= drawParams;
    var {text}= drawObj;
    var des;
    var retList = [];

    if (!text || !inPt) return retList;

    var wp = inPt; //cc.getWorldCoords(inPt);
    if (!wp) return retList;

    des = startRegionDes(RegionType.text, cc, [wp]);
    if (isEmpty(des)) {
        return retList;
    }

    des += setRegionPropertyDes(regionPropsList.COLOR, color);

    if (text) {
        des += addTextRelatedProps(drawObj, drawParams);
    }
    des = endRegionDes(des);
    retList.push(des);

    return retList;
}


export function makeNonHtml(s) {
    var retVal= s;

    if (s.endsWith(HTML_DEG)) {
        retVal= s.substring(0,s.indexOf(HTML_DEG)) + ' deg';
    }
    return retVal;
}


/**
 * add text related property to the region description.
 *
 * note: for current version, a seperate text region is created if either non default textloc or textoffset exists.
 *
 * @param drawObj
 * @param drawParams
 * @returns {*}
 */
export function addTextRelatedProps(drawObj, drawParams) {
    var des = '';
    var {fontSize, fontName, fontWeight, fontStyle}= drawParams;
    var {text}= drawObj;

    if (!text) return des;

    var s = getFontSize(fontSize);

    des = setRegionPropertyDes(regionPropsList.TEXT, makeNonHtml(text)) +
          setRegionPropertyDes(regionPropsList.FONT,  {name: fontName, size: s, weight: fontWeight, slant: fontStyle });
/*
    if (textLoc && textLoc !== DEFAULT_TEXTLOC[regionType.key]) {
        des += setRegionPropertyDes(regionPropsList.TEXTLOC, textLoc.key);
    }

    if (textOffset) {
       des += setRegionPropertyDes(regionPropsList.OFFX, textOffset.x) +
              setRegionPropertyDes(regionPropsList.OFFY, textOffset.y);
    }
*/
    return des;
}


/**
 * create text region seperate from the region of any shape in case there exit non default textloc or textoffset
 * @param drawObj
 * @param drawParams
 * @param regionType
 * @param cc
 * @returns {*}
 */
export function createTextRegionFromShape(drawObj, drawParams, regionType, cc) {
    var {textLoc}= drawParams;
    var {text, textOffset}= drawObj;

    var des = '';
    var searchTextLoc = () => {
        var doChild = null;

        if (has(drawObj, 'drawObjAry')) {
            doChild = drawObj.drawObjAry.find( ( childDrawObj ) =>  has(childDrawObj, 'textWorldLoc') );
        }

        return doChild ? doChild.textWorldLoc : null;
    };

    // check if no text
    if (!text) return des;

    // check if text location is default as defined and no offset involved
    if ((!textLoc || textLoc === DEFAULT_TEXTLOC[regionType.key]) &&
        (!textOffset || (textOffset.x === 0.0 && textOffset.y === 0.0)))  return des;

    // adapt the computed text location if there is or recompute the text location
    var textWorldLoc = get(drawObj, 'textWorldLoc', searchTextLoc());
    if (!textWorldLoc) return des;

    return makeTextRegion(cc.getWorldCoords(textWorldLoc), cc, drawObj, drawParams);
}

/**
 * handle text associted with the region
 * if the textloc or textoffset is not set as default, then create seperate text region line,
 * or attach the text to the original regiopn shape
 * @param retList
 * @param crtRegionDes
 * @param drawObj
 * @param drawParams
 * @param regionType
 * @param cc
 */
export function handleTextFromRegion(retList, crtRegionDes, drawObj, drawParams, regionType, cc) {
    var textDes = createTextRegionFromShape(drawObj, drawParams, regionType, cc);
    var des;

    if (textDes) {  // seperate region line for text
        des = endRegionDes(crtRegionDes);

        retList.push(des);
        retList.push(textDes);
    } else {        // attach text to the original region line
        des = crtRegionDes + addTextRelatedProps(drawObj, drawParams);
        des = endRegionDes(des);
        retList.push(des);
    }
}

export function handleCommonRegionProps(color, lineWidth) {
    return setRegionPropertyDes(regionPropsList.COLOR, color) +
           setRegionPropertyDes(regionPropsList.LNWIDTH, lineWidth);
}
/**
 * generate region description for line and the associated text
 * @param pts
 * @param cc
 * @param drawObj
 * @param drawParams
 * @returns {Array}
 */
function makeLineRegion(pts, cc, drawObj, drawParams) {
    var wp0= pts[0]; //cc.getWorldCoords(pts[0]);
    var wp1= pts[1]; //cc.getWorldCoords(pts[1]);

    var {color}= drawParams;
    var {lineWidth}= drawObj;
    var retList = [];

    if (!wp0 || !wp1)  return retList;

    var des;

    des = startRegionDes(RegionType.line, cc, [wp0, wp1]);
    if (isEmpty(des)) return retList;

    des += handleCommonRegionProps(color, lineWidth);

    handleTextFromRegion(retList, des, drawObj, drawParams, RegionType.line, cc);

    return retList;
}

/**
 * ONLY VALID FOR CIRCLE!
 * @param plot
 * @param pts
 * @return {*}
 */
function getCircleCenter(plot,pts) {
    const pt0= plot.getScreenCoords(pts[0]);
    const pt1= plot.getScreenCoords(pts[1]);
    if (!pt0 || !pt1) return null;
    const x= Math.min(pt0.x,pt1.x) + Math.abs(pt0.x-pt1.x)/2;
    const y= Math.min(pt0.y,pt1.y) + Math.abs(pt0.y-pt1.y)/2;
    return plot.getWorldCoords(makeScreenPt(x,y));
}

/**
 *
 * @param plot
 * @param pts
 * @return {number}
 */
function findRadius(plot,pts) {
    var retval= -1;
    var pt0= plot.getScreenCoords(pts[0]);
    var pt1= plot.getScreenCoords(pts[1]);
    if (pt0 && pt1) {
        var xDist= Math.abs(pt0.x-pt1.x)/2;
        var yDist= Math.abs(pt0.y-pt1.y)/2;
        retval= Math.min(xDist,yDist);
    }
    return retval;
}

/**
 * create RegionValue (defined in Region.js) based on value and unit in terms of UnitType
 * @param r
 * @param unitType
 */
function makeRegionValue(r, unitType) {
    var rV;

    if (unitType === ShapeDataObj.UnitType.PIXEL) {
        rV = RegionValue(r, RegionValueUnit.SCREEN_PIXEL);
    } else if (unitType === ShapeDataObj.UnitType.IMAGE_PIXEL) {
        rV = RegionValue(r, RegionValueUnit.IMAGE_PIXEL);
    } else if (unitType === ShapeDataObj.UnitType.ARCSEC) {
        rV = RegionValue(convertAngle('arcsec', 'deg', r), RegionValueUnit.DEGREE);
    }

    return rV;
}

/**
 * create RegionDimension based on RegionValue in two dimensions in terms of UnitType
 * @param w
 * @param h
 * @param unitType
 * @returns {*}
 */
function makeRegionDim(w, h, unitType) {
    return RegionDimension( makeRegionValue(w, unitType), makeRegionValue(h, unitType) );
}

/**
 * create RegionValue based on angle and angleUnit, the return is in unit of degree
 * @param cc
 * @param angle
 * @param angleUnit
 * @returns {*}
 */
function makeRegionValueOnAngle(cc, angle, angleUnit) {
    var rAngle = 0.0;

    if (!isNil(angle) && angleUnit) {
        if (angleUnit === ShapeDataObj.UnitType.ARCSEC) {
            rAngle = convertAngle('arcsec', 'deg', angle);
        } else if (angleUnit === ShapeDataObj.UnitType.IMAGE_PIXEL) {
            rAngle = convertAngle('radian', 'deg', cc.zoomFactor * angle);
        } else if (angleUnit === ShapeDataObj.UnitType.PIXEL) {
            rAngle = convertAngle('radian', 'deg', angle);
        }
    }

    return RegionValue(-rAngle, RegionValueUnit.DEGREE);
}

/**
 * generate region description for circle and the associated text
 * @param pts
 * @param cc
 * @param drawObj
 * @param drawParams
 * @returns {Array}
 */
function makeCircleRegion(pts, cc, drawObj, drawParams) {
    if (isEmpty(pts)) return [];

    var {color,unitType}= drawParams;
    var {radius, lineWidth}= drawObj;
    var wp;             // circle center in world coordinate
    var retList= [];

    var st= ShapeDataObj.UnitType.PIXEL;

    if (pts.length === 1 && radius) {   // radius in world, image or screen coordinate
        st = unitType;
        wp = pts[0]; //cc.getWorldCoords(pts[0]);
                     // keep the original coordinate system
    } else if (pts.length > 1) {
        wp= getCircleCenter(cc, pts);
        radius = findRadius(cc, pts);   // in screen coordinate
    } else {
        return retList;
    }

    var rV = makeRegionValue(radius, st);
    var des = startRegionDes(RegionType.circle, cc, [wp], null, [rV]);

    if (isEmpty(des)) {
        return retList;
    }
    des += handleCommonRegionProps(color, lineWidth);

    handleTextFromRegion(retList, des, drawObj, drawParams, RegionType.circle, cc);

    return retList;
}


/**
 * generate region description for box and the associated text
 * @param pts
 * @param cc
 * @param drawObj
 * @param drawParams
 * @returns {Array}
 */
function makeRectangleRegion(pts, cc, drawObj, drawParams) {
    if (isEmpty(pts)) return [];

    var {color, lineWidth, unitType} = drawParams;
    var {width, height, angle = 0.0, angleUnit, isCenter = false, isOnWorld = false} = drawObj;
    var rDim;
    var rAngle;
    var centerPt;
    var retList= [];

    // find center of the rectangle
    // find the width and height
    // find the angle
    // find the associated text location

    if (pts.length === 1 && width && height) {   // radius in world, image or screen coordinate

        var sRect = rectOnImage(pts[0], isCenter, cc, width, height, unitType, isOnWorld );

        if (!sRect) return retList;
        centerPt = sRect.centerPt;  // for isOnWorld==false, the center point is on image coordinate

        rDim = makeRegionDim(width, height, unitType);

    }  else if (pts.length > 1) {
        var pt0 = cc.getScreenCoords(pts[0]);
        var pt1 = cc.getScreenCoords(pts[1]);

        if (!pt0 || !pt1) {
            return retList;
        }

        var w = Math.abs(pt1.x - pt0.x);
        var h = Math.abs(pt1.y - pt0.y);

        rDim = makeRegionDim(w, h, ShapeDataObj.UnitType.PIXEL);
        centerPt = makeScreenPt((pt0.x + pt1.x) / 2, (pt0.y + pt1.y) / 2);  // in screen coordinate

    } else {
        return retList;
    }
    rAngle = makeRegionValueOnAngle(cc, angle, angleUnit);

    // compose region description
    var des = startRegionDes(RegionType.box, cc, [centerPt], [rDim], null, true, rAngle);

    if (isEmpty(des)) return retList;

    des += handleCommonRegionProps(color, lineWidth);

    handleTextFromRegion(retList, des, drawObj, drawParams, RegionType.box, cc);

    return retList;
}

/**
 * generate region description for ellipse and the associated text
 * @param pts
 * @param cc
 * @param drawObj
 * @param drawParams
 * @returns {Array}
 */
function makeEllipseRegion(pts, cc, drawObj, drawParams) {
    if (isEmpty(pts)) return [];

    var {radius1, radius2, angle = 0.0, angleUnit, isOnWorld = true}= drawObj;
    var {color, lineWidth, unitType}= drawParams;

    var des;
    var rDim;
    var rAngle;
    var centerPt;
    var retList= [];

    // find center of the ellipse
    // find the radius1 and radius2 & slanted angle on screen pixel
    // find the angle
    // find the associated text location


    if (isNil(radius1) || isNil(radius2))  return retList;   // radius in world, image or screen coordinate

    centerPt = isOnWorld ? pts[0] : cc.getImageCoords(pts[0]); // cc.getWorldCoords(pts[0]); keep the original coordinatte system in description

    rDim = makeRegionDim(radius1, radius2, unitType);

    // make angle in readian
    rAngle = makeRegionValueOnAngle(cc, angle, angleUnit);

    des = startRegionDes(RegionType.ellipse, cc, [centerPt], [rDim], null, true, rAngle);
    if (isEmpty(des)) {
        return retList;
    }

    des += handleCommonRegionProps(color, lineWidth);

    handleTextFromRegion(retList, des, drawObj, drawParams, RegionType.ellipse, cc);

    return retList;
}

/**
 * generate region annulus and associated text
 * @param pts
 * @param cc
 * @param drawObj
 * @param drawParams
 * @returns {Array}
 */
function makeAnnulusRegion(pts, cc, drawObj, drawParams) {

    if (!pts.length) return [];

    var {radiusAry}= drawObj;
    var {color, lineWidth, unitType}= drawParams;

    var valAry;
    var retList = [];
    var des;

    if (!radiusAry) {
        return retList;
    }

    valAry = radiusAry.map( (radius) => makeRegionValue(radius, unitType) );
    des = startRegionDes(RegionType.annulus, cc, [pts[0]], null, valAry);

    des += handleCommonRegionProps(color, lineWidth);

    handleTextFromRegion(retList, des, drawObj, drawParams, RegionType.annulus, cc);

    return retList;

}

/**
 * generate region description for boxannulus and the associated text
 * @param pts
 * @param cc
 * @param drawObj
 * @param drawParams
 * @returns {Array}
 */
function makeBoxAnnulusRegion(pts, cc, drawObj, drawParams) {

    if (!pts.length) return [];

    var {color, lineWidth, unitType} = drawParams;
    var {dimensionAry, angle, angleUnit}= drawObj;

    var rgDimAry;
    var retList = [];
    var rAngle;
    var des;

    if (!dimensionAry) {
        return retList;
    }

    rgDimAry = dimensionAry.map( (dim) => makeRegionDim(dim[0], dim[1], unitType) );

    rAngle = makeRegionValueOnAngle(cc, angle, angleUnit);

    des = startRegionDes(RegionType.boxannulus, cc, [pts[0]], rgDimAry, null, true, rAngle);

    des += handleCommonRegionProps(color, lineWidth);

    handleTextFromRegion(retList, des, drawObj, drawParams, RegionType.boxannulus, cc);

    return retList;

}

/**
 *
  * @param pts
 * @param cc
 * @param drawObj
 * @param drawParams
 * @returns {Array}
 */
function makeEllipseAnnulusRegion(pts, cc, drawObj, drawParams) {

    if (!pts.length) return [];

    var {color, lineWidth, unitType} = drawParams;
    var {dimensionAry, angle, angleUnit}= drawObj;

    var rgDimAry;
    var retList = [];
    var rAngle;
    var des;

    if (!dimensionAry) {
        return retList;
    }

    rgDimAry = dimensionAry.map( (dim) => makeRegionDim(dim[0], dim[1], unitType) );

    rAngle = makeRegionValueOnAngle(cc, angle, angleUnit);

    des = startRegionDes(RegionType.ellipseannulus, cc, [pts[0]], rgDimAry, null, true, rAngle);

    des += handleCommonRegionProps(color, lineWidth);

    handleTextFromRegion(retList, des, drawObj, drawParams, RegionType.ellipseannulus, cc);

    return retList;

}

/**
 * generate region description for polygon and the associted text
 * @param pts
 * @param cc
 * @param drawObj
 * @param drawParams
 * @returns {Array}
 */
function makePolygonRegion(pts, cc, drawObj, drawParams) {

    if (!pts.length) return [];

    var {color, lineWidth} = drawParams;
    var ptAry;
    var retList = [];
    var des;

    ptAry = pts.map( (pt) => (cc.getWorldCoords(pt)));

    des = startRegionDes(RegionType.polygon, cc, ptAry, null, null);

    des += handleCommonRegionProps(color, lineWidth);

    handleTextFromRegion(retList, des, drawObj, drawParams, RegionType.polygon, cc);

    return retList;
}

