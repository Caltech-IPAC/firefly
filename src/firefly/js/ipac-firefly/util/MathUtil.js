/**
    * @author tatianag
    */
"use strict";


/*
 Get the format string for a number, given an interval between the adjacent numbers
 and the number of significant digits you'd like to preserve in this interval
 */
export const getFormatString = function(range, numSigDigits) {

    var format; // string format

    // before ES6, use Math.log(val)/Math.LN10 for Math.log10;
    var firstSigDigitPos = Math.floor(Math.log10(Math.abs(range)))+1;
    if (firstSigDigitPos < numSigDigits) {
        format = "0.";
        var numDecPlaces;
        // find how many places after the decimal
        if (firstSigDigitPos > 0) {
            numDecPlaces = Math.abs(numSigDigits - firstSigDigitPos);
        } else {
            numDecPlaces = Math.abs(firstSigDigitPos) + numSigDigits;
        }
        for (let n = 0; n < numDecPlaces; n++) {
            format += "0";
        }

    } else {
        format = "0";
    }
    return format;
};

