import React, {memo} from 'react';
import PropTypes, {object} from 'prop-types';
import {FormControl, FormLabel, Switch, Tooltip} from '@mui/joy';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {toBoolean} from 'firefly/util/WebUtil';

export function SwitchInputFieldView({label, tooltip, orientation='horizontal', startDecorator, endDecorator,
                                         size, value, onChange, slotProps}) {
    return (
        <Tooltip title={tooltip} {...slotProps?.tooltip}>
            <FormControl orientation={orientation} {...slotProps?.control}
                         sx={{'.MuiSwitch-root': orientation==='horizontal' ? {margin: 0} : {alignSelf: 'unset'},
                             ...slotProps?.control?.sx}}>
                {label && <FormLabel {...slotProps?.label}>{label}</FormLabel>}
                <Switch {...{checked: toBoolean(value), onChange, startDecorator, endDecorator, size, ...slotProps?.input}}/>
            </FormControl>
        </Tooltip>
    );
}


export const SwitchInputField = memo((props) => {
    const {viewProps, fireValueChange} = useFieldGroupConnector(props);
    const newProps= {
        ...viewProps,
        value: Boolean(viewProps.value),
        onChange: (ev) => handleOnChange(ev, viewProps, fireValueChange)
    };
    return (<SwitchInputFieldView {...newProps} />);
});


function handleOnChange(ev, viewProps, fireValueChange) {
    const {validator, onChange} =  viewProps;

    const value = ev.target.checked;
    const {valid, message} = validator?.(value) || {valid: true};

    fireValueChange({ value, message, valid });
    onChange?.(value);
}


SwitchInputFieldView.propTypes = {
    label: PropTypes.string,
    tooltip: PropTypes.string,
    orientation: PropTypes.oneOf(['vertical', 'horizontal']),
    startDecorator: PropTypes.node,
    endDecorator: PropTypes.node,
    size: PropTypes.string,
    value: PropTypes.bool,
    onChange: PropTypes.func,
    slotProps: PropTypes.shape({
        control: object,
        input: object,
        label: object,
        tooltip: object
    })
};

SwitchInputField.propTypes = {
    fieldKey: PropTypes.string.isRequired,
    groupKey: PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.bool
    }),
    ...SwitchInputFieldView.propTypes
};