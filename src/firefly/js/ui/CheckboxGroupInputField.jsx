import React, {memo} from 'react';
import PropTypes, {object, shape} from 'prop-types';
import {Checkbox, FormControl, FormLabel, Stack, Switch, Tooltip} from '@mui/joy';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {splitVals} from 'firefly/tables/TableUtil.js';


const isChecked= (val,fieldValue) => (splitVals(fieldValue).indexOf(val) > -1);

const getCurrentValueArr= (v) => v ? splitVals(v) : [];


function convertValue(value,options) {
    if (value === '_all_') return options.map( (op) => op.value).toString();
    else if (!value || value === '_none_') return '';
    else return value;
}

export function CheckboxGroupInputFieldView({fieldKey, onChange, label, tooltip:toggleBoxTip, slotProps, type,
                                             options, alignment:orientation, value:fieldValue, sx}) {

    return (
        <Tooltip title={toggleBoxTip} sx={sx} {...slotProps?.tooltip}>
            <Stack orientation={orientation==='horizontal'?'row':'column'}>
                {label && (
                    <FormControl>
                        <FormLabel {...slotProps?.label}>{label}</FormLabel>
                    </FormControl>
                ) }
                <Stack className='ff-Checkbox-container' spacing={orientation==='vertical'?1:2} direction={orientation==='vertical' ? 'column' : 'row'}>
                    {options.map( ({value,label,tooltip}) => {
                        const cb= type==='switch' ?
                            (<Switch {...slotProps?.input}
                                       {...{ size:'sm', name:fieldKey, key:value, value,
                                           endDecorator: label,
                                           checked:isChecked(value,fieldValue), onChange, label, }} />)
                            :
                            (<Checkbox {...slotProps?.input}
                            {...{ className:'ff-Checkbox-item', size:'sm', name:fieldKey, key:value, value, checked:isChecked(value,fieldValue), onChange, label, }} />);
                        return tooltip ? <Tooltip {...{title:toggleBoxTip, key:value}}> {cb} </Tooltip> : cb;
                    })}
                </Stack>
            </Stack>
        </Tooltip>
    );
}

CheckboxGroupInputFieldView.propTypes= {
    options : PropTypes.array.isRequired,
    onChange:  PropTypes.func,
    alignment:  PropTypes.string,
    fieldKey:  PropTypes.string,
    value:  PropTypes.string.isRequired,
    label:  PropTypes.string,
    tooltip:  PropTypes.string,
    sx: object,
    slotProps: shape({
        input: object,
        label: object,
        tooltip: object
    })
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
    options : PropTypes.array.isRequired,
    alignment:  PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.string,
        tooltip: PropTypes.string,
        label:  PropTypes.string,
    }),
    forceReinit:  PropTypes.bool,
    fieldKey:   PropTypes.string,
    sx: object,

};

