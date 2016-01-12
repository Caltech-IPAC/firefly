/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import AppDataCntlr from '../core/AppDataCntlr.js';





function validUpdate(valid,onSuccess,onFail,groupKey,dialogId) {
    var funcToCall = valid ? onSuccess : onFail;

    // -lly : if there is more than one groupkey, it should combine the field values into a single request not
    // create an array of requests.  this is my opinion atm.  remove this comment once this is resolved.
    if (Array.isArray(groupKey)) {
        var requestAry = groupKey.map( (key) => FieldGroupUtils.getResults(key));
        funcToCall(requestAry);
    }
    else {
        var request = FieldGroupUtils.getResults(groupKey);
        funcToCall(request);
    }

    if (valid && dialogId) AppDataCntlr.hideDialog(dialogId);
}

function onClick(onSuccess,onFail,groupKey,dialogId) {
    if (groupKey) {
        FieldGroupUtils.validate(groupKey, (valid) => {
            validUpdate(valid,onSuccess,onFail,groupKey,dialogId);
        });
    }
    else {
        if (onSuccess) onSuccess();
        if (dialogId) AppDataCntlr.hideDialog(dialogId);
    }
}



function CompleteButton ({onFail, onSuccess, groupKey=null, text='OK', closeOnValid=true, dialogId,}, context) {
    if (!groupKey && context) groupKey= context.groupKey;
    return (
        <div>
            <button type='button' className='button-hl'  onClick={() => onClick(onSuccess,onFail,groupKey,dialogId)}><b>{text}</b></button>
        </div>
    );
}


CompleteButton.propTypes= {
    onFail: React.PropTypes.func,
    onSuccess: React.PropTypes.func,
    groupKey: React.PropTypes.string,
    text: React.PropTypes.string,
    closeOnValid: React.PropTypes.bool,
    dialogId: React.PropTypes.string
};

CompleteButton.contextTypes= {
    groupKey: React.PropTypes.string
};


export default CompleteButton;
