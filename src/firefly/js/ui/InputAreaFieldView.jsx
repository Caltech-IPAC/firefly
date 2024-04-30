import React, {forwardRef, useContext} from 'react';
import PropTypes, {bool, object, shape} from 'prop-types';
import {ConnectionCtx} from './ConnectionCtx.js';
import Textarea from '@mui/joy/Textarea';
import {FormControl, FormLabel, Stack, Tooltip} from '@mui/joy';
import {inputFieldTooltipProps, inputFieldValue} from 'firefly/ui/InputFieldView';


export const InputAreaFieldView= forwardRef( ({ visible=true,label,tooltip,value,
                                                  button, valid=true,onChange, onBlur,
                                                  showWarning=true, connectedMarker=false,
                                                 placeholderHighlight,
                                                  message='', type, placeholder, sx, slotProps,
                                                  orientation='vertical', minRows=2, maxRows}, ref ) => {
    const connectContext= useContext(ConnectionCtx);
    if (!visible) return null;

    const tooltipProps = inputFieldTooltipProps({valid, message, showWarning, tooltip});
    const connectedStyle= connectedMarker||connectContext.controlConnected ? {bgcolor:'yellow'} : {};

    return (
        <Stack {...{className:'ff-Input InputAreaFieldView', direction: 'row', spacing: 1, sx, ref}}>
            <Tooltip {...{...tooltipProps, ...slotProps?.tooltip}}>
                <FormControl {...{orientation, error:!valid, sx: {width: 1}, ...slotProps?.control}}>
                    {label && <FormLabel {...slotProps?.label}>{label}</FormLabel>}
                    <Textarea placeholder={placeholder}
                              minRows={minRows}
                              maxRows={maxRows}
                              slotProps={{
                                  textarea: {
                                      spellCheck: false,
                                      value: inputFieldValue(type, value),
                                      onChange: (ev) => onChange?.(ev),
                                      onBlur: (ev) => onBlur?.(ev),
                                      sx: {
                                          '--Textarea-placeholderColor': placeholderHighlight ?
                                              'var(--joy-palette-warning-plainColor)' : 'inherit'
                                      },
                                      ...slotProps?.textArea
                                  }
                              }}
                              sx={connectedStyle}
                              {...slotProps?.input}
                    />
                </FormControl>
            </Tooltip>
            {Boolean(button) && button}
        </Stack>
    );
});

InputAreaFieldView.propTypes= {
    valid   : PropTypes.bool,
    visible : PropTypes.bool,
    message : PropTypes.string,
    tooltip : PropTypes.string,
    label : PropTypes.string,
    sx: PropTypes.object,
    value   : PropTypes.string.isRequired,
    onChange : PropTypes.func.isRequired,
    onBlur : PropTypes.func,
    showWarning : PropTypes.bool,
    minRows: PropTypes.number,
    maxRows: PropTypes.number,
    placeholder: PropTypes.string,
    connectedMarker: bool,
    placeholderHighlight: bool,
    orientation: PropTypes.string,
    slotProps: shape({
        control: object,
        input: object,
        label: object,
        textArea: object,
        tooltip: object
    })
};


export const propTypes = InputAreaFieldView.propTypes;
