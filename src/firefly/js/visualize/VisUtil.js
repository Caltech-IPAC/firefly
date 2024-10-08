/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * @author Trey, Booth and many more
 */
import {flattenDeep, isArray, isEmpty} from 'lodash';
import CoordinateSys from './CoordSys.js';
import {doConv} from '../astro/conv/CoordConv.js';
import {makeImagePt, makeScreenPt, makeWorldPt, pointEquals} from './Point.js';
import {memorizeUsingMap} from '../util/WebUtil';


/** Constant for conversion Degrees => Radians */
export const DtoR = Math.PI / 180.0;
/** Constant for conversion Radians => Degrees */
export const RtoD = 180.0 / Math.PI;

export const toDegrees = (angle) => angle * (180 / Math.PI);
export const toRadians = (angle) => (angle * Math.PI) / 180;


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
export const computeDistance= (p1, p2) => (p1 && p2) ? computeDistanceAngularDistance(p1.x, p1.y, p2.x, p2.y) : 0;


const computeDistanceAngularDistance= memorizeUsingMap( (lon1,lat1,lon2,lat2) => {
    const lon1Radius = lon1 * DtoR;
    const lon2Radius = lon2 * DtoR;
    const lat1Radius = lat1 * DtoR;
    const lat2Radius = lat2 * DtoR;
    let cosine = Math.cos(lat1Radius) * Math.cos(lat2Radius) *
        Math.cos(lon1Radius - lon2Radius) +
        Math.sin(lat1Radius) * Math.sin(lat2Radius);

    if (Math.abs(cosine) > 1.0) cosine = cosine / Math.abs(cosine);
    return RtoD * Math.acos(cosine);
}, 3000);

/**
 * @param {Point} p1
 * @param {Point} p2
 * @return {number}
 */
export const computeSimpleDistance= function(p1, p2) {
    const dx = p1.x - p2.x;
    const dy = p1.y - p2.y;
    return Math.sqrt(dx * dx + dy * dy);
};


export const computeSimpleSlopeAngle = function (fromPt, toPt) {
    const dx = toPt.x - fromPt.x;
    const dy = toPt.y - fromPt.y;
    return Math.atan2(dy, dx);
};

export const computeSimpleSlope= function (fromPt, toPt) {
    const dx = toPt.x - fromPt.x;
    const dy = toPt.y - fromPt.y;
    return dy/dx;
};

export function getLonDist(lon1,lon2) {
    if (lon1>lon2) {
        const diff= lon1-lon2;
        if (diff<=180) return -diff;
        else return ( (360-lon1) - lon2);
    }
    else {
        const diff= lon2-lon1;
        if (diff<=180) return diff;
        else return -1*( (360-lon2) - lon1);

    }
}

export function getLatDist(lat1,lat2) {
    return -1*(lat1-lat2);
}

/**
 * Convert from one coordinate system to another. No action, if the world point coordinate system is non-celestial.
 *
 * @param {WorldPt} wpt the world point to convert
 * @param {CoordinateSys} to CoordSys, the coordinate system to convert to; defaults to J2000, ignored for non-celestial world points
 * @return {WorldPt|undefined} the world point in the new coordinate system
 */
export function convertCelestial(wpt, to= CoordinateSys.EQ_J2000) {
    if (!wpt) return;
    const from = wpt.getCoordSys();
    if (!to || !from.isCelestial() || from===to) return wpt;

    const tobs=  (from===CoordinateSys.EQ_B1950) ? 1983.5 : 0;
    const ll = doConv(
                          from.getJsys(), from.getEquinox(),
                          wpt.getLon(), wpt.getLat(),
                          to.getJsys(), to.getEquinox(), tobs);
    return makeWorldPt(ll.lon, ll.lat, to);
}

const convertToJ2000= (wpt) => convertCelestial(wpt);

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
 * call computeCentralPointAndRadius in 2 ways first with a flatten version of the 2d array
 * then again with group of points.  This allows us not to overweight a larger group when computing the center.
 *
 * @param {Array.<Array.<WorldPt>>} inPoints2dAry  a 2d array of world points. Each array represents a group of points
 * @param {number} minSizeDeg
 * @return {{centralPoint:WorldPt, avgOfCenters:WorldPt, fovSize:number}}
 */
export function computeCentralPtRadiusAverage(inPoints2dAry,minSizeDeg=.11) {

    const testAry= flattenDeep(inPoints2dAry);

    if (isEmpty(testAry)) return {centralPoint:undefined, fovSize: 0, avgOfCenters:undefined};
    if (isOnePoint(testAry)) return {centralPoint:testAry[0], fovSize: minSizeDeg, avgOfCenters:testAry[0]};

    const {centralPoint, maxRadius}= computeCentralPointAndRadius(testAry);
    if (inPoints2dAry.length===1) return {centralPoint, fovSize:Math.max(maxRadius,minSizeDeg), avgOfCenters:centralPoint};

    const centers= inPoints2dAry
        .map( (ptAry) => isOnePoint(ptAry) ? ptAry[0] : computeCentralPointAndRadius(ptAry).centralPoint)
        .filter((pt) => pt); // filter out undefined centers

    const {centralPoint:avgOfCenters}= computeCentralPointAndRadius(centers);
    return {centralPoint, fovSize: Math.max(maxRadius,minSizeDeg), avgOfCenters};
}

function isOnePoint(wpList) {
    return !wpList.some( (wp) => !pointEquals(wp,wpList[0]));
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
export function getPositionAngle(ra0, dec0, ra, dec) {
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
    const dist = Math.acos(cosd);
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
    // dist *= RtoD;
    if (dec0===90) pa = 180.0;
    if (dec0===-90) pa = 0.0;

    return pa;
}

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
 * UNUSED - but keep around
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






export function getAngleInDeg(cx,cy,x,y) {
    const ptX= Math.round(x)-Math.round(cx);
    const ptY= Math.round(y)-Math.round(cy);
    if (ptY===0) return ptX >= 0 ? 0 : 180;
    if (ptX===0) return ptY >= 0 ? 90 : 270;
    const angle= toDegrees(Math.atan(ptY / ptX));
    return circleAngle(Math.abs(angle),ptX,ptY);
}

function circleAngle(a,x,y) {
    if (x>=0 && y>=0) return a;  //quad 1
    if (x<0 && y>=0) return 180-a; // quad 2
    if (x<0 && y<0) return 180+a; // quad 3
    if (x>=0 && y<0) return 360-a; // quad 4
}






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

export function linesIntersect1(a,b,c,d,p,q,r,s) {
  var det, gamma, lambda;
  det = (c - a) * (s - q) - (r - p) * (d - b);
  if (det === 0) {
    return false;
  } else {
    lambda = ((s - q) * (r - a) + (p - r) * (s - b)) / det;
    gamma = ((b - d) * (r - a) + (c - a) * (s - b)) / det;
    return (0 < lambda && lambda < 1) && (0 < gamma && gamma < 1);
  }
}

export function lineIntersect2(x1,y1,x2,y2, x3,y3,x4,y4) {
    var x=((x1*y2-y1*x2)*(x3-x4)-(x1-x2)*(x3*y4-y3*x4))/((x1-x2)*(y3-y4)-(y1-y2)*(x3-x4));
    var y=((x1*y2-y1*x2)*(y3-y4)-(y1-y2)*(x3*y4-y3*x4))/((x1-x2)*(y3-y4)-(y1-y2)*(x3-x4));
    if (isNaN(x)||isNaN(y)) {
        return false;
    } else {
        if (x1>=x2) {
            if (!(x2<=x&&x<=x1)) {return false;}
        } else {
            if (!(x1<=x&&x<=x2)) {return false;}
        }
        if (y1>=y2) {
            if (!(y2<=y&&y<=y1)) {return false;}
        } else {
            if (!(y1<=y&&y<=y2)) {return false;}
        }
        if (x3>=x4) {
            if (!(x4<=x&&x<=x3)) {return false;}
        } else {
            if (!(x3<=x&&x<=x4)) {return false;}
        }
        if (y3>=y4) {
            if (!(y4<=y&&y<=y3)) {return false;}
        } else {
            if (!(y3<=y&&y<=y4)) {return false;}
        }
    }
    return true;
}


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
export function contains(x0, y0, w0, h0, x, y) {
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

export const containsEllipse=function(x, y, centerX, centerY, radius1, radius2) {
    return (Math.pow((x - centerX)/radius1, 2) + Math.pow((y - centerY)/radius2, 2)) < 1;
};

export const getArrowCoords= function(x1, y1, x2, y2) {

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
    const textBaseline= 'top';

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
        textY : extY,
        textBaseline
    };
};


/**
 * Get the bounding of of the array of points
 * @param {Array.<{x:number, y:number}>} ptAry
 * @return {{x: number, y: number, w: number, h: number}}
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
 * @param {object} selection obj with two properties pt0 & pt1
 * @param {WebPlot} plot web plot
 * @param objList array of DrawObj (must be an array and contain a getCenterPt() method)
 * @param selectedShape shape of selected area
 * @return {Array} indexes from the objList array that are selected
 */
// export function getSelectedPts(selection, plot, objList, selectedShape) {
//
//     if (selectedShape === SelectedShape.circle.key) {
//         return getSelectedPtsFromEllipse(selection, plot, objList);
//     } else {
//         return getSelectedPtsFromRect(selection, plot, objList);
//     }
// }

/**
 * get selected points from circular selected area
 * @param selection
 * @param plot
 * @param objList
 * @returns {Array}
 */
// function getSelectedPtsFromEllipse(selection, plot, objList) {
//     const selectedList= [];
//     if (selection && plot && objList && objList.length) {
//         const cc= CysConverter.make(plot);
//         const pt0= cc.getDeviceCoords(selection.pt0);
//         const pt1= cc.getDeviceCoords(selection.pt1);
//         if (!pt0 || !pt1) return selectedList;
//
//         const c_x = (pt0.x + pt1.x)/2;
//         const c_y = (pt0.y + pt1.y)/2;
//         const r1 =  Math.abs(pt0.x-pt1.x)/2;
//         const r2 =  Math.abs(pt0.y-pt1.y)/2;
//
//         objList.forEach( (obj,idx) => {
//             const testObj = cc.getDeviceCoords(DrawOp.getCenterPt(obj));
//
//             if (testObj &&  containsEllipse(testObj.x, testObj.y, c_x, c_y, r1, r2)) {
//                 selectedList.push(idx);
//             }
//         });
//     }
//     return selectedList;
//
// }

/**
 * get selected points from rectangular selected area
 * @param {object} selection obj with two properties pt0 & pt1
 * @param {WebPlot} plot web plot
 * @param objList array of DrawObj (must be an array and contain a getCenterPt() method)
 * @return {Array} indexes from the objList array that are selected
 */
// function getSelectedPtsFromRect(selection, plot, objList) {
//     const selectedList= [];
//     if (selection && plot && objList && objList.length) {
//         const cc= CysConverter.make(plot);
//         const pt0= cc.getDeviceCoords(selection.pt0);
//         const pt1= cc.getDeviceCoords(selection.pt1);
//         if (!pt0 || !pt1) return selectedList;
//
//         const x= Math.min( pt0.x,  pt1.x);
//         const y= Math.min(pt0.y, pt1.y);
//         const width= Math.abs(pt0.x-pt1.x);
//         const height= Math.abs(pt0.y-pt1.y);
//         objList.forEach( (obj,idx) => {
//             const testObj = cc.getDeviceCoords(DrawOp.getCenterPt(obj));
//             if (testObj && contains(x,y,width,height,testObj.x, testObj.y)) {
//                 selectedList.push(idx);
//             }
//         });
//     }
//     return selectedList;
// }

/**
 * Return a WorldPt that is offset by the relative ra and dec from the passed in position
 * @param {WorldPt} pos1
 * @param {number} offsetRa in arcseconds
 * @param {number} offsetDec in arcseconds
 * @return {WorldPt}
 */
export function calculatePosition(pos1, offsetRa, offsetDec ) {
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

export function isAngleUnit(unit) {
    return ['deg', 'degree', 'arcmin', 'arcsec', 'radian', 'rad'].includes(unit.toLowerCase());
}

/**
 * convert angle value of one unit to that of another unit
 * @param {string} from 'degree' or 'deg', 'arcmin', 'arcsec', 'radian' case insensitive
 * @param {string} to 'degree' or 'deg', 'arcmin', 'arcsec', 'radian' case insensitive
 * @param {*} angle  number or string
 * @returns {number}
 */
export function convertAngle(from, to, angle) {
    const angleUnit = [['deg', 'degree'], 'arcmin', 'arcsec', ['radian', 'rad']];
    const rIdx = angleUnit.length-1;
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

/**
 * find a point on the plot that is top and left but is still in view and on the image.
 * If the image is off the screen the return undefined.
 * @param {WebPlot} plot
 * @param {object} viewDim
 * @param {number} xOff
 * @param {number} yOff
 * @return {DevicePt} the found point
 */

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

export function doSegmentsCross(seg1x1, seg1y1, seg1x2, seg1y2, seg2x1, sec2y2, seg2x2, seg2y2) {
    const result= findIntersectionPt(seg1x1, seg1y1, seg1x2, seg1y2, seg2x1, sec2y2, seg2x2, seg2y2);
    if (!result) return false;
    return result.onSeg1 || result.onSeg2;
}

export function lineCrossesRect(segX1, segY1, segX2, segY2, x, y, w,h) {

    if (segX1>=x && segY1>=y && segX1<=x+w && segY1<=y+h) return true;
    if (segX2>=x && segY2>=y && segX2<=x+w && segY2<=y+h) return true;

    return Boolean(
        doSegmentsCross(segX1, segY1, segX2, segY2, x,y, x+w,y) ||
        doSegmentsCross(segX1, segY1, segX2, segY2, x+w,y, x+w,y+h) ||
        doSegmentsCross(segX1, segY1, segX2, segY2, x+w,y+h, x,y+h) ||
        doSegmentsCross(segX1, segY1, segX2, segY2, x,y+h, x,y)
    );
}


/*
 * direction from a to b to c, pixels on device coordinate domain
 * 0: collinear
 * 1: clockwise
 * 2: counterclockwise
 */
function direction(a, b, c) {
    const d = (b.y - a.y) * (c.x - b.x) - (b.x - a.x) * (c.y - b.y);

    if (d===0) {
        return 0;   // a, b, c collinear
    } else {
        return (d < 0) ? 2 : 1;
    }
}

// check if pt is between between linePt1 and linePt2 assuming that all points are on the same line
function onSameSegment(linePt1, linePt2, pt) {
    return (pt.x >= Math.min(linePt1.x, linePt2.x)) && (pt.x <= Math.max(linePt1.x, linePt2.x)) &&
        (pt.y >= Math.min(linePt1.y, linePt2.y)) && (pt.y <= Math.max(linePt1.y, linePt2.y));
}

// check if there is intersection between two segments
function isTwoSegmentsIntersect(line1Pt1, line1Pt2, line2Pt1, line2Pt2) {
    const dir1 = direction(line1Pt1, line1Pt2, line2Pt1);
    const dir2 = direction(line1Pt1, line1Pt2, line2Pt2);
    const dir3 = direction(line2Pt1, line2Pt2, line1Pt1);
    const dir4 = direction(line2Pt1, line2Pt2, line1Pt2);

    if (dir1 !== dir2 && dir3 !== dir4) return true;

    if (dir1 === 0 && onSameSegment(line1Pt1, line1Pt2, line2Pt1)) return true;
    if (dir2 === 0 && onSameSegment(line1Pt1, line1Pt2, line2Pt2)) return true;
    if (dir3 === 0 && onSameSegment(line2Pt1, line2Pt2, line1Pt1)) return true;
    if (dir4 === 0 && onSameSegment(line2Pt1, line2Pt2, line1Pt2)) return true;

    return false;
}

// check if a segment intersects a view area (assume a non-slanted rectangular area)
export function segmentIntersectRect(point1, point2,  view_corners) {
    const xAry = view_corners.map((one_corner) => one_corner.x);
    const yAry = view_corners.map((one_corner) => one_corner.y);
    const x1 = Math.min(...xAry);
    const x2 = Math.max(...xAry);
    const y1 = Math.min(...yAry);
    const y2 = Math.max(...yAry);

    if ((point1.x >= x1 && point1.y >= y1 && point1.x <= x2 && point1.y <= y2) ||
        (point2.x >= x1 && point2.y >= y1 && point2.x <= x2 && point2.y <= y2)) {
        return true;
    }


    for (let s = 0; s < view_corners.length; s++) {
        const next_s = (s + 1) % view_corners.length;

        if (isTwoSegmentsIntersect(point1, point2, view_corners[s], view_corners[next_s])) {
            return true;
        }
    }
    return false;
}



/**
 * distance between point and line defined by two end points
 * @param pts
 * @param cc
 * @param pt
 * @returns {number}
 */
export function distToLine(pts, cc, pt) {
    if (!pt || !pts || !pts[0] || !pts[1]) return NaN;
    const spt = cc ? cc.getScreenCoords(pt) : makeScreenPt(pt.x, pt.y);
    const pt0 = cc ? cc.getScreenCoords(pts[0]) : makeScreenPt(pts[0].x, pts[0].y);
    const pt1 = cc ? cc.getScreenCoords(pts[1]) : makeScreenPt(pts[1].x, pts[1].y);
    const e1 = makeScreenPt((pt1.x - pt0.x), (pt1.y - pt0.y));
    const e2 = makeScreenPt((spt.x - pt0.x), (spt.y - pt0.y));
    const e3 = makeScreenPt((pt0.x - pt1.x), (pt0.y - pt1.y));
    const e4 = makeScreenPt((spt.x - pt1.x), (spt.y - pt1.y));

    const dpe1e2 = e1.x * e2.x + e1.y * e2.y;
    const dpe3e4 = e3.x * e4.x + e3.y * e4.y;
    const e1len2 = e1.x * e1.x + e1.y * e1.y;
    let ppt;

    if (dpe1e2 > 0 && dpe3e4 > 0) { // spt projects between pt1 & pt2
        ppt = makeScreenPt(dpe1e2 * e1.x / e1len2 + pt0.x, dpe1e2 * e1.y / e1len2 + pt0.y);
    } else if (dpe1e2 <= 0) {       // spt projects to right side of pt2
        ppt = pt0;
    } else {                        // spt projects to left side of pt1
        ppt = pt1;
    }
    return computeScreenDistance(spt.x, spt.y, ppt.x, ppt.y);
}

/**
 * distance between point to polygon boundary
 * @param pts
 * @param cc
 * @param pt
 * @returns {*}
 */
export function distanceToPolygon(pts, cc, pt) {
    const spt = cc ? cc.getScreenCoords(pt) : makeScreenPt(pt.x, pt.y);
    const dist = Number.MAX_VALUE;

    if (pts.length < 3) return dist;

    const corners = pts.map((pt) => (cc ? cc.getScreenCoords(pt) : makeScreenPt(pt.x, pt.y)));
    const len = corners.length;

    return corners.reduce((prev, pt, idx) => {
        const nIdx = (idx+1)%len;
        const d = distToLine([corners[idx], corners[nIdx]], cc, spt);

        if (d < prev) {
            prev = d;
        }
        return prev;
    }, dist);
}

/**
 * distance between point to circle boundary
 * @param radius  in screen pixel
 * @param pts  center in any domain
 * @param cc
 * @param pt
 * @returns {Number}
 */
export function distanceToCircle(radius, pts, cc, pt) {
    const spt = cc ? cc.getScreenCoords(pt) : makeScreenPt(pt.x, pt.y);
    let dist = Number.MAX_VALUE;
    let r, center;

    if (!radius && pts) {
        const p0 = cc ? cc.getScreenCoords(pts[0]) : makeScreenPt(pts[0].x, pts[0].y);
        const p1 = cc ? cc.getScreenCoords(pts[1]) : makeScreenPt(pts[1].x, pts[1].y);

        r = computeSimpleDistance(p0, p1)/2;
        center = makeScreenPt((p0.x + p1.x)/2, (p0.y + p1.y)/2);
    } else if (radius && pts) {
        center = cc ? cc.getScreenCoords(pts[0]) : makeScreenPt(pts[0].x, pts[0].y);
        r = radius;
    } else {
        return dist;
    }

    dist = Math.abs(computeSimpleDistance(center, spt) - r);
    return dist;
}


/**
 * This function will create a line. either based on y or based on x depend on which is farther apart. The is will return
 * a point with x,y
 * @param beginPt
 * @param endPt
 * @param independentValue
 * @return {number}
 */
export function getXorYinEquation(beginPt,endPt, independentValue) {
    const x1 = beginPt.x;
    const y1 = beginPt.y;
    const x2 = endPt.x;
    const y2 = endPt.y;

    // delta X and Y in image pixels
    const deltaX = Math.abs(x2 - x1);
    const deltaY = Math.abs(y2 - y1);

    if (deltaX > deltaY) {
        const slope = (y2-y1)/(x2-x1);
        const yIntercept = y1-slope*x1;
        const y= (slope*independentValue + yIntercept);
        return {x:independentValue, y};
    } else if (y1 !== y2) {
        const  islope = (x2-x1)/(y2-y1);
        const xIntercept = x1-islope*y1;
        const x = (islope*independentValue + xIntercept);
        return {x, y:independentValue};
    }
}


export function getLinePointAry(pt1, pt2) {
    const {x:x1,y:y1} = pt1;
    const {x:x2,y:y2} = pt2;

    // delta X and Y in image pixels
    const deltaX = Math.abs(x2-x1);
    const deltaY = Math.abs(y2-y1);

    let x, y;
    const pts = [];
    if (deltaX > deltaY) {
        const slope = (y2-y1)/(x2-x1);
        const yIntercept = y1-slope*x1;
        const minX = Math.trunc(Math.min(x1, x2));
        const maxX = Math.trunc(Math.max(x1, x2));
        for (x=minX; x<=maxX; x+=1) {
            y = Math.trunc(slope*x + yIntercept);
            pts.push(makeImagePt(x,y));
        }
        return pts;
    } else if (y1!==y2) {
        const islope = (x2-x1)/(y2-y1);
        const xIntercept = x1-islope*y1;
        const minY = Math.trunc(Math.min(y1, y2));
        const maxY = Math.trunc(Math.max(y1, y2));
        for (y=minY; y<=maxY; y+=1) {
            x = Math.trunc(islope*y + xIntercept);
            pts.push(makeImagePt(x,y));
        }
        return pts;
    }
    return;
}

function getEllipsePointForAngle(cx, cy, rx, ry, rotationAngle, theta) {
    const { abs, sin, cos } = Math;
    const M = abs(rx) * cos(theta);
    const N = abs(ry) * sin(theta);
    return [
        cx + cos(rotationAngle) * M - sin(rotationAngle) * N,
        cy + sin(rotationAngle) * M + cos(rotationAngle) * N
    ];
}

export function getEllipseArcEndPoints(cx, cy, rx, ry,  theta, dTheta, rotationAngle=0) {

    const [x1, y1] = getEllipsePointForAngle(cx, cy, rx, ry, rotationAngle, theta);
    const [x2, y2] = getEllipsePointForAngle(cx, cy, rx, ry, rotationAngle, theta + dTheta);
    const fa = Math.abs(dTheta) > Math.PI ? 1 : 0;
    const fs = dTheta > 0 ? 1 : 0;
    return { x1, y1, x2, y2, fa, fs };
}

export function getPointOnEllipse(cx, cy, rx, ry, angle) {
    const [x, y] = getEllipsePointForAngle(cx, cy, rx, ry, 0, angle);
    return {x,y};
}


export default {
    convert: convertCelestial,
};

