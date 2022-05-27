/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {ABSOLUTE, PERCENTAGE, SIGMA, ZSCALE} from '../../../RangeValues.js';
import {get_pct, get_sigma} from './Histogram.js';
import {cdl_zscale} from './Zscale.js';

/**
 * The Bscale  keyword shall be used, along with the BZERO keyword, when the array pixel values are not the true  physical  values,
 * to transform the primary data array  values to the true physical values they represent, using Eq. 5.3. The value field shall contain a
 * floating point number representing the coefficient of the linear term in the scaling equation, the ratio of physical value to array value
 * at zero offset. The default value for this keyword is 1.0.BZERO Keyword
 * BZERO keyword shall be used, along with the BSCALE keyword, when the array pixel values are not the true  physical values, to transform
 * the primary data array values to the true values. The value field shall contain a floating point number representing the physical value corresponding to an array value of zero. The default value for this keyword is 0.0.
 * The transformation equation is as follows:
 * physical_values = BZERO + BSCALE × array_value	(5.3)
 *
 * This method return the physical data value for the given raw value with scaling coefficient applied
 *
 * @param raw_dn raw data value
 * @param imageHeader image header
 * @param {RangeValues} rv range values
 * @return physical data value
 */
export function getScaled(raw_dn, imageHeader, rv) {
    if ((raw_dn===imageHeader.blank_value) || isNaN(raw_dn)) {
        return NaN;
    } else {
        return rv.scalingK*(raw_dn * imageHeader.bscale + imageHeader.bzero);
    }
}

export function getShigh(rangeValues, float1d, imageHeader, hist) {
    const {upperWhich,upperValue}= rangeValues;
    switch (upperWhich) {
        case ABSOLUTE:
            return (upperValue - imageHeader.bzero) / imageHeader.bscale;
        case PERCENTAGE:
            return  get_pct(hist, upperValue, true);
        case SIGMA:
            return get_sigma(hist, upperValue, true);
        case ZSCALE:
            return getZscaleValue(float1d, imageHeader, rangeValues).z2;
        default:
            return 0;
    }
}

/**
 *
 * @param float1d
 * @param imageHeader
 * @param rangeValues
 * @return {{z1: number, z2: number}}
 */
export function getZscaleValue(float1d, imageHeader, rangeValues) {
    const {zscaleContrast, zscaleSamples, zscaleSamplesPerLine}= rangeValues;
    return cdl_zscale(float1d, imageHeader.naxis1, imageHeader.naxis2,
        imageHeader.bitpix, zscaleContrast / 100.0, zscaleSamples, zscaleSamplesPerLine,
        imageHeader.blank_value );
}

export function getSlow(rangeValues, float1d, imageHeader, hist) {
    const {lowerWhich,lowerValue}= rangeValues;
    switch (lowerWhich) {
        case ABSOLUTE:
            return  (lowerValue - imageHeader.bzero) /imageHeader.bscale;
        case PERCENTAGE:
            return get_pct(hist, lowerValue, false);
        case SIGMA:
            return get_sigma(hist, lowerValue, false);
        case ZSCALE:
            return getZscaleValue(float1d, imageHeader, rangeValues).z1;
        default:
            return 0;
    }
}