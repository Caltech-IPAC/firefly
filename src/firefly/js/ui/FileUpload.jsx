import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {get, has, isFunction, isNil, isString} from 'lodash';

import {InputFieldView} from './InputFieldView.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';

import LOADING from 'html/images/gxt/loading.gif';
import {upload} from '../rpc/CoreServices.js';


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
                tooltip={isFromURL ? 'enter a URL to upload from' : 'click to choose a file'}
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
            let fPos = {marginLeft: -150, width: '30em'};

            if (!isNil(fileNameStyle)) fPos = Object.assign(fPos, fileNameStyle);
            return (
                fileName && <div style={{...fPos}} className='text-ellipsis' title={fileName}>{fileName}</div>
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

export const FileUpload= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    let modViewProps;
    if (viewProps.isFromURL) {
        modViewProps= {
            ...viewProps,
            onChange: (ev) => onUrlChange(ev, viewProps, fireValueChange),
            value:  viewProps.displayValue,
            onUrlAnalysis: (value) => doUrlAnalysis(value, fireValueChange, viewProps.fileType, viewProps.fileAnalysis)
        };
    }
    else {
        modViewProps= {
            ...viewProps,
            value: viewProps.displayValue,
            onChange: (ev) => handleChange(ev, fireValueChange, viewProps.fileType, viewProps.fileAnalysis)
        };
    }
    return <FileUploadView {...modViewProps } /> ;
});

FileUpload.propTypes = {
    fieldKey : PropTypes.string.isRequired,
    isFromURL: PropTypes.bool,
    innerStyle: PropTypes.object,
    label: PropTypes.string,
    labelWidth: PropTypes.number,
    wrapperStyle: PropTypes.object,
    fileNameStyle: PropTypes.object,
    fileAnalysis: PropTypes.func,

    initialState: PropTypes.shape({
        tooltip: PropTypes.string,
        label:  PropTypes.string,
    }),
};







/*---------------------------- private ----------------------------*/


function onUrlChange(ev, store, fireValueChange) {
    const displayValue = ev.target.value;
    const {valid,message, ...others}= store.validator(displayValue);
    let value;

    has(others, 'value') && (value = others.value);    // allow the validator to modify the value.. useful in auto-correct.

    fireValueChange({ value, message, valid, displayValue, analysisResult:''});
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
        return doUpload(file, fileAnalysis, {}).then(({status, message, cacheKey, fileFormat, analysisResult}) => {
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

function doUpload(fileOrUrl, fileAnalysis, params={}) {

    const faFunction= isFunction(fileAnalysis) && fileAnalysis;
    faFunction && faFunction(true, isString(fileOrUrl) ? fileOrUrl : fileOrUrl?.name ? fileOrUrl.name : undefined);
    if (fileAnalysis) fileAnalysis=true;
    return upload(fileOrUrl, Boolean(fileAnalysis), params)
        .then( (results) => {
            faFunction && faFunction(false);
            return results;
        })
        .catch(() => {
            faFunction && faFunction(false);
        });
}
