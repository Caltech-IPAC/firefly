import validator from 'validator';

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

export class ExtensionResult {
    constructor() {
    }

    setExtValue(key,value) {
        if (validator.isFloat(value)) value= validator.toFloat(value);
        else if (validator.isBoolean(value)) value= validator.toBoolean(value);
        this[key]= value;
    }
    setNumberExtValue(key,value) {
        this[key]= value;
    }
}

