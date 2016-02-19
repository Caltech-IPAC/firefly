/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Shared by client and server
 *
 * @author Trey Roby
 */


import Enum from 'enum';
import CoordinateSys from './CoordSys.js';
import {CCUtil} from './CsysConverter.js';
import Point, {makeImageWorkSpacePt, makeViewPortPt, makeImagePt,
    makeScreenPt, makeWorldPt, isValidPoint} from './Point.js';
import {CysConverter} from './CsysConverter.js';
import ZoomUtil from './ZoomUtil.js';

var {AllPlots} = window.ffgwt ? window.ffgwt.Visualize : {AllPlots:null};


export const DtoR = Math.PI / 180.0;
export const RtoD = 180.0 / Math.PI;

function toDegrees (angle) { return angle * (180 / Math.PI); }
function toRadians(angle) { return angle * Math.PI / 180; }


const FullType= new Enum(['ONLY_WIDTH', 'WIDTH_HEIGHT', 'ONLY_HEIGHT', 'SMART']);


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
const computeScreenDistance= function (x1, y1, x2, y2) {
    const deltaXSq = (x1 - x2) * (x1 - x2);
    const  deltaYSq = (y1 - y2) * (y1 - y2);
    return Math.sqrt(deltaXSq + deltaYSq);
};

/**
 * compute the distance on the sky between two world points
 * @param p1 WorldPt
 * @param p2 WorldPt
 * @return
 */
const computeDistance= function(p1, p2) {
    var lon1Radius = p1.getLon() * DtoR;
    var lon2Radius = p2.getLon() * DtoR;
    var lat1Radius = p1.getLat() * DtoR;
    var lat2Radius = p2.getLat() * DtoR;
    var cosine = Math.cos(lat1Radius) * Math.cos(lat2Radius) *
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
        var dx = p1.x - p2.x;
        var dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
};



/**
 * Convert from one coordinate system to another.
 *
 * @param {object} wpt the world point to convert
 * @param {object} to CoordSys, the coordinate system to convert to
 * @return WorldPt the world point in the new coordinate system
 */
function convert(wpt, to= CoordinateSys.EQ_J2000) {
    var from = wpt.getCoordSys();
    if (!to || from==to) return wpt;

    const tobs=  (from===CoordinateSys.EQ_B1950) ? 1983.5 : 0;
    const ll = window.ffgwt.astro.CoordConv.doConv(
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
 * @return CentralPointRetval WorldPt and search radius
 */
const computeCentralPointAndRadius= function(inPoints) {
    var lon, lat;
    var radius;
    var maxRadius = Number.NEGATIVE_INFINITY;

    var points= inPoints.map((wp) => convertToJ2000(wp));


    /* get max,min of lon and lat */
    var maxLon = Number.NEGATIVE_INFINITY;
    var minLon = Number.POSITIVE_INFINITY;
    var maxLat = Number.NEGATIVE_INFINITY;
    var minLat = Number.POSITIVE_INFINITY;

    points.forEach((pt) => {
        if (pt.getLon() > maxLon) {
            maxLon = pt.getLon();
        }
        if (pt.getLon() < minLon) {
            minLon = pt.getLon();
        }
        if (pt.getLat() > maxLat) {
            maxLat = pt.getLat();
        }
        if (pt.getLat() < minLat) {
            minLat = pt.getLat();
        }
    });
    if (maxLon - minLon > 180) {
        minLon = 360 + minLon;
    }
    lon = (maxLon + minLon) / 2;
    if (lon > 360) lon -= 360;
    lat = (maxLat + minLat) / 2;

    var centralPoint = makeWorldPt(lon, lat);


    points.forEach((pt) => {
        radius = computeDistance(centralPoint,
                                 makeWorldPt(pt.getLon(), pt.getLat()));
        if (maxRadius < radius) {
            maxRadius = radius;
        }

    });

    return {centralPoint, maxRadius};
};


/**
 * Compute position angle
 *
 * @param ra0  the equatorial RA in degrees of the first object
 * @param dec0 the equatorial DEC in degrees of the first object
 * @param ra   the equatorial RA in degrees of the second object
 * @param dec  the equatorial DEC in degrees of the second object
 * @return position angle in degrees between the two objects
 */
const getPositionAngle= function(ra0, dec0, ra, dec) {
    var alf, alf0, del, del0;
    var sd, sd0, cd, cd0, cosda, cosd, sind, sinpa, cospa;
    var dist;
    var pa;

    alf = ra * DtoR;
    alf0 = ra0 * DtoR;
    del = dec * DtoR;
    del0 = dec0 * DtoR;

    sd0 = Math.sin(del0);
    sd = Math.sin(del);
    cd0 = Math.cos(del0);
    cd = Math.cos(del);
    cosda = Math.cos(alf - alf0);
    cosd = sd0 * sd + cd0 * cd * cosda;
    dist = Math.acos(cosd);
    pa = 0.0;
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
 * @return {WorldPt} the result new world position by applying the same displacement as the one from referencePosition to rotatedReferencePosition
 */
const getTranslateAndRotatePosition= function(referencePosition, rotatedReferencePosition, positionToRotate) {
    var ra1, dec1,ra2,dec2,ra,dec,cos_ra,sin_ra,cos_dec,sin_dec,x,y,z, cos_theta,sin_theta, x1,y1,z1;
    // Extract coordinates and transform to radians
     ra1 = toRadians(referencePosition.getLon());
     dec1 = toRadians(referencePosition.getLat());
     ra2 = toRadians(rotatedReferencePosition.getLon());
     dec2 = toRadians(rotatedReferencePosition.getLat());
     ra = toRadians(positionToRotate.getLon());
     dec = toRadians(positionToRotate.getLat());

    // Compute (x, y, z), the unit vector in R3 corresponding to positionToRotate
     cos_ra = Math.cos(ra);
     sin_ra = Math.sin(ra);
     cos_dec = Math.cos(dec);
     sin_dec = Math.sin(dec);

     x = cos_ra * cos_dec;
     y = sin_ra * cos_dec;
     z = sin_dec;

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
     cos_theta = Math.cos(-ra1);
     sin_theta = Math.sin(-ra1);
     x1 = cos_theta * x - sin_theta * y;
     y1 = sin_theta * x + cos_theta * y;
     z1 = z;

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
    var d, lon, lat;
     d = x1 * x1 + y1 * y1;
     lon = 0.0;
     lat = 0.0;
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
};

/**
 * Compute new position given a position and a distance and position angle
 *
 * @param ra   the equatorial RA in degrees of the first object
 * @param dec  the equatorial DEC in degrees of the first object
 * @param dist the distance in degrees to the second object
 * @param phi  the position angle in degrees to the second object
 * @return WorldPt of the new object
 */
const getNewPosition= function(ra, dec, dist, phi) {
    var tmp, newdec, deltaRa;
    var ra1, dec1;

    ra *= DtoR;
    dec *= DtoR;
    dist *= DtoR;
    phi *= DtoR;

    tmp = Math.cos(dist) * Math.sin(dec) + Math.sin(dist) * Math.cos(dec) * Math.cos(phi);
    newdec = Math.asin(tmp);
    dec1 = newdec * RtoD;

    tmp = Math.cos(dist) * Math.cos(dec) - Math.sin(dist) * Math.sin(dec) * Math.cos(phi);
    tmp /= Math.cos(newdec);
    deltaRa = Math.acos(tmp);
    if (Math.sin(phi) < 0.0) {
        ra1 = ra - deltaRa;
    }
    else {
        ra1 = ra + deltaRa;
    }
    ra1 *= RtoD;
    return makeWorldPt(ra1, dec1);
};

const getBestTitle= function(plot) {
    var t = plot.getPlotDesc();
    if (!t) {
        var mpw = plot.getPlotView().getMiniPlotWidget();
        if (mpw) t = mpw.getTitle();
    }
    return t;
};


const getRotationAngle= function(plot) {
    var retval = 0;
    var iWidth = plot.getImageWidth();
    var iHeight = plot.getImageHeight();
    var ix = iWidth / 2;
    var iy = iHeight / 2;
    var wptC = plot.getWorldCoords(makeImageWorkSpacePt(ix, iy));
    var wpt2 = plot.getWorldCoords(makeImageWorkSpacePt(ix, iHeight/4));
    if (wptC && wpt2) {
        retval = getPositionAngle(wptC.getLon(), wptC.getLat(), wpt2.getLon(), wpt2.getLat());
    }
    return retval;
};

export function isPlotNorth(plot) {
    var retval= false;
    var ix = plot.dataWidth/ 2;
    var iy = plot.dataHeight/ 2;
    var cc= CysConverter.make(plot);
    var wpt1 = cc.getWorldCoords(makeImageWorkSpacePt(ix, iy));
    if (wpt1) {
        var cdelt1 = cc.getImagePixelScaleInDeg();
        var wpt2 = makeWorldPt(wpt1.getLon(), wpt1.getLat() + (Math.abs(cdelt1) / plot.zoomFactor) * (5));
        var spt1 = cc.getScreenCoords(wpt1);
        var spt2 = cc.getScreenCoords(wpt2);
        if (spt1 && spt2) {
            retval = (spt1.x===spt2.x && spt1.y > spt2.y);
        }
    }
    return retval;
}

const getPossibleZoomLevels= function() {
        return ZoomUtil.levels;
};


const getEstimatedFullZoomFactor= function(fullType, dataWidth, dataHeight,
                                                  screenWidth, screenHeight, tryMinFactor=-1) {
    var zFact;
    if (fullType===FullType.ONLY_WIDTH || screenHeight <= 0 || dataHeight <= 0) {
        zFact =  screenWidth /  dataWidth;
    } else if (fullType===FullType.ONLY_HEIGHT || screenWidth <= 0 || dataWidth <= 0) {
        zFact =  screenHeight /  dataHeight;
    } else {
        var zFactW =  screenWidth /  dataWidth;
        var zFactH =  screenHeight /  dataHeight;
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
 * @param x0 the first point x, top left
 * @param y0 the first point y, top left
 * @param w0 the first rec width
 * @param h0 the first rec height
 * @param x the second point x, top left
 * @param y the second point y, top left
 * @param w h the second rec width
 * @param h the second rec height
 * @return true if rectangles intersect
 */
const intersects= function(x0, y0, w0, h0, x, y, w, h) {
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
const contains= function(x0, y0, w0, h0, x, y) {
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
const containsRec= function(x0, y0, w0, h0, x, y, w, h) {
     return contains(x0,y0,w0,h0,x,y) && contains(x0,y0,w0,h0,x+w,y+h);
};

const containsCircle= function(x, y, centerX, centerY, radius) {
    return Math.pow((x - centerX), 2) + Math.pow((y - centerY), 2) < radius * radius;
};

const getArrowCoords= function(x1, y1, x2, y2) {

    var barbLength = 10;

    /* compute shaft angle from arrowhead to tail */
    var deltaY = y2 - y1;
    var deltaX = x2 - x1;
    var shaftAngle = Math.atan2(deltaY, deltaX);
    var barbAngle = shaftAngle - 20 * Math.PI / 180; // 20 degrees from shaft
    var barbX = x2 - barbLength * Math.cos(barbAngle);  // end of barb
    var barbY = y2 - barbLength * Math.sin(barbAngle);

    var extX = x2 + 6;
    var extY = y2 + 6;

    var diffX = x2 - x1;
    var mult = ((y2 < y1) ? -1 : 1);
    if (diffX===0) {
        extX = x2;
        extY = y2 + mult * 14;
    } else {
        var slope = ( y2 - y1) / ( x2 - x1);
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

export const getCurrentPlot= function() {
    var retval= null;
    var mpw= AllPlots.getInstance().getMiniPlotWidget();
    if (mpw) {
        retval= mpw.getCurrentPlot();
    }
    return retval;
};

/**
 *
 * @param {object} selection obj with two properties pt0 & pt1
 * @param plot web plot
 * @param objList array of DrawObj (must be an array and contain a getCenterPt() method)
 * @return {Array} indexes from the objList array that are selected
 */
const getSelectedPts= function(selection, plot, objList) {
    var selectedList= [];
    if (selection && plot && objList && objList.length) {
        var pt0= plot.getScreenCoords(selection.pt0);
        var pt1= plot.getScreenCoords(selection.pt1);
        if (!pt0 || !pt1) return selectedList;

        var x= Math.min( pt0.x,  pt1.x);
        var y= Math.min(pt0.y, pt1.y);
        var width= Math.abs(pt0.x-pt1.x);
        var height= Math.abs(pt0.y-pt1.y);
        var testObj;
        objList.forEach( (obj,idx) => {
            testObj = plot.getScreenCoords(obj.getCenterPt());
            if (testObj && contains(x,y,width,height,testObj.x, testObj.y)) {
                selectedList.add(idx);
            }
        });
    }
    return selectedList;
};

const getCenterPtOfPlot= function(plot) {
    var dw = plot.dataWidth;
    var dh = plot.dataHeight;
    var ip= makeImagePt(dw/2,dh/2);
    return CCUtil.getWorldCoords(plot,ip);
};


const calculatePosition= function(pos1, offsetRa, offsetDec ) {
        var ra = toRadians(pos1.getLon());
        var dec = toRadians(pos1.getLat());
        var de = toRadians(offsetRa/3600.0); // east
        var dn = toRadians(offsetDec)/3600.0; // north

        var cosRa,sinRa,cosDec,sinDec;
        var cosDe,sinDe,cosDn,sinDn;
        var rhat= [];
        var shat= [];
        var uhat= [];
        var uxy;
        var ra2, dec2;

        cosRa  = Math.cos(ra);
        sinRa  = Math.sin(ra);
        cosDec = Math.cos(dec);
        sinDec = Math.sin(dec);

        cosDe = Math.cos(de);
        sinDe = Math.sin(de);
        cosDn = Math.cos(dn);
        sinDn = Math.sin(dn);


        rhat[0] = cosDe * cosDn;
        rhat[1] = sinDe * cosDn;
        rhat[2] = sinDn;

        shat[0] = cosDec * rhat[0] - sinDec * rhat[2];
        shat[1] = rhat[1];
        shat[2] = sinDec * rhat[0] + cosDec * rhat[2];

        uhat[0] = cosRa * shat[0] - sinRa * shat[1];
        uhat[1] = sinRa * shat[0] + cosRa * shat[1];
        uhat[2] = shat[2];

        uxy = Math.sqrt(uhat[0] * uhat[0] + uhat[1] * uhat[1]);
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
};

/**
 * Find the corners of a bounding box given the center and the radius
 * of a circle
 *
 * @param center WorldPt the center of the circle
 * @param radius  in arcsec
 * @return object with corners
 */
const getCorners= function(center, radius) {
        var posLeft = calculatePosition(center, +radius, 0.0);
        var posRight = calculatePosition(center, -radius, 0.0);
        var posUp = calculatePosition(center, 0.0, +radius);
        var posDown = calculatePosition(center, 0.0, -radius);
        var upperLeft = makeWorldPt(posLeft.getLon(), posUp.getLat());
        var upperRight = makeWorldPt(posRight.getLon(), posUp.getLat());
        var lowerLeft = makeWorldPt(posLeft.getLon(), posDown.getLat());
        var lowerRight = makeWorldPt(posRight.getLon(), posDown.getLat());

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

    var retval= null;
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
        case Point.VP_PT:
            retval= makeWorldPt(pt.x,pt.y, CoordinateSys.SCREEN_PIXEL);
            break;
        case Point.W_PT:
            retval=  pt;
            break;
    }
    return retval;
};

const makePt= function(type,  x, y) {
    var retval= null;
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
        case Point.VP_PT:
            retval= makeViewPortPt(x,y);
            break;
        case Point.W_PT:
            retval= makeWorldPt(x,y);
            break;
    }
    return retval;
};


export default {
    DtoR,RtoD,FullType,computeScreenDistance, computeDistance,
    computeSimpleDistance,convert,convertToJ2000,
    computeCentralPointAndRadius, getPositionAngle, getNewPosition,
    getBestTitle, getRotationAngle,getTranslateAndRotatePosition,
    isPlotNorth, getEstimatedFullZoomFactor,
    intersects, contains, containsRec,containsCircle,
    getArrowCoords, getSelectedPts, calculatePosition, getCorners,
    makePt, getWorldPtRepresentation, getCenterPtOfPlot, toDegrees
};



