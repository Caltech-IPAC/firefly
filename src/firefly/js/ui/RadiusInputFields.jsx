import React, {Component, PropTypes}  from 'react';
import {get} from 'lodash';

import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {convertAngle} from '../visualize/VisUtil.js';
import {InputFieldView} from './InputFieldView.jsx';
import {ListBoxInputFieldView} from './ListBoxInputField.jsx';
import Validate from '../util/Validate.js';


const invalidSizeMsg = 'size is out of range';
const DECDIGIT = 6;
const unitSign = { 'arcsec':'"', 'arcmin':'\'', 'deg':' Deg' };

/*
 * remove trailing zero from toFixed result
 */
function toMaxFixed(floatNum, digits) {
    return parseFloat(floatNum.toFixed(digits));
}

// in string format, size in degree
var sizeToDeg = (size, unit) => (
    (size)? toMaxFixed(convertAngle(((unit) ? unit :'deg'), 'deg', size), DECDIGIT).toString() : ''
);

// in string format, size from degree to any other unit
var sizeFromDeg= (sizeDeg, unit) => (
    (sizeDeg)? toMaxFixed(convertAngle('deg', ((unit) ? unit :'deg'), sizeDeg), DECDIGIT).toString() : ''
);

// validate size (in degree) within the range
var isSizeValid = (sizeDeg,  min, max) => (
    (sizeDeg && Validate.floatRange(min, max, 1, 'value of radius size in degree', sizeDeg).valid) ? true : false
);


function getProps(params, fireValueChange) {

    var unit = params.unit ? params.unit : 'deg';
    var valid = isSizeValid(params.value, params.min, params.max);
    var value = (valid&&params.value) ? params.value  : '';
    var displayValue = params.displayValue ? params.displayValue : sizeFromDeg(value, unit);

    return Object.assign({}, params,
        {
            onChange: (ev) => handleOnChange(ev, params, fireValueChange),
            unit,
            displayValue,
            value,
            valid
        });
}

function handleOnChange(ev, params, fireValueChange) {
     var sizeInfo = Object.assign({}, {displayValue: '', unit: 'deg', value: '', valid: true}, ev.sizeInfo);
     var {displayValue, unit, value, valid} = sizeInfo;

     fireValueChange({
         feedback: valid ? invalidSizeMsg: '',
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

class RadiusInputFieldView extends Component {
    constructor(props) {
        super(props);

        this.updateState(props);

        this.onSizeChange = this.onSizeChange.bind(this);
        this.onUnitChange = this.onUnitChange.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        this.updateState(nextProps);
    }

    updateState(props) {
        var unit = props.unit ? props.unit : 'deg';
        var valid = isSizeValid(props.value, props.min, props.max);
        var value = (valid&&props.value) ? props.value  : '';
        var displayValue = props.displayValue ? props.displayValue : sizeFromDeg(value, unit);

        this.state = {
            value,
            unit,
            displayValue,
            valid
        };
    }

    onSizeChange(ev) {
        var displayValue = get(ev, 'target.value');

        //console.log('type '+ev.type + ' displayValue '+displayValue);

        // validation test is determined when the typing is done
        // update displayVvalue, value, and valid
        if (ev.type.startsWith('blur') || (ev.type.startsWith('key') && ev.key === 'Enter')) {
            var tmpDeg = sizeToDeg(displayValue, this.state.unit);
            var valid = isSizeValid(tmpDeg, this.props.min, this.props.max);
            var value = (valid) ? tmpDeg: '';
            var stateUpdate = Object.assign({}, this.state, { displayValue,  value, valid });

            this.setState(stateUpdate);
            this.props.onChange(Object.assign({}, ev, {sizeInfo: stateUpdate }));
        } else {
            this.setState( { displayValue, valid: true });
        }
    }

    onUnitChange(ev) {
        var unit = get(ev, 'target.value');

        // update displayValue and unit
        if (unit !== this.state.unit) {
            var displayValue = sizeFromDeg(this.state.value, unit);
            var stateUpdate = Object.assign({}, this.state, {unit, displayValue});

            this.setState({unit, displayValue});
            this.props.onChange(Object.assign({}, ev, {sizeInfo: stateUpdate}));
        }
    }

    render() {
        var minInUnit = sizeFromDeg(this.props.min, this.state.unit);
        var maxInUnit = sizeFromDeg(this.props.max, this.state.unit);
        var sign = unitSign[this.state.unit];
        var message = `Valid range between: ${minInUnit}${sign} and ${maxInUnit}${sign}`;
        var {displayValue, valid, unit} = this.state;

        return (
            <div >
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

RadiusInputFieldView.propTypes = {
    unit:  PropTypes.string,
    min:   PropTypes.number.isRequired,
    max:   PropTypes.number.isRequired,
    displayValue: PropTypes.string,
    labelWidth: PropTypes.number,
    label:    PropTypes.string,
    onChange: PropTypes.func,
    value: PropTypes.string,
    valid: PropTypes.bool
};

RadiusInputFieldView.defaultProps = {
    label: 'Size: ',
    labelWidth: 50,
    unit: 'deg'
};

export const RadiusInputFields = fieldGroupConnector(RadiusInputFieldView, getProps, propTypes, null);