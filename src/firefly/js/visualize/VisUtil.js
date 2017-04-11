/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 *
 * @author Trey, Booth and many more
 */


import {isArray} from 'lodash';
import pointInPolygon from 'point-in-polygon';
import Enum from 'enum';
import CoordinateSys from './CoordSys.js';
import {CCUtil, CysConverter} from './CsysConverter.js';
import DrawOp from './draw/DrawOp.js';
import {primePlot} from './PlotViewUtil.js';
import {doConv} from '../astro/conv/CoordConv.js';
import Point, {makeImageWorkSpacePt, makeImagePt, makeScreenPt,
               makeWorldPt, makeDevicePt, isValidPoint} from './Point.js';


export const DtoR = Math.PI / 180.0;
export const RtoD = 180.0 / Math.PI;

export const toDegrees = (angle) => angle * (180 / Math.PI);
export const toRadians = (angle) => (angle * Math.PI) / 180;


export const FullType= new Enum(['ONLY_WIDTH', 'WIDTH_HEIGHT', 'ONLY_HEIGHT', 'SMART']);


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


/**
 *
 * @param {number} x1
 * @param {number} y1
 * @param {number} x2
 * @param {number} y2
 * @return {number}
 */
export const computeScreenDistance= function (x1, y1, x2, y2) {
    const deltaXSq = (x1 - x2) * (x1 - x2);
    const  deltaYSq = (y1 - y2) * (y1 - y2);
    return Math.sqrt(deltaXSq + deltaYSq);
};

/**
 * compute the distance on the sky between two world points
 * @param p1 WorldPt
 * @param p2 WorldPt
 * @return {number}
 */
const computeDistance= function(p1, p2) {
    const lon1Radius = p1.getLon() * DtoR;
    const lon2Radius = p2.getLon() * DtoR;
    const lat1Radius = p1.getLat() * DtoR;
    const lat2Radius = p2.getLat() * DtoR;
    let cosine = Math.cos(lat1Radius) * Math.cos(lat2Radius) *
                 Math.cos(lon1Radius - lon2Radius) +
                 Math.sin(lat1Radius) * Math.sin(lat2Radius);

    if (Math.abs(cosine) > 1.0) cosine = cosine / Math.abs(cosine);
    return RtoD * Math.acos(cosine);
};

/**
 *
 * @param p1 {Pt}
 * @param p2 {Pt}
 * @return {number}
 */
const computeSimpleDistance= function(p1, p2) {
    const dx = p1.x - p2.x;
    const dy = p1.y - p2.y;
    return Math.sqrt(dx * dx + dy * dy);
};



/**
 * Convert from one coordinate system to another.
 *
 * @param {object} wpt the world point to convert
 * @param {object} to CoordSys, the coordinate system to convert to
 * @return WorldPt the world point in the new coordinate system
 */
export function convert(wpt, to= CoordinateSys.EQ_J2000) {
    const from = wpt.getCoordSys();
    if (!to || from===to) return wpt;

    const tobs=  (from===CoordinateSys.EQ_B1950) ? 1983.5 : 0;
    const ll = doConv(
                          from.getJsys(), from.getEquinox(),
                          wpt.getLon(), wpt.getLat(),
                          to.getJsys(), to.getEquinox(), tobs);
    return makeWorldPt(ll.lon, ll.lat, to);
}

function convertToJ2000(wpt) { return convert(wpt); }

/**
 * Find an approximate central point and search radius for a group of positions
 *
 * @param inPoints array of points for which the central point is desired
 * @return {{centralPoint:WorldPt, maxRadius: Number}}
 */
export function computeCentralPointAndRadius(inPoints) {
    let lon;
    let radius;
    let maxRadius = Number.NEGATIVE_INFINITY;

    const points= inPoints.map((wp) => convertToJ2000(wp));


    /* get max,min of lon and lat */
    let maxLon = Number.NEGATIVE_INFINITY;
    let minLon = Number.POSITIVE_INFINITY;
    let maxLat = Number.NEGATIVE_INFINITY;
    let minLat = Number.POSITIVE_INFINITY;

    points.forEach((pt) => {
        if (pt.x > maxLon) {
            maxLon = pt.x;
        }
        if (pt.x < minLon) {
            minLon = pt.x;
        }
        if (pt.y > maxLat) {
            maxLat = pt.y;
        }
        if (pt.y < minLat) {
            minLat = pt.y;
        }
    });
    if (maxLon - minLon > 180) {
        minLon = 360 + minLon;
    }
    lon = (maxLon + minLon) / 2;
    if (lon > 360) lon -= 360;
    const lat = (maxLat + minLat) / 2;

    const centralPoint = makeWorldPt(lon, lat);


    points.forEach((pt) => {
        radius = computeDistance(centralPoint,
                                 makeWorldPt(pt.x, pt.y));
        if (maxRadius < radius) {
            maxRadius = radius;
        }

    });

    return {centralPoint, maxRadius};
}


/**
 * Compute position angle
 *
 * @param {number} ra0  the equatorial RA in degrees of the first object
 * @param {number} dec0 the equatorial DEC in degrees of the first object
 * @param {number} ra   the equatorial RA in degrees of the second object
 * @param {number} dec  the equatorial DEC in degrees of the second object
 * @return {number} position angle in degrees between the two objects
 */
function getPositionAngle(ra0, dec0, ra, dec) {
    let sind, sinpa, cospa;

    const alf = ra * DtoR;
    const alf0 = ra0 * DtoR;
    const del = dec * DtoR;
    const del0 = dec0 * DtoR;

    const sd0 = Math.sin(del0);
    const sd = Math.sin(del);
    const cd0 = Math.cos(del0);
    const cd = Math.cos(del);
    const cosda = Math.cos(alf - alf0);
    const cosd = sd0 * sd + cd0 * cd * cosda;
    let dist = Math.acos(cosd);
    let pa = 0.0;
    if (dist > 0.0000004) {
        sind = Math.sin(dist);
        cospa = (sd * cd0 - cd * sd0 * cosda) / sind;
        if (cospa > 1.0) cospa = 1.0;
        if (cospa < -1.0) cospa = -1.0;
        sinpa = cd * Math.sin(alf - alf0) / sind;
        pa = Math.acos(cospa) * RtoD;
        if (sinpa < 0.0) pa = 360.0 - (pa);
    }
    dist *= RtoD;
    if (dec0===90) pa = 180.0;
    if (dec0===-90) pa = 0.0;

    return pa;
};

/**
 * Rotates the given input position and returns the result. The rotation
 * applied to positionToRotate is the one which maps referencePosition to
 * rotatedReferencePosition.
 * @author Serge Monkewitz
 * @param {WorldPt} referencePosition the reference position to start
 * @param {WorldPt} rotatedReferencePosition the rotated reference position
 * @param {WorldPt} positionToRotate the position to be moved and rotated
 * @return {WorldPt} the result new world position by applying the same displacement as the one from
 *                   referencePosition to rotatedReferencePosition
 */
export function getTranslateAndRotatePosition(referencePosition, rotatedReferencePosition, positionToRotate) {
    // Extract coordinates and transform to radians
     const ra1 = toRadians(referencePosition.getLon());
     const dec1 = toRadians(referencePosition.getLat());
     const ra2 = toRadians(rotatedReferencePosition.getLon());
     const dec2 = toRadians(rotatedReferencePosition.getLat());
     const ra = toRadians(positionToRotate.getLon());
     const dec = toRadians(positionToRotate.getLat());

    // Compute (x, y, z), the unit vector in R3 corresponding to positionToRotate
     const cos_ra = Math.cos(ra);
     const sin_ra = Math.sin(ra);
     const cos_dec = Math.cos(dec);
     const sin_dec = Math.sin(dec);

     let x = cos_ra * cos_dec;
     let y = sin_ra * cos_dec;
     let z = sin_dec;

    // The rotation that maps referencePosition to rotatedReferencePosition
    // can be broken down into 3 rotations. The first is a rotation by an
    // angle of -ra1 around the z axis. The second is a rotation around the
    // y axis by an angle equal to (dec1 - dec2), and the last is around the
    // the z axis by ra2. We compute the individual rotations by
    // multiplication with the corresponding 3x3 rotation matrix (see
    // https://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations)

    // Rotate by angle theta = -ra1 around the z axis:
    //
    // [ x1 ]   [ cos(ra1) -sin(ra1) 0 ]   [ x ]
    // [ y1 ] = [ sin(ra1) cos(ra1)  0 ] * [ y ]
    // [ z1 ]   [ 0        0         1 ]   [ z ]
     let cos_theta = Math.cos(-ra1);
     let sin_theta = Math.sin(-ra1);
     let x1 = cos_theta * x - sin_theta * y;
     let y1 = sin_theta * x + cos_theta * y;
     let z1 = z;

    // Rotate by angle theta = (dec1 - dec2) around the y axis:
    //
    // [ x ]   [ cos(dec1 - dec2)  0 sin(dec1 - dec2) ]   [ x1 ]
    // [ y ] = [ 0                 1 0                ] * [ y1 ]
    // [ z ]   [ -sin(dec1 - dec2) 0 cos(dec1 - dec2) ]   [ z1 ]
    cos_theta = Math.cos(dec1 - dec2);
    sin_theta = Math.sin(dec1 - dec2);
    x = cos_theta * x1 + sin_theta * z1;
    y = y1;
    z = -sin_theta * x1 + cos_theta * z1;

    // Rotate by angle theta = ra2 around the z axis:
    //
    // [ x1 ]   [ cos(ra2) -sin(ra2) 0 ]   [ x ]
    // [ y1 ] = [ sin(ra2) cos(ra2)  0 ] * [ y ]
    // [ z1 ]   [ 0        0         1 ]   [ z ]
    cos_theta = Math.cos(ra2);
    sin_theta = Math.sin(ra2);
    x1 = cos_theta * x - sin_theta * y;
    y1 = sin_theta * x + cos_theta * y;
    z1 = z;

    // Convert the unit vector result back to a WorldPt.
    const d = x1 * x1 + y1 * y1;
    let lon = 0.0;
    let lat = 0.0;
    if (d !== 0.0) {
        lon = toDegrees(Math.atan2(y1, x1));
        if (lon < 0.0) {
            lon += 360.0;
        }
    }
    if (z1 !== 0.0) {
        lat = toDegrees(Math.atan2(z1, Math.sqrt(d)));
        if (lat > 90.0) {
            lat = 90.0;
        } else if (lat < -90.0) {
            lat = -90.0;
        }
    }
    return makeWorldPt(lon, lat);
}

/**
 * Compute new position given a position and a distance and position angle
 *
 * @param {number} ra   the equatorial RA in degrees of the first object
 * @param {number} dec  the equatorial DEC in degrees of the first object
 * @param {number} dist the distance in degrees to the second object
 * @param {number} phi  the position angle in degrees to the second object
 * @return {WorldPt} WorldPt of the new object
 */
const getNewPosition= function(ra, dec, dist, phi) {
    let tmp;
    let ra1;

    ra *= DtoR;
    dec *= DtoR;
    dist *= DtoR;
    phi *= DtoR;

    tmp = Math.cos(dist) * Math.sin(dec) + Math.sin(dist) * Math.cos(dec) * Math.cos(phi);
    const newdec = Math.asin(tmp);
    const dec1 = newdec * RtoD;

    tmp = Math.cos(dist) * Math.cos(dec) - Math.sin(dist) * Math.sin(dec) * Math.cos(phi);
    tmp /= Math.cos(newdec);
    const deltaRa = Math.acos(tmp);
    if (Math.sin(phi) < 0.0) {
        ra1 = ra - deltaRa;
    }
    else {
        ra1 = ra + deltaRa;
    }
    ra1 *= RtoD;
    return makeWorldPt(ra1, dec1);
};


export const getRotationAngle= function(plot) {
    let retval = 0;
    const iWidth = plot.dataWidth;
    const iHeight = plot.dataHeight;
    const ix = iWidth / 2;
    const iy = iHeight / 2;
    const cc= CysConverter.make(plot);
    const wptC = cc.getWorldCoords(makeImageWorkSpacePt(ix, iy));
    const wpt2 = cc.getWorldCoords(makeImageWorkSpacePt(ix, iHeight/4));
    if (wptC && wpt2) {
        if (wptC.y > wpt2.y) {
            retval = getPositionAngle(wpt2.getLon(), wpt2.getLat(), wptC.getLon(), wptC.getLat());
        }
        else {
            retval = getPositionAngle(wptC.getLon(), wptC.getLat(), wpt2.getLon(), wpt2.getLat());
        }
    }
    return retval;
};

/**
 * Is the image positioned so that north is up.
 * @param {WebPlot} plot
 * @return {boolean}
 */
export function isPlotNorth(plot) {
    let retval= false;
    const ix = plot.dataWidth/ 2;
    const iy = plot.dataHeight/ 2;
    const cc= CysConverter.make(plot);
    const wpt1 = cc.getWorldCoords(makeImageWorkSpacePt(ix, iy));
    if (wpt1) {
        const cdelt1 = cc.getImagePixelScaleInDeg();
        const wpt2 = makeWorldPt(wpt1.getLon(), wpt1.getLat() + (Math.abs(cdelt1) / plot.zoomFactor) * (5));
        const spt1 = cc.getScreenCoords(wpt1);
        const spt2 = cc.getScreenCoords(wpt2);
        if (spt1 && spt2) {
            retval = (spt1.x===spt2.x && spt1.y > spt2.y);
        }
    }
    return retval;
}


/**
 * When plot is rotated north is east on the left hand side.  This helps determine if a plot is flipped.
 * @param plot
 * @return {boolean}
 */
export function isEastLeft(plot) {
    const iy = plot.dataHeight/2;
    const cc= CysConverter.make(plot);
    const wpt1 = cc.getWorldCoords(makeImagePt(plot.dataWidth-1, iy));
    if (wpt1) {
        const wpt2 = cc.getWorldCoords(makeImagePt(1, iy));
        if (wpt2) return wpt2.x > wpt1.x;
    }
    return true;
}


const getEstimatedFullZoomFactor= function(fullType, dataWidth, dataHeight,
                                                  screenWidth, screenHeight, tryMinFactor=-1) {
    let zFact;
    if (fullType===FullType.ONLY_WIDTH || screenHeight <= 0 || dataHeight <= 0) {
        zFact =  screenWidth /  dataWidth;
    } else if (fullType===FullType.ONLY_HEIGHT || screenWidth <= 0 || dataWidth <= 0) {
        zFact =  screenHeight /  dataHeight;
    } else {
        const zFactW =  screenWidth /  dataWidth;
        const zFactH =  screenHeight /  dataHeight;
        if (fullType===FullType.SMART) {
            zFact = zFactW;
            if (zFactW > Math.max(tryMinFactor, 2)) {
                zFact = Math.min(zFactW, zFactH);
            }
        } else {
            zFact = Math.min(zFactW, zFactH);
        }
    }
    return zFact;
};



/**
 * Test to see if two rectangles intersect
 * @param {number} x0 the first point x, top left
 * @param {number} y0 the first point y, top left
 * @param {number} w0 the first rec width
 * @param {number} h0 the first rec height
 * @param {number} x the second point x, top left
 * @param {number} y the second point y, top left
 * @param {number} w h the second rec width
 * @param {number} h the second rec height
 * @return {boolean} true if rectangles intersect
 */
export const intersects= function(x0, y0, w0, h0, x, y, w, h) {
    if (w0 <= 0 || h0 <= 0 || w <= 0 || h <= 0) {
        return false;
    }
    return (x + w > x0 && y + h > y0 && x < x0 + w0 && y < y0 + h0);
};


/**
 * test to see if a point is in a rectangle
 * @param x0 the point x of the rec, top left
 * @param y0 the point y of the rec, top left
 * @param w0 the rec width
 * @param h0 the rec height
 * @param x the second point x, top left
 * @param y the second point y, top left
 * @return {boolean} true if rectangles intersect
 */
export const contains= function(x0, y0, w0, h0, x, y) {
    return (x >= x0 && y >= y0 && x < x0 + w0 && y < y0 + h0);
};

/**
 * test to see if the first rectangle contains the second rectangle
 * @param x0 the point x of the rec, top left
 * @param y0 the point y of the rec, top left
 * @param w0 the rec width
 * @param h0 the rec height
 * @param x the second point x, top left
 * @param y the second point y, top left
 * @param w h the second rec width
 * @param h the second rec height
 * @return {boolean} true if rectangles intersect
 */
export const containsRec= function(x0, y0, w0, h0, x, y, w, h) {
     return contains(x0,y0,w0,h0,x,y) && contains(x0,y0,w0,h0,x+w,y+h);
};

export const containsCircle= function(x, y, centerX, centerY, radius) {
    return Math.pow((x - centerX), 2) + Math.pow((y - centerY), 2) < radius * radius;
};

const getArrowCoords= function(x1, y1, x2, y2) {

    const barbLength = 10;

    /* compute shaft angle from arrowhead to tail */
    const deltaY = y2 - y1;
    const deltaX = x2 - x1;
    const shaftAngle = Math.atan2(deltaY, deltaX);
    const barbAngle = shaftAngle - 20 * Math.PI / 180; // 20 degrees from shaft
    const barbX = x2 - barbLength * Math.cos(barbAngle);  // end of barb
    const barbY = y2 - barbLength * Math.sin(barbAngle);

    let extX = x2 + 6;
    let extY = y2 + 6;

    const diffX = x2 - x1;
    const mult = ((y2 < y1) ? -1 : 1);
    if (diffX===0) {
        extX = x2;
        extY = y2 + mult * 14;
    } else {
        const slope = ( y2 - y1) / ( x2 - x1);
        if (slope >= 3 || slope <= -3) {
            extX = x2;
            extY = y2 + mult * 14;
        } else if (slope < 3 || slope > -3) {
            extY = y2 - 6;
            if (x2 < x1) {
                extX = x2 - 8;
            } else {
                extX = x2 + 2;
            }
        }

    }

    return {
        x1, y1, x2, y2,
        barbX1 : x2,
        barbY1 : y2,
        barbX2 : barbX,
        barbY2 : barbY,
        textX : extX,
        textY : extY
    };
};


/**
 * Get the bounding of of the array of points
 * @param {Array.<{x:number, y:number}>} ptAry
 * @return {{x, y, w: number, h: number}}
 */
export function getBoundingBox(ptAry) {
    const sortX= ptAry.map( (pt) => pt.x).sort( (v1,v2) => v1-v2);
    const sortY= ptAry.map( (pt) => pt.y).sort( (v1,v2) => v1-v2);
    const minX= sortX[0];
    const minY= sortY[0];
    const maxX= sortX[sortX.length-1];
    const maxY= sortY[sortY.length-1];
    return {x:minX, y:minY, w:Math.abs(maxX-minX), h:Math.abs(maxY-minY)};
}


/**
 *
 * @param {object} selection obj with two properties pt0 & pt1
 * @param {WebPlot} plot web plot
 * @param objList array of DrawObj (must be an array and contain a getCenterPt() method)
 * @return {Array} indexes from the objList array that are selected
 */
export function getSelectedPts(selection, plot, objList) {
    const selectedList= [];
    if (selection && plot && objList && objList.length) {
        const cc= CysConverter.make(plot);
        const pt0= cc.getDeviceCoords(selection.pt0);
        const pt1= cc.getDeviceCoords(selection.pt1);
        if (!pt0 || !pt1) return selectedList;

        const x= Math.min( pt0.x,  pt1.x);
        const y= Math.min(pt0.y, pt1.y);
        const width= Math.abs(pt0.x-pt1.x);
        const height= Math.abs(pt0.y-pt1.y);
        objList.forEach( (obj,idx) => {
            const testObj = cc.getDeviceCoords(DrawOp.getCenterPt(obj));
            if (testObj && contains(x,y,width,height,testObj.x, testObj.y)) {
                selectedList.push(idx);
            }
        });
    }
    return selectedList;
}

/**
 * get the world point at the center of the plot
 * @param {WebPlot} plot
 * @return {WorldPt}
 */
export function getCenterPtOfPlot(plot) {
    if (!plot) return null;
    const ip= makeImagePt(plot.dataWidth/2,plot.dataHeight/2);
    return CCUtil.getWorldCoords(plot,ip);
}

/**
 * Return a WorldPt that is offset by the relative ra and dec from the passed in position
 * @param {WorldPt} pos1
 * @param {number} offsetRa
 * @param {number} offsetDec
 * @return {WorldPt}
 */
function calculatePosition(pos1, offsetRa, offsetDec ) {
    const ra = toRadians(pos1.getLon());
    const dec = toRadians(pos1.getLat());
    const de = toRadians(offsetRa/3600.0); // east
    const dn = toRadians(offsetDec)/3600.0; // north

    const rhat= [];
    const shat= [];
    const uhat= [];
    let ra2, dec2;

    const cosRa  = Math.cos(ra);
    const sinRa  = Math.sin(ra);
    const cosDec = Math.cos(dec);
    const sinDec = Math.sin(dec);

    const cosDe = Math.cos(de);
    const sinDe = Math.sin(de);
    const cosDn = Math.cos(dn);
    const sinDn = Math.sin(dn);


    rhat[0] = cosDe * cosDn;
    rhat[1] = sinDe * cosDn;
    rhat[2] = sinDn;

    shat[0] = cosDec * rhat[0] - sinDec * rhat[2];
    shat[1] = rhat[1];
    shat[2] = sinDec * rhat[0] + cosDec * rhat[2];

    uhat[0] = cosRa * shat[0] - sinRa * shat[1];
    uhat[1] = sinRa * shat[0] + cosRa * shat[1];
    uhat[2] = shat[2];

    const uxy = Math.sqrt(uhat[0] * uhat[0] + uhat[1] * uhat[1]);
    if (uxy>0.0) {
        ra2 = Math.atan2(uhat[1], uhat[0]);
    }
    else {
        ra2 = 0.0;
    }
    dec2 = Math.atan2(uhat[2],uxy);

    ra2  = toDegrees(ra2);
    dec2 = toDegrees(dec2);

    if (ra2 < 0.0) ra2 +=360.0;

    return makeWorldPt(ra2, dec2);
}

/**
 * Find the corners of a bounding box given the center and the radius
 * of a circle
 *
 * @param center WorldPt the center of the circle
 * @param radius  in arcsec
 * @return object with corners
 */
const getCorners= function(center, radius) {
    const posLeft = calculatePosition(center, +radius, 0.0);
    const posRight = calculatePosition(center, -radius, 0.0);
    const posUp = calculatePosition(center, 0.0, +radius);
    const posDown = calculatePosition(center, 0.0, -radius);
    const upperLeft = makeWorldPt(posLeft.getLon(), posUp.getLat());
    const upperRight = makeWorldPt(posRight.getLon(), posUp.getLat());
    const lowerLeft = makeWorldPt(posLeft.getLon(), posDown.getLat());
    const lowerRight = makeWorldPt(posRight.getLon(), posDown.getLat());
    return {upperLeft, upperRight, lowerLeft, lowerRight};
};


/**
 * Return the same point using the WorldPt object.  the x,y value is the same but a world point is return with the
 * proper coordinate system.  If a WorldPt is passed the same point is returned.
 * <i>Important</i>: This method should not be used to convert between coordinate systems.
 * Example- a ScreenPt with (1,2) will return as a WorldPt with (1,2)
 * @param pt the point to translate
 * @return WorldPt the World point with the coordinate system set
 */
const getWorldPtRepresentation= function(pt) {
    if (!isValidPoint(pt)) return null;

    let retval= null;
    switch (pt.type) {
        case Point.IM_WS_PT:
            retval= makeWorldPt(pt.x,pt.y, CoordinateSys.PIXEL);
            break;
        case Point.SPT:
            retval= makeWorldPt(pt.x,pt.y, CoordinateSys.SCREEN_PIXEL);
            break;
        case Point.IM_PT:
            retval= makeWorldPt(pt.x,pt.y, CoordinateSys.PIXEL);
            break;
        case Point.W_PT:
            retval=  pt;
            break;
    }
    return retval;
};

const makePt= function(type,  x, y) {
    let retval= null;
    switch (type) {
        case Point.IM_WS_PT:
            retval= makeImageWorkSpacePt(x,y);
            break;
        case Point.SPT:
            retval= makeScreenPt(x,y);
            break;
        case Point.IM_PT:
            retval= makeImagePt(x,y);
            break;
        case Point.W_PT:
            retval= makeWorldPt(x,y);
            break;
    }
    return retval;
};

/**
 * convert angle value of one unit to that of another unit
 * @param {string} from 'degree' or 'deg', 'arcmin', 'arcsec', 'radian' case insensitive
 * @param {string} to 'degree' or 'deg', 'arcmin', 'arcsec', 'radian' case insensitive
 * @param {*} angle  number or string
 * @returns {number}
 */
export function convertAngle(from, to, angle) {
    const angleUnit = [['deg', 'degree'], 'arcmin', 'arcsec', 'radian'];
    const rIdx = angleUnit.indexOf('radian');
    let fromIdx, toIdx;
    let numAngle = (typeof angle === 'string') ? parseFloat(angle) : angle;
    const unitIdx = (unit) => angleUnit.findIndex( (au) => (isArray(au) ? au.includes(unit) : au === unit));

    if (((fromIdx = unitIdx(from.toLowerCase())) < 0) ||       // invalid unit
        ((toIdx = unitIdx(to.toLowerCase())) < 0)) {
        return numAngle;
    } else {
        if ( fromIdx === rIdx ) {
            numAngle = numAngle * 180.0/Math.PI;
            fromIdx = 0;
        }

        if (toIdx === rIdx) {
            numAngle = numAngle * Math.PI/180.0;
            toIdx = 0;
        }
        return numAngle * Math.pow(60.0, (toIdx - fromIdx));
    }
}


export function formatFlux(value, plot, band) {
    if (typeof value==='undefined') return '';
    const fluxUnits= plot.webFitsData[band.value].fluxUnits;
    return `${formatFluxValue(value)} ${fluxUnits}`;

}

export function formatFluxValue(value) {
    const absV= Math.abs(value);
    return (absV>1000||absV<.01) ?
        value.toExponential(6).replace('e+', 'E') :
        value.toFixed(6);
}

/**
 * find a point on the plot that is top and left but is still in view and on the image.
 * If the image is off the screen the return undefined.
 * @param {PlotView} pv
 * @param {number} xOff
 * @param {number} yOff
 * @return {DevicePt} the found point
 */
export function getTopmostVisiblePoint(pv,xOff, yOff) {
    const plot= primePlot(pv);
    const cc= CysConverter.make(plot);
    const ipt= cc.getImageCoords(makeDevicePt(xOff,yOff));
    if (isImageCoveringArea(pv,ipt,2,2)) return ipt;


    const {dataWidth,dataHeight}= plot;
    const {viewDim} = pv;

    const lineSegs= [
       {pt1: cc.getDeviceCoords(makeImagePt(0,0)), pt2: cc.getDeviceCoords(makeImagePt(dataWidth,0))},
       {pt1: cc.getDeviceCoords(makeImagePt(dataWidth,0)), pt2: cc.getDeviceCoords(makeImagePt(dataWidth,dataHeight))},
       {pt1: cc.getDeviceCoords(makeImagePt(dataWidth,dataHeight)), pt2: cc.getDeviceCoords(makeImagePt(0,dataHeight))},
       {pt1: cc.getDeviceCoords(makeImagePt(0,dataHeight)), pt2: cc.getDeviceCoords(makeImagePt(0,0))}
    ];

    const foundSegs= lineSegs
        .filter((lineSeg) => {
                 const {pt1,pt2}= lineSeg;
                 const iPt= findIntersectionPt(pt1.x,pt1.y,pt2.x,pt2.y, 0,0,viewDim.width-1,0);
                 return iPt && iPt.onSeg1 && iPt.onSeg2;
             })
        .sort( (l1, l2) => l1.pt1.x - l2.pt1.x);

    if (foundSegs[0]) {
        const pt= findIntersectionPt(foundSegs[0].pt1.x,foundSegs[0].pt1.y,
                                     foundSegs[0].pt2.x,foundSegs[0].pt2.y, 0,0,viewDim.width-1,0);
        return makeDevicePt(pt.x+xOff, pt.y+yOff);
    }

    const zXoff= xOff/plot.zoomFactor;
    const zYoff= xOff/plot.zoomFactor;

    const tryPts= [
        makeImagePt(1+zXoff,1+zXoff),
        makeImagePt(plot.dataWidth-zXoff,1+zYoff),
        makeImagePt(plot.dataWidth-zXoff,plot.dataHeight-zYoff),
        makeImagePt(1+zXoff, plot.dataHeight-zYoff),
    ];


    const highPts= tryPts
        .map( (p) => cc.getDeviceCoords(p) )
        .filter( (p) => cc.pointOnDisplay(p))
        .sort( (p1,p2) => p1.y!==p2.y ? p1.y - p2.y : p1.x - p2.x);

    return highPts[0];
}


/**
 * return true if the image is completely covering the area passed. The width and height are in Device coordinate
 * system.
 * @param {PlotView} pv
 * @param {SimplePoint} pt
 * @param {number} width in device coordinates
 * @param {number} height in device coordinates
 * @return {boolean} true if covering
 */
export function isImageCoveringArea(pv,pt, width,height) {

    const plot= primePlot(pv);
    const cc= CysConverter.make(plot);
    pt= cc.getDeviceCoords(pt);
    const testPts= [
        makeDevicePt(pt.x,pt.y),
        makeDevicePt(pt.x+width,pt.y),
        makeDevicePt(pt.x+width,pt.y+height),
        makeDevicePt(pt.x,pt.y+height),
    ];

    const polyPts= [
        cc.getDeviceCoords(makeImagePt(1,1)),
        cc.getDeviceCoords(makeImagePt(plot.dataWidth,1)),
        cc.getDeviceCoords(makeImagePt(plot.dataWidth,plot.dataHeight)),
        cc.getDeviceCoords(makeImagePt(1, plot.dataHeight))
    ];


    const polyPtsAsArray= polyPts.map( (p) => [p.x,p.y]);

    return testPts.every( (p) => pointInPolygon([p.x,p.y], polyPtsAsArray));
}

/**
 * Find the point at intersection of two line segments.
 * If the lines do intersect then return an object with the intersection point x,y and
 * two booleans to represent it the intersection point is on each line segment.
 * Return false if the lines do not intersect.
 * @param {number} seg1x1 - line segment 1 first point x
 * @param {number} seg1y1 - line segment 1 first point y
 * @param {number} seg1x2 - line segment 1 second point x
 * @param {number} seg1y2 - line segment 1 second point y
 * @param {number} seg2x1 - line segment 2 first point x
 * @param {number} sec2y2 - line segment 2 first point y
 * @param {number} seg2x2 - line segment 2 second point x
 * @param {number} seg2y2 - line segment 2 second point y
 * @return {{x: number, y:number, onSeg1:boolean, onSeg2:boolean} | boolean}
 */
export function findIntersectionPt(seg1x1, seg1y1, seg1x2, seg1y2, seg2x1, sec2y2, seg2x2, seg2y2) {
    const denom = (seg2y2 - sec2y2)*(seg1x2 - seg1x1) - (seg2x2 - seg2x1)*(seg1y2 - seg1y1);
    if (!denom) return false;

    const ua = ((seg2x2 - seg2x1)*(seg1y1 - sec2y2) - (seg2y2 - sec2y2)*(seg1x1 - seg2x1))/denom;
    const ub = ((seg1x2 - seg1x1)*(seg1y1 - sec2y2) - (seg1y2 - seg1y1)*(seg1x1 - seg2x1))/denom;
    return {
        x: seg1x1 + ua*(seg1x2 - seg1x1),
        y: seg1y1 + ua*(seg1y2 - seg1y1),
        onSeg1: ua >= 0 && ua <= 1,
        onSeg2: ub >= 0 && ub <= 1
    };
}



export default {
    DtoR,RtoD,FullType,computeScreenDistance, computeDistance,
    computeSimpleDistance,convert,convertToJ2000,
    computeCentralPointAndRadius, getPositionAngle, getNewPosition,
    getRotationAngle,getTranslateAndRotatePosition,
    isPlotNorth, getEstimatedFullZoomFactor,
    intersects, contains, containsRec,containsCircle,
    getArrowCoords, getSelectedPts, calculatePosition, getCorners,
    makePt, getWorldPtRepresentation, getCenterPtOfPlot, toDegrees
};



