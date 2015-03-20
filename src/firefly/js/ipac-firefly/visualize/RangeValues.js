/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint esnext:true*/
/*jshint curly:false*/
import validator from "validator";


export const PERCENTAGE_STR = "Percent";
export const ABSOLUTE_STR   = "Absolute";
export const SIGMA_STR      = "Sigma";

export const PERCENTAGE = 88;
export const MAXMIN     = 89;
export const ABSOLUTE   = 90;
export const ZSCALE     = 91;
export const SIGMA      = 92;


export const LINEAR_STR= "Linear";
export const LOG_STR= "Log";
export const LOGLOG_STR= "LogLog";
export const EQUAL_STR= "Equal";
export const SQUARED_STR= "Squared";
export const SQRT_STR= "Sqrt";

export const STRETCH_LINEAR= 44;
export const STRETCH_LOG   = 45;
export const STRETCH_LOGLOG= 46;
export const STRETCH_EQUAL = 47;
export const STRETCH_SQUARED = 48;
export const STRETCH_SQRT    = 49;


const BYTE_MAX_VALUE= 127;



class RangeValues {
    constructor( lowerWhich= PERCENTAGE,
                 lowerValue= 1.0,
                 upperWhich= PERCENTAGE,
                 upperValue= 99.0,
                 algorithm= STRETCH_LINEAR,
                 zscaleContrast= 25,
                 zscaleSamples= 600,
                 zscaleSamplesPerLine= 120,
                 bias= 0.5,
                 contrast= 1.0 ) {
        this.lowerWhich= lowerWhich;
        this.lowerValue= lowerValue;
        this.upperWhich= upperWhich;
        this.upperValue= upperValue;
        this.algorithm=  algorithm;
        this.zscaleContrast= zscaleContrast;
        this.zscaleSamples= zscaleSamples; /* desired number of pixels in sample */
        this.zscaleSamplesPerLine= zscaleSamplesPerLine; /* optimal number of pixels per line */
        this.bias= bias;
        this.contrast= contrast;
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
    static create(stretchType, lowerValue, upperValue, algorithm) {
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
        }
        return new RangeValues(s,lowerValue,s,upperValue,a);
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
                                this.upperValue, this.algorithm,
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
    parse(sIn) {
        if (!sIn) return null;


        var params= sIn.split(",").map(v=>{
            validator.toFloat(v);
        });

        var valid= params.reduce((v,value)=>{
            return (v&&!isNaN(value));
        },true);

        return valid ? new RangeValues(...params) : false;
    }

    serialize() {
        return this.lowerWhich+","+
               this.lowerValue+","+
               this.upperWhich+","+
               this.upperValue+","+
               this.stretchAlgorithm+","+
               this.zscaleContrast+","+
               this.zscaleSamples+","+
               this.zscaleSamplesPerLine;
    }
}

export default RangeValues;
