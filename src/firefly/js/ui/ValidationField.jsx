import React, {Component,PropTypes} from 'react';
import {pick} from 'lodash';

import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';


function onChange(ev, store, fireValueChange) {

    var {valid,message}= store.validator(ev.target.value);

    fireValueChange({ value : ev.target.value, message, valid });
}



function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            onChange: (ev) => onChange(ev,params, fireValueChange),
            value: String(params.value)
        });
}

const propTypes= {
      inline : React.PropTypes.bool
};



export const ValidationField= fieldGroupConnector(InputFieldView,getProps,propTypes);
