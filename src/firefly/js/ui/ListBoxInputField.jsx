import React, {memo} from 'react';
import PropTypes, {object} from 'prop-types';
import {Select, Option, Tooltip, FormControl, FormLabel, Stack} from '@mui/joy';
import {get, isArray, isEmpty} from 'lodash';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';

import InputFieldLabel from './InputFieldLabel.jsx';


function getCurrentValueArr(value) {
    if (value) {
        return (typeof value === 'string') ? value.split(',') : [value];
    }
    else {
        return [];
    }
}

const convertValue= (value,options) => (!value) ? get(options, [0, 'value']) : value;


export function ListBoxInputFieldView({value:fieldValue, onChange, fieldKey, options,
                                          orientation='horizontal', sx,
                                       multiple, placeholder, tooltip, label,
                                          readonly=false}) {

    const vAry= getCurrentValueArr(fieldValue);
    return (
        <Stack {...{className:'ff-Input ListBoxInputFieldView', sx}}>
            <FormControl {...{orientation}}>
                {label && <FormLabel>{label}</FormLabel>}
                <Tooltip {...{title:tooltip, placement:'top'}}>
                    <Select {...{name: fieldKey, multiple, onChange, placeholder,
                        disabled: readonly, value: multiple ? vAry : fieldValue}}>
                        {options?.map((({value,label,disabled=false},idx) => {
                            return (
                                <Option {...{value, key:`k${idx}`, disabled:disabled ? 'disabled' : false}}>
                                    {label || value}
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
    options : PropTypes.array,
    value:  PropTypes.any,
    fieldKey : PropTypes.string,
    onChange:  PropTypes.func,
    inline : PropTypes.bool,
    multiple : PropTypes.bool,
    label:  PropTypes.string,
    tooltip:  PropTypes.string,
    labelWidth : PropTypes.number,
    selectStyle: PropTypes.object,
    wrapperStyle: PropTypes.object,
    labelStyle: PropTypes.object,
    placeholder : PropTypes.string,
    readonly: PropTypes.bool,
    sx: PropTypes.object,
};

function handleOnChange(ev, newValue, params, fireValueChange) {
    const options = ev.target.options;
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
    fieldKey : PropTypes.string,
    groupKey : PropTypes.string,
    placeholder : PropTypes.string,
    forceReinit:  PropTypes.bool,
    initialState: PropTypes.shape({
        value: PropTypes.string,
        tooltip: PropTypes.string,
        label:  PropTypes.string,
    }),
    inline : PropTypes.bool,
    options : PropTypes.array,
    multiple : PropTypes.bool,
    labelWidth : PropTypes.number,
    orientation: PropTypes.oneOf(['vertical', 'horizontal']),
    readonly: PropTypes.bool,
    sx: PropTypes.object,
};

