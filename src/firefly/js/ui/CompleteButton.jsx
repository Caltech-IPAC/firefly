/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import AppDataCntlr from '../core/AppDataCntlr.js';





function validUpdate(valid,onSuccess,onFail,groupKey,dialogId) {
    var funcToCall = valid ? onSuccess : onFail;

    if (valid && dialogId) AppDataCntlr.hideDialog(dialogId);

    if (Array.isArray(groupKey)) {
        var requestAry = groupKey.map( (key) => FieldGroupUtils.getResults(key));
        funcToCall(requestAry);
    }
    else {
        var request = FieldGroupUtils.getResults(groupKey);
        funcToCall(request);
    }
}

function onClick(onSuccess,onFail,groupKey,dialogId) {
    if (groupKey) {
        FieldGroupUtils.validate(groupKey, (valid) => validUpdate(valid,onSuccess,onFail,groupKey,dialogId));
    }
    else {
        if (onSuccess) onSuccess();
        if (dialogId) AppDataCntlr.hideDialog(dialogId);
    }
}



function CompleteButton ({onFail, onSuccess, groupKey, text='OK', closeOnValid=true, dialogId,}) {
    return (
        <div>
            <button type='button' onClick={() => onClick(onSuccess,onFail,groupKey,dialogId)}>{text}</button>
        </div>
    );
}


CompleteButton.propTypes= {
    onFail: React.PropTypes.func,
    onSuccess: React.PropTypes.func,
    groupKey: React.PropTypes.any,
    text: React.PropTypes.string,
    closeOnValid: React.PropTypes.bool,
    dialogId: React.PropTypes.string
};




export default CompleteButton;
