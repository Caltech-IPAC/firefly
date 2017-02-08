/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {TitleOptions} from '../../visualize/WebPlotRequest.js';
import {logError} from '../../util/WebUtil.js';

import {getWebPlotRequestViaWISEIbe} from './wise/WisePlotRequests.js';  //WiseRequestList.js
import {makeLsstSdssPlotRequest} from './lsst_sdss/LsstSdssPlotRequests.js'; //LsstSdssRequestList.js';



/**
 * A function to create a WebPlotRequest from the given parameters
 * @callback WebplotRequestCreator
 * @param {TableModel} tableModel
 * @param {number} hlrow
 * @param {number} cutoutSize
 * @param {Object} params - mission specific parameters
 */


/**
 * @typedef {Object} LCMissionConverter - defines how to get LC data frm a mission specific table
 * @prop {number}  defaultImageCount - default number of images connected to a ResultView row
 * @prop {string}  defaultTimeCName - default time column in ResultView table
 * @prop {string}  defaultYCName - default Y column in ResultView table
 * @prop {string[]}  timeNames - valid column names for time axis
 * @prop {string[]}  yNames - valid column names for y axis
 * @prop {WebplotRequestCreator}  webplotRequestCreator a function to create a WebplotRequest
 */

/**
 * @type {{wise: LCMissionConverter, lsst_sdss: LCMissionConverter}}
 **/
export const converters = {
    'wise': {
        defaultImageCount: 5,
        defaultTimeCName: 'mjd',
        defaultYCname: 'w1mpro_ep',
        defaultYErrCname: '',
        timeNames: ['mjd'],
        yNames: ['w1mpro_ep', 'w2mpro_ep', 'w3mpro_ep', 'w4mpro_ep'],
        yErrNames: ['w1sigmpro_ep', 'w2sigmpro_ep', 'w3sigmpro_ep', 'w4sigmpro_ep'],
        webplotRequestCreator: getWebPlotRequestViaWISEIbe
    },
    'lsst_sdss': {
        defaultImageCount: 1,
        defaultTimeCName: 'exposure_time_mid',
        defaultYCname: 'mag',
        defaultYErrCname: '',
        timeNames: ['exposure_time_mid'],
        yNames: ['mag', 'tsv_flux'],
        yErrNames: ['magErr', 'tsv_fluxErr'],
        webplotRequestCreator: makeLsstSdssPlotRequest
    }
};

export function getAllConverterIds() {
    return Object.keys(converters);
}

export function getConverter(datasetConverterId) {
    const converter = datasetConverterId && converters[datasetConverterId];
    if (!converter) {
        logError(`Unable to find dataset converter ${datasetConverterId}`);
        return;
    }
    return converter;
}


/**
 * @param {WebPlotRequest} inWpr
 * @param {string} title
 * @param {WorldPt} wp
 * @returns {WebPlotRequest}
 */
export function addCommonReqParams(inWpr,title,wp) {
    const retWpr = inWpr.makeCopy();
    retWpr.setTitle(title);
    retWpr.setTitleOptions(TitleOptions.NONE);
    retWpr.setGroupLocked(true);
    retWpr.setPlotGroupId('LightCurveGroup');
    retWpr.setPreferenceColorKey('light-curve-color-pref');
    retWpr.setOverlayPosition(wp);
    return retWpr;
}
