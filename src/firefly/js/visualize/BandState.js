/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isString} from 'lodash';
import {WebPlotRequest} from './WebPlotRequest.js';
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
 * @prop plotRequest
 * @prop rangeValuesSerialize
 * @prop rangeValues
 * @prop directFileAccessData
 * @prop multiImageFile
 * @prop tileCompress
 * @prop cubeCnt
 * @prop cubePlaneNumber
 */

/**
 * @param {WebPlotRequest|String} plotRequest - the plot request or serialized version
 * @param {RangeValues|String} rangeValues - the range values or serialized version
 * @return {BandState}
 */
export function makeBandState(plotRequest, rangeValues) {
    return {
        workingFitsFileStr: undefined,
        originalFitsFileStr: undefined,
        uploadFileNameStr: undefined,
        imageIdx: 0,
        originalImageIdx: 0,
        directFileAccessData: undefined,
        multiImageFile: false,
        tileCompress: false,
        cubeCnt: 0,
        cubePlaneNumber: 0,
        plotRequest : isString(plotRequest) ? WebPlotRequest.parse(plotRequest) : plotRequest,
        rangeValues: isString(rangeValues) ? RangeValues.parse(rangeValues) : rangeValues,
    };
}


/**
 * make a BandState from a pure json representation
 * @param {object} bsJson parsed json object
 * @param {WebPlotRequest|String} overridePlotRequest -  a request or the serialized string version
 * @param {RangeValues|String} overrideRV -  a RangeValues or the serialized string version
 * @return {BandState}
 */
export function makeBandStateWithJson(bsJson, overridePlotRequest, overrideRV ) {
    if (!bsJson) return undefined;
    const bState= makeBandState(overridePlotRequest ?? bsJson.plotRequestSerialize, overrideRV ?? bsJson.rangeValuesSerialize);
    bState.workingFitsFileStr= bsJson.workingFitsFileStr;
    bState.originalFitsFileStr= bsJson.originalFitsFileStr;
    if (bsJson.uploadFileNameStr) bState.uploadFileNameStr= bsJson.uploadFileNameStr;
    bState.imageIdx= bsJson.imageIdx || 0;
    bState.originalImageIdx= bsJson.originalImageIdx || 0;
    bState.multiImageFile= Boolean(bsJson.multiImageFile);
    bState.tileCompress = Boolean(bsJson.tileCompress);
    bState.cubeCnt= bsJson.cubeCnt || 0;
    bState.cubePlaneNumber= bsJson.cubePlaneNumber || 0;
    bState.directFileAccessData= bsJson.directFileAccessData;
    return bState;
}

/**
 * @param {BandState|undefined|null} bs
 * @param {boolean} includeDirectAccessData include the directFileAccessData object
 */
export function convertBandStateToJSON(bs, includeDirectAccessData= true) {
    if (!bs || !bs.plotRequest) return undefined;
    const json= {};
    json.workingFitsFileStr= bs.workingFitsFileStr;
    if (bs.workingFitsFileStr!==bs.originalFitsFileStr) json.originalFitsFileStr= bs.originalFitsFileStr;
    if (bs.uploadFileNameStr) json.uploadFileNameStr= bs.uploadFileNameStr;
    if (bs.imageIdx) json.imageIdx= bs.imageIdx;
    if (bs.originalImageIdx) json.originalImageIdx= bs.originalImageIdx;
    json.plotRequestSerialize= bs.plotRequest.toStringServerSideOnly();


    json.rangeValuesSerialize= bs.rangeValues?.toJSON() ?? undefined;
    if (includeDirectAccessData) json.directFileAccessData= bs.directFileAccessData;
    if (bs.multiImageFile) json.multiImageFile= bs.multiImageFile;
    if (bs.tileCompress) json.tileCompress = bs.tileCompress;
    if (bs.cubeCnt) json.cubeCnt= bs.cubeCnt;
    if (bs.cubePlaneNumber) json.cubePlaneNumber= bs.cubePlaneNumber;
    return json;

}
