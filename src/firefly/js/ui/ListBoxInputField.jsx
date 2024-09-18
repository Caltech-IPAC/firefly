import React, {memo} from 'react';
import {number, object, shape, oneOf, array, any, string, func, bool, element} from 'prop-types';
import {Select, Option, Tooltip, FormControl, FormLabel, Stack} from '@mui/joy';
import {isArray, isEmpty, isFunction} from 'lodash';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';


function getCurrentValueArr(value) {
    if (value) {
        return (typeof value === 'string') ? value.split(',') : [value];
    }
    else {
        return [];
    }
}

const convertValue= (value,options) => (!value) ? options?.[0]?.value : value;


export function ListBoxInputFieldView({value:fieldValue='', onChange, fieldKey, options,
                                          orientation='horizontal', sx, slotProps={},
                                          renderValue, decorator, startDecorator,
                                          forceLabelToValue=false,
                                       multiple, placeholder, tooltip, label,
                                          readonly=false, size}) {

    const vAry= getCurrentValueArr(fieldValue);
    return (
        <Stack {...{className:'ff-Input ListBoxInputFieldView', sx}}>
            <FormControl {...{orientation, ...slotProps?.control}}>
                {label && <FormLabel {...slotProps?.label}>{label}</FormLabel>}
                <Tooltip {...{title:tooltip, placement:'top', ...slotProps?.tooltip}}>
                    <Select {...{name: fieldKey, multiple, onChange, placeholder, renderValue, startDecorator,
                        disabled: readonly, size, value: multiple ? vAry : fieldValue,
                        ...slotProps?.input}}>
                        {options?.map((({value,label,disabled=false},idx) => {
                            return (
                                <Option {...{value, key:`k${idx}`, label:forceLabelToValue?value:label, disabled}}>
                                    {isFunction(decorator) ? decorator(label,value) : (label || value)}
                                </Option>
                            );
                        }))}
                    </Select>
                </Tooltip>
            </FormControl>
        </Stack>
    );
}



ListBoxInputFieldView.propTypes= {
    options : array,
    value:  any,
    fieldKey : string,
    onChange:  func,
    multiple : bool,
    forceLabelToValue: bool,
    label:  string,
    tooltip:  string,
    selectStyle: object,
    labelStyle: object,
    placeholder : string,
    readonly: bool,
    sx: object,
    orientation: string,
    renderValue: func,
    decorator: func,
    startDecorator: element,
    size: string,
    slotProps: shape({
        input: object,
        control: object,
        label: object,
    }),
};

function handleOnChange(ev, newValue, params, fireValueChange) {
    const options = ev?.target?.options;
    let value;
    if (isArray(options)) {
        const valAry = [];
        for (var i = 0; i<options.length; i++) {
            if (options[i].selected) {
                valAry.push(options[i].value);
            }
        }
        value= valAry.toString();
    }
    else {
        value= isArray(newValue) ? newValue.toString() : newValue;
    }

    const {valid,message}=params.validator(value);
    // the value of this input field is a string
    fireValueChange({ value, message, valid });
    if (params.onChange) params.onChange(ev);
}



function checkForUndefined(v,props) {
    const multiple = props.multiple || false;

    return isEmpty(props.options) ? v :
            (!v && !multiple ? props.options[0].value : v);
}


export const ListBoxInputField= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector({...props, confirmValue:checkForUndefined});
    const newProps= {
        ...viewProps,
        value: !viewProps.multiple ? convertValue(viewProps.value, viewProps.options) : viewProps.value,
        onChange: (ev,newValue) => handleOnChange(ev,newValue,viewProps, fireValueChange)
    };
    return (<ListBoxInputFieldView {...newProps} />);
});



ListBoxInputField.propTypes= {
    fieldKey : string,
    groupKey : string,
    placeholder : string,
    forceReinit:  bool,
    initialState: shape({
        value: string,
        tooltip: string,
        label:  string,
    }),
    slotProps: shape({
        input: object,
        control: object,
        label: object,
    }),
    renderValue: func,
    decorator: func,
    startDecorator: element,
    options : array,
    multiple : bool,
    orientation: oneOf(['vertical', 'horizontal']),
    readonly: bool,
    sx: object,
};

