import React, {useState} from 'react';
import {bool, string, object, shape, arrayOf, func, oneOf, element} from 'prop-types';
import {Autocomplete, FormControl, FormLabel, Stack, Tooltip} from '@mui/joy';
import {isEmpty, omit} from 'lodash';

import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {inputFieldTooltipProps} from 'firefly/ui/InputFieldView.jsx';


export function AutoCompleteInput({slotProps, orientation, label, required, freeSolo=true, startDecorator, endDecorator, ...props}) {

    const {viewProps, fireValueChange}=  useFieldGroupConnector({...props, confirmValue: confirmValue(freeSolo)});
    const [open, setOpen] = useState(false);

    const {value, valid} = viewProps;
    const fixedOptions = viewProps.options?.map((v) => ({...v, label: v.label || v.value}));

    const inputProps= omit(props, 'initialState', 'fieldKey', 'groupKey');
    let {title, ...tooltipProps} = inputFieldTooltipProps(viewProps);
    title = open ? '' : title;          // remove tooltips when popup is open

    const onChange = (e,v) => {
        const value = v.value ?? v;
        fireValueChange({value});
    };


    return (
        <Tooltip title={title} {...tooltipProps} {...slotProps?.tooltip}>
            <FormControl className='ff-Input' orientation={orientation} error={!valid} required={required} {...slotProps?.control}>
                {label && <FormLabel {...slotProps?.label}>{label}</FormLabel>}
                <Autocomplete autoComplete={true}
                              autoSelect={true}
                              freeSolo={freeSolo}
                              value={value}
                              {...inputProps}
                              title=''
                              startDecorator={startDecorator && <Decorator setOpen={setOpen}>{startDecorator}</Decorator>}
                              endDecorator={endDecorator && <Decorator setOpen={setOpen}>{endDecorator}</Decorator>}
                              options={fixedOptions}
                              open={open}
                              onOpen={() => setOpen(true)}
                              onClose={() => setOpen(false)}
                              onInputChange={onChange}/>
            </FormControl>
        </Tooltip>
    );

}

AutoCompleteInput.propTypes= {
    fieldKey: string.isRequired,
    fieldGroup: string,
    initialState: shape({
        value: string,
        valid: bool,
        message: string,
        validator: func,
        nullAllowed: bool,
    }),
    options: arrayOf(object),       // {value,label,...anything-else}
    label: string,
    title: string,
    orientation: oneOf(['horizontal', 'vertical']),
    required: bool,
    freeSolo: bool,                 // allow values not in options
    endDecorator: element,
    startDecorator: element,
    slotProps: shape({
        control: object,
        label: object,
        tooltip: object
    }),
};

AutoCompleteInput.defaultProps = {
    orientation: 'horizontal'
};

// may not be needed
function confirmValue(freeSolo) {
    return (v,props) => {
        const {options=[], defaultValue} = props;
        const optionContain = (v) => Boolean(v && options.find((op) => op.value === v));
        if (freeSolo || isEmpty(options) || optionContain(v)) {
            return v;
        } else {
            return defaultValue ?? options[0].value;
        }
    };
}


function Decorator({children, setOpen}) {
    return (
        <Stack onClick={() => setOpen(false)}>
            {children}
        </Stack>

    );
}