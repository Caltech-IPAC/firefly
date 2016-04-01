/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

//LZ 06/11/15 add arcsine and power law gamma parameters
//LZ 06/26/15 add zp and wp parameters
import validator from 'validator';


export const PERCENTAGE_STR = 'Percent';
export const ABSOLUTE_STR   = 'Absolute';
export const SIGMA_STR      = 'Sigma';

export const PERCENTAGE = 88;
export const MAXMIN     = 89;
export const ABSOLUTE   = 90;
export const ZSCALE     = 91;
export const SIGMA      = 92;


export const LINEAR_STR= 'Linear';
export const LOG_STR= 'Log';
export const LOGLOG_STR= 'LogLog';
export const EQUAL_STR= 'Equal';
export const SQUARED_STR= 'Squared';
export const SQRT_STR= 'Sqrt';

export const ASINH_STR= 'asinh';
export const POWERLAW_GAMMA_STR= 'powerlaw_gamma';


export const STRETCH_LINEAR= 44;
export const STRETCH_LOG   = 45;
export const STRETCH_LOGLOG= 46;
export const STRETCH_EQUAL = 47;
export const STRETCH_SQUARED = 48;
export const STRETCH_SQRT    = 49;
export const  STRETCH_ASINH   = 50;
export const STRETCH_POWERLAW_GAMMA   = 51;


const BYTE_MAX_VALUE= 127;



export class RangeValues {
    constructor( lowerWhich= PERCENTAGE,
                 lowerValue= 1.0,
                 upperWhich= PERCENTAGE,
                 upperValue= 99.0,
                 drValue=1.0,
                 bpValue=0.0,
                 wpValue=1.0,
                 gammaValue=2.0,
                 algorithm= STRETCH_LINEAR,
                 zscaleContrast= 25,
                 zscaleSamples= 600,
                 zscaleSamplesPerLine= 120,
                 bias= 0.5,
                 contrast= 1.0 ) {
        this.lowerWhich= parseInt(lowerWhich);
        this.lowerValue= parseFloat(lowerValue);
        this.upperWhich= parseInt(upperWhich);
        this.upperValue= parseFloat(upperValue);
        this.drValue = parseFloat(drValue);
        this.bpValue = parseFloat(bpValue);
        this.wpValue = parseFloat(wpValue);
        this.gammaValue=parseFloat(gammaValue);
        this.algorithm=  parseInt(algorithm);
        this.zscaleContrast= parseInt(zscaleContrast);
        this.zscaleSamples= parseInt(zscaleSamples); /* desired number of pixels in sample */
        this.zscaleSamplesPerLine= parseInt(zscaleSamplesPerLine); /* optimal number of pixels per line */
        this.bias= parseFloat(bias);
        this.contrast= parseFloat(contrast);
    }


    computeBiasAndContrast(data) {
        var value = data>=0?data:(2*(BYTE_MAX_VALUE+1)+data);
        var offset = (BYTE_MAX_VALUE*(this.bias-0.5)*-4);
        var shift = (BYTE_MAX_VALUE*(1-this.contrast));

        value = ( offset+(value*this.contrast)+shift );
        if (value>(BYTE_MAX_VALUE*2)) value = BYTE_MAX_VALUE*2;
        if (value<0) value = 0;

        return value;
    }

    /**
     * @return {RangeValues}
     */
    static clone() {
        return new RangeValues( this.lowerWhich, this.lowerValue, this.upperWhich,
                                this.upperValue, this.drValue, this.bpValue, this.wpValue, this.gammaValue, this.algorithm,
                                this.zscaleContrast, this.zscaleSamples,
                                this.zscaleSamplesPerLine,
                                this.bias, this.contrast );
    }

    toString() { return this.serialize(); }


    /**
     * @return {RangeValues}
     */
    static makeDefaultSigma() {
        return new RangeValues(SIGMA,-2,SIGMA,10,STRETCH_LINEAR);
    }


    /**
     * @return {RangeValues}
     */
    static makeDefaultPercent() {
        return new RangeValues(PERCENTAGE,-2,PERCENTAGE,10,STRETCH_LINEAR);
    }

    /**
     * @return {RangeValues}
     */
    static makeDefaultZScale() {
        return new RangeValues(ZSCALE,1,ZSCALE,1,STRETCH_LINEAR,25, 600, 120);
    }

    /**
     * 
     * @param lowerWhich
     * @param lowerValue
     * @param upperWhich
     * @param upperValue
     * @param drValue
     * @param bpValue
     * @param wpValue
     * @param gammaValue
     * @param algorithm
     * @param zscaleContrast
     * @param zscaleSamples
     * @param zscaleSamplesPerLine
     * @param bias
     * @param contrast
     * @return {RangeValues}
     */
    static make(lowerWhich= PERCENTAGE,
                lowerValue= 1.0,
                upperWhich= PERCENTAGE,
                upperValue= 99.0,
                drValue=1.0,
                bpValue=0.0,
                wpValue=1.0,
                gammaValue=2.0,
                algorithm= STRETCH_LINEAR,
                zscaleContrast= 25,
                zscaleSamples= 600,
                zscaleSamplesPerLine= 120,
                bias= 0.5,
                contrast= 1.0 ) {

        return new RangeValues( lowerWhich, lowerValue, upperWhich, upperValue, drValue,
            bpValue, wpValue, gammaValue, algorithm, zscaleContrast, zscaleSamples,
            zscaleSamplesPerLine, bias, contrast);
    }

    /**
     * @param sIn serialized string representation of RangeValues
     * @return {RangeValues}
     */
    static parse(sIn) {
        if (!sIn) return null;


        var params= sIn.split(',').map( (v) => validator.toFloat(v) );
        var valid= params.every( (v)=> typeof v !== 'undefined' && !isNaN(v) );

        return valid ? new RangeValues(...params) : null;
    }

    serialize() {
        return RangeValues.serializeRV(this);
    }

    static serializeRV(rv) {
        return rv.lowerWhich+','+
               rv.lowerValue+','+
               rv.upperWhich+','+
               rv.upperValue+','+
               rv.drValue+','+
               rv.bpValue+','+
               rv.wpValue+','+
               rv.gammaValue+','+
               rv.algorithm+','+
               rv.zscaleContrast+','+
               rv.zscaleSamples+','+
               rv.zscaleSamplesPerLine;
    }
    
}

export default RangeValues;
