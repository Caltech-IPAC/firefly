import React, {memo, PureComponent} from 'react';
import PropTypes from 'prop-types';

import {InputAreaFieldView, propTypes} from './InputAreaFieldView.jsx';
import {useFieldGroupConnector} from './FieldGroupEnable';
import {InputFieldActOn} from './InputField';

export const InputAreaField = (props) => <InputFieldActOn View={InputAreaFieldView} {...props}/>;


function onChange(ev, validator, fireValueChange) {
    const {valid,message}= validator(ev.target.value);
    fireValueChange({ value : ev.target.value, message, valid });
}


export const InputAreaFieldConnected = memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    return (<InputAreaFieldView {...viewProps} onChange={(ev) => onChange(ev,viewProps.validator, fireValueChange)}/>);
});



InputAreaFieldConnected.defaultProps = {
    showWarning: true,
    actOn: ['changes'],
    labelWidth: 0,
    value: '',
    inline: false,
    visible: true,
    rows: 10,
    cols: 50
};

InputAreaFieldConnected.propTypes = {
    ...propTypes,
    fieldKey: PropTypes.string,
    onChange: PropTypes.func,
    actOn: PropTypes.arrayOf(PropTypes.oneOf(['blur', 'enter', 'changes'])),
    validator: PropTypes.func,
    value: PropTypes.string
};

