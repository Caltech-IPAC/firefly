import React, {Component, PropTypes}  from 'react';
import { has, isNaN} from 'lodash';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {dispatchValueChange} from '../fieldGroup/FieldGroupCntlr.js';
import {RangeSliderView, adjustMax, checkMarksObject} from './RangeSliderView.jsx'

function getProps(params, fireValueChange) {

    return Object.assign({}, params,
        {
            handleChange: (v) => handleOnChange(v, params, fireValueChange),
        });
}

/**
 * @summary callback to handle slider value change
 * @param {string} value
 * @param {Object} params
 * @param {function} fireValueChange
 */
function handleOnChange(value, params, fireValueChange){
     fireValueChange&&fireValueChange({
         value
     });

    var {min, max} = params;    //displayValue in string, min, max, step: number
    var {minStop=min, maxStop=max} = params;
    var val = parseFloat(value);

    if (!isNaN(val) && val >= minStop && val <= maxStop) {
        if (has(params, 'onValueChange')) {
            params.onValueChange(val);
        }
    }
}

const propTypes={
    associatedKey: PropTypes.string,
    label:       PropTypes.string,             // slider label
    slideValue:  PropTypes.string.required,    // slider value
    value:       PropTypes.string,
    onValueChange: PropTypes.func,                  // callback on slider change
    min:         PropTypes.number,                  // minimum end of slider
    max:         PropTypes.number,                  // maximum end of slider
    className:   PropTypes.string,                  // class name attached to slider component
    marks:       PropTypes.objectOf(checkMarksObject),   // marks shown on slider
    step:        PropTypes.number,                       // slider step size
    vertical:    PropTypes.bool,                         // slider is in vertical
    defaultValue: PropTypes.number,                      // default value of slider
    handle:      PropTypes.element,                      // custom made slider handle
    wrapperStyle: PropTypes.object,                      // wrapper style for entire component
    sliderStyle: PropTypes.object,                       // style for slider component
    labelWidth: PropTypes.number,                        // label width
    tooltip:  PropTypes.string,                          // tooltip on label
    minStop:  PropTypes.number,                          // minimum value the slider can be changed to
    maxStop:  PropTypes.number,                          // maximum value the slider can be changed to
    errMsg: PropTypes.string                            // message for invalid value
};

export const RangeSlider = fieldGroupConnector(RangeSliderView, getProps, propTypes, null);

