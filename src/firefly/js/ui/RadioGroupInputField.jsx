import React, {memo} from 'react';
import {bool, array, string, number, object, shape} from 'prop-types';
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
    inline : bool,
    options: array,
    defaultValue: string,
    alignment:  string,
    labelWidth : number,
    labelStyle: object,
    isGrouped: bool,
    forceReinit:  bool,
    fieldKey:   string,
    initialState: shape({
        value: string,
        tooltip: string,
        label:  string,
    }),
};
