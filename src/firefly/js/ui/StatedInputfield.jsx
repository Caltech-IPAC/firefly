import React, {useState, useEffect} from 'react';
import PropTypes, {element} from 'prop-types';
import {has} from 'lodash';

import {InputFieldView} from './InputFieldView.jsx';


export function StateInputField({defaultValue, visible=true, message, label='', tooltip, showWarning, type,
                                    startDecorator, endDecorator,
                                    orientation, sx={}, onKeyDown, onKeyUp, validator, valueChange}) {

    const [value, setValue] = useState(defaultValue);
    const [valid, setValid] = useState(true);

    const onChange= (ev) => {
        let newValue = ev.target.value;
        const {valid:newValidState,message, ...others}= validator ? validator(newValue) : {valid:true, message:''};
        has(others, 'value') && (newValue = others.value);    // allow the validator to modify the value.. useful in auto-correct.
        valueChange?.({ value:newValue, message, valid:newValidState });
        setValid(newValidState);
        setValue(newValue);
    };

    useEffect(() => {  // if the default value is replaced then because the new value
        const newValidState= validator?.(defaultValue)?.valid ?? true;
        setValue(defaultValue);
        setValid(newValidState);
    }, [defaultValue]);

    return (
        <InputFieldView {...{valid, visible, message, onChange, label, value, tooltip, type,
            startDecorator, endDecorator,
            orientation, showWarning, sx, onKeyDown, onKeyUp}}
        />
    );
}

StateInputField.propTypes= {
    message : PropTypes.string,
    tooltip : PropTypes.string,
    label : PropTypes.string,
    inline : PropTypes.bool,
    orientation: PropTypes.string,
    sx: PropTypes.object,
    defaultValue: PropTypes.string.isRequired,
    size : PropTypes.number,
    showWarning : PropTypes.bool,
    type: PropTypes.string,
    validator: PropTypes.func,
    startDecorator: element,
    endDecorator: element,
    onKeyUp: PropTypes.func,
    onKeyDown: PropTypes.func,
    valueChange: PropTypes.func.isRequired,
    visible: PropTypes.bool,
};
