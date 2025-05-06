import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {Divider, FormControl, FormHelperText, FormLabel, Stack, Typography} from '@mui/joy';
import {isNaN, memoize} from 'lodash';
import {ListBoxInputFieldView} from 'firefly/ui/ListBoxInputField';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {InputFieldView} from 'firefly/ui/InputFieldView';
import Validate from 'firefly/util/Validate';
import {useFieldGroupValue} from 'firefly/ui/SimpleComponent';
import {WAVELENGTH_UNITS} from 'firefly/visualize/VisUtil';

// following strings are the keys in WAVELENGTH_UNITS object
export const BASE_UNIT = 'um';
const WVL_UNITS_OPTIONS = [BASE_UNIT, 'nm', 'angstrom']; // options that appear in the units dropdown

// following are in BASE_UNIT (microns). These bounds work well for most of the EM spectrum we will ever deal with: X-rays to Microwaves
const MIN_WVL = 1e-5; // = 0.1 Å (gamma rays < 0.1 Å)
const MAX_WVL = 1e5; // = 0.1 m (radio waves > 0.1 m)

const convertWavelength = (value, fromUnit, toUnit) => {
    const fromUnitInMeters = WAVELENGTH_UNITS[fromUnit].m;
    const toUnitInMeters = WAVELENGTH_UNITS[toUnit].m;
    return value * fromUnitInMeters / toUnitInMeters;
};

// get the number of digits after decimal point that we will ever need to represent the lowest wavelength (MIN_WVL)
const getFracDigits = memoize((unit) =>
    Math.log10(convertWavelength(MIN_WVL, BASE_UNIT, unit)) * -1);

export const convertWavelengthStr = (valueStr, fromUnit, toUnit) => {
    const value = parseFloat(valueStr);
    if (isNaN(value)) return valueStr;
    const newValue = convertWavelength(value, fromUnit, toUnit);

    // to avoid floating-point rounding errors
    let newValueStr = newValue.toFixed(getFracDigits(toUnit));
    // remove leading 0s (if any)
    newValueStr = parseFloat(newValueStr).toString();
    return newValueStr;
};

const wvlWithUnitsStr = (value, unit) => `${convertWavelengthStr(value, BASE_UNIT, unit)} ${WAVELENGTH_UNITS[unit].symbol}`;


export const WavelengthUnitsView = ({ unit, onChange, ...props }) => (
    <Stack direction='row' alignItems='center'>
        <Divider orientation='vertical' />
        <ListBoxInputFieldView
            value={unit}
            onChange={onChange}
            options={WVL_UNITS_OPTIONS.map((unitKey) => ({
                label: WAVELENGTH_UNITS[unitKey].symbol,
                value: unitKey
            }))}
            tooltip='Unit of the wavelength'
            multiple={false}
            slotProps={{
                input: {
                    variant: 'plain',
                    sx: { minHeight: 'unset' },
                },
            }}
            {...props}
        />
    </Stack>
);

/**Controlled component for Wavelength Input.
 *
 * The state is controlled by `{value, displayValue, unit, valid, message}` props and the effect of state change is
 * controlled by `onChange`.
 *
 * @param props
 * @param props.value {string} wavelength value in BASE_UNIT
 * @param props.displayValue {string} wavelength value that is input by user
 * @param props.unit {string} wavelength unit input by user
 * @param props.valid {boolean} whether `value` is valid (i.e. satisfies min/max bounds, nullAllowed, etc.)
 * @param props.message {string} message to display if invalid
 * @param props.onChange {function(object, {value: string, displayValue: string, unit: string}): void} lifts the updated
 * value, displayValue, unit to the parent that controls the state.
 */
function WavelengthInputFieldView({min=MIN_WVL, max=MAX_WVL, sx, slotProps, inputStyle={},
                                      orientation='vertical', label, showFeedback=false,
                                      placeholder='Enter wavelength', tooltip='Enter wavelength within the valid range',
                                      onChange, valid, message, unit, value, displayValue}) {

    const handleWvlChange = (ev) => {
        const newDisplayValue = ev?.target?.value?.trim();
        const newValue = convertWavelengthStr(newDisplayValue, unit, BASE_UNIT);
        const wvlInfoUpdate = {
            value: newValue,
            displayValue: newDisplayValue,
            unit,
        };
        onChange?.(ev, wvlInfoUpdate); // lift the state
    };

    const handleUnitChange = (ev, newUnit) => {
        if (unit === newUnit) return;
        let newValue = value;
        let newDisplayValue = displayValue;

        if (valid) {
            newDisplayValue = convertWavelengthStr(value, BASE_UNIT, newUnit);
        } else {
            // maybe the displayValue (user-input number) is valid for new unit
            // so keep displayValue unchanged but update value (which will be checked for validity by parent component)
            newValue = convertWavelengthStr(displayValue, newUnit, BASE_UNIT);
        }

        const wvlInfoUpdate = {
            value: newValue,
            displayValue: newDisplayValue,
            unit: newUnit,
        };
        onChange?.(ev, wvlInfoUpdate);
    };

    const feedback = showFeedback ? `Valid range: ${wvlWithUnitsStr(min, unit)} - ${wvlWithUnitsStr(max, unit)}` : '';

    return (
        <Stack sx={sx}>
            <InputFieldView
                valid={valid}
                message={message}
                onChange={handleWvlChange}
                onBlur={handleWvlChange}
                value={displayValue}
                inputStyle={inputStyle}
                placeholder={placeholder}
                tooltip={tooltip}
                label={label}
                orientation={orientation}
                sx={{ '& .MuiInput-root': { paddingInlineEnd: 0 } }}
                endDecorator={<WavelengthUnitsView unit={unit} onChange={handleUnitChange} {...slotProps?.units}/>}
            />
            <FormHelperText {...{...slotProps?.feedback}}>
                {feedback}
            </FormHelperText>
        </Stack>
    );
}

const isValidWavelength = (value, unit, nullAllowed, min, max, description) => {
    const makeMinMaxStr = (value) => wvlWithUnitsStr(value, unit);
    return Validate.floatRange(min, max, null, description, value, nullAllowed, makeMinMaxStr);
};

const handleOnChange = (ev, wvlInfoUpdate, viewProps, fireValueChange) => {
    const { value, displayValue, unit=BASE_UNIT, validator, nullAllowed=false,
        min=MIN_WVL, max=MAX_WVL, description='Wavelength' } = { ...viewProps, ...wvlInfoUpdate };

    // run custom validator (`validator()`) before the default validator (`isValidWavelength()`) that checks wavelength bounds
    let validationResult = validator?.(value) ?? {valid: true, message: ''};
    if (validationResult?.valid) validationResult = isValidWavelength(value, unit, nullAllowed, min, max, description);
    const { valid, message } = validationResult;

    fireValueChange({ value, displayValue, unit, valid, message });
};

const updateInitState = ({value='', displayValue='', unit=BASE_UNIT}) => {
    const initialState = {value, displayValue, unit};
    if (value && !displayValue) initialState.displayValue = convertWavelengthStr(value, BASE_UNIT, unit);
    else if (!value && displayValue) initialState.value = convertWavelengthStr(displayValue, unit, BASE_UNIT);
    return initialState;
};


// TODO for future: Make a HOC called QuantityInputField for
//  - abstraction of the redundant code/logic in WavelengthInputField(View) and SizeInputField(View)
//  - cleanup state logic of SizeInputField (it's unnecessarily convoluted)

/**Uncontrolled component for Wavelength Input that manages the state with respect to the FieldGroup.
 */
export const WavelengthInputField = memo((props) => {
    const { viewProps, fireValueChange } = useFieldGroupConnector({
        ...props,
        initialState: updateInitState(props?.initialState),
    });

    const newProps = {
        ...viewProps,
        onChange: (ev, wvlInfoUpdate) => handleOnChange(ev, wvlInfoUpdate, viewProps, fireValueChange)
    };
    return (<WavelengthInputFieldView {...newProps}/>);
});

const commonPropTypes = {
    min: PropTypes.number,
    max: PropTypes.number,
    nullAllowed: PropTypes.bool,
    showFeedback: PropTypes.bool,
    label: PropTypes.string,
    tooltip: PropTypes.string,
    placeholder: PropTypes.string,
    orientation: PropTypes.string,
    inputStyle: PropTypes.object,
    sx: PropTypes.object,
    slotProps: PropTypes.shape({
        units: PropTypes.object,
        feedback: PropTypes.object,
    }),
};

WavelengthInputFieldView.propTypes = {
    ...commonPropTypes,
    value: PropTypes.string, // used by field group state, always in BASE_UNIT
    displayValue: PropTypes.string, // what's displayed in the input field UI
    unit: PropTypes.string,
    valid: PropTypes.bool,
    message: PropTypes.string,
    onChange: PropTypes.func,
};

WavelengthInputField.propTypes = {
    ...commonPropTypes,
    fieldKey: PropTypes.string,
    groupKey: PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.any,
        displayValue: PropTypes.string,
        unit: PropTypes.string,
    }),
    validator: PropTypes.func,
    description: PropTypes.string,
};

const MIN_MAX_WVL_RANGE_ERR = 'Max wavelength must not be smaller than Min wavelength';

export function WavelengthRangeInput({minFieldKey, maxFieldKey, label, slotProps, ...props}) {
    const [getMinVal] = useFieldGroupValue(minFieldKey);
    const [getMaxVal] = useFieldGroupValue(maxFieldKey);

    const minMaxWvlRangeValidator = (currentValueStr, pairedValueStr, isMaxField) => {
        const [currentValue, pairedValue] = [currentValueStr, pairedValueStr].map((str) => Number.parseFloat(str));
        if (!isNaN(currentValue) && !isNaN(pairedValue)) {
            return ((isMaxField && currentValue < pairedValue) // maxValue < minValue (validation happened on the Max Wvl field)
                || (!isMaxField && currentValue > pairedValue)) // minValue > maxValue (validation happened on the Min Wvl field)
                ? {valid: false, message: MIN_MAX_WVL_RANGE_ERR}
                : {valid: true, message: ''};
        }
    };

    return (
        <FormControl {...props}>
            {label && <FormLabel>{label}</FormLabel>}
            <Stack direction='row' spacing={1} alignItems='center'
                   sx={{ '& .MuiInput-root': { 'width': '14rem' } }}
                   {...slotProps?.root}>
                <WavelengthInputField fieldKey={minFieldKey}
                                      placeholder='Min wavelength'
                                      description='Min wavelength'
                                      validator={(value)=>minMaxWvlRangeValidator(value, getMaxVal(), false)}
                                      {...slotProps?.wvlMin} />
                <Typography level='body-md'>to</Typography>
                <WavelengthInputField fieldKey={maxFieldKey}
                                      placeholder='Max wavelength'
                                      description='Max wavelength'
                                      validator={(value)=>minMaxWvlRangeValidator(value, getMinVal(), true)}
                                      {...slotProps?.wvlMax} />
            </Stack>
        </FormControl>
    );
}

WavelengthRangeInput.propTypes = {
    label: PropTypes.node,
    minFieldKey: PropTypes.string.isRequired,
    maxFieldKey: PropTypes.string.isRequired,
    slotProps: PropTypes.shape({
        root: PropTypes.object,
        wvlMin: PropTypes.shape({...WavelengthInputField.propTypes}),
        wvlMax: PropTypes.shape({...WavelengthInputField.propTypes})
    })
};
