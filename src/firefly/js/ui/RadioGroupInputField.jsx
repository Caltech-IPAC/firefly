import React from 'react';
import PropTypes from 'prop-types';
import {isEmpty, isUndefined, get}  from 'lodash';
import {RadioGroupInputFieldView} from './RadioGroupInputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';


const convertValue= (value,options,defaultValue) => {
    if (!value) {
        return isUndefined(defaultValue) ? options[0].value : defaultValue;
    } else {
        return value;
    }
};

function getProps(params, fireValueChange) {

    var {value,options,defaultValue}= params;
    value= convertValue(value,options,defaultValue);

    return Object.assign({}, params,
        {
            value,
            onChange: (ev) => handleOnChange(ev,params, fireValueChange)
        });
}

function handleOnChange(ev, params, fireValueChange) {
    var val = get(ev, 'target.value', '');
    var checked = get(ev, 'target.checked', false);

    if (checked) {
        fireValueChange({ value: val, valid: true});
        if (params.onChange) {
            params.onChange(val);
        }
    }
}


const propTypes= {
    inline : PropTypes.bool,
    options: PropTypes.array.isRequired,
    defaultValue: PropTypes.string,
    alignment:  PropTypes.string,
    labelWidth : PropTypes.number,
    labelStyle: PropTypes.object,
    isGrouped: PropTypes.bool
};

function checkForUndefined(v,props) {
    const {options, defaultValue, isGrouped=false} = props;
    var optionContain = (v) => v && options.find((op) => op.value === v);
    if (isEmpty(options) || optionContain(v) || isGrouped) {
        return v;
    } else {
        return isUndefined(defaultValue) ? options[0].value : defaultValue;
    }
}


export const RadioGroupInputField= fieldGroupConnector(RadioGroupInputFieldView,
    getProps,propTypes,null,checkForUndefined);

