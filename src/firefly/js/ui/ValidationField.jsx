import React from 'react';
import PropTypes from 'prop-types';
import {has} from 'lodash';

import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {InputAreaFieldView} from './InputAreaFieldView';


function onChange(ev, store, fireValueChange) {
    var value = ev.target.value;
    var {valid,message, ...others}= store.validator(value);
    has(others, 'value') && (value = others.value);    // allow the validator to modify the value.. useful in auto-correct.

    fireValueChange({ value, message, valid });
}

function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            onChange: (ev) => onChange(ev,params, fireValueChange),
            value: String(params.value)
        });
}

const propTypes= {
      inline : PropTypes.bool
};

export const ValidationField= fieldGroupConnector(InputFieldView,getProps,propTypes);
export const ValidationTextArea= fieldGroupConnector(InputAreaFieldView,getProps,propTypes);
