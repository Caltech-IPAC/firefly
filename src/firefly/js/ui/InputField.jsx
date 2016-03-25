import React, {PropTypes} from 'react';

import {InputFieldView} from './InputFieldView.jsx';


function shouldAct(e, actOn) {
    if (e.type.startsWith('key') ) {
        return (actOn.includes('enter') && e.key === 'Enter');
    } else if (e.type.startsWith('blur')) {
        return  actOn.includes('blur');
    } else {
        return actOn.includes('changes');
    }
}

function newState({fieldKey='undef', valid=true, message='', value=''}) {
    return {fieldKey, valid, message, value};
}

export class InputField extends React.Component {
    constructor(props) {
        super(props);

        this.state = newState({value: props.value});
        this.handleChanges = this.handleChanges.bind(this);
    }

    handleChanges(e) {
        var {fieldKey, validator, onChange, label='', actOn} = this.props;
        const value = e.target.value;
        const nState = {fieldKey, value};
        if (shouldAct(e, actOn)) {
            var {valid, message} = validator ? validator(value) : {valid:true, message:''};
            nState.valid = valid;
            nState.message = valid ? '' : (label + message).replace('::', ':');
            onChange && onChange(nState);
        }
        this.setState(nState);
    }

    componentWillReceiveProps(nProps) {
        this.setState(newState({value: nProps.value}));
    }

    render() {

        var {label, labelWidth, tooltip, visible, inline, size, showWarning, style, wrapperStyle} = this.props;
        var {valid, value, message} = this.state;
        return (
            <InputFieldView
                valid={valid}
                visible= {visible}
                message={message}
                onChange={this.handleChanges}
                onBlur={this.handleChanges}
                onKeyPress={this.handleChanges}
                value={value}
                tooltip={tooltip}
                label={label}
                style={style}
                inline={inline}
                labelWidth={labelWidth}
                size={size}
                showWarning={showWarning}
                wrapperStyle={wrapperStyle}
            />
        );

    };

}


InputField.propTypes = {
    fieldKey: PropTypes.string,
    label: PropTypes.string,
    labelWidth: PropTypes.number,
    validator: PropTypes.func,
    tooltip: PropTypes.string,
    visible: PropTypes.bool,
    inline: PropTypes.bool,
    size: PropTypes.number,
    style: PropTypes.object,
    wrapperStyle: PropTypes.object,
    value: PropTypes.string,
    onChange: PropTypes.func,
    actOn: PropTypes.arrayOf(PropTypes.oneOf(['blur', 'enter', 'changes'])),
    showWarning : PropTypes.bool,
};

InputField.defaultProps = {
    showWarning : true,
    actOn: ['changes'],
    labelWidth: 0,
    value: '',
    inline: false,
    visible: true
};


