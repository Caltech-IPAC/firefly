/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {validateFieldGroup, getFieldGroupResults} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchHideDialog} from '../core/ComponentCntlr.js';





function validUpdate(valid,onSuccess,onFail,closeOnValid,groupKey,dialogId, includeUnmounted= false) {
    var funcToCall = valid ? onSuccess : onFail;

    if (Array.isArray(groupKey)) {
        var requestObj = groupKey.reduce( (obj,groupKey) => {
            obj[groupKey]= getFieldGroupResults(groupKey,includeUnmounted);
            return obj;
        },{});
        if (funcToCall) funcToCall(requestObj);
    }
    else {
        var request = getFieldGroupResults(groupKey,includeUnmounted);
        if (funcToCall) funcToCall(request);
    }

    if (valid && dialogId && closeOnValid) dispatchHideDialog(dialogId);
}

function onClick(onSuccess,onFail,closeOnValid,groupKey,dialogId,includeUnmounted, changeMasking) {
    if (groupKey) {
        if (changeMasking) changeMasking(true);
        validateFieldGroup(groupKey, includeUnmounted)
            .then( (valid)=> {
                if (changeMasking) changeMasking(false);
                validUpdate(valid,onSuccess,onFail,closeOnValid,groupKey,dialogId, includeUnmounted);
            });
    }
    else {
        if (onSuccess) onSuccess();
        if (dialogId && closeOnValid) dispatchHideDialog(dialogId);
    }
}



export function CompleteButton ({onFail, onSuccess, groupKey=null, text='OK',
                          closeOnValid=true, dialogId,includeUnmounted= false,
                          style={}, changeMasking}, context) {
    if (!groupKey && context) groupKey= context.groupKey;
    const onComplete = () => onClick(onSuccess,onFail,closeOnValid,groupKey,dialogId,includeUnmounted,changeMasking);
    return (
        <div style={style}>
            <button type='button' className='button std hl'  onClick={onComplete}>{text}</button>
        </div>
    );
}


CompleteButton.propTypes= {
    onFail: PropTypes.func,
    onSuccess: PropTypes.func,
    groupKey: PropTypes.any,
    text: PropTypes.string,
    closeOnValid: PropTypes.bool,
    dialogId: PropTypes.string,
    style: PropTypes.object,
    includeUnmounted : PropTypes.bool,
    changeMasking: PropTypes.func,
};

CompleteButton.contextTypes= {
    groupKey: PropTypes.string
};

CompleteButton.defaultProps= { includeUnmounted : false };

export default CompleteButton;
