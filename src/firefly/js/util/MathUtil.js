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


/**
 * Formats a number to at most `digits` decimal places by rounding.
 * Unlike `toFixed`, the result is a number (not a string) and has no unnecessary trailing zeros.
 *
 * @param {number|string} floatNum - The number to format.
 * @param {number} digits - Maximum number of decimal places to keep.
 * @returns {number} The formatted number with at most `digits` decimal places.
 *
 * @example
 * toMaxFixed(3.14159, 4) // 3.1416
 * toMaxFixed(3.10000, 4) // 3.1
 * toMaxFixed(3,       2) // 3
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

export const clampInRange = (num, min, max) => num <= max ? (num >= min ? num : min) : max;
