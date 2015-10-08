/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

const SPLIT_TOKEN= '--WebFitsData--';
import join from 'underscore.string/join';
import {parseInt, parseFloat, getStringWithNull } from '../util/StringUtils.js';

class WebFitsData {

    /**
     *
     * @param {number} dataMin - the minimum data value in the fits file
     * @param {number} dataMax - the maximum data value in the fits file
     * @param {number} fitsFileSize
     * @param {string} fluxUnits = the units string, i.e. a string the describes the units
     */
    constructor(dataMin, dataMax, fitsFileSize, fluxUnits ) {
        this.dataMin= dataMin;
        this.dataMax= dataMax;
        this.fitsFileSize= fitsFileSize;
        this.fluxUnits= fluxUnits;
    }

    toString() {
        return join(SPLIT_TOKEN, this.dataMin,  this.dataMax,  this.fitsFileSize,  this.fluxUnits);
    }

    static parse(s) {
        if (s==null) return null;
        var  sAry= s.split(SPLIT_TOKEN,5);
        if (sAry.length!==4) return null;

        var i= 0;
        const dataMin= parseFloat(sAry[i++], 0);
        const dataMax= parseFloat(sAry[i++], 0);
        const fitsFileSize= parseInt(sAry[i++], 0);
        const fluxUnits= getStringWithNull(sAry[i], 0);
        return new WebFitsData(dataMin,dataMax,fitsFileSize,fluxUnits);
    }
}

