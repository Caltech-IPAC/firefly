import React from 'react';
import PropTypes from 'prop-types';
import {has, isNumber, isString, isObject, isNaN} from 'lodash';
import {Slider, Box, Stack, Typography, Tooltip} from '@mui/joy';

export function RangeSliderView({min=0, max=100, minStop, maxStop, marks, step=1, vertical=false,
                                    defaultValue=0, slideValue, sx={},
                                    label='', labelWidth, tooltip, decimalDig=3, handleChange}){
    const toRangeBound = (val) => {
        if (minStop && val < minStop ) {
            val = minStop;
        } else if (maxStop && val > maxStop) {
            val = maxStop;
        }
        if (isNaN(val)) val = minStop || min;
        return val;
    };

    const onSliderChange = (ev) => {
        const val= ev?.target.value;
        const numDecimalDigits = (decimalDig < 0) ? 0 : (decimalDig > 20) ? 20 : decimalDig;
        handleChange?.(toRangeBound(val).toFixed(numDecimalDigits));
    };

    return (
        <Tooltip title={tooltip}>
            <Stack direction='column' p={0} spacing={0} sx={sx}>
                <Typography level='body-xs' width={labelWidth} mb='0'>{label}</Typography>
                <Slider
                    size={'sm'}
                    aria-label='steps'
                    min={min}
                    max={max}
                    marks={marks}
                    step={step}
                    orientation={vertical ? 'vertical' : 'horizontal'}
                    defaultValue={defaultValue}
                    value={toRangeBound(parseFloat(slideValue))}
                    onChange={onSliderChange} />
            </Stack>
        </Tooltip>
    );
}


RangeSliderView.propTypes = {
    min:   PropTypes.number,
    max:   PropTypes.number,
    minStop:  PropTypes.number,
    maxStop:  PropTypes.number,
    className: PropTypes.string,
    marks: PropTypes.array,
    step: PropTypes.number,
    vertical: PropTypes.bool,
    defaultValue: PropTypes.number,
    slideValue: PropTypes.oneOfType([PropTypes.string,PropTypes.number]).isRequired,
    handle: PropTypes.element,
    style: PropTypes.object,
    sx: PropTypes.object,
    label: PropTypes.string,
    labelWidth: PropTypes.number,
    tooltip:  PropTypes.string,
    decimalDig: PropTypes.number,
    handleChange: PropTypes.func           // callback on slider change
};

export function checkMarksObject(props, propName, componentName) {
    if (isNumber(propName) ||
        (isString(propName) && !isNaN(parseFloat(propName)))) {
        if (isString(props[propName]) || (isObject(props[propName]) &&
            has(props[propName], 'style') && has(props[propName], 'label'))) {
            return null;
        } else {
            return new Error('invalid value assigned to ' + propName + ' in ' + componentName + '. Validation failed.');
        }

    } else {
        return new Error('invalid ' + propName + ' supplied to ' + componentName + '. Validation failed');
    }
}
