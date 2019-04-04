/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent, memo} from 'react';
import PropTypes from 'prop-types';
import {has} from 'lodash';
import {clone} from '../util/WebUtil.js';
import {InputFieldView} from './InputFieldView.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {convertISOToMJD, convertMJDToISO, validateDateTime, validateMJD, fMoment} from './DateTimePickerField.jsx';
import {MJD, ISO} from './tap/TapUtil.js';


import CALENDAR from 'html/images/datetime_picker_16x16.png';
const invalidDate = 'invalid date/time';

const iconMap = {'calendar': {icon: CALENDAR, title: 'Show the calendar for selecting date/time'}};



class TimePanelView extends PureComponent {
    constructor(props) {
        super(props);
    }


    render() {
        const {showHelp, feedback, feedbackStyle, examples, label, labelStyle, labelPosition,
            valid, message, onChange, value, icon, onClickIcon, tooltip = 'select time',
            inputStyle, wrapperStyle, inputWidth, timeMode=ISO}= this.props;
        const ImagePadding = 3;

        const iconStyle = {
            position: 'absolute',
            top: 1,
            right: 1,
            padding: ImagePadding,
            cursor: 'pointer'
        };

        const iconField = icon && has(iconMap, icon) ?
            (<div style={iconStyle}>
                <img
                  title={iconMap[icon].title}
                  src={iconMap[icon].icon}
                  onClick={() => {onClickIcon && onClickIcon();}}/>
             </div>) : null;

        const spaceForImage = 16+ImagePadding*2;
        const newInputStyle = Object.assign({paddingRight: iconField ? spaceForImage : 2}, inputStyle,
                                            {width: iconField ? inputWidth - spaceForImage+2 : inputWidth});
        const placeHolder = timeMode === ISO ? 'YYYY-MM-DD HH:mm:ss' : 'float number .... ';
        const newWrapperStyle = clone(wrapperStyle, (iconField ? {width: '%100'} : {}));
        const inputFields = {
            valid, visible: true, message, onChange,
            value, tooltip, wrapperStyle: newWrapperStyle, style: newInputStyle,
            placeHolder
        };

        const timeField =  (<InputFieldView {...inputFields} />);

        const outsideWidth = inputWidth + 6;
        const timePart = iconField ? (<div style={{position: 'relative', width: outsideWidth}}>
                                            {timeField}
                                            {iconField}
                                      </div>)
                                    : (timeField);

        const newFeedbackStyle = clone(feedbackStyle, {width: inputWidth});
        const lStyle = clone(labelStyle, {whiteSpace:'nowrap'});
        const labelDiv = (<div style={lStyle}>{label}</div>);
        const timeDiv = (
            <div>
                {timePart}
                <TimeFeedback {...{showHelp, feedback, feedbackStyle: newFeedbackStyle, examples, timeMode}}/>
            </div>
        );
        const flexDirection = (labelPosition && labelPosition === 'top') ? 'column' : 'row';

        return (
            <div style={{display: 'flex', flexDirection}}>
                {labelDiv}
                {timeDiv}
            </div>
        );
    }
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
    inputWidth: PropTypes.number
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
const defaultStyle =  {paddingTop: 5, height: 50, display:'flex', contentJustify: 'center'};

function TimeFeedback({showHelp, feedback, style={}, examples={}, timeMode=ISO}) {

    examples = timeMode===ISO ? clone(defaulISOtExample, examples) : clone(defaultMJDExample, examples);
    style = clone(defaultStyle, style);

    if (showHelp) {
         return (
            <div style={style}>
                <i>e.g.:</i>
                <div style={{marginLeft: 3}}>
                   {examples}
                </div>
            </div>
         );
    } else {
         return (
            <div style={style}>
                <span dangerouslySetInnerHTML={{
                    __html: feedback
                }}/>
            </div>
         );
    }
}

TimeFeedback.propTypes = {
    showHelp: PropTypes.bool,
    feedback: PropTypes.string,
    style: PropTypes.string,
    examples: PropTypes.object,
    timeMode: PropTypes.string
};

function handleOnChange(ev, params, fireValueChange) {
    const v = ev.target.value;
    const {timeMode = ISO} = params;
    const result = {UTC: '', MJD: ''};
    let validateRet = {valid: true};

    if (timeMode === ISO) {
        validateRet = validateDateTime(v);
        if (validateRet.valid) {
            result.UTC = v ? (fMoment(validateRet.moment)): v;
            result.MJD = convertISOToMJD(validateRet.moment);
        }
    } else if (timeMode === MJD) {
        validateRet = validateMJD(v);
        if (validateRet.valid) {
            result.MJD = validateRet.value;
            result.UTC = convertMJDToISO(v);
        }
    }
    if (!validateRet.valid) {
        result.UTC = '';
        result.MJD = '';
        result.message = invalidDate;
    }
    const showHelp = isShowHelp(result.UTC, result.MJD);
    const feedback = showHelp ? '' : formFeedback(result.UTC, result.MJD);

    // time panel show v as display value, feedback shows standard moment utc string
    fireValueChange({valid: validateRet.valid,
                    message: validateRet.message,
                    value: v,
                    showHelp, feedback,
                    timeMode});
}

export const formFeedback = (utc, mjd) => {
    return isShowHelp(utc, mjd) ? '' :
            '<div style="font-size:11px">' +
                    `<i>UTC:&nbsp</i>${utc}<br/>`+
                    `<div style="padding-top: 6px"><i>MJD:&nbsp</i>${mjd}</div>`+'</div>';
};

export const isShowHelp = (utc, mjd) => {
    return !utc && !mjd;
};

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
    inputWidth: PropTypes.number
};

