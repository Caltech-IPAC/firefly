import React, {PropTypes}  from 'react';
import {InputFieldLabel} from './InputFieldLabel.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';


const isChecked= (val,fieldValue) => (fieldValue.split(',').indexOf(val) > -1);

const getCurrentValueArr= (v) => v ? v.split(',') : [];


function convertValue(value,options) {
    if (value === '_all_') return options.map( (op) => op.value).toString();
    else if (!value || value === '_none_') return '';
    else return value;
}

export function CheckboxGroupInputFieldView({fieldKey, onChange, label, tooltip, labelWidth,
                                             options, alignment, value }) {

    return (
        <div style={{whiteSpace: 'nowrap'}}>
            {label && <InputFieldLabel label={label} tooltip={tooltip} labelWidth={labelWidth} />}
            {options.map( (option) => {
                return (
                    <div key={option.value}
                         style={alignment==='vertical' ? {display:'block'}:{display:'inline-block'}}>
                        <input type='checkbox'
                               name={fieldKey}
                               value={option.value}
                               checked={isChecked(option.value,value)}
                               onChange={onChange}
                        />&nbsp;{option.label}&nbsp;&nbsp;
                    </div>
                );
            })}
        </div>
    );
}

CheckboxGroupInputFieldView.propTypes= {
    options : PropTypes.array.isRequired,
    onChange:  PropTypes.func,
    alignment:  PropTypes.string,
    fieldKey:  PropTypes.string,
    value:  PropTypes.string,
    label:  PropTypes.string,
    tooltip:  PropTypes.string,
    labelWidth: PropTypes.number
};


function getProps(params, fireValueChange) {

    var {value,options}= params;
    value= convertValue(value,options);
    
    return Object.assign({}, params,
        { value,
          onChange: (ev) => handleOnChange(ev,params, fireValueChange)
        });
}


function handleOnChange(ev, params, fireValueChange) {
    // when a checkbox is checked or unchecked
    // the array, representing the value of the group,
    // needs to be updated
    var value= convertValue(params.value,params.options);

    var val = ev.target.value;
    var checked = ev.target.checked;
    var curValueArr = getCurrentValueArr(value);
    var idx = curValueArr.indexOf(val);
    if (checked) {
        if (idx < 0) curValueArr.push(val); // add val to the array
    }
    else {
        if (idx > -1) curValueArr.splice(idx, 1); // remove val from the array
    }

    var {valid,message} = params.validator(curValueArr.toString());

    fireValueChange({ value: curValueArr.toString(), message, valid });

}
const propTypes= {
    options : PropTypes.array.isRequired,
    alignment:  PropTypes.string
};

export const CheckboxGroupInputField= fieldGroupConnector(CheckboxGroupInputFieldView,
                                                          getProps,propTypes,null);
