import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {get, isEmpty}  from 'lodash';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';

import InputFieldLabel from './InputFieldLabel.jsx';


function getCurrentValueArr(value) {
    if (value) {
        return (typeof value === 'string') ? value.split(',') : [value];
    }
    else {
        return [];
    }
}

const convertValue= (value,options) => (!value) ? get(options, [0, 'value']) : value;


export function ListBoxInputFieldView({inline, value, onChange, fieldKey, options,
                                       multiple, labelWidth, tooltip, label, wrapperStyle, selectStyle, readonly=false}) {

    var vAry= getCurrentValueArr(value);
    const style = Object.assign({whiteSpace:'nowrap', display: inline?'inline-block':'block'}, wrapperStyle);
    return (
        <div style={style}>
            {label && <InputFieldLabel label={label} tooltip={tooltip} labelWidth={labelWidth} />}
            <select name={fieldKey}
                    title={tooltip}
                    style={selectStyle}
                    multiple={multiple}
                    onChange={onChange}
                    disabled={readonly}
                    value={multiple ? vAry : value}>
                {options.map(( (option) => {
                    const optLabel = option.label || option.value;
                    return (
                        <option value={option.value}
                                key={option.value||0}
                                style={{paddingLeft: 5, paddingRight: 3}}
                                title={option.tooltip}
                                disabled={option.disabled ? 'disabled' : false}>
                            {optLabel}
                        </option>
                    );
                }))}
            </select>
        </div>
    );
}



ListBoxInputFieldView.propTypes= {
    options : PropTypes.array,
    value:  PropTypes.any,
    fieldKey : PropTypes.string,
    onChange:  PropTypes.func,
    inline : PropTypes.bool,
    multiple : PropTypes.bool,
    label:  PropTypes.string,
    tooltip:  PropTypes.string,
    labelWidth : PropTypes.number,
    selectStyle: PropTypes.object,
    wrapperStyle: PropTypes.object,
    readonly: PropTypes.bool
};

function handleOnChange(ev, params, fireValueChange) {
    var options = ev.target.options;
    var val = [];
    for (var i = 0; i<options.length; i++) {
        if (options[i].selected) {
            val.push(options[i].value);
        }
    }

    var {valid,message}=params.validator(val.toString());

    // the value of this input field is a string
    fireValueChange({
        value : val.toString(),
        message,
        valid
    });
    if (params.onChange) params.onChange(ev);
}



function checkForUndefined(v,props) {
    const multiple = props.multiple || false;

    return isEmpty(props.options) ? v :
            (!v && !multiple ? props.options[0].value : v);
}


export const ListBoxInputField= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector({...props, confirmValue:checkForUndefined});
    const newProps= {
        ...viewProps,
        value: !viewProps.multiple ? convertValue(viewProps.value, viewProps.options) : viewProps.value,
        onChange: (ev) => handleOnChange(ev,viewProps, fireValueChange)
    };
    return (<ListBoxInputFieldView {...newProps} />);
});



ListBoxInputField.propTypes= {
    fieldKey : PropTypes.string,
    groupKey : PropTypes.string,
    forceReinit:  PropTypes.bool,
    initialState: PropTypes.shape({
        value: PropTypes.string,
        tooltip: PropTypes.string,
        label:  PropTypes.string,
    }),
    inline : PropTypes.bool,
    options : PropTypes.array,
    multiple : PropTypes.bool,
    labelWidth : PropTypes.number,
    readonly: PropTypes.bool
};

