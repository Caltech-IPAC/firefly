import {
    Button, FormControl, FormLabel, IconButton, Radio, RadioGroup, Sheet, Stack, ToggleButtonGroup, Tooltip
} from '@mui/joy';
import React from 'react';
import {array, string, func, bool, object, oneOf, shape, oneOfType, element} from 'prop-types';


function makeRadioGroup(options,orientation='horizontal',radioValue,onChange,radioTooltip,joyProps={}) {

    return (
        <RadioGroup {...{orientation, ...joyProps.JoyRadioGroup}}>
            {
                options.map( ({label,value, disabled=false,tooltip }) => {
                    const radio= (<Radio
                        {...{ key:value, checked:value===radioValue, onChange, value, label, disabled, ...joyProps.JoyRadio }} />);
                    if (tooltip) {
                        return (
                            <Tooltip {...{key:value, title:tooltip ?? radioTooltip, placement:'top'}}>
                                {radio}
                            </Tooltip>
                        );
                    }
                    else {
                        return radio;
                    }
                } )
            }
        </RadioGroup>
    );
}



function createButtons(options, joyProps={}) {
    return options.map(({value, disabled, tooltip, icon, label, startDecorator, endDecorator}, idx) => {
        let b;
        if (icon) {
            b = (<IconButton {...{key: idx + '', value, disabled: disabled || false, ...joyProps.JoyIconButton}}>
                {icon}
            </IconButton>);
        } else {
            b = (<Button {...{key: idx + '', value, disabled: disabled || false,
                startDecorator, endDecorator,
                sx: {'--Button-minHeight' : 25}, ...joyProps.JoyButton}} >
                {label}
            </Button>);
        }
        return tooltip ? <Tooltip key={idx+''} title={tooltip}>{b}</Tooltip> : b;
    });
}

function fireOnChange(groupValue,newValue,onChange) {
    if (newValue && newValue!==groupValue) onChange({target: {value: newValue}});
}

function makeButtonGroup(options,groupValue,onChange,joyProps={}) {
    return (
        <ToggleButtonGroup {...{sz:'sm', value: groupValue,
            sx: {maxHeight: 25},
            ...joyProps.JoyToggleButtonGroup,
            onChange: (ev, newValue) => fireOnChange(groupValue,newValue,onChange) }}>
            { createButtons(options,joyProps) }
        </ToggleButtonGroup>
    );
}

export function RadioGroupInputFieldView({options,orientation='vertical',value,
                                             onChange,label,tooltip,
                                             buttonGroup= false,
                                             sx, joyProps={}}) {

    return (
        <Sheet sx={sx}>
            <Tooltip {...{key:value, title:tooltip, ...joyProps.JoyTooltip}}>
                <FormControl orientation={orientation} style={{whiteSpace:'nowrap'}}>
                    {label && <FormLabel {...{...joyProps.JoyFormLabel}}>{label}</FormLabel>}
                    <Stack direction='row'>
                        {buttonGroup ?
                            makeButtonGroup(options,value,onChange,joyProps) :
                            makeRadioGroup(options,orientation,value,onChange,tooltip,joyProps)}
                    </Stack>
                </FormControl>
            </Tooltip>
        </Sheet>
    );
}

const joyPropsShape= shape({ sx: object, style: object});

RadioGroupInputFieldView.propTypes= {
    options: array.isRequired,
    value: string.isRequired,
    orientation: oneOf(['vertical', 'horizontal']),
    onChange: func,
    label : string,
    tooltip : oneOfType([string,element]),
    inline : bool,
    sx: object,
    buttonGroup : bool,
    joyProps : shape({
        JoyRadioGroup: joyPropsShape,
        JoyRadio: joyPropsShape,
        JoyButton: joyPropsShape,
        JoyIconButton : joyPropsShape,
        JoyToggleButtonGroup: joyPropsShape,
        JoyTooltip: joyPropsShape
    })
};