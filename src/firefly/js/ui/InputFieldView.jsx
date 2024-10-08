import {FormControl, FormLabel, Input, Stack, Tooltip, Typography} from '@mui/joy';
import React from 'react';
import {bool, string, number, object, func, oneOfType, shape, element} from 'prop-types';


export function inputFieldTooltipProps({valid, message, showWarning=true, tooltip}) {
    const showErrorTip= !valid && showWarning && message;
    const title= showErrorTip ?
        (
            <Stack direction='column'>
                <Typography level='body-md' color={'danger'}> {message} </Typography>
                <Typography level='body-md' color={'neutral'} whiteSpace='pre'> {tooltip} </Typography>
            </Stack>
        ) : tooltip && <Typography level='body-md' whiteSpace='pre'>{tooltip}</Typography>;
    const enterDelay = showErrorTip ? 700 : undefined;
    return {title, enterDelay};
}

export const inputFieldValue = (type, value) => (type==='file') ? '' : (value??'');



export function InputFieldView(props) {
    const {visible=true,label,tooltip,value,inputRef, slotProps,
        valid=true,onChange, onBlur, onKeyPress, onKeyDown, onKeyUp, showWarning=true,
        message='', type='text', placeholder, sx, startDecorator, endDecorator,
        readonly=false, required=false, orientation='vertical'}= props;
    if (!visible) return null;
    let {form='__ignore'}= props;
    // form to relate this input field to.
    // assign form to null or empty-string to use it within a form tag (similar to input tag without form attribute).
    // if form is not given, it will default to __ignore so that it does not interfere with embedded forms.
    form = form || undefined;
    const showErrorTip= !valid && showWarning && message;
    const currValue = inputFieldValue(type, value);
    const tooltipProps = inputFieldTooltipProps({valid, message, showWarning, tooltip});

    return (
        <Stack {...{className:'ff-Input InputFieldView', sx}}>
            <Tooltip {...{...tooltipProps, placement:showErrorTip? 'right':undefined , ...slotProps?.tooltip}}>
                <FormControl {...{orientation, required, error:!valid, ...slotProps?.control}}>
                    {label && <FormLabel {...slotProps?.label}>{label}</FormLabel>}
                    <Input {...{
                        value: currValue??'',
                        disabled:readonly,
                        ref: inputRef,
                        startDecorator, endDecorator,
                        type,
                        form,
                        ...slotProps?.input,
                        placeholder,
                        title:'',       // explicitly remove browser's tooltip.  Required fields was generating default title.
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
    tooltip : oneOfType([string, bool]),
    label : string,
    orientation: string,
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

export const propTypes = InputFieldView.propTypes;
