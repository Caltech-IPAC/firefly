/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint esnext:true*/
/*jshint curly:false*/

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



class RangeValues {
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
        this.lowerWhich= Number.parseInt(lowerWhich+'');
        this.lowerValue= Number.parseFloat(lowerValue+'');
        this.upperWhich= Number.parseInt(upperWhich+'');
        this.upperValue= Number.parseFloat(upperValue+'');
        this.drValue = Number.parseFloat(drValue+'');
        this.bpValue = Number.parseFloat(bpValue+'');
        this.wpValue = Number.parseFloat(wpValue+'');
        this.gammaValue=Number.parseFloat(gammaValue+'');
        this.algorithm=  Number.parseInt(algorithm+'');
        this.zscaleContrast= Number.parseInt(zscaleContrast+'');
        this.zscaleSamples= Number.parseInt(zscaleSamples+''); /* desired number of pixels in sample */
        this.zscaleSamplesPerLine= Number.parseInt(zscaleSamplesPerLine+''); /* optimal number of pixels per line */
        this.bias= Number.parseFloat(bias+'');
        this.contrast= Number.parseFloat(contrast+'');
    }

    /**
     *
     * @param stretchType the stretch type, possible values:  "Percent", "Absolute", "Sigma"
     * @param lowerValue the lower value based on the stretch type
     * @param upperValue the upper value based on the stretch type
     * @param algorithm The Stretch algorithm, possible values "Linear", "Log", "LogLog", "Equal", "Squared", "Sqrt"
     *
     * @return
     */
    static create(stretchType, lowerValue, upperValue, drValue,bpValue, wpValue, gammaValue, algorithm) {
        var s= PERCENTAGE;
        if (stretchType) {
            stretchType= stretchType.toLowerCase();
            if (stretchType===PERCENTAGE_STR.toLowerCase()) s=PERCENTAGE;
            else if (stretchType===ABSOLUTE_STR.toLowerCase()) s=ABSOLUTE;
            else if (stretchType===SIGMA_STR.toLowerCase()) s=SIGMA;
        }
        var a= STRETCH_LINEAR;
        if (algorithm) {
            algorithm= algorithm.toLowerCase();
            if (algorithm===LINEAR_STR.toLowerCase()) a= STRETCH_LINEAR;
            else if (algorithm===LOG_STR.toLowerCase()) a=STRETCH_LOG;
            else if (algorithm===LOGLOG_STR.toLowerCase()) a= STRETCH_LOGLOG;
            else if (algorithm===EQUAL_STR.toLowerCase()) a= STRETCH_EQUAL;
            else if (algorithm===SQUARED_STR.toLowerCase()) a= STRETCH_SQUARED;
            else if (algorithm===SQRT_STR.toLowerCase()) a= STRETCH_SQRT;
            else if (algorithm===ASINH_STR.toLowerCase()) a= STRETCH_ASINH;
            else if (algorithm===POWERLAW_GAMMA_STR.toLowerCase()) a= STRETCH_POWERLAW_GAMMA;
        }
        return new RangeValues(s,lowerValue,s,upperValue,drValue,0, 1, gammaValue, a);
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
     * @param sIn serialized string representation of RangeValues
     * @return {RangeValues}
     */
    static parse(sIn) {
        if (!sIn) return null;


        var params= sIn.split(',').map( (v) => validator.toFloat(v) );
        var valid= params.every( (v)=> typeof v !== 'undefined' && !isNaN(v) );

        return valid ? new RangeValues(...params) : false;
    }

    serialize() {
        return this.lowerWhich+','+
               this.lowerValue+','+
               this.upperWhich+','+
               this.upperValue+','+
               this.drValue+','+
               this.bpValue+','+
               this.wpValue+','+
               this.gammaValue+','+
               this.algorithm+','+
               this.zscaleContrast+','+
               this.zscaleSamples+','+
               this.zscaleSamplesPerLine;
    }
}

export default RangeValues;
