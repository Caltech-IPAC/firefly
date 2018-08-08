/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import WebPlotRequest from './WebPlotRequest.js';
import {RangeValues} from './RangeValues.js';

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
 * @prop directFileAccessData
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
        this.directFileAccessData= null;
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
    getHeader() { return this.directFileAccessData; }

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
        if (bsJson.uploadFileNameStr) bState.uploadFileNameStr= bsJson.uploadFileNameStr;
        bState.imageIdx= bsJson.imageIdx || 0;
        bState.originalImageIdx= bsJson.originalImageIdx || 0;
        bState.plotRequestSerialize= bsJson.plotRequestSerialize;
        bState.rangeValuesSerialize= bsJson.rangeValuesSerialize;
        bState.multiImageFile= Boolean(bsJson.multiImageFile);
        bState.tileCompress = Boolean(bsJson.tileCompress);
        bState.cubeCnt= bsJson.cubeCnt || 0;
        bState.cubePlaneNumber= bsJson.cubePlaneNumber || 0;
        return bState;
    }

    /**
     * @param {BandState} bs
     * @param {boolean} includeDirectAccessData include the clientFitsHeader object
     */
    static convertToJSON(bs, includeDirectAccessData= true) {
        if (!bs || !bs.plotRequestSerialize) return null;
        const json= {};
        json.workingFitsFileStr= bs.workingFitsFileStr;
        if (bs.workingFitsFileStr!==bs.originalFitsFileStr) json.originalFitsFileStr= bs.originalFitsFileStr;
        if (bs.uploadFileNameStr) json.uploadFileNameStr= bs.uploadFileNameStr;
        if (bs.imageIdx) json.imageIdx= bs.imageIdx;
        if (bs.originalImageIdx) json.originalImageIdx= bs.originalImageIdx;
        json.plotRequestSerialize= bs.plotRequestSerialize;
        json.rangeValuesSerialize= bs.rangeValuesSerialize;
        if (includeDirectAccessData) json.directFileAccessData= bs.directFileAccessData;
        if (bs.multiImageFile) json.multiImageFile= bs.multiImageFile;
        if (bs.tileCompress) json.tileCompress = bs.tileCompress;
        if (bs.cubeCnt) json.cubeCnt= bs.cubeCnt;
        if (bs.cubePlaneNumber) json.cubePlaneNumber= bs.cubePlaneNumber;
        return json;

    }
}




export default BandState;
