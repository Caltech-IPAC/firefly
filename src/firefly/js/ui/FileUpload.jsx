// import React, {PropTypes} from 'react';
import React, {PropTypes} from 'react';
import {get} from 'lodash';

import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {fetchUrl} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';

import LOADING from 'html/images/gxt/loading.gif';
const UL_URL = getRootURL() + 'sticky/Firefly_FileUpload';


function FileUploadView({fileType, isLoading, label, valid, wrapperStyle, message, onChange, value}) {
    return (
        <div>
            <InputFieldView
                valid = {valid}
                visible = {true}
                message = {message}
                onChange = {onChange}
                type = 'file'
                label = {label}
                value = {value}
                tooltip = {value}
                labelWidth = {0}
                inline={true}
                style = {{border: 'none', background: 'none'}}
                wrapperStyle = {wrapperStyle}
            />
            {isLoading && <img style={{position: 'inline-block', width:14,height:14}} src={LOADING}/> }
        </div>
    );
}


FileUploadView.propTypes = {
    fileType   : PropTypes.string.isRequired,
    isLoading: PropTypes.bool.isRequired,
    message: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value : PropTypes.string.isRequired
};

FileUploadView.defaultProps = {
    fileType: 'TABLE',
    isLoading: false
};

export const FileUpload = fieldGroupConnector(FileUploadView, getProps);




/*---------------------------- private ----------------------------*/

function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            value: params.displayValue,
            onChange: (ev) => handleChange(ev, fireValueChange, params.fileType)
        });
}

function handleChange(ev, fireValueChange, type) {
    var file = get(ev, 'target.files.0');
    var displayValue = get(ev, 'target.value');

    fireValueChange({
        displayValue,
        value: makeDoUpload(file, type)
    });
}

function makeDoUpload(file, type) {
    const options = {
        method: 'multipart',
        params: {type, file}     // file should be the last param due to AnyFileUpload limitation
    };
    return () => {
        return {
            isLoading: true,
            value : fetchUrl(UL_URL, options).then( (response) => {
                return response.text().then( (text) => {
                    // text is in format ${status}::${message}::${cacheKey}
                    const resp =  text.split('::');
                    const valid = get(resp, [0]) === '200';
                    const message = get(resp, [1]);
                    const value = get(resp, [2]);
                    return {isLoading: false, valid, message, value};
                });
            }).catch(function(err) {
                return { isLoading: false, valid: false, message: `Unable to upload file: ${get(file, 'name')}`};
            })
        };
    };
}
