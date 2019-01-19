/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import CompleteButton from './CompleteButton.jsx';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {HelpIcon} from './HelpIcon.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {isNil, get} from 'lodash';

function handleFailfure() {

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

        // submitResult is not defined, or it is true, or it is false and hideOnInValid is true
        if (isNil(submitResult) || submitResult || (!submitResult&&get(params, 'hideOnInvalid', true))) {
            dispatchHideDropDown();
        }
    };
}

export const FormPanel = function (props) {
    const { children, onSuccess, onSubmit, onCancel=dispatchHideDropDown, onError, groupKey,
        action, params, title, style, inputStyle, submitBarStyle,
        submitText='Search',cancelText='Cancel', help_id, changeMasking,
        includeUnmounted=false, extraButtons=[]} = props;

    const childrenStyle = Object.assign({
        backgroundColor: 'white',
        border: '1px solid rgba(0,0,0,0.2)',
        padding: 5,
        marginBottom: 5,
        boxSizing: 'border-box',
        flexGrow: 1
    }, inputStyle);
    const mStyle = Object.assign({height: '100%', display:'flex', flexDirection: 'column', boxSizing: 'border-box'}, style);
    const barStyle = Object.assign({flexGrow: 0, display: 'inline-flex', justifyContent: 'space-between', boxSizing: 'border-box',
                                  width: '100%', alignItems: 'flex-end', padding:'2px 0px 3px'}, submitBarStyle);

    return (
        <div style={mStyle}>
            <div style={childrenStyle}>
                {children}
            </div>
            <div style={barStyle}>
                <div>
                    <CompleteButton style={{display: 'inline-block', marginRight: 10}}
                                    includeUnmounted={includeUnmounted}
                                    groupKey={groupKey}
                                    onSuccess={onSuccess||createSuccessHandler(action, params, title, onSubmit)}
                                    onFail={onError || handleFailfure}
                                    text = {submitText} changeMasking={changeMasking}
                    />
                    <button style={{display: 'inline-block'}} type='button' className='button std' onClick={onCancel}>{cancelText}</button>

                </div>
                <div>
                    {extraButtons && extraButtons.map((e,i,arr)=>{
                        const marginRight = i < arr.length-1 || help_id ? 10 : 0;
                        return (
                            <button style={{display: 'inline-block', marginRight}}
                                    type='button' className='button std'
                                    onClick={e.onClick} key={'extraBtn'+i}>
                                {e.text}
                            </button>
                        );
                    })}
                    {help_id && <HelpIcon helpId={help_id} />}

                </div>
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
    onSubmit: PropTypes.func, // onSubmit(request) - callback that accepts table request, use with action, params, and title props
    onSuccess: PropTypes.func, // onSuccess(fields) - callback that takes fields object, its keys are the field keys for fields in the given group
    onCancel: PropTypes.func,
    onError: PropTypes.func,
    groupKey: PropTypes.any,
    action: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
    params: PropTypes.object,
    help_id: PropTypes.string,
    changeMasking: PropTypes.func,
    includeUnmounted: PropTypes.bool,
    extraButtons: PropTypes.arrayOf(
        PropTypes.shape({
            text: PropTypes.string,
            onClick: PropTypes.func
        }))
};
