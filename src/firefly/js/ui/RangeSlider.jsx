import React, {Component, PropTypes}  from 'react';
import {get, has, isNumber, isString, isObject, isNil, isNaN} from 'lodash';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {InputFieldView}  from './InputFieldView.jsx';
import {dispatchValueChange} from '../fieldGroup/FieldGroupCntlr.js';

import Slider from 'rc-slider';
import './rc-slider.css';

const DEC = 8;

function getProps(params, fireValueChange) {

    return Object.assign({}, params,
        {
            handleChange: (v) => handleOnChange(v, params, fireValueChange),
            handleMaxChange: (v) => handleOnMaxChange(v, params, fireValueChange)
        });
}

/**
 * @summary callback to handle slider value change
 * @param {string} value
 * @param {Object} params
 * @param {function} fireValueChange
 */
function handleOnChange(value, params, fireValueChange){
     fireValueChange({
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

/**
 * @summary adjust the maximum to be the multiple of the step resolution if the maximum on the slider is updated
 * @param {number} max
 * @param {number} min
 * @param {number} step
 * @returns {{steps: number, max: number}}
 */
function adjustMax(max, min, step) {
    var newTotalSteps = Math.ceil((max - min) / step);
    var newMax = parseFloat((newTotalSteps*step + min).toFixed(DEC));
    var res = (newMax - min)/newTotalSteps;

    return {steps: newTotalSteps, max: newMax, res};
}

/**
 * @summary callback to handle the slider value change  entered in the input field
 * @param {string} vText
 * @param {Object} params
 * @param {function} fireValueChange
 */
function handleOnMaxChange(vText, params, fireValueChange) {
    var value = vText;
    var {steps, max} = adjustMax(parseFloat(vText), params.min, params.step);

    fireValueChange({
        value
    });

    var {groupKey} = params;
    var payload = Object.assign({}, {value: max}, {groupKey, fieldKey: 'periodMax'});
    dispatchValueChange(payload);
    payload = Object.assign({}, {value: steps}, {groupKey, fieldKey: 'periodSteps'});
    dispatchValueChange(payload);

    if (has(params, 'onValueChange')) {
        params.onValueChange(parseFloat(value));
    }
}

const propTypes={
    label:       PropTypes.string,                  // slider label
    value:       PropTypes.string.required,         // slider value
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
    canEnterValue: PropTypes.bool,                       // if the slider value can be mannually entered
    errMsg: PropTypes.string                            // message for invalid value
};


/**
 * @summary slider component
 */
class RangeSliderView extends Component {
    constructor(props) {
        super(props);

        var {value: displayValue, min, max, step} = props;
        this.state = {displayValue, min, max, step};

        this.onSliderChange = this.onSliderChange.bind(this);
        this.onValueChange = this.onValueChange.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        var {value: displayValue,  min, max, step} = nextProps;

        this.setState( {displayValue, min, max, step} );
    }


    onSliderChange(v) {
        var {handleChange, canEnterValue, minStop, maxStop} = this.props;

        if (!canEnterValue) {
            if (minStop && v < minStop ) {
                v = minStop;
            } else if (maxStop && v > maxStop) {
                v = maxStop;
            }
        }
        if (handleChange) {
            handleChange(`${v}`);      //value could be any number of decimal
        }

        this.setState({displayValue: `${v}`});

    }

    onValueChange(e) {
        var vText = get(e, 'target.value');
        var {max, handleMaxChange, handleChange, min, step} = this.props;

        if (!isNil(vText) && vText) {
              var val = parseFloat(vText);

              if (isNaN(val)) {

                  if (handleChange) {
                      handleChange(vText);
                  } else {
                      this.setState({displayValue: vText});
                  }
              } else if (val >  max) {     // value exceeds current max
                  if (handleMaxChange) {
                      handleMaxChange(vText);
                  } else {
                      var aMax = adjustMax(val, min, step );
                      this.setState({displayValue: vText, max: aMax.max, step: aMax.res});
                  }
              } else {                    // value within the min and max range
                  if (handleChange) {
                      handleChange(vText);
                  } else {
                      this.setState({displayValue: vText});
                  }
              }
        } else {
            if (handleChange) {
                handleChange('');
            } else {
                this.setState({displayValue: ''});
            }
        }
    }

    render() {
        var {wrapperStyle={}, sliderStyle={}, className, value, marks, vertical,
             defaultValue, handle, label, labelWidth, tooltip, minStop, maxStop, errMsg, canEnterValue} = this.props;
        var {min, max, displayValue, step} = this.state;    //displayValue in string, min, max, step: number
        var {minStop=min, maxStop=max} = this.props;
        var val = parseFloat(displayValue);
        var valid = (!isNaN(val)) && (val >= minStop && val <= maxStop);
        var v  = valid ? parseFloat(value) : min;

        var labelValue = () => {
            if (!label) return null;
            var msg = valid ? '' : errMsg || `invalid value: must be within [${minStop}, ${maxStop}]`;

            return (
                <InputFieldView tooltip={tooltip}
                                label={label}
                                labelWidth={labelWidth}
                                wrapperStyle={{marginBottom: 15}}
                                value={displayValue}
                                style={{width: 125}}
                                onChange={this.onValueChange}
                                type={'text'}
                                valid={valid}
                                message={msg}
                />

            );
        };

        var sliderValue = () => {
            if (!label) return null;

            return (
                <div style={{display: 'flex'}}>
                    <div title={tooltip}
                         style={{width: labelWidth, marginBOttom: 15, marginRight: 4}}>{label}</div>
                    <div>{displayValue}</div>
                </div>
            );
        };

        return (
            <div style={wrapperStyle}>
                {canEnterValue?labelValue():sliderValue()}
                <div style={sliderStyle}>
                    <Slider min={min}
                            max={max}
                            className={className}
                            marks={marks}
                            step={step}
                            vertical={vertical}
                            defaultValue={defaultValue}
                            value={v}
                            handle={handle}
                            tipFormatter={null}
                            onChange={this.onSliderChange} />
                </div>
            </div>
        );
    }
}

RangeSliderView.propTypes = {
    min:   PropTypes.number,
    max:   PropTypes.number,
    className: PropTypes.string,
    marks: PropTypes.objectOf(checkMarksObject),
    step: PropTypes.number,
    vertical: PropTypes.bool,
    defaultValue: PropTypes.number,
    value: PropTypes.string.isRequired,
    handle: PropTypes.element,
    label: PropTypes.string,
    wrapperStyle: PropTypes.object,
    sliderStyle: PropTypes.object,
    labelWidth: PropTypes.number,
    tooltip:  PropTypes.string,
    minStop:  PropTypes.number,
    maxStop:  PropTypes.number,
    canEnterValue: PropTypes.bool,
    errMsg: PropTypes.string
};

RangeSliderView.defaultProps = {
    min: 0,
    max: 100,
    step: 1,
    vertical: false,
    defaultValue: 0,
    value: '0.0',
    label: '',
    canEnterValue: true
};

function checkMarksObject(props, propName, componentName) {
    if (isNumber(propName) ||
        (isString(propName) && parseFloat(propName))) {
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

export const RangeSlider = fieldGroupConnector(RangeSliderView, getProps, propTypes, null);

