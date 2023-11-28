import React, {memo} from 'react';
import PropTypes, {bool, array, string, number, object, shape, oneOfType, element} from 'prop-types';
import {isEmpty, isUndefined, get}  from 'lodash';
import {RadioGroupInputFieldView} from './RadioGroupInputFieldView.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';


const assureValue= (props) => {
    const {value,options, defaultValue}= props;
    if (value) return value;
    return isUndefined(defaultValue) ? options?.[0].value : defaultValue;
};

function handleOnChange(ev, params, fireValueChange) {
    const val = get(ev, 'target.value', '');
    const checked = get(ev, 'target.checked', false);
    if (checked) {
        fireValueChange({ value: val, valid: true});
    }
}

function checkForUndefined(v,props) {
    const {options=[], defaultValue, isGrouped=false} = props;
    const optionContain = (v) => Boolean(v && options.find((op) => op.value === v));
    if (isEmpty(options) || optionContain(v) || isGrouped) {
        return v;
    } else {
        return isUndefined(defaultValue) ? options[0].value : defaultValue;
    }
}

export const RadioGroupInputField= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector({...props, confirmValue:checkForUndefined});
    const value = assureValue(viewProps);
    if (isUndefined(value)) return <div/>;
    const newProps= {...viewProps,  value};
    return (<RadioGroupInputFieldView {...newProps}
                                     onChange={(ev) => handleOnChange(ev,viewProps, fireValueChange)}/>) ;
});


RadioGroupInputField.propTypes= {
    options: array,
    defaultValue: string,
    orientation: PropTypes.oneOf(['vertical', 'horizontal']),
    tooltip : oneOfType([string,element]),
    isGrouped: bool,
    initialState: shape({
        value: string,
        label:  string,
    }),
};
