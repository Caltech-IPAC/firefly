/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {fill, isString} from 'lodash';
import {sprintf} from '../../externalSource/sprintf';
import {
    MR_ECL1950, MR_ECLJ2000, MR_EQB1950, MR_EQB1950_DCM, MR_EQJ2000_DCM,
    MR_EQJ2000_HMS, MR_FIELD_HIPS_MOUSE_READOUT1, MR_FIELD_HIPS_MOUSE_READOUT2, MR_FIELD_IMAGE_MOUSE_READOUT1,
    MR_FIELD_IMAGE_MOUSE_READOUT2, MR_FITS_IP, MR_WL, MR_ZERO_IP,
    MR_GALACTIC, MR_HEALPIX_NORDER, MR_HEALPIX_PIXEL, MR_PIXEL_SIZE, MR_SPIXEL_SIZE, MR_SUPER_GALACTIC, MR_WCS_COORDS,
    STATUS_NAN, STATUS_UNAVAILABLE, STATUS_UNDEFINED, STATUS_VALUE, TYPE_DECIMAL_INT, TYPE_EMPTY, TYPE_FLOAT,
    MR_BAND_WIDTH, EQ_TYPE
} from '../MouseReadoutCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {convertCelestial} from '../VisUtil';
import {isCelestialImage} from '../WebPlot.js';
import CoordUtil from '../CoordUtil.js';
import CoordinateSys from '../CoordSys.js';
import {showMouseReadoutOptionDialog} from './MouseReadoutOptionPopups.jsx';
import {getFormattedWaveLengthUnits, primePlot} from '../PlotViewUtil';
import {showInfoPopup} from '../../ui/PopupUtil';
import {getBixPix, getBScale, getBZero} from 'firefly/visualize/FitsHeaderUtil';


const myFormat= (v,precision) => !isNaN(v) ? sprintf(`%.${precision}f`,v) : '';


const labelMap = {
    [MR_EQJ2000_HMS]: 'EQ-J2000:',
    [MR_EQJ2000_DCM]: 'EQ-J2000:',
    [MR_ECLJ2000]: 'ECL-J2000:',
    [MR_ECL1950]: 'ECL-B1950:',
    [MR_GALACTIC]: 'Gal:',
    [MR_SUPER_GALACTIC]: 'SGal:',
    [MR_EQB1950]: 'Eq-B1950:',
    [MR_EQB1950_DCM]: 'Eq-B1950:',
    [MR_WCS_COORDS]: 'WCS-Coords:',
    [MR_FITS_IP]: 'Image Pixel:',
    [MR_ZERO_IP]: '0 Based Pix:',
    [MR_PIXEL_SIZE]: 'Pixel Size:',
    [MR_SPIXEL_SIZE]: 'Screen Pixel Size:',
    [MR_HEALPIX_PIXEL]: 'Pixel: ',
    [MR_HEALPIX_NORDER]: 'Norder: ',
    [MR_WL]: 'Wavelength: ',
    [MR_BAND_WIDTH]: 'Band Width: ',
};

const coordOpTitle= 'Choose readout coordinates';

export const getEqTypeFromMR = (readoutKey) => readoutKey.toUpperCase().startsWith('EQ')
    ? (readoutKey.toUpperCase().endsWith('DCM') ? EQ_TYPE.DCM : EQ_TYPE.HMS)
    : undefined; // undefined type if not Equatorial readout

export const getFluxRadix = (readoutPref, primePlot) => {
    // From FITS 4.00 (https://fits.gsfc.nasa.gov/standard40/fits_standard40aa-le.pdf), p. 14-15, Table 11 and section 4.4.2.5
    const isFluxInt = getBixPix(primePlot)>0 && getBScale(primePlot)===1 && (getBZero(primePlot)===0
            // OR if bZero was used as follows for representation of unsigned-integer data
            || (getBixPix(primePlot)===8 && getBZero(primePlot)===-128)
            || (getBixPix(primePlot)===16 && getBZero(primePlot)===32768)
            || (getBixPix(primePlot)===32 && getBZero(primePlot)===2147483648)
        );
    return Number(isFluxInt ? readoutPref.intFluxValueRadix : readoutPref.floatFluxValueRadix);
};

export function getNonFluxDisplayElements(readoutData, readoutPref, isHiPS= false) {
    const objList= getNonFluxReadoutElements(readoutData,  readoutPref, isHiPS);

    const {imageMouseReadout1, imageMouseReadout2, imageMouseNoncelestialReadout1, imageMouseNoncelestialReadout2,
        hipsMouseReadout1, hipsMouseReadout2, pixelSize, healpixPixel, healpixNorder, wl, bandWidth} = objList;


    let readout1, readout2, healpixPixelReadout, healpixNorderReadout, waveLength, bandWidthField= undefined;
    let showReadout1PrefChange, showReadout2PrefChange, showWavelengthFailed;

    if (isHiPS) {
        readout1= {...hipsMouseReadout1, label: labelMap[readoutPref.hipsMouseReadout1]};
        readout2= {...hipsMouseReadout2, label: labelMap[readoutPref.hipsMouseReadout2]};
        showReadout1PrefChange= () => showMouseReadoutOptionDialog(MR_FIELD_HIPS_MOUSE_READOUT1, readoutPref[MR_FIELD_HIPS_MOUSE_READOUT1], readoutPref.mouseReadoutValueCopy, coordOpTitle);
        showReadout2PrefChange= () => showMouseReadoutOptionDialog(MR_FIELD_HIPS_MOUSE_READOUT2, readoutPref[MR_FIELD_HIPS_MOUSE_READOUT2], readoutPref.mouseReadoutValueCopy, coordOpTitle);
        healpixPixelReadout= {...healpixPixel, label: labelMap.healpixPixel};
        healpixNorderReadout= {...healpixNorder, label: labelMap.healpixNorder};
    }
    else {
        const readoutItems = readoutData.readoutItems;

        // outside image use active plot (plotId=undefined)
        const csys = readoutItems.worldPt?.value?.cSys;
        const plotId = csys ? readoutData.plotId : undefined;
        const isCelestial = isCelestialImage(primePlot(visRoot(), plotId));

        if (isCelestial) {
            readout1 = {...imageMouseReadout1, label: labelMap[readoutPref.imageMouseReadout1]};
            readout2= {...imageMouseReadout2, label: labelMap[readoutPref.imageMouseReadout2]};
            showReadout1PrefChange= () => showMouseReadoutOptionDialog(MR_FIELD_IMAGE_MOUSE_READOUT1, readoutPref[MR_FIELD_IMAGE_MOUSE_READOUT1], readoutPref.mouseReadoutValueCopy, coordOpTitle);
            showReadout2PrefChange= () => showMouseReadoutOptionDialog(MR_FIELD_IMAGE_MOUSE_READOUT2, readoutPref[MR_FIELD_IMAGE_MOUSE_READOUT2], readoutPref.mouseReadoutValueCopy, coordOpTitle);
        } else {
            const wcsCoordLabel = createWCSCoordsLabel(plotId);
            const wcsCoordOptionTitle = wcsCoordLabel && wcsCoordLabel.substring(0, wcsCoordLabel.length - 1);
            let label1 = undefined;
            if (readoutPref.imageMouseNoncelestialReadout1 === 'wcsCoords') {
                label1 = wcsCoordLabel;
            }
            readout1 = {...imageMouseNoncelestialReadout1, label: label1 || labelMap[readoutPref.imageMouseNoncelestialReadout1]};

            let label2 = undefined;
            if (readoutPref.imageMouseNoncelestialReadout2 === 'wcsCoords') {
                label2 = wcsCoordLabel;
            }
            readout2= {...imageMouseNoncelestialReadout2, label: label2 || labelMap[readoutPref.imageMouseNoncelestialReadout2]};
            showReadout1PrefChange= () => showMouseReadoutOptionDialog('imageMouseNoncelestialReadout1', readoutPref.imageMouseNoncelestialReadout1, undefined, coordOpTitle, wcsCoordOptionTitle);
            showReadout2PrefChange= () => showMouseReadoutOptionDialog('imageMouseNoncelestialReadout2', readoutPref.imageMouseNoncelestialReadout2, undefined, coordOpTitle, wcsCoordOptionTitle);
        }


        if (wl?.value) {
            waveLength= {...wl, label:labelMap[MR_WL]};
            showWavelengthFailed= readoutItems.wl.failReason ? () => showInfoPopup(readoutItems.wl.failReason) : undefined;
        }
        if (bandWidth?.value) {
            bandWidthField= {...bandWidth, label:labelMap[MR_BAND_WIDTH]};
        }
    }

    return {
        readout1, readout2, waveLength, bandWidth:bandWidthField, showWavelengthFailed,
        showReadout1PrefChange, showReadout2PrefChange, healpixPixelReadout, healpixNorderReadout,
        pixelSize: {...pixelSize, label: labelMap[readoutPref.pixelSize]},
        showPixelPrefChange:() => showMouseReadoutOptionDialog('pixelSize', readoutPref.pixelSize, undefined, 'Choose pixel size'),
    };
}


export function getNonFluxReadoutElements(readoutData, readoutPref, isHiPS= false) {
    const readoutItems = readoutData.readoutItems;
    const plotId = readoutData.plotId;

    const copyPref = readoutPref?.mouseReadoutValueCopy || 'str';

    const keysToUse= Object.keys(readoutPref).filter( (key) =>
        isHiPS ?
            key.startsWith('hips') || !key.startsWith('image') :
            !key.startsWith('hips') || key.startsWith('image'));

    const retList={};
    keysToUse.forEach( (key) =>  {
        retList[key]=getReadoutElement(readoutItems, readoutPref[key], plotId, copyPref);
    });

    return retList;
}



/**
 * Get the mouse readouts from the standard readout and convert to the values based on the toCoordinaeName
 * @param readoutItems
 * @param readoutKey Readout preference value
 * @param plotId
 * @param copyPref Readout Copy preference value
 * @returns {*}
 */
export function getReadoutElement(readoutItems, readoutKey, plotId, copyPref) {
    if (!readoutItems) return {value:''};

    const wp= readoutItems?.worldPt?.value;
    switch (readoutKey) {
        case MR_PIXEL_SIZE:
            return {value:makePixelReturn(readoutItems.pixel)};
        case MR_SPIXEL_SIZE:
            return {value:makePixelReturn(readoutItems.screenPixel)};
        case MR_EQJ2000_HMS:
            return makeCoordReturn(wp, CoordinateSys.EQ_J2000, copyPref, true);
        case MR_EQJ2000_DCM:
            return makeCoordReturn(wp, CoordinateSys.EQ_J2000, copyPref);
        case MR_GALACTIC:
            return makeCoordReturn(wp, CoordinateSys.GALACTIC, copyPref);
        case MR_SUPER_GALACTIC :
            return makeCoordReturn(wp, CoordinateSys.SUPERGALACTIC, copyPref);
        case MR_EQB1950:
            return makeCoordReturn(wp, CoordinateSys.EQ_B1950, copyPref, true);
        case MR_EQB1950_DCM:
            return makeCoordReturn(wp, CoordinateSys.EQ_B1950, copyPref);
        case MR_ECLJ2000:
            return makeCoordReturn(wp, CoordinateSys.ECL_J2000, copyPref, false);
        case MR_ECL1950:
            return makeCoordReturn(wp, CoordinateSys.ECL_B1950, copyPref, false);
        case MR_WCS_COORDS:
            const plot = primePlot(visRoot(), plotId);
            const unit = plot?.projection?.header?.cunit1 || '';
            return {value:makeNoncelestialCoordReturn(wp, unit)};
        case MR_FITS_IP:
            return {value:makeImagePtReturn(readoutItems?.fitsImagePt?.value)};
        case MR_ZERO_IP:
            return {value:makeImagePtReturn(readoutItems?.zeroBasedImagePt?.value)};
        case MR_HEALPIX_PIXEL:
            const {healpixPixel}= readoutItems;
            return {value: (healpixPixel && healpixPixel.value) ? `${healpixPixel.value.toString(16)}` : ''};
        case MR_HEALPIX_NORDER:
            const {healpixNorder}= readoutItems;
            return {value: (healpixNorder && healpixNorder.value) ? `${healpixNorder.value}` : ''};
        case MR_WL:
            const {wl}= readoutItems;
            if (!wl) return {value:undefined};
            return {value:makeWLReturn(wl.value, getFormattedWaveLengthUnits(wl.unit))};
        case MR_BAND_WIDTH:
            const {bandWidth}= readoutItems;
            if (!bandWidth) return {value:undefined};
            return {value:makeWLReturn(bandWidth.value, getFormattedWaveLengthUnits(bandWidth.unit))};
    }

    return {value:''};
}

/**
 * Label for non-celestial coordinates readout item.
 * @param plotId
 */
function createWCSCoordsLabel(plotId) {
    const plot = primePlot(visRoot(), plotId);
    const header = plot?.projection?.header;
    if (!header) return undefined;

    const {ctype1, ctype2} = header;

    if (ctype1 && ctype2) {
        // CTYPE=LINEAR is a special case
        if (ctype1 !== 'LINEAR') return ctype1?.split('-')?.[0] + ', ' + ctype2?.split('-')?.[0] + ':';
    }
    return undefined;
}

function getFluxValueByType(readoutType,radix,valueBase10,valueBase16,unit,label,precision) {
    const is16= radix===16;
    switch (readoutType) {
        case TYPE_FLOAT:
            if (is16) return {value:valueBase16, label, unit};
            const v= Number(valueBase10);
            if (isNaN(v)) return {value: 'NaN', label};
            const min = Number('0.'+'0'.repeat(precision-1)+'1');
            const fluxValue = (Math.abs(v) < 1000  &&Math.abs(v)>min ) ?
                `${myFormat(v, precision)}` : v.toExponential(6).replace('e+', 'E');
            return {value: fluxValue, label, unit};
        case TYPE_DECIMAL_INT:
            return (is16) ? {value:valueBase16, label, unit}:  {value:valueBase10, label, unit};
        case TYPE_EMPTY:
        default:
            return {value:'', label, unit: unit ?? ''};
    }
}

function makeFluxEntry({valueBase10, valueBase16, readoutType,status,unit='',title:label,precision=6},radix=10) {
    const is16= radix===16;
    switch (status) {
        case STATUS_UNAVAILABLE: return {value: 'unavailable', label, unit:''};
        case STATUS_NAN: return is16 ? {value: `${valueBase16} (NaN)`, label, unit:''}  : {value: 'NaN', label, unit:''};
        case STATUS_UNDEFINED: return is16 ? {value: `${valueBase16} (undefined)`, label, unit:''} : {value: 'undefined', label, unit:''};
        case STATUS_VALUE:
            return getFluxValueByType(readoutType,radix,valueBase10,valueBase16,unit, label, precision);
        default:
            return {value: 'unavailable', label, unit:''};
    }
}

/**
 * This method passes the standard readout and then get the flux information
 * @param sndReadout
 * @param {number} radix
 * @returns {{fluxLabels: Array, fluxValues: Array}}
 */
export function getFluxInfo(sndReadout, radix=10){

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

    const fluxArray= fluxObj.map((obj) => makeFluxEntry(obj,radix));
    fluxArray.length= 3;
    fill(fluxArray,{value: '', label: '', unit:''}, fluxObj.length);
    return fluxArray;

}



function makeCoordReturn(wp, toCsys, copyPref, hms= false) {
    if (!wp) return {value:''};
    const p= convertCelestial(wp, toCsys);
    let str;
    if (hms) {
        const hmsLon = CoordUtil.convertLonToString(p.getLon(), toCsys);
        const hmsLat = CoordUtil.convertLatToString(p.getLat(), toCsys);
        str= `${hmsLon}, ${hmsLat}`;
    }
    else {
        str=  sprintf('%.7f, %.7f',p.getLon(), p.getLat());
    }

    const copyValue = copyPref==='skyCoord' ? getPythonSkyCoord(wp) : `${str} ${toCsys.toString()}`;
    return {value:str, copyValue};
}

function getPythonSkyCoord(wp) {
    const csys = CoordinateSys.EQ_J2000;
    const p = convertCelestial(wp, csys);
    const hmsLon = CoordUtil.convertLonToString(p.getLon(), csys);
    const hmsLat = CoordUtil.convertLatToString(p.getLat(), csys);
    return `SkyCoord('${hmsLon} ${hmsLat}', frame='icrs')`; //ICRS frame is approximately correct for EQ J2000
}

function makeNoncelestialCoordReturn(wp, unit = '') {
    if (!wp) return '';
    return sprintf('%.7f, %.7f %s', wp.getLon(), wp.getLat(), unit);
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

