/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, padEnd, fill} from 'lodash';
import numeral from 'numeral';
import VisUtil from '../VisUtil.js';
import CoordUtil from '../CoordUtil.js';
import CoordinateSys from '../CoordSys.js';
import {showMouseReadoutOptionDialog} from './MouseReadoutOptionPopups.jsx';


const precision7Digit = '0.0000000';
const precision1Digit = '0.0';

const myFormat= (v,precision) => !isNaN(v) ? numeral(v).format(padEnd('0.',precision+1,'0')) : '';


const labelMap = {
    eqj2000hms: 'EQ-J2000:',
    eqj2000DCM: 'EQ-J2000:',
    galactic: 'Gal:',
    eqb1950: 'Eq-B1950:',
    fitsIP: 'Image Pixel:',
    pixelSize: 'Pixel Size:',
    sPixelSize: 'Screen Pixel Size:',
    healpixPixel: 'Pixel: ',
    healpixNorder: 'Norder: ',
};


export function getNonFluxDisplayElements(readoutItems, readoutPref, isHiPS= false) {
    const objList= getNonFluxReadoutElements(readoutItems,  readoutPref, isHiPS);

    const {imageMouseReadout1, imageMouseReadout2, hipsMouseReadout1,
                     hipsMouseReadout2, pixelSize, healpixPixel, healpixNorder} = objList;


    let readout1, readout2, healpixPixelReadout, healpixNorderReadout;
    let showReadout1PrefChange, showReadout2PrefChange;

    if (isHiPS) {
        readout1= {value:hipsMouseReadout1, label: labelMap[readoutPref.hipsMouseReadout1]};
        readout2= {value:hipsMouseReadout2, label: labelMap[readoutPref.hipsMouseReadout2]};
        showReadout1PrefChange= () => showMouseReadoutOptionDialog('hipsMouseReadout1', readoutPref.hipsMouseReadout1);
        showReadout2PrefChange= () => showMouseReadoutOptionDialog('hipsMouseReadout2', readoutPref.hipsMouseReadout2);
        healpixPixelReadout= {value:healpixPixel, label: labelMap.healpixPixel};
        healpixNorderReadout= {value:healpixNorder, label: labelMap.healpixNorder};
    }
    else {
        readout1= {value:imageMouseReadout1, label: labelMap[readoutPref.imageMouseReadout1]};
        readout2= {value:imageMouseReadout2, label: labelMap[readoutPref.imageMouseReadout2]};
        showReadout1PrefChange= () => showMouseReadoutOptionDialog('imageMouseReadout1', readoutPref.imageMouseReadout1);
        showReadout2PrefChange= () => showMouseReadoutOptionDialog('imageMouseReadout2', readoutPref.imageMouseReadout2);
    }

    return {
        readout1, readout2,
        showReadout1PrefChange, showReadout2PrefChange, healpixPixelReadout, healpixNorderReadout,
        pixelSize: {value: pixelSize, label: labelMap[readoutPref.pixelSize]},
        showPixelPrefChange:() => showMouseReadoutOptionDialog('pixelSize', readoutPref.pixelSize)
    };
}


export function getNonFluxReadoutElements(readoutItems, readoutPref, isHiPS= false) {
    const keysToUse= Object.keys(readoutPref).filter( (key) =>
        isHiPS ?
            key.startsWith('hips') || !key.startsWith('image') :
            !key.startsWith('hips') || key.startsWith('image'));

    const retList={};
    keysToUse.forEach( (key) =>  {
        retList[key]=getReadoutElement(readoutItems,  readoutPref[key] );
    });
    return retList;
}



/**
 * Get the mouse readouts from the standard readout and convert to the values based on the toCoordinaeName
 * @param readoutItems
 * @param readoutKey
 * @returns {*}
 */
export function getReadoutElement(readoutItems, readoutKey) {

    if (!readoutItems) return '';

    const wp= get(readoutItems, 'worldPt.value');
    switch (readoutKey) {
        case 'pixelSize':
            return makePixelReturn(readoutItems.pixel);
        case 'sPixelSize':
            return makePixelReturn(readoutItems.screenPixel);
        case 'eqj2000hms':
            return makeCoordReturn(wp, CoordinateSys.EQ_J2000, true);
        case 'eqj2000DCM' :
            return makeCoordReturn(wp, CoordinateSys.EQ_J2000);
        case 'galactic' :
            return makeCoordReturn(wp, CoordinateSys.GALACTIC);
        case 'supergalactic' :
            return makeCoordReturn(wp, CoordinateSys.SUPERGALACTIC);
        case 'eqb1950' :
            return makeCoordReturn(wp, CoordinateSys.EQ_B1950, true);
        case 'fitsIP' :
            return makeImagePtReturn(get(readoutItems, 'imagePt.value'));
        case 'healpixPixel' :
            const {healpixPixel}= readoutItems;
            return (healpixPixel && healpixPixel.value) ? `${healpixPixel.value}` : '';
        case 'healpixNorder' :
            const {healpixNorder}= readoutItems;
            return (healpixNorder && healpixNorder.value) ? `${healpixNorder.value}` : '';
    }

    return '';
}

/**
 * This method passes the standard readout and then get the flux information
 * @param sndReadout
 * @returns {{fluxLabels: Array, fluxValues: Array}}
 */
export function getFluxInfo(sndReadout){

    const fluxObj = [];
    const {REDFlux, GREENFlux, BLUEFlux, nobandFlux}= sndReadout.readoutItems;
    if (sndReadout.threeColor){
        REDFlux && fluxObj.push(REDFlux);
        GREENFlux && fluxObj.push(GREENFlux);
        BLUEFlux && fluxObj.push(BLUEFlux);
    }
    else if (nobandFlux){
        fluxObj.push(nobandFlux);
    }

    const fluxArray= fluxObj.map( ({value,unit,title,precision}) => {
        let fluxValue= '';
        if (!isNaN(value )) {
            const min = Number('0.'+'0'.repeat(precision-1)+'1');
            fluxValue = (Math.abs(value) < 1000  &&Math.abs(value)>min ) ?
                `${myFormat(value, precision)}` :
                value.toExponential(6).replace('e+', 'E');
            if (unit && !isNaN(value)) fluxValue+= ` ${unit}`;
        }
        return {value: fluxValue, label: title};
    });

    fluxArray.length= 3;
    fill(fluxArray,{value: '', label: ''}, fluxObj.length);
    return fluxArray;

}



function makeCoordReturn(wp, toCsys, hms= false) {
    if (!wp) return '';
    const p= VisUtil.convert(wp, toCsys);
    if (hms) {
        const hmsLon = CoordUtil.convertLonToString(wp.getLon(), toCsys);
        const hmsLat = CoordUtil.convertLatToString(wp.getLat(), toCsys);
        return ` ${hmsLon}, ${hmsLat}`;
    }
    else {
        return` ${numeral(p.getLon()).format(precision7Digit)}, ${numeral(p.getLat()).format(precision7Digit)}`;
    }

}

function makeImagePtReturn(imagePt) {
    if (!imagePt) return '';
    return ` ${numeral(imagePt.x).format(precision1Digit)}, ${numeral(imagePt.y).format(precision1Digit)}`;
}

function makePixelReturn(pixel) {
    if (!pixel) return '';
    const {value,unit,precision} =pixel;
    return `${myFormat(value, precision)}  ${unit || ''}`;
}

