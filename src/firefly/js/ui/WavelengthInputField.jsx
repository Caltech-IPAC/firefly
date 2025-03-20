import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {Divider, FormHelperText, Stack} from '@mui/joy';
import {ListBoxInputFieldView} from 'firefly/ui/ListBoxInputField';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {InputFieldView} from 'firefly/ui/InputFieldView';
import Validate from 'firefly/util/Validate';
import {memoize} from 'lodash';

export const BASE_UNIT = 'um';
const WAVELENGTH_UNITS = {
    // key: { m: meters equivalent, label: full string that appears in dropdown, symbol: shorter string that appears elsewhere }
    m: { m: 1, label: 'meters', symbol: 'm' },
    [BASE_UNIT]: { m: 1e-6, label: 'microns', symbol: 'µm' },
    nm: { m: 1e-9, label: 'nanometers', symbol: 'nm' },
    angstrom: { m: 1e-10, label: 'angstroms', symbol: 'Å' },
};
const WVL_UNITS_OPTIONS = [BASE_UNIT, 'nm', 'angstrom']; // options that appear in the units dropdown

// Following are in BASE_UNIT (microns). These bounds work well for most of the EM spectrum we will ever deal with: X-rays to Microwaves
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

const isValidWavelength = (valueInMicrons, unit, nullAllowed, minInMicrons, maxInMicrons) => {
    const makeMinMaxStr = (value) => wvlWithUnitsStr(value, unit);
    return Validate.floatRange(minInMicrons, maxInMicrons, null, 'Wavelength', valueInMicrons, nullAllowed, makeMinMaxStr);
};


export const WavelengthUnitsView = ({ unit, onChange, ...props }) => (
    <Stack direction='row' alignItems='center'>
        <Divider orientation='vertical' />
        <ListBoxInputFieldView
            value={unit}
            onChange={onChange}
            options={WVL_UNITS_OPTIONS.map((unitKey) => ({
                label: WAVELENGTH_UNITS[unitKey].label,
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


// TODO for future: Make a HOC called QuantityInputField for abstraction of the redundant code in this and SizeInputField
function WavelengthInputFieldView({min=MIN_WVL, max=MAX_WVL, sx, slotProps, inputStyle={},
                                      orientation='vertical', label='Wavelength: ', showFeedback=false,
                                      onChange, valid, message, unit=BASE_UNIT, value, displayValue}) {

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
                placeholder='Enter wavelength'
                tooltip='Enter wavelength within the valid range'
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
};


const handleOnChange = (ev, wvlInfoUpdate, viewProps, fireValueChange) => {
    const { value, displayValue, unit=BASE_UNIT, nullAllowed=false,
        min=MIN_WVL, max=MAX_WVL } = { ...viewProps, ...wvlInfoUpdate }; //even validator can come from viewProps

    const { valid, message } = isValidWavelength(value, unit, nullAllowed, min, max);
    fireValueChange({ value, displayValue, unit, valid, message });
};


export const WavelengthInputField = memo((props) => {
    const { viewProps, fireValueChange } = useFieldGroupConnector(props);
    return (
        <WavelengthInputFieldView
            {...viewProps}
            onChange={(ev, wvlInfoUpdate) => handleOnChange(ev, wvlInfoUpdate, viewProps, fireValueChange)}
        />
    );
});

const commonPropTypes = {
    min: PropTypes.number,
    max: PropTypes.number,
    nullAllowed: PropTypes.bool,
    showFeedback: PropTypes.bool,
    label: PropTypes.string,
    tooltip: PropTypes.string,
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
    fieldKey: PropTypes.string.isRequired,
    groupKey: PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.any,
        displayValue: PropTypes.string,
        unit: PropTypes.string,
    }),
};