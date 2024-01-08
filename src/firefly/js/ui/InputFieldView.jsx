import {FormControl, FormLabel, Input, Stack, Tooltip, Typography} from '@mui/joy';
import React from 'react';
import {pickBy} from 'lodash';
import {bool, string, number, object, func, oneOfType, shape, element} from 'prop-types';


export function InputFieldView(props) {
    const {visible,disabled, label,tooltip,value,inputRef, slotProps,
        valid,onChange, onBlur, onKeyPress, onKeyDown, onKeyUp, showWarning,
        message, type, placeholder, sx, startDecorator, endDecorator,
        readonly, required, orientation='vertical'}= props;
    if (!visible) return null;
    let {form='__ignore'}= props;
    // form to relate this input field to.
    // assign form to null or empty-string to use it within a form tag (similar to input tag without form attribute).
    // if form is not given, it will default to __ignore so that it does not interfere with embedded forms.
    form = form || undefined;

    const currValue= (type==='file') ? '' : (value??'');
    const showErrorTip= !valid && showWarning && message;

    const title= showErrorTip ?
        (
            <Stack direction='column'>
                <Typography level='body-md' color={'danger'}> {message} </Typography>
                <Typography level='body-md' color={'neutral'}> {tooltip} </Typography>
            </Stack>
        ) : <div style={{whiteSpace:'pre'}}>{tooltip}</div>;

    return (
        <Stack {...{className:'ff-Input InputFieldView', sx}}>
            <Tooltip {...{title, enterDelay:showErrorTip?700:undefined, ...slotProps?.tooltip}}>
                <FormControl {...{orientation, error:!valid, ...slotProps?.control}}>
                    {label && <FormLabel {...slotProps?.label}>{label}</FormLabel>}
                    <Input {...{
                        value: currValue,
                        disabled:readonly,
                        ref: inputRef,
                        startDecorator, endDecorator,
                        type,
                        form,
                        ...slotProps?.input,
                        placeholder,
                        required,
                        onChange:(ev) => onChange ? onChange(ev) : null,
                        onBlur: (ev) => onBlur?.(ev),
                        onKeyPress: (ev) => onKeyPress && onKeyPress(ev,currValue),
                        onKeyDown: (ev) => onKeyDown && onKeyDown(ev,currValue),
                        onKeyUp: (ev) => onKeyUp && onKeyUp(ev),
                    }}
                    />
                </FormControl>
            </Tooltip>
        </Stack>
    );
}

InputFieldView.propTypes= {
    valid   : bool,
    visible : bool,
    disabled : bool,
    message : string,
    tooltip : string,
    label : string,
    inline : bool,
    value   : oneOfType([string, number]).isRequired,
    onChange : func.isRequired,
    onBlur : func,
    onKeyPress : func,
    onKeyDown: func,
    onKeyUp: func,
    showWarning : bool,
    type: string,
    placeholder: string,
    form: string,
    readonly: bool,
    required: bool,
    startDecorator: element,
    endDecorator: element,
    sx: object,
    slotProps: shape({
        input: object,
        control: object,
        label: object,
        tooltip: object
    })
};

InputFieldView.defaultProps= {
    showWarning : true,
    valid : true,
    visible : true,
    message: '',
    type: 'text',
    readonly: false,
    required: false
};

export const propTypes = InputFieldView.propTypes;
