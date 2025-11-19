/**
 * NOTE: "Size" in this component refers to the **Angular Size** (for measuring cone search radius, rotation angle, etc.).
 * It's not the linear size or a measure of (wave)length.
 */

import React, {useContext} from 'react';
import PropTypes, {bool, object} from 'prop-types';
import toFloat from 'validator/es/lib/toFloat';
import {convertAngle} from '../visualize/VisUtil.js';
import {ConnectionCtx} from './ConnectionCtx.js';
import {toMaxFixed} from '../util/MathUtil.js';
import {QuantityInputField} from 'firefly/ui/QuantityInputField';


const DEC_DIGIT = 6;
const BASE_UNIT = 'deg';
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
    const newVal = convertAngle(fromUnit, toUnit, value.toString());
    return toMaxFixed(newVal, DEC_DIGIT).toString();
};

const formatSize = (valueInDeg, outputUnit) => `${convertSizeUnits(valueInDeg, BASE_UNIT, outputUnit)}${ANGULAR_SIZE_UNITS[outputUnit].symbol}`;

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
