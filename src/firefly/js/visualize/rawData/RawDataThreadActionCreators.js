import {RawDataThreadActions} from '../../threadWorker/WorkerThreadActions.js';
import {getCmdSrvSyncURL, getRootURL} from '../../util/WebUtil.js';
import {isThreeColor} from '../PlotViewUtil.js';

/**
 *
 * @param {Object} obj
 * @param {WebPlot} obj.plot
 * @param obj.colorTableId
 * @param {number} obj.bias
 * @param {number} obj.contrast
 * @param obj.bandUse
 * @param {Array.<number>} obj.nanPixelColor
 * @param {String} obj.workerKey
 * @return {WorkerAction}
 */
export function makeColorAction({plot, colorTableId, bias, contrast, bandUse, nanPixelColor, workerKey}) {
    const {plotImageId, plotState} = plot;
    return {
        type: RawDataThreadActions.COLOR,
        workerKey,
        payload: {
            plotImageId,
            colorTableId,
            bias,
            contrast,
            nanPixelColor,
            ...bandUse,
            plotStateSerialized: plotState.toJson(true),
            threeColor: isThreeColor(plot),
            rootUrl: getRootURL()
        }
    };
}


/**
 * change mask color
 * @param obj
 * @param obj.plot
 * @param obj.maskColor
 * @param obj.workerKey
 * @return {WorkerAction}
 */
export function makeMaskColorAction({plot, maskColor, workerKey}) {
    const {plotImageId, plotState} = plot;
    return {
        type: RawDataThreadActions.MASK_COLOR,
        workerKey,
        payload: { plotImageId, maskColor, plotStateSerialized: plotState.toJson(true), rootUrl: getRootURL() }
    };
}



/**
 *
 * @param {String} plotImageId
 * @param {String} workerKey
 * @return {WorkerAction}
 */
export function makeAbortFetchAction(plotImageId, workerKey) {
    return {
        type: RawDataThreadActions.ABORT_FETCH,
        workerKey,
        payload: { plotImageId}
    };
}


/**
 *
 * @param {WebPlot} plot
 * @param plotState
 * @param {Object|undefined} maskOptions
 * @param {String} maskOptions.maskColor
 * @param {Number} maskOptions.maskBits
 * @param {String} dataCompress - should be 'FULL' or 'HALF' or 'QUARTER'
 * @param {boolean} veryLargeData
 * @param {String} workerKey
 * @return {WorkerAction}
 */
export function makeRetrieveStretchByteDataAction(plot, plotState, maskOptions, dataCompress, veryLargeData, workerKey) {
    const {plotImageId, colorTableId} = plot;
    const b = plot.plotState.firstBand();
    const {processHeader} = plot.rawData.bandData[b.value];
    const cleanProcessHeader = {...processHeader, imageCoordSys: processHeader.imageCoordSys.toString()};
    const threeColor = isThreeColor(plot);
    const {bias,contrast,nanPixelColor}= plot.rawData.bandData[0];
    const mask= Boolean(maskOptions);
    return {
        type: RawDataThreadActions.FETCH_STRETCH_BYTE_DATA,
        workerKey,
        payload: {
            plotImageId,
            dataCompress,
            veryLargeData,
            mask,
            maskBits: maskOptions?.maskBits,
            maskColor: maskOptions?.maskColor,
            dataWidth: plot.dataWidth,
            dataHeight: plot.dataHeight,
            plotStateSerialized: plotState.toJson(false),
            processHeader: cleanProcessHeader,
            colorTableId,
            bias,
            contrast,
            nanPixelColor,
            // cmdSrvUrl: getCmdSrvNoZipURL(),
            cmdSrvUrl: getCmdSrvSyncURL(),
            threeColor,
            rootUrl: getRootURL()
        }
    };
}

// /**
//  *
//  * @param {WebPlot} plot
//  * @param {Band} band
//  * @param {String} workerKey
//  * @param rvAry
//  * @return {WorkerAction}
//  */
// export function makeStretchAction(plot, band, workerKey, rvAry) {
//     const {plotImageId, plotState, dataWidth, dataHeight} = plot;
//     const b = plot.plotState.firstBand();
//     const {datamin, datamax, processHeader} = plot.rawData.bandData[b.value];
//     const cleanProcessHeader = {...processHeader, imageCoordSys: processHeader.imageCoordSys.toString()};
//     const threeColor = isThreeColor(plot);
//     let rvStrAry;
//     if (rvAry) {
//         rvStrAry = threeColor ?
//             allBandAry.map((b) => plotState.isBandUsed(b) ? rvAry[b.value]?.toString() : undefined) :
//             [rvAry[0].toString()];
//     } else {
//         rvStrAry = threeColor ?
//             allBandAry.map((b) => plotState.isBandUsed(b) ? plotState.getRangeValues(b).toString() : undefined) :
//             [plotState.getRangeValues().toString()];
//     }
//     return {
//         type: RawDataThreadActions.STRETCH,
//         workerKey,
//         payload: {
//             plotImageId,
//             plotStateSerialized: plotState.toJson(true),
//             dataWidth,
//             dataHeight,
//             datamin,
//             datamax,
//             band,
//             rvStrAry,
//             processHeader: cleanProcessHeader,
//             threeColor,
//             rootUrl: getRootURL()
//         }
//     };
// }

// /**
//  *
//  * @param {WebPlot} plot
//  * @param {ImagePt} ipt
//  * @param band
//  * @param {String} workerKey
//  * @return {WorkerAction}
//  */
// export function makeFluxDirectAction(plot, ipt, band, workerKey) {
//     return {
//         type: RawDataThreadActions.GET_FLUX,
//         workerKey,
//         payload: {
//             plotImageId: plot.plotImageId,
//             iptSerialized: ipt.toString(),
//             plotStateSerialized: plot.plotState.toJson(true),
//             band,
//         }};
// }
//
// /**
//  *
//  * @param {WebPlot} plot
//  * @param band
//  * @param {String} workerKey
//  * @return {WorkerAction}
//  */
// export function makeLoadAction(plot, band, workerKey) {
//     const {plotImageId, plotState} = plot;
//     const b = plot.plotState.firstBand();
//     const {processHeader} = plot.rawData.bandData[b.value];
//     const cleanProcessHeader = {...processHeader, imageCoordSys: processHeader.imageCoordSys.toString()};
//     const {bias,contrast}= plot.rawData.bandData[0].bias;
//     return {
//         type: RawDataThreadActions.FETCH_DATA,
//         workerKey,
//         payload: {
//             plotImageId,
//             plotStateSerialized: plotState.toJson(true),
//             band,
//             bias,
//             contrast,
//             processHeader: cleanProcessHeader,
//             cmdSrvUrl: getCmdSrvNoZipURL(),
//             rootUrl: getRootURL()
//         }
//     };
// }
//
