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
        const {message, valid} = fieldState;
        if (shouldAct(e, actOn)) {
            const {valid, message, value:vadVal} = validator ? validator(value) : {valid: true, message: ''};
            if (vadVal && vadVal !== NOT_CELL_DATA) {
                value = vadVal;
            }
            const nMessage = valid ? '' : (label + message).replace('::', ':');
            onChange && onChange({fieldKey, valid, value, message: nMessage});
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


