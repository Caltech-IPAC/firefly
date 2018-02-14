import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import { has, isNaN} from 'lodash';
import {RangeSliderView, checkMarksObject} from './RangeSliderView.jsx';
import {FieldGroupEnable} from './FieldGroupEnable.jsx';

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

    const {min, max} = params;    //displayValue in string, min, max, step: number
    const {minStop=min, maxStop=max} = params;
    const val = parseFloat(value);

    if (!isNaN(val) && val >= minStop && val <= maxStop) {
        if (has(params, 'onValueChange')) {
            params.onValueChange(val);
        }
    }
}


// export const RangeSlider = fieldGroupConnector(RangeSliderView, getProps, propTypes, null);


export class RangeSlider extends PureComponent {

    render()  {
        const {fieldKey, groupKey, onValueChange, ...restOfProps}= this.props;
        return (
            <FieldGroupEnable fieldKey={fieldKey} groupKey={groupKey} onValueChange={onValueChange} {...restOfProps}>
                {
                    (propsFromStore, fireValueChange) => {
                        const newProps= Object.assign({}, restOfProps, { value:propsFromStore.value});
                        return <RangeSliderView {...newProps}
                                           handleChange={(value) => handleOnChange(value,propsFromStore, fireValueChange)}/> ;
                    }
                }
            </FieldGroupEnable>
        );

    }
}

RangeSlider.propTypes={
    fieldKey: PropTypes.string,
    groupKey: PropTypes.string,
    associatedKey: PropTypes.string,
    label:       PropTypes.string,             // slider label
    slideValue:  PropTypes.oneOfType([PropTypes.string,PropTypes.number]).isRequired, // slider value
    value:       PropTypes.oneOfType([PropTypes.string,PropTypes.number]),
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

