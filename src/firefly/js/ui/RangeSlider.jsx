import React, {memo} from 'react';
import PropTypes, {element,object,bool,objectOf,func,arrayOf,shape,number,string} from 'prop-types';
import {isNaN} from 'lodash';
import {RangeSliderView} from './RangeSliderView.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';

/**
 * @summary callback to handle slider value change
 * @param {string} value
 * @param {Object} params
 * @param {function} fireValueChange
 */
function handleOnChange(value, params, fireValueChange){
     fireValueChange?.({value});

    const {min, max, minStop=min, maxStop=max} = params;    //displayValue in string, min, max, step: number
    const val = parseFloat(value);

    if (!isNaN(val) && val >= minStop && val <= maxStop) params?.onValueChange?.(val);
}


export const RangeSlider= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    return (<RangeSliderView {...viewProps}
                            handleChange={(value) => handleOnChange(value, viewProps, fireValueChange)}/>);
});


RangeSlider.propTypes={
    fieldKey: string,
    groupKey: string,
    associatedKey: string,
    label:       string,             // slider label
    slideValue:  PropTypes.oneOfType([PropTypes.string,PropTypes.number]).isRequired, // slider value
    onValueChange: func,                  // callback on slider change
    min:         number,                  // minimum end of slider
    max:         number,                  // maximum end of slider
    className:   string,                  // class name attached to slider component
    marks:       arrayOf(objectOf(
        shape({
            label: string,
            value: number,
        })
    )),   // marks shown on slider
    step:        number,                       // slider step size
    vertical:    bool,                         // slider is in vertical
    defaultValue: number,                      // default value of slider
    handle:      element,                      // custom made slider handle
    style: object,                             // style for entire component
    sliderStyle: object,                       // style for slider component
    tooltip:  string,                          // tooltip on label
    minStop:  number,                          // minimum value the slider can be changed to
    maxStop:  number,                          // maximum value the slider can be changed to
    errMsg: string                            // message for invalid value
};

