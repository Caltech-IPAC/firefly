/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {fill, isString} from 'lodash';
import {sprintf} from '../../externalSource/sprintf';
import VisUtil from '../VisUtil.js';
import CoordUtil from '../CoordUtil.js';
import CoordinateSys from '../CoordSys.js';
import {showMouseReadoutOptionDialog} from './MouseReadoutOptionPopups.jsx';
import {getFormattedWaveLengthUnits} from '../PlotViewUtil';
import {showInfoPopup} from '../../ui/PopupUtil';
import {findCoordSys} from '../CoordSys';


const myFormat= (v,precision) => !isNaN(v) ? sprintf(`%.${precision}f`,v) : '';


const labelMap = {
    eqj2000hms: 'EQ-J2000:',
    eqj2000DCM: 'EQ-J2000:',
    eclJ2000: 'ECL-J2000',
    galactic: 'Gal:',
    eqb1950: 'Eq-B1950:',
    fitsIP: 'Image Pixel:',
    zeroIP: '0 Based Pix:',
    pixelSize: 'Pixel Size:',
    sPixelSize: 'Screen Pixel Size:',
    healpixPixel: 'Pixel: ',
    healpixNorder: 'Norder: ',
    wl: 'Wavelength: ',
};


export function getNonFluxDisplayElements(readoutItems, readoutPref, isHiPS= false) {
    const objList= getNonFluxReadoutElements(readoutItems,  readoutPref, isHiPS);

    const {imageMouseReadout1, imageMouseReadout2, hipsMouseReadout1,
                     hipsMouseReadout2, pixelSize, healpixPixel, healpixNorder, wl} = objList;


    let readout1, readout2, healpixPixelReadout, healpixNorderReadout, waveLength;
    let showReadout1PrefChange, showReadout2PrefChange, showWavelengthFailed;

    if (isHiPS) {
        readout1= {...hipsMouseReadout1, label: labelMap[readoutPref.hipsMouseReadout1]};
        readout2= {...hipsMouseReadout2, label: labelMap[readoutPref.hipsMouseReadout2]};
        showReadout1PrefChange= () => showMouseReadoutOptionDialog('hipsMouseReadout1', readoutPref.hipsMouseReadout1);
        showReadout2PrefChange= () => showMouseReadoutOptionDialog('hipsMouseReadout2', readoutPref.hipsMouseReadout2);
        healpixPixelReadout= {...healpixPixel, label: labelMap.healpixPixel};
        healpixNorderReadout= {...healpixNorder, label: labelMap.healpixNorder};
    }
    else {
        readout1= {...imageMouseReadout1, label: labelMap[readoutPref.imageMouseReadout1]};
        readout2= {...imageMouseReadout2, label: labelMap[readoutPref.imageMouseReadout2]};
        if (wl?.value) {
            waveLength= {...wl, label:labelMap.wl};
            showWavelengthFailed= readoutItems.wl.failReason ? () => showInfoPopup(readoutItems.wl.failReason) : undefined;
        }
        showReadout1PrefChange= () => showMouseReadoutOptionDialog('imageMouseReadout1', readoutPref.imageMouseReadout1);
        showReadout2PrefChange= () => showMouseReadoutOptionDialog('imageMouseReadout2', readoutPref.imageMouseReadout2);
    }

    return {
        readout1, readout2, waveLength, showWavelengthFailed,
        showReadout1PrefChange, showReadout2PrefChange, healpixPixelReadout, healpixNorderReadout,
        pixelSize: {...pixelSize, label: labelMap[readoutPref.pixelSize]},
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

    if (!readoutItems) return {value:''};

    const wp= readoutItems?.worldPt?.value;
    switch (readoutKey) {
        case 'pixelSize':
            return {value:makePixelReturn(readoutItems.pixel)};
        case 'sPixelSize':
            return {value:makePixelReturn(readoutItems.screenPixel)};
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
        case 'eclJ2000' :
            return makeCoordReturn(wp, CoordinateSys.ECL_J2000, false);
        case 'fitsIP' :
            return {value:makeImagePtReturn(readoutItems?.fitsImagePt?.value)};
        case 'zeroIP' :
            return {value:makeImagePtReturn(readoutItems?.zeroBasedImagePt?.value)};
        case 'healpixPixel' :
            const {healpixPixel}= readoutItems;
            return {value: (healpixPixel && healpixPixel.value) ? `${healpixPixel.value}` : ''};
        case 'healpixNorder' :
            const {healpixNorder}= readoutItems;
            return {value: (healpixNorder && healpixNorder.value) ? `${healpixNorder.value}` : ''};
        case 'wl' :
            const {wl}= readoutItems;
            if (!wl) return {value:undefined};
            return {value:makeWLReturn(wl.value, getFormattedWaveLengthUnits(wl.unit))};
    }

    return {value:''};
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
        let fluxValue= value;
        if (!isNaN(value )) {
            if (precision) {
                const min = Number('0.'+'0'.repeat(precision-1)+'1');
                fluxValue = (Math.abs(value) < 1000  &&Math.abs(value)>min ) ?
                    `${myFormat(value, precision)}` :
                    value.toExponential(6).replace('e+', 'E');
            }
            else {
                fluxValue= `${Math.trunc(value)}`;
            }
            if (unit && !isNaN(value)) fluxValue+= ` ${unit}`;
        }

        return {value: fluxValue, label: title};
    });

    fluxArray.length= 3;
    fill(fluxArray,{value: '', label: ''}, fluxObj.length);
    return fluxArray;

}



function makeCoordReturn(wp, toCsys, hms= false) {
    if (!wp) return {value:''};
    const p= VisUtil.convert(wp, toCsys);
    let str;
    if (hms) {
        const hmsLon = CoordUtil.convertLonToString(p.getLon(), toCsys);
        const hmsLat = CoordUtil.convertLatToString(p.getLat(), toCsys);
        str= ` ${hmsLon}, ${hmsLat}`;
    }
    else {
        str=  sprintf('%.7f, %.7f',p.getLon(), p.getLat());
    }
    return {value:str, copyValue:`${str} ${toCsys.toString()}`};

}

function makeImagePtReturn(imagePt) {
    if (!imagePt) return '';
    return sprintf('%.1f, %.1f',imagePt.x, imagePt.y);
}

function makeWLReturn(value,unit) {
    if (isString(value)) return 'Failed';
    if (isNaN(value)) return 'NaN';
    if (value===0) return `0 ${unit}`;
    if (value < .0001) return `${value.toExponential(6).replace('e+', 'E')} ${unit}`;
    else               return `${sprintf('%.4f',value)} ${unit}`;
}

function makePixelReturn(pixel) {
    if (!pixel) return '';
    const {value,unit,precision} =pixel;
    return `${myFormat(value, precision)}  ${unit || ''}`;
}

