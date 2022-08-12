/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNumber, isString, isObject} from 'lodash';


export const PERCENTAGE_STR = 'percent';
export const ABSOLUTE_STR   = 'absolute';
export const SIGMA_STR      = 'sigma';

export const PERCENTAGE = 88;
// export const MINMAX     = 89;  // obsolete
export const ABSOLUTE   = 90;
export const ZSCALE     = 91;
export const SIGMA      = 92;

// the values should match the constants in RangeValues.java
export const STRETCH_LINEAR= 44;
export const STRETCH_LOG   = 45;
export const STRETCH_LOGLOG= 46;
export const STRETCH_EQUAL = 47;
export const STRETCH_SQUARED = 48;
export const STRETCH_SQRT    = 49;
export const STRETCH_ASINH   = 50;
export const STRETCH_POWERLAW_GAMMA   = 51;

const LINEAR_STR= 'linear';

const alStrToConst = {
    log : STRETCH_LOG,
    loglog : STRETCH_LOGLOG,
    equal : STRETCH_EQUAL,
    squared : STRETCH_SQUARED,
    sqrt : STRETCH_SQRT,
    asinh : STRETCH_ASINH,
    powerlaw_gamma : STRETCH_POWERLAW_GAMMA,
};


const boundsStrToConst = {
    percent:  PERCENTAGE,
    absolute: ABSOLUTE,
    sigma: SIGMA
};

const RGB_PRESERVE_HUE_DEFAULT= 0; // user 0 for false

export class RangeValues {
    constructor( lowerWhich= PERCENTAGE,
                 lowerValue= 1.0,
                 upperWhich= PERCENTAGE,
                 upperValue= 99.0,
                 asinhQValue=Number.NaN,
                 gammaValue=2.0,
                 algorithm= STRETCH_LINEAR,
                 zscaleContrast= 25,
                 zscaleSamples= 600,
                 zscaleSamplesPerLine= 120,
                 rgbPreserveHue= RGB_PRESERVE_HUE_DEFAULT,
                 asinhStretch=Number.NaN,
                 scalingK=1.0,
                 bias= 0.5,
                 contrast= 1.0 ) {
        this.lowerWhich= parseInt(lowerWhich+'');
        this.lowerValue= parseFloat(lowerValue+'');
        this.upperWhich= parseInt(upperWhich+'');
        this.upperValue= parseFloat(upperValue+'');
        this.asinhQValue = parseFloat(asinhQValue+'');
        this.gammaValue=parseFloat(gammaValue+'');
        this.algorithm=  parseInt(algorithm+'');
        this.zscaleContrast= parseInt(zscaleContrast+'');
        this.zscaleSamples= parseInt(zscaleSamples+''); /* desired number of pixels in sample */
        this.zscaleSamplesPerLine= parseInt(zscaleSamplesPerLine+''); /* optimal number of pixels per line */
        this.rgbPreserveHue=  parseInt(rgbPreserveHue+''); /* if 0, stretch by band, otherwise preserve hue*/
        this.asinhStretch=parseFloat(asinhStretch+'');
        this.scalingK=parseFloat(scalingK+'');
        if (this.rgbPreserveHue > 0) {
            this.algorithm = STRETCH_ASINH;
            this.upperWhich = ZSCALE;
        }
        this.bias= parseFloat(bias+'');
        this.contrast= parseFloat(contrast+'');
    }



    /**
     * @return {RangeValues}
     */
    clone() {
        return new RangeValues( this.lowerWhich, this.lowerValue, this.upperWhich,
            this.upperValue, this.asinhQValue,  this.gammaValue, this.algorithm,
            this.zscaleContrast, this.zscaleSamples, this.zscaleSamplesPerLine,
            this.rgbPreserveHue,this.asinhStretch,this.scalingK,
            this.bias, this.contrast );
    }

    toString() { return this.toJSON(); }


    /**
     *
     * @param p the params objects
     * @param p.which
     * @param p.lowerWhich
     * @param p.lowerValue
     * @param p.upperWhich
     * @param p.upperValue
     * @param p.asinhQValue
     * @param p.gammaValue
     * @param p.algorithm
     * @param p.zscaleContrast
     * @param p.zscaleSamples
     * @param p.zscaleSamplesPerLine
     * @param p.rgbPreserveHue
     * @param p.asinhStretch - stretch parameter for hue-preserving rgb
     * @param p.scalingK - flux scaling coefficient for hue-preserving rgb
     * @param p.bias
     * @param p.contrast
     * @return {RangeValues}
     */
    static makeRV({which= PERCENTAGE,
                      lowerWhich,
                      lowerValue= 1.0,
                      upperWhich,
                      upperValue= 99.0,
                      asinhQValue= Number.NaN,
                      gammaValue=2.0,
                      algorithm= STRETCH_LINEAR,
                      zscaleContrast= 25,
                      zscaleSamples= 600,
                      zscaleSamplesPerLine= 120,
                      rgbPreserveHue= RGB_PRESERVE_HUE_DEFAULT,
                      asinhStretch= Number.NaN,
                      scalingK=1.0,
                      bias= 0.5,
                      contrast= 1.0} ) {

        lowerWhich= lowerWhich || which;
        upperWhich= upperWhich || which;
        return new RangeValues( lowerWhich, lowerValue, upperWhich, upperValue, asinhQValue,
            gammaValue, algorithm, zscaleContrast, zscaleSamples,
            zscaleSamplesPerLine, rgbPreserveHue, asinhStretch, scalingK, bias, contrast);
    }

    /**
     * 
     * @param lowerWhich
     * @param lowerValue
     * @param upperWhich
     * @param upperValue
     * @param asinhQValue
     * @param gammaValue
     * @param algorithm
     * @param zscaleContrast
     * @param zscaleSamples
     * @param zscaleSamplesPerLine
     * @param rgbPreserveHue
     * @param asinhStretch used for hue preserving rgb, when NaN use zscale to estimate stretch value
     * @param scalingK used for hue preserving rgb
     * @param bias
     * @param contrast
     * @return {RangeValues}
     */
    static make(lowerWhich= PERCENTAGE,
                lowerValue= 1.0,
                upperWhich= PERCENTAGE,
                upperValue= 99.0,
                asinhQValue= Number.NaN,
                gammaValue= 2.0,
                algorithm= STRETCH_LINEAR,
                zscaleContrast= 25,
                zscaleSamples= 600,
                zscaleSamplesPerLine= 120,
                rgbPreserveHue= RGB_PRESERVE_HUE_DEFAULT,
                asinhStretch= Number.NaN,
                scalingK=1.0,
                bias= 0.5,
                contrast= 1.0 ) {
        return new RangeValues( lowerWhich, lowerValue, upperWhich, upperValue, asinhQValue,
             gammaValue, algorithm, zscaleContrast, zscaleSamples,
            zscaleSamplesPerLine, rgbPreserveHue, asinhStretch, scalingK, bias, contrast);
    }
    
    /**
     *
     * @param boundsType one of 'percent', 'absolute', 'sigma'
     * @param lowerValue lower value of stretch, based on stretchType
     * @param upperValue upper value of stretch, based on stretchType
     * @param algorithm one of 'linear', 'log', 'loglog', 'equal', 'squared', 'sqrt', 'asinh', powerlaw_gamma'
     * @return {*}
     */
    static makeSimple(boundsType= PERCENTAGE_STR,
                lowerValue= 1.0,
                upperValue= 1.0,
                algorithm= LINEAR_STR) {

        let btValue= PERCENTAGE;
        if (boundsStrToConst[boundsType.toLowerCase()]) {
            btValue= boundsStrToConst[boundsType.toLowerCase()];
        }

        let a= STRETCH_LINEAR;
        if (alStrToConst[algorithm.toLowerCase()]) {
            a= alStrToConst[algorithm.toLowerCase()];
        }

        return RangeValues.make( btValue, lowerValue, btValue, upperValue,Number.NaN,2, a);

    }

    static make2To10SigmaLinear() {
        return RangeValues.makeRV({which:SIGMA, lowerValue:-2, upperValue:10, algorithm:STRETCH_LINEAR});
    }

    /**
     *
     * @param boundsType one of 'percent', 'absolute', 'sigma'
     * @param lowerValue lower value of stretch, based on stretchType
     * @param upperValue upper value of stretch, based on stretchType
     * @param algorithm one of 'log', 'loglog', 'equal', 'squared', 'sqrt', 'asinh', powerlaw_gamma'
     * @return {*}
     */
    static serializeSimple(boundsType= PERCENTAGE_STR,
                           lowerValue= 1.0,
                           upperValue= 1.0,
                           algorithm= LINEAR_STR ) {
        const rv= RangeValues.makeSimple(boundsType,lowerValue,upperValue,algorithm);
        return rv.toJSON();
    }

    /**
     * @param sIn serialized string representation of RangeValues
     * @return {RangeValues}
     */
    static parse(sIn) {
        if (!sIn) return undefined;
        if (isObject(sIn) &&
            isNumber(sIn.lowerWhich) && isNumber(sIn.upperWhich) &&
            isNumber(sIn.lowerValue) && isNumber(sIn.upperValue) && isNumber(sIn.algorithm)) {
            return sIn;
        }
        if (!isString(sIn)) return undefined;


        const params= sIn.split(',').map( (v) => Number.parseFloat(v) );
        const valid= params.every( (v)=> typeof v !== 'undefined');

        return valid ? new RangeValues(...params) : undefined;
    }

    toJSON() {
        return RangeValues.serializeRV(this);
    }

    static serializeRV(rv) {
        return rv.lowerWhich+','+
            rv.lowerValue+','+
            rv.upperWhich+','+
            rv.upperValue+','+
            rv.asinhQValue+','+
            rv.gammaValue+','+
            rv.algorithm+','+
            rv.zscaleContrast+','+
            rv.zscaleSamples+','+
            rv.zscaleSamplesPerLine+','+
            rv.rgbPreserveHue+','+
            rv.asinhStretch+','+
            rv.scalingK;
    }
    
}

export default RangeValues;
