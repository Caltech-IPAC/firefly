import React, {Component, PropTypes}  from 'react';
import {get, has, isNumber, isString, isObject, isNaN} from 'lodash';

import Slider from 'rc-slider';
import './rc-slider.css';

/**
 * @summary slider component
 */
export class RangeSliderView extends Component {
    constructor(props) {
        super(props);

        this.state = {value: parseFloat(props.slideValue)};
        this.onSliderChange = this.onSliderChange.bind(this);
        var d = get(props, 'decimalDig', 3);
        this.decimalDig = (d < 0) ? 0 : (d > 20) ? 20 : d;
    }

    componentWillReceiveProps(nextProps) {
        this.setState( {value: parseFloat(nextProps.slideValue)} );
    }


    onSliderChange(v) {
        var {handleChange, minStop, maxStop} = this.props;

        if (minStop && v < minStop ) {
            v = minStop;
        } else if (maxStop && v > maxStop) {
            v = maxStop;
        }

        this.setState({value: v});
        v = parseFloat(v.toFixed(this.decimalDig));

        if (handleChange) {
            handleChange(`${v}`);      //value could be any number of decimal
        }
    }


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

        return (
            <div style={wrapperStyle}>
                <div style={sliderStyle} display='flex'>
                    <div title={tooltip} style={{width: labelWidth, marginBottom: 5}}>{label}</div>
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
    slideValue: PropTypes.string.isRequired,
    value: PropTypes.string,
    handle: PropTypes.element,
    wrapperStyle: PropTypes.object,
    sliderStyle: PropTypes.object,
    label: PropTypes.string,
    labelWidth: PropTypes.number,
    tooltip:  PropTypes.string,
    decimalDig: PropTypes.number,
    handleChange: PropTypes.func           // callback on slider change
};

RangeSliderView.defaultProps = {
    min: 0,
    max: 100,
    step: 1,
    vertical: false,
    defaultValue: 0,
    value: 0.0,
    label: '',
    decimalDig: 3
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

