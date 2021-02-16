/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {delay} from 'lodash';
import React, {useContext, useEffect} from 'react';
import PropTypes from 'prop-types';
import {validateFieldGroup, getFieldGroupResults} from '../fieldGroup/FieldGroupUtils.js';
import {dispatchHideDialog} from '../core/ComponentCntlr.js';
import {GroupKeyCtx} from './FieldGroup';


export const NONE='COMPLETE-BUTTON-NO-CONTEXT';

function inGroup(e, groupKey) {
    for(;e; e= e.parentElement) {
        if (e.getAttribute('groupkey')===groupKey) {
            return true;
        }
    }
    return false;
}



function validUpdate(valid,onSuccess,onFail,closeOnValid,groupKey,dialogId, groupsToUse, includeUnmounted= false) {
    var funcToCall = valid ? onSuccess : onFail;
    let continueValid= true;

    if (Array.isArray(groupKey)) {
        const groupsToValidate= groupsToUse();
        var requestObj = groupsToValidate.reduce( (obj,groupKey) => {
            obj[groupKey]= getFieldGroupResults(groupKey,includeUnmounted);
            return obj;
        },{});
        if (funcToCall) funcToCall(requestObj);
    }
    else {
        var request = getFieldGroupResults(groupKey,includeUnmounted);
        if (funcToCall) continueValid= funcToCall(request);
    }

    const stillValid= valid && (continueValid ?? true);

    if (stillValid && dialogId && closeOnValid) dispatchHideDialog(dialogId);
}

function onClick(onSuccess,onFail,closeOnValid,groupKey,dialogId,groupsToUse, includeUnmounted, changeMasking) {
    if (groupKey) {
        if (changeMasking) changeMasking(true);
        validateFieldGroup(groupsToUse(), includeUnmounted)
            .then( (valid)=> {
                if (changeMasking) changeMasking(false);
                validUpdate(valid,onSuccess,onFail,closeOnValid,groupKey,dialogId, groupsToUse, includeUnmounted);
            });
    }
    else {
        if (onSuccess) onSuccess();
        if (dialogId && closeOnValid) dispatchHideDialog(dialogId);
    }
}




export function CompleteButton ({onFail, onSuccess, groupKey=null, text='OK',
                          closeOnValid=true, dialogId,includeUnmounted= false,
                          groupsToUse= () => groupKey,
                          style={}, innerStyle= {}, changeMasking, fireOnEnter= false,
                                    getDoOnClickFunc}) {
    const context= useContext(GroupKeyCtx);
    if (!groupKey && context) groupKey= context.groupKey;
    if (groupKey===NONE) groupKey= undefined;
    const onComplete = () => onClick(onSuccess,onFail,closeOnValid,groupKey,dialogId,
                                      groupsToUse, includeUnmounted,changeMasking);


    useEffect(() => {
        if (fireOnEnter) {
            const keyCheck= (ev) => {
                if ( ev.key==='Enter' && ev.target.tagName==='INPUT' && inGroup(ev.target,groupKey)) {
                    delay( () => {
                        onComplete();
                    }, 100);
                }
            };
            document.addEventListener('keypress',keyCheck);
            return () => document.removeEventListener('keypress', keyCheck);
        }
    }, []);

    useEffect(() => {
        getDoOnClickFunc?.( () => onComplete());
    }, [getDoOnClickFunc]);


    return (
        <div style={style}>
            <button type='button' style={innerStyle} className='button std hl'  onClick={onComplete}>{text}</button>
        </div>
    );
}


CompleteButton.propTypes= {
    onFail: PropTypes.func,
    onSuccess: PropTypes.func,
    groupsToUse: PropTypes.func,
    groupKey: PropTypes.any,
    text: PropTypes.string,
    closeOnValid: PropTypes.bool,
    dialogId: PropTypes.string,
    style: PropTypes.object,
    innerStyle: PropTypes.object,
    includeUnmounted : PropTypes.bool,
    changeMasking: PropTypes.func,
    fireOnEnter: PropTypes.bool,
    getDoOnClickFunc: PropTypes.func
};

export default CompleteButton;
