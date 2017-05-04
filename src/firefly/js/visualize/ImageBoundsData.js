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


    const {dataWidth, dataHeight}= cc;
    const topLeft= cc.getWorldCoords(makeImagePt(0,0));
    const topRight= cc.getWorldCoords(makeImagePt(dataWidth,0));
    const bottomRight= cc.getWorldCoords(makeImagePt(dataWidth,dataHeight));
    const bottomLeft= cc.getWorldCoords(makeImagePt(0,dataHeight));
    const scale= cc.getImagePixelScaleInDeg();


    const wPad= 25;
    const hPad= 25;

    let minRa= 5000;
    let maxRa= -5000;
    let minDec=5000;
    let maxDec= -5000;

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

    const imageSize= scale * Math.max(dataHeight,dataWidth);
    const checkDeltaTop=    90-(2*imageSize);
    const checkDeltaBottom= -90 + (2*imageSize);

    const wrapsRa= (maxRa-minRa) > 90;
    const northPole= minDec>checkDeltaTop;
    const southPole= maxDec<checkDeltaBottom;


    if (northPole) { //if near the j2000 "north pole" then ignore ra check
        return (wp) => getJ2XY(wp).y> minDec;
    }
    else if (southPole) { //if near the j2000 "south pole" then ignore ra check
        return (wp) => getJ2XY(wp).y< maxDec;
    }
    else if (wrapsRa) { // if image wraps around 0 ra
        return (wp) => {
            const {x,y}= getJ2XY(wp);
            let retval= y> minDec && y< maxDec;
            if (retval) retval= x> maxRa || x< minRa;
            return retval;
        };
    }
    else { // normal case
        return (wp) => {
            const {x,y}= getJ2XY(wp);
            return (x> minRa && y> minDec && x< maxRa && y< maxDec);
        };
    }
};



