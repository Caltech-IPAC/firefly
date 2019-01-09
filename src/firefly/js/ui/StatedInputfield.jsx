import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {has} from 'lodash';

import {InputFieldView} from './InputFieldView.jsx';



const ARROW_UP = 38;
const ARROW_DOWN = 40;


export class StateInputField extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {
            value: props.defaultValue
        };
        this.onChange= this.onChange.bind(this);
    }

    onChange(ev) {
        const {validator, valueChange}= this.props;
        let value = ev.target.value;
        const {valid,message, ...others}= validator ? validator(value) : {valid:true, message:''};
        has(others, 'value') && (value = others.value);    // allow the validator to modify the value.. useful in auto-correct.
        valueChange && valueChange({ value, message, valid });
        this.setState(() => ({valid, value}));
    }

    componentWillReceiveProps(nextProps) {
        this.setState(() => ({valid:true, value:nextProps.defaultValue}));
    }


    render() {
        const {visible=true, message, label='', tooltip, labelWidth= 100, showWarning,
            style='', wrapperStyle='', onKeyDown, onKeyUp}= this.props;
        const {value, valid}= this.state;

        return (
            <InputFieldView
            valid={valid}
            visible={visible}
            message={message}
            onChange={this.onChange}
            label={''}
            value={value}
            tooltip={tooltip}
            labelWidth={labelWidth}
            inline={true}
            showWarning={showWarning}
            style={style}
            wrapperStyle={wrapperStyle}
            onKeyDown={onKeyDown}
            onKeyUp={onKeyUp}
        />
        );
    }

}

StateInputField.propTypes= {
    message : PropTypes.string,
    tooltip : PropTypes.string,
    label : PropTypes.string,
    inline : PropTypes.bool,
    labelWidth: PropTypes.number,
    style: PropTypes.object,
    wrapperStyle: PropTypes.object,
    labelStyle: PropTypes.object,
    defaultValue: PropTypes.string.isRequired,
    size : PropTypes.number,
    showWarning : PropTypes.bool,
    type: PropTypes.string,
    validator: PropTypes.func,
    onKeyUp: PropTypes.func,
    onKeyDown: PropTypes.func,
    valueChange: PropTypes.func.isRequired
};

StateInputField.defaultProps= {
    showWarning : true,
    valid : true,
    visible : true,
    message: '',
    type: 'text',
    style: {},
    wrapperStyle: {}
};
