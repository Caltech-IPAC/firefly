import {
    Box,
    Button, FormControl, FormLabel, IconButton, Radio, RadioGroup, Sheet, Stack, ToggleButtonGroup, Tooltip
} from '@mui/joy';
import React from 'react';
import {array, string, func, bool, object, oneOf, shape, oneOfType, element, arrayOf, any} from 'prop-types';


function makeRadioGroup(options,orientation='horizontal',radioValue,onChange,radioTooltip,slotProps={}) {

    return (
        <RadioGroup {...{className:'ff-RadioGroup-container', orientation, ...slotProps.group}}>
            {
                options.map( ({label,value, disabled=false,tooltip }) => {
                    const radio= (<Radio
                        {...{ className:tooltip?undefined:'ff-RadioGroup-item', key:value, size:'sm', checked:value===radioValue, onChange, value, label, disabled, ...slotProps.radio }} />);
                    if (tooltip||radioTooltip) {
                        return (
                            <Tooltip {...{className:'ff-RadioGroup-item', key:value, title:tooltip || radioTooltip, placement:tooltip?'top':undefined}}>
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



function createButtons(options, slotProps={}) {
    return options.map(({value, disabled, tooltip, icon, label, startDecorator, endDecorator}, idx) => {
        let b;
        if (icon) {
            b = (<IconButton {...{key: idx + '', value, disabled: disabled || false, ...slotProps.icon}}>
                {icon}
            </IconButton>);
        } else {
            b = (<Button {...{key: idx + '', value, disabled: disabled || false,
                startDecorator, endDecorator,
                sx: {'--Button-minHeight' : 25}, ...slotProps.button}} >
                {label}
            </Button>);
        }
        return tooltip ? <Tooltip key={idx+''} title={tooltip}>{b}</Tooltip> : b;
    });
}

function fireOnChange(groupValue,newValue,onChange) {
    if (newValue && newValue!==groupValue) onChange({target: {value: newValue}});
}

function makeButtonGroup(options,groupValue,onChange,slotProps={}) {
    return (
        <ToggleButtonGroup {...{value: groupValue,
            sx: {maxHeight: 25},
            ...slotProps.buttonGroup,
            onChange: (ev, newValue) => fireOnChange(groupValue,newValue,onChange) }}>
            { createButtons(options,slotProps) }
        </ToggleButtonGroup>
    );
}

export function RadioGroupInputFieldView({options,orientation='vertical',value,
                                             onChange,label,tooltip,
                                             buttonGroup= false,
                                             sx, slotProps={}}) {

    return (
        <Box className='ff-Input' sx={sx}>
                <FormControl orientation={orientation} style={{whiteSpace:'nowrap'}}>
                    {label && <FormLabel {...{...slotProps.label}}>{label}</FormLabel>}
                    <Stack direction='row'>
                        {buttonGroup ?
                            makeButtonGroup(options,value,onChange,slotProps) :
                            makeRadioGroup(options,orientation,value,onChange,tooltip,slotProps)}
                    </Stack>
                </FormControl>
        </Box>
    );
}

RadioGroupInputFieldView.propTypes= {
    options: arrayOf(shape( { value: string, label: any, tooltip: string} )).isRequired,
    value: string.isRequired,
    orientation: oneOf(['vertical', 'horizontal']),
    onChange: func,
    label : string,
    tooltip : oneOfType([string,element]),
    inline : bool,
    sx: object,
    buttonGroup : bool,
    slotProps: shape({
        group: object,
        radio: object,
        button: object,
        icon : object,
        buttonGroup: object,
        tooltip: object
    })
};