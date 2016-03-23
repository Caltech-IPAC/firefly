import React, {PropTypes}  from 'react';
import {isEmpty}  from 'lodash';
import {RadioGroupInputFieldView} from './RadioGroupInputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';


const convertValue= (value,options) => (!value) ? options[0].value : value;

function getProps(params, fireValueChange) {

    var {value,options}= params;
    value= convertValue(value,options);

    return Object.assign({}, params,
        {
            value,
            onChange: (ev) => handleOnChange(ev,params, fireValueChange)
        });
}

function handleOnChange(ev, params, fireValueChange) {
    var val = ev.target.value;
    var checked = ev.target.checked;

    if (checked) {
        fireValueChange({ value: val });
    }
}


const propTypes= {
    inline : PropTypes.bool,
    options: PropTypes.array.isRequired,
    alignment:  PropTypes.string,
    labelWidth : PropTypes.number
};

function checkForUndefined(v,props) {
    return  (!v && !isEmpty(props.options)) ? props.options[0].value : v;
}


export const RadioGroupInputField= fieldGroupConnector(RadioGroupInputFieldView,
    getProps,propTypes,null,checkForUndefined);

