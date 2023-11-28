/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import validator from 'validator';
import {isNil} from 'lodash';

const isInRange= function(val,min,max) {
    const retval= !(min !== undefined && !isNaN(min) && min!==null && val<min);
    return retval && !(max !== undefined && !isNaN(min) && max!==null && val>max);
};

const typeInject= {
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



const makePrecisionStr= function(value,precision) {
    if (!isNil(value)) {
        return (precision>-1) ? value.toFixed(precision) : value;
    }
    else return '';
};

const makeErrorMessage= function(description,min,max,precision) {
    let retval= '';
    const hasMin= (min !== undefined && min!==null);
    const hasMax= (min !== undefined && min!==null);
    const minStr= makePrecisionStr(min,precision);
    const maxStr= makePrecisionStr(max,precision);
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

const validateRange = function(min,max,precision,description,dType, valStr, nullAllowed) {
    const retval= {
        valid : true,
        message : ''
    };
    if (valStr) {
        valStr+= '';
        if (valStr && dType.testFunc(valStr)) {
            const v = dType.convertFunc(valStr);
            if (!isInRange(v, min, max) || isNaN(v)) {
                retval.valid = false;
                retval.message = makeErrorMessage(description, min, max, precision);
            }
        }
        else {
            retval.valid = false;
            retval.message = description + ': must be a '+ dType.dataTypeDesc + makeErrorMessage(null, min, max, precision);
        }
    }
    else if (!nullAllowed) {
        retval.valid = false;
        retval.message = description + ': must be a '+ dType.dataTypeDesc + makeErrorMessage(null, min, max, precision);
    }
    return retval;
};

export const NotBlank = (val='') => {
    const retval = {valid: true,message: ''};
    if (!val.trim()) {
        retval.valid = false;
        retval.message = 'Value must not be blank';
    }
    return retval;
};

export const textValidator = ({min=0, max, pattern, message}) => {
    return (val='') => {
        val = val.trim();
        let error = `Value must be at least ${min} character${min>1 ? 's' : ''}`;
        if (max) error += ` and no more than ${max} characters`;

        if(val.length === 0 && min > 0)    return {valid: false, message: message ?? 'This is a required field. \n' + error};
        if (val.length < min || val.length > (max ?? Number.MAX_VALUE)) return {valid: false, message: message ?? error};
        if (pattern && !val.match(pattern)) return {valid: false, message: message ?? `Value must match pattern: ${pattern}`};

        return {valid: true, message: ''};
    };
};

export const validateEmail = function(description,valStr) {
    const retval = {
        valid: true,
        message: ''
    };
    if (valStr && !validator.isEmail(valStr+'')) {
        retval.valid = false;
        retval.message = description + ': must be a valid email address';
    }
    return retval;
};

export const validateDate = function(description, valStr){
    const retval ={
        valid: true,
        message: ''
    };
    if(valStr){
        try {
            const dateObj = new Date(valStr);
            //Extracting the "date" string from the date object, in yyyy-mm-dd format.
            const parsedDate = dateObj.toISOString().substr(0, dateObj.toISOString().indexOf('T'));
            if(parsedDate !== valStr){
                retval.valid = false;
                retval.message = description + ': must be a valid date formatted as yyyy-mm-dd';
            }
        }
        catch (e) {
            retval.valid = false;
            retval.message = description + ': Unrecognized entry. date should follow the format yyyy-mm-dd';
        }
    }
    return retval;
};

export const validateUrl = function(description,valStr) {
    const retval = {
        valid: true,
        message: ''
    };
    if (valStr && !validator.isURL(valStr+'')) {
        retval.valid = false;
        retval.message = description + ': must be a valid URL';
    }
    return retval;
};

export const intRange = function(min,max,description, valStr, nullAllowed=true) {
   return validateRange(min,max,null,description,typeInject.asInt,valStr, nullAllowed);
};

export const floatRange = function(min,max,precision, description, valStr, nullAllowed=true) {
    return validateRange(min,max,precision,description,typeInject.asFloat,valStr, nullAllowed);
};

export const isFloat = function(description, valStr) {
    const retval= { valid : true, message : '' };
    if (valStr) {
        if (!validator.isFloat(valStr+'')) {
            retval.valid = false;
            retval.message = description + ': must be a number';
        }
    }
    return retval;
};

export const isPositiveFiniteNumber = (description, valStr)=>{
    const retval= { valid : true, message : '' };
    if (valStr) {
        const aNumber = Number.parseFloat(valStr);
        if (!isFinite(aNumber)) {
            retval.valid = false;
            retval.message = description + ': must be a finite float';
        }
        if (aNumber<0){
            retval.valid = false;
            retval.message = description + ': must be a positive float';
        }
    }
    return retval;

};

/**
 * Numbers that must be positive, but infinity as denoted by infValue is
 * allowed.
 * @param description Field name
 * @param valStr value of the field
 * @param allowedInfinityValue Allowed infinity value for the field
 * @returns {{valid: boolean, message: string}}
 */
const isFloatOrInfiniteNumber = (description, valStr, allowedInfinityValue)=> {
    const allowedSign = allowedInfinityValue === Infinity ? '+' : '-';
    const retval = {valid: true, message: ''};
    if (valStr) {
        if (valStr.endsWith('Inf')) {
            valStr = valStr.replaceAll('Inf', 'Infinity');
        }
        const aNumber = Number(valStr);
        if (isNaN(aNumber) || (aNumber < 0 && aNumber !== allowedInfinityValue)) {
            retval.valid = false;
            retval.message = description + `: must be a positive float or ${allowedSign}Inf`;
        }
    }
    return retval;
};

export const isInt = function(description, valStr) {
    const retval= { valid : true, message : '' };
    if (valStr) {
        if (!validator.isInt(valStr+'')) {
            retval.valid = false;
            retval.message = description + ': must be an int';
        }
    }
    return retval;
};

export const isHexColorStr = function(description, valStr) {
    const retval= { valid : true, message : '' };
    if (valStr && !/^#[0-9a-f]{6}/.test(valStr)) {
        retval.valid = false;
        retval.message = description + ': must be a hex color exactly 7 characters long';
    }

};


/*---------------------------- validator function used by InputField to validate a value -----------------------------*/
/*---- these factory functions creates a validation function that takes a value and return {valid,message} -----------*/
export const intValidator = function(min,max,description) {
    return (val) => intRange(min, max, description, val);
};

export const floatValidator = function(min,max,description) {
    return (val) => floatRange(min, max, null, description, val);
};

export const urlValidator = function(description) {
    return (val) => validateUrl(description, val);
};

export const emailValidator = function(description) {
    return (val) => validateEmail(description, val);
};

export const dateValidator = function(description) {
    return (val) => validateDate(description, val);
};

export const maximumPositiveFloatValidator = function (description) {
    return (val) => isFloatOrInfiniteNumber(description, val, Infinity);
};

export const minimumPositiveFloatValidator = function (description) {
    return (val) => isFloatOrInfiniteNumber(description, val, -Infinity);
};

/*--------------------------------------------------------------------------------------------------------------------*/



const Validate = {
    validateEmail, validateUrl, intRange, floatRange, isFloat, isInt,isPositiveFiniteNumber, validateDate
};
export default Validate;