import {isNaN, isString} from 'lodash';
import Moment from 'moment';
import Validate from '../util/Validate.js';

export const MJD = 'mjd';
export const ISO = 'iso';
const DiffUnixMJD = 40587.0;
const DiffUnixJD = 2440587.5; // don't delete - might be used in the future
const DiffMJDJD = 2400000.5; // don't delete - might be used in the future
const msecPerDay = 86400000.0;
export const MIN_VAL_MJD = 1; // for ISO 1858-11-18T00:00:00Z
export const MAX_VAL_MJD = 2973483;  // for ISO 9999-12-31T00:00:00Z

/**
 * format moment in UTC mode or local mode
 * @param m a moment
 * @param utc
 * @return {String}
 */
export function formatMoment(m, utc = true) {
    if (!m?.isValid()) return '';
    return utc ? m.utc().format() : m.local().format();
}

/**
 * try to convert date/time string to Moment, if not, keep the string
 * @param {String|Object} str_or_moment
 * @returns {Object} a moment
 */
export function convertToMoment(str_or_moment) {
    return isString(str_or_moment) ? aMoment(str_or_moment) : str_or_moment;
}

/**
 * create a utc moment
 * @param str
 * @returns {Object} a moment
 */
export function aMoment(str) {
    const m = Moment.utc(str);
    if (!m.isValid()) return m;
    return Moment(m.format());
}

/**
 * compare times in Moment or string
 * @param time1
 * @param time2
 */
export function isSameTimes(time1, time2) {
    const m1= convertToMoment(time1);
    const m2= convertToMoment(time2);
    if (m1.isValid() !== m2.isValid()) return false;
    return (m1.isValid()) ? m1.utc() === m2.utc() : true;
}

export function convertMomentToMJD(m) {
    return m.isValid() ? `${(m.valueOf() / msecPerDay + DiffUnixMJD)}` : '';
}

/**
 * convert time in ISO format to MJD string
 * @param timeInISO
 * @returns {string}
 */
export function convertISOToMJD(timeInISO) {
    const m = convertToMoment(timeInISO);   // empty string => empty string of MJD
    return convertMomentToMJD(m);
}

/**
 * convert MJD string to ISO string
 * @param {String} mjdVal
 */
export function convertMJDToISO(mjdVal) {
    if (!mjdVal || isNaN(Number(mjdVal))) return '';
    const m = Moment((Number(mjdVal) - DiffUnixMJD) * msecPerDay);
    return formatMoment(m);
}

/**
 * validate time in moment or date-time string format
 * @param mVal
 * @param nullAllowed allow empty string
 * @returns {{valid: boolean, value: string, moment: Moment, message: string}}
 */
export function validateDateTime(mVal, nullAllowed = true) {
    const retval = {valid: true};

    retval.moment = convertToMoment(mVal);
    retval.valid = (typeof mVal === 'string' && !mVal && nullAllowed) ? true : retval.moment.isValid();
    retval.value = (typeof mVal === 'string') ? mVal : formatMoment(mVal);

    if (retval.valid) {
        if (retval.moment.isValid()) {
            // make sure the date is within the valid range
            // valid range is defined for MJD
            if (validateMJD(convertMomentToMJD(retval.moment))?.valid) {
                retval.unitT = retval.moment.valueOf();
            } else {
                retval.unitT = null;
                retval.valid = false;
                retval.message = `ISO date is out of range ${convertMJDToISO(MIN_VAL_MJD)} to ${convertMJDToISO(MAX_VAL_MJD)}`;
            }
        }
    } else {
        retval.unitT = null;
        retval.message = 'not a valid ISO value';
    }

    return retval;
}

/**
 * validate MJD range
 * @param tStr
 * @param nullAllowed allow empty string
 * @returns {{valid: boolean, value: *, message: string}}
 */
export function validateMJD(tStr, nullAllowed = true) {
    const retval = {valid: true, value: tStr};

    retval.valid = (!tStr && nullAllowed) ? true : Validate.floatRange(MIN_VAL_MJD, MAX_VAL_MJD, 6, 'Time range in MJD', tStr).valid;
    if (!retval.valid) {
        retval.message = `MJD value is not valid or out of range ${MIN_VAL_MJD} to ${MAX_VAL_MJD}`;
    }

    return retval;
}


export function getTimeInfo(timeMode, value, valid, message) {
    const updateValue = timeMode === MJD ? value : (valid ? formatMoment(convertToMoment(value)) : value);
    const isoVal = timeMode === MJD ? convertMJDToISO(updateValue) : updateValue;
    const mjdVal = timeMode === ISO ? convertISOToMJD(updateValue) : updateValue;
    const isoValInfo = timeMode === MJD ? validateDateTime(isoVal) : {value: isoVal, valid, message};
    const mjdValInfo = timeMode === ISO ? validateMJD(mjdVal) : {value: mjdVal, valid, message};
    return {[ISO]: isoValInfo, [MJD]: mjdValInfo};
}


export const isTimeUndefined = (utc, mjd) => !utc && !mjd;

/**
 * Return a converted FieldGroupField from one time mode to the other
 * @param newTimeMode
 * @param {FieldGroupField} timeField
 * @returns {FieldGroupField}
 */
export const changeTimeMode = (newTimeMode, timeField) => {
    if (!timeField) return;
    const {value, valid, message, timeMode, ...rest} = timeField;
    const timeInfo = getTimeInfo(timeMode, value, valid, message);
    const newTimeInfo = timeInfo[newTimeMode];
    if (!newTimeInfo) return;

    const showHelp = isTimeUndefined(timeInfo[ISO].value, timeInfo[MJD].value);
    const feedback = {UTC: timeInfo[ISO].value, MJD: timeInfo[MJD].value};

    return {
        ...rest, valid: newTimeInfo.valid, value: newTimeInfo.value, message: newTimeInfo.message,
        timeMode: newTimeMode, showHelp, feedback
    };
};

/**
 * Return the min and max of the exposure range, If min or max not specified then return undefined for that one. Result
 * object includes a boolean if min and greater than max
 * @param {FieldGroupField} timeMinField
 * @param {FieldGroupField} timeMaxField
 * @returns {{minValue: string, maxValue: string, minGreaterThanMax: boolean, isoRange: number[], mjdRange: String[]}}
 */
export function checkExposureTime(timeMinField, timeMaxField) {
    const {mjd: minMjd} = getTimeInfo(timeMinField.timeMode, timeMinField.value, timeMinField.valid, timeMinField.message);
    const {mjd: maxMjd} = getTimeInfo(timeMaxField.timeMode, timeMaxField.value, timeMaxField.valid, timeMaxField.message);
    const minValue = minMjd.value.length ? minMjd.value : '-Inf';
    const maxValue = maxMjd.value.length ? maxMjd.value : '+Inf';
    const mjdRange = [Number(minValue) > 0 ? Number(minValue) : NaN, Number(maxValue) > 0 ? Number(maxValue) : NaN];
    const isoRange = [convertMJDToISO(mjdRange[0]), convertMJDToISO(mjdRange[1])];
    const minGreaterThanMax = minValue && maxValue && (Number(minValue) > Number(maxValue));
    return {minValue, maxValue, mjdRange, isoRange, minGreaterThanMax,
        minHasValidValue:!isNaN(mjdRange[0]),
        maxHasValidValue:!isNaN(mjdRange[0])};
} // field constants