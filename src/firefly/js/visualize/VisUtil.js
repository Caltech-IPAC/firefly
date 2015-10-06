/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/
/*globals ffgwt*/

/**
 * Shared by client and server
 *
 * @author Trey Roby
 */
"use strict";


import Enum from "enum";
import {WorldPt,Pt,ImageWorkSpacePt} from "./Point.js";
import CoordinateSys from "./CoordSys.js";

var {AllPlots} = window.ffgwt ? window.ffgwt.Visualize : {AllPlots:null};


export const DtoR = Math.PI / 180.0;
export const RtoD = 180.0 / Math.PI;


const FullType= new Enum(["ONLY_WIDTH", "WIDTH_HEIGHT", "ONLY_HEIGHT", "SMART"]);


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


/**
 *
 * @param x1
 * @param y1
 * @param x2
 * @param y2
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
 * @return
 */
export const computeDistance= function(p1, p2) {
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
export const computeSimpleDistance= function(p1, p2) {
        var dx = p1.x - p2.x;
        var dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
};


export const convertToJ2000= function(wpt) {
    return convert(wpt, CoordinateSys.EQ_J2000);
};

    /**
     * Convert from one coordinate system to another.
     *
     * @param wpt the world point to convert
     * @param to  CoordSys, the coordinate system to convert to
     * @return WorldPt the world point in the new coordinate system
     */
export const convert= function(wpt, to) {
    var retval;
    var from = wpt.getCoordSys();
    if (from.equals(to) || !to) {
        retval = wpt;
    } else {
        var tobs = 0.0;
        if (from===CoordinateSys.EQ_B1950) tobs = 1983.5;
        var ll = CoordConv.doConv(from.getJsys(), from.getEquinox(),
                                  wpt.getLon(), wpt.getLat(),
                                  to.getJsys(), to.getEquinox(), tobs);
        retval = new WorldPt(ll.getLon(), ll.getLat(), to);
    }
    return retval;
};


/**
 * Find an approximate central point and search radius for a group of positions
 *
 * @param inPoints array of points for which the central point is desired
 * @return CentralPointRetval WorldPt and search radius
 */
export const computeCentralPointAndRadius= function(inPoints) {
    var lon, lat;
    var radius;
    var max_radius = Number.NEGATIVE_INFINITY;

    var points= inPoints.map(wp => convertToJ2000(wp));


    /* get max,min of lon and lat */
    var max_lon = Number.NEGATIVE_INFINITY;
    var min_lon = Number.POSITIVE_INFINITY;
    var max_lat = Number.NEGATIVE_INFINITY;
    var min_lat = Number.POSITIVE_INFINITY;

    points.forEach(pt => {
        if (pt.getLon() > max_lon) {
            max_lon = pt.getLon();
        }
        if (pt.getLon() < min_lon) {
            min_lon = pt.getLon();
        }
        if (pt.getLat() > max_lat) {
            max_lat = pt.getLat();
        }
        if (pt.getLat() < min_lat) {
            min_lat = pt.getLat();
        }
    });
    if (max_lon - min_lon > 180) {
        min_lon = 360 + min_lon;
    }
    lon = (max_lon + min_lon) / 2;
    if (lon > 360) lon -= 360;
    lat = (max_lat + min_lat) / 2;

    var centralPoint = new WorldPt(lon, lat);


    points.forEach(pt => {
        radius = computeDistance(centralPoint,
                                 new WorldPt(pt.getLon(), pt.getLat()));
        if (max_radius < radius) {
            max_radius = radius;
        }

    });

    return {centralPoint, max_radius};
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
export const getPositionAngle= function(ra0, dec0, ra, dec) {
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
 * Compute new position given a position and a distance and position angle
 *
 * @param ra   the equatorial RA in degrees of the first object
 * @param dec  the equatorial DEC in degrees of the first object
 * @param dist the distance in degrees to the second object
 * @param phi  the position angle in degrees to the second object
 * @return WorldPt of the new object
 */
export const getNewPosition= function(ra, dec, dist, phi) {
    var tmp, newdec, delta_ra;
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
    delta_ra = Math.acos(tmp);
    if (Math.sin(phi) < 0.0)
        ra1 = ra - delta_ra;
    else
        ra1 = ra + delta_ra;
    ra1 *= RtoD;
    return new WorldPt(ra1, dec1);
};

export const getBestTitle= function(plot) {
    var t = plot.getPlotDesc();
    if (!t) {
        var mpw = plot.getPlotView().getMiniPlotWidget();
        if (mpw) t = mpw.getTitle();
    }
    return t;
};


export const getRotationAngle= function(plot) {
    var retval = 0;
    var iWidth = plot.getImageWidth();
    var iHeight = plot.getImageHeight();
    var ix = iWidth / 2;
    var iy = iHeight / 2;
    var wptC = plot.getWorldCoords(new ImageWorkSpacePt(ix, iy));
    var wpt2 = plot.getWorldCoords(new ImageWorkSpacePt(ix, iHeight/4));
    if (wptC && wpt2) {
        retval = getPositionAngle(wptC.getLon(), wptC.getLat(), wpt2.getLon(), wpt2.getLat());
    }
    return retval;
};

export const isPlotNorth= function(plot) {

    var retval= false;
    var iWidth = plot.getImageWidth();
    var iHeight = plot.getImageHeight();
    var ix = iWidth / 2;
    var iy = iHeight / 2;
    var wpt1 = plot.getWorldCoords(new ImageWorkSpacePt(ix, iy));
    if (wpt1) {
        var cdelt1 = plot.getImagePixelScaleInDeg();
        var zfact = plot.getZoomFact();
        var wpt2 = new WorldPt(wpt1.getLon(), wpt1.getLat() + (Math.abs(cdelt1) / zfact) * (5));

        var spt1 = plot.getScreenCoords(wpt1);
        var spt2 = plot.getScreenCoords(wpt2);
        if (spt1 && spt2) {
            retval = (spt1.getIX()===spt2.getIX() && spt1.getIY() > spt2.getIY());
        }
    }
    return retval;
};

export const getPossibleZoomLevels= function() {
        return ZoomUtil._levels;
};


export const getEstimatedFullZoomFactor= function(fullType, dataWidth, dataHeight,
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
 * @return true if rectangles intersect
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
 * @return true if rectangles intersect
 */
export const containsRec= function(x0, y0, w0, h0, x, y, w, h) {
     return contains(x0,y0,w0,h0,x,y) && contains(x0,y0,w0,h0,x+w,y+h);
};

export const containsCircle= function(x, y, centerX, centerY, radius) {
    return Math.pow((x - centerX), 2) + Math.pow((y - centerY), 2) < radius * radius;
};

export const getArrowCoords= function(x1, y1, x2, y2) {

    var barb_length = 10;

    /* compute shaft angle from arrowhead to tail */
    var delta_y = y2 - y1;
    var delta_x = x2 - x1;
    var shaft_angle = Math.atan2(delta_y, delta_x);
    var barb_angle = shaft_angle - 20 * Math.PI / 180; // 20 degrees from shaft
    var barbX = x2 - barb_length * Math.cos(barb_angle);  // end of barb
    var barbY = y2 - barb_length * Math.sin(barb_angle);

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
        x1 : x1,
        y1 : y1,
        x2 : x2,
        y2 : y2,
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
        retval= mpw.getCurrentPlot()
    }
    return retval;
}

/**
 *
 * @param selection obj with two properties pt0 & pt1
 * @param plot web plot
 * @param objList array of DrawObj (must be an array and contain a getCenterPt() method)
 * @return {Array} indexes from the objList array that are selected
 */
export const getSelectedPts= function(selection, plot, objList) {
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


export const calculatePosition= function(pos1, offsetRa, offsetDec ) {
        var ra = Math.toRadians(pos1.getLon());
        var dec = Math.toRadians(pos1.getLat());
        var de = Math.toRadians(offsetRa/3600.0); // east
        var dn = Math.toRadians(offsetDec)/3600.0; // north

        var cos_ra,sin_ra,cos_dec,sin_dec;
        var cos_de,sin_de,cos_dn,sin_dn;
        var rhat= [];
        var shat= [];
        var uhat= [];
        var uxy;
        var ra2, dec2;

        cos_ra  = Math.cos(ra);
        sin_ra  = Math.sin(ra);
        cos_dec = Math.cos(dec);
        sin_dec = Math.sin(dec);

        cos_de = Math.cos(de);
        sin_de = Math.sin(de);
        cos_dn = Math.cos(dn);
        sin_dn = Math.sin(dn);


        rhat[0] = cos_de * cos_dn;
        rhat[1] = sin_de * cos_dn;
        rhat[2] = sin_dn;

        shat[0] = cos_dec * rhat[0] - sin_dec * rhat[2];
        shat[1] = rhat[1];
        shat[2] = sin_dec * rhat[0] + cos_dec * rhat[2];

        uhat[0] = cos_ra * shat[0] - sin_ra * shat[1];
        uhat[1] = sin_ra * shat[0] + cos_ra * shat[1];
        uhat[2] = shat[2];

        uxy = Math.sqrt(uhat[0] * uhat[0] + uhat[1] * uhat[1]);
        if (uxy>0.0)
            ra2 = Math.atan2(uhat[1],uhat[0]);
        else
            ra2 = 0.0;
        dec2 = Math.atan2(uhat[2],uxy);

        ra2  = Math.toDegrees(ra2);
        dec2 = Math.toDegrees(dec2);

        if (ra2 < 0.0) ra2 +=360.0;

        return new WorldPt(ra2, dec2);
};

/**
 * Find the corners of a bounding box given the center and the radius
 * of a circle
 *
 * @param center WorldPt the center of the circle
 * @param radius  in arcsec
 * @return object with corners
 */
export const getCorners= function(center, radius) {
        var pos_left = calculatePosition(center, +radius, 0.0);
        var pos_right = calculatePosition(center, -radius, 0.0);
        var pos_up = calculatePosition(center, 0.0, +radius);
        var pos_down = calculatePosition(center, 0.0, -radius);
        var upperLeft = new WorldPt(pos_left.getLon(), pos_up.getLat());
        var upperRight = new WorldPt(pos_right.getLon(), pos_up.getLat());
        var lowerLeft = new WorldPt(pos_left.getLon(), pos_down.getLat());
        var lowerRight = new WorldPt(pos_right.getLon(), pos_down.getLat());

        return {upperLeft, upperRight, lowerLeft, lowerRight};
};

