import React from 'react';
import PropTypes from 'prop-types';
import {has, isNumber, isString, isObject, isNaN} from 'lodash';
import Slider from 'rc-slider';
import './rc-slider.css';


export function RangeSliderView({min=0, max=100, minStop, maxStop, className, marks, step=1, vertical=false,
                                    defaultValue=0, slideValue, handle, style={}, sliderStyle={},
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

    const onSliderChange = (v) => {
        const numDecimalDigits = (decimalDig < 0) ? 0 : (decimalDig > 20) ? 20 : decimalDig;

        handleChange?.(toRangeBound(v).toFixed(numDecimalDigits));
    };

    return (
        <div style={style}>
            <div style={{display: 'flex', ...sliderStyle}}>
                <div title={tooltip} style={{width: labelWidth, marginBottom: 5}}>{label}</div>
                <Slider min={min}
                        max={max}
                        className={className}
                        marks={marks}
                        step={step}
                        vertical={vertical}
                        defaultValue={defaultValue}
                        value={toRangeBound(parseFloat(slideValue))}
                        handle={handle}
                        tipFormatter={null}
                        included={true}
                        onChange={onSliderChange} />
            </div>
        </div>
    );
}


RangeSliderView.propTypes = {
    min:   PropTypes.number,
    max:   PropTypes.number,
    minStop:  PropTypes.number,
    maxStop:  PropTypes.number,
    className: PropTypes.string,
    marks: PropTypes.objectOf(checkMarksObject),
    step: PropTypes.number,
    vertical: PropTypes.bool,
    defaultValue: PropTypes.number,
    slideValue: PropTypes.oneOfType([PropTypes.string,PropTypes.number]).isRequired,
    handle: PropTypes.element,
    style: PropTypes.object,
    sliderStyle: PropTypes.object,
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
