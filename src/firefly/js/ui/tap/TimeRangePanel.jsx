import {Stack} from '@mui/joy';
import React, {useContext, useEffect} from 'react';
import PropTypes from 'prop-types';
import {makeDatePickerPopup} from '../DateTimePickerField.jsx';
import {FieldGroupCtx} from '../FieldGroup.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {useFieldGroupRerender, useFieldGroupValue, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {TimePanel} from '../TimePanel.jsx';
import {changeTimeMode, checkExposureTime, getTimeInfo, isTimeUndefined, ISO, MJD} from '../TimeUIUtil.js';

const START_TIME_GREATER_MSG= 'the start time is greater than the end time';
const timeOptions = [{label: 'UTC date/times (ISO format)', value: ISO}, {label: 'MJD values', value: MJD}];
const timeOptionsTip='Choose between:\nISO 8601 time format (e.g., 2021-03-20)\nor\nModified Julian Date time format (e.g., 59293.1)';


export function TimeRangePanel({initArgs, panelActive=true, turnOnPanel, sx={},
                                   minKey= 'exposureMin', maxKey= 'exposureMax',
                                   minLabel='Start Time', maxLabel='End Time',
                                   fromTip="'from' time", toTip="'to' time",
                                   fixedTimeMode, minExamples, maxExamples,}) {
    const {setFld,getFld,getVal} = useContext(FieldGroupCtx);
    const [getExposureTimeMode] = useFieldGroupValue('exposureTimeMode'); //user-controlled through RadioGroupInputField
    const timeMode = fixedTimeMode || getExposureTimeMode(); // if no fixed time mode passed, time mode is as chosen by user

    useFieldGroupRerender([minKey,maxKey]);

    useEffect(() => {
        const timeMode = initArgs?.urlApi?.exposureTimeMode || ISO;
        if (initArgs?.urlApi?.[minKey]) setFld(minKey, {value:initArgs?.urlApi?.[minKey], timeMode});
        if (initArgs?.urlApi?.[maxKey]) setFld(maxKey, {value:initArgs?.urlApi?.[maxKey], timeMode});
    }, [initArgs?.urlApi]);

    useFieldGroupWatch([minKey, maxKey],
        ([expMin, expMax], isInit) => {
            if (!isInit && !panelActive && (expMin || expMax)) {
                turnOnPanel?.();
            }
            if (!panelActive) return;
            const {minGreaterThanMax, maxHasValidValue} = checkExposureTime(getFld(minKey), getFld(maxKey));
            if (minGreaterThanMax) setFld(maxKey, {valid: false, message: START_TIME_GREATER_MSG});
            else if (maxHasValidValue) setFld(maxKey, {valid: true});
        },[panelActive]);

    useEffect(() => {
        const newMode = getExposureTimeMode();
        setFld(minKey, changeTimeMode(newMode, getFld(minKey)));
        setFld(maxKey, changeTimeMode(newMode, getFld(maxKey)));
    }, [getExposureTimeMode]);

    const setNewTimeValue= (key, value) => {
        const {[ISO]:{value:isoVal}, [MJD]:{value:mjdVal}}= getTimeInfo(timeMode, value, true, '');
        const feedback = fixedTimeMode === ISO ? {UTC: isoVal, MJD: ''}
            : (fixedTimeMode === MJD ? {UTC: '', MJD: mjdVal} : {UTC: isoVal, MJD: mjdVal});
        setFld(key, {value, timeMode, feedback, showHelp: isTimeUndefined(isoVal, mjdVal) });
    };

    return (
        <Stack spacing={1} sx={sx}>
            {!fixedTimeMode && <RadioGroupInputField
                fieldKey='exposureTimeMode' options={timeOptions} orientation='horizontal'
                tooltip={timeOptionsTip}
                initialState={{value: initArgs?.urlApi?.exposureTimeMode || ISO}}
            />}
            <Stack {...{direction: 'row', spacing:2, mb: 1}}>
                <TimePanel
                    fieldKey={minKey}
                    timeMode={timeMode}
                    label={minLabel}
                    tooltip={fromTip}
                    makePicker={
                        makeDatePickerPopup('select "from" time', getVal(minKey), timeMode,
                            (value) => setNewTimeValue(minKey,value) )}
                    value={initArgs?.urlApi?.exposureMin || getVal(minKey)}
                    isTimeModeFixed={Boolean(fixedTimeMode)}
                    examples={minExamples}/>
                <TimePanel
                    fieldKey={maxKey}
                    timeMode={timeMode}
                    label={maxLabel}
                    tooltip={toTip}
                    makePicker={
                        makeDatePickerPopup('select "to" time', getVal(maxKey), timeMode,
                            (value) => setNewTimeValue(maxKey,value) )}
                    value={initArgs?.urlApi?.exposureMax || getVal(maxKey)}
                    isTimeModeFixed={Boolean(fixedTimeMode)}
                    examples={maxExamples}/>
            </Stack>
        </Stack>
    );
}

TimeRangePanel.propTypes = {
    initArgs: PropTypes.object,
    panelActive: PropTypes.bool,
    turnOnPanel: PropTypes.func,
    sx: PropTypes.object,
    minKey: PropTypes.string,
    maxKey: PropTypes.string,
    minLabel: PropTypes.string,
    maxLabel: PropTypes.string,
    fromTip: PropTypes.string,
    toTip: PropTypes.string,
    fixedTimeMode : PropTypes.oneOf([ISO, MJD]),
    minExamples: PropTypes.object,
    maxExamples: PropTypes.object,
};