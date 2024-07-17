import React, {useState} from 'react';
import {bool, string, object, shape, arrayOf, func, oneOf, element} from 'prop-types';
import {Autocomplete, FormControl, FormLabel, Stack, Tooltip} from '@mui/joy';
import {isArray, isEmpty, omit} from 'lodash';

import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {inputFieldTooltipProps} from 'firefly/ui/InputFieldView.jsx';

export function AutoCompleteInput({slotProps, orientation='horizontal', label, required, freeSolo=true, startDecorator, endDecorator, multiple, ...props}) {

    const {viewProps, fireValueChange}=  useFieldGroupConnector({...props, confirmValue: confirmValue(freeSolo)});
    const [open, setOpen] = useState(false);
    const [tooltipOpen, setTooltipOpen] = useState(false);

    const {value: fieldValue, valid} = viewProps;
    const fixedOptions = viewProps.options?.map((v) => ({...v, label: v.label || v.value}));

    const inputProps= omit(props, 'initialState', 'fieldKey', 'groupKey');
    const {title, enterDelay} = inputFieldTooltipProps(viewProps);

    // inputValue is always a string, it's the value displayed in the textbox of autocomplete
    const onInputChange = (e, inputValue) => {
        if (multiple) return;
        fireValueChange({value: inputValue}); //set in the FieldGroup store
    };

    // selectedValue is any object or list of objects (in multiple mode);
    // it's the value(s) selected by the user from options listbox or by pressing enter
    const onChange = (e, selectedValue) => {
        const fieldValue = isArray(selectedValue) //in case of multiple
            ? selectedValue.map((v) => v?.value ?? v).toString()
            : selectedValue?.value ?? selectedValue;
        fireValueChange({value: fieldValue}); //set in the FieldGroup store
    };

    const valStrToArr = (val) => val ? val.split(',') : [];

    // state logic becomes complicated when using freeSolo (custom option) with multiple option mode
    // also UX is not intuitive: user has to press enter to create a new chip for custom option
    const allowFreeSolo = multiple ? false : freeSolo;

    return (
        <Tooltip title={title} enterDelay={enterDelay}
                 open={tooltipOpen}
                 onOpen={()=> {
                     !open && setTooltipOpen(true); //don't show tooltip as long as popup is open
                 }}
                 onClose={()=> setTooltipOpen(false)}
                 {...slotProps?.tooltip}>
            <FormControl className='ff-Input' orientation={orientation} error={!valid} required={required} {...slotProps?.control}>
                {label && <FormLabel {...slotProps?.label}>{label}</FormLabel>}
                <Autocomplete autoComplete={true}
                              autoSelect={true}
                              multiple={multiple}
                              freeSolo={allowFreeSolo}
                              isOptionEqualToValue={(option, value) => (option?.value ?? option) === value}
                              value={multiple ? valStrToArr(fieldValue) : fieldValue}
                              onChange={onChange}
                              onInputChange={onInputChange}
                              {...inputProps}
                              title=''
                              startDecorator={startDecorator && <Decorator setOpen={setOpen}>{startDecorator}</Decorator>}
                              endDecorator={endDecorator && <Decorator setOpen={setOpen}>{endDecorator}</Decorator>}
                              options={fixedOptions}
                              open={open}
                              onOpen={() => {
                                  setOpen(true);
                                  setTooltipOpen(false); //hide tooltip as soon as popup opens
                              }}
                              onClose={() => setOpen(false)}/>
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
    multiple : bool,
    slotProps: shape({
        control: object,
        label: object,
        tooltip: object
    }),
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