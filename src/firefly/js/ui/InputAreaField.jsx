import React, {forwardRef} from 'react';
import {omit} from 'lodash';
import PropTypes from 'prop-types';

import {InputAreaFieldView, propTypes} from './InputAreaFieldView.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {InputFieldActOn} from './InputField';

export const InputAreaField = (props) => <InputFieldActOn View={InputAreaFieldView} {...props}/>;


function onChange(ev, validator, fireValueChange) {
    const {valid,message}= validator(ev.target.value);
    fireValueChange({ value : ev.target.value, message, valid });
}


export const InputAreaFieldConnected =
    forwardRef( ({showWarning=true, actOn=['changes'], visible=true ,...rest}, ref) => {
        const {viewProps, fireValueChange}=  useFieldGroupConnector({showWarning,actOn,visible,...rest});
        return (<InputAreaFieldView ref={ref} {...viewProps} onChange={(ev) => onChange(ev,viewProps.validator, fireValueChange)}/>);
    });


InputAreaFieldConnected.propTypes = {
    ...omit(propTypes, ['value']),
    fieldKey: PropTypes.string,
    onChange: PropTypes.func,
    actOn: PropTypes.arrayOf(PropTypes.oneOf(['blur', 'enter', 'changes'])),
    validator: PropTypes.func,
    initialState: PropTypes.shape({
        value: PropTypes.string,
    }),
    additionalClasses: PropTypes.string,
};

