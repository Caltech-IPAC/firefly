import React, {Component, PropTypes}  from 'react';
import {get} from 'lodash';

import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {convertAngle} from '../visualize/VisUtil.js';
import {InputFieldView} from './InputFieldView.jsx';
import {ListBoxInputFieldView} from './ListBoxInputField.jsx';
import Validate from '../util/Validate.js';


const invalidSizeMsg = 'size is out of range or not set yet';
const DECDIGIT = 6;
const unitSign = { 'arcsec':'"', 'arcmin':'\'', 'deg':' Deg' };

/*
 * remove trailing zero from toFixed result
 */
function toMaxFixed(floatNum, digits) {
    return parseFloat(floatNum.toFixed(digits));
}

// in string format, size in degree
export const sizeToDeg = (size, unit) => (
    (size)? toMaxFixed(convertAngle(((unit) ? unit :'deg'), 'deg', size), DECDIGIT).toString() : ''
);

// in string format, size from degree to any other unit
export const sizeFromDeg= (sizeDeg, unit) => (
    (sizeDeg)? toMaxFixed(convertAngle('deg', ((unit) ? unit :'deg'), sizeDeg), DECDIGIT).toString() : ''
);

// validate size (in degree) within the range
var isSizeValid = (sizeDeg,  min, max) => (
    (sizeDeg && Validate.floatRange(min, max, 1, 'value of radius size in degree', sizeDeg).valid) ? true : false
);

function updateSizeInfo(params) {
    var unit = params.unit ? params.unit : 'deg';
    var valid = isSizeValid(params.value, params.min, params.max);
    var value = (valid&&params.value) ? params.value  : '';
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

     fireValueChange({
         feedback: valid ? '' :  invalidSizeMsg,
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
    max:         PropTypes.number.isRequired
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
        var valid = isSizeValid(tmpDeg, this.props.min, this.props.max);
        var value = (valid) ? tmpDeg: '';
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
                valid = isSizeValid(value, this.props.min, this.props.max);
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
        var message = `Valid range between: ${sizeFromDeg(min, unit)}${sign} and ${sizeFromDeg(max, unit)}${sign}`;


        return (
            <div style={wrapperStyle}>
                <div style={{display: 'flex', alignItems: 'center', justifyContent: 'flex-start'}} >
                    <InputFieldView
                        valid={valid}
                        onChange={this.onSizeChange}
                        onBlur={this.onSizeChange}
                        onKeyPress={this.onSizeChange}
                        value={displayValue}
                        message={invalidSizeMsg}
                        label={this.props.label}
                        labelWidth={this.props.labelWidth}
                        tooltip={'enter size within the valid range'}
                    />
                    <ListBoxInputFieldView
                        onChange={this.onUnitChange}
                        options={
                              [{label: 'Degree', value: 'deg'},
                               {label: 'Arc Minutes', value: 'arcmin'},
                               {label: 'Arc Seconds', value: 'arcsec'}
                               ]}
                        value={unit}
                        multiple={false}
                        labelWidth={2}
                        label={''}
                        tooltip={'unit of the size'}
                    />
                </div>
                <p>{message}</p>
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
    onChange: PropTypes.func,
    value: PropTypes.string,
    valid: PropTypes.bool,
    wrapperStyle: PropTypes.object
};

SizeInputFieldView.defaultProps = {
    label: 'Size: ',
    labelWidth: 50,
    unit: 'deg'
};

export const SizeInputFields = fieldGroupConnector(SizeInputFieldView, getProps, propTypes, null);