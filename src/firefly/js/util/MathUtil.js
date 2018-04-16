/*
 Get the format string for a number, given an interval between the adjacent numbers
 and the number of significant digits you'd like to preserve in this interval
 */
export const getFormatString = function(range, numSigDigits) {

    if (range===0) { return '0'; }

    const numDecPlaces = getDecimalPlaces(range, numSigDigits);
    if (numDecPlaces > 0) {
        let format = '0.';
        for (let n = 0; n < numDecPlaces; n++) {
            format += '0';
        }
        return format;
    } else {
        return '0';
    }
};

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