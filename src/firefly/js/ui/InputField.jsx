import React from 'react';

import {InputFieldView} from './InputFieldView.jsx';


export class InputField extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            fieldKey: 'undef',
            valid: true,
            message:'',
            value: props.value
        };

        this.onChange = this.onChange.bind(this);
    }

    onChange(e) {
        var {fieldKey, validator, onChange, label} = this.props;
        const value = e.target.value;
        var {valid, message} = validator ? validator(value) : {valid:true, message:''};
        message = valid ? '' : (label + message).replace('::', ':');
        const nState = {fieldKey, valid, message, value};
        onChange && onChange(nState);
        this.setState(nState);
    }

    render() {

        var {label, labelWidth, tooltip, visible, inline, size} = this.props;
        var {valid, value, message} = this.state;

        return (
            <InputFieldView
                style={{}}
                valid={valid}
                visible= {visible}
                message={message}
                onChange={this.onChange}
                value={value}
                tooltip={tooltip}
                label={label}
                inline={inline}
                labelWidth={labelWidth}
                size={size}
            />
        );

    };

}


InputField.propTypes = {
    fieldKey: React.PropTypes.string,
    label: React.PropTypes.string,
    labelWidth: React.PropTypes.number,
    validator: React.PropTypes.func,
    tooltip: React.PropTypes.string,
    visible: React.PropTypes.bool,
    inline: React.PropTypes.bool,
    size: React.PropTypes.number,
    value: React.PropTypes.string,
    onChange: React.PropTypes.func
};

InputField.defaultProps = {
    labelWidth: 0,
    value: '',
    inline: false,
    visible: true
};


