import React, {memo} from 'react';
import {object, shape, array, node, string, func, bool} from 'prop-types';
import {Tooltip, FormControl, FormLabel, Stack, ListItem, List, ListItemButton} from '@mui/joy';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {omit} from 'lodash';

/*
 This should have been named ListBox and ListBoxField, but it is already taken by ListBoxInputField(bad name).
 */
export function OptionList({value, options, decorator, orientation, label, title, onValueChange, slotProps, ...props}) {

    const listItems = toListItems({value, options, decorator, onValueChange, slotProps});

    return (
        <Stack className='ff-Input OptionList' {...slotProps?.root}>
            <FormControl {...{orientation, ...slotProps?.control}}>
                {label && <FormLabel {...slotProps?.label}>{label}</FormLabel>}
                <Tooltip {...{title, placement:'top', ...slotProps?.tooltip}}>
                    <List {...props}>
                        {listItems}
                    </List>
                </Tooltip>
            </FormControl>
        </Stack>
    );
}

OptionList.propTypes= {
    options : array,
    value:  string,
    decorator: func,
    orientation: string,
    label: node,
    title: node,
    onValueChange: func,
    slotProps: shape({
        root: object,
        control: object,
        label: object,
        tooltip: object,
        items: object,
    }),
};


export const OptionListField= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector({...props, confirmValue:checkForUndefined});
    const newProps= {
        ...omit(props, Object.keys(fieldPropTypes)),
        value: viewProps.value,
        onValueChange: (v) => handleOnChange(v, viewProps, fireValueChange)
    };
    return (<OptionList {...newProps} />);
});

const fieldPropTypes = {
    fieldKey : string,
    groupKey : string,
    forceReinit:  bool,
    initialState: shape({
        value: string,
    }),
};
OptionListField.propTypes= {
    ...fieldPropTypes,
    ...OptionList.propTypes
};


function toListItems({value, options, decorator, onValueChange, slotProps}) {

    decorator ??= (o) => o.label ?? o.value;

    return options?.map((o,idx) => {
        return (
            <ListItem key={idx} {...slotProps?.items}>
                <ListItemButton selected={o.value === value} onClick={() => onValueChange?.(o.value)}>
                    {decorator(o)}
                </ListItemButton>
            </ListItem>
        );
    });
}

function handleOnChange(value, viewProps, fireValueChange) {
    const {validator, options, onValueChange} =  viewProps;

    const selOpt = options.find((o) => o.value === value);
    if (!selOpt) {
        fireValueChange({ value, valid:'false', message: 'The value selected is not among the available options.'});
    } else {
        const {valid,message} = validator?.(value) || {valid:'true'};
        fireValueChange({ value, message, valid });
    }
    onValueChange?.(value);
}

function checkForUndefined(v,props) {
    return v ?? props.options?.[0]?.value;
}



