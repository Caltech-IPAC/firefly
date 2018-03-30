/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import WebPlotRequest from './WebPlotRequest.js';
import {RangeValues} from './RangeValues.js';
import {makeClientFitsHeader} from './ClientFitsHeader.js';

/**
 * @global
 * @public
 * @typedef {Object} BandState
 * @prop workingFitsFileStr
 * @prop originalFitsFileStr
 * @prop uploadFileNameStr
 * @prop imageIdx
 * @prop originalImageIdx
 * @prop plotRequestSerialize
 * @prop rangeValuesSerialize
 * @prop fitsHeader
 * @prop bandVisible
 * @prop multiImageFile
 * @prop tileCompress
 * @prop cubeCnt
 * @prop cubePlaneNumber
 */
export class BandState {
    constructor() {
        this.workingFitsFileStr = null;
        this.originalFitsFileStr= null;
        this.uploadFileNameStr= null;
        this.imageIdx= 0;
        this.originalImageIdx= 0;

        this.plotRequestSerialize = null; // Serialized WebPlotRequest
        this.rangeValuesSerialize = null; // Serialized RangeValues
        this.fitsHeader= null;
        this.bandVisible= true;
        this.multiImageFile = false;
        this.tileCompress = false;
        this.cubeCnt = 0;
        this.cubePlaneNumber = 0;

        this.plotRequestTmp= null;
        this.rangeValues   = null;
    }

    /**
     *
     * @return {number}
     */
    getImageIdx() { return this.imageIdx; }


    /**
     *
     * @return {boolean}
     */
    isMultiImageFile() { return this.multiImageFile; }

    /**
     *
     */
    getCubePlaneNumber() { return this.cubePlaneNumber; }

    /**
     *
     * @return {number}
     */
    getCubeCnt() { return this.cubeCnt; }

    /**
     *
     * @return {number|*}
     */
    getOriginalImageIdx() { return this.originalImageIdx; }

    /**
     * get a copy of the WebPlotRequest for this BandState.  Any changes to the object will not be reflected in
     * BandState you must set it back in
     * @return {WebPlotRequest}
     */
    getWebPlotRequest() {
        if (!this.plotRequestTmp) this.plotRequestTmp= WebPlotRequest.parse(this.plotRequestSerialize);
        return this.plotRequestTmp;
    }

    hasRequest() { return this.plotRequestSerialize!==null; }


    /**
     *
     * @return {RangeValues}
     */
    getRangeValues() {
        if (!this.rangeValues) this.rangeValues= RangeValues.parse(this.rangeValuesSerialize);
        return this.rangeValues;
    }


   /**
     *
     * @return {ClientFitsHeader}
     */
    getHeader() { return this.fitsHeader; }

    /**
     *
     * @return {string}
     */
    getWorkingFitsFileStr() { return this.workingFitsFileStr; }


    /**
     *
     * @return {string}
     */
    getOriginalFitsFileStr() { return this.originalFitsFileStr; }



    /**
     *
     * @return {string}
     */
    getUploadedFileName() { return this.uploadFileNameStr; }

    static makeBandState() { return new BandState(); }

    /**
     * make a BandState from a pure json representation
     * @param {object} bsJson parsed json object
     * @return {BandState}
     */
    static makeBandStateWithJson(bsJson) {
        if (!bsJson) return null;
        const bState= BandState.makeBandState();
        bState.workingFitsFileStr= bsJson.workingFitsFileStr;
        bState.originalFitsFileStr= bsJson.originalFitsFileStr;
        bState.uploadFileNameStr= bsJson.uploadFileNameStr;
        bState.imageIdx= bsJson.imageIdx;
        bState.originalImageIdx= bsJson.originalImageIdx;
        bState.plotRequestSerialize= bsJson.plotRequestSerialize;
        bState.rangeValuesSerialize= bsJson.rangeValuesSerialize;
        bState.fitsHeader= makeClientFitsHeader(bsJson.fitsHeader);
        bState.bandVisible= bsJson.bandVisible;
        bState.multiImageFile= bsJson.multiImageFile;
        bState.tileCompress = bsJson.tileCompress;
        bState.cubeCnt= bsJson.cubeCnt;
        bState.cubePlaneNumber= bsJson.cubePlaneNumber;
        return bState;
    }

    /**
     * @param {BandState} bs
     */
    static convertToJSON(bs) {
        if (!bs || !bs.plotRequestSerialize) return null;
        const json= {};
        json.workingFitsFileStr= bs.workingFitsFileStr;
        json.originalFitsFileStr= bs.originalFitsFileStr;
        json.uploadFileNameStr= bs.uploadFileNameStr;
        json.imageIdx= bs.imageIdx;
        json.originalImageIdx= bs.originalImageIdx;
        json.plotRequestSerialize= bs.plotRequestSerialize;
        json.rangeValuesSerialize= bs.rangeValuesSerialize;
        json.fitsHeader= bs.fitsHeader.headers;
        json.bandVisible= bs.bandVisible;
        json.multiImageFile= bs.multiImageFile;
        json.tileCompress = bs.tileCompress;
        json.cubeCnt= bs.cubeCnt;
        json.cubePlaneNumber= bs.cubePlaneNumber;
        return json;

    }
}




export default BandState;
