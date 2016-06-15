/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
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

function onClick(onSuccess,onFail,closeOnValid,groupKey,dialogId,includeUnmounted) {
    if (groupKey) {
        validateFieldGroup(groupKey, includeUnmounted)
            .then( (valid)=> validUpdate(valid,onSuccess,onFail,closeOnValid,groupKey,dialogId, includeUnmounted));
    }
    else {
        if (onSuccess) onSuccess();
        if (dialogId && closeOnValid) dispatchHideDialog(dialogId);
    }
}



export function CompleteButton ({onFail, onSuccess, groupKey=null, text='OK',
                          closeOnValid=true, dialogId,includeUnmounted= false,
                          style={}}, context) {
    if (!groupKey && context) groupKey= context.groupKey;
    const onComplete = () => onClick(onSuccess,onFail,closeOnValid,groupKey,dialogId,includeUnmounted);
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
    includeUnmounted : PropTypes.bool
};

CompleteButton.contextTypes= {
    groupKey: React.PropTypes.string
};

CompleteButton.defaultProps= { includeUnmounted : false };

export default CompleteButton;
