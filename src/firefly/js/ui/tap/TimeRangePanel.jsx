import React, {useContext, useEffect} from 'react';
import PropTypes from 'prop-types';
import {makeDatePickerPopup} from '../DateTimePickerField.jsx';
import {FieldGroupCtx} from '../FieldGroup.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {useFieldGroupRerender, useFieldGroupValue, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {TimePanel} from '../TimePanel.jsx';
import {changeTimeMode, checkExposureTime, formFeedback, getTimeInfo, isTimeUndefined, ISO, MJD} from '../TimeUIUtil.js';

const START_TIME_GREATER_MSG= 'the start time is greater than the end time';
const icon = 'calendar';
const timeOptions = [{label: 'UTC date/times (ISO format)', value: ISO}, {label: 'MJD values', value: MJD}];
const labelWidth=60;
const timePanelInputWidth= 175;
const timeOptionsTip='Choose between:\nISO 8601 time format (e.g., 2021-03-20)\nor\nModified Julian Date time format (e.g., 59293.1)';
const timeLabelStyle= { paddingRight: '4px', width: labelWidth, paddingTop:4, height:'1.5.em' };


export function TimeRangePanel({initArgs, panelActive=true, turnOnPanel, style={},
                                       minKey= 'exposureMin', maxKey= 'exposureMax',
                                       fromTip="'from' time",
                                       toTip="'to' time"}) {
    const {setFld,getFld,getVal} = useContext(FieldGroupCtx);
    const [getExposureTimeMode] = useFieldGroupValue('exposureTimeMode');
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
        const {[ISO]:{value:isoVal}, [MJD]:{value:mjdVal}}= getTimeInfo(getExposureTimeMode(), value, true, '');
        setFld(key, {value, timeMode: getExposureTimeMode(),
            feedback: formFeedback(isoVal, mjdVal), showHelp: isTimeUndefined(isoVal, mjdVal) });
    };

    return (
        <div style={{display: 'block', ...style}}>
            <RadioGroupInputField
                fieldKey='exposureTimeMode' options={timeOptions} alignment={'horizontal'}
                wrapperStyle={{marginTop: 5, marginLeft: 0}}
                label='Use' tooltip={timeOptionsTip}
                labelWidth={labelWidth}
                initialState={{value: initArgs?.urlApi?.exposureTimeMode || ISO}}
            />
            <div style={{display: 'flex', marginTop: 10}}>
                <div title={fromTip} style={timeLabelStyle}>Start Time</div>
                <TimePanel
                    fieldKey={minKey} timeMode={getExposureTimeMode()} icon={icon}
                    tooltip={fromTip} feedbackStyle={{height: 100}}
                    inputWidth={timePanelInputWidth} inputStyle={{overflow: 'auto', height: 16}}
                    onClickIcon={
                        makeDatePickerPopup('select "from" time', getVal(minKey), getExposureTimeMode(),
                            (value) => setNewTimeValue(minKey,value) )}
                    value={initArgs?.urlApi?.exposureMin || getVal(minKey)} />
            </div>
            <div style={{display: 'flex', marginTop: 5}}>
                <div title={toTip} style={timeLabelStyle}>End Time</div>
                <TimePanel
                    fieldKey={maxKey} timeMode={getExposureTimeMode()} icon={icon}
                    tooltip={toTip} feedbackStyle={{height: 100}}
                    inputWidth={timePanelInputWidth} labelStyle={{overflow: 'auto', height: 16}}
                    onClickIcon={
                        makeDatePickerPopup('select "to" time', getVal(maxKey), getExposureTimeMode(),
                            (value) => setNewTimeValue(maxKey,value) )}
                    value={initArgs?.urlApi?.exposureMax || getVal(maxKey)} />
            </div>
        </div>
    );
}

TimeRangePanel.propTypes = {
    initArgs: PropTypes.object,
    panelActive: PropTypes.bool,
    turnOnPanel: PropTypes.func,
    style: PropTypes.object,
    minKey: PropTypes.string,
    maxKey: PropTypes.string,
    fromTip: PropTypes.string,
    toTip: PropTypes.string,
};