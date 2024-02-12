import React from 'react';
import {func, bool, element, object, arrayOf, number, objectOf, shape, string, oneOfType} from 'prop-types';
import {has, isNumber, isString, isObject, isNaN} from 'lodash';
import {Slider, Stack, Typography, Tooltip} from '@mui/joy';

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
    min:   number,
    max:   number,
    minStop:  number,
    maxStop:  number,
    className: string,
    // react keeps showing warnings (i think wrongly) so I am commenting this out
    // marks: arrayOf(objectOf(
    //     shape({
    //         label: string,
    //         value: number,
    //     })
    // )),   // marks shown on slider
    step: number,
    vertical: bool,
    defaultValue: number,
    slideValue: oneOfType([string,number]).isRequired,
    handle: element,
    style: object,
    sx: object,
    label: string,
    labelWidth: number,
    tooltip:  string,
    decimalDig: number,
    handleChange: func           // callback on slider change
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
