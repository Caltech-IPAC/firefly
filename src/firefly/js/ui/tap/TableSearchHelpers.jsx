import React from 'react';
import {isDialogVisible} from 'firefly/core/ComponentCntlr';
import {POPUP_DIALOG_ID, showOptionsPopup} from 'firefly/ui/PopupUtil';
import {get, set, has, isUndefined} from 'lodash';
import FieldGroupUtils from 'firefly/fieldGroup/FieldGroupUtils';
import {
    convertISOToMJD,
    convertMJDToISO, DateTimePicker,
    DateTimePickerField, fMoment, tryConvertToMoment,
    validateDateTime,
    validateMJD
} from 'firefly/ui/DateTimePickerField';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField';
import {HeaderFont, ISO, MJD} from 'firefly/ui/tap/TapUtil';
import HelpIcon from 'firefly/ui/HelpIcon';
import * as PropTypes from 'prop-types';
import {formFeedback, isShowHelp} from 'firefly/ui/TimePanel';

/* Style Helpers */
export const LeftInSearch = 24;
export const LabelWidth = 110;
export const LableSaptail = 110;
export const SpatialWidth = 440;
export const SpatialLableSaptail = LableSaptail + 45 /* padding of target */ - 4 /* padding of label */;
export const Width_Column = 175;
export const SmallFloatNumericWidth = 12;
export const Width_Time_Wrapper = Width_Column + 30;

export const  FROM = 0;
export const  TO  = 1;

export const skey = 'TABLE_SEARCH_METHODS';


const showTimePicker = (loc, show, timeKey) => {
    const pickerKey = timeKey + 'Picker';
    const title = loc === FROM ? 'select "from" time' : 'select "to" time';

    const fields = FieldGroupUtils.getGroupFields(skey);
    const {value/*, valid, message*/} = get(fields, pickerKey, {value: '', valid: true, message: ''});

    const content = (
        <DateTimePickerField fieldKey={pickerKey}
                             groupKey={skey}
                             showInput={false}
                             openPicker={true}
                             initialState={{
                                 value
                             }}
                             inputStyle={{marginBottom: 3}}
        />);

    showOptionsPopup({content, title, modal: true, show});
};

export const changeDatePickerOpenStatusNew = (loc, timeKey, currentValue, currentTimeMode, setTimeCallback) => {
    const currentTimeInfo = getTimeInfo(currentTimeMode, currentValue, true, '');
    const doSetTime = function(moment) {
        const timeInfo = getTimeInfo(ISO, fMoment(moment), true, '');
        setTimeCallback(timeInfo[currentTimeMode].value);
    };
    return () => {
        const show = !isDialogVisible(POPUP_DIALOG_ID);
        const title = loc === FROM ? 'select "from" time' : 'select "to" time';
        const content = (
            <DateTimePicker showInput={false}
                            openPicker={true}
                            value={currentTimeInfo[ISO].value}
                            onChange={doSetTime}
                            inputStyle={{marginBottom: 3}}
            />);

        showOptionsPopup({content, title, modal: true, show});
    };
};

// ExpsoureStartFrom
export function changeDatePickerOpenStatus(loc, timeKey) {
    return () => {
        const show = !isDialogVisible(POPUP_DIALOG_ID);

        showTimePicker(loc, show, timeKey);
    };
}

export const getTimeInfo = function(timeMode, value, valid, message){
    const updateValue = timeMode === MJD ? value : (valid ? fMoment(tryConvertToMoment(value, true)): value);
    const isoVal = timeMode === MJD ? convertMJDToISO(updateValue) : updateValue;
    const mjdVal = timeMode === ISO ? convertISOToMJD(updateValue) : updateValue;
    const isoValInfo = timeMode === MJD ? validateDateTime(isoVal) : {value: isoVal, valid, message};
    const mjdValInfo = timeMode === ISO ? validateMJD(mjdVal) : {value: mjdVal, valid, message};
    return {[ISO]: isoValInfo, [MJD]: mjdValInfo};
};

export const onChangeTimeMode = (newTimeMode, inFields, rFields, updateComponents) => {
    updateComponents.forEach((timeKey) => {
        const field = inFields[timeKey];
        const timeInfo = getTimeInfo(field.timeMode, field.value, field.valid, field.message);
        const newTimeInfo = timeInfo[newTimeMode];

        const showHelp = isShowHelp(timeInfo[ISO].value, timeInfo[MJD].value);
        const feedback = formFeedback(timeInfo[ISO].value, timeInfo[MJD].value);

        rFields[timeKey] = {
            ...inFields[timeKey],
            value: newTimeInfo.value,
            valid: newTimeInfo.valid,
            message: newTimeInfo.message,
            showHelp, feedback,
            timeMode: newTimeMode
        };
    });
};

export const onChangeTimeField = (value, inFields, rFields, timeKey, timeOptionsKey) => {
    // only update picker & mjd when there is no pop-up picker (time input -> picker or mjd)
    if (!isDialogVisible(POPUP_DIALOG_ID)) {
        const {valid, message} = inFields?.[timeKey] ?? {};
        const currentTimeMode = inFields?.[timeOptionsKey]?.value;
        const timeInfo = getTimeInfo(currentTimeMode, value, valid, message);
        const showHelp = isShowHelp(timeInfo[ISO].value, timeInfo[MJD].value);
        const feedback = formFeedback(timeInfo[ISO].value, timeInfo[MJD].value);
        rFields[timeKey] = {
            ...inFields[timeKey],
            value: timeInfo[currentTimeMode].value, message, valid, showHelp, feedback,
            [ISO]: timeInfo[ISO],
            [MJD]: timeInfo[MJD]
        };
    }
};

export const onChangeDateTimePicker = (value, inFields, rFields, timeKey, pickerKey, timeOptions) => {
    const currentTimeMode = timeOptions.value;
    // update MJD & TimeFrom (TimeTo) when there is pop-up picker (picker -> time field & mjd)
    if (isDialogVisible(POPUP_DIALOG_ID)) {
        const {valid, message} = get(inFields, pickerKey) || {};
        const timeInfo = getTimeInfo(ISO, value, valid, message);
        const showHelp = isShowHelp(timeInfo[ISO].value, timeInfo[MJD].value);
        const feedback = formFeedback(timeInfo[ISO].value, timeInfo[MJD].value);
        rFields[timeKey] = {
            ...inFields[timeKey],
            value: timeInfo[currentTimeMode].value, message, valid, showHelp, feedback, timeMode: currentTimeMode,
            [ISO]: timeInfo[ISO],
            [MJD]: timeInfo[MJD]
        };
    }
};

const updateMessage = (retval, field) => {
    if (field) {
        retval.message = `field '${field.label}': ${retval.message}`;
    }
    return retval;
};

const getFieldValidity = (fields, fieldKey, nullAllowed) => {
    const {valid=true, message, value, displayValue} = get(fields, fieldKey) || {};
    const val = displayValue || value;
    const rVal = val && (typeof val === 'string') ? val.trim() : val;

    // if nullAllowed is undefined, just pass valid & message as assigned
    if (isUndefined(nullAllowed) || rVal) {
        return {valid, message: (valid ? '' : (message || 'entry error'))};
    } else if (!rVal) {
        return {valid: nullAllowed, message: !nullAllowed ? 'empty entry' : ''};
    }
};

export const checkField = (key, opFields, nullAllowed, fieldsValidity) => {
    const retval = getFieldValidity(opFields, key, nullAllowed);
    const field = get(opFields, key);
    const validity = {valid: retval.valid, message: retval.message};
    if (!retval.valid) {
        updateMessage(retval, field);
    }
    if (has(field, 'nullAllowed')) {
        validity.nullAllowed = nullAllowed;
    }
    fieldsValidity.set(key, validity);
    return validity;
};

export const getPanelPrefix = (panelTitle) => {
    return panelTitle[0].toLowerCase() + panelTitle.substr(1);
};

export const isPanelChecked = (panelTitle, panelPrefix, fields) => {
    const panelCheckId = `${panelPrefix}Check`;
    return get(fields, [panelCheckId, 'value' ]) === panelTitle;
};

/*
 * Encapsulates the logic to inspect a panel's check, field validity,
 * and update everything accordingly.
 */
export const updatePanelFields = (fieldsValidity, valid, fields, newFields, panelTitle, panelPrefix, defaultMessage) => {
    const panelCheckId = `${panelPrefix}Check`;
    const panelFieldKey = `${panelPrefix}SearchPanel`;
    const firstMessage = Array.from(fieldsValidity.values()).find((v) => !v.valid)?.message ?? defaultMessage;
    if (newFields) {
        const panelActive = get(fields, [panelCheckId, 'value']) === panelTitle;
        const panelValid = get(fields, [panelFieldKey, 'panelValid'], false);
        for (const [key, validity] of fieldsValidity.entries()) {
            newFields[key].validity = validity;
            if (has(newFields[key], 'nullAllowed')) {
                newFields[key].nullAllowed = !panelActive;
                newFields[key].valid = panelActive ? validity.valid : true;
            }
        }

        Object.assign(newFields[panelFieldKey], {
            'panelValid': valid,
            'panelMessage': !valid ? firstMessage : '',
        });
        if (valid && !panelValid && [...fieldsValidity.keys()].length > 0) {
            set(newFields, [panelCheckId, 'value'], panelTitle);
        }
    }
};

export function Header({title, helpID='', checkID, message, enabled=false, panelValue=undefined}) {
    const tooltip = title + ' search is included in the query if checked';
    return (
        <div style={{display: 'inline-flex', alignItems: 'center'}} title={title + ' search'}>
            <div onClick={(e) => e.stopPropagation()} title={tooltip}>
                <CheckboxGroupInputField
                    key={checkID}
                    fieldKey={checkID}
                    initialState={{
                        value: enabled ? panelValue || title:'',
                        label: ''
                    }}
                    options={[{label:'', value: panelValue || title}]}
                    alignment='horizontal'
                    wrapperStyle={{whiteSpace: 'norma'}}
                />
            </div>
            <div style={{...HeaderFont, marginRight: 5}}>{title}</div>
            <HelpIcon helpId={helpID}/>
            <div style={{marginLeft: 10, color: 'saddlebrown', fontStyle: 'italic', fontWeight: 'normal'}}>{message}</div>
        </div>
    );
}

Header.propTypes = {
    title: PropTypes.string,
    helpID: PropTypes.string,
    checkID: PropTypes.string,
    message: PropTypes.string
};