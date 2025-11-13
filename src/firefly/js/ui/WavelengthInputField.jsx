import React from 'react';
import PropTypes from 'prop-types';
import {FormControl, FormLabel, Stack, Typography} from '@mui/joy';
import {isNaN, memoize} from 'lodash';
import toFloat from 'validator/es/lib/toFloat';
import {QuantityInputField} from 'firefly/ui/QuantityInputField';
import {useFieldGroupValue} from 'firefly/ui/SimpleComponent';
import {WAVELENGTH_UNITS} from 'firefly/visualize/VisUtil';

// following must be a key from the WAVELENGTH_UNITS object
export const BASE_UNIT = 'um';

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

export const convertWvlUnits = (valueStr, fromUnit, toUnit) => {
    const value = toFloat(valueStr+'');
    if (isNaN(value)) return valueStr;
    const newValue = convertWavelength(value, fromUnit, toUnit);

    // to avoid floating-point rounding errors
    let newValueStr = newValue.toFixed(getFracDigits(toUnit));
    // remove leading 0s (if any)
    newValueStr = parseFloat(newValueStr).toString();
    return newValueStr;
};

const formatWvl = (baseValue, outputUnit) => `${convertWvlUnits(baseValue, BASE_UNIT, outputUnit)} ${WAVELENGTH_UNITS[outputUnit].symbol}`;

const wvlQuantityProps = {
    quantityName: 'Wavelength',
    convertQuantityUnits: convertWvlUnits,
    formatQuantity: formatWvl,
    quantityBaseUnit: BASE_UNIT,
    quantityUnitOptions: [BASE_UNIT, 'nm', 'angstrom'].map((unitKey) => ({
        label: WAVELENGTH_UNITS[unitKey].symbol,
        value: unitKey
    })),
};

export const WavelengthInputField = (props) => {
    return (
        <QuantityInputField {...{
            min: MIN_WVL, max: MAX_WVL, ...wvlQuantityProps,
            ...props // at the end to allow consumer overriding the defaults
        }}/>
    );
};

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
