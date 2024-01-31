/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Stack, Typography} from '@mui/joy';
import CALENDAR from 'html/images/datetime_picker_16x16.png';
import PropTypes from 'prop-types';
import React, {memo} from 'react';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {InputFieldView} from './InputFieldView.jsx';
import {
    convertISOToMJD, convertMJDToISO, formatMoment, formFeedback, ISO, isTimeUndefined, MJD, validateDateTime, validateMJD
} from './TimeUIUtil.js';

const invalidDate = 'invalid date/time';

const iconMap = {'calendar': {icon: CALENDAR, title: 'Show the calendar for selecting date/time'}};



function TimePanelView({showHelp, feedback, feedbackStyle, examples, label,
                           valid, message, onChange, value, icon, onClickIcon, tooltip = 'select time',
                           inputStyle, inputWidth, timeMode=ISO, isTimeModeFixed}) {
    const ImagePadding = 3;

    const iconField = iconMap?.[icon] ?
        ( <img title={iconMap[icon].title} src={iconMap[icon].icon} onClick={() => onClickIcon?.()}/> ) : undefined;

    const spaceForImage = 16+ImagePadding*2;
    const newInputStyle = {...inputStyle,
        width: iconField ? inputWidth - spaceForImage+2 : inputWidth,
        paddingRight: iconField ? spaceForImage : 2
    };
    const placeHolder = timeMode === ISO ? 'YYYY-MM-DD HH:mm:ss' : 'float number .... ';
    const inputFields = {
        valid, visible: true, message, onChange,
        value, tooltip, style: newInputStyle,
        endDecorator: iconField,
        placeHolder
    };

    const timeField =  (<InputFieldView {...{...inputFields, label}} />);

    const outsideWidth = inputWidth + 6;
    const timePart = iconField ? (<div style={{position: 'relative', width: outsideWidth}}>
                                        {timeField}
                                  </div>)
                                : timeField;

    const newFeedbackStyle = {width: inputWidth, ...feedbackStyle};
    const timeDiv = (
        <Stack>
            {timePart}
            <TimeFeedback {...{showHelp, feedback, style: newFeedbackStyle, examples, timeMode, isTimeModeFixed}}/>
        </Stack>
    );
    return timeDiv;
}

TimePanelView.propTypes = {
    showHelp   : PropTypes.bool,
    feedback: PropTypes.string,
    feedbackStyle: PropTypes.object,
    examples: PropTypes.object,
    message: PropTypes.string.isRequired,
    onChange: PropTypes.func,
    valid   : PropTypes.bool.isRequired,
    value : PropTypes.string.isRequired,
    labelStyle : PropTypes.object,
    label: PropTypes.string,
    tooltip: PropTypes.string,
    inputStyle: PropTypes.object,
    wrapperStyle: PropTypes.object,
    timeMode: PropTypes.string,
    icon: PropTypes.string,
    onClickIcon: PropTypes.func,
    labelPosition: PropTypes.oneOf(['top', 'left']),
    inputWidth: PropTypes.number,
    isTimeModeFixed: PropTypes.bool
};

TimePanelView.defaultProps = {
    valid: true,
    showHelp: true,
    feedback: '',
    message: '',
    value: '',
    timeMode: ISO
};

const defaulISOtExample = (<div style={{display: 'inline-block', fontSize: 11}}>
                        {'2019-02-07T08:00:20'}
                        <br/>
                        {'2019-02-07 08:00:20'}
                        <br/>
                        {'2019-02-07'}
                        </div>);

const defaultMJDExample = (<div style={{display: 'inline-block'}}>
                            {'56800, 56800.3333'}
                           </div>);


function TimeFeedback({showHelp, feedback, examples, timeMode=ISO, isTimeModeFixed}) {

    examples = timeMode===ISO ? (examples?.[ISO] ?? defaulISOtExample) : (examples?.[MJD] ?? defaultMJDExample);
    // style = {paddingTop: 5, height: isTimeModeFixed ? 'auto' : '4rem', // so that layout doesn't jump on toggling radio button
    //     display:'flex', contentJustify: 'center', ...style};

    if (showHelp) {
         return (
            <Stack {...{spacing:1, direction:'row', pt:1/2, height: isTimeModeFixed ? 'auto' : '4rem'}}>
                <Typography component='div' level={'title-md'}>e.g.</Typography>
                <Typography component='div'> {examples} </Typography>
            </Stack>
         );
    } else {
         return (
             <Stack {...{direction:'row', pt:1/4, height: isTimeModeFixed ? 'auto' : '4rem'}}>
                <span dangerouslySetInnerHTML={{
                    __html: feedback
                }}/>
            </Stack>
         );
    }
}

TimeFeedback.propTypes = {
    showHelp: PropTypes.bool,
    feedback: PropTypes.string,
    style: PropTypes.object,
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
    const feedback = showHelp ? '' : formFeedback(result.UTC, result.MJD);

    // time panel show v as display value, feedback shows standard moment utc string
    fireValueChange({valid: validateRet.valid,
                    message: validateRet.message,
                    value: v,
                    showHelp, feedback,
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
    icon: PropTypes.string,
    onClickIcon: PropTypes.func,
    feedbackStyle: PropTypes.object,
    labelPosition: PropTypes.oneOf(['top', 'left']),
    inputStyle: PropTypes.object,
    inputWidth: PropTypes.number,
    isTimeModeFixed: PropTypes.bool
};

