import PropTypes from 'prop-types';
import React, {useContext, useEffect, useState} from 'react';
import {isDialogVisible} from '../../core/ComponentCntlr.js';
import {maximumPositiveFloatValidator, minimumPositiveFloatValidator} from '../../util/Validate.js';
import {convertISOToMJD} from '../DateTimePickerField.jsx';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {POPUP_DIALOG_ID} from '../PopupUtil.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {useFieldGroupRerender, useFieldGroupValue, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {formFeedback, isShowHelp, TimePanel} from '../TimePanel.jsx';
import {ValidationField} from '../ValidationField.jsx';
import {makeAdqlQueryRangeFragment, ConstraintContext, siaQueryRange} from './Constraints.js';
import {
    changeDatePickerOpenStatus, checkExposureTime, DebugObsCore, FROM, getPanelPrefix, getTimeInfo,
    LabelWidth, LableSaptail, LeftInSearch, makeCollapsibleCheckHeader, makeFieldErrorList, makePanelStatusUpdater,
    onChangeTimeMode,
    SmallFloatNumericWidth, SpatialLableSaptail, TO, Width_Column, Width_Time_Wrapper
} from './TableSearchHelpers.jsx';
import {ISO, MJD, tapHelpId} from './TapUtil.js';

const START_TIME_GREATER_MSG= 'the start time is greater than the end time';
const START_EXP_GREATER_MSG= 'exposure time max must be greater than time min';
const ONE_POPULATED= 'at least one field must be populated';

const panelTitle = 'Timing';
const panelValue = 'Exposure';
const panelPrefix = getPanelPrefix(panelValue);
const exposureRangeOptions = [
    {label: 'Completed in the Last...', value: 'since' },
    {label: 'Overlapping specified range', value: 'range'}
];


function getSinceMill(sinceVal, ops) {
    const v = parseFloat(sinceVal);
    switch (ops) {
        case 'minutes': return v * 60 * 1000;
        case 'hours': return v * 60 * 60 * 1000;
        case 'days': return v * 24 * 60 * 60 * 1000;
        case 'years': return v * 365 * 24 * 60 * 60 * 1000;
        default: return 0;
    }
}

const onChangeDateTimePicker = (getter, setter) => {
    if (isDialogVisible(POPUP_DIALOG_ID)) {
        const {valid, message, value, timeMode, ...rest} = getter(true) || {};
        const timeInfo = getTimeInfo(timeMode, value, valid, message);
        const showHelp = isShowHelp(timeInfo[ISO].value, timeInfo[MJD].value);
        const feedback = formFeedback(timeInfo[ISO].value, timeInfo[MJD].value);

        setter(timeInfo[timeMode].value,
            {...rest, valid,
                message, showHelp, feedback, timeMode, [ISO]: timeInfo[ISO], [MJD]: timeInfo[MJD]
            }
        );
    }
};

function checkSinceTimeInMjd(sinceVal, sinceOp) {
    const sinceMillis = getSinceMill(parseFloat(sinceVal), sinceOp);
    const sinceString = new Date(Date.now() - sinceMillis).toISOString();
    return convertISOToMJD(sinceString);
}


function checkExposureDuration(expLenMin, expLenMax) {
    const minValue = !expLenMin?.length ? '-Inf' : expLenMin ?? '-Inf';
    const maxValue = !expLenMax?.length ? '+Inf' : expLenMax ?? '+Inf';
    const minGreaterThanMax= !minValue.endsWith('Inf') && !maxValue.endsWith('Inf') && Number(minValue) > Number(maxValue);
    return {minValue, maxValue,rangeList:[[minValue, maxValue]],minGreaterThanMax};
}

/**
 * @param rangeType
 * @param fldObj
 * @returns {InputConstraints}
 */
function makeExposureConstraints(rangeType, fldObj) {
    const errList= makeFieldErrorList();
    const siaConstraints= [];
    const siaConstraintErrors= [];
    const adqlConstraintsAry = [];

    const {exposureSinceValue:expSince,exposureLengthMin:expLenMinField, exposureLengthMax:expLenMaxField,
        exposureMin, exposureMax, exposureSinceOptions}= fldObj;

    const expLenMin = expLenMinField?.value;
    const expLenMax = expLenMaxField?.value;

    let seenValue = false;
    if (rangeType === 'range') {
        if (exposureMin?.value || exposureMax?.value) {
            const {minValue, maxValue}=  checkExposureTime(exposureMin,exposureMax);
            errList.checkForError(exposureMin);
            errList.checkForError(exposureMax);
            if (exposureMin?.valid && exposureMax?.valid) {
                const rangeList = [[minValue, maxValue]];
                adqlConstraintsAry.push(makeAdqlQueryRangeFragment('t_min', 't_max', rangeList, false));
                siaConstraints.push(...siaQueryRange('TIME', rangeList));
            }
            seenValue = true;
        }
    } else if (rangeType === 'since' && !isNaN(parseFloat(expSince?.value))) {
        errList.checkForError(expSince);
        if (expSince?.valid) {
            const rangeList = [[`${checkSinceTimeInMjd(expSince?.value, exposureSinceOptions?.value)}`, '+Inf']];
            adqlConstraintsAry.push(makeAdqlQueryRangeFragment('t_min', 't_max', rangeList));
            siaConstraints.push(...siaQueryRange('TIME', rangeList));
        }
        seenValue = true;
    }
    if (expLenMin || expLenMax) {
        const {rangeList, minGreaterThanMax}= checkExposureDuration(expLenMin,expLenMax);
        if (!minGreaterThanMax) {
            adqlConstraintsAry.push(makeAdqlQueryRangeFragment('t_exptime', 't_exptime', rangeList, true));
            siaConstraints.push(...siaQueryRange('EXPTIME', rangeList));
        }
        seenValue = true;
        errList.checkForError(expLenMinField);
        errList.checkForError(expLenMaxField);
    }
    if (!seenValue) errList.addError(ONE_POPULATED);

    const errAry= errList.getErrors();
    return { valid: errAry.length===0 && seenValue, errAry, adqlConstraintsAry, siaConstraints, siaConstraintErrors };

}


const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(panelValue));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;

const fldListAry= ['exposureSinceValue', 'exposureLengthMin', 'exposureLengthMax',
            'exposureMin', 'exposureMax', 'exposureSinceOptions', 'exposureRangeType'];

export function ExposureDurationSearch({initArgs}) {
    const {getVal,makeFldObj}= useContext(FieldGroupCtx);
    const {setConstraintFragment}= useContext(ConstraintContext);
    const [constraintResult, setConstraintResult] = useState({});
    useFieldGroupRerender([...fldListAry, ...collapsibleCheckHeaderKeys] ); // force rerender on any change

    const turnOnPanel= () => checkHeaderCtl.setPanelActive(true);

    const isRange= getVal('exposureRangeType') === 'range';
    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), panelValue);

    useEffect(() => {
        const constraints= makeExposureConstraints(getVal('exposureRangeType'), makeFldObj( fldListAry));
        updatePanelStatus(constraints, constraintResult, setConstraintResult);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    },[constraintResult]);

    return (
        <CollapsibleCheckHeader title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} initialStateOpen={false}>
            <div style={{marginTop: 5}}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    <div style={{display: 'block', marginTop: '5px'}}>
                        <ListBoxInputField
                            {...{fieldKey:'exposureRangeType', options: exposureRangeOptions, alignment:'horizontal',
                                label:'Time of Observation:', labelWidth: LableSaptail,
                                initialState:{value: initArgs?.urlApi?.exposureRangeType || 'since'} }} />
                        <div>
                            {isRange ?
                                <ExposeRange {...{initArgs, turnOnPanel, panelActive:checkHeaderCtl.isPanelActive()}}/> :
                                <ExposureSince {...{initArgs, turnOnPanel, panelActive:checkHeaderCtl.isPanelActive()}} /> }
                            <ExposureLength {...{initArgs, turnOnPanel, panelActive:checkHeaderCtl.isPanelActive()}}/>
                        </div>
                        <DebugObsCore {...{constraintResult}}/>
                    </div>
                </ForceFieldGroupValid>
            </div>
        </CollapsibleCheckHeader>
    );
}

ExposureDurationSearch.propTypes = {
    initArgs: PropTypes.object,
};


function ExposeRange({initArgs, panelActive, turnOnPanel}) {
    const [getExposureTimeMode] = useFieldGroupValue('exposureTimeMode');
    const [getExposureMin, setExposureMin] = useFieldGroupValue('exposureMin');
    const [getExposureMax, setExposureMax] = useFieldGroupValue('exposureMax');
    const icon = 'calendar';
    const timeOptions = [{label: 'UTC date/times (ISO format)', value: ISO}, {label: 'MJD values', value: MJD}];

    useEffect(() => {
        const timeMode = initArgs?.urlApi?.exposureTimeMode || ISO;
        if (initArgs?.urlApi?.exposureMin) setExposureMin(initArgs?.urlApi?.exposureMin, {timeMode});
        if (initArgs?.urlApi?.exposureMax) setExposureMax(initArgs?.urlApi?.exposureMax, {timeMode});
    }, [initArgs?.urlApi]);


    useEffect(() => {
        if (!panelActive) return;
        const {minGreaterThanMax}=  checkExposureTime(getExposureMin(true), getExposureMax(true));
        if (minGreaterThanMax) setExposureMax(getExposureMax(), {valid: false, message: START_TIME_GREATER_MSG });
    }, [panelActive]);

    useFieldGroupWatch(['exposureMin', 'exposureMax'],
        ([expMin,expMax],isInit) => !isInit && (expMin || expMax) && turnOnPanel() );

    useEffect(() => {
        onChangeDateTimePicker(getExposureMin, setExposureMin);
        const {minGreaterThanMax}=  checkExposureTime(getExposureMin(true), getExposureMax(true));
        if (minGreaterThanMax) setExposureMax(getExposureMax(), {valid: false, message: START_TIME_GREATER_MSG });
    }, [getExposureMin]);

    useEffect(() => {
        onChangeDateTimePicker(getExposureMax, setExposureMax);
        const {minGreaterThanMax}=  checkExposureTime(getExposureMin(true), getExposureMax(true));
        if (minGreaterThanMax) setExposureMax(getExposureMax(), {valid: false, message: START_TIME_GREATER_MSG });
    }, [getExposureMax]);

    useEffect(() => {
        onChangeTimeMode(getExposureTimeMode(), getExposureMin, setExposureMin);
        onChangeTimeMode(getExposureTimeMode(), getExposureMax, setExposureMax);
    }, [getExposureTimeMode]);



    return (
        <div style={{display: 'block', marginLeft: LeftInSearch, marginTop: 10}}>
            <RadioGroupInputField
                fieldKey='exposureTimeMode' options={timeOptions} alignment={'horizontal'}
                wrapperStyle={{width: LabelWidth, marginTop: 5, marginLeft: 0}}
                label='Use:' tooltip='Select time mode'
                labelWidth={32 /* FIXME: Not sure if this is best */}
                initialState={{value: initArgs?.urlApi?.exposureTimeMode || ISO}}
            />
            <div style={{display: 'flex', marginTop: 10}}>
                <div title='Start Time'
                     style={{
                         display: 'inline-block',
                         paddingRight: '4px',
                         width: SpatialLableSaptail
                     }}>Start Time
                </div>
                <div style={{width: Width_Time_Wrapper}}>
                    <TimePanel
                        fieldKey='exposureMin' timeMode={getExposureTimeMode()} icon={icon}
                        tooltip="'Exposure start from' time (t_min)"
                        feedbackStyle={{height: 100}}
                        inputWidth={Width_Column} inputStyle={{overflow: 'auto', height: 16}}
                        onClickIcon={
                            changeDatePickerOpenStatus(FROM, 'exposureMin', getExposureMin(), getExposureTimeMode(), (value) => {
                                /* NOTE: if we don't do timeMode: expTimeMode - we can't see the current time mode for this field (when new) */
                                setExposureMin(value, {timeMode: getExposureTimeMode()});
                            })}
                        value={initArgs?.urlApi?.exposureMin || getExposureMin()}
                    />
                </div>
            </div>
            <div style={{display: 'flex', marginTop: 5}}>
                <div title='End Time'
                     style={{display: 'inline-block', paddingRight: '4px', width: SpatialLableSaptail}}>End
                    Time
                </div>
                <div style={{width: Width_Time_Wrapper}}>
                    <TimePanel
                        fieldKey='exposureMax' timeMode={getExposureTimeMode()} icon={icon}
                        tooltip={"'Exposure end to' time (t_max)"}
                        feedbackStyle={{height: 100}}
                        inputWidth={Width_Column} inputStyle={{overflow: 'auto', height: 16}}
                        onClickIcon={changeDatePickerOpenStatus(TO, 'exposureMax', getExposureMax(), getExposureTimeMode(), (value) => {
                            setExposureMax(value, {timeMode: getExposureTimeMode()});
                        })}
                        value={initArgs?.urlApi?.exposureMax || getExposureMax()}
                    />
                </div>
            </div>
        </div>
    );
}

function ExposureSince({initArgs, turnOnPanel}) {

    useFieldGroupWatch(['exposureSinceValue'], ([expSince],isInit) => expSince && !isInit && turnOnPanel());

    return (
        <div style={{display: 'flex', marginTop: 10}}>
            <ValidationField
                fieldKey='exposureSinceValue' // FIXME: Introduce SinceValue or similar
                size={SmallFloatNumericWidth}
                inputStyle={{overflow: 'auto', height: 16}}
                validator={() => ({valid: true, message: '' })}
                wrapperStyle={{
                    marginLeft: LableSaptail,
                    paddingLeft: 4 /* Extra padding because there's no label */,
                    paddingBottom: 5
                }}
                initialState={{value: initArgs?.urlApi?.exposureSinceValue || ''}}/>
            <ListBoxInputField
                fieldKey={'exposureSinceOptions'} // FIXME: Introduce SinceOptions
                options={[
                    {label: 'Minutes', value: 'minutes'},
                    {label: 'Hours', value: 'hours'},
                    {label: 'Days', value: 'days'},
                    {label: 'Years', value: 'years'}
                ]}
                initialState={{value: initArgs?.urlApi?.exposureSinceOptions || 'hours'}}/>
        </div>
    );
}

function ExposureLength({initArgs, panelActive, turnOnPanel}) {
    const inputStyle= {overflow: 'auto', height: 16};

    const [getExposureLengthMin] = useFieldGroupValue('exposureLengthMin');
    const [getExposureLengthMax, setExposureLengthMax] = useFieldGroupValue('exposureLengthMax');

    const minMaxCheck= () => {
        const {minGreaterThanMax}=  checkExposureDuration(getExposureLengthMin(), getExposureLengthMax());
        if (minGreaterThanMax) {
            setExposureLengthMax(getExposureLengthMax(), {valid: false, message: START_EXP_GREATER_MSG});
        }
    };

    useFieldGroupWatch(['exposureLengthMin', 'exposureLengthMax'],
        ([expLenMin,expLenMax],isInit) => {
            minMaxCheck();
            if (!isInit && (expLenMin || expLenMax)) turnOnPanel();
        });

    useEffect(() => {
        if (panelActive) minMaxCheck();
    }, [panelActive]);


    return (
        <div style={{display: 'flex', marginTop: 5}}>
            <ValidationField fieldKey='exposureLengthMin'
                size={SmallFloatNumericWidth}
                inputStyle={inputStyle}
                label='Exposure Duration:'
                tooltip='Cumulative shutter-open exposure duration in seconds'
                labelWidth={LableSaptail}
                validator={minimumPositiveFloatValidator('Minimum Exposure Length')}
                placeholder='-Inf'
                initialState={{value: initArgs?.urlApi?.exposureLengthMin}}/>
            <div style={{display: 'flex', marginTop: 5, marginRight: '16px', paddingRight: '3px'}}>to</div>
            <ValidationField fieldKey='exposureLengthMax'
                size={SmallFloatNumericWidth}
                inputStyle={inputStyle}
                tooltip='Cumulative shutter-open exposure must be less than this amount'
                validator={maximumPositiveFloatValidator('Maximum Exposure Length')}
                placeholder='+Inf'
                initialState={{value: initArgs?.urlApi?.exposureLengthMax}}/>
            <div style={{display: 'flex', marginTop: 5}}>seconds</div>
        </div>

    );
}