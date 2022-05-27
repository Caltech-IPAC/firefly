/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



import {
    STRETCH_ASINH, STRETCH_EQUAL, STRETCH_LINEAR, STRETCH_LOG,
    STRETCH_LOGLOG, STRETCH_POWERLAW_GAMMA, STRETCH_SQRT, STRETCH_SQUARED, ZSCALE
} from '../../../RangeValues.js';
import {getDNfromBin, getTblArray} from './Histogram.js';
import {ImageMask} from './ImageMask.js';
import {getScaled, getShigh, getSlow} from './StretchUtil.js';
import {computeIntensity} from './RGBIntensity.js';

/**
 * @author Trey Roby
 */

const isHuePreserving= (rv) => rv.rgbPreserveHue;
const asinh= (x) => Math.log(x + Math.sqrt(x * x + 1));


/**
 *
 * @param rangeValues
 * @param {Float32Array} float1d
 * @param {Uint8Array} pixelData
 * @param processHeader
 * @param {HistogramData} hist
 * @param startPixel
 * @param lastPixel
 * @param startLine
 * @param lastLine
 */
export function stretchPixels8Bit(rangeValues, float1d, pixelData, processHeader, hist, startPixel,
                           lastPixel, startLine, lastLine ) {
    const slow = getSlow(rangeValues, float1d, processHeader, hist);
    const shigh = getShigh(rangeValues, float1d, processHeader, hist);
    stretchPixelsByBand(startPixel, lastPixel, startLine, lastLine, processHeader.naxis1, hist,
        255, float1d, pixelData, rangeValues,slow,shigh);
}

export function stretchPixels3Color(rangeValuesAry, float1dAry, pixelDataAry, imageHeaderAry,
                             histAry, rgbIntensity, startPixel, lastPixel,
                             startLine, lastLine) {

    if (float1dAry.length!==3 || imageHeaderAry.length!==3 || histAry.length!==3) {
        throw new Error('float1dAry, imageHeaderAry, histAry must be exactly 3 elements, some can be null ');
    }

    if (rangeValuesAry[0] && isHuePreserving(rangeValuesAry[0])) {
        stretchPixelsHuePreserving(startPixel, lastPixel, startLine, lastLine, imageHeaderAry, histAry,
            rgbIntensity, float1dAry, pixelDataAry, rangeValuesAry);
    }
    else {
        const fallbackRV=rangeValuesAry.find( (rv) => rv);
        for(let i=0; (i<float1dAry.length); i++) {
            if (float1dAry[i]) {
                const rv= rangeValuesAry[i] || fallbackRV;
                const slow = getSlow(rv, float1dAry[i], imageHeaderAry[i], histAry[i]);
                const shigh = getShigh(rv, float1dAry[i], imageHeaderAry[i], histAry[i]);
                stretchPixelsByBand(startPixel, lastPixel, startLine, lastLine,imageHeaderAry[i].naxis1, histAry[i],
                    rgbIntensity, float1dAry[i], pixelDataAry[i], rv,slow,shigh);
            }
            else {
                pixelDataAry[i] && pixelDataAry[i].fill(0);
            }
        }
    }

}

function stretchPixelsHuePreserving(startPixel, lastPixel, startLine, lastLine, imageHeaderAry, histAry,
                                    rgbIntensity, float1dAry, pixelDataAry, rangeValuesAry) {

    for (let i = 0; (i < float1dAry.length); i++) {
        if (!float1dAry[i] || !imageHeaderAry[i]|| !histAry[i]) {
            throw new Error('3 bands are required for hue preserving stretch.');
        }
    }
    if (imageHeaderAry[0].naxis1!==imageHeaderAry[1].naxis1 || imageHeaderAry[1].naxis1!==imageHeaderAry[2].naxis1 ||
        imageHeaderAry[0].naxis2!==imageHeaderAry[1].naxis2 || imageHeaderAry[1].naxis2!==imageHeaderAry[2].naxis2) {
        throw new Error('naxis1 and naxis2 must match. ' +
            'r: (' + imageHeaderAry[0].naxis1 + ',' + imageHeaderAry[0].naxis2 + ') ' +
            'g: (' + imageHeaderAry[1].naxis1 + ',' + imageHeaderAry[1].naxis2 + ') ' +
            'b: (' + imageHeaderAry[2].naxis1 + ',' + imageHeaderAry[2].naxis2 + ')');
    }

    const blankPxValAry = new Array(3);
    const slowAry = new Array(3);
    for(let i=0; i<3; i++) {
        blankPxValAry[i]= imageHeaderAry[i].blank_value;
        slowAry[i] = getSlow(rangeValuesAry[i], float1dAry[i], imageHeaderAry[i], histAry[i]);
        slowAry[i] = getScaled(slowAry[i], imageHeaderAry[i], rangeValuesAry[i]);
    }

    // recreate an array of intensities (the part that will be used)
    const naxis1 = imageHeaderAry[0].naxis1;
    const intensity = new Array(float1dAry[0].length);
    intensity.fill(NaN);
    let pixelCount = 0;
    for (let line = startLine; line <= lastLine; line++) {
        const start_index = line * naxis1 + startPixel;
        const last_index = line * naxis1 + lastPixel;

        for (let index = start_index; index <= last_index; index++) {

            if (float1dAry[0][index] !==blankPxValAry[0] && float1dAry[1][index] !==blankPxValAry[1] && float1dAry[2][index] !==blankPxValAry[1]) {
                intensity[index] = computeIntensity(index, float1dAry, imageHeaderAry, slowAry, rangeValuesAry);
            }
            pixelCount++;
        }
    }

    const rv = rangeValuesAry[0];

    // stretch an array of intensities
    const pixelData = new Array(pixelCount);

    // should we use z-scale to calculate intensity slow and shigh values
    const useZ = rangeValuesAry[0].lowerWhich===ZSCALE || !isFinite(rangeValuesAry[0].asinhStretch);
    const slow = useZ ? rgbIntensity.getIntensityLow() : rgbIntensity.getIntensityDataLow(); // lower range for intensity
    let stretch = useZ ? rgbIntensity.getIntensityHigh()-rgbIntensity.getIntensityLow() : rv.asinhStretch;

    if (!useZ) {
        const intensityRange = rgbIntensity.getIntensityDataHigh()-slow;
        if (stretch > intensityRange && intensityRange > 0) {
            stretch = intensityRange;
        } else if (stretch < 1e-10) {
            stretch = 1e-10;
        }
    }

    const shigh = slow + stretch; // upper range for intensity

    // for three color we use 0 as blank pixel value
    stretchPixelsUsingAsinh( startPixel, lastPixel,startLine,lastLine, naxis1,
        rgbIntensity.getIntensityDataLow(), rgbIntensity.getIntensityDataHigh(),
        0, intensity, pixelData, rv, slow, shigh);
    rangeValuesAry.forEach( (anRV) => {
        anRV.asinhQValue= rv.asinhQValue;
        anRV.asinhStretch= stretch;
    });

    // fill pixel data for each band
    pixelCount = 0;
    const rgb = new Array(3);
    const pixmax = 255;
    let maxv; // max value
    let flux;
    for (let line = startLine; line <= lastLine; line++) {
        const start_index = line * naxis1 + startPixel;
        const last_index = line * naxis1 + lastPixel;

        for (let index = start_index; index <= last_index; index++) {
            maxv = 0;
            for (let c=0; c<3; c++) {
                flux = getScaled(float1dAry[c][index], imageHeaderAry[c], rangeValuesAry[c])-slowAry[c];
                if (flux < 0 || isNaN(flux)) {
                    rgb[c] = 0;
                }
                else {
                    rgb[c] = (0xFF&pixelData[pixelCount])*flux/intensity[index];
                    if (rgb[c]>maxv) {
                        maxv = rgb[c];
                    }
                }
            }
            if (maxv > pixmax) {
                rgb[0] = pixmax * rgb[0] / maxv;
                rgb[1] = pixmax * rgb[1] / maxv;
                rgb[2] = pixmax * rgb[2] / maxv;
            }
            pixelDataAry[0][pixelCount] = rgb[0];
            pixelDataAry[1][pixelCount] = rgb[1];
            pixelDataAry[2][pixelCount] = rgb[2];
            pixelCount++;
        }
    }
}

/**
 * A pixel is a cell or small rectangle which stores the information the computer can handle. A discrete pixels make the map.
 * Each pixel store a value which represents the color of the map.
 * Byte image is the pixel having a value in the range of [0, 255].  One byte has 8 bit.  The stretch algorithm is able to convert
 * some invisible pixel value to become recognizable.
 * There are several stretch algorithm: liner, log, log-log etc.  Using those technique to calculate new pixel values.  For example:
 * Suppose you have a certain image in which the values range from 55 to 103. When this map is stretched linearly to output range 0 to
 * 255: the minimum input value 55 is brought to output value 0, and maximum input value 103 is brought to output value 255, and all
 * other values in between change accordingly (using the same formula). As 0 is by default displayed in black, and 255 in white, the
 * contrast will be better when the image is displayed.
 *
 * @param {Number} startPixel (tile info) start pixel in each line
 * @param {Number} lastPixel (tile info) end pixel in each line
 * @param {Number} startLine (tile info) start line
 * @param {Number} lastLine (tile info) end line
 * @param {Number} naxis1
 * @param {HistogramData} hist
 * @param {Number} blank_pixel_value blank pixel value
 * @param {Float32Array} float1dArray
 * @param {Uint8Array} pixeldata
 * @param {RangeValues} rangeValues
 * @param {Number} slow
 * @param {Number} shigh
 */
function stretchPixelsByBand(startPixel, lastPixel, startLine, lastLine, naxis1, hist, blank_pixel_value,
                             float1dArray, pixeldata, rangeValues, slow, shigh) {

    /*
     * This loop will go through all the pixels and assign them new values based on the
     * stretch algorithm
     */
    if (rangeValues.algorithm===STRETCH_ASINH) {
        stretchPixelsUsingAsinh( startPixel, lastPixel,startLine,lastLine, naxis1, hist.dnMin, hist.dnMax,
            blank_pixel_value, float1dArray, pixeldata, rangeValues,slow,shigh);


    }
    else {
        stretchPixelsUsingOtherAlgorithms(startPixel, lastPixel, startLine, lastLine,naxis1, hist,
            blank_pixel_value,float1dArray, pixeldata, rangeValues, slow, shigh);
    }
}



/**
 * Displayed flux value
 * @param raw_dn raw value
 * @param imageHeader image header
 * @return flux value to display
 */
export function getFlux(raw_dn, imageHeader){
    if ((raw_dn===imageHeader.blank_value) || (isNaN(raw_dn))) {
        return NaN;
    }
    if (imageHeader.origin.startsWith('Palomar Transient Factory')) {
        return  -2.5 * .43429 * Math.log(raw_dn / imageHeader.exptime) +
            imageHeader.imagezpt +
            imageHeader.extinct * imageHeader.airmass;
        /* .43429 changes from natural log to common log */
    } else {
        return raw_dn * imageHeader.bscale + imageHeader.bzero;
    }
}


/**
 *
 * @param startPixel
 * @param lastPixel
 * @param startLine
 * @param lastLine
 * @param naxis1
 * @param hist
 * @param blank_pixel_value
 * @param {Float32Array} float1dArray
 * @param {Uint8Array} pixeldata
 * @param {RangeValues} rangeValues
 * @param slow
 * @param shigh
 */
function stretchPixelsUsingOtherAlgorithms(startPixel, lastPixel, startLine, lastLine, naxis1, hist,
                                           blank_pixel_value, float1dArray, pixeldata, rangeValues,
                                           slow, shigh){

    const sdiff = slow===shigh ? 1.0 : shigh - slow;

    let dtbl = new Array(256);
    if (rangeValues.algorithm===STRETCH_LOG || rangeValues.algorithm===STRETCH_LOGLOG) {
        dtbl = getLogDtbl(sdiff, slow, rangeValues);
    }
    else if (rangeValues.algorithm===STRETCH_EQUAL) {
        dtbl = getTblArray(hist);
    }
    else if (rangeValues.algorithm===STRETCH_SQUARED){
        dtbl = getSquaredDbl(sdiff, slow, rangeValues);
    }
    else if( rangeValues.algorithm===STRETCH_SQRT) {
        dtbl = getSquaredDbl(sdiff, slow, rangeValues);
    }
    const deltasav = sdiff > 0 ? 64 : -64;

    const gamma=rangeValues.gammaValue;
    let pixelCount = 0;
    const {bias,contrast}= rangeValues;




    //-----
    let stretchThePixel;
    if (rangeValues.algorithm===STRETCH_LINEAR) {
        stretchThePixel= (floatPixel) => getLinearStretchedPixelValue( (floatPixel - slow) * 254 / sdiff);
    } else if (rangeValues.algorithm===STRETCH_POWERLAW_GAMMA) {
        stretchThePixel= (floatPixel) => getPowerLawGammaStretchedPixelValue(floatPixel, gamma, slow, shigh);
    } else {
        stretchThePixel= (floatPixel) => getNoneLinerStretchedPixelValue(floatPixel, dtbl, deltasav);
    }
    //-----

    for (let line = startLine; line <= lastLine; line++) {
        const start_index = line * naxis1 + startPixel;
        const last_index = line * naxis1 + lastPixel;

        for (let index = start_index; index <= last_index; index++) {
            if (isNaN(float1dArray[index])) { //original pixel value is NaN, assign it to blank
                pixeldata[pixelCount] = blank_pixel_value;
            } else {   // stretch each pixel
                pixeldata[pixelCount] = computeBiasAndContrast(stretchThePixel(float1dArray[index]), bias, contrast);
            }
            pixelCount++;
        }
    }
}

/**
 * The algorithm accepts positive Q, which should be controlled by a slider.
 * The mapping from flux to color value is 255 * 0.1 * asinh(Q*(x-xMin)/(xMax-xMin)) / asinh(0.1*Q)
 * Below xMin, the color will be 0; above xMax, the equation has to be applied and then clipped to 244, (255 is reserved)
 *
 * The parametrization using Q is explained in the footnote on page 3 of https://arxiv.org/pdf/astro-ph/0312483.pdf
 * The algorithm is based on asinh stretch algorithm used in
 *     https://github.com/astropy/astropy/blob/master/astropy/visualization/lupton_rgb.py
 *
 * Lupton’s formulation assumes that xMax is far below the bright features in the image.
 * He wants to see the features above xMax.
 *
 * If we know the brightest data value and upper and lower range values,
 * we can get the default Q from the following equation:
 *    0.1 * asinh(Q*(xDataMax-xMin)/(xMax-xMin)) / asinh(0.1*Q) = 1
 *
 * @param {Number} flux pixel value
 * @param {Number} maxFlux upper range value
 * @param {Number} minFlux lower range value
 * @param {Number} qvalue Q parameter for asinh stretch algorithm
 * @return mapped color value from 0 to 244
 */
function getASinhStretchedPixelValue(flux, maxFlux, minFlux, qvalue)  {
    if (flux <= minFlux)  return 0;
    const color = 255 * 0.1 * asinh(qvalue*(flux - minFlux) / (maxFlux - minFlux)) / asinh(0.1 * qvalue);
    return (color > 254) ? 254 : color;
}


/**
 *
 * @param dRunVal
 * @param dtbl
 * @param delta
 * @return {number} this should be a int value
 */
function getNoneLinerStretchedPixelValue(dRunVal, dtbl, delta) {

    let pixval = 128;

    if (dtbl[pixval] < dRunVal) pixval += delta;
    else pixval -= delta;

    delta >>= 1;
    if (dtbl[pixval] < dRunVal) pixval += delta;
    else pixval -= delta;

    delta >>= 1;
    if (dtbl[pixval] < dRunVal) pixval += delta;
    else pixval -= delta;

    delta >>= 1;
    if (dtbl[pixval] < dRunVal) pixval += delta;
    else pixval -= delta;

    delta >>= 1;
    if (dtbl[pixval] < dRunVal) pixval += delta;
    else pixval -= delta;

    delta >>= 1;
    if (dtbl[pixval] < dRunVal) pixval += delta;
    else pixval -= delta;

    delta >>= 1;
    if (dtbl[pixval] < dRunVal) pixval += delta;
    else pixval -= delta;

    delta >>= 1;
    if (dtbl[pixval] >= dRunVal) pixval -= 1;
    return pixval;
}

/**
 *
 * @param {number} dRenVal
 * @return this should be a byte value
 */
function getLinearStretchedPixelValue(dRenVal) {
    if (dRenVal < 0) return 0;
    else if (dRenVal > 254) return 254;
    else return  dRenVal;
}


/**
 *
 * @param sdiff
 * @param slow
 * @param {RangeValues} rangeValues
 * @return {Array.<Number>}
 */
function getLogDtbl(sdiff, slow, rangeValues) {

    const dtbl = new Array(256);
    for (let j = 0; j < 255; ++j) {
        let atbl = Math.pow(10., j / 254.0);
        if (rangeValues.algorithm===STRETCH_LOGLOG) atbl = Math.pow(10., (atbl - 1.0) / 9.0);
        dtbl[j] = (atbl - 1.) / 9. * sdiff + slow;
    }
    dtbl[255] = Number.MAX_VALUE;
    return dtbl;
}

/**
 *
 * @param startPixel
 * @param lastPixel
 * @param startLine
 * @param lastLine
 * @param naxis1
 * @param dnmin
 * @param dnmax
 * @param blank_pixel_value
 * @param float1dArray
 * @param pixeldata
 * @param {RangeValues} rangeValues
 * @param slow
 * @param shigh
 */
function stretchPixelsUsingAsinh(startPixel, lastPixel, startLine, lastLine, naxis1, dnmin, dnmax,
                                  blank_pixel_value, float1dArray, pixeldata, rangeValues, slow, shigh){


    let qvalue = rangeValues.asinhQValue;
    if (qvalue < 1e-10) qvalue = 0.1;
    else if (qvalue > 1e10) qvalue = 1e10;


    // using raw_dn for flux
    let maxFlux = shigh;
    let minFlux = slow;
    if (isNaN(minFlux) || !Number.isFinite((minFlux))){
        minFlux = dnmin;
    }

    if ( isNaN(maxFlux) || !Number.isFinite(maxFlux) ) {
        maxFlux = dnmax;
    }

    if ( !Number.isFinite(qvalue) ) {
        qvalue = getDefaultAsinhQ(minFlux, maxFlux, dnmax);
        rangeValues.asinhQValue=qvalue;
    }

    let pixelCount = 0;
    let flux;

    for (let line = startLine; line <= lastLine; line++) {
        const start_index = line * naxis1 + startPixel;
        const last_index = line * naxis1 + lastPixel;
        for (let index = start_index; index <= last_index; index++) {
            flux = float1dArray[index];
            if (isNaN(flux)) { // if original pixel value is NaN, assign it to blank
                pixeldata[pixelCount] = blank_pixel_value;
            } else {
                pixeldata[pixelCount] = getASinhStretchedPixelValue(flux, maxFlux, minFlux, qvalue);
            }
            pixelCount++;
        }
    }
}

/**
 * Find default Q from asinh(Q*(xDataMax-xMin)/(xMax-xMin)) = 10 * asinh(0.1*Q)
 * @param minFlux lower range value
 * @param maxFlux upper range value
 * @param dataMaxFlux maximum data value
 * @return Q value, which would allow to use full color range
 */
function getDefaultAsinhQ(minFlux, maxFlux, dataMaxFlux) {
    let bestQ = 0.1;
    const step = 0.1;
    let minDiff = Number.MAX_VALUE;
    const fact = (dataMaxFlux-minFlux)/(maxFlux-minFlux);
    let diff;
    // max default Q is 12 - which corresponds to fact=1000
    // (points 1000 times brighter than maxFlux will saturate)
    for (let q=0.1; q<=12; q=q+step) {
        diff = Math.abs(asinh(fact*q)-10*asinh(0.1*q));
        if ( diff < minDiff) {
            minDiff = diff;
            bestQ = q;
        }
    }
    return Math.round(10*bestQ)/10.0; // round to 1 decimal digit
}

/**
 * fill the 256 element table with values for a squared stretch
 *
 * @param sdiff
 * @param slow
 * @param {RangeValues} rangeValues
 * @return {*}
 */
function getSquaredDbl(sdiff, slow,rangeValues) {
    const dtbl = new Array(256);
    for (let j = 0; j < 255; ++j) {
        if (rangeValues.algorithm===STRETCH_SQUARED){
            dtbl[j] = Math.sqrt(sdiff * sdiff / 254 * j) + slow;
        }
        else {
            const dd = Math.sqrt(sdiff) / 254 * j;
            dtbl[j] = dd * dd + slow;
        }
    }
    dtbl[255]= Number.MAX_VALUE;
    return dtbl;
}

function getPowerLawGammaStretchedPixelValue(x, gamma, zp, mp){
    if (x <= zp) { return 0; }
    if (x >= mp) { return 254; }
    const rd =  x-zp;
    const nsd = Math.pow(rd, 1.0 / gamma)/ Math.pow(mp - zp, 1.0 / gamma);
    return 255*nsd;
}

/**
 * Return an array where each element corresponds to an element of
 * the histogram, and the value in each element is the screen pixel
 * value which would result from an image pixel which falls into that
 * histogram bin.
 *
 * @param hist
 * @param rangeValues
 * @param float1d
 * @param imageHeader
 * @return {Uint8Array} array of byte (4096 elements)
 */
function getHistColors(hist, rangeValues, float1d, imageHeader) {

        //calling stretch_pixel to calculate pixeldata, pixelhist
        // byte[] pixeldata = new byte[4096];

    const pixeldata= new Uint8Array(4096);
    const hist_bin_values = new Float32Array(4096);

    for (let i = 0; i < 4096; i++) {
        hist_bin_values[i] = getDNfromBin(hist,i);
    }

    const slow = getSlow(rangeValues, float1d, imageHeader,hist);
    const shigh = getShigh(rangeValues, float1d, imageHeader, hist);

    const start_pixel = 0;
    const last_pixel = 4095;
    const start_line = 0;
    const last_line = 0;
    const naxis1 = 1;
    const blank_pixel_value = 0;

    stretchPixelsByBand(start_pixel, last_pixel, start_line, last_line, naxis1, hist,
        blank_pixel_value, hist_bin_values, pixeldata,  rangeValues, slow, shigh);

    return pixeldata;
}

/**
 * add a new stretch method to do the mask plot
 * @param startPixel (tile info) start pixel in each line
 * @param lastPixel (tile info) end pixel in each line
 * @param startLine (tile info) start line
 * @param lastLine (tile info) end line
 * @param naxis1 number of pixels in a line
 * @param blank_pixel_value blank pixel value
 * @param {Float32Array} float1dArray
 * @param {Uint8Array} pixeldata
 * @param pixelhist
 * @param {Array.<ImageMask>} lsstMasks
 */
export function stretchPixelsForMask(startPixel, lastPixel, startLine, lastLine, naxis1, blank_pixel_value, float1dArray,
                              pixeldata, pixelhist, lsstMasks) {
    let pixelCount = 0;
    const combinedMask = ImageMask.combineWithAnd(lsstMasks);  //mask=33, index=0 and 6 are set

    for (let line = startLine; line <= lastLine; line++) {
        const start_index = line * naxis1 + startPixel;
        const last_index = line * naxis1 + lastPixel;

        for (let index = start_index; index <= last_index; index++) {

            if (isNaN(float1dArray[index])) { //original pixel value is NaN, assign it to blank
                pixeldata[pixelCount] = blank_pixel_value;
            } else {
                /*
                 The IndexColorModel is designed in the way that each pixel[index] contains the color in
                 lsstMasks[index].  In pixel index0, it stores the lsstMasks[0]'s color. Thus, assign
                 pixelData[pixelCount]=index of the lsstMasks, this pixel is going to be plotted using the
                 color stored there.  The color model is indexed.  For 8 bit image, it has 256 maximum colors.
                 For detail, see the indexColorModel defined in ImageData.java.
                 */
                const maskPixel= Math.trunc(float1dArray[index]);
                if (combinedMask.isSet(maskPixel )) {
                    for (let i = 0; i < lsstMasks.length; i++) {
                        if (lsstMasks[i].isSet(maskPixel)) {
                            pixeldata[pixelCount] = i;
                            break;
                        }
                    }
                }
                else {

                    /*
                    The transparent color is stored at pixel[lsstMasks.length].  The pixelData[pixelCount]=(byte) lsstMasks.length,
                    this pixel will be transparent.
                     */
                    pixeldata[pixelCount]= lsstMasks.length;
                }

                pixelhist[pixeldata[pixelCount] & 0xff]++;
            }
            pixelCount++;

        }
    }
}

const BYTE_MAX_VALUE= 127;

function computeBiasAndContrast(data, bias, contrast) {
    let value = data>=0?data:(2*(BYTE_MAX_VALUE+1)+data);
    const offset = (BYTE_MAX_VALUE*(bias-0.5)*-4);
    const shift = (BYTE_MAX_VALUE*(1-contrast));

    value = ( offset+(value*contrast)+shift );
    if (value>(BYTE_MAX_VALUE*2)) value = BYTE_MAX_VALUE*2;
    if (value<0) value = 0;

    return value;
}
