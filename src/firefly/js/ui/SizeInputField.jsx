/**
 * NOTE: "Size" in this component refers to the **Angular Size** (for measuring cone search radius, rotation angle, etc.).
 * It's not the linear size or a measure of (wave)length.
 */

import React, {useContext} from 'react';
import PropTypes, {bool, object} from 'prop-types';
import {memoize} from 'lodash';
import toFloat from 'validator/es/lib/toFloat';
import {convertAngle} from '../visualize/VisUtil.js';
import {ConnectionCtx} from './ConnectionCtx.js';
import {toMaxFixed} from '../util/MathUtil.js';
import {QuantityInputField} from 'firefly/ui/QuantityInputField';


const MIN_ARCSEC = 0.001; // lowest size (in arcsec) we will ever represent
const BASE_UNIT = 'deg';

// Get the number of digits after decimal point needed to represent lowest arcsec value (`MIN_ARCSEC`) in the given unit.
// Math.ceil(-log10(value)) gives digits needed to reach the first significant digit;
// +3 retains 3 more digits beyond that (making 4 significant digits total).
const getFracDigits = memoize((unit) =>
    Math.ceil(Math.log10(convertAngle('arcsec', unit, MIN_ARCSEC, false)) * -1) + 3);

export const ANGULAR_SIZE_UNITS = {
    [BASE_UNIT]: { name: 'degrees', symbol: '°' },
    'arcmin': { name: 'arcminutes', symbol: '\'' },
    'arcsec': { name: 'arcseconds', symbol: '"' },
};


/** Converter for handling all size conversions between units.
 *
 * @param {string} valueStr size value in the given fromUnit units
 * @param {string} fromUnit unit of the given valueStr
 * @param {string} toUnit unit to convert the valueStr into
 * @returns {*|string}
 */
const convertSizeUnits = (valueStr, fromUnit=BASE_UNIT, toUnit=BASE_UNIT) => {
    if (valueStr===undefined || valueStr===null || valueStr==='') return '';
    const value = toFloat(valueStr+'');
    if (isNaN(value)) return valueStr; // preserve invalid input to let input validation will handle it
    const newVal = convertAngle(fromUnit, toUnit, value.toString(), false);
    return toMaxFixed(newVal, getFracDigits(toUnit)).toString();
};

const formatSize = (valueInDeg, outputUnit) => `${convertSizeUnits(valueInDeg, BASE_UNIT, outputUnit)}${ANGULAR_SIZE_UNITS[outputUnit]?.symbol ?? ''}`;

const sizeQuantityProps = {
    quantityName: 'Size',
    convertQuantityUnits: convertSizeUnits,
    formatQuantity: formatSize,
    quantityBaseUnit: BASE_UNIT,
    quantityUnitOptions: Object.keys(ANGULAR_SIZE_UNITS).map((unitKey) => ({
        label: ANGULAR_SIZE_UNITS[unitKey].name,
        value: unitKey
    })),
};


export const SizeInputFields = (props) => {
    const connectContext= useContext(ConnectionCtx);
    const {value, displayValue, unit, ...restInitStateProps} = props?.initialState || {};

    const {min: minProp, max: maxProp} = {...props, ...restInitStateProps};
    const [min, max] = [minProp, maxProp].map(
        (num) => toMaxFixed(num, getFracDigits(BASE_UNIT)) //so that bounds validation respect precision
    );

    return (
        <QuantityInputField {...{
            label: `${sizeQuantityProps.quantityName}: `,
            connectedMarker: connectContext.controlConnected,
            ...sizeQuantityProps,
            ...props, // allow consumer overriding the props above

            // initialState logic only cares about stateful props: value, unit, displayValue
            // but other props might be nested in it, so destructure them out -- for backward compatibility
            initialState: {value, displayValue, unit},
            ...restInitStateProps,
            min, max
        }} />);
};

SizeInputFields.propTypes={
    fieldKey : PropTypes.string,
    groupKey : PropTypes.string,
    connectedMarker: bool,
    sx: object,
    orientation: PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.any,
        unit:  PropTypes.string,
        displayValue: PropTypes.string,
        // following are kept for backward compatibility, they should rather be supplied directly as props to SizeInputFields
        // since initialState logic only cares about stateful props (value, unit, displayValue)
        tooltip:  PropTypes.string,
        min:   PropTypes.number,
        max:   PropTypes.number,
        nullAllowed: PropTypes.bool,
        label:  PropTypes.string,
    }),
    label:       PropTypes.string,
    showFeedback:    PropTypes.bool
};
