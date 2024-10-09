/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {FormHelperText, IconButton, Stack} from '@mui/joy';
import Event from '@mui/icons-material/Event';
import PropTypes from 'prop-types';
import React, {memo} from 'react';

import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {
    convertISOToMJD, convertMJDToISO, formatMoment, ISO, isTimeUndefined, MJD, validateDateTime, validateMJD
} from './TimeUIUtil.js';

const invalidDate = 'invalid date/time';




function TimePanelView({showHelp=true, feedback={}, sx, slotProps, examples, label,
                           valid=true, message='', onChange, value='', makePicker,
                           tooltip = 'select time', timeMode=ISO, isTimeModeFixed, orientation}) {

    const endDecorator = makePicker
        ? (<IconButton onClick={() => makePicker?.()} aria-label='Show date/time picker'>
            <Event/>
        </IconButton>)
        : undefined;
    const placeholder = timeMode === ISO ? 'YYYY-MM-DD HH:mm:ss' : 'float number ...';

    return (
        <Stack spacing={.5} sx={sx}>
            <InputFieldView {...{valid, visible: true, message, onChange, value, tooltip, endDecorator,
                placeholder, orientation, label, ...slotProps?.input,
                sx: {'.MuiInput-root': {width: '16rem'}, ...slotProps?.input?.sx}}} />
            <TimeFeedback {...{showHelp, feedback, examples, timeMode, isTimeModeFixed, ...slotProps?.feedback}}/>
        </Stack>
    );
}

TimePanelView.propTypes = {
    showHelp   : PropTypes.bool,
    feedback: PropTypes.shape({UTC: PropTypes.string, MJD: PropTypes.string}),
    examples: PropTypes.object,
    message: PropTypes.string.isRequired,
    onChange: PropTypes.func,
    valid   : PropTypes.bool.isRequired,
    value : PropTypes.string.isRequired,
    label: PropTypes.string,
    tooltip: PropTypes.string,
    sx: PropTypes.object,
    slotProps: PropTypes.shape({
        input: PropTypes.object,
        feedback: PropTypes.object
    }),
    timeMode: PropTypes.string,
    makePicker: PropTypes.func,
    orientation: PropTypes.oneOf(['horizontal', 'vertical']),
    isTimeModeFixed: PropTypes.bool
};


const defaulISOtExample = (<div style={{display: 'inline-block'}}>
                        {'2019-02-07T08:00:20'}
                        <br/>
                        {'2019-02-07 08:00:20'}
                        <br/>
                        {'2019-02-07'}
                        </div>);

const defaultMJDExample = (<div style={{display: 'inline-block'}}>
                            {'56800, 56800.3333'}
                           </div>);


function TimeFeedback({showHelp, feedback, sx={}, examples, timeMode=ISO, isTimeModeFixed}) {
    examples = timeMode===ISO ? (examples?.[ISO] ?? defaulISOtExample) : (examples?.[MJD] ?? defaultMJDExample);
    return (
        <FormHelperText sx={{
            minHeight: isTimeModeFixed ? 'auto' : '3rem', // fixed height so that layout doesn't jump on toggling radio button
            alignItems: 'flex-start',
            ...sx
        }}>
            {showHelp ?
                <Stack direction='row' spacing={.5}>
                    <span>e.g.:</span>
                    {examples}
                </Stack> :
                <Stack spacing={.5}>
                    {feedback?.UTC && <span>UTC: {feedback.UTC}</span>}
                    {feedback?.MJD && <span>MJD: {feedback.MJD}</span>}
                </Stack>
            }
        </FormHelperText>
    );
}

TimeFeedback.propTypes = {
    showHelp: PropTypes.bool,
    feedback: PropTypes.shape({UTC: PropTypes.string, MJD: PropTypes.string}),
    sx: PropTypes.object,
    examples: PropTypes.object,
    timeMode: PropTypes.string,
    isTimeModeFixed: PropTypes.bool
};

function handleOnChange(ev, params, fireValueChange) {
    const v = ev.target.value;
    const {timeMode = ISO, isTimeModeFixed} = params;
    const result = {UTC: '', MJD: ''};
    let validateRet = {valid: true};

    if (timeMode === ISO) {
        validateRet = validateDateTime(v);
        if (validateRet.valid) {
            result.UTC = v ? (formatMoment(validateRet.moment)): v;
            if (!isTimeModeFixed) result.MJD = convertISOToMJD(validateRet.moment);
        }
    } else if (timeMode === MJD) {
        validateRet = validateMJD(v);
        if (validateRet.valid) {
            result.MJD = validateRet.value;
            if (!isTimeModeFixed) result.UTC = convertMJDToISO(v);
        }
    }
    if (!validateRet.valid) {
        result.UTC = '';
        result.MJD = '';
        result.message = invalidDate;
    }
    const showHelp = isTimeUndefined(result.UTC, result.MJD);

    // time panel show v as display value, feedback shows standard moment utc string
    fireValueChange({valid: validateRet.valid,
                    message: validateRet.message,
                    value: v,
                    showHelp, feedback: result,
                    timeMode, isTimeModeFixed});
}

export const TimePanel= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    const newProps= {...viewProps, value:viewProps.value || '' };
    return (<TimePanelView {...newProps} onChange={(ev) => handleOnChange(ev,viewProps, fireValueChange)}/>);
});

TimePanel.propTypes = {
    fieldKey : PropTypes.string,
    groupKey : PropTypes.string,
    timeMode: PropTypes.string,
    makePicker: PropTypes.func,
    orientation: PropTypes.oneOf(['horizontal', 'vertical']),
    sx: PropTypes.object,
    slotProps: PropTypes.shape({
        input: PropTypes.object,
        feedback: PropTypes.object
    }),
    isTimeModeFixed: PropTypes.bool
};

