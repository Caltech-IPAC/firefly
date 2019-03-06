import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';

import {InputAreaFieldView, propTypes} from './InputAreaFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {InputFieldActOn} from './InputField';

export const InputAreaField = (props) => <InputFieldActOn View={InputAreaFieldView} {...props}/>;


InputAreaField.propTypes = {
    ...propTypes,
    fieldKey: PropTypes.string,
    onChange: PropTypes.func,
    actOn: PropTypes.arrayOf(PropTypes.oneOf(['blur', 'enter', 'changes'])),
    validator: PropTypes.func,
    value: PropTypes.string
};

InputAreaField.defaultProps = {
    showWarning: true,
    actOn: ['changes'],
    labelWidth: 0,
    value: '',
    inline: false,
    visible: true,
    rows: 10,
    cols: 50
};

function onChange(ev, store, fireValueChange) {

    var {valid,message}= store.validator(ev.target.value);

    fireValueChange({ value : ev.target.value, message, valid });
}

function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            onChange: (ev) => onChange(ev, params, fireValueChange),
            value: String(params.value)
        });
}


export const InputAreaFieldConnected = fieldGroupConnector(InputAreaFieldView, getProps, InputAreaField.propTypes);
