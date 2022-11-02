import {isUndefined} from 'lodash';
import React, {useContext, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {ColsShape, ColumnFld, getColValidator} from '../../charts/ui/ColumnOrExpression.jsx';
import {isDialogVisible} from '../../core/ComponentCntlr.js';
import {DateTimePickerField} from '../DateTimePickerField.jsx';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {POPUP_DIALOG_ID, showOptionsPopup} from '../PopupUtil.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {useFieldGroupRerender, useFieldGroupValue, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {formFeedback, isShowHelp, TimePanel} from '../TimePanel.jsx';
import {ConstraintContext} from './Constraints.js';
import {
    checkExposureTime, DebugObsCore, FROM, getPanelPrefix, getTimeInfo,
    LabelWidth, LeftInSearch, makeFieldErrorList,
    makePanelStatusUpdater, onChangeTimeMode, SpatialPanelWidth, TO, Width_Column, Width_Time_Wrapper,
    makeCollapsibleCheckHeader
} from './TableSearchHelpers.jsx';
import {getColumnAttribute, ISO, maybeQuote, MJD, tapHelpId} from './TapUtil.js';

const Temporal = 'Temporal';
const TemporalColumns = 'temporalColumns';
const TimeFrom = 'timeFrom';
const TimeTo = 'timeTo';
const TimePickerFrom = 'timePickerFrom';
const TimePickerTo = 'timePickerTo';
const TimeOptions = 'timeOptions';


const title = Temporal;
const panelValue = title;
const panelPrefix = getPanelPrefix(title);
const timeOptions = [{label: 'ISO', value: ISO}, {label: 'MJD', value: MJD}];
const timeOptionsTip='Choose between:\nISO 8601 time format (e.g., 2021-03-20)\nor\nModified Julian Date time format (e.g., 59293.1)';


function changeDatePickerOpenStatus(loc, value,  groupKey) {
    return () => {
        const show = !isDialogVisible(POPUP_DIALOG_ID);
        const title = loc === FROM ? 'select "from" time' : 'select "to" time';
        const pickerKey= loc===FROM ? TimePickerFrom : TimePickerTo;
        const content = (
            <DateTimePickerField fieldKey={pickerKey} groupKey={groupKey} showInput={false} openPicker={true}
                                 initialState={{ value }} inputStyle={{marginBottom: 3}} />);

        showOptionsPopup({content, title, modal: true, show});
    };
}

function onChangeDateTimePicker(pickerField, fieldSetter, timeMode)  {
    if (!timeMode) return;
    const {value, valid, message} = pickerField;
    if (isUndefined(value)) return;
    const timeInfo = getTimeInfo(ISO, value, valid, message);
    const showHelp = isShowHelp(timeInfo[ISO].value, timeInfo[MJD].value);
    const feedback = formFeedback(timeInfo[ISO].value, timeInfo[MJD].value);

    fieldSetter({
        value: timeInfo[timeMode].value, message, valid, showHelp, feedback, timeMode,
        [ISO]: timeInfo[ISO], [MJD]: timeInfo[MJD]
    } );
}


function makeTemporalConstraints(columnsModel, fldObj) {
    let adqlConstraint = '';
    const errList= makeFieldErrorList();
    const {[TimeFrom]:timeFromField, [TimeTo]:timeToField, [TemporalColumns]:temporalColumnsField}= fldObj;

    if (timeFromField?.valid && timeToField?.valid && temporalColumnsField?.valid){
        const timeColumns = temporalColumnsField?.value?.split(',').map( (c) => c.trim()) ?? [];

        const {mjdRange, isoRange}=  checkExposureTime(timeFromField,timeToField);

        if (timeColumns.length === 1) {
            const timeColumn = timeColumns[0];

            // use MJD if time column has a numeric type, ISO otherwise
            const datatype = getColumnAttribute(columnsModel, timeColumn, 'datatype')?.toLowerCase() ?? '';
            // ISO types: char, varchar, timestamp, ?
            const useISO = !['double', 'float', 'real', 'int', 'long', 'short'].some((e) => datatype.includes(e));
            let timeRange = mjdRange;
            if (useISO) {
                // timeRange = mjdRange.map((value) => convertMJDToISO(value));
                timeRange = isoRange;
            }

            const timeConstraints = timeRange.map((oneRange, idx) => {
                if (!oneRange) return '';
                const limit  = useISO ? `'${oneRange}'` : oneRange;
                return idx === FROM ? `${maybeQuote(timeColumn)} >= ${limit}` : `${maybeQuote(timeColumn)} <= ${limit}`;
            });

            if (!timeConstraints[FROM] || !timeConstraints[TO]) {
                adqlConstraint = timeConstraints[FROM] + timeConstraints[TO];
            } else {
                adqlConstraint =`(${timeConstraints[FROM]} AND ${timeConstraints[TO]})`;
            }
        }
    }

    if (!timeFromField.value && !timeToField.value) errList.addError('Time is Required');
    errList.checkForError(timeFromField);
    errList.checkForError(timeToField);
    errList.checkForError(temporalColumnsField);
    const errAry= errList.getErrors();
    return { valid: errAry.length===0, errAry, adqlConstraintsAry:[adqlConstraint],
        siaConstraints:[], siaConstraintErrors:[]};
}

const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(Temporal));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;

const fldAry=[TimeTo,TimeFrom,TemporalColumns,TimePickerFrom,TimePickerTo, TimeOptions ];

/**
 *
 * @param props
 * @param props.cols
 * @param props.columnsModel
 * @returns {JSX.Element}
 */
export function TemporalSearch({cols, columnsModel}) {
    const [constraintResult, setConstraintResult] = useState({});

    const {getFld,setFld,getVal,makeFldObj,groupKey}= useContext(FieldGroupCtx);
    const {setConstraintFragment}= useContext(ConstraintContext);
    const [getTimeFrom,setTimeFrom]= useFieldGroupValue(TimeFrom);
    const [getTimeTo,setTimeTo]= useFieldGroupValue(TimeTo);
    useFieldGroupRerender([...fldAry, ...collapsibleCheckHeaderKeys]); // force rerender on any change

    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), panelValue, 'Error in temporal constraints');


    useFieldGroupWatch([TimeOptions],
        ([timeOp]) => {
            onChangeTimeMode(timeOp, getTimeTo, setTimeTo);
            onChangeTimeMode(timeOp, getTimeFrom, setTimeFrom);
        });

    useFieldGroupWatch([TimePickerFrom],
        ([tpFVal],isInit) => {
            const timeOp= getVal(TimeOptions);
            if (!timeOp || isInit) return;
            onChangeDateTimePicker(getFld(TimePickerFrom), (f)=> setFld(TimeFrom,f), timeOp);
            if (tpFVal && !isInit) checkHeaderCtl.setPanelActive(true);
        }
    );

    useFieldGroupWatch([TimePickerTo],
        ([tpTVal],isInit) => {
            const timeOp= getVal(TimeOptions);
            if (!timeOp || isInit) return;
            onChangeDateTimePicker(getFld(TimePickerTo), (f)=> setFld(TimeTo,f), timeOp);
            if (tpTVal) checkHeaderCtl.setPanelActive(true);
        }
    );

    useEffect(() => {
        if (!checkHeaderCtl.isPanelActive()) return;
        const {minGreaterThanMax}=  checkExposureTime(getTimeFrom(true),getTimeTo(true));
        if (minGreaterThanMax) setTimeTo(getTimeTo(),{valid: false, message: 'the start time is greater than the end time'});
    },[getTimeFrom,getTimeTo,checkHeaderCtl.isPanelActive()]);

    useFieldGroupWatch([TemporalColumns],
        ([tcVal],isInit) => {
            if (!tcVal) return;
            const timeColumns = tcVal.split(',').map( (c) => c.trim()) ?? [];
            if (timeColumns.length === 1) {
                if (!isInit) checkHeaderCtl.setPanelActive(true);
            }
            else {
                setFld(TemporalColumns, {value:tcVal, valid:false, message: 'you may only choose one column'});
            }
        });

    useEffect(() => {
        const constraints= makeTemporalConstraints(columnsModel, makeFldObj([TimeFrom,TimeTo,TemporalColumns]));
        updatePanelStatus(constraints, constraintResult, setConstraintResult);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);

    const temporalColumns = (
            <div style={{marginLeft: LeftInSearch}}>
                <ColumnFld fieldKey={TemporalColumns} cols={cols}
                           name='temporal column' // label that appears in column chooser
                           inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                           label='Temporal Column' tooltip={'Column for temporal search'}
                           labelWidth={LabelWidth}
                           validator={getColValidator(cols, true, false)}
                />
            </div>
        );

    const timeRange= (//  radio field is styled with padding right in consistent with the label part of 'temporal columns' entry
            <div style={{display: 'flex', marginLeft: LeftInSearch, marginTop: 5, width: SpatialPanelWidth}}>
                <RadioGroupInputField fieldKey={TimeOptions} options={timeOptions} alignment='horizontal'
                                      wrapperStyle={{width: LabelWidth, paddingRight:'4px'}}
                                      initialState={{ value: ISO }}
                                      tooltip={timeOptionsTip}
                />
                <div style={{width: Width_Time_Wrapper}}>
                    <TimePanel fieldKey={TimeFrom} timeMode={getVal(TimeOptions)} icon='calendar'
                               onClickIcon={changeDatePickerOpenStatus(FROM, getTimeFrom(), groupKey)}
                               feedbackStyle={{height: 100}} inputWidth={Width_Column}
                               inputStyle={{overflow:'auto', height:16}}
                               tooltip="'from' time"

                    />
                </div>
                <div style={{width: Width_Time_Wrapper}}>
                    <TimePanel fieldKey={TimeTo} timeMode={getVal(TimeOptions)} icon='calendar'
                               onClickIcon={changeDatePickerOpenStatus(TO, getTimeTo(), groupKey)}
                               feedbackStyle={{height: 100}} inputWidth={Width_Column}
                               inputStyle={{overflow:'auto', height:16}}
                               tooltip="'to' time"
                    />
                </div>
            </div>
        );

    return (

        <CollapsibleCheckHeader title={title} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} initialStateOpen={false}>
            <div style={{marginTop: 5, height: 100}}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    {temporalColumns}
                    {timeRange}
                </ForceFieldGroupValid>
            </div>
            <DebugObsCore {...{constraintResult}}/>
        </CollapsibleCheckHeader>
    );
}

TemporalSearch.propTypes = {
    cols: ColsShape,
    columnsModel: PropTypes.object,
};
