import {Button, CircularProgress, Input, Stack, Tooltip, Typography} from '@mui/joy';
import React, {memo, useEffect} from 'react';
import {object, bool, func, number, string, shape} from 'prop-types';
import {has, isFunction, isNil, isString} from 'lodash';
import {getHttpErrorMessage} from '../util/HttpErrorMessage.js';
import {validateUrl} from '../util/Validate.js';
import {getStatusFromFetchError} from '../util/WebUtil.js';
import {InputFieldView} from './InputFieldView.jsx';
import {useFieldGroupConnector} from './FieldGroupConnector.jsx';
import {upload} from '../rpc/CoreServices.js';


const DD_FILE_TXT= 'or drag & drop a file here';
const DD_ANOTHER_FILE_TXT= 'or drag & drop another file';
const NO_FILE_TXT= 'No file chosen';

function FileUploadView({isLoading=false, label, valid, message, onChange, value,  sx, hasUploadedData,
                         isFromURL=false, onUrlAnalysis, canDragDrop}) {
    return (
        <Stack {...{ direction:'row', spacing:1, alignItems:'center', whiteSpace:'nowrap', sx}}>
            {isFromURL ?
                <ChooseUrl {...{valid, message, onChange, label, value,canDragDrop, onUrlAnalysis, hasUploadedData}}/> :
                <ChooseUploadFile {...{onChange, value, canDragDrop,
                    fileName: value ?  value.split(/([\\/])/g).pop() : ''}}/>}
            {isLoading && <CircularProgress size='sm'/>}
        </Stack>
    );
}

const ChooseUrl= ({valid, message, onChange, label, value,canDragDrop, onUrlAnalysis, hasUploadedData}) => (
    <Stack {...{className:'ff-FileUpload-url', direction:'row', spacing:2, alignItems:'flex-end'}}>
        <InputFieldView {...{
            endDecorator: (
                <Button color='success' variant={hasUploadedData?'soft':'solid'}
                        onClick={() =>  onUrlAnalysis(value)}>{hasUploadedData?'Upload again':'Upload'}
                </Button>),
            sx:{width: '50rem', '.MuiInput-root':{ 'paddingInlineEnd': 1, }},
            valid, message, onChange, label, value, type:'text',
            tooltip:!value?'enter a URL to upload from' : `Upload URL: ${value}`,
        }} />
        {canDragDrop &&
            <Typography color={value?'neutral':'warning'} level='body-md'>
                {hasUploadedData ? DD_ANOTHER_FILE_TXT : DD_FILE_TXT}
            </Typography>
        }
    </Stack>
);


const ChooseUploadFile= ({onChange, value, fileName, canDragDrop}) => (
    <Stack {...{className:'ff-FileUpload-upload', maxWidth: '50rem', direction:'row', alignItems:'center', spacing:2}}>
        <Tooltip title={`Click to choose a file${canDragDrop ?' or just drag and drop a file':''}`}>
            <label htmlFor='upload-file'>
                <Input id='upload-file' type='file' onChange={onChange} sx={{display:'none'}}/>
                <Button color='warning' variant={fileName?'soft':'solid'} aria-label='upload file' component='span'>
                    {fileName?'Replace File':'Choose File'}
                </Button>
            </label>
        </Tooltip>
        {value &&
            <Tooltip title={`Upload File: ${value}`}>
                <Typography {...{level:'title-lg',
                    sx:{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis'} }}>
                    {fileName||NO_FILE_TXT}
                </Typography>
            </Tooltip>
        }
        {canDragDrop ?
            <Typography color={value?'neutral':'warning'} level='body-md'>
                { value? DD_ANOTHER_FILE_TXT  : `Choose a file ${DD_FILE_TXT}` }
            </Typography>
            : !value ? <Typography color='warning' level='body-md'>Choose a file</Typography> : undefined
        }
    </Stack>
);





FileUploadView.propTypes = {
    fileType: string,
    isLoading: bool,
    message: string.isRequired,
    onChange: func.isRequired,
    value : string.isRequired,
    label: string,
    valid: bool,
    sx: object,
    isFromURL: bool,
    onUrlAnalysis: func,
    canDragDrop: bool
};


export const FileUpload= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    const {fileType='TABLE'} = viewProps;
    let modViewProps;

    useEffect(() => {
        if (viewProps.dropEvent) {
            handleChange(viewProps.dropEvent, fireValueChange, fileType, viewProps.fileAnalysis);
        }
    },[viewProps.dropEvent]);

    if (viewProps.isFromURL && !viewProps.dropEvent) {
        modViewProps= {
            ...viewProps,
            onChange: (ev) => onUrlChange(ev, viewProps, fireValueChange),
            value:  viewProps.displayValue,
            onUrlAnalysis: (value) => doUrlAnalysis(value, fireValueChange, fileType, viewProps.fileAnalysis)
        };
    }
    else {
        modViewProps= {
            ...viewProps,
            value: viewProps.displayValue,
            onChange: (ev) => handleChange(ev, fireValueChange, fileType, viewProps.fileAnalysis)
        };
    }
    return <FileUploadView {...{...modViewProps, hasUploadedData:Boolean(modViewProps.analysisResult) }}/> ;
});

FileUpload.propTypes = {
    fieldKey : string.isRequired,
    isFromURL: bool,
    label: string,
    sx: object,
    fileAnalysis: func,
    canDragDrop: bool,
    setDropEvent: func,
    dropEvent: object,
    initialState: shape({
        tooltip: string,
        label:  string,
    }),
};

/*---------------------------- private ----------------------------*/


function onUrlChange(ev, store, fireValueChange) {
    const displayValue = ev.target.value;
    const {valid,message, ...others}= store.validator(displayValue);
    let value= displayValue;

    has(others, 'value') && (value = others.value);    // allow the validator to modify the value.. useful in auto-correct.

    fireValueChange({ value, message, valid, displayValue, analysisResult:''});
}


function doUrlAnalysis(value, fireValueChange, type, fileAnalysis) {
     fireValueChange({value: makeDoUpload(value, type, true, fileAnalysis)()});
}


function handleChange(ev, fireValueChange, type, fileAnalysis) {
    let file = ev?.target?.files?.[0];
    let displayValue = ev?.target?.value;
    if (ev.type === 'drop') { //drag drop files - instead of picking file from 'Choose File'
        file = Array.from(ev.dataTransfer.files)[0];
        displayValue = file?.name;
    }
    fireValueChange({
        displayValue,
        value: !fileAnalysis ? makeDoUpload(file, type) : makeDoUpload(file, type, false, fileAnalysis)()
    });
}

function makeDoUpload(file, type, isFromURL, fileAnalysis) {
    return () => {
        return doUpload(isFromURL, file, fileAnalysis, {}).then(({status, message, cacheKey, fileFormat, analysisResult}) => {
            let valid = status === '200';
            if (valid) {        // json file is not supported currently (among many others)
                if (!isNil(fileFormat)) { // TODO: doUpload is not returning fileFormat field (analysisResult JSON string has this field though), has to be refactored
                    if (fileFormat.toLowerCase() === 'json') {
                        valid = false;
                        message = 'json file is not supported';
                        analysisResult = '';
                    }
                }
            }
            else {
                throw new Error(isFromURL ?
                                        `Unable to upload file from ${file}, status ${status} (${getHttpErrorMessage(status)})` :
                                        `Unable to upload file: ${file?.name}`);
            }

            return {isLoading: false, valid, message, value: cacheKey, analysisResult};
        }).catch((e) => {
            return {isLoading: false, valid: false, message: e.message};
        });
    };
}

function doUpload(isFromURL, fileOrUrl, fileAnalysis, params={}) {
    if (isFromURL && !validateUrl('',fileOrUrl).valid) {
        return Promise.resolve({status:404,message:'bad Url'});
    }
    const faFunction= isFunction(fileAnalysis) && fileAnalysis;
    faFunction && faFunction(true, isString(fileOrUrl) ? fileOrUrl : fileOrUrl?.name ? fileOrUrl.name : undefined);
    if (fileAnalysis) fileAnalysis=true;
    return upload(fileOrUrl, Boolean(fileAnalysis), params)
        .then( (results) => {
            faFunction && faFunction?.(false);
            return results;
        })
        .catch((e) => {
            faFunction && faFunction?.(false);
            return {status:getStatusFromFetchError(e.message),message:e.message};
        });
}
