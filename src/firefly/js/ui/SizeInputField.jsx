import React, {Component, PropTypes}  from 'react';
import {get} from 'lodash';

import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {convertAngle} from '../visualize/VisUtil.js';
import {InputFieldView} from './InputFieldView.jsx';
import {ListBoxInputFieldView} from './ListBoxInputField.jsx';
import Validate from '../util/Validate.js';
import validator from 'validator';

const invalidSizeMsg = 'size is not set properly or size is out of range';
const DECDIGIT = 6;
const unitSign = { 'arcsec':'"', 'arcmin':'\'', 'deg':' Deg' };

function getUnit(unit) {
    return unitSign[unit];
}

/*
 * remove trailing zero from toFixed result
 */
function toMaxFixed(floatNum, digits) {
    return parseFloat(floatNum.toFixed(digits));
}

// input: string format,
// output: size in degree (string foramt, no decimal digit limit), '': invalid input
export const sizeToDeg = (sizestr, unit) => {
    if (sizestr && !validator.isFloat(sizestr)) {
        return sizestr;
    }
    return (sizestr) ? toMaxFixed(convertAngle(((unit) ? unit : 'deg'), 'deg', sizestr), DECDIGIT).toString() : '';
};

// input: size in degree string format
// output: size in string format in any given unit (for displaying)
export const sizeFromDeg= (sizeDeg, unit) => (
    (sizeDeg)? toMaxFixed(convertAngle('deg', ((unit) ? unit :'deg'), sizeDeg), DECDIGIT).toString() : ''
);

// validate if size (in degree, string foramt) is within the range
var isSizeValid = (sizeDeg,  min, max) => (
    (sizeDeg && Validate.floatRange(min, max, 1, 'value of  size in degree', sizeDeg).valid) ? true : false
);

function updateSizeInfo(params) {
    var unit = params.unit ? params.unit : 'deg';
    var valid = (params.nullAllowed && !params.value) || isSizeValid(params.value, params.min, params.max);
    var value = params.value; //(valid&&params.value) ? params.value  : '';
    var displayValue = params.displayValue ? params.displayValue : sizeFromDeg(value, unit);

    return {unit, valid, value, displayValue};
}

function getProps(params, fireValueChange) {

    var {unit, valid, value, displayValue} = updateSizeInfo(params);

    return Object.assign({}, params,
        {
            onChange: (ev, sizeInfo) => handleOnChange(ev, sizeInfo, params, fireValueChange),
            unit,
            displayValue,
            value,
            valid
        });
}

/**
 *
 * @param ev
 * @param sizeInfo
 * @param params
 * @param fireValueChange unit, value, displayValue are string, and valid is bool
 */
function handleOnChange(ev, sizeInfo, params, fireValueChange) {
     var {unit, value, valid, displayValue} = Object.assign({}, params, sizeInfo);
     var min = () => sizeFromDeg(params.min, params.unit);
     var max = () => sizeFromDeg(params.max, params.unit);
     var msg = valid? '': `${invalidSizeMsg}, ${min()}-${max()}${getUnit(params.unit)}.`;


     fireValueChange({
         feedback: msg,
         displayValue,
         unit,
         value,
         valid
     });
}

const propTypes={
    label:       PropTypes.string,
    labelWidth:  PropTypes.number,
    value:       PropTypes.string.required,
    unit:        PropTypes.string,
    min:         PropTypes.number.isRequired,
    max:         PropTypes.number.isRequired,
    showFeedback:    PropTypes.bool
};

/**
 * @param {string} value size value in degree
 * @param {string} displayValue size value displayed
 * @param {string} unit selected unit
 * @param {bool}   valid validation of size value in degree
 *
 */

class SizeInputFieldView extends Component {
    constructor(props) {
        super(props);

        this.onSizeChange = this.onSizeChange.bind(this);
        this.onUnitChange = this.onUnitChange.bind(this);

        this.state = updateSizeInfo(props);
    }

    componentWillReceiveProps(nextProps) {
        this.setState( updateSizeInfo(nextProps) );
    }



    onSizeChange(ev) {
        var displayValue = get(ev, 'target.value');
        var tmpDeg = sizeToDeg(displayValue, this.state.unit);
        var valid = (this.props.nullAllowed && !tmpDeg) || isSizeValid(tmpDeg, this.props.min, this.props.max);
        var value = tmpDeg; //(valid) ? tmpDeg: '';
        var stateUpdate = Object.assign({}, this.state, { displayValue,  value, valid });

        this.setState({displayValue, value, valid});
        this.props.onChange(ev, stateUpdate);
    }

    onUnitChange(ev) {
        var unit = get(ev, 'target.value');

        // update displayValue and unit
        if (unit !== this.state.unit) {
            var {value, valid, displayValue} = this.state;

            // in case current displayed value is invalid
            // try keep it if it is good for new unit
            if ( !valid ) {
                value = sizeToDeg(displayValue, unit);
                valid = (this.props.nullAllowed && !value) || isSizeValid(value, this.props.min, this.props.max);
                if (!valid) {
                    value = '';   // set back to empty string in case still invalid
                }
            } else {
                displayValue = sizeFromDeg(value, unit);
            }
            var stateUpdate = Object.assign({}, this.state, {unit, displayValue, value, valid});

            this.setState(stateUpdate);
            this.props.onChange(ev, stateUpdate);
        }
    }

    render() {
        var {displayValue, valid, unit} = this.state;
        var {min, max, wrapperStyle={}} = this.props;
        var sign = unitSign[unit];
        var minmsg = `${sizeFromDeg(min, unit)}`;
        var maxmsg = `${sizeFromDeg(max, unit)}`;
        var errmsg = `${invalidSizeMsg}, ${minmsg}-${maxmsg}${sign}.`;

        var showFeedback = () => {
            if (this.props.showFeedback) {
                var message = `Valid range between: ${minmsg}${sign} and ${maxmsg}${sign}`;

                return <p>{message}</p>;
            } else {
                return undefined;
            }
        };

        return (
            <div style={wrapperStyle}>
                <div style={{display: 'flex', alignItems: 'center', justifyContent: 'flex-start'}} >
                    <InputFieldView
                        valid={valid}
                        onChange={this.onSizeChange}
                        onBlur={this.onSizeChange}
                        onKeyPress={this.onSizeChange}
                        value={displayValue}
                        message={errmsg}
                        label={this.props.label}
                        labelWidth={this.props.labelWidth}
                        tooltip={'enter size within the valid range'}
                    />
                    <ListBoxInputFieldView
                        onChange={this.onUnitChange}
                        options={
                              [{label: 'degree', value: 'deg'},
                               {label: 'arcminute', value: 'arcmin'},
                               {label: 'arcsecond', value: 'arcsec'}
                               ]}
                        value={unit}
                        multiple={false}
                        labelWidth={2}
                        label={''}
                        tooltip={'unit of the size'}
                    />
                </div>
                {showFeedback()}
            </div>
        );
    }
}

SizeInputFieldView.propTypes = {
    unit:  PropTypes.string,
    min:   PropTypes.number.isRequired,
    max:   PropTypes.number.isRequired,
    displayValue: PropTypes.string,
    labelWidth: PropTypes.number,
    label:    PropTypes.string,
    nullAllowed: PropTypes.bool,
    onChange: PropTypes.func,
    value: PropTypes.string,
    valid: PropTypes.bool,
    showFeedback: PropTypes.bool,
    wrapperStyle: PropTypes.object
};

SizeInputFieldView.defaultProps = {
    label: 'Size: ',
    labelWidth: 50,
    unit: 'deg',
    showFeedback: false
};

export const SizeInputFields = fieldGroupConnector(SizeInputFieldView, getProps, propTypes, null);