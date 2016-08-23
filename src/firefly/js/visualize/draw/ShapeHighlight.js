
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import VisUtil, {convertAngle} from '../VisUtil.js';
import ShapeDataObj, {rectOnImage, lengthToImagePixel, widthAfterRotation, heightAfterRotation } from './ShapeDataObj.js';
import {makeScreenPt, makeImagePt} from '../Point.js';
import {get, isNil, set} from 'lodash';

const doAry = 'drawObjAry';

// in image coordinate or screen coordinate
var areaObj = (min_x, max_x, min_y, max_y, unit = ShapeDataObj.UnitType.IMAGE_PIXEL) => {
    var c_x = (min_x + max_x) / 2;
    var c_y = (min_y + max_y) / 2;

    return {
        upperLeft: (unit === ShapeDataObj.UnitType.PIXEL) ? makeScreenPt(min_x, min_y) : makeImagePt(min_x, max_y),
        center: (unit === ShapeDataObj.UnitType.PIXEL) ? makeScreenPt(c_x, c_y) : makeImagePt(c_x, c_y),
        width: (max_x - min_x + 1),
        height: (max_y - min_y + 1)
    };
};

/**
 * get the shape coverage area in terms of image coordinate
 * @param drawObj
 * @param cc
 * @param def
 * @returns {*} the upperLeft corner and the covered width and height
 */
export function getDrawobjArea(drawObj, cc, def={}) {
    var rCover = null;
    var firstObj;

    switch (drawObj.sType) {
        case ShapeDataObj.ShapeType.Line:
            rCover = getDrawobjLineArea(drawObj, cc);
            break;
        case ShapeDataObj.ShapeType.Circle:
            rCover = getDrawobjCircleArea(drawObj, cc, def);
            break;
        case ShapeDataObj.ShapeType.Rectangle:
            rCover = getDrawobjRectArea(drawObj, cc, def);
            break;
        case ShapeDataObj.ShapeType.Ellipse:
            rCover = getDrawobjEllipseArea(drawObj, cc, def);
            break;
        case ShapeDataObj.ShapeType.Polygon:
            rCover = getDrawobjPolygonArea(drawObj, cc);
            break;
        case ShapeDataObj.ShapeType.Annulus:
            if (firstObj = get(drawObj, [doAry, '0'])) {
                rCover = getDrawobjCircleArea(firstObj, cc, def);
            }
            break;
        case ShapeDataObj.ShapeType.BoxAnnulus:
            if (firstObj = get(drawObj, [doAry, '0'])) {
                rCover = getDrawobjRectArea(firstObj, cc, def);
            }
            break;
        case ShapeDataObj.ShapeType.EllipseAnnulus:
            if (firstObj = get(drawObj, [doAry, '0'])) {
                rCover = getDrawobjEllipseArea(firstObj, cc, def);
            }
            break;
    }
    return rCover;
}

/**
 * get covered area for drawobj on line
 * @param drawObj
 * @param cc
 * @returns
 */
function getDrawobjLineArea(drawObj, cc ) {
    var {pts} = drawObj;
    var pt1, pt2;

    if (pts.length < 2) return null;

    pt1 = cc.getImageCoords(pts[0]);
    pt2 = cc.getImageCoords(pts[1]);
    if ( !pt1 || !pt2) return null;

    var minx = Math.min(pt1.x, pt2.x);
    var maxx = Math.max(pt1.x, pt2.x);
    var miny = Math.min(pt1.y, pt2.y);
    var maxy = Math.max(pt1.y, pt2.y);

    return  areaObj(minx, maxx, miny, maxy);
}

/**
 * get covered area for drawobj on circle
 * @param drawObj
 * @param cc
 * @param def
 * @returns {*}
 */
function getDrawobjCircleArea(drawObj, cc, def) {
    var {pts, radius} = drawObj;
    var unitType = drawObj.unitType || def.unitType ||  ShapeDataObj.UnitType.PIXEL;
    var pt1, pt2;
    var imgRadius;

    if (pts.length < 1) return null;

    pt1 = cc.getImageCoords(pts[0]);
    if (!pt1) return null;

    if (pts.length === 1) {
        if (!radius) return null;

        imgRadius = lengthToImagePixel(radius, cc, unitType);

        return {
            center: pt1,
            upperLeft: makeImagePt(pt1.x - imgRadius, pt1.y + imgRadius),
            width: imgRadius * 2,
            height: imgRadius * 2
        };
    } else {
        pt2 = cc.getImageCoords(pts[1]);
        if (!pt2) return null;

        const xDist = Math.abs(pt1.x - pt2.x);
        const yDist = Math.abs(pt1.y - pt2.y);
        imgRadius = Math.min(xDist, yDist);

        return {
            center: makeImagePt(Math.min((pt1.x + pt2.x)/2, (pt1.x + pt2.y)/2)),
            upperLeft: makeImagePt(Math.min(pt1.x, pt2.x), Math.max(pt1.y, pt2.y)),
            width: imgRadius * 2,
            height: imgRadius * 2
        };
    }
}



/**
 * get covered area for drawobj on rect with rotated angle
 * @param drawObj
 * @param cc
 * @param def
 * @returns {*}
 */
function getDrawobjRectArea(drawObj, cc, def) {
    var {pts, width, height, angle, angleUnit, isOnWorld = false, isCenter = false} = drawObj;
    var unitType = drawObj.unitType || def.unitType ||  ShapeDataObj.UnitType.PIXEL;
    var pt1, pt2;
    var rAngle = 0.0;
    var centerPt, w, h;  //w, h in image

    if (pts.length < 1) return null;

    pt1 = cc.getImageCoords(pts[0]);
    if (!pt1) return null;

    if (pts.length === 1) {
        if (!width || !height) return null;
        var sRect;

       // return center, width (screen), height (screen), slanted angle (radian)
       sRect = rectOnImage(pts[0], isCenter, cc, width, height, unitType, isOnWorld);
       if (!sRect) return null;

       centerPt = cc.getImageCoords(sRect.centerPt);
       if (!centerPt) return null;

       w = sRect.width;
       h = sRect.height;
       rAngle = sRect.angle;
    } else {
        pt2 = cc.getImageCoords(pts[1]);
        if (!pt2) return null;

        w = Math.abs(pt1.x-pt2.x);
        h = Math.abs(pt1.y-pt2.y);
        centerPt = makeImagePt((pt1.x+pt2.x)/2, (pt1.y+pt2.y)/2);
    }

    // angle is for screen pixel coordinate
    if (isCenter && angleUnit) {
        if (angleUnit === ShapeDataObj.UnitType.ARCSEC) {
            rAngle -= convertAngle('arcsec', 'radian', angle);
        } else if (angleUnit === ShapeDataObj.UnitType.IMAGE_PIXEL) {
            rAngle -= cc.zoomFactor * angle;
        } else {
            rAngle -= angle;
        }
    }
    rAngle -= get(drawObj, 'renderOptions.rotAngle', 0.0);

    var area_w, area_h;

    area_w = widthAfterRotation(w, h, rAngle);
    area_h = heightAfterRotation(w, h, rAngle);

    return {
        center: centerPt,
        upperLeft: makeImagePt(centerPt.x - area_w/2, centerPt.y + area_h/2),
        width: area_w,
        height: area_h
    };
}

/**
 * get covered area for drawobj on ellipse including rotate angle
 * @param drawObj
 * @param cc
 * @param def
 * @returns {*}
 */
function getDrawobjEllipseArea(drawObj, cc, def) {
    var {pts, radius1, radius2, angle, angleUnit, isOnWorld = true} = drawObj;
    var unitType = drawObj.unitType || def.unitType ||  ShapeDataObj.UnitType.PIXEL;
    var rAngle;
    var centerPt, w, h;

    if (!pts || pts.length < 1 || !radius1 || !radius2) return null;

    // return center, width (screen), height (screen), slanted angle (radian)
    var sRect = rectOnImage(pts[0], true, cc, radius1*2, radius2*2, unitType, isOnWorld);
    if (!sRect) return null;

    centerPt = cc.getImageCoords(sRect.centerPt);
    if (!centerPt) return null;

    w = sRect.width;
    h = sRect.height;
    rAngle = sRect.angle;

    // angle is for screen pixel coordinate
    if (angleUnit) {
        if (angleUnit === ShapeDataObj.UnitType.ARCSEC) {
            rAngle -= convertAngle('arcsec', 'radian', angle);
        } else if (angleUnit === ShapeDataObj.UnitType.IMAGE_PIXEL) {
            rAngle -= cc.zoomFactor * angle;
        } else {
            rAngle -= angle;
        }
    }
    rAngle -= get(drawObj, 'renderOptions.rotAngle', 0.0);

    var ux = w * Math.cos(rAngle);
    var uy = w * Math.sin(rAngle);
    var vx = h * Math.cos(rAngle + Math.PI/2);
    var vy = h * Math.sin(rAngle + Math.PI/2);

    var area_w = Math.sqrt(ux*ux + vx*vx);
    var area_h = Math.sqrt(uy*uy + vy*vy);

    return {
        center: centerPt,
        upperLeft: makeImagePt(centerPt.x - area_w/2, centerPt.y + area_h/2),
        width: area_w,
        height: area_h
    };
}

/**
 * find the covered area for drawobj polygon
 * @param drawObj
 * @param cc
 * @returns {null}
 */
function getDrawobjPolygonArea(drawObj, cc) {
    var {pts} = drawObj;
    var minx, miny, maxx, maxy;
    var wpScreen;

    if (pts.length < 3) return null;

    wpScreen = cc.getImageCoords(pts[0]);
    if (!wpScreen) {
        return null;
    }
    minx = wpScreen.x;
    maxx = minx;
    miny = wpScreen.y;
    maxy = miny;

    pts.slice(1).some( (wp) => {
        wpScreen = cc.getImageCoords(wp);
        if (wpScreen) {
            if (wpScreen.x < minx)  minx = wpScreen.x;
            if (wpScreen.x > maxx)  maxx = wpScreen.x;
            if (wpScreen.y < miny)  miny = wpScreen.y;
            if (wpScreen.y > maxy)  maxy = wpScreen.y;
        }

        return isNil(wpScreen);
    });

    return areaObj(minx, maxx, miny, maxy);
}

export const DELTA = 2;


/**
 * check if any given point is inside the region on the screen with the consideration of line width
 * @param drawObj
 * @param pt
 * @param cc
 * @param def
 * @returns {*}
 */
export function isScreenPtInRegion(drawObj, pt, cc, def={}) {
    var sPt = cc.getScreenCoords(pt);
    var inside = false;

    if (!sPt) return {inside};

    switch (drawObj.sType) {
        case ShapeDataObj.ShapeType.Line:
            return isInDrawobjLine(drawObj, sPt, cc);
        case ShapeDataObj.ShapeType.Circle:
            return isInDrawobjCircle(drawObj, sPt, cc, def);
        case ShapeDataObj.ShapeType.Rectangle:
            return isInDrawobjRectangle(drawObj, sPt, cc, def);
        case ShapeDataObj.ShapeType.Ellipse:
            return isInDrawobjEllipse(drawObj, sPt, cc, def);
        case ShapeDataObj.ShapeType.Polygon:
            return isInDrawobjPolygon(drawObj, sPt, cc);
        case ShapeDataObj.ShapeType.Annulus:
            return isInDrawobjCircle(get(drawObj, [doAry, '0']), sPt, cc, def);
        case ShapeDataObj.ShapeType.BoxAnnulus:
            return isInDrawobjRectangle(get(drawObj, [doAry, '0']), sPt, cc, def);
        case ShapeDataObj.ShapeType.EllipseAnnulus:
            return isInDrawobjEllipse(get(drawObj, [doAry, '0']), sPt, cc, def);
    }
    return {inside};
}

/**
 * check if the screen point is inside a polygon (either convex or non-convex)
 * @param sPt
 * @param corners
 * @param cc
 * @param lineWidth
 * @returns {boolean}
 */
export function isWithinPolygon(sPt, corners, cc, lineWidth = 1) {

    if (corners.length < 3) return false;

    var lw = Math.floor((lineWidth+1)/2) + DELTA;
    var inside = false;
    var closeCorners = [...corners, corners[0]].map( (pt) => cc.getScreenCoords(pt) );
    var totalP = corners.length;
    var xOnLines = [];

    closeCorners.forEach ( (pt, index) => {
        if (index < totalP) {
            var pt1, pt2;

            pt1 = pt;
            pt2 = closeCorners[index + 1];

            if ((pt1.y !== pt2.y) &&
                (sPt.y >= Math.min(pt1.y, pt2.y)) &&
                (sPt.y <= Math.max(pt1.y, pt2.y))) {  // not include pt1.y === pt2.y
                var xLine = (pt2.x - pt1.x) * (sPt.y - pt1.y) / (pt2.y - pt1.y) + pt1.x;

                xOnLines.push(xLine);
            }
        }
    } );

    if (xOnLines.length > 0) {
        lw *= -1;

        var xSorted  = xOnLines.sort((a, b) => (a - b)).map( (x) => {
            x += lw;
            lw *= -1;
            return x;
        });

        // expand the position to the left and right per lineWidth for all edges alternatively
        xSorted.forEach( (x, index) => {
            if ( sPt.x < (x + index%2)) {
                inside = !inside;
            }
        });
    }


    return inside;
}


/**
 * rotate rotate point [x, y] around [0, 0]
 * @param sPt
 * @param angle
 */
function rotatePointAroundOrigin(sPt, angle) {
    var offx = sPt[0] * Math.cos(angle) - sPt[1] * Math.sin(angle);
    var offy = sPt[0] * Math.sin(angle) + sPt[1] * Math.cos(angle);

    return {offx, offy};
}

/**
 * check if a screen point is in touch with a line (within in very close distance)
 * @param drawObj
 * @param sPt
 * @param cc
 * @returns {*} inside or not and the vertical or horizontal distance to the line
 */
function isInDrawobjLine(drawObj, sPt, cc) {
    var {pts, lineWidth} = drawObj;
    var inside = false;

    if (pts.length < 2) return {inside};

    var pt1 = cc.getScreenCoords(pts[0]);
    var pt2 = cc.getScreenCoords(pts[1]);
    var dist;
    var lw = lineWidth ? Math.floor((lineWidth+1)/2) : 1;

    if (!pt1 || !pt2) return {inside};

    if (pt1.y === pt2.y) {
        if (sPt.x >= Math.min(pt1.x, pt2.x) && sPt.x <= Math.max(pt1.x, pt2.x)) {
            dist = Math.abs(sPt.y - pt1.y);
        }
    } else {
        if (sPt.y < Math.min(pt1.y, pt2.y) || sPt.y > Math.max(pt1.y, pt2.y)) {
            return {inside};
        }

        var linex = (pt2.x - pt1.x) * (sPt.y - pt1.y) / (pt2.y - pt1.y) + pt1.x;

        dist = Math.abs(sPt.x - linex);
    }

    return {inside: (dist <= (DELTA+lw)), dist};
}

/**
 * check if a screen point is within a circle,
 * @param drawObj
 * @param sPt
 * @param cc
 * @param def
 * @returns {*} inside or not  and the distance to the center
 */
function isInDrawobjCircle(drawObj, sPt, cc, def) {
    var inside = false;

    if (!drawObj) return {inside};

    var area = getDrawobjCircleArea(drawObj, cc, def);
    if (!area) return {inside};

    var {lineWidth} = drawObj;
    var lw = lineWidth ? Math.floor((lineWidth+1)/2) : 1;
    var {width, center} = area;
    var scrRadius = width * cc.zoomFactor/2;
    var scrCenter = cc.getScreenCoords(center);
    var dist = VisUtil.computeScreenDistance(sPt.x, sPt.y, scrCenter.x, scrCenter.y);

    inside = (dist < (scrRadius + DELTA + lw ));

    return { inside, dist};
}

/**
 * check if a screen point is within a rectangle (or a rectangle with rotate angle)
 * @param drawObj
 * @param sPt
 * @param cc
 * @param def
 * @returns {*} inside or not and the the distance to the center
 */
function isInDrawobjRectangle(drawObj, sPt, cc, def) {
    var inside = false;

    if (!drawObj) return {inside};

    var {pts, width, height, angle, angleUnit, lineWidth, isOnWorld = false, isCenter = false} = drawObj;
    var unitType = drawObj.unitType || def.unitType ||  ShapeDataObj.UnitType.PIXEL;
    var centerPt;
    var corners;
    var dist, w, h, rAngle;

    if (pts.length === 1) {
        // return center, width (screen), height (screen), slanted angle (radian)
        var sRect = rectOnImage(pts[0], isCenter, cc, width, height, unitType, isOnWorld);

        if (!sRect) return {inside};
        centerPt = cc.getImageCoords(sRect.centerPt);

        w = sRect.width;
        h = sRect.height;
        rAngle = sRect.angle;
    } else {
        var pt1 = cc.getImageCoords(pts[0]);
        var pt2 = cc.getImageCoords(pts[1]);

        w = Math.abs(pt1.x-pt2.x);
        h = Math.abs(pt1.y-pt2.y);
        centerPt = makeImagePt((pt1.x+pt2.x)/2, (pt1.y+pt2.y)/2);
        rAngle = 0.0;
    }

    // angle is for screen pixel coordinate
    if (isCenter && angleUnit) {
        if (angleUnit === ShapeDataObj.UnitType.ARCSEC) {
            rAngle -= convertAngle('arcsec', 'radian', angle);
        } else if (angleUnit === ShapeDataObj.UnitType.IMAGE_PIXEL) {
            rAngle -= cc.zoomFactor * angle;
        } else {
            rAngle -= angle;
        }
    }
    rAngle -= get(drawObj, 'renderOptiosn.rotAngle', 0.0);

    corners = [ [-w/2, +h/2],   // in image coordinate
                [+w/2, +h/2],
                [+w/2, -h/2],
                [-w/2, -h/2] ].map((pt) => {
                    var {offx, offy} = rotatePointAroundOrigin(pt, rAngle);

                    return makeImagePt(centerPt.x + offx, centerPt.y + offy);
                });


    inside = isWithinPolygon(sPt, corners, cc, lineWidth);

    var scrCenter = cc.getScreenCoords(centerPt);
    if (inside) {
        dist = VisUtil.computeScreenDistance(sPt.x, sPt.y, scrCenter.x, scrCenter.y);
    }
    return {inside, dist};

}

/**
 * check if a screen point is within a (rotated) ellipse
 * @param drawObj
 * @param sPt
 * @param cc
 * @param def
 * @returns {*} inside or not and the distance to the center
 */
function isInDrawobjEllipse(drawObj, sPt, cc, def) {
    var inside = false;

    if (!drawObj) return {inside};

    var {pts, radius1, radius2, angle, angleUnit, lineWidth, isOnWorld = true} = drawObj;
    var unitType = drawObj.unitType || def.unitType ||  ShapeDataObj.UnitType.PIXEL;
    var centerPt;
    var dist, w, h, rAngle;
    var lw = lineWidth ? Math.floor((lineWidth+1)/2) : 1;

     // return center, width (screen), height (screen), slanted angle (radian)
    var sRect = rectOnImage(pts[0], true, cc, radius1*2, radius2*2, unitType, isOnWorld);

    if (!sRect) return {inside};
    centerPt = cc.getScreenCoords(sRect.centerPt);

    w = sRect.width * cc.zoomFactor/2 + lw;    // in screen pixel, r1
    h = sRect.height * cc.zoomFactor/2 + lw;   // r2
    rAngle = -sRect.angle;

    if (angleUnit) {
        if (angleUnit === ShapeDataObj.UnitType.ARCSEC) {
            rAngle += convertAngle('arcsec', 'radian', angle);
        } else if (angleUnit === ShapeDataObj.UnitType.IMAGE_PIXEL) {
            rAngle += cc.zoomFactor * angle;
        } else {
            rAngle += angle;
        }
    }
    rAngle += get(drawObj, 'renderOptiosn.rotAngle', 0.0);

    var newx, newy;
    var cos = Math.cos(rAngle);
    var sin = Math.sin(rAngle);
    var offx = sPt.x - centerPt.x;
    var offy = sPt.y - centerPt.y;

    newx = (offx * cos + offy * sin)/w;    // in screen pixel
    newy = (-offx * sin + offy * cos)/h;

    inside = (((newx * newx) + (newy * newy)) <= 1.0); // (xcos(a) + ysin(a))^2/r1^2 + (xsin(a)-ycos(a))^2/r2^2 = 1.0

    if (inside) {
        dist = VisUtil.computeScreenDistance(sPt.x, sPt.y, centerPt.x, centerPt.y);
    }

    return {inside, dist};
}

/**
 * check if a screen point is within the polygon
 * @param drawObj
 * @param sPt
 * @param cc
 * @returns {*} inside or not and the distance to the center of the polygon
 */
function isInDrawobjPolygon(drawObj, sPt, cc) {
    var inside = false;
    var {pts, lineWidth} = drawObj;
    var corners;
    var dist;

    if (pts.length < 3) return {inside};
    corners = pts.map((pt) => cc.getScreenCoords(pt));

    inside = isWithinPolygon(sPt, corners, cc, lineWidth);

    if (inside) {
        var sumPt = corners.reduce((prev, pt) => {
            prev.x += pt.x;
            prev.y += pt.y;
            return prev;
        }, {x: 0, y: 0});

        dist = VisUtil.computeScreenDistance(sPt.x, sPt.y, sumPt.x/corners.length, sumPt.y/corners.length);
    }

    return {inside, dist};
}

/**
 * rendering option for highlight box
 * @param dObj
 */
export function makeShapeHighlightRenderOptions( dObj ) {
    set(dObj, 'color', '#DAA520' );
    set(dObj, 'lineWidth', 2);

    set(dObj, 'renderOptions', { lineDash: [8, 5, 2, 5]});
}

/**
 * generate rectangle drawObj to wrap around the highlighted object
 * @param drawObj
 * @param cc
 * @param def
 * @returns {*}
 */
export function makeHighlightShapeDataObj(drawObj, cc, def = {}) {
    var area = getDrawobjArea(drawObj, cc, def);

    if (!area) return null;

    var {lineWidth} = drawObj;
    var {width, height, center} = area;
    var inc = lineWidth ? (lineWidth+1)*2 : DELTA*2;
    var rectObj = ShapeDataObj.makeRectangleByCenter(cc.getWorldCoords(center), width, height,
                                                     ShapeDataObj.UnitType.IMAGE_PIXEL,
                                                     0.0, ShapeDataObj.UnitType.ARCSEC, false);

    rectObj.inc = inc;
    makeShapeHighlightRenderOptions( rectObj );
    return rectObj;
}

