import React, {memo, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {useFieldGroupValue} from 'firefly/ui/SimpleComponent';
import DateTime from 'react-datetime';
import {isDialogVisible} from '../core/ComponentCntlr.js';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {POPUP_DIALOG_ID, showOptionsPopup} from './PopupUtil.jsx';

import './tap/react-datetime.css';
import {
    aMoment, formatMoment, getTimeInfo, ISO, isSameTimes, convertToMoment, validateDateTime
} from './TimeUIUtil.js';

export function DateTimePicker({showInput,openPicker, value='', onChange, onChangeOpenStatus,wrapperStyle,
                                   inputStyle, groupKey, timeFieldKey}) {

    const [timeMoment, setTimeMoment] = useState(() => aMoment(value).isValid() ? aMoment(value) : value);
    const [,getTimeStr]= useFieldGroupValue(timeFieldKey);
    useEffect(() => {
        //componentDidMount code
        let unbinder;
        // update the state in case start time or end time are updated due to the change from the entry
        if (groupKey && timeFieldKey) {
           unbinder = FieldGroupUtils.bindToStore(groupKey, () => {
               const time= getTimeStr();
               if (!time || isSameTimes(time, timeMoment)) return;
               setTimeMoment(time);
            });
        }

        //componentWillUnmount code
        return () => {
            if (unbinder) unbinder();
        };
    }, [getTimeStr]);

    const onClose= () => {
        onChangeOpenStatus?.(false);
    };

    const onSelectedDate = (moment) => {

        const newTime = convertToMoment(moment);
        if (typeof moment === 'string' && !newTime.isValid()) {
            moment = '';
        }

        setTimeMoment(moment);
        onChange?.(moment);
    };

    wrapperStyle = {...wrapperStyle, margin: 10, height: 'calc(100% - 20pt)'};
    inputStyle = {...inputStyle, marginBottom: 3, width: 150};

    const showOneDatePicker = () => {
        // DateTime needs the value input in type of Moment
        return (
                <DateTime onBlur={onClose}
                          onChange={onSelectedDate}
                          value={convertToMoment(timeMoment)}
                          input={showInput}
                          open={openPicker}
                          inputProps={{style: inputStyle}}
                          utc={true}
                          timeFormat={'HH:mm:ss A'}
                />
        );
    };

    return (
        <div style={wrapperStyle}>
            {showOneDatePicker()}
        </div>
    );
}

DateTimePicker.propTypes= {
    showInput: PropTypes.bool,
    openPicker:  PropTypes.bool,
    value: PropTypes.string,
    onChange: PropTypes.func,
    onChangeOpenStatus: PropTypes.func,
    wrapperStyle: PropTypes.object,
    inputStyle: PropTypes.object,
    groupKey: PropTypes.string,
    timeFieldKey: PropTypes.string,
};

DateTimePicker.defaultValue = {
    showInput: true,
    openPicker: false
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
    showInput: PropTypes.bool,
    openPicker:  PropTypes.bool,
    onChange: PropTypes.func,
    onChangeOpenStatus: PropTypes.func,
    wrapperStyle: PropTypes.object,
    inputStyle: PropTypes.object,
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
            <DateTimePicker showInput={false} openPicker={true}
                            value={currentTimeInfo[ISO].value}
                            onChange={doSetTime}
                            inputStyle={{marginBottom: 3}}/>);

        showOptionsPopup({content, title, modal: true, show});
    };
};