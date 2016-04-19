import React, {PropTypes}  from 'react';
import {isEmpty}  from 'lodash';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';

import InputFieldLabel from './InputFieldLabel.jsx';


function getCurrentValueArr(value) {
    if (value) {
        return (typeof value === 'string') ? value.split(',') : [value];
    }
    else {
        return [];
    }
}

const convertValue= (value,options) => (!value) ? options[0].value : value;


export function ListBoxInputFieldView({inline, value, onChange, fieldKey, options,
                                       multiple, labelWidth, tooltip, label,wrapperStyle}) {

    var vAry= getCurrentValueArr(value);
    const style = Object.assign({whiteSpace:'nowrap', display: inline?'inline-block':'block'}, wrapperStyle);
    return (
        <div style={style}>
            <InputFieldLabel label={label} tooltip={tooltip} labelWidth={labelWidth} />
            <select name={fieldKey}
                    title={tooltip}
                    multiple={multiple}
                    onChange={onChange}
                    value={multiple ? vAry : value}>
                {options.map(( (option) => {
                    return (
                        <option value={option.value}
                                key={option.value}>
                            &nbsp;{option.label}&nbsp;
                        </option>
                    );
                }))}
            </select>
        </div>
    );
}



ListBoxInputFieldView.propTypes= {
    options : PropTypes.array.isRequired,
    value:  PropTypes.any,
    fieldKey : PropTypes.string,
    onChange:  PropTypes.func,
    inline : PropTypes.bool,
    multiple : PropTypes.bool,
    label:  PropTypes.string,
    tooltip:  PropTypes.string,
    labelWidth : React.PropTypes.number,
    wrapperStyle: PropTypes.object,
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
}


const propTypes= {
    inline : React.PropTypes.bool,
    options : React.PropTypes.array.isRequired,
    multiple : React.PropTypes.bool,
    labelWidth : React.PropTypes.number
};

function checkForUndefined(v,props) {
    var optionContain = (v) => props.options.find ( (op) => op.value === v );

    return isEmpty(props.options) ? v :
            (!v ? props.options[0].value : (optionContain(v) ? v : props.options[0].value));
}

export const ListBoxInputField = fieldGroupConnector(ListBoxInputFieldView,
                                                     getProps,propTypes,null,checkForUndefined);

