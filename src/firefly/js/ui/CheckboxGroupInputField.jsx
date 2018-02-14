import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {InputFieldLabel} from './InputFieldLabel.jsx';
import {FieldGroupEnable} from './FieldGroupEnable.jsx';


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
        <div style={style}>
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



function handleOnChange(ev, propsFromStore, fireValueChange) {
    // when a checkbox is checked or unchecked
    // the array, representing the value of the group,
    // needs to be updated
    const value= convertValue(propsFromStore.value,propsFromStore.options);

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
    const {valid,message} = propsFromStore.validator(curValueArr.toString());

    fireValueChange({ value: curValueArr.toString(), message, valid });

    if (propsFromStore.onChange) {
        propsFromStore.onChange(ev);
    }
}



export class CheckboxGroupInputField extends PureComponent {

    render()  {
        const {fieldKey, initialState, forceReinit, options}= this.props;
        return (
            <FieldGroupEnable fieldKey={fieldKey} initialState={initialState}
                               forceReinit={forceReinit} options={options}>
                {
                    (propsFromStore, fireValueChange) => {
                        const {value,options}= propsFromStore;
                        const newProps= Object.assign({}, this.props, propsFromStore,
                                                         { value: convertValue(value,options)});
                        return (
                            <CheckboxGroupInputFieldView {...newProps}
                                               onChange={(ev) => handleOnChange(ev,propsFromStore, fireValueChange)}/>
                        );
                    }

                }
            </FieldGroupEnable>
        );

    }
}

CheckboxGroupInputField.propTypes= {
    options : PropTypes.array.isRequired,
    alignment:  PropTypes.string,
    initialState:  PropTypes.object,
    forceReinit:  PropTypes.bool,
    fieldKey:   PropTypes.string
};

