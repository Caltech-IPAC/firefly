/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isNil} from 'lodash';
import {TitleOptions} from '../../visualize/WebPlotRequest.js';
import {logError} from '../../util/WebUtil.js';
import {getWebPlotRequestViaWISEIbe} from './wise/WisePlotRequests.js';
import {makeLsstSdssPlotRequest} from './lsst_sdss/LsstSdssPlotRequests.js';
import {makeURLPlotRequest} from './generic/DefaultPlotRequests.js';
import {basicURLPlotRequest} from './basic/BasicPlotRequests';
import {LsstSdssSettingBox, lsstSdssOnNewRawTable, lsstSdssOnFieldUpdate, lsstSdssRawTableRequest} from './lsst_sdss/LsstSdssMissionOptions.js';
import {WiseSettingBox, wiseOnNewRawTable, wiseOnFieldUpdate, wiseRawTableRequest, isBasicTableUploadValid} from './wise/WiseMissionOptions.js';
import {DefaultSettingBox, defaultOnNewRawTable, defaultOnFieldUpdate, defaultRawTableRequest} from './generic/DefaultMissionOptions.js';
import {BasicSettingBox, basicOnNewRawTable, basicOnFieldUpdate, basicRawTableRequest, imagesShouldBeDisplayed} from './basic/BasicMissionOptions.js';
import {LC} from './LcManager.js';

export const UNKNOWN_MISSION = 'generic';
export const COORD_SYSTEM_OPTIONS = ['EQ_J2000', 'EQ_B1950', 'GALACTIC'];
export const coordSysOptions = 'coordSysOptions';
/**
 * A function to create a WebPlotRequest from the given parameters
 * @callback WebplotRequestCreator
 * @param {TableModel} tableModel
 * @param {number} hlrow
 * @param {number} cutoutSize
 * @param {Object} params - mission specific parameters
 */

/**
 * @callback onNewRawTable
 * @param {object} rawTable
 * @param {LCMissionConverter} converterData
 * @param {Object} generalEntries
 * @return {Object} new missionEntries object
 */

/**
 * @callback onFieldUpdate
 * @param {string} fieldKey
 * @param {string} newValue
 * @return {Object} object with the mission entries keys that should update as a result of this change
 */

/**
 * @callback rawTableRequest
 * @param {LCMissionConverter} converterData
 * @param {string} uploaded file location
 * @return {TableRequest} table request object
 */

/**
 * @typedef {Object} LCMissionConverter - defines how to get LC data frm a mission specific table
 * @prop {string}  converterId - unique identifier for this object
 * @prop {number}  defaultImageCount - default number of images connected to a ResultView row
 * @prop {string}  defaultTimeCName - default time column in ResultView table
 * @prop {string}  defaultYCName - default Y column in ResultView table
 * @prop {string}  defaultYErrCName - default Y error column in ResultView table
 * @prop {string}  missionName - displayable mission name
 * @prop {Component} MissionOptions - React component for result page options
 * @prop {function} onNewRawTable - function called when new raw table is loaded, returns new mission entries object
 * @prop {function} onFieldUpdate - function that is called when a value of a field changes
 * @prop {function} rawTableRequest - function that creates raw table request
 * @prop {function} setFields
 * @prop {string[]} timeNames - valid column names for time axis
 * @prop {string[]} yNames - valid column names for y axis
 * @prop {WebplotRequestCreator}  webplotRequestCreator a function to create a WebplotRequest
 * @prop {boolean} shouldImagesBeDisplayed a function that return true if user inputs make sense to the mission
 *                  to show images
 * @prop {function} isTableUploadValid - check table on upload if make sens with mission selected
 */

/**
 * @type {Object<string, LCMissionConverter>}
 **/
const converters = {
    'wise': {
        converterId: 'wise',
        defaultImageCount: 5,
        defaultTimeCName: 'mjd',
        defaultYCname: 'w1mpro_ep',
        defaultYErrCname: '',
        missionName: 'WISE/NEOWISE',
        MissionOptions: WiseSettingBox,
        onNewRawTable: wiseOnNewRawTable,
        onFieldUpdate: wiseOnFieldUpdate,
        rawTableRequest: wiseRawTableRequest,
        /*timeNames: ['mjd'],*/
        /*yNames: ['w1mpro_ep', 'w2mpro_ep', 'w3mpro_ep', 'w4mpro_ep'],*/
        yErrNames: '',
        dataSource: 'frame_id',
        webplotRequestCreator: getWebPlotRequestViaWISEIbe,
        shouldImagesBeDisplayed: () => {return true;},
        isTableUploadValid: isBasicTableUploadValid,
        yNamesChangeImage: [],
        showPlotTitle:getPlotTitle
    },
    'lsst_sdss': {
        converterId: 'lsst_sdss',
        defaultImageCount: 3,
        defaultTimeCName: 'exposure_time_mid',
        defaultYCname: 'mag',
        defaultYErrCname: '',
        missionName: 'LSST SDSS',
        MissionOptions: LsstSdssSettingBox,
        onNewRawTable: lsstSdssOnNewRawTable,
        onFieldUpdate: lsstSdssOnFieldUpdate,
        rawTableRequest: lsstSdssRawTableRequest,
        timeNames: ['exposure_time_mid'],
        yNames: ['mag', 'tsv_flux'],
        yErrNames: ['magErr', 'tsv_fluxErr'],
        webplotRequestCreator: makeLsstSdssPlotRequest,
        shouldImagesBeDisplayed: () => {return true;},
        isTableUploadValid: () => {return true;},
        yNamesChangeImage: []
    },
    [UNKNOWN_MISSION]: {
        converterId: UNKNOWN_MISSION,
        defaultImageCount: 3,
        defaultTimeCName: '',
        defaultYCname: '',
        defaultYErrCname: '',
        missionName: '',
        defaultCoordX: '',
        defaultCoordY: '',
        defaultCoordSys: '',
        MissionOptions: DefaultSettingBox,
        onNewRawTable: defaultOnNewRawTable,
        onFieldUpdate: defaultOnFieldUpdate,
        rawTableRequest: defaultRawTableRequest,
        dataSource: '',
        [coordSysOptions]: COORD_SYSTEM_OPTIONS,
        webplotRequestCreator: makeURLPlotRequest,
        shouldImagesBeDisplayed: () => {return true;},
        isTableUploadValid: () => {return true;},
        yNamesChangeImage: [],     // TODO: y columns which will affect the image display
        noImageCutout: true        // no image cutout is used
    },
    // Case which should handle any ipac table and image column, X,Y, coord system not shown (Generic/Advanced case)
    'other': {
        converterId: 'other',
        defaultImageCount: 5,
        defaultTimeCName: '',
        defaultYCname: '',
        defaultYErrCname: '',
        missionName: '',
        defaultCoordX: '',
        defaultCoordY: '',
        defaultCoordSys: '',
        MissionOptions: BasicSettingBox,
        onNewRawTable: basicOnNewRawTable,
        onFieldUpdate: basicOnFieldUpdate,
        rawTableRequest: basicRawTableRequest,
        dataSource: '',
        [coordSysOptions]: COORD_SYSTEM_OPTIONS,
        webplotRequestCreator: basicURLPlotRequest,
        shouldImagesBeDisplayed: imagesShouldBeDisplayed,
        isTableUploadValid: () => {return true;},
        yNamesChangeImage: [],
        showPlotTitle:getPlotTitle
    }
};

function getPlotTitle(tableId){
    switch (tableId){
        case LC.RAW_TABLE:
            return 'Input Data';
        case LC.PHASE_FOLDED:
            return 'Phase Folded Data';
        case LC.PEAK_TABLE:
            return 'Peak Data';
        case LC.PERIODOGRAM_TABLE:
            return 'Periodogram Data';
        default:
            return '';
    }
}
export function getAllConverterIds() {
    return Object.keys(converters);
}

export function getConverter(converterId = UNKNOWN_MISSION) {
    const converter = converters[converterId];
    if (!converter) {
        logError(`Unable to find dataset converter ${converterId}`);
        return;
    }
    return converter;
}

export function getMissionName(converterId) {
    const converterData = converterId && converters[converterId];
    return get(converterData, 'missionName', converterId);
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
    if(!isNil(wp)){
        retWpr.setOverlayPosition(wp);
    }
    return retWpr;
}
