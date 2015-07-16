/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import WebPlotRequest from './WebPlotRequest.js';
import RangeValues from './RangeValues.js';
import MiniFitsHeader from './MiniFitsHeader.js';
import FileAndHeaderInfo from './FileAndHeaderInfo.js';
import join from 'underscore.string/join';
import toBoolean from 'underscore.string/toBoolean';
import validator from 'validator';
import {parseInt,checkNull} from '../util/StringUtils.js';

const SPLIT_TOKEN= '--BandState--';

class BandState {
    constructor() {
        this.workingFitsFileStr = null;
        this.originalFitsFileStr= null;
        this.uploadFileNameStr= null;
        this.imageIdx= 0;
        this.originalImageIdx= 0;

        this.plotRequestSerialize = null; // Serialized WebPlotRequest
        this.rangeValuesSerialize = null; // Serialized RangeValues
        this.fitsHeaderSerialize = null; // Serialized MiniFitsHeader
        this.bandVisible= true;
        this.multiImageFile = false;
        this.cubeCnt = 0;
        this.cubePlaneNumber = 0;

        this.plotRequestTmp= null;
        this.rangeValues   = null;
    }

    setImageIdx(idx) { this.imageIdx= idx; }

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
    setMultiImageFile(multiImageFile) { this.multiImageFile = multiImageFile; }

    getCubePlaneNumber() { return this.cubePlaneNumber; }
    setCubePlaneNumber(cubePlaneNumber) { this.cubePlaneNumber = cubePlaneNumber; }

    /**
     *
     * @return {number}
     */
    getCubeCnt() { return this.cubeCnt; }

    /**
     *
     * @param cubeCnt
     */
    setCubeCnt(cubeCnt) { this.cubeCnt = cubeCnt; }

    /**
     *
     * @param idx
     */
    setOriginalImageIdx(idx) { this.originalImageIdx= idx; }
    /**
     *
     * @return {number|*}
     */
    getOriginalImageIdx() { return this.originalImageIdx; }

    /**
     * this method will make a copy of WebPlotRequest. Any changes to the WebPlotRequest object
     * after the set will not be reflected here.
     * @param {WebPlotRequest} plotRequests copy this request
     */
    setWebPlotRequest(plotRequests) {
        this.plotRequestTmp = null;
        this.plotRequestSerialize = (plotRequests) ? null : plotRequests.toString();
    }

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
     * @param {boolean} visible
     */
    setBandVisible(visible) { this.bandVisible= visible; }

    /**
     *
     * @return {boolean}
     */
    isBandVisible() { return this.bandVisible; }


    /**
     *
     * @param {RangeValues} rangeValues
     */
    setRangeValues(rangeValues) {
        this.rangeValues= null;
        this.rangeValuesSerialize= (rangeValues==null) ? null : rangeValues.serialize();
    }

    /**
     *
     * @return {RangeValues}
     */
    getRangeValues() {
        if (this.rangeValues==null) this.rangeValues= RangeValues.parse(this.rangeValuesSerialize);
        return this.rangeValues;
    }

    /**
     * this method will make a copy of MiniFitsHeader. Any changes to the MiniFitsHeader object
     * after the set will not be reflected here.
     * @param {MiniFitsHeader} header
     */
    setFitsHeader(header) {
        this.fitsHeaderSerialize = header ? null : header.toString();
    }

   /**
     *
     * @return {MiniFitsHeader}
     */
    getHeader() { return MiniFitsHeader.parse(this.fitsHeaderSerialize); }

    /**
     *
     * @return {FileAndHeaderInfo}
     */
    getFileAndHeaderInfo() { return new FileAndHeaderInfo(this.workingFitsFileStr, this.fitsHeaderSerialize); }

    /**
     *
     * @return {string}
     */
    getWorkingFitsFileStr() { return this.workingFitsFileStr; }

    /**
     *
     * @param {string} fileStr
     */
    setWorkingFitsFileStr(fileStr) { this.workingFitsFileStr = fileStr; }

    /**
     *
     * @return {string}
     */
    getOriginalFitsFileStr() { return this.originalFitsFileStr; }

    /**
     *
     * @param {string} filetr
     */
    setOriginalFitsFileStr(fileStr) {this.originalFitsFileStr= fileStr; }


    /**
     *
     * @return {boolean}
     */
    isFileOriginal() { return this.originalFitsFileStr===this.workingFitsFileStr; }


    /**
     *
     * @param {string} uploadFile
     */
    setUploadedFileName(uploadFile) { this.uploadFileNameStr= uploadFile; }

    /**
     *
     * @return {string}
     */
    getUploadedFileName() { return this.uploadFileNameStr; }

    toString() {
        return join(SPLIT_TOKEN,
            this.workingFitsFileStr,
            this.originalFitsFileStr,
            this.uploadFileNameStr,
            this.imageIdx,
            this.originalImageIdx,
            this.plotRequestSerialize,
            this.rangeValuesSerialize,
            this.fitsHeaderSerialize,
            this.bandVisible,
            this.multiImageFile,
            this.cubeCnt,
            this.cubePlaneNumber);
    }

    serialize() { return this.toString(); }

    static parse(s) {
        if (!s) return null;
        var sAry= s.split(SPLIT_TOKEN,12);
        if (!sAry || sAry<12) return null;

        var i= 0;
        var workingFileStr=  checkNull(sAry[i++]);
        var originalFileStr= checkNull(sAry[i++]);
        var uploadFileStr=   checkNull(sAry[i++]);
        var imageIdx=        parseInt(sAry[i++],0);
        var originalImageIdx=parseInt(sAry[i++],0);
        var req=             WebPlotRequest.parse(sAry[i++]);
        var rv=              RangeValues.parse(sAry[i++]);
        var header=          MiniFitsHeader.parse(sAry[i++]);
        var bandVisible=     toBoolean(sAry[i++]);
        var multiImageFile=  toBoolean(sAry[i++]);
        var cubeCnt=         parseInt(sAry[i++],0);
        var cubePlaneNumber= parseInt(sAry[i++],0);
        var retval= null;
        if (req!=null && header!=null) {
            retval= new BandState();
            retval.setWorkingFitsFileStr(workingFileStr);
            retval.setOriginalFitsFileStr(originalFileStr);
            retval.setUploadedFileName(uploadFileStr);
            if (imageIdx) retval.setImageIdx(imageIdx);
            if (originalImageIdx)retval.setOriginalImageIdx(originalImageIdx);
            retval.setWebPlotRequest(req);
            retval.setRangeValues(rv);
            retval.setFitsHeader(header);
            retval.setBandVisible(bandVisible);
            retval.setMultiImageFile(multiImageFile);
            if (cubeCnt) retval.setCubeCnt(cubeCnt);
            if (cubePlaneNumber) retval.setCubePlaneNumber(cubePlaneNumber);
        }
        return retval;
    }

    equals(obj) {
        return (obj instanceof BandState) ? this.toString()===obj.toString() : false;
    }
}

