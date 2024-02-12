import React, {memo} from 'react';
import {has} from 'lodash';
import {useFieldGroupConnector, fgConnectPropsTypes} from './FieldGroupConnector.jsx';
import {InputFieldView} from './InputFieldView.jsx';


function onChange(ev, validator, fireValueChange) {
    let value = ev.target.value;
    const {valid,message, ...others}= validator(value);
    has(others, 'value') && (value = others.value);    // allow the validator to modify the value.. useful in auto-correct.
    fireValueChange({ value, message, valid });
}


export const ValidationField = memo( (props) => {
        const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
        return (<InputFieldView {...viewProps}
                               onChange={(ev) => onChange(ev,viewProps.validator, fireValueChange)}/>);
});

ValidationField.propType= {
    ...fgConnectPropsTypes
};
