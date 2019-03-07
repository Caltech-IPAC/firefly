import React, {useState, useEffect} from 'react';
import PropTypes from 'prop-types';

import {InputFieldView, propTypes} from './InputFieldView.jsx';
import {NOT_CELL_DATA} from '../tables/ui/TableRenderer.js';        // this is not right.. should revisit


function shouldAct(e, actOn) {
    if (e.type.startsWith('key') ) {
        return (actOn.includes('enter') && e.key === 'Enter');
    } else if (e.type.startsWith('blur')) {
        return  actOn.includes('blur');
    } else {
        return actOn.includes('changes');
    }
}

export const InputFieldActOn = React.memo(({View, fieldKey, valid=true, value, onChange, validator, actOn, ...others}) => {
    const [fieldState, setFieldState] = useState({valid, value, message:''});

    useEffect(() => {
        setFieldState({...fieldState, value});
    }, [value]);

    const handleChanges = (e) => {
        var {label=''} = others;
        let value = e.target.value;
        let {message, valid} = fieldState;
        if (shouldAct(e, actOn)) {
            if (validator) {
                const rval = validator(value) || {};
                valid = rval.valid;
                message = valid ? '' : (label + rval.message).replace('::', ':');
                if (rval.value !== NOT_CELL_DATA) {
                    value = rval.value;
                }
            }
            onChange && onChange({fieldKey, valid, value, message});
        }

        setFieldState({valid, value, message});
    };

    return (
        <View
            {...others}
            {...fieldState}
            onChange={handleChanges}
            onBlur={handleChanges}
            onKeyPress={handleChanges}
        />
    );
});


export const InputField = (props) => <InputFieldActOn View={InputFieldView} {...props}/>;

InputField.propTypes = {
    ...propTypes,
    fieldKey: PropTypes.string,
    onChange: PropTypes.func,
    actOn: PropTypes.arrayOf(PropTypes.oneOf(['blur', 'enter', 'changes'])),
    validator: PropTypes.func
};

InputField.defaultProps = {
    fieldKey:'undef',
    value: '',
    showWarning : true,
    actOn: ['changes'],
    labelWidth: 0,
    inline: false,
    visible: true
};


