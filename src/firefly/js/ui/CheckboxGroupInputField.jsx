import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {InputFieldLabel} from './InputFieldLabel.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';


const isChecked= (val,fieldValue) => (fieldValue.split(',').indexOf(val) > -1);

const getCurrentValueArr= (v) => v ? v.split(',') : [];


function convertValue(value,options) {
    if (value === '_all_') return options.map( (op) => op.value).toString();
    else if (!value || value === '_none_') return '';
    else return value;
}

export function CheckboxGroupInputFieldView({fieldKey, onChange, label, tooltip, labelWidth,
                                             options, alignment, value , wrapperStyle}) {
    const style = Object.assign({whiteSpace:'nowrap'}, wrapperStyle);
    return (
        <div style={style} title={tooltip}>
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
                               title={option.title}
                        /><span style={{paddingLeft: 3, paddingRight: 8}}>{option.label}</span>
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
    value:  PropTypes.string.isRequired,
    label:  PropTypes.string,
    tooltip:  PropTypes.string,
    labelWidth: PropTypes.number,
    wrapperStyle: PropTypes.object
};



function handleOnChange(ev, viewProps, fireValueChange) {
    // when a checkbox is checked or unchecked
    // the array, representing the value of the group,
    // needs to be updated
    const value= convertValue(viewProps.value,viewProps.options);

    const val = ev.target.value;
    const checked = ev.target.checked;
    const curValueArr = getCurrentValueArr(value);
    const idx = curValueArr.indexOf(val);
    if (checked) {
        if (idx < 0) curValueArr.push(val); // add val to the array
    }
    else {
        if (idx > -1) curValueArr.splice(idx, 1); // remove val from the array
    }
    const {valid,message} = viewProps.validator(curValueArr.toString());

    fireValueChange({ value: curValueArr.toString(), message, valid });

    if (viewProps.onChange) {
        viewProps.onChange(ev);
    }
}

export const CheckboxGroupInputField = memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    const newProps= {
        ...viewProps,
        value: convertValue(viewProps.value,viewProps.options),
        onChange: (ev) => handleOnChange(ev,viewProps, fireValueChange)
    };
    return ( <CheckboxGroupInputFieldView {...newProps} /> );
});

CheckboxGroupInputField.propTypes= {
    options : PropTypes.array.isRequired,
    alignment:  PropTypes.string,
    initialState: PropTypes.shape({
        value: PropTypes.string,
        tooltip: PropTypes.string,
        label:  PropTypes.string,
    }),
    forceReinit:  PropTypes.bool,
    fieldKey:   PropTypes.string
};

