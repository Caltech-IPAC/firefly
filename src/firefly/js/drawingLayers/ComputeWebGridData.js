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
 */

import { makeWorldPt, makeImagePt,makeImageWorkSpacePt} from '../visualize/Point.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import CoordinateSys from '../visualize/CoordSys.js';
import CoordUtil from '../visualize/CoordUtil.js';
import numeral from 'numeral';
import { getDrawLayerParameters} from './WebGrid.js';
const precision3Digit = '0.000';
const RANGE_THRESHOLD = 1.02;
const minUserDistance= 0.25;   // user defined max dist. (deg)
const maxUserDistance= 3.00;   // user defined min dist. (deg)
var userDefinedDistance = false;
import {Regrid} from '../util/Interp/Regrid.js';

const nPoints=20;

/**
 * This method does the calculation for drawing data array
 * @param plot - primePlot object
 * @param cc - the CoordinateSys object
 * @param useLabels - use Labels
 * @param numOfGridLines
 * @return a DrawData object
 */
export function makeGridDrawData (plot,  cc, useLabels, numOfGridLines=10){


    const {width, height, screenWidth, csys,labelFormat} = getDrawLayerParameters(plot);
    if (width > 0 && height >0) {
        const bounds = new Rectangle(0, 0, width, height);
        var factor =  plot.zoomFactor;
        if (factor < 1.0) factor = 1.0;
        const range = getRange(csys, width, height, cc);
        //calculate the levels
        const levelsCalcualted = getLevels(range, factor);
        //regrid the levels if the line counts is less than 10
        const levels = adjustLevels(levelsCalcualted, numOfGridLines);
        const labels = getLabels(levels, csys, labelFormat);
        const {xLines, yLines} = computeLines(cc, csys, range, levels, screenWidth);
        const wpt = cc.getWorldCoords(makeImageWorkSpacePt(1, 1), csys);
        const aitoff = (!wpt);
        const deltaX = (levels[0][1]-levels[0][0])/10.0;
        const deltaY = (levels[1][1]-levels[1][0]) /10.0;
        return  drawLines(bounds, labels, xLines, yLines, aitoff, screenWidth, useLabels, cc, deltaX, deltaY);

    }
}

function adjustLevels(levels,numOfGridLines){
    const nL0 = levels[0].length;
    const nL1 = levels[1].length;
    var newLevels = levels;
    if (nL0<numOfGridLines){
        newLevels[0] = Regrid(levels[0], numOfGridLines);
    }
    if (nL1<numOfGridLines){
        newLevels[1] = Regrid( levels[1], numOfGridLines);
    }
    return newLevels;
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

        var wpt =cc.getWorldCoords(makeImageWorkSpacePt(x,y),csys);
        //look for lower and upper longitude and latitude
        if (wpt) {
            vals[0] = wpt.getLon();
            vals[1] = wpt.getLat();

            if (wrap && vals[0] > 180) vals[0] = vals[0]-360;
            if (wrap!=null //true and false
                || wrap==null && cc.pointInPlot(makeWorldPt(vals[0], vals[1], csys))//temporary solution to solve the problem reported.
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



    if (cc.pointInPlot(makeWorldPt(sharedLon, sharedLat, csys))){
        range[0][0] = -179.999;
        range[0][1] =  179.999;
        range[1][1] = 90;
        poles += 1;
        wrap = true;
    }

    sharedLon= 0.0;
    sharedLat= -90.0;
    if (cc.pointInPlot(makeWorldPt(sharedLon, sharedLat, csys))){
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
        if (cc.pointInPlot(makeWorldPt(sharedLon, sharedLat, csys)))
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
            if (cc.pointInPlot(makeWorldPt(sharedLon, sharedLat,csys))) {
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

/**
 *
 * @param ranges - two -dimension array of the x and y ranges
 * @param factor - zoom factor
 * @returns  levels {array} number of line intervals
 */
function getLevels(ranges,factor){

    var levels=[];
    var  min, max, delta;
    var val, count;
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
            else if ( Math.abs(min - (-90.0))  < 0.1 && Math.abs(max - 90.0) <0.1){
                levels[i]= [-75.,-60., -45. -30., -15., 0., 15., 30.,  45., 60.,  75.];
            }
            else {

                delta =calculateDelta (min, max,factor);
                min=(max<min)?min-360:min;
                val = min<0? min-min%delta : min + delta-min%delta;
                count=0;
                while (val + count*delta < max){
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

                }
            }
        }

    }
    return levels;
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
 * Find the labels according to the coordinates
 * @param levels
 * @param csys
 * @param labelFormat
 * @returns {Array}
 */

function getLabels(levels,csys, labelFormat) {


    var labels = [];
    var offset = 0;
    var delta;

    var sexigesimal = (csys.toString()===CoordinateSys.EQ_J2000 || csys.toString()===CoordinateSys.EQ_B1950);
    for (let i=0; i < 2; i++){

         if (levels[i].length >=2){
            delta = levels[i][1]-levels[i][0];
            delta = delta<0?delta+360:delta;
         }

         var lon, lat;
         for (let j=0; j < levels[i].length; j++) {
              if (sexigesimal) {
                  if (i === 0) { //ra labels
                      lon = CoordUtil.convertLonToString(levels[i][j], csys);
                      labels[offset] = labelFormat === 'hms' ? lon : CoordUtil.convertStringToLon(lon, csys).toFixed(3);
                  }
                  else {
                      lat = CoordUtil.convertLatToString(levels[i][j], csys);
                      labels[offset] = labelFormat === 'hms' ? lat : CoordUtil.convertStringToLat(lat, csys).toFixed(3);
                  }
              }
              else {
                  labels[offset] = `${numeral(levels[i][j]).format(precision3Digit)}`;
              }
             offset += 1;
        }
    }

    return labels;
}
/**
 * @desc calculate lines
 *
 * @param  {object} cc - the CoordinateSys object
 * @param  {object} csys - the coordinate system the grid is drawing with
 * @param {object} direction - an integer,  0 and 1 to indicate which direction the lines are
 * @param {double} value - x or y value in the image
 * @param {object} range
 * @param {double} screenWidth - a screen width
 * @return the points found
 */
function findLine(cc,csys, direction, value, range, screenWidth){

    var intervals;
    var x, dx, y, dy;

    if (!direction )  {// X
        x  = value;
        dx = 0;
        y  = range[1][0];
        dy = (range[1][1]-range[1][0])/4;
    }
    else { // Y

        y = value;
        dy = 0;
        x = range[0][0];
        dx = (range[0][1]-range[0][0]);
        dx = dx < 0?dx+360:dx;
        dx /= 4;
    }
    var opoints = findPoints(cc, csys,4, x, y, dx, dy, null);
    var  straight = isStraight(opoints);

    var npoints = opoints;
    intervals = 8;
    var nstraight;
    while (intervals < screenWidth) {
        dx /= 2;
        dy /= 2;
        npoints = findPoints(cc, csys, intervals, x, y, dx, dy, opoints);
        nstraight = isStraight(npoints);
        if (straight && nstraight) {
            return fixPoints(npoints);
        }
        straight = nstraight;
        opoints = npoints;
        intervals *= 2;
    }

    const points = fixPoints(npoints);
    //regrid the points found to 20 points
    if (points.length<20){
        return  Regrid(findPoints, nPoints);
    }

    return points;
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

function findPoints(cc,csys, intervals, x0, y0,dx, dy,  opoints){


    // NOTE: there are intervals separate intervals so there are
    //                 intervals+1 separate points,
    // Hence the <= in the for loops below.

    var  xpoints = [[],[]];
    var i0, di;
    if (opoints)  {
        i0 = 1;
        di = 2;
        for (let i=0; i <= intervals; i += 2) {
            xpoints[0][i] = opoints[0][Math.trunc(i/2)];
            xpoints[1][i] = opoints[1][Math.trunc(i/2)];
        }
    }
    else {
        i0 = 0;
        di = 1;
    }

    var sharedLon, wpt, ip, xy,sharedLat,tx;
    for (let i=i0; i <= intervals; i += di) {
        tx= x0+i*dx;
        tx = tx > 360?tx-360:tx;
        tx=tx<0?tx+360:tx;
        sharedLon= tx;
        sharedLat= y0+i*dy;
        wpt= makeWorldPt(sharedLon, sharedLat, csys);
        ip = cc.getImageWorkSpaceCoords(wpt);
        if (ip) {
            xy = makeImagePt(ip.x, ip.y);
        }
        else {
            xy=makeImagePt(1.e20,1.e20);
        }
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

function count(array,val){
    var result=0;
    for(var a in array) {
        if (array[a] == val) {
            result++;
        }
    }
    return result;
}


function drawLabeledPolyLine (drawData, bounds,  label,  x, y, aitoff,screenWidth, useLabels,cc, deltaX, deltaY){


    //add the  draw line data to the drawData
    var ipt0, ipt1;
    var slopAngle;
    var labelPoint;
    for (let i=0; i<x.length-1; i+=1) {
        //check the x[i] and y[i] are inside the image screen
        if (x[i] > -1000 && x[i+1] > -1000 &&
            ((x[i] >= bounds.x) &&
            ((x[i] - bounds.x) < bounds.width) &&
            (y[i] >= bounds.y) &&
            ((y[i]-bounds.y) < bounds.height) ||
                // bounds check on x[i+1], y[i+1]
            (x[i+1] >= bounds.x) &&
            ((x[i+1] - bounds.x) < bounds.width) &&
            (y[i+1] >= bounds.y) &&
            ((y[i+1]-bounds.y) < bounds.height))) {
            ipt0= makeImageWorkSpacePt(x[i],y[i]);
            ipt1= makeImageWorkSpacePt(x[i+1], y[i+1]);
            if (!aitoff  ||  ((Math.abs(ipt1.x-ipt0.x) <screenWidth /8 ) && (aitoff))) {
                drawData.push(ShapeDataObj.makeLine(ipt0, ipt1));

                //find the middle point of the line, index from 0, so minus 1
                if (i===Math.round(x.length/2)-1 ) {

                    var wpt1 = cc.getScreenCoords(ipt0);
                    var wpt2 = cc.getScreenCoords(ipt1);
                    const slope = (wpt2.y - wpt1.y) / (wpt2.x - wpt1.x);
                    slopAngle =  Math.atan(slope) * 180 / Math.PI;

                    //difficult to find a padding to work for all coordinates. Adding deltaX(Y) does not make a big difference
                    labelPoint = makeImageWorkSpacePt(x[i]+deltaX , y[i]+deltaY);

                }
            }
        } //if
    } // for


    // draw the label.
    if (useLabels  ){
        drawData.push(ShapeDataObj.makeText(labelPoint, label, slopAngle+'deg'));
    }
}

function drawLines(bounds, labels, xLines,yLines, aitoff,screenWidth, useLabels,cc, deltaX, deltaY) {
    // Draw the lines previously computed.
    //get the locations where to put the labels
    var drawData=[];

    var  lineCount = xLines.length;
    for (let i=0; i<lineCount; i+=1) {
            drawLabeledPolyLine(drawData, bounds, labels[i] ,
            xLines[i], yLines[i], aitoff,screenWidth, useLabels,cc,deltaX, deltaY);
    }
    return drawData;

}


function computeLines(cc, csys, range, levels,screenWidth) {
    /* This is where we do all the work. */
    /* range and levels have a first dimension indicating x or y
     * and a second dimension for the different values (2 for range)
     * and a possibly variable number for levels.
     */

    var xLines = [];
    var yLines = [];
    var offset = 0;
    var points=[];
    for (let i=0; i<2; i++) {
        for (let j=0; j<levels[i].length; j++) {
            points = findLine(cc, csys,i, levels[i][j], range,screenWidth);
            xLines[offset] = points[0];
            yLines[offset] = points[1];
            offset += 1;
        }
    }
    return {xLines, yLines};
}



