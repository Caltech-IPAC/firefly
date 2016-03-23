/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import FieldGroupUtils, {validateFieldGroup, getFieldGroupResults} from '../fieldGroup/FieldGroupUtils.js';
import AppDataCntlr from '../core/AppDataCntlr.js';
import {dispatchHideDialog} from '../core/DialogCntlr.js';





function validUpdate(valid,onSuccess,onFail,groupKey,dialogId, includeUnmounted= false) {
    var funcToCall = valid ? onSuccess : onFail;

    // -lly : if there is more than one groupkey, it should combine the field values into a single request not
    // create an array of requests.  this is my opinion atm.  remove this comment once this is resolved.
    if (Array.isArray(groupKey)) {
        var requestAry = groupKey.map( (key) => getFieldGroupResults(key,includeUnmounted));
        funcToCall(requestAry);
    }
    else {
        var request = getFieldGroupResults(groupKey,includeUnmounted);
        funcToCall(request);
    }

    if (valid && dialogId) dispatchHideDialog(dialogId);
}

function onClick(onSuccess,onFail,groupKey,dialogId,includeUnmounted) {
    if (groupKey) {
        validateFieldGroup(groupKey, includeUnmounted, (valid) => {
            validUpdate(valid,onSuccess,onFail,groupKey,dialogId, includeUnmounted);
        });
    }
    else {
        if (onSuccess) onSuccess();
        if (dialogId) dispatchHideDialog(dialogId);
    }
}



function CompleteButton ({onFail, onSuccess, groupKey=null, text='OK',
                          closeOnValid=true, dialogId,includeUnmounted= false,
                          style={}}, context) {
    if (!groupKey && context) groupKey= context.groupKey;
    return (
        <div style={style}>
            <button type='button' className='button-hl'  onClick={() =>
                                    onClick(onSuccess,onFail,groupKey,dialogId,includeUnmounted)}>
                <b>{text}</b>
            </button>
        </div>
    );
}


CompleteButton.propTypes= {
    onFail: PropTypes.func,
    onSuccess: PropTypes.func,
    groupKey: PropTypes.string,
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
