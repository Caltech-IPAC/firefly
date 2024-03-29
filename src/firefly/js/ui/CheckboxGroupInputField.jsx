import React, {memo} from 'react';
import {element, bool, object, shape, oneOfType, string, func, arrayOf} from 'prop-types';
import {Checkbox, FormControl, FormLabel, Stack, Tooltip} from '@mui/joy';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {splitVals} from 'firefly/tables/TableUtil.js';


const isChecked= (val,fieldValue) => (splitVals(fieldValue).indexOf(val) > -1);

const getCurrentValueArr= (v) => v ? splitVals(v) : [];


function convertValue(value,options) {
    if (value === '_all_') return options.map( (op) => op.value).toString();
    else if (!value || value === '_none_') return '';
    else return value;
}

export function CheckboxGroupInputFieldView({fieldKey, onChange, label, tooltip:toggleBoxTip, slotProps,
                                             options, alignment:orientation, value:fieldValue, sx}) {
    return (
        <Tooltip title={toggleBoxTip} sx={sx} {...slotProps?.tooltip}>
            <FormControl orientation={orientation}  {...slotProps?.control}>
                {label && <FormLabel {...slotProps?.label}>{label}</FormLabel>}
                {/*following should be nested in a <FormGroup/> once joy-ui exposes it: https://mui.com/material-ui/react-checkbox/#formgroup*/}
                <Stack className='ff-Checkbox-container' spacing={orientation==='vertical'?1:2} direction={orientation==='vertical' ? 'column' : 'row'}>
                    {options.map( ({value,label,tooltip}) => {
                        const cb= (
                            <Checkbox {...{ className:'ff-Checkbox-item', size:'sm', name:fieldKey, key:value, value,
                                          checked:isChecked(value,fieldValue), onChange, label, ...slotProps?.input}} />
                        );
                        return (
                            <FormControl key={value}>{
                                //until https://github.com/mui/material-ui/issues/37764 & related issues are fixed,
                                //we need to wrap `cb` in a FormControl as a workaround: https://stackoverflow.com/a/66738444/8252556
                                tooltip
                                    ? <Tooltip {...{title: toggleBoxTip, key: value}}> {cb} </Tooltip>
                                    : cb
                            }</FormControl>
                        );
                    })}
                </Stack>
            </FormControl>
        </Tooltip>
    );
}

CheckboxGroupInputFieldView.propTypes= {
    options: arrayOf(shape( { value: string, label: string, tooltip: string} )).isRequired,
    onChange:  func,
    alignment:  string,
    fieldKey:  string,
    value:  string.isRequired,
    label:  string,
    sx: object,
    tooltip: oneOfType([string,element]),
    slotProps: shape({
        control: object,
        input: object,
        label: object,
        tooltip: oneOfType([string,element]),
    }),
};



function handleOnChange(ev, viewProps, fireValueChange) {
    // when a checkbox is checked or unchecked
    // the array, representing the value of the group,
    // needs to be updated
    const value= convertValue(viewProps.value,viewProps.options);

    const val = ev.target.value;
    const checked = ev.target.checked;
    const curValueArr = getCurrentValueArr(value);
    const idx = curValueArr.indexOf(val);
    if (checked) {
        if (idx < 0) curValueArr.push(val); // add val to the array
    }
    else {
        if (idx > -1) curValueArr.splice(idx, 1); // remove val from the array
    }
    const {valid,message} = viewProps.validator(curValueArr.toString());

    fireValueChange({ value: curValueArr.toString(), message, valid });

    if (viewProps.onChange) {
        viewProps.onChange(ev);
    }
}

export const CheckboxGroupInputField = memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    const newProps= {
        ...viewProps,
        value: convertValue(viewProps.value,viewProps.options),
        onChange: (ev) => handleOnChange(ev,viewProps, fireValueChange)
    };
    return ( <CheckboxGroupInputFieldView {...newProps} /> );
});

CheckboxGroupInputField.propTypes= {
    options: arrayOf(shape( { value: string, label: string, tooltip: string} )).isRequired,
    alignment:  string,
    initialState: shape({
        value: string,
        tooltip: string,
        label:  string,
    }),
    tooltip: oneOfType([string,element]),
    forceReinit:  bool,
    fieldKey:   string,
    sx: object,

};

