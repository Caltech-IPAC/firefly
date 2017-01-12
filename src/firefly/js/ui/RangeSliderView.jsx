import React, {Component, PropTypes}  from 'react';
import {get, has, isNumber, isString, isObject, isNil, isNaN} from 'lodash';
import {InputFieldView}  from './InputFieldView.jsx';

import Slider from 'rc-slider';
import './rc-slider.css';

export const DEC_PHASE = 3;


/**
 * @summary slider component
 */
export class RangeSliderView extends Component {
    constructor(props) {
        super(props);

        this.state = {value: parseFloat(props.value)};

        this.onSliderChange = this.onSliderChange.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        this.setState( {value: nextProps.value} );
    }


    onSliderChange(v) {
        var {handleChange, minStop, maxStop} = this.props;

        if (minStop && v < minStop ) {
            v = minStop;
        } else if (maxStop && v > maxStop) {
            v = maxStop;
        }

        this.setState({value: v});
        v = parseFloat(v.toFixed(DEC_PHASE));

        if (handleChange) {
            handleChange(`${v}`);      //value could be any number of decimal
        }
    }

    /*

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

    */


    render() {
        var {wrapperStyle={}, sliderStyle={}, className, marks, vertical,
             defaultValue, handle, label, labelWidth, tooltip, minStop, maxStop, min, max, step} = this.props;
        var {value} = this.state;    //displayValue in string, min, max, step: number
        var val = parseFloat(value);

        if (minStop && val < minStop ) {
            val = minStop;
        } else if (maxStop && val > maxStop) {
            val = maxStop;
        }


/*
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
*/
        return (
            <div style={wrapperStyle}>
                <div style={sliderStyle} display='flex'>
                    <div title={tooltip} style={{width: labelWidth}}>{label}</div>
                    <Slider min={min}
                            max={max}
                            className={className}
                            marks={marks}
                            step={step}
                            vertical={vertical}
                            defaultValue={defaultValue}
                            value={val}
                            handle={handle}
                            tipFormatter={null}
                            included={true}
                            onChange={this.onSliderChange} />
                </div>
            </div>
        );
    }
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
    value: PropTypes.string.isRequired,
    handle: PropTypes.element,
    wrapperStyle: PropTypes.object,
    sliderStyle: PropTypes.object,
    label: PropTypes.string,
    labelWidth: PropTypes.number,
    tooltip:  PropTypes.string
};

RangeSliderView.defaultProps = {
    min: 0,
    max: 100,
    step: 1,
    vertical: false,
    defaultValue: 0,
    value: 0.0,
    label: ''
};

export function checkMarksObject(props, propName, componentName) {
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

