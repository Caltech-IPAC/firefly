import React, {memo, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {Divider, Stack} from '@mui/joy';
import {DateCalendar, MultiSectionDigitalClock, LocalizationProvider} from '@mui/x-date-pickers';
import {AdapterMoment} from '@mui/x-date-pickers/AdapterMoment';
import moment from 'moment';

import {useFieldGroupValue} from 'firefly/ui/SimpleComponent';
import {isDialogVisible} from '../core/ComponentCntlr.js';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {POPUP_DIALOG_ID, showOptionsPopup} from './PopupUtil.jsx';
import {JoyToMUIThemeWrapper} from 'firefly/ui/JoyToMUIThemeWrapper';
import {formatMoment, getTimeInfo, ISO, validateDateTime} from './TimeUIUtil.js';


export function DateTimePicker({value='', onChange, sx, slotProps,
                                   groupKey, timeFieldKey}) {
    const initialMoment = moment.utc(value);
    const [dateMoment, setDateMoment] = useState(initialMoment.isValid() ? initialMoment : null);
    const [timeMoment, setTimeMoment] = useState(initialMoment.isValid() ? initialMoment
        : moment().utc().startOf('day')); //00:00:00

    useEffect(()=>{
        //combine date and time moment
        const datetimeMoment = dateMoment?.set({
            hour: timeMoment.hour(),
            minute: timeMoment.minute(),
            second: timeMoment.second()
        });
        if (datetimeMoment) onChange?.(datetimeMoment);
    }, [dateMoment, timeMoment]);

    const setDateTimeMoments = (str) => {
        const datetimeMoment = moment.utc(str);
        if (datetimeMoment.isValid()) {
            setDateMoment(datetimeMoment);
            setTimeMoment(datetimeMoment);
        }
    };

    useEffect(()=>{
        setDateTimeMoments(value);
    },[value]);

    const [,getTimeStr]= useFieldGroupValue(timeFieldKey, groupKey);
    useEffect(() => {
        if (timeFieldKey) setDateTimeMoments(getTimeStr());
    }, [getTimeStr]);

    return (
        <JoyToMUIThemeWrapper>
            <LocalizationProvider dateAdapter={AdapterMoment}>
                <Stack direction='row' sx={sx}>
                    <DateCalendar
                        sx={{width: '20rem', mr: 2, '.MuiYearCalendar-root': {width: 'auto'}}}
                        timezone='UTC'
                        value={dateMoment}
                        onChange={(newValue)=>setDateMoment(newValue)}
                        {...slotProps?.datePicker}
                    />
                    <Divider orientation='vertical'/>
                    <MultiSectionDigitalClock
                        sx={{width: 'auto', borderBottom: 0, '.MuiList-root': {maxHeight: 1}}}
                        views={['hours', 'minutes', 'seconds']}
                        ampm={false}
                        timezone='UTC'
                        value={timeMoment}
                        onChange={(newValue)=>setTimeMoment(newValue)}
                        {...slotProps?.timePicker}
                    />
                </Stack>
            </LocalizationProvider>
        </JoyToMUIThemeWrapper>
    );
}

DateTimePicker.propTypes= {
    value: PropTypes.string,
    onChange: PropTypes.func,
    sx: PropTypes.object,
    slotProps: PropTypes.shape({
        datePicker: PropTypes.object,
        timePicker: PropTypes.object
    }),
    groupKey: PropTypes.string,
    timeFieldKey: PropTypes.string,
};


function handleOnChange(momentVal, params, fireValueChange) {
    const {nullAllowed=true} = params;

    const {valid, message, moment} = validateDateTime(momentVal, nullAllowed);
    const value = formatMoment(moment);     // datetime picker store iso string

    // store the range in string style
    fireValueChange({value, valid, message});
}

export const DateTimePickerField= memo( (props) => {
    const {viewProps, fireValueChange}= useFieldGroupConnector(props);
    return (<DateTimePicker {...viewProps}
                           onChange={(momentVal) => handleOnChange(momentVal,viewProps, fireValueChange)}/>);
});


DateTimePickerField.propTypes= {
    onChange: PropTypes.func,
    sx: PropTypes.object,
    slotProps: PropTypes.shape({
        datePicker: PropTypes.object,
        timePicker: PropTypes.object
    }),
    timeFieldKey: PropTypes.string,
    groupKey: PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.string,
    })
};

export const makeDatePickerPopup = (title, currentValue, currentTimeMode, setTimeCallback) => {
    const currentTimeInfo = getTimeInfo(currentTimeMode, currentValue, true, '');
    const doSetTime = (moment) => {
        const timeInfo = getTimeInfo(ISO, formatMoment(moment), true, '');
        setTimeCallback(timeInfo[currentTimeMode].value);
    };
    return () => {
        const show = !isDialogVisible(POPUP_DIALOG_ID);
        const content = (
            <DateTimePicker value={currentTimeInfo[ISO].value}
                            onChange={doSetTime}/>);

        showOptionsPopup({content, title, modal: true, show});
    };
};