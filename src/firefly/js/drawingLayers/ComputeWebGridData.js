/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Lijun
 * 4/14/16
 *
 * 05/02/17
 * DM-6501
 *   Add the Regrid and LinearInterpolator in the utility
 *   Use the Regrid to adding more gride lines and the number of the points in the line
 *   Rotate the labels based on the angles
 *   Cleaned up the codes
 *
 *  02/13/18
 *  IRSA-1391
 *  Add the Grid lines for HiPs map
 *  The HiPs map is very different from the regular image map.  The HiPs map has a huge range in longitude.
 *  In order to get finer grid calculation, I adjust the algorithm, instead of using 4 for the intervals when finding lines,
 *  I used interval = range/0.5.
 *
 *  For HiPs map, the range is calculated differently.  The range calculation for regular image is using the data width
 *  data height (which are the naxis1/naxis2).  But for HiPs, there are no data width and height.  Instead of using
 *  data width and height by walking four corners, I used the field view angles and center point.  If the angle is >=180,
 *  the image is sphere.  If the fov < 180, the ranges are calculated using four corners and two middle points along the
 *  center latitude and longitude.  The four corners are calculated using the plot viewDim.
 *
 *  For the grid lines calculation, all valid points are in the range, there is no need to check with the screen width.
 *
 *
 *  8/28/18
 *  Some detailed to be noted here:
 *  1. If any corner is null, it means the image is a sphere.  Thus, the range is longitude = [0:360] and latitude=[-90,90]
 *  2. If none corner is null, the image is cover the whole view dimension in the screen.  In this case, there are
 *  a few situations:
 *    For Latitude:
 *     1. If north pole is visible, the latitude will be [lowerLat,90], where the lowerLat is calculated using
 *        the four corners.
 *     2. If the south pole is visible, the latitude will be [-90, upperLat], where the upperLat is calculated using
 *        the four corners.
 *     3. If both poles are visible, the latitude will be [-90, 90]
 *     4. If no pole is visible, the latitude will be [lowerLat, upperLat], where lowerLat and upperLat are calculated
 *        using the four corners.
 *
 *    For Longitude:
 *       1. If either pole is visible, the longitude will be [0, 360]
 *       2. If prime median is visible, ie, any point on the longitude=0 great circle is visible, there is a
 *          discontinuity point (0, 360) in the image, the longitude range will be in two separated direction,
 *          0 increasing to a value_1, and the 360 decreasing to a value_2.  Using four corners can not determine
 *          value_1 and value_2 precisely.  In this case, we do a loop through to find the points in the range and
 *          on the plots.
 *
 *       3. If no pole and no pm point visible, the longitude range will be [lowerLon, upperLon], where lowerLon
 *          and upperLan are calculated by the four corners.
 *
 *
 *  In order to see the grid lines, the algorithm calculated fixed number of lines based on the viewable range no matter
 *  how small the physical range is.  It works different from FITs image.  FITS image uses fixed distance in lon/lat to
 *  calculate grid lines.  Thus, when the image is zoomed out, the physical size is smaller, the grid lines are out of
 *  view.
 *
 *  Labels:
 *    When the field view angle is very small, the image is zoomed out a lot, the numerical values in the labels are
 *    displayed up to 6 digit precision.  Otherwise, the labels are displayed up to 3 digit precision.
 *
 *
 */

import { makeWorldPt, makeImagePt,makeDevicePt} from '../visualize/Point.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import CoordinateSys from '../visualize/CoordSys.js';
import CoordUtil from '../visualize/CoordUtil.js';
import {sprintf} from '../externalSource/sprintf';
import { getDrawLayerParameters} from './WebGrid.js';
import {Regrid} from '../util/Interp/Regrid.js';
import {getPointMaxSide} from  '../visualize/HiPSUtil.js';
import {convertCelestial} from '../visualize/VisUtil.js';
import {isHiPS, isHiPSAitoff} from '../visualize/WebPlot.js';
import {getFoV} from '../visualize/PlotViewUtil.js';

const precision3Digit = '%.3f';
const precision6Digit = '%.6f';
const RANGE_THRESHOLD = 1.02;
const minUserDistance= 0.25;   // user defined max dist. (deg)
const maxUserDistance= 3.00;   // user defined min dist. (deg)
var userDefinedDistance = false;
const angleStepForHipsMap=4.0;
/**
 * This method does the calculation for drawing data array
 * @param plot - primePlot object
 * @param cc - the CoordinateSys object
 * @param useLabels - use Labels
 * @param numOfGridLines
 * @return a DrawData object
 */
var nearCenterLonValue;
var middleLatValue=[];

export function makeGridDrawData (plot,  cc, useLabels, numOfGridLines=11){


    const {width,height, screenWidth, csys, labelFormat} = getDrawLayerParameters(plot);

    const wpt = cc.getWorldCoords(makeImagePt(1, 1), csys);
    const aitoff = (!wpt);
    const {fov, centerWp}= getPointMaxSide(plot, plot.viewDim);
    const centerWpt = convertCelestial(centerWp,csys);


    if (centerWpt && width > 0 && height >0) {
        const bounds=  (isHiPS(plot) && isHiPSAitoff(plot) && getFoV(plot) > 200)  ?
            undefined : new Rectangle(0, 0, width, height);

        const {xLines, yLines, labels} = plot.plotType==='hips'?computeHipGridLines(cc, csys,screenWidth, numOfGridLines, labelFormat,plot,fov, centerWpt)
        :computeImageGridLines(cc, csys, width,height,screenWidth, numOfGridLines, labelFormat,plot);

        return  drawLines(bounds, labels, xLines, yLines, aitoff, screenWidth, useLabels, cc, plot,csys, centerWpt,labelFormat);
    }
}

/**
 * Walk along the four side to get possible min/max values
 * The finer det is the better result will be gotten.  However, too small det will hurt performance
 * @param plot
 * @param csys
 * @param cc
 * @returns {Array.<*>}
 */
function getRangeFromFourSides(plot, csys,  cc){
    const {width, height} = plot.viewDim;
    const det=0.5;
    //from bottom left to right (0,0) - (width, 0)
    var points1=[], points2=[], points3=[], points4=[],  x, y;
    var wLen, hLen;
    wLen = width/det;
    hLen = height/det;
    for (var i=0; i<wLen; i++){
        x=i*det;
        y=0;
        points1[i]= cc.getWorldCoords(makeDevicePt(x, y), csys);
    }
    //from 0,0 to 0, height: (0, 0) - (0, height)
    for (var i=0; i<hLen; i++){
        x=0;
        y=i*det;
        points2[i]= cc.getWorldCoords(makeDevicePt(x, y), csys);
    }

    //from (0, height) - (width, height)
    for (var i=0; i<wLen; i++){
        x=i*det;
        y=height;
        points3[i]= cc.getWorldCoords(makeDevicePt(x, y), csys);
    }

    //from (width, 0) - (width, height)
    //from 0,0 to 0, height: (0, 0) - (0, height)
    for (var i=0; i<hLen; i++){
        x=width;
        y=i*det;
        points4[i]= cc.getWorldCoords(makeDevicePt(x, y), csys);
    }
    return points1.concat(points2, points3, points4);
}

function getForCorners(plot, csys,  cc) {
    const {width, height} = plot.viewDim;

    const corners = [
        cc.getWorldCoords(makeDevicePt(0, 0), csys),
        cc.getWorldCoords(makeDevicePt(0, height), csys),
        cc.getWorldCoords(makeDevicePt(width, height), csys),
        cc.getWorldCoords(makeDevicePt(width, 0), csys),
        cc.getWorldCoords(makeDevicePt(width/2, 0), csys),
        cc.getWorldCoords(makeDevicePt(width/2, height), csys),
        cc.getWorldCoords(makeDevicePt(0, height/2), csys),
        cc.getWorldCoords(makeDevicePt(width, height/2), csys)

    ];

    return corners;
}
/**
 * This method calculates the four corners and for middle points in the view port. If none of them is null,
 * it means the image cover the whole view area.  Thus, those values can be used to determine the min/max ra/dec ranges
 *
 * @param plot
 * @param csys
 * @param cc
 * @param ranges
 * @returns {*}
 */
function getViewPortInfo(plot, csys,  cc,ranges) {

    const corners = getForCorners(plot, csys,  cc);
    if( corners.indexOf(null)>-1)  return {corners:undefined, ranges};

    const allPoints = corners.concat(getRangeFromFourSides(plot, csys,  cc));
    var vals = [];
    var viewBorder = [[1.e20, -1.e20], [1.e20, -1.e20]];
    for (let i = 0; i < allPoints.length; i++) {
        if (allPoints[i]) {
            vals[0] = allPoints[i].getLon();
            vals[1] =allPoints[i].getLat();
            //assign the new lower and upper longitude if found
            if (vals[0] < viewBorder[0][0]) viewBorder[0][0] = vals[0];
            if (vals[0] > viewBorder[0][1]) viewBorder[0][1] = vals[0];

            //assign the new lower and upper latitude if found
            if (vals[1] < viewBorder[1][0]) viewBorder[1][0] = vals[1];
            if (vals[1] > viewBorder[1][1]) viewBorder[1][1] = vals[1];
        }
    }


    return {corners, viewBorder};
}

/**
 * When we use four corners and the points around four sides, we look for the minimal value at
 * as the lower range and the maximum value as a upper range.  However, if the prime median point
 * is seen, the borders need to be adjusted.  The smallest value is 0 that lies in the image.  The
 * lower border should be the maximum possible value along 0-lower border, this value will be less
 * than 180.  When the PM is on the left most or right most point, the whole range is 180.  Thus,
 * we use 180 as a number to determine the real border.  Similarly, we need to look for smallest possible
 * along 360-upper border.
 *
 * should be the real lower border, the upper border is always 360,
 * @param plot
 * @param csys
 * @param cc
 * @param centerWpt
 * @param viewBorder
 * @returns {Array.<number>}
 */
function getRangeForTruePM(plot, csys,  cc, centerWpt, viewBorder){
    const corners = getForCorners(plot, csys,  cc);
    const allPoints = corners.concat(getRangeFromFourSides(plot, csys,  cc));
    var lower=[], upper=[], lon;
    var  lowerCount=0, upperCount=0;

    for (var i=0; i<allPoints.length; i++){
        lon=allPoints[i].x;
        if (centerWpt.x<180) { // PM point on the left of the centerWpt
          if (lon<180) {
            lower[lowerCount] = lon;
            lowerCount++;
          }
          else {
            upper[upperCount]=lon;
            upperCount++;
          }

        }
        else {
            if (lon>180){
              upper[upperCount]=lon;
              upperCount++;
            }
            else {
              lower[lowerCount] = lon;
              lowerCount++;
            }
        }

    }

    return  [ lower && lower.length>0? Math.max(...lower):viewBorder[0], upper && upper.length>0? Math.min(...upper):viewBorder[1]];
}
/**
 * Define a rectangle object
 * @param x - the x coordinate
 * @param y - the y coordinate
 * @param width - the width of the rectangle
 * @param height - the height of the rectangle
 * @constructor
 */
function Rectangle(x, y, width, height){
    this.x= x;
    this.y= y;
    this.width= width;
    this.height= height;
}
/**
 * walk around four corners to find the proper ranges
 * @param intervals
 * @param x0 - the starting x value of the corner
 * @param y0 - the starting y value of the corner
 * @param dx - the increment
 * @param dy - the increment
 * @param range - the x and y rangs
 * @param csys - the coordinate system the grid is drawing with
 * @param wrap - boolean value, true or false
 * @param cc - the CoordinateSys object
 */
function edgeRun (intervals,x0,  y0,dx, dy, range, csys, wrap, cc) {


    var  x = x0;
    var  y = y0;

    var i = 0;
    var vals = [];

    while (i <= intervals) {

        const wpt =cc.getWorldCoords(makeImagePt(x,y),csys);
        //look for lower and upper longitude and latitude
        if (wpt) {
            vals[0] = wpt.getLon();
            vals[1] = wpt.getLat();

            if (wrap && vals[0] > 180) vals[0] = vals[0]-360;
            if (wrap!=null //true and false
                || wrap==null && cc.pointInView(makeWorldPt(vals[0], vals[1], csys))//temporary solution to solve the problem reported.
                || csys===CoordinateSys.EQ_B2000
                || csys===CoordinateSys.EQ_J2000 ) {
                //assign the new lower and upper longitude if found
                if (vals[0] < range[0][0]) range[0][0] = vals[0];
                if (vals[0] > range[0][1]) range[0][1] = vals[0];

                //assign the new lower and upper latitude if found
                if (vals[1] < range[1][0]) range[1][0] = vals[1];
                if (vals[1] > range[1][1]) range[1][1] = vals[1];
            }
        }
        x += dx;
        y += dy;

        ++i;
    }

}
/**
 * get the ege value
 * @param intervals - the number of interval to look for the values
 * @param width - the width of the image
 * @param height - the height of the image
 * @param csys - the coordinate system the grid is drawing with
 * @param wrap - boolean value, true or false
 * @param cc - the CoordinateSys object
 * @returns the value at the edge of the image
 */
function  edgeVals(intervals, width, height, csys, wrap, cc) {

    var  range = [[1.e20, -1.e20],[1.e20, -1.e20]];
    var xmin=0;
    var ymin=0;
    var xmax= width;
    var ymax= height;
    var xdelta, ydelta, x, y;


    // four corners.
    // point a[xmin, ymax], the top left point, from here toward to right top point b[xmax, ymax], ie the line
    //   a-b
    y = ymax;
    x = xmin;
    xdelta = (width / intervals) - 1; //define an interval of the point in line a-b
    ydelta = 0; //no change in the y direction, so ydelta is 0, thus the points should be alone line a-b
    edgeRun(intervals, x, y, xdelta, ydelta, range, csys,wrap, cc);

    // Bottom: right to left
    y = ymin;
    x = xmax;
    xdelta = -xdelta;
    edgeRun(intervals, x, y, xdelta, ydelta, range, csys, wrap, cc);

    // Left.  Bottom to top.
    xdelta = 0;
    ydelta = (height / intervals) - 1;
    y = ymin;
    x = xmin;
    edgeRun(intervals, x, y, xdelta, ydelta, range, csys, wrap, cc);

    // Right. Top to bottom.
    ydelta = -ydelta;
    y = ymax;
    x = xmax;
    edgeRun(intervals, x, y, xdelta, ydelta, range, csys, wrap,cc);

    // grid in the middle
    xdelta = (width / intervals) - 1;
    ydelta = (height / intervals) - 1;
    x = xmin;
    y = ymin;
    edgeRun(intervals, x, y, xdelta, ydelta, range, csys, wrap,cc);


    return range;
}
/**
 * Test to see if the edge is a real one
 * @param xrange
 * @param trange
 * @returns {boolean}
 */
function  testEdge( xrange, trange)
{
    /* This routine checks if the experimental minima and maxima
     * are significantly changed from the old test minima and
     * maxima.  xrange and trange are assumed to be multidimensional
     * extrema of the form double[ndim][2] with the minimum
     * in the first element and the maximum in the second.
     *
     * Note that xrange is modified to have the most extreme
     * value of the test or old set of data.
     */

    /* Find the differences between the old data */
    var delta =trange.map( (t)=>{
        return Math.abs(t[1]-t[0]);
    });

    for (let i=0; i<trange.length; i += 1) {
        var  ndelta = Math.abs(xrange[i][1]-xrange[i][0]);
        /* If both sets have nil ranges ignore this dimension */
        if (ndelta <= 0. && delta[i] <= 0.){
            continue;
        }

        /* If the old set was more extreme, use that value. */
        if (xrange[i][0] > trange[i][0]) {
            xrange[i][0] = trange[i][0];
        }

        if (xrange[i][1] < trange[i][1])  {
            xrange[i][1] = trange[i][1];
        }

        /* If the previous range was 0 then we've got a
         * substantial change [but see above if both have nil range]
         */
        if (!delta[i]) {
            return false;
        }

        /* If the range has increased by more than 2% than */
        if ( Math.abs(xrange[i][1]-xrange[i][0])/delta[i] >
            RANGE_THRESHOLD) {
            return false;
        }
    }

    return true;
}

/**
 * Get the line ranges
 * @param  {object} csys - the coordinate system the grid is drawing with
 * @param  {double} width - the width of the image
 * @param  {double} height - the height of the image
 * @param  {object} cc - the CoordinateSys object
 * @returns the range array
 */
function getRange( csys, width, height, cc) {


    //using three state boolean wrap=true/false, the null, null for temporary solution to solve the problem reported.
    var range=[[0.,0.],[0.,0.]];
    var poles=0;  // 0 = no poles, 1=north pole, 2=south pole, 3=both

    /* Get the range of values for the grid. */
    /* Check for the poles.  We allow the poles to
     * be a pixel outside of the image and still consider
     * them to be included.
     */

    var wrap=false;	/* Does the image wrap from 360-0. */
    var  sharedLon= 0.0;
    var  sharedLat= 90.0;



    if (cc.pointInView(makeWorldPt(sharedLon, sharedLat, csys))){
        range[0][0] = -179.999;
        range[0][1] =  179.999;
        range[1][1] = 90;
        poles += 1;
        wrap = true;
    }

    sharedLon= 0.0;
    sharedLat= -90.0;
    if (cc.pointInView(makeWorldPt(sharedLon, sharedLat, csys))){
        range[0][0] = -179.999;
        range[0][1] =  179.999;
        range[1][0] = -90;
        poles += 2;
        wrap = true;
    }

    /* If both poles are included we can just return */
    if (poles == 3){
        return range;
    }

    /* Now we have to go around the edges to find the remaining
     * minima and maxima.
     */
    var  trange = edgeVals(1, width, height, csys, wrap,cc);
    if (!wrap) {
        /* If we don't have a pole inside the map, check
         * to see if the image wraps around.  We do this
         * by checking to see if the point at [0, averageDec]
         * is in the map.
         */

        sharedLon= 0.0;
        sharedLat= (trange[1][0] + trange[1][1])/2;

        //this block is modified to fix the issue reported that only one line is drawn in some case
        if (cc.pointInView(makeWorldPt(sharedLon, sharedLat, csys)))
        {
            wrap = true;

            // Redo min/max
            trange = edgeVals(1, width, height, csys,wrap,cc);
        }
        //this block was a temporary solution to solve the problem once found.  Comment it out for now in js version
        else if (csys===CoordinateSys.GALACTIC){
            trange=edgeVals(1, width, height, csys,null,cc);
            sharedLon = 0.0;
            sharedLat = (trange[1][0] + trange[1][1]) / 2;
            if (cc.pointInView(makeWorldPt(sharedLon, sharedLat,csys))) {
                wrap=true;
                // Redo min/max
                trange =edgeVals(1, width, height, csys,wrap,cc);
            }
        }

    }

    var  xrange = trange;
    var xmin= 0;
    var  adder=2;
    for (let  intervals = xmin+adder;
         intervals < width; intervals+= adder) {

        xrange = edgeVals(intervals, width, height, csys,wrap,cc);
        if (testEdge(xrange, trange)) {
            break;
        }
        trange = xrange;
        adder*= 2;
    }

    if (!poles && wrap){
        xrange[0][0] += 360;
    }
    else if (poles ===1) {
        range[1][0] = xrange[1][0];
        return range;
    }
    else if (poles === 2)  {
        range[1][1] = xrange[1][1];
        return range;
    }

    return xrange;

}

function lookup(val, factor){

    const conditions=[val < 1,val > 90,val > 60 ,val > 30,val > 23,val > 18,val > 6, val > 3];
    const index = conditions.indexOf(true);
    var values =[val, 30, 20, 10, 6, 5, 2, 1] ;
    var retval = (index && index>=0)? values[index] :0.5;
    if (factor >=4.0) {
        retval = retval/2.0;
    }
    return retval;



}

function calculateDelta(min, max,factor){
    var delta = (max-min)<0? max-min+360:max-min;
    var  qDelta;
    if (delta > 1) { // more than 1 degree
        qDelta = lookup(delta,factor);
    }
    else if (60*delta > 1) {// more than one arc minute
        qDelta = lookup(60*delta,factor)/60;
    }
    else if (3600*delta > 1) {// more than one arc second
        qDelta= lookup(3600*delta,factor)/3600;
    }
    else{
        qDelta= Math.log(3600*delta)/Math.log(10);
        qDelta= Math.pow(10,Math.floor(qDelta));
    }
    if (userDefinedDistance && !(minUserDistance < qDelta && qDelta < maxUserDistance)){
        var minTry= Math.abs(minUserDistance-qDelta);
        var maxTry= Math.abs(maxUserDistance-qDelta);
        qDelta= (minTry<maxTry) ? minUserDistance :
            maxUserDistance;
    }

    return qDelta;

}

/**
 * When the fov is small such as 1 degree, the numerical value shows up to 6 digit precision.
 * Otherwise, it only shows up to 3 digit.
 * @param {Array.<Array.<number>>} levels
 * @param {CoordinateSys} csys
 * @param {String} labelFormat  pass 'hms' for sexigesimal
 * @param fov
 * @returns {Array}
 */
function getLabels(levels,csys, labelFormat, fov=180) {

    const labels = [];
    const isHms= labelFormat === 'hms' && (csys===CoordinateSys.EQ_J2000 || csys===CoordinateSys.EQ_B1950);

    for (let i=0; i < 2; i++){
        const toHms= i===0 ? CoordUtil.convertLonToString : CoordUtil.convertLatToString;
        for (let j=0; j < levels[i].length; j++) {
            const value= levels[i][j];
            labels.push(isHms ? toHms(value, csys) : ( fov && fov>1?sprintf(precision3Digit,value):sprintf(precision6Digit,value)) );
        }
    }
    return labels;
}

function findLine(cc,csys, direction, value, range, screenWidth, type='image'){

    var intervals;
    var x, dx, y, dy;

    const dLength=direction===0?range[1][1]-range[1][0]:range[0][1]-range[0][0];

    var nInterval = 4;
    if (type==='hips'){
       const n = parseInt(dLength/angleStepForHipsMap);
       nInterval = n>4?n:4;
    }

    if (!direction )  {// longitude lines
        x  = value;
        dx = 0;
        y  = range[1][0];
        dy = (range[1][1]-range[1][0])/nInterval;
    }
    else { // latitude lines

            y = value;
            dy = 0;
            x = range[0][0];
            dx = (range[0][1] - range[0][0]);
            dx = dx < 0 ? dx + 360 : dx;
            dx /= nInterval;

    }
    var opoints = findPoints(cc, csys,nInterval, x, y, dx, dy, null);
    if (type==='hips') return isHiPSAitoff(cc) ? opoints : fixPoints(opoints);

    //NO need to do this, but left here since it was here originally
    var  straight = isStraight(opoints);
    var npoints = opoints;
    intervals = 2* nInterval;
    var nstraight;
    var count=1;
    while (intervals < screenWidth  && count<10) { //limit longer loop
        dx /= 2;
        dy /= 2;
        npoints = findPoints(cc, csys, intervals, x, y, dx, dy, opoints);
        nstraight = isStraight(npoints);
        if (straight && nstraight) {
            break;
        }
        straight = nstraight;
        opoints = npoints;
        intervals *= 2;
        count++;
    }

    return fixPoints(npoints);


}


function isStraight(points){

    /* This function returns a boolean value depending
     * upon whether the points do not bend too rapidly.
     */
    const len = points[0].length;
    if (len < 3) return true;

    var dx0,  dy0,  len0;
    var  crossp;

    var dx1 = points[0][1]-points[0][0];
    var dy1 = points[1][1]-points[1][0];

    var len1 = (dx1*dx1) + (dy1*dy1);

    for (let i=1; i < len-1; i += 1)   {

        dx0 = dx1;
        dy0 = dy1;
        len0 = len1;
        dx1 = points[0][i+1]-points[0][i];
        dy1 = points[1][i+1]-points[1][i];

        if (dx1>=1.e20 || dy1>=1.e20) continue;
        len1 = (dx1*dx1) + (dy1*dy1);
        if (!len0  || !len1 ){
            continue;
        }
        crossp = (dx0*dx1 + dy0*dy1);
        var  cos_sq = (crossp*crossp)/(len0*len1);
        if (!cos_sq) return false;
        if (cos_sq >= 1) continue;

        var  tan_sq = (1-cos_sq)/cos_sq;
        if (tan_sq*(len0+len1) > 1) {
            return false;
        }
    }
    return true;
}

/**
 * For image map, the interval is hard coded as 4 in the original version (java).  I think that
 * since each image only covers a small stripe of the sky, the range of longitude usually less than 1 degree.
 * Howver for HiPs map, the range is whole sky (0-360).  The hard coded interval 4 is not good enough to find the
 * good points.
 *
 * For HiPs map, I use the interval = longitude-range/0.5, and interval = latitude-range/0.5.  Thus, more points
 * are checked and found for each line.
 *
 * @param cc
 * @param csys
 * @param intervals
 * @param x0
 * @param y0
 * @param dx
 * @param dy
 * @param opoints
 * @returns {*}
 */
function findPoints(cc,csys, intervals, x0, y0,dx, dy,  opoints){

    var  xpoints = [[],[]];
    var lon=[], lat=[];
    var i0, di;
    if (opoints)  {
        i0 = 1;
        di = 2;
        for (var i=0; i <= intervals; i += 2) {
            xpoints[0][i] = opoints[0][Math.trunc(i/2)];
            xpoints[1][i] = opoints[1][Math.trunc(i/2)];
        }
    }
    else {
        i0 = 0;
        di = 1;
    }

    var sharedLon, wpt, ip, xy,sharedLat,tx, ty;
    for (var i=i0; i <= intervals; i += di) {
        tx= x0+i*dx;
        tx = tx > 360?tx-360:tx;
        tx=tx<0?tx+360:tx;
        ty=y0+i*dy;
        ty=ty>90?ty-180:ty;
        ty=ty<-90?ty+180:ty;
        sharedLon= tx;
        sharedLat= ty;
        wpt= makeWorldPt(sharedLon, sharedLat, csys);
        xy = cc.getImageCoords(wpt);
        if (!xy) xy=makeImagePt(1.e20,1.e20);
        lon[i]= sharedLon;
        lat[i]=sharedLat;
        xpoints[0][i] = xy.x;
        xpoints[1][i] = xy.y;

    }
    return xpoints;
}

function fixPoints(points){

    // Convert points to fixed values.
    var len = points[0].length;
    for (let i=0; i < len; i += 1){
        if (points[0][i] < 1.e10) continue;
        points[0][i] = -10000;
        points[1][i] = -10000;

    }

    return points;
}

function isInBounds(bounds, x,y, i) {
    return (x[i] > -1000 && x[i+1] > -1000 &&
    ((x[i] >= bounds.x) && ((x[i] - bounds.x) < bounds.width) &&
        (y[i] >= bounds.y) && ((y[i]-bounds.y) < bounds.height) ||
        // bounds check on x[i+1], y[i+1]
        (x[i+1] >= bounds.x) && ((x[i+1] - bounds.x) < bounds.width) &&
        (y[i+1] >= bounds.y) && ((y[i+1]-bounds.y) < bounds.height)));

}


function drawLabeledPolyLine (drawData, bounds,  label, labelPoint, slopAngle, isLonLine, x, y, aitoff,screenWidth, useLabels,cc,plot){



    //add the  draw line data to the drawData
    let ipt0, ipt1;
    if(!x) return;

    let pointCount=0;
    const plotType = plot.plotType;
    for (let i=0; i<x.length-1; i+=1) {
        //check the x[i] and y[i] are inside the image screen
        ipt0= makeImagePt(x[i],y[i]);
        ipt1= makeImagePt(x[i+1], y[i+1]);
        let inbounds;

        if (isHiPSAitoff(plot)) {
            inbounds= bounds ? isInBounds(bounds,x,y,i) : true;
            inbounds= inbounds && !cc.coordsWrap(ipt0,ipt1,1);
        }
        else {
            inbounds= bounds && isInBounds(bounds,x,y,i);
        }

        if (inbounds) {
            //For image, the ra/dec interval is 8, so the points needed to be checked if they are located within the interval
            //For hips, the range for ra is 360, so no check is needed.
            if ( plotType==='hips' ||
                 plotType==='image' && (!aitoff  ||  ((Math.abs(ipt1.x-ipt0.x)<screenWidth /8 ) && (aitoff))) ) {

                 drawData.push(ShapeDataObj.makeLine(ipt0, ipt1));
                 pointCount++;
            }
        } //if
    } // for

    // add label location, text, and angle
    if (useLabels  &&  (plotType==='image' &&  pointCount>2  || plotType==='hips' &&  pointCount>1)  ){
       drawData.push(ShapeDataObj.makeText(labelPoint, label, slopAngle+'deg', isLonLine ));
    }
}
function getHiPsLabelPositionAndAngle(xLines, yLines, labels, cc, csys, centerWpt, labelFormat){

    const  lineCount = labels.length;
    let isLonLine, lon, lat, slope,slopAngle;
    let  scpt0,scpt1;

    let lonLinePos=[];
    let lonLineAngles=[];
    let latLinePos=[];
    let latLineAngles=[];

    const isHms = labelFormat === 'hms' && (csys === CoordinateSys.EQ_J2000 || csys === CoordinateSys.EQ_B1950);

    for (let i=0; i<lineCount; i++) {
        isLonLine = i < lineCount / 2 ? true : false;
        if (isLonLine){
                lon = isHms ? CoordUtil.convertStringToLon(labels[i], csys) : parseFloat(labels[i]);
                lat = middleLatValue[0];
                scpt0 = cc.getScreenCoords(makeWorldPt(lon, lat, csys));
                scpt1 = cc.getScreenCoords(makeWorldPt(lon, middleLatValue[1], csys));
                if (scpt0 && scpt1) {
                    slope = (scpt1.y - scpt0.y) / (scpt1.x - scpt0.x);
                    slopAngle = Math.atan(slope) * 180 / Math.PI;
                }

                lonLineAngles.push(slopAngle);
                lonLinePos.push(scpt0);

        }
        else {

            lat = isHms ? CoordUtil.convertStringToLat(labels[i], csys) : parseFloat(labels[i]);
            lon = centerWpt.x;

            scpt0 = cc.getScreenCoords(makeWorldPt(lon, lat, csys));
            scpt1 = cc.getScreenCoords(makeWorldPt(nearCenterLonValue, lat, csys));
            if (scpt0 && scpt1) {
                const slope = (scpt1.y - scpt0.y) / (scpt1.x - scpt0.x);
                slopAngle = Math.atan(slope) * 180 / Math.PI;
            }
            latLinePos.push(scpt0);
            latLineAngles.push(slopAngle);
        }

    }

    // checkAndUpdateLabelAngles(lonLineAngles);
    // checkAndUpdateLabelAngles(latLineAngles);

    return {lonLinePos, lonLineAngles, latLinePos, latLineAngles};
}

/*
 Use two workspace points to find the slop of the line, the angle = arctan(slope) are in the range [-90, 90].
 For all lon(or lat) lines, the slope may be positive or negative.  The text label is placed above the grid lines
 if the angle is positive and under the line if the angle is negative. In such case, if some lon lines have positive
 angle and some has negative angle, the negative angle is changed to 180+angle which is postive.  Thus, the label text
 are all placed in the same manner.

 check to see if all angles to make sure they are all the same positive or negative.
 If any of them are not the same sign as the other, some of the labels are above the line and some are under the line
 because the native angle.  Since  arctan(a) lies [-90, 90] and the tag(a)=tag(a+n*PI), if a <0, use 180+a
 */
function checkAndUpdateLabelAngles(angleArr){
    var negatives = angleArr.filter((e) => e < 0);
    if (negatives.length != 0 && negatives.length != angleArr.length) {
        for (let i = 0; i < angleArr.length; i++) {
            if (angleArr[i] < 0) {
                angleArr[i] += 180;
            }
        }
    }
}
function getImageLabelPositionAndAngle(xLines,yLines,cc){
    const  lineCount = xLines.length;
    var ipt0, ipt1, wpt0, wpt1;
    var slopAngle;
    let isLonLine=false;
    let lonLineAngles=[];
    let latLineAngles=[];
    let lonLinePos=[];
    let latLinePos=[];
    let midLinePos;

    for (let i=0; i<lineCount; i++) {
         isLonLine = i < lineCount / 2 ? true : false;
         midLinePos=Math.ceil(xLines[i].length / 2) - 1;

         ipt0 = makeImagePt(xLines[i][midLinePos], yLines[i][midLinePos]);
         ipt1 = makeImagePt(xLines[i][midLinePos + 1], yLines[i][midLinePos + 1]);
         wpt0 = cc.getScreenCoords(ipt0);
         wpt1 = cc.getScreenCoords(ipt1);

         const slope = (wpt1.y - wpt0.y) / (wpt1.x - wpt0.x);
         slopAngle = Math.atan(slope) * 180 / Math.PI; //Math.atan(slope) return [-pi/2, pi/2]
         if (isLonLine){
             lonLineAngles.push(slopAngle);
             lonLinePos.push(wpt0);
         }
         else {
             latLineAngles.push(slopAngle);
             latLinePos.push(wpt0);
         }

     }

    checkAndUpdateLabelAngles(lonLineAngles);
    checkAndUpdateLabelAngles(latLineAngles);
    return {lonLinePos, lonLineAngles, latLinePos, latLineAngles};
}

function drawLines(bounds,  labels, xLines,yLines, aitoff,screenWidth, useLabels,cc, plot, csys=null, centerWpt=null,labelFormat=null) {
    // Draw the lines previously computed.
    //get the locations where to put the labels
    let drawData=[];

    const  lineCount = xLines.length;
    // For image grid, we regrid the levels so that the line count is always the same for lon and lat (each have 11 lines)
    //For HiPs image, the total lines (lon and lat) is not fixed.  When the image is not in the full range, we add lines dynamically.
    //But the lon and lat lines are always have the same count.
    const lonLineCount = lineCount/2;
    let isLonLine=false, labelPosition, labelAngle;

   const {lonLinePos, lonLineAngles, latLinePos, latLineAngles}=isHiPS(plot) ?
        getHiPsLabelPositionAndAngle(xLines,yLines,labels, cc, csys, centerWpt, labelFormat)
       :getImageLabelPositionAndAngle(xLines,yLines,cc);
    for (let i=0; i<lineCount; i++) {
            isLonLine = i<lonLineCount?true:false;
            if (isLonLine){
                labelPosition=lonLinePos[i];
                labelAngle=lonLineAngles[i];
               // console.log( 'label='+labels[i] + ' labelPosition='+ labelPosition + 'labelAngle= '+labelAngle);
            }
            else {
                labelPosition=latLinePos[i-lonLineCount];
                labelAngle=latLineAngles[i-lonLineCount];

            }
            drawLabeledPolyLine(drawData, bounds, labels[i],labelPosition, labelAngle,isLonLine,
            xLines[i], yLines[i], aitoff,screenWidth, useLabels,cc, plot);

    }
    return drawData;

}


function computeImageGridLines(cc, csys, width,height, screenWidth, numOfGridLines, labelFormat, plot) {

   const range = getRange(csys, width, height, cc);

    const factor = plot.zoomFactor<1?1:plot.zoomFactor;

    //get levels for the whole longitude and latitude range
    var levels = getLevels(range, factor, numOfGridLines);


    /* This is where we do all the work. */
    /* range and levels have a first dimension indicating x or y
     * and a second dimension for the different values (2 for range)
     * and a possibly variable number for levels.
     */
    const labels = getLabels(levels, csys, labelFormat);

    var xLines = [];
    var yLines = [];
    var offset = 0;
    var points=[];
    for (let i=0; i<2; i++) {
        for (let j=0; j<levels[i].length; j++) {
            points= findLine(cc, csys, i, levels[i][j],range, screenWidth, plot);
            xLines[offset] = points[0];
            yLines[offset] = points[1];
            offset += 1;

        }
    }
    return {xLines, yLines, labels};
}

function isEven(value){
    if (value%2 === 0){
        return true;
    }
    else{
        return false;
    }

}

function getLonLevels(viewRange, centerWpt,  cc,csys,poles, isPrimeMeridianVisible, maxLines, plot) {
    var levels = [], range = [];

    //make odd number of lines so that the center line has symmetric lines count at each side
    const numLines = isEven(maxLines) ? maxLines + 1 : maxLines;

    var lon, det;
    if (poles>0){
        det = 360 / (numLines-1);
        for (var i=0; i<numLines; i++){
            levels[i]=i*det;
        }
        range = [0, 360];
        return {levels, range};
    }
    else  if (poles===0 && isPrimeMeridianVisible){
        /*
         When any point in primer meridian circle is visible, there will be a discontinuity in the longitude, that is
         the(0, 360) point is visible.  This point can be anywhere in the image.  Thus the range will be two part,
         one is from 0 increasing to the viewDim border at the left side, the other will be from 360 decreasing to
         the viewDim border at the right side (here I suppose the north pole is up.  If the south pole is up, it is similar.


         The upper range found by four corners is not the correct upper range in this case.   The upper range
         is 360.  We need to find the border value along the right hand side from 360.  Thus, we will have grid lines
         along 0-lower range and 360- border.  To find the border, we can start from 360, decrease a det until the point
         is not in the image, or start from the upper range and decrease a det until the the point
         is not in the image.  However, doing this way, it requires more calculation since all latitude values have to
         be evaluated because the point (0, 36) can be at any latitude position.

         Similar the lower range is not the correct lower range, the real lower range is 0.  So we need to iterate to find the
         real.
        *
        */
         //find the new range
        const lonRange = getRangeForTruePM(plot, csys,  cc, centerWpt, viewRange);

        //calculate det that will be the interval between lines
        det = (lonRange[0]+ 360-lonRange[1])/ (numLines-1);
        var count=0, len;
        lon=centerWpt.x;
        if (centerWpt.x<180){
            //center on the left of PM ---------C----PM------
            //center toward on the lower border, no PM in between
           while (lon<lonRange[0]){
                levels[count]=lon;
                count++;
                lon=centerWpt.x+count*det;
            };


            //center toward upper border, there is PM(0, 360)  in between
            //lon will be from centerWpt.x - 0, and 360 - longRange[1]
            count=0;//reset count
            len=levels.length;
            lon=centerWpt.x-det;

            while ( lon < centerWpt.x || lon>lonRange[1])  {
                levels[len+count]=lon;
                count++;
                lon=centerWpt.x-count*det;
                lon=lon<0?360+lon:lon;

            };
        }
        else {//center on the right of PM
              //------PM---center--------
            lon=centerWpt.x;
            //center toward upper border, no PM in between
            while (lon>lonRange[1] ){
                levels[count]=lon;
                count++;
                lon=centerWpt.x-count*det;
            };

            //go toward lower border, there is PM point in between
            //lon will be centerWpt.x-360, 0-lonRange[0]
            count=0;//reset count
            len=levels.length;
            lon=centerWpt.x+det;
            while ( lon>centerWpt.x || lon<lonRange[0]){
                levels[len+count]=lon;
                count++;
                lon=centerWpt.x+count*det;
                lon = lon>360? lon-360:lon;

            };
        }


        range = [0, 360];
        return {levels, range};

    }
    //Is a regular rectangle range
    else {
        det = (viewRange[1] - viewRange[0]) / (numLines-1);
        for (let i = 0; i <numLines; i++) {
            levels[i] = viewRange[0] + i * det;
        }

        range = viewRange;
        return {levels, range};
    }

}

function getLatLevels(viewRange, poles,  maxLines){
    var levels =[];
    var numLines = isEven(maxLines)?maxLines+1:maxLines;

    let range=viewRange;
    if (poles!==0){
        switch (poles){
            case 1:
                range[1] = 90;

                break;
            case 2:
                range[0] = -90;
                break;
            case 3:
                range=[-90,90];
                break;
        }

    }

    const det = (range[1] - range[0]) / (numLines - 1);
    for (let i = 0; i < numLines; i++) {
        levels[i] = range[0] + i * det;
    }

    return{levels, range};

}

function getLevelsAndRangeForHips(csys,  cc, viewBorder, centerWp, maxLines,plot) {


    const {poles, isPrimeMeridianVisible} = getViewableAreaInfo(cc, csys, viewBorder,centerWp);

    const {levels:lonLevels, range:lonRange}= getLonLevels(viewBorder[0],  centerWp, cc,csys,poles, isPrimeMeridianVisible, maxLines,plot);
    const {levels:latLevels, range:latRange}=getLatLevels(viewBorder[1], poles, maxLines);

    const levels = [lonLevels, latLevels];
    const range=[lonRange, latRange];

    return {levels, range};

}

function computeHipGridLines(cc, csys,  screenWidth, nGridLines, labelFormat, plot, fov, centerWp) {

    const fullRange = [[0, 360], [-90, 90]];

    const factor = plot.zoomFactor<1?1:plot.zoomFactor;
    var numOfGridLines=2*nGridLines;

    /*get the view border, NOTE, the border does not mean the maximum and minimum of the range. If there the
     Prime meridian is in the image, the minimum of border is the value in the range from 0-minBorder, and the
     maximum border is the range [maximum border - 360]
   */

    const  {corners, viewBorder} = getViewPortInfo(plot, csys,  cc, fullRange);

    var levels, range;

    if ( corners && corners.indexOf(null)===-1) {
        numOfGridLines=nGridLines;
        const {levels:l, range:r} = getLevelsAndRangeForHips( csys,  cc,viewBorder, centerWp,numOfGridLines,plot);
        levels=l;
        range=r;
    }
    else {
        levels = getLevels(fullRange, factor, numOfGridLines);
        range = fullRange;

    }

    //find the appropriate latitude to put the labels
    const zeroLatIdx = levels[1].indexOf(0);
    const midIdx = parseInt(zeroLatIdx / 2);

    /**
     * This block is taking care of the sphere case.  If the sphere is visible, it has three condition,
     * 1. north pole [0, 90] is facing users [a,..0, 10, 20, ...90]
     * 2. south pole [0, -90] is facing users [-90, ..., 0, .... a]
     * 3. no pole is visible
     *
     * In order the labels are visible, we don't use the middle line.  Instead we zero index as a measure.
     * If the north pole is facing to us, we use the line no = zeroLatIdx + half/positve_lat_length;
     * If the south pole is facing us, half of the zeroLatIndex.
     */
    //When the [0, -90] is facing user, we want to make sure the labels are visible.
    if (zeroLatIdx!=-1  && cc.pointInView(makeWorldPt(0, -90, csys)) && !cc.pointInView(makeWorldPt(0, 90, csys))) {

         middleLatValue[0] = levels[1][midIdx ];
         middleLatValue[1] = levels[1][midIdx  + 1];

     }   //When the [0, 90] is facing user, we want to make sure the labels are visible.
     else if (zeroLatIdx!=-1  && cc.pointInView(makeWorldPt(0, 90, csys)) &&  !cc.pointInView(makeWorldPt(0, -90, csys))) {
         middleLatValue[0] = levels[1][zeroLatIdx + midIdx];
         middleLatValue[1] = levels[1][zeroLatIdx + midIdx + 1];


     }
    else { //all other cases are falling in here
            middleLatValue[0] = levels[1][parseInt(levels[1].length / 2)];
            middleLatValue[1] = levels[1][parseInt(levels[1].length / 2) + 1];

    }


    var dist=10000, d;
    let i;
    for (i=0; i<levels[0].length; i++){
        if (!levels[0][i] || levels[0][i].toFixed(4)===centerWp.x.toFixed(4)) continue;
        d = Math.abs(levels[0][i]-centerWp.x);
        if (d<dist){
            dist=d;
            nearCenterLonValue=levels[0][i];
        }

    }
    /* This is where we do all the work. */
    /* range and levels have a first dimension indicating x or y
     * and a second dimension for the different values (2 for range)
     * and a possibly variable number for levels.
     */
    const labels = getLabels(levels, csys, labelFormat, fov);

    var xLines = [];
    var yLines = [];
    var offset = 0;
    var points=[];
    for (i=0; i<2; i++) {
        for (let j=0; j<levels[i].length; j++) {
            points= findLine(cc, csys, i, levels[i][j], range, screenWidth, plot.plotType);
            xLines[offset] = points[0];
            yLines[offset] = points[1];
            offset += 1;

        }
    }
    return {xLines, yLines, labels};
}


function isSorted(arr){
    var sorted = true;

    for (let i = 0; i < arr.length - 1; i++) {
        if (arr[i] > arr[i+1]) {
            sorted = false;
            break;
        }
    }
    return sorted;
}
function getPoles(cc, csys){
    let poles=0,  lon, lat; //north pole: poles=1; south pole: poles = 2; both poles:poles=3

    //check if north pole is visible
    lon=0;
    lat=90;
    if (cc.pointInView(makeWorldPt(lon, lat, csys))){
        poles +=1;
    }

    lat=-90;
    if (cc.pointInView(makeWorldPt(lon, lat, csys))){
        poles +=2;
    }
    return poles;
}
function getViewableAreaInfo(cc, csys, viewBorder, centerWp){

    let isPrimeMeridianVisible=false,  lat;
    const poles=getPoles(cc, csys); //north pole: poles=1; south pole: poles = 2; both poles:poles=3


    const arr = [viewBorder[0][0], centerWp.x, viewBorder[0][1]];
    if (isSorted(arr)) {
        const det=(viewBorder[1][1]-viewBorder[1][0])/100;
        for (var i = 0; i <= 100; i++) {
            lat = viewBorder[1][0] + i * det;
            if (cc.pointInView(makeWorldPt(0, lat, csys))) {
                isPrimeMeridianVisible = true;
                break;

            }
        }
    }
    else {
        isPrimeMeridianVisible=true;
    }


    return {poles, isPrimeMeridianVisible};
}


/**
 *
 * @param ranges
 * @param factor
 * @param maxLines
 * @returns {Array}
 */
function getLevels(ranges,factor, maxLines){

    var levels=[];
    var  min, max, delta;
    for (let i=0; i<ranges.length; i++){
        /* Expect max and min for each dimension */
        if (ranges[i].length!==2){
            levels[i]=[];
        }
        else {
            min = ranges[i][0];
            max =ranges[i][1];
            if (min===max){
                levels[i]=[];
            }
            else if ( Math.abs(min - (-90.0))  < 0.1 && Math.abs(max - 90.0) <0.1){ //include both poles
                levels[i]= [-75.,-60., -45., -30., -15., 0., 15., 30.,  45., 60.,  75.];
            }
            else {
                /* LZ DM-10491: introduced this simple algorithm to calculate the intervals.  The previous one
                commented below caused line missing. For example, 45,0 wise, 45, 90 wise etc.

                The algorithm previous used (commented ) missed one line. I don't understand the purpose of
                 the algorithm.  The levels can be simply defined as the loop below
                 */
                levels[i] = [];

                delta =calculateDelta (min, max,factor);


                var count = Math.ceil ( (max -min)/delta);
                if (count<=2){
                    delta=delta/2.0;
                    count=2*count;
                }
                for (let j=0; j<count; j++){
                    levels[i][j] = j*delta + min;
                  if (!i && levels[i][j] > 360){
                    levels[i][j] -= 360;
                  }
                  else if (!i && levels[i][j] < 0){
                    levels[i][j] += 360;
                  }

                }


                /* We've now got the increment between levels.
                 * Now find all the levels themselves.
                 */

                //LZ comment out the original algorithm to calculate the intervals
                /* min=(max<min)?min-360:min;
                 val = min<0? min-min%delta : min + delta-min%delta;
                 count=0;
                 while (val + count*delta <= max){
                     count++;
                 }
                 if (count<=2){
                     delta=delta/2.0;
                     count=2*count;
                 }
                 levels[i] = [];
                 for (let j=0; j<count; j++){
                     levels[i][j] = j*delta + val;
                     if (!i && levels[i][j] > 360){
                         levels[i][j] -= 360;
                     }
                     else if (!i && levels[i][j] < 0){
                         levels[i][j] += 360;
                     }

                 }*/
            }
        }

    }

    return levels.map( (row)=>{
        if (row.length<maxLines  || row.length>maxLines){
            return Regrid(row,  maxLines, true);
        }
        else {
            return row;
        }

     });

}

