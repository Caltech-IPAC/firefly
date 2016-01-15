/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/* eslint prefer-template:0 */
//var validator= require('validator');
//var sprintf= require('underscore.string/sprintf');
import validator from 'validator';
import sprintf from 'underscore.string/sprintf';

var isInRange= function(val,min,max) {
    var retval= !(min !== undefined && min!==null && val<min);
    return retval && !(max !== undefined && max!==null && val>max);
};

var typeInject= {
    asInt : {
        dataTypeDesc : 'integer',
        testFunc : validator.isInt,
        convertFunc : validator.toInt
    },
    asFloat : {
        dataTypeDesc : 'float',
        testFunc : validator.isFloat,
        convertFunc : validator.toFloat
    }
};



var makePrecisionStr= function(value,precision) {
    if (value !== undefined && value!==null && precision) {
        return sprintf('%.'+precision+'f',value);
    }
    return (value || '')+ '';
};

var makeErrorMessage= function(description,min,max,precision) {
    var retval= '';
    var hasMin= (min !== undefined && min!==null);
    var hasMax= (min !== undefined && min!==null);
    var minStr= makePrecisionStr(min,precision);
    var maxStr= makePrecisionStr(max,precision);
    description= description || '';
    if (hasMin && hasMax) {
        retval= description + ': must be between ' + minStr + ' and ' + maxStr;
    }
    else if (hasMin) {
        retval= description + ': must be greater than ' + minStr;
    }
    else if (hasMax) {
        retval= description + ': must be less than ' + maxStr;
    }
    return retval;
};

var validateRange = function(min,max,precision,description,dType, valStr) {
    var retval= {
        valid : true,
        message : ''
    };
    if (valStr) {
        if (valStr && dType.testFunc(valStr)) {
            var v = dType.convertFunc(valStr);
            if (!isInRange(v, min, max) || isNaN(v)) {
                retval.valid = false;
                retval.message = makeErrorMessage(description, min, max, precision);
            }
        }
        else {
            retval.valid = false;
            retval.message = description + ': must be a '+ dType.dataTypeDesc + makeErrorMessage(null, min, max, null);
        }
    }
    return retval;
};

export const validateEmail = function(description,valStr) {
    var retval = {
        valid: true,
        message: ''
    };
    if (!validator.isEmail(valStr)) {
        retval.valid = false;
        retval.message = description + ': must be a valid email address';
    }
    return retval;
};

export const validateUrl = function(description,valStr) {
    var retval = {
        valid: true,
        message: ''
    };
    if (!validator.isURL(valStr)) {
        retval.valid = false;
        retval.message = description + ': must be a valid URL';
    }
    return retval;
};

export const intRange = function(min,max,description, valStr) {
   return validateRange(min,max,null,description,typeInject.asInt,valStr);
};

export const floatRange = function(min,max,precision, description, valStr) {
    return validateRange(min,max,precision,description,typeInject.asFloat,valStr);
};

export const isFloat = function(description, valStr) {
    var retval= { valid : true, message : '' };
    if (valStr) {
        if (!validator.isFloat(valStr)) {
            retval.valid = false;
            retval.message = description + ': must be a float';
        }
    }
    return retval;
};


export const isInt = function(description, valStr) {
    var retval= { valid : true, message : '' };
    if (valStr) {
        if (!validator.isInt(valStr)) {
            retval.valid = false;
            retval.message = description + ': must be an int';
        }
    }
    return retval;
};

var Validate = {
    validateEmail, validateUrl, intRange, floatRange, isFloat, isInt
};
export default Validate;