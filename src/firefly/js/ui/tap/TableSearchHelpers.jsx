import HelpIcon from 'firefly/ui/HelpIcon';
import {isEqual} from 'lodash';
import PropTypes from 'prop-types';
import React from 'react';
import {getAppOptions} from '../../api/ApiUtil.js';
import {isDialogVisible} from '../../core/ComponentCntlr.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {
    convertISOToMJD, convertMJDToISO, DateTimePicker, fMoment, tryConvertToMoment, validateDateTime, validateMJD
} from '../DateTimePickerField.jsx';
import {FieldGroupCollapsible} from '../panel/CollapsiblePanel.jsx';
import {POPUP_DIALOG_ID, showOptionsPopup} from '../PopupUtil.jsx';
import {useFieldGroupValue} from '../SimpleComponent.jsx';
import {formFeedback, isShowHelp} from '../TimePanel.jsx';
import {ISO, MJD} from './TapUtil.js';
export const HeaderFont = {fontSize: 12, fontWeight: 'bold', alignItems: 'center'};

// Style Helpers
export const LeftInSearch = 24;
export const LabelWidth = 110;
export const LableSaptail = 110;
export const SpatialWidth = 440;
export const SpatialLableSaptail = LableSaptail + 45 /* padding of target */ - 4 /* padding of label */;
export const Width_Column = 175;
export const SmallFloatNumericWidth = 12;
export const Width_Time_Wrapper = Width_Column + 30;
export const SpatialPanelWidth = Math.max(Width_Time_Wrapper * 2, SpatialWidth) + LabelWidth + 10;
// field constants
export const  FROM = 0;
export const  TO  = 1;

const DEF_ERR_MSG= 'Constraints Error';


export const changeDatePickerOpenStatus = (loc, timeKey, currentValue, currentTimeMode, setTimeCallback) => {
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
                            inputStyle={{marginBottom: 3}} />);

        showOptionsPopup({content, title, modal: true, show});
    };
};

export const getTimeInfo = function(timeMode, value, valid, message){
    const updateValue = timeMode === MJD ? value : (valid ? fMoment(tryConvertToMoment(value, true)): value);
    const isoVal = timeMode === MJD ? convertMJDToISO(updateValue) : updateValue;
    const mjdVal = timeMode === ISO ? convertISOToMJD(updateValue) : updateValue;
    const isoValInfo = timeMode === MJD ? validateDateTime(isoVal) : {value: isoVal, valid, message};
    const mjdValInfo = timeMode === ISO ? validateMJD(mjdVal) : {value: mjdVal, valid, message};
    return {[ISO]: isoValInfo, [MJD]: mjdValInfo};
};


export const onChangeTimeMode = (newTimeMode, getter, setter) => {
    if (!getter(true)) return;
    const {value,valid,message, timeMode,...rest} = getter(true);
    const timeInfo = getTimeInfo(timeMode, value, valid, message);
    const newTimeInfo = timeInfo[newTimeMode];
    if (!newTimeInfo) return;

    const showHelp = isShowHelp(timeInfo[ISO].value, timeInfo[MJD].value);
    const feedback = formFeedback(timeInfo[ISO].value, timeInfo[MJD].value);

    setter (newTimeInfo.value, {valid:newTimeInfo.valid,
        ...rest, message: newTimeInfo.message, timeMode: newTimeMode, showHelp, feedback});
};

/**
 * make a FieldErrorList object
 * @returns {FieldErrorList}
 */
export const makeFieldErrorList = () => {
    const errors= [];
    const checkForError= ({valid=true, message=''}= {}) => !valid && errors.push(message);
    const addError= (message) => errors.push(message);
    const getErrors= () => [...errors];
    return {checkForError,addError,getErrors};
};

export const getPanelPrefix = (panelTitle) => {
    return panelTitle[0].toLowerCase() + panelTitle.substr(1);
};


function getPanelAdqlConstraint(panelActive, panelTitle,constraintsValid,adqlConstraintsAry,firstMessage, defErrorMessage=DEF_ERR_MSG) {
    if (!panelActive) return {adqlConstraint:'',adqlConstraintErrors:[]};

    if (constraintsValid && adqlConstraintsAry?.length) {
        return {adqlConstraint:adqlConstraintsAry.join(' AND '),adqlConstraintErrors:[]};
    } else {
        const msg= (!constraintsValid && firstMessage) ?
            `Error processing ${panelTitle} constraints: ${firstMessage}` : defErrorMessage;
        return {adqlConstraint:'',adqlConstraintErrors:[msg]};
    }
}

/**
 *
 * @param {boolean} panelActive
 * @param {String} panelTitle
 * @param {String} [defErrorMessage]
 * @returns {Function}
 */
export function makePanelStatusUpdater(panelActive,panelTitle,defErrorMessage) {
    /**
     * @Function
     * @param {InputConstraints} constraints
     * @param {ConstraintResult} lastConstraintResult
     * @param {Function} setConstraintResult - a function to set the constraint result setConstraintResult(ConstraintResult)
     * @String string - panel message
     */
    return (constraints, lastConstraintResult, setConstraintResult) => {
        const {valid:constraintsValid,errAry, adqlConstraintsAry, siaConstraints, siaConstraintErrors}= constraints;

        const simpleError= constraintsValid ? '' : (errAry[0]|| defErrorMessage || '');

        const {adqlConstraint, adqlConstraintErrors}=
            getPanelAdqlConstraint(panelActive,panelTitle, constraintsValid,adqlConstraintsAry,errAry[0], defErrorMessage);
        const cr = { adqlConstraint, adqlConstraintErrors, siaConstraints, siaConstraintErrors, simpleError};
        if (constrainResultDiffer(cr, lastConstraintResult)) setConstraintResult(cr);

        return simpleError;
    };
}


function constrainResultDiffer(c1, c2) {
    return (c1?.adqlConstraint !== c2?.adqlConstraint ||
        (c1.simpleError!==c2.simpleError) ||
        !isEqual(c1.adqlConstraintErrors, c2.adqlConstraintErrors) ||
        !isEqual(c1.siaConstraints, c2.siaConstraints) ||
        !isEqual(c1.siaConstraintErrors, c2.siaConstraintErrors));
}



export function Header({title, helpID='', checkID, message, enabled=false, panelValue=undefined}) {
    const tooltip = title + ' search is included in the query if checked';
    return (
        <div style={{display: 'inline-flex', alignItems: 'center'}} title={title + ' search'}>
            <div onClick={(e) => e.stopPropagation()} title={tooltip}>
                <CheckboxGroupInputField key={checkID} fieldKey={checkID}
                    initialState={{ value: enabled ? panelValue || title:'', label: '' }}
                    options={[{label:'', value: panelValue || title}]}
                    alignment='horizontal' wrapperStyle={{whiteSpace: 'norma'}} />
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
    message: PropTypes.string,
    panelValue: PropTypes.string,
    enabled: PropTypes.bool
};

export function InternalCollapsibleCheckHeader({title, helpID, children, fieldKey, checkKey, message, initialState, initialStateChecked, panelValue}) {

    return (
        <FieldGroupCollapsible header={<Header title={title} helpID={helpID}
                                               enabled={initialStateChecked}
                                               checkID={checkKey} message={message} panelValue={panelValue}/>}
                               initialState={initialState} fieldKey={fieldKey} headerStyle={HeaderFont}>
            {children}
        </FieldGroupCollapsible>

    );
}



export function makeCollapsibleCheckHeader(base) {
    const panelKey= base+'-panelKey';
    const panelCheckKey= base+'-panelCheckKey';
    const panelValue= base+'-panelEnabled';

    const retObj= {
            isPanelActive: () => false,
            setPanelActive: () => undefined,
            collapsibleCheckHeaderKeys:  [panelKey,panelCheckKey],
        };

    retObj.CollapsibleCheckHeader= ({title,helpID,message,initialStateOpen, initialStateChecked,children}) => {
        const [getPanelActive, setPanelActive] = useFieldGroupValue(panelCheckKey);// eslint-disable-line react-hooks/rules-of-hooks
        const isActive= getPanelActive() === panelValue;
        retObj.isPanelActive= () => getPanelActive() === panelValue;
        retObj.setPanelActive= (active) => setPanelActive(active ? panelValue : '');
        return (
            <InternalCollapsibleCheckHeader {...{title, helpID, checkKey:panelCheckKey, fieldKey:panelKey,
                                            message: isActive ? message:'', initialStateChecked, panelValue,
                                            initialState:{value: initialStateOpen ? 'open' : 'close'}}} >
                {children}
            </InternalCollapsibleCheckHeader>
        );
    };
    return retObj;
}






export function DebugObsCore({constraintResult}) {
    if (!getAppOptions().tapObsCore?.debug) return false;
    return (
        <div>
            adql fragment: {constraintResult?.adqlConstraint} <br/>
            sia fragment: {constraintResult?.siaConstraintErrors?.length ?
                           `Error: ${constraintResult?.siaConstraintErrors?.join(' ')}` :
                           constraintResult?.siaConstraints?.join('&')}
        </div> );
}




/**
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
    const isoRange = [convertMJDToISO(mjdRange[0]), convertMJDToISO(mjdRange[0])];
    const minGreaterThanMax = minValue && maxValue && (Number(minValue) > Number(maxValue));
    return {minValue, maxValue, mjdRange, isoRange, minGreaterThanMax};
}


/**
 * @typedef {Object} FieldErrorList
 * @prop {Function} addError - add a string error message, addError(string)
 * @prop {Function} checkForError - check and FieldGroupField for errors and add if found, checkForError(field)
 * @prop {Function} getErrors - return the arrays of errors, const errAry= errList.getErrors()
 */

/**
 * @typedef {Object} InputConstraints
 *
 * @prop {boolean} valid
 * @prop {Array.<String>} errAry
 * @props {Array.<String>} adqlConstraintsAry
 * @props {Array.<String>} siaConstraints
 * @props {Array.<String>} siaConstraintErrors
 */

/**
 * @typedef {Object} ConstraintResult
 *
 * @prop {String} adqlConstraint
 * @props {Array.<String>} adqlConstraintErrors
 * @props {Array.<String>} siaConstraints
 * @props {Array.<String>} siaConstraintErrors
 * @prop {String} simpleError
 *
 */