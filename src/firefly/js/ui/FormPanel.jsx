/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Button, Stack} from '@mui/joy';
import React, {useCallback} from 'react';
import PropTypes from 'prop-types';
import CompleteButton from './CompleteButton.jsx';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {HelpIcon} from './HelpIcon.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {isNil} from 'lodash';
import {dispatchFormCancel, dispatchFormSubmit} from 'firefly/core/AppDataCntlr.js';

function handleFailure() {

}

function createSuccessHandler(action, params={}, title, onSubmit) {
    return (request={}) => {
        request = Object.assign({}, params, request);
        const reqTitle = title && (typeof title === 'function') ? title(request) : title;
        request = makeTblRequest(request.id, reqTitle || request.title, request, params);

        if (action) {
            if (typeof action === 'function') {
                action(request);
            } else {
                switch (action) {
                    case TablesCntlr.TABLE_SEARCH  :
                        TablesCntlr.dispatchTableSearch(request);
                        break;
                }
            }
        }

        let submitResult;
        if (onSubmit) {
            submitResult = onSubmit(request);
        }

        // By default, onSubmit returns true.  So, return false only when onSubmit explicitly returns false
        return isNil(submitResult) || submitResult;
    };
}

export const FormPanel = function (props) {
    const { children, onSuccess, onSubmit, onCancel=dispatchHideDropDown, onError, groupKey, groupsToUse,
        action, params, title, getDoOnClickFunc, submitText='Search',cancelText='Cancel', help_id, changeMasking,
        includeUnmounted=false, extraWidgets=[], extraWidgetsRight=[]} = props;
    let { style, inputStyle, submitBarStyle, buttonStyle} = props;

    inputStyle = Object.assign({
        backgroundColor: 'white',
        border: '1px solid rgba(0,0,0,0.2)',
        padding: 5,
        marginBottom: 5,
        boxSizing: 'border-box',
        flexGrow: 1
    }, inputStyle);
    style = Object.assign({height: '100%', display:'flex', flexDirection: 'column', boxSizing: 'border-box'}, style);
    submitBarStyle = Object.assign({flexGrow: 0, display: 'inline-flex', justifyContent: 'space-between', boxSizing: 'border-box',
                                  width: '100%', alignItems: 'flex-end', padding:'2px 0px 3px'}, submitBarStyle);
    buttonStyle = Object.assign({flexGrow: 0, display: 'inline-flex', width: '100%', justifyContent: 'space-between'}, buttonStyle);

    const doSubmit = ((p) => {
        const handler = onSuccess ?? createSuccessHandler(action, params, title, onSubmit);
        const valid = handler(p);
        if (valid) {
            dispatchFormSubmit(p);
        }
        // handle dropdown
        if (params?.disabledDropdownHide) return;
        if (valid || (params?.hideOnInvalid ?? true)) {
            dispatchHideDropDown();
        }
    });

    const doCancel = useCallback(() => {
        dispatchFormCancel();
        onCancel?.();
    }, []);

    return (
        <div style={style}>
            <div style={inputStyle}>
                {children}
            </div>
            <div style={submitBarStyle}>
                <Stack spacing={2} direction='row'>
                    <Stack spacing={1} direction='row'>
                        <CompleteButton style={{display: 'inline-block', marginRight: 10}}
                                        includeUnmounted={includeUnmounted}
                                        groupKey={groupKey}
                                        getDoOnClickFunc={getDoOnClickFunc}
                                        groupsToUse={groupsToUse}
                                        onSuccess={doSubmit}
                                        onFail={onError || handleFailure}
                                        text = {submitText} changeMasking={changeMasking} />
                        <ExtraButton onClick={doCancel} text={cancelText}/>
                    </Stack>
                    {Boolean(extraWidgets?.length) &&
                        <Stack spacing={1} direction='row' alignItems='center'>
                            {extraWidgets}
                        </Stack>}
                </Stack>
                <>
                    {extraWidgetsRight}
                    {help_id && <HelpIcon helpId={help_id} />}
                </>
            </div>
        </div>
    );
};


// Use onSubmit, action, param, and title props when the callback expects a table request
// Use onSuccess for a generic callback, expecting object with key-values for group fields
// If onSuccess is provided, onSubmit, action, param, and title properties are ignored
FormPanel.propTypes = {
    submitText: PropTypes.string,
    cancelText:PropTypes.string,
    title: PropTypes.string,
    style: PropTypes.object,
    inputStyle: PropTypes.object,
    submitBarStyle: PropTypes.object,
    buttonStyle: PropTypes.object,
    onSubmit: PropTypes.func, // onSubmit(request) - callback that accepts table request, use with action, params, and title props
    onSuccess: PropTypes.func, // onSuccess(fields) - callback that takes fields object, its keys are the field keys for fields in the given group
    onCancel: PropTypes.func,
    onError: PropTypes.func,
    groupKey: PropTypes.any,
    groupsToUse: PropTypes.func,
    action: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
    params: PropTypes.object,
    help_id: PropTypes.string,
    changeMasking: PropTypes.func,
    includeUnmounted: PropTypes.bool,
    extraWidgets: PropTypes.arrayOf(PropTypes.element),
    getDoOnClickFunc: PropTypes.func
};

export function ExtraButton(props) {
    const {text, onClick, style={}} = props;
    return (
        <Button {...{variant: 'soft', color: 'neutral', size:'md', onClick}}>{text}</Button>
    );
}

ExtraButton.propTypes = {
    text: PropTypes.string,
    onClick: PropTypes.func,
    style: PropTypes.object
};