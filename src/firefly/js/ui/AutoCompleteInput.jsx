import React, {memo, useEffect, useRef, useState} from 'react';
import {bool, string, object, shape, arrayOf, func, oneOf, oneOfType, element, node, any} from 'prop-types';
import {Autocomplete, FormControl, FormLabel, Stack, Tooltip} from '@mui/joy';
import {isArray, isEmpty, omit} from 'lodash';

import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {inputFieldTooltipProps} from 'firefly/ui/InputFieldView.jsx';
import {logger} from '../util/Logger.js';


// firefly / field-group concerns that must not be spread onto the Joy <Autocomplete>
const VIEW_OMIT_LIST= ['initialState', 'fieldKey', 'groupKey', 'validator', 'fireValueChange',
    'displayValue', 'visible', 'tooltip', 'showWarning', 'nullAllowed', 'labelWidth',
    'confirmValue', 'confirmValueOnInit', 'forceReinit'];

// options may be bare strings; spreading 'raw' would yield {0:'r',1:'a',2:'w',label:undefined}
const toOption= (v) => typeof v === 'string' ? {value:v, label:v} : {...v, label: v.label || v.value};

const defIsOptionEqualToValue= (option, value) => (option?.value ?? option) === value;

const valStrToArr= (val) => val ? val.split(',') : [];


export function AutoCompleteInputView({slotProps, orientation='horizontal', label, required, freeSolo=true,
                                          startDecorator, endDecorator, multiple,
                                          value:fieldValue='', valid=true, message='', options, validator,
                                          fireValueChange, isOptionEqualToValue= defIsOptionEqualToValue,
                                          disableClearable= !multiple,
                                          onChange:onChangeProp, ...props}) {

    const [open, setOpen] = useState(false);
    const [tooltipOpen, setTooltipOpen] = useState(false);

    // control/label/tooltip are consumed here, anything else (listbox, option, input, ...) is Joy's
    const {control:controlSlot, label:labelSlot, tooltip:tooltipSlot, ...acSlotProps}= slotProps ?? {};
    const fixedOptions = options?.map(toOption);

    const inputProps= omit(props, VIEW_OMIT_LIST);
    const {title, enterDelay} = inputFieldTooltipProps({valid, message, tooltip:props.tooltip, showWarning:props.showWarning});

    // like the other field views, this one owns calling the validator - the store does not run it on a value change
    const fireValidatedChange = (value) =>
        fireValueChange(validator ? {value, ...validator(value)} : {value}); //set in the FieldGroup store

    // handles an edge case where Joy overwrites the textbox with the picked option's label and only
    // repairs it when `value` changed - so an unchanged value leaves the field showing the bare label
    const syncInputToValue = Boolean(onChangeProp) && !multiple;

    // inputValue is always a string, it's the value displayed in the textbox of autocomplete
    const onInputChange = (e, inputValue, reason) => {
        if (multiple) return;
        // only 'input' is a user edit; every other reason is joy resetting its own textbox after a
        // pick/blur, and writing those back would store the option's bare label (and re-validate it)
        if (reason!=='input') return;
        // like the other field views, keystrokes carry validity - a field goes red mid-word
        fireValidatedChange(inputValue);
    };

    // selectedValue is any object or list of objects (in multiple mode);
    // it's the value(s) selected by the user from options listbox or by pressing enter
    const onChange = (e, selectedValue, reason, details) => {
        // caller owns the selection -> stored-value mapping: supplying onChange replaces both the
        // default write below and the validator for picks. Typing still writes via onInputChange.
        if (onChangeProp) {
            onChangeProp(e, selectedValue, reason, {...details, value:fieldValue, fireValueChange});
            return;
        }
        const value = isArray(selectedValue) //in case of multiple
            ? selectedValue.map((v) => v?.value ?? v).toString()
            : selectedValue?.value ?? selectedValue;
        fireValidatedChange(value);
    };

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
                 {...tooltipSlot}>
            <FormControl className='ff-Input' orientation={orientation} error={!valid} required={required} {...controlSlot}>
                {label && <FormLabel {...labelSlot}>{label}</FormLabel>}
                <Autocomplete autoComplete={true}
                              multiple={multiple}
                              freeSolo={allowFreeSolo}
                              isOptionEqualToValue={isOptionEqualToValue}
                              disableClearable={disableClearable}
                              value={multiple ? valStrToArr(fieldValue) : fieldValue}
                              inputValue={syncInputToValue ? (fieldValue ?? '') : undefined}
                              onChange={onChange}
                              onInputChange={onInputChange}
                              {...inputProps}
                              slotProps={acSlotProps}
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


AutoCompleteInputView.propTypes= {
    value: any,
    valid: bool,
    message: string,
    tooltip: string,
    showWarning: bool,
    validator: func,                // (value) => ({valid, message}), run on every keystroke and on a selection
    fireValueChange: func.isRequired,
    options: arrayOf(oneOfType([string,object])),   // string, or {value,label,...anything-else}
    label: node,
    title: string,
    orientation: oneOf(['horizontal', 'vertical']),
    required: bool,
    freeSolo: bool,                 // allow values not in options
    endDecorator: element,
    startDecorator: element,
    multiple : bool,
    loading: bool,
    // a single-value field reads as a text input, where the clear 'x' is redundant and was never there
    // pre-joy; in multiple mode it is the only one-gesture way to drop all the chips, so it stays on
    disableClearable: bool,         // default: !multiple
    isOptionEqualToValue: func,     // (option,value) => boolean
    onChange: func,                 // (ev, selectedValue, reason, {value, fireValueChange, option}); replaces the
                                    // default store write and the validator for selections (not for typing)
    slotProps: shape({    // control/label/tooltip are used here, the rest is passed to <Autocomplete>
        control: object,
        label: object,
        tooltip: object,
        listbox: object,
    }),
};


export const AutoCompleteInput= memo( ({freeSolo=true, multiple, ...props}) => {
    const {viewProps, fireValueChange}=
        useFieldGroupConnector({...props, freeSolo, multiple,
            confirmValue: freeSolo ? confirmFreeSoloValue : confirmListedValue});
    return <AutoCompleteInputView {...{...viewProps, fireValueChange}}/>;
});


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
    ...omit(AutoCompleteInputView.propTypes, 'value', 'valid', 'message', 'fireValueChange'),
    tooltip: string,
};


// may not be needed
const confirmFreeSoloValue= (v) => v;

const confirmListedValue= (v,props) => {
    const {options=[], defaultValue} = props;
    const optionContain= Boolean(v && options.find((op) => (op?.value ?? op) === v));
    if (isEmpty(options) || optionContain) return v;
    return defaultValue ?? (options[0]?.value ?? options[0]);
};


/**
 * Coalesce rapid changes: returns value only after it has stayed unchanged for `wait` ms.
 * @param {*} value - the value to debounce
 * @param {number} wait - quiet period in ms
 * @returns {*} the latest value that has been stable for `wait` ms
 */
export function useDebounced(value, wait) {
    const [debounced, setDebounced]= useState(value);
    useEffect(() => {
        const id= setTimeout(() => setDebounced(value), wait);
        return () => clearTimeout(id);
    }, [value, wait]);
    return debounced;
}


/**
 * Resolve a list of options that the caller may produce either synchronously or asynchronously.
 * Options from a superseded request are dropped; side effects inside getOptions are not.
 * @param {function} getOptions - (value) => Array|Promise<Array>, called whenever value changes
 * @param {string} value - the current input value to look options up for
 * @returns {{options:Array, loading:boolean}}
 */
export function useAsyncOptions(getOptions, value) {
    const [result, setResult]= useState({options:[], loading:false});
    const getOptionsRef= useRef(getOptions);
    const latestRef= useRef(undefined);

    useEffect(() => { getOptionsRef.current= getOptions; }); // keep the callback fresh without re-querying

    useEffect(() => {
        const arrayOrPromise= getOptionsRef.current?.(value);
        latestRef.current= arrayOrPromise;
        if (!arrayOrPromise || isArray(arrayOrPromise)) {
            setResult({options: arrayOrPromise || [], loading:false});
            return;
        }
        setResult((r) => ({options:r.options, loading:true})); // keep showing the old list until the new one lands
        Promise.resolve(arrayOrPromise)
            .then((options) => {
                if (arrayOrPromise!==latestRef.current) return; // a newer request was made, this one is stale
                setResult({options: isArray(options) ? options : [], loading:false});
            })
            .catch((err) => {
                if (arrayOrPromise!==latestRef.current) return;
                logger.error(err);
                setResult({options:[], loading:false});
            });
    }, [value]);

    return result;
}


function Decorator({children, setOpen}) {
    return (
        <Stack onClick={() => setOpen(false)}>
            {children}
        </Stack>

    );
}
