import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';

import {has} from 'lodash';
import {InputAreaFieldView} from './InputAreaFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';

function shouldAct(e, actOn) {
    if (e.type.startsWith('key')) {
        return (actOn.includes('enter') && e.key === 'Enter');
    } else if (e.type.startsWith('blur')) {
        return actOn.includes('blur');
    } else {
        return actOn.includes('changes');
    }
}

function newState({fieldKey='undef', valid=true, message='', value=''}) {
    return {fieldKey, valid, message, value};
}

export class InputAreaField extends PureComponent {
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
            var {valid, message, ...others} = validator ? validator(value) : {valid: true, message: ''};
            has(others, 'value') && (nState.value = others.value);    // allow the validator to modify the value.. useful in auto-correct.
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

        var {label, labelWidth, rows, cols, tooltip, visible, inline, size, showWarning, style, wrapperStyle} = this.props;
        var {valid, value, message} = this.state;
        return (
            <InputAreaFieldView
                valid={valid}
                visible={visible}
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
                rows={rows}
                cols={cols}
            />
        );

    };

}


InputAreaField.propTypes = {
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
    showWarning: PropTypes.bool,
    rows: PropTypes.number,
    cols: PropTypes.number
};

InputAreaField.defaultProps = {
    showWarning: true,
    actOn: ['changes'],
    labelWidth: 0,
    value: '',
    inline: false,
    visible: true,
    rows: 10,
    cols: 50
};

function onChange(ev, store, fireValueChange) {

    var {valid,message}= store.validator(ev.target.value);

    fireValueChange({ value : ev.target.value, message, valid });
}

function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            onChange: (ev) => onChange(ev, params, fireValueChange),
            value: String(params.value)
        });
}


export const InputAreaFieldConnected = fieldGroupConnector(InputAreaFieldView, getProps, InputAreaField.propTypes);
