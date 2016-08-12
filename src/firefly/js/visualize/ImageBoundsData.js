/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import CoordinateSys from './CoordSys.js';
import VisUtil from './VisUtil.js';
import {makeImagePt} from './Point';



function getJ2XY(wp) {
    if (CoordinateSys.EQ_J2000!==wp.getCoordSys()) {
        wp= VisUtil.convert(wp,CoordinateSys.EQ_J2000);
    }
    return {x: wp.getLon(), y: wp.getLat()};
}



/**
 *
 * Return function that returns false it the point is definitely not in cc.  It returns true if the point might be in the cc.
 * Used for tossing out points that we know that are not in cc without having to do all the math.  It is much faster.
 * When called the return function will take and WorldPt and return true in we guess it might be in the bounds,
 * false if we know that it is not in the bounds
 * @param {object} cc
 */
export const makeRoughGuesser= function(cc) {


    var dataWidth= cc.dataWidth;
    var dataHeight= cc.dataHeight;
    var topLeft= cc.getWorldCoords(makeImagePt(0,0));
    var topRight= cc.getWorldCoords(makeImagePt(0,dataWidth));
    var bottomLeft= cc.getWorldCoords(makeImagePt(dataHeight,0));
    var bottomRight= cc.getWorldCoords(makeImagePt(dataHeight,dataWidth));
    var scale= cc.getImagePixelScaleInDeg();


    var wPad= 25;
    var hPad= 25;

    var minRa= 5000;
    var maxRa= -5000;
    var minDec=5000;
    var maxDec= -5000;

                    // if I can find the corners then rough guess will not work
    if (!topLeft ||  !topRight || !bottomLeft || !bottomRight) return () => true;

    [topLeft, topRight, bottomLeft, bottomRight].forEach( (wp) => {
        if (wp.getLon() < minRa) minRa= wp.getLon();
        if (wp.getLon() > maxRa) maxRa= wp.getLon();
        if (wp.getLat() < minDec) minDec= wp.getLat();
        if (wp.getLat() > maxDec) maxDec= wp.getLat();
    });


    minRa-= (wPad *scale);
    minDec-= (hPad*scale);
    maxDec+= (hPad*scale);
    maxRa+= (wPad*scale);

    var imageSize= scale * Math.max(dataHeight,dataWidth);
    var checkDeltaTop=    90-(2*imageSize);
    var checkDeltaBottom= -90 + (2*imageSize);

    var wrapsRa= (maxRa-minRa) > 90;
    var northPole= minDec>checkDeltaTop;
    var southPole= maxDec<checkDeltaBottom;


    if (northPole) { //if near the j2000 "north pole" then ignore ra check
        return (wp) => getJ2XY(wp).y> minDec;
    }
    else if (southPole) { //if near the j2000 "south pole" then ignore ra check
        return (wp) => getJ2XY(wp).y< maxDec;
    }
    else if (wrapsRa) { // if image wraps around 0 ra
        return (wp) => {
            var {x,y}= getJ2XY(wp);
            var retval= y> minDec && y< maxDec;
            if (retval) retval= x> maxRa || x< minRa;
            return retval;
        };
    }
    else { // normal case
        return (wp) => {
            var {x,y}= getJ2XY(wp);
            return (x> minRa && y> minDec && x< maxRa && y< maxDec);
        };
    }
};



