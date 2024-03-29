/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Button, Sheet, Stack} from '@mui/joy';
import React, {useCallback} from 'react';
import PropTypes, {object, shape} from 'prop-types';
import CompleteButton from './CompleteButton.jsx';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {HelpIcon} from './HelpIcon.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {isNil} from 'lodash';
import {dispatchFormCancel, dispatchFormSubmit} from 'firefly/core/AppDataCntlr.js';
import {Stacker} from 'firefly/ui/Stacker.jsx';

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
        requireAllValid,
        includeUnmounted=false, extraWidgets, extraWidgetsRight, sx, slotProps} = props;
    const { style, inputStyle, submitBarStyle} = props;

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

    const searchBarEnd = (
        <>
            {extraWidgetsRight}
            {help_id && <HelpIcon helpId={help_id} />}
        </>
    );

    return (
        <Stack component={Sheet} className='ff-FormPanel' flexGrow={1} spacing={1} p={1} height={1} sx={{...style, ...sx}}>
            <Stack flexGrow={1} sx={{...inputStyle, ...slotProps?.input}}>
                {children}
            </Stack>
            <Stacker endDecorator={searchBarEnd} {...slotProps?.searchBar}
                     sx={{...submitBarStyle, ...slotProps?.searchBar?.sx}}>
                <CompleteButton includeUnmounted={includeUnmounted}
                                groupKey={groupKey}
                                requireAllValid={requireAllValid}
                                getDoOnClickFunc={getDoOnClickFunc}
                                groupsToUse={groupsToUse}
                                onSuccess={doSubmit}
                                onFail={onError || handleFailure}
                                text = {submitText} changeMasking={changeMasking} />
                {cancelText && <ExtraButton onClick={doCancel} text={cancelText}/>}

                {extraWidgets}
            </Stacker>
        </Stack>
    );
};


// Use onSubmit, action, param, and title props when the callback expects a table request
// Use onSuccess for a generic callback, expecting object with key-values for group fields
// If onSuccess is provided, onSubmit, action, param, and title properties are ignored
FormPanel.propTypes = {
    submitText: PropTypes.string,
    cancelText:PropTypes.string,
    title: PropTypes.string,
    sx: PropTypes.object,
    style: PropTypes.object,
    inputStyle: PropTypes.object,
    submitBarStyle: PropTypes.object,
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
    getDoOnClickFunc: PropTypes.func,
    slotProps: shape({
        input: object,
        searchBar: object,
    }),
};

export function ExtraButton({text, onClick}) {
    return (
        <Button {...{size:'md', onClick}}>{text}</Button>
    );
}

ExtraButton.propTypes = {
    text: PropTypes.string,
    onClick: PropTypes.func,
    style: PropTypes.object
};