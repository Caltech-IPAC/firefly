/**
 * Generic Quantity Input Field component and helpers.
 *
 * "Quantity" refers to a physical quantity that consists of a scalar (numeric value) and a unit,
 * for e.g., length, angular size, etc.
 */

import React, {memo, useEffect} from 'react';
import PropTypes from 'prop-types';
import {Divider, Stack, FormHelperText, Typography} from '@mui/joy';
import { ListBoxInputFieldView } from 'firefly/ui/ListBoxInputField';
import { InputFieldView } from 'firefly/ui/InputFieldView';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import Validate from 'firefly/util/Validate';

/**
 * Generic units picker used by QuantityInputFieldView.
 *
 * If there is more than one unit option, it shows a dropdown ListBoxInputFieldView to select the unit otherwise
 * it just shows the single unit label.
 *
 * @param {object} props
 * @param {string} props.unit
 * @param {function} props.onChange
 * @param {Array<{label:string,value:string}>} props.unitOptions
 */
const QuantityUnitsPicker = ({ unit, onChange, unitOptions = [], ...props }) => (
    <Stack direction='row' alignItems='center'>
        <Divider orientation='vertical' />
        {unitOptions.length > 1
            ? <ListBoxInputFieldView
                value={unit}
                onChange={onChange}
                options={unitOptions}
                multiple={false}
                slotProps={{
                    input: {
                        variant: 'plain',
                        sx: {minHeight: 'unset'},
                    },
                }}
                {...props}/>
            : <Typography sx={{px: 1}} {...props}>{unitOptions[0]?.label || unit}</Typography>
        }
    </Stack>
);

/**
 * Generic component for a Quantity (= Numeric Value + Unit) Input.
 *
 * It's a controlled component in which state is sent through `{value, displayValue, unit, valid, message}` props and
 * is received back through `onChange()` prop.
 *
 * @param {object} props
 * @param {number} props.min
 * @param {number} props.max
 * @param {object} props.sx
 * @param {object} props.slotProps
 * @param {object} props.inputStyle
 * @param {string} props.orientation
 * @param {string} props.label
 * @param {boolean} props.showFeedback
 * @param {string} props.placeholder
 * @param {string} props.tooltip
 * @param {boolean} props.connectedMarker
 *
 * <hr>Stateful props:
 * @param {string} props.value quantity value in `quantityBaseUnit` (for internal storage)
 * @param {string} props.displayValue quantity value that is input by user (shown in the UI)
 * @param {string} props.unit quantity unit input by user (shown in the UI)
 * @param {boolean} props.valid whether `value` is valid (i.e. satisfies min/max bounds, nullAllowed, etc.)
 * @param {string} props.message message to display if invalid
 * @param {function(object, {value: string, displayValue: string, unit: string}): void} props.onChange lifts the updated
 * value, displayValue, unit to the parent that controls the state.
 *
 * <hr>Quantity-specific props (required):
 * @param {string} props.quantityName the name of the quantity (for display purposes)
 * @param {function} props.convertQuantityUnits converts numeric value between units. Signature: (valueStr, fromUnit, toUnit) => string
 * @param {function} props.formatQuantity formats the value (in quantityBaseUnit) to given unit. Signature: (valueInBaseUnit, outputUnit) => string
 * @param {string} props.quantityBaseUnit the canonical base unit key to use for `value` prop. Must be one of the `quantityUnitOptions` values.
 * @param {Array} props.quantityUnitOptions options array for the units picker (label and value pairs)
 */
export function QuantityInputFieldView({min, max, sx, slotProps, inputStyle = {},
                                           orientation = 'vertical', label, showFeedback = false, connectedMarker,
                                           placeholder, tooltip, onChange, valid, message,
                                           unit, value, displayValue, quantityName,
                                           convertQuantityUnits, formatQuantity, quantityBaseUnit, quantityUnitOptions}) {
    const handleValueChange = (ev) => {
        const newDisplayValue = ev?.target?.value?.trim();
        const newValue = convertQuantityUnits(newDisplayValue, unit, quantityBaseUnit);
        const quantityInfoUpdate = { value: newValue, displayValue: newDisplayValue, unit };
        onChange?.(ev, quantityInfoUpdate);  // pass changes from UI up to parent
    };

    const handleUnitChange = (ev, newUnit) => {
        if (unit === newUnit) return;
        let newValue = value;
        let newDisplayValue = displayValue;

        if (valid) {
            newDisplayValue = convertQuantityUnits(value, quantityBaseUnit, newUnit);
        } else {
            // maybe the displayValue (user-input number) is valid for new unit
            // so keep displayValue unchanged but update value (which will be checked for validity by parent component)
            newValue = convertQuantityUnits(displayValue, newUnit, quantityBaseUnit);
        }

        const quantityInfoUpdate = { value: newValue, displayValue: newDisplayValue, unit: newUnit };
        onChange?.(ev, quantityInfoUpdate); // pass changes from UI up to parent
    };

    const feedback = showFeedback && formatQuantity && min !== undefined && max !== undefined
        ? `Valid range: ${formatQuantity(min, unit)} - ${formatQuantity(max, unit)}`
        : '';

    return (
        <Stack sx={{width: 'min-content', ...sx}}>
            <InputFieldView
                valid={valid}
                message={message}
                onChange={handleValueChange}
                onBlur={handleValueChange}
                value={displayValue}  // displayValue is what appears in the UI
                inputStyle={inputStyle}
                placeholder={placeholder || `Enter ${quantityName}`}
                tooltip={tooltip || `Enter ${quantityName} within the valid range`}
                label={label}
                orientation={orientation}
                connectedMarker={connectedMarker}
                sx={{ '& .MuiInput-root': { paddingInlineEnd: 0 } }}
                { ...slotProps?.inputField }
                endDecorator={
                    <QuantityUnitsPicker unit={unit} onChange={handleUnitChange}
                                         unitOptions={quantityUnitOptions}
                                         tooltip={`Unit of the ${quantityName}`}
                                         {...slotProps?.units} />
                }
            />
            <FormHelperText {...{ ...slotProps?.feedback }}>
                {feedback}
            </FormHelperText>
        </Stack>
    );
}


QuantityUnitsPicker.propTypes = {
    unit: PropTypes.string,
    onChange: PropTypes.func,
    unitOptions: PropTypes.array,
};

QuantityInputFieldView.propTypes = {
    min: PropTypes.number,
    max: PropTypes.number,
    sx: PropTypes.object,
    slotProps: PropTypes.shape({
        inputField: PropTypes.object,
        units: PropTypes.object,
        feedback: PropTypes.object,
    }),
    inputStyle: PropTypes.object,
    orientation: PropTypes.string,
    label: PropTypes.string,
    showFeedback: PropTypes.bool,
    placeholder: PropTypes.string,
    tooltip: PropTypes.string,
    connectedMarker: PropTypes.bool,

    // stateful props
    onChange: PropTypes.func,
    valid: PropTypes.bool,
    message: PropTypes.string,
    unit: PropTypes.string,
    value: PropTypes.string,
    displayValue: PropTypes.string,

    // quantity-specific
    quantityName: PropTypes.string,
    convertQuantityUnits: PropTypes.func,
    formatQuantity: PropTypes.func,
    quantityBaseUnit: PropTypes.string,
    quantityUnitOptions: PropTypes.array,
};

const normalizeInitState = (initialState={}, quantityBaseUnit, convertQuantityUnits) => {
    const { value='', displayValue='', unit=quantityBaseUnit } = initialState;
    const norm = { value, displayValue, unit }; // both strings
    if (value && !displayValue) {
        norm.displayValue = convertQuantityUnits(value, quantityBaseUnit, unit);
    } else if (!value && displayValue) {
        norm.value = convertQuantityUnits(displayValue, unit, quantityBaseUnit);
    }
    return norm;
};

const quantityBoundsValidator = (min, max, nullAllowed, unit, quantityName, formatQuantity) => (valueStr) => {
    const makeMinMaxStr = (value) => formatQuantity(value, unit);
    return Validate.floatRange(min, max, null, quantityName, valueStr, nullAllowed, makeMinMaxStr);
};


/**Uncontrolled component for Quantity Input that wires QuantityInputFieldView's state with the FieldGroup store.**/
export const QuantityInputField = memo((props) => {
    const { quantityBaseUnit, convertQuantityUnits, quantityName, formatQuantity } = props;
    const normalizedInitialState = normalizeInitState(props?.initialState, quantityBaseUnit, convertQuantityUnits);
    const { unit: initUnit } = normalizedInitialState;
    const { viewProps, fireValueChange } = useFieldGroupConnector({
        ...props,
        initialState: normalizedInitialState,
    });

    useEffect(() => {
        if ((viewProps?.value || viewProps?.value===0) && viewProps?.unit && !viewProps?.displayValue) {
            // if value changed in the store (for e.g. by a setFieldValue()) and there's no displayValue, set it
            const newDisplayValue = convertQuantityUnits(viewProps.value, quantityBaseUnit, viewProps.unit);
            fireValueChange({displayValue: newDisplayValue});
        }
        }, [viewProps?.value, viewProps?.unit, viewProps?.displayValue,
            fireValueChange, convertQuantityUnits, quantityBaseUnit, initUnit]);

    const handleOnChange = (ev, quantityInfoUpdate) => {
        const { value, displayValue, unit=quantityBaseUnit, validator,
            nullAllowed=false, min, max } = { ...viewProps, ...quantityInfoUpdate };

        // Run custom validator first (on base-unit value). If valid, run bounds validator if min/max provided.
        let validationResult = validator?.(value) ?? { valid: true, message: '' };
        if (validationResult?.valid && (min!==undefined || max!==undefined)) {
            const boundsValidator = quantityBoundsValidator(min, max, nullAllowed, unit, quantityName, formatQuantity);
            validationResult = boundsValidator(value);
        }
        const { valid=true, message='' } = validationResult;

        // Set the UI updates and validation result in FieldGroup store
        fireValueChange({ value, displayValue, unit, valid, message });
    };

    return (
        <QuantityInputFieldView
            {...{
                ...viewProps, //stateful props from FieldGroup store along with pass-through visual props
                onChange: handleOnChange, //callback function that receives the state updates from UI (ev, quantityInfoUpdate)
            }}
        />
    );
});

QuantityInputField.propTypes = {
    ...QuantityInputFieldView.propTypes,
    // FieldGroup wiring
    fieldKey: PropTypes.string.isRequired,
    groupKey: PropTypes.string,
    // validation + bounds
    validator: PropTypes.func,
    min: PropTypes.number,
    max: PropTypes.number,
    nullAllowed: PropTypes.bool,
    // initial state
    initialState: PropTypes.shape({
        value: PropTypes.any,
        displayValue: PropTypes.string,
        unit: PropTypes.string,
    }),
};