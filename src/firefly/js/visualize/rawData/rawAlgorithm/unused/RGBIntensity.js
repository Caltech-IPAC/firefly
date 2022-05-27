/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getScaled, getSlow, getZscaleValue} from './StretchUtil.js';
import {ZSCALE} from '../../../RangeValues.js';

/**
 * This class is used to store intensity-related info for RGB hue preserving algorithm.
 * It accumulates range values and calculates the statistics for the intensity, when the range value info
 * for all three bands is available.
 * Intensity and the related values are recalculated only when the black points (as reflected by range values) are changing.
 * @author tatianag
 */
export class RGBIntensity {

    constructor() {
        this.rangeValuesAry = undefined;
        this.intensityCacheKey = '';
        this.intensityDataLow = NaN;
        this.intensityDataHigh = NaN;
        this.intensityLow = NaN;
        this.intensityHigh = NaN;
    }

    addRangeValues(float1dAry, processHeaderAry, histAry, rangeValuesAry) {
        this.rangeValuesAry= rangeValuesAry;
        this.rangeValuesAry=rangeValuesAry;
        this.computeRGBIntensityStats(float1dAry, processHeaderAry, histAry, rangeValuesAry);
    }


    computeRGBIntensityStats(float1dAry, processHeaderAry, histAry, rangeValuesAry) {

        if (float1dAry.some( (a) => !a)) throw new Error('fitsReadAry should contain 3 non-null values for hue-preserving RGB.');

        const blankPxValAry = [];
        for(let i=0; i<3; i++) {
            blankPxValAry[i]= processHeaderAry[i].blank_value;
        }
        for(let i=0; (i<float1dAry.length); i++) {
            if (!float1dAry[i] || !processHeaderAry[i] || !histAry[i]) {
                throw new Error('3 bands are required for hue preserving stretch.');
            }
        }
        if (processHeaderAry[0].naxis1!==processHeaderAry[1].naxis1 || processHeaderAry[1].naxis1!==processHeaderAry[2].naxis1 ||
                processHeaderAry[0].naxis2!==processHeaderAry[1].naxis2 || processHeaderAry[1].naxis2!==processHeaderAry[2].naxis2) {
            throw new Error('Hue-preserving stretch: naxis1 and naxis2 must match. '+
                    'r: ('+processHeaderAry[0].naxis1+','+processHeaderAry[0].naxis2+') '+
                    'g: ('+processHeaderAry[1].naxis1+','+processHeaderAry[1].naxis2+') '+
                    'b: ('+processHeaderAry[2].naxis1+','+processHeaderAry[2].naxis2+')');
        }

        // green and blue images might be reprojected, hence it is important to work with scaled values

        const slowAry = [];
        for(let i=0; i<3; i++) {
            blankPxValAry[i]= processHeaderAry[i].blank_value;
            slowAry[i] = getSlow(rangeValuesAry[i], float1dAry[i], processHeaderAry[i], histAry[i]);
            slowAry[i] = getScaled(slowAry[i], processHeaderAry[i], rangeValuesAry[i]);
        }
        const intensity = new Float32Array(float1dAry[0].length);
        intensity.fill(NaN);
        let val, minVal = Number.MAX_VALUE, maxVal=Number.MIN_VALUE;
        for (let i=0; i<float1dAry[0].length; i++) {
            // check for blank pixel values
            if (float1dAry[0][i]=== blankPxValAry[0] || float1dAry[1][i]=== blankPxValAry[1] || float1dAry[2][i]=== blankPxValAry[1]) {
                continue;
            }

            val = computeIntensity(i, float1dAry, processHeaderAry, slowAry, rangeValuesAry);

            // save min and max
            if (val < minVal) { minVal = val; }
            if (val > maxVal) { maxVal = val; }

            intensity[i] = val;
        }
        this.intensityDataLow = (minVal !== Number.MAX_VALUE) ? minVal : NaN;
        this.intensityDataHigh = (maxVal !== Number.MIN_VALUE) ? maxVal : NaN;

        // zscale settings are shared between bands
        const useZ = rangeValuesAry[0].lowerWhich===ZSCALE;
        if (useZ || !isFinite(rangeValuesAry[0].asinhStretch)) {
            // for zscale only: calculate z1 and z2 for intensity
            // use the last image header, because after reprojection, bzero and bscale are removed in green and blue
            // zscale parameters are shared between range values, no matter range values which to use
            const {z1,z2} = getZscaleValue(intensity, processHeaderAry[2], rangeValuesAry[0]);
            this.intensityLow = z1;
            this.intensityHigh = z2;
        } else {
            this.intensityLow = NaN;
            this.intensityHigh = NaN;
        }
        this.intensityCacheKey = getCacheKey(rangeValuesAry);
    }

    getIntensityDataLow() { return this.intensityDataLow; }
    getIntensityDataHigh() { return this.intensityDataHigh; }
    getIntensityLow() { return this.intensityLow; }
    getIntensityHigh() { return this.intensityHigh; }
}

export function computeIntensity(i, float1dAry, imageHeaderAry, slowAry, rangeValuesAry) {
    const val = (getScaled(float1dAry[0][i], imageHeaderAry[0], rangeValuesAry[0])-slowAry[0]+
        getScaled(float1dAry[1][i], imageHeaderAry[1], rangeValuesAry[1])-slowAry[1]+
        getScaled(float1dAry[2][i], imageHeaderAry[2],rangeValuesAry[2])-slowAry[2])/3.0;
    return val > 0 ? val : 0;
}

/**
 * Intensity and the limits needs to be recalculated when the black points of the bands change.
 * If the key is the same, no need to recalculate.
 * @param rangeValuesAry and array of red, green, and blue range values
 * @return a string describing black points, derived from range values
 */
const getCacheKey= (rangeValuesAry) =>
           rangeValuesAry?.reduce( (str, rv) => str+`${rv.scalingK},${rv.lowerWhich},${rv.lowerValue};`, '') ?? '';

