/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isString, isNumber} from 'lodash';

export function getDecimalPlaces(range, numSigDigits) {
    if (range===0) { return undefined; }

    let numDecPlaces = 0;
    // before ES6, use Math.log(val)/Math.LN10 for Math.log10;
    const firstSigDigitPos = Math.floor(Math.log10(Math.abs(range)))+1;
    if (firstSigDigitPos < numSigDigits) {
        // find how many places after the decimal
        if (firstSigDigitPos > 0) {
            numDecPlaces = Math.abs(numSigDigits - firstSigDigitPos);
        } else {
            numDecPlaces = Math.abs(firstSigDigitPos) + numSigDigits;
        }
    }
    return numDecPlaces;
}

/*
 * remove trailing zero from toFixed result
 */
export function toMaxFixed(floatNum, digits) {
    return parseFloat(Number(floatNum).toFixed(digits));
}

export function isDigit(c) {
    if (isNumber(c) && c>=0 && c<=9) return true;
    return isString(c) && c.length===1 && !isNaN(parseInt(c));
}

export function allDigits(s) {
    if (isNumber(s)) return Math.trunc(s)===s;
    return isString(s) && [...s].every( (c) => isDigit(c));
}
