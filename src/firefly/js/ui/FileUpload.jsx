import React from 'react';
import PropTypes from 'prop-types';
import {get, has, isFunction, isNil} from 'lodash';

import {InputFieldView} from './InputFieldView.jsx';
import {fieldGroupConnector} from './FieldGroupConnector.jsx';
import {fetchUrl} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {ServerParams} from '../data/ServerParams.js';

import LOADING from 'html/images/gxt/loading.gif';
const UL_URL = `${getRootURL()}sticky/CmdSrv?${ServerParams.COMMAND}=${ServerParams.UPLOAD}`;


function FileUploadView({fileType, isLoading, label, valid, wrapperStyle,  message, onChange, value, labelWidth,
                         innerStyle, isFromURL, onUrlAnalysis, fileNameStyle}) {
    var style = !isFromURL ? Object.assign({color: 'transparent', border: 'none', background: 'none'}, innerStyle) : (innerStyle || {});
    var fileName = (!isFromURL && value) ?  value.split(/(\\|\/)/g).pop() : 'No file chosen';

    const inputEntry = () => {
        const labelW = isNil(labelWidth) ? 0 : labelWidth;

        return (
            <InputFieldView
                valid={valid}
                visible={true}
                message={message}
                onChange={onChange}
                type={isFromURL ? 'text' : 'file'}
                label={label}
                value={value}
                tooltip={value}
                labelWidth={labelW}
                inline={true}
                style={style}
                wrapperStyle={wrapperStyle}
            />
        );
    };

    const actionPart = () => {
        if (isFromURL) {
            return (
                <div style={{display:'inline-block', whiteSpace:'nowrap'}}>
                    <button type='button' className='button std hl'
                            onClick={() =>  onUrlAnalysis(value)}>{'Upload'}</button>
                </div>
            );
        } else {
            let fPos = {marginLeft: -150};

            if (!isNil(fileNameStyle)) fPos = Object.assign(fPos, fileNameStyle);
            return (
                fileName && <div style={{display:'inline-block', ...fPos}}>{fileName}</div>
            );
        }
    };

    return (
        <div>
            {inputEntry() }
            {actionPart() }
            {isLoading && <img style={{display: 'inline-block', marginLeft: 10, width:14,height:14}} src={LOADING}/> }
        </div>
    );
}


FileUploadView.propTypes = {
    fileType   : PropTypes.string.isRequired,
    isLoading: PropTypes.bool.isRequired,
    message: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value : PropTypes.string.isRequired,
    innerStyle: PropTypes.object,
    label: PropTypes.string,
    labelWidth: PropTypes.number,
    valid: PropTypes.bool,
    wrapperStyle: PropTypes.object,
    isFromURL: PropTypes.bool.isRequired,
    onUrlAnalysis: PropTypes.func,
    fileNameStyle: PropTypes.object
};

FileUploadView.defaultProps = {
    fileType: 'TABLE',
    isLoading: false,
    isFromURL: false,
    labelWidth: 0
};

export const FileUpload = fieldGroupConnector(FileUploadView, getProps);

/*---------------------------- private ----------------------------*/


function onUrlChange(ev, store, fireValueChange) {
    const displayValue = ev.target.value;
    const {valid,message, ...others}= store.validator(displayValue);
    let value;

    has(others, 'value') && (value = others.value);    // allow the validator to modify the value.. useful in auto-correct.

    fireValueChange({ value, message, valid, displayValue, analysisResult:''});
}

function getProps(params, fireValueChange) {
    if (has(params, 'isFromURL') && params.isFromURL) {
        return Object.assign({}, params,
            {
                onChange: (ev) => onUrlChange(ev, params, fireValueChange),
                value: params.displayValue,
                onUrlAnalysis: (value) => doUrlAnalysis(value, fireValueChange, params.fileType,
                                                                                params.fileAnalysis)
            }
        );
    } else {
        return Object.assign({}, params,
            {
                value: params.displayValue,
                onChange: (ev) => handleChange(ev, fireValueChange, params.fileType, params.fileAnalysis)
            }
        );
    }
}

function doUrlAnalysis(value, fireValueChange, type, fileAnalysis) {
     fireValueChange({value: makeDoUpload(value, type, true, fileAnalysis)()});
}


function handleChange(ev, fireValueChange, type, fileAnalysis) {
    var file = get(ev, 'target.files.0');
    var displayValue = get(ev, 'target.value');

    fireValueChange({
        displayValue,
        value: !fileAnalysis ? makeDoUpload(file, type) : makeDoUpload(file, type, false, fileAnalysis)()
    });
}

function makeDoUpload(file, type, isFromURL, fileAnalysis) {
    return () => {
        return doUpload(file, {type, isFromURL, fileAnalysis}).then(({status, message, cacheKey, fileFormat, analysisResult}) => {
            let valid = status === '200';
            if (valid) {        // json file is not supported currently
                if (!isNil(fileFormat)) {
                    if (fileFormat.toLowerCase() === 'json') {
                        valid = false;
                        message = 'json file is not supported';
                        analysisResult = '';
                    }
                }
            }

            return {isLoading: false, valid, message, value: cacheKey, analysisResult};
        }).catch((err) => {
            return {isLoading: false, valid: false,
                    message: (isFromURL ? `Unable to upload file from ${file}` : `Unable to upload file: ${get(file, 'name')}`)};
        });
    };
}

/**
 * post the data in
 * @param {File} file
 * @param {Object} params additional parameters if any
 * @returns {Promise.<{Object}>}  The returned object is : {status:string, message:string, cacheKey:string}
 */
export function doUpload(file, params={}) {
    if (!file) return Promise.reject('Required file parameter not given');

    if (has(params, 'isFromURL') && params.isFromURL) {
        params = Object.assign(params, {URL: file});
    } else {
        params = Object.assign(params, {file});   // file should be the last param due to AnyFileUpload limitation
    }
    const options = {method: 'multipart', params};

    if (params.fileAnalysis && isFunction(params.fileAnalysis)) {
        params.fileAnalysis();
        options.params.fileAnalysis = true;
    }

    return fetchUrl(UL_URL, options).then( (response) => {
        return response.text().then( (text) => {
            // text is in format ${status}::${message}::${message}::${cacheKey}::${analysisResult}
            const result = text.split('::');
            const [status, message, cacheKey, fileFormat] = result.slice(0, 4);
            const analysisResult = result.slice(4).join('::');
            return {status, message, cacheKey, fileFormat, analysisResult};
        });
    });
}
