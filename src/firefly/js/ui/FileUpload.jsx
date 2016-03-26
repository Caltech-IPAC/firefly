// import React, {PropTypes} from 'react';
import {get} from 'lodash';

import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {fetchUrl} from '../util/WebUtil.js';
import {getRootPath} from '../util/BrowserUtil.js';

const UL_URL = getRootPath() + 'sticky/Firefly_FileUpload';

function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            message: params.message,
            value: params.displayValue,
            type: 'file',
            style: {border: 'none', background: 'none'},
            labelWidth: 0,
            onChange: (ev) => handleChange(ev, fireValueChange)
        });
}

function handleChange(ev, fireValueChange) {
    var file = get(ev, 'target.files.0');
    var displayValue = get(ev, 'target.value');

    fireValueChange({
        displayValue,
        value: makeDoUpload(file, displayValue)
    });
}

function makeDoUpload(file) {
    const options = {
        method: 'post',
        params: {file}
    };
    return () => {
        return fetchUrl(UL_URL, options).then( (response) => {
            return response.text().then( (text) => {
                // text is in format ${status}::${message}::${cacheKey}
                const resp =  text.split('::');
                const valid = get(resp, [0]) === '200';
                const message = get(resp, [1]);
                const value = get(resp, [2]);
                return {valid, message, value};
            });
        }).catch(function(err) {
            return { valid: false, message: `Unable to upload file: ${get(file, 'name')}`};
        });
    };
}


export const FileUpload = fieldGroupConnector(InputFieldView, getProps);

